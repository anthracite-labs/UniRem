package com.unirem.tv.internal

import com.unirem.core.api.LanAccess
import com.unirem.core.api.NetPurpose
import com.unirem.core.api.PrivatePeer
import com.unirem.core.api.RedactedLog
import com.unirem.tv.api.ConnectionOutcome
import com.unirem.tv.api.ConnectionRequest
import com.unirem.tv.api.TvConnectionControl
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private const val TAG = "Connection"

/**
 * Socket-scoped session holder for the status surface (Story 1.7).
 *
 * One instance per open surface. The pin is validated during the TLS
 * handshake before any HTTP upgrade or token bytes; mismatch aborts with
 * [ConnectionOutcome.IdentityChanged] and no token is sent. The token rides
 * the WSS URL only after the pin matches. `TokenAuthSupport=false` uses
 * pin-only; a token-auth TV with no token returns [ConnectionOutcome.PairingRequired]
 * without a socket attempt. `ms.channel.unauthorized` maps to
 * [ConnectionOutcome.PairingRequired], never offline. Each attempt is bounded
 * (default 10s); network change is treated as a socket close via [close] and
 * the stale socket is never reused. There is no queue: [pendingWrites] stays
 * zero and [close] leaves no socket, no backoff, no background work.
 *
 * WSS-only, no trust-all manager, no WSS-to-WS fallback, no cloud. The
 * scoped client is the only place hostname checks relax (pin is the anchor).
 */
internal class ConnectionSession(
    private val gateCheck: () -> LanAccess = { LanAccess.Granted },
    private val channel: PinnedTlsChannel = PinnedTlsChannel(),
) : TvConnectionControl {

    @Volatile
    private var socket: WebSocket? = null

    @Volatile
    private var client: OkHttpClient? = null

    @Volatile
    private var messageListener: ((String) -> Unit)? = null

    override val pendingWrites: Int = 0

    override fun sendFrame(text: String): Boolean {
        val live = socket ?: return false
        return try {
            live.send(text)
        } catch (e: Exception) {
            RedactedLog.debug(TAG, "Command write missed the socket")
            false
        }
    }

    override fun setMessageListener(listener: ((String) -> Unit)?) {
        messageListener = listener
    }

    /**
     * One inbound channel frame. The installed tracker hook runs first; a
     * throwing hook never breaks latch matching. The frame is parsed once
     * inside the guard and the single parsed value feeds both latch
     * branches; unparseable frames are ignored after the hook. The text is
     * never logged: it may carry a token.
     */
    internal fun onChannelMessage(
        text: String,
        result: AtomicReference<ConnectionOutcome?>,
        latch: CountDownLatch,
    ) {
        try {
            messageListener?.invoke(text)
        } catch (e: Exception) {
            RedactedLog.debug(TAG, "Message listener ignored")
        }
        val parsed = try {
            ChannelEvents.parse(text)
        } catch (e: Exception) {
            return
        }
        when (parsed.name) {
            "ms.channel.connect" -> {
                if (result.compareAndSet(null, ConnectionOutcome.Connected(parsed.token))) {
                    latch.countDown()
                }
            }
            "ms.channel.unauthorized" -> {
                if (result.compareAndSet(null, ConnectionOutcome.PairingRequired)) {
                    latch.countDown()
                }
            }
        }
    }

    override suspend fun connect(request: ConnectionRequest): ConnectionOutcome {
        if (gateCheck() == LanAccess.Denied) return ConnectionOutcome.LanDenied
        val ip = request.ip.trim()
        if (!PrivatePeer.allowsHost(ip, ChannelUrl.PORT, NetPurpose.Channel)) {
            return ConnectionOutcome.Failed("target is not a private IP literal")
        }
        // Token-auth TV with no token is pairing-required; never retry with
        // an absent or stale token and never open a socket to probe it.
        if (request.tokenAuthSupport && request.token.isNullOrBlank()) {
            return ConnectionOutcome.PairingRequired
        }
        // Pin first: the TLS handshake validates the leaf SPKI before any
        // HTTP upgrade or token bytes exist on the wire.
        when (val probe = channel.verify(ip, request.pin)) {
            is PinnedTlsChannel.Probe.Mismatch -> return ConnectionOutcome.IdentityChanged
            is PinnedTlsChannel.Probe.Missing -> return ConnectionOutcome.IdentityChanged
            is PinnedTlsChannel.Probe.Failed -> return ConnectionOutcome.Offline
            is PinnedTlsChannel.Probe.Match -> Unit
            is PinnedTlsChannel.Probe.Captured -> Unit
        }
        return open(ip, request.pin, request.token, request.timeoutSeconds)
    }

    private fun open(ip: String, pin: String, token: String?, timeoutSeconds: Long): ConnectionOutcome {
        closeSocket()
        if (token != null && pin.isBlank()) return ConnectionOutcome.IdentityChanged
        val owned = channel.client(pin)
        client = owned
        val request = Request.Builder().url(ChannelUrl.build(ip, token)).build()
        val latch = CountDownLatch(1)
        val result = AtomicReference<ConnectionOutcome?>(null)
        val listener = object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                onChannelMessage(text, result, latch)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (result.compareAndSet(null, ConnectionOutcome.Offline)) {
                    latch.countDown()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (result.compareAndSet(null, ConnectionOutcome.Offline)) {
                    latch.countDown()
                }
            }
        }
        val opened: WebSocket = try {
            owned.newWebSocket(request, listener)
        } catch (e: Exception) {
            shutdown(owned)
            client = null
            return ConnectionOutcome.Offline
        }
        val bounded = timeoutSeconds.coerceIn(1L, 30L)
        val completed = try {
            latch.await(bounded, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
        if (!completed) {
            opened.cancel()
            shutdown(owned)
            client = null
            socket = null
            return ConnectionOutcome.Offline
        }
        return when (val outcome = result.get() ?: ConnectionOutcome.Offline) {
            is ConnectionOutcome.Connected -> {
                // Keep the open channel for the surface lifetime; close()
                // shuts it. Claiming Connected requires this open channel.
                socket = opened
                outcome
            }
            else -> {
                opened.close(1000, "attempt done")
                shutdown(owned)
                client = null
                socket = null
                outcome
            }
        }
    }

    /**
     * Closes the socket, stops backoff, and leaves no background work. The
     * outbound queue is empty by construction ([pendingWrites] is zero).
     */
    override fun close() {
        closeSocket()
        RedactedLog.debug(TAG, "Session closed")
    }

    private fun closeSocket() {
        try {
            socket?.close(1000, "surface closed")
        } catch (e: Exception) {
            RedactedLog.debug(TAG, "Socket close ignored")
        } finally {
            socket = null
        }
        try {
            client?.let { shutdown(it) }
        } catch (e: Exception) {
            RedactedLog.debug(TAG, "Client shutdown ignored")
        } finally {
            client = null
        }
    }

    private fun shutdown(owned: OkHttpClient) {
        owned.connectionPool.evictAll()
        owned.dispatcher.executorService.shutdown()
    }
}
