---
title: 'Spike A: prove the pairing and identity path'
type: 'feature'
created: '2026-10-23'
status: 'done'
route: 'dispatch'
baseline_commit: '54397f7ea8ee34aae4914e5b4fc641010491a051'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/unirem/spec-samsung-remote/protocol-reference.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Epic 1 commits to a trust model — discovery, Allow prompt, token persistence, SPKI pinning, stable identity — that nothing has proven on real Tizen hardware. If the model is wrong, the pilot dies at the first screen, after the foundation is built.

**Approach:** Ship a throwaway Spike A harness and its report. The harness records exact model, firmware, region, and network per TV; runs SSDP discovery and manual-IP fallback; completes first pairing; re-uses the token across a restart; captures and re-validates the leaf SPKI; keeps two TVs distinct by stable identity; and exercises denial, reconnect, and pin mismatch. It writes machine-readable results, and the report is filled from those results only.

## Boundaries & Constraints

**Always:**
- A row is `pass` only with a recorded run for that model, byte-exact firmware, and network type. `blocked` is not `pass`; a hard failure recommends a re-plan, never "mostly passed".
- Identity is device ID first, byte-exact MAC second, under one normalization rule; `TvId` is its hash, never an IP. No resolvable identity means a session-only target: in-memory, no durable record, no Keystore alias.
- Validate the stored leaf SPKI before attaching `&token=`; a mismatch fails closed into visible re-pair with no token sent.
- Silence is not failure; a socket write is never a TV action; no queue, silent retry, or replay after reconnect.
- Never log a token, pin, SPKI, or full MAC — log a `TvId` prefix, and a MAC only as `xx:xx:xx:xx:xx:??`.
- Harness only: no production UI, no `:core`/`:tv`/`:app` code, no Gradle module, no key set, repeat, text, pointer, voice, cast, mirror, multi-TV switcher, or Settings.
- Harness form is an Android app under `tools/spike-a/` with its own standalone build, because `MulticastLock`, Keystore, and backup exclusion are the claims under test.
- The story closes with the harness and the report; every row stays `blocked` and the gate stays shut until recorded runs exist. A green build is not a pass.

**Never:** trust-all `X509TrustManager`, hostname-check suppression outside the pinned client, WSS-to-WS fallback, pairing material outside Keystore-backed backup-excluded storage, or identity derived from an IP or the current connectivity path.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Discovery empty | SSDP yields nothing | Manual-IP offered, same `/api/v2/` read attempted | Record `NoDiscovery`, not a TV failure |
| Gate denied | No local-network grant | Both paths blocked, row records `LanDenied` | Never report "TV didn't answer" |
| Manual IP, no identity | `/api/v2/` lacks device ID and MAC | Target marked session-only | No durable record, no Keystore alias |
| First pairing allowed | No token, prompt shown | `ms.channel.connect` carries the token | Store token and leaf SPKI keyed by identity |
| Denied or timeout | Prompt dismissed or unanswered | `ms.channel.unauthorized` or close | Persist nothing |
| Token rejected later | Stored token refused | Visible "Pairing is required for this TV." | Re-pair action, no token resent |
| Pin mismatch | Stored SPKI differs from leaf | Fail closed before token attach | Visible re-pair, mismatch recorded |
| Socket closes | Mid-run or between runs | Offline state, no queue or retry | Backoff visible, never silent |
| Two TVs paired | Same model preferred | Two distinct `TvId`s | Collision recorded, synthetic identical-ID test run |

</frozen-after-approval>

## Code Map

No production source exists: no wrapper, no modules, no JDK or Android SDK on this machine. Shorthand `.../` means `tools/spike-a/app/src/main/kotlin/com/unirem/spikea/`.

