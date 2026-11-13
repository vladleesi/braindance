# Developing Braindance

This guide covers building, running, and validating the Android, iOS, and Web clients. Run commands from the
repository root and install only the tooling you need. For feedback and pull requests, see
[CONTRIBUTING.md](../../CONTRIBUTING.md).

## Requirements

- JDK 21 and the checked-in Gradle wrapper. Detekt 1.23.8 cannot run on JDK 25.
- Android: Android Studio Rabbit 2 (2026.2.2) or newer with AGP 9.5 support and SDK API 37;
  minimum supported device API 24.
- iOS: macOS, Xcode, and an iOS Simulator; minimum iOS 15.0. Device builds need your own signing configuration.
- Web: a modern browser with WasmGC support. Web support is experimental.

`shared` owns Compose UI, ViewModels, and application logic. `androidApp`, `iosApp`, and `webApp` are the platform
hosts; `webApp` also owns the HTML shell and web-only assets. Keep platform APIs in platform source sets or hosts,
and never put API credentials in client code or builds.

## Configure client builds

Add this line to the ignored root `local.properties`, creating it if needed:

```properties
API_FIXTURES=true
```

- **`API_FIXTURES=true`** serves invented games, giveaways, and plain-color images from the app itself, on every
  platform, with no API or network. Builds without the flag contain no fixture code.
- Release builds set `BACKEND_BASE_URL` in CI instead; setting both fails the build. The API is maintained privately
  and is not part of this repository; [openapi.yaml](openapi.yaml) describes what the clients use.

Android Studio adds `sdk.dir` to this file itself; command-line builds without it need `ANDROID_HOME` or `sdk.dir`.
Gradle properties (`-PAPI_FIXTURES=true`, or `ORG_GRADLE_PROJECT_*` variables) override `local.properties`.
Rebuild after changing either.

## Run a client

| Platform | Start |
| --- | --- |
| Android | Run `androidApp` in Android Studio or `./gradlew :androidApp:assembleDebug`. |
| iOS | Open `iosApp/iosApp.xcodeproj` in Xcode and run `iosApp` on an iOS Simulator. |
| Web (experimental) | Run `./gradlew :webApp:wasmJsBrowserDevelopmentRun` and open the printed URL. |

Android APKs are in `androidApp/build/outputs/apk/debug/`. Xcode invokes
`:shared:embedAndSignAppleFrameworkForXcode`; configure your own development team and bundle identifier for devices
or distribution.

## Validation

Run checks relevant to the change. The [validation workflow](../../.github/workflows/build-test-push.yml) defines
the full CI environment. See [security checks](security.md) for coverage and limitations.

| Check | Command |
| --- | --- |
| Android build | `./gradlew :androidApp:build` |
| Android/shared unit tasks | `./gradlew :androidApp:testDebugUnitTest :shared:testAndroidHostTest` |
| Android device/emulator | `./gradlew :androidApp:connectedCheck` |
| Android baseline profile | `./gradlew :androidApp:generateReleaseBaselineProfile` (rooted emulator or API 33+ device, backend reachable); commit the regenerated `androidApp/src/release/generated/baselineProfiles/` |
| Production Web bundle | `./gradlew :webApp:wasmJsBrowserDistribution` |
| iOS Simulator framework | `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` |
| Formatting and static analysis | `./gradlew ktlintFormat detekt` |
| License notices | `python3 .github/scripts/check-licenses.py` |

Android connected checks need a device/emulator; CI covers API 24 and 35, including packaged backup rules.
iOS checks need macOS/Xcode and target the ARM64 Simulator. Some test tasks have no test sources, so success alone
does not establish behavior coverage. Review autocorrections from formatting/Detekt and report unavailable checks.
Documentation-only changes need link and accuracy checks, not an app build.

When the client's API use changes, update [openapi.yaml](openapi.yaml), the models, and the fixtures in
`shared/src/apiFixtures` together. `ApiContractTest`, in the shared unit tasks, fails when the contract and the
client's endpoints or models differ.

Version and changelog rules are in [releases.md](releases.md#version-changes).

## Optional independent hosting

Hosting your own Web build needs your own API that implements [openapi.yaml](openapi.yaml); this repository does
not include one. Set its HTTPS URL as `BACKEND_BASE_URL`, run `./gradlew :webApp:wasmJsBrowserDistribution`, and
serve `webApp/build/dist/wasmJs/productionExecutable` over HTTPS with compression and the `application/wasm` MIME
type. The API's CORS allowlist must include the site's exact origin.

Serve `index.html` for any path that is not a file, so deep links such as `/games/42` load the app. Cache
fingerprinted assets (`*.wasm`, `webApp.*`) immutably and keep HTML revalidatable. The distribution's `_headers`
file sets these lifetimes and the security headers on Cloudflare Pages; mirror them on other hosts.

Before enabling publication workflows for your own infrastructure, replace or disable their existing deployment
steps; they do not configure a new host automatically.

## Distribution

Ship an exact-revision source link and required third-party notices with apps and browser executables, including
transitive and native components. Keep corresponding source accessible and preserve MPL source rights in executable
EULAs. External data, trademarks, and excluded assets have separate terms: see
[THIRD_PARTY_NOTICES.md](../../THIRD_PARTY_NOTICES.md). Verify API commercial and attribution permissions before
monetization and review the final package's notices and source disclosure. See the
[Mozilla FAQ](https://www.mozilla.org/en-US/MPL/2.0/FAQ/).
