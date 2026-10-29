# Contributing to Braindance

Bug reports, ideas, code, and documentation improvements are welcome.
Please follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## Bugs and ideas

Search [existing issues](https://github.com/vladleesi/braindance/issues) before opening one. For bugs, include the
platform, app/source version, reproduction steps, and expected/actual behavior. Remove credentials and personal
information from screenshots or logs. For ideas, explain the problem and proposed benefit.

## Making a change

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
