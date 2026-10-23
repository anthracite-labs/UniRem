package com.unirem.tv.internal

import com.unirem.core.api.LanAccess
import com.unirem.core.api.NetPurpose
import com.unirem.core.api.PrivatePeer
import com.unirem.core.api.RedactedLog
import com.unirem.tv.api.PairOutcome
import com.unirem.tv.api.TvCandidate
import com.unirem.tv.api.TvPairingControl
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.X509TrustManager
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private const val TAG = "Pairing"

/** Leaf SPKI pin verdicts. The pin itself is never logged. */
internal enum class PinVerdict {
    MATCH,
    MISMATCH,
    MISSING,
    INVALID,
}

internal object PinRules {

    private const val SHA256_BYTES = 32

    fun sha256Base64(spki: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(spki)
        return Base64.getEncoder().encodeToString(digest)
    }

    fun compare(stored: String?, leaf: String?): PinVerdict {
        val storedBytes = decode(stored)
        if (storedBytes == null) {
            return if (stored.isNullOrBlank()) PinVerdict.MISSING else PinVerdict.INVALID
        }
        val leafBytes = decode(leaf) ?: return PinVerdict.INVALID
        return if (MessageDigest.isEqual(storedBytes, leafBytes)) PinVerdict.MATCH else PinVerdict.MISMATCH
    }

