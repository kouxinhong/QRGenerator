package com.example.qrgenerator.ui

import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.example.qrgenerator.MainActivity
import com.example.qrgenerator.data.model.QrCardConfig
import com.example.qrgenerator.data.model.QrColorConfig
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.data.model.QrScene
import com.example.qrgenerator.data.repository.QrHistoryRepository
import com.example.qrgenerator.domain.QrConfigHolder
import com.example.qrgenerator.domain.QrContentCodec
import com.example.qrgenerator.domain.QrReadability
import com.example.qrgenerator.domain.QrReadabilityChecker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import java.util.UUID

/** 每条测试只清理自己创建的记录和图片，保留原有草稿与用户数据。 */
class CardCreationFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val token: String = "卡片验收${UUID.randomUUID().toString().take(8)}"
    private var originalConfig: QrConfig? = null
    private var originalImageIds: Set<Long> = emptySet()
    private val exportedPayloads: MutableSet<String> = mutableSetOf()

    @Before
    fun prepareIsolatedDraft() {
        if (Build.VERSION.SDK_INT <= 28) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("pm grant ${compose.activity.packageName} android.permission.WRITE_EXTERNAL_STORAGE").close()
        }
        originalImageIds = readGalleryImages().keys
        compose.runOnIdle {
            originalConfig = holder().config.value
            holder().executeSetConfig(QrConfig())
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("连 Wi-Fi").fetchSemanticsNodes().isNotEmpty() }
    }

    @After
    fun removeOnlyTestArtifacts() {
        val repository: QrHistoryRepository = GlobalContext.get().get()
        runBlocking {
            val records = repository.executeGetAll().first().filter { it.configJson.contains(token) }
            repository.executeDeleteRecords(records)
        }
        readGalleryImages().filterKeys { it !in originalImageIds }.values.forEach { uri ->
            if (matchesExportedPayload(uri)) compose.activity.contentResolver.delete(uri, null, null)
        }
        originalConfig?.let { config -> compose.runOnIdle { holder().executeSetConfig(config) } }
    }

    @Test
    fun wifiCardCanBeSavedSearchedByTitleAndRestored() {
        val network: String = "QA-$token"
        val password: String = "welcome/2026;中文"
        compose.onNodeWithText("连 Wi-Fi").performClick()
        compose.onNode(hasSetTextAction() and hasText("Wi-Fi 名称")).performScrollTo().performTextInput(network)
        compose.onNode(hasSetTextAction() and hasText("Wi-Fi 密码")).performScrollTo().performTextInput(password)
        hideKeyboard()
        changeCardTitle(token)
        waitForVerifiedArtwork()
        assertPreviewShape(isPortrait = true)
        val expected: QrConfig = readConfig()
        assertEquals(QrScene.WIFI, expected.card.scene)
        assertEquals(password, QrContentCodec.fields(expected.contentType, expected.content)["password"])
        exportedPayloads += QrContentCodec.encode(expected).encode()
        compose.onNodeWithText("保存到相册").performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithText("已保存到相册").fetchSemanticsNodes().isNotEmpty() }
        assertSavedPortraitImage()
        compose.runOnIdle { holder().executeSetConfig(QrConfig()) }
        compose.onNodeWithText("已保存").performClick()
        compose.onNode(hasSetTextAction()).performTextInput(token)
        hideKeyboard()
        compose.waitUntil(15_000) { compose.onAllNodes(hasText(token, substring = true) and !hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText(token, substring = true) and !hasSetTextAction()).performScrollTo().performClick()
        waitForVerifiedArtwork()
        val restored: QrConfig = readConfig()
        assertEquals(expected.card, restored.card)
        assertEquals(expected.contentType, restored.contentType)
        assertEquals(expected.content, restored.content)
    }

    @Test
    fun unreadableStyleBlocksExportAndRepairPreservesCardAndContent() {
        val expected: QrConfig = createMenuDraft()
        val invisible: QrColorConfig = QrColorConfig(solidColor = Color.WHITE)
        compose.runOnIdle { holder().executeSetConfig(expected.copy(darkColor = invisible, ballColor = invisible, frameColor = invisible)) }
        compose.waitUntil(20_000) { compose.onAllNodesWithText("当前样式未通过本机识别检查").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("保存到相册").assertIsNotEnabled()
        compose.onNodeWithText("分享").assertIsNotEnabled()
        compose.onNodeWithText("保存为文件").assertIsNotEnabled()
        compose.onNodeWithText("恢复清晰样式").performScrollTo().performClick()
        waitForVerifiedArtwork()
        assertEquals(expected.content, readConfig().content)
        assertEquals(expected.card, readConfig().card)
        compose.onNodeWithText("保存到相册").assertIsEnabled()
    }

    @Test
    fun switchingPlainCodeAndCardRetainsPayloadAndCopy() {
        val expected: QrConfig = createMenuDraft()
        compose.runOnIdle { holder().executeSetConfig(expected) }
        waitForVerifiedArtwork()
        compose.onNodeWithText("带说明的小卡").performScrollTo().performClick()
        waitForVerifiedArtwork()
        assertEquals(expected, readConfig())
        compose.onNodeWithText("只要二维码").performScrollTo().performClick()
        waitForVerifiedArtwork()
        assertFalse(readConfig().card.enabled)
        assertEquals(expected.content, readConfig().content)
        assertEquals(expected.card.title, readConfig().card.title)
        assertPreviewShape(isPortrait = false)
        compose.onNodeWithText("带说明的小卡").performScrollTo().performClick()
        waitForVerifiedArtwork()
        assertEquals(expected.card, readConfig().card)
        assertEquals(expected.content, readConfig().content)
        assertPreviewShape(isPortrait = true)
    }

    private fun changeCardTitle(title: String) {
        if (compose.onAllNodes(hasSetTextAction() and hasText("卡片标题")).fetchSemanticsNodes().isEmpty()) {
            compose.onNodeWithText("修改卡片文字").performScrollTo().performClick()
        }
        compose.onNode(hasSetTextAction() and hasText("卡片标题")).performScrollTo().performTextReplacement(title)
        hideKeyboard()
    }

    private fun waitForVerifiedArtwork() {
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("本机识别检查通过").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodes(hasText("保存到相册") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("本机识别检查通过").performScrollTo().assertIsDisplayed()
    }

    private fun assertPreviewShape(isPortrait: Boolean) {
        compose.runOnIdle {
            val description: String = if (isPortrait) "将要保存的二维码小卡" else "将要保存的二维码"
            val image: ImageView = requireNotNull(findPreview(compose.activity.window.decorView, description))
            val bitmap: Bitmap = (image.drawable as BitmapDrawable).bitmap
            if (isPortrait) assertTrue(bitmap.height > bitmap.width) else assertEquals(bitmap.width, bitmap.height)
        }
    }

    private fun findPreview(view: View, description: String): ImageView? {
        if (view is ImageView && view.contentDescription?.toString()?.startsWith(description) == true) return view
        if (view !is ViewGroup) return null
        return (0 until view.childCount).firstNotNullOfOrNull { index -> findPreview(view.getChildAt(index), description) }
    }

    private fun assertSavedPortraitImage() {
        val candidates: List<Uri> = readGalleryImages().filterKeys { it !in originalImageIds }.values.filter(::matchesExportedPayload)
        assertEquals("本次保存应只产生一张图片", 1, candidates.size)
        val bitmap: Bitmap = requireNotNull(compose.activity.contentResolver.openInputStream(candidates.single())?.use(BitmapFactory::decodeStream))
        try { assertTrue(bitmap.height > bitmap.width) } finally { bitmap.recycle() }
    }

    private fun readGalleryImages(): Map<Long, Uri> {
        val collection: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val result: MutableMap<Long, Uri> = mutableMapOf()
        compose.activity.contentResolver.query(collection, arrayOf(MediaStore.Images.Media._ID), "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?", arrayOf("二维码_%.png"), null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id: Long = cursor.getLong(0)
                result[id] = ContentUris.withAppendedId(collection, id)
            }
        }
        return result
    }

    private fun matchesExportedPayload(uri: Uri): Boolean {
        if (exportedPayloads.isEmpty()) return false
        val bitmap: Bitmap = compose.activity.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream) ?: return false
        return try { exportedPayloads.any { QrReadabilityChecker().execute(bitmap, it) == QrReadability.VERIFIED } } finally { bitmap.recycle() }
    }

    private fun hideKeyboard() {
        compose.runOnIdle { WindowCompat.getInsetsController(compose.activity.window, compose.activity.window.decorView).hide(WindowInsetsCompat.Type.ime()) }
        compose.waitUntil(5_000) {
            compose.runOnIdle { ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) != true }
        }
    }

    private fun createMenuDraft(): QrConfig = QrConfig(
        contentType = QrContentType.URL, content = "https://example.com/menu?test=$token",
        card = QrCardConfig(QrScene.MENU, true, token, "扫一扫，慢慢选")
    )

    private fun readConfig(): QrConfig = compose.runOnIdle { holder().config.value }
    private fun holder(): QrConfigHolder = GlobalContext.get().get()
}
