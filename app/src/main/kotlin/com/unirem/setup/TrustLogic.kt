package com.unirem.setup

import com.unirem.tv.api.PairingEntry

/**
 * Pure trust-sheet rules for Story 1.6 (JVM-tested, no Android dependency).
 *
 * - The sheet is durable-only: a stored record keyed by TvId. Session-only
 *   targets never get a sheet.
 * - Forget is claimed only when the Room row is gone AND no trust material
 *   remains. A missing Keystore alias counts as already deleted.
 * - Cancel or dismiss deletes nothing; failure keeps the record with a
 *   user-action-only retry.
 */
object TrustLogic {

    sealed interface ForgetDecision {
        data object Succeeded : ForgetDecision
        data object Failed : ForgetDecision
    }

    /**
     * True only when the Room row is gone and no trust material remains
     * after the clear. A null [afterClear] covers both a removed entry and
     * an alias that was already missing.
     */
    fun decideForget(roomGone: Boolean, afterClear: PairingEntry?): ForgetDecision =
        if (roomGone && afterClear == null) ForgetDecision.Succeeded else ForgetDecision.Failed

    /**
     * Production sheet predicate: the sheet is offered only for a durable TV
     * with a stored record, an open request for that TvId, and a finished
     * Keystore load. Session-only targets never get a sheet.
     */
    fun sheetVisible(
        sessionOnlyActive: Boolean,
        trustTvIdHex: String?,
        hasRecord: Boolean,
        entryLoaded: Boolean,
    ): Boolean =
        !sessionOnlyActive && trustTvIdHex != null && hasRecord && entryLoaded
}
