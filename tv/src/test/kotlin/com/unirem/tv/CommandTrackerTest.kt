package com.unirem.tv

import com.unirem.core.api.command.CommandOutcome
import com.unirem.core.api.command.Evidence
import com.unirem.core.api.command.RemoteCommand
import com.unirem.tv.channel.CommandTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * JVM tests for the single command-truth owner (Story 1.11). Short injected
 * timeouts keep the suite fast; every wait polls the flow with a deadline
 * instead of sleeping blind.
 */
class CommandTrackerTest {

    private lateinit var scope: CoroutineScope
    private lateinit var frames: MutableList<String>
    private var sendResult: Boolean = true

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        frames = mutableListOf()
        sendResult = true
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun tracker(
        timeoutMillis: Long = 50L,
        holdMillis: Long = 60L,
        timeoutFor: (RemoteCommand) -> Long = { timeoutMillis },
    ): CommandTracker = CommandTracker(
        scope = scope,
        send = { text ->
            frames += text
            sendResult
        },
        defaultTimeoutMillis = timeoutMillis,
        timeoutFor = timeoutFor,
        observedHoldMillis = holdMillis,
    )

    private suspend fun awaitOutcome(
        tracker: CommandTracker,
        description: String,
        timeoutMillis: Long = 2_000L,
        predicate: (CommandOutcome) -> Boolean,
    ) {
        var waited = 0L
        while (waited < timeoutMillis) {
            if (predicate(tracker.outcome.value)) return
            delay(5L)
            waited += 5L
        }
        throw AssertionError("timed out waiting for $description, holding ${tracker.outcome.value}")
    }

    @Test
    fun submitEmitsSubmittedAfterOneFrame() {
        val tracker = tracker()
        tracker.submit(RemoteCommand.Up)
        assertEquals(CommandOutcome.Submitted(1L), tracker.outcome.value)
        assertEquals(1, frames.size)
        assertTrue(frames.single().contains("KEY_UP"))
    }

    @Test
    fun missedSocketStillSubmitsWithNoQueue() = runBlocking {
        sendResult = false
        val tracker = tracker()
        tracker.submit(RemoteCommand.Left)
        // The write was attempted; truth comes from evidence, never the
        // write. Nothing is queued for a retry.
        assertEquals(CommandOutcome.Submitted(1L), tracker.outcome.value)
        awaitOutcome(tracker, "unconfirmed after a missed write") { it is CommandOutcome.Unconfirmed }
        assertEquals(1, frames.size)
    }

    @Test
    fun silenceBecomesUnconfirmedAfterTimeout() = runBlocking {
        val tracker = tracker(timeoutMillis = 40L)
        tracker.submit(RemoteCommand.Down)
        awaitOutcome(tracker, "unconfirmed") { it == CommandOutcome.Unconfirmed(1L) }
    }

    @Test
    fun perCapabilityTimeoutOverridesTheDefault() = runBlocking {
        val tracker = tracker(timeoutMillis = 5_000L, timeoutFor = { 30L })
        tracker.submit(RemoteCommand.Right)
        awaitOutcome(tracker, "fast per-capability unconfirmed") { it is CommandOutcome.Unconfirmed }
    }

    @Test
    fun machineEventBeforeTimeoutObservesWithoutWaiting() = runBlocking {
        val tracker = tracker()
        tracker.submit(RemoteCommand.Up)
        tracker.onRemoteMessage("""{"event":"ms.channel.connect","data":{"token":"abc"}}""")
        assertEquals(CommandOutcome.Observed(1L, false), tracker.outcome.value)
        awaitOutcome(tracker, "idle after the observed hold") { it == CommandOutcome.Idle }
    }

    @Test
    fun foreignInboundTextIsNotEvidence() {
        val tracker = tracker()
        tracker.submit(RemoteCommand.Up)
        tracker.onRemoteMessage("not json")
        tracker.onRemoteMessage("")
        tracker.onRemoteMessage("""{"method":"other.method","params":{}}""")
        assertEquals(CommandOutcome.Submitted(1L), tracker.outcome.value)
    }

