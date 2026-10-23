---
title: 'Spike A2: key delivery and command evidence report'
status: 'blocked'
created: '2026-10-23'
spec: '_bmad-output/implementation-artifacts/spec-1-2-prove-key-delivery-and-command-evidence-spike-a.md'
harness: 'tools/spike-a'
baseline_commit: 'c8c61b12e9e6aef22674dce66bfdcce219fd3f8c'
verdict: 'blocked'
---

# Spike A2 — key delivery and command evidence

This report is filled from `Results` JSONL rows only. No row is promoted from
memory, a model table, a probe, or a user confirmation. A green build is not a
pass.

## Gate decision

**BLOCKED.** No recorded run exists for this report. The harness builds and its
JVM logic tests pass, but that is not evidence about a TV. The gate stays shut
until the Story 1.1 pairings exist and recorded key runs cover the required
test set below.

A row is `pass` only with a recorded run for that model, byte-exact firmware,
and network. `blocked` is not `pass`. A hard failure produces a re-plan
recommendation, never a "mostly passed" note. Without the Story 1.1 pairings
the gate stays shut no matter what builds.

## Required test set

| # | Model / year / region | Exact firmware | Network (Wi-Fi band / Ethernet / VLAN) | Row status |
|---|---|---|---|---|
| 1 | TBD / TBD / TBD | TBD | TBD | blocked — no run |
| 2 | TBD / TBD / TBD | TBD | TBD | blocked — no run |
| 3 (recent set) | TBD / TBD / TBD | TBD | TBD | blocked — no run |
| Phone | Physical Android test phone | — | Same LAN as the TVs | blocked — no run |

The phone row is context only; only TV rows feed `GateRules.verdict`.

Per Story 1.2 AC1, the Story 1.1 pairings are the entry ticket: if no pairing
is stored for a TV, the key session refuses to open and every key row stays
`blocked`.

## Matrix rows

Every cell below is `blocked` until a Results row supplies it.

| Model / year / region | Firmware | Network | MVP key (1 frame) | Evidence source | Latency | Repeat | Late evidence | Socket close | Power off | Notes |
|---|---|---|---|---|---|---|---|---|---|---|
| TBD | TBD | TBD | blocked | blocked | blocked | blocked | blocked | blocked | blocked | No run recorded |
| TBD | TBD | TBD | blocked | blocked | blocked | blocked | blocked | blocked | blocked | No run recorded |
| TBD | TBD | TBD | blocked | blocked | blocked | blocked | blocked | blocked | blocked | No run recorded |

## Per-key evidence

One row per MVP key per TV, filled from `gesture` rows (`pass`) only. `key`
rows are submits: they stay `blocked` by design (a write is not a TV action)
and never count as evidence.

| Key | DataOfCmd | TV 1 effect / source | TV 2 effect / source | TV 3 effect / source | Status |
|---|---|---|---|---|---|
| Up | KEY_UP | blocked | blocked | blocked | blocked — no run |
| Down | KEY_DOWN | blocked | blocked | blocked | blocked — no run |
| Left | KEY_LEFT | blocked | blocked | blocked | blocked — no run |
| Right | KEY_RIGHT | blocked | blocked | blocked | blocked — no run |
| OK | KEY_ENTER | blocked | blocked | blocked | blocked — no run |
| Back | KEY_RETURN | blocked | blocked | blocked | blocked — no run |
| Home | KEY_HOME | blocked | blocked | blocked | blocked — no run |
| Apps | KEY_CONTENTS | blocked | blocked | blocked | blocked — no run; provisional wire code |
| Search | KEY_SEARCH | blocked | blocked | blocked | blocked — no run; provisional wire code |
| Vol+ | KEY_VOLUP | blocked | blocked | blocked | blocked — no run |
| Vol- | KEY_VOLDOWN | blocked | blocked | blocked | blocked — no run |
| Mute | KEY_MUTE | blocked | blocked | blocked | blocked — no run |
| Play | KEY_PLAY | blocked | blocked | blocked | blocked — no run |
| Pause | KEY_PAUSE | blocked | blocked | blocked | blocked — no run |
| Power | KEY_POWER | blocked | blocked | blocked | blocked — no run; discrete key, never a toggle |

