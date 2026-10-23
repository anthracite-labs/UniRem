package com.unirem.spikea.logic

/**
 * Submit-to-evidence latency per key group. The recommendation keeps the
 * 3 s default unless runs prove a per-group tail needs more room; it is
 * never drawn from a single sample. Fewer than [MIN_SAMPLES] samples means
 * "too few samples": the caller records blocked, never a recommendation.
 */
object LatencyStats {

    const val DEFAULT_TIMEOUT_MS = 3000L
    const val MIN_SAMPLES = 5
    const val TAIL_MARGIN_MS = 500L

    fun medianMs(samples: List<Long>): Long? {
        if (samples.isEmpty()) return null
        val sorted = samples.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[mid]
        } else {
            sorted[mid - 1] + (sorted[mid] - sorted[mid - 1]) / 2
        }
    }

    /** Nearest-rank 95th percentile tail. */
    fun tailP95Ms(samples: List<Long>): Long? {
        if (samples.isEmpty()) return null
        val sorted = samples.sorted()
        val rank = ((95 * sorted.size) + 99) / 100
        return sorted[(rank - 1).coerceIn(0, sorted.size - 1)]
    }

    /**
     * Recommended wait for evidence, or null when the floor is not met and
     * the caller must record blocked instead.
     */
    fun recommendTimeoutMs(
        samples: List<Long>,
        defaultMs: Long = DEFAULT_TIMEOUT_MS,
        minSamples: Int = MIN_SAMPLES,
    ): Long? {
        if (samples.size < minSamples) return null
        val tail = tailP95Ms(samples) ?: return null
        return maxOf(defaultMs, tail + TAIL_MARGIN_MS)
    }
}

/**
 * Power-off probe interpretation. Evidence is reachability only: the
 * `/api/v2/` info read plus whether the key socket is still open. A TV that
 * answers either is recorded off, asleep, or unknown — still answering
 * means the state is unknown, which is blocked, never a guess.
 */
object PowerProbe {

    fun interpret(apiReachable: Boolean, channelOpen: Boolean): RowResult =
        if (!apiReachable || !channelOpen) RowResult.PASS else RowResult.BLOCKED
}
