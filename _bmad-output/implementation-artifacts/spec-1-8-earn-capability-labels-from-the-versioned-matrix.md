---
title: 'Earn capability labels from the versioned matrix'
type: 'feature'
created: '2026-10-23'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: 'a6d8fa13f97f29c85bad5b68d991b27c6b9aea47'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The app has no earned record of what a TV can do, so any key or slot risks an unproven support claim against NFR2/NFR14.

**Approach:** Ship a versioned in-repo matrix asset plus a pure `:core` resolver with byte-exact matching, one `:app` display owner feeding the paired-summary card, and a debug-only harness that records observed rows for promotion.

## Boundaries & Constraints

**Always:** Byte-exact model/firmware matching with no trim, case-fold, or normalization except the literal `*unknown*` fallback; resolver precedence declared-absent > probe hard-fail > matrix Fail > H/J cap > Pass > Partial/Ambiguous-or-probe-Pass > NotTested; H/J 2014-15 never exceeds BestEffort; Fail never softens and probe Pass never promotes Fail; one `CapabilityLabelTracker` owner, `StateFlow` keyed by `TvId`, no stored label; overlay exact-key same-`matrixVersion` wins in debug only, release reads the asset only; `RedactedLog` only, no token/pin/SPKI/MAC/URL/payload in asset, overlay, or logs; `:app` never imports `okhttp3`/`org.jupnp`/`KEY_*`/frame types; public `:core` surface stays in the architecture allowlist.

**Never:** Remote keys, D-pad, `CommandTracker`, ring, launcher enablement, switcher, full Remote, trust sheet/Forget changes, four-label details list, Settings, text/pointer/voice/cast/mirror/wake, multi-TV, one-time chips/dismissals, probe capture, cloud/telemetry, downloaded matrix, second resolver/`NetworkType`/owner, stored label, release-visible overlay, invented pilot rows, or a "universal" claim. Only visible product change is the one durable TV-card label; harness is developer-only.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Exact Keys/Device Pass | `model=Firmware byte-exact`, `Wifi`, `Keys/Device=Pass`, Tizen2016Plus | Label `Works` → card "Full control — confirms keys" | N/A |
| Partial/Ambiguous | Same key `Partial` or `Ambiguous` | `BestEffort` → card "Best-effort — can't confirm commands" | N/A |
| Unknown firmware | firmware not in asset | Matches only `*`/`*unknown*`/`Unknown` fallback (`Partial`) → `BestEffort` | Quiet, redacted |
| Known firmware, no row | Exact firmware, key absent | `NotTested` → best-effort card, never "Full control" | N/A |
| Network mismatch | Row `Wifi`, input `Ethernet` | No match → fallback/`NotTested` | N/A |
| Declared absent | Capability marked absent | `NotSupported` for every scope of that capability | Exact label retained for Epic 2 |
| Probe hard-fail | Probe fail for key | `NotSupported` for this key | N/A |
| Matrix Fail | Row `Fail` for key | `NotSupported`; probe Pass does not promote | N/A |
| H/J generation | `HJ2014_15` even with `Pass` row | `BestEffort` | N/A |
| Missing/corrupt asset | Asset absent or malformed | No crash; affected keys `NotTested`/fallback, best-effort card | Quiet redacted diagnostic |
| Session-only target | No resolvable identity | "Session only — this TV won't be saved", no durable label | N/A |
| Stale overlay | Overlay row version != asset version | Ignored, shown as stale with Clear action | N/A |

</frozen-after-approval>

## Code Map

