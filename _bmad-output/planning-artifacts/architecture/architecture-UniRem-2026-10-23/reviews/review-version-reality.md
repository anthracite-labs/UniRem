# Review — Version & Reality Check

- **Reviewer role:** version/reality reviewer (architecture spine reviewer gate)
- **Target:** `_bmad-output/planning-artifacts/architecture/architecture-UniRem-2026-10-23/ARCHITECTURE-SPINE.md`
- **Date:** 2026-10-23
- **Question:** was every committed technical decision web-researched / reality-checked rather than asserted, and are the live defaults of the stack confirmed?
- **Method:** every Stack-table version verified against release registries (Gradle releases page, Android developer docs release notes, Kotlin docs release/compatibility tables, Google Maven + Maven Central metadata, project GitHub releases). Protocol and platform claims verified against current first-party docs and maintained OSS implementations. Nothing was taken from model memory alone.

## Verdict

**pass-with-fixes**

The spine is unusually well reality-anchored: every pinned artifact exists, nearly every pin is the current stable release (several published in the last 8 weeks: OkHttp 5.5.1 on 2026-10-22, Kotlin 2.4.21 on 2026-10-08, jUPnP 3.0.5 on 2026-08-12), and the protocol/platform claims (Tizen 8001/8002 + token pairing, AVTransport, WoL, Android 17 local-network permission, NSC range limitation) all hold against current sources. One committed number is build-breaking (`compileSdk 36`) and two Kotlin/AGP wording/compatibility points need correction. All fixes are local edits to the Stack table plus one AD note — no architectural change.

## Findings

| # | Severity | Section / row | Issue | Fix |
| --- | --- | --- | --- | --- |
| 1 | high | `## Stack` → `compileSdk / targetSdk 36` | Cannot build the pinned stack. Compose BOM 2026.09.00 maps to Compose 1.12.x, whose release notes say "Updated Compose compileSdk to API 37 … minimum AGP version of 9.2.0"; the same note is in Lifecycle 2.11.0 and Hilt Navigation Compose 1.4.0. AAR metadata enforces `minCompileSdk 37`, so `compileSdk 36` fails at build time. | Split the row: `compileSdk 37` / `targetSdk 36`. AGP 9.4.0 supports API 37 and ≥ 9.2.0 is satisfied. Keep targetSdk 36 deliberately (it matches the Play requirement and defers the Android 17 local-network runtime permission), and say so in the AD-11 note. |
| 2 | medium | `## Stack` → `Kotlin 2.4.21 (AGP built-in; kotlin-android not applied)` | Half right, half unconfirmed. `kotlin-android` must indeed not be applied (AGP 9.0+ built-in Kotlin; applying it fails with "no longer required since AGP 9.0"). But 2.4.21 is not what AGP supplies: AGP 9.0.1 documents a runtime dependency on KGP **2.2.10** and you upgrade via a top-level `buildscript` classpath pin. AGP 9.4.0's release notes state no KGP version, and third-party reports for 9.2.1/9.4.1 still show bundled 2.2.10 — so the exact built-in compiler for 9.4.0 is **not confirmable from first-party docs**. | Reword to: `Kotlin | 2.4.21 (KGP pinned via top-level buildscript classpath; AGP 9.4 built-in Kotlin, so kotlin-android is not applied)`. Add: pin KSP in the same block (`com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.12`) and verify with `./gradlew buildEnvironment` after first sync. |
| 3 | medium | `## Stack` → `Android Gradle Plugin 9.4.0` + `Kotlin 2.4.21` | Pairing is past the documented support ceiling. JetBrains' KGP matrix (updated 2026-10-06) lists KGP 2.4.20–2.4.21 as fully supported with AGP **8.5.2–9.3.1**. AGP 9.4.0 is one minor beyond it (they also allow "up to latest with possible warnings"). | Either pin `AGP 9.3.1` (removes the deviation; Gradle 9.6.0 still satisfies KGP's 7.6.3–9.7.0 range), or keep 9.4.0 and record the deviation + a smoke-test in the open questions. Do not bump Gradle past 9.7.0 while on KGP 2.4.21. |
| 4 | medium | `:cast` → `bounded Ktor CIO media server` (AD-9) | Ktor does not document Android as a server platform. Ktor's server-engines table lists CIO for JVM/Native/GraalVM/JS/WasmJs; Android appears only for client engines. Android server usage is community-proven and Ktor does fix Android server bugs (3.1 changelog: "Android: 'Array has more than one element' error when starting a server"), but the platform remains officially unlisted. | Keep Ktor CIO but mark AD-9's server as a spike/risk: verify start, `HEAD`, and byte `Range` on a real device early; note `ktor-server-partial-content` as the Range mechanism, and a fallback (raw `ServerSocket`/NanoHTTPD) if the CIO engine misbehaves under an Android foreground service. |
| 5 | low | `## Stack` (omission) | The Compose Compiler Gradle plugin (`org.jetbrains.kotlin.plugin.compose`, version = Kotlin version) is still required per Compose module with built-in Kotlin; the spine doesn't mention it. Also note built-in Kotlin forces `kotlin.compilerOptions{}` (not `android.kotlinOptions{}`) and would break `kotlin-kapt` (spine uses KSP — good). | Add a Stack row `Compose Compiler plugin (org.jetbrains.kotlin.plugin.compose) 2.4.21` or a note in the structural seed's build section. |

