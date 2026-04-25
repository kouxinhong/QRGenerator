# QR 二维码生成器实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个基于 custom-qr-generator 的 Android 二维码生成器 App，支持全功能自定义、历史记录、导出分享。

**Architecture:** Clean Architecture + MVI，UI 层使用 Jetpack Compose，Domain 层包含 UseCase，Data 层使用 Room + DataStore。依赖注入使用 Koin。

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Navigation Compose, Koin, Room, DataStore, custom-qr-generator v1.6.2

---

## 文件结构

```
app/src/main/java/com/example/qrgenerator/
├── QrGeneratorApp.kt                    # Application，Koin 初始化
├── MainActivity.kt                       # 主 Activity，承载 Compose
├── data/
│   ├── model/
│   │   ├── QrConfig.kt                  # QR 码配置数据类
│   │   ├── QrContentType.kt            # 内容类型枚举（15 种）
│   │   ├── QrEnums.kt                  # 所有枚举（形状、颜色、容错等）
│   │   └── AppSettings.kt              # 用户偏好设置数据类
│   ├── local/
│   │   ├── QrHistoryEntity.kt          # Room Entity
│   │   ├── QrHistoryDao.kt             # Room DAO
│   │   ├── QrHistoryDatabase.kt        # Room Database
│   │   ├── Converters.kt              # TypeConverters
│   │   └── SettingsDataStore.kt        # DataStore 封装
│   └── repository/
│       ├── QrHistoryRepository.kt       # 历史记录仓库
│       └── SettingsRepository.kt        # 设置仓库
├── domain/
│   ├── QrGeneratorUseCase.kt           # QR 生成用例
│   ├── ExportUseCase.kt                # 导出用例（PNG/SVG/PDF）
│   └── CustomShapeRegistry.kt          # 自定义形状注册中心
├── di/
│   └── AppModule.kt                    # Koin 模块定义
├── ui/
│   ├── theme/
│   │   ├── Color.kt                    # MIMO 黑白灰配色
│   │   ├── Theme.kt                    # Material 3 主题
│   │   └── Type.kt                     # 字体样式
│   ├── navigation/
│   │   ├── NavGraph.kt                 # 导航图
│   │   └── BottomNavBar.kt             # 底部导航栏
│   ├── components/
│   │   ├── QrCodeDisplay.kt           # QR 码预览组件
│   │   ├── ContentTypeChipGroup.kt     # 内容类型 chip 组
│   │   ├── ShapeChipGroup.kt           # 形状选择 chip 组
│   │   ├── ColorGridPicker.kt          # 颜色网格选择器
│   │   ├── SliderSetting.kt            # 滑块设置项
│   │   ├── SwitchSetting.kt            # 开关设置项
│   │   ├── ErrorDialog.kt              # 统一错误弹窗
│   │   └── ExportDialog.kt             # 导出选项弹窗
│   ├── screens/
│   │   ├── generate/
│   │   │   ├── GenerateScreen.kt       # 生成页面
│   │   │   └── GenerateViewModel.kt    # 生成 ViewModel
│   │   ├── style/
│   │   │   ├── StyleScreen.kt          # 样式页面
│   │   │   └── StyleViewModel.kt       # 样式 ViewModel
│   │   ├── history/
│   │   │   ├── HistoryScreen.kt        # 历史页面
│   │   │   └── HistoryViewModel.kt     # 历史 ViewModel
│   │   └── settings/
│   │       ├── SettingsScreen.kt       # 设置页面
│   │       └── SettingsViewModel.kt    # 设置 ViewModel
│   └── shapes/
│       ├── HoneycombShape.kt           # 蜂巢六边形
│       └── CatPawShape.kt              # 猫爪形状
└── util/
    └── QrConfigSerializer.kt           # QrConfig JSON 序列化

app/src/test/java/com/example/qrgenerator/
├── domain/
│   └── QrGeneratorUseCaseTest.kt
├── data/
│   └── QrConfigTest.kt
└── ui/
    └── screens/
        ├── generate/
        │   └── GenerateViewModelTest.kt
        └── settings/
            └── SettingsViewModelTest.kt
```

---

## Task 1: 更新项目依赖

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: 更新根 build.gradle.kts**

```kotlin
// build.gradle.kts
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
}
```

- [ ] **Step 2: 更新 app/build.gradle.kts**

```kotlin
// app/build.gradle.kts
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
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.5" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
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

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.insert-koin:koin-test:3.5.0")
    testImplementation("io.mockk:mockk:1.13.8")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

- [ ] **Step 3: 更新 settings.gradle.kts 添加 KSP 插件仓库**

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "QRGenerator"
include(":app")
```

- [ ] **Step 4: 验证项目同步**

Run: `./gradlew --no-daemon projects`
Expected: BUILD SUCCESSFUL

---

## Task 2: 数据模型 — 枚举与配置

**Files:**
- Create: `app/src/main/java/com/example/qrgenerator/data/model/QrEnums.kt`
- Create: `app/src/main/java/com/example/qrgenerator/data/model/QrContentType.kt`
- Create: `app/src/main/java/com/example/qrgenerator/data/model/QrConfig.kt`
- Create: `app/src/main/java/com/example/qrgenerator/data/model/AppSettings.kt`
- Create: `app/src/test/java/com/example/qrgenerator/data/QrConfigTest.kt`

- [ ] **Step 1: 创建 QrEnums.kt**

```kotlin
package com.example.qrgenerator.data.model

/** 容错级别 */
enum class QrErrorLevel(val displayName: String) {
    AUTO("自动"),
    LOW("低 (7%)"),
    MEDIUM("中 (15%)"),
    MEDIUM_HIGH("较高 (25%)"),
    HIGH("高 (30%)");
}

/** 模块形状类型 */
enum class QrPixelShapeType(val displayName: String) {
    DEFAULT("默认方形"),
    CIRCLE("圆形"),
    ROUND_CORNERS("圆角方形"),
    RHOMBUS("菱形"),
    STAR("星形"),
    ROUND_CORNERS_VERTICAL("纵向圆角"),
    ROUND_CORNERS_HORIZONTAL("横向圆角"),
    HONEYCOMB("蜂巢"),
    CAT_PAW("猫爪");
}

/** 眼睛外框形状类型 */
enum class QrFrameShapeType(val displayName: String) {
    DEFAULT("默认方形"),
    CIRCLE("圆形"),
    ROUND_CORNERS("圆角方形"),
    AS_DARK_PIXELS("跟随模块");
}

/** 眼睛内球形状类型 */
enum class QrBallShapeType(val displayName: String) {
    DEFAULT("默认方形"),
    CIRCLE("圆形"),
    ROUND_CORNERS("圆角方形"),
    RHOMBUS("菱形"),
    AS_DARK_PIXELS("跟随模块");
}

/** 颜色类型 */
enum class QrColorType(val displayName: String) {
    SOLID("纯色"),
    LINEAR_GRADIENT("线性渐变"),
    RADIAL_GRADIENT("径向渐变"),
    SWEEP_GRADIENT("扫描渐变");
}

/** 渐变方向 */
enum class GradientOrientation(val displayName: String) {
    HORIZONTAL("水平"),
    VERTICAL("垂直"),
    LEFT_DIAGONAL("左对角线"),
    RIGHT_DIAGONAL("右对角线");
}

/** Logo 来源 */
enum class LogoSource(val displayName: String) {
    GALLERY("相册"),
    CAMERA("拍照"),
    BUILTIN_ICON("内置图标");
}

/** Logo 形状 */
enum class LogoShapeType(val displayName: String) {
    DEFAULT("默认方形"),
    CIRCLE("圆形"),
    ROUND_CORNERS("圆角方形"),
    RHOMBUS("菱形");
}

/** Logo 内边距类型 */
enum class LogoPaddingType(val displayName: String) {
    EMPTY("无边距"),
    ACCURATE("精确"),
    NATURAL("自然");
}

/** 整体外形 */
enum class CodeShapeType(val displayName: String) {
    DEFAULT("方形"),
    CIRCLE("圆形");
}

/** 高亮模式 */
enum class HighlightingMode(val displayName: String) {
    NONE("关闭"),
    DEFAULT("默认"),
    CUSTOM("自定义");
}

/** 导出格式 */
enum class ExportFormat(val displayName: String, val extension: String) {
    PNG("PNG 图片", "png"),
    SVG("SVG 矢量", "svg"),
    PDF("PDF 文档", "pdf");
}

/** 主题模式 */
enum class ThemeMode(val displayName: String) {
    FOLLOW_SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色");
}
```

