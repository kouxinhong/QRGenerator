package com.example.qrgenerator.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

    var isLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isLoaded = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {
                Text(
                    "设置",
                    style = MaterialTheme.typography.headlineMedium
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 主题模式
        AnimatedSettingsItem(
            title = "主题模式",
            subtitle = settings.themeMode.displayName,
            onClick = { showThemeDialog = true },
            isVisible = isLoaded,
            delayMillis = 0
        )

        // 默认容错级别
        AnimatedSettingsItem(
            title = "默认容错级别",
            subtitle = settings.defaultErrorLevel.displayName,
            onClick = { showErrorDialog = true },
            isVisible = isLoaded,
            delayMillis = 60
        )

        // 默认导出格式
        AnimatedSettingsItem(
            title = "默认导出格式",
            subtitle = settings.defaultExportFormat.displayName,
            onClick = { showFormatDialog = true },
            isVisible = isLoaded,
            delayMillis = 120
        )

        // 默认图片尺寸
        AnimatedSettingsItem(
            title = "默认图片尺寸",
            subtitle = "${settings.defaultImageSize} x ${settings.defaultImageSize}",
            onClick = { },
            isVisible = isLoaded,
            delayMillis = 180
        )

        // 关于
        AnimatedSettingsItem(
            title = "关于",
            subtitle = "版本 1.0.0",
            onClick = { },
            isVisible = isLoaded,
            delayMillis = 240
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

/** 带入场动画的设置项 */
@Composable
private fun AnimatedSettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isVisible: Boolean,
    delayMillis: Int
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(350, delayMillis = delayMillis)) + slideInVertically(
            tween(350, delayMillis = delayMillis),
            initialOffsetY = { it / 10 }
        )
    ) {
        SettingsItem(
            title = title,
            subtitle = subtitle,
            onClick = onClick
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
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    FilterChip(
                        selected = selectedIndex == index,
                        onClick = { onSelect(index) },
                        label = {
                            Text(
                                option,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = null
                    )
                }
            }
        },
        confirmButton = {}
    )
}
