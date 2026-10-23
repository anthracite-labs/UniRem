---
status: draft
stepsCompleted: [2]
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

NFR16: Permissions and foreground services — `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, `POST_NOTIFICATIONS`, `RECORD_AUDIO`, the local-network permission for API 37, and MediaProjection consent from an Activity; no storage permissions; Photo Picker only; FGS types cast `connectedDevice`, mirror `mediaProjection`, voice `microphone`; only user-started sessions.

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
- Permissions and FGS: `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, `POST_NOTIFICATIONS`, `RECORD_AUDIO` for the Voice sheet, the local-network permission for API 37, and MediaProjection consent from an Activity; no storage permissions; the Photo Picker is the only picker; FGS types cast `connectedDevice`, mirror `mediaProjection`, voice `microphone`; only for user-started sessions.
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

## Epic List

**Total:** 7 epics. **FR coverage:** 41 of 41 FRs assigned exactly once.

### Epic 1: Pair and press

The user can find a Samsung TV on the local network, complete the TV's Allow prompt, trust the connection, and use a working one-TV remote — with every key and power-off action honest about what the phone observed.

**FRs covered:** FR1-FR5, FR8-FR13, FR24, FR37-FR38, FR40-FR41.

**Implementation notes:**
- **Spike A gate first.** No scaffold or foundation work starts before the Spike A report. Exit evidence: at least three model years including one recent Tizen; Allow/Deny; token persistence and re-prompt; pin change; reconnect; target selection; repeat at 500/80ms; per-key machine evidence versus user-only; power-off observation; the no-remote Allow path; and two same-model TVs producing distinct `TvId`. If the gate cannot run, stop and re-plan.
- **Scope is the single-TV slice.** No multi-TV picker, no full TV details or Settings, and no wake. Full FR39 remains Epic 2.
- **Trust closure.** The one-TV switcher opens a minimal trust sheet: name, model, masked MAC, IP, SPKI fingerprint, Forget, and the copy "Pairing is stored encrypted on this phone; nothing is sent to the internet." The pairing card's Details link resolves here. This is the FR4/FR5 acceptance subset, not full FR39.
- **Power-off honesty.** Before the first power-off, show a one-time chip: "Power off turns the TV off. This build can't wake it — use the TV's power button or remote." No confirmation sheet. After power-off, the offline copy repeats the limitation.
- **Seams.** Ship all six launcher slots disabled behind one `SlotAvailability` state owner; later epics register availability policy only. Build the TV switcher as a list with one TV, not a single-TV special case. Keep `activeTarget` separate from any last-used value. Key the session owner by `TvId` from the start. No dead "Add a TV" action.
- **Inert data.** Persist the wake MAC, its provenance, `NetworkType`, and the reached interface even though wake ships in Epic 2. Store declared surfaces, probe results, chip dismissals, and the TV record; never store a resolved capability label.
- **Matrix denominator.** Pin the pilot TV pair and seed their matrix rows in Epic 1. Author the literal `*unknown*` row. Define overlay-to-release-asset promotion and `matrixVersion` review. Each epic exit commits its relevant rows and runs one release-asset-only dry run.
- **Identity and build.** Confirm the application ID/package base `com.unirem` and the Play reservation in the first scaffold work, before any signed build. Release smoke starts in Epic 1.
- **Permissions.** Epic 1's manifest has no `RECORD_AUDIO`, `POST_NOTIFICATIONS`, or MediaProjection; no voice, cast, mirror, media-server, or capture code.
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