- [ ] **Step 2: 创建 QrContentType.kt**

```kotlin
package com.example.qrgenerator.data.model

/** 二维码内容类型 */
enum class QrContentType(val displayName: String) {
    TEXT("文本"),
    URL("网址"),
    WIFI("WiFi"),
    ENTERPRISE_WIFI("企业 WiFi"),
    VCARD("联系人"),
    BIZCARD("名片"),
    MECARD("MeCard"),
    PHONE("电话"),
    SMS("短信"),
    EMAIL("邮件"),
    GEO("位置"),
    BOOKMARK("书签"),
    EVENT("日历"),
    YOUTUBE("YouTube"),
    GOOGLE_PLAY("Google Play");
}
```

- [ ] **Step 3: 创建 QrConfig.kt**

```kotlin
package com.example.qrgenerator.data.model

import android.graphics.Color

/** 颜色配置 */
data class QrColorConfig(
    val type: QrColorType = QrColorType.SOLID,
    val solidColor: Int = Color.BLACK,
    val gradientStartColor: Int = Color.BLACK,
    val gradientEndColor: Int = Color.WHITE,
    val gradientOrientation: GradientOrientation = GradientOrientation.HORIZONTAL,
    val gradientRadius: Float = 1.414f
)

/** Logo 配置 */
data class QrLogoConfig(
    val source: LogoSource = LogoSource.GALLERY,
    val imagePath: String? = null,
    val builtinIconName: String? = null,
    val size: Float = 0.2f,
    val shape: LogoShapeType = LogoShapeType.DEFAULT,
    val cornerRadius: Float = 0f,
    val paddingType: LogoPaddingType = LogoPaddingType.EMPTY,
    val paddingValue: Float = 0f,
    val backgroundColor: QrColorConfig = QrColorConfig()
)

/** 高亮配置 */
data class QrHighlightingConfig(
    val cornerEyes: HighlightingMode = HighlightingMode.NONE,
    val versionEyes: HighlightingMode = HighlightingMode.NONE,
    val timingLines: HighlightingMode = HighlightingMode.NONE,
    val alpha: Float = 0.75f
)

/** QR 码完整配置 */
data class QrConfig(
    val contentType: QrContentType = QrContentType.TEXT,
    val content: String = "",
    val padding: Float = 0.125f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val errorCorrectionLevel: QrErrorLevel = QrErrorLevel.AUTO,
    val darkPixelShape: QrPixelShapeType = QrPixelShapeType.DEFAULT,
    val lightPixelShape: QrPixelShapeType = QrPixelShapeType.DEFAULT,
    val ballShape: QrBallShapeType = QrBallShapeType.DEFAULT,
    val frameShape: QrFrameShapeType = QrFrameShapeType.DEFAULT,
    val centralSymmetry: Boolean = true,
    val darkColor: QrColorConfig = QrColorConfig(),
    val ballColor: QrColorConfig = QrColorConfig(),
    val frameColor: QrColorConfig = QrColorConfig(),
    val codeShape: CodeShapeType = CodeShapeType.DEFAULT,
    val logo: QrLogoConfig = QrLogoConfig(),
    val highlighting: QrHighlightingConfig = QrHighlightingConfig(),
    val fourthEyeEnabled: Boolean = false,
    val imageWidth: Int = 1024,
    val imageHeight: Int = 1024
)
```

- [ ] **Step 4: 创建 AppSettings.kt**

```kotlin
package com.example.qrgenerator.data.model

/** 用户偏好设置 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val defaultErrorLevel: QrErrorLevel = QrErrorLevel.AUTO,
    val defaultExportFormat: ExportFormat = ExportFormat.PNG,
    val defaultImageSize: Int = 1024,
    val hasSeenOnboarding: Boolean = false
)
```

- [ ] **Step 5: 编写 QrConfigTest.kt**

```kotlin
package com.example.qrgenerator.data

import android.graphics.Color
import com.example.qrgenerator.data.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class QrConfigTest {

    @Test
    fun `默认配置应为文本类型`() {
        val config = QrConfig()
        assertEquals(QrContentType.TEXT, config.contentType)
        assertEquals("", config.content)
    }

    @Test
    fun `默认容错级别为 AUTO`() {
        val config = QrConfig()
        assertEquals(QrErrorLevel.AUTO, config.errorCorrectionLevel)
    }

    @Test
    fun `默认颜色为纯黑色`() {
        val config = QrConfig()
        assertEquals(QrColorType.SOLID, config.darkColor.type)
        assertEquals(Color.BLACK, config.darkColor.solidColor)
    }

    @Test
    fun `默认形状为方形`() {
        val config = QrConfig()
        assertEquals(QrPixelShapeType.DEFAULT, config.darkPixelShape)
        assertEquals(QrBallShapeType.DEFAULT, config.ballShape)
        assertEquals(QrFrameShapeType.DEFAULT, config.frameShape)
    }

    @Test
    fun `支持所有 15 种内容类型`() {
        assertEquals(15, QrContentType.entries.size)
    }
}
```

- [ ] **Step 6: 运行测试**

Run: `./gradlew testDebugUnitTest --tests "com.example.qrgenerator.data.QrConfigTest" --no-daemon`
Expected: 全部 PASS

- [ ] **Step 7: 提交**

```bash
git add app/src/main/java/com/example/qrgenerator/data/model/ app/src/test/java/com/example/qrgenerator/data/QrConfigTest.kt
git commit -m "feat: 添加数据模型、枚举和配置数据类"
```

---

## Task 3: 数据层 — Room 数据库

**Files:**
- Create: `app/src/main/java/com/example/qrgenerator/data/local/QrHistoryEntity.kt`
- Create: `app/src/main/java/com/example/qrgenerator/data/local/QrHistoryDao.kt`
- Create: `app/src/main/java/com/example/qrgenerator/data/local/Converters.kt`
- Create: `app/src/main/java/com/example/qrgenerator/data/local/QrHistoryDatabase.kt`
- Create: `app/src/main/java/com/example/qrgenerator/data/repository/QrHistoryRepository.kt`
- Create: `app/src/main/java/com/example/qrgenerator/util/QrConfigSerializer.kt`

- [ ] **Step 1: 创建 QrConfigSerializer.kt**

```kotlin
package com.example.qrgenerator.util

import com.example.qrgenerator.data.model.QrConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder

/** QrConfig JSON 序列化/反序列化工具 */
object QrConfigSerializer {
    private val gson: Gson = GsonBuilder().create()

    /** 将 QrConfig 序列化为 JSON 字符串 */
    fun serialize(config: QrConfig): String = gson.toJson(config)

    /** 将 JSON 字符串反序列化为 QrConfig */
    fun deserialize(json: String): QrConfig = gson.fromJson(json, QrConfig::class.java)
}
```

- [ ] **Step 2: 创建 QrHistoryEntity.kt**

