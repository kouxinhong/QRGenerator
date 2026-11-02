@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.qrgenerator.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.qrgenerator.data.model.ExportFormat

@Composable
fun ExportDialog(onExport: (ExportFormat) -> Unit, onDismiss: () -> Unit, initialFormat: ExportFormat = ExportFormat.PNG) {
    var selectedFormat: ExportFormat by remember { mutableStateOf(if (initialFormat == ExportFormat.SVG) ExportFormat.PNG else initialFormat) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存为文件") },
        text = {
            Column {
                Text("接下来选择保存位置。")
                Spacer(Modifier.height(12.dp))
                listOf(ExportFormat.PNG to "PNG 图片 · 适合发送和使用", ExportFormat.PDF to "PDF 文档 · 适合打印").forEach { (format, title) ->
                    FilterChip(selected = format == selectedFormat, onClick = { selectedFormat = format }, label = { Text(title) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = { onExport(selectedFormat) }) { Text("选择保存位置") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