- `tools/spike-a/` -- standalone harness build, outside the Gradle graph; never added to a `settings.gradle.kts`.
- `.../Discovery` -- SSDP M-SEARCH to `239.255.255.250:1900`, then `GET <tv>:8001/api/v2/` for `modelName`, `model`, `softwareVersion`, `TokenAuthSupport`, `wifiMac`, and the reported device ID.
- `.../Channel` -- `wss://<ip>:8002/api/v2/channels/samsung.remote.control?name=<base64>`; handles `ms.channel.connect`, `ms.channel.unauthorized`, `ms.channel.ready`, close.
- `.../PinnedTls` -- leaf SPKI capture and compare; hostname check relaxed only inside this client.
- `.../Vault` -- token and pin per identity alias; backup-excluded rules file.
- `.../Results` -- append-only JSONL, redacted at write.
- `_bmad-output/implementation-artifacts/spike-a-pairing.md` -- report, filled from Results only.
- Reuse `protocol-reference.md` (endpoints, frames, TLS rule) and `verification-matrix.md` (report columns).

## Tasks & Acceptance

**Execution:**
- [x] `tools/spike-a/settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, wrapper -- standalone build on Gradle 9.5.0, AGP 9.3.1, JDK 17, Kotlin 2.4.21 -- keeps the production graph untouched.
- [x] `tools/spike-a/app/src/main/AndroidManifest.xml`, `res/xml/backup_rules.xml` -- `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`; backup and data-extraction excluded -- AC5, AC8.
- [x] `.../LanGate.kt` -- one `MulticastLock` holder and the permission gate, `LanDenied` on refusal -- AC2 denial path.
- [x] `.../Discovery.kt` -- SSDP, `/api/v2/` identity read, manual-IP entry -- AC2.
- [x] `.../PinnedTls.kt` -- capture, store, compare the leaf SPKI, fail closed -- AC5.
- [x] `.../Channel.kt` -- connect, prompt, token capture, unauthorized, close, visible backoff, no queue or retry -- AC3, AC4, AC7.
- [x] `.../StableIdentity.kt` + unit test -- precedence, one normalization, `TvId` hash, session-only fallback -- AC6.
- [x] `.../Vault.kt` -- token and pin per identity alias in Keystore-backed storage -- AC5.
- [x] `.../Results.kt` + unit test -- JSONL rows, redaction of token, pin, SPKI, MAC -- AC8, AC9.
- [x] `.../SpikeAScreen.kt` -- one scrollable run log and the run buttons; no product UI -- AC10.
- [x] `_bmad-output/implementation-artifacts/spike-a-pairing.md` -- report skeleton, verdict `blocked` until runs exist -- AC9.
- [x] `.gitignore` -- ignore harness build output only.

**Acceptance Criteria:**
- Given at least three Tizen 2016+ TVs and a physical phone on one LAN, when the harness runs, then it records model, year, region, exact firmware, and network type per TV, and a missing TV blocks the gate rather than passing it.
- Given a completed run set, when the report is written, then each row cites recorded results and states pass, fail, or blocked, the exact firmware, the re-prompt and pin-stability outcomes, the no-remote Allow result, and any identity-collision gap.
- Given the finished harness, when storage, logs, and TLS configuration are inspected, then no token, pin, SPKI, or full MAC is logged or backed up, and no trust-all or WSS-to-WS path exists.

## Implementation Notes

Build session 2026-10-23 (dispatch route; subagent quota failed twice, then a retry dispatch succeeded and I verified its output against the diff, not its report).

- Correction to the dispatch report: the harness did not "already exist". All files under `tools/spike-a/` (31 source files, 7 test classes) and `spike-a-pairing.md` are new in this run, untracked, against `baseline_commit 54397f7`. The report's "no source changes were needed" is wrong; the work is real, it was just done by the dispatch, not by me directly.
- Verified by me, not trusted: `:app:cleanTestDebugUnitTest :app:testDebugUnitTest` re-executed — 41 tests, 0 failures/errors/skipped across 7 classes. `:app:assembleDebug` BUILD SUCCESSFUL (`app-debug.apk`, 1.6 MB). `:app:lintDebug` 0 errors, 16 warnings (7 SetTextI18n spike UI, 2 CustomX509TrustManager = the pinning itself, 2 AGP-version, 2 NewerVersionAvailable, 1 OldTargetApi since the SDK tops out at 36, 1 GradleDependency, 1 MissingApplicationIcon).
- Matrix audit: all 9 I/O rows map to passing JVM tests (SSDP parse/reject, session-only identity, wss-only URL, connect/unauthorized events, pin match/mismatch/missing/malformed, visible capped backoff, collision/distinct-TvId rules, redaction, gate verdict). Token-reject and deny-persistence orchestration live in `Harness` (Android) and are hardware-gated: no JVM test can observe a TV prompt, so the report keeps those rows `blocked` and no test stands in for them.
- Policy spot-checks: no `println`/`android.util.Log`; hostname verifier relaxed only inside the pinned client; TOFU capture never on a token path; token attached only after pin `Match`; vault is AndroidKeyStore AES/GCM; `ResultRow` schema has no token/pin/SPKI/MAC field; backup and data-extraction excluded; root Gradle graph untouched (no root `settings.gradle.kts`).
- `.gitignore` gained explicit `tools/spike-a/` build-output lines on top of the existing `**/build/` coverage; `git check-ignore` confirms APKs, `.gradle/`, and `local.properties` are all ignored. Build outputs (~540 files) will never enter a commit.
- Gate stays shut by design: no Tizen hardware on this machine, every report row `blocked`, AC1 unsatisfied until real runs.

Review pass 2026-10-23 (step-04): three reviewer layers (blind/edge-case/verification-gap) filed 46 findings; all verified against code and logged in `## Review Triage Log`. 13 patch groups applied by the implementation dispatch, no intent_gap/bad_spec, no loopback: `GateRules` counts only rows with non-blank model/firmware/network; TV-reported fields scrubbed in `ResultsFormat.line`; `Redaction.withoutTokenParam` strips `token=` params at failure-reason sites; SSDP hosts filtered through `HostRules.normalizeIp` and `Channel.connect` rejects non-IPv4; `parseInfo` mapping extracted to `logic/ApiInfo.kt`; `twoTvCheck` checks collisions first with non-blank labels; backoff-denied reconnect and absent-clear now record honestly; `cycleTarget` guarded mid-run; interrupt status restored; HTTP codes in discovery failures; report How-to-run gained trusted-LAN, phone-context, and TV-precondition lines. Post-patch verification by me: 60 JVM tests pass (9 new ApiInfo, 4 new Redaction, 6 new Results), assemble BUILD SUCCESSFUL, lint 0 errors/16 warnings unchanged.

