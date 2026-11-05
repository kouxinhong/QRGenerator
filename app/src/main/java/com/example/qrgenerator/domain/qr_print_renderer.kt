package com.example.qrgenerator.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.qrgenerator.data.model.QrPrintLayout
import com.example.qrgenerator.data.model.QrPrintPlan
import com.example.qrgenerator.data.model.QrPrintPlanner
import com.example.qrgenerator.data.model.QrPrintRect
import java.io.OutputStream

/** 输出与预览共用的打印绘制；仅使用调用方图片，不修改、回收图片或关闭输出流。 */
class QrPrintRenderer {
    fun writePdf(artwork: Bitmap, layout: QrPrintLayout, stream: OutputStream): Unit {
        val plan: QrPrintPlan = QrPrintPlanner.createPlan(layout)
        val document: PdfDocument = PdfDocument()
        try {
            val page: PdfDocument.Page = document.startPage(PdfDocument.PageInfo.Builder(plan.pageWidthPoints, plan.pageHeightPoints, 1).create())
            drawPage(page.canvas, artwork, plan)
            document.finishPage(page)
            document.writeTo(stream)
        } finally {
            document.close()
        }
    }

    /** 画布坐标以 PDF point 为单位；预览端只需按页面宽高等比缩放画布。 */
    fun drawPage(canvas: Canvas, artwork: Bitmap, plan: QrPrintPlan): Unit {
        require(!artwork.isRecycled) { "打印图片已失效，请重新生成" }
        canvas.drawRect(0f, 0f, plan.pageWidthPoints.toFloat(), plan.pageHeightPoints.toFloat(), Paint().apply { color = Color.WHITE })
        val imagePaint: Paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val markPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF888888.toInt()
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
        }
        plan.cards.forEach { card ->
            val image: QrPrintRect = QrPrintPlanner.fitArtwork(artwork.width, artwork.height, card)
            canvas.drawBitmap(artwork, null, RectF(image.left, image.top, image.right, image.bottom), imagePaint)
            drawCropMarks(canvas, card, markPaint)
        }
    }

    private fun drawCropMarks(canvas: Canvas, card: QrPrintRect, paint: Paint): Unit {
        val gap: Float = QrPrintPlanner.CROP_MARK_GAP_MM * QrPrintPlanner.POINTS_PER_MM
        val length: Float = QrPrintPlanner.CROP_MARK_LENGTH_MM * QrPrintPlanner.POINTS_PER_MM
        listOf(card.top, card.bottom).forEach { y ->
            canvas.drawLine(card.left - gap - length, y, card.left - gap, y, paint)
            canvas.drawLine(card.right + gap, y, card.right + gap + length, y, paint)
        }
        listOf(card.left, card.right).forEach { x ->
            canvas.drawLine(x, card.top - gap - length, x, card.top - gap, paint)
            canvas.drawLine(x, card.bottom + gap, x, card.bottom + gap + length, paint)
        }
    }
}
