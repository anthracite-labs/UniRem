---
title: 'Spike A: pairing and identity report'
status: 'blocked'
created: '2026-10-23'
spec: '_bmad-output/implementation-artifacts/spec-1-1-prove-the-pairing-and-identity-path-spike-a.md'
harness: 'tools/spike-a'
baseline_commit: '54397f7ea8ee34aae4914e5b4fc641010491a051'
verdict: 'blocked'
---

# Spike A — pairing and identity

This report is filled from `Results` JSONL rows only. No row is promoted from
memory, a model table, a probe, or a user confirmation. A green build is not a
pass.

## Gate decision

**BLOCKED.** No recorded run exists for this report. The harness builds and its
JVM logic tests pass, but that is not evidence about a TV. The gate stays shut
until at least three Tizen 2016+ TVs (including one recent set) and a physical
Android phone on the same LAN produce recorded rows.

A row is `pass` only with a recorded run for that model, byte-exact firmware,
and network type. `blocked` is not `pass`. A hard failure produces a re-plan
recommendation, never a "mostly passed" note.

## Required test set

| # | Model / year / region | Exact firmware | Network (Wi-Fi band / Ethernet / VLAN) | Row status |
|---|---|---|---|---|
| 1 | TBD / TBD / TBD | TBD | TBD | blocked — no run |
| 2 | TBD / TBD / TBD | TBD | TBD | blocked — no run |
| 3 (recent set) | TBD / TBD / TBD | TBD | TBD | blocked — no run |
| Phone | Physical Android test phone | — | Same LAN as the TVs | blocked — no run |

If the required test set is unavailable, the story is blocked and the gate is
not marked passed.

## Matrix rows

Columns follow `_bmad-output/unirem/spec-samsung-remote/verification-matrix.md`.
Every cell below is `blocked` until a Results row supplies it.

| Model / year / region | Firmware | Network | /api/v2/ | Pair prompt | Token persists | Re-prompt? | Pin stable | Keys MVP | Text native | Text app-grid | Pointer | Wake | DMR | JPEG/MP3/H264 | HEVC | Subtitles | Seek | Notes |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TBD | TBD | TBD | blocked | blocked | blocked | blocked | blocked | out of scope | out of scope | out of scope | out of scope | out of scope | out of scope | out of scope | out of scope | out of scope | out of scope | Spike A covers pairing/identity only |
| TBD | TBD | TBD | blocked | blocked | blocked | blocked | blocked | out of scope | out of scope | out of scope | out of scope | out of scope | out of scope | out of scope | out of scope | out of scope | out of scope | Spike A covers pairing/identity only |
| TBD | TBD | TBD | blocked | blocked | blocked | blocked | blocked | out of scope | out of scope | out of scope | out of scope | out of scope | out of scope | out of scope | out of scope | out of scope | out of scope | Spike A covers pairing/identity only |

Key/Text/Pointer/Wake/DMR/JPEG/HEVC/Subtitles/Seek belong to Spike A2, B, C, and
later gates; they are not claimed here.

## Acceptance evidence checklist

| AC | What would count | Current status |
|---|---|---|
| AC1 — test set and harness | Three Tizen 2016+ TVs (one recent) and a phone; model, year, region, byte-exact firmware, network per TV recorded; discovery, manual IP, and WSS attempted without the production app | blocked — harness exists, no recorded run |
| AC2 — discovery and manual IP | SSDP M-SEARCH under a held multicast lock; `GET <tv>:8001/api/v2/`; manual IP offered on empty discovery; session-only target when no identity; `LanDenied` when the gate is denied | blocked — no recorded run |
| AC3 — first pairing and Allow/Deny | TV prompt; Allow yields `ms.channel.connect` with a token on token-auth models; Deny/timeout yields `ms.channel.unauthorized` or a close with nothing persisted; no-remote Allow result recorded | blocked — no recorded run |
| AC4 — token persistence and re-prompt | Reconnect after process restart skips the prompt on models that persist the token; re-prompt recorded per firmware; visible re-pair exercised; no token, pin, or full MAC in logs | blocked — no recorded run |
| AC5 — SPKI pin capture and validation | Leaf SPKI captured and stored with the token, keyed by identity, Keystore-backed, backup-excluded; every later connect validates the pin before attaching the token; mismatch fails closed into visible re-pair; no trust-all manager, no hostname suppression outside the pinned client, no WSS-to-WS fallback | blocked — no recorded run |
| AC6 — stable identity and two TVs | Device ID first, byte-exact MAC second, one normalization rule; `TvId` is the hash; two distinct records; not derived from IP or connectivity path; same-model gap recorded and synthetic identical-ID test run | blocked — no recorded run; synthetic logic test is not a real collision |
| AC7 — reconnect and failure states | Visible backoff; token rejection/expiry surfaces "Pairing is required for this TV." with re-pair; socket close surfaces offline; no command silently queued or retried | blocked — no recorded run |
| AC8 — security inspection | No token, pin, SPKI, full MAC, or pairing payload logged or backed up; AC5 security checks evidenced, not asserted | blocked — runtime inspection not yet recorded on device |
| AC9 — spike report and matrix rows | This report with one row per TV/firmware/network, pass/fail/blocked, exact firmware, re-prompt and pin-stability results, no-remote Allow result, identity-collision gap, gate decision, and re-plan recommendation if hard-failed | blocked — report skeleton only |
| AC10 — scope discipline | Harness and report only; no production UI, keys, multi-TV switcher, Settings, voice, cast, or mirror | blocked — harness-only diff built, but no independent scope review recorded |