## Spec Change Log

## Review Triage Log

First review pass, 2026-10-23, diff `spike-a-review.diff` (167.6 kB, 43 files). Verified each claim against the cited file before rendering. Groups: G1 gate-verdict integrity, G2 unscrubbed TV-reported fields, G3 token-bearing failure text, G4 SSDP host validation, G5 parseInfo mapping tests, G6 detector tests, G7 two-TV check branches, G8 backoff row, G9 clear-absent row, G10 cycle guard, G11 interrupt status, G12 readInfo root cause, G13 report doc lines.

- B1 `GateRules.verdict` passes on three distinct prefixes with no FAIL/BLOCKED, so discovery-only PASS rows pass without pairing evidence; nothing calls `verdict()` yet except tests, and the human report is the real gate — verdict: medium, patch, group G1.
- B2 `PREFIX_length` vs `PREFIX_LENGTH` "would not compile": refuted — forced `:app:compileDebugKotlin --rerun-tasks` on 2026-10-23 is BUILD SUCCESSFUL, so the compiler accepts the current sources — verdict: false.
- B3 model, firmware, networkType, networkDetail, identitySource written unscrubbed in `ResultsFormat.line` (Results.kt:76-82): true, TV-reported strings reach JSONL raw — verdict: low, patch, group G2.
- B4 short tokens/PINs survive `scrub` (only 32+ hex / 40+ base64 masked): true as a code fact; no demonstrated message carrying a secret, but `Channel.onFailure` logs `t.message` which can embed a request URL — verdict: low, patch, group G3.
- B5 SSDP LOCATION hostnames drive WSS/TLS with no validation (`ChannelUrl.build` takes any string; scan never filters): true — verdict: low, patch, group G4.
- B6 `https` LOCATION silently dropped (`Ssdp.LOCATION_HOST` is http-only): true, but fails safe into NoDiscovery/blocked and Samsung DMR LOCATIONs are http — verdict: low, rejected (fails safe, unlikely met).
- B7 `parseInfo` missing-vs-blank collapse and untested id/duid/casing branches: true as a test gap; behavior itself follows the spec mapping — verdict: low, patch via tests, group G5.
- B8 `tvNetworkLabel` "Unknown" masks phone transport in `networkType`: the same row's `networkDetail` preserves both (`phone=`/`tv=`), so no evidence is lost — verdict: false.
- B9 `sameModel` true on two blank labels (Harness.kt:668): true — verdict: low, patch, group G7.
- B10 `Vault.audit` vacuously true on empty vault: true as a code fact, but `securityInspect` returns BLOCKED on zero entries so no PASS results — verdict: false.
- B11 TOFU capture has no hostile-LAN warning in How-to-run: true as a docs gap; the spec's threat model is the operator's own LAN — verdict: low, patch, group G13.
- B12 reflection nulls ignored in `securityInspect`: the `backup_rules`/`data_extraction_rules` resource checks are authoritative and recorded; reflection is documented best-effort — verdict: false.
- B13 missing JVM tests for Vault/LanGate/`Harness.row`: those need Android APIs (Keystore, MulticastLock, Context) and cannot run on JVM; `parseInfo` gap is real and grouped with G5 — verdict: false for the Android-bound parts.
- B14 `cycleTarget` mutates `active` on the UI thread while the executor mutates it under only `volatile`: true — verdict: low, patch, group G10.
- B15 phone row vs `REQUIRED_DISTINCT_TVS` mapping undocumented: true as a docs gap — verdict: low, patch, group G13.
- B16 How-to-run omits TV preconditions (pairing mode, same subnet, timeouts, per-scenario reading): true as a docs gap — verdict: low, patch, group G13.
- E1 malformed SSDP host escapes `Channel.connect` as `IllegalArgumentException` (no try around `Request.Builder().url()`); manual IPs are pre-validated but SSDP hosts are not — verdict: low, patch, group G4.
- E2 `Thread.sleep` interrupt in `searchHosts` not re-asserted, collapses to generic `Failed`: true — verdict: low, patch, group G11.
- E3 wall-clock deadline skew: true as a code fact, but a 4 s window with NTP slew has no demonstrable impact — verdict: low, rejected (negligible harm).
- E4 non-2xx/exceptions collapse to null in `readInfo`, losing code/body: true — verdict: low, patch, group G12.
- E5 blank model/firmware still yields Found/PASS rows that the gate counts: true — verdict: medium, patch, group G1.
- E6 duplicate terminal frame silently discarded: first-terminal-wins is the specified behavior (silence is its own outcome; no queue), no bad outcome shown — verdict: false.
- E7 backoff-denied reconnect writes no `ResultRow`: true (Harness.kt:439-441 returns before `record`) — verdict: low, patch, group G8.
- E8 clearing an absent vault entry records PASS: true (`SharedPreferences.remove().commit()` returns true for absent keys) — verdict: low, patch, group G9.
- E9 session-only checked before collisions in `twoTvCheck`, hiding a real collision behind BLOCKED: true (Harness.kt:669-674 order) — verdict: low, patch, group G7.
- E10 bare-hex MACs and short tokens bypass `scrub`: same code fact as B4 — verdict: low, patch, group G3.
- E11 model/firmware/network fields unscrubbed: same as B3 — verdict: low, patch, group G2.
- E12 LOCATION without trailing slash returns null host: true per regex, but fails safe into NoDiscovery and Samsung LOCATIONs always carry a path — verdict: low, rejected (fails safe).
- E13 no LAN-range check on SSDP host: true, but hostile-LAN steering is outside this spike's threat model and the operator runs the ceremony — verdict: low, rejected (threat model, unlikely met).
- E14 leading-zero/reserved IPs accepted by `normalizeIp`: true as a code fact; whether Android `InetAddress` misparses octal forms could not be verified on this machine, and operators rarely type padded octets — verdict: maybe-false, rejected with note (settle on device if ever observed; if true it would be low).
- E15 same MAC with different separators yields different `TvId`: true, but the frozen spec mandates byte-exact MAC with one normalization rule, which is exactly what the code does — verdict: false.
- E16 placeholder device IDs treated as stable: no evidence any Tizen firmware reports placeholders; the denylist fix would invent behavior the spec never describes — verdict: maybe-false, rejected with note (reopen with a recorded `/api/v2/` sample).
- E17 explicit pin-check throw escapes as Failed instead of Mismatch: refuted — handshake-time rejection throws `SSLHandshakeException` (an `SSLException`) with the `CertificateException` cause, which `verify()` maps to Mismatch; the explicit call only runs after a passing handshake — verdict: false.
- E18 corrupt vault entry reads as missing: true (`decrypt` null and absent key are indistinguishable), but likelihood is negligible on a spike phone and recovery is clear-app-data/re-pair; a fix would change the public `Vault.get` signature — verdict: low, rejected (unlikely met, self-healing).
- E19 hardcoded 12-byte GCM IV: the code always encrypts with its own provider-default 12-byte IV, so decrypt never meets a foreign IV size — verdict: false.
- E20 cycle allowed mid-run: same as B14 — verdict: low, patch, group G10.
- E21 `snapshotState` does Keystore/file I/O on the UI thread (`MainActivity:25` → `currentState`): true, but files are tiny and the tool is manual; caching would add state for no demonstrated ANR — verdict: low, rejected (unlikely harm; reopen on a strict-mode violation).
- E22 manifest nulls ignored: same as B12 — verdict: false.
- E23 gate ignores model/firmware/network: same root as B1/E5 — verdict: medium, patch, group G1.
- E24 scrub-audit claim on unscrubbed fields/short secrets: same as B3/B4 — verdict: low, patch, groups G2/G3.
- E25 normalization vs byte-exact MAC: the frozen spec mandates one normalization rule applied identically; the code implements exactly that — verdict: false.
- V1 `parseInfo` untested (pre-verified by the layer; confirmed zero test imports of `Discovery`): missing coverage of the id/duid fallback, `TokenAuthSupport` casings, and neither-present path — verdict: medium, patch, group G5.
- V2 `containsMac`/`containsLongSecret` untested (pre-verified; `ResultsTest` never calls them): a stubbed detector would flip `securityInspect` to PASS on a leaked MAC — verdict: medium, patch, group G6.

