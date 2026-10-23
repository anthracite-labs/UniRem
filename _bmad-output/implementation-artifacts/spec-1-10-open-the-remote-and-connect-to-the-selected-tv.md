---
title: 'Open the Remote and connect to the selected TV'
type: 'feature'
created: '2026-10-23'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '6ca887580c01618abd585588f744251c8e05e50a'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The paired TV card still opens a "Check connection" status surface instead of a real Remote, so the pilot user cannot see which TV they control or recover the connection before sending keys.

**Approach:** Ship the Remote shell for the active `TvId` only: one UI-scoped session reusing Story 1.7's connection/backoff/banner, an Idle/Offline ring plus state line, and an "Open remote" card entry — with the status surface removed and no keys rendered yet.

## Boundaries & Constraints

**Always:** Remote opens only for the active `TvId` from `TargetSession`; no target or unreadable record never opens and keeps the Story 1.9 no-TV / "TV unavailable — Pair again" path; app launch and paired summary never auto-connect — opening the Remote starts the first connection, and "Paired" is never "Connected"; exactly one remote session per `TvId` reusing Story 1.7 connection, pin-first validation, backoff, `ConnectionHealth`, re-pair and no-queue behavior; session scoped to Remote foreground lifetime — leave, background, target clear, Forget, or target change closes socket and stops backoff with no background reconnect; recomposition/config change creates no second channel; return relaunch starts fresh with revalidated pin and no cached health; session-only target uses in-memory trust with no Room record, no Keystore alias, no switcher caret, no details action, no durable label; switcher and trust sheets over the Remote never close the session; ring is Idle/Offline only (`ring-idle` no glow vs `ring-offline` salmon glow, static under reduced motion), never spinner/checkmark/sweep/percentage/text; state line is 24dp-minimum centered 6dp-lamp + 13sp/500 "Idle"/"Offline", polite live region announcing once per change, never health copy; topbar dot reports socket health, ring reports command state, never swapped; banner reuses Story 1.7 oxblood style with one recovery action, assertive only for terminal offline and identity changed; one banner at a time, content pushed not overlaid; `RedactedLog` only; `:app` never imports `okhttp3`/`org.jupnp`/`KEY_*`/frame types.

**Never:** D-pad/directions/OK, function or utility rows, volume/play-pause/power, key frames, `CommandTracker`/`submit()`, repeat/gesture generation, Submitted/Unconfirmed/Observed states, chips, text/pointer/voice/cast/mirror/wake, Settings gear, launcher enablement, multi-TV/Add TV, full TV details label list, trust/Forget/matrix changes, session chip, foreground service/notification/wake lock, command queue, second connection/health/target owner, WSS-to-WS fallback, trust-all manager, cloud/telemetry, dead "Check connection" route.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Open remote | Card "Open remote" tapped, active `TvId` | Remote opens, first connection starts; card never opens socket | N/A |
| No target / unreadable | `NoTarget` or unreadable record | Remote does not open; no-TV setup or "TV unavailable"/"Pair again" | Quiet, redacted |
| Leave / background | Back, target clear, Forget, app background | Session closes, backoff stops, no socket outlives Remote | N/A |
| Recomposition / rotate | Recompose or config change mid-session | Still one session, one channel, no cross-target socket | N/A |
| First attempt fails | Initial connect fails | Amber dot + "connecting" label, no banner/spinner; then reconnecting banner | Backoff per Story 1.7 |
| Terminal offline | Backoff exhausted | "TV didn't answer — retry?" + Retry/Not now; keys off | Retry = stored IP first, one same-`TvId` scan only |
| Identity / pairing | Pin mismatch / token rejected | "identity changed — Re-pair" / "Pairing required — Re-pair"; failed re-pair keeps old record | Old channel closed before re-pair, no overlap |
| LAN denied | Gate denied | "UniRem needs local network access…" + Open settings/Not now; no socket | Never "TV didn't answer" |
| Forget while open | Forget from details over Remote | Session closes without command, returns to no-TV setup | No stale Remote/sheet |
| Session-only | No resolvable identity | Same Remote on in-memory trust; name+dot no caret, no sheet/details/label | No persistence |

</frozen-after-approval>

## Code Map

