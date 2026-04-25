package com.example.qrgenerator.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrgenerator.data.model.AppSettings
import com.example.qrgenerator.data.model.ExportFormat
import com.example.qrgenerator.data.model.QrErrorLevel
import com.example.qrgenerator.data.model.ThemeMode
import com.example.qrgenerator.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 设置页面 ViewModel */
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            repository.executeGetSettings().collect { _settings.value = it }
        }
    }

    fun executeUpdateThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.executeUpdateThemeMode(mode) }
    }

    fun executeUpdateErrorLevel(level: QrErrorLevel) {
        viewModelScope.launch { repository.executeUpdateErrorLevel(level) }
    }

    fun executeUpdateExportFormat(format: ExportFormat) {
        viewModelScope.launch { repository.executeUpdateExportFormat(format) }
    }

    fun executeUpdateImageSize(size: Int) {
        viewModelScope.launch { repository.executeUpdateImageSize(size) }
    }
}
