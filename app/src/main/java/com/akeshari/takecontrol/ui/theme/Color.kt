package com.akeshari.takecontrol.ui.theme

import androidx.compose.ui.graphics.Color

// ── Bold & Graphic Palette ──────────────────────────────────────────────────

// Base
val Background = Color(0xFF0A0E1A)       // Deep navy
val Surface = Color(0xFF111827)           // Dark panel
val SurfaceVariant = Color(0xFF1E2940)    // Card
val SurfaceHigh = Color(0xFF283352)       // Elevated card

// Primary
val Primary = Color(0xFF00F0FF)           // Electric cyan
val PrimaryDim = Color(0xFF0A8F99)        // Muted cyan
val OnPrimary = Color(0xFF0A0E1A)

// Accent / Risk
val Accent = Color(0xFFFF3B5C)            // Signal red — danger/critical
val Warning = Color(0xFFFFB800)           // Amber
val Safe = Color(0xFF00E676)              // Acid green

// Text
val OnBackground = Color(0xFFF0F4FF)      // Bright white-blue
val OnSurface = Color(0xFFF0F4FF)
val OnSurfaceVar = Color(0xFF7B8BA8)      // Muted label

// Risk colors (mapped to new palette)
val RiskCritical = Accent
val RiskHigh = Color(0xFFFF6B35)
val RiskMedium = Warning
val RiskLow = Safe
val RiskSafe = Primary

// Light theme (kept but also bold)
val PrimaryLight = Color(0xFF0088AA)
val BackgroundLight = Color(0xFFF0F4FF)
val SurfaceLight = Color.White
val SurfaceVariantLight = Color(0xFFE4EAF5)
val OnBackgroundLight = Color(0xFF0A0E1A)
val OnSurfaceLight = Color(0xFF0A0E1A)
val OnSurfaceVariantLight = Color(0xFF5A6980)
