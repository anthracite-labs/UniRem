package com.unirem.remote

import com.unirem.connection.BannerKind
import com.unirem.connection.TargetSession
import com.unirem.connection.TargetSwitchRules
import com.unirem.core.api.LanAccess
import com.unirem.core.api.TvId
import com.unirem.core.api.command.CommandOutcome
import com.unirem.tv.TvRecord
import com.unirem.tv.api.PairingEntry
import com.unirem.tv.channel.ConnectionState

/**
 * Remote shell states. The ring reports command state only: Idle while a
 * usable socket remains, Submitted/Unconfirmed/Observed while a command is
 * outstanding, Offline whenever no usable socket remains. The topbar dot
 * reports socket health; the two are never swapped.
 */
enum class RemoteRing {
    Idle,
    Submitted,
    Unconfirmed,
    Observed,
    Offline,
}

/**
 * Pure remote rules for Story 1.10 (JVM-tested, no Android dependency).
 *
 * - The remote opens only for the active target: a selected durable TvId
 *   with a readable record, or a session-only IP with in-memory trust.
 *   No target or an unreadable record never opens.
 * - Exactly one session per TvId; session-only trust stays in memory only.
 * - The ring shows Offline whenever no usable socket remains: socket
 *   Offline, a terminal banner, or denied local access.
 */
object RemoteRules {

    /**
     * Socket plus banner plus gate to ring: Offline whenever no usable
     * socket remains (socket Offline, a terminal Offline/IdentityChanged/
     * PairingRequired banner, or denied local access). The first attempt
     * stays Idle with only the topbar dot until a banner lands.
     */
    fun ringFor(state: ConnectionState, banner: BannerKind, lanDenied: Boolean): RemoteRing =
        if (state == ConnectionState.Offline ||
            banner == BannerKind.Offline ||
            banner == BannerKind.IdentityChanged ||
            banner == BannerKind.PairingRequired ||
            lanDenied
        ) {
            RemoteRing.Offline
        } else {
            RemoteRing.Idle
        }

    /** Display label twin for the ring. Color never acts alone. */
    fun stateLabel(ring: RemoteRing): String = when (ring) {
        RemoteRing.Idle -> "Idle"
        RemoteRing.Submitted -> "Sent"
        RemoteRing.Unconfirmed -> "Waiting for TV…"
        RemoteRing.Observed -> "TV confirmed"
        RemoteRing.Offline -> "Offline"
    }

    /**
     * Command outcome to ring (Story 1.11). Offline bridging only: whenever
     * no usable socket remains the ring shows Offline no matter what the
     * tracker holds; the tracker itself is cleared to Idle on that path.
     * Otherwise the ring belongs to the last press: Submitted breathes once,
     * Unconfirmed glows amber, Observed glows mint.
     */
    fun ringFor(
        state: ConnectionState,
        banner: BannerKind,
        lanDenied: Boolean,
        outcome: CommandOutcome,
    ): RemoteRing {
        if (shouldClearCommands(state, banner, lanDenied)) {
            return RemoteRing.Offline
        }
        return when (outcome) {
            CommandOutcome.Idle -> RemoteRing.Idle
            is CommandOutcome.Submitted -> RemoteRing.Submitted
            is CommandOutcome.Unconfirmed -> RemoteRing.Unconfirmed
            is CommandOutcome.Observed -> RemoteRing.Observed
        }
    }

    /**
     * Tracker-clear truth (Story 1.11). True whenever no usable socket
     * remains: socket Offline, a terminal Offline/IdentityChanged/
     * PairingRequired banner, or denied local access. Every transition into
     * this set clears the tracker to Idle with no queue or replay; the
     * matching ring shows the 1.10 Offline pair.
     */
    fun shouldClearCommands(
        state: ConnectionState,
        banner: BannerKind,
        lanDenied: Boolean,
    ): Boolean = state == ConnectionState.Offline ||
        banner == BannerKind.Offline ||
        banner == BannerKind.IdentityChanged ||
        banner == BannerKind.PairingRequired ||
        lanDenied

    /**
     * State-line copy for one outcome. Machine evidence reads "TV confirmed",
     * the "It worked" tap reads "You confirmed". The Offline pair comes from
     * [ringFor]/[stateLabel]; this mapper never claims socket health.
     */
    fun outcomeLabel(outcome: CommandOutcome): String = when (outcome) {
        CommandOutcome.Idle -> "Idle"
        is CommandOutcome.Submitted -> "Sent"
        is CommandOutcome.Unconfirmed -> "Waiting for TV…"
        is CommandOutcome.Observed -> if (outcome.userConfirmed) "You confirmed" else "TV confirmed"
    }

