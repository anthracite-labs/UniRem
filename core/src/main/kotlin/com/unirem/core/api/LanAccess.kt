package com.unirem.core.api

import java.util.Locale

/**
 * Process-wide local-network permission state owned by the `:app`
 * [LanAccessGate][com.unirem.gate.LanAccessGate].
 *
 * - [Granted]: the platform allows local-network access (or the platform has
 *   no such permission, below API 37).
 * - [Denied]: the user refused; every network attempt must be blocked and
 *   mapped to the module `LanDenied` failure without touching the network.
 * - [NotDeclared]: setup has not asked yet; the explainer precedes any system
 *   prompt.
 */
enum class LanAccess {
    Granted,
    Denied,
    NotDeclared,
}

/**
 * Closed transport triple per AD-5. Never promoted from a label table; the
 * gate derives it from the live platform transport.
 */
enum class NetworkType {
    Wifi,
    Ethernet,
    Unknown,
}

/**
 * Quiet diagnostic code shown by the shell readout. It distinguishes denial
 * from isolation from ok and is never marketing copy.
 */
enum class LanDiagnosticCode {
    Denied,
    Isolated,
    Ok,
}

/**
 * Locale-stable wire string for [LanDiagnosticCode]. Uses [Locale.ROOT] so
 * the code never breaks under locales with dotted/dotless-I rules.
 */
fun LanDiagnosticCode.code(): String = name.lowercase(Locale.ROOT)

/**
 * Pure diagnostic rule: denial wins over everything; without [Granted] access
 * the device is isolated from the TV; a granted device on an unknown
 * transport is isolated; otherwise the path reads ok.
 */
fun resolveDiagnosticCode(access: LanAccess, networkType: NetworkType): LanDiagnosticCode =
    when {
        access == LanAccess.Denied -> LanDiagnosticCode.Denied
        access != LanAccess.Granted -> LanDiagnosticCode.Isolated
        networkType == NetworkType.Unknown -> LanDiagnosticCode.Isolated
        else -> LanDiagnosticCode.Ok
    }
