# Deepen: pointer, text, wake, casting-detail (native-Kotlin → Samsung Tizen) — r1
Accessed: 2026-10-16. Evidence retrieved this run only; training data used for queries only.

## Claims

### 1. MOUSE / pointer
- Pointer frame is `{"Cmd":"Move","Position":{"x":int,"y":int,"Time":str(duration)},"TypeOfRemote":"ProcessMouseDevice"}` on `ms.remote.control`; click semantics are separate `SendRemoteKey` Click/Press/Release frames, not part of mouse frame | source=samsungtvws/remote.py publisher=xchwarze/samsung-tv-ws-api (GitHub) pub=undated (master, ~2019-2024 lineage) accessed=2026-10-16 | confidence=high | class=pattern
- `move_cursor(x, y, duration=0)` sends with `key_press_delay=0` (no inter-key sleep), implying pointer moves are fire-and-forget stream, unlike key clicks which default to ~1s delay | source=samsungtvws/remote.py publisher=xchwarze/samsung-tv-ws-api pub=undated accessed=2026-10-16 | confidence=high | class=pattern
- HA fork confirms same WS remote-control channel (`/api/v2/channels/samsung.remote.control`), token-via-`ms.channel.connect`, and app-list/running-app plumbing around the pointer channel | source=samsungws.py publisher=ollo69/ha-samsungtv-smart (GitHub) pub=undated accessed=2026-10-16 | confidence=high | class=pattern
- Samsung's own UX model defines pointer interactions as Hover (position without click) + Move (change cursor location), i.e. TV-side cursor exists and hover is a first-class state distinct from focus/d-pad | source=Input Methods - Smart TV publisher=Samsung Developer pub=undated accessed=2026-10-16 | confidence=high | class=pattern
- Pointer is a Smart-TV motion/pointer feature with per-TV pointer speed/size settings; physical-mouse and Smart-remote pointer paths exist in parallel to the WS `ProcessMouseDevice` path | source=How to connect a Keyboard or Mouse in Samsung Smart TV publisher=Samsung Support pub=undated accessed=2026-10-16 | confidence=medium | class=ecosystem
- No retrieved source this run documented absolute coordinate scaling (e.g. 1920x1080 vs normalized 0-1) or which surfaces (browser vs Netflix/Prime/YouTube grids) follow vs ignore the WS pointer; coordinate-range and per-surface behavior remain unverified | source=(no source retrieved) publisher=(none) pub=undated accessed=2026-10-16 | confidence=low | class=other

### 2. KEYBOARD / text entry
- Text wire format is `{"Cmd": base64(utf-8 text), "DataOfCmd":"base64", "TypeOfRemote":"SendInputString"}`; session close is `{"TypeOfRemote":"SendInputEnd"}` with no payload | source=samsungtvws/remote.py publisher=xchwarze/samsung-tv-ws-api pub=undated accessed=2026-10-16 | confidence=high | class=pattern
- Some TVs require a `{"method":"ms.channel.emit","params":{"event":"custom.remote.textReceived","to":"broadcast"}}` broadcast before first text; implementation sends it once per IME session (`_first_text_sent` flag), reset on IME start/end | source=samsungtvws/remote.py publisher=xchwarze/samsung-tv-ws-api pub=undated accessed=2026-10-16 | confidence=high | class=pattern
- TV signals IME session bounds as `ms.remote.imeStart` / `ms.remote.imeEnd` events; library resets text-session state on both | source=samsungtvws/event.py publisher=xchwarze/samsung-tv-ws-api pub=undated accessed=2026-10-16 | confidence=high | class=pattern
- Hold-to-repeat for keys is Press … sleep(seconds) … Release (`SendRemoteKey.hold`), i.e. explicit press/release pair, not an auto-repeat flag; same mechanism is the only shipped repeat primitive | source=samsungtvws/remote.py publisher=xchwarze/samsung-tv-ws-api pub=undated accessed=2026-10-16 | confidence=high | class=pattern
- Native-IME-vs-app-drawn-grid distinction and any field-bound send/cancel contract beyond SendInputEnd were not documented in retrieved sources; treat grid-app text as unverified per-app behavior | source=(no source retrieved) publisher=(none) pub=undated accessed=2026-10-16 | confidence=low | class=other

### 3. WAKE
- Only reliable wake path reported across threads is a WOL magic packet (FF x6 + MAC x16) to the TV's MAC, UDP port 9 (port 7 also standard); one SmartThings thread specifies sending multiple/burst packets | source=Samsung TV - Can't Turn On (SmartThings Community) publisher=SmartThings Community pub=~2019-2024 thread accessed=2026-10-16 | confidence=medium | class=pattern
- WOL is UDP port 9 by default (port 7 alternate encapsulation) | source=Wake-on-LAN publisher=Wikipedia pub=undated (stable article) accessed=2026-10-16 | confidence=high | class=pattern
- Failure mode is consistent: TV enters deep sleep powering down Ethernet/Wi-Fi adapters, so WOL stops working; HA users report this as the dominant WOL failure | source=WOL Samsung TV - Configuration publisher=Home Assistant Community pub=undated accessed=2026-10-16 | confidence=medium | class=ecosystem
- Samsung's first-party sender-side WoWLAN path is documented inside Smart View SDK enhanced features (Tizen TV can use Wake on Wireless LAN to power on a previously discovered TV) | source=Enhanced Features publisher=Samsung Developer (Smart View SDK) pub=undated accessed=2026-10-16 | confidence=medium | class=pattern
- "Power On with Mobile" / Network Standby setting names, subnet-directed vs limited-broadcast behavior, ARP-expiry timing, Wi-Fi-vs-Ethernet standby reliability ranking, and `/api/v2/ wifiMac` capture were not verified in retrieved sources this run | source=(no source retrieved) publisher=(none) pub=undated accessed=2026-10-16 | confidence=low | class=other

