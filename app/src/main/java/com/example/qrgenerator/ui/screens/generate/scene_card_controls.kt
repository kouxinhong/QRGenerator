@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.qrgenerator.ui.screens.generate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.qrgenerator.data.model.QrCardConfig
import com.example.qrgenerator.data.model.QrScene

/** 用途先于格式，日常场景只展示当前需要填写的内容。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScenePicker(selectedScene: QrScene, onSelectScene: (QrScene) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("选择用途", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(QrScene.WIFI, QrScene.MENU, QrScene.CONTACT, QrScene.FREE).forEach { scene ->
                FilterChip(
                    selected = selectedScene == scene,
                    onClick = { onSelectScene(scene) },
                    label = { Text(scene.title) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardOutputPicker(isCardEnabled: Boolean, onSetCardEnabled: (Boolean) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = isCardEnabled,
            onClick = { onSetCardEnabled(true) },
            label = { Text("带说明的小卡") },
            modifier = Modifier.heightIn(min = 48.dp)
        )
        FilterChip(
            selected = !isCardEnabled,
            onClick = { onSetCardEnabled(false) },
            label = { Text("只要二维码") },
            modifier = Modifier.heightIn(min = 48.dp)
        )
    }
}

@Composable
fun CardTextEditor(card: QrCardConfig, onTitleChange: (String) -> Unit, onSubtitleChange: (String) -> Unit) {
    var isExpanded: Boolean by rememberSaveable(card.scene) { mutableStateOf(false) }
    TextButton(
        onClick = { isExpanded = !isExpanded },
        modifier = Modifier.heightIn(min = 48.dp).semantics { stateDescription = if (isExpanded) "已展开" else "已收起" }
    ) {
        Text(if (isExpanded) "收起卡片文字" else "修改卡片文字")
        Icon(if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
    }
    if (isExpanded) CardTextFields(card, onTitleChange, onSubtitleChange)
}

/** 卡片文字独立于扫码内容；空白输入使用当前用途的默认文案。 */
@Composable
private fun CardTextFields(card: QrCardConfig, onTitleChange: (String) -> Unit, onSubtitleChange: (String) -> Unit) {
    OutlinedTextField(
        value = card.title,
        onValueChange = onTitleChange,
        label = { Text("卡片标题") },
        placeholder = { Text(card.scene.defaultTitle) },
        supportingText = { Text("最多 24 字；留空使用默认标题") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = card.subtitle,
        onValueChange = onSubtitleChange,
        label = { Text("扫码提示") },
        placeholder = { Text(card.scene.defaultSubtitle) },
        supportingText = { Text("最多 48 字；留空使用默认提示") },
        maxLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
}
