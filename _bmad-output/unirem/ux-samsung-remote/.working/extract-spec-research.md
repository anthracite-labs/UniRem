# UX Extract — SPEC + Research (UniRem Samsung remote)

Extraction only — no recommendations, no invented content. Verbatim quotes in "quotes" preserve load-bearing wording.
**Source tags:** SPEC = spec-samsung-remote/spec-samsung-remote.md · VM = spec-samsung-remote/verification-matrix.md · PR = spec-samsung-remote/protocol-reference.md · RES = research-native-kotlin-samsung-remote/research-native-kotlin-samsung-remote.md (§ numbers).

---

## 1. UX-mandated behaviors

### Outcome model & no silent failures
- "Socket-write success is never presented as TV action; command outcomes are submitted/unconfirmed/observed." (SPEC §Constraints)
- CAP-1 success: "outcomes distinguish submitted/unconfirmed/observed, and unsupported keys report instead of failing silently." (SPEC §Capabilities CAP-1)
- "A successful socket write **is not** a confirmed TV action. Model command outcome as *submitted / unconfirmed / observed*." (RES §4.1)
- "report unsupported commands to the user instead of failing silently" (RES §4.1)
- Observed-effect probes define what "observed" means per feature: "Keys: on-TV response per key. Text: glyphs appear in the focused field. Pointer: cursor visibly moves, click activates. Wake: TV answers on 8001/8002. Cast: TV renders with responsive transport. Mirror: output visible on the receiver." (VM §Observed-effect probes; header: "socket-write success never counts")
- Beta pattern carried forward: "*pending / submitted / unconfirmed / observed* outcomes" as separate authority owners for control, cast, mirror. (RES §10)
- "If the phone sends but the TV or app ignores it, classify it as unsupported, not as a success." (RES §11.1 Spike B)

### Capability gating with plain-language explanations
- "No Samsung compatibility contract exists: every remote feature ships behind a provider with capability detection, and nothing claims support a physical test has not observed." (SPEC §Constraints)
- CAP-3 success: "Pointer visibly moves and click activates on models passing the cursor probe; the feature stays hidden elsewhere with a plain-language explanation." (SPEC §Capabilities CAP-3)
- "Disabled unless capability tests pass for that model. No 'universal touchpad' claim. Explain in the UI that pointer works only where the TV app supports it." (RES §4.4)
- "'universal' means gated per model, never blanket." (SPEC §Non-goals)
- "keep anything beyond a conservative subset behind capability flags" — Samsung publishes no key-support matrix. (RES §3.5)
- "a model without a recorded pass surfaces no wake claim." (SPEC §Constraints)
- Product scope: "Tizen TVs (2016+ committed, 2014–15 H/J as unsupported best-effort), built capability-gated per model." (SPEC §Why)

### H/J 2014–15 labeling (UI-copy mandate)
- "H/J 2014–15 sets, where attempted, are explicitly labeled unsupported best-effort in product and UI copy and never counted toward matrix pass claims." (SPEC §Constraints)
- "failure falling back to declared-unsupported" (SPEC §Assumptions); H/J standing is "**DISPUTED (high)**" — openHAB says not supported, HA says encrypted subset supported; "record per-model outcomes, do not generalize." (RES §16.1)

### Trust, pin mismatch, re-pair
- "Per-TV trust-on-first-use SPKI pin plus token in a client scoped to that TV; the pin is validated before the token is sent, mismatch triggers a visible re-pair, and there are no trust-all managers and no silent WSS-to-WS downgrade." (SPEC §Constraints)
- "Trust-on-first-use pinned to the leaf SPKI, confirmed in the pairing UI; every later connect throws on mismatch into a re-pair flow." (PR §TLS rule)
- "Show a visible **re-pair** flow when the certificate changes. **Validate the pinned identity before sending the stored token.**" (RES §3.4)
- TOFU capture: "On first pairing, capture the leaf certificate's SPKI hash and confirm in the UI (the TV's Allow prompt confirms physical presence)." (RES §3.4)
- CAP-8 success: "pairing-required and identity-changed states surface a re-pair flow." (SPEC §Capabilities CAP-8)
- `ms.channel.unauthorized` = "Pairing denied or token rejected → re-pair flow." (RES §3.6; PR §Channel events)

