package com.example.qrgenerator.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.qrgenerator.data.model.AppSettings
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qrgenerator.data.model.ExportFormat
import com.example.qrgenerator.data.model.QrErrorLevel
import com.example.qrgenerator.data.model.ThemeMode
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val versionName = remember(context) {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知"
    }
    val canEdit = status.hasLoaded && !status.isSaving
    LaunchedEffect(status.message) {
        status.message?.let { snackbar.showSnackbar(it); viewModel.onEvent(SettingsEvent.ClearMessage) }
    }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    val imageSizes = AppSettings.IMAGE_SIZES
    val formats = listOf(ExportFormat.PNG, ExportFormat.PDF)
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        },
        snackbarHost = { SnackbarHost(snackbar) }, containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            if (!status.hasLoaded || status.isSaving) LinearProgressIndicator(Modifier.fillMaxWidth())
            SettingsGroupLabel("显示")
            SettingsRow("深色模式", settings.themeMode.displayName, canEdit) { dialog = "theme" }
            SettingsGroupLabel("制作和保存")
            SettingsRow("图片清晰度", when (settings.defaultImageSize) {
                512 -> "标准 · 512 像素"; 2048 -> "超清 · 2048 像素"; else -> "高清 · 1024 像素"
            }, canEdit) { dialog = "size" }
            SettingsRow("文件保存格式", settings.defaultExportFormat.displayName, canEdit) { dialog = "format" }
            SettingsRow("遮挡后识别能力", settings.defaultErrorLevel.displayName, canEdit) { dialog = "error" }
            Text("这里选择的清晰度和识别能力会用于当前二维码，并作为下次制作的默认值。保存到相册始终是 PNG 图片。",
                Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            SettingsGroupLabel("使用帮助")
            SettingsRow("怎么做二维码", "三个步骤") { dialog = "help" }
            SettingsRow("数据与权限", "了解内容保存在什么地方") { dialog = "privacy" }
            SettingsRow("关于二维码生成器", "版本 $versionName") { dialog = "about" }
            SettingsGroupLabel("恢复设置")
            SettingsRow("恢复默认设置", "保留填写内容和已保存的记录", canEdit) { dialog = "reset" }
            Spacer(Modifier.height(24.dp))
        }
    }
    when (dialog) {
        "theme" -> ChoiceDialog("深色模式", ThemeMode.entries.map { it.displayName },
            ThemeMode.entries.indexOf(settings.themeMode), {
                viewModel.onEvent(SettingsEvent.UpdateThemeMode(ThemeMode.entries[it])); dialog = null
            }, { dialog = null })
        "size" -> ChoiceDialog("图片清晰度", listOf("标准（512 像素）", "高清（1024 像素）", "超清（2048 像素）"),
            imageSizes.indexOf(settings.defaultImageSize), {
                viewModel.onEvent(SettingsEvent.UpdateImageSize(imageSizes[it])); dialog = null
            }, { dialog = null })
        "format" -> ChoiceDialog("文件保存格式", formats.map { it.displayName },
            formats.indexOf(settings.defaultExportFormat), {
                viewModel.onEvent(SettingsEvent.UpdateExportFormat(formats[it])); dialog = null
            }, { dialog = null })
        "error" -> ChoiceDialog("遮挡后识别能力", QrErrorLevel.entries.map { it.displayName },
            QrErrorLevel.entries.indexOf(settings.defaultErrorLevel), {
                viewModel.onEvent(SettingsEvent.UpdateErrorLevel(QrErrorLevel.entries[it])); dialog = null
            }, { dialog = null })
        "help" -> InfoDialog("三个步骤就能做好", "1. 选文字、网址或 Wi-Fi，按提示填写。\n\n2. 点“换样式”，挑喜欢的形状、颜色和小图。\n\n3. 返回预览，点“保存到相册”或“分享”。\n\n做好后，用另一台手机扫一扫。深色图案、浅色背景更容易扫出来。", { dialog = null })
        "about" -> InfoDialog("关于二维码生成器", "版本 $versionName\n\n内容和图片在本机处理，无需注册。\n\n保存或分享后的作品会留在“已保存”，点开即可继续编辑。删除应用会同时移除应用内的记录。\n\n基于开源 custom-qr-generator（MIT）与 ZXing 制作。", { dialog = null })
        "privacy" -> InfoDialog("数据与权限", "二维码在本机生成，不需要联网。填写的内容、导入的小图和背景、已保存记录会存放在应用内。\n\n从相册选图时，只读取你选中的图片；拍照会打开系统相机。Android 8 和 9 保存到相册时需要存储权限，也可以选择“保存为文件”。\n\n主动分享时，接收的应用可以读取该二维码图片。删除应用内记录不会删除已导出到相册或文件夹的图片。", { dialog = null })
        "reset" -> AlertDialog(
            onDismissRequest = { dialog = null }, title = { Text("恢复默认设置？") },
            text = { Text("将恢复跟随系统的外观、高清 1024 像素、PNG 文件和自动识别保护。当前内容、图片样式和已保存记录会保留。") },
            confirmButton = { TextButton(onClick = { viewModel.onEvent(SettingsEvent.RestoreDefaults); dialog = null }) { Text("恢复默认") } },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("取消") } }
        )
    }
    status.errorMessage?.let { message ->
        AlertDialog(onDismissRequest = { viewModel.onEvent(SettingsEvent.ClearFeedback) },
            title = { Text("设置暂时不可用") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = { viewModel.onEvent(SettingsEvent.Retry) }) { Text("重试") } },
            dismissButton = { TextButton(onClick = { viewModel.onEvent(SettingsEvent.ClearFeedback) }) { Text("稍后再试") } })
    }
}

@Composable
private fun SettingsGroupLabel(text: String) {
    Text(text, Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SettingsRow(title: String, value: String, enabled: Boolean = true, onClick: () -> Unit) {
    Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).heightIn(min = 64.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Divider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
    }
}

@Composable
private fun ChoiceDialog(title: String, options: List<String>, selectedIndex: Int,
    onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            options.forEachIndexed { index, label ->
                Row(Modifier.fillMaxWidth().selectable(selectedIndex == index,
                    onClick = { onSelect(index) }, role = Role.RadioButton).heightIn(min = 52.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedIndex == index, onClick = null)
                    Text(label, Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun InfoDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = { Text(message, Modifier.verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } })
}
