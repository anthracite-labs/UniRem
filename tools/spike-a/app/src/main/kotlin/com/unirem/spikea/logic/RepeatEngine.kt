package com.unirem.spikea.logic

/**
 * The single repeat engine. Only D-pad directions repeat: the first repeat
 * fires 500 ms after the hold starts, then roughly every 80 ms until the
 * operator lifts and the harness sends `Release`. There is exactly one
 * instance of this class in the harness; a second hold while one is active,
 * or a hold of a non-D-pad key, is refused rather than synthesized through
 * a second path.
 */
object RepeatRules {
    const val INITIAL_DELAY_MS = 500L
    const val CADENCE_MS = 80L

    /**
     * How many repeat frames are due for a hold lasting [heldMs]. Pure math so
     * the cadence is unit-tested without timers; the harness scheduler only
     * fires at this cadence.
     */
    fun repeatsDue(heldMs: Long): Int {
        if (heldMs < INITIAL_DELAY_MS) return 0
        return 1 + ((heldMs - INITIAL_DELAY_MS) / CADENCE_MS).toInt()
    }
}

class RepeatEngine {

    private var activeKey: MvpKey? = null
    private var startMs: Long = 0L
    private var emitted: Int = 0

    /** Starts a hold; false when the key may not repeat or an hold is active. */
    @Synchronized
    fun start(key: MvpKey, nowMs: Long): Boolean {
        if (!KeyFrame.isRepeatable(key)) return false
        if (activeKey != null) return false
        activeKey = key
        startMs = nowMs
        emitted = 0
        return true
    }

    /** Repeat frames due since the last [markEmitted]; 0 when idle. */
    @Synchronized
    fun due(nowMs: Long): Int {
        val key = activeKey ?: return 0
        if (!KeyFrame.isRepeatable(key)) return 0
        return (RepeatRules.repeatsDue(nowMs - startMs) - emitted).coerceAtLeast(0)
    }

    @Synchronized
    fun markEmitted(count: Int) {
        if (activeKey != null && count > 0) emitted += count
    }

    @Synchronized
    fun emittedCount(): Int = emitted

    @Synchronized
    fun active(): MvpKey? = activeKey

    /**
     * Ends the hold. The caller must send the `Release` frame; the return is
     * the key to release, or null when no hold was active.
     */
    @Synchronized
    fun stop(): MvpKey? {
        val key = activeKey
        activeKey = null
        return key
    }
}
