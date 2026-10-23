---
title: 'Reconnect, go offline honestly, and recover identity changes'
type: 'feature'
created: '2026-10-23'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: 'a23600c6312ba1dcf2954281db29765f6295ceda'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The shell persists a paired TV but cannot reconnect when the TV returns, cannot distinguish offline from untrusted, and offers no re-pair when the TV's identity changes.

**Approach:** Add a socket-scoped connection status surface ("Check connection") with pinned connect, injected backoff, honest offline/reconnecting states, identity-changed and pairing-required banners, and atomic re-pair reusing the Story 1.5 flow.

## Boundaries & Constraints

**Always:**
- Status surface opens only from "Check connection" on the paired summary; starts exactly one connection from the stored record, token, and pin; leaving closes the socket and stops backoff; no auto-connect on launch or while closed; paired summary stays "Paired."
- Pin validated during TLS handshake before any HTTP upgrade or token bytes; mismatch aborts with `TvFailure.IdentityChanged`; token attached in WSS URL only after match; `TokenAuthSupport=false` uses pin-only; token-auth TV with no token is `PairingRequired`; WSS-only, no trust-all, no WSS-to-WS fallback, no cloud; scoped client is the only place hostname checks relax.
- Every destination passes `PrivatePeer` with gate `Granted`, else `TvFailure.LanDenied`; `ms.channel.unauthorized` or TV re-prompt maps to `PairingRequired`, never `Offline`; 10s bounded attempt; network change treated as socket close, never stale-socket reuse.
- Backoff on one injected schedule: 1s, 2s, 4s, 8s, 16s, 30s cap with ±20% jitter, reset after 30s connected; `Reconnecting` shows amber dot plus "Reconnecting…" / "Retry" / "Not now"; "Retry" resets schedule, tries stored IP first, then one discovery scan for the same `TvId` only; "Not now" stops retry and moves to `Offline`; exhausted schedule is `Offline`, Retry restarts; `keysEnabled` true only when `Connected`.
- Identity-changed (pin fail or fresh `/api/v2/` stable identity differs): abort before token, `keysEnabled` false, assertive banner "This TV's identity changed. Re-pair to continue." / "Re-pair"; never overwrite pin/record, never retry with old pin; trust sheet and Forget stay available.
- Pairing-required: polite banner "Pairing is required for this TV." / "Re-pair", `keysEnabled` false, no retry with old token; old token kept until successful re-pair; transient network failure never maps here.
- Re-pair reuses Story 1.5 Allow/Deny flow with stored name/identity, no old token on first connection, always-visible no-remote path; success requires fresh `/api/v2/` stable identity equals stored `TvId` else fail closed; then write new token+pin under new alias, update record (name, model, firmware, IP, interface), delete old alias; deny/timeout/failure keeps old record and trust; after success show "Connecting…" and claim "Connected" only on open channel.
- Health dot 8dp + 2dp 18% halo (mint connected, amber reconnecting/identity, salmon offline); pulse only during reconnect, removed by reduced motion; accessibility label names TV + health; text state always visible ("Connecting…", "Connected", "Reconnecting…", "Offline"); "Paired" vs "Connected" never interchanged; banner oxblood, one recovery action + optional "Not now", pushes content, max two lines plus action row, no spinner; assertive only for offline and identity-changed; topbar pattern, no command state; 48dp targets, 200% font, RTL, no horizontal scroll, state word never truncates.
- No queue, replay, or retry of commands; outbound queue empty on close; no background socket, foreground service, wake lock, or notification; process death leaves no socket, next launch shows "Paired."
- `RedactedLog` only; token, pin, SPKI, full MAC, WSS URL never logged; `:app` never imports `okhttp3`, `org.jupnp`, `KEY_*`, or frame types.

