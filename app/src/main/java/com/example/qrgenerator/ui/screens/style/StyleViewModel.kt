package com.example.qrgenerator.ui.screens.style

import androidx.lifecycle.ViewModel
import com.example.qrgenerator.data.model.*
import com.example.qrgenerator.domain.QrConfigHolder
import kotlinx.coroutines.flow.StateFlow

/** 样式页面 ViewModel — 通过 QrConfigHolder 与 GenerateViewModel 共享配置 */
class StyleViewModel : ViewModel() {

    val config: StateFlow<QrConfig> = QrConfigHolder.config

    fun executeUpdatePixelShape(shape: QrPixelShapeType) {
        QrConfigHolder.executeUpdateConfig { it.copy(darkPixelShape = shape) }
    }

    fun executeUpdateLightPixelShape(shape: QrPixelShapeType) {
        QrConfigHolder.executeUpdateConfig { it.copy(lightPixelShape = shape) }
    }

    fun executeUpdateBallShape(shape: QrBallShapeType) {
        QrConfigHolder.executeUpdateConfig { it.copy(ballShape = shape) }
    }

    fun executeUpdateFrameShape(shape: QrFrameShapeType) {
        QrConfigHolder.executeUpdateConfig { it.copy(frameShape = shape) }
    }

    fun executeUpdateDarkColor(color: QrColorConfig) {
        QrConfigHolder.executeUpdateConfig { it.copy(darkColor = color) }
    }

    fun executeUpdateBallColor(color: QrColorConfig) {
        QrConfigHolder.executeUpdateConfig { it.copy(ballColor = color) }
    }

    fun executeUpdateFrameColor(color: QrColorConfig) {
        QrConfigHolder.executeUpdateConfig { it.copy(frameColor = color) }
    }

    fun executeUpdateCodeShape(shape: CodeShapeType) {
        QrConfigHolder.executeUpdateConfig { it.copy(codeShape = shape) }
    }

    fun executeUpdateErrorLevel(level: QrErrorLevel) {
        QrConfigHolder.executeUpdateConfig { it.copy(errorCorrectionLevel = level) }
    }

    fun executeUpdateFourthEye(enabled: Boolean) {
        QrConfigHolder.executeUpdateConfig { it.copy(fourthEyeEnabled = enabled) }
    }

    fun executeUpdateCentralSymmetry(enabled: Boolean) {
        QrConfigHolder.executeUpdateConfig { it.copy(centralSymmetry = enabled) }
    }

    fun executeUpdateHighlighting(highlighting: QrHighlightingConfig) {
        QrConfigHolder.executeUpdateConfig { it.copy(highlighting = highlighting) }
    }

    fun executeUpdateLogo(logo: QrLogoConfig) {
        QrConfigHolder.executeUpdateConfig { it.copy(logo = logo) }
    }
}