### Wake
- CAP-5: power off and wake are "two separate actions"; "Wake sends the magic packet from the stored MAC behind the setup checklist and reports success only when the TV answers on 8001/8002." (SPEC §Capabilities CAP-5)
- "**UX:** a setup checklist ('enable Power On with Mobile'), a 'Wake' button separate from 'Power', and honest failure messages." (RES §4.5)
- Wake committed per model/network type only after a matrix pass (see gating above). (SPEC §Constraints)
- Setting OFF "Blocked — setup checklist must gate it." (RES §16.5)

### Casting (TV-fetches, session lifetime, screen-off)
- "Casting is TV-fetches-from-phone: the TV receives an http(s) URL never a content:// URI, and the phone server supports HEAD and Range with stable reachability during the session." (SPEC §Constraints)
- CAP-6 success: "serving is bounded to selected items with unguessable IDs, stops at session end, and survives screen-off during the session." (SPEC §Capabilities CAP-6)
- "In DLNA the **phone is the control point** and the **TV fetches the media** from a URL the phone hosts." Server needs "Stable reachability while the phone screen is off." (RES §4.6)
- Ladder: "JPEG/MP3/H.264-AAC short clips play with transport controls on validated renderers; HEVC, subtitles, seek, and 4K/long-file follow per model." (SPEC §Capabilities CAP-6)

### Discovery, manual IP, isolation diagnostics
- "Discovery and wake assume one broadcast domain with manual IP entry always offered, and isolation is explained in onboarding diagnostics." (SPEC §Constraints)
- "**Always offer manual IP entry.** Discovery can fail on guest Wi-Fi, VLANs, AP isolation or networks that filter multicast." (RES §3.1)
- "**[Eng. note] Onboarding diagnostics.** Detect the phone's subnet, flag likely isolation (no SSDP responses, but a manual IP that answers on 8001), and explain the network constraints in plain language. With SmartThings out of the key path, there is no cloud fallback for keys." (RES §5)

### Cloud out of key path / local-only latency
- "Cloud remote stays out of the key-repeat hot path; key, text, and pointer traffic is local-LAN only." (SPEC §Constraints)
- SmartThings rate limits "~12 requests/min" per device: "Fatal for key repeat. Rapid volume or D-pad presses would hit HTTP 429 within seconds." (RES §2.3)
- Key latency has no measured figures: "'near-zero latency' is unsupported, so measure it in Spike A"; reconciliation #11 verdict: "**Unmeasured**." (RES §4.1, §13)

### Foreground services & notifications
- "The remote socket lives only while the remote UI is active; foreground services exist only for user-started cast/mirror/voice sessions with the matching service type." (SPEC §Constraints)
- "Remote keys: open the WebSocket **only while the remote UI is active** … **No always-on foreground service** for ordinary keys." (RES §6.3)
- Cast FGS: "a foreground service **started by the user, for that session**, with the matching FGS type"; mirroring: FGS type `mediaProjection`. (RES §6.3)
- Mirroring FGS requires "an ongoing notification and the status-bar indicator." (RES §4.7)

### Text (explicit send, focus semantics)
- CAP-2: "send phone-typed text into the TV's focused native text field or browser URL field as an explicit send action"; success: "with send/cancel semantics and focus state; app-drawn character grids are excluded unless a per-surface probe passes." (SPEC §Capabilities CAP-2)
- "**Product contract:** text entry should be an explicit, field-bound 'send' action with its own focus and capability state and send/cancel semantics, not a stream of phone keystrokes." (RES §4.2)
- "**Do not market it as typing into Netflix/YouTube/etc. search.**" (RES §4.2); "Text entry into app-drawn character grids as a launch promise" is a non-goal. (SPEC §Non-goals)