Apps (`KEY_CONTENTS`, the long-standing Samsung Smart Hub / Apps-panel key)
and Search (`KEY_SEARCH`, following the `KEY_*` convention for the documented
Search key) are provisional wire codes: the protocol short list names neither.
A firmware that ignores either is recorded `unsupported-key` (one-time chip
candidate), never failed. The hardware run settles both codes.

## Latency

No submit-to-evidence samples exist. The 3 s default stands; no per-group
recommendation is made. A recommendation needs at least 5 samples per group
(`LatencyStats.MIN_SAMPLES`); fewer than that records `latency` as `blocked`,
never a recommendation from one sample.

| Group | Samples | Median | Tail (p95) | Recommendation | Status |
|---|---|---|---|---|---|
| nav | 0 | — | — | 3 s default kept | blocked — no run |
| system | 0 | — | — | 3 s default kept | blocked — no run |
| volume | 0 | — | — | 3 s default kept | blocked — no run |
| transport | 0 | — | — | 3 s default kept | blocked — no run |
| power | 0 | — | — | 3 s default kept | blocked — no run |

## Repeat

No hold run exists. The engine is single, D-pad-only, 500 ms delay with
~80 ms cadence, `Release` on lift. A second hold while one is active, or a
hold of a non-D-pad key, is refused; there is no second engine and no
synthesized repeat path.

| Check | Status |
|---|---|
| Direction held past 500 ms repeats ~80 ms until release | blocked — no run |
| Release frame sent on lift | blocked — no run |
| TV effect and dropped/reordered frames recorded | blocked — no run |
| Second engine or synthesized repeat path | none exists (code-level; exactly one `RepeatEngine` in `Harness`) |

## Power off

No power run exists. Power is the discrete `KEY_POWER` sent once — never a
toggle. Evidence is a state probe (`GET <tv>:8001/api/v2/` reachability plus
key-channel open/closed), never a sheet, never a guess. A TV that still
answers is recorded off, asleep, or unknown — still answering means the state
is unknown (`blocked`), never marked success without observation.

| Check | Status |
|---|---|
| Distinct power key sent once | blocked — no run |
| TV off / asleep / unknown via probe | blocked — no run |
| Unobserved stays unconfirmed | blocked — no run |

## Unsupported keys

None recorded. Keys a firmware ignores stay usable for the rest of the set
and are recorded `unsupported-key` as one-time capability-chip candidates,
never failed.

## Acceptance evidence checklist

| AC | What would count | Current status |
|---|---|---|
| AC1 — paired setup | 1.1-paired TVs and a phone on one LAN; key session opens on the pinned token channel; model, firmware, network, on/reachable recorded; missing pairing blocks instead of passing | blocked — harness built, no recorded run |
| AC2 — MVP key + evidence | One gesture submits exactly one frame with the correct `DataOfCmd`; per-key evidence cites machine event, probe, or operator confirmation; no socket write recorded as a TV action | blocked — no recorded run |
| AC3 — storage, logs, TLS, repeat | No token, pin, SPKI, or full MAC logged or backed up; exactly one repeat engine; WSS-only with pin validated before token attach | blocked — runtime inspection not yet recorded on device (code-level claims unit-tested) |

## Harness

Story 1.1 harness extended in place (`tools/spike-a/`, standalone build,
Gradle 9.5.0 / AGP 9.3.1 / JDK 17 / Kotlin 2.4.21). Same permissions, same
backup exclusion, same vault, same redaction contract; `ResultRow` gained no
columns — key code, frame, latency, generation, and evidence source ride the
`checks` map, which cannot carry a secret (forbidden key parts and value
scrubbing are unit-tested, including every new check key).

New logic (all JVM-tested, no Android dependency):

- `logic/KeyFrame.kt` — Click/Press/Release builders plus the 15-key MVP
  `DataOfCmd` table. One gesture maps to exactly one frame.
- `KeySession.kt` — long-lived sibling of `Channel` (one-shot latch-then-close
  was the wrong shape for a held socket). Verifies the pin, attaches the
  token, holds the socket open, sends frames, forwards machine events with
  their generation context, closes honestly. `send` reports only socket
  acceptance, never a TV action.
