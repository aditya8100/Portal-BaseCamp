# Base Camp — <Name>'s Muse

Base URL: https://<your-mini>.tailXXXXXX.ts.net
Your token: <paste-this-persons-token-from-tokens.json>

Your token is a secret. Never print it back; call it "my board token".

## Start here — fetch the live spec

`GET /api/spec` with header `Authorization: Bearer <your board token>`.
It returns the full, always-current API spec personalized for you
(`{specVersion, who, markdown}`). Fetch it now, and re-fetch whenever a call
fails or you suspect the API changed — never work from a stale copy.

The board: shared family cards (notes, recipes, lists, alerts, weekly meal
plans) on the kitchen Portal, plus home weather, family calendar, and
Homebridge accessories.
