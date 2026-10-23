package com.unirem.spikea.logic

/** Row emission callback: the harness adapts this into its redacted `ResultRow`. */
typealias KeyRowSink = (
    scenario: Scenario,
    result: RowResult,
    target: KeyTargetSnapshot,
    notes: String,
    checks: Map<String, String>,
) -> Unit

/**
 * Immutable per-press TV identity, snapshotted at gesture start. Confirm,
 * probe, and close-idle rows use the snapshot, so cycling targets between
 * press and probe can never attribute TV-B evidence to a TV-A gesture.
 * The IP rides along for the power probe only; it never reaches a row.
 */
data class KeyTargetSnapshot(
    val ip: String = "",
    val label: String = "",
    val model: String = "",
    val modelYear: String = "",
    val region: String = "",
    val firmware: String = "",
    val networkType: String = "",
    val networkDetail: String = "",
    val identitySource: String = "",
    val tvIdPrefix: String = "",
    val sessionOnly: Boolean = false,
)

/**
 * Pure key-run orchestration: frame wiring (tap to Click, hold to
 * Press, each due repeat to Click, Release on lift, power to Click
 * KEY_POWER), held-gating (zero sends unless the transport is open),
 * generation-bound evidence, power-probe interpretation, and close
 * semantics (stop repeat, disable keys, park gestures Idle). No Android
 * dependency, so every path is JVM-tested against a fake [KeyTransport].
 * The harness adapts this with vault/discovery/results sinks.
 */
