package com.example.qrgenerator.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 通用形状选择 chip 组 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun <T> ShapeChipGroup(
    title: String,
    options: List<T>,
    selected: T,
    displayName: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = selected == option
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) primary else surfaceVariant,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "chipContainerColor"
                )
                val labelColor by animateColorAsState(
                    targetValue = if (isSelected) onPrimary else onSurfaceVariant,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "chipLabelColor"
                )

                FilterChip(
                    selected = isSelected,
                    onClick = { onSelected(option) },
                    label = { Text(displayName(option)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = containerColor,
                        selectedLabelColor = labelColor,
                        containerColor = containerColor,
                        labelColor = labelColor
                    )
                )
            }
        }
    }
}