### 4. CASTING DETAIL (DLNA control point)
- Discovery is SSDP M-SEARCH over UDP to 239.255.255.250:1900 with `ST: urn:schemas-upnp-org:device:MediaRenderer:1`, response gives `Location:` device-description URL | source=Sending a video content to a DLNA/UPnP software/device using curl publisher=Accella (2013 guide, protocol-stable) pub=2013-06-21 accessed=2026-10-16 | confidence=high | class=pattern
- Device description exposes AVTransport service + controlURL; playback is two SOAP calls: `SetAVTransportURI` (InstanceID 0, CurrentURI, CurrentURIMetaData) then `Play` (InstanceID 0, Speed 1) | source=Sending a video content to a DLNA/UPnP software/device using curl publisher=Accella pub=2013-06-21 accessed=2026-10-16 | confidence=high | class=pattern
- DIDL-Lite CurrentURIMetaData may be empty for basic push (worked against XBMC renderer in cited guide); richer metadata (protocolInfo incl. DLNA.ORG_OP, duration, byte-range) is renderer-dependent, not mandatory for minimal play | source=Sending a video content to a DLNA/UPnP software/device using curl publisher=Accella pub=2013-06-21 accessed=2026-10-16 | confidence=medium | class=pattern
- Phone-HTTP-server HEAD+Range support, bounded unguessable IDs, Photo Picker/SAF sourcing, Samsung subtitle metadata handling, and conservative-codec ladder (JPEG/MP3/H.264-AAC then HEVC/seek/long-file) were not verified in retrieved sources this run | source=(no source retrieved) publisher=(none) pub=undated accessed=2026-10-16 | confidence=low | class=other

### 5. Minimal capability probes (observed TV effect, not socket-write success)
- Mouse: with browser open (`org.tizen.browser` NATIVE_LAUNCH), send Move sweep; PROVEN only if on-screen cursor visibly relocates; then d-pad/Enter-vs-click cross-check on same target | source=synthesis of remote.py + Input Methods publisher=(derived) pub=undated accessed=2026-10-16 | confidence=medium | class=other
- Text: focus a native text field, await `ms.remote.imeStart`, send textReceived→SendInputString→SendInputEnd; PROVEN only if glyphs appear in field; `ms.remote.imeEnd` bounds the session | source=synthesis of remote.py + event.py publisher=(derived) pub=undated accessed=2026-10-16 | confidence=medium | class=other
- Wake: TV in standby, send burst of magic packets to captured MAC via directed broadcast; PROVEN only if NIC wakes and port 8001/8002 answers / TV powers on | source=synthesis of SmartThings + HA threads publisher=(derived) pub=undated accessed=2026-10-16 | confidence=medium | class=other
- Cast: M-SEARCH → controlURL → SetAVTransportURI(phone HTTP URL) → Play; PROVEN only if TV leaves menu and renders the media with transport (Pause/Seek) responsive | source=synthesis of Accella flow publisher=(derived) pub=undated accessed=2026-10-16 | confidence=medium | class=other

## LEADS (unfollowed, budget-spent)
- `samsungtvws/COMMANDS.md` + `shortcuts.py` (key list, known-good click set for fallback when pointer ignored).
- openHAB Samsung TV binding docs (websocket vs UPnP channel split; useful second source for per-model gating).
- UPnP AVTransport spec PDF (Seek/GetPositionInfo/GetTransportInfo for trick-play probe design).
- Android NSD + MulticastLock docs, jUPnP/UPnPCast repos (phone-side SSDP + HTTP Range server patterns).
- Samsung Smart View SDK Enhanced Features full page (first-party WoWLAN preconditions).
- Samsung Developer TV Model Groups + Media/Video Specifications (codec ladder per year/model).

## NOT FOUND (asked, unverified this run)
- Pointer absolute coordinate space and scaling; per-surface follow/ignore matrix (browser vs Netflix/Prime/YouTube); per-model pointer gating.
- Native-Tizen-IME vs app-drawn-grid text matrix; field-bound send/cancel contract beyond SendInputEnd; key-repeat coalescing policy.
- "Power On with Mobile"/Network Standby exact setting path; directed-vs-limited broadcast requirement; ARP-expiry behavior; Wi-Fi-vs-Ethernet standby ranking; wifiMac JSON field path.
- Samsung DLNA subtitle metadata handling; conservative start vectors and per-model HEVC/seek/long-file limits.
