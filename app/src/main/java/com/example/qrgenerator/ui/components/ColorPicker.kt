package com.example.qrgenerator.ui.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/**
 * 预设颜色列表
 */
private val presetColors = listOf(
    Color.Black,
    Color.White,
    Color(0xFFFF6900), // 小米橙
    Color(0xFFE53935), // 红色
    Color(0xFF43A047), // 绿色
    Color(0xFF1E88E5), // 蓝色
    Color(0xFF8E24AA), // 紫色
    Color(0xFFFDD835), // 黄色
    Color(0xFF00ACC1), // 青色
    Color(0xFFFF7043), // 深橙
    Color(0xFF5C6BC0), // 靛蓝
    Color(0xFF26A69A), // 蓝绿
)

/**
 * 颜色选择器组件
 * 显示预设颜色供用户选择
 *
 * @param title 标题文本
 * @param selectedColor 当前选中颜色
 * @param onColorSelected 颜色选择回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(
    title: String,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            presetColors.forEach { color ->
                val colorInt = color.toArgb()
                val isSelected = selectedColor == colorInt
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 3.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                            } else {
                                Modifier.border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                            }
                        )
                        .clickable { onColorSelected(colorInt) }
                )
            }
        }
    }
}
