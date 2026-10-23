package com.unirem.tv

import com.unirem.core.api.LanAccess
import com.unirem.tv.api.DiscoveryResult
import com.unirem.tv.internal.SsdpDiscovery
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Discovery boundary tests with a stubbed HTTP layer: the gate, the
 * private-literal refusal, and read aggregation run without sockets.
 * Real SSDP multicast stays on owned hardware.
 */
class SsdpDiscoveryTest {

    private fun deviceJson(id: String): String =
        "{\"device\":{\"id\":\"$id\",\"name\":\"TV\",\"modelName\":\"M\",\"model\":\"M\"," +
            "\"softwareVersion\":\"FW1\",\"TokenAuthSupport\":true," +
            "\"wifiMac\":\"AA:BB:CC:DD:EE:01\",\"PowerState\":\"on\",\"networkType\":\"wireless\"}}"

    private class StubClient(
        private val calls: AtomicInteger,
        private val handler: (String) -> Triple<Int, String?, String>,
    ) {
        val client: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(
                Interceptor { chain ->
                    calls.incrementAndGet()
                    val url = chain.request().url.toString()
                    val (code, location, body) = handler(url)
                    val builder = Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(code)
                        .message("stub")
                        .body(body.toResponseBody("application/json".toMediaType()))
                    if (location != null) builder.header("Location", location)
                    builder.build()
                },
            )
            .build()
    }

    private fun discovery(
        gate: LanAccess = LanAccess.Granted,
        hosts: Set<String> = emptySet(),
        handler: (String) -> Triple<Int, String?, String> = { Triple(200, null, deviceJson("d1")) },
        calls: AtomicInteger = AtomicInteger(),
    ): Pair<SsdpDiscovery, AtomicInteger> {
        val stub = StubClient(calls, handler)
        return SsdpDiscovery(
            gateCheck = { gate },
            lockRunner = { block -> block() },
            infoClient = stub.client,
            searchHostsFn = { hosts },
        ) to calls
    }

    @Test
    fun deniedGateBlocksScanWithNoSocketWork(): Unit = runBlocking {
        var entered = false
        val gate = SsdpDiscovery(
            gateCheck = { LanAccess.Denied },
            lockRunner = {
                entered = true
                emptySet()
            },
        )
        assertTrue(gate.scan() is DiscoveryResult.LanDenied)
        assertEquals(false, entered)
    }

    @Test
    fun deniedGateBlocksManualIp(): Unit = runBlocking {
        val (gate, _) = discovery(gate = LanAccess.Denied)
        assertTrue(gate.readManual("192.168.1.20") is DiscoveryResult.LanDenied)
    }

    @Test
    fun publicManualHostRefusedBeforeRequest(): Unit = runBlocking {
        val (gate, calls) = discovery()
        assertTrue(gate.readManual("8.8.8.8") is DiscoveryResult.Failed)
        assertEquals(0, calls.get())
    }

    @Test
    fun manualReadReturnsTheCard(): Unit = runBlocking {
        val (gate, calls) = discovery(handler = { Triple(200, null, deviceJson("manual-1")) })
        val result = gate.readManual("192.168.1.20")
        assertTrue(result is DiscoveryResult.Found)
        assertEquals("192.168.1.20", (result as DiscoveryResult.Found).targets.single().ip)
        assertEquals(1, calls.get())
    }

    @Test
    fun mixedGoodAndBadRowsStillFindTheGoodTv(): Unit = runBlocking {
        val (gate, _) = discovery(
            hosts = setOf("192.168.1.2", "192.168.1.3"),
            handler = { url ->
                if (url.contains("192.168.1.2")) Triple(200, null, deviceJson("good-1"))
                else Triple(500, null, "")
            },
        )
        val result = gate.scan()
        assertTrue(result is DiscoveryResult.Found)
        assertEquals(listOf("192.168.1.2"), (result as DiscoveryResult.Found).targets.map { it.ip })
    }

    @Test
    fun allBadRowsYieldFailedWithCodeDetail(): Unit = runBlocking {
        val (gate, _) = discovery(
            hosts = setOf("192.168.1.9"),
            handler = { Triple(500, null, "") },
        )
        val result = gate.scan()
        assertTrue(result is DiscoveryResult.Failed)
        assertTrue((result as DiscoveryResult.Failed).reason.contains("500"))
    }

    @Test
    fun redirectIsCheckedButNeverFollowed(): Unit = runBlocking {
        val (gate, calls) = discovery(
            handler = { Triple(301, "http://192.168.1.2:8001/api/v2/", deviceJson("d1")) },
        )
        val result = gate.readManual("192.168.1.20")
        assertTrue(result is DiscoveryResult.Failed)
        assertEquals(1, calls.get())
    }
}