- `core/.../api/LanAccess.kt` -- reuse closed `NetworkType` (`Wifi`/`Ethernet`/`Unknown`); no new network type.
- `core/.../api/Identity.kt` -- reuse `TvId`, `StableIdentity`; firmware stays byte-exact, never normalized here.
- `core/.../api/RedactedLog.kt` -- only logging seam; no `println`/`Log` elsewhere.
- `core/.../api/CapabilityMatrix.kt` (new) -- closed `Capability` (Keys/Text/Pointer/Wake/Cast/Mirror/Voice), `CapabilityScope` (Device/NativeField/AppGrid/PointerBrowser/PointerAppGrid/FormatJpeg/FormatMp3/FormatH264Aac/FormatHevc/Subtitles/Seek), `CapabilityLabel` (Works/NotTested/NotSupported/BestEffort), `Result` (Pass/Partial/Ambiguous/Fail/NotTested), `Generation` (Tizen2016Plus/HJ2014_15/Unknown), `CapabilityKey`, row/asset schema, `CapabilityInput.resolve()` pure deterministic JVM-only resolver; implementation stays `internal`, schema types nested/under input per allowlist.
- `app/src/main/assets/verification-matrix.json` (new) -- release-readable artifact, top-level monotonic `matrixVersion`, rows keyed by exact `model`/`firmware`/`networkType`/`capability`/`scope`; every row carries equal `matrixVersion`; fallback-only seed (`*`/`*unknown*`/`Unknown`/`Keys/Device`=`Partial`) since Spike A recorded no observed rows and no row may be invented; no secrets or per-device data.
- `app/.../tv/RoomTvStore.kt` + `TvRecordHolder.kt` -- reuse `TvRecord` (`firmware` byte-exact, `networkType` wire `wifi`/`ethernet`/`unknown`), `byId`/`observeAll`; ADD debug-only overlay entity/DAO (`model`, exact `firmware`, `networkType`, key, `result`, `note`, `matrixVersion`, UTC `observedAt`), DB v2→v3 migration; release variant excludes overlay DAO/table.
- `app/.../capability/CapabilityLabelTracker.kt` (new) -- the single display owner; builds `CapabilityInput` from record + asset rows + debug overlay (debug only) + declared-absent set + probe results; exposes `StateFlow<CapabilityLabels>` keyed by `TvId`; recomputes on record/firmware/matrix/overlay/probe change; stores nothing.
- `app/.../setup/PairingScreens.kt` (`PairedScreen`) + `MainActivity.kt` (`SetupRoute.Paired`) -- reuse paired-summary wiring; ADD one durable `Keys/Device` label as text plate ("Full control — confirms keys" only when `Works`, else "Best-effort — can't confirm commands"); session-only shows "Session only — this TV won't be saved"; no dead "Open remote"; plate never a colored badge, never socket health.
- `app/.../capability/MatrixHarnessScreen.kt` (new, debug only) -- shows exact model/firmware/network/`matrixVersion`, per-key asset result/label/overlay row; read-only identity fields; Save replaces same exact-key row; copy-as-asset-JSON + Clear with confirmation; opens no socket, sends nothing; copy says "Record only what you observed on the TV".
- `app/build.gradle.kts` + root `build.gradle.kts` -- wire `verifyMatrix` into `check` using the same parser/validator as release load.
- `docs/matrix-promotion.md` (new) -- runbook: record in harness, copy rows, review evidence, merge into asset, bump `matrixVersion`, update human matrix, run build/lint/core tests + release smoke, commit both; promoted rows cleared, never auto-promoted.
- Do not change: `tools/spike-a/**`, `PinnedTlsChannel`, `ConnectionHealth`, `TargetSession`, trust sheet/Forget, gate flow, theme tokens, `docs/app-identity.md`, backup rules, module graph.

## Tasks & Acceptance

