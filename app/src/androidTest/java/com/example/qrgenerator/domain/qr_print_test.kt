package com.example.qrgenerator.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.qrgenerator.data.model.ExportFormat
import com.example.qrgenerator.data.model.QrCardConfig
import com.example.qrgenerator.data.model.QrPrintLayout
import com.example.qrgenerator.data.model.QrPrintPlan
import com.example.qrgenerator.data.model.QrPrintPlanner
import com.example.qrgenerator.data.model.QrPrintRect
import com.example.qrgenerator.data.model.QrScene
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.roundToInt

/** 将实际 PDF 回读为图片，逐张验证完整码，而非只检查文件头。 */
@RunWith(AndroidJUnit4::class)
class QrPrintTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val exporter: ExportUseCase = ExportUseCase(context)
    private val checker: QrReadabilityChecker = QrReadabilityChecker()
    private val payload: String = "https://example.com/menu?table=8"

    @Test
    fun singleCardPdfKeepsA4PageAndReadableArtwork(): Unit {
        assertPdfCardsDecode(QrPrintLayout.SINGLE_CARD)
    }

    @Test
    fun fourCardPdfHasFourIndependentlyReadableCopies(): Unit {
        assertPdfCardsDecode(QrPrintLayout.A4_FOUR)
    }

    @Test
    fun previewUsesSameCardPlacementAndDoesNotRecycleSource(): Unit {
        val source: Bitmap = createArtwork()
        val plan: QrPrintPlan = QrPrintPlanner.createPlan(QrPrintLayout.A4_FOUR)
        val preview: Bitmap = Bitmap.createBitmap(plan.pageWidthPoints * SCALE, plan.pageHeightPoints * SCALE, Bitmap.Config.ARGB_8888)
        try {
            val canvas: Canvas = Canvas(preview)
            canvas.scale(SCALE.toFloat(), SCALE.toFloat())
            QrPrintRenderer().drawPage(canvas, source, plan)
            assertCardsDecode(preview, plan)
            assertFalse(source.isRecycled)
            assertEquals(Color.WHITE, preview.getPixel(0, 0))
        } finally {
            preview.recycle()
            source.recycle()
        }
    }

    @Test
    fun pngExportIgnoresPrintPresetAndPreservesOriginalDimensions(): Unit {
        val source: Bitmap = createArtwork()
        val file: File = File.createTempFile("qr_print_", ".png", context.cacheDir)
        try {
            exporter.executeSaveDocument(source, Uri.fromFile(file), ExportFormat.PNG, QrPrintLayout.A4_FOUR)
            val actual: Bitmap = requireNotNull(BitmapFactory.decodeFile(file.absolutePath))
            try {
                assertEquals(source.width, actual.width)
                assertEquals(source.height, actual.height)
                assertEquals(QrReadability.VERIFIED, checker.execute(actual, payload))
                assertFalse(source.isRecycled)
            } finally {
                actual.recycle()
            }
        } finally {
            source.recycle()
            file.delete()
        }
    }

    private fun assertPdfCardsDecode(layout: QrPrintLayout): Unit {
        val source: Bitmap = createArtwork()
        val file: File = File.createTempFile("qr_print_", ".pdf", context.cacheDir)
        try {
            exporter.executeSaveDocument(source, Uri.fromFile(file), ExportFormat.PDF, layout)
            assertTrue(file.length() > 0)
            PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                assertEquals(1, renderer.pageCount)
                renderer.openPage(0).use { page ->
                    assertEquals(595, page.width)
                    assertEquals(842, page.height)
                    val rendered: Bitmap = Bitmap.createBitmap(page.width * SCALE, page.height * SCALE, Bitmap.Config.ARGB_8888)
                    try {
                        rendered.eraseColor(Color.WHITE)
                        page.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        assertCardsDecode(rendered, QrPrintPlanner.createPlan(layout))
                    } finally {
                        rendered.recycle()
                    }
                }
            }
            assertFalse(source.isRecycled)
        } finally {
            source.recycle()
            file.delete()
        }
    }

    private fun assertCardsDecode(page: Bitmap, plan: QrPrintPlan): Unit {
        plan.cards.forEachIndexed { index, card ->
            val tile: Bitmap = cropCard(page, card)
            try {
                assertEquals("第 ${index + 1} 张小卡必须完整可扫", QrReadability.VERIFIED, checker.execute(tile, payload))
            } finally {
                tile.recycle()
            }
        }
    }

    private fun cropCard(page: Bitmap, card: QrPrintRect): Bitmap = Bitmap.createBitmap(
        page, (card.left * SCALE).roundToInt(), (card.top * SCALE).roundToInt(),
        (card.width * SCALE).roundToInt(), (card.height * SCALE).roundToInt()
    )

    private fun createArtwork(): Bitmap {
        val matrix: BitMatrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, mapOf(EncodeHintType.MARGIN to 4))
        val pixels: IntArray = IntArray(QR_SIZE * QR_SIZE) { index -> if (matrix[index % QR_SIZE, index / QR_SIZE]) Color.BLACK else Color.WHITE }
        val code: Bitmap = Bitmap.createBitmap(pixels, QR_SIZE, QR_SIZE, Bitmap.Config.ARGB_8888)
        return try {
            QrCardRenderer().execute(code, QrCardConfig(QrScene.MENU, true, "今日菜单", "扫一扫，查看菜单"))
        } finally {
            code.recycle()
        }
    }

    private companion object {
        const val QR_SIZE: Int = 768
        const val SCALE: Int = 3
    }
}
