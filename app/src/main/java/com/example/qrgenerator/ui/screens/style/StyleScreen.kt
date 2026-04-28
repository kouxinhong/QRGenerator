package com.example.qrgenerator.ui.screens.style

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import com.example.qrgenerator.data.model.*
import com.example.qrgenerator.ui.components.ColorGridPicker
import com.example.qrgenerator.ui.components.ShapeChipGroup

/** 样式页面 */
@Composable
fun StyleScreen(viewModel: StyleViewModel = koinViewModel()) {
    val config by viewModel.config.collectAsStateWithLifecycle()

    var isLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isLoaded = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // 模块形状
        AnimatedStyleCard(
            title = "模块形状",
            isVisible = isLoaded,
            delayMillis = 0
        ) {
            ShapeChipGroup(
                title = "暗色模块",
                options = QrPixelShapeType.entries,
                selected = config.darkPixelShape,
                displayName = { it.displayName },
                onSelected = { viewModel.executeUpdatePixelShape(it) }
            )
            Spacer(modifier = Modifier.height(14.dp))
            ShapeChipGroup(
                title = "亮色模块",
                options = QrPixelShapeType.entries,
                selected = config.lightPixelShape,
                displayName = { it.displayName },
                onSelected = { viewModel.executeUpdateLightPixelShape(it) }
            )
        }

        // 眼睛形状
        AnimatedStyleCard(
            title = "眼睛形状",
            isVisible = isLoaded,
            delayMillis = 80
        ) {
            ShapeChipGroup(
                title = "外框",
                options = QrFrameShapeType.entries,
                selected = config.frameShape,
                displayName = { it.displayName },
                onSelected = { viewModel.executeUpdateFrameShape(it) }
            )
            Spacer(modifier = Modifier.height(14.dp))
            ShapeChipGroup(
                title = "内球",
                options = QrBallShapeType.entries,
                selected = config.ballShape,
                displayName = { it.displayName },
                onSelected = { viewModel.executeUpdateBallShape(it) }
            )
        }

        // 颜色
        AnimatedStyleCard(
            title = "颜色",
            isVisible = isLoaded,
            delayMillis = 160
        ) {
            ColorGridPicker(
                title = "前景色",
                selectedColor = config.darkColor.solidColor,
                onColorSelected = {
                    viewModel.executeUpdateDarkColor(QrColorConfig(solidColor = it))
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
            ColorGridPicker(
                title = "眼睛外框",
                selectedColor = config.frameColor.solidColor,
                onColorSelected = {
                    viewModel.executeUpdateFrameColor(QrColorConfig(solidColor = it))
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
            ColorGridPicker(
                title = "眼睛内球",
                selectedColor = config.ballColor.solidColor,
                onColorSelected = {
                    viewModel.executeUpdateBallColor(QrColorConfig(solidColor = it))
                }
            )
        }

        // 容错级别
        AnimatedStyleCard(
            title = "容错级别",
            isVisible = isLoaded,
            delayMillis = 240
        ) {
            ShapeChipGroup(
                title = "",
                options = QrErrorLevel.entries,
                selected = config.errorCorrectionLevel,
                displayName = { it.displayName },
                onSelected = { viewModel.executeUpdateErrorLevel(it) }
            )
        }

        // 整体外形
        AnimatedStyleCard(
            title = "整体外形",
            isVisible = isLoaded,
            delayMillis = 320
        ) {
            ShapeChipGroup(
                title = "",
                options = CodeShapeType.entries,
                selected = config.codeShape,
                displayName = { it.displayName },
                onSelected = { viewModel.executeUpdateCodeShape(it) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/** 带入场动画的样式卡片 */
@Composable
private fun AnimatedStyleCard(
    title: String,
    isVisible: Boolean,
    delayMillis: Int,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(400, delayMillis = delayMillis)) + slideInVertically(
            tween(400, delayMillis = delayMillis),
            initialOffsetY = { it / 10 }
        )
    ) {
        StyleCard(title = title, content = content)
    }
}

/** 样式卡片容器 */
@Composable
private fun StyleCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}
