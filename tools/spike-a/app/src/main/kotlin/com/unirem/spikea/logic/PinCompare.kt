package com.unirem.spikea.logic

import java.security.MessageDigest
import java.util.Base64

enum class PinVerdict {
    MATCH,
    MISMATCH,
    MISSING,
    INVALID,
}

/**
 * A pin is base64(SHA-256(leaf SubjectPublicKeyInfo)). Comparison is
 * constant-time over the decoded bytes. The pin itself is never logged.
 */
object PinRules {

    private const val SHA256_BYTES = 32

    fun sha256Base64(spki: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(spki)
        return Base64.getEncoder().encodeToString(digest)
    }

    fun compare(stored: String?, leaf: String?): PinVerdict {
        val storedBytes = decode(stored)
        if (storedBytes == null) {
            return if (stored.isNullOrBlank()) PinVerdict.MISSING else PinVerdict.INVALID
        }
        val leafBytes = decode(leaf) ?: return PinVerdict.INVALID
        return if (MessageDigest.isEqual(storedBytes, leafBytes)) PinVerdict.MATCH else PinVerdict.MISMATCH
    }

    private fun decode(value: String?): ByteArray? {
        if (value.isNullOrBlank()) return null
        return try {
            Base64.getDecoder().decode(value.trim()).takeIf { it.size == SHA256_BYTES }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
