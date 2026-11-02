package com.example.qrgenerator.ui.screens.generate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qrgenerator.data.model.ExportFormat
import com.example.qrgenerator.data.model.QrScene
import com.example.qrgenerator.domain.QrReadability
import com.example.qrgenerator.ui.components.ContentTypeChipGroup
import com.example.qrgenerator.ui.components.ExportDialog
import org.koin.androidx.compose.koinViewModel

/** 每个动作按使用顺序排列；高级样式由明确入口进入。 */
@Composable
fun GenerateScreen(viewModel: GenerateViewModel = koinViewModel(), onOpenStyle: () -> Unit = {}) {
    val state: GenerateUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar: SnackbarHostState = remember { SnackbarHostState() }
    var showExport: Boolean by rememberSaveable { mutableStateOf(false) }
    var permissionMessage: String? by remember { mutableStateOf(null) }
    val savePng = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { viewModel.onEvent(GenerateEvent.SaveDocument(it)) }
    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { viewModel.onEvent(GenerateEvent.SaveDocument(it)) }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.onEvent(GenerateEvent.SaveToGallery)
        else permissionMessage = "未获得相册权限，可以点“保存为文件”选择位置"
    }
    LaunchedEffect(state.shareIntent) {
        state.shareIntent?.let { intent ->
            runCatching { context.startActivity(Intent.createChooser(intent, "分享二维码")) }
                .onFailure { snackbar.showSnackbar("没有可用的分享应用，可以先保存图片") }
            viewModel.onEvent(GenerateEvent.ClearShareIntent)
        }
    }
    LaunchedEffect(state.documentRequest) {
        state.documentRequest?.let { request ->
            runCatching {
                if (request.format == ExportFormat.PDF) savePdf.launch(request.filename) else savePng.launch(request.filename)
            }.onFailure {
                viewModel.onEvent(GenerateEvent.SaveDocument(null))
                permissionMessage = "无法打开文件选择器，可以先保存到相册"
            }
            viewModel.onEvent(GenerateEvent.ClearDocumentRequest)
        }
    }
    LaunchedEffect(state.message, state.errorMessage, permissionMessage) {
        val message: String = state.errorMessage ?: state.message ?: permissionMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        viewModel.onEvent(GenerateEvent.ClearMessage)
        viewModel.onEvent(GenerateEvent.ClearError)
        permissionMessage = null
    }
    val canSave: Boolean = state.qrDrawable != null && state.readability == QrReadability.VERIFIED && !state.isGenerating && !state.isSaving
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            requestPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else viewModel.onEvent(GenerateEvent.SaveToGallery)
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(8.dp)
                ) { Text(if (state.isSaving) "正在保存…" else "保存到相册", style = MaterialTheme.typography.bodyLarge) }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("二维码小卡", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                Text("连 Wi-Fi、看菜单、留联系方式，做成一张小卡。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ScenePicker(state.config.card.scene) { viewModel.onEvent(GenerateEvent.SelectScene(it)) }
            FormSection {
                Text(if (state.config.card.scene == QrScene.FREE) "你想分享什么？" else "填写内容", style = MaterialTheme.typography.titleMedium)
                if (state.config.card.scene == QrScene.FREE) {
                    ContentTypeChipGroup(state.config.contentType, { viewModel.onEvent(GenerateEvent.UpdateContentType(it)) })
                }
                when (state.config.card.scene) {
                    QrScene.MENU -> Text("填写已有菜单网页的链接", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    QrScene.WIFI -> Text("二维码包含连接信息，请只分享给需要联网的人。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> Unit
                }
                ContentForm(state.config.contentType, state.config.content) { viewModel.onEvent(GenerateEvent.UpdateContent(it)) }
                state.validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
            FormSection {
                Text("选择成品", style = MaterialTheme.typography.titleMedium)
                CardOutputPicker(state.config.card.enabled) { viewModel.onEvent(GenerateEvent.SetCardEnabled(it)) }
                if (state.config.card.enabled) {
                    CardTextEditor(
                        card = state.config.card,
                        onTitleChange = { viewModel.onEvent(GenerateEvent.UpdateCardTitle(it)) },
                        onSubtitleChange = { viewModel.onEvent(GenerateEvent.UpdateCardSubtitle(it)) }
                    )
                }
            }
            FormSection {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("成品预览", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onOpenStyle) {
                        Icon(Icons.Outlined.Palette, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("换个样式")
                    }
                }
                SceneCardPreview(state.qrDrawable, state.isGenerating, state.config.card)
                if (!state.isGenerating) ReadabilityStatus(state.readability) { viewModel.onEvent(GenerateEvent.RestoreReadableStyle) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { viewModel.onEvent(GenerateEvent.Share) }, enabled = canSave, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Outlined.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("分享")
                }
                OutlinedButton(onClick = { showExport = true }, enabled = canSave, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(8.dp)) { Text("保存为文件") }
            }
            Text("内容只在本机生成，不会上传。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(8.dp))
        }
    }
    if (showExport) ExportDialog(
        initialFormat = state.defaultExportFormat,
        onExport = { showExport = false; viewModel.onEvent(GenerateEvent.ExportFile(it)) },
        onDismiss = { showExport = false }
    )
}

@Composable
private fun FormSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
