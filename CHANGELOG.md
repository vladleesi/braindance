# Frontend changelog

Release notes for Android, iOS, and Web, plus notable website and documentation changes. Each version lists only
what changed.
History begins with the introduction of shared version tracking in `version.xcconfig`.
The Braindance API is versioned and released separately.

## 0.17.0 — 2026-11-14

### Android, iOS, and Web

- Refresh the look with the new Braindance brand: the "bd" app icon, a darker ink background, and the brand's cyan,
  magenta, and yellow.

### iOS

- Add dark and tinted app icons for the iOS 18 Home Screen styles.

### Website and documentation

- Rebrand braindancegg.com with the new logo and colors, and rebuild its game cover wall with rounded, sharper,
  higher-resolution covers and a refreshed set of games.
- Publish the website from its own `landing` folder; project documentation and the brand kit now live in `docs`.

## 0.16.1 — 2026-11-14

### Android, iOS, and Web

- Add Braindance on Bluesky (@braindancegg.com) to the Community links on Profile.

### Website and documentation

- Add "Follow @braindancegg" to the braindancegg.com footer, with links to Braindance on Bluesky, Threads, and X.

## 0.16.0 — 2026-11-13

### Android and iOS

- Report crashes from release builds to Firebase Crashlytics, so they can be found and fixed. Debug builds report
  nothing, and the apps collect no analytics.
- Test builds can be sent to testers through Firebase App Distribution.

### Android

- Add a Braindance Staging build for testers, which installs next to other Braindance builds.

### Website and documentation

- Add a Firebase guide covering crash reporting, Android staging builds, and sending test builds.

## 0.15.5 — 2026-11-13

### Android, iOS, and Web

- Add a Share button to the game details toolbar that shares a link to the game.
- Show the release date on game details as a calm chip with a calendar icon instead of a bright white tag.

### Web

- Restyle the Open Releases and Open News buttons on the wide Home screen as outlined buttons with an arrow, so they
  no longer look like the Coming soon tag.

## 0.15.4 — 2026-11-13

### Android, iOS, and Web

- Update the JSON library with upstream fixes and faster JSON text output.

### Android

- Build with the latest Android Gradle Plugin bug-fix release.

## 0.15.3 — 2026-11-13

### Android, iOS, and Web

- Show the "In development" badge on Profile in the website's colors, with lighter pink text that is easier to read.

### Website and documentation

- Rewrite braindancegg.com for players, in plain language, and the README for developers, with a quick start.
- Add a security policy for private vulnerability reports, a pull request template, and a code style guide.
- Date every changelog entry.

## 0.15.2 — 2026-11-13

### Android, iOS, and Web

- Developer builds can run offline on built-in sample data (`API_FIXTURES=true`), without a Braindance API. Release
  builds are unchanged and contain no sample data.

## 0.15.1 — 2026-11-12

### Android, iOS, and Web

- Download game covers as WebP, which is smaller than JPEG at the same size.

### Android and Web

- Download a smaller cover file wherever a card is shown small enough for it, such as desktop cards, so covers appear
  sooner.

## 0.15.0 — 2026-11-12

### Android, iOS, and Web

- Redesign Profile into grouped sections: account features that are coming, community links (GitHub, Threads, X, and
  sharing), and About (release notes and the IGDB and GamerPower credits). The app version and its "In development"
  stage sit at the bottom.
- Replace the profile icon with an abstract user avatar in the navigation and page headers.

### Web

- Fix a blank white page in Safari 27.2 and in every browser on iOS and iPadOS 27.2.
- The sidebar now ends with the Profile entry only; the project links, credits, and version moved to Profile.
- The profile button no longer repeats in the page header while the sidebar is shown, and the "In development" badge
  left the Home header.

## 0.14.1 — 2026-11-12

### Android, iOS, and Web

- Credit the data providers next to the app version as "Game data by IGDB" and "Giveaways by GamerPower", with each
  provider name linking to its site.

## 0.14.0 — 2026-11-12

### Android, iOS, and Web

- Credit IGDB as the source of game data with a "Powered by IGDB" link next to the app version.

### Web

- Game pages now use addresses such as `/games/42`. Older `/game/42` links still open the game.

## 0.13.3 — 2026-11-11

### Android, iOS, and Web

- Open a giveaway from Home instantly, using the details already loaded with the list instead of waiting for
  another request. Direct links and Retry still load it from the server.

## 0.13.2 — 2026-11-10

### Android, iOS, and Web

- Show Nintendo Store links on game details when provided by IGDB.

## 0.13.1 — 2026-11-10

### Android

- Update the precompiled startup and Home code profile for the new shared cards.

## 0.13.0 — 2026-11-10

### Android, iOS, and Web

- Show each game's title and why it is trending below its cover in Popular Right Now and Similar games.
- Use the same game and giveaway cards everywhere, with the same loading, empty, and Retry states.
- Share now recommends the Braindance Web app with a short message and its link instead of the GitHub repository;
  where sharing isn't available, Copy invite copies the same text.
- Remove the "See all" buttons that did nothing.
- Show the selected tab's line under the last item of the navigation rail, such as Library in landscape.

