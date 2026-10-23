package com.unirem.tv

import com.unirem.core.api.command.CommandOutcome
import com.unirem.core.api.command.RemoteCommand
import com.unirem.tv.api.ConnectionOutcome
import com.unirem.tv.api.TvGateways
import com.unirem.tv.internal.ConnectionSession
import com.unirem.tv.internal.KeyFrameWriter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Wire-through tests for the Story 1.11 command path: the gateway builds a
 * tracker whose submits each yield exactly one writer frame, the session
 * reports a dead socket honestly, and inbound frames reach the installed
 * listener without breaking latch matching.
 */
class CommandWireTest {

    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun gatewayControlSendsExactlyOneFramePerSubmit() {
        val frames = mutableListOf<String>()
        val control = TvGateways.commandControl(scope, send = { text ->
            frames += text
            true
        })
        for ((index, command) in RemoteCommand.entries.withIndex()) {
            control.submit(command)
            assertEquals(index + 1, frames.size)
            assertEquals(KeyFrameWriter.frameFor(command), frames.last())
        }
        assertEquals(
            CommandOutcome.Submitted(RemoteCommand.entries.size.toLong()),
            control.outcome.value,
        )
    }

    @Test
    fun sendFrameWithNoSocketReturnsFalse() {
        assertFalse(ConnectionSession().sendFrame("{}"))
    }

    @Test
    fun inboundTextReachesTheInstalledListener() {
        val session = ConnectionSession()
        val received = mutableListOf<String>()
        session.setMessageListener { received += it }
        val result = AtomicReference<ConnectionOutcome?>(null)
        val latch = CountDownLatch(1)
        val text = """{"event":"ms.channel.connect","data":{"token":"abc"}}"""
        session.onChannelMessage(text, result, latch)
        assertEquals(listOf(text), received)
        assertEquals(0L, latch.count)
        val outcome = result.get()
        assertTrue(outcome is ConnectionOutcome.Connected)
        assertEquals("abc", (outcome as ConnectionOutcome.Connected).token)
    }

    @Test
    fun throwingListenerDoesNotBreakLatchMatching() {
        val session = ConnectionSession()
        session.setMessageListener { throw IllegalStateException("boom") }
        val result = AtomicReference<ConnectionOutcome?>(null)
        val latch = CountDownLatch(1)
        session.onChannelMessage("""{"method":"ms.channel.unauthorized"}""", result, latch)
        assertEquals(0L, latch.count)
        assertEquals(ConnectionOutcome.PairingRequired, result.get())
    }

    @Test
    fun malformedFrameIsIgnoredAfterTheHook() {
        val session = ConnectionSession()
        val received = mutableListOf<String>()
        session.setMessageListener { received += it }
        val result = AtomicReference<ConnectionOutcome?>(null)
        val latch = CountDownLatch(1)
        session.onChannelMessage("not json", result, latch)
        assertEquals(listOf("not json"), received)
        assertEquals(1L, latch.count)
        assertNull(result.get())
    }
}