- `app/src/main/kotlin/com/unirem/MainActivity.kt` -- owns `SetupRoute`, `TargetSessionHolder`, `ConnectionHealth`/`ConnectionController`, `startConnectionRun`/`closeConnection`/`LaunchedEffect(connectionTvIdHex)`; REMOVE `SetupRoute.Connection` branch + `connectionTvIdHex` state, ADD Remote route + UI-scoped session bound to active `TvId`.
- `app/src/main/kotlin/com/unirem/ShellScreen.kt` -- paired card: replace `GatePrimaryAction(connection_check)` / `onCheckConnection` with "Open remote" entry; keeps details action, durable label, health dot; owns no socket.
- `app/src/main/kotlin/com/unirem/connection/ConnectionController.kt` + `tv/**/ConnectionSession.kt` + `tv/**/channel/ConnectionHealth.kt` -- REUSE as-is: pin-first connect, backoff (`ConnectionBackoff`), `retry()` stored-IP-then-one-scan, `stop()`/`notNow()`; no second owner, no queue.
- `app/src/main/kotlin/com/unirem/connection/ConnectionScreen.kt` (`ConnectionBanner`, `HealthDot`) + `connection/ConnectionUiRules.kt` (`BannerKind`, `textFor`, `keysEnabled`, `bannerFor`, `isAssertive`) -- REUSE banner + dot vocabulary for Remote; banner pushes content, assertive only offline/identity-changed.
- `app/src/main/kotlin/com/unirem/connection/TargetSession.kt` (`TargetSessionHolder`, `TargetSwitchRules.resolveTargetHex/projectTarget`) -- sole target source; Remote reads only the active `TvId`, never Room directly.
- `app/src/main/kotlin/com/unirem/remote/RemoteScreen.kt` (new) -- Remote shell: 56dp topbar, 360–480dp column, 236dp ring (208dp fallback), 24dp state line, disabled launcher; one session per `TvId`; Idle/Offline mapping only.
- `app/src/main/kotlin/com/unirem/setup/SetupChrome.kt` (`TvSwitcherButton`, `DisabledLauncher`) + `setup/TvSwitcherSheet.kt` -- REUSE switcher button/sheet over Remote (opens no socket, current tap no-op); REUSE trust-sheet entry (`TrustSheet.kt`/`TrustLogic.kt`) read-only over Remote.
- `app/src/main/kotlin/com/unirem/tv/RoomTvStore.kt` + `tv/KeystorePairingStore.kt` -- REUSE `byId`/`all` + `load(TvId)` for session open (`lastIp` + token/pin); session-only bypasses both.
- `app/src/main/kotlin/com/unirem/ui/graphitemint/Tokens.kt` -- REUSE `RingIdle`/`RingOffline`, banner/surface/ink tokens, `MaxColumnWidth` 480dp, `MinTarget` 48dp; no new shadow/icon.
- `app/src/main/res/values/strings.xml` -- ADD "Open remote" + Remote banner/state copy; REUSE gate-denied + reconnect/offline/identity/pairing strings.
- Do not change: `tools/spike-a/**`, `PinnedTlsChannel`, `CapabilityMatrix` resolver/asset, `CapabilityLabelTracker` owner, overlay DAO, gate flow, backup rules, module graph.

## Tasks & Acceptance

