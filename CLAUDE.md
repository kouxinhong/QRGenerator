# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

QR 二维码生成器 Android App，基于 [custom-qr-generator](https://github.com/alexzhirkevich/custom-qr-generator) 库，使用 Jetpack Compose 构建。

## 常用命令

```bash
# 构建项目
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 运行单元测试
./gradlew test

# 运行单个测试类
./gradlew testDebugUnitTest --tests "com.example.qrgenerator.ClassName"

# 清理构建
./gradlew clean

# 检查依赖
./gradlew dependencies
```

## 项目架构

### 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **架构**: Clean Architecture + MVI
- **QR 库**: custom-qr-generator (Vector API)
- **状态管理**: StateFlow + ViewModel

### 目录结构

```
app/src/main/java/com/example/qrgenerator/
├── QrGeneratorApp.kt          # Application 入口
├── MainActivity.kt             # 主 Activity
├── data/                       # 数据层
│   ├── QrConfig.kt            # QR 配置数据类（所有可定制参数）
│   └── QrContentType.kt       # 内容类型枚举
├── domain/                     # 领域层
│   └── QrGeneratorUseCase.kt  # QR 码生成用例
└── ui/                         # 表现层
    ├── theme/                   # MIMO 风格主题
    │   ├── Color.kt            # 配色方案（小米橙主色调）
    │   ├── Theme.kt            # Material 3 主题配置
    │   └── Type.kt             # 字体样式
    ├── components/              # 可复用组件
    │   ├── QrCodeDisplay.kt   # QR 码显示组件
    │   ├── ContentTypeSelector.kt  # 内容类型选择器
    │   ├── ShapeSelector.kt   # 形状选择器
    │   ├── ColorPicker.kt     # 颜色选择器
    │   └── ErrorLevelSelector.kt  # 容错级别选择器
    └── screens/
        └── home/
            ├── HomeScreen.kt  # 首页界面
            └── HomeViewModel.kt  # 首页 ViewModel
```

### 核心设计

- **数据流**: ViewModel → StateFlow → Compose UI（单向数据流）
- **QR 生成**: QrGeneratorUseCase 将 QrConfig 转换为 custom-qr-generator 的 API 调用
- **实时预览**: 配置变更时自动触发 QR 码重新生成

## 语言规范

- **用户沟通**: 使用中文
- **代码**: 全部使用英文（关键字、标识符等）
- **注释**: 使用中文
- **文档**: 使用中文
- **技术术语**: 保留专业术语（如 Android, Kotlin, token, user）的英文形式

## Kotlin 通用规范

### 基本原则

- 所有变量和函数必须声明类型（参数和返回值）
- 避免使用 `any`，创建必要的类型
- 函数内部不留空行

### 命名规范

- 类: PascalCase
- 变量、函数、方法: camelCase
- 文件和目录: snake_case
- 环境变量: UPPERCASE
- 避免魔法数字，定义常量
- 函数以动词开头
- 布尔变量使用动词: isLoading, hasError, canDelete
- 使用完整单词，避免缩写（标准缩写除外: API, URL; 常见缩写: i, j, err, ctx, req, res）

### 函数

- 单一职责，少于 20 条指令
- 布尔返回值使用 isX/hasX/canX 命名
- 无返回值使用 executeX/saveX 命名
- 通过 early return 和提取工具函数避免嵌套
- 使用高阶函数（map, filter, reduce）替代嵌套
- 简单函数（少于 3 条指令）使用箭头函数
- 使用默认参数值替代 null 检查
- 使用 RO-RO 模式（对象传参、对象返回）

### 数据

- 使用 data class 表示数据
- 避免滥用原始类型，封装为复合类型
- 优先使用不可变数据（val）

### 类

- 遵循 SOLID 原则
- 优先组合而非继承
- 使用接口定义契约
- 小类：少于 200 条指令、10 个公共方法、10 个属性

### 异常

- 用异常处理非预期错误
- catch 异常时：修复预期问题、添加上下文、或使用全局处理器

### 测试

- 遵循 Arrange-Act-Assert 约定
- 测试变量命名: inputX, mockX, actualX, expectedX
- 为每个公共函数编写单元测试，使用测试替身
- 为每个模块编写验收测试，遵循 Given-When-Then 约定

## Android 专属规范

### 架构

- 使用 Clean Architecture
- 使用 Repository 模式进行数据持久化
- 使用 MVI 模式管理 ViewModel 中的状态和事件
- 使用 Navigation Component 管理导航
- 使用 Jetpack Compose 构建所有 UI
- 使用 StateFlow 管理 UI 状态
- 使用 Material 3（Material You）组件
- 使用 Modifier 链式修饰
- 使用 collectAsStateWithLifecycle() 收集 ViewModel 的 Flow
