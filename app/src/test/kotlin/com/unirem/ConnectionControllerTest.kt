package com.unirem

import com.unirem.connection.BannerKind
import com.unirem.connection.ConnectionController
import com.unirem.connection.TargetSession
import com.unirem.connection.TargetSessionHolder
import com.unirem.core.api.IdentityRules
import com.unirem.core.api.TvId
import com.unirem.tv.api.ConnectionOutcome
import com.unirem.tv.api.ConnectionRequest
import com.unirem.tv.api.TvConnectionControl
import com.unirem.tv.channel.ConnectionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fake socket-scoped session: scripted outcomes, records every IP attempted. */
class FakeConnectionSession(
    private val outcomes: List<ConnectionOutcome>,
) : TvConnectionControl {
    val attemptedIps = mutableListOf<String>()
    var closes = 0
    private var calls = 0
    override val pendingWrites: Int = 0

    override suspend fun connect(request: ConnectionRequest): ConnectionOutcome {
        attemptedIps += request.ip
        val outcome = outcomes[calls.coerceAtMost(outcomes.size - 1)]
        calls++
        return outcome
    }

    override fun close() {
        closes++
    }
}

class ConnectionControllerTest {

    private fun tvId(deviceId: String): TvId =
        IdentityRules.tvId(IdentityRules.resolve(deviceId, null))!!

    @Test
    fun unauthorizedEndsPairingRequiredWithNoRetry() = runBlocking {
        val session = FakeConnectionSession(listOf(ConnectionOutcome.PairingRequired))
        val controller = ConnectionController(
            session = session,
            schedule = { 0L },
        )
        controller.run(tvId("device-1"), "192.168.1.20", "pin", "token", true)
        val snap = controller.snapshot.value
        assertEquals(ConnectionState.Reconnecting, snap.state)
        assertEquals(BannerKind.PairingRequired, snap.banner)
        assertEquals(listOf("192.168.1.20"), session.attemptedIps)
    }

    @Test
    fun identityChangedAbortsBeforeAnyRetry() = runBlocking {
        val session = FakeConnectionSession(listOf(ConnectionOutcome.IdentityChanged))
        val controller = ConnectionController(
            session = session,
            schedule = { 0L },
        )
        controller.run(tvId("device-1"), "192.168.1.20", "pin", "token", true)
        assertEquals(BannerKind.IdentityChanged, controller.snapshot.value.banner)
        assertEquals(1, session.attemptedIps.size)
    }

    @Test
    fun freshIdentityMismatchNeverOpensSocket() = runBlocking {
        val session = FakeConnectionSession(listOf(ConnectionOutcome.Connected(null)))
        val controller = ConnectionController(
            session = session,
            schedule = { 0L },
            freshIdentityOf = { tvId("device-other") },
        )
        controller.run(tvId("device-1"), "192.168.1.20", "pin", "token", true)
        assertEquals(BannerKind.IdentityChanged, controller.snapshot.value.banner)
        assertTrue(session.attemptedIps.isEmpty())
    }

    @Test
    fun socketLossExhaustsToOffline() = runBlocking {
        val session = FakeConnectionSession(listOf(ConnectionOutcome.Offline))
        val controller = ConnectionController(
            session = session,
            schedule = { 0L },
        )
        controller.run(tvId("device-1"), "192.168.1.20", "pin", null, false)
        val snap = controller.snapshot.value
        assertEquals(ConnectionState.Offline, snap.state)
        assertEquals(BannerKind.Offline, snap.banner)
    }

    @Test
    fun retryTriesStoredIpThenOneSameTvScan() = runBlocking {
        val session = FakeConnectionSession(
            listOf(ConnectionOutcome.Offline, ConnectionOutcome.Connected(null)),
        )
        val controller = ConnectionController(
            session = session,
            schedule = { 0L },
            rescanSameTv = { "192.168.1.21" },
        )
        val reached = controller.retry(tvId("device-1"), "192.168.1.20", "pin", null, false)
        assertEquals("192.168.1.21", reached)
        assertEquals(listOf("192.168.1.20", "192.168.1.21"), session.attemptedIps)
    }

    @Test
    fun retryNeverConnectsDifferentTv() = runBlocking {
        val session = FakeConnectionSession(listOf(ConnectionOutcome.Offline))
        var scans = 0
        val controller = ConnectionController(
            session = session,
            schedule = { 0L },
            // The lambda contract returns one same-TvId IP only; a null here
            // means no same-TvId host was found, so no second attempt runs.
            rescanSameTv = {
                scans++
                null
            },
        )
        controller.retry(tvId("device-1"), "192.168.1.20", "pin", null, false)
        assertEquals(1, scans)
        assertEquals(listOf("192.168.1.20"), session.attemptedIps)
    }

    @Test
    fun notNowStopsRetryAndMovesOffline() = runBlocking {
        val session = FakeConnectionSession(listOf(ConnectionOutcome.Offline))
        val controller = ConnectionController(
            session = session,
            schedule = { 0L },
        )
        controller.notNow()
        assertEquals(ConnectionState.Offline, controller.snapshot.value.state)
        assertEquals(BannerKind.Offline, controller.snapshot.value.banner)
    }

