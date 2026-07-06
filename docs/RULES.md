L5R 4E Table-Play Mechanics — Player-Facing Feature Candidates

Scope: Book of Earth (pp. 78–98), Spell Casting (pp. 163–166), Kata/Kiho (pp. 259–266). Only mechanics a player manages for their own character are listed. GM tables (Honor/Glory/Status gain-loss, NPC recognition, crafting) and pure lore are excluded except where they surface a value a player tracks.

---
[Dice roller] — core roll-and-keep engine

Roll & Keep + Exploding + Ten Dice Rule (pp. 75, 77)
The foundation. Format XkY: roll X d10, keep Y highest (player may keep lower to fail deliberately), sum vs TN.
- Exploding dice: any die showing 10 is re-rolled and added; chains indefinitely (10,10,7 = 27).
- Ten Dice Rule (must encode): no roll uses more than 10 dice. Excess rolled dice convert to kept at 2 rolled → 1 kept. Once at 10k10, each further rolled die = +2, each further kept die = +2 (per the reduction). Worked examples: 12k4→10k5; 13k9→10k10+2; 10k12→10k10+4; 14k12→10k10+12.
- Unskilled rolls (p. 80): dice do NOT explode; no Raises allowed. App must support a "no-explode / no-raise" toggle keyed on skill rank 0.

Raises (p. 79)
Player voluntarily raises TN by +5 per Raise. Max Raises per roll = Void Ring. Free Raises don't count toward the cap and may alternatively reduce TN by 5. If the raised TN isn't met, the roll fails even if the original TN was met. UI: a Raise stepper (capped at Void), showing effective TN.

Second-attempt penalty (p. 80): failed non-attack skill roll can be re-tried at +10 TN. Attacks can't retry.

Void Point spend before roll (p. 78): +1k1 to a Skill/Trait/Ring/Spell roll (declared before roll; not damage). Also lets an unskilled char roll as skill 1. The roller should offer a "spend Void" toggle that adds 1k1 and decrements the Void tracker.

---
[Contextual roll] — launched from the sheet with pre-filled pools

Skill roll (p. 79): roll (Trait + Skill) k (Trait). Tap a skill → prefills pool. This is the primary contextual roll.

Trait roll / Ring roll (pp. 79–80): roll & keep = Trait rank (or Ring rank). Raw-Trait variants exist for Fear resistance (see below).

Spell Casting roll (pp. 80, 163): roll (Ring + Shugenja School Rank) k (Ring), vs TN = 5 + (5 × Mastery Level). Tap a spell → prefill pool and TN from its ML. Raises here can be spent to reduce casting time (see combat tracker).

Attack roll (p. 82): a skill roll vs target's Armor TN. Contextual roll from a weapon. Penalty: −10 to total for ranged attack against a target in melee range.

Damage roll (pp. 80, 82):
- Melee: add Strength to the DR's rolled dice (katana DR 3k2 + Str 3 = 6k2). Damage rolls can NOT be boosted by Void.
- Bows have their own Strength rating added instead of the character's (yumi Str 3 + arrow 2k2 = 5k2).
- Unarmed: 0k1 → roll Strength dice, keep 1.

Initiative roll (p. 81): (Insight Rank + Reflexes) k Reflexes. Feeds the combat tracker.

Contested rolls (p. 80): both roll same type, higher wins; Raises must beat opponent by 5 each. Used by Disarm, Knockdown, Grapple, Iaijutsu Focus. App could roll both sides or just the player's side.

Fear resistance (p. 90): roll Raw Willpower vs TN = 5 + (5 × Fear Rank), +Honor Rank to total. Fail → −(Fear Rank)k0 penalty to all rolls until encounter ends; fail by 15+ = flees/cowers. This is both a contextual roll and a session-state condition.

---
[Combat tracker] — round/turn/stance/initiative state

Round structure (pp. 81–82): Stage 1 Initiative (+ pick Stance) → Stage 2 Turns in Initiative order (may Delay) → Stage 3 Reactions (expiring effects). Initiative rolled only on first round; may change via abilities. Track current Initiative Score per combatant.

Actions economy (p. 88): per turn either 1 Complex + Free actions OR 2 Simple + Free actions. Move rates: Free = Water×5 ft, Simple = Water×10 ft, max Water×20 ft/round. Table 2.2 lists which actions are Free/Simple/Complex — useful in-tracker reference.

