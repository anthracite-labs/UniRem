package com.unirem.core.api.command

/**
 * Adapters emit evidence, never a verdict (Story 1.11). [TvEvent] is one
 * machine frame observed on the open channel, [StateProbe] is one state
 * read that observed the TV, [UserConfirmed] is the pilot tapping
 * "It worked" while [CommandOutcome.Unconfirmed] shows. Each carries the
 * generation it belongs to; the tracker ignores stale generations and
 * anything after the first win.
 */
sealed interface Evidence {
    val generation: Long

    data class TvEvent(override val generation: Long) : Evidence
    data class StateProbe(override val generation: Long) : Evidence
    data class UserConfirmed(override val generation: Long) : Evidence
}
