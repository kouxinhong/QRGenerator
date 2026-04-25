package com.example.qrgenerator.domain

import android.graphics.drawable.Drawable
import com.example.qrgenerator.data.model.*
import com.github.alexzhirkevich.customqrgenerator.QrData
import com.github.alexzhirkevich.customqrgenerator.QrErrorCorrectionLevel
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.createQrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.*

class QrGeneratorUseCase {
    fun execute(config: QrConfig): Drawable {
        val qrData = mapQrData(config)
        val options = mapQrOptions(config)
        return QrCodeDrawable(qrData, options)
    }

    internal fun mapQrData(config: QrConfig): QrData = when (config.contentType) {
        QrContentType.TEXT -> QrData.Text(config.content)
        QrContentType.URL -> QrData.Url(config.content)
        QrContentType.WIFI -> parseWifi(config.content)
        QrContentType.ENTERPRISE_WIFI -> parseEnterpriseWifi(config.content)
        QrContentType.VCARD -> parseVCard(config.content)
        QrContentType.BIZCARD -> parseBizCard(config.content)
        QrContentType.MECARD -> parseMeCard(config.content)
        QrContentType.PHONE -> QrData.Phone(config.content)
        QrContentType.SMS -> QrData.SMS(config.content, "", false)
        QrContentType.EMAIL -> QrData.Email(email = config.content)
        QrContentType.GEO -> parseGeo(config.content)
        QrContentType.BOOKMARK -> QrData.Bookmark(config.content, "书签")
        QrContentType.EVENT -> QrData.Event(summary = config.content)
        QrContentType.YOUTUBE -> QrData.YouTube(config.content)
        QrContentType.GOOGLE_PLAY -> QrData.GooglePlay(config.content)
    }

    private fun mapQrOptions(config: QrConfig) = createQrVectorOptions {
        padding = config.padding
        errorCorrectionLevel = mapErrorLevel(config.errorCorrectionLevel)
        fourthEyeEnabled = config.fourthEyeEnabled
        shapes {
            darkPixel = mapPixelShape(config.darkPixelShape)
            lightPixel = mapPixelShape(config.lightPixelShape)
            ball = mapBallShape(config.ballShape)
            frame = mapFrameShape(config.frameShape)
            centralSymmetry = config.centralSymmetry
        }
        colors {
            dark = mapColor(config.darkColor)
            ball = mapColor(config.ballColor)
            frame = mapColor(config.frameColor)
        }
        if (config.logo.imagePath != null && config.logo.size > 0f) {
            logo {
                size = config.logo.size
                shape = mapLogoShape(config.logo.shape, config.logo.cornerRadius)
                padding = mapLogoPadding(config.logo.paddingType, config.logo.paddingValue)
            }
        }
    }

    private fun mapErrorLevel(level: QrErrorLevel): QrErrorCorrectionLevel = when (level) {
        QrErrorLevel.AUTO -> QrErrorCorrectionLevel.Auto
        QrErrorLevel.LOW -> QrErrorCorrectionLevel.Low
        QrErrorLevel.MEDIUM -> QrErrorCorrectionLevel.Medium
        QrErrorLevel.MEDIUM_HIGH -> QrErrorCorrectionLevel.MediumHigh
        QrErrorLevel.HIGH -> QrErrorCorrectionLevel.High
    }

    private fun mapPixelShape(type: QrPixelShapeType): QrVectorPixelShape = when (type) {
        QrPixelShapeType.DEFAULT -> QrVectorPixelShape.Default
        QrPixelShapeType.CIRCLE -> QrVectorPixelShape.Circle()
        QrPixelShapeType.ROUND_CORNERS -> QrVectorPixelShape.RoundCorners(0.5f)
        QrPixelShapeType.RHOMBUS -> QrVectorPixelShape.Rhombus()
        QrPixelShapeType.STAR -> QrVectorPixelShape.Star
        QrPixelShapeType.ROUND_CORNERS_VERTICAL -> QrVectorPixelShape.RoundCornersVertical()
        QrPixelShapeType.ROUND_CORNERS_HORIZONTAL -> QrVectorPixelShape.RoundCornersHorizontal()
        QrPixelShapeType.HONEYCOMB -> CustomShapeRegistry.getPixelShape("honeycomb") ?: QrVectorPixelShape.Default
        QrPixelShapeType.CAT_PAW -> CustomShapeRegistry.getPixelShape("cat_paw") ?: QrVectorPixelShape.Default
    }

    private fun mapBallShape(type: QrBallShapeType): QrVectorBallShape = when (type) {
        QrBallShapeType.DEFAULT -> QrVectorBallShape.Default
        QrBallShapeType.CIRCLE -> QrVectorBallShape.Circle(1f)
        QrBallShapeType.ROUND_CORNERS -> QrVectorBallShape.RoundCorners(0.25f)
        QrBallShapeType.RHOMBUS -> QrVectorBallShape.Rhombus()
        QrBallShapeType.AS_DARK_PIXELS -> QrVectorBallShape.AsDarkPixels
    }

