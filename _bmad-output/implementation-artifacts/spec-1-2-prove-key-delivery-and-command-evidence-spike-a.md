---
title: 'Spike A: prove key delivery and command evidence'
type: 'feature'
created: '2026-10-23'
status: 'done'
route: 'dispatch'
baseline_commit: 'c8c61b12e9e6aef22674dce66bfdcce219fd3f8c'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/unirem/spec-samsung-remote/protocol-reference.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Epic 1 promises honest Submitted/Unconfirmed/Observed key states and a defensible timeout, but nothing has proven the MVP key path, repeat behavior, or the evidence model on real Tizen hardware. Invented timeouts and silent socket-write "success" would ship lies.

**Approach:** Extend the Story 1.1 throwaway harness in place with a long-lived key session: it sends each MVP key as one `ms.remote.control` frame over the pinned, token-bearing channel, correlates machine events, state probes, and operator confirmations per key, measures submit-to-evidence latency, repeats held directions through one engine, and resolves every gesture against a generation so late evidence can never rewrite a newer press. Findings land in `spike-a-keys.md`, filled from Results rows only.

## Boundaries & Constraints

**Always:**
- One gesture submits exactly one `ms.remote.control` command with the correct `DataOfCmd`; repeat frames and synthesized press/release never submit. Socket-write success is never a TV action.
- Evidence per key is a machine event, a state probe, or the operator's confirmation only. Silence is not failure; unobserved stays unconfirmed, never "failed", never green.
- Exactly one repeat engine; only D-pad directions repeat (500 ms delay, ~80 ms cadence, Release on lift). No queue, no silent retry, no replay after reconnect.
- The story closes with harness plus report, every row `blocked` until recorded runs exist. AC1 needs the Story 1.1 pairings; without them the gate stays shut. A green build is not a pass; a hard failure recommends a re-plan.

**Never:** production remote UI, key-state design beyond the evidence model, text, pointer, voice, cast, mirror, multi-TV, Settings; diagonal keys or swipe navigation; a power toggle — power off is a distinct key; trust-all managers, hostname relaxation outside the pinned client, or WSS-to-WS fallback; token, pin, SPKI, or full MAC in logs, results, or backup.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Paired setup | 1.1-paired TV, phone, same LAN | Key session opens on the pinned token channel; model, firmware, network, on/reachable recorded | No pairing or unreachable TV records `blocked`, never a key row |
| MVP key | One gesture per key (D-pad, OK, Back, Home, Apps, Search, VolUp/VolDown/Mute, Play/Pause, Power) | Exactly one frame, correct `DataOfCmd`, on-TV effect recorded | Wrong frame or duplicate submit is a `fail` row |
| Evidence | TV responds or stays silent | Source recorded: machine event, state probe, or operator confirmation; write never counts as action | Unsupported keys stay usable, recorded for the one-time chip, never "failed" |
| Latency | Repeated presses per TV | Median and tail per key group; 3 s default kept or per-capability replacement recommended from runs, never one sample | Too few samples records `blocked`, not a recommendation |
| Repeat | Direction held past 500 ms | Repeats ~80 ms until release; Release frame sent; TV effect and dropped/reordered frames recorded | Second engine or synthesized repeat path is a `fail` row |
| Late evidence | Older press evidence arrives after newer press starts | Older evidence ignored; newer gesture keeps its state; open gesture resolves Idle on socket close | Stale evidence turning a newer press green is a `fail` row |
| Socket closes | Close mid-press or between presses | Repeat stops, gesture Idle, keys disabled, no queue or retry; best-effort Release possibility recorded | Any silent command after close is a `fail` row |
| Power off | TV on, power key sent | Distinct power key; TV recorded off, asleep, or unknown; unobserved stays unconfirmed, no confirmation sheet | Toggle behavior or marked-success-without-observation is a `fail` row |

</frozen-after-approval>

## Code Map

Story 1.1 harness exists at `tools/spike-a/` (standalone build, Gradle 9.5.0 / AGP 9.3.1 / JDK 17 / Kotlin 2.4.21). Shorthand `.../` means `tools/spike-a/app/src/main/kotlin/com/unirem/spikea/`.

