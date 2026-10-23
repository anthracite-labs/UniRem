package com.unirem.core.api.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins the Story 1.11 command vocabulary: five commands, honest outcomes. */
class CommandTypesTest {

    @Test
    fun remoteCommandsCoverDpadPlusEnter() {
        assertEquals(
            setOf(
                RemoteCommand.Up,
                RemoteCommand.Down,
                RemoteCommand.Left,
                RemoteCommand.Right,
                RemoteCommand.Enter,
            ),
            RemoteCommand.entries.toSet(),
        )
        assertEquals(5, RemoteCommand.entries.size)
    }

    @Test
    fun outcomesCarryGenerationsWithNoConfirmedBoolean() {
        val submitted = CommandOutcome.Submitted(1L)
        assertEquals(1L, submitted.generation)
        assertEquals(CommandOutcome.Unconfirmed(2L).generation, 2L)
        assertEquals(false, CommandOutcome.Observed(3L, false).userConfirmed)
        assertEquals(true, CommandOutcome.Observed(3L, true).userConfirmed)
        val idle: CommandOutcome = CommandOutcome.Idle
        assertEquals(CommandOutcome.Idle, idle)
        // No boolean named "confirmed" anywhere on the outcome surface.
        for (method in CommandOutcome.Observed::class.java.declaredMethods) {
            assertTrue(method.name != "getConfirmed")
        }
    }

    @Test
    fun evidenceCarriesGenerationsAndNeverAVerdict() {
        assertEquals(7L, Evidence.TvEvent(7L).generation)
        assertEquals(7L, Evidence.StateProbe(7L).generation)
        assertEquals(7L, Evidence.UserConfirmed(7L).generation)
    }
}
