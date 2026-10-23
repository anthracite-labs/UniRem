package com.unirem.ui.graphitemint

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Graphite Mint tokens, byte-exact from DESIGN.md. Dark-only: there is no
 * light palette, no dynamic color, no shadow, gradient, blur, or glassmorphism.
 * Depth is tonal (surface-base -> surface-raised -> surface-key) plus 1dp
 * hairlines.
 */
object GraphiteMint {
    // Ground.
    val SurfaceBase = Color(0xFF151719)
    val SurfaceRaised = Color(0xFF1E2124)
    val SurfaceKey = Color(0xFF2A2E32)
    val SurfaceKeyPressed = Color(0xFF343A3E)

    // Ink.
    val InkPrimary = Color(0xFFECEEF0)
    val InkSecondary = Color(0xFF98A1A9)
    val InkDisabled = Color(0xFF8D959C)
    val InkOnAccent = Color(0xFF0B1A13)

    // Mint signal. Reserved for the OK key, primary actions, focus, and the
    // observed ring. Never fills cards, banners, or sheets.
    val Accent = Color(0xFF3DDC97)
    val AccentPressed = Color(0xFF22B877)
    val AccentWash = Color(0xFF173A2B)

    // Structure.
    val Hairline = Color(0xFF343A3E)

    // Command-state ring. Reported with a text twin; never color alone.
    val RingIdle = Color(0xFF343A3E)
    val RingSubmitted = Color(0xFF86E9BE)
    val RingObserved = Color(0xFF3DDC97)
    val RingUnconfirmed = Color(0xFFE8D44D)
    val RingOffline = Color(0xFFFF7A6B)

    // Connection health. Reports the socket, never a command outcome.
    val StatusConnected = Color(0xFF3DDC97)
    val StatusReconnecting = Color(0xFFE8D44D)
    val StatusOffline = Color(0xFFFF7A6B)

    // Recovery banner.
    val BannerSurface = Color(0xFF2B1A18)
    val BannerBorder = Color(0xFF5A2A25)
    val BannerInk = Color(0xFFFFD8D3)
    val BannerAction = Color(0xFFFF9D92)

    // Focus and scrim.
    val FocusRing = Color(0xFF86E9BE)
    val Scrim = Color(0xFF0A0B0D)
}

/** Typography roles from DESIGN.md. Set in sp; honored at system font scale. */
object GraphiteMintType {
    val Display = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Light,
        lineHeight = 35.sp,
        letterSpacing = (-0.32).sp,
    )
    val Title = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 29.sp,
        letterSpacing = (-0.12).sp,
    )
    val Headline = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 25.sp,
    )
    val Body = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
        letterSpacing = 0.08.sp,
    )
    val BodySmall = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
        letterSpacing = 0.07.sp,
    )
    val Label = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = 0.96.sp,
    )
    val Micro = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 14.sp,
        letterSpacing = 1.32.sp,
    )
    val Button = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 18.sp,
        letterSpacing = 0.3.sp,
    )
    val State = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 17.sp,
        letterSpacing = 0.26.sp,
    )
    val Key = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 17.sp,
        letterSpacing = 0.15.sp,
    )
    val Code = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 17.sp,
        letterSpacing = 0.24.sp,
    )
}

/** Spacing, radii, and layout bounds from DESIGN.md. 4dp scale. */
object GraphiteMintSpacing {
    val X1: Dp = 4.dp
    val X2: Dp = 8.dp
    val X3: Dp = 12.dp
    val X4: Dp = 16.dp
    val X5: Dp = 20.dp
    val X6: Dp = 24.dp
    val X7: Dp = 32.dp
    val X8: Dp = 40.dp
    val X9: Dp = 48.dp
    val X10: Dp = 64.dp
    val ScreenX: Dp = 20.dp
    val Section: Dp = 24.dp
    val KeyGap: Dp = 8.dp
    val MinTarget: Dp = 48.dp
    val MaxColumnWidth: Dp = 480.dp
}

/** Corner radii from DESIGN.md. */
object GraphiteMintRadii {
    val Sm: Dp = 8.dp
    val Md: Dp = 14.dp
    val Lg: Dp = 18.dp
    val Xl: Dp = 24.dp
    val Full: Dp = 9999.dp
}
