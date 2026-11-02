package com.example.qrgenerator.ui.screens.settings

import androidx.lifecycle.ViewModelStore
import com.example.qrgenerator.data.model.*
import com.example.qrgenerator.data.repository.SettingsRepository
import com.example.qrgenerator.domain.QrConfigHolderImpl
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private val store: ViewModelStore = ViewModelStore()
    private val repository: SettingsRepository = mockk()
    private val settings: MutableStateFlow<AppSettings> = MutableStateFlow(AppSettings())
    private val holder: QrConfigHolderImpl = QrConfigHolderImpl()

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
    fun `再次选择相同默认尺寸和纠错也会应用到当前自定义二维码`() = runTest(dispatcher) {
        val inputConfig: QrConfig = sampleConfig()
        holder.executeSetConfig(inputConfig)
        coEvery { repository.executeUpdateImageSize(1024) } just Runs
        coEvery { repository.executeUpdateErrorLevel(QrErrorLevel.AUTO) } just Runs
        val viewModel: SettingsViewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(SettingsEvent.UpdateImageSize(1024))
        runCurrent()
        assertEquals(1024, holder.config.value.imageWidth)
        assertEquals(1024, holder.config.value.imageHeight)
        assertEquals(QrErrorLevel.HIGH, holder.config.value.errorCorrectionLevel)
        viewModel.onEvent(SettingsEvent.UpdateErrorLevel(QrErrorLevel.AUTO))
        runCurrent()
        assertEquals(inputConfig.copy(imageWidth = 1024, imageHeight = 1024, errorCorrectionLevel = QrErrorLevel.AUTO), holder.config.value)
        assertEquals(AppSettings(), viewModel.settings.value)
    }

    @Test
    fun `保存失败不改变当前二维码且重试成功后才应用`() = runTest(dispatcher) {
        val inputConfig: QrConfig = sampleConfig()
        holder.executeSetConfig(inputConfig)
        var shouldFail: Boolean = true
        coEvery { repository.executeUpdateImageSize(2048) } coAnswers {
            if (shouldFail) throw IOException("磁盘暂时无法写入")
            settings.value = settings.value.copy(defaultImageSize = 2048)
        }
        val viewModel: SettingsViewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(SettingsEvent.UpdateImageSize(2048))
        assertTrue(viewModel.status.value.isSaving)
        runCurrent()
        assertEquals(inputConfig, holder.config.value)
        assertFalse(viewModel.status.value.isSaving)
        assertTrue(viewModel.status.value.errorMessage.orEmpty().contains("重试"))
        shouldFail = false
        viewModel.onEvent(SettingsEvent.Retry)
        runCurrent()
        assertEquals(inputConfig.copy(imageWidth = 2048, imageHeight = 2048), holder.config.value)
        assertEquals(2048, viewModel.settings.value.defaultImageSize)
        assertNull(viewModel.status.value.errorMessage)
        assertFalse(viewModel.status.value.isSaving)
        coVerify(exactly = 2) { repository.executeUpdateImageSize(2048) }
    }

    @Test
    fun `恢复默认保留内容小图背景和形状`() = runTest(dispatcher) {
        val inputConfig: QrConfig = sampleConfig()
        holder.executeSetConfig(inputConfig)
        settings.value = AppSettings(ThemeMode.DARK, QrErrorLevel.HIGH, ExportFormat.PDF, 2048)
        coEvery { repository.executeResetSettings() } coAnswers { settings.value = AppSettings() }
        val viewModel: SettingsViewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(SettingsEvent.RestoreDefaults)
        runCurrent()
        assertEquals(AppSettings(), viewModel.settings.value)
        assertEquals(inputConfig.copy(imageWidth = 1024, imageHeight = 1024, errorCorrectionLevel = QrErrorLevel.AUTO), holder.config.value)
        assertEquals("已恢复默认设置", viewModel.status.value.message)
        assertNull(viewModel.status.value.errorMessage)
        coVerify(exactly = 1) { repository.executeResetSettings() }
    }

    @Test
    fun `恢复默认写入失败保留用户设置和二维码`() = runTest(dispatcher) {
        val inputConfig: QrConfig = sampleConfig()
        val inputSettings: AppSettings = AppSettings(ThemeMode.DARK, QrErrorLevel.HIGH, ExportFormat.PDF, 2048)
        holder.executeSetConfig(inputConfig)
        settings.value = inputSettings
        coEvery { repository.executeResetSettings() } throws IOException("存储空间不足")
        val viewModel: SettingsViewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(SettingsEvent.RestoreDefaults)
        runCurrent()
        assertEquals(inputSettings, viewModel.settings.value)
        assertEquals(inputConfig, holder.config.value)
        assertNotNull(viewModel.status.value.errorMessage)
        assertFalse(viewModel.status.value.isSaving)
    }

    @Test
    fun `读取IOException自动重试恢复且不会更改当前作品`() = runTest(dispatcher) {
        var attempts: Int = 0
        val inputConfig: QrConfig = sampleConfig()
        val expectedSettings: AppSettings = AppSettings(themeMode = ThemeMode.DARK, defaultImageSize = 2048)
        holder.executeSetConfig(inputConfig)
        every { repository.executeGetSettings() } returns flow {
            attempts += 1
            if (attempts == 1) throw IOException("读取暂时失败")
            emit(expectedSettings)
            awaitCancellation()
        }
        val viewModel: SettingsViewModel = createViewModel()
        runCurrent()
        assertFalse(viewModel.status.value.hasLoaded)
        assertTrue(viewModel.status.value.errorMessage.orEmpty().contains("重试"))
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(2, attempts)
        assertTrue(viewModel.status.value.hasLoaded)
        assertNull(viewModel.status.value.errorMessage)
        assertEquals(expectedSettings, viewModel.settings.value)
        assertEquals(inputConfig, holder.config.value)
    }

    @Test
    fun `点击重试立即重新读取而不等待自动重试`() = runTest(dispatcher) {
        var attempts: Int = 0
        every { repository.executeGetSettings() } returns flow {
            attempts += 1
            if (attempts == 1) throw IOException("读取暂时失败")
            emit(AppSettings(defaultExportFormat = ExportFormat.PDF))
            awaitCancellation()
        }
        val viewModel: SettingsViewModel = createViewModel()
        runCurrent()
        assertEquals(1, attempts)
        viewModel.onEvent(SettingsEvent.Retry)
        runCurrent()
        assertEquals(2, attempts)
        assertEquals(ExportFormat.PDF, viewModel.settings.value.defaultExportFormat)
        assertNull(viewModel.status.value.errorMessage)
        assertTrue(viewModel.status.value.hasLoaded)
    }

    @Test
    fun `保存过程中重复点击不会发起第二次写入`() = runTest(dispatcher) {
        val completion: CompletableDeferred<Unit> = CompletableDeferred()
        coEvery { repository.executeUpdateImageSize(2048) } coAnswers { completion.await() }
        val viewModel: SettingsViewModel = createViewModel()
        runCurrent()
        viewModel.onEvent(SettingsEvent.UpdateImageSize(2048))
        runCurrent()
        viewModel.onEvent(SettingsEvent.UpdateImageSize(2048))
        runCurrent()
        assertTrue(viewModel.status.value.isSaving)
        coVerify(exactly = 1) { repository.executeUpdateImageSize(2048) }
        completion.complete(Unit)
        runCurrent()
        assertFalse(viewModel.status.value.isSaving)
        assertEquals(2048, holder.config.value.imageWidth)
    }

    private fun createViewModel(): SettingsViewModel = SettingsViewModel(repository, holder).also { store.put("settings", it) }

    private fun sampleConfig(): QrConfig = QrConfig(
        content = "保留我的二维码", darkPixelShape = QrPixelShapeType.BAMBOO,
        logo = QrLogoConfig(source = LogoSource.BUILTIN_ICON, builtinIconName = "leaf"),
        background = QrBackgroundConfig(imagePath = "/private/saved-background.png"),
        imageWidth = 768, imageHeight = 1536, errorCorrectionLevel = QrErrorLevel.HIGH
    )
}
