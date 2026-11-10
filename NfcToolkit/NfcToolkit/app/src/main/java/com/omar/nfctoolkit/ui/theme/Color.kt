package com.omar.nfctoolkit.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E0FF),
    onPrimaryContainer = Color(0xFF13006B),
    secondary = Color(0xFF0D9488),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBDF0E6),
    onSecondaryContainer = Color(0xFF00201B),
    tertiary = Color(0xFFDB2777),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFBFBFE),
    onBackground = Color(0xFF1A1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1B1F),
    surfaceVariant = Color(0xFFE7E7F0),
    onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF767680),
    error = Color(0xFFBA1A1A)
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFBEC2FF),
    onPrimary = Color(0xFF1B2280),
    primaryContainer = Color(0xFF343B9A),
    onPrimaryContainer = Color(0xFFE0E0FF),
    secondary = Color(0xFF5FE9D5),
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF005048),
    onSecondaryContainer = Color(0xFFBDF0E6),
    tertiary = Color(0xFFFFB0CD),
    onTertiary = Color(0xFF5E1140),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE4E2E9),
    surface = Color(0xFF1A1B20),
    onSurface = Color(0xFFE4E2E9),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC6C5D0),
    outline = Color(0xFF90909A),
    error = Color(0xFFFFB4AB)
)

/** Gradient pairs used for the Wallet cards; chosen by a tag's colorSeed. */
val CardGradients = listOf(
    Color(0xFF4F46E5) to Color(0xFF7C3AED),
    Color(0xFF0D9488) to Color(0xFF0EA5E9),
    Color(0xFFDB2777) to Color(0xFFF97316),
    Color(0xFF2563EB) to Color(0xFF06B6D4),
    Color(0xFF7C3AED) to Color(0xFFDB2777),
    Color(0xFF059669) to Color(0xFF84CC16),
    Color(0xFFEA580C) to Color(0xFFFACC15),
    Color(0xFF475569) to Color(0xFF0F172A)
)
