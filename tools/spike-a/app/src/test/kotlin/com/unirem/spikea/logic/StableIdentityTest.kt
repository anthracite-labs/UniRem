package com.unirem.spikea.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StableIdentityTest {

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
    fun noIdentityIsSessionOnly() {
        val identity = IdentityRules.resolve(null, "  ")
        assertEquals(StableIdentity.SessionOnly, identity)
        assertEquals(IdentitySource.NONE, identity.source)
        assertNull(IdentityRules.tvId(identity))
    }

    @Test
    fun tvIdIsSha256HexAndExposesOnlyAPrefix() {
        val identity = IdentityRules.resolve("device-1", null)
        val tvId = IdentityRules.tvId(identity)!!
        assertEquals(64, tvId.hex.length)
        assertEquals(12, tvId.prefix.length)
        assertEquals(tvId.hex.take(12), tvId.prefix)
        assertTrue(tvId.hex.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun tvIdIgnoresCaseAndWhitespaceButNotIdentityKind() {
        val a = IdentityRules.tvId(IdentityRules.resolve(" ABC ", null))
        val b = IdentityRules.tvId(IdentityRules.resolve("abc", null))
        assertEquals(a, b)

        val asDeviceId = IdentityRules.tvId(IdentityRules.resolve("aa", null))
        val asMac = IdentityRules.tvId(IdentityRules.resolve(null, "aa"))
        assertNotEquals(asDeviceId, asMac)
    }

    @Test
    fun syntheticIdenticalDeviceIdCollides() {
        val first = IdentityEntry(
            label = "synthetic-a 10.0.0.1",
            identity = IdentityRules.resolve("SYNTHETIC-ID-0001", "AA:BB:CC:DD:EE:01"),
            tvId = IdentityRules.tvId(IdentityRules.resolve("SYNTHETIC-ID-0001", "AA:BB:CC:DD:EE:01")),
        )
        val second = IdentityEntry(
            label = "synthetic-b 10.0.0.2",
            identity = IdentityRules.resolve("SYNTHETIC-ID-0001", "AA:BB:CC:DD:EE:02"),
            tvId = IdentityRules.tvId(IdentityRules.resolve("SYNTHETIC-ID-0001", "AA:BB:CC:DD:EE:02")),
        )
        val collisions = CollisionRules.collisions(listOf(first, second))
        assertEquals(1, collisions.size)
        assertEquals(listOf("synthetic-a 10.0.0.1", "synthetic-b 10.0.0.2"), collisions.single().labels)
    }

    @Test
    fun differentMacsDoNotCollideWhenNoDeviceIdExists() {
        val first = IdentityEntry(
            label = "tv-a",
            identity = IdentityRules.resolve(null, "AA:BB:CC:DD:EE:01"),
            tvId = IdentityRules.tvId(IdentityRules.resolve(null, "AA:BB:CC:DD:EE:01")),
        )
        val second = IdentityEntry(
            label = "tv-b",
            identity = IdentityRules.resolve(null, "AA:BB:CC:DD:EE:02"),
            tvId = IdentityRules.tvId(IdentityRules.resolve(null, "AA:BB:CC:DD:EE:02")),
        )
        assertTrue(CollisionRules.collisions(listOf(first, second)).isEmpty())
    }

    @Test
    fun sameIdentityOnTwoAddressesIsStillOneTvId() {
        val identity = IdentityRules.resolve("device-1", null)
        val tvId = IdentityRules.tvId(identity)
        val first = IdentityEntry("192.168.1.10", identity, tvId)
        val second = IdentityEntry("192.168.1.11", identity, tvId)
        assertEquals(1, CollisionRules.collisions(listOf(first, second)).size)
    }
}
