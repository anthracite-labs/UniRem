# Spec extract — UX-relevant

Extraction only. No design decisions, no screens, no flows proposed.

Citation keys: `spec Lnn` = `spec-samsung-remote.md` line; `protocol §X Lnn` = `protocol-reference.md`; `matrix §X Lnn` = `verification-matrix.md`; `memlog Lnn` = `.memlog.md`.

## Source files

| File | Role |
|---|---|
| `spec-samsung-remote.md` | Canonical contract: Why, CAP-1..CAP-8 (intent + success), 13 constraints, 6 non-goals, success signal, assumptions, open questions. Declares itself "the complete, preservation-validated contract" with companions (L7). |
| `protocol-reference.md` | Wire detail the kernel cites: ports/schemes, pairing lifecycle, message frames, channel events, TLS rule, wake packet, DLNA flow. |
| `verification-matrix.md` | Spikes A/A2/B/C/V/M/L in run order, observed-effect probes, per-model matrix template, MVP green-light gates. |
| `.memlog.md` | Decision log: capability/constraint restatements, user calls on SKU floor, text scope, voice scope, cast ladder, minSdk, mirror distribution, wake commitment. Also records spec-time questions whose resolution status matters (see Gaps). |

## Product intent and success signal

**Why (verbatim, spec L13):** "Samsung TV owners juggle the physical remote, the first-party app, and separate casting/mirroring flows — no single native app covers responsive local keys, phone-keyboard text, voice, a touchpad pointer, DLNA casting, wake, and mirroring against Tizen TVs. UniRem makes that one Android app exist for Samsung Tizen TVs (2016+ committed, 2014–15 H/J as unsupported best-effort), built capability-gated per model because the local control channel is community-reverse-engineered with no vendor compatibility contract."

**Success signal (verbatim, spec L69):** "A user pairs a recent Tizen TV plus a second older set, then drives keys, sends a text string, moves the pointer, casts a photo and a video, powers off, wakes, and mirrors through the receiver — every step with observed on-TV effect — while the matrix records model, firmware, and network context per result. That demonstration, not merged code, is the release gate."

Load-bearing terms to preserve: **capability-gated per model**, **observed on-TV effect**, **submitted/unconfirmed/observed**, **no vendor compatibility contract**, **unsupported best-effort**, **"universal" means gated per model, never blanket** (spec L61).

Implication: the product's claim surface is evidence-bound. Any "works with your TV" statement must be traceable to a matrix row, and the proof of quality is on-TV behavior, not app-side success (spec L45–46, L69).

## Capabilities (verbatim IDs and names)

No separate display names exist; only IDs and intent text. UX vocabulary must not rename them.

### CAP-1 — core TV keys
- **Intent (verbatim):** "User can drive core TV keys (D-pad, OK, back, home, volume, mute, play/pause) from the phone with observed on-TV effect." (L17–18)
- **Success (verbatim):** "On each matrix-passing model, every MVP key press produces the corresponding on-TV response; outcomes distinguish submitted/unconfirmed/observed, and unsupported keys report instead of failing silently." (L19)
- **User must see/do:** per-press outcome distinguishable across three levels; unsupported key yields a report, not a dead button or silent no-op. "On each matrix-passing model" ties the promise to the matrix, not to the app install.
- **Gating language:** "matrix-passing model"; "unsupported keys report instead of failing silently."

### CAP-2 — phone-typed text into native field
- **Intent (verbatim):** "User can send phone-typed text into the TV's focused native text field or browser URL field as an explicit send action." (L21)
- **Success (verbatim):** "Sent string appears verbatim in a native Settings/browser field on matrix models, with send/cancel semantics and focus state; app-drawn character grids are excluded unless a per-surface probe passes." (L22)
- **User must see/do:** an explicit send action (not live/streaming keystrokes), a cancel affordance, and visible focus state. Text targets only already-focused native fields; the app cannot assume it may type anywhere.
- **Gating language:** "on matrix models"; "excluded unless a per-surface probe passes" (app-drawn grids). Matrix carries a distinct **Text native** and **Text app-grid** column (matrix L21), i.e. two separately gated surfaces.
- **Scope note:** decision recorded as "text native+browser" (memlog L22); grids are a non-goal as a launch promise (spec L64).

