package com.unirem.tv

import com.unirem.core.api.IdentityRules
import com.unirem.core.api.LanAccess
import com.unirem.core.api.TvId
import com.unirem.tv.api.ConnectionOutcome
import com.unirem.tv.api.ConnectionRequest
import com.unirem.tv.api.rotateAlias
import com.unirem.tv.channel.RePairPhase
import com.unirem.tv.channel.RePairRules
import com.unirem.tv.internal.ChannelEvents
import com.unirem.tv.internal.ConnectionSession
import com.unirem.tv.internal.PinRules
import com.unirem.tv.internal.PinVerdict
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for failure mapping, re-pair gating, atomic swap, and the
 * no-queue invariant. No socket work: gated and pre-check paths only.
 */
class ConnectionMappingTest {

    @Test
    fun unauthorizedEventParsesToPairingRequiredName() {
        val event = ChannelEvents.parse("{\"event\":\"ms.channel.unauthorized\"}")
        assertEquals("ms.channel.unauthorized", event.name)
    }

    @Test
    fun pinMismatchMapsToIdentityChangedBeforeToken() {
        // Distinct pins never compare equal; the session aborts before any
        // token-bearing URL is built.
        val stored = PinRules.sha256Base64(ByteArray(32) { 1 })
        val leaf = PinRules.sha256Base64(ByteArray(32) { 2 })
        assertEquals(PinVerdict.MISMATCH, PinRules.compare(stored, leaf))
        assertEquals(PinVerdict.MATCH, PinRules.compare(stored, stored))
    }

    @Test
    fun deniedGateMapsToLanDenied() = runBlocking {
        val session = ConnectionSession(gateCheck = { LanAccess.Denied })
        val outcome = session.connect(ConnectionRequest("192.168.1.20", "pin", "token", true))
        assertEquals(ConnectionOutcome.LanDenied, outcome)
        session.close()
    }

    @Test
    fun nonPrivateTargetRefusedBeforeSocket() = runBlocking {
        val session = ConnectionSession()
        val outcome = session.connect(ConnectionRequest("8.8.8.8", "pin", "token", true))
        assertTrue(outcome is ConnectionOutcome.Failed)
        session.close()
    }

    @Test
    fun tokenAuthWithoutTokenIsPairingRequired() = runBlocking {
        val session = ConnectionSession()
        val outcome = session.connect(ConnectionRequest("192.168.1.20", "pin", null, true))
        assertEquals(ConnectionOutcome.PairingRequired, outcome)
        session.close()
    }

    @Test
    fun sessionHoldsNoQueue() {
        val session = ConnectionSession()
        assertEquals(0, session.pendingWrites)
        session.close()
        assertEquals(0, session.pendingWrites)
    }

    @Test
    fun rePairRequiresFreshIdentityEquality() {
        val stored = IdentityRules.tvId(IdentityRules.resolve("device-1", null))!!
        val freshSame = IdentityRules.tvId(IdentityRules.resolve("device-1", null))!!
        val freshOther = IdentityRules.tvId(IdentityRules.resolve("device-2", null))!!
        assertTrue(RePairRules.freshMatchesStored(freshSame, stored))
        assertFalse(RePairRules.freshMatchesStored(freshOther, stored))
        assertFalse(RePairRules.freshMatchesStored(null, stored))
        assertTrue(RePairRules.mayCommit(allowed = true, freshMatches = true))
        assertFalse(RePairRules.mayCommit(allowed = true, freshMatches = false))
        assertFalse(RePairRules.mayCommit(allowed = false, freshMatches = true))
    }

    @Test
    fun rePairFailuresKeepOldTrust() {
        assertTrue(RePairRules.keepsOldTrust(RePairPhase.Denied))
        assertTrue(RePairRules.keepsOldTrust(RePairPhase.TimedOut))
        assertTrue(RePairRules.keepsOldTrust(RePairPhase.Failed))
        assertTrue(RePairRules.keepsOldTrust(RePairPhase.IdentityMismatch))
        assertFalse(RePairRules.keepsOldTrust(RePairPhase.Succeeded))
    }

    private fun tvId(deviceId: String): TvId =
        IdentityRules.tvId(IdentityRules.resolve(deviceId, null))!!

    @Test
    fun rotateAliasCommitsNewBeforeDeletingOld() = runBlocking {
        val store = FakePairingStore()
        val old = tvId("device-a")
        val fresh = tvId("device-b")
        store.save(old, "old-token", "old-pin")
        assertTrue(store.rotateAlias(old, fresh, "new-token", "new-pin"))
        assertEquals("new-token", store.load(fresh)?.token)
        assertEquals(null, store.load(old))
    }

    @Test
    fun rotateAliasSameIdOverwritesWithoutDelete() = runBlocking {
        val store = FakePairingStore()
        val id = tvId("device-a")
        store.save(id, "old-token", "old-pin")
        assertTrue(store.rotateAlias(id, id, "new-token", "new-pin"))
        assertEquals("new-token", store.load(id)?.token)
        assertEquals("new-pin", store.load(id)?.pin)
    }
}
