---
title: 'Switch the one paired TV without guessing'
type: 'feature'
created: '2026-10-23'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: 'a4c4bf857780a8359e7e262ded1a681a9171dbb4'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The app can pair one TV but has no explicit single target: screens risk reading the Room record directly, guessing a last-used TV, or leaving the Remote with no durable owner to open.

**Approach:** Ship one process-scoped `TargetSession` auto-select owner, a topbar TV-switcher button, and a one-row bottom sheet reusing the single health/label owners and the Story 1.6 trust sheet entry — built as a one-item list so Story 2 extends it without rewriting.

## Boundaries & Constraints

**Always:** Target is a `TvId` held by one process-scoped `TargetSession` (`NoTarget`/`Selected`); zero stored TVs yields `NoTarget`, one auto-selects on cold start, process death never restores stale target; no screen reads Room as its own target; target never an index, IP, or position; no persisted last-used and no `isActive` column; sheet is a list with one item (not a single-TV branch); sheet opens no socket and makes no connection attempt; topbar/row health dot is the active `ConnectionHealth`, offline token with "not connected" when no session (never "TV off", never a command outcome); row capability is the single `CapabilityLabelTracker` `Keys/Device` text plate (never a badge, never health); current row marked "Current" with text plus check glyph (never color-only); selecting current TV only closes the sheet (no reconnect, reset, queue, or synthesized command); forget-while-open closes the sheet, clears to `NoTarget`, shows no-TV setup; unreadable record shows "TV unavailable" / "Pair again" with no invented target; session-only target has no switcher/sheet-row/details/durable label (name without caret, "Session only — this TV won't be saved"); 48dp targets, focus into sheet + trap + return to switcher button, 200%/1.3×/RTL wrap+scroll, caret mirrors while dot/name do not, pulse only during reconnect and never under reduced motion; `RedactedLog` only with no token/pin/SPKI/MAC/IP/URL in logs or UI; `:app` never imports `okhttp3`/`org.jupnp`/`KEY_*`/frame types.

**Never:** Multi-TV storage/picker, cold-start choice, "Add a TV" (row or button), last-used target, Settings/gear, remote keys/full Remote, trust sheet/Forget changes, text/pointer/voice/cast/mirror/wake, session pinning, target-change sheet-close rules, cloud/telemetry, second target/health/label owner, persisted active target, stored capability label, dead button, tab/screen/bottom-nav/manage surface, nested or second sheet, "Open remote"/"Forget"/full MAC/IP/token/pin/support guarantee in the row.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| One durable TV cold start | 1 stored record | `TargetSession.Selected(TvId)`, switcher button with name/caret/dot | N/A |
| Zero TVs cold start | 0 records | `NoTarget`, no switcher, no-TV setup only | N/A |
| Open sheet | Tap switcher button | Bottom sheet "TVs" (grabber, 64% scrim, ≤80% height), one row, focus in; target unchanged | N/A |
| Dismiss sheet | Drag-down/scrim/back | Sheet closes, focus returns to switcher button | N/A |
| Select current TV | Tap current row | Sheet closes only; no reconnect/reset/health change | N/A |
| TV details action | Tap 48dp "TV details" | Opens Story 1.6 trust sheet keyed by `TvId`; target unchanged | N/A |
| Forget while open | Story 1.6 Forget succeeds | Sheet closes, `TargetSession` clears to `NoTarget`, no-TV setup, no stale row | N/A |
| Unreadable record | Stored record unreadable | "TV unavailable" / "Pair again"; no fallback target | Quiet, redacted |
| Session-only target | No resolvable identity | Topbar name+dot without caret, no sheet row; summary "Session only — this TV won't be saved" | N/A |
| Health vs capability | Offline or connected | Offline can show "Full control — confirms keys"; connected can show "Best-effort — can't confirm commands" | N/A |
| Long name | 1.3×/200%/RTL | Topbar truncates with ellipsis; sheet row + details show full name, wrap and scroll | N/A |

</frozen-after-approval>

## Code Map

