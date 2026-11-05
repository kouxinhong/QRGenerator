package com.example.qrgenerator.ui.scanner

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.example.qrgenerator.data.model.QrScene

enum class QrScanSource { CAMERA, PHOTO }

/** 任意 Activity 或 Compose 页面都可注册此入口；取消时返回 null，成功时保留完整原文。 */
class QrScanContract(
    private val targetScene: QrScene? = null,
    private val source: QrScanSource = QrScanSource.CAMERA
) : ActivityResultContract<Unit, String?>() {
    override fun createIntent(context: Context, input: Unit): Intent = Intent(context, QrScanActivity::class.java)
        .putExtra(TARGET_SCENE, targetScene?.name)
        .putExtra(SOURCE, source.name)

    override fun parseResult(resultCode: Int, intent: Intent?): String? =
        if (resultCode == Activity.RESULT_OK) intent?.getStringExtra(RESULT_CONTENT) else null

    companion object {
        internal const val RESULT_CONTENT: String = "com.example.qrgenerator.SCAN_CONTENT"
        internal const val TARGET_SCENE: String = "com.example.qrgenerator.SCAN_TARGET_SCENE"
        internal const val SOURCE: String = "com.example.qrgenerator.SCAN_SOURCE"

        internal fun readTargetScene(intent: Intent): QrScene? =
            intent.getStringExtra(TARGET_SCENE)?.let { value -> QrScene.entries.firstOrNull { it.name == value } }

        internal fun readSource(intent: Intent): QrScanSource =
            QrScanSource.entries.firstOrNull { it.name == intent.getStringExtra(SOURCE) } ?: QrScanSource.CAMERA
    }
}
