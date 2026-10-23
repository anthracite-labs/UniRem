package com.unirem.tv.api

/**
 * Injected backoff schedule (Story 1.7).
 *
 * Base delays in seconds: 1, 2, 4, 8, 16, 30 cap. Production applies ±20%
 * jitter; tests inject a fixed schedule. Reset after 30s continuously
 * connected. Ported from `tools/spike-a/.../logic/Backoff.kt` by value; the
 * spike is never imported (see scope constraint).
 */
fun interface BackoffSchedule {
    /** Delay before attempt [attempt] (0-based), in milliseconds. */
    fun delayMillis(attempt: Int): Long
}

object ConnectionBackoff {

    val BASE_DELAYS_SECONDS = listOf(1L, 2L, 4L, 8L, 16L, 30L)

    const val JITTER_FRACTION = 0.20
    const val RESET_CONNECTED_SECONDS = 30L

    fun baseDelaySeconds(attempt: Int): Long =
        BASE_DELAYS_SECONDS[attempt.coerceIn(0, BASE_DELAYS_SECONDS.lastIndex)]

    /**
     * Applies ±20% jitter. [randomUnit] is a unit-interval double in [0,1);
     * tests inject fixed values, production passes `Random.nextDouble()`.
     */
    fun withJitter(baseMillis: Long, randomUnit: Double): Long {
        val clamped = randomUnit.coerceIn(0.0, 0.999_999)
        val factor = 1.0 - JITTER_FRACTION + 2 * JITTER_FRACTION * clamped
        return (baseMillis * factor).toLong().coerceAtLeast(0L)
    }

    fun delayMillis(attempt: Int, randomUnit: Double): Long =
        withJitter(baseDelaySeconds(attempt) * 1_000L, randomUnit)

    /** True when a continuous connection resets the schedule. */
    fun shouldResetAfterConnected(connectedSeconds: Long): Boolean =
        connectedSeconds >= RESET_CONNECTED_SECONDS

    /** Fixed (jitter-free) schedule for deterministic tests. */
    fun fixed(): BackoffSchedule =
        BackoffSchedule { attempt -> baseDelaySeconds(attempt) * 1_000L }

    /** Production schedule with the caller's randomness. */
    fun jittered(randomUnit: () -> Double): BackoffSchedule =
        BackoffSchedule { attempt -> delayMillis(attempt, randomUnit()) }
}
