package com.l5rcm.companion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

/**
 * Root theme. Provides the L5R palette and type scale via CompositionLocals, applies
 * the active character's clan accent, and exposes a minimal Material 3 ColorScheme so
 * stock Material components inherit sensible parchment colours. Light theme only (docs).
 *
 * Read tokens through [L5RTheme.colors] / [L5RTheme.type] inside composables.
 */
@Composable
fun L5RTheme(
    clanId: String? = null,
    content: @Composable () -> Unit,
) {
    val accent = remember(clanId) { ClanAccent.forClan(clanId) }
    val colors = remember(accent) {
        L5RColors(
            accentPrimary = accent.primary,
            accentPrimaryDark = accent.primaryDark,
            accentSelectedBg = accent.selectedBg,
        )
    }
    val typography = remember { L5RTypography() }

    val material3Scheme = lightColorScheme(
        primary = colors.accentCrimson,
        onPrimary = colors.whiteWash,
        secondary = colors.accentGold,
        background = colors.paper,
        onBackground = colors.ink,
        surface = colors.paper,
        onSurface = colors.ink,
        surfaceVariant = colors.paperDark,
        error = colors.error,
        outline = colors.parchmentBorder,
    )

    CompositionLocalProvider(
        LocalL5RColors provides colors,
        LocalL5RTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = material3Scheme,
            typography = remember(typography) { typography.toMaterial() },
            content = content,
        )
    }
}

/** Convenience accessors so composables can read `L5RTheme.colors` / `L5RTheme.type`. */
object L5RTheme {
    val colors: L5RColors
        @Composable get() = LocalL5RColors.current
    val type: L5RTypography
        @Composable get() = LocalL5RTypography.current
}

/** Maps a handful of our tokens onto Material 3's Typography so stock widgets look right. */
private fun L5RTypography.toMaterial(): Typography {
    val base = Typography()
    return base.copy(
        titleLarge = display,
        titleMedium = heading2,
        labelLarge = label,
        bodyLarge = body,
        bodyMedium = body,
        bodySmall = caption,
    )
}
