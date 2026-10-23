package com.unirem

import com.unirem.core.api.IdentityRules
import com.unirem.setup.TrustLogic
import com.unirem.tv.api.PairingEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for Story 1.6 trust rules: durable-only sheet gating, the Forget
 * state machine, cancel/failure paths, and the missing-alias contract.
 */
class TrustLogicTest {

    @Test
    fun sheetVisibleOnlyForDurableLoadedRecord() {
        assertTrue(TrustLogic.sheetVisible(sessionOnlyActive = false, trustTvIdHex = "abc", hasRecord = true, entryLoaded = true))
        assertFalse(TrustLogic.sheetVisible(sessionOnlyActive = true, trustTvIdHex = "abc", hasRecord = true, entryLoaded = true))
        assertFalse(TrustLogic.sheetVisible(sessionOnlyActive = false, trustTvIdHex = null, hasRecord = true, entryLoaded = true))
        assertFalse(TrustLogic.sheetVisible(sessionOnlyActive = false, trustTvIdHex = "abc", hasRecord = false, entryLoaded = true))
        assertFalse(TrustLogic.sheetVisible(sessionOnlyActive = false, trustTvIdHex = "abc", hasRecord = true, entryLoaded = false))
        assertFalse(TrustLogic.sheetVisible(sessionOnlyActive = true, trustTvIdHex = null, hasRecord = false, entryLoaded = false))
    }

    @Test
    fun forgetSucceedsOnlyWhenRoomAndTrustBothGone() {
        assertEquals(
            TrustLogic.ForgetDecision.Succeeded,
            TrustLogic.decideForget(roomGone = true, afterClear = null),
        )
        assertEquals(
            TrustLogic.ForgetDecision.Failed,
            TrustLogic.decideForget(roomGone = false, afterClear = null),
        )
        assertEquals(
            TrustLogic.ForgetDecision.Failed,
            TrustLogic.decideForget(roomGone = true, afterClear = PairingEntry("t", "p")),
        )
        assertEquals(
            TrustLogic.ForgetDecision.Failed,
            TrustLogic.decideForget(roomGone = false, afterClear = PairingEntry("t", "p")),
        )
    }

    @Test
    fun missingAliasCountsAsAlreadyDeleted() {
        // Keystore.clear returns false when the alias was already absent;
        // the decision still succeeds when nothing remains.
        assertEquals(
            TrustLogic.ForgetDecision.Succeeded,
            TrustLogic.decideForget(roomGone = true, afterClear = null),
        )
    }

    @Test
    fun maskedMacNeverLeaksFullMac() {
        val masked = IdentityRules.maskMac("AA:BB:CC:DD:EE:01")
        assertFalse(masked.contains("aa:bb:cc:dd"))
        assertTrue(masked.endsWith("ee:01"))
        assertTrue(masked.startsWith("xx:xx:xx:xx:"))
    }
}
