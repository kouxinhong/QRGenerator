package com.example.qrgenerator.ui

import androidx.lifecycle.ViewModelStore
import com.example.qrgenerator.data.local.QrHistoryEntity
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrPixelShapeType
import com.example.qrgenerator.data.repository.QrHistoryRepository
import com.example.qrgenerator.domain.QrConfigHolderImpl
import com.example.qrgenerator.ui.screens.history.HistoryEvent
import com.example.qrgenerator.ui.screens.history.HistoryViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<QrHistoryRepository>(relaxed = true)
    private val records = MutableStateFlow<List<QrHistoryEntity>>(emptyList())
    private val holder = QrConfigHolderImpl()
    private val store = ViewModelStore()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { repository.executeGetAll() } returns records
    }
    @After fun teardown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test fun searchKeepsLatestQueryWhenDatabaseChanges() = runTest(dispatcher) {
        val model = createModel()
        records.value = listOf(record(1, "学校"), record(2, "家里"))
        advanceUntilIdle()
        model.onEvent(HistoryEvent.Search("学"))
        model.onEvent(HistoryEvent.Search("家"))
        records.value = records.value + record(3, "家人")
        advanceUntilIdle()
        assertEquals(listOf(2L, 3L), model.uiState.value.records.map { it.id })
    }

    @Test fun openingRecordRestoresContentAndCreativeStyle() = runTest(dispatcher) {
        val inputRecord = record(1, "春游")
        val expected = QrConfig(content = "春游", darkPixelShape = QrPixelShapeType.BAMBOO)
        every { repository.executeRestoreConfig(inputRecord) } returns expected
        val model = createModel()
        assertTrue(model.restoreRecord(inputRecord))
        assertEquals(expected, holder.config.value)
    }

    @Test fun deleteOnlyRemovesExplicitlySelectedVisibleRecords() = runTest(dispatcher) {
        val first = record(1, "学校")
        val second = record(2, "家里")
        records.value = listOf(first, second)
        val model = createModel()
        advanceUntilIdle()
        model.onEvent(HistoryEvent.ToggleDeleteMode)
        model.onEvent(HistoryEvent.ToggleSelection(2))
        model.onEvent(HistoryEvent.DeleteSelected)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.executeDeleteRecords(listOf(second)) }
        assertFalse(model.uiState.value.isDeleteMode)
        assertEquals("已删除 1 条记录", model.uiState.value.message)
    }

    @Test fun changingSearchClearsHiddenSelectionAndCannotDeleteIt() = runTest(dispatcher) {
        records.value = listOf(record(1, "学校"), record(2, "家里"))
        val model = createModel()
        advanceUntilIdle()
        model.onEvent(HistoryEvent.ToggleDeleteMode)
        model.onEvent(HistoryEvent.ToggleSelection(1))
        model.onEvent(HistoryEvent.Search("家"))
        assertTrue(model.uiState.value.selectedIds.isEmpty())
        model.onEvent(HistoryEvent.DeleteSelected)
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.executeDeleteRecords(any()) }
    }

    @Test fun selectAllOnlyIncludesCurrentSearchAndCanBeCancelled() = runTest(dispatcher) {
        records.value = listOf(record(1, "学校"), record(2, "家里"), record(3, "家人"))
        val model = createModel()
        advanceUntilIdle()
        model.onEvent(HistoryEvent.Search("家"))
        model.onEvent(HistoryEvent.ToggleDeleteMode)
        model.onEvent(HistoryEvent.ToggleSelectAll)
        assertEquals(setOf(2L, 3L), model.uiState.value.selectedIds)
        model.onEvent(HistoryEvent.ToggleSelection(1))
        assertEquals(setOf(2L, 3L), model.uiState.value.selectedIds)
        model.onEvent(HistoryEvent.ToggleSelectAll)
        assertTrue(model.uiState.value.selectedIds.isEmpty())
    }

    @Test fun removedRecordsArePrunedFromSelection() = runTest(dispatcher) {
        records.value = listOf(record(1, "学校"), record(2, "家里"))
        val model = createModel()
        advanceUntilIdle()
        model.onEvent(HistoryEvent.ToggleDeleteMode)
        model.onEvent(HistoryEvent.ToggleSelectAll)
        records.value = listOf(record(2, "家里"))
        advanceUntilIdle()
        assertEquals(setOf(2L), model.uiState.value.selectedIds)
    }

    @Test fun leavingManagementClearsSelection() = runTest(dispatcher) {
        records.value = listOf(record(1, "学校"))
        val model = createModel()
        advanceUntilIdle()
        model.onEvent(HistoryEvent.ToggleDeleteMode)
        model.onEvent(HistoryEvent.ToggleSelectAll)
        model.onEvent(HistoryEvent.ToggleDeleteMode)
        assertFalse(model.uiState.value.isDeleteMode)
        assertTrue(model.uiState.value.selectedIds.isEmpty())
    }

    @Test fun deletionBlocksDuplicateRequestsAndSelectionChanges() = runTest(dispatcher) {
        records.value = listOf(record(1, "学校"))
        coEvery { repository.executeDeleteRecords(any()) } coAnswers { delay(1_000) }
        val model = createModel()
        advanceUntilIdle()
        model.onEvent(HistoryEvent.ToggleDeleteMode)
        model.onEvent(HistoryEvent.ToggleSelectAll)
        model.onEvent(HistoryEvent.DeleteSelected)
        assertTrue(model.uiState.value.isDeleting)
        model.onEvent(HistoryEvent.DeleteSelected)
        model.onEvent(HistoryEvent.Search("其他"))
        model.onEvent(HistoryEvent.ToggleDeleteMode)
        assertEquals("", model.uiState.value.searchQuery)
        assertTrue(model.uiState.value.isDeleteMode)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.executeDeleteRecords(any()) }
        assertFalse(model.uiState.value.isDeleting)
    }

    @Test fun deletionFailureKeepsSelectionForRetry() = runTest(dispatcher) {
        records.value = listOf(record(1, "学校"))
        coEvery { repository.executeDeleteRecords(any()) } throws IllegalStateException("Database unavailable")
        val model = createModel()
        advanceUntilIdle()
        model.onEvent(HistoryEvent.ToggleDeleteMode)
        model.onEvent(HistoryEvent.ToggleSelectAll)
        model.onEvent(HistoryEvent.DeleteSelected)
        advanceUntilIdle()
        assertFalse(model.uiState.value.isDeleting)
        assertTrue(model.uiState.value.isDeleteMode)
        assertEquals(setOf(1L), model.uiState.value.selectedIds)
        assertNotNull(model.uiState.value.errorMessage)
        assertNull(model.uiState.value.message)
    }

    @Test fun readingFailureCanBeRetriedWithoutCrashing() = runTest(dispatcher) {
        every { repository.executeGetAll() } returnsMany listOf(flow { throw IllegalStateException("Read failed") }, records)
        val model = createModel()
        advanceUntilIdle()
        assertFalse(model.uiState.value.isLoading)
        assertNotNull(model.uiState.value.loadError)
        records.value = listOf(record(1, "已恢复"))
        model.onEvent(HistoryEvent.RetryLoading)
        advanceUntilIdle()
        assertNull(model.uiState.value.loadError)
        assertEquals("已恢复", model.uiState.value.records.single().contentPreview)
    }

    private fun createModel(): HistoryViewModel = HistoryViewModel(repository, holder).also { store.put("history", it) }
    private fun record(id: Long, preview: String): QrHistoryEntity = QrHistoryEntity(id, "{}", "", "TEXT", preview)
}
