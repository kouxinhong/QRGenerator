package com.example.qrgenerator.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrgenerator.data.local.QrHistoryEntity
import com.example.qrgenerator.data.repository.QrHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 历史页面 UI 状态 */
data class HistoryUiState(
    val records: List<QrHistoryEntity> = emptyList(),
    val searchQuery: String = "",
    val isDeleteMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
)

/** 历史页面 ViewModel */
class HistoryViewModel(private val repository: QrHistoryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        executeLoadHistory()
    }

    private fun executeLoadHistory() {
        viewModelScope.launch {
            repository.executeGetAll().collect { records ->
                _uiState.update { it.copy(records = records) }
            }
        }
    }

    fun executeSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            if (query.isBlank()) {
                repository.executeGetAll().collect { records ->
                    _uiState.update { it.copy(records = records) }
                }
            } else {
                repository.executeSearch(query).collect { records ->
                    _uiState.update { it.copy(records = records) }
                }
            }
        }
    }

    fun executeToggleDeleteMode() {
        _uiState.update { it.copy(isDeleteMode = !it.isDeleteMode, selectedIds = emptySet()) }
    }

    fun executeToggleSelection(id: Long) {
        _uiState.update {
            val newIds = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id
            it.copy(selectedIds = newIds)
        }
    }

    fun executeDeleteSelected() {
        viewModelScope.launch {
            _uiState.value.records
                .filter { it.id in _uiState.value.selectedIds }
                .forEach { repository.executeDelete(it) }
            _uiState.update { it.copy(isDeleteMode = false, selectedIds = emptySet()) }
        }
    }

    fun executeDeleteAll() {
        viewModelScope.launch {
            repository.executeDeleteAll()
            _uiState.update { it.copy(isDeleteMode = false, selectedIds = emptySet()) }
        }
    }
}
