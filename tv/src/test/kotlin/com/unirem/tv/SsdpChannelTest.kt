package com.unirem.tv

import com.unirem.tv.internal.ChannelEvents
import com.unirem.tv.internal.ChannelUrl
import com.unirem.tv.internal.PinRules
import com.unirem.tv.internal.PinVerdict
import com.unirem.tv.internal.SsdpPacket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SsdpPacketTest {

    @Test
    fun searchTargetIsSamsungReceiver() {
        assertEquals("urn:samsung.com:device:RemoteControlReceiver:1", SsdpPacket.SEARCH_TARGET)
        assertEquals("239.255.255.250", SsdpPacket.MULTICAST_HOST)
        assertEquals(1900, SsdpPacket.MULTICAST_PORT)
    }

    @Test
    fun mSearchCarriesTargetAndHost() {
        val payload = SsdpPacket.mSearch()
        assertTrue(payload.startsWith("M-SEARCH * HTTP/1.1"))
        assertTrue(payload.contains("ST: urn:samsung.com:device:RemoteControlReceiver:1"))
        assertTrue(payload.contains("HOST: 239.255.255.250:1900"))
    }

    @Test
    fun parseResponseReadsLocationCaseInsensitively() {
        val text = "HTTP/1.1 200 OK\r\nLOCATION: http://192.168.1.20:8001/api/v2/\r\nST: urn:samsung.com:device:RemoteControlReceiver:1\r\n\r\n"
        val response = SsdpPacket.parseResponse(text)!!
        assertEquals("http://192.168.1.20:8001/api/v2/", response.location)
    }

    @Test
    fun parseResponseRejectsNonHttp() {
        assertNull(SsdpPacket.parseResponse("NOTIFY * HTTP/1.1\r\nlocation: http://x/\r\n"))
        assertNull(SsdpPacket.parseResponse("garbage"))
    }

    @Test
    fun hostFromLocationExtractsHost() {
        assertEquals("192.168.1.20", SsdpPacket.hostFromLocation("http://192.168.1.20:8001/api/v2/"))
        assertEquals("192.168.1.21", SsdpPacket.hostFromLocation("http://192.168.1.21/"))
        assertEquals("192.168.1.22", SsdpPacket.hostFromLocation("http://192.168.1.22"))
        assertEquals("192.168.1.23", SsdpPacket.hostFromLocation("https://192.168.1.23:8001/api/v2/"))
        assertEquals("fe80::1", SsdpPacket.hostFromLocation("http://[fe80::1]:8001/api/v2/"))
        assertEquals("fe80::2", SsdpPacket.hostFromLocation("http://[fe80::2]/"))
        assertNull(SsdpPacket.hostFromLocation(null))
        assertNull(SsdpPacket.hostFromLocation("not a url"))
        assertNull(SsdpPacket.hostFromLocation("http://"))
    }
}

class ChannelSecurityTest {

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
    }

    @Test
    fun malformedPinsAreInvalid() {
        val leaf = PinRules.sha256Base64(spki)
        assertEquals(PinVerdict.INVALID, PinRules.compare("not base64 !!", leaf))
        assertEquals(PinVerdict.INVALID, PinRules.compare(leaf, "AAAA"))
    }

    @Test
    fun urlIsAlwaysWssAndNeverFallsBack() {
        val withoutToken = ChannelUrl.build("192.168.1.20")
        val withToken = ChannelUrl.build("192.168.1.20", "token-value")
        assertTrue(withoutToken.startsWith("wss://"))
        assertTrue(withToken.startsWith("wss://"))
        assertFalse(withoutToken.startsWith("ws://"))
        assertFalse(withToken.contains("ws://192"))
        assertTrue(ChannelUrl.build("192.168.1.20").contains(":8002/api/v2/channels/samsung.remote.control"))
    }

    @Test
    fun tokenAttachedOnlyWhenProvided() {
        assertFalse(ChannelUrl.build("192.168.1.20").contains("token="))
        assertTrue(ChannelUrl.build("192.168.1.20", "token-value").contains("&token=token-value"))
    }

    @Test
    fun ipv6HostsAreBracketed() {
        val url = ChannelUrl.build("fe80::1")
        assertTrue(url.startsWith("wss://[fe80::1]:"))
        assertTrue(ChannelUrl.build("fe80::1", "t").contains("[fe80::1]"))
    }

    @Test
    fun channelEventsParseBothCasings() {
        val connect = ChannelEvents.parse("{\"event\":\"ms.channel.connect\",\"data\":{\"token\":\"abc\"}}")
        assertEquals("ms.channel.connect", connect.name)
        assertEquals("abc", connect.token)
        val method = ChannelEvents.parse("{\"method\":\"ms.channel.connect\",\"data\":{\"token\":\"abc\"}}")
        assertEquals("ms.channel.connect", method.name)
        val denied = ChannelEvents.parse("{\"event\":\"ms.channel.unauthorized\"}")
        assertEquals("ms.channel.unauthorized", denied.name)
        assertNull(denied.token)
    }
}
