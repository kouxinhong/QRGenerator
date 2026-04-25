package com.example.qrgenerator.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qr_history")
data class QrHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val configJson: String,
    val thumbnailPath: String,
    val contentType: String,
    val contentPreview: String,
    val createdAt: Long = System.currentTimeMillis()
)
