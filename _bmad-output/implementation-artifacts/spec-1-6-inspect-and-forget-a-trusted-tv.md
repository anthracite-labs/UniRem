---
title: 'Inspect and forget a trusted TV'
type: 'feature'
created: '2026-10-23'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '4571bbce62c9b2a47c63a61343b86ef16b231c35'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The shell persists a paired TV but the pilot cannot see what UniRem stores about it or remove that trust without hunting through settings.

**Approach:** Add a reusable `TvId`-keyed trust sheet ("TV details") reading the stored record and pin with no socket, plus a Forget confirmation deleting the Keystore material and Room record and returning to the no-TV state.

## Boundaries & Constraints

**Always:**
- Durable-only sheet via "TV details" on the paired summary; reads Room record plus `PairingStore` entry, never opens a socket (works TV-off); dismisses on back, scrim tap, or drag-down with focus back on "TV details"; keyed by `TvId` for Story 1.9 reuse; session-only targets get no sheet and show "Session only — this TV won't be saved."
- Shows name, model, Tizen version, `maskMac` output (at most last two octets), last-reached IP labeled "Last reached IP", and the SPKI fingerprint as SHA-256 of the stored pin in `code` type: space-separated uppercase byte pairs (e.g. `AB CD EF`), fully visible, selectable, wrapping or scrolling; never a live probe and no claim the TV shows the same string; missing or unreadable pin shows "Fingerprint unavailable" with Forget still available; never shows full MAC, token, pin, or capability label.
- Shows "Paired" as the durable state (never "Connected" or live health), makes no online claim, and shows exactly "Pairing is stored encrypted on this phone; nothing is sent to the internet."; nothing critical truncates.
- Forget runs inside the sheet (no nested sheet): "Forget [TV]? You'll need to pair again." with "Forget" in salmon/`ring-offline` outline (never filled red) and "Cancel" secondary; confirm closes any open channel for that TV with no command sent, deletes Keystore alias/token/pin and the Room record plus dependents, clears the active target, and returns to the no-TV state (launcher disabled, "Add your first TV" works, re-pair mints a new record and alias); notes "The TV may still list this phone until you remove it on the TV." and never claims remote removal.
- Cancel or dismiss deletes nothing and keeps the sheet; deletion failure keeps the record and shows "The TV couldn't be forgotten. Try again." / "Try again" / "Close."; forgotten is never claimed until trust material and record are both gone; a missing Keystore alias counts as already deleted; "Try again" is user-action-only.
- `RedactedLog` is the only logger; the fingerprint may display but is never logged; token, pin, SPKI, full MAC, and pairing payloads never appear in logs; no `println`/direct `Log` outside the sink; deletion leaves no token, pin, alias, or full MAC in Room, DataStore, or app-private files; `:app` never imports `okhttp3`, `org.jupnp`, `KEY_*`, or frame types.

**Never:** full TV details (capability labels, last verified, diagnostics), Settings, switcher, remote, reconnect, health dot, matrix, text, pointer, voice, cast, mirror, wake, token/pin display, cloud, trust-all, WSS-to-WS fallback, second trust store, or any test needing a live TV, asserting a socket write as success, or printing secrets.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Open sheet | Durable TV, "TV details" tapped | "TV details" sheet naming the TV, store fields only, no socket | N/A |
| Session-only | No stored record | No sheet or Forget; session-only label | N/A |
| Pin unreadable | Stored pin missing/corrupt | "Fingerprint unavailable", Forget available | N/A |
| Forget executes | Confirm tapped | Silent channel close, Keystore + Room deleted, target cleared, no-TV state | Failure keeps record, retry copy shown; missing alias counts as deleted |
| Cancel | Cancel/dismiss | Nothing deleted, sheet stays | N/A |

</frozen-after-approval>

## Code Map

- `tv/.../api/TvApi.kt` -- reuse `PairingStore` (`save`/`load`/`clear` true-if-removed) and `PairingEntry(token, pin)`; new seam only if Forget needs one.
- `tv/.../internal/PinnedTlsChannel.kt` -- reuse `PinRules.sha256Base64`/`compare`; home for (or beside) the new pure fingerprint hex-group formatter, no Android import.
- `core/.../api/Identity.kt` -- reuse `TvId`, `StableIdentity`, `maskMac` for display and deletion keying.
- `core/.../api/RedactedLog.kt` -- only logging seam; fingerprint displayable, never logged.
- `app/.../tv/RoomTvStore.kt` + `TvRecordHolder.kt` -- reuse `byId`/`observeAll`; ADD `TvDao` delete-by-`TvId` plus holder `forget(tvId)` (no delete path exists by design); `KeystorePairingStore.clear` reused as-is.
- `app/.../MainActivity.kt` -- `SetupRoute` plus Shell wiring; add the `TvId`-keyed sheet destination and the Forget-to-no-TV reset.
- `app/.../ShellScreen.kt` -- paired summary; add the "TV details" action (launcher stays disabled).
- `app/.../setup/` (new `TrustSheet` + confirmation) -- Graphite Mint tokens, salmon-outline destructive, story-settled copy; follow the `gate/ExplainerSheet.kt` scrim/back/dialog pattern; custom line glyphs only.
- Do not change: `tools/spike-a/**`, gate permission flow, theme tokens, `docs/app-identity.md`, backup rules, CI command.