- `.../Channel.kt:31-102` -- one-shot connect only (`connect`, latch, close); sends nothing. Do not retrofit: add a sibling session instead.
- `.../logic/ChannelUrl.kt:17-28` -- reuse wss URL + token attach for the key channel.
- `.../logic/ChannelEvent.kt:8-23` -- reuse `ChannelEvents.parse` for server acks and machine events.
- `.../logic/Backoff.kt:11-17` -- reuse visible delay math for repeat cadence; repeat is not reconnect backoff, keep them separate.
- `.../logic/Results.kt:9-56` -- reuse `Scenario`, `ResultRow`, `ResultsFormat`, `GateRules`; new scenarios go here, key/latency evidence rides the `checks` map (no schema change: there is deliberately no secret field).
- `.../logic/ApiInfo.kt:23-57` -- reuse pure identity mapping for per-TV rows.
- `.../Discovery.kt:69-105`, `.../Vault.kt:43-54` -- reuse target scan and token/pin lookup; the key session takes an already-paired target.
- `.../Harness.kt:91-147` (scan), `:834-840` (`record`), `:908-911` (redacted log) -- extend with `runKey`-style methods following the `pair` pattern; one press is one manual run.
- `.../SpikeAScreen.kt:150-170,202-223` -- reuse `addButtonRow`; per-key buttons plus Observed / Not-observed marking fit the existing pattern.
- `_bmad-output/implementation-artifacts/spike-a-keys.md` -- new report, same contract as `spike-a-pairing.md`: filled from Results rows only, verdict `blocked` until runs exist.
- Reuse `protocol-reference.md` for Click/Press/Release frames, the `KEY_*` names, and channel events; the power list there settles the distinct-power-key question.

## Tasks & Acceptance

**Execution:**
- [x] `.../logic/KeyFrame.kt` + unit test -- Click/Press/Release builders plus the MVP `DataOfCmd` table -- one gesture maps to exactly one frame.
- [x] `.../KeySession.kt` -- verify pin, attach token, hold the socket open, send frames, correlate ack/timeout, close honestly -- the long-lived sibling `Channel` never became.
- [x] `.../logic/RepeatEngine.kt` + unit test -- 500 ms delay, ~80 ms cadence, single engine, D-pad only, Release on lift -- AC5 without a second repeat path.
- [x] `.../logic/GestureTracker.kt` + unit test -- per-gesture generation, late evidence ignored, Idle on close, stale evidence can never confirm -- AC6 as logic.
- [x] `.../logic/LatencyStats.kt` + unit test -- median and tail per key group, sample-count floor, timeout recommendation -- AC4 from runs, not samples of one.
- [x] `.../logic/Results.kt` -- new `Scenario` values (key, repeat, latency, gesture, power-off, unsupported-key); key code, `DataOfCmd`, latency, and evidence source ride `checks` -- evidence schema without touching the redacted row shape.
- [x] `.../Harness.kt` -- key-run methods (send key, repeat hold, power off, unsupported-key record) following the `pair` one-press pattern; socket close stops repeat, disables keys, records Release possibility -- AC2, AC3, AC7-AC9.
- [x] `.../SpikeAScreen.kt` -- per-key buttons, Observed / Not-observed marking, latency readout -- operator confirmation as a first-class evidence source.
- [x] `.../Results.kt` (app) -- JSONL append path already handles new scenarios; extend only if a scenario needs it.
- [x] `_bmad-output/implementation-artifacts/spike-a-keys.md` -- report skeleton: matrix rows, per-key evidence, latency, repeat, power-off, unsupported keys, timeout recommendation, verdict `blocked` -- AC10.
- [x] `.../logic/*Test.kt` -- one covering test per I/O matrix row; hardware-only rows (real Allow, real prompt, real re-prompt) stay `blocked` in the report and no test stands in for them.
- [x] `debug/` -- publish the rebuilt sideloadable beside `spike-a-debug.apk` only if the screen changed enough to matter on device.

**Acceptance Criteria:**
- Given 1.1-paired TVs and a phone on one LAN, when the key harness runs, then each MVP key produces exactly one frame with the correct `DataOfCmd`, and a missing pairing blocks the gate rather than passing it.
- Given completed runs, when the report is written, then per-key evidence cites machine event, probe, or operator confirmation, latency states median and tail with the timeout recommendation, and no socket write is recorded as a TV action.
- Given the finished harness, when storage, logs, and TLS configuration are inspected, then no token, pin, SPKI, or full MAC is logged or backed up, and exactly one repeat engine exists.

## Implementation Notes

Build session 2026-10-23 (dispatch route; subagent implementation, verified against the diff, not its report).

