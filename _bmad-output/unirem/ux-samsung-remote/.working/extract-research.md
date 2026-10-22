# Research extract — UX-relevant

Extraction only. No design decisions, screens, or flows proposed.

**Citation keys:** `R` = `research-native-kotlin-samsung-remote.md`; `I` = `imports/external-report-2026-10-16.md`; `B` = `brief.md`; `D1`–`D4` = `digests/d1-landscape.md` … `d4-ecosystem.md`; `DA` = `digests/deepen-d2A-r1.md`; `DB` = `digests/deepen-d2B-select-r1.md`; `DC` = `digests/deepen-d2C-r1.md`; `DI` = `digests/deepen-input-wake-cast-r1.md`; `DM` = `digests/deepen-mirror-r1.md`; `DV` = `digests/deepen-voice-r1.md`; `M` = `.memlog.md`.

**Evidence labels used below**

- **Observed** — explicit protocol/platform behavior, a verified primary-document finding, or a directly quoted research finding. It does *not* mean usability observation; the corpus contains none.
- **Forum-reported** — community thread, integration README, or issue report; useful signal, not independently verified.
- **Speculative** — inference from constraints, engineering judgement, or product framing; not evidence of user behavior.

## Source files and roles

| File | Role | UX-relevant content / limits |
|---|---|---|
| `research-native-kotlin-samsung-remote.md` (R) | Consolidated technical report and decision record, §§0–16 | Primary source: feature feasibility, protocol behavior, failure states, test matrix. Technical, not user research. |
| `brief.md` (B) | Research prompt | Defines scope and technical questions: keys, text, voice, pointer, DLNA, mirroring, Play constraints. Product framing only; no user evidence. |
| `digests/d1-landscape.md` (D1) | 12-claim ledger on control paths | Confirms local WS is community-only; SmartThings/UPnP/DIAL limits. |
| `digests/d2-integration.md` (D2) | Protocol/auth/cast/WOL/TLS claims | Pairing prompt, token, TLS pin, text/mouse frames, WOL, DLNA. |
| `digests/d3-platform.md` (D3) | Android SDK/permissions/FGS claims | targetSdk 36, minSdk 26, local-network permission, FGS types, MediaProjection. |
| `digests/d4-ecosystem.md` (D4) | Library/SmartThings/regret claims | SmartThings quotas/paid tiers, mirroring regret, AppT/Beta evidence limits. |
| `digests/deepen-d2A-r1.md` (DA) | Support matrix / wake / receiver distribution | Tizen generations, H/J dispute, WOL/WoWLAN setting paths, Seller Office/sideload. |
| `digests/deepen-d2B-select-r1.md` (DB) | DLNA library select / minSdk | jUPnP vs UPnPCast, screen-off uncertainty; little direct UX. |
| `digests/deepen-d2C-r1.md` (DC) | Text surfaces / voice mapping / codec vectors | Native vs grid text, voice key mapping, codec/subtitle ladder. |
| `digests/deepen-input-wake-cast-r1.md` (DI) | Pointer/text/wake/cast mechanics | Hover, fire-and-forget pointer, IME session, WOL, DLNA flow, observed-effect probes. |
| `digests/deepen-mirror-r1.md` (DM) | Mirroring transports | MediaProjection constraints, WebRTC receiver requirements, Smart View/Miracast limits, latency expectations. |
| `digests/deepen-voice-r1.md` (DV) | Voice evidence | Raw-audio claim unverified; TV APIs app-local; STT fallback. |
| `imports/external-report-2026-10-16.md` (I) | Earlier consolidated report, pre-D1/D2 | Near-duplicate of R §§0–14; no unique UX evidence. R §§15–16 supersede. |
| `.memlog.md` (M) | Process and verification log | Records unresolved two-source gaps and verifier statuses; useful for confidence, not UX. |

**Coverage.** All 14 files were read end-to-end (13 content files + memlog). R and I overlap heavily; I lacks the D1/D2 deepen sections. All UX-bearing material in I also appears in R.

**Evidence quality — critical for use.** No user interviews, app-store reviews, usability tests, analytics, screenshots, journey maps, or support tickets exist in this corpus. B is a technical/implementation brief (B, Research questions 1–5); it does not commission end-user research. R §1 explicitly states that no source reviewed gives measured, fleet-wide failure rates, latency figures, or support-burden data. M records the remaining two-source gaps: SmartThings quotas are single-source and endpoint-sensitive; Tizen compatibility rests on a community cluster with no Samsung contract; WOL reliability and certificate scope are unverified; UPnPCast provenance is unresolved; voice and pointer evidence conflict; no DLNA per-model matrix exists (M, verification entries). Therefore every “user problem” below is inferred from protocol/platform constraints unless labelled forum-reported.

## User problems and jobs to be done

Jobs are inferred from the product scope (B) and the interaction constraints (R §4). The corpus establishes friction, not demand, frequency, or priority.

