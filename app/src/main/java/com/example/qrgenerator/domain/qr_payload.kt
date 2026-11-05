package com.example.qrgenerator.domain

import com.example.qrgenerator.data.model.QrContentType
import java.net.URI

enum class QrPayloadKind { TEXT, URL, WIFI, CONTACT }

data class QrPayloadField(val label: String, val value: String, val isSensitive: Boolean = false)

/** 展示语义和原始载荷分离；不能完整表达的内容只允许保留原样制作。 */
data class QrPayload(
    val rawPayload: String,
    val kind: QrPayloadKind,
    val title: String,
    val summary: String,
    val fields: List<QrPayloadField>,
    val editableType: QrContentType? = null,
    val editableContent: String? = null
)

object QrPayloadParser {
    fun parse(rawPayload: String): QrPayload {
        val candidate: String = rawPayload.trim().removePrefix("\uFEFF").trimStart()
        if (WIFI_PREFIX.containsMatchIn(candidate)) return parseWifi(rawPayload, candidate)
        if (candidate.startsWith("BEGIN:VCARD", ignoreCase = true)) return parseVCard(rawPayload, candidate)
        if (candidate.startsWith("MECARD:", ignoreCase = true)) return parseMeCard(rawPayload, candidate)
        parseUrl(rawPayload, candidate)?.let { return it }
        return QrPayload(rawPayload, QrPayloadKind.TEXT, "文字内容", summarize(rawPayload), listOf(QrPayloadField("内容", rawPayload)), QrContentType.TEXT, rawPayload)
    }

    private fun parseUrl(rawPayload: String, candidate: String): QrPayload? {
        val uri: URI = runCatching { URI(candidate) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()) return null
        val url: String = uri.scheme.lowercase() + candidate.substring(candidate.indexOf(':'))
        return QrPayload(rawPayload, QrPayloadKind.URL, "网页链接", summarize(candidate), listOf(QrPayloadField("网址", candidate)), QrContentType.URL, QrContentCodec.serialize(mapOf("url" to url)))
    }

    private fun parseWifi(rawPayload: String, candidate: String): QrPayload {
        val fallback: QrPayload = QrPayload(rawPayload, QrPayloadKind.WIFI, "Wi-Fi 网络", "Wi-Fi 二维码", emptyList())
        if (!candidate.startsWith("WIFI:", ignoreCase = true) || !candidate.endsWith(';')) return fallback
        val entries: List<TaggedValue> = parseTags(candidate.substring(5)) ?: return fallback
        val network: TaggedValue = entries.singleOrNull { it.key == "S" } ?: return fallback
        if ((splitEscaped(network.rawValue, ':')?.size ?: 0) != 1) return fallback
        if (network.value.any { it.isISOControl() }) return fallback
        val values: Map<String, String> = entries.associate { it.key to it.value }
        val ssid: String = values["S"]?.takeIf { it.isNotBlank() } ?: return fallback
        val fields: List<QrPayloadField> = buildList {
            add(QrPayloadField("网络名称", ssid))
            values["P"]?.takeIf { it.isNotEmpty() }?.let { add(QrPayloadField("密码", it, isSensitive = true)) }
            values["T"]?.let { add(QrPayloadField("加密方式", if (it.equals("nopass", true) || it.equals("OPEN", true)) "无密码" else it)) }
            values["H"]?.let { add(QrPayloadField("隐藏网络", when (it.lowercase()) { "true" -> "是"; "false" -> "否"; else -> "未识别" })) }
        }
        val form: String? = editableWifiForm(entries, values)
        return fallback.copy(summary = "Wi-Fi：${summarize(ssid)}", fields = fields, editableType = form?.let { QrContentType.WIFI }, editableContent = form)
    }

    private fun editableWifiForm(entries: List<TaggedValue>, values: Map<String, String>): String? {
        if (entries.map { it.key }.distinct().size != entries.size || values.keys.any { it !in WIFI_KEYS }) return null
        val authentication: String = when (values["T"]?.uppercase()) {
            null, "NOPASS", "OPEN" -> "OPEN"
            "WPA" -> "WPA"
            "WEP" -> "WEP"
            else -> return null
        }
        val hidden: String = values["H"]?.lowercase() ?: "false"
        if (hidden !in setOf("true", "false")) return null
        val form: String = QrContentCodec.serialize(mapOf("ssid" to values["S"].orEmpty(), "password" to values["P"].orEmpty(), "auth" to authentication, "hidden" to hidden))
        return form.takeIf { ContentValidator().validate(QrContentType.WIFI, it) == ValidationResult.Valid }
    }

