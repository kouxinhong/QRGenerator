package com.example.qrgenerator.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrgenerator.data.model.*
import com.example.qrgenerator.data.repository.SettingsRepository
import com.example.qrgenerator.domain.QrConfigHolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface SettingsEvent {
    data class UpdateThemeMode(val mode: ThemeMode) : SettingsEvent
    data class UpdateErrorLevel(val level: QrErrorLevel) : SettingsEvent
    data class UpdateExportFormat(val format: ExportFormat) : SettingsEvent
    data class UpdateImageSize(val size: Int) : SettingsEvent
    data object RestoreDefaults : SettingsEvent
    data object Retry : SettingsEvent
    data object ClearFeedback : SettingsEvent
    data object ClearMessage : SettingsEvent
}

data class SettingsStatus(
    val hasLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val configHolder: QrConfigHolder
) : ViewModel() {
    private val mutableSettings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = mutableSettings.asStateFlow()
    private val mutableStatus = MutableStateFlow(SettingsStatus())
    val status: StateFlow<SettingsStatus> = mutableStatus.asStateFlow()
    private var readJob: Job? = null
    private var failedEvent: SettingsEvent? = null

    init { observeSettings() }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.ClearMessage -> mutableStatus.update { it.copy(message = null) }
            SettingsEvent.ClearFeedback -> mutableStatus.update { it.copy(errorMessage = null, message = null) }
            SettingsEvent.Retry -> failedEvent?.let(::saveSettings) ?: observeSettings()
            else -> saveSettings(event)
        }
    }

    private fun observeSettings() {
        readJob?.cancel()
        readJob = viewModelScope.launch {
            var hasReportedReadError: Boolean = false
            flow { emitAll(repository.executeGetSettings()) }.retryWhen { error, _ ->
                if (error !is IOException) return@retryWhen false
                if (!hasReportedReadError && failedEvent == null) mutableStatus.update { it.copy(errorMessage = "读取设置失败，正在重试。也可以点“重试”立即读取。") }
                hasReportedReadError = true
                delay(2_000)
                true
            }.catch { error ->
                if (error is CancellationException) throw error
                mutableStatus.update { it.copy(errorMessage = "暂时无法读取设置，请重试") }
            }.collect { settings ->
                hasReportedReadError = false
                mutableSettings.value = settings
                mutableStatus.update { it.copy(hasLoaded = true, errorMessage = if (failedEvent == null) null else it.errorMessage) }
            }
        }
    }

    private fun saveSettings(event: SettingsEvent) {
        if (mutableStatus.value.isSaving) return
        mutableStatus.update { it.copy(isSaving = true, errorMessage = null, message = null) }
        viewModelScope.launch {
            try {
                writeSetting(event)
                applyToCurrentQr(event)
                failedEvent = null
                // 普通选项直接在列表显示新值，避免成功提示遮住下方的设置入口。
                mutableStatus.update { it.copy(isSaving = false, message = if (event == SettingsEvent.RestoreDefaults) "已恢复默认设置" else null) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failedEvent = event
                mutableStatus.update { it.copy(isSaving = false, errorMessage = "设置没有保存成功，请检查存储空间后重试") }
            }
        }
    }

    private suspend fun writeSetting(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.UpdateThemeMode -> repository.executeUpdateThemeMode(event.mode)
            is SettingsEvent.UpdateErrorLevel -> repository.executeUpdateErrorLevel(event.level)
            is SettingsEvent.UpdateExportFormat -> repository.executeUpdateExportFormat(event.format)
            is SettingsEvent.UpdateImageSize -> repository.executeUpdateImageSize(event.size)
            SettingsEvent.RestoreDefaults -> repository.executeResetSettings()
            else -> Unit
        }
    }

    /** 同一个默认值也要应用到当前二维码，不能依赖 DataStore 再发一次通知。 */
    private fun applyToCurrentQr(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.UpdateImageSize -> configHolder.executeUpdateConfig { it.copy(imageWidth = event.size, imageHeight = event.size) }
            is SettingsEvent.UpdateErrorLevel -> configHolder.executeUpdateConfig { it.copy(errorCorrectionLevel = event.level) }
            SettingsEvent.RestoreDefaults -> configHolder.executeUpdateConfig {
                it.copy(imageWidth = AppSettings().defaultImageSize, imageHeight = AppSettings().defaultImageSize, errorCorrectionLevel = AppSettings().defaultErrorLevel)
            }
            else -> Unit
        }
    }
}