## Harness

- Location: `tools/spike-a/` — a standalone Android app. It is not in any
  `settings.gradle.kts` and adds no Gradle module to the production graph.
- Build: Gradle 9.5.0 wrapper, AGP 9.3.1, JDK 17, Kotlin 2.4.21, `compileSdk 36`,
  `minSdk 26`, `targetSdk 36`.
- Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`,
  `CHANGE_WIFI_MULTICAST_STATE`. Nothing else.
- Backup: `android:allowBackup="false"` plus `backup_rules.xml` and
  `data_extraction_rules.xml` exclude every domain.
- Storage: token and pin are AES/GCM ciphertext in app-private SharedPreferences;
  the wrapping key is generated in AndroidKeyStore under the fixed alias
  `unirem-spike-a-vault`. Each entry is keyed by the `TvId` hash
  (`pin_<hex>`, `token_<hex>`), never an IP or MAC.
- Results: append-only JSONL at
  `/data/data/com.unirem.spikea/files/spike-a-results.jsonl`. Pull with
  `adb exec-out run-as com.unirem.spikea cat files/spike-a-results.jsonl`.
- Redaction: the row schema has no token, pin, SPKI, or full-MAC field; free text
  is scrubbed; a MAC appears only as `xx:xx:xx:xx:xx:??`; a `TvId` appears only
  as a 12-hex-character prefix.
- TLS: trust-on-first-use pinned to the leaf SPKI; the pin is validated before a
  token is attached; mismatch fails closed into re-pair with no token sent; no
  trust-all manager on a token path; hostname verification is relaxed only
  inside the pinned client; the channel URL builder emits `wss://` only.
- Discovery: SSDP M-SEARCH to `239.255.255.250:1900` under one `MulticastLock`,
  then `GET <tv>:8001/api/v2/`; manual IPv4 entry attempts the same read.
- No queue, no silent retry, no replay: every network run is one manual press;
  backoff is visible and only gates the next manual attempt.

## Harness verification (build-time evidence, not gate evidence)

Checks run on 2026-10-23 with `JAVA_HOME` and `ANDROID_HOME` exported from the
paths in the spec:

| Check | Command | Result |
|---|---|---|
| Debug APK | `tools\spike-a\gradlew.bat -p tools/spike-a :app:assembleDebug` | `BUILD SUCCESSFUL` |
| JVM logic tests | `tools\spike-a\gradlew.bat -p tools/spike-a :app:testDebugUnitTest` | 41 tests, 0 failures |
| Android lint | `tools\spike-a\gradlew.bat -p tools/spike-a :app:lintDebug` | 0 errors, 16 warnings |

The 16 lint warnings are advisory: seven `SetTextI18n` (harness screen text, not
product UI and not translated), two `CustomX509TrustManager`, two
`AndroidGradlePluginVersion` plus one `GradleDependency` and two
`NewerVersionAvailable` (newer tool or library versions exist), one `OldTargetApi`
(targetSdk 36; only platform 36 is installed), and one `MissingApplicationIcon`
(harness only).

The two `CustomX509TrustManager` warnings are the pinning mechanism itself: the
capture manager accepts exactly the first leaf and rejects a change within the
same handshake; the pinned manager accepts only the stored leaf SPKI. Neither is
trust-all, and the capture manager is never used on a token-bearing channel.
`usesCleartextTraffic="true"` is present because `GET <tv>:8001/api/v2/` is plain
HTTP by protocol; that info read is the only cleartext URL the harness constructs
(`Discovery.readInfo`), and the channel builder emits `wss://` only.

Built-APK manifest, dumped with
`aapt2 dump xmltree tools\spike-a\app\build\outputs\apk\debug\app-debug.apk --file AndroidManifest.xml`:

- exactly four `uses-permission` entries: `INTERNET`, `ACCESS_NETWORK_STATE`,
  `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`;
- `allowBackup=false`;
- `fullBackupContent=@0x7f030000` (`res/xml/backup_rules.xml`);
- `dataExtractionRules=@0x7f030001` (`res/xml/data_extraction_rules.xml`);
- `usesCleartextTraffic=true`;
- `MainActivity` `exported=true`.

This is build evidence only. It does not make any TV row `pass` and does not move
the gate. Runtime security inspection still records `blocked` until a pairing
creates the Keystore alias.

## How to run

