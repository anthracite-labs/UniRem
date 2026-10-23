package com.unirem.tv.channel

import com.unirem.core.api.TvId

/**
 * Pure re-pair rules (Story 1.7).
 *
 * Re-pair reuses the Story 1.5 Allow/Deny flow token-less with the stored
 * name/identity and the always-visible no-remote path. Success commits only
 * when the fresh `/api/v2/` stable identity equals the stored [TvId]; a
 * mismatch fails closed with stored trust untouched. Deny, timeout, or
 * failure keeps the old record and trust with the banner staying.
 */
enum class RePairPhase {
    Idle,
    Pairing,
    WaitingAllow,
    Succeeded,
    Denied,
    TimedOut,
    Failed,
    IdentityMismatch,
}

object RePairRules {

    /** True only when the fresh identity resolves to exactly the stored TvId. */
    fun freshMatchesStored(freshTvId: TvId?, storedTvId: TvId): Boolean =
        freshTvId != null && freshTvId.hex == storedTvId.hex

    /** Terminal failure keeps old trust: nothing is overwritten or retried. */
    fun keepsOldTrust(phase: RePairPhase): Boolean = when (phase) {
        RePairPhase.Denied,
        RePairPhase.TimedOut,
        RePairPhase.Failed,
        RePairPhase.IdentityMismatch,
        RePairPhase.Idle,
        RePairPhase.Pairing,
        RePairPhase.WaitingAllow,
        -> true
        RePairPhase.Succeeded -> false
    }

    /** Commit requires an Allow with a matching fresh identity. */
    fun mayCommit(allowed: Boolean, freshMatches: Boolean): Boolean = allowed && freshMatches
}
