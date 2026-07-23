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

- ✅ **Room-backed session overlay** — the persistence layer for everything in this theme; never
  mutates the imported save. Landed with the wounds tracker: `data/session` (`SessionState` entity
  keyed per character `uuid`, DAO, `SessionDatabase`, `SessionRepository`), merged onto the derived
  baseline in `AppViewModel`. A missing row means "no overlay yet" → the baseline applies.
- ✅ **Wounds tracker** — in the new **Combat** tab: apply a specific damage/heal amount (native
  number entry, not a stepper), Rest (nightly heal rate), Reset (to baseline). Surfaces the current
  wound level + penalty (Healthy 0 · Nicked +3 · Grazed +5 · Hurt +10 · Injured +15 · Crippled +20 ·
  Down +40 · Out) via the pure `woundStatus()` (Layer B — no desktop oracle; RAW boundary = a rank
  holds until wounds *exceed* its capacity). Capacity = Earth×5 then Earth× the campaign multiplier
  (x2 default); rest-heal = 2×Stamina + Insight Rank per night.
- ✅ **Void points** — spend/regain against the pool (= Void Ring); one spend per round; refreshes on
  daily rest. In the Combat tab as a tappable pip track.
- ✅ **Spell slots** — per-element daily counters (= element Ring) plus the flexible Void-Ring bonus
  pool (spendable for any element); a failed cast still burns a slot, and a Rest refreshes them all
  (sunrise refresh, RAW p.166). In the **Spells** tab as tappable pip tracks (tap filled to spend,
  empty to regain), shown only for shugenja. Pure `spellSlots()` (Layer B) sizes the pools; the
  spent counts live in the Room overlay (`SessionState`), never touching the `.l5r`.
  - 💭 *Debt — affinity/deficiency slots.* Pool size is the raw element Ring: **affinity does not add
    a slot** and **deficiency does not remove the pool**. Several schools/advantages also shift slot
    counts (e.g. an affinity treating the Ring as one higher for how many spells you may cast, or
    "one less Earth slot"). These are unmodelled; `SpellsView.affinities`/`deficiencies` are already
    derived, so a first pass could apply the flat ±1 before a full modifier engine (Theme 4) lands.
- 🔜 **Status conditions** — toggle the standard conditions (Blinded, Dazed, Entangled, Fatigued,
  Grappled, Prone, Stunned, Fear) and apply their roll/Armor-TN effects; expose their recovery rolls.
- ✅ **Session notes** — a per-character **play-session log** in the **Notes** tab: an email-style
  list of sessions, each identified by a date + running counter ("16 giugno 2026 — Session #1"),
  with add / edit / delete (tap a row to edit its body, ✚ to start a new session). The body is the
  player's worklist for things the app deliberately does *not* apply to the sheet — **XP gained**,
  **loot / new equipment**, plot notes — to later transcribe into the L5RCM desktop app (which stays
  the system of record for advancement). Room-backed (`SessionNote` entity, one-to-many per character
  `uuid`); never writes the `.l5r`. This replaced the old read-only page that echoed the imported
  character's `.l5r` notes.
- 💭 **Active Kata / Kiho** — track the currently-active Kata (one at a time) / Kiho and apply their
  modifiers; same overlay pattern as conditions.
- 💭 **Monk tattoos (irezumi)** — for tattooed monks, track which tattoo abilities are active and
  apply their effects; same overlay pattern as Kata / Kiho.
- 💭 **Shadowlands Taint** — track the Taint rank as a session/persistent value.

---

## Theme 2 — Table tools

**Why.** Reduce the number of physical props at the table.

- ✅ **Roll & Keep engine (v1.2)** — the load-bearing primitive under everything else here. Port the
  desktop's `drcore.py`: XkY roll-and-keep, **exploding tens**, the **Ten Dice Rule** (cap at 10
  dice, convert 2-rolled→1-kept, +2 beyond 10k10), **Void boost** (spend a point for +1k1), unskilled
  rolls (no explosion / no Raises), and the dice-penalty rule (kept never exceeds rolled). The Ten
  Dice Rule and the penalty rule are the easy-to-get-wrong pieces — unit-test them against the
  manual's worked examples.
- 🔨 **Raises & Free Raises** — a Raise stepper on every roll: **called Raises** each add +5 to the
  TN (capped at the Void Ring) and, if met, improve the result; **Free Raises** (granted by mastery
  abilities, techniques, kata…) do *not* count toward the cap and may instead reduce the TN by 5.
  Show the effective TN live, and fail the roll if a declared Raise's TN isn't met.
- 🔜 **Skill Mastery Abilities** — surface the mastery abilities the character has unlocked (skill
  ranks 3/5/7) and apply the ones that touch rolls — most commonly **Free Raises** and flat
  roll/damage bonuses — to the matching contextual rolls automatically.
