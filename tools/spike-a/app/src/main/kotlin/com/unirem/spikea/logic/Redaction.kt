package com.unirem.spikea.logic

import java.util.Locale

/**
 * Redaction is deliberately boring: the results writer never accepts a token,
 * pin, or SPKI field, and free text is scrubbed before it is written.
 */
object Redaction {

    const val REDACTED = "<redacted>"
    const val MAC_PLACEHOLDER = "xx:xx:xx:xx:xx:??"

    private val MAC = Regex("(?i)\\b[0-9a-f]{2}(?:[:-][0-9a-f]{2}){5}\\b")
    private val LONG_HEX = Regex("(?i)\\b[0-9a-f]{32,}\\b")
    private val LONG_BASE64 = Regex("[A-Za-z0-9+/]{40,}={0,2}")
    private val TOKEN_PARAM = Regex("([?&])token=[^&\\s]*")

    private val FORBIDDEN_KEY_PARTS = listOf(
        "token",
        "pin",
        "spki",
        "mac",
        "secret",
        "cert",
        "authorization",
    )

    fun mac(raw: String?): String = if (raw.isNullOrBlank()) "" else MAC_PLACEHOLDER

    fun scrub(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        return text
            .replace(MAC, MAC_PLACEHOLDER)
            .replace(LONG_HEX, REDACTED)
            .replace(LONG_BASE64, REDACTED)
    }

    fun containsMac(text: String): Boolean = MAC.containsMatchIn(text)

    fun containsLongSecret(text: String): Boolean =
        LONG_HEX.containsMatchIn(text) || LONG_BASE64.containsMatchIn(text)

    /**
     * Exception text can echo the request URL, token query included. Strip any
     * `token=` parameter before the message reaches a log or a results row.
     */
    fun withoutTokenParam(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        return text.replace(TOKEN_PARAM, "$1token=" + REDACTED)
    }

    /** A checks map may not smuggle a secret in through its key. */
    fun keyAllowed(key: String): Boolean {
        val lowered = key.lowercase(Locale.ROOT)
        return lowered.isNotBlank() && FORBIDDEN_KEY_PARTS.none { lowered.contains(it) }
    }
}
