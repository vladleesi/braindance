# Braindance

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin&logoColor=white&style=flat-square)](https://kotlinlang.org/docs/multiplatform.html) [![Platforms: Android, iOS, and experimental Web](https://img.shields.io/badge/platforms-Android%20%C2%B7%20iOS%20%C2%B7%20Web%20%28experimental%29-0D9488?style=flat-square)](#platforms) [![Source release](https://img.shields.io/github/v/release/vladleesi/braindance?filter=app-v%2A&label=source&color=2563EB&style=flat-square)](https://github.com/vladleesi/braindance/releases) [![CI checks on develop](https://img.shields.io/github/actions/workflow/status/vladleesi/braindance/build-test-push.yml?branch=develop&event=push&label=CI&style=flat-square)](https://github.com/vladleesi/braindance/actions/workflows/build-test-push.yml?query=branch%3Adevelop+event%3Apush) [![License](https://img.shields.io/badge/license-MPL--2.0-B45309?style=flat-square)](#license)

Braindance is a game discovery and tracking app in development for Android and iOS, with an **experimental Web preview**.
Browse upcoming and popular games, explore their details, and find game giveaways in one place.

![Braindance vibe](https://github.com/vladleesi/braindance/assets/30999008/cdb06536-ecbf-43ae-9336-3833a89a3718)

## What you can do

- Browse anticipated and currently popular games.
- Open game details and discover related games.
- Find giveaways and view their details.

Personal tracking and collections, search, news, and profile screens are still in progress.
The current app uses data from IGDB and GamerPower.

## Platforms

| Platform | Availability |
| --- | --- |
| **Web (experimental)** | [Open the experimental Web preview](https://braindance.vladleesi.dev/). Requires WasmGC support. |
| Android and iOS | Developer builds are supported; official store releases are planned. |

Braindance is actively developed, and some screens are unfinished.
**Web support is experimental:** browser behavior, performance, and feature availability may change.
The preview requires a modern browser with WasmGC support.
The goal is an official app you can install or open without configuring API credentials or running servers.
Mobile store links will be added here when releases are available; no release date is announced.

The current frontend source version is
[0.4.1](https://github.com/vladleesi/braindance/releases/tag/app-v0.4.1).
GitHub releases currently provide source code, not store-ready mobile downloads.
See [CHANGELOG.md](CHANGELOG.md) for changes across Android, iOS, and Web.

## Help improve Braindance

Bug reports, ideas, code, and documentation improvements are welcome.
[Report an issue](https://github.com/vladleesi/braindance/issues) or read
[CONTRIBUTING.md](CONTRIBUTING.md) for a simple path from an idea to a pull request.
You do not need to run a backend to report a problem or suggest an improvement.

Braindance is open source under MPL-2.0. You can explore the code, contribute, modify it, build your own version,
and host it independently under the applicable license terms.

## For developers

Android, iOS, and Web share UI and application logic through Kotlin Multiplatform and Compose Multiplatform.
A Kotlin/JVM Ktor backend keeps Twitch credentials server-side and provides IGDB and GamerPower data.
Its independent version is `0.7.0`. See the [backend changelog](backend/CHANGELOG.md) for release notes.

- [Development guide](docs/development.md): requirements, project layout, local setup, validation, and releases.
- [Backend reference](backend/README.md): server configuration, API, packaging, and optional independent hosting.

Developer builds need a reachable backend for data features. The development guide covers that configuration.

## License

First-party source, tests, build scripts, configuration, and documentation use [MPL-2.0](LICENSE).
Modification, redistribution, commercial use, and forks are permitted under its terms. Preserve notices and MPL
source rights; previously published GPL versions retain their rights. Third-party code, fonts, artwork, and external
data have separate terms and exclusions in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

For first-party files without inline comments, this notice applies:

> This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
> If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.

See [contribution requirements](CONTRIBUTING.md#licensing) and
[distribution guidance](docs/development.md#distribution) for notices, corresponding source, and third-party terms.
