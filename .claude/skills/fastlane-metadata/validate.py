#!/usr/bin/env python3
"""Validate the fastlane metadata tree for F-Droid / IzzyOnDroid and report gaps.

Checks character/byte limits, mandatory files, and per-locale/global gaps
(missing titles, missing changelog for the current versionCode, missing icon /
feature graphic / screenshots). Read-only: it never writes files.

Usage:
    python validate.py                 # validate, human-readable report
    python validate.py --json          # machine-readable report
Exit code: 0 if no hard errors, 1 if any ERROR-level problem was found.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

# --- Limits (stricter of F-Droid / IzzyOnDroid where they differ) ------------
TITLE_MAX = 30          # IzzyOnDroid 30, F-Droid 50 -> keep the strict one
SHORT_MAX = 80          # characters
FULL_MAX = 4000         # characters
CHANGELOG_MAX_BYTES = 500

IMAGE_EXTS = (".png", ".jpg", ".jpeg")
SCREENSHOT_DIRS = (
    "phoneScreenshots",
    "sevenInchScreenshots",
    "tenInchScreenshots",
    "tvScreenshots",
    "wearScreenshots",
)

REPO_ROOT = Path(__file__).resolve().parents[3]
META_ROOT = REPO_ROOT / "fastlane" / "metadata" / "android"


@dataclass
class Report:
    errors: list[str] = field(default_factory=list)      # blocks a good release
    warnings: list[str] = field(default_factory=list)    # should fix
    gaps: list[str] = field(default_factory=list)        # recommended, informational
    info: list[str] = field(default_factory=list)

    def as_dict(self) -> dict:
        return {"errors": self.errors, "warnings": self.warnings,
                "gaps": self.gaps, "info": self.info}


def read_version() -> tuple[int | None, str | None]:
    """Return (versionCode, versionName) from app/build.gradle.kts."""
    gradle = REPO_ROOT / "app" / "build.gradle.kts"
    if not gradle.exists():
        return None, None
    text = gradle.read_text(encoding="utf-8")
    code = re.search(r"versionCode\s*=\s*(\d+)", text)
    name = re.search(r'versionName\s*=\s*"([^"]+)"', text)
    return (int(code.group(1)) if code else None,
            name.group(1) if name else None)


def _content_len(path: Path) -> int:
    """Character count ignoring a single trailing newline."""
    return len(path.read_text(encoding="utf-8").rstrip("\n"))


def _check_text(rep: Report, path: Path, limit: int, label: str) -> None:
    n = _content_len(path)
    if n == 0:
        rep.errors.append(f"{path.relative_to(REPO_ROOT)}: {label} is empty")
    elif n > limit:
        rep.errors.append(
            f"{path.relative_to(REPO_ROOT)}: {label} is {n} chars (max {limit})")


def validate() -> Report:
    rep = Report()
    version_code, version_name = read_version()
    if version_code is None:
        rep.warnings.append("Could not read versionCode from app/build.gradle.kts")
    else:
        rep.info.append(f"versionCode={version_code} versionName={version_name}")

    if not META_ROOT.exists():
        rep.errors.append(f"Missing metadata root: {META_ROOT.relative_to(REPO_ROOT)}")
        return rep

    locales = sorted(p.name for p in META_ROOT.iterdir() if p.is_dir())
    if "en-US" not in locales:
        rep.errors.append("Missing en-US locale (required F-Droid fallback)")
    rep.info.append(f"locales: {', '.join(locales)}")

    for loc in locales:
        d = META_ROOT / loc
        short = d / "short_description.txt"
        full = d / "full_description.txt"
        title = d / "title.txt"

        # mandatory
        if short.exists():
            _check_text(rep, short, SHORT_MAX, "short_description")
        else:
            rep.errors.append(f"{loc}: missing short_description.txt (mandatory)")
        if full.exists():
            _check_text(rep, full, FULL_MAX, "full_description")
        else:
            rep.errors.append(f"{loc}: missing full_description.txt (mandatory)")

        # title: mandatory for the fallback locale, optional (inherits) elsewhere
        if title.exists():
            _check_text(rep, title, TITLE_MAX, "title")
        elif loc == "en-US":
            rep.warnings.append("en-US: missing title.txt (app name)")
        else:
            rep.gaps.append(f"{loc}: no title.txt (will inherit en-US title)")

        # changelog for the current versionCode
        if version_code is not None:
            cl = d / "changelogs" / f"{version_code}.txt"
            if cl.exists():
                if cl.stat().st_size > CHANGELOG_MAX_BYTES:
                    rep.errors.append(
                        f"{cl.relative_to(REPO_ROOT)}: {cl.stat().st_size} bytes "
                        f"(max {CHANGELOG_MAX_BYTES})")
            elif loc == "en-US":
                rep.gaps.append(
                    f"en-US: no changelogs/{version_code}.txt "
                    f"(optional until the app is published; needed for later updates)")
            else:
                rep.gaps.append(f"{loc}: no changelogs/{version_code}.txt")

    # images (checked against en-US, the fallback locale)
    img = META_ROOT / "en-US" / "images"
    icon = any((img / f"icon{e}").exists() for e in IMAGE_EXTS)
    feat = any((img / f"featureGraphic{e}").exists() for e in IMAGE_EXTS)
    if not icon:
        rep.gaps.append("en-US/images/icon.png missing (recommended)")
    if not feat:
        rep.gaps.append("en-US/images/featureGraphic.png missing (recommended)")

    shots = img / "phoneScreenshots"
    n_shots = 0
    if shots.is_dir():
        n_shots = sum(1 for f in shots.iterdir() if f.suffix.lower() in IMAGE_EXTS)
    if n_shots == 0:
        rep.gaps.append("en-US/images/phoneScreenshots/ missing or empty (recommended)")
    else:
        rep.info.append(f"en-US phoneScreenshots: {n_shots}")

    return rep


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--json", action="store_true", help="machine-readable output")
    args = ap.parse_args()

    rep = validate()
    if args.json:
        print(json.dumps(rep.as_dict(), indent=2))
    else:
        for label, items, mark in (
            ("ERROR", rep.errors, "x"),
            ("WARN", rep.warnings, "!"),
            ("GAP", rep.gaps, "-"),
            ("INFO", rep.info, "i"),
        ):
            for it in items:
                print(f"[{mark}] {label}: {it}")
        if not (rep.errors or rep.warnings or rep.gaps):
            print("[i] OK: no issues found")
    return 1 if rep.errors else 0


if __name__ == "__main__":
    sys.exit(main())
