---
title: 'Put the D-pad in the ring and tell the truth about every tap'
type: 'feature'
created: '2026-10-23'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: 'e681163237be3f02f1608a2f3d8dce237edf2925'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The Story 1.10 Remote shell renders an empty 236dp ring with a disabled launcher, so the pilot user cannot navigate the TV and has no honest signal for whether a key reached the TV.

**Approach:** Compose the 3×3 D-pad inside the existing ring and wire one tap to exactly one `TvControl.submit(RemoteCommand)` through a single socket-bound `CommandTracker` in `:tv`, with ring + state line reporting Submitted / Unconfirmed / Observed / Idle and an "It worked" user-confirmation path.

## Boundaries & Constraints

**Always:** Remote opens only for active `TvId` reusing 1.10 UI-scoped session, `ConnectionHealth`, backoff, banners; D-pad 3×3 (64/76/64 cols, 60/70/60 rows, 4dp gaps) centered in 236dp 2dp unfilled ring; directions 60×56dp `rounded.md` 14dp `surface-key` hairline 15sp chevron, OK 70dp circle `accent` + `ink-on-accent` 15sp/500; pressed scale 0.98/80ms, disabled 45% + disabled semantics, 48dp-min targets; one `submit()` per tap, one `ms.remote.control` Click frame per submit (directions `KEY_*`, OK = Enter); `CommandTracker` sole `CommandOutcome` writer, one `StateFlow`, monotonic `Long` generation per socket session, injected 3s timeout + Spike-A per-capability override; Submitted pale-mint 900ms breath once, Unconfirmed amber + "Waiting for TV…" + 48dp "It worked", Observed mint glow 4s ("TV confirmed" machine / "You confirmed" user), then Idle; first evidence wins, late/stale ignored; close/reconnect/target-change clears to Idle, no queue/replay/retry; `keysEnabled` gates enablement, silent Unconfirmed/Observed stays enabled; ring = command state, dot = socket health; state line polite live region one announcement per change; TalkBack names Up/Down/Left/Right/OK + button role, ring non-focusable no CD; RTL cluster stays physical (Left left, chevrons unmirrored); `RedactedLog` only; `:app` never imports `okhttp3`/`org.jupnp`/`KEY_*`/frame/socket types.

**Never:** Press-and-hold repeat, 500/80ms timers, `Press`/`Release` frames, key-up, held-gesture handling (Story 1.13); utility row, function keys, Back/Home/Apps/Search/volume/mute/play-pause/power (Story 1.12); one-time chips, model-limit chips, durable limitation record (Story 1.15); haptics, sound, reduced-motion setting, compact 208dp ring/D-pad, full 200%/RTL/1.3× audit (Story 1.14); text/pointer/voice/cast/mirror/wake/Settings/multi-TV/switcher changes/launcher enablement/full details/trust-Forget/matrix changes; second tracker/session/health/target owner, second banner, toast/snackbar/dialog, queue/retry/background socket, WSS→WS fallback, trust-all manager, cloud/telemetry, new permission/service/notification; socket write or silence presented as success; `tools/spike-a` import.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Tap enabled key | Usable socket, `keysEnabled` true, tap Up/Down/Left/Right/OK | One `submit()`, one Click frame, tracker `Submitted` + breath + "Sent" | N/A |
| Timeout no evidence | 3s elapse, socket open, generation current | `Unconfirmed`, amber glow, "Waiting for TV…" + "It worked" | Stays tappable, no failure copy |
| Machine evidence | `TvEvent`/`StateProbe` for current generation, pre- or post-timeout | `Observed` machine, "TV confirmed", 4s hold then Idle | Late duplicates ignored, no re-hold |
| User confirm | Tap "It worked" while Unconfirmed | `Observed` `UserConfirmed`, "You confirmed", 4s then Idle, no submit/frame | Button absent outside Unconfirmed, stale tap cannot confirm newer generation |
| Supersede | New tap while any state showing | Fresh `Submitted`, breath + timeout reset, ring belongs to last press | Old generation evidence ignored |
| Socket closes in flight | Close/background/Forget/target change | Generation cleared to `Idle`, ring+line show 1.10 Offline pair, keys disable | No Unconfirmed/Observed by close, no queue |
| Disabled tap | Connecting/Reconnecting/Offline/identity/pairing/LAN-denied | No click action, zero `submit()`, zero frames | 1.10 banner is only recovery surface |
| Long hold | Press-and-hold direction | Same single tap command, no repeat/release | 1.13 owns repeat |
| Recompose/rotate/TalkBack | Config change or a11y activation mid-session | No replay, one submit per user action | N/A |

</frozen-after-approval>

## Code Map

