package com.example.qrgenerator.ui.scanner

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.qrgenerator.domain.QrScanFailure
import com.example.qrgenerator.domain.QrScanResult
import com.example.qrgenerator.domain.QrScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanUiState(
    val contents: List<String> = emptyList(),
    val isChoosingImage: Boolean = false,
    val isReadingImage: Boolean = false,
    val message: String? = null,
    val session: Long = 0L
) {
    val canAnalyze: Boolean get() = contents.isEmpty() && !isChoosingImage && !isReadingImage && message == null
}

/** 会话编号隔离相机旧帧与图片任务，避免后到的结果覆盖当前操作。 */
class ScanViewModel(private val scanner: QrScanner, private val savedState: SavedStateHandle = SavedStateHandle()) : ViewModel() {
    private val mutableState: MutableStateFlow<ScanUiState> = MutableStateFlow(ScanUiState(
        contents = savedState.get<ArrayList<String>>(RESULTS)?.toList().orEmpty(),
        message = if (savedState.get<Boolean>(READING_IMAGE) == true) "上次图片识别未完成，请重新选择图片" else null
    ))
    val uiState: StateFlow<ScanUiState> = mutableState.asStateFlow()
    private var imageJob: Job? = null

    suspend fun scanFrame(bitmap: Bitmap): QrScanResult = scanner.scan(bitmap)

    fun pauseForImageSelection(): Unit {
        imageJob?.cancel()
        savedState[READING_IMAGE] = false
        mutableState.update { it.copy(isChoosingImage = true, isReadingImage = false, session = it.session + 1) }
    }

    fun readImage(uri: Uri?): Unit {
        if (uri == null) {
            mutableState.update { it.copy(isChoosingImage = false) }
            return
        }
        imageJob?.cancel()
        savedState[RESULTS] = arrayListOf<String>()
        savedState[READING_IMAGE] = true
        mutableState.update { ScanUiState(isReadingImage = true, session = it.session + 1) }
        val session: Long = mutableState.value.session
        imageJob = viewModelScope.launch {
            val result: QrScanResult = scanner.scan(uri)
            if (session == mutableState.value.session) showResult(result)
        }
    }

    fun acceptCameraResult(session: Long, result: QrScanResult): Unit {
        if (session != mutableState.value.session || !mutableState.value.canAnalyze || result == QrScanResult.NotFound) return
        showResult(result)
    }

    fun acceptCameraError(session: Long, message: String): Unit {
        if (session == mutableState.value.session && mutableState.value.canAnalyze) showError(message)
    }

    fun retry(): Unit {
        imageJob?.cancel()
        savedState[RESULTS] = arrayListOf<String>()
        savedState[READING_IMAGE] = false
        mutableState.update { ScanUiState(session = it.session + 1) }
    }

    fun showError(message: String): Unit {
        imageJob?.cancel()
        savedState[READING_IMAGE] = false
        mutableState.update { it.copy(isReadingImage = false, isChoosingImage = false, message = message, session = it.session + 1) }
    }

    private fun showResult(result: QrScanResult): Unit {
        savedState[READING_IMAGE] = false
        if (result is QrScanResult.Success) savedState[RESULTS] = ArrayList(result.contents)
        mutableState.update { state ->
            when (result) {
                is QrScanResult.Success -> state.copy(contents = result.contents, isReadingImage = false, message = null)
                QrScanResult.NotFound -> state.copy(isReadingImage = false, message = "图片中没有识别到二维码，请换一张清晰图片")
                is QrScanResult.Failure -> state.copy(isReadingImage = false, message = result.reason.toMessage())
            }
        }
    }

    private companion object {
        const val RESULTS: String = "scan_results"
        const val READING_IMAGE: String = "scan_reading_image"
    }
}

private fun QrScanFailure.toMessage(): String = when (this) {
    QrScanFailure.UNSUPPORTED_DEVICE -> "当前设备暂不支持扫码，可以返回手动填写内容"
    QrScanFailure.INITIALIZATION -> "扫码功能暂时无法启动，请重试或重新打开应用"
    QrScanFailure.IMAGE_READ -> "无法读取这张图片，请重新选择图片"
    QrScanFailure.DECODE -> "这次识别未完成，请重试或换一张图片"
}
