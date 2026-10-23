---
title: 'Find a TV, pair it, and persist trust'
type: 'feature'
created: '2026-10-23'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '09013772c59b526d397400bd2051f80474589fc9'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The shell has a LAN gate but no way to find a TV, complete the Allow prompt, or remember trust — the pilot cannot get past setup.

**Approach:** Add gated SSDP plus manual-IP identity read, the explicit-choice pairing card with the Allow flow, and Keystore-backed token/pin persistence keyed by stable identity; session-only targets stay in memory only.

## Boundaries & Constraints

**Always:**
- Gate consults first: `TvApi.checkLanAccess` / `CastApi.checkLanAccess` already exist; every scan, `/api/v2/` fetch, redirect, and WSS connect passes the single `:core` `PrivatePeer` predicate (RFC1918 + 169.254/16 + fe80::/10; global IPv6 and ULA refused); `Denied` blocks scan and manual IP identically with the gate explainer, never "TV didn't answer".
- SSDP M-SEARCH to `239.255.255.250:1900` uses the Spike A search target under the gate's held scoped `MulticastLock`; jUPnP lock disabled; lock released on screen-leave, success, failure, and cancellation.
- Identity is device ID first, byte-exact MAC second, one normalization rule applied identically everywhere; `TvId` is the hash of that identity, never an IP; firmware stored byte-exact, never normalized; masked MAC shows at most last two octets.
- First WSS opens token-less to `wss://<ip>:8002/api/v2/channels/samsung.remote.control?name=<base64>`; leaf SPKI captured TOFU, then stored; every later connect validates the pinned leaf before attaching `&token=`; mismatch fails closed with no token sent.
- Token and pin live in Keystore-backed encrypted storage under a per-TV alias; Room stores the record plus alias, never secrets; pairing material backup-excluded; `RedactedLog` only, no `println`/`Log`, no token/pin/SPKI/full-MAC in logs, results, or diagnostics.
- One writer per value; `PairingStore` port declared in `:tv`, implemented in `:app`; `:tv` never reads Room/Keystore directly; `:app` never imports `okhttp3`, `org.jupnp`, `KEY_*`, or frame types.
- Copy is byte-settled by the story: pairing card, Allow prompt with always-visible no-remote path and button diagram, deny/timeout, paired state; topbar on Add TV/Pairing is back plus static title, right empty.

**Never:** reconnect/backoff, token-rejection or identity-changed re-pair, trust sheet/SPKI display, Forget, multi-TV switcher, remote keys, repeat engine, matrix labels, text/pointer/voice/cast/mirror/wake, Settings, cloud fallback, trust-all manager, hostname suppression outside the pinned client, WSS-to-WS fallback, second `PrivatePeer` or second lock, resolved capability label storage.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| No-TV shell | No stored TV, gate granted | "No TVs yet." + quiet `code` readout + disabled six-slot launcher; "Add your first TV" starts exactly one scan | No auto-scan loop; stored TV replaces setup entry |
| Discovery | Add TV opened | Results appear as found under "Scanning for TVs…", no spinner; "Scan again" after run; leave stops scan, releases lock | Bad `/api/v2/` skipped with diagnostic; one TV never stops the scan |
| Gate denied | `Denied` | Scan and manual IP both blocked as `LanDenied` with gate explainer | Zero network attempts |
| Manual IP valid | IPv4/IPv6 literal, private | Same pairing card as discovery; dedupes to same `TvId`/record | Public/DNS/blank rejected inline before any request |
| Manual IP no identity | No device ID and no MAC | "Session only" label; in-memory token/pin; no Room record, no alias | Gone after process ends |
| Pairing choice | Card shown | "This is my TV — connect" primary, "That's not my TV" always visible, removes card for this run | No auto-pair, no pre-selected card |
| Allow | TV allows | `ms.channel.connect` token stored with pin (or pin-only when `TokenAuthSupport=false`); socket closes, no remote opened, no "Connected" claim | `TokenAuthSupport=true` with no token stores nothing |
| Deny/timeout | Prompt denied or closes | "The TV said no." / "The TV didn't answer." with Retry/Not now; diagram stays visible | Retry is user-action only; nothing stored |
| Paired | Success | "Paired" + name/model + Done; shell shows single-TV summary; same-model TVs yield distinct `TvId`s | Never replaces or forgets a stored TV |