- 🔨 **Contextual rolls from the sheet** — tap a roll affordance next to a stat and the app rolls the
  matching pool automatically, pre-filling pool and TN from the deriver, and auto-applying the
  current wound penalty. Covers: **Skill** (Trait+Skill k Trait), **Trait / Ring**, **Spell**
  (Ring+School Rank, TN = 5 + 5×Mastery Level), **Attack** (vs target Armor TN; −10 for ranged into
  melee), **Damage** (+Strength; bows use their own Strength; not boostable by Void), **Initiative**,
  **contested** rolls, and **Fear resistance** (Raw Willpower vs 5 + 5×Fear Rank, +Honor Rank).
- 🔨 **Combat tracker (v1.3)** — round structure (Initiative → Turns → Reactions) and turn tracking
  for the player's own character; live **Armor TN** ("TN to be hit" = Reflexes×5+5 + armor + stance /
  condition modifiers); action economy reference (1 Complex or 2 Simple + Free; move = Water×5/10/20
  ft); multi-round **spell casting countdown** (Complex actions = Mastery Level).
- ✅ **Weapon selection** — pick the weapon used to attack from the character's equipment (Equip
  toggle in the Equipment section); it drives the attack roll's **skill** (e.g. Kenjutsu for a katana)
  and the **damage roll** (DR + Strength, or the bow's own Strength), surfaced in the Combat tab's
  Equipped Weapon panel with stance-adjusted pools.
- ✅ **Armor selection** — the `.l5r` carries a single armor; a **worn / removed** toggle in the
  Equipment section drops its Armor TN + Reduction bonus, reflected live in the Combat tab (Layer B
  overlay — the derived Character sheet always shows the armored baseline).
- ✅ **Stance selection** — set the character's combat stance (Attack, Full Attack, Defence,
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
applied. The deeper issue for a *play* app: the desktop only implements the modifiers needed to
**print a static sheet** (fixed trait/TN/wound/insight bonuses). Table play also needs the
**situational** modifiers that never show on a printed sheet because they only fire under a condition
— *+1k0 attacking with a katana*, *+1k1 vs Shadowlands creatures*, *in Attack stance*, *once per
skirmish*. There is no complete desktop implementation to mirror for these, so this is an *extension*,
not a parity job. See **Design principles → Two modifier layers** below for the governing rule.

- 💭 **Static-modifier parity (Layer A)** — grow evaluation of the datapack `ModifierDef`
  expressions that feed the *derived sheet*, so those numbers match the desktop across more
  schools/techniques/advantages. Desktop stays the source of truth here.
- 💭 **Desktop-oracle parity tests** — automate golden tests that export characters across
  clans/schools/shugenja and assert the derived sheet numbers match the desktop.
- 💭 **Modifier taxonomy** — classify every `ModifierDef` as *auto* (unconditional → Layer A),
  *conditional* (known trigger → an automatic toggle), or *manual* (fallback to showing the rule
  text). This classification is the data both layers key off; worth documenting as a spec in the
  desktop repo (`DATAPACK_FORMAT.md` or a new doc), per the "update the spec" rule.
- 💭 **Graceful degradation for unsupported modifiers** — anything the engine can't evaluate is
  surfaced as a **toggle/prompt carrying the rule text** in the relevant roll or tracker, so the
  player applies it with one tap. Turns an unsupported modifier from a silent bug into a checklist
  item — and works *before* any expression engine exists.
- 💭 **Situational-modifier engine (Layer B), incremental** — grow a conditional-modifier evaluator
  by **trigger category** (stance, weapon type, target type, once-per-X, Void spend…). Each category
  covered moves modifiers from *manual* to *automatic*. Built additively on top of Layer A, never
  forking the desktop's partial model.

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

## Design principles

Cross-cutting rules that shape Themes 1, 2 and 4 — decided once, applied everywhere.

### Two modifier layers

L5R modifiers split into two kinds with **two different contracts**:

- **Layer A — derived sheet (static).** The fixed bonuses that print on a sheet (trait/TN/wound/
  insight). These **must match the desktop exactly**; the desktop is the source of truth and parity
  is enforced by golden tests. This layer never diverges.
- **Layer B — play-time modifiers (situational).** Conditional/triggered bonuses that don't appear
  on a printed sheet and that the desktop doesn't fully model. There is **no desktop reference to be
  "parity" with** — this layer is the app's own extension. Its contract is different: **transparent
  and overridable, never silently wrong.**

Consequences:

- Keep the two layers **separate**. Layer B is additive on top of Layer A; it never rewrites the
  parity-tested sheet numbers.
- **Never silently ignore or guess** a modifier the engine can't evaluate. Degrade to a visible
  toggle/prompt that shows the rule text, so the player decides (see Theme 4).
- Always show **what was applied and why** in a roll/tracker result, and let the player override it.
- The datapack `ModifierDef` is treated as **shared data** interpreted more deeply than the desktop
  needs — not a fork of the desktop's partial implementation.

---

## 💭 Considered but out of scope (for now)

Recorded so the decision is traceable — **not** planned work:

- **Bidirectional QR** — showing a QR from the app to re-export state to another device/the desktop.
- **Multi-device session sync** — sharing the *session state* (Theme 1) across devices, or live
  GM↔player sync at the table. Would only make sense once session state exists.

These may be revisited after the earlier themes land.
