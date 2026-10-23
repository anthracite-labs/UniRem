package com.unirem.spikea

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.security.NetworkSecurityPolicy
import com.unirem.spikea.logic.Backoff
import com.unirem.spikea.logic.ChannelUrl
import com.unirem.spikea.logic.CollisionRules
import com.unirem.spikea.logic.EvidenceSource
import com.unirem.spikea.logic.IdentityEntry
import com.unirem.spikea.logic.IdentityRules
import com.unirem.spikea.logic.KeyOrchestrator
import com.unirem.spikea.logic.KeyTargetSnapshot
import com.unirem.spikea.logic.MvpKey
import com.unirem.spikea.logic.RepeatRules
import com.unirem.spikea.logic.Redaction
import com.unirem.spikea.logic.ResultRow
import com.unirem.spikea.logic.RowResult
import com.unirem.spikea.logic.Scenario
import com.unirem.spikea.logic.Ssdp
import java.io.File
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Spike A orchestration. Every network run is one manual press; there is no
 * automatic reconnect, queue, or replay. Facts that need hardware stay blocked
 * in the report until a real run records them.
 */
class Harness(context: Context) {

    interface Listener {
        fun onLog(line: String)
        fun onState(state: State)
    }

    data class State(
        val activeTarget: String = "",
        val activeTvId: String = "",
        val sessionOnly: Boolean = false,
        val paired: Boolean = false,
        val rePairRequired: Boolean = false,
        val running: Boolean = false,
        val resultsPath: String = "",
        val resultLines: Int = 0,
        val targetCount: Int = 0,
        val keyConnected: Boolean = false,
        val keysEnabled: Boolean = false,
        val lastKey: String = "",
        val lastLatencyMs: Long = -1L,
        val lastEvidence: String = "",
    )

    private val appContext = context.applicationContext
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val lanGate = LanGate(appContext)
    private val discovery = Discovery(appContext, lanGate)
    private val pinnedTls = PinnedTls()
    private val channel = Channel(pinnedTls)
    private val vault = Vault(appContext)
    private val results = Results(File(appContext.filesDir, "spike-a-results.jsonl"))

    @Volatile
    var listener: Listener? = null

    @Volatile
    private var active: TvTarget? = null

    @Volatile
    private var discovered: List<TvTarget> = emptyList()

    @Volatile
    private var reconnectAttempt: Int = 0

    @Volatile
    private var nextReconnectAt: Long = 0L

    @Volatile
    private var running: Boolean = false

    @Volatile
    private var rePairRequired: Boolean = false

    // Key-run orchestration. The pure flow (frames, generations, evidence,
    // probes, close semantics) lives in KeyOrchestrator and is JVM-tested
    // against a fake transport; the harness only adapts vault, discovery,
    // results, and UI state around it. Exactly one repeat engine exists, owned
    // by the orchestrator; there is no second repeat path anywhere.
    private val keySession = KeySession(pinnedTls)
    private val repeatScheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var repeatFuture: ScheduledFuture<*>? = null

    private val keyLock = Any()

    private val keys = KeyOrchestrator(
        transport = keySession,
        onRow = { scenario, result, snap, notes, checks ->
            record(rowSnap(scenario, result, snap, notes, checks))
        },
        onLog = { line -> log(line) },
    )

    init {
        keySession.listener = object : KeySession.Listener {
            override fun onMachineEvent(name: String) = handleMachineEvent(name)
            override fun onSocketClosed(code: Int, reason: String) = handleKeySocketClosed(code, reason)
        }
    }

    fun resultsPath(): String = results.path()

    fun currentState(): State = snapshotState()

    fun reconnectInSeconds(): Long {
        val remaining = nextReconnectAt - System.currentTimeMillis()
        return if (remaining <= 0L) 0L else (remaining + 999L) / 1_000L
    }

    fun scan(year: String, region: String, label: String) = run("discovery") {
        log("SSDP M-SEARCH ${Ssdp.MULTICAST_HOST}:${Ssdp.MULTICAST_PORT} under one multicast lock.")
        when (val outcome = discovery.scan(year, region, label)) {
            is DiscoveryOutcome.Found -> {
                discovered = outcome.targets
                if (outcome.droppedHosts > 0) {
                    log("Dropped ${outcome.droppedHosts} non-IPv4 SSDP host(s).")
                }
                outcome.targets.forEach { target ->
                    log(
                        "found ${target.ip} | ${target.info.modelLabel} | fw=${target.info.firmware} | " +
                            "id=${target.identity.source.wire} | tvId=${target.tvId?.prefix ?: "session-only"}",
                    )
                }
                val entries = outcome.targets.map {
                    IdentityEntry("${it.ip} ${it.displayName}", it.identity, it.tvId)
                }
                val collisions = CollisionRules.collisions(entries)
                val distinct = outcome.targets.mapNotNull { it.tvId }.distinctBy { it.hex }
                record(
                    row(
                        scenario = Scenario.DISCOVERY,
                        result = RowResult.PASS,
                        target = outcome.targets.firstOrNull(),
                        notes = "targets=${outcome.targets.size} distinctTvIds=${distinct.size} collisions=${collisions.size}",
                        checks = mapOf(
                            "multicastLockHeld" to outcome.multicastHeld.toString(),
                            "targets" to outcome.targets.size.toString(),
                            "distinctTvIds" to distinct.size.toString(),
                        ),
                    ),
                )
                if (collisions.isNotEmpty()) {
                    log(
                        "COLLISION: ${collisions.joinToString("; ") { it.tvId.prefix }} on " +
                            collisions.joinToString("; ") { it.labels.joinToString(",") },
                    )
                }
                select(outcome.targets.first())
            }
            DiscoveryOutcome.NoDiscovery -> {
                record(
                    row(
                        Scenario.NO_DISCOVERY,
                        RowResult.BLOCKED,
                        notes = "SSDP returned no hosts; manual IP offered",
                    ),
                )
                log("NoDiscovery: SSDP returned nothing. Enter the TV IP and use Read /api/v2/. Not a TV failure.")
            }
            is DiscoveryOutcome.LanDenied -> {
                record(row(Scenario.LAN_DENIED, RowResult.BLOCKED, notes = outcome.reason))
                log("LanDenied: ${outcome.reason}. Both discovery and manual IP are blocked.")
            }
            is DiscoveryOutcome.Failed -> {
                record(row(Scenario.DISCOVERY, RowResult.FAIL, notes = outcome.reason))
                log("Discovery failed: ${outcome.reason}")
            }
        }
    }

