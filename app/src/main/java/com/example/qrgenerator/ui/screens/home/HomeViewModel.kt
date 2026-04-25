package com.example.qrgenerator.ui.screens.home

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrgenerator.data.model.QrBallShapeType
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.data.model.QrErrorLevel
import com.example.qrgenerator.data.model.QrFrameShapeType
import com.example.qrgenerator.data.model.QrPixelShapeType
import com.example.qrgenerator.domain.QrGeneratorUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel
 * 管理二维码的配置状态和生成逻辑
 */
class HomeViewModel : ViewModel() {

    /** QR 码生成用例 */
    private val qrGeneratorUseCase = QrGeneratorUseCase()

    /** UI 状态 */
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** 更新内容类型 */
    fun executeUpdateContentType(type: QrContentType) {
        _uiState.update { it.copy(contentType = type) }
        executeGenerateQrCode()
    }

    /** 更新内容文本 */
    fun executeUpdateContent(content: String) {
        _uiState.update { it.copy(content = content) }
    }

    /** 更新容错级别 */
    fun executeUpdateErrorLevel(level: QrErrorLevel) {
        _uiState.update { it.copy(errorCorrectionLevel = level) }
        executeGenerateQrCode()
    }

    /** 更新模块形状 */
    fun executeUpdatePixelShape(shape: QrPixelShapeType) {
        _uiState.update { it.copy(pixelShape = shape) }
        executeGenerateQrCode()
    }

    /** 更新眼睛外框形状 */
    fun executeUpdateFrameShape(shape: QrFrameShapeType) {
        _uiState.update { it.copy(frameShape = shape) }
        executeGenerateQrCode()
    }

    /** 更新眼睛内球形状 */
    fun executeUpdateBallShape(shape: QrBallShapeType) {
        _uiState.update { it.copy(ballShape = shape) }
        executeGenerateQrCode()
    }

    /** 更新前景色 */
    fun executeUpdateForegroundColor(color: Int) {
        _uiState.update { it.copy(foregroundColor = color) }
        executeGenerateQrCode()
    }

    /** 更新背景色 */
    fun executeUpdateBackgroundColor(color: Int) {
        _uiState.update { it.copy(backgroundColor = color) }
        executeGenerateQrCode()
    }

    /** 更新内边距 */
    fun executeUpdatePadding(padding: Float) {
        _uiState.update { it.copy(padding = padding) }
        executeGenerateQrCode()
    }

    /** 生成二维码 */
    fun executeGenerateQrCode() {
        val currentState = _uiState.value
        if (currentState.content.isBlank()) {
            _uiState.update { it.copy(qrDrawable = null, isGenerating = false) }
            return
        }

        _uiState.update { it.copy(isGenerating = true) }

        viewModelScope.launch {
            try {
                val config = QrConfig(
                    contentType = currentState.contentType,
                    content = currentState.content,
                    padding = currentState.padding,
                    errorCorrectionLevel = currentState.errorCorrectionLevel,
                    pixelShape = currentState.pixelShape,
                    frameShape = currentState.frameShape,
                    ballShape = currentState.ballShape,
                    foregroundColor = currentState.foregroundColor,
                    backgroundColor = currentState.backgroundColor
                )
                val drawable = qrGeneratorUseCase.execute(config)
                _uiState.update { it.copy(qrDrawable = drawable, isGenerating = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(qrDrawable = null, isGenerating = false) }
            }
        }
    }
}

/**
 * 首页 UI 状态
 */
data class HomeUiState(
    /** 内容类型 */
    val contentType: QrContentType = QrContentType.TEXT,
    /** 内容文本 */
    val content: String = "",
    /** 容错级别 */
    val errorCorrectionLevel: QrErrorLevel = QrErrorLevel.MEDIUM,
    /** 模块形状 */
    val pixelShape: QrPixelShapeType = QrPixelShapeType.DEFAULT,
    /** 眼睛外框形状 */
    val frameShape: QrFrameShapeType = QrFrameShapeType.DEFAULT,
    /** 眼睛内球形状 */
    val ballShape: QrBallShapeType = QrBallShapeType.DEFAULT,
    /** 前景色 */
    val foregroundColor: Int = android.graphics.Color.BLACK,
    /** 背景色 */
    val backgroundColor: Int = android.graphics.Color.WHITE,
    /** 内边距 */
    val padding: Float = 0.125f,
    /** 生成的 QR Drawable */
    val qrDrawable: Drawable? = null,
    /** 是否正在生成 */
    val isGenerating: Boolean = false
)
