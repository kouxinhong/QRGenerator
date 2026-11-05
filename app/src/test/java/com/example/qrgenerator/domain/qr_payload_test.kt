package com.example.qrgenerator.domain

import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.data.model.QrSourcePayload
import com.example.qrgenerator.util.QrConfigSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrPayloadTest {
    @Test
    fun `WiFi展示网络名并隐藏密码而表单完整保留转义内容`() {
        val input: String = "  WIFI:T:WPA;S:店铺\\;分店\\:二号;P: a\\;b\\:c\\\\d\\,e\\\"f ;H:true;;\n"
        val actual: QrPayload = QrPayloadParser.parse(input)
        assertEquals(input, actual.rawPayload)
        assertEquals(QrPayloadKind.WIFI, actual.kind)
        assertEquals("Wi-Fi：店铺;分店:二号", actual.summary)
        assertEquals(QrContentType.WIFI, actual.editableType)
        val fields: Map<String, String> = QrContentCodec.fields(QrContentType.WIFI, requireNotNull(actual.editableContent))
        assertEquals("店铺;分店:二号", fields["ssid"])
        assertEquals(" a;b:c\\d,e\"f ", fields["password"])
        assertEquals("true", fields["hidden"])
        assertTrue(actual.fields.single { it.label == "密码" }.isSensitive)
        assertFalse(actual.summary.contains("a;b"))
    }

    @Test
    fun `无密码及WEP网络可编辑`() {
        val open: QrPayload = QrPayloadParser.parse("WIFI:S:Guest;T:nopass;;")
        val wep: QrPayload = QrPayloadParser.parse("WIFI:S:Legacy;T:WEP;P:abcde;;")
        assertEquals("OPEN", QrContentCodec.fields(QrContentType.WIFI, requireNotNull(open.editableContent))["auth"])
        assertEquals("WEP", QrContentCodec.fields(QrContentType.WIFI, requireNotNull(wep.editableContent))["auth"])
    }

    @Test
    fun `损坏WiFi也不降为文字显示密码`() {
        listOf(
            "WIFI:S:Cafe;P:secret",
            "WIFI :S:Cafe;P:secret;;",
            "WIFI:S:Cafe P:secret;;",
            "WIFI:S:Cafe;P:secret;bad-token;;",
            "WIFI:S:Cafe;S:secret;P:password;;",
            "wifi:P:secret;;"
        ).forEach { input ->
            val actual: QrPayload = QrPayloadParser.parse(input)
            assertEquals(QrPayloadKind.WIFI, actual.kind)
            assertEquals("Wi-Fi 二维码", actual.summary)
            assertNull(actual.editableContent)
            assertTrue(actual.fields.isEmpty())
            assertFalse(QrContentCodec.preview(QrContentType.TEXT, input).contains("secret"))
        }
    }

    @Test
    fun `未知认证或额外WiFi字段保留原文但禁止有损编辑`() {
        listOf(
            "WIFI:T:WPA2-EAP;S:Office;P:secret;I:user;E:PEAP;;",
            "WIFI:T:WPA;S:Office;P:secret;X:unknown;;",
            "WIFI:T:WPA;S:Office;P:first;P:secret;;",
            "WIFI:T:WPA;S:Office;P:secret;H:unexpected;;"
        ).forEach { input ->
            val actual: QrPayload = QrPayloadParser.parse(input)
            assertEquals(input, actual.rawPayload)
            assertEquals(QrPayloadKind.WIFI, actual.kind)
            assertEquals("Wi-Fi：Office", actual.summary)
            assertNull(actual.editableType)
            assertNull(actual.editableContent)
            assertFalse(actual.summary.contains("secret"))
        }
    }

    @Test
    fun `旧版按TEXT保存的WiFi摘要不再泄露密码`() {
        val input: String = "WIFI:T:WPA;S:Cafe;P:secret123;;"
        assertEquals("Wi-Fi：Cafe", QrContentCodec.preview(QrContentType.TEXT, input))
        assertEquals(input, QrContentCodec.encode(QrConfig(content = input)).encode())
    }

    @Test
    fun `未编辑的扫码表单逐字生成原文并可序列化恢复`() {
        val input: String = " WIFI:S:Cafe;P: secret ;T:WPA;H:FALSE;;\n"
        val config: QrConfig = importEditable(input)
        assertEquals(input, QrContentCodec.encode(config).encode())
        val restored: QrConfig = QrConfigSerializer.deserialize(QrConfigSerializer.serialize(config))
        assertEquals(input, QrContentCodec.encode(restored).encode())
        assertEquals(config.sourcePayload, restored.sourcePayload)
    }

    @Test
    fun `修改WiFi密码后不再生成旧的扫码载荷`() {
        val config: QrConfig = importEditable("WIFI:S:Cafe;P:old-password;T:WPA;;")
        val fields: Map<String, String> = QrContentCodec.fields(config.contentType, config.content)
        val edited: QrConfig = config.copy(content = QrContentCodec.serialize(fields + ("password" to "new-password")))
        val actual: String = QrContentCodec.encode(edited).encode()
        assertTrue(actual.contains("new-password"))
        assertFalse(actual.contains("old-password"))
    }

    @Test
    fun `切换内容类型时不会误用来源载荷`() {
        val config: QrConfig = importEditable("WIFI:S:Cafe;P:old-password;T:WPA;;")
        val edited: QrConfig = config.copy(contentType = QrContentType.TEXT)
        assertEquals(config.content, QrContentCodec.encode(edited).encode())
    }

    @Test
    fun `网址只识别有效http或https并保留原文`() {
        val input: String = " HTTPS://example.com/menu?a=1&b=2 \n"
        val actual: QrPayload = QrPayloadParser.parse(input)
        assertEquals(QrPayloadKind.URL, actual.kind)
        assertEquals(QrContentType.URL, actual.editableType)
        assertEquals("https://example.com/menu?a=1&b=2", QrContentCodec.fields(QrContentType.URL, requireNotNull(actual.editableContent))["url"])
        assertEquals(input, QrContentCodec.encode(importEditable(input)).encode())
        listOf("javascript:alert(1)", "intent://app", "http:///missing-host", "www.example.com", "https://bad host").forEach {
            assertEquals(QrPayloadKind.TEXT, QrPayloadParser.parse(it).kind)
        }
    }

    @Test
    fun `普通文字不会因JSON表单标记或换行而改写`() {
        val input: String = "  {\"_qrForm\":\"1\",\"text\":\"你好\"}\n下一行  "
        val actual: QrPayload = QrPayloadParser.parse(input)
        assertEquals(QrPayloadKind.TEXT, actual.kind)
        assertEquals(input, actual.editableContent)
        assertEquals(input, actual.fields.single().value)
        assertEquals(input, QrContentCodec.encode(importEditable(input)).encode())
    }

    @Test
    fun `项目生成的联系人支持编辑并原样回传`() {
        val input: String = "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:小\\;王\r\nN:小\\;王;;;;\r\nTEL:13800138000\r\nEMAIL:test@example.com\r\nADR:;;北京市\\;门店;;;;\r\nNOTE:第一行\\n第二行\r\nEND:VCARD"
        val actual: QrPayload = QrPayloadParser.parse(input)
        assertEquals(QrPayloadKind.CONTACT, actual.kind)
        assertEquals("联系人：小;王", actual.summary)
        assertEquals(QrContentType.VCARD, actual.editableType)
        val fields: Map<String, String> = QrContentCodec.fields(QrContentType.VCARD, requireNotNull(actual.editableContent))
        assertEquals("北京市;门店", fields["address"])
        assertEquals("第一行\n第二行", fields["note"])
        assertEquals(input, QrContentCodec.encode(importEditable(input)).encode())
    }

    @Test
    fun `复杂联系人保留所有电话号码和未知字段但不做有损表单转换`() {
        val input: String = "BEGIN:VCARD\nVERSION:3.0\nFN:Alice\nN:Doe;Alice;;;\nTEL;TYPE=CELL:13800138000\nTEL;TYPE=WORK:13900139000\nX-CUSTOM:原始字段\nEND:VCARD"
        val actual: QrPayload = QrPayloadParser.parse(input)
        assertEquals(QrPayloadKind.CONTACT, actual.kind)
        assertEquals("联系人：Alice", actual.summary)
        assertEquals(listOf("13800138000", "13900139000"), actual.fields.filter { it.label == "电话" }.map { it.value })
        assertNull(actual.editableType)
        assertNull(actual.editableContent)
        assertEquals(input, actual.rawPayload)
    }

    @Test
    fun `重复联系人属性或分段地址不会因编辑丢失信息`() {
        listOf(
            "BEGIN:VCARD\nVERSION:3.0\nFN:Alice\nTEL:13800138000\nTEL:13900139000\nEND:VCARD",
            "BEGIN:VCARD\nVERSION:3.0\nFN:Alice\nADR:;;Street;City;Region;100000;Country\nEND:VCARD",
            "BEGIN:VCARD\nVERSION:3.0\nFN:Alice\nORG:Company;Department\nEND:VCARD",
            "BEGIN:VCARD\nVERSION:3.0\nFN:Alice\nN:Doe;Alice;;;\nEND:VCARD"
        ).forEach { input ->
            val actual: QrPayload = QrPayloadParser.parse(input)
            assertEquals(QrPayloadKind.CONTACT, actual.kind)
            assertNull(actual.editableContent)
        }
    }

    @Test
    fun `折行联系人可展示而损坏联系人不显示协议原文`() {
        val folded: QrPayload = QrPayloadParser.parse("BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Long\r\n Name\r\nEND:VCARD")
        assertEquals("联系人：LongName", folded.summary)
        assertNotNull(folded.editableContent)
        val broken: QrPayload = QrPayloadParser.parse("BEGIN:VCARD\nFN:Somebody\nTEL:secret")
        assertEquals("联系人二维码", broken.summary)
        assertTrue(broken.fields.isEmpty())
    }

    @Test
    fun `MECARD简单字段可编辑但重复电话和结构化姓名保留原样`() {
        val input: String = "MECARD:N:小王;TEL:13800138000;EMAIL:test@example.com;ADR:北京市;NOTE:欢迎\\;联系;;"
        val actual: QrPayload = QrPayloadParser.parse(input)
        assertEquals(QrContentType.VCARD, actual.editableType)
        assertEquals("联系人：小王", actual.summary)
        assertEquals("欢迎;联系", QrContentCodec.fields(QrContentType.VCARD, requireNotNull(actual.editableContent))["note"])
        assertEquals(input, QrContentCodec.encode(importEditable(input)).encode())
        assertNull(QrPayloadParser.parse("MECARD:N:Doe,John;TEL:13800138000;;").editableContent)
        assertNull(QrPayloadParser.parse("MECARD:N:小王;TEL:13800138000;TEL:13900139000;;").editableContent)
    }

    @Test
    fun `没有sourcePayload的旧记录仍按原有方式生成`() {
        val actual: QrConfig = QrConfigSerializer.deserialize("""{"contentType":"TEXT","content":"旧二维码"}""")
        assertNull(actual.sourcePayload)
        assertEquals("旧二维码", QrContentCodec.encode(actual).encode())
    }

    @Test
    fun `带BOM的WiFi载荷仍生成安全摘要并原样保留`() {
        val input: String = "\uFEFFWIFI:T:WPA;S:Cafe;P:secret123;;"
        val actual: QrPayload = QrPayloadParser.parse(input)
        assertEquals(QrPayloadKind.WIFI, actual.kind)
        assertEquals("Wi-Fi：Cafe", actual.summary)
        assertEquals(input, QrContentCodec.encode(importEditable(input)).encode())
    }

    private fun importEditable(rawPayload: String): QrConfig {
        val payload: QrPayload = QrPayloadParser.parse(rawPayload)
        val type: QrContentType = requireNotNull(payload.editableType)
        val content: String = requireNotNull(payload.editableContent)
        return QrConfig(contentType = type, content = content, sourcePayload = QrSourcePayload(rawPayload, type, content))
    }
}
