package com.example.qrgenerator.ui.screens.style

import androidx.lifecycle.ViewModel
import com.example.qrgenerator.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 样式页面 ViewModel */
class StyleViewModel : ViewModel() {

    private val _config = MutableStateFlow(QrConfig())
    val config: StateFlow<QrConfig> = _config.asStateFlow()

    fun executeUpdatePixelShape(shape: QrPixelShapeType) {
        _config.update { it.copy(darkPixelShape = shape) }
    }

    fun executeUpdateBallShape(shape: QrBallShapeType) {
        _config.update { it.copy(ballShape = shape) }
    }

    fun executeUpdateFrameShape(shape: QrFrameShapeType) {
        _config.update { it.copy(frameShape = shape) }
    }

    fun executeUpdateDarkColor(color: QrColorConfig) {
        _config.update { it.copy(darkColor = color) }
    }

    fun executeUpdateBallColor(color: QrColorConfig) {
        _config.update { it.copy(ballColor = color) }
    }

    fun executeUpdateFrameColor(color: QrColorConfig) {
        _config.update { it.copy(frameColor = color) }
    }

    fun executeUpdateCodeShape(shape: CodeShapeType) {
        _config.update { it.copy(codeShape = shape) }
    }

    fun executeUpdateErrorLevel(level: QrErrorLevel) {
        _config.update { it.copy(errorCorrectionLevel = level) }
    }

    fun executeUpdateFourthEye(enabled: Boolean) {
        _config.update { it.copy(fourthEyeEnabled = enabled) }
    }

    fun executeUpdateCentralSymmetry(enabled: Boolean) {
        _config.update { it.copy(centralSymmetry = enabled) }
    }

    fun executeUpdateHighlighting(highlighting: QrHighlightingConfig) {
        _config.update { it.copy(highlighting = highlighting) }
    }

    fun executeUpdateLogo(logo: QrLogoConfig) {
        _config.update { it.copy(logo = logo) }
    }
}
