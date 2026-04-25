package com.example.qrgenerator.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [QrHistoryEntity::class], version = 1, exportSchema = false)
abstract class QrHistoryDatabase : RoomDatabase() {
    abstract fun qrHistoryDao(): QrHistoryDao
    companion object {
        private const val DATABASE_NAME = "qr_history.db"
        fun create(context: Context): QrHistoryDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                QrHistoryDatabase::class.java,
                DATABASE_NAME
            ).build()
        }
    }
}
