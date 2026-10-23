package com.unirem.spikea.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackoffTest {

    @Test
    fun scheduleIsVisibleAndCapped() {
        assertEquals(listOf(1L, 2L, 4L, 8L, 16L, 30L, 30L), (0..6).map { Backoff.delaySeconds(it) })
    }

    @Test
    fun attemptIsAllowedOnlyAtOrAfterTheVisibleMoment() {
        val next = Backoff.nextAttemptAt(nowMillis = 1_000L, attempt = 2)
        assertEquals(5_000L, next)
        assertFalse(Backoff.canAttempt(4_999L, next))
        assertTrue(Backoff.canAttempt(5_000L, next))
        assertTrue(Backoff.canAttempt(5_001L, next))
    }

    @Test
    fun negativeAttemptsUseTheFirstDelay() {
        assertEquals(1L, Backoff.delaySeconds(-5))
    }
}
