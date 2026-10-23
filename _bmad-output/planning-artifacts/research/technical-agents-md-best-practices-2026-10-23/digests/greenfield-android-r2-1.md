# Digest — greenfield and Android specifics (round 2, assistant 2)

Lane: greenfield + Android/Kotlin specifics. Budget used: ~10 sources / ~18 calls. Accessed 2026-10-23.

## Findings

1. **A greenfield seed file should be written at clone time, but only as a placeholder scaffold, not a full specification.** — source: https://nyosegawa.com/en/posts/agents-md-generator/ | publisher: Sakasegawa (practitioner blog) | published: 2026-02-15 | accessed: 2026-10-23 | confidence: high | class: guidance
   - Argues that fully specifying build commands, tests, and conventions "before any code exists is a fruitless exercise," but leaving the file empty makes the agent "grope around blindly."
   - His generator writes `AGENTS.md` (plus a `CLAUDE.md` symlink) the moment an empty repo is cloned, with `Project Overview`, `Commands`, `Code Conventions`, and `Architecture` left as placeholders.
   - Empty-repo detection is "fewer than 3 entries at the repo root (excluding `.git`)"; repos with code or an existing `AGENTS.md` are left alone.

2. **Only information the agent cannot discover from the code earns a line; auto-generated codebase overviews are redundant and can hurt.** — source: https://addyosmani.com/blog/agents-md/ | publisher: Addy Osmani (Google, practitioner) | published: 2026-02-23 | accessed: 2026-10-23 | confidence: high | class: evidence
   - Summarizes the ETH Zurich study: LLM-generated context files reduced task success by 2–3% while increasing cost by over 20%; developer-written files improved success by about 4%, but also increased cost by up to 19%.
   - Reports that 100% of Sonnet 4.5 and 99% of GPT-5.2 auto-generated files contained codebase overviews — exactly what the agent can discover by listing directories and reading the repo.
   - Concrete positive example: mentioning `uv` raised its use from fewer than 0.01 times per task to 1.6; other repo-specific tools went from fewer than 0.05 to 2.5 uses per task.
   - Practical filter: "can the agent discover this on its own by reading your code? If yes, delete it."

3. **The seed should use explicit placeholder markers and a permanent maintenance section, then delete placeholders as facts become true.** — source: https://nyosegawa.com/en/posts/agents-md-generator/ | publisher: Sakasegawa (practitioner blog) | published: 2026-02-15 | accessed: 2026-10-23 | confidence: high | class: guidance
   - Placeholders use `[To be determined]` or `[Add your ... here]`; once a section is filled, the placeholder comment is deleted because it still consumes budget.
   - A `## Maintenance Notes` section is explicitly marked permanent with an HTML comment, instructing agents to remove placeholder sections, review regularly, keep the file under 20–30 lines, update commands immediately, rewrite architecture after major changes, and delete anything inferable.
   - A top-of-file HTML comment ("Do not restructure or delete sections") guards against agents rewriting the file structure.
   - Claims a 20–30 line budget because frontier models reliably follow roughly 150–200 instructions total, most of which the system prompt already consumes.

4. **Use the Gradle wrapper, never a system Gradle installation; commit the wrapper files including the JAR.** — source: https://docs.gradle.org/current/userguide/gradle_wrapper.html | publisher: Gradle | published: unknown (Gradle 9.8.2 docs) | accessed: 2026-10-23 | confidence: high | class: guidance
   - "The recommended way to execute any Gradle build is with the help of the Gradle Wrapper"; `./gradlew build` invokes the declared Gradle version rather than the installed `gradle`.
   - The wrapper standardizes the Gradle version across developers, IDEs, and CI; `gradle-wrapper.properties` stores the distribution URL/version.
   - Wrapper files, including `gradle-wrapper.jar`, are expected to be checked into version control; a new project gets them via `gradle init` or `gradle :wrapper`.
   - Downloads are cached in `GRADLE_USER_HOME`, so subsequent builds reuse the distribution until `distributionUrl` changes.

