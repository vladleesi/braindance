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

## 🎮 What's inside

Braindance helps you decide what to play next. No endless store pages — just a focused place to see what is hot,
what is coming and what is free.

| | |
| --- | --- |
| 🔥 **Popular Right Now** | See what everyone is playing today. |
| ⏳ **Most Anticipated** | Keep track of upcoming releases worth waiting for. |
| 🧭 **Game details & similar games** | Open any game, then jump to similar titles when you want more of the same. |
| 🎁 **Free giveaways** | Browse current giveaways, read the instructions and follow the link to claim. |

Game data comes from [IGDB](https://www.igdb.com/), and giveaways from [GamerPower](https://www.gamerpower.com/).

## 📱 Platforms

> [!NOTE]
> Braindance is in active development toward a public launch, with no confirmed release date. Personal tracking,
> collections, search results, news, and account management are not available yet.

| Platform | Availability |
| --- | --- |
| **Web (experimental)** | [Preview available now](https://app.braindancegg.com/) in a modern browser with WasmGC support. |
| Android and iOS | Official releases are planned; store downloads are not available yet. |

Web support is experimental: browser behavior, performance, and feature availability may change.
See [CHANGELOG.md](CHANGELOG.md) for updates across Android, iOS, and Web.

## 🤝 Help improve Braindance

- 🐞 [Report a bug](https://github.com/vladleesi/braindance/issues/new?template=bug_report.md).
- 💡 [Suggest a feature or share usability feedback](
  https://github.com/vladleesi/braindance/issues/new?template=feature_or_feedback.md).
- 🛠️ [Contribute code or documentation](CONTRIBUTING.md).

Feedback from the Web preview is welcome; you do not need to build the app to participate.

## 🧩 For developers

### Architecture

- **Clients (this repository, open source):** the Android, iOS, and Web apps share UI and application logic through
  Kotlin Multiplatform and Compose Multiplatform, with thin platform hosts in `androidApp`, `iosApp`, and `webApp`.
- **Braindance API (private):** developed and deployed separately from a private repository. It ranks the trending
  feed and similar-game recommendations and keeps Twitch credentials server-side.
- **Third-party data:** game data from [IGDB](https://www.igdb.com/) and giveaways from
  [GamerPower](https://www.gamerpower.com/), served through the API.

Client builds run without the API on built-in sample data; see the
[development guide](docs/project/development.md#configure-client-builds) and the
[API contract](docs/project/openapi.yaml).

| | |
| --- | --- |
| 📘 **[Development guide](docs/project/development.md)** | Requirements, project layout, local builds, debugging, and validation. |
| 🚀 **[Release guide](docs/project/releases.md)** | Versioning and publication workflows. |
| 🎨 **[Brand assets](docs/project/branding/README.md)** | Logos, icons, banners, palette, and usage guidance. |
| 🌐 **[Landing site](docs/)** | The static page served at [braindancegg.com](https://braindancegg.com/). |

The current frontend source version is
[0.15.2](https://github.com/vladleesi/braindance/releases/tag/app-v0.15.2).
GitHub releases provide source code, not store-ready mobile downloads.

The Web publication badge shows checks for the revision on `master`.

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
