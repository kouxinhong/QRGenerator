package com.example.qrgenerator.domain

import com.example.qrgenerator.data.model.QrCardConfig
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.data.model.QrScene
import com.example.qrgenerator.data.model.QrSourcePayload

/** 识别负责返回内容，导入策略负责保留当前用途；原文与可编辑字段分别保存。 */
object QrImportPolicy {
    fun createConfig(current: QrConfig, payload: QrPayload): QrConfig {
        val type: QrContentType = payload.editableType ?: QrContentType.TEXT
        val content: String = payload.editableContent ?: payload.rawPayload
        val scene: QrScene = when (payload.kind) {
            QrPayloadKind.WIFI -> QrScene.WIFI
            QrPayloadKind.CONTACT -> QrScene.CONTACT
            QrPayloadKind.URL -> QrScene.MENU
            QrPayloadKind.TEXT -> QrScene.FREE
        }
        val canKeepCard: Boolean = (current.card.scene == scene &&
            (scene != QrScene.FREE || current.content.isNotBlank() || current.card.enabled ||
                current.card.title.isNotBlank() || current.card.subtitle.isNotBlank())) ||
            (current.card.scene == QrScene.FREE && current.contentType == type && current.card.enabled)
        val card: QrCardConfig = if (canKeepCard) current.card else QrCardConfig(
            scene = scene, enabled = true,
            title = if (payload.kind == QrPayloadKind.URL) "扫码查看详情" else scene.defaultTitle,
            subtitle = if (payload.kind == QrPayloadKind.URL) "扫一扫，打开页面" else scene.defaultSubtitle
        )
        return current.copy(contentType = type, content = content, card = card,
            sourcePayload = QrSourcePayload(payload.rawPayload, type, content))
    }
}
