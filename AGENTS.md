<!-- bmad:context -->
<!-- Verified 2026-10-23 against c38fdbb. Managed by bmad-project-context; edits inside this block are replaced on refresh. Keep anything you want preserved outside the markers. -->

# UniRem

Native Kotlin Android remote for Samsung Tizen TVs (2016+ committed; 2014–15 best-effort). Four Gradle modules, one acyclic graph. No source exists yet: planning is final and implementation starts with the Spike A gate in Epic 1.

## Policy

- Never add or remove a Gradle module, or change the dependency direction. The graph is `:app → :tv, :cast, :core`; `:tv → :core`; `:cast → :core`. Change it only through an architecture decision in the spine.
- `:app` never imports `okhttp3`, `org.jupnp`, `KEY_*`, or a protocol frame type. Cross-module declarations live only in each module's `api` package.
- Never log a token, pin, SPKI, text payload, cast media ID or URL, or full MAC. Use `RedactedLog`; no `println` or direct `Log` call.
- Never commit `local.properties`, `keystore.properties`, `*.jks`, `*.keystore`, or any signing material. Pairing material lives only in Keystore, backup-excluded.
- No telemetry, analytics, crash-reporting, or remote-config SDK in V1.
- No WSS→WS fallback and no trust-all `X509TrustManager`. Validate the pinned leaf SPKI before attaching a token.
- Capability claims come only from a recorded matrix pass for the exact model, firmware, and `NetworkType`. Never promote a label from a model table, probe, or user confirmation.
- Do not edit `_bmad/`, `.agents/`, or `.opencode/`; customize through `_bmad/custom/`. Do not rewrite finalized planning artifacts to match code — change the plan through BMad.

## Where things are

- Canonical spec: `_bmad-output/unirem/spec-samsung-remote/spec-samsung-remote.md`; protocol reference and verification matrix sit beside it.
- Architecture law: `_bmad-output/planning-artifacts/architecture/architecture-UniRem-2026-10-23/ARCHITECTURE-SPINE.md`
- UX contract: `_bmad-output/unirem/ux-samsung-remote/DESIGN.md` and `EXPERIENCE.md`
- Epic and story detail: `_bmad-output/planning-artifacts/epics.md` (large; read only the relevant story). Epic 1 is the only elaborated epic.
- Research behind these agent instructions: `_bmad-output/planning-artifacts/research/technical-agents-md-best-practices-2026-10-23/research.md`
- `tizen-receiver/` is a separate Tizen web app, not a Gradle module.

## Running and verifying

- Decided toolchain: Gradle 9.5.0, AGP 9.3.1, JDK 17, Kotlin 2.4.21. No wrapper or source exists yet, so every command below is `[TBD after scaffold]` until Story 1.3 lands.
- `[TBD after scaffold]` `./gradlew :core:test` — JVM tests.
- `[TBD after scaffold]` `./gradlew :app:assembleDebug` and `./gradlew lintDebug`.
- `[TBD after scaffold]` `./gradlew :app:connectedDebugAndroidTest` — instrumentation, owned-device only.
- First scaffold task: run `./gradlew projects` and `./gradlew tasks --all`, then replace every TBD with a verified invocation and add `Verified <date> @ <commit>` here.
- Use the wrapper (`./gradlew`), never a system `gradle`; commit `gradlew`, `gradlew.bat`, and `gradle/wrapper/*`. Versions live only in `gradle/libs.versions.toml`.
- AGP 9 has built-in Kotlin: do not apply `kotlin-android`; use `kotlin { compilerOptions {} }`, not `android.kotlinOptions {}`; use KSP, not kapt.
- The Compose Compiler plugin (`org.jetbrains.kotlin.plugin.compose`) tracks the Kotlin version.
- CI runs the Gradle build, lint, and `:core` JVM tests. Instrumentation and matrix runs are manual on owned TVs.

## Conventions that differ from defaults

- One `submit()` per user-expressed command. Repeat frames and synthesized press/release never submit. A socket write is not success: `CommandOutcome` is `Submitted`, `Unconfirmed`, `Observed`, or `Idle`; no boolean named `confirmed`.
- `TvId` is the hash of `StableIdentity`, never an IP. `SessionId` is UUIDv4. `MediaId` is minted by `:cast`. Use `Instant` UTC at every boundary; firmware strings are byte-exact and never normalized.
- One writer per state object. Expose `StateFlow`; never let a `MutableStateFlow` escape.
- One sealed failure type per module (`TvFailure`, `CastFailure`, `MirrorFailure`); no exception crosses a module boundary. User copy lives in `:app`.
- Names: `Gateway`, `Control`, `Tracker`, `Session`, `Health`, `Resolver`, `Mapper`. `Manager`, `Helper`, and `Util` are banned except `RedactedLog`.
- Tests: `:core` is JVM-only. Pure logic in `:tv` and `:cast` is JVM-tested. Keystore, sockets, and MediaProjection are instrumentation-only. No test asserts a socket write as success.

<!-- /bmad:context -->
