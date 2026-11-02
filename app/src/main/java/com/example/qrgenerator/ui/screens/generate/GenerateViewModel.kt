package com.example.qrgenerator.ui.screens.generate

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.content.res.Resources
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrgenerator.data.model.*
import com.example.qrgenerator.data.repository.QrHistoryRepository
import com.example.qrgenerator.data.repository.SettingsRepository
import com.example.qrgenerator.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

sealed interface GenerateEvent {
    data class SelectScene(val scene: QrScene) : GenerateEvent
    data class UpdateCardTitle(val title: String) : GenerateEvent
    data class UpdateCardSubtitle(val subtitle: String) : GenerateEvent
    data class SetCardEnabled(val enabled: Boolean) : GenerateEvent
    data object RestoreReadableStyle : GenerateEvent
    data class UpdateContentType(val type: QrContentType) : GenerateEvent
    data class UpdateContent(val content: String) : GenerateEvent
    data object Generate : GenerateEvent
    data object SaveToGallery : GenerateEvent
    data object Share : GenerateEvent
    data class ExportFile(val format: ExportFormat) : GenerateEvent
    data class SaveDocument(val uri: Uri?) : GenerateEvent
    data object ClearDocumentRequest : GenerateEvent
    data object ClearShareIntent : GenerateEvent
    data object ClearError : GenerateEvent
    data object ClearMessage : GenerateEvent
}

data class DocumentRequest(val format: ExportFormat, val filename: String)
data class GenerateUiState(
    val config: QrConfig = QrConfig(),
    val qrDrawable: Drawable? = null,
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val validationMessage: String? = null,
    val errorMessage: String? = null,
    val message: String? = null,
    val shareIntent: Intent? = null,
    val documentRequest: DocumentRequest? = null,
    val readability: QrReadability? = null,
    val defaultExportFormat: ExportFormat = ExportFormat.PNG
)

private data class RenderedQr(val config: QrConfig, val bitmap: Bitmap)
private data class CheckedArtwork(val bitmap: Bitmap, val readability: QrReadability)

