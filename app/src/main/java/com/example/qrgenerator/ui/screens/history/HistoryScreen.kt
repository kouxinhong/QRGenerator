package com.example.qrgenerator.ui.screens.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.data.local.QrHistoryEntity
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = koinViewModel(), onOpenGenerator: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    BackHandler(enabled = state.isDeleteMode) {
        if (!state.isDeleting) viewModel.onEvent(HistoryEvent.ToggleDeleteMode)
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.onEvent(HistoryEvent.ClearMessage)
        }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TopAppBar(
                title = { Text("已保存", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    if ((state.records.isNotEmpty() || state.isDeleteMode) && state.loadError == null && !state.isLoading) {
                        TextButton(onClick = { viewModel.onEvent(HistoryEvent.ToggleDeleteMode) }, enabled = !state.isDeleting) {
                            Text(if (state.isDeleteMode) "完成" else "管理")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
            OutlinedTextField(
                value = state.searchQuery, onValueChange = { viewModel.onEvent(HistoryEvent.Search(it)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("搜索保存的内容") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true, enabled = !state.isDeleting
            )
            if (state.isDeleteMode) {
                val allSelected = state.records.isNotEmpty() && state.records.all { it.id in state.selectedIds }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("已选 ${state.selectedIds.size} 条", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { viewModel.onEvent(HistoryEvent.ToggleSelectAll) }, enabled = state.records.isNotEmpty() && !state.isDeleting) {
                        Text(if (allSelected) "取消全选" else "全选当前结果")
                    }
                }
            }
            when {
                state.isLoading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                }
                state.loadError != null -> Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(state.loadError.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { viewModel.onEvent(HistoryEvent.RetryLoading) }) { Text("重新加载") }
                }
                state.records.isEmpty() -> Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Outlined.History, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text(if (state.searchQuery.isBlank()) "保存后，在这里找到二维码" else "没有找到相关内容", style = MaterialTheme.typography.titleMedium)
                    Text(if (state.searchQuery.isBlank()) "点开一条记录，就能继续修改和分享" else "试试其他关键词", Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.searchQuery.isBlank()) TextButton(onClick = onOpenGenerator) { Text("去做一个二维码") }
                }
                else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 12.dp)) {
                    items(state.records, key = { it.id }) { record ->
                        HistoryRow(record, state.isDeleteMode, record.id in state.selectedIds, enabled = !state.isDeleting) {
                            if (state.isDeleteMode) viewModel.onEvent(HistoryEvent.ToggleSelection(record.id))
                            else if (viewModel.restoreRecord(record)) onOpenGenerator()
                        }
                        Divider(Modifier.padding(start = 104.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
            if (state.isDeleteMode) {
                Button(onClick = { showDeleteConfirm = true }, enabled = state.selectedIds.isNotEmpty() && !state.isDeleting,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text(if (state.isDeleting) "正在删除…" else "删除选中的 ${state.selectedIds.size} 条")
                }
            }
        }
    }
    if (showDeleteConfirm) AlertDialog(
        onDismissRequest = { showDeleteConfirm = false }, title = { Text("删除选中的 ${state.selectedIds.size} 条记录？") },
        text = { Text("仅删除应用里的记录，已保存到相册的图片会保留。") },
        confirmButton = { TextButton(onClick = {
            viewModel.onEvent(HistoryEvent.DeleteSelected); showDeleteConfirm = false
        }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
    )
    state.errorMessage?.let { message ->
        AlertDialog(onDismissRequest = { viewModel.onEvent(HistoryEvent.ClearError) },
            text = { Text(message) }, confirmButton = {
                TextButton(onClick = { viewModel.onEvent(HistoryEvent.ClearError) }) { Text("知道了") }
            })
    }
}

@Composable
private fun HistoryRow(record: QrHistoryEntity, isManaging: Boolean, isSelected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val typeName = QrContentType.entries.firstOrNull { it.name == record.contentType }?.displayName ?: "二维码"
    val date = remember(record.createdAt) { SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA).format(Date(record.createdAt)) }
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).clickable(enabled = enabled, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        AsyncImage(model = File(record.thumbnailPath), contentDescription = "二维码缩略图",
            modifier = Modifier.size(72.dp).background(Color.White).padding(4.dp), contentScale = ContentScale.Fit)
        Column(Modifier.weight(1f)) {
            Text(record.contentPreview.ifBlank { typeName }, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(6.dp))
            Text("$typeName  $date", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        if (isManaging) Checkbox(checked = isSelected, onCheckedChange = null, enabled = enabled)
        else Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
