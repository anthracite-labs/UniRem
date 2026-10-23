package com.unirem

import com.unirem.cast.api.CastApi
import com.unirem.core.api.CastFailure
import com.unirem.core.api.LanAccess
import com.unirem.core.api.LanDiagnosticCode
import com.unirem.core.api.NetworkType
import com.unirem.core.api.TvFailure
import com.unirem.gate.ContinueAction
import com.unirem.gate.LanAccessGate
import com.unirem.gate.LanAccessStore
import com.unirem.gate.MulticastLockController
import com.unirem.gate.StoredGateDecision
import com.unirem.tv.api.TvApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** In-memory retained decision for JVM tests. */
private class FakeLanAccessStore(
    initial: StoredGateDecision = StoredGateDecision(null, false),
) : LanAccessStore {
    var stored: StoredGateDecision = initial

    override suspend fun load(): StoredGateDecision = stored

    override suspend fun save(decision: StoredGateDecision) {
        stored = decision
    }
}

/** Fake lock handle for JVM tests: scripted acquire results with call counts. */
private class FakeMulticastLock(
    var acquireResult: Boolean = true,
) : MulticastLockController {
    var acquireCalls = 0
    var releaseCalls = 0

    override fun acquire(): Boolean {
        acquireCalls++
        return acquireResult
    }

    override fun release() {
        releaseCalls++
    }
}

/**
 * JVM tests for the gate: pure reducers for every state change, both failure
 * mappings through a fake gate, the injectable-transport diagnostic, and the
 * shared lock exclusion on success, failure, skip, nesting, and cancellation.
 */
class LanGateLogicTest {

    private fun testScope(): CoroutineScope = CoroutineScope(Dispatchers.Unconfined)

    private fun gate(
        sdk: Int = 37,
        stored: StoredGateDecision = StoredGateDecision(null, false),
        platformGranted: Boolean = false,
        transport: NetworkType = NetworkType.Wifi,
        lock: FakeMulticastLock = FakeMulticastLock(),
        store: FakeLanAccessStore = FakeLanAccessStore(stored),
    ): Pair<LanAccessGate, FakeLanAccessStore> {
        val gate = LanAccessGate(
            sdkInt = sdk,
            store = store,
            platformGranted = { platformGranted },
            transportSource = { transport },
            lockController = lock,
            scope = testScope(),
        )
        return gate to store
    }

    // Initial rule.

    @Test
    fun below37IsGrantedWithoutAsking() {
        val (gate) = gate(sdk = 36, stored = StoredGateDecision(null, false))
        assertEquals(LanAccess.Granted, gate.checkBeforeAttempt())
        assertEquals(
            LanAccess.Granted,
            LanAccessGate.resolveInitial(36, stored = null, platformGranted = false),
        )
    }

    @Test
    fun below37IgnoresStoredDenial() {
        assertEquals(
            LanAccess.Granted,
            LanAccessGate.resolveInitial(36, StoredGateDecision(LanAccess.Denied, true).access, false),
        )
    }

    @Test
    fun storedDecisionSurvivesRestart() {
        val (gate) = gate(sdk = 37, stored = StoredGateDecision(LanAccess.Denied, true))
        assertEquals(LanAccess.Denied, gate.checkBeforeAttempt())
        assertEquals(
            LanAccess.Granted,
            LanAccessGate.resolveInitial(37, StoredGateDecision(LanAccess.Granted, true).access, false),
        )
    }

    @Test
    fun freshInstallFallsBackToPlatformCheck() {
        assertEquals(
            LanAccess.Granted,
            LanAccessGate.resolveInitial(37, stored = null, platformGranted = true),
        )
        assertEquals(
            LanAccess.NotDeclared,
            LanAccessGate.resolveInitial(37, stored = null, platformGranted = false),
        )
    }

    // Prompt result and settings return.

    @Test
    fun permissionResultSettles() {
        assertEquals(LanAccess.Granted, LanAccessGate.reducePermissionResult(true))
        assertEquals(LanAccess.Denied, LanAccessGate.reducePermissionResult(false))
    }

