#!/usr/bin/env python3
# SPDX-License-Identifier: MPL-2.0

"""Check the static landing site in docs/ without builds or network access."""

import json
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ElementTree


ROOT = Path(__file__).resolve().parents[2]
SITE = ROOT / "docs"
# docs/project holds repository documentation and is never published with the site.
UNPUBLISHED = {"project"}
REQUIRED_FILES = [
    "index.html", "404.html", "robots.txt", "sitemap.xml", "favicon.svg", "favicon.ico", "favicon-32.png",
    "apple-touch-icon.png", "og-image.png",
]
LOCAL_REFERENCE = re.compile(r'(?:href|src)="(/[^"#?]*)"|url\((/[^)]+)\)')
IMAGE_TAG = re.compile(r"<img\b[^>]*>")
JSON_LD = re.compile(r'<script type="application/ld\+json">(.*?)</script>', re.S)


def check_page(page: Path, errors: list[str]) -> None:
    """Checks metadata, local references, image alt text, and structured data of one HTML page."""
    html = page.read_text(encoding="utf-8")
    name = page.name
    if "<title>" not in html:
        errors.append(f"{name} needs a <title>")
    for match in LOCAL_REFERENCE.finditer(html):
        reference = match.group(1) or match.group(2)
        relative = reference.lstrip("/")
        if reference != "/" and (relative.split("/", 1)[0] in UNPUBLISHED or not (SITE / relative).exists()):
            errors.append(f"{name}: broken local reference {reference}")
    for tag in IMAGE_TAG.findall(html):
        if " alt=" not in tag:
            errors.append(f"{name}: image without alt text: {tag[:80]}")
    for block in JSON_LD.findall(html):
        try:
            json.loads(block)
        except json.JSONDecodeError as error:
            errors.append(f"{name}: invalid JSON-LD: {error}")


def main() -> int:
    errors: list[str] = []
    for file_name in REQUIRED_FILES:
        if not (SITE / file_name).is_file():
            errors.append(f"missing file: docs/{file_name}")
    if not errors:
        home = (SITE / "index.html").read_text(encoding="utf-8")
        if 'name="description"' not in home or 'rel="canonical"' not in home:
            errors.append("index.html needs a meta description and a canonical link")
        for page in ("index.html", "404.html"):
            check_page(SITE / page, errors)
        try:
            ElementTree.parse(SITE / "sitemap.xml")
        except ElementTree.ParseError as error:
            errors.append(f"sitemap.xml is not valid XML: {error}")
    if errors:
        print("\n".join(f"FAIL {error}" for error in errors))
        return 1
    print("Landing site checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
