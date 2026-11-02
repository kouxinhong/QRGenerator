@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.qrgenerator.ui.screens.generate

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.domain.QrContentCodec
import java.time.LocalDateTime

private data class ContentField(
    val key: String,
    val label: String,
    val hint: String = "",
    val isExtra: Boolean = false,
    val isMultiline: Boolean = false,
    val isDate: Boolean = false,
    val keyboard: KeyboardType = KeyboardType.Text
)

@Composable
fun ContentForm(type: QrContentType, content: String, onContentChange: (String) -> Unit) {
    val values: Map<String, String> = QrContentCodec.fields(type, content)
    var showExtra: Boolean by rememberSaveable(type) { mutableStateOf(false) }
    val fields: List<ContentField> = contentFields(type)
    val update: (String, String) -> Unit = { key, value ->
        if (type == QrContentType.TEXT) onContentChange(value)
        else onContentChange(QrContentCodec.serialize(values + (key to value)))
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        fields.filter { !it.isExtra || showExtra }.forEach { field ->
            if (!(type == QrContentType.WIFI && field.key == "password" && values["auth"] == "OPEN")) {
                ContentInput(field, values[field.key].orEmpty()) { update(field.key, it) }
            }
        }
        if (type == QrContentType.WIFI) {
            ChoiceInput("是否需要密码", listOf("WPA" to "需要密码", "OPEN" to "不需要密码", "WEP" to "老式网络"), values["auth"].orEmpty().ifBlank { "WPA" }) { update("auth", it) }
        }
        if (type == QrContentType.ENTERPRISE_WIFI) {
            ChoiceInput("认证方法", listOf("PEAP" to "PEAP", "TLS" to "TLS", "TTLS" to "TTLS", "PWD" to "PWD"), values["eap"].orEmpty().ifBlank { "PEAP" }) { update("eap", it) }
        }
        if (showExtra && type in listOf(QrContentType.WIFI, QrContentType.ENTERPRISE_WIFI)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("这是隐藏网络", modifier = Modifier.padding(top = 14.dp))
                Switch(checked = values["hidden"] == "true", onCheckedChange = { update("hidden", it.toString()) })
            }
        }
        if (type == QrContentType.SMS && showExtra) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("使用彩信", modifier = Modifier.padding(top = 14.dp))
                Switch(checked = values["mms"] == "true", onCheckedChange = { update("mms", it.toString()) })
            }
        }
        if (fields.any { it.isExtra } || type in listOf(QrContentType.WIFI, QrContentType.SMS)) {
            TextButton(onClick = { showExtra = !showExtra }) { Text(if (showExtra) "收起其他信息" else "填写其他信息（可选）") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceInput(label: String, choices: List<Pair<String, String>>, value: String, onChange: (String) -> Unit) {
    Text(label, style = MaterialTheme.typography.bodyMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { (key, title) -> FilterChip(selected = key == value, onClick = { onChange(key) }, label = { Text(title) }, modifier = Modifier.heightIn(min = 48.dp)) }
    }
}

@Composable
private fun ContentInput(field: ContentField, value: String, onChange: (String) -> Unit) {
    val context = LocalContext.current
    var isPasswordVisible: Boolean by rememberSaveable(field.key) { mutableStateOf(false) }
    val isPassword: Boolean = field.key == "password"
    val pickDate: () -> Unit = {
        val now: LocalDateTime = runCatching { LocalDateTime.parse(value, QrContentCodec.dateFormat) }.getOrElse { LocalDateTime.now() }
        DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute ->
                onChange(LocalDateTime.of(year, month + 1, day, hour, minute).format(QrContentCodec.dateFormat))
            }, now.hour, now.minute, true).show()
        }, now.year, now.monthValue - 1, now.dayOfMonth).show()
    }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(field.label) },
            placeholder = { Text(field.hint) },
            modifier = Modifier.fillMaxWidth(),
            minLines = if (field.isMultiline) 3 else 1,
            maxLines = if (field.isMultiline) 6 else 1,
            singleLine = !field.isMultiline,
            readOnly = field.isDate,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else field.keyboard),
            visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = when {
                isPassword -> { { IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (isPasswordVisible) "隐藏密码" else "显示密码")
                } } }
                field.isDate -> { { IconButton(onClick = pickDate) { Icon(Icons.Outlined.CalendarMonth, "选择${field.label}") } } }
                else -> null
            }
        )
        if (field.isDate) Box(Modifier.matchParentSize().clickable(onClickLabel = "选择${field.label}", onClick = pickDate))
    }
}

