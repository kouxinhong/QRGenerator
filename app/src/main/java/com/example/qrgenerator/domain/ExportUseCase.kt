package com.example.qrgenerator.domain

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.qrgenerator.data.model.ExportFormat
import com.example.qrgenerator.data.model.QrPrintLayout
import java.io.File
import java.io.OutputStream

/** 保存真实 PNG / PDF；文件保存使用系统选择器，无需存储权限。 */
class ExportUseCase(private val context: Context) {
    fun executeDrawableToBitmap(drawable: Drawable, width: Int = 1024, height: Int = 1024): Bitmap {
        require(width in 256..4096 && height in 256..4096) { "图片尺寸应在 256 到 4096 之间" }
        val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    fun executeSaveToGallery(bitmap: Bitmap, filename: String): Uri {
        val values: ContentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QRGenerator")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri: Uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建相册图片，请检查存储空间")
        try {
            context.contentResolver.openOutputStream(uri)?.use { writePng(bitmap, it) } ?: error("无法写入相册")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            return uri
        } catch (exception: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw exception
        }
    }

    fun executeSaveDocument(bitmap: Bitmap, uri: Uri, format: ExportFormat, printLayout: QrPrintLayout = QrPrintLayout.SINGLE_CARD) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            when (format) {
                ExportFormat.PNG -> writePng(bitmap, stream)
                ExportFormat.PDF -> QrPrintRenderer().writePdf(bitmap, printLayout, stream)
                ExportFormat.SVG -> error("请选择 PNG 图片或 PDF 文档")
            }
        } ?: error("无法打开这个文件，请重新选择保存位置")
    }

    fun executeCreateShareIntent(bitmap: Bitmap): Intent {
        val directory: File = File(context.cacheDir, "shares").apply { mkdirs() }
        directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 24 * 60 * 60 * 1000L }?.forEach { it.delete() }
        val file: File = File(directory, "二维码_${System.currentTimeMillis()}.png")
        file.outputStream().use { writePng(bitmap, it) }
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("二维码", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun executeSaveThumbnail(bitmap: Bitmap): String {
        val directory: File = File(context.filesDir, "qr_history").apply { mkdirs() }
        val file: File = File(directory, "${System.currentTimeMillis()}.png")
        val ratio: Float = 256f / maxOf(bitmap.width, bitmap.height)
        val thumbnail: Bitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt().coerceAtLeast(1), (bitmap.height * ratio).toInt().coerceAtLeast(1), true)
        try { file.outputStream().use { writePng(thumbnail, it) } } finally { if (thumbnail !== bitmap) thumbnail.recycle() }
        return file.absolutePath
    }

    private fun writePng(bitmap: Bitmap, stream: OutputStream) {
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "图片写入失败，请检查存储空间" }
    }

}
