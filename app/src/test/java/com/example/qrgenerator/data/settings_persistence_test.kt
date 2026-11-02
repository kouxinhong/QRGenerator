package com.example.qrgenerator.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import com.example.qrgenerator.data.local.SettingsDataStore
import com.example.qrgenerator.data.model.*
import com.example.qrgenerator.data.repository.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** 使用真实临时 preferences 文件，验证磁盘重读与原子重置，不依赖模拟仓库。 */
class SettingsPersistenceTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `设置写入后关闭存储再打开仍保留并且恢复默认只发出完整快照`() = runBlocking {
        withTimeout(15_000) {
            val file: File = File(temporaryFolder.root, "settings.preferences_pb")
            val firstScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val expected: AppSettings = AppSettings(ThemeMode.DARK, QrErrorLevel.HIGH, ExportFormat.PDF, 2048)
            try {
                val repository: SettingsRepository = createRepository(file, firstScope)
                repository.executeUpdateThemeMode(ThemeMode.DARK)
                repository.executeUpdateErrorLevel(QrErrorLevel.HIGH)
                repository.executeUpdateExportFormat(ExportFormat.PDF)
                repository.executeUpdateImageSize(2048)
                assertEquals(expected, repository.executeGetSettings().first())
            } finally {
                firstScope.coroutineContext[Job]!!.cancelAndJoin()
            }
            val secondScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val repository: SettingsRepository = createRepository(file, secondScope)
                assertEquals(expected, repository.executeGetSettings().first())
                val firstEmission: CompletableDeferred<Unit> = CompletableDeferred()
                val snapshots: Deferred<List<AppSettings>> = async {
                    repository.executeGetSettings().onEach { firstEmission.complete(Unit) }.take(2).toList()
                }
                firstEmission.await()
                repository.executeResetSettings()
                assertEquals(listOf(expected, AppSettings()), snapshots.await())
                assertEquals(AppSettings(), repository.executeGetSettings().first())
            } finally {
                secondScope.coroutineContext[Job]!!.cancelAndJoin()
            }
        }
    }

    @Test
    fun `无效历史枚举与不支持尺寸读取时回到界面可选默认值`() = runBlocking {
        withTimeout(15_000) {
            val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) {
                    File(temporaryFolder.root, "legacy.preferences_pb")
                }
                store.edit {
                    it[SettingsDataStore.KEY_THEME_MODE] = "UNKNOWN"
                    it[SettingsDataStore.KEY_ERROR_LEVEL] = "UNKNOWN"
                    it[SettingsDataStore.KEY_EXPORT_FORMAT] = "SVG"
                    it[SettingsDataStore.KEY_IMAGE_SIZE] = 768
                }
                val repository: SettingsRepository = SettingsRepository(SettingsDataStore(store))
                assertEquals(AppSettings(), repository.executeGetSettings().first())
                assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.executeUpdateImageSize(768) } }
                assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.executeUpdateExportFormat(ExportFormat.SVG) } }
                assertEquals(AppSettings(), repository.executeGetSettings().first())
            } finally {
                scope.coroutineContext[Job]!!.cancelAndJoin()
            }
        }
    }

    private fun createRepository(file: File, scope: CoroutineScope): SettingsRepository =
        SettingsRepository(SettingsDataStore(PreferenceDataStoreFactory.create(scope = scope) { file }))
}