- Verified by me, not trusted: `:app:cleanTestDebugUnitTest :app:testDebugUnitTest` re-executed — 99 tests, 0 failures/errors/skipped across 14 classes (39 new: KeyFrame 9, RepeatEngine 7, GestureTracker 7, LatencyStats 6, KeyMatrix 10). `:app:assembleDebug` BUILD SUCCESSFUL (1.68 MB APK). `:app:lintDebug` 0 errors, 9 warnings (down from 16; the 7 SetTextI18n are suppressed file-wide in the new screen).
- Matrix audit: `KeyMatrixTest` covers all 8 I/O rows (paired setup, one-frame-per-gesture, write-never-action, latency median/tail/floor, single-engine D-pad-only repeat, late-evidence generations, socket-close Idle, power-off probe) plus scenario serialization/filter and no-secret checks; all ran fresh after clean and passed. Hardware-only rows (real TV effect, real re-prompt, real pin stability across restart) stay `blocked` in `spike-a-keys.md`; no test stands in for them.
- Read the new code: `KeySession` fails closed on blank pin before token attach, validates IP, forwards machine events only while held; `send()` reports socket acceptance only. `Harness.sendKey` records `blocked` on write failure with no queue; close path records the Release as possibility (`release-sent`/`release-unknown`/`no-hold`), never fact. Machine events never auto-confirm — operator binds the exact generation via Observed: machine/probe; `GestureTracker.observe` enforces generation match and terminal Observed. `PowerProbe` treats still-answering as `blocked`.
- Provisional codes flagged honestly: `KEY_CONTENTS`/`KEY_SEARCH` are marked provisional in `KeyFrame.kt` with the unsupported-key fallback; wrong code yields an `unsupported-key` row, never a fail. PLAY and PAUSE are separate discrete keys, matching the story's MVP list.
- Policy: no `println`/`Log`, no trust-all, no `ws://` in new code; new `checks` keys unit-tested against the redaction filter; pin revalidated before token attach on `openKeys`; root Gradle graph untouched.
- Gate stays shut by design: no Tizen hardware here, `spike-a-keys.md` verdict `blocked`, AC1 additionally gated on the Story 1.1 pairings.

Review pass 2026-10-23 (step-04): three layers filed 42 findings; all verified against code and logged in `## Review Triage Log`. 10 patch groups applied, no intent_gap/bad_spec, no loopback: `KeyTransport` seam plus `KeyOrchestrator` (gestures, repeat, latency, target snapshots, enabled flag) with `KeyOrchestratorTest` (15) and `KeySessionTest` (4); `KeySession.open` refuses re-entrant open and ignores post-timeout connects; repeat ticks capped at 4 per tick with break-on-failure; press-time target snapshots feed confirm/probe/close rows; phone-offline probe records `blocked` inconclusive; ambiguous confirms refuse per the report; `releasePossibility` unified; key buttons gated on `keysEnabled`; resolved gesture records pruned on close; report gained the phone-context line and the tracked APK path. Post-patch verification by me: 118 JVM tests pass (19 new), assemble BUILD SUCCESSFUL (fresh APK republished to `debug/`), lint 0 errors/9 warnings unchanged.

## Spec Change Log

## Review Triage Log

Second review pass, 2026-10-23, diff `spike-a2-review.diff` (112.5 kB, 17 files). Forced `:app:compileDebugKotlin --rerun-tasks` BUILD SUCCESSFUL during triage. Every finding verified against the cited code; verdicts below. Groups: G-SEAM harness/session test seams, G-SESSION session lifecycle guards, G-BURST repeat burst, G-ATTR press-time target, G-PROBE offline probe, G-AMBIG ambiguous confirm, G-SCHEMA check keys, G-UI button gating, G-MEM record pruning, G-DOCS report lines.