- `core/src/main/kotlin/com/unirem/core/api/Identity.kt` -- REUSE `TvId`/`StableIdentity` hashing; new command types live beside it, never an IP.
- `core/src/main/kotlin/com/unirem/core/api/command/` (new: `RemoteCommand.kt`, `CommandOutcome.kt`, `Evidence.kt`) -- `RemoteCommand` (Up/Down/Left/Right/Enter), `CommandOutcome` (Idle/Submitted/Unconfirmed/Observed), `Evidence` (`TvEvent`/`StateProbe`/`UserConfirmed` + generation); pure, JVM-only.
- `tv/src/main/kotlin/com/unirem/tv/api/TvApi.kt` -- REUSE gateway seams; add `TvControl.submit(RemoteCommand)` port here (declared in `:tv`, no frame leakage).
- `tv/src/main/kotlin/com/unirem/tv/channel/ConnectionHealth.kt` -- REUSE as sole socket-truth; do not add outcome state.
- `tv/src/main/kotlin/com/unirem/tv/internal/PinnedTlsChannel.kt` + `tv/.../internal/ConnectionSession.kt` -- REUSE WSS-only pin-before-token channel; bind tracker lifetime to session, creator + canceller.
- `tv/.../channel/CommandTracker.kt` (new) + `tv/.../internal/KeyFrameWriter.kt` (new) -- tracker: one generation per tap, injected timeout/4s-hold clocks, first-evidence-wins; writer: one Click frame per submit, `:app` never sees `KEY_*`.
- `app/src/main/kotlin/com/unirem/remote/RemoteScreen.kt` -- ADD D-pad cluster + five-state ring/state-line/"It worked" wiring inside existing shell; keep topbar/banner/launcher placement, ring non-focusable.
- `app/src/main/kotlin/com/unirem/remote/RemoteRules.kt` -- EXTEND `ringFor`/enablement mappers for Submitted/Unconfirmed/Observed; keep Idle/Offline + open/socket/retry resolvers unchanged.
- `app/src/main/kotlin/com/unirem/connection/ConnectionUiRules.kt` + `connection/ConnectionScreen.kt` -- REUSE `keysEnabled`/`bannerFor`/`textFor`/banner + dot; combine tracker + health read-only, never swap owners.
- `app/src/main/kotlin/com/unirem/MainActivity.kt` -- REUSE one UI-scoped session per `TvId`, `TargetSessionHolder`, sheets-over-remote; observe tracker flow, no second outcome/timer/confirmed boolean.
- `app/src/main/kotlin/com/unirem/connection/TargetSession.kt` -- REUSE sole target source; no persisted last-used, no multi-TV.
- `app/src/main/kotlin/com/unirem/ui/graphitemint/Tokens.kt` -- REUSE `surface-key(-pressed)`, `accent(-pressed)`, `ink-disabled`, ring Idle/Submitted/Unconfirmed/Observed/Offline, hairline; no new shadow/gradient.
- `app/src/main/res/values/strings.xml` -- ADD "Sent"/"Waiting for TV…"/"TV confirmed"/"You confirmed"/"It worked" + five D-pad CDs; REUSE 1.10 remote/banner copy.
- Do not change: `tools/spike-a/**`, `PinnedTlsChannel` pinning, `CapabilityMatrix` resolver/asset, `CapabilityLabelTracker` owner, Room/Keystore schemas, gate flow, backup rules, module graph.

## Tasks & Acceptance

**Execution:**
- [x] `core/.../api/command/` (new) -- add `RemoteCommand`/`CommandOutcome`/`Evidence` pure types -- AC2, AC4, AC6.
- [x] `tv/.../api/TvApi.kt` + `tv/.../channel/CommandTracker.kt` + `tv/.../internal/KeyFrameWriter.kt` (new) -- `TvControl.submit`, socket-bound tracker with injected 3s + per-cap override, 4s Observed hold, monotonic generation, one Click per submit, first-wins + stale-ignore + close-to-Idle -- AC2, AC3, AC4, AC6.
- [x] `app/.../remote/RemoteRules.kt` -- extend ring/enablement/state-line mappers to five states, combine with `keysEnabled` -- AC3, AC5, AC7.
- [x] `app/.../remote/RemoteScreen.kt` + `MainActivity.kt` -- D-pad cluster in ring, five-state ring + state line + "It worked", one-flow observation, session-bound tracker lifecycle, no replay on recompose/rotate -- AC1, AC2, AC5.
- [x] `app/.../connection/*` reuse -- `keysEnabled` gating, 1.10 banners unchanged, dot-vs-ring roles kept, Offline bridging only -- AC5.
- [x] `Tokens.kt` + `strings.xml` -- pressed/disabled/ring/state tokens + new copy, no icon/emoji/text-as-icon -- AC1, AC7.
- [x] JVM tests -- one submit + one frame per tap, zero when disabled, OK→Enter, Submitted-after-write, 3s→Unconfirmed, TvEvent/Probe→Observed, It-worked→UserConfirmed no-submit, 4s→Idle, supersede, stale-ignore, close→Idle, new-session→Idle, no queue/retry -- AC8.
- [ ] Compose/UI or manual pilot tests -- geometry/tokens, pressed/disabled, ring colors + labels, expansion, offline banner + disabled keys, TalkBack/roles/live region, row-major Up/Left/OK/Right/Down order, RTL non-mirror, 200% no-scroll -- AC8.
- [x] Scope guards -- no repeat/Press/Release, no 1.12/1.14/1.15 keys/features, no second tracker/queue/fallback/trust-all, no banned imports/logs -- AC9.

