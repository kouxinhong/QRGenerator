package com.example.qrgenerator.ui.screens.style

import com.example.qrgenerator.data.model.*
import org.junit.Assert.*
import org.junit.Test

class StylePresetTest {
    @Test
    fun applyingPresetPreservesImagesContentCanvasAndCustomSpacing() {
        val original = QrConfig(
            content = "周六见", imageWidth = 768, imageHeight = 1024, padding = 0.35f,
            offsetX = 0.1f, codeShape = CodeShapeType.CIRCLE,
            logo = QrLogoConfig(imagePath = "/private/avatar.png"),
            background = QrBackgroundConfig(imagePath = "/private/background.png"),
            lightColor = QrColorConfig(type = QrColorType.TRANSPARENT)
        )
        StylePreset.entries.forEach { preset ->
            val actual = preset.applyTo(original)
            assertEquals(original.content, actual.content)
            assertEquals(original.logo, actual.logo)
            assertEquals(original.background, actual.background)
            assertEquals(original.padding, actual.padding)
            assertEquals(original.offsetX, actual.offsetX)
            assertEquals(original.codeShape, actual.codeShape)
            assertEquals(original.lightColor, actual.lightColor)
            assertEquals(original.imageWidth, actual.imageWidth)
            assertEquals(original.imageHeight, actual.imageHeight)
            assertTrue(preset.isAppliedTo(actual))
        }
    }

    @Test
    fun selectedPresetBecomesCustomWhenItsControlledFieldsChange() {
        val preset = StylePreset.BAMBOO
        val applied = preset.applyTo(QrConfig())
        assertTrue(preset.isAppliedTo(applied))
        assertFalse(preset.isAppliedTo(applied.copy(darkColor = applied.darkColor.copy(type = QrColorType.LINEAR_GRADIENT))))
        assertFalse(preset.isAppliedTo(applied.copy(frameShape = QrFrameShapeType.CIRCLE)))
        assertFalse(preset.isAppliedTo(applied.copy(ballColor = QrColorConfig(solidColor = 0xFF000000.toInt()))))
        assertFalse(preset.isAppliedTo(applied.copy(errorCorrectionLevel = QrErrorLevel.LOW)))
        assertTrue(preset.isAppliedTo(applied.copy(padding = 0.4f, logo = QrLogoConfig(builtinIconName = "heart"))))
    }
}
