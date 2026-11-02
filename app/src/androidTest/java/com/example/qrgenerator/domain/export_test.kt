package com.example.qrgenerator.domain

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.qrgenerator.data.model.ExportFormat
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrErrorLevel
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** 回读实际导出的文件和分享 URI，验证内容、尺寸与授权都可以使用。 */
@RunWith(AndroidJUnit4::class)
class ExportTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val useCase: ExportUseCase = ExportUseCase(context)
    private val content: String = "导出后也能扫一扫，中文内容保持完整。"

    @Test
    fun savedPngAndPdfRetainTheOriginalQrContent() {
        val source: Bitmap = createSourceBitmap()
        val png: File = File.createTempFile("qr_export_test_", ".png", context.cacheDir)
        val pdf: File = File.createTempFile("qr_export_test_", ".pdf", context.cacheDir)
        try {
            useCase.executeSaveDocument(source, Uri.fromFile(png), ExportFormat.PNG)
            val decodedPng: Bitmap = requireNotNull(BitmapFactory.decodeFile(png.absolutePath))
            try {
                assertEquals(source.width, decodedPng.width)
                assertEquals(source.height, decodedPng.height)
                assertEquals(content, decodeQr(decodedPng))
            } finally {
                decodedPng.recycle()
            }
            useCase.executeSaveDocument(source, Uri.fromFile(pdf), ExportFormat.PDF)
            PdfRenderer(ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                assertEquals(1, renderer.pageCount)
                renderer.openPage(0).use { page ->
                    val renderedPage: Bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    try {
                        renderedPage.eraseColor(Color.WHITE)
                        page.render(renderedPage, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        assertEquals(content, decodeQr(renderedPage))
                    } finally {
                        renderedPage.recycle()
                    }
                }
            }
        } finally {
            source.recycle()
            png.delete()
            pdf.delete()
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun shareIntentContainsReadableGrantedUriAndThumbnailIsStored() {
        val source: Bitmap = createSourceBitmap()
        var shareUri: Uri? = null
        var thumbnailFile: File? = null
        try {
            val intent: Intent = useCase.executeCreateShareIntent(source)
            shareUri = requireNotNull(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
            assertEquals(Intent.ACTION_SEND, intent.action)
            assertEquals("image/png", intent.type)
            assertEquals("content", shareUri.scheme)
            assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            assertNotNull(intent.clipData)
            assertEquals(shareUri, intent.clipData?.getItemAt(0)?.uri)
            val sharedBitmap: Bitmap = context.contentResolver.openInputStream(shareUri)?.use {
                requireNotNull(BitmapFactory.decodeStream(it))
            } ?: error("分享 URI 必须可读取")
            try {
                assertEquals(source.width, sharedBitmap.width)
                assertEquals(source.height, sharedBitmap.height)
                assertEquals(content, decodeQr(sharedBitmap))
            } finally {
                sharedBitmap.recycle()
            }
            thumbnailFile = File(useCase.executeSaveThumbnail(source))
            assertTrue(thumbnailFile.isFile && thumbnailFile.length() > 0)
            val thumbnail: Bitmap = requireNotNull(BitmapFactory.decodeFile(thumbnailFile.absolutePath))
            try {
                assertEquals(256, maxOf(thumbnail.width, thumbnail.height))
                assertEquals(source.width.toFloat() / source.height, thumbnail.width.toFloat() / thumbnail.height, 0.001f)
            } finally {
                thumbnail.recycle()
            }
        } finally {
            source.recycle()
            shareUri?.let { context.contentResolver.delete(it, null, null) }
            thumbnailFile?.delete()
        }
    }

    private fun createSourceBitmap(): Bitmap {
        val config: QrConfig = QrConfig(content = content, errorCorrectionLevel = QrErrorLevel.HIGH)
        return useCase.executeDrawableToBitmap(QrGeneratorUseCase(context).execute(config), 768, 1024)
    }

    private fun decodeQr(bitmap: Bitmap): String {
        val pixels: IntArray = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val luminance: RGBLuminanceSource = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(luminance)), mapOf(DecodeHintType.TRY_HARDER to true)).text
    }
}