### CAP-3 — touchpad pointer
- **Intent (verbatim):** "User can drive an on-TV pointer from a phone touchpad surface where the TV surface follows pointer input." (L24)
- **Success (verbatim):** "Pointer visibly moves and click activates on models passing the cursor probe; the feature stays hidden elsewhere with a plain-language explanation." (L25)
- **User must see/do:** pointer motion and click activation as the only accepted proof; where the probe fails the feature is absent but an explanation exists.
- **Gating language:** "where the TV surface follows pointer input"; "on models passing the cursor probe"; "hidden elsewhere with a plain-language explanation." Support is **per surface and per model** (protocol L26).

### CAP-4 — phone-mic voice
- **Intent (verbatim):** "User can control the TV by phone-mic voice covering the full key set plus short dictation, executed as mapped keys or text through the phone-side pipeline." (L27)
- **Success (verbatim):** "Key commands and dictated phrases from the agreed set execute with observed on-TV effect; dictation sends only into focused native fields; raw-audio injection stays behind the voice spike gate and out of launch commitments." (L28)
- **User must see/do:** voice resolves to discrete key actions or to text; dictated text obeys the same focused-field restriction as CAP-2. No promise of raw audio.
- **Gating language:** "the agreed set" (undefined — see Gaps), "behind the voice spike gate", "out of launch commitments". Ships as STT-to-command (matrix L27); "phone-side speech-to-text mapped to keys plus short dictation counts as the launch voice implementation" (L76).

### CAP-5 — power off / wake (two separate actions)
- **Intent (verbatim):** "User can power the TV off and wake it as two separate actions." (L30)
- **Success (verbatim):** "Power-off sends the power key with an asleep/offline state; Wake sends the magic packet from the stored MAC behind the setup checklist and reports success only when the TV answers on 8001/8002." (L31)
- **User must see/do:** two distinct actions, never one toggle. Wake is preceded by a setup checklist and requires a stored MAC. Wake success is defined by the TV answering on 8001/8002 — not by packet send.
- **Gating language:** "reports success only when the TV answers on 8001/8002"; wake is "committed per model on any network type — Wi-Fi standby included — only once the verification matrix records a pass for that model and network type; a model without a recorded pass surfaces no wake claim" (L52).

### CAP-6 — DLNA casting
- **Intent (verbatim):** "User can cast selected phone photo/audio/video to a discovered TV renderer and control playback, committing to rung-1 vectors with the ladder following per model." (L33)
- **Success (verbatim):** "JPEG/MP3/H.264-AAC short clips play with transport controls on validated renderers; HEVC, subtitles, seek, and 4K/long-file follow per model; serving is bounded to selected items with unguessable IDs, stops at session end, and survives screen-off during the session." (L34)
- **User must see/do:** item selection (only selected items are served), a discovered renderer target, transport controls for play/pause/stop (seek per model). Format support is a ladder, not uniform: JPEG/MP3/H.264-AAC first; HEVC, subtitles, seek, 4K/long-file follow per model.
- **Gating language:** "rung-1 vectors", "ladder following per model", "on validated renderers", "follow per model". Matrix columns: **DMR**, **JPEG/MP3/H264**, **HEVC**, **Subtitles**, **Seek** (matrix L21).

### CAP-7 — screen mirroring
- **Intent (verbatim):** "User can mirror the phone screen to the TV through the TV-side WebRTC receiver." (L36)
- **Success (verbatim):** "Mirrored output is visible on the receiver with latency and battery figures recorded per spike; mirroring without a TV-side receiver is excluded." (L37)
- **User must see/do:** mirroring is conditional on a TV-side receiver existing and running. The only accepted proof is output visible on the receiver.
- **Gating language:** "Mirroring without a TV-side receiver" is a non-goal (L60). V1 receiver "ships as a Tizen dev-mode sideload for pilot and matrix validation; a store listing is pursued only after CAP-7 latency and battery figures are recorded" (L55). Latency/battery are recorded, not given target values.

