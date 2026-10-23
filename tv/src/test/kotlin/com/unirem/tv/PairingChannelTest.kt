package com.unirem.tv

import com.unirem.core.api.LanAccess
import com.unirem.tv.api.PairOutcome
import com.unirem.tv.api.TvCandidate
import com.unirem.tv.internal.PinnedTlsChannel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pairing boundary tests that need no socket: the denied/non-private
 * pre-guards and the pure Connected-to-outcome classification. The live
 * handshake stays on owned hardware per the Keystore/sockets rule.
 */
class PairingChannelTest {

    private fun candidate(ip: String): TvCandidate = TvCandidate(
        ip = ip,
        name = "TV",
        modelName = "M",
        model = "M",
        firmware = "FW",
        tokenAuthSupport = true,
        wifiMac = "",
        deviceId = "device-1",
        deviceIdField = "device.id",
        powerState = "on",
        reportedNetworkType = "wireless",
    )

    @Test
    fun deniedGateBlocksBeforeAnySocketWork() = runBlocking {
        val channel = PinnedTlsChannel(gateCheck = { LanAccess.Denied })
        val outcome = channel.pair(candidate("192.168.1.20"))
        assertTrue(outcome is PairOutcome.Failed)
        assertEquals(PinnedTlsChannel.GATE_DENIED, (outcome as PairOutcome.Failed).reason)
    }

    @Test
    fun nonPrivateTargetRefusedBeforeAnySocketWork() = runBlocking {
        val channel = PinnedTlsChannel()
        val outcome = channel.pair(candidate("8.8.8.8"))
        assertTrue(outcome is PairOutcome.Failed)
    }

    @Test
    fun classifyMapsConnectedTokenRule() {
        val channel = PinnedTlsChannel()
        assertEquals(
            PairOutcome.Paired("tok", "pin"),
            channel.classifyConnected("tok", "pin", true),
        )
        assertEquals(
            PairOutcome.TokenMissing,
            channel.classifyConnected(null, "pin", true),
        )
        assertEquals(
            PairOutcome.TokenMissing,
            channel.classifyConnected("", "pin", true),
        )
        assertEquals(
            PairOutcome.Paired(null, "pin"),
            channel.classifyConnected(null, "pin", false),
        )
    }
}
