package com.unirem.setup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.unirem.R
import com.unirem.connection.BannerKind
import com.unirem.connection.HealthDot
import com.unirem.gate.GateQuietAction
import com.unirem.tv.channel.ConnectionState
import com.unirem.ui.graphitemint.GraphiteMint
import com.unirem.ui.graphitemint.GraphiteMintRadii
import com.unirem.ui.graphitemint.GraphiteMintSpacing
import com.unirem.ui.graphitemint.GraphiteMintType

/**
 * One-TV switcher sheet for Story 1.9 ("TVs").
 *
 * Deliberately a one-item list, not a single-TV branch, so Story 2 adds rows
 * without a model change. The sheet is presentation-only: it opens nothing,
 * attempts no network work, and changes no target on open. Tapping the
 * current row closes the sheet with no reconnect, reset, or health change;
 * "TV details" hands the `TvId` to the Story 1.6 trust sheet without changing
 * the target. An unreadable record shows "TV unavailable" with "Pair again"
 * and invents no fallback target. Session-only targets never reach here and
 * get no row. Chrome (grabber, 64% scrim, at most 80% height, drag-down /
 * scrim / back dismiss) follows the `TrustSheet` idiom; focus moves into the
 * sheet on open and the caller returns it to the switcher button on close.
 * There is no "Add a TV" row or button and never a second sheet.
 */
@Composable
fun TvSwitcherSheet(
    tvName: String,
    tvModel: String,
    capabilityPlate: String?,
    stateText: String,
    unreadable: Boolean,
    onSelectCurrent: () -> Unit,
    onShowDetails: () -> Unit,
    onPairAgain: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)
    val sheetFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try {
            sheetFocus.requestFocus()
        } catch (e: Exception) {
            // Focus stays where the system left it; the sheet is still
            // dismissible via back, scrim, and drag-down.
        }
    }
    val sheetTitle = stringResource(R.string.switcher_tvs)
    val dismissHandleDescription = stringResource(R.string.trust_dismiss_handle)
    val dismissThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val dismissDrag = rememberDraggableState { delta ->
        dragOffsetPx = (dragOffsetPx + delta).coerceAtLeast(0f)
    }
    fun endDrag() {
        if (dragOffsetPx > dismissThresholdPx) {
            onDismiss()
        }
        dragOffsetPx = 0f
    }
    Box(
        modifier = modifier.fillMaxSize().semantics { dialog() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GraphiteMint.Scrim.copy(alpha = 0.64f))
                .clickable(onClick = onDismiss),
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, dragOffsetPx.roundToInt()) },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * 0.8f)
                    .background(GraphiteMint.SurfaceRaised)
                    .border(1.dp, GraphiteMint.Hairline)
                    .padding(GraphiteMintSpacing.X5),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = GraphiteMintSpacing.X2)
                        .draggable(
                            state = dismissDrag,
                            orientation = Orientation.Vertical,
                            onDragStopped = { endDrag() },
                        )
                        .semantics {
                            contentDescription = dismissHandleDescription
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(GraphiteMint.Hairline),
                    )
                }
                BasicText(
                    text = sheetTitle,
                    style = GraphiteMintType.Title.copy(color = GraphiteMint.InkPrimary),
                    modifier = Modifier
                        .focusRequester(sheetFocus)
                        .focusable()
                        .semantics { heading() },
                )
                Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
                if (unreadable) {
                    SwitcherUnavailable(
                        onPairAgain = onPairAgain,
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        item(contentType = "switcher-row") {
                            SwitcherRow(
                                tvName = tvName,
                                tvModel = tvModel,
                                capabilityPlate = capabilityPlate,
                                stateText = stateText,
                                onSelectCurrent = onSelectCurrent,
                                onShowDetails = onShowDetails,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
            }
        }
    }
}

/**
 * The one row: health dot plus full name plus model plus the durable
 * text-plate label, marked current by text plus a check glyph (never
 * color-only). The row body closes the sheet only; the details action opens
 * the trust sheet. The row shows the full name with wrap; only the topbar
 * button truncates.
 */
@Composable
private fun SwitcherRow(
    tvName: String,
    tvModel: String,
    capabilityPlate: String?,
    stateText: String,
    onSelectCurrent: () -> Unit,
    onShowDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentText = stringResource(R.string.switcher_current)
    // The row's merged name carries name, model, health, current marker, and
    // capability: the health dot is a Canvas and carries no semantics itself.
    val rowCd = listOfNotNull(
        tvName,
        tvModel.ifBlank { null },
        stateText,
        currentText,
        capabilityPlate,
    ).joinToString(", ")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GraphiteMintRadii.Md))
            .background(GraphiteMint.SurfaceBase)
            .border(1.dp, GraphiteMint.Hairline, RoundedCornerShape(GraphiteMintRadii.Md))
            .padding(GraphiteMintSpacing.X4),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(GraphiteMintRadii.Md))
                .clickable(role = Role.Button, onClick = onSelectCurrent)
                .semantics(mergeDescendants = true) {
                    contentDescription = rowCd
                }
                .heightIn(min = 48.dp)
                .padding(vertical = GraphiteMintSpacing.X1),
        ) {
            HealthDot(state = ConnectionState.Offline, banner = BannerKind.None)
            Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
            BasicText(
                text = tvName,
                style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
            BasicText(
                text = currentText,
                style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
            )
            Spacer(modifier = Modifier.width(GraphiteMintSpacing.X1))
            BasicText(
                text = "✓",
                style = GraphiteMintType.Body.copy(color = GraphiteMint.Accent),
            )
        }
        if (tvModel.isNotBlank()) {
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
            BasicText(
                text = tvModel,
                style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
            )
        }
        if (capabilityPlate != null) {
            // The single Keys/Device text plate, never a badge and never
            // health. Offline rows may still read full control: the plate
            // reports the recorded matrix pass, not the live link.
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
            BasicText(
                text = capabilityPlate,
                style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
            )
        }
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        GateQuietAction(
            label = stringResource(R.string.trust_details),
            description = stringResource(R.string.trust_details_cd),
            onClick = onShowDetails,
        )
    }
}

/** Unreadable record: no invented target, only the re-pair entry point. */
@Composable
private fun SwitcherUnavailable(
    onPairAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        BasicText(
            text = stringResource(R.string.switcher_unavailable),
            style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
        GateQuietAction(
            label = stringResource(R.string.switcher_pair_again),
            description = stringResource(R.string.switcher_pair_again_cd),
            onClick = onPairAgain,
        )
    }
}
