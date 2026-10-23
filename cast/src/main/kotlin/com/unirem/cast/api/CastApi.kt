package com.unirem.cast.api

import com.unirem.core.api.CastFailure
import com.unirem.core.api.LanAccess

/**
 * Placeholder for the cast seam. Members arrive with their story; this story
 * only proves the module graph plus the denial mapping.
 */
interface CastApi {
    companion object {
        /**
         * Pure gate consultation: a [LanAccess.Denied] gate blocks the attempt
         * before any socket work and maps to [CastFailure.LanDenied]. Any
         * other state returns null (the attempt may proceed). Never requests
         * a permission, never holds a lock, never launches UI.
         */
        fun checkLanAccess(access: LanAccess): CastFailure? =
            if (access == LanAccess.Denied) CastFailure.LanDenied else null
    }
}
