package com.example.qrgenerator.domain

import com.example.qrgenerator.ui.shapes.CatPawShape
import com.example.qrgenerator.ui.shapes.HoneycombShape
import com.example.qrgenerator.ui.shapes.BambooShape
import com.example.qrgenerator.ui.shapes.KiteShape
import com.example.qrgenerator.ui.shapes.PetalShape
import com.example.qrgenerator.ui.shapes.RippleShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape

object CustomShapeRegistry {
    private val pixelShapes = mutableMapOf<String, QrVectorPixelShape>()
    private val colors = mutableMapOf<String, QrVectorColor>()

    init {
        registerPixelShape("honeycomb", HoneycombShape)
        registerPixelShape("cat_paw", CatPawShape)
        registerPixelShape("bamboo", BambooShape)
        registerPixelShape("kite", KiteShape)
        registerPixelShape("petal", PetalShape)
        registerPixelShape("ripple", RippleShape)
    }

    fun registerPixelShape(name: String, shape: QrVectorPixelShape) { pixelShapes[name] = shape }
    fun registerColor(name: String, color: QrVectorColor) { colors[name] = color }
    fun getPixelShape(name: String): QrVectorPixelShape? = pixelShapes[name]
    fun getColor(name: String): QrVectorColor? = colors[name]
    fun getPixelShapeNames(): Set<String> = pixelShapes.keys
    fun getColorNames(): Set<String> = colors.keys
}
