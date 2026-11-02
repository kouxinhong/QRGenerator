package com.example.qrgenerator.domain

import com.example.qrgenerator.data.model.QrConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** QrConfigHolder 默认实现 */
class QrConfigHolderImpl : QrConfigHolder {

    private val _config = MutableStateFlow(QrConfig())
    override val config: StateFlow<QrConfig> = _config.asStateFlow()
    override var replacementRevision: Long = 0L
        private set

    override fun executeUpdateConfig(transform: (QrConfig) -> QrConfig) {
        _config.update(transform)
    }

    override fun executeSetConfig(config: QrConfig) {
        replacementRevision += 1L
        _config.value = config
    }
}
