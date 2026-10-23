package com.unirem

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.unirem.core.api.IdentityRules
import com.unirem.core.api.LanAccess
import com.unirem.core.api.CapabilityKey
import com.unirem.core.api.CapabilityLabel
import com.unirem.core.api.NetworkType
import com.unirem.core.api.RedactedLog
import com.unirem.core.api.TvId
import com.unirem.capability.CapabilityCopy
import com.unirem.capability.CapabilityLabelTracker
import com.unirem.capability.MatrixAsset
import com.unirem.capability.MatrixHarnessScreen
import com.unirem.capability.toCapabilityRow
import com.unirem.connection.BannerKind
import com.unirem.connection.ConnectionController
import com.unirem.connection.ConnectionUiRules
import com.unirem.connection.RePairCommit
import com.unirem.connection.TargetSession
import com.unirem.connection.TargetSessionHolder
import com.unirem.connection.TargetSwitchRules
import com.unirem.remote.RemoteRules
import com.unirem.remote.RemoteScreen
import com.unirem.gate.LanAccessGate
import com.unirem.gate.GateQuietAction
import com.unirem.setup.AddTvScreen
import com.unirem.setup.PairedScreen
import com.unirem.setup.PairingScreen
import com.unirem.setup.SetupLogic
import com.unirem.setup.SetupTopBar
import com.unirem.setup.TrustLogic
import com.unirem.setup.TrustSheet
import com.unirem.setup.TvSwitcherSheet
import com.unirem.tv.KeystorePairingStore
import com.unirem.tv.RoomTvStore
import com.unirem.tv.MatrixOverlayRow
import com.unirem.tv.MatrixOverlayStore
import com.unirem.tv.wire
import com.unirem.tv.api.DiscoveryResult
import com.unirem.tv.api.PairOutcome
import com.unirem.tv.api.PairingEntry
import com.unirem.tv.api.TvCandidate
import com.unirem.tv.api.TvGateways
import com.unirem.tv.api.FingerprintFormat
import com.unirem.tv.api.ConnectionBackoff
import com.unirem.tv.channel.ConnectionHealth
import com.unirem.ui.graphitemint.GraphiteMint
import com.unirem.ui.graphitemint.GraphiteMintSpacing
import com.unirem.ui.graphitemint.GraphiteMintTheme
import com.unirem.ui.graphitemint.GraphiteMintType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class MainActivity : ComponentActivity() {

    private lateinit var gate: LanAccessGate

    // The Activity owns the permission launcher; the gate stays Activity-free
    // and only receives the forwarded result.
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            RedactedLog.debug(TAG, "Permission result received")
            gate.onPermissionResult(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gate = (application as UniRemApp).gate
        enableEdgeToEdge()
        RedactedLog.debug(TAG, "Shell created")
        setContent {
            var explainerVisible by rememberSaveable { mutableStateOf(false) }
            GraphiteMintTheme {
                val snapshot by gate.snapshot.collectAsState()
                val scope = rememberCoroutineScope()
                var route by remember { mutableStateOf<SetupRoute>(SetupRoute.Shell) }
                var records by remember { mutableStateOf(listOf<com.unirem.tv.TvRecord>()) }
                var scanning by remember { mutableStateOf(false) }
                var scanDone by remember { mutableStateOf(false) }
                var targets by remember { mutableStateOf(listOf<TvCandidate>()) }
                var diagnostic by remember { mutableStateOf<String?>(null) }
                var manualIp by rememberSaveable { mutableStateOf("") }
                var manualError by remember { mutableStateOf<String?>(null) }
                var dismissed by remember { mutableStateOf(setOf<String>()) }
                var manualKeys by remember { mutableStateOf(setOf<String>()) }
                var scanJob by remember { mutableStateOf<Job?>(null) }
                var pairJob by remember { mutableStateOf<Job?>(null) }
                var pairingWaiting by remember { mutableStateOf(false) }
                var pairingDenied by remember { mutableStateOf(false) }
                var pairingTimedOut by remember { mutableStateOf(false) }
                var pairingTokenMissing by remember { mutableStateOf(false) }
                var pairingFailed by remember { mutableStateOf<String?>(null) }
                var pairedName by remember { mutableStateOf("") }
                var pairedModel by remember { mutableStateOf("") }
                var pairedSessionOnly by remember { mutableStateOf(false) }
                // Durable TvId hex behind the paired summary; null while
                // session-only. Drives the one capability plate (Story 1.8).
                var pairedTvIdHex by rememberSaveable { mutableStateOf<String?>(null) }
                val sessionTrust = remember { mutableStateMapOf<String, PairingEntry>() }
                // Reusable TvId-keyed trust sheet destination (Story 1.9 reuses
                // the same key). Holds the TvId hex, never an IP.
                var trustTvIdHex by rememberSaveable { mutableStateOf<String?>(null) }
                var trustConfirming by rememberSaveable { mutableStateOf(false) }
                var trustForgetting by remember { mutableStateOf(false) }
                var trustForgetFailed by remember { mutableStateOf(false) }
                var trustEntry by remember { mutableStateOf<PairingEntry?>(null) }
                var trustEntryLoaded by remember { mutableStateOf(false) }
                val detailsFocus = remember { FocusRequester() }
                // Story 1.7: process-scoped target plus the socket-scoped
                // status-surface destination. No screen owns the target and no
                // last-used value is persisted; a relaunch shows "Paired".
                // Story 1.9: the shell auto-selects below when exactly one
                // stored record exists, so the target survives leaving the
                // status surface within this process.
                val targetSession = remember { TargetSessionHolder() }
                val target by targetSession.session.collectAsState()
                // Story 1.9: one-TV switcher sheet state plus the focus owner
                // the sheet returns to on every dismiss path.
                var switcherOpen by rememberSaveable { mutableStateOf(false) }
                val switcherFocus = remember { FocusRequester() }
                // Story 1.10: UI-scoped remote destination. Durable remotes
                // hold a TvId hex; session-only remotes hold an IP with
                // in-memory trust. Exactly one is set while the remote is
                // open; both null means the remote is closed. The paired
                // summary never auto-connects: opening the remote starts the
                // first connection.
                var remoteTvIdHex by remember { mutableStateOf<String?>(null) }
                var remoteSessionIp by remember { mutableStateOf<String?>(null) }
                var connEntry by remember { mutableStateOf<PairingEntry?>(null) }
                var connLoaded by remember { mutableStateOf(false) }
                var connRunJob by remember { mutableStateOf<Job?>(null) }
                var rePairing by remember { mutableStateOf(false) }
                var rePairDenied by remember { mutableStateOf(false) }
                var rePairTimedOut by remember { mutableStateOf(false) }
                var rePairFailed by remember { mutableStateOf<String?>(null) }
                var rePairCandidate by remember { mutableStateOf<TvCandidate?>(null) }
                var rePairTargetHex by rememberSaveable { mutableStateOf<String?>(null) }

                val discovery = remember {
                    TvGateways.discovery(
                        gateCheck = { gate.checkBeforeAttempt() },
                        lockRunner = { block -> gate.underHeldScope(block) },
                    )
                }
                val pairing = remember {
                    TvGateways.pairing(gateCheck = { gate.checkBeforeAttempt() })
                }
                val tvStore = remember { RoomTvStore.get(applicationContext) }
                val pairingStore = remember { KeystorePairingStore(applicationContext) }
                // Story 1.8: the single capability display owner plus the
                // debug overlay. Release builds serve overlay nothing, so the
                // card reads the asset only.
                val labelTracker = remember { CapabilityLabelTracker(scope, BuildConfig.DEBUG) }
                val overlayStore = remember { tvStore.overlayStore(BuildConfig.DEBUG) }
                var matrixAsset by remember { mutableStateOf(MatrixAsset.empty()) }
                val overlayRows by overlayStore.observe().collectAsState(initial = emptyList())
                val connectionSession = remember {
                    TvGateways.connection(gateCheck = { gate.checkBeforeAttempt() })
                }
                // One health owner per open remote; recreated per session key
                // so a stale socket never leaks into the next remote. One
                // session per TvId: the key is the durable hex or the
                // session-only IP, never both.
                val remoteKey = remember(remoteTvIdHex, remoteSessionIp) {
                    remoteTvIdHex ?: remoteSessionIp?.let { RemoteRules.sessionOnlyKey(it) }
                }
                val connectionHealth = remember(remoteKey) { ConnectionHealth() }
                val connectionController = remember(remoteKey) {
                    ConnectionController(
                        session = connectionSession,
                        health = connectionHealth,
                        schedule = ConnectionBackoff.jittered {
                            kotlin.random.Random.nextDouble()
                        },
                        freshIdentityOf = { ip ->
                            try {
                                when (val read = discovery.readManual(ip)) {
                                    is DiscoveryResult.Found -> read.targets.firstOrNull()?.tvId
                                    else -> null
                                }
                            } catch (e: Exception) {
                                null
                            }
                        },
                        rescanSameTv = {
                            val wanted = remoteTvIdHex
                            if (wanted == null) {
                                null
                            } else {
                                try {
                                    when (val scanned = discovery.scan()) {
                                        is DiscoveryResult.Found -> scanned.targets
                                            .firstOrNull { it.tvId?.hex == wanted }?.ip
                                        else -> null
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        },
                    )
                }
                val connSnapshot by connectionController.snapshot.collectAsState()

                // Story 1.11: one socket-scoped command tracker per open
                // remote. Same session key as the connection, created and
                // cancelled with it; the tracker is the sole command-outcome
                // writer. Recomposition never replays: taps are the only
                // submit source, and rotation rebuilds the tracker at Idle.
                val trackerScope = remember(remoteKey) {
                    CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
                }
                val commandTracker = remember(remoteKey) {
                    TvGateways.commandControl(
                        scope = trackerScope,
                        send = { text -> connectionSession.sendFrame(text) },
                    )
                }
                val commandOutcome by commandTracker.outcome.collectAsState()
                DisposableEffect(remoteKey) {
                    onDispose {
                        try {
                            connectionSession.setMessageListener(null)
                        } catch (e: Exception) {
                            RedactedLog.debug(TAG, "Message listener not detached")
                        }
                        commandTracker.close()
                        trackerScope.cancel()
                    }
                }
                // Socket truth clears command truth: a terminal banner, an
                // Offline socket, or denied access clears the tracker to Idle
                // with no queue, no replay, and no retry.
                LaunchedEffect(connSnapshot.banner, connSnapshot.state, snapshot.access) {
                    if (RemoteRules.shouldClearCommands(
                            connSnapshot.state,
                            connSnapshot.banner,
                            snapshot.access == LanAccess.Denied,
                        )
                    ) {
                        commandTracker.close()
                    }
                }

                LaunchedEffect(Unit) {
                    records = withContext(Dispatchers.IO) { tvStore.all() }
                }

                // Story 1.9: cold-start auto-select. Exactly one stored
                // record selects its TvId; zero records clear to NoTarget, so
                // process death never restores a stale target and nothing is
                // ever guessed. Applied only through select/clear; there is no
                // persisted last-used value.
                LaunchedEffect(records) {
                    when (val next = TargetSwitchRules.projectTarget(records.map { it.tvId }, targetSession.session.value)) {
                        is TargetSession.Selected -> targetSession.select(TvId(next.tvIdHex))
                        TargetSession.NoTarget -> targetSession.clear()
                        null -> Unit
                    }
                }

                // Focus returns to the switcher button after an actual sheet
                // dismiss, never on first composition.
                var switcherWasOpen by remember { mutableStateOf(false) }
                LaunchedEffect(switcherOpen) {
                    if (switcherOpen) {
                        switcherWasOpen = true
                    } else if (switcherWasOpen) {
                        switcherWasOpen = false
                        try {
                            switcherFocus.requestFocus()
                        } catch (e: Exception) {
                            RedactedLog.debug(TAG, "Switcher focus not restored")
                        }
                    }
                }

                // Story 1.8: capability inputs. The asset loads once per
                // paired TV, the subject refreshes from the latest record
                // (a firmware change rematches the matrix), and overlay
                // rows flow from the debug store. The tracker recomputes.
                LaunchedEffect(pairedTvIdHex) {
                    val hex = pairedTvIdHex ?: return@LaunchedEffect
                    val asset = withContext(Dispatchers.IO) {
                        MatrixAsset.load(applicationContext)
                    }
                    matrixAsset = asset
                    labelTracker.setAsset(asset)
                    val record = withContext(Dispatchers.IO) { tvStore.byId(TvId(hex)) }
                    if (record != null) {
                        labelTracker.setSubject(
                            CapabilityLabelTracker.subjectOf(
                                TvId(hex),
                                record.model.ifBlank { record.modelName },
                                record.firmware,
                                record.networkType,
                            ),
                        )
                    }
                }
                LaunchedEffect(records) {
                    val hex = pairedTvIdHex ?: return@LaunchedEffect
                    val record = records.firstOrNull { it.tvId == hex } ?: return@LaunchedEffect
                    labelTracker.setSubject(
                        CapabilityLabelTracker.subjectOf(
                            TvId(hex),
                            record.model.ifBlank { record.modelName },
                            record.firmware,
                            record.networkType,
                        ),
                    )
                }
                LaunchedEffect(overlayRows, matrixAsset) {
                    labelTracker.setOverlays(overlayRows.mapNotNull { it.toCapabilityRow() })
                }

                // Durable-only sheet data: stored Room record plus the
                // Keystore entry, never a socket (works TV-off).
                val trustRecord = remember(records, trustTvIdHex) {
                    trustTvIdHex?.let { hex -> records.firstOrNull { it.tvId == hex } }
                }
                LaunchedEffect(trustTvIdHex) {
                    if (trustTvIdHex == null) {
                        trustEntry = null
                        trustEntryLoaded = false
                        trustConfirming = false
                        trustForgetting = false
                    } else {
                        val hex = trustTvIdHex ?: return@LaunchedEffect
                        trustForgetFailed = false
                        trustEntryLoaded = false
                        trustEntry = withContext(Dispatchers.IO) {
                            pairingStore.load(TvId(hex))
                        }
                        // Gate the sheet on the finished load so a missing or
                        // unreadable pin ("Fingerprint unavailable") never
                        // flashes while the Keystore read is in flight.
                        trustEntryLoaded = true
                    }
                }
                // Focus returns to "TV details" when the sheet closes.
                LaunchedEffect(trustTvIdHex) {
                    if (trustTvIdHex == null) {
                        try {
                            detailsFocus.requestFocus()
                        } catch (e: Exception) {
                            RedactedLog.debug(TAG, "Details focus not restored")
                        }
                    }
                }

                fun runForget(tvId: TvId) {
                    if (trustForgetting) return
                    trustForgetting = true
                    trustForgetFailed = false
                    scope.launch(Dispatchers.IO) {
                        // Silent by construction: no socket work, no command
                        // sent. Clear Keystore material first, then the Room
                        // row; a missing alias counts as already deleted.
                        pairingStore.clear(tvId)
                        val roomGone = tvStore.forget(tvId)
                        val afterClear = pairingStore.load(tvId)
                        val decision = TrustLogic.decideForget(roomGone, afterClear)
                        withContext(Dispatchers.Main) {
                            trustForgetting = false
                            when (decision) {
                                TrustLogic.ForgetDecision.Succeeded -> {
                                    records = tvStore.all()
                                    labelTracker.removeSubject(tvId.hex)
                                    pairedName = ""
                                    pairedModel = ""
                                    pairedSessionOnly = false
                                    pairedTvIdHex = null
                                    trustConfirming = false
                                    trustForgetFailed = false
                                    trustTvIdHex = null
                                    // Forget-while-open: the switcher sheet
                                    // closes and the target clears to NoTarget
                                    // with no stale row left behind.
                                    switcherOpen = false
                                    when (val after = TargetSwitchRules.reduce(targetSession.session.value, TargetSwitchRules.TargetEvent.ForgetSucceeded)) {
                                        is TargetSession.Selected -> targetSession.select(TvId(after.tvIdHex))
                                        TargetSession.NoTarget -> targetSession.clear()
                                    }
                                    // Forget-while-open: the remote session
                                    // closes without a command and returns to
                                    // the no-TV setup with no stale sheet.
                                    connRunJob?.cancel()
                                    connRunJob = null
                                    connectionController.stop()
                                    connectionSession.setMessageListener(null)
                                    commandTracker.close()
                                    connEntry = null
                                    connLoaded = false
                                    rePairing = false
                                    rePairDenied = false
                                    rePairTimedOut = false
                                    rePairFailed = null
                                    rePairCandidate = null
                                    rePairTargetHex = null
                                    remoteTvIdHex = null
                                    remoteSessionIp = null
                                    route = SetupRoute.Shell
                                    RedactedLog.debug(TAG, "Trust forgotten")
                                }
                                TrustLogic.ForgetDecision.Failed -> {
                                    trustForgetFailed = true
                                    RedactedLog.warn(TAG, "Trust not forgotten")
                                }
                            }
                        }
                    }
                }

                fun dismissKey(candidate: TvCandidate): String =
                    SetupLogic.cardKey(candidate)

                fun visibleTargets(): List<TvCandidate> =
                    targets.filter { dismissKey(it) !in dismissed }

                fun startScan() {
                    if (gate.checkBeforeAttempt() == LanAccess.Denied) return
                    scanJob?.cancel()
                    scanning = true
                    scanDone = false
                    diagnostic = null
                    scanJob = scope.launch(Dispatchers.IO) {
                        val result = discovery.scan()
                        withContext(Dispatchers.Main) {
                            scanning = false
                            scanDone = true
                            when (val ui = SetupLogic.mapScan(result)) {
                                is SetupLogic.ScanUi.Found -> {
                                    targets = SetupLogic.dedupeByTvId(targets + ui.targets)
                                        .fold(emptyList()) { acc, next -> SetupLogic.mergeAsFound(acc, next) }
                                    diagnostic = null
                                }
                                SetupLogic.ScanUi.Empty -> diagnostic = null
                                is SetupLogic.ScanUi.LanDenied -> diagnostic = ui.reason
                                is SetupLogic.ScanUi.Error -> diagnostic = ui.reason
                                else -> diagnostic = null
                            }
                        }
                    }
                }

                fun cancelPair() {
                    pairJob?.cancel()
                    pairJob = null
                    pairingWaiting = false
                    RedactedLog.debug(TAG, "Pair cancelled")
                }

                fun openAddTv() {
                    dismissed = emptySet()
                    manualKeys = emptySet()
                    targets = emptyList()
                    diagnostic = null
                    manualError = null
                    scanDone = false
                    route = SetupRoute.AddTv
                    startScan()
                }

                fun runPair(candidate: TvCandidate) {
                    if (pairingWaiting) return
                    pairJob?.cancel()
                    pairingWaiting = true
                    pairingDenied = false
                    pairingTimedOut = false
                    pairingTokenMissing = false
                    pairingFailed = null
                    pairJob = scope.launch(Dispatchers.IO) {
                        val outcome = pairing.pair(candidate)
                        withContext(Dispatchers.Main) {
                            pairingWaiting = false
                            pairJob = null
                            when (outcome) {
                                is PairOutcome.Paired -> {
                                    if (!SetupLogic.persists(outcome, candidate.tokenAuthSupport)) {
                                        pairingTokenMissing = true
                                        RedactedLog.debug(TAG, "Pair outcome not persistable, nothing stored")
                                    } else if (SetupLogic.isSessionOnly(candidate)) {
                                        val key = candidate.ip
                                        sessionTrust[key] = PairingEntry(outcome.token, outcome.pin)
                                        pairedName = candidate.displayName
                                        pairedModel = candidate.modelLabel
                                        pairedSessionOnly = true
                                        pairedTvIdHex = null
                                        // Session-only targets carry no TvId
                                        // and never become a target.
                                        targetSession.clear()
                                        route = SetupRoute.Paired
                                    } else {
                                        val tvId = candidate.tvId!!
                                        pairingStore.save(tvId, outcome.token, outcome.pin)
                                        val record = tvStore.persistPaired(
                                            candidate,
                                            snapshot.networkType,
                                            interfaceLabel(snapshot.networkType),
                                        )
                                        if (record == null) {
                                            pairingFailed = getString(R.string.pairing_not_stored)
                                        } else {
                                            records = withContext(Dispatchers.IO) { tvStore.all() }
                                            pairedName = candidate.displayName
                                            pairedModel = candidate.modelLabel
                                            pairedSessionOnly = false
                                            pairedTvIdHex = record.tvId
                                            // The just-paired TV selects; this
                                            // is the known TvId, never a guess.
                                            targetSession.select(tvId)
                                            route = SetupRoute.Paired
                                        }
                                    }
                                }
                                PairOutcome.Denied -> pairingDenied = true
                                PairOutcome.TimedOut -> pairingTimedOut = true
                                PairOutcome.TokenMissing -> pairingTokenMissing = true
                                is PairOutcome.Failed -> pairingFailed = outcome.reason
                            }
                        }
                    }
                }

                // Story 1.10: UI-scoped remote session. Starts exactly one
                // connection from the stored record, token, and pin (or the
                // in-memory session-only entry); leaving, background, target
                // clear, forget, or target change closes the socket and stops
                // backoff. No auto-connect on launch or while closed; the
                // paired summary stays "Paired". Leaving keeps the
                // process-scoped target (reduce with SurfaceLeft is
                // identity); only Forget clears it.
                fun closeRemote() {
                    connRunJob?.cancel()
                    connRunJob = null
                    connectionController.stop()
                    connectionSession.setMessageListener(null)
                    commandTracker.close()
                    connEntry = null
                    connLoaded = false
                    rePairing = false
                    rePairDenied = false
                    rePairTimedOut = false
                    rePairFailed = null
                    rePairCandidate = null
                    rePairTargetHex = null
                    remoteTvIdHex = null
                    remoteSessionIp = null
                    route = SetupRoute.Shell
                }

                fun pauseRemoteSocket() {
                    connRunJob?.cancel()
                    connRunJob = null
                    connectionController.stop()
                    connectionSession.setMessageListener(null)
                    commandTracker.close()
                }

                fun startConnectionRun(
                    tvIdHex: String,
                    ip: String,
                    pin: String,
                    token: String?,
                    tokenAuth: Boolean,
                ) {
                    connRunJob?.cancel()
                    val running = scope.launch(Dispatchers.IO) {
                        connectionController.run(TvId(tvIdHex), ip, pin, token, tokenAuth)
                    }
                    connRunJob = running
                    connectionController.attachJob(running)
                }

                fun startSessionOnlyRun(
                    ip: String,
                    pin: String,
                    token: String?,
                    tokenAuth: Boolean,
                ) {
                    connRunJob?.cancel()
                    val running = scope.launch(Dispatchers.IO) {
                        val id = RemoteRules.resolveRetryTarget(null, ip, false, pin.isNotBlank())
                            ?: return@launch
                        connectionController.run(id, ip, pin, token, tokenAuth)
                    }
                    connRunJob = running
                    connectionController.attachJob(running)
                }

                fun openRemoteDurable(hex: String) {
                    val opened = RemoteRules.resolveDurableOpen(
                        targetSession.session.value,
                        records.map { it.tvId },
                        hex,
                    ) ?: return
                    rePairDenied = false
                    rePairTimedOut = false
                    rePairFailed = null
                    rePairCandidate = null
                    rePairTargetHex = null
                    remoteSessionIp = null
                    remoteTvIdHex = opened
                    route = SetupRoute.Remote(opened, null)
                }

                fun openRemoteSessionOnly(ip: String) {
                    val opened = RemoteRules.resolveSessionOnlyOpen(
                        sessionTrust.isNotEmpty() && records.isEmpty(),
                        ip,
                        sessionTrust[ip]?.pin,
                    ) ?: return
                    rePairDenied = false
                    rePairTimedOut = false
                    rePairFailed = null
                    rePairCandidate = null
                    rePairTargetHex = null
                    remoteTvIdHex = null
                    remoteSessionIp = opened
                    route = SetupRoute.Remote(null, opened)
                }

                fun launchRemoteSession() {
                    connRunJob?.cancel()
                    // Inbound channel text reaches the tracker, which parses
                    // it in `:tv` and decides whether it is evidence. The
                    // listener carries opaque strings only: no frame, key, or
                    // socket type crosses into `:app`.
                    connectionSession.setMessageListener { text ->
                        commandTracker.onRemoteMessage(text)
                    }
                    val hex = remoteTvIdHex
                    val ip = remoteSessionIp
                    when {
                        hex != null -> {
                            targetSession.select(TvId(hex))
                            connRunJob = scope.launch(Dispatchers.IO) {
                                val entry = pairingStore.load(TvId(hex))
                                withContext(Dispatchers.Main) {
                                    connEntry = entry
                                    connLoaded = true
                                    val record = records.firstOrNull { it.tvId == hex }
                                    if (record != null && entry != null &&
                                        RemoteRules.shouldStartSocket(snapshot.access, record, entry)
                                    ) {
                                        startConnectionRun(
                                            hex,
                                            record.lastIp,
                                            entry.pin,
                                            entry.token,
                                            entry.token != null,
                                        )
                                    } else if (record != null && snapshot.access != LanAccess.Denied) {
                                        // Stored record but no trust entry: terminal
                                        // pairing-required, no socket work.
                                        connectionController.showPairingRequired()
                                    }
                                }
                            }
                        }
                        ip != null -> {
                            val entry = sessionTrust[ip]
                            connRunJob = scope.launch(Dispatchers.Main) {
                                connEntry = entry
                                connLoaded = true
                                if (snapshot.access == LanAccess.Denied) {
                                    connRunJob?.cancel()
                                    connRunJob = null
                                    return@launch
                                }
                                if (entry != null) {
                                    startSessionOnlyRun(
                                        ip,
                                        entry.pin,
                                        entry.token,
                                        entry.token != null,
                                    )
                                }
                            }
                        }
                    }
                }

                // Re-pair reuses the Story 1.5 Allow/Deny flow token-less with
                // the stored name/identity and the always-visible no-remote
                // path. Deny, timeout, or failure keeps the old record and
                // trust with the banner staying; old trust dies only after
                // the new commit verifies.
                fun runRePairPair(candidate: TvCandidate, storedHex: String) {
                    if (rePairing) return
                    rePairing = true
                    rePairDenied = false
                    rePairTimedOut = false
                    rePairFailed = null
                    scope.launch(Dispatchers.IO) {
                        val storedId = TvId(storedHex)
                        val outcome = pairing.pair(candidate)
                        when (outcome) {
                            is PairOutcome.Paired -> {
                                val ok = RePairCommit.commit(
                                    storedTvId = storedId,
                                    freshCandidate = candidate,
                                    freshTvId = candidate.tvId,
                                    token = outcome.token,
                                    pin = outcome.pin,
                                    pairingStore = pairingStore,
                                    tvStore = tvStore,
                                    networkTypeWire = snapshot.networkType.wire(),
                                    networkInterface = interfaceLabel(snapshot.networkType),
                                )
                                withContext(Dispatchers.Main) {
                                    rePairing = false
                                    if (!ok) {
                                        rePairFailed = getString(R.string.pairing_not_stored)
                                    } else {
                                        rePairCandidate = null
                                        rePairTargetHex = null
                                        connEntry = PairingEntry(outcome.token, outcome.pin)
                                        connLoaded = true
                                        startConnectionRun(
                                            storedId.hex,
                                            candidate.ip,
                                            outcome.pin,
                                            outcome.token,
                                            candidate.tokenAuthSupport,
                                        )
                                    }
                                    records = tvStore.all()
                                }
                            }
                            PairOutcome.Denied -> withContext(Dispatchers.Main) {
                                rePairing = false
                                rePairDenied = true
                            }
                            PairOutcome.TimedOut -> withContext(Dispatchers.Main) {
                                rePairing = false
                                rePairTimedOut = true
                            }
                            else -> withContext(Dispatchers.Main) {
                                rePairing = false
                                rePairFailed = getString(R.string.pairing_not_stored)
                            }
                        }
                    }
                }

                fun runRePair(record: com.unirem.tv.TvRecord) {
                    if (rePairing || rePairCandidate != null) return
                    connRunJob?.cancel()
                    connRunJob = null
                    rePairDenied = false
                    rePairTimedOut = false
                    rePairFailed = null
                    scope.launch(Dispatchers.IO) {
                        val storedId = TvId(record.tvId)
                        val read = try {
                            discovery.readManual(record.lastIp)
                        } catch (e: Exception) {
                            null
                        }
                        val fresh = (read as? DiscoveryResult.Found)?.targets?.firstOrNull()
                        // Fresh identity must equal the stored TvId or the
                        // re-pair fails closed with stored trust untouched. A
                        // null read (TV silent) moves to Offline; a mismatch
                        // stays silent with the banner kept.
                        if (fresh == null) {
                            withContext(Dispatchers.Main) {
                                connectionController.notNow()
                            }
                            return@launch
                        }
                        if (fresh.tvId?.hex != storedId.hex) {
                            return@launch
                        }
                        withContext(Dispatchers.Main) {
                            rePairCandidate = fresh
                            rePairTargetHex = storedId.hex
                            runRePairPair(fresh, storedId.hex)
                        }
                    }
                }

                fun cancelRePair() {
                    rePairing = false
                    rePairCandidate = null
                    rePairTargetHex = null
                    rePairDenied = false
                    rePairTimedOut = false
                    rePairFailed = null
                }

                // Open the remote: select the process-scoped target (durable
                // only), load the stored trust, then start the single
                // connection. The Keystore read gates the first attempt so no
                // socket opens without the stored pin. Session-only remotes
                // use the in-memory entry and never touch Room or Keystore.
                // Recomposition never opens a second channel: this effect is
                // keyed on the single session key.
                LaunchedEffect(remoteKey) {
                    if (remoteKey == null) {
                        connEntry = null
                        connLoaded = false
                    } else {
                        launchRemoteSession()
                    }
                }
                // Network change is a socket close, never stale-socket reuse.
                LaunchedEffect(snapshot.networkType) {
                    if (remoteKey != null && connLoaded) {
                        connectionController.onNetworkChanged()
                        commandTracker.close()
                    }
                }
                // Target change or clear while the remote is open closes the
                // socket with no cross-target work: the remote never guesses.
                LaunchedEffect(target) {
                    if (RemoteRules.shouldCloseOnTargetChange(
                            remoteTvIdHex,
                            target,
                            records.map { it.tvId },
                            route is SetupRoute.Remote,
                        )
                    ) {
                        pauseRemoteSocket()
                        connEntry = null
                        connLoaded = false
                        remoteTvIdHex = null
                        remoteSessionIp = null
                        route = SetupRoute.Shell
                    }
                }

                when (val current = route) {
                    SetupRoute.Shell -> {
                        // Story 1.9: the shell never reads Room as its own
                        // target. The TargetSession target resolves to at most
                        // one stored record; anything else is NoTarget.
                        val targetHex = remember(target, records) {
                            TargetSwitchRules.resolveTargetHex(target, records.map { it.tvId })
                        }
                        val targetRecord = remember(records, targetHex) {
                            targetHex?.let { hex -> records.firstOrNull { it.tvId == hex } }
                        }
                        val summary = targetRecord?.let {
                            if (it.model.isBlank()) it.name else "${it.name} · ${it.model}"
                        }?.ifBlank { getString(R.string.setup_not_reported) }
                        val sessionOnlyActive = sessionTrust.isNotEmpty() && records.isEmpty()
                        // Labels subscribe only while a target is selected;
                        // NoTarget holds no empty-key flow.
                        val switcherLabels by if (targetHex != null) {
                            labelTracker.labelsFor(targetHex).collectAsState()
                        } else {
                            remember { mutableStateOf(emptyMap<CapabilityKey, CapabilityLabel>()) }
                        }
                        val switcherPlate =
                            if (targetHex == null || targetRecord == null) {
                                null
                            } else {
                                CapabilityCopy.cardLine(
                                    switcherLabels[CapabilityLabelTracker.CARD_KEY],
                                    getString(R.string.capability_full_control),
                                    getString(R.string.capability_best_effort),
                                )
                            }
                        ShellScreen(
                            versionName = BuildConfig.VERSION_NAME,
                            snapshot = snapshot,
                            explainerVisible = explainerVisible,
                            onGrant = { explainerVisible = true },
                            onExplainerContinue = {
                                gate.onExplainerContinue(
                                    firePrompt = {
                                        permissionLauncher.launch(LanAccessGate.LOCAL_NETWORK_PERMISSION)
                                    },
                                    onProceed = {},
                                )
                                explainerVisible = false
                            },
                            onExplainerDismiss = { explainerVisible = false },
                            onOpenSettings = {
                                explainerVisible = false
                                openAppSettings()
                            },
                            hasPairedTv = targetRecord != null,
                            tvSummary = summary,
                            onAddTv = { openAddTv() },
                            onShowDetails = {
                                val hex = targetHex
                                if (hex != null && targetRecord != null) {
                                    trustConfirming = false
                                    trustForgetFailed = false
                                    trustTvIdHex = hex
                                }
                            },
                            detailsFocusRequester = detailsFocus,
                            sessionOnlyActive = sessionOnlyActive,
                            onOpenRemote = {
                                val hex = targetHex
                                if (hex != null && targetRecord != null) {
                                    openRemoteDurable(hex)
                                } else if (sessionOnlyActive) {
                                    val ip = sessionTrust.keys.firstOrNull()
                                    if (ip != null) openRemoteSessionOnly(ip)
                                }
                            },
                            switcherName = targetRecord?.name?.ifBlank {
                                getString(R.string.setup_not_reported)
                            },
                            onOpenSwitcher = { switcherOpen = true },
                            switcherFocusRequester = switcherFocus,
                            sessionOnlyName = if (sessionOnlyActive) {
                                pairedName.ifBlank { null }
                            } else {
                                null
                            },
                        )
                        // Durable-only trust sheet keyed by TvId. Session-only
                        // targets get no sheet. Gated on the finished
                        // Keystore load so "Fingerprint unavailable" never
                        // flashes mid-read. Never on top of the switcher sheet:
                        // one sheet at a time.
                        if (trustRecord != null && !switcherOpen && TrustLogic.sheetVisible(sessionOnlyActive, trustTvIdHex, true, trustEntryLoaded)) {
                            TrustSheet(
                                record = trustRecord,
                                maskedMac = IdentityRules.maskMac(trustRecord.wifiMac),
                                fingerprint = FingerprintFormat.format(trustEntry?.pin),
                                confirming = trustConfirming,
                                forgetting = trustForgetting,
                                forgetFailed = trustForgetFailed,
                                onRequestForget = {
                                    trustForgetFailed = false
                                    trustConfirming = true
                                },
                                onConfirmForget = {
                                    val hex = trustTvIdHex ?: return@TrustSheet
                                    runForget(TvId(hex))
                                },
                                onCancelConfirm = {
                                    // Cancel deletes nothing and keeps the sheet.
                                    trustConfirming = false
                                },
                                onRetryForget = {
                                    val hex = trustTvIdHex ?: return@TrustSheet
                                    runForget(TvId(hex))
                                },
                                onCloseError = { trustForgetFailed = false },
                                onDismiss = {
                                    if (trustConfirming) {
                                        trustConfirming = false
                                    } else {
                                        trustTvIdHex = null
                                    }
                                },
                            )
                        }
                        // Story 1.9: the one-TV switcher sheet. A one-item
                        // list over the TargetSession target; opens nothing
                        // and changes nothing on open. Selecting the current
                        // row only closes; details hands the TvId to the trust
                        // sheet with the target unchanged; forget-while-open
                        // closes and clears via runForget.
                        if (TargetSwitchRules.sheetVisible(target, switcherOpen, sessionOnlyActive, trustTvIdHex != null)) {
                            val sheetHex = (target as TargetSession.Selected).tvIdHex
                            TvSwitcherSheet(
                                tvName = targetRecord?.name?.ifBlank {
                                    getString(R.string.setup_not_reported)
                                } ?: getString(R.string.switcher_unavailable),
                                tvModel = listOfNotNull(targetRecord?.model, targetRecord?.modelName)
                                    .firstOrNull { it.isNotBlank() } ?: "",
                                capabilityPlate = switcherPlate,
                                stateText = getString(R.string.switcher_not_connected),
                                unreadable = TargetSwitchRules.rowUnreadable(target, targetRecord != null),
                                onSelectCurrent = {
                                    // Current tap is a no-op: close only, no
                                    // reconnect, reset, or health change.
                                    switcherOpen = false
                                },
                                onShowDetails = {
                                    switcherOpen = false
                                    trustConfirming = false
                                    trustForgetFailed = false
                                    trustTvIdHex = sheetHex
                                },
                                onPairAgain = {
                                    switcherOpen = false
                                    openAddTv()
                                },
                                onDismiss = { switcherOpen = false },
                            )
                        }
                    }
                    SetupRoute.AddTv -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Column(
                                modifier = Modifier
                                    .widthIn(max = GraphiteMintSpacing.MaxColumnWidth)
                                    .windowInsetsPadding(WindowInsets.safeDrawing)
                                    .padding(horizontal = GraphiteMintSpacing.ScreenX)
                                    .padding(top = GraphiteMintSpacing.X10),
                            ) {
                                SetupTopBar(
                                    onBack = {
                                        scanJob?.cancel()
                                        scanJob = null
                                        scanning = false
                                        route = SetupRoute.Shell
                                    },
                                )
                                Spacer(modifier = Modifier.height(GraphiteMintSpacing.X4))
                                AddTvScreen(
                                    scanning = scanning,
                                    scanDone = scanDone,
                                    targets = visibleTargets(),
                                    diagnostic = diagnostic,
                                    manualIp = manualIp,
                                    manualError = manualError,
                                    gateBlocked = snapshot.access == LanAccess.Denied,
                                    onScanAgain = { startScan() },
                                    manualKeys = manualKeys,
                                    onManualIpChange = {
                                        manualIp = it
                                        manualError = null
                                    },
                                    onManualRead = {
                                        val error = SetupLogic.manualIpError(manualIp)
                                        manualError = error
                                        if (error != null || gate.checkBeforeAttempt() == LanAccess.Denied) return@AddTvScreen
                                        scope.launch(Dispatchers.IO) {
                                            val result = discovery.readManual(manualIp.trim())
                                            withContext(Dispatchers.Main) {
                                                when (result) {
                                                    is DiscoveryResult.Found -> {
                                                        val found = result.targets.first()
                                                        if (dismissKey(found) !in dismissed) {
                                                            targets = SetupLogic.mergeAsFound(targets, found)
                                                            manualKeys = manualKeys + dismissKey(found)
                                                        }
                                                        diagnostic = null
                                                    }
                                                    DiscoveryResult.NoDiscovery -> {
                                                        diagnostic = "No TV answered at that address."
                                                    }
                                                    is DiscoveryResult.LanDenied -> diagnostic = result.reason
                                                    is DiscoveryResult.Failed -> diagnostic = result.reason
                                                }
                                            }
                                        }
                                    },
                                    onChoose = { candidate ->
                                        if (pairingWaiting) return@AddTvScreen
                                        pairingDenied = false
                                        pairingTimedOut = false
                                        pairingTokenMissing = false
                                        pairingFailed = null
                                        route = SetupRoute.Pairing(candidate)
                                        runPair(candidate)
                                    },
                                    onDismissCard = { candidate ->
                                        dismissed = dismissed + dismissKey(candidate)
                                    },
                                )
                            }
                        }
                    }
                    is SetupRoute.Pairing -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Column(
                                modifier = Modifier
                                    .widthIn(max = GraphiteMintSpacing.MaxColumnWidth)
                                    .windowInsetsPadding(WindowInsets.safeDrawing)
                                    .padding(horizontal = GraphiteMintSpacing.ScreenX)
                                    .padding(top = GraphiteMintSpacing.X10),
                            ) {
                                SetupTopBar(
                                    onBack = {
                                        cancelPair()
                                        pairingDenied = false
                                        pairingTimedOut = false
                                        pairingTokenMissing = false
                                        pairingFailed = null
                                        route = SetupRoute.AddTv
                                    },
                                )
                                Spacer(modifier = Modifier.height(GraphiteMintSpacing.X4))
                                PairingScreen(
                                    candidate = current.candidate,
                                    waiting = pairingWaiting,
                                    denied = pairingDenied,
                                    timedOut = pairingTimedOut,
                                    tokenMissing = pairingTokenMissing,
                                    onRetry = { runPair(current.candidate) },
                                    onNotNow = {
                                        cancelPair()
                                        route = SetupRoute.AddTv
                                    },
                                    failureReason = pairingFailed,
                                )
                            }
                        }
                    }
                    SetupRoute.Paired -> {
                        val pairedLabels by labelTracker.labelsFor(pairedTvIdHex ?: "")
                            .collectAsState()
                        val capabilityPlate = if (pairedSessionOnly || pairedTvIdHex == null) {
                            null
                        } else {
                            CapabilityCopy.cardLine(
                                pairedLabels[CapabilityLabelTracker.CARD_KEY],
                                getString(R.string.capability_full_control),
                                getString(R.string.capability_best_effort),
                            )
                        }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Column(
                                modifier = Modifier
                                    .widthIn(max = GraphiteMintSpacing.MaxColumnWidth)
                                    .windowInsetsPadding(WindowInsets.safeDrawing)
                                    .padding(horizontal = GraphiteMintSpacing.ScreenX)
                                    .padding(top = GraphiteMintSpacing.X10),
                            ) {
                                PairedScreen(
                                    name = pairedName,
                                    model = pairedModel,
                                    onDone = {
                                        scope.launch(Dispatchers.IO) {
                                            val fresh = tvStore.all()
                                            withContext(Dispatchers.Main) {
                                                records = fresh
                                                route = SetupRoute.Shell
                                            }
                                        }
                                    },
                                    sessionOnly = pairedSessionOnly,
                                    capabilityLine = capabilityPlate,
                                )
                                // Developer-only entry to the matrix harness.
                                // Release builds never offer this route.
                                val harnessHex = pairedTvIdHex
                                if (BuildConfig.DEBUG && harnessHex != null && !pairedSessionOnly) {
                                    Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
                                    GateQuietAction(
                                        label = getString(R.string.matrix_harness_open),
                                        description = getString(R.string.matrix_harness_open_cd),
                                        onClick = { route = SetupRoute.MatrixHarness(harnessHex) },
                                    )
                                }
                            }
                        }
                    }
                    is SetupRoute.MatrixHarness -> {
                        // Debug-only route: the harness records observed rows
                        // and never opens a socket. Release builds never
                        // navigate here and render nothing if they do.
                        if (BuildConfig.DEBUG) {
                            val hex = current.tvIdHex
                            val record = records.firstOrNull { it.tvId == hex }
                            val harnessLabels by labelTracker.labelsFor(hex).collectAsState()
                            val harnessModel = record?.model.ifNullOrBlank(record?.modelName)
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .widthIn(max = GraphiteMintSpacing.MaxColumnWidth)
                                        .windowInsetsPadding(WindowInsets.safeDrawing)
                                        .padding(horizontal = GraphiteMintSpacing.ScreenX)
                                        .padding(top = GraphiteMintSpacing.X10),
                                ) {
                                    MatrixHarnessScreen(
                                        model = harnessModel,
                                        firmware = record?.firmware ?: "",
                                        networkLabel = networkName(record?.networkType),
                                        networkWire = record?.networkType ?: "unknown",
                                        matrixVersion = matrixAsset.matrixVersion,
                                        assetRows = matrixAsset.rows,
                                        overlayRows = overlayRows.filter { row ->
                                            row.model == harnessModel &&
                                                row.firmware == (record?.firmware ?: "")
                                        },
                                        resolved = harnessLabels,
                                        onSave = { key, result, note ->
                                            scope.launch(Dispatchers.IO) {
                                                val fresh = tvStore.byId(TvId(hex))
                                                    ?: return@launch
                                                val exactModel = fresh.model
                                                    .ifBlank { fresh.modelName }
                                                overlayStore.save(
                                                    MatrixOverlayRow(
                                                        overlayKey = MatrixOverlayStore.overlayKeyOf(
                                                            exactModel,
                                                            fresh.firmware,
                                                            fresh.networkType,
                                                            key.capability.name,
                                                            key.scope.name,
                                                        ),
                                                        model = exactModel,
                                                        firmware = fresh.firmware,
                                                        networkType = fresh.networkType,
                                                        capability = key.capability.name,
                                                        scope = key.scope.name,
                                                        result = result.name,
                                                        note = note,
                                                        matrixVersion = matrixAsset.matrixVersion,
                                                        observedAtEpochSeconds = Instant.now().epochSecond,
                                                    ),
                                                )
                                            }
                                        },
                                        onClear = { overlayKey ->
                                            scope.launch(Dispatchers.IO) {
                                                overlayStore.clear(overlayKey)
                                            }
                                        },
                                        onBack = { route = SetupRoute.Paired },
                                    )
                                }
                            }
                        }
                    }
                    is SetupRoute.Remote -> {
                        val durableHex = current.tvIdHex
                        val sessionIp = current.sessionIp
                        val record = durableHex?.let { hex -> records.firstOrNull { it.tvId == hex } }
                        val sessionEntry = sessionIp?.let { sessionTrust[it] }
                        val lifecycle = LocalLifecycleOwner.current.lifecycle
                        // Background closes the socket with no background
                        // reconnect; foreground restarts fresh with a
                        // revalidated pin and no cached health.
                        DisposableEffect(lifecycle, remoteKey) {
                            val observer = LifecycleEventObserver { _, event ->
                                when (event) {
                                    Lifecycle.Event.ON_STOP -> pauseRemoteSocket()
                                    Lifecycle.Event.ON_RESUME -> {
                                        if (remoteKey != null) launchRemoteSession()
                                    }
                                    else -> Unit
                                }
                            }
                            lifecycle.addObserver(observer)
                            onDispose { lifecycle.removeObserver(observer) }
                        }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Column(
                                modifier = Modifier
                                    .widthIn(max = GraphiteMintSpacing.MaxColumnWidth)
                                    .windowInsetsPadding(WindowInsets.safeDrawing)
                                    .padding(top = GraphiteMintSpacing.X2),
                            ) {
                                if (durableHex != null && record == null) {
                                    SetupTopBar(onBack = { closeRemote() })
                                    Spacer(modifier = Modifier.height(GraphiteMintSpacing.X4))
                                    ShellScreenUnavailable(
                                        onPairAgain = {
                                            pauseRemoteSocket()
                                            remoteTvIdHex = null
                                            remoteSessionIp = null
                                            openAddTv()
                                        },
                                    )
                                } else if (sessionIp != null && sessionEntry == null) {
                                    SetupTopBar(onBack = { closeRemote() })
                                    Spacer(modifier = Modifier.height(GraphiteMintSpacing.X4))
                                } else if (rePairCandidate != null && durableHex != null) {
                                    // Re-pair Allow flow, reused from Story 1.5
                                    // with the stored identity: the TV shows
                                    // its Allow prompt, the no-remote path
                                    // stays visible, no old token is sent.
                                    // Back keeps the remote banner.
                                    val candidate = rePairCandidate!!
                                    val targetHex = rePairTargetHex
                                    PairingScreen(
                                        candidate = candidate,
                                        waiting = rePairing,
                                        denied = rePairDenied,
                                        timedOut = rePairTimedOut,
                                        tokenMissing = false,
                                        onRetry = {
                                            if (targetHex != null) {
                                                runRePairPair(candidate, targetHex)
                                            }
                                        },
                                        onNotNow = { cancelRePair() },
                                        failureReason = rePairFailed,
                                    )
                                } else {
                                    val isSessionOnly = sessionIp != null
                                    val tvName = when {
                                        record != null -> record.name.ifBlank {
                                            getString(R.string.setup_not_reported)
                                        }
                                        isSessionOnly -> pairedName.ifBlank {
                                            sessionIp
                                        }
                                        else -> getString(R.string.setup_not_reported)
                                    }
                                    val lanDenied = snapshot.access == LanAccess.Denied
                                    RemoteScreen(
                                        tvName = tvName,
                                        sessionOnly = isSessionOnly,
                                        showSwitcherCaret = !isSessionOnly && record != null,
                                        switcherStateText = ConnectionUiRules.textFor(connSnapshot.state),
                                        onOpenSwitcher = { switcherOpen = true },
                                        state = connSnapshot.state,
                                        banner = connSnapshot.banner,
                                        lanDenied = lanDenied,
                                        outcome = commandOutcome,
                                        dpadEnabled = RemoteRules.dpadEnabled(
                                            connSnapshot.state,
                                            connSnapshot.banner,
                                            lanDenied,
                                        ),
                                        onDpad = { command ->
                                            if (RemoteRules.dpadEnabled(
                                                    connSnapshot.state,
                                                    connSnapshot.banner,
                                                    snapshot.access == LanAccess.Denied,
                                                )
                                            ) {
                                                commandTracker.submit(command)
                                            }
                                        },
                                        onConfirm = { commandTracker.confirmUser() },
                                        onBack = { closeRemote() },
                                        onRetry = {
                                            if (lanDenied) return@RemoteScreen
                                            if (isSessionOnly) {
                                                val retryId = RemoteRules.resolveRetryTarget(
                                                    null,
                                                    sessionIp,
                                                    false,
                                                    sessionEntry != null,
                                                )
                                                val entry = sessionEntry
                                                val ip = sessionIp
                                                if (retryId == null || entry == null || ip == null) return@RemoteScreen
                                                connRunJob?.cancel()
                                                connRunJob = scope.launch(Dispatchers.IO) {
                                                    val reached = connectionController.retry(
                                                        retryId,
                                                        ip,
                                                        entry.pin,
                                                        entry.token,
                                                        entry.token != null,
                                                    )
                                                    withContext(Dispatchers.Main) {
                                                        if (reached == null &&
                                                            connectionController.snapshot.value.banner ==
                                                            BannerKind.Reconnecting
                                                        ) {
                                                            startSessionOnlyRun(
                                                                ip,
                                                                entry.pin,
                                                                entry.token,
                                                                entry.token != null,
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                val entry = connEntry
                                                val hex = durableHex
                                                val rec = hex?.let { records.firstOrNull { it.tvId == hex } }
                                                val retryId = RemoteRules.resolveRetryTarget(
                                                    hex,
                                                    null,
                                                    rec != null,
                                                    false,
                                                )
                                                if (entry == null || rec == null || hex == null || retryId == null) {
                                                    return@RemoteScreen
                                                }
                                                val ip = rec.lastIp
                                                connRunJob?.cancel()
                                                connRunJob = scope.launch(Dispatchers.IO) {
                                                    val reached = connectionController.retry(
                                                        retryId,
                                                        ip,
                                                        entry.pin,
                                                        entry.token,
                                                        entry.token != null,
                                                    )
                                                    withContext(Dispatchers.Main) {
                                                        if (reached == null &&
                                                            connectionController.snapshot.value.banner ==
                                                            BannerKind.Reconnecting
                                                        ) {
                                                            startConnectionRun(
                                                                hex,
                                                                ip,
                                                                entry.pin,
                                                                entry.token,
                                                                entry.token != null,
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        onNotNow = {
                                            connRunJob?.cancel()
                                            connectionController.notNow()
                                        },
                                        onRePair = {
                                            val rec = record
                                            if (rec != null) runRePair(rec)
                                        },
                                        onOpenSettings = { openAppSettings() },
                                    )
                                }
                            }
                        }
                        // Sheets over the remote never close the session:
                        // details and switcher render on top with the socket
                        // untouched. Forget-while-open closes via runForget.
                        val remoteTargetHex = durableHex
                        val remoteLabels by if (remoteTargetHex != null) {
                            labelTracker.labelsFor(remoteTargetHex).collectAsState()
                        } else {
                            remember { mutableStateOf(emptyMap<CapabilityKey, CapabilityLabel>()) }
                        }
                        val remotePlate = if (remoteTargetHex == null || record == null) {
                            null
                        } else {
                            CapabilityCopy.cardLine(
                                remoteLabels[CapabilityLabelTracker.CARD_KEY],
                                getString(R.string.capability_full_control),
                                getString(R.string.capability_best_effort),
                            )
                        }
                        if (trustRecord != null && !switcherOpen && TrustLogic.sheetVisible(sessionTrust.isNotEmpty() && records.isEmpty(), trustTvIdHex, true, trustEntryLoaded)) {
                            TrustSheet(
                                record = trustRecord,
                                maskedMac = IdentityRules.maskMac(trustRecord.wifiMac),
                                fingerprint = FingerprintFormat.format(trustEntry?.pin),
                                confirming = trustConfirming,
                                forgetting = trustForgetting,
                                forgetFailed = trustForgetFailed,
                                onRequestForget = {
                                    trustForgetFailed = false
                                    trustConfirming = true
                                },
                                onConfirmForget = {
                                    val hex = trustTvIdHex ?: return@TrustSheet
                                    runForget(TvId(hex))
                                },
                                onCancelConfirm = { trustConfirming = false },
                                onRetryForget = {
                                    val hex = trustTvIdHex ?: return@TrustSheet
                                    runForget(TvId(hex))
                                },
                                onCloseError = { trustForgetFailed = false },
                                onDismiss = {
                                    if (trustConfirming) {
                                        trustConfirming = false
                                    } else {
                                        trustTvIdHex = null
                                    }
                                },
                            )
                        }
                        if (durableHex != null && TargetSwitchRules.sheetVisible(target, switcherOpen, sessionTrust.isNotEmpty() && records.isEmpty(), trustTvIdHex != null)) {
                            TvSwitcherSheet(
                                tvName = record?.name?.ifBlank {
                                    getString(R.string.setup_not_reported)
                                } ?: getString(R.string.switcher_unavailable),
                                tvModel = listOfNotNull(record?.model, record?.modelName)
                                    .firstOrNull { it.isNotBlank() } ?: "",
                                capabilityPlate = remotePlate,
                                stateText = ConnectionUiRules.textFor(connSnapshot.state),
                                unreadable = TargetSwitchRules.rowUnreadable(target, record != null),
                                onSelectCurrent = { switcherOpen = false },
                                onShowDetails = {
                                    switcherOpen = false
                                    trustConfirming = false
                                    trustForgetFailed = false
                                    val hex = (target as? TargetSession.Selected)?.tvIdHex ?: durableHex
                                    trustTvIdHex = hex
                                },
                                onPairAgain = {
                                    switcherOpen = false
                                    pauseRemoteSocket()
                                    remoteTvIdHex = null
                                    remoteSessionIp = null
                                    openAddTv()
                                },
                                onDismiss = { switcherOpen = false },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::gate.isInitialized) {
            gate.refresh()
        }
    }

    private fun interfaceLabel(networkType: NetworkType): String = when (networkType) {
        NetworkType.Wifi -> "wlan"
        NetworkType.Ethernet -> "eth"
        NetworkType.Unknown -> "unknown"
    }

    private fun String?.ifNullOrBlank(fallback: String?): String =
        if (this.isNullOrBlank()) fallback ?: "" else this

    private fun networkName(wire: String?): String = when (wire) {
        "wifi" -> getString(R.string.gate_net_wifi)
        "ethernet" -> getString(R.string.gate_net_ethernet)
        else -> getString(R.string.gate_net_unknown)
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            RedactedLog.warn(TAG, "Settings intent failed")
        }
    }

    companion object {
        private const val TAG = "Shell"
    }
}

private sealed interface SetupRoute {
    data object Shell : SetupRoute
    data object AddTv : SetupRoute
    data class Pairing(val candidate: TvCandidate) : SetupRoute
    data object Paired : SetupRoute
    data class MatrixHarness(val tvIdHex: String) : SetupRoute
    data class Remote(val tvIdHex: String?, val sessionIp: String?) : SetupRoute
}

@Composable
private fun ShellScreenUnavailable(
    onPairAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        BasicText(
            text = stringResource(R.string.switcher_unavailable),
            style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
        GateQuietAction(
            label = stringResource(R.string.switcher_pair_again),
            description = stringResource(R.string.switcher_pair_again_cd),
            onClick = onPairAgain,
        )
    }
}
