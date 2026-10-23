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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.LaunchedEffect
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
import com.unirem.core.api.IdentityRules
import com.unirem.core.api.LanAccess
import com.unirem.core.api.NetworkType
import com.unirem.core.api.RedactedLog
import com.unirem.core.api.TvId
import com.unirem.connection.BannerKind
import com.unirem.connection.ConnectionController
import com.unirem.connection.ConnectionScreen
import com.unirem.connection.RePairCommit
import com.unirem.connection.TargetSessionHolder
import com.unirem.gate.LanAccessGate
import com.unirem.setup.AddTvScreen
import com.unirem.setup.PairedScreen
import com.unirem.setup.PairingScreen
import com.unirem.setup.SetupLogic
import com.unirem.setup.SetupTopBar
import com.unirem.setup.TrustLogic
import com.unirem.setup.TrustSheet
import com.unirem.tv.KeystorePairingStore
import com.unirem.tv.RoomTvStore
import com.unirem.tv.wire
import com.unirem.tv.api.DiscoveryResult
import com.unirem.tv.api.PairOutcome
import com.unirem.tv.api.PairingEntry
import com.unirem.tv.api.TvCandidate
import com.unirem.tv.api.TvGateways
import com.unirem.tv.api.FingerprintFormat
import com.unirem.tv.api.ConnectionBackoff
import com.unirem.tv.channel.ConnectionHealth
import com.unirem.ui.graphitemint.GraphiteMintSpacing
import com.unirem.ui.graphitemint.GraphiteMintTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                val targetSession = remember { TargetSessionHolder() }
                var connectionTvIdHex by remember { mutableStateOf<String?>(null) }
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
                val connectionSession = remember {
                    TvGateways.connection(gateCheck = { gate.checkBeforeAttempt() })
                }
                // One health owner per open surface; recreated per TvId so a
                // stale socket never leaks into the next surface.
                val connectionHealth = remember(connectionTvIdHex) { ConnectionHealth() }
                val connectionController = remember(connectionTvIdHex) {
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
                            val wanted = connectionTvIdHex
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

                LaunchedEffect(Unit) {
                    records = withContext(Dispatchers.IO) { tvStore.all() }
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
                                    pairedName = ""
                                    pairedModel = ""
                                    pairedSessionOnly = false
                                    trustConfirming = false
                                    trustForgetFailed = false
                                    trustTvIdHex = null
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

                // Story 1.7: socket-scoped session. Starts exactly one
                // connection from the stored record, token, and pin; leaving
                // closes the socket and stops backoff. No auto-connect on
                // launch or while closed; the paired summary stays "Paired".
                fun closeConnection() {
                    connRunJob?.cancel()
                    connRunJob = null
                    connectionController.stop()
                    connEntry = null
                    connLoaded = false
                    rePairing = false
                    rePairDenied = false
                    rePairTimedOut = false
                    rePairFailed = null
                    rePairCandidate = null
                    rePairTargetHex = null
                    connectionTvIdHex = null
                    targetSession.clear()
                    route = SetupRoute.Shell
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

                // Open the surface: select the process-scoped target, load
                // the stored trust, then start the single connection. The
                // Keystore read gates the first attempt so no socket opens
                // without the stored pin.
                LaunchedEffect(connectionTvIdHex) {
                    val hex = connectionTvIdHex
                    if (hex == null) {
                        connEntry = null
                        connLoaded = false
                    } else {
                        targetSession.select(TvId(hex))
                        connLoaded = false
                        val entry = withContext(Dispatchers.IO) { pairingStore.load(TvId(hex)) }
                        connEntry = entry
                        connLoaded = true
                        val record = records.firstOrNull { it.tvId == hex }
                        if (record != null && entry != null) {
                            startConnectionRun(
                                hex,
                                record.lastIp,
                                entry.pin,
                                entry.token,
                                entry.token != null,
                            )
                        } else if (record != null) {
                            // Stored record but no Keystore entry: terminal
                            // pairing-required, no socket work.
                            connectionController.showPairingRequired()
                        }
                    }
                }
                // Network change is a socket close, never stale-socket reuse.
                LaunchedEffect(snapshot.networkType) {
                    if (connectionTvIdHex != null && connLoaded) {
                        connectionController.onNetworkChanged()
                    }
                }

                when (val current = route) {
                    SetupRoute.Shell -> {
                        val summary = records.firstOrNull()?.let {
                            if (it.model.isBlank()) it.name else "${it.name} · ${it.model}"
                        }?.ifBlank { getString(R.string.setup_not_reported) }
                        val sessionOnlyActive = sessionTrust.isNotEmpty() && records.isEmpty()
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
                            hasPairedTv = records.isNotEmpty(),
                            tvSummary = summary,
                            onAddTv = {
                                dismissed = emptySet()
                                manualKeys = emptySet()
                                targets = emptyList()
                                diagnostic = null
                                manualError = null
                                scanDone = false
                                route = SetupRoute.AddTv
                                startScan()
                            },
                            onShowDetails = {
                                val hex = records.firstOrNull()?.tvId
                                if (hex != null) {
                                    trustConfirming = false
                                    trustForgetFailed = false
                                    trustTvIdHex = hex
                                }
                            },
                            detailsFocusRequester = detailsFocus,
                            sessionOnlyActive = sessionOnlyActive,
                            onCheckConnection = {
                                val hex = records.firstOrNull()?.tvId
                                if (hex != null) {
                                    rePairDenied = false
                                    rePairTimedOut = false
                                    rePairFailed = null
                                    connectionTvIdHex = hex
                                    route = SetupRoute.Connection(hex)
                                }
                            },
                        )
                        // Durable-only trust sheet keyed by TvId. Session-only
                        // targets get no sheet. Gated on the finished
                        // Keystore load so "Fingerprint unavailable" never
                        // flashes mid-read.
                        if (trustRecord != null && TrustLogic.sheetVisible(sessionOnlyActive, trustTvIdHex, true, trustEntryLoaded)) {
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
                                )
                            }
                        }
                    }
                    is SetupRoute.Connection -> {
                        val record = records.firstOrNull { it.tvId == current.tvIdHex }
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
                                if (record == null) {
                                    SetupTopBar(onBack = { closeConnection() })
                                    Spacer(modifier = Modifier.height(GraphiteMintSpacing.X4))
                                } else if (rePairCandidate != null) {
                                    // Re-pair Allow flow, reused from Story 1.5
                                    // with the stored identity: the TV shows
                                    // its Allow prompt, the no-remote path
                                    // stays visible, no old token is sent.
                                    // Back keeps the connection banner.
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
                                    ConnectionScreen(
                                        tvName = record.name.ifBlank { getString(R.string.setup_not_reported) },
                                        state = connSnapshot.state,
                                        power = connSnapshot.power,
                                        banner = connSnapshot.banner,
                                        onBack = { closeConnection() },
                                        onRetry = {
                                            val entry = connEntry
                                            val rec = records.firstOrNull { it.tvId == current.tvIdHex }
                                            if (entry == null || rec == null) {
                                                return@ConnectionScreen
                                            }
                                            val ip = rec.lastIp
                                            connRunJob?.cancel()
                                            connRunJob = scope.launch(Dispatchers.IO) {
                                                val reached = connectionController.retry(
                                                    TvId(current.tvIdHex),
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
                                                            current.tvIdHex,
                                                            ip,
                                                            entry.pin,
                                                            entry.token,
                                                            entry.token != null,
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onNotNow = {
                                            connRunJob?.cancel()
                                            connectionController.notNow()
                                        },
                                        onRePair = { runRePair(record) },
                                    )
                                }
                            }
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
    data class Connection(val tvIdHex: String) : SetupRoute
}
