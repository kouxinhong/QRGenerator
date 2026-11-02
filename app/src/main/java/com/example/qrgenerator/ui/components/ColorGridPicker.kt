package com.example.qrgenerator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.qrgenerator.ui.theme.PresetColors

/** 每个色块有名称、选中标识和 48dp 点击范围。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorGridPicker(
    title: String,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var customHex by remember(selectedColor) { mutableStateOf(String.format("%06X", selectedColor and 0xFFFFFF)) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title.isNotEmpty()) Text(title, style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PresetColors.forEach { color ->
                val colorInt = color.toArgb()
                val isSelected = selectedColor == colorInt
                Box(
                    modifier = Modifier.size(48.dp)
                        .semantics { contentDescription = "颜色 ${String.format("#%06X", colorInt and 0xFFFFFF)}" }
                        .selectable(selected = isSelected, role = Role.RadioButton) { onColorSelected(colorInt) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(34.dp).background(color, CircleShape)
                            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) Icon(Icons.Default.Check, null, tint = if (color.luminance() > 0.45f) Color.Black else Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
        OutlinedTextField(
            value = customHex,
            onValueChange = { value ->
                customHex = value.removePrefix("#").take(6).uppercase()
                if (customHex.matches(Regex("[0-9A-F]{6}"))) onColorSelected((0xFF000000L or customHex.toLong(16)).toInt())
            },
            label = { Text("自定义颜色（6 位色号）") },
            prefix = { Text("#") },
            singleLine = true,
            isError = customHex.isNotEmpty() && !customHex.matches(Regex("[0-9A-F]{6}")),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