</frozen-after-approval>

## Code Map

- `core/src/main/kotlin/com/unirem/core/api/LanAccess.kt` (exists) -- reuse `LanAccess`, `NetworkType`, diagnostic codes; add nothing here unless `PrivatePeer` needs a home beside it.
- `core/src/main/kotlin/com/unirem/core/api/PrivatePeer.kt` (new) -- single predicate over typed destination+purpose; sole caller for OkHttp interceptor, SSDP filter, media/wake paths; wake broadcast allowlist lives inside per AD-8.
- `core/src/main/kotlin/com/unirem/core/api/Identity.kt` (new) -- pure `StableIdentity` precedence + one normalization, `TvId` hash, `maskMac`, `validateIpLiteral`, session-only rule; JVM-only, no Android import.
- `core/src/main/kotlin/com/unirem/core/api/Failures.kt` (extend) -- keep `LanDenied`; add only what 1.5 needs (`TvNotFound`-style read failures stay local, never a new gate failure).
- `tv/src/main/kotlin/com/unirem/tv/api/TvApi.kt` (extend) -- keep `checkLanAccess`; declare `PairingStore` port + `TvDiscoveryGateway`/`TvPairingControl` seams; public API package only.
- `tv/src/main/kotlin/com/unirem/tv/internal/` (new) -- `SsdpDiscovery` (M-SEARCH + `Location` filter via `PrivatePeer`), `ApiInfoMapper` (pure `/api/v2/` parse per `logic/ApiInfo.kt` pattern), `PinnedTlsChannel` (TOFU capture, pin-then-token, no fallback per `logic/PinCompare.kt` + `logic/ChannelUrl.kt`).
- Reuse pattern (copy, never depend): `tools/spike-a/.../logic/Ssdp.kt`, `ApiInfo.kt`, `ChannelUrl.kt`, `ChannelEvent.kt`, `StableIdentity.kt`, `PinCompare.kt`, `Redaction.kt`, `Discovery.kt`, `PinnedTls.kt`, `Vault.kt`, `Harness.kt` pair flow.
- `app/src/main/kotlin/com/unirem/tv/` (new) -- `RoomTvStore` entity/DAO/database (tvId, identity, name, model, firmware byte-exact, wifiMac full + provenance, NetworkType, interface, alias, pairedAt), `KeystorePairingStore` (`PairingStore` impl, per-TV alias), backup-exclusion rules.
- `app/src/main/kotlin/com/unirem/setup/` (new) -- `AddTvScreen`, `PairingCard`, `PairingScreen` (Allow prompt + no-remote diagram), `PairedScreen`; wire into `ShellScreen` + `MainActivity`; Graphite Mint tokens only, custom line glyphs.
- `app/src/main/AndroidManifest.xml` (edit) -- keep three gate permissions; add backup-exclusion, no new runtime permissions; `app/build.gradle.kts` wires `okhttp`, `jupnp`, `room` from catalog (KSP), no `:app` import of `okhttp3`/`org.jupnp`.
- Do not change: `tools/spike-a/**`, `debug/*`, gate permission flow, Graphite Mint tokens, `docs/app-identity.md`, CI command.

## Tasks & Acceptance

**Execution:**
- [x] `core/.../PrivatePeer.kt` + `Identity.kt` -- single predicate, identity/TvId/mask/validate pure logic -- AC2, AC4, AC5, AC8, AC9.
- [x] `tv/.../TvApi.kt` -- declare `PairingStore` port + discovery/pairing seams, keep gate mapping -- AC7, AC8.
- [x] `tv/.../internal/*` -- SSDP under gate lock, `/api/v2/` read with redirect filtering, pinned WSS pair (capture TOFU, pin-then-token, close after success) -- AC2, AC6, AC7.
- [x] `app/.../tv/RoomTvStore` + `KeystorePairingStore` + backup rules -- record + alias, secrets only in Keystore, exclusion -- AC7, AC8.
- [x] `app/.../setup/*` + `ShellScreen`/`MainActivity` wiring -- no-TV shell, Add TV scan/states, manual IP form, pairing card/choice, Allow + no-remote diagram, deny/timeout, paired state -- AC1-AC9.
- [x] `app/src/test` + `core/src/test` + `tv/src/test` -- JVM: identity precedence, TvId stability, duplicate resolution, MAC masking, IP validation, session-only gating, scan-state mapping, `PairingStore` fake contract -- AC10.
- [x] Scope-guard updates -- `:app` import ban (`okhttp3`/`org.jupnp`/`KEY_*`), single-lock/single-predicate, `RedactedLog`-only, no telemetry/manager-helper-util -- AC11.

