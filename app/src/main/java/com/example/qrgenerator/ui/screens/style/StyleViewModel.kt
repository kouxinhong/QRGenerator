package com.example.qrgenerator.ui.screens.style

import androidx.lifecycle.ViewModel
import com.example.qrgenerator.data.model.*
import com.example.qrgenerator.domain.QrConfigHolder
import kotlinx.coroutines.flow.StateFlow

/** 样式页面 ViewModel — 通过 QrConfigHolder 与 GenerateViewModel 共享配置 */
class StyleViewModel(
    private val qrConfigHolder: QrConfigHolder
) : ViewModel() {

    val config: StateFlow<QrConfig> = qrConfigHolder.config

    fun executeUpdatePixelShape(shape: QrPixelShapeType) {
        qrConfigHolder.executeUpdateConfig { it.copy(darkPixelShape = shape) }
    }

    fun executeUpdateLightPixelShape(shape: QrPixelShapeType) {
        qrConfigHolder.executeUpdateConfig { it.copy(lightPixelShape = shape) }
    }

    fun executeUpdateBallShape(shape: QrBallShapeType) {
        qrConfigHolder.executeUpdateConfig { it.copy(ballShape = shape) }
    }

    fun executeUpdateFrameShape(shape: QrFrameShapeType) {
        qrConfigHolder.executeUpdateConfig { it.copy(frameShape = shape) }
    }

    fun executeUpdateDarkColor(color: QrColorConfig) {
        qrConfigHolder.executeUpdateConfig { it.copy(darkColor = color) }
    }

    fun executeUpdateBallColor(color: QrColorConfig) {
        qrConfigHolder.executeUpdateConfig { it.copy(ballColor = color) }
    }

    fun executeUpdateFrameColor(color: QrColorConfig) {
        qrConfigHolder.executeUpdateConfig { it.copy(frameColor = color) }
    }

    fun executeUpdateCodeShape(shape: CodeShapeType) {
        qrConfigHolder.executeUpdateConfig { it.copy(codeShape = shape) }
    }

    fun executeUpdateErrorLevel(level: QrErrorLevel) {
        qrConfigHolder.executeUpdateConfig { it.copy(errorCorrectionLevel = level) }
    }

    fun executeUpdateFourthEye(enabled: Boolean) {
        qrConfigHolder.executeUpdateConfig { it.copy(fourthEyeEnabled = enabled) }
    }

    fun executeUpdateCentralSymmetry(enabled: Boolean) {
        qrConfigHolder.executeUpdateConfig { it.copy(centralSymmetry = enabled) }
    }

    fun executeUpdateHighlighting(highlighting: QrHighlightingConfig) {
        qrConfigHolder.executeUpdateConfig { it.copy(highlighting = highlighting) }
    }

    fun executeUpdateLogo(logo: QrLogoConfig) {
        qrConfigHolder.executeUpdateConfig { it.copy(logo = logo) }
    }
}
