package com.unirem.spikea.logic

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultsTest {

    private fun row(
        scenario: Scenario = Scenario.PAIR,
        result: RowResult = RowResult.PASS,
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
    fun lineIsSingleLineJson() {
        val line = ResultsFormat.line(row())
        assertFalse(line.contains("\n"))
        assertTrue(line.startsWith("{"))
        assertTrue(line.endsWith("}"))
        val json = JSONObject(line)
        assertEquals("pair", json.getString("scenario"))
        assertEquals("pass", json.getString("result"))
        assertEquals("0123456789ab", json.getString("tvIdPrefix"))
        assertFalse(json.getBoolean("sessionOnly"))
    }

    @Test
    fun tokenPinAndSpkiNeverAppear() {
        val secret = "A".repeat(64)
        val hexSecret = "0123456789abcdef".repeat(4)
        val line = ResultsFormat.line(
            row(notes = "token=$secret pin=$secret spki=$secret hex=$hexSecret"),
        )
        assertFalse(line.contains(secret))
        assertFalse(line.contains(hexSecret))
        assertTrue(line.contains(Redaction.REDACTED))
    }

    @Test
    fun fullMacIsNeverWritten() {
        val line = ResultsFormat.line(row(notes = "wifiMac=AA:BB:CC:DD:EE:FF"))
        assertFalse(line.contains("AA:BB:CC:DD:EE:FF"))
        assertTrue(line.contains(Redaction.MAC_PLACEHOLDER))
        assertEquals(Redaction.MAC_PLACEHOLDER, Redaction.mac("AA:BB:CC:DD:EE:FF"))
    }

    @Test
    fun forbiddenCheckKeysAreDropped() {
        val line = ResultsFormat.line(
            row(checks = mapOf("token" to "secret", "wifiMac" to "AA:BB:CC:DD:EE:FF", "multicastLockHeld" to "true")),
        )
        val checks = JSONObject(line).getJSONObject("checks")
        assertFalse(checks.has("token"))
        assertFalse(checks.has("wifiMac"))
        assertEquals("true", checks.getString("multicastLockHeld"))
    }

    @Test
    fun evidenceCheckKeysSurviveTheFilter() {
        val keys = listOf(
            "authSupport",
            "authDelivered",
            "storedAfterDeny",
            "sentAfterVerify",
            "resultsFullHwAddress",
            "resultsHighEntropyValue",
            "multicastLockHeld",
            "keystoreBacked",
            "vaultCiphertextOnly",
        )
        val checks = JSONObject(ResultsFormat.line(row(checks = keys.associateWith { "true" }))).getJSONObject("checks")
        keys.forEach { assertTrue("dropped evidence key: $it", checks.has(it)) }
    }

    @Test
    fun jsonEscapingKeepsOneRowPerLine() {
        val line = ResultsFormat.line(row(notes = "quote=\" backslash=\\ newline=\n tab=\t"))
        val json = JSONObject(line)
        assertEquals("quote=\" backslash=\\ newline=\n tab=\t", json.getString("notes"))
    }

    @Test
    fun tvSuppliedFieldsAreScrubbed() {
        val line = ResultsFormat.line(row(firmware = "T-KTM2DEUC_1520 AA:BB:CC:DD:EE:FF"))
        assertFalse(line.contains("AA:BB:CC:DD:EE:FF"))
        assertTrue(line.contains(Redaction.MAC_PLACEHOLDER))
    }

    @Test
    fun containsMacMirrorsScrub() {
        val withMac = "wifiMac=AA:BB:CC:DD:EE:FF"
        assertTrue(Redaction.containsMac(withMac))
        assertFalse(Redaction.containsMac(Redaction.scrub(withMac)))
        assertFalse(Redaction.containsMac("no hardware address here"))
    }

    @Test
    fun containsLongSecretMirrorsScrub() {
        val hexSecret = "0123456789abcdef".repeat(4)
        assertTrue(Redaction.containsLongSecret("pin=$hexSecret"))
        assertFalse(Redaction.containsLongSecret(Redaction.scrub("pin=$hexSecret")))
        assertFalse(Redaction.containsLongSecret("short value"))
    }

    @Test
    fun rawJsonlLineWithMacIsDetectedByAudit() {
        val rawLine = "{\"scenario\":\"pair\",\"notes\":\"mac AA:BB:CC:DD:EE:FF\"}"
        assertTrue(Redaction.containsMac(rawLine))
        val cleanLine = ResultsFormat.line(row(notes = "mac AA:BB:CC:DD:EE:FF"))
        assertFalse(Redaction.containsMac(cleanLine))
        assertFalse(Redaction.containsLongSecret(cleanLine))
    }

    @Test
    fun discoveryOnlyPassRowsDoNotOpenTheGate() {
        val rows = listOf(
            row(tvIdPrefix = "aaaaaaaaaaaa"),
            row(tvIdPrefix = "bbbbbbbbbbbb"),
            row(tvIdPrefix = "cccccccccccc", model = "", firmware = "", networkType = ""),
        )
        assertEquals(GateVerdict.BLOCKED, GateRules.verdict(rows))
    }

    @Test
    fun blankFirmwareAloneKeepsGateBlocked() {
        val rows = listOf(
            row(tvIdPrefix = "aaaaaaaaaaaa"),
            row(tvIdPrefix = "bbbbbbbbbbbb"),
            row(tvIdPrefix = "cccccccccccc", firmware = ""),
        )
        assertEquals(GateVerdict.BLOCKED, GateRules.verdict(rows))
    }

    @Test
    fun tvIdPrefixIsTruncatedAtTheSchemaBoundary() {
        val line = ResultsFormat.line(row(tvIdPrefix = "0123456789abcdef0123456789abcdef"))
        assertEquals(12, JSONObject(line).getString("tvIdPrefix").length)
    }

    @Test
    fun gateIsBlockedWithoutThreeDistinctTvs() {
        val rows = listOf(
            row(tvIdPrefix = "aaaaaaaaaaaa"),
            row(tvIdPrefix = "bbbbbbbbbbbb"),
        )
        assertEquals(GateVerdict.BLOCKED, GateRules.verdict(rows))
    }

    @Test
    fun gatePassesOnlyWithThreeDistinctTvsAndNoBlockedOrFail() {
        val rows = listOf(
            row(tvIdPrefix = "aaaaaaaaaaaa"),
            row(tvIdPrefix = "bbbbbbbbbbbb"),
            row(tvIdPrefix = "cccccccccccc"),
        )
        assertEquals(GateVerdict.PASS, GateRules.verdict(rows))
    }

    @Test
    fun blockedIsNotPassAndFailOutranksBlocked() {
        val blocked = listOf(
            row(tvIdPrefix = "aaaaaaaaaaaa"),
            row(tvIdPrefix = "bbbbbbbbbbbb"),
            row(tvIdPrefix = "cccccccccccc", result = RowResult.BLOCKED),
        )
        assertEquals(GateVerdict.BLOCKED, GateRules.verdict(blocked))

        val failed = blocked + row(tvIdPrefix = "dddddddddddd", result = RowResult.FAIL)
        assertEquals(GateVerdict.FAIL, GateRules.verdict(failed))
    }

    @Test
    fun emptyResultsAreBlocked() {
        assertEquals(GateVerdict.BLOCKED, GateRules.verdict(emptyList()))
    }
}