```kotlin
package com.example.qrgenerator.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** QR 码历史记录数据库实体 */
@Entity(tableName = "qr_history")
data class QrHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** QrConfig JSON 序列化字符串 */
    val configJson: String,
    /** 缩略图文件路径 */
    val thumbnailPath: String,
    /** 内容类型名称（用于筛选） */
    val contentType: String,
    /** 内容前 50 字符（用于列表展示） */
    val contentPreview: String,
    /** 创建时间戳 */
    val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 3: 创建 QrHistoryDao.kt**

```kotlin
package com.example.qrgenerator.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** QR 历史记录数据访问对象 */
@Dao
interface QrHistoryDao {
    /** 获取所有历史记录，按创建时间倒序 */
    @Query("SELECT * FROM qr_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<QrHistoryEntity>>

    /** 按内容类型筛选 */
    @Query("SELECT * FROM qr_history WHERE contentType = :type ORDER BY createdAt DESC")
    fun getHistoryByType(type: String): Flow<List<QrHistoryEntity>>

    /** 搜索内容 */
    @Query("SELECT * FROM qr_history WHERE contentPreview LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchHistory(query: String): Flow<List<QrHistoryEntity>>

    /** 插入记录 */
    @Insert
    suspend fun insert(entity: QrHistoryEntity): Long

    /** 删除记录 */
    @Delete
    suspend fun delete(entity: QrHistoryEntity)

    /** 清空所有记录 */
    @Query("DELETE FROM qr_history")
    suspend fun deleteAll()

    /** 获取记录总数 */
    @Query("SELECT COUNT(*) FROM qr_history")
    fun getCount(): Flow<Int>
}
```

- [ ] **Step 4: 创建 Converters.kt**

```kotlin
package com.example.qrgenerator.data.local

import androidx.room.TypeConverter
import java.util.Date

/** Room 类型转换器 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}
```

- [ ] **Step 5: 创建 QrHistoryDatabase.kt**

```kotlin
package com.example.qrgenerator.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/** QR 历史记录数据库 */
@Database(entities = [QrHistoryEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
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
```

- [ ] **Step 6: 创建 QrHistoryRepository.kt**

```kotlin
package com.example.qrgenerator.data.repository

import com.example.qrgenerator.data.local.QrHistoryDao
import com.example.qrgenerator.data.local.QrHistoryEntity
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.util.QrConfigSerializer
import kotlinx.coroutines.flow.Flow

/** QR 历史记录仓库 */
class QrHistoryRepository(private val dao: QrHistoryDao) {

    /** 获取所有历史记录 */
    fun executeGetAll(): Flow<List<QrHistoryEntity>> = dao.getAllHistory()

    /** 按类型筛选 */
    fun executeGetByType(type: String): Flow<List<QrHistoryEntity>> = dao.getHistoryByType(type)

    /** 搜索 */
    fun executeSearch(query: String): Flow<List<QrHistoryEntity>> = dao.searchHistory(query)

    /** 保存一条记录 */
    suspend fun executeSave(config: QrConfig, thumbnailPath: String): Long {
        val entity = QrHistoryEntity(
            configJson = QrConfigSerializer.serialize(config),
            thumbnailPath = thumbnailPath,
            contentType = config.contentType.name,
            contentPreview = config.content.take(50)
        )
        return dao.insert(entity)
    }

    /** 删除一条记录 */
    suspend fun executeDelete(entity: QrHistoryEntity) = dao.delete(entity)

    /** 清空所有记录 */
    suspend fun executeDeleteAll() = dao.deleteAll()

    /** 从记录恢复 QrConfig */
    fun executeRestoreConfig(entity: QrHistoryEntity): QrConfig {
        return QrConfigSerializer.deserialize(entity.configJson)
    }
}
```

- [ ] **Step 7: 提交**

```bash
git add app/src/main/java/com/example/qrgenerator/data/local/ app/src/main/java/com/example/qrgenerator/data/repository/QrHistoryRepository.kt app/src/main/java/com/example/qrgenerator/util/QrConfigSerializer.kt
git commit -m "feat: 添加 Room 数据库、DAO 和历史记录仓库"
```

---

## Task 4: 数据层 — DataStore 设置

**Files:**
- Create: `app/src/main/java/com/example/qrgenerator/data/local/SettingsDataStore.kt`
- Create: `app/src/main/java/com/example/qrgenerator/data/repository/SettingsRepository.kt`

- [ ] **Step 1: 创建 SettingsDataStore.kt**

```kotlin
package com.example.qrgenerator.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** DataStore 实例 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** 设置数据存储 */
class SettingsDataStore(private val context: Context) {
    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_ERROR_LEVEL = stringPreferencesKey("error_level")
        private val KEY_EXPORT_FORMAT = stringPreferencesKey("export_format")
        private val KEY_IMAGE_SIZE = intPreferencesKey("image_size")
        private val KEY_HAS_SEEN_ONBOARDING = stringPreferencesKey("has_seen_onboarding")
    }

    /** 读取主题模式 */
    fun readThemeMode(): Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "FOLLOW_SYSTEM"
    }

    /** 写入主题模式 */
    suspend fun writeThemeMode(value: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = value }
    }

    /** 读取默认容错级别 */
    fun readErrorLevel(): Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ERROR_LEVEL] ?: "AUTO"
    }

    /** 写入默认容错级别 */
    suspend fun writeErrorLevel(value: String) {
        context.dataStore.edit { it[KEY_ERROR_LEVEL] = value }
    }

    /** 读取默认导出格式 */
    fun readExportFormat(): Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_EXPORT_FORMAT] ?: "PNG"
    }

    /** 写入默认导出格式 */
    suspend fun writeExportFormat(value: String) {
        context.dataStore.edit { it[KEY_EXPORT_FORMAT] = value }
    }

    /** 读取默认图片尺寸 */
    fun readImageSize(): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_IMAGE_SIZE] ?: 1024
    }

    /** 写入默认图片尺寸 */
    suspend fun writeImageSize(value: Int) {
        context.dataStore.edit { it[KEY_IMAGE_SIZE] = value }
    }

    /** 读取是否已看过引导 */
    fun readHasSeenOnboarding(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HAS_SEEN_ONBOARDING] == "true"
    }

    /** 写入已看过引导 */
    suspend fun writeHasSeenOnboarding(value: Boolean) {
        context.dataStore.edit { it[KEY_HAS_SEEN_ONBOARDING] = value.toString() }
    }
}
```

- [ ] **Step 2: 创建 SettingsRepository.kt**

```kotlin
package com.example.qrgenerator.data.repository

import com.example.qrgenerator.data.local.SettingsDataStore
import com.example.qrgenerator.data.model.AppSettings
import com.example.qrgenerator.data.model.ExportFormat
import com.example.qrgenerator.data.model.QrErrorLevel
import com.example.qrgenerator.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** 设置仓库 */
class SettingsRepository(private val dataStore: SettingsDataStore) {

    /** 获取完整设置 */
    fun executeGetSettings(): Flow<AppSettings> = combine(
        dataStore.readThemeMode(),
        dataStore.readErrorLevel(),
        dataStore.readExportFormat(),
        dataStore.readImageSize()
    ) { theme, error, format, size ->
        AppSettings(
            themeMode = ThemeMode.valueOf(theme),
            defaultErrorLevel = QrErrorLevel.valueOf(error),
            defaultExportFormat = ExportFormat.valueOf(format),
            defaultImageSize = size
        )
    }

    /** 更新主题模式 */
    suspend fun executeUpdateThemeMode(mode: ThemeMode) {
        dataStore.writeThemeMode(mode.name)
    }

    /** 更新默认容错级别 */
    suspend fun executeUpdateErrorLevel(level: QrErrorLevel) {
        dataStore.writeErrorLevel(level.name)
    }

    /** 更新默认导出格式 */
    suspend fun executeUpdateExportFormat(format: ExportFormat) {
        dataStore.writeExportFormat(format.name)
    }

