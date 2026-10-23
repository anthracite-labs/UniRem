package com.unirem.core.api

/**
 * Sealed failure types, one per module. Members arrive with their stories;
 * each starts with the local-network denial mapped from the gate state. No
 * exception crosses a module boundary.
 */
sealed interface TvFailure {
    /** The gate denied local-network access; no attempt ran. */
    data object LanDenied : TvFailure
    /** Pin mismatch or fresh `/api/v2/` identity differs; token never sent. */
    data object IdentityChanged : TvFailure
    /** Channel reported `ms.channel.unauthorized` or TV re-prompted; no retry with old token. */
    data object PairingRequired : TvFailure
}

/** Sealed failure type for the cast seam. See [TvFailure]. */
sealed interface CastFailure {
    /** The gate denied local-network access; no attempt ran. */
    data object LanDenied : CastFailure
}