Stances (pp. 84–85) — a tracked toggle with mechanical deltas the app should surface/apply:
- Attack: no restrictions.
- Full Attack: +2k1 to attack rolls, Armor TN −10; only attack/approach actions; melee only; +5 ft move once/round. Not while mounted/fatigued.
- Defense: +Air Ring + Defense Skill Rank to Armor TN; may not attack.
- Full Defense: roll Defense/Reflexes, add half the total (round up) to Armor TN until next turn; costs a Complex Action (Free actions only).
- Center: no actions this round; next round +1k1+Void Ring on one roll, +10 Initiative that round.

Armor TN (p. 82): base = Reflexes × 5 + 5 + armor + stance bonuses. Void spend can add +10 for one round. Prone: −10 vs melee. Grappled/Stunned: Armor TN = 5 + armor. Blinded: base = Reflexes + 5. This is a live "TN to be hit" value the tracker should compute.

Maneuvers (Raise costs) (pp. 85–89) — encode the raise cost table for the roller:
- Called Shot: variable (limb 1 / hand-foot 2 / head 3 / small part 4).
- Disarm: 3 Raises; deals only 2k1 (no Str), then Contested Strength.
- Extra Attack: 5 Raises (Raises give no bonus; grant a 2nd attack).
- Feint: 2 Raises; adds half the excess-over-Armor-TN to damage, cap = 5 × Insight Rank.
- Guard: 0 Raises (Simple action; ally's Armor TN +10, your Armor TN −5).
- Increased Damage: 1+ Raises = +1k0 damage each.
- Knockdown: 2 (biped) / 4 (quadruped) Raises + Contested Strength.

Iaijutsu Duel (pp. 86–87): 3-round structured mini-sequence (Assessment: Iaijutsu/Awareness vs 10 + opp Insight×5; Focus: Contested Iaijutsu/Void, beat by 5 = first strike + Free Raise per extra 5; Strike: Iaijutsu/Reflexes vs Armor TN). Both duelists in Center Stance. A candidate guided-flow feature.

Grapple (pp. 88–89): initiate via Jiujutsu/Agility (ignores armor bonus); each round Contested Jiujutsu/Strength for control; controller may Hit/Throw/Break/Pass. Grappled condition = Armor TN 5+armor.

Spell casting time (pp. 163–164): a spell takes Complex Actions = its Mastery Level to complete (first is the casting roll). Each Raise-for-time reduces by 1 (min 1). Interruption on damage: Willpower roll vs TN 5 + damage suffered (TN 10 if merely distracted). Tracker should count down multi-round casts.

---
[Session state] — mutable overlay tracked during play

Wounds & Wound Ranks (pp. 82–84) — the central tracker. Damage fills ranks in order; each rank holds Earth × multiplier wounds. Encode the penalty table (all are "+X to ALL TNs"):

┌──────────┬────────────┬─────────────────────────────────────────────────────────┐
│   Rank   │ TN penalty │                          Notes                          │
├──────────┼────────────┼─────────────────────────────────────────────────────────┤
│ Healthy  │ 0          │ capacity = Earth × 5 (always, even in non-x5 campaigns) │
├──────────┼────────────┼─────────────────────────────────────────────────────────┤
│ Nicked   │ +3         │                                                         │
├──────────┼────────────┼─────────────────────────────────────────────────────────┤
│ Grazed   │ +5         │                                                         │
├──────────┼────────────┼─────────────────────────────────────────────────────────┤
│ Hurt     │ +10        │                                                         │
├──────────┼────────────┼─────────────────────────────────────────────────────────┤
│ Injured  │ +15        │                                                         │
├──────────┼────────────┼─────────────────────────────────────────────────────────┤
│ Crippled │ +20        │ Move actions cost one difficulty level higher           │
├──────────┼────────────┼─────────────────────────────────────────────────────────┤
│ Down     │ +40        │ Free actions only, must spend Void to act, whisper only │
├──────────┼────────────┼─────────────────────────────────────────────────────────┤
│ Out      │ —          │ immobile/unconscious; further damage = death            │
└──────────┴────────────┴─────────────────────────────────────────────────────────┘

Earth multiplier for ranks 1–7 is campaign-configurable (x2 default, x3/x4/x5 options, p. 84). Healing: per night's rest recover 2 × Stamina + Insight Rank wounds. Void spend can reduce one damage source by 10 (declared after damage announced). Note: Low Pain Threshold disadvantage = +5 per rank; Grasp the Earth Dragon / Indomitable Warrior Style / Root the Mountain reduce wound penalties by Earth Ring; Permanent Wound = first rank always full.

Void Points (p. 78): pool = Void Ring, refreshed on daily rest. One Void spend per round (school techniques exempt). Track current/max; log spends (roll boost, damage reduction, Armor TN +10, Initiative +10 or swap, unskilled→skill 1). Momoku disadvantage: only spendable on techniques. Lord Moon's Curse / Emerald & Topaz Champion paths grant bonus daily Void.

Spell slots (p. 163): per-element slots = the element Ring; plus Void Ring bonus slots usable for any element. A failed casting still burns the slot; an interrupted (post-success) cast does not. Perfect per-element counter overlay.

Conditional / Status effects (pp. 88–90) — toggleable conditions each with encoded effects and recovery rolls:
- Blinded: −3k3 ranged / −1k1 melee attacks; Armor TN = Reflexes+5; Water −2 for movement.
- Dazed: −3k0 all actions; only Defense/Full Defense; recover via Earth Ring roll TN 20 in Reaction stage, TN −5 per prior fail.
- Entangled: only break-free actions (Strength roll, GM TN).
- Fasting: after 2 days +5 TN to skill/physical-trait/spellcast rolls, +5/extra day; no Void regen from rest.
- Fatigued: after 24h no rest +5 TN (as above), +5/day; can't Full Attack; after Stamina days, Willpower TN 20 every 2h to stay awake.
- Grappled: Armor TN = 5 + armor.
- Mounted/Higher: +1k0 attack vs lower targets; can't Full Attack.
- Prone: Armor TN −10 vs melee; no move; Defense/Attack only; can't use large weapons, −2k0 with medium/small; stand = Simple action.
- Stunned: no actions; Armor TN = 5 + armor; recover via Earth Ring roll TN 20 in Reaction stage.
- Fear (p. 90): −(Rank)k0 to all rolls until encounter ends.

Note Dice Penalties rule (p. 89): a −Xk0 penalty reduces rolled dice; if kept then exceeds rolled, kept is also reduced (never keep more than you roll). The roller must apply this after all penalties.

Kata active state (pp. 259–260): a bushi may have one Kata active at a time; executing is a Simple Action, dropping is Free. Track the active Kata and apply its modifier (many touch Armor TN, Initiative, or attack/damage pools — e.g. Striking as Air: Defense Armor TN +Air; Balance the Elements: use Void for Initiative; Reckless Abandon: +Fire to Armor TN in Full Attack).

Kiho active state (pp. 261–264): monks may hold one each of Internal/Kharmic/Mystical plus multiple Martial simultaneously (one Kiho effect delivered per unarmed strike/turn). Activation: spend Void (Free), or Meditation/Void TN 15 (Complex) / TN 30 (Simple). Track active Kiho + durations (many last "X rounds/minutes = a Ring or Insight Rank"). Several modify Armor TN, wound penalties, or add dice — same overlay pattern as Kata.

Shadowlands Taint (pp. 162, 263): a ranked spiritual condition (starts at 0.5 from the disadvantage). Increases in play; several Kiho suppress or reverse it. A trackable session/persistent value.

---
[Lookup] — reference tables the player hits repeatedly mid-game

- Standard TN table (Table 2.1, p. 79): None / 5 / 10 / 15 / 20 / 25 / 30 / 40 / 60 with difficulty labels — quick reference when GM names a difficulty word.
- Wound penalty table (p. 82–83, above) — the single most-consulted table.
- Maneuver Raise-cost table (above) — needed every combat.
- Stance effects card (above).
- Action economy / Move rates and Table 2.2 example actions (p. 88).
- Armor TN / TN-to-be-hit derived value (already computed by the deriver; surface it live with stance/condition modifiers).
- Falling & Drowning (p. 83): falling = 1k1 per 10 ft; drowning = Athletics(Swim)/Strength TN 15 per minute, then hold breath Stamina rounds, then 2k2/round. Environmental damage helper.