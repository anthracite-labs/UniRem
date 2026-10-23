package com.unirem.tv.internal

import com.unirem.core.api.LanAccess
import com.unirem.core.api.NetPurpose
import com.unirem.core.api.PrivatePeer
import com.unirem.core.api.RedactedLog
import com.unirem.tv.api.DiscoveryResult
import com.unirem.tv.api.TvCandidate
import com.unirem.tv.api.TvDiscoveryGateway
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

private const val TAG = "Discovery"

/**
 * SSDP discovery plus manual-IP identity read.
 *
 * The scan runs inside [lockRunner], which the `:app` gate supplies from its
 * single process-wide held scope; this class never owns a platform handle and
 * never mentions one. Every destination (multicast send, unicast reply host,
 * `/api/v2/` fetch, redirect target) passes the single `:core` [PrivatePeer]
 * predicate before any socket work. A `Denied` gate blocks scan and manual IP
 * identically with no network attempt.
 *
 * Results surface as found with no spinner; one TV never stops the scan; bad
 * `/api/v2/` rows are skipped with a diagnostic.
 */
internal class SsdpDiscovery(
    private val gateCheck: () -> LanAccess,
    private val lockRunner: (block: () -> Set<String>) -> Set<String>,
    private val infoClient: OkHttpClient = defaultInfoClient(),
    private val searchHostsFn: (() -> Set<String>)? = null,
) : TvDiscoveryGateway {

    override suspend fun scan(): DiscoveryResult {
        val access = gateCheck()
        if (access == LanAccess.Denied) return DiscoveryResult.LanDenied(GATE_DENIED_REASON)
        if (!PrivatePeer.allowsHost(SsdpPacket.MULTICAST_HOST, SsdpPacket.MULTICAST_PORT, NetPurpose.Scan)) {
            return DiscoveryResult.Failed("SSDP target refused by the local-peer rule")
        }
        val rawHosts = try {
            lockRunner { searchHostsFn?.invoke() ?: searchHosts() }
        } catch (e: Exception) {
            RedactedLog.warn(TAG, "SSDP scan failed")
            return DiscoveryResult.Failed(scrub(e.message ?: "scan failed"))
        }
        if (rawHosts.isEmpty()) return DiscoveryResult.NoDiscovery
        return readAll(rawHosts)
    }

    override suspend fun readManual(ip: String): DiscoveryResult {
        val access = gateCheck()
        if (access == LanAccess.Denied) return DiscoveryResult.LanDenied(GATE_DENIED_REASON)
        val host = ip.trim()
        if (host.isEmpty() || !PrivatePeer.allowsHost(host, INFO_PORT, NetPurpose.InfoRead)) {
            return DiscoveryResult.Failed("manual target is not a private IP literal")
        }
        val read = readInfo(host)
        val info = read.info
            ?: return DiscoveryResult.Failed(
                "no /api/v2/ answer at $host:$INFO_PORT" + (read.httpCode?.let { " (http $it)" } ?: ""),
                read.httpCode,
            )
        return DiscoveryResult.Found(listOf(info.toCandidate(host)))
    }

    private fun readAll(rawHosts: Set<String>): DiscoveryResult {
        val reads = rawHosts.map { host -> host to readInfoFiltered(host) }
        val targets = reads.mapNotNull { (host, read) -> read.info?.toCandidate(host) }
        if (targets.isNotEmpty()) return DiscoveryResult.Found(targets)
        val codes = reads.mapNotNull { it.second.httpCode }.distinct().sorted()
        val detail = if (codes.isNotEmpty()) " (http ${codes.joinToString(",")})" else ""
        return DiscoveryResult.Failed("SSDP found ${rawHosts.size} host(s) but /api/v2/ did not answer$detail")
    }

    private fun readInfoFiltered(host: String): InfoRead {
        if (!PrivatePeer.allowsHost(host, INFO_PORT, NetPurpose.InfoRead)) return InfoRead(null, null)
        return readInfo(host)
    }

    private fun searchHosts(): Set<String> {
        val hosts = LinkedHashSet<String>()
        DatagramSocket().use { socket ->
            socket.soTimeout = 900
            val payloads = listOf(
                SsdpPacket.mSearch().toByteArray(Charsets.UTF_8),
                SsdpPacket.mSearch("ssdp:all").toByteArray(Charsets.UTF_8),
            )
            val group = InetAddress.getByName(SsdpPacket.MULTICAST_HOST)
            repeat(3) {
                payloads.forEach { payload ->
                    if (!PrivatePeer.allowsHost(SsdpPacket.MULTICAST_HOST, SsdpPacket.MULTICAST_PORT, NetPurpose.Scan)) return@forEach
                    socket.send(DatagramPacket(payload, payload.size, group, SsdpPacket.MULTICAST_PORT))
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
                    val response = SsdpPacket.parseResponse(text) ?: continue
                    val host = SsdpPacket.hostFromLocation(response.location) ?: continue
                    if (PrivatePeer.allowsHost(host, INFO_PORT, NetPurpose.InfoRead)) hosts += host
                } catch (e: SocketTimeoutException) {
                    // Keep listening until the deadline; empty is NoDiscovery.
                }
            }
        }
        return hosts
    }

    private data class InfoRead(val info: ApiDevice?, val httpCode: Int?)

    private fun readInfo(host: String): InfoRead {
        val urlHost = if (host.contains(':')) "[$host]" else host
        val request = Request.Builder()
            .url("http://$urlHost:$INFO_PORT/api/v2/")
            .get()
            .build()
        return try {
            infoClient.newCall(request).execute().use { response ->
                if (response.isRedirect) {
                    val target = response.header("Location")
                    val targetHost = SsdpPacket.hostFromLocation(target)
                    if (targetHost == null || !PrivatePeer.allowsHost(targetHost, INFO_PORT, NetPurpose.Redirect)) {
                        return InfoRead(null, response.code)
                    }
                    return InfoRead(null, response.code)
                }
                if (!response.isSuccessful) return InfoRead(null, response.code)
                val body = response.body?.string() ?: return InfoRead(null, response.code)
                InfoRead(ApiInfoMapper.parse(body), response.code)
            }
        } catch (e: Exception) {
            InfoRead(null, null)
        }
    }

    private fun ApiDevice.toCandidate(host: String): TvCandidate = TvCandidate(
        ip = host,
        name = name,
        modelName = modelName,
        model = model,
        firmware = firmware,
        tokenAuthSupport = tokenAuthSupport,
        wifiMac = wifiMac,
        deviceId = deviceId,
        deviceIdField = deviceIdField,
        powerState = powerState,
        reportedNetworkType = reportedNetworkType,
    )

    private fun scrub(text: String): String {
        if (text.isEmpty()) return ""
        return text.replace(Regex("[?&]token=[^\\s]*"), "?token=[REDACTED]")
    }

    companion object {
        const val INFO_PORT = 8001
        const val GATE_DENIED_REASON = "Local network access denied. Both discovery and manual IP are blocked."

        fun defaultInfoClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .callTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }
}
