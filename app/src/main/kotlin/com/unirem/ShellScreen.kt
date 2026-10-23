package com.unirem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import com.unirem.core.api.LanAccess
import com.unirem.gate.DiagnosticReadout
import com.unirem.gate.ExplainerSheet
import com.unirem.gate.GatePrimaryAction
import com.unirem.gate.GateQuietAction
import com.unirem.gate.LanGateSnapshot
import com.unirem.gate.PermissionCard
import com.unirem.setup.DisabledLauncher
import com.unirem.ui.graphitemint.GraphiteMint
import com.unirem.ui.graphitemint.GraphiteMintSpacing
import com.unirem.ui.graphitemint.GraphiteMintType

/**
 * Foundation shell with the Story 1.5 setup entry. With no stored TV the
 * shell reads "No TVs yet." plus the quiet code readout and a disabled
 * six-slot launcher; "Add your first TV" starts exactly one scan. With a
 * stored TV the setup entry is replaced by the single-TV summary plus the
 * reusable `TvId`-keyed "TV details" trust sheet (Story 1.6); the launcher
 * stays disabled. Session-only targets get no sheet and show the session-only
 * label.
 */
@Composable
fun ShellScreen(
    versionName: String,
    snapshot: LanGateSnapshot,
    explainerVisible: Boolean,
    onGrant: () -> Unit,
    onExplainerContinue: () -> Unit,
    onExplainerDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    hasPairedTv: Boolean = false,
    tvSummary: String? = null,
    onAddTv: () -> Unit = {},
    onShowDetails: () -> Unit = {},
    detailsFocusRequester: FocusRequester? = null,
    sessionOnlyActive: Boolean = false,
    onCheckConnection: () -> Unit = {},
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = GraphiteMintSpacing.MaxColumnWidth)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GraphiteMintSpacing.ScreenX)
                .padding(top = GraphiteMintSpacing.X10),
        ) {
            BasicText(
                text = stringResource(R.string.shell_title),
                style = GraphiteMintType.Display.copy(color = GraphiteMint.InkPrimary),
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
            BasicText(
                text = stringResource(R.string.shell_subtitle),
                style = GraphiteMintType.Body.copy(color = GraphiteMint.InkSecondary),
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.Section))
            BasicText(
                text = stringResource(R.string.shell_version, versionName),
                style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X4))
            PermissionCard(
                state = snapshot,
                onGrant = onGrant,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
            if (hasPairedTv) {
                BasicText(
                    text = stringResource(R.string.setup_shell_summary, tvSummary ?: ""),
                    style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
                )
                Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
                DisabledLauncher(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
                GatePrimaryAction(
                    label = stringResource(R.string.connection_check),
                    description = stringResource(R.string.connection_check_cd),
                    onClick = onCheckConnection,
                )
                Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
                GateQuietAction(
                    label = stringResource(R.string.trust_details),
                    description = stringResource(R.string.trust_details_cd),
                    onClick = onShowDetails,
                    modifier = if (detailsFocusRequester != null) {
                        Modifier.focusRequester(detailsFocusRequester)
                    } else {
                        Modifier
                    },
                )
                Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
            } else {
                if (sessionOnlyActive) {
                    BasicText(
                        text = stringResource(R.string.paired_session_only),
                        style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
                    )
                    Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
                }
                BasicText(
                    text = stringResource(R.string.setup_no_tvs),
                    style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
                )
                Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
                BasicText(
                    text = stringResource(R.string.setup_intro),
                    style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
                )
                Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
                DisabledLauncher(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
                GatePrimaryAction(
                    label = stringResource(R.string.setup_add_first),
                    description = stringResource(R.string.setup_add_first_cd),
                    onClick = onAddTv,
                )
                Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
            }
            DiagnosticReadout(snapshot = snapshot)
        }
        if (explainerVisible) {
            ExplainerSheet(
                denied = snapshot.access == LanAccess.Denied,
                onContinue = onExplainerContinue,
                onNotNow = onExplainerDismiss,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}
