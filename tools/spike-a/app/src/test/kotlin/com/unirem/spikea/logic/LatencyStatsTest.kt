package com.unirem.spikea.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LatencyStatsTest {

    @Test
    fun medianOfOddEvenSingleAndEmpty() {
        assertEquals(200L, LatencyStats.medianMs(listOf(300L, 100L, 200L)))
        assertEquals(250L, LatencyStats.medianMs(listOf(100L, 400L, 200L, 300L)))
        assertEquals(120L, LatencyStats.medianMs(listOf(120L)))
        assertNull(LatencyStats.medianMs(emptyList()))
    }

    @Test
    fun tailIsTheNinetyFifthPercentile() {
        val samples = (1L..20L).map { it * 100L }
        assertEquals(1_900L, LatencyStats.tailP95Ms(samples))
        assertEquals(500L, LatencyStats.tailP95Ms(listOf(100L, 200L, 300L, 400L, 500L)))
        assertEquals(700L, LatencyStats.tailP95Ms(listOf(700L)))
        assertNull(LatencyStats.tailP95Ms(emptyList()))
    }

    @Test
    fun tooFewSamplesMeansBlockedNotARecommendation() {
        assertNull(LatencyStats.recommendTimeoutMs(listOf(100L, 200L, 300L, 400L)))
        assertNull(LatencyStats.recommendTimeoutMs(emptyList()))
    }

    @Test
    fun fastRunsKeepTheThreeSecondDefault() {
        val samples = listOf(120L, 180L, 220L, 150L, 300L, 260L)
        assertEquals(3_000L, LatencyStats.recommendTimeoutMs(samples))
    }

    @Test
    fun slowTailRaisesTheRecommendationWithMargin() {
        val samples = listOf(2_900L, 3_100L, 3_000L, 3_200L, 3_050L)
        assertEquals(3_200L + 500L, LatencyStats.recommendTimeoutMs(samples))
    }

    @Test
    fun powerProbeNeedsSilenceOnEitherChannel() {
        assertEquals(RowResult.BLOCKED, PowerProbe.interpret(apiReachable = true, channelOpen = true))
        assertEquals(RowResult.PASS, PowerProbe.interpret(apiReachable = false, channelOpen = true))
        assertEquals(RowResult.PASS, PowerProbe.interpret(apiReachable = true, channelOpen = false))
        assertEquals(RowResult.PASS, PowerProbe.interpret(apiReachable = false, channelOpen = false))
    }
}
