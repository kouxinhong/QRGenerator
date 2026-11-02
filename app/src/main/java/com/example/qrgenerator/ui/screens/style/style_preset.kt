package com.example.qrgenerator.ui.screens.style

import com.example.qrgenerator.data.model.*

/** 成套样式保持主色足够深，独立设置和默认配置仍可随时调整。 */
enum class StylePreset(val title: String, val pixel: QrPixelShapeType, val color: Int, val description: String) {
    CLASSIC("清晰黑白", QrPixelShapeType.DEFAULT, 0xFF191919.toInt(), "简单、清楚，适合打印"),
    BAMBOO("节节青竹", QrPixelShapeType.BAMBOO, 0xFF176247.toInt(), "像竹节一样连接的纹理"),
    KITE("春日纸鸢", QrPixelShapeType.KITE, 0xFF214F83.toInt(), "留住一只小风筝的轮廓"),
    PETAL("小小花窗", QrPixelShapeType.PETAL, 0xFF73334F.toInt(), "把窗花装进小方格"),
    RIPPLE("池塘涟漪", QrPixelShapeType.RIPPLE, 0xFF225C68.toInt(), "圆润的水纹小印记");

    fun applyTo(current: QrConfig): QrConfig {
        val colors = QrColorConfig(solidColor = color)
        return current.copy(
            darkPixelShape = pixel,
            ballShape = if (this == CLASSIC) QrBallShapeType.DEFAULT else QrBallShapeType.ROUND_CORNERS,
            frameShape = if (this == CLASSIC) QrFrameShapeType.DEFAULT else QrFrameShapeType.ROUND_CORNERS,
            darkColor = colors,
            frameColor = colors,
            ballColor = colors,
            errorCorrectionLevel = QrErrorLevel.HIGH
        )
    }

    /** 只比较本预设控制的字段，用户的图片和留白不影响选中状态。 */
    fun isAppliedTo(config: QrConfig): Boolean = applyTo(config) == config
}
