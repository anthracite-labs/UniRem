---
status: final
stepsCompleted: [3]
inputDocuments:
  - _bmad-output/unirem/spec-samsung-remote/spec-samsung-remote.md
  - _bmad-output/unirem/spec-samsung-remote/protocol-reference.md
  - _bmad-output/unirem/spec-samsung-remote/verification-matrix.md
  - _bmad-output/planning-artifacts/architecture/architecture-UniRem-2026-10-23/ARCHITECTURE-SPINE.md
  - _bmad-output/unirem/ux-samsung-remote/DESIGN.md
  - _bmad-output/unirem/ux-samsung-remote/EXPERIENCE.md
---

# UniRem - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for UniRem, decomposing the requirements from the finalized spec (the PRD-equivalent), the UX design contract, and the architecture spine into implementable stories.

> **Requirements source.** No PRD exists for this project. `spec-samsung-remote.md` and its companions are the canonical contract; the architecture spine is the technical law; `DESIGN.md` and `EXPERIENCE.md` are one UX contract. Research, memlogs, reviews, and mockups are traceability evidence, not inputs to this inventory.

## Requirements Inventory

### Functional Requirements

#### Discovery, pairing, and multi-TV (CAP-8)

FR1 (CAP-8): Discover Samsung Tizen TVs on the local network using SSDP M-SEARCH to `239.255.255.250:1900` under a held multicast lock, then fetch `GET <tv>:8001/api/v2/` for the TV name, model, Tizen version, `TokenAuthSupport`, and Wi-Fi MAC.

FR2 (CAP-8): Offer manual IP entry whenever discovery is unavailable, empty, or blocked; a manual-IP target with no resolvable stable identity is session-only, labeled as such, and never receives a durable TV record or Keystore alias.

FR3 (CAP-8): Present discovered TVs with identity details (name, model, Tizen version, masked MAC, "same Wi-Fi as this phone") and an explicit pairing choice: "This is my TV — connect" or "That's not my TV."

FR4 (CAP-8): Pair through the TV's Allow/Deny prompt; on Allow, persist the token and leaf SPKI pin per TV keyed by stable identity; on deny or timeout, store no token and show the specified retry/close paths.

FR5 (CAP-8): Validate the pinned leaf SPKI before attaching the token on every later connect; on mismatch, disable keys, show "This TV's identity changed. Re-pair to continue.", and require re-pair; never trust a changed certificate silently and never fall back from WSS to WS.

FR6 (CAP-8): Support multiple stored TVs with explicit target selection; with one TV, auto-select it; with two or more, open the TV switcher on cold start and require an explicit choice; never silently target the last-used TV.

FR7 (CAP-8): Keep a chosen TV as the target across surfaces within an app session; a process restart or return from a killed state reopens the picker when multiple TVs exist; switching TVs closes Text, Voice, Cast, Mirror, and Wake sheets but does not stop pinned cast or mirror sessions.

FR8 (CAP-8): Reconnect with visible backoff; on token rejection or expiry show "Pairing is required for this TV." and a re-pair action; on socket close disable keys and show the offline banner; never silently queue commands.

#### Keys (CAP-1)

FR9 (CAP-1): Send the MVP key set — D-pad directions, OK, Back, Home, Apps, Search, volume up/down/mute, play/pause, and power off — over the authenticated channel, with one tracked command per user gesture.

FR10 (CAP-1): Press-and-hold a direction repeats after 500ms, then every 80ms; release sends key-up; exactly one repeat engine exists in `:tv`; there are no diagonal keys and no swipe navigation on the hero D-pad.

FR11 (CAP-1): Report every command through Submitted → Unconfirmed/Observed, distinguishing machine observation from user confirmation; never present a socket write as an on-TV effect; ignore evidence for older gestures; resolve an open gesture to Idle on socket close or target change.

FR12 (CAP-1): Keep every key visible; when a model does not report a key, keep it usable with a one-time chip ("This TV doesn't report [key] — it may still work.") and record the limitation in TV details; do not call it failed or hide it without explanation.

FR13 (CAP-1): Disable keys when the socket is closed, show the offline banner and recovery actions, and re-enable on reconnect; never send silently into a closed socket.

#### Text (CAP-2)

FR14 (CAP-2): Provide a Text sheet with a single-line draft for the TV's focused native field or browser URL; explicit Send; no live keystrokes; the draft is held in memory only, cleared on close, and never logged.

FR15 (CAP-2): Send text as Base64 UTF-8 with an explicit IME start/commit/end lifecycle; emit `custom.remote.textReceived` before the first text after IME start; after Send show "Sent. Check the TV." with "It's there" and "Try again"; never say "Text sent successfully."

FR16 (CAP-2): Do not claim to know whether the TV has a focused native field; show the standing helper ("Phone typing works in TV settings and the browser. If this app draws its own keyboard, use the D-pad here."); support a per-surface probe to enable a specific app grid later; never promise "type anywhere."

FR17 (CAP-2): Handle password fields without logging, retaining, or suggesting text; clear the draft on cancel or background; rely on the platform's secure-entry behavior.

#### Pointer (CAP-3)

FR18 (CAP-3): Gate the Pad sheet behind an explicit pointer probe; before first use, explain "This will move the TV's pointer. You can stop anytime." and run the probe; on failure, disable the Pad slot with "This TV didn't pass the pointer check. Keys still work." and record "Not supported" in TV details.

FR19 (CAP-3): In the Pad sheet, drag moves the pointer and tap clicks; use one named absolute TV panel coordinate space, clamped and converted only inside `:tv`; the hero D-pad never becomes a trackpad and never mixes pointer movement with key taps.

#### Voice (CAP-4)

FR20 (CAP-4): Provide a Voice sheet with a 72dp hold-to-talk mic key; press-and-hold starts listening, release stops, slide up while holding locks until "Done"; there is no wake word and no always-listening state.

FR21 (CAP-4): Use the platform speech recognizer and never record, store, or transmit audio; the permission explainer says "Speech is handled by your phone" and does not claim on-device recognition unless the platform guarantees it; no raw-audio injection.

FR22 (CAP-4): Map recognized phrases through one versioned `VoiceCommandMap` to the same `RemoteCommand` vocabulary; non-destructive commands execute on release; power off requires an explicit tap; dictation never auto-sends and requires a "Send to TV" review; one phrase produces exactly one `submit()`.

FR23 (CAP-4): For dictation, show a draft and a "Send to TV" action; require the user to confirm a native field is open or use a recorded per-surface pass; never claim focus detection; if the user says no, show "Open a text field on the TV, then come back" and offer "Use the D-pad."

#### Power and wake (CAP-5)

FR24 (CAP-5): Send power off as a distinct key while the TV is on, not a toggle; no confirmation sheet; visually separate it from OK; show asleep/offline state.

FR25 (CAP-5): Offer Wake as a separate launcher slot, visible on every TV; if the matrix has no recorded wake pass for the model/network, disable the slot with a lock mark and explanation and send no magic packet.

FR26 (CAP-5): With a pass, show the wake checklist (TV plugged in; phone and TV on the same network; "Power On with Mobile" on; MAC saved), send the 102-byte magic packet to UDP ports 9 and 7 (subnet-directed broadcast plus `255.255.255.255`) from a socket bound to the private interface, and report success only when the TV answers on 8001/8002.

FR27 (CAP-5): Capture and persist the TV's Wi-Fi MAC with provenance and network type, separate from `TvId`; never show the full MAC; use it only for wake.

#### Cast (CAP-6)

FR28 (CAP-6): Provide a Cast sheet for one item per session — a photo/video via the system Photo Picker or an audio file via the system document picker — with the active TV preselected and named; no queue, playlist, autoplay, or broad storage permission.

FR29 (CAP-6): Before play, check the renderer; if absent, show "This TV isn't showing a cast receiver. Make sure it's on the same Wi-Fi." and create no session; surface known format/subtitle limits before play with "Play without subtitles," "Try anyway," or "Choose another"; never claim unverified codec, subtitle, seek, or long-file support.

FR30 (CAP-6): Cast via TV-fetches-from-phone: `:cast` mints `MediaId`, owns the URL path shape and id-to-bytes mapping, emits an `http(s)` URL (never `content://`), supports `HEAD` and byte `Range`, serves only the selected item, binds to the interface through which the TV was reached, rejects non-private peers, and stops at session end.

FR31 (CAP-6): Guarantee item readability for the session via a persistable read grant (re-validated at session start) or a session-cache copy under one byte limit checked against free space; unreadability at start is `CastFailure.ItemUnreadable`.

FR32 (CAP-6): Provide transport controls (play/pause, stop, elapsed/duration when reported, seek where supported); show a "Casting to [TV]" session chip and a foreground-service notification with Stop; keep the cast alive through screen-off and app backgrounding; end on user Stop, renderer stop/error, TV disconnect, or force-stop; show "Cast stopped" with the reason and "Start again" when the file is still available; never restart silently.

#### Mirror (CAP-7)

FR33 (CAP-7): Provide a Mirror sheet that checks for the UniRem receiver first through one handshake (`hello` → `helloAck`) with contract major/minor negotiation; if no receiver, show setup steps and "Check again" and request no MediaProjection consent; if the version mismatches, show `MirrorFailure.VersionMismatch`.

FR34 (CAP-7): If the receiver is ready, explain "Your phone screen will be shared until you stop," then start capture consent from an Activity; denial returns to the sheet with "Screen capture wasn't allowed." / "Open settings" / "Not now"; never start capture hoping a receiver appears.

FR35 (CAP-7): During mirroring, show a "Mirroring to [TV]" session chip and a foreground-service notification with Stop; do not promise screen-off, audio, latency, or battery figures; if capture ends, show "Mirror stopped" and "Start again"; stop requires "Stop mirroring?" / "Stop" / "Keep mirroring."

FR36 (CAP-7): Ship the versioned receiver contract artifact before the Mirror slot is enabled; the contract is the single source for endpoint discovery, transport binding, message names, and payload schema; the envelope and version negotiation are fixed by AD-10.

#### Cross-cutting functional requirements

FR37 (all): One app-level `LanAccessGate` owns the local-network permission, the single process-wide `MulticastLock`, the isolation diagnostic, and the manual-IP path; `:tv` and `:cast` consult it before any attempt and map denial to `TvFailure.LanDenied` / `CastFailure.LanDenied`; neither module requests permissions or launches UI.

FR38 (all): Persist TV records, declared-surface observations, probe results, chip dismissals, and the debug overlay in Room; scalars and debug flags in DataStore; token/pin in Keystore; the verification matrix as a read-only asset; never store a resolved capability label for display.

FR39 (all): Provide Settings for haptics, reduced motion, permissions, diagnostics, and about; provide TV details with identity, four capability labels, SPKI fingerprint, IP, masked MAC, Tizen version, last verified, and "Forget TV"; keep error codes in diagnostics, not primary copy.

FR40 (all): Enforce local-LAN-only egress for keys, text, pointer, and voice-derived commands; no cloud fallback in the press path; every outbound destination passes the single `PrivatePeer` predicate; the wake broadcast is the only allowlisted exception.

FR41 (all): Provide a debug-only matrix harness that writes overlay rows; release builds read the asset matrix only; overlay rows never satisfy a release gate; a firmware change invalidates any stored snapshot.

### NonFunctional Requirements

NFR1: Platform floor — native Kotlin Android; minSdk 26; targetSdk 36; compileSdk 37; JDK 17; portrait-first phone surface 360–480dp; dark-only V1; no tablet, web, iOS, or desktop surface.

NFR2: Capability honesty — every remote feature is gated per model by the verification matrix; no blanket "universal" claim; H/J 2014–15 sets are labeled unsupported best-effort; no wake claim without a recorded pass for the model and network type; no support claim a physical test has not observed.

NFR3: Evidence-backed outcomes — socket-write success is never presented as a TV action; Submitted, Unconfirmed, and Observed are distinct; machine observation and user confirmation are distinguished; no green check before evidence.

NFR4: Pairing security — per-TV TOFU leaf SPKI pin plus token in Keystore-backed encrypted storage; the pin is validated before the token is sent; mismatch forces visible re-pair; no trust-all manager; no hostname-check suppression outside the pinned client; no silent WSS-to-WS downgrade; pairing material is backup-excluded and never logged.

NFR5: Privacy — no telemetry, analytics, crash-reporting, or remote-config SDK in V1; no audio recording, retention, or transmission; no text-payload logging; no full MAC, token, pin, SPKI, cast media ID, or URL logging; `RedactedLog` only.

NFR6: Local-only egress — keys, text, pointer, and voice-derived commands are local-LAN only; no cloud fallback in the press path; every destination passes one `PrivatePeer` predicate; the wake broadcast is the only allowlist exception.

NFR7: Reliability — visible reconnect with backoff; no silent queue or retry; the remote socket is UI-scoped and closes when the Remote surface leaves the foreground; cast survives screen-off during a session; mirror depends on Android's capture session and is not promised to survive screen-off; process death ends sessions and is reported, not restarted.

NFR8: Performance — key latency is measured in Spike A; 3s is the default unconfirmed timeout with per-capability override; D-pad repeat is 500ms/80ms; cast start, `HEAD`, and byte `Range` are validated in Spike C; mirror latency and battery are recorded in Spike M; loading is text, not a spinner.

NFR9: Accessibility — WCAG 2.2 AA minimum, AAA for primary body text; 48dp minimum targets; 56dp direction keys; 70dp OK; 200% font scale; TalkBack names, roles, and live regions; reduced motion; RTL; 1.3× string expansion; color is never the sole carrier of state.

NFR10: Visual — Graphite Mint dark palette with the measured contrast table; Roboto plus one monospace face; 4dp spacing; no shadows, gradients, blur, or glassmorphism; custom 1.5dp line icons; dark-only, no dynamic color.

NFR11: Architecture — four Gradle modules (`:app`, `:core`, `:tv`, `:cast`) with the exact acyclic graph; `:core` is JVM-only; protocol types stay internal; public API allowlist; one writer per state object; no `Manager`, `Helper`, or `Util` except `RedactedLog`.

NFR12: Testability — `:core` JVM tests with no Android dependency; pure `:tv` and `:cast` logic is JVM-tested; Keystore, sockets, and MediaProjection are instrumentation-only; no test asserts a socket write as success; no test depends on a live TV.

NFR13: Build and CI — Gradle 9.5.0, AGP 9.3.1, JDK 17, Kotlin 2.4.21, Compose compiler 2.4.21, KSP 2.3.12, Compose BOM 2026.09.00, Hilt 2.60.1, Room 2.8.5, OkHttp 5.5.1, jUPnP 3.0.5, Ktor CIO 3.6.0; every change runs the Gradle build, lint, and `:core` JVM tests; instrumentation and physical matrix runs are manual.

NFR14: Matrix data — a versioned in-repo artifact keyed by byte-exact model and firmware strings, a closed `NetworkType`, and `CapabilityKey`; unknown firmware matches only a literal `*unknown*` row; every row carries `matrixVersion`; a firmware change invalidates stored snapshots; no reader normalizes.

NFR15: Distribution — signed AAB to Play internal testing, then production; signing key held by the project owner; the Tizen receiver stays a dev-mode sideload for V1; no store listing until CAP-7 latency and battery figures are recorded.

NFR16: Permissions and foreground services — `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, `POST_NOTIFICATIONS`, `RECORD_AUDIO`, `VIBRATE` (normal install-time, haptics only), the local-network permission for API 37, and MediaProjection consent from an Activity; no storage permissions; Photo Picker only; FGS types cast `connectedDevice`, mirror `mediaProjection`, voice `microphone`; only user-started sessions.

NFR17: Localization and RTL — externalized strings, no concatenation, 1.3× expansion, sentence case; text and rows mirror in RTL; D-pad arrows, volume/mute, and media transport icons do not mirror.

NFR18: Documentation and contract — `docs/receiver-contract.md` must exist before the Mirror slot is enabled; the app ID/package base `com.unirem` is a seed and must be confirmed before the first signed build; the verification matrix and voice command map are versioned with the app.

### Additional Requirements

**Scaffold and modules**

- Greenfield Gradle multi-module scaffold: `settings.gradle.kts`, `gradle/libs.versions.toml`, `app/`, `core/`, `tv/`, `cast/`, and `tizen-receiver/` (a separate web app, not a Gradle module). No starter template is specified; Epic 1 Story 1 is the scaffold.
- Module graph exactly `:app → :tv, :cast, :core`; `:tv → :core`; `:cast → :core`; `:core` has no Android dependency; no other project dependencies exist.
- Public surfaces are limited to the seams `TvControl` (with `RemoteCommand`, `TextOp`, `PointerOp`), `RendererGateway`, `CastEngine`; the `:core` shared values (`TvId`, `SessionId`, `MediaId`, `StableIdentity`, `CommandOutcome`, `Evidence`, `CapabilityKey`, `CapabilityScope`, `CapabilityLabel`, `CapabilityInput`, `PrivatePeer`, `LanAccess`, `SessionState`, `NetworkType`, `TvFailure`, `CastFailure`, `MirrorFailure`, `RedactedLog`); the consumer-owned `PairingStore` port; and the `CommandTracker` / `ConnectionHealth` state owners. Everything else is `internal`, public declarations live in `api`, and a review check rejects anything else.
- Naming and visibility conventions: adapters suffix `Gateway` or `Control`; state owners `Tracker`, `Session`, or `Health`; pure functions `Resolver` or `Mapper`; `Manager`, `Helper`, and `Util` are banned except `RedactedLog`; `:app` never imports `okhttp3`, `org.jupnp`, `KEY_*`, or a protocol frame type.

**Command truth and owned state**

- AD-2: one `CommandTracker` per target TV, owned by `:tv`, exposing `StateFlow<CommandOutcome>`; adapters emit `Evidence` (`TvEvent`, `StateProbe`, `UserConfirmed`) and never a verdict; the tracker holds a monotonic gesture generation bound to the socket instance, ignores older-generation evidence, and applies an injected timeout (3s default, per-capability override); tracker lifetime is exactly the socket session; `:app` calls `submit()` exactly once per user-expressed command; repeat frames and synthesized press/release never submit; `TvControl`'s operation set is fixed; text carries an explicit IME lifecycle; pointer uses one absolute TV panel coordinate space, clamped and converted only inside `:tv`; wake is a separate operation; exactly one repeat engine exists in `:tv`.
- AD-3: four owned values — command outcome, connection health, session lifetime, and TV power state — with one writer each; `ConnectionHealth` in `:tv/channel` owns every 8001/8002 observation and the power state `On`, `Asleep`, or `Unknown`; wake requests a probe and consumes its `StateProbe`; probe and thin connections are tagged and never counted by health, backoff, or `clientConnect`; no shared timer or state object; `:app` combines the four read-only with fixed precedence: identity changed, then unauthorized/pairing required, then offline, then reconnecting, then asleep, then awake.
- AD-4: `CapabilityResolver` in `:core` evaluates per `CapabilityKey` (capability plus one `CapabilityScope` from `Device`, `NativeField`, `AppGrid`, `PointerBrowser`, `PointerAppGrid`, `FormatJpeg`, `FormatMp3`, `FormatH264Aac`, `FormatHevc`, `Subtitles`, `Seek`): declared device-level surface absent → `Not supported` for every scope; probe hard-fail → `Not supported` for this key; matrix pass → `Works`; matrix partial/ambiguous or H/J generation → `Best-effort`; no row and no probe → `Not tested`. Declared surfaces are the UPnP service/action list and `TokenAuthSupport`; IME events are `TvEvent` evidence, not declarations. User confirmation changes command provenance only. `:app` has exactly one capability display owner; every surface reads the same flow, recomputed from the input tuple, never from a stored label.

**Matrix and capability data**

- AD-5: the verification matrix is a versioned in-repo artifact keyed by byte-exact model and firmware strings, a closed `NetworkType` (`Wifi`, `Ethernet`, `Unknown`), and `CapabilityKey`; unknown firmware matches only a literal `*unknown*` row; every row carries `matrixVersion`; a firmware change invalidates any stored snapshot; exactly one `:app` component determines `NetworkType` from the observed path; `:core` defines the schema and resolution; `:app` loads the asset and passes rows in; a debug harness writes a debug-only Room overlay table; the resolver takes asset rows plus overlay rows in debug builds with the exact-key overlay winning; release builds read the asset only; overlay rows never satisfy a release gate; readers never normalize.

**Session ownership and lifecycle**

- AD-6: `CastSessionOwner` and `MirrorSessionOwner` in `:app` are the sole writers of `SessionState`, each with exactly one `end(reason)`; `:cast`, the renderer, the media server, and the foreground service publish facts only; a renderer stop is a fact the owner maps to an end reason from `UserStop`, `RendererError`, `TvDisconnected`, `ProcessDeath`, or `OwnerCancelled`; the media server's idle grace window is at least the owner's liveness window and never self-terminates while an unended session claims it; at most one pinned session exists at a time; the remote socket is UI-scoped and closes when the Remote surface leaves the foreground; wake and Wi-Fi locks are owned exclusively by the session owner, acquired after start, and released only in `end()`; the foreground service, notification, and Activity consent hand-off never acquire locks; MediaProjection consent is requested by an Activity and handed to the owner; if `POST_NOTIFICATIONS` is denied, the session is foreground-only and ends on background with the fixed copy; process death ends the session and the next launch reports it ended without restarting it.

**Trust boundary and identity**

- AD-7: `StableIdentity` has fixed precedence — the device ID the TV reports first, the MAC second, byte-exact as reported, with one normalization rule applied identically everywhere — and `TvId` is the hash of exactly that; a durable TV record and Keystore alias are created only against a resolved identity; a manual-IP target with no resolvable identity is session-only and says so; identity is immutable per TV record and never derived from the current connectivity path; a client scoped to one TV validates the leaf SPKI pin before attaching the token; a mismatch enters a visible re-pair flow; no trust-all `X509TrustManager`, no hostname-check suppression outside the pinned client, and no fallback from WSS to WS; pairing material is Keystore-backed and backup-excluded; Room stores the TV record and Keystore alias, never the token or pin; `:tv` is the only reader of the alias; the wake MAC is a separate stored field with its own provenance and network type; token, pin, and SPKI are never logged.

**Network egress**

- AD-8: `:core` owns exactly one `PrivatePeer` predicate over a typed destination and purpose; it allows IPv4 RFC1918 (`10/8`, `172.16/12`, `192.168/16`), IPv4 link-local (`169.254/16`), and IPv6 link-local (`fe80::/10`); global IPv6 and IPv6 ULA are deferred; the wake purpose additionally allows the subnet-directed broadcast computed from the active `LinkProperties` and `255.255.255.255`, on UDP ports 9 and 7 only, from a socket bound to the private interface; the OkHttp interceptor, jUPnP transport configuration, both discovery filters, the media server's inbound peer check, and the wake sender call this one implementation; no second predicate exists and no send path bypasses it; the network-security-config permits cleartext and says why; the wake allowlist lives inside the predicate.

**Cast serving**

- AD-9: `:cast` mints `MediaId`, owns the URL path shape, the id-to-bytes mapping, and the single DIDL-Lite and `protocolInfo` assembly; `:app` hands the picked `Uri` plus metadata and never a URL; item bytes are guaranteed readable for the session by one mechanism — a persistable read grant where the picker offers one, re-validated at session start, with a session-cache copy as fallback under one byte limit checked against free space; unreadability at start is `CastFailure.ItemUnreadable`; the TV receives an `http(s)` URL with the `MediaId` that also appears in diagnostics; the server supports `HEAD` and byte `Range`, binds to the interface address through which the TV was reached, rejects non-private peers, serves only the session's selected item, and stops at the owner's `end()`; no `content://` URI crosses the boundary; if the route changes, the session restarts serving on the new address or ends visibly; Ktor CIO with `ktor-server-partial-content` is the pinned engine; Spike C validates start, `HEAD`, and byte `Range` on a real device before the Cast slot is enabled; any replacement must satisfy this AD with no second server present.

**Mirroring contract**

- AD-10: the receiver contract is a versioned in-repo artifact owned with the receiver and is the single source for endpoint discovery, transport binding, message names, and payload schema; the spine fixes the envelope: one handshake, `hello` then `helloAck`; the Mirror sheet's "Check again" retries this handshake and there is no separate presence ping; `hello` carries `contractMajor`, `appVersion`, and the viewer-auth request; `helloAck` carries `contractMajor`, `contractMinor`, `receiverVersion`, receiver-issued viewer and session tokens, `codecs`, `maxResolution`, and the signalling handle; major must match and the app minor must not exceed the receiver minor, otherwise `MirrorFailure.VersionMismatch`; the failure taxonomy is `ReceiverAbsent`, `VersionMismatch`, `ViewerRejected`, `Busy`, `SignallingFailed`, `MediaFailed`; transport is a local WebSocket to the receiver control endpoint named by the contract; cleartext is allowed only under AD-8's private-peer rule and no cloud relay exists; viewer auth is receiver-issued, per-session, short-lived, never logged, and requires no account; no `helloAck` means no mirror surface and no MediaProjection consent; the receiver is a separate Tizen codebase and not a Gradle module.

**LAN access gate**

- AD-11: one app-level `LanAccessGate` owns the local-network permission, the single process-wide `MulticastLock`, the isolation diagnostic, and the manual-IP path; it publishes `LanAccess` (`Granted`, `Denied`, `NotDeclared`) from `:core`; `:tv` and `:cast` consult it before any attempt and map denial to exactly one failure each — `TvFailure.LanDenied` and `CastFailure.LanDenied` — which `:app` maps to the single gate explainer; neither module requests permissions or launches UI; jUPnP's own multicast lock is disabled and both discovery paths use the gate's scoped handle; a denied gate blocks scan and manual IP identically; notifications and microphone permissions are outside this gate.

