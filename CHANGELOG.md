# Frontend changelog

Release notes for Android, iOS, and Web, plus notable website and documentation changes. Each version lists only
what changed.
History begins with the introduction of shared version tracking in `version.xcconfig`.
The Braindance API is versioned and released separately.

## 0.31.0 — 2026-11-25

### Android, iOS, and Web

- Adult games are marked: opened from a link, a game made for adults shows an Adult tag, and its images and
  description stay hidden. Such games no longer appear in search, lists, or similar games.
- Status tags have new colors: Alpha, Beta, and Early access are yellow, and Offline, Cancelled, and Delisted are blue.

### Website and documentation

- The privacy policy is rewritten in plain first person. It states the 13+ age rule and how mature and adult games are
  handled, and covers two-step verification, passkeys, and security emails.
- braindancegg.com publishes the file Android needs to use passkeys with Braindance.

## 0.30.2 — 2026-11-24

### Android, iOS, and Web

- Internal maintenance for event logo loading placeholders.

### Web

- The event page scrollbar stays within its track when you scroll to the last row of games.

## 0.30.1 — 2026-11-24

### Android, iOS, and Web

- Event pages show placeholders while loading, keep the show name and schedule in view as you scroll, and let
  you share a link to the event. The featured games grid keeps your place when you return from a game.

## 0.30.0 — 2026-11-21

### Android, iOS, and Web

- Browse live, upcoming, and recent gaming events on Home. See when shows start in your time zone, then open a show
  to watch its stream or explore its featured games. Desktop shows one row, with See all for the full list.
- Game pages open with an Overview for the selected edition and up to three highlights. A compact About panel shows
  credits, play styles, themes, series, and estimated play times. Critic and player scores appear in separate
  progress rings beside the color-coded release status when available.
- Game pages scroll with a steady scrollbar. When switching editions, the page keeps its place while the new details
  load and eases into layout changes. On desktop, About sits beside the overview and media, and collapsing expanded
  text keeps the reading position steady.

### Web

- Trailers match the media preview's rounded corners, show when they are loading, and allow page scrolling over
  the picture and player controls. On desktop Home, the News preview spans the main column.

## 0.29.0 — 2026-11-20

### Android, iOS, and Web

- Screenshots open full screen with the button in the corner of the large picture. Swipe or use the arrow keys to
  move through them; pinch, double-tap, or Ctrl-scroll to zoom in, drag to look around, and switch between the whole
  image and one that fills the screen. Closing returns to the screenshot you viewed last. Trailers stay in the large
  picture on the game page.
- If a trailer can't be played in the app, a link opens it on YouTube instead.
- The editions and updates row no longer repeats the selected version's kind under the chips.
- The top of a game page shows the game's key art when it has some, instead of always its first screenshot, and keeps
  faces in view on wide screens.
- "Where to buy" lists the stores as rows with their logos, which are easier to tap and show that they open outside
  the app.

### Android and iOS

- Trailers play right on the game page instead of opening YouTube. YouTube's own button in the player still opens
  the YouTube app.
- Full screen works in portrait and landscape.

### Android

- A playing trailer keeps playing when you turn the phone.

### Web

- The browser's Back button and Esc close full screen. A button there also switches the browser itself to full screen.
- The page scrolls with the mouse wheel over a playing trailer, and a click on the trailer pauses it.
- A trailer starts on the first click: clicking again while it loads no longer stops it.
- On wide screens, the gallery is narrower, with its thumbnails under the large picture and "Where to buy" beside it.
- Profile no longer offers to share the app on desktop, where the address bar already has the link.
- On iPhone and iPad, the keyboard opens when a search button or a second tap on Discover starts a search, not only
  when you tap the search field.

### Website and documentation

- braindancegg.com's latest updates are refreshed.

## 0.28.0 — 2026-11-19

### Android, iOS, and Web

- Game pages have a Media section under the title with the game's trailers and screenshots, videos first. Pick a
  thumbnail to show it large; screenshots keep their shape. Swipe the large picture or use the arrow keys to move
  between them. On a wide screen, the thumbnails sit in a column beside the picture.
- While pictures load, the gallery shows placeholders. A picture you pick appears at once from its thumbnail and
  sharpens when the full-size image arrives.