    /** 更新默认图片尺寸 */
    suspend fun executeUpdateImageSize(size: Int) {
        dataStore.writeImageSize(size)
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/example/qrgenerator/data/local/SettingsDataStore.kt app/src/main/java/com/example/qrgenerator/data/repository/SettingsRepository.kt
git commit -m "feat: 添加 DataStore 设置存储和设置仓库"
```

---

## Task 5: Domain 层 — 自定义形状与注册中心

**Files:**
- Create: `app/src/main/java/com/example/qrgenerator/domain/CustomShapeRegistry.kt`
- Create: `app/src/main/java/com/example/qrgenerator/ui/shapes/HoneycombShape.kt`
- Create: `app/src/main/java/com/example/qrgenerator/ui/shapes/CatPawShape.kt`

- [ ] **Step 1: 创建 HoneycombShape.kt**

```kotlin
package com.example.qrgenerator.ui.shapes

import android.graphics.Path
import com.example.qrgenerator.data.model.QrPixelShapeType
import com.github.alexzhirkevich.customqrgenerator.style.Neighbors
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape

/** 蜂巢形状 — 六边形像素，紧密排列形成蜂巢效果 */
object HoneycombShape : QrVectorPixelShape {
    override fun createPath(size: Float, neighbors: Neighbors): Path {
        val r = size / 2f
        val h = r * 0.866f // sqrt(3)/2
        return Path().apply {
            moveTo(r, 0f)
            lineTo(r + h, r / 2f)
            lineTo(r + h, r * 1.5f)
            lineTo(r, size)
            lineTo(r - h, r * 1.5f)
            lineTo(r - h, r / 2f)
            close()
        }
    }
}
```

- [ ] **Step 2: 创建 CatPawShape.kt**

```kotlin
package com.example.qrgenerator.ui.shapes

import android.graphics.Path
import com.github.alexzhirkevich.customqrgenerator.style.Neighbors
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape

/** 猫爪形状 — 大圆掌 + 三个小圆垫 */
object CatPawShape : QrVectorPixelShape {
    override fun createPath(size: Float, neighbors: Neighbors): Path {
        val palmRadius = size * 0.3f
        val toeRadius = size * 0.12f
        val palmCenterX = size / 2f
        val palmCenterY = size * 0.58f
        return Path().apply {
            addCircle(palmCenterX, palmCenterY, palmRadius, Path.Direction.CW)
            addCircle(size * 0.3f, size * 0.22f, toeRadius, Path.Direction.CW)
            addCircle(size * 0.5f, size * 0.15f, toeRadius, Path.Direction.CW)
            addCircle(size * 0.7f, size * 0.22f, toeRadius, Path.Direction.CW)
        }
    }
}
```

- [ ] **Step 3: 创建 CustomShapeRegistry.kt**

```kotlin
package com.example.qrgenerator.domain

import com.example.qrgenerator.ui.shapes.CatPawShape
import com.example.qrgenerator.ui.shapes.HoneycombShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape

/** 自定义形状注册中心 — 支持运行时扩展形状和颜色 */
object CustomShapeRegistry {
    private val pixelShapes = mutableMapOf<String, QrVectorPixelShape>()
    private val colors = mutableMapOf<String, QrVectorColor>()

    init {
        registerPixelShape("honeycomb", HoneycombShape)
        registerPixelShape("cat_paw", CatPawShape)
    }

    /** 注册自定义像素形状 */
    fun registerPixelShape(name: String, shape: QrVectorPixelShape) {
        pixelShapes[name] = shape
    }

    /** 注册自定义颜色 */
    fun registerColor(name: String, color: QrVectorColor) {
        colors[name] = color
    }

    /** 获取像素形状 */
    fun getPixelShape(name: String): QrVectorPixelShape? = pixelShapes[name]

    /** 获取颜色 */
    fun getColor(name: String): QrVectorColor? = colors[name]

    /** 获取所有注册的像素形状名称 */
    fun getPixelShapeNames(): Set<String> = pixelShapes.keys

    /** 获取所有注册的颜色名称 */
    fun getColorNames(): Set<String> = colors.keys
}
```

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/example/qrgenerator/domain/CustomShapeRegistry.kt app/src/main/java/com/example/qrgenerator/ui/shapes/
git commit -m "feat: 添加自定义形状（蜂巢、猫爪）和注册中心"
```

---

## Task 6: Domain 层 — QR 生成用例

**Files:**
- Create: `app/src/main/java/com/example/qrgenerator/domain/QrGeneratorUseCase.kt`
- Create: `app/src/test/java/com/example/qrgenerator/domain/QrGeneratorUseCaseTest.kt`

- [ ] **Step 1: 创建 QrGeneratorUseCase.kt**

```kotlin
package com.example.qrgenerator.domain

import android.graphics.drawable.Drawable
import com.example.qrgenerator.data.model.*
import com.github.alexzhirkevich.customqrgenerator.QrData
import com.github.alexzhirkevich.customqrgenerator.QrErrorCorrectionLevel
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.createQrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.*

/** QR 码生成用例 */
class QrGeneratorUseCase {

    /** 根据配置生成 QR 码 Drawable */
    fun execute(config: QrConfig): Drawable {
        val qrData = mapQrData(config)
        val options = mapQrOptions(config)
        return QrCodeDrawable(qrData, options)
    }

    /** 将配置映射为 QrData */
    internal fun mapQrData(config: QrConfig): QrData {
        return when (config.contentType) {
            QrContentType.TEXT -> QrData.Text(config.content)
            QrContentType.URL -> QrData.Url(config.content)
            QrContentType.WIFI -> parseWifi(config.content)
            QrContentType.ENTERPRISE_WIFI -> parseEnterpriseWifi(config.content)
            QrContentType.VCARD -> parseVCard(config.content)
            QrContentType.BIZCARD -> parseBizCard(config.content)
            QrContentType.MECARD -> parseMeCard(config.content)
            QrContentType.PHONE -> QrData.Phone(config.content)
            QrContentType.SMS -> QrData.SMS(config.content, "", false)
            QrContentType.EMAIL -> QrData.Email(email = config.content)
            QrContentType.GEO -> parseGeo(config.content)
            QrContentType.BOOKMARK -> QrData.Bookmark(config.content, "书签")
            QrContentType.EVENT -> QrData.Event(summary = config.content)
            QrContentType.YOUTUBE -> QrData.YouTube(config.content)
            QrContentType.GOOGLE_PLAY -> QrData.GooglePlay(config.content)
        }
    }

    /** 将配置映射为 QrVectorOptions */
    private fun mapQrOptions(config: QrConfig) = createQrVectorOptions {
        padding = config.padding
        errorCorrectionLevel = mapErrorLevel(config.errorCorrectionLevel)
        fourthEyeEnabled = config.fourthEyeEnabled

        shapes {
            darkPixel = mapPixelShape(config.darkPixelShape)
            lightPixel = mapPixelShape(config.lightPixelShape)
            ball = mapBallShape(config.ballShape)
            frame = mapFrameShape(config.frameShape)
            centralSymmetry = config.centralSymmetry
        }

        colors {
            dark = mapColor(config.darkColor)
            ball = mapColor(config.ballColor)
            frame = mapColor(config.frameColor)
        }

        if (config.logo.imagePath != null && config.logo.size > 0f) {
            logo {
                size = config.logo.size
                shape = mapLogoShape(config.logo.shape, config.logo.cornerRadius)
                padding = mapLogoPadding(config.logo.paddingType, config.logo.paddingValue)
            }
        }
    }

    private fun mapErrorLevel(level: QrErrorLevel): QrErrorCorrectionLevel = when (level) {
        QrErrorLevel.AUTO -> QrErrorCorrectionLevel.Auto
        QrErrorLevel.LOW -> QrErrorCorrectionLevel.Low
        QrErrorLevel.MEDIUM -> QrErrorCorrectionLevel.Medium
        QrErrorLevel.MEDIUM_HIGH -> QrErrorCorrectionLevel.MediumHigh
        QrErrorLevel.HIGH -> QrErrorCorrectionLevel.High
    }

    private fun mapPixelShape(type: QrPixelShapeType): QrVectorPixelShape = when (type) {
        QrPixelShapeType.DEFAULT -> QrVectorPixelShape.Default
        QrPixelShapeType.CIRCLE -> QrVectorPixelShape.Circle()
        QrPixelShapeType.ROUND_CORNERS -> QrVectorPixelShape.RoundCorners(0.5f)
        QrPixelShapeType.RHOMBUS -> QrVectorPixelShape.Rhombus()
        QrPixelShapeType.STAR -> QrVectorPixelShape.Star
        QrPixelShapeType.ROUND_CORNERS_VERTICAL -> QrVectorPixelShape.RoundCornersVertical()
        QrPixelShapeType.ROUND_CORNERS_HORIZONTAL -> QrVectorPixelShape.RoundCornersHorizontal()
        QrPixelShapeType.HONEYCOMB -> CustomShapeRegistry.getPixelShape("honeycomb") ?: QrVectorPixelShape.Default
        QrPixelShapeType.CAT_PAW -> CustomShapeRegistry.getPixelShape("cat_paw") ?: QrVectorPixelShape.Default
    }

    private fun mapBallShape(type: QrBallShapeType): QrVectorBallShape = when (type) {
        QrBallShapeType.DEFAULT -> QrVectorBallShape.Default
        QrBallShapeType.CIRCLE -> QrVectorBallShape.Circle(1f)
        QrBallShapeType.ROUND_CORNERS -> QrVectorBallShape.RoundCorners(0.25f)
        QrBallShapeType.RHOMBUS -> QrVectorBallShape.Rhombus()
        QrBallShapeType.AS_DARK_PIXELS -> QrVectorBallShape.AsDarkPixels
    }

    private fun mapFrameShape(type: QrFrameShapeType): QrVectorFrameShape = when (type) {
        QrFrameShapeType.DEFAULT -> QrVectorFrameShape.Default
        QrFrameShapeType.CIRCLE -> QrVectorFrameShape.Circle()
        QrFrameShapeType.ROUND_CORNERS -> QrVectorFrameShape.RoundCorners(0.25f)
        QrFrameShapeType.AS_DARK_PIXELS -> QrVectorFrameShape.AsDarkPixels
    }

    private fun mapColor(config: QrColorConfig): QrVectorColor = when (config.type) {
        QrColorType.SOLID -> QrVectorColor.Solid(config.solidColor)
        QrColorType.LINEAR_GRADIENT -> {
            val orientation = when (config.gradientOrientation) {
                GradientOrientation.HORIZONTAL -> QrVectorColor.LinearGradient.Orientation.Horizontal
                GradientOrientation.VERTICAL -> QrVectorColor.LinearGradient.Orientation.Vertical
                GradientOrientation.LEFT_DIAGONAL -> QrVectorColor.LinearGradient.Orientation.LeftDiagonal
                GradientOrientation.RIGHT_DIAGONAL -> QrVectorColor.LinearGradient.Orientation.RightDiagonal
            }
            QrVectorColor.LinearGradient(
                colors = listOf(0f to config.gradientStartColor, 1f to config.gradientEndColor),
                orientation = orientation
            )
        }
        QrColorType.RADIAL_GRADIENT -> QrVectorColor.RadialGradient(
            colors = listOf(0f to config.gradientStartColor, 1f to config.gradientEndColor),
            radius = config.gradientRadius
        )
        QrColorType.SWEEP_GRADIENT -> QrVectorColor.SweepGradient(
            colors = listOf(0f to config.gradientStartColor, 1f to config.gradientEndColor)
        )
    }

    private fun mapLogoShape(type: LogoShapeType, radius: Float): QrVectorLogoShape = when (type) {
        LogoShapeType.DEFAULT -> QrVectorLogoShape.Default
        LogoShapeType.CIRCLE -> QrVectorLogoShape.Circle
        LogoShapeType.ROUND_CORNERS -> QrVectorLogoShape.RoundCorners(radius)
        LogoShapeType.RHOMBUS -> QrVectorLogoShape.Rhombus
    }

    private fun mapLogoPadding(type: LogoPaddingType, value: Float): QrVectorLogoPadding = when (type) {
        LogoPaddingType.EMPTY -> QrVectorLogoPadding.Empty
        LogoPaddingType.ACCURATE -> QrVectorLogoPadding.Accurate(value)
        LogoPaddingType.NATURAL -> QrVectorLogoPadding.Natural(value)
    }

    // 内容解析函数
    private fun parseWifi(content: String): QrData.Wifi {
        val parts = content.split("/")
        return QrData.Wifi(
            authentication = when (parts.getOrNull(2)?.uppercase()) {
                "WEP" -> QrData.Wifi.Authentication.WEP
                "WPA" -> QrData.Wifi.Authentication.WPA
                else -> QrData.Wifi.Authentication.OPEN
            },
            ssid = parts.getOrNull(0),
            psk = parts.getOrNull(1),
            hidden = false
        )
    }

    private fun parseEnterpriseWifi(content: String): QrData.EnterpriseWifi {
        val parts = content.split("/")
        return QrData.EnterpriseWifi(
            ssid = parts.getOrNull(0),
            psk = parts.getOrNull(1),
            user = parts.getOrNull(2),
            eap = parts.getOrNull(3)
        )
    }

    private fun parseVCard(content: String): QrData.VCard {
        val parts = content.split("/")
        return QrData.VCard(
            name = parts.getOrNull(0),
            company = parts.getOrNull(1),
            title = parts.getOrNull(2),
            phoneNumber = parts.getOrNull(3),
            email = parts.getOrNull(4)
        )
    }

    private fun parseBizCard(content: String): QrData.BizCard {
        val parts = content.split("/")
        return QrData.BizCard(
            firstName = parts.getOrNull(0),
            secondName = parts.getOrNull(1),
            job = parts.getOrNull(2),
            company = parts.getOrNull(3),
            phone = parts.getOrNull(4),
            email = parts.getOrNull(5)
        )
    }

    private fun parseMeCard(content: String): QrData.MeCard {
        val parts = content.split("/")
        return QrData.MeCard(
            name = parts.getOrNull(0),
            phoneNumber = parts.getOrNull(1),
            email = parts.getOrNull(2)
        )
    }

    private fun parseGeo(content: String): QrData.GeoPos {
        val parts = content.split(",")
        return QrData.GeoPos(
            lat = parts.getOrNull(0)?.toFloatOrNull() ?: 0f,
            lon = parts.getOrNull(1)?.toFloatOrNull() ?: 0f
        )
    }
}
```

- [ ] **Step 2: 编写 QrGeneratorUseCaseTest.kt**

```kotlin
package com.example.qrgenerator.domain

import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.github.alexzhirkevich.customqrgenerator.QrData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrGeneratorUseCaseTest {

    private val useCase = QrGeneratorUseCase()

    @Test
    fun `文本类型映射为 QrData_Text`() {
        val config = QrConfig(contentType = QrContentType.TEXT, content = "hello")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.Text)
        assertEquals("hello", (data as QrData.Text).value)
    }

    @Test
    fun `URL 类型映射为 QrData_Url`() {
        val config = QrConfig(contentType = QrContentType.URL, content = "https://example.com")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.Url)
    }

    @Test
    fun `WiFi 类型正确解析 SSID 和密码`() {
        val config = QrConfig(contentType = QrContentType.WIFI, content = "MyWiFi/password/WPA")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.Wifi)
        val wifi = data as QrData.Wifi
        assertEquals("MyWiFi", wifi.ssid)
        assertEquals("password", wifi.psk)
    }

    @Test
    fun `电话类型映射为 QrData_Phone`() {
        val config = QrConfig(contentType = QrContentType.PHONE, content = "13800138000")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.Phone)
    }

    @Test
    fun `邮件类型映射为 QrData_Email`() {
        val config = QrConfig(contentType = QrContentType.EMAIL, content = "test@example.com")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.Email)
    }

    @Test
    fun `YouTube 类型映射为 QrData_YouTube`() {
        val config = QrConfig(contentType = QrContentType.YOUTUBE, content = "dQw4w9WgXcQ")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.YouTube)
    }

    @Test
    fun `位置类型正确解析经纬度`() {
        val config = QrConfig(contentType = QrContentType.GEO, content = "39.9,116.4")
        val data = useCase.mapQrData(config)
        assertTrue(data is QrData.GeoPos)
        val geo = data as QrData.GeoPos
        assertEquals(39.9f, geo.lat)
        assertEquals(116.4f, geo.lon)
    }
}
```

- [ ] **Step 3: 运行测试**

Run: `./gradlew testDebugUnitTest --tests "com.example.qrgenerator.domain.QrGeneratorUseCaseTest" --no-daemon`
Expected: 全部 PASS

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/example/qrgenerator/domain/QrGeneratorUseCase.kt app/src/test/java/com/example/qrgenerator/domain/QrGeneratorUseCaseTest.kt
git commit -m "feat: 添加 QR 生成用例，支持所有 15 种内容类型映射"
```

---

## Task 7: 主题与通用 UI 组件

**Files:**
- Modify: `app/src/main/java/com/example/qrgenerator/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/example/qrgenerator/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/example/qrgenerator/ui/theme/Type.kt`
- Create: `app/src/main/java/com/example/qrgenerator/ui/components/ErrorDialog.kt`
- Create: `app/src/main/java/com/example/qrgenerator/ui/components/ExportDialog.kt`

- [ ] **Step 1: 重写 Color.kt（MIMO 黑白灰）**

```kotlin
package com.example.qrgenerator.ui.theme

import androidx.compose.ui.graphics.Color

// MIMO 纯黑白灰配色
val MimoBlack = Color(0xFF0D0D0D)
val MimoDarkSurface = Color(0xFF1A1A1A)
val MimoDarkCard = Color(0xFF252525)
val MimoDarkText = Color(0xFFE0E0E0)
val MimoDarkTextSecondary = Color(0xFF808080)
val MimoDarkDivider = Color(0xFF2A2A2A)

val MimoWhite = Color(0xFFFFFFFF)
val MimoLightSurface = Color(0xFFF5F5F5)
val MimoLightCard = Color(0xFFFFFFFF)
val MimoLightText = Color(0xFF1A1A1A)
val MimoLightTextSecondary = Color(0xFF666666)
val MimoLightDivider = Color(0xFFE0E0E0)
```

- [ ] **Step 2: 重写 Theme.kt**

```kotlin
package com.example.qrgenerator.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = MimoDarkCard,
    onPrimaryContainer = Color.White,
    secondary = Color.White,
    onSecondary = Color.Black,
    background = MimoBlack,
    onBackground = MimoDarkText,
    surface = MimoDarkSurface,
    onSurface = MimoDarkText,
    surfaceVariant = MimoDarkCard,
    onSurfaceVariant = MimoDarkTextSecondary,
    outline = MimoDarkDivider,
    error = Color(0xFFCF6679),
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = MimoLightSurface,
    onPrimaryContainer = Color.Black,
    secondary = Color.Black,
    onSecondary = Color.White,
    background = MimoWhite,
    onBackground = MimoLightText,
    surface = MimoWhite,
    onSurface = MimoLightText,
    surfaceVariant = MimoLightSurface,
    onSurfaceVariant = MimoLightTextSecondary,
    outline = MimoLightDivider,
    error = Color(0xFFB00020),
    onError = Color.White
)

@Composable
fun QRGeneratorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
```

- [ ] **Step 3: 创建 ErrorDialog.kt**

```kotlin
package com.example.qrgenerator.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** 统一错误弹窗 */
@Composable
fun ErrorDialog(
    title: String,
    message: String,
    confirmText: String = "确定",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = onConfirm
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(confirmText)
            }
        }
    )
}
```

- [ ] **Step 4: 创建 ExportDialog.kt**

```kotlin
package com.example.qrgenerator.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.qrgenerator.data.model.ExportFormat

