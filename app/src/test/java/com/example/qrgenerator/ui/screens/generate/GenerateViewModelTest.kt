package com.example.qrgenerator.ui.screens.generate

import androidx.lifecycle.ViewModelStore
import com.example.qrgenerator.data.model.*
import com.example.qrgenerator.data.repository.QrHistoryRepository
import com.example.qrgenerator.data.repository.SettingsRepository
import com.example.qrgenerator.domain.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class GenerateViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private val store: ViewModelStore = ViewModelStore()
    private val holder: QrConfigHolderImpl = QrConfigHolderImpl()
    private val settings: MutableStateFlow<AppSettings> = MutableStateFlow(AppSettings())
    private val repository: SettingsRepository = mockk()
    private val generator: QrGeneratorUseCase = mockk()
    private val exporter: ExportUseCase = mockk()
    private val history: QrHistoryRepository = mockk()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { repository.executeGetSettings() } returns settings
    }

    @After
    fun teardown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `首次打开采用默认尺寸`() = runTest(dispatcher) {
        settings.value = AppSettings(defaultImageSize = 2048)
        createViewModel()
        runCurrent()
        assertEquals(2048, holder.config.value.imageWidth)
        assertEquals(2048, holder.config.value.imageHeight)
    }

    @Test
    fun `重建页面不会覆盖恢复记录的尺寸和容错级别`() = runTest(dispatcher) {
        holder.executeSetConfig(QrConfig(contentType = QrContentType.URL, content = "待修改的网址", imageWidth = 768, imageHeight = 1024, errorCorrectionLevel = QrErrorLevel.HIGH))
        settings.value = AppSettings(defaultImageSize = 2048, defaultErrorLevel = QrErrorLevel.LOW)
        createViewModel()
        runCurrent()
        assertEquals(768, holder.config.value.imageWidth)
        assertEquals(1024, holder.config.value.imageHeight)
        assertEquals(QrErrorLevel.HIGH, holder.config.value.errorCorrectionLevel)
    }

    @Test
    fun `改变主题不会覆盖样式页手动尺寸`() = runTest(dispatcher) {
        createViewModel()
        runCurrent()
        holder.executeUpdateConfig { it.copy(imageWidth = 768, imageHeight = 1536, errorCorrectionLevel = QrErrorLevel.HIGH) }
        settings.value = AppSettings(themeMode = ThemeMode.DARK)
        runCurrent()
        assertEquals(768, holder.config.value.imageWidth)
        assertEquals(1536, holder.config.value.imageHeight)
        assertEquals(QrErrorLevel.HIGH, holder.config.value.errorCorrectionLevel)
        settings.value = settings.value.copy(defaultImageSize = 2048)
        runCurrent()
        assertEquals(2048, holder.config.value.imageWidth)
        assertEquals(QrErrorLevel.HIGH, holder.config.value.errorCorrectionLevel)
    }

    @Test
    fun `无效输入只显示字段提示且不能保存分享`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onEvent(GenerateEvent.UpdateContentType(QrContentType.URL))
        viewModel.onEvent(GenerateEvent.UpdateContent("invalid"))
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.validationMessage)
        assertNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.qrDrawable)
        viewModel.onEvent(GenerateEvent.SaveToGallery)
        viewModel.onEvent(GenerateEvent.Share)
        viewModel.onEvent(GenerateEvent.ExportFile(ExportFormat.PNG))
        advanceUntilIdle()
        verify { generator wasNot Called }
        verify { exporter wasNot Called }
        coVerify { history wasNot Called }
    }

    @Test
    fun `切换类型后能找回未完成的内容`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onEvent(GenerateEvent.UpdateContent("周六见"))
        viewModel.onEvent(GenerateEvent.UpdateContentType(QrContentType.URL))
        viewModel.onEvent(GenerateEvent.UpdateContent("https://"))
        viewModel.onEvent(GenerateEvent.UpdateContentType(QrContentType.TEXT))
        assertEquals("周六见", holder.config.value.content)
        viewModel.onEvent(GenerateEvent.UpdateContentType(QrContentType.URL))
        assertEquals("https://", holder.config.value.content)
    }

    @Test
    fun `设置延迟返回时已经输入的新草稿仍采用已保存的默认值`() = runTest(dispatcher) {
        val delayedSettings = MutableSharedFlow<AppSettings>(extraBufferCapacity = 1)
        every { repository.executeGetSettings() } returns delayedSettings
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(GenerateEvent.UpdateContentType(QrContentType.URL))
        viewModel.onEvent(GenerateEvent.UpdateContent("https://"))
        delayedSettings.emit(AppSettings(defaultImageSize = 2048, defaultErrorLevel = QrErrorLevel.HIGH))
        runCurrent()
        assertEquals("https://", holder.config.value.content)
        assertEquals(2048, holder.config.value.imageWidth)
        assertEquals(2048, holder.config.value.imageHeight)
        assertEquals(QrErrorLevel.HIGH, holder.config.value.errorCorrectionLevel)
    }

    @Test
    fun `设置延迟返回期间恢复的历史即使尺寸是默认值也不会覆盖`() = runTest(dispatcher) {
        val delayedSettings = MutableSharedFlow<AppSettings>(extraBufferCapacity = 1)
        every { repository.executeGetSettings() } returns delayedSettings
        createViewModel()
        runCurrent()
        val restored = QrConfig(contentType = QrContentType.URL, content = "旧记录待补全")
        holder.executeSetConfig(restored)
        delayedSettings.emit(AppSettings(defaultImageSize = 2048, defaultErrorLevel = QrErrorLevel.HIGH))
        runCurrent()
        assertEquals(restored, holder.config.value)
    }

    @Test
    fun `设置首次返回保留空白草稿已有的手工样式值`() = runTest(dispatcher) {
        holder.executeUpdateConfig { it.copy(imageWidth = 768, imageHeight = 1536, errorCorrectionLevel = QrErrorLevel.HIGH) }
        settings.value = AppSettings(defaultImageSize = 2048, defaultErrorLevel = QrErrorLevel.LOW)
        createViewModel()
        runCurrent()
        assertEquals(768, holder.config.value.imageWidth)
        assertEquals(1536, holder.config.value.imageHeight)
        assertEquals(QrErrorLevel.HIGH, holder.config.value.errorCorrectionLevel)
    }

    @Test
    fun `设置延迟返回保留等待期间手工调整的尺寸和容错`() = runTest(dispatcher) {
        val delayedSettings = MutableSharedFlow<AppSettings>(extraBufferCapacity = 1)
        every { repository.executeGetSettings() } returns delayedSettings
        createViewModel()
        runCurrent()
        holder.executeUpdateConfig { it.copy(imageWidth = 768, imageHeight = 1536, errorCorrectionLevel = QrErrorLevel.HIGH) }
        delayedSettings.emit(AppSettings(defaultImageSize = 2048, defaultErrorLevel = QrErrorLevel.LOW))
        runCurrent()
        assertEquals(768, holder.config.value.imageWidth)
        assertEquals(1536, holder.config.value.imageHeight)
        assertEquals(QrErrorLevel.HIGH, holder.config.value.errorCorrectionLevel)
    }

    @Test
    fun `设置读取IOException后保留内容并重试恢复`() = runTest(dispatcher) {
        var attempts = 0
        every { repository.executeGetSettings() } returns flow {
            attempts += 1
            if (attempts == 1) throw IOException("暂时无法读取")
            emit(AppSettings(defaultImageSize = 2048, defaultErrorLevel = QrErrorLevel.HIGH))
            awaitCancellation()
        }
        val viewModel = createViewModel()
        viewModel.onEvent(GenerateEvent.UpdateContentType(QrContentType.URL))
        viewModel.onEvent(GenerateEvent.UpdateContent("https://"))
        runCurrent()
        assertEquals("https://", holder.config.value.content)
        assertEquals(1024, holder.config.value.imageWidth)
        assertTrue(viewModel.uiState.value.message.orEmpty().contains("正在重试"))
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, attempts)
        assertEquals("https://", holder.config.value.content)
        assertEquals(2048, holder.config.value.imageWidth)
        assertEquals(QrErrorLevel.HIGH, holder.config.value.errorCorrectionLevel)
        assertEquals("设置已恢复", viewModel.uiState.value.message)
    }

    @Test
    fun `设置读取重试保留上次快照不会覆盖读取失败期间的手工值`() = runTest(dispatcher) {
        var attempts = 0
        every { repository.executeGetSettings() } returns flow {
            attempts += 1
            emit(AppSettings(defaultImageSize = 2048, defaultErrorLevel = QrErrorLevel.HIGH))
            if (attempts == 1) {
                delay(1_000)
                throw IOException("读取中断")
            }
            awaitCancellation()
        }
        val viewModel = createViewModel()
        runCurrent()
        assertEquals(2048, holder.config.value.imageWidth)
        holder.executeUpdateConfig { it.copy(imageWidth = 768, imageHeight = 1536, errorCorrectionLevel = QrErrorLevel.LOW) }
        advanceTimeBy(1_000)
        runCurrent()
        assertTrue(viewModel.uiState.value.message.orEmpty().contains("正在重试"))
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, attempts)
        assertEquals(768, holder.config.value.imageWidth)
        assertEquals(1536, holder.config.value.imageHeight)
        assertEquals(QrErrorLevel.LOW, holder.config.value.errorCorrectionLevel)
        assertEquals("设置已恢复", viewModel.uiState.value.message)
    }

    private fun createViewModel(): GenerateViewModel = GenerateViewModel(generator, exporter, ContentValidator(), holder, history, repository).also { store.put("generate", it) }

    @Test
    fun `切换用途保留每张卡片的内容标题和样式`() = runTest(dispatcher) {
        val viewModel: GenerateViewModel = createViewModel()
        viewModel.onEvent(GenerateEvent.UpdateContent("原来的文字"))
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.WIFI))
        viewModel.onEvent(GenerateEvent.UpdateContent(QrContentCodec.serialize(mapOf("ssid" to "访客网络", "password" to "secret"))))
        viewModel.onEvent(GenerateEvent.UpdateCardTitle("欢迎来小店"))
        holder.executeUpdateConfig { it.copy(darkPixelShape = QrPixelShapeType.BAMBOO) }
        val wifiDraft: QrConfig = holder.config.value
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.MENU))
        assertEquals(QrContentType.URL, holder.config.value.contentType)
        assertTrue(holder.config.value.card.enabled)
        assertEquals("", holder.config.value.content)
        viewModel.onEvent(GenerateEvent.UpdateContent("https://example.com/menu"))
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.WIFI))
        assertEquals(wifiDraft, holder.config.value)
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.FREE))
        assertEquals("原来的文字", holder.config.value.content)
        assertFalse(holder.config.value.card.enabled)
    }

    @Test
    fun `恢复清晰样式保留卡片文案内容和导出尺寸`() = runTest(dispatcher) {
        val viewModel: GenerateViewModel = createViewModel()
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.CONTACT))
        viewModel.onEvent(GenerateEvent.UpdateCardTitle("我的联系方式"))
        viewModel.onEvent(GenerateEvent.UpdateCardSubtitle("下次活动再见"))
        holder.executeUpdateConfig { it.copy(content = "小王", padding = 0f, offsetX = 1f, imageWidth = 2048, logo = QrLogoConfig(size = .8f)) }
        val original: QrConfig = holder.config.value
        viewModel.onEvent(GenerateEvent.RestoreReadableStyle)
        val actual: QrConfig = holder.config.value
        assertEquals(original.content, actual.content)
        assertEquals(original.contentType, actual.contentType)
        assertEquals(original.card, actual.card)
        assertEquals(2048, actual.imageWidth)
        assertEquals(QrConfig().padding, actual.padding)
        assertEquals(0f, actual.offsetX)
        assertEquals(QrLogoConfig(), actual.logo)
        assertEquals(QrErrorLevel.HIGH, actual.errorCorrectionLevel)
    }

    @Test
    fun `切换纯码不改变二维码内容或丢失卡片文字`() = runTest(dispatcher) {
        val viewModel: GenerateViewModel = createViewModel()
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.MENU))
        viewModel.onEvent(GenerateEvent.UpdateContent("https://example.com/menu"))
        viewModel.onEvent(GenerateEvent.UpdateCardTitle("今日菜单"))
        val original: QrConfig = holder.config.value
        viewModel.onEvent(GenerateEvent.SetCardEnabled(false))
        assertEquals(original.copy(card = original.card.copy(enabled = false)), holder.config.value)
        viewModel.onEvent(GenerateEvent.SetCardEnabled(true))
        assertEquals(original, holder.config.value)
    }
}
