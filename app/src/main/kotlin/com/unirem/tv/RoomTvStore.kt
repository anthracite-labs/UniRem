package com.unirem.tv

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.unirem.core.api.NetworkType
import kotlinx.coroutines.flow.Flow

/** 1 -> 2 adds the `lastIp` column shown as "Last reached IP". */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tvs ADD COLUMN lastIp TEXT NOT NULL DEFAULT ''")
    }
}

/** 2 -> 3 adds the debug-only harness overlay table (Story 1.8). */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS matrix_overlay (" +
                "overlayKey TEXT NOT NULL PRIMARY KEY, " +
                "model TEXT NOT NULL, " +
                "firmware TEXT NOT NULL, " +
                "networkType TEXT NOT NULL, " +
                "capability TEXT NOT NULL, " +
                "scope TEXT NOT NULL, " +
                "result TEXT NOT NULL, " +
                "note TEXT NOT NULL DEFAULT '', " +
                "matrixVersion INTEGER NOT NULL, " +
                "observedAtEpochSeconds INTEGER NOT NULL)",
        )
    }
}

/**
 * Room record for one paired TV. Holds the identity plus the Keystore alias;
 * never holds the token, pin, or SPKI. Firmware is stored byte-exact, never
 * normalized; the full `wifiMac` stays here for provenance and is masked
 * before display.
 */
@Entity(tableName = "tvs")
data class TvRecord(
    @PrimaryKey val tvId: String,
    val identityKind: String,
    val identityValue: String,
    val name: String,
    val modelName: String,
    val model: String,
    val firmware: String,
    val wifiMac: String,
    val macProvenance: String,
    val networkType: String,
    val networkInterface: String,
    val alias: String,
    val pairedAtEpochSeconds: Long,
    /** Last IP that yielded this record, shown as "Last reached IP". Never a live probe. */
    val lastIp: String = "",
)

@Dao
interface TvDao {
    @Query("SELECT * FROM tvs ORDER BY pairedAtEpochSeconds DESC")
    fun observeAll(): Flow<List<TvRecord>>

    @Query("SELECT * FROM tvs ORDER BY pairedAtEpochSeconds DESC")
    suspend fun all(): List<TvRecord>

    @Query("SELECT * FROM tvs WHERE tvId = :tvId LIMIT 1")
    suspend fun byId(tvId: String): TvRecord?

    @Query("SELECT COUNT(*) FROM tvs")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: TvRecord)

    @Query("DELETE FROM tvs WHERE tvId = :tvId")
    suspend fun deleteById(tvId: String): Int
}

@Database(entities = [TvRecord::class, MatrixOverlayRow::class], version = 3, exportSchema = false)
abstract class TvDatabase : androidx.room.RoomDatabase() {
    abstract fun tvDao(): TvDao
    abstract fun overlayDao(): MatrixOverlayDao
}

/**
 * Debug-harness overlay row (Story 1.8): one observed matrix row per exact
 * key. The primary key is the exact key (model, firmware, wire network,
 * capability, scope), so Save replaces the prior row. Rows carry the asset
 * [matrixVersion] they were recorded against; stale rows never resolve.
 * Release builds never read this table.
 */
@Entity(tableName = "matrix_overlay")
data class MatrixOverlayRow(
    @PrimaryKey val overlayKey: String,
    val model: String,
    val firmware: String,
    val networkType: String,
    val capability: String,
    val scope: String,
    val result: String,
    val note: String = "",
    val matrixVersion: Int,
    val observedAtEpochSeconds: Long,
)

@Dao
interface MatrixOverlayDao {
    @Query("SELECT * FROM matrix_overlay ORDER BY observedAtEpochSeconds DESC")
    fun observeAll(): Flow<List<MatrixOverlayRow>>

    @Query("SELECT * FROM matrix_overlay ORDER BY observedAtEpochSeconds DESC")
    suspend fun all(): List<MatrixOverlayRow>

    @Query("SELECT * FROM matrix_overlay WHERE overlayKey = :overlayKey LIMIT 1")
    suspend fun byKey(overlayKey: String): MatrixOverlayRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: MatrixOverlayRow)

    @Query("DELETE FROM matrix_overlay WHERE overlayKey = :overlayKey")
    suspend fun deleteByKey(overlayKey: String): Int

    @Query("DELETE FROM matrix_overlay")
    suspend fun clearAll(): Int
}

/** Closed network-type wire mapping for the record. */
fun NetworkType.wire(): String = when (this) {
    NetworkType.Wifi -> "wifi"
    NetworkType.Ethernet -> "ethernet"
    NetworkType.Unknown -> "unknown"
}

fun networkTypeOf(wire: String): NetworkType = when (wire) {
    "wifi" -> NetworkType.Wifi
    "ethernet" -> NetworkType.Ethernet
    else -> NetworkType.Unknown
}
