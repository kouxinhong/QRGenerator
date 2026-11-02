package com.example.qrgenerator.ui.screens.style

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.qrgenerator.data.model.*
import com.example.qrgenerator.ui.components.ShapeChipGroup
import java.io.File

@Composable
internal fun ShapeOptions(config: QrConfig, update: ((QrConfig) -> QrConfig) -> Unit) {
    PixelPicker("小格子的形状", config.darkPixelShape) { value -> update { it.copy(darkPixelShape = value) } }
    ShapeChipGroup("整体轮廓", CodeShapeType.entries, config.codeShape, { it.displayName }, { value -> update { it.copy(codeShape = value) } })
    if (config.codeShape == CodeShapeType.CIRCLE) ValueSlider("圆形外圈大小", config.circlePadding, 1f..2f, onChange = { value -> update { it.copy(circlePadding = value) } })
    ShapeChipGroup("三个角的外框", QrFrameShapeType.entries, config.frameShape, { it.displayName }, { value -> update { it.copy(frameShape = value) } })
    ShapeChipGroup("三个角的中心", QrBallShapeType.entries, config.ballShape, { it.displayName }, { value -> update { it.copy(ballShape = value) } })
    HelpText("三个角用来帮助手机找到二维码。想更容易识别，可以保留方形或圆角。")
}

@Composable
internal fun ColorOptions(config: QrConfig, update: ((QrConfig) -> QrConfig) -> Unit) {
    var target by rememberSaveable { mutableStateOf("主色") }
    ShapeChipGroup("调整哪一部分", listOf("主色", "浅色格子", "角上外框", "角上中心"), target, { it }, { target = it })
    val color = when (target) {
        "浅色格子" -> config.lightColor
        "角上外框" -> config.frameColor
        "角上中心" -> config.ballColor
        else -> config.darkColor
    }
    ColorEditor("", color, allowFollowDark = target == "角上外框" || target == "角上中心") { value ->
        update {
            when (target) {
                "浅色格子" -> it.copy(lightColor = value)
                "角上外框" -> it.copy(frameColor = value)
                "角上中心" -> it.copy(ballColor = value)
                else -> it.copy(darkColor = value)
            }
        }
    }
    HelpText("深色图案配浅色背景更容易扫。保存后，先用手机扫一扫。")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LogoOptions(config: QrConfig, update: ((QrConfig) -> QrConfig) -> Unit, onError: (String) -> Unit) {
    val actions = rememberStyleImageActions(onSelected = { path, camera ->
        update { it.copy(logo = it.logo.copy(imagePath = path, builtinIconName = null, source = if (camera) LogoSource.CAMERA else LogoSource.GALLERY, paddingType = LogoPaddingType.NATURAL, paddingValue = 0.12f), errorCorrectionLevel = QrErrorLevel.HIGH) }
    }, onError = onError)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = actions.selectImage, modifier = Modifier.heightIn(min = 48.dp)) { Text("从相册选择") }
        OutlinedButton(onClick = actions.takePhoto, modifier = Modifier.heightIn(min = 48.dp)) { Text("拍一张") }
    }
    ShapeChipGroup("也可以用小图标", listOf("heart", "star", "smile", "leaf"), config.logo.builtinIconName, { when (it) { "heart" -> "爱心"; "star" -> "星星"; "smile" -> "笑脸"; else -> "叶子" } }, { value ->
        update { it.copy(logo = it.logo.copy(builtinIconName = value, imagePath = null, source = LogoSource.BUILTIN_ICON, paddingType = LogoPaddingType.NATURAL, paddingValue = 0.12f), errorCorrectionLevel = QrErrorLevel.HIGH) }
    })
    if (config.logo.imagePath != null || config.logo.builtinIconName != null) {
        config.logo.imagePath?.let { path -> AsyncImage(File(path), "已选择的小图", modifier = Modifier.size(72.dp)) }
        TextButton(onClick = { update { it.copy(logo = QrLogoConfig()) } }, modifier = Modifier.heightIn(min = 48.dp)) { Text("移除小图") }
        ValueSlider("小图大小", config.logo.size, 0.1f..0.3f, onChange = { value -> update { it.copy(logo = it.logo.copy(size = value)) } })
        ShapeChipGroup("小图形状", LogoShapeType.entries, config.logo.shape, { it.displayName }, { value -> update { it.copy(logo = it.logo.copy(shape = value)) } })
        if (config.logo.shape == LogoShapeType.ROUND_CORNERS) ValueSlider("圆角大小", config.logo.cornerRadius, 0f..0.5f, onChange = { value -> update { it.copy(logo = it.logo.copy(cornerRadius = value)) } })
        ShapeChipGroup("图片显示方式", ImageScaleType.entries, config.logo.scale, { it.displayName }, { value -> update { it.copy(logo = it.logo.copy(scale = value)) } })
        ShapeChipGroup("小图周围留白", LogoPaddingType.entries, config.logo.paddingType, { it.displayName }, { value -> update { it.copy(logo = it.logo.copy(paddingType = value)) } })
        if (config.logo.paddingType != LogoPaddingType.EMPTY) ValueSlider("留白大小", config.logo.paddingValue, 0f..0.3f, onChange = { value -> update { it.copy(logo = it.logo.copy(paddingValue = value)) } })
        var showLogoColor by rememberSaveable { mutableStateOf(false) }
        TextButton(onClick = { showLogoColor = !showLogoColor }, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (showLogoColor) "收起小图底色" else "设置小图底色") }
        if (showLogoColor) ColorEditor("小图底色", config.logo.backgroundColor) { value -> update { it.copy(logo = it.logo.copy(backgroundColor = value)) } }
        HelpText("已开启较强的识别保护。小图不要太大，保存后先试扫。")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BackgroundOptions(config: QrConfig, update: ((QrConfig) -> QrConfig) -> Unit, onError: (String) -> Unit) {
    val actions = rememberStyleImageActions(onSelected = { path, _ ->
        update { it.copy(background = it.background.copy(imagePath = path), lightColor = QrColorConfig(type = QrColorType.TRANSPARENT), errorCorrectionLevel = QrErrorLevel.HIGH, highlighting = it.highlighting.copy(cornerEyes = HighlightingMode.DEFAULT, timingLines = HighlightingMode.DEFAULT)) }
    }, onError = onError)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = actions.selectImage, modifier = Modifier.heightIn(min = 48.dp)) { Text("选择背景图片") }
        if (config.background.imagePath != null) TextButton(onClick = { update { it.copy(background = it.background.copy(imagePath = null), lightColor = QrColorConfig(solidColor = android.graphics.Color.WHITE)) } }, modifier = Modifier.heightIn(min = 48.dp)) { Text("移除图片") }
    }
    config.background.imagePath?.let { path ->
        AsyncImage(File(path), "已选择的背景图片", modifier = Modifier.fillMaxWidth().height(120.dp))
        ShapeChipGroup("图片显示方式", ImageScaleType.entries, config.background.scale, { it.displayName }, { value -> update { it.copy(background = it.background.copy(scale = value)) } })
        HelpText("已让浅色格子透明，背景才能显示出来。建议使用浅色、简单的图片。")
    }
    ColorEditor("背景底色", config.background.color) { value -> update { it.copy(background = it.background.copy(color = value)) } }
}

