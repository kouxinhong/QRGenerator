package com.example.qrgenerator.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrgenerator.data.local.QrHistoryEntity
import com.example.qrgenerator.data.repository.QrHistoryRepository
import com.example.qrgenerator.domain.QrConfigHolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface HistoryEvent {
    data class Search(val query: String) : HistoryEvent
    data object ToggleDeleteMode : HistoryEvent
    data class ToggleSelection(val id: Long) : HistoryEvent
    data object ToggleSelectAll : HistoryEvent
    data object DeleteSelected : HistoryEvent
    data object DeleteAll : HistoryEvent
    data object RetryLoading : HistoryEvent
    data object ClearError : HistoryEvent
    data object ClearMessage : HistoryEvent
}

data class HistoryUiState(
    val records: List<QrHistoryEntity> = emptyList(),
    val searchQuery: String = "",
    val isDeleteMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val loadError: String? = null,
    val errorMessage: String? = null,
    val message: String? = null
)

class HistoryViewModel(
    private val repository: QrHistoryRepository,
    private val configHolder: QrConfigHolder
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    private var allRecords: List<QrHistoryEntity> = emptyList()
    private var loadJob: Job? = null

    init { loadRecords() }

    fun onEvent(event: HistoryEvent) {
        if (_uiState.value.isDeleting && event != HistoryEvent.ClearError && event != HistoryEvent.ClearMessage) return
        when (event) {
            is HistoryEvent.Search -> {
                _uiState.update { it.copy(searchQuery = event.query, selectedIds = if (it.searchQuery == event.query) it.selectedIds else emptySet()) }
                refreshRecords()
            }
            HistoryEvent.ToggleDeleteMode -> _uiState.update {
                it.copy(isDeleteMode = !it.isDeleteMode, selectedIds = emptySet())
            }
            is HistoryEvent.ToggleSelection -> toggleSelection(event.id)
            HistoryEvent.ToggleSelectAll -> toggleSelectAll()
            HistoryEvent.DeleteSelected -> deleteRecords(false)
            HistoryEvent.DeleteAll -> deleteRecords(true)
            HistoryEvent.RetryLoading -> loadRecords()
            HistoryEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            HistoryEvent.ClearMessage -> _uiState.update { it.copy(message = null) }
        }
    }

    fun restoreRecord(record: QrHistoryEntity): Boolean = try {
        configHolder.executeSetConfig(repository.executeRestoreConfig(record))
        true
    } catch (exception: Exception) {
        _uiState.update { it.copy(errorMessage = "这条记录暂时无法打开，请重新制作二维码") }
        false
    }

    private fun loadRecords() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            try {
                repository.executeGetAll().collect { records ->
                    allRecords = records
                    refreshRecords()
                    _uiState.update { it.copy(isLoading = false, loadError = null) }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(isLoading = false, loadError = "暂时读不到已保存的记录，请重试", isDeleteMode = false, selectedIds = emptySet()) }
            }
        }
    }

    private fun refreshRecords() {
        _uiState.update { state ->
            val query = state.searchQuery.trim()
            val visibleRecords = allRecords.filter { query.isEmpty() || it.contentPreview.contains(query, ignoreCase = true) }
            state.copy(records = visibleRecords, selectedIds = state.selectedIds.intersect(visibleRecords.map { it.id }.toSet()))
        }
    }

    private fun toggleSelection(id: Long) {
        _uiState.update { state ->
            if (!state.isDeleteMode || state.records.none { it.id == id }) state
            else state.copy(selectedIds = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id)
        }
    }

    private fun toggleSelectAll() {
        _uiState.update { state ->
            val visibleIds = state.records.map { it.id }.toSet()
            if (!state.isDeleteMode) state
            else state.copy(selectedIds = if (visibleIds.isNotEmpty() && state.selectedIds.containsAll(visibleIds)) emptySet() else visibleIds)
        }
    }

    private fun deleteRecords(deleteAll: Boolean) {
        val state = _uiState.value
        if (!deleteAll && !state.isDeleteMode) return
        val selected = if (deleteAll) allRecords else state.records.filter { it.id in state.selectedIds }
        if (selected.isEmpty()) return
        _uiState.update { it.copy(isDeleting = true, errorMessage = null, message = null) }
        viewModelScope.launch {
            try {
                if (deleteAll) repository.executeDeleteAll() else repository.executeDeleteRecords(selected)
                _uiState.update { it.copy(isDeleteMode = false, selectedIds = emptySet(), message = "已删除 ${selected.size} 条记录") }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(errorMessage = "删除没有完成，请重试") }
            } finally {
                _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }
}