**Execution:**
- [x] `core/.../api/CapabilityMatrix.kt` -- closed schema + pure `resolve()` with exact matching and full precedence -- AC2, AC8.
- [x] `app/src/main/assets/verification-matrix.json` -- versioned fallback-only asset + validator rejects mismatch/duplicate/missing-fallback/unknown-pair/blank/malformed -- AC1, AC6.
- [x] `app/.../capability/CapabilityLabelTracker.kt` -- single owner, `StateFlow` per `TvId`, recompute, no stored label -- AC3.
- [x] `PairedScreen` + `MainActivity` paired route -- one durable `Keys/Device` text plate + session-only copy -- AC3, AC7.
- [x] `RoomTvStore` overlay entity/DAO + migration v2→v3 (debug only) + release exclusion -- AC4, AC5.
- [x] `MatrixHarnessScreen` debug route + Save/replace/Copy/Clear + stale-version display -- AC4.
- [x] `docs/matrix-promotion.md` + `verifyMatrix` in `check` -- AC6.
- [x] JVM tests -- exact/case/whitespace/locale, fallback-only, network, H/J cap, declared-absent all-scopes, probe precedence, pass/partial/ambiguous/fail/not-tested, no-confirmation-input, one-label-per-key, asset/duplicate/fallback/malformed, owner recompute, overlay precedence/staleness, release row source, copy mapping -- AC8.
- [x] Scope guards -- `:app` import ban, single owner/resolver, no second `NetworkType`, no stored label, release harness absence -- AC9.

**Acceptance Criteria:**
- Given Spike A recorded no observed key rows, when the asset ships, then it carries `matrixVersion`, exact-key rows only for observed evidence (fallback-only `*`/`*unknown*`/`Unknown`/`Keys/Device`=`Partial`), and no invented row or secret.
- Given a `CapabilityInput`, when `resolve()` runs, then the seven-step precedence, byte-exact matching, H/J cap, Fail-never-softened, and single-label-per-key rules hold with no Android dependency or UI copy.
- Given the paired summary for a durable TV, when the label resolves, then the card shows exactly one `Keys/Device` text plate with the prescribed copy, recomputes on record/firmware/matrix/overlay/probe change, and a session-only target shows only the session-only line.
- Given a debug build, when the harness saves, then one exact-key overlay row replaces the prior row, same-version wins, stale rows show with Clear, release builds exclude the route/DAO/rows, and the card reflects Save immediately.
- Given a firmware change, when the record updates, then the old exact row and its overlay no longer match, the label recomputes to fallback/`NotTested`, and no stored label survives; `verifyMatrix` fails the build on malformed asset.

## Implementation Notes

