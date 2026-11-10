# Frontend changelog

Release notes for Android, iOS, and Web. Each version includes only platforms with user-visible changes.
History begins with the introduction of shared version tracking in `version.xcconfig`.
Backend releases are tracked in the [backend changelog](backend/CHANGELOG.md).

## 0.13.1

### Android

- Update the precompiled startup and Home code profile for the new shared cards.

## 0.13.0

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

## 0.12.0

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

## 0.11.7

### Android and Web

- Keep Home game and giveaway images at the size they are shown instead of their full download size, which uses
  less memory and graphics work while scrolling without lowering image quality.

## 0.11.6

### Android, iOS, and Web

- Scroll Home more smoothly: sections are prepared before they scroll into view and stay ready when you scroll back.
- Paint the screen background fewer times per frame, which lightens scrolling on the GPU.
- Fill the giveaway details top bar with color as you scroll, as on game details.

## 0.11.5

### Web

- Fix the app staying on the loading screen in Safari.
- Download the app's files with the browser's and hosting provider's standard loading and compression.

## 0.11.4

### Web

- Start downloading the app together with the page, so it opens sooner.
- Download about a fifth less on the first visit.
- Show the loading icon immediately, without waiting for a separate download.
- Publish a robots.txt file so search engines can read the app's crawling rules.

## 0.11.3

### Android, iOS, and Web

- Improve heading hierarchy, paragraph spacing, and supporting-text contrast while keeping the Poppins fonts.

### Web

- Hide horizontal list arrow controls on devices with a touchscreen; swipe to browse instead.
- Make enabled list arrows brighter and easier to distinguish from disabled arrows.

## 0.11.2

### Web

- Show the Braindance icon above the loading indicator while the app downloads.
- Mark the app as the page's main content, so screen readers can jump straight to it.
- Download less on the first visit, and load the app from the browser's cache on later visits.

## 0.11.1

### Android, iOS, and Web

- Open game details faster by reusing the cover image already loaded for the game's card.
- Ship two fewer font files, so the app is smaller.

### Web

- Show a loading indicator instead of a blank page while the app downloads.
- Show text in the app's font from the first frame instead of switching fonts after start.
- Stop the pull-to-refresh indicator getting stuck when scrolling past the top with a mouse wheel or trackpad.
- Show the hand cursor over clickable game and giveaway cards.

## 0.11.0

### Android, iOS, and Web

- Introduce the new Braindance app icon.

### Android

- Support themed launcher icons and refresh round and adaptive icons.

### iOS

- Include home-screen icon sizes for iPad.

### Web

- Refresh browser and home-screen icons.

## 0.10.1

### Web

- Fix garbled or missing text shortly after an update, when a cache still held the previous version's text.

## 0.10.0

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

## 0.9.0

### Android, iOS, and Web

- Show labeled bottom navigation in compact windows and a left navigation rail at widths of 600 dp and above.
- Keep the current screen and navigation history when resizing, rotating, or entering split-screen.

## 0.8.1

### Web

- Use plain addresses such as `/game/42` instead of `/#/game/42`.
- Make the browser's Back button retrace your steps across tabs: after opening a game and switching tabs, Back
  returns to the game and then to Home before leaving the site.
- Open Home's first screen when its address is entered directly.

## 0.8.0

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

## 0.7.2

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

## 0.7.1

### Android, iOS, and Web

- Show an error after a few seconds when the server is slow or unreachable, instead of loading indefinitely; brief
  server hiccups are retried automatically.
- Keep pull to refresh visible until every Home section has reloaded, and never let an older, slower response
  replace a newer one.
- Stop Home and Similar Games lists from crashing on incomplete or repeated cards.
- Show a "not found" message for a game or giveaway that no longer exists, instead of an empty page.
- Show store and giveaway buttons only for secure links, and stop crashing when no app can open a link.

## 0.7.0

### Android, iOS, and Web

- Add an open-source card to Profile with GitHub star and share actions alongside the app version.

## 0.6.1

### Android, iOS, and Web

- Rename Trending Now to Popular Right Now, which describes it more accurately: it ranks the released games people
  are discovering now and moves up recent launches, new platform releases, and showcase appearances. It now normally
  shows about 20 games instead of about five; upcoming games stay in Most Anticipated.

## 0.6.0

### Android, iOS, and Web

- Replace Popular Right Now with Trending Now at the top of Home. It favors games with recent launches, early access
  exits, new platform releases, or showcase appearances over consistently popular games.
- Label trending games with the reason when it is known, such as New release or Now on PS5.

### iOS

- Remove duplicate safe-area spacing above content and below bottom navigation.

## 0.5.0

### Android, iOS, and Web

- Rank Popular Right Now by current play, Steam players, Twitch viewing, and IGDB visits instead of Twitch alone,
  showing only released games.
- Load Popular Right Now with one request instead of two.

## 0.4.2

### Web

- Stay on the current screen, with its back history, after reloading the page.
- Keep carousel titles and cards in place when loading placeholders are replaced by games.

## 0.4.1

### Android, iOS, and Web

- Show loading placeholders while similar games load on game details.

### Web

- Place carousel arrow buttons closer together and give them more space from the content on their right.

## 0.4.0

### Android, iOS, and Web

- Show up to 24 similar games on game details, ranked by gameplay similarity instead of IGDB's fixed list of 10.
- Exclude editions, DLC, and bundles from similar games.
- Show game details without waiting for similar games to load.

## 0.3.6

### Android, iOS, and Web

- Make scrolling and loading placeholders smoother on the home and details screens.

### Web

- Show the dark app background instead of a white page while the app loads.
- Keep carousel arrow highlights inside their round buttons and show a pointer cursor on hover.

## 0.3.5

### Android, iOS, and Web

- Show the app version at the bottom of Profile.

### Web

- Version the browser bundle to prevent cached JavaScript from loading removed Wasm files after an update.

## 0.3.4

### Android, iOS, and Web

- Update shared UI, navigation, and networking libraries with upstream fixes.

### Android

- Target Android 17 (API 37).

### Web

- Tighten spacing between carousel arrows and the See all action.

## 0.3.3

### Android

- Restrict backups and device transfers to approved exports and require encryption for cloud backups.

## 0.3.2

### Android, iOS, and Web

- Use cacheable game reads and start a background backend wake-up request to reduce loading delays.

## 0.3.1

### Web

- Add previous and next controls to horizontal game and giveaway lists.
- Add a favicon and improve browser startup.

## 0.3.0

### Android and iOS

- Update network loading for game covers and giveaway images while retaining disk caching.

### Web

- Add a Kotlin/Wasm browser app with the shared game and giveaway screens.
- Adapt layouts to the browser viewport and enable loading game covers and giveaway images.

## 0.2.1

### iOS

- Raise the minimum supported iOS version from 14.1 to 15.0.

## 0.2.0

### Android and iOS

- Load giveaway lists and details through the configured backend alongside game data.

## 0.1.0

### Android and iOS

- Begin shared Android/iOS version tracking at version `0.1.0`, build `1`.

### Android

- Remove the `-release` suffix from release version names.
