package com.unirem.gate

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.unirem.core.api.LanAccess
import com.unirem.core.api.LanDiagnosticCode
import com.unirem.core.api.NetworkType
import com.unirem.core.api.RedactedLog
import com.unirem.core.api.resolveDiagnosticCode
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "LanGate"

/**
 * Retained gate decision: the last settled access plus whether the system
 * prompt has ever fired. Both survive restart and rotation so a denial is
 * never re-offered as a fresh setup.
 */
data class StoredGateDecision(
    val access: LanAccess?,
    val asked: Boolean,
)

/** Persistence seam for the retained decision; faked in JVM tests. */
interface LanAccessStore {
    suspend fun load(): StoredGateDecision
    suspend fun save(decision: StoredGateDecision)
}

/** Quiet snapshot rendered by the shell diagnostic readout. */
data class LanGateSnapshot(
    val access: LanAccess,
    val networkType: NetworkType,
    val code: LanDiagnosticCode,
    val scanned: Boolean = false,
)

/** What pressing Continue on the explainer must do. Never prompt when denied. */
enum class ContinueAction {
    FirePrompt,
    Proceed,
    OpenSettings,
}

/**
 * Single process-wide scoped handle for the Wifi multicast lock. The Android
 * implementation is the only [WifiManager]/`MulticastLock` holder in the
 * product; JVM tests inject a fake.
 */
interface MulticastLockController {
    fun acquire(): Boolean
    fun release()
}

/**
 * Sole owner of the local-network permission, the single process-wide
 * multicast lock, the isolation diagnostic, and the manual-IP route.
 *
 * Never holds an Activity reference: the Activity owns the permission
 * launcher and the settings intent and forwards results here. Transport and
 * platform reads are constructor-injected lambdas so JVM tests run without a
 * device.
 */