@Composable
internal fun AdvancedOptions(config: QrConfig, update: ((QrConfig) -> QrConfig) -> Unit) {
    HelpText("不确定怎么选时，保持默认即可。下面的设置会同时用于预览和保存。")
    ShapeChipGroup("识别保护（容错）", QrErrorLevel.entries, config.errorCorrectionLevel, { it.displayName }, { value -> update { it.copy(errorCorrectionLevel = value) } })
    ValueSlider("四周留白", config.padding, 0.05f..0.4f, onChange = { value -> update { it.copy(padding = value) } })
    HelpText("白边越宽，越容易扫。建议保持 28% 或更多。")
    Text("图片大小", style = MaterialTheme.typography.titleMedium)
    ShapeChipGroup("常用尺寸", listOf(512, 1024, 2048), if (config.imageWidth == config.imageHeight) config.imageWidth else -1, { "$it 像素" }, { value -> update { it.copy(imageWidth = value, imageHeight = value) } })
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DimensionField("宽度", config.imageWidth, Modifier.weight(1f)) { value -> update { it.copy(imageWidth = value) } }
        DimensionField("高度", config.imageHeight, Modifier.weight(1f)) { value -> update { it.copy(imageHeight = value) } }
    }
    ValueSlider("左右移动", config.offsetX, -0.25f..0.25f, onChange = { value -> update { it.copy(offsetX = value) } })
    ValueSlider("上下移动", config.offsetY, -0.25f..0.25f, onChange = { value -> update { it.copy(offsetY = value) } })
    Divider()
    Text("形状细节", style = MaterialTheme.typography.titleMedium)
    PixelPicker("浅色格子的形状", config.lightPixelShape) { value -> update { it.copy(lightPixelShape = value) } }
    val pixelShapes = listOf(config.darkPixelShape, config.lightPixelShape)
    if (pixelShapes.any { it == QrPixelShapeType.CIRCLE || it == QrPixelShapeType.RHOMBUS }) {
        ValueSlider("圆形和菱形格子的大小", config.pixelSize, 0.5f..1f, onChange = { value -> update { it.copy(pixelSize = value) } })
    }
    if (pixelShapes.any { it == QrPixelShapeType.ROUND_CORNERS || it == QrPixelShapeType.ROUND_CORNERS_HORIZONTAL || it == QrPixelShapeType.ROUND_CORNERS_VERTICAL }) {
        ValueSlider("小格子圆角", config.pixelCornerRadius, 0f..0.5f, onChange = { value -> update { it.copy(pixelCornerRadius = value) } })
    }
    if (config.ballShape == QrBallShapeType.CIRCLE || config.ballShape == QrBallShapeType.RHOMBUS) {
        ValueSlider("角上中心大小", config.ballSize, 0.5f..1f, onChange = { value -> update { it.copy(ballSize = value) } })
    }
    if (config.ballShape == QrBallShapeType.ROUND_CORNERS) {
        ValueSlider("角上中心圆角", config.ballCornerRadius, 0f..0.5f, onChange = { value -> update { it.copy(ballCornerRadius = value) } })
    }
    if (config.frameShape == QrFrameShapeType.CIRCLE || config.frameShape == QrFrameShapeType.ROUND_CORNERS) {
        ValueSlider("角上外框粗细", config.frameWidth, 0.5f..1.5f, onChange = { value -> update { it.copy(frameWidth = value) } })
    }
    if (config.frameShape == QrFrameShapeType.ROUND_CORNERS) {
        ValueSlider("角上外框圆角", config.frameCornerRadius, 0f..0.5f, onChange = { value -> update { it.copy(frameCornerRadius = value) } })
    }
    ToggleRow("对称排列", config.centralSymmetry, "让三个角的形状朝向中心") { value -> update { it.copy(centralSymmetry = value) } }
    ToggleRow("右下角再加一个定位框", config.fourthEyeEnabled, "会遮挡部分内容，使用后请试扫") { value -> update { it.copy(fourthEyeEnabled = value, errorCorrectionLevel = if (value) QrErrorLevel.HIGH else it.errorCorrectionLevel) } }
    Divider()
    HighlightOptions(config.highlighting) { value -> update { it.copy(highlighting = value) } }
}

