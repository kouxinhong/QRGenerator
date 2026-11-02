package com.example.qrgenerator.data

import com.example.qrgenerator.data.local.QrHistoryDao
import com.example.qrgenerator.data.local.QrHistoryEntity
import com.example.qrgenerator.data.model.QrCardConfig
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.data.model.QrScene
import com.example.qrgenerator.data.repository.QrHistoryRepository
import com.example.qrgenerator.domain.QrContentCodec
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CardHistoryTest {
    @Test
    fun `卡片历史可按标题查找且摘要不会暴露网络密码`() = runTest {
        val dao: QrHistoryDao = mockk()
        val saved = slot<QrHistoryEntity>()
        coEvery { dao.insert(capture(saved)) } returns 1L
        val repository: QrHistoryRepository = QrHistoryRepository(dao)
        val config: QrConfig = QrConfig(
            contentType = QrContentType.WIFI,
            content = QrContentCodec.serialize(mapOf("ssid" to "Guest", "password" to "never-in-preview")),
            card = QrCardConfig(QrScene.WIFI, true, "小店访客网络", "扫码连接")
        )
        repository.executeSave(config, "/tmp/card.png")
        assertTrue(saved.captured.contentPreview.contains("小店访客网络"))
        assertTrue(saved.captured.contentPreview.contains("Guest"))
        assertFalse(saved.captured.contentPreview.contains("never-in-preview"))
        assertEquals(config, repository.executeRestoreConfig(saved.captured))
    }
}
