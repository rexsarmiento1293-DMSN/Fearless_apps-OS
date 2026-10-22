package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SovereignDarkColorScheme = darkColorScheme(
    primary = SovereignGold,
    onPrimary = VaultObsidian,
    primaryContainer = VaultCardSurface,
    onPrimaryContainer = SovereignGoldBright,
    secondary = CyberCyan,
    onSecondary = VaultObsidian,
    secondaryContainer = VaultCardBorder,
    onSecondaryContainer = TextPrimary,
    tertiary = TacticalEmerald,
    onTertiary = VaultObsidian,
    error = OutlawCrimson,
    background = VaultObsidian,
    onBackground = TextPrimary,
    surface = VaultSurface,
    onSurface = TextPrimary,
    surfaceVariant = VaultCardSurface,
    onSurfaceVariant = TextSecondary,
    outline = VaultCardBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SovereignDarkColorScheme,
        typography = Typography,
        content = content
    )
}
