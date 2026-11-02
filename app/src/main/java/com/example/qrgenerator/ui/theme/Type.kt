package com.example.qrgenerator.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 系统中文字体随设备和用户字号设置，避免装饰字体影响阅读。
private fun textStyle(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal): TextStyle =
    TextStyle(fontFamily = FontFamily.Default, fontSize = size.sp, lineHeight = lineHeight.sp,
        fontWeight = weight, letterSpacing = 0.sp)

val Typography: Typography = Typography(
    displayLarge = textStyle(32, 40, FontWeight.Bold),
    displayMedium = textStyle(28, 36, FontWeight.Bold),
    displaySmall = textStyle(24, 32, FontWeight.Bold),
    headlineLarge = textStyle(24, 32, FontWeight.SemiBold),
    headlineMedium = textStyle(20, 28, FontWeight.SemiBold),
    headlineSmall = textStyle(18, 26, FontWeight.SemiBold),
    titleLarge = textStyle(18, 26, FontWeight.Medium),
    titleMedium = textStyle(16, 24, FontWeight.Medium),
    titleSmall = textStyle(14, 22, FontWeight.Medium),
    bodyLarge = textStyle(16, 24), bodyMedium = textStyle(14, 22), bodySmall = textStyle(12, 18),
    labelLarge = textStyle(16, 24, FontWeight.Medium),
    labelMedium = textStyle(14, 20, FontWeight.Medium), labelSmall = textStyle(12, 18)
)
