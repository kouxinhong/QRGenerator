package com.example.qrgenerator.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.dataStore)

    fun readPreferences(): Flow<Preferences> = store.data
    suspend fun writeThemeMode(value: String) { store.edit { it[KEY_THEME_MODE] = value } }
    suspend fun writeErrorLevel(value: String) { store.edit { it[KEY_ERROR_LEVEL] = value } }
    suspend fun writeExportFormat(value: String) { store.edit { it[KEY_EXPORT_FORMAT] = value } }
    suspend fun writeImageSize(value: Int) { store.edit { it[KEY_IMAGE_SIZE] = value } }

    /** 一次写入，避免恢复过程中出现混合的新旧设置。 */
    suspend fun resetPreferences() {
        store.edit {
            it.remove(KEY_THEME_MODE)
            it.remove(KEY_ERROR_LEVEL)
            it.remove(KEY_EXPORT_FORMAT)
            it.remove(KEY_IMAGE_SIZE)
        }
    }

    companion object {
        val KEY_THEME_MODE: Preferences.Key<String> = stringPreferencesKey("theme_mode")
        val KEY_ERROR_LEVEL: Preferences.Key<String> = stringPreferencesKey("error_level")
        val KEY_EXPORT_FORMAT: Preferences.Key<String> = stringPreferencesKey("export_format")
        val KEY_IMAGE_SIZE: Preferences.Key<Int> = intPreferencesKey("image_size")
    }
}