    private fun parseVCard(rawPayload: String, candidate: String): QrPayload {
        val fallback: QrPayload = contactFallback(rawPayload)
        val lines: List<String> = candidate.replace("\r\n", "\n").replace('\r', '\n').replace(Regex("\n[ \\t]"), "").lines().filter { it.isNotEmpty() }
        if (lines.firstOrNull()?.uppercase() != "BEGIN:VCARD" || lines.lastOrNull()?.uppercase() != "END:VCARD") return fallback
        val entries: List<TaggedValue> = lines.drop(1).dropLast(1).map { parseTag(it, allowNewline = true) ?: return fallback }
        val grouped: Map<String, List<TaggedValue>> = entries.groupBy { it.key.substringBefore(';') }
        val name: String = grouped["FN"]?.firstOrNull()?.value ?: grouped["N"]?.firstOrNull()?.value?.replace(';', ' ')?.trim().orEmpty()
        val fields: List<QrPayloadField> = createContactFields(name, grouped)
        val form: String? = editableVCardForm(entries, grouped, name)
        return fallback.copy(summary = name.takeIf { it.isNotBlank() }?.let { "联系人：${summarize(it)}" } ?: fallback.summary, fields = fields, editableType = form?.let { QrContentType.VCARD }, editableContent = form)
    }

    private fun editableVCardForm(entries: List<TaggedValue>, grouped: Map<String, List<TaggedValue>>, name: String): String? {
        if (name.isBlank() || entries.any { it.key !in VCARD_KEYS } || grouped.values.any { it.size > 1 }) return null
        if (grouped["VERSION"]?.singleOrNull()?.value !in setOf("2.1", "3.0", "4.0")) return null
        val values: Map<String, String> = entries.associate { it.key to it.value }
        val organization: String? = grouped["ORG"]?.singleOrNull()?.rawValue
        if (organization != null && (splitEscaped(organization, ';')?.size ?: 0) != 1) return null
        val nameParts: List<String> = grouped["N"]?.singleOrNull()?.rawValue?.let { splitEscaped(it, ';') ?: return null }.orEmpty()
        if (nameParts.isNotEmpty() && (decodeEscapes(nameParts.first(), true) != name || nameParts.drop(1).any { it.isNotEmpty() })) return null
        val address: String? = editableAddress(grouped["ADR"]?.singleOrNull()?.rawValue)
        if (values.containsKey("ADR") && address == null) return null
        val form: Map<String, String> = VCARD_FORM_KEYS.mapNotNull { (tag, key) -> values[tag]?.let { key to it } }.toMap() + mapOf("name" to name, "address" to address.orEmpty())
        return validateContactForm(form)
    }

    private fun editableAddress(rawValue: String?): String? {
        if (rawValue == null) return ""
        val parts: List<String> = splitEscaped(rawValue, ';') ?: return null
        if (parts.size != 7 || parts.filterIndexed { index, _ -> index != 2 }.any { it.isNotEmpty() }) return null
        return decodeEscapes(parts[2], true)
    }

    private fun parseMeCard(rawPayload: String, candidate: String): QrPayload {
        val fallback: QrPayload = contactFallback(rawPayload)
        if (!candidate.endsWith(';')) return fallback
        val entries: List<TaggedValue> = parseTags(candidate.substring(7), allowNewline = true) ?: return fallback
        val grouped: Map<String, List<TaggedValue>> = entries.groupBy { it.key }
        val name: String = grouped["N"]?.firstOrNull()?.value.orEmpty()
        val fields: List<QrPayloadField> = createContactFields(name, grouped)
        val form: String? = editableMeCardForm(entries, name)
        return fallback.copy(summary = name.takeIf { it.isNotBlank() }?.let { "联系人：${summarize(it)}" } ?: fallback.summary, fields = fields, editableType = form?.let { QrContentType.VCARD }, editableContent = form)
    }