5. **Versions and plugin coordinates belong in `gradle/libs.versions.toml`, referenced through generated `libs.*` accessors.** — source: https://docs.gradle.org/current/userguide/version_catalogs.html | publisher: Gradle | published: unknown (Gradle 9.8.2 docs) | accessed: 2026-10-23 | confidence: high | class: guidance
   - `gradle/libs.versions.toml` is imported automatically; no `settings.gradle` registration is needed for the default `libs` catalog.
   - The TOML has `[versions]`, `[libraries]`, `[bundles]`, and `[plugins]` sections; dependencies are referenced as `libs.<alias>` and plugins as `libs.plugins.<alias>`.
   - Aliases with dashes map to nested accessors (`ktor-client-core` → `libs.ktor.client.core`), so casual renaming can break build scripts.
   - The catalog centralizes version declarations but does not enforce them during conflict resolution.

6. **On AGP 9.0+, Kotlin support is built in: do not apply `kotlin-android`, do not use `kotlinOptions`, and replace `kapt` with KSP.** — source: https://developer.android.com/build/migrate-to-built-in-kotlin | publisher: Android Developers (Google) | published: unknown | accessed: 2026-10-23 | confidence: high | class: guidance
   - AGP 9.0 enables built-in Kotlin by default for modules where AGP is applied; applying `org.jetbrains.kotlin.android`/`kotlin-android` now fails with "Cannot add extension with name 'kotlin'" or a message that the plugin is no longer required.
   - Remove the plugin from module build files, the root build file, and the version catalog `[plugins]` entry if present.
   - `org.jetbrains.kotlin.kapt` is incompatible with built-in Kotlin; migrate to KSP, or temporarily use `com.android.legacy-kapt` at the same version as AGP.
   - Migrate `android.kotlinOptions {}` to `kotlin { compilerOptions {} }`; with built-in Kotlin, `jvmTarget` defaults to `android.compileOptions.targetCompatibility`.
   - `kotlin.sourceSets {}` is unsupported; add Kotlin source directories through `android.sourceSets {}` or the variant API.

7. **Build variants are the Cartesian product of product flavor and build type, and their names determine task names.** — source: https://developer.android.com/build/build-variants | publisher: Android Developers (Google) | published: unknown | accessed: 2026-10-23 | confidence: high | class: guidance
   - `debug` and `release` build types are created automatically; with a `demo` flavor, Gradle creates `demoDebug` and `demoRelease` variants named `<product-flavor><Build-Type>`.
   - Flavors must belong to a named flavor dimension; multiple dimensions combine, and dimension order affects resource/configuration priority.
   - The page documents variant configuration and signing, not the full test/lint task matrix. Project-specific command lists must come from the project, not the generic page.

8. **Kotlin 2.0+ requires the separate Compose Compiler Gradle plugin, versioned in lockstep with Kotlin.** — source: https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler | publisher: Android Developers (Google) | published: 2026-10-16 | accessed: 2026-10-23 | confidence: high | class: guidance
   - The plugin is `org.jetbrains.kotlin.plugin.compose` and is only available from Kotlin 2.0+.
   - In a version catalog, add `compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }` and remove any reference to the old Compose compiler.
   - Apply it in every module that uses Compose; `composeCompiler {}` configures reports/stability options.
   - Compose 1.12.0+ requires `compileSdk 37` and AGP 9; the page currently recommends Compose BOM `2026.09.00`.

9. **`nowinandroid`'s AGENTS.md is a mature-repo command sheet: strong on exact variants and test libraries, silent on toolchain versions, secrets, and codegen.** — source: https://github.com/android/nowinandroid/blob/main/AGENTS.md | publisher: Google/Android (`android/nowinandroid`) | published: unknown (main branch) | accessed: 2026-10-23 | confidence: high | class: practice
   - Includes: project description; architecture stack (Compose/M3, UDF with Coroutines/Flow, Hilt, Navigation 2, Room/DataStore, Retrofit/OkHttp, WorkManager); module map (`app/`, `feature/`, `core/`); build variants (`demo`/`prod` × `debug`/`release`); commands (`./gradlew assemble{Variant}`, "Typically `assembleDemoDebug`"; `spotlessApply`; `{variant}Test`; single test with `--tests`; `verifyRoborazziDemoDebug`); Gradle-managed-device tasks (`pixel6api31aospDebugAndroidTest`, etc.); test-authoring conventions (ComposeTestRule + ComponentActivity, kotlinx-coroutines, Turbine, Truth); CI/screenshot policy; git location.
   - Omits: JDK/Gradle/AGP/Kotlin versions; wrapper policy; version-catalog editing rules; `local.properties`, keystores, signing, `google-services.json`; Compose compiler plugin alignment; KSP vs kapt; Hilt/Room codegen; module dependency boundaries; lint task; emulator/SDK setup; commit/PR rules; any date or version stamp.
   - Non-inferable: exact GMD device task IDs, `verifyRoborazziDemoDebug`, the `{variant}Test` pattern in a flavored build, "typically `assembleDemoDebug`," the chosen test libraries, and the CI rule that screenshot tests must not be checked in from a workstation.
   - Restating the codebase: "written in Kotlin," single-activity, Compose/M3/Hilt/Navigation/Room/Retrofit/WorkManager, the `app/feature/core` folder map, and the GitHub URL are all discoverable from build files and directory listings.
   - Staleness risk: it says "Navigation 2" while current Android projects may use Navigation 3, and the file carries no date or tool-version pin.

