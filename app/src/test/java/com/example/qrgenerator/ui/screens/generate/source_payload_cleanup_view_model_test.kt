package com.example.qrgenerator.ui.screens.generate

import androidx.lifecycle.ViewModelStore
import com.example.qrgenerator.data.model.AppSettings
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.data.model.QrScene
import com.example.qrgenerator.data.repository.QrHistoryRepository
import com.example.qrgenerator.data.repository.SettingsRepository
import com.example.qrgenerator.domain.ContentValidator
import com.example.qrgenerator.domain.ExportUseCase
import com.example.qrgenerator.domain.QrConfigHolderImpl
import com.example.qrgenerator.domain.QrContentCodec
import com.example.qrgenerator.domain.QrGeneratorUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SourcePayloadCleanupViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private val store: ViewModelStore = ViewModelStore()
    private val holder: QrConfigHolderImpl = QrConfigHolderImpl()
    private val settingsRepository: SettingsRepository = mockk()
    private val rawPayload: String = " WIFI:S:Cafe;P:old-private-password;T:WPA;;\n"

    @Before
    fun prepareMainDispatcher(): Unit {
        Dispatchers.setMain(dispatcher)
        every { settingsRepository.executeGetSettings() } returns MutableStateFlow(AppSettings())
    }

    @After
    fun clearViewModel(): Unit {
        try {
            store.clear()
            dispatcher.scheduler.runCurrent()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `修改表单立即同步移除界面和共享配置中的旧来源`(): Unit {
        val viewModel: GenerateViewModel = createViewModel()
        viewModel.onEvent(GenerateEvent.ImportScannedContent(rawPayload))
        val current: QrConfig = holder.config.value
        val edited: String = QrContentCodec.serialize(
            QrContentCodec.fields(current.contentType, current.content) + ("password" to "new-private-password")
        )

        viewModel.onEvent(GenerateEvent.UpdateContent(edited))

        assertNull(viewModel.uiState.value.config.sourcePayload)
        assertEquals(holder.config.value, viewModel.uiState.value.config)
    }

    @Test
    fun `切换类型立即同步移除来源`(): Unit {
        val viewModel: GenerateViewModel = createViewModel()
        viewModel.onEvent(GenerateEvent.ImportScannedContent(rawPayload))

        viewModel.onEvent(GenerateEvent.UpdateContentType(QrContentType.TEXT))

        assertNull(viewModel.uiState.value.config.sourcePayload)
        assertEquals(holder.config.value, viewModel.uiState.value.config)
    }

    @Test
    fun `切换用途清理当前来源但切回仍恢复原完整草稿`(): Unit {
        val viewModel: GenerateViewModel = createViewModel()
        viewModel.onEvent(GenerateEvent.ImportScannedContent(rawPayload))
        viewModel.onEvent(GenerateEvent.UpdateCardTitle("访客网络"))
        val previous: QrConfig = holder.config.value

        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.MENU))

        assertNull(holder.config.value.sourcePayload)
        assertEquals(holder.config.value, viewModel.uiState.value.config)
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.WIFI))
        assertEquals(previous, holder.config.value)
        assertEquals(rawPayload, QrContentCodec.encode(holder.config.value).encode())
    }

    private fun createViewModel(): GenerateViewModel = GenerateViewModel(
        qrGeneratorUseCase = mockk<QrGeneratorUseCase>(),
        exportUseCase = mockk<ExportUseCase>(),
        contentValidator = ContentValidator(),
        qrConfigHolder = holder,
        historyRepository = mockk<QrHistoryRepository>(),
        settingsRepository = settingsRepository
    ).also { store.put("generate", it) }
}
