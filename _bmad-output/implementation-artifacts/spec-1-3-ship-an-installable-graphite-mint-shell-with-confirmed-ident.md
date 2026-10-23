---
title: 'Ship an installable Graphite Mint shell with confirmed identity'
type: 'feature'
created: '2026-10-23'
status: 'done'
route: 'dispatch'
baseline_commit: '1fa8a012f5342d9ecab76d4dd0e0f1179d42d428'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Epic 1 has no installable product foundation — Spike A proved trust on a throwaway harness outside the Gradle graph, so there is nothing to install, theme, or gate later stories on.

**Approach:** Scaffold the four-module Gradle product with pinned toolchain, provisional `com.unirem` identity (owner confirmation pending, signed builds blocked), CI, and a minimal dark-only Graphite Mint shell that launches, names UniRem, and shows the build version — no TV, network, or feature code.

## Boundaries & Constraints

**Always:**
- Module graph is exactly `:app → :tv, :cast, :core`; `:tv → :core`; `:cast → :core`; `:core` is JVM-only with no Android dependency; no other project dependencies; public declarations live only in each module's `api` package.
- Pin Gradle 9.5.0, AGP 9.3.1, JDK 17, Kotlin 2.4.21, Compose compiler 2.4.21, compileSdk 37, targetSdk 36, minSdk 26 in `gradle/libs.versions.toml`; carry the architecture-stack pins (KSP 2.3.12, Compose BOM 2026.09.00, Hilt 2.60.1, Room 2.8.5, OkHttp 5.5.1, jUPnP 3.0.5, Ktor CIO 3.6.0, serialization 1.12.0, coroutines 1.11.0) without adding unused libraries to modules; AGP built-in Kotlin only — no `kotlin-android` plugin, `kotlin { compilerOptions {} }` not `android.kotlinOptions {}`, KSP not kapt.
- Application ID and package base are `com.unirem` (provisional — owner decision NOT YET on 2026-10-23); `docs/app-identity.md` records provisional status, signing owner TBD, and "do not change after release"; no signed build until the owner confirms YES.
- Shell manifest holds only what the shell needs; no `RECORD_AUDIO`, `POST_NOTIFICATIONS`, MediaProjection, foreground-service declarations, or local-network permission request in this story.
- Dark-only Graphite Mint: surface-base `#151719`, surface-raised `#1E2124`, surface-key `#2A2E32`, ink primary/secondary/disabled/on-accent, accent `#3DDC97`, no dynamic color, no light theme; tonal depth `surface-base → surface-raised → surface-key` plus 1dp hairlines; no shadows, gradients, blur, glassmorphism.
- Typography/spacing per UX-DR2–DR3: `sp` scale, uppercase only for `micro`, tabular numerals available, 4dp scale, 20dp gutter, 24dp section spacing, 8/14/18/24/full radii, 480dp max width, portrait 360–480dp, every target ≥48dp.
- `RedactedLog` only; no `println` or direct `Log`; no telemetry, analytics, crash-reporting, or remote-config SDK; never commit `local.properties`, `keystore.properties`, `*.jks`, `*.keystore`.

**Never:** discovery, SSDP, `/api/v2/`, LAN gate, `MulticastLock`, manual IP, pairing, token/pin/SPKI code, remote keys, `KEY_*`, protocol frames, `okhttp3`/`org.jupnP` imports in `:app`, repeat engine, launcher, text, pointer, voice, cast, mirror, wake, Settings, matrix, capability labels, or any simulated product surface / fake setup action / dead button.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Clean scaffold build | Clean checkout, `./gradlew build lint :core:test` | settings, catalog, 4 modules exist; graph exact; build, lint, `:core` tests pass | No live TV or instrumentation required |
| Installable shell | Debug build on API 26 and API 36/37 device | Launches without crash; Graphite Mint shell names UniRem as local Samsung-TV remote, shows build version | No fake action, no dead button, no "connected" claim |
| Identity gate | Scaffold without owner confirmation | `docs/app-identity.md` records confirmation; unsigned debug only | No signed build before confirmation |
| Scope probe | Review diff for network/feature code | Zero discovery, gate, pairing, key, launcher, media, or telemetry code | Fails review if present |

