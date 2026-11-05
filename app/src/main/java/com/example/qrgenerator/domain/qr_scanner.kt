package com.example.qrgenerator.domain

import android.graphics.Bitmap
import android.net.Uri

/** 相机帧、相册图片和其他页面共用的本机二维码识别入口。 */
interface QrScanner {
    /** 不修改或回收调用方的图片；调用完成前请保留图片。 */
    suspend fun scan(bitmap: Bitmap): QrScanResult
    suspend fun scan(uri: Uri): QrScanResult
}

sealed interface QrScanResult {
    /** 保留原文，包括空格和换行；同一内容只返回一次。 */
    data class Success(val contents: List<String>) : QrScanResult
    data object NotFound : QrScanResult
    data class Failure(val reason: QrScanFailure) : QrScanResult
}

enum class QrScanFailure { UNSUPPORTED_DEVICE, INITIALIZATION, IMAGE_READ, DECODE }
