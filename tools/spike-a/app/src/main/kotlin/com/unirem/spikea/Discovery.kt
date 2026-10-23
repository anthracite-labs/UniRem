package com.unirem.spikea

import android.content.Context
import com.unirem.spikea.logic.ApiInfo
import com.unirem.spikea.logic.HostRules
import com.unirem.spikea.logic.IdentityRules
import com.unirem.spikea.logic.Redaction
import com.unirem.spikea.logic.Ssdp
import com.unirem.spikea.logic.StableIdentity
import com.unirem.spikea.logic.TvId
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

data class TvInfo(
    val name: String,
    val modelName: String,
    val model: String,
    val firmware: String,
    val tokenAuthSupport: Boolean,
    val wifiMac: String,
    val deviceId: String,
    val deviceIdField: String,
    val powerState: String,
    val reportedNetworkType: String,
) {
    val identity: StableIdentity = IdentityRules.resolve(deviceId, wifiMac)
    val tvId: TvId? = IdentityRules.tvId(identity)
    val modelLabel: String get() = modelName.ifBlank { model }
}

data class TvTarget(
    val ip: String,
    val info: TvInfo,
    val year: String,
    val region: String,
    val label: String,
) {
    val identity: StableIdentity get() = info.identity
    val tvId: TvId? get() = info.tvId
    val sessionOnly: Boolean get() = info.identity == StableIdentity.SessionOnly
    val displayName: String get() = label.ifBlank {
        info.name.ifBlank { info.modelLabel.ifBlank { ip } }
    }
}

sealed interface DiscoveryOutcome {
    data class Found(
        val targets: List<TvTarget>,
        val multicastHeld: Boolean,
        val droppedHosts: Int = 0,
    ) : DiscoveryOutcome
    data object NoDiscovery : DiscoveryOutcome
    data class LanDenied(val reason: String) : DiscoveryOutcome
    data class Failed(val reason: String) : DiscoveryOutcome
}

class Discovery(context: Context, private val lanGate: LanGate) {

    private val infoClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    fun scan(year: String, region: String, label: String): DiscoveryOutcome {
        val gate = lanGate.check()
        if (gate is LanGate.Status.Denied) return DiscoveryOutcome.LanDenied(gate.reason)

        return try {
            val rawHosts = lanGate.withMulticast { searchHosts() }
            val multicastHeld = lanGate.lastMulticastHeld
            if (rawHosts.isEmpty()) {
                DiscoveryOutcome.NoDiscovery
            } else {
                val hosts = rawHosts.mapNotNullTo(LinkedHashSet()) { HostRules.normalizeIp(it) }
                val dropped = rawHosts.size - hosts.size
                val reads = hosts.map { host -> host to readInfo(host) }
                val targets = reads.mapNotNull { (host, read) -> read.info?.toTarget(host, year, region, label) }
                if (targets.isEmpty()) {
                    val codes = reads.mapNotNull { it.second.httpCode }.distinct().sorted()
                    val detail = if (codes.isNotEmpty()) " (http ${codes.joinToString(",")})" else ""
                    val dropNote = if (dropped > 0) "; dropped $dropped non-IPv4 host(s)" else ""
                    DiscoveryOutcome.Failed("SSDP found ${rawHosts.size} host(s) but /api/v2/ did not answer$detail$dropNote")
                } else {
                    DiscoveryOutcome.Found(targets, multicastHeld, droppedHosts = dropped)
                }
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            DiscoveryOutcome.Failed("interrupted")
        } catch (e: Exception) {
            DiscoveryOutcome.Failed(Redaction.scrub(Redaction.withoutTokenParam(e.message ?: e.javaClass.simpleName)))
        }
    }

    fun readManual(ip: String, year: String, region: String, label: String): DiscoveryOutcome {
        val gate = lanGate.check()
        if (gate is LanGate.Status.Denied) return DiscoveryOutcome.LanDenied(gate.reason)

        val host = HostRules.normalizeIp(ip)
            ?: return DiscoveryOutcome.Failed("manual target is not an IPv4 literal")

        return try {
            val read = readInfo(host)
            val info = read.info
                ?: return DiscoveryOutcome.Failed(
                    "no /api/v2/ answer at $host:8001" + (read.httpCode?.let { " (http $it)" } ?: ""),
                )
            DiscoveryOutcome.Found(listOf(info.toTarget(host, year, region, label)), multicastHeld = false)
        } catch (e: Exception) {
            DiscoveryOutcome.Failed(Redaction.scrub(Redaction.withoutTokenParam(e.message ?: e.javaClass.simpleName)))
        }
    }

    private fun searchHosts(): Set<String> {
        val hosts = LinkedHashSet<String>()
        DatagramSocket().use { socket ->
            socket.soTimeout = 900
            val payloads = listOf(
                Ssdp.mSearch().toByteArray(Charsets.UTF_8),
                Ssdp.mSearch("ssdp:all").toByteArray(Charsets.UTF_8),
            )
            val group = InetAddress.getByName(Ssdp.MULTICAST_HOST)
            repeat(3) {
                payloads.forEach { payload ->
                    socket.send(DatagramPacket(payload, payload.size, group, Ssdp.MULTICAST_PORT))
                }
                Thread.sleep(250)
            }
            val deadline = System.currentTimeMillis() + 4_000
            val buffer = ByteArray(4096)
            while (System.currentTimeMillis() < deadline) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val text = String(packet.data, packet.offset, packet.length, Charsets.UTF_8)
                    val response = Ssdp.parseResponse(text) ?: continue
                    Ssdp.hostFromLocation(response.location)?.let { hosts += it }
                } catch (e: SocketTimeoutException) {
                    // Keep listening until the deadline; an empty result is NoDiscovery.
                }
            }
        }
        return hosts
    }

    private data class InfoRead(val info: TvInfo?, val httpCode: Int?)

    private fun readInfo(host: String): InfoRead {
        val request = Request.Builder()
            .url("http://$host:8001/api/v2/")
            .get()
            .build()
        return try {
            infoClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return InfoRead(null, response.code)
                val body = response.body?.string() ?: return InfoRead(null, response.code)
                InfoRead(parseInfo(body), response.code)
            }
        } catch (e: Exception) {
            InfoRead(null, null)
        }
    }

    private fun parseInfo(body: String): TvInfo? {
        val api = ApiInfo.parse(body) ?: return null
        return TvInfo(
            name = api.name,
            modelName = api.modelName,
            model = api.model,
            firmware = api.firmware,
            tokenAuthSupport = api.tokenAuthSupport,
            wifiMac = api.wifiMac,
            deviceId = api.deviceId,
            deviceIdField = api.deviceIdField,
            powerState = api.powerState,
            reportedNetworkType = api.reportedNetworkType,
        )
    }

    private fun TvInfo.toTarget(host: String, year: String, region: String, label: String): TvTarget =
        TvTarget(
            ip = host,
            info = this,
            year = year.trim(),
            region = region.trim(),
            label = label.trim(),
        )
}
