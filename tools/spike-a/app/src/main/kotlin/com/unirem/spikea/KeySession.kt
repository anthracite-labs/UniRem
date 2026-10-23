package com.unirem.spikea

import com.unirem.spikea.logic.ChannelEvents
import com.unirem.spikea.logic.ChannelUrl
import com.unirem.spikea.logic.HostRules
import com.unirem.spikea.logic.KeyTransport
import com.unirem.spikea.logic.Redaction
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * The long-lived sibling of [Channel]. `Channel` proved the trust path with a
 * one-shot latch-then-close; its shape is wrong for a held socket, so this
 * class reuses [PinnedTls], [ChannelUrl], [ChannelEvents], and the vault
 * entry the harness passes in, and keeps the token-bearing socket open across
 * key sends.
 *
 * A socket write is never a TV action: [send] reports only whether bytes were
 * handed to the socket. Evidence correlation (machine event, state probe,
 * operator confirmation) lives in the harness gesture tracker. The raw
 * message text is never logged because URLs and payloads must not leak.
 */
class KeySession(private val pinnedTls: PinnedTls) : KeyTransport {

    sealed interface OpenOutcome {
        data object Opened : OpenOutcome
        data object Unauthorized : OpenOutcome
        data object TimedOut : OpenOutcome
        data class Closed(val code: Int, val reason: String) : OpenOutcome
        data class Failed(val reason: String) : OpenOutcome
    }

    interface Listener {
        fun onMachineEvent(name: String)
        fun onSocketClosed(code: Int, reason: String)
    }

    @Volatile
    var listener: Listener? = null

    private val lock = Any()
    private var client: OkHttpClient? = null
    private var socket: WebSocket? = null

    /** True once ms.channel.connect arrived and before any close. */
    private val held = AtomicBoolean(false)

    override fun isOpen(): Boolean = held.get()

    fun open(
        ip: String,
        pin: String,
        token: String?,
        timeoutSeconds: Long,
        onEvent: (String) -> Unit,
    ): OpenOutcome {
        // A second open while held would orphan the prior socket.
        if (held.get()) return OpenOutcome.Failed("already open")
        // Fail closed before a token is attached: no pin, no channel.
        if (pin.isBlank()) return OpenOutcome.Failed("stored pin missing; fail closed before token attach")
        val host = HostRules.normalizeIp(ip) ?: return OpenOutcome.Failed("bad ip")
        val client = pinnedTls.client(pin)
        val request = Request.Builder().url(ChannelUrl.build(host, token)).build()
        val latch = CountDownLatch(1)
        val result = AtomicReference<OpenOutcome?>(null)
        // Settles once: the first settlement wins, and a straggler arriving
        // after the timeout is ignored instead of parking held with no socket.
        val done = AtomicBoolean(false)

        val webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onEvent("key channel open")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val event = ChannelEvents.parse(text)
                when (event.name) {
                    "ms.channel.connect" -> {
                        if (done.compareAndSet(false, true)) {
                            if (result.compareAndSet(null, OpenOutcome.Opened)) {
                                held.set(true)
                            }
                            latch.countDown()
                        }
                    }
                    "ms.channel.unauthorized" -> {
                        if (done.compareAndSet(false, true)) {
                            result.compareAndSet(null, OpenOutcome.Unauthorized)
                            latch.countDown()
                        }
                    }
                    "ms.channel.ready" -> onEvent("key channel ready")
                    "ms.channel.close" -> onEvent("key channel close event")
                    else -> if (event.name.isNotBlank()) {
                        val scrubbed = Redaction.scrub(event.name)
                        onEvent("key event: $scrubbed")
                        // Only forwarded while the session is held; a late event
                        // after close is never attributed to a newer press.
                        if (held.get()) listener?.onMachineEvent(scrubbed)
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (done.compareAndSet(false, true)) {
                    result.compareAndSet(
                        null,
                        OpenOutcome.Failed(Redaction.scrub(Redaction.withoutTokenParam(t.message ?: t.javaClass.simpleName))),
                    )
                    latch.countDown()
                } else if (held.getAndSet(false)) {
                    listener?.onSocketClosed(-1, Redaction.scrub(t.message ?: "socket failed"))
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (done.compareAndSet(false, true)) {
                    result.compareAndSet(null, OpenOutcome.Closed(code, Redaction.scrub(reason)))
                    latch.countDown()
                } else if (held.getAndSet(false)) {
                    listener?.onSocketClosed(code, Redaction.scrub(reason))
                }
            }
        })

        val completed = try {
            latch.await(timeoutSeconds, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }

        if (!completed) {
            done.set(true)
            held.set(false)
            webSocket.cancel()
            shutdown(client)
            return OpenOutcome.TimedOut
        }

        return when (val outcome = result.get() ?: OpenOutcome.TimedOut) {
            is OpenOutcome.Opened -> {
                synchronized(lock) {
                    this.client = client
                    this.socket = webSocket
                }
                outcome
            }
            else -> {
                webSocket.close(1000, "spike key probe done")
                shutdown(client)
                outcome
            }
        }
    }

    /**
     * Hands one frame to the socket. True means the bytes were accepted by
     * the socket, never that the TV acted.
     */
    override fun send(frameJson: String): Boolean {
        if (!held.get()) return false
        val webSocket = synchronized(lock) { socket } ?: return false
        return try {
            webSocket.send(frameJson)
        } catch (e: Exception) {
            false
        }
    }

    override fun close(reason: String) {
        if (!held.getAndSet(false)) {
            synchronized(lock) {
                socket?.cancel()
                client?.let { shutdown(it) }
                socket = null
                client = null
            }
            return
        }
        val webSocket = synchronized(lock) { socket }
        val current = synchronized(lock) { client }
        try {
            webSocket?.close(1000, Redaction.scrub(reason).take(120))
        } catch (e: Exception) {
            // Best effort; the session is already marked not-held.
        }
        synchronized(lock) {
            socket = null
            client = null
        }
        current?.let { shutdown(it) }
    }

    private fun shutdown(client: OkHttpClient) {
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
    }
}
