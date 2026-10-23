# Epic 1 Context: Pair and press

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Ship the single-TV slice: find a Samsung TV on the local network, complete its Allow prompt, trust the connection, and drive a remote where every key and the power-off action is honest about what the phone observed. It also lays the gated foundation that later epics extend.

## Stories

- Story 1.1: Prove pairing and identity (Spike A)
- Story 1.2: Prove key delivery and evidence (Spike A)
- Story 1.3: Installable Graphite Mint shell
- Story 1.4: Gate local-network access
- Story 1.5: Discover, pair, persist trust
- Story 1.6: Inspect and forget a TV
- Story 1.7: Reconnect and recover identity changes
- Story 1.8: Capability labels from the matrix
- Story 1.9: Switch the one paired TV
- Story 1.10: Open the Remote and connect
- Story 1.11: D-pad and honest command truth
- Story 1.12: Utility keys and honest power off
- Story 1.13: Repeat holds without late-evidence rewrite
- Story 1.14: Complete the accessibility floor
- Story 1.15: Explain model limits once

## Requirements & Constraints

- Scope: discovery and manual IP, pairing trust, reconnect, MVP keys and repeat, command truth, model limits, power off, LAN gate, persistence, local-only egress, matrix harness. Single-TV only — no multi-TV picker, Settings or full TV details, wake, text, pointer, voice, cast, or mirror.
- Gates: nothing ships before both Spike A reports cover at least three Tizen 2016+ TVs and a physical phone as per-model/firmware/network rows. A hard failure yields a re-plan, never a "mostly passed" note.
- Truth: a socket write is never a TV action; Submitted, Unconfirmed and Observed are distinct; no queue, replay, or silent retry; silence is never "failed." No support claim without a recorded matrix pass for the exact model, byte-exact firmware, and network type; H/J 2014–15 stays best-effort regardless of a pass.
- Security and build: pin validated before the token, Keystore-backed and backup-excluded, no trust-all manager or WSS→WS fallback, `RedactedLog` only, no telemetry, one `PrivatePeer` predicate. The manifest omits `RECORD_AUDIO`, `POST_NOTIFICATIONS`, and MediaProjection; `VIBRATE` is the only install-time addition. `./gradlew build lint :core:test` stays green; tests are JVM-only with fakes and no live TV.
- Floor and exit: WCAG 2.2 AA (AAA body), 48dp targets, 200% font, 1.3× expansion, RTL, reduced motion, color never the sole cue; 360×800dp gates release, 360×640dp is a stress check. Exit evidence adds the no-remote Allow path, distinct `TvId`s from same-model TVs, and an observed power-off.

## Technical Decisions

- Fixed graph: `:app → :tv, :cast, :core`; `:tv → :core`; `:cast → :core`; `:core` is JVM-only. Public seams only — `TvControl`, the `PairingStore` port (declared in `:tv`, implemented in `:app`), `CommandTracker`, `ConnectionHealth`, and `:core` value types. `:app` never imports `okhttp3`, jUPnP, `KEY_*`, or a frame type.
- Identity: `StableIdentity` is the reported device ID first, byte-exact MAC second, under one normalization rule; `TvId` is its hash, immutable, never an IP. No resolvable identity means a session-only target with in-memory trust and no Room record or Keystore alias.
- One writer per value: `CommandTracker` in `:tv` (single outcome flow, socket-bound generation, injected timeout, cancelled with the session); `ConnectionHealth` in `:tv/channel` (all 8001/8002 observations, power `On`/`Unknown`); `TargetSession` in `:app` with no persisted last-used value; `CapabilityLabelTracker` in `:app` as the only resolver caller. `:app` combines read-only state under fixed precedence: identity changed, pairing required, offline, reconnecting, asleep, awake.
- Commands: exactly one `submit()` per user command; adapters emit `Evidence` (`TvEvent`, `StateProbe`, `UserConfirmed`), never a verdict; repeat frames and synthesized press/release never submit. Only D-pad directions repeat, through one engine in `:tv` fed a `:app`-supplied `RepeatConfig`.
- Network: one `LanAccessGate` owns the permission, the single process-wide `MulticastLock`, the isolation diagnostic, and manual IP; `:tv` and `:cast` map denial to their own `LanDenied` failure and never request permissions. Discovery is SSDP plus `GET :8001/api/v2/` with results as found, no spinner.
- Data and build: the matrix is a versioned in-repo asset keyed by byte-exact model/firmware, closed network type, and capability key, with one `*unknown*` fallback and a pure `:core` resolver; one display owner recomputes and never stores a label, and the debug overlay wins only in debug. Room holds the TV record, chip dismissals, and overlay; Keystore holds token and pin per alias; no DataStore scalar; `Instant` UTC. Toolchain is pinned in the version catalog; application ID `com.unirem` is confirmed before any signed build.

## UX & Interaction Patterns

- Graphite Mint, dark-only: tonal surfaces with 1dp hairlines, no shadows or gradients, custom line icons (no Material icons, emoji, or text-as-icon), 4dp spacing, 480dp maximum column. Remote composition is topbar switcher, banner, utility row, function keys, a non-interactive ring holding the D-pad, the state line, and the always-visible disabled launcher.
- The ring reports command state and the health dot reports connection health; neither is relabeled as the other. Vocabulary is Idle, Sent, Waiting for TV…, TV confirmed, You confirmed, Offline — "Paired" is never "Connected," and silence is never "failed." Limits are explained once, persist in TV details, and never disable the affected key.

## Cross-Story Dependencies

- 1.1 and 1.2 hard-gate 1.3 onward; their findings set pairing behavior, per-capability timeout, repeat cadence, and per-key evidence rules.
- Backbone: 1.3 scaffold → 1.4 gate before any network attempt → 1.5 discovery/pairing/persistence → 1.6 trust sheet and Forget → 1.7 connect and re-pair → 1.9 target and one-TV switcher → 1.10 Remote shell (removes 1.7's status surface) → 1.11 command truth → 1.12 remaining keys → 1.13 repeat and late evidence → 1.14 accessibility audit → 1.15 chips and durable records.
- 1.8's labels feed the card, switcher row, and 1.15; 1.5's pairing is reused by re-pair, 1.7's health and banner by 1.10/1.15, 1.11's tracker by 1.12–1.15 — no second implementation. Epic 2 extends this owner for multi-TV and adds Settings, full TV details, and wake; Epics 3–5 build on it, Epic 7 verifies all.
