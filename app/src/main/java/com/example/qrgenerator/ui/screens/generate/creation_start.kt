package com.example.qrgenerator.ui.screens.generate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.qrgenerator.data.model.QrScene

/** 首次制作先选任务；恢复已有草稿时不会挡住编辑页。 */
@Composable
internal fun CreationStart(onSelectScene: (QrScene) -> Unit, onScan: () -> Unit, onPickImage: () -> Unit): Unit {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("这次想做什么？", style = MaterialTheme.typography.titleLarge)
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
            Column {
                CreationTask("连 Wi-Fi", "把访客网络做成一张桌牌", Icons.Outlined.Wifi) { onSelectScene(QrScene.WIFI) }
                Divider(Modifier.padding(start = 60.dp))
                CreationTask("分享页面", "菜单、报名表或店铺的已有链接", Icons.Outlined.Link) { onSelectScene(QrScene.MENU) }
                Divider(Modifier.padding(start = 60.dp))
                CreationTask("留联系方式", "让别人扫码保存姓名和电话", Icons.Outlined.Contacts) { onSelectScene(QrScene.CONTACT) }
            }
        }
        Text("已有二维码？直接导入制作", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        ImportSourceButtons(onScan, onPickImage)
        TextButton(onClick = { onSelectScene(QrScene.FREE) }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("自由制作 · 文字和更多类型") }
        Text("内容在本机处理，选好用途即可预览小卡。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CreationTask(title: String, description: String, icon: ImageVector, onClick: () -> Unit): Unit {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(20.dp))
    }
}

@Composable
internal fun ImportSourceButtons(onScan: () -> Unit, onPickImage: () -> Unit): Unit {
    val buttons: @Composable (Modifier) -> Unit = { modifier ->
        OutlinedButton(onClick = onScan, modifier = modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Outlined.QrCodeScanner, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("扫一扫导入")
        }
        OutlinedButton(onClick = onPickImage, modifier = modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Outlined.Image, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("从图片导入")
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 300.dp || LocalDensity.current.fontScale > 1.2f) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                buttons(Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                buttons(Modifier.weight(1f))
            }
        }
    }
}
