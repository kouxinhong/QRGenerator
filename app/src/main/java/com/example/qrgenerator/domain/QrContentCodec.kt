package com.example.qrgenerator.domain

import com.example.qrgenerator.data.model.QrConfig
import com.example.qrgenerator.data.model.QrContentType
import com.github.alexzhirkevich.customqrgenerator.QrData
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.net.URI
import java.net.URLDecoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** 表单字段以 JSON 保存，旧版本的普通文本和分隔格式仍可打开。 */
object QrContentCodec {
    private val gson: Gson = Gson()
    private const val FORM_MARKER: String = "_qrForm"
    val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun serialize(values: Map<String, String>): String {
        if (values.values.all { it.isBlank() }) return ""
        return gson.toJson(values + (FORM_MARKER to "1"))
    }

    fun isStructured(content: String): Boolean = readJson(content) != null

    fun fields(type: QrContentType, content: String): Map<String, String> {
        if (type == QrContentType.TEXT) return mapOf("text" to content)
        readJson(content)?.let { return it }
        val parts: List<String> = content.split(if (type == QrContentType.GEO) "," else "/")
        val keys: List<String> = when (type) {
            QrContentType.WIFI -> listOf("ssid", "password", "auth")
            QrContentType.ENTERPRISE_WIFI -> listOf("ssid", "password", "user", "eap", "phase")
            QrContentType.VCARD -> listOf("name", "company", "job", "phone", "email")
            QrContentType.BIZCARD -> listOf("firstName", "lastName", "job", "company", "phone", "email")
            QrContentType.MECARD -> listOf("name", "phone", "email")
            QrContentType.SMS -> listOf("phone", "message")
            QrContentType.GEO -> listOf("latitude", "longitude")
            else -> return mapOf(primaryKey(type) to content)
        }
        return keys.mapIndexed { index, key -> key to parts.getOrElse(index) { "" } }.toMap()
    }

    fun primaryKey(type: QrContentType): String = when (type) {
        QrContentType.TEXT -> "text"
        QrContentType.URL, QrContentType.BOOKMARK -> "url"
        QrContentType.WIFI, QrContentType.ENTERPRISE_WIFI -> "ssid"
        QrContentType.VCARD, QrContentType.MECARD -> "name"
        QrContentType.BIZCARD -> "firstName"
        QrContentType.PHONE, QrContentType.SMS -> "phone"
        QrContentType.EMAIL -> "email"
        QrContentType.GEO -> "latitude"
        QrContentType.EVENT -> "summary"
        QrContentType.YOUTUBE -> "video"
        QrContentType.GOOGLE_PLAY -> "app"
    }

    fun preview(type: QrContentType, content: String): String {
        val values: Map<String, String> = fields(type, content)
        return when (type) {
            QrContentType.WIFI, QrContentType.ENTERPRISE_WIFI -> "Wi-Fi：${values["ssid"].orEmpty()}"
            QrContentType.GEO -> "${values["latitude"].orEmpty()}, ${values["longitude"].orEmpty()}"
            QrContentType.BIZCARD -> values["lastName"].orEmpty() + values["firstName"].orEmpty()
            else -> values[primaryKey(type)].orEmpty()
        }.replace('\n', ' ').take(80)
    }

