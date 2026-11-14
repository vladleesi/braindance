#!/usr/bin/env python3
# SPDX-License-Identifier: MPL-2.0

"""Check source-tree licensing records without builds or network access."""

import hashlib
from pathlib import Path
import subprocess
import sys


ROOT = Path(__file__).resolve().parents[2]
ICON_DIR = Path("shared/src/commonMain/kotlin/dev/vladleesi/braindanceapp/ui/style/icons")
LUCIDE_ICONS = {
    "IconBell.kt", "IconCalendarDays.kt", "IconChevronLeft.kt", "IconCompass.kt",
    "IconGithub.kt", "IconHouse.kt", "IconLibrary.kt", "IconNewspaper.kt", "IconPlus.kt", "IconRss.kt",
    "IconSearch.kt", "IconSlidersHorizontal.kt", "IconUsers.kt", "IconDatabase.kt",
    "IconFileText.kt", "IconGift.kt",
}
SIMPLE_ICONS = {"IconBlueskyLogo.kt", "IconThreadsLogo.kt", "IconXLogo.kt"}
SOURCE_SUFFIXES = {".kt", ".kts", ".swift", ".py", ".sh"}
# Official Mozilla plain text and the upstream copies linked in THIRD_PARTY_NOTICES.md.
LICENSE_HASHES = {
    "LICENSE": "3f3d9e0024b1921b067d6f7f88deb4a60cbe7a78e76c64e3f1d7fc3b779b9d04",
    "LICENSES/Apache-2.0.txt": "cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30",
    "LICENSES/CC0-1.0.txt": "a2010f343487d3f7618affe54f789f5487602331c0a8d03f49e9a7c547cf0499",
    "LICENSES/Feather-MIT.txt": "308028e93fcf84972523cdf6e616f73168546b4953895f516d01287f16fe7bee",
    "LICENSES/Lucide-ISC.txt": "1e7290b35280a048667bbf0ebabac1c7fd52a75300e8b2946ac165715997f2bc",
    "LICENSES/Poppins-OFL-1.1.txt": "8503c30a4d7e09c4c09015fee42f829f18acbe6330755e9930ba50ea44eaa157",
}


def main():
    errors = []
    for name, expected in LICENSE_HASHES.items():
        path = ROOT / name
        if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != expected:
            errors.append(f"{name}: missing or differs from the recorded upstream text")

    if not (ROOT / "THIRD_PARTY_NOTICES.md").is_file():
        errors.append("THIRD_PARTY_NOTICES.md: missing licensing documentation")
    if "[MPL-2.0](LICENSE)" not in (ROOT / "README.md").read_text():
        errors.append("README.md: missing current license link")

    output = subprocess.check_output(
        ["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard"], cwd=ROOT,
    )
    checked = 0
    for name in sorted(set(output.decode().split("\0")) - {""}):
        path = Path(name)
        # A tracked file that is already deleted in the working tree has no header to check before the commit.
        if path.suffix not in SOURCE_SUFFIXES or not (ROOT / path).is_file():
            continue
        identifier = "MPL-2.0"
        if path.parent == ICON_DIR and path.name in LUCIDE_ICONS:
            identifier = "ISC AND MIT"
        elif path.parent == ICON_DIR and path.name in SIMPLE_ICONS:
            identifier = "CC0-1.0"
        elif path == ICON_DIR / "IconOpenInNew.kt":
            identifier = "Apache-2.0"
        prefix = "#" if path.suffix in {".py", ".sh"} else "//"
        lines = (ROOT / path).read_text().splitlines()
        index = 1 if lines and lines[0].startswith("#!") else 0
        expected = f"{prefix} SPDX-License-Identifier: {identifier}"
        if len(lines) <= index or lines[index] != expected:
            errors.append(f"{name}: expected {expected}")
        checked += 1

    for name, marker in (("gradlew", "# Licensed under the Apache License"),
                         ("gradlew.bat", "@rem Licensed under the Apache License")):
        if marker not in (ROOT / name).read_text():
            errors.append(f"{name}: missing upstream Apache notice")
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"Licensing records verified for {checked} source files; ownership/dependency audit remains separate.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
