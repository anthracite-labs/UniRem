package com.unirem.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.unirem.R
import com.unirem.core.api.LanAccess
import com.unirem.ui.graphitemint.GraphiteMint
import com.unirem.ui.graphitemint.GraphiteMintRadii
import com.unirem.ui.graphitemint.GraphiteMintSpacing
import com.unirem.ui.graphitemint.GraphiteMintType

/**
 * The single permission entry point on the shell. Shows the live gate state
 * with exactly one appropriate action: grant (opens the explainer), open
 * settings (when denied), or a quiet access-ready note. Never claims a TV is
 * connected.
 */
@Composable
fun PermissionCard(
    state: LanGateSnapshot,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(GraphiteMintRadii.Md))
            .background(GraphiteMint.SurfaceRaised)
            .border(1.dp, GraphiteMint.Hairline, RoundedCornerShape(GraphiteMintRadii.Md))
            .padding(GraphiteMintSpacing.X4),
    ) {
        when (state.access) {
            LanAccess.Granted -> {
                BasicText(
                    text = stringResource(R.string.gate_card_ready),
                    style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
                )
            }
            LanAccess.NotDeclared -> {
                Column {
                    BasicText(
                        text = stringResource(R.string.gate_card_title),
                        style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
                    )
                    Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
                    BasicText(
                        text = stringResource(R.string.gate_card_needed),
                        style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
                    )
                    Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
                    GatePrimaryAction(
                        label = stringResource(R.string.gate_explainer_continue),
                        description = stringResource(R.string.gate_cd_continue),
                        onClick = onGrant,
                    )
                }
            }
            LanAccess.Denied -> {
                Column {
                    BasicText(
                        text = stringResource(R.string.gate_denied_body),
                        style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
                    )
                    Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
                    GatePrimaryAction(
                        label = stringResource(R.string.gate_denied_settings),
                        description = stringResource(R.string.gate_cd_settings),
                        onClick = onOpenSettings,
                    )
                }
            }
        }
    }
}
