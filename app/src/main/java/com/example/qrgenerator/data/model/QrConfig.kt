package com.example.qrgenerator.data.model

import android.graphics.Color

/** 颜色配置 */
data class QrColorConfig(
    val type: QrColorType = QrColorType.SOLID,
    val solidColor: Int = Color.BLACK,
    val gradientStartColor: Int = Color.BLACK,
    val gradientEndColor: Int = 0xFF075E46.toInt(),
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
    val backgroundColor: QrColorConfig = QrColorConfig(solidColor = Color.WHITE),
    val scale: ImageScaleType = ImageScaleType.CENTER_CROP
)

/** 图片和底色 */
data class QrBackgroundConfig(
    val imagePath: String? = null,
    val color: QrColorConfig = QrColorConfig(solidColor = Color.WHITE),
    val scale: ImageScaleType = ImageScaleType.CENTER_CROP
)

/** 高亮配置 */
data class QrHighlightingConfig(
    val cornerEyes: HighlightingMode = HighlightingMode.NONE,
    val versionEyes: HighlightingMode = HighlightingMode.NONE,
    val timingLines: HighlightingMode = HighlightingMode.NONE,
    val alpha: Float = 0.75f,
    val color: QrColorConfig = QrColorConfig(solidColor = Color.WHITE),
    val shape: QrPixelShapeType = QrPixelShapeType.DEFAULT
)

/** QR 码完整配置 */
data class QrConfig(
    // 内容
    val contentType: QrContentType = QrContentType.TEXT,
    val content: String = "",
    // 模块形状
    val darkPixelShape: QrPixelShapeType = QrPixelShapeType.DEFAULT,
    val lightPixelShape: QrPixelShapeType = QrPixelShapeType.DEFAULT,
    val ballShape: QrBallShapeType = QrBallShapeType.DEFAULT,
    val frameShape: QrFrameShapeType = QrFrameShapeType.DEFAULT,
    val codeShape: CodeShapeType = CodeShapeType.DEFAULT,
    val centralSymmetry: Boolean = true,
    val pixelSize: Float = 1f,
    val pixelCornerRadius: Float = 0.5f,
    val ballSize: Float = 1f,
    val ballCornerRadius: Float = 0.25f,
    val frameWidth: Float = 1f,
    val frameCornerRadius: Float = 0.25f,
    val circlePadding: Float = 1.1f,
    // 颜色
    val darkColor: QrColorConfig = QrColorConfig(),
    val lightColor: QrColorConfig = QrColorConfig(solidColor = Color.WHITE),
    val background: QrBackgroundConfig = QrBackgroundConfig(),
    val ballColor: QrColorConfig = QrColorConfig(),
    val frameColor: QrColorConfig = QrColorConfig(),
    // 渲染参数
    // 库按总宽度计算留白；.28 为最小版本保留约四格白边。
    val padding: Float = 0.28f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val errorCorrectionLevel: QrErrorLevel = QrErrorLevel.AUTO,
    val fourthEyeEnabled: Boolean = false,
    val imageWidth: Int = 1024,
    val imageHeight: Int = 1024,
    // Logo 与高亮
    val logo: QrLogoConfig = QrLogoConfig(),
    val highlighting: QrHighlightingConfig = QrHighlightingConfig(),
    // 卡面文字独立于二维码内容，历史恢复时一并保留。
    val card: QrCardConfig = QrCardConfig()
)
