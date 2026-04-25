package com.example.qrgenerator.ui.shapes

import android.graphics.Path
import com.github.alexzhirkevich.customqrgenerator.style.Neighbors
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape

object HoneycombShape : QrVectorPixelShape {
    override fun createPath(size: Float, neighbors: Neighbors): Path {
        val r = size / 2f
        val h = r * 0.866f
        return Path().apply {
            moveTo(r, 0f)
            lineTo(r + h, r / 2f)
            lineTo(r + h, r * 1.5f)
            lineTo(r, size)
            lineTo(r - h, r * 1.5f)
            lineTo(r - h, r / 2f)
            close()
        }
    }
}
