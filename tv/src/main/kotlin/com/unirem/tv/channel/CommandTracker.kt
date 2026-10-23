package com.unirem.tv.channel

import com.unirem.core.api.RedactedLog
import com.unirem.core.api.command.CommandOutcome
import com.unirem.core.api.command.Evidence
import com.unirem.core.api.command.RemoteCommand
import com.unirem.tv.api.TvControl
import com.unirem.tv.internal.ChannelEvents
import com.unirem.tv.internal.KeyFrameWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "Commands"

/**
 * Sole [CommandOutcome] writer for one socket session (Story 1.11).
 *
 * One `submit()` per tap mints a monotonic generation, emits `Submitted`
 * after the single Click write, and arms the injected timeout (default 3s;
 * [timeoutFor] carries the Spike-A per-capability override). Silence becomes
 * `Unconfirmed`; the first evidence for the current generation wins
 * `Observed` (machine or user) and holds 4s ([observedHoldMillis]) before
 * `Idle`. A newer tap supersedes: the ring belongs to the last press and
 * older generations are stale. [close] invalidates the generation, cancels
 * every timer, and clears to `Idle` with no queue, no replay, no retry.
 *
 * Exposes [StateFlow] only; the mutable flow never escapes. All transitions
 * hold one lock: `submit()` runs on the UI thread while channel callbacks
 * arrive on the socket thread.
 */
internal class CommandTracker(
    private val scope: CoroutineScope,
    private val send: (String) -> Boolean,
    private val defaultTimeoutMillis: Long = SUBMIT_TIMEOUT_MILLIS,
    private val timeoutFor: (RemoteCommand) -> Long = { defaultTimeoutMillis },
    private val observedHoldMillis: Long = OBSERVED_HOLD_MILLIS,
) : TvControl {

    private val _outcome = MutableStateFlow<CommandOutcome>(CommandOutcome.Idle)
    override val outcome: StateFlow<CommandOutcome> = _outcome.asStateFlow()

    private val lock = Any()
    private var generation: Long = 0L
    private var timeoutJob: Job? = null
    private var holdJob: Job? = null

    override fun submit(command: RemoteCommand) {
        val current: Long
        synchronized(lock) {
            generation += 1
            current = generation
            timeoutJob?.cancel()
            holdJob?.cancel()
            timeoutJob = null
            holdJob = null
            _outcome.value = CommandOutcome.Submitted(current)
        }
        // The write itself is outside the lock; the outcome is already
        // Submitted, so a slow socket cannot reorder generations.
        val wrote = try {
            send(KeyFrameWriter.frameFor(command))
        } catch (e: Exception) {
            RedactedLog.debug(TAG, "Command write failed")
            false
        }
        if (!wrote) {
            RedactedLog.debug(TAG, "Command write missed the socket")
        }
        synchronized(lock) {
            if (generation != current) return
            val wait = timeoutFor(command).coerceAtLeast(0L)
            timeoutJob = scope.launch {
                delay(wait)
                onTimeoutFired(current)
            }
        }
    }

    override fun onRemoteMessage(text: String) {
        if (text.isEmpty() || text.length > MAX_FRAME_CHARS) return
        val name = try {
            ChannelEvents.parse(text).name
        } catch (e: Exception) {
            ""
        }
        if (name.isBlank()) return
        // Only the Samsung channel speaks here; foreign frames never confirm
        // a command.
        if (!name.startsWith(CHANNEL_PREFIX)) return
        val current = synchronized(lock) { generation }
        // The adapter tags inbound evidence with the live generation; a
        // superseded generation never confirms. Any named channel event
        // while a command is outstanding counts as machine evidence.
        onEvidence(Evidence.TvEvent(current))
    }

    override fun onEvidence(evidence: Evidence) {
        val hold: Long
        synchronized(lock) {
            val showing = _outcome.value
            if (showing is CommandOutcome.Idle) return
            if (evidence.generation != generation) return
            if (showing is CommandOutcome.Observed) return
            timeoutJob?.cancel()
            timeoutJob = null
            holdJob?.cancel()
            _outcome.value = CommandOutcome.Observed(
                generation = generation,
                userConfirmed = evidence is Evidence.UserConfirmed,
            )
            hold = observedHoldMillis.coerceAtLeast(0L)
            val current = generation
            holdJob = scope.launch {
                delay(hold)
                onHoldExpired(current)
            }
        }
    }

    override fun confirmUser() {
        synchronized(lock) {
            val showing = _outcome.value
            if (showing is CommandOutcome.Unconfirmed && showing.generation == generation) {
                onEvidenceLocked(Evidence.UserConfirmed(generation))
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            // Invalidate the generation: a submit with its socket write in
            // flight past this clear must not re-arm its timeout and revive
            // a zombie Unconfirmed on a dead session.
            generation += 1
            timeoutJob?.cancel()
            holdJob?.cancel()
            timeoutJob = null
            holdJob = null
            _outcome.value = CommandOutcome.Idle
        }
        RedactedLog.debug(TAG, "Command session cleared")
    }

    private fun onTimeoutFired(fired: Long) {
        synchronized(lock) {
            val showing = _outcome.value
            if (fired == generation && showing is CommandOutcome.Submitted && showing.generation == fired) {
                _outcome.value = CommandOutcome.Unconfirmed(fired)
            }
        }
    }

    private fun onHoldExpired(held: Long) {
        synchronized(lock) {
            val showing = _outcome.value
            if (held == generation && showing is CommandOutcome.Observed && showing.generation == held) {
                _outcome.value = CommandOutcome.Idle
            }
        }
    }

    /** Caller holds [lock]. Arms the 4s hold for user evidence. */
    private fun onEvidenceLocked(evidence: Evidence) {
        val showing = _outcome.value
        if (showing is CommandOutcome.Idle) return
        if (evidence.generation != generation) return
        if (showing is CommandOutcome.Observed) return
        timeoutJob?.cancel()
        timeoutJob = null
        holdJob?.cancel()
        val hold = observedHoldMillis.coerceAtLeast(0L)
        _outcome.value = CommandOutcome.Observed(
            generation = generation,
            userConfirmed = evidence is Evidence.UserConfirmed,
        )
        val current = generation
        holdJob = scope.launch {
            delay(hold)
            onHoldExpired(current)
        }
    }

    companion object {
        const val SUBMIT_TIMEOUT_MILLIS = 3_000L
        const val OBSERVED_HOLD_MILLIS = 4_000L
        private const val MAX_FRAME_CHARS = 32_768
        private const val CHANNEL_PREFIX = "ms."
    }
}
