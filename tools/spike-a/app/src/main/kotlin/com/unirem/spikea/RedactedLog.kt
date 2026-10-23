package com.unirem.spikea

import com.unirem.spikea.logic.Redaction

/** The only log sink in the harness. No println and no android.util.Log. */
object RedactedLog {
    fun line(message: String): String = Redaction.scrub(message)
}