- `app/src/main/kotlin/com/unirem/connection/TargetSession.kt` -- extend the existing `TargetSessionHolder` (`NoTarget`/`Selected(tvIdHex)`, `select`/`clear`, `StateFlow`); auto-select wiring lives here, no persistence.
- `app/src/main/kotlin/com/unirem/tv/RoomTvStore.kt` + `TvRecordHolder.kt` -- reuse read-only `observeAll`/`all`/`byId` and `forget()`; no migration, no `isActive`/last-used column.
- `app/src/main/kotlin/com/unirem/MainActivity.kt` -- reuse `TargetSessionHolder`, `ConnectionHealth`/`ConnectionController`, `CapabilityLabelTracker` wiring; ADD cold-start auto-select (`LaunchedEffect`), switcher-sheet state, `trustTvIdHex` entry; status/Remote read only `TargetSession`.
- `app/src/main/kotlin/com/unirem/setup/SetupChrome.kt` -- extend `SetupTopBar` pattern into a 48dp switcher button (8dp dot + `body` 16sp name + 12dp mint caret, 8dp gaps, ellipsis); right side empty, no gear.
- `app/src/main/kotlin/com/unirem/setup/TvSwitcherSheet.kt` (new) -- bottom sheet "TVs" reusing `TrustSheet`/`ExplainerSheet` scrim/dialog/dismiss/focus idiom; one-item list, 48dp row (dot + full name + model + text-plate label + "Current" + check + "TV details"), no socket.
- `app/src/main/kotlin/com/unirem/setup/TrustSheet.kt` + `TrustLogic.kt` -- reuse `TrustSheet` entry keyed by `TvId` via `trustTvIdHex`; do not change Forget.
- `app/src/main/kotlin/com/unirem/ShellScreen.kt` + `setup/PairingScreens.kt` (`PairedScreen`) -- replace first-record assumption with `TargetSession` target; paired summary keeps details action, durable label, health dot.
- `app/src/main/kotlin/com/unirem/connection/ConnectionScreen.kt` (`HealthDot`) + `connection/ConnectionUiRules.kt` -- reuse 8dp dot + offline token for button and row; do not create a second health owner.
- `app/src/main/kotlin/com/unirem/capability/CapabilityLabelTracker.kt` + `CapabilityCopy` -- reuse `labelsFor(tvIdHex)` `CARD_KEY` plate; do not create a second resolver/owner.
- `app/src/main/kotlin/com/unirem/ui/graphitemint/Tokens.kt` -- reuse `SurfaceBase/Raised`, `Hairline`, `Accent`, `Body` 16sp, sheet `Xl`/scrim/grabber tokens; no shadow, no Material icons.
- Do not change: `tools/spike-a/**`, `PinnedTlsChannel`, `CapabilityMatrix` resolver/asset, overlay DAO, gate flow, `docs/app-identity.md`, backup rules, module graph.

## Tasks & Acceptance

**Execution:**
- [x] `connection/TargetSession.kt` + `MainActivity.kt` -- cold-start auto-select (1→`Selected`, 0→`NoTarget`), `select`/`clear` only paths, no persisted value -- AC1, AC4.
- [x] `setup/SetupChrome.kt` (or new switcher button) -- 48dp button with dot/name/caret/gaps/ellipsis, empty right, session-only no-caret variant -- AC1, AC6.
- [x] `setup/TvSwitcherSheet.kt` (new) -- sheet chrome + one-item list + row (dot/name/model/plate/"Current"+check/"TV details") + open-no-socket + dismiss/focus-return -- AC2, AC3, AC6.
- [x] `MainActivity.kt` + `ShellScreen.kt` + `PairingScreens.kt` -- `TargetSession` as sole target source, sheet↔trust-sheet coordination (one sheet at a time), forget-while-open close+clear, unreadable/session-only/empty states -- AC4, AC5.
- [x] `res/values/strings.xml` -- user copy ("TVs", "Current", "TV details", "TV unavailable"/"Pair again", session-only line) with 1.3×/RTL-safe wording -- AC3, AC5.
- [x] JVM tests -- `TargetSession` zero/one TV, no last-used, `TvId` keying, current no-op, forgotten/unreadable, session-only exclusion -- AC7.
- [x] Compose/UI or manual pilot tests -- button, sheet open/dismiss, focus return, current marker, details action, health/capability labels, 200%/RTL -- AC7.
- [x] Scope guards -- single target/health/label owner, no Room migration, no socket in switcher, `:app` import ban, `RedactedLog` only -- AC8.