private fun contentFields(type: QrContentType): List<ContentField> = when (type) {
    QrContentType.TEXT -> listOf(ContentField("text", "想让别人看到什么？", "例如：周六下午三点，操场见！", isMultiline = true))
    QrContentType.URL -> listOf(ContentField("url", "网址", "粘贴完整链接，例如 https://www.qq.com", keyboard = KeyboardType.Uri))
    QrContentType.WIFI -> listOf(ContentField("ssid", "Wi-Fi 名称", "和手机设置里的网络名称一样"), ContentField("password", "Wi-Fi 密码", "填写连接这个网络的密码"))
    QrContentType.ENTERPRISE_WIFI -> listOf(ContentField("ssid", "网络名称"), ContentField("user", "网络账号"), ContentField("password", "网络密码"), ContentField("phase", "第二阶段认证", "例如 MSCHAPV2", isExtra = true))
    QrContentType.VCARD -> listOf(ContentField("name", "姓名"), ContentField("phone", "电话（可选）", keyboard = KeyboardType.Phone), ContentField("email", "邮箱（可选）", isExtra = true, keyboard = KeyboardType.Email), ContentField("company", "单位（可选）", isExtra = true), ContentField("job", "职位（可选）", isExtra = true), ContentField("address", "地址（可选）", isExtra = true), ContentField("website", "网址（可选）", isExtra = true, keyboard = KeyboardType.Uri), ContentField("note", "备注（可选）", isExtra = true, isMultiline = true))
    QrContentType.BIZCARD -> listOf(ContentField("lastName", "姓"), ContentField("firstName", "名"), ContentField("phone", "电话（可选）", keyboard = KeyboardType.Phone), ContentField("company", "单位（可选）", isExtra = true), ContentField("job", "职位（可选）", isExtra = true), ContentField("email", "邮箱（可选）", isExtra = true, keyboard = KeyboardType.Email), ContentField("address", "地址（可选）", isExtra = true))
    QrContentType.MECARD -> listOf(ContentField("name", "姓名"), ContentField("phone", "电话（可选）", keyboard = KeyboardType.Phone), ContentField("email", "邮箱（可选）", isExtra = true, keyboard = KeyboardType.Email), ContentField("address", "地址（可选）", isExtra = true))
    QrContentType.PHONE -> listOf(ContentField("phone", "电话号码", "例如 13800138000", keyboard = KeyboardType.Phone))
    QrContentType.SMS -> listOf(ContentField("phone", "收信人电话", keyboard = KeyboardType.Phone), ContentField("message", "短信内容（可选）", "扫码后打开短信，不会自动发送", isMultiline = true))
    QrContentType.EMAIL -> listOf(ContentField("email", "收件人邮箱", "例如 name@qq.com", keyboard = KeyboardType.Email), ContentField("subject", "邮件主题（可选）"), ContentField("body", "邮件内容（可选）", isMultiline = true), ContentField("copyTo", "抄送邮箱（可选）", isExtra = true, keyboard = KeyboardType.Email))
    QrContentType.GEO -> listOf(ContentField("latitude", "纬度", "例如 39.9042", keyboard = KeyboardType.Text), ContentField("longitude", "经度", "例如 116.4074", keyboard = KeyboardType.Text))
    QrContentType.BOOKMARK -> listOf(ContentField("url", "网页地址", "例如 https://www.qq.com", keyboard = KeyboardType.Uri), ContentField("title", "网页名称（可选）"))
    QrContentType.EVENT -> listOf(ContentField("summary", "日程名称", "例如 家长会"), ContentField("start", "开始时间", "点击选择日期和时间", isDate = true), ContentField("end", "结束时间", "点击选择日期和时间", isDate = true), ContentField("organizer", "组织者（可选）", isExtra = true), ContentField("uid", "日程编号（可选）", isExtra = true), ContentField("stamp", "记录时间（可选）", isExtra = true, isDate = true))
    QrContentType.YOUTUBE -> listOf(ContentField("video", "视频链接或编号", "粘贴 YouTube 视频链接", keyboard = KeyboardType.Uri))
    QrContentType.GOOGLE_PLAY -> listOf(ContentField("app", "应用链接或包名", "例如 com.example.app", keyboard = KeyboardType.Uri))
}
