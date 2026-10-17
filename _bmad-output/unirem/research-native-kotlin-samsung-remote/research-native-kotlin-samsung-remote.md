---
title: 'technical research: native-kotlin-samsung-remote-feasibility'
type: 'technical'
topic: 'native-kotlin-samsung-remote-feasibility'
decision: 'Pick feasible native-Kotlin integration approach for Samsung TV control + DLNA/UPnP casting + mirroring + keyboard/voice/mouse inputs before committing roadmap'
source: 'external consolidated report (Input A decision register + Input B feasibility spec), producing tool unnamed, cut-off 2026-10-16, filed to imports/external-report-2026-10-16.md'
status: complete
preset: 'standard'
validation: 'normal'
created: '2026-10-16'
updated: '2026-10-16'
deepen: d1-full-vision-technical-2026-10-16, d2-open-questions-2026-10-16
---

# Research: native Kotlin Samsung Tizen remote + casting

**Status:** Consolidated research and decision record · **Research cut-off / access date:** 2026-10-16
**Audience:** Android engineering, product and roadmap owners
**Scope:** remote keys, phone-keyboard text, voice, mouse/touchpad, power (on/off), DLNA/UPnP audio/video/photo casting, screen mirroring. All of this runs from a native Kotlin Android phone app to Samsung Tizen TVs (2016+).

