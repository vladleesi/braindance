# Contributing to Braindance

Thanks for helping improve Braindance. Bug reports, ideas, code, and documentation are all useful contributions.
You can start with an issue or a small fix; running a server is not a prerequisite for participating.

## Bugs and ideas

Use the [issue tracker](https://github.com/vladleesi/braindance/issues). Before opening an issue, check whether
someone has already reported it. For bugs, include the platform, app/source version, steps to reproduce, and
expected versus actual behavior. Screenshots or relevant logs help; remove credentials and personal information.
For suggestions, explain the problem you want to solve and how the change would help.

## Making a change

1. Fork the repository and create a branch from `develop`.
2. Follow the [development guide](docs/development.md) for the platforms you need. Backend work has its own
   [configuration and run instructions](backend/README.md). Documentation changes do not require the app toolchain.
3. Keep the change focused. For larger features or changes in behavior, opening an issue first helps agree on scope.
4. Run the relevant [validation checks](docs/development.md#validation) and review your diff. Add or update focused
   tests for behavior changes where appropriate. For UI changes, include a screenshot when it helps explain the result.
5. Open a pull request against `develop`. Explain the change, link any related issue, and state what you checked
   and what you could not run. An existing issue is not required for a small fix.

## Code and validation

Follow neighboring code and `.editorconfig`: four-space Kotlin indentation, 120-character lines, explicit imports,
and trailing commas. Keep shared behavior in `shared` and platform-specific work in the appropriate host or source
set. Declare dependency versions in `gradle/libs.versions.toml`; reuse existing UI components and dimensions.

Use `./gradlew ktlintFormat detekt` before submitting Kotlin or Gradle changes and review any corrections.
Detekt currently enables autocorrection. Run the affected platform/build tests described in the development guide;
CI runs the full validation workflow. If a platform is unavailable locally, say so in the pull request.
For documentation-only changes, check links and accuracy; a local app build is unnecessary.

Follow the existing [version and changelog conventions](docs/development.md#versions-and-releases) for runtime changes.
Documentation, tests, formatting, and CI-only changes do not require product version bumps.

## Licensing

First-party contributions are submitted under [MPL-2.0](LICENSE). Submit only work you own or have permission to
license on these terms, including any employer or client permissions. Copyright remains with its holders;
contributing does not transfer ownership.

Add `SPDX-License-Identifier: MPL-2.0` in an appropriate comment to new first-party source files. For formats where
inline comments are impractical, the [README license notice](README.md#license) applies. Preserve upstream terms
for third-party material and record origins, modifications, permissions, and required notices in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Identify any different terms before submitting third-party work.
After licensing changes, run `python3 .github/scripts/check-licenses.py`; it checks repository notices, not ownership
or a complete dependency audit.
