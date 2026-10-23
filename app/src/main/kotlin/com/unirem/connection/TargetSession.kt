package com.unirem.connection

import com.unirem.core.api.TvId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-scoped target owner (Story 1.7).
 *
 * Holds at most one selected [TvId]; no screen owns the target and no
 * last-used value is persisted. The status surface selects on open and
 * clears on leave; a relaunch starts with [TargetSession.NoTarget] and the
 * shell shows "Paired".
 */
sealed interface TargetSession {
    data object NoTarget : TargetSession
    data class Selected(val tvIdHex: String) : TargetSession
}

class TargetSessionHolder {

    private val _session = MutableStateFlow<TargetSession>(TargetSession.NoTarget)
    val session: StateFlow<TargetSession> = _session.asStateFlow()

    fun select(tvId: TvId) {
        _session.value = TargetSession.Selected(tvId.hex)
    }

    fun clear() {
        _session.value = TargetSession.NoTarget
    }
}
