package com.example.qrgenerator.domain

import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.data.model.QrPixelShapeType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class QrConfigHolderImplTest {

    private lateinit var holder: QrConfigHolderImpl

    @Before
    fun setup() {
        holder = QrConfigHolderImpl()
    }

    @Test
    fun `初始配置应为默认值`() {
        val config = holder.config.value
        assertEquals(QrConfig(), config)
    }

    @Test
    fun `executeSetConfig 应替换完整配置`() {
        val newConfig = QrConfig(contentType = QrContentType.URL, content = "https://example.com")
        holder.executeSetConfig(newConfig)
        assertEquals(newConfig, holder.config.value)
    }

    @Test
    fun `executeUpdateConfig 应只修改指定字段`() {
        holder.executeUpdateConfig { it.copy(darkPixelShape = QrPixelShapeType.CIRCLE) }
        val config = holder.config.value
        assertEquals(QrPixelShapeType.CIRCLE, config.darkPixelShape)
        assertEquals(QrContentType.TEXT, config.contentType)
    }

    @Test
    fun `多次更新应累积变更`() {
        holder.executeUpdateConfig { it.copy(content = "test") }
        holder.executeUpdateConfig { it.copy(contentType = QrContentType.EMAIL) }
        val config = holder.config.value
        assertEquals("test", config.content)
        assertEquals(QrContentType.EMAIL, config.contentType)
    }
}
