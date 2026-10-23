package com.unirem.capability

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.unirem.R
import com.unirem.core.api.CapabilityInput
import com.unirem.core.api.CapabilityKey
import com.unirem.core.api.CapabilityLabel
import com.unirem.core.api.MatrixRules
import com.unirem.gate.GatePrimaryAction
import com.unirem.gate.GateQuietAction
import com.unirem.tv.MatrixOverlayRow
import com.unirem.ui.graphitemint.GraphiteMint
import com.unirem.ui.graphitemint.GraphiteMintRadii
import com.unirem.ui.graphitemint.GraphiteMintSpacing
import com.unirem.ui.graphitemint.GraphiteMintType

/**
 * Developer-only matrix harness (Story 1.8, debug builds only).
 *
 * Shows the exact model, firmware, network, and asset version (read-only),
 * the per-key asset result with its resolved label, and the overlay rows for
 * this TV. Save replaces the prior row for the same exact key. Copy renders
 * the row as asset JSON for the promotion runbook. Clear asks twice. Stale
 * rows (recorded against an older asset) show as stale with Clear.
 *
 * Opens no socket and sends nothing: every callback is Room or clipboard.
 */
@Composable
fun MatrixHarnessScreen(
    model: String,
    firmware: String,
    networkLabel: String,
    networkWire: String,
    matrixVersion: Int,
    assetRows: List<CapabilityInput.Row>,
    overlayRows: List<MatrixOverlayRow>,
    resolved: Map<CapabilityKey, CapabilityLabel>,
    onSave: (key: CapabilityKey, result: CapabilityInput.Result, note: String) -> Unit,
    onClear: (overlayKey: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedKeyIndex by rememberSaveable { mutableStateOf(0) }
    val results = CapabilityInput.Result.entries
    // Explicit Partial default: never depend on the enum declaration order.
    var selectedResultIndex by rememberSaveable {
        mutableStateOf(results.indexOf(CapabilityInput.Result.Partial))
    }
    var note by rememberSaveable { mutableStateOf("") }
    var confirmClearKey by rememberSaveable { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    val keys = MatrixRules.allKeys
    val selectedKey = keys[selectedKeyIndex.coerceIn(keys.indices)]
    val selectedResult = results[selectedResultIndex.coerceIn(results.indices)]
    val identityLine = stringResource(
        R.string.matrix_harness_identity,
        model.ifBlank { stringResource(R.string.setup_not_reported) },
        firmware.ifBlank { stringResource(R.string.setup_not_reported) },
        networkLabel,
        matrixVersion,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        BasicText(
            text = stringResource(R.string.matrix_harness_title),
            style = GraphiteMintType.Headline.copy(color = GraphiteMint.InkPrimary),
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
        BasicText(
            text = stringResource(R.string.matrix_harness_record_only),
            style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        BasicText(
            text = identityLine,
            style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))

        keys.forEachIndexed { index, key ->
            val asset = assetRows.firstOrNull { row ->
                row.key == key && row.model == model &&
                    row.firmware == firmware &&
                    row.networkType == com.unirem.tv.networkTypeOf(networkWire)
            }
            val overlay = overlayRows.firstOrNull { row ->
                row.capability == key.capability.name && row.scope == key.scope.name &&
                    row.model == model && row.firmware == firmware &&
                    row.networkType == networkWire
            }
            val label = resolved[key]
            HarnessKeyRow(
                key = key,
                assetResult = asset?.result?.name,
                label = label?.name,
                overlayResult = overlay?.result,
                stale = overlay != null && overlay.matrixVersion != matrixVersion,
                selected = index == selectedKeyIndex,
                onSelect = { selectedKeyIndex = index },
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
        }

        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
        BasicText(
            text = selectedKey.capability.name + " · " + selectedKey.scope.name,
            style = GraphiteMintType.Body.copy(color = GraphiteMint.InkPrimary),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        ResultSelector(
            selected = selectedResult,
            onSelect = { result -> selectedResultIndex = results.indexOf(result) },
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        BasicTextField(
            value = note,
            onValueChange = { note = it },
            textStyle = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .border(1.dp, GraphiteMint.Hairline, RoundedCornerShape(GraphiteMintRadii.Md))
                .padding(GraphiteMintSpacing.X3),
        )
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        Row(verticalAlignment = Alignment.CenterVertically) {
            GatePrimaryAction(
                label = stringResource(R.string.matrix_harness_save),
                description = stringResource(R.string.matrix_harness_save_cd),
                onClick = {
                    onSave(selectedKey, selectedResult, note)
                    note = ""
                },
            )
            Spacer(modifier = Modifier.width(GraphiteMintSpacing.X2))
            GateQuietAction(
                label = stringResource(R.string.matrix_harness_copy),
                description = stringResource(R.string.matrix_harness_copy_cd),
                onClick = {
                    val row = CapabilityInput.Row(
                        model = model,
                        firmware = firmware,
                        networkType = com.unirem.tv.networkTypeOf(networkWire),
                        key = selectedKey,
                        result = selectedResult,
                        matrixVersion = matrixVersion,
                    )
                    clipboard.setText(AnnotatedString(CapabilityCopy.rowJson(row)))
                },
            )
        }
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X2))
        SelectionContainer {
            BasicText(
                text = CapabilityCopy.rowJson(
                    CapabilityInput.Row(
                        model = model,
                        firmware = firmware,
                        networkType = com.unirem.tv.networkTypeOf(networkWire),
                        key = selectedKey,
                        result = selectedResult,
                        matrixVersion = matrixVersion,
                    ),
                ),
                style = GraphiteMintType.Code.copy(color = GraphiteMint.InkSecondary),
            )
        }

        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X3))
        val staleRows = overlayRows.filter { it.matrixVersion != matrixVersion }
        val currentRows = overlayRows.filter { it.matrixVersion == matrixVersion }
        if (currentRows.isEmpty() && staleRows.isEmpty()) {
            BasicText(
                text = stringResource(R.string.matrix_harness_no_rows),
                style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkSecondary),
            )
        }
        currentRows.forEach { row ->
            OverlayRowLine(
                row = row,
                stale = false,
                confirming = confirmClearKey == row.overlayKey,
                onClear = {
                    if (confirmClearKey == row.overlayKey) {
                        confirmClearKey = null
                        onClear(row.overlayKey)
                    } else {
                        confirmClearKey = row.overlayKey
                    }
                },
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
        }
        staleRows.forEach { row ->
            OverlayRowLine(
                row = row,
                stale = true,
                confirming = confirmClearKey == row.overlayKey,
                onClear = {
                    if (confirmClearKey == row.overlayKey) {
                        confirmClearKey = null
                        onClear(row.overlayKey)
                    } else {
                        confirmClearKey = row.overlayKey
                    }
                },
            )
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
        }
        Spacer(modifier = Modifier.height(GraphiteMintSpacing.X4))
        GateQuietAction(
            label = stringResource(R.string.setup_back),
            description = stringResource(R.string.setup_back_cd),
            onClick = onBack,
        )
    }
}

@Composable
private fun HarnessKeyRow(
    key: CapabilityKey,
    assetResult: String?,
    label: String?,
    overlayResult: String?,
    stale: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val summary = listOfNotNull(
        "asset " + (assetResult ?: "-"),
        "label " + (label ?: "-"),
        if (overlayResult != null) "overlay " + overlayResult + if (stale) " (stale)" else "" else null,
    ).joinToString(" · ")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GraphiteMintRadii.Md))
            .border(
                1.dp,
                if (selected) GraphiteMint.Accent else GraphiteMint.Hairline,
                RoundedCornerShape(GraphiteMintRadii.Md),
            )
            .clickable(role = Role.Button, onClick = onSelect)
            .semantics(mergeDescendants = true) {
                contentDescription = key.capability.name + " " + key.scope.name + " " + summary
            }
            .padding(GraphiteMintSpacing.X3),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = key.capability.name + " · " + key.scope.name,
                style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkPrimary),
            )
            BasicText(
                text = summary,
                style = GraphiteMintType.Micro.copy(color = GraphiteMint.InkSecondary),
            )
        }
    }
}

