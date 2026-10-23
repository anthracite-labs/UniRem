package com.unirem.spikea

import com.unirem.spikea.logic.PinRules
import com.unirem.spikea.logic.PinVerdict
import com.unirem.spikea.logic.Redaction
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Trust-on-first-use pinned to the leaf SPKI.
 *
 * capture() runs one token-less handshake with a manager that accepts exactly
 * the first leaf it sees and rejects any change; that pin is then held in memory
 * and used for the token-less pairing channel. verify() runs a handshake with a
 * manager that accepts only the stored pin. A token-bearing channel is only
 * created after verify() returns Match. There is no trust-all manager on any
 * token path, no WSS-to-WS fallback, and the hostname verifier is relaxed only
 * inside client() because the pin, not the hostname, is the trust anchor.
 */
class PinnedTls {

    sealed interface Probe {
        data class Captured(val pin: String) : Probe
        data class Match(val pin: String) : Probe
        data class Mismatch(val verdict: PinVerdict) : Probe
        data object Missing : Probe
        data class Failed(val reason: String) : Probe
    }

    fun capture(ip: String): Probe {
        val trust = TofuCaptureTrust()
        return try {
            handshake(ip, trust)
            val pin = trust.capturedPin ?: return Probe.Failed("no leaf certificate")
            Probe.Captured(pin)
        } catch (e: Exception) {
            Probe.Failed(Redaction.scrub(e.message ?: e.javaClass.simpleName))
        }
    }

    fun verify(ip: String, expectedPin: String): Probe {
        if (expectedPin.isBlank()) return Probe.Missing
        val trust = PinnedTrust(expectedPin)
        return try {
            handshake(ip, trust)
            Probe.Match(expectedPin)
        } catch (e: SSLException) {
            if (hasCertificateCause(e)) {
                Probe.Mismatch(PinVerdict.MISMATCH)
            } else {
                Probe.Failed(Redaction.scrub(e.message ?: "TLS handshake failed"))
            }
        } catch (e: Exception) {
            Probe.Failed(Redaction.scrub(e.message ?: e.javaClass.simpleName))
        }
    }

    /** A WSS-only client pinned to the leaf SPKI. No cleartext spec is added. */
    fun client(pin: String): OkHttpClient {
        val trust = PinnedTrust(pin)
        val context = SSLContext.getInstance("TLS")
        context.init(null, arrayOf<TrustManager>(trust), null)
        return OkHttpClient.Builder()
            .sslSocketFactory(context.socketFactory, trust)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }

    private fun handshake(ip: String, trust: X509TrustManager) {
        val context = SSLContext.getInstance("TLS")
        context.init(null, arrayOf<TrustManager>(trust), null)
        Socket().use { raw ->
            raw.connect(InetSocketAddress(ip, PORT), 6_000)
            raw.soTimeout = 6_000
            val socket = context.socketFactory.createSocket(raw, ip, PORT, true) as SSLSocket
            socket.use {
                it.startHandshake()
                val leaf = it.session.peerCertificates.firstOrNull() as? X509Certificate
                    ?: throw CertificateException("no leaf certificate")
                // The handshake already invoked the manager. This explicit call
                // keeps the pin check visible to a reader auditing the path.
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

    private companion object {
        const val PORT = 8002
    }
}

/**
 * TOFU capture only. It is never used on a token-bearing channel; it pins the
 * first leaf and rejects a changed leaf within the same capture handshake.
 */
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

/** Accepts only the stored leaf SPKI pin. */
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
