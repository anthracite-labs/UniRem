package com.unirem.spikea.logic

/**
 * Visible backoff: the harness never reconnects on its own. It only publishes
 * the next moment a manual reconnect is allowed. No queue, no silent retry.
 */
object Backoff {

    private val delaysSeconds = listOf(1L, 2L, 4L, 8L, 16L, 30L)

    fun delaySeconds(attempt: Int): Long = delaysSeconds[attempt.coerceIn(0, delaysSeconds.lastIndex)]

    fun nextAttemptAt(nowMillis: Long, attempt: Int): Long =
        nowMillis + delaySeconds(attempt) * 1_000L

    fun canAttempt(nowMillis: Long, nextAttemptAtMillis: Long): Boolean =
        nowMillis >= nextAttemptAtMillis
}
