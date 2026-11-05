package com.example.qrgenerator.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.qrgenerator.data.scanner.WeChatQrScanner
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** 在 ARM 设备上实际加载模型和 SO，验证相机帧、相册共用的识别能力。 */
@RunWith(AndroidJUnit4::class)
class WeChatQrScannerTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scanner: QrScanner = WeChatQrScanner(context)

    @Before
    fun requireArmDevice(): Unit {
        assumeTrue(Build.SUPPORTED_ABIS.any { it == "armeabi-v7a" || it == "arm64-v8a" })
    }

    @Test
    fun preservesWhitespaceAndUnicodeWithoutRecyclingCallerBitmap(): Unit = runBlocking {
        val payload: String = "  中文菜单 https://example.com/menu\n保留空白  "
        val bitmap: Bitmap = createQrBitmap(payload)
        try {
            assertEquals(QrScanResult.Success(listOf(payload)), scanner.scan(bitmap))
            assertFalse(bitmap.isRecycled)
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun reportsNoCodeForBlankImage(): Unit = runBlocking {
        val bitmap: Bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.WHITE)
            assertEquals(QrScanResult.NotFound, scanner.scan(bitmap))
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun returnsEveryUniquePayloadInOneImage(): Unit = runBlocking {
        val payloads: List<String> = listOf("https://example.com/one", "https://example.com/two")
        val tiles: List<Bitmap> = payloads.map(::createQrBitmap)
        val bitmap: Bitmap = Bitmap.createBitmap(QR_SIZE * tiles.size, QR_SIZE, Bitmap.Config.ARGB_8888)
        try {
            val canvas: Canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            tiles.forEachIndexed { index, tile -> canvas.drawBitmap(tile, (index * QR_SIZE).toFloat(), 0f, null) }
            val result: QrScanResult = scanner.scan(bitmap)
            assertEquals(payloads.toSet(), (result as QrScanResult.Success).contents.toSet())
        } finally {
            tiles.forEach(Bitmap::recycle)
            bitmap.recycle()
        }
    }

    @Test
    fun decodesPhotoContentUriAndReportsUnreadableImage(): Unit = runBlocking {
        val payload: String = "WIFI:T:WPA;S:店铺 Wi-Fi;P:password123;;"
        val bitmap: Bitmap = createQrBitmap(payload)
        val file: File = File.createTempFile("scan_test_", ".png", context.cacheDir)
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            assertEquals(QrScanResult.Success(listOf(payload)), scanner.scan(uri))
            file.writeText("not an image")
            assertEquals(QrScanResult.Failure(QrScanFailure.IMAGE_READ), scanner.scan(uri))
        } finally {
            file.delete()
            bitmap.recycle()
        }
    }

    @Test
    fun serializesConcurrentScansAcrossScannerInstances(): Unit = runBlocking {
        val payload: String = "Concurrent scanning"
        val bitmap: Bitmap = createQrBitmap(payload)
        try {
            val scanners: List<QrScanner> = listOf(scanner, WeChatQrScanner(context))
            val results: List<QrScanResult> = scanners.map { decoder -> async { decoder.scan(bitmap) } }.awaitAll()
            assertEquals(List(scanners.size) { QrScanResult.Success(listOf(payload)) }, results)
        } finally {
            bitmap.recycle()
        }
    }

    private fun createQrBitmap(payload: String): Bitmap {
        val matrix: BitMatrix = QRCodeWriter().encode(
            payload, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE,
            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to 4)
        )
        val pixels: IntArray = IntArray(QR_SIZE * QR_SIZE) { index ->
            if (matrix[index % QR_SIZE, index / QR_SIZE]) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, QR_SIZE, QR_SIZE, Bitmap.Config.ARGB_8888)
    }

    private companion object {
        const val QR_SIZE: Int = 512
    }
}
