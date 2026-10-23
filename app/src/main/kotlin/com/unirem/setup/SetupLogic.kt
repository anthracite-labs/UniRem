package com.unirem.setup

import com.unirem.core.api.IdentityRules
import com.unirem.core.api.PrivatePeer
import com.unirem.tv.api.DiscoveryResult
import com.unirem.tv.api.PairOutcome
import com.unirem.tv.api.TvCandidate

/**
 * Pure setup rules for Story 1.5 (JVM-tested, no Android dependency beyond
 * the plain data types above).
 *
 * - One scan per user press; results merge as found.
 * - Manual IP accepts private literals only, before any request.
 * - Same [com.unirem.core.api.TvId] dedupes to one record; session-only
 *   targets never persist.
 * - `TokenAuthSupport=true` with no token stores nothing.
 */
object SetupLogic {

    sealed interface ScanUi {
        data object Idle : ScanUi
        data object Scanning : ScanUi
        data class Found(val targets: List<TvCandidate>) : ScanUi
        data object Empty : ScanUi
        data class LanDenied(val reason: String) : ScanUi
        data class Error(val reason: String) : ScanUi
    }

    fun mapScan(result: DiscoveryResult): ScanUi = when (result) {
        is DiscoveryResult.Found -> if (result.targets.isEmpty()) ScanUi.Empty else ScanUi.Found(result.targets)
        DiscoveryResult.NoDiscovery -> ScanUi.Empty
        is DiscoveryResult.LanDenied -> ScanUi.LanDenied(result.reason)
        is DiscoveryResult.Failed -> ScanUi.Error(result.reason)
    }

    /** As-found merge: same TvId or same IP never appears twice. */
    fun mergeAsFound(existing: List<TvCandidate>, next: TvCandidate): List<TvCandidate> {
        val byId = next.tvId
        if (byId != null && existing.any { it.tvId == byId }) return existing
        if (existing.any { it.ip == next.ip }) return existing
        return existing + next
    }

    /** Collapse repeat sightings of one TV to a single card. */
    fun dedupeByTvId(candidates: List<TvCandidate>): List<TvCandidate> {
        val seen = LinkedHashSet<String>()
        val out = ArrayList<TvCandidate>(candidates.size)
        for (candidate in candidates) {
            val id = candidate.tvId?.hex ?: run {
                out += candidate
                continue
            }
            if (seen.add(id)) out += candidate
        }
        return out
    }

    /** Null when [input] may be read; otherwise the inline form message. */
    fun manualIpError(input: String): String? {
        if (input.isBlank()) return "Enter a private IP address."
        if (!PrivatePeer.isPrivateLiteral(input.trim())) return "Enter a private IP address."
        return null
    }

    /** Stable card key shared by the screen and the host: TvId hash or ip. */
    fun cardKey(candidate: TvCandidate): String =
        candidate.tvId?.hex ?: "ip:${candidate.ip}"

    fun isSessionOnly(candidate: TvCandidate): Boolean =
        IdentityRules.isSessionOnly(candidate.identity) || candidate.tvId == null

    /** True when the pair outcome may be persisted. */
    fun persists(outcome: PairOutcome, tokenAuthSupport: Boolean): Boolean = when (outcome) {
        is PairOutcome.Paired -> if (outcome.token == null) !tokenAuthSupport else true
        else -> false
    }

    fun denies(outcome: PairOutcome): Boolean = outcome == PairOutcome.Denied

    fun timedOut(outcome: PairOutcome): Boolean = outcome == PairOutcome.TimedOut
}
