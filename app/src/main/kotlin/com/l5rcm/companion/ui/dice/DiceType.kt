package com.l5rcm.companion.ui.dice

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.l5rcm.companion.ui.theme.FontCinzel
import com.l5rcm.companion.ui.theme.FontEBGaramond

/**
 * Type sizes that only the dice roller needs (`design_handoff_l5r_dice_roller` §Typography). They
 * live here — the one place — so the screen never inlines sizes; they still build on the shared
 * font-family tokens from `theme/Type.kt`, so bundling the real faces later flows through.
 * Colours are always applied by the caller from `L5RTheme.colors`.
 */
object DiceType {
    /** The giant Roll & Keep notation (`6k3+5`). */
    val notation = TextStyle(fontFamily = FontEBGaramond, fontWeight = FontWeight.SemiBold, fontSize = 52.sp)

    /** The result total. */
    val total = TextStyle(fontFamily = FontEBGaramond, fontWeight = FontWeight.SemiBold, fontSize = 56.sp)

    /** A config tile's current value. */
    val configValue = TextStyle(fontFamily = FontEBGaramond, fontWeight = FontWeight.Medium, fontSize = 28.sp)

    /** A kept die's value. */
    val keptDie = TextStyle(fontFamily = FontEBGaramond, fontWeight = FontWeight.SemiBold, fontSize = 24.sp)

    /** A discarded die's value. */
    val discardDie = TextStyle(fontFamily = FontEBGaramond, fontWeight = FontWeight.Normal, fontSize = 18.sp)

    /** Gold roll-type header above the notation (`DESTREZZA + KENJUTSU`). */
    val rollHeader = TextStyle(
        fontFamily = FontCinzel, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.14.em,
    )

    /** 10px uppercase section labels. */
    val sectionLabel = TextStyle(
        fontFamily = FontCinzel, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 0.08.em,
    )

    /** Segmented-control / button label. */
    val button = TextStyle(
        fontFamily = FontCinzel, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.06.em,
    )

    /** The primary "TIRA I DADI" call-to-action. */
    val rollButton = TextStyle(
        fontFamily = FontCinzel, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.06.em,
    )
}