**Voice**

- AD-12: UniRem never records, stores, or transmits audio itself; it uses the platform speech recognizer and consumes only the final text or command; the permission explainer says "Speech is handled by your phone" and never claims on-device recognition unless the platform guarantees it; there is no background capture and no wake word; raw audio is never sent to the TV and raw-audio injection is out of scope; one versioned in-app `VoiceCommandMap` maps recognized phrases to the same `RemoteCommand` type used by the D-pad and text paths; a recognized phrase becomes exactly one user-expressed command and therefore exactly one `submit()`.

**Persistence, logging, and config**

- Persistence: Room stores TV records, declared-surface observations, probe results, chip dismissals, and the debug harness overlay; DataStore owns scalars and debug flags; Keystore owns token and pin; matrix data is a read-only asset; inputs only — no resolved label is stored for display.
- Logging: `RedactedLog` only; never log token, pin, SPKI, text payloads, cast media IDs or URLs, or a full MAC; log a `TvId` prefix; no `println` and no direct `Log` call outside the sink.
- Config: BuildConfig carries build-time flags only and no secrets; runtime flags live in DataStore; matrix data and the voice command map are versioned with the app.
- IDs and time: `Instant` in UTC at every boundary and in storage; local formatting only in `:app`; firmware is the byte-exact string the TV reports and is never normalized; `TvId` is the hash of `StableIdentity`; `SessionId` is UUIDv4; `MediaId` is 128-bit random base64url minted by `:cast`; gesture generation is a monotonic `Long` per socket session.
- Errors: one sealed failure type per module (`TvFailure`, `CastFailure`, `MirrorFailure`); no exception crosses a module boundary; user copy lives in `:app`.

**Build, CI, and operations**

- Testing: `:core` is JVM-only with no Android test dependency; pure logic in `:tv` and `:cast` is JVM-tested; Keystore, sockets, and MediaProjection are instrumentation-only; no test asserts a socket write as success.
- CI: every change runs the Gradle build, lint, and `:core` JVM tests; instrumentation and physical matrix runs are manual on owned TVs; results are committed as matrix rows.
- Distribution: signed AAB to Play internal testing, then production; signing key held by the project owner; the Tizen receiver stays a dev-mode sideload for V1.
- Debug environment: `debug` includes the matrix harness and overlay, verbose redacted logs, and strict-mode checks; `release` reads the asset matrix only and has no harness; there is no backend and no staging environment.
- Permissions and FGS: `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, `POST_NOTIFICATIONS`, `RECORD_AUDIO` for the Voice sheet, `VIBRATE` (normal install-time, haptics only), the local-network permission for API 37, and MediaProjection consent from an Activity; no storage permissions; the Photo Picker is the only picker; FGS types cast `connectedDevice`, mirror `mediaProjection`, voice `microphone`; only for user-started sessions.
- Multicast: `LanAccessGate` owns the single process-wide `MulticastLock`; jUPnP's own lock is disabled.
- Secrets: Keystore only; no secret in BuildConfig, resources, or logs; pairing material is backup-excluded.
- Matrix harness: debug-only Room overlay; overlay rows never satisfy a release gate; release reads the asset only.
- Receiver contract: `docs/receiver-contract.md` must exist before the Mirror slot is enabled; its endpoint discovery and exact message names are receiver-owned; the envelope in AD-10 binds both sides.
- App identity: the application ID and package base `com.unirem` are seed values; confirm before the first signed build because changing them after release creates a new app identity.
- Open questions carried from the spine: per-capability timeout values are set by Spike A; the WebRTC client artifact and codec set are selected by Spike M; global IPv6 and IPv6 ULA are deferred; a Ktor CIO replacement must satisfy AD-9 and AD-8 with no second server; the receiver contract artifact must exist before the Mirror slot is enabled.

### UX Design Requirements

#### Design tokens and visual system

UX-DR1: Implement the Graphite Mint dark theme: `surface-base #151719`, `surface-raised #1E2124`, `surface-key #2A2E32`, `surface-key-pressed #343A3E`; ink primary/secondary/disabled/on-accent; accent/pressed/wash; ring, status, banner, focus, and scrim tokens; dark-only, no dynamic color, no light theme.

UX-DR2: Implement the typography scale — display 32sp/300, title 24sp/400, headline 20sp/400, body 16sp/400, body-small 14sp/400, label 12sp/500/+0.08em, micro 11sp/500/+0.12em, button 15sp/500, state 13sp/500, key 15sp/400, code 12sp/400 monospace; `sp` units; no weight below 400 below 18sp; uppercase only for `micro`; tabular numerals in the state line, timestamps, and progress; no critical label truncates.

UX-DR3: Implement the 4dp spacing scale, 20dp screen gutter, 24dp section spacing, rounded tokens (8/14/18/24/full), and 480dp maximum content width; portrait 360–480dp; on a short screen, collapse the quiet space first, then reduce the ring to 208dp; never reduce a touch target below 48dp.

UX-DR4: Implement tonal depth: `surface-base → surface-raised → surface-key` plus 1dp hairlines; no drop shadows, gradients, blur, or glassmorphism; the ring glow and 2dp health halo are the only glows; pressed keys change tone and scale to 0.98 for 80ms; sheets use a 64% scrim; reduced motion removes the breath and pressed scale.

UX-DR5: Implement the custom 24dp/1.5dp line icon set: twelve core glyphs (TV, power, speaker, play/pause, home, back, apps grid, search, keyboard, microphone, pointer, cast) plus Mirror and Wake and the simple utility marks; no Material icons, emoji, unicode arrows, or text-as-icon; sizes 18dp in function keys, 20dp in utility keys and launcher wells, 24dp in the topbar; the OK key uses the word "OK."

#### Core components

UX-DR6: Topbar — 56dp, `surface-base`, 1dp bottom hairline, 20dp side padding; on Remote, TV switcher left and 48dp settings right; on Discovery/Add TV, Pairing, TV details, and Settings, back and static title left, right empty; no command state in the topbar.

UX-DR7: TV switcher — text button with an 8dp health dot, TV name in `body`, 12dp mint caret, 8dp gaps, 48dp minimum height; truncates a long name but keeps the dot and caret; opens the switcher sheet; shows every stored TV with health, model, and durable capability label; explicit selection; "Add a TV" last; never auto-targets the last-used TV on cold start when multiple exist.

UX-DR8: Health dot — 8dp circle with a 2dp halo at 18%; connected mint, reconnecting amber, offline salmon; accessibility label names the TV and health; never indicates a command outcome; no pulse except during reconnect, removed under reduced motion.

UX-DR9: One-time chip — 48dp minimum, raised surface, 1dp hairline, 14dp radius, 14dp padding, 14sp secondary text, 18dp info glyph, 48dp close target; maximum two lines; one at a time; names the limit and what still works; dismissed once per TV/capability; returns only on re-pair, firmware change, or probe-result change.

UX-DR10: State ring — 236dp, 2dp stroke, no fill, not tappable or focusable; idle, submitted (pale mint), observed (mint), unconfirmed (amber), offline (salmon); one 900ms breath per press; observed 24dp glow held 4s; unconfirmed glow held until resolved; offline glow held until reconnect; no checkmark, percentage, sweep, or segmented arc; reduced motion changes color instantly.

UX-DR11: D-pad — 3×3 grid, 64/76/64 columns, 60/70/60 rows, 4dp gap; direction keys 60×56dp, 14dp radius, `surface-key`, 1dp hairline, 15sp chevron; OK 70dp mint circle with `ink-on-accent` 15sp/500; pressed direction `surface-key-pressed`, pressed OK `accent-pressed`; disabled keys use `ink-disabled` and 45% opacity; effective touch targets at least 48dp; no diagonals, no swipe surface.

UX-DR12: Utility row — centered 48dp row; volume rocker 180×48dp with three 60dp segments and hairline dividers; play/pause and power 48dp circles with 20dp line icons and 14dp gaps; power off only, visually separate, never a filled red block or a ring color; no mint fill.

UX-DR13: Function keys — 52dp high, four equal columns, 8dp gaps, 14dp radius, `surface-key`, 1dp hairline, 18dp icon above a 15sp label; whole key is the target; no mint fill, no uppercase, no pills.

UX-DR14: Launcher — 84dp shelf, `surface-raised`, 1dp top hairline, 24dp top corners; six fixed slots in order Text, Voice, Pad, Cast, Mirror, Wake; 52×60dp slots, 44dp circular wells, 11sp labels; disabled slots keep position, icon, and label with a dashed hairline, `ink-disabled`, and a small lock mark; always visible, including with no TV; 3×2 at <360dp or 200% font scale; no reordering, hiding, or overflow.

UX-DR15: Bottom sheet — `surface-raised`, 24dp top corners, 20dp side padding, 12dp top padding, 24dp bottom padding, 38×4dp grabber, 64% scrim, 1dp top hairline, maximum 80% height; one at a time; drag-down, scrim tap, or back dismisses; focus moves in and returns to the trigger; keyboard insets keep Send above the IME; no nested sheets.

UX-DR16: Text field — 52dp minimum, `surface-key`, 1dp hairline, 14dp radius, 20dp horizontal padding, `body` text, 2dp mint caret and focus ring; multiline up to three lines before the sheet scrolls; no label inside; error copy below in `body-small`; no live mirror of the TV.

UX-DR17: Mic key — 72dp mint circle, `ink-on-accent`, 28dp microphone line icon; listening shows a 2dp `accent` halo at 40% and "Listening…"; locked shows a full-strength halo, a small lock mark, and "Locked"; hold/release/slide-up-lock; not a play/stop toggle.

UX-DR18: Session chip — 48dp-high single-line pill, `surface-key`, 1dp hairline, `rounded.full`, 14dp horizontal padding, 14sp text; names the session and TV ("Casting to [TV]" / "Mirroring to [TV]"); sits above the launcher or at the bottom of other surfaces; returns to the session; is not a command state and does not use ring colors as a badge.

UX-DR19: Banner — oxblood `#2B1A18`, 1dp `#5A2A25` border, 14dp radius, 16dp padding, 16sp `banner-ink`, one recovery action in `banner-action` plus an optional "Not now"; only for blocking or actionable conditions (offline, reconnecting, identity changed, cast stopped, pairing required); pushes content; maximum two lines plus one action row; no spinner or command state.

UX-DR20: State line — 24dp minimum, centered, 13sp/500, 6dp lamp plus a short label ("Idle", "Sent", "Waiting for TV…", "TV confirmed", "You confirmed", "Offline"); tabular numerals; expands to 48dp with a 48dp "It worked" action in the unconfirmed state; polite live region; never truncates the state word.

UX-DR21: Pairing card — raised card, 20dp padding, 18dp radius, 1dp hairline; name in `headline`; model, Tizen version, masked MAC, and "same Wi-Fi as this phone" in `body-small`; first-time note in a `surface-key` inset with a 3dp mint left rule; primary "This is my TV — connect"; secondary "That's not my TV" always visible; no full MAC or token; SPKI fingerprint under Details.

UX-DR22: TV card — raised card, 16dp padding, 18dp radius, 1dp hairline; name in `headline`; model/Tizen/status in `body-small`; one durable capability label as a text plate ("Full control — confirms keys" or "Best-effort — can't confirm commands"); actions Open remote and TV details; Forget lives in details; the health dot is health, not capability.

UX-DR23: Buttons — primary 52dp mint fill with `ink-on-accent`; secondary 48dp transparent with a 1dp hairline; destructive 48dp transparent with a 1dp `ring-offline` border and text; one primary per sheet or card; no icons unless the icon is the only label; no loading spinners; destructive actions are never filled red blocks.

UX-DR24: Permission explainer — bottom sheet with a 24sp title and 16sp body; one sentence of why, what the app will do, and what happens if the user says no; Continue triggers the system prompt; "Not now" leaves the feature unstarted; denied state offers "Open settings" and a plain explanation; no permission wall on first launch and no repeated system prompts.

UX-DR25: Empty state — `display` title "No TVs yet.", 16sp body, a quiet `code` diagnostic readout (network, scan result, diagnostics), and one primary action "Add your first TV"; launcher visible and disabled; not a marketing page.

#### Information architecture and behavior

UX-DR26: Implement the 13-surface IA and navigation rules — Remote, TV switcher, Add TV/Discovery, Pairing, TV details, Text sheet, Voice sheet, Cast sheet, Cast session, Mirror sheet, Mirror session, Wake sheet, and Settings; one sheet at a time; the TV switcher is a sheet, not a tab; explicit target selection; switching TVs closes Text, Voice, Cast picker, Mirror setup, and Wake sheets but does not stop pinned sessions; every surface has a way back to Remote; every blocking state has one next action.

UX-DR27: Implement the canonical terms and copy contract — paired vs connected vs submitted/unconfirmed/observed/user-confirmed/best-effort; UI labels; the error formula fact + next action + limit; sentence case; the ellipsis character `…` for in-progress states; no exclamation marks, emoji, or all-caps sentences; no "seamless," "magic," "universal," "works with any Samsung TV," or "just"; never call submitted "done" or unconfirmed "failed."

UX-DR28: Implement the state-ring rules — every press starts at Submitted; one 900ms breath; 3s submitted-to-unconfirmed (provisional, validated in Spike A); observed holds 4s then fades to idle; late evidence for older presses is ignored; keys stay enabled while the socket is open but silent, and disabled when it is closed; machine evidence produces "TV confirmed," user confirmation produces "You confirmed"; no checkmark, sweep, or segmented arc.

UX-DR29: Implement the command-state variants — an unsupported key remains usable with a one-time chip; a late observation for an older press is ignored; no "TV confirmed" without machine evidence; power off may remain amber on models without an off-state event; the ring belongs to the last press only.

UX-DR30: Implement the Text sheet behavior — title "Text to [TV]"; helper "Typing goes to the TV's focused field."; explicit Send via button or IME action; no live keystrokes or per-character writes; after Send "Sent. Check the TV." with "It's there" and "Try again"; never "Text sent successfully"; the draft stays for retry until close; passwords are not logged, retained, or suggested; the app-grid limit is stated before the attempt.

UX-DR31: Implement the Voice sheet behavior — mic key hold/release/slide-lock; visible transcript; non-destructive commands execute on release; power off requires a tap on a destructive action; dictation shows a draft and "Send to TV"; the user confirms a native field is open; mic denial offers "Open settings"; no raw audio, no wake word, no always-listening state.

UX-DR32: Implement the Cast sheet and session behavior — one item per session; active TV preselected and named; renderer check before play; pre-play format/subtitle warnings; play/pause, stop, elapsed/duration when reported, seek where supported; session chip and foreground notification with Stop; cast survives screen-off; end on user Stop, renderer stop/error, TV disconnect, or force-stop; "Cast stopped" with the reason and "Start again"; no silent restart; no `content://`; no unverified codec, subtitle, seek, or long-file claims.

UX-DR33: Implement the Mirror sheet and session behavior — receiver check first; no MediaProjection consent before readiness; consent denial returns to the sheet with "Screen capture wasn't allowed." / "Open settings" / "Not now"; session chip and foreground notification with Stop; stop confirmation "Stop mirroring?" / "Stop" / "Keep mirroring"; no screen-off, audio, latency, or battery promises; capture-ended "Mirror stopped" and "Start again"; no silent install.

UX-DR34: Implement the Wake sheet behavior — disabled slot without a matrix pass; with a pass, the checklist (plugged in, same network, "Power On with Mobile," MAC saved); "Waking. This can take up to a minute."; success only when the TV answers on 8001/8002; timeout "The TV didn't wake. Try the TV's power button." with "Try again" / "Close"; no retry loop or false success; power off remains separate.

UX-DR35: Implement the Pairing and TV details behavior — TOFU card; Allow prompt with the always-visible no-remote path and button diagram; timeout and denial copy; pin validation before token; identity-changed banner and re-pair; four capability labels (Works, Not tested, Not supported, Best-effort); SPKI fingerprint, IP, masked MAC, Tizen version, last verified; Forget confirmation; the durable label is the source of truth.

UX-DR36: Implement the connectivity and identity states — cold start with no TVs, scanning, scan empty, one TV, multiple TVs, TV found unpaired, pairing prompt, pairing timeout, pairing denied, local-network denied, connected/idle, reconnecting, offline, identity changed, pairing required; exact copy, actions, and rules per the `EXPERIENCE.md` state table.

UX-DR37: Implement the capability and permission states — text no field, text app-drawn grid, voice permission denied, voice listening, voice review, Pad not probed, Pad unsupported, cast renderer missing, cast warning, mirror receiver missing, mirror consent denied, notification permission denied, wake no pass, wake in progress, wake timeout, background/lock, restored app; exact copy and actions per the `EXPERIENCE.md` state table.

UX-DR38: Implement the interaction primitives — tap is the default; press-and-hold only for D-pad repeat after 500ms, Back long-press after 1.2s where supported, and the Voice mic key; slide-up-to-lock only on the mic key; no swipe on the D-pad, no double-tap, shake, or multi-finger gestures; sheets dismiss on drag-down, scrim tap, or back; system IME; haptics 8ms key, two ticks observed, 12ms unconfirmed, 20ms offline; limited motion; no sound; text loading, not spinners; scrolling only inside sheets and settings; platform back; pinned sessions.

UX-DR39: Implement the accessibility floor — WCAG 2.2 AA minimum and AAA for primary body text; measured contrast tokens; 48dp minimum targets, 56dp direction keys, 70dp OK; color never alone; TalkBack names and roles; the state line is a polite live region and the banner is assertive only for offline/identity changed; focus order topbar → chip → utility row → function keys → D-pad → state line → launcher; 200% font scale; reduced motion; haptics supplementary; transcript exposed as text; RTL mirroring with non-mirrored physical glyphs; externalized strings and 1.3× expansion; no accessibility-only mode.

UX-DR40: Implement responsive and platform behavior — portrait 360–480dp; column centers at 480dp max; landscape uses the same column; on a short viewport collapse the quiet space, then reduce the ring to 208dp, never targets below 48dp; edge-to-edge transparent status/navigation bars; respect cutouts and gesture insets; launcher bottom padding includes the safe area; no immersive mode; no tablet layout.

UX-DR41: Implement motion and haptics — ring breath 900ms once per press; observed hold 4s; sheet transition 220ms slide/fade; pressed-key scale 0.98 for 80ms; reduced motion removes the breath and scale, changes color instantly, and reduces the sheet to a fade; haptics 8ms key, two ticks observed, 12ms unconfirmed, 20ms offline; the Settings toggle and system haptic setting are both honored; no sound, autoplay, parallax, or looping animation.

UX-DR42: Implement the journeys as acceptance scenarios — Flow 1 ("The remote is gone"): no TVs → local-network permission → discovery → pairing without a physical remote or the documented fallback → volume/play with Sent, TV confirmed, or You confirmed; Flow 2 ("The search box"): two TVs → explicit choice → text into a native field → app-grid limit → cast → switch TVs without stopping the cast → best-effort label; plus the supporting voice, mirror, wake, and Pad flows.

UX-DR43: Enforce the anti-patterns — no dashboard, tabs, FAB, carousel, horizontal scrolling, or persistent bottom navigation beyond the six-slot launcher; no green check, confetti, or "Success!" before confirmation; no spinners for pairing, discovery, or wake; no "universal," "seamless," "magic," or "just"; no silent queue, silent retry, or silent fallback to the last TV; no mirroring without a receiver; no raw audio; no permission wall on first launch; no autoplay, playlist, or queue; no hiding a gated capability without explanation; never swap the ring and health-dot roles.

### FR Coverage Map

FR1: Epic 1 - SSDP discovery and `/api/v2/` identity read.
FR2: Epic 1 - Manual-IP entry; session-only target when no stable identity.
FR3: Epic 1 - Pairing card and explicit "this is my TV" choice.
FR4: Epic 1 - Allow/Deny pairing, token and leaf SPKI pin persistence.
FR5: Epic 1 - Pin validation on every connect; identity-changed re-pair.
FR6: Epic 2 - Multiple stored TVs and explicit target selection.
FR7: Epic 2 - Target/session rule; 7a target/restart in Epic 2, 7b sheet-close in Epics 3-6, 7c session pinning verified in Epics 5-6.
FR8: Epic 1 - Reconnect/backoff, pairing-required state, offline keys.
FR9: Epic 1 - MVP key set.
FR10: Epic 1 - Direction press-and-hold repeat.
FR11: Epic 1 - Submitted/Unconfirmed/Observed command truth; target-change clause in Epic 2.
FR12: Epic 1 - Unsupported-key chip and durable limitation record; full details UI in Epic 2.
FR13: Epic 1 - Disabled keys while offline.
FR14: Epic 3 - Text sheet and memory-only draft.
FR15: Epic 3 - IME lifecycle, Base64 send, sent/check copy.
FR16: Epic 3 - Focus honesty and per-surface probe.
FR17: Epic 3 - Password-field handling.
FR18: Epic 3 - Pad probe gate and unsupported state.
FR19: Epic 3 - Pad drag/tap and absolute coordinate space.
FR20: Epic 4 - Voice sheet and hold-to-talk.
FR21: Epic 4 - Platform speech recognizer; no audio recording.
FR22: Epic 4 - Versioned `VoiceCommandMap` and one-submit rule.
FR23: Epic 4 - Dictation review and "open a field" guidance.
FR24: Epic 1 - Power off as a distinct key, with the wake limitation warned.
FR25: Epic 2 - Wake slot only with a recorded matrix pass.
FR26: Epic 2 - Wake checklist, magic packet, 8001/8002 answer.
FR27: Epic 2 - Wi-Fi MAC capture and provenance; Epic 1 captures the inert data.
FR28: Epic 5 - One-item Cast picker.
FR29: Epic 5 - Renderer check and pre-play warnings.
FR30: Epic 5 - `:cast` media server and TV-fetches-URL path.
FR31: Epic 5 - Session readability guarantee.
FR32: Epic 5 - Transport controls, chip, FGS; notification-denied path qualified.
FR33: Epic 6 - Receiver handshake and version negotiation.
FR34: Epic 6 - MediaProjection consent only after receiver readiness.
FR35: Epic 6 - Mirror session, stop confirmation, ended states.
FR36: Epic 6 - Versioned receiver contract; Epic 6 also owns the receiver build and sideload.
FR37: Epic 1 - App-level `LanAccessGate`.
FR38: Epic 1 - Room/DataStore/Keystore persistence.
FR39: Epic 2 - Settings and full TV details; Epic 1 ships the identity/fingerprint/Forget trust subset.
FR40: Epic 1 - Local-LAN-only egress; wake clause in Epic 2; full verification in Epic 6.
FR41: Epic 1 - Debug matrix harness; release-asset-only half in Epic 7.

### Cross-cutting coverage notes

- NFR1-NFR18 and UX-DR1-UX-DR43 are acceptance criteria on the epics above, not a separate foundation epic.
- NFR9, NFR17, and UX-DR39 are per-epic definitions of done and are audited in Epic 7.
- NFR14/AD-5 is seeded in Epic 1 and released through the signed artifact in Epic 7.
- NFR15 is Epic 7. NFR16 is feature-owned: LAN gate in Epic 1, permission explainers in Epic 2, microphone in Epic 4, cast FGS/notifications in Epic 5, MediaProjection in Epic 6.
- UX-DR1-UX-DR25 are the Epic 1 design system; UX-DR26-UX-DR38 are owned by the surface epic that introduces them; UX-DR39-UX-DR43 are per-epic and audited in Epic 7.

### Epic 1 coverage exceptions

These are explicit, reviewed exceptions, not silent gaps.

- **UX-DR38 Back long-press:** not enabled in Epic 1. The protocol reference has no canonical per-model long-Back support signal and the matrix has no `Keys/LongBack` key, so Back is tap-only; Story 1.13 records the condition for enabling it later.
- **UX-DR14 disabled launcher explanation:** the launcher remains visible and disabled. Story 1.14 makes the slots focusable and announces "Not available in this build"; the explanation sheet is deferred to launcher enablement.
- **FR12 per-key wording:** Epic 1 uses a model-level one-time chip because the matrix exposes `Keys/Device`, not per-key support. The per-key `[key]` wording is deferred until per-key evidence exists; Epic 1 never claims the TV does not report a specific key.
- **NFR9 compact D-pad:** on the optional 208dp short-viewport ring, directions are 56×52dp rather than the standard 60×56dp; every target remains at least 48dp. The standard 236dp ring keeps 60×56dp.
- **360×640dp at 200% with a banner:** a stress finding, not a silent clip. 360×800dp is the release gate; Story 1.14 records a named design follow-up if the short case cannot fit.

### Epic 1 traceability notes

These notes close traceability questions without changing the approved story scope.

- **UX-DR23 button ownership:** the button component is part of the Epic 1 shell/design system. Stories 1.4–1.6 exercise the primary, secondary, and destructive variants; Story 1.14 audits their targets and contrast. No separate story is added for the component.
- **FR38 persistence scope:** Epic 1 creates the Room and Keystore stores it actually needs: the TV record and pairing material in Story 1.5, deletion in Story 1.6, the debug-only overlay in Story 1.8, and chip dismissals in Story 1.15. Declared-surface observations and probe results arrive with the text/pointer probes in Epic 3; no DataStore scalar or runtime debug flag exists in Epic 1, so DataStore is not instantiated. The first story that needs those stores creates them.

