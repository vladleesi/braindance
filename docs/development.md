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
- **A locally run API**, for maintainers with access to it, takes `BACKEND_BASE_URL=http://localhost:8081` (any
  port). Plain HTTP is accepted only for `localhost` and `127.0.0.1` in development builds; release, benchmark, and
  Baseline Profile builds, iOS release frameworks, and the Web production bundle fail with it. To try the Web app
  against it, start the API on port 8081 with `CORS_ALLOWED_ORIGINS=http://localhost:8080`, then run
  `./gradlew -PAPI_FIXTURES=false -PBACKEND_BASE_URL=http://localhost:8081 :webApp:wasmJsBrowserDevelopmentRun` and
  open `http://localhost:8080/`. The development server opens game addresses only from the start page; restart it
  after a code change.

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

The rules are in the [brand guide](branding/README.md#in-the-product). Every screen shows the canvas color, which
`NavigationScaffold` paints once, so screens add no background of their own. The bottom bar, rail, desktop
sidebar, and desktop header use the darker chrome color. Build new screens from these shared pieces in
`shared/src/commonMain/kotlin/dev/vladleesi/braindanceapp/ui` before adding new ones:

### Theme colors

Components read colors only through the theme's roles in `ui/style/Colors.kt`, never as fixed values, so a light
theme can come later as one more palette without changes to any component. The roles are `navBar` (chrome),
`background` (canvas), `surface`, `secondary`, `secondaryVariant`, `shimmer`, `foreground` (text and icons),
`secondaryText`, `accent`, `error`, the status tag colors, and `positive`. `BraindanceTheme` provides the palette
(`AppColors`) as `LocalAppColors`, builds the typography and Material's colors from it, and sets `foreground` as the
default content color.

- Read roles in composition. Code that draws or builds a brush outside composition takes the colors as parameters, or
  `remember`s the brush keyed by them. A `Modifier.Node` reads `currentValueOf(LocalAppColors)` while drawing, as
  `focusRing` does. The roles come from a static composition local, so reading them costs no tracking.
- Fixed colors are for two cases only: brand artwork (the logo, its trail, the score rings, the avatar), and what sits
  on photos and video, which stay dark in any theme (`onMedia`, `black`, the overlay scrims).
- Do not add color literals in components. A new color is a new role in `AppColors`, with a value in every palette.
- Still outside the theme, for the light theme to handle: the Web page's loading background and `theme-color`
  (`webApp/src/wasmJsMain/resources/index.html`), and Android's system bar style, which `MainActivity` sets from the
  dark palette before the first frame.

| Piece | Where | Use |
| --- | --- | --- |
| Colors, `CornerShapes`, `Dimens`, typography | `style/` | Tokens: color roles, the three corner shapes, spacing, text styles |
| `pageColumn`, `pageWidth`, `pageGutter` | `components/PageColumn.kt` | Page layout; see [page layout](#page-layout) |
| `SupportingPaneLayout`, `SectionColumns` | `components/` | Desktop layouts that fill the page column. See [desktop layout](#desktop-layout) |
| `DesktopHeader`, `DesktopSidebar` | `components/desktop/` | The desktop layout's header and sidebar. See [header and sidebar](#header-and-sidebar) |
| `TopLevelTopAppBar`, `TopAppBar`, `ProfileButton` | `components/TopAppBar.kt` | A screen's own title, back button, and actions, and the account button |
| `ScrollAwayBarLayout` | `components/ScrollAwayBar.kt` | A screen's own bar that scrolls away with its content in the desktop layout. See [header and sidebar](#header-and-sidebar) |
| `FooterLinks`, `DataCredits`, `FooterLink` | `components/FooterLinks.kt` | Legal links, support, and the data credits, in the sidebar and Profile |
| `PrimaryButton`, `TonalButton`, `OutlineButton` | `components/Buttons.kt` | The main action, secondary actions, and links inside cards |
| `SurfaceCard`, `SurfaceTile` | `components/Surfaces.kt` | Containers: cards and panels, and compact tiles |
| `LinkRow`, `LinkDivider` | `components/LinkRow.kt` | Rows of links on a card, such as Profile's links and a game's stores |
| `Tag`, `ComingSoonLabel`, `ComingSoonCard` | `components/Tag.kt`, `ComingSoon.kt` | Status labels and placeholders for planned features |
| `CardCover`, `CarouselSection` | `components/` | Game covers and horizontal rows of them |
| `MediaGallery`, `MediaViewer`, `MediaGalleryState` | `components/media/` | A game's videos and screenshots: a preview, thumbnails, and the full-screen viewer |
| `ShareButton` | `components/Share.kt` | Sharing a game, giveaway, event, or list; see [sharing](#sharing) |
| `BraindanceWordmark`, `BrandAccentStrip` | `style/icons/`, `components/desktop/` | The logo and the three-color accent |

### Page layout

Every screen lays out its content in the same page column, at most `Dimens.pageMaxWidth` (1920 dp) wide and centered
beside the navigation, so the content's edges stay in place when you switch screens. Its side margin,
`pageGutter()`, is 16 dp on phones and tablets and 32 dp in the desktop layout of wide Web windows. The navigation
stays at the window's edge at every width.

- Pages are mostly grids of covers, so a wider window shows more of them instead of empty space, as in other media
  catalogs. Paged grids fit as many columns as their minimum card width allows. Home's rows stop at a maximum per
  section (`DesktopHomeSections.kt`) that keeps their cards near their usual size. Only windows wider than about
  2200 dp leave spare width, split evenly on both sides of the column.

- The top bar takes the page's width as `maxContentWidth`, so its back button, title, and actions line up with the
  content. Backgrounds and full-bleed artwork, such as the game details hero, still span the whole width.
- Inside the column, running text, the search field, and placeholder cards keep their readable width
  (`Dimens.readableTextWidth`, 720 dp), aligned to its start.
- The desktop layout fills the column. See [desktop layout](#desktop-layout).
- Scrolling containers span the whole width so the wheel also works over the margins: apply `pageColumn` after
  `verticalScroll`, inset lazy grids and lists with `pageSidePadding`, and let carousels take `pageWidth` and inset
  their cards by the gutter.

### Desktop layout

The Web app switches to a desktop layout in windows 768 dp and wider (`desktopLayout()`). Android and iOS keep their
phone and tablet layouts at every size. The desktop layout has one rule: **every screen fills the page column**.
Spare width becomes more content, larger media, or a side column, never a narrow column beside empty space. Screens
fill it in one of four ways, after Material's [canonical layouts][canonical-layouts]:

| Pattern | For | Reuse | Screens |
| --- | --- | --- | --- |
| Feed | Grids of equal cards | `CardGrid`, `PagedGrid`, `DesktopCardRow` | Home's rows, search results, See all, events, an event's games |
| Supporting pane | One item's main content and its facts | `SupportingPaneLayout` | Game and giveaway details. Home's context column follows the same pattern |
| Section columns | Independent lists or cards | `SectionColumns` | Profile, Discover's previews |
| Full-width blocks | Heroes, banners, a calendar, a feed's placeholder | `pageColumn`, `ComingSoonCard(maxWidth = Dp.Unspecified)` | Home's hero, an event's header, Library, Releases, News |

- The navigation stays at the window's edge, and the page column is centered beside it. At any window size, every
  screen's content starts and ends in the same place.
- Text keeps a readable line length: paragraphs, the search field, and forms stay at most
  `Dimens.readableTextWidth` (720 dp), at the start of their column, while the cards and panels around them fill.
  Material's search bar has the same limit: it is 360 to 720 dp wide.
- Grids add columns from a minimum card width instead of growing their cards, and Home's rows cap the columns per
  section to keep cards near their usual size.
- Media grow with their column and fill it. The game gallery's preview is at most 1440 dp wide, wider than its
  column gets, and 75% of the window's height, so the whole gallery still fits on a landscape phone.
- A supporting pane is `SupportingPaneWidth` (320 dp) wide at the column's end, top-aligned with the main content.
  Where the main content would get narrower than its minimum, the pane moves below it at the readable width. Home's
  context column is 280 dp, so a 1440 dp window still fits five game cards beside it.
- Section columns are at least `SectionColumnMinWidth` (360 dp) wide, top-aligned, and in reading order, so keyboard
  and screen reader order match what you see.
- A placeholder takes the space of what it stands for: a feed's preview spans the column, and a widget's preview
  fills its side column.
- Reuse these components and `pageColumn` before writing a new layout. Phones and tablets keep their layouts unless
  a change means to change them. Check a new desktop layout at 1280, 1920, and 2560 dp.

The breakpoints, with the [Material window size classes][window-size-classes] they fall in:

| Width | Material class | What changes |
| --- | --- | --- |
| Below 600 dp | Compact | Phone layouts and the bottom navigation bar |
| 600 dp | Medium | Tablet layouts and the navigation rail (`isLargeWindow()`) |
| 768 dp | Medium | Web: the desktop layout and its header, with the sidebar collapsed (`DesktopLayout.Compact`) |
| 1100 dp | Expanded | Web: the expanded sidebar with the footer links, which people can collapse (`DesktopLayout.Sidebar`) |
| 1440 dp | Large | Web: Home's context column (`DesktopLayout.Wide`) |
| 1600 dp | Extra-large | The game gallery's thumbnails move into a rail beside the preview (`isExtraLargeWindow()`) |
| About 2200 dp | Extra-large | The page column reaches 1920 dp, and wider windows add equal margins around it |

The desktop tiers follow the cards' minimum widths rather than Material's class boundaries exactly. The gallery's rail
waits for an extra-large window because the rail and a game's supporting pane together take about 560 dp. In a
narrower window the preview beside them would be too small, so the thumbnails stay in a strip under it.

#### Header and sidebar

The desktop layout has a header above every screen and the navigation beside them. Both are chrome, in the darker
`navBar` color, and neither scrolls with the page. The header is 56 dp tall with a 40 dp search field, compact for a
bar on every screen, and the sidebar's brand row has the same height, so the logo and the field share a line.

- The header (`DesktopHeader`) holds only what applies on every screen: the game search field at the start of the
  page column, then notifications and the account button (`ProfileButton`) at its end. A screen's own title, back
  button, and actions stay in its top bar below the header, so the header looks the same everywhere.
- A screen's own bar is 56 dp in the desktop layout, like the header, and 82 dp on phones and tablets for their
  large titles. On screens of titles and lists it scrolls away with the content, so reading takes the whole height
  under the header: use `ScrollAwayBarLayout` for a bar above scrolling content, or put the bar inside the scrolling
  column, as Home, Library, and Profile do. Content never scrolls under a title that stays.
- Game, giveaway, and event pages keep their bar over the hero on screen, 82 dp tall on every layout
  (`Dimens.heroTopBarHeight`), as are their loading and error states, so Back never moves. It starts clear over the
  artwork and fills, with the page's name, as the page scrolls under it. Phones and tablets keep every screen's bar
  on screen, as Material's top app bar does.
- Heroes with artwork (game and giveaway pages) leave a band of it between the bar and their content: 16 dp on phones
  and 32 dp on large screens. On large screens the game backdrop is the smallest IGDB size that fills the window's pixel
  width, up to 1080p (`BackdropImage.wideUrlFor`), chosen by the window rather than the page, so the file stays the same
  while the sidebar changes the page's width. Phones keep the small size, where the backdrop sits dimmed and data
  matters more. The artwork moves with a parallax as the page scrolls (`HeroBackdrop`), shifted while drawing, so
  scrolling never recomposes. Decorative motion like this and the sidebar's animation stays still when the system asks
  for less motion (`prefersReducedMotion()`).
- While a search shows, Discover's title names it ("Results for …") and scrolls away with the results.
- The header's field and Discover share one search (`SearchQueryState`, provided by `NavigationScaffold` as
  `LocalSearchQuery`). Typing on any screen opens Discover's results. The field shows the search only while Discover's
  own screen is open. In the desktop layout Discover shows no field of its own, and `SearchAction` draws nothing.
- `Navigator.openSearch` and the shortcuts focus the header's field: "/" when no field has the focus, and Ctrl+K or
  ⌘K anywhere (`SearchShortcuts`, Web only). The empty field shows the "/" hint. Escape clears the search and leaves
  the field, and Enter searches at once.
- The expanded sidebar (232 dp) holds the wordmark, the destinations, and at its bottom `FooterLinks`: the privacy
  policy, licenses, support, and the data credits, in 24 dp rows of the smallest label size. Profile is the header's
  account button.
- One button in the sidebar's bottom corner, in the same place in both states, collapses the sidebar to an 80 dp
  rail like Material's navigation rail and expands it again. The rail holds the small mark (`BraindanceMark`, the
  brand kit's `mark-small-on-ink`) and each destination as an icon in a pill over its label. The width animates for
  250 ms with Material's emphasized easing while the two layouts cross-fade, each at its own width, so nothing
  reflows. The choice is kept in the browser's `localStorage` (`LayoutPreferences`), and the privacy policy mentions
  it. Windows narrower than 1100 dp show the collapsed sidebar without the button. The collapsed sidebar has no footer
  links. Profile lists the same pages, as on phones and tablets.
- A layout whose width can change while it shows, through the sidebar or a resized window, must follow at once.
  `animateContentSize` animates the width too, so fix the width before it (`fillMaxWidth()`) when only the height
  should ease.
- The app has no footer bar. A bar that stays on screen takes height from every page for links people rarely open.
  Add a status bar only for live state worth watching, such as being offline.

[canonical-layouts]: https://developer.android.com/develop/adaptive-apps/guides/canonical-layouts
[window-size-classes]: https://developer.android.com/develop/adaptive-apps/guides/use-window-size-classes

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
| `/games/{id}` (also the earlier `/game/{id}`), `/giveaway/{id}`, `/events/{id}` | Game, giveaway, or event details |
| `/popular`, `/anticipated`, `/giveaways`, `/events` | The full list behind a Home section's See all |
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
`shared/.../navigation/ShareTarget.kt` names what can be shared (a game, a giveaway, an event, or a Home section's
full list) and builds each link from the paths above. It shares only a link that opens the same screen again, so an ID no
screen accepts gets no share button. A giveaway links to its Braindance page, which shows its details and the claim
link, not to GamerPower or the store.

Use `ShareButton` (`ui/components/Share.kt`) in the top bar of a full list, giveaway, event, or game. It opens the
system share sheet through ShareKit:
Android's chooser, iOS's activity sheet, or the Web Share API, which needs HTTPS and a click. Where there is no sheet,
or it cannot open, the button copies the message and shows a check mark for two seconds; cancelling the sheet is not
an error. `showsShareActions()` shows share buttons on every touch-screen device, including Web tablets and
foldables in the desktop layout, and in the standard layout of narrow windows. It hides them only in the Web desktop
layout on devices without a touch screen, whose address bar already holds the link; Profile's share row follows the
same rule. UI tests provide their own `ShareSheet` through `LocalShareSheet`, because the real sheet needs a running
app.

### Search and link previews

The Web app draws into a canvas, so search engines and link previews read only each page's HTML head. Every address
therefore gets its own title, description, canonical link, and preview image:

| Pages | Served by | Indexed |
| --- | --- | --- |
| `/`, `/popular`, `/anticipated`, `/giveaways`, `/events` | Files written by the build (`popular.html` is served at `/popular`) | Yes, and listed in `sitemap.xml` |
| `/discover`, `/releases`, `/news`, `/library`, `/profile` | Files written by the build | No: placeholders or personal pages |
| `/games/{id}`, `/game/{id}`, `/giveaway/{id}`, `/events/{id}` | `_worker.js`, a Cloudflare Pages Function, from the API | No: previews for shared links |
| Any other address, or an unknown game or giveaway ID | `404.html` or `_worker.js`, with status 404 | No |

- **Fixed pages:** `webPages` in `webApp/build.gradle.kts` holds their titles and descriptions. The build copies the
  final `index.html` for each, replacing the tags between its `page-meta` markers, and writes `sitemap.xml` and
  `404.html`. `WEB_PUBLIC_URL` (default `https://app.braindancegg.com`) sets the canonical and image addresses.
- **Game, giveaway, and event pages:** `_routes.json` runs `_worker.js` only on their paths; every other request stays a free
  static file. It serves the same app page with the item's title, description, and image, read from
  the API through its edge cache and kept per data center for a day (games) or an hour (events, giveaways, and unknown IDs).
  Events outside the current feed keep a generic preview so older links can still open their games.
  An edition's or update's page keeps its own preview but names its game's address as canonical, since the app shows
  it as a version of that game, selected among the game's versions. It never caches the page itself, which names the current deployment's
  bundle. When the API fails, the page keeps a
  generic preview. If the Function does not run, for example after the free daily allowance (100,000 requests) is
  used up, `_redirects` still serves the app on those paths.
- **Tab titles while browsing:** the HTML title covers the address the page was opened at. As the app moves between
  screens, each screen names itself with `PageTitle(route, title)` from its own composable, next to the title it shows
  (never from its route's `Content`), such as "Discover | Braindance" or a game's name once it has loaded, so the tab
  and its history entries say where they lead, and screen readers announce it. A screen that has not named itself shows
  "Braindance".
- **Abuse:** the zone's per-IP rate limit covers game and giveaway preview paths with the API's per-game routes and search. Event previews use the shared events feed.
- **Checks:** `node --test webApp/src/edgeTest/*.test.mjs` tests the worker. To try the whole site locally, serve
  the production distribution with `npx wrangler@4.168.0 pages dev <dist>`; the app's own API requests then fail
  CORS, but the HTML, statuses, and previews are the same as on Cloudflare.

A new fixed path needs an entry in `webPages`; a new ID path needs the worker's routes, `_routes.json`, and
`_redirects`. Otherwise the address answers 404.

## Web app runtime

The Web app draws every screen into one canvas with Compose. `WebAppHost` (`shared/src/wasmJsMain`), which
`webApp`'s `main` wraps around the app, supplies the browser behavior Compose's Web host lacks:

- **Startup:** the page's loading screen (`#boot` in `index.html`) sits outside the app's container and stays until
  every string and the typography's fonts (`AppFonts`) are in Compose's resource caches; it is removed in the same
  frame as the first screen, so no page shows empty text or a fallback font. `WebResourceReader` reads the
  resources at their fingerprinted addresses (served by the page's preloads and the browser cache) and keeps the
  strings file in memory. Compose's default reader keeps a Cache Storage copy that it clears on every load and then
  reads each string through it in turn; the app deletes that copy. If a resource fails to load, the app starts
  without waiting.
- **Scrolling:** Compose's Web host drops lazy layouts' prefetch requests, so items were composed only once they
  scrolled into view, inside the wheel event. `WebPrefetchScheduler` runs those requests in the time each animation
  frame leaves, which also lets cache windows (`rememberCachedLazyListState`, `rememberCachedLazyGridState`) prepare
  items ahead. It implements Compose's `@InternalComposeUiApi` `PlatformPrefetchScheduler`: after a Compose upgrade,
  check that it still compiles and that scrolling a long grid keeps wheel handling short, and remove it once Compose
  Web schedules prefetching itself.
- **Links:** apply `Modifier.routeLink(route)` to anything that opens a screen (cards use it through
  `cardClickable`), including buttons such as "Open Releases" and chips such as a game's versions. On the Web, a
  middle click, Ctrl-click (⌘-click on Apple systems), or Shift-click opens the route's address with `window.open`,
  so the browser chooses the tab or window as for a real link. Elsewhere it does nothing. For a link inside a text,
  such as a `LinkAnnotation` span, apply `Modifier.routeTextLink(route) { position -> ... }` to the text, with a
  check of whether the position is over the link (`TextLayoutResult.hits` in `GameRelations.kt`).
- **Software keyboard:** Safari on iPhone and iPad opens the keyboard only for focus given during a tap, not for
  focus a screen gives its field after it opens. A tap that leads to a text field, such as `Navigator.openSearch`,
  calls `prepareSoftwareKeyboard()` first: on touch devices it focuses an invisible input at once, and the keyboard
  stays open when the field takes focus.
- **External links:** `WebUriHandler` opens pages in a new tab with `noopener` and `mailto:` addresses from the
  current tab. Give elements that open them `Modifier.externalLink(open)` with the same action as their click, so a
  middle click opens them too.
- **Videos and the media viewer:** videos play only in the game gallery's preview (`MediaGallery`); the full-screen
  viewer (`MediaViewer`) shows screenshots only and shares its `MediaGalleryState`. It is drawn above the app through
  `FullWindowOverlay`, not as a route: the browser's Back closes it through its own history entry
  (`BrowserBackEntry`), and it handles Android's Back key itself, because Compose would spend the key on clearing focus.
  - YouTube's privacy-enhanced player loads only after the user presses play: an iframe over the canvas on the Web, and
    the player page from `YouTubeEmbed.kt` in a WebView or WKWebView on Android and iOS (`YouTubePlayer.*.kt`). The
    page reports a failure by navigating to `PLAYER_ERROR_URL`, which the web view intercepts.
  - On Android and iOS, the preview keeps one web view from the first play until it leaves: another video loads into
    it, and stopping unloads, hides, and shrinks it. Never remove a platform view on its own while its parent layout
    stays; Compose on Android can then crash with "LayoutNode should be attached to an owner". A WebView in Compose
    needs `MATCH_PARENT` layout parameters and is destroyed only after Compose has detached it.
  - `MainActivity` handles rotation and resizing itself (`configChanges`), so a playing video survives them. Read the
    window size through `windowSize()` (`system/WindowSize.kt`): on Android, `LocalWindowInfo.containerSize` trails
    one rotation behind.
  - On the Web, the iframe is clipped below the bar the page scrolls under (`coveredTop`). It keeps the mouse wheel to
    itself, so a transparent shield sends wheel deltas over the picture to the details page while the video starts,
    plays, or is paused. Clicks over the picture pause or resume playback. YouTube's controls and paused header stay
    accessible, and its end screen takes over when playback finishes. Native scroll chaining over those areas
    moves the details page through the player's scrollable wrapper. The Media heading shows startup progress.
    The app reads YouTube's message channel (`enablejsapi`, `EmbedEvent`) only from the frame and YouTube's origin.
    It retries once with sound muted if playback is blocked or stalls before buffering. Without a response, the
    shield steps aside after ten seconds. Keep `frame-src` and
    `https://i.ytimg.com` in `index.html`'s content security policy in step with it.
- **Autoscrolling:** on Windows, a middle press that no link takes, over a page that can scroll, starts the
  browser-style autoscroll (`PageAutoscrollHost`). A marker appears at the press point, and the page scrolls faster
  the farther the pointer moves from it. After a click, autoscrolling continues until the next click, Esc, a wheel
  scroll, or the window losing focus. That click does nothing else. A press that moves past the dead zone scrolls
  only until it is released. Pages take part through `PageScrollbar(state)`, so a new scrolling screen needs only
  that call. Autoscrolling moves the page's own vertical scroll, not horizontal rows inside it. Only Windows gets it:
  macOS has no such convention, and Linux pastes on a middle click.
- **List scrollbars:** a list of fixed size inside a page, such as the game gallery's thumbnails, takes
  `ListScrollbar(state)` after it in a row, or `ListScrollbar(state, orientation = Orientation.Horizontal)` under a
  horizontal one in a column. For mouse users it stays visible while the list can scroll, and it keeps its space when
  the list fits, so the list never changes size.

Some browser behavior is out of reach while Compose draws into a canvas: a right click shows no browser menu, because
the browser offers its link menu only over a real link element, and Compose has no link role for elements to place
one for (Compose blocks the menu on its canvas); for the same reason, hovering a link shows no address in the
browser's status area; text is selectable only inside Compose selection containers; and a mouse wheel
moves lists by the distance the browser reports in its wheel events, which Compose converts without a setting for
the app to tune, so one notch can move a list less than the same browser scrolls a page.

## Optional independent hosting

Hosting your own Web build needs your own API that implements [openapi.yaml](openapi.yaml); this repository does
not include one. Set its HTTPS URL as `BACKEND_BASE_URL`, run `./gradlew :webApp:wasmJsBrowserDistribution`, and
serve `webApp/build/dist/wasmJs/productionExecutable` over HTTPS with compression and the `application/wasm` MIME
type. The API's CORS allowlist must include the site's exact origin.

Serve the fixed pages' files at their paths without the extension (`popular.html` at `/popular`), `index.html` for
`/games/*`, `/game/*`, `/events/*`, and `/giveaway/*`, and `404.html` with status 404 for anything else; `_worker.js`,
`_routes.json`, and `_redirects` do this on Cloudflare Pages only. Set `WEB_PUBLIC_URL` to the site's own origin, so
canonical links and the sitemap do not point to the Braindance Web app. Cache
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
