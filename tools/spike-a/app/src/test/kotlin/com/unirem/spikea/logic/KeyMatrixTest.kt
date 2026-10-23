package com.unirem.spikea.logic

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One covering test per I/O matrix row of the key-delivery spike. These pin
 * the pure-logic rules; hardware rows (real Allow, real prompt, real
 * re-prompt) stay `blocked` in `spike-a-keys.md` and no test stands in for
 * them.
 */
class KeyMatrixTest {

    private fun row(
        scenario: Scenario = Scenario.KEY,
        result: RowResult = RowResult.BLOCKED,
        tvIdPrefix: String = "0123456789ab",
        model: String = "QN90C",
        firmware: String = "T-KTM2DEUC_1520.1",
        networkType: String = "Wifi",
        notes: String = "",
        checks: Map<String, String> = emptyMap(),
    ) = ResultRow(
        runId = "run-1",
        at = "2026-10-23T10:00:00Z",
        scenario = scenario,
        result = result,
        tvIdPrefix = tvIdPrefix,
        model = model,
        firmware = firmware,
        networkType = networkType,
        notes = notes,
        checks = checks,
    )

    @Test
    fun pairedSetup_missingPairingBlocksRatherThanPasses() {
        val rows = listOf(
            row(scenario = Scenario.KEY, result = RowResult.BLOCKED, notes = "no stored pairing"),
            row(scenario = Scenario.KEY, result = RowResult.BLOCKED, notes = "no stored pairing"),
            row(scenario = Scenario.KEY, result = RowResult.BLOCKED, notes = "no stored pairing"),
        )
        assertEquals(GateVerdict.BLOCKED, GateRules.verdict(rows))
    }

    @Test
    fun mvpKey_oneGestureIsExactlyOneFrameWithTheRightCode() {
        MvpKey.entries.forEach { key ->
            val parsed = KeyFrame.parse(KeyFrame.click(key))
            assertEquals("Click", parsed!!.cmd)
            assertEquals(key.dataOfCmd, parsed.dataOfCmd)
            assertEquals(key, MvpKey.fromDataOfCmd(parsed.dataOfCmd))
        }
        // A duplicate submit would be a second frame for one gesture; the
        // builder returns a single frame string carrying exactly one command.
        assertEquals(1, "\"DataOfCmd\"".toRegex().findAll(KeyFrame.click(MvpKey.OK)).count())
    }

    @Test
    fun evidence_writeIsNeverATvAction() {
        val tracker = GestureTracker()
        val gesture = tracker.start(MvpKey.DPAD_DOWN)
        tracker.markWritten(gesture.id)
        assertEquals(GestureState.UNCONFIRMED, tracker.stateOf(gesture.id))
        // Only a machine event, a state probe, or the operator moves it.
        assertTrue(tracker.observe(gesture.id, gesture.generation, EvidenceSource.MACHINE) != null)
    }

    @Test
    fun latency_medianTailFloorAndRecommendation() {
        val samples = listOf(180L, 220L, 200L, 260L, 240L, 210L)
        assertEquals(215L, LatencyStats.medianMs(samples))
        assertTrue(LatencyStats.tailP95Ms(samples) != null)
        assertEquals(3_000L, LatencyStats.recommendTimeoutMs(samples))
        assertNull(LatencyStats.recommendTimeoutMs(samples.take(2)))
    }

    @Test
    fun repeat_singleEngineDpadOnlyWithReleaseOnLift() {
        val engine = RepeatEngine()
        assertTrue(engine.start(MvpKey.DPAD_UP, 0L))
        assertFalse(engine.start(MvpKey.DPAD_UP, 600L))
        assertFalse(engine.start(MvpKey.MUTE, 600L))
        assertEquals(1, engine.due(500L))
        assertEquals(MvpKey.DPAD_UP, engine.stop())
        assertEquals(KeyFrame.parse(KeyFrame.release(MvpKey.DPAD_UP))!!.cmd, "Release")
    }

    @Test
    fun lateEvidence_olderPressNeverRewritesANewerPress() {
        val tracker = GestureTracker()
        val older = tracker.start(MvpKey.HOME)
        tracker.markWritten(older.id)
        val newer = tracker.start(MvpKey.HOME)
        tracker.markWritten(newer.id)
        assertNull(tracker.observe(newer.id, older.generation, EvidenceSource.OPERATOR))
        assertEquals(GestureState.UNCONFIRMED, tracker.stateOf(newer.id))
    }

    @Test
    fun socketClose_repeatStopsGestureIdleKeysDisabled() {
        val tracker = GestureTracker()
        val engine = RepeatEngine()
        assertTrue(engine.start(MvpKey.DPAD_LEFT, 0L))
        val gesture = tracker.start(MvpKey.DPAD_LEFT)
        tracker.markWritten(gesture.id)
        // Close mid-press: engine stopped, gesture Idle, evidence frozen. The
        // resolved record is dropped from the map so it cannot accumulate.
        engine.stop()
        val idled = tracker.closeAll()
        assertEquals(1, idled.size)
        assertNull(tracker.stateOf(gesture.id))
        assertNull(tracker.observe(gesture.id, gesture.generation, EvidenceSource.MACHINE))
    }

    @Test
    fun powerOff_distinctKeyWithProbeEvidence() {
        assertEquals("KEY_POWER", MvpKey.POWER.dataOfCmd)
        assertEquals(1, MvpKey.entries.count { it.dataOfCmd == "KEY_POWER" })
        assertEquals(RowResult.BLOCKED, PowerProbe.interpret(apiReachable = true, channelOpen = true))
        assertEquals(RowResult.PASS, PowerProbe.interpret(apiReachable = false, channelOpen = false))
    }

    @Test
    fun newScenariosSerializeAndNewCheckKeysSurviveTheFilter() {
        val wires = mapOf(
            Scenario.KEY to "key",
            Scenario.REPEAT to "repeat",
            Scenario.LATENCY to "latency",
            Scenario.GESTURE to "gesture",
            Scenario.POWER_OFF to "power-off",
            Scenario.UNSUPPORTED_KEY to "unsupported-key",
        )
        wires.forEach { (scenario, wire) ->
            val line = ResultsFormat.line(row(scenario = scenario))
            assertEquals(wire, JSONObject(line).getString("scenario"))
        }
        val keys = listOf(
            "key", "dataOfCmd", "group", "frame", "evidence", "latencyMs",
            "generation", "openGestures", "repeatCount", "repeatCadenceMs", "repeatEngine",
            "releaseSent", "releasePossibility", "powerProbe", "supported",
            "sampleCount", "medianMs", "tailMs", "recommendationMs", "defaultTimeoutMs",
        )
        keys.forEach { assertTrue("dropped key evidence: $it", Redaction.keyAllowed(it)) }
        val surviving = JSONObject(
            ResultsFormat.line(row(checks = keys.associateWith { "true" })),
        ).getJSONObject("checks")
        keys.forEach { assertTrue("filtered key evidence: $it", surviving.has(it)) }
    }

    @Test
    fun keyRowsCarryNoSecret() {
        val line = ResultsFormat.line(
            row(
                scenario = Scenario.GESTURE,
                result = RowResult.PASS,
                notes = "observed via operator",
                checks = mapOf(
                    "key" to "Up",
                    "dataOfCmd" to "KEY_UP",
                    "evidence" to "operator",
                    "latencyMs" to "210",
                    "generation" to "3",
                ),
            ),
        )
        assertFalse(Redaction.containsMac(line))
        assertFalse(Redaction.containsLongSecret(line))
    }
}