### Non-findings worth recording (checked, no action)

- KSP 2.3.12 + Kotlin 2.4.21: supported by evidence — JetBrains' own KSP quickstart pairs Kotlin 2.4.21 with KSP 2.3.10, and KSP 2.3.10 fixed "KSP works with Kotlin 2.4.0 default module names" plus R-class resolution under AGP 9 built-in Kotlin. **Caveat:** KSP's requirements table (last touched 2026-09-30) still says KGP max `2.3.x`; that table is stale rather than correct. Low risk, re-check at implementation.
- Hilt 2.60.1 + KSP: Dagger's own KSP guide requires KSP plugin 2.3.9+ and `ksp` configuration for Dagger/Hilt 2.60+, and "Dagger KSP is officially considered stable as of 2.60". So Hilt 2.60.1 + KSP 2.3.12 is the supported path.
- Every pinned artifact exists and is at/near current stable (details below).
- Protocol facts and platform facts: confirmed, listed below with sources.

## 1. `## Stack` table — row-by-row verification (2026-10-23)

| Row | Claimed | Live status | Verdict |
| --- | --- | --- | --- |
| Gradle | 9.6.0 | Exists, released 2026-06-18; it is AGP 9.4.0's minimum **and** default Gradle. Newer: 9.6.1 (2026-06-26), 9.7.x, 9.8.0/9.8.2 (2026-10). | Confirmed. 9.6.0 is the right conservative pin for AGP 9.4.0. |
| Android Gradle Plugin | 9.4.0 | Exists (September 2026 release notes, page updated 2026-10-21). Min/default Gradle 9.6.0, min/default JDK 17, max supported API level 37. Variant-API module opt-out available until AGP 10. | Confirmed (compatibility caveat in Finding 3). |
| JDK | 17 | AGP 9.4.0: minimum 17, default 17. | Confirmed. |
| Kotlin | 2.4.21 "(AGP built-in; kotlin-android not applied)" | Kotlin 2.4.21 exists (2026-10-08, current 2.4-line bug-fix release; 2.5.0 planned Dec 2026). `kotlin-android` must not be applied with AGP 9+. Bundled KGP for AGP 9.4.0 not stated. | Exists/right version; wording and pinning unclear — Findings 2 & 3. |
| KSP | 2.3.12 | Exists, released 2026-09-09, latest release; min AGP 8.12.0; supports AGP 9 built-in Kotlin. | Confirmed. |
| Compose BOM | 2026.09.00 | Exists; newest BOM (BOM metadata latest = 2026.09.00); maps to Compose 1.12.x (compose-ui 1.12.1 is current stable, 1.13.0-beta02 in flight). | Confirmed, but see Finding 1 (`compileSdk 37`). |
| Activity Compose | 1.13.0 | Exists; current stable (1.14.0-alphas are newer). Its release notes only start requiring compileSdk 37 at 1.14.0-alpha02, so 1.13.0 itself is fine. | Confirmed. |
| Lifecycle | 2.11.0 | Exists; current stable (2.12.0-alphas newer). 2.11.0 release notes carry the Compose compileSdk-37 note. | Confirmed + Finding 1. |
| Navigation Compose | 2.10.2 | Exists; latest stable. (Navigation 3 exists as the newer direction, but 2.10.x is current and fit for one `NavHost`.) | Confirmed. |
| Hilt | 2.60.1 | Exists; latest (Maven Central metadata latest = 2.60.1). | Confirmed. |
| Hilt Navigation Compose | 1.4.0 | Exists; latest (androidx.hilt group). 1.4.0-beta01 notes carry the Compose compileSdk-37 change, so 1.4.0 stable needs compileSdk 37. | Confirmed + Finding 1. |
| Room | 2.8.5 | Exists; latest stable; no compileSdk-37 note on its release page. | Confirmed. |
| DataStore | 1.3.0 | Exists; latest stable (1.3.0 final after rc01); no compileSdk-37 note. | Confirmed. |
| OkHttp | 5.5.1 | Exists; latest (metadata lastUpdated 2026-10-22 — published the day before this review). | Confirmed. |
| kotlinx.serialization | 1.12.0 | Exists; latest stable (1.12.0 after RC). | Confirmed. |
| kotlinx-coroutines | 1.11.0 | Exists; latest stable. | Confirmed. |
| jUPnP | 3.0.5 | Exists; latest (published 2026-08-12). `org.jupnp`, `org.jupnp.support`, and the Android integration artifact `org.jupnp.android` are all at 3.0.5. Project maintained (fork of Cling), with first-party Android guidance (jupnp.org/docs/android). | Confirmed — good DLNA fit. |
| Ktor CIO | 3.6.0 | Exists; latest (2026-09-16). Server-engine platform support caveat — Finding 4. | Confirmed with caveat. |
| compileSdk / targetSdk | 36 | API 36 = Android 16, which is exactly what Play requires for new apps/updates since 2026-08-31. But Compose 1.12.x/Lifecycle 2.11.0/Hilt-navigation-compose 1.4.0 require compileSdk 37. | **Wrong for compileSdk** — Finding 1. |
| minSdk | 26 | Android 8.0; within every pinned library's floor (Compose, OkHttp 5, Ktor 3, jUPnP Android). | Confirmed. |

