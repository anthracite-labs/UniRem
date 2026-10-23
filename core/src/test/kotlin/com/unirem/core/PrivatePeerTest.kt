package com.unirem.core

import com.unirem.core.api.NetPurpose
import com.unirem.core.api.PrivatePeer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the single destination predicate: RFC1918 plus link-local
 * allowed; global IPv6, ULA, loopback, multicast-as-target, DNS, and blanks
 * refused; narrow SCAN and WAKE allowlists live inside.
 */
class PrivatePeerTest {

    @Test
    fun rfc1918Allowed() {
        assertTrue(PrivatePeer.allowsIpLiteral("10.0.0.2"))
        assertTrue(PrivatePeer.allowsIpLiteral("172.16.0.2"))
        assertTrue(PrivatePeer.allowsIpLiteral("172.31.255.254"))
        assertTrue(PrivatePeer.allowsIpLiteral("192.168.0.2"))
    }

    @Test
    fun linkLocalAllowed() {
        assertTrue(PrivatePeer.allowsIpLiteral("169.254.9.9"))
        assertTrue(PrivatePeer.allowsIpLiteral("fe80::1"))
        assertTrue(PrivatePeer.allowsIpLiteral("FE80::AABB:CCDD:EEFF:0011"))
    }

    @Test
    fun publicAndUlaRefused() {
        assertFalse(PrivatePeer.allowsIpLiteral("8.8.8.8"))
        assertFalse(PrivatePeer.allowsIpLiteral("1.1.1.1"))
        assertFalse(PrivatePeer.allowsIpLiteral("172.32.0.1"))
        assertFalse(PrivatePeer.allowsIpLiteral("2001:db8::1"))
        assertFalse(PrivatePeer.allowsIpLiteral("fd00::1"))
        assertFalse(PrivatePeer.allowsIpLiteral("fc00::9"))
    }

    @Test
    fun loopbackAndMulticastTargetsRefused() {
        assertFalse(PrivatePeer.allowsIpLiteral("127.0.0.1"))
        assertFalse(PrivatePeer.allowsIpLiteral("::1"))
        assertFalse(PrivatePeer.allowsIpLiteral("239.255.255.250"))
        assertFalse(PrivatePeer.allowsIpLiteral("224.0.0.1"))
    }

    @Test
    fun dnsAndBlankRefused() {
        assertFalse(PrivatePeer.allowsIpLiteral(""))
        assertFalse(PrivatePeer.allowsIpLiteral("   "))
        assertFalse(PrivatePeer.allowsIpLiteral("tv.local"))
        assertFalse(PrivatePeer.allowsIpLiteral("samsung.com"))
        assertFalse(PrivatePeer.allowsHost("samsung.com", 8001, NetPurpose.InfoRead))
        assertFalse(PrivatePeer.allowsHost("", 8001, NetPurpose.InfoRead))
    }

    @Test
    fun ssdpMulticastAllowedForScanOnly() {
        assertTrue(PrivatePeer.allowsHost("239.255.255.250", 1900, NetPurpose.Scan))
        assertFalse(PrivatePeer.allowsHost("239.255.255.250", 1900, NetPurpose.InfoRead))
        assertFalse(PrivatePeer.allowsHost("239.255.255.250", 1900, NetPurpose.Channel))
    }

    @Test
    fun wakeBroadcastAllowlistedForWakeOnly() {
        assertTrue(PrivatePeer.allowsHost("255.255.255.255", 9, NetPurpose.Wake))
        assertTrue(PrivatePeer.allowsHost("192.168.1.255", 9, NetPurpose.Wake))
        assertFalse(PrivatePeer.allowsHost("255.255.255.255", 9, NetPurpose.Scan))
        assertFalse(PrivatePeer.allowsHost("255.255.255.255", 9, NetPurpose.Channel))
    }

    @Test
    fun wakeSuffixNeedsAnIpv4Literal() {
        assertFalse(PrivatePeer.allowsHost("8.8.8.255", 9, NetPurpose.Wake))
        assertFalse(PrivatePeer.allowsHost("foo.255", 9, NetPurpose.Wake))
        assertFalse(PrivatePeer.allowsHost("tv.255", 9, NetPurpose.Wake))
    }

    @Test
    fun privateHostsAllowedForEveryOperationalPurpose() {
        for (purpose in listOf(NetPurpose.Scan, NetPurpose.InfoRead, NetPurpose.Redirect, NetPurpose.Channel, NetPurpose.Media)) {
            assertTrue("192.168.1.20 for $purpose", PrivatePeer.allowsHost("192.168.1.20", 8001, purpose))
        }
        assertFalse(PrivatePeer.allowsHost("8.8.8.8", 8001, NetPurpose.InfoRead))
        assertFalse(PrivatePeer.allowsHost("8.8.8.8", 8002, NetPurpose.Channel))
        assertFalse(PrivatePeer.allowsHost("2001:db8::1", 8002, NetPurpose.Channel))
    }
}
