package com.unirem.core.api.command

/**
 * Honest command truth for one tap (Story 1.11). A socket write is never a
 * TV action: [Submitted] means the frame left the phone, [Unconfirmed]
 * means the 3s window passed with no evidence, [Observed] means first
 * evidence won (machine or user), [Idle] means nothing is outstanding.
 * There is no boolean named `confirmed`; the UI reads this flow only.
 *
 * The generation is monotonic per socket session and lets adapters ignore
 * late or stale evidence. A new session restarts at [Idle].
 */
sealed interface CommandOutcome {
    data object Idle : CommandOutcome
    data class Submitted(val generation: Long) : CommandOutcome
    data class Unconfirmed(val generation: Long) : CommandOutcome
    data class Observed(val generation: Long, val userConfirmed: Boolean) : CommandOutcome
}
