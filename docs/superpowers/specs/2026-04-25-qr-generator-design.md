# QR 二维码生成器 — 设计规格

## 概述

基于 custom-qr-generator 库的 Android 二维码生成器 App，使用 Jetpack Compose + Material 3 构建，UI 风格参考小米 MIMO（mimo.xiaomi.com）纯黑白灰极简设计。

## 需求总结


| #   | 需求                      | 方案                        |
| --- | ----------------------- | ------------------------- |
| 1   | 中文本地化                   | 所有 UI 文本使用简体中文            |
| 2   | MIMO 风格 UI              | 纯黑白灰极简，大圆角，大量留白           |
| 3   | Jetpack Compose         | 纯 Compose UI + Material 3 |
| 4   | custom-qr-generator 全功能 | 覆盖库的所有 API                |
| 5   | 代码加中文注释                 | KDoc + 行内注释               |


## 架构方案

### 技术栈

- Kotlin + Jetpack Compose + Material 3
- Navigation Compose（底部导航 + 页面路由）
- Koin（依赖注入）
- Room（历史记录持久化）
- DataStore（用户偏好设置）
- custom-qr-generator v1.6.2（QR 生成，Vector API）

### 分层结构

```
UI 层（Compose + ViewModel）
    ↓
Domain 层（UseCase）
    ↓
Data 层（Room / DataStore / FileStore）
```

### 导航结构

底部导航四个 tab：


| Tab | 图标          | 页面             | 职责                    |
| --- | ----------- | -------------- | --------------------- |
| 生成  | AutoAwesome | GenerateScreen | 输入内容、选择类型、实时预览、导出/分享  |
| 样式  | Palette     | StyleScreen    | 形状、颜色、Logo、容错、高亮、整体外形 |
| 历史  | History     | HistoryScreen  | 已生成二维码列表，搜索/筛选/删除     |
| 设置  | Settings    | SettingsScreen | 主题、默认值、导出格式、清除数据、关于   |


路由：

- `"generate"` → GenerateScreen
- `"style"` → StyleScreen
- `"history"` → HistoryScreen
- `"settings"` → SettingsScreen
- `"export"` → ExportDialog（弹窗）

## 数据模型

### QrConfig（QR 码全部配置）

```
QrConfig
├── contentType: QrContentType（15 种）
│   TEXT, URL, WIFI, ENTERPRISE_WIFI, VCARD, BIZCARD, MECARD,
│   PHONE, SMS, EMAIL, GEO, BOOKMARK, EVENT, YOUTUBE, GOOGLE_PLAY
├── content: String
├── padding: Float (0.0~0.5)
├── offset: QrOffset (x, y)
├── errorCorrectionLevel: QrErrorLevel
│   AUTO, LOW, MEDIUM, MEDIUM_HIGH, HIGH
│
├── shapes
│   ├── darkPixel: QrPixelShapeType（8 种：DEFAULT, CIRCLE, ROUND_CORNERS, RHOMBUS, STAR, ROUND_CORNERS_VERTICAL, ROUND_CORNERS_HORIZONTAL, CUSTOM）
│   ├── lightPixel: QrPixelShapeType（同上）
│   ├── ball: QrBallShapeType（6 种：DEFAULT, CIRCLE, ROUND_CORNERS, RHOMBUS, AS_DARK_PIXELS, CUSTOM）
│   ├── frame: QrFrameShapeType（5 种：DEFAULT, CIRCLE, ROUND_CORNERS, AS_DARK_PIXELS, CUSTOM）
│   └── centralSymmetry: Boolean
│
├── colors
│   ├── dark: QrColorConfig
│   │   类型：SOLID, LINEAR_GRADIENT, RADIAL_GRADIENT, SWEEP_GRADIENT
│   │   每种类型包含对应参数（颜色值、方向、半径等）
│   ├── ball: QrColorConfig
│   ├── frame: QrColorConfig
│   └── 特殊色：TRANSPARENT, ERASER 作为选项
│
├── codeShape: CodeShapeType（DEFAULT, CIRCLE）
│
├── logo
│   ├── source: LogoSource（GALLERY, CAMERA, BUILTIN_ICON）
│   ├── drawable: Drawable?
│   ├── size: Float (0.0~0.33)
│   ├── shape: LogoShapeType（DEFAULT, CIRCLE, ROUND_CORNERS, RHOMBUS）
│   ├── padding: LogoPaddingType + value
│   ├── scale: BitmapScale
│   └── backgroundColor: QrColorConfig
│
├── highlighting
│   ├── cornerEyes: HighlightingMode（NONE, DEFAULT, CUSTOM）
│   ├── versionEyes: HighlightingMode
│   ├── timingLines: HighlightingMode
│   └── alpha: Float (0.0~1.0)
│
├── fourthEyeEnabled: Boolean
└── imageWidth / imageHeight: Int
```

### QrHistory（历史记录，Room Entity）