**Execution:**
- [x] `ShellScreen.kt` + `MainActivity.kt` -- replace "Check connection" with "Open remote", remove `SetupRoute.Connection` branch and its connection-entry wiring; paired summary keeps details/label/dot, owns no socket -- AC1, AC4.
- [x] `remote/RemoteScreen.kt` (new) + nav -- topbar (56dp, hairline, switcher-or-static-name, empty right), 360–480dp column, 236dp ring lower-third 62–70%, 24dp state line, disabled six-slot launcher; no utility/function/D-pad/chips/nav -- AC2.
- [x] `MainActivity.kt` + `RemoteScreen.kt` -- one UI-scoped session per active `TvId` (open one channel, close on leave/background/clear/Forget/change, no recomposition duplicate, fresh on return, session-only in-memory) -- AC3.
- [x] `RemoteScreen.kt` -- Idle/Offline ring + state line mapping, `ring-idle`/`ring-offline` + salmon glow, polite live region, dot-vs-ring roles kept -- AC4.
- [x] `RemoteScreen.kt` + `ConnectionScreen.kt`/`ConnectionUiRules.kt` -- banner reuse with all copy variants, initial-attempt dot-only, assertive routing, Retry/Not now/Re-pair semantics, LAN-denied branch, single banner -- AC5.
- [x] `MainActivity.kt` + `TvSwitcherSheet.kt` + `TrustSheet.kt` -- sheet/details over Remote without socket or session restart; Forget/unreadable close paths; no keys rendered -- AC6.
- [x] `RemoteScreen.kt` + strings -- 48dp targets, focus order topbar→banner→state→launcher, TalkBack names, non-color cues, 200%/1.3×/RTL wrap, reduced-motion static, launcher only bottom bar -- AC7.
- [x] JVM tests -- entry guards, one-session-per-`TvId`, close paths, no auto-connect, no recomposition duplicate, session-only exclusion, Idle/Offline mapping, no-queue/no-tracker invariant -- AC8.
- [ ] Compose/UI or manual pilot tests -- Open remote, removed status surface, topbar/switcher, empty ring, state line, banners, disabled launcher, focus order, 200%/RTL, close on back/background -- AC8.
- [x] Scope guards -- no keys/tracker/repeat/service/queue/second owner/fallback/trust-all; reuse 1.5/1.6/1.7/1.8/1.9 owners -- AC9.

**Acceptance Criteria:**
- Given a durable or session-only target, when the card is shown, then its primary action is "Open remote" (no socket itself), the status surface and "Check connection" are gone, and leaving the Remote closes its session.
- Given the Remote open, when rendered, then the topbar/column/ring/state-line/launcher match AC2 geometry with no keys, rows, chips, tabs, or scrolling.
- Given health changes, when mapped, then ring + state line show Idle vs Offline only per AC4 and the banner recovers per AC5 with no toast/snackbar/dialog.
- Given zero/forgotten/unreadable/session-only states, when entered, then the Remote never guesses a target and session-only persists nothing.

## Implementation Notes

- Dispatch built the slice; verified against the staged diff (9 files, +1279/-97 vs baseline `6ca8875`), not the report alone. New: `remote/RemoteRules.kt` (pure entry/session/ring rules), `remote/RemoteScreen.kt` (shell, ring, state line, gate banner), `RemoteRulesTest` (7 tests) + `RemoteScopeGuardTest` (6 tests), 5 new strings. Removed: `SetupRoute.Connection` branch, `onCheckConnection` wiring, `connection_check` usage (dead strings retained).
- Verification fixes applied after diff review (code changed, diff rewritten, suites re-run): ring is now a true 2dp stroke with no idle fill (was 10dp + fill discs) and carries no content description (state line is the sole announcer; matches the AC7 focus order and the Story 1.11 no-CD direction); gate-denied banner is a polite live region (was unannounced); Remote topbar switcher text is live `ConnectionUiRules.textFor(connSnapshot.state)` (was hardcoded "not connected" while connected); three always-false null checks removed after compiler proof; guard suite extended with persisted-store exclusion (`Room`/`Keystore`/`DataStore`/`SharedPreferences`) and a no-`remote_ring`-copy assertion.
- Matrix → test mapping: entry/session-only/close/no-auto-connect/no-duplicate/ring/queue rows → `RemoteRulesTest` (ran green); reuse rows (first-attempt dot-only, terminal-offline Retry, identity/pairing, backoff) → Story 1.7 suites (`ConnectionUiRules`, `ConnectionController`, `RePairCommit`), which ran green in the full build; store-exclusion/no-network/no-log/no-command rows → `RemoteScopeGuardTest` (ran green). Compose/UI rows have no harness in repo (established Story 1.9); owned-device pilot still required.
- Verified by execution: `:app:testDebugUnitTest` Remote suites 7/7 + 6/6 green; `build lint :core:test` BUILD SUCCESSFUL, no new warnings; `Select-String 'okhttp3|org\.jupnp|KEY_'` over `app/src/main` clean; no `println`/`android.util.Log` outside `RedactedLog`.
- Deferred: the Compose/UI-or-manual-pilot task stays unchecked — no compose harness, no owned TV here. Manual pilot must cover: card opens Remote + connects; back/background closes socket with no reconnect; Retry/Not now/Re-pair per banner; gate-denied copy; `adb logcat` shows no token, pin, SPKI, MAC, or URL.
- Review pass (1 round, no loopback): 30 findings triaged — 2 medium + 8 low patched, 1 medium-unverified deferred (session-only identity upgrade, logged in `deferred-work.md`), rest false/rejected with evidence. Patch rework: `ringFor(state, banner, lanDenied)` now yields Offline for terminal banners and gate-denied per AC4; `launchRemoteSession` tracks and cancels its trust-load jobs (no post-close or double socket); entry/lifecycle/retry decisions extracted as tested pure rules (`resolveDurableOpen`, `resolveSessionOnlyOpen`, `shouldStartSocket`, `shouldCloseOnTargetChange`, `resolveRetryTarget`, single session-placeholder constructor); gate-denied Not now stays with terminal Offline; Remote sheet uses live health text; state-line heading removed; dead effect/strings/stub helpers deleted. Re-verified: `RemoteRulesTest` 10/10 + `RemoteScopeGuardTest` 6/6 green, `build lint :core:test` BUILD SUCCESSFUL, no banned imports/logs, no stale `connection_check`/`heading()`/`ringFor(state)` references.
- Known risks for review: config change keeps the session in plain `remember` (matches the old Story 1.7 pattern), so rotation drops toward Shell rather than seamlessly retaining the Remote — no second channel either way; session-only reuses a `"session:<ip>"` TvId placeholder purely to satisfy `ConnectionController.run/retry` signatures (no Room/Keystore touch); gate-banner "Not now" closes the Remote to Shell instead of showing terminal Offline; one `DisposableEffect(remoteKey)` block is a documented no-op.