### Mirroring receiver
- CAP-7: mirror "through the TV-side WebRTC receiver"; "mirroring without a TV-side receiver is excluded" (also SPEC §Non-goals). "Mirrored output is visible on the receiver with latency and battery figures recorded per spike." (SPEC §Capabilities CAP-7)
- "The V1 mirroring receiver ships as a Tizen dev-mode sideload for pilot and matrix validation; a store listing is pursued only after CAP-7 latency and battery figures are recorded." (SPEC §Constraints)

### Multi-TV
- CAP-8: "pair, persist, and switch between multiple TVs with explicit target selection"; "Two TVs pair and persist across restart per device; reconnect uses backoff." (SPEC §Capabilities CAP-8)
- "Multiple TVs: keep one credential/device record per TV and require explicit target selection." (RES §3.3); "Require explicit target selection when there are multiple TVs." (RES §5)

### Release-gate demo (end-to-end UX script)
- "A user pairs a recent Tizen TV plus a second older set, then drives keys, sends a text string, moves the pointer, casts a photo and a video, powers off, wakes, and mirrors through the receiver — every step with observed on-TV effect … That demonstration, not merged code, is the release gate." (SPEC §Success signal)

---

## 2. Flows needing screens

### Pairing (TOFU + TV Allow prompt)
- Discovery: SSDP M-SEARCH to `239.255.255.250:1900` under a held MulticastLock, then `GET <tv>:8001/api/v2/` for model, `TokenAuthSupport`, `wifiMac` ("persist the MAC for wake"). (PR §Pairing lifecycle; RES §3.1)
- "First connect without token: TV shows Allow/Deny for the physical remote to answer." Allow → `ms.channel.connect` (carries token on token-auth models); "deny/timeout yields `ms.channel.unauthorized` or close." (PR §Pairing lifecycle; RES §3.3)
- Prompt window: "Pairing overlay Allow/Deny ≤30s, token cached." (RES §16.1)
- TOFU pin confirmation happens in the pairing UI; the TV Allow prompt "confirms physical presence." (PR §TLS rule; RES §3.4)
- Assumption: "Phone and TV share a LAN at pairing time, and user can answer the TV Allow prompt with the physical remote." (SPEC §Assumptions)
- Manual IP entry always offered alongside discovery. (SPEC §Constraints; RES §3.1)
- Denied-pairing fallback (user-side TV steps): "first-time-only + purge device list + re-prompt; no silent retry"; newer sets default Access Notification to ask-every-time → "Device Connection Manager → Access Notification → **First Time Only** + purge the Device List to stop re-prompts." (RES §16.1)

### Multi-TV selection / switching
- Explicit target selection screen/affordance required whenever multiple TVs exist; one persisted record per TV (identity, MAC, last IP, model label, capabilities, pairing state, pin, health). (RES §3.3, §5, §6.1; SPEC CAP-8)
- Reconnect uses backoff; persist across app restart. (SPEC §Capabilities CAP-8)

### Re-pair
- Triggered by: pin/cert mismatch ("TV identity changed — re-pair required" thrown before token send), `ms.channel.unauthorized`, firmware re-prompts ("Community packages document **repeated authorization prompts** on some models and firmware, so design for re-pairing"). (RES §3.4, §3.6, §3.3)
- Must be "a visible re-pair flow" — never silent; pairing-required and identity-changed are distinct surfaced states. (SPEC §Constraints, CAP-8; RES §5)

### Wake
- Setup checklist screen for TV setting "Power On with Mobile" / Network Standby before wake is offered; setting paths: 2022–2026 `Settings → All Settings → Connections → Network → Expert Settings`; 2017–2021 `Settings → General → Network → Expert Settings`. (RES §4.5, §16.5; PR §Wake packet)
- "Wake" button separate from "Power"; honest failure messages; success reported "only when the TV answers on 8001/8002." (RES §4.5; SPEC CAP-5)
- Magic packet needs stored MAC (captured at pairing from `wifiMac`); sent to subnet-directed broadcast + `255.255.255.255`, UDP ports 9 and 7. (PR §Wake packet; RES §4.5)

