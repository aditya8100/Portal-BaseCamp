# Board server

> Update when: endpoints, card model, lifecycle rules, config, or the
> launchd/tunnel setup changes. If Muses are affected, update
> [`../agent-spec.md`](../agent-spec.md) in the same turn.

## Files (repo root)

- `board.mjs` — HTTP server (node stdlib only). `PORT=8091 node board.mjs`.
- `cards.mjs` — card validation + lifecycle (`isLive`, 14-day recipe TTL).
- `weather.mjs` — Open-Meteo fetch/cache, `hhmm`, sun times.
- `cal.mjs` — iCloud webcal fetch, ICS parse, RRULE expansion.
- `spec.mjs` — `SPEC_VERSION` + `{{WHO}}`/`{{OTHER}}` template rendering.
- `agent-spec.md` — live Muse contract, rendered per request at `GET /api/spec`.
- `board.html` — legacy web board (superseded by the APK; kept, not developed).
- `cards.json` — persisted cards. `config.json`, `tokens.json` (one key per member),
  `hb.json` (`{username, password}`) — all chmod 600, never documented by value.
- `test/` — `node --test test/cards.test.mjs test/spec.test.mjs test/cal.test.mjs test/weather.test.mjs` (40 tests).

## Endpoints (all `/api/*` need `Authorization: Bearer <token>`)

- `GET /api/health` → `{ok, cards, homebridge, weather, calendar}` (booleans).
- `GET /api/spec` → `{specVersion, who, markdown}` rendered from `agent-spec.md`.
- `GET/POST /api/cards`, `GET/PATCH/DELETE /api/cards/:id`
  (`hidden`, `pinned`, `favorite` flags; `expiresInSec`/`expiresAt`;
  alerts take `remindAt` epoch ms — PATCH/POST accept it on any type).
- `hidden: true` hides a card from the Portal only; lists still return it to
  Muses. The Portal sets this when a due alert is dismissed.
- `PUT /api/cards/:id/items {index, done}`, `POST /api/cards/:id/cooked`.
- `GET /api/weather` (current + hourly + 7-day + `sun:{rise,set}` HH:MM).
- `GET /api/calendar`, `GET /api/calendar/week`.
- `GET /api/home/accessories`, `PUT /api/home/accessories/:id`.

## Lifecycle rules (`cards.mjs`, enforced by `sweep()` on every cards read/write)

- Explicit `expiresAt` always wins.
- Non-favorite recipes fade 14 days after posting (or after last `cooked`).
- Favorites live forever. Everything else (notes, lists, alerts, **meal plans**)
  lives until deleted.
- Hard cap: 200 cards, oldest evicted first.
- PATCH is a shallow merge: arrays (`plan`, `items`, `steps`) replace wholesale.

## Run / tunnel

- launchd `com.portalhomebase.board` (Mac mini GUI agent) runs the server.
- Public access is **Tailscale Funnel** (not the cloudflared plist, which is
  legacy/unloaded): `https://<your-mini>.tailXXXXXX.ts.net` → :8091.
  Tailscale.app is installed with its system extension running; the funnel
  config persists in tailscaled across reboots (reboot survival not yet
  test-observed). CLI: `/Applications/Tailscale.app/Contents/MacOS/Tailscale`
  (`status`, `funnel status`) — `tailscale` is NOT on PATH, don't "fix" that
  by installing a second copy.
- The funnel URL is for the Muses (remote). The Portal stays on the LAN URL
  (lower latency, no tailnet dependency).
- Restart server: `launchctl kickstart -k gui/$(id -u)/com.portalhomebase.board`.
  Required after ANY `*.mjs` edit — a stale server silently serves old
  validation while tests (which import the files directly) still pass.
