# Verification matrix (UniRem Samsung remote)

Companion to `spec-samsung-remote.md`. Spikes, probes, and gates; owned by bmad-spec.

## Spikes (in run order)

1. **A — keys and pairing (1–2 iterations).** ≥3 model years incl. one recent Tizen: Allow/Deny, token persistence, pin flow, cert-change handling, re-prompt case, reconnect, target selection, MVP key set. Measure key latency. Covers CAP-1, CAP-8.
2. **A2 — wake.** MAC capture, subnet-directed plus limited broadcast, setting on/off, Wi-Fi vs Ethernet, short vs long standby; confirm 8001/8002 answers after wake. Covers CAP-5.
3. **B — text and pointer gates.** Native-field arrival plus `imeStart`/`imeEnd` delivery; streaming-app grids tested separately; pointer prototype only where the cursor probe passes. Covers CAP-2, CAP-3.
4. **C — casting.** jUPnP spike: multicast lock, server reachability, `GET`/`HEAD`/`Range`, JPEG/MP3/H.264-AAC vectors, then HEVC, subtitles, seek, long files, screen-off behavior; CDDL/Jetty audit. UPnPCast only after provenance resolves. Covers CAP-6.
5. **V — voice.** Phone STT→keys/text build with the agreed command set, latency measured; raw-injection hypothesis needs Pastebin retrieval plus a proven TV ingestion endpoint before any roadmap claim. Covers CAP-4.
6. **M — mirroring, order B→A→C.** B: WebRTC to the Tizen receiver first (only low-latency path); A: live-URI→AVTransport falsification second (cheap kill-or-revive); C: DIAL/launch harness last. Record latency and battery per transport. Covers CAP-7.
7. **L — local-network permission.** Full flow under targetSdk-37 behavior (discovery, socket, WOL, media server) before rollout.

## Observed-effect probes (socket-write success never counts)

- Keys: on-TV response per key. Text: glyphs appear in the focused field. Pointer: cursor visibly moves, click activates. Wake: TV answers on 8001/8002. Cast: TV renders with responsive transport. Mirror: output visible on the receiver.

## Matrix template

| Model / year / region | Firmware | Network (Wi-Fi band / Ethernet / VLAN) | /api/v2/ | Pair prompt | Token persists | Re-prompt? | Pin stable | Keys MVP | Text native | Text app-grid | Pointer | Wake | DMR | JPEG/MP3/H264 | HEVC | Subtitles | Seek | Notes |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| e.g. QN90x / 2024 | | | | | | | | | | | | | | | | | | |

## MVP green-light gates

Core remote plus casting ship only after: keys and pairing pass on ≥3 model years; text/pointer gated per surface; cast vectors pass on validated renderers; wake validated per network type; mirroring held to its receiver plus recorded figures. Voice ships as STT-to-command; raw injection stays a spike. No fleet-wide percentages without measured data.
