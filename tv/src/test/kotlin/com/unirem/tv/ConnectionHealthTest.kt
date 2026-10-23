package com.unirem.tv

import com.unirem.tv.channel.ConnectionHealth
import com.unirem.tv.channel.ConnectionHealthRules
import com.unirem.tv.channel.ConnectionState
import com.unirem.tv.channel.PowerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests for the single health owner: transitions and keysEnabled. */
class ConnectionHealthTest {

    @Test
    fun keysEnabledOnlyWhenConnected() {
        assertFalse(ConnectionHealthRules.keysEnabled(ConnectionState.Connecting))
        assertTrue(ConnectionHealthRules.keysEnabled(ConnectionState.Connected))
        assertFalse(ConnectionHealthRules.keysEnabled(ConnectionState.Reconnecting))
        assertFalse(ConnectionHealthRules.keysEnabled(ConnectionState.Offline))
    }

    @Test
    fun socketDropMovesToReconnecting() {
        val closed = ConnectionHealthRules.onSocketClosed()
        assertEquals(ConnectionState.Reconnecting, closed.state)
        assertFalse(closed.keysEnabled)
    }

    @Test
    fun exhaustedScheduleMovesToOffline() {
        val offline = ConnectionHealthRules.onExhausted()
        assertEquals(ConnectionState.Offline, offline.state)
        assertFalse(offline.keysEnabled)
    }

    @Test
    fun connectedClaimsKeysEnabled() {
        val connected = ConnectionHealthRules.onConnected(PowerState.On)
        assertEquals(ConnectionState.Connected, connected.state)
        assertEquals(PowerState.On, connected.power)
        assertTrue(connected.keysEnabled)
    }

    @Test
    fun holderExposesFlowOnlyAndNoQueue() {
        val health = ConnectionHealth()
        assertEquals(0, health.pendingWrites)
        assertEquals(ConnectionState.Connecting, health.snapshot.value.state)
        health.markConnected()
        assertEquals(ConnectionState.Connected, health.snapshot.value.state)
        assertTrue(health.snapshot.value.keysEnabled)
        health.markSocketClosed()
        assertEquals(ConnectionState.Reconnecting, health.snapshot.value.state)
        health.markExhausted()
        assertEquals(ConnectionState.Offline, health.snapshot.value.state)
        health.markStopped()
        assertEquals(ConnectionState.Offline, health.snapshot.value.state)
    }
}
