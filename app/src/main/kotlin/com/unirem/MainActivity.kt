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
import com.unirem.core.api.LanAccess
import com.unirem.core.api.NetworkType
import com.unirem.core.api.RedactedLog
import com.unirem.gate.LanAccessGate
import com.unirem.setup.AddTvScreen
import com.unirem.setup.PairedScreen
import com.unirem.setup.PairingScreen
import com.unirem.setup.SetupLogic
import com.unirem.setup.SetupTopBar
import com.unirem.tv.KeystorePairingStore
import com.unirem.tv.RoomTvStore
import com.unirem.tv.api.DiscoveryResult
import com.unirem.tv.api.PairOutcome
import com.unirem.tv.api.PairingEntry
import com.unirem.tv.api.TvCandidate
import com.unirem.tv.api.TvGateways
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
                val sessionTrust = remember { mutableStateMapOf<String, PairingEntry>() }

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

                LaunchedEffect(Unit) {
                    records = withContext(Dispatchers.IO) { tvStore.all() }
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

                when (val current = route) {
                    SetupRoute.Shell -> {
                        val summary = records.firstOrNull()?.let {
                            if (it.model.isBlank()) it.name else "${it.name} · ${it.model}"
                        }?.ifBlank { getString(R.string.setup_not_reported) }
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
                        )
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
                                )
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
}
