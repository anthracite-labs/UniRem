# Deepen D2C-R1 — Samsung native-Kotlin remote: text surfaces (Q3), voice-set mapping (Q4), codec vectors (Q7)

Scope: local WS channel (SendInputString text, ProcessMouseDevice pointer) + DLNA casting. Informs decisions; user decides.
Freshness classes: version|compat|pattern|ecosystem|other. Accessed 2026-10-16 throughout.

## CLAIMS

### Q3 — text surfaces (SendInputString acceptance)
- Native Tizen `input`/`textarea` focused elements summon the system IME and receive text input events; IME show triggers include Remote Enter, mouse click, or JS focus | source=Keyboard/IME publisher=Samsung Developer pub=undated accessed=2026-10-16 | confidence=high | class=pattern
- Samsung IME docs cover only first-party/native controls (`input` types text/search/password/tel/url, `textarea`); they say nothing about Netflix/YouTube/Prime in-app search grids | source=Keyboard/IME publisher=Samsung Developer pub=undated accessed=2026-10-16 | confidence=high | class=other
- WS lib encodes SendInputString as base64 (`Cmd`+`DataOfCmd=base64`, `TypeOfRemote=SendInputString`) with SendInputEnd terminator; a socket write succeeding does NOT prove the foreground control accepted text | source=samsung-tv-ws-api remote.py publisher=xchwarze/GitHub pub=undated accessed=2026-10-16 | confidence=high | class=pattern
- TV browser URL entry is the only third-party-adjacent surface with a repeated open-source text-injection recipe (launch browser + keyCode text string with quoted text) | source=Samsung TV Binding publisher=openHAB pub=undated accessed=2026-10-16 | confidence=medium | class=ecosystem
- Netflix/YouTube/Prime search on Samsung TVs is consistently driven in open-source recipes via D-pad keys and app launch IDs, not via SendInputString; no positive acceptance report found in budget | source=Home Assistant app-launch thread + openHAB long-press-RETURN-exits-Netflix note publisher=community pub=undated accessed=2026-10-16 | confidence=low | class=ecosystem
- Password fields have a separate `send_password` path in the WS lib, but acceptance/focus behavior on Samsung system PIN/password dialogs is unverified in budget | source=samsung-tv-ws-api remote.py publisher=xchwarze/GitHub pub=undated accessed=2026-10-16 | confidence=low | class=other

### Q4 — voice-set → channel primitives
- Power/navigation/system map 1:1 to KEY_POWER/HOME/MENU/SOURCE/RETURN + UP/DOWN/LEFT/RIGHT/ENTER | source=COMMANDS.md publisher=xchwarze/GitHub pub=undated accessed=2026-10-16 | confidence=high | class=compat
- Volume/channel/digits map 1:1: VOLUP/VOLDOWN/MUTE, CHUP/CHDOWN/PRECH/CH_LIST, KEY_0–KEY_9 (+ENTER for tune) | source=COMMANDS.md publisher=xchwarze/GitHub pub=undated accessed=2026-10-16 | confidence=high | class=compat
- Transport maps 1:1: PLAY/PAUSE/STOP/REWIND/FF/REC/LIVE + color keys RED/GREEN/YELLOW/BLUE, GUIDE/INFO/TOOLS | source=COMMANDS.md + Remote Control guide publisher=xchwarze/Samsung Developer pub=undated accessed=2026-10-16 | confidence=high | class=compat
- App names map only via out-of-channel deep-link launch (app IDs, e.g. Netflix 3201907018807 / YouTube 111299001912), not via a WS key; availability varies by model/region and post-2019 discovery API removal | source=Home Assistant app IDs + openHAB App Discovery publisher=community/openHAB pub=undated accessed=2026-10-16 | confidence=medium | class=ecosystem
- Short dictated phrases map ONLY to SendInputString when a native IME control is focused; unmappable when an app-drawn grid has focus (see Q3) | source=Keyboard/IME + remote.py publisher=Samsung/xchwarze pub=undated accessed=2026-10-16 | confidence=medium | class=pattern
- Pointer paths ("swipe to X", "click the third tile", drag/scroll trajectories) have no single-key equivalent; ProcessMouseDevice exposes only move-to-x/y + click (ENTER), so paths must be scripted and are fragile | source=COMMANDS.md cursor section + openHAB mouse events publisher=xchwarze/openHAB pub=undated accessed=2026-10-16 | confidence=medium | class=pattern
- Long-press semantics (e.g. hold RETURN to exit Netflix, hold POWER) need Press→sleep→Release (`hold-key`), not a plain Click; voice set must distinguish tap vs hold | source=COMMANDS.md + openHAB keyCode channel publisher=xchwarze/openHAB pub=undated accessed=2026-10-16 | confidence=medium | class=pattern

