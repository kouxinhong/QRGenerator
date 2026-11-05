package com.example.qrgenerator.di

import com.example.qrgenerator.data.local.QrHistoryDatabase
import com.example.qrgenerator.data.local.SettingsDataStore
import com.example.qrgenerator.data.repository.QrHistoryRepository
import com.example.qrgenerator.data.repository.SettingsRepository
import com.example.qrgenerator.domain.ContentValidator
import com.example.qrgenerator.domain.ExportUseCase
import com.example.qrgenerator.domain.QrConfigHolder
import com.example.qrgenerator.domain.QrConfigHolderImpl
import com.example.qrgenerator.domain.QrGeneratorUseCase
import com.example.qrgenerator.domain.QrCardRenderer
import com.example.qrgenerator.domain.QrReadabilityChecker
import com.example.qrgenerator.domain.QrScanner
import com.example.qrgenerator.data.scanner.WeChatQrScanner
import com.example.qrgenerator.ui.scanner.ScanViewModel
import com.example.qrgenerator.ui.screens.generate.GenerateViewModel
import com.example.qrgenerator.ui.screens.history.HistoryViewModel
import com.example.qrgenerator.ui.screens.settings.SettingsViewModel
import com.example.qrgenerator.ui.screens.style.StyleViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/** Koin 依赖注入模块 */
val appModule = module {
    // 数据库
    single { QrHistoryDatabase.create(androidContext()) }
    single { get<QrHistoryDatabase>().qrHistoryDao() }

    // DataStore
    single { SettingsDataStore(androidContext()) }

    // 仓库
    single { QrHistoryRepository(get(), java.io.File(androidContext().filesDir, "qr_history")) }
    single { SettingsRepository(get()) }

    // UseCase
    single { QrGeneratorUseCase(androidContext()) }
    single { ExportUseCase(androidContext()) }
    single { ContentValidator() }
    single { QrCardRenderer() }
    single { QrReadabilityChecker() }
    single<QrScanner> { WeChatQrScanner(androidContext()) }

    // 共享状态
    single<QrConfigHolder> { QrConfigHolderImpl() }

    // ViewModel
    viewModel { GenerateViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { StyleViewModel(get(), get()) }
    viewModel { HistoryViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { ScanViewModel(get(), get()) }
}
