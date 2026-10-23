package com.unirem.remote

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.unirem.R
import com.unirem.connection.BannerKind
import com.unirem.connection.ConnectionBanner
import com.unirem.connection.HealthDot
import com.unirem.setup.DisabledLauncher
import com.unirem.tv.channel.ConnectionState
import com.unirem.ui.graphitemint.GraphiteMint
import com.unirem.ui.graphitemint.GraphiteMintRadii
import com.unirem.ui.graphitemint.GraphiteMintSpacing
import com.unirem.ui.graphitemint.GraphiteMintType

/**
 * Remote shell for the active target only (Story 1.10).
 *
 * Opens for one durable TvId or one session-only IP; the caller owns the
 * single socket-scoped session and closes it on leave, background, clear,
 * forget, or target change. This screen opens no socket itself and renders
 * no command rows, chips, tabs, or scrolling content.
 *
 * Layout: 56dp topbar with a hairline and an empty right side, a 360-480dp
 * column, the ring in the lower third, a 24dp state line, and the disabled
 * launcher pinned to the bottom bar. The topbar dot reports socket health;
 * the ring reports command state. One banner at a time pushes content.
 */
@Composable
fun RemoteScreen(
    tvName: String,
    sessionOnly: Boolean,
    showSwitcherCaret: Boolean,
    switcherStateText: String,
    onOpenSwitcher: () -> Unit,
    state: ConnectionState,
    banner: BannerKind,
    lanDenied: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onNotNow: () -> Unit,
    onRePair: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val ring = RemoteRules.ringFor(state, banner, lanDenied)
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = GraphiteMintSpacing.MaxColumnWidth)
                .fillMaxWidth()
                .padding(horizontal = GraphiteMintSpacing.ScreenX),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RemoteTopBar(
                tvName = tvName,
                sessionOnly = sessionOnly,
                showSwitcherCaret = showSwitcherCaret,
                switcherStateText = switcherStateText,
                state = state,
                banner = banner,
                onBack = onBack,
                onOpenSwitcher = onOpenSwitcher,
            )
            if (lanDenied) {
                Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
                RemoteGateBanner(
                    onOpenSettings = onOpenSettings,
                    onNotNow = onNotNow,
                )
            } else if (banner != BannerKind.None) {
                Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
                ConnectionBanner(
                    banner = banner,
                    onRetry = onRetry,
                    onNotNow = onNotNow,
                    onRePair = onRePair,
                )
            }
            Spacer(modifier = Modifier.weight(1.6f))
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val ringSize: Dp = if (maxHeight < 600.dp) 208.dp else 236.dp
                RemoteRing(
                    ring = ring,
                    diameter = ringSize,
                )
            }
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
            RemoteStateLine(ring = ring)
            Spacer(modifier = Modifier.weight(1f))
            DisabledLauncher(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X4))
        }
    }
}

/**
 * 56dp topbar: back action, socket-health dot plus the TV name (with a
 * mint caret for durable targets, caret-less static text for session-only),
 * and an empty right side. A 1dp hairline sits below.
 */
@Composable
private fun RemoteTopBar(
    tvName: String,
    sessionOnly: Boolean,
    showSwitcherCaret: Boolean,
    switcherStateText: String,
    state: ConnectionState,
    banner: BannerKind,
    onBack: () -> Unit,
    onOpenSwitcher: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backLabel = stringResource(R.string.setup_back)
    val backCd = stringResource(R.string.setup_back_cd)
    val switcherCd = stringResource(R.string.switcher_open_cd)
    val name = tvName.ifBlank { stringResource(R.string.setup_not_reported) }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
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
                        horizontal = GraphiteMintSpacing.X2,
                        vertical = GraphiteMintSpacing.X3,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = backLabel,
                    style = GraphiteMintType.Button.copy(color = GraphiteMint.InkSecondary),
                )
            }
            if (showSwitcherCaret && !sessionOnly) {
                val merged = "$name, $switcherStateText, $switcherCd"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(GraphiteMintRadii.Md))
                        .clickable(role = Role.Button, onClick = onOpenSwitcher)
                        .semantics(mergeDescendants = true) { contentDescription = merged }
                        .heightIn(min = 48.dp)
                        .padding(
                            horizontal = GraphiteMintSpacing.X2,
                            vertical = GraphiteMintSpacing.X3,
                        ),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HealthDot(state = state, banner = banner)
                        Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
                        BasicText(
                            text = name,
                            style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
                        RemoteCaret()
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .padding(
                            horizontal = GraphiteMintSpacing.X2,
                            vertical = GraphiteMintSpacing.X3,
                        )
                        .semantics(mergeDescendants = true) {
                            contentDescription = "$name, $switcherStateText"
                        },
                ) {
                    HealthDot(state = state, banner = banner)
                    Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
                    BasicText(
                        text = name,
                        style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
            Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GraphiteMint.Hairline),
        )
    }
}

