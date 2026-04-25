package com.example.qrgenerator.di

import com.example.qrgenerator.data.local.QrHistoryDatabase
import com.example.qrgenerator.data.local.SettingsDataStore
import com.example.qrgenerator.data.repository.QrHistoryRepository
import com.example.qrgenerator.data.repository.SettingsRepository
import com.example.qrgenerator.domain.QrGeneratorUseCase
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
    single { QrHistoryRepository(get()) }
    single { SettingsRepository(get()) }

    // UseCase
    single { QrGeneratorUseCase() }

    // ViewModel
    viewModel { GenerateViewModel(get()) }
    viewModel { StyleViewModel() }
    viewModel { HistoryViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
}
