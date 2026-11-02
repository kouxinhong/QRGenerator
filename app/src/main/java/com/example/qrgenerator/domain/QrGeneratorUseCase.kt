package com.example.qrgenerator.domain

import android.content.Context
import android.graphics.drawable.Drawable
import com.example.qrgenerator.data.model.*
import com.github.alexzhirkevich.customqrgenerator.HighlightingType
import com.github.alexzhirkevich.customqrgenerator.QrData
import com.github.alexzhirkevich.customqrgenerator.QrErrorCorrectionLevel
import com.github.alexzhirkevich.customqrgenerator.QrHighlighting
import com.github.alexzhirkevich.customqrgenerator.style.BitmapScale
import com.github.alexzhirkevich.customqrgenerator.style.QrOffset
import com.github.alexzhirkevich.customqrgenerator.style.QrShape
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.QrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.*

/** 所有预览和导出都经过同一份配置映射。 */
class QrGeneratorUseCase(context: Context? = null) {
    private val imageLoader: QrImageLoader = QrImageLoader(context?.applicationContext)

    fun execute(config: QrConfig): Drawable =
        QrCodeDrawable(mapQrData(config), mapQrOptions(config), Charsets.UTF_8)

    internal fun mapQrData(config: QrConfig): QrData = QrContentCodec.encode(config)

    internal fun mapQrOptions(config: QrConfig): QrVectorOptions = QrVectorOptions.Builder()
        .setPadding(config.padding.coerceIn(0f, .5f))
        .setOffset(QrOffset(config.offsetX.coerceIn(-1f, 1f), config.offsetY.coerceIn(-1f, 1f)))
        .setCodeShape(if (config.codeShape == CodeShapeType.CIRCLE) QrShape.Circle(config.circlePadding.coerceIn(1f, 2f)) else QrShape.Default)
        .setErrorCorrectionLevel(mapErrorLevel(config.errorCorrectionLevel))
        .setFourthEyeEnabled(config.fourthEyeEnabled)
        .setShapes(QrVectorShapes(
            darkPixel = mapPixelShape(config.darkPixelShape, config),
            lightPixel = mapPixelShape(config.lightPixelShape, config),
            ball = mapBallShape(config),
            frame = mapFrameShape(config),
            centralSymmetry = config.centralSymmetry
        ))
        .setColors(QrVectorColors(
            dark = mapColor(config.darkColor),
            light = mapColor(config.lightColor),
            ball = mapColor(config.ballColor),
            frame = mapColor(config.frameColor)
        ))
        .setBackground(QrVectorBackground(
            drawable = imageLoader.loadImage(config.background.imagePath),
            scale = mapImageScale(config.background.scale),
            color = mapColor(config.background.color)
        ))
        .setLogo(mapLogo(config.logo))
        .setAnchorsHighlighting(mapHighlighting(config))
        .build()

    private fun mapLogo(config: QrLogoConfig): QrVectorLogo {
        val drawable: Drawable? = if (config.source == LogoSource.BUILTIN_ICON) {
            imageLoader.createBuiltin(config.builtinIconName)
        } else {
            imageLoader.loadImage(config.imagePath)
        }
        if (drawable == null || config.size <= 0f) return QrVectorLogo(size = 0f)
        return QrVectorLogo(
            drawable = drawable,
            size = config.size.coerceIn(0.01f, 1f / 3f),
            shape = mapLogoShape(config.shape, config.cornerRadius),
            padding = mapLogoPadding(config.paddingType, config.paddingValue),
            scale = mapImageScale(config.scale),
            backgroundColor = mapColor(config.backgroundColor)
        )
    }

    private fun mapHighlighting(config: QrConfig): QrHighlighting = QrHighlighting(
        cornerEyes = mapHighlightingMode(config.highlighting.cornerEyes, config),
        versionEyes = mapHighlightingMode(config.highlighting.versionEyes, config),
        timingLines = mapHighlightingMode(config.highlighting.timingLines, config),
        alpha = config.highlighting.alpha.coerceIn(0f, 1f)
    )

    private fun mapHighlightingMode(mode: HighlightingMode, config: QrConfig): HighlightingType = when (mode) {
        HighlightingMode.NONE -> HighlightingType.None
        HighlightingMode.DEFAULT -> HighlightingType.Default
        HighlightingMode.CUSTOM -> HighlightingType.Styled(
            shape = mapPixelShape(config.highlighting.shape, config),
            color = mapColor(config.highlighting.color)
        )
    }

    private fun mapErrorLevel(level: QrErrorLevel): QrErrorCorrectionLevel = when (level) {
        QrErrorLevel.AUTO -> QrErrorCorrectionLevel.Auto
        QrErrorLevel.LOW -> QrErrorCorrectionLevel.Low
        QrErrorLevel.MEDIUM -> QrErrorCorrectionLevel.Medium
        QrErrorLevel.MEDIUM_HIGH -> QrErrorCorrectionLevel.MediumHigh
        QrErrorLevel.HIGH -> QrErrorCorrectionLevel.High
    }

