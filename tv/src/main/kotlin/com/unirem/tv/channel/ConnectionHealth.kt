package com.unirem.tv.channel

import com.unirem.core.api.RedactedLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "Connection"

/**
 * Single owner of live connection state (Story 1.7).
 *
 * All 8001/8002 observations land here. [ConnectionState] reports the
 * socket only, never a command outcome; [PowerState] reports power only.
 * `keysEnabled` is derived: true only when [ConnectionState.Connected].
 * No queue, replay, or retry of commands lives here; [pendingWrites] stays
 * zero and the outbound queue is empty on close by construction.
 */
enum class ConnectionState {
    Connecting,
    Connected,
    Reconnecting,
    Offline,
}

enum class PowerState {
    On,
    Unknown,
}

data class HealthSnapshot(
    val state: ConnectionState,
    val power: PowerState,
    val keysEnabled: Boolean,
)

/** Pure transition rules; JVM-tested without a socket. */
object ConnectionHealthRules {

    fun initial(): HealthSnapshot =
        HealthSnapshot(ConnectionState.Connecting, PowerState.Unknown, keysEnabled = false)

    fun keysEnabled(state: ConnectionState): Boolean = state == ConnectionState.Connected

    fun onConnected(power: PowerState = PowerState.Unknown): HealthSnapshot =
        HealthSnapshot(ConnectionState.Connected, power, keysEnabled = true)

    fun onSocketClosed(): HealthSnapshot =
        HealthSnapshot(ConnectionState.Reconnecting, PowerState.Unknown, keysEnabled = false)

    fun onExhausted(): HealthSnapshot =
        HealthSnapshot(ConnectionState.Offline, PowerState.Unknown, keysEnabled = false)

    fun onRetry(): HealthSnapshot =
        HealthSnapshot(ConnectionState.Reconnecting, PowerState.Unknown, keysEnabled = false)

    fun onStopped(): HealthSnapshot =
        HealthSnapshot(ConnectionState.Offline, PowerState.Unknown, keysEnabled = false)

    fun onConnecting(): HealthSnapshot =
        HealthSnapshot(ConnectionState.Connecting, PowerState.Unknown, keysEnabled = false)
}

/**
 * Process-surface-scoped holder. One instance per open status surface; the
 * caller closes it on leave, which stops backoff and leaves no socket.
 * Exposes [StateFlow] only; the mutable flow never escapes.
 */
class ConnectionHealth {

    private val _snapshot = MutableStateFlow(ConnectionHealthRules.initial())
    val snapshot: StateFlow<HealthSnapshot> = _snapshot.asStateFlow()

    /** Outbound queue depth. Always zero: this story never queues commands. */
    val pendingWrites: Int = 0

    fun markConnecting() {
        _snapshot.value = ConnectionHealthRules.onConnecting()
    }

    fun markConnected(power: PowerState = PowerState.Unknown) {
        _snapshot.value = ConnectionHealthRules.onConnected(power)
        RedactedLog.debug(TAG, "Channel connected")
    }

    fun markSocketClosed() {
        _snapshot.value = ConnectionHealthRules.onSocketClosed()
        RedactedLog.debug(TAG, "Socket closed, reconnecting")
    }

    fun markExhausted() {
        _snapshot.value = ConnectionHealthRules.onExhausted()
        RedactedLog.debug(TAG, "Backoff exhausted, offline")
    }

    fun markRetry() {
        _snapshot.value = ConnectionHealthRules.onRetry()
        RedactedLog.debug(TAG, "Manual retry requested")
    }

    fun markStopped() {
        _snapshot.value = ConnectionHealthRules.onStopped()
        RedactedLog.debug(TAG, "Connection stopped")
    }
}
