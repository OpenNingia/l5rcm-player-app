package com.l5rcm.companion.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The manuscript type families. The design system bundles Cinzel / IM Fell English /
 * EB Garamond as static instances (docs §3.1). The TTF binaries are not yet in the
 * repo, so these fall back to a serif face; drop the fonts in `res/font` and rewire
 * these three vals (one place) to switch the whole app to the real faces.
 */
val FontCinzel: FontFamily = FontFamily.Serif // TODO: bundle Cinzel (titles/labels) — weight 600+
val FontIMFell: FontFamily = FontFamily.Serif // TODO: bundle IM Fell English (body) — 400 only
val FontEBGaramond: FontFamily = FontFamily.Serif // TODO: bundle EB Garamond (stat numbers)

/** Type scale tokens (docs §3.2 / §3.4). */
@Immutable
data class L5RTypography(
    val display: TextStyle = TextStyle(
        fontFamily = FontCinzel, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 0.08.em,
    ),
    val heading1: TextStyle = TextStyle(
        fontFamily = FontCinzel, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 0.08.em,
    ),
    val heading2: TextStyle = TextStyle(
        fontFamily = FontCinzel, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.08.em,
    ),
    val label: TextStyle = TextStyle(
        fontFamily = FontCinzel, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.08.em,
    ),
    val body: TextStyle = TextStyle(
        fontFamily = FontIMFell, fontWeight = FontWeight.Normal, fontSize = 13.sp,
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = FontIMFell, fontWeight = FontWeight.Normal, fontSize = 11.sp,
    ),
    val captionItalic: TextStyle = TextStyle(
        fontFamily = FontIMFell, fontWeight = FontWeight.Normal, fontSize = 11.sp, fontStyle = FontStyle.Italic,
    ),
    val statLarge: TextStyle = TextStyle(
        fontFamily = FontEBGaramond, fontWeight = FontWeight.SemiBold, fontSize = 36.sp,
    ),
    val statMedium: TextStyle = TextStyle(
        fontFamily = FontEBGaramond, fontWeight = FontWeight.Medium, fontSize = 22.sp,
    ),
    val statSmall: TextStyle = TextStyle(
        fontFamily = FontEBGaramond, fontWeight = FontWeight.Normal, fontSize = 15.sp,
    ),
    val xpValue: TextStyle = TextStyle(
        fontFamily = FontEBGaramond, fontWeight = FontWeight.SemiBold, fontSize = 28.sp,
    ),
)

val LocalL5RTypography = staticCompositionLocalOf { L5RTypography() }

/** Decoration helper so headings/labels render uppercase as the spec mandates for Cinzel. */
val NoUnderline = TextDecoration.None
