# Third-party notices

The root MPL license does not relicense the components and assets below. Preserve their notices when copying,
converting, bundling, or distributing them. Include the full applicable license texts with distributed copies.

## Agent guidelines

The imported rules in `AGENTS.md` and `docs/project/agent-guidance/*.md` come from
[vladleesi/agent-guidelines v1.8.0](https://github.com/vladleesi/agent-guidelines/tree/v1.8.0),
commit `3951202798c0cb5604cc0f9c0c00412a894f3154`.
Selected modules: general, agent-workflow, testing, security, git, ci, release, deployment, kotlin, compose,
python, and frontend. Placement and headings were adapted for local task loading; imported bullets
remain verbatim. The copyright and permission notice is retained in the
[MIT license](LICENSES/Agent-guidelines-MIT.txt), separate from
first-party MPL-2.0 instructions. This is a one-time import; fetching, comparing with, or updating the collection
requires an explicit user request.

## Bundled code and fonts

| Material | Location | License and attribution |
| --- | --- | --- |
| Poppins 4.004, three font weights | `shared/src/commonMain/composeResources/font/poppins_*.ttf` | Copyright 2020 The Poppins Project Authors. [SIL OFL 1.1](LICENSES/Poppins-OFL-1.1.txt), retained for the fonts and their modifications. [Upstream](https://github.com/google/fonts/tree/main/ofl/poppins). |
| Lucide/Feather icon adaptations | `shared/src/commonMain/kotlin/dev/vladleesi/braindanceapp/ui/style/icons/Icon*.kt`, except `IconOpenInNew.kt`, `IconShare.kt`, and `IconStar.kt` | [Lucide ISC](LICENSES/Lucide-ISC.txt) and [Feather MIT](LICENSES/Feather-MIT.txt); both sets of notices are retained through SPDX `ISC AND MIT`. [Upstream artwork](https://github.com/lucide-icons/lucide/tree/0.468.0/icons). |
| Threads and X logos from Simple Icons 14.0.0 | Same directory, `IconThreadsLogo.kt` and `IconXLogo.kt` | [CC0-1.0](LICENSES/CC0-1.0.txt) for the artwork. The logos remain trademarks of Meta Platforms, Inc. and X Corp.; the license does not grant trademark rights, and the app uses them only to link to the project's own profiles. [Upstream artwork](https://github.com/simple-icons/simple-icons/tree/14.0.0/icons). |
| Google Material open-in-new icon adaptation | Same directory, `IconOpenInNew.kt` | [Apache-2.0](LICENSES/Apache-2.0.txt). [Upstream artwork](https://github.com/google/material-design-icons/blob/master/symbols/web/open_in_new/materialsymbolsoutlined/open_in_new_24px.svg). |
| Gradle wrapper | `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` | [Apache-2.0](LICENSES/Apache-2.0.txt). Existing script copyright and license headers are preserved. |

Dependencies and SDKs retain their own terms. Application and container distributors must include the required
copyright, license, and NOTICE material for the exact components shipped, including transitive dependencies,
native libraries, and container base software. The files above do not constitute a complete binary-release notice
bundle. Generated license comments alone may omit required full license texts.

Adaptive navigation uses JetBrains' Compose Multiplatform Material 3 and navigation suite
(`org.jetbrains.compose.material3:material3` and `material3-adaptive-navigation-suite`, `1.12.0-alpha03`),
with Material 3 Adaptive, WindowManager, Graphics Shapes, and annotation transitive components, under
[Apache-2.0](LICENSES/Apache-2.0.txt).
See the [Compose 1.12.2 component versions](https://kotlinlang.org/docs/multiplatform/whats-new-compose-112.html#dependencies).
Retain the copyright, license, and any NOTICE material in the published artifacts when distributing them.

Profile sharing uses [ShareKit 0.2.0](https://github.com/vladleesi/kmp-sharekit/tree/v0.2.0)
(`dev.vladleesi:kmp-sharekit`) under [Apache-2.0](LICENSES/Apache-2.0.txt). Its only declared runtime dependency is
the Kotlin standard library, already used by the application. Preserve any notices in the published artifacts.

## Artwork and branding

The following assets are excluded from the root MPL grant. Their provenance and redistribution permissions remain
unverified; inclusion in Git does not establish those rights. Record the original source, permission/license,
modifications, and applicable brand guidelines before redistributing them.

- `shared/src/commonMain/composeResources/drawable/ic_platform_logo_*.xml` (Android, iOS, Linux, macOS,
  PlayStation, Switch, Windows, Xbox).
- `shared/src/commonMain/composeResources/drawable/ic_store_logo_*.xml` (Epic Games, GOG, Steam).
- The Braindance kit in `docs/project/branding/`, supplied by the repository owner as `brand-kit (1).zip`, including
  outlined Outfit Regular/SemiBold lettering, logos, marks, banners, and icons. No separate asset license or font
  files were supplied. See the [brand usage guide](docs/project/branding/README.md).
- Copies of that kit in `androidApp/src/main/ic_launcher-playstore.png`, launcher PNGs and adaptive XML in
  `androidApp/src/main/res/mipmap-*`, vectors in `androidApp/src/main/res/drawable/ic_launcher_*.xml`, and
  `androidApp/src/main/res/values/ic_launcher_background.xml`.
- Copies and resized exports in `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/*.png`,
  `iosApp/iosApp/appstore.png`, and `iosApp/iosApp/playstore.png`.
- Copies in `webApp/src/wasmJsMain/resources/favicon*` and `apple-touch-icon-180.png` in the same directory.
- The root README banner, copied from the kit and stored in `docs/project/branding/banner/readme-banner.png`.
- Copies of the kit on the landing site: `docs/favicon*`, `docs/apple-touch-icon.png`, `docs/mark.svg`,
  `docs/lockup-dark.svg`, and `docs/og-image.png`.
- `docs/img/wall-*.webp`: the landing site's hero wall, a collage of game covers from IGDB, resized and re-encoded.
  Each cover remains the property of its game's rights holders; see External data below.

A copyright license for an icon does not grant general rights to its trademark. Platform software licenses also
do not determine the licenses of platform logos or mascot artwork.

## External data

Game covers, screenshots, giveaway images, descriptions, metadata, and brands supplied by IGDB/Twitch or GamerPower
remain subject to their rightsholders' and service terms, not MPL. API access does not establish ownership of that
content. Follow [IGDB's commercial and attribution requirements](https://api-docs.igdb.com/#business-related-faq)
and [GamerPower's API terms](https://www.gamerpower.com/api-read), including attribution and the restriction on
selling its data without permission.
