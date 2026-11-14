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
- Smallest sizes: mark 24 px wide, wordmark 120 px wide. At 48 px and under, `mark-small-on-ink` has a deeper trail
  that survives small sizes. The favicons use the regular trail on an Ink tile, like the touch and app icons, so the
  letters' counters stay open.
- Place the logo on Ink or a very dark, flat color. On light or busy backgrounds, put it on an Ink tile.
- Where only one color is possible, use `mark-white` / `wordmark-white` or `mark-ink` / `wordmark-ink`, without the
  trail.
- The d-pad and button backdrop is for the social avatar only. In app icons the mark is 72% of the visible icon.
- Do not reorder or recolor the trail, split it, add effects, rotate or stretch the logo, or retype the wordmark.

Source and permissions are recorded in [artwork and branding notices](../../THIRD_PARTY_NOTICES.md#artwork-and-branding).
