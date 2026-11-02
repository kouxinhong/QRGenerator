package com.example.qrgenerator.ui.screens.style

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.qrgenerator.data.model.*
import com.example.qrgenerator.domain.QrGeneratorUseCase
import com.example.qrgenerator.ui.components.ColorGridPicker
import com.example.qrgenerator.ui.components.ShapeChipGroup
import com.github.alexzhirkevich.customqrgenerator.style.Neighbors
import kotlin.math.roundToInt

@Composable
internal fun PresetPicker(config: QrConfig, onSelected: (StylePreset) -> Unit) {
    Text("一键换样式", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StylePreset.entries.forEach { preset ->
            val selected = preset.isAppliedTo(config)
            Surface(
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(100.dp).selectable(selected = selected, role = Role.RadioButton, onClick = { onSelected(preset) })
                    .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelSample(preset.pixel, Color(preset.color), Modifier.size(40.dp))
                    Text(preset.title, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PixelPicker(title: String, selected: QrPixelShapeType, onSelected: (QrPixelShapeType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            QrPixelShapeType.entries.forEach { shape ->
                val isSelected = selected == shape
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.width(88.dp).selectable(isSelected, role = Role.RadioButton, onClick = { onSelected(shape) })
                        .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PixelSample(shape, MaterialTheme.colorScheme.onSurface, Modifier.size(30.dp))
                        Text(shape.displayName.removePrefix("默认"), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PixelSample(shape: QrPixelShapeType, color: Color, modifier: Modifier) {
    val pixel = remember(shape) { QrGeneratorUseCase().mapPixelShape(shape) }
    val paint = remember(color) { Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() } }
    Canvas(modifier) {
        val cell = size.minDimension / 2.3f
        drawIntoCanvas { canvas ->
            listOf(0f to 0f, 1.3f to 0f, 0f to 1.3f, 1.3f to 1.3f).forEach { (x, y) ->
                canvas.nativeCanvas.save()
                canvas.nativeCanvas.translate(x * cell, y * cell)
                canvas.nativeCanvas.drawPath(pixel.createPath(cell, Neighbors.Empty), paint)
                canvas.nativeCanvas.restore()
            }
        }
    }
}

@Composable
internal fun ColorEditor(title: String, config: QrColorConfig, allowFollowDark: Boolean = false, onChange: (QrColorConfig) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (title.isNotEmpty()) Text(title, style = MaterialTheme.typography.titleMedium)
        ShapeChipGroup("颜色效果", QrColorType.entries.filter { allowFollowDark || it != QrColorType.FOLLOW_DARK }, config.type, { it.displayName }, { onChange(config.copy(type = it)) })
        when (config.type) {
            QrColorType.SOLID -> ColorGridPicker("", config.solidColor, { onChange(config.copy(solidColor = it)) })
            QrColorType.TRANSPARENT, QrColorType.ERASER, QrColorType.FOLLOW_DARK -> Unit
            else -> {
                ColorGridPicker("开始的颜色", config.gradientStartColor, { onChange(config.copy(gradientStartColor = it)) })
                ColorGridPicker("结束的颜色", config.gradientEndColor, { onChange(config.copy(gradientEndColor = it)) })
                if (config.type == QrColorType.LINEAR_GRADIENT) ShapeChipGroup("渐变方向", GradientOrientation.entries, config.gradientOrientation, { it.displayName }, { onChange(config.copy(gradientOrientation = it)) })
                if (config.type == QrColorType.RADIAL_GRADIENT) ValueSlider("渐变范围", config.gradientRadius, 0.25f..2f, onChange = { onChange(config.copy(gradientRadius = it)) })
            }
        }
    }
}

@Composable
internal fun ValueSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, suffix: String = "%", factor: Float = 100f, onChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text("${(value * factor).roundToInt()}$suffix", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value.coerceIn(range), onValueChange = onChange, valueRange = range, modifier = Modifier.fillMaxWidth().semantics { contentDescription = title })
    }
}

@Composable
internal fun ToggleRow(title: String, checked: Boolean, description: String, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description.isNotEmpty()) Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onCheckedChange)
    }
}

@Composable
internal fun HelpText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
