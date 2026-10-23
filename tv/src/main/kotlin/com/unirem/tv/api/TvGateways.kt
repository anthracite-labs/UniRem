package com.unirem.tv.api

import com.unirem.core.api.LanAccess
import com.unirem.tv.internal.ConnectionSession
import com.unirem.tv.internal.PinnedTlsChannel
import com.unirem.tv.internal.SsdpDiscovery

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
}