| Job (user intent) | Documented friction | Evidence | Status |
|---|---|---|---|
| Control the TV without reaching for the physical remote | First connection requires the TV’s **Allow/Deny** overlay to be answered with the physical remote; prompts can recur on some firmware; newer TVs default to ask-every-time. Key support beyond a conservative set varies by model. A socket write is not proof the TV acted. No measured key latency. | R §3.3, §3.5, §4.1, §16.1; DA §Q1 | Observed protocol; repeat prompts forum-reported; latency unknown |
| Type a search, URL, or password on the phone | Text injection reportedly works only into a focused **native Tizen** text field. Streaming-app character grids likely reject it. IME focus events are inconsistent, so a manual “type” action is still needed. Report contract: send is explicit and field-bound, not a stream of keystrokes. Success is glyphs on screen, not socket success. | R §4.2, §16.6; DC §Q3 | Observed/protocol; grid and password acceptance unknown |
| Point and click on the TV | Protocol accepts pointer `Move` frames; Samsung’s own TV UX treats **hover** as a first-class cursor state. The TV browser reportedly follows the cursor, but Netflix/Prime/YouTube-style grid apps often ignore pointer events, “which makes the feature look broken.” Click is a separate key frame, not part of the move. Coordinate scaling and cursor visibility are unverified. | R §4.4; DI §1; R §12 #12 | Forum-reported + unverified |
| Talk to the TV | No phone-mic-to-TV audio transport is established. Samsung `VoiceInteraction`/`VoiceControl` APIs are app-local to the TV. The buildable path identified is phone STT → keys/text, which needs `RECORD_AUDIO` and does not create a TV voice protocol. Raw-audio-over-WS remains unverified. First-party mic/Bixby availability varies by region, model, and assistant. | R §4.3, §15.1; DV, Claims | Observed absence; raw claim unverified |
| Turn the TV off and back on | Power-off is a key while connected; the socket then drops and the TV enters standby. Power-on is a separate WOL/WoWLAN path requiring the TV’s MAC, same LAN, a TV setting (“Power On with Mobile”), and standby timing. Deep sleep can power down the NIC; wired Ethernet has per-model quirks. | R §4.5, §16.5; DA §Q8 | Observed protocol; per-model failures forum-reported |
| Play phone photos/videos/music on the TV | DLNA is **TV-fetches-from-phone**: the phone hosts an HTTP URL; the TV cannot fetch `content://` or `file://` URIs. The phone server must support `HEAD`/`Range` and survive screen-off during the session. Codec/subtitle support varies by model; live/unbounded streams are expected to be rejected. | R §4.6, §6.3, §15.2, §16.8; DC §Q7; DM, Select matrix | Observed protocol; per-model coverage unknown |
| Show the phone screen on the TV | Android can capture the screen, but no compatible third-party Samsung receiver/transport has been established. Smart View/Miracast is system/OEM-owned and cannot be started by a third-party app. MediaProjection consent is per-session and single-use; a foreground service and status-bar indicator are required. | R §4.7, §15.2; DM, Claims | Observed platform constraint; receiver feasibility unverified |
| Use more than one TV | One credential/device record per TV; explicit target selection is required when multiple TVs exist. Navigation or receiver changes must not redirect or stop unrelated sessions. | R §3.3, §5, §6.1 | Observed design constraint |
| Recover when the network or TV changes | SSDP discovery fails on guest Wi-Fi, VLANs, AP isolation, or multicast-filtering networks; manual IP entry must always be offered. A changed certificate or repeated prompt requires a visible re-pair. The report models asleep/offline/paired/pairing-required/permission-denied/identity-changed as distinct states. | R §3.1, §3.4, §5, §16.1 | Observed/reported |
| Use one app instead of the physical remote + SmartThings + separate casting/mirroring apps | This is the product framing in B (“universal remote”) and the decision record’s scope, not a finding from users. The corpus contains no evidence of how often users juggle these tools, which job matters most, or whether they want one app. | B, “Decision this serves”; R §0 | **Speculative** |

**Unvalidated product assumptions.** The corpus does not establish that users will: accept answering a TV overlay with a physical remote; tolerate repeated prompts; prefer phone text entry for native fields but accept that streaming-app search grids may not work; accept a touchpad that only works in some apps; install or sideload a TV-side receiver for mirroring; tolerate per-session MediaProjection consent; accept live-cast or mirror latency and phone battery/thermal cost; or understand per-model capability gating instead of “works with any Samsung TV.” These are open UX research questions, not findings.

## Existing solutions and their UX gaps

**No user praise/complaint data exists in this corpus for any consumer app.** There are no app-store reviews, user interviews, usability findings, or competitive UI teardowns. The table below records what the technical sources establish and the friction they imply; “praise/complaint evidence” is absent unless explicitly noted.

| Solution | What the corpus establishes | UX gap / friction | Praise or complaint evidence |
|---|---|---|---|
| **Physical remote** | The report says the TV’s pairing Allow/Deny overlay must be answered with the physical remote. The app’s key list mirrors physical-remote keys; long-press semantics are documented separately. | No phone-keyboard path; streaming-app search is D-pad grid typing. It must be at hand during first pairing and possibly re-prompts. Nothing in the corpus says users find this annoying; that is inference. | None recorded. |
| **Samsung SmartThings** | Official first-party phone remote. Microphone/Bixby availability varies by region, model, and assistant. Capability-based: no evidence it can inject arbitrary keys, text, pointer input, or microphone audio. Published limits are ~12 requests/min per device for commands and state polling; rapid volume/D-pad presses would hit HTTP 429 “within seconds.” Paid API tiers were announced for October 2026 ($4.99/month Personal; commercial unpublished); final terms were unconfirmed at cut-off. Home API is a restricted preview needing a Samsung account, Matter hub, and separate APK. | Cloud/account path, not local; cannot carry keyboard/pointer; rate limit is fatal for key repeat; paid/terms uncertainty; no real-time state sync at 12 polls/min. | Official/possible supplement for state or power is an engineering assessment (R §8), not user praise. The rate limit is a documented constraint, not a user complaint. |
| **Third-party remote apps** | The corpus does **not** review consumer third-party apps. AppT and Beta are same-organization reference implementations; neither has a passed physical model/firmware row. Community integrations (samsungtvws, HA, openHAB) show repeated authorization prompts on some firmware and subnet/VLAN caveats. | Compatibility uncertainty; pairing friction; manual IP fallback needed; no evidence of their UI patterns or user satisfaction. | None recorded. |
| **Casting apps / DLNA control points** | DLNA control point model: phone hosts media, TV fetches it. Live/unbounded streams are reported incompatible with Samsung DMRs (live feeds fail via DLNA while Chromecast works). External SRT over DLNA is repeatedly reported missing; PGS image subs need burn-in. Codec support follows a per-model ladder, not the TV’s USB codec table. UMS uses per-device renderer `.conf` profiles to pin behavior. | Per-model format/subtitle failures; phone server must stay reachable; `content://` cannot be handed to the TV; live content is a poor fit; no official DLNA matrix. | Community reports of missing subtitles and live-stream failures (DC §Q7; DM, Claims); no app reviews or praise. |
| **Mirroring flows (Smart View / Miracast / Smart View SDK)** | Galaxy Smart View/Miracast is built into the phone/TV system but third-party apps cannot programmatically start a Miracast session. Smart View SDK is app-to-app: sender + installed TV receiver; new Cloud TV apps ended after 2019 and DMP after 2021. Beta’s WebRTC receiver has not landed. Android MediaProjection works, but the TV-side receiver is the unsolved part. | No one-tap third-party mirroring; receiver acquisition/install burden; per-session consent; ongoing notification/status chip; latency and battery cost. | None recorded. “Highest regret risk” is an engineering assessment (R §8), not a user complaint. |
| **DIAL** | Discovers and launches TV apps only; not power, keys, casting, or mirroring. | Not a substitute for any control/cast/mirror flow. | None recorded. |

