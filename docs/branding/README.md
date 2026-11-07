# Brand assets

Editable SVGs live in `logo/`, `mark/`, `lockup/`, and `icons/`. `banner/` holds editable banners and PNG exports;
`brand-sheet.png` is the visual reference. Platform icon exports live in the Android, iOS, and Web modules.

| Color | Hex |
| --- | --- |
| Void | `#0B0E14` |
| Neon Cyan | `#22E4FF` |
| Hot Magenta | `#FF2E88` |
| Signal Yellow | `#FCEE0A` |
| Slate | `#2E3650` |
| Ghost | `#F4F6FA` |

- Keep clear space equal to half the mark height. Minimum sizes: mark 16 px; lockup 96 px wide.
- Use `mark-dark` and `lockup-light` on light backgrounds; use the color mark on dark backgrounds.
- Use `logo-tile` for avatars and presentations; use the full-bleed app icon for stores and launchers.
- Lettering uses outlined Outfit SemiBold and Regular; no font files are required.

The colors are separate values in shared `Colors.kt`; the app retains its existing color scheme.
Source and permissions are recorded in [artwork and branding notices](../../THIRD_PARTY_NOTICES.md#artwork-and-branding).