### Story-breakdown scope

Detailed stories are complete for Epic 1. Epics 2–7 remain at epic level with FR assignments and implementation notes; their stories are intentionally generated in later passes. The 41-FR map above assigns every FR exactly once at epic level.

## Epic List

**Total:** 7 epics. **FR coverage:** 41 of 41 FRs assigned exactly once.

### Epic 1: Pair and press

The user can find a Samsung TV on the local network, complete the TV's Allow prompt, trust the connection, and use a working one-TV remote — with every key and power-off action honest about what the phone observed.

**FRs covered:** FR1-FR5, FR8-FR13, FR24, FR37-FR38, FR40-FR41.

**Implementation notes:**
- **Spike A gate first.** No scaffold or foundation work starts before the Spike A report. Exit evidence: at least three model years including one recent Tizen; Allow/Deny; token persistence and re-prompt; pin change; reconnect; target selection; repeat at 500/80ms; per-key machine evidence versus user-only; power-off observation; the no-remote Allow path; and two same-model TVs producing distinct `TvId`. If the gate cannot run, stop and re-plan.
- **Scope is the single-TV slice.** No multi-TV picker, no full TV details or Settings, and no wake. Full FR39 remains Epic 2.
- **Trust closure.** The one-TV switcher opens a minimal trust sheet: name, model, masked MAC, IP, SPKI fingerprint, Forget, and the copy "Pairing is stored encrypted on this phone; nothing is sent to the internet." The pairing card's Details link resolves here. This is the FR4/FR5 acceptance subset, not full FR39.
- **Power-off honesty.** Before the first power-off, show a one-time chip: "Power off only. This build can't wake the TV; use the TV's power button." No confirmation sheet. After power-off, the offline copy repeats the limitation.
- **Seams.** Ship all six launcher slots disabled behind one `SlotAvailability` state owner; later epics register availability policy only. Build the TV switcher as a list with one TV, not a single-TV special case. Keep `activeTarget` separate from any last-used value. Key the session owner by `TvId` from the start. No dead "Add a TV" action.
- **Inert data.** Persist the wake MAC, its provenance, `NetworkType`, and the reached interface even though wake ships in Epic 2. Store the TV record, chip dismissals, and debug overlay in Epic 1; declared-surface observations and probe results are stored by the first story that captures them (Epic 3); never store a resolved capability label.
- **Matrix denominator.** Pin the pilot TV pair and seed their matrix rows in Epic 1. Author the literal `*unknown*` row. Define overlay-to-release-asset promotion and `matrixVersion` review. Each epic exit commits its relevant rows and runs one release-asset-only dry run.
- **Identity and build.** Confirm the application ID/package base `com.unirem` and the Play reservation in the first scaffold work, before any signed build. Release smoke starts in Epic 1.
- **Permissions.** Epic 1's manifest has no `RECORD_AUDIO`, `POST_NOTIFICATIONS`, or MediaProjection; `VIBRATE` is the only install-time addition, for local haptics only; no voice, cast, mirror, media-server, or capture code.
- **Proof owned here.** Per-key evidence, the no-remote Allow path, two-TV identity, and power-off observation are Epic 1 exit evidence, not Epic 7 surprises.
- **Cross-cutting.** NFR4-NFR6, NFR9, NFR12, NFR17, and UX-DR39 apply from the first work item.

### Epic 2: Own the setup

A user with more than one TV can choose the right target, inspect and forget each TV, set preferences, and wake a TV the matrix says can wake — without the app guessing which screen to control.

**FRs covered:** FR6-FR7, FR25-FR27, FR39.

**Implementation notes:**
- **Spike A2 first.** Validate MAC capture, subnet-directed plus limited broadcast, the "Power On with Mobile" setting, Wi-Fi versus Ethernet, short versus long standby, and an 8001/8002 answer after wake.
- **Full switcher.** With one TV, auto-select it. With two or more, open the switcher on cold start and require an explicit choice; never auto-target the last-used TV. The Epic 1 list component is extended with multi-TV policy, not rewritten.
- **FR7 is split.** 7a target/restart is owned here. 7b sheet-close is verified in Epics 3-6, including the Pad sheet. 7c session pinning is verified in Epics 5-6.
- **Full FR39.** Settings for haptics, reduced motion, permissions, diagnostics, and about; TV details with identity, four capability labels, SPKI fingerprint, IP, masked MAC, Tizen version, last verified, and Forget. The Epic 1 trust sheet is the subset, not a duplicate surface.
- **Wake honesty.** Wake is a separate launcher slot, visible on every TV. Without a recorded pass for the model and network type, it stays locked with the explanation and sends no packet. A manual-IP, session-only TV cannot wake; say so.
- **Network type.** `NetworkType` is the TV's observed path from device evidence, `Unknown` otherwise; it is not inferred from the phone's current link. The wake MAC stays separate from `TvId`.
- **Labels.** Map the TV card's summary label to AD-4's four labels and keep the full four in TV details. Do not use one label for both jobs.
- **Target tests.** Epic 2 owns fake-session tests for the target rule; Epics 5-6 supply the real session integration.
- **Partial L check.** Exercise the WOL path under the local-network gate here; the full discovery/socket/WOL/media-server flow remains Epic 7.
- **No dead actions.** Settings may show a permission row that becomes active in a later epic, but the row explains the dependency and does not open a dead surface.

### Epic 3: Type and point

The user can send text to a focused TV field and, where the TV passes the pointer probe, move a pointer and click — without the app claiming focus detection it does not have.

**FRs covered:** FR14-FR19.

**Implementation notes:**
- **Spike B first.** Validate native-field arrival and `imeStart`/`imeEnd` delivery, test app-drawn grids separately, and prototype the pointer only where the cursor probe passes.
- **Text sheet.** One-line draft, explicit Send, no live keystrokes, memory-only draft, cleared on close, never logged. Password fields are not logged, retained, or suggested.
- **Pad gate.** Explain "This will move the TV's pointer. You can stop anytime." before the probe. On failure, disable the Pad slot with "This TV didn't pass the pointer check. Keys still work." and record the limitation.
- **Pointer space.** Use one named absolute TV panel coordinate space, clamped and converted only inside `:tv`. The hero D-pad never becomes a trackpad and never mixes pointer movement with key taps.
- **App-grid honesty.** FR16's app-grid probe is a per-surface capability, not a promise that typing works anywhere. State the limit before the attempt.
- **FR7 7b.** Switching TVs closes Text and Pad; the pointer does not retarget into a new TV.
- **Exit evidence.** One native field and one pointer pass on the pilot TV pair; an app-grid attempt with the honest limit shown.

### Epic 4: Talk

The user can hold to talk, map a recognized phrase to a remote command, or review dictation before sending it — with no recording, no wake word, and no background listening.

**FRs covered:** FR20-FR23.

**Implementation notes:**
- **Spike V first.** Build the phone STT-to-keys/text path, agree the command set, and measure latency. Raw-audio injection stays out of scope unless a proven TV ingestion endpoint exists.
- **Voice sheet.** 72dp hold-to-talk, release stops, slide up while holding locks until Done. No wake word and no always-listening state.
- **Audio privacy.** Use the platform recognizer; never record, store, or transmit audio. The explainer says "Speech is handled by your phone" and does not claim on-device recognition unless the platform guarantees it.
- **Command mapping.** One versioned `VoiceCommandMap` maps phrases to the same `RemoteCommand` vocabulary. One phrase produces exactly one `submit()`. Non-destructive commands execute on release; power off requires an explicit tap. Dictation goes through the TextOp path with a "Send to TV" review.
- **FGS decision.** Resolve the `microphone` foreground-service type against AD-12's no-background-capture rule before this epic starts. If there is no background capture, drop the type or justify it in the architecture note.
- **FR7 7b.** Switching TVs closes the Voice sheet. FR23 relies on the prior Epic 3 text path, not on a future epic.
- **Exit evidence.** One command phrase and one dictation review on the pilot TV pair, with the microphone-denied path shown.

### Epic 5: Cast

The user can pick one photo, video, or audio file and cast it to the active TV, control playback, and stop it — with the TV fetching from the phone over the local network and the session ending visibly when it ends.

**FRs covered:** FR28-FR32.

**Implementation notes:**
- **Spike C first.** Validate the jUPnP multicast lock, server reachability, `GET`/`HEAD`/`Range`, JPEG/MP3/H.264-AAC vectors, screen-off behavior, and the CDDL/Jetty audit. UPnPCast is considered only after provenance resolves.
- **One item per session.** System Photo Picker or document picker; active TV preselected and named; no queue, playlist, autoplay, or broad storage permission.
- **Renderer check.** Check before play; if absent, show the cast-receiver copy and create no session. Surface known format/subtitle limits before play. Never claim unverified codec, subtitle, seek, or long-file support.
- **Media server.** `:cast` mints `MediaId`, owns the URL path and id-to-bytes mapping, emits `http(s)` only, supports `HEAD` and byte `Range`, binds to the reached interface, rejects non-private peers, serves only the selected item, and stops at session end.
- **Readability.** Guarantee the item is readable for the session via a persistable grant re-validated at start or a session-cache copy under the byte limit. Surface `CastFailure.ItemUnreadable` with a visible state.
- **Session coordination.** Build the active-session coordinator here. Starting Mirror while Cast is active ends Cast with a visible `Superseded` reason. Epics 5-6 share one pinned session; no second owner.
- **Notification truth.** Cast survives screen-off only when the notification is granted. If `POST_NOTIFICATIONS` is denied, the session is foreground-only and ends on background with the fixed copy. Qualify FR32/UX-DR32 accordingly.
- **FR7 7c.** Switching TVs does not stop a pinned cast. Include switch-mid-cast in the exit evidence.
- **End states.** End on user Stop, renderer stop/error, TV disconnect, or force-stop. Show "Cast stopped" with the reason and "Start again" when the file is still available. Never restart silently.
- **Release smoke.** By Epic 5 exit, run a release-mode smoke with the asset-only matrix, R8, backup exclusion, and permissions.
- **Exit evidence.** One photo and one audio cast on the pilot pair, a `HEAD`/Range check, and a notification-denied path.

### Epic 6: Mirror

The user can check for the UniRem receiver, consent to screen capture only after the receiver is ready, mirror the phone screen, and stop it — with a versioned receiver contract and no promise the platform cannot keep.

**FRs covered:** FR33-FR36.

**Implementation notes:**
- **Contract first.** `docs/receiver-contract.md` must exist before the Mirror slot is enabled. It is the single source for endpoint discovery, transport binding, message names, and payload schema.
- **Spike M first.** Run the receiver path in the order B (WebRTC to the Tizen receiver), A (live-URI to AVTransport falsification), C (DIAL/launch harness). B requires a receiver.
- **Receiver ownership.** Epic 6 owns the tagged Tizen receiver build, the dev-mode sideload runbook, the `helloAck` handshake, and one WebRTC stream. A contract document alone is not a receiver. Epic 7 performs the pre-flight reinstall and handshake on the pilot pair.
- **Receiver states.** Implement `ReceiverAbsent`, `VersionMismatch`, `ViewerRejected`, `Busy`, `SignallingFailed`, and `MediaFailed` with visible copy and recovery actions. No MediaProjection consent before `helloAck`.
- **Slot gating.** Enable Mirror only with the contract, a matrix pass, and a ready receiver. Pre-2022 sets show the disabled slot with the honest explanation.
- **Session coordination.** Register Mirror with the Epic 5 active-session coordinator. Test cast-to-mirror and mirror-to-cast; the previous session ends visibly with `Superseded`.
- **FR7 7b/7c.** Switching TVs closes the Mirror setup sheet but does not stop a pinned mirror. The stop confirmation is "Stop mirroring?" / "Stop" / "Keep mirroring."
- **Notification truth.** Mirror uses the `mediaProjection` foreground service; the notification-denied path is foreground-only and ends on background.
- **Exit evidence.** A real receiver handshake, one mirrored stream with recorded latency/battery, a receiver-absent path, and a version-mismatch path.

### Epic 7: Prove the pilot

The project owner can install a signed build on the pilot phones and demonstrate the spec's success signal end to end on the named TV pair — with every claim backed by a matrix row or observed run, and every deferred risk closed.

**FRs covered:** None new; verifies all 41 FRs and the NFR/UX contract.

**Implementation notes:**
- **Spike L last.** Run the full discovery, socket, WOL, and media-server flow under targetSdk-37 behavior before rollout. Epic 1 has already run the gate smoke under the shipped targetSdk 36; Epic 2 has checked WOL; Epic 5 has checked the media server. Clarify in the release notes that L is a future-behavior audit, not a change to the shipping target.
- **Matrix on the signed artifact.** Use the named release denominator, the pilot pair, the `*unknown*` row, overlay-to-asset promotion, `matrixVersion`, and firmware invalidation. Debug overlay rows never satisfy a release gate.
- **Signed build.** Confirm the application ID, signing key, backup exclusion, R8 behavior, permissions, and Play internal-testing upload. The first signed build is not the first release check.
- **Accessibility audit.** TalkBack names/roles/live regions, 200% font scale, reduced motion, RTL, 1.3x string expansion, focus order, and contrast. Each epic has already carried its own accessibility definition of done.
- **End-to-end demo.** Two TVs; no-remote fallback; pairing; keys; text; pointer where supported; voice; cast; power off; wake; mirror. Include the denied, offline, identity-changed, and receiver-absent paths.
- **Receiver pre-flight.** Reinstall the tagged receiver, run the handshake, and confirm the contract version on the demo TV.
- **Failure rule.** If any gate fails, reopen the owning epic and fix it there. Do not turn a failed gate into a pilot note.

**Natural dependencies:** Epic 1 is standalone. Epic 2 builds on Epic 1. Epics 3, 4, and 5 build on Epic 1; Epic 4 uses the Epic 3 text path for dictation. Epic 6 builds on Epic 5's session coordinator. Epic 7 verifies all prior epics and adds no new feature.

## Epic 1: Pair and press

The user can find a Samsung TV on the local network, complete the TV's Allow prompt, trust the connection, and use a working one-TV remote — with every key and power-off action honest about what the phone observed.

**FRs covered:** FR1-FR5, FR8-FR13, FR24, FR37-FR38, FR40-FR41.

### Story 1.1: Prove the pairing and identity path (Spike A)

As the project owner,
I want to prove Samsung discovery, pairing, stable identity, token persistence, and SPKI pinning on the pilot TVs before the app foundation is built,
So that Epic 1 commits to a trust model the hardware actually supports and the pilot does not fail at the first screen.

**FRs:** FR1-FR5, FR8, FR37-FR38, FR40-FR41 (evidence only; no production implementation).

**Out of scope:** key delivery, repeat, text, pointer, voice, cast, mirror, multi-TV UI, Settings.

**Acceptance Criteria:**

**AC1 — Test set and harness**
**Given** at least three Tizen 2016+ TVs, including one recent set, and a physical Android test phone on the same LAN,
**When** the Spike A harness runs,
**Then** it records model, year, region, exact firmware string, and network type for each TV,
**And** it can attempt discovery, manual-IP entry, and a WSS connection without depending on the production app,
**And** if the required test set is unavailable, the story is blocked and the gate is not marked passed.

**AC2 — Discovery and manual IP**
**Given** the local-network gate is granted,
**When** discovery runs,
**Then** the harness sends SSDP M-SEARCH to `239.255.255.250:1900` under a held multicast lock and fetches `GET <tv>:8001/api/v2/` for name, model, Tizen version, `TokenAuthSupport`, and Wi-Fi MAC,
**And** when discovery is empty or blocked, manual IP entry is offered and attempts the same identity read,
**And** a manual-IP target with no resolvable stable identity is marked session-only and receives no durable record or Keystore alias,
**And** when the gate is denied, both discovery and manual IP are blocked and the result is recorded as `LanDenied`, not "TV didn't answer."

**AC3 — First pairing and Allow/Deny**
**Given** an unpaired TV,
**When** the harness opens the first WSS connection without a token,
**Then** the TV shows its Allow/Deny prompt, and Allow yields `ms.channel.connect` with a token on token-auth models,
**And** Deny or timeout yields `ms.channel.unauthorized` or a socket close, with no token or pin persisted,
**And** the harness records whether the Allow prompt can be answered without a physical remote; if it cannot, the required fallback is documented and the pilot premise is flagged.

**AC4 — Token persistence and re-prompt**
**Given** a successful Allow on a token-auth model,
**When** the harness reconnects after a process restart,
**Then** the token skips the prompt on models that persist it,
**And** any model that re-prompts is recorded per firmware, with the visible re-pair path exercised,
**And** no token, pin, or full MAC appears in logs.

**AC5 — SPKI pin capture and validation**
**Given** a successful pairing,
**When** the leaf SPKI is captured and stored,
**Then** the pin and token are keyed by stable identity, stored in Keystore-backed encrypted storage, and excluded from backup,
**And** every later connect validates the pinned leaf SPKI before attaching the token,
**And** a changed or mismatched pin fails closed into a visible re-pair flow, with no token sent,
**And** the harness confirms there is no trust-all `TrustManager`, no hostname-check suppression outside the pinned client, and no WSS-to-WS fallback.

**AC6 — Stable identity and two TVs**
**Given** two TVs, preferably the same model,
**When** both are paired,
**Then** stable identity follows device ID first and byte-exact MAC second, with one normalization rule applied identically everywhere,
**And** `TvId` is the hash of that identity; the two records are distinct; identity is not derived from IP or the current connectivity path,
**And** if no same-model pair is available, the gap is recorded and a synthetic identical-device-ID test is run; the gate cannot be fully passed until the real collision is resolved.

**AC7 — Reconnect and failure states**
**Given** a paired TV,
**When** the socket closes or the token is rejected,
**Then** reconnect uses visible backoff, token rejection/expiry surfaces "Pairing is required for this TV." with a re-pair action, and socket close surfaces the offline state,
**And** the harness confirms no command is silently queued or retried.

**AC8 — Security inspection**
**Given** the completed harness,
**When** storage, logs, and TLS configuration are inspected,
**Then** no token, pin, SPKI, full MAC, or pairing payload is logged or backed up,
**And** the security checks from AC5 are evidenced, not asserted.

**AC9 — Spike report and matrix rows**
**Given** all runs are complete,
**When** the spike is closed,
**Then** a report is committed at `_bmad-output/implementation-artifacts/spike-a-pairing.md` with one matrix row per TV/firmware/network, pass/fail/blocked results, exact firmware strings, re-prompt and pin-stability results, the no-remote Allow result, and any identity-collision gap,
**And** the report states whether the gate passes, what remains blocked, and whether Epic 1 foundation work may start,
**And** any hard failure produces a re-plan recommendation rather than a "mostly passed" note.

**AC10 — Scope discipline**
**Given** the spike,
**Then** it ships only a throwaway harness and the report — no production UI, no key delivery, no multi-TV switcher, no Settings, no voice, cast, or mirror code.

### Story 1.2: Prove key delivery and command evidence (Spike A)

As the project owner,
I want to prove the MVP key path, repeat behavior, and command-evidence model on the pilot TVs,
So that Epic 1 can show honest Submitted/Unconfirmed/Observed states and choose a defensible timeout instead of inventing one.

**FRs:** FR9-FR13, FR24 (evidence only; no production implementation).

**Out of scope:** production remote UI, multi-TV, text, pointer, voice, cast, mirror, Settings.

**Acceptance Criteria:**

**AC1 — Paired test setup**
**Given** the TVs paired in Story 1.1 and a physical Android test phone on the same LAN,
**When** the key-delivery harness connects over the authenticated WSS channel,
**Then** it records model, exact firmware, network type, and whether the TV is on and reachable,
**And** it can send raw protocol frames independently of the production app.

**AC2 — MVP key set**
**Given** an authenticated connection,
**When** the harness sends each MVP key — D-pad up/down/left/right, OK, Back, Home, Apps, Search, volume up/down/mute, play/pause, and power off —
**Then** each gesture produces exactly one `ms.remote.control` command with the correct `DataOfCmd`,
**And** the on-TV effect is recorded per key,
**And** no diagonal key or swipe navigation is used.

**AC3 — Evidence per key**
**Given** a key command,
**When** the TV responds or stays silent,
**Then** the harness records the evidence source for that key: machine event, state probe, or user confirmation only,
**And** a successful socket write is never recorded as a TV action,
**And** the matrix captures which keys are observably confirmed per model/firmware.

**AC4 — Latency and timeout**
**Given** repeated key presses on each test TV,
**When** the harness measures submit-to-evidence and submit-to-timeout,
**Then** it reports median and tail latency per key or key group,
**And** it states whether the 3s default is supported or which per-capability timeout should replace it,
**And** the recommendation is based on recorded runs, not a single sample.

**AC5 — Repeat behavior**
**Given** a held direction key,
**When** the hold passes 500ms,
**Then** the harness repeats at approximately 80ms intervals until release,
**And** release sends the key-up/Release frame,
**And** exactly one repeat engine is active; no other path synthesizes repeats,
**And** the TV effect and any dropped/reordered frames are recorded.

**AC6 — Late evidence and gesture generation**
**Given** two presses in sequence,
**When** evidence for the older press arrives after a newer press has started,
**Then** the older evidence is ignored and the newer gesture keeps its own state,
**And** an open gesture resolves to Idle when the socket closes,
**And** no stale evidence can turn a newer press green.

**AC7 — Socket close and offline behavior**
**Given** an open socket,
**When** the socket closes mid-press or between presses,
**Then** the repeat stops, the open gesture resolves to Idle, and further keys are disabled,
**And** no command is silently queued or retried on reconnect,
**And** the harness records whether a best-effort Release frame is possible without a live socket.

**AC8 — Power-off honesty**
**Given** a TV that is on,
**When** the harness sends power off,
**Then** it sends a distinct power key, not a toggle,
**And** it records whether the TV reports off, asleep, or unknown afterward,
**And** an unobserved power-off remains unconfirmed rather than being marked successful,
**And** no confirmation sheet is used.

**AC9 — Unsupported-key behavior**
**Given** a model that does not report a key,
**When** the user presses that key,
**Then** the key remains usable and the limitation is recorded for the one-time chip and TV record,
**And** the harness does not label the key "failed" or hide it without explanation.

**AC10 — Spike report and matrix rows**
**Given** all runs are complete,
**When** the spike is closed,
**Then** a report is committed at `_bmad-output/implementation-artifacts/spike-a-keys.md` with one matrix row per TV/firmware/network, per-key evidence, latency, repeat behavior, power-off observation, and unsupported-key results,
**And** the report states whether the key path passes, what remains blocked, and the recommended timeout values,
**And** any hard failure produces a re-plan recommendation rather than a "mostly passed" note.

**AC11 — Scope discipline**
**Given** the spike,
**Then** it ships only a throwaway harness and the report — no production remote UI, no key-state design work beyond the evidence model, and no text, pointer, voice, cast, or mirror code.

### Story 1.3: Ship an installable Graphite Mint shell with confirmed identity

As the pilot user,
I want to install UniRem and see the Graphite Mint app shell on my phone,
So that I can confirm the app runs before the TV setup flow is added.

**FRs:** Enabling story; UX-DR1-UX-DR4, NFR1, NFR10-NFR13, NFR18.

**Out of scope:** discovery, LAN gate, no-TV CTA, pairing, remote, launcher, voice, cast, mirror, Settings.

**Acceptance Criteria:**

**AC1 — Four-module scaffold**
**Given** a clean checkout,
**When** `./gradlew build lint :core:test` runs,
**Then** `settings.gradle.kts`, the version catalog, and `:app`, `:core`, `:tv`, `:cast` exist,
**And** the module graph is exactly `:app → :tv, :cast, :core`; `:tv → :core`; `:cast → :core`,
**And** `:core` has no Android dependency and no other project dependencies exist,
**And** the build, lint, and `:core` tests pass.

**AC2 — Pinned toolchain and CI**
**Given** the repository,
**Then** Gradle 9.5.0, AGP 9.3.1, JDK 17, Kotlin 2.4.21, compileSdk 37, targetSdk 36, and minSdk 26 are pinned,
**And** the version catalog carries the architecture stack pins without adding unused libraries to modules,
**And** a GitHub Actions workflow runs `./gradlew build lint :core:test` on push and pull request,
**And** CI does not require a live TV or instrumentation run.

**AC3 — Confirmed app identity**
**Given** the scaffold,
**When** the project owner confirms the package identity,
**Then** the application ID and package base are `com.unirem`,
**And** the confirmation, signing owner, and "do not change after release" note are recorded in `docs/app-identity.md`,
**And** no signed build is produced before that confirmation,
**And** the manifest contains only the permissions the shell needs; it has no `RECORD_AUDIO`, `POST_NOTIFICATIONS`, MediaProjection, or foreground-service declarations.

**AC4 — Installable shell**
**Given** a debug build,
**When** it is installed on an API 26 device and an API 36/37 test device,
**Then** it launches without a crash and shows a simple shell using the Graphite Mint theme,
**And** the screen identifies UniRem as a local remote for Samsung TVs and shows the build version,
**And** there is no fake setup action, no dead button, and no claim that a TV is connected.

**AC5 — Graphite Mint tokens**
**Given** the shell,
**Then** the dark-only palette uses the specified surface, ink, accent, ring, status, banner, focus, and scrim tokens,
**And** dynamic color and a light theme are disabled,
**And** tonal depth uses `surface-base → surface-raised → surface-key` plus 1dp hairlines,
**And** there are no drop shadows, gradients, blur, or glassmorphism; the ring glow and 2dp health halo remain the only glows.

