# Versions and releases

## Version changes

- Frontend: update the shared Android/iOS/Web version and positive Android/iOS build number in `version.xcconfig`.
  Add matching user-visible notes in [CHANGELOG.md](../CHANGELOG.md); group identical notes and omit unchanged platforms.
  Update the README version/release link.
- Backend: update `backend/build.gradle.kts`, [backend/CHANGELOG.md](../backend/CHANGELOG.md), and README/backend guide
  versions and examples.
- Use one increment per affected product for runtime changes. Docs, tests, formatting, and CI-only changes need none.

## Publication and recovery

CI validates `develop` and `master`; successful `develop` pushes fast-forward `master` without force pushes.
Web publishes to `web-app`, preserving history and removing obsolete generated files.

Frontend version changes create one `app-vMAJOR.MINOR.PATCH` source release after all checks pass for that commit
on `master`. The [release workflow](../.github/workflows/release-frontend.yml) uses its matching changelog entry.
Preserve existing releases. PRs and manual validation do not publish releases; source releases are separate from
mobile store downloads.

- Diverged branches: merge `master` into `develop` and push to validate again.
- Failed publication: rerun the failed workflow jobs.

See the [development guide](development.md) for setup and validation.