    @Test
    fun settingsReturnSettlesBothDirections() {
        assertEquals(
            LanAccess.Granted,
            LanAccessGate.reduceSettingsReturn(LanAccess.Denied, 37, nowGranted = true),
        )
        assertEquals(
            LanAccess.Denied,
            LanAccessGate.reduceSettingsReturn(LanAccess.Granted, 37, nowGranted = false),
        )
        assertEquals(
            LanAccess.Denied,
            LanAccessGate.reduceSettingsReturn(LanAccess.Denied, 37, nowGranted = false),
        )
        assertEquals(
            LanAccess.NotDeclared,
            LanAccessGate.reduceSettingsReturn(LanAccess.NotDeclared, 37, nowGranted = false),
        )
        assertEquals(
            LanAccess.Granted,
            LanAccessGate.reduceSettingsReturn(LanAccess.Denied, 36, nowGranted = false),
        )
    }

    @Test
    fun permissionResultPersistsDecisionAndAsked() {
        val (gate, store) = gate(sdk = 37)
        gate.onPermissionResult(false)
        assertEquals(LanAccess.Denied, gate.checkBeforeAttempt())
        assertEquals(StoredGateDecision(LanAccess.Denied, true), store.stored)
        gate.onPermissionResult(true)
        assertEquals(LanAccess.Granted, gate.checkBeforeAttempt())
        assertEquals(StoredGateDecision(LanAccess.Granted, true), store.stored)
    }

    @Test
    fun settingsRevokeFromGrantedRecordsDenial() {
        val (gate, store) = gate(
            sdk = 37,
            stored = StoredGateDecision(LanAccess.Granted, true),
            platformGranted = false,
        )
        assertEquals(LanAccess.Granted, gate.checkBeforeAttempt())
        gate.onSettingsReturned(nowGranted = false)
        assertEquals(LanAccess.Denied, gate.checkBeforeAttempt())
        assertEquals(StoredGateDecision(LanAccess.Denied, true), store.stored)
    }

    @Test
    fun refreshRecomputesSnapshotFromLiveReads() {
        var platform = false
        var transport = NetworkType.Unknown
        val (gate) = gate(
            sdk = 37,
            stored = StoredGateDecision(LanAccess.NotDeclared, false),
            platformGranted = platform,
            transport = transport,
        )
        assertEquals(LanDiagnosticCode.Isolated, gate.snapshot.value.code)
        platform = true
        transport = NetworkType.Wifi
        val liveGate = LanAccessGate(
            sdkInt = 37,
            store = FakeLanAccessStore(StoredGateDecision(LanAccess.NotDeclared, false)),
            platformGranted = { platform },
            transportSource = { transport },
            lockController = FakeMulticastLock(),
            scope = testScope(),
        )
        liveGate.refresh()
        assertEquals(LanAccess.Granted, liveGate.checkBeforeAttempt())
        assertEquals(LanDiagnosticCode.Ok, liveGate.snapshot.value.code)
        assertEquals(NetworkType.Wifi, liveGate.snapshot.value.networkType)
    }

    // Continue rule.

    @Test
    fun continueRuleNeverPromptsWhenDenied() {
        assertEquals(
            ContinueAction.OpenSettings,
            LanAccessGate.continueActionFor(LanAccess.Denied, 37),
        )
        assertEquals(
            ContinueAction.OpenSettings,
            LanAccessGate.continueActionFor(LanAccess.Denied, 36),
        )
    }

    @Test
    fun continueRuleProceedsWhenGrantedOrBelow37() {
        assertEquals(ContinueAction.Proceed, LanAccessGate.continueActionFor(LanAccess.Granted, 37))
        assertEquals(
            ContinueAction.Proceed,
            LanAccessGate.continueActionFor(LanAccess.NotDeclared, 36),
        )
    }

