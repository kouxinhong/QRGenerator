@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.qrgenerator.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.qrgenerator.data.model.QrContentType

/** 先露出最常用的六项，其他类型按需展开。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContentTypeChipGroup(selectedType: QrContentType, onTypeSelected: (QrContentType) -> Unit, modifier: Modifier = Modifier) {
    val common: List<QrContentType> = listOf(QrContentType.TEXT, QrContentType.URL, QrContentType.WIFI, QrContentType.VCARD, QrContentType.PHONE, QrContentType.EMAIL)
    var isExpanded: Boolean by rememberSaveable { mutableStateOf(false) }
    Column(modifier) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (if (isExpanded) common + QrContentType.entries.filter { it !in common } else common).forEach { type ->
                FilterChip(selected = selectedType == type, onClick = { onTypeSelected(type) }, label = { Text(type.displayName) }, modifier = Modifier.heightIn(min = 48.dp))
            }
            if (!isExpanded && selectedType !in common) FilterChip(selected = true, onClick = { isExpanded = true }, label = { Text(selectedType.displayName) }, modifier = Modifier.heightIn(min = 48.dp))
        }
        TextButton(onClick = { isExpanded = !isExpanded }) { Text(if (isExpanded) "收起" else "更多内容类型") }
    }
}
