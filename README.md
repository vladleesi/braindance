# Braindance

> **Important:**
> It is an ongoing project and is currently under development. However, I'm actively working on adding new features, enhancing existing functionality, and addressing any issues or bugs.

![Braindance vibe](https://github.com/vladleesi/braindance/assets/30999008/cdb06536-ecbf-43ae-9336-3833a89a3718)

## Overview
Braindance is a versatile game-tracking application that allows users to search, explore, and keep up with their favorite games. Powered by the extensive [IGDB database](https://api-docs.igdb.com/#getting-started), users can effortlessly find detailed information about any game, add them to their favorites, stay updated with release dates using the integrated calendar, and read the latest game news.

## Platforms
Braindance is a Kotlin full-stack project: Android, iOS, and browser clients share UI and logic through
[Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) and Compose Multiplatform.
The backend runs Kotlin/JVM with Ktor.

## Releases

The current frontend version for Android, iOS, and Web is `0.3.2`, with one shared
[Braindance App release](https://github.com/vladleesi/braindance/releases/tag/app-v0.3.2).
All three platforms use the same `MAJOR.MINOR.PATCH` version from `version.xcconfig`; Android and iOS also use its
shared build number. Existing `mobile-v*` releases remain available as historical releases.

The backend is versioned independently from the frontend. Its current version is `0.5.1`.
GitHub Actions publishes one frontend source release tagged `app-vMAJOR.MINOR.PATCH` only after Android,
iOS, Web, and backend checks succeed. Validated `develop` pushes are promoted to `master`; direct `master` pushes
run the same checks before release publication. Frontend releases publish only when `MARKETING_VERSION` differs
from the caller's pre-push revision (`develop` before promotion or `master` on direct pushes). An unreadable previous
version allows the first release. The release targets the exact validated commit and skips publication if `master`
has advanced or that version's release already exists. Retry a failed publication by rerunning the validation
workflow's failed jobs; pull requests and manual validation runs do not publish releases.

Release notes come from the matching `## MAJOR.MINOR.PATCH` entry in [CHANGELOG.md](CHANGELOG.md). Maintain concise
change bullets only for platforms with user-visible changes. Group identical notes under a combined heading, such
as `### Android and iOS` or `### Android, iOS, and Web`; separate sections only when notes differ. Exclude backend-only
changes. Update the entry with the frontend version change; the release workflow fails if that version's entry or
platform notes are missing.

## Project layout

| Directory | Purpose |
| --- | --- |
| `androidApp` | Android application host |
| `shared` | Shared Compose UI, app logic, and networking for Android, iOS, and Wasm |
| `iosApp` | Xcode and SwiftUI host |
| `webApp` | Compose Multiplatform browser host |
| `backend` | Kotlin/JVM Ktor API that proxies IGDB and GamerPower |

The clients call the backend for both IGDB and GamerPower data. Its URL is included in client builds and is not a
secret. Keep Twitch credentials in the backend runtime secret store. See [backend setup](backend/README.md).
After all validation checks succeed, backend-impacting changes pushed or promoted to `master` publish a GHCR
image and deploy its digest to the existing Cloud Run service. Publication builds the exact validated commit;
frontend and documentation changes outside `backend` do not trigger it. Backend-impacting files include `backend/**`,
root Gradle build/settings/properties files, wrapper scripts/files, the version catalog, and backend lint configuration.
Manual runs of **Publish and Deploy Backend** on `master` force deployment of the current `master` backend.
The backend can run on other container hosts or a Java 21 server; see
[hosting elsewhere or from a fork](backend/README.md#hosting-elsewhere-or-from-a-fork) for configuration and workflow changes.
See the [Cloud Run deployment guide](backend/cloud-run.md) for runtime configuration and cost controls.

## API credentials

IGDB does not issue a separate API key: it uses a Twitch developer application's client ID and client secret.
GamerPower requires no API key. See the
[backend credential guide](backend/README.md#configuration) for configuration and secret storage.

## Requirements

- JDK 21 for the backend and client builds; Android SDK API 37 for Android.
- macOS with Xcode and an iOS Simulator for iOS builds.
- A container host to deploy the backend.

Use the checked-in Gradle wrapper; a separate Gradle installation is unnecessary.

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

   The development server prints the local URL. The browser client requires a browser with WasmGC support.

## Web publication

Pushes to `master` build the production browser app and replace the `web-app` branch with its static files. The
develop validation workflow also publishes after it promotes a validated commit to `master`. Configure these under
**Repository Settings → Secrets and variables → Actions**:

- Add the backend URL as the repository **secret** `BACKEND_BASE_URL`.
- To use a custom domain, add its hostname as the repository **variable** `WEB_CUSTOM_DOMAIN`, without `https://`
  or a trailing slash. **Do not add `WEB_CUSTOM_DOMAIN` as a secret:** the publication workflow reads the `vars`
  context and would otherwise omit the generated `CNAME` file on every deployment.

Select the `web-app` branch and `/ (root)` in GitHub Pages settings, then configure the matching DNS record and Pages
custom-domain setting. Include the deployed website origin in the backend's `CORS_ALLOWED_ORIGINS` variable. The
workflow preserves configured custom domains in the generated branch but does not change Pages, DNS, or backend
settings.

For another static host, build with `./gradlew :webApp:wasmJsBrowserDistribution` and deploy the contents of
`webApp/build/dist/wasmJs/productionExecutable`. Configure `BACKEND_BASE_URL` before building and include the
website origin in the backend's `CORS_ALLOWED_ORIGINS`. Serve over HTTPS with `application/wasm` for Wasm files.
Replace or disable `.github/workflows/publish-web-app.yml` if GitHub Pages publication is unnecessary.

The project can be built without a backend URL. Configure a reachable backend before running backend-powered flows,
and add both Twitch secrets for IGDB requests. A fresh clone does not include anyone else's URL or credentials.

## License

Braindance's first-party source, build scripts, configuration, and documentation use [MPL-2.0](LICENSE).
Commercial use is permitted. Keep distributed covered files and modifications under MPL, preserve required notices,
and provide binary recipients with the matching source. See the [Mozilla FAQ](https://www.mozilla.org/en-US/MPL/2.0/FAQ/).

For first-party files without inline comments, this notice applies:

> This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
> If a copy of the MPL was not distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.

Third-party components, external data, and artwork are outside this grant; see
[third-party terms and asset exclusions](THIRD_PARTY_NOTICES.md). Previously published GPL versions retain their rights.

### Contributions and future development

Contribute work you have permission to license under MPL-2.0. Add `SPDX-License-Identifier: MPL-2.0` to new
first-party source, preserve upstream notices, and run `python3 .github/scripts/check-licenses.py` after licensing changes.

Commercial forks and paid services are permitted. Future subscriptions should enforce entitlements on a trusted
backend. Verify API commercial permissions and store payment rules separately; MPL does not guarantee store approval.

Report issues through the
[GitHub issue tracker](https://github.com/vladleesi/braindance/issues).
