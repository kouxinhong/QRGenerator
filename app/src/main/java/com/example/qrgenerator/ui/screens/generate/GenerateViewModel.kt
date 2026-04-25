package com.example.qrgenerator.ui.screens.generate

import android.app.Application
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.domain.ExportUseCase
import com.example.qrgenerator.domain.QrConfigHolder
import com.example.qrgenerator.domain.QrGeneratorUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 生成页面 UI 状态 */
data class GenerateUiState(
    val config: QrConfig = QrConfig(),
    val qrDrawable: Drawable? = null,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val shareIntent: Intent? = null
)

/** 生成页面 ViewModel */
class GenerateViewModel(
    private val qrGeneratorUseCase: QrGeneratorUseCase,
    private val exportUseCase: ExportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenerateUiState())
    val uiState: StateFlow<GenerateUiState> = _uiState.asStateFlow()

    private var generateJob: Job? = null

    init {
        // 监听共享配置变更（来自样式页面）
        viewModelScope.launch {
            QrConfigHolder.config.collect { sharedConfig ->
                _uiState.update { it.copy(config = sharedConfig) }
                executeDebounceGenerate()
            }
        }
    }

    /** 更新内容类型 */
    fun executeUpdateContentType(type: QrContentType) {
        val newConfig = _uiState.value.config.copy(contentType = type, content = "")
        _uiState.update { it.copy(config = newConfig) }
        QrConfigHolder.executeSetConfig(newConfig)
    }

    /** 更新内容文本 */
    fun executeUpdateContent(content: String) {
        val newConfig = _uiState.value.config.copy(content = content)
        _uiState.update { it.copy(config = newConfig) }
        QrConfigHolder.executeSetConfig(newConfig)
        executeDebounceGenerate()
    }

    /** 防抖生成（300ms 延迟） */
    private fun executeDebounceGenerate() {
        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            delay(300)
            executeGenerate()
        }
    }

    /** 立即生成二维码 */
    fun executeGenerate() {
        val config = _uiState.value.config
        if (config.content.isBlank()) {
            _uiState.update { it.copy(qrDrawable = null, isGenerating = false) }
            return
        }
        _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val drawable = withContext(Dispatchers.Default) {
                    qrGeneratorUseCase.execute(config)
                }
                _uiState.update { it.copy(qrDrawable = drawable, isGenerating = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        qrDrawable = null,
                        isGenerating = false,
                        errorMessage = "生成失败：${e.message ?: "内容过长或格式错误"}"
                    )
                }
            }
        }
    }

    /** 保存二维码到相册 */
    fun executeSaveToGallery() {
        val drawable = _uiState.value.qrDrawable ?: return
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    exportUseCase.executeDrawableToBitmap(drawable)
                }
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val filename = "QR_$timestamp.png"
                val uri = withContext(Dispatchers.IO) {
                    exportUseCase.executeSaveToGallery(bitmap, filename)
                }
                if (uri != null) {
                    _uiState.update { it.copy(errorMessage = null) }
                } else {
                    _uiState.update { it.copy(errorMessage = "保存失败：无法写入相册") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "保存失败：${e.message}") }
            }
        }
    }

    /** 分享二维码 */
    fun executeShare() {
        val drawable = _uiState.value.qrDrawable ?: return
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    exportUseCase.executeDrawableToBitmap(drawable)
                }
                val intent = withContext(Dispatchers.IO) {
                    exportUseCase.executeCreateShareIntent(bitmap)
                }
                _uiState.update { it.copy(shareIntent = intent) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "分享失败：${e.message}") }
            }
        }
    }

    /** 清除分享 Intent */
    fun executeClearShareIntent() {
        _uiState.update { it.copy(shareIntent = null) }
    }

    /** 清除错误消息 */
    fun executeClearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