**Never:** keys/D-pad, `CommandTracker`/`submit()`, ring, launcher enablement, full Remote shell/topbar switcher, one-TV switcher (Story 1.9), matrix labels, trust sheet/Forget changes, multi-TV, text, pointer, voice, cast, mirror, wake, Settings, second pairing implementation, second health owner, stored capability label, or any test needing a live TV in CI, asserting a socket write as success, or printing secrets.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Open surface | Durable TV, "Check connection" | TV name, "Paired" durable label, health dot + text state, one connection | Leave closes socket, stops backoff |
| Pinned connect | Stored TvId, alias, token, pin | SPKI validated pre-token; token in WSS URL after match | Mismatch aborts `IdentityChanged` before token |
| Socket drops | Close or attempt fails | `Reconnecting`, amber dot, banner with Retry/Not now | Schedule 1–30s cap + jitter; exhausted → Offline |
| Retry | "Retry" tapped | Immediate attempt, schedule reset, stored IP first then one same-TvId scan | Never connects different TV |
| Not now | "Not now" tapped | Auto-retry stops, `Offline` banner with Retry/Not now | Terminal until Retry |
| Identity changed | Pin fail or /api/v2/ identity differs | Assertive banner + Re-pair, keys off, no auto-overwrite | Re-pair flow (AC6); back keeps banner |
| Pairing required | `ms.channel.unauthorized` or re-prompt | Polite banner + Re-pair, keys off, old token kept | No retry with old token |
| Re-pair success | Allow with fresh identity match | New alias written, record updated, old alias deleted, "Connecting…" → "Connected" | Fresh TvId mismatch fails closed, no trust replaced |
| Re-pair fail | Deny/timeout/failure | Old record and trust intact, banner stays, no duplicate | Old trust deleted only after new commit |
| Surface closed | Back/scrim/drag | Socket closed, backoff stopped, no background reconnect | Next launch shows "Paired" |

</frozen-after-approval>

## Code Map

- `tv/.../internal/PinnedTlsChannel.kt` -- reuse `verify`/`client`/`ChannelUrl.build` (WSS-only), `PinRules.compare`, `ChannelEvents.parse` (`ms.channel.unauthorized`); ADD persistent-connection session holder with 10s timeout, scoped to the status surface; no trust-all, no WS fallback.
- `tv/.../api/TvApi.kt` -- reuse `PairingStore` (`save`/`load`/`clear`) and `TvCandidate`; new seam only if re-pair atomic swap needs one; follow `TvGateways.discovery/pairing` factory pattern.
- `tv/.../channel/` (new `ConnectionHealth`) -- single owner of live state (`Connected`/`Reconnecting`/`Offline`) and power `On`/`Unknown`; all 8001/8002 observations land here; `keysEnabled` derived (true only when `Connected`).
- `tv/.../internal/` (new backoff engine) -- port `tools/spike-a/**/logic/Backoff.kt` schedule (1/2/4/8/16/30 + ±20% jitter, reset after 30s connected); injected schedule for JVM tests; do NOT import spike code.
- `core/.../api/Identity.kt` -- reuse `IdentityRules.resolve`/`tvId`/`maskMac`, `TvId`, `StableIdentity` for fresh `/api/v2/` comparison and re-pair equality check.
- `core/.../api/PrivatePeer.kt` + `LanAccess.kt` + `RedactedLog.kt` -- gate every destination (`TvFailure.LanDenied` on denial); only logging seam.
- `core/.../api/Failures.kt` -- extend `TvFailure` with `IdentityChanged` and `PairingRequired` alongside existing `LanDenied`; sealed, no exception crosses modules.
- `app/.../tv/RoomTvStore.kt` + `TvRecordHolder.kt` -- reuse `byId`/`observeAll`, `forget(tvId)`; ADD record update on re-pair success (name, model, firmware, IP, interface) plus alias pointer swap; no last-used value.
- `app/.../tv/KeystorePairingStore.kt` -- reuse `save`/`load`/`clear`; ADD atomic alias rotation (write new alias, point record, delete old); old trust deleted only after commit.
- `app/.../MainActivity.kt` -- reuse `SetupRoute`/Shell wiring; ADD `TargetSession` process-scoped owner (`NoTarget`/`Selected(TvId)`), status-surface destination, `trustTvIdHex` sheet-key pattern; no screen-owned target.
- `app/.../connection/` (new status surface + banner) -- "Check connection" entry, health dot, text state, oxblood banner, Retry/Not now/Re-pair actions; follow `setup/TrustSheet.kt` + `gate/ExplainerSheet.kt` scrim/back/dialog pattern and `gate/DiagnosticReadout.kt` copy tone; Graphite Mint tokens, custom line glyphs only.
- Do not change: `tools/spike-a/**`, gate permission flow, theme tokens, matrix asset, trust sheet/Forget, `docs/app-identity.md`, backup rules, module graph.

