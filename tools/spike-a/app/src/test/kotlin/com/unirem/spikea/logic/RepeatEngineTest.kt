package com.unirem.spikea.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeatEngineTest {

    @Test
    fun cadenceConstantsAreFiveHundredAndEighty() {
        assertEquals(500L, RepeatRules.INITIAL_DELAY_MS)
        assertEquals(80L, RepeatRules.CADENCE_MS)
    }

    @Test
    fun repeatsDueCountsFromTheDelay() {
        assertEquals(0, RepeatRules.repeatsDue(0))
        assertEquals(0, RepeatRules.repeatsDue(499))
        assertEquals(1, RepeatRules.repeatsDue(500))
        assertEquals(1, RepeatRules.repeatsDue(579))
        assertEquals(2, RepeatRules.repeatsDue(580))
        assertEquals(1 + 10, RepeatRules.repeatsDue(500 + 10 * 80))
    }

    @Test
    fun nonDpadKeysNeverRepeat() {
        val engine = RepeatEngine()
        listOf(MvpKey.OK, MvpKey.BACK, MvpKey.HOME, MvpKey.MUTE, MvpKey.PLAY, MvpKey.POWER).forEach { key ->
            assertFalse("must refuse ${key.name}", engine.start(key, 0L))
            assertNull(engine.active())
        }
    }

    @Test
    fun singleEngineRefusesASecondHold() {
        val engine = RepeatEngine()
        assertTrue(engine.start(MvpKey.DPAD_UP, 1_000L))
        assertFalse(engine.start(MvpKey.DPAD_DOWN, 1_100L))
        assertEquals(MvpKey.DPAD_UP, engine.active())
        assertEquals(0, engine.due(1_100L))
    }

    @Test
    fun dueFollowsTheCadenceMinusEmitted() {
        val engine = RepeatEngine()
        assertTrue(engine.start(MvpKey.DPAD_LEFT, 0L))
        assertEquals(0, engine.due(499L))
        assertEquals(1, engine.due(500L))
        engine.markEmitted(1)
        assertEquals(0, engine.due(500L))
        assertEquals(1, engine.due(580L))
        engine.markEmitted(1)
        assertEquals(2, engine.due(740L))
    }

    @Test
    fun stopReturnsTheKeyForReleaseAndAllowsARestart() {
        val engine = RepeatEngine()
        assertTrue(engine.start(MvpKey.DPAD_RIGHT, 0L))
        assertEquals(MvpKey.DPAD_RIGHT, engine.stop())
        assertEquals(0, engine.due(10_000L))
        assertTrue(engine.start(MvpKey.DPAD_DOWN, 10_000L))
    }

    @Test
    fun idleEngineHasNothingDue() {
        assertEquals(0, RepeatEngine().due(99_999L))
    }
}
