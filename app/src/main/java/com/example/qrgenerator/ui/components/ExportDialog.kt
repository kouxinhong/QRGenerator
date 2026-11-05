@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.qrgenerator.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.qrgenerator.data.model.ExportFormat
import com.example.qrgenerator.data.model.QrPrintLayout

@Composable
fun ExportDialog(
    onExport: (ExportFormat, QrPrintLayout) -> Unit,
    onDismiss: () -> Unit,
    initialFormat: ExportFormat = ExportFormat.PNG,
    artwork: Bitmap? = null
): Unit {
    var selectedFormat: ExportFormat by rememberSaveable { mutableStateOf(if (initialFormat == ExportFormat.SVG) ExportFormat.PNG else initialFormat) }
    var printLayout: QrPrintLayout by rememberSaveable { mutableStateOf(QrPrintLayout.SINGLE_CARD) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存为文件") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                Text("接下来选择保存位置。")
                Spacer(Modifier.height(12.dp))
                listOf(ExportFormat.PNG to "PNG 图片 · 适合发送和使用", ExportFormat.PDF to "PDF 文档 · 适合打印").forEach { (format, title) ->
                    FilterChip(selected = format == selectedFormat, onClick = { selectedFormat = format }, label = { Text(title) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp))
                }
                if (selectedFormat == ExportFormat.PDF) {
                    Spacer(Modifier.height(12.dp))
                    Text("打印排版", style = MaterialTheme.typography.titleSmall)
                    QrPrintLayout.entries.forEach { layout ->
                        FilterChip(selected = printLayout == layout, onClick = { printLayout = layout },
                            label = { Column(Modifier.padding(vertical = 4.dp)) {
                                Text(layout.title)
                                Text("每张 ${layout.dimensionsLabel}", style = MaterialTheme.typography.bodySmall)
                            } }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    artwork?.let { QrPrintPreview(it, printLayout, Modifier.align(Alignment.CenterHorizontally)) }
                    Spacer(Modifier.height(12.dp))
                    Text("使用 A4 纸，打印请选择“实际大小”或 100%，不要缩放以适应纸张。按裁切线裁开即可。", style = MaterialTheme.typography.bodySmall)
                    Text("成品等比居中，可能留有白边；打印前请先试扫。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onExport(selectedFormat, printLayout) }) { Text("选择保存位置") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
