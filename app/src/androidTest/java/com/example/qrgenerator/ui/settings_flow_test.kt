package com.example.qrgenerator.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.qrgenerator.MainActivity
import com.example.qrgenerator.data.model.*
import com.example.qrgenerator.data.repository.QrHistoryRepository
import com.example.qrgenerator.data.repository.SettingsRepository
import com.example.qrgenerator.domain.QrConfigHolder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/** 验证真实设置入口、应用联动、Activity 重建以及可取消的恢复操作。 */
class SettingsFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()
    private var originalSettings: AppSettings? = null
    private var insertedHistoryId: Long? = null

    @Before
    fun prepareSettings() {
        runBlocking {
            originalSettings = settingsRepository().executeGetSettings().first()
            settingsRepository().executeResetSettings()
        }
        compose.runOnIdle { holder().executeSetConfig(QrConfig(content = "设置测试二维码")) }
        compose.waitUntil(20_000) {
            compose.onAllNodes(hasText("保存到相册") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun restoreSettings() {
        runBlocking {
            insertedHistoryId?.let { id ->
                historyRepository().executeGetAll().first().firstOrNull { it.id == id }?.let { historyRepository().executeDelete(it) }
            }
            originalSettings?.let { original ->
                settingsRepository().executeUpdateThemeMode(original.themeMode)
                settingsRepository().executeUpdateImageSize(original.defaultImageSize)
                settingsRepository().executeUpdateExportFormat(original.defaultExportFormat)
                settingsRepository().executeUpdateErrorLevel(original.defaultErrorLevel)
            }
        }
    }

    @Test
    fun settingsApplyToGeneratorAndSurviveActivityRecreation() {
        openSettings()
        choose("深色模式", ThemeMode.DARK.displayName)
        waitForSettings { it.themeMode == ThemeMode.DARK }
        compose.runOnIdle { assertEquals(0xFF111111.toInt(), compose.activity.window.statusBarColor) }
        choose("图片清晰度", "超清（2048 像素）")
        choose("文件保存格式", ExportFormat.PDF.displayName)
        choose("遮挡后识别能力", QrErrorLevel.HIGH.displayName)
        waitForSettings { it == AppSettings(ThemeMode.DARK, QrErrorLevel.HIGH, ExportFormat.PDF, 2048) }
        compose.runOnIdle {
            assertEquals(2048, holder().config.value.imageWidth)
            assertEquals(2048, holder().config.value.imageHeight)
            assertEquals(QrErrorLevel.HIGH, holder().config.value.errorCorrectionLevel)
        }
        compose.onNodeWithText("做二维码").performClick()
        waitForGenerator()
        compose.onNodeWithText("保存为文件").performScrollTo().performClick()
        compose.onNodeWithText("PDF 文档 · 适合打印").assertIsSelected()
        compose.onNodeWithText("取消").performClick()
        compose.activityRule.scenario.recreate()
        waitForGenerator()
        openSettings()
        compose.onNodeWithText("深色").assertExists()
        compose.onNodeWithText("超清 · 2048 像素").assertExists()
        compose.onNodeWithText(ExportFormat.PDF.displayName).assertExists()
        compose.onNodeWithText(QrErrorLevel.HIGH.displayName).assertExists()
        compose.runOnIdle {
            assertEquals(0xFF111111.toInt(), compose.activity.window.statusBarColor)
            assertEquals(2048, holder().config.value.imageWidth)
        }
    }

    @Test
    fun restoreRequiresConfirmationPreservesWorkAndAboutShowsInstalledVersion() {
        val inputConfig: QrConfig = QrConfig(
            content = "恢复设置也要保留我", darkPixelShape = QrPixelShapeType.BAMBOO,
            logo = QrLogoConfig(source = LogoSource.BUILTIN_ICON, builtinIconName = "leaf"),
            imageWidth = 768, imageHeight = 1536, errorCorrectionLevel = QrErrorLevel.HIGH
        )
        compose.runOnIdle { holder().executeSetConfig(inputConfig) }
        insertedHistoryId = runBlocking { historyRepository().executeSave(inputConfig, "") }
        openSettings()
        choose("深色模式", ThemeMode.DARK.displayName)
        choose("文件保存格式", ExportFormat.PDF.displayName)
        openRow("恢复默认设置")
        compose.onNodeWithText("恢复默认设置？").assertExists()
        compose.onNodeWithText("取消").performClick()
        compose.runOnIdle { assertEquals(inputConfig, holder().config.value) }
        assertEquals(ThemeMode.DARK, runBlocking { settingsRepository().executeGetSettings().first().themeMode })
        openRow("恢复默认设置")
        compose.onNodeWithText("恢复默认").performClick()
        waitForSettings { it == AppSettings() }
        compose.runOnIdle {
            assertEquals(inputConfig.copy(imageWidth = 1024, imageHeight = 1024, errorCorrectionLevel = QrErrorLevel.AUTO), holder().config.value)
        }
        assertTrue(runBlocking { historyRepository().executeGetAll().first().any { it.id == insertedHistoryId } })
        openRow("关于二维码生成器")
        @Suppress("DEPRECATION")
        val version: String = compose.activity.packageManager.getPackageInfo(compose.activity.packageName, 0).versionName ?: "未知"
        compose.onNode(hasText("版本 $version", substring = true) and hasAnyAncestor(isDialog())).assertExists()
        compose.onNodeWithText("知道了").performClick()
        openRow("数据与权限")
        compose.onNode(hasText("二维码制作和识别都在本机完成", substring = true) and hasAnyAncestor(isDialog())).assertExists()
        compose.onNode(hasText("从图片识别不需要相机权限", substring = true) and hasAnyAncestor(isDialog())).assertExists()
        compose.onNodeWithText("知道了").performClick()
        openRow("怎么做二维码")
        compose.onNodeWithText("三个步骤就能做好").assertExists()
        compose.onNodeWithText("知道了").performClick()
        compose.onNodeWithText("做二维码").performClick()
        waitForGenerator()
        compose.onNodeWithText("保存为文件").performScrollTo().performClick()
        compose.onNodeWithText("PNG 图片 · 适合发送和使用").assertIsSelected()
        compose.onNodeWithText("取消").performClick()
    }

    private fun openSettings() {
        compose.onNode(hasText("设置") and hasClickAction()).performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasText("深色模式") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openRow(title: String) {
        compose.onNode(hasText(title) and hasClickAction()).performScrollTo().performClick()
    }

    private fun choose(title: String, option: String) {
        openRow(title)
        compose.onNode(hasText(option) and hasAnyAncestor(isDialog())).performScrollTo().performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodes(hasText(title) and hasClickAction() and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForSettings(predicate: (AppSettings) -> Boolean) {
        compose.waitUntil(15_000) { runBlocking { predicate(settingsRepository().executeGetSettings().first()) } }
        compose.waitForIdle()
    }

    private fun waitForGenerator() {
        compose.waitUntil(20_000) {
            compose.onAllNodes(hasText("保存到相册") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun settingsRepository(): SettingsRepository = GlobalContext.get().get()
    private fun historyRepository(): QrHistoryRepository = GlobalContext.get().get()
    private fun holder(): QrConfigHolder = GlobalContext.get().get()
}
