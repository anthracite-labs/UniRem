package com.unirem

import com.unirem.connection.BannerKind
import com.unirem.core.api.command.CommandOutcome
import com.unirem.remote.RemoteRing
import com.unirem.remote.RemoteRules
import com.unirem.tv.channel.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests for the Story 1.11 command-truth mappers. */
class CommandTruthRulesTest {

    private val idle = CommandOutcome.Idle
    private val submitted = CommandOutcome.Submitted(1L)
    private val unconfirmed = CommandOutcome.Unconfirmed(1L)
    private val machine = CommandOutcome.Observed(1L, false)
    private val user = CommandOutcome.Observed(1L, true)

    @Test
    fun ringFollowsTheLastPressOnAUsableSocket() {
        assertEquals(RemoteRing.Idle, RemoteRules.ringFor(ConnectionState.Connected, BannerKind.None, false, idle))
        assertEquals(RemoteRing.Submitted, RemoteRules.ringFor(ConnectionState.Connected, BannerKind.None, false, submitted))
        assertEquals(RemoteRing.Unconfirmed, RemoteRules.ringFor(ConnectionState.Connected, BannerKind.None, false, unconfirmed))
        assertEquals(RemoteRing.Observed, RemoteRules.ringFor(ConnectionState.Connected, BannerKind.None, false, machine))
        assertEquals(RemoteRing.Observed, RemoteRules.ringFor(ConnectionState.Connected, BannerKind.None, false, user))
        // First attempts stay Idle with only the topbar dot; connecting and
        // reconnecting sockets report command Idle, never Offline.
        assertEquals(RemoteRing.Idle, RemoteRules.ringFor(ConnectionState.Connecting, BannerKind.None, false, idle))
        assertEquals(RemoteRing.Idle, RemoteRules.ringFor(ConnectionState.Reconnecting, BannerKind.Reconnecting, false, idle))
    }

    @Test
    fun ringBridgesToOfflineOnly() {
        // Offline bridging: no usable socket means Offline no matter what the
        // tracker holds; the tracker itself is cleared to Idle on that path.
        for (outcome in listOf(idle, submitted, unconfirmed, machine, user)) {
            assertEquals(RemoteRing.Offline, RemoteRules.ringFor(ConnectionState.Offline, BannerKind.None, false, outcome))
            assertEquals(RemoteRing.Offline, RemoteRules.ringFor(ConnectionState.Reconnecting, BannerKind.Offline, false, outcome))
            assertEquals(RemoteRing.Offline, RemoteRules.ringFor(ConnectionState.Reconnecting, BannerKind.IdentityChanged, false, outcome))
            assertEquals(RemoteRing.Offline, RemoteRules.ringFor(ConnectionState.Reconnecting, BannerKind.PairingRequired, false, outcome))
            assertEquals(RemoteRing.Offline, RemoteRules.ringFor(ConnectionState.Connected, BannerKind.None, true, outcome))
        }
    }

    @Test
    fun outcomeLabelsTellTheTruth() {
        assertEquals("Idle", RemoteRules.outcomeLabel(idle))
        assertEquals("Sent", RemoteRules.outcomeLabel(submitted))
        assertEquals("Waiting for TV…", RemoteRules.outcomeLabel(unconfirmed))
        assertEquals("TV confirmed", RemoteRules.outcomeLabel(machine))
        assertEquals("You confirmed", RemoteRules.outcomeLabel(user))
    }

    @Test
    fun ringLabelsTwinEveryRing() {
        assertEquals("Idle", RemoteRules.stateLabel(RemoteRing.Idle))
        assertEquals("Sent", RemoteRules.stateLabel(RemoteRing.Submitted))
        assertEquals("Waiting for TV…", RemoteRules.stateLabel(RemoteRing.Unconfirmed))
        assertEquals("TV confirmed", RemoteRules.stateLabel(RemoteRing.Observed))
        assertEquals("Offline", RemoteRules.stateLabel(RemoteRing.Offline))
    }

    @Test
    fun shouldClearCommandsPinsEveryTerminalTransition() {
        // Every terminal banner clears, whatever the socket shows.
        for (banner in listOf(
            BannerKind.Offline,
            BannerKind.IdentityChanged,
            BannerKind.PairingRequired,
        )) {
            assertTrue(RemoteRules.shouldClearCommands(ConnectionState.Connected, banner, false))
            assertTrue(RemoteRules.shouldClearCommands(ConnectionState.Reconnecting, banner, false))
        }
        // An Offline socket clears even with no banner.
        assertTrue(RemoteRules.shouldClearCommands(ConnectionState.Offline, BannerKind.None, false))
        assertTrue(RemoteRules.shouldClearCommands(ConnectionState.Offline, BannerKind.Reconnecting, false))
        // Denied local access clears even on a live-looking socket.
        assertTrue(RemoteRules.shouldClearCommands(ConnectionState.Connected, BannerKind.None, true))
        assertTrue(RemoteRules.shouldClearCommands(ConnectionState.Connecting, BannerKind.None, true))
        // Live paths never clear.
        assertFalse(RemoteRules.shouldClearCommands(ConnectionState.Connecting, BannerKind.None, false))
        assertFalse(RemoteRules.shouldClearCommands(ConnectionState.Connected, BannerKind.None, false))
        assertFalse(RemoteRules.shouldClearCommands(ConnectionState.Reconnecting, BannerKind.None, false))
        assertFalse(RemoteRules.shouldClearCommands(ConnectionState.Reconnecting, BannerKind.Reconnecting, false))
        assertFalse(RemoteRules.shouldClearCommands(ConnectionState.Connected, BannerKind.Reconnecting, false))
    }

    @Test
    fun dpadEnabledNeedsAUsableSocketWithNoOtherRecovery() {
        assertTrue(RemoteRules.dpadEnabled(ConnectionState.Connected, BannerKind.None, false))
        // Every gated state disables: the 1.10 banner is the only recovery.
        assertFalse(RemoteRules.dpadEnabled(ConnectionState.Connecting, BannerKind.None, false))
        assertFalse(RemoteRules.dpadEnabled(ConnectionState.Reconnecting, BannerKind.None, false))
        assertFalse(RemoteRules.dpadEnabled(ConnectionState.Offline, BannerKind.None, false))
        assertFalse(RemoteRules.dpadEnabled(ConnectionState.Reconnecting, BannerKind.Reconnecting, false))
        assertFalse(RemoteRules.dpadEnabled(ConnectionState.Reconnecting, BannerKind.Offline, false))
        assertFalse(RemoteRules.dpadEnabled(ConnectionState.Reconnecting, BannerKind.IdentityChanged, false))
        assertFalse(RemoteRules.dpadEnabled(ConnectionState.Reconnecting, BannerKind.PairingRequired, false))
        assertFalse(RemoteRules.dpadEnabled(ConnectionState.Connected, BannerKind.None, true))
        assertFalse(RemoteRules.dpadEnabled(ConnectionState.Connected, BannerKind.Reconnecting, true))
    }
}
