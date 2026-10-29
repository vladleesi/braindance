# Security checks

[Security checks](../.github/workflows/security.yml) run during validation, weekly, and manually.
Scheduled scans do not deploy. Failures block promotion and publication, including Web on direct `master` pushes.

- **CodeQL:** blocks high/critical backend and Android findings, including shared Kotlin. Swift, iOS-specific Kotlin,
  and Wasm coverage is incomplete. The backup-query exception is in [the config](../.github/codeql-config.yml).
- **Dependency Review:** blocks new high/critical vulnerabilities on pushes and PRs; incomplete snapshots fail.
  Both revisions resolve with read-only permissions; privileged submission never executes PR code. Push reviews
  wait for submission; fork PR submission needs its workflow on the default branch. Snapshots also refresh weekly.
- **Trivy:** blocks high/critical backend-image OS/library vulnerabilities during validation and before publication,
  including unfixed findings. Review findings instead of broadly suppressing them.
- **zizmor:** checks workflows under [the policy](../.github/zizmor.yml); Actions use readable release tags.

Enable Dependency graph, Dependabot alerts, secret scanning, and push protection. Require security and validation
checks in branch rulesets; workflow dependencies alone do not prevent direct pushes.

## Android backups

Backup/device transfer allows only `filesDir/backup_exports/` for reviewed, non-sensitive exports; none are currently
written. Keep credentials, private databases, and device-specific state outside it. Cloud backup requires client-side
encryption on Android 9+; Android 7–8.1 excludes all app data. Review legacy and Android 12+ rules before adding
persistent user data.

See the [development guide](development.md#validation) for local validation commands.
