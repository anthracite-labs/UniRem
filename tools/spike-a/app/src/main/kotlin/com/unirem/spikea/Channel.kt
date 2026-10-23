package com.unirem.spikea

import com.unirem.spikea.logic.ChannelEvents
import com.unirem.spikea.logic.ChannelUrl
import com.unirem.spikea.logic.HostRules
import com.unirem.spikea.logic.Redaction
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * One WSS attempt per call. No queue, no automatic retry, no replay. Silence is
 * its own outcome (TimedOut), never a failure claim. The raw message text is
 * never logged because it can carry a token.
 */
class Channel(private val pinnedTls: PinnedTls) {

    sealed interface Outcome {
        data class Connected(val token: String?) : Outcome
        data object Unauthorized : Outcome
        data object TimedOut : Outcome
        data class Closed(val code: Int, val reason: String) : Outcome
        data class Failed(val reason: String) : Outcome
    }

    fun connect(
        ip: String,
        pin: String,
        token: String?,
        timeoutSeconds: Long,
        onEvent: (String) -> Unit,
    ): Outcome {
        val host = HostRules.normalizeIp(ip) ?: return Outcome.Failed("bad ip")
        val client = pinnedTls.client(pin)
        val request = Request.Builder().url(ChannelUrl.build(host, token)).build()
        val latch = CountDownLatch(1)
        val result = AtomicReference<Outcome?>(null)

        val webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onEvent("channel open")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val event = ChannelEvents.parse(text)
                when (event.name) {
                    "ms.channel.connect" -> {
                        result.compareAndSet(null, Outcome.Connected(event.token))
                        latch.countDown()
                    }
                    "ms.channel.unauthorized" -> {
                        result.compareAndSet(null, Outcome.Unauthorized)
                        latch.countDown()
                    }
                    "ms.channel.ready" -> onEvent("channel ready")
                    "ms.channel.close" -> onEvent("channel close event")
                    else -> if (event.name.isNotBlank()) {
                        onEvent("event: ${Redaction.scrub(event.name)}")
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                result.compareAndSet(
                    null,
                    Outcome.Failed(Redaction.scrub(Redaction.withoutTokenParam(t.message ?: t.javaClass.simpleName))),
                )
                latch.countDown()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                result.compareAndSet(null, Outcome.Closed(code, Redaction.scrub(reason)))
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
            webSocket.cancel()
            shutdown(client)
            return Outcome.TimedOut
        }

        webSocket.close(1000, "spike done")
        shutdown(client)
        return result.get() ?: Outcome.TimedOut
    }

    private fun shutdown(client: OkHttpClient) {
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
    }
}