    fun readManual(ip: String, year: String, region: String, label: String) = run("manual-ip") {
        when (val outcome = discovery.readManual(ip, year, region, label)) {
            is DiscoveryOutcome.Found -> {
                discovered = outcome.targets
                val target = outcome.targets.first()
                select(target)
                log(
                    "read ${target.ip} | ${target.info.modelLabel} | fw=${target.info.firmware} | " +
                        "id=${target.identity.source.wire} | tvId=${target.tvId?.prefix ?: "session-only"}",
                )
                if (target.sessionOnly) {
                    log("Session-only target: no device ID and no MAC. No durable record and no Keystore alias will be created.")
                    record(
                        row(
                            Scenario.SESSION_ONLY,
                            RowResult.BLOCKED,
                            target,
                            notes = "no resolvable identity; session-only; no durable record or alias",
                        ),
                    )
                } else {
                    record(row(Scenario.MANUAL_IP, RowResult.PASS, target, notes = "manual /api/v2/ read"))
                }
            }
            DiscoveryOutcome.NoDiscovery -> {
                record(row(Scenario.MANUAL_IP, RowResult.BLOCKED, notes = "no /api/v2/ answer"))
                log("No /api/v2/ answer at the manual IP. Recorded blocked.")
            }
            is DiscoveryOutcome.LanDenied -> {
                record(row(Scenario.LAN_DENIED, RowResult.BLOCKED, notes = outcome.reason))
                log("LanDenied: ${outcome.reason}.")
            }
            is DiscoveryOutcome.Failed -> {
                record(row(Scenario.MANUAL_IP, RowResult.FAIL, notes = outcome.reason))
                log("Manual read failed: ${outcome.reason}")
            }
        }
    }

    fun cycleTarget() {
        if (running) {
            log("cycle ignored: a run is already in progress.")
            return
        }
        val targets = discovered
        if (targets.isEmpty()) {
            log("No discovered targets to cycle.")
            return
        }
        val currentIp = active?.ip
        val index = targets.indexOfFirst { it.ip == currentIp }
        select(targets[(index + 1) % targets.size])
    }

    fun pair(allowWithoutRemote: Boolean) = run("pair") {
        val target = active ?: return@run log("No target selected. Scan or read a manual IP first.")
        if (lanDenied(target)) return@run
        val tvId = target.tvId
        if (tvId == null || target.sessionOnly) {
            record(
                row(
                    Scenario.SESSION_ONLY,
                    RowResult.BLOCKED,
                    target,
                    notes = "pairing refused: no stable identity; nothing persisted",
                ),
            )
            log("Session-only target: pairing is not persisted and no Keystore alias is created.")
            return@run
        }
        if (vault.get(tvId) != null) {
            log("A token/pin is already stored for ${tvId.prefix}. Use Reconnect, or Clear pairing to re-pair.")
            return@run
        }

        log("Pairing ${target.displayName} @ ${target.ip}. Watch the TV for the Allow/Deny prompt.")
        when (val probe = pinnedTls.capture(target.ip)) {
            is PinnedTls.Probe.Captured -> {
                log("Leaf SPKI captured. Connecting WSS without a token.")
                when (val outcome = channel.connect(target.ip, probe.pin, null, PAIR_TIMEOUT_SECONDS) { log(it) }) {
                    is Channel.Outcome.Connected -> {
                        val token = outcome.token
                        if (target.info.tokenAuthSupport && token.isNullOrBlank()) {
                            record(
                                row(
                                    Scenario.PAIR,
                                    RowResult.FAIL,
                                    target,
                                    allowWithoutRemote = yesNo(allowWithoutRemote),
                                    prompt = "connect",
                                    tokenPersists = "not-issued",
                                    pinStable = "captured",
                                    notes = "TokenAuthSupport=true but ms.channel.connect carried no token",
                                ),
                            )
                            log("Allow received but no token was delivered. Pairing premise failed for this firmware.")
                        } else {
                            try {
                                vault.put(tvId, token, probe.pin)
                                rePairRequired = false
                                reconnectAttempt = 0
                                nextReconnectAt = 0L
                                record(
                                    row(
                                        Scenario.PAIR,
                                        RowResult.PASS,
                                        target,
                                        allowWithoutRemote = yesNo(allowWithoutRemote),
                                        prompt = "connect",
                                        tokenPersists = if (token != null) "stored" else "not-issued",
                                        rePrompt = "no",
                                        pinStable = "captured",
                                        checks = mapOf(
                                            "authSupport" to target.info.tokenAuthSupport.toString(),
                                            "authDelivered" to (!token.isNullOrBlank()).toString(),
                                        ),
                                    ),
                                )
                                log("Paired. Token ${if (token != null) "stored" else "not issued (non-token-auth)"}; pin stored in Keystore-backed storage.")
                            } catch (e: Exception) {
                                record(
                                    row(
                                        Scenario.PAIR,
                                        RowResult.FAIL,
                                        target,
                                        allowWithoutRemote = yesNo(allowWithoutRemote),
                                        prompt = "connect",
                                        tokenPersists = "not-stored",
                                        pinStable = "captured",
                                        notes = "vault write failed",
                                    ),
                                )
                                log("Pairing could not be persisted: ${e.message ?: e.javaClass.simpleName}")
                            }
                        }
                    }
                    Channel.Outcome.Unauthorized -> {
                        record(
                            row(
                                Scenario.PAIR,
                                RowResult.FAIL,
                                target,
                                allowWithoutRemote = yesNo(allowWithoutRemote),
                                prompt = "unauthorized",
                                tokenPersists = "no",
                                pinStable = "captured",
                                notes = "TV denied or the prompt timed out; nothing persisted",
                            ),
                        )
                        log("Denied/unauthorized. Nothing persisted.")
                    }
                    Channel.Outcome.TimedOut -> {
                        record(
                            row(
                                Scenario.PAIR,
                                RowResult.BLOCKED,
                                target,
                                allowWithoutRemote = yesNo(allowWithoutRemote),
                                prompt = "no-response",
                                tokenPersists = "no",
                                pinStable = "captured",
                                notes = "no Allow/Deny evidence within timeout; silence is not failure",
                            ),
                        )
                        log("No channel event within the timeout. Recorded blocked, not failed. Nothing persisted.")
                    }
                    is Channel.Outcome.Closed -> {
                        record(
                            row(
                                Scenario.PAIR,
                                RowResult.FAIL,
                                target,
                                allowWithoutRemote = yesNo(allowWithoutRemote),
                                prompt = "closed",
                                tokenPersists = "no",
                                pinStable = "captured",
                                notes = "socket closed (${outcome.code}); nothing persisted",
                            ),
                        )
                        log("Socket closed (${outcome.code}). Nothing persisted.")
                    }
                    is Channel.Outcome.Failed -> {
                        record(
                            row(
                                Scenario.PAIR,
                                RowResult.FAIL,
                                target,
                                allowWithoutRemote = yesNo(allowWithoutRemote),
                                prompt = "failed",
                                tokenPersists = "no",
                                pinStable = "captured",
                                notes = outcome.reason,
                            ),
                        )
                        log("Channel failed: ${outcome.reason}")
                    }
                }
            }
            is PinnedTls.Probe.Failed -> {
                record(row(Scenario.PAIR, RowResult.FAIL, target, notes = "TLS capture failed: ${probe.reason}"))
                log("TLS capture failed: ${probe.reason}")
            }
            is PinnedTls.Probe.Match, is PinnedTls.Probe.Mismatch, is PinnedTls.Probe.Missing -> {
                record(row(Scenario.PAIR, RowResult.FAIL, target, notes = "unexpected probe state during first pairing"))
                log("Unexpected TLS probe state during first pairing.")
            }
        }
    }