## Tasks & Acceptance

**Execution:**
- [x] Fingerprint formatter (pure, JVM-testable) -- stored-pin SHA-256 bytes to grouped uppercase hex -- AC2, AC6, AC7.
- [x] `TvDao` delete-by-`TvId` + holder `forget(tvId)` -- AC4, AC5.
- [x] `TrustSheet` UI + `MainActivity`/`ShellScreen` wiring -- durable-only sheet, fields, dismiss/focus, confirmation, Forget reset, session-only label -- AC1-AC5.
- [x] JVM tests -- masking, fingerprint format, Forget state machine, cancel/failure paths, `PairingStore.delete(tvId)` fake contract -- AC7.
- [x] Scope guards -- `RedactedLog`-only, `:app` import ban, no second store -- AC6, AC8.

**Acceptance Criteria:**
- Given a durable TV, when "TV details" is tapped, then the reusable `TvId`-keyed sheet opens from stored fields only, works TV-off, and dismisses with focus back on "TV details."
- Given the sheet, when shown, then identity, fingerprint, durable state, and local-only copy match the settled rules with no truncation and no secret leakage.
- Given Forget confirmed, when deletion runs, then the channel closes silently, Keystore and Room are both gone, the target clears, the no-TV state returns with the TV-side note, and remote removal is never claimed.
- Given cancel or deletion failure, when it happens, then nothing is deleted or the record is kept with user-action-only retry, and forgotten is never claimed early.
- Given this story, when the diff is reviewed, then no full details, labels, switcher, remote, reconnect, second store, or live-TV test exists.

## Implementation Notes

- Dispatch built the full slice; verified against the diff, not the report. Three fixes applied after the dispatch: (1) drag-down dismiss was missing — added a handle-only vertical drag (threshold 64dp, sheet follows the drag, no fight with the scrolling content) with `trust_dismiss_handle` copy, hoisting `stringResource` out of the `semantics` lambda per the 1.5 `COMPOSABLE_INVOCATION` lesson; (2) the sheet could flash "Fingerprint unavailable" while the Keystore read was in flight — the sheet is now gated on `trustEntryLoaded`; (3) the confirmation state swapped its buttons for the "Scanning…" string while forgetting — it now keeps both buttons with Forget disabled, mirroring the details state.
- `./gradlew build lint :core:test` green after the fixes (37s warm): BUILD SUCCESSFUL, 0 lint errors. JVM: app 78 (TrustLogic 4, RoomTvStore 9), tv 43 (FingerprintFormat 5), core 37 — 158 total, 0 failures. Scope sweeps: no `okhttp3`/`org.jupnp`/`KEY_` in `app/src/main`; the only `println`/`android.util.Log` hit is `RedactedLog.kt`'s own policy comment.
- Matrix coverage: every row has a running JVM test except Cancel and Open-sheet navigation, which are structural (cancel flips a boolean and calls no deletion; the production gating predicate is unit-tested in `TrustLogic.sheetVisible`) plus build-green and the owned-device manual checks below.
- Patch pass 2026-10-23 (step-04, no loopback; applied directly, no subagent re-engagement needed): G1 forget-title uses the Not-reported fallback instead of raw hex when the TV name is blank; G2 new `migration1_2AddsLastIpColumnWithDefault` pins the exact `ALTER TABLE` via a `SupportSQLiteDatabase` proxy (on-device upgrade stays a manual check); G3 production gating routes through `TrustLogic.sheetVisible` (replacing the uncalled `shouldOfferSheet`) with a 6-assert truth-table test; G4 `forgetDaoThrowCountsAsFailure` covers the holder catch branch; G5 `surroundingWhitespaceIsIgnored` pins the formatter trim. `./gradlew build lint :core:test` green after patches: 273 tasks, 0 failures; app 79 / tv 43 / core 37 JVM tests, 0 failures. Diff file rewritten to the patched tree.
- Hardware-only rows stay manual on an owned TV: Keystore alias + Room row deletion after process restart, `adb logcat` secret-freedom, 200% font / 1.3x expansion / small-screen fingerprint wrap, drag-handle dismiss feel, focus return to "TV details."

## Spec Change Log

## Review Triage Log