    @Test
    fun continueRuleFiresPromptOnceWhenUndecided() {
        assertEquals(
            ContinueAction.FirePrompt,
            LanAccessGate.continueActionFor(LanAccess.NotDeclared, 37),
        )
    }

    @Test
    fun explainerContinueFiresPromptAndRecordsAsked() {
        val (gate, store) = gate(sdk = 37)
        var fired = 0
        gate.onExplainerContinue(firePrompt = { fired++ }, onProceed = { fail("must not proceed") })
        assertEquals(1, fired)
        assertEquals(LanAccess.NotDeclared, gate.checkBeforeAttempt())
        assertTrue(store.stored.asked)
    }

    @Test
    fun explainerContinueBelow37GrantsWithoutPrompt() {
        val (gate, store) = gate(sdk = 36)
        var proceeded = 0
        gate.onExplainerContinue(firePrompt = { fail("must not prompt") }, onProceed = { proceeded++ })
        assertEquals(1, proceeded)
        assertEquals(LanAccess.Granted, gate.checkBeforeAttempt())
        assertEquals(StoredGateDecision(LanAccess.Granted, false), store.stored)
    }

    @Test
    fun explainerContinueOnDeniedGateNeverPrompts() {
        val (gate) = gate(sdk = 37, stored = StoredGateDecision(LanAccess.Denied, true))
        gate.onExplainerContinue(firePrompt = { fail("must not prompt") }, onProceed = {})
        assertEquals(LanAccess.Denied, gate.checkBeforeAttempt())
    }

    // Denial mapping through a fake gate.

    @Test
    fun deniedBlocksBothPathsIdentically() {
        val (gate) = gate(sdk = 37, stored = StoredGateDecision(LanAccess.Denied, true))
        assertEquals(TvFailure.LanDenied, TvApi.checkLanAccess(gate.checkBeforeAttempt()))
        assertEquals(CastFailure.LanDenied, CastApi.checkLanAccess(gate.checkBeforeAttempt()))
    }

    @Test
    fun grantedAndUndecidedPassMapping() {
        assertNull(TvApi.checkLanAccess(LanAccess.Granted))
        assertNull(CastApi.checkLanAccess(LanAccess.Granted))
        assertNull(TvApi.checkLanAccess(LanAccess.NotDeclared))
        assertNull(CastApi.checkLanAccess(LanAccess.NotDeclared))
    }

    // Diagnostic snapshot.

    @Test
    fun snapshotMapsTransportToCode() {
        assertEquals(
            LanDiagnosticCode.Denied,
            LanAccessGate.snapshotFor(LanAccess.Denied, NetworkType.Wifi).code,
        )
        assertEquals(
            LanDiagnosticCode.Ok,
            LanAccessGate.snapshotFor(LanAccess.Granted, NetworkType.Wifi).code,
        )
        assertEquals(
            LanDiagnosticCode.Isolated,
            LanAccessGate.snapshotFor(LanAccess.Granted, NetworkType.Unknown).code,
        )
    }

    @Test
    fun snapshotNeverClaimsAScan() {
        for (access in LanAccess.entries) {
            for (network in NetworkType.entries) {
                assertFalse(LanAccessGate.snapshotFor(access, network).scanned)
            }
        }
    }

    @Test
    fun throwingTransportFallsBackToUnknown() {
        val gate = LanAccessGate(
            sdkInt = 37,
            store = FakeLanAccessStore(),
            platformGranted = { false },
            transportSource = { throw IllegalStateException("boom") },
            lockController = FakeMulticastLock(),
            scope = testScope(),
        )
        assertEquals(NetworkType.Unknown, gate.snapshot.value.networkType)
        assertEquals(LanDiagnosticCode.Isolated, gate.snapshot.value.code)
        gate.refresh()
        assertEquals(NetworkType.Unknown, gate.snapshot.value.networkType)
        assertEquals(LanDiagnosticCode.Isolated, gate.snapshot.value.code)
    }

    // Lock exclusion.