### CAP-8 — multi-TV pairing / persistence / switching
- **Intent (verbatim):** "User can pair, persist, and switch between multiple TVs with explicit target selection." (L39)
- **Success (verbatim):** "Two TVs pair and persist across restart per device; reconnect uses backoff; pairing-required and identity-changed states surface a re-pair flow." (L40)
- **User must see/do:** explicit target selection (no implicit "last TV"); paired TVs survive app restart; named states **pairing-required** and **identity-changed** each surface a re-pair path; reconnect visibly backs off.
- **Gating language:** "per device" persistence; "explicit target selection"; re-pair flow as the remedy for both named states.

## UX-bearing constraints

Every item below changes what a user sees, can do, or is told.

**Support and claims**
1. No Samsung compatibility contract; every remote feature ships behind a **provider** with **capability detection**, and "nothing claims support a physical test has not observed" (L45). Implies support labels are evidence artifacts, not marketing defaults.
2. Compatibility claims may not exceed the tested matrix; **"universal" means gated per model, never blanket** (L61). The word "universal" may not appear unqualified.
3. "No fleet-wide percentages without measured data" (matrix L27). Aggregate reliability numbers are prohibited.
4. H/J 2014–15 sets "are explicitly labeled **unsupported best-effort in product and UI copy** and never counted toward matrix pass claims" (L56). Same sets are "attempted as unsupported best-effort via the encrypted flow, with failure falling back to **declared-unsupported**" (L74).
5. The matrix records "model, firmware, and network context per result" (L69); wake is validated "per model and network type" (L52). Support is therefore a composite fact (model + firmware + network), not a per-model boolean.

**Command truthfulness**
6. "Socket-write success is never presented as TV action; command outcomes are **submitted/unconfirmed/observed**" (L46). No "sent = done" copy, no optimistic checkmarks.
7. CAP-1 requires unsupported keys to "report instead of failing silently" (L19) — a visible outcome for every press, including negative ones.

**Pairing, trust, re-pair**
8. Per-TV **trust-on-first-use SPKI pin** plus token in a client scoped to that TV; "the pin is validated before the token is sent, mismatch triggers a **visible re-pair**"; "no trust-all managers and no **silent WSS-to-WS downgrade**" (L47).
9. Pin is "confirmed in the pairing UI" (protocol L34) — pairing has an explicit user-visible confirmation moment.
10. Pairing material is Keystore-encrypted, "excluded from backup, never logged" (L48). Consequence: restore/device migration cannot carry pairings; no secret material may appear in error text or diagnostics.
11. First connect shows **Allow/Deny on the TV**, answered with the physical remote (protocol L16; assumption L75). Deny/timeout yields `ms.channel.unauthorized` or a close (protocol L17).
12. Later connects attach `&token=` and skip the prompt, but the app must "design for re-pairing since some firmware re-prompts" (protocol L19).
13. Pairing is keyed by "stable device identity (MAC/device ID), never by IP alone" (protocol L18) — an IP change must not read as a new or changed TV.

**Session and lifecycle boundaries**
14. "The remote socket lives only while the remote UI is active" (L49) — leaving the remote surface ends the control channel.
15. Foreground services "exist only for user-started cast/mirror/voice sessions with the matching service type" (L49) — keys/text/pointer have no background service; cast has session-scoped continuity.
16. Cast "survives screen-off during the session" (L34, L49) but "stops at session end" (L34). Session-end definition is not specified.
17. Cloud remote "stays out of the key-repeat hot path; key, text, and pointer traffic is **local-LAN only**" (L53).
18. Discovery and wake "assume one broadcast domain with **manual IP entry always offered**, and isolation is explained in **onboarding diagnostics**" (L51).

**Casting specifics**
19. Casting is "**TV-fetches-from-phone**: the TV receives an http(s) URL never a content:// URI, and the phone server supports HEAD and Range with **stable reachability during the session**" (L50). Any reachability break is a user-visible cast failure mode; the UI never hands a local file handle to the TV.
20. Serving is "bounded to selected items with **unguessable IDs**" (L34) — scope is the selection, not the library.