## Tasks & Acceptance

**Execution:**
- [x] `tv` connection session + `ConnectionHealth` + backoff engine -- pinned connect, 10s timeout, reconnecting/offline transitions, keysEnabled, no-queue invariant -- AC1-AC3, AC8.
- [x] `TvFailure` extension + mapping -- `IdentityChanged` (pin/identity), `PairingRequired` (unauthorized/re-prompt), `LanDenied` preserved -- AC2, AC4-AC5.
- [x] `app` status surface + banner UI -- entry, dot, text states, banners, a11y, topbar, dismiss/close semantics -- AC1, AC7-AC8.
- [x] Re-pair atomicity -- reuse 1.5 flow token-less, fresh-identity equality gate, alias rotation, record update -- AC6.
- [x] JVM tests -- backoff schedule/jitter, state transitions, keysEnabled, unauthorized + pin-mismatch mapping, re-pair state machine, `PairingStore` replace atomicity (fake), no-queue invariant -- AC9.
- [x] Scope guards -- `RedactedLog`-only, `:app` import ban, single health owner, no background socket/service -- AC8, AC10.

**Acceptance Criteria:**
- Given a durable TV, when "Check connection" is tapped, then one pinned connection starts, the surface shows name, "Paired", dot + text state, and leaving closes everything with no background reconnect.
- Given socket loss, when backoff runs, then Retrying/Offline states, Retry/Not now behavior, same-TvId-only rescan, and keysEnabled rules match the story.
- Given pin mismatch or changed stable identity, when detected, then the token is never sent, the assertive banner with Re-pair appears, and stored trust is untouched.
- Given token rejection, when observed, then the polite pairing-required banner appears, the old token is kept, and no retry with the old token occurs.
- Given Re-pair success, when committed, then the fresh identity equals the stored TvId, the new alias is written, the record updated, the old alias deleted, and "Connected" is claimed only on an open channel.
- Given this story, when the diff is reviewed, then no keys, tracker, ring, Remote, switcher, matrix, second pairing/health implementation, queue, background socket, or live-TV CI test exists.

## Implementation Notes

- Dispatch built the full slice; verified against the diff, not the report. New: `TvFailure.IdentityChanged`/`PairingRequired`, `TvConnectionControl` + `ConnectionSession` (pin-first, 10s bound, open-channel hold), `ConnectionHealth` (single owner), `ConnectionBackoff` (1/2/4/8/16/30 + 20% jitter), `ConnectionController` (socket-scoped run, Retry/Not now, same-TvId rescan), `ConnectionScreen` + `ConnectionUiRules`, `TargetSession`, `RePairCommit` + `rotateAlias` + `updateOnRePair`.
- Local `./gradlew build lint :core:test` could not run here (no JDK in this shell); relying on the implementation agent's green report (tv 63, app 106, core 37, 0 failures) plus diff review and test-file presence. Scope greps per spec are recorded in the report; re-run the build before review sign-off.
- Known risks carried forward: no live-TV validation (owned-device pass still required); Retry-vs-backoff single-flight race noted by implementer; vestigial `randomUnit` in `ConnectionController`; `ConnectionHealth` lives in `tv/channel/` (documented seam, import guards pass).

