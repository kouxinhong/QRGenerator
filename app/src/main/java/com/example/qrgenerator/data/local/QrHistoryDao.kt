package com.example.qrgenerator.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface QrHistoryDao {
    @Query("SELECT * FROM qr_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<QrHistoryEntity>>

    @Query("SELECT * FROM qr_history ORDER BY createdAt DESC")
    suspend fun getAllHistorySnapshot(): List<QrHistoryEntity>

    @Query("SELECT * FROM qr_history WHERE contentType = :type ORDER BY createdAt DESC")
    fun getHistoryByType(type: String): Flow<List<QrHistoryEntity>>

    @Query("SELECT * FROM qr_history WHERE contentPreview LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchHistory(query: String): Flow<List<QrHistoryEntity>>

    @Insert
    suspend fun insert(entity: QrHistoryEntity): Long

    @Delete
    suspend fun delete(entity: QrHistoryEntity)

    @Delete
    suspend fun deleteRecords(entities: List<QrHistoryEntity>)

    @Query("DELETE FROM qr_history")
    suspend fun deleteAll()

    @Transaction
    suspend fun deleteAllReturningRecords(): List<QrHistoryEntity> {
        val records = getAllHistorySnapshot()
        deleteAll()
        return records
    }

    @Query("SELECT COUNT(*) FROM qr_history")
    fun getCount(): Flow<Int>
}
