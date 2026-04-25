package com.example.qrgenerator.data

import android.graphics.Color
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.data.model.QrErrorLevel
import com.example.qrgenerator.data.model.QrColorType
import com.example.qrgenerator.data.model.QrPixelShapeType
import com.example.qrgenerator.data.model.QrBallShapeType
import com.example.qrgenerator.data.model.QrFrameShapeType
import org.junit.Assert.assertEquals
import org.junit.Test

class QrConfigTest {

    @Test
    fun `默认配置应为文本类型`() {
        val config = QrConfig()
        assertEquals(QrContentType.TEXT, config.contentType)
        assertEquals("", config.content)
    }

    @Test
    fun `默认容错级别为 AUTO`() {
        val config = QrConfig()
        assertEquals(QrErrorLevel.AUTO, config.errorCorrectionLevel)
    }

    @Test
    fun `默认颜色为纯黑色`() {
        val config = QrConfig()
        assertEquals(QrColorType.SOLID, config.darkColor.type)
        assertEquals(Color.BLACK, config.darkColor.solidColor)
    }

    @Test
    fun `默认形状为方形`() {
        val config = QrConfig()
        assertEquals(QrPixelShapeType.DEFAULT, config.darkPixelShape)
        assertEquals(QrBallShapeType.DEFAULT, config.ballShape)
        assertEquals(QrFrameShapeType.DEFAULT, config.frameShape)
    }

    @Test
    fun `支持所有 15 种内容类型`() {
        assertEquals(15, QrContentType.entries.size)
    }
}