### Cast session start / control / end
- Start: discover MediaRenderer (AVTransport, RenderingControl for volume "only if advertised"); user selects items via Photo Picker/SAF; phone hosts bounded HTTP server; `SetAVTransportURI` with phone URL. (RES §4.6; PR §DLNA flow)
- Control: `Play`/`Pause`/`Stop`/`Seek` transport controls; volume via RenderingControl only if advertised. (PR §DLNA flow; RES §4.6)
- End: server "stops at session end"; serving bounded to selected items with unguessable IDs. (SPEC CAP-6; RES §4.6)
- Screen-off survival during session via user-started FGS. (SPEC §Constraints; RES §4.6, §6.3)
- Per-model ladder features (HEVC, subtitles, seek, 4K/long files) gated by validation. (SPEC CAP-6; VM §Gates)

### Mirror session
- Requires TV-side WebRTC receiver (V1: dev-mode sideload pilot — Developer Mode via "Apps panel → 12345 → ON + host IP → power-cycle", same-LAN SDB to TV, `.wgt` install; Tizen 7+ needs Samsung-account cert signing). (SPEC CAP-7/§Constraints; RES §16.4)
- Per-session consent: Android 14+ "Each consent result is **single-use** … consent is needed for every session." (RES §4.7)
- During session: `mediaProjection` FGS with ongoing notification + status-bar indicator. (RES §4.7, §6.3)
- Latency/battery figures recorded per spike before any store claim. (SPEC CAP-7, §Constraints; VM §Spikes M)

### Voice dictation
- User-started flow; `RECORD_AUDIO` requested "inside the user-started voice flow." (RES §7.3; SPEC §Constraints on FGS)
- Pipeline: phone STT → mapped keys or text through the existing WS channel; "raw-audio injection stays behind the voice spike gate and out of launch commitments." (SPEC CAP-4; RES §15.1)
- "dictation sends only into focused native fields" (SPEC CAP-4); dictated phrases → SendInputString "only when a native IME control is focused." (RES §16.7)
- Voice session carries an FGS (user-started cast/mirror/voice sessions only). (SPEC §Constraints)

### Text send / cancel with focus semantics
- Focus signals: `ms.remote.imeStart`/`imeEnd` = "A TV text field gained or lost focus → show or hide the phone keyboard UI" — "(open/close phone keyboard UI, plus a manual type action since delivery is inconsistent)." (RES §3.6; PR §Channel events)
- "Event delivery is described as occasional or inconsistent, so also offer a manual 'type' action." (RES §4.2)
- Send is explicit with send/cancel semantics and focus state; emit `custom.remote.textReceived` before first text after IME start. (SPEC CAP-2; PR §Message frames; RES §3.5)
- Arrival test: "Test that the text actually *arrives* on screen, not just that the socket write succeeded." (RES §4.2; VM §Probes)

### Touchpad probe + hide
- Feature visible only "on models passing the cursor probe"; "stays hidden elsewhere with a plain-language explanation." (SPEC CAP-3)
- Probe: "with browser open, send Move sweep; PROVEN only if on-screen cursor visibly relocates; cross-check d-pad/Enter-vs-click on same target." (RES §15.3)
- Pointer moves are fire-and-forget stream; clicks are separate key frames; per-surface follow/ignore matrix open (browser vs Netflix/Prime/YouTube). (RES §15.3, §4.4)

---

## 3. Failure & edge states (RES §5 + §4, and spec)