@Composable
private fun DimensionField(title: String, value: Int, modifier: Modifier, onChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input.filter(Char::isDigit).take(4)
            text.toIntOrNull()?.takeIf { it in 256..4096 }?.let(onChange)
        },
        label = { Text("$title（像素）") },
        supportingText = { Text("256–4096") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        isError = (text.toIntOrNull() ?: 0) !in 256..4096,
        modifier = modifier
    )
}

@Composable
private fun HighlightOptions(config: QrHighlightingConfig, onChange: (QrHighlightingConfig) -> Unit) {
    Text("让识别区域更清楚", style = MaterialTheme.typography.titleMedium)
    HelpText("使用背景图片时，给重要区域加一层浅色底。")
    ShapeChipGroup("三个角", HighlightingMode.entries, config.cornerEyes, { it.displayName }, { onChange(config.copy(cornerEyes = it)) })
    ShapeChipGroup("内部定位区域", HighlightingMode.entries, config.versionEyes, { it.displayName }, { onChange(config.copy(versionEyes = it)) })
    ShapeChipGroup("连接三个角的线", HighlightingMode.entries, config.timingLines, { it.displayName }, { onChange(config.copy(timingLines = it)) })
    if (config.cornerEyes != HighlightingMode.NONE || config.versionEyes != HighlightingMode.NONE || config.timingLines != HighlightingMode.NONE) {
        ValueSlider("底色不透明度", config.alpha, 0f..1f, onChange = { onChange(config.copy(alpha = it)) })
        if (config.cornerEyes == HighlightingMode.CUSTOM || config.versionEyes == HighlightingMode.CUSTOM || config.timingLines == HighlightingMode.CUSTOM) {
            PixelPicker("保护底的形状", config.shape) { onChange(config.copy(shape = it)) }
            ColorEditor("保护底的颜色", config.color) { onChange(config.copy(color = it)) }
        }
    }
}
