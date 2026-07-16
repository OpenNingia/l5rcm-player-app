package com.l5rcm.companion.ui.sheet

import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.l5rcm.companion.domain.model.CharacterView
import com.l5rcm.companion.domain.model.WoundLevel
import com.l5rcm.companion.domain.rules.WoundStatus
import com.l5rcm.companion.domain.rules.woundStatus
import com.l5rcm.companion.ui.CombatUiState
import com.l5rcm.companion.ui.theme.L5RTheme
import com.l5rcm.companion.ui.theme.Radii
import com.l5rcm.companion.ui.theme.Spacing
import com.l5rcm.companion.ui.widgets.PlainRule
import com.l5rcm.companion.ui.widgets.PointTrack
import com.l5rcm.companion.ui.widgets.RingCard
import com.l5rcm.companion.ui.widgets.SectionHeader
import com.l5rcm.companion.ui.widgets.SheetPanel
import com.l5rcm.companion.ui.widgets.StatRow

/** Renders the body for the selected [section]. */
@Composable
fun SectionContent(
    section: SheetSection,
    view: CharacterView,
    combat: CombatUiState?,
    onDamage: (Int) -> Unit,
    onHeal: (Int) -> Unit,
    onRest: () -> Unit,
    onResetWounds: () -> Unit,
) {
    SectionHeader(section.title, section.kanji)
    when (section) {
        SheetSection.CHARACTER -> CharacterSection(view)
        SheetSection.COMBAT -> CombatSection(view, combat, onDamage, onHeal, onRest, onResetWounds)
        SheetSection.SKILLS -> SkillsSection(view)
        SheetSection.TECHNIQUES -> TechniquesSection(view)
        SheetSection.SPELLS -> SpellsSection(view)
        SheetSection.KATA_KIHO -> KataKihoSection(view)
        SheetSection.MERITS -> MeritsSection(view)
        SheetSection.EQUIPMENT -> EquipmentSection(view)
        SheetSection.MODIFIERS -> ModifiersSection(view)
        SheetSection.NOTES -> NotesSection(view)
        SheetSection.ABOUT -> AboutSection(view)
    }
}

@Composable
private fun CharacterSection(view: CharacterView) {
    // Rings (four elements + void) as colored cards, two per row.
    val ringCards = view.rings.map { Triple(it.ring.id, it.ring.name, it.rank) } +
        Triple("void", "Void", view.voidRank)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        ringCards.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { (id, name, rank) ->
                    RingCard(id, name, rank, modifier = Modifier.weight(1f))
                }
                if (pair.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            }
        }
    }

    SheetPanel("Traits") {
        view.traits.forEach { t ->
            val value = if (t.modifiedRank != t.rank) "${t.modifiedRank} (${t.rank})" else "${t.rank}"
            StatRow(t.trait.id.replaceFirstChar { it.uppercase() }, value)
        }
    }

    SheetPanel("Insight & Honor") {
        StatRow("Insight Rank", "${view.insightRank}")
        StatRow("Insight", "${view.insightValue}")
        PlainRule(Modifier.padding(vertical = Spacing.s2))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s5)) {
            PointTrack("Honor", view.honor, L5RTheme.colors.success)
            PointTrack("Glory", view.glory, L5RTheme.colors.warning)
        }
        StatRow("Status", String.format("%.1f", view.status))
        if (view.taint > 0) StatRow("Taint", String.format("%.1f", view.taint))
        if (view.infamy > 0) StatRow("Infamy", String.format("%.1f", view.infamy))
    }

    SheetPanel("Experience") {
        StatRow("Spent", "${view.xp.spent}")
        StatRow("Available", "${view.xp.available}")
        StatRow("Remaining", "${view.xp.left}")
        if (view.xp.fromFlaws > 0) StatRow("From flaws", "+${view.xp.fromFlaws}")
    }

    SheetPanel("Wounds & Defense") {
        StatRow("Earth", "${view.health.earthRank}")
        StatRow("Wounds", "${view.health.currentWounds} / ${view.health.maxWounds}")
        StatRow("Heal rate", "${view.health.healRate}")
        StatRow("Base TN", "${view.combat.baseTn}")
        StatRow("Full TN (armor)", "${view.combat.fullTn}")
        StatRow("Reduction", "${view.combat.fullRd}")
        StatRow("Initiative", "${view.combat.initiativeRoll}k${view.combat.initiativeKeep}")
        PlainRule(Modifier.padding(vertical = Spacing.s2))
        view.health.levels.forEach { level ->
            val right = buildString {
                level.threshold?.let { append("≤$it") }
                append("  ·  ")
                append(if (level.penalty == null) "Out" else "+${level.penalty}")
            }
            StatRow(level.name, right)
        }
    }
}

