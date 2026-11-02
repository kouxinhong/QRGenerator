package com.example.qrgenerator.data.repository

import com.example.qrgenerator.data.local.QrHistoryDao
import com.example.qrgenerator.data.local.QrHistoryEntity
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.domain.QrContentCodec
import com.example.qrgenerator.util.QrConfigSerializer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class QrHistoryRepository(
    private val dao: QrHistoryDao,
    private val historyDirectory: File? = null
) {
    fun executeGetAll(): Flow<List<QrHistoryEntity>> = dao.getAllHistory()
    fun executeGetByType(type: String): Flow<List<QrHistoryEntity>> = dao.getHistoryByType(type)
    fun executeSearch(query: String): Flow<List<QrHistoryEntity>> = dao.searchHistory(query)

    suspend fun executeSave(config: QrConfig, thumbnailPath: String): Long {
        val entity = QrHistoryEntity(
            configJson = QrConfigSerializer.serialize(config),
            thumbnailPath = thumbnailPath,
            contentType = config.contentType.name,
            contentPreview = buildPreview(config)
        )
        return dao.insert(entity)
    }

    suspend fun executeDelete(entity: QrHistoryEntity) {
        executeDeleteRecords(listOf(entity))
    }

    suspend fun executeDeleteRecords(entities: List<QrHistoryEntity>) {
        if (entities.isEmpty()) return
        dao.deleteRecords(entities)
        removeUnreferencedThumbnails(entities)
    }

    suspend fun executeDeleteAll() {
        val deleted = dao.deleteAllReturningRecords()
        removeUnreferencedThumbnails(deleted)
    }

    fun executeRestoreConfig(entity: QrHistoryEntity): QrConfig = QrConfigSerializer.deserialize(entity.configJson)

    private fun buildPreview(config: QrConfig): String {
        val content: String = QrContentCodec.preview(config.contentType, config.content)
        if (!config.card.enabled) return content
        val title: String = config.card.title.ifBlank { config.card.scene.defaultTitle }.replace('\n', ' ').take(24)
        return listOf(title, content).filter { it.isNotBlank() }.joinToString(" · ")
    }

    /** 记录已删除后，缩略图清理失败不应把删除结果误报成失败。 */
    private suspend fun removeUnreferencedThumbnails(records: List<QrHistoryEntity>) {
        val directory = historyDirectory ?: return
        withContext(Dispatchers.IO) {
            try {
                val root = directory.canonicalFile
                val referencedPaths = dao.getAllHistorySnapshot().map { File(it.thumbnailPath).canonicalPath }.toSet()
                records.map { it.thumbnailPath }.distinct().forEach { path ->
                    val file = File(path)
                    val canonical = file.canonicalFile
                    if (canonical.parentFile == root && file.isFile && canonical.path !in referencedPaths) file.delete()
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // 文件系统暂时不可用时保留孤立缩略图，不触碰目录外文件。
            }
        }
    }
}