- B1 `record` shadowing breaks the build: refuted — forced full recompile of main sources is BUILD SUCCESSFUL with zero errors; Kotlin resolves the call to the member function — verdict: false.
- B2 `powerOff` sleeps 5 s on the run executor: true, but the wait is logged, one press runs at a time, and async restructuring would break the one-press pattern — verdict: low, rejected (announced wait, manual tool).
- B3 repeat bursts uncapped after a stalled tick: true (`repeatTick` loops full `due`) — verdict: low, patch, group G-BURST.
- B4 gesture records grow unbounded: true (no eviction; `closeAll` parks but never removes) — verdict: low, patch, group G-MEM.
- B5 `sendTimes` leak on not-observed/hold-stop/power paths: retention is required — removal would fall into the `?: now` 0 ms trap; entries clear on confirm/close — verdict: false (proposed fix would create a worse bug).
- B6 torn `last*` volatile snapshot: true as a code fact, display-only on a manual screen — verdict: low, rejected (no demonstrated harm).
- B7 `open()` while held leaks the prior socket: true, no re-entrancy guard — verdict: low, patch, group G-SESSION.
- B8 `closeKeysInternal` sends without `isOpen` check: refuted — `KeySession.send` returns false unless held, and the false branch records `release-unknown` honestly — verdict: false.
- B9 `releaseSent` vs `releasePossibility` split schema: true, two names for the release outcome across hold/close/power rows — verdict: low, patch, group G-SCHEMA.
- B10 buttons gated only on `!running`: true (SpikeAScreen.kt:135) — disabled-session taps spam `blocked` rows — verdict: low, patch, group G-UI.
- B11 no Harness-level tests: true (14 test files, all `logic/`) — verdict: medium, patch, group G-SEAM.
- B12 report table/phone-row mapping plus wrong APK path (`tools/spike-a/debug/` vs published `debug/`): true on both — verdict: low, patch, group G-DOCS.
- E1 mid-burst send failure continues the loop (`return@repeat` skips to next iteration): true — verdict: low, patch, group G-BURST.
- E2 stalled scheduler yields huge `due` burst: true (fixed-delay still stalls under CPU sleep) — verdict: low, patch, group G-BURST.
- E3 late `connect` after timeout sets `held` with no stored socket: true (CAS succeeds post-timeout; `send` stays null-safe so harm is a lying `isOpen` plus forwarded events) — verdict: low, patch, group G-SESSION.
- E4 open-while-held leak: same root as B7 — verdict: low, patch, group G-SESSION.
- E5 missing `sendTimes` records 0 ms PASS: refuted — adds and removes are paired on every path (send/confirm/close), no path confirms without a timestamp — verdict: false.
- E6 probe/confirm rows use press-time `active`, not press-time target: true — cycling TVs between press and probe attributes TV-B evidence to TV-A's gesture (1.1's cycle guard covers mid-run only, not between presses) — verdict: medium, patch, group G-ATTR.
- E7 holdStop orphans the press gesture: refuted — `holdStart` sets `lastGestureId`, so the press stays confirmable; the repeat row honestly awaits it — verdict: false.
- E8 rapid second tap orphans the first: true mechanism, but harm is closed by refusing ambiguous confirms (E13) plus the documented tap-then-mark workflow — verdict: low, patch via group G-AMBIG.
- E9 `KeyFrame.parse` accepts blank `Cmd`: true, but `parse` has no production consumer (tests only) — verdict: false.
- E10 phone-offline probe records power PASS: true — `readManual`'s `LanDenied` collapses into `apiReachable=false`, indistinguishable from TV-off — verdict: medium, patch, group G-PROBE.
- E11 blank `frameJson` sent: refuted — all six call sites pass `KeyFrame` builder outputs, non-blank by construction; a guard would be dead code — verdict: false.
- E12 unbounded observed records: same root as B4 — verdict: low, patch, group G-MEM.
- E13 report promises refusal on ambiguity, code confirms last-wins: true mismatch (report lines 183-187 vs `confirmLastGesture`) — verdict: low, patch code to match the report, group G-AMBIG.
- E14 hold frames contradict one-gesture-one-frame: refuted — the frozen matrix distinguishes tap (one Click) from hold (Press + repeats + Release), and `KeyFrame` documents it — verdict: false.
- V1 frame-chooser wiring untested (pre-verified; zero test references to `sendKey`/`holdStart`/`repeatTick`): a tap-as-Press or missing Release would ship with all tests green — verdict: medium, patch, group G-SEAM.
- V2 `KeySession` held-gating untested (pre-verified; zero references): post-close events or token-before-pin would ship undetected — verdict: medium, patch, group G-SEAM.
- V3 orchestration composition untested (pre-verified; close/confirm/probe wiring only composed in `Harness`): repeats-after-close or always-PASS probe would ship undetected — verdict: medium, patch, group G-SEAM.

## Design Notes

The key session is a sibling of `Channel`, not a retrofit: `Channel` proved the trust path one-shot, and its latch-then-close shape is wrong for a held socket. `KeySession` reuses `PinnedTls`, `ChannelUrl`, `ChannelEvents`, and `Vault` unchanged. `ResultRow` needs no new columns — `checks` already carries redacted evidence, and the report renders key code, latency, and source from there. Power evidence is a state probe (`/api/v2/` reachability plus 8001/8002 answers after the key), never a sheet, never a guess.

## Verification

**Commands:**
- `tools\spike-a\gradlew.bat -p tools/spike-a :app:testDebugUnitTest` -- expected: all tests pass, including the new key, repeat, gesture, latency, and frame-builder classes.
- `tools\spike-a\gradlew.bat -p tools/spike-a :app:assembleDebug` -- expected: BUILD SUCCESSFUL.
- `tools\spike-a\gradlew.bat -p tools/spike-a :app:lintDebug` -- expected: 0 errors; warnings at or below the 16 already recorded in `spike-a-pairing.md`.

**Manual checks (if no CLI):**
- Same environment as Story 1.1 (`JAVA_HOME`/`ANDROID_HOME` exports, `local.properties` carries `sdk.dir`). Confirm on the build machine before the first TV run.
- Watch `adb logcat` through a full key run for any token, pin, SPKI, or full MAC.
- On device: hold a D-pad key, confirm ~80 ms repeat and a Release on lift; pull the JSONL and confirm one row per gesture.