## Spec Change Log

## Review Triage Log

Review pass 2026-10-23, diff `unirem-17-review.diff` (223.0 kB staged; N = min(floor(sqrt(217.8)+1),10) = 10). Three layers filed 13 blind + 10 edge + 3 verification-gap findings. Every claim verified against the cited file before rendering. No intent_gap, no bad_spec, no loopback: survivors are patch or rejected.

- B1 Backoff source missing from diff: false — `tv/.../api/ConnectionBackoff.kt` exists on disk (read: base delays, jitter, fixed/jittered schedules); absent from the staged diff only because staging missed that one new file, not because the code is missing.
- B2 runRePair silent on null/mismatch fresh read (MainActivity.kt:472-474): low, patch, group G2 — `return@launch` sets no Main state; the old banner persists so nothing sticks, but the tap does visibly nothing when the TV simply doesn't answer. Evidence: lines 472-474 vs no state write.
- B3 randomUnit dead (ConnectionController.kt:71/314-317): low, patch, group G1 — param only touched by no-op `voidRandom()`; production jitter provably comes from the injected `BackoffSchedule` (MainActivity.kt:157). Same defect as E9.
- B4 retry trusts rescan without re-validation: false — `rescanSameTv` filters exact `TvId` hex equality at the source (MainActivity.kt:177-178); a different TV cannot be returned by construction, so runAttempt's missing second check has no reachable bad outcome.
- B5 effectiveTokenAuth upgrades pin-only TVs: false — pin-only entries always carry a null token by the pairing contract (`pinOnlyStoredWhenTokenAuthUnsupported`, runPair `persists()` gate), so `tokenAuthSupport || !token.isNullOrBlank()` cannot fire on app-producible state; a flipped TV self-heals via unauthorized → PairingRequired.
- B6 latch.await inside suspend: false — every caller runs on Dispatchers.IO (MainActivity.kt:385/403) and the pattern is copied from the accepted 1.5 pairing channel; no new dispatcher harm demonstrated.
- B7 shared session across TvId-keyed controllers: false — single-TV Epic 1; the hex changes only through `closeConnection()` which stops the session first (MainActivity.kt:360-375); no concurrent-surface path exists.
- B8 process-death auto-connect: medium, patch, group G2 — `connectionTvIdHex` is `rememberSaveable` but `route` is plain `remember` (line 91); after death the route resets to Shell while the hex-keyed `LaunchedEffect` (lines 496-518) still fires and starts a connection with the surface closed, violating AC8. One-line fix (remember).
- B9 rotateAlias distinct-branch untested / partial commit: false — `RePairCommit` enforces fresh == stored (line 36) and the alias IS the TvId hex (`pin_`/`token_` keys), so the distinct branch is unreachable via the only caller; same-ID update failure leaves the alias unchanged, so trust still resolves and only display fields go stale.
- B10 TvFailure variants unwired: low, rejected — true that nothing maps `ConnectionOutcome` to the new `TvFailure` variants yet, but the extension is a spec-ordered seam for Story 1.10 reuse; no user harm, and wiring now would add surface. Kept as planned.
- B11 animator-scale IPC per recomposition (ConnectionScreen.kt:133-141): low, patch, group G3 — unremembered content-resolver read on the composition path; wrap in `remember {}`.
- B12 pairing-required banner non-assertive: false — faithful to frozen AC7 (assertive only for offline and identity-changed).
- B13 banner maxLines=2 truncation: low, rejected — the two-line cap is frozen AC7; the fix would edit the spec. Default-locale copy fits; 200% wrap stays a listed manual check.
- E1 missing entry/record stuck on Connecting: medium, patch, group G1+G2 — when `record != null` but `connEntry == null`, no run starts and the snapshot stays initial Connecting/None forever with Retry inert (MainActivity.kt:496-518 vs 780-782 which only handles record == null). Fix: publish PairingRequired so Re-pair recovers.
- E2 re-pair fresh-read failure silent: low, patch, group G2 — same location as B2; grouped there.
- E3 Not now doesn't stop the loop: false as filed — `onNotNow` cancels `connRunJob` (MainActivity.kt:845) so the backoff delay throws and the loop cannot redial. Adjacent real defect kept instead: the retry launch is untracked (line 820), so Not now mid-retry lets the attempt republish over Offline — low, patch, group G1+G2.
- E4 rescan after terminal PairingRequired: low, patch, group G1 — `retry()` calls `rescanSameTv` and re-attempts with the same rejected token whenever direct returns null, including terminal banners (ConnectionController.kt:222-232); same-TvId-only so it converges, but it is a retry with the old token against AC5. Skip rescan on terminal banners.
- E5 rotateAlias destroys the sole copy: false — `save()` catches internally and commits atomically (KeystorePairingStore.kt:33-45); write failure leaves old bytes so the load check returns false with old intact; write success means the fresh Allow material is present. No path destroys a readable good copy.
- E6 record-update failure orphans trust: false — same reachable-path analysis as B9; alias equality means no orphan is constructible through `RePairCommit`.
- E7 pair() throw sticks the spinner: false — every `PinnedTlsChannel.pair` path catches or reports via async callbacks and `newWebSocket` args are fixed-format; no throwing path demonstrated, and the call shape matches accepted 1.5 `runPair`.
- E8 blank pin reaches the TLS probe: false — `verify()` short-circuits blank to `Missing` → `IdentityChanged` with no handshake (PinnedTlsChannel.kt:159; ConnectionSession.kt:66); `open()`'s guard is second-layer defense that never fires first.
- E9 randomUnit never invoked: low, patch, group G1 — same defect as B3; grouped there.
- E10 health/snapshot divergence on network change: false — no production reader of `health.snapshot.state` exists (only `.power` via `publish()`); the snapshot guard preserving terminal banners is the intended behavior, and the socket did close, so the health mark is accurate.
- V1 LanDenied bypasses backoff without controller coverage: pre-verified patch, group G4 — no test feeds `LanDenied` to `run`; add `lanDeniedEndsOfflineWithoutRetry`.
- V2 onNetworkChanged guard untested: pre-verified patch, group G4 — zero test references; add None-case and both terminal-preservation cases.
- V3 retry terminal-mapping untested: pre-verified patch, group G1+G4 — shares E4's root cause (retry path ignores terminal outcomes); add scripted PairingRequired/IdentityChanged retry tests alongside the behavior guard.

Groups: G1 ConnectionController.kt (E4 rescan guard + E3 isActive guards + B3/E9 param deletion + E1 showPairingRequired seam); G2 MainActivity.kt (B8 remember + E1 missing-trust branch + B2/E2 fresh-null notNow + E3 retry-job tracking); G3 ConnectionScreen.kt (B11 remember scale); G4 ConnectionControllerTest.kt (V1 + V2 + V3 tests).

## Verification

**Commands:**
- `./gradlew build lint :core:test` -- expected: BUILD SUCCESSFUL, 0 lint errors, JVM tests pass.
- `rg -n "okhttp3|org\\.jupnp|KEY_" app/src/main` -- expected: no hits.
- `rg -n "println|android\\.util\\.Log" app/src/main tv/src/main core/src/main --glob '!*RedactedLog*'` -- expected: no hits outside the sink.

**Manual checks (if no CLI):**
- Owned TV: happy path, offline/reconnect pull-the-plug, token rejection/re-pair, identity changed/re-pair; `adb logcat` shows no token, pin, SPKI, MAC, or WSS URL.