## Spec Change Log

## Review Triage Log

Review pass 2026-10-23, diff `unirem-1-10.diff` (178.4 kB staged; N = min(floor(sqrt(178.4)+1),10) = 10). Three layers filed 16 blind + 11 edge (+2 claims) + 3 verification-gap findings. Every claim verified against the cited file before rendering. No intent_gap, no bad_spec: all survivors are patch or defer.

- B1 empty DisposableEffect: low, patch — `MainActivity.kt:1249` block is a documented no-op; leave/background already owned by `closeRemote`/`pauseRemoteSocket`, so the block is dead weight (group P-C).
- B2 rotation drops Remote: false — `route` itself is plain `remember` (`MainActivity.kt:117`), so rotation resets the whole app to Shell by design; AC3 requires only no second channel, which holds.
- B3 stale connEntry after STOP: false — Remote is backgrounded (invisible) while stale, and `ON_RESUME` reloads entry/loaded fresh; the network-change guard firing mid-background is harmless.
- B4 ON_RESUME/leave-during-load double channel: medium, patch — `launchRemoteSession` fires untracked `scope.launch` blocks (`MainActivity.kt`, durable + session-only branches) that are never assigned to `connRunJob`, so backgrounding during the trust load (or a quick resume) starts a socket after close or runs two channels (group P-A with E11).
- B5 session-only target-change gap: false — no route out of `SetupRoute.Remote` mutates `records` except `runForget` (which closes the Remote) and durable re-pair; a session-only target cannot change mid-remote.
- B6 session:ip placeholder: low, patch — `TvId` is an unvalidated `data class` (`Identity.kt:101`), so no crash, and the placeholder never persists or logs; still bends the never-an-IP invariant, so centralize it in one tested resolver (group P-I with V3).
- B7 session-only blocked when records exist: false — the gate mirrors the 1.9 session-only definition (`sessionTrust.isNotEmpty() && records.isEmpty()`, `MainActivity.kt:842`); blocking is correct by definition.
- B8 gate-denied Not now closes: low, patch — AC5 sends every Not now to terminal Offline while staying; `closeRemote()` on denied deviates (group P-D).
- B9 sheet hardcodes not-connected: low, patch — the Remote sheet instance (`MainActivity.kt:1459`) shows `switcher_not_connected` while the topbar shows live `textFor(state)`; pass the live text (group P-E).
- B10 state-line heading: low, patch — `heading()` on the merged status text adds a wrong role to the polite live region; deletion-safe (group P-F).
- B11 gate banner maxLines/clickable: false — AC5 mandates a two-line maximum, and the boxes match the approved `ConnectionBanner` idiom.
- B12 ring breakpoint: false — the `BoxWithConstraints` incoming maxHeight is screen-bounded (≈740dp on 800dp screens → 236dp; short viewports → 208dp), approximating the spec geometry.
- B13 ring shows Idle for terminal banners/denied: medium, patch — `ringFor` maps `Reconnecting`+`IdentityChanged`/`PairingRequired` banners and gate-denied (`Connecting`) to Idle, but AC4 requires Offline whenever no usable socket remains; the enum framing is wrong (`ConnectionState` has only 4 states) but the mapping gap is real (group P-B).
- B14 vacuous guard test: low, rejected per the cosmetic rule — CI runs at repo root and the suites demonstrably execute (13 tests ran here); fail-fast would add branches for a scenario with no named harm.
- B15 dead connection_check strings: low, patch — unwired since the entry removal (guard asserts this); safe 2-line deletion (group P-C).
- B16 empty logs/spec meta: false — Verification is populated, Change/Triage logs are empty by design pre-review, and statuses match this workflow stage.
- E1 session-only target close: false — same evidence as B5.
- E2 both keys set: false — each opener nulls the other key (`openRemoteDurable`/`openRemoteSessionOnly`), so the state is unreachable.
- E3 blank sessionEntry branch: maybe-false, rejected — no path evicts `sessionTrust` mid-remote, and back recovery exists; low harm if ever reached.
- E4 record deleted mid-load: false — open and load read the same `records` snapshot, and `LaunchedEffect(target)` closes on later deletes.
- E5 re-pair no-op with null record: defer — reachable only through the E9 path (session-only + later-resolvable identity); grouped in D-A.
- E6 denied banner hides recovery: false — the controller snapshot survives denial untouched and `ON_RESUME` restarts the session; nothing is destroyed.
- E7 gate banner truncation: false — same evidence as B11.
- E8 short-window clipping: false as a code defect — no-scroll plus 208dp shrink is the spec shape and 360×640dp arithmetic fits; owned-device pilot confirms.
- E9 session-only later-resolvable identity: defer — `freshIdentityOf` does a live `readManual(ip)`, so a TV whose `/api/v2/` starts answering after a session-only pair would publish `IdentityChanged` against the `session:` placeholder with a dead Re-pair; medium severity unverified, grouped in D-A.
- E10 stale health on resume: false — every `run()` republishes `Connecting`/`None` before any socket work (`ConnectionController.kt:102-104`), so reuse of the holder is not user-visible caching.
- E11 resume without cancel: medium, patch — same defect as B4 (group P-A).
- V1 host entry wrappers unverified: patch — pre-verified; extract `resolveDurableOpen`/`resolveSessionOnlyOpen` pure decisions plus cases (group P-G).
- V2 lifecycle decisions unverified: patch — pre-verified; extract `shouldStartSocket`/`shouldCloseOnTargetChange` plus cases (group P-H).
- V3 retry identity/denied branching unverified: patch — pre-verified; extract `resolveRetryTarget` plus cases and centralize the placeholder (group P-I with B6).

Groups: P-A lifecycle job tracking (B4, E11); P-B ring/banner/denied mapping (B13); P-C dead code (B1, B15); P-D Not-now behavior (B8); P-E sheet text (B9); P-F heading (B10); P-G entry extraction (V1); P-H lifecycle extraction (V2); P-I retry extraction + placeholder (V3, B6); D-A deferred session-only identity upgrade (E5, E9, medium-unverified, settles with an owned-device session-only flow where `/api/v2/` answers post-pair).

## Verification

**Commands:**
- `./gradlew :app:testDebugUnitTest` -- expected: new Remote/session/guard suites pass.
- `./gradlew build lint :core:test` -- expected: BUILD SUCCESSFUL, 0 lint errors.
- `rg -n "okhttp3|org\\.jupnp|KEY_" app/src/main` -- expected: no hits.
- `rg -n "println|android\\.util\\.Log" app/src/main tv/src/main core/src/main --glob '!*RedactedLog*'` -- expected: no hits outside the sink.

**Manual checks (if no CLI):**
- Owned device: card opens Remote and connects; back/background closes socket with no reconnect; Retry/Not now/Re-pair behave per AC5; `adb logcat` shows no token, pin, SPKI, MAC, or URL.
