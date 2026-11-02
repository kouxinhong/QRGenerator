package com.example.qrgenerator.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    GENERATE("generate", "做二维码", Icons.Outlined.QrCode),
    STYLE("style", "换样式", Icons.Outlined.Palette),
    HISTORY("history", "已保存", Icons.Outlined.History),
    SETTINGS("settings", "设置", Icons.Outlined.Settings)
}

@Composable
fun BottomNavBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    Column {
        Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            BottomNavItem.entries.filter { it != BottomNavItem.STYLE }.forEach { item ->
                NavigationBarItem(
                    selected = currentRoute == item.route || (item == BottomNavItem.GENERATE && currentRoute == BottomNavItem.STYLE.route),
                    onClick = { onNavigate(item.route) },
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.surface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
