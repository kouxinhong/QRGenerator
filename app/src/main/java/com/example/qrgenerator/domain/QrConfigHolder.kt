package com.example.qrgenerator.domain

import com.example.qrgenerator.data.model.QrConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * QR 配置共享持有者
 * 用于在 GenerateViewModel 和 StyleViewModel 之间共享配置状态
 */
object QrConfigHolder {

    private val _config = MutableStateFlow(QrConfig())
    val config: StateFlow<QrConfig> = _config.asStateFlow()

    /** 更新配置 */
    fun executeUpdateConfig(transform: (QrConfig) -> QrConfig) {
        _config.update(transform)
    }

    /** 设置完整配置 */
    fun executeSetConfig(config: QrConfig) {
        _config.value = config
    }
}
