# Versions and releases

## Version changes

- Frontend: update the shared Android/iOS/Web version and positive Android/iOS build number in `version.xcconfig`.
  Add matching user-visible notes in [CHANGELOG.md](../CHANGELOG.md) under `## MAJOR.MINOR.PATCH — YYYY-MM-DD`;
  group identical notes and omit unchanged platforms. Notable website or documentation changes go under
  `### Website and documentation`. Update the README version/release link.
- Use one increment for runtime changes. Docs, tests, formatting, and CI-only changes need none.

## Publication

- **Promotion:** CI validates `develop` and `master`. A successful `develop` push fast-forwards `master`, without
  force pushes.
- **Web app:** the validated distribution deploys to Cloudflare Pages with Wrangler. It needs the repository secrets
  `CLOUDFLARE_API_TOKEN` (an account token with only Cloudflare Pages edit access) and `CLOUDFLARE_ACCOUNT_ID`, and
  the repository variable `CLOUDFLARE_PAGES_PROJECT`. The JavaScript bundle and Wasm files are named by their
  content, so browsers cache them for a year, while the page always revalidates. Each deployment replaces the
  previous one as a whole; earlier deployments stay listed in Cloudflare Pages and can be rolled back there.
- **Landing site:** `landing` deploys to GitHub Pages from `master` only, through the `github-pages` environment;
  `docs` is never published. After promotion, validation dispatches
  [publish-landing.yml](../.github/workflows/publish-landing.yml) on `master`, and a direct `master` push calls
  it. `.github/scripts/check-landing.py` checks the site before every promotion and again before each deployment.
- **Source release:** a frontend version change creates one `app-vMAJOR.MINOR.PATCH` source release after all checks
  pass for that commit on `master`. Each version is released once, while its tag does not exist yet, so a version whose
  first push failed validation is still released when it passes. The
  [release workflow](../.github/workflows/release-frontend.yml) uses its matching changelog entry. Preserve existing
  releases. Pull requests and manual validation publish nothing; source releases are separate from mobile store
  downloads.
- **Tester builds:** each new frontend release also sends the Android staging build to Firebase App Distribution
  tester groups, after the release is created. iOS test builds are sent manually. See [Firebase](firebase.md).
- **API:** the Braindance API is released and deployed from its own repository; changes here never deploy it.

## Recovery

- Diverged branches: merge `master` into `develop` and push to validate again.
- Failed publication: rerun the failed workflow jobs.

See the [development guide](development.md) for setup and validation.
