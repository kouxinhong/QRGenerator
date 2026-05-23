package com.example.qrgenerator.domain

import com.example.qrgenerator.data.model.QrContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContentValidatorTest {

    private lateinit var validator: ContentValidator

    @Before
    fun setup() {
        validator = ContentValidator()
    }

    // --- 通用 ---

    @Test
    fun `空白内容应返回无效`() {
        val result = validator.validate(QrContentType.TEXT, "")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `纯空格内容应返回无效`() {
        val result = validator.validate(QrContentType.TEXT, "   ")
        assertTrue(result is ValidationResult.Invalid)
    }

    // --- TEXT ---

    @Test
    fun `普通文本应有效`() {
        val result = validator.validate(QrContentType.TEXT, "Hello World")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `超长文本应返回无效`() {
        val longText = "a".repeat(4297)
        val result = validator.validate(QrContentType.TEXT, longText)
        assertTrue(result is ValidationResult.Invalid)
    }

    // --- URL ---

    @Test
    fun `有效 http URL 应通过`() {
        val result = validator.validate(QrContentType.URL, "http://example.com")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `有效 https URL 应通过`() {
        val result = validator.validate(QrContentType.URL, "https://example.com/path?q=1")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `无效 URL 应返回无效`() {
        val result = validator.validate(QrContentType.URL, "not-a-url")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `缺少协议的 URL 应返回无效`() {
        val result = validator.validate(QrContentType.URL, "example.com")
        assertTrue(result is ValidationResult.Invalid)
    }

    // --- WIFI ---

    @Test
    fun `有效 WPA WiFi 应通过`() {
        val result = validator.validate(QrContentType.WIFI, "MySSID/password123/WPA")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `有效 OPEN WiFi 应通过`() {
        val result = validator.validate(QrContentType.WIFI, "OpenNetwork//OPEN")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `WiFi 缺少分隔符应返回无效`() {
        val result = validator.validate(QrContentType.WIFI, "just-a-name")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `WiFi 空 SSID 应返回无效`() {
        val result = validator.validate(QrContentType.WIFI, "/password/WPA")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `WiFi 无效认证方式应返回无效`() {
        val result = validator.validate(QrContentType.WIFI, "SSID/pass/INVALID")
        assertTrue(result is ValidationResult.Invalid)
    }

    // --- PHONE ---

    @Test
    fun `有效电话号码应通过`() {
        val result = validator.validate(QrContentType.PHONE, "13800138000")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `带横线的电话号码应通过`() {
        val result = validator.validate(QrContentType.PHONE, "138-0013-8000")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `无效电话号码应返回无效`() {
        val result = validator.validate(QrContentType.PHONE, "abc")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `过短电话号码应返回无效`() {
        val result = validator.validate(QrContentType.PHONE, "123")
        assertTrue(result is ValidationResult.Invalid)
    }

    // --- EMAIL ---

    @Test
    fun `有效邮箱应通过`() {
        val result = validator.validate(QrContentType.EMAIL, "test@example.com")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `无效邮箱应返回无效`() {
        val result = validator.validate(QrContentType.EMAIL, "not-an-email")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `缺少域名的邮箱应返回无效`() {
        val result = validator.validate(QrContentType.EMAIL, "test@")
        assertTrue(result is ValidationResult.Invalid)
    }

    // --- GEO ---

    @Test
    fun `有效坐标应通过`() {
        val result = validator.validate(QrContentType.GEO, "39.9042,116.4074")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `缺少经度应返回无效`() {
        val result = validator.validate(QrContentType.GEO, "39.9042")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `非数字坐标应返回无效`() {
        val result = validator.validate(QrContentType.GEO, "abc,def")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `纬度超范围应返回无效`() {
        val result = validator.validate(QrContentType.GEO, "91.0,116.0")
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `经度超范围应返回无效`() {
        val result = validator.validate(QrContentType.GEO, "39.0,181.0")
        assertTrue(result is ValidationResult.Invalid)
    }

    // --- VCARD ---

    @Test
    fun `有效 vCard 应通过`() {
        val result = validator.validate(QrContentType.VCARD, "张三/某公司/工程师/13800138000/test@example.com")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `vCard 空姓名应返回无效`() {
        val result = validator.validate(QrContentType.VCARD, "/company/title")
        assertTrue(result is ValidationResult.Invalid)
    }

    // --- BIZCARD ---

    @Test
    fun `有效 BizCard 应通过`() {
        val result = validator.validate(QrContentType.BIZCARD, "三/张/工程师/某公司/13800138000/test@example.com")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `BizCard 缺少姓名应返回无效`() {
        val result = validator.validate(QrContentType.BIZCARD, "三")
        assertTrue(result is ValidationResult.Invalid)
    }

    // --- MECARD ---

    @Test
    fun `有效 MeCard 应通过`() {
        val result = validator.validate(QrContentType.MECARD, "张三/13800138000/test@example.com")
        assertEquals(ValidationResult.Valid, result)
    }

    // --- SMS ---

    @Test
    fun `有效 SMS 应通过`() {
        val result = validator.validate(QrContentType.SMS, "13800138000/Hello")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `SMS 无效号码应返回无效`() {
        val result = validator.validate(QrContentType.SMS, "abc/Hello")
        assertTrue(result is ValidationResult.Invalid)
    }

    // --- YOUTUBE / GOOGLE_PLAY ---

    @Test
    fun `YouTube URL 应通过 URL 验证`() {
        val result = validator.validate(QrContentType.YOUTUBE, "https://youtube.com/watch?v=abc")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `Google Play URL 应通过 URL 验证`() {
        val result = validator.validate(QrContentType.GOOGLE_PLAY, "https://play.google.com/store/apps/details?id=com.example")
        assertEquals(ValidationResult.Valid, result)
    }
}