@Composable
private fun RemoteCaret(
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

/**
 * Command-state ring: idle has no glow, offline carries a salmon glow.
 * Static in every motion setting; the text twin below carries the meaning.
 */
@Composable
fun RemoteRing(
    ring: RemoteRing,
    diameter: Dp,
    modifier: Modifier = Modifier,
) {
    val base = if (ring == RemoteRing.Idle) GraphiteMint.RingIdle else GraphiteMint.RingOffline
    // Purely visual: the state line below carries the Idle/Offline label, so
    // the ring stays out of the accessibility tree and focus order.
    Canvas(modifier = modifier.size(diameter)) {
        val stroke = 2.dp.toPx()
        if (ring == RemoteRing.Offline) {
            drawCircle(
                color = GraphiteMint.RingOffline.copy(alpha = 0.18f),
                radius = size.minDimension / 2f,
            )
        }
        drawCircle(
            color = base,
            radius = size.minDimension / 2f - stroke / 2f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
        )
    }
}

/**
 * 24dp-minimum centered state line: a 6dp lamp plus 13sp/500 Idle/Offline.
 * Polite live region announcing once per change; never health copy.
 */
@Composable
fun RemoteStateLine(
    ring: RemoteRing,
    modifier: Modifier = Modifier,
) {
    val idleText = stringResource(R.string.remote_idle)
    val offlineText = stringResource(R.string.remote_offline)
    val text = if (ring == RemoteRing.Idle) idleText else offlineText
    val lamp = if (ring == RemoteRing.Idle) GraphiteMint.RingIdle else GraphiteMint.RingOffline
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 24.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = text
                liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
            },
    ) {
        Canvas(modifier = Modifier.size(6.dp)) {
            drawCircle(color = lamp, radius = size.minDimension / 2f)
        }
        Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
        BasicText(
            text = text,
            style = GraphiteMintType.State.copy(color = GraphiteMint.InkPrimary),
        )
    }
}

/**
 * Oxblood gate-denied banner for the remote: local-network copy with one
 * recovery action plus Not now. Pushes content; never asserts and never
 * claims the TV answered.
 */
@Composable
private fun RemoteGateBanner(
    onOpenSettings: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settingsLabel = stringResource(R.string.gate_denied_settings)
    val notNowLabel = stringResource(R.string.gate_not_now)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }
            .clip(RoundedCornerShape(GraphiteMintRadii.Md))
            .background(GraphiteMint.BannerSurface)
            .border(1.dp, GraphiteMint.BannerBorder, RoundedCornerShape(GraphiteMintRadii.Md))
            .padding(GraphiteMintSpacing.X4),
    ) {
        BasicText(
            text = stringResource(R.string.gate_denied_body),
            style = GraphiteMintType.Body.copy(color = GraphiteMint.BannerInk),
            maxLines = 2,
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(GraphiteMintRadii.Md))
                    .background(GraphiteMint.BannerAction)
                    .clickable(role = Role.Button, onClick = onOpenSettings)
                    .semantics(mergeDescendants = true) {
                        contentDescription = settingsLabel
                    }
                    .heightIn(min = 48.dp)
                    .padding(
                        horizontal = GraphiteMintSpacing.X5,
                        vertical = GraphiteMintSpacing.X3,
                    ),
            ) {
                BasicText(
                    text = settingsLabel,
                    style = GraphiteMintType.Button.copy(color = GraphiteMint.SurfaceBase),
                )
            }
            Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
            Box(
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onNotNow)
                    .semantics(mergeDescendants = true) {
                        contentDescription = notNowLabel
                    }
                    .heightIn(min = 48.dp)
                    .padding(
                        horizontal = GraphiteMintSpacing.X4,
                        vertical = GraphiteMintSpacing.X3,
                    ),
            ) {
                BasicText(
                    text = notNowLabel,
                    style = GraphiteMintType.Button.copy(color = GraphiteMint.BannerAction),
                )
            }
        }
    }
}
