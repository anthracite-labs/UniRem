package com.unirem.spikea.logic

/**
 * Where key evidence came from. A socket write is never a source: silence
 * stays unconfirmed, and only one of these three moves a gesture to
 * observed.
 */
enum class EvidenceSource(val wire: String) {
    MACHINE("machine"),
    PROBE("probe"),
    OPERATOR("operator"),
}

/**
 * Honest per-gesture outcome. There is no boolean "confirmed": Submitted is
 * the built frame, Unconfirmed is the socket write, Observed is evidence,
 * Idle is a close without evidence.
 */
enum class GestureState(val wire: String) {
    SUBMITTED("submitted"),
    UNCONFIRMED("unconfirmed"),
    OBSERVED("observed"),
    IDLE("idle"),
}

data class GestureRecord(
    val id: Long,
    val key: MvpKey,
    val generation: Long,
    var state: GestureState,
)

/**
 * One generation per gesture. Every new press bumps the generation, and
 * evidence resolves only when its generation matches the gesture it names:
 * late evidence for an older press can never rewrite a newer press, and a
 * socket close parks every open gesture at Idle.
 */
class GestureTracker {

    private var nextId = 1L
    private var nextGeneration = 1L
    private val records = LinkedHashMap<Long, GestureRecord>()

    @Synchronized
    fun start(key: MvpKey): GestureRecord {
        val record = GestureRecord(nextId++, key, nextGeneration++, GestureState.SUBMITTED)
        records[record.id] = record
        return record
    }

    /** The frame was written to the socket. Still not a TV action. */
    @Synchronized
    fun markWritten(id: Long): Boolean {
        val record = records[id] ?: return false
        if (record.state != GestureState.SUBMITTED) return false
        record.state = GestureState.UNCONFIRMED
        return true
    }

    /**
     * Applies evidence. Returns the record only when [generation] matches the
     * gesture and it is still open; stale, unknown, or already-resolved
     * evidence returns null and changes nothing.
     */
    @Synchronized
    fun observe(id: Long, generation: Long, source: EvidenceSource): GestureRecord? {
        val record = records[id] ?: return null
        if (record.generation != generation) return null
        if (record.state != GestureState.SUBMITTED && record.state != GestureState.UNCONFIRMED) {
            return null
        }
        record.state = GestureState.OBSERVED
        return record
    }

    @Synchronized
    fun unconfirmed(): List<GestureRecord> =
        records.values.filter {
            it.state == GestureState.SUBMITTED || it.state == GestureState.UNCONFIRMED
        }

    @Synchronized
    fun stateOf(id: Long): GestureState? = records[id]?.state

    /** Parks every open gesture at Idle. Observed rows are never rewritten. */
    @Synchronized
    fun closeAll(): List<GestureRecord> {
        val idled = records.values.filter {
            it.state == GestureState.SUBMITTED || it.state == GestureState.UNCONFIRMED
        }
        idled.forEach { it.state = GestureState.IDLE }
        // Resolved records accumulate forever otherwise: drop every idled
        // and previously observed record. Callers that need the rows already
        // hold the returned list.
        val resolved = records.values.filter {
            it.state == GestureState.IDLE || it.state == GestureState.OBSERVED
        }
        resolved.forEach { records.remove(it.id) }
        return idled
    }
}
