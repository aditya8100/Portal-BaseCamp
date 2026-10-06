# Base Camp — agent docs

Base Camp (repo name: Portal-BaseCamp) is a $0 kitchen display on a Facebook
Portal+ (ADB enabled): a shared board for two household members' Muse
agents plus Homebridge accessories, weather, iCloud calendar, recipes,
and a weekly meal plan. Native Kotlin/Compose APK on the Portal, zero-
dependency Node board server on the Mac mini.

## Doc map

- [architecture.md](architecture.md) — components, data flow, key design decisions.
- [server.md](server.md) — board server: files, endpoints, lifecycle, config, tests.
- [android-app.md](android-app.md) — APK: screens, components, build/install/verify.
- [current-state.md](current-state.md) — what is live now, recent decisions, risks, open items.
- [`../agent-spec.md`](../agent-spec.md) — the LIVE Muse API contract (served at
  `GET /api/spec`). This is what both household Muses read. It is not in
  `docs/` because the server renders it directly.

## Hygiene rules (read before changing anything)

1. **One fact, one home.** `agent-spec.md` owns the Muse-facing API contract;
   `docs/` owns code architecture and live state. Never copy a fact into two
   places — link to it. If two docs disagree, the code wins, then fix the loser.
2. **Every doc has an "Update when" header.** Obey it. If your change matches a
   trigger, update that doc in the same turn as the code — docs ship with the
   change, never later.
3. **API/behavior change checklist:** (a) update `agent-spec.md` first (it is
   live the moment you save — the server reads it per request); (b) update the
   matching doc in `docs/`; (c) add or adjust a test if server behavior changed;
   (d) record it in [current-state.md](current-state.md).
4. **Verify, then write.** Only document behavior you observed this session
   (test output, `curl` response, Portal screenshot). Never write docs from
   memory of what the code "should" do.
5. **`current-state.md` is the handoff.** Keep "Live now" and "Known issues"
   accurate: move finished items out, add new risks as you find them. A stale
   handoff is worse than none.
6. **No secrets in docs.** Secret files (`tokens.json`, `hb.json`, cheatsheets)
   are referenced by name only. Never paste tokens, passwords, or URLs with
   credentials.
7. **New doc?** Add it to the map above and give it an "Update when" header.
   Prefer extending an existing doc over creating one.
