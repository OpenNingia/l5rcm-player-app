#!/usr/bin/env python3
"""Render the app's adaptive launcher icon to a flat store PNG for fastlane.

Reads the actual Android resources so the store icon stays in sync with the app:
  - background colour  <- app/src/main/res/values/ic_launcher_background.xml
  - the five rings     <- app/src/main/res/drawable/ic_launcher_foreground.xml
and writes fastlane/metadata/android/en-US/images/icon.png (512x512).

Re-run this whenever the launcher icon changes. Requires Pillow.

Usage:
    python .claude/skills/fastlane-metadata/render_icon.py [--size 512]
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

from PIL import Image, ImageDraw

REPO_ROOT = Path(__file__).resolve().parents[3]
BG_XML = REPO_ROOT / "app/src/main/res/values/ic_launcher_background.xml"
FG_XML = REPO_ROOT / "app/src/main/res/drawable/ic_launcher_foreground.xml"
OUT = REPO_ROOT / "fastlane/metadata/android/en-US/images/icon.png"
VIEWPORT = 108.0  # adaptive-icon foreground viewport is 108x108


def read_background() -> str:
    m = re.search(r'name="ic_launcher_background"\s*>\s*(#[0-9A-Fa-f]{6,8})',
                  BG_XML.read_text(encoding="utf-8"))
    if not m:
        sys.exit(f"Could not find ic_launcher_background colour in {BG_XML}")
    return m.group(1)


def read_circles() -> list[tuple[float, float, float, str]]:
    """Parse (cx, cy, r, colour) for each <path> circle in the foreground vector.

    Each ring is a circle drawn as pathData 'M{cx},{cy}m-{r},0a...'.
    fillColor precedes pathData within each <path>, so findall order pairs up.
    """
    text = FG_XML.read_text(encoding="utf-8")
    colors = re.findall(r'android:fillColor="(#[0-9A-Fa-f]{6,8})"', text)
    coords = re.findall(r'M(\d+(?:\.\d+)?),(\d+(?:\.\d+)?)m-(\d+(?:\.\d+)?),0', text)
    if not coords or len(colors) != len(coords):
        sys.exit(f"Could not parse ring circles from {FG_XML} "
                 f"({len(colors)} colours, {len(coords)} circles)")
    return [(float(cx), float(cy), float(r), col)
            for (cx, cy, r), col in zip(coords, colors)]


def render(size: int) -> None:
    ss = 4  # supersample for antialiasing
    s = size * ss
    scale = s / VIEWPORT
    img = Image.new("RGB", (s, s), read_background())
    d = ImageDraw.Draw(img)
    for cx, cy, r, col in read_circles():
        d.ellipse([(cx - r) * scale, (cy - r) * scale,
                   (cx + r) * scale, (cy + r) * scale], fill=col)
    img = img.resize((size, size), Image.LANCZOS)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT, "PNG")
    print(f"wrote {OUT.relative_to(REPO_ROOT).as_posix()} ({size}x{size})")


if __name__ == "__main__":
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--size", type=int, default=512, help="output px (default 512)")
    render(ap.parse_args().size)