10. **Meshtastic's AGENTS.md is a routing layer: a thin root file pointing to `.skills/*`, with an exact baseline verification command and explicit secret/memory hygiene.** — source: https://github.com/meshtastic/Meshtastic-Android/blob/main/AGENTS.md | publisher: Meshtastic project | published: unknown (main branch) | accessed: 2026-10-23 | confidence: high | class: practice
    - Root file defines role, project goal, `.agent_memory/` as local-only/git-ignored scratch capped at ~5 entries, and a list of skills to consult first (project overview with bootstrap steps, KMP architecture, Compose UI, design standards, navigation/DI, testing/CI, feature workflow, code review, branching, Spec Kit).
    - Process rules: run mandatory bootstrap before any build; baseline is `./gradlew spotlessApply spotlessCheck detekt detektTypeResolved assembleDebug test allTests`; zero lint tolerance; verify before push because "CI has failed repeatedly due to skipped local checks."
    - Architecture rules: never import `java.*`/`android.*` in `commonMain`; prefer Compose Multiplatform APIs; do not hand-edit generated protobufs; never touch secrets; never log PII/location/keys.
    - Includes legal/rebrand constraints and says `AGENTS.md` is the source of truth while `CLAUDE.md`/`GEMINI.md` redirect here.
    - Non-inferable: baseline command, bootstrap requirement, memory cap/path, skill routing, protobuf upstream pinning, rebrand/license obligations.
    - Omits: JDK/Gradle/AGP versions, version-catalog edit policy beyond the protobuf pin, `local.properties`/keystore handling, Compose compiler alignment, build-variant matrix, emulator setup.

11. **`local.properties` is machine-local SDK configuration and must be excluded from source control.** — source: https://developer.android.com/build/android-build-structure | publisher: Android Developers (Google) | published: 2026-02-26 | accessed: 2026-10-23 | confidence: high | class: guidance
    - The build-structure table defines `local.properties` as "Local machine configuration" containing properties such as the Android SDK location, with the explicit instruction: "Exclude this file from source control!"
    - The same table says `.gradle/` is Gradle-managed project cache and "Don't change files in this directory!"; `gradlew`/`gradlew.bat` bootstrap the build without a preinstalled Gradle.
    - The table defines `gradle/libs.versions.toml` as the version catalog ensuring consistency across subprojects.

12. **Signing material must be loaded from local, untracked properties — not written into `build.gradle.kts`.** — source: https://developer.android.com/build/build-variants | publisher: Android Developers (Google) | published: unknown | accessed: 2026-10-23 | confidence: medium | class: guidance
    - The signing section shows a `signingConfigs` block with `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`; the page's guidance is to load the keystore from a local properties file and, "for security reasons, don't add this file to source control."
    - This establishes the instruction-file rule: passwords and keystore paths belong in an untracked local file or environment/CI secret, never in a tracked Gradle file.

## Answers to the six questions

