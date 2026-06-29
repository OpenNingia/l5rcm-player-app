# L5RCM Companion (Android)

A native **Android companion app** for [L5RCM](https://github.com/OpenNingia/l5r-character-manager-3) —
a *play-at-the-table* tool for players, **not** a character builder. It imports a character from
the desktop app's `.l5r` save files, downloads the matching datapacks from the official repository,
and shows a read-only character sheet with all derived values computed natively.

Character **advancement is out of scope** — the companion only *reads* the character.

## Status — v1 (MVP)

- ✅ Import `.l5r` via the Storage Access Framework
- ✅ Download datapacks from `OpenNingia/l5rcm-data-packs` (GitHub releases), with host allow-list + zip-slip guard
- ✅ Native re-implementation of the `.l5r` + datapack readers
- ✅ Pure-Kotlin derivation engine (traits/rings/void, skills, schools/techniques, spells, insight, wounds, TN/RD, XP)
- ✅ Read-only sheet with the L5R manuscript design system (clan-accented theme)
- 🔜 Session state, dice roller, combat tracker — see the plan's *Future phases*

## Prerequisites

You need **JDK 17** and the **Android SDK** (Platform 35 + Build-Tools 35). Gradle itself is **not**
required — the Gradle wrapper (`gradlew`) is committed and downloads the right version on first run.

### Windows (winget + PowerShell)

**1. JDK 17**

```powershell
winget install --id EclipseAdoptium.Temurin.17.JDK -e
```

Make JDK 17 the active JDK (Gradle/AGP need it; if you also have an older JDK installed the SDK tools
will fail with *"Java version 17 or higher is required"*):

```powershell
$jdk17 = (Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory |
          Where-Object Name -like "jdk-17*" | Select-Object -First 1).FullName
setx JAVA_HOME "$jdk17"     # persistent — reopen the terminal afterwards
$env:JAVA_HOME = $jdk17     # current session
$env:Path = "$jdk17\bin;$env:Path"
java -version               # must print 17
```

> If the folder isn't found, locate `java.exe` with
> `Get-ChildItem "C:\Program Files" -Recurse -Filter java.exe -ErrorAction SilentlyContinue | Select FullName`
> and use the path up to (but not including) `\bin\java.exe` as `JAVA_HOME`.

**2. Android SDK** — choose one:

*Option A — Android Studio (includes the SDK Manager + emulator):*

```powershell
winget install --id Google.AndroidStudio -e
```
On first launch, install **SDK Platform 35**, **Build-Tools 35**, and **Platform-Tools** via the wizard.

*Option B — command-line tools only (headless builds, no IDE):*

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
Invoke-WebRequest -Uri "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip" `
  -OutFile "$env:USERPROFILE\Downloads\cmdline-tools.zip"
Expand-Archive "$env:USERPROFILE\Downloads\cmdline-tools.zip" -DestinationPath "$sdk\cmdline-tools-tmp"
New-Item -ItemType Directory -Force "$sdk\cmdline-tools\latest" | Out-Null
Move-Item "$sdk\cmdline-tools-tmp\cmdline-tools\*" "$sdk\cmdline-tools\latest"
Remove-Item -Recurse -Force "$sdk\cmdline-tools-tmp"
```

**3. Environment + SDK components**

```powershell
setx ANDROID_HOME "$env:LOCALAPPDATA\Android\Sdk"   # reopen the terminal afterwards
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"  # current session

# install the components this project targets (compileSdk 35 / minSdk 26)
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" "platform-tools" "platforms;android-35" "build-tools;35.0.0"
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
```

> Gradle locates the SDK via `ANDROID_HOME`. Alternatively create a `local.properties` (git-ignored) with
> `sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`.

### macOS / Linux

```sh
# JDK 17 (Homebrew example)
brew install temurin@17 android-commandlinetools
export ANDROID_HOME="$HOME/Library/Android/sdk"   # or your SDK location
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
sdkmanager --licenses
```

(Or install Android Studio and let it manage the SDK.)

## Build & test

From the repo root:

```sh
./gradlew test          # JVM unit tests (parsers + derivation engine) — no device needed
./gradlew assembleDebug # debug APK → app/build/outputs/apk/debug/
```

On Windows use `.\gradlew.bat test` / `.\gradlew.bat assembleDebug`. The first run downloads the
Gradle distribution and dependencies (needs network).

CI (`.github/workflows/ci.yml`) runs both `test` and `assembleDebug` on every push and PR.

## Specification

This app is a clean-room re-implementation against specs authored in the desktop repo
(`../l5r-character-manager-3/docs/`):

- `FILE_FORMAT_L5R.md` — the `.l5r` JSON format and how to derive state from `advans[]`
- `DATAPACK_FORMAT.md` — datapack folder / `manifest` / `<L5RCM>` XML schema
- `L5R_UI_Design_System.md` — design tokens ported into the Compose theme

See [`CLAUDE.md`](CLAUDE.md) for the architecture, conventions, and documented v1 approximations.

## License

See [LICENSE](LICENSE).
