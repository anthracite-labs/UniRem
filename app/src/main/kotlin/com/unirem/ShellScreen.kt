package com.unirem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import com.unirem.ui.graphitemint.GraphiteMint
import com.unirem.ui.graphitemint.GraphiteMintSpacing
import com.unirem.ui.graphitemint.GraphiteMintType

/**
 * Foundation shell: a static screen that names UniRem as a local remote for
 * Samsung TVs and shows the build version. No actions, no state claims, no
 * "connected" language. Command surfaces arrive with their stories.
 */
@Composable
fun ShellScreen(versionName: String) {
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
        }
    }
}
