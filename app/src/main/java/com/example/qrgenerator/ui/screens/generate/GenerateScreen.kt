package com.example.qrgenerator.ui.screens.generate

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import com.example.qrgenerator.ui.components.ContentTypeChipGroup
import com.example.qrgenerator.ui.components.ErrorDialog
import com.example.qrgenerator.ui.components.QrCodeDisplay

/** 生成页面 */
@Composable
fun GenerateScreen(viewModel: GenerateViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 入场动画状态
    var isLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isLoaded = true }

    // 处理分享 Intent
    LaunchedEffect(uiState.shareIntent) {
        uiState.shareIntent?.let { intent ->
            context.startActivity(Intent.createChooser(intent, "分享二维码"))
            viewModel.executeClearShareIntent()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // QR 码预览 — 带入场动画
        AnimatedVisibility(
            visible = isLoaded,
            enter = fadeIn(tween(400)) + slideInVertically(
                tween(400),
                initialOffsetY = { it / 8 }
            )
        ) {
            QrCodeDisplay(
                qrDrawable = uiState.qrDrawable,
                isGenerating = uiState.isGenerating,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 内容类型选择 — 延迟入场
        AnimatedVisibility(
            visible = isLoaded,
            enter = fadeIn(tween(400, delayMillis = 100)) + slideInVertically(
                tween(400, delayMillis = 100),
                initialOffsetY = { it / 8 }
            )
        ) {
            ContentTypeChipGroup(
                selectedType = uiState.config.contentType,
                onTypeSelected = { viewModel.executeUpdateContentType(it) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 内容输入 — 延迟入场
        AnimatedVisibility(
            visible = isLoaded,
            enter = fadeIn(tween(400, delayMillis = 200)) + slideInVertically(
                tween(400, delayMillis = 200),
                initialOffsetY = { it / 8 }
            )
        ) {
            OutlinedTextField(
                value = uiState.config.content,
                onValueChange = { viewModel.executeUpdateContent(it) },
                label = {
                    Text(
                        "输入${uiState.config.contentType.displayName}内容",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 保存和分享按钮 — 延迟入场
        AnimatedVisibility(
            visible = isLoaded,
            enter = fadeIn(tween(400, delayMillis = 300)) + slideInVertically(
                tween(400, delayMillis = 300),
                initialOffsetY = { it / 8 }
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.executeSaveToGallery() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = uiState.qrDrawable != null
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(" 保存", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = { viewModel.executeShare() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = uiState.qrDrawable != null
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(" 分享", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    // 错误弹窗
    uiState.errorMessage?.let { message ->
        ErrorDialog(
            title = "生成失败",
            message = message,
            onConfirm = { viewModel.executeClearError() }
        )
    }
}
