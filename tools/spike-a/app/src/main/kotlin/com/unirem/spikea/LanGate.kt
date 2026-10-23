package com.unirem.spikea

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager

data class NetworkSnapshot(val type: String, val detail: String)

/**
 * One process-wide MulticastLock holder and the local-network gate. Discovery
 * and manual-IP both ask this object before touching the network, so a denial
 * is recorded as LanDenied rather than as a TV that did not answer.
 */
class LanGate(context: Context) {

    sealed interface Status {
        data object Granted : Status
        data class Denied(val reason: String) : Status
    }

    private val appContext = context.applicationContext
    private val connectivity =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val multicastLock: WifiManager.MulticastLock? by lazy {
        wifi?.createMulticastLock(MULTICAST_LOCK_TAG)?.apply { setReferenceCounted(false) }
    }

    @Volatile
    var lastMulticastHeld: Boolean = false
        private set

    private var held = false

    fun check(): Status {
        if (!hasLanTransport()) return Status.Denied("no LAN transport (Wi-Fi/Ethernet)")
        if (localNetworkPermissionDeclared() && !localNetworkPermissionGranted()) {
            return Status.Denied("local-network permission denied")
        }
        return Status.Granted
    }

    @Synchronized
    fun <T> withMulticast(block: () -> T): T {
        val acquired = acquire()
        lastMulticastHeld = acquired
        return try {
            block()
        } finally {
            if (acquired) release()
        }
    }

    fun networkSnapshot(): NetworkSnapshot {
        val activeCapabilities = connectivity?.activeNetwork?.let { network ->
            connectivity.getNetworkCapabilities(network)
        }
        val wifiActive = activeCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val ethernetActive = activeCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        return when {
            wifiActive -> NetworkSnapshot("Wifi", wifiDetail())
            ethernetActive -> NetworkSnapshot("Ethernet", "Ethernet")
            anyWifiTransport() -> NetworkSnapshot("Wifi", wifiDetail())
            else -> NetworkSnapshot("Unknown", "Unknown")
        }
    }

    private fun hasLanTransport(): Boolean {
        val manager = connectivity ?: return false
        val active = manager.activeNetwork?.let { manager.getNetworkCapabilities(it) }
        if (active?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
            active?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        ) {
            return true
        }
        return manager.allNetworks.any { network ->
            val capabilities = manager.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        }
    }

    private fun anyWifiTransport(): Boolean {
        val manager = connectivity ?: return false
        return manager.allNetworks.any { network ->
            manager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }

    @Suppress("DEPRECATION")
    private fun localNetworkPermissionDeclared(): Boolean {
        return try {
            val info = appContext.packageManager.getPackageInfo(
                appContext.packageName,
                PackageManager.GET_PERMISSIONS,
            )
            info.requestedPermissions?.contains(LOCAL_NETWORK_PERMISSION) == true
        } catch (e: Exception) {
            false
        }
    }

    private fun localNetworkPermissionGranted(): Boolean =
        appContext.checkSelfPermission(LOCAL_NETWORK_PERMISSION) == PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    private fun wifiDetail(): String {
        val frequency = try {
            wifi?.connectionInfo?.frequency ?: -1
        } catch (e: Exception) {
            -1
        }
        return when (frequency) {
            in 2400..2500 -> "Wi-Fi 2.4 GHz"
            in 4900..5900 -> "Wi-Fi 5 GHz"
            in 5925..7125 -> "Wi-Fi 6 GHz"
            else -> "Wi-Fi (band unknown)"
        }
    }

    private fun acquire(): Boolean {
        if (held) return true
        val lock = multicastLock ?: return false
        return try {
            lock.acquire()
            held = true
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun release() {
        if (!held) return
        try {
            multicastLock?.release()
        } catch (e: Exception) {
            // Already released; the lock is reference-counted off.
        }
        held = false
    }

    companion object {
        private const val MULTICAST_LOCK_TAG = "unirem-spike-a"
        private const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
    }
}
