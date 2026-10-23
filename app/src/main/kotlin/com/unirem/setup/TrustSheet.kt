package com.unirem.setup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.unirem.gate.GateQuietAction
import com.unirem.tv.TvRecord
import com.unirem.ui.graphitemint.GraphiteMint
import com.unirem.ui.graphitemint.GraphiteMintRadii
import com.unirem.ui.graphitemint.GraphiteMintSpacing
import com.unirem.ui.graphitemint.GraphiteMintType

/**
 * Reusable `TvId`-keyed trust sheet ("TV details") for Story 1.6.
 *
 * Durable-only: reads the stored [TvRecord] plus the already-loaded pin
 * display string, never opens a socket (works TV-off). Session-only targets
 * never reach here; they show the session-only label instead. Dismisses on
 * back, scrim tap, or drag-down with focus returning to "TV details" via the
 * caller. Forget runs inside this sheet (no nested sheet). Closing a channel
 * here is silent by construction: this sheet performs no socket work and
 * sends no command.
 */
@Composable
fun TrustSheet(
    record: TvRecord,
    maskedMac: String,
    fingerprint: String?,
    confirming: Boolean,
    forgetting: Boolean,
    forgetFailed: Boolean,
    onRequestForget: () -> Unit,
    onConfirmForget: () -> Unit,
    onCancelConfirm: () -> Unit,
    onRetryForget: () -> Unit,
    onCloseError: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = { if (confirming) onCancelConfirm() else onDismiss() })
    val sheetTitle = stringResource(R.string.trust_details)
    val notReportedName = stringResource(R.string.setup_not_reported)
    val dismissHandleDescription = stringResource(R.string.trust_dismiss_handle)
    // Drag-down dismiss: a handle-only vertical drag so scrolling content
    // never fights the gesture. Past the threshold the sheet dismisses like
    // back and scrim tap (confirmation state cancels first).
    val dismissThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val dismissDrag = rememberDraggableState { delta ->
        dragOffsetPx = (dragOffsetPx + delta).coerceAtLeast(0f)
    }
    fun endDrag() {
        if (dragOffsetPx > dismissThresholdPx) {
            if (confirming) onCancelConfirm() else onDismiss()
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
                .background(GraphiteMint.Scrim.copy(alpha = 0.6f))
                .clickable(onClick = { if (confirming) onCancelConfirm() else onDismiss() }),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, dragOffsetPx.roundToInt()) }
                .background(GraphiteMint.SurfaceRaised)
                .border(1.dp, GraphiteMint.Hairline)
                .padding(GraphiteMintSpacing.X5)
                .verticalScroll(rememberScrollState()),
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
                modifier = Modifier.semantics { heading() },
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
            if (!confirming) {
                TrustDetailsContent(
                    record = record,
                    maskedMac = maskedMac,
                    fingerprint = fingerprint,
                )
                Spacer(modifier = Modifier.height(GraphiteMintSpacing.X4))
                if (forgetFailed) {
                    TrustForgetError(
                        onRetry = onRetryForget,
                        onClose = onCloseError,
                    )
                    Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TrustDestructiveAction(
                        label = stringResource(R.string.trust_forget),
                        description = stringResource(R.string.trust_forget_cd),
                        onClick = onRequestForget,
                        enabled = !forgetting,
                    )
                    Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
                    GateQuietAction(
                        label = stringResource(R.string.trust_cancel),
                        description = stringResource(R.string.trust_cancel_cd),
                        onClick = onDismiss,
                    )
                }
            } else {
                BasicText(
                    text = stringResource(R.string.trust_forget_title, record.name.ifBlank { notReportedName }),
                    style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
                )
                Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
                BasicText(
                    text = stringResource(R.string.trust_tv_note),
                    style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
                )
                Spacer(modifier = Modifier.height(GraphiteMintSpacing.X4))
                if (forgetFailed) {
                    TrustForgetError(
                        onRetry = onRetryForget,
                        onClose = onCloseError,
                    )
                    Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TrustDestructiveAction(
                        label = stringResource(R.string.trust_forget),
                        description = stringResource(R.string.trust_forget_cd),
                        onClick = onConfirmForget,
                        enabled = !forgetting,
                    )
                    Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
                    GateQuietAction(
                        label = stringResource(R.string.trust_cancel),
                        description = stringResource(R.string.trust_cancel_cd),
                        onClick = onCancelConfirm,
                    )
                }
            }
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        }
    }
}

