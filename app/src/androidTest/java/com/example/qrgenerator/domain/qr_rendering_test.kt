package com.example.qrgenerator.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.qrgenerator.data.model.*
import com.example.qrgenerator.ui.screens.style.StylePreset
import com.github.alexzhirkevich.customqrgenerator.HighlightingType
import com.github.alexzhirkevich.customqrgenerator.style.Neighbors
import com.github.alexzhirkevich.customqrgenerator.style.QrShape
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** 真正绘制二维码并回读，避免只检查对象存在而遗漏可扫描性。 */
@RunWith(AndroidJUnit4::class)
class QrRenderingTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val useCase: QrGeneratorUseCase = QrGeneratorUseCase(context)
    private val creativeShapes: List<QrPixelShapeType> = listOf(
        QrPixelShapeType.BAMBOO, QrPixelShapeType.KITE, QrPixelShapeType.PETAL, QrPixelShapeType.RIPPLE
    )

    @Test
    fun creativeShapesDecodeChineseAndUrlsAtTwoSizes() {
        val contents: List<String> = listOf("1", "你好，小朋友！", "https://example.com/class/3", "今日作业：阅读二十分钟，带好水杯。")
        creativeShapes.forEach { shape ->
            contents.forEach { content ->
                listOf(512, 1024).forEach { size ->
                    val config: QrConfig = QrConfig(content = content, darkPixelShape = shape, errorCorrectionLevel = QrErrorLevel.HIGH)
                    assertEquals("$shape at $size", content, decode(render(config, size)))
                }
            }
        }
    }

    @Test
    fun completePresetsDecodeWithTheirOwnColorsAndFinderShapes() {
        StylePreset.entries.forEach { preset ->
            listOf("1", "https://example.com/活动报名").forEach { content ->
                val config: QrConfig = preset.applyTo(QrConfig(content = content))
                assertEquals(preset.title, content, decode(render(config, 512)))
            }
        }
    }

    @Test
    fun shortestCodeHasQuietZoneAndDecodes() {
        val config: QrConfig = QrConfig(content = "1")
        val bitmap: Bitmap = render(config, 512)
        assertEquals("1", decode(bitmap))
        assertTrue(config.padding / 2f / (1f - config.padding) * 21f >= 4f)
        assertEquals(Color.WHITE, bitmap.getPixel(0, 0))
    }

    @Test
    fun creativeShapesStayInsideTheirCell() {
        creativeShapes.forEach { shape ->
            val path = useCase.mapPixelShape(shape).createPath(100f, Neighbors.Empty)
            val bounds: RectF = RectF()
            path.computeBounds(bounds, true)
            assertFalse(path.isEmpty)
            assertTrue("$shape bounds", bounds.left >= 0f && bounds.top >= 0f && bounds.right <= 100f && bounds.bottom <= 100f)
            val bitmap: Bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).drawPath(path, android.graphics.Paint().apply { color = Color.BLACK })
            assertEquals("$shape must keep its center", Color.BLACK, bitmap.getPixel(50, 50))
        }
    }

    @Test
    fun builtinLogoAndGradientsRemainReadable() {
        listOf(QrColorType.SOLID, QrColorType.LINEAR_GRADIENT, QrColorType.RADIAL_GRADIENT, QrColorType.SWEEP_GRADIENT).forEach { colorType ->
            val config: QrConfig = QrConfig(
                content = "班级阅读打卡",
                darkColor = QrColorConfig(type = colorType, gradientStartColor = Color.BLACK, gradientEndColor = 0xFF075E46.toInt()),
                errorCorrectionLevel = QrErrorLevel.HIGH,
                logo = QrLogoConfig(source = LogoSource.BUILTIN_ICON, builtinIconName = "smile", size = .18f, paddingType = LogoPaddingType.NATURAL, paddingValue = .12f)
            )
            assertEquals(config.content, decode(render(config, 1024)))
        }
    }

    @Test
    fun circleOutlineAndRectangularCanvasDecode() {
        val config: QrConfig = QrConfig(content = "圆形二维码", codeShape = CodeShapeType.CIRCLE, circlePadding = 1.15f, errorCorrectionLevel = QrErrorLevel.HIGH)
        val options = useCase.mapQrOptions(config)
        assertTrue(options.codeShape is QrShape.Circle)
        assertEquals(config.content, decode(render(config, 1024, 1280)))
    }

    @Test
    fun imageBackgroundAndLogoAreLoadedAndHighlighted() {
        val imageFile: File = File(context.cacheDir, "qr_render_test.png")
        val source: Bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFFE8F3EF.toInt()) }
        try {
            imageFile.outputStream().use { source.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val config: QrConfig = QrConfig(
                content = "背景图片测试",
                background = QrBackgroundConfig(imagePath = imageFile.absolutePath),
                logo = QrLogoConfig(imagePath = imageFile.absolutePath, size = .15f, paddingType = LogoPaddingType.ACCURATE, paddingValue = .1f),
                highlighting = QrHighlightingConfig(cornerEyes = HighlightingMode.DEFAULT, versionEyes = HighlightingMode.DEFAULT, timingLines = HighlightingMode.DEFAULT, alpha = 1f),
                errorCorrectionLevel = QrErrorLevel.HIGH
            )
            val options = useCase.mapQrOptions(config)
            assertNotNull(options.background.drawable)
            assertNotNull(options.logo.drawable)
            assertTrue(options.highlighting.cornerEyes is HighlightingType.Default)
            assertEquals(1f, options.highlighting.alpha, .001f)
            assertEquals(config.content, decode(render(config, 1024)))
        } finally {
            imageFile.delete()
        }
    }

    @Test
    fun everyAdvancedOptionReachesTheLibrary() {
        val config: QrConfig = QrConfig(
            content = "设置检查", padding = .2f, offsetX = .3f, offsetY = -.2f,
            fourthEyeEnabled = true, centralSymmetry = false,
            lightColor = QrColorConfig(solidColor = 0xFFF5F5F5.toInt()),
            highlighting = QrHighlightingConfig(cornerEyes = HighlightingMode.CUSTOM, shape = QrPixelShapeType.ROUND_CORNERS, alpha = .9f)
        )
        val options = useCase.mapQrOptions(config)
        assertEquals(.2f, options.padding, .001f)
        assertEquals(.3f, options.offset.x, .001f)
        assertEquals(-.2f, options.offset.y, .001f)
        assertTrue(options.fourthEyeEnabled)
        assertFalse(options.shapes.centralSymmetry)
        assertTrue(options.highlighting.cornerEyes is HighlightingType.Styled)
        assertEquals(0xFFF5F5F5.toInt(), options.colors.light.createPaint(100f, 100f).color)
    }

    private fun render(config: QrConfig, width: Int, height: Int = width): Bitmap {
        val drawable = useCase.execute(config)
        val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    private fun decode(bitmap: Bitmap): String {
        val pixels: IntArray = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source: RGBLuminanceSource = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)), mapOf(DecodeHintType.TRY_HARDER to true)).text
    }
}