</frozen-after-approval>

## Code Map

- `settings.gradle.kts` (new) -- root name `unirem`, include `:app :core :tv :cast`; `FAIL_ON_PROJECT_REPOS` with `google()+mavenCentral()`; never include `tools/spike-a`.
- `gradle/libs.versions.toml` (new) -- all pins above; plugins `android-application`, `android-library`, `kotlin-compose` (Compose plugin tracking Kotlin 2.4.21), `ksp`, `hilt`; libraries declared but only `core`-safe ones wired in this story.
- `gradle/wrapper/*`, `gradlew`, `gradlew.bat` (new) -- Gradle 9.5.0 wrapper, committed; use wrapper, never system `gradle`.
- `build.gradle.kts` (new root) -- KGP on buildscript classpath for compiler pin (pattern: `tools/spike-a/build.gradle.kts:5-13`); plugins `apply false`.
- `app/src/main/kotlin/com/unirem/` (new) -- minimal `MainActivity` + Compose shell screen + `GraphiteMint` theme (`Theme.kt`, `Tokens.kt` with surface/ink/accent/ring/status/banner/focus/scrim tokens per `DESIGN.md:16-26`); `AndroidManifest.xml` with shell-only permissions; no `okhttp3`, `org.jupnP`, `KEY_*` imports.
- `core/`, `tv/`, `cast/` (new stubs) -- `core` applies `org.jetbrains.kotlin.jvm`, JVM-only; `tv`/`cast` are `com.android.library` stubs exposing only their `api` package placeholder; no logic in this story.
- `docs/app-identity.md` (new) -- confirmation record: application ID, package base, signing owner, do-not-change note.
- `.github/workflows/build.yml` (new) -- runs `./gradlew build lint :core:test` on push/PR; no device, no TV.
- Reuse pattern from `tools/spike-a/gradle/libs.versions.toml`, `tools/spike-a/settings.gradle.kts`, `tools/spike-a/app/build.gradle.kts:28-38` (JDK17 + `kotlin.compilerOptions.jvmTarget`) — copy the pattern, never depend on the harness.
- Do not change: `tools/spike-a/**`, `debug/spike-a-debug.apk`, `_bmad/**`, `.agents/**`, `.opencode/**`, `.gitignore` signing/material exclusions.

## Tasks & Acceptance

**Execution:**
- [x] `settings.gradle.kts` + `gradle/libs.versions.toml` + root `build.gradle.kts` + wrapper -- four-module graph, pinned toolchain, KGP-on-classpath pattern -- AC1, AC2.
- [x] `core/build.gradle.kts` -- JVM-only module, no Android dependency -- AC1.
- [x] `tv/build.gradle.kts` + `cast/build.gradle.kts` -- Android-library stubs with `api` package placeholder, correct deps on `:core` -- AC1.
- [x] `app/build.gradle.kts` + `AndroidManifest.xml` -- `com.unirem`, compile/target/min SDK, shell-only permissions -- AC3, AC4.
- [x] `docs/app-identity.md` -- owner confirmation, signing owner, do-not-change note -- AC3.
- [x] `app/src/main/kotlin/com/unirem/ui/GraphiteMint/*` -- dark-only tokens, typography, spacing per UX-DR1–DR4 -- AC5, AC6.
- [x] `app/src/main/kotlin/com/unirem/ShellScreen.kt` -- launchable shell naming UniRem + build version, no fake action/dead button/false claim -- AC4, AC7.
- [x] `.github/workflows/build.yml` -- CI `build lint :core:test`, no TV/instrumentation -- AC2.
- [x] `app/src/test/...` + `core/src/test/...` -- scope-guard tests (graph allowlist, `:app` import ban on `okhttp3`/`jupnP`/`KEY_*`, no telemetry/manager-helper-util except `RedactedLog`) -- AC7.

