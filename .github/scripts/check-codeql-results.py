#!/usr/bin/env python3
# SPDX-License-Identifier: MPL-2.0

"""Fail CI when CodeQL reports high or critical security findings, apart from reviewed exceptions."""

import json
from pathlib import Path
import sys

# Reviewed findings that may stay, each limited to one rule in one file. An exception holds only while its file still
# contains every required line and none of the forbidden ones, so a change that undoes the reason fails CI again.
# Keep docs/security.md in step.
EXCEPTIONS = [
    {
        "rule": "java/android/websettings-javascript-enabled",
        "path": (
            "shared/src/androidMain/kotlin/dev/vladleesi/braindanceapp/ui/components/media/"
            "YouTubePlayer.android.kt"
        ),
        # YouTube's player is a script, so its WebView needs JavaScript. It loads only the app's own player page and
        # YouTube's privacy-enhanced frame and blocks every other navigation, as long as it keeps these limits.
        "requires": ["allowFileAccess = false", "allowContentAccess = false", "setSupportMultipleWindows(false)"],
        "forbids": ["addJavascriptInterface", "setAllowUniversalAccessFromFileURLs", "setAllowFileAccessFromFileURLs"],
    },
]


def result_path(result):
    """The repository-relative path of a result's first location, or None."""
    for location in result.get("locations", []):
        uri = location.get("physicalLocation", {}).get("artifactLocation", {}).get("uri")
        if uri:
            return uri.removeprefix("file://").removeprefix("./")
    return None


def exception_for(rule_id, path, root):
    """The exception that covers this finding, or None. Prints why an exception for its rule does not apply."""
    for exception in EXCEPTIONS:
        if exception["rule"] != rule_id or path is None:
            continue
        if path != exception["path"] and not path.endswith("/" + exception["path"]):
            continue
        source = root / exception["path"]
        if not source.is_file():
            print(f"Exception for {rule_id} does not apply: {exception['path']} is missing.", file=sys.stderr)
            return None
        text = source.read_text()
        missing = [line for line in exception["requires"] if line not in text]
        present = [line for line in exception["forbids"] if line in text]
        if missing or present:
            print(
                f"Exception for {rule_id} does not apply: {exception['path']} "
                f"lacks {missing} or contains {present}.",
                file=sys.stderr,
            )
            return None
        return exception
    return None


def main():
    reports = sorted(Path(sys.argv[1]).rglob("*.sarif"))
    root = Path(sys.argv[2]) if len(sys.argv) > 2 else Path.cwd()
    if not reports:
        print("CodeQL SARIF output is missing.", file=sys.stderr)
        return 1

    blocked = 0
    accepted = 0
    for report in reports:
        for run in json.loads(report.read_text())["runs"]:
            tool = run["tool"]
            components = [tool["driver"], *tool.get("extensions", [])]
            rules = {
                rule["id"]: rule
                for component in components
                for rule in component.get("rules", [])
            }
            for result in run.get("results", []):
                rule_id = result.get("ruleId") or result.get("rule", {}).get("id")
                rule = rules.get(rule_id, {})
                severity = float(rule.get("properties", {}).get("security-severity", 0))
                if severity < 7:
                    continue
                path = result_path(result)
                if exception_for(rule_id, path, root):
                    accepted += 1
                    print(f"Reviewed exception: {rule_id} in {path}")
                else:
                    blocked += 1
                    print(f"High/critical CodeQL finding: {rule_id} in {path}", file=sys.stderr)

    print(
        f"Reviewed {len(reports)} CodeQL reports; {blocked} high/critical findings, "
        f"{accepted} reviewed exceptions."
    )
    return 1 if blocked else 0


if __name__ == "__main__":
    sys.exit(main())
