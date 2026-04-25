package com.example.qrgenerator

import android.app.Application
import com.example.qrgenerator.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/** 应用程序入口 */
class QrGeneratorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@QrGeneratorApp)
            modules(appModule)
        }
    }
}