Review pass 2026-10-23, diff `unirem-16-impl-utf8.diff` (58.2 kB; floor N = min(floor(sqrt(58.2)+1),10) = 8). Three layers filed 11 blind + 8 edge + 2 verification-gap findings. Every claim verified against the cited file before rendering. No intent_gap, no bad_spec, no loopback: surviving entries are patch.

- B1 firstOrNull limits 1.9 reuse: false as a defect — the destination IS `TvId`-keyed (`trustTvIdHex`); first-record is the correct single-TV caller and 1.9 populates the key.
- B2 runForget never closes a channel: false — 1.6 owns no channel (pairing closes its socket; connections are Story 1.7), so "silent by construction" is accurate, not a missing close.
- B3 clear/load throw kills the coroutine with the flag stuck: false — `KeystorePairingStore.clear/load/save` all catch internally (KeystorePairingStore.kt:42/54/66) and return false/null, which `decideForget` maps to the failure UI.
- B4 no DataStore/file sweep: false — Epic 1's only DataStore is the LAN-gate decision store (no TV secrets); post-clear prefs hold no keys; `pairedName/Model` are cleared; `sessionTrust` is in-memory, process-scoped, and keyed by IP for an unresolvable-identity target, never the forgotten TV.
- B5 stale lastIp plus no cascade: false — staleness is story-acknowledged ("Last reached IP ... may be stale") and the DB holds a single entity (`TvRecord`), so no dependent rows exist.
- B6 non-transactional delete/recheck race: false — upserts happen only on the Pairing route while Forget runs only from the Shell sheet; no concurrent path was demonstrated.
- B7a no progress indication: false — the spec requires no progress copy; the disabled Forget button is the indicator.
- B7b dismiss mid-flight races runForget: low, rejected per rule 82 — millisecond local-IO window, failure lands safe (record kept, retry available), and the fix is dismissal guards (complexity). Groups with E4.
- B8 no max-height/wrap handling: false — `BasicText` soft-wraps by default inside a vertically scrolling column; 200%/small-screen behavior is already a listed manual check.
- B9a java.util.Base64 below API 26: false — minSdk is 26 and `java.util.Base64` exists since API 26.
- B9b TrustDisplay.kt/FingerprintFormat name mismatch: false — no bad outcome established (compiles, Kotlin permits, no repo filename rule, guards pass).
- B10a Close keeps the sheet: false — faithful to the story's "Try again"/"Close" (closes the error, sheet stays for retry).
- B10b raw hex TvId in the forget title when the name is blank: low, patch, group G1.
- B11 test-gap bundle: low, patch in part — dao-throw and whitespace-trim pinning tests are trivial additions (groups G4, G5); migration execution groups with V1 (G2); gating coverage groups with V2 (G3); URL-safe alphabet and focus/dismiss are false (pins are our own standard-alphabet output; dismiss/focus structural plus manual per spec).
- E1 record vanishes mid-sheet: false — nothing mutates `records` while the sheet is open; unreachable.
- E2 corrupt trustTvIdHex crashes TvId(): false — `TvId` is a plain data class with no validation (Identity.kt:101), and the hex comes only from our own stored records, never user input.
- E3 throw inside the forget coroutine: false — same evidence as B3.
- E4 dismiss during in-flight forget: low, rejected per rule 82 — same defect as B7b; grouped there.
- E5 drag-cancel leaves the sheet offset: low, rejected per rule 82 — the handle has no competing gesture so cancellation needs system interruption, the offset state disposes with the sheet (self-healing), and `draggable` offers no cancel callback (fix = custom gesture plumbing).
- E6 upsert interleave misreports success as failure: false — same evidence as B6.
- E7 session+durable coexistence hides the label: false — one TV cannot be both (session-only means unresolvable identity means no record); any different-TV staleness is invisible in-memory state outside single-TV scope.
- E8 channel-close claim: false — same evidence as B2.
- V1 migration 1→2 runs no test (pre-verified: only FakeTvDao coverage, no androidTest dir): patch, group G2.
- V2 shouldOfferSheet tested but never called by production (pre-verified: zero main callers): patch, group G3.

Groups: G1 B10b title fallback (low); G2 V1+B11-migration SQL pin test (low); G3 V2+B11-gating production wiring (low); G4 B11 dao-throw test (low); G5 B11 whitespace-trim test (low).

## Verification

**Commands:**
- `./gradlew build lint :core:test` -- expected: BUILD SUCCESSFUL, 0 lint errors, JVM tests pass.
- `rg -n "okhttp3|org\\.jupnp|KEY_" app/src/main` -- expected: no hits.
- `rg -n "println|android\\.util\\.Log" app/src/main tv/src/main core/src/main --glob '!*RedactedLog*'` -- expected: no hits outside the sink.

**Manual checks (if no CLI):**
- Owned device: Keystore alias and Room record deletion verified after process restart; no token, pin, or full MAC remains after Forget; `adb logcat` shows no secrets during sheet and Forget.
