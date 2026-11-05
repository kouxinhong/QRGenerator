package com.example.qrgenerator.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.example.qrgenerator.MainActivity
import com.example.qrgenerator.data.model.QrCardConfig
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.data.model.QrErrorLevel
import com.example.qrgenerator.data.model.QrPrintLayout
import com.example.qrgenerator.data.model.QrScene
import com.example.qrgenerator.data.model.QrSourcePayload
import com.example.qrgenerator.domain.QrConfigHolder
import com.example.qrgenerator.domain.QrConfigHolderImpl
import com.example.qrgenerator.domain.QrContentCodec
import com.example.qrgenerator.domain.QrScanResult
import com.example.qrgenerator.domain.QrScanner
import com.example.qrgenerator.ui.scanner.QrScanActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** 独立单例在 Activity 创建前注入，避免已有草稿和用例运行顺序影响首次进入的判断。 */
class ImportCreationFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val holder: QrConfigHolder = QrConfigHolderImpl()
    private val scanner: FrameScanner = FrameScanner()
    private var scenario: ActivityScenario<MainActivity>? = null
    private var originalHolder: QrConfigHolder? = null
    private var originalScanner: QrScanner? = null

    @Before
    fun prepareIsolatedDependencies(): Unit {
        val command: String = "pm grant ${context.packageName} android.permission.CAMERA"
        ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ).use { it.readBytes() }
        originalHolder = GlobalContext.get().get<QrConfigHolder>()
        originalScanner = GlobalContext.get().get<QrScanner>()
        loadKoinModules(module {
            single<QrConfigHolder> { holder }
            single<QrScanner> { scanner }
        })
    }

    @After
    fun restoreOriginalDependencies(): Unit {
        try {
            // 断言在扫码结果页失败时，也关闭它，避免下一条测试继续使用测试替身。
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val monitor = ActivityLifecycleMonitorRegistry.getInstance()
                Stage.values().flatMap { monitor.getActivitiesInStage(it) }
                    .filterIsInstance<QrScanActivity>().distinct().forEach { it.finish() }
            }
            scenario?.close()
        } finally {
            originalHolder?.let { original -> loadKoinModules(module { single<QrConfigHolder> { original } }) }
            originalScanner?.let { original -> loadKoinModules(module { single<QrScanner> { original } }) }
        }
    }

    @Test
    fun firstVisitShowsTasksAndWifiStartsWithAnEditableCard(): Unit {
        launchCreation()
        waitForText("这次想做什么？")
        compose.onNodeWithText("连 Wi-Fi").assertExists()
        compose.onNodeWithText("分享页面").assertExists()
        compose.onNodeWithText("留联系方式").assertExists()
        compose.onNodeWithText("扫一扫导入").assertExists()
        compose.onNodeWithText("从图片导入").assertExists()
        compose.onNodeWithText("保存到相册").assertDoesNotExist()
        compose.onNodeWithText("成品预览").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).assertDoesNotExist()

        compose.onNodeWithText("连 Wi-Fi").performClick()
        waitForText("Wi-Fi 名称")
        compose.onNodeWithText("这次想做什么？").assertDoesNotExist()
        compose.onNode(hasSetTextAction() and hasText("Wi-Fi 名称")).assertExists()
        compose.onNode(hasSetTextAction() and hasText("Wi-Fi 密码")).assertExists()
        compose.onNodeWithText("带说明的小卡").assertIsSelected()
        compose.runOnIdle {
            assertEquals(QrScene.WIFI, holder.config.value.card.scene)
            assertEquals(QrContentType.WIFI, holder.config.value.contentType)
            assertTrue(holder.config.value.card.enabled)
        }
        compose.onNode(hasSetTextAction() and hasText("Wi-Fi 名称")).performScrollTo().performTextInput("首次访客网络")
        compose.runOnIdle {
            val config: QrConfig = holder.config.value
            assertTrue(QrContentCodec.isStructured(config.content))
            assertEquals("首次访客网络", QrContentCodec.fields(config.contentType, config.content)["ssid"])
        }
    }

    @Test
    fun restoredDraftOpensTheEditorAndSurvivesActivityRecreation(): Unit {
        val expected: QrConfig = existingWifiDraft()
        holder.executeSetConfig(expected)
        launchCreation()
        waitForText("Wi-Fi 名称")
        assertRestoredWifiEditor(expected)

        requireNotNull(scenario).recreate()
        waitForText("Wi-Fi 名称")
        assertRestoredWifiEditor(expected)
        compose.onNodeWithText("修改卡片文字").performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText("卡片标题")).assertTextContains(expected.card.title)
        compose.onNode(hasSetTextAction() and hasText("扫码提示")).assertTextContains(expected.card.subtitle)
    }

    @Test
    fun scanningWifiPreservesCardAndOriginalPayloadAndUndoRestoresTheWholeDraft(): Unit {
        val previous: QrConfig = existingWifiDraft()
        val password: String = "scan;pass2026"
        val rawPayload: String = " \nWIFI:T:WPA;S: 新访客\\;网络 ;P:scan\\;pass2026;H:true;;\n "
        holder.executeSetConfig(previous)
        scanner.result.set(QrScanResult.Success(listOf(rawPayload)))
        launchCreation()
        waitForText("扫一扫导入")
        compose.onNodeWithContentDescription("显示密码").performScrollTo().performClick()
        compose.onNodeWithContentDescription("隐藏密码").assertExists()
        compose.onNodeWithText("扫一扫导入").performScrollTo().performClick()
        waitForText("填入当前 Wi-Fi 卡")
        compose.onNodeWithText(password).assertDoesNotExist()
        compose.onNodeWithText(rawPayload).assertDoesNotExist()
        assertTrue("导入结果应由真实 CameraX 图像分析帧触发", scanner.frameCount.get() > 0)

        compose.onNodeWithText("填入当前 Wi-Fi 卡").performScrollTo().performClick()
        waitForText("撤销")
        compose.onNodeWithText("这次想做什么？").assertDoesNotExist()
        compose.onNodeWithContentDescription("显示密码").assertExists()
        compose.onNodeWithContentDescription("隐藏密码").assertDoesNotExist()
        compose.runOnIdle {
            val imported: QrConfig = holder.config.value
            assertEquals(previous.card, imported.card)
            assertEquals(previous.imageWidth, imported.imageWidth)
            assertEquals(previous.errorCorrectionLevel, imported.errorCorrectionLevel)
            assertEquals(QrContentType.WIFI, imported.contentType)
            assertEquals(" 新访客;网络 ", QrContentCodec.fields(imported.contentType, imported.content)["ssid"])
            assertEquals(password, QrContentCodec.fields(imported.contentType, imported.content)["password"])
            assertEquals(rawPayload, QrContentCodec.encode(imported).encode())
        }
        compose.onNodeWithContentDescription("显示密码").performScrollTo().performClick()
        compose.onNodeWithContentDescription("隐藏密码").assertExists()
        requireNotNull(scenario).recreate()
        waitForText("Wi-Fi 名称")
        compose.onNodeWithContentDescription("显示密码").assertExists()
        compose.onNodeWithContentDescription("隐藏密码").assertDoesNotExist()
        compose.onNodeWithText("撤销").assertExists()
        compose.runOnIdle { assertEquals(rawPayload, QrContentCodec.encode(holder.config.value).encode()) }
        compose.onNodeWithText("撤销").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(previous, holder.config.value) }
        compose.onNodeWithText("撤销").assertDoesNotExist()
        compose.onNode(hasSetTextAction() and hasText("Wi-Fi 名称")).assertTextContains("旧访客网络")
    }

    @Test
    fun complexWifiOriginalCollapsesAfterRecreationAndRepeatedImport(): Unit {
        val rawPayload: String = " WIFI:T:WPA2-EAP;S:Office;P:enterprise-private-password;I:employee;E:PEAP;;\n"
        val summaryNote: String = "已保留全部原始内容，可直接制作。此二维码暂不支持逐项编辑。"
        scanner.result.set(QrScanResult.Success(listOf(rawPayload)))
        launchCreation()
        waitForText("扫一扫导入")
        compose.onNodeWithText("扫一扫导入").performScrollTo().performClick()
        waitForText("保留原内容制作")
        compose.onNodeWithText("保留原内容制作").performScrollTo().performClick()
        waitForText(summaryNote)
        compose.onNodeWithText(rawPayload).assertDoesNotExist()
        compose.onNodeWithText("查看原文（含连接信息）").performScrollTo().performClick()
        compose.onNodeWithText(rawPayload).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(rawPayload, QrContentCodec.encode(holder.config.value).encode()) }

        requireNotNull(scenario).recreate()
        waitForText(summaryNote)
        compose.onNodeWithText("查看原文（含连接信息）").assertExists()
        compose.onNodeWithText("收起原文").assertDoesNotExist()
        compose.onNodeWithText(rawPayload).assertDoesNotExist()
        compose.onNodeWithText("查看原文（含连接信息）").performScrollTo().performClick()
        compose.onNodeWithText(rawPayload).performScrollTo().assertIsDisplayed()

        val previousFrames: Int = scanner.frameCount.get()
        compose.onNodeWithText("扫一扫导入").performScrollTo().performClick()
        waitForText("保留原内容制作")
        compose.onNodeWithText("保留原内容制作").performScrollTo().performClick()
        waitForText(summaryNote)
        compose.onNodeWithText("查看原文（含连接信息）").assertExists()
        compose.onNodeWithText("收起原文").assertDoesNotExist()
        compose.onNodeWithText(rawPayload).assertDoesNotExist()
        assertTrue("再次导入仍应经过真实 CameraX 图像分析帧", scanner.frameCount.get() > previousFrames)
        compose.runOnIdle { assertEquals(rawPayload, QrContentCodec.encode(holder.config.value).encode()) }
    }

    @Test
    fun cancellingFourCardPdfPreviewLeavesTheDraftUnchanged(): Unit {
        val expected: QrConfig = existingWifiDraft()
        holder.executeSetConfig(expected)
        launchCreation()
        compose.waitUntil(20_000) {
            compose.onAllNodes(hasText("保存为文件") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("保存为文件").performScrollTo().performClick()
        compose.onNodeWithText("PDF 文档 · 适合打印").performScrollTo().performClick()
        compose.onNodeWithText("A4 四张小卡").performScrollTo().performClick()
        val layout: QrPrintLayout = QrPrintLayout.A4_FOUR
        compose.onNodeWithTag("print_preview").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("A4 打印预览，${layout.cardCount} 张，每张 ${layout.dimensionsLabel}，含裁切线").assertExists()
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithTag("print_preview").assertDoesNotExist()
        compose.runOnIdle { assertEquals(expected, holder.config.value) }
    }

    private fun launchCreation(): Unit {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun waitForText(text: String): Unit {
        compose.waitUntil(20_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun assertRestoredWifiEditor(expected: QrConfig): Unit {
        compose.onNodeWithText("这次想做什么？").assertDoesNotExist()
        compose.onNode(hasSetTextAction() and hasText("Wi-Fi 名称")).assertTextContains("旧访客网络")
        compose.onNodeWithText("带说明的小卡").assertIsSelected()
        compose.runOnIdle { assertEquals(expected, holder.config.value) }
    }

    private fun existingWifiDraft(): QrConfig {
        val content: String = QrContentCodec.serialize(mapOf(
            "ssid" to "旧访客网络", "password" to "previous-secret", "auth" to "WPA", "hidden" to "false"
        ))
        return QrConfig(
            contentType = QrContentType.WIFI,
            content = content,
            imageWidth = 768,
            imageHeight = 768,
            errorCorrectionLevel = QrErrorLevel.HIGH,
            card = QrCardConfig(QrScene.WIFI, true, "小店访客网络", "欢迎光临，扫码连接"),
            sourcePayload = QrSourcePayload("WIFI:S:旧访客网络;P:previous-secret;T:WPA;H:false;;", QrContentType.WIFI, content)
        )
    }

    /** 仅隔离解码内容；拍摄、帧分析、导航和结果回传均走应用的真实链路。 */
    private class FrameScanner : QrScanner {
        val result: AtomicReference<QrScanResult> = AtomicReference(QrScanResult.NotFound)
        val frameCount: AtomicInteger = AtomicInteger()

        override suspend fun scan(bitmap: Bitmap): QrScanResult {
            check(!bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0)
            frameCount.incrementAndGet()
            return result.get()
        }

        override suspend fun scan(uri: Uri): QrScanResult = result.get()
    }
}