**Wake specifics**
21. Wake is committed per model "on any network type — Wi-Fi standby included — only once the verification matrix records a pass for that model and network type; **a model without a recorded pass surfaces no wake claim**" (L52).
22. Wake needs the stored MAC and the TV's "**Power On with Mobile**" / Network Standby setting (protocol L38); CAP-5 places this "behind the setup checklist" (L31).

**Mirroring specifics**
23. "The V1 mirroring receiver ships as a **Tizen dev-mode sideload** for pilot and matrix validation" (L55); "Mirroring ships only with a TV-side receiver the project builds or adopts; V1 distribution is the dev-mode sideload pilot" (L77).
24. Mirroring success is "output visible on the receiver" plus "latency and battery figures recorded per spike" (L37) — figures are recorded for the gate, no user-facing threshold is set.

**Platform / build**
25. targetSdk 36 with "Play requirement effective 2026-08-31"; minSdk 26 (L44). Matrix spike L requires "Full flow under **targetSdk-37 behavior** (discovery, socket, WOL, media server) before rollout" (matrix L13) — a version mismatch that directly affects local-network permission UX (see Tensions).

## Non-goals and exclusions

Verbatim (spec L58–65):
- "Mirroring without a TV-side receiver."
- "Compatibility claims beyond the tested matrix; \"universal\" means gated per model, never blanket."
- "Pre-2016 Orsay sets and the legacy port-55000 protocol."
- "Raw voice-audio injection as a launch commitment."
- "Text entry into app-drawn character grids as a launch promise."
- "An iOS client."

Additional exclusions stated elsewhere: port 55000 TCP is "Out of scope" (protocol L11); app-drawn grids excluded "unless a per-surface probe passes" (L22); raw injection "stays behind the voice spike gate and out of launch commitments" (L28); UPnPCast only after provenance/pinned artifact (L54); store listing for the receiver deferred (L55); Orsay excluded per research (L73).

## States and transitions implied by the spec

