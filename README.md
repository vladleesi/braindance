# Braindance
[![Development status: in development](https://img.shields.io/badge/status-in%20development-FCEE0A?style=flat-square&labelColor=0B0E14)](#-platforms) [![Platforms: Android, iOS, and experimental Web](https://img.shields.io/badge/platforms-Android%20%C2%B7%20iOS%20%C2%B7%20Web%20%28experimental%29-22ECFF?style=flat-square&labelColor=0B0E14)](#-platforms) [![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin&logoColor=white&style=flat-square&labelColor=0B0E14)](https://kotlinlang.org/docs/multiplatform.html) [![App source release](https://img.shields.io/github/v/release/vladleesi/braindance?filter=app-v%2A&label=source&color=22ECFF&labelColor=0B0E14&style=flat-square)](https://github.com/vladleesi/braindance/releases) [![CI checks on develop](https://img.shields.io/github/actions/workflow/status/vladleesi/braindance/build-test-push.yml?branch=develop&event=push&label=CI&labelColor=0B0E14&style=flat-square)](https://github.com/vladleesi/braindance/actions/workflows/build-test-push.yml?query=branch%3Adevelop+event%3Apush) [![Web publication checks on master](https://img.shields.io/github/check-runs/vladleesi/braindance/master?nameFilter=Publish%20Promoted%20Web%20App%20%2F%20Deploy%20Web%20App%20to%20Cloudflare%20Pages&label=Web%20publication&labelColor=0B0E14&style=flat-square)](https://github.com/vladleesi/braindance/actions/workflows/build-test-push.yml?query=branch%3Adevelop+event%3Apush) [![License: MPL-2.0](https://img.shields.io/badge/license-MPL--2.0-FF2E88?style=flat-square&labelColor=0B0E14)](#-license)

<div align="center">

<a href="https://braindancegg.com/"><img src="docs/project/branding/banner/readme-banner.png"
  alt="braindance — games / backlog / what's next" width="100%"></a>

### Find what to play next.

Popular and upcoming games, similar titles and free giveaways — in one place.<br>
Open-source clients for **Android · iOS · Web**.

**[braindancegg.com](https://braindancegg.com/)** &nbsp;·&nbsp; **[Open the web app](https://app.braindancegg.com/)**
&nbsp;·&nbsp; [Report a bug](https://github.com/vladleesi/braindance/issues/new?template=bug_report.md)
&nbsp;·&nbsp; [Contribute](CONTRIBUTING.md)

</div>

---

Braindance is a game discovery app in active development. This repository holds its open-source Android, iOS, and
Web clients, built with Kotlin Multiplatform and Compose Multiplatform. The API they use is maintained privately.

## ✨ Features

- **Popular Right Now** and **Most Anticipated** game feeds.
- **Game details** with similar games.
- **Giveaways** with instructions and claim links.

Not available yet: personal tracking, collections, search results, news, and accounts. Game data comes from
[IGDB](https://www.igdb.com/), and giveaways from [GamerPower](https://www.gamerpower.com/).

## 📱 Platforms

| Platform | Status |
| --- | --- |
| **Web (experimental)** | [Preview](https://app.braindancegg.com/) in up-to-date Chrome, Edge, Firefox, or Safari; behavior, performance, and features may change. |
| Android and iOS | Build from source; store releases are planned, with no confirmed date. |

See [CHANGELOG.md](CHANGELOG.md) for changes in each version.

## 🚀 Quick start

Requirements: JDK 21; Android Studio for Android; macOS and Xcode for iOS.

```sh
git clone https://github.com/vladleesi/braindance.git
cd braindance
echo "API_FIXTURES=true" >> local.properties  # offline sample data, no API needed
./gradlew :webApp:wasmJsBrowserDevelopmentRun  # Web; or :androidApp:assembleDebug for Android
```

For iOS, open `iosApp/iosApp.xcodeproj` in Xcode. The [development guide](docs/project/development.md) covers
configuration, code style, and validation.

## 🧩 Architecture

- **Clients (this repository):** the Android, iOS, and Web apps share UI and application logic in `shared`, with
  thin platform hosts in `androidApp`, `iosApp`, and `webApp`.
- **Braindance API (private):** developed and deployed separately. It ranks the trending feed and similar-game
  recommendations and keeps Twitch credentials server-side. The [API contract](docs/project/openapi.yaml) describes
  what the clients use.
- **Third-party data:** game data from IGDB and giveaways from GamerPower, served through the API.

## 📚 Documentation

| Guide | Covers |
| --- | --- |
| [Development guide](docs/project/development.md) | Requirements, configuration, code style, running, validation, and hosting. |
| [API contract](docs/project/openapi.yaml) | Endpoints and fields the clients use (OpenAPI). |
| [Releases](docs/project/releases.md) | Versioning, publication, and recovery. |
| [Security checks](docs/project/security.md) | CI security checks, repository settings, and Android backups. |
| [Brand assets](docs/project/branding/README.md) | Logos, icons, banners, palette, and usage. |
| [Landing site](docs/) | The static page served at [braindancegg.com](https://braindancegg.com/). |

The current frontend source version is
[0.15.3](https://github.com/vladleesi/braindance/releases/tag/app-v0.15.3).
GitHub releases provide source code, not store-ready mobile downloads.
The Web publication badge shows checks for the revision on `master`.

## 🤝 Contributing

Bug reports, ideas, and pull requests are welcome; see [CONTRIBUTING.md](CONTRIBUTING.md). Report security issues
privately as described in [SECURITY.md](SECURITY.md).

## 📄 License

First-party source, tests, build scripts, configuration, and documentation in this repository use
[MPL-2.0](LICENSE). Modification, redistribution, commercial use, and forks are permitted under its terms; preserve
notices and MPL source rights.

- Previously published GPL versions retain their rights.
- The private backend is not part of this repository. Backend source in earlier Git history keeps the terms it was
  published under and is no longer maintained here.
- Third-party code, fonts, artwork, and external data have separate terms and exclusions in
  [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Game covers shown on the landing site are © their respective
  owners, via [IGDB](https://www.igdb.com/).

For first-party files without inline comments, this notice applies:

> This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
> If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.

See [contribution requirements](CONTRIBUTING.md#licensing) and
[distribution guidance](docs/project/development.md#distribution) for notices, corresponding source, and third-party
terms.

---

<div align="center">

<a href="https://braindancegg.com/"><img src="docs/mark.svg" alt="Braindance mark" width="40"></a>

<sub><b>braindance</b> · games / backlog / what's next</sub>

</div>
