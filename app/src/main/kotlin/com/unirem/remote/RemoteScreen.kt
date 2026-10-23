package com.unirem.remote

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
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
import com.unirem.core.api.command.CommandOutcome
import com.unirem.core.api.command.RemoteCommand
import com.unirem.setup.DisabledLauncher
import com.unirem.tv.channel.ConnectionState
import com.unirem.ui.graphitemint.GraphiteMint
import com.unirem.ui.graphitemint.GraphiteMintRadii
import com.unirem.ui.graphitemint.GraphiteMintSpacing
import com.unirem.ui.graphitemint.GraphiteMintType

/**
 * Remote shell for the active target only (Story 1.10) with the Story 1.11
 * D-pad and command truth.
 *
 * Opens for one durable TvId or one session-only IP; the caller owns the
 * single socket-scoped session and the socket-scoped command tracker this
 * screen observes, and closes both on leave, background, clear, forget, or
 * target change. This screen opens no socket itself and renders no command
 * rows, chips, tabs, or scrolling content.
 *
 * Layout: 56dp topbar with a hairline and an empty right side, a 360-480dp
 * column, the 236dp ring holding the 3×3 D-pad in the lower third, a 24dp
 * state line, an "It worked" recovery action only while Unconfirmed, and
 * the disabled launcher pinned to the bottom bar. The topbar dot reports
 * socket health; the ring reports command state. One banner at a time
 * pushes content.
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
    outcome: CommandOutcome,
    dpadEnabled: Boolean,
    onDpad: (RemoteCommand) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onNotNow: () -> Unit,
    onRePair: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val ring = RemoteRules.ringFor(state, banner, lanDenied, outcome)
    // The breath key: every new Submitted generation replays the 900ms
    // breath exactly once, so a superseding tap never inherits a stale 1f.
    val outcomeGeneration = when (outcome) {
        is CommandOutcome.Submitted -> outcome.generation
        is CommandOutcome.Unconfirmed -> outcome.generation
        is CommandOutcome.Observed -> outcome.generation
        CommandOutcome.Idle -> -1L
    }
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
            // The ring holds the D-pad: the circle reports command state and
            // the centered 3×3 cluster sends it. Corners stay empty spacers:
            // no diagonals, no swipe, no placeholders.
            Box(
                modifier = Modifier.size(RemoteRingDiameter),
                contentAlignment = Alignment.Center,
            ) {
                RemoteRing(
                    ring = ring,
                    diameter = RemoteRingDiameter,
                    generation = outcomeGeneration,
                )
                DpadCluster(
                    enabled = dpadEnabled,
                    onDpad = onDpad,
                )
            }
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
            RemoteStateLine(ring = ring, outcome = outcome)
            if (ring == RemoteRing.Unconfirmed) {
                Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
                ItWorkedButton(onConfirm = onConfirm)
            }
            Spacer(modifier = Modifier.weight(1f))
            DisabledLauncher(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X4))
        }
    }
}

/** Fixed ring diameter from the UX contract; the D-pad grid nests inside. */
private val RemoteRingDiameter: Dp = 236.dp

/**
 * 3×3 D-pad cluster (Story 1.11): 64/76/64 columns, 60/70/60 rows, 4dp
 * gaps, centered in the 236dp ring. Row-major order is Up, Left, OK, Right,
 * Down. The cluster is pinned to physical layout: Left stays left and the
 * chevrons never mirror in RTL.
 */
@Composable
private fun DpadCluster(
    enabled: Boolean,
    onDpad: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Physical cluster: forced LTR so Left stays left in RTL locales and the
    // custom chevrons (drawn paths, never auto-mirrored) keep pointing.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Spacer(modifier = Modifier.size(DpadNarrow, DpadShort))
                DirectionKey(
                    command = RemoteCommand.Up,
                    labelRes = R.string.dpad_up,
                    cellWidth = DpadWide,
                    cellHeight = DpadShort,
                    enabled = enabled,
                    onDpad = onDpad,
                )
                Spacer(modifier = Modifier.size(DpadNarrow, DpadShort))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DirectionKey(
                    command = RemoteCommand.Left,
                    labelRes = R.string.dpad_left,
                    cellWidth = DpadNarrow,
                    cellHeight = DpadTall,
                    enabled = enabled,
                    onDpad = onDpad,
                )
                OkKey(
                    enabled = enabled,
                    onDpad = onDpad,
                )
                DirectionKey(
                    command = RemoteCommand.Right,
                    labelRes = R.string.dpad_right,
                    cellWidth = DpadNarrow,
                    cellHeight = DpadTall,
                    enabled = enabled,
                    onDpad = onDpad,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Spacer(modifier = Modifier.size(DpadNarrow, DpadShort))
                DirectionKey(
                    command = RemoteCommand.Down,
                    labelRes = R.string.dpad_down,
                    cellWidth = DpadWide,
                    cellHeight = DpadShort,
                    enabled = enabled,
                    onDpad = onDpad,
                )
                Spacer(modifier = Modifier.size(DpadNarrow, DpadShort))
            }
        }
    }
}