    fun deny() = run("deny") {
        val target = active ?: return@run log("No target selected.")
        if (lanDenied(target)) return@run
        val tvId = target.tvId
        if (tvId == null || target.sessionOnly) {
            log("Deny test needs a stable identity to prove nothing was persisted.")
            return@run
        }
        if (vault.get(tvId) != null) {
            record(row(Scenario.DENY, RowResult.BLOCKED, target, notes = "stored pairing present; clear first"))
            log("A stored pairing exists. Clear pairing first, then run the Deny test.")
            return@run
        }

        log("Deny test: press Deny on the TV prompt (or let it time out). Nothing may be persisted.")
        log("If the TV remembers this phone, remove it in the TV's external device manager first.")
        when (val probe = pinnedTls.capture(target.ip)) {
            is PinnedTls.Probe.Captured -> {
                when (val outcome = channel.connect(target.ip, probe.pin, null, PAIR_TIMEOUT_SECONDS) { log(it) }) {
                    Channel.Outcome.Unauthorized, is Channel.Outcome.Closed -> {
                        val persisted = vault.get(tvId) != null
                        record(
                            row(
                                Scenario.DENY,
                                if (persisted) RowResult.FAIL else RowResult.PASS,
                                target,
                                prompt = "denied",
                                tokenPersists = "no",
                                pinStable = "captured",
                                checks = mapOf("storedAfterDeny" to persisted.toString()),
                            ),
                        )
                        log("Deny observed. Nothing persisted: ${!persisted}.")
                    }
                    Channel.Outcome.TimedOut -> {
                        record(
                            row(
                                Scenario.DENY,
                                RowResult.BLOCKED,
                                target,
                                prompt = "no-response",
                                notes = "no unauthorized/close evidence; silence is not failure",
                            ),
                        )
                        log("No deny/close event. Recorded blocked.")
                    }
                    is Channel.Outcome.Connected -> {
                        record(
                            row(
                                Scenario.DENY,
                                RowResult.FAIL,
                                target,
                                prompt = "connect",
                                notes = "Deny was not observed; channel connected",
                            ),
                        )
                        log("Channel connected; Deny was not observed.")
                    }
                    is Channel.Outcome.Failed -> {
                        record(row(Scenario.DENY, RowResult.FAIL, target, notes = outcome.reason))
                        log("Channel failed: ${outcome.reason}")
                    }
                }
            }
            is PinnedTls.Probe.Failed -> {
                record(row(Scenario.DENY, RowResult.FAIL, target, notes = probe.reason))
                log("TLS capture failed: ${probe.reason}")
            }
            is PinnedTls.Probe.Match, is PinnedTls.Probe.Mismatch, is PinnedTls.Probe.Missing -> {
                record(row(Scenario.DENY, RowResult.FAIL, target, notes = "unexpected probe state"))
                log("Unexpected TLS probe state.")
            }
        }
    }

