package com.example.qrgenerator.ui.screens.generate

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.domain.QrGeneratorUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 生成页面 UI 状态 */
data class GenerateUiState(
    val config: QrConfig = QrConfig(),
    val qrDrawable: Drawable? = null,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null
)

/** 生成页面 ViewModel */
class GenerateViewModel(private val qrGeneratorUseCase: QrGeneratorUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(GenerateUiState())
    val uiState: StateFlow<GenerateUiState> = _uiState.asStateFlow()

    private var generateJob: Job? = null

    /** 更新配置并触发实时预览 */
    fun executeUpdateConfig(newConfig: QrConfig) {
        _uiState.update { it.copy(config = newConfig) }
        executeDebounceGenerate()
    }

    /** 更新内容类型 */
    fun executeUpdateContentType(type: QrContentType) {
        _uiState.update { it.copy(config = it.config.copy(contentType = type, content = "")) }
    }

    /** 更新内容文本 */
    fun executeUpdateContent(content: String) {
        _uiState.update { it.copy(config = it.config.copy(content = content)) }
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
        try {
            val drawable = qrGeneratorUseCase.execute(config)
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

    /** 清除错误消息 */
    fun executeClearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
