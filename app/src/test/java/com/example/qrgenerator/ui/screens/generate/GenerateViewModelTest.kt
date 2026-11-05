package com.example.qrgenerator.ui.screens.generate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.example.qrgenerator.data.model.*
import com.example.qrgenerator.data.repository.QrHistoryRepository
import com.example.qrgenerator.data.repository.SettingsRepository
import com.example.qrgenerator.domain.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
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
    private val viewModelJobs: MutableList<Job> = mutableListOf()
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
    fun teardown() = runTest(dispatcher) {
        try {
            store.clear()
            // Default 上的渲染取消后仍需回到 Main 收尾，等待完成再重置测试调度器。
            viewModelJobs.joinAll()
        } finally {
            Dispatchers.resetMain()
        }
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

    private fun createViewModel(savedState: SavedStateHandle = SavedStateHandle()): GenerateViewModel = GenerateViewModel(
        generator, exporter, ContentValidator(), holder, history, repository, savedState = savedState
    ).also {
        store.put("generate", it)
        viewModelJobs += it.viewModelScope.coroutineContext.job
    }

    @Test
    fun `扫码填入保留WiFi场景原文样式并能撤销整个草稿`() {
        val viewModel: GenerateViewModel = createViewModel()
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.WIFI))
        viewModel.onEvent(GenerateEvent.UpdateContent(QrContentCodec.serialize(mapOf("ssid" to "原网络", "password" to "12345678"))))
        holder.executeUpdateConfig { it.copy(imageWidth = 768, imageHeight = 768, padding = .2f) }
        val previous: QrConfig = holder.config.value
        val payload: String = "WIFI:T:WPA;S:店名\\;分店;P: abc\\:123 ;H:true;;"
        viewModel.onEvent(GenerateEvent.ImportScannedContent(payload))
        val actual: QrConfig = holder.config.value
        assertEquals(QrScene.WIFI, actual.card.scene)
        assertEquals(QrContentType.WIFI, actual.contentType)
        assertEquals("店名;分店", QrContentCodec.fields(actual.contentType, actual.content)["ssid"])
        assertEquals(payload, QrContentCodec.encode(actual).encode())
        assertEquals(previous.padding, actual.padding)
        assertEquals(previous.imageWidth, actual.imageWidth)
        assertEquals(previous.card.enabled, actual.card.enabled)
        assertTrue(viewModel.uiState.value.canUndoImport)
        viewModel.onEvent(GenerateEvent.UndoImport)
        assertEquals(previous, holder.config.value)
        assertFalse(viewModel.uiState.value.canUndoImport)
    }

    @Test
    fun `扫码填入自由制作保留卡片文字和首尾空白`() {
        val viewModel: GenerateViewModel = createViewModel()
        viewModel.onEvent(GenerateEvent.UpdateCardTitle("我的卡片"))
        viewModel.onEvent(GenerateEvent.UpdateCardSubtitle("扫码了解详情"))
        val card: QrCardConfig = holder.config.value.card
        val payload: String = "  第一行\r\n第二行  "
        viewModel.onEvent(GenerateEvent.ImportScannedContent(payload))
        assertEquals(card, holder.config.value.card)
        assertEquals(payload, QrContentCodec.encode(holder.config.value).encode())
    }

    @Test
    fun `空扫码结果不覆盖当前草稿`() {
        val viewModel: GenerateViewModel = createViewModel()
        viewModel.onEvent(GenerateEvent.UpdateContent("保留的草稿"))
        val previous: QrConfig = holder.config.value
        viewModel.onEvent(GenerateEvent.ImportScannedContent(""))
        assertEquals(previous, holder.config.value)
    }

    @Test
    fun `菜单导入网址保持自定义标题而不变成自由制作`() {
        val viewModel: GenerateViewModel = createViewModel()
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.MENU))
        viewModel.onEvent(GenerateEvent.UpdateCardTitle("午间菜单"))
        val previous: QrConfig = holder.config.value
        val payload: String = "https://example.com/menu?x=1&y=2"
        viewModel.onEvent(GenerateEvent.ImportScannedContent(payload))
        assertEquals(previous.card, holder.config.value.card)
        assertEquals(QrContentType.URL, holder.config.value.contentType)
        assertEquals(payload, QrContentCodec.encode(holder.config.value).encode())
    }

    @Test
    fun `自由文字替换可撤销且撤销后切换用途仍保留原草稿`() {
        val viewModel: GenerateViewModel = createViewModel()
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.FREE))
        viewModel.onEvent(GenerateEvent.UpdateContent("原草稿"))
        val previous: QrConfig = holder.config.value
        viewModel.onEvent(GenerateEvent.ImportScannedContent("替换后的内容"))
        viewModel.onEvent(GenerateEvent.UndoImport)
        assertEquals(previous, holder.config.value)
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.WIFI))
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.FREE))
        assertEquals(previous, holder.config.value)
    }

    @Test
    fun `导入后继续编辑会取消撤销避免覆盖新输入`() {
        val viewModel: GenerateViewModel = createViewModel()
        viewModel.onEvent(GenerateEvent.ImportScannedContent("扫码内容"))
        viewModel.onEvent(GenerateEvent.UpdateContent("后来手动改的内容"))
        viewModel.onEvent(GenerateEvent.UndoImport)
        assertFalse(viewModel.uiState.value.canUndoImport)
        assertEquals("后来手动改的内容", holder.config.value.content)
    }

    @Test
    fun `从历史恢复内容后旧导入撤销不能覆盖恢复记录`() {
        val viewModel: GenerateViewModel = createViewModel()
        viewModel.onEvent(GenerateEvent.ImportScannedContent("扫码内容"))
        val restored: QrConfig = QrConfig(content = "历史记录")
        holder.executeSetConfig(restored)
        viewModel.onEvent(GenerateEvent.UndoImport)
        assertEquals(restored, holder.config.value)
    }

    @Test
    fun `恢复相同内容的历史也立即隐藏旧导入撤销`() = runTest(dispatcher) {
        val viewModel: GenerateViewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(GenerateEvent.ImportScannedContent("扫码内容"))
        runCurrent()
        assertTrue(viewModel.uiState.value.canUndoImport)
        val imported: QrConfig = holder.config.value
        holder.executeSetConfig(imported)
        runCurrent()
        assertFalse(viewModel.uiState.value.canUndoImport)
        viewModel.onEvent(GenerateEvent.UndoImport)
        assertEquals(imported, holder.config.value)
    }

    @Test
    fun `第一次进入先选用途而已有草稿直接编辑`() = runTest(dispatcher) {
        val viewModel: GenerateViewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isChoosingTask)
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.FREE))
        assertFalse(viewModel.uiState.value.isChoosingTask)
        holder.executeSetConfig(QrConfig(content = "恢复的文字"))
        runCurrent()
        assertFalse(viewModel.uiState.value.isChoosingTask)
    }

    @Test
    fun `返回用途选择保留非空草稿且设置更新和重新渲染不会打开编辑`() = runTest(dispatcher) {
        val viewModel: GenerateViewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.MENU))
        viewModel.onEvent(GenerateEvent.UpdateContent("尚未填完的网址"))
        viewModel.onEvent(GenerateEvent.UpdateCardTitle("午间菜单"))
        holder.executeUpdateConfig { it.copy(darkPixelShape = QrPixelShapeType.BAMBOO, padding = .2f) }
        val previous: QrConfig = holder.config.value
        viewModel.onEvent(GenerateEvent.ReturnToTaskSelection)
        viewModel.onEvent(GenerateEvent.Generate)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isChoosingTask)
        assertEquals(previous, viewModel.uiState.value.config)
        assertNotNull(viewModel.uiState.value.validationMessage)
        settings.value = settings.value.copy(defaultImageSize = 2048)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isChoosingTask)
        assertEquals(previous.copy(imageWidth = 2048, imageHeight = 2048), holder.config.value)
        assertEquals(holder.config.value, viewModel.uiState.value.config)
    }

    @Test
    fun `导入后返回用途选择保留原始扫码内容和完整草稿`() = runTest(dispatcher) {
        val viewModel: GenerateViewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.WIFI))
        viewModel.onEvent(GenerateEvent.UpdateCardTitle("访客网络"))
        holder.executeUpdateConfig { it.copy(imageWidth = 768, imageHeight = 768, padding = .2f) }
        val payload: String = "WIFI:T:WPA;S:店名\\;分店;P: abc\\:123 ;H:true;;"
        viewModel.onEvent(GenerateEvent.ImportScannedContent(payload))
        val imported: QrConfig = holder.config.value
        assertNotNull(imported.sourcePayload)
        viewModel.onEvent(GenerateEvent.ReturnToTaskSelection)
        viewModel.onEvent(GenerateEvent.Generate)
        runCurrent()
        assertTrue(viewModel.uiState.value.isChoosingTask)
        assertTrue(viewModel.uiState.value.canUndoImport)
        assertEquals(imported, holder.config.value)
        assertEquals(imported, viewModel.uiState.value.config)
        assertEquals(payload, QrContentCodec.encode(holder.config.value).encode())
    }

    @Test
    fun `从用途选择恢复相同历史会打开编辑且之后仍能返回`() = runTest(dispatcher) {
        val viewModel: GenerateViewModel = createViewModel()
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.MENU))
        viewModel.onEvent(GenerateEvent.UpdateContent("尚未填完的网址"))
        advanceUntilIdle()
        val restored: QrConfig = holder.config.value
        viewModel.onEvent(GenerateEvent.ReturnToTaskSelection)
        assertTrue(viewModel.uiState.value.isChoosingTask)
        holder.executeSetConfig(restored)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isChoosingTask)
        assertEquals(restored, viewModel.uiState.value.config)
        viewModel.onEvent(GenerateEvent.ReturnToTaskSelection)
        viewModel.onEvent(GenerateEvent.Generate)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isChoosingTask)
        assertEquals(restored, holder.config.value)
    }

    @Test
    fun `重建ViewModel恢复用途选择时不会因非空草稿跳回编辑`() = runTest(dispatcher) {
        val savedState: SavedStateHandle = SavedStateHandle()
        val viewModel: GenerateViewModel = createViewModel(savedState)
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.MENU))
        viewModel.onEvent(GenerateEvent.UpdateContent("尚未填完的网址"))
        viewModel.onEvent(GenerateEvent.UpdateCardTitle("稍后继续填写"))
        advanceUntilIdle()
        viewModel.onEvent(GenerateEvent.ReturnToTaskSelection)
        val previous: QrConfig = holder.config.value
        // 模拟旧页面真正销毁；仅覆盖同名实例不会清理旧 ViewModel 的协程资源。
        store.clear()
        viewModel.viewModelScope.coroutineContext.job.join()
        val recreated: GenerateViewModel = createViewModel(savedState)
        advanceUntilIdle()
        assertTrue(recreated.uiState.value.isChoosingTask)
        assertEquals(previous, recreated.uiState.value.config)
        recreated.onEvent(GenerateEvent.SelectScene(QrScene.MENU))
        assertFalse(recreated.uiState.value.isChoosingTask)
        assertEquals(previous, holder.config.value)
    }

    @Test
    fun `返回后重新选择同一用途或其他用途不会丢失各自草稿`() = runTest(dispatcher) {
        val viewModel: GenerateViewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.WIFI))
        viewModel.onEvent(GenerateEvent.UpdateContent(QrContentCodec.serialize(mapOf("ssid" to "访客网络", "password" to "secret"))))
        viewModel.onEvent(GenerateEvent.UpdateCardTitle("欢迎来小店"))
        holder.executeUpdateConfig { it.copy(darkPixelShape = QrPixelShapeType.BAMBOO) }
        val wifiDraft: QrConfig = holder.config.value
        viewModel.onEvent(GenerateEvent.ReturnToTaskSelection)
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.WIFI))
        assertFalse(viewModel.uiState.value.isChoosingTask)
        assertEquals(wifiDraft, holder.config.value)
        viewModel.onEvent(GenerateEvent.ReturnToTaskSelection)
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.MENU))
        viewModel.onEvent(GenerateEvent.UpdateContent("尚未填完的网址"))
        viewModel.onEvent(GenerateEvent.UpdateCardTitle("今日菜单"))
        val menuDraft: QrConfig = holder.config.value
        viewModel.onEvent(GenerateEvent.ReturnToTaskSelection)
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.WIFI))
        assertEquals(wifiDraft, holder.config.value)
        viewModel.onEvent(GenerateEvent.ReturnToTaskSelection)
        viewModel.onEvent(GenerateEvent.SelectScene(QrScene.MENU))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isChoosingTask)
        assertEquals(menuDraft, viewModel.uiState.value.config)
    }

    @Test
    fun `复杂联系人保留原码且恢复样式不丢未知字段`() {
        val viewModel: GenerateViewModel = createViewModel()
        val raw: String = "BEGIN:VCARD\nVERSION:3.0\nFN:小王\nTEL:13800138000\nTEL:13900139000\nX-CUSTOM:保留我\nEND:VCARD"
        viewModel.onEvent(GenerateEvent.ImportScannedContent(raw))
        assertEquals(QrScene.CONTACT, holder.config.value.card.scene)
        assertEquals(QrContentType.TEXT, holder.config.value.contentType)
        viewModel.onEvent(GenerateEvent.RestoreReadableStyle)
        assertEquals(raw, QrContentCodec.encode(holder.config.value).encode())
    }

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
