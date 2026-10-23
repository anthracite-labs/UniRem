package com.unirem

import com.unirem.core.api.NetworkType
import com.unirem.tv.RoomTvStore
import com.unirem.tv.TvDao
import com.unirem.tv.TvRecord
import com.unirem.tv.api.TvCandidate
import com.unirem.tv.networkTypeOf
import com.unirem.tv.wire
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory TvDao so the persist mapping runs on the JVM. */
class FakeTvDao : TvDao {
    private val rows = LinkedHashMap<String, TvRecord>()
    private val flow = MutableStateFlow<List<TvRecord>>(emptyList())

    override fun observeAll(): Flow<List<TvRecord>> = flow

    override suspend fun all(): List<TvRecord> = rows.values.sortedByDescending { it.pairedAtEpochSeconds }

    override suspend fun byId(tvId: String): TvRecord? = rows[tvId]

    override suspend fun count(): Int = rows.size

    override suspend fun upsert(record: TvRecord) {
        rows[record.tvId] = record
        flow.value = rows.values.toList()
    }
}

class RoomTvStoreTest {

    private fun candidate(
        ip: String,
        deviceId: String = "device-1",
        mac: String = "",
    ): TvCandidate = TvCandidate(
        ip = ip,
        name = "Living TV",
        modelName = "M",
        model = "UE55",
        firmware = "T-KTM2DEUC-2000.1",
        tokenAuthSupport = true,
        wifiMac = mac,
        deviceId = deviceId,
        deviceIdField = if (deviceId.isNotBlank()) "device.id" else "",
        powerState = "on",
        reportedNetworkType = "wireless",
    )

    @Test
    fun sessionOnlyTargetsPersistNothing(): Unit = runBlocking {
        val store = RoomTvStore.forTests(FakeTvDao())
        assertNull(store.persistPaired(candidate("192.168.1.4", deviceId = ""), NetworkType.Wifi, "wlan"))
        assertEquals(0, store.count())
    }

    @Test
    fun persistMapsEveryFieldByteExact(): Unit = runBlocking {
        val dao = FakeTvDao()
        val store = RoomTvStore.forTests(dao)
        val record = store.persistPaired(
            candidate("192.168.1.2", deviceId = "  ABC-123 ", mac = "AA:BB:CC:DD:EE:01"),
            NetworkType.Wifi,
            "wlan",
        )!!
        assertEquals(record.tvId, record.alias)
        assertEquals("device-id", record.identityKind)
        assertEquals("abc-123", record.identityValue)
        assertEquals("Living TV", record.name)
        assertEquals("UE55", record.model)
        assertEquals("T-KTM2DEUC-2000.1", record.firmware)
        assertEquals("AA:BB:CC:DD:EE:01", record.wifiMac)
        assertEquals("device.id", record.macProvenance)
        assertEquals("wifi", record.networkType)
        assertEquals("wlan", record.networkInterface)
        assertTrue(record.pairedAtEpochSeconds > 0)
        assertEquals(1, dao.count())
    }

    @Test
    fun macProvenanceFallsBackWithoutDeviceField(): Unit = runBlocking {
        val store = RoomTvStore.forTests(FakeTvDao())
        val record = store.persistPaired(
            candidate("192.168.1.3", deviceId = "", mac = "AA:BB:CC:DD:EE:02"),
            NetworkType.Unknown,
            "unknown",
        )!!
        assertEquals("mac", record.identityKind)
        assertEquals("device.wifiMac", record.macProvenance)
    }

    @Test
    fun networkWireRoundTrips(): Unit = runBlocking {
        assertEquals("wifi", NetworkType.Wifi.wire())
        assertEquals("ethernet", NetworkType.Ethernet.wire())
        assertEquals("unknown", NetworkType.Unknown.wire())
        assertEquals(NetworkType.Wifi, networkTypeOf("wifi"))
        assertEquals(NetworkType.Ethernet, networkTypeOf("ethernet"))
        assertEquals(NetworkType.Unknown, networkTypeOf("unknown"))
        assertEquals(NetworkType.Unknown, networkTypeOf("bogus"))
    }
}
