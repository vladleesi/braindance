# Contributing to Braindance

You can help improve Braindance by reporting bugs, suggesting features, sharing usability feedback,
or contributing code and documentation. You do not need to build the app or run a backend to give feedback;
you can use the [experimental Web preview](https://app.braindancegg.com/).
Please follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## Report a bug

Search [existing issues](https://github.com/vladleesi/braindance/issues) first; if someone has reported the same
problem, add any useful details there. Otherwise, [open a bug report](https://github.com/vladleesi/braindance/issues/new?template=bug_report.md)
and describe:

- What you were trying to do, what happened, and what you expected.
- Steps to reproduce the problem, if you know them.
- Your platform and browser or operating system, plus the app version if known.
  For the Web preview, the page URL and approximate time can help.

Screenshots can help explain a problem. Logs are optional; remove credentials and personal information
from any screenshots, links, or logs before sharing them publicly. Partial reports are welcome if you cannot
reproduce the issue consistently or find the version.

## Suggest a feature or share feedback

Search existing issues, then [share an idea or usability feedback](https://github.com/vladleesi/braindance/issues/new?template=feature_or_feedback.md).
Explain what you wanted to do and what would make it easier. Feedback about confusing navigation, unclear text,
or accessibility is welcome. A proposed solution is optional; you do not need a technical design.

## Contribute code or documentation

Follow `.editorconfig`, neighboring code, and the [project conventions](AGENTS.md#project-conventions).

1. Fork the repository and create a branch from `develop`.
2. Use the [development guide](docs/development.md) or [backend setup](backend/README.md) as needed.
   Documentation changes need no app toolchain.
3. Keep changes focused. For larger features or behavior changes, an issue helps agree on scope; small fixes need none.
4. Run affected [validation checks](docs/development.md#validation), add focused behavior tests where appropriate,
   and review your diff. For Kotlin/Gradle changes, run `./gradlew ktlintFormat detekt` and review autocorrections.
   Docs-only changes need link and accuracy checks, not an app build.
5. Open a pull request against `develop`. Describe the change, link related issues, and report checks run or skipped.
   Include UI screenshots when useful.

Follow the [release conventions](docs/releases.md#version-changes) for version and changelog updates.

## Licensing

Submit first-party contributions under [MPL-2.0](LICENSE), with permission from all relevant rightsholders,
including employers or clients. Copyright ownership stays with its holders.

Add `SPDX-License-Identifier: MPL-2.0` in a comment to new first-party source files; use the
[README notice](README.md#license) where inline comments are impractical. Preserve third-party terms, identify
different licenses before submission, and record origins, modifications, permissions, and required notices in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

After licensing changes, run `python3 .github/scripts/check-licenses.py`. It checks notices, not ownership
or complete dependency compliance.
