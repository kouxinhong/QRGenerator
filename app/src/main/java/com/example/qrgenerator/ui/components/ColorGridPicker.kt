package com.example.qrgenerator.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.qrgenerator.ui.theme.PresetColors

/** 颜色网格选择器 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorGridPicker(
    title: String,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PresetColors.forEach { color ->
                val colorInt = color.toArgb()
                val isSelected = selectedColor == colorInt
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) primary else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "colorBorderColor"
                )
                val borderWidth by animateDpAsState(
                    targetValue = if (isSelected) 3.dp else 1.5.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "colorBorderWidth"
                )
                val size by animateDpAsState(
                    targetValue = if (isSelected) 40.dp else 36.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "colorSize"
                )

                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(color)
                        .border(borderWidth, borderColor, CircleShape)
                        .then(
                            if (!isSelected) {
                                Modifier.border(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    CircleShape
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onColorSelected(colorInt) }
                )
            }
        }
    }
}
