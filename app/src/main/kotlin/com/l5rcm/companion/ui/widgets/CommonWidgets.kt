package com.l5rcm.companion.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.l5rcm.companion.ui.theme.L5RTheme
import com.l5rcm.companion.ui.theme.Radii
import com.l5rcm.companion.ui.theme.Spacing
import kotlin.math.roundToInt

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
 * A 1px hairline that fades in from transparent at both ends to a solid sepia border in the
 * middle (design handoff "plain rule": `transparent → #C8B89A 15%…85% → transparent`).
 */
@Composable
fun GradientRule(modifier: Modifier = Modifier) {
    val border = L5RTheme.colors.parchmentBorder
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    0.0f to Color.Transparent,
                    0.15f to border,
                    0.85f to border,
                    1.0f to Color.Transparent,
                ),
            ),
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

/**
 * A ring card (docs §6.11 / design handoff): ring-coloured surface, big EB Garamond numeral +
 * Cinzel ring name, its two attributes as name/value rows, and a faint element-kanji watermark
 * peeking from the bottom-right corner. Pass an empty [attrs] for the Void card (value + name only).
 */
@Composable
fun RingCard(
    ringId: String,
    ringName: String,
    value: Int,
    kanji: String,
    attrs: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
) {
    val colors = L5RTheme.colors
    val white = Color.White
    Box(
        modifier
            .shadow(4.dp, Radii.card)
            .clip(Radii.card)
            .background(colors.ringColor(ringId))
            .padding(14.dp),
    ) {
        // Decorative element kanji, faint, peeking from the bottom-right (clipped by the card).
        Text(
            kanji,
            style = L5RTheme.type.body.copy(color = white.copy(alpha = 0.10f), fontSize = 60.sp),
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 18.dp),
        )
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$value", style = L5RTheme.type.statLarge.copy(color = white))
                Spacer(Modifier.width(Spacing.s2))
                Text(
                    ringName.uppercase(),
                    style = L5RTheme.type.heading2.copy(
                        color = white,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                    ),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            if (attrs.isNotEmpty()) {
                Column(
                    Modifier.padding(top = Spacing.s3),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s1),
                ) {
                    attrs.forEach { (name, v) ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(name, style = L5RTheme.type.body.copy(color = white.copy(alpha = 0.88f)))
                            Text(
                                "$v",
                                style = L5RTheme.type.statSmall.copy(
                                    color = white,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A unified Honor/Glory/Status track (design handoff "Social / Spiritual"): a colour swatch + name,
 * the `N,N` score (comma decimal), and a row of nine numbered dots — one per tenth of a rank. Dots
 * up to the current tenth are filled (paper-coloured digit on the stat colour); the rest are
 * outlined (stat-coloured digit). Read-only. [showTopBorder] draws the inter-row divider.
 */
@Composable
fun VirtueRow(
    name: String,
    score: Double,
    color: Color,
    showTopBorder: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = L5RTheme.colors
    val rank = score.toInt()
    val tenths = ((score - rank) * 10).roundToInt().coerceIn(0, 9)
    Column(modifier.fillMaxWidth()) {
        if (showTopBorder) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.paperDark))
        }
        Column(Modifier.padding(top = if (showTopBorder) Spacing.s4 else 0.dp, bottom = Spacing.s4)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                Box(Modifier.size(11.dp).clip(CircleShape).background(color))
                Text(name.uppercase(), style = L5RTheme.type.label.copy(color = color))
            }
            Row(
                Modifier.padding(top = Spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
            ) {
                Text("$rank,$tenths", style = L5RTheme.type.statMedium.copy(color = color))
                NumberedDotRow(tenths, color)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NumberedDotRow(tenths: Int, color: Color) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        for (i in 1..9) NumberedDot(digit = i, filled = i <= tenths, color = color)
    }
}

@Composable
private fun NumberedDot(digit: Int, filled: Boolean, color: Color) {
    val colors = L5RTheme.colors
    Box(
        Modifier
            .size(20.dp)
            .clip(CircleShape)
            .then(if (filled) Modifier.background(color) else Modifier.border(1.5.dp, color, CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$digit",
            style = L5RTheme.type.statSmall.copy(
                color = if (filled) colors.paper else color,
                fontSize = 9.sp,
            ),
        )
    }
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