### Required distinct states
- "Represent **asleep / offline / paired / pairing-required / permission-denied / identity-changed** as distinct states." (RES §5)
- Sealed state hierarchy example: "`Disconnected`, `Discovering`, `Connecting`, `PairingRequired`, `Connected`, `Asleep`, `IdentityChanged`, `Error`" exposed as observable state. (RES §6.2)

### Connection drops & reconnect
- "Reconnect on Wi-Fi or address changes, with backoff." "Expire stale discovery entries." "Keep manual-IP fallback." (RES §5)
- Power-off: "Expect the socket to drop as the TV enters standby. Represent *asleep/offline* as its own state." (RES §4.5)

### TV sleep / standby & wake failures
- "A sleeping TV's WebSocket server usually **isn't reachable**, so a power key sent to port 8002 will time out." (RES §4.5)
- "Unicast to the last-known IP often fails once the router's ARP entry has expired." (RES §4.5); "dominant failure = deep-sleep NIC power-down." (RES §15.4)
- Wi-Fi standby may wake "less reliably than over Ethernet, depending on its standby radio policy" — per-model matrix column. (RES §4.5)
- Cross-subnet wake "Unsupported without directed-broadcast forwarding; `255.255.255.255` will not cross routers." (RES §16.5)
- "wait up to ~1min after power-off before WOL (TV not yet in standby)." (RES §16.5)

### Token invalidation / re-prompts
- Repeated authorization prompts on some firmware → design for re-pairing; token treated as opaque. (RES §3.3)
- `ms.channel.unauthorized` → re-pair flow. (RES §3.6)
- Recovery after firmware reset or token invalidation is an open question ("What recovery is needed after a firmware reset or token invalidation?"). (RES §12 Q2)

### Identity changed
- Cert/SPKI mismatch → throw before token send → visible re-pair flow; open question whether TV cert is per-device or shared across a model line (pin may prove "a Samsung TV with this key," not "this TV"). (RES §3.4, §12 Q2)

### Network isolation
- SSDP (multicast) and WOL (broadcast) "fail across VLANs, IoT/guest SSIDs with client isolation, or networks that filter multicast." (RES §5)
- Correction: "unicast TCP … **can** cross subnets if the router routes between them and the firewall allows it. What reliably breaks is discovery and wake. Hence: **manual IP entry + saved IP** for control, and documented router settings (directed-broadcast forwarding) for wake." (RES §5)
- Same subnet/VLAN required for SSDP discovery, WS pairing and WOL. (RES §16.1)
- Onboarding diagnostic signal: no SSDP responses but manual IP answers on 8001 → likely isolation. (RES §5)

### Multicast lock
- Without a held `MulticastLock`, "many devices drop multicast responses and TV discovery becomes unreliable"; needs `CHANGE_WIFI_MULTICAST_STATE`. (RES §6.4)

### Per-model capability variance
- "Treat every field as **firmware-dependent** and confirm it on hardware." (RES §3 header)
- Regional/firmware variance examples: "app-list API removed ~2019/2020; Frame art API removed 2022, restored fw 1622+"; `ed.installedApp.get` "not available on all TVs." (RES §16.1, §3.5)
- DLNA: renderer presence and codecs "must be **discovered per TV**, not assumed"; "A TV's internal USB/player codec table **is not** a DLNA guarantee." (RES §2.4, §4.6)

### Keyboard-text surface variance
- Surface matrix: native Tizen `input`/`textarea` "**ACCEPTS (high)**"; TV browser URL entry "medium"; "Netflix / YouTube / Prime search grids **UNKNOWN, likely reject**"; "System PIN / password dialogs Unknown." (RES §16.6)
- Grid apps "draw their own character grid, which may never raise the system IME." (RES §4.2)
- "**Rule: socket-write ≠ acceptance.** PROVEN only on observed effect (glyphs on screen)." (RES §16.6)

### Pointer-support variance
- Browser follows pointer; "Grid/D-pad apps such as Netflix, Prime Video and YouTube often **ignore pointer events**, which makes the feature look broken." (RES §4.4)
- "Pointer visibility, coordinate scaling, click semantics and firmware differences are **unvalidated**." (RES §4.4)

