package com.example.qrgenerator.data.repository

import com.example.qrgenerator.data.local.QrHistoryDao
import com.example.qrgenerator.data.local.QrHistoryEntity
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.util.QrConfigSerializer
import kotlinx.coroutines.flow.Flow

class QrHistoryRepository(private val dao: QrHistoryDao) {
    fun executeGetAll(): Flow<List<QrHistoryEntity>> = dao.getAllHistory()
    fun executeGetByType(type: String): Flow<List<QrHistoryEntity>> = dao.getHistoryByType(type)
    fun executeSearch(query: String): Flow<List<QrHistoryEntity>> = dao.searchHistory(query)

    suspend fun executeSave(config: QrConfig, thumbnailPath: String): Long {
        val entity = QrHistoryEntity(
            configJson = QrConfigSerializer.serialize(config),
            thumbnailPath = thumbnailPath,
            contentType = config.contentType.name,
            contentPreview = config.content.take(50)
        )
        return dao.insert(entity)
    }

    suspend fun executeDelete(entity: QrHistoryEntity) = dao.delete(entity)
    suspend fun executeDeleteAll() = dao.deleteAll()
    fun executeRestoreConfig(entity: QrHistoryEntity): QrConfig {
        return QrConfigSerializer.deserialize(entity.configJson)
    }
}
