package com.unirem.spikea.logic

enum class RowResult(val wire: String) {
    PASS("pass"),
    FAIL("fail"),
    BLOCKED("blocked"),
}

enum class Scenario(val wire: String) {
    DISCOVERY("discovery"),
    NO_DISCOVERY("no-discovery"),
    MANUAL_IP("manual-ip"),
    LAN_DENIED("lan-denied"),
    SESSION_ONLY("session-only"),
    PAIR("pair"),
    DENY("deny"),
    RECONNECT("reconnect"),
    TOKEN_REJECTED("token-rejected"),
    PIN_REVALIDATE("pin-revalidate"),
    PIN_MISMATCH("pin-mismatch"),
    TWO_TV("two-tv"),
    SYNTHETIC_COLLISION("synthetic-collision"),
    CLEAR_PAIRING("clear-pairing"),
    SOCKET_CLOSED("socket-closed"),
    SECURITY("security"),
}

/**
 * One append-only JSONL row. There is deliberately no field for a token, pin,
 * SPKI, or full MAC: the schema cannot carry a secret even by accident.
 */
data class ResultRow(
    val runId: String,
    val at: String,
    val scenario: Scenario,
    val result: RowResult,
    val label: String = "",
    val model: String = "",
    val modelYear: String = "",
    val region: String = "",
    val firmware: String = "",
    val networkType: String = "",
    val networkDetail: String = "",
    val identitySource: String = "",
    val tvIdPrefix: String = "",
    val sessionOnly: Boolean = false,
    val allowWithoutRemote: String = "",
    val prompt: String = "",
    val tokenPersists: String = "",
    val rePrompt: String = "",
    val pinStable: String = "",
    val collision: String = "",
    val tokenSent: Boolean = false,
    val notes: String = "",
    val checks: Map<String, String> = emptyMap(),
)

object ResultsFormat {

    fun line(row: ResultRow): String {
        val parts = mutableListOf<String>()

        fun str(key: String, value: String) {
            parts += "${quote(key)}:${quote(value)}"
        }

        fun raw(key: String, value: String) {
            parts += "${quote(key)}:$value"
        }

        str("runId", row.runId)
        str("at", row.at)
        str("scenario", row.scenario.wire)
        str("result", row.result.wire)
        str("label", Redaction.scrub(row.label))
        str("model", Redaction.scrub(row.model))
        str("modelYear", Redaction.scrub(row.modelYear))
        str("region", Redaction.scrub(row.region))
        str("firmware", Redaction.scrub(row.firmware))
        str("networkType", Redaction.scrub(row.networkType))
        str("networkDetail", Redaction.scrub(row.networkDetail))
        str("identitySource", Redaction.scrub(row.identitySource))
        str("tvIdPrefix", row.tvIdPrefix.take(TvId.PREFIX_LENGTH))
        raw("sessionOnly", row.sessionOnly.toString())
        str("allowWithoutRemote", row.allowWithoutRemote)
        str("prompt", row.prompt)
        str("tokenPersists", row.tokenPersists)
        str("rePrompt", row.rePrompt)
        str("pinStable", row.pinStable)
        str("collision", row.collision)
        raw("tokenSent", row.tokenSent.toString())
        str("notes", Redaction.scrub(row.notes))

        val checks = row.checks
            .filterKeys(Redaction::keyAllowed)
            .mapValues { (_, value) -> Redaction.scrub(value) }
        val checkParts = checks.map { (key, value) -> "${quote(key)}:${quote(value)}" }
        parts += "${quote("checks")}:{${checkParts.joinToString(",")}}"

        return parts.joinToString(separator = ",", prefix = "{", postfix = "}")
    }

    private fun quote(value: String): String {
        val out = StringBuilder(value.length + 2)
        out.append('"')
        for (ch in value) {
            when (ch) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> if (ch < ' ') {
                    out.append("\\u")
                    out.append(ch.code.toString(16).padStart(4, '0'))
                } else {
                    out.append(ch)
                }
            }
        }
        out.append('"')
        return out.toString()
    }
}

enum class GateVerdict(val wire: String) {
    PASS("pass"),
    FAIL("fail"),
    BLOCKED("blocked"),
}

object GateRules {

    const val REQUIRED_DISTINCT_TVS = 3

    fun distinctTvIds(rows: List<ResultRow>): Set<String> =
        rows.asSequence()
            .filter { it.model.isNotBlank() && it.firmware.isNotBlank() && it.networkType.isNotBlank() }
            .map { it.tvIdPrefix }
            .filter { it.isNotBlank() }
            .toSet()

    /**
     * A row is a pass only with recorded evidence for the exact model, firmware,
     * and network. Blocked is not pass; a hard failure outranks a gap. Only
     * rows carrying all three metadata fields count toward the TV quorum, so a
     * discovery-only pass can never open the gate.
     */
    fun verdict(rows: List<ResultRow>): GateVerdict {
        if (rows.isEmpty()) return GateVerdict.BLOCKED
        if (rows.any { it.result == RowResult.FAIL }) return GateVerdict.FAIL
        if (distinctTvIds(rows).size < REQUIRED_DISTINCT_TVS) return GateVerdict.BLOCKED
        if (rows.any { it.result == RowResult.BLOCKED }) return GateVerdict.BLOCKED
        return GateVerdict.PASS
    }
}
