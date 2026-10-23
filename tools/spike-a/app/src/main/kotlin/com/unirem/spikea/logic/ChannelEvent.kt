package com.unirem.spikea.logic

import org.json.JSONObject

/** One server frame from the Tizen channel. The raw text is never retained. */
data class ChannelEvent(val name: String, val token: String?)

object ChannelEvents {

    /**
     * Tizen sends server frames with `event`; client frames use `method`. Accept
     * either so a firmware that uses one or the other still pairs, and read the
     * token only from `data.token`.
     */
    fun parse(text: String): ChannelEvent = try {
        val json = JSONObject(text)
        val name = json.optString("event").ifBlank { json.optString("method") }
        val token = json.optJSONObject("data")?.optString("token")?.takeIf { it.isNotBlank() }
        ChannelEvent(name, token)
    } catch (e: Exception) {
        ChannelEvent("", null)
    }
}