    private fun decode(value: String?): ByteArray? {
        if (value.isNullOrBlank()) return null
        return try {
            Base64.getDecoder().decode(value.trim()).takeIf { it.size == SHA256_BYTES }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

/** The control channel is WSS only. No function here can produce a ws:// URL. */
internal object ChannelUrl {

    const val SCHEME = "wss"
    const val PORT = 8002
    const val APP_NAME = "UniRem"

    fun build(ip: String, token: String? = null): String {
        val hostPart = if (ip.contains(':')) "[$ip]" else ip
        val encodedName = URLEncoder.encode(
            Base64.getEncoder().encodeToString(APP_NAME.toByteArray(Charsets.UTF_8)),
            "UTF-8",
        )
        val base = "$SCHEME://$hostPart:$PORT/api/v2/channels/samsung.remote.control?name=$encodedName"
        return if (token.isNullOrEmpty()) {
            base
        } else {
            "$base&token=${URLEncoder.encode(token, "UTF-8")}"
        }
    }
}

/** One server frame from the Tizen channel. The raw text is never retained. */
internal data class ChannelEvent(val name: String, val token: String?)

internal object ChannelEvents {

    private val EVENT = Regex("\"(event|method)\"\\s*:\\s*\"([^\"]*)\"")
    private val TOKEN = Regex("\"token\"\\s*:\\s*\"([^\"]+)\"")

    fun parse(text: String): ChannelEvent {
        if (text.isEmpty() || text.length > 32_768) return ChannelEvent("", null)
        return try {
            val name = EVENT.find(text)?.groupValues?.get(2) ?: ""
            val token = TOKEN.find(text)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
            ChannelEvent(name, token)
        } catch (e: Exception) {
            ChannelEvent("", null)
        }
    }
}

/**
 * Trust-on-first-use pairing channel pinned to the leaf SPKI.
 *
 * First WSS opens token-less; the leaf pin is captured, then the channel
 * waits for `ms.channel.connect` with the token. Every later connect
 * validates the pinned leaf before attaching `&token=` and fails closed on
 * mismatch with no token sent. There is no trust-all manager and no WSS to WS
 * fallback; the hostname check is relaxed only inside the pinned client
 * because the pin, not the hostname, is the trust anchor.
 */
internal class PinnedTlsChannel(
    private val gateCheck: () -> LanAccess = { LanAccess.Granted },
) : TvPairingControl {

    sealed interface Probe {
        data class Captured(val pin: String) : Probe
        data class Match(val pin: String) : Probe
        data class Mismatch(val verdict: PinVerdict) : Probe
        data object Missing : Probe
        data class Failed(val reason: String) : Probe
    }

    override suspend fun pair(candidate: TvCandidate, timeoutSeconds: Long): PairOutcome {
        if (gateCheck() == LanAccess.Denied) return PairOutcome.Failed(GATE_DENIED)
        val ip = candidate.ip.trim()
        if (!PrivatePeer.allowsHost(ip, ChannelUrl.PORT, NetPurpose.Channel)) {
            return PairOutcome.Failed("target is not a private IP literal")
        }
        return when (val probe = capture(ip)) {
            is Probe.Captured -> connectAndClassify(ip, probe.pin, null, timeoutSeconds, candidate.tokenAuthSupport)
            is Probe.Failed -> PairOutcome.Failed(probe.reason)
            else -> PairOutcome.Failed("unexpected probe state during first pairing")
        }
    }

    fun capture(ip: String): Probe {
        if (!PrivatePeer.allowsHost(ip, ChannelUrl.PORT, NetPurpose.Channel)) {
            return Probe.Failed("target is not a private IP literal")
        }
        val trust = TofuCaptureTrust()
        return try {
            handshake(ip, trust)
            val pin = trust.capturedPin ?: return Probe.Failed("no leaf certificate")
            Probe.Captured(pin)
        } catch (e: Exception) {
            Probe.Failed(scrub(e.message ?: "TLS capture failed"))
        }
    }

    fun verify(ip: String, expectedPin: String): Probe {
        if (expectedPin.isBlank()) return Probe.Missing
        if (!PrivatePeer.allowsHost(ip, ChannelUrl.PORT, NetPurpose.Channel)) {
            return Probe.Failed("target is not a private IP literal")
        }
        val trust = PinnedTrust(expectedPin)
        return try {
            handshake(ip, trust)
            Probe.Match(expectedPin)
        } catch (e: SSLException) {
            if (hasCertificateCause(e)) Probe.Mismatch(PinVerdict.MISMATCH)
            else Probe.Failed(scrub(e.message ?: "TLS handshake failed"))
        } catch (e: Exception) {
            Probe.Failed(scrub(e.message ?: "TLS handshake failed"))
        }
    }

    /** WSS-only client pinned to the leaf SPKI. No cleartext spec is added. */
    fun client(pin: String): OkHttpClient {
        val trust = PinnedTrust(pin)
        val context = SSLContext.getInstance("TLS")
        context.init(null, arrayOf(trust), null)
        return OkHttpClient.Builder()
            .sslSocketFactory(context.socketFactory, trust)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(70, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }

    private fun connectAndClassify(
        ip: String,
        pin: String,
        token: String?,
        timeoutSeconds: Long,
        tokenAuthSupport: Boolean,
    ): PairOutcome {
        return when (val outcome = connect(ip, pin, token, timeoutSeconds)) {
            is ChannelOutcome.Connected -> classifyConnected(outcome.token, pin, tokenAuthSupport)
            ChannelOutcome.Unauthorized -> PairOutcome.Denied
            ChannelOutcome.TimedOut -> PairOutcome.TimedOut
            is ChannelOutcome.Closed -> PairOutcome.Failed("socket closed (${outcome.code})")
            is ChannelOutcome.Failed -> PairOutcome.Failed(outcome.reason)
        }
    }

    internal fun classifyConnected(token: String?, pin: String, tokenAuthSupport: Boolean): PairOutcome =
        if (tokenAuthSupport && token.isNullOrBlank()) PairOutcome.TokenMissing
        else PairOutcome.Paired(token, pin)

    internal sealed interface ChannelOutcome {
        data class Connected(val token: String?) : ChannelOutcome
        data object Unauthorized : ChannelOutcome
        data object TimedOut : ChannelOutcome
        data class Closed(val code: Int) : ChannelOutcome
        data class Failed(val reason: String) : ChannelOutcome
    }

    internal fun connect(ip: String, pin: String, token: String?, timeoutSeconds: Long): ChannelOutcome {
        if (!PrivatePeer.allowsHost(ip, ChannelUrl.PORT, NetPurpose.Channel)) {
            return ChannelOutcome.Failed("target is not a private IP literal")
        }
        // Pin first: a token-bearing URL is only built after the pin exists.
        if (token != null && pin.isBlank()) return ChannelOutcome.Failed("missing pin")
        val client = client(pin)
        val request = Request.Builder().url(ChannelUrl.build(ip, token)).build()
        val latch = CountDownLatch(1)
        val result = AtomicReference<ChannelOutcome?>(null)
        val socket: WebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                when (ChannelEvents.parse(text).name) {
                    "ms.channel.connect" -> {
                        val parsed = ChannelEvents.parse(text)
                        result.compareAndSet(null, ChannelOutcome.Connected(parsed.token))
                        latch.countDown()
                    }
                    "ms.channel.unauthorized" -> {
                        result.compareAndSet(null, ChannelOutcome.Unauthorized)
                        latch.countDown()
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                result.compareAndSet(null, ChannelOutcome.Failed(scrub(t.message ?: "channel failed")))
                latch.countDown()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                result.compareAndSet(null, ChannelOutcome.Closed(code))
                latch.countDown()
            }
        })
        val completed = try {
            latch.await(timeoutSeconds, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
        if (!completed) {
            socket.cancel()
            shutdown(client)
            return ChannelOutcome.TimedOut
        }
        socket.close(1000, "pair done")
        shutdown(client)
        return result.get() ?: ChannelOutcome.TimedOut
    }

    private fun handshake(ip: String, trust: X509TrustManager) {
        val context = SSLContext.getInstance("TLS")
        context.init(null, arrayOf(trust), null)
        Socket().use { raw ->
            raw.connect(InetSocketAddress(ip, ChannelUrl.PORT), 6_000)
            raw.soTimeout = 6_000
            val socket = context.socketFactory.createSocket(raw, ip, ChannelUrl.PORT, true)
                as javax.net.ssl.SSLSocket
            socket.use {
                it.startHandshake()
                val leaf = it.session.peerCertificates.firstOrNull() as? X509Certificate
                    ?: throw CertificateException("no leaf certificate")
                trust.checkServerTrusted(arrayOf(leaf), it.session.protocol)
            }
        }
    }

    private fun hasCertificateCause(throwable: Throwable): Boolean {
        var cause: Throwable? = throwable
        while (cause != null) {
            if (cause is CertificateException) return true
            cause = cause.cause
        }
        return false
    }

    private fun shutdown(client: OkHttpClient) {
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
    }

    private fun scrub(text: String): String {
        if (text.isEmpty()) return ""
        return text.replace(Regex("[?&]token=[^\\s]*"), "?token=[REDACTED]")
            .replace(Regex("(?i)\\b[0-9a-f]{2}(?::[0-9a-f]{2}){5}\\b"), "xx:xx:xx:xx:xx:xx")
            .take(280)
    }

    companion object {
        const val GATE_DENIED = "Local network access denied."
    }
}

private class TofuCaptureTrust : X509TrustManager {

    @Volatile
    var capturedPin: String? = null
        private set

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val leaf = chain?.firstOrNull() ?: throw CertificateException("empty certificate chain")
        val pin = PinRules.sha256Base64(leaf.publicKey.encoded)
        val current = capturedPin
        if (current == null) {
            capturedPin = pin
        } else if (PinRules.compare(current, pin) != PinVerdict.MATCH) {
            throw CertificateException("leaf changed during capture")
        }
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        throw CertificateException("client certificates are not accepted")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}

private class PinnedTrust(private val expectedPin: String) : X509TrustManager {

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val leaf = chain?.firstOrNull() ?: throw CertificateException("empty certificate chain")
        val actualPin = PinRules.sha256Base64(leaf.publicKey.encoded)
        if (PinRules.compare(expectedPin, actualPin) != PinVerdict.MATCH) {
            throw CertificateException("SPKI pin mismatch")
        }
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        throw CertificateException("client certificates are not accepted")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
