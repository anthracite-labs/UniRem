package com.unirem

import com.unirem.connection.BannerKind
import com.unirem.connection.TargetSession
import com.unirem.core.api.LanAccess
import com.unirem.remote.RemoteRing
import com.unirem.remote.RemoteRules
import com.unirem.tv.TvRecord
import com.unirem.tv.api.PairingEntry
import com.unirem.tv.channel.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests for the Story 1.10 remote shell rules. */
class RemoteRulesTest {

    private fun record(hex: String): TvRecord = TvRecord(
        tvId = hex,
        identityKind = "device",
        identityValue = "device-$hex",
        name = "TV $hex",
        modelName = "M",
        model = "M",
        firmware = "T-KTM2AKUC-1000.0",
        wifiMac = "",
        macProvenance = "device.wifiMac",
        networkType = "wifi",
        networkInterface = "wlan",
        alias = hex,
        pairedAtEpochSeconds = 1L,
        lastIp = "192.168.1.20",
    )

    private fun entry(): PairingEntry = PairingEntry("token", "pin")

    @Test
    fun durableEntryNeedsSelectedTargetAndReadableRecord() {
        assertTrue(RemoteRules.canOpenDurable(TargetSession.Selected("hex"), true))
        assertFalse(RemoteRules.canOpenDurable(TargetSession.NoTarget, true))
        assertFalse(RemoteRules.canOpenDurable(TargetSession.Selected("hex"), false))
        assertFalse(RemoteRules.canOpenDurable(TargetSession.Selected(""), true))
    }

    @Test
    fun sessionOnlyEntryNeedsActiveSessionIpAndMemoryEntry() {
        assertTrue(RemoteRules.canOpenSessionOnly(true, "192.168.1.20", true))
        assertFalse(RemoteRules.canOpenSessionOnly(false, "192.168.1.20", true))
        assertFalse(RemoteRules.canOpenSessionOnly(true, null, true))
        assertFalse(RemoteRules.canOpenSessionOnly(true, "", true))
        assertFalse(RemoteRules.canOpenSessionOnly(true, "192.168.1.20", false))
    }

    @Test
    fun oneSessionPerTvId() {
        assertEquals(
            RemoteRules.sessionKey("abc"),
            RemoteRules.sessionKey("abc"),
        )
        assertNotEquals(
            RemoteRules.sessionKey("abc"),
            RemoteRules.sessionKey("def"),
        )
        assertNotEquals(
            RemoteRules.sessionKey("abc"),
            RemoteRules.sessionOnlyKey("192.168.1.20"),
        )
        assertEquals(
            RemoteRules.sessionOnlyKey("192.168.1.20"),
            RemoteRules.sessionOnlyKey("192.168.1.20"),
        )
    }

    @Test
    fun ringStaysIdleOnlyWhileASocketRemains() {
        assertEquals(RemoteRing.Idle, RemoteRules.ringFor(ConnectionState.Connecting, BannerKind.None, false))
        assertEquals(RemoteRing.Idle, RemoteRules.ringFor(ConnectionState.Connected, BannerKind.None, false))
        assertEquals(RemoteRing.Idle, RemoteRules.ringFor(ConnectionState.Reconnecting, BannerKind.Reconnecting, false))
        assertEquals(RemoteRing.Offline, RemoteRules.ringFor(ConnectionState.Offline, BannerKind.None, false))
        assertEquals(RemoteRing.Offline, RemoteRules.ringFor(ConnectionState.Offline, BannerKind.Offline, false))
        assertEquals("Idle", RemoteRules.stateLabel(RemoteRing.Idle))
        assertEquals("Offline", RemoteRules.stateLabel(RemoteRing.Offline))
    }

    @Test
    fun ringGoesOfflineForTerminalBannersAndDeniedGate() {
        assertEquals(
            RemoteRing.Offline,
            RemoteRules.ringFor(ConnectionState.Reconnecting, BannerKind.Offline, false),
        )
        assertEquals(
            RemoteRing.Offline,
            RemoteRules.ringFor(ConnectionState.Reconnecting, BannerKind.IdentityChanged, false),
        )
        assertEquals(
            RemoteRing.Offline,
            RemoteRules.ringFor(ConnectionState.Reconnecting, BannerKind.PairingRequired, false),
        )
        assertEquals(
            RemoteRing.Offline,
            RemoteRules.ringFor(ConnectionState.Connected, BannerKind.None, true),
        )
        assertEquals(
            RemoteRing.Offline,
            RemoteRules.ringFor(ConnectionState.Connecting, BannerKind.None, true),
        )
    }

