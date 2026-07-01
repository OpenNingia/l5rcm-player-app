# Fastlane metadata reference (F-Droid / IzzyOnDroid)

Authoritative sources:
- https://f-droid.org/en/docs/All_About_Descriptions_Graphics_and_Screenshots/
- https://izzyondroid.org/docs/general/Fastlane/

This project uses the layout `fastlane/metadata/android/<locale>/` at the repo root.

## Directory layout

```
fastlane/metadata/android/
  en-US/                       <- REQUIRED fallback locale
    title.txt                  app name
    short_description.txt       MANDATORY
    full_description.txt        MANDATORY
    video.txt                   optional (a single video URL)
    images/
      icon.png                  recommended
      featureGraphic.png        recommended (landscape promo banner)
      promoGraphic.png          optional
      tvBanner.png              optional
      phoneScreenshots/         recommended: 1.png, 2.png, ... (or .jpg)
      sevenInchScreenshots/     optional
      tenInchScreenshots/       optional
      tvScreenshots/            optional
      wearScreenshots/          optional
    changelogs/
      <versionCode>.txt         e.g. 7.txt  (recommended; one per release)
  it-IT/  es-ES/  pt-BR/ ...    other locales, same shape (files optional; inherit en-US)
```

## Limits (validator enforces the stricter value where docs differ)

| File                    | Limit                | Notes |
|-------------------------|----------------------|-------|
| `title.txt`             | 30 chars (IzzyOnDroid); F-Droid allows 50 | keep ≤ 30 to be safe |
| `short_description.txt` | 80 chars             | mandatory |
| `full_description.txt`  | 4000 chars           | mandatory; plain text / light Markdown / basic HTML |
| `changelogs/<vc>.txt`   | 500 **bytes**        | plain text, no HTML |

## Formatting guidance

- Don't repeat the app name in descriptions — add real information instead.
- `full_description.txt`: "Markdown Lite" travels best across F-Droid + IzzyOnDroid +
  Play: paragraphs separated by a blank line, links, lists, emphasis. Avoid heading tags.
- Changelogs are plain ASCII/text only — no HTML, no Markdown links.

## Images

- Formats: PNG or JPG/JPEG only. EXIF is stripped; files may be recompressed.
- `icon.png`: 48–512 px square.
- `featureGraphic.png`: 512×250 or 1024×500 px, landscape.
- `tvBanner.png`: 1280×720 px.
- Screenshots: height:width ratio ≤ 2:1; displayed downscaled to 350px on the short side.
- Screenshots are numbered `1.png`, `2.png`, … in ascending display order.

## Changelog naming (critical)

- File name = the exact `versionCode` (from `app/build.gradle.kts`), no zero-padding.
- Current project versionCode lives in `app/build.gradle.kts` (`versionCode = N`).
- Changelogs should exist **before** the release tag is created (F-Droid builds the tag).
- Only en-US is required; other locales inherit en-US if their file is absent.