Run only on a trusted LAN: first pairing is trust-on-first-use and pins the
first leaf the TV presents. TV preconditions: pairing mode on, phone and TV on
the same subnet with no AP isolation; the harness waits up to 60 s for the
pair-prompt answer and 15 s on reconnect. The phone row in the required test
set is context only; only TV rows feed `GateRules.verdict`.

1. Build and install:
   - `tools\spike-a\gradlew.bat -p tools/spike-a :app:assembleDebug`
   - `adb install -r tools/spike-a/app/build/outputs/apk/debug/app-debug.apk`
2. Open **UniRem Spike A**. Enter the TV IPv4 (manual fallback), a label, the
   region, and the model year.
3. Press **Scan SSDP**; if discovery is empty, press **Read /api/v2/** with the
   TV IPv4. A target with no device ID and no MAC is marked session-only.
4. Press **Pair (Allow)** and answer Allow on the TV. Record whether Allow could
   be answered without the physical remote by ticking the checkbox before
   pairing.
5. Force-stop and reopen the app, then press **Reconnect**. Record whether the TV
   showed a prompt with the reconnect checkbox.
6. Press **Revalidate pin**. A changed leaf must fail closed into visible
   re-pair with no token sent.
7. For a second TV, scan again, press **Next discovered TV**, and pair it; press
   **Two-TV check**. If no same-model pair is available, record the gap and run
   **Synthetic collision**.
8. For the denial path, clear pairing, remove the phone from the TV's external
   device manager, press **Deny test**, and press Deny on the TV.
9. Press **Inspect security** and record every check.
10. Pull the JSONL and fill this report only from those rows.

## Results schema

Each line is one JSON object with `runId`, `at` (UTC ISO-8601), `scenario`,
`result` (`pass`/`fail`/`blocked`), model, year, region, byte-exact firmware,
TV network type, phone network detail, identity source, `tvIdPrefix`,
`sessionOnly`, `allowWithoutRemote`, `prompt`, `tokenPersists`, `rePrompt`,
`pinStable`, `collision`, `tokenSent`, `notes`, and a `checks` object.

`GateRules` returns `pass` only when there are at least three distinct
`tvIdPrefix` values and no `fail` or `blocked` row. Empty results are `blocked`.

## Identity rules

- Precedence: the reported device ID first, the reported MAC second.
- One normalization rule for both: trim, lowercase (`Locale.ROOT`), remove
  whitespace. MAC separators are preserved; the raw MAC stays a separate
  in-memory field for later wake provenance, and this spike stores no wake MAC.
- `TvId` = SHA-256 of the normalized identity with its kind (`device-id:` or
  `mac:`), never an IP and never the current connectivity path.
- No resolvable identity: session-only target, in memory only, no durable record,
  no Keystore alias.
- Collision: two targets with the same `TvId`. A same-model pair is preferred;
  the synthetic test proves only the logic, not the hardware.

## Security inspection

Runtime checks recorded by **Inspect security**:

- `allowBackup` must be `false`.
- `dataExtractionRules` and `fullBackupContent` must be present.
- The AndroidKeyStore alias must exist and every Vault preference value must
  decrypt as ciphertext.
- The results file must contain no full MAC and no long hex/base64 secret.
- `cleartextPermitted` is expected `true` only because `/api/v2/` on 8001 is plain
  HTTP by protocol; the channel scheme is `wss` and there is no WSS-to-WS path.
- `wssToWsFallback=none` and `hostnameVerifier=pinned-client-only` are code-level
  claims, evidenced by `ChannelUrlTest` and inspection of `PinnedTls`, not by a
  runtime probe.

Manual checks:

- Watch `adb logcat` through a full run for any token, pin, SPKI, or full MAC.
- Confirm clearing app data loses the token and that backup/transfer excludes the
  app.

## No-remote Allow result

Not recorded. The pair row's `allowWithoutRemote` field is `yes`/`no`. If the
prompt cannot be answered without a physical remote, the report must document the
required fallback and flag the pilot premise.

## Identity-collision gap

The real same-model collision has not been run. The synthetic identical-device-ID
test proves the normalization/hash/collision logic only. The gate cannot fully
pass until the real collision is resolved or the gap is explicitly accepted by a
re-plan.

## Re-plan triggers

- Any TV/firmware/network row fails pairing, token persistence, pin stability, or
  identity distinctness.
- The Allow prompt cannot be answered without a physical remote and no fallback
  is acceptable.
- A pin mismatch cannot be made visible and fail-closed.
- Any token, pin, SPKI, or full MAC appears in logs, storage, or backup.
- Fewer than three distinct Tizen 2016+ TVs (including one recent) are available.

## Change log

- 2026-10-23 — skeleton created with the harness; verdict `blocked`; every matrix
  row `blocked`; no run recorded.
- 2026-10-23 — build verified: `:app:assembleDebug` successful, 41 JVM tests pass,
  `:app:lintDebug` 0 errors/16 warnings; built-APK manifest checked with `aapt2`
  (four permissions, backup excluded, cleartext used only for the 8001 info read);
  AC10 moved to `blocked` pending scope review. No TV run recorded, so the verdict
  and every matrix row stay `blocked`.