**AC6 — Typography, spacing, and sizing**
**Given** the shell,
**Then** the typography scale uses the specified display/title/headline/body/body-small/label/micro/button/state/key/code sizes and weights in `sp`,
**And** uppercase is used only for `micro`, critical labels do not truncate, and tabular numerals are available where specified,
**And** spacing uses the 4dp scale, 20dp screen gutter, 24dp section spacing, the 8/14/18/24/full radius tokens, and a 480dp maximum content width,
**And** the portrait surface supports 360–480dp and every interactive target is at least 48dp.

**AC7 — Shell scope discipline**
**Given** this story,
**Then** it adds no discovery, LAN gate, manual IP, pairing, remote keys, launcher, voice, cast, mirror, Settings, telemetry, or network client code,
**And** the shell is a foundation build, not a simulated product surface.

### Story 1.4: Gate local-network access and own the permission state

As the pilot user,
I want UniRem to ask for local-network access only when I start setup and to explain a denial,
So that I can grant or refuse knowingly and never see a silent empty scan.

**FRs:** FR37, FR40; UX-DR24, UX-DR36; NFR4-NFR6, NFR16; AD-8, AD-11.

**Out of scope:** SSDP discovery, `/api/v2/` identity read, manual-IP form, pairing, remote, Settings, voice, cast, mirror.

**Acceptance Criteria:**

**AC1 — One gate, one owner**
**Given** the app,
**Then** a single app-level `LanAccessGate` owns the local-network permission, the process-wide `MulticastLock`, the isolation diagnostic, and the manual-IP route,
**And** `:core` defines `LanAccess` with exactly `Granted`, `Denied`, and `NotDeclared`,
**And** `:tv` and `:cast` consult the gate before any network attempt and map denial to `TvFailure.LanDenied` and `CastFailure.LanDenied`,
**And** neither module requests a permission or launches permission UI.

**AC2 — Permission explainer**
**Given** the user starts setup,
**When** the gate needs local-network access,
**Then** a bottom sheet explains in one sentence why the app needs it, what it will do, and what happens if the user says no,
**And** Continue triggers the system prompt; "Not now" leaves the feature unstarted,
**And** a denied state offers "Open settings" and a plain explanation,
**And** there is no permission wall on first launch and no repeated system prompt after a decision.

**AC3 — API 36 and API 37 behavior**
**Given** the targetSdk is 36 and the local-network permission is enforced for apps targeting API 37+,
**Then** the manifest declares the local-network permission and the gate remains the only requester,
**And** on an API 37 test device the runtime permission is requested and mapped to `Granted` or `Denied`,
**And** on API 36 the gate reports the platform-appropriate state without showing a permission wall,
**And** no discovery or manual-IP path bypasses the gate on either version.

**AC4 — Denied access blocks both paths**
**Given** the gate is `Denied`,
**When** the user attempts discovery or manual IP,
**Then** both are blocked identically and the result is `LanDenied`,
**And** the UI shows the single gate explainer, not "TV didn't answer,"
**And** no scan, socket, or fallback attempt is made.

**AC5 — Multicast lock lifecycle**
**Given** a scoped network operation,
**When** the gate starts and finishes it,
**Then** exactly one process-wide `MulticastLock` is held for the operation and released afterward,
**And** no second lock is acquired by another module or by jUPnP,
**And** a failure or cancellation still releases the lock.

**AC6 — Isolation diagnostic**
**Given** the gate is denied, or a scan is empty while access is granted,
**Then** the app exposes a quiet diagnostic readout with the network state, scan result, and the relevant diagnostic code,
**And** the readout distinguishes permission denial from network isolation and from "no TVs responded,"
**And** the copy is diagnostic, not a marketing explanation.

**AC7 — Visible permission card**
**Given** the shell,
**Then** a "Local network access" card shows the current state and offers the appropriate action: grant, open settings, or explain that access is ready,
**And** the card never claims a TV is connected,
**And** it is the only entry point for permission management in Epic 1; full Settings remains Epic 2.

**AC8 — Testability**
**Given** the gate and its state mapping,
**Then** JVM tests cover `Granted`, `Denied`, and `NotDeclared` transitions and the `TvFailure`/`CastFailure` mappings,
**And** the permission prompt and multicast lock lifecycle are verified on the API 36 and API 37 test devices,
**And** no test requires a live TV or a cloud service.

**AC9 — Scope discipline**
**Given** this story,
**Then** it adds no SSDP, `/api/v2/`, pairing, manual-IP form, remote keys, launcher, voice, cast, mirror, Settings, telemetry, or cloud fallback.

### Story 1.5: Find a TV, pair it, and persist trust

As the pilot user,
I want to find my Samsung TV, confirm it is mine, and answer the TV's Allow prompt,
So that UniRem remembers the TV and I do not have to pair it again every time.

**FRs:** FR1-FR4, FR5 (pin capture only; later validation in Story 1.7), FR37-FR38, FR40; UX-DR14, UX-DR21, UX-DR25, UX-DR35 (pairing subset), UX-DR36; NFR4-NFR6, NFR12, NFR16; AD-7, AD-8, AD-11.

**Out of scope:** reconnect/backoff, token rejection, identity-changed re-pair, trust sheet/SPKI display, Forget, multi-TV switcher, remote keys, matrix labels, text, pointer, voice, cast, mirror, wake, Settings.

**Acceptance Criteria:**

**AC1 — No-TV setup shell**
**Given** the app has no stored TV and the local-network gate is granted,
**When** the setup shell opens,
**Then** it shows "No TVs yet." / "UniRem controls Samsung TVs on your Wi-Fi. Nothing is sent to the internet." / "Add your first TV,"
**And** it shows a quiet `code` diagnostic readout with the phone's network type, scan result ("Not scanned"), and a diagnostic code when one exists; it is not a marketing page,
**And** the six-slot launcher is visible in order Text, Voice, Pad, Cast, Mirror, Wake, disabled with a dashed hairline, `ink-disabled`, and a lock mark; no slot starts a feature,
**And** the launcher uses the custom line glyphs, not Material icons, emoji, or text-as-icon,
**And** "Add your first TV" opens Add TV and starts exactly one scan; there is no auto-scan loop,
**And** when a TV is already stored, the setup entry is not shown; the shell shows the paired TV summary from AC9 instead.

**AC2 — Discovery and identity read**
**Given** the local-network gate is granted and the user opens Add TV,
**When** the scan starts,
**Then** it sends SSDP M-SEARCH to `239.255.255.250:1900` using the search target proven in Spike A, under the gate's held multicast lock,
**And** it parses each response's `Location`, rejects non-private hosts, and fetches `GET http://<ip>:8001/api/v2/` for name, model, Tizen/firmware, `TokenAuthSupport`, and `wifiMac`,
**And** every request, including redirects, passes the single `:core` `PrivatePeer` predicate; no public, ULA, or non-private destination is contacted,
**And** the firmware string is stored byte-exact and never normalized,
**And** results appear as found under "Scanning for TVs…" with no spinner,
**And** manual IP is offered in the scanning, empty, and results states,
**And** the scan runs once on open, offers "Scan again" afterward, stops when the screen leaves, and releases the multicast lock,
**And** a failed `/api/v2/` result is skipped with a diagnostic record; one bad TV does not crash or stop the scan.

**AC3 — Empty, denied, and isolation states**
**Given** the scan ends with no results,
**Then** the screen shows "No TVs found. Check that your phone and TV are on the same Wi-Fi." / "Enter IP address" / "Scan again,"
**And** a Details link explains client isolation in plain language; it does not fall back silently to a stored TV,
**And** when the gate is denied, the screen shows "UniRem needs local network access to reach your TV." / "Open settings" / "Not now,"
**And** no scan or manual-IP request is made while denied, and the result is not presented as "TV didn't answer,"
**And** the Add TV and Pairing surfaces use the topbar pattern: back plus a static title, right side empty.

**AC4 — Manual IP and session-only targets**
**Given** the user chooses "Enter IP address,"
**When** they enter an IPv4 or IPv6 literal,
**Then** whitespace is trimmed, DNS names are rejected, and invalid input shows an inline error without any request,
**And** the address must pass the same `PrivatePeer` predicate; public addresses are rejected before a socket is opened,
**And** a valid address fetches `/api/v2/` and shows the same pairing card as discovery,
**And** when the address resolves to an already discovered TV, it maps to the same `TvId` and no duplicate card or record is created,
**And** when no device ID and no MAC resolve, the card is labeled "Session only — this TV won't be saved," pairing may proceed with an in-memory token/pin, and no Room record or Keystore alias is created,
**And** a failed manual-IP read shows "The TV didn't answer. Check the IP and try again." with "Try again" / "Cancel."

**AC5 — Pairing card and explicit choice**
**Given** a discovered or manually entered TV,
**Then** its card shows the name, model, Tizen version, masked MAC, and the first-time note "First time connecting — verify it's yours,"
**And** a discovered card says "same Wi-Fi as this phone"; a manual-IP card says "Added by IP address" instead of claiming a shared Wi-Fi,
**And** a missing field is shown as "Not reported," never invented, and the masked MAC reveals at most the last two octets,
**And** the primary action is "This is my TV — connect"; the secondary "That's not my TV" is always visible,
**And** no card auto-pairs, auto-connects, or appears selected by default,
**And** "That's not my TV" removes the card from the current results and stores nothing; a later scan may show it again,
**And** the card never shows a full MAC, token, SPKI, or capability claim.

**AC6 — Pairing prompt and no-remote path**
**Given** the user taps "This is my TV — connect,"
**When** the first WSS connection opens without a token to `wss://<ip>:8002/api/v2/channels/samsung.remote.control?name=<URL-encoded base64(appName)>`,
**Then** the Pairing surface shows "Press Allow on your TV." / "A prompt is on your TV screen right now."
**And** the no-remote path is always visible: "No remote? On many Samsung TVs, the button under the logo can select Allow. Press it once, then press and hold."
**And** the surface includes a simple line diagram of the button under the Samsung logo and says "If your TV's button can't select Allow, you'll need the remote."
**And** the app never promises remote-free pairing, never tries to press Allow itself, and shows no spinner or silent retry,
**And** the scoped client accepts the TV's self-signed certificate only for this first connection, captures the leaf SPKI, and installs no global trust manager or hostname-check suppression,
**And** there is no fallback from WSS 8002 to WS 8001.

**AC7 — Allow, deny, timeout, and persistence**
**Given** the pairing connection is open,
**When** the TV allows the client,
**Then** `ms.channel.connect` yields the token on token-auth models; the app stores the token with the captured leaf SPKI pin, or stores the pin only when the TV reports `TokenAuthSupport=false`,
**And** if `TokenAuthSupport=true` but no token arrives, pairing fails with no durable record,
**And** the TV record is keyed by the stable identity from `/api/v2/`, never by IP alone,
**And** on deny, the app shows "The TV said no." / "You can try again from the TV's Allow prompt," stores no token or pin, and does not auto-retry,
**And** on timeout or close without a token, the app shows "The TV didn't answer." / "Keep the Allow prompt open and try again." / "Retry" / "Not now," keeps the button diagram visible, and stores no token or pin,
**And** "Retry" starts a new attempt only on user action; "Not now" returns to the card/results with nothing stored,
**And** the pairing socket closes after success; no remote session is opened and the app does not claim "Connected."

**AC8 — Storage, backup exclusion, and logging**
**Given** a successful pairing with a resolved identity,
**Then** Room stores the TV record: `tvId`, stable identity, name, model, exact firmware, full Wi-Fi MAC and its provenance, `NetworkType`, reached interface, Keystore alias, and paired timestamp,
**And** `NetworkType` comes from TV evidence when reported, otherwise `Unknown`; it is never inferred from the phone's current link,
**And** the full MAC is stored for wake only and is never displayed,
**And** the token and leaf SPKI pin live in Keystore-backed encrypted storage under a per-TV alias; Room stores the alias, never the token or pin,
**And** pairing material is excluded from Android Auto Backup and device transfer,
**And** the `PairingStore` port is declared by `:tv` and implemented in `:app`; `:tv` never reads Room or Keystore directly,
**And** `RedactedLog` is the only logger; token, pin, SPKI, full MAC, and pairing payloads never appear in logs, and no `println` or direct `Log` call exists outside the sink.

**AC9 — Paired state, identity, and dedupe**
**Given** pairing succeeds,
**Then** the success state shows "Paired," the TV name and model, and a "Done" action; it never says "Connected" and shows no remote keys,
**And** returning to the shell shows the single paired TV summary, with the launcher still visible and disabled; the trust sheet and Forget arrive in Story 1.6,
**And** `StableIdentity` follows the confirmed device ID first and the byte-exact MAC second, with one normalization rule; `TvId` is the hash of that identity and never derived from IP,
**And** two same-model TVs produce distinct `TvId`s, and a manual-IP target that resolves to an already discovered TV reuses the same record and alias,
**And** a session-only target uses an in-memory key, not a persisted `TvId`, and is gone after the process ends,
**And** this story never replaces or forgets a stored TV; pairing a second TV and multi-TV selection are Epic 2.

**AC10 — Testability**
**Given** the discovery, identity, pairing, and storage logic,
**Then** JVM tests cover identity precedence, `TvId` stability, duplicate resolution, MAC masking, IP validation, session-only gating, scan-state mapping, and the `PairingStore` contract with a fake store,
**And** instrumentation or manual pilot tests cover SSDP, `/api/v2/`, first WSS pairing, Allow/Deny, token persistence after process restart, and the no-remote path,
**And** no CI test requires a live TV, and no test asserts a socket write as success or prints pairing material.

**AC11 — Scope discipline**
**Given** this story,
**Then** it adds no reconnect/backoff, token-rejection or identity-changed flow, trust sheet, Forget, multi-TV switcher, remote keys, matrix labels, text, pointer, voice, cast, mirror, wake, or Settings,
**And** it adds no cloud fallback, no trust-all manager, no WSS-to-WS fallback, and no second `PrivatePeer` or multicast-lock implementation.

### Story 1.6: Inspect and forget a trusted TV

As the pilot user,
I want to see what UniRem stores about my TV and forget it when I choose,
So that I can verify the trust relationship and remove it without hunting through settings.

**FRs:** FR39 (Epic 1 trust subset), FR38 (deletion subset); UX-DR21 (Details), UX-DR35 (TV details subset); NFR4-NFR5, NFR12; AD-7.

**Out of scope:** full TV details (four capability labels, last verified, diagnostics), Settings, one-TV switcher, remote, reconnect, health dot, matrix, text, pointer, voice, cast, mirror, wake.

**Acceptance Criteria:**

**AC1 — Reachability and durable-only**
**Given** a durable paired TV,
**When** the user taps "TV details" on the paired summary,
**Then** the trust sheet opens, titled "TV details" and naming the TV,
**And** it reads the stored TV record and pin without opening a socket, so it works with the TV off,
**And** it is a reusable destination keyed by `TvId`; Story 1.9's one-TV switcher opens the same sheet,
**And** a session-only target has no trust sheet or Forget; its summary says "Session only — this TV won't be saved,"
**And** the sheet dismisses on back, scrim tap, or drag-down, and focus returns to "TV details."

**AC2 — Identity and fingerprint**
**Given** the trust sheet,
**Then** it shows the TV name, model, Tizen version, masked MAC, last reached IP, and SPKI fingerprint,
**And** the mask reveals at most the last two MAC octets, and the IP is labeled "Last reached IP" because it may be stale,
**And** the fingerprint is the SHA-256 of the pinned leaf SPKI, shown as grouped uppercase hex in `code` typography, fully visible and selectable,
**And** it is derived from the stored pin, not a live probe; the app does not claim the TV displays the same string,
**And** if the stored pin is missing or unreadable, the sheet shows "Fingerprint unavailable" and Forget remains available,
**And** the sheet never shows the full MAC, token, pin, or a capability label.

**AC3 — Durable state and local-only copy**
**Given** the trust sheet,
**Then** it shows "Paired" as the durable state and never "Connected" or a live health state,
**And** it shows the exact copy "Pairing is stored encrypted on this phone; nothing is sent to the internet."
**And** it makes no claim that the TV is online, reachable, or currently connected,
**And** no critical label or fingerprint line truncates; the fingerprint wraps or scrolls inside its block.

**AC4 — Forget confirmation and deletion**
**Given** the trust sheet,
**When** the user taps "Forget TV,"
**Then** the sheet content changes to the confirmation "Forget [TV]? You'll need to pair again." / "Forget" / "Cancel," with no nested sheet,
**And** "Forget" is the destructive action in salmon outline, never a filled red block, and "Cancel" is secondary,
**And** on confirm, the app closes any open channel for that TV without sending a command, deletes the Keystore alias/token/pin, deletes the Room TV record and its dependent rows, clears the active target, and returns to the no-TV state,
**And** the app notes "The TV may still list this phone until you remove it on the TV,"
**And** it does not claim to remove the phone from the TV; it only removes local trust,
**And** the launcher remains disabled, and "Add your first TV" works again; a new pairing creates a new record and alias.

**AC5 — Cancel and failure paths**
**Given** the Forget confirmation,
**When** the user taps "Cancel" or dismisses it,
**Then** nothing is deleted and the trust sheet remains available,
**And** if Keystore or Room deletion fails, the app keeps the record and shows "The TV couldn't be forgotten. Try again." / "Try again" / "Close,"
**And** it does not claim the TV was forgotten until both the trust material and the record are gone,
**And** a missing Keystore alias is treated as already deleted, so the Room record can still be removed,
**And** "Try again" retries only on user action; no silent retry or partial success message.

**AC6 — Privacy and logging**
**Given** the trust sheet and Forget,
**Then** `RedactedLog` is the only logger; the fingerprint may be displayed but is never logged,
**And** token, pin, SPKI, full MAC, and pairing payloads never appear in logs,
**And** no `println` or direct `Log` call exists outside the sink,
**And** deletion leaves no token, pin, alias, or full MAC in Room, DataStore, or app-private files.

**AC7 — Testability**
**Given** the trust sheet and deletion logic,
**Then** JVM tests cover MAC masking, fingerprint formatting, the Forget state machine, cancel and failure paths, and the `PairingStore.delete(tvId)` contract with a fake store,
**And** instrumentation or manual tests verify Keystore alias deletion and Room record deletion after process restart,
**And** no token, pin, or full MAC remains after a successful Forget,
**And** no test requires a live TV, asserts a socket write as success, or prints secrets.

**AC8 — Scope discipline**
**Given** this story,
**Then** it adds no full TV details, four capability labels, last verified, diagnostics, Settings, one-TV switcher, remote, reconnect, health dot, matrix, text, pointer, voice, cast, mirror, or wake,
**And** the trust sheet is the Epic 1 FR39 subset, not full FR39,
**And** it adds no token/pin display, cloud call, trust-all manager, or second trust store.

### Story 1.7: Reconnect, go offline honestly, and recover identity changes

As the pilot user,
I want UniRem to reconnect when the TV comes back, tell me when it is offline or no longer trusted, and let me re-pair,
So that I can recover without guessing whether the TV is off, the network failed, or the TV's identity changed.

**FRs:** FR5, FR8, FR13 (state only; key UI in Stories 1.10-1.11), FR37, FR40; UX-DR8, UX-DR19, UX-DR36, UX-DR39; NFR4, NFR6-NFR7, NFR12; AD-3, AD-7, AD-11.

**Out of scope:** keys/D-pad, `CommandTracker`/`submit()`, ring, launcher enablement, full Remote shell/topbar switcher, matrix labels, trust sheet/Forget changes, multi-TV target switching, text, pointer, voice, cast, mirror, wake, Settings.

**Acceptance Criteria:**

**AC1 — Connection status surface**
**Given** a durable paired TV,
**When** the user taps "Check connection" on the paired summary,
**Then** the status surface opens and starts exactly one connection using the stored record, token, and pin,
**And** it shows the TV name, "Paired" as the durable label, a health dot, and a text state: "Connecting…" then "Connected," "Reconnecting…," or "Offline"; no spinner,
**And** leaving the surface closes the socket and stops backoff; there is no background reconnect,
**And** there is no auto-connect on app launch or while the surface is closed; the paired summary remains "Paired,"
**And** the TV name is static; the TV switcher is Story 1.9,
**And** the surface adds no keys, ring, launcher enablement, or command state,
**And** `ConnectionHealth` owns the live state and power state `On`/`Unknown`; asleep and wake observations are later.

**AC2 — Pinned connection and token order**
**Given** the connection starts,
**Then** the app selects the stored `TvId` and alias and builds a client scoped to that TV,
**And** the client validates the pinned leaf SPKI during the TLS handshake before any HTTP upgrade or token bytes are sent; a mismatch aborts with `TvFailure.IdentityChanged`,
**And** on a match, the token, when present, is attached in the WSS URL; the token may be in the URL string but is not written to the socket until the handshake succeeds,
**And** a TV with `TokenAuthSupport=false` uses the pin-only path, and a token-auth TV with no token is `PairingRequired`,
**And** the scoped client is the only place hostname checks are relaxed; there is no trust-all manager, no WSS-to-WS fallback, and no cloud path,
**And** every destination passes `PrivatePeer` and the gate must be `Granted`; otherwise the result is `TvFailure.LanDenied`, not "TV didn't answer,"
**And** `ms.channel.unauthorized` or a TV re-prompt maps to `PairingRequired`, not `Offline`,
**And** the token, pin, SPKI, full MAC, and WSS URL are never logged; `RedactedLog` is the only logger.

**AC3 — Backoff, reconnecting, and offline**
**Given** the socket closes or a connection attempt fails,
**Then** the health state moves to `Reconnecting`, the dot is amber, and the banner shows "Reconnecting…" / "Retry" / "Not now," with no spinner,
**And** the app retries on one injected schedule: 1s, 2s, 4s, 8s, 16s, 30s cap, with ±20% jitter, reset after 30s connected; there is no tight loop or second timer,
**And** "Retry" attempts immediately, resets the schedule, and first tries the stored IP; if that fails and the gate is granted, it may run one discovery scan and reconnect only to the same `TvId`, never a different TV,
**And** "Not now" stops auto-retry and moves to `Offline`; the offline banner shows "TV didn't answer — retry?" / "Keys are off until the TV reconnects." / "Retry" / "Not now,"
**And** when the schedule is exhausted, the state is `Offline`; Retry restarts it,
**And** a network change is treated as a socket close and never reuses a stale socket,
**And** a connection attempt has a bounded 10s timeout,
**And** `keysEnabled` is false in `Reconnecting` and `Offline`, and true only when `Connected`; this story renders no keys,
**And** no command is queued, replayed, or retried; the outbound queue is empty on close.

**AC4 — Identity changed**
**Given** the pin check fails, or a fresh `/api/v2/` read reports a different stable identity than the stored record,
**Then** the app aborts before sending the token, sets `keysEnabled` false, and shows "This TV's identity changed. Re-pair to continue." / "Re-pair,"
**And** it never overwrites the stored pin or record automatically, never retries with the old pin, and never falls back from WSS to WS,
**And** the banner is assertive, and the health dot is amber,
**And** "Re-pair" opens the re-pair flow in AC6; back leaves the banner and keys disabled,
**And** the trust sheet and Forget from Story 1.6 remain available,
**And** no token, pin, or SPKI is logged.

**AC5 — Pairing required**
**Given** the TV rejects the token with `ms.channel.unauthorized` or re-prompts despite a token,
**Then** the app shows "Pairing is required for this TV." / "Re-pair," sets `keysEnabled` false, and does not retry with the old token,
**And** the old token is not deleted until a successful re-pair; the record and trust sheet remain,
**And** a transient network failure is not mislabeled as pairing required; only an explicit unauthorized or re-prompt maps here,
**And** the pairing-required banner is polite, not assertive, and back leaves it in place.

**AC6 — Re-pair atomicity and identity**
**Given** the user taps "Re-pair,"
**Then** the Story 1.5 Allow/Deny flow opens with the stored TV name and identity, including the always-visible no-remote path, and no old token on the first connection,
**And** on success, the app verifies the fresh `/api/v2/` stable identity equals the stored `TvId`; if it does not, it fails closed as identity changed and does not replace trust,
**And** on success, the new token and pin are written under a new alias, the TV record is updated to point to it, the old alias is deleted, and no duplicate record or alias exists,
**And** the record's name, model, firmware, IP, and reached interface are updated when they changed,
**And** on deny, timeout, or failure, the old record and trust material remain but are still unusable; the banner remains and no duplicate is created,
**And** the old trust is not deleted before the new trust is committed; a failed replacement leaves the old record and alias intact,
**And** re-pair reuses Story 1.5's copy and paths; there is no second pairing implementation,
**And** after success, the app shows "Connecting…" and only claims "Connected" when the new channel is open.

**AC7 — Health UI and accessibility**
**Given** the status surface,
**Then** the health dot is 8dp with a 2dp halo at 18%: connected mint, reconnecting amber, offline salmon; it does not pulse except during reconnect, and reduced motion removes the pulse,
**And** its accessibility label names the TV and health, and color is not the only carrier because the text state is visible,
**And** the banner uses the oxblood style, one recovery action plus an optional "Not now," pushes content, stays within two lines plus the action row, and shows no spinner,
**And** the banner is assertive only for offline and identity changed; reconnecting and pairing required are polite,
**And** "Paired" and "Connected" are distinct and never used interchangeably,
**And** the state word never truncates; targets are at least 48dp; 200% font scale, RTL, and no horizontal scrolling are respected,
**And** the status surface uses the topbar pattern with no command state in the topbar.