- `logic/RepeatEngine.kt` — the single repeat engine: D-pad only, 500 ms
  delay, ~80 ms cadence, `Release` on lift. A second hold or a non-D-pad hold
  is refused.
- `logic/GestureTracker.kt` — per-gesture generation. Evidence resolves only
  on a generation match, so late evidence for an older press can never rewrite
  a newer press; a socket close parks open gestures at Idle. Observed rows are
  never rewritten.
- `logic/LatencyStats.kt` — median and p95 tail per key group, 5-sample floor,
  3 s default kept unless runs prove a per-group tail needs more. Plus the
  power-probe interpretation (silence on either channel is evidence; still
  answering is unknown, never a guess).
- `logic/Results.kt` — new `Scenario` values `key`, `repeat`, `latency`,
  `gesture`, `power-off`, `unsupported-key`. The app-side `Results.kt` append
  path needed no change: it serializes `scenario.wire` generically.
- `Harness.kt` — key-run methods following the `pair` one-press pattern: open
  (pin revalidated before token attach), tap, hold start/stop, operator /
  machine / probe confirmations, not-observed, unsupported-key record,
  power-off with delayed state probe, latency summary, close. Socket close
  stops repeat, parks gestures Idle, disables keys, records the best-effort
  Release possibility; no queue, no silent retry, no replay, no command after
  close (attempts record `blocked`, proving nothing was sent).
- `SpikeAScreen.kt` — per-key buttons, hold controls, Observed / Not observed
  / Observed-via-machine / Observed-via-probe, unsupported marking, latency
  readout, key-session status. `SetTextI18n` suppressed file-wide with a
  comment: this is a harness surface, never translated.

Machine events are logged with the open-gesture context but never
auto-confirm: the harness cannot know which event evidences which press, and a
delayed event must never rewrite a newer press. The operator binds an event to
its exact generation with Observed-via-machine; an ambiguous state (more than
one open gesture) refuses to confirm.

## Harness verification (build-time evidence, not gate evidence)

Checks run on 2026-10-23 with `JAVA_HOME` and `ANDROID_HOME` exported from the
toolchain paths in the build machine (`jdk-17`, `android-sdk`):

| Check | Command | Result |
|---|---|---|
| JVM logic tests | `tools\spike-a\gradlew.bat -p tools/spike-a :app:testDebugUnitTest` | 99 tests, 0 failures |
| Debug APK | `tools\spike-a\gradlew.bat -p tools/spike-a :app:assembleDebug` | `BUILD SUCCESSFUL` |
| Android lint | `tools\spike-a\gradlew.bat -p tools/spike-a :app:lintDebug` | 0 errors, 9 warnings |

The 9 lint warnings are advisory and below the 16 recorded for Story 1.1: two
`CustomX509TrustManager` (the pinning mechanism itself, never trust-all),
two `AndroidGradlePluginVersion` plus one `GradleDependency` and two
`NewerVersionAvailable` (newer tool or library versions exist), one
`OldTargetApi`, one `MissingApplicationIcon` (harness only). The seven
`SetTextI18n` warnings from Story 1.1 are now suppressed file-wide on the
harness screen with a never-translated comment. `usesCleartextTraffic="true"`
is unchanged: only the 8001 info read is plain HTTP; the channel builder emits
`wss://` only.

This is build evidence only. It does not make any TV row `pass` and does not
move the gate. Runtime security inspection still records `blocked` until key
runs on paired TVs prove the vault, the logs, and the single engine on
device.

## How to run

Same trusted-LAN preconditions as Story 1.1, plus a stored pairing per TV
(`Open keys` refuses without one). One press is one manual run; every press
needs a follow-up mark.

1. Build and install:
   - `tools\spike-a\gradlew.bat -p tools/spike-a :app:assembleDebug`
   - `adb install -r tools/spike-a/app/build/outputs/apk/debug/app-debug.apk`
   - (or install `debug/spike-a-debug.apk`, the published copy)
