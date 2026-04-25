package com.example.qrgenerator.ui.screens.style

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
import androidx.compose.runtime.getValue
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 模块形状
        StyleCard(title = "模块形状") {
            ShapeChipGroup(
                title = "暗色模块",
                options = QrPixelShapeType.entries,
                selected = config.darkPixelShape,
                displayName = { it.displayName },
                onSelected = { viewModel.executeUpdatePixelShape(it) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ShapeChipGroup(
                title = "亮色模块",
                options = QrPixelShapeType.entries,
                selected = config.lightPixelShape,
                displayName = { it.displayName },
                onSelected = { viewModel.executeUpdatePixelShape(it) }
            )
        }

        // 眼睛形状
        StyleCard(title = "眼睛形状") {
            ShapeChipGroup(
                title = "外框",
                options = QrFrameShapeType.entries,
                selected = config.frameShape,
                displayName = { it.displayName },
                onSelected = { viewModel.executeUpdateFrameShape(it) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ShapeChipGroup(
                title = "内球",
                options = QrBallShapeType.entries,
                selected = config.ballShape,
                displayName = { it.displayName },
                onSelected = { viewModel.executeUpdateBallShape(it) }
            )
        }

        // 颜色
        StyleCard(title = "颜色") {
            ColorGridPicker(
                title = "前景色",
                selectedColor = config.darkColor.solidColor,
                onColorSelected = {
                    viewModel.executeUpdateDarkColor(QrColorConfig(solidColor = it))
                }
            )
        }

        // 容错级别
        StyleCard(title = "容错级别") {
            ShapeChipGroup(
                title = "",
                options = QrErrorLevel.entries,
                selected = config.errorCorrectionLevel,
                displayName = { it.displayName },
                onSelected = { viewModel.executeUpdateErrorLevel(it) }
            )
        }

        // 整体外形
        StyleCard(title = "整体外形") {
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

/** 样式卡片容器 */
@Composable
private fun StyleCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}
