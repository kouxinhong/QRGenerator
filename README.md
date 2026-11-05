# QRGenerator · 二维码小卡

面向小店、摊位和活动现场的 Android 二维码工具。选择用途、填写内容，即可得到带标题与扫码说明的小卡，也可以保存纯二维码。

## 当前能力

- 首页按用途进入：连接 Wi-Fi、分享页面、保存联系方式，也可直接扫码或选照片导入；自由制作保留其他内容类型。分享页面支持已有的菜单、报名、活动介绍等网页链接。
- 相机与照片识别支持多码选择和复制，结果按网页、Wi-Fi、联系人或文字展示。普通 Wi-Fi 和简单联系人可回填对应表单；含复杂或未知字段的码保留原文制作。
- 导入内容与当前用途匹配时保留卡面标题和提示；用途不匹配时切换到对应卡片。导入后可撤销，直到后续编辑或替换草稿；原用途草稿仍可切回继续编辑。
- 卡片自动排版，支持修改标题与扫码提示；预览、相册、分享、PNG 文件导出使用同一张成品。
- PDF 提供单张桌牌（100 × 140 毫米）和 A4 四张小卡（每张 90 × 120 毫米），均放在 A4 纸上并附裁切线；导出前可查看实际内容的整页排版预览。打印选择「100%／实际大小」，不要缩放适应纸张。
- 样式页会真实回读当前样式预览，并明确标注示例内容；生成页还会对最终成品再次回读、逐字比较扫码内容。检查失败时可恢复清晰样式，保留内容和卡片文字。
- 本地保存记录支持按卡片标题搜索、恢复后继续编辑；旧记录仍以纯二维码打开。
- 现有样式、Logo、主题及导出设置继续可用。

生成过程无需上传内容。Wi-Fi 连接信息会写入二维码；本机识别检查仅验证图像与内容，不验证网址可访问、密码正确，也不保证所有相机或打印条件下都能扫描。

## 开发与验证

项目使用 Kotlin、Jetpack Compose、Material 3、Koin、Room 和 DataStore，需要 JDK 17 与 Android SDK 34。在 `local.properties` 配置本机 `sdk.dir`。

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
```

Debug 安装包位于 `app/build/outputs/apk/debug/app-debug.apk`。设备测试应在专用模拟器运行，覆盖实际渲染回读、导出、场景卡片流程和历史恢复。

扫码使用 [WeChatQRCode](https://github.com/jenly1314/WeChatQRCode/tree/2.4.0) 2.4.0（兼容当前 compileSdk 34），只打包 `armeabi-v7a`、`arm64-v8a`。设备测试需使用 ARM 真机或 ARM64 模拟器；x86 模拟器不在此安装包支持范围内。扫码模型随包提供，识别在本机执行；照片入口直接选图，不申请相机权限，相机权限被拒绝时也仍可选照片。

## 复用扫码能力

页面注册 `QrScanContract`，启动后收到用户确认的单条原文，取消返回 `null`。`targetScene` 用于呈现当前用途的导入说明，`source` 选择相机或照片入口：

```kotlin
val contract = remember(currentConfig.card.scene) {
    QrScanContract(
        targetScene = currentConfig.card.scene,
        source = QrScanSource.PHOTO // CAMERA 为默认值
    )
}
val scanQr = rememberLauncherForActivityResult(contract) { rawPayload ->
    rawPayload?.let {
        val payload = QrPayloadParser.parse(it)
        val importedConfig = QrImportPolicy.createConfig(currentConfig, payload)
        // 将 importedConfig 交给业务状态持有者；保存导入前的草稿可支持撤销。
    }
}
// 点击按钮时调用
scanQr.launch(Unit)
```

不需要界面的场景可从 Koin 获取 `QrScanner`，在协程中调用 `scan(bitmap)` 或 `scan(uri)`。统一返回 `Success(contents)`、`NotFound` 或 `Failure(reason)`；成功结果可包含多个码。初始化和识别在后台按需串行执行，调用方的 Bitmap 不会被修改或回收。相机帧和照片都走这个入口。

`QrPayloadParser` 负责识别内容类型、生成展示字段，以及判断能否完整回填表单；无法完整表达的复杂内容不做有损转换。`QrImportPolicy` 负责选择目标用途、保留匹配卡面，并同时保存表单值与 `sourcePayload` 原文。只要内容类型和表单仍与导入时一致，`QrContentCodec` 就使用原文编码，保留空格、换行、字段顺序和结构化细节；编辑内容后根据表单重新编码，并移除已失效的原文快照，避免把旧连接信息带进无关记录。

扫码不会自动打开链接或连接 Wi-Fi。样式预览与成品回读使用独立的 ZXing 校验；预览通过仅说明当前预览可识别，不能替代最终成品检查或实际打印试扫。

产品决策、竞品依据及用户验证方案见 [产品策略与验收](docs/product/2026-10-29-qr-cards-strategy.md)。当前定位是待真实用户验证的产品假设。