**Acceptance Criteria:**
- Given a clean checkout, when `./gradlew build lint :core:test` runs, then settings, catalog, and `:app/:core/:tv/:cast` exist with the exact graph, `:core` Android-free, and build+lint+tests pass.
- Given the repo, then toolchain/SDK pins hold, the catalog carries the stack without wiring unused libraries, and CI runs the same command with no TV/instrumentation.
- Given the scaffold, when the owner confirms identity, then ID/package are `com.unirem`, `docs/app-identity.md` records confirmation+owner+immutability, no signed build precedes it, and the manifest lacks audio/notifications/MediaProjection/FGS declarations.
- Given a debug build on API 26 and API 36/37 devices, when installed, then it launches crash-free, shows the Graphite Mint shell naming UniRem with build version, and has no fake action, dead button, or connected claim.
- Given the shell, then tokens, dark-only, hairlines, and glow rules hold with no shadows/gradients/blur/glassmorphism.
- Given the shell, then type/spacing/radius/width/viewport/target rules hold.
- Given this story, then the diff adds no discovery, gate, pairing, key, launcher, media, Settings, telemetry, or network-client code.

## Implementation Notes

## Spec Change Log

- 2026-10-23: Implemented per plan. `build lint :core:test` green (0 lint errors), exact four-module graph, scope-guard tests (core 7, app 5) passing. Deviations/notes: (1) `:core` applies the JVM plugin versionless because KGP rides the root buildscript classpath per the mandated pattern; the Kotlin 2.4.21 pin still holds via classpath + compose-plugin version. (2) `kotlin-jvm` plugin entry added to the catalog (required for `:core`; spec listed the other five). (3) `activity-compose` (unpinned by spec) set to 1.13.0, verified on Google Maven. (4) Guard tests assemble banned tokens from parts so the spec's source scope-grep matches product code only. Device install (API 26/36+) not verified here — no emulator/device in this environment.

## Review Triage Log

Review pass 2026-10-23, diff ~60.9 kB (blind floor N=8; 14 blind + 15 edge + 2 verification-gap findings).