// --- Combat tab: wounds tracker (Layer B session overlay) + read-only initiative/defense ---

private enum class WoundDialogKind { DAMAGE, HEAL }

@Composable
private fun CombatSection(
    view: CharacterView,
    combat: CombatUiState?,
    onDamage: (Int) -> Unit,
    onHeal: (Int) -> Unit,
    onRest: () -> Unit,
    onResetWounds: () -> Unit,
) {
    // Until the overlay flow emits, fall back to the derived baseline so the panel is never blank.
    val current = combat?.currentWounds ?: view.health.currentWounds
    val max = combat?.maxWounds ?: view.health.maxWounds
    val healRate = combat?.healRate ?: view.health.healRate
    val status = combat?.status ?: woundStatus(current, view.health.levels)

    var dialog by remember { mutableStateOf<WoundDialogKind?>(null) }

    SheetPanel("Wounds") {
        WoundSummary(current, max, status)
        PlainRule(Modifier.padding(vertical = Spacing.s3))

        // Damage / heal are entered as a specific amount (from a dice roll); Rest applies the
        // nightly heal rate; Reset drops the overlay back to the imported baseline.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
            WoundActionButton(
                "Damage", L5RTheme.colors.accentCrimson, L5RTheme.colors.whiteWash,
                Modifier.weight(1f),
            ) { dialog = WoundDialogKind.DAMAGE }
            WoundActionButton(
                "Heal", L5RTheme.colors.success, L5RTheme.colors.whiteWash,
                Modifier.weight(1f), enabled = current > 0,
            ) { dialog = WoundDialogKind.HEAL }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = Spacing.s2),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            WoundActionButton(
                if (healRate > 0) "Rest −$healRate" else "Rest",
                L5RTheme.colors.paperDark, L5RTheme.colors.ink,
                Modifier.weight(1f), enabled = current > 0,
                onClick = onRest,
            )
            WoundActionButton(
                "Reset", L5RTheme.colors.paperDark, L5RTheme.colors.inkMuted,
                Modifier.weight(1f), enabled = current > 0,
                onClick = onResetWounds,
            )
        }

        PlainRule(Modifier.padding(vertical = Spacing.s3))
        view.health.levels.forEachIndexed { i, level ->
            WoundLevelRow(level, isCurrent = i == status.levelIndex)
        }
    }

    SheetPanel("Initiative & Defense") {
        StatRow("Initiative", "${view.combat.initiativeRoll}k${view.combat.initiativeKeep}")
        StatRow("Base TN", "${view.combat.baseTn}")
        StatRow("Full TN (armor)", "${view.combat.fullTn}")
        StatRow("Reduction", "${view.combat.fullRd}")
    }

    dialog?.let { kind ->
        WoundAmountDialog(
            kind = kind,
            onDismiss = { dialog = null },
            onConfirm = { amount ->
                if (kind == WoundDialogKind.DAMAGE) onDamage(amount) else onHeal(amount)
                dialog = null
            },
        )
    }
}

@Composable
private fun WoundSummary(current: Int, max: Int, status: WoundStatus) {
    val color = woundLevelColor(status)
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("$current / $max", style = L5RTheme.type.statLarge.copy(color = L5RTheme.colors.ink))
            Text("WOUNDS", style = L5RTheme.type.label.copy(color = L5RTheme.colors.inkMuted))
        }
        Column(horizontalAlignment = Alignment.End) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(color.copy(alpha = 0.18f))
                    .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
            ) {
                Text(status.levelName.uppercase(), style = L5RTheme.type.label.copy(color = color))
            }
            Text(
                if (status.isOut) "Incapacitated" else "penalty +${status.penalty ?: 0}",
                style = L5RTheme.type.caption.copy(color = L5RTheme.colors.inkMuted),
                modifier = Modifier.padding(top = Spacing.s1),
            )
        }
    }
}

@Composable
private fun woundLevelColor(status: WoundStatus): Color = when {
    status.isOut -> L5RTheme.colors.error
    status.levelIndex == 0 -> L5RTheme.colors.success
    (status.penalty ?: 0) >= 15 -> L5RTheme.colors.error
    else -> L5RTheme.colors.warning
}

