package com.unirem

import androidx.sqlite.db.SupportSQLiteDatabase
import com.unirem.tv.MIGRATION_2_3
import com.unirem.tv.MatrixOverlayDao
import com.unirem.tv.MatrixOverlayRow
import com.unirem.tv.MatrixOverlayStore
import java.lang.reflect.Proxy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory overlay DAO so the debug store runs on the JVM. */
class FakeOverlayDao : MatrixOverlayDao {
    private val rows = LinkedHashMap<String, MatrixOverlayRow>()
    private val flow = MutableStateFlow<List<MatrixOverlayRow>>(emptyList())

    override fun observeAll(): Flow<List<MatrixOverlayRow>> = flow

    override suspend fun all(): List<MatrixOverlayRow> = rows.values.toList()

    override suspend fun byKey(overlayKey: String): MatrixOverlayRow? = rows[overlayKey]

    override suspend fun upsert(row: MatrixOverlayRow) {
        rows[row.overlayKey] = row
        flow.value = rows.values.toList()
    }

    override suspend fun deleteByKey(overlayKey: String): Int {
        val removed = rows.remove(overlayKey) != null
        flow.value = rows.values.toList()
        return if (removed) 1 else 0
    }

    override suspend fun clearAll(): Int {
        val count = rows.size
        rows.clear()
        flow.value = emptyList()
        return count
    }

    fun size(): Int = rows.size
}

/** Debug overlay store and v2→v3 migration tests for Story 1.8. */
class MatrixOverlayTest {

    private fun row(
        overlayKey: String = "k1",
        result: String = "Pass",
        version: Int = 1,
    ) = MatrixOverlayRow(
        overlayKey = overlayKey,
        model = "QN90C",
        firmware = "T-PTLAKUC-1000.1",
        networkType = "wifi",
        capability = "Keys",
        scope = "Device",
        result = result,
        matrixVersion = version,
        observedAtEpochSeconds = 1L,
    )

    @Test
    fun disabledStoreServesNothingAndIgnoresWrites(): Unit = runBlocking {
        val store = MatrixOverlayStore.disabled()
        assertFalse(store.save(row()))
        assertTrue(store.all().isEmpty())
        assertFalse(store.clear("k1"))
        assertFalse(store.clearAll())
    }

    @Test
    fun saveReplacesSameExactKey(): Unit = runBlocking {
        val dao = FakeOverlayDao()
        val store = MatrixOverlayStore.forTests(dao)
        assertTrue(store.save(row(result = "Partial")))
        assertTrue(store.save(row(result = "Pass")))
        assertEquals(1, dao.size())
        assertEquals("Pass", dao.byKey("k1")?.result)
    }

    @Test
    fun clearRemovesOnlyThatKey(): Unit = runBlocking {
        val dao = FakeOverlayDao()
        val store = MatrixOverlayStore.forTests(dao)
        store.save(row(overlayKey = "a"))
        store.save(row(overlayKey = "b"))
        assertTrue(store.clear("a"))
        assertEquals(listOf("b"), store.all().map { it.overlayKey })
        assertTrue(store.clearAll())
        assertTrue(store.all().isEmpty())
    }

    @Test
    fun overlayKeyDistinguishesEveryField(): Unit {
        val base = MatrixOverlayStore.overlayKeyOf("M", "F", "wifi", "Keys", "Device")
        assertFalse(base == MatrixOverlayStore.overlayKeyOf("M2", "F", "wifi", "Keys", "Device"))
        assertFalse(base == MatrixOverlayStore.overlayKeyOf("M", "F2", "wifi", "Keys", "Device"))
        assertFalse(base == MatrixOverlayStore.overlayKeyOf("M", "F", "ethernet", "Keys", "Device"))
        assertFalse(base == MatrixOverlayStore.overlayKeyOf("M", "F", "wifi", "Text", "Device"))
        assertFalse(base == MatrixOverlayStore.overlayKeyOf("M", "F", "wifi", "Keys", "NativeField"))
        assertEquals(base, MatrixOverlayStore.overlayKeyOf("M", "F", "wifi", "Keys", "Device"))
    }

    @Test
    fun migration2_3CreatesOverlayTable() {
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
        MIGRATION_2_3.migrate(db)
        assertEquals(1, executed.size)
        val sql = executed.single()
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS matrix_overlay"))
        for (column in listOf(
            "overlayKey", "model", "firmware", "networkType", "capability",
            "scope", "result", "matrixVersion", "observedAtEpochSeconds",
        )) {
            assertTrue("migration must create $column: $sql", sql.contains(column))
        }
    }
}
