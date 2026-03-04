package com.whispervault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── WhisperVault brand palette ──
val VaultPurple       = Color(0xFF7C4DFF)
val VaultPurpleDark   = Color(0xFF5722CC)
val VaultPurpleLight  = Color(0xFFB388FF)
val VaultTeal         = Color(0xFF00BFA5)
val VaultBg           = Color(0xFF0D0D1A)
val VaultSurface      = Color(0xFF1A1A2E)
val VaultCard         = Color(0xFF16213E)
val VaultOnSurface    = Color(0xFFE8E8F0)
val VaultSubtext      = Color(0xFF9090B0)
val VaultError        = Color(0xFFFF5252)
val VaultSuccess      = Color(0xFF69F0AE)
val VaultWarning      = Color(0xFFFFD740)
val TorGreen          = Color(0xFF00E676)
val TorOrange         = Color(0xFFFF6D00)

private val DarkColorScheme = darkColorScheme(
    primary          = VaultPurple,
    onPrimary        = Color.White,
    primaryContainer = VaultPurpleDark,
    secondary        = VaultTeal,
    onSecondary      = Color.Black,
    background       = VaultBg,
    surface          = VaultSurface,
    onSurface        = VaultOnSurface,
    error            = VaultError,
    outline          = VaultSubtext,
    surfaceVariant   = VaultCard,
    onSurfaceVariant = VaultSubtext
)

private val LightColorScheme = lightColorScheme(
    primary          = VaultPurpleDark,
    onPrimary        = Color.White,
    primaryContainer = VaultPurpleLight,
    secondary        = VaultTeal,
    onSecondary      = Color.Black,
    background       = Color(0xFFF5F5FF),
    surface          = Color.White,
    onSurface        = Color(0xFF1A1A2E),
    error            = VaultError,
    outline          = Color(0xFFB0B0C8),
    surfaceVariant   = Color(0xFFEEEEFF),
    onSurfaceVariant = Color(0xFF505070)
)

@Composable
fun WhisperVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = WhisperVaultTypography,
        content     = content
    )
}
