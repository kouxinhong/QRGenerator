package com.example.qrgenerator.ui.screens.style

import android.graphics.BitmapFactory
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

internal data class StyleImageActions(val selectImage: () -> Unit, val takePhoto: () -> Unit)

/** 仅访问用户选中的图片，复制到应用目录后不再依赖临时 URI 授权。 */
@Composable
internal fun rememberStyleImageActions(
    onSelected: (String, Boolean) -> Unit,
    onError: (String) -> Unit
): StyleImageActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    val importImage: (Uri, Boolean) -> Unit = { uri, isCamera ->
        scope.launch {
            try {
                val path = withContext(Dispatchers.IO) { copySelectedImage(context, uri) }
                onSelected(path, isCamera)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                onError("图片没有添加成功，请选择一张小于 20 MB 的图片重试")
            } finally {
                if (isCamera) {
                    cameraPath?.let { File(it).delete() }
                    cameraPath = null
                }
            }
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importImage(uri, false)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val path = cameraPath
        if (success && path != null) importImage(Uri.fromFile(File(path)), true)
        else {
            path?.let { File(it).delete() }
            cameraPath = null
        }
    }
    return StyleImageActions(
        selectImage = { gallery.launch(arrayOf("image/*")) },
        takePhoto = {
            try {
                val file = File.createTempFile("qr_photo_", ".jpg", context.cacheDir)
                cameraPath = file.absolutePath
                camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
            } catch (exception: Exception) {
                cameraPath?.let { File(it).delete() }
                cameraPath = null
                onError("无法打开相机，请从相册选择图片")
            }
        }
    )
}

private fun copySelectedImage(context: Context, uri: Uri): String {
    val directory = File(context.filesDir, "qr_images").apply { mkdirs() }
    val file = File(directory, "${UUID.randomUUID()}.image")
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var total = 0L
                var count = input.read(buffer)
                while (count != -1) {
                    total += count
                    require(total <= 20L * 1024L * 1024L)
                    output.write(buffer, 0, count)
                    count = input.read(buffer)
                }
                require(total > 0)
            }
        } ?: error("图片无法读取")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0)
        return file.absolutePath
    } catch (exception: Exception) {
        file.delete()
        throw exception
    }
}
