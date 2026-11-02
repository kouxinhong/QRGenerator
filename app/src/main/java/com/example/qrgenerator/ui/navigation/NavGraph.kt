package com.example.qrgenerator.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.qrgenerator.ui.screens.generate.GenerateScreen
import com.example.qrgenerator.ui.screens.history.HistoryScreen
import com.example.qrgenerator.ui.screens.settings.SettingsScreen
import com.example.qrgenerator.ui.screens.settings.SettingsViewModel
import com.example.qrgenerator.ui.screens.style.StyleScreen

/** 导航图 */
@Composable
fun NavGraph(navController: NavHostController, settingsViewModel: SettingsViewModel) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.GENERATE.route
    ) {
        composable(BottomNavItem.GENERATE.route) {
            GenerateScreen(onOpenStyle = {
                navController.navigate(BottomNavItem.STYLE.route) { launchSingleTop = true }
            })
        }
        composable(BottomNavItem.STYLE.route) {
            StyleScreen(onDone = {
                navController.navigate(BottomNavItem.GENERATE.route) {
                    popUpTo(BottomNavItem.GENERATE.route) { inclusive = false }
                    launchSingleTop = true
                }
            })
        }
        composable(BottomNavItem.HISTORY.route) {
            HistoryScreen(onOpenGenerator = {
                navController.navigate(BottomNavItem.GENERATE.route) {
                    popUpTo(BottomNavItem.GENERATE.route) { inclusive = false }
                    launchSingleTop = true
                }
            })
        }
        composable(BottomNavItem.SETTINGS.route) {
            SettingsScreen(viewModel = settingsViewModel)
        }
    }
}