class GenerateViewModel(
    private val qrGeneratorUseCase: QrGeneratorUseCase,
    private val exportUseCase: ExportUseCase,
    private val contentValidator: ContentValidator,
    private val qrConfigHolder: QrConfigHolder,
    private val historyRepository: QrHistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val cardRenderer: QrCardRenderer = QrCardRenderer(),
    private val readabilityChecker: QrReadabilityChecker = QrReadabilityChecker()
) : ViewModel() {
    private val mutableState: MutableStateFlow<GenerateUiState> = MutableStateFlow(GenerateUiState())
    val uiState: StateFlow<GenerateUiState> = mutableState.asStateFlow()
    private val renderRevision: MutableStateFlow<Int> = MutableStateFlow(0)
    private var renderedQr: RenderedQr? = null
    private var documentQr: RenderedQr? = null
    private var documentFormat: ExportFormat = ExportFormat.PNG
    private val drafts: MutableMap<QrContentType, String> = mutableMapOf()
    private val sceneDrafts: MutableMap<QrScene, QrConfig> = mutableMapOf()
    private val initialConfig: QrConfig = qrConfigHolder.config.value
    private val initialReplacementRevision: Long = qrConfigHolder.replacementRevision
    private var previousSettings: AppSettings? = null
    private var hasSettingsReadFailure: Boolean = false

    init {
        observeSettings()
        viewModelScope.launch {
            combine(qrConfigHolder.config, renderRevision) { config, _ -> config }.collectLatest { config -> render(config) }
        }
    }

    /** 读取失败只重试读取，不把默认值写回，也不重置用户正在编辑的内容。 */
    private fun observeSettings() {
        viewModelScope.launch {
            flow { emitAll(settingsRepository.executeGetSettings()) }
                .retryWhen { cause, attempt ->
                    if (cause !is IOException) return@retryWhen false
                    if (!hasSettingsReadFailure) {
                        hasSettingsReadFailure = true
                        mutableState.update { it.copy(message = "暂时读取不了设置，正在重试。当前内容已保留。") }
                    }
                    delay(minOf(1_000L * (1L shl attempt.coerceAtMost(5).toInt()), 30_000L))
                    true
                }
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    mutableState.update { it.copy(errorMessage = "读取设置失败，当前内容已保留。请重新打开应用后再试。") }
                }
                .collect { settings -> applySettings(settings) }
        }
    }

    private fun applySettings(settings: AppSettings) {
        val previous: AppSettings? = previousSettings
        qrConfigHolder.executeUpdateConfig { config ->
            val isInitialDraft: Boolean = initialReplacementRevision == 0L && initialConfig.content.isBlank() &&
                qrConfigHolder.replacementRevision == initialReplacementRevision
            val defaults: QrConfig = QrConfig()
            val applySize: Boolean = if (previous != null) previous.defaultImageSize != settings.defaultImageSize else
                isInitialDraft && initialConfig.imageWidth == defaults.imageWidth && initialConfig.imageHeight == defaults.imageHeight &&
                    config.imageWidth == initialConfig.imageWidth && config.imageHeight == initialConfig.imageHeight
            val applyErrorLevel: Boolean = if (previous != null) previous.defaultErrorLevel != settings.defaultErrorLevel else
                isInitialDraft && initialConfig.errorCorrectionLevel == defaults.errorCorrectionLevel &&
                    config.errorCorrectionLevel == initialConfig.errorCorrectionLevel
            config.copy(
                imageWidth = if (applySize) settings.defaultImageSize else config.imageWidth,
                imageHeight = if (applySize) settings.defaultImageSize else config.imageHeight,
                errorCorrectionLevel = if (applyErrorLevel) settings.defaultErrorLevel else config.errorCorrectionLevel
            )
        }
        mutableState.update {
            it.copy(
                defaultExportFormat = if (settings.defaultExportFormat == ExportFormat.SVG) ExportFormat.PNG else settings.defaultExportFormat,
                message = if (hasSettingsReadFailure) "设置已恢复" else it.message
            )
        }
        hasSettingsReadFailure = false
        previousSettings = settings
    }

    fun onEvent(event: GenerateEvent) {
        when (event) {
            is GenerateEvent.SelectScene -> selectScene(event.scene)
            is GenerateEvent.UpdateCardTitle -> updateConfig { it.copy(card = it.card.copy(title = event.title.take(24))) }
            is GenerateEvent.UpdateCardSubtitle -> updateConfig { it.copy(card = it.card.copy(subtitle = event.subtitle.take(48))) }
            is GenerateEvent.SetCardEnabled -> updateConfig { it.copy(card = it.card.copy(enabled = event.enabled)) }
            GenerateEvent.RestoreReadableStyle -> restoreReadableStyle()
            is GenerateEvent.UpdateContent -> updateConfig { it.copy(content = event.content) }
            is GenerateEvent.UpdateContentType -> changeType(event.type)
            GenerateEvent.Generate -> renderRevision.update { it + 1 }
            GenerateEvent.SaveToGallery -> saveToGallery()
            GenerateEvent.Share -> share()
            is GenerateEvent.ExportFile -> requestDocument(event.format)
            is GenerateEvent.SaveDocument -> saveDocument(event.uri)
            GenerateEvent.ClearDocumentRequest -> mutableState.update { it.copy(documentRequest = null) }
            GenerateEvent.ClearShareIntent -> mutableState.update { it.copy(shareIntent = null) }
            GenerateEvent.ClearError -> mutableState.update { it.copy(errorMessage = null) }
            GenerateEvent.ClearMessage -> mutableState.update { it.copy(message = null) }
        }
    }

    private fun changeType(type: QrContentType) {
        if (type == qrConfigHolder.config.value.contentType) return
        val current: QrConfig = qrConfigHolder.config.value
        drafts[current.contentType] = current.content
        updateConfig { it.copy(contentType = type, content = drafts[type].orEmpty(), card = it.card.copy(scene = QrScene.FREE)) }
    }

    /** 用途之间保留完整草稿，切回时不丢失标题和手工样式。 */
    private fun selectScene(scene: QrScene) {
        val current: QrConfig = qrConfigHolder.config.value
        if (current.card.scene == scene) return
        sceneDrafts[current.card.scene] = current
        drafts[current.contentType] = current.content
        val next: QrConfig = sceneDrafts[scene] ?: current.copy(
            contentType = if (scene == QrScene.FREE) current.contentType else scene.contentType,
            content = if (scene == QrScene.FREE || scene.contentType == current.contentType) current.content else drafts[scene.contentType].orEmpty(),
            card = QrCardConfig(scene, scene != QrScene.FREE, scene.defaultTitle, scene.defaultSubtitle)
        )
        updateConfig { next }
    }

    private fun restoreReadableStyle() {
        updateConfig { current ->
            QrConfig(
                contentType = current.contentType, content = current.content,
                imageWidth = current.imageWidth, imageHeight = current.imageHeight,
                errorCorrectionLevel = QrErrorLevel.HIGH, card = current.card
            )
        }
    }

    private fun updateConfig(transform: (QrConfig) -> QrConfig) {
        val current: QrConfig = qrConfigHolder.config.value
        val updated: QrConfig = transform(current)
        if (updated == current) return
        renderedQr = null
        mutableState.update { it.copy(config = updated, qrDrawable = null, validationMessage = null, readability = null) }
        qrConfigHolder.executeUpdateConfig { updated }
    }

    private suspend fun render(config: QrConfig) {
        renderedQr = null
        mutableState.update { it.copy(config = config, qrDrawable = null, isGenerating = false, validationMessage = null, readability = null) }
        if (config.content.isBlank()) return
        delay(350)
        val validation: ValidationResult = contentValidator.validate(config.contentType, config.content)
        if (validation is ValidationResult.Invalid) {
            mutableState.update { it.copy(validationMessage = validation.message) }
            return
        }
        mutableState.update { it.copy(isGenerating = true) }
        try {
            val artwork: CheckedArtwork = withContext(Dispatchers.Default) {
                val qrBitmap: Bitmap = exportUseCase.executeDrawableToBitmap(qrGeneratorUseCase.execute(config), config.imageWidth, config.imageHeight)
                val bitmap: Bitmap = cardRenderer.execute(qrBitmap, config.card)
                if (bitmap !== qrBitmap) qrBitmap.recycle()
                CheckedArtwork(bitmap, readabilityChecker.execute(bitmap, QrContentCodec.encode(config).encode()))
            }
            if (qrConfigHolder.config.value != config) return
            renderedQr = if (artwork.readability == QrReadability.VERIFIED) RenderedQr(config, artwork.bitmap) else null
            mutableState.update { it.copy(qrDrawable = BitmapDrawable(Resources.getSystem(), artwork.bitmap), isGenerating = false, readability = artwork.readability) }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            renderedQr = null
            mutableState.update { it.copy(isGenerating = false, validationMessage = "暂时生成不了，请减少内容或换回简洁样式。") }
        }
    }

    private fun saveToGallery() = performExport { qr ->
        exportUseCase.executeSaveToGallery(qr.bitmap, "二维码_${System.currentTimeMillis()}.png")
        "已保存到相册"
    }

    private fun share() = performExport { qr ->
        val intent: Intent = exportUseCase.executeCreateShareIntent(qr.bitmap)
        mutableState.update { it.copy(shareIntent = intent) }
        null
    }

    private fun requestDocument(format: ExportFormat) {
        if (mutableState.value.isSaving) return
        documentQr = currentQr() ?: return
        documentFormat = if (format == ExportFormat.SVG) ExportFormat.PNG else format
        mutableState.update { it.copy(documentRequest = DocumentRequest(documentFormat, "二维码_${System.currentTimeMillis()}.${documentFormat.extension}")) }
    }

    private fun saveDocument(uri: Uri?) {
        val qr: RenderedQr = documentQr ?: return
        documentQr = null
        if (uri == null) return
        performExport(qr) {
            exportUseCase.executeSaveDocument(it.bitmap, uri, documentFormat)
            "文件已保存"
        }
    }

    private fun currentQr(): RenderedQr? = renderedQr?.takeIf {
        it.config == qrConfigHolder.config.value && mutableState.value.qrDrawable != null &&
            !mutableState.value.isGenerating && mutableState.value.readability == QrReadability.VERIFIED
    }

    private fun performExport(qr: RenderedQr? = currentQr(), action: suspend (RenderedQr) -> String?) {
        if (qr == null || mutableState.value.isSaving) return
        mutableState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val message: String? = withContext(Dispatchers.IO) { action(qr) }
                var historyFailed: Boolean = false
                withContext(Dispatchers.IO) {
                    try { historyRepository.executeSave(qr.config, exportUseCase.executeSaveThumbnail(qr.bitmap)) }
                    catch (exception: CancellationException) { throw exception }
                    catch (exception: Exception) { historyFailed = true }
                }
                mutableState.update { it.copy(isSaving = false, message = if (historyFailed) "${message ?: "分享图片已准备好"}，但未能保存到记录" else message) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableState.update { it.copy(isSaving = false, errorMessage = exception.message ?: "保存失败，请检查存储空间后重试") }
            }
        }
    }
}