**AC8 — No queue and no background socket**
**Given** the status surface is open,
**Then** the socket is scoped to that surface; leaving closes it and stops backoff,
**And** no command is queued, replayed, or retried on reconnect, and no synthetic frame is emitted,
**And** the app does not auto-connect on launch or while the surface is closed,
**And** process death leaves no socket; the next launch shows "Paired," not "Connected,"
**And** the story acquires no foreground service, wake lock, or notification for this connection.

**AC9 — Testability**
**Given** the connection and recovery logic,
**Then** JVM tests cover the backoff schedule, state transitions, `keysEnabled`, unauthorized and pin-mismatch mapping, the re-pair state machine, `PairingStore` replace atomicity with a fake store, and the no-queue invariant,
**And** an instrumentation test with a local TLS test server verifies that a pin mismatch aborts before any token request bytes, a token rejection maps to `PairingRequired`, and a changed stable identity fails re-pair,
**And** manual pilot tests cover the real TV happy path, offline/reconnect, token rejection/re-pair, and identity changed/re-pair,
**And** no test requires a live TV in CI, asserts a socket write as success, or prints secrets.

**AC10 — Scope discipline**
**Given** this story,
**Then** it adds no keys/D-pad, `CommandTracker`/`submit()`, ring, launcher enablement, full Remote shell/topbar switcher, matrix labels, one-TV switcher, trust sheet/Forget changes, multi-TV, text, pointer, voice, cast, mirror, wake, or Settings,
**And** it adds no cloud fallback, no WSS-to-WS fallback, no trust-all manager, no command queue or retry, no background socket, foreground service, wake lock, or token/pin logging,
**And** the status surface is a precursor; Story 1.10 replaces it with the Remote and reuses `ConnectionHealth` and the banner,
**And** the re-pair flow reuses Story 1.5; no second pairing implementation exists.

### Story 1.8: Earn capability labels from the versioned matrix

As the pilot user and matrix owner,
I want UniRem to resolve what this TV can do from a versioned matrix and give me a debug harness to record what I actually observed,
So that the app never claims support it hasn't earned and the pilot has a defensible denominator.

**FRs:** FR41 (full), FR38 (read-only asset, debug overlay, no stored label subset), FR12 (capability-limitation record precursor); NFR2, NFR12, NFR14, NFR18; UX-DR22, UX-DR27, UX-DR43; AD-4, AD-5.

**Out of scope:** remote keys/D-pad, `CommandTracker`, ring, launcher enablement, one-TV switcher, full Remote, trust sheet/Forget changes, full TV details/four-label list, Settings, text, pointer, voice, cast, mirror, wake, multi-TV, one-time chips/dismissals, probe capture, cloud/telemetry, downloaded matrix.

**Acceptance Criteria:**

**AC1 — Versioned matrix asset and schema**
**Given** the Spike A reports from Stories 1.1-1.2 and the named pilot TV set,
**When** the matrix asset is authored,
**Then** `app/src/main/assets/verification-matrix.json` is the release-readable artifact, with a monotonic top-level `matrixVersion` and rows keyed by byte-exact `model`, byte-exact `firmware`, `NetworkType`, and `CapabilityKey` (capability plus scope),
**And** every row carries a `matrixVersion` equal to the artifact version, a result (`Pass`, `Partial`, `Ambiguous`, `Fail`, `NotTested`), a generation (`Tizen2016Plus`, `HJ2014_15`, `Unknown`), and an optional evidence note; no row carries a UI label,
**And** the closed vocabulary is:
- `Capability`: Keys, Text, Pointer, Wake, Cast, Mirror, Voice
- `CapabilityScope`: Device, NativeField, AppGrid, PointerBrowser, PointerAppGrid, FormatJpeg, FormatMp3, FormatH264Aac, FormatHevc, Subtitles, Seek
- `NetworkType`: Wifi, Ethernet, Unknown, reused from Story 1.5
- `CapabilityLabel`: Works, NotTested, NotSupported, BestEffort

**And** the allowed pairs are validated; an unknown capability/scope pair is rejected,
**And** the pilot rows seed only the keys Spike A actually observed, at minimum `Keys/Device`; `Keys/Device` is `Pass` only when every MVP key has observed evidence, otherwise `Partial`; no row is invented,
**And** the literal fallback row is `model="*"`, `firmware="*unknown*"`, `networkType=Unknown`, `Keys/Device=Partial`; it is the only wildcard, it requires both literal sentinels, and it never matches a known firmware,
**And** the asset contains no token, pin, SPKI, full MAC, IP, or per-device data; it is versioned with the app and never downloaded or edited at runtime.

**AC2 — Resolver precedence and exact matching**
**Given** a `CapabilityInput` for one TV with its record, asset rows, declared-absent capabilities, and probe results,
**When** the resolver evaluates a `CapabilityKey`,
**Then** it applies, in order:
1. a capability marked declared absent → `NotSupported` for every scope of that capability;
2. a probe hard-fail for this key → `NotSupported` for this key;
3. a matrix `Fail` for this key → `NotSupported`;
4. an H/J 2014-15 generation → `BestEffort`, even with a matrix `Pass`;
5. a matrix `Pass` → `Works`;
6. a matrix `Partial`/`Ambiguous`, or a probe `Pass` without a matrix `Pass` → `BestEffort`;
7. no row and no probe → `NotTested`.

**And** a `NotTested` matrix row resolves to `NotTested`; a `Fail` is never softened to `BestEffort`, and a passing probe never promotes a matrix `Fail`,
**And** matching is byte-exact on model and firmware, exact on network type and key; no trim, case-fold, locale, normalization, or wildcard except the literal `*unknown*` fallback; a `Wifi` row does not match `Ethernet` or `Unknown`,
**And** the resolver has no user-confirmation input; user confirmation changes command provenance only,
**And** it is pure, deterministic, has no Android dependency, returns one label per key, and contains no UI copy.

**AC3 — One display owner and the TV-card label**
**Given** the paired TV summary,
**Then** `:app` has exactly one capability display owner, `CapabilityLabelTracker`, exposing `StateFlow<CapabilityLabels>` keyed by `TvId`; it builds `CapabilityInput` from the TV record, asset rows, debug overlay (debug only), declared-absent set, and probe results, and calls the resolver,
**And** the TV card shows one durable label for `Keys/Device`: "Full control — confirms keys" only when the exact label is `Works`; otherwise "Best-effort — can't confirm commands,"
**And** the exact four-value label remains available to Epic 2 TV details; the card summary is a confirmation summary, not a support guarantee,
**And** the label is a text plate, not a colored badge; it does not change with socket health and is not the health dot,
**And** the label is based on the stored, last-observed firmware; a fresh `/api/v2/` read or re-pair updates it, and the card never claims the firmware is current,
**And** it recomputes when the TV record, firmware, matrix version, overlay, declared surfaces, or probe results change; no resolved label is stored in Room, DataStore, or preferences,
**And** the owner is the only caller of the resolver; the card and future TV details read the same flow; no screen caches a label or invents a support claim,
**And** a session-only target shows "Session only — this TV won't be saved" and no durable capability label,
**And** the card keeps its existing actions; no dead "Open remote" before Story 1.10,
**And** the card never says "universal," "works with any Samsung TV," or a blanket support claim.

**AC4 — Debug harness and overlay**
**Given** a debug build,
**When** the developer opens the debug-only "Matrix harness" from the paired summary,
**Then** the harness is absent from release builds and is not one of the 13 product surfaces,
**And** it shows the active TV's exact model, firmware, network type, loaded `matrixVersion`, each capability key's asset result, resolved label, and matching overlay row,
**And** model, firmware, and network type are read-only from the TV record; the developer records only a result (`Pass`, `Partial`, `Ambiguous`, `Fail`, `NotTested`) and a short evidence note,
**And** Save writes one overlay row to the debug-only Room database, replacing the previous row for the same exact key; the row carries model, exact firmware, network type, capability key, result, note, `matrixVersion`, and UTC `observedAt`,
**And** the harness opens no socket, sends no keys, runs no probe, and makes no observation itself; its copy says "Record only what you observed on the TV,"
**And** the debug resolver reads asset rows plus overlay rows; an overlay row with the same exact key and the same `matrixVersion` wins; a row with a different version is stale, ignored, and shown as stale with a Clear action,
**And** the harness can copy its overlay rows as asset-shaped JSON and clear the overlay after confirmation; it never edits the release asset,
**And** the TV card reflects the same owner immediately after Save,
**And** the overlay contains no token, pin, SPKI, full MAC, IP, or text payload.

**AC5 — Release reads the asset only**
**Given** a release build,
**Then** the debug harness route, overlay database, DAO, and overlay rows are not compiled or reachable; release reads only `app/src/main/assets/verification-matrix.json`,
**And** a stale debug database on the device is never opened by release,
**And** no overlay row can satisfy a release gate; the release label is computed from the asset and TV record only,
**And** debug and release use the same `CapabilityInput.resolve()` path; only the row source differs, and the resolver is not build-variant-aware,
**And** a release smoke confirms the harness route/strings are absent and the TV-card label matches the asset.

**AC6 — Matrix version, firmware invalidation, and promotion**
**Given** the matrix asset,
**Then** a validator rejects a version mismatch, duplicate exact key, missing `*unknown*` fallback, unknown capability/scope pair, unknown network type, blank model/firmware, and malformed JSON,
**And** a known firmware matches only its exact row; an unknown firmware matches only the literal fallback; a firmware change on the TV record invalidates the old exact row and any overlay row for the old firmware, so the label recomputes to the fallback or `NotTested`; no stored label survives,
**And** `docs/matrix-promotion.md` documents the runbook: record in the debug harness, copy asset-shaped rows, review the evidence, merge into the asset, bump `matrixVersion`, update the human verification matrix evidence, run `./gradlew build lint :core:test` plus a release smoke, and commit both; promoted overlay rows are cleared and never auto-promoted,
**And** a `verifyMatrix` task runs the same parser/validator against the shipped asset as part of `check`, so CI catches a malformed asset,
**And** the harness shows the loaded version and stale rows; a version change requires re-observation or explicit review, not silent reuse.

**AC7 — Unknown, missing, and failure behavior**
**Given** the asset is missing, malformed, or has no matching row,
**Then** the app does not crash; affected keys resolve to `NotTested` or the literal fallback and the card shows "Best-effort — can't confirm commands," never "Full control,"
**And** a missing model, firmware, or network type is "Not reported"/`Unknown`, matches only the literal fallback, and never a known row,
**And** the app never presents a capability label as a support guarantee; a `Fail` is not hidden, but the key remains usable per FR12 later and the exact label is retained for Epic 2,
**And** the diagnostic is quiet and redacted; no token, pin, SPKI, full MAC, IP, or text payload appears in the harness, logs, or asset.

**AC8 — Testability**
**Given** the resolver, asset, display owner, and harness,
**Then** `:core` JVM tests cover exact matching (case/whitespace/locale differences do not match), unknown-firmware fallback only, network-type matching, H/J capping, declared-absent for every scope, probe fail/pass precedence, matrix pass/partial/ambiguous/fail/not-tested, no user-confirmation input, and one label per key,
**And** `:app` tests or the `verifyMatrix` task cover the asset schema/version, duplicate keys, missing `*unknown*`, malformed JSON, the single owner recomputing with no stored label, debug overlay precedence and stale versions, release row source ignoring overlay, and TV-card copy mapping,
**And** a release-mode smoke confirms the harness/overlay is absent and the card reads the asset only,
**And** no test requires a live TV, asserts a socket write as success, or prints secrets.

**AC9 — Scope discipline**
**Given** this story,
**Then** it adds no remote keys/D-pad, `CommandTracker`, ring, launcher enablement, one-TV switcher, full Remote, trust sheet/Forget changes, full TV details/four-label list, Settings, text, pointer, voice, cast, mirror, wake, multi-TV, one-time chips/dismissals, or probe capture,
**And** it adds no cloud, telemetry, remote config, downloaded matrix, second resolver, second `NetworkType`, second capability display owner, stored resolved label, or release-visible overlay,
**And** the only visible product change is the one durable TV-card label; the debug harness is developer-only,
**And** rows for untested features stay absent or `NotTested`; no placeholder support and no "universal" claim,
**And** the public `:core` surface stays within the architecture allowlist; matrix schema types are nested/under `CapabilityInput`, and the resolver implementation stays internal,
**And** Story 1.9's switcher and Epic 2's TV details reuse the same resolver/owner without a second implementation.

### Story 1.9: Switch the one paired TV without guessing

As the pilot user,
I want to open a TV switcher from the topbar, see the one TV I paired, and reach its details,
So that the app has one explicit target and the switcher is ready for more TVs without changing the model later.

**FRs:** FR6 (one-TV auto-select subset; multi-TV policy in Epic 2), FR7 (target-identity precursor; full rule in Epic 2); UX-DR6, UX-DR7 (one-TV list subset), UX-DR15, UX-DR22, UX-DR26 (switcher sheet), UX-DR27, UX-DR39; NFR9, NFR12, NFR17; AD-3, AD-7.

**Out of scope:** multi-TV storage, multi-TV picker, cold-start choice, "Add a TV," last-used target, Settings/gear, remote keys, full Remote, trust sheet/Forget changes, text, pointer, voice, cast, mirror, wake, session pinning, target-change sheet-close rules, pinned sessions.

**Acceptance Criteria:**

**AC1 — Topbar switcher and one active target**
**Given** a durable paired TV,
**When** the paired summary or the future Remote topbar is shown,
**Then** the left side is a 48dp TV switcher button with an 8dp health dot, the TV name in `body` 16sp, a 12dp mint caret, and 8dp gaps; the right side is empty in Epic 1, with no Settings gear,
**And** the button opens the TV switcher sheet and does not change the target,
**And** the name truncates with an ellipsis when long; the full name is always in the switcher sheet and TV details,
**And** the health dot is the active `ConnectionHealth`; when no connection session exists it uses the offline token and its accessibility label says "not connected," never "TV off" and never a command outcome,
**And** the active target is a `TvId` held by one process-scoped `TargetSession` owner; no screen reads the Room record as its own target,
**And** with one stored TV the target is auto-selected; with zero there is no target; there is no persisted last-used target and no `isActive` flag in Room,
**And** the topbar never contains command state or a capability claim.

**AC2 — The one-TV switcher sheet**
**Given** the user taps the switcher button,
**Then** the TV switcher opens as a bottom sheet with the standard grabber, 64% scrim, maximum 80% height, title "TVs," and a list with one row for the stored TV,
**And** it is a list with one item, not a single-TV layout branch; Story 2 adds multi-TV policy and rows without rewriting it,
**And** it is a sheet, not a tab, screen, or bottom navigation; one sheet at a time; no nested sheet,
**And** it opens no socket and makes no connection attempt; the row's health dot reflects the active `ConnectionHealth` or the offline/not-connected state,
**And** it dismisses on drag-down, scrim tap, or back; focus moves into the sheet and returns to the switcher button,
**And** it has no "Add a TV" action in Epic 1; Story 2 adds it as the last row, and there is no dead button,
**And** it is not a separate "Manage" surface; TV details is a row action.

**AC3 — TV row content and actions**
**Given** the switcher row,
**Then** it shows the health dot, the full TV name, the model, and the durable capability label from Story 1.8; the label is a text plate, not a colored badge,
**And** the current TV is marked "Current" with a text label and a check glyph; the mark is not color-only,
**And** tapping the row selects that TV and closes the sheet; with one TV already current, it only closes and does not restart, reconnect, or reset health,
**And** the row has a separate 48dp "TV details" action that opens the Story 1.6 trust sheet keyed by `TvId`; it does not change the target,
**And** the row never shows "Open remote," "Forget," a full MAC, IP, token, pin, or a support guarantee; Forget stays in TV details,
**And** health and capability are distinct: an offline TV can still show "Full control — confirms keys," and a connected TV can still show "Best-effort — can't confirm commands,"
**And** the row reads the single `CapabilityLabelTracker` and the single `ConnectionHealth`; no second label or health computation,
**And** the row is at least 48dp high and uses the existing raised/surface tokens with a 1dp hairline, not a shadow.

**AC4 — Target selection and cold start**
**Given** the stored TV list,
**Then** `TargetSession` exposes `NoTarget` or `Selected(TvId)`; zero TVs yields `NoTarget`, one yields `Selected` automatically, and two or more cannot be created in Epic 1 because multi-TV is Epic 2,
**And** cold start with one TV auto-selects it; cold start with zero shows the no-TV setup; process death never restores a stale target,
**And** no last-used value is persisted or consulted, and the target is never an index, IP, or record position,
**And** the Story 1.7 status surface and the future Remote read the active `TvId` from `TargetSession`; no screen holds a second target,
**And** selecting the current TV is a no-op beyond closing the sheet; no command is queued or synthesized,
**And** Story 2 adds multi-TV choice and "Add a TV" by extending this owner and list, not by replacing them.

**AC5 — Empty, forgotten, and session-only states**
**Given** the switcher is open or the TV is forgotten,
**Then** if the stored TV is removed by Story 1.6 while the sheet is open, the sheet closes, `TargetSession` clears to `NoTarget`, and the no-TV setup appears; no stale row remains,
**And** if the active TV record is unreadable, the app shows "TV unavailable" / "Pair again" and does not select another TV or invent a target,
**And** a session-only target is not a stored TV: it has no switcher button, no sheet row, no TV details action, and no durable capability label; its topbar shows the name without a caret and its summary says "Session only — this TV won't be saved,"
**And** with no stored TV the switcher is absent; the no-TV setup is the only surface,
**And** the switcher never selects a TV from a stale last-used value or a removed record.

**AC6 — Accessibility and layout**
**Given** the topbar button and switcher sheet,
**Then** the button and row are at least 48dp targets; the row's accessible name includes TV name, model, health or not-connected state, current state, and capability label; the health dot has its own label and is not color-only,
**And** the "TV details" action has an accessible label that names the TV; the check glyph is not the only current-state signal,
**And** the sheet moves focus in, traps it while open, and returns it to the switcher button on dismiss; TalkBack announces the current row as selected/current,
**And** at 200% font scale, 1.3× string expansion, and RTL, the row wraps and the sheet scrolls; the full name and actions do not truncate; the topbar may truncate because the full name is in the sheet and details,
**And** the caret mirrors in RTL while the health dot and TV name do not; the health dot pulses only during reconnect and not under reduced motion,
**And** the sheet has no horizontal scrolling, tabs, FAB, or persistent bottom navigation, and adds no new animation or sound.

**AC7 — Testability**
**Given** the target state, topbar, and switcher,
**Then** JVM tests cover `TargetSession` with zero and one TV, no last-used selection, `TvId` keying, current-TV no-op, forgotten/unreadable records, and session-only exclusion,
**And** Compose/UI tests or manual pilot tests cover the topbar button, sheet open/dismiss, focus return, current marker, TV-details action, health/capability labels, and 200%/RTL layout,
**And** no test requires a live TV, asserts a socket write as success, or prints token, pin, SPKI, or full MAC,
**And** the tests reuse the single `CapabilityLabelTracker` and `ConnectionHealth`; no duplicate state owner or fake label table.

**AC8 — Scope discipline**
**Given** this story,
**Then** it adds no multi-TV storage, multi-TV picker, cold-start choice, "Add a TV," last-used target, Settings/gear, remote keys, full Remote, trust sheet/Forget changes, text, pointer, voice, cast, mirror, wake, session pinning, or target-change sheet-close rules,
**And** it adds no cloud, telemetry, second target owner, second health owner, second capability label, persisted active target, or dead button,
**And** the only visible changes are the topbar switcher, the one-TV sheet, and the TV-details entry point,
**And** Story 1.10's Remote reuses the topbar, switcher, and `TargetSession`; Story 2 extends the same list and owner for multiple TVs.

### Story 1.10: Open the Remote and connect to the selected TV

As the pilot user,
I want the paired TV card to open a real Remote that connects to that TV and tells me when it is reconnecting or offline,
So that I can see which TV I am controlling and recover the connection before I send any key.

**FRs:** FR8 (Remote shell and recovery), FR13 (keys-disabled state only; no keys rendered), FR37, FR40; UX-DR6, UX-DR7 (reuse), UX-DR8, UX-DR10 (Idle/Offline subset), UX-DR14 (launcher visibility only), UX-DR19, UX-DR20 (Idle/Offline subset), UX-DR22 (Open remote), UX-DR26, UX-DR27, UX-DR36, UX-DR39, UX-DR43; NFR4, NFR6-NFR7, NFR9, NFR12, NFR17; AD-3, AD-6, AD-7, AD-8.

**Out of scope:** D-pad/directions/OK, function keys, utility row/volume/play-pause/power, any key frame, `CommandTracker`/`submit()`, repeat/gesture generation, Submitted/Unconfirmed/Observed ring states, one-time chips, text, pointer, voice, cast, mirror, wake, Settings, launcher enablement, multi-TV/Add TV, full TV details/four-label list, trust/Forget/matrix changes, session chip, foreground service/notification/wake lock, cloud/telemetry.

**Acceptance Criteria:**

**AC1 — Entry, target, and retired status surface**
**Given** a durable or session-only target selected by `TargetSession`,
**When** the paired summary's TV card is shown,
**Then** its primary action is "Open remote," replacing "Check connection," and it opens the Remote without opening a socket itself,
**And** the Story 1.7 status surface, its route, and its "Check connection" action are removed; no dead action or duplicate connection surface remains,
**And** the Remote opens only for the active `TvId`; with no target or an unreadable stored record, it does not open and the Story 1.9 no-TV/"TV unavailable — Pair again" path remains,
**And** app launch and the paired summary do not auto-connect; opening the Remote starts the first connection, and "Paired" is never shown as "Connected,"
**And** the paired summary remains the Epic 1 pre-Remote home and keeps its TV details action, durable capability label, health dot, and debug-only Matrix harness; it owns no socket and is not a second Remote,
**And** a session-only target opens the same Remote using in-memory trust, with no switcher, caret, TV details action, or durable capability label,
**And** leaving the Remote closes its session and returns to the paired summary; no socket outlives it.

**AC2 — Remote shell layout**
**Given** the Remote opens,
**Then** it uses the topbar pattern: 56dp, `surface-base`, 1dp bottom hairline, 20dp side padding; the left is Story 1.9's TV switcher for a durable target, or the TV name and health dot without a caret for a session-only target; the right is empty in Epic 1, with no Settings gear,
**And** the column is 360–480dp wide, centered, with a 20dp gutter and no scrolling; the banner, when present, sits under the topbar and pushes content,
**And** the 236dp ring is centered in the lower third, 2dp stroke, no fill, not tappable or focusable; on a typical 800dp-tall phone its center sits between 62% and 70% of the viewport; on a short viewport it may shrink to 208dp only after the quiet space collapses; its interior is empty in this story, with no D-pad, placeholder keys, loading indicator, or "coming soon" copy,
**And** the 24dp state line sits directly under the ring, and the existing six-slot launcher is visible above the bottom safe area,
**And** the launcher stays disabled: all six slots keep their positions, dashed hairlines, lock marks, and `ink-disabled`; no slot starts a feature or opens a sheet,
**And** there is no utility row, function-key row, D-pad, one-time chip, session chip, dashboard, tab, FAB, bottom navigation, or horizontal scrolling,
**And** the ring and launcher never move into the banner's space; the state word and banner copy wrap rather than truncate, and every target stays at least 48dp.

**AC3 — One UI-scoped session and one health owner**
**Given** the Remote is open for a target,
**When** its session starts,
**Then** exactly one remote session is bound to that `TvId`; it opens exactly one channel with the stored record, token, and pin (or the in-memory trust for a session-only target), and never reads Room as its own target,
**And** it reuses Story 1.7's connection, pin validation, backoff, `ConnectionHealth`, re-pair, and no-queue behavior; no second connection, health owner, or socket is created,
**And** the session is scoped to the Remote's foreground lifetime; leaving the Remote, backgrounding the app, clearing the target, forgetting the TV, or changing the target closes the socket and stops backoff, with no background reconnect,
**And** recomposition or a configuration change does not create a second channel; at most one session exists for the target, and no socket crosses a target,
**And** returning to the Remote, or relaunching after process death, starts a fresh session, revalidates the pin, and shows no cached health or command state; process death leaves no socket,
**And** a session-only target uses its in-memory token/pin and creates no Room record or Keystore alias,
**And** the switcher and TV details sheets over the Remote do not close the session; only leaving the Remote or losing the target does,
**And** no foreground service, notification, wake lock, or command queue is created; `keysEnabled` is read from `ConnectionHealth` but no key is rendered yet.

