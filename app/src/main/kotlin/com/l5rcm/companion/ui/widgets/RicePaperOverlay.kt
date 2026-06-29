package com.l5rcm.companion.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.l5rcm.companion.ui.theme.L5RTheme

/**
 * Procedural rice-paper grain (docs §8.1): short ink fibre marks placed by a
 * fixed-seed LCG so the pattern is stable across recompositions (consistent fibres
 * read as "paper"; random ones read as noise). Ink at ~6% opacity. Draw once at a
 * high level (e.g. the screen background) so the grain stays continuous.
 */
@Composable
fun RicePaperOverlay(modifier: Modifier = Modifier) {
    val ink = L5RTheme.colors.ink
    Canvas(modifier = modifier) {
        val markColor = ink.copy(alpha = 0.06f)
        // ~1 mark per 25px² → density tuned down for performance on large surfaces.
        val area = size.width * size.height
        val marks = (area / (25f * 25f)).toInt().coerceIn(0, 6000)
        var seed = 1337L
        fun next(): Float {
            // Numerical Recipes LCG; deterministic for a stable fibre pattern.
            seed = (seed * 1664525L + 1013904223L) and 0xFFFFFFFFL
            return (seed and 0xFFFFFF).toFloat() / 0xFFFFFF.toFloat()
        }
        repeat(marks) {
            val x = next() * size.width
            val y = next() * size.height
            if (next() < 0.7f) {
                drawCircle(markColor, radius = 0.6f, center = Offset(x, y))
            } else {
                val len = 1.5f + next() * 2.5f
                val dx = (next() - 0.5f) * len
                val dy = (next() - 0.5f) * len
                drawLine(markColor, Offset(x, y), Offset(x + dx, y + dy), strokeWidth = 0.7f)
            }
        }
    }
}
