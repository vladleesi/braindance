# Developing Braindance

For app availability, see the [README](../README.md#platforms); for pull requests, see
[CONTRIBUTING.md](../CONTRIBUTING.md). Install only the tooling you need and run commands from the repository root.

## Requirements

- JDK 21 and the checked-in Gradle wrapper. It is the newest maintained Temurin release below JDK 25;
  JDK 22–24 are end-of-life, and Detekt 1.23.8 cannot run on JDK 25.
  The backend container uses Java 25 independently of the build JDK.
- Android: Android Studio Rabbit 2 (2026.2.2) or newer with AGP 9.5 support and SDK API 37;
  minimum supported device API 24.
- iOS: macOS, Xcode, and an iOS Simulator; minimum iOS 15.0. Device builds need your own signing configuration.
- Web: an **experimental preview** requiring a modern browser with WasmGC support.
  Browser behavior, performance, and feature availability may change.

`shared` owns Compose UI, ViewModels, and application logic. `androidApp`, `iosApp`, and `webApp` own their
platform hosts; `webApp` also owns the HTML shell and web-only assets. `backend` is the Kotlin/JVM Ktor API.
Keep platform APIs in platform source sets or hosts and Twitch credentials on the backend.

## Configure client builds

Data screens need a reachable backend. Use one available for development or follow the
[backend setup](../backend/README.md#run-and-package), including its Twitch credential configuration.
GamerPower needs no key. Keep credentials out of Git, client builds, container images, and `local.properties`.

Create an ignored root `local.properties`:

```properties
sdk.dir=/path/to/your/Android/sdk
BACKEND_BASE_URL=https://api.example.com
```

Add `sdk.dir` only when needed. The backend URL is public build configuration, must be reachable from the target,
and must have no trailing slash. Alternatively, pass `-PBACKEND_BASE_URL=https://api.example.com` or set
`ORG_GRADLE_PROJECT_BACKEND_BASE_URL`. Rebuild clients after changing it. Builds without a URL cannot load data.

For browsers, add the exact origin, including scheme and port, to the backend's `CORS_ALLOWED_ORIGINS`;
localhost and `127.0.0.1` are not implicitly allowed. Device/emulator localhost may not reach your development
machine. Use HTTPS for mobile builds; the hosts do not enable unrestricted cleartext HTTP.

## Run a client

| Platform | Start |
| --- | --- |
| Android | Run `androidApp` in Android Studio or `./gradlew :androidApp:assembleDebug`. |
| iOS | Open `iosApp/iosApp.xcodeproj` in Xcode and run `iosApp` on an iOS Simulator. |
| Web (experimental) | Run `./gradlew :webApp:wasmJsBrowserDevelopmentRun` and open the printed URL. |

Android APKs are in `androidApp/build/outputs/apk/debug/`. Xcode invokes
`:shared:embedAndSignAppleFrameworkForXcode`; configure your own development team and bundle identifier for devices
or distribution. Allow the Web development server's exact origin in backend CORS before checking data flows.

## Validation

Run checks relevant to the change. The [validation workflow](../.github/workflows/build-test-push.yml) defines
the full CI environment, including container checks. See [security checks](security.md) for coverage and limitations.

| Check | Command |
| --- | --- |
| Android build | `./gradlew :androidApp:build` |
| Android/shared unit tasks | `./gradlew :androidApp:testDebugUnitTest :shared:testAndroidHostTest` |
| Android device/emulator | `./gradlew :androidApp:connectedCheck` |
| Production Web bundle | `./gradlew :webApp:wasmJsBrowserDistribution` |
| iOS Simulator framework | `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` |
| Backend build and distribution | `./gradlew :backend:build :backend:installDist` |
| Formatting and static analysis | `./gradlew ktlintFormat detekt` |
| License notices | `python3 .github/scripts/check-licenses.py` |

Android connected checks need a device/emulator; CI covers API 24 and 35, including packaged backup rules.
iOS checks need macOS/Xcode and target the ARM64 Simulator. Backend Redis integration tests use `TEST_REDIS_URL`
with a disposable database; CI supplies it. Some frontend test tasks have no test sources, so success alone
does not establish behavior coverage. Review autocorrections from formatting/Detekt and report unavailable checks.
Documentation-only changes need link and accuracy checks, not an app build.

## Versions and releases

Frontend version `0.4.0` and its shared Android/iOS build number come from `version.xcconfig`.
Backend version `0.7.0` is independent, in `backend/build.gradle.kts`. Runtime changes require the affected product's
version, changelog, and README updates; documentation, tests, formatting, and CI-only changes need no version bump.
See [release conventions and publication](releases.md) for the full workflow and recovery steps.

## Optional independent hosting

For Web, configure the backend URL, run `./gradlew :webApp:wasmJsBrowserDistribution`, and serve
`webApp/build/dist/wasmJs/productionExecutable` over HTTPS with compression and the `application/wasm` MIME type.
Cache fingerprinted assets immutably; keep HTML revalidatable. Allow the exact website origin in backend CORS;
allowlist changes need no client rebuild. For the server, see [backend hosting](../backend/README.md#hosting).

Before enabling publication workflows for your own infrastructure, replace or disable their existing deployment
steps; they do not configure a new host automatically.

## Distribution

Ship an exact-revision source link and required third-party notices with apps, browser executables, backend bundles,
and containers, including transitive, native, and base-image components. Keep corresponding source accessible and
preserve MPL source rights in executable EULAs. External data, trademarks, and excluded assets have separate terms:
see [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md). Verify API commercial and attribution permissions before
monetization and review the final package's notices and source disclosure. See the
[Mozilla FAQ](https://www.mozilla.org/en-US/MPL/2.0/FAQ/).