**AC4 — Idle/offline ring and state line**
**Given** the Remote session's health,
**Then** the ring has only two states in this story: Idle and Offline; the pale mint, mint, and amber command states, the 900ms breath, and the "It worked" action remain Story 1.11,
**And** the ring is Idle when the session is connected and no command is in flight, and also while the first attempt or an active reconnect is in progress; it is Offline only when no usable socket remains: terminal offline, identity changed, pairing required, or LAN denied,
**And** the idle ring uses `ring-idle` with no glow; the offline ring uses `ring-offline` with the salmon glow, held until Retry/re-pair starts or the session becomes usable again; under reduced motion both change instantly with no pulse or animated glow,
**And** the ring never shows a spinner, checkmark, sweep, percentage, segmented arc, or command text; it is not a connection indicator,
**And** the state line is a 24dp-minimum centered row with a 6dp lamp and a 13sp/500 label: "Idle" or "Offline"; it matches the ring and never reports health,
**And** the state line is a polite live region, announces the state once per change, and never says "Sent," "Waiting for TV…," "TV confirmed," "You confirmed," "It worked," or "Connected,"
**And** the state word does not truncate and wraps to two lines at 200%; the ring and state line add no haptic, sound, or new animation,
**And** the topbar health dot and the ring never swap roles: the dot reports socket health, the ring reports command state, and neither is the other's label.

**AC5 — Banner and recovery**
**Given** the Remote's health or identity changes,
**Then** the Story 1.7 banner is reused with its oxblood style, one recovery action, optional "Not now," 16dp padding, 14dp radius, maximum two lines plus the action row, no spinner, and content pushed rather than overlaid,
**And** "Reconnecting…" / "Retry" / "Not now" appears while backoff is active; "TV didn't answer — retry?" / "Keys are off until the TV reconnects." / "Retry" / "Not now" appears when terminal offline; "This TV's identity changed. Re-pair to continue." / "Re-pair" appears on pin mismatch; "Pairing is required for this TV." / "Re-pair" appears on token rejection,
**And** the initial attempt is represented by the amber health dot and its "connecting" label, with no banner and no spinner; once it fails, the reconnecting banner appears,
**And** the banner is assertive only for terminal offline and identity changed; reconnecting, pairing required, and LAN denied are polite,
**And** "Retry" immediately attempts once, resets the backoff, tries the stored IP first, and may run one discovery scan only to reconnect the same `TvId`; it never selects a different TV,
**And** "Not now" stops auto-retry and moves to terminal Offline; "Re-pair" opens Story 1.7's re-pair flow, which reuses Story 1.5's pairing with the stored identity and no old token, and after success the Remote opens a fresh session on the same target,
**And** a failed re-pair keeps the old record and trust material, leaves the banner and keys disabled, and creates no duplicate; the old channel is closed before re-pair starts and no two sessions overlap,
**And** if the local-network gate is denied, no socket opens and the banner uses "UniRem needs local network access to reach your TV." / "Open settings" / "Not now"; it never says "TV didn't answer" or "TV off,"
**And** one banner is shown at a time; no toast, snackbar, dialog, or nested banner replaces it.

**AC6 — Switcher, details, and no command surface**
**Given** the Remote is open,
**Then** the durable topbar switcher opens Story 1.9's sheet; it opens no socket and does not restart the session; the current row closes the sheet without reconnecting or resetting health; the row's "TV details" opens Story 1.6's trust sheet and does not change the target,
**And** TV details reads the stored record and pin without opening a socket; closing it returns focus to the switcher row, and the Remote session continues,
**And** if the active TV is forgotten from TV details, the Remote closes the session without sending a command and returns to the no-TV setup; no stale Remote or sheet remains,
**And** if the active record becomes unreadable while the Remote is open, the session closes and the Story 1.9 "TV unavailable" / "Pair again" state replaces the Remote; no other target is selected,
**And** the Remote renders no keys, D-pad, utility/function row, command state, one-time chip, session chip, Settings gear, launcher enablement, or key sheet,
**And** the only visible changes are the Remote shell, the "Open remote" card action, and the removal of the status surface/Check connection.

**AC7 — Accessibility and layout**
**Given** the Remote shell,
**Then** the topbar switcher, banner actions, and launcher slots are at least 48dp; the ring is not focusable; focus order is topbar/switcher → banner action(s) → state line → launcher, with future utility/function/D-pad slots inserted in the UX-DR39 order,
**And** TalkBack names the switcher with the TV name, health, and "double tap to switch TV"; the session-only name is static and names the health; the state line announces "Idle" or "Offline"; the banner is a live region that is assertive only for terminal offline and identity changed,
**And** health, ring state, and disabled launcher are never color-only; each has a label, lamp, lock, or text,
**And** at 200% font scale, 1.3× string expansion, and RTL, the topbar name truncates with an ellipsis, the state line and banner wrap, the sheet scrolls, and no horizontal scrolling or critical label truncation occurs; the caret mirrors, while the health dot, ring, and TV name do not,
**And** reduced motion removes the health-dot reconnect pulse, keeps ring color/glow static, and adds no new animation or sound,
**And** the shell has no tabs, FAB, persistent bottom navigation, or dashboard; the launcher is the only bottom bar and stays disabled.

**AC8 — Testability**
**Given** the Remote shell, session binding, and card entry,
**Then** JVM tests cover entry only with a target, no-target/unreadable guards, one session per `TvId`, close on leave/background/target clear/Forget, no auto-connect from the card, no duplicate on recomposition, session-only exclusion from Room/Keystore, health-to-ring/state-line mapping (Idle vs Offline), and the no-queue/no-`CommandTracker` invariant,
**And** Compose/UI tests or manual pilot tests cover the Open remote action, removed status surface, topbar/switcher, empty ring, Idle/Offline state line, banner states, disabled launcher, focus order, 200%/RTL, and session close on back/background,
**And** the Story 1.7 instrumentation tests for pinned connect/disconnect/timeout/re-pair are reused with the Remote shell; no live TV is required in CI,
**And** no test writes a key, asserts a socket write as success, or prints token, pin, SPKI, full MAC, IP, or WSS URL.

**AC9 — Scope discipline**
**Given** this story,
**Then** it adds no D-pad/directions/OK, function keys, utility row/volume/play-pause/power, key frames, `CommandTracker`/`submit()`, repeat/gesture generation, Submitted/Unconfirmed/Observed states, one-time chips, text, pointer, voice, cast, mirror, wake, Settings, launcher enablement, multi-TV/Add TV, full TV details/four-label list, trust/Forget/matrix changes, or session chip,
**And** it adds no cloud, telemetry, remote config, background socket, foreground service, notification, wake lock, command queue, second connection/health/target owner, WSS-to-WS fallback, or trust-all manager,
**And** it reuses Story 1.5's pairing/re-pair, Story 1.6's TV details/Forget, Story 1.7's connection/backoff/banner, Story 1.8's capability owner via the card/switcher, and Story 1.9's topbar/switcher/`TargetSession`; no second implementation,
**And** the status surface is removed, not left as a dead route; "Check connection" no longer exists,
**And** Story 1.11 adds the D-pad and command truth inside the ring; Story 1.12 adds the utility/function keys and power; Story 1.13 adds repeat and late evidence; Story 1.14 completes accessibility/RTL.

### Story 1.11: Put the D-pad in the ring and tell the truth about every tap

As the pilot user,
I want a D-pad inside the ring that sends one key per tap and tells me whether the TV confirmed it,
So that I can navigate the TV without mistaking a socket write for an on-TV effect.

**FRs:** FR9 (D-pad and OK subset), FR11, FR13 (key enablement subset); UX-DR3 (standard 236dp subset), UX-DR4, UX-DR5, UX-DR10, UX-DR11, UX-DR20, UX-DR27, UX-DR28, UX-DR29, UX-DR39; NFR3, NFR8, NFR9, NFR12, NFR17; AD-2, AD-3.

**Out of scope:** FR10 and every press-and-hold behavior — the 500ms/80ms repeat, `Press`/`Release` frames, key-up on release, held-gesture handling, no strobing on repeat, and adversarial late-evidence coverage (Story 1.13); the utility row, function keys, power off, and Back (Story 1.12); one-time chips, unsupported-key limits, and the durable limitation record (Story 1.15); haptics, reduced-motion work, the compact 208dp D-pad, and the full accessibility/RTL/200% audit (Story 1.14); text, pointer, voice, cast, mirror, wake, Settings, multi-TV/Add TV, target switching, session chip, launcher enablement, full TV details/four-label list, trust/Forget/matrix changes, cloud, and telemetry.

**Acceptance Criteria:**

**AC1 — D-pad geometry and key states**
**Given** the Remote is open for the active target and Story 1.10's 236dp ring is rendered,
**When** the D-pad is composed inside it,
**Then** it is a 3×3 cluster with columns 64dp/76dp/64dp, rows 60dp/70dp/60dp, and 4dp gaps, centered inside the ring without crossing its 2dp stroke,
**And** the cells hold Up top-center, Left middle-left, OK center, Right middle-right, and Down bottom-center; the four corners are empty,
**And** the four direction keys are 60×56dp, `rounded.md` (14dp), `surface-key`, 1dp `hairline`, with a custom 15sp chevron; no unicode arrow, Material icon, triangle, or text-as-icon,
**And** OK is a 70dp circle with `accent` fill and an `ink-on-accent` 15sp/500 "OK" label centered in the middle cell; it is the only mint fill in the cluster,
**And** pressed directions use `surface-key-pressed`, pressed OK uses `accent-pressed`, and both scale to 0.98 for 80ms; no shadow, gradient, bevel, or haptic,
**And** disabled keys keep their position, glyph, and label, use `ink-disabled` with the key surface at 45% opacity, and expose disabled semantics; the 4dp gap is structural and every effective touch target is at least 48dp,
**And** all five keys are always present; there are no diagonals, no swipe surface, no pointer behavior, no placeholder, and no key hidden for model support,
**And** the ring remains 236dp, 2dp, unfilled, not tappable, and not focusable; on a short viewport collapse the quiet space first and keep 236dp in this story. Story 1.14 owns the compact 208dp ring and the compact D-pad variant, which must preserve 56dp-wide directions, a 70dp OK, and every target at least 48dp,
**And** the ring, state line, and launcher keep Story 1.10's placement and the launcher stays disabled; no utility row, function-key row, empty reserved row, or placeholder is added.

**AC2 — One tap, one tracked command, one submit**
**Given** the socket is usable and the D-pad is enabled,
**When** the user taps a direction or OK once,
**Then** `:app` calls `TvControl.submit(RemoteCommand)` exactly once for that tap, mapping each direction and OK to its matching `RemoteCommand`; OK sends the Enter command,
**And** `:tv`'s existing key path writes exactly one `ms.remote.control` Click frame for that submitted command; `:app` imports no `KEY_*` constant, no protocol frame, and no socket,
**And** `CommandTracker` in `:tv` is the one tracker for the active target, created with the channel session and cancelled with it; it is the only writer of `CommandOutcome`, exposes one `StateFlow<CommandOutcome>`, and opens exactly one gesture generation for the tap,
**And** `:app` observes that one flow and keeps no second outcome, timer, or `confirmed` boolean,
**And** a recomposition, TalkBack activation, or configuration change does not replay the tap; one user action yields one `submit()` and one frame,
**And** a long touch produces the same one command as a tap: there is no `Press`/`Release`, no long-press handler, no repeat timer, and no repeat engine; Story 1.13 adds the only repeat engine,
**And** when the D-pad is disabled, no click action is invoked, `submit()` is not called, and no frame is written, queued, or replayed.

**AC3 — Submitted, Unconfirmed, and the injected timeout**
**Given** the D-pad is enabled and the socket is open,
**When** an accepted tap's frame is written,
**Then** the tracker moves to `Submitted`; the ring uses `ring-submitted` with one 900ms breath, once for that press; the state line shows "Sent" with its 6dp lamp in the same color,
**And** the breath starts once at Submitted and is never restarted by Unconfirmed or Observed; there is no loop, checkmark, percentage, sweep, or segmented arc,
**And** Submitted is pale mint, not a success color; no write acknowledgement or socket event changes that,
**And** the tracker applies the injected command timeout, default 3000ms with the per-capability override from Spike A; no composable or `:app` timer owns the 3s threshold,
**When** the timeout elapses with no evidence for the current generation and the socket is still open,
**Then** the tracker moves to `Unconfirmed`; the ring uses `ring-unconfirmed` with the amber glow; the state line shows "Waiting for TV…" and expands to 48dp with a 48dp "It worked" action,
**And** Unconfirmed persists until evidence, user confirmation, the next tap, or the session ends; it is amber, never red, and never says "Failed," "No response," or "Error",
**When** a new tap is accepted while any state is showing,
**Then** it starts at `Submitted` again, resets the breath and timeout, and supersedes the previous ring state; the ring belongs to the last press only.

**AC4 — Evidence, provenance, and "It worked"**
**Given** an accepted command for the current generation,
**When** a `TvEvent` or a command-scoped `StateProbe` tagged to that generation arrives,
**Then** the tracker moves to `Observed` with machine provenance; the ring uses `ring-observed` with the 24dp mint glow; the state line says "TV confirmed"; the state holds for 4s and then returns to `Idle`,
**And** machine evidence may arrive before or after the 3s timeout and may resolve Unconfirmed to Observed; the pale-mint breath does not restart,
**When** the user taps "It worked" while the state is Unconfirmed,
**Then** the tracker moves to `Observed` with `UserConfirmed` provenance; the ring uses `ring-observed`; the state line says "You confirmed"; the state holds for 4s and then returns to `Idle`,
**And** "It worked" emits `UserConfirmed` evidence only: it never calls `submit()`, writes a frame, or re-sends the key,
**And** Observed is never entered from a socket write, a write callback, `ms.channel.ready`, `clientConnect`, a generic health transition, silence, or the absence of an error,
**And** adapters emit `Evidence` (`TvEvent`, `StateProbe`, `UserConfirmed`) and never a `CommandOutcome` or a `confirmed` verdict; the tracker is the only resolver, following Story 1.2's per-key evidence rule,
**And** "It worked" appears only in Unconfirmed, is 48dp, and is absent in Submitted, Observed, and Idle; after a generation settles or is superseded it cannot confirm another tap,
**And** the first evidence that resolves a generation wins; later duplicate or late evidence for that generation does not change provenance, restart the 4s hold, or reopen the ring,
**And** machine and user provenance are never interchanged: "TV confirmed" requires `TvEvent` or `StateProbe`; "You confirmed" requires `UserConfirmed`; no checkmark, confetti, "Success!", or green check appears before evidence.

**AC5 — Socket gating, offline, and no queue**
**Given** the Remote session and its existing `ConnectionHealth`,
**When** the socket is usable and `keysEnabled` is true for `Connected`,
**Then** the D-pad is enabled, including while the TV is silent and the command is Unconfirmed or Observed; the user can tap again,
**And** the D-pad re-enables when `keysEnabled` becomes true again; no stale disabled state survives a reconnect,
**When** the socket is closed,
**Then** the D-pad is disabled for the initial connecting period, `Reconnecting`, terminal `Offline`, identity changed, pairing required, and LAN denied; the existing Story 1.10 recovery banner remains the only recovery surface and is unchanged,
**And** when the socket closes with a command in flight, the tracker clears the open generation to `Idle`, never `Unconfirmed` or `Observed`; no frame is written, queued, replayed, or retried; the ring and state line show Story 1.10's Offline pair and the keys disable,
**And** a reconnect or new session starts at `Idle`; no previous outcome, pending timeout, or last key is restored, and the next tap requires a new `submit()`,
**And** `Offline` is not a `CommandOutcome`: the tracker is `Idle`, `ConnectionHealth` owns the offline, identity, pairing, and LAN states, and `:app` combines the two read-only,
**And** the topbar dot remains socket health and the ring remains command state; only Story 1.10's Offline pair bridges them, and neither owner is swapped,
**And** no command is queued, replayed, or retried on reconnect, no synthetic frame is emitted, and no socket write or write acknowledgement is presented as an on-TV effect.

**AC6 — Minimum generation guard and session lifetime**
**Given** a tap opens generation N and the tracker is bound to the current socket instance,
**When** evidence or a user confirmation for generation N arrives while N is current,
**Then** it may resolve N exactly once,
**When** a newer tap opens generation N+1,
**Then** any `TvEvent`, `StateProbe`, or pending "It worked" action for N is ignored and cannot change the N+1 ring; a stale "It worked" cannot confirm a newer tap,
**And** the generation is a monotonic `Long` per socket session; a new socket session starts a fresh generation and accepts no evidence from the old socket,
**And** the tracker is created with the channel session and cancelled with it; on socket close, session end, or a target change when Epic 2 adds one, the open generation resolves to `Idle` and cannot later become `Unconfirmed` or `Observed`,
**And** once a generation settles to `Idle` after Observed or is superseded, later evidence for it cannot reopen the ring or restart the 4s hold,
**And** this is the minimum single-tap guard only; full repeat, held-gesture, repeat-frame, and adversarial late-evidence coverage belongs to Story 1.13, and this story adds no repeat engine, press/release synthesis, or held-gesture timer.

**AC7 — State line, accessibility, RTL, and focus**
**Given** the D-pad, the ring, and the state line,
**When** they are rendered and used with TalkBack or in RTL,
**Then** every key is at least 48dp (directions 60×56, OK 70) and exposes a button role with the TalkBack names "Direction up, button," "Direction down, button," "Direction left, button," "Direction right, button," and "OK, button"; a disabled key reports disabled and has no send action,
**And** the ring has no content description, click action, or focus stop; the state line keeps Story 1.10's 24dp-minimum centered 13sp/500 row and 6dp lamp and is the polite live region; it announces one state per change: "Idle," "Sent," "Waiting for TV…," "TV confirmed," "You confirmed," or "Offline"; in the unconfirmed state it announces "Waiting for TV. The TV hasn't confirmed this key.",
**And** "It worked" is a 48dp button with the accessible name "It worked"; it is reachable in the state line only when Unconfirmed and is absent otherwise,
**And** color is never the sole carrier: every ring state has its text label, every key has a glyph or the word OK, and disabled adds disabled semantics to the 45% opacity,
**And** the banner remains assertive only for offline and identity changed, and polite for reconnecting and pairing required, per Story 1.10,
**And** focus order is topbar → banner actions → one-time chip (absent) → utility row (absent) → function keys (absent) → D-pad in row-major order Up, Left, OK, Right, Down → state line and its "It worked" action → launcher; absent rows are skipped and the D-pad does not trap focus,
**And** at 200% font scale and 1.3× expansion, "OK," "Sent," and "Waiting for TV…" remain legible; the state line wraps to two lines, "It worked" stays 48dp, and no critical label truncates or forces horizontal scrolling,
**And** in RTL the cluster is physically laid out, not start/end: Up stays top, Down bottom, Left left, Right right; the chevrons do not mirror; the Left key still sends Left; the OK label is unchanged; only surrounding text and sheet rows mirror; focus order and accessibility names do not swap direction,
**And** this story adds no haptics, no sound, and no new reduced-motion setting; the 900ms breath and 0.98 pressed scale are the only new motion, and Story 1.14 owns their reduced-motion audit and the full accessibility/RTL/200% pass.

**AC8 — Testability**
**Given** the tracker, the D-pad, and the state UI,
**When** the JVM, Compose, and manual suites run,
**Then** `:tv` JVM tests with a fake clock and fake channel cover: one `submit()` per tap; one Click frame per tap; zero for a disabled tap; OK maps to Enter; Submitted only after a write; 3s timeout to Unconfirmed; Unconfirmed remains while open; Observed from `TvEvent` and command-scoped `StateProbe`; Observed with `UserConfirmed` provenance from "It worked" with no extra submit; "It worked" absent outside Unconfirmed; 4s Observed hold to Idle; one breath per press; a new tap supersedes; stale evidence for an older generation is ignored; socket close resolves Idle; a new session starts Idle; no queue or retry,
**And** Compose/UI tests or manual pilot tests cover the 3×3 geometry and tokens, pressed and disabled states, ring state colors and text, state-line expansion, "It worked," the offline banner with disabled keys, TalkBack names/roles/live region, row-major focus order, RTL arrow non-mirroring, 200% font scale, and no horizontal scrolling,
**And** no test waits on real 3s or 4s timers, requires a live TV, asserts a socket write as success, or prints token, pin, SPKI, full MAC, or WSS URL,
**And** the tests reuse Story 1.10's session and health fakes and Story 1.7's connection fixtures; there is no second tracker, timer, or outcome implementation.

**AC9 — Scope discipline**
**Given** this story,
**When** it is implemented,
**Then** the only product changes are the D-pad inside the ring, the `CommandTracker` tap path, the five ring states, the state line's command labels and "It worked," and the D-pad's accessibility/RTL behavior,
**And** it adds no press-and-hold repeat, key-up/release, held-gesture timer, Back long-press, repeat-frame handling, or adversarial late-evidence coverage; those are Story 1.13, which adds the only repeat engine and the full generation-monotonicity coverage,
**And** it adds no utility row, function keys, power, Back/Home/Apps/Search, volume, mute, or play/pause; Story 1.12 reuses this tracker, ring, and state line for those keys,
**And** it adds no one-time unsupported-key chip, model-limit chip, power-off warning, or durable limitation record; Story 1.15 owns those,
**And** it adds no haptics, reduced-motion implementation, compact 208dp D-pad, or full accessibility audit; Story 1.14 owns those,
**And** it adds no text, pointer, voice, cast, mirror, wake, Settings, multi-TV/Add TV, target switching, session chip, launcher enablement, full TV details, trust/Forget, or matrix change,
**And** it adds no cloud, telemetry, remote config, new permission, foreground service, notification, wake lock, background socket, second session/health/target owner, second `CommandTracker`, second banner, toast, snackbar, dialog, command queue, retry, WSS-to-WS fallback, or trust-all manager,
**And** it reuses Story 1.10's Remote shell, single UI-scoped session, `ConnectionHealth`, offline/reconnecting/identity/pairing banners, ring placement, state line, and disabled launcher; Story 1.7's connection, backoff, and re-pair; and Story 1.5's pairing; no second implementation exists,
**And** holding a D-pad key in this story sends one tap and nothing more; the story does not partially implement Story 1.13, and no later story is required for the behavior this story claims.

### Story 1.12: Add the utility and function keys and keep power off honest

As the pilot user,
I want volume, mute, play/pause, Home, Back, Apps, Search, and a separate power-off key on the Remote,
So that I can use the everyday controls without the app pretending a socket write or a TV disconnect proves what happened.

**FRs:** FR9, FR11, FR13, FR24 (power-off subset; the one-time warning is Story 1.15); UX-DR3, UX-DR4, UX-DR5, UX-DR12, UX-DR13, UX-DR20, UX-DR27, UX-DR29, UX-DR39; NFR3, NFR8, NFR9, NFR12, NFR17; AD-2, AD-3.

**Out of scope:** D-pad direction/OK changes, D-pad repeat, key-up, volume repeat, Back long-press, late-evidence and generation work, wake/power-on/magic packet, the one-time power-off warning and model-limit chips (Story 1.15), haptics, Settings, launcher enablement, text, pointer, voice, cast, mirror, multi-TV/Add TV, session chip, any new sheet or dialog, command queue/retry/background socket, capability-label or matrix changes, and the full 200%/RTL/1.3×/reduced-motion/haptics audit (Story 1.14).

**Acceptance Criteria:**

**AC1 — Utility row geometry**
**Given** the Remote is open for the active TV and the socket is open,
**When** the utility row is laid out at a 360–480dp screen width,
**Then** it is a centered 48dp-high row with a 180×48dp volume rocker, a 48dp play/pause circle, and a 48dp power-off circle, with 14dp gaps and a 304dp total width,
**And** the rocker is one 24dp-radius capsule with three 60×48dp segments in the order volume up, mute, volume down, separated by 1dp hairlines that do not add to the 180dp width,
**And** each segment is its own hit target with the whole 60×48dp area tappable; the rocker has no swipe, drag, or repeat behavior,
**And** the volume glyphs are the custom 20dp speaker-plus, speaker-mute, and speaker-minus line icons; play/pause and power use their custom 20dp line icons,
**And** the row uses `surface-key` with 1dp hairlines and no mint fill; power uses `ink-primary` and a 1dp hairline and is never a filled red block or a ring color,
**And** the row fits inside the 320dp content width at a 360dp screen without clipping, wrapping, or horizontal scrolling, and stays centered at wider widths.

**AC2 — Function key row geometry**
**Given** the Remote is open,
**When** the function-key row is laid out below the utility row,
**Then** it is a 52dp-high row with four equal-width columns, 8dp gaps, `rounded.md` 14dp corners, `surface-key`, and a 1dp hairline,
**And** each key has an 18dp custom line icon above a 15sp/400 sentence-case label: Home, Back, Apps, Search; the icon-to-label gap is 4dp and the whole 52dp key is the target,
**And** no key is a pill, uses uppercase, uses mint fill, or uses a Material icon, emoji, unicode arrow, or text-as-icon,
**And** at 360dp the four columns share the 320dp content width; the labels are centered and are not truncated at 100% font scale, and under larger font scale the row may grow vertically rather than clip, with Story 1.14 verifying 200%,
**And** the row never wraps into two rows, never scrolls horizontally, and never overlaps the ring.

**AC3 — Remote order and unchanged D-pad and ring**
**Given** the Remote shell from Story 1.10 and the command-state UI from Story 1.11,
**When** the new rows are added,
**Then** the vertical order below the topbar and any banner is quiet space, utility row, 24dp gap, function-key row, 24dp gap, 236dp ring with the D-pad, 24dp gap, state line, launcher; when the banner is present it still pushes content and never overlays or reorders a row,
**And** the utility and function rows sit above the ring and outside it; they are never placed below the ring, between the ring and state line, or inside the D-pad,
**And** the ring, D-pad geometry, D-pad key behavior, and command-state behavior are unchanged from Stories 1.10–1.11; this story only inserts the two rows,
**And** all extra vertical space is absorbed above the utility row; the two rows consume the quiet region above the ring, and on a typical 800dp-tall phone with no banner the ring center stays within Story 1.10's 62–70% band without taking space from the fixed gap between the ring and state line,
**And** on a short viewport the quiet space collapses first and the ring stays 236dp in this story; Story 1.14 owns the compact 208dp ring and D-pad variant, which must keep every target at least 48dp,
**And** the Remote does not scroll; the utility and function rows do not wrap, shrink below their specified heights, or move under the launcher, status bar, or gesture inset,
**And** the launcher stays visible and disabled; the rows keep their order and heights when the health dot changes, and the banner's existing push behavior may move the group but never reorders or overlays a row.

