# Current state

> Update when: anything below becomes untrue. This is the handoff — keep it
> short, keep it true. Last verified: 2026-10-06 (see rule 4 in
> [README.md](README.md): re-verify before trusting).

## Live now

- Board server on the Mac mini (`com.portalhomebase.board`, :8091): healthy —
  cards + homebridge + weather + calendar all `true`.
- Portal+ in the kitchen on the latest APK: Board / Home / Week / Meals /
  Recipes / Settings, flip clock, Austin skyline, sun-driven Auto theme.
- Cards on the board: a seeded weekly meal plan (two meals linked to the
  saved recipe), Groceries list, a feature-requests note, one favorited family
  recipe, one hidden agent bootstrap card.
- `/api/spec` (v3) serves the current `agent-spec.md`: Board contents rule,
  retroactive recipe linking, meal-plan weekly hygiene.
- Tailscale Funnel live (mini hostname `*.ts.net` → :8091; exact URL lives in
  the gitignored Muse cheatsheets): verified `/api/health` 200, `/api/spec`
  401 without token.

## Standing decisions (don't relitigate without the user)

- Display name "Base Camp"; no file/package/launchd renames.
- Board = notes, lists, alerts, meal plans. Recipes only in Recipes tab.
- Summary header only on Board (scrolls with cards); all other tabs fullscreen.
- Meal plans never auto-expire; the weekly rhythm is delete-then-repost.

## Known issues / risks

- Portal uses the LAN URL (`http://192.168.1.73:8091`); DHCP drift on the
  mini's IP would break it (funnel URL is the fallback).
- Skyline canvas jank on the 2019 Portal (~21–30 ms/frame); acceptable today.
- Funnel reboot survival not yet test-observed (config should persist in
  tailscaled, but nobody has rebooted the mini to prove it).
- Board tokens live in app prefs and the Muse cheatsheets; server hardening
  (beyond Bearer [REDACTED] deferred by the user.
- Old meal plans stack on the Meals tab until a Muse deletes them (by design —
  spec documents the hygiene).
- Hourly weather strip still shows % on every hour (daily rows follow the
  rain-only rule).

## Open / parked

- Second household member's Muse onboarding; grocery import (blocked on access).
