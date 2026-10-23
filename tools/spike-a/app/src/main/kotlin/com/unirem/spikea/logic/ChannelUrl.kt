package com.unirem.spikea.logic

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * The control channel is WSS only. There is no function here that can produce a
 * ws:// URL, and no fallback path elsewhere in the harness.
 */
object ChannelUrl {

    const val SCHEME = "wss"
    const val PORT = 8002
    const val APP_NAME = "UniRem Spike A"

    fun build(ip: String, token: String? = null): String {
        val encodedName = URLEncoder.encode(
            Base64.getEncoder().encodeToString(APP_NAME.toByteArray(StandardCharsets.UTF_8)),
            "UTF-8",
        )
        val base = "$SCHEME://$ip:$PORT/api/v2/channels/samsung.remote.control?name=$encodedName"
        return if (token.isNullOrEmpty()) {
            base
        } else {
            "$base&token=${URLEncoder.encode(token, "UTF-8")}"
        }
    }
}
