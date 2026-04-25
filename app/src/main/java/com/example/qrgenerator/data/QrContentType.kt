package com.example.qrgenerator.data

/**
 * 二维码内容类型枚举
 * 定义所有支持的二维码编码格式
 */
enum class QrContentType(
    val displayName: String,
    val icon: String
) {
    /** 普通文本 */
    TEXT("文本", "text"),

    /** 网址链接 */
    URL("网址", "url"),

    /** WiFi 连接信息 */
    WIFI("WiFi", "wifi"),

    /** 联系人名片 */
    VCARD("联系人", "vcard"),

    /** 电话号码 */
    PHONE("电话", "phone"),

    /** 短信 */
    SMS("短信", "sms"),

    /** 邮件 */
    EMAIL("邮件", "email"),

    /** 地理位置 */
    GEO("位置", "geo"),

    /** 书签 */
    BOOKMARK("书签", "bookmark"),

    /** 日历事件 */
    EVENT("日历", "event");
}