| # | State | Trigger | What spec says user sees / must do | Citation | Status |
|---|---|---|---|---|---|
| S1 | No TV paired / discovery | First run or no stored pairing | Discovery on one broadcast domain; manual IP entry always offered; isolation explained in onboarding diagnostics | spec L51 | explicit |
| S2 | TV found, unpaired | SSDP + `GET /api/v2/` succeeds | Model, `TokenAuthSupport`, `wifiMac` obtained; pairing can start | protocol L15 | explicit |
| S3 | Pairing prompt on TV (Allow/Deny) | First connect without token | User must answer on the TV **with the physical remote** | protocol L16; spec L75 | explicit |
| S4 | Pairing denied / timed out | Deny or timeout | `ms.channel.unauthorized` or close | protocol L17 | explicit (copy undefined) |
| S5 | Pairing UI pin confirmation | Before token is sent | Pin "confirmed in the pairing UI" | protocol L34 | explicit |
| S6 | Paired / token persisted | Allow + token received | Later connects skip the prompt | protocol L19 | explicit |
| S7 | Re-prompt by firmware | Some firmware re-asks despite token | Re-pair expected; "design for re-pairing" | protocol L19 | explicit |
| S8 | Identity changed | Pin/identity mismatch | State named **identity-changed**; surfaces re-pair flow | spec L40, L47; protocol L34 | explicit (definition of "identity" undefined) |
| S9 | Pairing required | Stored pairing unusable/absent | State named **pairing-required**; surfaces re-pair flow | spec L40 | explicit |
| S10 | Target selection (multi-TV) | ≥2 paired TVs | Explicit target selection required | spec L39 | explicit |
| S11 | Reconnecting | Dropped channel | "reconnect uses backoff" | spec L40 | explicit (timing undefined) |
| S12 | Submitted | Local socket write accepted | Outcome labeled **submitted** | spec L46 | explicit |
| S13 | Unconfirmed | No observed TV response | Outcome labeled **unconfirmed** | spec L46 | explicit (timeout undefined) |
| S14 | Observed | On-TV effect confirmed | Outcome labeled **observed** | spec L46; matrix L17 | explicit |
| S15 | Unsupported key pressed | Key outside model's gated set | Must "report instead of failing silently" | spec L19 | explicit (form undefined) |
| S16 | Capability hidden (pointer) | Cursor probe failed for this surface/model | Feature absent **with a plain-language explanation** | spec L25 | explicit |
| S17 | Text focus present | `ms.remote.imeStart` | Phone keyboard UI opens; manual type action still available | protocol L30; spec L22 | explicit |
| S18 | Text focus lost | `ms.remote.imeEnd` | Keyboard UI closes; send context gone | protocol L30 | explicit |
| S19 | Text send / cancel | User sends or cancels | "send/cancel semantics and focus state" | spec L22 | explicit |
| S20 | Dictation target invalid | No focused native field | Dictation "sends only into focused native fields" — must not send elsewhere | spec L28 | explicit (handling undefined) |
| S21 | Powering off | User invokes power-off | Power key sent; **asleep/offline state** presented | spec L31 | explicit (how "asleep" is known is not) |
| S22 | Asleep / offline | TV not answering | State exists as a presented condition | spec L31 | explicit |
| S23 | Wake in progress | User invokes Wake | Magic packet from stored MAC; success only on 8001/8002 answer | spec L31; protocol L38 | explicit (timeout undefined) |
| S24 | Wake unavailable / no claim | No matrix pass for this model+network | "surfaces no wake claim" | spec L52 | explicit |
| S25 | Setup checklist incomplete | Wake prerequisites unmet | Wake sits "behind the setup checklist" | spec L31 | explicit (contents undefined) |
| S26 | Cast selection | User picks items | Serving "bounded to selected items" | spec L34 | explicit |
| S27 | Cast session active | Renderer accepted URL, playback starts | Transport controls; survives screen-off | spec L34, L49 | explicit |
| S28 | Cast session ended | Session end (definition unspecified) | Server stops; playback path ends | spec L34 | explicit (trigger undefined) |
| S29 | Cast format unsupported | HEVC/subtitles/seek/4K/long file not passed for model | "follow per model" — per-model variance | spec L34; matrix L21 | explicit (per-format surfacing undefined) |
| S30 | Mirror unavailable | No TV-side receiver | Mirroring excluded; receiver is a dev-mode sideload | spec L37, L55, L60 | explicit |
| S31 | Mirror session active | Receiver accepting stream | User-started foreground service; output visible on receiver | spec L37, L49 | explicit |
| S32 | Voice session active | User-started voice | Foreground service with voice service type | spec L49 | explicit |
| S33 | Local-network permission denied | OS denies local network access | Spike L must cover "full flow under targetSdk-37 behavior (discovery, socket, WOL, media server)" | matrix L13 | **[INFERRED]** — permission is named, denial UX is not |
| S34 | TV unreachable / different subnet | Discovery finds nothing; isolation | Manual IP entry; isolation explained in onboarding diagnostics | spec L51 | **[INFERRED]** from the stated fallback |
| S35 | Another client connects/disconnects | `clientConnect` / `clientDisconnect` | Events exist on the channel | protocol L30 | **[INFERRED]** — no user-visible handling specified |
| S36 | Protocol error | `ms.error` | Event exists | protocol L30 | **[INFERRED]** — copy undefined |
| S37 | Pairings lost after restore | Backup exclusion | Must re-pair; no silent expectation of continuity | spec L48 | **[INFERRED]** — consequence of exclusion, not stated |

## Protocol/behavior facts that shape interaction

**Ports and transport**
- `8001` HTTP/WS: device info `GET /api/v2/`; legacy insecure remote on some models; "Plain text; never silently fall back from WSS" (protocol L9).
- `8002` HTTPS/WSS: authenticated channel `wss://<ip>:8002/api/v2/channels/samsung.remote.control?name=<base64(appName)>[&token=<token>]`; self-signed TV cert, pin per device (protocol L10).
- `55000` TCP Orsay: out of scope (protocol L11).
- All control traffic is local-LAN only (spec L53). No WAN path is defined for keys/text/pointer.

