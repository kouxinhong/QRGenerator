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
    private val _replacementChanges: MutableStateFlow<Long> = MutableStateFlow(0L)
    override val replacementChanges: StateFlow<Long> = _replacementChanges.asStateFlow()
    override val replacementRevision: Long
        get() = _replacementChanges.value

    override fun executeUpdateConfig(transform: (QrConfig) -> QrConfig) {
        _config.update { transform(it).withoutStaleSourcePayload() }
    }

    override fun executeSetConfig(config: QrConfig) {
        _config.value = config.withoutStaleSourcePayload()
        _replacementChanges.update { it + 1L }
    }
}
