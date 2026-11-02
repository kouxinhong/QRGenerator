package com.example.qrgenerator.domain

import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.client.result.ParsedResult
import com.google.zxing.client.result.ResultParser
import com.google.zxing.client.result.SMSParsedResult
import com.google.zxing.client.result.WifiParsedResult
import com.google.zxing.client.result.URIParsedResult
import org.junit.Assert.*
import org.junit.Test

/** 用扫码器的真实结果解析器验证接收方理解的字段，不仅断言生成字符串。 */
class QrPayloadSemanticsTest {
    @Test
    fun `扫码短信后号码和中文正文正确分开`() {
        val inputFields = mapOf("phone" to "+8613800138000", "message" to "周六见：记得带书/水杯\n下午三点")
        val actual = parse(QrContentType.SMS, inputFields) as SMSParsedResult
        assertArrayEquals(arrayOf("+8613800138000"), actual.numbers)
        assertEquals(inputFields["message"], actual.body)
    }

    @Test
    fun `扫码彩信后号码和正文正确分开`() {
        val inputFields = mapOf("phone" to "13800138000", "message" to "活动照片：第一组", "mms" to "true")
        val actual = parse(QrContentType.SMS, inputFields) as SMSParsedResult
        assertArrayEquals(arrayOf("13800138000"), actual.numbers)
        assertEquals("活动照片：第一组", actual.body)
    }

    @Test
    fun `旧短信记录也能解析为独立号码和正文`() {
        val payload = QrContentCodec.encode(QrConfig(contentType = QrContentType.SMS, content = "13800138000/你好")).encode()
        val actual = parsePayload(payload) as SMSParsedResult
        assertArrayEquals(arrayOf("13800138000"), actual.numbers)
        assertEquals("你好", actual.body)
    }

    @Test
    fun `扫码企业网络识别加密类型账号和认证方法`() {
        val inputFields = mapOf("ssid" to "Office", "password" to "secret", "user" to "alice", "eap" to "PEAP", "phase" to "MSCHAPV2", "hidden" to "true")
        val actual = parse(QrContentType.ENTERPRISE_WIFI, inputFields) as WifiParsedResult
        assertEquals("WPA2-EAP", actual.networkEncryption)
        assertEquals("Office", actual.ssid)
        assertEquals("secret", actual.password)
        assertEquals("alice", actual.identity)
        assertEquals("PEAP", actual.eapMethod)
        assertEquals("MSCHAPV2", actual.phase2Method)
        assertTrue(actual.isHidden)
    }

    @Test
    fun `企业网络特殊字符不会截断密码或注入协议字段`() {
        val inputFields = mapOf("ssid" to "教室;S:另一网络", "password" to "a\\b;c:d,e\"f", "user" to "姓名;I:另一个人", "eap" to "TTLS", "phase" to "PAP")
        val actual = parse(QrContentType.ENTERPRISE_WIFI, inputFields) as WifiParsedResult
        assertEquals(inputFields["ssid"], actual.ssid)
        assertEquals(inputFields["password"], actual.password)
        assertEquals(inputFields["user"], actual.identity)
        assertEquals("WPA2-EAP", actual.networkEncryption)
        assertEquals("TTLS", actual.eapMethod)
        assertEquals("PAP", actual.phase2Method)
        assertFalse(actual.isHidden)
    }

    @Test
    fun `网页书签保留带分号的网址`() {
        val inputUrl = "https://example.com/a;jsessionid=abc?x=1&y=2"
        val actual = parse(QrContentType.BOOKMARK, mapOf("url" to inputUrl, "title" to "学校网站")) as URIParsedResult
        assertEquals(inputUrl, actual.uri)
        assertEquals("学校网站", actual.title)
    }

    private fun parse(type: QrContentType, fields: Map<String, String>): ParsedResult {
        val config = QrConfig(contentType = type, content = QrContentCodec.serialize(fields))
        return parsePayload(QrContentCodec.encode(config).encode())
    }

    private fun parsePayload(payload: String): ParsedResult = ResultParser.parseResult(Result(payload, null, null, BarcodeFormat.QR_CODE))
}