@Composable
private fun ResultSelector(
    selected: CapabilityInput.Result,
    onSelect: (CapabilityInput.Result) -> Unit,
) {
    Column {
        CapabilityInput.Result.entries.chunked(3).forEach { chunk ->
            Row {
                chunk.forEach { result ->
                    val active = result == selected
                    BasicText(
                        text = result.name,
                        style = GraphiteMintType.State.copy(
                            color = if (active) GraphiteMint.InkOnAccent else GraphiteMint.InkSecondary,
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(GraphiteMintRadii.Full))
                            .border(
                                1.dp,
                                if (active) GraphiteMint.Accent else GraphiteMint.Hairline,
                                RoundedCornerShape(GraphiteMintRadii.Full),
                            )
                            .clickable(role = Role.Button) { onSelect(result) }
                            .padding(
                                horizontal = GraphiteMintSpacing.X3,
                                vertical = GraphiteMintSpacing.X2,
                            ),
                    )
                    Spacer(modifier = Modifier.width(GraphiteMintSpacing.X1))
                }
            }
            Spacer(modifier = Modifier.height(GraphiteMintSpacing.X1))
        }
    }
}

@Composable
private fun OverlayRowLine(
    row: MatrixOverlayRow,
    stale: Boolean,
    confirming: Boolean,
    onClear: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = row.capability + " · " + row.scope + " → " + row.result,
                style = GraphiteMintType.BodySmall.copy(color = GraphiteMint.InkPrimary),
            )
            BasicText(
                text = if (stale) {
                    stringResource(R.string.matrix_harness_stale)
                } else {
                    "v" + row.matrixVersion
                },
                style = GraphiteMintType.Micro.copy(color = GraphiteMint.InkSecondary),
            )
        }
        GateQuietAction(
            label = if (confirming) {
                stringResource(R.string.matrix_harness_confirm_clear)
            } else {
                stringResource(R.string.matrix_harness_clear)
            },
            description = stringResource(R.string.matrix_harness_clear_cd),
            onClick = onClear,
        )
    }
}
