package com.example.qrgenerator.ui.screens.style

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qrgenerator.data.model.*
import com.example.qrgenerator.domain.QrReadability
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/** 先选成套样式，需要时再展开具体设置。 */
@Composable
fun StyleScreen(viewModel: StyleViewModel = koinViewModel(), onDone: () -> Unit = {}) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    var expandedSection by rememberSaveable { mutableStateOf<String?>(null) }
    var showReset by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val update: ((QrConfig) -> QrConfig) -> Unit = { viewModel.onEvent(StyleEvent.UpdateConfig(it)) }
    val onError: (String) -> Unit = { scope.launch { snackbar.showSnackbar(it) } }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("选样式", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onDone, modifier = Modifier.heightIn(min = 48.dp)) { Text("完成") }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Text("点一下就能换，下面还可以自己调整", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
            StylePreview(preview) { viewModel.onEvent(StyleEvent.ResetStyle) }
            PresetPicker(config, onSelected = { viewModel.onEvent(StyleEvent.ApplyPreset(it)) })
            Spacer(Modifier.height(16.dp))
            StyleSection("shape", "形状", config.darkPixelShape.displayName, Icons.Outlined.Category, expandedSection, { expandedSection = it }) {
                ShapeOptions(config, update)
            }
            StyleSection("color", "颜色", config.darkColor.type.displayName, Icons.Outlined.Palette, expandedSection, { expandedSection = it }) {
                ColorOptions(config, update)
            }
            StyleSection("logo", "中间小图", if (config.logo.imagePath != null || config.logo.builtinIconName != null) "已添加" else "添加头像或图标", Icons.Outlined.Image, expandedSection, { expandedSection = it }) {
                LogoOptions(config, update, onError)
            }
            StyleSection("background", "背景", if (config.background.imagePath != null) "已添加图片" else "颜色和图片", Icons.Outlined.Wallpaper, expandedSection, { expandedSection = it }) {
                BackgroundOptions(config, update, onError)
            }
            Spacer(Modifier.height(12.dp))
            StyleSection("advanced", "更多设置", "大小、留白、识别保护", Icons.Outlined.Tune, expandedSection, { expandedSection = it }) {
                AdvancedOptions(config, update)
            }
            TextButton(onClick = { showReset = true }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp).heightIn(min = 48.dp)) {
                Text("恢复默认样式")
            }
        }
    }
    if (showReset) AlertDialog(
        onDismissRequest = { showReset = false },
        title = { Text("恢复默认样式？") },
        text = { Text("颜色、形状和图片会恢复默认，填写的内容会保留。") },
        confirmButton = { TextButton(onClick = { viewModel.onEvent(StyleEvent.ResetStyle); showReset = false }) { Text("恢复默认") } },
        dismissButton = { TextButton(onClick = { showReset = false }) { Text("取消") } }
    )
}

@Composable
private fun StylePreview(preview: StylePreviewState, onRestoreStyle: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.widthIn(max = 220.dp).fillMaxWidth().height(220.dp).background(Color.White, RoundedCornerShape(8.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
            when {
                preview.isLoading -> CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                preview.bitmap != null -> Image(preview.bitmap.asImageBitmap(), "当前二维码样式预览", modifier = Modifier.fillMaxSize())
                preview.error != null -> Text(preview.error, color = Color(0xFF666666), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        when {
            preview.isLoading -> Text("正在预览并检查…", style = MaterialTheme.typography.bodySmall)
            preview.readability == QrReadability.VERIFIED -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(if (preview.isExample) "示例样式预览已识别" else "样式预览已识别", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Text(if (preview.isExample) "填写内容后，制作页会检查最终成品。" else "仅检查此处预览，制作页会再次检查最终成品。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            preview.readability == QrReadability.UNREADABLE || preview.error != null -> {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (preview.error != null) "暂时无法检查样式预览" else "样式预览暂未识别", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("可减少小图遮挡，使用深色码点和浅色背景，或恢复默认样式。", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onRestoreStyle) { Text("恢复清晰样式") }
                    }
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun StyleSection(
    key: String,
    title: String,
    summary: String,
    icon: ImageVector,
    expandedSection: String?,
    onExpand: (String?) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val expanded = expandedSection == key
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            TextButton(
                onClick = { onExpand(if (expanded) null else key) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                shape = RoundedCornerShape(0.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Icon(icon, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.widthIn(max = 136.dp))
                Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ChevronRight, if (expanded) "收起$title" else "展开$title", modifier = Modifier.size(22.dp))
            }
            if (expanded) Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
            Divider(modifier = Modifier.padding(start = 54.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        }
    }
}
