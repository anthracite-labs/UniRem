package com.unirem

import com.unirem.connection.RePairCommit
import com.unirem.core.api.IdentityRules
import com.unirem.core.api.NetworkType
import com.unirem.core.api.TvId
import com.unirem.tv.RoomTvStore
import com.unirem.tv.api.PairingEntry
import com.unirem.tv.api.PairingStore
import com.unirem.tv.api.TvCandidate
import com.unirem.tv.wire
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory [PairingStore] for the re-pair commit tests. */
class FakeKeystoreStore : PairingStore {
    private val rows = LinkedHashMap<String, PairingEntry>()
    override suspend fun save(tvId: TvId, token: String?, pin: String) {
        rows[tvId.hex] = PairingEntry(token, pin)
    }
    override suspend fun load(tvId: TvId): PairingEntry? = rows[tvId.hex]
    override suspend fun clear(tvId: TvId): Boolean = rows.remove(tvId.hex) != null
}

class RePairCommitTest {

    private fun tvId(deviceId: String): TvId =
        IdentityRules.tvId(IdentityRules.resolve(deviceId, null))!!

    private fun candidate(ip: String, deviceId: String): TvCandidate = TvCandidate(
        ip = ip,
        name = "Living TV",
        modelName = "M",
        model = "UE55",
        firmware = "T-KTM2DEUC-2000.2",
        tokenAuthSupport = true,
        wifiMac = "",
        deviceId = deviceId,
        deviceIdField = "device.id",
        powerState = "on",
        reportedNetworkType = "wireless",
    )

    @Test
    fun commitWritesNewAliasUpdatesRecordAndDeletesOld() = runBlocking {
        val stored = tvId("device-1")
        val store = RoomTvStore.forTests(FakeTvDao())
        val persisted = store.persistPaired(
            candidate("192.168.1.20", "device-1"),
            NetworkType.Wifi,
            "wlan",
        )!!
        val pairingStore = FakeKeystoreStore()
        pairingStore.save(stored, "old-token", "old-pin")

        val ok = RePairCommit.commit(
            storedTvId = stored,
            freshCandidate = candidate("192.168.1.21", "device-1"),
            freshTvId = stored,
            token = "new-token",
            pin = "new-pin",
            pairingStore = pairingStore,
            tvStore = store,
            networkTypeWire = NetworkType.Wifi.wire(),
            networkInterface = "wlan",
        )
        assertTrue(ok)
        assertEquals(PairingEntry("new-token", "new-pin"), pairingStore.load(stored))
        val updated = store.byId(stored)!!
        assertEquals("192.168.1.21", updated.lastIp)
        assertEquals("T-KTM2DEUC-2000.2", updated.firmware)
        assertEquals(persisted.tvId, updated.tvId)
    }

    @Test
    fun freshMismatchFailsClosedWithOldTrustIntact() = runBlocking {
        val stored = tvId("device-1")
        val store = RoomTvStore.forTests(FakeTvDao())
        store.persistPaired(candidate("192.168.1.20", "device-1"), NetworkType.Wifi, "wlan")
        val pairingStore = FakeKeystoreStore()
        pairingStore.save(stored, "old-token", "old-pin")

        val ok = RePairCommit.commit(
            storedTvId = stored,
            freshCandidate = candidate("192.168.1.21", "device-2"),
            freshTvId = tvId("device-2"),
            token = "new-token",
            pin = "new-pin",
            pairingStore = pairingStore,
            tvStore = store,
            networkTypeWire = NetworkType.Wifi.wire(),
            networkInterface = "wlan",
        )
        assertFalse(ok)
        // Old record and trust untouched, nothing overwritten.
        assertEquals(PairingEntry("old-token", "old-pin"), pairingStore.load(stored))
        assertEquals("192.168.1.20", store.byId(stored)!!.lastIp)
    }

    @Test
    fun nullFreshIdentityFailsClosed() = runBlocking {
        val stored = tvId("device-1")
        val store = RoomTvStore.forTests(FakeTvDao())
        store.persistPaired(candidate("192.168.1.20", "device-1"), NetworkType.Wifi, "wlan")
        val pairingStore = FakeKeystoreStore()
        pairingStore.save(stored, "old-token", "old-pin")

        val ok = RePairCommit.commit(
            storedTvId = stored,
            freshCandidate = candidate("192.168.1.21", "device-1"),
            freshTvId = null,
            token = "new-token",
            pin = "new-pin",
            pairingStore = pairingStore,
            tvStore = store,
            networkTypeWire = NetworkType.Wifi.wire(),
            networkInterface = "wlan",
        )
        assertFalse(ok)
        assertEquals(PairingEntry("old-token", "old-pin"), pairingStore.load(stored))
    }

    @Test
    fun recordUpdateKeepsIdentityAndAliasPointer() = runBlocking {
        val stored = tvId("device-1")
        val store = RoomTvStore.forTests(FakeTvDao())
        val before = store.persistPaired(
            candidate("192.168.1.20", "device-1"),
            NetworkType.Wifi,
            "wlan",
        )!!
        val updated = store.updateOnRePair(
            tvId = stored,
            name = "Renamed TV",
            model = "UE65",
            firmware = "T-KTM2DEUC-3000.1",
            ip = "192.168.1.22",
            networkTypeWire = NetworkType.Ethernet.wire(),
            networkInterface = "eth",
            alias = stored.hex,
        )!!
        assertEquals("Renamed TV", updated.name)
        assertEquals("UE65", updated.model)
        assertEquals("192.168.1.22", updated.lastIp)
        assertEquals("ethernet", updated.networkType)
        assertEquals(stored.hex, updated.alias)
        // Identity never moves; the paired timestamp is preserved.
        assertEquals(before.identityKind, updated.identityKind)
        assertEquals(before.identityValue, updated.identityValue)
        assertEquals(before.pairedAtEpochSeconds, updated.pairedAtEpochSeconds)
    }

    @Test
    fun recordUpdateOnMissingRowReturnsNull() = runBlocking {
        val store = RoomTvStore.forTests(FakeTvDao())
        assertNull(
            store.updateOnRePair(
                tvId = tvId("ghost"),
                name = "n",
                model = "m",
                firmware = "f",
                ip = "192.168.1.9",
                networkTypeWire = NetworkType.Wifi.wire(),
                networkInterface = "wlan",
                alias = "ghost",
            ),
        )
    }
}