### Android and iOS

- A trailer opens in the YouTube app, or in the browser without it.

### Web

- A trailer plays in the page once you press play. Nothing loads from YouTube's player before that, and it uses
  YouTube's privacy-enhanced mode, which sets no cookies until you play.
- Previous and next buttons on the large picture move through the media with a mouse. On a wide screen, a scrollbar
  beside the thumbnail column shows how far it goes and can be dragged.

### Website and documentation

- braindancegg.com's First Alpha list is shorter and easier to read, now with screenshots and trailers, and its
  latest updates are refreshed.

## 0.27.0 — 2026-11-19

### Android, iOS, and Web

- The app's fonts are about an eighth of their former size. On the Web, a first visit downloads about 175 KB less
  and opens sooner on slow connections. The fonts now cover English and Spanish. Other letters, such as Polish or
  Japanese ones, appear in a fallback font.

### Web

- On Windows, a middle click on a page that scrolls starts autoscrolling, as on other websites. Move the mouse up or
  down from the marker to scroll, faster the farther you go. A click, Esc, or the mouse wheel stops it. If you hold
  the middle button while moving, releasing it stops autoscrolling.
- A middle click or Ctrl-click (⌘-click on a Mac) opens more links in a new tab: the game named under a DLC's or
  remaster's title, a game's versions, the profile and search buttons, Explore games, Open Releases, and Open News.
  A middle click also opens store, giveaway, community, and data-source links in a new tab.
- When the server can't be reached, for example while offline, Home and other screens say so and offer Retry
  instead of loading forever.

### Website and documentation

- braindancegg.com's latest updates are refreshed.
- braindancegg.com combines Features and Roadmap into one Roadmap section. It shows the rollout steps for Web,
  Android, and iOS at the top and lists every feature under the current First Alpha milestone.

## 0.26.1 — 2026-11-19

### Web

- On desktop Web, a draggable scrollbar appears while scrolling and fades out shortly after scrolling stops.

## 0.26.0 — 2026-11-18

### Web

- Games, giveaways, lists, and the sidebar's destinations open in a new tab with a middle click or Ctrl-click
  (⌘-click on a Mac), and in a new window with Shift-click. A normal click still opens them in the app.
- Text shows from the first screen: the loading screen stays until the app's fonts and text are ready, instead of
  showing pages without text or in a fallback font. Repeat visits start faster, without the cache the app used to
  rebuild on every load.
- Scrolling lists and game grids does less work per mouse-wheel step: items about to scroll into view are prepared
  between frames instead of while the page moves.
- Pages from links, such as stores, open in a new tab that cannot reach back to the app, and the support email
  opens the mail app without an empty tab.

### Android, iOS, and Web

- Profile lists the support address, support@braindancegg.com, under About.
- Game and giveaway grids, such as Popular Right Now and search results, prepare about a screen of cards ahead, so
  covers start loading before they scroll into view.

### Website and documentation

- braindancegg.com lists the support address in the footer and the community section, and its latest updates are
  refreshed.

## 0.25.0 — 2026-11-18

### Android, iOS, and Web

- A game's editions and updates, such as a Game of the Year Edition or a free next-gen update, appear as one row of
  choices under the game. The one you are viewing is marked, with what kind of version it is, and the page shows
  its own name, cover, platforms, genres, stores, and description, with the game's for anything it lacks; the base
  game is one tap away. Switching between them updates the page in place, without opening a new screen or losing
  your place; placeholders show the details that are changing while a slower version loads. Back leaves the game
  instead of stepping through each one. Sharing the page keeps that edition or update.
- A single Related section lists the game's DLC and expansions, remasters and ports, and the bundles that include it,
  as compact rows with tabs for each kind. A bundle lists the games it contains.
- On phones, more than two versions or Related tabs collapse to the selected one and a "+N more" chip that shows the
  rest, so each takes one line.
- Genres use the same chips as versions and tabs.
- A DLC, expansion, or remaster says what it is and links its game right under its title, such as "Expansion for
  The Witcher 3: Wild Hunt".
- Similar games on an edition's or update's page are those of its game.
- Opening the game right under the current page, such as an expansion's game after coming from it, goes back to it
  instead of opening another copy.

### Web