**Acceptance Criteria:**
- Given granted gate and no TV, when shell opens, then no-TV copy, quiet readout, disabled launcher, and one scan per "Add your first TV" hold.
- Given Add TV, when scan/manual IP run, then private-only destinations, byte-exact firmware, as-found results, manual-IP dedupe, and session-only labeling with no stored record.
- Given pairing, when Allow/deny/timeout resolve, then token+pin (or pin-only) persisted per identity, socket closed without opening remote, and deny/timeout store nothing.
- Given success, when shell returns, then single-TV summary, distinct `TvId`s for same-model TVs, no replacement of stored TV, and secrets absent from Room/logs/backup.
- Given this story, when the diff is reviewed, then no reconnect, re-pair, trust sheet, Forget, switcher, keys, labels, or second predicate/lock exists.

## Implementation Notes

- Dispatch built the full slice; verified by me, not trusted. Fixes I applied after the dispatch: (1) `PinnedTlsChannel.ChannelOutcome` private to internal (Kotlin EXPOSED_FUNCTION_RETURN_TYPE broke `:tv` compile); (2) hoisted `stringResource` out of `Modifier.semantics` lambdas in `SetupChrome`/`AddTvScreen` (COMPOSABLE_INVOCATION broke `:app` compile); (3) excluded `RedactedLog.kt` from its own policy-doc hits in `PairingScopeGuardTest` (its policy comment names `println`/`android.util.Log`/`telemetry`, failing 2 guard tests).
- `./gradlew build lint :core:test` green with `JAVA_HOME=C:\Users\KAVEN\dev\toolchain\jdk-17` (toolchain kept off PATH; `local.properties` carries `sdk.dir`). 127 JVM tests pass, 0 failures: app 64, core 36, tv 27. `lint` BUILD SUCCESSFUL. Scope sweeps: no `okhttp3`/`org.jupnp`/`KEY_` in `app/src/main`; `MulticastLock`/`WifiManager` only in `LanAccessGate.kt`.
- Redirects are checked but never followed (`InfoRead(null, code)` on redirect even when the target is private) — fail-safe; a follow would need a second gated fetch. `sessionTrust` in `MainActivity` is write-only until a later story opens a channel; `TvRecordView` in `TvApi` is unused scaffolding for later stories.
- Hardware-only rows stay manual on owned LAN: SSDP find, `/api/v2/` card, Allow/Deny/timeout, restart-persistent record, same-model distinct `TvId`s, `adb logcat` secret-freedom.
- Patch pass 2026-10-23 (step-04, no loopback; same-session re-engagement unavailable, applied directly): G1 bracketed IPv6 in `readManual`/`ChannelUrl.build` and extended Location regex (https, `[v6]`, slashless); G2 tracked `pairJob` with cancel-on-back/not-now/retry, double-choose guard, `pairingFailed` surfaced on PairingScreen, persist-null maps to failure; G3 settled copy (AC1 intro + AC3 empty/isolation, AC5 firmware/first-time/provenance/Not reported, AC6 allow/no-remote/deny/timeout lines) plus `manualKeys` provenance plumbing and shell/paired Not-reported fallbacks; G4 diagram content description, disabled manual/scan-again visuals, `SetupLogic.cardKey` shared by screen and host; G5 Wake suffix requires a private literal; G6 `internal` on `:tv` impl types; G7 `clear` true-if-removed; G8 device-transfer block plus journal companions; G9 new JVM tests (`RoomTvStore` mapping, `SsdpDiscovery` boundary, pair pre-guards + `classifyConnected`, cardKey, wake-suffix, IPv6 URL/location) and `SetupLogic.mapScan` call-site use. `./gradlew build lint :core:test` green: 144 JVM tests, 0 failures; scope sweeps clean. Keystore real-impl coverage stays an owned-device instrumentation task per AGENTS.md.

## Review Triage Log

