package com.example.qrgenerator.domain

import com.example.qrgenerator.data.model.QrContentType

/** 内容验证结果 */
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val message: String) : ValidationResult
}

/** 内容输入验证器 */
class ContentValidator {

    fun validate(type: QrContentType, content: String): ValidationResult {
        if (content.isBlank()) {
            return ValidationResult.Invalid("内容不能为空")
        }
        return when (type) {
            QrContentType.TEXT -> validateText(content)
            QrContentType.URL -> validateUrl(content)
            QrContentType.WIFI -> validateWifi(content)
            QrContentType.ENTERPRISE_WIFI -> validateEnterpriseWifi(content)
            QrContentType.VCARD -> validateVCard(content)
            QrContentType.BIZCARD -> validateBizCard(content)
            QrContentType.MECARD -> validateMeCard(content)
            QrContentType.PHONE -> validatePhone(content)
            QrContentType.SMS -> validateSms(content)
            QrContentType.EMAIL -> validateEmail(content)
            QrContentType.GEO -> validateGeo(content)
            QrContentType.BOOKMARK -> validateText(content)
            QrContentType.EVENT -> validateText(content)
            QrContentType.YOUTUBE -> validateUrl(content)
            QrContentType.GOOGLE_PLAY -> validateUrl(content)
        }
    }

    private fun validateText(content: String): ValidationResult {
        if (content.length > MAX_TEXT_LENGTH) {
            return ValidationResult.Invalid("文本过长，最多 $MAX_TEXT_LENGTH 个字符")
        }
        return ValidationResult.Valid
    }

    private fun validateUrl(content: String): ValidationResult {
        if (!URL_REGEX.matches(content.trim())) {
            return ValidationResult.Invalid("请输入有效的 URL（以 http:// 或 https:// 开头）")
        }
        return ValidationResult.Valid
    }

    private fun validateWifi(content: String): ValidationResult {
        val parts = content.split("/")
        if (parts.size < 2) {
            return ValidationResult.Invalid("格式：SSID/密码/认证方式（WPA/WEP/OPEN）")
        }
        val ssid = parts[0].trim()
        if (ssid.isEmpty()) {
            return ValidationResult.Invalid("SSID 不能为空")
        }
        val auth = parts.getOrNull(2)?.trim()?.uppercase()
        if (auth != null && auth !in VALID_WIFI_AUTH) {
            return ValidationResult.Invalid("认证方式必须是 WPA、WEP 或 OPEN")
        }
        return ValidationResult.Valid
    }

    private fun validateEnterpriseWifi(content: String): ValidationResult {
        val parts = content.split("/")
        if (parts.size < 2) {
            return ValidationResult.Invalid("格式：SSID/密码/用户名/EAP方法")
        }
        if (parts[0].trim().isEmpty()) {
            return ValidationResult.Invalid("SSID 不能为空")
        }
        return ValidationResult.Valid
    }

    private fun validateVCard(content: String): ValidationResult {
        val parts = content.split("/")
        if (parts.size < 1 || parts[0].trim().isEmpty()) {
            return ValidationResult.Invalid("至少需要提供姓名")
        }
        return ValidationResult.Valid
    }

    private fun validateBizCard(content: String): ValidationResult {
        val parts = content.split("/")
        if (parts.size < 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
            return ValidationResult.Invalid("至少需要提供姓和名")
        }
        return ValidationResult.Valid
    }

    private fun validateMeCard(content: String): ValidationResult {
        val parts = content.split("/")
        if (parts.size < 1 || parts[0].trim().isEmpty()) {
            return ValidationResult.Invalid("至少需要提供姓名")
        }
        return ValidationResult.Valid
    }

    private fun validatePhone(content: String): ValidationResult {
        val cleaned = content.trim().replace(Regex("[\\s\\-+]"), "")
        if (!PHONE_REGEX.matches(cleaned)) {
            return ValidationResult.Invalid("请输入有效的电话号码")
        }
        return ValidationResult.Valid
    }

    private fun validateSms(content: String): ValidationResult {
        val parts = content.split("/")
        if (parts.size < 1 || parts[0].trim().isEmpty()) {
            return ValidationResult.Invalid("至少需要提供手机号码")
        }
        val cleaned = parts[0].trim().replace(Regex("[\\s\\-+]"), "")
        if (!PHONE_REGEX.matches(cleaned)) {
            return ValidationResult.Invalid("请输入有效的手机号码")
        }
        return ValidationResult.Valid
    }

    private fun validateEmail(content: String): ValidationResult {
        if (!EMAIL_REGEX.matches(content.trim())) {
            return ValidationResult.Invalid("请输入有效的邮箱地址")
        }
        return ValidationResult.Valid
    }

    private fun validateGeo(content: String): ValidationResult {
        val parts = content.split(",")
        if (parts.size < 2) {
            return ValidationResult.Invalid("格式：纬度,经度（如 39.9042,116.4074）")
        }
        val lat = parts[0].trim().toFloatOrNull()
        val lon = parts[1].trim().toFloatOrNull()
        if (lat == null || lon == null) {
            return ValidationResult.Invalid("纬度和经度必须是数字")
        }
        if (lat !in -90f..90f) {
            return ValidationResult.Invalid("纬度范围：-90 到 90")
        }
        if (lon !in -180f..180f) {
            return ValidationResult.Invalid("经度范围：-180 到 180")
        }
        return ValidationResult.Valid
    }

    companion object {
        private const val MAX_TEXT_LENGTH = 4296
        private val URL_REGEX = Regex("^https?://\\S+$")
        private val PHONE_REGEX = Regex("^\\d{5,15}$")
        private val EMAIL_REGEX = Regex("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")
        private val VALID_WIFI_AUTH = setOf("WPA", "WEP", "OPEN")
    }
}