    private fun mapFrameShape(type: QrFrameShapeType): QrVectorFrameShape = when (type) {
        QrFrameShapeType.DEFAULT -> QrVectorFrameShape.Default
        QrFrameShapeType.CIRCLE -> QrVectorFrameShape.Circle()
        QrFrameShapeType.ROUND_CORNERS -> QrVectorFrameShape.RoundCorners(0.25f)
        QrFrameShapeType.AS_DARK_PIXELS -> QrVectorFrameShape.AsDarkPixels
    }

    private fun mapColor(config: QrColorConfig): QrVectorColor = when (config.type) {
        QrColorType.SOLID -> QrVectorColor.Solid(config.solidColor)
        QrColorType.LINEAR_GRADIENT -> {
            val orientation = when (config.gradientOrientation) {
                GradientOrientation.HORIZONTAL -> QrVectorColor.LinearGradient.Orientation.Horizontal
                GradientOrientation.VERTICAL -> QrVectorColor.LinearGradient.Orientation.Vertical
                GradientOrientation.LEFT_DIAGONAL -> QrVectorColor.LinearGradient.Orientation.LeftDiagonal
                GradientOrientation.RIGHT_DIAGONAL -> QrVectorColor.LinearGradient.Orientation.RightDiagonal
            }
            QrVectorColor.LinearGradient(listOf(0f to config.gradientStartColor, 1f to config.gradientEndColor), orientation)
        }
        QrColorType.RADIAL_GRADIENT -> QrVectorColor.RadialGradient(listOf(0f to config.gradientStartColor, 1f to config.gradientEndColor), config.gradientRadius)
        QrColorType.SWEEP_GRADIENT -> QrVectorColor.SweepGradient(listOf(0f to config.gradientStartColor, 1f to config.gradientEndColor))
    }

    private fun mapLogoShape(type: LogoShapeType, radius: Float): QrVectorLogoShape = when (type) {
        LogoShapeType.DEFAULT -> QrVectorLogoShape.Default
        LogoShapeType.CIRCLE -> QrVectorLogoShape.Circle
        LogoShapeType.ROUND_CORNERS -> QrVectorLogoShape.RoundCorners(radius)
        LogoShapeType.RHOMBUS -> QrVectorLogoShape.Rhombus
    }

    private fun mapLogoPadding(type: LogoPaddingType, value: Float): QrVectorLogoPadding = when (type) {
        LogoPaddingType.EMPTY -> QrVectorLogoPadding.Empty
        LogoPaddingType.ACCURATE -> QrVectorLogoPadding.Accurate(value)
        LogoPaddingType.NATURAL -> QrVectorLogoPadding.Natural(value)
    }

    private fun parseWifi(content: String): QrData.Wifi {
        val parts = content.split("/")
        return QrData.Wifi(
            authentication = when (parts.getOrNull(2)?.uppercase()) { "WEP" -> QrData.Wifi.Authentication.WEP; "WPA" -> QrData.Wifi.Authentication.WPA; else -> QrData.Wifi.Authentication.OPEN },
            ssid = parts.getOrNull(0), psk = parts.getOrNull(1), hidden = false
        )
    }

    private fun parseEnterpriseWifi(content: String): QrData.EnterpriseWifi {
        val parts = content.split("/")
        return QrData.EnterpriseWifi(ssid = parts.getOrNull(0), psk = parts.getOrNull(1), user = parts.getOrNull(2), eap = parts.getOrNull(3))
    }

    private fun parseVCard(content: String): QrData.VCard {
        val parts = content.split("/")
        return QrData.VCard(name = parts.getOrNull(0), company = parts.getOrNull(1), title = parts.getOrNull(2), phoneNumber = parts.getOrNull(3), email = parts.getOrNull(4))
    }

    private fun parseBizCard(content: String): QrData.BizCard {
        val parts = content.split("/")
        return QrData.BizCard(firstName = parts.getOrNull(0), secondName = parts.getOrNull(1), job = parts.getOrNull(2), company = parts.getOrNull(3), phone = parts.getOrNull(4), email = parts.getOrNull(5))
    }

    private fun parseMeCard(content: String): QrData.MeCard {
        val parts = content.split("/")
        return QrData.MeCard(name = parts.getOrNull(0), phoneNumber = parts.getOrNull(1), email = parts.getOrNull(2))
    }

    private fun parseGeo(content: String): QrData.GeoPos {
        val parts = content.split(",")
        return QrData.GeoPos(lat = parts.getOrNull(0)?.toFloatOrNull() ?: 0f, lon = parts.getOrNull(1)?.toFloatOrNull() ?: 0f)
    }
}
