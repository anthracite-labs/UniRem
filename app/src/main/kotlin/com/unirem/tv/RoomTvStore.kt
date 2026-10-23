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

@Database(entities = [TvRecord::class], version = 2, exportSchema = false)
abstract class TvDatabase : androidx.room.RoomDatabase() {
    abstract fun tvDao(): TvDao
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
