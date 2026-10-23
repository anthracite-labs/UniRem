package com.unirem.core.api

import java.security.MessageDigest
import java.util.Locale

/**
 * Stable identity for one TV (Story 1.5).
 *
 * Precedence is device ID first, byte-exact MAC second, under one
 * normalization rule applied identically to both inputs: trim, lowercase
 * (Locale.ROOT), remove whitespace. Separators inside a MAC are preserved.
 * Firmware strings are stored byte-exact elsewhere and never normalized here.
 * JVM-only; no Android import.
 */
object IdentityRules {

    fun normalize(raw: String?): String? {
        if (raw == null) return null
        val normalized = raw.trim().lowercase(Locale.ROOT).filterNot { it.isWhitespace() }
        return normalized.ifEmpty { null }
    }

    /** Device ID first, byte-exact MAC second, session-only when neither exists. */
    fun resolve(deviceId: String?, mac: String?): StableIdentity {
        normalize(deviceId)?.let { return StableIdentity.DeviceId(it) }
        normalize(mac)?.let { return StableIdentity.Mac(it) }
        return StableIdentity.SessionOnly
    }

    /** TvId is the hash of exactly the resolved identity, never an IP. */
    fun tvId(identity: StableIdentity): TvId? = when (identity) {
        StableIdentity.SessionOnly -> null
        is StableIdentity.DeviceId -> TvId(sha256Hex("device-id:" + identity.normalized))
        is StableIdentity.Mac -> TvId(sha256Hex("mac:" + identity.normalized))
    }

    fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return buildString(digest.size * 2) {
            for (byte in digest) {
                val unsigned = byte.toInt() and 0xFF
                append(HEX[unsigned ushr 4])
                append(HEX[unsigned and 0x0F])
            }
        }
    }

    /** True when the identity cannot persist: no device ID and no MAC. */
    fun isSessionOnly(identity: StableIdentity): Boolean = identity == StableIdentity.SessionOnly

    /**
     * Masked MAC for display and diagnostics: at most the last two octets
     * survive, the rest are literal "xx". Returns "" for blank input and a
     * fixed placeholder when the input is not a six-group MAC.
     */
    fun maskMac(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val groups = raw.trim().split(Regex("[:-]"))
        if (groups.size != 6 || groups.any { it.isBlank() }) return "xx:xx:xx:xx:xx:xx"
        val tail = groups.takeLast(2).map { it.trim().lowercase(Locale.ROOT).takeLast(2).padStart(2, 'x') }
        return "xx:xx:xx:xx:${tail[0]}:${tail[1]}"
    }

    /**
     * Manual-IP form rule: IPv4/IPv6 literal, private per [PrivatePeer].
     * Public literals, DNS names, and blanks return false before any request.
     */
    fun validateIpLiteral(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        return PrivatePeer.isPrivateLiteral(value)
    }

    private const val HEX = "0123456789abcdef"
}

enum class IdentitySource(val wire: String) {
    DEVICE_ID("device-id"),
    MAC("mac"),
    NONE("none"),
}

sealed interface StableIdentity {
    val source: IdentitySource
    val normalized: String

    data class DeviceId(override val normalized: String) : StableIdentity {
        override val source: IdentitySource get() = IdentitySource.DEVICE_ID
    }

    data class Mac(override val normalized: String) : StableIdentity {
        override val source: IdentitySource get() = IdentitySource.MAC
    }

    data object SessionOnly : StableIdentity {
        override val source: IdentitySource get() = IdentitySource.NONE
        override val normalized: String get() = ""
    }
}

/** A SHA-256 hash of [StableIdentity]. Only a prefix is ever logged. */
data class TvId(val hex: String) {
    val prefix: String get() = hex.take(PREFIX_LENGTH)

    companion object {
        const val PREFIX_LENGTH = 12
    }
}

/** One target's identity, labelled for collision reporting. */
data class IdentityEntry(
    val label: String,
    val identity: StableIdentity,
    val tvId: TvId?,
)

data class IdentityCollision(
    val tvId: TvId,
    val labels: List<String>,
)

object CollisionRules {

    /**
     * A collision means two targets resolved to the same TvId. If both are the
     * same physical TV seen on two addresses this is not a second TV; the
     * caller still records the ambiguity because identity alone cannot tell
     * them apart. Session-only entries never collide.
     */
    fun collisions(entries: List<IdentityEntry>): List<IdentityCollision> =
        entries
            .filter { it.tvId != null }
            .groupBy { it.tvId!! }
            .filterValues { it.size > 1 }
            .map { (tvId, group) -> IdentityCollision(tvId, group.map { it.label }.sorted()) }
            .sortedBy { it.tvId.hex }
}
