package com.example.qrgenerator.data.repository

import com.example.qrgenerator.data.local.SettingsDataStore
import com.example.qrgenerator.data.model.AppSettings
import com.example.qrgenerator.data.model.ExportFormat
import com.example.qrgenerator.data.model.QrErrorLevel
import com.example.qrgenerator.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: SettingsDataStore) {
    fun executeGetSettings(): Flow<AppSettings> = dataStore.readPreferences().map { values ->
        AppSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == values[SettingsDataStore.KEY_THEME_MODE] } ?: ThemeMode.FOLLOW_SYSTEM,
            defaultErrorLevel = QrErrorLevel.entries.firstOrNull { it.name == values[SettingsDataStore.KEY_ERROR_LEVEL] } ?: QrErrorLevel.AUTO,
            defaultExportFormat = ExportFormat.entries.firstOrNull { it.name == values[SettingsDataStore.KEY_EXPORT_FORMAT] && it != ExportFormat.SVG } ?: ExportFormat.PNG,
            defaultImageSize = values[SettingsDataStore.KEY_IMAGE_SIZE]?.takeIf { it in AppSettings.IMAGE_SIZES } ?: 1024
        )
    }.distinctUntilChanged()

    suspend fun executeUpdateThemeMode(mode: ThemeMode) { dataStore.writeThemeMode(mode.name) }
    suspend fun executeUpdateErrorLevel(level: QrErrorLevel) { dataStore.writeErrorLevel(level.name) }
    suspend fun executeUpdateExportFormat(format: ExportFormat) {
        require(format != ExportFormat.SVG) { "请选择 PNG 图片或 PDF 文档" }
        dataStore.writeExportFormat(format.name)
    }
    suspend fun executeUpdateImageSize(size: Int) {
        require(size in AppSettings.IMAGE_SIZES) { "请选择标准、高清或超清" }
        dataStore.writeImageSize(size)
    }
    suspend fun executeResetSettings() { dataStore.resetPreferences() }
}
