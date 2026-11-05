package com.example.qrgenerator.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.example.qrgenerator.domain.QrScanResult
import com.example.qrgenerator.domain.QrScanner
import com.example.qrgenerator.ui.scanner.QrScanActivity
import com.example.qrgenerator.ui.scanner.QrScanContract
import com.example.qrgenerator.ui.scanner.QrScanSource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean

/** 单独运行前由宿主撤销 CAMERA；在目标进程里撤权会被系统杀进程，不能作为用例步骤。 */
class ScanPhotoEntryTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val frameCount: AtomicInteger = AtomicInteger()
    private var scenario: ActivityScenario<QrScanActivity>? = null
    private var originalScanner: QrScanner? = null

    @Before
    fun prepareWithoutCameraPermission(): Unit {
        assumeTrue("请在宿主撤销 CAMERA 后单独运行 ScanPhotoEntryTest", context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
        originalScanner = GlobalContext.get().get<QrScanner>()
        loadKoinModules(module { single<QrScanner> { object : QrScanner {
            override suspend fun scan(bitmap: Bitmap): QrScanResult {
                frameCount.incrementAndGet()
                return QrScanResult.NotFound
            }
            override suspend fun scan(uri: Uri): QrScanResult = QrScanResult.NotFound
        } } })
    }

    @After
    fun restoreScanner(): Unit {
        scenario?.close()
        originalScanner?.let { original -> loadKoinModules(module { single<QrScanner> { original } }) }
    }

    @Test
    fun choosingPhotoDoesNotRequestCameraAndCancellationReturnsToSourceChoice(): Unit {
        scenario = ActivityScenario.launchActivityForResult(QrScanContract(source = QrScanSource.PHOTO).createIntent(context, Unit))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        compose.waitUntil(10_000) {
            executeShell("dumpsys activity activities").lineSequence().any { line ->
                (line.contains("ResumedActivity") || line.contains("mResumed")) &&
                    (line.contains("photopicker", ignoreCase = true) || line.contains("documentsui", ignoreCase = true))
            }
        }
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission(Manifest.permission.CAMERA))
        executeShell("input keyevent KEYCODE_BACK")
        // 系统选择器退出后先等扫码 Activity 恢复，避免尚无 Compose 根节点时直接查询。
        compose.waitUntil(10_000) {
            val isResumed: AtomicBoolean = AtomicBoolean(false)
            instrumentation.runOnMainSync {
                isResumed.set(ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED).any { it is QrScanActivity })
            }
            isResumed.get()
        }
        instrumentation.waitForIdleSync()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("使用相机识别").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("从图片识别").assertIsEnabled()
        assertEquals(0, frameCount.get())
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission(Manifest.permission.CAMERA))
    }

    private fun executeShell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
    ).bufferedReader().use { it.readText() }
}