## 2. AGP built-in Kotlin — does the spine's statement hold?

Confirmed behaviors (first-party):

- AGP 9.0+ enables built-in Kotlin by default; applying `org.jetbrains.kotlin.android` produces an error ("The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support since AGP 9.0" / duplicate `kotlin` extension). Opt-out (`android.builtInKotlin=false`) exists but is removed in AGP 10. So **"kotlin-android not applied" is correct and mandatory**.
- AGP 9.0.1 documents a runtime dependency on KGP **2.2.10** (minimum and default). To use a higher KGP: `buildscript { dependencies { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21") } }`; to also control KSP: `classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.12")`.
- AGP 9.4.0's release notes contain **no KGP version row** (their compatibility table lists only Gradle, Build Tools, NDK, JDK). Third-party reports for AGP 9.2.1 and 9.4.1 show `kotlin-gradle-plugin:2.2.10` on the classpath until overridden. Therefore: *the exact built-in Kotlin compiler for AGP 9.4.0 is not confirmable from first-party sources at review time.*

**Defensible wording for the Stack row** (recommended):

```
| Kotlin | 2.4.21 — KGP pinned in the top-level buildscript classpath (AGP 9.4 built-in Kotlin; kotlin-android is NOT applied) |
```

with a footnote: "AGP 9.4 publishes no bundled-KGP version; AGP 9.0–9.2 bundled 2.2.10. Confirm the effective compiler with `./gradlew buildEnvironment | grep kotlin-gradle-plugin` on first sync, and pin KSP 2.3.12 in the same `buildscript` block."

Is 2.4.21 the right compiler pairing? Version-wise yes (current bug-fix release of the 2.4 line; KSP/Hilt/Compose all have 2.4-era support). Pairing-wise, 2.4.21 is documented as fully supported with AGP up to **9.3.1** and Gradle up to **9.7.0** — the spine's AGP 9.4.0 is beyond that ceiling (Finding 3).