### Cast edge states
- TV can't fetch `content://` or `file://` URIs; byte-range support needed "for seeking and long files"; Samsung renderers "picky about DIDL-Lite `protocolInfo` and DLNA headers." (RES §4.6)
- Subtitles "vary by firmware"; external-SRT over DLNA "repeatedly reported missing"; PGS subs need transcode/burn-in. (PR §DLNA flow; RES §16.8)

### Latency figures
- None measured anywhere: "No source reviewed gives measured, fleet-wide failure rates, latency figures or support-burden data. **Statements about reliability, latency or 'most TVs' are qualitative unless a test result is cited.**" (RES §1)
- Key latency: measure in Spike A; voice latency: measure in Spike V; mirror latency: record per transport. (VM §Spikes; RES §4.1, §15.1)
- Unverified mirror estimates only: transport A "High, multi-second"; transport B "Low, sub-second capable." (RES §15.2)

---

## 4. Platform constraints touching UX

- **Android 14+ mirroring:** MediaProjection consent is single-use per session (reuse throws `SecurityException`) → consent UI every mirror session; `mediaProjection` FGS "with an ongoing notification and the status-bar indicator." (RES §4.7)
- **FGS types & notifications:** FGS only for user-started cast/mirror/voice with matching type (`mediaPlayback`/`connectedDevice` likely for cast; `mediaProjection` for mirror; microphone type for voice); "**Don't use `dataSync`** … capped at 6 hours per 24 hours" on Android 15+. (RES §6.3, §7.3; SPEC §Constraints)
- **Runtime permission prompts:**
  - `POST_NOTIFICATIONS` — "Notification UX … Doesn't replace an FGS permission." (RES §7.3)
  - `RECORD_AUDIO` — "Dangerous runtime permission; request it inside the user-started voice flow." (RES §7.3)
  - **Local-network permission** — covers "SSDP/mDNS, `NsdManager`, raw sockets, inbound and outbound LAN TCP/UDP"; "Opt-in under Android 16 guidance. **Enforced as a runtime permission for apps targeting API 37+ (Android 17).** Build and test a permission gate now." Spike L runs the full flow under targetSdk-37 behavior "before rollout." (RES §7.3; VM §Spikes L)
  - Media access: "*(none)*" — Photo Picker/SAF "Avoids broad media permissions; selected items are still served over the LAN." (RES §7.3)
- **Permission-denied is a first-class state** in the state model. (RES §5)
- **Battery during mirroring:** "heavy battery and thermal load" expected for transport A; both spike transports "High sustained encode … (magnitude unmeasured)"; latency and battery figures must be recorded per spike before store listing. (RES §15.2; SPEC §Constraints, CAP-7)
- **SDK floors:** targetSdk 36 (Play requirement effective 2026-08-31); minSdk 26 settled ("no API-29-only dependency found; 29 sheds reach for nothing"). (SPEC §Constraints; RES §16.2)
- **No cloud fallback for keys** — with SmartThings out of the key path, local-LAN failure must be explained, not routed around. (RES §5; SPEC §Constraints)

---

## 5. Voice/STT UX notes (RES §4.3 + §15.1 + §16.7)

