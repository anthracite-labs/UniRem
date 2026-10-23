package com.unirem.tv

import com.unirem.core.api.IdentityRules
import com.unirem.core.api.TvId
import com.unirem.tv.api.PairingEntry
import com.unirem.tv.api.PairingStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory fake of the [PairingStore] port; the Keystore impl lives in `:app`. */
class FakePairingStore : PairingStore {
    private val rows = LinkedHashMap<String, PairingEntry>()

    override suspend fun save(tvId: TvId, token: String?, pin: String) {
        rows[tvId.hex] = PairingEntry(token, pin)
    }

    override suspend fun load(tvId: TvId): PairingEntry? = rows[tvId.hex]

    override suspend fun clear(tvId: TvId): Boolean = rows.remove(tvId.hex) != null

    fun size(): Int = rows.size
}

/**
 * JVM contract for the [PairingStore] port: per-identity aliases, token
 * optional (pin-only), clear removes the alias, distinct TVs stay distinct.
 */
class PairingStoreContractTest {

    private fun tvId(deviceId: String): TvId =
        IdentityRules.tvId(IdentityRules.resolve(deviceId, null))!!

    @Test
    fun saveAndLoadRoundTrips(): Unit = runBlocking {
        val store: PairingStore = FakePairingStore()
        val id = tvId("device-1")
        store.save(id, "token-1", "pin-1")
        assertEquals(PairingEntry("token-1", "pin-1"), store.load(id))
    }

    @Test
    fun pinOnlyStoredWhenTokenAuthUnsupported(): Unit = runBlocking {
        val store: PairingStore = FakePairingStore()
        val id = tvId("device-2")
        store.save(id, null, "pin-2")
        assertEquals(PairingEntry(null, "pin-2"), store.load(id))
    }

    @Test
    fun aliasesArePerIdentity(): Unit = runBlocking {
        val store = FakePairingStore()
        val first = tvId("device-a")
        val second = tvId("device-b")
        store.save(first, "token-a", "pin-a")
        assertNull(store.load(second))
        store.save(second, "token-b", "pin-b")
        assertEquals(2, store.size())
        assertEquals(PairingEntry("token-a", "pin-a"), store.load(first))
    }

    @Test
    fun clearRemovesOnlyThatAlias(): Unit = runBlocking {
        val store = FakePairingStore()
        val first = tvId("device-a")
        val second = tvId("device-b")
        store.save(first, "token-a", "pin-a")
        store.save(second, "token-b", "pin-b")
        assertTrue(store.clear(first))
        assertNull(store.load(first))
        assertEquals(PairingEntry("token-b", "pin-b"), store.load(second))
        assertFalse(store.clear(first))
    }

    @Test
    fun sameModelDistinctDevicesYieldDistinctIds(): Unit = runBlocking {
        val store = FakePairingStore()
        val first = tvId("SYNTHETIC-A")
        val second = tvId("SYNTHETIC-B")
        assertTrue(first != second)
        store.save(first, "t1", "p1")
        store.save(second, "t2", "p2")
        assertEquals(2, store.size())
    }
}
