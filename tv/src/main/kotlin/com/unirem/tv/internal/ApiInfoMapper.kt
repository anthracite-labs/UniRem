package com.unirem.tv.internal

/**
 * Raw device fields from `GET <tv>:8001/api/v2/`, before identity resolution.
 * Pure mapping with no Android dependency, so the id/duid fallback and the
 * `TokenAuthSupport` casings are covered by JVM tests. Firmware is kept
 * byte-exact, never normalized.
 */
internal data class ApiDevice(
    val name: String,
    val modelName: String,
    val model: String,
    val firmware: String,
    val tokenAuthSupport: Boolean,
    val wifiMac: String,
    val deviceId: String,
    val deviceIdField: String,
    val powerState: String,
    val reportedNetworkType: String,
)

internal object ApiInfoMapper {

    fun parse(body: String): ApiDevice? {
        val trimmed = body.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null
        // Scope reads to the nested device object when present; the TV wraps
        // fields under "device" on current firmware.
        val scope = deviceScope(trimmed) ?: trimmed
        val reportedId = stringField(scope, "id")
        val reportedDuid = stringField(scope, "duid")
        val deviceId = reportedId.ifBlank { reportedDuid }
        val deviceIdField = when {
            reportedId.isNotBlank() -> "device.id"
            reportedDuid.isNotBlank() -> "device.duid"
            else -> ""
        }
        val tokenAuth = when {
            hasField(scope, "TokenAuthSupport") -> boolField(scope, "TokenAuthSupport")
            hasField(scope, "tokenAuthSupport") -> boolField(scope, "tokenAuthSupport")
            else -> false
        }
        return ApiDevice(
            name = stringField(scope, "name"),
            modelName = stringField(scope, "modelName"),
            model = stringField(scope, "model"),
            firmware = stringField(scope, "softwareVersion"),
            tokenAuthSupport = tokenAuth,
            wifiMac = stringField(scope, "wifiMac"),
            deviceId = deviceId,
            deviceIdField = deviceIdField,
            powerState = stringField(scope, "PowerState"),
            reportedNetworkType = stringField(scope, "networkType"),
        )
    }

    private fun deviceScope(body: String): String? {
        val key = "\"device\""
        val keyIndex = body.indexOf(key)
        if (keyIndex < 0) return null
        val colon = body.indexOf(':', keyIndex + key.length)
        if (colon < 0) return null
        val open = body.indexOf('{', colon)
        if (open < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (i in open until body.length) {
            val c = body[i]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (c == '\\') {
                    escaped = true
                } else if (c == '"') {
                    inString = false
                }
                continue
            }
            when (c) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return body.substring(open, i + 1)
                }
            }
        }
        return null
    }

    private fun stringField(scope: String, key: String): String {
        val pattern = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        val raw = pattern.find(scope)?.groupValues?.get(1) ?: return ""
        return raw.replace(Regex("\\\\(.)")) { it.groupValues[1] }
    }

    private fun boolField(scope: String, key: String): Boolean {
        val pattern = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*(true|false)")
        return pattern.find(scope)?.groupValues?.get(1) == "true"
    }

    private fun hasField(scope: String, key: String): Boolean =
        Regex("\"" + Regex.escape(key) + "\"\\s*:").containsMatchIn(scope)
}
