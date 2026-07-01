# Roadmap

Product roadmap for **L5RCM Companion** (Android). Organised by **theme**, not by fixed
release dates — themes ship when they are ready and may span more than one release.

This is a *play-at-the-table companion*: it imports a `.l5r` from the desktop app and renders a
**read-only** character sheet with all derived values computed natively. Character advancement
stays **out of scope** — see [`CLAUDE.md`](../CLAUDE.md) for architecture and the v1 approximations
already documented there.

**Near-term focus:** finish and polish the **single-character** experience (session state, table
tools, sheet refinements) *before* broadening to multiple characters. The themes below are ordered
accordingly.

> Legend: ✅ done · 🔨 in progress · 🔜 planned · 💭 exploratory (not committed)

---

## ✅ Shipped — v1 (MVP)

Baseline the roadmap builds on:

- Import a `.l5r` via the Storage Access Framework, plus animated multi-frame **QR import**.
- Download & manage datapacks from `OpenNingia/l5rcm-data-packs` (host allow-list + zip-slip guard).
- Native re-implementation of the `.l5r` + datapack readers.
- Pure-Kotlin derivation engine (traits/rings/void, skills, schools/techniques, spells, insight,
  wounds, TN/RD, XP).
- Read-only character sheet with the L5R manuscript design system (clan-accented theme).

Today the app tracks a **single** last-opened character (`AppPreferences` stores one character URI).

---

## Theme 1 — Session state (v1.1)

**Why.** At the table the numbers change: wounds taken, Void spent, spells cast. The companion must
track this *without ever writing back* to the `.l5r`. All of the below is a local overlay, keyed
per character, resettable to the derived baseline.

- 🔜 **Room-backed session overlay** — the persistence layer for everything in this theme; never
  mutates the imported save.
- 🔜 **Wounds tracker** — mark damage against the wound ranks and surface the current TN penalty
  (Healthy 0 · Nicked +3 · Grazed +5 · Hurt +10 · Injured +15 · Crippled +20 · Down +40 · Out).
  Capacity = Earth×5 then Earth× the campaign multiplier (x2 default); rest-heal = 2×Stamina +
  Insight Rank per night.
- 🔜 **Void points** — spend/regain against the pool (= Void Ring); one spend per round; refreshes on
  daily rest.
- 🔜 **Spell slots** — per-element counters (= element Ring) plus the Void-Ring bonus slots; a failed
  cast still burns a slot.
- 🔜 **Status conditions** — toggle the standard conditions (Blinded, Dazed, Entangled, Fatigued,
  Grappled, Prone, Stunned, Fear) and apply their roll/Armor-TN effects; expose their recovery rolls.
- 🔜 **Session notes** — free-form per-character notes for things earned during play that the app
  deliberately does *not* apply to the sheet: **XP gained**, **loot / new equipment**, plot notes.
  These are the player's worklist to later transcribe into the L5RCM desktop app (which stays the
  system of record for advancement).
- 💭 **Active Kata / Kiho** — track the currently-active Kata (one at a time) / Kiho and apply their
  modifiers; same overlay pattern as conditions.
- 💭 **Monk tattoos (irezumi)** — for tattooed monks, track which tattoo abilities are active and
  apply their effects; same overlay pattern as Kata / Kiho.
- 💭 **Shadowlands Taint** — track the Taint rank as a session/persistent value.

---

## Theme 2 — Table tools

**Why.** Reduce the number of physical props at the table.

- 🔜 **Roll & Keep engine (v1.2)** — the load-bearing primitive under everything else here. Port the
  desktop's `drcore.py`: XkY roll-and-keep, **exploding tens**, the **Ten Dice Rule** (cap at 10
  dice, convert 2-rolled→1-kept, +2 beyond 10k10), **Raises** (+5 TN each, capped at Void Ring),
  **Void boost** (spend a point for +1k1), unskilled rolls (no explosion / no Raises), and the
  dice-penalty rule (kept never exceeds rolled). The Ten Dice Rule and the penalty rule are the
  easy-to-get-wrong pieces — unit-test them against the manual's worked examples.
- 🔜 **Contextual rolls from the sheet** — tap a roll affordance next to a stat and the app rolls the
  matching pool automatically, pre-filling pool and TN from the deriver, and auto-applying the
  current wound penalty. Covers: **Skill** (Trait+Skill k Trait), **Trait / Ring**, **Spell**
  (Ring+School Rank, TN = 5 + 5×Mastery Level), **Attack** (vs target Armor TN; −10 for ranged into
  melee), **Damage** (+Strength; bows use their own Strength; not boostable by Void), **Initiative**,
  **contested** rolls, and **Fear resistance** (Raw Willpower vs 5 + 5×Fear Rank, +Honor Rank).
