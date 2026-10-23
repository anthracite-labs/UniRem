package com.unirem

import com.unirem.core.api.IdentityRules
import com.unirem.setup.SetupLogic
import com.unirem.tv.api.DiscoveryResult
import com.unirem.tv.api.PairOutcome
import com.unirem.tv.api.TvCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupLogicTest {

    private fun candidate(
        ip: String,
        deviceId: String = "",
        mac: String = "",
        tokenAuth: Boolean = true,
    ): TvCandidate = TvCandidate(
        ip = ip,
        name = "TV",
        modelName = "M",
        model = "M",
        firmware = "T-TEST_0001",
        tokenAuthSupport = tokenAuth,
        wifiMac = mac,
        deviceId = deviceId,
        deviceIdField = if (deviceId.isNotBlank()) "device.id" else "",
        powerState = "on",
        reportedNetworkType = "wireless",
    )

    @Test
    fun scanMapsFoundEmptyDeniedAndError() {
        val found = SetupLogic.mapScan(DiscoveryResult.Found(listOf(candidate("192.168.1.2", deviceId = "d1"))))
        assertTrue(found is SetupLogic.ScanUi.Found)
        assertTrue(SetupLogic.mapScan(DiscoveryResult.Found(emptyList())) is SetupLogic.ScanUi.Empty)
        assertTrue(SetupLogic.mapScan(DiscoveryResult.NoDiscovery) is SetupLogic.ScanUi.Empty)
        assertTrue(SetupLogic.mapScan(DiscoveryResult.LanDenied("blocked")) is SetupLogic.ScanUi.LanDenied)
        assertTrue(SetupLogic.mapScan(DiscoveryResult.Failed("boom")) is SetupLogic.ScanUi.Error)
    }

    @Test
    fun mergeAsFoundNeverDuplicatesTvOrIp() {
        val first = candidate("192.168.1.2", deviceId = "same-id")
        val sameTvNewIp = candidate("192.168.1.3", deviceId = "same-id")
        val sameIp = candidate("192.168.1.2", deviceId = "other-id")
        val merged = SetupLogic.mergeAsFound(listOf(first), sameTvNewIp)
        assertEquals(1, merged.size)
        assertEquals(listOf(first), SetupLogic.mergeAsFound(listOf(first), sameIp))
        val fresh = candidate("192.168.1.9", deviceId = "fresh-id")
        assertEquals(2, SetupLogic.mergeAsFound(listOf(first), fresh).size)
    }

    @Test
    fun dedupeCollapsesRepeatSightingsButKeepsSessionOnly() {
        val first = candidate("192.168.1.2", deviceId = "same-id")
        val repeat = candidate("192.168.1.3", deviceId = "same-id")
        assertEquals(1, SetupLogic.dedupeByTvId(listOf(first, repeat)).size)
        val sessionA = candidate("192.168.1.4")
        val sessionB = candidate("192.168.1.5")
        assertEquals(2, SetupLogic.dedupeByTvId(listOf(sessionA, sessionB)).size)
    }

    @Test
    fun manualIpAcceptsPrivateLiteralsOnly() {
        assertNull(SetupLogic.manualIpError("192.168.1.20"))
        assertNull(SetupLogic.manualIpError("  10.0.0.5  "))
        assertNull(SetupLogic.manualIpError("fe80::1"))
        assertEquals("Enter a private IP address.", SetupLogic.manualIpError("8.8.8.8"))
        assertEquals("Enter a private IP address.", SetupLogic.manualIpError("tv.local"))
        assertEquals("Enter a private IP address.", SetupLogic.manualIpError(""))
        assertEquals("Enter a private IP address.", SetupLogic.manualIpError("   "))
    }

    @Test
    fun sessionOnlyGatingHolds() {
        assertTrue(SetupLogic.isSessionOnly(candidate("192.168.1.4")))
        assertFalse(SetupLogic.isSessionOnly(candidate("192.168.1.2", deviceId = "d1")))
        assertFalse(SetupLogic.isSessionOnly(candidate("192.168.1.3", mac = "AA:BB:CC:DD:EE:01")))
    }

    @Test
    fun persistsOnlyWhenTokenRuleSatisfied() {
        assertTrue(SetupLogic.persists(PairOutcome.Paired("t", "p"), true))
        assertTrue(SetupLogic.persists(PairOutcome.Paired(null, "p"), false))
        assertFalse(SetupLogic.persists(PairOutcome.Paired(null, "p"), true))
        assertFalse(SetupLogic.persists(PairOutcome.TokenMissing, false))
        assertFalse(SetupLogic.persists(PairOutcome.Denied, true))
        assertFalse(SetupLogic.persists(PairOutcome.TimedOut, true))
        assertFalse(SetupLogic.persists(PairOutcome.Failed("x"), true))
        assertTrue(SetupLogic.denies(PairOutcome.Denied))
        assertTrue(SetupLogic.timedOut(PairOutcome.TimedOut))
    }

    @Test
    fun maskedMacNeverLeaksFullMac() {
        val masked = IdentityRules.maskMac("AA:BB:CC:DD:EE:01")
        assertFalse(masked.contains("aa:bb:cc:dd"))
        assertTrue(masked.endsWith("ee:01"))
    }

    @Test
    fun cardKeyPrefersTvIdOverIp() {
        val identified = candidate("192.168.1.2", deviceId = "d1")
        assertEquals(identified.tvId!!.hex, SetupLogic.cardKey(identified))
        val session = candidate("192.168.1.4")
        assertEquals("ip:192.168.1.4", SetupLogic.cardKey(session))
    }
}
