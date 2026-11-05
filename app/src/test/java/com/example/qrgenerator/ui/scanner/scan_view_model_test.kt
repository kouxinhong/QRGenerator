package com.example.qrgenerator.ui.scanner

import android.net.Uri
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.SavedStateHandle
import com.example.qrgenerator.domain.QrScanFailure
import com.example.qrgenerator.domain.QrScanResult
import com.example.qrgenerator.domain.QrScanner
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private val scanner: QrScanner = mockk()
    private val store: ViewModelStore = ViewModelStore()
    private lateinit var viewModel: ScanViewModel

    @Before
    fun setup(): Unit {
        Dispatchers.setMain(dispatcher)
        viewModel = ScanViewModel(scanner)
        store.put("scanner", viewModel)
    }

    @After
    fun teardown(): Unit {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `选择图片后忽略之前的相机结果和绑定失败`(): Unit {
        val session: Long = viewModel.uiState.value.session
        viewModel.pauseForImageSelection()
        viewModel.acceptCameraResult(session, QrScanResult.Success(listOf("旧相机帧")))
        viewModel.acceptCameraError(session, "旧绑定失败")
        assertTrue(viewModel.uiState.value.isChoosingImage)
        assertTrue(viewModel.uiState.value.contents.isEmpty())
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `识别图片保留多个码的原始空格换行并暂停相机`(): Unit = runTest(dispatcher) {
        val uri: Uri = mockk()
        val expectedContents: List<String> = listOf("  WIFI:S:门店;P:pass;;\n", "https://example.com")
        coEvery { scanner.scan(uri) } returns QrScanResult.Success(expectedContents)
        viewModel.pauseForImageSelection()
        viewModel.readImage(uri)
        advanceUntilIdle()
        assertEquals(expectedContents, viewModel.uiState.value.contents)
        assertFalse(viewModel.uiState.value.isReadingImage)
        assertFalse(viewModel.uiState.value.canAnalyze)
    }

    @Test
    fun `取消图片选择保留已有结果`(): Unit {
        viewModel.acceptCameraResult(viewModel.uiState.value.session, QrScanResult.Success(listOf("原内容")))
        viewModel.pauseForImageSelection()
        viewModel.readImage(null)
        assertEquals(listOf("原内容"), viewModel.uiState.value.contents)
        assertFalse(viewModel.uiState.value.isChoosingImage)
        assertFalse(viewModel.uiState.value.canAnalyze)
    }

    @Test
    fun `图片未识别到二维码提示换图而相机空帧继续扫描`(): Unit = runTest(dispatcher) {
        viewModel.acceptCameraResult(viewModel.uiState.value.session, QrScanResult.NotFound)
        assertTrue(viewModel.uiState.value.canAnalyze)
        val uri: Uri = mockk()
        coEvery { scanner.scan(uri) } returns QrScanResult.NotFound
        viewModel.readImage(uri)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.canAnalyze)
    }

    @Test
    fun `继续扫描使旧帧和未完成图片任务失效`(): Unit = runTest(dispatcher) {
        val uri: Uri = mockk()
        val deferred: CompletableDeferred<QrScanResult> = CompletableDeferred()
        coEvery { scanner.scan(uri) } coAnswers { deferred.await() }
        viewModel.readImage(uri)
        runCurrent()
        val oldSession: Long = viewModel.uiState.value.session
        viewModel.retry()
        deferred.complete(QrScanResult.Success(listOf("旧图片")))
        viewModel.acceptCameraResult(oldSession, QrScanResult.Success(listOf("旧帧")))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canAnalyze)
        assertTrue(viewModel.uiState.value.contents.isEmpty())
    }

    @Test
    fun `初始化失败暂停识别避免后续帧重复覆盖提示`(): Unit {
        val session: Long = viewModel.uiState.value.session
        viewModel.acceptCameraResult(session, QrScanResult.Failure(QrScanFailure.INITIALIZATION))
        val message: String? = viewModel.uiState.value.message
        viewModel.acceptCameraResult(session, QrScanResult.Success(listOf("后来的结果")))
        assertNotNull(message)
        assertEquals(message, viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.contents.isEmpty())
        assertFalse(viewModel.uiState.value.canAnalyze)
    }

    @Test
    fun `恢复尚未处理的扫码结果不会再次开始相机分析`(): Unit {
        val contents: ArrayList<String> = arrayListOf("  WIFI:S:Cafe;P:secret;;\n", "https://example.com")
        val savedState: SavedStateHandle = SavedStateHandle(mapOf("scan_results" to contents))
        val restored: ScanViewModel = ScanViewModel(scanner, savedState)
        store.put("restored", restored)
        assertEquals(contents, restored.uiState.value.contents)
        assertFalse(restored.uiState.value.canAnalyze)
        restored.retry()
        assertTrue(savedState.get<ArrayList<String>>("scan_results").orEmpty().isEmpty())
    }

    @Test
    fun `后台图片任务无法恢复时提示重新选图而不是无限等待`(): Unit {
        val restored: ScanViewModel = ScanViewModel(scanner, SavedStateHandle(mapOf("scan_reading_image" to true)))
        store.put("restored", restored)
        assertFalse(restored.uiState.value.isReadingImage)
        assertEquals("上次图片识别未完成，请重新选择图片", restored.uiState.value.message)
    }
}
