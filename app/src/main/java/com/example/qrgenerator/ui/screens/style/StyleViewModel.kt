package com.example.qrgenerator.ui.screens.style

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrgenerator.data.model.*
import com.example.qrgenerator.domain.QrConfigHolder
import com.example.qrgenerator.domain.QrGeneratorUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 样式页面事件；所有改动都进入共享配置，生成和保存使用同一份配置。 */
sealed interface StyleEvent {
    data class UpdatePixelShape(val shape: QrPixelShapeType) : StyleEvent
    data class UpdateLightPixelShape(val shape: QrPixelShapeType) : StyleEvent
    data class UpdateBallShape(val shape: QrBallShapeType) : StyleEvent
    data class UpdateFrameShape(val shape: QrFrameShapeType) : StyleEvent
    data class UpdateDarkColor(val color: QrColorConfig) : StyleEvent
    data class UpdateBallColor(val color: QrColorConfig) : StyleEvent
    data class UpdateFrameColor(val color: QrColorConfig) : StyleEvent
    data class UpdateCodeShape(val shape: CodeShapeType) : StyleEvent
    data class UpdateErrorLevel(val level: QrErrorLevel) : StyleEvent
    data class UpdateFourthEye(val enabled: Boolean) : StyleEvent
    data class UpdateCentralSymmetry(val enabled: Boolean) : StyleEvent
    data class UpdateHighlighting(val highlighting: QrHighlightingConfig) : StyleEvent
    data class UpdateLogo(val logo: QrLogoConfig) : StyleEvent
    data class UpdateConfig(val transform: (QrConfig) -> QrConfig) : StyleEvent
    data class ApplyPreset(val preset: StylePreset) : StyleEvent
    data object ResetStyle : StyleEvent
}

data class StylePreviewState(
    val bitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class StyleViewModel(
    private val qrConfigHolder: QrConfigHolder,
    private val qrGeneratorUseCase: QrGeneratorUseCase
) : ViewModel() {
    val config: StateFlow<QrConfig> = qrConfigHolder.config
    private val mutablePreview: MutableStateFlow<StylePreviewState> = MutableStateFlow(StylePreviewState())
    val preview: StateFlow<StylePreviewState> = mutablePreview.asStateFlow()

    init {
        viewModelScope.launch {
            config.collectLatest { current ->
                mutablePreview.value = StylePreviewState(isLoading = true)
                delay(180)
                renderPreview(current)
            }
        }
    }

    fun onEvent(event: StyleEvent) {
        when (event) {
            is StyleEvent.UpdatePixelShape -> update { it.copy(darkPixelShape = event.shape) }
            is StyleEvent.UpdateLightPixelShape -> update { it.copy(lightPixelShape = event.shape) }
            is StyleEvent.UpdateBallShape -> update { it.copy(ballShape = event.shape) }
            is StyleEvent.UpdateFrameShape -> update { it.copy(frameShape = event.shape) }
            is StyleEvent.UpdateDarkColor -> update { it.copy(darkColor = event.color) }
            is StyleEvent.UpdateBallColor -> update { it.copy(ballColor = event.color) }
            is StyleEvent.UpdateFrameColor -> update { it.copy(frameColor = event.color) }
            is StyleEvent.UpdateCodeShape -> update { it.copy(codeShape = event.shape) }
            is StyleEvent.UpdateErrorLevel -> update { it.copy(errorCorrectionLevel = event.level) }
            is StyleEvent.UpdateFourthEye -> update { it.copy(fourthEyeEnabled = event.enabled) }
            is StyleEvent.UpdateCentralSymmetry -> update { it.copy(centralSymmetry = event.enabled) }
            is StyleEvent.UpdateHighlighting -> update { it.copy(highlighting = event.highlighting) }
            is StyleEvent.UpdateLogo -> update { it.copy(logo = event.logo) }
            is StyleEvent.UpdateConfig -> update(event.transform)
            is StyleEvent.ApplyPreset -> update { event.preset.applyTo(it) }
            StyleEvent.ResetStyle -> update { QrConfig(contentType = it.contentType, content = it.content, imageWidth = it.imageWidth, imageHeight = it.imageHeight, card = it.card) }
        }
    }

    private suspend fun renderPreview(current: QrConfig) {
        try {
            val bitmap: Bitmap = withContext(Dispatchers.Default) {
                val previewConfig = if (current.content.isBlank()) current.copy(contentType = QrContentType.TEXT, content = "你好，二维码！") else current
                val drawable = qrGeneratorUseCase.execute(previewConfig)
                val previewWidth = 480
                val previewHeight = (previewWidth * current.imageHeight.toFloat() / current.imageWidth).toInt().coerceIn(240, 960)
                Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888).also {
                    drawable.setBounds(0, 0, previewWidth, previewHeight)
                    drawable.draw(Canvas(it))
                }
            }
            mutablePreview.value = StylePreviewState(bitmap = bitmap)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            mutablePreview.value = StylePreviewState(error = "暂时无法预览，请检查填写的内容或换一种样式")
        }
    }

    private fun update(transform: (QrConfig) -> QrConfig) {
        qrConfigHolder.executeUpdateConfig(transform)
    }
}
