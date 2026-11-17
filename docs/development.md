# Developing Braindance

This guide covers building, running, and validating the Android, iOS, and Web clients. Run commands from the
repository root and install only the tooling you need. For feedback and pull requests, see
[CONTRIBUTING.md](../CONTRIBUTING.md).

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

Crash reporting and signed Android staging builds for testers need local Firebase configuration and signing
files; builds work without them. See [Firebase](firebase.md).

## Run a client

| Platform | Start |
| --- | --- |
| Android | Run `androidApp` in Android Studio or `./gradlew :androidApp:assembleDebug`. |
| iOS | Open `iosApp/iosApp.xcodeproj` in Xcode and run `iosApp` on an iOS Simulator. |
| Web (experimental) | Run `./gradlew :webApp:wasmJsBrowserDevelopmentRun` and open the printed URL. |

Android APKs are in `androidApp/build/outputs/apk/debug/`. Xcode invokes
`:shared:embedAndSignAppleFrameworkForXcode`; configure your own development team and bundle identifier for devices
or distribution.

## Code style

- Follow `.editorconfig`: four-space indentation, 120-character lines, and trailing commas. `./gradlew ktlintFormat
  detekt` applies and checks most rules.
- Use explicit imports and short names instead of fully qualified ones; use import aliases for name conflicts.
- Declare dependency and plugin versions only in `gradle/libs.versions.toml` and use its aliases in build files.
- Pass business dependencies through constructors. In suspending code, rethrow `CancellationException`.
- In Compose, use `Dimens` for spacing, `CornerShapes` for corners, and `PrimaryButton` or `TonalButton` for
  buttons, following the [product design rules](branding/README.md#in-the-product). Give every new UI component a
  `@Preview` in the same file: an `internal fun <Name>Preview()` in `BraindanceTheme`, with sample data and no-op
  callbacks. Pass navigation and data in as parameters so it previews without a ViewModel.
- Start new source files with `SPDX-License-Identifier: MPL-2.0`; see [licensing](../CONTRIBUTING.md#licensing).

## Design system

The rules are in the [brand guide](branding/README.md#in-the-product). Build new screens from these shared pieces in
`shared/src/commonMain/kotlin/dev/vladleesi/braindanceapp/ui` before adding new ones:

| Piece | Where | Use |
| --- | --- | --- |
| Colors, `CornerShapes`, `Dimens`, typography | `style/` | Tokens: color roles, the three corner shapes, spacing, text styles |
| `PrimaryButton`, `TonalButton`, `OutlineButton` | `components/Buttons.kt` | The main action, secondary actions, and links inside cards |
| `SurfaceCard`, `SurfaceTile` | `components/Surfaces.kt` | Containers: cards and panels, and compact tiles |
| `Tag`, `ComingSoonLabel`, `ComingSoonCard` | `components/Tag.kt`, `ComingSoon.kt` | Status labels and placeholders for planned features |
| `CardCover`, `CarouselSection` | `components/` | Game covers and horizontal rows of them |
| `ShareButton` | `components/Share.kt` | Sharing a game, giveaway, or list; see [sharing](#sharing) |
| `BraindanceWordmark`, `BrandAccentStrip` | `style/icons/`, `components/desktop/` | The logo and the three-color accent |

## Validation

Run checks relevant to the change. The [validation workflow](../.github/workflows/build-test-push.yml) defines
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

## Links

Every screen a link can open has one address on the Web app, `https://app.braindancegg.com`. `webPathToRoute` in
`shared/.../navigation/BrowserHistory.kt` is the only parser, so the browser, Android, and iOS open the same screen
for the same link:

| Path | Screen |
| --- | --- |
| `/`, `/discover`, `/releases`, `/news`, `/library` | A tab, at its root |
| `/games/{id}` (also the earlier `/game/{id}`), `/giveaway/{id}` | Game or giveaway details |
| `/popular`, `/anticipated`, `/giveaways` | The full list behind a Home section's See all |
| `/profile` | Profile |

A link opens its screen on top of the current tab, and a tab's link switches to that tab, so Back returns to where
the user was; at launch, the screen opens on top of Home. Mobile apps accept only `https` links to that exact host
(`appLinkRoute` in `AppLinks.kt`) and only turn them into routes; an unknown or invalid path opens nothing new.
Without the app, or before the platform has verified the domain, links open in the browser.

- **Android App Links:** the intent filter in `androidApp/src/main/AndroidManifest.xml` lists the paths above; keep
  it in step with the parser. Android verifies the domain against
  `webApp/src/wasmJsMain/resources/.well-known/assetlinks.json`, which names each package and the SHA-256 fingerprint
  of its signing certificate (`apksigner verify --print-certs <apk>`). It covers the staging build; add the store
  build's package and its signing (or Play App Signing) certificate when it ships. Debug builds are not verified: test
  them with `adb shell am start -a android.intent.action.VIEW -d "https://app.braindancegg.com/games/1942"
  dev.vladleesi.braindanceapp`.
- **iOS Universal Links:** `ContentView.swift` forwards links to the shared code, but iOS hands them to the app only
  once a paid Apple Developer team signs it with the Associated Domains entitlement
  (`applinks:app.braindancegg.com`, set through `CODE_SIGN_ENTITLEMENTS`). The Web app must then also serve
  `.well-known/apple-app-site-association` with the team's app ID and `Content-Type: application/json`, through a
  `_headers` rule.

### Sharing

Content is shared as a short message with its link on the Web app. `ShareTarget` in
`shared/.../navigation/ShareTarget.kt` names what can be shared (a game, a giveaway, or a Home section's full list)
and builds each link from the paths above. It shares only a link that opens the same screen again, so an ID no
screen accepts gets no share button. A giveaway links to its Braindance page, which shows its details and the claim
link, not to GamerPower or the store.

Share content with `ShareButton` (`ui/components/Share.kt`) in a screen's top bar: each full list, giveaway, and game
has one, while Home's carousels and the cards stay without it. It opens the system share sheet through ShareKit:
Android's chooser, iOS's activity sheet, or the Web Share API, which needs HTTPS and a click. Where there is no sheet,
or it cannot open, the button copies the message and shows a check mark for two seconds; cancelling the sheet is not
an error. `showsShareActions()` shows share buttons on every touch-screen device, including Web tablets and
foldables in the desktop layout, and in the standard layout of narrow windows. It hides them only in the Web desktop
layout on devices without a touch screen, whose address bar already holds the link. UI tests provide their own `ShareSheet` through `LocalShareSheet`, because the real sheet needs a running app.

## Optional independent hosting

Hosting your own Web build needs your own API that implements [openapi.yaml](openapi.yaml); this repository does
not include one. Set its HTTPS URL as `BACKEND_BASE_URL`, run `./gradlew :webApp:wasmJsBrowserDistribution`, and
serve `webApp/build/dist/wasmJs/productionExecutable` over HTTPS with compression and the `application/wasm` MIME
type. The API's CORS allowlist must include the site's exact origin.

Serve `index.html` for any path that is not a file, so deep links such as `/games/42` load the app. Cache
fingerprinted assets (`*.wasm`, `webApp.*`, and `composeResources/`, whose URLs carry a content fingerprint in the
`v` query) immutably, include the query in any CDN cache key, and keep HTML revalidatable. The distribution's
`_headers` file sets these lifetimes and the security headers on Cloudflare Pages; mirror them on other hosts.

Before enabling publication workflows for your own infrastructure, replace or disable their existing deployment
steps; they do not configure a new host automatically.

## Distribution

Ship an exact-revision source link and required third-party notices with apps and browser executables, including
transitive and native components. Keep corresponding source accessible and preserve MPL source rights in executable
EULAs. External data, trademarks, and excluded assets have separate terms: see
[THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md). Verify API commercial and attribution permissions before
monetization and review the final package's notices and source disclosure. See the
[Mozilla FAQ](https://www.mozilla.org/en-US/MPL/2.0/FAQ/).
