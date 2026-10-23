package com.unirem

import androidx.sqlite.db.SupportSQLiteDatabase
import com.unirem.core.api.NetworkType
import com.unirem.tv.MIGRATION_1_2
import com.unirem.tv.RoomTvStore
import com.unirem.tv.TvDao
import com.unirem.tv.TvRecord
import com.unirem.tv.api.TvCandidate
import com.unirem.tv.networkTypeOf
import com.unirem.tv.wire
import java.lang.reflect.Proxy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    override suspend fun deleteById(tvId: String): Int {
        val removed = rows.remove(tvId) != null
        flow.value = rows.values.toList()
        return if (removed) 1 else 0
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

    @Test
    fun persistStoresLastReachedIp(): Unit = runBlocking {
        val store = RoomTvStore.forTests(FakeTvDao())
        val record = store.persistPaired(
            candidate("192.168.1.9", deviceId = "device-9"),
            NetworkType.Wifi,
            "wlan",
        )!!
        assertEquals("192.168.1.9", record.lastIp)
    }

    @Test
    fun forgetDeletesOnlyThatTvId(): Unit = runBlocking {
        val dao = FakeTvDao()
        val store = RoomTvStore.forTests(dao)
        val first = store.persistPaired(
            candidate("192.168.1.2", deviceId = "device-a"),
            NetworkType.Wifi,
            "wlan",
        )!!
        val second = store.persistPaired(
            candidate("192.168.1.3", deviceId = "device-b"),
            NetworkType.Wifi,
            "wlan",
        )!!
        val firstId = com.unirem.core.api.TvId(first.tvId)
        assertTrue(store.forget(firstId))
        assertNull(store.byId(firstId))
        assertEquals(1, store.count())
        assertEquals(second.tvId, store.all().single().tvId)
    }

    @Test
    fun forgetMissingRowCountsAsGone(): Unit = runBlocking {
        val store = RoomTvStore.forTests(FakeTvDao())
        val missing = com.unirem.core.api.TvId(
            com.unirem.core.api.IdentityRules.tvId(
                com.unirem.core.api.IdentityRules.resolve("ghost", null),
            )!!.hex,
        )
        assertTrue(store.forget(missing))
    }

    @Test
    fun forgetDaoThrowCountsAsFailure(): Unit = runBlocking {
        val throwing = object : TvDao by FakeTvDao() {
            override suspend fun deleteById(tvId: String): Int = throw RuntimeException("boom")
        }
        val store = RoomTvStore.forTests(throwing)
        val missing = com.unirem.core.api.TvId(
            com.unirem.core.api.IdentityRules.tvId(
                com.unirem.core.api.IdentityRules.resolve("ghost", null),
            )!!.hex,
        )
        assertFalse(store.forget(missing))
    }

    @Test
    fun migration1_2AddsLastIpColumnWithDefault() {
        val executed = mutableListOf<String>()
        val db = Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            if (method.name == "execSQL") {
                executed.add(args?.get(0) as String)
                null
            } else when (method.returnType) {
                Boolean::class.javaPrimitiveType -> false
                Int::class.javaPrimitiveType -> 0
                Long::class.javaPrimitiveType -> 0L
                else -> null
            }
        } as SupportSQLiteDatabase
        MIGRATION_1_2.migrate(db)
        assertEquals(
            listOf("ALTER TABLE tvs ADD COLUMN lastIp TEXT NOT NULL DEFAULT ''"),
            executed,
        )
    }
}