- **Q1:** One practitioner source addresses greenfield explicitly, and it rejects both extremes. Nyosegawa says fully thinking through commands and architecture before code exists is "a fruitless exercise," but an empty file makes the agent grope blindly; his answer is to generate a minimal seed at clone time and fill it as the scaffold lands [1][3]. Addy Osmani does not discuss greenfield directly, but his evidence implies that a codebase overview written before code exists is exactly the redundant, anchoring content that hurts performance; only non-discoverable decisions and tooling facts earn a line [2]. No retrieved vendor page recommends writing a full instruction file before code, and none recommends waiting for the first successful build either. Defensible synthesis: **write a small seed now, containing decisions and constraints; defer commands until the first build; never write architecture prose about code that does not exist.** That synthesis is ours, not a quoted vendor rule.
- **Q2:** A non-aspirational seed should contain: (a) a one-paragraph purpose and the fixed stack decisions that constrain implementation (e.g., AGP 9 built-in Kotlin, Compose, Hilt/KSP, version catalog); (b) links to the spec, architecture doc, or ADRs as sources of truth, with an instruction not to duplicate or contradict them — Nyosegawa explicitly says to move detailed docs to separate files and reference them [3]; (c) planned module/package boundaries only if the spec has already fixed them; (d) commands marked with a concrete placeholder such as `[TBD after scaffold]` or `# unverified`, never presented as runnable; (e) the rule that the first post-scaffold task is to run the build, replace every placeholder with the verified command, and delete the placeholder comment [3]; (f) a permanent maintenance section with the 20–30 line budget, "delete anything inferable," and "update commands immediately" [3]; (g) a short "do not invent versions or tasks" rule pointing at `gradle/libs.versions.toml` and `./gradlew tasks`. Dating/versioning is **not** prescribed by any retrieved source; a `Last verified: <date> @ <commit>` line plus Gradle/AGP/Kotlin versions is reasonable, but label it as our convention, not vendor guidance.
- **Q3:** Official guidance supports these instruction lines:
  - **Wrapper vs system Gradle:** always `./gradlew`; commit `gradlew`, `gradlew.bat`, `gradle/wrapper/*`; never run a globally installed `gradle`; change the wrapper only via `./gradlew :wrapper --gradle-version …` [4].
  - **Version catalogs:** add libraries/plugins/versions to `gradle/libs.versions.toml`; reference `libs.*`; do not hardcode versions in module build files; preserve alias names because dashes become nested accessors [5][11].
  - **AGP built-in Kotlin:** on AGP 9+, do not apply `kotlin-android`; do not add it back to the catalog; use `kotlin { compilerOptions {} }`, not `android.kotlinOptions {}` [6].
  - **Build variants/tasks:** variants are `<flavor><BuildType>` (`demoDebug`); task names inherit the variant (`assembleDemoDebug`). `nowinandroid` documents `assemble{Variant}`, `{variant}Test`, and device tasks like `pixel6api31aospDebugAndroidTest` [7][9]. Generic Android docs show `assembleDebug`/`installDebug`, but a flavored project will not have those exact tasks — agents must not copy generic commands.
  - **Compose compiler:** Kotlin 2.0+ requires `org.jetbrains.kotlin.plugin.compose`; its version must match Kotlin (`version.ref = "kotlin"`); remove old Compose compiler references; apply per Compose module [8].
  - **KSP vs kapt:** kapt is incompatible with AGP 9 built-in Kotlin; new Hilt/Room codegen should use KSP, or `com.android.legacy-kapt` as a temporary bridge [6]. Hilt/Room-specific KSP configuration was not verified from official docs this session.
  - **`local.properties`/SDK:** never commit it; it holds the machine-specific SDK path [11]. **JDK pinning, daemon flags, and configuration-cache quirks were not covered by any source retrieved.** Classification: wrapper, catalogs, built-in Kotlin, variants, Compose, KSP, and `local.properties` are **official**; the exact command matrix and "typically `assembleDemoDebug`" are **practitioner/project practice** from Google's own sample repo [9].
