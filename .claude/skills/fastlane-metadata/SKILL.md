---
name: fastlane-metadata
description: Create, update, validate and maintain the F-Droid / IzzyOnDroid fastlane metadata tree (fastlane/metadata/android/<locale>/). Use when adding a changelog for a new release, adding a locale, editing store descriptions/title/screenshots, or checking metadata is complete and within limits before tagging a release.
---

# Fastlane metadata maintenance

Maintains `fastlane/metadata/android/<locale>/` for this app's F-Droid and
IzzyOnDroid listings. See `reference.md` for the full structure, limits, and
image specs. Limits are enforced by `validate.py`.

## Always start by validating

Run the validator first — it reads the current `versionCode` from
`app/build.gradle.kts`, checks every locale against the limits, and reports gaps:

```sh
python .claude/skills/fastlane-metadata/validate.py
```

Report lines are prefixed `ERROR` (blocks a good release), `WARN` (should fix),
`GAP` (recommended/informational, e.g. missing icon or screenshots), `INFO`.
The script exits non-zero if any ERROR exists. Re-run it after any change.

## Task: add a changelog for the current release

The changelog is **not needed for the first, not-yet-published release** — F-Droid
uses it for update notes between versions. Add it once the app is published and a
new version ships. The file name **is** the `versionCode` (no padding), e.g. `7.txt`.

1. Get the current `versionCode` from the validator's INFO line (or
   `app/build.gradle.kts`).
2. Look at recent git history / the version bump to summarise what changed for
   users (not internal build tweaks unless user-visible).
3. Write `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
   — plain text, **≤ 500 bytes**, no HTML/Markdown links.
4. Only en-US is required; other locales inherit it. Add localized changelogs
   for `it-IT` / `es-ES` / `pt-BR` only if the user wants translated release notes.
5. Re-run the validator.

## Task: (re)generate the store icon

`render_icon.py` renders the app's adaptive launcher icon to a flat
`en-US/images/icon.png` (512×512). It reads the actual resources
(`ic_launcher_background.xml` + `ic_launcher_foreground.xml`), so it stays in
sync with the app icon — re-run it whenever the launcher icon changes:

```sh
python .claude/skills/fastlane-metadata/render_icon.py   # --size to override 512
```

Requires Pillow. This is the only asset the skill can generate (it derives from
the app's own vector); screenshots and featureGraphic must still be supplied by
the user.

## Task: report gaps / audit before a release

Run the validator and relay ERRORs, WARNs and GAPs to the user in plain language,
e.g. "the app icon and phone screenshots are missing", "no changelog for the
current versionCode 7", "it-IT full_description exceeds 4000 chars". Do not
invent or generate images — screenshots/icons are assets the user must provide;
just flag them.

## Task: add a locale

Create `fastlane/metadata/android/<locale>/` with at least
`short_description.txt` and `full_description.txt` (mandatory). `title.txt` is
optional (inherits en-US). Match the tone of the existing en-US copy; translate
rather than transliterate. Respect the limits in `reference.md`.

## Task: edit descriptions / title

Edit the relevant `.txt` files, then validate. Keep within limits; don't repeat
the app name in descriptions; keep `full_description` as light Markdown/plain text.

## Rules

- Never hard-code limits from memory — trust `validate.py` / `reference.md`.
- A single trailing newline in text files is fine (the validator ignores it).
- Changelogs must exist **before** the release tag is created (F-Droid builds the tag).
- This is metadata only — it never affects the build. The icon can be regenerated
  from the app's vector via `render_icon.py`; other assets (screenshots,
  featureGraphic) must be supplied by the user — the skill only detects and reports them.
