package com.unirem.ui.graphitemint

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

private val LocalGraphiteMintColors = staticCompositionLocalOf { GraphiteMint }

/**
 * Dark-only Graphite Mint theme. Provides the token objects; there is no
 * light theme and no dynamic color. Surfaces are tonal with 1dp hairlines;
 * no shadows, gradients, blur, or glassmorphism.
 */
@Composable
fun GraphiteMintTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGraphiteMintColors provides GraphiteMint) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GraphiteMint.SurfaceBase),
        ) {
            content()
        }
    }
}
