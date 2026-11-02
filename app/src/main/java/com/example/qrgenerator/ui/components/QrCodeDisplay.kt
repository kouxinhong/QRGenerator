package com.example.qrgenerator.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** 白色预览底保证深色主题下也能观察二维码实际效果。 */
@Composable
fun QrCodeDisplay(qrDrawable: Drawable?, isGenerating: Boolean, modifier: Modifier = Modifier) {
    val previewSize: Modifier = if (qrDrawable == null && !isGenerating) Modifier.fillMaxWidth().height(90.dp) else Modifier.widthIn(max = 200.dp).fillMaxWidth().aspectRatio(1f)
    Box(modifier = modifier.then(previewSize).background(Color.White), contentAlignment = Alignment.Center) {
        when {
            isGenerating -> CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
            qrDrawable != null -> AndroidView(
                factory = { context -> android.widget.ImageView(context).apply { scaleType = android.widget.ImageView.ScaleType.FIT_CENTER; contentDescription = "生成的二维码" } },
                update = { it.setImageDrawable(qrDrawable) }, modifier = Modifier.fillMaxSize()
            )
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.QrCode2, null, Modifier.size(36.dp), tint = Color(0xFFB5B8B6))
                Text("填好内容，二维码就会出现", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF666666))
            }
        }
    }
}
