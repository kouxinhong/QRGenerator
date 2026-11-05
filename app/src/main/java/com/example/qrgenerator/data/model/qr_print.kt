package com.example.qrgenerator.data.model

import kotlin.math.roundToInt

/** 裁下来的卡片尺寸固定；图片在卡片内等比居中，不裁剪二维码或说明文字。 */
enum class QrPrintLayout(
    val title: String,
    val cardWidthMm: Float,
    val cardHeightMm: Float,
    val columns: Int,
    val rows: Int
) {
    SINGLE_CARD("单张桌牌", 100f, 140f, 1, 1),
    A4_FOUR("A4 四张小卡", 90f, 120f, 2, 2);

    val dimensionsLabel: String get() = "${cardWidthMm.toInt()} × ${cardHeightMm.toInt()} 毫米"
    val cardCount: Int get() = columns * rows
}

data class QrPrintRect(val left: Float, val top: Float, val width: Float, val height: Float) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

data class QrPrintPlan(val pageWidthPoints: Int, val pageHeightPoints: Int, val cards: List<QrPrintRect>)

/** 无 Android 依赖的毫米排版算法，PDF 与屏幕预览共用同一坐标。 */
object QrPrintPlanner {
    const val POINTS_PER_MM: Float = 72f / 25.4f
    const val CROP_MARK_GAP_MM: Float = 1.5f
    const val CROP_MARK_LENGTH_MM: Float = 3f
    private const val PAGE_WIDTH_MM: Float = 210f
    private const val PAGE_HEIGHT_MM: Float = 297f
    private const val CARD_GAP_MM: Float = 10f

    fun createPlan(layout: QrPrintLayout): QrPrintPlan {
        val pageWidth: Int = (PAGE_WIDTH_MM * POINTS_PER_MM).roundToInt()
        val pageHeight: Int = (PAGE_HEIGHT_MM * POINTS_PER_MM).roundToInt()
        val width: Float = layout.cardWidthMm * POINTS_PER_MM
        val height: Float = layout.cardHeightMm * POINTS_PER_MM
        val gap: Float = CARD_GAP_MM * POINTS_PER_MM
        val left: Float = (pageWidth - width * layout.columns - gap * (layout.columns - 1)) / 2f
        val top: Float = (pageHeight - height * layout.rows - gap * (layout.rows - 1)) / 2f
        val cards: List<QrPrintRect> = List(layout.cardCount) { index ->
            QrPrintRect(left + index % layout.columns * (width + gap), top + index / layout.columns * (height + gap), width, height)
        }
        return QrPrintPlan(pageWidth, pageHeight, cards)
    }

    fun fitArtwork(width: Int, height: Int, card: QrPrintRect): QrPrintRect {
        require(width > 0 && height > 0) { "图片尺寸必须大于零" }
        val scale: Float = minOf(card.width / width, card.height / height)
        val fittedWidth: Float = width * scale
        val fittedHeight: Float = height * scale
        return QrPrintRect(card.left + (card.width - fittedWidth) / 2f, card.top + (card.height - fittedHeight) / 2f, fittedWidth, fittedHeight)
    }
}