@Composable
private fun WoundLevelRow(level: WoundLevel, isCurrent: Boolean) {
    val colors = L5RTheme.colors
    val right = buildString {
        level.threshold?.let { append("≤$it") }
        append("  ·  ")
        append(if (level.penalty == null) "Out" else "+${level.penalty}")
    }
    val fg = if (isCurrent) colors.ink else colors.inkMuted
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Radii.input)
            .background(if (isCurrent) colors.accentPrimary.copy(alpha = 0.16f) else Color.Transparent)
            .padding(horizontal = Spacing.s2, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(level.name + if (isCurrent) "  ◀" else "", style = L5RTheme.type.body.copy(color = fg))
        Text(right, style = L5RTheme.type.body.copy(color = fg))
    }
}

@Composable
private fun WoundActionButton(
    label: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = L5RTheme.colors
    Box(
        modifier
            .clip(Radii.button)
            .background(if (enabled) bg else colors.paperDark.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = Spacing.s3),
        contentAlignment = Alignment.Center,
    ) {
        Text(label.uppercase(), style = L5RTheme.type.label.copy(color = if (enabled) fg else colors.inkFaint))
    }
}

@Composable
private fun WoundAmountDialog(
    kind: WoundDialogKind,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val amount = text.toIntOrNull()
    val valid = amount != null && amount > 0
    val damage = kind == WoundDialogKind.DAMAGE
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = L5RTheme.colors.paper,
        title = {
            Text(
                if (damage) "Apply damage" else "Heal wounds",
                style = L5RTheme.type.heading2.copy(color = L5RTheme.colors.ink),
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter { it.isDigit() }.take(3) },
                singleLine = true,
                label = { Text(if (damage) "Wounds taken" else "Wounds healed") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
            )
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { amount?.let(onConfirm) }) {
                Text((if (damage) "Damage" else "Heal").uppercase())
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
    )
}

@Composable
private fun SkillsSection(view: CharacterView) {
    if (view.skills.isEmpty()) { EmptyNote("No skills."); return }
    SheetPanel("Skills") {
        view.skills.forEach { s ->
            Column(Modifier.padding(vertical = 3.dp)) {
                StatRow(s.name, "Rank ${s.rank}${s.trait?.let { "  ·  ${it.replaceFirstChar { c -> c.uppercase() }}" } ?: ""}")
                if (s.emphases.isNotEmpty()) {
                    androidx.compose.material3.Text(
                        "Emphases: ${s.emphases.joinToString(", ")}",
                        style = L5RTheme.type.caption.copy(color = L5RTheme.colors.inkMuted),
                    )
                }
            }
        }
    }
}

@Composable
private fun TechniquesSection(view: CharacterView) {
    if (view.techniques.isEmpty()) { EmptyNote("No techniques."); return }
    view.techniques.forEach { t ->
        SheetPanel(t.name) {
            androidx.compose.material3.Text(
                "${t.schoolName ?: ""} — Insight Rank ${t.insightRank}",
                style = L5RTheme.type.caption.copy(color = L5RTheme.colors.inkMuted),
            )
            if (t.description.isNotBlank()) {
                androidx.compose.material3.Text(
                    t.description,
                    style = L5RTheme.type.body.copy(color = L5RTheme.colors.ink),
                    modifier = Modifier.padding(top = Spacing.s2),
                )
            }
        }
    }
}

@Composable
private fun SpellsSection(view: CharacterView) {
    val spells = view.spells
    if (spells.known.isEmpty() && spells.memorized.isEmpty()) { EmptyNote("No spells."); return }
    if (spells.affinities.isNotEmpty() || spells.deficiencies.isNotEmpty()) {
        SheetPanel("Affinities") {
            if (spells.affinities.isNotEmpty()) StatRow("Affinity", spells.affinities.joinToString(", "))
            if (spells.deficiencies.isNotEmpty()) StatRow("Deficiency", spells.deficiencies.joinToString(", "))
        }
    }
    val all = (spells.memorized.ifEmpty { spells.known })
    SheetPanel("Spells") {
        all.forEach { s ->
            Column(Modifier.padding(vertical = 3.dp)) {
                StatRow(s.name, "${s.element?.replaceFirstChar { it.uppercase() } ?: ""}  ·  M${s.mastery}")
                if (s.range.isNotBlank() || s.area.isNotBlank()) {
                    androidx.compose.material3.Text(
                        listOfNotNull(
                            s.range.takeIf { it.isNotBlank() }?.let { "Range: $it" },
                            s.area.takeIf { it.isNotBlank() }?.let { "Area: $it" },
                            s.duration.takeIf { it.isNotBlank() }?.let { "Duration: $it" },
                        ).joinToString("  ·  "),
                        style = L5RTheme.type.caption.copy(color = L5RTheme.colors.inkMuted),
                    )
                }
            }
        }
    }
}