Review pass 2026-10-23, diff `unirem-15-review.diff` (143777 bytes, 140.4 kB; floor N = min(floor(sqrt(140.4)+1),10) = 10). Three layers filed 17 blind + 14 edge + 4 verification-gap findings. Every claim verified against the cited file before rendering. No intent_gap, no bad_spec, no loopback: all surviving entries are patch.

- B1 rotation loses setup route/results (remember not rememberSaveable): true as a code fact. Verdict: low, rejected per rule 82 (fix needs a custom Saver for SetupRoute/TvCandidate; harm is a re-scan after rotation).
- B2 pairing job never cancelled on Back/Not now; completion flips to Paired afterward: true (MainActivity:307/317 only set route; runPair job untracked). Verdict: low, patch, group G2.
- B3 sessionTrust write-only, no consumer: true, acknowledged in Implementation Notes. Verdict: false (in-memory map satisfies AC4/AC9 session-only rule; the channel consumer is a later story).
- B4 records loaded once, first-row-only summary, no second-TV UI: true. Verdict: false for this story (a second stored TV is unreachable: 1.5 never replaces/forgets; multi-TV is Epic 2).
- B5 SetupLogic.mapScan tested but MainActivity re-implements the mapping inline (MainActivity:128-137): true. Verdict: low, patch, group G9.
- B6 dedupeByTvId keeps all null-tvId candidates incl. repeat IPs: true mechanism, but startScan folds through mergeAsFound which drops same-IP dupes (SetupLogicTest.mergeAsFound). Verdict: false (no bad outcome).
- B7 hardcoded "No remote path diagram" contentDescription; setup_session_only_body unused: true (PairingScreens.kt:127; strings.xml:48). Verdict: low, patch, group G4 (resource + use the body line on the session-only card).
- B8 manual field/button stay enabled when gateBlocked (early-return only): true. Verdict: low, patch, group G4 (enabled flags on Gate*Actions, default true).
- B9 KeystorePairingStore.clear returns commit()=true on absent keys vs Fake false: true. Verdict: low, patch, group G7 (true-if-removed + document on PairingStore).
- B10 destructive migration wipes rows on bump; observeAll unguarded; all/byId/count swallow: true. Verdict: low, rejected per rule 82 (no version bump exists; migrations now are speculative; swallow-and-empty matches the 1.4 gate pattern and no error UI exists in 1.5 scope).
- B11 blank name/model stored verbatim; shell summary and PairedScreen render blank: true. Verdict: low, patch, groups G3/G4 (Not reported fallback).
- B12 extraction rules lack device-transfer; backup excludes miss -wal/-shm/-journal: true. Verdict: low, patch, group G8.
- B13/E9 isWakeBroadcast accepts any ".255" (8.8.8.255, foo.255) without literal check: true (PrivatePeer.kt:65-66). Verdict: low, patch, group G5 (require IPv4 chars first; no Wake caller in 1.5, hardens the shared predicate).
- B14 redirect targets checked but never followed; hostFromLocation drops https/IPv6/slashless; sequential reads: partially true. Redirect-not-followed is the specified fail-safe (Implementation Notes). Verdict: false on redirects; the host/URL IPv6 part is real and grouped with G1.
- B15 pair() never calls verify, re-pair would re-capture TOFU: true mechanism. Verdict: false (no re-pair flow in 1.5; first-pair TOFU is the specified ceremony; verify is 1.7's seam).
- B16 TvGateways (api package) imports internal concretes; SsdpDiscovery/PinnedTlsChannel lack internal modifier: true. Verdict: low, patch, group G6 (add internal; tests stay same-module).
- B17 dead surface (TvRecordView, denies/timedOut, clear, byId/count): true as a code fact. Verdict: false (forward seams for 1.6/1.7 plus test-covered helpers; KEEP TvRecordView, PairingStore.clear, TvDao.byId/count).
- E1/E8/E14 IPv6 manual read builds unbracketed URL and always fails, against AC4's IPv6 promise (SsdpDiscovery.kt:124; ChannelUrl.build): true. Verdict: medium, patch, group G1.
- E2 Location regex drops https/IPv6-bracketed/slashless URLs (SsdpPacket.kt:22): true. Verdict: low, patch, group G1 (Samsung DMR answers are http-with-path, so everyday harm is low, but the fix is a direct regex correction).
- E3 Found-targets.first() crash on empty manual result: refuted, readManual only ever returns a single-candidate Found. Verdict: false.
- E4 persistPaired null still navigates to Paired (return ignored, MainActivity:163-174): true. Verdict: low, patch, group G2 (null maps to the Pairing failure state).
- E5 double-choose launches overlapping pair attempts (no pairingWaiting guard): true. Verdict: low, patch, group G2.
- E6 PairOutcome.Failed writes AddTv diagnostic while route is Pairing, so the reason is invisible: true. Verdict: low, patch, group G2 (surface the reason on PairingScreen).
- E7 non-positive timeout reaches latch await: refuted, the only caller uses the 30L default. Verdict: false.
- E10 same-TvId new IP discarded, stale DHCP IP kept: true mechanism. Verdict: false for this story (no reconnect/session in 1.5; the 1.7 story owns staleness).
- E11 blank token treated as persistable by SetupLogic.persists (== null check, SetupLogic.kt:71): true mechanism, unreachable (connectAndClassify maps isNullOrBlank to TokenMissing first). Verdict: false.
- E12 pin mismatch maps to Failed: refuted for this story's flow (verify() has the Mismatch mapping and is 1.7's seam; pair() cannot mismatch its own TOFU capture). Verdict: false.
- E13 DB error masquerades as no TVs: true mechanism. Verdict: low, rejected per rule 82 (no error state exists in 1.5 scope; fresh single-row Room).
- C1 (added in Tasks verification): no-TV shell omits AC1 body line (ShellScreen shows shell_subtitle, not "UniRem controls Samsung TVs on your Wi-Fi. Nothing is sent to the internet."). Verdict: medium (user-facing contract line), patch, group G3.
- C2 (added): PairingCard omits Tizen version, first-time note, same-WiFi/Added-by-IP provenance, Not reported fallbacks (AC5). Verdict: medium, patch, group G3 (provenance via call-site param, no api growth).
- C3 (added): Allow/no-remote copy differs from AC6 byte-settled lines. Verdict: medium, patch, group G3.
- V1 RoomTvStore.persistPaired mapping untested (pre-verified): verdict patch, group G9 (JVM test via forTests seam).
- V2 real KeystorePairingStore untested (pre-verified): verdict patch, group G9 (owned-device androidTest; JVM cannot touch Keystore per AGENTS.md).
- V3 SsdpDiscovery scan/manual untested (pre-verified): verdict patch, group G9 (JVM tests via searchHostsFn + fake OkHttpClient seams).
- V4 pair() guards/mapping untested (pre-verified): verdict patch, group G9 (pre-guard JVM tests + extract pure classify for the mapping test).
- scrub-misses-JSON-tokens: true mechanism. Verdict: low, rejected per rule 82 (scrub inputs are IO/TLS exception messages with no demonstrated secret vector).

