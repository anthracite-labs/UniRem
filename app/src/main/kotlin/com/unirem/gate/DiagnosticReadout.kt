package com.unirem.gate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.unirem.R
import com.unirem.core.api.LanAccess
import com.unirem.core.api.NetworkType
import com.unirem.core.api.code
import com.unirem.ui.graphitemint.GraphiteMint
import com.unirem.ui.graphitemint.GraphiteMintType

/**
 * Quiet diagnostic readout: phone network type, scan result, and the code
 * distinguishing denial from isolation from ok. Diagnostic copy only, never
 * marketing. No scan runs in this story, so the scan line always reads
 * "Not scanned".
 */
@Composable
fun DiagnosticReadout(
    snapshot: LanGateSnapshot,
    modifier: Modifier = Modifier,
) {
    val networkLabel = when (snapshot.networkType) {
        NetworkType.Wifi -> stringResource(R.string.gate_net_wifi)
        NetworkType.Ethernet -> stringResource(R.string.gate_net_ethernet)
        NetworkType.Unknown -> stringResource(R.string.gate_net_unknown)
    }
    val scanLabel = stringResource(R.string.gate_readout_not_scanned)
    val accessLabel = when (snapshot.access) {
        LanAccess.Granted -> stringResource(R.string.gate_net_granted)
        LanAccess.Denied -> stringResource(R.string.gate_net_denied)
        LanAccess.NotDeclared -> stringResource(R.string.gate_net_not_declared)
    }
    Column(modifier = modifier) {
        BasicText(
            text = stringResource(R.string.gate_readout_access, accessLabel),
            style = GraphiteMintType.Code.copy(color = GraphiteMint.InkSecondary),
        )
        BasicText(
            text = stringResource(R.string.gate_readout_network, networkLabel),
            style = GraphiteMintType.Code.copy(color = GraphiteMint.InkSecondary),
        )
        BasicText(
            text = stringResource(R.string.gate_readout_scan, scanLabel),
            style = GraphiteMintType.Code.copy(color = GraphiteMint.InkSecondary),
        )
        BasicText(
            text = stringResource(R.string.gate_readout_code, snapshot.code.code()),
            style = GraphiteMintType.Code.copy(color = GraphiteMint.InkSecondary),
        )
    }
}