private val DpadNarrow: Dp = 64.dp
private val DpadWide: Dp = 76.dp
private val DpadShort: Dp = 60.dp
private val DpadTall: Dp = 70.dp

/**
 * One 60×56dp direction key: `rounded.md` 14dp, `surface-key` with a 1dp
 * hairline, a 15sp custom line chevron. Pressed scales to 0.98 with the
 * pressed surface; disabled drops to 45% with disabled semantics. Both axes
 * clear the 48dp minimum; press-and-hold never repeats (Story 1.13).
 */
@Composable
private fun DirectionKey(
    command: RemoteCommand,
    labelRes: Int,
    cellWidth: Dp,
    cellHeight: Dp,
    enabled: Boolean,
    onDpad: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(labelRes)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.98f else 1f,
        animationSpec = tween(80),
        label = "dpad-press",
    )
    // The 60×56dp key centers in its grid cell.
    Box(
        modifier = modifier.size(cellWidth, cellHeight),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(60.dp, 56.dp)
                .graphicsLayer(scaleX = pressScale, scaleY = pressScale)
                .alpha(if (enabled) 1f else 0.45f)
                .clip(RoundedCornerShape(GraphiteMintRadii.Md))
                .background(if (pressed && enabled) GraphiteMint.SurfaceKeyPressed else GraphiteMint.SurfaceKey)
                .border(1.dp, GraphiteMint.Hairline, RoundedCornerShape(GraphiteMintRadii.Md))
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = { onDpad(command) },
                )
                .semantics(mergeDescendants = true) { contentDescription = name },
            contentAlignment = Alignment.Center,
        ) {
            DpadChevron(command = command)
        }
    }
}

/**
 * The 70dp OK circle: `accent` fill, `ink-on-accent` 15sp/500 label.
 * Pressed drops to the pressed accent with the same 0.98 scale; disabled
 * drops to 45% with disabled semantics. Emits Enter on the wire.
 */
@Composable
private fun OkKey(
    enabled: Boolean,
    onDpad: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(R.string.dpad_ok)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.98f else 1f,
        animationSpec = tween(80),
        label = "ok-press",
    )
    Box(
        modifier = modifier
            .size(DpadTall)
            .graphicsLayer(scaleX = pressScale, scaleY = pressScale)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .background(if (pressed && enabled) GraphiteMint.AccentPressed else GraphiteMint.Accent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = { onDpad(RemoteCommand.Enter) },
            )
            .semantics(mergeDescendants = true) { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = name,
            style = GraphiteMintType.Button.copy(color = GraphiteMint.InkOnAccent),
        )
    }
}

/** Custom 15sp line chevron: no icon font, no emoji, no text-as-icon. */
@Composable
private fun DpadChevron(
    command: RemoteCommand,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(16.dp)) {
        val stroke = 2.dp.toPx()
        val left = 3.dp.toPx()
        val right = size.width - 3.dp.toPx()
        val top = 4.dp.toPx()
        val bottom = size.height - 4.dp.toPx()
        val midX = size.width / 2f
        val midY = size.height / 2f
        val color = GraphiteMint.InkPrimary
        when (command) {
            RemoteCommand.Up -> {
                drawLine(color, Offset(midX, top), Offset(right, bottom), stroke)
                drawLine(color, Offset(midX, top), Offset(left, bottom), stroke)
            }
            RemoteCommand.Down -> {
                drawLine(color, Offset(midX, bottom), Offset(right, top), stroke)
                drawLine(color, Offset(midX, bottom), Offset(left, top), stroke)
            }
            RemoteCommand.Left -> {
                drawLine(color, Offset(left, midY), Offset(right, top), stroke)
                drawLine(color, Offset(left, midY), Offset(right, bottom), stroke)
            }
            RemoteCommand.Right, RemoteCommand.Enter -> {
                drawLine(color, Offset(right, midY), Offset(left, top), stroke)
                drawLine(color, Offset(right, midY), Offset(left, bottom), stroke)
            }
        }
    }
}

/**
 * "It worked" recovery action: visible only while Unconfirmed, 48dp minimum.
 * Confirms the outstanding generation with no submit and no frame.
 */