### Web

- Show Most Anticipated as covers only on desktop.
- Show the focus outline and focus highlight only for keyboard navigation, not after a click or tap, and drop the
  mobile tap flash.
- Give the Open Releases and Open News buttons on desktop Home a background, so they look like buttons before hover.
- Rename the Star button to "Star us", still on one row with Share in the sidebar and on Profile.

## 0.12.0 — 2026-11-10

### Android, iOS, and Web

- Mark the selected tab in the navigation bar and rail with a short cyan line under its label.
- Shorten Profile's "Star on GitHub" button to "Star", so it stays on one row with Share on narrow screens.

### Web

- Add a desktop layout for wide browser windows: a navigation sidebar, a Home header and discovery banner, and
  Popular Right Now, Most Anticipated, and Latest Giveaways as fixed card rows instead of carousels.
- Show Upcoming releases and Recommended for you as Coming soon previews beside Home content in the widest windows.
- Move the GitHub links and the app version from Profile to the bottom of the sidebar.
- Support hover and keyboard focus on desktop cards and navigation. Narrow windows and mobile browsers keep the
  current layout.

## 0.11.7 — 2026-11-09

### Android and Web

- Keep Home game and giveaway images at the size they are shown instead of their full download size, which uses
  less memory and graphics work while scrolling without lowering image quality.

## 0.11.6 — 2026-11-09

### Android, iOS, and Web

- Scroll Home more smoothly: sections are prepared before they scroll into view and stay ready when you scroll back.
- Paint the screen background fewer times per frame, which lightens scrolling on the GPU.
- Fill the giveaway details top bar with color as you scroll, as on game details.

## 0.11.5 — 2026-11-09

### Web

- Fix the app staying on the loading screen in Safari.
- Download the app's files with the browser's and hosting provider's standard loading and compression.

## 0.11.4 — 2026-11-09

### Web

- Start downloading the app together with the page, so it opens sooner.
- Download about a fifth less on the first visit.
- Show the loading icon immediately, without waiting for a separate download.
- Publish a robots.txt file so search engines can read the app's crawling rules.

## 0.11.3 — 2026-11-08

### Android, iOS, and Web

- Improve heading hierarchy, paragraph spacing, and supporting-text contrast while keeping the Poppins fonts.

### Web

- Hide horizontal list arrow controls on devices with a touchscreen; swipe to browse instead.
- Make enabled list arrows brighter and easier to distinguish from disabled arrows.

## 0.11.2 — 2026-11-08

### Web

- Show the Braindance icon above the loading indicator while the app downloads.
- Mark the app as the page's main content, so screen readers can jump straight to it.
- Download less on the first visit, and load the app from the browser's cache on later visits.

## 0.11.1 — 2026-11-07

### Android, iOS, and Web

- Open game details faster by reusing the cover image already loaded for the game's card.
- Ship two fewer font files, so the app is smaller.

### Web

- Show a loading indicator instead of a blank page while the app downloads.
- Show text in the app's font from the first frame instead of switching fonts after start.
- Stop the pull-to-refresh indicator getting stuck when scrolling past the top with a mouse wheel or trackpad.
- Show the hand cursor over clickable game and giveaway cards.

## 0.11.0 — 2026-11-07

### Android, iOS, and Web

- Introduce the new Braindance app icon.

### Android

- Support themed launcher icons and refresh round and adaptive icons.

### iOS

- Include home-screen icon sizes for iPad.

### Web

- Refresh browser and home-screen icons.

## 0.10.1 — 2026-11-06

### Web

- Fix garbled or missing text shortly after an update, when a cache still held the previous version's text.

## 0.10.0 — 2026-11-06

### Android, iOS, and Web

- Reorganize navigation into Home, Discover, Releases, News, and Library. Discover replaces Search, Library replaces
  Collections, and Releases is new.
- Open Profile from the profile button at the top of each main screen instead of from its own tab.
- Preview planned features, each marked as coming soon: recommendations, a release calendar, news categories and
  sources, game statuses and custom lists, and account features in Profile.
- Fade between tabs, and fade and shift sideways when opening or leaving a screen, instead of sliding a new screen
  over the previous one.

### Web

- Open the new screens at `/discover`, `/releases`, and `/library`; Profile stays at `/profile`.

## 0.9.0 — 2026-11-06

### Android, iOS, and Web

- Show labeled bottom navigation in compact windows and a left navigation rail at widths of 600 dp and above.
- Keep the current screen and navigation history when resizing, rotating, or entering split-screen.

## 0.8.1 — 2026-11-04

### Web

- Use plain addresses such as `/game/42` instead of `/#/game/42`.
- Make the browser's Back button retrace your steps across tabs: after opening a game and switching tabs, Back
  returns to the game and then to Home before leaving the site.
- Open Home's first screen when its address is entered directly.

## 0.8.0 — 2026-11-04

### Android

- Start noticeably faster and download much less: the app is about 2.5 MB instead of 11.6 MB, and its startup and
  Home code is precompiled on install.

### iOS

- Use the wide layout on iPhone in landscape, as on Android.

### Web

