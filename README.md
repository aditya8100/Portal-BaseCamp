# Base Camp

A $0 kitchen display on a Facebook Portal+: a shared board for two household
members' Muse agents, plus Homebridge accessories, weather, family calendar,
recipes, and a weekly meal plan.

- **Board server**: zero-dependency Node.js, runs on a Mac mini.
- **Portal app**: native Kotlin/Compose APK, kiosk mode, animated skyline.

## What you need

- Mac mini (or any always-on Mac) with Node.js 18+.
- Facebook Portal+ with ADB access, on the same LAN.
- Homebridge on the network (optional but recommended).
- Tailscale on the Mac (for the stable public URL your Muses use remotely).
- An iCloud published calendar URL and your home coordinates (for weather).

## Setup

1. **Clone and configure the server:**
   ```sh
   git clone https://github.com/aditya8100/Portal-BaseCamp.git && cd Portal-BaseCamp
   cp config.example.json config.json        # home lat/lon, tz, calendar URL
   cp tokens.example.json tokens.json        # then replace with real tokens:
   openssl rand -hex 32                      # one per household member
   chmod 600 config.json tokens.json
   ```
   Optional: `hb.json` with `{"username": "...", "password": "..."}` for
   Homebridge (chmod 600). Without it, Home features report unavailable.
   `HB_URL` env var overrides the default Homebridge address; `PORT` (8091)
   overrides the board port.
2. **Run it:**
   ```sh
   node board.mjs
   curl http://localhost:8091/api/health   # all true = good
   ```
   Keep it alive with launchd (see `docs/server.md`).
3. **Public URL:** `tailscale funnel --bg http://127.0.0.1:8091` on the mini.
   Your Muses reach the board at `https://<mini>.tailXXXXXX.ts.net`.
4. **Onboard each Muse:** copy `muse-template.md`, fill in the base URL and
   that person's token from `tokens.json`, and hand it to their Muse. The
   Muse fetches the live API spec from `GET /api/spec` — never paste the
   spec itself.
5. **Portal app:** enable ADB on the Portal, then (see `docs/android-app.md`):
   ```sh
   cd android
   export JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=$HOME/Library/Android/sdk
   ./gradlew :app:assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   Open Settings (gear) on the Portal, enter the server URL (LAN URL of the
   mini, e.g. `http://192.168.1.10:8091`) and a board token, Test & Save.
   Set the app as the home screen for kiosk mode.

## Repo layout

- `board.mjs`, `cards.mjs`, `weather.mjs`, `cal.mjs`, `spec.mjs` — server.
- `agent-spec.md` — live Muse API contract (served at `/api/spec`, personalized
  per token). Edit this when Muse-facing behavior changes.
- `android/` — Portal APK source.
- `docs/` — agent docs: start at [`docs/README.md`](docs/README.md).
- `test/` — `node --test test/*.test.mjs`.
- `board.html` — legacy web board, superseded by the APK.

## Privacy

Secrets and personal data never ship: `tokens.json`, `hb.json`, `config.json`,
`cards.json`, and per-person `muse-*.md` cheatsheets are all gitignored (see
`.gitignore`). Only `*.example.json` templates and `muse-template.md` are
committed.
