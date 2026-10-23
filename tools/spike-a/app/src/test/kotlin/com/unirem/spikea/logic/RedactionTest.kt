package com.unirem.spikea.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedactionTest {

    @Test
    fun tokenQueryParamIsStripped() {
        val url = "wss://192.168.1.20:8002/api/v2/channels/samsung.remote.control?name=abc&token=secret-value"
        val stripped = Redaction.withoutTokenParam(url)
        assertFalse(stripped.contains("secret-value"))
        assertTrue(stripped.contains("&token=<redacted>"))
        assertTrue(stripped.startsWith("wss://192.168.1.20"))
    }

    @Test
    fun firstTokenParamIsStripped() {
        val stripped = Redaction.withoutTokenParam("wss://h:8002/c?token=abc123&x=1")
        assertFalse(stripped.contains("abc123"))
        assertTrue(stripped.contains("?token=<redacted>&x=1"))
    }

    @Test
    fun textWithoutTokenParamIsUntouched() {
        assertEquals("plain failure", Redaction.withoutTokenParam("plain failure"))
        assertEquals("", Redaction.withoutTokenParam(null))
        assertEquals("", Redaction.withoutTokenParam(""))
    }

    @Test
    fun strippedOutputSurvivesScrub() {
        val secret = "A".repeat(48)
        val cleaned = Redaction.scrub(Redaction.withoutTokenParam("unexpected response wss://h:8002/c?token=$secret"))
        assertFalse(cleaned.contains(secret))
        assertTrue(cleaned.contains("token=<redacted>"))
    }
}
