package com.example.qrgenerator.domain

import com.example.qrgenerator.data.model.QrConfig
import kotlinx.coroutines.flow.StateFlow

/** QR 配置共享持有者接口 — 用于在 ViewModel 之间共享配置状态 */
interface QrConfigHolder {

    val config: StateFlow<QrConfig>

    /** 更新配置 */
    fun executeUpdateConfig(transform: (QrConfig) -> QrConfig)

    /** 设置完整配置 */
    fun executeSetConfig(config: QrConfig)
}