- Search engines treat an edition's or update's page as its game's page, while a shared link still shows the
  edition or update.

### Website and documentation

- Refresh the latest updates on braindancegg.com.

## 0.24.0 — 2026-11-17

### Web

- Links to a game or giveaway show its name, description, and picture when you share them in chats and on social
  networks, instead of a generic Braindance card.
- Popular Right Now, Most Anticipated, Latest Giveaways, and the other pages have their own titles and descriptions
  in search results, link previews, and browser tabs.
- An address that does not exist now reports "page not found" to browsers and search engines, while still opening
  the app.

### Website and documentation

- Refresh the latest updates on braindancegg.com.

## 0.23.0 — 2026-11-17

### Android, iOS, and Web

- Giveaway pages are redesigned. The giveaway's artwork fills the top, with what it is (a game, DLC, or early
  access), the price you save, and the button to get it. Wide windows show its details in a panel beside the text.
- See when a giveaway ends, with the days left in its last month, how many players claimed it, when it was added,
  and every platform and store it is for, including those without a logo.
- Claim instructions are numbered steps, and a new link opens the giveaway's page on GamerPower.
- A giveaway that has ended says so, and its button is turned off.

### Website and documentation

- Refresh the latest updates on braindancegg.com.

## 0.22.1 — 2026-11-17

### Android, iOS, and Web

- Profile and giveaway pages are a single centered column on tablets and wide windows, with the back button and
  title right above their content instead of at the window's edge.
- Library's game statuses and the Releases week view keep a compact width on tablets and wide windows, so the
  month's arrows stay next to its name.

### Web

- In wide windows, every screen lines its content up with Home: the same margins, and the title and buttons stay in
  place as you switch tabs. On very wide monitors, content stays centered at a comfortable width instead of
  stretching across the screen.
- Full lists, search results, and game pages start at the same edge as Home. The arrows for a game's similar games
  sit at the end of their row instead of at the window's edge.

## 0.22.0 — 2026-11-17

### Android, iOS, and Web

- Share Popular Right Now, Most Anticipated, and Latest Giveaways from the top of their full lists, and any giveaway
  from the top of its page. Game pages keep their Share button. Each link opens the same screen in the Web app, and
  in the Android app where it is installed.
- Sharing opens your device's share sheet. Where a browser has none, the button copies the link and briefly shows a
  check mark.

### Web

- Share buttons appear on phones, tablets, and foldables at any size, and in narrow windows. Wide windows on
  computers without a touch screen leave them out, since the address bar already holds the link to every screen.

### Website and documentation

- List giveaway and list sharing among the features on braindancegg.com and refresh the latest updates.

## 0.21.0 — 2026-11-17

### Android, iOS, and Web

- See all next to Popular Right Now, Most Anticipated, and Latest Giveaways opens the whole list: up to 100 popular
  games, 200 anticipated ones, and every current giveaway. More load before you reach the end, and if a page fails,
  the games above it stay and Retry loads it again. Coming back from a game keeps your place in the list.
- Home now shows its games and giveaways first, with previews of upcoming features below them.
- The search buttons on Home and Library open Discover with the search field ready to type in, and the keyboard
  opens on phones. Library's button searched nothing before. Tapping Discover while it is open does the same, now
  also after the keyboard was put away.

### Android

- Links to games, giveaways, and the full lists on app.braindancegg.com open in the tester app, on top of the screen
  you were on, so Back returns to it. Without the app, they keep opening in the browser.

### Web

- The wide Home layout has the search button too, and Explore games also starts a search on Discover. Each full list has its own address, such as
  `/popular`, that you can bookmark or share.

### Website and documentation

- Refresh the latest updates on braindancegg.com.

## 0.20.1 — 2026-11-17

### Android

- Precompile the code for searching games on Discover and scrolling through results, so the first search after an
  install or update runs smoothly. The startup and Home profile is refreshed as well.

## 0.20.0 — 2026-11-16

### Android, iOS, and Web

- Search games by title on Discover. Results appear as you type, best match first, and show each game's release
  year, so games that share a title are easy to tell apart. Placeholders fill the space while the first results
  load, and clearing the search brings back Discover.
- Scroll through up to 200 results without waiting: more load before you reach the end. If a page fails to load, the
  results above it stay and a Retry button loads it again.
