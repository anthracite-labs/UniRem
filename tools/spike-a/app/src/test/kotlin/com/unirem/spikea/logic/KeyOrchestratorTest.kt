package com.unirem.spikea.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Fake socket: scriptable openness, counts every send attempt. */
class FakeKeyTransport(var open: Boolean = true) : KeyTransport {
    val sent = mutableListOf<String>()
    var attempts = 0
    var failAfter: Int = Int.MAX_VALUE
    val closedReasons = mutableListOf<String>()

    override fun isOpen(): Boolean = open

    override fun send(frameJson: String): Boolean {
        attempts++
        if (!open || sent.size >= failAfter) return false
        sent += frameJson
        return true
    }

    override fun close(reason: String) {
        open = false
        closedReasons += reason
    }
}

data class CapturedRow(
    val scenario: Scenario,
    val result: RowResult,
    val target: KeyTargetSnapshot,
    val notes: String,
    val checks: Map<String, String>,
)

/**
 * Frame wiring, held-gating, and orchestration against a fake transport:
 * every assertion the review asked for on tap/hold/repeat/release/power,
 * closed sessions, close semantics, generation-bound evidence, probes, and
 * machine events.
 */
class KeyOrchestratorTest {

    private lateinit var transport: FakeKeyTransport
    private lateinit var rows: MutableList<CapturedRow>
    private lateinit var logs: MutableList<String>
    private lateinit var orch: KeyOrchestrator
    private var now = 1_000_000L

    private val tvA = KeyTargetSnapshot(
        ip = "192.168.1.10", label = "TV-A", model = "QN90C",
        modelYear = "2024", region = "EU", firmware = "T-KTM2DEUC_1520.1",
        networkType = "Wifi", networkDetail = "Wifi",
        identitySource = "device-id", tvIdPrefix = "aaaaaaaaaaaa",
    )
    private val tvB = KeyTargetSnapshot(
        ip = "192.168.1.11", label = "TV-B", model = "QN90C",
        modelYear = "2024", region = "EU", firmware = "T-KTM2DEUC_1520.1",
        networkType = "Wifi", networkDetail = "Wifi",
        identitySource = "device-id", tvIdPrefix = "bbbbbbbbbbbb",
    )

    @Before
    fun setUp() {
        transport = FakeKeyTransport()
        rows = mutableListOf()
        logs = mutableListOf()
        now = 1_000_000L
        orch = KeyOrchestrator(
            transport = transport,
            clock = { now },
            onRow = { scenario, result, target, notes, checks ->
                rows += CapturedRow(scenario, result, target, notes, checks)
            },
            onLog = { logs += it },
        )
        orch.setEnabled(true)
    }

    private fun parsed(index: Int): KeyFrame.ParsedFrame =
        KeyFrame.parse(transport.sent[index]) ?: throw AssertionError("unparseable frame $index")

    private fun lastRow(): CapturedRow = rows.last()

    @Test
    fun tapSendsExactlyOneClick() {
        orch.tap(MvpKey.OK, tvA)
        assertEquals(1, transport.sent.size)
        assertEquals("Click", parsed(0).cmd)
        assertEquals("KEY_ENTER", parsed(0).dataOfCmd)
        assertEquals(Scenario.KEY, lastRow().scenario)
        assertEquals(RowResult.BLOCKED, lastRow().result)
        assertEquals("click", lastRow().checks["frame"])
        assertEquals("unconfirmed", lastRow().checks["evidence"])
    }

    @Test
    fun holdStartSendsOnePressRepeatsSendClicksHoldStopSendsRelease() {
        assertTrue(orch.holdStart(MvpKey.DPAD_UP, tvA))
        assertEquals("Press", parsed(0).cmd)

        now += 500L
        assertEquals(1, orch.holdTick())
        now += 80L
        assertEquals(1, orch.holdTick())
        assertEquals(
            listOf("Press", "Click", "Click"),
            transport.sent.mapIndexed { index, _ -> parsed(index).cmd },
        )

        orch.holdStop(tvA)
        assertEquals("Release", parsed(3).cmd)
        assertEquals("KEY_UP", parsed(3).dataOfCmd)
        val repeat = rows.first { it.scenario == Scenario.REPEAT }
        assertEquals("2", repeat.checks["repeatCount"])
        assertEquals("sent", repeat.checks["releasePossibility"])
        assertFalse(repeat.checks.containsKey("releaseSent"))
    }

