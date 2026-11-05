package com.example.qrgenerator.data.model

/** 表单未变时逐字使用扫码原文；用户编辑后使用新的表单内容生成。 */
data class QrSourcePayload(
    val rawPayload: String,
    val contentType: QrContentType,
    val formContent: String
)
