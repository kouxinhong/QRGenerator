package com.example.qrgenerator.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.InputStream
import java.io.IOException

/** 读取相册和私有目录图片，并限制解码尺寸以控制内存占用。 */
internal class QrImageLoader(private val context: Context?) {
    fun loadImage(path: String?): Drawable? {
        if (path.isNullOrBlank()) return null
        val appContext: Context = requireNotNull(context) { "无法读取图片，请重新选择图片" }
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source: ImageDecoder.Source = if (path.startsWith("content:")) {
                ImageDecoder.createSource(appContext.contentResolver, Uri.parse(path))
            } else {
                ImageDecoder.createSource(resolveFile(path))
            }
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val ratio: Float = MAX_IMAGE_SIZE.toFloat() / maxOf(info.size.width, info.size.height)
                if (ratio < 1f) decoder.setTargetSize((info.size.width * ratio).toInt().coerceAtLeast(1), (info.size.height * ratio).toInt().coerceAtLeast(1))
            }
        } else {
            decodeLegacy(appContext, path)
        }
        return BitmapDrawable(appContext.resources, bitmap)
    }

    fun createBuiltin(name: String?): Drawable? {
        if (name.isNullOrBlank()) return null
        val bitmap: Bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(bitmap)
        val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF078653.toInt() }
        canvas.drawColor(Color.WHITE)
        when (name) {
            "heart" -> drawHeart(canvas, paint)
            "star" -> drawStar(canvas, paint)
            "leaf" -> drawLeaf(canvas, paint)
            else -> drawSmile(canvas, paint)
        }
        return BitmapDrawable(context?.resources, bitmap)
    }

    private fun decodeLegacy(appContext: Context, path: String): Bitmap {
        val bounds: BitmapFactory.Options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(appContext, path).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "这张图片无法读取，请换一张图片" }
        var sampleSize: Int = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_IMAGE_SIZE) sampleSize *= 2
        val options: BitmapFactory.Options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap: Bitmap = openStream(appContext, path).use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IllegalArgumentException("这张图片无法读取，请换一张图片")
        return rotateLegacyImage(appContext, path, bitmap)
    }

    private fun rotateLegacyImage(appContext: Context, path: String, bitmap: Bitmap): Bitmap {
        val orientation: Int = try {
            openStream(appContext, path).use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        } catch (exception: IOException) {
            // 部分图片格式没有 EXIF 信息，按原方向显示即可。
            ExifInterface.ORIENTATION_NORMAL
        }
        val matrix: Matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(-90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        val rotated: Bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun openStream(appContext: Context, path: String): InputStream =
        if (path.startsWith("content:")) {
            appContext.contentResolver.openInputStream(Uri.parse(path))
                ?: throw IllegalArgumentException("无法读取图片，请重新选择图片")
        } else {
            resolveFile(path).inputStream()
        }

    private fun resolveFile(path: String): File =
        if (path.startsWith("file:")) File(requireNotNull(Uri.parse(path).path)) else File(path)

    private fun drawHeart(canvas: Canvas, paint: Paint) {
        val path: Path = Path().apply {
            moveTo(128f, 214f)
            cubicTo(16f, 144f, 30f, 45f, 88f, 50f)
            cubicTo(110f, 50f, 122f, 66f, 128f, 80f)
            cubicTo(134f, 66f, 146f, 50f, 168f, 50f)
            cubicTo(226f, 45f, 240f, 144f, 128f, 214f)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawStar(canvas: Canvas, paint: Paint) {
        val path: Path = Path()
        repeat(10) { index ->
            val angle: Double = Math.PI * index / 5.0 - Math.PI / 2.0
            val radius: Double = if (index % 2 == 0) 96.0 else 46.0
            val x: Float = (128.0 + kotlin.math.cos(angle) * radius).toFloat()
            val y: Float = (128.0 + kotlin.math.sin(angle) * radius).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawLeaf(canvas: Canvas, paint: Paint) {
        val path: Path = Path().apply {
            moveTo(53f, 206f)
            cubicTo(16f, 110f, 124f, 35f, 215f, 41f)
            cubicTo(225f, 145f, 145f, 223f, 53f, 206f)
            close()
        }
        canvas.drawPath(path, paint)
        paint.color = Color.WHITE
        paint.strokeWidth = 12f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(70f, 188f, 176f, 82f, paint)
    }

    private fun drawSmile(canvas: Canvas, paint: Paint) {
        canvas.drawCircle(128f, 128f, 91f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(96f, 111f, 10f, paint)
        canvas.drawCircle(160f, 111f, 10f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 12f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawArc(81f, 103f, 175f, 179f, 15f, 150f, false, paint)
    }

    private companion object {
        const val MAX_IMAGE_SIZE: Int = 2048
    }
}
