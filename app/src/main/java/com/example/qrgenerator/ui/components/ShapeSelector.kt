package com.example.qrgenerator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.qrgenerator.data.QrBallShapeType
import com.example.qrgenerator.data.QrFrameShapeType
import com.example.qrgenerator.data.QrPixelShapeType

/**
 * 模块形状选择器
 *
 * @param selectedShape 当前选中的形状
 * @param onShapeSelected 形状选择回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PixelShapeSelector(
    selectedShape: QrPixelShapeType,
    onShapeSelected: (QrPixelShapeType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "模块形状",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QrPixelShapeType.entries.forEach { shape ->
                FilterChip(
                    selected = selectedShape == shape,
                    onClick = { onShapeSelected(shape) },
                    label = { Text(shape.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

/**
 * 眼睛外框形状选择器
 *
 * @param selectedShape 当前选中的形状
 * @param onShapeSelected 形状选择回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FrameShapeSelector(
    selectedShape: QrFrameShapeType,
    onShapeSelected: (QrFrameShapeType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "眼睛外框",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QrFrameShapeType.entries.forEach { shape ->
                FilterChip(
                    selected = selectedShape == shape,
                    onClick = { onShapeSelected(shape) },
                    label = { Text(shape.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

/**
 * 眼睛内球形状选择器
 *
 * @param selectedShape 当前选中的形状
 * @param onShapeSelected 形状选择回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BallShapeSelector(
    selectedShape: QrBallShapeType,
    onShapeSelected: (QrBallShapeType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "眼睛内球",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QrBallShapeType.entries.forEach { shape ->
                FilterChip(
                    selected = selectedShape == shape,
                    onClick = { onShapeSelected(shape) },
                    label = { Text(shape.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}
