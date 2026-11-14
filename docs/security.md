# Security checks

[Security checks](../.github/workflows/security.yml) run during validation, weekly, and manually. Scheduled scans
never deploy. A failure blocks promotion and publication, including Web publication on direct `master` pushes.
To report a vulnerability, see the [security policy](../SECURITY.md).

## Checks

- **CodeQL:** blocks high and critical findings in Android code, including shared Kotlin. Swift, iOS-specific
  Kotlin, and Wasm coverage is incomplete. The backup-query exception is in
  [the config](../.github/codeql-config.yml).
- **Dependency Review:** blocks new high and critical vulnerabilities on pushes and pull requests; incomplete
  snapshots fail.
  - Both revisions resolve with read-only permissions; the privileged snapshot submission never runs pull request
    code.
  - Push reviews wait for the submission. Submission for fork pull requests needs its workflow on the default branch.
  - Snapshots also refresh weekly.
- **zizmor:** checks workflows against [the policy](../.github/zizmor.yml); Actions use readable release tags.

## Repository settings

Enable Dependency graph, Dependabot alerts, secret scanning, push protection, and private vulnerability reporting.
Require the security and validation checks in branch rulesets; workflow dependencies alone do not prevent direct
pushes.

## Android backups

- Backup and device transfer allow only `filesDir/backup_exports/`, for reviewed, non-sensitive exports; none are
  written yet.
- Keep credentials, private databases, and device-specific state outside it.
- Cloud backup requires client-side encryption on Android 9+; Android 7–8.1 exclude all app data.
- Review the legacy and Android 12+ rules before adding persistent user data.

See the [development guide](development.md#validation) for local validation commands.
