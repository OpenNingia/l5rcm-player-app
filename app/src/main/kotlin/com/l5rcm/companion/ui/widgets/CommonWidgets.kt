package com.l5rcm.companion.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.l5rcm.companion.ui.theme.L5RTheme
import com.l5rcm.companion.ui.theme.Radii
import com.l5rcm.companion.ui.theme.Spacing

/**
 * Ornate divider (docs §6.13): two faded sepia hairlines flanking a centred gold
 * fleuron. Marks structural breaks between major blocks — not every horizontal line.
 */
@Composable
fun OrnateDivider(modifier: Modifier = Modifier, glyph: String = "❖") {
    val colors = L5RTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Hairline(Modifier.weight(1f))
        Text(
            text = glyph,
            style = L5RTheme.type.heading2.copy(color = colors.accentGold.copy(alpha = 0.55f)),
            modifier = Modifier.padding(horizontal = Spacing.s3),
        )
        Hairline(Modifier.weight(1f))
    }
}

@Composable
private fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(1.dp)
            .background(L5RTheme.colors.parchmentBorder.copy(alpha = 0.6f)),
    )
}

/** Plain 1px in-panel rule (docs §6.13). */
@Composable
fun PlainRule(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(L5RTheme.colors.parchmentBorder),
    )
}

/**
 * Clan-tinted content panel (docs §6 / §12.3): gold Cinzel title, hairline rule,
 * subtle bordered card surface.
 */
@Composable
fun SheetPanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = L5RTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radii.card)
            .background(colors.accentPrimary.copy(alpha = 0.10f))
            .border(1.dp, colors.parchmentBorder, Radii.card)
            .padding(Spacing.s4),
    ) {
        Text(
            text = title.uppercase(),
            style = L5RTheme.type.heading2.copy(color = colors.accentGold),
        )
        PlainRule(Modifier.padding(top = Spacing.s2, bottom = Spacing.s3))
        content()
    }
}

/** Section header with kanji watermark + Cinzel title (docs §4.2 content header). */
@Composable
fun SectionHeader(title: String, kanji: String, modifier: Modifier = Modifier) {
    val colors = L5RTheme.colors
    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            text = kanji,
            style = L5RTheme.type.display.copy(
                color = colors.ink.copy(alpha = 0.10f),
                fontSize = 56.sp,
            ),
            modifier = Modifier.align(Alignment.CenterEnd),
        )
        Column {
            Text(title.uppercase(), style = L5RTheme.type.display.copy(color = colors.ink))
            PlainRule(Modifier.padding(top = Spacing.s2))
        }
    }
}

/** A ring card (docs §6.11): ring-coloured surface, big EB Garamond numeral, white text. */
@Composable
fun RingCard(
    ringId: String,
    ringName: String,
    rank: Int,
    modifier: Modifier = Modifier,
) {
    val colors = L5RTheme.colors
    Column(
        modifier = modifier
            .clip(Radii.card)
            .background(colors.ringColor(ringId))
            .padding(Spacing.s3),
    ) {
        Text("$rank", style = L5RTheme.type.statLarge.copy(color = Color.White))
        Text(ringName.uppercase(), style = L5RTheme.type.heading2.copy(color = Color.White))
    }
}

/**
 * Honor/Glory dot track (docs §6.12): score left of a row of dots, one dot per tenth.
 * Read-only here (no scroll-to-edit).
 */
@Composable
fun PointTrack(
    label: String,
    score: Double,
    filledColor: Color,
    modifier: Modifier = Modifier,
) {
    val colors = L5RTheme.colors
    val rank = score.toInt()
    val tenths = ((score - rank) * 10).toInt().coerceIn(0, 9)
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), style = L5RTheme.type.label.copy(color = colors.ink))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s1)) {
            Text(
                String.format("%.1f", score),
                style = L5RTheme.type.statMedium.copy(color = filledColor),
                modifier = Modifier.padding(end = Spacing.s2),
            )
            for (i in 1..9) {
                Dot(filled = i <= tenths, color = filledColor)
            }
        }
    }
}

@Composable
private fun Dot(filled: Boolean, color: Color) {
    Box(
        Modifier
            .size(14.dp)
            .clip(CircleShape)
            .then(
                if (filled) Modifier.background(color)
                else Modifier.border(1.5.dp, color, CircleShape),
            ),
    )
}

/** Two-column label/value row used throughout the sheet sections. */
@Composable
fun StatRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = L5RTheme.type.body.copy(color = L5RTheme.colors.inkMuted))
        Text(value, style = L5RTheme.type.body.copy(color = L5RTheme.colors.ink), textAlign = TextAlign.End)
    }
}
