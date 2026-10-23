# Developing Braindance

This guide is for exploring the source, contributing changes, or building your own version.
For current app availability, see the [README](../README.md#platforms); for pull requests, see
[CONTRIBUTING.md](../CONTRIBUTING.md).

## Requirements

- JDK 21 and the checked-in Gradle wrapper; no separate Gradle installation is needed.
- Android: Android Studio and Android SDK API 37. The app's minimum supported Android API is 24.
- iOS: macOS, Xcode, and an iOS Simulator. The app's minimum iOS version is 15.0; device builds also need signing.
- Web: a modern browser with WasmGC support for the Kotlin/Wasm client.

Install only the platform tooling needed for your change. Run Gradle commands from the repository root.

## Project layout

| Module | Purpose |
| --- | --- |
| `shared` | Shared Compose UI, application logic, networking, and platform implementations |
| `androidApp` | Android application host |
| `iosApp` | Xcode project and SwiftUI host |
| `webApp` | Kotlin/Wasm browser host, HTML shell, and web-only assets |
| `backend` | Kotlin/JVM Ktor API for IGDB and GamerPower |

The clients share Compose UI and ViewModels. Platform APIs belong in platform source sets or hosts;
the backend owns Twitch credentials and upstream API access.

## Backend and credentials

Data screens need a reachable backend. Use a backend available for your development work, or follow the
[backend run instructions](../backend/README.md#run-and-package) to start your own. Running your own backend is
an option for development, not a requirement for reporting issues or improving documentation.

For your own backend, create a confidential app in the [Twitch Developer Console](https://dev.twitch.tv/console/apps).
IGDB uses its client ID and secret; there is no separate IGDB key. GamerPower needs no key.
Supply `TWITCH_CLIENT_ID` and `TWITCH_CLIENT_SECRET` only to the backend environment or runtime secret store.
The backend guide covers the ignored `backend/.env`, exporting variables, health checks, and optional Redis.
Do not put Twitch credentials in Git, client builds, container images, or root `local.properties`.

## Configure client builds

Create an ignored root `local.properties` file:

```properties
sdk.dir=/path/to/your/Android/sdk
BACKEND_BASE_URL=https://api.example.com
```

Add `sdk.dir` only if your Android environment needs it. Use a backend URL reachable from the target device or
browser, without a trailing slash. The URL is public build configuration; it is not a secret.
For temporary builds, pass `-PBACKEND_BASE_URL=https://api.example.com` to Gradle or set
`ORG_GRADLE_PROJECT_BACKEND_BASE_URL` in the environment. Rebuild clients after changing the backend URL.

Builds can omit the URL, but backend-powered screens will not work without a configured, reachable service.
For local browser development, add the development server's exact origin, including scheme and port, to the
backend's `CORS_ALLOWED_ORIGINS`. Neither localhost nor `127.0.0.1` is allowed implicitly.
Devices and emulators have their own network context: their localhost may not reach the development machine.
Use an accessible HTTPS backend for mobile builds; the checked-in hosts do not enable unrestricted cleartext HTTP.

## Run a client

### Android

Run the `androidApp` configuration in Android Studio, or build a debug APK:

```sh
./gradlew :androidApp:assembleDebug
```

The APK is in `androidApp/build/outputs/apk/debug/`. Install it on a compatible emulator or device.

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode, select an iOS Simulator, and run the `iosApp` target.
Xcode invokes `:shared:embedAndSignAppleFrameworkForXcode`. For a physical device or distribution, configure
your own Apple development team and bundle identifier. The CI framework check targets the ARM64 iOS Simulator.

### Web

```sh
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

Open the local URL printed by the development server in a browser with WasmGC support.
Allow that exact origin in the backend's CORS configuration before checking data flows.

## Validation

Run the checks relevant to your changes; the [validation workflow](../.github/workflows/build-test-push.yml)
defines the complete CI environment. It checks licenses, Android builds and unit/instrumented tasks, the production
Web bundle, the iOS Simulator framework, and the backend build and container.

| Check | Command |
| --- | --- |
| Android build | `./gradlew :androidApp:build` |
| Android/shared unit tasks | `./gradlew :androidApp:testDebugUnitTest :shared:testAndroidHostTest` |
| Android device/emulator checks | `./gradlew :androidApp:connectedCheck` |
| Production Web bundle | `./gradlew :webApp:wasmJsBrowserDistribution` |
| iOS Simulator framework | `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` |
| Backend build and distribution | `./gradlew :backend:build :backend:installDist` |
| Formatting and static analysis | `./gradlew ktlintFormat detekt` |
| Repository license notices | `python3 .github/scripts/check-licenses.py` |

Android connected checks need a running emulator or device; CI covers API 24 and 35. iOS checks require macOS/Xcode.
Backend tests use `TEST_REDIS_URL` for optional integration tests against a disposable Redis database; CI supplies it.
Some frontend test tasks currently have no test sources, so their success alone does not establish behavior coverage.
Detekt enables autocorrection; review the diff after formatting and analysis. Report any unavailable platform checks
in your pull request. Documentation-only changes need link and accuracy checks, not a local application build.

## Versions and releases

Frontend version `0.3.2` comes from root `version.xcconfig`. Keep Android, iOS, and Web on the same SemVer value;
Android and iOS also share its positive integer build number. Update both values for frontend runtime changes and
maintain the matching `## MAJOR.MINOR.PATCH` entry in [CHANGELOG.md](../CHANGELOG.md). Group identical user-visible
notes under a shared platform heading; omit unchanged platforms. Keep the README's version and release link current.

Backend version `0.5.1` is independent, in `backend/build.gradle.kts`. Update it for backend runtime changes and keep
the README/backend guide version text and examples synchronized. Use one final increment per affected product;
documentation, tests, formatting, and CI-only changes do not need version bumps.

CI validates `develop` and `master`, then fast-forwards successful `develop` pushes to `master` without force pushes.
If the branches diverge, merge `master` into `develop` and push to run validation again.
Web publication appends commits to `web-app`, preserving its history and removing obsolete generated files.
Frontend version changes publish one `app-vMAJOR.MINOR.PATCH` source release after all checks pass for that commit on
`master`.
The [release workflow](../.github/workflows/release-frontend.yml) reads that version's changelog entry.
Existing releases are preserved; pull requests and manual validation do not publish frontend releases.
Retry failed publication through the failed workflow jobs.
GitHub source releases are separate from official mobile store distribution.

## Optional independent hosting

You can build and host your own version under the applicable licenses. For a browser build, set the backend URL,
run `./gradlew :webApp:wasmJsBrowserDistribution`, and serve
`webApp/build/dist/wasmJs/productionExecutable` on an HTTPS static host with the `application/wasm` MIME type.
Enable compression, cache fingerprinted assets immutably, and keep the HTML entry point revalidatable.
Add the website's exact origin to the backend's CORS allowlist; changing that allowlist needs no client rebuild.

For the server, use the [backend hosting reference](../backend/README.md#hosting).
The included publication workflows have deployment steps for the maintained project. Review and replace or disable
those steps for your own infrastructure before enabling them; they do not configure a new host automatically.

## Distribution

Distribute apps, browser executables, backend bundles, and containers with an exact-revision source link and required
third-party license/NOTICE texts, including shipped transitive, native, and base-image components. Keep matching
source accessible and preserve MPL source rights in executable EULAs. The first-party grant does not cover external
data, trademarks, or excluded assets; follow [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md).
Verify API commercial and attribution permissions separately before monetization, and check the final package's
notices and source disclosure. See the [Mozilla FAQ](https://www.mozilla.org/en-US/MPL/2.0/FAQ/).
