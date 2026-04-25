package com.example.qrgenerator.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import com.example.qrgenerator.data.model.ExportFormat
import com.example.qrgenerator.data.model.QrErrorLevel
import com.example.qrgenerator.data.model.ThemeMode

/** 设置页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showFormatDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(title = { Text("设置") })

        // 主题模式
        SettingsItem(
            title = "主题模式",
            subtitle = settings.themeMode.displayName,
            onClick = { showThemeDialog = true }
        )

        // 默认容错级别
        SettingsItem(
            title = "默认容错级别",
            subtitle = settings.defaultErrorLevel.displayName,
            onClick = { showErrorDialog = true }
        )

        // 默认导出格式
        SettingsItem(
            title = "默认导出格式",
            subtitle = settings.defaultExportFormat.displayName,
            onClick = { showFormatDialog = true }
        )

        // 默认图片尺寸
        SettingsItem(
            title = "默认图片尺寸",
            subtitle = "${settings.defaultImageSize} x ${settings.defaultImageSize}",
            onClick = { }
        )

        // 关于
        SettingsItem(
            title = "关于",
            subtitle = "版本 1.0.0",
            onClick = { }
        )
    }

    // 主题选择弹窗
    if (showThemeDialog) {
        SingleChoiceDialog(
            title = "主题模式",
            options = ThemeMode.entries.map { it.displayName },
            selectedIndex = ThemeMode.entries.indexOf(settings.themeMode),
            onSelect = {
                viewModel.executeUpdateThemeMode(ThemeMode.entries[it])
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    // 容错级别选择弹窗
    if (showErrorDialog) {
        SingleChoiceDialog(
            title = "默认容错级别",
            options = QrErrorLevel.entries.map { it.displayName },
            selectedIndex = QrErrorLevel.entries.indexOf(settings.defaultErrorLevel),
            onSelect = {
                viewModel.executeUpdateErrorLevel(QrErrorLevel.entries[it])
                showErrorDialog = false
            },
            onDismiss = { showErrorDialog = false }
        )
    }

    // 导出格式选择弹窗
    if (showFormatDialog) {
        SingleChoiceDialog(
            title = "默认导出格式",
            options = ExportFormat.entries.map { it.displayName },
            selectedIndex = ExportFormat.entries.indexOf(settings.defaultExportFormat),
            onSelect = {
                viewModel.executeUpdateExportFormat(ExportFormat.entries[it])
                showFormatDialog = false
            },
            onDismiss = { showFormatDialog = false }
        )
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(title) },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    FilterChip(
                        selected = selectedIndex == index,
                        onClick = { onSelect(index) },
                        label = { Text(option) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {}
    )
}
