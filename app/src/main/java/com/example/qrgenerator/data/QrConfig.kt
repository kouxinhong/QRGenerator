package com.example.qrgenerator.data

import android.graphics.Color

/**
 * 二维码配置数据类
 * 包含二维码的所有可定制参数
 */
data class QrConfig(
    /** 内容类型 */
    val contentType: QrContentType = QrContentType.TEXT,
    /** 编码内容 */
    val content: String = "",
    /** 内边距比例 (0.0 ~ 0.5) */
    val padding: Float = 0.125f,
    /** 容错级别 */
    val errorCorrectionLevel: QrErrorLevel = QrErrorLevel.MEDIUM,
    /** 模块形状 */
    val pixelShape: QrPixelShapeType = QrPixelShapeType.DEFAULT,
    /** 眼睛外框形状 */
    val frameShape: QrFrameShapeType = QrFrameShapeType.DEFAULT,
    /** 眼睛内球形状 */
    val ballShape: QrBallShapeType = QrBallShapeType.DEFAULT,
    /** 前景色 */
    val foregroundColor: Int = Color.BLACK,
    /** 背景色 */
    val backgroundColor: Int = Color.WHITE,
    /** 是否使用渐变色前景 */
    val useGradientForeground: Boolean = false,
    /** 渐变起始色 */
    val gradientStartColor: Int = Color.BLACK,
    /** 渐变结束色 */
    val gradientEndColor: Int = Color.parseColor("#FF6900"),
    /** 渐变方向 */
    val gradientOrientation: GradientOrientation = GradientOrientation.HORIZONTAL,
    /** Logo 大小比例 (0.0 ~ 0.3) */
    val logoSize: Float = 0f,
    /** Logo 圆角 */
    val logoCornerRadius: Float = 0f,
    /** Logo 是否圆形 */
    val logoIsCircle: Boolean = false,
    /** Logo 内边距类型 */
    val logoPaddingType: LogoPaddingType = LogoPaddingType.EMPTY,
    /** Logo 内边距值 */
    val logoPaddingValue: Float = 0f,
    /** 是否启用第四只眼 */
    val fourthEyeEnabled: Boolean = false,
    /** 是否启用锚点高亮 */
    val anchorsHighlighting: Boolean = false,
    /** 生成图片宽度 */
    val imageWidth: Int = 1024,
    /** 生成图片高度 */
    val imageHeight: Int = 1024
)

/**
 * 容错级别
 */
enum class QrErrorLevel(val displayName: String, val level: Int) {
    LOW("低 (7%)", 0),
    MEDIUM("中 (15%)", 1),
    MEDIUM_HIGH("较高 (25%)", 2),
    HIGH("高 (30%)", 3);
}

/**
 * 模块形状类型
 */
enum class QrPixelShapeType(val displayName: String) {
    DEFAULT("默认方形"),
    CIRCLE("圆形"),
    ROUND_CORNERS("圆角方形"),
    RHOMBUS("菱形"),
    STAR("星形"),
    ROUND_CORNERS_VERTICAL("纵向圆角"),
    ROUND_CORNERS_HORIZONTAL("横向圆角");
}

/**
 * 眼睛外框形状类型
 */
enum class QrFrameShapeType(val displayName: String) {
    DEFAULT("默认方形"),
    CIRCLE("圆形"),
    ROUND_CORNERS("圆角方形"),
    AS_DARK_PIXELS("跟随模块样式");
}

/**
 * 眼睛内球形状类型
 */
enum class QrBallShapeType(val displayName: String) {
    DEFAULT("默认方形"),
    CIRCLE("圆形"),
    ROUND_CORNERS("圆角方形"),
    RHOMBUS("菱形"),
    AS_DARK_PIXELS("跟随模块样式");
}

/**
 * 渐变方向
 */
enum class GradientOrientation(val displayName: String) {
    HORIZONTAL("水平"),
    VERTICAL("垂直"),
    LEFT_DIAGONAL("左对角线"),
    RIGHT_DIAGONAL("右对角线");
}

/**
 * Logo 内边距类型
 */
enum class LogoPaddingType(val displayName: String) {
    EMPTY("无边距"),
    ACCURATE("精确"),
    NATURAL("自然");
}
