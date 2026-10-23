package com.unirem.tv

import android.content.Context
import androidx.room.Room
import com.unirem.core.api.IdentityRules
import com.unirem.core.api.NetworkType
import com.unirem.core.api.RedactedLog
import com.unirem.core.api.TvId
import com.unirem.tv.api.TvCandidate
import java.time.Instant
import kotlinx.coroutines.flow.Flow

private const val TAG = "TvStore"

/**
 * Room-backed TV record holder. Stores one row per stable identity plus the
 * Keystore alias; secrets never enter Room. Session-only targets (no device
 * ID and no MAC) are never persisted and have no alias. Story 1.6 owns the
 * single delete path: [forget] removes one [TvId]-keyed row; upserts stay
 * keyed by [TvId].
 */
class RoomTvStore private constructor(private val dao: TvDao) {

    fun observeAll(): Flow<List<TvRecord>> = dao.observeAll()

    suspend fun all(): List<TvRecord> = try {
        dao.all()
    } catch (e: Exception) {
        RedactedLog.warn(TAG, "TV records unreadable")
        emptyList()
    }

    suspend fun byId(tvId: TvId): TvRecord? = try {
        dao.byId(tvId.hex)
    } catch (e: Exception) {
        RedactedLog.warn(TAG, "TV record unreadable")
        null
    }

    suspend fun count(): Int = try {
        dao.count()
    } catch (e: Exception) {
        RedactedLog.warn(TAG, "TV count unreadable")
        0
    }

    /**
     * Persists [candidate] under its stable identity. Returns null for
     * session-only targets (nothing stored, no alias minted).
     */
    suspend fun persistPaired(
        candidate: TvCandidate,
        networkType: NetworkType,
        networkInterface: String,
        now: Instant = Instant.now(),
    ): TvRecord? {
        val tvId = candidate.tvId ?: return null
        if (IdentityRules.isSessionOnly(candidate.identity)) return null
        val record = TvRecord(
            tvId = tvId.hex,
            identityKind = candidate.identity.source.wire,
            identityValue = candidate.identity.normalized,
            name = candidate.name,
            modelName = candidate.modelName,
            model = candidate.model,
            firmware = candidate.firmware,
            wifiMac = candidate.wifiMac,
            macProvenance = candidate.deviceIdField.ifBlank { "device.wifiMac" },
            networkType = networkType.wire(),
            networkInterface = networkInterface,
            alias = tvId.hex,
            lastIp = candidate.ip,
            pairedAtEpochSeconds = now.epochSecond,
        )
        return try {
            dao.upsert(record)
            RedactedLog.debug(TAG, "TV record stored for ${tvId.prefix}")
            record
        } catch (e: Exception) {
            RedactedLog.warn(TAG, "TV record unwritable")
            null
        }
    }

    /**
     * Forgets one durable TV by [TvId]: deletes the Room row plus dependents.
     * Secrets live in Keystore and are cleared separately via
     * `PairingStore.clear`; this holder never touches them. Returns true only
     * when no row for [tvId] remains (an already-absent row counts as gone);
     * false keeps the caller's record with a user-action-only retry.
     */
    suspend fun forget(tvId: TvId): Boolean = try {
        dao.deleteById(tvId.hex)
        val gone = dao.byId(tvId.hex) == null
        if (gone) {
            RedactedLog.debug(TAG, "TV record forgotten")
        } else {
            RedactedLog.warn(TAG, "TV record not forgotten")
        }
        gone
    } catch (e: Exception) {
        RedactedLog.warn(TAG, "TV record not forgotten")
        false
    }

    companion object {
        @Volatile
        private var instance: RoomTvStore? = null

        fun get(context: Context): RoomTvStore {
            val current = instance
            if (current != null) return current
            synchronized(this) {
                instance?.let { return it }
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    TvDatabase::class.java,
                    "unirem-tvs",
                ).addMigrations(MIGRATION_1_2).fallbackToDestructiveMigration(false).build()
                val created = RoomTvStore(db.tvDao())
                instance = created
                return created
            }
        }

        /** Test seam: wraps an in-memory DAO without touching disk. */
        fun forTests(dao: TvDao): RoomTvStore = RoomTvStore(dao)
    }
}
