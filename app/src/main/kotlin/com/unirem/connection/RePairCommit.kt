package com.unirem.connection

import com.unirem.core.api.RedactedLog
import com.unirem.core.api.TvId
import com.unirem.tv.RoomTvStore
import com.unirem.tv.api.PairingStore
import com.unirem.tv.api.TvCandidate
import com.unirem.tv.api.rotateAlias

private const val TAG = "RePair"

/**
 * Atomic re-pair commit (Story 1.7).
 *
 * Re-pair reuses the Story 1.5 Allow/Deny flow token-less; this helper runs
 * only after Allow with a fresh `/api/v2/` identity that equals the stored
 * [TvId]. It writes the new token+pin under the new alias, points the record
 * at it (name, model, firmware, IP, interface updated), then deletes the old
 * alias only after the commit. Deny, timeout, failure, or a fresh-identity
 * mismatch fails closed: the old record and trust stay intact and nothing is
 * retried. Old trust is deleted only after the new commit verifies.
 */
object RePairCommit {

    suspend fun commit(
        storedTvId: TvId,
        freshCandidate: TvCandidate,
        freshTvId: TvId?,
        token: String?,
        pin: String,
        pairingStore: PairingStore,
        tvStore: RoomTvStore,
        networkTypeWire: String,
        networkInterface: String,
    ): Boolean {
        if (freshTvId == null || freshTvId.hex != storedTvId.hex) {
            RedactedLog.warn(TAG, "Fresh identity mismatch, re-pair fails closed")
            return false
        }
        // Write the new alias first; the old alias dies only after commit.
        val newAliasId = freshTvId
        val committed = pairingStore.rotateAlias(storedTvId, newAliasId, token, pin)
        if (!committed) {
            RedactedLog.warn(TAG, "Re-pair trust commit failed")
            return false
        }
        val updated = tvStore.updateOnRePair(
            tvId = storedTvId,
            name = freshCandidate.name,
            model = freshCandidate.model,
            firmware = freshCandidate.firmware,
            ip = freshCandidate.ip,
            networkTypeWire = networkTypeWire,
            networkInterface = networkInterface,
            alias = newAliasId.hex,
        )
        if (updated == null) {
            RedactedLog.warn(TAG, "Re-pair record update failed")
            return false
        }
        RedactedLog.debug(TAG, "Re-pair committed")
        return true
    }
}
