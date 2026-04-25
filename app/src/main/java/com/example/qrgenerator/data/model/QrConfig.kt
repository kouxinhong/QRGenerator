package com.example.qrgenerator.data.model

import android.graphics.Color

/** 颜色配置 */
data class QrColorConfig(
    val type: QrColorType = QrColorType.SOLID,
    val solidColor: Int = Color.BLACK,
    val gradientStartColor: Int = Color.BLACK,
    val gradientEndColor: Int = Color.WHITE,
    val gradientOrientation: GradientOrientation = GradientOrientation.HORIZONTAL,
    val gradientRadius: Float = 1.414f
)

/** Logo 配置 */
data class QrLogoConfig(
    val source: LogoSource = LogoSource.GALLERY,
    val imagePath: String? = null,
    val builtinIconName: String? = null,
    val size: Float = 0.2f,
    val shape: LogoShapeType = LogoShapeType.DEFAULT,
    val cornerRadius: Float = 0f,
    val paddingType: LogoPaddingType = LogoPaddingType.EMPTY,
    val paddingValue: Float = 0f,
    val backgroundColor: QrColorConfig = QrColorConfig()
)

/** 高亮配置 */
data class QrHighlightingConfig(
    val cornerEyes: HighlightingMode = HighlightingMode.NONE,
    val versionEyes: HighlightingMode = HighlightingMode.NONE,
    val timingLines: HighlightingMode = HighlightingMode.NONE,
    val alpha: Float = 0.75f
)

/** QR 码完整配置 */
data class QrConfig(
    val contentType: QrContentType = QrContentType.TEXT,
    val content: String = "",
    val padding: Float = 0.125f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val errorCorrectionLevel: QrErrorLevel = QrErrorLevel.AUTO,
    val darkPixelShape: QrPixelShapeType = QrPixelShapeType.DEFAULT,
    val lightPixelShape: QrPixelShapeType = QrPixelShapeType.DEFAULT,
    val ballShape: QrBallShapeType = QrBallShapeType.DEFAULT,
    val frameShape: QrFrameShapeType = QrFrameShapeType.DEFAULT,
    val centralSymmetry: Boolean = true,
    val darkColor: QrColorConfig = QrColorConfig(),
    val ballColor: QrColorConfig = QrColorConfig(),
    val frameColor: QrColorConfig = QrColorConfig(),
    val codeShape: CodeShapeType = CodeShapeType.DEFAULT,
    val logo: QrLogoConfig = QrLogoConfig(),
    val highlighting: QrHighlightingConfig = QrHighlightingConfig(),
    val fourthEyeEnabled: Boolean = false,
    val imageWidth: Int = 1024,
    val imageHeight: Int = 1024
)
