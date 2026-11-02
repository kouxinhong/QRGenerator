package com.example.qrgenerator.data

import android.graphics.Color
import com.example.qrgenerator.data.model.ImageScaleType
import com.example.qrgenerator.data.model.QrColorType
import com.example.qrgenerator.data.model.QrPixelShapeType
import com.example.qrgenerator.util.QrConfigSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrConfigCompatibilityTest {
    @Test
    fun `old saved code retains content and fills new fields with defaults`() {
        val oldJson: String = """{"contentType":"TEXT","content":"旧二维码","padding":0.125,"darkPixelShape":"CIRCLE","logo":{"imagePath":null,"size":0.2},"highlighting":{"alpha":0.75}}"""
        val actualConfig = QrConfigSerializer.deserialize(oldJson)
        assertEquals("旧二维码", actualConfig.content)
        assertEquals(.125f, actualConfig.padding, .0001f)
        assertEquals(QrPixelShapeType.CIRCLE, actualConfig.darkPixelShape)
        assertEquals(Color.WHITE, actualConfig.background.color.solidColor)
        assertEquals(QrColorType.SOLID, actualConfig.lightColor.type)
        assertEquals(Color.WHITE, actualConfig.lightColor.solidColor)
        assertEquals(ImageScaleType.CENTER_CROP, actualConfig.logo.scale)
        assertEquals(Color.WHITE, actualConfig.highlighting.color.solidColor)
        assertNull(actualConfig.background.imagePath)
    }
}