**Acceptance Criteria:**
- Given the Remote open on the active target, when the D-pad renders, then the 3×3 geometry/tokens/pressed/disabled/ring-placement match AC1 with no diagonals, swipe, placeholders, or added rows.
- Given an enabled tap, when sent, then exactly one `submit()` yields one Click frame and the tracker/ring/state-line truthfully report Submitted → Unconfirmed (3s) → Observed (4s) → Idle per AC3–AC4 with first-evidence-wins and supersede.
- Given socket/health changes, when gated, then keys enable/disable per AC5, close clears to Idle with no queue, and reconnect starts at Idle.
- Given TalkBack/RTL/200%, when used, then names/roles/live-region/focus-order/non-mirroring/wrapping meet AC7 with color never sole carrier.

## Implementation Notes

- Dispatch built the full slice and verified by execution. `core/api/command/` holds `RemoteCommand` (Up/Down/Left/Right/Enter), `CommandOutcome` (Idle/Submitted/Unconfirmed/Observed, no `confirmed` boolean), and `Evidence` (TvEvent/StateProbe/UserConfirmed + generation), all JVM-only. `:tv` gained `TvControl` (api seam) + internal `CommandTracker` (sole outcome writer: monotonic generation, injected 3s default + per-capability `timeoutFor` override, 4s Observed hold, first-evidence-wins, stale-ignore, close-to-Idle, `ms.`-prefixed channel frames only) + internal `KeyFrameWriter` (Click/`SendRemoteKey`, Up→KEY_UP … OK→KEY_ENTER, never Press/Release). `ConnectionSession` gained `sendFrame` (immediate write, `pendingWrites` stays 0) and a `setMessageListener` inbound hook (opaque strings only, never logged); `TvGateways.commandControl` binds tracker lifetime to the session.
- `:app` gained the 3x3 cluster (64/76/64 cols, 60/70/60 rows, 4dp gaps; 60x56dp `rounded.md` direction keys, 70dp circle OK, 236dp fixed ring) with row-major Up/Left/OK/Right/Down order, forced-LTR physical layout, 0.98/80ms press, 45% disabled, five-state ring + state line + Unconfirmed-only "It worked"; `MainActivity` owns one tracker per `remoteKey`, observes its flow, and clears it on pause/close/forget/target-change/terminal-banner/network-change. `RemoteScopeGuardTest` now pins the 1.11 boundary (D-pad + outcome wiring required; Press/Release/utility keys/second tracker still banned).
- `./gradlew build lint :core:test` green: BUILD SUCCESSFUL, 0 lint errors (5 pre-existing warnings, none on new code). JVM: core 63, tv 80, app 187 -- 330 total, 0 failures. Scope sweeps: no `okhttp3`/`org.jupnp`/`KEY_` in `app/src/main`; no `println`/`android.util.Log` outside `RedactedLog`.
- Not run: owned-device pilot (tap each key, "It worked" timing, hold-sends-once, background close, 200%/RTL audit is Story 1.14's). Two pre-existing `MainActivity` smart-cast warnings in untouched 1.10 retry code left alone.

## Spec Change Log

## Review Triage Log

Review pass 2026-10-23, diff `unirem-1-11-diff.txt` (83.5 kB staged; N = min(floor(sqrt(83.5)+1),10) = 10). Three layers filed 12 blind + 5 edge (4 guards + 1 claim) + 4 verification-gap findings. Every claim verified against the cited file before rendering. No intent_gap, no bad_spec: the spec is clear and all survivors are patch or defer.

- B1 compact 208dp removed: false — frozen AC1 mandates fixed 236dp with quiet-space collapse and assigns compact to 1.14; removal is spec-directed, not a defect.
- B2 ring/outcome mismatch render: false — `RemoteScreen.kt:101,156` derive both from the same `outcome` in one composition; no stale pair is constructible.
- B3 Idle during Connecting with keys disabled: false — Idle means no outstanding command (true); socket state stays on the topbar dot per 1.10 design and spec AC5.
- B4 stale-socket capture in send lambda: false — tracker and session share `remember(remoteKey)` lifetime (`MainActivity.kt:256-264`); dispose cancels scope and closes tracker, so in-flight writes target the session that was current.
- B5 Submitted kept on failed write: false — Submitted is pale-mint not-success by spec vocabulary; `dpadEnabled` gating makes failure race-only and the timeout still resolves honestly to Unconfirmed; `missedSocketStillSubmitsWithNoQueue` pins the specified behavior.
- B6 any-ms.* counts as evidence: false — generation-scoping is the spec's correlation (AC4, first-wins + stale-ignore tested); the channel carries no key-level signal for an allowlist.
- B7 close without generation bump: medium, patch — a submit with its write in flight past a concurrent `close()` re-arms its timeout after the clear (close never invalidates `generation`), reviving zombie Unconfirmed with a live It Worked on a dead session (group P-C).
- B8 double parse + sync listener before latch: low, patch — `ConnectionSession.kt:110,112` parses twice; listener-before-latch order is harmless but the repeat is waste sharing E4's root cause (group P-B).
- B9 timeoutFor/StateProbe dead: false — spine fixes only the 3s default plus the injection point and Spike A came back blocked, so default-only wiring is the only faithful option; the seam is unit-tested (`perCapabilityTimeoutOverridesTheDefault`).
- B10 English mapper strings bypass localization: false — `RemoteStateLine` renders exclusively via `stringResource` (`RemoteScreen.kt:615-620`); `outcomeLabel`/`stateLabel` are test-only twins with no UI caller.
- B11 focus indicator/ripple/reduced-motion: low, rejected — touch-first surface with roles + CDs present; ripple split is deliberate (press-scale replaces ripple on keys); focus visuals and motion audit belong to 1.14's full pass, beyond 1.11 tokens.
- B12 no debounce/coalescing: false — 1:1 tap:submit plus supersede is the specified behavior (AC2); debounce would violate it and no-replay is guarded by session-scoped `remember`.
- E1 breath never restarts on supersede: low, patch — `RemoteRing` keys `animateFloatAsState` on the ring enum only, so Submitted→Submitted keeps target 1f and AC3's per-press breath never replays (group P-A with E5).
- E2 timeoutFor throws wedges tracker: false — no throwing caller exists (prod uses the default, tests pass pure lambdas); try/catch would guard undemonstrated state.
- E3 evidence-during-write leaves stray timer: false — `onTimeoutFired` rechecks Submitted-under-lock, so the redundant job is a no-op, never a wrong ring.
- E4 malformed inbound throws in onMessage: medium, patch — `ConnectionSession.kt:110` parses outside the try/catch, so one malformed frame throws on the socket thread and the connect latch depends on the bounded await instead of ignoring the frame (group P-B with B8).
- E5 supersede rearms timeout but breath target stays 1f: low, patch — same defect as E1, same fix (group P-A).
- V1 outbound socket path unwired-verified: patch, pre-verified — no test runs `TvGateways.commandControl` into `ConnectionSession.sendFrame`; deleting the lambda or the call keeps all 330 tests green (group P-D).
- V2 inbound evidence forwarding unwired-verified: patch, pre-verified — no test drives `ConnectionSession.onMessage` into the installed listener or asserts `MainActivity` installs/clears it; deleting either keeps the suite green (group P-E).
- V3 close-on-banner/network/target unwired-verified: patch, pre-verified — `close()` itself is tested but no test covers the six production transitions that must invoke it (group P-F).
- V4 D-pad mapping + It Worked visibility unwired-verified: defer — real gap but the spec explicitly defers Compose/pilot verification to the owned-device pilot and Story 1.14; settles with a Compose test (fake `TvControl`, per-key `onDpad` values, Unconfirmed-only button) there.

Groups: P-A breath restart (E1, E5); P-B guarded single parse (E4, B8); P-C close invalidates generation (B7); P-D outbound wiring tests (V1); P-E forwarding tests (V2); P-F clear-predicate tests (V3); D-A deferred Compose mapping tests (V4).

## Verification

**Commands:**
- `./gradlew :app:testDebugUnitTest` -- expected: new tracker/rules/guard suites pass.
- `./gradlew build lint :core:test` -- expected: BUILD SUCCESSFUL, 0 lint errors.
- `rg -n "okhttp3|org\\.jupnp|KEY_" app/src/main` -- expected: no hits.
- `rg -n "println|android\\.util\\.Log" app/src/main tv/src/main core/src/main --glob '!*RedactedLog*'` -- expected: no hits outside sink.

**Manual checks (if no CLI):**
- Owned device: tap each D-pad key → Submitted breath then Unconfirmed or Observed per TV; "It worked" only in Unconfirmed; hold sends one command; background/close clears to Offline; `adb logcat` shows no token, pin, SPKI, MAC, or URL.
