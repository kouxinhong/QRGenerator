package com.example.qrgenerator.domain

import com.example.qrgenerator.data.model.QrConfig
import kotlinx.coroutines.flow.StateFlow

/** QR 配置共享持有者接口 — 用于在 ViewModel 之间共享配置状态 */
interface QrConfigHolder {

    val config: StateFlow<QrConfig>
    /** 完整恢复的次数，用于区分新草稿与历史/系统恢复。 */
    val replacementRevision: Long
    /** 即使恢复内容与当前草稿相同，也通知页面完整恢复已经发生。 */
    val replacementChanges: StateFlow<Long>

    /** 更新配置 */
    fun executeUpdateConfig(transform: (QrConfig) -> QrConfig)

    /** 设置完整配置 */
    fun executeSetConfig(config: QrConfig)
}
