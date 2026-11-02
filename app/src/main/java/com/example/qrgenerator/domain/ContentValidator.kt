package com.example.qrgenerator.domain

import com.example.qrgenerator.data.model.QrContentType
import java.net.URI
import java.time.LocalDateTime

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val message: String) : ValidationResult
}

/** 在表单下方提示可执行的修改方法，不在用户打字时弹窗。 */
class ContentValidator {
    fun validate(type: QrContentType, content: String): ValidationResult {
        if (content.isBlank()) return invalid("请先填写内容")
        val f: Map<String, String> = QrContentCodec.fields(type, content)
        val main: String = f[QrContentCodec.primaryKey(type)].orEmpty()
        if (main.isBlank()) return invalid("请填写${requiredLabel(type)}")
        if (content.length > 12000 || (type == QrContentType.TEXT && main.length > 4296)) return invalid("内容太长，请删减后再试")
        return when (type) {
            QrContentType.URL, QrContentType.BOOKMARK -> if (isUrl(main)) valid() else invalid("请输入完整网址，例如 https://www.qq.com")
            QrContentType.PHONE, QrContentType.SMS -> if (isPhone(main)) valid() else invalid("请填写正确的电话号码，例如 13800138000")
            QrContentType.EMAIL -> validateEmail(f)
            QrContentType.WIFI -> validateWifi(f)
            QrContentType.ENTERPRISE_WIFI -> if (f["user"].isNullOrBlank()) invalid("请填写网络账号") else valid()
            QrContentType.GEO -> validateGeo(f)
            QrContentType.VCARD, QrContentType.MECARD, QrContentType.BIZCARD -> validateCard(type, f)
            QrContentType.EVENT -> validateEvent(f, QrContentCodec.isStructured(content))
            QrContentType.YOUTUBE -> if (Regex("[A-Za-z0-9_-]{3,}").matches(QrContentCodec.extractVideoId(main))) valid() else invalid("请粘贴 YouTube 视频链接或视频编号")
            QrContentType.GOOGLE_PLAY -> if (Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+").matches(QrContentCodec.extractPackage(main))) valid() else invalid("请粘贴 Google Play 应用链接或包名")
            else -> valid()
        }
    }

    private fun validateWifi(f: Map<String, String>): ValidationResult {
        val auth: String = f["auth"].orEmpty().ifBlank { "WPA" }.uppercase()
        if (auth !in setOf("WPA", "WEP", "OPEN")) return invalid("请选择网络加密方式")
        if (auth != "OPEN" && f["password"].isNullOrEmpty()) return invalid("请填写 Wi-Fi 密码，无密码网络请选“不需要密码”")
        return valid()
    }

    private fun validateEmail(f: Map<String, String>): ValidationResult {
        if (!isEmail(f["email"].orEmpty())) return invalid("请填写正确的邮箱，例如 name@qq.com")
        if (!f["copyTo"].isNullOrBlank() && !isEmail(f["copyTo"].orEmpty())) return invalid("请检查抄送邮箱")
        return valid()
    }

    private fun validateCard(type: QrContentType, f: Map<String, String>): ValidationResult {
        if (type == QrContentType.BIZCARD && f["lastName"].isNullOrBlank()) return invalid("请填写姓氏")
        if (!f["phone"].isNullOrBlank() && !isPhone(f["phone"].orEmpty())) return invalid("请检查联系人电话")
        if (!f["email"].isNullOrBlank() && !isEmail(f["email"].orEmpty())) return invalid("请检查联系人邮箱")
        if (!f["website"].isNullOrBlank() && !isUrl(f["website"].orEmpty())) return invalid("请填写完整的联系人网址")
        return valid()
    }

    private fun validateGeo(f: Map<String, String>): ValidationResult {
        val lat: Double? = f["latitude"]?.toDoubleOrNull()
        val lon: Double? = f["longitude"]?.toDoubleOrNull()
        if (lat == null || !lat.isFinite() || lat !in -90.0..90.0) return invalid("纬度请填写 -90 到 90 的数字")
        if (lon == null || !lon.isFinite() || lon !in -180.0..180.0) return invalid("经度请填写 -180 到 180 的数字")
        return valid()
    }

    private fun validateEvent(f: Map<String, String>, isStructured: Boolean): ValidationResult {
        if (!isStructured) return valid()
        val start: LocalDateTime = parseTime(f["start"]) ?: return invalid("请选择开始时间")
        val end: LocalDateTime = parseTime(f["end"]) ?: return invalid("请选择结束时间")
        if (!end.isAfter(start)) return invalid("结束时间要晚于开始时间")
        if (!f["stamp"].isNullOrBlank() && parseTime(f["stamp"]) == null) return invalid("请选择记录时间")
        return valid()
    }

    private fun parseTime(value: String?): LocalDateTime? = runCatching { LocalDateTime.parse(value, QrContentCodec.dateFormat) }.getOrNull()
    private fun isPhone(value: String): Boolean = Regex("\\+?[0-9][0-9 ()-]{4,20}").matches(value.trim()) && value.count { it.isDigit() } in 5..15
    private fun isEmail(value: String): Boolean = Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(value.trim())
    private fun isUrl(value: String): Boolean = runCatching { URI(value.trim()).let { it.scheme in setOf("http", "https") && !it.host.isNullOrBlank() } }.getOrDefault(false)
    private fun valid(): ValidationResult = ValidationResult.Valid
    private fun invalid(message: String): ValidationResult = ValidationResult.Invalid(message)
    private fun requiredLabel(type: QrContentType): String = when (type) {
        QrContentType.WIFI, QrContentType.ENTERPRISE_WIFI -> "网络名称"
        QrContentType.VCARD, QrContentType.MECARD, QrContentType.BIZCARD -> "姓名"
        QrContentType.PHONE, QrContentType.SMS -> "电话号码"
        QrContentType.EMAIL -> "收件人邮箱"
        QrContentType.EVENT -> "日程名称"
        else -> "内容"
    }
}