**Pairing**
- SSDP M-SEARCH to `239.255.255.250:1900` under a held `MulticastLock`, then `GET <tv>:8001/api/v2/` for model, `TokenAuthSupport`, `wifiMac` (protocol L15). Discovery requires multicast to work; `MulticastLock` implies discovery has a start/stop lifecycle.
- TV shows Allow/Deny for the physical remote (protocol L16) — pairing pauses on a second device the user must physically reach.
- Allow yields `ms.channel.connect` carrying the token; deny/timeout yields `ms.channel.unauthorized` or close (protocol L17).
- Token + pin persisted "per TV keyed by stable device identity (MAC/device ID), never by IP alone" (protocol L18).
- Some firmware re-prompts despite a stored token (protocol L19).
- Pin mismatch "throws on mismatch into a re-pair flow; one scoped client per TV; hostname check relaxed only inside the pinned client" (protocol L34).

**Keys**
- Click frame uses `Cmd:"Click"`, `DataOfCmd:"KEY_ENTER"`, `Option:"false"`, `TypeOfRemote:"SendRemoteKey"` (protocol L23).
- Hold uses `Press`/`Release` or a repeat loop **with coalescing** (protocol L23) — hold produces a rate-limited stream, so UI must tolerate coalesced repeats.
- MVP key names enumerated: `KEY_UP/DOWN/LEFT/RIGHT/ENTER/RETURN/HOME/MENU/SOURCE/GUIDE/INFO/TOOLS/POWER/VOLUP/VOLDOWN/MUTE/CHUP/CHDOWN/CH_LIST/0-9/PLAY/PAUSE/STOP/FF/REWIND/RED/GREEN/YELLOW/BLUE`; "anything beyond the MVP set sits behind capability flags" (protocol L24).
- Key latency is measured in spike A (matrix L7) but no target number appears anywhere.

**Text**
- Base64 UTF-8 in `Cmd`, `DataOfCmd:"base64"`, `TypeOfRemote:"SendInputString"`, then `SendInputEnd`; emit `custom.remote.textReceived` before the first text after IME start (protocol L25). Text arrives as a batch terminated by an end frame.
- `ms.remote.imeStart`/`imeEnd` "open/close phone keyboard UI, plus a **manual type action since delivery is inconsistent**" (protocol L30). Focus signals are unreliable, so the manual path is permanent, not a fallback for edge cases.

**Pointer**
- Move frame: `Cmd:"Move"`, `Position:{x,y,Time:"0"}`, `TypeOfRemote:"ProcessMouseDevice"`; "support is per surface and per model" (protocol L26).
- Accepted proof: "cursor visibly moves, click activates" (matrix L17).

**Voice**
- Launch implementation is "Phone STT→keys/text build with the agreed command set, latency measured" (matrix L11); raw injection "needs Pastebin retrieval plus a proven TV ingestion endpoint before any roadmap claim" (matrix L11).

**Cast**
- SSDP discovery of a MediaRenderer advertising AVTransport "(plus RenderingControl for volume)" (protocol L42).
- SOAP `SetAVTransportURI` with phone URL + DIDL-Lite metadata (`protocolInfo`, `DLNA.ORG_OP=01` for byte-seek); `Play`/`Pause`/`Stop`/`Seek`; TV fetches via `GET`/`HEAD`/`Range` (protocol L42).
- Subtitles "ride extra DIDL-Lite metadata and **vary by firmware**"; validation starts with JPEG, MP3, H.264/AAC-in-MP4 (protocol L42).

**Wake**
- 102 bytes (`FF`×6 + MAC×16) over UDP to ports 9 and 7, to subnet-directed broadcast plus `255.255.255.255`; "requires the stored MAC and the TV's 'Power On with Mobile' / Network Standby setting" (protocol L38).
- Confirmation is not the packet; it is the TV answering on 8001/8002 (spec L31; matrix L17).

**Mirror**
- Order B→A→C: WebRTC to the Tizen receiver first ("only low-latency path"), live-URI→AVTransport falsification second, DIAL/launch harness last; "Record latency and battery per transport" (matrix L12).

