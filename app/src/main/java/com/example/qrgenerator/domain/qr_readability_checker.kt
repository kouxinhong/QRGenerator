package com.example.qrgenerator.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlin.math.roundToInt

enum class QrReadability { VERIFIED, UNREADABLE }

/** 对最终成品回读且逐字比对内容，不等同于所有相机和打印条件下的保证。 */
class QrReadabilityChecker {
    fun execute(bitmap: Bitmap, expectedPayload: String): QrReadability {
        val sample: Bitmap = createSample(bitmap)
        return try {
            val pixels: IntArray = IntArray(sample.width * sample.height)
            sample.getPixels(pixels, 0, sample.width, 0, 0, sample.width, sample.height)
            val source: RGBLuminanceSource = RGBLuminanceSource(sample.width, sample.height, pixels)
            val reader: QRCodeReader = QRCodeReader()
            val decoded: String = reader.decode(BinaryBitmap(HybridBinarizer(source)), mapOf(DecodeHintType.TRY_HARDER to true, DecodeHintType.CHARACTER_SET to "UTF-8")).text
            if (decoded == expectedPayload) QrReadability.VERIFIED else QrReadability.UNREADABLE
        } catch (_: ReaderException) {
            QrReadability.UNREADABLE
        } finally {
            sample.recycle()
        }
    }

    /** 限制回读内存，并按实际白底呈现透明图像。不会修改或回收调用方的图片。 */
    private fun createSample(bitmap: Bitmap): Bitmap {
        val scale: Float = minOf(1f, MAX_SAMPLE_EDGE.toFloat() / maxOf(bitmap.width, bitmap.height))
        val width: Int = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val height: Int = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        val sample: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(sample)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
        return sample
    }

    private companion object {
        const val MAX_SAMPLE_EDGE: Int = 2048
    }
}
