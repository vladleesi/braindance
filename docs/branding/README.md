# Brand assets

The Braindance brand kit: the "bd" mark, the wordmark, and their exports for every platform. SVGs are outlined, so no
font is needed. The kit was supplied by the repository owner as `braindance-brand.zip`.

| Folder | Contents | Used in |
| --- | --- | --- |
| `logo/svg`, `logo/png` | The mark, the wordmark, and the lockups, with and without the trail | Landing site, README |
| `android/res`, `android/*.svg` | Adaptive launcher icons (foreground, monochrome, legacy) for every density | `androidApp` |
| `android/play-store` | Store icon (512) and feature graphic (1024 × 500) | Google Play |
| `ios` | App icon at 1024: standard, dark, and tinted | `iosApp` |
| `web` | Favicons, touch and app icons, maskable icon, link-preview image, and `site.webmanifest` | Landing site, `webApp` |
| `social` | Avatars, with and without the d-pad backdrop, and the X / Bluesky cover | Social profiles |
| `marketing` | Banner without avatar space, and the GitHub social preview (1280 × 640) | README, repository settings |
| `banner` | Cover-wall social headers (3000 × 1000), straight and angled | Social profiles |

## Color

The brand is dark only. Cyan, magenta, and yellow always appear together, in that order, as the trail.

| Name | Hex | Use |
| --- | --- | --- |
| Ink | `#0A0D14` | Backgrounds |
| White | `#F4F6FA` | Letters and main text |
| Cyan | `#22E4FF` | Trail, first step |
| Magenta | `#FF2E88` | Trail, second step |
| Yellow | `#FFEE0A` | Trail, third step |
| Grey | `#A9B1C2` | Secondary text |
| Navy | `#202A45` | Background symbols |

## Type and the Echo trail

Poppins: Bold for the wordmark, logo, and headings (letter spacing about −3%), Medium for the tagline, Regular for
body text. Behind the white letters sit three flat copies, cyan, then magenta, then yellow, each moved down and right
by 1/24 of the font size. No blur, glow, gradient, or gaps.

## Using the logo

- Keep clear space of half the mark's height on every side.
- Smallest sizes: mark 24 px wide, wordmark 120 px wide. At 48 px and under, `mark-small-on-ink` has a deeper trail that
  survives small sizes. The app's collapsed desktop sidebar uses it (`BraindanceMark`). The favicons use the regular
  trail on an Ink tile, like the touch and app icons, so the letters' counters stay open.
- Place the logo on Ink or a very dark, flat color. On light or busy backgrounds, put it on an Ink tile.
- Where only one color is possible, use `mark-white` / `wordmark-white` or `mark-ink` / `wordmark-ink`, without the
  trail.
- The d-pad and button backdrop is for the social avatar only. In app icons the mark is 72% of the visible icon.
- Do not reorder or recolor the trail, split it, add effects, rotate or stretch the logo, or retype the wordmark.

## In the product

The kit sets the identity; the app and the landing site translate it, each for its job. The landing site is a
marketing page and can be loud. The app is used every day and stays calm.

### Color roles

| Role | Color | Where |
| --- | --- | --- |
| Canvas | Ink `#0A0D14` | Page and screen backgrounds |
| Chrome | `#070A10` | Sidebar, bottom bar, status bar |
| Surface | `#12161F` | Cards, panels, tiles |
| Control | `#1B202B` | Chips, inputs, selected rows; hairlines on the site |
| Raised control | `#262C39` | Secondary buttons, outlines |
| Text | White `#F4F6FA`, Grey `#A9B1C2` | Main and secondary text |
| Accent | Cyan `#22E4FF` | Selection, focus, links: the one functional color |

The surface steps are low-chroma neutrals built from Ink. Navy stays a brand color for background symbols; as a
surface tint it gave the interface a blue cast that competed with cover art. Score rings use a flat
magenta-to-yellow-to-cyan gradient. Game status tags use magenta for development, yellow for unavailable games, and
cyan for rumors. The app's development status uses magenta.

### Shape

- **App:** soft geometry in three steps. Imagery and compact elements (covers, screenshots, tiles, rows) round by
  8 dp; containers (cards, panels, banners) by 16 dp; everything pressable or labelling (buttons, chips, tags, the
  search field) is a pill. Icon buttons and the avatar are circles. Cards are filled with the surface color, not
  outlined.
- **Landing site:** the hero groups square 3:4 covers into strips with 8 px corners. The reel and release target use
  16 px corners and the app’s neutral surface fill. Hairlines divide sections. Buttons and tags are pills.

### The trail in the interface

The trail belongs to the logo first. In the interface it marks only the main action: the primary button (white on Ink in
the dark theme, a pill) lifts and shows the trail under a pointer, on the landing site and in the app alike. The landing
site also uses it on the hero’s final word and the release version numeral. Do not put it on cards, lists, or icons.

Source and permissions are recorded in [artwork and branding notices](../../THIRD_PARTY_NOTICES.md#artwork-and-branding).
