package com.unirem

import com.unirem.connection.TargetSession
import com.unirem.connection.TargetSessionHolder
import com.unirem.connection.TargetSwitchRules
import com.unirem.core.api.TvId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for Story 1.9 target rules: cold-start auto-select, `TvId`
 * keying, the current-row no-op, forgotten/unreadable states, session-only
 * exclusion, and one-sheet-at-a-time gating. The holder itself stays
 * persistence-free (see [TargetSwitcherGuardTest] for the structural side).
 */
class TargetSessionTest {

    @Test
    fun freshHolderStartsWithNoTarget() {
        assertEquals(TargetSession.NoTarget, TargetSessionHolder().session.value)
    }

    @Test
    fun zeroStoredTvsYieldNoTarget() {
        assertEquals(TargetSession.NoTarget, TargetSwitchRules.autoSelect(emptyList()))
    }

    @Test
    fun oneStoredTvAutoSelectsByTvId() {
        val hex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        assertEquals(
            TargetSession.Selected(hex),
            TargetSwitchRules.autoSelect(listOf(hex)),
        )
    }

    @Test
    fun manyStoredTvsNeverGuess() {
        assertEquals(
            TargetSession.NoTarget,
            TargetSwitchRules.autoSelect(listOf("aa", "bb")),
        )
    }

    @Test
    fun targetIsKeyedByTvIdNeverIndexOrIp() {
        val first = TvId("aa")
        val second = TvId("bb")
        val holder = TargetSessionHolder()
        holder.select(first)
        assertEquals(TargetSession.Selected("aa"), holder.session.value)
        holder.select(second)
        assertEquals(TargetSession.Selected("bb"), holder.session.value)
        holder.clear()
        assertEquals(TargetSession.NoTarget, holder.session.value)
    }

    @Test
    fun selectingCurrentTvIsANoOp() {
        val holder = TargetSessionHolder()
        holder.select(TvId("aa"))
        val before = holder.session.value
        assertTrue(TargetSwitchRules.isCurrent(before, "aa"))
        assertFalse(TargetSwitchRules.isCurrent(before, "bb"))
        // Re-selecting the current target changes nothing observable.
        holder.select(TvId("aa"))
        assertEquals(before, holder.session.value)
    }

    @Test
    fun forgottenTargetClearsWithNoStaleRow() {
        val holder = TargetSessionHolder()
        holder.select(TvId("aa"))
        holder.clear()
        assertEquals(TargetSession.NoTarget, holder.session.value)
        // The cleared target backs no record, yet auto-select over zero
        // records invents no replacement.
        assertTrue(TargetSwitchRules.rowUnreadable(TargetSession.Selected("aa"), hasRecord = false))
        assertEquals(TargetSession.NoTarget, TargetSwitchRules.autoSelect(emptyList()))
    }

    @Test
    fun unreadableRecordShowsNoFallbackTarget() {
        val target = TargetSession.Selected("aa")
        assertTrue(TargetSwitchRules.rowUnreadable(target, hasRecord = false))
        assertFalse(TargetSwitchRules.rowUnreadable(target, hasRecord = true))
        assertFalse(TargetSwitchRules.rowUnreadable(TargetSession.NoTarget, hasRecord = false))
    }

    @Test
    fun sessionOnlyTargetNeverSelectsAndNeverOpensTheSheet() {
        // Session-only targets carry no TvId, so auto-select over zero stored
        // records stays NoTarget even while a session-only pairing is active.
        val target = TargetSwitchRules.autoSelect(emptyList())
        assertEquals(TargetSession.NoTarget, target)
        assertFalse(
            TargetSwitchRules.sheetVisible(
                target = target,
                requested = true,
                sessionOnlyActive = true,
                otherSheetOpen = false,
            ),
        )
    }

    @Test
    fun coldStartProjectionSelectsOneLoadedRecord() {
        assertEquals(
            TargetSession.Selected("aa"),
            TargetSwitchRules.projectTarget(listOf("aa"), TargetSession.NoTarget),
        )
    }

    @Test
    fun coldStartProjectionClearsZeroRecords() {
        assertEquals(
            TargetSession.NoTarget,
            TargetSwitchRules.projectTarget(emptyList(), TargetSession.Selected("aa")),
        )
    }

    @Test
    fun coldStartProjectionKeepsAMatchingHolder() {
        assertNull(TargetSwitchRules.projectTarget(listOf("aa"), TargetSession.Selected("aa")))
        assertNull(TargetSwitchRules.projectTarget(emptyList(), TargetSession.NoTarget))
    }

    @Test
    fun resolveTargetHexMatchesSelectionAgainstStoredRecords() {
        assertEquals(
            "aa",
            TargetSwitchRules.resolveTargetHex(TargetSession.Selected("aa"), listOf("aa")),
        )
    }

    @Test
    fun resolveTargetHexYieldsNullWithoutATarget() {
        assertNull(TargetSwitchRules.resolveTargetHex(TargetSession.NoTarget, listOf("aa")))
    }

    @Test
    fun resolveTargetHexYieldsNullOnMismatch() {
        assertNull(TargetSwitchRules.resolveTargetHex(TargetSession.Selected("bb"), listOf("aa")))
        assertNull(TargetSwitchRules.resolveTargetHex(TargetSession.Selected("aa"), emptyList()))
    }

    @Test
    fun leavingASurfaceNeverClears() {
        val selected = TargetSession.Selected("aa")
        assertEquals(
            selected,
            TargetSwitchRules.reduce(selected, TargetSwitchRules.TargetEvent.SurfaceLeft),
        )
        assertEquals(
            TargetSession.NoTarget,
            TargetSwitchRules.reduce(TargetSession.NoTarget, TargetSwitchRules.TargetEvent.SurfaceLeft),
        )
    }

    @Test
    fun forgetSuccessClears() {
        assertEquals(
            TargetSession.NoTarget,
            TargetSwitchRules.reduce(TargetSession.Selected("aa"), TargetSwitchRules.TargetEvent.ForgetSucceeded),
        )
    }

    @Test
    fun sheetOpensOnlyForASelectedDurableTargetAlone() {
        val selected = TargetSession.Selected("aa")
        assertTrue(
            TargetSwitchRules.sheetVisible(
                target = selected,
                requested = true,
                sessionOnlyActive = false,
                otherSheetOpen = false,
            ),
        )
        assertFalse(
            "unrequested sheet never opens",
            TargetSwitchRules.sheetVisible(
                target = selected,
                requested = false,
                sessionOnlyActive = false,
                otherSheetOpen = false,
            ),
        )
        assertFalse(
            "no sheet without a target",
            TargetSwitchRules.sheetVisible(
                target = TargetSession.NoTarget,
                requested = true,
                sessionOnlyActive = false,
                otherSheetOpen = false,
            ),
        )
        assertFalse(
            "never on top of the trust sheet",
            TargetSwitchRules.sheetVisible(
                target = selected,
                requested = true,
                sessionOnlyActive = false,
                otherSheetOpen = true,
            ),
        )
    }
}
