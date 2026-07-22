package com.l5rcm.companion.ui.sheet

import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.l5rcm.companion.domain.dice.RollConfig
import com.l5rcm.companion.domain.dice.RollMode
import com.l5rcm.companion.domain.dice.RollType
import com.l5rcm.companion.domain.model.CharacterView
import com.l5rcm.companion.domain.model.Ring
import com.l5rcm.companion.domain.model.Trait
import com.l5rcm.companion.domain.model.WeaponView
import com.l5rcm.companion.domain.model.WoundLevel
import com.l5rcm.companion.domain.rules.Stance
import com.l5rcm.companion.domain.rules.StanceRules
import com.l5rcm.companion.domain.rules.WoundStatus
import com.l5rcm.companion.domain.rules.woundStatus
import com.l5rcm.companion.ui.CombatUiState
import com.l5rcm.companion.ui.dice.DicePreset
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
    onSetStance: (Stance) -> Unit,
    onSpendVoid: () -> Unit,
    onRegainVoid: () -> Unit,
    onEquipWeapon: (String?) -> Unit,
    onToggleArmor: () -> Unit,
    onSetFullDefenseTotal: (Int) -> Unit,
    onCombatRoll: (DicePreset) -> Unit,
) {
    SectionHeader(section.title, section.kanji)
    when (section) {
        SheetSection.CHARACTER -> CharacterSection(view)
        SheetSection.COMBAT -> CombatSection(
            view, combat, onDamage, onHeal, onRest, onResetWounds,
            onSetStance, onSpendVoid, onRegainVoid, onSetFullDefenseTotal, onCombatRoll,
        )
        SheetSection.SKILLS -> SkillsSection(view)
        SheetSection.TECHNIQUES -> TechniquesSection(view)
        SheetSection.SPELLS -> SpellsSection(view)
        SheetSection.KATA_KIHO -> KataKihoSection(view)
        SheetSection.MERITS -> MeritsSection(view)
        SheetSection.EQUIPMENT -> EquipmentSection(
            view, combat?.equippedWeaponName, combat?.armorEquipped ?: true, onEquipWeapon, onToggleArmor,
        )
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

// --- Combat tab: session overlay (wounds · Void · stance · equipped weapon) + dice presets ---

private enum class WoundDialogKind { DAMAGE, HEAL }

@Composable
private fun CombatSection(
    view: CharacterView,
    combat: CombatUiState?,
    onDamage: (Int) -> Unit,
    onHeal: (Int) -> Unit,
    onRest: () -> Unit,
    onResetWounds: () -> Unit,
    onSetStance: (Stance) -> Unit,
    onSpendVoid: () -> Unit,
    onRegainVoid: () -> Unit,
    onSetFullDefenseTotal: (Int) -> Unit,
    onCombatRoll: (DicePreset) -> Unit,
) {
    // Until the overlay flow emits, fall back to the derived baseline so panels are never blank.
    val current = combat?.currentWounds ?: view.health.currentWounds
    val max = combat?.maxWounds ?: view.health.maxWounds
    val healRate = combat?.healRate ?: view.health.healRate
    val status = combat?.status ?: woundStatus(current, view.health.levels)

    val stance = combat?.stance ?: Stance.ATTACK
    val voidCurrent = combat?.voidCurrent ?: view.voidRank
    val voidMax = combat?.voidMax ?: view.voidRank
    val fullDefenseTotal = combat?.fullDefenseTotal

    // Stance-relevant derived numbers, all read from the character view.
    val airRank = view.rings.firstOrNull { it.ring == Ring.AIR }?.rank ?: 0
    val reflexes = view.traits.firstOrNull { it.trait == Trait.REFLEXES }?.modifiedRank ?: 0
    val defenseRank = view.skills
        .firstOrNull { it.id == "defense" || it.name.equals("Defense", ignoreCase = true) }?.rank ?: 0
    val armorTnDelta = StanceRules.armorTnDelta(stance, airRank, defenseRank, fullDefenseTotal)
    // Removing armor (play-time overlay, Layer B) drops its own TN / RD bonus; the derived sheet
    // (Layer A, Character tab) always shows the armored baseline and is never rewritten here.
    val armorEquipped = combat?.armorEquipped ?: true
    val armorTnBonus = if (armorEquipped) 0 else (view.armor?.tn ?: 0)
    val armoredBaseTn = view.combat.fullTn - armorTnBonus
    val effectiveArmorTn = armoredBaseTn + armorTnDelta
    val effectiveRd = view.combat.fullRd - (if (armorEquipped) 0 else (view.armor?.rd ?: 0))

    var dialog by remember { mutableStateOf<WoundDialogKind?>(null) }
    var fullDefenseDialog by remember { mutableStateOf(false) }

    SheetPanel("Wounds") {
        WoundSummary(current, max, status)
        PlainRule(Modifier.padding(vertical = Spacing.s3))

        // Damage / heal are entered as a specific amount (from a dice roll); Rest applies the
        // nightly heal rate (and refreshes Void); Reset drops wounds back to the imported baseline.
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
                Modifier.weight(1f), enabled = current > 0 || voidCurrent < voidMax,
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

    VoidPanel(voidCurrent, voidMax, onSpendVoid, onRegainVoid)

    StancePanel(
        stance = stance,
        defenseRank = defenseRank,
        baseArmorTn = armoredBaseTn,
        effectiveArmorTn = effectiveArmorTn,
        fullDefenseTotal = fullDefenseTotal,
        onSetStance = onSetStance,
        onRollFullDefense = {
            onCombatRoll(
                DicePreset(
                    RollConfig(
                        mode = RollMode.OPEN,
                        rollType = RollType.SKILL,
                        skilled = defenseRank >= 1,
                        rolled = (defenseRank + reflexes).coerceAtLeast(1),
                        kept = reflexes.coerceAtLeast(1),
                    ),
                    "Difesa Totale — Difesa/Reflessi",
                ),
            )
        },
        onEnterFullDefenseTotal = { fullDefenseDialog = true },
    )

    EquippedWeaponPanel(
        weapon = view.weapons.firstOrNull { it.name == combat?.equippedWeaponName },
        stance = stance,
        onRollAttack = { w ->
            val (bonusRolled, bonusKept) = StanceRules.attackPoolBonus(stance)
            onCombatRoll(
                DicePreset(
                    RollConfig(
                        mode = RollMode.OPEN,
                        rollType = RollType.SKILL,
                        skilled = true,
                        rolled = w.attackRolled + bonusRolled,
                        kept = w.attackKept + bonusKept,
                    ),
                    "Attacco — ${w.name}",
                ),
            )
        },
        onRollDamage = { w ->
            onCombatRoll(
                DicePreset(
                    RollConfig(
                        mode = RollMode.OPEN,
                        rollType = RollType.TRAIT, // damage explodes and takes no Raises
                        rolled = w.damageRolled,
                        kept = w.damageKept,
                    ),
                    "Danno — ${w.name}",
                ),
            )
        },
    )

    SheetPanel("Initiative & Defense") {
        StatRow("Initiative", "${view.combat.initiativeRoll}k${view.combat.initiativeKeep}")
        StatRow("Base TN", "${view.combat.baseTn}")
        StatRow("Armor TN (stance)", "$effectiveArmorTn" + if (!armorEquipped) "  (no armor)" else "")
        StatRow("Reduction", "$effectiveRd" + if (!armorEquipped) "  (no armor)" else "")
        WoundActionButton(
            "Roll initiative", L5RTheme.colors.accentBlue, L5RTheme.colors.whiteWash,
            Modifier.fillMaxWidth().padding(top = Spacing.s3),
        ) {
            onCombatRoll(
                DicePreset(
                    RollConfig(
                        mode = RollMode.OPEN,
                        rollType = RollType.TRAIT,
                        rolled = view.combat.initiativeRoll,
                        kept = view.combat.initiativeKeep,
                    ),
                    "Iniziativa",
                ),
            )
        }
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

    if (fullDefenseDialog) {
        NumberEntryDialog(
            title = "Full Defense",
            label = "Total rolled (Defense/Reflexes)",
            confirmLabel = "Set",
            onDismiss = { fullDefenseDialog = false },
            onConfirm = { total ->
                onSetFullDefenseTotal(total)
                fullDefenseDialog = false
            },
        )
    }
}

// --- Void points: a tappable pip track (tap a filled pip to spend, an empty pip to regain) ---

@Composable
private fun VoidPanel(current: Int, max: Int, onSpend: () -> Unit, onRegain: () -> Unit) {
    SheetPanel("Void Points") {
        if (max <= 0) {
            EmptyNoteInline("No Void Ring — no Void Points to spend.")
            return@SheetPanel
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("$current / $max", style = L5RTheme.type.statLarge.copy(color = L5RTheme.colors.ink))
            Text("VOID", style = L5RTheme.type.label.copy(color = L5RTheme.colors.ringVoid))
        }
        Row(
            Modifier.fillMaxWidth().padding(top = Spacing.s2),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            for (i in 1..max) {
                VoidPip(filled = i <= current, onClick = { if (i <= current) onSpend() else onRegain() })
            }
        }
        Text(
            "Tap a filled point to spend, an empty one to regain. Rest refreshes to full.",
            style = L5RTheme.type.caption.copy(color = L5RTheme.colors.inkMuted),
            modifier = Modifier.padding(top = Spacing.s2),
        )
    }
}

@Composable
private fun VoidPip(filled: Boolean, onClick: () -> Unit) {
    val color = L5RTheme.colors.ringVoid
    Box(
        Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(50))
            .background(if (filled) color else Color.Transparent)
            .then(if (filled) Modifier else Modifier.border(1.5.dp, color, RoundedCornerShape(50)))
            .clickable(onClick = onClick),
    )
}

// --- Stance selector + live Armor TN ---

@Composable
private fun StancePanel(
    stance: Stance,
    defenseRank: Int,
    baseArmorTn: Int,
    effectiveArmorTn: Int,
    fullDefenseTotal: Int?,
    onSetStance: (Stance) -> Unit,
    onRollFullDefense: () -> Unit,
    onEnterFullDefenseTotal: () -> Unit,
) {
    val colors = L5RTheme.colors
    SheetPanel("Stance") {
        Stance.entries.forEach { s ->
            val enabled = StanceRules.canUse(s, defenseRank)
            StanceRow(s, selected = s == stance, enabled = enabled) { if (enabled) onSetStance(s) }
        }
        if (defenseRank < 1) {
            Text(
                "Defensive stances need at least rank 1 in Defense.",
                style = L5RTheme.type.captionItalic.copy(color = colors.inkFaint),
                modifier = Modifier.padding(top = Spacing.s1),
            )
        }
        PlainRule(Modifier.padding(vertical = Spacing.s3))
        Text(stance.effects, style = L5RTheme.type.body.copy(color = colors.inkMuted))
        StatRow("Armor TN", "$effectiveArmorTn" + if (effectiveArmorTn != baseArmorTn) "  (base $baseArmorTn)" else "")

        if (stance == Stance.FULL_DEFENSE) {
            Row(
                Modifier.fillMaxWidth().padding(top = Spacing.s2),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                WoundActionButton(
                    "Roll defense", colors.accentBlue, colors.whiteWash, Modifier.weight(1f),
                    onClick = onRollFullDefense,
                )
                WoundActionButton(
                    if (fullDefenseTotal != null) "Total: $fullDefenseTotal" else "Enter total",
                    colors.paperDark, colors.ink, Modifier.weight(1f),
                    onClick = onEnterFullDefenseTotal,
                )
            }
        }
    }
}

@Composable
private fun StanceRow(stance: Stance, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = L5RTheme.colors
    val fg = when {
        !enabled -> colors.inkFaint
        selected -> colors.accentCrimson
        else -> colors.ink
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Radii.input)
            .background(if (selected) colors.accentPrimary.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Spacing.s2, vertical = Spacing.s2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stance.displayName, style = L5RTheme.type.body.copy(color = fg))
        if (selected) Text("◀", style = L5RTheme.type.body.copy(color = colors.accentCrimson))
    }
}

// --- Equipped weapon: attack + damage rolls ---

@Composable
private fun EquippedWeaponPanel(
    weapon: WeaponView?,
    stance: Stance,
    onRollAttack: (WeaponView) -> Unit,
    onRollDamage: (WeaponView) -> Unit,
) {
    val colors = L5RTheme.colors
    SheetPanel("Equipped Weapon") {
        if (weapon == null) {
            EmptyNoteInline("Equip a weapon from the Equipment section to roll attacks here.")
            return@SheetPanel
        }
        Text(weapon.name, style = L5RTheme.type.heading2.copy(color = colors.ink))
        val sub = listOfNotNull(
            weapon.skillName.takeIf { it.isNotBlank() },
            weapon.range.takeIf { it.isNotBlank() },
        ).joinToString("  ·  ")
        if (sub.isNotBlank()) {
            Text(sub, style = L5RTheme.type.caption.copy(color = colors.inkMuted))
        }
        val bonus = StanceRules.attackPoolBonus(stance)
        StatRow(
            "Attack",
            if (weapon.hasAttack) {
                "${weapon.attackRolled + bonus.first}k${weapon.attackKept + bonus.second}" +
                    if (bonus.first > 0) "  (stance)" else ""
            } else "—",
        )
        StatRow("Damage", weapon.damageRoll.ifBlank { "—" })
        Row(
            Modifier.fillMaxWidth().padding(top = Spacing.s3),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            WoundActionButton(
                "Attack", colors.accentCrimson, colors.whiteWash, Modifier.weight(1f),
                enabled = weapon.hasAttack, onClick = { onRollAttack(weapon) },
            )
            WoundActionButton(
                "Damage", colors.accentGold, colors.ink, Modifier.weight(1f),
                enabled = weapon.hasDamage, onClick = { onRollDamage(weapon) },
            )
        }
    }
}

@Composable
private fun EmptyNoteInline(text: String) {
    Text(text, style = L5RTheme.type.body.copy(color = L5RTheme.colors.inkMuted))
}

/** A single-field positive-integer entry dialog (Full Defense total). Mirrors [WoundAmountDialog]. */
@Composable
private fun NumberEntryDialog(
    title: String,
    label: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val amount = text.toIntOrNull()
    val valid = amount != null && amount >= 0
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = L5RTheme.colors.paper,
        title = { Text(title, style = L5RTheme.type.heading2.copy(color = L5RTheme.colors.ink)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter { it.isDigit() }.take(3) },
                singleLine = true,
                label = { Text(label) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
            )
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { amount?.let(onConfirm) }) {
                Text(confirmLabel.uppercase())
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
    )
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
private fun EquipmentSection(
    view: CharacterView,
    equippedWeapon: String?,
    armorEquipped: Boolean,
    onEquipWeapon: (String?) -> Unit,
    onToggleArmor: () -> Unit,
) {
    SheetPanel("Wealth") {
        StatRow("Koku", "${view.money.koku}")
        StatRow("Bu", "${view.money.bu}")
        StatRow("Zeni", "${view.money.zeni}")
    }
    view.armor?.let {
        SheetPanel("Armor") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    androidx.compose.material3.Text(
                        it.name.ifBlank { "Armor" },
                        style = L5RTheme.type.body.copy(color = L5RTheme.colors.ink),
                    )
                    androidx.compose.material3.Text(
                        "TN +${it.tn}  ·  RD ${it.rd}",
                        style = L5RTheme.type.caption.copy(color = L5RTheme.colors.inkMuted),
                    )
                }
                EquipToggle(armorEquipped) { onToggleArmor() }
            }
            if (!armorEquipped) {
                androidx.compose.material3.Text(
                    "Removed — Armor TN and Reduction drop by the armor's bonus in the Combat tab.",
                    style = L5RTheme.type.caption.copy(color = L5RTheme.colors.inkMuted),
                    modifier = Modifier.padding(top = Spacing.s1),
                )
            }
        }
    }
    if (view.weapons.isNotEmpty()) {
        SheetPanel("Weapons") {
            view.weapons.forEach { w ->
                val equipped = w.name == equippedWeapon
                Column(Modifier.padding(vertical = 3.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Text(
                            w.name,
                            style = L5RTheme.type.body.copy(color = L5RTheme.colors.ink),
                            modifier = Modifier.weight(1f),
                        )
                        androidx.compose.material3.Text(
                            w.damageRoll,
                            style = L5RTheme.type.body.copy(color = L5RTheme.colors.ink),
                        )
                        EquipToggle(equipped) { onEquipWeapon(w.name) }
                    }
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

/** A small pill that equips/unequips a weapon (drives the Combat tab's attack/damage rolls). */
@Composable
private fun EquipToggle(equipped: Boolean, onClick: () -> Unit) {
    val colors = L5RTheme.colors
    val bg = if (equipped) colors.accentCrimson else colors.paperDark
    val fg = if (equipped) colors.whiteWash else colors.inkMuted
    Box(
        Modifier
            .padding(start = Spacing.s2)
            .clip(Radii.button)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.s3, vertical = Spacing.s1),
    ) {
        androidx.compose.material3.Text(
            (if (equipped) "Equipped" else "Equip").uppercase(),
            style = L5RTheme.type.label.copy(color = fg),
        )
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
