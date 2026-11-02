package com.example.qrgenerator.domain

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.qrgenerator.data.model.QrCardConfig
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrScene
import com.example.qrgenerator.ui.screens.style.StylePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** 从真实卡片回读二维码，验证含中文的场景内容而非仅验证图片存在。 */
@RunWith(AndroidJUnit4::class)
class QrCardRenderingTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val generator: QrGeneratorUseCase = QrGeneratorUseCase(context)
    private val exporter: ExportUseCase = ExportUseCase(context)
    private val renderer: QrCardRenderer = QrCardRenderer()
    private val checker: QrReadabilityChecker = QrReadabilityChecker()

    @Test
    fun everySceneCardRetainsItsFullPayload() {
        QrScene.entries.forEach { scene ->
            val inputConfig: QrConfig = createSceneConfig(scene)
            assertCardDecodes(inputConfig, 768, 768)
        }
    }

    @Test
    fun styledCardAndRectangularCodeKeepCompleteQuietZone() {
        val inputConfig: QrConfig = StylePreset.BAMBOO.applyTo(createSceneConfig(QrScene.FREE))
        assertCardDecodes(inputConfig, 768, 1024)
    }

    @Test
    fun longCopyCannotCoverTheCodeOrOverflowMaximumCanvas() {
        val inputConfig: QrConfig = createSceneConfig(QrScene.FREE).copy(
            card = QrCardConfig(QrScene.FREE, true, "很长的中文标题🌿\n".repeat(20), "很长的使用说明，扫码试一试。\n".repeat(30))
        )
        assertCardDecodes(inputConfig, 2048, 4096)
    }

    @Test
    fun plainCodeReturnsOriginalBitmapAndStillPassesReadback() {
        val inputConfig: QrConfig = QrConfig(content = "纯二维码，保留原始尺寸")
        val inputBitmap: Bitmap = renderQr(inputConfig, 512, 768)
        try {
            val actualBitmap: Bitmap = renderer.execute(inputBitmap, inputConfig.card)
            assertSame(inputBitmap, actualBitmap)
            assertEquals(QrReadability.VERIFIED, checker.execute(actualBitmap, inputConfig.content))
            assertFalse(inputBitmap.isRecycled)
        } finally {
            inputBitmap.recycle()
        }
    }

    @Test
    fun readableCodeWithDifferentContentIsRejected() {
        val inputBitmap: Bitmap = renderQr(QrConfig(content = "正确内容"), 512, 512)
        try {
            assertEquals(QrReadability.UNREADABLE, checker.execute(inputBitmap, "其他内容"))
            assertFalse(inputBitmap.isRecycled)
        } finally {
            inputBitmap.recycle()
        }
    }

    @Test
    fun blankImageCannotBeMarkedVerified() {
        val inputBitmap: Bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        try {
            inputBitmap.eraseColor(Color.WHITE)
            assertEquals(QrReadability.UNREADABLE, checker.execute(inputBitmap, "需要出现的内容"))
        } finally {
            inputBitmap.recycle()
        }
    }

    private fun assertCardDecodes(config: QrConfig, width: Int, height: Int) {
        val inputBitmap: Bitmap = renderQr(config, width, height)
        val actualBitmap: Bitmap = renderer.execute(inputBitmap, config.card)
        try {
            assertTrue(actualBitmap.width <= 4096 && actualBitmap.height <= 4096)
            assertEquals(Color.WHITE, actualBitmap.getPixel(0, 0))
            assertEquals(config.card.scene.title, QrReadability.VERIFIED, checker.execute(actualBitmap, QrContentCodec.encode(config).encode()))
            assertFalse(inputBitmap.isRecycled)
        } finally {
            if (actualBitmap !== inputBitmap) actualBitmap.recycle()
            inputBitmap.recycle()
        }
    }

    private fun renderQr(config: QrConfig, width: Int, height: Int): Bitmap =
        exporter.executeDrawableToBitmap(generator.execute(config), width, height)

    private fun createSceneConfig(scene: QrScene): QrConfig {
        val content: String = when (scene) {
            QrScene.FREE -> "欢迎来玩，周六见！"
            QrScene.WIFI -> QrContentCodec.serialize(mapOf("ssid" to "巷口小馆", "password" to "a/b;中文密码", "auth" to "WPA"))
            QrScene.MENU -> QrContentCodec.serialize(mapOf("url" to "https://example.com/menu?table=8"))
            QrScene.CONTACT -> QrContentCodec.serialize(mapOf("name" to "小林", "phone" to "13800138000", "company" to "巷口小馆"))
        }
        return QrConfig(contentType = scene.contentType, content = content, card = QrCardConfig(scene, true))
    }
}
