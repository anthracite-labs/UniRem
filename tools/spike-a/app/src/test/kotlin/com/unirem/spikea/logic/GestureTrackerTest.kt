package com.unirem.spikea.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureTrackerTest {

    @Test
    fun eachPressGetsANewGeneration() {
        val tracker = GestureTracker()
        val first = tracker.start(MvpKey.OK)
        val second = tracker.start(MvpKey.OK)
        assertTrue(second.generation > first.generation)
        assertTrue(second.id != first.id)
    }

    @Test
    fun writeIsUnconfirmedNeverObserved() {
        val tracker = GestureTracker()
        val gesture = tracker.start(MvpKey.DPAD_UP)
        assertEquals(GestureState.SUBMITTED, tracker.stateOf(gesture.id))
        assertTrue(tracker.markWritten(gesture.id))
        assertEquals(GestureState.UNCONFIRMED, tracker.stateOf(gesture.id))
    }

    @Test
    fun matchingEvidenceObserves() {
        val tracker = GestureTracker()
        val gesture = tracker.start(MvpKey.HOME)
        tracker.markWritten(gesture.id)
        val observed = tracker.observe(gesture.id, gesture.generation, EvidenceSource.OPERATOR)
        assertNotNull(observed)
        assertEquals(GestureState.OBSERVED, tracker.stateOf(gesture.id))
    }

    @Test
    fun staleGenerationNeverConfirms() {
        val tracker = GestureTracker()
        val older = tracker.start(MvpKey.VOL_UP)
        tracker.markWritten(older.id)
        val newer = tracker.start(MvpKey.VOL_UP)
        tracker.markWritten(newer.id)
        // Late evidence naming the older generation must not touch the newer press.
        assertNull(tracker.observe(newer.id, older.generation, EvidenceSource.MACHINE))
        assertEquals(GestureState.UNCONFIRMED, tracker.stateOf(newer.id))
        // Nor may it rewrite the older press through the newer id.
        assertNull(tracker.observe(older.id, newer.generation, EvidenceSource.MACHINE))
        assertEquals(GestureState.UNCONFIRMED, tracker.stateOf(older.id))
    }

    @Test
    fun observedIsTerminalAndUnknownIdsAreIgnored() {
        val tracker = GestureTracker()
        val gesture = tracker.start(MvpKey.BACK)
        tracker.markWritten(gesture.id)
        assertNotNull(tracker.observe(gesture.id, gesture.generation, EvidenceSource.PROBE))
        assertNull(tracker.observe(gesture.id, gesture.generation, EvidenceSource.OPERATOR))
        assertEquals(GestureState.OBSERVED, tracker.stateOf(gesture.id))
        assertNull(tracker.observe(999L, 1L, EvidenceSource.OPERATOR))
        assertNull(tracker.stateOf(999L))
    }

    @Test
    fun closeParksOpenGesturesAtIdleAndDropsResolvedRecords() {
        val tracker = GestureTracker()
        val open = tracker.start(MvpKey.PLAY)
        tracker.markWritten(open.id)
        val done = tracker.start(MvpKey.PAUSE)
        tracker.markWritten(done.id)
        tracker.observe(done.id, done.generation, EvidenceSource.OPERATOR)
        val idled = tracker.closeAll()
        assertEquals(listOf(open.id), idled.map { it.id })
        // Resolved records (idled and observed) are dropped from the map so
        // they cannot accumulate and late evidence has nothing to rewrite.
        assertNull(tracker.stateOf(open.id))
        assertNull(tracker.stateOf(done.id))
    }

    @Test
    fun nothingResolvesAfterClose() {
        val tracker = GestureTracker()
        val gesture = tracker.start(MvpKey.MUTE)
        tracker.markWritten(gesture.id)
        tracker.closeAll()
        assertNull(tracker.observe(gesture.id, gesture.generation, EvidenceSource.OPERATOR))
        assertNull(tracker.stateOf(gesture.id))
    }
}