## 3. Technology existence, maintenance, and fit

- **jUPnP 3.0.5 as the DLNA stack** — maintained and fit: 3.0.5 published 2026-08-12 (all three artifacts aligned), it is the successor to the unmaintained Cling, it explicitly documents Android usage (`AndroidUpnpServiceImpl`, `AndroidUpnpServiceConfiguration`), and its support set covers UPnP-AV/DLNA (AVTransport, RenderingControl) — what a Tizen TV's MediaRenderer exposes. No contradiction found.
- **Ktor CIO 3.6.0 as an embedded Android media server** — library is current and the engine is pure-Kotlin/coroutine-based (right call over Netty/Jetty on Android), but Android is not an officially listed server platform (Finding 4). `HEAD` and byte `Range` are served by Ktor core + `ktor-server-partial-content`; that is what AD-9 should name so "supports HEAD and byte Range" is a scheduled feature, not an assumption.
- **OkHttp 5.5.1 for WSS with a custom trust manager** — current (published 2026-10-22). OkHttp supports `sslSocketFactory(SSLSocketFactory, X509TrustManager)` for custom trust, and the AD-7 rule ("no trust-all manager, pin the leaf SPKI, no WSS→WS fallback") is implementable exactly as written. Note for the implementer: the TV's 8002 endpoint presents a self-signed certificate, so the pinned client must (a) pin the leaf SPKI and (b) accept that specific certificate — that is not a trust-all exception and should be written that way in `:tv`.
- **Hilt 2.60.1 + KSP 2.3.12 + Kotlin 2.4.21** — supported combination: Dagger documents KSP for Dagger/Hilt at 2.60+ requiring KSP plugin 2.3.9+, and Dagger KSP is stable as of 2.60. A 2026-10 field report (AGP 9.4.1 + Kotlin 2.4.21 + Hilt 2.60.1) corroborates the combination on AGP 9. Caveat: KSP's own requirements table is stale (caps at KGP 2.3.x) — treat as documentation lag, not a blocker (see non-findings).

## 4. Protocol facts

| Claim in spine | Status | Evidence |
| --- | --- | --- |
| Samsung remote channel on ports 8001 (ws) / 8002 (wss) | Confirmed | openHAB `samsungtv` binding: ">2016 TV — websocket on 8001, websocketsecure on 8002 … if the TV supports 8002 you *must* use it"; samsungctl/issues and multiple maintained integrations use the same split. |
| `wss://<tv>:8002/api/v2/channels/samsung.remote.control?name=<base64>` + `/api/v2/` device info on 8001 | Confirmed | Working reference implementations (e.g. petterhj.net notes; Hubitat community driver) use exactly this endpoint and path; device info via `http://<tv>:8001/api/v2/`. |
| Token pairing: `ms.channel.connect` event carries `data.token`; reuse via `&token=…` | Confirmed | `ms.channel.connect` response contains the token; the TV shows an allow prompt on first connect; subsequent connections pass the token. Matches AD-7's "trust-on-first-use + token" model. |
| UPnP AVTransport control of a Samsung DMR | Confirmed | Samsung TVs expose a MediaRenderer with `AVTransport1` SOAP control (see HA community logs: `http://<tv>:9197/upnp/control/AVTransport1`; UPnP AVTransport:1 service spec). jUPnP's `org.jupnp.support` covers these actions. |
| Wake-on-LAN magic packet + 8001/8002 probe | No contradiction found | WoL magic packets to the TV MAC are the standard mechanism (TV setting must be enabled); the port probe is a reasonable secondary check. Nothing current contradicts it. |

No protocol claim was contradicted by a credible current source.

## 5. Android platform facts

