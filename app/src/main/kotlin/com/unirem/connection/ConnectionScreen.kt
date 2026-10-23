package com.unirem.connection

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.remember
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.unirem.R
import com.unirem.setup.SetupTopBar
import com.unirem.tv.channel.ConnectionState
import com.unirem.tv.channel.PowerState
import com.unirem.ui.graphitemint.GraphiteMint
import com.unirem.ui.graphitemint.GraphiteMintRadii
import com.unirem.ui.graphitemint.GraphiteMintSpacing
import com.unirem.ui.graphitemint.GraphiteMintType

/**
 * Socket-scoped status surface (Story 1.7).
 *
 * Opens only from "Check connection" on the paired summary; leaving closes
 * the socket and stops backoff. Shows the TV name, the durable "Paired"
 * label, a health dot plus an always-visible text state, and one oxblood
 * banner at a time. The banner pushes content, never overlays, with one
 * recovery action plus an optional "Not now". No spinner, no command state.
 */
@Composable
fun ConnectionScreen(
    tvName: String,
    state: ConnectionState,
    power: PowerState,
    banner: BannerKind,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onNotNow: () -> Unit,
    onRePair: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val stateText = ConnectionUiRules.textFor(state)
    val a11y = ConnectionUiRules.a11yLabel(tvName, state, power)
    Column(modifier = modifier.fillMaxWidth()) {
        SetupTopBar(onBack = onBack)
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X4))
        BasicText(
            text = tvName,
            style = GraphiteMintType.Headline.copy(color = GraphiteMint.InkPrimary),
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
        BasicText(
            text = stringResource(R.string.connection_paired),
            style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = a11y
            },
        ) {
            HealthDot(state = state, banner = banner)
            Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
            BasicText(
                text = stateText,
                style = GraphiteMintType.State.copy(color = GraphiteMint.InkPrimary),
            )
        }
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
        if (banner != BannerKind.None) {
            ConnectionBanner(
                banner = banner,
                onRetry = onRetry,
                onNotNow = onNotNow,
                onRePair = onRePair,
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
        }
    }
}

/**
 * Health dot: 8dp dot plus a 2dp 18%-alpha halo. Mint when connected, amber
 * when reconnecting or identity-changed, salmon when offline. Pulses only
 * during reconnect; system reduced motion (animator scale 0) removes the
 * pulse. The text state beside it carries the meaning; color never acts
 * alone.
 */
@Composable
fun HealthDot(
    state: ConnectionState,
    banner: BannerKind,
    modifier: Modifier = Modifier,
) {
    val base = when {
        state == ConnectionState.Connected -> GraphiteMint.StatusConnected
        banner == BannerKind.IdentityChanged -> GraphiteMint.StatusReconnecting
        state == ConnectionState.Offline -> GraphiteMint.StatusOffline
        else -> GraphiteMint.StatusReconnecting
    }
    val context = LocalContext.current
    val animatorScale = remember(context) {
        try {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        } catch (e: Exception) {
            1f
        }
    }
    val reducedMotion = animatorScale == 0f
    val pulsing = state == ConnectionState.Reconnecting && !reducedMotion
    if (pulsing) {
        val transition = rememberInfiniteTransition(label = "health")
        val halo by transition.animateFloat(
            initialValue = 0.12f,
            targetValue = 0.24f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "halo",
        )
        Canvas(modifier = modifier.size(16.dp)) {
            drawCircle(color = base.copy(alpha = halo), radius = size.minDimension / 2f)
            drawCircle(color = base, radius = 4.dp.toPx())
        }
    } else {
        Canvas(modifier = modifier.size(16.dp)) {
            drawCircle(color = base.copy(alpha = 0.18f), radius = size.minDimension / 2f)
            drawCircle(color = base, radius = 4.dp.toPx())
        }
    }
}

/**
 * Oxblood recovery banner. Pushes content, max two lines plus the action row,
 * one recovery action plus an optional "Not now", no spinner. Assertive live
 * region only for offline and identity-changed.
 */
@Composable
fun ConnectionBanner(
    banner: BannerKind,
    onRetry: () -> Unit,
    onNotNow: () -> Unit,
    onRePair: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = when (banner) {
        BannerKind.Reconnecting -> stringResource(R.string.connection_reconnecting)
        BannerKind.Offline -> stringResource(R.string.connection_offline)
        BannerKind.IdentityChanged -> stringResource(R.string.connection_identity_changed)
        BannerKind.PairingRequired -> stringResource(R.string.connection_pairing_required)
        BannerKind.None -> return
    }
    val assertive = ConnectionUiRules.isAssertive(banner)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GraphiteMintRadii.Md))
            .background(GraphiteMint.BannerSurface)
            .border(1.dp, GraphiteMint.BannerBorder, RoundedCornerShape(GraphiteMintRadii.Md))
            .padding(GraphiteMintSpacing.X4)
            .semantics {
                if (assertive) liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Assertive
            },
    ) {
        BasicText(
            text = message,
            style = GraphiteMintType.Body.copy(color = GraphiteMint.BannerInk),
            maxLines = 2,
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (ConnectionUiRules.hasRetry(banner)) {
                ConnectionBannerAction(
                    label = stringResource(R.string.connection_retry),
                    description = stringResource(R.string.connection_retry_cd),
                    onClick = onRetry,
                )
                Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
                ConnectionBannerQuiet(
                    label = stringResource(R.string.connection_not_now),
                    description = stringResource(R.string.connection_not_now_cd),
                    onClick = onNotNow,
                )
            }
            if (ConnectionUiRules.hasRePair(banner)) {
                ConnectionBannerAction(
                    label = stringResource(R.string.connection_repair),
                    description = stringResource(R.string.connection_repair_cd),
                    onClick = onRePair,
                )
                Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
                ConnectionBannerQuiet(
                    label = stringResource(R.string.connection_not_now),
                    description = stringResource(R.string.connection_not_now_cd),
                    onClick = onNotNow,
                )
            }
        }
    }
}

@Composable
private fun ConnectionBannerAction(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(GraphiteMintRadii.Md))
            .background(GraphiteMint.BannerAction)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$label $description" }
            .heightIn(min = 48.dp)
            .padding(
                horizontal = GraphiteMintSpacing.X5,
                vertical = GraphiteMintSpacing.X3,
            ),
    ) {
        BasicText(
            text = label,
            style = GraphiteMintType.Button.copy(color = GraphiteMint.SurfaceBase),
            modifier = Modifier.alpha(1f),
        )
    }
}

@Composable
private fun ConnectionBannerQuiet(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$label $description" }
            .heightIn(min = 48.dp)
            .padding(
                horizontal = GraphiteMintSpacing.X4,
                vertical = GraphiteMintSpacing.X3,
            ),
    ) {
        BasicText(
            text = label,
            style = GraphiteMintType.Button.copy(color = GraphiteMint.BannerAction),
        )
    }
}
