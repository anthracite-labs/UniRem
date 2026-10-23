package com.unirem.core.api

import java.net.InetAddress

/**
 * Typed purpose for every network destination. The predicate below is the
 * single allow rule for scan, info reads, redirects, channel connects, and
 * the media/wake paths owned by later stories.
 */
enum class NetPurpose {
    Scan,
    InfoRead,
    Redirect,
    Channel,
    Media,
    Wake,
}

/**
 * Single destination predicate for the product (AD-8).
 *
 * Allows RFC1918 IPv4, 169.254/16 link-local IPv4, and fe80::/10 link-local
 * IPv6. Refuses global IPv6, ULA (fc00::/7), loopback, DNS names, blanks, and
 * anything that is not an IP literal. Two narrow allowlists live inside:
 * the SSDP multicast group for [NetPurpose.Scan] only, and the directed
 * broadcast addresses for [NetPurpose.Wake] only.
 *
 * JVM-only; no Android import.
 */
object PrivatePeer {

    const val SSDP_HOST = "239.255.255.250"
    const val SSDP_PORT = 1900

    /**
     * Returns true when [host] is an allowed literal for [purpose] on [port].
     * DNS names always return false; callers reject them before any request.
     */
    fun allowsHost(host: String, port: Int, purpose: NetPurpose): Boolean {
        val value = host.trim()
        if (value.isEmpty()) return false
        if (purpose == NetPurpose.Scan && value == SSDP_HOST && port == SSDP_PORT) return true
        if (purpose == NetPurpose.Wake && isWakeBroadcast(value)) return true
        return allowsIpLiteral(value)
    }

    /** Returns true when [value] is a private literal per the rule above. */
    fun allowsIpLiteral(value: String): Boolean {
        val literal = value.trim()
        if (literal.isEmpty()) return false
        val address = parseLiteral(literal) ?: return false
        if (address.isMulticastAddress) return false
        if (address.isLoopbackAddress) return false
        val bytes = address.address ?: return false
        return when (bytes.size) {
            4 -> isPrivateIpv4(bytes)
            16 -> isLinkLocalIpv6(bytes)
            else -> false
        }
    }

    /** Public IPv4/IPv6 literal check for the manual-IP form (private only). */
    fun isPrivateLiteral(value: String): Boolean = allowsIpLiteral(value)

    private fun isWakeBroadcast(value: String): Boolean =
        value == "255.255.255.255" || (value.endsWith(".255") && allowsIpLiteral(value))

    private fun parseLiteral(value: String): InetAddress? {
        if (!looksLikeIpLiteral(value)) return null
        return try {
            // Strip a zone id (fe80::1%wlan0) before parsing.
            val stripped = value.substringBefore('%').trim()
            if (stripped.isEmpty()) return null
            val address = InetAddress.getByName(stripped)
            // getByName resolves DNS names too; only accept when the input
            // round-trips as a literal (or is the canonical form of one).
            if (!looksLikeIpLiteral(stripped)) return null
            address
        } catch (e: Exception) {
            null
        }
    }

    private fun looksLikeIpLiteral(value: String): Boolean {
        if (value.isEmpty()) return false
        if (value.any { it.isWhitespace() }) return false
        if (value.contains(':')) return isIpv6Chars(value.substringBefore('%'))
        if (value.contains('.')) return isIpv4Chars(value)
        return false
    }

    private fun isIpv4Chars(value: String): Boolean {
        if (value.isEmpty() || value.length > 15) return false
        return value.all { it in '0'..'9' || it == '.' }
    }

    private fun isIpv6Chars(value: String): Boolean {
        if (value.isEmpty()) return false
        return value.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' || it == ':' || it == '.' }
    }

    private fun isPrivateIpv4(bytes: ByteArray): Boolean {
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        return when {
            b0 == 10 -> true
            b0 == 172 && b1 in 16..31 -> true
            b0 == 192 && b1 == 168 -> true
            b0 == 169 && b1 == 254 -> true
            else -> false
        }
    }

    private fun isLinkLocalIpv6(bytes: ByteArray): Boolean {
        // fe80::/10: first 10 bits are 1111111010.
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        return b0 == 0xFE && (b1 and 0xC0) == 0x80
    }
}
