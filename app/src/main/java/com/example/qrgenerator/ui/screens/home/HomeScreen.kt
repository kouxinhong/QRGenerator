package com.example.qrgenerator.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qrgenerator.ui.components.BallShapeSelector
import com.example.qrgenerator.ui.components.ColorPicker
import com.example.qrgenerator.ui.components.ContentTypeSelector
import com.example.qrgenerator.ui.components.ErrorLevelSelector
import com.example.qrgenerator.ui.components.FrameShapeSelector
import com.example.qrgenerator.ui.components.PixelShapeSelector
import com.example.qrgenerator.ui.components.QrCodeDisplay

/**
 * 首页界面
 * 包含二维码预览和所有自定义选项
 *
 * @param viewModel 首页 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "二维码生成器",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // QR 码预览区域
            QrCodeDisplay(
                qrDrawable = uiState.qrDrawable,
                backgroundColor = uiState.backgroundColor,
                isGenerating = uiState.isGenerating,
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 内容输入
            OutlinedTextField(
                value = uiState.content,
                onValueChange = { viewModel.executeUpdateContent(it) },
                label = {
                    Text(
                        text = when (uiState.contentType) {
                            com.example.qrgenerator.data.QrContentType.TEXT -> "输入文本内容"
                            com.example.qrgenerator.data.QrContentType.URL -> "输入网址 (如 https://example.com)"
                            com.example.qrgenerator.data.QrContentType.WIFI -> "WiFi 信息 (SSID/密码/加密类型)"
                            com.example.qrgenerator.data.QrContentType.VCARD -> "联系人 (姓名/公司/职位/电话/邮箱)"
                            com.example.qrgenerator.data.QrContentType.PHONE -> "输入电话号码"
                            com.example.qrgenerator.data.QrContentType.SMS -> "输入手机号码"
                            com.example.qrgenerator.data.QrContentType.EMAIL -> "输入邮箱地址"
                            com.example.qrgenerator.data.QrContentType.GEO -> "坐标 (纬度,经度)"
                            com.example.qrgenerator.data.QrContentType.BOOKMARK -> "输入网址"
                            com.example.qrgenerator.data.QrContentType.EVENT -> "输入事件名称"
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5
            )

            // 生成按钮
            Button(
                onClick = { viewModel.executeGenerateQrCode() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                enabled = uiState.content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "生成二维码",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 内容类型选择
            ContentTypeSelector(
                selectedType = uiState.contentType,
                onTypeSelected = { viewModel.executeUpdateContentType(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 容错级别选择
            ErrorLevelSelector(
                selectedLevel = uiState.errorCorrectionLevel,
                onLevelSelected = { viewModel.executeUpdateErrorLevel(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 模块形状选择
            PixelShapeSelector(
                selectedShape = uiState.pixelShape,
                onShapeSelected = { viewModel.executeUpdatePixelShape(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 眼睛外框形状选择
            FrameShapeSelector(
                selectedShape = uiState.frameShape,
                onShapeSelected = { viewModel.executeUpdateFrameShape(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 眼睛内球形状选择
            BallShapeSelector(
                selectedShape = uiState.ballShape,
                onShapeSelected = { viewModel.executeUpdateBallShape(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 前景色选择
            ColorPicker(
                title = "前景色",
                selectedColor = uiState.foregroundColor,
                onColorSelected = { viewModel.executeUpdateForegroundColor(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 背景色选择
            ColorPicker(
                title = "背景色",
                selectedColor = uiState.backgroundColor,
                onColorSelected = { viewModel.executeUpdateBackgroundColor(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
