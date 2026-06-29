# CLAUDE.md

Guidance for working in this repository.

## What this is

A native **Android companion app** for L5RCM (Legend of the Five Rings 4E character manager).
It is a *play-at-the-table* tool for players: it **imports** a `.l5r` save from the desktop app,
downloads the matching datapacks, and renders a **read-only** character sheet with all derived
values computed natively. It is **not** a character builder — advancement is out of scope.

This is a clean-room reimplementation (Kotlin/Compose) of the desktop app's `.l5r` and datapack
readers; it deliberately does **not** reuse the PySide6/QML + python-for-android stack.

## Specs (the contract — read these before touching parsing/derivation)

Authored in the sibling desktop repo `../l5r-character-manager-3/docs/`:

- `FILE_FORMAT_L5R.md` — `.l5r` JSON format; **how to derive state from `advans[]`**.
- `DATAPACK_FORMAT.md` — datapack folder / `manifest` / `<L5RCM>` XML schema.
- `L5R_UI_Design_System.md` — design tokens (ported into `ui/theme`).

If code and a spec disagree, the desktop **code** is the source of truth — update the spec.

## Build & test

JDK 17 + Android SDK (`ANDROID_SDK_ROOT`). No Gradle install needed (wrapper committed).

```sh
./gradlew test          # JVM unit tests — parsers + derivation engine (no device)
./gradlew assembleDebug # debug APK
```

CI (`.github/workflows/ci.yml`) runs `test` + `assembleDebug` on push/PR.

### Build flavors (`dist` dimension)

Two flavors differ **only** in the QR decoder — the lone proprietary dependency:

- `floss` → **zxing-cpp** (`io.github.zxing-cpp:android`, Apache-2.0). The FOSS build
  published on **F-Droid**; contains no proprietary deps. This is the alphabetically-first
  flavor, so plain `test` / `assembleDebug` resolve to it.
- `full` → **ML Kit** (`com.google.mlkit:barcode-scanning`, bundled on-device, no Play
  Services, but proprietary). Shipped on Play Store / as a direct APK.

The decoder lives behind `qrFrameAnalyzer(onValue): ImageAnalysis.Analyzer` (same package +
signature in `app/src/{floss,full}/kotlin/.../ui/imports/qr/QrFrameAnalyzer.kt`); the CameraX
camera/permission/overlay UI in `main` is flavor-agnostic. The F-Droid recipe builds `floss`
(`gradle: [floss]`). `test` / `assembleDebug` build **both** flavors — narrow to
`testFlossDebugUnitTest` / `assembleFlossDebug` if you only need the FOSS one.

## Architecture

Single Gradle module `:app`, package root `com.l5rcm.companion`. Layered MVVM + Hilt.

```
data/
  save/      .l5r parsing → SaveModel
             - qr/QrChunkAssembler: reassembles a .l5r from animated multi-frame QR codes
               (gzip+base64, chunked). Pure JVM. Shared wire format + golden vector:
               ../l5r-character-manager-3/docs/QR_IMPORT_FORMAT.md (desktop writes, app reads).
             - Advancement: sealed class, custom JsonContentPolymorphicSerializer keyed on
               the literal "type" string (AdvancementSerializer). Unknown types → GenericAdv.
             - SaveParser: lenient Json (ignoreUnknownKeys, coerceInputValues).
             - LenientStringMapSerializer: `properties` keeps only string values (saves carry
               array junk like "money":[0,0,0] in that dict).
  datapack/  XML parsing → typed entities → Datapack / DatapackSet (merged, core-first/last-wins)
             - DatapackParser drives an XmlParserFactory (provider abstraction):
               AndroidXmlParserFactory (android.util.Xml) in prod; kxml2 in unit tests.
               This is what makes the XML parser JVM-testable.
  catalog/   GitHub releases client (OkHttp) + install
             - DatapackCatalog: pinned repo OpenNingia/l5rcm-data-packs, host allow-list on the
               redirect target, ZipExtractor with a zip-slip path-traversal guard.
  repository/ CharacterRepository (SAF load), DatapackRepository (install/enable/merge/missing-deps),
             AppPreferences (DataStore: installed-pack registry + last-opened character uri).
domain/
  model/     Trait/Ring enums, immutable CharacterView (+ sub-views) the UI consumes.
  rules/     CharacterDeriver — the heart. PURE Kotlin, no Android. Ports the desktop's
             pure-arithmetic subset to build CharacterView from SaveModel + DatapackSet.
ui/
  theme/     L5RColors + L5RTypography via CompositionLocals; ClanAccent; L5RTheme. Light only.
  widgets/   RicePaperOverlay (procedural, fixed-seed LCG), OrnateDivider, SheetPanel, RingCard,
             PointTrack, StatRow, SectionHeader.
  imports/   ImportRouter (SAF picker + state routing), import landing, missing-datapack gate.
             qr/QrScanScreen — CameraX scanner driving QrChunkAssembler; the QR decoder
             (qrFrameAnalyzer) is flavor-specific (zxing-cpp / ML Kit — see Build flavors).
  library/   LibraryScreen — catalog install / enable-disable / remove.
  sheet/     SheetScreen (drawer nav) + SheetSections (read-only sections).
  AppViewModel (single Hilt VM), AppNav (NavHost), AppState.
di/          AppModule — Hilt wiring (OkHttp, dirs, parser, catalog, repos).
```

