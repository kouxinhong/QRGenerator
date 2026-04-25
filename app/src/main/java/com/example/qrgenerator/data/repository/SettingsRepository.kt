package com.example.qrgenerator.data.repository

import com.example.qrgenerator.data.local.SettingsDataStore
import com.example.qrgenerator.data.model.AppSettings
import com.example.qrgenerator.data.model.ExportFormat
import com.example.qrgenerator.data.model.QrErrorLevel
import com.example.qrgenerator.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SettingsRepository(private val dataStore: SettingsDataStore) {
    fun executeGetSettings(): Flow<AppSettings> = combine(
        dataStore.readThemeMode(),
        dataStore.readErrorLevel(),
        dataStore.readExportFormat(),
        dataStore.readImageSize()
    ) { theme, error, format, size ->
        AppSettings(
            themeMode = ThemeMode.valueOf(theme),
            defaultErrorLevel = QrErrorLevel.valueOf(error),
            defaultExportFormat = ExportFormat.valueOf(format),
            defaultImageSize = size
        )
    }
    suspend fun executeUpdateThemeMode(mode: ThemeMode) { dataStore.writeThemeMode(mode.name) }
    suspend fun executeUpdateErrorLevel(level: QrErrorLevel) { dataStore.writeErrorLevel(level.name) }
    suspend fun executeUpdateExportFormat(format: ExportFormat) { dataStore.writeExportFormat(format.name) }
    suspend fun executeUpdateImageSize(size: Int) { dataStore.writeImageSize(size) }
}
