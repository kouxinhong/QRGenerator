package com.example.qrgenerator.data.model

/** 场景只提供用途和文字，不把二维码内容或密码复制到卡面。 */
enum class QrScene(
    val title: String,
    val contentType: QrContentType,
    val defaultTitle: String,
    val defaultSubtitle: String
) {
    FREE("自由制作", QrContentType.TEXT, "扫码看看", "用手机相机扫一扫"),
    WIFI("连 Wi-Fi", QrContentType.WIFI, "欢迎连接 Wi-Fi", "扫一扫，加入无线网络"),
    MENU("看菜单", QrContentType.URL, "扫码看菜单", "扫一扫，查看菜单"),
    CONTACT("存联系方式", QrContentType.VCARD, "保持联系", "扫一扫，存入通讯录")
}

data class QrCardConfig(
    val scene: QrScene = QrScene.FREE,
    val enabled: Boolean = false,
    val title: String = "",
    val subtitle: String = ""
)
