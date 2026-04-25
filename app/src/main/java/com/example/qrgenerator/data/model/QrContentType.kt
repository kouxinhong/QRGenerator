package com.example.qrgenerator.data.model

/** 二维码内容类型 */
enum class QrContentType(val displayName: String) {
    TEXT("文本"),
    URL("网址"),
    WIFI("WiFi"),
    ENTERPRISE_WIFI("企业 WiFi"),
    VCARD("联系人"),
    BIZCARD("名片"),
    MECARD("MeCard"),
    PHONE("电话"),
    SMS("短信"),
    EMAIL("邮件"),
    GEO("位置"),
    BOOKMARK("书签"),
    EVENT("日历"),
    YOUTUBE("YouTube"),
    GOOGLE_PLAY("Google Play");
}
