package com.example.qrgenerator.ui.scanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qrgenerator.data.model.ThemeMode
import com.example.qrgenerator.ui.screens.settings.SettingsViewModel
import com.example.qrgenerator.ui.theme.QRGeneratorTheme
import org.koin.androidx.compose.koinViewModel

class QrScanActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?): Unit {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val isDark: Boolean = when (settings.themeMode) {
                ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            QRGeneratorTheme(darkTheme = isDark) {
                ScanScreen(
                    activity = this,
                    onClose = ::finish,
                    onUseContent = ::returnContent,
                    targetScene = QrScanContract.readTargetScene(intent),
                    initialSource = QrScanContract.readSource(intent)
                )
            }
        }
    }

    private fun returnContent(content: String): Unit {
        setResult(RESULT_OK, Intent().putExtra(QrScanContract.RESULT_CONTENT, content))
        finish()
    }
}