**Forum-reported friction (not user satisfaction):** repeated authorization prompts on some firmware (R §3.3; D2, Claims); pointer ignored by major streaming apps (R §4.4); live DLNA feeds failing while Chromecast works (DM, Claims); external SRT missing over DLNA and PGS needing burn-in (DC §Q7); deep-sleep WOL failures and wired-Ethernet/soundbar-ARC failure (DA §Q8; R §16.5); TV-browser cursor following while streaming grids do not (R §4.4). No praise signals are recorded for any solution.

## Interaction findings that shape UX

### Key-by-key

- **MVP set:** D-pad, OK/enter, back, home, volume, mute, play/pause. Extra keys sit behind a provider capability set; unsupported commands must be reported, not fail silently. (R §4.1)
- **Hold-to-repeat:** the protocol uses explicit `Press` … sleep … `Release`, not an auto-repeat flag. Research engineering note: add coalescing/debouncing so aggressive tapping cannot flood the socket. (R §4.1, §3.5; DI §2)
- **Truthful outcome:** a successful socket write is not a confirmed TV action. Model outcomes as *submitted / unconfirmed / observed*. (R §4.1; R §10)
- **Latency:** no measured data. Input B’s “near-zero latency” is unsupported; measure in Spike A. (R §4.1; R §13 #11; R §11.1)
- **Long-press semantics:** hold RETURN to exit Netflix, hold POWER, etc. require Press→sleep→Release; a voice/command set must distinguish tap vs hold. (DC §Q4)
- **App launch:** app names map only through out-of-channel deep-link IDs, not a WS key; availability varies by model/region, and the app-list API was removed around 2019/2020. (DC §Q4; DA §Q1)
- **Pointer cross-check:** for a click, compare d-pad/Enter vs pointer click on the same target; a visible cursor move alone is not enough. (DI §5)

### Text entry

- **Surface scope:** native Tizen `input`/`textarea` focused elements accept text (high confidence); TV browser URL entry has a medium-confidence recipe; Netflix/YouTube/Prime grids are **unknown, likely reject**; system PIN/password dialogs are unknown, with a separate `send_password` path in the library. (R §16.6; DC §Q3)
- **Session bounds:** `ms.remote.imeStart` / `imeEnd` open/close the phone keyboard UI. Event delivery is occasional/inconsistent, so a manual “type” action is required even when focus events fail. (R §4.2)
- **Wire behavior:** emit `custom.remote.textReceived` once per IME session before the first text; send Base64 UTF-8 in `Cmd` with `DataOfCmd: "base64"`; close with `SendInputEnd`. (R §3.5; DI §2)
- **Interaction contract:** text entry is an explicit, field-bound “send” action with its own focus and capability state and send/cancel semantics, not a stream of phone keystrokes. (R §4.2; Beta keyboard contract, R §10)
- **Verification:** success is glyphs appearing in the TV field, not a successful socket write. (R §4.2; DC §Q3)
- **Focus state is load-bearing but unreliable:** `imeStart`/`imeEnd` are the only focus signals, and they are inconsistent. No independent focus-detection mechanism is documented. (R §4.2; R §16.6)

### Pointer / touchpad

- **Mechanics:** `ProcessMouseDevice` / `Move` with `{x, y, Time}`; moves are fire-and-forget (`key_press_delay=0`), while clicks are separate `SendRemoteKey` Click/Press/Release frames. (DI §1)
- **Hover is a first-class TV state:** Samsung’s own input model distinguishes Hover (position without click) from Move (cursor relocation), separate from focus/D-pad. (DI §1)
- **App dependence:** the TV browser reportedly follows the pointer; grid/D-pad apps such as Netflix, Prime Video, and YouTube often ignore pointer events, making the feature look broken. (R §4.4)
- **Unknowns:** absolute coordinate space/scaling (e.g. 1920×1080 vs normalized), cursor visibility, click semantics, and per-surface follow/ignore are unvalidated. (R §4.4; DI §1; R §12 #12)
- **Capability posture:** the report treats this as an experimental provider, disabled unless capability tests pass for that model; explain that pointer works only where the TV app supports it. (R §4.4)

### Voice

- **No TV-side transport:** no stable public third-party phone-mic-to-TV contract exists. `VoiceInteraction` (privilege `voicecontrol`) and `VoiceControl` (`tizen.voicecontrol`, privilege `recorder`) expose callbacks/command lists to code running **on the TV**, not phone audio transport. (R §4.3, §15.1; DV, Claims)
- **Raw-audio claim:** a January 2026 discussion claims raw audio over `samsung.remote.control`, but shows no frames, method, codec, sample rate, or chunking; four Pastebin blobs were not retrieved. Treat as unverified. (R §15.1; DV, Claims)
- **Buildable fallback:** phone `AudioRecord` → `SpeechRecognizer`/on-device STT → intent-to-key/text mapping into the existing WS channel. Needs `RECORD_AUDIO` inside a user-started flow; latency and per-intent accuracy are unmeasured. (R §15.1; R §7.3; DV, Claims)
- **Launch-set shape:** 1:1 key commands (power/nav/system, volume/channel/digits, transport/colour) are high-confidence. App names via deep links are medium/region-dependent. Short dictated phrases only work with a focused native IME field. Pointer paths (“swipe to X”, “third tile”, drag/scroll) have no single-key equivalent and are excluded. Tap vs hold must be distinguished. (R §16.7; DC §Q4)
- **First-party variability:** Samsung’s SmartThings phone-remote help says mic/Bixby availability varies by region, model, and assistant; this describes the first-party app, not third-party API access. (R §4.3)

### Wake / power

- **Power off:** send `KEY_POWER` while connected; expect the socket to drop as the TV enters standby. Asleep/offline is its own state. (R §4.5)
- **Power on is separate:** a sleeping TV’s WebSocket server is usually unreachable. Wake requires WOL/WoWLAN, the TV’s MAC, and a TV setting. (R §4.5)
- **Packet/path:** 102-byte magic packet (6×`FF` + MAC×16), UDP port 9 (7 alternate), to subnet-directed broadcast and `255.255.255.255`. Unicast to the last IP often fails after ARP expiry; routers do not forward broadcasts across subnets without configuration. (R §4.5)
- **TV setting:** “Power On with Mobile” is the master switch. Paths: 2022–2026 `Settings → All Settings → Connections → Network → Expert Settings`; 2017–2021 `Settings → General → Network → Expert Settings`. OFF blocks mobile/SmartThings power-on. (R §16.5; DA §Q8)
- **Prior connection:** WoWLAN expects at least one prior successful connection so the TV registers the phone’s MAC; the sender stores the TV `wifiMac` from `/api/v2/`. (DA §Q8)
- **Timing:** wait up to ~1 min after power-off before WOL (TV not yet in standby); wake may take ~1 min. Deep-sleep NIC power-down is the dominant failure; reports include wired Ethernet failing with a soundbar on ARC/eARC. (R §16.5; DA §Q8; R §15.4)
- **Confirmation:** success is the TV answering on 8001/8002 or visibly powering on, not the packet send. The report’s UX note calls for a setup checklist, a “Wake” action separate from “Power,” and honest failure messages. (R §4.5; R §11.1)

### Cast

- **Model:** the phone is the control point; the TV fetches the media from a phone-hosted HTTP URL. The TV cannot fetch `content://` or `file://`. The server must serve only selected items, use unguessable IDs, and stop at session end. (R §4.6)
- **Server requirements:** correct `Content-Type`, byte-range support for seek/long files, and stable reachability while the phone screen is off. A user-started foreground service with the matching type is required for screen-off continuity; an ongoing notification is visible. (R §4.6, §6.3; D3)
- **Flow:** SSDP M-SEARCH for a MediaRenderer advertising AVTransport (ideally RenderingControl) → `SetAVTransportURI` with URL + DIDL-Lite → `Play`/`Pause`/`Stop`/`Seek`. Minimal DIDL can work; richer metadata is renderer-dependent. Volume works only if RenderingControl is advertised. (R §4.6, §15.4; DI §4)
- **Live content:** AVTransport is bounded-file-oriented; Samsung DMRs have reported live-stream incompatibility (live feeds/dashboards fail while Chromecast works). Expect multi-second latency from capture + encode + HTTP + TV buffering; unsuitable for gaming/interactive use. (R §15.2; DM, Claims)
- **Codec/subtitles:** no official DLNA matrix; USB/player tables are **not** DLNA guarantees. V1 test vectors are JPEG, MP3, H.264/AAC in MP4. Renderer-tested ladder: H.264 all → HEVC 2016+ → VP9 2019+ → AV1 2022+ → no Dolby Vision; TrueHD at best PCM. External SRT over DLNA is repeatedly reported missing; PGS needs burn-in. (R §16.8; DC §Q7; R §4.6)
- **Session ownership:** cast playback is TV-side after `SetAVTransportURI`; navigation or receiver changes must not stop an unrelated cast. The phone server still matters for file delivery. (DB §Q5; R §6.1)

### Mirror

- **Capture is feasible; receiver is not established.** MediaProjection → VirtualDisplay → Surface → MediaCodec works. Android 14+ consent is single-use per session; reusing it throws `SecurityException`. A `mediaProjection` foreground service, ongoing notification, and status-bar indicator are required. Android 15 QPR1 auto-stops on lock; the app must handle `onStop` and release the VirtualDisplay/surface. (R §4.7; DM, Claims)
- **System mirroring is not third-party drivable:** Miracast is supported natively on Samsung TVs but owned by the system/OEM; third-party apps cannot programmatically start a session. Galaxy Smart View is separate from the Smart View SDK. (R §4.7; DM, Claims)
- **Smart View SDK:** app-to-app, requiring a sender and an installed TV receiver; new Cloud TV apps ended after 2019 and DMP after 2021. It cannot by itself carry live screen video. (R §2.2; DM, Claims)
- **WebRTC candidate:** Tizen 2022+ game mode supports WebRTC (`use.game.mode`); SDP must predefine min/max resolution and max fps via `a=imageattr`; FHD models cap at 1920×1080 (`isUdPanelSupported()`); one audio + one video track in the same `<video>`; background `visibilityChange` disables tracks; server must answer FIR via RTP/RTSP. A custom Tizen Web receiver is required. (R §15.2; DM, Claims)
- **Latency:** B (WebRTC) is sub-second-capable; A (phone-hosted live stream via AVTransport) is multi-second and unsuitable for gaming. Battery/thermal cost is high for both, magnitude unmeasured. (R §15.2)
- **Receiver distribution:** sideload pilot via Developer Mode/SDB (Tizen 7+ needs Samsung-account cert signing); store scale via Seller Office, scoped by model group, US-default, phased updates. (R §16.4; DA §Q6)

### Pairing

- **First connection:** TV shows an Allow/Deny overlay; the user must answer with the physical remote. The prompt must be accepted within ~30s. Allow returns `ms.channel.connect` (token on TokenAuth models); deny/timeout yields `ms.channel.unauthorized` or a close. (R §3.3, §16.1; DA §Q1)
- **Token lifecycle:** persist per TV, keyed by stable identity (MAC/device ID), not IP; treat the token as an opaque string. Later connections add `&token=` and should skip the prompt, but some firmware re-prompts; re-pair is expected. (R §3.3)
- **Ask-every-time default:** newer sets default Access Notification to ask-every-time. The workaround is TV-side `Device Connection Manager → Access Notification → First Time Only` plus purging the Device List. Denied pairing has no silent retry; fallback is re-prompt. (R §16.1; DA §Q1)
- **TLS identity:** TV uses a self-signed certificate on 8002. Recommended trust-on-first-use SPKI pin per TV, validated before sending the token; visible re-pair when the certificate changes; never trust-all; never silently downgrade WSS→WS. (R §3.4)
- **Uncertain identity scope:** whether the TV key/certificate is unique per device or shared across a model line is unverified; if shared, the pin proves “a Samsung TV with this key,” not “this TV.” (R §3.4; D2, Claims)

### Multi-TV

- One credential/device record per TV; explicit target selection when multiple TVs exist; no implicit “last TV.” (R §3.3, §5)
- Separate owners for TV control, cast playback, and mirroring; navigation or receiver changes must not redirect or stop unrelated sessions. (R §6.1)
- Per-TV pinned TLS client and token isolation; a stale session must not publish state or persist credentials. (R §3.4, §6.2)

### Permission and consent moments

- **TV-side pairing consent:** Allow/Deny overlay, answered by physical remote, ~30s timeout. (R §3.3, §16.1)
- **TV-side access setting:** “Access Notification: First Time Only” plus Device List purge to stop re-prompts; this is a setup/consent step the user may not know exists. (R §16.1; DA §Q1)
- **Android local-network permission:** opt-in under Android 16 guidance; enforced as a runtime permission for apps targeting API 37+ (Android 17). Affects SSDP, raw sockets, local TCP/UDP, and the phone media server. (R §7.3; D3)
- **Microphone:** `RECORD_AUDIO` is a dangerous runtime permission; request it inside the user-started voice flow. If voice runs in the background, a microphone FGS type/permission is also required. (R §7.3)
- **Screen capture:** MediaProjection consent every session; Android 14+ consent is single-use and offers entire-screen vs single-app sharing. A `mediaProjection` FGS notification and status-bar chip are visible. (R §4.7; DM, Claims)
- **Foreground services:** cast and mirror sessions need user-started FGS with matching type and an ongoing notification; `POST_NOTIFICATIONS` is separate. Do not use `dataSync` (6h/24h cap on Android 15+). (R §6.3, §7.3; D3)
- **Media access:** Photo Picker/SAF avoids broad media permissions; selected items are still served over the LAN. (R §4.6, §7.3)
- **SmartThings (optional):** OAuth/account and paid-tier terms if used; not in the key path. (R §2.3)

### Latency expectations at a glance

| Interaction | What the corpus supports | Status |
|---|---|---|
| Key press | No measured data; “near-zero” is unsupported. Measure in Spike A. | Unknown (R §4.1; R §13 #11) |
| Pairing prompt | Accept within ~30s; deny/timeout closes/unauthorized. | Observed (R §16.1; DA §Q1) |
| Wake | Wait up to ~1 min after power-off; wake may take ~1 min; deep sleep can fail. | Observed/reported (R §16.5; DA §Q8) |
| Text send | No latency data; batch send; IME focus events inconsistent. | Unknown (R §4.2) |
| Pointer move | No latency data; fire-and-forget stream; click separate. | Unknown (DI §1) |
| Voice STT | No measured latency or accuracy; per-intent mapping unproven. | Unknown (R §15.1; DV) |
| Cast start/play | TV buffering; live streams multi-second; no measured figures. | Estimate (R §15.2; DM) |
| Mirror | B sub-second-capable; A multi-second; both unmeasured. | Estimate (R §15.2) |
| SmartThings command | ~12 requests/min/device; rapid input hits HTTP 429 in seconds. | Observed limit (R §2.3) |

## Device/platform realities

| Reality | What it changes for UX | Source |
|---|---|---|
| **Tizen 2016+ (K-series onward)** is the committed remote path; pre-2016 Orsay (C/D/E/F 2010–2013, port 55000) is out of scope. | Committed scope is 2016+ and only per tested model; Orsay sets (2010–2013) are out of scope. | R §16.1; DA §Q1; R §2.1 |
| **H/J 2014–15 is disputed.** openHAB reports PIN+encryption and no support; HA reports an encrypted subset supported and auto-detected. | Committed WS target treats H/J as unsupported unless the encrypted-legacy path is built and device-tested; treat as best-effort, never average the two reports. | R §16.1; DA §Q1 |
| **WSS 8002 is preferred; if the TV supports 8002 it must be used or keyCode fails.** 8001 is legacy/insecure. | No silent WSS→WS downgrade; a pairing/transport failure must surface, not fall back invisibly. | R §16.1; R §3.4; D2, Claims |
| **No Samsung key-support matrix** for external clients; app-list API removed ~2019/2020; Frame art API removed 2022, restored in fw 1622+. | Capability flags and “unsupported” outcomes are unavoidable; model/firmware/region variance is the operating condition. | R §3.5; DA §Q1 |
| **Self-signed certificate on 8002; pin per TV.** Certificate scope (per-device vs model-shared) is unverified. | The report calls for a visible trust/re-pair moment; “identity changed” is a real state. | R §3.4; D2, Claims |
| **Discovery and wake assume one broadcast domain.** SSDP multicast and WOL broadcast fail across VLANs, guest/IoT SSIDs with client isolation, or multicast filtering. Unicast control can cross routed subnets if routing/firewall allow. | Manual IP entry must always exist; discovery failure is normal, not exceptional. Wake cannot cross subnets without directed-broadcast forwarding. | R §5, §6.4, §16.1 |
| **MulticastLock required** around SSDP; `CHANGE_WIFI_MULTICAST_STATE`. | Discovery has a start/stop lifecycle and can be unreliable without the lock. | R §6.4; D3, Claims |
| **Standby/WOL:** “Power On with Mobile” master switch; WoWLAN primary, wired WoL also noted on ’16+ with LAN cable; prior connection registers the phone MAC. | Wake requires TV-side setup and is not uniformly available; per-model/network validation is required. | R §16.5; DA §Q8 |
| **SmartThings is capability-based, not arbitrary input.** Home API is restricted preview; ~12/min/device; paid tiers Oct 2026; 24h PATs push OAuth. | Cannot be the key/text/pointer path; at most an optional state/power supplement. | R §2.3; D4, Claims |
| **Text surfaces:** native Tizen `input`/`textarea` accept; browser URL recipe exists; Netflix/YouTube/Prime grids unknown/likely reject; password dialogs unknown. | Supported scope: native fields only, not “type anywhere”; streaming-app search remains D-pad. | R §16.6; DC §Q3 |
| **Pointer:** browser reportedly follows; grid apps reportedly ignore; coordinate space, cursor visibility, and click semantics unverified. | Pointer is per-app/per-model, not a universal trackpad. | R §4.4; DI §1 |
| **Voice:** no phone→TV transport; TV APIs app-local; STT fallback only. First-party mic varies by region/model. | Voice is a phone-side command feature, not a TV microphone. Raw audio is not a supportable promise. | R §4.3, §15.1; DV |
| **DLNA:** DMR/AVTransport must be discovered per TV; RenderingControl only if advertised. No official per-model DLNA matrix; USB codec tables are not DLNA guarantees. | Report scope: casting is limited to discovered/validated renderers and per-format capability, not “all Samsung TVs.” | R §4.6, §16.8; DC §Q7 |
| **DLNA codecs:** V1 JPEG/MP3/H.264-AAC MP4; ladder H.264 all → HEVC 2016+ → VP9 2019+ → AV1 2022+ → no Dolby Vision; TrueHD at best PCM. DTS absent 2021+; WMA dropped 2025+; RA/RMVB 2026+ (USB/player scope). | Format support is a ladder with per-model gaps; the evidence does not support uniform playback. | R §16.8; DC §Q7 |
| **Subtitles:** same-basename SRT + UTF-8 is USB guidance; external SRT over DLNA repeatedly reported missing; PGS needs burn-in; UMS per-device `.conf` is the established pinning method. | Subtitle availability is a per-model/renderer variable, not a default. | R §16.8; DC §Q7 |
| **DLNA live streams:** AVTransport is bounded-file-oriented; Samsung DMR live-feed failures reported; no verified model accepts phone-hosted live RTSP/RTMP/live-TS/HLS. | Live casting is not a reliable promise; mirroring is a different problem. | R §15.2; DM, Claims |
| **Mirroring:** MediaProjection single-use consent; `mediaProjection` FGS + notification/status chip; Android 15 QPR1 auto-stop on lock. No third-party Miracast start; Smart View SDK is app-to-app with a TV receiver; Cloud/DMP paths removed. | Mirroring needs a receiver the user must obtain/run; each session has a visible consent and capture indicator. | R §4.7; DM, Claims |
| **WebRTC receiver:** Tizen 2022+ game mode; `use.game.mode`; `a=imageattr`; FHD cap 1920×1080; one audio + one video track; background track disable; FIR handling. Tizen Web Engine WebRTC support is recent; older sets partial/absent. | Best-case latency is sub-second, but only on 2022+ with a custom receiver; pre-2022 is unknown/limited. | R §15.2; DM, Claims |
| **Receiver distribution:** Seller Office model-group review, US-default, phased updates; closed beta/alpha; Developer Mode sideload (Tizen 7+ cert signing); USB fallback unreliable. | Report verdict: pilot via sideload, scale via store. | R §16.4; DA §Q6 |
| **Android platform:** targetSdk 36 required for new/updated Play apps since 2026-08-31; minSdk 26 settled; local-network runtime permission for target 37+; cleartext HTTP to TV needs a reviewed exception; unsafe TrustManager is Play-flagged; `dataSync` FGS capped 6h/24h. | Permission/setup friction is part of the product surface; security policy constrains pairing implementation. | R §7.1, §7.3, §7.4; D3, Claims |
| **Remote socket lifecycle:** open only while the remote UI is active; no always-on FGS for keys. | Leaving the remote surface can strand an in-flight command; reconnection on return is normal. | R §6.3 |
| **No cloud fallback for keys** once SmartThings is out of the key path. | Local network failure is a hard failure for control; manual IP is the only fallback. | R §5 |

## Competitive/interface idioms worth noting

**No consumer-app UI teardown exists in the corpus.** There are no screenshots, navigation maps, control layouts, or interaction recordings for SmartThings, Smart View, or third-party apps. The items below are protocol/platform idioms that comparable apps must accommodate, not observed visual patterns and not prescriptions.

| Idiom | What was observed | Source |
|---|---|---|
| Physical remote as pairing authority | The TV’s Allow/Deny overlay is answered on the TV, not in the phone app. | R §3.3 |
| TV-side access-notification setting | Newer TVs default to ask-every-time; “First Time Only” + Device List purge is the documented way to stop re-prompts. | R §16.1; DA §Q1 |
| Separate Wake and Power | The report’s UX note calls for a Wake action distinct from Power, behind a setup checklist, with honest failure messages. | R §4.5 |
| Explicit field-bound send/cancel for text | Beta’s keyboard contract uses an explicit send action, focus state, and cancel semantics rather than live keystrokes. | R §4.2, §10 |
| Capability-gated controls | Controls are hidden or gated unless the model/surface passes a probe; unsupported keys report instead of failing silently. | R §4.1, §4.4; R §10 |
| Submitted / unconfirmed / observed outcomes | Beta/AppT contracts separate local write, lack of confirmation, and observed TV effect. | R §4.1, §10 |
| Explicit target selection for multi-TV | One credential per TV; no implicit last-TV; target selection required when multiple TVs exist. | R §3.3, §5 |
| Session-scoped foreground notification | Cast/mirror sessions are user-started FGS with an ongoing notification; mirroring also shows a status-bar chip. | R §6.3, §4.7; D3 |
| Android consent sheet: entire screen vs single app | Android 14+ MediaProjection consent offers both; the app can opt out of single-app via `MediaProjectionConfig` (OEM-overridable). | DM, Claims |
| Per-device DLNA renderer profile | UMS uses per-device `.conf` files to pin DLNA vectors; no Samsung matrix exists. | DC §Q7 |
| App launch by deep-link ID, not text | App names are launched via out-of-channel IDs; region/model availability varies. | DC §Q4 |
| Voice set ranked by mapping confidence | 1:1 keys first; app names via deep link; short dictation only in native IME; pointer paths excluded; tap vs hold distinguished. | R §16.7 |
| Hover as a first-class pointer state | Samsung’s TV input model treats hover separately from move/click, unlike a phone touch model. | DI §1 |
| Manual IP fallback | Discovery can fail on ordinary network setups; manual IP entry is always offered. | R §3.1 |
| TV-fetches-from-phone cast model | The phone is a control point and media server; the TV pulls the URL. This is not a push/mirror model. | R §4.6; DB §Q5 |
| DLNA ≠ mirroring; DIAL = launch | The corpus repeatedly separates these; DIAL only discovers/launches apps. | R §9; DM, Claims |
| No unqualified “universal” claim | The report requires per-model capability detection and a physical test matrix; “universal” is only gated per model. | R §0, §8, §11.2 |

## Risks, unknowns, and contradictions

### [RISK]

- **[RISK] Reverse-engineered control channel.** No Samsung compatibility contract; behavior depends on model/firmware; the report calls it the largest five-year regret risk. (R §0, §8; D4, Claims)
- **[RISK] No measured latency/reliability.** The corpus has no fleet-wide failure rates, latency figures, or support-burden data; “responsive” cannot be substantiated. (R §1; R §13 #11)
- **[RISK] Pairing friction.** Physical remote required; repeated prompts on some firmware; newer TVs ask every time; deny/timeout leaves no control path. (R §3.3, §16.1)
- **[RISK] Text-scope expectation gap.** Only native fields are supported; streaming-app search grids likely reject; a “type on your phone” claim would exceed the evidenced scope. (R §4.2, §16.6; DC §Q3)
- **[RISK] Pointer looks broken.** Major streaming apps ignore pointer events; coordinate/cursor behavior unverified. (R §4.4)
- **[RISK] Voice no-go.** No TV transport; STT fallback unmeasured; microphone permission; app-name mapping region-dependent. (R §4.3, §15.1; DV)
- **[RISK] Wake unreliability.** Per-model/network/setting failures; deep sleep; wired + soundbar ARC reports; waits up to ~1 min. (R §16.5; DA §Q8)
- **[RISK] Cast variability.** Codec/subtitle/live-stream gaps; phone server lifetime; `content://` not fetchable; no DLNA matrix. (R §4.6, §16.8; DC §Q7; DM)
- **[RISK] Mirroring receiver gap.** No broadly compatible third-party Samsung receiver/transport; highest regret risk; receiver distribution burden. (R §4.7, §8, §16.4)
- **[RISK] SmartThings not viable for keys.** Rate limit, cloud/account, paid/unsettled terms; cannot carry keyboard/pointer. (R §2.3)
- **[RISK] Android platform friction.** Android 17 local-network permission; cleartext HTTP exception; Play TrustManager policy; FGS declarations. (R §7.3, §7.4; D3)
- **[RISK] Certificate scope.** If the TV key/certificate is shared across a model line, the pin may not identify the individual TV. (R §3.4; D2, Claims)
- **[RISK] Session bleed.** Control, cast, and mirror need separate owners; target/receiver changes must not stop unrelated sessions. (R §6.1)
- **[RISK] WebRTC constraints.** 2022+ game mode, FHD cap, one audio + one video track, FIR handling; pre-2022 limits unknown. (R §15.2; DM, Claims)

### [UNKNOWN]

- **[UNKNOWN] Latency, failure rates, support burden.** No measured data anywhere. (R §1)
- **[UNKNOWN] Certificate/key scope.** Per-device vs model-shared. (R §3.4; R §12 #2)
- **[UNKNOWN] Pointer matrix.** Absolute coordinates, scaling, cursor visibility, per-surface follow/ignore, click semantics. (R §4.4; DI §1; R §12 #12)
- **[UNKNOWN] Text matrix.** Netflix/YouTube/Prime grid acceptance, password dialogs, IME event reliability, independent focus detection. (R §16.6; DC §Q3)
- **[UNKNOWN] Voice raw channel.** Frame format, method, codec, sample rate, chunking, TV ingestion endpoint; Pastebin blobs unretrieved. (R §15.1; DV, Claims)
- **[UNKNOWN] WOL behavior.** Directed vs limited broadcast, ARP expiry, Wi-Fi vs Ethernet ranking, deep-sleep behavior per model. (R §16.5; DA §Q8)
- **[UNKNOWN] DLNA coverage.** Which TVs advertise DMR/AVTransport, which codecs/subtitles work through the renderer, whether any accept live URIs. (R §16.8; DM, Claims)
- **[UNKNOWN] WebRTC receiver.** Pre-2022 browser limits, LAN STUN/TURN-less handshake, measured latency/battery, receiver install/retention. (R §15.2; DM, Claims)
- **[UNKNOWN] SmartThings terms.** Final October 2026 pricing, quotas, endpoint scope, PAT/OAuth details. (R §2.3, §12 #7)
- **[UNKNOWN] H/J 2014–15.** Contradictory support reports; no per-model resolution. (R §16.1; DA §Q1)
- **[UNKNOWN] Target SKUs/regions/firmware.** The report returns this for user decision; no final list. (R §12 #1)
- **[UNKNOWN] User demand and priorities.** No user research, app reviews, or usage data; which jobs matter, to whom, and how often is unknown. (B; R §1; M, verification entries)
- **[UNKNOWN] Willingness to install/sideload a TV receiver.** No user evidence; distribution is a technical path, not a validated adoption path. (R §16.4; DA §Q6)
- **[UNKNOWN] Cast session-end definition.** “Stop when the session ends” is stated; what ends a session (app kill, target switch, playback stop, TV-side stop) is not. (R §4.6)
- **[UNKNOWN] Power-off observability.** A TV that is off cannot confirm; how long to wait before showing “asleep” is undefined. (R §4.5)

### [CONTRADICTION]

- **[CONTRADICTION] Voice.** A January 2026 reverse-engineering discussion claims raw audio over the WS channel; an October 2026 implementation omits voice; Input B claims raw audio is impossible/NDA-gated. The report resolves to “no-go for roadmap; raw claim unverified.” (R §4.3, §15.1; R §13 #5; DV, Claims)
- **[CONTRADICTION] Pointer.** Input B’s summary says “robustly supported”; its body reports severe app incompatibility; the report resolves to experimental, capability-gated. (R §4.4, §13 #4)
- **[CONTRADICTION] Text.** Input B says “robustly supported” and puts Base64 text in `DataOfCmd`; the verified wire format puts it in `Cmd` with `DataOfCmd: "base64"`; scope is native fields only. (R §4.2, §13 #3)
- **[CONTRADICTION] Mirroring.** Input B presents an RTSP + AVTransport pipeline as feasible; Input A says no-go without a receiver; the report keeps Input B’s pipeline as an unverified spike. (R §4.7, §13 #8)
- **[CONTRADICTION] Background execution.** Input B says always-on foreground service (`dataSync`); Input A says lifecycle-scoped remote; the report rejects always-on and notes `dataSync` is time-capped. (R §6.3, §13 #6)
- **[CONTRADICTION] Subnets.** Input B says “complete application failure” and WebSocket cannot cross subnets; Input A says manual IP works; the report resolves that unicast control can cross routed subnets, while discovery and wake cannot. (R §5, §13 #9)
- **[CONTRADICTION] Key latency.** Input B says “near-zero latency”; no measurements exist; the report marks it unmeasured. (R §4.1, §13 #11)
- **[CONTRADICTION] Wake.** Input B says “rock-solid”; openHAB/HA/community reports show per-model failures, deep-sleep issues, and wired/soundbar quirks. (R §13 #14, §16.5; DA §Q8)
- **[CONTRADICTION] H/J 2014–15.** openHAB: PIN+encryption, not supported. HA: encrypted subset supported and auto-detected. The report marks the generation DISPUTED and forbids averaging. (R §16.1; DA §Q1)
- **[CONTRADICTION] SmartThings quotas.** Input A treats the ~12/min figures as a single-source, endpoint-sensitive warning; Input B presents a firm quota table. The report keeps the table with the caveat. (R §2.3, §13 #15)
- **[CONTRADICTION] UPnPCast provenance.** The v1.3.0 release note says Maven Central; Sonatype Central lists only 1.1.2 (2025-06-12); releases are JitPack-distributed. This affects the cast stack, not directly the user-facing UX. (R §16.3; DB §Q5)
- **[CONTRADICTION] Evidence quality, not behavior.** AppT’s module README says discovery is the only public operation; the source exposes open/forget/sessions; the root README says saved reconnection works. Treat README status as stale. (R §10)

## Direct quotes worth keeping

**No verbatim end-user quotes exist in this corpus.** The following are exact report/contract/problem statements, not user voice; they are the closest available wording for problem framing.

| # | Quote | Source |
|---|---|---|
| 1 | “The user must answer it with the physical remote.” | R §3.3 |
| 2 | “Grid/D-pad apps such as Netflix, Prime Video and YouTube often ignore pointer events, which makes the feature look broken.” | R §4.4 |
| 3 | “Do not market it as typing into Netflix/YouTube/etc. search.” | R §4.2 |
| 4 | “an explicit, field-bound "send" action with its own focus and capability state and send/cancel semantics, not a stream of phone keystrokes” | R §4.2 |
| 5 | “A successful socket write is not a confirmed TV action.” | R §4.1 |
| 6 | “If the phone sends but the TV or app ignores it, classify it as unsupported, not as a success.” | R §11.1 |
| 7 | “a "Wake" button separate from "Power", and honest failure messages.” | R §4.5 |
| 8 | “wait up to ~1min after power-off before WOL (TV not yet in standby)” | R §16.5 |
| 9 | “No broadly compatible Samsung receiver or transport” | R §8 |
| 10 | “Newer sets default Access Notification to ask-every-time” | R §16.1 |
