package com.unirem.tv.api

import java.util.Base64

/**
 * Pure SPKI fingerprint formatter for Story 1.6 (JVM-only, no Android import).
 *
 * The stored pin is already the SHA-256 of the leaf SPKI (32 bytes, Base64).
 * This formatter decodes those SHA-256 bytes and renders them as
 * space-separated uppercase hex byte pairs (`AB CD EF ...`) for display in
 * the `code` type. Returns null when the pin is missing or unreadable so the
 * sheet can show "Fingerprint unavailable" with Forget still available.
 * The fingerprint may display but is never logged.
 */
object FingerprintFormat {

    private const val SHA256_BYTES = 32
    private const val HEX = "0123456789ABCDEF"

    fun format(pinBase64: String?): String? {
        if (pinBase64.isNullOrBlank()) return null
        val bytes = try {
            Base64.getDecoder().decode(pinBase64.trim())
        } catch (e: IllegalArgumentException) {
            return null
        }
        if (bytes.size != SHA256_BYTES) return null
        val out = StringBuilder(SHA256_BYTES * 3 - 1)
        for (index in bytes.indices) {
            if (index > 0) out.append(' ')
            val unsigned = bytes[index].toInt() and 0xFF
            out.append(HEX[unsigned ushr 4])
            out.append(HEX[unsigned and 0x0F])
        }
        return out.toString()
    }
}
