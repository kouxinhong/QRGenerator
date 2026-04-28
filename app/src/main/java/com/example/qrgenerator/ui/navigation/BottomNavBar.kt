package com.example.qrgenerator.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** 底部导航项 */
enum class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    GENERATE("generate", "生成", Icons.Default.AutoAwesome),
    STYLE("style", "样式", Icons.Default.Palette),
    HISTORY("history", "历史", Icons.Default.History),
    SETTINGS("settings", "设置", Icons.Default.Settings);
}

/** 底部导航栏 */
@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        BottomNavItem.entries.forEach { item ->
            val isSelected = currentRoute == item.route
            val iconColor by animateColorAsState(
                targetValue = if (isSelected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "navIconColor"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "navTextColor"
            )

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = iconColor,
                        modifier = Modifier.drawBehind {
                            if (isSelected) {
                                drawRoundRect(
                                    color = primary.copy(alpha = 0.12f),
                                    topLeft = Offset(-4.dp.toPx(), -2.dp.toPx()),
                                    size = Size(size.width + 8.dp.toPx(), size.height + 4.dp.toPx()),
                                    cornerRadius = CornerRadius(8.dp.toPx())
                                )
                            }
                        }
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = textColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}
