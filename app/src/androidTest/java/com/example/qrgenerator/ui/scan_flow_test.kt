package com.example.qrgenerator.ui

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.qrgenerator.domain.QrScanFailure
import com.example.qrgenerator.domain.QrScanResult
import com.example.qrgenerator.domain.QrScanner
import com.example.qrgenerator.domain.QrPayloadParser
import com.example.qrgenerator.data.model.QrScene
import com.example.qrgenerator.ui.scanner.QrScanActivity
import com.example.qrgenerator.ui.scanner.QrScanContract
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** 使用真实 CameraX 帧驱动界面，以替身隔离画面内容的不确定性；原生解码另有设备测试。 */
class ScanFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scanner: FrameScanner = FrameScanner()
    private var scenario: ActivityScenario<QrScanActivity>? = null
    private var originalScanner: QrScanner? = null

    @Before
    fun prepareCameraAndScanner(): Unit {
        val command: String = "pm grant ${context.packageName} android.permission.CAMERA"
        ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ).use { it.readBytes() }
        originalScanner = GlobalContext.get().get<QrScanner>()
        loadKoinModules(module { single<QrScanner> { scanner } })
    }

    @After
    fun restoreScanner(): Unit {
        scenario?.close()
        originalScanner?.let { original -> loadKoinModules(module { single<QrScanner> { original } }) }
    }

    @Test
    fun choosingSecondCameraResultReturnsCompleteOriginalPayload(): Unit {
        val expected: String = "  中文二维码\n第二行保留空白  "
        scanner.result.set(QrScanResult.Success(listOf("https://example.com/first", expected)))
        launchScanner()
        waitForText("识别到 2 个二维码，选择要用的内容")
        compose.onNodeWithText(QrPayloadParser.parse(expected).summary).assertExists()
        compose.onAllNodesWithText("选择这个二维码")[1].performScrollTo().performClick()
        compose.onNodeWithText("使用原内容制作").performScrollTo().performClick()
        val result = requireNotNull(scenario).result
        assertEquals(Activity.RESULT_OK, result.resultCode)
        assertEquals(expected, QrScanContract().parseResult(result.resultCode, result.resultData))
        assertTrue("结果应来自真实 CameraX 图像分析链路", scanner.frameCount.get() > 0)
    }

    @Test
    fun copyingAndContinuingScanDoesNotReturnContentUntilConfirmed(): Unit {
        val first: String = "  可复制的原文\n下一行  "
        val second: String = "下一次扫码内容"
        scanner.result.set(QrScanResult.Success(listOf(first)))
        launchScanner()
        waitForText(QrPayloadParser.parse(first).summary)
        compose.onNodeWithText("复制原文").performScrollTo().performClick()
        compose.runOnIdle {
            val clipboard: ClipboardManager = context.getSystemService(ClipboardManager::class.java)
            assertEquals(first, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        }
        val previousFrames: Int = scanner.frameCount.get()
        scanner.result.set(QrScanResult.Success(listOf(second)))
        compose.onNodeWithText("继续扫描").performScrollTo().performClick()
        waitForText(QrPayloadParser.parse(second).summary)
        assertTrue(scanner.frameCount.get() > previousFrames)
        compose.onNodeWithContentDescription("返回").performClick()
        val result = requireNotNull(scenario).result
        assertEquals(Activity.RESULT_CANCELED, result.resultCode)
        assertNull(QrScanContract().parseResult(result.resultCode, result.resultData))
    }

    @Test
    fun failedCameraDecodeAllowsPhotoSelectionAndCameraRetry(): Unit {
        scanner.result.set(QrScanResult.Failure(QrScanFailure.DECODE))
        launchScanner()
        waitForText("这次识别未完成，请重试或换一张图片")
        compose.onNodeWithText("从图片识别").assertIsEnabled()
        val expected: String = "重试后识别成功"
        scanner.result.set(QrScanResult.Success(listOf(expected)))
        compose.onNodeWithText("重新扫描").performClick()
        waitForText(expected)
        compose.onNodeWithText("使用原内容制作").performScrollTo().performClick()
        val result = requireNotNull(scenario).result
        assertEquals(expected, QrScanContract().parseResult(result.resultCode, result.resultData))
    }

    @Test
    fun wifiPasswordIsHiddenAndCurrentSceneActionIsExplicit(): Unit {
        val password: String = "private-password-123"
        val payload: String = "WIFI:T:WPA;S:Cafe;P:$password;;"
        scanner.result.set(QrScanResult.Success(listOf(payload)))
        scenario = ActivityScenario.launchActivityForResult(QrScanContract(targetScene = QrScene.WIFI).createIntent(context, Unit))
        waitForText("填入当前 Wi-Fi 卡")
        compose.onNodeWithText(password).assertDoesNotExist()
        compose.onNodeWithText(payload).assertDoesNotExist()
        val passwordLabel: String = QrPayloadParser.parse(payload).fields.first { it.isSensitive }.label
        compose.onNodeWithText("显示$passwordLabel").performScrollTo().performClick()
        compose.onNodeWithText(password).assertExists()
        requireNotNull(scenario).recreate()
        waitForText("填入当前 Wi-Fi 卡")
        compose.onNodeWithText(password).assertDoesNotExist()
        compose.onNodeWithText("填入当前 Wi-Fi 卡").performScrollTo().performClick()
        val result = requireNotNull(scenario).result
        assertEquals(payload, QrScanContract().parseResult(result.resultCode, result.resultData))
    }

    private fun launchScanner(): Unit {
        scenario = ActivityScenario.launchActivityForResult(Intent(context, QrScanActivity::class.java))
    }

    private fun waitForText(text: String): Unit {
        compose.waitUntil(20_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private class FrameScanner : QrScanner {
        val result: AtomicReference<QrScanResult> = AtomicReference(QrScanResult.NotFound)
        val frameCount: AtomicInteger = AtomicInteger()

        override suspend fun scan(bitmap: Bitmap): QrScanResult {
            check(!bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0)
            frameCount.incrementAndGet()
            return result.get()
        }

        override suspend fun scan(uri: Uri): QrScanResult = result.get()
    }
}
