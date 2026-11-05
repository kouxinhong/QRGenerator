plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.qrgenerator"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.qrgenerator"
        minSdk = 26
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables { useSupportLibrary = true }
        ndk { abiFilters += setOf("armeabi-v7a", "arm64-v8a") }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.5" }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
        // AGP 8.2 使用压缩 SO，避免 16 KB 设备直接映射未对齐的 APK 条目。
        jniLibs { useLegacyPackaging = true }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Activity & Lifecycle
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Core
    implementation("androidx.core:core-ktx:1.12.0")

    // QR Code Generator
    implementation("com.github.alexzhirkevich:custom-qr-generator:1.6.2")
    implementation("com.google.zxing:core:3.5.1")

    // 微信二维码识别：2.4.0 支持 compileSdk 34，原生库仅打包 ARM。
    val weChatQrCodeVersion = "2.4.0"
    implementation("com.github.jenly1314.WeChatQRCode:opencv:$weChatQrCodeVersion")
    implementation("com.github.jenly1314.WeChatQRCode:opencv-armv7a:$weChatQrCodeVersion")
    implementation("com.github.jenly1314.WeChatQRCode:opencv-armv64:$weChatQrCodeVersion")
    implementation("com.github.jenly1314.WeChatQRCode:wechat-qrcode:$weChatQrCodeVersion")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")

    // Koin (依赖注入)
    implementation("io.insert-koin:koin-android:3.5.0")
    implementation("io.insert-koin:koin-androidx-compose:3.5.0")

    // Room (数据库)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore (偏好设置)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // JSON 序列化
    implementation("com.google.code.gson:gson:2.10.1")

    // Coil (图片加载)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.zxing:core:3.5.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.insert-koin:koin-test:3.5.0")
    testImplementation("io.mockk:mockk:1.13.8")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("com.google.zxing:core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
