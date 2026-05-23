package com.example.qrgenerator.domain

import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.github.alexzhirkevich.customqrgenerator.QrData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QrGeneratorUseCaseTest {

    private lateinit var useCase: QrGeneratorUseCase

    @Before
    fun setup() {
        useCase = QrGeneratorUseCase()
    }

    // --- mapQrData 测试 ---

    @Test
    fun `TEXT 类型应映射为 QrData_Text`() {
        val config = QrConfig(contentType = QrContentType.TEXT, content = "Hello")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.Text)
    }

    @Test
    fun `URL 类型应映射为 QrData_Url`() {
        val config = QrConfig(contentType = QrContentType.URL, content = "https://example.com")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.Url)
    }

    @Test
    fun `PHONE 类型应映射为 QrData_Phone`() {
        val config = QrConfig(contentType = QrContentType.PHONE, content = "13800138000")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.Phone)
    }

    @Test
    fun `EMAIL 类型应映射为 QrData_Email`() {
        val config = QrConfig(contentType = QrContentType.EMAIL, content = "test@example.com")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.Email)
    }

    @Test
    fun `SMS 类型应映射为 QrData_SMS`() {
        val config = QrConfig(contentType = QrContentType.SMS, content = "13800138000")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.SMS)
    }

    @Test
    fun `YOUTUBE 类型应映射为 QrData_YouTube`() {
        val config = QrConfig(contentType = QrContentType.YOUTUBE, content = "dQw4w9WgXcQ")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.YouTube)
    }

    @Test
    fun `GOOGLE_PLAY 类型应映射为 QrData_GooglePlay`() {
        val config = QrConfig(contentType = QrContentType.GOOGLE_PLAY, content = "com.example.app")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.GooglePlay)
    }

    @Test
    fun `BOOKMARK 类型应映射为 QrData_Bookmark`() {
        val config = QrConfig(contentType = QrContentType.BOOKMARK, content = "https://example.com")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.Bookmark)
    }

    @Test
    fun `EVENT 类型应映射为 QrData_Event`() {
        val config = QrConfig(contentType = QrContentType.EVENT, content = "Meeting")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.Event)
    }

    // --- WiFi 解析测试 ---

    @Test
    fun `WiFi 完整格式应正确解析`() {
        val config = QrConfig(contentType = QrContentType.WIFI, content = "MySSID/password123/WPA")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.Wifi)
        val wifi = data as QrData.Wifi
        assertEquals("MySSID", wifi.ssid)
        assertEquals("password123", wifi.psk)
    }

    @Test
    fun `WiFi WEP 认证应正确解析`() {
        val config = QrConfig(contentType = QrContentType.WIFI, content = "SSID/pass/WEP")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.Wifi)
    }

    @Test
    fun `WiFi OPEN 认证应正确解析`() {
        val config = QrConfig(contentType = QrContentType.WIFI, content = "SSID//OPEN")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.Wifi)
    }

    // --- Geo 解析测试 ---

    @Test
    fun `Geo 坐标应正确解析`() {
        val config = QrConfig(contentType = QrContentType.GEO, content = "39.9042,116.4074")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.GeoPos)
        val geo = data as QrData.GeoPos
        assertEquals(39.9042f, geo.lat, 0.001f)
        assertEquals(116.4074f, geo.lon, 0.001f)
    }

    @Test
    fun `Geo 无效坐标应默认为 0`() {
        val config = QrConfig(contentType = QrContentType.GEO, content = "abc,def")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.GeoPos)
        val geo = data as QrData.GeoPos
        assertEquals(0f, geo.lat, 0.001f)
        assertEquals(0f, geo.lon, 0.001f)
    }

    // --- VCard 解析测试 ---

    @Test
    fun `VCard 应正确解析所有字段`() {
        val config = QrConfig(contentType = QrContentType.VCARD, content = "张三/某公司/工程师/13800138000/test@example.com")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.VCard)
        val vcard = data as QrData.VCard
        assertEquals("张三", vcard.name)
        assertEquals("某公司", vcard.company)
        assertEquals("工程师", vcard.title)
        assertEquals("13800138000", vcard.phoneNumber)
        assertEquals("test@example.com", vcard.email)
    }

    // --- execute 集成测试 ---

    @Test
    fun `execute 应返回非 null Drawable`() {
        val config = QrConfig(contentType = QrContentType.TEXT, content = "Hello World")
        val drawable = useCase.execute(config)
        assertNotNull(drawable)
    }

    @Test
    fun `execute URL 应返回非 null Drawable`() {
        val config = QrConfig(contentType = QrContentType.URL, content = "https://example.com")
        val drawable = useCase.execute(config)
        assertNotNull(drawable)
    }
}
