# Braindance

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin&logoColor=white&style=flat-square)](https://kotlinlang.org/docs/multiplatform.html) [![Platforms](https://img.shields.io/badge/platforms-Android%20%C2%B7%20iOS%20%C2%B7%20Web-0D9488?style=flat-square)](#platforms) [![App version](https://img.shields.io/github/v/release/vladleesi/braindance?filter=app-v%2A&label=app&color=2563EB&style=flat-square)](https://github.com/vladleesi/braindance/releases) [![CI](https://img.shields.io/github/actions/workflow/status/vladleesi/braindance/build-test-push.yml?branch=develop&event=push&label=CI&style=flat-square)](https://github.com/vladleesi/braindance/actions/workflows/build-test-push.yml) [![License](https://img.shields.io/badge/license-MPL--2.0-B45309?style=flat-square)](https://github.com/vladleesi/braindance/blob/master/LICENSE)

A game-tracking app for Android, iOS, and Web, built with Kotlin Multiplatform and Compose Multiplatform,
powered by IGDB.

> **Important:**
> It is an ongoing project and is currently under development. However, I'm actively working on adding new features, enhancing existing functionality, and addressing any issues or bugs.

![Braindance vibe](https://github.com/vladleesi/braindance/assets/30999008/cdb06536-ecbf-43ae-9336-3833a89a3718)

## Overview

Browse upcoming and popular games, game details, and giveaways using IGDB and GamerPower data.
Search, collections, and news screens are still in progress.

## Platforms

Braindance is a Kotlin full-stack project: Android, iOS, and browser clients share UI and logic through
[Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
and Compose Multiplatform.
The backend runs Kotlin/JVM with Ktor.

## Project layout

| Module | Purpose |
| --- | --- |
| `shared` | Shared Compose UI, app logic, and networking |
| `androidApp` | Android host |
| `iosApp` | Xcode and SwiftUI host |
| `webApp` | Kotlin/Wasm browser host |
| `backend` | Ktor API and server-side credentials |

## Requirements

- JDK 21 and the checked-in Gradle wrapper.
- Android: Android Studio and Android SDK API 37.
- iOS: macOS, Xcode, and an iOS Simulator; device builds also need signing configuration.
- Web: a modern browser with WasmGC support.

## IGDB credentials

Create a confidential app in the [Twitch Developer Console](https://dev.twitch.tv/console/apps), then set
`TWITCH_CLIENT_ID` and `TWITCH_CLIENT_SECRET` in the backend environment or hosting secret store.
IGDB uses these Twitch credentials; there is no separate IGDB key. Keep them out of Git and client builds.
See [backend configuration](backend/README.md#configuration) for details.

## Local setup

1. [Run or deploy the backend](backend/README.md#run-and-package) with its Twitch secrets and chosen rate limiter.
2. Create a root `local.properties` file. Add your Android SDK path if your environment needs it, and add the
   backend URL without a trailing slash:

   ```properties
   sdk.dir=/path/to/your/Android/sdk
   BACKEND_BASE_URL=https://api.example.com
   ```

   `local.properties` is ignored by Git. For CI or temporary builds, pass
   `-PBACKEND_BASE_URL=https://api.example.com` to Gradle or set
   `ORG_GRADLE_PROJECT_BACKEND_BASE_URL`. No Twitch client ID or secret belongs in this file or in a mobile build.
   Builds can omit the backend URL, but data screens need a reachable backend. For local Web development, add
   the development server's exact origin to the backend's `CORS_ALLOWED_ORIGINS`; localhost is not allowed implicitly.
3. Build the Android app from the repository root:

   ```sh
   ./gradlew :androidApp:assembleDebug
   ```

   Install the resulting APK from `androidApp/build/outputs/apk/debug/`, or run the `androidApp` configuration in
   Android Studio.
4. On macOS, open `iosApp/iosApp.xcodeproj` in Xcode, select an iOS Simulator, and run the `iosApp` target. Xcode
   invokes `:shared:embedAndSignAppleFrameworkForXcode`. For a physical device or distribution, select your own
   Apple development team and bundle identifier in Xcode.
5. Run the browser client from the repository root:

   ```sh
   ./gradlew :webApp:wasmJsBrowserDevelopmentRun
   ```

## Validation

Use JDK 21 and run checks from the repository root:

```sh
./gradlew :androidApp:build :androidApp:testDebugUnitTest :shared:testAndroidHostTest
./gradlew :webApp:wasmJsBrowserDistribution
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
./gradlew :backend:build :backend:installDist
./gradlew :androidApp:connectedCheck
```

The iOS check requires macOS/Xcode; connected Android checks require a running emulator or device.
Backend tests use `TEST_REDIS_URL` for optional Redis integration coverage; see the [backend guide](backend/README.md).
The [validation workflow](.github/workflows/build-test-push.yml) defines the full CI environment and checks.

## Releases

Android, iOS, and Web share version [0.3.2](https://github.com/vladleesi/braindance/releases/tag/app-v0.3.2)
from `version.xcconfig`; Android and iOS also share its build number. The backend has its own version, `0.5.1`.

CI promotes validated `develop` pushes to `master`. After validation, frontend version changes publish an
`app-vMAJOR.MINOR.PATCH` source release using the matching version entry in [CHANGELOG.md](CHANGELOG.md).
Group identical user-visible notes under a shared platform heading.
Pull requests and manual validation do not publish frontend releases.

The included publication workflows contain deployment steps. Review and replace or disable those steps for your
own infrastructure before enabling automation; they do not select or configure a hosting provider for you.

## Hosting

### Web

Use any HTTPS static host. Set `BACKEND_BASE_URL` before building, then run
`./gradlew :webApp:wasmJsBrowserDistribution` and serve
`webApp/build/dist/wasmJs/productionExecutable` over HTTPS with the `application/wasm` MIME type.
Enable compression, cache fingerprinted assets immutably, and keep the HTML entry point revalidatable.
Add the website's exact origin to the backend's `CORS_ALLOWED_ORIGINS`. Changing the backend URL requires rebuilding
the clients; changing the backend's CORS allowlist does not.

### Backend

Run the backend on any host supporting Java 21 or containers. Supply your own Twitch credentials at runtime,
expose the API over HTTPS, and set `PUBLIC_BASE_URL` to its external origin. Use the in-memory limiter for one
instance or shared Redis rate limiting for multiple instances. See [backend setup](backend/README.md) for commands,
configuration, health checks, and troubleshooting.

## License

First-party source, tests, build scripts, configuration, and documentation use [MPL-2.0](LICENSE).
Commercial use and forks are permitted. Preserve notices and MPL source rights; previously published GPL versions
retain their rights. Third-party code, fonts, artwork, and external data have separate terms and exclusions in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

For first-party files without inline comments, this notice applies:

> This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
> If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.

Submit contributions through pull requests with permission to license the work under MPL-2.0, including employer
or client permissions. Copyright stays with its holders. Add `SPDX-License-Identifier: MPL-2.0` to new first-party
source, preserve upstream notices, and record third-party origins and terms in the notices file.
After licensing changes, run `python3 .github/scripts/check-licenses.py` to check repository notices.

Distribute apps, browser executables, backend bundles, and containers with an exact-revision source link and
required third-party license/NOTICE texts, including shipped transitive, native, and base-image components.
Keep matching source accessible and preserve MPL source rights in executable EULAs. Verify API commercial and
attribution permissions separately before monetization.
See the [Mozilla FAQ](https://www.mozilla.org/en-US/MPL/2.0/FAQ/).

[Report an issue](https://github.com/vladleesi/braindance/issues).
