package com.example.qrgenerator.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.qrgenerator.data.model.QrScene
import com.example.qrgenerator.domain.QrPayload
import com.example.qrgenerator.domain.QrPayloadField
import com.example.qrgenerator.domain.QrPayloadKind
import com.example.qrgenerator.domain.QrPayloadParser
import kotlinx.coroutines.launch
import java.net.URI

@Composable
internal fun ScanResults(
    contents: List<String>,
    targetScene: QrScene?,
    onUseContent: (String) -> Unit,
    onRetry: () -> Unit,
    snackbar: SnackbarHostState,
    modifier: Modifier,
    isCameraSelected: Boolean
): Unit {
    val payloads: List<QrPayload> = remember(contents) { contents.map(QrPayloadParser::parse) }
    var selectedIndex: Int by rememberSaveable(contents) { mutableStateOf(if (contents.size == 1) 0 else -1) }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (contents.size > 1) "识别到 ${contents.size} 个二维码，选择要用的内容" else "识别成功", style = MaterialTheme.typography.titleMedium)
        payloads.forEachIndexed { index, payload ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (contents.size > 1) "${index + 1} · ${payload.title}" else payload.title, style = MaterialTheme.typography.titleMedium)
                    Text(if (payload.kind == QrPayloadKind.URL) runCatching { URI(payload.rawPayload.trim()).host }.getOrNull() ?: payload.summary else payload.summary,
                        style = MaterialTheme.typography.bodyLarge)
                    if (selectedIndex == index) {
                        ScanPayloadDetails(payload)
                        ScanPayloadActions(payload, targetScene, onUseContent, snackbar)
                    } else {
                        TextButton(onClick = { selectedIndex = index }) { Text("选择这个二维码") }
                    }
                }
            }
        }
        TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(if (isCameraSelected) "继续扫描" else "识别另一张图片") }
    }
}

@Composable
private fun ScanPayloadDetails(payload: QrPayload): Unit {
    var isRawVisible: Boolean by remember(payload.rawPayload) { mutableStateOf(false) }
    if (payload.kind != QrPayloadKind.TEXT) {
        payload.fields.forEach { field -> ScanPayloadField(field, payload.rawPayload) }
    }
    if (payload.editableContent == null && payload.kind != QrPayloadKind.TEXT) {
        Text("这个码包含特殊信息，将保留全部原内容制作。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (payload.kind == QrPayloadKind.URL) {
        Text("只保留链接内容，不改变原网站的有效期。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    TextButton(onClick = { isRawVisible = !isRawVisible }) {
        Text(if (isRawVisible) "收起原文" else if (payload.kind == QrPayloadKind.WIFI) "查看原文（含连接信息）" else "查看完整原文")
    }
    if (isRawVisible) SelectionContainer { Text(payload.rawPayload, style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun ScanPayloadField(field: QrPayloadField, payloadKey: String): Unit {
    var isVisible: Boolean by remember(payloadKey, field.label) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(field.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (field.isSensitive && !isVisible) Text("••••••••", style = MaterialTheme.typography.bodyMedium)
        else SelectionContainer { Text(field.value, style = MaterialTheme.typography.bodyMedium) }
        if (field.isSensitive) TextButton(onClick = { isVisible = !isVisible }) { Text(if (isVisible) "隐藏${field.label}" else "显示${field.label}") }
    }
}

@Composable
private fun ScanPayloadActions(payload: QrPayload, targetScene: QrScene?, onUseContent: (String) -> Unit, snackbar: SnackbarHostState): Unit {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    if (payload.kind == QrPayloadKind.WIFI) {
        Text("复制原文会包含网络连接信息。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Button(onClick = { onUseContent(payload.rawPayload) }, modifier = Modifier.fillMaxWidth()) { Text(getScanActionLabel(payload, targetScene)) }
    OutlinedButton(onClick = {
        clipboard.setText(AnnotatedString(payload.rawPayload))
        scope.launch { snackbar.showSnackbar("已复制原文") }
    }, modifier = Modifier.fillMaxWidth()) { Text("复制原文") }
}

internal fun getScanActionLabel(payload: QrPayload, targetScene: QrScene?): String {
    if (payload.editableType == null && payload.kind != QrPayloadKind.TEXT) return "保留原内容制作"
    return when (payload.kind) {
        QrPayloadKind.WIFI -> if (targetScene == QrScene.WIFI) "填入当前 Wi-Fi 卡" else "制作 Wi-Fi 小卡"
        QrPayloadKind.URL -> if (targetScene == QrScene.MENU) "填入当前链接卡" else "制作链接小卡"
        QrPayloadKind.CONTACT -> if (targetScene == QrScene.CONTACT) "填入当前联系卡" else "制作联系卡"
        QrPayloadKind.TEXT -> if (targetScene == QrScene.FREE) "填入当前卡片" else "使用原内容制作"
    }
}
