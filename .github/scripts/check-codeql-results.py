#!/usr/bin/env python3
# SPDX-License-Identifier: MPL-2.0

"""Fail CI when CodeQL reports high or critical security findings."""

import json
from pathlib import Path
import sys


def main():
    reports = sorted(Path(sys.argv[1]).rglob("*.sarif"))
    if not reports:
        print("CodeQL SARIF output is missing.", file=sys.stderr)
        return 1

    blocked = 0
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
                if severity >= 7:
                    blocked += 1
                    print(f"High/critical CodeQL finding: {rule_id}", file=sys.stderr)

    print(f"Reviewed {len(reports)} CodeQL reports; {blocked} high/critical findings.")
    return 1 if blocked else 0


if __name__ == "__main__":
    sys.exit(main())
