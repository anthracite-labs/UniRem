package com.unirem.tv

import com.unirem.tv.api.ConnectionBackoff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests for the injected backoff schedule: 1/2/4/8/16/30 cap + ±20% jitter. */
class ConnectionBackoffTest {

    @Test
    fun baseScheduleMatchesStory() {
        assertEquals(listOf(1L, 2L, 4L, 8L, 16L, 30L), ConnectionBackoff.BASE_DELAYS_SECONDS)
        assertEquals(1L, ConnectionBackoff.baseDelaySeconds(0))
        assertEquals(2L, ConnectionBackoff.baseDelaySeconds(1))
        assertEquals(4L, ConnectionBackoff.baseDelaySeconds(2))
        assertEquals(8L, ConnectionBackoff.baseDelaySeconds(3))
        assertEquals(16L, ConnectionBackoff.baseDelaySeconds(4))
        assertEquals(30L, ConnectionBackoff.baseDelaySeconds(5))
    }

    @Test
    fun attemptsCapAtThirtySeconds() {
        assertEquals(30L, ConnectionBackoff.baseDelaySeconds(6))
        assertEquals(30L, ConnectionBackoff.baseDelaySeconds(100))
        assertEquals(1L, ConnectionBackoff.baseDelaySeconds(-4))
    }

    @Test
    fun jitterStaysWithinTwentyPercent() {
        val base = 8_000L
        // randomUnit 0 -> -20%, 0.5 -> exact, ~1 -> +20%.
        assertEquals(6_400L, ConnectionBackoff.withJitter(base, 0.0))
        assertEquals(8_000L, ConnectionBackoff.withJitter(base, 0.5))
        assertEquals(9_599L, ConnectionBackoff.withJitter(base, 0.999_999))
        val low = ConnectionBackoff.withJitter(base, 0.0)
        val high = ConnectionBackoff.withJitter(base, 0.999_999)
        assertTrue(low >= (base * 0.8).toLong())
        assertTrue(high <= (base * 1.2).toLong())
    }

    @Test
    fun fixedScheduleIsDeterministic() {
        val schedule = ConnectionBackoff.fixed()
        assertEquals(1_000L, schedule.delayMillis(0))
        assertEquals(30_000L, schedule.delayMillis(5))
        assertEquals(30_000L, schedule.delayMillis(9))
    }

    @Test
    fun resetAfterThirtySecondsConnected() {
        assertFalse(ConnectionBackoff.shouldResetAfterConnected(29L))
        assertTrue(ConnectionBackoff.shouldResetAfterConnected(30L))
        assertTrue(ConnectionBackoff.shouldResetAfterConnected(120L))
    }
}