> **Where this came from.** This document merges and deduplicates two earlier inputs:
> - **Input A:** *Decision register: native Kotlin Samsung Tizen remote + casting*. It is evidence-graded, has a numbered source table, and includes a review of the AppT and Beta repositories.
> - **Input B:** *Technical Feasibility and Architectural Specification for Native-Kotlin Samsung Smart TV Integration*. It has more protocol and implementation detail but is often more assertive than its evidence allows.
>
> Where the two agreed, the claim appears once. Where they disagreed, the better-evidenced position wins, and the disagreement is recorded in [§13 Reconciliation log](#13-reconciliation-log). A few claims were spot-checked against primary sources during consolidation (marked *verified 2026-10-16*). Engineering judgement added during consolidation, rather than taken from a cited source, is marked **[Eng. note]**.

---

## Contents

0. [Decision in one minute](#0-decision-in-one-minute)
1. [Evidence conventions](#1-evidence-conventions)
2. [Landscape: which integration paths exist](#2-landscape-which-integration-paths-exist)
3. [Protocol reference: Samsung local WebSocket remote](#3-protocol-reference-samsung-local-websocket-remote)
4. [Feature-by-feature feasibility](#4-feature-by-feature-feasibility)
5. [Network topology and designing for failure](#5-network-topology-and-designing-for-failure)
6. [Native Kotlin architecture](#6-native-kotlin-architecture)
7. [Android platform, permissions and Play Store reality](#7-android-platform-permissions-and-play-store-reality)
8. [Ecosystem health and regret risk](#8-ecosystem-health-and-regret-risk)
9. [Comparison matrix](#9-comparison-matrix)
10. [Reference implementations reviewed (AppT, Beta)](#10-reference-implementations-reviewed-appt-beta)
11. [Recommended implementation plan and test matrix](#11-recommended-implementation-plan-and-test-matrix)
12. [Roadmap blockers and open questions](#12-roadmap-blockers-and-open-questions)
13. [Reconciliation log](#13-reconciliation-log)
14. [Sources](#14-sources)
15. [Deepen D1 — full-vision technical implementation (legalities parked, 2026-10-16)](#15-deepen-d1--full-vision-technical-implementation-legalities-parked-2026-10-16)
16. [Deepen D2 — spec open-question resolution (2026-10-16)](#16-deepen-d2--spec-open-question-resolution-2026-10-16)

---

## 0. Decision in one minute

**Proceed conditionally with two separate subsystems:**

1. A **local Samsung WebSocket remote** for core keys.
2. A **separate DLNA/UPnP casting subsystem**.

**Do not commit to "universal" keyboard, voice, mouse, power-on or screen-mirroring claims.**

Samsung does not support the local remote channel as a phone API. It has been reverse-engineered by the community. It is the most direct route to responsive local keys, but the evidence comes from community implementations, not a Samsung compatibility contract. Treat each feature as a *provider* with capability detection and per-model tests. [6][7][8]

**Keep SmartThings cloud out of the key path.** Its published per-device rate limits are too low for remote input. Its API also moved to paid tiers announced for October 2026, with terms not yet confirmed. At most it is an optional, later supplement for state or power. [13][14][49]

**Roadmap gate:**
- Green-light a core-remote + casting MVP **only after a real-TV matrix passes**: model year, firmware, region, network type, pairing, standby/wake, key set and supported media (see [§11](#11-recommended-implementation-plan-and-test-matrix)).
- Voice and mirroring are separate feasibility spikes. **Do not promise either in the first release.**

| Capability | Verdict | Confidence | Release posture |
|---|---|---|---|
| Remote keys (D-pad, OK, back, home, volume, media) | **Go (conditional)** | Medium. Consistent community evidence; no official contract | MVP, behind an adapter, with a physical-TV gate |
| Phone keyboard → TV text | **Conditional** | Low–medium; limited to certain fields | Optional MVP feature, gated by capability; native Tizen text fields only |
| Mouse / touchpad | **Experimental** | Low | Hidden unless capability tests pass on that model |
| Voice (phone mic → TV) | **No-go for roadmap** | Very low / conflicting | Spike only if it is a hard launch requirement |
| Power **off** | Conditional | Medium | Via remote key while the TV is reachable |
| Power **on** (wake) | **Conditional, separate feature** | Low–medium | Wake-on-LAN / "Power On with Mobile", with a setup check and physical validation |
| DLNA casting (photo/audio/video) | **Go (conditional)** | Medium for the protocol; low for Samsung model/codec coverage | Separate MVP, limited to discovered and validated renderers |
| Screen mirroring | **No-go without a receiver path** | — | Defer. Android can capture the screen, but no compatible Samsung receiver has been established |
| SmartThings cloud | Optional supplement | Low (terms unsettled) | Not in the key path; revisit after pricing, quotas and capabilities are confirmed |

> **Deepen D1 deltas (2026-10-16, technical only; see §15).**
> - Voice: still no-go for raw-audio-over-WS (#177 claim UNVERIFIED low); **buildable now via phone STT→keys/text**, raw channel experimental only [9][63][65][66].
> - Mirroring: still no zero-TV-app path; **spike-viable via TV-side WebRTC receiver** (Tizen 2022+ game-mode primitive VERIFIED high, Moonlight precedent) — spike order B→A→C [63][64][26].
> - Mouse/keyboard: conditional strengthened — wire format confirmed (no change), gate by observed-effect probes incl. per-surface pointer matrix [8][45].
> - Wake/cast: procedures + observed-effect probes added; casting core flow confirmed (no change) [12][15].
>
> **Deepen D2 deltas (2026-10-16, spec open questions; see §16).**
> - Library: **jUPnP 3.0.5 picked** (sole Central-provenance gate-passer); UPnPCast 1.3.0 runner-up with win-conditions; RendererGateway hedge; refresh after 2 quarters [17][19][20][23].
> - minSdk: **26 SETTLED** — no API-29-only dependency; 29 sheds ~5pp reach for nothing [76].
> - Wake/receiver: WoWLAN primary with year-split setting paths; store-scale vs sideload-pilot routes resolved [12][67][68][69].
> - V1 vectors + voice launch-set proposed; H/J 2014–15 marked DISPUTED — Q1/Q3/Q4/Q5 returned for user decision [50][51].

---

## 1. Evidence conventions

| Tier | Meaning | How to treat it |
|---|---|---|
| **Official** | Samsung, Google/Android or SmartThings documentation | Authoritative for what it actually covers. Watch for scope: several Samsung APIs are for apps running *on the TV*, not phone→TV control |
| **Standard** | UPnP Forum / DIAL specifications | Authoritative for the protocol; says nothing about whether a given TV implements it |
| **Community code** | Open-source implementations (samsungtvws, HA integrations, AppT) | Proves code exists and a contract was scripted. **Not** proof that a named TV/firmware works |
| **Community discussion** | Forums, issues, Reddit, Stack Overflow | Leads and hypotheses only |
| **Secondary press** | News coverage | Context; confirm against the primary source |
| **[Eng. note]** | Engineering judgement added during consolidation | Reasoned recommendation; validate in spikes |

No source reviewed gives measured, fleet-wide failure rates, latency figures or support-burden data. **Statements about reliability, latency or "most TVs" are qualitative unless a test result is cited.**

---

## 2. Landscape: which integration paths exist

### 2.1 Samsung local WebSocket remote (primary candidate for keys)

- Community implementations use the `samsung.remote.control` channel on the TV:
  - Usually **WSS on port 8002** with a per-TV authorization token.
  - Plain **WS on port 8001** in older or legacy setups.
  - Device information over HTTP at `/api/v2/`.
- Discovery usually combines SSDP with that `/api/v2/` query. [6][7][8][51]
- Samsung does **not** document this as a public phone API. Protocol details, key names, pairing prompts, token behaviour and compatibility are reverse-engineered and depend on model and firmware.
- Several independent community codebases report Tizen-era support. That is not Samsung certification. [6][7][8][52]
- **Pre-2016 (Orsay) TVs** used a different legacy TCP protocol, usually on port 55000. They are **out of scope** for this product. [50]

### 2.2 Official Samsung APIs, and why they are easy to misread

- **`TVInputDevice`** and **`VoiceInteraction`** describe input and assistant events for an app running **on the TV**. They do not document a phone-to-TV remote protocol. [2][3]
- **Smart View SDK** is app-to-app: a sender app plus a receiver app on the TV. Samsung says support for new Cloud TV apps ended after 2019, and support for its default media player (DMP) ended after 2021. The Android SDK download page still lists package 2.5.34, dated 2024-07-25. That is a maintenance signal, not evidence of universal remote commands. [4][5]

### 2.3 SmartThings (cloud): optional supplement only

- **Capability-based.** Which functions are available depends on the device. There is no evidence that SmartThings can inject arbitrary keys, text, pointer input or microphone audio. [16][29]
- **Home API (Matter):** the reference lists media-TV capabilities (`MediaPlayback`, `MediaTrackControl`, `KeypadInput`, `Switch`). However, it is a **restricted preview**. It needs authorization, a Samsung account, a Matter-enabled SmartThings hub, and a separately distributed Home Service APK. It is not a generally shippable universal-TV API. [16][29]
- **Rate limits.** The figures below come from the SmartThings rate-limit and quota pages as reported by the inputs. Input A treats them as a **single-source warning that depends on the endpoint**. Confirm which endpoints they apply to before relying on them. [13][47]

  | SmartThings limit (as published) | Quota | Impact on a remote app |
  |---|---|---|
  | Device command execution (per device) | ~12 requests/min | Fatal for key repeat. Rapid volume or D-pad presses would hit HTTP 429 within seconds |
  | Device state polling (per device) | ~12 requests/min | No real-time volume or channel state sync |
  | Execution lifecycles (per installed app) | ~20 requests/min | Limits state updates and syncing |
  | State events | 50 per request | Limits bulk status retrieval |

- **Commercial terms (unsettled at cut-off).**
  - On 2026-06-23/24 SmartThings announced paid API tiers targeting **October 2026**: a **$4.99/month Personal Plan** for non-commercial individual developers, plus commercial tiers with unpublished pricing and quotas. Free access was promised through Q3 2026. [14][48][49]
  - Samsung says the SmartThings app and developing device integrations stay free. Direct programmatic API access does **not** get a free tier. [49]
  - Secondary coverage also reports that SmartThings Personal Access Tokens created after 2024-12-30 are valid for only 24 hours, which pushes integrations towards OAuth. [49]
  - No source found by the cut-off confirms the final October terms as in force. **Re-check before any pricing, authentication or architecture decision that depends on SmartThings.**

### 2.4 UPnP / DLNA: the casting path, not a remote

- UPnP AV is a **media-renderer control** path. AVTransport defines setting a media URI and transport controls; RenderingControl covers volume. Which services and formats a TV actually exposes varies by device. [15]
- Many Samsung TVs act as UPnP **Media Renderers (DMR)**, but this must be **discovered per TV**, not assumed. [1][15][50]
- Samsung's general-specification page is useful for researching media formats by model. It does not prove a TV exposes a DLNA renderer or accepts a given profile over DLNA. [1]

### 2.5 DIAL

DIAL (DIscovery And Launch) discovers and launches TV apps. It is **not** a general power, remote-key or screen-mirroring protocol. [18]

### 2.6 What changed in the last 12 months (2025-10-16 → 2026-10-16)

- **No reliable primary source** shows a universal change to Samsung remote-protocol authentication or encryption in this period.
- `samsungtvws` **3.0.6** (2026-09-11) documents modern Tizen support plus caveats about subnets/VLANs and repeated authorization prompts. This shows the community is still maintaining it, not a Samsung guarantee. [6]
- **SmartThings moved to paid API tiers** (see §2.3). [14][49]
- **jUPnP 3.0.5** (2026-08-12) shipped, including an Android service-startup fix. [19][20]
- **UPnPCast v1.3.0** (2026-09-02) shipped, but its Maven Central publication is unclear (see §7.2). [17][23]
- **Google Play target API** requirement (effective 2026-08-31): new and updated phone apps must target API 36. [22]

---

## 3. Protocol reference: Samsung local WebSocket remote

> Everything in this section comes from **community code and discussion**, mainly `samsungtvws`. Message formats were checked against its current `remote.py` and `event.py` (*verified 2026-10-16*). [8][45] Treat every field as **firmware-dependent** and confirm it on hardware.

### 3.1 Discovery and device info

1. **Discover candidates** with SSDP on the local subnet: M-SEARCH to `239.255.255.250:1900`. Android needs a `WifiManager.MulticastLock` held while discovery runs (§6.4).
2. **Query device info:** `GET http://<tv-ip>:8001/api/v2/` (or `https://…:8002/api/v2/`). The JSON reportedly includes model name, OS/firmware data, whether token authentication is supported (`TokenAuthSupport`) and the Wi-Fi MAC (`wifiMac`). Persist the MAC: you need it for Wake-on-LAN (§4.5). [8][51]
3. **Always offer manual IP entry.** Discovery can fail on guest Wi-Fi, VLANs, AP isolation or networks that filter multicast (§5). [6]

### 3.2 Endpoints and ports

| Port / scheme | Use (as reported) | Notes |
|---|---|---|
| `8001` HTTP / WS | Device info (`/api/v2/`); legacy/insecure remote channel on some models | Plain text. **Never silently fall back** from WSS to WS (§3.4) |
| `8002` HTTPS / WSS | Authenticated remote channel: `wss://<ip>:8002/api/v2/channels/samsung.remote.control?name=<base64(appName)>[&token=<token>]` | Self-signed TV certificate; token after first pairing |
| `55000` TCP | Pre-2016 Orsay legacy protocol | Out of scope [50] |

### 3.3 Pairing and token lifecycle

1. **First connection** (no token): the TV shows an **Allow / Deny** overlay. The user must answer it with the physical remote. [7][8][53]
2. **Allow:** the socket receives an `ms.channel.connect` event. On token-auth models it carries a token in its data. **Deny / timeout:** expect `ms.channel.unauthorized` or the socket closing. [45][53]
3. **Persist the token per TV**, keyed by a stable device identity such as the MAC or device ID, not only the IP. Store it in Android Keystore-backed encrypted storage. Never log authorization payloads or tokens. Treat the token as an **opaque string**: community reports describe it as short (often 8 characters), but don't validate its format. [6][7][8]
4. **Later connections** add `&token=<token>` and should skip the prompt. Community packages document **repeated authorization prompts** on some models and firmware, so design for re-pairing. [6]
5. **Multiple TVs:** keep one credential/device record per TV and require explicit target selection. [6][7][8]

### 3.4 TLS on port 8002: pin per device, never "trust all"

The TV presents a **self-signed** certificate, and a LAN IP address cannot carry a publicly trusted certificate. So default Android/OkHttp validation fails, typically with `SSLHandshakeException` / "unable to find valid certification path".

**Rejected approach (from Input B):** a no-op `X509TrustManager` (empty `checkServerTrusted`) plus a `HostnameVerifier` that always returns `true`, installed globally. [54] Reasons to reject it:

- It turns any attacker on the LAN into a trusted TV and leaks the pairing token.
- **Google Play flags unsafe TrustManager implementations.** Apps that ignore certificate validation errors can be removed after the deadline set in Play Console. [55]

**Recommended approach:**

- **Trust-on-first-use, pinned to that TV.** On first pairing, capture the leaf certificate's SPKI hash and confirm in the UI (the TV's Allow prompt confirms physical presence). Store the pin with the token.
- On every later connection, `checkServerTrusted` **must throw** if the SPKI doesn't match. Show a visible **re-pair** flow when the certificate changes. **Validate the pinned identity before sending the stored token.** [34][35][41]
- **Scope it narrowly.** Build a dedicated `OkHttpClient` (or `SSLSocketFactory`) per paired TV. Never install a process-wide trust override. Relax hostname verification **only** on that pinned client: the certificate won't match the IP, so the pin replaces it.
- **Never downgrade** WSS → WS silently. [6][7][8]
- **Open question:** whether the TV key/certificate is unique to each device or shared across a model line has not been verified. If it is shared, the pin proves "a Samsung TV with this key" rather than "this TV", so also bind the device ID or MAC (§12 Q2).

```kotlin
// [Eng. note] Sketch only. Device-scoped TOFU pin; throws on mismatch (Play-safe pattern).
class PinnedTvTrustManager(
    private val pinnedSpkiSha256: ByteArray?,            // null ONLY during an explicit, user-started first pairing
    private val onFirstSeen: (X509Certificate) -> Unit,  // UI must confirm before the token/pin is persisted
) : X509TrustManager {
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        val leaf = chain.firstOrNull() ?: throw CertificateException("Empty chain")
        val spki = MessageDigest.getInstance("SHA-256").digest(leaf.publicKey.encoded)
        when {
            pinnedSpkiSha256 == null -> onFirstSeen(leaf)
            !MessageDigest.isEqual(spki, pinnedSpkiSha256) ->
                throw CertificateException("TV identity changed — re-pair required")
        }
    }
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
        throw CertificateException("Client auth not supported")
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
```

### 3.5 Message formats

All remote commands are JSON frames with `"method": "ms.remote.control"` sent over the `samsung.remote.control` channel. (Input B called this the "ms.remote.control channel", which mixes up the method and the channel.) [8]

**Key click** (`Cmd` may also be `Press` / `Release` for hold-to-repeat):

```json
{"method":"ms.remote.control","params":{"Cmd":"Click","DataOfCmd":"KEY_ENTER","Option":"false","TypeOfRemote":"SendRemoteKey"}}
```

Common keys in community references [8][46]:

| Group | Keys |
|---|---|
| Navigation | `KEY_UP`, `KEY_DOWN`, `KEY_LEFT`, `KEY_RIGHT`, `KEY_ENTER`, `KEY_RETURN` |
| System | `KEY_HOME`, `KEY_MENU`, `KEY_SOURCE`, `KEY_GUIDE`, `KEY_INFO`, `KEY_TOOLS`, `KEY_POWER` |
| Volume / channel | `KEY_VOLUP`, `KEY_VOLDOWN`, `KEY_MUTE`, `KEY_CHUP`, `KEY_CHDOWN`, `KEY_CH_LIST`, `KEY_0`…`KEY_9` |
| Media / colour | `KEY_PLAY`, `KEY_PAUSE`, `KEY_STOP`, `KEY_FF`, `KEY_REWIND`; `KEY_RED`/`GREEN`/`YELLOW`/`BLUE` |

Samsung publishes no key-support matrix for external clients, so keep anything beyond a conservative subset behind capability flags. [7][8]

**Text input.** The UTF-8 text is Base64-encoded into **`Cmd`**, and **`DataOfCmd` is the literal string `"base64"`**. This corrects Input B, which put the encoded text in `DataOfCmd`.

```json
{"method":"ms.remote.control","params":{"Cmd":"<base64(utf8 text)>","DataOfCmd":"base64","TypeOfRemote":"SendInputString"}}
{"method":"ms.remote.control","params":{"TypeOfRemote":"SendInputEnd"}}
```

`samsungtvws` sends an `ms.channel.emit` broadcast of `custom.remote.textReceived` before the first text after IME start, because *some TVs require it*. Copy this and test it. [8]

**Mouse move** (behaviour depends heavily on the model and the foreground app; §4.4):

```json
{"method":"ms.remote.control","params":{"Cmd":"Move","Position":{"x":10,"y":-4,"Time":"0"},"TypeOfRemote":"ProcessMouseDevice"}}
```

**Adjacent capability (not in scope, noted for completeness):**
- App launch via `ms.channel.emit` / `ed.apps.launch` (`DEEP_LINK` or `NATIVE_LAUNCH`).
- Installed-app list via `ed.installedApp.get`. This is "not available on all TVs". [8]

### 3.6 Events worth handling

| Event | Meaning / use |
|---|---|
| `ms.channel.connect` | Connected; may carry the token (§3.3) |
| `ms.channel.unauthorized` | Pairing denied or token rejected → re-pair flow |
| `ms.channel.ready`, `ms.channel.clientConnect` / `clientDisconnect` | Channel lifecycle |
| `ms.remote.imeStart` / `ms.remote.imeEnd` | A TV text field gained or lost focus → show or hide the phone keyboard UI |
| `ms.error` | Error with a message payload |
| `ed.installedApp.get` | App-list response (where supported) |

Event names are from `samsungtvws` `event.py` (*verified 2026-10-16*). [45] Input B's `MS_REMOTE_IME_START_EVENT` / `MS_REMOTE_IME_END_EVENT` are that library's *Python constant names*. The actual values on the wire are `ms.remote.imeStart` / `ms.remote.imeEnd`.

---

## 4. Feature-by-feature feasibility

### 4.1 Remote keys: Go (conditional), medium confidence

- **Best path:** the local WebSocket channel (§3). Community references consistently show key injection working on Tizen-era TVs. [6][7][8]
- **MVP set:** D-pad, OK/enter, back, home, volume, mute, play/pause. Map extra keys through a provider capability set, and report unsupported commands to the user instead of failing silently. [7][8]
- **Latency:** no measured data. Input B's "near-zero latency" is unsupported, so measure it in Spike A.
- **[Eng. note]** Add hold-to-repeat (Press/Release or a repeat loop), plus **coalescing/debouncing** so aggressive tapping can't flood the socket.
- A successful socket write **is not** a confirmed TV action. Model command outcome as *submitted / unconfirmed / observed*. [34][41]

### 4.2 Phone keyboard → TV text: conditional, low–medium confidence

- `SendInputString` reportedly works **only when a native Tizen text field (IME) is focused**. [7][8]
- Use `ms.remote.imeStart` / `imeEnd` to open and close the phone-side text UI. Event delivery is described as occasional or inconsistent, so also offer a manual "type" action. [8][45]
- **Do not market it as typing into Netflix/YouTube/etc. search.** Those apps often draw their own character grid, which may never raise the system IME. Samsung's own TV docs describe IME input for focused app text controls; they do not validate external WebSocket injection. [2][7][8]
- **Product contract:** text entry should be an explicit, field-bound "send" action with its own focus and capability state and send/cancel semantics, not a stream of phone keystrokes. [41]
- **Test** that the text actually *arrives* on screen, not just that the socket write succeeded.

### 4.3 Voice (phone mic → TV): no-go for the roadmap

- **Evidence conflicts.** A January 2026 reverse-engineering discussion claims raw voice over the channel. An October 2026 community implementation explicitly leaves voice out. [9][10]
- Input B claims raw-audio ingestion is impossible: that the TV exposes no socket for PCM/WAV, and audio pipelines are proprietary and reserved for partners under NDA. Its sources are a forum thread and a FAQ. That claim is **not verified** by primary documentation, but it points the same way: **no stable public third-party contract exists.** [61][62]
- Samsung's SmartThings phone-remote help says microphone/Bixby availability varies by region, model and assistant. That only describes **the first-party app**. [11]
- `VoiceInteraction` is an API for apps on the TV. [3]
- **Fallback, if voice is wanted:** do speech-to-text on the phone and map recognised intents to keys or text. That is a phone-side NLP feature. It needs `RECORD_AUDIO` and does **not** create a TV voice protocol. [3][11][27]
- **Decision:** exclude from roadmap commitments. Run a spike only if it is a hard launch requirement and a public API is identified (§12 Q4).
- > **Deepen D1 pointer:** raw-voice-over-WS stays UNVERIFIED (low); VoiceInteraction/VoiceControl confirmed TV-app-local (VERIFIED high); STT→keys/text path buildable now, latency unmeasured — see §15.1 [3][9][65][66].

### 4.4 Mouse / touchpad: experimental, low confidence

- The protocol accepts `ProcessMouseDevice` / `Move` frames (§3.5). [8][9]
- Input B reports that the Samsung web browser shows a cursor that follows these updates. Grid/D-pad apps such as Netflix, Prime Video and YouTube often **ignore pointer events**, which makes the feature look broken. [11][61]
- Pointer visibility, coordinate scaling, click semantics and firmware differences are **unvalidated**. Samsung's public TV API pages establish no phone-pointer protocol. [2][8][9]
- **Decision:** experimental provider, **disabled unless capability tests pass** for that model. No "universal touchpad" claim. Explain in the UI that pointer works only where the TV app supports it.
- > **Deepen D1 pointer:** mechanics confirmed (`Move` + separate Click/Press/Release; fire-and-forget stream); coordinate space + per-surface matrix still open — gate by observed-cursor probes, see §15.3 [8].

### 4.5 Power on/off: conditional, separate feature

**Power off.** Send `KEY_POWER` while connected. Expect the socket to drop as the TV enters standby. Represent *asleep/offline* as its own state. [12]

**Power on.** A sleeping TV's WebSocket server usually **isn't reachable**, so a power key sent to port 8002 will time out. Waking needs a separate path:

- **Wake-on-LAN (WOL).** Samsung documents **"Power On with Mobile"** as a TV setting and lists WOL as a wake reason. Whether it works depends on the model, settings (Network Standby / Power On with Mobile) and network state. [12]
- **Magic packet:** 102 bytes = 6 × `0xFF` + the TV's MAC address × 16. Send it over UDP to a **broadcast** address on port **9** (or 7). [57]
  - Unicast to the last-known IP often fails once the router's ARP entry has expired.
  - Routers do not forward broadcasts across subnets unless broadcast forwarding is configured. [58]
- **Requires the MAC address**, captured during discovery or pairing (`wifiMac` from `/api/v2/`, §3.1). Without it you can't wake the TV. [51][57]
- **[Eng. note]** Send to the **subnet-directed broadcast** address computed from the current network's `LinkProperties`, as well as `255.255.255.255`. Some Android devices and APs drop the limited broadcast.
- **[Eng. note]** A TV connected over Wi-Fi may wake less reliably than over Ethernet, depending on its standby radio policy. Make this a column in the test matrix.
- **SmartThings** may be able to switch power if the TV is registered and the plan/API allows. Validate third-party command access and current terms first. [12][13][16]
- **UX:** a setup checklist ("enable Power On with Mobile"), a "Wake" button separate from "Power", and honest failure messages.

```kotlin
// [Eng. note] Sketch only.
fun magicPacket(mac: String): ByteArray {
    val m = mac.split(':', '-').map { it.toInt(16).toByte() }.toByteArray()
    require(m.size == 6) { "Bad MAC" }
    return ByteArray(6) { 0xFF.toByte() } + ByteArray(16 * 6) { m[it % 6] }
}

suspend fun sendWol(mac: String, broadcasts: List<InetAddress>) = withContext(Dispatchers.IO) {
    val p = magicPacket(mac)
    DatagramSocket().use { s ->
        s.broadcast = true
        for (addr in broadcasts) for (port in intArrayOf(9, 7)) s.send(DatagramPacket(p, p.size, addr, port))
    }
}
```

### 4.6 DLNA/UPnP casting (photo, audio, video): Go (conditional)

**How it works.** In DLNA the **phone is the control point** and the **TV fetches the media** from a URL the phone hosts. No raw bytes are pushed over a custom socket. [15][17]

1. **Host:** an embedded HTTP server on the phone exposes user-selected media, e.g. `http://<phone-ip>:<port>/media/<opaque-id>`. Use Photo Picker/SAF URIs as the source. The TV can't fetch `content://` or `file://` URIs. [30]
2. **Discover:** SSDP M-SEARCH (`239.255.255.250:1900`) finds a MediaRenderer advertising **AVTransport** (and ideally **RenderingControl**). [15]
3. **Load:** SOAP `SetAVTransportURI` with the URL and DIDL-Lite metadata. [15]
4. **Play / control:** `Play`, `Pause`, `Stop`, `Seek`. Volume uses RenderingControl, **only if advertised**. [15]
5. The TV issues HTTP `GET` (and possibly `HEAD` and `Range`) requests against the phone server while playing.

**Requirements for the phone server:**
- A correct `Content-Type`.
- **Byte-range support** for seeking and long files.
- Stable reachability while the phone screen is off (§6.3).
- Bounded scope: serve only selected items, use unguessable IDs, and stop when the session ends. [15][17][42]

**[Eng. note]** Samsung renderers can be picky about DIDL-Lite `protocolInfo` and DLNA headers, e.g. `DLNA.ORG_OP=01` to advertise byte-seek, and `transferMode.dlna.org`. Copy proven values from the chosen library and test them.

**Subtitles.** These need extra DIDL-Lite metadata pointing at a subtitle URL. Samsung-specific subtitle metadata is commonly used in the DLNA ecosystem and handling has varied across firmware. UPnPCast v1.3.0 claims Samsung subtitle support. These are library and community claims, so test on hardware. [17][59]

**Codec / container compatibility.**
- No current official DLNA renderer matrix, by model, was found. A TV's internal USB/player codec table **is not** a DLNA guarantee. [1]
- Start lab testing with conservative **test vectors**: JPEG, MP3, H.264/AAC in MP4. Then test HEVC, other containers, subtitles, seeking and long files per exact model and firmware. *These are test vectors, not validated supported formats.* [1][15][17]

**Library:** see §7.2 (jUPnP 3.0.5 vs UPnPCast 1.3.0; avoid Cling).

### 4.7 Screen mirroring: no-go without a receiver path

- **Capture is feasible on Android.** `MediaProjection` with a per-session consent intent creates a `VirtualDisplay` → `Surface` → hardware `MediaCodec` (H.264/VP8). [26][27]
- **Android 14+ constraints:**
  - Each consent result is **single-use**. Reusing it to create another `VirtualDisplay` throws `SecurityException`, so consent is needed for every session.
  - It needs a foreground service of type `mediaProjection` with an ongoing notification and the status-bar indicator. [26][27]
- **Miracast:** Samsung TVs support it natively, but third-party apps **cannot programmatically start** a Miracast session. It is owned by the system and OEM (e.g. Samsung's Smart View on Galaxy phones). [28]
- **The unsolved part is the TV-side receiver and transport.** MediaProjection captures the phone screen, but no compatible Samsung receiver exists for third-party apps. Smart View SDK is app-to-app and its relevant paths are deprecated. DLNA casting is not mirroring, and DIAL launches apps. [4][5][18][26][28]
- **Candidate approaches (spike only; none validated):**

  | Approach | Source | Key risks |
  |---|---|---|
  | Phone-hosted live stream (RTSP/RTMP, or HTTP live TS/HLS) handed to the TV via AVTransport `SetAVTransportURI` | Input B [60] | **Unverified** whether Samsung DMRs accept RTSP or live/unbounded HTTP streams over DLNA. Expect seconds of latency (capture + encode + Wi-Fi + TV buffering), heavy battery and thermal load, and MediaCodec/RTSP maintenance across devices. Unsuitable for gaming or interactive use |
  | WebRTC to a browser-based receiver on the TV, with independent viewer authorization | Beta contracts [40][42][60] | The receiver and network implementation have not landed. Needs a browser the TV trusts, a handshake and named-TV evidence |

- **Decision:** defer from roadmap commitment. Before committing, require a working receiver, a repeatable named-TV test and measured latency/battery figures.
- > **Deepen D1 pointer:** transport select matrix + spike order B(WebRTC)→A(live-URI falsification)→C(DIAL/launch harness) added; MediaProjection single-use + FGS type VERIFIED (high) — see §15.2 [26][63][64].

---

## 5. Network topology and designing for failure

- **Same broadcast domain assumed.** SSDP discovery (multicast) and WOL (broadcast) are limited to the subnet. They fail across VLANs, IoT/guest SSIDs with client isolation, or networks that filter multicast. [6][58]
- **Correction to Input B:** unicast TCP (the WebSocket on 8002, HTTP to 8001) **can** cross subnets *if* the router routes between them and the firewall allows it. What reliably breaks is discovery and wake. Hence: **manual IP entry + saved IP** for control, and documented router settings (directed-broadcast forwarding) for wake. [6][58]
- **Design for failures instead of asserting how often they happen.** No independent 6–12-month postmortem or workload study was found:
  - Reconnect on Wi-Fi or address changes, with backoff.
  - Expire stale discovery entries.
  - Keep manual-IP fallback.
  - Represent **asleep / offline / paired / pairing-required / permission-denied / identity-changed** as distinct states.
  - Require explicit target selection when there are multiple TVs. [6][19][20]
- **[Eng. note] Onboarding diagnostics.** Detect the phone's subnet, flag likely isolation (no SSDP responses, but a manual IP that answers on 8001), and explain the network constraints in plain language. With SmartThings out of the key path, there is no cloud fallback for keys.

---

## 6. Native Kotlin architecture

### 6.1 Provider boundaries

1. **`TvDeviceRepository`:** persisted TV identity (device ID, MAC), last IP, model label, last-known capabilities, pairing state, certificate pin and health.
2. **`SamsungLocalProvider`:** discovery, `/api/v2/` probe, WebSocket lifecycle, token and pin handling, key/text/pointer commands, IME events, reconnect/backoff, observable connection state. **One retained session owner** owns the socket. [33][34]
3. **`WakeProvider`:** builds and sends the WOL magic packet using the stored MAC; setup check for "Power On with Mobile". Kept separate from the socket provider. [12]
4. **`DlnaRendererProvider`:** SSDP discovery, device/service parsing, AVTransport/RenderingControl, DIDL-Lite metadata, playback state, and a **bounded** phone HTTP server for selected media.
5. **`SmartThingsProvider` (optional, later):** OAuth, cloud and capability path, only after current access terms, endpoint quotas and the TV's capability profile are confirmed. **Never in the key-repeat hot path.** [13][14]
6. **`ProjectionSender` (deferred):** only once a compatible receiver and transport are chosen. Keep consent and session lifecycle separate from casting.

**Separate owners for TV control, cast playback and mirroring.** Navigation or receiver changes must not redirect or stop unrelated sessions. [40][41][42]

### 6.2 State and concurrency

- Use Kotlin coroutines for all network I/O. Never block the main thread; avoid legacy `AsyncTask` or blocking-thread patterns.
- Model connection state as a sealed hierarchy (e.g. `Disconnected`, `Discovering`, `Connecting`, `PairingRequired`, `Connected`, `Asleep`, `IdentityChanged`, `Error`) and expose it through `StateFlow` to ViewModels and Compose. The repository/provider boundary keeps protocol failures from leaking into the UI. [6][19][20]
- **Fail closed, in a fixed order:**
  1. Validate the stored TLS identity **before** sending the token.
  2. Cancel stale sessions before they can publish state or persist credentials.
  3. Invalidate session generations on identity or key loss. [34][35]

### 6.3 Lifecycle and background execution

**Conflict resolved: Input B said to always run the remote in a foreground service. That is rejected.**

- **Remote keys:** open the WebSocket **only while the remote UI is active** (a lifecycle-scoped connection). **No always-on foreground service** for ordinary keys. [27]
- **Casting:** while the TV is streaming from the phone's HTTP server, the server must survive screen-off and backgrounding. Use a foreground service **started by the user, for that session**, with the matching FGS type and its type-specific permission. [27]
  - **[Eng. note]** `mediaPlayback` or `connectedDevice` are the likely candidates. Confirm against the current FGS-type definitions and the Play policy declaration.
- **Mirroring:** FGS type `mediaProjection`. [26][27]
- **Don't use `dataSync`** for these sessions. On Android 15+ (apps targeting 35+), `dataSync` FGS is capped at 6 hours per 24 hours, and the semantics don't match. [56]
- jUPnP's Android guidance uses a **bound Android service** to manage the shared UPnP stack's lifecycle; 3.0.5 includes an Android service-startup fix. [19][20][21]

### 6.4 Multicast lock

Acquire a `WifiManager.MulticastLock` around SSDP discovery and release it afterwards. Without it, many devices drop multicast responses and TV discovery becomes unreliable. This needs `CHANGE_WIFI_MULTICAST_STATE`. [21][50]

---

## 7. Android platform, permissions and Play Store reality

### 7.1 SDK policy

- **`targetSdk 36`:** required for new and updated Play phone apps as of 2026-08-31. [22]
- **`minSdk`:** a product/support decision, **not** a Samsung protocol requirement.
  - Input A recommended **26**. AppT ships **29** (with `compileSdk 37`).
  - Choose between 26 and 29 once handset support, dependency constraints and test scope are agreed. jUPnP's Android module supports much older levels, so it doesn't force the choice. [20][21][38]

### 7.2 Candidate stack

| Concern | Choice | Notes |
|---|---|---|
| Language / async | Kotlin, coroutines, `Flow`/`StateFlow`, AndroidX lifecycle/ViewModel | Compose or Views is a product choice |
| Samsung remote transport | **OkHttp WebSocket**, current stable, pinned after dependency/security review | Per-TV pinned TLS (§3.4) |
| JSON | kotlinx.serialization (or equivalent) | **[Eng. note]** Typed command/event models |
| DLNA (primary spike) | **jUPnP 3.0.5 Android module** (2026-08-12; on Maven Central) | Active 2026 releases; Android service docs and permissions. **Audit** CDDL-1.0 obligations, Jetty transport and transitive dependencies, app size and security impact. Input B cited 3.0.4, which 3.0.5 supersedes [19][20][21] |
| DLNA (challenger) | **UPnPCast v1.3.0** (2026-09-02) | Kotlin-first; 92 tests reported, including a fake renderer; claims local-file HTTP streaming, Range, AVTransport/RenderingControl and Samsung subtitle metadata. **But** the release page says Maven Central `1.3.0`, while Sonatype Central still lists only `1.1.2` (2025-06-12). Resolve that, and verify signatures and provenance, before adopting. Its tests don't establish Samsung hardware interoperability [17][23] |
| DLNA (avoid) | **Cling** | README marks it end-of-life; last update 2019-06-20 [24] |
| Phone media server | Ktor (CIO) or a minimal embedded server | **[Eng. note]** Input B also suggested NanoHTTPD. Check its maintenance status before use. Must support `HEAD` and `Range` and bounded serving |
| Secrets | Android Keystore-backed encrypted storage | Tokens and pins per device; exclude from backup; never log [41] |
| Wake | `java.net.DatagramSocket` | §4.5 |
| Mirroring (deferred) | MediaProjection + MediaCodec + chosen transport (RTSP/HLS or WebRTC) | Spike only (§4.7) |

### 7.3 Manifest and runtime permissions (declare only what ships)

| Permission | Needed for | Notes |
|---|---|---|
| `INTERNET` | All LAN TCP/UDP: WebSocket, SOAP, WOL, phone HTTP server | |
| `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | Network change handling; subnet/broadcast calculation | Listed in jUPnP's Android guide [21] |
| `CHANGE_WIFI_MULTICAST_STATE` | `MulticastLock` for SSDP | [21] |
| **Local-network permission** | SSDP/mDNS, `NsdManager`, raw sockets, inbound and outbound LAN TCP/UDP | Opt-in under Android 16 guidance. **Enforced as a runtime permission for apps targeting API 37+ (Android 17).** Build and test a permission gate now; don't assume `INTERNET` will stay sufficient [25] |
| `FOREGROUND_SERVICE` + type-specific permission (e.g. `FOREGROUND_SERVICE_MEDIA_PLAYBACK` / `…_CONNECTED_DEVICE` / `…_MEDIA_PROJECTION` / `…_MICROPHONE`) | User-started cast, mirror or mic sessions only | Declare the matching `foregroundServiceType`. Avoid `dataSync` [27][56] |
| `POST_NOTIFICATIONS` | Notification UX | Doesn't replace an FGS permission [27] |
| `RECORD_AUDIO` | Only if phone-side voice capture ships | Dangerous runtime permission; request it inside the user-started voice flow. It doesn't create a TV voice protocol [3][11][27] |
| *(none)* for media access | Photo Picker / Storage Access Framework | Avoids broad media permissions; selected items are still served over the LAN [30] |

### 7.4 Other Play / platform gotchas

- **No trust-all TLS.** Play flags unsafe `X509TrustManager` implementations (§3.4). [55]
- **[Eng. note] Cleartext HTTP to the TV.** Device info on 8001, UPnP device descriptions and SOAP control URLs are plain HTTP. With `targetSdk ≥ 28`, cleartext is blocked by default in stacks that honour `NetworkSecurityPolicy`, such as OkHttp and `HttpURLConnection`. Network Security Config `domain-config` can't match IP ranges, so this usually needs a deliberate, security-reviewed cleartext exception. Verify how jUPnP's HTTP client behaves. Inbound connections to the phone's own server aren't affected.
- **Doze / background limits:** covered by the lifecycle policy in §6.3.

---

## 8. Ecosystem health and regret risk

| Component | Positive signals | Risks | Mitigation |
|---|---|---|---|
| **Samsung local remote** | Ongoing community maintenance (`samsungtvws` 3.0.6, 2026-09-11; HA integrations) [6][7] | Reverse-engineered; varies by model and firmware; the **largest five-year regret risk** | Adapter boundary, capability reporting, protocol tests, manual IP, a narrow key MVP. Don't tie product behaviour to one community library |
| **jUPnP** | Active 2026 releases; published Android artifact; recent Android lifecycle fix [19][20][21] | Java/Jetty integration; CDDL review | License and dependency audit in Spike C |
| **UPnPCast** | Recent, Kotlin-oriented; a substantial v1.3.0 test addition [17] | Small project; Central publication conflicts with the release notes [23] | Adopt only after provenance is clear and it has been validated on external devices |
| **SmartThings** | Official; possible fallback for state, power or basic capabilities | Rate limits (~12/min/device, single source); paid tiers from Oct 2026 with unconfirmed terms; 24-hour PATs push towards OAuth [13][14][49] | Keep optional; re-verify terms and quotas before any commitment |
| **Mirroring** | Android capture works | No broadly compatible Samsung receiver or transport; Smart View notes don't close the gap; **highest regret risk** [4][5][26][27] | Defer from roadmap commitment |

---

## 9. Comparison matrix

Legend: **✓** available in defined scope · **△** conditional/limited · **✗** not provided · **?** unresolved. This compares feasibility; it is not a Samsung certification matrix.

| Capability | Samsung local WebSocket | SmartThings (public cloud; Home API caveat) | UPnP/DLNA only |
|---|---|---|---|
| **Keys** | **✓/△** Best candidate for common keys; undocumented, depends on TV and firmware [6][7][8] | **△** Only commands that TV's capabilities expose; ~12/min/device is too slow for key repeat; Home API `KeypadInput` is a restricted preview [13][16][29] | **✗** AVTransport is media transport, not key injection [15] |
| **Text** | **△** `SendInputString` only into a focused native Tizen field; not app-drawn search grids [7][8] | **✗/△** No general text-injection contract verified [16][29] | **✗** [15] |
| **Voice** | **?** Conflicting community evidence; no public phone-mic API. Don't promise [9][10][11] | **△** First-party app mic varies by market and device; no third-party voice transport [3][11] | **✗** [15] |
| **Mouse/touchpad** | **?** `ProcessMouseDevice` exists in community code; support by model and app unvalidated [8][9] | **✗** [13][14] | **✗** [15] |
| **Cast audio/video/photo** | **✗** Remote commands aren't media transfer | **△** Capability-dependent media controls don't prove phone-file casting [16][29] | **✓/△** Right path when the TV advertises DMR/AVTransport; codecs, subtitles, URL reachability and model support must be tested [1][15][17] |
| **Screen mirroring** | **✗** | **✗** | **✗** DLNA ≠ mirroring; DIAL = app launch [18][26] |
| └ transport A: phone-hosted live → AVTransport URI | **?/△** Unverified; expect DMR live-stream rejection; falsify cheap [15] | — | **?** Bounded-file-oriented; live handling receiver-dependent |
| └ transport B: WebRTC → Tizen receiver (spike-first) | — (needs TV Web app, 2022+ game mode VERIFIED high) [63] | — | — (Moonlight/WASM precedent, not drop-in) [64] |
| └ transport C: DIAL / Smart View launch | control-plane only; launch harness for B, no media by itself [4][18] | — | — |
| **Power** | **△** Off via key while reachable; **on** needs WOL / "Power On with Mobile" [12] | **△** May switch power if registered and the plan permits; verify [12][13][16] | **✗/△** Not a general power contract [15] |
| **Auth / topology** | Local LAN; pairing prompt + token; self-signed TLS (pin it); subnet/VLAN/multicast issues | Samsung account + OAuth; quotas; paid tiers; Home API in preview | Local SSDP + SOAP/HTTP; phone must host a URL the TV can reach |
| **Verdict** | **Core remote MVP** behind an adapter; no universal compatibility claim | **Optional supplement**, never the keystroke or keyboard path | **Independent casting MVP** for discovered, validated renderers; never a remote or mirroring substitute |

> **Deepen D2 library select (2026-10-16):** DLNA stack = **jUPnP 3.0.5 primary** (sole hard-gate passer); **UPnPCast 1.3.0 conditional** (adopt only if `1.3.x` becomes Central-verifiable or JitPack + commit-hash pin is formally accepted) — frame, scores and counter-argument in §16.3 [17][19][20][23].

---

## 10. Reference implementations reviewed (AppT, Beta)

**Method and evidence bar.** The public `main` snapshots of Anthracite Labs' [AppT](https://github.com/anthracite-labs/AppT) and [Beta](https://github.com/anthracite-labs/Beta) were reviewed on 2026-10-16. Both come from the same organization, so they are **implementation and design references**. They are not independent evidence of Samsung compatibility, failure rates, performance or ecosystem maturity.

A source file proves the code exists. A scripted test proves only the scripted contract. Neither proves that a named TV and firmware works. [31][36][37][39][40][43]

| Area | AppT: code and tests observed | Beta: contracts and code observed | Implication |
|---|---|---|---|
| **Discovery / keys** | Production Kotlin for bounded discovery, session opening, typed commands and the Samsung transport behind a public interface. S04 merge (#105, 2026-09-29) adds durable saved pairing and reconnection [33][34][35] | Detailed Samsung platform contract; architecture says network, discovery and vendor adapters are future work; current modules are presentation surfaces [40][41][44] | **Use AppT** as the implementation pattern for a Samsung-first remote |
| **Pairing / credentials** | Checks the saved TLS identity before attaching a token; encrypted pairing material; invalidates stale sessions; fails closed on identity or key loss; fixture/JVM + Keystore contract tests [34][35] | Specifies device-scoped SPKI trust, first-contact confirmation, atomic credential binding, backup exclusion, fail-closed recovery (contracts only) [41][42] | Adopt per-device trust/token isolation, explicit re-pair and no downgrade (§3.4). Still validate real TV consent and certificate behaviour |
| **Text / voice / mouse / power** | Current route excludes voice, casting, mirroring and mouse; text and wake are later slices [37] | Explicit-send keyboard contract; capability-gated controls; no voice or pointer adapter [41] | Keep text as a separate field test. Neither repo makes voice, mouse or wake available |
| **DLNA casting** | Out of scope [37] | Control-point, playback-owner and bounded HTTP/Range media-source contracts; casting screen is presentation only [40][42][44] | Use Beta as a test and ownership checklist. The library choice stays jUPnP vs UPnPCast |
| **Mirroring** | Out of scope [37] | MediaProjection consent, viewer authorization, receiver handshake, WebRTC; receiver not landed [40][42] | Makes the receiver gap concrete; **does not lift the no-go** |
| **Physical proof** | Testing plan separates fixture tests from a physical-TV matrix; **no passed model/firmware row found** [35][36][37] | Verification contract: unit and scripted tests can't establish physical TV response; **no executed Samsung result found** [43] | Keep the physical matrix as a release gate; label compatibility "not established" |

**Status drift in AppT.**
- The `:samsung` README (2026-09-26) still lists S02 discovery as the only public operation.
- The current source exposes `open`, `forget`, remembered IDs and live sessions.
- The root README (2026-10-01) says the app reaches saved reconnection.

Treat the module README as stale. Treat the root README's "works today" as repository status, not proof of which TV generations are supported. [31][32][33][34][35][36][37]

**Patterns to carry forward:**
- A deep Samsung provider behind an app-facing interface, with one retained session owner. [33][34]
- Fail-closed ordering: identity before token; no "write = success"; cancel stale sessions. [34][35]
- Separate authority owners for control, cast and mirror, with *pending / submitted / unconfirmed / observed* outcomes. [40][41][42]
- An explicit, field-bound keyboard operation. [41]

**Net effect on the decision:** none. The original recommendation stands. Neither repository justifies broader compatibility claims.

---

## 11. Recommended implementation plan and test matrix

### 11.1 Spikes

1. **Spike A: keys and pairing (1–2 iterations).**
   - Physical TVs from **at least three model years**, including one recent Tizen set.
   - Test first-pair approval and denial, token persistence, the WSS pin flow and certificate-change handling, the repeated-prompt case, reconnect, explicit target selection and the common key set.
   - Measure key latency.
   - Track pass/fail by model, firmware and Wi-Fi/Ethernet. **No made-up fleet-wide percentages.**
2. **Spike A2: wake.**
   - Capture the MAC; test WOL with subnet-directed and limited broadcast, with the "Power On with Mobile" setting on and off, over Wi-Fi and Ethernet, after short and long standby.
   - Confirm the socket becomes reachable after wake.
3. **Spike B: text and pointer capability gates.**
   - Check that text appears in a native Settings or browser field, and that `ms.remote.imeStart`/`imeEnd` arrive. Then test app-owned search screens separately.
   - Prototype the pointer only on models where it is confirmed to work.
   - **If the phone sends but the TV or app ignores it, classify it as unsupported, not as a success.**
4. **Spike C: UPnP casting (D2: jUPnP primary).**
   - Start with jUPnP 3.0.5 (Android module), behind an app-owned `RendererGateway` seam (discover/cast/play/pause/seek/volume/subtitle DTOs; DIDL built in the app module) so either library adapts behind it [19][20][21].
   - Test SSDP on real Wi-Fi with the multicast lock, phone HTTP reachability, `GET`/`HEAD`/`Range`, and the JPEG/MP3/H.264-AAC MP4 vectors. Then HEVC, subtitles, seek, volume, long files, and phone screen-off/background behaviour under the chosen FGS type.
   - Complete the CDDL/Jetty audit.
   - Smoke-test UPnPCast 1.3.0 **only after** its provenance trigger fires: `com.yinnho.upnpcast:upnpcast:1.3.x` verifiable on Central, or JitPack + commit-hash pin formally accepted (Central lists only 1.1.2; the v1.3.0 "Maven Central" note is false — JitPack-only, VERIFIED high) [17][23].
5. **Spike L: local-network permission.** Run the whole flow (discovery, socket, WOL, media server) with `targetSdk 37` behaviour enabled. [25]
6. **Spike V (STT voice fallback, buildable now):** AudioRecord → on-device/cloud STT → intent→key/text map into existing WS channel; no new TV protocol; measure latency + accuracy per intent; gate by `RECORD_AUDIO` user-started flow [66].
7. **Mirroring spike order (see §15.2): B→A→C.** B: WebRTC→Tizen receiver first (only low-latency path); A: phone-hosted live-URI→AVTransport falsification in parallel/second (cheap kill-or-revive); C: DIAL/Smart-View launch harness last (scaffolding for B). Gate every transport by observed-TV-effect probes, not socket-write success [26][63][64].
8. **Deferred:**
   - Mirroring and third-party voice wait until a receiver or API and a repeatable device test are identified.
   - SmartThings waits until October 2026 terms, exact quotas, the TV's capability profile and arbitrary key/text exposure are confirmed.

### 11.2 Physical test matrix (template)

| Model / year / region | Firmware | Network (Wi-Fi band / Ethernet / VLAN) | `/api/v2/` OK | Pair prompt | Token persists | Repeated prompt? | Pin stable across reboot | Keys (MVP set) | Text (native field) | Text (streaming app) | Pointer | Wake (WOL) | DMR advertised | JPEG / MP3 / H.264-AAC | HEVC | Subtitles | Seek | Notes |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| *e.g. QN90x / 2024 / ZA* | | | | | | | | | | | | | | | | | | |

Every result must record the model, software version, network context and the **effect actually seen on the TV**. [43]

**D2:** add an explicit **H/J 2014–15 row marked DISPUTED** — openHAB binding reports PIN+encryption, not supported [50]; HA reports an encrypted subset supported/auto-detected [51]; do not average: record the per-model outcome against the binding-scoped reading in §16.1.

---

## 12. Roadmap blockers and open questions

1. **Target SKUs:** which model years, regions and firmware must be supported? What is the declared fallback when local pairing is absent or rejected? **[RETURNED-FOR-USER-DECISION — D2 supplies the matrix (§16.1); user approves the final SKU list.]**
2. **TLS and identity:** does every target TV accept WSS with a stable certificate that can be pinned? Is the key per-device or shared? What recovery is needed after a firmware reset or token invalidation? Community evidence can't answer this across model years. *D2 changes nothing — stands as stated.*
3. **Text scope:** is typing into native fields enough, or is typing into streaming-app grids a must-have? The evidence says these are different input surfaces, and the second isn't exposed by the local channel. [7][8] **[RETURNED-FOR-USER-DECISION — D2 supplies the surface matrix (§16.6); user decides whether native-only suffices or grids are must-have.]**
4. **Voice [answered-in-part by Deepen D1, §15.1]:** is phone-mic-to-TV voice a real launch requirement? If so, identify a public third-party API and test it before roadmap approval. First-party SmartThings mic support doesn't prove API access. [3][9][10][11] *Deepen D1: raw-WS-voice still UNVERIFIED (low); STT→keys/text is the buildable fallback; open: Pastebin-blob retrieval (#177 yWUgrTay/TghJF135/ajjZFvLu/DMgnDCBP) + TV ingestion endpoint/method.* **[RETURNED-FOR-USER-DECISION — D2 proposes a confidence-ranked launch set (§16.7); user approves the set.]**
5. **DLNA coverage:** which TVs expose DMR/AVTransport, and what codec/container/subtitle matrix works **through the renderer** (not USB playback)? [1][15] **[RETURNED-FOR-USER-DECISION — D2 settles V1 vectors + ladder as test targets (§16.8); user approves the scope.]**
6. **Mirroring receiver [answered-in-part by Deepen D1, §15.2]:** what is the intended TV-side receiver and transport? Without one, "mirror to any Samsung TV" can't be built. [26][28] *Deepen D1: B (WebRTC→2022+ game-mode receiver) is the spike-first candidate [63][64]; open: live-DMR falsification result (transport A), LAN STUN/TURN-less handshake proof, pre-2022 browser limits.* **[SETTLED in part by D2 — distribution routes resolved (§16.4); receiver transport still spike-gated B→A→C.]**
7. **SmartThings terms:** what October 2026 commercial terms and quotas are actually in force? Check the rate-limit page against the exact command endpoints. [13][14][49] *D2 changes nothing — re-check terms before any commitment.*
8. **DLNA library:** is the jUPnP CDDL/Jetty stack acceptable, or should a smaller library be chosen once UPnPCast's publication mismatch is resolved? [17][19][20][21][23] **[SETTLED by D2 — jUPnP 3.0.5 primary, UPnPCast 1.3.0 conditional (§16.3).]**
9. **Android 17 local-network permission:** the migration must be tested before rollout. SSDP, local TCP, WOL and the phone media server are all affected. [25] *D2 changes nothing — Spike L stands.*
10. **Cleartext policy [Eng. note]:** which network security config exception, if any, does the app need for HTTP to TV endpoints, and does it pass security review? *D2 changes nothing — stands as stated.*
11. **Wake reliability [Eng. note]:** does WOL work over Wi-Fi standby on target models, or only over Ethernet? **[SETTLED by D2 as verdict table (§16.5) — WoWLAN primary; Wi-Fi-vs-Ethernet ranking stays per-model.]**
12. **Deepen D1 open probes (see §15.3–15.4):** per-surface pointer follow/ignore matrix (browser vs Netflix/Prime/YouTube) + absolute coordinate scaling; text grid-app matrix beyond `SendInputEnd`; wake directed-vs-limited broadcast + ARP-expiry + Wi-Fi-vs-Ethernet ranking per model. **[SETTLED in part by D2 — support/wake/surface matrices supplied (§16.1/16.5/16.6); remaining unknowns need device probes.]**

---

## 13. Reconciliation log

Where Input A (decision register) and Input B (feasibility research) disagreed, and how this document resolves each case:

| # | Topic | Input A | Input B | Resolution |
|---|---|---|---|---|
| 1 | SmartThings role | Optional supplement after terms and quotas are confirmed | "Completely abandon" | **Not in the key path; optional later supplement** for state or power. Both agree it can't carry keys (§2.3) |
| 2 | TLS handling | Per-device pin; never trust-all | No-op TrustManager + always-true hostname verifier, called "industry standard" | **Per-device TOFU pin.** Trust-all is unsafe and Play flags it [55] (§3.4) |
| 3 | Text input | Low–medium; native fields only | "Robustly supported"; Base64 text in `DataOfCmd` | **Limited to native fields.** Wire format corrected: text in `Cmd`, `DataOfCmd: "base64"` (*verified* [8]) |
| 4 | Mouse / pointer | Low; experimental | Summary says "robustly supported"; body says severe app incompatibility | **Experimental, gated by capability** (§4.4) |
| 5 | Voice | Conflicting evidence; don't promise | "Impossible"; NDA-encrypted pipelines | **No-go for roadmap.** Input B's mechanism claim isn't verified; the outcome is the same (§4.3) |
| 6 | Background execution | Lifecycle-scoped remote; FGS only for user-started cast/mirror sessions | Always-on FGS (Data Sync type) for the remote | **Input A.** `dataSync` is also time-capped on Android 15+ [56] (§6.3) |
| 7 | jUPnP version | 3.0.5 (2026-08-12) | 3.0.4 | **3.0.5** |
| 8 | Screen mirroring | No-go without a receiver | Detailed RTSP + AVTransport pipeline presented as feasible | **No-go for commitment**; Input B's pipeline kept as an *unverified* spike candidate next to Beta's WebRTC approach (§4.7) |
| 9 | Subnets / VLANs | Manual IP; discovery may fail | "Complete application failure"; WebSocket can't cross subnets | **Unicast control can cross routed subnets; discovery and WOL can't** (§5) |
| 10 | DLNA renderer presence | Must be discovered per model | "Samsung TVs operate as UPnP Media Renderers" | **Discover per TV**; no universal claim |
| 11 | Key latency | No measurements | "Near-zero latency" | **Unmeasured**; measure in Spike A |
| 12 | Channel naming | `samsung.remote.control` channel | "ms.remote.control channel" | Channel = `samsung.remote.control`; method = `ms.remote.control` |
| 13 | IME events | — | `MS_REMOTE_IME_START_EVENT` / `END_EVENT` | Values on the wire are `ms.remote.imeStart` / `ms.remote.imeEnd` (*verified* [45]) |
| 14 | Wake | Separate feature; validate | Detailed WOL procedure; "rock-solid" | **Kept Input B's procedure** with Input A's caveats and added broadcast notes (§4.5) |
| 15 | Rate-limit figures | Single-source, endpoint-sensitive warning | Firm quota table | **Table kept, with the caveat** (§2.3) |
| 16 | Permissions | Detailed checklist incl. Android 17 local-network permission | Short table (OkHttp, WOL, jUPnP, MediaProjection, FGS) | **Merged** into §7.3; dropped the "Data Sync" FGS type |

**Removed as duplicates or low value:** repeated descriptions of the DLNA flow, pairing and SmartThings pricing; Input B's narrative framing ("irreversibly altered", "mathematically impossible"); and its weakest citations (YouTube, Arduino voice projects, generic WOL explainers), except where they back a specific retained claim.

---

## 14. Sources

All pages accessed **2026-10-16** unless noted.
- "Undated" means no publication date was visible when checked.
- For repositories, the date is the latest visible file/commit date of the reviewed snapshot. It is not a release or hardware-validation date.
- Community implementation claims are not Samsung guarantees.
- Sources **[1]–[44]** keep Input A's numbering. **[45]+** were added during consolidation, either from Input B's bibliography or from spot-checks. Input B's own citations were undated; their dates were **not re-verified** unless marked.

| # | Publisher / title | Date | URL |
|---:|---|---|---|
| 1 | Samsung Developer — General specifications | Undated | [URL](https://developer.samsung.com/smarttv/develop/specifications/general-specifications.html) |
| 2 | Samsung Developer — TV Input Device API | Undated | [URL](https://developer.samsung.com/smarttv/develop/api-references/tizen-web-device-api-references/tvinputdevice-api.html) |
| 3 | Samsung Developer — VoiceInteraction API | Undated | [URL](https://developer.samsung.com/smarttv/develop/api-references/samsung-product-api-references/voiceinteraction-api.html) |
| 4 | Samsung Developer — Smart View SDK Getting Started | Undated | [URL](https://developer.samsung.com/smarttv/develop/extension-libraries/smart-view-sdk/getting-started.html) |
| 5 | Samsung Developer — Smart View SDK Download (Android 2.5.34 listing) | 2024-07-25 | [URL](https://developer.samsung.com/smarttv/develop/extension-libraries/smart-view-sdk/download.html) |
| 6 | `samsungtvws` PyPI — package/release notes, v3.0.6 | 2026-09-11 | [URL](https://pypi.org/project/samsungtvws/) |
| 7 | TheFab21 — `ha-samsungtv-smart` integration | 2026-08-17 (repo update) | [URL](https://github.com/TheFab21/ha-samsungtv-smart) |
| 8 | xchwarze — Samsung TV WebSocket API `remote.py` (message formats *verified 2026-10-16*) | Undated | [URL](https://github.com/xchwarze/samsung-tv-ws-api/blob/master/samsungtvws/remote.py) |
| 9 | xchwarze — voice/mouse reverse-engineering discussion | 2026-01-22 | [URL](https://github.com/xchwarze/samsung-tv-ws-api/discussions/177) |
| 10 | ThaerM — Samsung Tizen provider PR (voice omitted; key/text implementation) | 2026-10 | [URL](https://github.com/ThaerM/Remote-TV-2026-/pull/15) |
| 11 | Samsung Support HK — SmartThings app mobile remote / microphone note | 2026-09-14 | [URL](https://www.samsung.com/hk_en/support/tv-audio-video/controlling-your-samsung-smart-tv-with-the-smartthings-app-mobile-remote/) |
| 12 | Samsung Australia Support — Power On with Mobile / WOL troubleshooting | Undated | [URL](https://www.samsung.com/au/support/tv-audio-video/tv-powers-on-off-automatically/) |
| 13 | SmartThings Developer — API rate limits | Undated | [URL](https://developer.smartthings.com/docs/getting-started/rate-limits) |
| 14 | SmartThings Blog — "A New Enhanced SmartThings API Experience" | 2026-06 | [URL](https://blog.smartthings.com/smartthings-updates/a-new-enhanced-smartthings-api-experience/) |
| 15 | UPnP Forum — AVTransport v2 Service Template | 2006-05-31 | [URL](https://upnp.org/specs/av/UPnP-av-AVTransport-v2-Service-20060531.pdf) |
| 16 | SmartThings Developer — Home API getting started (restricted preview) | Undated | [URL](https://developer.smartthings.com/docs/home-api/get-started-with-home-api) |
| 17 | yinnho — UPnPCast repository and v1.3.0 release | 2026-09-02 | [Repository](https://github.com/yinnho/UPnPCast) · [v1.3.0](https://github.com/yinnho/UPnPCast/releases/tag/v1.3.0) |
| 18 | DIAL — official overview | 2026-08-13 (page date) | [URL](https://www.dial-multiscreen.org/) |
| 19 | jUPnP — release 3.0.5 | 2026-08-12 | [URL](https://github.com/jupnp/jupnp/releases) |
| 20 | Sonatype Central — jUPnP 3.0.5 artifacts | Undated listing | [URL](https://central.sonatype.com/artifact/org.jupnp/org.jupnp.android) |
| 21 | jUPnP — Android integration documentation | Undated | [URL](https://www.jupnp.org/docs/android/) |
| 22 | Android Developers — Google Play target API requirement | Effective 2026-08-31 | [URL](https://developer.android.com/google/play/requirements/target-sdk) |
| 23 | Sonatype Central — UPnPCast version list (still 1.1.2) | 1.1.2 published 2025-06-12 | [URL](https://central.sonatype.com/artifact/com.yinnho.upnpcast/upnpcast/versions) |
| 24 | 4thline — Cling EOL README | Repo last update 2019-06-20 | [URL](https://github.com/4thline/cling) |
| 25 | Android Developers — Local network permission | Current page | [URL](https://developer.android.com/privacy-and-security/local-network-permission) |
| 26 | Android Developers — Media projection | Current page | [URL](https://developer.android.com/media/grow/media-projection) |
| 27 | Android Developers — Foreground-service types; Android 14 type requirements | Current pages | [Service types](https://developer.android.com/develop/background-work/services/fgs/service-types) · [Android 14 changes](https://developer.android.com/about/versions/14/changes/fgs-types-required) |
| 28 | Samsung Support US — Smart View / screen mirroring compatibility | 2026-01-19 | [URL](https://www.samsung.com/us/support/answer/ANS10001896/) |
| 29 | SmartThings Developer — Home API Reference (media-TV capabilities) | Undated | [URL](https://developer.smartthings.com/docs/home-api/home-api-reference) |
| 30 | Android Developers — Minimize permission requests (Photo Picker / SAF) | 2026-08-19 | [URL](https://developer.android.com/privacy-and-security/minimize-permission-requests) |
| 31 | Anthracite Labs — AppT root README | 2026-10-01 | [URL](https://github.com/anthracite-labs/AppT/blob/main/README.md) |
| 32 | Anthracite Labs — AppT `:samsung` README | 2026-09-26 | [URL](https://github.com/anthracite-labs/AppT/blob/main/samsung/README.md) |
| 33 | Anthracite Labs — AppT `SamsungTvsImpl.kt` | 2026-09-30 | [URL](https://github.com/anthracite-labs/AppT/blob/main/samsung/src/main/java/dev/anthracite/appt/samsung/internal/SamsungTvsImpl.kt) |
| 34 | Anthracite Labs — AppT `LiveSession.kt`, `OkHttpSessionTransport.kt` | 2026-10-02 | [LiveSession](https://github.com/anthracite-labs/AppT/blob/main/samsung/src/main/java/dev/anthracite/appt/samsung/internal/LiveSession.kt) · [OkHttp transport](https://github.com/anthracite-labs/AppT/blob/main/samsung/src/main/java/dev/anthracite/appt/samsung/internal/OkHttpSessionTransport.kt) |
| 35 | Anthracite Labs — AppT S04 durable saved-pairing merge (#105) | 2026-09-29 | [Commit](https://github.com/anthracite-labs/AppT/commit/c2594513bae681c96f354645e9df854880e8759c) |
| 36 | Anthracite Labs — AppT testing architecture | 2026-10-02 | [URL](https://github.com/anthracite-labs/AppT/blob/main/docs/architecture/testing.md) |
| 37 | Anthracite Labs — AppT implementation slice map | 2026-10-02 | [URL](https://github.com/anthracite-labs/AppT/blob/main/docs/architecture/slices.md) |
| 38 | Anthracite Labs — AppT Gradle config (`minSdk 29`, `targetSdk 36`, `compileSdk 37`) | 2026-10-02 | [URL](https://github.com/anthracite-labs/AppT/blob/main/app/build.gradle.kts) |
| 39 | Anthracite Labs — Beta documentation index | 2026-10-12 | [URL](https://github.com/anthracite-labs/Beta/blob/main/docs/INDEX.md) |
| 40 | Anthracite Labs — Beta architecture | 2026-10-12 | [URL](https://github.com/anthracite-labs/Beta/blob/main/docs/architecture/ARCHITECTURE.md) |
| 41 | Anthracite Labs — Beta Samsung, Remote and Keyboard contracts | 2026-10-12 | [Samsung](https://github.com/anthracite-labs/Beta/blob/main/docs/platforms/SAMSUNG.md) · [Remote](https://github.com/anthracite-labs/Beta/blob/main/docs/features/REMOTE.md) · [Keyboard](https://github.com/anthracite-labs/Beta/blob/main/docs/features/KEYBOARD.md) |
| 42 | Anthracite Labs — Beta Casting and Mirroring contracts | 2026-10-12 | [Casting](https://github.com/anthracite-labs/Beta/blob/main/docs/features/CASTING.md) · [Mirroring](https://github.com/anthracite-labs/Beta/blob/main/docs/features/MIRRORING.md) |
| 43 | Anthracite Labs — Beta Android platform and verification contracts | 2026-10-12 | [Android platform](https://github.com/anthracite-labs/Beta/blob/main/docs/architecture/ANDROID-PLATFORM.md) · [Verification](https://github.com/anthracite-labs/Beta/blob/main/docs/quality/VERIFICATION.md) |
| 44 | Anthracite Labs — Beta feature-source surfaces | 2026-10-12 | [App](https://github.com/anthracite-labs/Beta/blob/main/app/src/main/kotlin/labs/anthracite/beta/BetaApp.kt) · [Remote](https://github.com/anthracite-labs/Beta/tree/main/feature/remote/src/main/kotlin/labs/anthracite/beta/remote) · [Casting](https://github.com/anthracite-labs/Beta/tree/main/feature/casting/src/main/kotlin/labs/anthracite/beta/casting) · [Mirroring](https://github.com/anthracite-labs/Beta/tree/main/feature/mirroring/src/main/kotlin/labs/anthracite/beta/mirroring) |
| 45 | xchwarze — `samsungtvws` `event.py` (event names *verified 2026-10-16*) | Undated | [URL](https://github.com/xchwarze/samsung-tv-ws-api/blob/master/samsungtvws/event.py) |
| 46 | xchwarze — `COMMANDS.md` (key list) | Undated (via Input B) | [URL](https://github.com/xchwarze/samsung-tv-ws-api/blob/master/COMMANDS.md) |
| 47 | SmartThings Developer — Quotas and Limits | Undated (via Input B) | [URL](https://developer.smartthings.com/docs/enterprise/quotas) |
| 48 | SmartThings Community — "A New Enhanced SmartThings API Experience" thread | 2026-06-23 | [URL](https://community.smartthings.com/t/a-new-enhanced-smartthings-api-experience/309947) |
| 49 | Secondary coverage of SmartThings paid tiers (Android Authority; XDA; Consumer Rights Wiki; daily.dev re: 24-hour PATs) | 2026-06-26 · 2026-09-14 · 2026-07-28 · 2026-09-14 | [Android Authority](https://www.androidauthority.com/smarthings-api-paid-tiers-3681929/) · [XDA](https://www.xda-developers.com/smartthings-fee-check-before-you-buy/) · [Consumer Rights Wiki](https://consumerrights.wiki/w/Samsung_SmartThings_API_monetization) · [daily.dev](https://daily.dev/posts/samsung-s-4-99-smartthings-fee-follows-you-to-home-assistant-fpe1cju5j) |
| 50 | openHAB — Samsung TV binding (legacy/Orsay, UPnP) | Undated (via Input B) | [URL](https://www.openhab.org/addons/bindings/samsungtv/) |
| 51 | Home Assistant — Samsung Smart TV integration | Undated (via Input B) | [URL](https://www.home-assistant.io/integrations/samsungtv/) |
| 52 | roberodin — `ha-samsungtv-custom` `remote_websocket.py` | Undated (via Input B) | [URL](https://github.com/roberodin/ha-samsungtv-custom/blob/master/custom_components/samsungtv_custom/samsungctl_080b/remote_websocket.py) |
| 53 | Stack Overflow — "Token not returning from Samsung smart TV remote control API" | Undated (via Input B) | [URL](https://stackoverflow.com/questions/77258944/token-not-returning-from-samsung-smart-tv-remote-control-api) |
| 54 | Baeldung — Trusting a self-signed certificate in OkHttp (the **rejected** trust-all pattern) | Undated (via Input B) | [URL](https://www.baeldung.com/okhttp-self-signed-cert) |
| 55 | Google Play Help — How to fix apps containing an unsafe implementation of TrustManager (*verified 2026-10-16*) | Undated | [URL](https://support.google.com/faqs/answer/6346016) |
| 56 | Android Developers — Foreground service timeouts (`dataSync` 6h/24h, Android 15+) (*verified 2026-10-16*) | Current page | [URL](https://developer.android.com/develop/background-work/services/fgs/timeout) |
| 57 | Wake-on-LAN magic-packet references (j2i.net; Paul Mutton) | 2024-11-06 · undated (via Input B) | [j2i.net](https://blog.j2i.net/2024/11/06/building-a-wake-on-lan-packet/) · [jibble.org](https://www.jibble.org/wake-on-lan/) |
| 58 | OneUptime — Configure broadcast forwarding for Wake-on-LAN | 2026-03-20 (via Input B) | [URL](https://oneuptime.com/blog/post/2026-03-20-configure-broadcast-forwarding-wake-on-lan/view) |
| 59 | VideoHelp — Universal Media Server version history (Samsung subtitle handling) | Undated (via Input B) | [URL](https://www.videohelp.com/software/Universal-Media-Server/version-history) |
| 60 | Mirroring pipeline examples (Fora Soft: MediaProjection + WebRTC, 2026; AndroidScreenCaster) | 2026 · undated (via Input B) | [Fora Soft](https://www.forasoft.com/blog/article/screen-sharing-android-1644) · [AndroidScreenCaster](https://magicsih.github.io/AndroidScreenCaster/) |
| 61 | Community discussion on Samsung WS voice/pointer/power (r/crestron; Hubitat) | Undated (via Input B) | [Reddit](https://www.reddit.com/r/crestron/comments/vx9ak4/samsung_tv_via_websockets_api/) · [Hubitat](https://community.hubitat.com/t/samsung-tv-integration-question-driver-info-starting-post-127/8341?page=4) |
| 62 | Samsung Developer — FAQ | Undated (via Input B) | [URL](https://developer.samsung.com/faq) |
| 63 | Samsung Developer — Implementing Cloud Game Applications (WebRTC game mode, 2022+; SDP imageattr/FHD cap/track policy) | Undated | [URL](https://developer.samsung.com/smarttv/develop/extension-libraries/cloud-game.html) |
| 64 | Moonlight WebRTC Tizen client thread + Samsung WASM Player / Sockets Extension game-streaming path | Undated | [Reddit](https://www.reddit.com/r/MoonlightStreaming/) · [Samsung WASM Player](https://developer.samsung.com/smarttv/develop/extension-libraries/wasm-player.html) |
| 65 | Samsung Developer / Tizen — VoiceControl API (TV-app-side predefined-command recognition; `getVoiceControlClient`, recorder privilege) | Undated | [URL](https://developer.samsung.com/smarttv/develop/api-references/samsung-product-api-references/voicecontrol-api.html) |
| 66 | Android Developers — SpeechRecognizer / speech-recognition APIs + AudioRecord capture (phone-side STT pattern) | Current pages | [SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer) · [AudioRecord](https://developer.android.com/reference/android/media/AudioRecord) |
| 67 | Samsung Developer — Smart View SDK Enhanced Features (WoWLAN primary; prior-connection/MAC preconditions; SDK 2.4.0+ auto-wake timings) | Undated (via D2A digest, accessed 2026-10-16) | [D2A digest](digests/deepen-d2A-r1.md) |
| 68 | Samsung Developer — Distributing Applications (Seller Office release flow; model-group scoping; closed beta; partner-only alpha; phased rollout; US-default distribution) | Undated (via D2A digest, accessed 2026-10-16) | [D2A digest](digests/deepen-d2A-r1.md) |
| 69 | SmookeyDev — MoonlightTizenInstaller README (Developer Mode + SDB sideload; Tizen 7+ Samsung-account cert signing) | Undated (via D2A digest, accessed 2026-10-16) | [Repo](https://github.com/SmookeyDev/MoonlightTizenInstaller) |
| 70 | OneLiberty — moonlight-chrome-tizen Discussion #33 (USB widget fallback; persistence caveat) | 2024-11-17 (via D2A digest, accessed 2026-10-16) | [Repo](https://github.com/OneLiberty/moonlight-chrome-tizen) |
| 71 | Samsung Developer — Game Streaming on Tizen TV with Wasm (WASM player + sockets extension; sideload-pattern acknowledgement) | Undated (via D2A digest, accessed 2026-10-16) | [D2A digest](digests/deepen-d2A-r1.md) |
| 72 | Samsung Developer — Keyboard / IME guide (native input/textarea surfaces; IME show triggers) | Undated (via D2C digest, accessed 2026-10-16) | [D2C digest](digests/deepen-d2C-r1.md) |
| 73 | Samsung Developer — Media Specifications hub + 2021/2022 per-year TV video tables (USB/player scope only — NOT DLNA) | Undated (via D2C digest, accessed 2026-10-16) | [D2C digest](digests/deepen-d2C-r1.md) |
| 74 | jellyfin-tizen issue #90 (DTS disable/transcode; HD-audio hangs) + JellyWatch codec-ladder guide (H.264/HEVC/VP9/AV1 anchors; no Dolby Vision) | Undated / 2026 (via D2C digest, accessed 2026-10-16) | [jellyfin-tizen #90](https://github.com/jellyfin/jellyfin-tizen/issues/90) |
| 75 | Universal Media Server — device-support workflow (per-model renderer .conf pinning) + community DLNA subtitle threads (external-SRT missing; PGS burn-in) | Undated (via D2C digest, accessed 2026-10-16) | [Repo](https://github.com/UniversalMediaServer/UniversalMediaServer) |
| 76 | Android API-level distribution aggregator, Jun-2026 figures (API 26 ≈ 96.1% vs API 29 ≈ 91.1%) | 2026-06 (via D2B digest, accessed 2026-10-16) | [D2B digest](digests/deepen-d2B-select-r1.md) |
| 77 | Mend / FOSSA CDDL-1.0 analyses (file-level copyleft; unmodified-binary attribution load) | Undated (via D2B digest, accessed 2026-10-16) | [D2B digest](digests/deepen-d2B-select-r1.md) |

---

## 15. Deepen D1 — full-vision technical implementation (legalities parked, 2026-10-16)

> Technical implementation only. Legal/policy/money gates are named in §15.5, not applied. Verifier flags: (a) Tizen 2022+ game-mode WebRTC + Moonlight-Tizen precedent VERIFIED high; (b) VoiceInteraction/VoiceControl TV-app-local, no phone transport VERIFIED high; (c) MediaProjection single-use consent (Android 14+) + FGS type VERIFIED high; (d) #177 raw-voice-over-WS claim UNVERIFIED low.

### 15.1 Voice

- **Raw-channel hypothesis: UNVERIFIED (low).** #177 describes Voice Control as raw-audio streaming + state tracking over `samsung.remote.control`, but shows no frames, method, codec, sample-rate or chunking — only 4 unretrieved Pastebin blobs [9]. **What would prove it:** (1) retrieve blobs (yWUgrTay, TghJF135, ajjZFvLu, DMgnDCBP) via authenticated fetch / issue #163; (2) identify the TV-side ingestion method/endpoint accepting PCM/Opus chunks. Until both land, do not design to it.
- **TV APIs are app-local: VERIFIED (high).** `VoiceInteraction` (webapis.voiceinteraction, privilege `voicecontrol`) and `VoiceControl` (`tizen.voicecontrol`, privilege `recorder`) expose callbacks/command-lists to code **running on the TV**; neither documents phone→TV audio transport [3][65].
- **Buildable now: phone STT→keys/text.** AudioRecord PCM16 capture → Android SpeechRecognizer (or on-device/ML-Kit path) → intent→key/text map into the existing WS key/text channel [66]. No new TV protocol; needs `RECORD_AUDIO` in a user-started flow. Latency and per-intent accuracy unmeasured — measure in Spike V (§11).

### 15.2 Mirroring transports + select matrix

| Dimension | A: phone-hosted live → AVTransport URI | B: WebRTC → Tizen receiver (spike-first) | C: DIAL / Smart View (control-plane only) |
|---|---|---|---|
| Latency (unverified est.) | High, multi-second (capture+encode+HTTP+TV buffer); TV expects bounded files | Low, sub-second capable; phone encode + LAN RTT dominate [63] | N/A — media latency = paired transport |
| Interactivity | Video/slides maybe; gaming no | Only path where gaming/interactive possible (2022+ tuning) [63][64] | None by itself |
| Phone cost | High sustained encode+serve (magnitude unmeasured) | High sustained encode+WebRTC (magnitude unmeasured) | Low (discovery/launch/messages) [4][18] |
| Receiver | None *if* DMR accepted live URIs — unverified; live-feed failures reported | Custom Tizen Web receiver required (game-mode `use.game.mode`; SDP `a=imageattr`, FHD cap, 1 audio+1 video track, FIR/RTP handling) [63] | Installed Tizen receiver required; Cloud/DMP paths removed post-'19/'21 [4] |
| Verdict | Falsify cheap, don't build on | Spike-first: documented live primitive + shipped Moonlight/WASM precedent (not drop-in) [64] | Launch harness for B, not a transport |

- **Capture base: VERIFIED (high).** `createScreenCaptureIntent→getMediaProjection→createVirtualDisplay→Surface→MediaCodec`; Android 14+ single-use consent per session + `mediaProjection` FGS type/permission; Android 15 QPR1 chip/auto-stop + `onStop` release [26].
- **Spike order B→A→C:** B proves the interactivity ceiling; A is one cheap test (serve HLS/live-TS from phone, `SetAVTransportURI`, expect rejection — one result kills or revives it); C scaffolds B's launch/signalling. Gate all by observed-TV-effect, named-TV rows, measured latency/battery.

### 15.3 Mouse / keyboard mechanics + probes

- **Mouse:** `ProcessMouseDevice/Move` with `{x, y, Time}`; clicks are separate `SendRemoteKey` Click/Press/Release — not in the mouse frame; moves are fire-and-forget (`key_press_delay=0`) vs keys' default delay [8]. Samsung UX treats hover as first-class cursor state. **Unverified:** absolute coordinate space/scaling; per-surface follow/ignore (browser vs Netflix/Prime/YouTube grids) — build the per-surface pointer matrix in Spike B.
- **Keyboard: no change — wire format confirmed.** `Cmd=base64(utf-8)`, `DataOfCmd="base64"`, close with `SendInputEnd`; `custom.remote.textReceived` broadcast once per IME session; `ms.remote.imeStart/imeEnd` bound the session; hold-to-repeat = Press…sleep…Release [8][45]. Grid-app text stays per-app unverified.
- **Observed-effect probes:** mouse — with browser open, send Move sweep; PROVEN only if on-screen cursor visibly relocates; cross-check d-pad/Enter-vs-click on same target. Text — focus native field, await `imeStart`, send textReceived→string→End; PROVEN only if glyphs appear; `imeEnd` bounds session.

### 15.4 Wake + casting detail + probes

- **Wake:** burst WOL magic packets (FF×6 + MAC×16) via UDP 9 (7 alt) to captured MAC; directed-broadcast + `255.255.255.255`; dominant failure = deep-sleep NIC power-down [12]. Smart View SDK documents a first-party WoWLAN sender path [4]. **Probes:** standby TV + burst to stored MAC; PROVEN only if NIC wakes and 8001/8002 answers. Still open: "Power On with Mobile"/Network-Standby path, directed-vs-limited requirement, ARP-expiry timing, Wi-Fi-vs-Ethernet ranking per model.
- **Casting: no change to core flow — confirmation.** SSDP M-SEARCH (`ST: MediaRenderer:1`) → device-description `Location:` → AVTransport controlURL → `SetAVTransportURI(CurrentURI + CurrentURIMetaData)` → `Play(Speed 1)`; minimal push can work with empty DIDL-Lite, richer `protocolInfo`/seek metadata renderer-dependent [15]. Phone HEAD+Range server, bounded IDs, codec ladder unchanged (§4.6). **Probe:** M-SEARCH→URI→Play; PROVEN only if TV leaves menu, renders media, Pause/Seek respond.

### 15.5 Parked legalities (named, not applied — re-add at spec stage)

- SmartThings terms/quotas/commercial tiers and per-endpoint rate-limit scoping [13][14].
- Play policy: TrustManager/safe-TLS declarations, FGS-type + type-specific permission declarations, cleartext/ JewIP exception review [55][27].
- NDA/ToS judgements on decompiling first-party APKs, scraping Pastebins, private-API reuse, Seller Office distribution for a custom TV receiver.

---

## 16. Deepen D2 — spec open-question resolution (2026-10-16)

> Decision-first answers to the §12 blockers, from digests D2A (support/wake/receiver), D2B (library/minSdk) and D2C (surfaces/voice/vectors), all accessed 2026-10-16. Verifier flags applied inline. Per-model hardware validation still required everywhere — D2 settles scopes and defaults, not device behaviour.

### 16.1 Support matrix

| Generation | Remote path | Standing |
|---|---|---|
| Tizen 2016+ (K-series onward) | WS 8001 / WSS 8002; **wss-preferred** — if the TV supports 8002 it MUST be used or keyCode fails | Consensus [8][50][51] |
| Legacy C (2010) / D (2011) / E (2012) / F (2013) | TCP 55000 (Orsay) | Out of scope [50] |
| H (2014) / J (2015) | Encrypted-legacy vs WS-subset | **DISPUTED (high)** — see below |

**H/J dispute — both sides cited, never averaged.** openHAB binding: H/J use PIN+encryption and are NOT supported [50]. HA integration: an H/J encrypted subset IS supported and auto-detected on WS connect [51]; the sermayoral encrypted component is a third witness for support. **Binding-scoped reading:** the native-Kotlin WS target treats H/J as unsupported unless the encrypted-legacy path is explicitly built and device-tested; record per-model outcomes, do not generalize.

**Subnet/pairing rules.** Same subnet/VLAN required for SSDP discovery, WS pairing and WOL; cross-subnet fails (masquerade/proxy workaround only) [50][51][8]. Pairing overlay Allow/Deny ≤30s, token cached [51][8]. Newer sets default Access Notification to ask-every-time → set Device Connection Manager → Access Notification → **First Time Only** + purge the Device List to stop re-prompts [51][8]. **Denied-pairing fallback:** first-time-only + purge device list + re-prompt; no silent retry. Regional/firmware variance stands (app-list API removed ~2019/2020; Frame art API removed 2022, restored fw 1622+) [50].

### 16.2 minSdk 26 — SETTLED (medium)

No API-29-only floor found in either candidate path: UPnPCast floor is API 24 [17]; jUPnP's Android module carries no 29 floor [19][20][21]. FGS *types* are a targetSdk-34+ declaration matter (Play requires target 36 since 2026-08-31 — orthogonal to minSdk) [22][27]; Photo Picker and MediaProjection-29 deltas are runtime/targetSdk concerns [26][30]. Distribution ≈ 96.1% (API 26) vs ≈ 91.1% (API 29): minSdk 29 sheds ~5pp reach for zero DLNA benefit [76]. **Binding reason:** no API-29-only dependency exists in either candidate path. AppT-29 precedent carries no weight (no evidence available). If local-file serving must survive screen-off, add FGS `mediaPlayback` + Play declaration at target 36 — still not a minSdk raiser. (Verifier: minSdk-26 verdict VERIFIED medium — no API-29-only floor found.)

### 16.3 DLNA library select — SETTLED

**Approved frame:** four hard gates — maintained recency, Maven Central provenance, shippable license, Android-fit transports — then weighted prefs (Kotlin > Samsung quirks > footprint > screen-off, /5).

| Candidate | Kotlin | Samsung | Footprint | Screen-off | Gates |
|---|---|---|---|---|---|
| UPnPCast 1.3.0 | 5 (coroutine-first, minSdk 24) | 4* (explicit Samsung subtitle handling; *vendor-claimed) | 5 (NanoHTTPD only, no OkHttp/Gson) | 1 (unverified) | 3/4 — **Central FAIL at 1.3.0** |
| jUPnP 3.0.5 | 2 (Java API) | 2 (generic, no Samsung handling) | 2 (Jetty weight) | 2 (service infra, still unproven) | **4/4 PASS** (CDDL with obligations) |

**PICK: jUPnP 3.0.5** — the only gate-passer (Central-verified [19][20]; CDDL-1.0 shippable with obligations [19][77]). **RUNNER-UP: UPnPCast 1.3.0** — dominates every weighted pref [17] but fails the provenance gate: Central lists **only 1.1.2** (2025-06-12) [23]; the v1.3.0 "(Maven Central)" note is false, releases are JitPack-distributed (VERIFIED high). **Win conditions:** `com.yinnho.upnpcast:upnpcast:1.3.x` becomes Central-verifiable, or the team formally accepts JitPack + commit-hash pinning + vendored artifact. **Strongest counter-argument:** Java API + Jetty weight + CDDL tracking vs a Kotlin-native, NanoHTTPD-light library with explicit Samsung subtitle handling — expect more glue code and APK bloat with jUPnP. **Hedge:** app-owned `RendererGateway` seam (discover/cast/play/pause/seek/volume/subtitle DTOs; DIDL built in the app module) — both libraries adapt behind it. **Staleness:** a selection older than two quarters must refresh before action. Cling stays cut (EOL 2019 [24]).

### 16.4 Receiver distribution — routes SETTLED

**Store scale path:** Seller Office → Request New Release → Samsung review/verification → Launched per model-group + service country; defects via Defect Resolve; upgrades resubmitted [68]. Scoping is by model group; Tizen backward-compat defaults to a single version; phased rollout 3%→10%→100% applies to updates only [68]. Public sellers default to US distribution; wider needs partnership [68]. Closed beta (2021+ groups, ≤90d extendable to 180d, ≤1000 one-time codes) and partner-only alpha (1 test, ≤50 DUID, ≤30d, 2020+ non-China) exist for staged validation [68].

**Dev-mode sideload pilot:** enable Developer Mode (Apps panel → 12345 → ON + host IP → power-cycle), same-LAN SDB connect to TV_IP:26101, install `.wgt`; Tizen 7+ needs Samsung-account cert signing [69]. Moonlight is the precedent — and it is sideload, not store [69]; USB fallback (Tizen 5.5+, root/userwidget + license) exists but persistence is unreliable [70]. Samsung's own WASM game-streaming doc acknowledges the sideload-receiver pattern [71].

**Verdict:** pilot via sideload, scale via store. (Receiver *transport* choice stays spike-gated B→A→C per §15.2.)

### 16.5 Wake — procedure SETTLED (per-model validation still required)

Samsung's developer model is **WoWLAN primary**; wired WoL noted as also-supported on '16+ TVs with LAN cable [67]. "Power On with Mobile" exists (VERIFIED high); year-split setting paths (medium): 2022–2026 `Settings → All Settings → Connections → Network → Expert Settings`; 2017–2021 `Settings → General → Network → Expert Settings`; OFF blocks mobile/SmartThings power-on [12].

| Case | Verdict |
|---|---|
| Same-LAN Wi-Fi, setting ON, MAC stored | **Primary path** — a prior successful connection registers the mobile MAC; sender issues WakeOnWirelessLan [67]; HA precedent sends the magic packet when the MAC is known [51] |
| Ethernet | Test per model — openHAB quirks: fails on wired Ethernet with soundbar on ARC/eARC; works on Wi-Fi ± soundbar [50] |
| Setting OFF | Blocked — setup checklist must gate it |
| Cross-subnet | Unsupported without directed-broadcast forwarding; `255.255.255.255` will not cross routers [50][51][8] |

SDK 2.4.0+ automates WOW (standby TVs surface ~7s post-discovery; connect() wakes then waits ~1min) [67]. Post-2016 only, never pre-2016; wait up to ~1min after power-off before WOL (TV not yet in standby) [50]. Deep-sleep NIC-down reports explain per-model flakiness (low) — keep Wi-Fi-vs-Ethernet as a matrix column.

### 16.6 Text-surface matrix — SETTLED as test matrix

| Surface | Standing |
|---|---|
| Native Tizen `input`/`textarea` (focused, IME-bound) | **ACCEPTS (high)** — focused elements summon the system IME [72]; base64 wire format confirmed [8] |
| TV browser URL entry | Recipe exists (launch browser + keyCode text) — medium [50] |
| Netflix / YouTube / Prime search grids | **UNKNOWN, likely reject** — all recipes found use D-pad keys + app IDs; no positive SendInputString report in budget (low) [50][51]; Samsung IME docs cover native controls only [72] |
| System PIN / password dialogs | Unknown — separate `send_password` path exists, acceptance unverified (low) [8] |

**Rule: socket-write ≠ acceptance.** PROVEN only on observed effect (glyphs on screen), per probes in §15.3.

### 16.7 Voice launch-set proposal — RETURNED FOR USER DECISION

Ranked by mapping confidence (key names per [46] unless noted):

1. **1:1 high — ship first:** power/nav/system (`POWER/HOME/MENU/SOURCE/RETURN` + `UP/DOWN/LEFT/RIGHT/ENTER`); volume/channel/digits (`VOLUP/VOLDOWN/MUTE`, `CHUP/CHDOWN`, `0–9`); transport (`PLAY/PAUSE/STOP/REWIND/FF` + colour keys, `GUIDE/INFO/TOOLS`).
2. **Medium — conditional:** app names only via out-of-channel deep-link IDs (region-dependent, post-2019 discovery limits) [50][51]; short dictated phrases → SendInputString only when a native IME control is focused [72][8]; long-press (e.g. hold RETURN to exit Netflix) via Press→sleep→Release — voice set must distinguish tap vs hold [46][50].
3. **Excluded:** pointer paths ("swipe to X", "third tile", drag/scroll) — no single-key equivalent; `ProcessMouseDevice` exposes only move + click, so paths must be scripted and are fragile [46][50].

User picks the launch set.

### 16.8 V1 cast vectors + 5-rung ladder — vectors SETTLED as test targets; scope RETURNED

**USB tables ≠ DLNA — stated on every Samsung-spec claim below.** The general-spec hub and per-year video tables are authoritative for USB/player only, NOT DLNA; DLNA negotiation must be validated against the renderer [73][1].

**V1 vectors:** JPEG, MP3, H.264/AAC in MP4 — test vectors, not guarantees [1][15].

**5-rung ladder (renderer-tested anchors, low–medium):** H.264 all models → HEVC 2016+ → VP9 2019+ → AV1 2022+ → no Dolby Vision (HDR10/HDR10+ only); TrueHD decoded to PCM at best [74]. Codec drop-offs (USB/player scope): DTS absent on 2021+; WMA dropped 2025+; RA/RMVB dropped 2026+ [73]. jellyfin-tizen explicitly disables DTS (transcode) on 2018+; HD-audio files hang unless a compatible track is default/first [74].

**Subtitles/metadata:** USB guidance is same-basename SRT + UTF-8; over DLNA external-SRT delivery is repeatedly reported missing; PGS image subs require transcode/burn-in [75]. The UMS renderer-profile workflow (per-device `.conf`) is the established method for pinning DLNA vectors per Samsung model; no Samsung DLNA matrix was found [75].

User approves the V1 scope; the lab validates per renderer.
