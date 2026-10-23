package com.unirem.spikea.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SsdpTest {

    @Test
    fun parsesSsdpResponseHeadersCaseInsensitively() {
        val text = "HTTP/1.1 200 OK\r\n" +
            "LOCATION: http://192.168.1.20:8001/ms/1.0/\r\n" +
            "SERVER: Linux/3.0 UPnP/1.0 Samsung\r\n" +
            "ST: urn:samsung.com:device:RemoteControlReceiver:1\r\n" +
            "USN: uuid:1234::urn:samsung.com:device:RemoteControlReceiver:1\r\n" +
            "\r\n"
        val response = Ssdp.parseResponse(text)!!
        assertEquals("http://192.168.1.20:8001/ms/1.0/", response.location)
        assertEquals("Linux/3.0 UPnP/1.0 Samsung", response.server)
        assertEquals("urn:samsung.com:device:RemoteControlReceiver:1", response.searchTarget)
        assertTrue(response.usn!!.startsWith("uuid:1234"))
    }

    @Test
    fun rejectsNonSsdpPayloads() {
        assertNull(Ssdp.parseResponse("NOT HTTP AT ALL"))
        assertNull(Ssdp.parseResponse(""))
    }

    @Test
    fun extractsHostFromLocation() {
        assertEquals("192.168.1.20", Ssdp.hostFromLocation("http://192.168.1.20:8001/ms/1.0/"))
        assertEquals("192.168.1.21", Ssdp.hostFromLocation("http://192.168.1.21/"))
        assertNull(Ssdp.hostFromLocation("https://192.168.1.20:8001/"))
        assertNull(Ssdp.hostFromLocation(null))
    }

    @Test
    fun mSearchCarriesTheRequiredHeaders() {
        val message = Ssdp.mSearch()
        assertTrue(message.startsWith("M-SEARCH * HTTP/1.1\r\n"))
        assertTrue(message.contains("HOST: 239.255.255.250:1900"))
        assertTrue(message.contains("MAN: \"ssdp:discover\""))
        assertTrue(message.contains("ST: urn:samsung.com:device:RemoteControlReceiver:1"))
        assertTrue(message.endsWith("\r\n\r\n"))
    }

    @Test
    fun manualIpMustBeAnIpv4Literal() {
        assertTrue(HostRules.isIpv4("192.168.1.20"))
        assertTrue(HostRules.isIpv4("10.0.0.1"))
        assertFalse(HostRules.isIpv4("192.168.1.999"))
        assertFalse(HostRules.isIpv4("192.168.1"))
        assertFalse(HostRules.isIpv4("tv.local"))
        assertFalse(HostRules.isIpv4("192.168.1.1 "))
        assertEquals("10.0.0.1", HostRules.normalizeIp(" 10.0.0.1 "))
    }
}
