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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.l5rcm.companion.ui.theme.L5RTheme
import com.l5rcm.companion.ui.theme.Spacing

private fun KeypadField.title(): String = when (this) {
    KeypadField.ROLLED -> "Rolled dice"
    KeypadField.KEPT -> "Kept dice"
    KeypadField.BONUS -> "Bonus"
    KeypadField.TN -> "Target Number"
}

private fun KeypadField.hint(): String = when (this) {
    KeypadField.ROLLED -> "1–10"
    KeypadField.KEPT -> "1–rolled dice"
    KeypadField.BONUS -> "0–50, signed ±"
    KeypadField.TN -> "5–120"
}

/** Contents of the numeric keypad bottom sheet: title/hint, live value, and a 3×4 key grid. */
@Composable
fun KeypadContent(
    state: KeypadState,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onSign: () -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = L5RTheme.colors
    val shown = buildString {
        if (state.field.signed && state.negative) append("−")
        append(state.buffer.ifEmpty { "0" })
    }
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.s4, vertical = Spacing.s4),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            SectionLabel(state.field.title())
            Text(shown, style = DiceType.total.copy(color = colors.ink))
            Text(state.field.hint(), style = L5RTheme.type.caption.copy(color = colors.inkFaint))
        }

        // Rows 1-3: digits 1..9.
        for (row in 0 until 3) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
                for (col in 1..3) {
                    val digit = row * 3 + col
                    Key(digit.toString()) { onDigit(digit) }
                }
            }
        }
        // Row 4: sign-or-backspace, 0, commit.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            if (state.field.signed) {
                Key(if (state.negative) "−" else "+", onClick = onSign)
            } else {
                Key("⌫", onClick = onBackspace)
            }
            Key("0") { onDigit(0) }
            Key("✓", accent = colors.accentCrimson, fg = colors.paperLight, onClick = onCommit)
        }
        if (state.field.signed) {
            // Signed field keeps backspace reachable on its own row.
            Row(Modifier.fillMaxWidth()) { Key("⌫", onClick = onBackspace) }
        }
    }
}

@Composable
private fun RowScope.Key(
    label: String,
    accent: Color? = null,
    fg: Color = L5RTheme.colors.ink,
    onClick: () -> Unit,
) {
    val colors = L5RTheme.colors
    val bg = accent ?: colors.paperDark
    Box(
        Modifier
            .weight(1f)
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, colors.parchmentBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = L5RTheme.type.statMedium.copy(color = fg))
    }
}