2. Pair first with the Story 1.1 buttons if no stored pairing exists.
3. Press **Open keys** (pin revalidated, stored token attached).
4. Press one key button per gesture — **Up/Down/Left/Right/OK/Back/Home/Apps/
   Search/Vol+/Vol-/Mute/Play/Pause** — then mark **Observed** (saw the
   effect), **Observed: machine** (run log shows a machine event evidencing
   it), **Observed: probe** (a state probe evidences it), or **Not observed**.
   Unmarked presses stay unconfirmed and the report stays blocked.
5. For repeat: **Hold Up/Down/Left/Right**, lift with **Release hold**, then
   mark the effect as in step 4.
6. For power: **Power off** sends the discrete key once, waits 5 s, then
   probes 8001 and the channel. **Probe power** re-probes later against the
   same power press.
7. Keys a firmware ignores: press, confirm no effect, **Mark unsupported**.
8. Press **Latency summary** after several confirmed presses per group.
9. Press **Close keys** (or pull the cable): repeat stops, gestures Idle, keys
   disabled, no retry. Pull the JSONL and fill this report only from those
   rows:
   `adb exec-out run-as com.unirem.spikea cat files/spike-a-results.jsonl`

## Results schema

Same append-only JSONL row as Story 1.1, plus the six new `scenario` values
above. Per-gesture evidence rides `checks` with keys `key`, `dataOfCmd`,
`group`, `frame`, `evidence` (`unconfirmed`/`machine`/`probe`/`operator`),
`latencyMs`, `generation`; repeat rows add `repeatCount`,
`repeatCadenceMs`, `repeatEngine=single`, `releaseSent`; power rows add
`powerProbe` (`8001=answers|silent;8002=open|closed`) and
`releasePossibility`; latency rows add `sampleCount`, `medianMs`, `tailMs`,
`recommendationMs`, `defaultTimeoutMs`. Every new key survives the forbidden-
key filter by unit test; values carry no secret by construction.

## Security inspection

Same runtime checks as Story 1.1, plus the key surface:

- `allowBackup` must be `false`; backup/data-extraction rules present.
- Keystore alias exists; every vault value decrypts as ciphertext.
- Results file contains no full MAC and no long hex/base64 secret (the audit
  in `securityInspect` covers the new key/latency/repeat rows too).
- `cleartextPermitted` expected `true` only for the 8001 info read and the
  power probe; channel scheme `wss`, no WSS-to-WS path.
- Exactly one repeat engine: one `RepeatEngine` field in `Harness`, no second
  repeat path in the codebase.

Manual checks:

- Watch `adb logcat` through a full key run for any token, pin, SPKI, or full
  MAC. Only `DataOfCmd` values (`KEY_*`) and generation/latency numbers may
  appear.
- Confirm a hold past 500 ms repeats at ~80 ms and sends `Release` on lift.
- Confirm pulling the JSONL shows one `key` submit row per gesture plus
  exactly one evidence row per confirmed gesture.

## Re-plan triggers

- Any TV/firmware/network row fails key delivery, repeat, power, or identity.
- Any socket write is recorded — or mistaken — as a TV action.
- Stale evidence confirms a newer press, or a command is sent after close.
- A second repeat engine or a synthesized repeat path appears.
- Any token, pin, SPKI, or full MAC appears in logs, storage, or backup.
- The 3 s default is replaced on fewer than 5 samples per group.
- The Story 1.1 pairings are unavailable.
- Fewer than three distinct Tizen 2016+ TVs (including one recent) are
  available.

## Change log

- 2026-10-23 — skeleton created with the harness; verdict `blocked`; every
  matrix row `blocked`; no run recorded.
- 2026-10-23 — build verified: `:app:assembleDebug` successful, 99 JVM tests
  pass (60 carried from the Story 1.1 suite, 39 new key/repeat/gesture/
  latency/frame/matrix tests), `:app:lintDebug` 0 errors / 9 warnings (below the 16 from Story 1.1;
  `SetTextI18n` suppressed on the harness-only screen, fixed-rate scheduling
  replaced with fixed-delay per lint). Sideloadable published to
  `debug/spike-a-debug.apk`. No TV run recorded, so the verdict
  and every matrix row stay `blocked`.