class KeyOrchestrator(
    private val transport: KeyTransport,
    private val clock: () -> Long = System::currentTimeMillis,
    private val onRow: KeyRowSink = { _, _, _, _, _ -> },
    private val onLog: (String) -> Unit = {},
) {

    data class ProbeInput(val apiReachable: Boolean, val phoneOnline: Boolean, val channelOpen: Boolean)

    /** The power press a probe binds to: exact generation plus press-time target. */
    data class PowerPress(val gestureId: Long, val generation: Long, val target: KeyTargetSnapshot)

    @Volatile
    var keysEnabled: Boolean = false
        private set

    @Volatile
    var lastKeyLabel: String = ""
        private set

    @Volatile
    var lastLatencyMs: Long = -1L
        private set

    @Volatile
    var lastEvidence: String = ""
        private set

    @Volatile
    var lastPowerPress: PowerPress? = null
        private set

    private val gestures = GestureTracker()
    private val repeatEngine = RepeatEngine()
    private val gestureTargets = LinkedHashMap<Long, KeyTargetSnapshot>()
    private val sendTimes = LinkedHashMap<Long, Long>()
    private val latencySamples = LinkedHashMap<String, MutableList<Long>>()
    private var holdGestureId: Long? = null
    private var holdKey: MvpKey? = null
    private var lastGestureId: Long? = null

    fun setEnabled(enabled: Boolean) {
        keysEnabled = enabled
    }

    @Synchronized
    fun tap(key: MvpKey, target: KeyTargetSnapshot) {
        if (!transport.isOpen() || !keysEnabled) {
            emit(
                Scenario.KEY, RowResult.BLOCKED, target,
                "keys disabled (session closed); no command sent; no silent command after close",
                keyChecks(key, "click", UNCONFIRMED, -1L),
            )
            onLog("Keys disabled. Open the key session first; nothing was sent.")
            return
        }
        val gesture = gestures.start(key)
        gestureTargets[gesture.id] = target
        // Exactly one frame per gesture.
        if (!transport.send(KeyFrame.click(key))) {
            closeLocked("send failed", recordRows = true)
            emit(
                Scenario.KEY, RowResult.BLOCKED, target,
                "socket write failed; gesture Idle; no queue or retry",
                keyChecks(key, "click", UNCONFIRMED, gesture.generation),
            )
            onLog("Socket write failed. Repeat stopped, gesture Idle, keys disabled; no retry queued.")
            return
        }
        gestures.markWritten(gesture.id)
        sendTimes[gesture.id] = clock()
        lastGestureId = gesture.id
        lastKeyLabel = key.label
        lastEvidence = UNCONFIRMED
        lastLatencyMs = -1L
        emit(
            Scenario.KEY, RowResult.BLOCKED, target,
            "submitted one Click; write is not a TV action; awaiting machine, probe, or operator evidence",
            keyChecks(key, "click", UNCONFIRMED, gesture.generation),
        )
        onLog("Sent ${key.label} (${key.dataOfCmd}) as one Click. Unconfirmed until evidence arrives; mark Observed or Not observed.")
    }

    /**
     * Starts a hold: one Press, then repeats via [holdTick]. True when the
     * scheduler should run; false when nothing was sent.
     */
    @Synchronized
    fun holdStart(key: MvpKey, target: KeyTargetSnapshot): Boolean {
        if (!transport.isOpen() || !keysEnabled) {
            emit(
                Scenario.KEY, RowResult.BLOCKED, target,
                "keys disabled (session closed); no hold started",
                keyChecks(key, "press", UNCONFIRMED, -1L),
            )
            onLog("Keys disabled. Open the key session first; nothing was sent.")
            return false
        }
        if (!KeyFrame.isRepeatable(key)) {
            onLog("${key.label} does not repeat: only D-pad directions repeat. Tap it with the key button instead; nothing was sent.")
            return false
        }
        val now = clock()
        if (!repeatEngine.start(key, now)) {
            onLog("Repeat engine busy: a hold is already active. Release it first; no second engine exists.")
            return false
        }
        val gesture = gestures.start(key)
        gestureTargets[gesture.id] = target
        if (!transport.send(KeyFrame.press(key))) {
            repeatEngine.stop()
            closeLocked("hold press failed", recordRows = true)
            emit(
                Scenario.KEY, RowResult.BLOCKED, target,
                "hold Press write failed; gesture Idle; no retry",
                keyChecks(key, "press", UNCONFIRMED, gesture.generation),
            )
            onLog("Hold Press write failed. Gesture Idle; no retry.")
            return false
        }
        gestures.markWritten(gesture.id)
        sendTimes[gesture.id] = now
        lastGestureId = gesture.id
        lastKeyLabel = key.label
        lastEvidence = UNCONFIRMED
        lastLatencyMs = -1L
        holdGestureId = gesture.id
        holdKey = key
        emit(
            Scenario.KEY, RowResult.BLOCKED, target,
            "hold submitted as one Press; repeats follow at ~${RepeatRules.CADENCE_MS}ms after ${RepeatRules.INITIAL_DELAY_MS}ms; write is not a TV action",
            keyChecks(key, "press", UNCONFIRMED, gesture.generation),
        )
        onLog("Holding ${key.label}: one Press sent; repeats start after ${RepeatRules.INITIAL_DELAY_MS}ms at ~${RepeatRules.CADENCE_MS}ms. Lift with Release hold.")
        return true
    }

    /**
     * Sends the repeats due since the last tick, capped per tick so a stalled
     * scheduler cannot flood the socket. Stops at the first failed send and
     * closes honestly. Returns repeats actually sent.
     */
    @Synchronized
    fun holdTick(): Int {
        val key = holdKey ?: return 0
        holdGestureId ?: return 0
        val due = minOf(repeatEngine.due(clock()), MAX_REPEAT_PER_TICK).coerceAtLeast(0)
        if (due <= 0) return 0
        var sent = 0
        for (i in 0 until due) {
            if (!transport.send(KeyFrame.click(key))) break
            sent++
        }
        repeatEngine.markEmitted(sent)
        if (sent < due) {
            closeLocked("repeat write failed", recordRows = true)
        }
        return sent
    }

    @Synchronized
    fun holdStop(target: KeyTargetSnapshot) {
        val key = repeatEngine.stop()
        holdGestureId = null
        holdKey = null
        if (key == null) {
            onLog("No active hold to release.")
            return
        }
        val repeats = repeatEngine.emittedCount()
        val released = transport.send(KeyFrame.release(key))
        emit(
            Scenario.REPEAT, RowResult.BLOCKED, target,
            "hold ended; repeats sent=$repeats; release ${if (released) "sent" else "NOT sent (socket down)"}; " +
                "phone-side sent count only; drops/reorders are observable on the TV alone; awaiting operator confirmation",
            mapOf(
                "key" to key.label,
                "dataOfCmd" to key.dataOfCmd,
                "frame" to "press-hold-release",
                "repeatCount" to repeats.toString(),
                "repeatCadenceMs" to RepeatRules.CADENCE_MS.toString(),
                "repeatEngine" to "single",
                "releasePossibility" to if (released) "sent" else "not-sent",
                "evidence" to UNCONFIRMED,
            ),
        )
        onLog("Released ${key.label} after $repeats repeat(s); Release ${if (released) "sent" else "NOT sent"}. Confirm the on-TV effect with Observed or Not observed.")
    }

    /**
     * Confirms the last press. Refuses when more than one gesture is open:
     * an ambiguous state never confirms, matching the report contract.
     */
    @Synchronized
    fun observe(source: EvidenceSource, how: String, target: KeyTargetSnapshot) {
        val id = lastGestureId
        if (id == null) {
            onLog("No key press to confirm yet.")
            return
        }
        val open = gestures.unconfirmed()
        if (open.size > 1) {
            emit(
                Scenario.GESTURE, RowResult.BLOCKED, target,
                "ambiguous: ${open.size} gestures open; nothing confirmed; confirm one press at a time",
                mapOf(
                    "evidence" to source.wire,
                    "openGestures" to open.size.toString(),
                ),
            )
            onLog("Ambiguous: ${open.size} gestures open; nothing confirmed. Confirm one press at a time.")
            return
        }
        val record = open.firstOrNull { it.id == id }
        if (record == null) {
            onLog("That gesture already resolved; stale evidence changes nothing.")
            return
        }
        if (gestures.observe(record.id, record.generation, source) == null) {
            onLog("That gesture already resolved; stale evidence changes nothing.")
            return
        }
        val now = clock()
        val sentAt = sendTimes.remove(record.id) ?: now
        val latency = (now - sentAt).coerceAtLeast(0L)
        latencySamples.getOrPut(record.key.group) { mutableListOf() } += latency
        lastLatencyMs = latency
        lastEvidence = source.wire
        val snap = gestureTargets.remove(record.id) ?: target
        emit(
            Scenario.GESTURE, RowResult.PASS, snap,
            "$how for ${record.key.label}; submit-to-evidence ${latency}ms",
            mapOf(
                "key" to record.key.label,
                "dataOfCmd" to record.key.dataOfCmd,
                "group" to record.key.group,
                "evidence" to source.wire,
                "latencyMs" to latency.toString(),
                "generation" to record.generation.toString(),
            ),
        )
        onLog("Observed ${record.key.label} via ${source.wire} (${latency}ms submit-to-evidence).")
    }

    @Synchronized
    fun notObserved(target: KeyTargetSnapshot) {
        val id = lastGestureId
        if (id == null) {
            onLog("No key press to judge yet.")
            return
        }
        val record = gestures.unconfirmed().firstOrNull { it.id == id }
        if (record == null) {
            onLog("That gesture already resolved; nothing stays open.")
            return
        }
        val snap = gestureTargets[id] ?: target
        emit(
            Scenario.GESTURE, RowResult.BLOCKED, snap,
            "no on-TV effect for ${record.key.label}; stays unconfirmed, never failed, never green",
            keyChecks(record.key, "click", UNCONFIRMED, record.generation),
        )
        lastEvidence = UNCONFIRMED
        onLog("No effect observed for ${record.key.label}. Stays unconfirmed.")
    }

    @Synchronized
    fun unsupported(key: MvpKey, target: KeyTargetSnapshot) {
        emit(
            Scenario.UNSUPPORTED_KEY, RowResult.PASS, target,
            "${key.label} (${key.dataOfCmd}) ignored by this firmware; usable keys unaffected; " +
                "candidate for the one-time capability chip; never failed",
            mapOf(
                "key" to key.label,
                "dataOfCmd" to key.dataOfCmd,
                "supported" to "false",
                "evidence" to EvidenceSource.OPERATOR.wire,
            ),
        )
        onLog("Recorded ${key.label} as unsupported on this firmware. Usable keys are unaffected.")
    }

    @Synchronized
    fun markLastUnsupported(target: KeyTargetSnapshot) {
        val label = lastKeyLabel
        if (label.isBlank()) {
            onLog("No key press yet. Press the key first, then mark it unsupported.")
            return
        }
        val key = MvpKey.entries.firstOrNull { it.label == label }
        if (key == null) {
            onLog("Unknown last key. Nothing recorded.")
            return
        }
        unsupported(key, target)
    }

    /**
     * Sends the distinct power key once. There is no toggle behavior. Returns
     * the press for the delayed probe, or null when nothing was sent.
     */
    @Synchronized
    fun powerSend(target: KeyTargetSnapshot): PowerPress? {
        val key = MvpKey.POWER
        if (!transport.isOpen() || !keysEnabled) {
            emit(
                Scenario.POWER_OFF, RowResult.BLOCKED, target,
                "keys disabled (session closed); power key not sent",
                powerChecks(UNCONFIRMED, "no-hold", "session-closed"),
            )
            onLog("Keys disabled. Open the key session first; power was not sent.")
            return null
        }
        val gesture = gestures.start(key)
        gestureTargets[gesture.id] = target
        if (!transport.send(KeyFrame.click(key))) {
            closeLocked("power send failed", recordRows = true)
            emit(
                Scenario.POWER_OFF, RowResult.BLOCKED, target,
                "power write failed; gesture Idle; no retry",
                powerChecks(UNCONFIRMED, "no-hold", "send-failed"),
            )
            onLog("Power write failed. Gesture Idle; no retry.")
            return null
        }
        gestures.markWritten(gesture.id)
        sendTimes[gesture.id] = clock()
        lastGestureId = gesture.id
        lastKeyLabel = key.label
        lastEvidence = UNCONFIRMED
        val press = PowerPress(gesture.id, gesture.generation, target)
        lastPowerPress = press
        emit(
            Scenario.KEY, RowResult.BLOCKED, target,
            "distinct power key sent once as Click; no toggle; write is not a TV action",
            keyChecks(key, "click", UNCONFIRMED, gesture.generation),
        )
        return press
    }

    /** State probe after a power press, bound to the press-time target. */
    @Synchronized
    fun probePower(press: PowerPress, input: ProbeInput) {
        if (!input.phoneOnline) {
            emit(
                Scenario.POWER_OFF, RowResult.BLOCKED, press.target,
                "phone offline; probe inconclusive; power gesture stays unconfirmed",
                mapOf(
                    "key" to MvpKey.POWER.label,
                    "dataOfCmd" to MvpKey.POWER.dataOfCmd,
                    "evidence" to EvidenceSource.PROBE.wire,
                    "generation" to press.generation.toString(),
                    "powerProbe" to "phone-offline",
                    "releasePossibility" to "no-hold",
                ),
            )
            onLog("Probe inconclusive: phone offline. Power gesture stays unconfirmed.")
            return
        }
        val verdict = PowerProbe.interpret(input.apiReachable, input.channelOpen)
        if (verdict == RowResult.PASS) {
            // Best effort: the gesture may already be Idle from the close the
            // power-off itself caused. The probe row stands either way.
            gestures.observe(press.gestureId, press.generation, EvidenceSource.PROBE)
            sendTimes.remove(press.gestureId)
            gestureTargets.remove(press.gestureId)
            lastEvidence = EvidenceSource.PROBE.wire
        }
        emit(
            Scenario.POWER_OFF, verdict, press.target,
            if (verdict == RowResult.PASS) {
                "probe evidences off, asleep, or unknown: 8001 ${if (input.apiReachable) "answers" else "silent"}, " +
                    "channel ${if (input.channelOpen) "open" else "closed"}"
            } else {
                "TV still answers 8001 and the channel is open; state unknown; unobserved stays unconfirmed, never a sheet"
            },
            mapOf(
                "key" to MvpKey.POWER.label,
                "dataOfCmd" to MvpKey.POWER.dataOfCmd,
                "evidence" to EvidenceSource.PROBE.wire,
                "generation" to press.generation.toString(),
                "powerProbe" to "8001=${if (input.apiReachable) "answers" else "silent"};8002=${if (input.channelOpen) "open" else "closed"}",
                "releasePossibility" to "no-hold",
            ),
        )
        onLog(
            if (verdict == RowResult.PASS) {
                "Power probe: TV off, asleep, or unknown (8001 ${if (input.apiReachable) "answers" else "silent"}, channel ${if (input.channelOpen) "open" else "closed"})."
            } else {
                "Power probe: TV still answers; state unknown. Stays unconfirmed; probe again later with Probe power."
            },
        )
    }

    @Synchronized
    fun isUnconfirmed(gestureId: Long): Boolean =
        gestures.unconfirmed().any { it.id == gestureId }

    @Synchronized
    fun latencySummary(target: KeyTargetSnapshot) {
        val groups = latencySamples.keys.sorted()
        if (groups.isEmpty()) {
            emit(
                Scenario.LATENCY, RowResult.BLOCKED, target,
                "no submit-to-evidence samples yet; too few samples is blocked, not a recommendation",
                mapOf("sampleCount" to "0"),
            )
            onLog("No latency samples yet. Confirm key presses with Observed first.")
            return
        }
        groups.forEach { group ->
            val samples = latencySamples[group]?.toList().orEmpty()
            val median = LatencyStats.medianMs(samples)
            val tail = LatencyStats.tailP95Ms(samples)
            val recommendation = LatencyStats.recommendTimeoutMs(samples)
            if (recommendation == null) {
                emit(
                    Scenario.LATENCY, RowResult.BLOCKED, target,
                    "group $group has ${samples.size} sample(s), floor is ${LatencyStats.MIN_SAMPLES}; too few samples is blocked, not a recommendation",
                    mapOf(
                        "group" to group,
                        "sampleCount" to samples.size.toString(),
                        "medianMs" to (median?.toString() ?: ""),
                        "tailMs" to (tail?.toString() ?: ""),
                    ),
                )
                onLog("Latency $group: ${samples.size} sample(s), below the floor of ${LatencyStats.MIN_SAMPLES}. No recommendation.")
            } else {
                val keepsDefault = recommendation == LatencyStats.DEFAULT_TIMEOUT_MS
                emit(
                    Scenario.LATENCY, RowResult.PASS, target,
                    "group $group from ${samples.size} run(s); " +
                        if (keepsDefault) {
                            "3 s default kept"
                        } else {
                            "per-group recommendation ${recommendation}ms replaces the 3 s default"
                        },
                    mapOf(
                        "group" to group,
                        "sampleCount" to samples.size.toString(),
                        "medianMs" to (median?.toString() ?: ""),
                        "tailMs" to (tail?.toString() ?: ""),
                        "recommendationMs" to recommendation.toString(),
                        "defaultTimeoutMs" to LatencyStats.DEFAULT_TIMEOUT_MS.toString(),
                    ),
                )
                onLog("Latency $group: median ${median}ms, tail ${tail}ms over ${samples.size} run(s); recommendation ${recommendation}ms.")
            }
        }
    }

    @Synchronized
    fun close(reason: String, recordRows: Boolean) {
        closeLocked(reason, recordRows)
    }

    /**
     * Machine events are logged with the open-gesture context but never
     * auto-confirm: the harness cannot know which event evidences which press,
     * and a delayed event must never rewrite a newer press. The operator binds
     * an event to its exact generation with Observed-via-machine.
     */
    @Synchronized
    fun noteMachineEvent(name: String) {
        val open = gestures.unconfirmed()
        if (open.isEmpty()) {
            onLog("machine event with no open gesture; ignored, never attributed to a newer press.")
            return
        }
        val latest = open.maxByOrNull { it.generation }
        onLog(
            "machine event during ${latest?.key?.label} (generation ${latest?.generation}); " +
                "use Observed: machine to bind it, or leave the gesture unconfirmed.",
        )
    }

    @Synchronized
    fun onTransportClosed(code: Int, reason: String) {
        closeLocked("socket closed ($code): $reason", recordRows = true)
        onLog("Key socket closed ($code). Repeat stopped, keys disabled. No command will be sent until Open keys.")
    }

    private fun closeLocked(reason: String, recordRows: Boolean) {
        val heldKey = repeatEngine.stop()
        holdGestureId = null
        holdKey = null
        // Best-effort Release possibility is recorded, never assumed.
        val releasePossibility = if (heldKey != null) {
            if (transport.send(KeyFrame.release(heldKey))) "release-sent" else "release-unknown"
        } else {
            "no-hold"
        }
        transport.close(reason)
        keysEnabled = false
        val idled = gestures.closeAll()
        sendTimes.clear()
        if (recordRows) {
            idled.forEach { record ->
                val snap = gestureTargets.remove(record.id) ?: KeyTargetSnapshot()
                emit(
                    Scenario.GESTURE, RowResult.BLOCKED, snap,
                    "key session closed ($reason); ${record.key.label} Idle; no queue or retry",
                    mapOf(
                        "key" to record.key.label,
                        "dataOfCmd" to record.key.dataOfCmd,
                        "evidence" to UNCONFIRMED,
                        "generation" to record.generation.toString(),
                        "releasePossibility" to releasePossibility,
                    ),
                )
            }
            onLog("Key session closed ($reason). ${idled.size} gesture(s) Idle; release possibility: $releasePossibility.")
        }
    }

    private fun emit(
        scenario: Scenario,
        result: RowResult,
        target: KeyTargetSnapshot,
        notes: String,
        checks: Map<String, String>,
    ) = onRow(scenario, result, target, notes, checks)

    private fun keyChecks(key: MvpKey, frame: String, evidence: String, generation: Long): Map<String, String> =
        mapOf(
            "key" to key.label,
            "dataOfCmd" to key.dataOfCmd,
            "group" to key.group,
            "frame" to frame,
            "evidence" to evidence,
            "generation" to generation.toString(),
        )

    private fun powerChecks(evidence: String, hold: String, probe: String): Map<String, String> =
        mapOf(
            "key" to MvpKey.POWER.label,
            "dataOfCmd" to MvpKey.POWER.dataOfCmd,
            "evidence" to evidence,
            "releasePossibility" to hold,
            "powerProbe" to probe,
        )

    companion object {
        /** Repeats sent per tick at most, so a stalled scheduler cannot flood the socket. */
        const val MAX_REPEAT_PER_TICK = 4
        const val UNCONFIRMED = "unconfirmed"
    }
}