Groups: G1 IPv6 (medium), G2 pair lifecycle (low), G3 settled copy + card completeness (medium), G4 actions/diagram/fallback polish (low), G5 wake predicate (low), G6 internal boundary (low), G7 clear semantics (low), G8 backup rules (low), G9 tests + mapScan use (patch).

## Design Notes

Pairing reuses the spike ceremony exactly: capture-then-connect with no token, store on `ms.channel.connect`, fail closed on mismatch or missing token when `TokenAuthSupport=true`. Discovery failure is a diagnostic row, never a `TvFailure` that later stories confuse with pairing denial. Session-only uses an in-memory key, never a persisted `TvId`.

## Verification

**Commands:**
- `./gradlew build lint :core:test` -- expected: BUILD SUCCESSFUL, 0 lint errors, core/app/tv JVM tests pass.
- `rg -n "okhttp3|org\\.jupnp|KEY_" app/src/main` -- expected: no hits.
- `rg -n "MulticastLock|WifiManager" tv/src/main cast/src/main core/src/main app/src/main --glob '!*LanAccessGate*'` -- expected: no product hits outside the gate.

**Manual checks (if no CLI):**
- On owned LAN: SSDP finds TV, `/api/v2/` card correct, Allow yields paired restart-persistent record; Deny/timeout store nothing; same-model second TV yields distinct `TvId`; `adb logcat` shows no token/pin/SPKI/full MAC.
