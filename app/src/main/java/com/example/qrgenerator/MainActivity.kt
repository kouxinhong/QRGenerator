package com.example.qrgenerator

import android.os.Bundle
import android.widget.Toast
import com.example.qrgenerator.domain.QrConfigHolder
import com.example.qrgenerator.util.QrConfigSerializer
import org.koin.android.ext.android.inject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qrgenerator.data.model.ThemeMode
import com.example.qrgenerator.ui.screens.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.qrgenerator.ui.navigation.BottomNavBar
import com.example.qrgenerator.ui.navigation.NavGraph
import com.example.qrgenerator.ui.theme.QRGeneratorTheme

/** 主 Activity */
class MainActivity : ComponentActivity() {
    private val configHolder: QrConfigHolder by inject()

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("qr_draft", QrConfigSerializer.serialize(configHolder.config.value))
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getString("qr_draft")?.let { json ->
            runCatching { configHolder.executeSetConfig(QrConfigSerializer.deserialize(json)) }
                .onFailure { Toast.makeText(this, "上次填写的内容未能恢复，请重新填写", Toast.LENGTH_LONG).show() }
        }
        setContent {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            QRGeneratorTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        BottomNavBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavGraph(navController = navController, settingsViewModel = settingsViewModel)
                    }
                }
            }
        }
    }
}
