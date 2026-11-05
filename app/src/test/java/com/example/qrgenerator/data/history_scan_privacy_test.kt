package com.example.qrgenerator.data

import com.example.qrgenerator.data.local.QrHistoryDao
import com.example.qrgenerator.data.local.QrHistoryEntity
import com.example.qrgenerator.data.model.QrCardConfig
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.data.model.QrScene
import com.example.qrgenerator.data.repository.QrHistoryRepository
import com.example.qrgenerator.util.QrConfigSerializer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryScanPrivacyTest {
    @Test
    fun `新保存的文字型WiFi码只记录网络摘要`() = runTest {
        val dao: QrHistoryDao = mockk()
        val captured = slot<QrHistoryEntity>()
        coEvery { dao.insert(capture(captured)) } returns 1L
        val config: QrConfig = QrConfig(content = PAYLOAD, card = QrCardConfig(QrScene.FREE, true, "访客网络", "扫码连接"))
        val repository: QrHistoryRepository = QrHistoryRepository(dao)
        repository.executeSave(config, "/tmp/test-qr.png")
        assertEquals("访客网络 · Wi-Fi：Cafe", captured.captured.contentPreview)
        assertFalse(captured.captured.contentPreview.contains(PASSWORD))
        assertEquals(PAYLOAD, repository.executeRestoreConfig(captured.captured).content)
    }

    @Test
    fun `旧记录列表及按类型读取会隐藏曾经落库的密码`() = runTest {
        val original: QrHistoryEntity = record(QrConfigSerializer.serialize(QrConfig(content = PAYLOAD)))
        val repository: QrHistoryRepository = createRepository(listOf(original))
        val actual: QrHistoryEntity = repository.executeGetAll().first().single()
        assertEquals("Wi-Fi：Cafe", actual.contentPreview)
        assertFalse(actual.contentPreview.contains(PASSWORD))
        assertEquals(original.configJson, actual.configJson)
        assertEquals(original.id, actual.id)
        assertEquals(actual, repository.executeGetByType(QrContentType.TEXT.name).first().single())
    }

    @Test
    fun `搜索匹配可见摘要而不会通过密码定位旧记录`() = runTest {
        val record: QrHistoryEntity = record(QrConfigSerializer.serialize(QrConfig(content = PAYLOAD)))
        val repository: QrHistoryRepository = createRepository(listOf(record))
        assertTrue(repository.executeSearch(PASSWORD).first().isEmpty())
        assertTrue(repository.executeSearch("P:secret").first().isEmpty())
        assertEquals(listOf(record.id), repository.executeSearch("cafe").first().map { it.id })
        assertEquals("Wi-Fi：Cafe", repository.executeSearch("Wi-Fi").first().single().contentPreview)
    }

    @Test
    fun `损坏JSON及空字段记录的旧WiFi摘要仍保守隐藏`() = runTest {
        val records: List<QrHistoryEntity> = listOf(
            record("{broken", id = 1L),
            record("""{"contentType":null,"content":null}""", id = 2L),
            record("null", id = 3L)
        )
        val repository: QrHistoryRepository = createRepository(records)
        assertEquals(List(records.size) { "Wi-Fi 二维码" }, repository.executeGetAll().first().map { it.contentPreview })
        assertTrue(repository.executeSearch(PASSWORD).first().isEmpty())
        assertEquals(records.size, repository.executeSearch("Wi-Fi").first().size)
    }

    @Test
    fun `读取不会把普通历史摘要误改成WiFi或丢掉标题`() = runTest {
        val note: QrHistoryEntity = record(QrConfigSerializer.serialize(QrConfig(content = "营业时间"))).copy(contentPreview = "今日营业 · 营业时间")
        val repository: QrHistoryRepository = createRepository(listOf(note))
        assertEquals(note, repository.executeGetAll().first().single())
        assertEquals(listOf(note), repository.executeSearch("今日营业").first())
    }

    private fun createRepository(records: List<QrHistoryEntity>): QrHistoryRepository {
        val dao: QrHistoryDao = mockk()
        every { dao.getAllHistory() } returns flowOf(records)
        return QrHistoryRepository(dao)
    }

    private fun record(configJson: String, id: Long = 1L): QrHistoryEntity = QrHistoryEntity(
        id = id, configJson = configJson, thumbnailPath = "/tmp/test-qr.png",
        contentType = QrContentType.TEXT.name, contentPreview = "旧卡片 · $PAYLOAD"
    )

    private companion object {
        const val PASSWORD: String = "secret123"
        const val PAYLOAD: String = "WIFI:T:WPA;S:Cafe;P:secret123;;"
    }
}