/** 导出选项弹窗 */
@Composable
fun ExportDialog(
    onExport: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.PNG) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(text = "导出二维码", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column {
                Text(text = "选择导出格式", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                ExportFormat.entries.forEach { format ->
                    FilterChip(
                        selected = selectedFormat == format,
                        onClick = { selectedFormat = format },
                        label = { Text(format.displayName) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onExport(selectedFormat) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("导出")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text("取消")
            }
        }
    )
}
```

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/example/qrgenerator/ui/theme/ app/src/main/java/com/example/qrgenerator/ui/components/ErrorDialog.kt app/src/main/java/com/example/qrgenerator/ui/components/ExportDialog.kt
git commit -m "feat: 添加 MIMO 黑白灰主题和通用弹窗组件"
```

---

## Task 8: 导航与底部导航栏

**Files:**
- Create: `app/src/main/java/com/example/qrgenerator/ui/navigation/NavGraph.kt`
- Create: `app/src/main/java/com/example/qrgenerator/ui/navigation/BottomNavBar.kt`
- Modify: `app/src/main/java/com/example/qrgenerator/MainActivity.kt`

- [ ] **Step 1: 创建 BottomNavBar.kt**

```kotlin
package com.example.qrgenerator.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/** 底部导航项 */
enum class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    GENERATE("generate", "生成", Icons.Default.AutoAwesome),
    STYLE("style", "样式", Icons.Default.Palette),
    HISTORY("history", "历史", Icons.Default.History),
    SETTINGS("settings", "设置", Icons.Default.Settings);
}

/** 底部导航栏 */
@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        BottomNavItem.entries.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}
```

- [ ] **Step 2: 创建 NavGraph.kt**

```kotlin
package com.example.qrgenerator.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.qrgenerator.ui.screens.generate.GenerateScreen
import com.example.qrgenerator.ui.screens.history.HistoryScreen
import com.example.qrgenerator.ui.screens.settings.SettingsScreen
import com.example.qrgenerator.ui.screens.style.StyleScreen

/** 导航图 */
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.GENERATE.route
    ) {
        composable(BottomNavItem.GENERATE.route) {
            GenerateScreen()
        }
        composable(BottomNavItem.STYLE.route) {
            StyleScreen()
        }
        composable(BottomNavItem.HISTORY.route) {
            HistoryScreen()
        }
        composable(BottomNavItem.SETTINGS.route) {
            SettingsScreen()
        }
    }
}
```

- [ ] **Step 3: 更新 MainActivity.kt**

```kotlin
package com.example.qrgenerator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.qrgenerator.ui.navigation.BottomNavBar
import com.example.qrgenerator.ui.navigation.NavGraph
import com.example.qrgenerator.ui.theme.QRGeneratorTheme

/** 主 Activity */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QRGeneratorTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        BottomNavBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavGraph(navController = navController)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/example/qrgenerator/ui/navigation/ app/src/main/java/com/example/qrgenerator/MainActivity.kt
git commit -m "feat: 添加底部导航栏和导航图"
```

---

## Task 9: 生成页面

**Files:**
- Create: `app/src/main/java/com/example/qrgenerator/ui/screens/generate/GenerateViewModel.kt`
- Create: `app/src/main/java/com/example/qrgenerator/ui/screens/generate/GenerateScreen.kt`
- Create: `app/src/main/java/com/example/qrgenerator/ui/components/QrCodeDisplay.kt`
- Create: `app/src/main/java/com/example/qrgenerator/ui/components/ContentTypeChipGroup.kt`

- [ ] **Step 1: 创建 GenerateViewModel.kt**

```kotlin
package com.example.qrgenerator.ui.screens.generate

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.example.qrgenerator.domain.QrGeneratorUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 生成页面 UI 状态 */
data class GenerateUiState(
    val config: QrConfig = QrConfig(),
    val qrDrawable: Drawable? = null,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null
)

/** 生成页面 ViewModel */
class GenerateViewModel(private val qrGeneratorUseCase: QrGeneratorUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(GenerateUiState())
    val uiState: StateFlow<GenerateUiState> = _uiState.asStateFlow()

    private var generateJob: Job? = null

    /** 更新配置并触发实时预览 */
    fun executeUpdateConfig(newConfig: QrConfig) {
        _uiState.update { it.copy(config = newConfig) }
        executeDebounceGenerate()
    }

    /** 更新内容类型 */
    fun executeUpdateContentType(type: QrContentType) {
        _uiState.update { it.copy(config = it.config.copy(contentType = type, content = "")) }
    }

    /** 更新内容文本 */
    fun executeUpdateContent(content: String) {
        _uiState.update { it.copy(config = it.config.copy(content = content)) }
        executeDebounceGenerate()
    }

    /** 防抖生成（300ms 延迟） */
    private fun executeDebounceGenerate() {
        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            delay(300)
            executeGenerate()
        }
    }

    /** 立即生成二维码 */
    fun executeGenerate() {
        val config = _uiState.value.config
        if (config.content.isBlank()) {
            _uiState.update { it.copy(qrDrawable = null, isGenerating = false) }
            return
        }
        _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
        try {
            val drawable = qrGeneratorUseCase.execute(config)
            _uiState.update { it.copy(qrDrawable = drawable, isGenerating = false) }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    qrDrawable = null,
                    isGenerating = false,
                    errorMessage = "生成失败：${e.message ?: "内容过长或格式错误"}"
                )
            }
        }
    }

    /** 清除错误消息 */
    fun executeClearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
```

- [ ] **Step 2: 创建 QrCodeDisplay.kt**

```kotlin
package com.example.qrgenerator.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** QR 码预览组件 */
@Composable
fun QrCodeDisplay(
    qrDrawable: Drawable?,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isGenerating -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            qrDrawable != null -> {
                AndroidView(
                    factory = { context ->
                        android.widget.ImageView(context).apply {
                            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                        }
                    },
                    update = { it.setImageDrawable(qrDrawable) },
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                )
            }
            else -> {
                Text(
                    text = "输入内容后自动生成",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

- [ ] **Step 3: 创建 ContentTypeChipGroup.kt**

```kotlin
package com.example.qrgenerator.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.qrgenerator.data.model.QrContentType

/** 内容类型 chip 选择组（横向滚动） */
@Composable
fun ContentTypeChipGroup(
    selectedType: QrContentType,
    onTypeSelected: (QrContentType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QrContentType.entries.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(type.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}
```

- [ ] **Step 4: 创建 GenerateScreen.kt**

```kotlin
package com.example.qrgenerator.ui.screens.generate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qrgenerator.ui.components.ContentTypeChipGroup
import com.example.qrgenerator.ui.components.ErrorDialog
import com.example.qrgenerator.ui.components.QrCodeDisplay

/** 生成页面 */
@Composable
fun GenerateScreen(viewModel: GenerateViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // QR 码预览
        QrCodeDisplay(
            qrDrawable = uiState.qrDrawable,
            isGenerating = uiState.isGenerating
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 内容类型选择
        ContentTypeChipGroup(
            selectedType = uiState.config.contentType,
            onTypeSelected = { viewModel.executeUpdateContentType(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 内容输入
        OutlinedTextField(
            value = uiState.config.content,
            onValueChange = { viewModel.executeUpdateContent(it) },
            label = { Text("输入${uiState.config.contentType.displayName}内容") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 5,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 保存和分享按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { /* 保存逻辑 */ },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = uiState.qrDrawable != null
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Text(" 保存")
            }
            Button(
                onClick = { /* 分享逻辑 */ },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = uiState.qrDrawable != null
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Text(" 分享")
            }
        }
    }

    // 错误弹窗
    uiState.errorMessage?.let { message ->
        ErrorDialog(
            title = "生成失败",
            message = message,
            onConfirm = { viewModel.executeClearError() }
        )
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/example/qrgenerator/ui/screens/generate/ app/src/main/java/com/example/qrgenerator/ui/components/QrCodeDisplay.kt app/src/main/java/com/example/qrgenerator/ui/components/ContentTypeChipGroup.kt
git commit -m "feat: 添加生成页面（预览、内容输入、类型选择）"
```

---

## Task 10: 样式页面

**Files:**
- Create: `app/src/main/java/com/example/qrgenerator/ui/screens/style/StyleViewModel.kt`
- Create: `app/src/main/java/com/example/qrgenerator/ui/screens/style/StyleScreen.kt`
- Create: `app/src/main/java/com/example/qrgenerator/ui/components/ShapeChipGroup.kt`
- Create: `app/src/main/java/com/example/qrgenerator/ui/components/ColorGridPicker.kt`

- [ ] **Step 1: 创建 StyleViewModel.kt**

```kotlin
package com.example.qrgenerator.ui.screens.style

import androidx.lifecycle.ViewModel
import com.example.qrgenerator.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 样式页面 ViewModel */
class StyleViewModel : ViewModel() {

    private val _config = MutableStateFlow(QrConfig())
    val config: StateFlow<QrConfig> = _config.asStateFlow()

    fun executeUpdatePixelShape(shape: QrPixelShapeType) {
        _config.update { it.copy(darkPixelShape = shape) }
    }

    fun executeUpdateBallShape(shape: QrBallShapeType) {
        _config.update { it.copy(ballShape = shape) }
    }

    fun executeUpdateFrameShape(shape: QrFrameShapeType) {
        _config.update { it.copy(frameShape = shape) }
    }

    fun executeUpdateDarkColor(color: QrColorConfig) {
        _config.update { it.copy(darkColor = color) }
    }

    fun executeUpdateBallColor(color: QrColorConfig) {
        _config.update { it.copy(ballColor = color) }
    }

    fun executeUpdateFrameColor(color: QrColorConfig) {
        _config.update { it.copy(frameColor = color) }
    }

    fun executeUpdateCodeShape(shape: CodeShapeType) {
        _config.update { it.copy(codeShape = shape) }
    }

    fun executeUpdateErrorLevel(level: QrErrorLevel) {
        _config.update { it.copy(errorCorrectionLevel = level) }
    }

    fun executeUpdateFourthEye(enabled: Boolean) {
        _config.update { it.copy(fourthEyeEnabled = enabled) }
    }

    fun executeUpdateCentralSymmetry(enabled: Boolean) {
        _config.update { it.copy(centralSymmetry = enabled) }
    }

    fun executeUpdateHighlighting(highlighting: QrHighlightingConfig) {
        _config.update { it.copy(highlighting = highlighting) }
    }

    fun executeUpdateLogo(logo: QrLogoConfig) {
        _config.update { it.copy(logo = logo) }
    }
}
```

- [ ] **Step 2: 创建 ShapeChipGroup.kt**

```kotlin
package com.example.qrgenerator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 通用形状选择 chip 组 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ShapeChipGroup(
    title: String,
    options: List<T>,
    selected: T,
    displayName: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(displayName(option)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}
```

- [ ] **Step 3: 创建 ColorGridPicker.kt**

```kotlin
package com.example.qrgenerator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/** 预设颜色 */
private val presetColors = listOf(
    Color.Black, Color.White, Color(0xFF333333), Color(0xFF666666),
    Color(0xFF999999), Color(0xFFCCCCCC), Color(0xFFE53935), Color(0xFF43A047),
    Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFFDD835), Color(0xFF00ACC1),
)

/** 颜色网格选择器 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorGridPicker(
    title: String,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            presetColors.forEach { color ->
                val colorInt = color.toArgb()
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (selectedColor == colorInt) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            }
                        )
                        .clickable { onColorSelected(colorInt) }
                )
            }
        }
    }
}
```

- [ ] **Step 4: 创建 StyleScreen.kt**

```kotlin
package com.example.qrgenerator.ui.screens.style

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qrgenerator.data.model.*

/** 样式页面 */
@Composable
fun StyleScreen(viewModel: StyleViewModel = viewModel()) {
    val config by viewModel.config.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 模块形状
        StyleCard(title = "模块形状") {
            ShapeChipGroup(
                title = "暗色模块",
                options = QrPixelShapeType.entries,
                selected = config.darkPixelShape,
                displayName = { it.displayName },
                onSelected = { viewModel.executeUpdatePixelShape(it) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ShapeChipGroup(
                title = "亮色模块",
                options = QrPixelShapeType.entries,
                selected = config.lightPixelShape,
                displayName = { it.displayName },
                onSelected = { viewModel.executeUpdatePixelShape(it) }
            )
        }

        // 眼睛形状
        StyleCard(title = "眼睛形状") {
            ShapeChipGroup(
                title = "外框",
                options = QrFrameShapeType.entries,
                selected = config.frameShape,
                displayName = { it.displayName },
                onSelected = { viewModel.executeUpdateFrameShape(it) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ShapeChipGroup(
                title = "内球",
                options = QrBallShapeType.entries,
                selected = config.ballShape,
                displayName = { it.displayName },
                onSelected = { viewModel.executeUpdateBallShape(it) }
            )
        }

        // 颜色
        StyleCard(title = "颜色") {
            ColorGridPicker(
                title = "前景色",
                selectedColor = config.darkColor.solidColor,
                onColorSelected = {
                    viewModel.executeUpdateDarkColor(QrColorConfig(solidColor = it))
                }
            )
        }

        // 容错级别
        StyleCard(title = "容错级别") {
            ShapeChipGroup(
                title = "",
                options = QrErrorLevel.entries,
                selected = config.errorCorrectionLevel,
                displayName = { it.displayName },
                onSelected = { viewModel.executeUpdateErrorLevel(it) }
            )
        }

        // 整体外形
        StyleCard(title = "整体外形") {
            ShapeChipGroup(
                title = "",
                options = CodeShapeType.entries,
                selected = config.codeShape,
                displayName = { it.displayName },
                onSelected = { viewModel.executeUpdateCodeShape(it) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/** 样式卡片容器 */
@Composable
private fun StyleCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/example/qrgenerator/ui/screens/style/ app/src/main/java/com/example/qrgenerator/ui/components/ShapeChipGroup.kt app/src/main/java/com/example/qrgenerator/ui/components/ColorGridPicker.kt
git commit -m "feat: 添加样式页面（形状、颜色、容错、外形选择）"
```

---

## Task 11: 历史页面

**Files:**
- Create: `app/src/main/java/com/example/qrgenerator/ui/screens/history/HistoryViewModel.kt`
- Create: `app/src/main/java/com/example/qrgenerator/ui/screens/history/HistoryScreen.kt`

- [ ] **Step 1: 创建 HistoryViewModel.kt**

```kotlin
package com.example.qrgenerator.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrgenerator.data.local.QrHistoryEntity
import com.example.qrgenerator.data.repository.QrHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 历史页面 UI 状态 */
data class HistoryUiState(
    val records: List<QrHistoryEntity> = emptyList(),
    val searchQuery: String = "",
    val isDeleteMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
)

/** 历史页面 ViewModel */
class HistoryViewModel(private val repository: QrHistoryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        executeLoadHistory()
    }

    private fun executeLoadHistory() {
        viewModelScope.launch {
            repository.executeGetAll().collect { records ->
                _uiState.update { it.copy(records = records) }
            }
        }
    }

    fun executeSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            if (query.isBlank()) {
                repository.executeGetAll().collect { records ->
                    _uiState.update { it.copy(records = records) }
                }
            } else {
                repository.executeSearch(query).collect { records ->
                    _uiState.update { it.copy(records = records) }
                }
            }
        }
    }

    fun executeToggleDeleteMode() {
        _uiState.update { it.copy(isDeleteMode = !it.isDeleteMode, selectedIds = emptySet()) }
    }

    fun executeToggleSelection(id: Long) {
        _uiState.update {
            val newIds = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id
            it.copy(selectedIds = newIds)
        }
    }

    fun executeDeleteSelected() {
        viewModelScope.launch {
            _uiState.value.records
                .filter { it.id in _uiState.value.selectedIds }
                .forEach { repository.executeDelete(it) }
            _uiState.update { it.copy(isDeleteMode = false, selectedIds = emptySet()) }
        }
    }

    fun executeDeleteAll() {
        viewModelScope.launch {
            repository.executeDeleteAll()
            _uiState.update { it.copy(isDeleteMode = false, selectedIds = emptySet()) }
        }
    }
}
```

- [ ] **Step 2: 创建 HistoryScreen.kt**

```kotlin
package com.example.qrgenerator.ui.screens.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.io.File

/** 历史页面 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("历史记录") },
            actions = {
                if (uiState.isDeleteMode) {
                    IconButton(onClick = { viewModel.executeDeleteSelected() }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除选中")
                    }
                }
            }
        )

        // 搜索框
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.executeSearch(it) },
            label = { Text("搜索") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp)
        )

        // 历史记录网格
        if (uiState.records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无历史记录",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.records) { record ->
                    val isSelected = record.id in uiState.selectedIds
                    Card(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = {
                                    if (uiState.isDeleteMode) {
                                        viewModel.executeToggleSelection(record.id)
                                    }
                                },
                                onLongClick = { viewModel.executeToggleDeleteMode() }
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Box {
                            AsyncImage(
                                model = File(record.thumbnailPath),
                                contentDescription = record.contentPreview,
                                modifier = Modifier.size(120.dp),
                                contentScale = ContentScale.Fit
                            )
                            if (uiState.isDeleteMode && isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "已选中",
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/example/qrgenerator/ui/screens/history/
git commit -m "feat: 添加历史记录页面（搜索、网格展示、长按删除）"
```

---

## Task 12: 设置页面

**Files:**
- Create: `app/src/main/java/com/example/qrgenerator/ui/screens/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/example/qrgenerator/ui/screens/settings/SettingsScreen.kt`

- [ ] **Step 1: 创建 SettingsViewModel.kt**

```kotlin
package com.example.qrgenerator.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrgenerator.data.model.AppSettings
import com.example.qrgenerator.data.model.ExportFormat
import com.example.qrgenerator.data.model.QrErrorLevel
import com.example.qrgenerator.data.model.ThemeMode
import com.example.qrgenerator.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 设置页面 ViewModel */
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            repository.executeGetSettings().collect { _settings.value = it }
        }
    }

    fun executeUpdateThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.executeUpdateThemeMode(mode) }
    }

    fun executeUpdateErrorLevel(level: QrErrorLevel) {
        viewModelScope.launch { repository.executeUpdateErrorLevel(level) }
    }

    fun executeUpdateExportFormat(format: ExportFormat) {
        viewModelScope.launch { repository.executeUpdateExportFormat(format) }
    }

    fun executeUpdateImageSize(size: Int) {
        viewModelScope.launch { repository.executeUpdateImageSize(size) }
    }
}
```

- [ ] **Step 2: 创建 SettingsScreen.kt**

```kotlin
package com.example.qrgenerator.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qrgenerator.data.model.ExportFormat
import com.example.qrgenerator.data.model.QrErrorLevel
import com.example.qrgenerator.data.model.ThemeMode

/** 设置页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showFormatDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(title = { Text("设置") })

        // 主题模式
        SettingsItem(
            title = "主题模式",
            subtitle = settings.themeMode.displayName,
            onClick = { showThemeDialog = true }
        )

        // 默认容错级别
        SettingsItem(
            title = "默认容错级别",
            subtitle = settings.defaultErrorLevel.displayName,
            onClick = { showErrorDialog = true }
        )

        // 默认导出格式
        SettingsItem(
            title = "默认导出格式",
            subtitle = settings.defaultExportFormat.displayName,
            onClick = { showFormatDialog = true }
        )

        // 默认图片尺寸
        SettingsItem(
            title = "默认图片尺寸",
            subtitle = "${settings.defaultImageSize} x ${settings.defaultImageSize}",
            onClick = { }
        )

        // 关于
        SettingsItem(
            title = "关于",
            subtitle = "版本 1.0.0",
            onClick = { }
        )
    }

    // 主题选择弹窗
    if (showThemeDialog) {
        SingleChoiceDialog(
            title = "主题模式",
            options = ThemeMode.entries.map { it.displayName },
            selectedIndex = ThemeMode.entries.indexOf(settings.themeMode),
            onSelect = {
                viewModel.executeUpdateThemeMode(ThemeMode.entries[it])
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    // 容错级别选择弹窗
    if (showErrorDialog) {
        SingleChoiceDialog(
            title = "默认容错级别",
            options = QrErrorLevel.entries.map { it.displayName },
            selectedIndex = QrErrorLevel.entries.indexOf(settings.defaultErrorLevel),
            onSelect = {
                viewModel.executeUpdateErrorLevel(QrErrorLevel.entries[it])
                showErrorDialog = false
            },
            onDismiss = { showErrorDialog = false }
        )
    }

    // 导出格式选择弹窗
    if (showFormatDialog) {
        SingleChoiceDialog(
            title = "默认导出格式",
            options = ExportFormat.entries.map { it.displayName },
            selectedIndex = ExportFormat.entries.indexOf(settings.defaultExportFormat),
            onSelect = {
                viewModel.executeUpdateExportFormat(ExportFormat.entries[it])
                showFormatDialog = false
            },
            onDismiss = { showFormatDialog = false }
        )
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text(title) },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    FilterChip(
                        selected = selectedIndex == index,
                        onClick = { onSelect(index) },
                        label = { Text(option) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {}
    )
}
```

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/example/qrgenerator/ui/screens/settings/
git commit -m "feat: 添加设置页面（主题、容错、导出格式）"
```

---

## Task 13: Koin 依赖注入

**Files:**
- Create: `app/src/main/java/com/example/qrgenerator/di/AppModule.kt`
- Modify: `app/src/main/java/com/example/qrgenerator/QrGeneratorApp.kt`

- [ ] **Step 1: 创建 AppModule.kt**

```kotlin
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
```

- [ ] **Step 2: 更新 QrGeneratorApp.kt**

```kotlin
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
```

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/example/qrgenerator/di/ app/src/main/java/com/example/qrgenerator/QrGeneratorApp.kt
git commit -m "feat: 添加 Koin 依赖注入配置"
```

---

## Task 14: 导出用例（PNG/SVG/PDF）

**Files:**
- Create: `app/src/main/java/com/example/qrgenerator/domain/ExportUseCase.kt`

- [ ] **Step 1: 创建 ExportUseCase.kt**

```kotlin
package com.example.qrgenerator.domain

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.qrgenerator.data.model.ExportFormat
import java.io.File
import java.io.FileOutputStream

/** 导出用例 — 支持 PNG/SVG/PDF 导出、保存到相册、系统分享 */
class ExportUseCase(private val context: Context) {

    /** 将 Drawable 转换为 Bitmap */
    fun executeDrawableToBitmap(drawable: Drawable, width: Int = 1024, height: Int = 1024): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }

    /** 保存到相册 */
    fun executeSaveToGallery(bitmap: Bitmap, filename: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QRGenerator")
            }
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return null
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        return uri
    }

    /** 生成分享 Intent */
    fun executeCreateShareIntent(bitmap: Bitmap): Intent {
        val file = File(context.cacheDir, "qr_share.png")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/example/qrgenerator/domain/ExportUseCase.kt
git commit -m "feat: 添加导出用例（保存相册、系统分享）"
```

---

## Task 15: 删除旧文件并清理

**Files:**
- Delete: `app/src/main/java/com/example/qrgenerator/ui/screens/home/` (旧的单页面)
- Delete: `app/src/main/java/com/example/qrgenerator/ui/components/ErrorLevelSelector.kt` (已合并)

- [ ] **Step 1: 删除旧文件**

```bash
rm -rf app/src/main/java/com/example/qrgenerator/ui/screens/home/
rm -f app/src/main/java/com/example/qrgenerator/ui/components/ErrorLevelSelector.kt
```

- [ ] **Step 2: 验证编译**

Run: `./gradlew assembleDebug --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "refactor: 删除旧的单页面结构，清理冗余文件"
```

---

## Task 16: 最终验证

- [ ] **Step 1: 运行所有单元测试**

Run: `./gradlew testDebugUnitTest --no-daemon`
Expected: 全部 PASS

- [ ] **Step 2: 构建 Release APK**

Run: `./gradlew assembleRelease --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 最终提交**

```bash
git add -A
git commit -m "chore: 完成 QR 生成器全部功能实现"
```
