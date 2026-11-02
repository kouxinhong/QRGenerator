package com.example.qrgenerator.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.example.qrgenerator.data.model.QrCardConfig
import kotlin.math.roundToInt

/** 预览和所有导出共用的卡面；二维码按原始比例完整绘制。 */
class QrCardRenderer {
    fun execute(qrBitmap: Bitmap, card: QrCardConfig): Bitmap {
        if (!card.enabled) return qrBitmap
        val layout: CardLayout = createLayout(qrBitmap)
        val bitmap: Bitmap = Bitmap.createBitmap(layout.width, layout.height, Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        drawTitle(canvas, card, layout)
        canvas.drawBitmap(qrBitmap, null, layout.qrBounds, Paint(Paint.FILTER_BITMAP_FLAG))
        drawSubtitle(canvas, card, layout)
        return bitmap
    }

    private fun createLayout(qrBitmap: Bitmap): CardLayout {
        val desiredWidth: Float = qrBitmap.width.coerceIn(MIN_WIDTH, MAX_EDGE).toFloat()
        val initialScale: Float = desiredWidth / BASE_WIDTH
        val qrHeight: Float = (desiredWidth - 2 * MARGIN * initialScale) * qrBitmap.height / qrBitmap.width
        val desiredHeight: Float = (HEADER_HEIGHT + FOOTER_HEIGHT) * initialScale + qrHeight
        val fit: Float = minOf(1f, MAX_EDGE / desiredHeight)
        val scale: Float = initialScale * fit
        val width: Int = (desiredWidth * fit).roundToInt().coerceAtLeast(1)
        val height: Int = (desiredHeight * fit).roundToInt().coerceIn(1, MAX_EDGE)
        return CardLayout(width, height, scale, RectF(MARGIN * scale, HEADER_HEIGHT * scale, width - MARGIN * scale, HEADER_HEIGHT * scale + qrHeight * fit))
    }

    private fun drawTitle(canvas: Canvas, card: QrCardConfig, layout: CardLayout) {
        val title: String = limitText(card.title.ifBlank { card.scene.defaultTitle }, TITLE_LIMIT)
        val paint: TextPaint = createPaint(TITLE_SIZE * layout.scale, INK_COLOR, Typeface.DEFAULT_BOLD)
        drawText(canvas, title, paint, layout, 56f * layout.scale, 2, Layout.Alignment.ALIGN_NORMAL)
    }

    private fun drawSubtitle(canvas: Canvas, card: QrCardConfig, layout: CardLayout) {
        val subtitle: String = limitText(card.subtitle.ifBlank { card.scene.defaultSubtitle }, SUBTITLE_LIMIT)
        val paint: TextPaint = createPaint(SUBTITLE_SIZE * layout.scale, SECONDARY_COLOR, Typeface.DEFAULT)
        drawText(canvas, subtitle, paint, layout, layout.qrBounds.bottom + 32f * layout.scale, 3, Layout.Alignment.ALIGN_CENTER)
    }

    private fun drawText(canvas: Canvas, text: String, paint: TextPaint, layout: CardLayout, top: Float, maxLines: Int, alignment: Layout.Alignment) {
        val availableWidth: Int = (layout.width - 2 * MARGIN * layout.scale).roundToInt().coerceAtLeast(1)
        val textLayout: StaticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, availableWidth)
            .setAlignment(alignment).setIncludePad(false).setLineSpacing(0f, 1.15f)
            .setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END).build()
        canvas.save()
        canvas.translate(MARGIN * layout.scale, top)
        textLayout.draw(canvas)
        canvas.restore()
    }

    private fun createPaint(size: Float, color: Int, font: Typeface): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = font
    }

    private fun limitText(value: String, limit: Int): String {
        val trimmed: String = value.trim()
        val count: Int = trimmed.codePointCount(0, trimmed.length)
        return if (count <= limit) trimmed else trimmed.substring(0, trimmed.offsetByCodePoints(0, limit)) + "…"
    }

    private data class CardLayout(val width: Int, val height: Int, val scale: Float, val qrBounds: RectF)

    private companion object {
        const val BASE_WIDTH: Float = 1024f
        const val MIN_WIDTH: Int = 768
        const val MAX_EDGE: Int = 4096
        const val MARGIN: Float = 56f
        const val HEADER_HEIGHT: Float = 184f
        const val FOOTER_HEIGHT: Float = 164f
        const val TITLE_SIZE: Float = 42f
        const val SUBTITLE_SIZE: Float = 28f
        const val TITLE_LIMIT: Int = 60
        const val SUBTITLE_LIMIT: Int = 120
        val INK_COLOR: Int = 0xFF174F3B.toInt()
        val SECONDARY_COLOR: Int = 0xFF505C56.toInt()
    }
}
