package com.example.qrgenerator.data.model

/** 二维码内容类型 */
enum class QrContentType(val displayName: String) {
    TEXT("文字"),
    URL("网址"),
    WIFI("Wi-Fi"),
    ENTERPRISE_WIFI("企业网络"),
    VCARD("联系人"),
    BIZCARD("商务名片"),
    MECARD("简易名片"),
    PHONE("电话"),
    SMS("短信"),
    EMAIL("邮件"),
    GEO("位置"),
    BOOKMARK("书签"),
    EVENT("日程"),
    YOUTUBE("YouTube"),
    GOOGLE_PLAY("Google Play");
}
