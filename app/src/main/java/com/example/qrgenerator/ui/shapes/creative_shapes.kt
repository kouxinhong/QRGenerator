package com.example.qrgenerator.ui.shapes

import android.graphics.Path
import android.graphics.RectF
import com.github.alexzhirkevich.customqrgenerator.style.Neighbors
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape

/** 保留实心中心和足够墨量，装饰只作用于模块边缘。 */
object BambooShape : QrVectorPixelShape {
    override fun createPath(size: Float, neighbors: Neighbors): Path = Path().apply {
        moveTo(size * .22f, 0f)
        lineTo(size * .78f, 0f)
        lineTo(size * .88f, size * .22f)
        lineTo(size, size * .24f)
        lineTo(size, size * .36f)
        lineTo(size * .88f, size * .38f)
        lineTo(size * .88f, size * .70f)
        lineTo(size, size * .72f)
        lineTo(size, size * .84f)
        lineTo(size * .84f, size * .86f)
        lineTo(size * .78f, size)
        lineTo(size * .22f, size)
        lineTo(size * .16f, size * .86f)
        lineTo(0f, size * .84f)
        lineTo(0f, size * .72f)
        lineTo(size * .12f, size * .70f)
        lineTo(size * .12f, size * .38f)
        lineTo(0f, size * .36f)
        lineTo(0f, size * .24f)
        lineTo(size * .12f, size * .22f)
        close()
    }
}

/** 纸鸢的不对称折角；中间保持连续的深色区域。 */
object KiteShape : QrVectorPixelShape {
    override fun createPath(size: Float, neighbors: Neighbors): Path = Path().apply {
        moveTo(size * .32f, 0f)
        lineTo(size * .72f, size * .10f)
        lineTo(size, size * .32f)
        lineTo(size * .90f, size * .72f)
        lineTo(size * .68f, size)
        lineTo(size * .28f, size * .90f)
        lineTo(0f, size * .68f)
        lineTo(size * .10f, size * .28f)
        close()
    }
}

/** 四瓣花窗，花瓣并入实心中心，不挖空数据点。 */
object PetalShape : QrVectorPixelShape {
    override fun createPath(size: Float, neighbors: Neighbors): Path = Path().apply {
        addRoundRect(RectF(size * .12f, size * .12f, size * .88f, size * .88f), size * .18f, size * .18f, Path.Direction.CW)
        addCircle(size * .5f, size * .25f, size * .25f, Path.Direction.CW)
        addCircle(size * .75f, size * .5f, size * .25f, Path.Direction.CW)
        addCircle(size * .5f, size * .75f, size * .25f, Path.Direction.CW)
        addCircle(size * .25f, size * .5f, size * .25f, Path.Direction.CW)
    }
}

/** 四边错位的波纹方块，轻微凹凸不破坏中心采样点。 */
object RippleShape : QrVectorPixelShape {
    override fun createPath(size: Float, neighbors: Neighbors): Path = Path().apply {
        moveTo(size * .14f, size * .04f)
        cubicTo(size * .40f, size * .18f, size * .60f, 0f, size * .86f, size * .04f)
        quadTo(size * .98f, size * .04f, size * .96f, size * .14f)
        cubicTo(size * .82f, size * .40f, size, size * .60f, size * .96f, size * .86f)
        quadTo(size * .96f, size * .98f, size * .86f, size * .96f)
        cubicTo(size * .60f, size * .82f, size * .40f, size, size * .14f, size * .96f)
        quadTo(size * .02f, size * .96f, size * .04f, size * .86f)
        cubicTo(size * .18f, size * .60f, 0f, size * .40f, size * .04f, size * .14f)
        quadTo(size * .04f, size * .02f, size * .14f, size * .04f)
        close()
    }
}
