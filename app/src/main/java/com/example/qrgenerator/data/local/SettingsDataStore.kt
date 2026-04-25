package com.example.qrgenerator.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_ERROR_LEVEL = stringPreferencesKey("error_level")
        private val KEY_EXPORT_FORMAT = stringPreferencesKey("export_format")
        private val KEY_IMAGE_SIZE = intPreferencesKey("image_size")
    }

    fun readThemeMode(): Flow<String> = context.dataStore.data.map { it[KEY_THEME_MODE] ?: "FOLLOW_SYSTEM" }
    suspend fun writeThemeMode(value: String) { context.dataStore.edit { it[KEY_THEME_MODE] = value } }
    fun readErrorLevel(): Flow<String> = context.dataStore.data.map { it[KEY_ERROR_LEVEL] ?: "AUTO" }
    suspend fun writeErrorLevel(value: String) { context.dataStore.edit { it[KEY_ERROR_LEVEL] = value } }
    fun readExportFormat(): Flow<String> = context.dataStore.data.map { it[KEY_EXPORT_FORMAT] ?: "PNG" }
    suspend fun writeExportFormat(value: String) { context.dataStore.edit { it[KEY_EXPORT_FORMAT] = value } }
    fun readImageSize(): Flow<Int> = context.dataStore.data.map { it[KEY_IMAGE_SIZE] ?: 1024 }
    suspend fun writeImageSize(value: Int) { context.dataStore.edit { it[KEY_IMAGE_SIZE] = value } }
}
