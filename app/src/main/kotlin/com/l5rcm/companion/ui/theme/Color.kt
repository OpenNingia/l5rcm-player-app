package com.l5rcm.companion.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The full L5R palette (docs/L5R_UI_Design_System.md §2). Material 3's ColorScheme
 * can't hold all these tokens, so we expose them via [LocalL5RColors]. Composables
 * must read colours from here, never hard-code hex.
 */
@Immutable
data class L5RColors(
    // Base / neutral
    val paper: Color = Color(0xFFF5EDD6),
    val paperDark: Color = Color(0xFFEDE0C0),
    val paperLight: Color = Color(0xFFFAF4E4),
    val parchmentBorder: Color = Color(0xFFC8B89A),
    val ink: Color = Color(0xFF2C1A0E),
    val inkMuted: Color = Color(0xFF6B4F35),
    val inkFaint: Color = Color(0xFFA08060),
    val whiteWash: Color = Color(0xFFFFFDF5),

    // Accent / semantic
    val accentCrimson: Color = Color(0xFF8B1A1A),
    val accentCrimsonDark: Color = Color(0xFF6B1010),
    val accentCrimsonBg: Color = Color(0xFFF5E0E0),
    val accentGold: Color = Color(0xFFB8860B),
    val accentGoldLight: Color = Color(0xFFD4A017),
    val accentBlue: Color = Color(0xFF4A6FA5),
    val accentBlueDark: Color = Color(0xFF3A5A8A),
    val accentBlueBg: Color = Color(0xFFE0E8F5),

    // Rings
    val ringEarth: Color = Color(0xFF5C4A30),
    val ringAir: Color = Color(0xFF4A7A8A),
    val ringWater: Color = Color(0xFF2A4A7A),
    val ringFire: Color = Color(0xFF8B2A1A),
    val ringVoid: Color = Color(0xFF4A2A6A),

    // Status / feedback
    val success: Color = Color(0xFF3A7A3A),
    val warning: Color = Color(0xFFB87A1A),
    val error: Color = Color(0xFF8B1A1A),
    val errorBorder: Color = Color(0xFFC03030),
    val disabledBg: Color = Color(0xFFDDD0B8),
    val disabledText: Color = Color(0xFFA08060),

    // Clan accent (overridden at runtime — see ClanAccent)
    val accentPrimary: Color = Color(0xFF4A6FA5),
    val accentPrimaryDark: Color = Color(0xFF3A5A8A),
    val accentSelectedBg: Color = Color(0xFFE0E8F5),
) {
    fun ringColor(ringId: String): Color = when (ringId) {
        "earth" -> ringEarth
        "air" -> ringAir
        "water" -> ringWater
        "fire" -> ringFire
        "void" -> ringVoid
        else -> ringEarth
    }
}

val LocalL5RColors = staticCompositionLocalOf { L5RColors() }

/** Clan → (primary, primaryDark) accent pair (docs §5). */
data class ClanAccent(val primary: Color, val primaryDark: Color) {
    /** Selected-list wash: paper tinted ~16% toward the primary. */
    val selectedBg: Color get() = primary.copy(alpha = 0.16f)

    companion object {
        private val MAP = mapOf(
            "crab" to ClanAccent(Color(0xFF5A5A2A), Color(0xFF404020)),
            "crane" to ClanAccent(Color(0xFF4A6FA5), Color(0xFF3A5A8A)),
            "dragon" to ClanAccent(Color(0xFF2A6A4A), Color(0xFF1A5A3A)),
            "lion" to ClanAccent(Color(0xFFB8860B), Color(0xFF8A6208)),
            "mantis" to ClanAccent(Color(0xFF2A7A5A), Color(0xFF1A6A4A)),
            "phoenix" to ClanAccent(Color(0xFF8B4A1A), Color(0xFF6B3A10)),
            "scorpion" to ClanAccent(Color(0xFF8B1A1A), Color(0xFF6B1010)),
            "spider" to ClanAccent(Color(0xFF3A2A4A), Color(0xFF2A1A3A)),
            "unicorn" to ClanAccent(Color(0xFF6A2A6A), Color(0xFF5A1A5A)),
        )
        private val RONIN = ClanAccent(Color(0xFF5A4A3A), Color(0xFF4A3A2A))

        fun forClan(clanId: String?): ClanAccent = MAP[clanId?.lowercase()] ?: RONIN
    }
}
