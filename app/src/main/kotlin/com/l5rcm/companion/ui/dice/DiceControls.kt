package com.l5rcm.companion.ui.dice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.l5rcm.companion.ui.theme.L5RTheme
import com.l5rcm.companion.ui.theme.Radii
import com.l5rcm.companion.ui.theme.Spacing

/** One choice in a [SegmentedControl]. */
data class Segment<T>(val value: T, val label: String)

/**
 * A row of mutually-exclusive buttons on a paper-dark track (design §Roll-type / Mode / Skilled).
 * The active segment fills with [activeBg]; inactive segments are transparent muted-ink text.
 */
@Composable
fun <T> SegmentedControl(
    options: List<Segment<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    activeBg: Color = L5RTheme.colors.accentCrimson,
    activeText: Color = L5RTheme.colors.paperLight,
) {
    val colors = L5RTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radii.button)
            .background(colors.paperDark)
            .border(1.dp, colors.parchmentBorder, Radii.button)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { option ->
            val active = option.value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(Radii.button)
                    .background(if (active) activeBg else Color.Transparent)
                    .clickable { onSelect(option.value) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option.label,
                    style = DiceType.button.copy(color = if (active) activeText else colors.inkMuted),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** A rounded state chip (TN, RAISE, INESPERTO, VUOTO, ENFASI…). */
@Composable
fun StatePill(
    text: String,
    fg: Color,
    bg: Color = fg.copy(alpha = 0.12f),
    border: Color = fg.copy(alpha = 0.35f),
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .padding(horizontal = Spacing.s3, vertical = 4.dp),
    ) {
        Text(text, style = DiceType.sectionLabel.copy(color = fg))
    }
}

/** A tappable config tile (DADI / TENUTI / BONUS / TN) that opens the numeric keypad. */
@Composable
fun RowScope.ConfigTile(label: String, value: String, onClick: () -> Unit) {
    val colors = L5RTheme.colors
    Column(
        Modifier
            .weight(1f)
            .clip(Radii.button)
            .background(colors.whiteWash)
            .border(1.dp, colors.parchmentBorder, Radii.button)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.s3),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label.uppercase(), style = DiceType.sectionLabel.copy(color = colors.inkMuted))
        Text(value, style = DiceType.configValue.copy(color = colors.ink), modifier = Modifier.padding(top = 2.dp))
    }
}

/** A round − / + button for the Raise stepper. */
@Composable
fun StepperButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = L5RTheme.colors
    val fg = if (enabled) colors.accentCrimson else colors.disabledText
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(1.5.dp, fg, CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = L5RTheme.type.statMedium.copy(color = fg))
    }
}

/** A 10px uppercase section label. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = DiceType.sectionLabel.copy(color = L5RTheme.colors.inkMuted), modifier = modifier)
}

/** Full-width tappable pill button (Emphasis / Void spend) that fills [activeBg] when [active]. */
@Composable
fun ToggleBar(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    activeBg: Color,
    modifier: Modifier = Modifier,
) {
    val colors = L5RTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(Radii.button)
            .background(if (active) activeBg else colors.whiteWash)
            .border(1.dp, if (active) activeBg else colors.parchmentBorder, Radii.button)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.uppercase(),
            style = DiceType.button.copy(color = if (active) colors.paperLight else colors.inkMuted),
        )
    }
}