@Composable
private fun ItWorkedButton(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.remote_it_worked)
    val cd = stringResource(R.string.remote_it_worked_cd)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(GraphiteMintRadii.Md))
            .background(GraphiteMint.SurfaceKey)
            .border(1.dp, GraphiteMint.Hairline, RoundedCornerShape(GraphiteMintRadii.Md))
            .clickable(role = Role.Button, onClick = onConfirm)
            .semantics(mergeDescendants = true) { contentDescription = "$label. $cd" }
            .heightIn(min = 48.dp)
            .padding(
                horizontal = GraphiteMintSpacing.X5,
                vertical = GraphiteMintSpacing.X3,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = GraphiteMintType.Button.copy(color = GraphiteMint.InkPrimary),
        )
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
 * Command-state ring: idle has no glow, submitted breathes pale mint once
 * over 900ms, unconfirmed and observed hold their amber/mint glow while the
 * tracker holds the state, offline carries a salmon glow. Static in every
 * motion setting beyond the single submitted breath; the text twin below
 * carries the meaning, so the ring stays out of the accessibility tree and
 * focus order.
 *
 * @param generation the tracker generation behind the ring. The breath
 * effect restarts on it, so every new Submitted replays the 900ms breath
 * exactly once even when the ring enum does not change (supersede).
 */
@Composable
fun RemoteRing(
    ring: RemoteRing,
    diameter: Dp,
    generation: Long,
    modifier: Modifier = Modifier,
) {
    val base = when (ring) {
        RemoteRing.Idle -> GraphiteMint.RingIdle
        RemoteRing.Submitted -> GraphiteMint.RingSubmitted
        RemoteRing.Unconfirmed -> GraphiteMint.RingUnconfirmed
        RemoteRing.Observed -> GraphiteMint.RingObserved
        RemoteRing.Offline -> GraphiteMint.RingOffline
    }
    // One 900ms breath per Submitted press: the effect restarts on the
    // tracker generation, so a superseding tap replays instead of holding a
    // stale 1f target. Every other state snaps the halo back to rest.
    val breath = remember { Animatable(0f) }
    LaunchedEffect(ring, generation) {
        if (ring == RemoteRing.Submitted) {
            breath.snapTo(0f)
            breath.animateTo(1f, tween(900))
        } else {
            breath.snapTo(0f)
        }
    }
    val haloAlpha = when (ring) {
        RemoteRing.Idle -> 0f
        RemoteRing.Submitted -> 0.18f * breath.value
        else -> 0.18f
    }
    // Purely visual: the state line below carries the label, so the ring
    // stays out of the accessibility tree and focus order.
    Canvas(modifier = modifier.size(diameter)) {
        val stroke = 2.dp.toPx()
        if (haloAlpha > 0f) {
            drawCircle(
                color = base.copy(alpha = haloAlpha),
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
 * 24dp-minimum centered state line: a 6dp lamp plus 13sp/500 outcome copy.
 * Sent for Submitted, Waiting for TV… for Unconfirmed, TV confirmed for
 * machine evidence, You confirmed for the "It worked" tap, Offline only
 * when no usable socket remains. Polite live region announcing once per
 * change; never socket-health copy.
 */
@Composable
fun RemoteStateLine(
    ring: RemoteRing,
    outcome: CommandOutcome,
    modifier: Modifier = Modifier,
) {
    val idleText = stringResource(R.string.remote_idle)
    val offlineText = stringResource(R.string.remote_offline)
    val sentText = stringResource(R.string.remote_sent)
    val waitingText = stringResource(R.string.remote_waiting)
    val tvConfirmedText = stringResource(R.string.remote_tv_confirmed)
    val youConfirmedText = stringResource(R.string.remote_you_confirmed)
    val text = when (ring) {
        RemoteRing.Offline -> offlineText
        RemoteRing.Idle -> idleText
        RemoteRing.Submitted -> sentText
        RemoteRing.Unconfirmed -> waitingText
        RemoteRing.Observed -> {
            val confirmedByUser = (outcome as? CommandOutcome.Observed)?.userConfirmed == true
            if (confirmedByUser) youConfirmedText else tvConfirmedText
        }
    }
    val lamp = when (ring) {
        RemoteRing.Idle -> GraphiteMint.RingIdle
        RemoteRing.Submitted -> GraphiteMint.RingSubmitted
        RemoteRing.Unconfirmed -> GraphiteMint.RingUnconfirmed
        RemoteRing.Observed -> GraphiteMint.RingObserved
        RemoteRing.Offline -> GraphiteMint.RingOffline
    }
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
