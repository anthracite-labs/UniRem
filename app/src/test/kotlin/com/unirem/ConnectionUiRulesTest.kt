package com.unirem

import com.unirem.connection.BannerKind
import com.unirem.connection.ConnectionUiRules
import com.unirem.tv.channel.ConnectionState
import com.unirem.tv.channel.PowerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests for the status-surface rules: text states, banners, keys, a11y. */
class ConnectionUiRulesTest {

    @Test
    fun textStatesAlwaysVisible() {
        assertEquals("Connecting…", ConnectionUiRules.textFor(ConnectionState.Connecting))
        assertEquals("Connected", ConnectionUiRules.textFor(ConnectionState.Connected))
        assertEquals("Reconnecting…", ConnectionUiRules.textFor(ConnectionState.Reconnecting))
        assertEquals("Offline", ConnectionUiRules.textFor(ConnectionState.Offline))
    }

    @Test
    fun keysEnabledOnlyWhenConnected() {
        assertFalse(ConnectionUiRules.keysEnabled(ConnectionState.Connecting))
        assertTrue(ConnectionUiRules.keysEnabled(ConnectionState.Connected))
        assertFalse(ConnectionUiRules.keysEnabled(ConnectionState.Reconnecting))
        assertFalse(ConnectionUiRules.keysEnabled(ConnectionState.Offline))
    }

    @Test
    fun bannerPrecedenceIdentityThenPairingThenOffline() {
        assertEquals(
            BannerKind.IdentityChanged,
            ConnectionUiRules.bannerFor(ConnectionState.Reconnecting, true, true),
        )
        assertEquals(
            BannerKind.PairingRequired,
            ConnectionUiRules.bannerFor(ConnectionState.Reconnecting, false, true),
        )
        assertEquals(
            BannerKind.Offline,
            ConnectionUiRules.bannerFor(ConnectionState.Offline, false, false),
        )
        assertEquals(
            BannerKind.Reconnecting,
            ConnectionUiRules.bannerFor(ConnectionState.Reconnecting, false, false),
        )
        assertEquals(
            BannerKind.None,
            ConnectionUiRules.bannerFor(ConnectionState.Connected, false, false),
        )
        assertEquals(
            BannerKind.None,
            ConnectionUiRules.bannerFor(ConnectionState.Connecting, false, false),
        )
    }

    @Test
    fun assertiveOnlyForOfflineAndIdentityChanged() {
        assertTrue(ConnectionUiRules.isAssertive(BannerKind.Offline))
        assertTrue(ConnectionUiRules.isAssertive(BannerKind.IdentityChanged))
        assertFalse(ConnectionUiRules.isAssertive(BannerKind.Reconnecting))
        assertFalse(ConnectionUiRules.isAssertive(BannerKind.PairingRequired))
        assertFalse(ConnectionUiRules.isAssertive(BannerKind.None))
    }

    @Test
    fun retryOnlyForReconnectingAndOffline() {
        assertTrue(ConnectionUiRules.hasRetry(BannerKind.Reconnecting))
        assertTrue(ConnectionUiRules.hasRetry(BannerKind.Offline))
        assertFalse(ConnectionUiRules.hasRetry(BannerKind.IdentityChanged))
        assertFalse(ConnectionUiRules.hasRetry(BannerKind.PairingRequired))
    }

    @Test
    fun rePairOnlyForIdentityChangedAndPairingRequired() {
        assertTrue(ConnectionUiRules.hasRePair(BannerKind.IdentityChanged))
        assertTrue(ConnectionUiRules.hasRePair(BannerKind.PairingRequired))
        assertFalse(ConnectionUiRules.hasRePair(BannerKind.Reconnecting))
        assertFalse(ConnectionUiRules.hasRePair(BannerKind.Offline))
    }

    @Test
    fun a11yNamesTvAndHealth() {
        val label = ConnectionUiRules.a11yLabel("Living TV", ConnectionState.Reconnecting, PowerState.Unknown)
        assertTrue(label.contains("Living TV"))
        assertTrue(label.contains("Reconnecting…"))
    }

    @Test
    fun durableLabelStaysPaired() {
        assertEquals("Paired", ConnectionUiRules.durableLabel())
    }
}
