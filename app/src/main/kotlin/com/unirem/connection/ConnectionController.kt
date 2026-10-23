package com.unirem.connection

import com.unirem.core.api.RedactedLog
import com.unirem.core.api.TvId
import com.unirem.tv.api.BackoffSchedule
import com.unirem.tv.api.ConnectionBackoff
import com.unirem.tv.api.ConnectionOutcome
import com.unirem.tv.api.ConnectionRequest
import com.unirem.tv.api.TvConnectionControl
import com.unirem.tv.channel.ConnectionHealth
import com.unirem.tv.channel.ConnectionState
import com.unirem.tv.channel.PowerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

private const val TAG = "Connection"

/** Snapshot rendered by the status surface. */
data class ConnectionSnapshot(
    val state: ConnectionState,
    val power: PowerState,
    val banner: BannerKind,
    val keysEnabled: Boolean,
    val attempt: Int,
)

private fun snapshotOf(
    state: ConnectionState,
    power: PowerState,
    banner: BannerKind,
    attempt: Int,
): ConnectionSnapshot = ConnectionSnapshot(
    state = state,
    power = power,
    banner = banner,
    keysEnabled = state == ConnectionState.Connected,
    attempt = attempt,
)

/**
 * Socket-scoped orchestrator for one open status surface (Story 1.7).
 *
 * Starts exactly one connection from the stored record, token, and pin;
 * [stop] closes the socket and stops backoff with no background reconnect.
 * No auto-connect on launch or while closed. The outbound queue stays empty.
 * Identity-changed and pairing-required are terminal banners (no auto-retry
 * with old material); socket loss runs the injected backoff schedule with
 * ±20% jitter and a 30s-connected reset, exhausting to Offline.
 *
 * Identity-change detection is defense in depth: the session aborts on pin
 * mismatch before any token bytes, and [freshIdentityOf] (a `/api/v2/` read
 * for the stored IP) aborts when the fresh stable identity differs.
 *
 * @param freshIdentityOf returns the fresh `/api/v2/` TvId for an IP, or
 * null when the TV did not answer. Never throws.
 * @param rescanSameTv returns one same-TvId IP from a single discovery scan,
 * or null. Never returns a different TV.
 */
