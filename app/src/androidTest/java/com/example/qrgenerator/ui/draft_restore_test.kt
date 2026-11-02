package com.example.qrgenerator.ui

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.qrgenerator.MainActivity
import com.example.qrgenerator.data.model.*
import com.example.qrgenerator.domain.QrConfigHolder
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

class DraftRestoreTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun savedActivityStateRestoresDraftAfterInMemoryStateIsLost() {
        val holder = GlobalContext.get().get<QrConfigHolder>()
        val expected = QrConfig(content = "放学后去图书馆", darkPixelShape = QrPixelShapeType.PETAL,
            imageWidth = 768, imageHeight = 1536, errorCorrectionLevel = QrErrorLevel.HIGH,
            logo = QrLogoConfig(source = LogoSource.BUILTIN_ICON, builtinIconName = "leaf"))
        compose.runOnIdle { holder.executeSetConfig(expected) }
        val originalActivity = compose.activity
        val application = originalActivity.application
        // 模拟系统回收后的内存丢失；新 Activity 必须从 Bundle 恢复，不能依赖单例。
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityDestroyed(activity: Activity) {
                if (activity === originalActivity) holder.executeSetConfig(QrConfig())
            }
            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
        }
        application.registerActivityLifecycleCallbacks(callbacks)
        try {
            compose.activityRule.scenario.recreate()
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(expected, holder.config.value) }
        } finally {
            application.unregisterActivityLifecycleCallbacks(callbacks)
        }
    }
}