    fun reconnect(rePromptSeen: Boolean) = run("reconnect") {
        val target = active ?: return@run log("No target selected.")
        if (lanDenied(target)) return@run
        val tvId = target.tvId
        if (tvId == null || target.sessionOnly) {
            log("Reconnect needs a stable identity.")
            return@run
        }
        val entry = vault.get(tvId) ?: return@run log("No stored pairing for ${tvId.prefix}. Pair first.")
        if (!Backoff.canAttempt(System.currentTimeMillis(), nextReconnectAt)) {
            val seconds = reconnectInSeconds()
            record(
                row(
                    Scenario.RECONNECT,
                    RowResult.BLOCKED,
                    target,
                    notes = "backoff: reconnect allowed in ${seconds}s; no automatic retry",
                    checks = mapOf("backoffSeconds" to seconds.toString()),
                ),
            )
            log("Backoff: reconnect allowed in ${seconds}s. No automatic retry.")
            return@run
        }

        when (val probe = pinnedTls.verify(target.ip, entry.pin)) {
            is PinnedTls.Probe.Match -> {
                log("Pinned leaf SPKI matches. Attaching the stored token.")
                val tokenSent = entry.token != null
                when (val outcome = channel.connect(target.ip, entry.pin, entry.token, RECONNECT_TIMEOUT_SECONDS) { log(it) }) {
                    is Channel.Outcome.Connected -> {
                        reconnectAttempt = 0
                        nextReconnectAt = 0L
                        rePairRequired = false
                        record(
                            row(
                                Scenario.RECONNECT,
                                RowResult.PASS,
                                target,
                                prompt = "none",
                                tokenPersists = if (tokenSent) "accepted" else "not-issued",
                                rePrompt = if (rePromptSeen) "yes" else "no",
                                pinStable = "match",
                                tokenSent = tokenSent,
                                checks = mapOf("sentAfterVerify" to tokenSent.toString()),
                            ),
                        )
                        log("Reconnected. Prompt seen: ${if (rePromptSeen) "yes" else "no"}.")
                    }
                    Channel.Outcome.Unauthorized -> {
                        val cleared = vault.clear(tvId)
                        rePairRequired = true
                        record(
                            row(
                                Scenario.TOKEN_REJECTED,
                                RowResult.FAIL,
                                target,
                                prompt = "unauthorized",
                                tokenPersists = "rejected",
                                rePrompt = "required",
                                pinStable = "match",
                                tokenSent = tokenSent,
                                notes = "Pairing is required for this TV." +
                                    (if (cleared) "" else " Stored pairing could not be cleared."),
                            ),
                        )
                        log("Pairing is required for this TV. Token rejected; re-pair. No token resent.")
                    }
                    Channel.Outcome.TimedOut -> {
                        armBackoff()
                        record(
                            row(
                                Scenario.RECONNECT,
                                RowResult.BLOCKED,
                                target,
                                tokenPersists = "unknown",
                                pinStable = "match",
                                tokenSent = tokenSent,
                                notes = "no channel event within timeout; offline; no retry queued",
                            ),
                        )
                        log("No channel event. Offline; backoff armed; no retry queued.")
                    }
                    is Channel.Outcome.Closed -> {
                        armBackoff()
                        record(
                            row(
                                Scenario.SOCKET_CLOSED,
                                RowResult.FAIL,
                                target,
                                tokenPersists = "unknown",
                                pinStable = "match",
                                tokenSent = tokenSent,
                                notes = "socket closed (${outcome.code}); offline; no retry queued",
                                checks = mapOf("offlineShown" to "true", "retries" to "0"),
                            ),
                        )
                        log("Socket closed (${outcome.code}). Offline; backoff armed; no retry queued.")
                    }
                    is Channel.Outcome.Failed -> {
                        armBackoff()
                        record(
                            row(
                                Scenario.RECONNECT,
                                RowResult.FAIL,
                                target,
                                tokenPersists = "unknown",
                                pinStable = "match",
                                tokenSent = tokenSent,
                                notes = outcome.reason,
                                checks = mapOf("retries" to "0"),
                            ),
                        )
                        log("Reconnect failed: ${outcome.reason}. Offline; backoff armed.")
                    }
                }
            }
            is PinnedTls.Probe.Mismatch -> {
                rePairRequired = true
                record(
                    row(
                        Scenario.PIN_MISMATCH,
                        RowResult.FAIL,
                        target,
                        pinStable = "mismatch",
                        tokenSent = false,
                        rePrompt = "required",
                        notes = "stored leaf SPKI differs from the leaf; fail closed before token attach",
                    ),
                )
                log("Pin mismatch. Failing closed: no token sent. Pairing is required for this TV.")
            }
            PinnedTls.Probe.Missing -> {
                record(
                    row(
                        Scenario.PIN_MISMATCH,
                        RowResult.FAIL,
                        target,
                        pinStable = "missing",
                        tokenSent = false,
                        notes = "stored pin missing",
                    ),
                )
                log("Stored pin missing. Re-pair.")
            }
            is PinnedTls.Probe.Failed -> {
                armBackoff()
                record(
                    row(
                        Scenario.RECONNECT,
                        RowResult.FAIL,
                        target,
                        tokenPersists = "unknown",
                        pinStable = "unknown",
                        tokenSent = false,
                        notes = "pin revalidation failed: ${probe.reason}",
                    ),
                )
                log("Pin revalidation failed: ${probe.reason}. Offline; backoff armed.")
            }
            is PinnedTls.Probe.Captured -> {
                record(row(Scenario.PIN_MISMATCH, RowResult.FAIL, target, notes = "unexpected capture during reconnect"))
                log("Unexpected capture during reconnect.")
            }
        }
    }

    fun revalidatePin() = run("pin-revalidate") {
        val target = active ?: return@run log("No target selected.")
        if (lanDenied(target)) return@run
        val tvId = target.tvId
        if (tvId == null || target.sessionOnly) {
            log("Pin revalidation needs a stable identity.")
            return@run
        }
        val entry = vault.get(tvId) ?: return@run log("No stored pairing for ${tvId.prefix}. Pair first.")
        when (val probe = pinnedTls.verify(target.ip, entry.pin)) {
            is PinnedTls.Probe.Match -> {
                record(row(Scenario.PIN_REVALIDATE, RowResult.PASS, target, pinStable = "match", tokenSent = false))
                log("Pin stable: leaf SPKI matches the stored pin. No token sent.")
            }
            is PinnedTls.Probe.Mismatch -> {
                rePairRequired = true
                record(row(Scenario.PIN_MISMATCH, RowResult.FAIL, target, pinStable = "mismatch", tokenSent = false))
                log("Pin mismatch: fail closed, no token sent.")
            }
            PinnedTls.Probe.Missing -> {
                record(row(Scenario.PIN_MISMATCH, RowResult.FAIL, target, pinStable = "missing", tokenSent = false))
                log("Stored pin missing: re-pair.")
            }
            is PinnedTls.Probe.Failed -> {
                record(row(Scenario.PIN_REVALIDATE, RowResult.BLOCKED, target, pinStable = "unknown", tokenSent = false, notes = probe.reason))
                log("Pin revalidation did not complete: ${probe.reason}. Recorded blocked.")
            }
            is PinnedTls.Probe.Captured -> {
                record(row(Scenario.PIN_MISMATCH, RowResult.FAIL, target, notes = "unexpected capture during revalidation"))
                log("Unexpected capture during revalidation.")
            }
        }
    }

    fun clearPairing() = run("clear-pairing") {
        val target = active ?: return@run log("No target selected.")
        val tvId = target.tvId
        if (tvId == null || target.sessionOnly) {
            log("Session-only target has no stored pairing.")
            return@run
        }
        if (vault.get(tvId) == null) {
            log("No stored pairing for ${tvId.prefix}. Nothing to clear.")
            return@run
        }
        if (!vault.clear(tvId)) {
            record(
                row(
                    Scenario.CLEAR_PAIRING,
                    RowResult.FAIL,
                    target,
                    tokenPersists = "unknown",
                    pinStable = "unknown",
                    notes = "stored pairing could not be cleared; re-pair is not proven",
                ),
            )
            log("Could not clear the stored pairing. Do not claim a re-pair; record the failure.")
            return@run
        }
        rePairRequired = true
        reconnectAttempt = 0
        nextReconnectAt = 0L
        record(
            row(
                Scenario.CLEAR_PAIRING,
                RowResult.PASS,
                target,
                tokenPersists = "cleared",
                pinStable = "cleared",
                notes = "token and pin removed; visible re-pair path",
            ),
        )
        log("Stored token and pin removed for ${tvId.prefix}. Pairing is required for this TV.")
    }