**AC4 — One submit per tap**
**Given** the socket is open and `keysEnabled` is true,
**When** the user taps volume up, mute, volume down, play/pause, Home, Back, Apps, Search, or power off once,
**Then** the app calls the existing `submit(RemoteCommand)` exactly once for that gesture with that key's single command (`VolumeUp`, `Mute`, `VolumeDown`, `PlayPause`, `Home`, `Back`, `Apps`, `Search`, `PowerOff`) and writes no frame itself,
**And** a press, release, recomposition, or long hold does not add a submit; holding volume, play/pause, a function key, or power sends only the one command from the tap,
**And** there is no volume repeat, no D-pad repeat, no Back long-press, no key-up, and no synthesized press/release in this story; Story 1.13 adds repeat only where it owns it,
**And** play/pause is one ordinary command, not an app-side play/pause toggle: the app does not inspect or track playback state, and the internal adapter maps `PlayPause` to the single play/pause key frame validated in Spike A without synthesizing a `KEY_PLAY`/`KEY_PAUSE` sequence,
**And** the internal adapter maps each typed command to its protocol key; `:app` imports no `KEY_*` or frame type,
**And** every one of these keys remains visible and usable when `keysEnabled` is true, regardless of the capability label or matrix result; unsupported-key chips and limitation records are Story 1.15 and are not added here,
**And** Search sends the TV Search key and opens no phone text field; Home, Back, and Apps send their TV keys and open no sheet,
**And** tapping one rocker segment sends only that segment's command; the other segments do not fire.

**AC5 — Shared command truth**
**Given** Story 1.11's single `CommandTracker` and its ring and state-line UI,
**When** any new key is submitted,
**Then** the same tracker records one gesture and the same UI starts at Submitted: pale-mint ring and "Sent", with the single 900ms breath reused, regardless of the previous state,
**And** no evidence within the injected 3s timeout yields Unconfirmed: amber ring, "Waiting for TV…", and the existing 48dp "It worked" action; the timeout is configuration, not a new timer,
**And** machine evidence (`TvEvent` or `StateProbe`) yields Observed with "TV confirmed"; only an explicit `UserConfirmed` yields "You confirmed"; neither copy appears without its provenance,
**And** evidence for an older gesture never changes the current ring, the ring belongs to the last press across D-pad, utility, and function keys, and a new press resets the previous state,
**And** a socket close or target change resolves an open gesture to Idle under Story 1.11's rules; it is never rendered as Unconfirmed or Observed by the close,
**And** the new keys add no second tracker, second outcome flow, per-key status badge, success color, checkmark, confetti, "Success!", or state text outside the existing ring and state line,
**And** the state line keeps Story 1.11's 24dp minimum, 13sp/500 label, 6dp lamp, and 48dp "It worked" expansion; this story changes no state copy, timeout, or glow timing,
**And** a socket write alone never produces Observed, a green ring, or "TV confirmed"; the state line remains the polite live region.

**AC6 — Power off honesty**
**Given** the TV is on or its power state is Unknown and the socket is open,
**When** the utility row is composed,
**Then** power is a 48dp circle at the far right, visually separated from OK by the function-key row and ring gap, with a 1dp hairline and an `ink-primary` glyph; it never uses a ring color, filled red block, or mint fill, and it opens no confirmation sheet, dialog, snackbar, or long-press action,
**When** the user taps it once,
**Then** exactly one `submit(PowerOff)` is sent as a distinct command and control, not a combined on/off toggle and not a wake request; no second power frame, magic packet, wake operation, or retry follows,
**And** if no off-state evidence arrives, the command may remain Submitted then amber Unconfirmed after the 3s timeout, and the app does not call it failed or retry it,
**And** if a `TvEvent` or `StateProbe` indicates the TV is asleep or off for the current gesture, the existing tracker may show Observed with "TV confirmed"; a user tap on "It worked" may show "You confirmed"; neither is claimed from the write,
**And** if the socket closes, the open gesture resolves to Idle and the ring may show Offline through the existing health path; the app never turns that close, a failed probe, or a `clientDisconnect` into Observed, "TV confirmed", "TV off", or "Asleep",
**And** no new power or asleep state, surface, or guard is added; `ConnectionHealth` remains the only health and power-state owner, and if `keysEnabled` is false the power key is disabled and sends nothing,
**And** no confirmation sheet, dialog, snackbar, or one-time chip is added for power off; the one-time power-off warning and the durable no-wake limitation are Story 1.15, not a confirmation and not part of this story's claim.

**AC7 — Offline gating and no queue**
**Given** the socket is closed, terminal offline, reconnecting, identity changed, pairing required, or LAN denied, so `keysEnabled` is false,
**When** the user taps or holds any new key, or the socket closes during a tap,
**Then** all nine new keys remain visible but disabled: `ink-disabled`, 45% surface opacity, disabled semantics, no pressed command state, and no ring change on tap,
**And** a tap, long hold, or repeated tap calls `submit()` zero times and writes no frame; there is no queue, replay, retry, synthetic key-up, or deferred send when the socket reopens,
**And** the existing health/offline banner is reused unchanged; this story adds no second banner, toast, snackbar, inline error, spinner, or command state in the topbar,
**And** keys stay enabled while the socket is open but the TV is silent; the state line may show Unconfirmed, and the user can keep pressing,
**And** reconnect re-enables keys only when `ConnectionHealth` reports a usable connected socket; a `Reconnecting` state keeps them disabled,
**And** if the socket closes between the tap and dispatch, the tap is dropped, any open gesture resolves to Idle, and no command is sent into the closed socket,
**And** power off cannot be tapped into a closed socket and never becomes a wake attempt.

**AC8 — Accessibility names, roles, and focus**
**Given** TalkBack and directional focus navigation are active,
**When** the Remote is traversed by accessibility or directional focus,
**Then** each rocker segment is a separate focusable button, not one merged control, named "Volume up", "Mute", and "Volume down"; play/pause is "Play or pause"; the function keys are "Home", "Back", "Apps", and "Search"; power is "Power off" and is never named "Power" or announced as a toggle,
**And** each control has `Role.Button`; a visible label or the icon's own name supplies the accessible name, and icons are decorative when a visible label exists,
**And** disabled keys are announced as disabled through `enabled=false`, not color or opacity alone, and the utility row does not merge away its three segment names,
**And** no key uses toggle or checked semantics for mute, play/pause, or power; mute is a sent key, not a persistent switch, and the state line is the only command-outcome live region,
**And** every utility segment and circle is at least 48×48dp, every function key is at least 52dp high, and the focus indicator is the existing 2dp mint `focus-ring`,
**And** focus order is topbar → banner actions → one-time chip (Story 1.15; absent here) → utility row → function keys → D-pad → state line → launcher, with the visual start-to-end order inside each row and the ring not focusable,
**And** no key's content description includes "Sent", "TV confirmed", or any other command state,
**And** labels use externalized strings, 15sp/400, and sentence case, and function-key labels are not truncated at 100% font scale; the layout uses Start/End and flexible widths so Story 1.14 can verify RTL, 1.3× expansion, 200% font scale, reduced motion, and haptics,
**And** this story adds no haptic, sound, new animation, accessibility-only mode, or color-only cue.

**AC9 — Testability**
**Given** the new key surfaces and the reused tracker,
**When** the test suites and pilot checks run,
**Then** JVM tests with a fake `TvControl` and Story 1.11's `CommandTracker` test fixture assert each of the nine taps submits exactly one exact command; a long hold submits once; disabled, reconnecting, offline, identity-changed, pairing-required, and LAN-denied states submit zero; and reconnect submits nothing automatically,
**And** tests assert no repeat, no Back long-press, no volume repeat, no key-up, no second submit on recomposition, no local play/pause state, and no wake/power-on path,
**And** a fake adapter asserts `PlayPause`, `PowerOff`, and the volume and function commands map to exactly one protocol command each internally, with `:app` tests importing no `KEY_*` or frame type,
**And** a fake evidence source asserts Submitted → Unconfirmed and Submitted → Observed provenance; a socket close resolves Idle/Offline and never Observed; power off with no evidence may stay amber; and a disconnect never produces "TV confirmed",
**And** Compose/UI or manual pilot tests assert the exact utility geometry (180×48 rocker, three 60×48 segments, 48dp circles, 14dp gaps, 304dp total), function-row geometry (four equal columns, 52dp high, 8dp gaps, 18dp icons, 15sp labels), vertical order above the ring, no horizontal scrolling, disabled rendering, accessible names and roles, and focus order,
**And** no CI test requires a live TV or asserts a socket write as success; no test prints token, pin, SPKI, full MAC, IP, or URL; and the tests reuse Story 1.11's tracker fixtures and add no second test double for command truth.

**AC10 — Scope discipline and handoffs**
**Given** this story,
**When** its diff and visible surfaces are reviewed against the Epic 1 scope and adjacent stories,
**Then** it adds only the utility row, the function-key row, and their command wiring; it changes no D-pad direction or OK geometry or behavior and no ring or state-line behavior,
**And** it adds no D-pad repeat, key-up, volume repeat, Back long-press, or late-evidence handling; Story 1.13 owns those and extends the same Back and D-pad surfaces without a second key implementation,
**And** it adds no wake, power-on, magic packet, one-time chip, unsupported-key limitation record, haptics, Settings gear, launcher enablement, text, pointer, voice, cast, mirror, multi-TV, Add TV, session chip, sheet, dialog, command queue, background socket, foreground service, notification, or wake lock,
**And** it adds no second `CommandTracker`, `ConnectionHealth`, target or session owner, command vocabulary, protocol parser, `PrivatePeer`, or multicast lock; `:app` still imports no protocol type,
**And** it does not alter the matrix asset, capability labels, health precedence, or the existing banner copy; Story 1.14 owns the full 200%/RTL/1.3×/reduced-motion/focus/haptics audit,
**And** the one-time power-off warning, model-level key-limit chip, and durable limitation record are Story 1.15; this story adds no chip and does not replace the warning with a confirmation sheet or a false "TV off" claim,
**And** the visible changes are limited to the two rows and the behavior of their nine keys; the launcher remains disabled and no other surface changes.

### Story 1.13: Repeat D-pad holds and keep late evidence from rewriting the last press

As the pilot user,
I want a held direction to repeat predictably, Back to stay tap-only until this build can prove long-press support, and late TV confirmations to never rewrite my latest press,
So that holding a direction feels responsive, no gesture is hidden on a TV that cannot perform it, and the ring always tells the truth about the command I just sent.

**FRs:** FR9–FR11, FR13; UX-DR4, UX-DR28, UX-DR29, UX-DR38 (repeat + deliberate Back exception), UX-DR39; NFR7–NFR9, NFR12, NFR17; AD-1, AD-2, AD-3.

**Out of scope:** new key surfaces or layouts; utility-row, function-key, or power behavior; volume/play-pause repeat; OK repeat; long-press on Home, Apps, or Search; Back long-press emission (deliberately not enabled by AC6 because no canonical per-model support signal exists); a `Keys/LongBack` matrix key or any Story 1.8 vocabulary change; command-state visuals or ring composition; one-time chips and limitation records (Story 1.15); haptics and reduced-motion implementation (Story 1.14); Settings; launcher enablement; multi-TV UI or target-picker changes; text, pointer, voice, cast, mirror, wake; live TV or instrumentation; cloud, telemetry, or remote config.

**Acceptance Criteria:**

**AC1 — One repeat engine, with thresholds injected by `:app`**
**Given** the Remote is open on an established socket and Story 1.11's D-pad is visible,
**When** a direction is pressed and held,
**Then** exactly one repeat engine exists, in `:tv`; it owns the cadence, the repeat frame writes, coalescing, and the key-up path,
**And** `:app` supplies `RepeatConfig(initialDelayMs = 500, intervalMs = 80)` to `:tv` through the `TvControl` seam and emits only the press/release gesture edges; `:app` contains no repeat timer, loop, coroutine delay, or frame-level key writer,
**And** `:tv` contains no hard-coded `500` or `80` in the repeat path; a test injects different values and observes the injected cadence,
**And** the engine is started by the one `submit(RemoteCommand)` press edge for the gesture and stopped by the matching `release(RemoteCommand)` key-up edge; no `repeat()`, key-specific method, extra `press()` call, or second public operation is added outside AD-2's fixed `TvControl` operation set,
**And** only the four D-pad directions are repeatable; OK, Back, Home, Apps, Search, volume up/down/mute, play/pause, and power off never enter the engine,
**And** the D-pad uses one pointer press/release detector; no `combinedClickable`, `onLongClick`, or second repeat path synthesizes frames,
**And** the engine is keyed to the socket instance and is cancelled with that socket; no repeat engine, timer, or tracker survives a socket close or target change.

**AC2 — 500ms threshold, 80ms cadence, and no catch-up burst**
**Given** a direction press at `t = 0` on a fake clock and an established socket,
**When** the hold is advanced,
**Then** the initial key frame is Story 1.11's documented `Click` for that direction at `t = 0`,
**And** no repeat is sent at `t = 499ms`; a release at `t = 500ms` wins the tie and sends no repeat; a release at `t = 501ms` has sent exactly one repeat at `t = 500ms`,
**And** while the hold continues, repeats occur at `t = 500ms, 580ms, 660ms, 740ms, ...`, one `Click` frame per tick with no drift and no skipped or duplicated tick,
**And** the 500ms/80ms values are the product thresholds validated in Spike A's repeat run; they are UX configuration, not a protocol guarantee, and no copy or code claims the TV itself repeats,
**And** a release at an exact repeat timestamp suppresses that tick; a release one millisecond later retains it,
**And** if a frame write is delayed or the socket backpressures, at most one repeat is coalesced and sent when the writer resumes; missed ticks are not replayed as a burst, and the next repeat is scheduled one interval after the last sent repeat,
**And** the repeat cadence never calls `submit()`, never starts a second tracker timeout, and never resets the 3s command timeout; the timeout remains measured from the initial submit.

**AC3 — Release, cancellation, and close mid-hold**
**Given** a direction hold that has entered repeat,
**When** the pointer is released while the socket is open,
**Then** the repeat engine stops before or at the release, and exactly one `Release` frame for the same `KEY_*` is written; no trailing `Click` is written after the release, and no second `submit()` occurs,
**And** a release that arrives after the engine has already stopped is idempotent: it writes at most one `Release` and never re-arms the engine,
**And** when the pointer is cancelled or capture is lost, the same release path runs for a repeat-entered hold, the gesture settles to `Idle` if it has no evidence, and no new command is synthesized,
**And** when the socket closes mid-hold, the repeat stops immediately, no `Release` is queued or written into the closed socket, no command is retried on reconnect, and the open gesture resolves to `Idle` under AC8,
**And** the release frame is one documented `Release` for the same `KEY_*`, `Option:"false"`, and `TypeOfRemote:"SendRemoteKey"`; the initial press and repeats remain the Spike A-validated `Click` pattern, and no `Press` frame is invented,
**And** the one release-edge exception is the short tap in AC5: a hold that never enters repeat writes no `Release` and remains Story 1.11's single `Click`.

**AC4 — One tracked gesture; repeats never submit or restart the ring**
**Given** a direction press that repeats ten times on one socket,
**When** the repeats are observed by Story 1.11's `CommandTracker` and ring,
**Then** `submit()` is called exactly once, one gesture generation is created, and the tracker emits one `Submitted` outcome,
**And** no repeat frame calls `submit()`, creates a generation, emits another `Submitted`, or replays the 900ms ring breath, the submitted color transition, or the 80ms pressed-key scale,
**And** the ring belongs to the initial press for the whole hold: the 3s timeout runs once from the initial submit, and a hold longer than 3s produces one `Unconfirmed` transition while unresolved, not a new `Submitted` on each repeat,
**And** once a generation resolves to `Observed`, further evidence for that same generation, including evidence caused by later repeat frames, is ignored; the observed glow starts once, holds its existing duration, and does not re-enter or re-trigger on each repeat,
**And** if the generation is `Unconfirmed`, the first qualifying evidence for that same generation may resolve it to `Observed` once, per UX-DR28; later evidence for that generation is ignored,
**And** the key remains visually pressed for the hold and does not strobe between pressed and resting states on each repeat,
**And** only a new user press ends the old generation and starts a new `Submitted`/breath.

**AC5 — Direction-only holds, short-tap parity, and second pointer**
**Given** a direction press shorter than 500ms,
**When** the pointer is released,
**Then** exactly one initial `Click` and zero repeats are sent, no `Release` is sent, and the gesture is tracked once; this is the same frame behavior as Story 1.11's tap,
**And** a press of OK, Back, Home, Apps, Search, volume up/down/mute, play/pause, or power off never arms the repeat engine and never produces hold frames; those keys keep their Story 1.11/1.12 tap behavior,
**And** while one direction is held, a second pointer or a second direction does not start a second hold, submit a second command, or create a second generation; the second pointer is ignored until the first gesture ends,
**And** no diagonal key, swipe, double-tap, multi-finger gesture, or D-pad-to-trackpad behavior is added,
**And** when keys are disabled because the socket is closed, no direction hold or repeat can start.

**AC6 — Back long-press is deliberately not enabled at 1.2s**
**Given** the Back key from Story 1.12 and any TV in the Epic 1 matrix,
**When** the user taps Back,
**Then** exactly one normal `KEY_RETURN` click is sent, unchanged from Story 1.12,
**And** when the user holds Back for 1.2s or longer, no 1.2s long-press timer is armed, no `Press`/`Release` pair is sent, no long-press command or extra frame is emitted, no haptic marks the threshold, and no command-state transition is created; the hold remains the same single Back tap,
**And** the Back key stays visible, enabled, and usable while the socket is open; it is not disabled, hidden, or given a new label, chip, or settings toggle,
**And** this is a deliberate UX-DR38 exception, not an implementation gap: the protocol reference documents `Press`/`Release` and a repeat loop, but no canonical source defines a per-model long-Back support signal, no `Keys/LongBack` key or matrix column exists, and `Keys/Device = Works` is evidence for ordinary keys, not for a long press,
**And** no `Keys/LongBack` capability, matrix row, probe, or new `CapabilityScope` is invented; Story 1.8's closed vocabulary is unchanged,
**And** fake-clock tests at 1199ms, 1200ms, and 1201ms, each followed by a release, assert the total frame count and tracker state match a short Back tap and contain no `Press` or `Release` frame,
**And** a future story may add the gesture only after a recorded support signal exists; it must then send the validated hold frames and exactly one `submit()` for the gesture,
**And** the Epic 1 coverage notes record this as the only deliberate UX-DR38 exception, with the condition for enabling it later.

**AC7 — Monotonic generations bound to the socket instance**
**Given** Story 1.11's single `CommandTracker` for the target TV,
**When** the tracker is created for a socket session,
**Then** each `submit()` assigns a monotonically increasing `Long` generation for that socket instance, and the generation is bound to that socket instance's identity,
**And** repeat frames carry the existing generation and never increment it; a new generation is created only by a new user-expressed command,
**And** every `Evidence` item is tagged with the `GestureToken(socketInstanceId, generation)` it belongs to before it reaches the tracker; `GestureToken` stays internal to `:tv` and does not widen the public API,
**And** an evidence item that cannot be attributed to a specific generation is not guessed into the current one; the adapter drops it as uncorrelated rather than risk confirming a newer press,
**And** the tracker ignores evidence with a different socket instance or an older generation, and never lets that evidence change the current outcome, provenance, ring, or state line,
**And** evidence for the current generation is accepted only while that generation is active; evidence arriving after the generation has settled to `Observed`/`Idle`, or after a newer generation has started, is ignored,
**And** a late `UserConfirmed` for an older generation is ignored exactly like a late `TvEvent` or `StateProbe`,
**And** two presses of the same key are distinguished by generation, not by key name; a late confirmation for the older press can never resolve the newer press,
**And** evidence for the current generation arriving after the 3s `Unconfirmed` state may resolve it to `Observed` once, as UX-DR28 requires; this is not treated as stale.

**AC8 — Socket close and target change resolve the open gesture to `Idle`**
**Given** an open gesture on a socket,
**When** the socket closes, the Remote leaves the foreground, or the active target changes through Story 1.9's `TargetSession`,
**Then** the repeat engine and the tracker timeout are cancelled together with the socket, and the open generation resolves to `Idle` exactly once, never `Unconfirmed` or `Observed`,
**And** no `Release` is written into a closed socket, no command is queued, replayed, or retried on reconnect, and no evidence from the old socket is accepted,
**And** a new socket gets a new socket-instance identity and a new tracker; the generation counter may start again for the new instance, but evidence carrying the old instance identity is ignored even when its numeric generation matches,
**And** a target change closes the old socket before the new target's tracker exists; no two repeat engines or trackers overlap, and no in-flight command is re-targeted,
**And** the Remote may still show Story 1.10's Offline ring for connection health, but that is a connection presentation, not a command outcome; the tracker's state is `Idle`,
**And** returning to the Remote or relaunching after process death starts with no cached generation, no cached outcome, and no repeat state.

**AC9 — Accessibility and no new visual or haptic noise**
**Given** TalkBack, 200% font scale, RTL, and reduced-motion settings,
**When** a direction is tapped, held, or repeated on the Remote,
**Then** every direction keeps its existing 56dp target, accessible name, and button role; a TalkBack double-tap sends one key and does not require a long press,
**And** the repeat gesture adds no new accessibility action, focusable element, content description, or custom announcement; the state line announces only outcome changes, not each repeat, and the ring remains non-focusable,
**And** the key's pressed tone and scale are not replayed per repeat; the key stays pressed until release, and no new animation, sound, or haptic is added,
**And** Back's scoped-out long-press adds no label, hint, chip, or spoken instruction,
**And** the existing 200% font-wrap, RTL physical-glyph, and no-horizontal-scrolling behavior is unchanged because this story adds no text, layout, or icon,
**And** Story 1.14 remains the owner of the haptic policy, reduced-motion behavior, and the cross-surface accessibility audit; this story adds nothing for it to retrofit.

**AC10 — Deterministic tests and scope discipline**
**Given** the repeat engine, tracker generation, and Back decision,
**When** the test suite runs without a TV,
**Then** `:tv` and `:app` JVM tests use a fake clock and virtual coroutine time and cover: 499/500/501ms, the 500/580/660ms cadence, release at an exact tick, no catch-up burst, one `submit()` for ten repeats, one `Submitted` and one breath, no ring restart when repeat evidence arrives, settled-generation evidence ignored, late evidence for an older generation, uncorrelated late evidence dropped, same-key presses, socket close mid-hold, socket restart, target change, Back at 1199/1200/1201ms, a short tap with no `Release`, a second pointer, and a non-direction key,
**And** a protocol fixture asserts the exact `Click` and `Release` JSON for one direction, the unchanged `DataOfCmd`, and that `KEY_*` never appears in `:app`,
**And** an injected `RepeatConfig` with values other than 500/80 changes the cadence, proving `:app` owns the thresholds and `:tv` owns the engine,
**And** no test uses `Thread.sleep`, a live TV, instrumentation, or asserts a socket write as success,
**And** `./gradlew build lint :core:test` and the relevant `:tv` JVM tests pass; CI still needs no TV,
**And** this story adds no new key surface, layout, utility/function/power behavior, command-state visual, one-time chip, haptic, Settings, launcher enablement, multi-TV behavior, text/pointer/voice/cast/mirror/wake work, public API outside AD-2's allowlist, second repeat engine, second tracker, or invented matrix key,
**And** it reuses Story 1.11's tracker, `submit()`, ring, and state line, Story 1.12's Back/utility/function keys, and Story 1.10's socket lifecycle; it does not duplicate any of them.

### Story 1.14: Complete the accessibility floor across the Epic 1 surfaces

As the pilot user,
I want the complete Epic 1 experience to work with my font size, screen reader, reading direction, motion preference, and haptic setting,
So that I can pair, press every key, read every state, and recover from every failure without relying on sight, color, animation, or a particular language direction.

**FRs:** FR9–FR13, FR24 (power-off copy); UX-DR1–UX-DR15, UX-DR19–UX-DR20, UX-DR38–UX-DR41, UX-DR43; NFR1, NFR3, NFR9, NFR10, NFR12, NFR16, NFR17; AD-2, AD-3, AD-4.

**Out of scope:** Settings and the independent app-level haptics and reduced-motion toggles (Epic 2); multi-TV, Add a TV, cold-start choice, and launcher enablement; text, pointer, voice (including the transcript and mic-state clauses of UX-DR39), cast, mirror, wake, session chip, foreground service, and notifications; new keys, commands, protocol frames, one-time chips (Story 1.15), copy beyond accessibility labels that restate existing visible copy, retry/queue/backoff, target rules, capability rows, or state owners; tablet or landscape layouts, a second accessibility mode, an accessibility service, custom gestures, sound, new animation, new dependencies, shipped translations, or a live-TV requirement.

**Acceptance Criteria:**

