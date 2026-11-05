package com.example.qrgenerator.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.qrgenerator.data.model.QrPrintLayout
import com.example.qrgenerator.data.model.QrPrintPlan
import com.example.qrgenerator.data.model.QrPrintPlanner
import com.example.qrgenerator.domain.QrPrintRenderer

/** 预览直接调用 PDF 的绘制服务，确保卡片位置、留白与裁切线一致。 */
@Composable
fun QrPrintPreview(artwork: Bitmap, layout: QrPrintLayout, modifier: Modifier = Modifier): Unit {
    val plan: QrPrintPlan = remember(layout) { QrPrintPlanner.createPlan(layout) }
    val renderer: QrPrintRenderer = remember { QrPrintRenderer() }
    Canvas(modifier.height(190.dp).aspectRatio(plan.pageWidthPoints.toFloat() / plan.pageHeightPoints)
        .background(Color.White).border(1.dp, MaterialTheme.colorScheme.outlineVariant)
        .testTag("print_preview").semantics {
            contentDescription = "A4 打印预览，${layout.cardCount} 张，每张 ${layout.dimensionsLabel}，含裁切线"
        }) {
        drawIntoCanvas { canvas ->
            val nativeCanvas: android.graphics.Canvas = canvas.nativeCanvas
            val saveCount: Int = nativeCanvas.save()
            try {
                nativeCanvas.scale(size.width / plan.pageWidthPoints, size.height / plan.pageHeightPoints)
                renderer.drawPage(nativeCanvas, artwork, plan)
            } finally {
                nativeCanvas.restoreToCount(saveCount)
            }
        }
    }
}
