package com.unirem.connection

import com.unirem.core.api.TvId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-scoped target owner (Stories 1.7, 1.9).
 *
 * Holds at most one selected [TvId]; no screen owns the target and no
 * last-used value is persisted. The shell auto-selects when exactly one
 * stored record exists ([TargetSwitchRules.autoSelect]) and clears when zero
 * remain; the status surface selects on open. A relaunch starts with
 * [TargetSession.NoTarget] and the shell shows "Paired". Session-only targets
 * have no resolvable identity and therefore never become a [TargetSession].
 */
sealed interface TargetSession {
    data object NoTarget : TargetSession
    data class Selected(val tvIdHex: String) : TargetSession
}

/**
 * Pure single-target rules for Story 1.9 (JVM-tested, no Android dependency).
 *
 * - The target is a [TvId] hex held by one process-scoped holder; it is never
 *   an index, an IP, or a position.
 * - Exactly one stored TV auto-selects on cold start; zero TVs yield
 *   [TargetSession.NoTarget]. More than one never occurs in V1; the rule
 *   returns [TargetSession.NoTarget] rather than guessing.
 * - Selecting the current TV is a no-op: the sheet closes with no reconnect,
 *   reset, or synthesized command.
 * - A selected target with no readable record (forgotten or unreadable) shows
 *   "TV unavailable" with no invented fallback target.
 * - Session-only targets carry no [TvId], so they never select and never open
 *   the sheet; they render caret-less chrome instead.
 */
object TargetSwitchRules {

    /** One stored TV selects; zero (or an ambiguous many) select nothing. */
    fun autoSelect(storedTvIdHexes: List<String>): TargetSession =
        if (storedTvIdHexes.size == 1) {
            TargetSession.Selected(storedTvIdHexes[0])
        } else {
            TargetSession.NoTarget
        }

    /** True when [rowTvIdHex] is the currently selected target. */
    fun isCurrent(target: TargetSession, rowTvIdHex: String): Boolean =
        (target as? TargetSession.Selected)?.tvIdHex == rowTvIdHex

    /**
     * The switcher sheet opens only for a selected durable target behind an
     * explicit open request, never for a session-only target, and never on
     * top of the trust sheet (one sheet at a time).
     */
    fun sheetVisible(
        target: TargetSession,
        requested: Boolean,
        sessionOnlyActive: Boolean,
        otherSheetOpen: Boolean,
    ): Boolean =
        requested && !sessionOnlyActive && !otherSheetOpen && target is TargetSession.Selected

    /** True when the target is selected but no readable record backs it. */
    fun rowUnreadable(target: TargetSession, hasRecord: Boolean): Boolean =
        target is TargetSession.Selected && !hasRecord

    /**
     * Cold-start projection: the holder value the stored records call for.
     * Null when the holder already reflects storage, so callers apply the
     * result only through `select`/`clear` on a non-null projection.
     */
    fun projectTarget(storedTvIdHexes: List<String>, current: TargetSession): TargetSession? {
        val next = autoSelect(storedTvIdHexes)
        return if (next == current) null else next
    }

    /**
     * Stored hex behind the selected target. Null for [TargetSession.NoTarget]
     * and when the selection matches no stored record (forgotten or
     * unreadable): no fallback is ever invented.
     */
    fun resolveTargetHex(target: TargetSession, storedTvIdHexes: List<String>): String? {
        val hex = (target as? TargetSession.Selected)?.tvIdHex ?: return null
        return hex.takeIf { storedTvIdHexes.contains(it) }
    }

    /** Target lifecycle events with a decided outcome. */
    enum class TargetEvent {
        SurfaceLeft,
        ForgetSucceeded,
    }

    /**
     * Leaving a surface retains the process-scoped target; a successful
     * Forget clears it to [TargetSession.NoTarget].
     */
    fun reduce(target: TargetSession, event: TargetEvent): TargetSession = when (event) {
        TargetEvent.SurfaceLeft -> target
        TargetEvent.ForgetSucceeded -> TargetSession.NoTarget
    }
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
