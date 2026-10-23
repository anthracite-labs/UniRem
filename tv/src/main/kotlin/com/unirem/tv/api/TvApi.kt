package com.unirem.tv.api

import com.unirem.core.api.LanAccess
import com.unirem.core.api.TvFailure

/**
 * Placeholder for the TV seam. Members arrive with the discovery/pairing
 * story; this story only proves the module graph plus the denial mapping.
 */
interface TvApi {
    companion object {
        /**
         * Pure gate consultation: a [LanAccess.Denied] gate blocks the attempt
         * before any socket work and maps to [TvFailure.LanDenied]. Any other
         * state returns null (the attempt may proceed). Never requests a
         * permission, never holds a lock, never launches UI.
         */
        fun checkLanAccess(access: LanAccess): TvFailure? =
            if (access == LanAccess.Denied) TvFailure.LanDenied else null
    }
}
