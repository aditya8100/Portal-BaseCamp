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
  full-span grid item, so the whole screen scrolls as one. Alerts past
  `remindAt` render as full-width "Due now" cards (excluded from the normal
  grid); tap → Keep/Dismiss dialog, Dismiss sets `hidden: true`.
- `Chime.kt` — synthesized two-tone reminder chime (AudioTrack, no assets).
  A 15s tick re-checks due alerts; a per-run `fired` set keeps one chime
  per card per process launch (re-chimes after app restart until dismissed).
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

## Remote ADB (Portal stays in the kitchen)

The Portal runs ADB over Wi-Fi so builds/installs/screenshots work from the
Mac mini with no USB cable. One-time setup per Portal reboot, with USB
attached:

```sh
adb tcpip 5555 && adb connect 192.168.1.87:5555   # Portal's Wi-Fi IP
```

- Accept the on-device RSA prompt once (tick "always allow"); later connects
  are silent.
- While USB is also attached, target TCP explicitly: `adb -s 192.168.1.87:5555
  <cmd>`. After unplugging USB, plain `adb` commands just work.
- If the IP changes (DHCP/network switch), read it on the Portal itself:
  Settings (gear) > "This Portal" card shows the Wi-Fi IP and the exact
  connect command. From the mini, `scripts/find-portal.sh [subnet]` probes
  the LAN for port 5555 (a router DHCP reservation avoids all of this).
- **Reboot caveat:** TCP mode does NOT survive a Portal reboot (Android 10, no
  wireless-pairing; `persist.adb.tcp.port` is SELinux-blocked). The Portal is
  always-on, so this is rare — but any move/power-loss triggers it.
- **After a reboot (recovery procedure):** someone with physical access plugs
  ANY laptop into the Portal over USB and runs `adb tcpip 5555`, accepts the
  on-device RSA prompt (tick "always allow"), then unplugs. The Portal never
  needs to come back to the Mac mini's desk. Then from the mini: find the new
  IP (Settings > This Portal on-device, or `scripts/find-portal.sh`) and
  `adb connect <ip>:5555`. Verify with `adb shell echo ok` before resuming work.
- Reconnect after network hiccups (no reboot): `adb disconnect && adb connect
  <ip>:5555`.
- If the port refuses but the IP is right, the TCP port may differ from 5555
  (a laptop typo once started it on 5556): check over USB with
  `adb shell getprop service.adb.tcp.port`. The finder script probes both
  5555 and 5556.

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
