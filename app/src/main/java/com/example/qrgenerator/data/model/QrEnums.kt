package com.example.qrgenerator.data.model

/** 容错级别 */
enum class QrErrorLevel(val displayName: String) {
    AUTO("自动"),
    LOW("低 (7%)"),
    MEDIUM("中 (15%)"),
    MEDIUM_HIGH("较高 (25%)"),
    HIGH("高 (30%)");
}

/** 模块形状类型 */
enum class QrPixelShapeType(val displayName: String) {
    DEFAULT("默认方形"),
    CIRCLE("圆形"),
    ROUND_CORNERS("圆角方形"),
    RHOMBUS("菱形"),
    STAR("星形"),
    ROUND_CORNERS_VERTICAL("纵向圆角"),
    ROUND_CORNERS_HORIZONTAL("横向圆角"),
    HONEYCOMB("蜂巢"),
    CAT_PAW("猫爪");
}

/** 眼睛外框形状类型 */
enum class QrFrameShapeType(val displayName: String) {
    DEFAULT("默认方形"),
    CIRCLE("圆形"),
    ROUND_CORNERS("圆角方形"),
    AS_DARK_PIXELS("跟随模块");
}

/** 眼睛内球形状类型 */
enum class QrBallShapeType(val displayName: String) {
    DEFAULT("默认方形"),
    CIRCLE("圆形"),
    ROUND_CORNERS("圆角方形"),
    RHOMBUS("菱形"),
    AS_DARK_PIXELS("跟随模块");
}

/** 颜色类型 */
enum class QrColorType(val displayName: String) {
    SOLID("纯色"),
    LINEAR_GRADIENT("线性渐变"),
    RADIAL_GRADIENT("径向渐变"),
    SWEEP_GRADIENT("扫描渐变");
}

/** 渐变方向 */
enum class GradientOrientation(val displayName: String) {
    HORIZONTAL("水平"),
    VERTICAL("垂直"),
    LEFT_DIAGONAL("左对角线"),
    RIGHT_DIAGONAL("右对角线");
}

/** Logo 来源 */
enum class LogoSource(val displayName: String) {
    GALLERY("相册"),
    CAMERA("拍照"),
    BUILTIN_ICON("内置图标");
}

/** Logo 形状 */
enum class LogoShapeType(val displayName: String) {
    DEFAULT("默认方形"),
    CIRCLE("圆形"),
    ROUND_CORNERS("圆角方形"),
    RHOMBUS("菱形");
}

/** Logo 内边距类型 */
enum class LogoPaddingType(val displayName: String) {
    EMPTY("无边距"),
    ACCURATE("精确"),
    NATURAL("自然");
}

/** 整体外形 */
enum class CodeShapeType(val displayName: String) {
    DEFAULT("方形"),
    CIRCLE("圆形");
}

/** 高亮模式 */
enum class HighlightingMode(val displayName: String) {
    NONE("关闭"),
    DEFAULT("默认"),
    CUSTOM("自定义");
}

/** 导出格式 */
enum class ExportFormat(val displayName: String, val extension: String) {
    PNG("PNG 图片", "png"),
    SVG("SVG 矢量", "svg"),
    PDF("PDF 文档", "pdf");
}

/** 主题模式 */
enum class ThemeMode(val displayName: String) {
    FOLLOW_SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色");
}
