package com.example.qrgenerator.ui.scanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qrgenerator.data.model.QrScene
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScanScreen(
    activity: Activity,
    onClose: () -> Unit,
    onUseContent: (String) -> Unit,
    targetScene: QrScene? = null,
    initialSource: QrScanSource = QrScanSource.CAMERA,
    viewModel: ScanViewModel = koinViewModel()
): Unit {
    val state: ScanUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasCamera: Boolean = remember { activity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    var hasPermission: Boolean by remember { mutableStateOf(hasCameraPermission(activity)) }
    var hasRequestedPermission: Boolean by rememberSaveable { mutableStateOf(false) }
    var shouldExplainPermission: Boolean by remember { mutableStateOf(ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) }
    var isCameraSelected: Boolean by rememberSaveable { mutableStateOf(initialSource == QrScanSource.CAMERA) }
    var hasStarted: Boolean by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
        shouldExplainPermission = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    }
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia(), viewModel::readImage)
    val snackbar: SnackbarHostState = remember { SnackbarHostState() }
    val chooseImage: () -> Unit = {
        viewModel.pauseForImageSelection()
        try {
            imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (_: Exception) {
            viewModel.showError("无法打开图片选择器，请稍后重试")
        }
    }
    val useCamera: () -> Unit = {
        isCameraSelected = true
        viewModel.retry()
        if (!hasPermission) {
            hasRequestedPermission = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, activity) {
        val observer: LifecycleEventObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = hasCameraPermission(activity)
                shouldExplainPermission = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        if (!hasStarted) {
            hasStarted = true
            if (state.contents.isEmpty() && state.message == null) {
                if (initialSource == QrScanSource.PHOTO) chooseImage()
                else if (hasCamera && !hasPermission && !hasRequestedPermission) {
                    hasRequestedPermission = true
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("识别已有二维码") }, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "返回") } }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = chooseImage, enabled = !state.isReadingImage && !state.isChoosingImage, modifier = Modifier.fillMaxWidth()) { Text("从图片识别") }
                Text("仅在本机识别，不会上传图片。", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                state.isReadingImage || state.isChoosingImage -> ScanProgress(state.isReadingImage, Modifier.weight(1f))
                state.contents.isNotEmpty() -> ScanResults(state.contents, targetScene, onUseContent,
                    { if (isCameraSelected) viewModel.retry() else chooseImage() }, snackbar, Modifier.weight(1f), isCameraSelected)
                state.message != null -> ScanMessage(state.message.orEmpty(), if (isCameraSelected) "重新扫描" else "重新选择图片",
                    { if (isCameraSelected) viewModel.retry() else chooseImage() }, Modifier.weight(1f))
                !isCameraSelected -> ScanMessage("选择一张含二维码的图片，也可以使用相机识别", if (hasCamera) "使用相机识别" else null, useCamera, Modifier.weight(1f))
                !hasCamera -> ScanMessage("这台设备没有可用相机，可以选择图片识别", null, {}, Modifier.weight(1f))
                !hasPermission -> {
                    val shouldOpenSettings: Boolean = hasRequestedPermission && !shouldExplainPermission
                    ScanMessage("需要相机权限才能扫一扫，也可以直接从图片识别", if (shouldOpenSettings) "前往设置开启相机" else "允许使用相机", {
                        if (shouldOpenSettings) {
                            try {
                                activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${activity.packageName}")))
                            } catch (_: Exception) {
                                viewModel.showError("无法打开设置，请在系统设置中允许本应用使用相机")
                            }
                        } else {
                            hasRequestedPermission = true
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }, Modifier.weight(1f))
                }
                else -> {
                    Text("将二维码放入画面，识别后选择要制作的内容", style = MaterialTheme.typography.bodyMedium)
                    ScanCameraPreview(viewModel, state.session, Modifier.fillMaxWidth().weight(1f))
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun ScanProgress(isReadingImage: Boolean, modifier: Modifier): Unit {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(Modifier.size(36.dp))
        Spacer(Modifier.height(16.dp))
        Text(if (isReadingImage) "正在识别图片…" else "请在图片选择器中选择二维码")
    }
}

@Composable
private fun ScanMessage(message: String, action: String?, onAction: () -> Unit, modifier: Modifier): Unit {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

private fun hasCameraPermission(activity: Activity): Boolean = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
