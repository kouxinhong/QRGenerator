package com.example.qrgenerator.data

import com.example.qrgenerator.data.local.QrHistoryDao
import com.example.qrgenerator.data.local.QrHistoryEntity
import com.example.qrgenerator.data.repository.QrHistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

class HistoryThumbnailCleanupTest {
    @get:Rule val temporary = TemporaryFolder()
    private val dao = mockk<QrHistoryDao>(relaxed = true)

    @Test fun deletingRecordRemovesOnlyItsUnreferencedPrivateThumbnail() = runBlocking {
        val directory = temporary.newFolder("qr_history")
        val thumbnail = File(directory, "own.png").apply { writeText("image") }
        val keep = File(directory, "keep.png").apply { writeText("image") }
        val record = record(1, thumbnail.path)
        coEvery { dao.getAllHistorySnapshot() } returns listOf(record(2, keep.path))
        QrHistoryRepository(dao, directory).executeDelete(record)
        coVerify(exactly = 1) { dao.deleteRecords(listOf(record)) }
        assertFalse(thumbnail.exists())
        assertTrue(keep.exists())
    }

    @Test fun sharedThumbnailStaysUntilItsLastReferenceIsDeletedIncludingPathAliases() = runBlocking {
        val directory = temporary.newFolder("qr_history")
        val thumbnail = File(directory, "shared.png").apply { writeText("image") }
        val first = record(1, thumbnail.path)
        val second = record(2, File(directory, "./shared.png").path)
        val repository = QrHistoryRepository(dao, directory)
        coEvery { dao.getAllHistorySnapshot() } returns listOf(second)
        repository.executeDelete(first)
        assertTrue(thumbnail.exists())
        coEvery { dao.getAllHistorySnapshot() } returns emptyList()
        repository.executeDelete(second)
        assertFalse(thumbnail.exists())
    }

    @Test fun externalTraversalAndSymlinkPathsCannotDeleteFilesOutsideHistoryDirectory() = runBlocking {
        val directory = temporary.newFolder("qr_history")
        val outside = temporary.newFile("outside.png").apply { writeText("keep") }
        val link = File(directory, "outside_link.png")
        Files.createSymbolicLink(link.toPath(), outside.toPath())
        coEvery { dao.getAllHistorySnapshot() } returns emptyList()
        val records = listOf(record(1, outside.path), record(2, File(directory, "../outside.png").path), record(3, link.path))
        QrHistoryRepository(dao, directory).executeDeleteRecords(records)
        assertTrue(outside.exists())
        assertEquals("keep", outside.readText())
        assertTrue(Files.isSymbolicLink(link.toPath()))
    }

    @Test fun databaseFailureNeverDeletesThumbnail() = runBlocking {
        val directory = temporary.newFolder("qr_history")
        val thumbnail = File(directory, "own.png").apply { writeText("image") }
        coEvery { dao.deleteRecords(any()) } throws IllegalStateException("Database write failed")
        val result = runCatching { QrHistoryRepository(dao, directory).executeDelete(record(1, thumbnail.path)) }
        assertTrue(result.isFailure)
        assertTrue(thumbnail.exists())
        coVerify(exactly = 0) { dao.getAllHistorySnapshot() }
    }

    @Test fun clearAllCleansDeletedPrivateFilesAndPreservesExternalFiles() = runBlocking {
        val directory = temporary.newFolder("qr_history")
        val own = File(directory, "own.png").apply { writeText("image") }
        val outside = temporary.newFile("outside.png").apply { writeText("keep") }
        coEvery { dao.deleteAllReturningRecords() } returns listOf(record(1, own.path), record(2, outside.path))
        coEvery { dao.getAllHistorySnapshot() } returns emptyList()
        QrHistoryRepository(dao, directory).executeDeleteAll()
        assertFalse(own.exists())
        assertTrue(outside.exists())
    }

    @Test fun absentAuthorizedDirectoryNeverRemovesFiles() = runBlocking {
        val thumbnail = temporary.newFile("image.png")
        QrHistoryRepository(dao).executeDelete(record(1, thumbnail.path))
        assertTrue(thumbnail.exists())
    }

    private fun record(id: Long, thumbnail: String): QrHistoryEntity = QrHistoryEntity(id, "{}", thumbnail, "TEXT", "二维码")
}
