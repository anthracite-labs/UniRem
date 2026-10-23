package com.unirem.gate

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.unirem.R
import com.unirem.ui.graphitemint.GraphiteMint
import com.unirem.ui.graphitemint.GraphiteMintSpacing
import com.unirem.ui.graphitemint.GraphiteMintType

/**
 * In-context explainer sheet per UX-DR24. It always precedes the system
 * prompt: first setup shows the settled explainer with Continue / Not now; a
 * denied gate shows the plain denied copy with Open settings / Not now and
 * never re-fires the prompt. Dismissing (Not now or scrim) leaves the feature
 * unstarted with no state change.
 */
@Composable
fun ExplainerSheet(
    denied: Boolean,
    onContinue: () -> Unit,
    onNotNow: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onNotNow)
    Box(
        modifier = modifier.fillMaxSize().semantics { dialog() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GraphiteMint.Scrim.copy(alpha = 0.6f))
                .clickable(onClick = onNotNow),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GraphiteMint.SurfaceRaised)
                .border(1.dp, GraphiteMint.Hairline)
                .padding(GraphiteMintSpacing.X5),
        ) {
            BasicText(
                text = stringResource(R.string.gate_card_title),
                style = GraphiteMintType.Title.copy(color = GraphiteMint.InkPrimary),
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
            BasicText(
                text = stringResource(
                    if (denied) R.string.gate_denied_body else R.string.gate_explainer_body,
                ),
                style = GraphiteMintType.Body.copy(color = GraphiteMint.InkSecondary),
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X4))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (denied) {
                    GatePrimaryAction(
                        label = stringResource(R.string.gate_denied_settings),
                        description = stringResource(R.string.gate_cd_settings),
                        onClick = onOpenSettings,
                    )
                } else {
                    GatePrimaryAction(
                        label = stringResource(R.string.gate_explainer_continue),
                        description = stringResource(R.string.gate_cd_continue),
                        onClick = onContinue,
                    )
                }
                Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
                GateQuietAction(
                    label = stringResource(R.string.gate_not_now),
                    description = stringResource(R.string.gate_cd_dismiss),
                    onClick = onNotNow,
                )
            }
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        }
    }
}
