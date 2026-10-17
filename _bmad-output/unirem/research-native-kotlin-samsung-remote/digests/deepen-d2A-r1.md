# Deepen D2A-R1 — Native-Kotlin Samsung Remote: support matrix / wake / receiver distribution

## Claims

### Q1 — Support matrix
- Local WS remote is Tizen-only (2016+/K-series onward): ws port 8001, wss port 8002; if TV supports wss/8002 it MUST be used or keyCode fails | source=Samsung TV binding publisher=openHAB pub=undated accessed=2026-10-16 | confidence=high | class=compat
- Legacy protocol is port 55000 for C(2010)/D(2011)/E(2012)/F(2013); H(2014)+J(2015) use PIN+encryption and are NOT supported by openHAB binding | source=Samsung TV binding publisher=openHAB pub=undated accessed=2026-10-16 | confidence=high | class=compat
- HA integration scope is Tizen (2016+) WebSocket (+REST); H/J subset uses encrypted pairing flow auto-detected on WS connect | source=Samsung Smart TV publisher=Home Assistant pub=undated (v2026.10.2) accessed=2026-10-16 | confidence=high | class=compat
- Modern WS library targets all Tizen 2016+ via Ethernet-or-WiFi; Orsay H(2014)+part-of-J(2015) only via encrypted v1 API extra | source=samsung-tv-ws-api README publisher=xchwarze/GitHub pub=undated accessed=2026-10-16 | confidence=high | class=compat
- Official compat anchors are SmartView supported-TVs page + TV model-groups page (linked as references, not scraped this run) | source=samsung-tv-ws-api README publisher=xchwarze/GitHub pub=undated accessed=2026-10-16 | confidence=medium | class=other
- WS requires same subnet/VLAN; cross-subnet fails (masquerade/proxy only workaround); token pairing popup must be accepted ≤30s then token cached | source=Samsung TV binding + Samsung Smart TV + samsung-tv-ws-api publisher=openHAB/Home Assistant/xchwarze pub=undated accessed=2026-10-16 | confidence=high | class=pattern
- Newer TVs default Access Notification to ask-every-time; set Device Connection Manager → Access Notification → First Time Only + purge Device List to stop re-prompt | source=Samsung Smart TV + samsung-tv-ws-api publisher=Home Assistant/xchwarze pub=undated accessed=2026-10-16 | confidence=high | class=pattern
- Regional/firmware variance: some channels need SmartThings/apps config on >2016; brightness/contrast/sharpness dead on newer sets; app-list API removed ~2019/2020 (manual samsungtv.cfg needed); Frame art API removed 2022 then restored in fw 1622+ | source=Samsung TV binding publisher=openHAB pub=undated accessed=2026-10-16 | confidence=medium | class=ecosystem
- Orsay (pre-2016, pre-Tizen) excluded from 8001/8002 WS remote; at best encrypted-legacy path, not the native-Kotlin WS target | source=Samsung TV binding + samsung-tv-ws-api publisher=openHAB/xchwarze pub=undated accessed=2026-10-16 | confidence=high | class=compat

### Q8 — Wi-Fi wake
- Master switch is TV setting Power On with Mobile: 2022–2026 path Settings→All Settings→Connections→Network→Expert Settings; 2017–2021 path Settings→General→Network→Expert Settings; OFF blocks SmartThings/mobile power-on | source=Power on your TV with the SmartThings App publisher=Samsung Support AU pub=undated accessed=2026-10-16 | confidence=high | class=version
- Samsung developer model is WoWLAN (wireless) primary; wired WoL noted as also-supported on '16 TVs with LAN cable | source=Enhanced Features (Smart View SDK) publisher=Samsung Developer pub=undated accessed=2026-10-16 | confidence=high | class=pattern
- WoWLAN precondition: at least one prior successful connection so TV registers mobile MAC; sender stores TV wifiMac (getDeviceInfo) then WakeOnWirelessLan / WakeOnWirelessAndConnect | source=Enhanced Features (Smart View SDK) publisher=Samsung Developer pub=undated accessed=2026-10-16 | confidence=high | class=pattern
- SDK 2.4.0+ automates WOW: standby TVs surface ~7s after discovery, connect() wakes then waits ~1min for service; older manual flow: vanish ≤60s, reappear (standby) ≤30s | source=Enhanced Features (Smart View SDK) publisher=Samsung Developer pub=undated accessed=2026-10-16 | confidence=medium | class=version
- HA turn_on sends WOL magic packet when MAC known from discovery; requires WOL enabled on TV | source=Samsung Smart TV publisher=Home Assistant pub=undated accessed=2026-10-16 | confidence=high | class=pattern
- openHAB WOL quirks: post-2016 only; never <2016; fails on wired Ethernet when soundbar on ARC/eARC; works on Wi-Fi ±soundbar; may need TV-side enable; wait up to ~1min after power-off before WOL (TV not yet in standby) | source=Samsung TV binding publisher=openHAB pub=undated accessed=2026-10-16 | confidence=medium | class=ecosystem
- Deep-sleep Ethernet-port-down reports (30–60s after off) and some models with no WOL at all (e.g. UE55ES7000, UN75HU8850 threads) explain per-model unreliability | source=SmartThings Community / HA Community / Samsung US Community publisher=community threads pub=mixed/undated accessed=2026-10-16 | confidence=low | class=ecosystem
- Cross-subnet WS/WOL limitation corroborated across all three open-source sources; WOL needs same-L2 or subnet-directed broadcast forwarding — 255.255.255.255 will not cross routers; ARP-expiry/refresh specifics not documented by Samsung this run | source=Samsung TV binding + Samsung Smart TV + samsung-tv-ws-api publisher=openHAB/Home Assistant/xchwarze pub=undated accessed=2026-10-16 | confidence=medium | class=pattern