class LanAccessGate(
    private val sdkInt: Int,
    private val store: LanAccessStore,
    private val platformGranted: () -> Boolean,
    private val transportSource: () -> NetworkType,
    private val lockController: MulticastLockController,
    private val scope: CoroutineScope,
) {
    private val _access = MutableStateFlow(initialSyncAccess())
    val access: StateFlow<LanAccess> = _access.asStateFlow()

    private val _snapshot = MutableStateFlow(snapshotFor(_access.value, readTransport()))
    val snapshot: StateFlow<LanGateSnapshot> = _snapshot.asStateFlow()

    @Volatile
    private var asked = false

    // Shared exclusion for both lock paths: one atomic held flag plus depth
    // counting so nested scopes share the single platform lock.
    private val lockExclusion = Any()
    private var lockDepth = 0
    private val lockHeld = AtomicBoolean(false)

    init {
        scope.launch {
            val stored = try {
                store.load()
            } catch (e: Exception) {
                RedactedLog.warn(TAG, "Retained decision unreadable, re-deriving")
                StoredGateDecision(null, false)
            }
            asked = stored.asked
            val resolved = resolveInitial(sdkInt, stored.access, runCatching { platformGranted() }.getOrDefault(false))
            publish(resolved, persist = false)
        }
    }

    /** Current gate state. Every network attempt (scan, socket, manual IP) passes here first. */
    fun checkBeforeAttempt(): LanAccess = _access.value

    fun isLockHeld(): Boolean = lockHeld.get()

    /**
     * Continue pressed on the explainer. Fires the system prompt only for the
     * undecided state on API 37+; below API 37 the gate is already granted;
     * when denied this never re-fires the prompt.
     */
    fun onExplainerContinue(firePrompt: () -> Unit, onProceed: () -> Unit) {
        when (continueActionFor(_access.value, sdkInt)) {
            ContinueAction.FirePrompt -> {
                asked = true
                persist()
                firePrompt()
            }
            ContinueAction.Proceed -> {
                publish(LanAccess.Granted, persist = true)
                onProceed()
            }
            ContinueAction.OpenSettings -> {
                RedactedLog.debug(TAG, "Continue on denied gate ignored, no prompt")
            }
        }
    }

    /** System-prompt result forwarded by the Activity. */
    fun onPermissionResult(granted: Boolean) {
        asked = true
        publish(reducePermissionResult(granted), persist = true)
    }

    /**
     * Settles a return from system settings in both directions: an external
     * grant upgrades to granted; a revoke from granted records denial;
     * anything else is unchanged.
     */
    fun onSettingsReturned(nowGranted: Boolean) {
        publish(reduceSettingsReturn(_access.value, sdkInt, nowGranted), persist = true)
    }

    /**
     * Recomputes the snapshot from live platform state. Called from
     * `onResume` so joining Wi-Fi or a settings-side change surfaces without
     * a relaunch. Never prompts and never opens a wall on its own.
     */
    fun refresh() {
        val current = _access.value
        val next = reduceSettingsReturn(current, sdkInt, runCatching { platformGranted() }.getOrDefault(false))
        publish(next, persist = next != current)
    }

    /** Runs [block] under the single process-wide lock, releasing on every exit. */
    fun <T> withMulticastLock(block: () -> T): T =
        guardedRun(::platformAcquire, ::platformRelease, block)

    /**
     * Alias scanners use so product callers never spell the platform handle.
     * Same single held scope as [withMulticastLock], same release semantics.
     */
    fun <T> underHeldScope(block: () -> T): T = withMulticastLock(block)

    /** Suspend twin of [withMulticastLock]; cancellation still releases. */
    suspend fun <T> withMulticastLockSuspend(block: suspend () -> T): T =
        guardedRunSuspend(::platformAcquire, ::platformRelease, block)

    private fun initialSyncAccess(): LanAccess =
        if (sdkInt < LOCAL_NETWORK_SDK) {
            LanAccess.Granted
        } else if (runCatching { platformGranted() }.getOrDefault(false)) {
            LanAccess.Granted
        } else {
            LanAccess.NotDeclared
        }

    private fun publish(next: LanAccess, persist: Boolean) {
        _access.value = next
        _snapshot.value = snapshotFor(next, readTransport())
        if (persist) persist()
    }

    private fun persist() {
        val decision = StoredGateDecision(_access.value, asked)
        scope.launch {
            try {
                store.save(decision)
            } catch (e: Exception) {
                RedactedLog.warn(TAG, "Retained decision unwritable")
            }
        }
    }

    private fun readTransport(): NetworkType =
        try {
            transportSource()
        } catch (e: Exception) {
            RedactedLog.warn(TAG, "Transport unreadable, assuming unknown")
            NetworkType.Unknown
        }

    private fun platformAcquire(): Boolean =
        synchronized(lockExclusion) {
            if (lockDepth > 0) {
                lockDepth++
                return true
            }
            val ok = try {
                lockController.acquire()
            } catch (e: Exception) {
                RedactedLog.warn(TAG, "Multicast lock acquisition failed")
                false
            }
            if (ok) {
                lockDepth = 1
                lockHeld.set(true)
            }
            ok
        }

    private fun platformRelease() {
        synchronized(lockExclusion) {
            if (lockDepth <= 0) return
            lockDepth--
            if (lockDepth == 0) {
                lockHeld.set(false)
                try {
                    lockController.release()
                } catch (e: Exception) {
                    RedactedLog.warn(TAG, "Multicast lock release failed")
                }
            }
        }
    }

    companion object {
        /** Literal local-network permission name: present in API 37, absent in API 36. */
        const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

        /** API level that introduces the local-network runtime permission. */
        const val LOCAL_NETWORK_SDK = 37

        /** Initial rule: below 37 granted; stored decision wins; else the live platform check. */
        fun resolveInitial(
            sdkInt: Int,
            stored: LanAccess?,
            platformGranted: Boolean,
        ): LanAccess =
            when {
                sdkInt < LOCAL_NETWORK_SDK -> LanAccess.Granted
                stored != null -> stored
                platformGranted -> LanAccess.Granted
                else -> LanAccess.NotDeclared
            }

        /** Prompt-result reducer. */
        fun reducePermissionResult(granted: Boolean): LanAccess =
            if (granted) LanAccess.Granted else LanAccess.Denied

        /** Settings-return reducer, both directions; below 37 always granted. */
        fun reduceSettingsReturn(
            current: LanAccess,
            sdkInt: Int,
            nowGranted: Boolean,
        ): LanAccess =
            when {
                sdkInt < LOCAL_NETWORK_SDK -> LanAccess.Granted
                nowGranted -> LanAccess.Granted
                current == LanAccess.Granted -> LanAccess.Denied
                else -> current
            }

        /** Pure prompt-firing rule behind Continue. */
        fun continueActionFor(access: LanAccess, sdkInt: Int): ContinueAction =
            when {
                access == LanAccess.Denied -> ContinueAction.OpenSettings
                access == LanAccess.Granted -> ContinueAction.Proceed
                sdkInt < LOCAL_NETWORK_SDK -> ContinueAction.Proceed
                else -> ContinueAction.FirePrompt
            }

        /** Pure diagnostic snapshot for one access/transport pair. */
        fun snapshotFor(access: LanAccess, networkType: NetworkType): LanGateSnapshot =
            LanGateSnapshot(
                access = access,
                networkType = networkType,
                code = resolveDiagnosticCode(access, networkType),
                scanned = false,
            )

        /**
         * Scoped-lock core, sync path: acquire, run, release when held. A
         * failed acquire still runs the block unprotected (the op must not
         * wedge); release is skipped when nothing was acquired.
         */
        fun <T> guardedRun(
            tryAcquire: () -> Boolean,
            doRelease: () -> Unit,
            block: () -> T,
        ): T {
            val acquired = runCatching { tryAcquire() }.getOrDefault(false)
            try {
                return block()
            } finally {
                if (acquired) runCatching { doRelease() }
            }
        }

        /** Scoped-lock core, suspend path: identical semantics, `finally` survives cancellation. */
        suspend fun <T> guardedRunSuspend(
            tryAcquire: () -> Boolean,
            doRelease: () -> Unit,
            block: suspend () -> T,
        ): T {
            val acquired = runCatching { tryAcquire() }.getOrDefault(false)
            try {
                return block()
            } finally {
                if (acquired) runCatching { doRelease() }
            }
        }

        /**
         * Builds the production gate from an application context. The gate
         * keeps the application context only indirectly through these
         * lambdas; no Activity is ever retained.
         */
        fun create(context: Context, scope: CoroutineScope): LanAccessGate {
            val app = context.applicationContext
            val wifiManager = app.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val connectivity =
                app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val sdk = android.os.Build.VERSION.SDK_INT
            return LanAccessGate(
                sdkInt = sdk,
                store = DataStoreLanAccessStore(app.lanGateDataStore),
                platformGranted = {
                    if (sdk < LOCAL_NETWORK_SDK) {
                        true
                    } else {
                        app.checkSelfPermission(LOCAL_NETWORK_PERMISSION) ==
                            PackageManager.PERMISSION_GRANTED
                    }
                },
                transportSource = { readTransport(connectivity) },
                lockController = AndroidMulticastLock(wifiManager),
                scope = scope,
            )
        }

        private fun readTransport(connectivity: ConnectivityManager?): NetworkType {
            val active = try {
                connectivity?.getNetworkCapabilities(connectivity.activeNetwork)
            } catch (e: Exception) {
                RedactedLog.warn(TAG, "Connectivity query failed")
                null
            } ?: return NetworkType.Unknown
            return when {
                active.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.Wifi
                active.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.Ethernet
                else -> NetworkType.Unknown
            }
        }
    }
}