- Going back to a recent search, such as after deleting a letter, shows its results at once.
- Scrolling search results puts the keyboard away and keeps your search.
- Games without a cover, or whose cover fails to load, show their title on the card instead of an empty tile, and
  their page gives the title the full width instead of leaving an empty space for the cover.

### Website and documentation

- List game search among the features on braindancegg.com and refresh its latest updates.

## 0.19.2 — 2026-11-16

### Web

- Return visits load fonts, text, and icons from the browser's cache without checking with the server first, so
  text appears sooner on slow connections. They stay cached across releases until they change.

## 0.19.1 — 2026-11-16

### Android and iOS

- Opening a game reuses the cover its card already saved instead of requesting it again, and a cover shown
  in both card styles is stored once.

## 0.19.0 — 2026-11-16

### Android, iOS, and Web

- Redesign the top of game pages. The game's first screenshot now sits dimmed behind the cover, title, release date,
  and platforms, with a quiet gradient for games without screenshots. Long titles step down in size to stay compact.
- Add an "Add to collection" button and a wishlist button to game pages. They are marked "Coming soon" and stay
  disabled until accounts and collections arrive.
- On tablets and wide browser windows, show a larger cover with the title, genres, and buttons beside it.
- Show genres as quiet tags and the release date as plain text, so nothing looks pressable that does nothing.

### Website and documentation

- Refresh the latest updates on braindancegg.com.

## 0.18.2 — 2026-11-15

### Android, iOS, and Web

- Make the Discover search field compact, with a clear button, a shorter placeholder, and room between it and the
  content scrolling under it, which a thin line marks once you scroll.
- While you type in the Discover search field, the title and profile button make way, so the field moves to the top
  on phones and tablets.
- Back now leaves Discover search step by step: it first closes the keyboard, then leaves search, and only then leaves
  the screen. The arrow in the field and the Escape key also leave search.
- Turn off autocorrect in the Discover search field, so game titles stay as typed.
- Tapping Discover while a screen such as Profile is open on it now returns to Discover; tapping it again focuses
  the search field.
- Keep the screen in view while the keyboard is open. The keyboard now covers the navigation bar; on iOS, it
  used to squeeze the Discover screen down to its title.

### Website and documentation

- Move the game covers into the braindancegg.com hero as three drifting rows in place of the app screenshot.
- Shorten the braindancegg.com features to four short titles with one line each, and list Web, Android, and iOS
  beside the heading with their status as plain text instead of button-like tags.
- Part the roadmap milestones with a thin line, list their features in two aligned columns, and drop the completion
  marks.
- Refresh the latest updates on braindancegg.com.

## 0.18.1 — 2026-11-15

### Android, iOS, and Web

- Add a permanent Discord invite to Profile and put Discord and Reddit first in the Community links.

### Website and documentation

- Add the permanent Discord invite to braindancegg.com and put Discord and Reddit first in its social links.
- Shorten braindancegg.com: what you can try today replaces the feature screenshots, and a new panel shows that
  Braindance is one app for Web, Android, and iOS, with Android alpha testing next and iOS after it.
- Add a roadmap timeline with each milestone's status, the Q2 2027 target for version 1.0, the latest updates, and a
  Community section that puts Discord and Reddit first, with X, Bluesky, and Threads as smaller buttons.
- Show the social icons on braindancegg.com in each platform's own color.

## 0.18.0 — 2026-11-15

### Android, iOS, and Web

- Refine the look: calmer neutral surfaces instead of blue-tinted ones, softly rounded covers and cards, and
  round buttons and tags.
- Make the main button of a screen white, so the main action is easy to find.
- Start the Home screen's first row right under its title.
- Set headings slightly tighter.
- Add the Braindance subreddit (r/BraindanceGG) to the Community links on Profile.

### Web

- Show the Braindance logo in the sidebar.
- Keep long descriptions at a comfortable reading width on wide windows.
- The main button lifts and shows the logo's colors under the mouse pointer.

### Website and documentation

- Redesign braindancegg.com: a large headline, numbered sections, real screenshots of the app, and a band of game
  covers in place of the cover wall.
- Add a link to the Braindance subreddit to the braindancegg.com footer.

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