class ConnectionController(
    private val session: TvConnectionControl,
    private val health: ConnectionHealth = ConnectionHealth(),
    private val schedule: BackoffSchedule = ConnectionBackoff.fixed(),
    private val freshIdentityOf: suspend (ip: String) -> TvId? = { null },
    private val rescanSameTv: suspend () -> String? = { null },
) {
    private val _snapshot = MutableStateFlow(
        snapshotOf(ConnectionState.Connecting, PowerState.Unknown, BannerKind.None, 0),
    )
    val snapshot: StateFlow<ConnectionSnapshot> = _snapshot.asStateFlow()

    @Volatile
    private var stopped = false

    @Volatile
    private var job: Job? = null

    /** Outbound queue depth. Always zero: commands are never queued. */
    val pendingWrites: Int = 0

    fun attachJob(running: Job) {
        job = running
    }

    /**
     * Runs one socket-scoped session. Returns when the surface leaves, the
     * terminal banner lands, or the scope cancels. Callers launch this once
     * per surface open and call [stop] on leave.
     */
    suspend fun run(
        tvId: TvId,
        ip: String,
        pin: String,
        token: String?,
        tokenAuthSupport: Boolean,
    ) {
        stopped = false
        health.markConnecting()
        publish(ConnectionState.Connecting, BannerKind.None, 0)
        var attempt = 0
        var connectedSinceMillis = 0L
        while (coroutineContext.isActive && !stopped) {
            // Fresh-identity gate: a differing stable identity aborts before
            // any token bytes, without overwriting stored trust.
            val fresh = try {
                freshIdentityOf(ip)
            } catch (e: Exception) {
                null
            }
            if (fresh != null && fresh.hex != tvId.hex) {
                publish(ConnectionState.Reconnecting, BannerKind.IdentityChanged, attempt)
                RedactedLog.debug(TAG, "Identity changed, re-pair required")
                return
            }
            val effectiveTokenAuth = tokenAuthSupport || !token.isNullOrBlank()
            val outcome = try {
                session.connect(
                    ConnectionRequest(
                        ip = ip,
                        pin = pin,
                        token = token,
                        tokenAuthSupport = effectiveTokenAuth,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ConnectionOutcome.Offline
            }
            when (outcome) {
                is ConnectionOutcome.Connected -> {
                    health.markConnected(PowerState.Unknown)
                    connectedSinceMillis = System.currentTimeMillis()
                    publish(ConnectionState.Connected, BannerKind.None, attempt)
                    RedactedLog.debug(TAG, "Status surface connected")
                    return
                }
                ConnectionOutcome.PairingRequired -> {
                    health.markSocketClosed()
                    publish(ConnectionState.Reconnecting, BannerKind.PairingRequired, attempt)
                    RedactedLog.debug(TAG, "Pairing required, old token kept")
                    return
                }
                ConnectionOutcome.IdentityChanged -> {
                    health.markSocketClosed()
                    publish(ConnectionState.Reconnecting, BannerKind.IdentityChanged, attempt)
                    RedactedLog.debug(TAG, "Identity changed, re-pair required")
                    return
                }
                ConnectionOutcome.LanDenied -> {
                    health.markExhausted()
                    publish(ConnectionState.Offline, BannerKind.Offline, attempt)
                    RedactedLog.debug(TAG, "Local network denied")
                    return
                }
                is ConnectionOutcome.Failed -> {
                    health.markSocketClosed()
                    publish(ConnectionState.Reconnecting, BannerKind.Reconnecting, attempt)
                }
                ConnectionOutcome.Offline -> {
                    health.markSocketClosed()
                    publish(ConnectionState.Reconnecting, BannerKind.Reconnecting, attempt)
                }
            }
            // Backoff before the next attempt; exhausted schedule is Offline.
            if (attempt >= ConnectionBackoff.BASE_DELAYS_SECONDS.lastIndex + 1) {
                health.markExhausted()
                publish(ConnectionState.Offline, BannerKind.Offline, attempt)
                RedactedLog.debug(TAG, "Backoff exhausted, offline")
                return
            }
            val waitMillis = try {
                schedule.delayMillis(attempt)
            } catch (e: Exception) {
                ConnectionBackoff.baseDelaySeconds(attempt) * 1_000L
            }
            // Clamp the injected schedule to the story cap so fakes cannot
            // wedge the surface; jitter bounds hold by construction.
            val bounded = waitMillis.coerceIn(0L, 36_000L)
            try {
                delay(bounded)
            } catch (e: CancellationException) {
                throw e
            }
            if (!coroutineContext.isActive || stopped) return
            // Reset the schedule after 30s continuously connected. The single
            // open-channel session returns on connect, so a reset applies to
            // the manual Retry path that follows a long-lived connection.
            if (connectedSinceMillis > 0L &&
                System.currentTimeMillis() - connectedSinceMillis >=
                ConnectionBackoff.RESET_CONNECTED_SECONDS * 1_000L
            ) {
                attempt = 0
                connectedSinceMillis = 0L
            } else {
                attempt++
            }
        }
    }

    /**
     * Manual Retry: resets the schedule, tries the stored IP first, then one
     * discovery scan for the same TvId only. Never connects a different TV;
     * the scan result is used only when its TvId matches.
     */
    suspend fun retry(
        tvId: TvId,
        storedIp: String,
        pin: String,
        token: String?,
        tokenAuthSupport: Boolean,
    ): String? {
        health.markRetry()
        publish(ConnectionState.Reconnecting, BannerKind.Reconnecting, 0)
        val direct = runAttempt(storedIp, pin, token, tokenAuthSupport)
        if (!coroutineContext.isActive) return null
        if (direct != null) return direct
        // A terminal direct attempt keeps its banner; never re-attempt with
        // the same rejected material on the rescanned address.
        when (_snapshot.value.banner) {
            BannerKind.PairingRequired, BannerKind.IdentityChanged -> return null
            else -> Unit
        }
        val rescanned = try {
            rescanSameTv()
        } catch (e: Exception) {
            null
        }
        if (rescanned != null && rescanned != storedIp) {
            return runAttempt(rescanned, pin, token, tokenAuthSupport)
        }
        return null
    }

    private suspend fun runAttempt(
        ip: String,
        pin: String,
        token: String?,
        tokenAuthSupport: Boolean,
    ): String? {
        val outcome = try {
            session.connect(ConnectionRequest(ip, pin, token, tokenAuthSupport))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ConnectionOutcome.Offline
        }
        // A later Not now or surface leave wins: publish nothing when the
        // retry scope is no longer active.
        if (!coroutineContext.isActive) return null
        return when (outcome) {
            is ConnectionOutcome.Connected -> {
                health.markConnected()
                publish(ConnectionState.Connected, BannerKind.None, 0)
                ip
            }
            ConnectionOutcome.PairingRequired -> {
                publish(ConnectionState.Reconnecting, BannerKind.PairingRequired, 0)
                null
            }
            ConnectionOutcome.IdentityChanged -> {
                publish(ConnectionState.Reconnecting, BannerKind.IdentityChanged, 0)
                null
            }
            else -> null
        }
    }

    /** "Not now": stops auto-retry and moves to Offline. Terminal until Retry. */
    fun notNow() {
        health.markStopped()
        val current = _snapshot.value
        _snapshot.value = current.copy(state = ConnectionState.Offline, banner = BannerKind.Offline)
        RedactedLog.debug(TAG, "Auto-retry stopped")
    }

    /**
     * Stored record but no Keystore entry: polite pairing-required with no
     * socket work. The old token is absent, so no attempt runs.
     */
    fun showPairingRequired() {
        health.markSocketClosed()
        val current = _snapshot.value
        _snapshot.value = current.copy(
            state = ConnectionState.Reconnecting,
            banner = BannerKind.PairingRequired,
            keysEnabled = false,
        )
        RedactedLog.debug(TAG, "Pairing required, no stored token")
    }

    /**
     * Network change: treated as a socket close. The stale socket is never
     * reused; the surface reports reconnecting and the next attempt redials.
     */
    fun onNetworkChanged() {
        try {
            session.close()
        } catch (e: Exception) {
            RedactedLog.debug(TAG, "Session close ignored")
        }
        health.markSocketClosed()
        val current = _snapshot.value
        if (current.banner == BannerKind.None) {
            _snapshot.value = current.copy(
                state = ConnectionState.Reconnecting,
                banner = BannerKind.Reconnecting,
                keysEnabled = false,
            )
        }
        RedactedLog.debug(TAG, "Network changed, socket closed")
    }

    /** Surface leave or network change: closes the socket, stops backoff. */
    fun stop() {
        stopped = true
        job?.cancel()
        job = null
        try {
            session.close()
        } catch (e: Exception) {
            RedactedLog.debug(TAG, "Session close ignored")
        }
        health.markStopped()
    }

    private fun publish(state: ConnectionState, banner: BannerKind, attempt: Int) {
        val power = health.snapshot.value.power
        _snapshot.value = snapshotOf(state, power, banner, attempt)
    }
}
