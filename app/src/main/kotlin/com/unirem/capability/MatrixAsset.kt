package com.unirem.capability

import android.content.Context
import com.unirem.core.api.CapabilityInput
import com.unirem.core.api.CapabilityLabel
import com.unirem.core.api.MatrixRules
import com.unirem.core.api.RedactedLog

private const val TAG = "MatrixAsset"

/**
 * Release-readable matrix asset loader (Story 1.8).
 *
 * Reads `assets/verification-matrix.json`, parses it with the `:core` schema,
 * and rejects structural violations. A missing or malformed asset is never a
 * crash: the loader logs one quiet redacted diagnostic and serves an empty
 * asset, so every affected key resolves `NotTested` and the card stays on its
 * best-effort line.
 */
object MatrixAsset {

    const val ASSET_NAME = "verification-matrix.json"

    fun load(context: Context): CapabilityInput.Asset {
        val json = try {
            context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            RedactedLog.warn(TAG, "Matrix asset unreadable")
            return empty()
        }
        return loadFromText(json)
    }

    /** Parses and validates in-memory text; falls back to empty on any fault. */
    fun loadFromText(json: String): CapabilityInput.Asset {
        return try {
            val asset = MatrixRules.parseAsset(json)
            val errors = MatrixRules.validate(asset)
            if (errors.isNotEmpty()) {
                RedactedLog.warn(TAG, "Matrix asset invalid")
                empty()
            } else {
                RedactedLog.debug(TAG, "Matrix asset loaded")
                asset
            }
        } catch (e: Exception) {
            RedactedLog.warn(TAG, "Matrix asset unreadable")
            empty()
        }
    }

    fun empty(): CapabilityInput.Asset = CapabilityInput.Asset(matrixVersion = 0, rows = emptyList())
}

/**
 * Card copy mapping for the one durable TV-card label (Story 1.8).
 *
 * `Works` reads "Full control — confirms keys"; every other label reads the
 * best-effort line. The four-value label stays available for Epic 2 details;
 * only this two-way card mapping ships in V1. Strings are injected so JVM
 * tests pin them without resources.
 */
object CapabilityCopy {

    fun cardLine(
        label: CapabilityLabel?,
        fullControl: String,
        bestEffort: String,
    ): String = if (label == CapabilityLabel.Works) fullControl else bestEffort

    /**
     * Renders one observed row as copy-paste asset JSON for the promotion
     * runbook. Carries matrix fields only: never the harness note, never an
     * identity beyond the exact model and firmware observed.
     */
    fun rowJson(row: CapabilityInput.Row): String = buildString {
        append("{\n")
        append("  \"model\": \"${escape(row.model)}\",\n")
        append("  \"firmware\": \"${escape(row.firmware)}\",\n")
        append("  \"networkType\": \"${row.networkType.name}\",\n")
        append("  \"capability\": \"${row.key.capability.name}\",\n")
        append("  \"scope\": \"${row.key.scope.name}\",\n")
        append("  \"result\": \"${row.result.name}\",\n")
        append("  \"matrixVersion\": ${row.matrixVersion}\n")
        append("}")
    }

    private fun escape(raw: String): String = buildString {
        for (char in raw) {
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char.code < 0x20) {
                    append("\\u%04x".format(char.code))
                } else {
                    append(char)
                }
            }
        }
    }
}
