package com.example.qrgenerator.domain

import android.graphics.drawable.Drawable
import com.example.qrgenerator.data.model.GradientOrientation
import com.example.qrgenerator.data.model.LogoPaddingType
import com.example.qrgenerator.data.model.QrBallShapeType
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.data.model.QrErrorLevel
import com.example.qrgenerator.data.model.QrFrameShapeType
import com.example.qrgenerator.data.model.QrPixelShapeType
import com.github.alexzhirkevich.customqrgenerator.QrData
import com.github.alexzhirkevich.customqrgenerator.QrErrorCorrectionLevel
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.createQrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogo
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoPadding
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape

/**
 * QR 码生成用例
 * 负责将配置转换为 QR 码 Drawable
 */
class QrGeneratorUseCase {

    /**
     * 根据配置生成 QR 码 Drawable
     *
     * @param config QR 码配置
     * @param logoDrawable Logo Drawable（可选）
     * @param backgroundDrawable 背景 Drawable（可选）
     * @return QR 码 Drawable
     */
    fun execute(
        config: QrConfig,
        logoDrawable: Drawable? = null,
        backgroundDrawable: Drawable? = null
    ): Drawable {
        val qrData = createQrData(config)
        val options = createQrOptions(config, logoDrawable, backgroundDrawable)
        return QrCodeDrawable(qrData, options)
    }

    /**
     * 根据内容类型创建 QrData
     */
    private fun createQrData(config: QrConfig): QrData {
        return when (config.contentType) {
            QrContentType.TEXT -> QrData.Text(config.content)
            QrContentType.URL -> QrData.Url(config.content)
            QrContentType.WIFI -> parseWifiData(config.content)
            QrContentType.VCARD -> parseVCardData(config.content)
            QrContentType.PHONE -> QrData.Phone(config.content)
            QrContentType.SMS -> QrData.SMS(
                phoneNumber = config.content,
                subject = "",
                isMMS = false
            )
            QrContentType.EMAIL -> QrData.Email(email = config.content)
            QrContentType.GEO -> parseGeoData(config.content)
            QrContentType.BOOKMARK -> QrData.Bookmark(
                url = config.content,
                title = "书签"
            )
            QrContentType.EVENT -> QrData.Event(summary = config.content)
        }
    }

    /**
     * 解析 WiFi 数据
     * 格式: SSID/密码/加密类型
     */
    private fun parseWifiData(content: String): QrData.Wifi {
        val parts = content.split("/")
        val ssid = parts.getOrNull(0) ?: ""
        val password = parts.getOrNull(1) ?: ""
        val auth = when (parts.getOrNull(2)?.uppercase()) {
            "WEP" -> QrData.Wifi.Authentication.WEP
            "WPA" -> QrData.Wifi.Authentication.WPA
            else -> QrData.Wifi.Authentication.OPEN
        }
        return QrData.Wifi(
            authentication = auth,
            ssid = ssid,
            psk = password,
            hidden = false
        )
    }

    /**
     * 解析 VCard 数据
     * 格式: 姓名/公司/职位/电话/邮箱
     */
    private fun parseVCardData(content: String): QrData.VCard {
        val parts = content.split("/")
        return QrData.VCard(
            name = parts.getOrNull(0),
            company = parts.getOrNull(1),
            title = parts.getOrNull(2),
            phoneNumber = parts.getOrNull(3),
            email = parts.getOrNull(4)
        )
    }

    /**
     * 解析地理坐标数据
     * 格式: 纬度,经度
     */
    private fun parseGeoData(content: String): QrData.GeoPos {
        val parts = content.split(",")
        return QrData.GeoPos(
            lat = parts.getOrNull(0)?.toFloatOrNull() ?: 0f,
            lon = parts.getOrNull(1)?.toFloatOrNull() ?: 0f
        )
    }