- **Q4:** `nowinandroid` is analyzed in finding 9. It is a mature-repo file, not a seed: nearly every line assumes the code and build already exist. Its most valuable lines are the exact variant/device task names and test-library choices; its least valuable lines are the architecture-stack recital and folder map, which are discoverable. It omits every greenfield hazard: toolchain versions, secrets, codegen, and module boundaries. Meshtastic (finding 10) is the second repo: role, memory policy, skill index, bootstrap, exact baseline command, hard architectural boundaries, secret/license rules. It omits toolchain versions and local secret-file mechanics, and at ~6.4 KB it is far longer than the 20–30 line budget Nyosegawa recommends — but much of that length is legal/rebrand material a template repo would not need.
- **Q5:** **No systematic issue-report or maintainer dataset** showing agent failure rates on Android tasks was found. What exists is indirect, maintainer-authored evidence: two independent Android agent files hard-code exact Gradle tasks and warn about verification — `nowinandroid` specifies `assembleDemoDebug`, `{variant}Test`, and named GMD tasks [9]; Meshtastic specifies a full baseline command and says CI "failed repeatedly due to skipped local checks" [10]. That is **anecdote/practice**, not measured evidence. Mechanically, predictable failures: (a) using `assembleDebug` in a flavored project where only `assembleDemoDebug` exists [7]; (b) adding a separate Compose compiler version when Kotlin 2.x requires lockstep [8]; (c) applying `kotlin-android` on AGP 9 [6]; (d) inventing test/lint task names instead of reading `./gradlew tasks`. Preventive instruction: "Run `./gradlew tasks --all` and `./gradlew projects` before proposing a command; use only task names that appear; state the exact variant; report any check you did not run." Manifest permissions, Compose previews, and emulator/instrumentation failures remain **unverified**.
- **Q6:** An Android instruction file should say:
  - `local.properties`: machine-local, contains `sdk.dir`; never commit, never treat as project configuration; let each machine/CI provide it [11].
  - `keystore.properties` / `*.jks` / `*.keystore`: never commit production keystores or passwords; load signing values from an untracked local properties file or CI secrets, not from tracked `build.gradle.kts` [12].
  - `google-services.json`: do not assume it is a keystore. Meshtastic tracks a placeholder file and keeps real secrets out of git [10]. The official Firebase position — API keys are not secret but should be restricted — was **not retrieved**, so the instruction should state the project's policy explicitly rather than rely on a blanket "never commit."
  - `.gradle/`, `build/`, `.kotlin/`: Gradle-managed/generated; never edit or commit [11]. `~/.gradle` is outside the repo and is the wrapper distribution cache (`GRADLE_USER_HOME`) [4]; never copy it into the repo.
  - Also: "If a command needs a secret, stop and ask; never print or commit the value."

## Contradictions

- **Seed now vs wait:** Nyosegawa says pre-code specification is fruitless yet generates a seed at empty clone [1][3]; Addy says codebase descriptions are redundant and harmful [2]. These reconcile only if "seed" means decisions/placeholders, not architecture prose or invented commands.
- **AGENTS.md helps vs hurts:** Addy reports one study where human-written files cut runtime 28.6% and tokens 16.6%, and another where generated files cut success 2–3% while raising cost 20%+ [2]. The variable is content provenance, not file existence.
- **Length:** Nyosegawa recommends 20–30 lines [3]; Meshtastic's root file is ~6.4 KB [10]. The routing-layer pattern partly resolves this, but Meshtastic's root is still far above the stated budget.
- **Generic vs project task names:** Android's generic CLI docs use `assembleDebug`/`test`/`connectedAndroidTest`, while a flavored project exposes `assembleDemoDebug`/`{variant}Test`/named GMD tasks [7][9]. Copying the generic command is a predictable failure.
- **Version-dependent Kotlin setup:** Official AGP 9 guidance says remove `kotlin-android` [6], while many current templates and older docs still apply it. The instruction file must pin the AGP major version or the rule is ambiguous.
- **Staleness:** `nowinandroid` says Navigation 2 [9], while current Android work may use Navigation 3 [10] — evidence that undated architecture recitals decay.

## Gaps

- **No vendor page specifically addresses greenfield repositories.** The closest is a practitioner essay [1]; round 1's conclusion stands.
- **No source explicitly recommends dating/versioning AGENTS.md.** The `Last verified` convention in Q2 is inference.
- **JDK pinning, Gradle daemon settings, and configuration-cache behavior** were not covered by any retrieved source; do not treat recommendations as official.
- **Hilt/Room KSP configuration details** are not verified; only the general "kapt is incompatible, migrate to KSP" rule [6].
- **Official Firebase guidance on `google-services.json`** was not retrieved; the placeholder practice is from Meshtastic [10].
- **Exact `testDebugUnitTest`, `connectedDebugAndroidTest`, and `lintDebug` task names** were not found on the official pages fetched. Standard AGP conventions and consistent with `nowinandroid`'s `{variant}Test` pattern [9], but not verified verbatim.
- **No failure-rate or issue-report evidence** for agents on build variants, Compose previews, manifest permissions, or emulator/instrumentation. Q5's failure mechanisms are inference plus maintainer anecdote, not measured data.
- **Repository files are undated and may change**; both AGENTS.md analyses reflect `main` as fetched 2026-10-23.
