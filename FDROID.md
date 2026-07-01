# Publishing on F-Droid

Operational guide for maintaining **L5RCM Companion** (`com.l5rcm.companion`) on F-Droid.

## Where metadata lives

- **Listing** (title, summary, description, screenshots, changelog) → in **this** repo, under
  `fastlane/metadata/android/<locale>/`. F-Droid pulls it automatically on every build.
- **Build recipe** (`Builds`, `commit`, `gradle`, license, categories…) → in the
  [`fdroiddata`](https://gitlab.com/fdroid/fdroiddata) repo, file `metadata/com.l5rcm.companion.yml`.

### Fastlane structure

```
fastlane/metadata/android/
  en-US/
    title.txt              # app name (replaces AutoName)
    short_description.txt   # summary, MAX 80 characters
    full_description.txt    # long description (max ~4000)
    changelogs/
      <versionCode>.txt     # e.g. 2.txt — release notes for that versionCode
  it-IT/  short_description.txt + full_description.txt
  es-ES/  short_description.txt + full_description.txt
  pt-BR/  short_description.txt + full_description.txt
```

> Note: in the fastlane structure the summary file is `short_description.txt` (not `summary.txt`)
> and the app name goes in `title.txt`. These names differ from those used in the fdroiddata
> metadata directory.

## Releasing an update (the normal case)

Thanks to `AutoUpdateMode: Version` + `UpdateCheckMode: Tags` in the `.yml`, the F-Droid bot
discovers new tags, adds the build entry itself, builds and publishes. **No MR on fdroiddata.**

Flow, **in this repo only**:

1. Bump the version in `app/build.gradle.kts` — `versionCode` MUST increase:
   ```kotlin
   versionCode = 3
   versionName = "0.2"
   ```
2. (Optional) Add release notes: `fastlane/metadata/android/en-US/changelogs/3.txt`.
3. Commit, push, annotated tag, push the tag:
   ```sh
   git commit -am "release 0.2"
   git push
   git tag -a 0.2 -m "Release 0.2"
   git push origin 0.2
   ```

The bot will see the tag, notice the higher `versionCode`, and publish.

### Rules to follow

1. **`versionCode` must always increase** on each release (if it doesn't, the bot ignores the tag).
2. **Consistent tag scheme** (`0.1.1`, `0.2`, …). Changing it (e.g. a `v` prefix) may require
   fixing the `.yml`.
3. **Build unchanged**: the template is copied from the last build entry
   (`subdir: app`, `gradle: [floss]`, …). As long as the recipe stays the same, it's fully automatic.

## When an MR on fdroiddata IS needed

Only when the **build recipe** or structural metadata changes:

- new proprietary dependencies, new flavor, prebuilt artifacts, different `gradle:`, moved subdir
- special flags (`scanignore`, `prebuild`, `rm`, AntiFeatures…)
- categories, license, links in the `.yml`

Listing metadata (summary/description/screenshots/changelog) **never** requires an MR.

## Constraints imposed by F-Droid maintainers

- **No inline `Summary`/`Description`** in the `.yml`: they belong in this repo's fastlane structure.
- **`commit:` must be the full commit hash (40 chars), never a tag or branch** — a movable
  reference is not reproducible. To get the hash for a tag:
  ```sh
  git rev-list -n 1 <tag>           # if it's a lightweight tag
  git rev-parse <tag>^{}            # dereference an annotated tag to its commit
  ```

## Reproducible builds & signing key

F-Droid is configured to **ship the developer-signed APK** (not re-sign with F-Droid's key).
It builds the app from source, verifies the result reproduces the APK we publish on GitHub
Releases, and then distributes our signed binary. This keeps the signature identical across
GitHub / Play Store / F-Droid, so users can switch sources without uninstalling.

fdroiddata fields that enable this:

```yaml
Binaries: https://github.com/OpenNingia/l5rcm-player-app/releases/download/%v/l5rcm-companion-%v.apk
AllowedAPKSigningKeys: <sha256 of the signing certificate, lowercase hex, no colons>
```

### How signing is wired

- `app/build.gradle.kts` defines a `release` `signingConfig` that reads secrets from env vars
  (`SIGNING_KEYSTORE_FILE`, `SIGNING_KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`)
  or matching gradle properties. When absent, the release build stays **unsigned** — which is what
  F-Droid's build-from-source produces and compares.
- `.github/workflows/release.yml` triggers on a version tag, decodes the keystore from the
  `SIGNING_KEYSTORE_BASE64` repo secret, builds `assembleFlossRelease`, and uploads
  `l5rcm-companion-<tag>.apk` to the GitHub Release.

### Required GitHub repo secrets

| Secret | Value |
|--------|-------|
| `SIGNING_KEYSTORE_BASE64` | `base64 -w0 release.jks` of the keystore file |
| `SIGNING_KEYSTORE_PASSWORD` | keystore password |
| `SIGNING_KEY_ALIAS` | key alias (e.g. `l5rcm`) |
| `SIGNING_KEY_PASSWORD` | key password |

### Creating the key (once) and reading its fingerprint

```sh
keytool -genkeypair -v -keystore release.jks -alias l5rcm \
  -keyalg RSA -keysize 4096 -validity 10000 -storetype PKCS12

# SHA-256 for AllowedAPKSigningKeys (strip the colons, lowercase):
keytool -list -v -keystore release.jks -alias l5rcm | grep -i SHA256
```

> ⚠️ **Back up `release.jks` AND its passwords offline.** F-Droid distributes binaries signed
> with this key: lose it and you can never publish a verifiable update (users would have to
> uninstall/reinstall); leak it and someone can sign malware as you.

### Keeping the build reproducible

F-Droid rebuilds from source and byte-compares the result to our published APK, so **the
release APK must be built in the same way F-Droid builds it.** What we had to pin so the two
match (all already in the repo):

- **Same JDK as the F-Droid build server.** This is the subtle one. D8 emits the dex
  annotations directory in HashMap iteration order, which changes between JDK major versions,
  so a mismatch makes `classes*.dex` differ. `.github/workflows/release.yml` must use the
  **same JDK major** that F-Droid uses — **not a hardcoded number**: check the F-Droid build
  log (it runs `update-alternatives --set java …/java-<N>-openjdk/...`) and set the workflow's
  `setup-java` `java-version` to that `<N>`. It was 21 as of 0.1.5; re-check if F-Droid bumps it.
- **No ART baseline profile.** `app/build.gradle.kts` disables the `*ArtProfile` tasks —
  `assets/dexopt/baseline.prof` and the profile-guided dex layout are not reproducible.
- **Pinned R8** (`settings.gradle.kts`, `com.android.tools:r8`) and **`buildToolsVersion`**
  (`app/build.gradle.kts`) matching what F-Droid resolves. build-tools must stay **34.0.0**:
  apksigner from build-tools ≥ 35 produces APKs F-Droid's `apksigcopier` cannot verify
  (fdroid #3299).

To validate a fix without waiting for F-Droid's server: F-Droid's *previous* build APK (kept as
a failed-build artifact) was made with its JDK, so building the reference the same way and
comparing `classes2.dex` predicts the outcome:

```sh
unzip -o -q l5rcm-companion-<new>.apk classes2.dex -d new
unzip -o -q <fdroid-build-artifact>.apk classes2.dex -d fdroid
cmp new/classes2.dex fdroid/classes2.dex   # match ⇒ reproducible
```

## fdroiddata `.yml` state (reference)

```yaml
Categories:
  - Game Helper
License: GPL-3.0-only
AuthorName: Daniele Simonetti
SourceCode: https://github.com/OpenNingia/l5rcm-player-app
IssueTracker: https://github.com/OpenNingia/l5rcm-player-app/issues
RepoType: git
Repo: https://github.com/OpenNingia/l5rcm-player-app.git

Builds:
  - versionName: '0.1.1'
    versionCode: 2
    commit: <full-commit-hash>
    subdir: app
    gradle:
      - floss

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: '0.1.1'
CurrentVersionCode: 2
```

> `CurrentVersion`/`CurrentVersionCode` mark the "suggested" version; the bot updates them
> itself when it publishes a new version via auto-update.
