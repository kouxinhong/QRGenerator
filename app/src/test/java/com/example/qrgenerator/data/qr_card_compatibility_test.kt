package com.example.qrgenerator.data

import com.example.qrgenerator.data.model.QrCardConfig
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.data.model.QrScene
import com.example.qrgenerator.util.QrConfigSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class QrCardCompatibilityTest {
    @Test
    fun `old history without a card stays a plain code`() {
        val inputJson: String = """{"contentType":"TEXT","content":"旧记录"}"""
        val actualConfig: QrConfig = QrConfigSerializer.deserialize(inputJson)
        assertEquals("旧记录", actualConfig.content)
        assertEquals(QrCardConfig(), actualConfig.card)
        assertFalse(actualConfig.card.enabled)
    }

    @Test
    fun `card scene and edited copy survive history serialization`() {
        val inputConfig: QrConfig = QrConfig(
            contentType = QrContentType.URL,
            content = "https://example.com/menu",
            card = QrCardConfig(QrScene.MENU, true, "巷口小馆", "先看看菜单，再慢慢选")
        )
        val actualConfig: QrConfig = QrConfigSerializer.deserialize(QrConfigSerializer.serialize(inputConfig))
        assertEquals(inputConfig, actualConfig)
    }

    @Test
    fun `partial card config receives defaults for added fields`() {
        val inputJson: String = """{"content":"hello","card":{"enabled":true,"scene":"WIFI"}}"""
        val actualConfig: QrConfig = QrConfigSerializer.deserialize(inputJson)
        assertEquals(QrCardConfig(scene = QrScene.WIFI, enabled = true), actualConfig.card)
    }
}
