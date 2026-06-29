package com.l5rcm.companion.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** 8px spacing scale (docs §4.1). */
object Spacing {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 24.dp
    val s6 = 32.dp
    val s7 = 48.dp
}

/** Corner radii (docs §6): 6 cards / 3 buttons / 2 inputs. */
object Radii {
    val card = RoundedCornerShape(6.dp)
    val button = RoundedCornerShape(3.dp)
    val input = RoundedCornerShape(2.dp)
}

/** Layout constants (docs §4.2 / §4.4). */
object Layout {
    val sidebarWidth = 240.dp
    val navRowHeight = 44.dp
    val compactBreakpointDp = 760
    val drawerMaxWidth = 280.dp
}

/** Motion durations in ms (docs §9). */
object Motion {
    const val BUTTON = 120
    const val LIST_SELECT = 150
    const val DIALOG_OPEN = 200
}
