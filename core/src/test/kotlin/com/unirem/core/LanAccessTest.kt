package com.unirem.core

import com.unirem.core.api.CastFailure
import com.unirem.core.api.LanAccess
import com.unirem.core.api.LanDiagnosticCode
import com.unirem.core.api.NetworkType
import com.unirem.core.api.TvFailure
import com.unirem.core.api.code
import com.unirem.core.api.resolveDiagnosticCode
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM tests for the gate tri-state, the closed transport triple, the pure
 * diagnostic rule, locale-stable codes, and the sealed module failures.
 */
class LanAccessTest {

    @Test
    fun triStateHasExactlyThreeMembers() {
        assertEquals(
            setOf(LanAccess.Granted, LanAccess.Denied, LanAccess.NotDeclared),
            LanAccess.entries.toSet(),
        )
    }

    @Test
    fun networkTypeIsClosedTriple() {
        assertEquals(
            setOf(NetworkType.Wifi, NetworkType.Ethernet, NetworkType.Unknown),
            NetworkType.entries.toSet(),
        )
    }

    @Test
    fun denialWinsOverEveryTransport() {
        for (network in NetworkType.entries) {
            assertEquals(
                "denied on $network",
                LanDiagnosticCode.Denied,
                resolveDiagnosticCode(LanAccess.Denied, network),
            )
        }
    }

    @Test
    fun grantedKnownTransportsReadOk() {
        assertEquals(
            LanDiagnosticCode.Ok,
            resolveDiagnosticCode(LanAccess.Granted, NetworkType.Wifi),
        )
        assertEquals(
            LanDiagnosticCode.Ok,
            resolveDiagnosticCode(LanAccess.Granted, NetworkType.Ethernet),
        )
    }

    @Test
    fun grantedUnknownTransportReadsIsolated() {
        assertEquals(
            LanDiagnosticCode.Isolated,
            resolveDiagnosticCode(LanAccess.Granted, NetworkType.Unknown),
        )
    }

    @Test
    fun undecidedAccessReadsIsolated() {
        for (network in NetworkType.entries) {
            assertEquals(
                "not-declared on $network",
                LanDiagnosticCode.Isolated,
                resolveDiagnosticCode(LanAccess.NotDeclared, network),
            )
        }
    }

    @Test
    fun codeStringsAreLowercaseRoots() {
        assertEquals("denied", LanDiagnosticCode.Denied.code())
        assertEquals("isolated", LanDiagnosticCode.Isolated.code())
        assertEquals("ok", LanDiagnosticCode.Ok.code())
    }

    @Test
    fun codeStringsSurviveDottedILocale() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale("tr"))
        try {
            assertEquals("isolated", LanDiagnosticCode.Isolated.code())
            assertEquals("denied", LanDiagnosticCode.Denied.code())
            assertEquals("ok", LanDiagnosticCode.Ok.code())
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun failuresStartWithLanDenied() {
        val tv: TvFailure = TvFailure.LanDenied
        val cast: CastFailure = CastFailure.LanDenied
        assertEquals(TvFailure.LanDenied, tv)
        assertEquals(CastFailure.LanDenied, cast)
    }
}
