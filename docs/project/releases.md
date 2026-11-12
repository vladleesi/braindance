# Versions and releases

## Version changes

- Frontend: update the shared Android/iOS/Web version and positive Android/iOS build number in `version.xcconfig`.
  Add matching user-visible notes in [CHANGELOG.md](../../CHANGELOG.md); group identical notes and omit unchanged
  platforms. Update the README version/release link.
- Use one increment for runtime changes. Docs, tests, formatting, and CI-only changes need none.

## Publication and recovery

CI validates `develop` and `master`; successful `develop` pushes fast-forward `master` without force pushes.
The Braindance API is released and deployed from its own repository; changes here never deploy it.
Web deploys the validated distribution to Cloudflare Pages with Wrangler. The workflow needs the repository secrets
`CLOUDFLARE_API_TOKEN` (an account token with only Cloudflare Pages edit access) and `CLOUDFLARE_ACCOUNT_ID`, and the
repository variable `CLOUDFLARE_PAGES_PROJECT`. The JavaScript bundle and Wasm files are named by their content, so
browsers cache them for a year, while the page always revalidates. Each deployment replaces the previous one as a whole;
earlier deployments stay listed in Cloudflare Pages and can be rolled back there.
The landing site in `docs` deploys to GitHub Pages from `master` only, through the `github-pages` environment: after
promotion, validation dispatches [publish-landing.yml](../../.github/workflows/publish-landing.yml) on `master`, and a
direct `master` push calls it. `docs/project` is never published. `.github/scripts/check-landing.py` checks the site
before every promotion and again before each deployment.

Frontend version changes create one `app-vMAJOR.MINOR.PATCH` source release after all checks pass for that commit
on `master`. The [release workflow](../../.github/workflows/release-frontend.yml) uses its matching changelog entry.
Preserve existing releases. PRs and manual validation do not publish releases; source releases are separate from
mobile store downloads.

- Diverged branches: merge `master` into `develop` and push to validate again.
- Failed publication: rerun the failed workflow jobs.

See the [development guide](development.md) for setup and validation.