@Composable
private fun KataKihoSection(view: CharacterView) {
    if (view.katas.isEmpty() && view.kihos.isEmpty()) { EmptyNote("No kata or kiho."); return }
    if (view.katas.isNotEmpty()) {
        SheetPanel("Kata") { view.katas.forEach { NamedBlock(it.name, it.description) } }
    }
    if (view.kihos.isNotEmpty()) {
        SheetPanel("Kiho") { view.kihos.forEach { NamedBlock(it.name, it.description) } }
    }
}

@Composable
private fun MeritsSection(view: CharacterView) {
    if (view.merits.isEmpty() && view.flaws.isEmpty()) { EmptyNote("No merits or flaws."); return }
    if (view.merits.isNotEmpty()) {
        SheetPanel("Merits") { view.merits.forEach { StatRow(it.name, "Rank ${it.rank}") } }
    }
    if (view.flaws.isNotEmpty()) {
        SheetPanel("Flaws") { view.flaws.forEach { StatRow(it.name, "Rank ${it.rank}") } }
    }
}

@Composable
private fun EquipmentSection(view: CharacterView) {
    SheetPanel("Wealth") {
        StatRow("Koku", "${view.money.koku}")
        StatRow("Bu", "${view.money.bu}")
        StatRow("Zeni", "${view.money.zeni}")
    }
    view.armor?.let {
        SheetPanel("Armor") {
            StatRow(it.name.ifBlank { "Armor" }, "TN +${it.tn}  ·  RD ${it.rd}")
        }
    }
    if (view.weapons.isNotEmpty()) {
        SheetPanel("Weapons") {
            view.weapons.forEach { w ->
                Column(Modifier.padding(vertical = 3.dp)) {
                    StatRow(w.name, w.damageRoll)
                    val sub = listOfNotNull(
                        w.skillName.takeIf { it.isNotBlank() },
                        w.range.takeIf { it.isNotBlank() },
                        w.tags.takeIf { it.isNotEmpty() }?.joinToString(", "),
                    ).joinToString("  ·  ")
                    if (sub.isNotBlank()) {
                        androidx.compose.material3.Text(
                            sub,
                            style = L5RTheme.type.caption.copy(color = L5RTheme.colors.inkMuted),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModifiersSection(view: CharacterView) {
    if (view.modifiers.isEmpty()) { EmptyNote("No modifiers."); return }
    SheetPanel("Modifiers") {
        view.modifiers.forEach { m ->
            StatRow(
                "${m.target}${m.detail?.let { " ($it)" } ?: ""}",
                "${if (m.value >= 0) "+" else ""}${m.value}${if (!m.active) " (off)" else ""}",
            )
        }
    }
}

@Composable
private fun NotesSection(view: CharacterView) {
    if (view.notesHtml.isBlank()) { EmptyNote("No notes."); return }
    SheetPanel("Notes") {
        val inkArgb = android.graphics.Color.rgb(0x2C, 0x1A, 0x0E) // --color-ink
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                TextView(ctx).apply {
                    textSize = 14f
                    setTextColor(inkArgb)
                }
            },
            update = { tv ->
                tv.text = HtmlCompat.fromHtml(view.notesHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)
            },
        )
    }
}

@Composable
private fun AboutSection(view: CharacterView) {
    if (view.properties.isNotEmpty()) {
        SheetPanel("Personal") {
            view.properties.forEach { (k, v) ->
                if (v.isNotBlank()) StatRow(k.replaceFirstChar { it.uppercase() }, v)
            }
        }
    }
    SheetPanel("Datapacks") {
        view.packRefs.forEach { ref ->
            StatRow(
                "${ref.name.ifBlank { ref.id }}${if (ref.version.isNotBlank()) " v${ref.version}" else ""}",
                if (ref.installed) "installed" else "missing",
            )
        }
    }
}

@Composable
private fun NamedBlock(name: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        androidx.compose.material3.Text(name, style = L5RTheme.type.label.copy(color = L5RTheme.colors.ink))
        if (description.isNotBlank()) {
            androidx.compose.material3.Text(
                description,
                style = L5RTheme.type.body.copy(color = L5RTheme.colors.inkMuted),
            )
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    SheetPanel("") {
        androidx.compose.material3.Text(text, style = L5RTheme.type.body.copy(color = L5RTheme.colors.inkMuted))
    }
}
