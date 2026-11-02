package com.example.qrgenerator.domain

import com.example.qrgenerator.data.model.QrContentType
import org.junit.Assert.*
import org.junit.Test

class StructuredContentValidatorTest {
    private val validator: ContentValidator = ContentValidator()

    @Test
    fun `未填密码的加密网络不能生成`() {
        assertInvalid(QrContentType.WIFI, mapOf("ssid" to "家里", "auth" to "WPA"))
        assertValid(QrContentType.WIFI, mapOf("ssid" to "家里", "auth" to "OPEN"))
    }

    @Test
    fun `联系人可只填写姓名但电话填写后必须有效`() {
        assertValid(QrContentType.VCARD, mapOf("name" to "小明"))
        assertInvalid(QrContentType.VCARD, mapOf("name" to "小明", "phone" to "abc"))
    }

    @Test
    fun `结束时间必须晚于开始时间`() {
        assertInvalid(QrContentType.EVENT, mapOf("summary" to "活动", "start" to "2026-10-29 15:00", "end" to "2026-10-29 14:00"))
        assertValid(QrContentType.EVENT, mapOf("summary" to "活动", "start" to "2026-10-29 15:00", "end" to "2026-10-29 16:00"))
    }

    @Test
    fun `不接受无限或非数坐标`() {
        assertInvalid(QrContentType.GEO, mapOf("latitude" to "NaN", "longitude" to "116.4"))
        assertInvalid(QrContentType.GEO, mapOf("latitude" to "39.9", "longitude" to "Infinity"))
        assertValid(QrContentType.GEO, mapOf("latitude" to "-39.9", "longitude" to "-116.4"))
    }

    @Test
    fun `网址必须有可用主机`() {
        assertInvalid(QrContentType.URL, mapOf("url" to "https://"))
        assertValid(QrContentType.URL, mapOf("url" to "https://www.qq.com/path?q=你好"))
    }

    private fun assertValid(type: QrContentType, fields: Map<String, String>) {
        assertEquals(ValidationResult.Valid, validator.validate(type, QrContentCodec.serialize(fields)))
    }

    private fun assertInvalid(type: QrContentType, fields: Map<String, String>) {
        assertTrue(validator.validate(type, QrContentCodec.serialize(fields)) is ValidationResult.Invalid)
    }
}