    @Test
    fun powerSendsExactlyOneClickPower() {
        val press = orch.powerSend(tvA)
        assertNotNull(press)
        assertEquals(1, transport.sent.size)
        assertEquals("Click", parsed(0).cmd)
        assertEquals("KEY_POWER", parsed(0).dataOfCmd)
        assertEquals(tvA, press!!.target)
    }

    @Test
    fun closedSessionSendsNothing() {
        transport.open = false
        orch.tap(MvpKey.OK, tvA)
        assertFalse(orch.holdStart(MvpKey.DPAD_UP, tvA))
        assertNull(orch.powerSend(tvA))
        assertTrue(transport.sent.isEmpty())
        assertEquals(0, transport.attempts)
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it.result == RowResult.BLOCKED })
        assertTrue(logs.any { it.contains("Keys disabled") })
    }

    @Test
    fun repeatTickCapsAtFourPerTick() {
        orch.holdStart(MvpKey.DPAD_DOWN, tvA)
        now += 500L + 80L * 20L
        assertEquals(4, orch.holdTick())
        assertEquals(1 + 4, transport.sent.size)
        assertEquals(4, orch.holdTick())
        assertEquals(1 + 8, transport.sent.size)
    }

    @Test
    fun repeatSendFailureBreaksAndCloses() {
        orch.holdStart(MvpKey.DPAD_LEFT, tvA)
        transport.open = false
        now += 580L
        val before = rows.size
        assertEquals(0, orch.holdTick())
        assertFalse(orch.keysEnabled)
        // Press attempt + one failed repeat attempt + the best-effort Release
        // attempt during close: a break, not a flood.
        assertEquals(3, transport.attempts)
        assertEquals(1, transport.sent.size)
        val idle = rows.drop(before).first { it.scenario == Scenario.GESTURE }
        assertEquals(RowResult.BLOCKED, idle.result)
        assertEquals("release-unknown", idle.checks["releasePossibility"])
        assertEquals(0, orch.holdTick())
    }

    @Test
    fun closeStopsDisablesIdlesWithReleasePossibility() {
        orch.holdStart(MvpKey.DPAD_RIGHT, tvA)
        val before = rows.size
        orch.close("manual", recordRows = true)
        assertFalse(orch.keysEnabled)
        assertTrue(transport.closedReasons.isNotEmpty())
        val idle = rows.drop(before).first { it.scenario == Scenario.GESTURE }
        assertEquals(RowResult.BLOCKED, idle.result)
        assertEquals("TV-A", idle.target.label)
        assertEquals("release-sent", idle.checks["releasePossibility"])
        val count = rows.size
        orch.close("manual", recordRows = true)
        assertEquals(count, rows.size)
    }

    @Test
    fun socketClosedPathDisablesAndIdles() {
        orch.tap(MvpKey.MUTE, tvA)
        transport.open = false
        val before = rows.size
        orch.onTransportClosed(1006, "gone")
        assertFalse(orch.keysEnabled)
        val idle = rows.drop(before).first { it.scenario == Scenario.GESTURE }
        assertEquals("Mute", idle.checks["key"])
        assertEquals("no-hold", idle.checks["releasePossibility"])
        val count = rows.size
        orch.onTransportClosed(1006, "gone")
        assertEquals(count, rows.size)
    }

    @Test
    fun singleOpenGestureObservesWithGeneration() {
        orch.tap(MvpKey.HOME, tvA)
        val submit = lastRow()
        orch.observe(EvidenceSource.OPERATOR, "operator saw it", tvA)
        val confirmed = lastRow()
        assertEquals(Scenario.GESTURE, confirmed.scenario)
        assertEquals(RowResult.PASS, confirmed.result)
        assertEquals("operator", confirmed.checks["evidence"])
        assertEquals(submit.checks["generation"], confirmed.checks["generation"])
        assertEquals("Home", confirmed.checks["key"])
        assertEquals("0", confirmed.checks["latencyMs"])
    }

    @Test
    fun ambiguousStateRefusesToConfirm() {
        orch.tap(MvpKey.OK, tvA)
        orch.tap(MvpKey.BACK, tvA)
        orch.observe(EvidenceSource.OPERATOR, "operator saw it", tvA)
        val refused = lastRow()
        assertEquals(Scenario.GESTURE, refused.scenario)
        assertEquals(RowResult.BLOCKED, refused.result)
        assertEquals("2", refused.checks["openGestures"])
        // Neither gesture resolved: a second attempt still refuses.
        orch.observe(EvidenceSource.OPERATOR, "operator saw it", tvA)
        assertEquals("2", lastRow().checks["openGestures"])
        assertTrue(logs.any { it.contains("Ambiguous") })
    }

    @Test
    fun probePassObservesAndBlockedLeavesUnconfirmed() {
        val press = orch.powerSend(tvA)!!
        orch.probePower(press, KeyOrchestrator.ProbeInput(apiReachable = false, phoneOnline = true, channelOpen = false))
        assertEquals(Scenario.POWER_OFF, lastRow().scenario)
        assertEquals(RowResult.PASS, lastRow().result)
        assertEquals("probe", lastRow().checks["evidence"])
        assertFalse(orch.isUnconfirmed(press.gestureId))

        val press2 = orch.powerSend(tvA)!!
        orch.probePower(press2, KeyOrchestrator.ProbeInput(apiReachable = true, phoneOnline = true, channelOpen = true))
        assertEquals(Scenario.POWER_OFF, lastRow().scenario)
        assertEquals(RowResult.BLOCKED, lastRow().result)
        assertTrue(orch.isUnconfirmed(press2.gestureId))
    }

    @Test
    fun phoneOfflineProbeIsBlockedWithoutObserving() {
        val press = orch.powerSend(tvA)!!
        orch.probePower(press, KeyOrchestrator.ProbeInput(apiReachable = false, phoneOnline = false, channelOpen = false))
        assertEquals(Scenario.POWER_OFF, lastRow().scenario)
        assertEquals(RowResult.BLOCKED, lastRow().result)
        assertTrue(lastRow().notes.contains("phone offline"))
        assertEquals("phone-offline", lastRow().checks["powerProbe"])
        assertTrue(orch.isUnconfirmed(press.gestureId))
        assertEquals("unconfirmed", orch.lastEvidence)
    }

    @Test
    fun bareMachineEventNeverObserves() {
        orch.tap(MvpKey.VOL_UP, tvA)
        orch.noteMachineEvent("ms.channel.ready")
        assertTrue(rows.none { it.scenario == Scenario.GESTURE })
        orch.noteMachineEvent("volume-changed")
        assertTrue(rows.none { it.scenario == Scenario.GESTURE })
        assertTrue(logs.any { it.contains("machine event during Vol+") })
    }

    @Test
    fun confirmAndProbeRowsUseThePressTimeSnapshot() {
        orch.tap(MvpKey.OK, tvA)
        // Target cycled to TV-B before the operator confirms.
        orch.observe(EvidenceSource.OPERATOR, "operator saw it", tvB)
        assertEquals("TV-A", lastRow().target.label)
        assertEquals("aaaaaaaaaaaa", lastRow().target.tvIdPrefix)

        val press = orch.powerSend(tvA)!!
        orch.probePower(press, KeyOrchestrator.ProbeInput(apiReachable = false, phoneOnline = true, channelOpen = false))
        assertEquals("TV-A", lastRow().target.label)

        orch.tap(MvpKey.BACK, tvA)
        orch.close("manual", recordRows = true)
        val idle = rows.last { it.scenario == Scenario.GESTURE }
        assertEquals("TV-A", idle.target.label)
    }

    @Test
    fun latencySummaryNeedsSamples() {
        orch.latencySummary(tvA)
        assertEquals(Scenario.LATENCY, lastRow().scenario)
        assertEquals(RowResult.BLOCKED, lastRow().result)

        orch.tap(MvpKey.PLAY, tvA)
        orch.observe(EvidenceSource.OPERATOR, "saw it", tvA)
        repeat(4) {
            orch.tap(MvpKey.PLAY, tvA)
            orch.observe(EvidenceSource.OPERATOR, "saw it", tvA)
        }
        orch.latencySummary(tvA)
        val summary = lastRow()
        assertEquals(Scenario.LATENCY, summary.scenario)
        assertEquals(RowResult.PASS, summary.result)
        assertEquals("transport", summary.checks["group"])
        assertEquals("5", summary.checks["sampleCount"])
        assertEquals("3000", summary.checks["recommendationMs"])
    }
}
