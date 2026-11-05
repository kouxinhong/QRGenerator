package com.example.qrgenerator.util

import com.example.qrgenerator.data.model.QrConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder

object QrConfigSerializer {
    private val gson: Gson = GsonBuilder().create()
    fun serialize(config: QrConfig): String = gson.toJson(config.withoutStaleSourcePayload())
    fun deserialize(json: String): QrConfig = gson.fromJson(json, QrConfig::class.java).withoutStaleSourcePayload()
}
