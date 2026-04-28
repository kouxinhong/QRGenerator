package com.example.qrgenerator.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** QR 码预览组件 */
@Composable
fun QrCodeDisplay(
    qrDrawable: Drawable?,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth(0.72f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .drawBehind {
                // 微妙的径向渐变背景
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            surfaceVariant,
                            surfaceVariant.copy(alpha = 0.6f)
                        ),
                        center = Offset(size.width / 2, size.height / 2),
                        radius = size.width * 0.8f
                    )
                )
            }
            .border(
                width = 1.dp,
                color = outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isGenerating,
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.8f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.8f)
        ) {
            CircularProgressIndicator(
                color = primary,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(36.dp)
            )
        }

        AnimatedVisibility(
            visible = qrDrawable != null && !isGenerating,
            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.9f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.9f)
        ) {
            AndroidView(
                factory = { context ->
                    android.widget.ImageView(context).apply {
                        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                    }
                },
                update = { it.setImageDrawable(qrDrawable) },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        }

        AnimatedVisibility(
            visible = qrDrawable == null && !isGenerating,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200))
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCode2,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = onSurfaceVariant.copy(alpha = 0.4f)
                )
                Text(
                    text = "输入内容后自动生成",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