### Q6 — Receiver distribution
- Store path is TV Seller Office → Request New Release → Samsung review/verification → Launched per model-group + service country; defects via Applications>Defect Resolve; upgrades resubmitted | source=Distributing Applications publisher=Samsung Developer pub=undated accessed=2026-10-16 | confidence=high | class=pattern
- Release scoping is by model group (all Tizen versions in group included); Tizen backward-compat default single version; Tizen .NET only 2018+ groups; phased rollout 3%→10%→100% for updates only (not first release, not ≤2020 groups) | source=Distributing Applications publisher=Samsung Developer pub=undated accessed=2026-10-16 | confidence=high | class=version
- Public sellers default to US distribution; wider countries need partnership | source=Samsung Apps TV Seller Office / Publishing-an-application publisher=Samsung Developer pub=undated accessed=2026-10-16 | confidence=medium | class=other
- Closed beta: 2021+ model groups, ≤90d (extendable to 180d), ≤1000 CSV activation codes (one-time), tester enters hidden key 134678 in Settings then Install; close reverts to normal version or deletes | source=Distributing Applications publisher=Samsung Developer pub=undated accessed=2026-10-16 | confidence=high | class=version
- Alpha (partner sellers only): 1 test at a time, ≤50 DUID TVs, ≤30d, 2020+ non-China, no approval; DUID from Support>About This TV>Smart Hub Information; TV must have used Smart Service within last month + be online | source=Distributing Applications publisher=Samsung Developer pub=undated accessed=2026-10-16 | confidence=high | class=version
- Moonlight-Tizen precedent is sideload, not store: enable Developer Mode (Apps panel → 12345 → ON + Host PC IP → power-cycle, DEVELOP MODE banner), same-LAN, SDB connect to TV_IP:26101, tizen install Moonlight.wgt; Tizen 7+ needs Samsung-account cert signing (author+distributor, cached); Electron/Docker installer automates this | source=MoonlightTizenInstaller README publisher=SmookeyDev/GitHub pub=undated accessed=2026-10-16 | confidence=high | class=pattern
- USB fallback for Moonlight (Tizen 5.5+): root/userwidget + app.tmg + widget.license, insert and wait; persistence flagged unreliable vs SDB method | source=moonlight-chrome-tizen Discussion #33 (v1.2.0) publisher=OneLiberty/GitHub pub=2024-11-17 accessed=2026-10-16 | confidence=medium | class=ecosystem
- Samsung documents Moonlight-on-Tizen WASM adaptation (enet + Tizen Sockets Extension + WASM player pipeline) — de-facto acknowledgement of sideload receiver pattern | source=Game Streaming on Tizen TV with Wasm publisher=Samsung Developer pub=undated accessed=2026-10-16 | confidence=medium | class=ecosystem

## LEADS
- Fetch SmartView Supported TVs + TV Model Groups pages to pin year→Tizen→model-letter table (cited but unread this run).
- Check jellyfin-tizen #222 (Seller Office release thread) for current certification latency/pitfalls.
- Re-check openHAB tested-TV-models table + HA known-issues for per-year WOL success rates.
- Confirm Developer Mode expiry/persistence policy and Tizen 8/9 cert changes (ToyPoodle 2025 video + Tizen Studio docs).

## NOT FOUND
- Authoritative Samsung spec page stating exact year cutover Orsay→Tizen for WS remote (inferred from 2016/K-series consensus, not a single Samsung matrix line).
- Samsung-authored subnet/broadcast + ARP-expiry behavior for WOL/WoWLAN.
- Samsung enterprise/kiosk distribution option for TV companion receiver (MDM/ Hospitality Display path unevaluated).
- Current (≤1mo) store review latency or 2026 policy change notice.
