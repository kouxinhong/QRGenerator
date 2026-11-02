package com.example.qrgenerator.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.example.qrgenerator.data.local.QrHistoryDatabase
import com.example.qrgenerator.data.local.QrHistoryEntity
import com.example.qrgenerator.data.repository.QrHistoryRepository
import com.example.qrgenerator.domain.QrConfigHolderImpl
import com.example.qrgenerator.ui.screens.history.HistoryScreen
import com.example.qrgenerator.ui.screens.history.HistoryViewModel
import com.example.qrgenerator.ui.theme.QRGeneratorTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** 用独立的内存数据库验证管理流程，避免依赖已有历史或测试顺序。 */
class HistoryManagementFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var database: QrHistoryDatabase
    private val store = ViewModelStore()

    @Before fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, QrHistoryDatabase::class.java).build()
        runBlocking {
            listOf("学校一", "家里", "学校二").forEach { title ->
                database.qrHistoryDao().insert(QrHistoryEntity(configJson = "{}", thumbnailPath = "", contentType = "TEXT", contentPreview = title))
            }
        }
        val model = HistoryViewModel(QrHistoryRepository(database.qrHistoryDao()), QrConfigHolderImpl())
        store.put("history", model)
        compose.setContent { QRGeneratorTheme { HistoryScreen(viewModel = model) } }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("学校一").fetchSemanticsNodes().isNotEmpty() }
    }

    @After fun cleanup() {
        compose.runOnIdle { store.clear() }
        database.close()
    }

    @Test fun searchClearsSelectionAndBackExitsManagementBeforeLeavingPage() {
        compose.onNodeWithText("管理").performClick()
        compose.onNodeWithText("学校一").performClick()
        compose.onNodeWithText("已选 1 条").assertExists()
        compose.onNode(hasSetTextAction()).performTextInput("家")
        compose.onNodeWithText("已选 0 条").assertExists()
        compose.onNodeWithText("删除选中的 0 条").assertIsNotEnabled()
        compose.onNodeWithText("全选当前结果").performClick()
        compose.onNodeWithText("已选 1 条").assertExists()
        compose.onNodeWithText("取消全选").performClick()
        compose.onNodeWithText("已选 0 条").assertExists()
        // 第一次返回收起键盘，第二次返回退出管理，不离开历史页。
        hideKeyboard()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("管理").assertExists()
        compose.onNodeWithText("家里").assertExists()
        compose.onNodeWithText("删除选中的 0 条").assertDoesNotExist()
    }

    private fun hideKeyboard() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val activity = compose.activity
            val manager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            manager.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
            activity.window.decorView.clearFocus()
        }
        compose.waitUntil(5_000) {
            var isKeyboardVisible = false
            compose.runOnIdle {
                isKeyboardVisible = ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true
            }
            !isKeyboardVisible
        }
    }

    @Test fun deleteConfirmationCanCancelThenDeletesOnlyCurrentSearchResults() {
        compose.onNode(hasSetTextAction()).performTextInput("学校")
        hideKeyboard()
        compose.onNodeWithText("管理").performClick()
        compose.onNodeWithText("全选当前结果").performClick()
        compose.onNodeWithText("已选 2 条").assertExists()
        compose.onNodeWithText("删除选中的 2 条").performClick()
        compose.onNodeWithText("取消").performClick()
        assertEquals(3, runBlocking { database.qrHistoryDao().getAllHistorySnapshot().size })
        compose.onNodeWithText("删除选中的 2 条").performClick()
        compose.onNodeWithText("删除", substring = false).performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("没有找到相关内容").fetchSemanticsNodes().isNotEmpty() }
        val remaining = runBlocking { database.qrHistoryDao().getAllHistorySnapshot() }
        assertEquals(listOf("家里"), remaining.map { it.contentPreview })
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNodeWithText("家里").assertExists()
        compose.onNodeWithText("管理").assertExists()
    }
}
