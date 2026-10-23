---
title: 'Gate local-network access and own the permission state'
type: 'feature'
created: '2026-10-23'
status: 'done'
route: 'dispatch'
baseline_commit: '391e6c38beea7e8f21486dcdbbcacbc6e4afbd6a'
review_loop_iteration: 1
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** No scan, socket, or manual-IP attempt may run before the platform allows local-network access, but no component owns that permission — a future scan would hit a platform denial or fail silently with "TV didn't answer."

**Approach:** Ship one app-level `LanAccessGate` owning the local-network permission, the single process-wide `MulticastLock`, the isolation diagnostic, and the manual-IP route; `:core` defines the `LanAccess` tri-state and the `LanDenied` failures; `:tv`/`:cast` map denial without ever requesting permissions; the shell gains a permission card, an in-context explainer sheet, and a quiet diagnostic readout.

## Boundaries & Constraints

**Always:**
- One gate, one owner: `LanAccessGate` in `:app` owns the permission flow, the single process-wide `MulticastLock` (scoped acquire/release, freed on success, failure, and cancellation), the isolation diagnostic, and the manual-IP route; `:core` defines `LanAccess` with exactly `Granted`, `Denied`, `NotDeclared`.
- Permission is the literal `android.permission.ACCESS_LOCAL_NETWORK` (verified present in API 37, absent in API 36): declared once in the manifest; runtime request only on API 37+ behind a version check; on API <37 the gate reports `Granted` with no permission wall.
- Gate is the only requester: the system prompt fires only from Continue on the explainer, once per install unless the user re-opens it via the card; "Not now" leaves the feature unstarted; a denied gate offers "Open settings" plus a plain explanation and never re-fires the system prompt.
- `:tv` and `:cast` consult the gate before any network attempt and map denial to exactly `TvFailure.LanDenied` / `CastFailure.LanDenied`; neither module requests a permission, holds a lock, or launches UI. No second lock exists anywhere, including jUPnP.
- Denied blocks both paths identically: scan and manual IP return `LanDenied`; the UI shows the single gate explainer, never "TV didn't answer"; no scan, socket, or fallback attempt is made while denied.
- Copy is byte-settled by the UX contract: explainer "UniRem needs to find your TV on this Wi-Fi. Nothing is sent to the internet." with Continue / Not now; denied "UniRem needs local network access to reach your TV." with Open settings / Not now.
- The shell card shows the live gate state with the one appropriate action (grant / open settings / access-ready note); it never claims a TV is connected and is the only permission entry point in Epic 1. The diagnostic readout shows phone network type, scan result ("Not scanned" — no scan runs in this story), and a diagnostic code distinguishing denial from isolation from ok.
- `RedactedLog` only; besides the local-network runtime permission, only invisible install-time normal permissions (`ACCESS_NETWORK_STATE`, `CHANGE_WIFI_MULTICAST_STATE`) may be declared — still no `RECORD_AUDIO`, `POST_NOTIFICATIONS`, MediaProjection, or FGS. (Renegotiated 2026-10-23: the gate's transport readout and MulticastLock need declared normal permissions on hardware; the runtime-sensitive ban is unchanged.)

**Never:** SSDP, `/api/v2/`, pairing, token/pin/SPKI code, manual-IP form (the route is owned here; the form is story 1.5), remote keys, launcher slots starting features, text/pointer/voice/cast/mirror/wake, Settings, matrix labels, telemetry, a permission wall on first launch, a second `MulticastLock`, or `:app` importing `okhttp3`/`org.jupnp`/`KEY_*`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| First setup | `NotDeclared`, user starts setup | Explainer sheet with settled copy; Continue / Not now | Not now leaves feature unstarted, no prompt |
| Grant | System prompt allowed (API 37) | State `Granted`; card notes access ready; prompt never re-fires | N/A |
| Deny | System prompt refused | State `Denied`; explainer with Open settings / Not now; both paths blocked as `LanDenied` | No repeated system prompts; never "TV didn't answer" |
| API 36 device | Any setup start | State `Granted`; no wall, no prompt | Platform has no such permission (verified absent) |
| Scoped lock | Operation starts/finishes, incl. failure/cancel | Exactly one process-wide lock held during the op, then released | Cancel/failure still releases; no second lock |
| Readout | Denied, or Granted with no scan | Quiet readout: network type, "Not scanned", code for denial / isolation / ok | Diagnostic copy only, never marketing |
| Module mapping | `:tv`/`:cast` consult with `Denied` | `TvFailure.LanDenied` / `CastFailure.LanDenied`; no permission request, no UI | Covered by JVM tests with a fake gate |

</frozen-after-approval>

## Code Map

- `core/src/main/kotlin/com/unirem/core/api/LanAccess.kt` (new) -- `LanAccess` (`Granted`, `Denied`, `NotDeclared`) + `NetworkType` (`Wifi`, `Ethernet`, `Unknown`) per AD-11/AD-5; JVM-only, no Android import.
- `core/src/main/kotlin/com/unirem/core/api/Failures.kt` (new) -- sealed `TvFailure` / `CastFailure`, each starting with the single member `LanDenied`; extensible by later stories; `MirrorFailure` untouched.
- `tv/src/main/kotlin/com/unirem/tv/api/TvApi.kt` (extend) -- pure `checkLanAccess(LanAccess): TvFailure?` mapping (`Denied` → `LanDenied`, else null); no permission/lock/UI code. `cast/.../CastApi.kt` mirrors it for `CastFailure`.
- `app/src/main/kotlin/com/unirem/gate/LanAccessGate.kt` (new) -- sole owner: `StateFlow<LanAccess>`, version-gated permission request launcher hookup, single `MulticastLock` scoped handle, `NetworkType` derivation, diagnostic snapshot; never holds an Activity reference. Retained decision: the last decision plus an asked flag persist in DataStore, so Denied survives restart and rotation; initial rule is API under 37 to Granted, stored decision if present, else platform check (Granted or NotDeclared). Settings-return settles both directions (granted to Granted; revoked-from-Granted to Denied; otherwise unchanged). Both lock paths share one exclusion with an atomic held flag and depth counting; the transport source is constructor-injectable for JVM tests; code strings use Locale.ROOT; actions carry content descriptions. MainActivity calls gate.refresh() in onResume to recompute the snapshot.
- `app/src/main/.../gate/PermissionCard.kt` + `ExplainerSheet.kt` + `DiagnosticReadout.kt` (new) -- card on the shell below the version block; bottom sheet per UX-DR24; quiet `code`-style readout; Graphite Mint tokens only.
- `app/src/main/kotlin/com/unirem/MainActivity.kt` (edit) -- owns the `RequestPermission` launcher, forwards results to the gate; gate stays Activity-free.
- `app/src/main/AndroidManifest.xml` (edit) -- declares `ACCESS_LOCAL_NETWORK` (runtime, API 37+) plus normal install-time `ACCESS_NETWORK_STATE` and `CHANGE_WIFI_MULTICAST_STATE` (required for the transport readout and the MulticastLock to function on hardware); still no audio/notifications/MediaProjection/FGS entries.
- `app/src/main/res/values/strings.xml` (edit) -- settled explainer/denied/card/readout copy; sentence case, no exclamation marks.
- `app/src/test/...` + `core/src/test/...` (extend) -- JVM tests for tri-state transitions, pure reducers for every state change (result, settings-return, initial), both failure mappings (fake gate), injectable-transport diagnostic (Wifi to Ok, Unknown to Isolated, Denied to Denied), lock release on success/failure/skip, Locale-safe codes; manifest test updated to allow exactly the three declared permissions (one runtime + two normal); scope-guard bans (second lock, module permission requests) extended. Matrix edge cases covered without a device.
- Do not change: `tools/spike-a/**`, `debug/*`, GraphiteMint tokens, `docs/app-identity.md`, CI command, `_bmad/**`, `.agents/**`, `.opencode/**`.

## Tasks & Acceptance

**Execution:**
- [x] `core/.../LanAccess.kt` -- tri-state + network type, JVM-only -- AC1.
- [x] `core/.../Failures.kt` -- `TvFailure`/`CastFailure` with `LanDenied` -- AC1, AC8.
- [x] `tv/.../TvApi.kt` + `cast/.../CastApi.kt` -- pure denial mapping, no platform code -- AC1, AC8.
- [x] `app/.../gate/LanAccessGate.kt` -- single owner: permission, one lock, diagnostic, route, DataStore-retained decision -- AC1, AC5, AC6.
- [x] `app/.../gate/PermissionCard.kt` + `ExplainerSheet.kt` + `DiagnosticReadout.kt` -- settled copy, one action per state -- AC2, AC6, AC7.
- [x] `app/.../MainActivity.kt` -- launcher ownership, result forwarding -- AC2, AC3.
- [x] `app/.../AndroidManifest.xml` + `strings.xml` -- one permission, settled copy -- AC2, AC3.
- [x] `app/src/test` + `core/src/test` -- tri-state + mapping JVM tests; manifest/guard tests updated; matrix edge cases -- AC4, AC8, AC9.

**Acceptance Criteria:**
- Given the app, when the gate is `Denied` and the user attempts discovery or manual IP, then both are blocked identically as `LanDenied` with the single explainer and zero network attempts.
- Given API 37, when setup starts, then the explainer precedes the system prompt, Continue fires it once, and denial lands on Open settings / Not now with no re-prompt.
- Given API 36, when setup starts, then the gate reports `Granted` with no wall and no prompt.
- Given a scoped operation ending in failure or cancellation, when it exits, then the single process-wide lock is released and no second lock exists.
- Given this story, when the diff is reviewed, then it contains no SSDP, pairing, key, launcher-feature, media, Settings, telemetry, or cloud-fallback code.

## Implementation Notes

- Step-03 verification (2026-10-23): matrix row "Scoped lock" had no covering JVM test, so the lock-guard core was extracted to pure `LanAccessGate.guardedRun` / `guardedRunSuspend` companions (identical semantics, delegation only) with 4 new `LanGateLogicTest` cases (release on success/failure, skip when not acquired, suspend success+failure). Full `./gradlew build lint :core:test` green: 39 product tests, 0 lint errors. Device prompt/lock-lifecycle checks (API 36/37) still need an owned phone.
- Re-derivation after review loopback 1 (2026-10-23): DataStore-retained decision (denial survives restart/rotation), both-directions settings return, resume refresh, shared lock exclusion with atomic flag and depth counting, injectable transport, Locale.ROOT codes, three-permission manifest. Full `./gradlew build lint :core:test` green: 65 product tests, 0 lint errors; scope greps clean. Device-only checks (API 37 prompt flow, API 36 no-wall, real transport, lock lifecycle, double-tap) still need owned hardware.

## Spec Change Log

- 2026-10-23 (review loopback 1, bad_spec + human-renegotiated intent_gap): triggering findings B2/E4 (denial lost across restart and rotation), E3/E11 (undeclared CHANGE_WIFI_MULTICAST_STATE means the lock can never be held), B7/V2 (undeclared ACCESS_NETWORK_STATE means Ok unreachable), E9 (stale diagnostic), E5/E10/B3 (settings revoke), B5/E1/E2 (lock exclusion), B4/V1 (untested transitions), E8 (locale), B9 (a11y), B1 (dead const), E7/V3 deferred to device checks. Amended: frozen permission line now allows declared install-time normal permissions (human-approved; runtime-sensitive ban unchanged); Code Map gains DataStore-retained decision plus asked flag, both-directions settings rule, resume refresh, shared lock exclusion with atomic flag and depth counting, injectable transport, Locale.ROOT, content descriptions, three-permission manifest; tasks cover reducers, injectable diagnostic, and release tests. Known-bad avoided: re-prompt after restart/rotation, permanently Unknown readout, never-held lock, stale revoked-ready card, locale-broken codes. KEEP: pure-companion pattern and JVM-test style, gate Activity-free seam with Activity-owned launcher, byte-settled copy tests, manifest/guard tests, card/sheet/readout UI structure with insets, guardedRun extraction, coroutines-core wiring precedent, RedactedLog-only.

## Review Triage Log

Review pass 2026-10-23, diff ~60.9 kB (blind floor N=8; 10 blind + 11 edge + 3 verification-gap findings).

- B1 dead MANUAL_IP_ROUTE const — verdict: low. Declared with no consumer, test, or route wiring. Route: patch G-DEAD (delete; checkBeforeAttempt is the real route).
- B2 denial lost across restart — verdict: medium. Denied re-derives to NotDeclared, so a relaunch re-offers Continue and the prompt can re-fire, against AC2. Spec's initial-state rule retains nothing. Route: bad_spec G-PERSIST.
- B3 settings revocation never recorded — verdict: low. onSettingsReturned only upgrades; a settings-side revoke leaves the card on ready. Route: patch G-SETTINGS.
- B4 instance transitions untested — verdict: low. No test constructs the gate or calls onPermissionResult/onSettingsReturned/checkBeforeAttempt/diagnostic; same defect as V1. Route: patch G-COVERAGE.
- B5 suspend lock path unsynchronized — verdict: low. withMulticastLockSuspend skips the monitor and held is a plain var; no callers yet, so latent. Route: patch G-LOCK (shared with E1).
- B6 isolation conflated into Unknown — verdict: false. AD-5 mandates the closed NetworkType triple and the story only requires the denial/isolation/ok distinction, which the code makes.
- B7 transport degrades by catch — verdict: low. Without ACCESS_NETWORK_STATE the query throws into Unknown, so Ok is unreachable on device; tests use synthetic inputs only. Route: patch G-TRANSPORT.
- B8 AC numbering missing — verdict: low. Tasks cite AC1–AC9 against five unnumbered bullets. REJECTED per rule 86 (fix would edit this build's spec).
- B9 sheet/card a11y contract — verdict: low. Role.Button actions lack content descriptions and focus order; no UI-state tests. Route: patch G-A11Y.
- B10 settings-open failure silent — verdict: low. Catch logs only, leaving a possibly dead button. REJECTED per rule 82 (everyday-use unlikely; fix needs new UI state).
- E1 sync/suspend lock race — verdict: low. Same defect as B5: interleaved acquire/release can leak held. Route: patch G-LOCK.
- E2 nested scope releases early — verdict: low. Inner exit clears held and releases while the outer block still runs; no nested caller exists yet. Route: patch G-LOCK (depth counter).
- E3 lock unavailable runs block unprotected — verdict: medium. CHANGE_WIFI_MULTICAST_STATE is undeclared so the lock can never be granted; every future op would run with zero locks against AC5. Fix needs the frozen permission line changed. Route: intent_gap G-MANIFEST.
- E4 rotation rebuilds the gate — verdict: medium. A rotation after denial resets to NotDeclared and re-offers Continue (same AC2 violation as B2, without killing the process). Same unretained-state root. Route: bad_spec G-PERSIST.
- E5 revoke-in-settings stale — verdict: low. Same defect as B3. Route: patch G-SETTINGS.
- E6 denial recorded below API 37 — verdict: false. The launcher never fires below 37 and onExplainerContinue hardcodes the granted path there; no caller can record a denial.
- E7 double-tap Continue — verdict: maybe-false. A second launch() while a prompt is in flight may throw; unsettleable without hardware. Route: defer G-DOUBLETAP (settler: rapid double-tap Continue on API-37 device; medium-if-true).
- E8 locale-unsafe lowercase — verdict: low. code.name.lowercase() breaks under tr (dotless i); nothing asserts code strings. Route: patch G-LOCALE (Locale.ROOT).
- E9 stale diagnostic after transport change — verdict: low. The snapshot recomputes only on access/sheet changes, so joining WiFi keeps a stale isolated code; the spec never settled refresh. Route: bad_spec G-REFRESH.
- E10 downgrade missing on settings return — verdict: low. Same defect as B3 (upgrade-only return). Route: patch G-SETTINGS.
- E11 block runs with null lock — verdict: medium. Same root as E3: undeclared multicast permission means a null lock and unprotected ops. Route: intent_gap G-MANIFEST.
- V1 instance transitions have no executing test — verdict: low (pre-verified; inverted ternary undetected). Route: patch G-COVERAGE (pure reducers for all transitions, folded into re-derivation).
- V2 transport Ok unreachable — verdict: low (pre-verified; Granted WiFi reads Unknown/isolated). Route: patch G-TRANSPORT (injectable source + JVM test; declaration via G-MANIFEST).
- V3 prompt-firing rule manual-only — verdict: low (pre-verified; pure rule already tested via continueActionFor, wiring is JVM-untestable). Route: defer (settler: owned-device prompt matrix on API 37/36, already assigned to manual Verification).

## Verification

**Commands:**
- `./gradlew build lint :core:test` -- expected: BUILD SUCCESSFUL, 0 lint errors, `:core` + `:app` tests pass, no device needed.
- `rg -n "MulticastLock|WifiManager" app/src/main tv/src/main cast/src/main core/src/main` -- expected: hits only inside the gate file (tests and comments carry no product lock references).
- `rg -n "requestPermissions|RequestPermission" tv/src cast/src core/src` -- expected: no hits (gate-only requesting).

**Manual checks (if no CLI):**
- On an API 37 device: fresh install → shell card offers grant → explainer copy exact → Continue fires system prompt → deny → card shows Open settings / Not now, no re-prompt.
- On an API 36 device: fresh install → card notes access ready, no prompt, no wall.
- Manifest declares the three gate permissions (`ACCESS_LOCAL_NETWORK`, `ACCESS_NETWORK_STATE`, `CHANGE_WIFI_MULTICAST_STATE`); no audio/notification/MediaProjection/FGS entries.
Review pass 2, 2026-10-23, re-derived tree, diff ~92.5 kB (blind floor N=10; 13 blind + 7 edge + 2 verification-gap findings).

- B1 double-tap Continue unguarded — verdict: carried. Same location and claim as E7; code still reads as described (firePrompt with no in-flight guard, direct launcher call). Carried maybe-false, defer G-DOUBLETAP; never re-patched or re-deferred.
- B2 platformGranted throws in refresh/init — verdict: low. Lines 111 and 165 call it bare while line 180 uses runCatching; a throwing source fails the init coroutine or crashes onResume. Route: patch G-PGUARD.
- B3 onPermissionResult below API 37 — verdict: carried. Same location and claim as E6; code still reads as described (reducer unconditional but unreachable below 37 via the version-gated Continue path). Carried false.
- B4 manual-IP route unwired — verdict: false. Zero production callers is by design: story 1.5 owns scan, socket, and the manual-IP form; the consult API plus both pure mappings plus their tests are the owned route this story tasks.
- B5 lock-acquire failure unobservable — verdict: false. Both acquire and release paths log via RedactedLog.warn, and fail-open is specified in the guardedRun contract and pinned by skip-release tests; not a silent path.
- B6 loading transient before store load — verdict: low. Sync seed shows until the init coroutine publishes; a sub-second wrong card is possible but no prompt can fire without a tap. REJECTED per rule 82 (millisecond window; fix needs a new settled-flow surface).
- B7 sheet visibility lost on rotation — verdict: low. explainerVisible is plain mutableStateOf, so rotation dismisses an open explainer while the gate retains state. Route: patch G-SHEET (rememberSaveable).
- B8 sheet back/dialog semantics — verdict: low. Dismissal is scrim-tap only with no back handler or dialog role; same a11y-contract root as round-1 B9. Route: patch G-A11Y.
- B9 48dp target unenforced — verdict: low. GateActions padding sums to roughly 42dp vertical with no minimum size despite the 48dp comment. Route: patch G-TARGET (heightIn minimum).
- B10 semantics merge splits label — verdict: low. contentDescription sits on the container while BasicText stays separate; TalkBack may announce the description without the visible label. Same a11y root. Route: patch G-A11Y.
- B11 readout tri-state jargon — verdict: false. The readout is a code-style diagnostic by design with monospace styling; the settled-copy contract governs explainer, denied, and card wording, and no UX rule demands plain language inside the code readout.
- B12 Main dispatcher for DataStore — verdict: false. DataStore is main-safe by design, so Main.immediate scope plus warn-and-continue catch paths is a harmless, documented arrangement.
- B13 manifest maxSdkVersion absent — verdict: false. The version gate short-circuits below 37 before any platform query, unknown-permission declarations are manifest-ignored on 36, and below-37 tests pin Granted.
- E1 async denial transient — verdict: low. Same defect as B6 (sync seed until store load publishes). Route: REJECTED per rule 82, same evidence.
- E2 rapid Continue taps — verdict: carried. Same location and claim as E7; code still reads as described. Carried maybe-false, defer G-DOUBLETAP.
- E3 platformGranted throw — verdict: low. Same defect as B2 (bare calls at lines 111 and 165). Route: patch G-PGUARD.
- E4 second launch in flight — verdict: carried. Same location and claim as E7; code still reads as described. Carried maybe-false, defer G-DOUBLETAP.
- E5 sheet visibility on rotation — verdict: low. Same defect as B7. Route: patch G-SHEET.
- E6 denial below API 37 — verdict: carried. Same location and claim as round-1 E6. Carried false.
- E7 every-NotDeclared Continue refires — verdict: false. The spec permits re-fire on explicit re-open ("once per install unless the user re-opens it via the card"), Denied routes to Open settings instead of the prompt, and a prompt dismissed without a decision leaves NotDeclared where a fresh Continue is a new explicit gesture.
- V1 throwing transport source untested — verdict: low (pre-verified; deleting the readTransport catch breaks nothing today). Route: patch G-VTEST (JVM test with a throwing source asserting Unknown/Isolated survival).
- V2 resume-refresh wiring manual-only — verdict: carried. Same location and claim as round-1 V3; code still reads as described (helper tested, Activity call manual-only). Carried low, defer; settler stands.

