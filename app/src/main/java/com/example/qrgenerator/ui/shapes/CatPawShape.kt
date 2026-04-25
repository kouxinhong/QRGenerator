package com.example.qrgenerator.ui.shapes

import android.graphics.Path
import com.github.alexzhirkevich.customqrgenerator.style.Neighbors
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape

object CatPawShape : QrVectorPixelShape {
    override fun createPath(size: Float, neighbors: Neighbors): Path {
        val palmRadius = size * 0.3f
        val toeRadius = size * 0.12f
        return Path().apply {
            addCircle(size / 2f, size * 0.58f, palmRadius, Path.Direction.CW)
            addCircle(size * 0.3f, size * 0.22f, toeRadius, Path.Direction.CW)
            addCircle(size * 0.5f, size * 0.15f, toeRadius, Path.Direction.CW)
            addCircle(size * 0.7f, size * 0.22f, toeRadius, Path.Direction.CW)
        }
    }
}