### Key derivation facts (see `CharacterDeriver`)

- Trait array order: stamina, willpower, reflexes, awareness, strength, perception, agility, intelligence.
  Rings: Earth(0,1) Air(2,3) Water(4,5) Fire(6,7); ring rank = min of its two traits; Void is separate.
- `trait_rank = starting + count(AttribAdv) + family(+1) + first-school(+1)`; `modified` applies `weak_<trait>` −1.
- Insight value (method 1) = Σ ring×10 + Σ skill ranks (+ mastery bonuses); rank via the RAW threshold table.
- Wounds: `health_rank(0)=earth×5`, `(1..7)=earth×2`; penalties `[0,3,5,10,15,20,40]`; heal = stamina×2 + insight_rank.
- TN = reflexes×5+5 (+armor); initiative = (insight_rank+reflexes) k reflexes; XP = Σ(cost>0), flaws add to the limit.

### v1 approximations (documented, intentional)

- Datapack **`ModifierDef` expressions are not evaluated** (no expression engine yet). Only flat
  family/school +1, simple rule flags (`weak_*`, `strength_of_earth`, `monkey_tokus_lesson`,
  `crane_the_force_of_honor`, `crab_the_mountain_does_not_move`), and the save's own `ModifierModel`
  entries (artn/arrd/hrnk/wpen) are applied.
- `insightRank` is computed from the insight *value* (matches the rules module), not the count of
  rank advancements — they coincide for finished imported characters.
- Honor/glory/status = datapack/rules starting baseline + stored delta (no fame-merit nuance).
- Pending `*_to_choose` advancement fields are ignored (read-only companion).

## Conventions

- Kotlin official style, 4-space indent, max line 120 (`.editorconfig`). No wildcard imports
  (except inside `@Composable`-heavy files where ktlint is relaxed for the naming rule).
- Composables read colours/spacing from `L5RTheme.colors` / `Spacing` / `Radii` — **never** hard-code
  hex or dp literals (mirrors the QML "no inline tokens" rule).
- Fonts: the design system bundles Cinzel / IM Fell English / EB Garamond. The TTFs are **not yet in
  the repo**; `ui/theme/Type.kt` falls back to serif via three `FontFamily` vals — drop the fonts in
  `res/font` and rewire those vals (one place) to switch the whole app.
- Parsers are pure/JVM-testable; keep Android types out of `data/save`, `data/datapack`, `domain/`.

## Testing

JVM unit tests (`app/src/test`) are the safety net for the reimplementation:
- `SaveParserTest` — real `.l5r` fixtures in `src/test/resources/saves`; advancement discrimination,
  defaults, lenient `properties`.
- `DatapackParserTest` — inline XML fixture; tag dispatch, school techs, perks, weapons.
- `CharacterDeriverTest` — synthetic save + datapack with **hand-computed** expected numbers
  (traits/rings/void, skills, insight, wounds, TN, initiative, XP) + insight threshold boundaries.
- `CatalogTest` / `ZipExtractorTest` — version parsing, host allow-list, zip-slip rejection.

> The strongest parity check (not yet automated) is golden tests against the **desktop app as oracle**:
> export characters across clans/schools/shugenja and assert the derived numbers match the desktop sheet.

## Future phases (not built in v1)

v1.1 session state (Room: wounds/void/spell-slots overlay, never writes the `.l5r`) · v1.2 dice roller
(port `drcore.py`) · v1.3 combat tracker (own character) · later: `ModifierDef` expression engine, i18n.
