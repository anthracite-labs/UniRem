package com.unirem.core.api

/**
 * Sealed failure types, one per module. Members arrive with their stories;
 * each starts with the local-network denial mapped from the gate state. No
 * exception crosses a module boundary.
 */
sealed interface TvFailure {
    /** The gate denied local-network access; no attempt ran. */
    data object LanDenied : TvFailure
}

/** Sealed failure type for the cast seam. See [TvFailure]. */
sealed interface CastFailure {
    /** The gate denied local-network access; no attempt ran. */
    data object LanDenied : CastFailure
}
