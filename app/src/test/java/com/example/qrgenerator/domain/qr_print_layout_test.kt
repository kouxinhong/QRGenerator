package com.example.qrgenerator.domain

import com.example.qrgenerator.data.model.QrPrintLayout
import com.example.qrgenerator.data.model.QrPrintPlan
import com.example.qrgenerator.data.model.QrPrintPlanner
import com.example.qrgenerator.data.model.QrPrintRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrPrintLayoutTest {
    @Test
    fun `单张桌牌保持毫米尺寸且在A4纸上居中`(): Unit {
        val actualPlan: QrPrintPlan = QrPrintPlanner.createPlan(QrPrintLayout.SINGLE_CARD)
        val actualCard: QrPrintRect = actualPlan.cards.single()
        assertEquals(595, actualPlan.pageWidthPoints)
        assertEquals(842, actualPlan.pageHeightPoints)
        assertEquals(100f, actualCard.width / QrPrintPlanner.POINTS_PER_MM, .001f)
        assertEquals(140f, actualCard.height / QrPrintPlanner.POINTS_PER_MM, .001f)
        assertEquals(actualCard.left, actualPlan.pageWidthPoints - actualCard.right, .001f)
        assertEquals(actualCard.top, actualPlan.pageHeightPoints - actualCard.bottom, .001f)
    }

    @Test
    fun `四张小卡保留裁切间距且全部位于可打印区域`(): Unit {
        val actualPlan: QrPrintPlan = QrPrintPlanner.createPlan(QrPrintLayout.A4_FOUR)
        assertEquals(4, actualPlan.cards.size)
        actualPlan.cards.forEach { card ->
            assertEquals(90f, card.width / QrPrintPlanner.POINTS_PER_MM, .001f)
            assertEquals(120f, card.height / QrPrintPlanner.POINTS_PER_MM, .001f)
            assertCropMarksInsidePage(card, actualPlan)
        }
        val first: QrPrintRect = actualPlan.cards[0]
        val second: QrPrintRect = actualPlan.cards[1]
        val third: QrPrintRect = actualPlan.cards[2]
        assertEquals(10f, (second.left - first.right) / QrPrintPlanner.POINTS_PER_MM, .001f)
        assertEquals(10f, (third.top - first.bottom) / QrPrintPlanner.POINTS_PER_MM, .001f)
    }

    @Test
    fun `横图竖图和方图都等比完整放入卡片且不覆盖裁切线`(): Unit {
        QrPrintLayout.entries.forEach { layout ->
            val card: QrPrintRect = QrPrintPlanner.createPlan(layout).cards.first()
            listOf(1024 to 512, 512 to 1024, 768 to 768).forEach { (width, height) ->
                val actualImage: QrPrintRect = QrPrintPlanner.fitArtwork(width, height, card)
                assertEquals(width.toFloat() / height, actualImage.width / actualImage.height, .001f)
                assertTrue(actualImage.left >= card.left - .001f && actualImage.top >= card.top - .001f)
                assertTrue(actualImage.right <= card.right + .001f && actualImage.bottom <= card.bottom + .001f)
                assertEquals(card.left + card.width / 2f, actualImage.left + actualImage.width / 2f, .001f)
                assertEquals(card.top + card.height / 2f, actualImage.top + actualImage.height / 2f, .001f)
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `拒绝无效图片尺寸而不生成错误排版`(): Unit {
        QrPrintPlanner.fitArtwork(0, 512, QrPrintPlanner.createPlan(QrPrintLayout.SINGLE_CARD).cards.first())
    }

    private fun assertCropMarksInsidePage(card: QrPrintRect, plan: QrPrintPlan): Unit {
        val markExtent: Float = (QrPrintPlanner.CROP_MARK_GAP_MM + QrPrintPlanner.CROP_MARK_LENGTH_MM) * QrPrintPlanner.POINTS_PER_MM
        val printerMargin: Float = 5f * QrPrintPlanner.POINTS_PER_MM
        assertTrue(card.left - markExtent >= printerMargin)
        assertTrue(card.top - markExtent >= printerMargin)
        assertTrue(card.right + markExtent <= plan.pageWidthPoints - printerMargin)
        assertTrue(card.bottom + markExtent <= plan.pageHeightPoints - printerMargin)
    }
}