- **targetSdk 36** — matches Google Play's current requirement (new apps and updates must target API 36 since 2026-08-31). Android 17 = API 37 exists (AGP 9.4 supports up to API 37), so 36 is deliberately one behind the platform, not stale. Correct as targetSdk; must not be used as compileSdk (Finding 1).
- **minSdk 26** — no pinned library requires higher (Compose/OkHttp/Ktor/jUPnP all at or below 26 in practice); nothing contradicts it.
- **Local-network permission trajectory** — the spine's "API 37 enforcement" language is accurate and current: Android 17 introduces `android.permission.ACCESS_LOCAL_NETWORK`; apps **targeting API 37+ are blocked by default** and must declare and request the runtime permission for LAN traffic (smart-home/casting being the named use cases). Android 16 had an opt-in form. Consequence for the spine: with targetSdk 36 this app is not blocked today, so AD-11's gate is future-proofing — recommend the gate declare `ACCESS_LOCAL_NETWORK` and gate the runtime request on `Build.VERSION.SDK_INT >= 37` so a later targetSdk bump does not break discovery. This strengthens rather than contradicts AD-11.
- **network-security-config cannot scope cleartext by IP range** — correct: `<domain-config>` takes `<domain>` hostnames (and specific IP literals), but no CIDR/range support. The spine's design (permit cleartext broadly + a single OkHttp interceptor enforcing RFC1918/link-local, same predicate on the jUPnP transport) is the right workaround, and AD-8's framing of it as "the exception" is accurate.

## Sources consulted (primary unless noted)

- Gradle releases — https://gradle.org/releases/ ; AGP 9.4.0 release notes — https://developer.android.com/build/releases/agp-9-4-0-release-notes ; AGP 9.0.1 release notes (built-in Kotlin / runtime KGP dependency / KSP pinning) — https://developer.android.com/build/releases/agp-9-0-0-release-notes ; migrate to built-in Kotlin — https://developer.android.com/build/migrate-to-built-in-kotlin
- Kotlin releases — https://kotlinlang.org/docs/releases.html ; KGP↔Gradle↔AGP compatibility table — https://kotlinlang.org/docs/gradle-configure-project.html ; KSP overview/requirements + quickstart — https://kotlinlang.org/docs/ksp-overview.html , https://kotlinlang.org/docs/ksp-quickstart.html ; Compose compiler migration guide — https://kotlinlang.org/docs/compose-compiler-migration-guide.html ; KSP releases (2.3.10–2.3.12 notes) — https://github.com/google/ksp/releases
- Compose BOM mapping — https://developer.android.com/develop/ui/compose/bom/bom-mapping ; Compose setup + BOM 2026.09.00 example — https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler ; Compose UI release notes (compileSdk 37) — https://developer.android.com/jetpack/androidx/releases/compose-ui ; Lifecycle / Activity / Hilt release notes — androidx release pages
- Version existence/timestamps — Google Maven `maven-metadata.xml` (compose-bom, activity-compose, lifecycle-runtime-ktx, navigation-compose, room-runtime, datastore, hilt-navigation-compose) and Maven Central `maven-metadata.xml` (hilt-android, okhttp, kotlinx-serialization-json, kotlinx-coroutines-core, org.jupnp / org.jupnp.android / org.jupnp.support, ktor-server-cio, symbol-processing-api)
- Dagger KSP guide — https://dagger.dev/dev-guide/ksp.html ; jUPnP — https://github.com/jupnp/jupnp , https://www.jupnp.org/docs/android/ ; Ktor server engines — https://ktor.io/docs/server-engines.html , Ktor 3.1 changelog (Android server fix) — https://ktor.io/changelog/3.1/
- Tizen remote protocol — openHAB `samsungtv` binding docs, samsungctl/samsungtvws-style implementations, petterhj.net Samsung API notes (secondary but corroborated across maintained implementations)
- Android platform — local network permission — https://developer.android.com/privacy-and-security/local-network-permission ; Android 17 behavior changes — https://developer.android.com/about/versions/17/behavior-changes-17 ; Play target API requirement — https://developer.android.com/google/play/requirements/target-sdk ; network security configuration — https://developer.android.com/privacy-and-security/security-config

## Reviewer note for the aggregator

Findings 1–3 are corrections to committed version/configuration statements; Findings 4–5 are risk/omission notes. If the spine is amended, only the `## Stack` table (split compileSdk/targetSdk; reword Kotlin; optionally AGP 9.3.1) and a one-line addition to AD-9/AD-11 are required. Nothing validated by research contradicts the spine's architecture, module graph, or AD-1…AD-11 rules.