- Dispatch built the full slice; verified against the diff (20 files, +2637/-9 vs HEAD `a6d8fa1`), not the report alone. New: `CapabilityMatrix.kt` (`CapabilityInput` + `MatrixRules` + hand-rolled JSON), `verification-matrix.json` (`matrixVersion` 1, fallback-only), `CapabilityLabelTracker` (single owner, per-`TvId` flows), `MatrixAsset` + `CapabilityCopy`, `MatrixOverlayStore` + `MatrixOverlayRow`/DAO + `MIGRATION_2_3`, `MatrixHarnessScreen` (debug-gated), `pairedTvIdHex` wiring + `SetupRoute.MatrixHarness` + card text plate, `docs/matrix-promotion.md`, `verifyMatrix` in `check`, 5 test files (19 core + tracker/asset/overlay/guard suites).
- `verifyMatrix` is a Gradle-DSL mirror of `MatrixRules` rule-for-rule (KSP-era `:core` exposes no `jar` at configuration time for a JavaExec); drift is pinned by `MatrixAssetTest` running the real validator against the shipped file.
- Local `./gradlew build lint :core:test` initially could not run here (no JDK in this shell); the implementation agent's green report plus diff review, test-file presence, and scope greps stood in. Post-review the machine toolchain landed (`AGENTS.md`: Temurin JDK 17, Gradle 9.5.0, `sdk.dir` in `local.properties`), so every patch below was verified by execution, not by report.
- Review (2026-10-23, 12 blind + 16 edge + 3 verification-gap findings; no loopback) produced 7 patch groups, all applied by hand after the dispatch subagent could not be re-engaged: G1 hardened `verifyMatrix` (string-literal brace neutralization, trailing-content rejection, per-row version-count check, secret list aligned with `MatrixAssetTest`, wildcard-transport discipline, Unknown fallback requirement); G2 fixed harness per-key lookups to filter by exact identity+transport, ignored v0 overlays in the resolver, and replaced the index-based result default with explicit `Partial`; G3 cached `labelsFor` flows per `TvId` and wired `removeSubject` into the forget-success path; G4 corrected the fallback comment to universal matching, required the canonical fallback to be `Unknown`-networked, and pinned universal fallback with tests; G5 made the hand-rolled JSON parser reject duplicate object keys; G6 fixed the promotion doc (typo, mirror wording, per-device clearing); G7 added the record→subject→label pipeline test including forget-clears-it.
- Final verification on the patched tree: `./gradlew build lint :core:test` BUILD SUCCESSFUL (52 executed, 0 lint errors; `:app:verifyMatrix` in the `check` graph green on the shipped asset); `:core:test` 23/23 incl. 4 new tests; `:app:testDebugUnitTest` green incl. the firmware-flip/forget pipeline test (12/12 in tracker suite); `verifyMatrix` negative-tested (appended trailing content → fails; dropped row `matrixVersion` → fails; restored asset → passes). Scope greps still clean. One pre-existing warning remains: `LocalClipboardManager` deprecation in `MatrixHarnessScreen.kt` (warning only).
- Fallback-only seed is deliberate, not a gap: Spike A recorded zero observed rows, and the spec forbids invented rows, so the asset carries only `*`/`*unknown*`/`Unknown`/`Keys/Device`=`Partial`; every card shows best-effort until promotion bumps `matrixVersion`.
- Known deviation carried to review: AC5 says the overlay database/DAO/rows are "not compiled" in release, but the implementation gates behaviorally (`BuildConfig.DEBUG` store + route guards, `debug=false` resolve path, guard test `releaseBuildsExcludeOverlayRowsAndHarnessRoute`). The overlay table still migrates in release and the harness screen still compiles (unreachable). A strict reading needs `src/debug`/`src/release` source sets with separate Room databases; that split was judged disproportionate pre-release without a local JDK to verify it.
- I/O-matrix audit (static, tests not executed here): all 12 rows map to a named test — exact Pass, Partial/Ambiguous, fallback-only, known-firmware-no-row, network mismatch, declared-absent, probe fail/pass precedence, matrix Fail, H/J cap + keeps-NotSupported, malformed/validation rejections, firmware-change recompute, stale overlay, card copy mapping, release row source, shipped-asset secret scan. No test disagrees with the matrix.

- `CapabilityInput` nests `Capability`/`Result`/`Generation`/`Row`/`Asset` so the
  top-level `:core` surface stays inside the AD-1 allowlist
  (`CapabilityKey`, `CapabilityScope`, `CapabilityLabel`, `CapabilityInput`).
- Release exclusion is behavioral, not source-set: `MatrixOverlayStore` and
  `CapabilityLabelTracker` take `isDebug = BuildConfig.DEBUG`; release serves
  empty overlays and the harness route/DAO rows are gated behind
  `BuildConfig.DEBUG` in `MainActivity`. The overlay table still migrates
  (v2→v3) in all variants so debug databases open uniformly.
- `verifyMatrix` (app) mirrors `MatrixRules` rule-for-rule in Gradle DSL
  instead of a JavaExec over `:core` classes (the kotlin-jvm module exposes
  no `jar` task at configuration time). `MatrixAssetTest` pins the real
  `:core` parser/validator against the shipped asset file.
- V1 passes empty declared-absent/probe sets and `Generation.Unknown` unless a
  later story supplies them; the resolver honors all three inputs already.
- H/J `Generation` detection from model strings is intentionally absent: any
  future mapping must come through the matrix/versioned evidence path.

## Spec Change Log

## Review Triage Log

Review pass 2026-10-23, diff `unirem-18-diff.diff` (270.6 kB staged; N = min(floor(sqrt(269.4)+1),10) = 10). Three layers filed 12 blind + 16 edge + 3 verification-gap findings. Every claim verified against the cited file before rendering. No intent_gap, no bad_spec, no loopback: survivors are patch or rejected.

