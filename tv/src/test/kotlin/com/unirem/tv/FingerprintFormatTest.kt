package com.unirem.tv

import com.unirem.tv.api.FingerprintFormat
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the pure SPKI fingerprint formatter: stored-pin SHA-256
 * bytes render as grouped uppercase hex; missing or unreadable pins yield
 * null so the sheet shows "Fingerprint unavailable".
 */
class FingerprintFormatTest {

    @Test
    fun formatsThirtyTwoBytesAsGroupedUppercaseHex() {
        val bytes = ByteArray(32) { it.toByte() }
        val pin = Base64.getEncoder().encodeToString(bytes)
        val formatted = FingerprintFormat.format(pin)!!
        val groups = formatted.split(" ")
        assertEquals(32, groups.size)
        assertTrue(groups.all { it.length == 2 && it.all { c -> c in '0'..'9' || c in 'A'..'F' } })
        assertEquals("00 01 02 03", groups.take(4).joinToString(" "))
        assertEquals("1E 1F", groups.takeLast(2).joinToString(" "))
        // Fully visible: 64 hex digits plus 31 separators.
        assertEquals(95, formatted.length)
    }

    @Test
    fun decodesKnownPinToKnownHex() {
        val bytes = ByteArray(32) { 0xAB.toByte() }
        val pin = Base64.getEncoder().encodeToString(bytes)
        assertEquals(
            List(32) { "AB" }.joinToString(" "),
            FingerprintFormat.format(pin),
        )
    }

    @Test
    fun missingPinYieldsNull() {
        assertNull(FingerprintFormat.format(null))
        assertNull(FingerprintFormat.format(""))
        assertNull(FingerprintFormat.format("   "))
    }

    @Test
    fun surroundingWhitespaceIsIgnored() {
        val bytes = ByteArray(32) { 0xAB.toByte() }
        val pin = "  " + Base64.getEncoder().encodeToString(bytes) + "\n"
        assertEquals(
            List(32) { "AB" }.joinToString(" "),
            FingerprintFormat.format(pin),
        )
    }

    @Test
    fun corruptPinYieldsNull() {
        assertNull(FingerprintFormat.format("not-base64!!"))
        val short = Base64.getEncoder().encodeToString(ByteArray(16))
        assertNull(FingerprintFormat.format(short))
        val long = Base64.getEncoder().encodeToString(ByteArray(33))
        assertNull(FingerprintFormat.format(long))
    }
}
