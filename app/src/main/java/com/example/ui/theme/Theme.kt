package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
    darkColorScheme(
        primary = NeonCyan,
        onPrimary = Color(0xFF041E26),
        primaryContainer = Color(0xFF083344),
        onPrimaryContainer = Color(0xFFA5F3FC),
        secondary = NeonEmerald,
        onSecondary = Color(0xFF064E3B),
        secondaryContainer = Color(0xFF065F46),
        onSecondaryContainer = Color(0xFFA7F3D0),
        tertiary = NeonPurple,
        onTertiary = Color(0xFF2E1065),
        tertiaryContainer = Color(0xFF4C1D95),
        onTertiaryContainer = Color(0xFFDDD6FE),
        background = CyberDarkBg,
        onBackground = TextPrimary,
        surface = CyberSurface,
        onSurface = TextPrimary,
        surfaceVariant = CyberSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        outline = CyberBorder,
        outlineVariant = Color(0xFF1E293B),
        error = NeonRose,
        onError = Color(0xFF4C0519)
    )

private val LightColorScheme = DarkColorScheme // Default to high-contrast cyber theme for retro sandbox gaming

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep distinctive cyber-gaming palette
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