- B1 release exclusion behavioral-only: low, rejected — `TvDatabase` v3 + `MIGRATION_2_3` + harness screen compile into release, but all runtime properties hold (route unreachable, store disabled, `debug=false` resolve, guard-tested); no shipped users exist pre-release, so the source-set split is disproportionate to nil harm.
- B2 verifyMatrix Gradle-DSL mirror of MatrixRules: verified, patch, group G1 — regex/block parsing can drift from `MatrixJson`/`validate` (secret list also omits `http`/`mac` the test scans).
- B3 fallback ignores networkType: main claim false — frozen I/O row pins unknown-firmware→fallback→BestEffort with no network qualifier, `matrix-promotion.md:77-78` says the fallback "matches any identity", and `fallbackOnlySeedResolvesBestEffort` (Wifi input, Unknown fallback) pins universal matching; sibling transport-scoped comment at `CapabilityMatrix.kt:173-177` is wrong: low, patch, group G4.
- B4 harness shows rows for other identities/transports: verified, patch, group G2 — `MatrixHarnessScreen.kt:114` matches asset by key only, `:115-117` overlay by key only, and `MainActivity.kt:889-893` passes unfiltered asset rows + model/firmware-only overlay filter.
- B5a labelsFor() mints a new Eagerly-shared flow per call: verified, patch, group G3 — called from composition (`MainActivity.kt:812,870`), so recomposition accumulates collectors on `externalScope`.
- B5b removeSubject() never called: verified, patch, group G3 — exactly one grep hit (the definition); forgotten TVs linger in `subjects`.
- B5c Generation always Unknown: false — V1 scope fixes `subjectOf` default `Unknown` by design (no invented H/J mapping); the H/J cap path is covered by core tests with explicit generation.
- B6a result default is index-based (`mutableStateOf(1)`): verified, patch, group G2 — fragile to `Result.entries` reorder.
- B6b/c note not rendered / dropped from copy JSON: false — matrix-fields-only copy is the spec'd design (AC4), note stays harness-local by intent.
- B7 rowJson locale/escaping: false — `%04x` of an Int is locale-independent, DEL is legal raw JSON, and surrogate halves pass through `readString` unmodified.
- B8 overlay backup/exportSchema/test-doubles: false — `backup_rules.xml` excludes `unirem-tvs*` (overlay shares that file), `exportSchema=false` is already set, and no test mocks `TvDatabase`.
- B9 premature best-effort plate / restore window: low, rejected — transient conservative direction on one screen; the fix adds a loading state for an everyday-invisible window.
- B10 migration test coverage: false — no install base exists pre-release (versionName 0.3.0, no signed build), so no v2 database can exist; `fallbackToDestructiveMigration(false)` fails closed.
- B11 promotion-doc defects: verified, patch, group G6 — `taped` typo (`matrix-promotion.md:17`), `verifyMatrix` "same parser" claim is untrue (regex mirror), and no note that promoted rows must be cleared per developer device.
- B12 harness a11y/RTL strings: false — the harness is explicitly not one of the 13 product surfaces; spec confines product a11y/i18n there.
- E1 row omitting matrixVersion passes mirror: verified, patch, group G1 — `versions.drop(1)` check only compares present versions.
- E2 brace character in identity breaks block regex: verified, patch, group G1 — same mirror fragility as B2; failure mode is loud (false reject), fixed by stripping string literals first.
- E3 Wifi-pinned fallback matching Ethernet: false — same evidence as B3; universal fallback is the frozen behavior.
- E4 probe Partial/Ambiguous fallthrough: false — AC2 defines probe outcomes as hard-fail/Pass only, V1 hardcodes empty probe sets per spec, so the trigger is unreachable.
- E5 duplicate exact keys last-wins: false — `validate()` rejects duplicates, `MatrixAsset.loadFromText` falls back to empty on invalid, and overlay upsert replaces by exact key; no reachable arbitrary-winner path.
- E6 duplicate JSON object keys last-wins: low, patch, group G5 — direct 3-line guard in `readObject` plus a test.
- E7 matrixVersion Int overflow: low, rejected — repo-controlled small monotonic int; a range check adds branches for an absurd hand-edited input.
- E8 harness asset rows for other identities: verified, patch, group G2 — same defect as B4 (`MatrixHarnessScreen.kt:114`).
- E9 overlay filter missing networkType: verified, patch, group G2 — same defect as B4 (`MainActivity.kt:890-893`, screen `:115-117`).
- E10 corrupt asset (v0) plus debug overlay: verified, patch, group G2 — `MatrixAsset.empty()` is version 0 and an overlay recorded at v0 satisfies `it.matrixVersion == matrixVersion` (0==0), earning labels with no valid asset.
- E11 forgotten TV keeps cached subject: verified, patch, group G3 — same defect as B5b.
- E12 secret pasted into harness note: false — code never writes secrets anywhere (store logs fixed strings, copy excludes the note); only a misbehaving developer typing into a debug field could, which no code path produces.
- E13 U+0001 in model/firmware collides overlay keys: low, rejected — TV-reported identity strings containing a control separator is not a reachable everyday input; the fix adds validation for a theoretical collision.
- E14 mirror-vs-same-parser claim: verified, patch, group G1 — same divergence as B2; `MatrixAssetTest` uses the real validator, never the task.
- E15 release still migrates overlay table: low, rejected — same as B1.
- E16 resolveFor hardcodes empty probes: false — spec Code Map orders exactly this for V1 ("passes empty declared-absent/probe sets"); probe capture arrives with Epic 3.
- V1 transport-pinned fallback (pre-verified): matching claim false — same evidence as B3/E3; the validator half (pin required fallback network) is adopted under group G4.
- V2 verifyMatrix passes fixtures the runtime rejects (pre-verified): mirror imprecision verified, patch, group G1 — trailing content and missing row-version pass the task while `parseAsset` throws; `MatrixAssetTest` backstops the shipped file with the real validator, so the ship-past-check consequence needs both gates to fail, but the mirror must still be hardened.
- V3 paired-card firmware-rematch wiring untested (pre-verified): patch, group G7 — `LaunchedEffect(records)` re-subjecting exists (`MainActivity.kt:236-247`) but no executing test drives record→subject→label; guard source-text assertions do not count as execution.