```
QrHistory
├── id: Long（自增主键）
├── configJson: String（QrConfig JSON 序列化）
├── thumbnailPath: String（缩略图文件路径）
├── contentType: String（用于筛选）
├── contentPreview: String（内容前 50 字符，用于列表展示）
└── createdAt: Long（时间戳）
```

### AppSettings（用户偏好，DataStore）

```
AppSettings
├── themeMode: ThemeMode（FOLLOW_SYSTEM, LIGHT, DARK）
├── defaultErrorLevel: QrErrorLevel
├── defaultExportFormat: ExportFormat（PNG, SVG, PDF）
├── defaultImageSize: Int
└── hasSeenOnboarding: Boolean
```

### 扩展机制

```
CustomShapeRegistry
├── builtInShapes: Map<String, QrVectorPixelShape>
├── builtInColors: Map<String, QrVectorColor>
├── register(name, shape)
├── register(name, color)
└── get(name) → shape/color

// 枚举统一加 CUSTOM 类型，配合注册中心实现运行时扩展
```

## 自定义形状示例

### 蜂巢（Honeycomb）— 六边形像素

```kotlin
object Honeycomb : QrVectorPixelShape {
    override fun createPath(size: Float, neighbors: Neighbors): Path {
        val r = size / 2f
        val h = r * 0.866f
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

## UI 设计

### 配色方案（MIMO 纯黑白灰）


| 元素   | 暗色模式      | 亮色模式      |
| ---- | --------- | --------- |
| 主色   | `#FFFFFF` | `#000000` |
| 背景   | `#0D0D0D` | `#FFFFFF` |
| 卡片   | `#1A1A1A` | `#F5F5F5` |
| 文字   | `#E0E0E0` | `#1A1A1A` |
| 次要文字 | `#808080` | `#666666` |
| 分割线  | `#2A2A2A` | `#E0E0E0` |
| 交互高亮 | 反色填充      | 反色填充      |


### 组件风格

- 卡片：`RoundedCornerShape(16.dp)`，无阴影，背景色微差
- 按钮：胶囊形 `RoundedCornerShape(50)`，反色填充
- 输入框：`OutlinedTextField`，圆角 `12.dp`
- 选择器：`FilterChip`，选中时反色填充
- 颜色选择：圆形色块，选中时外圈边框
- Dialog：`AlertDialog`，圆角 20dp，无图标，反色胶囊按钮

### 生成页面

```
┌──────────────────────────┐
│  ┌────────────────────┐  │
│  │    QR 码预览        │  │  ← 圆角卡片，实时更新
│  └────────────────────┘  │
│  ┌────────────────────┐  │
│  │ 内容类型 [chip 横滚] │  │
│  └────────────────────┘  │
│  ┌────────────────────┐  │
│  │ 输入内容 [多行]      │  │
│  └────────────────────┘  │
│  ┌─────┐ ┌───────────┐  │
│  │保存  │ │ 分享       │  │  ← 并排按钮
│  └─────┘ └───────────┘  │
└──────────────────────────┘
```

### 样式页面

滚动列表，每个选项分类一个卡片：

- 模块形状、眼睛外框、眼睛内球 → chip 选择器
- 前景色、背景色 → 圆形色块网格
- 渐变设置 → 开关 + 颜色 + 方向
- Logo → 来源/大小/形状
- 高级 → 容错/高亮/第四眼
- 整体外形 → 方形/圆形

### 历史页面

- LazyGrid 缩略图网格
- 支持搜索和按类型筛选
- 长按进入删除模式
- 点击查看详情 + 重新编辑/导出

### 设置页面

设置项列表：

- 主题模式（跟随系统/浅色/深色）
- 默认容错级别
- 默认导出格式（PNG/SVG/PDF）
- 默认图片尺寸
- 清除历史记录
- 关于/版本信息

## QR 生成流程

```
用户操作 → ViewModel 更新 StateFlow
    → QrGeneratorUseCase.execute(config)
    → createQrVectorOptions { ... }
    → QrCodeDrawable(data, options)
    → Compose UI 实时刷新
```

## 错误处理


| 场景        | 处理方式                                 |
| --------- | ------------------------------------ |
| 内容为空      | 预览区占位提示，禁用导出                         |
| 内容过长      | Dialog："生成失败" / "内容过长，请缩短或提高容错级别"    |
| Logo 加载失败 | Dialog："图片加载失败" / "无法加载所选图片，请重新选择"   |
| 权限被拒      | Dialog："需要权限" / "请在设置中允许访问相册"，跳转设置按钮 |
| 导出失败      | Dialog："导出失败" + 具体原因                 |
| Room 写入失败 | 静默失败 + 日志                            |


## 测试策略


| 层级    | 覆盖范围                                                       |
| ----- | ---------------------------------------------------------- |
| 单元测试  | QrGeneratorUseCase、各内容类型编码、QrConfig 验证、CustomShapeRegistry |
| UI 测试 | 各页面 Compose 组件渲染、交互状态                                      |
| 集成测试  | ViewModel → UseCase → QR 生成完整流程                            |


