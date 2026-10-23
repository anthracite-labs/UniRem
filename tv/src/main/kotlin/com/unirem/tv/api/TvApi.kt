package com.unirem.tv.api

import com.unirem.core.api.LanAccess
import com.unirem.core.api.NetworkType
import com.unirem.core.api.StableIdentity
import com.unirem.core.api.TvFailure

/**
 * TV seam for Story 1.5. The gate mapping stays pure; discovery and pairing
 * travel through the seams below. Cross-module declarations live only in this
 * `api` package; `:tv` never reads Room/Keystore directly.
 */
interface TvApi {
    companion object {
        /**
         * Pure gate consultation: a [LanAccess.Denied] gate blocks the attempt
         * before any socket work and maps to [TvFailure.LanDenied]. Any other
         * state returns null (the attempt may proceed). Never requests a
         * permission, never holds a scope, never launches UI.
         */
        fun checkLanAccess(access: LanAccess): TvFailure? =
            if (access == LanAccess.Denied) TvFailure.LanDenied else null
    }
}

/** Token and pin held per stable identity. Secrets never reach Room. */
data class PairingEntry(
    val token: String?,
    val pin: String,
)

/**
 * Persistence port declared in `:tv`, implemented in `:app`. The alias is the
 * [com.unirem.core.api.TvId] hash, never an IP; Room holds the record plus
 * the alias while the secrets live in Keystore-backed storage.
 */
interface PairingStore {
    suspend fun save(tvId: com.unirem.core.api.TvId, token: String?, pin: String)
    suspend fun load(tvId: com.unirem.core.api.TvId): PairingEntry?
    /** Removes the entry. Returns true only when an entry was present. */
    suspend fun clear(tvId: com.unirem.core.api.TvId): Boolean
}

/**
 * One discovered or manually read TV before pairing. Firmware is stored
 * byte-exact, never normalized; the full `wifiMac` stays here for
 * provenance and is masked before display or diagnostics.
 */
data class TvCandidate(
    val ip: String,
    val name: String,
    val modelName: String,
    val model: String,
    val firmware: String,
    val tokenAuthSupport: Boolean,
    val wifiMac: String,
    val deviceId: String,
    val deviceIdField: String,
    val powerState: String,
    val reportedNetworkType: String,
) {
    val identity: StableIdentity =
        com.unirem.core.api.IdentityRules.resolve(deviceId, wifiMac)
    val tvId: com.unirem.core.api.TvId? =
        com.unirem.core.api.IdentityRules.tvId(identity)
    val sessionOnly: Boolean = identity == StableIdentity.SessionOnly
    val displayName: String =
        name.ifBlank { modelName.ifBlank { model.ifBlank { ip } } }
    val modelLabel: String get() = modelName.ifBlank { model }
}

/**
 * Discovery result. A read failure is a diagnostic row carried in [Failed];
 * it is never a [TvFailure] that later stories could confuse with pairing
 * denial.
 */
sealed interface DiscoveryResult {
    data class Found(val targets: List<TvCandidate>) : DiscoveryResult
    data object NoDiscovery : DiscoveryResult
    data class LanDenied(val reason: String) : DiscoveryResult
    data class Failed(val reason: String, val httpCode: Int? = null) : DiscoveryResult
}

/** Discovery seam: SSDP scan plus manual-IP identity read. */
interface TvDiscoveryGateway {
    suspend fun scan(): DiscoveryResult
    suspend fun readManual(ip: String): DiscoveryResult
}

/** Pairing outcome. Silence is [TimedOut], never a failure claim. */
sealed interface PairOutcome {
    data class Paired(val token: String?, val pin: String) : PairOutcome
    data object Denied : PairOutcome
    data object TimedOut : PairOutcome
    /** `TokenAuthSupport=true` but the channel carried no token: store nothing. */
    data object TokenMissing : PairOutcome
    data class Failed(val reason: String) : PairOutcome
}

/** Pairing seam: token-less first WSS, store on `ms.channel.connect`. */
interface TvPairingControl {
    suspend fun pair(candidate: TvCandidate, timeoutSeconds: Long = PAIR_TIMEOUT_SECONDS): PairOutcome

    companion object {
        const val PAIR_TIMEOUT_SECONDS = 30L
    }
}

/** Persisted TV record shape (Room holds this plus the alias in `:app`). */
data class TvRecordView(
    val tvIdHex: String,
    val identityKind: String,
    val identityValue: String,
    val name: String,
    val model: String,
    val firmware: String,
    val wifiMac: String,
    val macProvenance: String,
    val networkType: NetworkType,
    val networkInterface: String,
    val alias: String,
    val pairedAtEpochSeconds: Long,
)