    fun twoTvCheck() = run("two-tv") {
        val targets = discovered
        if (targets.size < 2) {
            record(row(Scenario.TWO_TV, RowResult.BLOCKED, notes = "need two TVs; discovered ${targets.size}"))
            log("Two-TV check blocked: need two discovered TVs. Record the gap; the gate cannot fully pass.")
            return@run
        }
        val entries = targets.map { IdentityEntry("${it.ip} ${it.displayName}", it.identity, it.tvId) }
        val collisions = CollisionRules.collisions(entries)
        val distinct = entries.mapNotNull { it.tvId }.distinctBy { it.hex }
        val sessionOnly = entries.count { it.tvId == null }
        val paired = distinct.count { vault.get(it) != null }
        val labels = targets.map { it.info.modelLabel }
        val sameModel = labels.all { it.isNotBlank() } && labels.distinct().size == 1
        val result = when {
            collisions.isNotEmpty() -> RowResult.FAIL
            sessionOnly > 0 -> RowResult.BLOCKED
            paired < 2 -> RowResult.BLOCKED
            distinct.size >= 2 -> RowResult.PASS
            else -> RowResult.BLOCKED
        }
        record(
            row(
                Scenario.TWO_TV,
                result,
                targets.first(),
                collision = if (collisions.isEmpty()) "none" else "detected",
                notes = "targets=${entries.size} paired=$paired distinctTvIds=${distinct.size} " +
                    "collisions=${collisions.size} sessionOnly=$sessionOnly sameModel=$sameModel" +
                    (if (paired < 2) "; pair both TVs before this check" else ""),
                checks = mapOf(
                    "paired" to paired.toString(),
                    "distinctTvIds" to distinct.size.toString(),
                    "collisions" to collisions.size.toString(),
                    "sessionOnly" to sessionOnly.toString(),
                    "sameModel" to sameModel.toString(),
                ),
            ),
        )
        log("Two-TV check: $paired paired, ${distinct.size} distinct TvId(s), ${collisions.size} collision(s), $sessionOnly session-only. Same model: $sameModel.")
        if (collisions.isNotEmpty()) {
            log("Identity collision: ${collisions.joinToString("; ") { it.tvId.prefix }}. Gate cannot fully pass until resolved.")
        }
    }

    fun syntheticCollision() = run("synthetic-collision") {
        val identityA = IdentityRules.resolve("SYNTHETIC-ID-0001", "AA:BB:CC:DD:EE:01")
        val identityB = IdentityRules.resolve("SYNTHETIC-ID-0001", "AA:BB:CC:DD:EE:02")
        val entries = listOf(
            IdentityEntry("synthetic-a 10.0.0.1", identityA, IdentityRules.tvId(identityA)),
            IdentityEntry("synthetic-b 10.0.0.2", identityB, IdentityRules.tvId(identityB)),
        )
        val collisions = CollisionRules.collisions(entries)
        val pass = collisions.size == 1 && collisions.single().tvId == IdentityRules.tvId(identityA)
        record(
            row(
                Scenario.SYNTHETIC_COLLISION,
                if (pass) RowResult.PASS else RowResult.FAIL,
                collision = if (collisions.isEmpty()) "none" else "detected",
                notes = "identical device ID with different MACs resolves to one TvId; synthetic only; real same-model collision still required",
                checks = mapOf(
                    "syntheticCollisions" to collisions.size.toString(),
                    "gateImpact" to "blocked",
                ),
            ),
        )
        log("Synthetic collision: ${collisions.size} collision(s). This is a logic test only; the real two-TV gap remains.")
    }

