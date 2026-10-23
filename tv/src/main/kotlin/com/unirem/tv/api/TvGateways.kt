package com.unirem.tv.api

import com.unirem.core.api.LanAccess
import com.unirem.core.api.command.RemoteCommand
import com.unirem.tv.api.TvControl
import com.unirem.tv.channel.CommandTracker
import com.unirem.tv.internal.ConnectionSession
import com.unirem.tv.internal.PinnedTlsChannel
import com.unirem.tv.internal.SsdpDiscovery
import kotlinx.coroutines.CoroutineScope

/**
 * Factories that keep `:app` on the `api` package only. The discovery scan
 * runs inside the [lockRunner] scope owned by the `:app` gate; the `:tv`
 * side never owns a platform handle.
 */
object TvGateways {

    fun discovery(
        gateCheck: () -> LanAccess,
        lockRunner: (block: () -> Set<String>) -> Set<String>,
    ): TvDiscoveryGateway = SsdpDiscovery(gateCheck, lockRunner)

    fun pairing(gateCheck: () -> LanAccess): TvPairingControl =
        PinnedTlsChannel(gateCheck)

    fun connection(gateCheck: () -> LanAccess): TvConnectionControl =
        ConnectionSession(gateCheck)

    /**
     * Socket-scoped command tracker (Story 1.11). One instance per open
     * remote; [send] writes one raw frame string to the open socket and
     * returns true only when the bytes left the phone. The frame is built
     * in `:tv`, so `:app` only ever passes opaque strings. [timeoutFor]
     * carries the Spike-A per-capability override; the default is 3s flat.
     */
    fun commandControl(
        scope: CoroutineScope,
        send: (String) -> Boolean,
        timeoutFor: (RemoteCommand) -> Long = { CommandTracker.SUBMIT_TIMEOUT_MILLIS },
    ): TvControl = CommandTracker(scope = scope, send = send, timeoutFor = timeoutFor)
}
