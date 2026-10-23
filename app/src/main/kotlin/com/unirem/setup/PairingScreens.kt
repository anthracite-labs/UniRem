package com.unirem.setup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.unirem.R
import com.unirem.gate.GatePrimaryAction
import com.unirem.gate.GateQuietAction
import com.unirem.tv.api.TvCandidate
import com.unirem.ui.graphitemint.GraphiteMint
import com.unirem.ui.graphitemint.GraphiteMintRadii
import com.unirem.ui.graphitemint.GraphiteMintSpacing
import com.unirem.ui.graphitemint.GraphiteMintType

/**
 * Pairing screen: the Allow prompt with the always-visible no-remote path and
 * its button diagram, plus deny/timeout states with Retry / Not now. The
 * diagram stays visible in every state.
 */
@Composable
fun PairingScreen(
    candidate: TvCandidate,
    waiting: Boolean,
    denied: Boolean,
    timedOut: Boolean,
    tokenMissing: Boolean,
    onRetry: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
    failureReason: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        BasicText(
            text = stringResource(R.string.pairing_allow_title),
            style = GraphiteMintType.Headline.copy(color = GraphiteMint.InkPrimary),
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        BasicText(
            text = stringResource(R.string.pairing_allow_body),
            style = GraphiteMintType.Body.copy(color = GraphiteMint.InkSecondary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
        NoRemoteDiagram(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
        if (denied) {
            BasicText(
                text = stringResource(R.string.pairing_denied),
                style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
            BasicText(
                text = stringResource(R.string.pairing_denied_body),
                style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        }
        if (timedOut) {
            BasicText(
                text = stringResource(R.string.pairing_timeout),
                style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
            BasicText(
                text = stringResource(R.string.pairing_timeout_body),
                style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        }
        if (tokenMissing) {
            BasicText(
                text = stringResource(R.string.pairing_token_missing),
                style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        }
        if (failureReason != null) {
            BasicText(
                text = failureReason,
                style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (waiting) {
                BasicText(
                    text = stringResource(R.string.pairing_waiting),
                    style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
                )
            } else {
                GatePrimaryAction(
                    label = stringResource(R.string.pairing_retry),
                    description = stringResource(R.string.pairing_retry_cd),
                    onClick = onRetry,
                )
                Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
                GateQuietAction(
                    label = stringResource(R.string.pairing_not_now),
                    description = stringResource(R.string.pairing_not_now_cd),
                    onClick = onNotNow,
                )
            }
        }
    }
}

/**
 * No-remote path diagram: line glyphs only (custom canvas strokes plus
 * labels), never icons or emoji. Shows the on-TV button path to Allow.
 */
@Composable
fun NoRemoteDiagram(
    modifier: Modifier = Modifier,
) {
    val diagramCd = stringResource(R.string.pairing_diagram_cd)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GraphiteMintRadii.Md))
            .border(1.dp, GraphiteMint.Hairline, RoundedCornerShape(GraphiteMintRadii.Md))
            .padding(GraphiteMintSpacing.X4)
            .semantics { contentDescription = diagramCd },
    ) {
        BasicText(
            text = stringResource(R.string.pairing_no_remote_title),
            style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
        BasicText(
            text = stringResource(R.string.pairing_no_remote_body),
            style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(72.dp, 48.dp)) {
                val stroke = 2.dp.toPx()
                drawRect(
                    color = GraphiteMint.Hairline,
                    topLeft = Offset(4f, 4f),
                    size = androidx.compose.ui.geometry.Size(size.width - 8f, size.height - 8f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                )
                drawLine(
                    color = GraphiteMint.InkSecondary,
                    start = Offset(size.width * 0.3f, size.height * 0.5f),
                    end = Offset(size.width * 0.7f, size.height * 0.5f),
                    strokeWidth = stroke,
                )
                drawCircle(
                    color = GraphiteMint.Accent,
                    radius = 5.dp.toPx(),
                    center = Offset(size.width * 0.7f, size.height * 0.5f),
                )
            }
            Spacer(modifier = Modifier.width(GraphiteMintSpacing.X3))
            Column {
                BasicText(
                    text = stringResource(R.string.pairing_diagram_step_one),
                    style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
                )
                BasicText(
                    text = stringResource(R.string.pairing_diagram_step_two),
                    style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
                )
            }
        }
    }
}

/** Paired state: name plus model, Done returns to the shell. */
@Composable
fun PairedScreen(
    name: String,
    model: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    sessionOnly: Boolean = false,
) {
    val notReported = stringResource(R.string.setup_not_reported)
    val line = (if (model.isBlank()) name else "$name · $model").ifBlank { notReported }
    Column(modifier = modifier.fillMaxWidth()) {
        BasicText(
            text = stringResource(R.string.paired_title),
            style = GraphiteMintType.Headline.copy(color = GraphiteMint.InkPrimary),
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        BasicText(
            text = line,
            style = GraphiteMintType.Body.copy(color = GraphiteMint.InkSecondary),
        )
        if (sessionOnly) {
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
            BasicText(
                text = stringResource(R.string.paired_session_only),
                style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
            )
        }
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X4))
        GatePrimaryAction(
            label = stringResource(R.string.paired_done),
            description = stringResource(R.string.paired_done_cd),
            onClick = onDone,
        )
    }
}
