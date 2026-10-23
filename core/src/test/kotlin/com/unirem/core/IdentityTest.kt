package com.unirem.core

import com.unirem.core.api.CollisionRules
import com.unirem.core.api.IdentityEntry
import com.unirem.core.api.IdentityRules
import com.unirem.core.api.IdentitySource
import com.unirem.core.api.PrivatePeer
import com.unirem.core.api.StableIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for Story 1.5 identity: one normalization rule, precedence,
 * TvId stability, duplicate resolution, MAC masking, IP validation, and the
 * session-only gate.
 */
class IdentityTest {

    @Test
    fun normalizationIsOneRuleForBothInputs() {
        assertEquals(
            IdentityRules.resolve("  ABC-123 ", "AA:BB:CC:DD:EE:01"),
            IdentityRules.resolve("abc-123", "aa:bb:cc:dd:ee:01"),
        )
        assertEquals("abc-123", IdentityRules.normalize("  ABC-123 "))
        assertEquals("aa:bb:cc:dd:ee:01", IdentityRules.normalize(" AA:BB:CC:DD:EE:01 "))
    }

    @Test
    fun deviceIdWinsOverMac() {
        val identity = IdentityRules.resolve("device-1", "AA:BB:CC:DD:EE:01")
        assertTrue(identity is StableIdentity.DeviceId)
        assertEquals("device-1", identity.normalized)
        assertEquals(IdentitySource.DEVICE_ID, identity.source)
    }

    @Test
    fun blankDeviceIdFallsBackToMac() {
        val identity = IdentityRules.resolve("   ", "AA:BB:CC:DD:EE:01")
        assertTrue(identity is StableIdentity.Mac)
        assertEquals("aa:bb:cc:dd:ee:01", identity.normalized)
        assertEquals(IdentitySource.MAC, identity.source)
    }

    @Test
    fun noIdentityIsSessionOnlyWithNoTvId() {
        val identity = IdentityRules.resolve(null, "  ")
        assertEquals(StableIdentity.SessionOnly, identity)
        assertEquals(IdentitySource.NONE, identity.source)
        assertNull(IdentityRules.tvId(identity))
        assertTrue(IdentityRules.isSessionOnly(identity))
    }

    @Test
    fun stableIdentitySurvivesCaseAndWhitespace() {
        val first = IdentityRules.resolve(" ABC ", null)
        val second = IdentityRules.resolve("abc", null)
        assertEquals(first, second)
        assertEquals(IdentityRules.tvId(first), IdentityRules.tvId(second))
    }

    @Test
    fun tvIdIsSha256HexWithShortPrefix() {
        val tvId = IdentityRules.tvId(IdentityRules.resolve("device-1", null))!!
        assertEquals(64, tvId.hex.length)
        assertEquals(12, tvId.prefix.length)
        assertEquals(tvId.hex.take(12), tvId.prefix)
        assertTrue(tvId.hex.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun tvIdIgnoresIpAndSeparatesKinds() {
        val asDeviceId = IdentityRules.tvId(IdentityRules.resolve("aa", null))
        val asMac = IdentityRules.tvId(IdentityRules.resolve(null, "aa"))
        assertNotEquals(asDeviceId, asMac)
    }

    @Test
    fun duplicateIdentitiesResolveToOneTvId() {
        val identity = IdentityRules.resolve("device-1", null)
        val first = IdentityEntry("192.168.1.10", identity, IdentityRules.tvId(identity))
        val second = IdentityEntry("192.168.1.11", identity, IdentityRules.tvId(identity))
        assertEquals(1, CollisionRules.collisions(listOf(first, second)).size)
    }

    @Test
    fun sameModelDifferentMacsDoNotCollide() {
        val first = IdentityRules.resolve(null, "AA:BB:CC:DD:EE:01")
        val second = IdentityRules.resolve(null, "AA:BB:CC:DD:EE:02")
        val entries = listOf(
            IdentityEntry("tv-a", first, IdentityRules.tvId(first)),
            IdentityEntry("tv-b", second, IdentityRules.tvId(second)),
        )
        assertTrue(CollisionRules.collisions(entries).isEmpty())
    }

    @Test
    fun maskedMacShowsAtMostLastTwoOctets() {
        assertEquals("", IdentityRules.maskMac(null))
        assertEquals("", IdentityRules.maskMac("  "))
        val masked = IdentityRules.maskMac("AA:BB:CC:DD:EE:01")
        assertTrue("masked must hide the head: $masked", masked.startsWith("xx:xx:xx:xx:"))
        assertTrue("masked must keep the tail: $masked", masked.endsWith("ee:01"))
        assertFalse("masked must never equal the full MAC", masked == "aa:bb:cc:dd:ee:01")
        val dashMasked = IdentityRules.maskMac("AA-BB-CC-DD-EE-02")
        assertTrue(dashMasked.endsWith("ee:02"))
    }

    @Test
    fun ipValidationAcceptsPrivateLiteralsOnly() {
        assertTrue(IdentityRules.validateIpLiteral("192.168.1.20"))
        assertTrue(IdentityRules.validateIpLiteral("10.0.0.5"))
        assertTrue(IdentityRules.validateIpLiteral("172.16.4.9"))
        assertTrue(IdentityRules.validateIpLiteral("169.254.9.9"))
        assertTrue(IdentityRules.validateIpLiteral("fe80::1"))
        assertFalse(IdentityRules.validateIpLiteral("8.8.8.8"))
        assertFalse(IdentityRules.validateIpLiteral("2001:db8::1"))
        assertFalse(IdentityRules.validateIpLiteral("tv.local"))
        assertFalse(IdentityRules.validateIpLiteral("samsung.com"))
        assertFalse(IdentityRules.validateIpLiteral(""))
        assertFalse(IdentityRules.validateIpLiteral("   "))
        assertFalse(IdentityRules.validateIpLiteral(null))
        assertTrue(PrivatePeer.isPrivateLiteral("192.168.1.20"))
    }

    @Test
    fun sessionOnlyGatingHolds() {
        assertTrue(IdentityRules.isSessionOnly(StableIdentity.SessionOnly))
        assertFalse(IdentityRules.isSessionOnly(IdentityRules.resolve("d1", null)))
        assertFalse(IdentityRules.isSessionOnly(IdentityRules.resolve(null, "AA:BB:CC:DD:EE:01")))
    }
}
