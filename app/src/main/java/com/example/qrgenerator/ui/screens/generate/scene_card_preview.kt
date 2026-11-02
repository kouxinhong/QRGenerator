package com.example.qrgenerator.ui.screens.generate

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.qrgenerator.data.model.QrCardConfig
import com.example.qrgenerator.domain.QrReadability

/** 直接展示将要导出的图片，保留竖版卡片的真实比例。 */
@Composable
fun SceneCardPreview(drawable: Drawable?, isGenerating: Boolean, card: QrCardConfig) {
    Box(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isGenerating -> Column(
                modifier = Modifier.heightIn(min = 160.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                Text("正在生成并检查…", style = MaterialTheme.typography.bodyMedium)
            }
            drawable != null -> RenderedCardImage(drawable, card)
            else -> Column(
                modifier = Modifier.heightIn(min = 144.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                Icon(Icons.Outlined.QrCode2, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("填好内容，就能看到成品", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RenderedCardImage(drawable: Drawable, card: QrCardConfig) {
    val ratio: Float = if (drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
        drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
    } else 1f
    AndroidView(
        factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
        update = { imageView ->
            imageView.setImageDrawable(drawable)
            imageView.contentDescription = if (card.enabled) {
                val title: String = card.title.trim().ifBlank { card.scene.defaultTitle }
                val subtitle: String = card.subtitle.trim().ifBlank { card.scene.defaultSubtitle }
                "将要保存的二维码小卡。标题：$title。扫码提示：$subtitle"
            } else "将要保存的二维码"
        },
        modifier = Modifier.widthIn(max = 260.dp).fillMaxWidth().aspectRatio(ratio).background(Color.White)
    )
}

@Composable
fun ReadabilityStatus(readability: QrReadability?, onRestoreStyle: () -> Unit) {
    when (readability) {
        QrReadability.VERIFIED -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text("本机识别检查通过", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text("实际扫码仍受手机、光线和打印效果影响，使用前请试扫。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        QrReadability.UNREADABLE -> Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                    Text("当前样式未通过本机识别检查", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                }
                Text("换回清晰样式后再保存，已填内容和卡片文字会保留。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                TextButton(onClick = onRestoreStyle) { Text("恢复清晰样式") }
            }
        }
        null -> Unit
    }
}
