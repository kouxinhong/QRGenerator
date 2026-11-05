package com.example.qrgenerator.ui

import android.graphics.Color
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.qrgenerator.data.model.QrBackgroundConfig
import com.example.qrgenerator.data.model.QrCardConfig
import com.example.qrgenerator.data.model.QrColorConfig
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.data.model.QrScene
import com.example.qrgenerator.data.model.QrSourcePayload
import com.example.qrgenerator.domain.QrConfigHolderImpl
import com.example.qrgenerator.domain.QrContentCodec
import com.example.qrgenerator.domain.QrGeneratorUseCase
import com.example.qrgenerator.domain.QrReadability
import com.example.qrgenerator.domain.QrReadabilityChecker
import com.example.qrgenerator.ui.screens.style.StyleEvent
import com.example.qrgenerator.ui.screens.style.StylePreviewState
import com.example.qrgenerator.ui.screens.style.StyleViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** 实际渲染和解码样式预览，检查失败立即反馈，最终成品仍由制作页独立校验。 */
@RunWith(AndroidJUnit4::class)
class StylePreviewCheckTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val holder: QrConfigHolderImpl = QrConfigHolderImpl()
    private val store: ViewModelStore = ViewModelStore()
    private var viewModelJob: Job? = null

    @After
    fun closeViewModel(): Unit = runBlocking {
        instrumentation.runOnMainSync { store.clear() }
        withTimeout(5_000) { viewModelJob?.join() }
    }

    @Test
    fun importedPayloadIsVerifiedWithoutDroppingOriginalFields(): Unit = runBlocking {
        val inputConfig: QrConfig = createImportedConfig()
        val viewModel: StyleViewModel = createViewModel(inputConfig)
        val actualPreview: StylePreviewState = awaitPreview(viewModel)
        assertEquals(QrReadability.VERIFIED, actualPreview.readability)
        assertFalse(actualPreview.isExample)
        assertEquals(QrReadability.VERIFIED, QrReadabilityChecker().execute(requireNotNull(actualPreview.bitmap), requireNotNull(inputConfig.sourcePayload).rawPayload))
        assertEquals(inputConfig, holder.config.value)
    }

    @Test
    fun unreadableStyleClearsOldCheckAndCanRecoverWithoutLosingImportedData(): Unit = runBlocking {
        val inputConfig: QrConfig = createImportedConfig()
        val viewModel: StyleViewModel = createViewModel(inputConfig)
        assertEquals(QrReadability.VERIFIED, awaitPreview(viewModel).readability)
        instrumentation.runOnMainSync {
            viewModel.onEvent(StyleEvent.UpdateConfig { current -> current.copy(
                darkColor = QrColorConfig(solidColor = Color.WHITE),
                ballColor = QrColorConfig(solidColor = Color.WHITE),
                frameColor = QrColorConfig(solidColor = Color.WHITE)
            ) })
            assertTrue(viewModel.preview.value.isLoading)
            assertNull(viewModel.preview.value.readability)
        }
        assertEquals(QrReadability.UNREADABLE, awaitPreview(viewModel).readability)
        instrumentation.runOnMainSync { viewModel.onEvent(StyleEvent.ResetStyle) }
        assertEquals(QrReadability.VERIFIED, awaitPreview(viewModel).readability)
        assertEquals(inputConfig.content, holder.config.value.content)
        assertEquals(inputConfig.card, holder.config.value.card)
        assertEquals(inputConfig.sourcePayload, holder.config.value.sourcePayload)
        assertEquals(inputConfig.imageWidth, holder.config.value.imageWidth)
        assertEquals(inputConfig.imageHeight, holder.config.value.imageHeight)
    }

    @Test
    fun missingBackgroundDoesNotRetainPriorSuccessfulCheck(): Unit = runBlocking {
        val viewModel: StyleViewModel = createViewModel(QrConfig(content = "检查错误反馈"))
        assertEquals(QrReadability.VERIFIED, awaitPreview(viewModel).readability)
        instrumentation.runOnMainSync {
            viewModel.onEvent(StyleEvent.UpdateConfig { current -> current.copy(background = QrBackgroundConfig(imagePath = "/missing/qr-style-image.png")) })
        }
        val actualPreview: StylePreviewState = awaitPreview(viewModel)
        assertNotNull(actualPreview.error)
        assertNull(actualPreview.bitmap)
        assertNull(actualPreview.readability)
        assertFalse(actualPreview.isLoading)
    }

    @Test
    fun blankContentUsesClearlyMarkedExampleWithoutChangingDraft(): Unit = runBlocking {
        val viewModel: StyleViewModel = createViewModel(QrConfig())
        val actualPreview: StylePreviewState = awaitPreview(viewModel)
        assertTrue(actualPreview.isExample)
        assertEquals(QrReadability.VERIFIED, actualPreview.readability)
        assertEquals("", holder.config.value.content)
    }

    private fun createViewModel(config: QrConfig): StyleViewModel {
        holder.executeSetConfig(config)
        lateinit var viewModel: StyleViewModel
        instrumentation.runOnMainSync {
            viewModel = StyleViewModel(holder, QrGeneratorUseCase(instrumentation.targetContext))
            viewModelJob = viewModel.viewModelScope.coroutineContext.job
            store.put("style_preview", viewModel)
        }
        return viewModel
    }

    private suspend fun awaitPreview(viewModel: StyleViewModel): StylePreviewState = withTimeout(5_000) {
        viewModel.preview.first { !it.isLoading && (it.bitmap != null || it.error != null) }
    }

    private fun createImportedConfig(): QrConfig {
        val content: String = QrContentCodec.serialize(mapOf("ssid" to "门店网络", "password" to "password123", "auth" to "WPA", "hidden" to "true"))
        return QrConfig(contentType = QrContentType.WIFI, content = content, imageWidth = 768, imageHeight = 1024,
            card = QrCardConfig(QrScene.WIFI, true, "欢迎上网", "扫一扫，连接网络"),
            sourcePayload = QrSourcePayload("WIFI:T:WPA;S:门店网络;P:password123;H:true;X:保留的额外字段;;", QrContentType.WIFI, content))
    }
}
