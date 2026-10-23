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

<!-- Story design follows in Step 3. -->
