package com.l5rcm.companion.ui.dice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.l5rcm.companion.domain.dice.Die
import com.l5rcm.companion.domain.dice.EffectiveRoll
import com.l5rcm.companion.domain.dice.RollMode
import com.l5rcm.companion.domain.dice.RollResult
import com.l5rcm.companion.ui.theme.L5RTheme
import com.l5rcm.companion.ui.theme.Radii
import com.l5rcm.companion.ui.theme.Spacing

/**
 * The outcome card shown after a roll: a success/failure (or open) banner, the big gold total, and
 * the kept vs. discarded dice grids. Exploded kept dice show a spark and their `a+b+c` breakdown.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultCard(effective: EffectiveRoll, result: RollResult, mode: RollMode, modifier: Modifier = Modifier) {
    val colors = L5RTheme.colors
    val kept = remember(result) { result.results.filter { it.kept }.sortedByDescending { it.value } }
    val discarded = remember(result) { result.results.filterNot { it.kept }.sortedByDescending { it.value } }
    val dieCount = kept.size + discarded.size

    // Reveal the dice one by one (~80ms apart, design §Roll animation), then pop the total and
    // fade in the banner once the last die has landed.
    var revealed by remember(result) { mutableIntStateOf(0) }
    LaunchedEffect(result) {
        revealed = 0
        for (i in 0 until dieCount) {
            delay(REVEAL_STAGGER_MS)
            revealed = i + 1
        }
    }
    val finished = revealed >= dieCount

    // totalPop: scale 0.5 → 1.1 → 1 once the reveal finishes.
    val pop = remember(result) { Animatable(0.5f) }
    LaunchedEffect(finished) {
        if (finished) {
            pop.animateTo(
                1f,
                animationSpec = keyframes {
                    durationMillis = 380
                    0.5f at 0
                    1.1f at 260
                    1f at 380
                },
            )
        }
    }
    // bannerIn: opacity 0 → 1 with a small upward slide.
    val appear by animateFloatAsState(
        targetValue = if (finished) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "bannerIn",
    )

    Column(
        modifier
            .fillMaxWidth()
            .clip(Radii.card)
            .background(colors.whiteWash)
            .border(1.dp, colors.parchmentBorder, Radii.card)
            .padding(Spacing.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        Box(
            Modifier.fillMaxWidth().graphicsLayer {
                alpha = appear
                translationY = (1f - appear) * 8.dp.toPx()
            },
            contentAlignment = Alignment.Center,
        ) {
            ResultBanner(effective, result, mode)
        }

        Text(
            "${result.total}",
            style = DiceType.total.copy(color = colors.accentGold),
            modifier = Modifier.graphicsLayer {
                alpha = appear
                scaleX = pop.value
                scaleY = pop.value
            },
        )
        SectionLabel("Total")

        if (kept.isNotEmpty()) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                kept.forEachIndexed { i, die -> RevealDie(i < revealed) { KeptDie(die) } }
            }
        }
        if (discarded.isNotEmpty()) {
            SectionLabel("Discarded")
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s1, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(Spacing.s1),
            ) {
                discarded.forEachIndexed { j, die -> RevealDie(kept.size + j < revealed) { DiscardDie(die) } }
            }
        }
    }
}

/** Fades and scales its die in when [visible] flips true, keeping its grid slot so layout is stable. */
@Composable
private fun RevealDie(visible: Boolean, content: @Composable () -> Unit) {
    val p by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "dieReveal",
    )
    Box(
        Modifier.graphicsLayer {
            alpha = p
            scaleX = 0.6f + 0.4f * p
            scaleY = 0.6f + 0.4f * p
        },
    ) {
        content()
    }
}

private const val REVEAL_STAGGER_MS = 80L

