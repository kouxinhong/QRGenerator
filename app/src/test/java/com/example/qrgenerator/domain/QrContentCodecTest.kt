package com.example.qrgenerator.domain

import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.github.alexzhirkevich.customqrgenerator.QrData
import org.junit.Assert.*
import org.junit.Test

class QrContentCodecTest {
    @Test
    fun `表单不把斜线密码拆开`() {
        val content = QrContentCodec.serialize(mapOf("ssid" to "家里的/WiFi", "password" to "a/b;c\\d", "auth" to "WPA", "hidden" to "true"))
        val actual = QrContentCodec.encode(QrConfig(contentType = QrContentType.WIFI, content = content)) as QrData.Wifi
        assertEquals("家里的/WiFi", actual.ssid)
        assertEquals("a/b;c\\d", actual.psk)
        assertTrue(actual.hidden)
        assertEquals("Wi-Fi：家里的/WiFi", QrContentCodec.preview(QrContentType.WIFI, content))
        assertFalse(QrContentCodec.preview(QrContentType.WIFI, content).contains("a/b"))
    }

    @Test
    fun `短信正文完整进入二维码`() {
        val content = QrContentCodec.serialize(mapOf("phone" to "13800138000", "message" to "周六见/记得带书", "mms" to "false"))
        assertEquals("SMSTO:13800138000:周六见/记得带书", encode(QrContentType.SMS, content))
    }

    @Test
    fun `邮箱编码包含主题正文和抄送`() {
        val content = QrContentCodec.serialize(mapOf("email" to "a@qq.com", "copyTo" to "b@qq.com", "subject" to "课程", "body" to "第一节/第二节"))
        val actual = QrContentCodec.encode(QrConfig(contentType = QrContentType.EMAIL, content = content)) as QrData.Email
        assertEquals("课程", actual.subject)
        assertEquals("第一节/第二节", actual.body)
        assertEquals("b@qq.com", actual.copyTo)
    }

    @Test
    fun `联系人职位包含正确分隔并转义换行`() {
        val content = QrContentCodec.serialize(mapOf("name" to "小明", "job" to "老师", "note" to "一班\n二班", "address" to "学校"))
        val actual = encode(QrContentType.VCARD, content)
        assertTrue(actual.contains("FN:小明\n"))
        assertTrue(actual.contains("TITLE:老师\n"))
        assertTrue(actual.contains("NOTE:一班\\n二班\n"))
        assertTrue(actual.contains("ADR:;;学校;;;;\n"))
    }

    @Test
    fun `日程选择的时间转换为日历格式`() {
        val content = QrContentCodec.serialize(mapOf("summary" to "家长会", "start" to "2026-10-29 15:00", "end" to "2026-10-29 16:00"))
        val actual = encode(QrContentType.EVENT, content)
        assertTrue(actual.contains("DTSTART:20261029T150000"))
        assertTrue(actual.contains("DTEND:20261029T160000"))
    }

    @Test
    fun `应用和视频链接不被套入错误协议`() {
        assertEquals("https://play.google.com/store/apps/details?id=com.example.app", encode(QrContentType.GOOGLE_PLAY, "https://play.google.com/store/apps/details?id=com.example.app&hl=zh"))
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", encode(QrContentType.YOUTUBE, "https://youtu.be/dQw4w9WgXcQ?t=1"))
    }

    @Test
    fun `普通JSON文字不会误判为表单`() {
        val actual = encode(QrContentType.TEXT, "{\"name\":\"小明\"}")
        assertEquals("{\"name\":\"小明\"}", actual)
    }

    @Test
    fun `普通文字即使包含内部标记也保持原样`() {
        val input = "{\"_qrForm\":\"1\",\"text\":\"你好\"}"
        assertEquals(input, encode(QrContentType.TEXT, input))
    }

    @Test
    fun `旧版联系人和短信依然能打开`() {
        assertEquals("小明", QrContentCodec.fields(QrContentType.VCARD, "小明/学校/老师/13800138000/a@qq.com")["name"])
        assertEquals("SMSTO:13800138000:你好", encode(QrContentType.SMS, "13800138000/你好"))
    }

    @Test
    fun `无密码网络不写出历史密码`() {
        val content = QrContentCodec.serialize(mapOf("ssid" to "公共网络", "password" to "旧密码", "auth" to "OPEN"))
        assertFalse(encode(QrContentType.WIFI, content).contains("旧密码"))
    }

    @Test
    fun `先选无密码或隐藏网络时保留草稿选项`() {
        val open = QrContentCodec.serialize(mapOf("ssid" to "", "auth" to "OPEN"))
        assertEquals("OPEN", QrContentCodec.fields(QrContentType.WIFI, open)["auth"])
        val hidden = QrContentCodec.serialize(mapOf("hidden" to "true"))
        assertEquals("true", QrContentCodec.fields(QrContentType.WIFI, hidden)["hidden"])
        assertEquals(ValidationResult.Invalid("请填写网络名称"), ContentValidator().validate(QrContentType.WIFI, open))
    }

    private fun encode(type: QrContentType, content: String): String = QrContentCodec.encode(QrConfig(contentType = type, content = content)).encode()
}