    internal fun mapPixelShape(type: QrPixelShapeType, config: QrConfig = QrConfig()): QrVectorPixelShape = when (type) {
        QrPixelShapeType.DEFAULT -> QrVectorPixelShape.Default
        QrPixelShapeType.CIRCLE -> QrVectorPixelShape.Circle(config.pixelSize.coerceIn(.5f, 1f))
        QrPixelShapeType.ROUND_CORNERS -> QrVectorPixelShape.RoundCorners(config.pixelCornerRadius.coerceIn(0f, .5f))
        QrPixelShapeType.RHOMBUS -> QrVectorPixelShape.Rhombus(config.pixelSize.coerceIn(.5f, 1f))
        QrPixelShapeType.STAR -> QrVectorPixelShape.Star
        QrPixelShapeType.ROUND_CORNERS_VERTICAL -> QrVectorPixelShape.RoundCornersVertical(config.pixelCornerRadius.coerceIn(0f, .5f) * 2f)
        QrPixelShapeType.ROUND_CORNERS_HORIZONTAL -> QrVectorPixelShape.RoundCornersHorizontal(config.pixelCornerRadius.coerceIn(0f, .5f) * 2f)
        else -> requireNotNull(CustomShapeRegistry.getPixelShape(type.name.lowercase(java.util.Locale.ROOT)))
    }

    private fun mapBallShape(config: QrConfig): QrVectorBallShape = when (config.ballShape) {
        QrBallShapeType.DEFAULT -> QrVectorBallShape.Default
        QrBallShapeType.CIRCLE -> QrVectorBallShape.Circle(config.ballSize.coerceIn(.5f, 1f))
        QrBallShapeType.ROUND_CORNERS -> QrVectorBallShape.RoundCorners(config.ballCornerRadius.coerceIn(0f, .5f))
        QrBallShapeType.RHOMBUS -> QrVectorBallShape.Rhombus(config.ballSize.coerceIn(.5f, 1f))
        QrBallShapeType.AS_DARK_PIXELS -> QrVectorBallShape.AsDarkPixels
    }

    private fun mapFrameShape(config: QrConfig): QrVectorFrameShape = when (config.frameShape) {
        QrFrameShapeType.DEFAULT -> QrVectorFrameShape.Default
        QrFrameShapeType.CIRCLE -> QrVectorFrameShape.Circle(width = config.frameWidth.coerceIn(.5f, 1.5f))
        QrFrameShapeType.ROUND_CORNERS -> QrVectorFrameShape.RoundCorners(config.frameCornerRadius.coerceIn(0f, .5f), width = config.frameWidth.coerceIn(.5f, 1.5f))
        QrFrameShapeType.AS_DARK_PIXELS -> QrVectorFrameShape.AsDarkPixels
    }

    internal fun mapColor(config: QrColorConfig): QrVectorColor = when (config.type) {
        QrColorType.SOLID -> QrVectorColor.Solid(config.solidColor)
        QrColorType.TRANSPARENT -> QrVectorColor.Transparent
        QrColorType.ERASER -> QrVectorColor.Eraser
        QrColorType.FOLLOW_DARK -> QrVectorColor.Unspecified
        QrColorType.LINEAR_GRADIENT -> QrVectorColor.LinearGradient(
            listOf(0f to config.gradientStartColor, 1f to config.gradientEndColor), mapGradientOrientation(config.gradientOrientation)
        )
        QrColorType.RADIAL_GRADIENT -> QrVectorColor.RadialGradient(
            listOf(0f to config.gradientStartColor, 1f to config.gradientEndColor), config.gradientRadius.coerceIn(.1f, 3f)
        )
        QrColorType.SWEEP_GRADIENT -> QrVectorColor.SweepGradient(
            listOf(0f to config.gradientStartColor, .5f to config.gradientEndColor, 1f to config.gradientStartColor)
        )
    }

    private fun mapGradientOrientation(orientation: GradientOrientation): QrVectorColor.LinearGradient.Orientation = when (orientation) {
        GradientOrientation.HORIZONTAL -> QrVectorColor.LinearGradient.Orientation.Horizontal
        GradientOrientation.VERTICAL -> QrVectorColor.LinearGradient.Orientation.Vertical
        GradientOrientation.LEFT_DIAGONAL -> QrVectorColor.LinearGradient.Orientation.LeftDiagonal
        GradientOrientation.RIGHT_DIAGONAL -> QrVectorColor.LinearGradient.Orientation.RightDiagonal
    }

    private fun mapLogoShape(type: LogoShapeType, radius: Float): QrVectorLogoShape = when (type) {
        LogoShapeType.DEFAULT -> QrVectorLogoShape.Default
        LogoShapeType.CIRCLE -> QrVectorLogoShape.Circle
        LogoShapeType.ROUND_CORNERS -> QrVectorLogoShape.RoundCorners(radius.coerceIn(0f, .5f))
        LogoShapeType.RHOMBUS -> QrVectorLogoShape.Rhombus
    }

    private fun mapLogoPadding(type: LogoPaddingType, value: Float): QrVectorLogoPadding = when (type) {
        LogoPaddingType.EMPTY -> QrVectorLogoPadding.Empty
        LogoPaddingType.ACCURATE -> QrVectorLogoPadding.Accurate(value.coerceIn(0f, .5f))
        LogoPaddingType.NATURAL -> QrVectorLogoPadding.Natural(value.coerceIn(0f, .5f))
    }

    private fun mapImageScale(type: ImageScaleType): BitmapScale = when (type) {
        ImageScaleType.FIT_XY -> BitmapScale.FitXY
        ImageScaleType.CENTER_CROP -> BitmapScale.CenterCrop
    }
}
