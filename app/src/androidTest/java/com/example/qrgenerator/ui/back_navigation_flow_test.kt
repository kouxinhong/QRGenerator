package com.example.qrgenerator.ui

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.qrgenerator.MainActivity
import com.example.qrgenerator.data.model.QrCardConfig
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.data.model.QrErrorLevel
import com.example.qrgenerator.data.model.QrScene
import com.example.qrgenerator.data.model.QrSourcePayload
import com.example.qrgenerator.domain.QrConfigHolder
import com.example.qrgenerator.domain.QrConfigHolderImpl
import com.example.qrgenerator.domain.QrContentCodec
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

/** 使用独立草稿验证真实导航，不写入相册、历史或用户设置。 */
class BackNavigationFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val holder: QrConfigHolder = QrConfigHolderImpl()
    private var scenario: ActivityScenario<MainActivity>? = null
    private var originalHolder: QrConfigHolder? = null

    @Before
    fun prepareIsolatedDraft(): Unit {
        originalHolder = GlobalContext.get().get<QrConfigHolder>()
        loadKoinModules(module { single<QrConfigHolder> { holder } })
    }

    @After
    fun restoreOriginalDraft(): Unit {
        try {
            scenario?.close()
        } finally {
            originalHolder?.let { original -> loadKoinModules(module { single<QrConfigHolder> { original } }) }
        }
    }

    @Test
    fun allCreationTasksSupportVisibleAndSystemBack(): Unit {
        launchCreation()
        val tasks: List<Pair<String, String>> = listOf(
            "自由制作 · 文字和更多类型" to "想让别人看到什么？",
            "连 Wi-Fi" to "Wi-Fi 名称",
            "分享页面" to "网址",
            "留联系方式" to "姓名"
        )
        tasks.forEach { (task, field) ->
            assertTaskSelection()
            compose.onNodeWithText(task).performScrollTo().performClick()
            waitForText(field)
            compose.onNodeWithText("返回选择用途").assertIsDisplayed().performClick()
            assertTaskSelection()
            compose.onNodeWithText(task).performScrollTo().performClick()
            waitForText(field)
            pressSystemBack()
            assertTaskSelection()
            requireNotNull(scenario).onActivity { assertFalse(it.isFinishing) }
        }
    }

    @Test
    fun returningWithContentKeepsTaskSelectionAcrossRecreationAndTabSwitches(): Unit {
        val expected: QrConfig = existingWifiDraft()
        holder.executeSetConfig(expected)
        launchCreation()
        assertWifiEditor(expected)
        compose.onNodeWithText("返回选择用途").performClick()
        assertTaskSelection()
        compose.runOnIdle { assertEquals(expected, holder.config.value) }
        requireNotNull(scenario).recreate()
        assertTaskSelection()
        compose.onNodeWithText("设置").performClick()
        waitForText("深色模式")
        compose.onNodeWithText("做二维码").performClick()
        assertTaskSelection()
        compose.runOnIdle { assertEquals(expected, holder.config.value) }
        compose.onNodeWithText("连 Wi-Fi").performScrollTo().performClick()
        assertWifiEditor(expected)
        pressSystemBack()
        assertTaskSelection()
        compose.runOnIdle { assertEquals(expected, holder.config.value) }
    }

    @Test
    fun styleBackAndBottomNavigationReturnToEditorWithoutReopeningStyle(): Unit {
        val expected: QrConfig = existingWifiDraft()
        holder.executeSetConfig(expected)
        launchCreation()
        assertWifiEditor(expected)
        openStyle()
        pressSystemBack()
        assertWifiEditor(expected)
        openStyle()
        compose.onNodeWithText("做二维码").performClick()
        assertWifiEditor(expected)
        openStyle()
        compose.onNodeWithText("设置").performClick()
        waitForText("深色模式")
        compose.onNodeWithText("做二维码").performClick()
        assertWifiEditor(expected)
        pressSystemBack()
        assertTaskSelection()
    }

    @Test
    fun styleDoneRemainsVisibleAfterScrollingAndReturnsToEditor(): Unit {
        val expected: QrConfig = existingWifiDraft()
        holder.executeSetConfig(expected)
        launchCreation()
        openStyle()
        compose.onNodeWithText("更多设置").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("完成").assertIsDisplayed().performClick()
        assertWifiEditor(expected)
    }

    private fun launchCreation(): Unit {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun openStyle(): Unit {
        waitForText("换个样式")
        compose.onNodeWithText("换个样式").performScrollTo().performClick()
        waitForText("选样式")
    }

    private fun pressSystemBack(): Unit {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    private fun assertTaskSelection(): Unit {
        waitForText("这次想做什么？")
        compose.onNodeWithText("这次想做什么？").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("返回选择用途").assertDoesNotExist()
        compose.onNodeWithText("保存到相册").assertDoesNotExist()
    }

    private fun assertWifiEditor(expected: QrConfig): Unit {
        waitForText("返回选择用途")
        compose.onNodeWithText("这次想做什么？").assertDoesNotExist()
        compose.onNodeWithText("选样式").assertDoesNotExist()
        compose.onNode(hasSetTextAction() and hasText("Wi-Fi 名称")).assertTextContains("返回测试网络")
        compose.runOnIdle { assertEquals(expected, holder.config.value) }
    }

    private fun waitForText(text: String): Unit {
        compose.waitUntil(20_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun existingWifiDraft(): QrConfig {
        val content: String = QrContentCodec.serialize(mapOf(
            "ssid" to "返回测试网络", "password" to "draft-secret", "auth" to "WPA", "hidden" to "false"
        ))
        return QrConfig(
            contentType = QrContentType.WIFI,
            content = content,
            imageWidth = 768,
            imageHeight = 768,
            errorCorrectionLevel = QrErrorLevel.HIGH,
            card = QrCardConfig(QrScene.WIFI, true, "返回后保留标题", "返回后保留说明"),
            sourcePayload = QrSourcePayload("WIFI:S:返回测试网络;P:draft-secret;T:WPA;H:false;;", QrContentType.WIFI, content)
        )
    }
}
