package com.unirem.setup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.unirem.R
import com.unirem.connection.BannerKind
import com.unirem.connection.HealthDot
import com.unirem.tv.channel.ConnectionState
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

/**
 * Single-TV switcher button for Story 1.9 (left of the paired summary and the
 * future Remote topbar; right side stays empty, no gear).
 *
 * Durable target: an 8dp health dot (the shared [HealthDot], offline while no
 * socket is open, never a command outcome) plus the `body` 16sp name plus a
 * 12dp mint caret, 8dp gaps, 48dp target, topbar truncation with ellipsis.
 * Session-only targets ([showCaret] false) render the same dot and name with
 * no caret and no click action. The caret mirrors in RTL; the dot and the
 * name do not. No capability claim and no command state live in this button.
 */
@Composable
fun TvSwitcherButton(
    name: String,
    stateText: String,
    actionDescription: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    showCaret: Boolean = true,
    focusRequester: FocusRequester? = null,
) {
    val merged = "$name, $stateText, $actionDescription"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(GraphiteMintRadii.Md))
                .then(if (showCaret) Modifier.clickable(role = Role.Button, onClick = onOpen) else Modifier)
                .semantics(mergeDescendants = true) {
                    contentDescription = merged
                }
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .heightIn(min = 48.dp)
                .padding(
                    horizontal = GraphiteMintSpacing.X2,
                    vertical = GraphiteMintSpacing.X3,
                ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HealthDot(state = ConnectionState.Offline, banner = BannerKind.None)
                Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
                BasicText(
                    text = name,
                    style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (showCaret) {
                    Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
                    SwitcherCaret()
                }
            }
        }
    }
}

/**
 * 12dp mint caret. The caret mirrors in RTL via [LocalLayoutDirection]; the
 * dot and the name never mirror.
 */
@Composable
private fun SwitcherCaret(
    modifier: Modifier = Modifier,
) {
    val mirrored = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(
        modifier = modifier
            .size(12.dp)
            .then(if (mirrored) Modifier.scale(scaleX = -1f, scaleY = 1f) else Modifier),
    ) {
        val stroke = 2.dp.toPx()
        val left = Offset(2.dp.toPx(), 4.dp.toPx())
        val tip = Offset(size.width / 2f, size.height - 2.dp.toPx())
        val right = Offset(size.width - 2.dp.toPx(), 4.dp.toPx())
        drawLine(
            color = GraphiteMint.Accent,
            start = left,
            end = tip,
            strokeWidth = stroke,
        )
        drawLine(
            color = GraphiteMint.Accent,
            start = tip,
            end = right,
            strokeWidth = stroke,
        )
    }
}
