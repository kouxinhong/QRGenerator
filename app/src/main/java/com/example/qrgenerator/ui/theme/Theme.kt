package com.example.qrgenerator.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 暗色主题配色方案
 */
private val DarkColorScheme = darkColorScheme(
    primary = XiaomiOrange,
    onPrimary = Color.White,
    primaryContainer = XiaomiOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = XiaomiOrangeLight,
    onSecondary = Color.White,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurface,
    tertiary = Info,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnBackground,
    outline = DarkDivider,
    error = Error,
    onError = Color.White
)

/**
 * 亮色主题配色方案
 */
private val LightColorScheme = lightColorScheme(
    primary = XiaomiOrange,
    onPrimary = Color.White,
    primaryContainer = XiaomiOrangeLight,
    onPrimaryContainer = Color.White,
    secondary = XiaomiOrangeDark,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightOnSurface,
    tertiary = Info,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnBackground,
    outline = LightDivider,
    error = Error,
    onError = Color.White
)

/**
 * QR 生成器主题
 * 参考 MIMO 小米社区的简洁设计风格
 *
 * @param darkTheme 是否使用暗色主题
 * @param content 内容 Composable
 */
@Composable
fun QRGeneratorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
