package com.unirem.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.unirem.ui.graphitemint.GraphiteMint
import com.unirem.ui.graphitemint.GraphiteMintRadii
import com.unirem.ui.graphitemint.GraphiteMintSpacing
import com.unirem.ui.graphitemint.GraphiteMintType

/**
 * Shared primary action: an Accent-filled pill with a 48dp target and an
 * explicit content description. Ink only, never an icon or emoji.
 */
@Composable
internal fun GatePrimaryAction(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(GraphiteMintRadii.Md))
            .background(GraphiteMint.Accent)
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
            style = GraphiteMintType.Button.copy(color = GraphiteMint.InkOnAccent),
        )
    }
}

/**
 * Shared quiet action: unfilled text with a 48dp target and an explicit
 * content description.
 */
@Composable
internal fun GateQuietAction(
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
            style = GraphiteMintType.Button.copy(color = GraphiteMint.InkSecondary),
        )
    }
}
