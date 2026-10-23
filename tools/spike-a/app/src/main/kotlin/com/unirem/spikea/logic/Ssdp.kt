package com.unirem.spikea.logic

data class SsdpResponse(val headers: Map<String, String>) {
    val location: String? get() = headers["location"]
    val server: String? get() = headers["server"]
    val searchTarget: String? get() = headers["st"]
    val usn: String? get() = headers["usn"]
}

object Ssdp {

    const val MULTICAST_HOST = "239.255.255.250"
    const val MULTICAST_PORT = 1900
    const val SEARCH_TARGET = "urn:samsung.com:device:RemoteControlReceiver:1"
    const val USER_AGENT = "UniRem-SpikeA/0.1 UPnP/1.1"

    private val LOCATION_HOST = Regex("^http://([^/:]+)(?::(\\d+))?/", RegexOption.IGNORE_CASE)

    fun parseResponse(text: String): SsdpResponse? {
        val lines = text.replace("\r\n", "\n").split("\n")
        if (lines.isEmpty() || !lines.first().startsWith("HTTP/", ignoreCase = true)) return null
        val headers = LinkedHashMap<String, String>()
        for (line in lines.drop(1)) {
            val separator = line.indexOf(':')
            if (separator <= 0) continue
            val name = line.substring(0, separator).trim().lowercase()
            val value = line.substring(separator + 1).trim()
            if (name.isNotEmpty()) headers[name] = value
        }
        return SsdpResponse(headers)
    }

    fun hostFromLocation(location: String?): String? {
        if (location.isNullOrBlank()) return null
        return LOCATION_HOST.find(location)?.groupValues?.get(1)
    }

    fun mSearch(searchTarget: String = SEARCH_TARGET): String = buildString {
        append("M-SEARCH * HTTP/1.1\r\n")
        append("HOST: $MULTICAST_HOST:$MULTICAST_PORT\r\n")
        append("MAN: \"ssdp:discover\"\r\n")
        append("MX: 2\r\n")
        append("ST: $searchTarget\r\n")
        append("USER-AGENT: $USER_AGENT\r\n")
        append("\r\n")
    }
}

object HostRules {

    fun isIpv4(value: String): Boolean {
        val parts = value.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            part.isNotEmpty() &&
                part.length <= 3 &&
                part.all { it in '0'..'9' } &&
                part.toIntOrNull() in 0..255
        }
    }

    fun normalizeIp(value: String): String? = value.trim().takeIf { isIpv4(it) }
}
