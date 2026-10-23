package com.unirem.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.unirem.R
import com.unirem.ui.graphitemint.GraphiteMint
import com.unirem.ui.graphitemint.GraphiteMintRadii
import com.unirem.ui.graphitemint.GraphiteMintSpacing
import com.unirem.ui.graphitemint.GraphiteMintType

/**
 * Setup top bar: back action plus a static title, right side always empty.
 * 48dp targets, Graphite Mint tokens only.
 */
@Composable
fun SetupTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backLabel = stringResource(R.string.setup_back)
    val backCd = stringResource(R.string.setup_back_cd)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(GraphiteMintRadii.Md))
                .clickable(role = Role.Button, onClick = onBack)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$backLabel $backCd"
                }
                .heightIn(min = 48.dp)
                .padding(
                    horizontal = GraphiteMintSpacing.X4,
                    vertical = GraphiteMintSpacing.X3,
                ),
        ) {
            BasicText(
                text = stringResource(R.string.setup_back),
                style = GraphiteMintType.Button.copy(color = GraphiteMint.InkSecondary),
            )
        }
        BasicText(
            text = stringResource(R.string.setup_add_title),
            style = GraphiteMintType.Title.copy(color = GraphiteMint.InkPrimary),
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
    }
}

/** Six disabled slots shown on the no-TV shell. No state claims. */
@Composable
fun DisabledLauncher(
    modifier: Modifier = Modifier,
) {
    val slotCd = stringResource(R.string.setup_slot_cd)
    val slotEmpty = stringResource(R.string.setup_slot_empty)
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        repeat(2) {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(GraphiteMintSpacing.X1)
                            .clip(RoundedCornerShape(GraphiteMintRadii.Md))
                            .background(GraphiteMint.SurfaceRaised)
                            .border(1.dp, GraphiteMint.Hairline, RoundedCornerShape(GraphiteMintRadii.Md))
                            .padding(
                                horizontal = GraphiteMintSpacing.X3,
                                vertical = GraphiteMintSpacing.X4,
                            )
                            .semantics { contentDescription = slotCd },
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = slotEmpty,
                            style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkDisabled),
                        )
                    }
                }
            }
        }
    }
}
