package com.unirem.connection

import com.unirem.tv.channel.ConnectionState
import com.unirem.tv.channel.PowerState

/**
 * Pure status-surface rules (Story 1.7, JVM-tested).
 *
 * Vocabulary: "Paired" is durable trust, never a socket claim. The surface
 * shows the TV name, the durable "Paired" label, a health dot plus an always
 * visible text state ("Connecting…", "Connected", "Reconnecting…",
 * "Offline"). `keysEnabled` is true only when Connected. Banners push content
 * (never overlay), carry one recovery action plus an optional "Not now", and
 * are assertive only for offline and identity-changed.
 */
enum class BannerKind {
    None,
    Reconnecting,
    Offline,
    IdentityChanged,
    PairingRequired,
}

object ConnectionUiRules {

    fun textFor(state: ConnectionState): String = when (state) {
        ConnectionState.Connecting -> "Connecting…"
        ConnectionState.Connected -> "Connected"
        ConnectionState.Reconnecting -> "Reconnecting…"
        ConnectionState.Offline -> "Offline"
    }

    fun keysEnabled(state: ConnectionState): Boolean = state == ConnectionState.Connected

    fun bannerFor(
        state: ConnectionState,
        identityChanged: Boolean,
        pairingRequired: Boolean,
    ): BannerKind = when {
        identityChanged -> BannerKind.IdentityChanged
        pairingRequired -> BannerKind.PairingRequired
        state == ConnectionState.Offline -> BannerKind.Offline
        state == ConnectionState.Reconnecting -> BannerKind.Reconnecting
        else -> BannerKind.None
    }

    fun isAssertive(banner: BannerKind): Boolean = when (banner) {
        BannerKind.Offline, BannerKind.IdentityChanged -> true
        BannerKind.None, BannerKind.Reconnecting, BannerKind.PairingRequired -> false
    }

    fun hasRetry(banner: BannerKind): Boolean = when (banner) {
        BannerKind.Reconnecting, BannerKind.Offline -> true
        BannerKind.None, BannerKind.IdentityChanged, BannerKind.PairingRequired -> false
    }

    fun hasRePair(banner: BannerKind): Boolean = when (banner) {
        BannerKind.IdentityChanged, BannerKind.PairingRequired -> true
        BannerKind.None, BannerKind.Reconnecting, BannerKind.Offline -> false
    }

    /** Accessibility label names the TV plus its health; color is never the sole cue. */
    fun a11yLabel(tvName: String, state: ConnectionState, power: PowerState): String {
        val health = textFor(state)
        val powerLabel = if (power == PowerState.On) ", TV on" else ""
        return "$tvName, $health$powerLabel"
    }

    /** The paired summary stays "Paired" on every surface; never "Connected". */
    fun durableLabel(): String = "Paired"
}
