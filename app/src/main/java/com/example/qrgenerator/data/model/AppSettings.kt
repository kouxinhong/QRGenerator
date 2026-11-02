package com.example.qrgenerator.data.model

/** 用户偏好设置 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val defaultErrorLevel: QrErrorLevel = QrErrorLevel.AUTO,
    val defaultExportFormat: ExportFormat = ExportFormat.PNG,
    val defaultImageSize: Int = 1024
) {
    companion object {
        val IMAGE_SIZES: List<Int> = listOf(512, 1024, 2048)
    }
}