    /**
     * 创建 QR 码样式选项
     */
    private fun createQrOptions(
        config: QrConfig,
        logoDrawable: Drawable?,
        backgroundDrawable: Drawable?
    ) = createQrVectorOptions {
        padding = config.padding

        // 容错级别
        errorCorrectionLevel = mapErrorCorrectionLevel(config.errorCorrectionLevel)

        // 启用第四只眼
        fourthEyeEnabled = config.fourthEyeEnabled

        // 模块形状
        shapes {
            darkPixel = mapPixelShape(config.pixelShape)
            ball = mapBallShape(config.ballShape)
            frame = mapFrameShape(config.frameShape)
        }

        // 颜色配置
        colors {
            dark = if (config.useGradientForeground) {
                mapGradientColor(config)
            } else {
                QrVectorColor.Solid(config.foregroundColor)
            }
            ball = QrVectorColor.Solid(config.foregroundColor)
            frame = QrVectorColor.Solid(config.foregroundColor)
        }

        // Logo 配置
        if (logoDrawable != null && config.logoSize > 0f) {
            logo {
                drawable = logoDrawable
                size = config.logoSize
                shape = if (config.logoIsCircle) {
                    QrVectorLogoShape.Circle
                } else if (config.logoCornerRadius > 0f) {
                    QrVectorLogoShape.RoundCorners(config.logoCornerRadius)
                } else {
                    QrVectorLogoShape.Default
                }
                padding = when (config.logoPaddingType) {
                    LogoPaddingType.EMPTY -> QrVectorLogoPadding.Empty
                    LogoPaddingType.ACCURATE -> QrVectorLogoPadding.Accurate(config.logoPaddingValue)
                    LogoPaddingType.NATURAL -> QrVectorLogoPadding.Natural(config.logoPaddingValue)
                }
            }
        }

        // 背景配置
        if (backgroundDrawable != null) {
            background {
                drawable = backgroundDrawable
            }
        }
    }

    /**
     * 映射容错级别
     */
    private fun mapErrorCorrectionLevel(level: QrErrorLevel): QrErrorCorrectionLevel {
        return when (level) {
            QrErrorLevel.LOW -> QrErrorCorrectionLevel.Low
            QrErrorLevel.MEDIUM -> QrErrorCorrectionLevel.Medium
            QrErrorLevel.MEDIUM_HIGH -> QrErrorCorrectionLevel.MediumHigh
            QrErrorLevel.HIGH -> QrErrorCorrectionLevel.High
        }
    }

    /**
     * 映射模块形状
     */
    private fun mapPixelShape(type: QrPixelShapeType): QrVectorPixelShape {
        return when (type) {
            QrPixelShapeType.DEFAULT -> QrVectorPixelShape.Default
            QrPixelShapeType.CIRCLE -> QrVectorPixelShape.Circle()
            QrPixelShapeType.ROUND_CORNERS -> QrVectorPixelShape.RoundCorners(0.5f)
            QrPixelShapeType.RHOMBUS -> QrVectorPixelShape.Rhombus()
            QrPixelShapeType.STAR -> QrVectorPixelShape.Star
            QrPixelShapeType.ROUND_CORNERS_VERTICAL -> QrVectorPixelShape.RoundCornersVertical()
            QrPixelShapeType.ROUND_CORNERS_HORIZONTAL -> QrVectorPixelShape.RoundCornersHorizontal()
        }
    }

    /**
     * 映射眼睛外框形状
     */
    private fun mapFrameShape(type: QrFrameShapeType): QrVectorFrameShape {
        return when (type) {
            QrFrameShapeType.DEFAULT -> QrVectorFrameShape.Default
            QrFrameShapeType.CIRCLE -> QrVectorFrameShape.Circle()
            QrFrameShapeType.ROUND_CORNERS -> QrVectorFrameShape.RoundCorners(0.25f)
            QrFrameShapeType.AS_DARK_PIXELS -> QrVectorFrameShape.AsDarkPixels
        }
    }

    /**
     * 映射眼睛内球形状
     */
    private fun mapBallShape(type: QrBallShapeType): QrVectorBallShape {
        return when (type) {
            QrBallShapeType.DEFAULT -> QrVectorBallShape.Default
            QrBallShapeType.CIRCLE -> QrVectorBallShape.Circle(1f)
            QrBallShapeType.ROUND_CORNERS -> QrVectorBallShape.RoundCorners(0.25f)
            QrBallShapeType.RHOMBUS -> QrVectorBallShape.Rhombus()
            QrBallShapeType.AS_DARK_PIXELS -> QrVectorBallShape.AsDarkPixels
        }
    }

    /**
     * 映射渐变颜色
     */
    private fun mapGradientColor(config: QrConfig): QrVectorColor.LinearGradient {
        val orientation = when (config.gradientOrientation) {
            GradientOrientation.HORIZONTAL -> QrVectorColor.LinearGradient.Orientation.Horizontal
            GradientOrientation.VERTICAL -> QrVectorColor.LinearGradient.Orientation.Vertical
            GradientOrientation.LEFT_DIAGONAL -> QrVectorColor.LinearGradient.Orientation.LeftDiagonal
            GradientOrientation.RIGHT_DIAGONAL -> QrVectorColor.LinearGradient.Orientation.RightDiagonal
        }
        return QrVectorColor.LinearGradient(
            colors = listOf(
                0f to config.gradientStartColor,
                1f to config.gradientEndColor
            ),
            orientation = orientation
        )
    }
}
