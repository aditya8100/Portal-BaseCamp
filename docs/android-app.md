# Android app (Portal+)

> Update when: a screen, component, theme rule, or layout behavior changes, or
> the build/verify workflow changes.

## What it is

Native Kotlin/Compose app, package `com.portalhomebase.app`, installed as the
Portal's HOME (kiosk) with keep-screen-on. Polls the board server every 30 s
(`data/BoardState.kt`); server URL + token persist in app prefs
(`data/Prefs.kt`, empty by default — set them in Settings on first run).

## Screens (`ui/`)

- `App.kt` — title row ("Base Camp" + flip clock at right), tab row, ambient
  background. No fixed summary header: Board scrolls its header with the cards;
  every other tab is fullscreen.
- `BoardScreen.kt` — household cards only: notes, lists, alerts, meal plans
  (pinned first). **Never recipes.** Weather + Coming Up header is the first
  full-span grid item, so the whole screen scrolls as one.
- `HomeScreen.kt` — fullscreen Homebridge accessory grid.
- `WeekScreen.kt` — fullscreen 7-day calendar grid; events show time ranges.
- `MealsScreen.kt` — all mealplan cards stacked; linked meals (blue, `→`) open
  the recipe dialog. Multiple plans stack — see lifecycle note in `agent-spec.md`.
- `RecipesScreen.kt` — searchable recipe library (title + notes + ingredients),
  pinned first, then favorites, then newest. ✕ clears the query.
- `SettingsScreen.kt` — appearance (Auto/Light/Dark) + server connection.
- `Header.kt` — weather card + Coming Up card. Locations show venue name only
  (first line, before any comma).
- `SkylineBackground.kt` — hand-drawn animated Austin skyline Canvas behind
  everything: day/night crossfade, weather FX (rain/snow/fog/storm/overcast),
  Texas Capitol + downtown landmarks, swaying treeline, day-only paddleboards.
- `FlipClock.kt` — split-flap clock (`TUE OCT 6 11:18:56 AM`), real 3D flap
  animation per change. Digits settle to the new value at flip start (kills a
  one-frame old-value flash — do not "simplify" this ordering).
- `RecipeDetail.kt`, `theme/` — recipe dialog with anchor-based scaler; theme
  tokens. Auto mode follows Open-Meteo sunrise/sunset (07–19 fallback).

## Build / install / verify (Mac mini)

```sh
cd ~/Projects/PortalHomebase/android
export JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=$HOME/Library/Android/sdk
./gradlew :app:assembleDebug --rerun-tasks   # full rebuild; plain assembleDebug is fine for small edits
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop com.portalhomebase.app   # HOME app relaunches itself
```

- Always `--rerun-tasks` when a change seems to not take effect (stale APKs have
  happened); confirm visually, never assume the install is fresh.
- After data changes on the server, wait 35 s for the app poll before judging.
- Verify with screenshots: `adb exec-out screencap -p > /tmp/x.png`, view it.

## Portal reference (2019 Portal+, 2160x1440 @180dpi, landscape)

- Tab row: y=246; x = 230 (Board), 631 (Home), 1031 (Week), 1431 (Meals),
  1831 (Recipes), 2081 (gear). Tap: `adb shell input tap X Y`.
- Text input: `adb shell input text hello` (`%s` for spaces). Back: `adb shell
  input keyevent 4`. Scroll: `adb shell input swipe X Y1 X Y2 MS`.
- **Do not use `uiautomator dump`** — the always-on animations keep the UI
  perpetually non-idle, so dumps fail and `cat` re-reads a stale file. Use the
  computed coordinates above.
- Perf note: the always-on skyline canvas runs ~21–30 ms/frame with high jank
  on this old device; taps still feel responsive. A 30 fps throttle is the
  standing idea if it ever feels sluggish.