    /**
     * D-pad enablement (Story 1.11). True only on a usable socket with no
     * terminal banner and no gate denial: the 1.10 banner is the only
     * recovery surface. A showing Unconfirmed or Observed never disables;
     * keys stay tappable while the command truth resolves.
     */
    fun dpadEnabled(
        state: ConnectionState,
        banner: BannerKind,
        lanDenied: Boolean,
    ): Boolean = state == ConnectionState.Connected &&
        banner == BannerKind.None &&
        !lanDenied

    /**
     * Durable entry: the target must be the selected TvId and a readable
     * record for it must exist. Anything else keeps the no-TV path.
     */
    fun canOpenDurable(target: TargetSession, hasRecord: Boolean): Boolean {
        val hex = (target as? TargetSession.Selected)?.tvIdHex ?: return false
        return hex.isNotBlank() && hasRecord
    }

    /**
     * Session-only entry: no TvId exists, so the in-memory entry for the
     * session IP must be present. Nothing is ever persisted.
     */
    fun canOpenSessionOnly(
        sessionOnlyActive: Boolean,
        sessionIp: String?,
        hasEntry: Boolean,
    ): Boolean =
        sessionOnlyActive && !sessionIp.isNullOrBlank() && hasEntry

    /** Session identity for one durable TV. Equal hex means the same session. */
    fun sessionKey(tvIdHex: String): String = tvIdHex

    /** Session identity for the session-only shell, keyed by IP only. */
    fun sessionOnlyKey(ip: String): String = "session:$ip"

    /**
     * Durable open: the requested hex must be the resolved active target
     * backed by a stored record. Mismatch, unreadable, blank, or NoTarget
     * yields null and the remote never opens.
     */
    fun resolveDurableOpen(
        target: TargetSession,
        storedHexes: List<String>,
        hex: String,
    ): String? {
        if (hex.isBlank()) return null
        if (!canOpenDurable(target, storedHexes.contains(hex))) return null
        if (TargetSwitchRules.resolveTargetHex(target, storedHexes) != hex) return null
        return hex
    }

    /**
     * Session-only open: the IP must carry a non-blank in-memory pin while
     * the session-only shell is active. Anything else yields null with
     * nothing persisted.
     */
    fun resolveSessionOnlyOpen(
        sessionOnlyActive: Boolean,
        ip: String?,
        pin: String?,
    ): String? {
        if (ip.isNullOrBlank() || pin.isNullOrBlank()) return null
        if (!canOpenSessionOnly(sessionOnlyActive, ip, true)) return null
        return ip
    }

    /**
     * Socket start: the gate must allow it and both the stored record and
     * the trust entry must be present. A missing entry with a record is
     * pairing-required with no socket work; denial never opens a socket.
     */
    fun shouldStartSocket(access: LanAccess, record: TvRecord?, entry: PairingEntry?): Boolean =
        access != LanAccess.Denied && record != null && entry != null

    /**
     * Target-change close: while the remote route is open, an unreadable
     * key, a resolved target that no longer matches the key, or a cleared
     * target with records remaining ends the session. The remote never
     * guesses a cross-target socket.
     */
    fun shouldCloseOnTargetChange(
        key: String?,
        target: TargetSession,
        storedHexes: List<String>,
        routeIsRemote: Boolean,
    ): Boolean {
        if (!routeIsRemote || key == null) return false
        if (!storedHexes.contains(key)) return true
        val resolved = TargetSwitchRules.resolveTargetHex(target, storedHexes)
        if (resolved != null && resolved != key) return true
        return resolved == null && storedHexes.isNotEmpty()
    }

    /**
     * Retry identity: the durable TvId when its record is present, else the
     * session-only placeholder. Session-only shells carry no TvId, so the
     * placeholder below is minted from the session IP; it lives only for
     * the socket attempt of this open remote and is never written to any
     * persisted store. Null when neither side can retry.
     */
    fun resolveRetryTarget(
        durableHex: String?,
        sessionIp: String?,
        hasRecord: Boolean,
        hasSessionEntry: Boolean,
    ): TvId? {
        if (durableHex != null && hasRecord) return TvId(durableHex)
        if (sessionIp != null && hasSessionEntry) return TvId(sessionOnlyKey(sessionIp))
        return null
    }
}