**Model/firmware variability and recording**
- Matrix rows are per "Model / year / region" with columns Firmware, Network (Wi-Fi band / Ethernet / VLAN), `/api/v2/`, Pair prompt, Token persists, Re-prompt?, Pin stable, Keys MVP, Text native, Text app-grid, Pointer, Wake, DMR, JPEG/MP3/H264, HEVC, Subtitles, Seek, Notes (matrix L21–23).
- Spikes must include "≥3 model years incl. one recent Tizen" for keys/pairing (matrix L7); green light requires "keys and pairing pass on ≥3 model years; text/pointer gated per surface; cast vectors pass on validated renderers; wake validated per network type; mirroring held to its receiver plus recorded figures" (matrix L27).
- "Socket-write success never counts" as evidence (matrix L15–17).

## Tensions or ambiguities the UX must resolve

1. **[TENSION] targetSdk 36 vs targetSdk-37 behavior.** Spec fixes targetSdk 36 (L44) while the local-network permission spike must run "under targetSdk-37 behavior (discovery, socket, WOL, media server)" (matrix L13). The OS behavior governing discovery and cast reachability — the most failure-prone user moments — is therefore stated two ways.
2. **[TENSION] Pointer "stays hidden elsewhere with a plain-language explanation"** (L25). A hidden feature has no surface from which to explain itself; the spec requires both invisibility and discoverable explanation without saying where the explanation lives.
3. **[TENSION] Power-off observability.** CAP-5 says power-off presents "an asleep/offline state" (L31), yet L46 forbids presenting socket-write success as a TV action. A TV that is off cannot confirm; the spec does not say what evidence justifies showing "asleep" or how long to wait.
4. **[TENSION] Wake confirmation timing vs. no timeout.** "Reports success only when the TV answers on 8001/8002" (L31) — TV boot can be long; no timeout, retry, or intermediate state is defined.
5. **[GAP] Mirroring requires a receiver the user must obtain by dev-mode sideload** (L55, L77), but CAP-7's success assumes it is already running (L37). No user-facing acquisition/enablement story exists, and no-state describes "receiver not installed / not running / wrong version."
6. **[GAP] Focus state is load-bearing but unreliable.** CAP-2 promises "focus state" (L22) and CAP-4 restricts dictation to "focused native fields" (L28), while the only focus signals (`imeStart`/`imeEnd`) are documented as "inconsistent" (protocol L30). No independent focus-detection mechanism is defined.
7. **[GAP] Capability granularity is undefined for copy.** Support is recorded per model + firmware + network (L69, L52; matrix L21). The spec never states at what granularity "supported" is shown to a user, nor how a model that passes on Ethernet but fails Wi-Fi standby reads.
8. **[TENSION] H/J offering vs labeling.** H/J sets are "attempted as unsupported best-effort" (L74) while being "explicitly labeled unsupported best-effort in product and UI copy" (L56) — an offer and a disclaimer on the same surface, with no rule for which dominates.
9. **[GAP] "Identity-changed" is undefined.** CAP-8 names the state (L40) and the pin is keyed by MAC/device ID "never by IP alone" (protocol L18), but what constitutes changed identity (new TV at same IP, MAC change, firmware identity change, cert rotation) is not specified.
10. **[TENSION] Remote socket dies with the UI.** "The remote socket lives only while the remote UI is active" (L49) while outcomes must reach observed/unconfirmed (L46) and cast "survives screen-off" (L34). Leaving the remote surface can strand an in-flight command with no terminal outcome.
11. **[GAP] Cast session-end is undefined.** Serving "stops at session end" (L34), but what ends a session (app kill, target switch, screen off, playback stop, TV-side stop) is not stated. Screen-off explicitly does **not** end it.
12. **[GAP] "Cloud remote" appears once** — "Cloud remote stays out of the key-repeat hot path" (L53) — but no capability, non-goal, or user-visible cloud path is defined anywhere else.
13. **[GAP] Voice command set never enumerated.** CAP-4 depends on "the agreed set" (L28) and matrix V on "the agreed command set" (L11); memlog records "voice keys+dictation" (L22) but no vocabulary, phrasing tolerance, or failure behavior.
14. **[GAP] Setup checklist contents undefined.** Wake sits "behind the setup checklist" (L31); prerequisites named elsewhere are stored MAC and the TV's "Power On with Mobile"/Network Standby setting (protocol L38), but no checklist items, order, or verification states are given.
15. **[GAP] Denied-pairing fallback was raised and never recorded as answered.** memlog lists "launch matrix SKUs + denied-pairing fallback" as an open question (L19), while the spec declares "None — all spec-time questions resolved" (L81). SKUs were resolved (L22); the denied-pairing fallback answer is absent.
16. **[GAP] Event handling absent.** `ms.error`, `clientConnect`, `clientDisconnect`, `ms.channel.ready`, and `ed.installedApp.get` ("where supported") exist as channel events (protocol L30) with no described user-visible consequence.
17. **[GAP] Multi-TV concurrency.** "One scoped client per TV" (protocol L34) plus explicit target selection (L39) does not define what happens to an active cast/mirror/voice session when the user switches targets, or whether two TVs can be driven at once.
18. **[GAP] Cast reachability failure.** "Stable reachability during the session" (L50) is a requirement, not a behavior: no state covers phone IP change, Wi-Fi drop, or TV losing the URL mid-playback.
19. **[GAP] No latency/quality thresholds anywhere.** "Responsive" (L13) and "low-latency path" (matrix L12) are unquantified; spike A only "Measure[s] key latency" (L7) and CAP-7 only records figures (L37). Copy promising responsiveness has no numeric backing.
20. **[TENSION] "Universal" is both the product promise and a restricted term** (L13 vs L61): the app is framed as the one app that covers everything, while the word may only be used in its gated sense.
21. **[GAP] Pairing assumes a physical remote is at hand** (L75). Deny/timeout (protocol L17) leaves a user without one with no stated path.
22. **[INFERRED] [GAP] Backup exclusion silently costs pairings.** "Excluded from backup" (L48) means restore/migration loses tokens, but no state or copy acknowledges the resulting re-pair.