@Composable
private fun ResultBanner(effective: EffectiveRoll, result: RollResult, mode: RollMode) {
    val colors = L5RTheme.colors
    if (mode == RollMode.OPEN) {
        Text("OPEN ROLL", style = DiceType.rollHeader.copy(color = colors.inkMuted))
        return
    }
    val success = result.total >= effective.effTn
    val fg = if (success) colors.success else colors.accentCrimson
    Box(
        Modifier
            .fillMaxWidth()
            .clip(Radii.button)
            .background(if (success) colors.success.copy(alpha = 0.12f) else colors.accentCrimsonBg)
            .border(1.dp, fg.copy(alpha = if (success) 0.4f else 0.3f), Radii.button)
            .padding(vertical = Spacing.s2),
        contentAlignment = Alignment.Center,
    ) {
        val verdict = if (success) "SUCCESS" else "FAILURE"
        Text("$verdict · TN ${effective.effTn}", style = DiceType.button.copy(color = fg))
    }
}

@Composable
private fun KeptDie(die: Die) {
    val colors = L5RTheme.colors
    Column(
        Modifier
            .width(46.dp)
            .clip(Radii.button)
            .background(colors.paperLight)
            .border(2.dp, colors.accentGold, Radii.button)
            .padding(vertical = Spacing.s1),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("${die.value}", style = DiceType.keptDie.copy(color = colors.accentGold))
        if (die.exploded) {
            SparkGlyph()
            Text(
                die.rolls.joinToString("+"),
                style = DiceType.sectionLabel.copy(color = colors.inkMuted),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DiscardDie(die: Die) {
    val colors = L5RTheme.colors
    Box(
        Modifier
            .size(width = 38.dp, height = 42.dp)
            .clip(Radii.button)
            .background(colors.paperDark)
            .border(1.dp, colors.parchmentBorder, Radii.button),
        contentAlignment = Alignment.Center,
    ) {
        Text("${die.value}", style = DiceType.discardDie.copy(color = colors.inkFaint))
    }
}

/** The exploded-die marker: a gold spark pulsing forever (design §spark, 1.5s alpha + scale). */
@Composable
private fun SparkGlyph() {
    val transition = rememberInfiniteTransition(label = "spark")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1500), RepeatMode.Reverse),
        label = "sparkPulse",
    )
    Text(
        "✦",
        style = DiceType.sectionLabel.copy(color = L5RTheme.colors.accentGoldLight),
        modifier = Modifier.graphicsLayer {
            alpha = 0.45f + 0.55f * pulse
            val s = 1f + 0.3f * pulse
            scaleX = s
            scaleY = s
        },
    )
}

/** The STORICO list: newest first, each row restorable on tap. */
@Composable
fun HistorySection(
    history: List<HistoryEntry>,
    onClear: () -> Unit,
    onRestore: (HistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = L5RTheme.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("History")
            Text(
                "CLEAR",
                style = DiceType.sectionLabel.copy(color = colors.accentCrimson),
                modifier = Modifier.clickable(onClick = onClear),
            )
        }
        history.forEach { HistoryRow(it, onRestore) }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onRestore: (HistoryEntry) -> Unit) {
    val colors = L5RTheme.colors
    val eff = entry.effective
    val open = entry.config.mode == RollMode.OPEN
    val success = !open && entry.result.total >= eff.effTn

    val badge = when {
        open -> "○"
        success -> "✓"
        else -> "✗"
    }
    val badgeColor = when {
        open -> colors.inkFaint
        success -> colors.success
        else -> colors.accentCrimson
    }

    val flags = buildList {
        if (entry.config.voidSpent) add("Void")
        if (!eff.effSkilled) add("Unskilled")
        if (eff.emphasis) add("Emphasis")
        add(if (open) "Open" else "TN ${eff.effTn}")
        add("kept ${entry.result.kept.joinToString(", ")}")
    }.joinToString(" · ")

    Row(
        Modifier
            .fillMaxWidth()
            .clip(Radii.button)
            .background(colors.whiteWash)
            .border(1.dp, colors.parchmentBorder, Radii.button)
            .clickable { onRestore(entry) }
            .padding(horizontal = Spacing.s3, vertical = Spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
    ) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(badgeColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(badge, style = DiceType.sectionLabel.copy(color = badgeColor))
        }
        Column(Modifier.weight(1f)) {
            Text(eff.notation, style = L5RTheme.type.body.copy(color = colors.ink))
            Text(flags, style = L5RTheme.type.caption.copy(color = colors.inkMuted))
        }
        Text("${entry.result.total}", style = DiceType.keptDie.copy(color = colors.accentGold))
    }
}
