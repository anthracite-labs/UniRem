package com.unirem.spikea

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * KeySession held-gating that is provable without a network: a fresh session
 * sends nothing, blank-pin and bad-IP opens fail closed without holding, and
 * closing a never-opened session is safe. Live open/close races stay
 * hardware-driven; the guards they rely on (`held`) are covered here.
 */
class KeySessionTest {

    private fun session(): KeySession = KeySession(PinnedTls())

    @Test
    fun sendBeforeOpenIsFalse() {
        assertFalse(session().send("""{"method":"ms.remote.control"}"""))
    }

    @Test
    fun blankPinFailsClosedWithoutHolding() {
        val session = session()
        val outcome = session.open("192.168.1.10", "", null, 1L) {}
        assertTrue(outcome is KeySession.OpenOutcome.Failed)
        assertFalse(session.isOpen())
        assertFalse(session.send("{}"))
    }

    @Test
    fun badIpFailsWithoutHolding() {
        val session = session()
        val outcome = session.open("not-an-ip", "pin", null, 1L) {}
        assertTrue(outcome is KeySession.OpenOutcome.Failed)
        assertFalse(session.isOpen())
    }

    @Test
    fun closeOnNeverOpenedIsSafe() {
        val session = session()
        session.close("test")
        assertFalse(session.isOpen())
    }
}