    private fun editableMeCardForm(entries: List<TaggedValue>, name: String): String? {
        if (name.isBlank() || entries.any { it.key !in MECARD_KEYS } || entries.map { it.key }.distinct().size != entries.size) return null
        val nameParts: List<String> = splitEscaped(entries.first { it.key == "N" }.rawValue, ',') ?: return null
        if (nameParts.size != 1) return null
        val values: Map<String, String> = entries.associate { it.key to it.value }
        val form: Map<String, String> = VCARD_FORM_KEYS.mapNotNull { (tag, key) -> values[tag]?.let { key to it } }.toMap() + mapOf("name" to name, "address" to values["ADR"].orEmpty())
        return validateContactForm(form)
    }

    private fun validateContactForm(values: Map<String, String>): String? {
        val form: String = QrContentCodec.serialize(values)
        return form.takeIf { ContentValidator().validate(QrContentType.VCARD, it) == ValidationResult.Valid }
    }

    private fun createContactFields(name: String, grouped: Map<String, List<TaggedValue>>): List<QrPayloadField> = buildList {
        if (name.isNotBlank()) add(QrPayloadField("姓名", name))
        CONTACT_LABELS.forEach { (key, label) -> grouped[key]?.forEach { entry ->
            if (entry.value.isNotBlank()) add(QrPayloadField(label, if (key == "ADR") entry.value.replace(';', ' ').trim() else entry.value))
        } }
    }

    private fun contactFallback(rawPayload: String): QrPayload = QrPayload(rawPayload, QrPayloadKind.CONTACT, "联系人", "联系人二维码", emptyList())

    private fun parseTags(body: String, allowNewline: Boolean = false): List<TaggedValue>? {
        val tokens: List<String> = splitEscaped(body, ';') ?: return null
        return tokens.filter { it.isNotEmpty() }.map { parseTag(it, allowNewline) ?: return null }
    }

    private fun parseTag(token: String, allowNewline: Boolean): TaggedValue? {
        val index: Int = token.indexOf(':')
        if (index <= 0) return null
        val key: String = token.substring(0, index).uppercase()
        val rawValue: String = token.substring(index + 1)
        return TaggedValue(key, decodeEscapes(rawValue, allowNewline) ?: return null, rawValue)
    }

    /** 保留转义符，便于后续继续区分姓名、地址里的分隔符与真实字符。 */
    private fun splitEscaped(value: String, delimiter: Char): List<String>? {
        val parts: MutableList<String> = mutableListOf()
        val current: StringBuilder = StringBuilder()
        var isEscaped: Boolean = false
        value.forEach { character ->
            when {
                isEscaped -> { current.append(character); isEscaped = false }
                character == '\\' -> { current.append(character); isEscaped = true }
                character == delimiter -> { parts.add(current.toString()); current.clear() }
                else -> current.append(character)
            }
        }
        if (isEscaped) return null
        parts.add(current.toString())
        return parts
    }

    private fun decodeEscapes(value: String, allowNewline: Boolean): String? {
        val decoded: StringBuilder = StringBuilder()
        var isEscaped: Boolean = false
        value.forEach { character ->
            when {
                isEscaped -> {
                    if (allowNewline && character in "nN") decoded.append('\n')
                    else if (character in "\\;:,\"") decoded.append(character)
                    else return null
                    isEscaped = false
                }
                character == '\\' -> isEscaped = true
                else -> decoded.append(character)
            }
        }
        return if (isEscaped) null else decoded.toString()
    }

    private fun summarize(value: String): String = value.replace('\r', ' ').replace('\n', ' ').take(80)

    private data class TaggedValue(val key: String, val value: String, val rawValue: String)
    private val WIFI_PREFIX: Regex = Regex("^WIFI\\s*:", RegexOption.IGNORE_CASE)
    private val WIFI_KEYS: Set<String> = setOf("S", "T", "P", "H")
    private val VCARD_KEYS: Set<String> = setOf("VERSION", "FN", "N", "TEL", "EMAIL", "ORG", "TITLE", "ADR", "URL", "NOTE")
    private val MECARD_KEYS: Set<String> = setOf("N", "TEL", "EMAIL", "ADR", "URL", "NOTE")
    private val VCARD_FORM_KEYS: Map<String, String> = mapOf("TEL" to "phone", "EMAIL" to "email", "ORG" to "company", "TITLE" to "job", "URL" to "website", "NOTE" to "note")
    private val CONTACT_LABELS: Map<String, String> = mapOf("TEL" to "电话", "EMAIL" to "邮箱", "ORG" to "单位", "TITLE" to "职位", "ADR" to "地址", "URL" to "网址", "NOTE" to "备注")
}
