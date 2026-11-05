package com.example.qrgenerator.data.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import com.example.qrgenerator.domain.QrImageLoader
import com.example.qrgenerator.domain.QrScanFailure
import com.example.qrgenerator.domain.QrScanResult
import com.example.qrgenerator.domain.QrScanner
import com.king.wechat.qrcode.WeChatQRCodeDetector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.opencv.OpenCV
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

/** 按需加载模型并在后台识别；所有实例共用锁，避免并发访问上游静态检测器。 */
class WeChatQrScanner(context: Context) : QrScanner {
    private val appContext: Context = context.applicationContext
    private val imageLoader: QrImageLoader = QrImageLoader(appContext)

    override suspend fun scan(bitmap: Bitmap): QrScanResult = withContext(Dispatchers.Default) {
        if (!isArmDevice()) return@withContext QrScanResult.Failure(QrScanFailure.UNSUPPORTED_DEVICE)
        detectorMutex.withLock {
            coroutineContext.ensureActive()
            if (!initializeDetector()) return@withLock QrScanResult.Failure(QrScanFailure.INITIALIZATION)
            decodeBitmap(bitmap)
        }
    }

    override suspend fun scan(uri: Uri): QrScanResult = withContext(Dispatchers.IO) {
        if (!isArmDevice()) return@withContext QrScanResult.Failure(QrScanFailure.UNSUPPORTED_DEVICE)
        val bitmap: Bitmap = try {
            (imageLoader.loadImage(uri.toString()) as? BitmapDrawable)?.bitmap
                ?: return@withContext QrScanResult.Failure(QrScanFailure.IMAGE_READ)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            return@withContext QrScanResult.Failure(QrScanFailure.IMAGE_READ)
        }
        try {
            coroutineContext.ensureActive()
            scan(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private fun initializeDetector(): Boolean {
        if (isInitialized) return true
        return try {
            if (!OpenCV.initOpenCV()) return false
            WeChatQRCodeDetector.init(appContext)
            verifyDetector()
            isInitialized = true
            true
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            false
        } catch (_: LinkageError) {
            false
        }
    }

    /** 上游 init 会吞掉异常，通过一次空白图识别确认模型确实可用。 */
    private fun verifyDetector(): Unit {
        val probe: Bitmap = Bitmap.createBitmap(PROBE_SIZE, PROBE_SIZE, Bitmap.Config.ARGB_8888)
        try {
            probe.eraseColor(Color.WHITE)
            WeChatQRCodeDetector.detectAndDecode(probe)
        } finally {
            probe.recycle()
        }
    }

    private fun decodeBitmap(bitmap: Bitmap): QrScanResult {
        var sample: Bitmap? = null
        return try {
            require(!bitmap.isRecycled)
            sample = createSample(bitmap)
            val contents: List<String> = WeChatQRCodeDetector.detectAndDecode(sample)
                .filter { it.isNotEmpty() }.distinct()
            if (contents.isEmpty()) QrScanResult.NotFound else QrScanResult.Success(contents)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            QrScanResult.Failure(QrScanFailure.DECODE)
        } catch (_: LinkageError) {
            isInitialized = false
            QrScanResult.Failure(QrScanFailure.INITIALIZATION)
        } finally {
            sample?.recycle()
        }
    }

    /** 限制原生解码内存，并统一为软件 ARGB 图片；透明区域按白底识别。 */
    private fun createSample(bitmap: Bitmap): Bitmap {
        val scale: Float = minOf(1f, MAX_SAMPLE_EDGE.toFloat() / maxOf(bitmap.width, bitmap.height))
        val width: Int = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val height: Int = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        val sample: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas: Canvas = Canvas(sample)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(bitmap, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
            return sample
        } catch (exception: Exception) {
            sample.recycle()
            throw exception
        }
    }

    private fun isArmDevice(): Boolean = Build.SUPPORTED_ABIS.any { it in ARM_ABIS }

    private companion object {
        val ARM_ABIS: Set<String> = setOf("armeabi-v7a", "arm64-v8a")
        val detectorMutex: Mutex = Mutex()
        var isInitialized: Boolean = false
        const val MAX_SAMPLE_EDGE: Int = 2048
        const val PROBE_SIZE: Int = 64
    }
}
