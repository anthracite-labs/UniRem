package com.unirem.setup

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.unirem.R
import com.unirem.core.api.IdentityRules
import com.unirem.gate.GatePrimaryAction
import com.unirem.gate.GateQuietAction
import com.unirem.tv.api.TvCandidate
import com.unirem.ui.graphitemint.GraphiteMint
import com.unirem.ui.graphitemint.GraphiteMintRadii
import com.unirem.ui.graphitemint.GraphiteMintSpacing
import com.unirem.ui.graphitemint.GraphiteMintType

/**
 * Add TV screen: as-found results under "Scanning for TVs…", a "Scan again"
 * action after a run, and the manual-IP form. Leaving this screen stops the
 * scan and releases the held scope in the caller.
 */
@Composable
fun AddTvScreen(
    scanning: Boolean,
    scanDone: Boolean,
    targets: List<TvCandidate>,
    diagnostic: String?,
    manualIp: String,
    manualError: String?,
    gateBlocked: Boolean,
    onScanAgain: () -> Unit,
    onManualIpChange: (String) -> Unit,
    onManualRead: () -> Unit,
    onChoose: (TvCandidate) -> Unit,
    onDismissCard: (TvCandidate) -> Unit,
    modifier: Modifier = Modifier,
    manualKeys: Set<String> = emptySet(),
) {
    val manualCd = stringResource(R.string.setup_manual_cd)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        BasicText(
            text = if (scanning) {
                stringResource(R.string.setup_scanning)
            } else {
                stringResource(R.string.setup_scan_idle)
            },
            style = GraphiteMintType.Headline.copy(color = GraphiteMint.InkPrimary),
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        if (gateBlocked) {
            BasicText(
                text = stringResource(R.string.setup_gate_blocked),
                style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
        }
        if (diagnostic != null) {
            BasicText(
                text = diagnostic,
                style = GraphiteMintType.Code.copy(color = GraphiteMint.InkSecondary),
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
        }
        targets.forEach { target ->
            PairingCard(
                candidate = target,
                onConfirm = { onChoose(target) },
                onReject = { onDismissCard(target) },
                modifier = Modifier.fillMaxWidth(),
                manual = manualKeys.contains(SetupLogic.cardKey(target)),
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        }
        if (scanDone && !scanning && targets.isEmpty() && !gateBlocked) {
            BasicText(
                text = stringResource(R.string.setup_empty_title),
                style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
            BasicText(
                text = stringResource(R.string.setup_isolation_body),
                style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
        }
        if (scanDone && !scanning) {
            GateQuietAction(
                label = stringResource(R.string.setup_scan_again),
                description = stringResource(R.string.setup_scan_again_cd),
                onClick = onScanAgain,
                enabled = !gateBlocked,
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
        }
        BasicText(
            text = stringResource(R.string.setup_manual_title),
            style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
        BasicTextField(
            value = manualIp,
            onValueChange = onManualIpChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GraphiteMintRadii.Md))
                .border(1.dp, GraphiteMint.Hairline, RoundedCornerShape(GraphiteMintRadii.Md))
                .padding(GraphiteMintSpacing.X4)
                .semantics { contentDescription = manualCd },
            textStyle = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
            singleLine = true,
            enabled = !gateBlocked,
        )
        if (manualError != null) {
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
            BasicText(
                text = manualError,
                style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
            )
        }
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        GatePrimaryAction(
            label = stringResource(R.string.setup_manual_read),
            description = stringResource(R.string.setup_manual_read_cd),
            onClick = onManualRead,
            enabled = !gateBlocked,
        )
    }
}

/**
 * Explicit-choice pairing card: primary connect plus an always-visible
 * reject path. No auto-pair, no pre-selected card.
 */
@Composable
fun PairingCard(
    candidate: TvCandidate,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
    manual: Boolean = false,
) {
    val notReported = stringResource(R.string.setup_not_reported)
    val sessionOnly = SetupLogic.isSessionOnly(candidate)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GraphiteMintRadii.Md))
            .border(1.dp, GraphiteMint.Hairline, RoundedCornerShape(GraphiteMintRadii.Md))
            .padding(GraphiteMintSpacing.X4),
    ) {
        BasicText(
            text = candidate.displayName,
            style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
        BasicText(
            text = candidate.modelLabel.ifBlank { notReported },
            style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
        )
        BasicText(
            text = candidate.firmware.ifBlank { notReported },
            style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
        )
        if (sessionOnly) {
            BasicText(
                text = stringResource(R.string.setup_session_only),
                style = GraphiteMintType.Code.copy(color = GraphiteMint.InkSecondary),
            )
            BasicText(
                text = stringResource(R.string.setup_session_only_body),
                style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
            )
        } else {
            BasicText(
                text = stringResource(R.string.setup_masked_mac, IdentityRules.maskMac(candidate.wifiMac)),
                style = GraphiteMintType.Code.copy(color = GraphiteMint.InkSecondary),
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
            BasicText(
                text = stringResource(R.string.setup_first_time),
                style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
            )
            BasicText(
                text = if (manual) {
                    stringResource(R.string.setup_added_by_ip)
                } else {
                    stringResource(R.string.setup_same_wifi)
                },
                style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
            )
        }
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
        Row(verticalAlignment = Alignment.CenterVertically) {
            GatePrimaryAction(
                label = stringResource(R.string.pairing_connect),
                description = stringResource(R.string.pairing_connect_cd),
                onClick = onConfirm,
            )
            Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
            GateQuietAction(
                label = stringResource(R.string.pairing_not_mine),
                description = stringResource(R.string.pairing_not_mine_cd),
                onClick = onReject,
            )
        }
    }
}
