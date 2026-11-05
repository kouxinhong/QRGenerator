package com.example.qrgenerator.ui.screens.generate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.example.qrgenerator.domain.QrPayload
import com.example.qrgenerator.domain.QrPayloadKind

/** 无法完整映射为表单的内容保持原样，避免静默丢字段或直接摊开网络密码。 */
@Composable
internal fun ImportedContentSummary(payload: QrPayload, importRevision: Long): Unit {
    var isShowingOriginal: Boolean by remember(payload.rawPayload, importRevision) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(payload.summary, style = MaterialTheme.typography.bodyLarge)
        Text("已保留全部原始内容，可直接制作。此二维码暂不支持逐项编辑。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { isShowingOriginal = !isShowingOriginal }) {
            Text(if (isShowingOriginal) "收起原文" else if (payload.kind == QrPayloadKind.WIFI) "查看原文（含连接信息）" else "查看原文")
        }
        if (isShowingOriginal) Text(payload.rawPayload, style = MaterialTheme.typography.bodySmall)
    }
}