    @Test
    fun stateProbeObservesLikeAMachineEvent() = runBlocking {
        val tracker = tracker()
        tracker.submit(RemoteCommand.Enter)
        tracker.onEvidence(Evidence.StateProbe(1L))
        assertEquals(CommandOutcome.Observed(1L, false), tracker.outcome.value)
        awaitOutcome(tracker, "idle after the probe hold") { it == CommandOutcome.Idle }
    }

    @Test
    fun itWorkedConfirmsOnlyFromUnconfirmedWithNoNewFrame() = runBlocking {
        val tracker = tracker(timeoutMillis = 30L)
        tracker.submit(RemoteCommand.Up)
        // Early tap cannot confirm: the button exists only while Unconfirmed.
        tracker.confirmUser()
        assertEquals(CommandOutcome.Submitted(1L), tracker.outcome.value)
        awaitOutcome(tracker, "unconfirmed") { it == CommandOutcome.Unconfirmed(1L) }
        tracker.confirmUser()
        assertEquals(CommandOutcome.Observed(1L, true), tracker.outcome.value)
        assertEquals(1, frames.size)
        awaitOutcome(tracker, "idle after the user hold") { it == CommandOutcome.Idle }
        // A stale tap after Idle confirms nothing.
        tracker.confirmUser()
        assertEquals(CommandOutcome.Idle, tracker.outcome.value)
        assertEquals(1, frames.size)
    }

    @Test
    fun firstEvidenceWinsAndLateDuplicatesDoNotRehold() = runBlocking {
        val tracker = tracker()
        tracker.submit(RemoteCommand.Up)
        tracker.onEvidence(Evidence.TvEvent(1L))
        assertEquals(CommandOutcome.Observed(1L, false), tracker.outcome.value)
        tracker.onEvidence(Evidence.TvEvent(1L))
        tracker.confirmUser()
        assertEquals(CommandOutcome.Observed(1L, false), tracker.outcome.value)
        awaitOutcome(tracker, "single hold still expires once") { it == CommandOutcome.Idle }
    }

    @Test
    fun supersedeBelongsToTheLastPress() = runBlocking {
        val tracker = tracker()
        tracker.submit(RemoteCommand.Up)
        tracker.submit(RemoteCommand.Down)
        assertEquals(CommandOutcome.Submitted(2L), tracker.outcome.value)
        assertEquals(2, frames.size)
        // Old-generation evidence is stale.
        tracker.onEvidence(Evidence.TvEvent(1L))
        assertEquals(CommandOutcome.Submitted(2L), tracker.outcome.value)
        tracker.onEvidence(Evidence.StateProbe(2L))
        assertEquals(CommandOutcome.Observed(2L, false), tracker.outcome.value)
        awaitOutcome(tracker, "idle after the superseded hold") { it == CommandOutcome.Idle }
    }

    @Test
    fun closeClearsToIdleWithNoLateTransitions() = runBlocking {
        val tracker = tracker(timeoutMillis = 30L, holdMillis = 30L)
        tracker.submit(RemoteCommand.Up)
        tracker.close()
        assertEquals(CommandOutcome.Idle, tracker.outcome.value)
        delay(120L)
        assertEquals(CommandOutcome.Idle, tracker.outcome.value)
    }

    @Test
    fun newSessionStartsAtIdle() {
        assertEquals(CommandOutcome.Idle, tracker().outcome.value)
    }

    @Test
    fun submitWorksAgainAfterCloseForTheResumePath() {
        val tracker = tracker()
        tracker.submit(RemoteCommand.Up)
        // close() invalidates the generation, so the resumed submit mints a
        // fresh one instead of reviving the cleared session.
        tracker.close()
        tracker.submit(RemoteCommand.Down)
        assertEquals(CommandOutcome.Submitted(3L), tracker.outcome.value)
        assertEquals(2, frames.size)
    }
}