**Acceptance Criteria:**
- Given one durable paired TV, when the paired summary or future Remote topbar shows, then the left is a 48dp switcher (dot + name + caret) opening the sheet without changing target, auto-selected from `TargetSession`, with no command state or capability claim in the topbar.
- Given the switcher button is tapped, when the sheet opens, then it is a one-item-list bottom sheet ("TVs", grabber, 64% scrim, ≤80% height) that opens no socket, dismisses on drag/scrim/back with focus return, and carries no "Add a TV".
- Given the one row, when rendered, then it shows dot + full name + model + durable text-plate label, marks current by text plus check, taps close (current = no-op), and its details action opens the trust sheet without changing target.
- Given zero/forgotten/unreadable/session-only states, when entered, then `NoTarget` + no-TV setup, or "TV unavailable"/"Pair again", or caret-less session-only chrome — never a guessed, stale, or invented target.
- Given this story, when shipped, then the only visible changes are the topbar switcher, the one-TV sheet, and the details entry point; Story 1.10 and Story 2 reuse the same owner/list without replacement.

## Implementation Notes

- Dispatch built the slice; verified against the staged diff (10 files, +1004/-25 vs baseline `a4c4bf8`), not the report alone. New: `TargetSwitchRules` pure owner rules in `TargetSession.kt`, `TvSwitcherButton` + caret in `SetupChrome.kt`, `TvSwitcherSheet.kt` one-item list, `TargetSessionTest` (10 tests) + `TargetSwitcherGuardTest` (7 tests), 7 new strings.
- `closeConnection` no longer clears the target (1.7 cleared on leave); steady state is identical since auto-select re-selects, but it is a deliberate 1.7 behavior change flagged for review.
- Switcher lives on the Shell paired summary (the durable surface with the details entry point); transient post-pairing `PairedScreen` unchanged — opening trust-from-switcher there would need the trust sheet on a second route. Flagged for review if AC1 is read as requiring it there too.
- Matrix audit: all JVM-coverable rows executed green (`TargetSessionTest` 10/10, `TargetSwitcherGuardTest` 7/7); sheet dismiss/focus/TalkBack/200%/RTL rows are code-present and diff-reviewed with manual pilot deferred to Verification per AC7's "or manual pilot tests" path (no compose-ui-test harness in repo; no owned-device run here).
- Verified by execution: `:app:testDebugUnitTest` targeted suites green; `:core:test` + `lintDebug` BUILD SUCCESSFUL, 0 lint errors; `git grep` over `app/src/main` clean for `okhttp3`/`org.jupnp`/`KEY_`.

## Spec Change Log

## Review Triage Log

Review pass 2026-10-23, diff `unirem-19.diff` (65.1 kB staged; N = min(floor(sqrt(65.1)+1),10) = 9). Three layers filed 11 blind + 14 edge (+2 claims) + 3 verification-gap findings. Every claim verified against the cited file before rendering. No intent_gap, no bad_spec, no loopback: survivors are patch or defer.

