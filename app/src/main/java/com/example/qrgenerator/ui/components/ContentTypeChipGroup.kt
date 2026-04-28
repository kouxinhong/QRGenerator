package com.example.qrgenerator.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.qrgenerator.data.model.QrContentType

/** 内容类型 chip 选择组（横向滚动） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentTypeChipGroup(
    selectedType: QrContentType,
    onTypeSelected: (QrContentType) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QrContentType.entries.forEach { type ->
            val isSelected = selectedType == type
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
                onClick = { onTypeSelected(type) },
                label = { Text(type.displayName) },
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