## Exact vocabulary to reuse

**Capability and support**
- `CAP-1` … `CAP-8` (never renamed, never renumbered)
- capability-gated per model · capability detection · provider · gated per model, never blanket
- matrix · matrix-passing model · validated renderers · tested matrix · matrix pass claims
- unsupported best-effort · declared-unsupported · unsupported (as a key outcome)
- rung-1 vectors · the ladder (per model)
- MVP key set · MVP green-light gates

**Outcome and evidence**
- submitted / unconfirmed / observed (this order; three distinct levels)
- observed on-TV effect · observed-effect probes · socket-write success (never counts)
- physically observed · no claim without a recorded pass

**Pairing and identity**
- trust-on-first-use (TOFU) · SPKI pin · pin · certificate pin · pin stable
- pairing-required · identity-changed · re-pair · re-pair flow · re-prompt
- Allow / Deny · token · token persists · pairing UI
- target selection · explicit target selection · stable device identity

**TV conditions**
- asleep/offline state · Wake (capitalized, distinct from power-off) · magic packet
- Power On with Mobile · Network Standby · Wi-Fi standby
- broadcast domain · manual IP entry · onboarding diagnostics · MulticastLock

**Feature names as written**
- core TV keys · phone-typed text · native text field · browser URL field · focused native field · send/cancel · focus state · app-drawn character grids
- touchpad surface · pointer · cursor probe · click activates
- phone-mic voice · phone-side pipeline · phone STT · STT-to-command · short dictation · raw-audio injection / raw injection · voice spike gate
- DLNA casting · MediaRenderer · renderer · transport controls · TV-fetches-from-phone · session end · screen-off
- screen mirroring · TV-side WebRTC receiver · receiver · dev-mode sideload
- setup checklist · local-LAN only · cloud remote

**Matrix column labels (reuse verbatim when surfacing support)**
- Model / year / region · Firmware · Network (Wi-Fi band / Ethernet / VLAN) · `/api/v2/` · Pair prompt · Token persists · Re-prompt? · Pin stable · Keys MVP · Text native · Text app-grid · Pointer · Wake · DMR · JPEG/MP3/H264 · HEVC · Subtitles · Seek · Notes
