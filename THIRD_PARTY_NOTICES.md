# Third-party notices

The root MPL license does not relicense the components and assets below. Preserve their notices when copying,
converting, bundling, or distributing them. Include the full applicable license texts with distributed copies.

## Agent guidelines

The imported rules in `AGENTS.md` and `docs/agent-guidance/*.md` come from
[vladleesi/agent-guidelines v1.3.0](https://github.com/vladleesi/agent-guidelines/tree/v1.3.0),
commit `a8423f9d663d046cefb4f7f239bcf816087b9526`.
Selected modules: general, agent-workflow, testing, security, git, ci, release, deployment, kotlin, compose,
backend, python, and frontend. Placement and headings were adapted for local task loading; imported bullets
remain verbatim. The copyright and permission notice is retained in the
[MIT license](LICENSES/Agent-guidelines-MIT.txt), separate from
first-party MPL-2.0 instructions. This is a one-time import; fetching, comparing with, or updating the collection
requires an explicit user request.

## Bundled code and fonts

| Material | Location | License and attribution |
| --- | --- | --- |
| Poppins 4.004, five font weights | `shared/src/commonMain/composeResources/font/poppins_*.ttf` | Copyright 2020 The Poppins Project Authors. [SIL OFL 1.1](LICENSES/Poppins-OFL-1.1.txt), retained for the fonts and their modifications. [Upstream](https://github.com/google/fonts/tree/main/ofl/poppins). |
| Lucide/Feather icon adaptations | `shared/src/commonMain/kotlin/dev/vladleesi/braindanceapp/ui/style/icons/Icon*.kt`, except `IconOpenInNew.kt` | [Lucide ISC](LICENSES/Lucide-ISC.txt) and [Feather MIT](LICENSES/Feather-MIT.txt); both sets of notices are retained through SPDX `ISC AND MIT`. [Upstream artwork](https://github.com/lucide-icons/lucide/tree/0.468.0/icons). |
| Google Material open-in-new icon adaptation | Same directory, `IconOpenInNew.kt` | [Apache-2.0](LICENSES/Apache-2.0.txt). [Upstream artwork](https://github.com/google/material-design-icons/blob/master/symbols/web/open_in_new/materialsymbolsoutlined/open_in_new_24px.svg). |
| Gradle wrapper | `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` | [Apache-2.0](LICENSES/Apache-2.0.txt). Existing script copyright and license headers are preserved. |

Dependencies and SDKs retain their own terms. Application and container distributors must include the required
copyright, license, and NOTICE material for the exact components shipped, including transitive dependencies,
native libraries, and container base software. The files above do not constitute a complete binary-release notice
bundle. Generated license comments alone may omit required full license texts.

## Artwork and branding

The following assets are excluded from the root MPL grant. Their provenance and redistribution permissions remain
unverified; inclusion in Git does not establish those rights. Record the original source, permission/license,
modifications, and applicable brand guidelines before redistributing them.

- `shared/src/commonMain/composeResources/drawable/ic_platform_logo_*.xml` (Android, iOS, Linux, macOS,
  PlayStation, Switch, Windows, Xbox).
- `shared/src/commonMain/composeResources/drawable/ic_store_logo_*.xml` (Epic Games, GOG, Steam).
- `shared/src/commonMain/composeResources/drawable/ic_launcher_background.xml`.
- `androidApp/src/main/ic_launcher-playstore.png`, `androidApp/src/main/res/drawable/ic_launcher_background.xml`,
  `androidApp/src/main/res/mipmap-*/ic_launcher*.png`, and launcher XML files in `mipmap-anydpi-v26`.
- `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/*.png`, `iosApp/iosApp/appstore.png`, and
  `iosApp/iosApp/playstore.png`.
- `webApp/src/wasmJsMain/resources/favicon.png`.
- The externally hosted “Braindance vibe” image linked in `README.md`.

A copyright license for an icon does not grant general rights to its trademark. Platform software licenses also
do not determine the licenses of platform logos or mascot artwork.

## External data

Game covers, screenshots, giveaway images, descriptions, metadata, and brands supplied by IGDB/Twitch or GamerPower
remain subject to their rightsholders' and service terms, not MPL. API access does not establish ownership of that
content. Follow [IGDB's commercial and attribution requirements](https://api-docs.igdb.com/#business-related-faq)
and [GamerPower's API terms](https://www.gamerpower.com/api-read), including attribution and the restriction on
selling its data without permission.
