---
title: 'Spike A: prove the pairing and identity path'
type: 'feature'
created: '2026-10-23'
status: 'ready-for-dev'
route: 'dispatch'
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
- [ ] `tools/spike-a/settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, wrapper -- standalone build on Gradle 9.5.0, AGP 9.3.1, JDK 17, Kotlin 2.4.21 -- keeps the production graph untouched.
- [ ] `tools/spike-a/app/src/main/AndroidManifest.xml`, `res/xml/backup_rules.xml` -- `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`; backup and data-extraction excluded -- AC5, AC8.
- [ ] `.../LanGate.kt` -- one `MulticastLock` holder and the permission gate, `LanDenied` on refusal -- AC2 denial path.
- [ ] `.../Discovery.kt` -- SSDP, `/api/v2/` identity read, manual-IP entry -- AC2.
- [ ] `.../PinnedTls.kt` -- capture, store, compare the leaf SPKI, fail closed -- AC5.
- [ ] `.../Channel.kt` -- connect, prompt, token capture, unauthorized, close, visible backoff, no queue or retry -- AC3, AC4, AC7.
- [ ] `.../StableIdentity.kt` + unit test -- precedence, one normalization, `TvId` hash, session-only fallback -- AC6.
- [ ] `.../Vault.kt` -- token and pin per identity alias in Keystore-backed storage -- AC5.
- [ ] `.../Results.kt` + unit test -- JSONL rows, redaction of token, pin, SPKI, MAC -- AC8, AC9.
- [ ] `.../SpikeAScreen.kt` -- one scrollable run log and the run buttons; no product UI -- AC10.
- [ ] `_bmad-output/implementation-artifacts/spike-a-pairing.md` -- report skeleton, verdict `blocked` until runs exist -- AC9.
- [ ] `.gitignore` -- ignore harness build output only.

**Acceptance Criteria:**
- Given at least three Tizen 2016+ TVs and a physical phone on one LAN, when the harness runs, then it records model, year, region, exact firmware, and network type per TV, and a missing TV blocks the gate rather than passing it.
- Given a completed run set, when the report is written, then each row cites recorded results and states pass, fail, or blocked, the exact firmware, the re-prompt and pin-stability outcomes, the no-remote Allow result, and any identity-collision gap.
- Given the finished harness, when storage, logs, and TLS configuration are inspected, then no token, pin, SPKI, or full MAC is logged or backed up, and no trust-all or WSS-to-WS path exists.

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Design Notes

The harness is deliberately not production code: `:core` does not exist yet, and duplicating `RedactedLog`, `StableIdentity`, and the pin client is cheaper than building the foundation a gate is meant to authorize. Story 1.5 re-derives them properly; only findings carry forward. Results are JSONL so the report comes from data, not memory, and a disputed row traces to a run.

## Verification

**Commands:**
- `tools\spike-a\gradlew.bat -p tools/spike-a :app:assembleDebug` -- expected: BUILD SUCCESSFUL.
- `tools\spike-a\gradlew.bat -p tools/spike-a :app:testDebugUnitTest` -- expected: identity, redaction, and pin-compare tests pass.

**Environment (verified 2026-10-23):** JDK 17.0.20.1 at `C:\Users\KAVEN\dev\toolchain\jdk-17`; Android SDK at `C:\Users\KAVEN\dev\toolchain\android-sdk` (platform 36, build-tools 36.0.0, platform-tools 37.0.1); Gradle 9.5.0 at `C:\Users\KAVEN\dev\toolchain\gradle-9.5.0`, kept off `PATH` and used only to generate the wrapper. A minimal AGP 9.3.1 app built with this exact chain: `BUILD SUCCESSFUL in 3m 29s`, 874 KB `app-debug.apk`. Until the wrapper exists, the same build runs as `C:\Users\KAVEN\dev\toolchain\gradle-9.5.0\bin\gradle.bat -p tools/spike-a :app:assembleDebug`.

**Manual checks (if no CLI):**
- Watch `adb logcat` through a full run for any token, pin, SPKI, or full MAC.
- Confirm backup exclusion covers the app, and that clearing app data loses the token.
