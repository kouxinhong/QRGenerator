# QRGenerator · 二维码小卡

面向小店、摊位和活动现场的 Android 二维码工具。选择用途、填写内容，即可得到带标题与扫码说明的小卡，也可以保存纯二维码。

## 当前能力

- 三个快捷用途：连接 Wi-Fi、打开已有菜单网页、保存联系方式；自由制作保留其他内容类型。
- 卡片自动排版，支持修改标题与扫码提示；预览、相册、分享、PNG/PDF 文件导出使用同一张成品。
- 在本机回读成品图像并逐字比较扫码内容；检查失败时可恢复清晰样式，保留内容和卡片文字。
- 本地保存记录支持按卡片标题搜索、恢复后继续编辑；旧记录仍以纯二维码打开。
- 现有样式、Logo、主题及导出设置继续可用。

生成过程无需上传内容。Wi-Fi 连接信息会写入二维码；本机识别检查仅验证图像与内容，不验证网址可访问、密码正确，也不保证所有相机或打印条件下都能扫描。

## 开发与验证

项目使用 Kotlin、Jetpack Compose、Material 3、Koin、Room 和 DataStore，需要 JDK 17 与 Android SDK 34。在 `local.properties` 配置本机 `sdk.dir`。

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
```

Debug 安装包位于 `app/build/outputs/apk/debug/app-debug.apk`。设备测试应在专用模拟器运行，覆盖实际渲染回读、导出、场景卡片流程和历史恢复。

产品决策、竞品依据及用户验证方案见 [产品策略与验收](docs/product/2026-10-29-qr-cards-strategy.md)。当前定位是待真实用户验证的产品假设。