@Composable
private fun TrustDetailsContent(
    record: TvRecord,
    maskedMac: String,
    fingerprint: String?,
    modifier: Modifier = Modifier,
) {
    val notReported = stringResource(R.string.setup_not_reported)
    Column(modifier = modifier.fillMaxWidth()) {
        // Identity: name, model, Tizen version. Never a live probe, never a
        // capability label, never a claim the TV shows the same string.
        BasicText(
            text = record.name.ifBlank { notReported },
            style = GraphiteMintType.Headline.copy(color = GraphiteMint.InkPrimary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
        BasicText(
            text = stringResource(
                R.string.trust_model,
                record.model.ifBlank { record.modelName.ifBlank { notReported } },
            ),
            style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
        BasicText(
            text = stringResource(R.string.trust_firmware, record.firmware.ifBlank { notReported }),
            style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
        // Masked MAC shows at most the last two octets; never the full MAC.
        BasicText(
            text = stringResource(R.string.trust_mac, maskedMac.ifBlank { notReported }),
            style = GraphiteMintType.Code.copy(color = GraphiteMint.InkSecondary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
        BasicText(
            text = stringResource(R.string.trust_last_ip, record.lastIp.ifBlank { notReported }),
            style = GraphiteMintType.Code.copy(color = GraphiteMint.InkSecondary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
        // Durable state only: never "Connected", never live health.
        BasicText(
            text = stringResource(R.string.trust_paired),
            style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
        BasicText(
            text = stringResource(R.string.trust_local_only),
            style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
        BasicText(
            text = stringResource(R.string.trust_fingerprint_label),
            style = GraphiteMintType.Label.copy(color = GraphiteMint.InkSecondary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
        if (fingerprint == null) {
            BasicText(
                text = stringResource(R.string.trust_fingerprint_unavailable),
                style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
            )
        } else {
            // Fully visible, selectable, wrapping or scrolling; never logged.
            SelectionContainer {
                BasicText(
                    text = fingerprint,
                    style = GraphiteMintType.Code.copy(color = GraphiteMint.InkPrimary),
                )
            }
        }
    }
}

@Composable
private fun TrustForgetError(
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        BasicText(
            text = stringResource(R.string.trust_forget_failed),
            style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        Row(verticalAlignment = Alignment.CenterVertically) {
            GateQuietAction(
                label = stringResource(R.string.trust_retry),
                description = stringResource(R.string.trust_retry_cd),
                onClick = onRetry,
            )
            Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
            GateQuietAction(
                label = stringResource(R.string.trust_close),
                description = stringResource(R.string.trust_close_cd),
                onClick = onClose,
            )
        }
    }
}

/**
 * Salmon/ring-offline outline destructive action. Outline only, never a
 * filled red button.
 */
@Composable
private fun TrustDestructiveAction(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(GraphiteMintRadii.Md))
            .border(1.dp, GraphiteMint.RingOffline, RoundedCornerShape(GraphiteMintRadii.Md))
            .clickable(role = Role.Button, onClick = onClick, enabled = enabled)
            .semantics(mergeDescendants = true) { contentDescription = "$label $description" }
            .heightIn(min = 48.dp)
            .padding(
                horizontal = GraphiteMintSpacing.X5,
                vertical = GraphiteMintSpacing.X3,
            ),
    ) {
        BasicText(
            text = label,
            style = GraphiteMintType.Button.copy(color = GraphiteMint.RingOffline),
        )
    }
}