- Use the browser's Back and Forward buttons, and share or bookmark links to games and giveaways, such as
  `#/game/42`. Reloading keeps your place.
- Switch between the narrow and wide layouts as you resize the window.
- Restrict the page to Braindance's own servers, IGDB images, and font downloads with a content security policy.

## 0.7.2 — 2026-11-04

### Android, iOS, and Web

- Explain why content could not load, such as no connection, a slow or busy server, or too many requests, instead
  of showing technical error text.
- Show "not found" for a giveaway that has been removed, instead of a generic error.
- Release a game or giveaway screen's data when you leave it, so browsing many items no longer keeps using more
  memory.
- Give image downloads the same timeout and automatic retry as other requests, and size the image cache to the
  device instead of a fixed 512 MB.
- Stop writing diagnostic logs in release builds.

### iOS

- Fix a crash when Braindance is opened in a second window on iPad.

## 0.7.1 — 2026-11-03

### Android, iOS, and Web

- Show an error after a few seconds when the server is slow or unreachable, instead of loading indefinitely; brief
  server hiccups are retried automatically.
- Keep pull to refresh visible until every Home section has reloaded, and never let an older, slower response
  replace a newer one.
- Stop Home and Similar Games lists from crashing on incomplete or repeated cards.
- Show a "not found" message for a game or giveaway that no longer exists, instead of an empty page.
- Show store and giveaway buttons only for secure links, and stop crashing when no app can open a link.

## 0.7.0 — 2026-11-03

### Android, iOS, and Web

- Add an open-source card to Profile with GitHub star and share actions alongside the app version.

## 0.6.1 — 2026-11-01

### Android, iOS, and Web

- Rename Trending Now to Popular Right Now, which describes it more accurately: it ranks the released games people
  are discovering now and moves up recent launches, new platform releases, and showcase appearances. It now normally
  shows about 20 games instead of about five; upcoming games stay in Most Anticipated.

## 0.6.0 — 2026-11-01

### Android, iOS, and Web

- Replace Popular Right Now with Trending Now at the top of Home. It favors games with recent launches, early access
  exits, new platform releases, or showcase appearances over consistently popular games.
- Label trending games with the reason when it is known, such as New release or Now on PS5.

### iOS

- Remove duplicate safe-area spacing above content and below bottom navigation.

## 0.5.0 — 2026-10-31

### Android, iOS, and Web

- Rank Popular Right Now by current play, Steam players, Twitch viewing, and IGDB visits instead of Twitch alone,
  showing only released games.
- Load Popular Right Now with one request instead of two.

## 0.4.2 — 2026-10-31

### Web

- Stay on the current screen, with its back history, after reloading the page.
- Keep carousel titles and cards in place when loading placeholders are replaced by games.

## 0.4.1 — 2026-10-31

### Android, iOS, and Web

- Show loading placeholders while similar games load on game details.

### Web

- Place carousel arrow buttons closer together and give them more space from the content on their right.

## 0.4.0 — 2026-10-31

### Android, iOS, and Web

- Show up to 24 similar games on game details, ranked by gameplay similarity instead of IGDB's fixed list of 10.
- Exclude editions, DLC, and bundles from similar games.
- Show game details without waiting for similar games to load.

## 0.3.6 — 2026-10-31

### Android, iOS, and Web

- Make scrolling and loading placeholders smoother on the home and details screens.

### Web

- Show the dark app background instead of a white page while the app loads.
- Keep carousel arrow highlights inside their round buttons and show a pointer cursor on hover.

## 0.3.5 — 2026-10-30

### Android, iOS, and Web

- Show the app version at the bottom of Profile.

### Web

- Version the browser bundle to prevent cached JavaScript from loading removed Wasm files after an update.

## 0.3.4 — 2026-10-30

### Android, iOS, and Web

- Update shared UI, navigation, and networking libraries with upstream fixes.

### Android

- Target Android 17 (API 37).

### Web

- Tighten spacing between carousel arrows and the See all action.

## 0.3.3 — 2026-10-24

### Android

- Restrict backups and device transfers to approved exports and require encryption for cloud backups.

## 0.3.2 — 2026-10-02

### Android, iOS, and Web

- Use cacheable game reads and start a background backend wake-up request to reduce loading delays.

## 0.3.1 — 2026-09-23

### Web

- Add previous and next controls to horizontal game and giveaway lists.
- Add a favicon and improve browser startup.

## 0.3.0 — 2026-09-21

### Android and iOS

- Update network loading for game covers and giveaway images while retaining disk caching.

### Web

- Add a Kotlin/Wasm browser app with the shared game and giveaway screens.
- Adapt layouts to the browser viewport and enable loading game covers and giveaway images.

## 0.2.1 — 2026-09-21

### iOS

- Raise the minimum supported iOS version from 14.1 to 15.0.

## 0.2.0 — 2026-09-21

### Android and iOS

- Load giveaway lists and details through the configured backend alongside game data.

## 0.1.0 — 2026-09-20

### Android and iOS

- Begin shared Android/iOS version tracking at version `0.1.0`, build `1`.

### Android

- Remove the `-release` suffix from release version names.