**AC1 — Audit coverage and evidence**
**Given** the Epic 1 build with the surfaces from Stories 1.1–1.13,
**When** the accessibility audit runs,
**Then** it covers the no-TV setup, Add TV/discovery (scanning, results, empty, denied, manual IP), Pairing (prompt, deny, timeout), paired summary, TV details/Forget, TV switcher, Remote (topbar, banner, utility row, function keys, D-pad, state line, launcher), and the re-pair flow,
**And** it runs at 100% and 200% font scale, with a 1.3× string-expansion pseudo-locale, in LTR and RTL, with reduced motion on, and with system haptics on and off,
**And** each finding is fixed here or recorded at `_bmad-output/implementation-artifacts/a11y-audit-epic1.md` with the surface, configuration, exact control or copy, severity, and disposition; no unrecorded "known issue" remains,
**And** the record names the device/API level, TalkBack version, font scale, locale, animator scale, haptic setting, and test fixture used; it contains no token, pin, SPKI, full MAC, IP, or WSS URL,
**And** the debug-only matrix harness is spot-checked for labels and focus but is not counted as one of the product surfaces,
**And** Story 1.15's one-time limit chip is not part of this audit; Story 1.15 adds it with the same accessibility contract and updates this record,
**And** no finding is closed by adding a feature, key, screen, Settings surface, or accessibility-only mode.

**AC2 — TalkBack names, roles, and live regions**
**Given** TalkBack is enabled,
**When** the user explores the Epic 1 surfaces,
**Then** every remote key exposes a name matching its visible label (or its key name when icon-only), the button role, and a "Send" action label so TalkBack says "Double tap to send"; icon-only utility keys are named "Volume up," "Mute," "Play or pause," and "Power off," and direction keys read "Direction up, button," "Direction down, button," "Direction left, button," and "Direction right, button," with OK read as "OK, button,"
**And** disabled keys remain in the accessibility tree with a disabled state and a reason ("TV not connected"); disabled launcher slots remain focusable and announce their label and "Not available in this build"; no explanation sheet is added in Epic 1 because Story 1.10 keeps every launcher slot non-opening, and the full explanation belongs to launcher enablement,
**And** the topbar switcher reads "[TV name], connected/reconnecting/offline/not connected. Double tap to switch TV"; the health dot is merged into that name and is not a separate stop; a session-only name is static and names health,
**And** the ring has no semantics and is not focusable; the state line is the spoken state and a polite live region; the banner is assertive only for terminal offline and identity changed and polite for reconnecting, pairing required, and LAN denied,
**And** the state line announces a changed state once per change, not on recomposition, rotation, or focus change; the banner announces once per appearance; the ring, health dot, lamps, keys, launcher slots, and sheet grabber are not live regions,
**And** sheet titles are headings and pane titles; the current switcher row exposes "Current" as text and a selected state; decorative icons are cleared so "Current" plus the check glyph is not read twice; the "It worked" action is a 48dp button with its own name,
**And** the manual-IP field has a programmatic label, hint, and associated error; the fingerprint is exposed as grouped text, not an image; sheet actions name their object ("TV details for [TV]") and Forget is announced as a destructive button, not by color.

**AC3 — Focus order and sheet focus**
**Given** a hardware keyboard, D-pad/keyboard navigation, or TalkBack,
**When** focus moves through the Remote,
**Then** the order is topbar/switcher → banner actions when present → utility row → function keys → D-pad → state line (and "It worked" when shown) → launcher; there is no one-time chip in this story,
**And** within the D-pad, focus follows spatial order Up → Left → OK → Right → Down and does not reverse in RTL; within a mirrored row, traversal follows visual start-to-end,
**And** the 2dp focus ring is visible on every focused key and action, is not clipped by the ring, scrim, or insets, and the focused item exposes its focused state,
**And** a sheet moves focus to its title, traps focus inside until dismissed, and returns it to the trigger on drag-down, scrim, or back; if the trigger is gone (for example, the TV was forgotten), focus goes to the Remote switcher or the no-TV primary action, never nowhere,
**And** the background Remote is not focusable or announced while a sheet is open; one sheet at a time; the grabber and scrim are not focus stops,
**And** a TV-details Forget confirmation moves focus to the confirmation heading and returns it to "Forget TV" on Cancel; a permission explainer starts at its title and returns to the action that opened it,
**And** disabled keys and launcher slots remain in traversal so their state is reachable, but they send nothing and fire no command haptic.

**AC4 — 200% font scale, 1.3× expansion, and the short viewport**
**Given** font scale 200% on the 360×800dp reference phone, a 1.3× string-expansion pseudo-locale, and the 360×640dp stress viewport,
**When** the Epic 1 surfaces render,
**Then** the launcher becomes a 3×2 grid with the same six slots and fixed order; function-key labels wrap to two lines and the row grows; the state line wraps to two lines and the hero shifts upward; the ring stays 236dp until the quiet space collapses, then may shrink to 208dp,
**And** at 208dp the D-pad uses a compact 56/70/56 column and 52/70/52 row grid with 4dp gaps; directions stay at least 56dp wide and 52dp high, OK stays 70dp, chevrons and labels are unchanged, and no target drops below 48dp,
**And** the 360×800dp reference is the release gate; the 360×640dp viewport at 200% is a stress check. If the full stack cannot fit without clipping, the story records a named finding and a design follow-up; it never clips, overlaps, pushes a control under the gesture bar, or silently adds horizontal scrolling,
**And** where a fixed 100% height would clip copy (topbar title, function row, banner, launcher), the component grows rather than ellipsizing; the 56dp topbar, 52dp function row, 84dp launcher, and two-line banner limits are 100% values, not truncation rules at 200%,
**And** sheets scroll vertically at 200%, keep their title and primary action reachable, never scroll horizontally, and the manual-IP field scrolls above the IME with its error visible,
**And** no critical label truncates: key labels, function labels, launcher labels, state words, banner copy, button labels, sheet titles, capability labels, and Forget confirmation copy wrap or grow; only the topbar TV name and non-critical metadata may ellipsize, and the full name stays in the switcher and TV details; the SPKI fingerprint wraps or scrolls inside its code block,
**And** at 1.3× expansion no button, row, or state line clips or ellipsizes critical copy; state-line numerals stay tabular and the state word never truncates,
**And** there is no new tablet or landscape layout: landscape uses the same centered 480dp column with the launcher at the bottom and the same compression rules.

**AC5 — RTL layout and physical glyphs**
**Given** the phone is in RTL using the `ar-XB` pseudo-locale or forced RTL,
**When** the Epic 1 surfaces render,
**Then** `android:supportsRtl="true"` is set and the app does not force LTR globally; text alignment, start/end padding, topbar switcher order, list and card rows, sheet headers, banners, button rows, and the six-slot launcher mirror, while the logical order of slots and keys is unchanged,
**And** the TV-switcher caret and the topbar Back affordance mirror to the reading direction; the health dot, TV name, ring, and state line are not flipped or transformed; no text or icon uses `scaleX(-1)`,
**And** the D-pad keeps its physical 3×3 geometry and chevron orientation (Left stays left, Right stays right, Up stays up, Down stays down); the D-pad cluster is pinned to LTR for layout while its labels remain localized,
**And** volume up/mute/down, play/pause, power, and media transport glyphs do not mirror; their rows mirror only as rows, and no glyph is drawn with a negative scale,
**And** focus traversal follows visual start-to-end in mirrored rows while the group order stays topbar → banner → utility → function → D-pad → state line → launcher,
**And** IP, masked MAC, fingerprint, and other code values stay LTR with bidi isolation; dates and times use the locale; strings are externalized with no concatenated sentences; 1.3× expansion still fits,
**And** no text, control, caret, or sheet clips horizontally at 200% or 1.3×; no hardcoded left/right padding breaks RTL; the switcher caret remains visible next to a truncated TV name.

**AC6 — Reduced motion**
**Given** the system animator duration scale is 0 (Remove animations / reduced motion),
**When** the user presses keys and opens sheets,
**Then** the ring has no 900ms breath, no glow pulse, and no fade; it changes color and static glow instantly, and the Observed 4s hold ends with an instant return to Idle,
**And** pressed keys change tone but do not scale to 0.98; the 80ms scale is absent,
**And** the health dot does not pulse during reconnect; its amber color and label are unchanged,
**And** the sheet transition is a short fade with no slide, spring, or scale; the scrim does not slide; one sheet at a time,
**And** no looping animation exists: no `rememberInfiniteTransition`, `infiniteRepeatable`, autoplay, parallax, shimmer, or spinner; under reduced motion there is no reconnect pulse at all, and in the normal-motion path any reconnect pulse is a single pulse on entering Reconnecting and never loops,
**And** reduced motion changes no state, timing threshold, copy, or haptic; Submitted, Unconfirmed, Observed, and Offline keep their 3s/4s semantics and the state line still names them,
**And** changing the system setting while the app is open takes effect without a restart and does not replay an old animation; the motion policy exposes one reduced-motion input, wired to the system setting in Epic 1 and available to Epic 2's preference without a second animation path.

**AC7 — Haptics**
**Given** the system haptic feedback setting is on and the device has a vibrator,
**When** the user performs a remote key gesture,
**Then** exactly one 8ms tick fires at gesture start for that key, including D-pad, utility, function, and power keys; no tick fires on key-up, a repeat frame, or a synthesized press/release, so a 2s held direction is still one tick,
**And** when the current gesture reaches Observed (TV confirmed or You confirmed), exactly two 8ms ticks fire once with one gap constant defined in the pattern table; when it reaches Unconfirmed, one 12ms pulse fires once; when the ring enters Offline, one 20ms pulse fires once; Submitted, Idle, and Reconnecting produce no outcome pattern, and disabled-key taps do not repeat the Offline pulse,
**And** only the ring's current gesture produces an outcome pattern; stale evidence, an older generation, or an ignored late event is silent,
**And** the "It worked" action adds no separate press tick on top of the Observed pattern,
**And** Back long-press is not enabled in Epic 1 (Story 1.13), so there is no 1.2s threshold and no threshold haptic,
**And** if the system haptic setting is off, the device has no vibrator, or haptics are otherwise unavailable, no haptic is emitted, no error or toast appears, and the app remains fully usable; haptics never replace the ring, state line, or banner,
**And** no app-level haptics toggle or Settings surface is added; Epic 1 reads the system setting only and the haptics owner exposes one `enabled` input and one pattern table so Epic 2 can add its independent toggle without changing call sites,
**And** a socket write never produces a success haptic; power off gets the key tick and, if unobserved, the 12ms Unconfirmed pulse, never an Observed pattern without evidence; no sound is used,
**And** this story adds the normal install-time `VIBRATE` permission and updates NFR16's permission list in the same change; it is install-time only, adds no runtime prompt or data access, and all vibration calls go through the one haptics owner.

**AC8 — Targets, contrast, and color-not-alone**
**Given** the audited surfaces,
**When** targets, tokens, and state cues are measured,
**Then** every touch target is at least 48dp: direction keys at least 56dp wide and 52dp high (60×56dp standard, 56×52dp compact), OK 70dp, utility segments 60×48dp, function keys 52dp high, launcher slots 52×60dp, topbar 48dp, and "It worked" 48dp; targets do not overlap and the 4dp D-pad structural gap is included in each direction key's effective target,
**And** the DESIGN.md measured pairs hold: `ink-primary` on `surface-base` 15.45:1 (AAA), `ink-secondary` 6.85:1 (AA), `accent` 10.17:1, `ink-on-accent` on `accent` 10.14:1 and on `accent-pressed` 6.99:1, `ink-disabled` on `surface-key` 4.50:1, `banner-action` 8.30:1, `banner-ink` on `banner-surface` 12.67:1, `focus-ring` on `surface-key` 9.39:1, and observed/unconfirmed/offline rings 10.17:1/11.94:1/7.06:1; `ring-idle` and hairlines remain 1.56:1 exceptions only because the state line and labels name the state,
**And** color is never alone: ring + state word, health dot + accessibility label, capability text plate, disabled slots dashed + lock + label, current row "Current" + check, errors text + message, focus 2dp ring,
**And** disabled keys and slots keep a readable label and a programmatic disabled state; disabled styling is never the only signal, and informational text (state line, banner, capability, errors) never uses `ink-disabled` or reduced opacity,
**And** the specified 45% disabled-key treatment is an inactive-control affordance, not information: the key name, disabled state, and offline reason remain available in text and speech, and no enabled or informational text is dimmed,
**And** no new color, gradient, shadow, blur, or token is introduced; the focus ring is visible on every surface and is not obscured by the scrim, insets, or the ring,
**And** all target and contrast checks pass at 200%, 1.3×, and RTL; no target is shrunk to make copy fit.

**AC9 — State truth and no feature drift**
**Given** the accessibility, motion, and haptic changes,
**When** the Remote reports a command or connection state,
**Then** every announcement, ring state, and haptic is derived from the existing `CommandOutcome`, `CommandTracker`, `ConnectionHealth`, and ring/health state; no second state owner, tracker, timer, or stored preference is added,
**And** a socket write never produces Observed, "TV confirmed," a success haptic, or an assertive success announcement; Submitted stays Submitted until evidence or the injected timeout, and Unconfirmed is not presented as failure,
**And** on socket close or target change an open gesture resolves to Idle as before; it never becomes Unconfirmed or Observed, and late evidence or an older generation produces no haptic, no announcement, and no ring change,
**And** the health dot remains connection health and the ring remains command state; neither is announced, colored, or haptically signaled as the other, and the fixed precedence for identity changed, pairing required, offline, reconnecting, asleep, and awake is unchanged,
**And** no key, command, protocol frame, queue, retry, backoff, target rule, capability row, trust behavior, or reconnect policy changes; keys stay enabled while the socket is open but silent and disabled when it is closed,
**And** no new product surface, Settings, haptics or reduced-motion toggle, permission beyond the normal `VIBRATE` above, multi-TV, Add a TV, launcher enablement, text, pointer, voice, cast, mirror, wake, one-time chip (Story 1.15), or accessibility-only mode is added,
**And** no new copy is introduced beyond accessibility labels that restate existing visible copy; no success, capability, or support claim changes.

**AC10 — Testability and scope discipline**
**Given** the implementation,
**When** the story is verified,
**Then** automated tests cover semantics names/roles/disabled states, ring not focusable, live-region modes, focus order and sheet trap/return, target bounds, contrast ratios from tokens, 200%/1.3× layout, RTL glyph and layout rules, reduced-motion policy, and haptics policy with a fake effect sink (one gesture tick, zero repeat ticks, two Observed pulses, one 12ms Unconfirmed, one 20ms Offline, silent when disabled),
**And** Compose/UI tests use the existing fake `CommandTracker`/`ConnectionHealth` fixtures from Stories 1.11–1.13 and require no live TV; no test asserts a socket write as success or prints token, pin, SPKI, full MAC, IP, or WSS URL,
**And** manual checks on a physical phone cover TalkBack, 200% font scale, 1.3× pseudo-locale, RTL, reduced motion, and system haptics on/off across the AC1 surfaces; the audit record is committed at `_bmad-output/implementation-artifacts/a11y-audit-epic1.md`,
**And** source checks reject user-visible hardcoded strings, infinite animation APIs, direct vibration or `performHapticFeedback` calls outside the one haptics owner, sound APIs, and hardcoded left/right padding in RTL-sensitive components,
**And** `./gradlew build lint :core:test` passes; the story adds no tablet or landscape layout, no new dependency, no Settings, no new feature, and no protocol or state-model change,
**And** any finding that cannot be fixed here is a named exception with an owner and a next action, not a silent acceptance.

### Story 1.15: Explain a model's limits once and keep the record honest

As the pilot user,
I want one clear explanation when this TV can't confirm keys or can't be woken, and a durable record in TV details,
So that I don't mistake a model limitation for a failed key and I don't see the same warning every time.

**FRs:** FR12, FR24 (power-off warning and durable no-wake record), FR38 (chip-dismissal persistence subset); UX-DR3, UX-DR4, UX-DR5, UX-DR9, UX-DR19, UX-DR27, UX-DR29, UX-DR35 (TV details subset), UX-DR39, UX-DR43; NFR2, NFR3, NFR9, NFR12, NFR17; AD-3, AD-4.

**Out of scope:** new keys, commands, `CommandTracker`, ring, repeat, or key layouts; utility/function/power behavior; Settings and the app-level haptics/reduced-motion toggles; full TV details/four capability labels (Epic 2); multi-TV/Add TV; launcher enablement or disabled-slot explanation sheets; text, pointer, voice, cast, mirror, wake; per-key capability probes or matrix rows; Back long-press; haptics; new permissions; protocol frames; command queue/retry/background socket; confirmation sheets; power toggle; a new asleep or power state; cloud/telemetry.

**Acceptance Criteria:**

**AC1 — One-time chip component and placement**
**Given** the Remote is open for a durable target and Story 1.8's `CapabilityLabelTracker` or the power-off limitation applies,
**When** a one-time explanation is due,
**Then** at most one chip is shown below the topbar and below any banner, above the utility row; it is a 48dp-minimum raised strip with a 1dp hairline, 14dp radius, 14dp padding, 14sp secondary text, an 18dp info glyph, and a 48dp close target, and it pushes content rather than overlaying it,
**And** the key-limit chip appears only when `Keys/Device` is `BestEffort` or `NotSupported` and says "This TV can't confirm keys — they may still work."; it makes no per-key claim because Epic 1's matrix exposes a device-level key result, not per-key support,
**And** no key-limit chip appears when `Keys/Device` is `Works` or `NotTested`; the app does not invent a limitation from silence,
**And** the power-off warning appears before the first power-off on a durable target: "Power off only. This build can't wake the TV; use the TV's power button."; it is one-time, never a confirmation, and never blocks or intercepts the power key,
**And** when both limits apply, the power warning is shown first; after it is dismissed, the key-limit chip may appear; only one is visible at a time, and both remain recorded in TV details,
**And** the chip never calls `submit()`, writes a frame, opens a sheet, or changes the ring, state line, health, or power state; the keys remain usable while it is shown.

**AC2 — One-time dismissal and persistence**
**Given** a chip is shown,
**When** the user taps its 48dp close,
**Then** it is dismissed for that TV and limit identity and does not return on recomposition, a new Remote session, or process restart,
**And** dismissals are stored in the existing Room persistence layer per FR38, keyed by `TvId`, limit id, firmware, and matrix/probe version where applicable; they are not held only in memory or in a second preferences store,
**And** a dismissal is re-evaluated only when the TV is re-paired, its firmware changes, the matrix or probe result for the key limit changes, or the build's wake capability changes; the power warning is not shown when the build can wake the TV, and a chip never returns merely because time passed,
**And** forgetting the TV deletes its chip dismissal rows with the TV record; a re-paired TV starts with the warning due again,
**And** the store has one owner, no duplicate chip records, and no unredacted secret or full MAC in its keys or values,
**And** a session-only target may show the power warning once per session but has no durable key-limit chip or TV details record because Story 1.8 exposes no durable capability label without a `TvId`; this limitation is stated, not hidden.

**AC3 — Durable limits in TV details**
**Given** Story 1.6's trust sheet for a durable TV,
**When** it is opened and a limit exists,
**Then** it shows a "Limits" section with the device-level key label ("Best-effort" or "Not supported") and the power-off note "Power off only; this build can't wake the TV."; no full four-label matrix or Settings surface is added,
**And** the section reads the same `CapabilityLabelTracker` as the Remote; it does not depend on whether the chip was dismissed, opens no socket, sends no command, and adds no second resolver, matrix row, or stored label,
**And** the durable record is the source of truth and the chip is only a nudge; dismissing the chip does not erase the limit, and tapping "It worked" does not change the capability label,
**And** when no limit exists, the section is absent; the app does not claim "all keys work" or "this TV is fully supported" from the absence of a chip,
**And** the trust sheet's existing identity, masked MAC, IP, SPKI fingerprint, Tizen version, and Forget action are unchanged; full TV details remain Epic 2.

**AC4 — Power-off offline copy**
**Given** the last user-expressed command was power off and the socket then closes into terminal offline,
**When** the existing Story 1.10 offline banner is shown,
**Then** it uses the power-aware copy "TV may be off. Use the TV's power button; this build can't wake it." with the existing "Retry" and "Not now" actions, within the two-line-plus-action limit,
**And** the Remote keeps one session-scoped `lastCommandWasPowerOff` flag, set only by the power-off submit and cleared by any other submit, reconnect, target change, or session end; it selects this copy and is not a command outcome, does not alter the tracker, and is never persisted,
**And** the copy says "may be off," never "TV off," "Asleep," or "Power off succeeded"; the socket close and health transition are not command evidence,
**And** the banner remains owned by `ConnectionHealth` and the existing offline state; this story adds no new health state, timer, retry policy, or power-state writer,
**And** the generic offline copy remains for every other disconnect; the power-aware copy appears only after a power-off gesture on the current target,
**And** reconnecting or a successful Retry clears the banner and restores the existing online state; the ring's command outcome is `Idle` after the close, not Observed, and no success haptic or announcement is produced.

**AC5 — Truth, no per-key claim, and no false success**
**Given** the chip, TV details record, and offline copy,
**When** the app explains a limit,
**Then** every statement is derived from the existing `CapabilityLabelTracker` or the build's known no-wake limitation; no new capability label, per-key probe, matrix key, or support claim is invented,
**And** the app never says "This TV doesn't report [key]" in Epic 1 because there is no per-key signal; the model-level copy is used instead, and the per-key version is reserved for a later story with recorded per-key evidence,
**And** a key remains visible and usable while its limit is explained; the app does not hide it, disable it, call it failed, or use a red error state,
**And** the power warning is a build limitation, not a claim about the TV model; it says the build cannot wake the TV and names the physical power button as the next action,
**And** the chip, record, and copy never turn a socket write, a disconnect, or the absence of evidence into success, "TV confirmed," or "TV off"; command truth remains Story 1.11's owner.

**AC6 — Accessibility**
**Given** TalkBack, 200% font scale, RTL, and reduced motion,
**When** the chip and TV details Limits section are used,
**Then** the chip body is announced once as a polite status when it appears and is not re-announced on recomposition, focus change, or rotation; the 48dp close button is named "Dismiss explanation" and is the only focus stop inside the chip,
**And** the chip text wraps to at most two lines at 100% and grows rather than clipping at 200%; the close target stays 48dp, the info glyph is decorative, and the layout uses Start/End so the close moves to the reading-direction end in RTL,
**And** focus order is topbar → banner actions → chip close → utility row → function keys → D-pad → state line → launcher; the chip does not trap focus and the underlying keys remain reachable,
**And** the TV details Limits section uses text labels, not color alone; rows read the limit and the next action, and the 1.14 focus, target, contrast, and live-region rules apply,
**And** the chip adds no haptic, sound, animation, or new color; reduced motion changes nothing because the chip does not animate,
**And** Story 1.15 updates `_bmad-output/implementation-artifacts/a11y-audit-epic1.md` with the chip and Limits section so the Epic 1 audit covers the final surface.

**AC7 — Testability**
**Given** the chip selection, dismissal store, TV details, and offline copy,
**When** the JVM, Compose, and manual suites run,
**Then** JVM tests cover: no chip for `Works`/`NotTested`; the model-level chip for `BestEffort`/`NotSupported`; no per-key copy; power warning due before the first power-off; power warning priority when both limits apply; one chip at a time; dismissal persistence and no return; re-evaluation on re-pair, firmware change, matrix/probe change, and build wake-capability change, with no power warning when the build can wake; delete on Forget; no label change from "It worked"; no tracker/command call,
**And** banner tests cover the power-aware copy only after a power-off plus offline, the generic copy for other disconnects, no "TV off"/"Asleep"/success claim, and the existing Retry/Not now behavior,
**And** Compose/UI tests cover the 48dp close, two-line layout, 200%/RTL growth, focus order, TalkBack announcement, TV details Limits section, and no horizontal scrolling,
**And** no test requires a live TV, asserts a socket write as success, or prints token, pin, SPKI, full MAC, IP, or WSS URL; the tests reuse Story 1.8's label fixtures, Story 1.6's trust-sheet fixtures, Story 1.10's health/banner fixtures, and Story 1.12's power key fixture.

**AC8 — Scope discipline**
**Given** this story,
**When** its diff and visible surfaces are reviewed,
**Then** the only product changes are the one-time chip, its dismissal persistence, the TV details Limits section, and the power-aware offline copy,
**And** it adds no key, command, tracker, ring state, repeat, power toggle, confirmation sheet, Settings, full TV details, four-label list, multi-TV, launcher enablement, disabled-slot explanation sheet, text, pointer, voice, cast, mirror, wake, Back long-press, haptic, new permission, protocol frame, command queue, retry, background socket, cloud, or telemetry,
**And** it adds no second `CapabilityLabelTracker`, resolver, matrix row, per-key probe, `ConnectionHealth`, or power-state writer, and only one new chip-dismissal store; it reuses Story 1.8's label owner, Story 1.6's trust sheet, Story 1.10's Remote/banner, and Story 1.12's power key,
**And** it does not change the existing command-truth, health precedence, target, or session rules; all keys remain visible and usable, and no limit explanation is presented as a failure,
**And** the per-key FR12 wording remains a recorded Epic 1 limitation: the current matrix is device-level, so the model-level chip is the honest implementation until per-key evidence exists,
**And** the power-off warning is informational, not a confirmation; tapping power still sends exactly one `PowerOff` through Story 1.12.
