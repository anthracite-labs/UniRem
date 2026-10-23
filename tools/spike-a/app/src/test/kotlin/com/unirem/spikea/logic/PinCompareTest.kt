package com.unirem.spikea.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class PinCompareTest {

    private val spki = byteArrayOf(0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2A, 0x86.toByte())

    @Test
    fun identicalPinsMatch() {
        val pin = PinRules.sha256Base64(spki)
        assertEquals(PinVerdict.MATCH, PinRules.compare(pin, pin))
    }

    @Test
    fun differentPinsMismatch() {
        val stored = PinRules.sha256Base64(spki)
        val leaf = PinRules.sha256Base64(spki + byteArrayOf(0x00))
        assertEquals(PinVerdict.MISMATCH, PinRules.compare(stored, leaf))
    }

    @Test
    fun missingPinIsNotAMismatch() {
        val leaf = PinRules.sha256Base64(spki)
        assertEquals(PinVerdict.MISSING, PinRules.compare(null, leaf))
        assertEquals(PinVerdict.MISSING, PinRules.compare("", leaf))
        assertEquals(PinVerdict.MISSING, PinRules.compare("   ", leaf))
    }

    @Test
    fun malformedOrWrongLengthPinsAreInvalid() {
        val leaf = PinRules.sha256Base64(spki)
        assertEquals(PinVerdict.INVALID, PinRules.compare("not base64 !!", leaf))
        assertEquals(PinVerdict.INVALID, PinRules.compare(leaf, "AAAA"))
    }

    @Test
    fun pinIsBase64OfA32ByteDigest() {
        val pin = PinRules.sha256Base64(spki)
        assertEquals(44, pin.length)
        assertEquals(32, java.util.Base64.getDecoder().decode(pin).size)
    }
}