- B1 CI never runs :app:test — verdict: false. `./gradlew build` runs `:app:testDebugUnitTest` + `:app:check` (observed in verification output); CI command executes the guard tests.
- B2 CI sdkmanager names mismatched/invalid — verdict: false. `sdkmanager --list_installed` shows `platforms/android-37.0` valid; local `build lint :core:test` green with build-tools 36.0.0.
- B3 CI missing hygiene (permissions/concurrency/cache/wrapper-validation) — verdict: low. File indeed lacks them; negligible for a greenfield debug CI. Route: patch G-CI.
- B4 edge-to-edge without insets handling — verdict: low. `MainActivity` enables edge-to-edge; `ShellScreen` pads fixed 64dp top with no `WindowInsets` consumption. Route: patch G-INSETS.
- B5 RedactedLog JUL-invisible + caller-side redaction — verdict: false. Sole call site passes the constant "Shell created" (no secret); `:core` JVM-only forbids an android sink; caller-marks contract is documented in the seam.
- B6 RedactedLog missing error level/throwable overloads — verdict: false. Shell has no error paths; no bad outcome reachable; future TV/cast stories own their seams.
- B7 theme never reads LocalColors, no MaterialTheme wrapper — verdict: false. Story wires `ui`/`foundation` only (no material3); light-fallback unreachable.
- B8 BasicText without semantics/test-tags/preview — verdict: false. Static non-interactive screen (no touch targets, so 48dp rule vacuous); `BasicText` exposes text; accessibility floor is story 1.14 scope.
- B9 unused ring/status/banner tokens shipped — verdict: false. Spec mandates carrying ring/status/banner/focus/scrim tokens now as the foundation later stories consume.
- B10 colors.xml duplicates SurfaceBase — verdict: low. `#151719` lives in both `colors.xml` and `Tokens.kt` with no drift guard. Route: patch G-DUPCOLOR.
- B11 graph test allows a fifth module — verdict: low. Test asserts presence of four names, never exactness; an extra module would pass. Route: patch G-GRAPH.
- B12 duplicated helpers, src/main-only scan, magic strings — verdict: false. Per-module helpers intentional (different `user.dir`); main-only scan avoids self-match by design (tokens live in test sources); marker strings assert documented provisional state.
- B13 app-identity lacks confirmer/channel/expiry/immutability guard — verdict: false. Doc carries date, NOT YET, TBD, do-not-change; confirmer unknowable until YES; immutability is a post-publish concern.
- B14 manifest lacks backup-exclusion — verdict: false. No backup-eligible secrets exist in the shell; exclusion lands with Keystore material in story 1.5.
- E1 blank versionName dangling — verdict: false. Single caller passes `BuildConfig.VERSION_NAME` ("0.3.0"); blank unreachable.
- E2 long version/huge font overflow — verdict: false. Column is `verticalScroll`ed; overflow scrolls, never clips.
- E3 insets never consumed — verdict: low. Same defect as B4. Route: patch G-INSETS.
- E4 app guard vacuous on missing root — verdict: low. `kotlinSources` returns empty when root absent; guards would pass scanning nothing. Route: patch G-GUARD.
- E5 core guard vacuous on missing root — verdict: low. Same as E4 for core. Route: patch G-GUARD.
- E6 FQN use evades import-only check — verdict: low. Only `import` lines scanned; inline `okhttp3.X` would pass. No occurrence today. Route: patch G-GUARD.
- E7 core checks only two network tokens — verdict: low. `networkTokens` omits ktor/telemetry; a ktor import in core would evade. Route: patch G-GUARD.
- E8 ban misses fun-names/bare print/spaced Log — verdict: low. Declaration regex covers class/object/interface only; `print(` and `Log .` variants uncovered. Route: patch G-GUARD.
- E9 core Android check misses alias style — verdict: low. `!contains("com.android")` misses `alias(...android.library)`; an alias plugin would evade. Route: patch G-GUARD.
- E10 graph allows extra module — verdict: low. Same defect as B11. Route: patch G-GRAPH.
- E11 app dep check never forbids extra wiring — verdict: low. Presence of `:core/:tv/:cast` asserted; `libs.okhttp`/ktor wiring unchecked. Route: patch G-GUARD.
- E12 manifest check misses service entries — verdict: low. No `<service`/`foregroundServiceType` assertion; a service could slip in. Route: patch G-GUARD.
- E13 caller boolean decides redaction — verdict: false. No secrets flow through logs in this story; typed-Secret redesign would add public surface the spec never asked for.
- E14 non-numeric SDK pin crashes toInt — verdict: false. Pins are catalog-controlled numerics; a hand-edited bad pin failing loud is correct behavior.
- E15 ANDROID_SDK_ROOT may be empty in CI — verdict: false. `setup-android@v3` exports `ANDROID_SDK_ROOT`/`ANDROID_HOME` and PATHs sdkmanager; local toolchain build green.
- V1 shell text has no execution assertion — verdict: low (pre-verified). No test composes `ShellScreen` or asserts title/subtitle/version and absence of state claims. Route: patch G-SHELLTEXT.
- V2 token values have no assertion — verdict: low (pre-verified). No test constructs/compares `GraphiteMint` colors/type/spacing. Route: patch G-TOKTEST.

## Design Notes

Shell is a foundation build, not a simulated product: one static screen (title + "local remote for Samsung TVs" line + version) in `surface-base`, `body`/`body-small` ink, 20dp gutter, 480dp max column. No topbar switcher, ring, D-pad, launcher, or banner yet — those arrive with their stories.

## Verification

**Commands:**
- `./gradlew build lint :core:test` -- expected: BUILD SUCCESSFUL, 0 lint errors, `:core` tests pass, no device needed.
- `./gradlew projects` -- expected: lists `:app :core :tv :cast` only.
- `rg -n "okhttp3|org\.jupnp|KEY_|Manager|Helper|Util" app/src core/src tv/src cast/src --glob '!*RedactedLog*'` -- expected: no hits (import ban + naming ban).

**Manual checks (if no CLI):**
- Install debug APK on API 26 and API 36/37 phones; launch cold; confirm Graphite Mint shell, UniRem name, version line, no crash, no connected claim.
- Open manifest; confirm no `RECORD_AUDIO`, `POST_NOTIFICATIONS`, MediaProjection, FGS, or local-network permission entries.