## Design Notes

The harness is deliberately not production code: `:core` does not exist yet, and duplicating `RedactedLog`, `StableIdentity`, and the pin client is cheaper than building the foundation a gate is meant to authorize. Story 1.5 re-derives them properly; only findings carry forward. Results are JSONL so the report comes from data, not memory, and a disputed row traces to a run.

Decision logic sits in a plain-Kotlin `logic` package with no Android imports, so every I/O matrix row is covered by a JVM test that actually runs on any machine. Android classes only adapt inputs to that logic. Facts that need hardware — `MulticastLock`, Keystore, a live TV — stay `blocked` in the report until a real run records them; no test stands in for them.

## Verification

**Commands:**
- `tools\spike-a\gradlew.bat -p tools/spike-a :app:assembleDebug` -- expected: BUILD SUCCESSFUL.
- `tools\spike-a\gradlew.bat -p tools/spike-a :app:testDebugUnitTest` -- expected: identity, redaction, and pin-compare tests pass.

**Environment (verified 2026-10-23):** JDK 17.0.20.1 at `C:\Users\KAVEN\dev\toolchain\jdk-17`; Android SDK at `C:\Users\KAVEN\dev\toolchain\android-sdk` (platform 36, build-tools 36.0.0, platform-tools 37.0.1); Gradle 9.5.0 at `C:\Users\KAVEN\dev\toolchain\gradle-9.5.0`, kept off `PATH` and used only to generate the wrapper. A minimal AGP 9.3.1 app built with this exact chain: `BUILD SUCCESSFUL in 3m 29s`, 874 KB `app-debug.apk`. Until the wrapper exists, the same build runs as `C:\Users\KAVEN\dev\toolchain\gradle-9.5.0\bin\gradle.bat -p tools/spike-a :app:assembleDebug`.

This shell does not inherit `JAVA_HOME` or `ANDROID_HOME`; export both from the paths above before either command. `tools/spike-a/local.properties` carries `sdk.dir` and is gitignored, so it stays machine-local.

**Manual checks (if no CLI):**
- Watch `adb logcat` through a full run for any token, pin, SPKI, or full MAC.
- Confirm backup exclusion covers the app, and that clearing app data loses the token.
