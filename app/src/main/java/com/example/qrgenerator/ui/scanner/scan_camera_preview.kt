package com.example.qrgenerator.ui.scanner

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.qrgenerator.domain.QrScanFailure
import com.example.qrgenerator.domain.QrScanResult
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** 分析线程串行处理帧；退出预览时只解绑本页持有的用例。 */
@Composable
internal fun ScanCameraPreview(viewModel: ScanViewModel, session: Long, modifier: Modifier = Modifier): Unit {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView: PreviewView = remember { PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
    var camera: Camera? by remember { mutableStateOf(null) }
    var isTorchEnabled: Boolean by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner, previewView, session) {
        val isActive: AtomicBoolean = AtomicBoolean(true)
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val preview: Preview = Preview.Builder().build().apply { setSurfaceProvider(previewView.surfaceProvider) }
        val analysis: ImageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
                ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
            ).build())
            .build()
        var provider: ProcessCameraProvider? = null
        analysis.setAnalyzer(executor) { image -> analyzeFrame(image, isActive, viewModel, session, previewView) }
        providerFuture.addListener({
            if (isActive.get()) {
                try {
                    provider = providerFuture.get()
                    val selector: CameraSelector = if (provider!!.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                    camera = provider!!.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                } catch (_: Exception) {
                    viewModel.acceptCameraError(session, "无法打开相机，请检查相机权限或选择图片识别")
                }
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            isActive.set(false)
            analysis.clearAnalyzer()
            camera?.cameraControl?.enableTorch(false)
            provider?.unbind(preview, analysis)
            camera = null
            executor.shutdown()
        }
    }
    LaunchedEffect(camera, isTorchEnabled) {
        camera?.cameraControl?.enableTorch(isTorchEnabled)
    }
    Box(modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            Button(onClick = { isTorchEnabled = !isTorchEnabled }, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                Text(if (isTorchEnabled) "关闭手电筒" else "打开手电筒")
            }
        }
    }
}

private fun analyzeFrame(image: ImageProxy, isActive: AtomicBoolean, viewModel: ScanViewModel, session: Long, preview: PreviewView): Unit {
    var bitmap: Bitmap? = null
    try {
        if (!isActive.get()) return
        val frame: Bitmap = createUprightBitmap(image)
        bitmap = frame
        val result: QrScanResult = runBlocking { viewModel.scanFrame(frame) }
        if (result != QrScanResult.NotFound) preview.post {
            if (isActive.get()) viewModel.acceptCameraResult(session, result)
        }
    } catch (_: Exception) {
        preview.post {
            if (isActive.get()) viewModel.acceptCameraResult(session, QrScanResult.Failure(QrScanFailure.DECODE))
        }
    } finally {
        bitmap?.recycle()
        image.close()
    }
}

private fun createUprightBitmap(image: ImageProxy): Bitmap {
    val bitmap: Bitmap = image.toBitmap()
    if (image.imageInfo.rotationDegrees == 0) return bitmap
    return try {
        val matrix: Matrix = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
        val rotated: Bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        rotated
    } catch (exception: Exception) {
        bitmap.recycle()
        throw exception
    }
}