    fun securityInspect() = run("security") {
        val appInfo = appContext.applicationInfo
        val allowBackup = (appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
        val backupRules = runCatching {
            appContext.resources.getXml(R.xml.backup_rules).close()
        }.isSuccess
        val dataExtractionRules = runCatching {
            appContext.resources.getXml(R.xml.data_extraction_rules).close()
        }.isSuccess
        val manifestFullBackup = hiddenApplicationInfoInt("fullBackupContent")
        val manifestDataExtraction = hiddenApplicationInfoInt("dataExtractionRulesRes")
        val keystore = vault.isKeystoreBacked()
        val vaultAudit = vault.audit()
        val resultsAudit = auditResults()
        val cleartext = NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted
        val checks = linkedMapOf(
            "allowBackup" to allowBackup.toString(),
            "backupRulesResource" to backupRules.toString(),
            "dataExtractionRulesResource" to dataExtractionRules.toString(),
            "manifestFullBackupContent" to (manifestFullBackup?.toString() ?: "unreadable"),
            "manifestDataExtractionRules" to (manifestDataExtraction?.toString() ?: "unreadable"),
            "keystoreBacked" to keystore.toString(),
            "vaultEntries" to vaultAudit.entries.toString(),
            "vaultCiphertextOnly" to vaultAudit.ciphertextOnly.toString(),
            "resultsFullHwAddress" to resultsAudit.fullMac.toString(),
            "resultsHighEntropyValue" to resultsAudit.longSecret.toString(),
            "cleartextPermitted" to cleartext.toString(),
            "channelScheme" to ChannelUrl.SCHEME,
            "wssToWsFallback" to "none",
            "hostnameVerifier" to "pinned-client-only",
        )
        val structural = !allowBackup && backupRules && dataExtractionRules
        val secrets = vaultAudit.ciphertextOnly && !resultsAudit.fullMac && !resultsAudit.longSecret
        val inspection = when {
            !structural || !secrets -> RowResult.FAIL
            vaultAudit.entries > 0 && !keystore -> RowResult.FAIL
            vaultAudit.entries == 0 -> RowResult.BLOCKED
            else -> RowResult.PASS
        }
        record(
            row(
                Scenario.SECURITY,
                inspection,
                notes = if (inspection == RowResult.BLOCKED) {
                    "runtime inspection blocked: pair a TV first so the Keystore alias exists; manifest linkage is recorded from the built APK with aapt2"
                } else {
                    "runtime inspection; manifest linkage is recorded from the built APK with aapt2; TLS trust-manager and wss-only claims are code-level and unit-tested; cleartext is limited to the 8001 info read"
                },
                checks = checks,
            ),
        )
        log(
            "Security inspection: ${inspection.wire}. allowBackup=$allowBackup " +
                "backupRules=$backupRules dataExtractionRules=$dataExtractionRules keystore=$keystore " +
                "vaultCiphertextOnly=${vaultAudit.ciphertextOnly} resultsFullMac=${resultsAudit.fullMac} " +
                "resultsLongSecret=${resultsAudit.longSecret}.",
        )
        if (inspection == RowResult.BLOCKED) {
            log("Pair a TV first, then run Inspect security again to prove the Keystore-backed vault.")
        } else if (inspection == RowResult.FAIL) {
            log("Security inspection found a failing check; record it and do not pass the gate.")
        }
    }

    fun shutdown() {
        executor.shutdownNow()
        repeatScheduler.shutdownNow()
        keySession.close("harness shutdown")
    }

    // Key runs. One gesture is one manual press; repeat frames never submit;
    // a socket write is never recorded as a TV action.

    fun openKeys() = run("open-keys") {
        val target = active ?: return@run log("No target selected. Scan or read a manual IP first.")
        if (lanDenied(target)) return@run
        val tvId = target.tvId
        if (tvId == null || target.sessionOnly) {
            record(
                row(
                    Scenario.KEY,
                    RowResult.BLOCKED,
                    target,
                    notes = "session-only target; key session needs a stable identity",
                    checks = mapOf("evidence" to "unconfirmed"),
                ),
            )
            log("Session-only target: no key session without a stable identity.")
            return@run
        }
        val entry = vault.get(tvId)
        if (entry == null) {
            record(
                row(
                    Scenario.KEY,
                    RowResult.BLOCKED,
                    target,
                    notes = "no stored pairing; pair first; a missing pairing blocks the gate",
                    checks = mapOf("evidence" to "unconfirmed"),
                ),
            )
            log("No stored pairing for ${tvId.prefix}. Pair first; the gate stays shut.")
            return@run
        }
        if (keySession.isOpen()) {
            log("Key session already open.")
            return@run
        }
        // Revalidate the pin before the token is attached, same as reconnect.
        when (val probe = pinnedTls.verify(target.ip, entry.pin)) {
            is PinnedTls.Probe.Match -> {
                log("Pin matches. Attaching the stored token and holding the key channel open.")
                when (val outcome = keySession.open(target.ip, entry.pin, entry.token, KEY_OPEN_TIMEOUT_SECONDS) { log(it) }) {
                    is KeySession.OpenOutcome.Opened -> {
                        keys.setEnabled(true)
                        log("Key session open on the pinned, token-bearing channel. Model, firmware, network, on/reachable recorded per key row.")
                    }
                    KeySession.OpenOutcome.Unauthorized -> {
                        val cleared = vault.clear(tvId)
                        rePairRequired = true
                        record(
                            row(
                                Scenario.TOKEN_REJECTED,
                                RowResult.FAIL,
                                target,
                                prompt = "unauthorized",
                                tokenPersists = "rejected",
                                rePrompt = "required",
                                pinStable = "match",
                                tokenSent = entry.token != null,
                                notes = "key channel rejected the token; pairing is required for this TV." +
                                    (if (cleared) "" else " Stored pairing could not be cleared."),
                            ),
                        )
                        log("Pairing is required for this TV. Token rejected on the key channel; re-pair. No token resent.")
                    }
                    KeySession.OpenOutcome.TimedOut -> {
                        record(
                            row(
                                Scenario.KEY,
                                RowResult.BLOCKED,
                                target,
                                notes = "no channel event within timeout; silence is not failure; no command sent",
                                checks = mapOf("evidence" to "unconfirmed"),
                            ),
                        )
                        log("No channel event within the timeout. Recorded blocked; no command sent.")
                    }
                    is KeySession.OpenOutcome.Closed -> {
                        record(
                            row(
                                Scenario.KEY,
                                RowResult.BLOCKED,
                                target,
                                notes = "socket closed during open (${outcome.code}); no command sent",
                                checks = mapOf("evidence" to "unconfirmed"),
                            ),
                        )
                        log("Socket closed during open (${outcome.code}). No command sent.")
                    }
                    is KeySession.OpenOutcome.Failed -> {
                        record(
                            row(
                                Scenario.KEY,
                                RowResult.BLOCKED,
                                target,
                                notes = outcome.reason,
                                checks = mapOf("evidence" to "unconfirmed"),
                            ),
                        )
                        log("Key channel failed: ${outcome.reason}")
                    }
                }
            }
            is PinnedTls.Probe.Mismatch -> {
                rePairRequired = true
                record(
                    row(
                        Scenario.PIN_MISMATCH,
                        RowResult.FAIL,
                        target,
                        pinStable = "mismatch",
                        tokenSent = false,
                        rePrompt = "required",
                        notes = "stored leaf SPKI differs; fail closed before token attach; key session not opened",
                    ),
                )
                log("Pin mismatch. Failing closed: no token sent, key session not opened.")
            }
            PinnedTls.Probe.Missing -> {
                record(
                    row(
                        Scenario.PIN_MISMATCH,
                        RowResult.FAIL,
                        target,
                        pinStable = "missing",
                        tokenSent = false,
                        notes = "stored pin missing; re-pair",
                    ),
                )
                log("Stored pin missing: re-pair.")
            }
            is PinnedTls.Probe.Failed -> {
                record(
                    row(
                        Scenario.KEY,
                        RowResult.BLOCKED,
                        target,
                        notes = "pin revalidation failed: ${probe.reason}; no token sent",
                        checks = mapOf("evidence" to "unconfirmed"),
                    ),
                )
                log("Pin revalidation failed: ${probe.reason}. No token sent.")
            }
            is PinnedTls.Probe.Captured -> {
                record(row(Scenario.PIN_MISMATCH, RowResult.FAIL, target, notes = "unexpected capture during key open"))
                log("Unexpected capture during key open.")
            }
        }
    }

    fun sendKey(keyName: String) = run("key") {
        val target = active ?: return@run log("No target selected.")
        val key = MvpKey.byName(keyName)
            ?: return@run log("Unknown key \"$keyName\". No frame built, no row recorded.")
        if (lanDenied(target)) return@run
        keys.tap(key, snapshotOf(target))
    }

    fun holdStart(keyName: String) = run("hold-start") {
        val target = active ?: return@run log("No target selected.")
        val key = MvpKey.byName(keyName)
            ?: return@run log("Unknown key \"$keyName\". No frame built.")
        if (lanDenied(target)) return@run
        if (!keys.holdStart(key, snapshotOf(target))) return@run
        synchronized(keyLock) {
            repeatFuture?.cancel(false)
            // Fixed-delay, not fixed-rate: lint discourages fixed-rate for
            // cached Android processes, and due() self-corrects drift anyway.
            // Ticks are capped inside the orchestrator so a stalled scheduler
            // cannot flood the socket.
            repeatFuture = repeatScheduler.scheduleWithFixedDelay(
                { repeatTick() },
                RepeatRules.INITIAL_DELAY_MS,
                RepeatRules.CADENCE_MS,
                TimeUnit.MILLISECONDS,
            )
        }
    }

    fun holdStop() = run("hold-stop") {
        cancelRepeat()
        keys.holdStop(snapshotOf(active))
    }

    fun markObserved() = run("observed") {
        keys.observe(EvidenceSource.OPERATOR, "operator saw the on-TV effect", snapshotOf(active))
    }

    fun markMachineObserved() = run("observed-machine") {
        keys.observe(EvidenceSource.MACHINE, "run log shows a machine event evidencing the effect", snapshotOf(active))
    }

    fun markProbeObserved() = run("observed-probe") {
        keys.observe(EvidenceSource.PROBE, "state probe evidences the effect", snapshotOf(active))
    }

    fun markNotObserved() = run("not-observed") {
        keys.notObserved(snapshotOf(active))
    }

    fun recordUnsupported(keyName: String) = run("unsupported-key") {
        val key = MvpKey.byName(keyName)
            ?: return@run log("Unknown key \"$keyName\". Nothing recorded.")
        keys.unsupported(key, snapshotOf(active))
    }

    fun markLastUnsupported() = run("unsupported-key") {
        keys.markLastUnsupported(snapshotOf(active))
    }

    fun powerOff() = run("power-off") {
        val target = active ?: return@run log("No target selected.")
        if (lanDenied(target)) return@run
        val press = keys.powerSend(snapshotOf(target)) ?: return@run
        log("Power sent once (${MvpKey.POWER.dataOfCmd}). Waiting ${POWER_PROBE_DELAY_SECONDS}s for standby, then probing /api/v2/ and the channel.")
        try {
            Thread.sleep(POWER_PROBE_DELAY_SECONDS * 1_000L)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        keys.probePower(press, probeInput(press.target))
    }

    fun probePower() = run("probe-power") {
        val press = keys.lastPowerPress
            ?: return@run log("Probe power needs a power press first. Send Power off, then probe.")
        if (!keys.isUnconfirmed(press.gestureId)) {
            return@run log("That power gesture already resolved; send Power off again to probe.")
        }
        keys.probePower(press, probeInput(press.target))
    }

    fun latencySummary() = run("latency") {
        keys.latencySummary(snapshotOf(active))
    }

    fun closeKeys() = run("close-keys") {
        cancelRepeat()
        keys.close("manual", recordRows = true)
    }

    private fun repeatTick() {
        try {
            val was = keys.keysEnabled
            keys.holdTick()
            if (was && !keys.keysEnabled) publishState()
        } catch (t: Throwable) {
            log("repeat tick failed: ${t.message ?: t.javaClass.simpleName}")
        }
    }

    /**
     * Machine events are logged with the open-gesture context but never
     * auto-confirm a key: the harness cannot know which event evidences which
     * press, and a delayed event must never rewrite a newer press. The
     * operator attributes an event with the Observed: machine button, which
     * binds the exact generation.
     */
    private fun handleMachineEvent(name: String) {
        keys.noteMachineEvent(name)
    }

    private fun handleKeySocketClosed(code: Int, reason: String) {
        cancelRepeat()
        keys.onTransportClosed(code, reason)
        publishState()
    }

    private fun cancelRepeat() {
        synchronized(keyLock) {
            repeatFuture?.cancel(false)
            repeatFuture = null
        }
    }

    /** State probe inputs for a power press: /api/v2/ reachability plus channel state. */
    private fun probeInput(snap: KeyTargetSnapshot): KeyOrchestrator.ProbeInput {
        val outcome = discovery.readManual(snap.ip, snap.modelYear, snap.region, snap.label)
        return KeyOrchestrator.ProbeInput(
            apiReachable = outcome is DiscoveryOutcome.Found,
            phoneOnline = outcome !is DiscoveryOutcome.LanDenied,
            channelOpen = keySession.isOpen(),
        )
    }

    private fun run(name: String, block: () -> Unit) {
        if (running) {
            log("$name ignored: a run is already in progress.")
            return
        }
        running = true
        publishState()
        executor.execute {
            try {
                block()
            } catch (t: Throwable) {
                log("$name failed: ${t.message ?: t.javaClass.simpleName}")
            } finally {
                running = false
                publishState()
            }
        }
    }

    private fun select(target: TvTarget) {
        active = target
        reconnectAttempt = 0
        nextReconnectAt = 0L
        log("Selected ${target.displayName} @ ${target.ip} (${target.tvId?.prefix ?: "session-only"}).")
        publishState()
    }

    private fun lanDenied(target: TvTarget?): Boolean {
        val gate = lanGate.check()
        if (gate is LanGate.Status.Denied) {
            record(row(Scenario.LAN_DENIED, RowResult.BLOCKED, target, notes = gate.reason))
            log("LanDenied: ${gate.reason}. Network work is blocked; this is not a TV failure.")
            return true
        }
        return false
    }

    private fun armBackoff() {
        nextReconnectAt = Backoff.nextAttemptAt(System.currentTimeMillis(), reconnectAttempt)
        reconnectAttempt = (reconnectAttempt + 1).coerceAtMost(10)
    }

    private fun record(row: ResultRow) {
        try {
            results.append(row)
        } catch (e: Exception) {
            log("RESULT WRITE FAILED: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun row(
        scenario: Scenario,
        result: RowResult,
        target: TvTarget? = null,
        notes: String = "",
        checks: Map<String, String> = emptyMap(),
        allowWithoutRemote: String = "",
        prompt: String = "",
        tokenPersists: String = "",
        rePrompt: String = "",
        pinStable: String = "",
        collision: String = "",
        tokenSent: Boolean = false,
    ): ResultRow {
        val phone = lanGate.networkSnapshot()
        val tvNetwork = target?.info?.reportedNetworkType?.let { tvNetworkLabel(it) } ?: ""
        return ResultRow(
            runId = UUID.randomUUID().toString(),
            at = Instant.now().toString(),
            scenario = scenario,
            result = result,
            label = target?.displayName ?: "",
            model = target?.info?.modelLabel ?: "",
            modelYear = target?.year ?: "",
            region = target?.region ?: "",
            firmware = target?.info?.firmware ?: "",
            networkType = tvNetwork.ifBlank { phone.type },
            networkDetail = "${phone.detail}; phone=${phone.type}" +
                if (tvNetwork.isNotBlank()) "; tv=$tvNetwork" else "",
            identitySource = target?.identity?.source?.wire ?: "",
            tvIdPrefix = target?.tvId?.prefix ?: "",
            sessionOnly = target?.sessionOnly == true,
            allowWithoutRemote = allowWithoutRemote,
            prompt = prompt,
            tokenPersists = tokenPersists,
            rePrompt = rePrompt,
            pinStable = pinStable,
            collision = collision,
            tokenSent = tokenSent,
            notes = notes,
            checks = checks,
        )
    }

    /**
     * Press-time snapshot of the active target. Confirm, probe, and close-idle
     * rows are built from this, so cycling targets between press and probe can
     * never attribute TV-B evidence to a TV-A gesture. The IP rides along for
     * the power probe only and never reaches a row.
     */
    private fun snapshotOf(target: TvTarget?): KeyTargetSnapshot {
        val phone = lanGate.networkSnapshot()
        val tvNetwork = target?.info?.reportedNetworkType?.let { tvNetworkLabel(it) } ?: ""
        return KeyTargetSnapshot(
            ip = target?.ip ?: "",
            label = target?.displayName ?: "",
            model = target?.info?.modelLabel ?: "",
            modelYear = target?.year ?: "",
            region = target?.region ?: "",
            firmware = target?.info?.firmware ?: "",
            networkType = tvNetwork.ifBlank { phone.type },
            networkDetail = "${phone.detail}; phone=${phone.type}" +
                if (tvNetwork.isNotBlank()) "; tv=$tvNetwork" else "",
            identitySource = target?.identity?.source?.wire ?: "",
            tvIdPrefix = target?.tvId?.prefix ?: "",
            sessionOnly = target?.sessionOnly == true,
        )
    }

    private fun rowSnap(
        scenario: Scenario,
        result: RowResult,
        snap: KeyTargetSnapshot,
        notes: String = "",
        checks: Map<String, String> = emptyMap(),
    ): ResultRow = ResultRow(
        runId = UUID.randomUUID().toString(),
        at = Instant.now().toString(),
        scenario = scenario,
        result = result,
        label = snap.label,
        model = snap.model,
        modelYear = snap.modelYear,
        region = snap.region,
        firmware = snap.firmware,
        networkType = snap.networkType,
        networkDetail = snap.networkDetail,
        identitySource = snap.identitySource,
        tvIdPrefix = snap.tvIdPrefix,
        sessionOnly = snap.sessionOnly,
        notes = notes,
        checks = checks,
    )

    private fun snapshotState(): State {
        val target = active
        val tvId = target?.tvId
        val paired = if (tvId != null) vault.get(tvId) != null else false
        return State(
            activeTarget = target?.let { "${it.displayName} @ ${it.ip}" } ?: "",
            activeTvId = tvId?.prefix ?: "",
            sessionOnly = target?.sessionOnly == true,
            paired = paired,
            rePairRequired = rePairRequired,
            running = running,
            resultsPath = results.path(),
            resultLines = results.lineCount(),
            targetCount = discovered.size,
            keyConnected = keySession.isOpen(),
            keysEnabled = keys.keysEnabled,
            lastKey = keys.lastKeyLabel,
            lastLatencyMs = keys.lastLatencyMs,
            lastEvidence = keys.lastEvidence,
        )
    }

    private fun publishState() {
        val state = snapshotState()
        main.post { listener?.onState(state) }
    }

    private fun log(message: String) {
        val line = "${TIME.format(LocalTime.now())} ${RedactedLog.line(message)}"
        main.post { listener?.onLog(line) }
    }

    private fun yesNo(value: Boolean): String = if (value) "yes" else "no"

    private fun tvNetworkLabel(reported: String): String = when (reported.trim().lowercase(Locale.ROOT)) {
        "wifi", "wi-fi", "wireless" -> "Wifi"
        "ethernet", "wired" -> "Ethernet"
        else -> if (reported.isBlank()) "" else "Unknown"
    }

    /**
     * The SDK stubs hide ApplicationInfo.fullBackupContent and
     * ApplicationInfo.dataExtractionRulesRes. Read them best-effort; the report
     * cites the built APK's manifest with aapt2 for the authoritative linkage.
     */
    private fun hiddenApplicationInfoInt(name: String): Int? = try {
        ApplicationInfo::class.java.getField(name).getInt(appContext.applicationInfo)
    } catch (e: Exception) {
        null
    }

    private data class ResultsAudit(val fullMac: Boolean, val longSecret: Boolean)

    private fun auditResults(): ResultsAudit {
        val lines = results.readLines()
        return ResultsAudit(
            fullMac = lines.any { Redaction.containsMac(it) },
            longSecret = lines.any { Redaction.containsLongSecret(it) },
        )
    }

    private companion object {
        const val PAIR_TIMEOUT_SECONDS = 60L
        const val RECONNECT_TIMEOUT_SECONDS = 15L
        const val KEY_OPEN_TIMEOUT_SECONDS = 15L
        const val POWER_PROBE_DELAY_SECONDS = 5L
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    }
}