    @Test
    fun stopClosesSocketAndLeavesNoQueue() = runBlocking {
        val session = FakeConnectionSession(listOf(ConnectionOutcome.Connected(null)))
        val controller = ConnectionController(
            session = session,
            schedule = { 0L },
        )
        controller.run(tvId("device-1"), "192.168.1.20", "pin", null, false)
        controller.stop()
        assertEquals(1, session.closes)
        assertEquals(0, controller.pendingWrites)
        assertEquals(0, session.pendingWrites)
    }

    @Test
    fun lanDeniedLandsOfflineWithoutBackoff() = runBlocking {
        val session = FakeConnectionSession(listOf(ConnectionOutcome.LanDenied))
        var delays = 0
        val controller = ConnectionController(
            session = session,
            schedule = {
                delays++
                0L
            },
        )
        controller.run(tvId("device-1"), "192.168.1.20", "pin", "token", true)
        val snap = controller.snapshot.value
        assertEquals(ConnectionState.Offline, snap.state)
        assertEquals(BannerKind.Offline, snap.banner)
        assertEquals(listOf("192.168.1.20"), session.attemptedIps)
        assertEquals(0, delays)
    }

    @Test
    fun networkChangeFromIdleMovesToReconnecting() = runBlocking {
        val session = FakeConnectionSession(listOf(ConnectionOutcome.Offline))
        val controller = ConnectionController(
            session = session,
            schedule = { 0L },
        )
        assertEquals(BannerKind.None, controller.snapshot.value.banner)
        controller.onNetworkChanged()
        val snap = controller.snapshot.value
        assertEquals(ConnectionState.Reconnecting, snap.state)
        assertEquals(BannerKind.Reconnecting, snap.banner)
        assertEquals(1, session.closes)
    }

    @Test
    fun networkChangeKeepsTerminalBanners() = runBlocking {
        val session = FakeConnectionSession(listOf(ConnectionOutcome.PairingRequired))
        val controller = ConnectionController(
            session = session,
            schedule = { 0L },
        )
        controller.run(tvId("device-1"), "192.168.1.20", "pin", "token", true)
        assertEquals(BannerKind.PairingRequired, controller.snapshot.value.banner)
        controller.onNetworkChanged()
        assertEquals(BannerKind.PairingRequired, controller.snapshot.value.banner)
    }

    @Test
    fun retryWithPairingRequiredReturnsNullWithNoSecondAttempt() = runBlocking {
        val session = FakeConnectionSession(listOf(ConnectionOutcome.PairingRequired))
        var scans = 0
        val controller = ConnectionController(
            session = session,
            schedule = { 0L },
            rescanSameTv = {
                scans++
                "192.168.1.21"
            },
        )
        val reached = controller.retry(tvId("device-1"), "192.168.1.20", "pin", "token", true)
        assertEquals(null, reached)
        assertEquals(BannerKind.PairingRequired, controller.snapshot.value.banner)
        assertEquals(listOf("192.168.1.20"), session.attemptedIps)
        assertEquals(0, scans)
    }

    @Test
    fun retryWithIdentityChangedReturnsNullWithNoSecondAttempt() = runBlocking {
        val session = FakeConnectionSession(listOf(ConnectionOutcome.IdentityChanged))
        var scans = 0
        val controller = ConnectionController(
            session = session,
            schedule = { 0L },
            rescanSameTv = {
                scans++
                "192.168.1.21"
            },
        )
        val reached = controller.retry(tvId("device-1"), "192.168.1.20", "pin", "token", true)
        assertEquals(null, reached)
        assertEquals(BannerKind.IdentityChanged, controller.snapshot.value.banner)
        assertEquals(listOf("192.168.1.20"), session.attemptedIps)
        assertEquals(0, scans)
    }

    @Test
    fun showPairingRequiredPublishesWithoutSocketWork() = runBlocking {
        val session = FakeConnectionSession(listOf(ConnectionOutcome.Connected(null)))
        val controller = ConnectionController(
            session = session,
            schedule = { 0L },
        )
        controller.showPairingRequired()
        val snap = controller.snapshot.value
        assertEquals(ConnectionState.Reconnecting, snap.state)
        assertEquals(BannerKind.PairingRequired, snap.banner)
        assertEquals(false, snap.keysEnabled)
        assertTrue(session.attemptedIps.isEmpty())
    }

    @Test
    fun targetSessionHoldsOneTargetWithNoPersistence() {
        val holder = TargetSessionHolder()
        assertEquals(TargetSession.NoTarget, holder.session.value)
        val id = tvId("device-1")
        holder.select(id)
        assertEquals(TargetSession.Selected(id.hex), holder.session.value)
        holder.clear()
        assertEquals(TargetSession.NoTarget, holder.session.value)
    }
}
