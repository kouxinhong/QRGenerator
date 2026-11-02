package com.example.qrgenerator.ui

import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.example.qrgenerator.MainActivity
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.data.model.QrPixelShapeType
import com.example.qrgenerator.domain.QrConfigHolder
import com.example.qrgenerator.domain.QrContentCodec
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/** 从真实输入到真实保存，再从历史恢复；不依赖测试执行顺序。 */
class CreationFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetDraft() {
        if (Build.VERSION.SDK_INT <= 28) {
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            automation.executeShellCommand("pm grant ${compose.activity.packageName} android.permission.WRITE_EXTERNAL_STORAGE").close()
        }
        compose.runOnIdle { holder().executeSetConfig(QrConfig()) }
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasSetTextAction() and hasText("想让别人看到什么？")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun chineseContentCanChangeStyleSaveAndRestore() {
        val content = "你好，小朋友！操场见 ${System.currentTimeMillis()}"
        compose.onNode(hasSetTextAction() and hasText("想让别人看到什么？")).performTextInput(content)
        hideKeyboard()
        waitForSaveButton()
        compose.onNodeWithText("换个样式").performScrollTo().performClick()
        compose.onNodeWithText("节节青竹").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(QrPixelShapeType.BAMBOO, holder().config.value.darkPixelShape) }
        compose.onNodeWithText("完成").performClick()
        waitForSaveButton()
        compose.onNodeWithText("保存到相册").performClick()
        compose.waitUntil(20_000) {
            compose.onAllNodes(hasText("已保存到相册", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("已保存").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText(content).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(content).performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("二维码小卡").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle {
            assertEquals(content, holder().config.value.content)
            assertEquals(QrPixelShapeType.BAMBOO, holder().config.value.darkPixelShape)
        }
        waitForSaveButton()
    }

    @Test
    fun wifiUsesSeparateFieldsAndPreservesSlashInPassword() {
        val network = "家里的 Wi-Fi"
        val password = "study/2026;一起学习"
        compose.onNodeWithText("Wi-Fi").performClick()
        compose.onNode(hasSetTextAction() and hasText("Wi-Fi 名称")).performTextInput(network)
        compose.onNode(hasSetTextAction() and hasText("Wi-Fi 密码")).performScrollTo().performTextInput(password)
        hideKeyboard()
        waitForSaveButton()
        compose.runOnIdle {
            val config = holder().config.value
            val fields = QrContentCodec.fields(config.contentType, config.content)
            assertEquals(QrContentType.WIFI, config.contentType)
            assertEquals(network, fields["ssid"])
            assertEquals(password, fields["password"])
        }
        compose.onNodeWithText("Wi-Fi 密码").performScrollTo()
        compose.onNodeWithContentDescription("显示密码").performClick()
        compose.onNode(hasSetTextAction() and hasText(password)).assertExists()
        compose.onNodeWithContentDescription("隐藏密码").performClick()
    }

    private fun waitForSaveButton() {
        compose.waitUntil(20_000) {
            compose.onAllNodes(hasText("保存到相册") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun hideKeyboard() {
        compose.runOnIdle {
            WindowCompat.getInsetsController(compose.activity.window, compose.activity.window.decorView)
                .hide(WindowInsetsCompat.Type.ime())
        }
        compose.waitForIdle()
    }

    private fun holder(): QrConfigHolder = GlobalContext.get().get()
}