    fun encode(config: QrConfig): QrData {
        val f: Map<String, String> = fields(config.contentType, config.content)
        fun get(key: String): String = f[key].orEmpty()
        fun optional(key: String): String? = f[key]?.takeIf { it.isNotBlank() }
        return when (config.contentType) {
            QrContentType.TEXT -> QrData.Text(get("text"))
            QrContentType.URL -> QrData.Url(get("url").trim())
            QrContentType.PHONE -> QrData.Phone(get("phone").trim())
            QrContentType.SMS -> QrData.Text("${if (get("mms") == "true") "MMSTO" else "SMSTO"}:${get("phone")}:${get("message")}")
            QrContentType.EMAIL -> QrData.Email(get("email"), optional("copyTo"), optional("subject"), optional("body"))
            QrContentType.WIFI -> QrData.Wifi(
                authentication = when (get("auth").uppercase()) {
                    "OPEN" -> QrData.Wifi.Authentication.OPEN
                    "WEP" -> QrData.Wifi.Authentication.WEP
                    else -> QrData.Wifi.Authentication.WPA
                }, ssid = get("ssid"), psk = if (get("auth") == "OPEN") null else get("password"), hidden = get("hidden") == "true"
            )
            QrContentType.ENTERPRISE_WIFI -> encodeEnterpriseWifi(f)
            QrContentType.VCARD -> encodeVCard(f)
            QrContentType.BIZCARD -> QrData.BizCard(
                escapeCard(get("firstName")), escapeCard(get("lastName")), optional("job")?.let(::escapeCard),
                optional("company")?.let(::escapeCard), optional("address")?.let(::escapeCard), optional("phone"), optional("email")
            )
            QrContentType.MECARD -> QrData.MeCard(escapeCard(get("name")), optional("address")?.let(::escapeCard), optional("phone"), optional("email"))
            QrContentType.GEO -> QrData.GeoPos(get("latitude").toFloatOrNull() ?: 0f, get("longitude").toFloatOrNull() ?: 0f)
            QrContentType.BOOKMARK -> QrData.Bookmark(escapeCard(get("url")), escapeCard(get("title").ifBlank { "网页" }))
            QrContentType.EVENT -> QrData.Event(
                uid = optional("uid"), stamp = optional("stamp")?.let(::calendarTime), organizer = optional("organizer"),
                start = optional("start")?.let(::calendarTime), end = optional("end")?.let(::calendarTime), summary = escapeCard(get("summary"))
            )
            QrContentType.YOUTUBE -> QrData.Url("https://www.youtube.com/watch?v=${extractVideoId(get("video"))}")
            QrContentType.GOOGLE_PLAY -> QrData.Url("https://play.google.com/store/apps/details?id=${extractPackage(get("app"))}")
        }
    }

    private fun encodeEnterpriseWifi(fields: Map<String, String>): QrData = QrData.Text(buildString {
        append("WIFI:T:WPA2-EAP;")
        mapOf("ssid" to "S", "password" to "P", "user" to "I").forEach { (key, tag) ->
            append("$tag:${escapeWifi(fields[key].orEmpty())};")
        }
        append("E:${escapeWifi(fields["eap"].orEmpty().ifBlank { "PEAP" })};")
        fields["phase"]?.takeIf { it.isNotBlank() }?.let { append("PH2:${escapeWifi(it)};") }
        append("H:${fields["hidden"] == "true"};;")
    })

    private fun escapeWifi(value: String): String = buildString {
        value.forEach { character ->
            if (character in charArrayOf('\\', ';', ':', ',', '"')) append('\\')
            append(character)
        }
    }

    private fun encodeVCard(fields: Map<String, String>): QrData = QrData.Text(buildString {
        append("BEGIN:VCARD\nVERSION:3.0\n")
        fields["name"]?.let { append("FN:${escapeCard(it)}\nN:${escapeCard(it)};;;;\n") }
        mapOf("company" to "ORG", "job" to "TITLE", "phone" to "TEL", "email" to "EMAIL", "website" to "URL", "note" to "NOTE").forEach { (key, tag) ->
            fields[key]?.takeIf { it.isNotBlank() }?.let { append("$tag:${escapeCard(it)}\n") }
        }
        fields["address"]?.takeIf { it.isNotBlank() }?.let { append("ADR:;;${escapeCard(it)};;;;\n") }
        append("END:VCARD")
    })

    fun extractVideoId(value: String): String {
        if (!value.startsWith("http")) return value.trim()
        val uri: URI = runCatching { URI(value.trim()) }.getOrNull() ?: return ""
        if (uri.host?.lowercase() in setOf("youtu.be", "www.youtu.be")) return uri.path.trim('/')
        return query(uri, "v") ?: uri.path.substringAfterLast('/')
    }

    fun extractPackage(value: String): String {
        if (!value.startsWith("http")) return value.trim()
        return runCatching { query(URI(value.trim()), "id").orEmpty() }.getOrDefault("")
    }

    private fun query(uri: URI, key: String): String? = uri.rawQuery?.split('&')?.firstOrNull { it.substringBefore('=') == key }
        ?.substringAfter('=')?.let { URLDecoder.decode(it, "UTF-8") }

    private fun calendarTime(value: String): String = LocalDateTime.parse(value, dateFormat).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))

    private fun escapeCard(value: String): String = value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r", "").replace("\n", "\\n")

    private fun readJson(content: String): Map<String, String>? = runCatching {
        val json = JsonParser.parseString(content)
        if (!json.isJsonObject || json.asJsonObject.get(FORM_MARKER)?.asString != "1") return null
        json.asJsonObject.entrySet().filter { it.key != FORM_MARKER }.associate { it.key to it.value.asString }
    }.getOrNull()
}
