package com.example.qrgenerator.data

import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.data.model.QrPixelShapeType
import com.example.qrgenerator.domain.QrConfigHolderImpl
import com.example.qrgenerator.domain.QrContentCodec
import com.example.qrgenerator.domain.QrImportPolicy
import com.example.qrgenerator.domain.QrPayloadParser
import com.example.qrgenerator.util.QrConfigSerializer
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class QrSourcePayloadCleanupTest {
    private val rawPayload: String = " WIFI:S:Cafe;P:old-private-password;T:WPA;H:false;;\n"

    @Test
    fun `编辑内容时共享配置移除旧来源且编码只包含新密码`(): Unit {
        val holder: QrConfigHolderImpl = QrConfigHolderImpl()
        holder.executeSetConfig(importedConfig())

        holder.executeUpdateConfig { it.copy(content = changedForm(it)) }

        val actual: QrConfig = holder.config.value
        assertNull(actual.sourcePayload)
        assertTrue(QrContentCodec.encode(actual).encode().contains("new-private-password"))
        assertFalse(QrContentCodec.encode(actual).encode().contains("old-private-password"))
    }

    @Test
    fun `仅切换内容类型也会移除不再适用的来源`(): Unit {
        val holder: QrConfigHolderImpl = QrConfigHolderImpl()
        val original: QrConfig = importedConfig()
        holder.executeSetConfig(original)

        holder.executeUpdateConfig { it.copy(contentType = QrContentType.TEXT) }

        assertEquals(original.content, holder.config.value.content)
        assertNull(holder.config.value.sourcePayload)
    }

    @Test
    fun `完整设置时清理失配来源但原草稿快照仍可恢复`(): Unit {
        val holder: QrConfigHolderImpl = QrConfigHolderImpl()
        val snapshot: QrConfig = importedConfig()
        val edited: QrConfig = snapshot.copy(content = changedForm(snapshot))

        holder.executeSetConfig(edited)

        assertNull(holder.config.value.sourcePayload)
        assertEquals(rawPayload, snapshot.sourcePayload?.rawPayload)
        holder.executeSetConfig(snapshot)
        assertEquals(snapshot, holder.config.value)
        assertEquals(rawPayload, QrContentCodec.encode(holder.config.value).encode())
    }

    @Test
    fun `样式和卡面更新仍保留完全相同的扫码原文`(): Unit {
        val holder: QrConfigHolderImpl = QrConfigHolderImpl()
        val original: QrConfig = importedConfig()
        holder.executeSetConfig(original)

        holder.executeUpdateConfig {
            it.copy(darkPixelShape = QrPixelShapeType.CIRCLE, imageWidth = 768,
                card = it.card.copy(title = "新的标题", subtitle = "新的说明", enabled = false))
        }

        assertSame(original.sourcePayload, holder.config.value.sourcePayload)
        assertEquals(rawPayload, QrContentCodec.encode(holder.config.value).encode())
    }

    @Test
    fun `序列化已编辑表单不保存无关旧密码且不修改原对象`(): Unit {
        val original: QrConfig = importedConfig()
        val edited: QrConfig = original.copy(content = changedForm(original))

        val json: String = QrConfigSerializer.serialize(edited)

        assertFalse(json.contains("old-private-password"))
        assertTrue(json.contains("new-private-password"))
        assertNull(QrConfigSerializer.deserialize(json).sourcePayload)
        assertEquals(original.sourcePayload, edited.sourcePayload)
    }

    @Test
    fun `恢复旧版本持久化的失配来源时主动清理`(): Unit {
        val original: QrConfig = importedConfig()
        val staleConfigs: List<QrConfig> = listOf(
            original.copy(content = changedForm(original)),
            original.copy(contentType = QrContentType.TEXT)
        )

        staleConfigs.forEach { stale ->
            val oldJson: String = Gson().toJson(stale)
            val restored: QrConfig = QrConfigSerializer.deserialize(oldJson)
            assertNull(restored.sourcePayload)
            assertEquals(stale.content, restored.content)
            assertEquals(stale.contentType, restored.contentType)
        }
        val restoredEdited: QrConfig = QrConfigSerializer.deserialize(Gson().toJson(staleConfigs.first()))
        assertFalse(QrConfigSerializer.serialize(restoredEdited).contains("old-private-password"))
    }

    @Test
    fun `匹配的扫码来源经过保存恢复仍逐字编码`(): Unit {
        val original: QrConfig = importedConfig()

        val restored: QrConfig = QrConfigSerializer.deserialize(QrConfigSerializer.serialize(original))

        assertEquals(original, restored)
        assertEquals(rawPayload, QrContentCodec.encode(restored).encode())
    }

    @Test
    fun `没有来源字段的旧记录保持兼容`(): Unit {
        val oldJson: String = """{"contentType":"TEXT","content":"旧二维码"}"""

        val restored: QrConfig = QrConfigSerializer.deserialize(oldJson)

        assertEquals("旧二维码", restored.content)
        assertEquals(QrContentType.TEXT, restored.contentType)
        assertNull(restored.sourcePayload)
    }

    @Test
    fun `完整恢复相同配置仍增加可观察的替换次数`(): Unit {
        val holder: QrConfigHolderImpl = QrConfigHolderImpl()
        val original: QrConfig = importedConfig()

        holder.executeSetConfig(original)
        holder.executeSetConfig(original)

        assertEquals(2L, holder.replacementChanges.value)
        assertEquals(holder.replacementChanges.value, holder.replacementRevision)
        assertEquals(original, holder.config.value)
    }

    private fun importedConfig(): QrConfig = QrImportPolicy.createConfig(QrConfig(), QrPayloadParser.parse(rawPayload))

    private fun changedForm(config: QrConfig): String = QrContentCodec.serialize(
        QrContentCodec.fields(config.contentType, config.content) + ("password" to "new-private-password")
    )
}
