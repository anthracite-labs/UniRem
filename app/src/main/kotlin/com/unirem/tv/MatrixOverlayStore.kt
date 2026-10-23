package com.unirem.tv

import com.unirem.core.api.RedactedLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private const val TAG = "MatrixOverlay"

/**
 * Debug-harness overlay behind the Room [MatrixOverlayDao] (Story 1.8).
 *
 * The harness records observed rows per exact key; Save replaces the prior
 * row for the same exact key and stale rows (version != asset version) are
 * never served. Release builds pass `isDebug = false`, which serves empty
 * lists and ignores writes, so release labels read the asset only.
 *
 * Notes and identities never reach logs; failures stay quiet and redacted.
 */
class MatrixOverlayStore(
    private val dao: MatrixOverlayDao?,
    private val isDebug: Boolean,
) {

    fun observe(): Flow<List<MatrixOverlayRow>> {
        if (!isDebug || dao == null) return flowOf(emptyList())
        return try {
            dao.observeAll()
        } catch (e: Exception) {
            RedactedLog.warn(TAG, "Overlay rows unreadable")
            flowOf(emptyList())
        }
    }

    suspend fun all(): List<MatrixOverlayRow> {
        if (!isDebug || dao == null) return emptyList()
        return try {
            dao.all()
        } catch (e: Exception) {
            RedactedLog.warn(TAG, "Overlay rows unreadable")
            emptyList()
        }
    }

    /** Saves one exact-key row, replacing the prior row for that key. */
    suspend fun save(row: MatrixOverlayRow): Boolean {
        if (!isDebug || dao == null) return false
        return try {
            dao.upsert(row)
            RedactedLog.debug(TAG, "Overlay row stored")
            true
        } catch (e: Exception) {
            RedactedLog.warn(TAG, "Overlay row unwritable")
            false
        }
    }

    suspend fun clear(overlayKey: String): Boolean {
        if (!isDebug || dao == null) return false
        return try {
            dao.deleteByKey(overlayKey)
            RedactedLog.debug(TAG, "Overlay row cleared")
            true
        } catch (e: Exception) {
            RedactedLog.warn(TAG, "Overlay row not cleared")
            false
        }
    }

    suspend fun clearAll(): Boolean {
        if (!isDebug || dao == null) return false
        return try {
            dao.clearAll()
            RedactedLog.debug(TAG, "Overlay rows cleared")
            true
        } catch (e: Exception) {
            RedactedLog.warn(TAG, "Overlay rows not cleared")
            false
        }
    }

    companion object {
        /**
         * Exact-key primary key: model, firmware, wire network, capability,
         * scope, joined on U+0001 (never part of a validated identity field).
         */
        fun overlayKeyOf(
            model: String,
            firmware: String,
            networkTypeWire: String,
            capability: String,
            scope: String,
        ): String = listOf(model, firmware, networkTypeWire, capability, scope)
            .joinToString(separator = "")

        /** Disabled store: release builds and tests that need no overlay. */
        fun disabled(): MatrixOverlayStore = MatrixOverlayStore(dao = null, isDebug = false)

        /** Test seam over an in-memory DAO. */
        fun forTests(dao: MatrixOverlayDao, isDebug: Boolean = true): MatrixOverlayStore =
            MatrixOverlayStore(dao, isDebug)
    }
}