- Launch voice experience = phone-side STT: "do speech-to-text on the phone and map recognised intents to keys or text. That is a phone-side NLP feature. It needs `RECORD_AUDIO` and does **not** create a TV voice protocol." (RES §4.3)
- Spec assumption: "Phone-side speech-to-text mapped to keys plus short dictation counts as the launch voice implementation; raw injection is a spike." (SPEC §Assumptions; also Non-goals: "Raw voice-audio injection as a launch commitment")
- CAP-4 scope: "voice covering the full key set plus short dictation, executed as mapped keys or text through the phone-side pipeline." (SPEC §Capabilities CAP-4)
- Raw-audio-over-WS claim is UNVERIFIED (low) — "Until both land, do not design to it." (RES §15.1)
- "Latency and per-intent accuracy unmeasured — measure in Spike V." (RES §15.1); VM Spike V: "Phone STT→keys/text build with the agreed command set, latency measured." (VM §Spikes)
- Launch command set (proposed, confidence-ranked — "User picks the launch set"): (RES §16.7)
  1. **1:1 high — ship first:** power/nav/system keys, volume/channel/digits, transport + colour keys, GUIDE/INFO/TOOLS.
  2. **Medium — conditional:** app names via deep-link IDs (region-dependent); short dictated phrases → only into focused native IME control; long-press commands — "voice set must distinguish tap vs hold."
  3. **Excluded:** pointer paths ("swipe to X", "third tile", drag/scroll) — "no single-key equivalent … fragile."

---

## 6. Numbers worth designing around

**Ports & addresses** (PR §Endpoints; RES §3.2, §3.1, §4.5)
- `8001` HTTP/WS — device info `/api/v2/`; legacy insecure remote on some models; "Plain text; never silently fall back from WSS."
- `8002` HTTPS/WSS — authenticated remote channel; self-signed TV cert, pin per device.
- `55000` TCP — pre-2016 Orsay legacy, out of scope.
- SSDP multicast `239.255.255.250:1900`; WOL UDP ports `9` and `7`; magic packet = 102 bytes (`FF`×6 + MAC×16).

**Timers & timeouts**
- Pairing Allow/Deny overlay: ≤30s. (RES §16.1)
- Wake: standby TVs surface ~7s post-discovery; connect() wakes then waits ~1min (SDK 2.4.0+ behavior); wait up to ~1min after power-off before sending WOL. (RES §16.5)
- `dataSync` FGS cap: 6h per 24h on Android 15+ (a reason it's banned, not a budget to use). (RES §6.3)
- SmartThings cloud: ~12 requests/min/device — "Rapid volume or D-pad presses would hit HTTP 429 within seconds." (RES §2.3)

**Latency**
- No measured latency figures exist for keys, text, voice, or cast (RES §1, §4.1); mirror estimates unverified: multi-second (transport A) vs sub-second capable (transport B). (RES §15.2)

**Model-year floors & cohorts** (SPEC §Why/§Assumptions; RES §16.1, §15.2, §16.8)
- Tizen 2016+ (K-series onward) = committed floor; H/J 2014–15 = unsupported best-effort (DISPUTED); pre-2016 Orsay (C/D/E/F 2010–2013) = out of scope.
- WebRTC game-mode receiver primitive: Tizen 2022+.
- Codec ladder (renderer-tested anchors): H.264 all models → HEVC 2016+ → VP9 2019+ → AV1 2022+; no Dolby Vision (HDR10/HDR10+ only).
- Wake setting paths split: 2017–2021 vs 2022–2026 menus.

**Platform figures** (SPEC §Constraints; RES §7.1, §16.2, §7.3, §2.3)
- targetSdk 36, Play deadline 2026-08-31; minSdk 26 (≈96.1% distribution vs ≈91.1% at API 29 — "29 sheds ~5pp reach for nothing").
- Local-network permission enforced at runtime for apps targeting API 37+ (Android 17).
- Token: opaque string, "often 8 characters" — never validate format, never log. (RES §3.3)
- SmartThings paid tiers from Oct 2026 ($4.99/mo Personal Plan; PATs valid 24h) — context for cloud exclusion only. (RES §2.3)

**Matrix gate thresholds** (VM §MVP green-light gates; VM §Spikes A)
- Keys+pairing must pass on ≥3 model years (incl. one recent Tizen) before core remote + casting ship; "No fleet-wide percentages without measured data."

**Mirror pilot (sideload) figures** (RES §16.4)
- Dev mode: Apps panel → code `12345` → ON + host IP → power-cycle; SDB connect to TV on port `26101`; Tizen 7+ requires Samsung-account cert signing.
