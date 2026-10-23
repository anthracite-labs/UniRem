package com.unirem.core.api

import java.util.logging.Level
import java.util.logging.Logger

/**
 * The only logging and redaction seam in UniRem.
 *
 * Policy: no `println`, no direct `android.util.Log` call, no telemetry SDK.
 * Secrets (tokens, PINs, SPKI fingerprints, text payloads, cast media IDs and
 * URLs, full MACs) must pass through [redactIfSecret] and never reach a log.
 */
object RedactedLog {
    const val REDACTED = "[REDACTED]"

    private val logger: Logger = Logger.getLogger("UniRem")

    fun debug(tag: String, message: String) {
        logger.log(Level.FINE, "[$tag] $message")
    }

    fun info(tag: String, message: String) {
        logger.log(Level.INFO, "[$tag] $message")
    }

    fun warn(tag: String, message: String, cause: Throwable? = null) {
        logger.log(Level.WARNING, "[$tag] $message", cause)
    }

    /**
     * Returns [REDACTED] when [isSecret] is true, otherwise returns [value]
     * unchanged. Callers mark every value the privacy policy forbids logging.
     */
    fun redactIfSecret(value: String, isSecret: Boolean): String =
        if (isSecret) REDACTED else value
}