### Q7 — codec vectors (Samsung general-spec tables are authoritative for USB/player, NOT DLNA — this caveat applies to every Samsung-spec claim below)
- Samsung general-spec media hub defers video to per-year/per-device tables and covers music/picture/MSE-EME; it is authoritative for USB/player, NOT DLNA — DLNA negotiation must be validated against the renderer | source=Media Specifications publisher=Samsung Developer pub=undated accessed=2026-10-16 | confidence=high | class=other
- Per-model video table (2022 TV, authoritative for USB/player, NOT DLNA): H.264 + HEVC(H.265 Main10) in AVI/MKV/ASF/MP4/MOV/TS-family containers; audio AC3/LPCM/AAC/HE-AAC/DD+/MP3/OPUS etc.; resolution/bitrate caps vary by codec | source=2022 TV Video Specifications publisher=Samsung Developer pub=undated accessed=2026-10-16 | confidence=high | class=version
- DTS audio codec not supported on 2021 TVs (authoritative for USB/player, NOT DLNA); WMA dropped on 2025+; RA/RMVB dropped on 2026+ (authoritative for USB/player, NOT DLNA) | source=2021 TV Video Specs note + Media Specifications note publisher=Samsung Developer pub=undated accessed=2026-10-16 | confidence=high | class=version
- Renderer-tested: jellyfin-tizen explicitly disables DTS (transcode) on 2018+; HD-audio files (DTS-MA/TrueHD/Atmos) hang playback unless a compatible track is default/first; AAC expected OK | source=Audio Codecs Incompatibility (jellyfin-tizen #90) publisher=GitHub community pub=undated accessed=2026-10-16 | confidence=medium | class=ecosystem
- Renderer-tested ladder anchors: H.264 all models; HEVC 2016+; VP9 2019+; AV1 2022+; no Dolby Vision (HDR10/HDR10+ only); TrueHD decoded to PCM at best | source=Jellyfin on Samsung TV guide codec table publisher=JellyWatch pub=2026 accessed=2026-10-16 | confidence=low | class=ecosystem
- Subtitle/metadata: Samsung USB guidance is same-basename SRT + UTF-8; over DLNA external-SRT delivery is renderer/server-dependent and repeatedly reported missing (WD/Serviio/Jellyfin-DLNA threads); PGS image subs require transcode/burn-in | source=Serviio/WD/Emby community threads + USB subtitle guide publisher=community/YouTube pub=undated accessed=2026-10-16 | confidence=medium | class=ecosystem
- UMS renderer-profile workflow (match headers → per-device .conf) is the established method for pinning DLNA vectors per Samsung model; no Samsung DLNA matrix found in budget | source=How to improve support for my device publisher=Universal Media Server pub=undated accessed=2026-10-16 | confidence=medium | class=pattern

## LEADS
- 2021–2026 Samsung per-year video-spec pages (pin V1 caps per target model year) — Samsung Developer.
- UMS Samsung renderer .conf files (transcode/DLNA flags per model) — UniversalMediaServer GitHub.
- jellyfin-tizen browserDeviceProfile + issues #286/#289 (HEVC/4K-HDR failure modes) — GitHub.
- openHAB SamsungTV binding full page §keyCode/mouse/apps (text-entry + pointer recipes) — openhab.org.
- samsung-tv-ws-api remote.py + issues (SendInputString/SendInputEnd/password behavior reports) — GitHub.
- Samsung Subtitles + AVPlay guides (subtitle formats over non-USB paths) — Samsung Developer.

## NOT FOUND (absence of evidence = finding, not "unsupported")
- No Samsung doc stating Netflix/YouTube/Prime search grids accept SendInputString (unknown cells, must be device-tested per app).
- No public Samsung DLNA codec matrix (all Samsung-spec codec cells are USB/player-only; DLNA cells unknown until renderer-tested).
- No per-app IME-focus probe results (which app surfaces expose native input vs canvas/grid) within budget.
- No authoritative Bixby/voice-key injection contract over local WS (voice set must stay key-primitives only).
- No Samsung DLNA subtitle-metadata (SRT/ASS/PGS over DLNA) guarantee found.
