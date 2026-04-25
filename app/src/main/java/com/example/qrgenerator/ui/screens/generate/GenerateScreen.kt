package com.example.qrgenerator.ui.screens.generate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qrgenerator.ui.components.ContentTypeChipGroup
import com.example.qrgenerator.ui.components.ErrorDialog
import com.example.qrgenerator.ui.components.QrCodeDisplay

/** 生成页面 */
@Composable
fun GenerateScreen(viewModel: GenerateViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // QR 码预览
        QrCodeDisplay(
            qrDrawable = uiState.qrDrawable,
            isGenerating = uiState.isGenerating
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 内容类型选择
        ContentTypeChipGroup(
            selectedType = uiState.config.contentType,
            onTypeSelected = { viewModel.executeUpdateContentType(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 内容输入
        OutlinedTextField(
            value = uiState.config.content,
            onValueChange = { viewModel.executeUpdateContent(it) },
            label = { Text("输入${uiState.config.contentType.displayName}内容") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 5,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 保存和分享按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { /* 保存逻辑 */ },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = uiState.qrDrawable != null
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Text(" 保存")
            }
            Button(
                onClick = { /* 分享逻辑 */ },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = uiState.qrDrawable != null
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Text(" 分享")
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
