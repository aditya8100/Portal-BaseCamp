# Architecture

> Update when: a component is added/removed, data flow changes, or a major
> design decision is made or reversed.

## Components

```
Muse (member 1) ─┐
                   ├─HTTPS/HTTP─> board server (Mac mini :8091) ──> Homebridge (Pi :8581)
Muse (member 2) ───┘                      │  │  │
                                       │  │  └─> iCloud webcal (family calendar)
                                       │  └────> Open-Meteo (weather °C + sun times)
                                       └───────> Portal+ APK (polls every 30s)
```

- **Board server** (`board.mjs`, ~770 lines total with modules, zero deps): cards
  CRUD, weather/calendar/Homebridge proxies, live agent spec. See [server.md](server.md).
- **Portal APK** (`android/`, Kotlin/Compose, package `com.portalhomebase.app`):
  kiosk HOME app, keep-screen-on, polls the server. See [android-app.md](android-app.md).
- **Homebridge** (Pi, default `HB_URL` in `board.mjs`, insecure mode so the UI/API
  works): accessories + on/off/brightness. Credentials in `hb.json` (owner-only).
- **Upstreams**: Open-Meteo (fixed home coordinates from `config.json`,
  cached 15 min server-side) and one iCloud webcal URL (family calendar, own
  RRULE expander in `cal.mjs`).

## Data flow

- Writes: Muses `POST/PATCH /api/cards` with a Bearer [REDACTED] Cards persist to `cards.json`.
- Reads: the APK polls `GET /api/cards`, `/api/weather`, `/api/calendar`,
  `/api/calendar/week`, `/api/home/accessories` every 30 s. No push/SSE.
- The app never talks to Homebridge/iCloud/Open-Meteo directly — only the server.

## Key design decisions (locked unless the user reopens)

- $0: existing Mac mini + Pi + Portal only. No new hardware or paid services.
- No Google services on the Portal; no backward compat for the card API.
- Board server on the Mac mini under launchd; the Portal uses the LAN URL.
- Fixed home location (no GPS on the Portal).
- Pretty UI is paramount: warm light paper theme, sun-driven Auto mode,
  hand-drawn animated Austin skyline; translucency must keep text readable.
- Weather in Celsius; rain % only on rain-bearing days, emoji on every condition.
- Display name is **Base Camp**; package, files, and launchd labels keep the
  old `portalhomebase` names (no renames).