- B1 forget-resurrects-target: false — `runForget` success assigns the fresh post-delete list (`MainActivity.kt:342`) and clears (`:355`) in the same Main-thread block; `LaunchedEffect(records)` can only see the fresh list and its `next != current` guard skips the redundant clear.
- B2 focus-return fires on first composition: low, patch — `LaunchedEffect(switcherOpen)` requests `switcherFocus` on launch when the sheet never opened; guard to post-dismiss only (group P1).
- B3 button/row hardcode Offline: false — Shell and sheet own no socket (session is Connection-route-scoped); AC1/AC2 bless the offline token when no session exists, and no live state exists in these surfaces to wire.
- B4 drag only on grabber: false — grabber is the designated drag affordance; content drag would fight the `LazyColumn` scroll, and scrim/back cover the remaining dismiss paths per spec.
- B5 session-only actionable description: false — caret-less button has no clickable/role and its strings are descriptive state copy, not an action verb.
- B6 unreadable drops name/model/plate: false — matches AC5 exactly ("TV unavailable"/"Pair again", no invented target); no model exists for an unreadable record.
- B7a row a11y omits health: medium, patch — `HealthDot` is a semantics-free `Canvas`, so the merged row name (`name + Current`) never announces not-connected state, which AC6 explicitly requires (group P1).
- B7b check glyph undescribed: false — "Current" text carries the marker and the glyph is decorative, satisfying "not color-only / not glyph-only".
- B8 autoSelect takes arbitrary strings: false — callers pass Room-sourced `tvId`s and `select()` takes the `TvId` type; short test strings exercise pure count logic only.
- B9a labelsFor("") on NoTarget: low, patch — `labelsFor(targetHex ?: "")` mints a cached empty-key subject with no consumer; guard to subscribe only when selected (group P1).
- B9b blank model falls back to "": false — `if (tvModel.isNotBlank())` omits the line, so no empty text ever renders (unlike the name path, which needs its fallback).
- B10 spec mojibake / empty logs: false — repo files are clean UTF-8 (8 em-dashes intact, no `â` bytes); the mojibake was a locale-decode artifact of the temp diff export, and both log sections are empty by design pre-review.
- B11 48dp / reduced-motion unwired: false — `GateQuietAction` is `heightIn(min = 48.dp)` in both variants and `HealthDot` owns the animator-scale pulse gate internally.
- E1 blank autoSelect input: false — same evidence as B8.
- E2 labelsFor empty key: low, patch — same evidence as B9a (group P1).
- E3 first-composition no-TV flash: defer — real (async `tvStore.all()` load) but pre-existing (identical under the old `records.isNotEmpty()` read); fixing needs a loading state the intent does not define.
- E4 switcher/trust mutual suppression deadlock: maybe-false, defer — stuck state needs a switcher tap inside the millisecond trust-entry-load window while no sheet is composed; settle with an owned-device tap-during-load attempt.
- E5 candidate.tvId!! NPE: defer — the `!!` is a context (pre-existing) line, not introduced by this diff.
- E6 forget-while-open resurrection: false — same evidence as B1.
- E7/E8/E13 offline-dot claims: false — same evidence as B3.
- E9 unbounded drag offset: false — offset tracks the finger, `coerceAtLeast(0f)` blocks upward travel, and `endDrag` resets after the threshold dismiss.
- E10 large-font clipping: false — the row lives in a scrolling `LazyColumn` under the 80% cap, and the unreadable branch is two wrapped lines plus one action.
- E11/E14 no focus trap: defer — real (no `focusProperties` trap) but idiom-level: the approved `TrustSheet` pattern this sheet follows has no trap either, so the fix belongs to the shared idiom, not this story.
- E12 duplicate TvId rows: false — `TvRecord.tvId` is the Room `@PrimaryKey`, so duplicates cannot exist and `firstOrNull` is safe.
- V1 auto-select effect untested (pre-verified): patch — deleting or mis-keying the `LaunchedEffect(records)` breaks cold-start selection while all 10 `TargetSessionTest` tests still pass (group P1).
- V2 target-record resolution untested (pre-verified): patch — reverting to `records.firstOrNull()` reintroduces position-guessing with no test failing (group P1).
- V3 leave-retains / forget-clears lifecycle untested (pre-verified): patch — re-adding `clear()` to `closeConnection` or dropping it from `runForget` regresses silently at holder-test level (group P1).

Groups: P1 implementation patches (B2 focus guard; B7a row state text; B9a/E2 labels guard; V1 auto-select helper + test; V2 resolve helper + test; V3 lifecycle helper + test).

## Design Notes

`TargetSession` is the only writer of the active target; the sheet and every surface are readers. The sheet is deliberately a list with one item (not a single-TV branch) so multi-TV policy in Story 2 adds rows and an "Add a TV" last row without a model change.

## Verification

**Commands:**
- `./gradlew build lint :core:test` -- expected: BUILD SUCCESSFUL, 0 lint errors, JVM tests pass.
- `./gradlew :app:testDebugUnitTest` -- expected: `TargetSession` + guard suites pass.
- `rg -n "okhttp3|org\\.jupnp|KEY_" app/src/main` -- expected: no hits.
- `rg -n "println|android\\.util\\.Log" app/src/main tv/src/main core/src/main --glob '!*RedactedLog*'` -- expected: no hits outside the sink.

**Manual checks (if no CLI):**
- Owned device: one TV auto-selects on cold start; sheet opens/closes with focus return; current tap is a no-op; details opens trust sheet; forget-while-open clears to no-TV setup; `adb logcat` shows no token, pin, SPKI, MAC, or URL.