private val Context.lanGateDataStore by preferencesDataStore(name = "lan_gate")

private object LanGateKeys {
    val ACCESS = stringPreferencesKey("lan_access")
    val ASKED = booleanPreferencesKey("lan_asked")
}

/** DataStore-backed retained decision. Runs on the app scope; never on the UI thread. */
class DataStoreLanAccessStore(
    private val dataStore: DataStore<Preferences>,
) : LanAccessStore {
    override suspend fun load(): StoredGateDecision {
        val prefs = dataStore.data.first()
        val access = prefs[LanGateKeys.ACCESS]?.let { name ->
            runCatching { LanAccess.valueOf(name) }.getOrNull()
        }
        return StoredGateDecision(access = access, asked = prefs[LanGateKeys.ASKED] == true)
    }

    override suspend fun save(decision: StoredGateDecision) {
        dataStore.edit { prefs ->
            if (decision.access == null) {
                prefs.remove(LanGateKeys.ACCESS)
            } else {
                prefs[LanGateKeys.ACCESS] = decision.access.name
            }
            prefs[LanGateKeys.ASKED] = decision.asked
        }
    }
}

/** The single production multicast-lock holder. */
private class AndroidMulticastLock(
    wifiManager: WifiManager?,
) : MulticastLockController {
    private val lock: WifiManager.MulticastLock? = try {
        wifiManager?.createMulticastLock("UniRemLan")?.apply { setReferenceCounted(false) }
    } catch (e: Exception) {
        RedactedLog.warn(TAG, "Multicast lock unavailable")
        null
    }

    override fun acquire(): Boolean {
        val held = lock ?: return false
        return try {
            if (!held.isHeld) held.acquire()
            true
        } catch (e: Exception) {
            RedactedLog.warn(TAG, "Multicast lock acquisition failed")
            false
        }
    }

    override fun release() {
        val held = lock ?: return
        try {
            if (held.isHeld) held.release()
        } catch (e: Exception) {
            RedactedLog.warn(TAG, "Multicast lock release failed")
        }
    }
}