- 🔜 **Combat tracker (v1.3)** — round structure (Initiative → Turns → Reactions) and turn tracking
  for the player's own character; live **Armor TN** ("TN to be hit" = Reflexes×5+5 + armor + stance /
  condition modifiers); action economy reference (1 Complex or 2 Simple + Free; move = Water×5/10/20
  ft); multi-round **spell casting countdown** (Complex actions = Mastery Level).
- 🔜 **Weapon selection** — pick the weapon used to attack from the character's equipment; it drives
  the attack roll's **skill** (e.g. Kenjutsu for a katana) and the **damage roll** (DR + Strength,
  or the bow's own Strength), so the contextual attack/damage rolls above are pre-filled correctly.
- 🔜 **Armor selection** — choose the equipped armor (or **no armor**); feeds the armor bonus into
  the live Armor TN.
- 🔜 **Stance selection** — set the character's combat stance (Attack, Full Attack, Defence,
  Full Defence, Center); the choice adjusts the affected derived values and feeds the rolls above.
  E.g. Full Attack +2k1 attack / Armor TN −10; Full Defence adds half a Defence/Reflexes roll to
  Armor TN; Center skips the round for +1k1 and +10 Initiative next round.
- 💭 **Maneuvers** — surface the Raise-cost table (Disarm 3, Extra Attack 5, Feint 2, Knockdown 2/4,
  Called Shot 1–4, Increased Damage 1+…) and fold the cost into the attack roll's Raises.
- 💭 **Iaijutsu duel** — guided 3-stage flow (Assessment → Focus → Strike) for the player's side.
- 💭 **Grapple** — track the grappled state and its per-round contested Strength check.

---

## Theme 3 — Sheet UI/UX refinements

**Why.** The sheet is the core surface; polish improves everyday table use.

- 🔜 **Navigation & readability** — refine section layout, in-sheet navigation, and information
  density for quick lookups mid-game.
- 🔜 **Quick-reference tables** — surface the tables a player hits repeatedly mid-game: standard TN
  table, wound-penalty table, maneuver Raise costs, stance-effects card, action economy / move rates,
  and falling/drowning. Read-only, no dice needed.
- 🔜 **Bundled display fonts** — drop the Cinzel / IM Fell English / EB Garamond TTFs into
  `res/font` and rewire the `FontFamily` vals (currently falling back to serif).
- 💭 **Clan theming polish** — extend the clan-accent treatment across more of the sheet.

---

## Theme 4 — Rules engine depth

**Why.** v1 intentionally does not evaluate datapack `ModifierDef` expressions; only flat
family/school bonuses, a handful of named rule flags, and the save's own `ModifierModel` entries are
applied. A real expression engine removes those approximations.

- 💭 **`ModifierDef` expression engine** — evaluate datapack modifier expressions so derived values
  match the desktop app across more schools/techniques/advantages.
- 💭 **Desktop-oracle parity tests** — automate golden tests that export characters across
  clans/schools/shugenja and assert the derived numbers match the desktop sheet.

---

## Theme 5 — Multi-character & campaigns

**Why.** A player often runs different characters across several campaigns or one-shots. Once the
single-character experience is solid, the app should hold a *library* of imported characters and let
the player switch between them instantly, without re-importing or re-picking a file each time.

- 🔜 **Character library** — persist a registry of imported characters (replacing the single
  "last-opened URI"), each with its stored source and derived summary (name, clan, school, rank).
- 🔜 **Quick switcher** — pick the active character from a list; the sheet reflects it immediately.
- 🔜 **Campaign / adventure grouping** — tag or group characters by campaign so a player in multiple
  games sees them organised, not as one flat list.
- 🔜 **Per-character datapack context** — remember which enabled datapacks a character was derived
  against, and surface a clear prompt if required packs are missing on switch.
- 🔜 **Per-character session state** — generalise Theme 1's overlay so each character keeps its own
  wounds / Void / spell-slot state.
- 🔜 **Manage entries** — rename (display label), re-import/refresh from an updated `.l5r`, remove.

---

## Theme 6 — Internationalisation

- 💭 **i18n** — externalise UI strings and support additional locales (fastlane metadata tree already
  supports multiple locales).

---

## 💭 Considered but out of scope (for now)

Recorded so the decision is traceable — **not** planned work:

- **Bidirectional QR** — showing a QR from the app to re-export state to another device/the desktop.
- **Multi-device session sync** — sharing the *session state* (Theme 1) across devices, or live
  GM↔player sync at the table. Would only make sense once session state exists.

These may be revisited after the earlier themes land.