    @Test
    fun resolveDurableOpenNeedsMatchAndReadableRecord() {
        assertEquals(
            "a",
            RemoteRules.resolveDurableOpen(TargetSession.Selected("a"), listOf("a", "b"), "a"),
        )
        assertNull(RemoteRules.resolveDurableOpen(TargetSession.Selected("a"), listOf("a", "b"), "b"))
        assertNull(RemoteRules.resolveDurableOpen(TargetSession.Selected("gone"), listOf("a"), "gone"))
        assertNull(RemoteRules.resolveDurableOpen(TargetSession.NoTarget, listOf("a"), "a"))
        assertNull(RemoteRules.resolveDurableOpen(TargetSession.Selected("a"), listOf("a"), ""))
    }

    @Test
    fun resolveSessionOnlyOpenNeedsActiveIpAndPin() {
        assertEquals("192.168.1.20", RemoteRules.resolveSessionOnlyOpen(true, "192.168.1.20", "pin"))
        assertNull(RemoteRules.resolveSessionOnlyOpen(false, "192.168.1.20", "pin"))
        assertNull(RemoteRules.resolveSessionOnlyOpen(true, null, "pin"))
        assertNull(RemoteRules.resolveSessionOnlyOpen(true, "", "pin"))
        assertNull(RemoteRules.resolveSessionOnlyOpen(true, "192.168.1.20", null))
        assertNull(RemoteRules.resolveSessionOnlyOpen(true, "192.168.1.20", ""))
    }

    @Test
    fun shouldStartSocketNeedsGateRecordAndEntry() {
        assertTrue(RemoteRules.shouldStartSocket(LanAccess.Granted, record("a"), entry()))
        assertFalse(RemoteRules.shouldStartSocket(LanAccess.Denied, record("a"), entry()))
        assertFalse(RemoteRules.shouldStartSocket(LanAccess.Granted, null, entry()))
        assertFalse(RemoteRules.shouldStartSocket(LanAccess.Granted, record("a"), null))
        assertFalse(RemoteRules.shouldStartSocket(LanAccess.Denied, null, null))
    }

    @Test
    fun shouldCloseOnTargetChange() {
        assertFalse(
            RemoteRules.shouldCloseOnTargetChange("a", TargetSession.Selected("a"), listOf("a"), false),
        )
        assertFalse(
            RemoteRules.shouldCloseOnTargetChange(null, TargetSession.NoTarget, listOf("a"), true),
        )
        assertFalse(
            RemoteRules.shouldCloseOnTargetChange("a", TargetSession.Selected("a"), listOf("a"), true),
        )
        assertTrue(
            RemoteRules.shouldCloseOnTargetChange("a", TargetSession.NoTarget, emptyList(), true),
        )
        assertTrue(
            RemoteRules.shouldCloseOnTargetChange("gone", TargetSession.Selected("gone"), listOf("a"), true),
        )
        assertTrue(
            RemoteRules.shouldCloseOnTargetChange("b", TargetSession.Selected("a"), listOf("a", "b"), true),
        )
        assertTrue(
            RemoteRules.shouldCloseOnTargetChange("a", TargetSession.NoTarget, listOf("a"), true),
        )
    }

    @Test
    fun resolveRetryTargetPrefersDurableThenSessionPlaceholder() {
        assertEquals("abc", RemoteRules.resolveRetryTarget("abc", null, true, false)?.hex)
        assertEquals(
            RemoteRules.sessionOnlyKey("192.168.1.20"),
            RemoteRules.resolveRetryTarget(null, "192.168.1.20", false, true)?.hex,
        )
        assertEquals(
            RemoteRules.sessionOnlyKey("192.168.1.20"),
            RemoteRules.resolveRetryTarget("abc", "192.168.1.20", false, true)?.hex,
        )
        assertNull(RemoteRules.resolveRetryTarget("abc", null, false, false))
        assertNull(RemoteRules.resolveRetryTarget(null, null, false, false))
        assertNull(RemoteRules.resolveRetryTarget(null, "192.168.1.20", false, false))
    }
}