    @Test
    fun guardedRunReleasesOnSuccess() {
        var released = 0
        val result = LanAccessGate.guardedRun({ true }, { released++ }) { 42 }
        assertEquals(42, result)
        assertEquals(1, released)
    }

    @Test
    fun guardedRunReleasesOnFailure() {
        var released = 0
        try {
            LanAccessGate.guardedRun({ true }, { released++ }) {
                throw IllegalStateException("boom")
            }
            fail("must propagate")
        } catch (expected: IllegalStateException) {
            assertEquals(1, released)
        }
    }

    @Test
    fun guardedRunSkipsReleaseWhenNotAcquired() {
        var released = 0
        val result = LanAccessGate.guardedRun({ false }, { released++ }) { "ran" }
        assertEquals("ran", result)
        assertEquals(0, released)
    }

    @Test
    fun guardedRunSuspendReleasesOnSuccessAndFailure() = runBlocking {
        var released = 0
        val result = LanAccessGate.guardedRunSuspend({ true }, { released++ }) { 7 }
        assertEquals(7, result)
        assertEquals(1, released)
        try {
            LanAccessGate.guardedRunSuspend({ true }, { released++ }) {
                throw IllegalStateException("boom")
            }
            fail("must propagate")
        } catch (expected: IllegalStateException) {
            assertEquals(2, released)
        }
    }

    @Test
    fun guardedRunSuspendReleasesOnCancellation() = runBlocking {
        var released = 0
        val job = launch {
            LanAccessGate.guardedRunSuspend({ true }, { released++ }) {
                awaitCancellation()
            }
        }
        yield()
        job.cancelAndJoin()
        assertEquals(1, released)
    }

    @Test
    fun instanceLockDelegatesWithSharedExclusion() {
        val lock = FakeMulticastLock()
        val (gate) = gate(sdk = 37, lock = lock)
        val result = gate.withMulticastLock { "ok" }
        assertEquals("ok", result)
        assertEquals(1, lock.acquireCalls)
        assertEquals(1, lock.releaseCalls)
        assertFalse(gate.isLockHeld())
    }

    @Test
    fun instanceLockReleasesOnFailureAndRunsUnprotectedOnSkip() {
        val failing = FakeMulticastLock()
        val (failingGate) = gate(sdk = 37, lock = failing)
        try {
            failingGate.withMulticastLock { throw IllegalStateException("boom") }
            fail("must propagate")
        } catch (expected: IllegalStateException) {
            assertEquals(1, failing.releaseCalls)
            assertFalse(failingGate.isLockHeld())
        }
        val skipping = FakeMulticastLock(acquireResult = false)
        val (skippingGate) = gate(sdk = 37, lock = skipping)
        assertEquals("ran", skippingGate.withMulticastLock { "ran" })
        assertEquals(0, skipping.releaseCalls)
    }

    @Test
    fun nestedScopesShareTheSingleLock() {
        val lock = FakeMulticastLock()
        val (gate) = gate(sdk = 37, lock = lock)
        gate.withMulticastLock {
            gate.withMulticastLock {
                assertTrue(gate.isLockHeld())
            }
        }
        assertEquals(1, lock.acquireCalls)
        assertEquals(1, lock.releaseCalls)
        assertFalse(gate.isLockHeld())
    }

    @Test
    fun suspendScopeReleasesToo() = runBlocking {
        val lock = FakeMulticastLock()
        val gate = LanAccessGate(
            sdkInt = 37,
            store = FakeLanAccessStore(),
            platformGranted = { false },
            transportSource = { NetworkType.Wifi },
            lockController = lock,
            scope = this,
        )
        val result = gate.withMulticastLockSuspend { 11 }
        assertEquals(11, result)
        try {
            gate.withMulticastLockSuspend { throw IllegalStateException("boom") }
            fail("must propagate")
        } catch (expected: IllegalStateException) {
            assertEquals(2, lock.acquireCalls)
            assertEquals(2, lock.releaseCalls)
            assertFalse(gate.isLockHeld())
        }
    }
}