Groups: G1 verifyMatrix hardening (`app/build.gradle.kts`); G2 harness correctness (`MatrixHarnessScreen.kt`, `MainActivity.kt` wiring); G3 tracker lifecycle (`CapabilityLabelTracker.kt` flow cache + forget path); G4 fallback comment/validator/test (`CapabilityMatrix.kt`, `CapabilityMatrixTest.kt`); G5 duplicate-key guard (`CapabilityMatrix.kt` parser + test); G6 promotion doc (`docs/matrix-promotion.md`); G7 pipeline test (record→subject→label).

## Design Notes

Resolver is a total function `CapabilityInput(keys, assetRows, overlayRows?, declaredAbsent, probeResults, model, firmware, networkType, generation, matrixVersion) -> Map<CapabilityKey, CapabilityLabel>`. Release and debug share it; only the row source differs. Copy mapping lives in `:app`, never in `:core`: `Works -> "Full control — confirms keys"`, all others -> "Best-effort — can't confirm commands" for the card; the four-value label stays available for Epic 2 details.

## Verification

**Commands:**
- `./gradlew build lint :core:test` -- expected: BUILD SUCCESSFUL, 0 lint errors, JVM tests pass.
- `./gradlew :app:testDebugUnitTest` -- expected: owner/validator/harness-copy tests pass.
- `rg -n "okhttp3|org\\.jupnp|KEY_" app/src/main` -- expected: no hits.
- `rg -n "println|android\\.util\\.Log" app/src/main tv/src/main core/src/main --glob '!*RedactedLog*'` -- expected: no hits outside the sink.

**Manual checks (if no CLI):**
- Owned TV: paired summary shows best-effort plate on fallback-only asset; debug harness Save flips the plate after Save; release smoke confirms harness route absent and card reads asset only; `adb logcat` shows no token, pin, SPKI, MAC, or URL.
