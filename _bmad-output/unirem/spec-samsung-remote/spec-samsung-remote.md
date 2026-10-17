---
id: SPEC-samsung-remote
companions: [protocol-reference.md, verification-matrix.md]
sources: [../research-native-kotlin-samsung-remote/research-native-kotlin-samsung-remote.md]
---

> **Canonical contract.** This SPEC and the files in `companions:` are the complete, preservation-validated contract for what to build, test, and validate. Source documents listed in frontmatter are for traceability — consult them only if you need narrative rationale or prose color this contract intentionally omits.

# UniRem — native-Kotlin Samsung TV remote

## Why

A vision to realize, over a concrete pain: Samsung TV owners juggle the physical remote, the first-party app, and separate casting/mirroring flows — no single native app covers responsive local keys, phone-keyboard text, voice, a touchpad pointer, DLNA casting, wake, and mirroring against Tizen TVs. UniRem makes that one Android app exist for Samsung Tizen TVs (2016+ committed, 2014–15 H/J as unsupported best-effort), built capability-gated per model because the local control channel is community-reverse-engineered with no vendor compatibility contract.

## Capabilities

- **CAP-1**
  - **intent:** User can drive core TV keys (D-pad, OK, back, home, volume, mute, play/pause) from the phone with observed on-TV effect.
  - **success:** On each matrix-passing model, every MVP key press produces the corresponding on-TV response; outcomes distinguish submitted/unconfirmed/observed, and unsupported keys report instead of failing silently.
- **CAP-2**
  - **intent:** User can send phone-typed text into the TV's focused native text field or browser URL field as an explicit send action.
  - **success:** Sent string appears verbatim in a native Settings/browser field on matrix models, with send/cancel semantics and focus state; app-drawn character grids are excluded unless a per-surface probe passes.
- **CAP-3**
  - **intent:** User can drive an on-TV pointer from a phone touchpad surface where the TV surface follows pointer input.
  - **success:** Pointer visibly moves and click activates on models passing the cursor probe; the feature stays hidden elsewhere with a plain-language explanation.
- **CAP-4**
  - **intent:** User can control the TV by phone-mic voice covering the full key set plus short dictation, executed as mapped keys or text through the phone-side pipeline.
  - **success:** Key commands and dictated phrases from the agreed set execute with observed on-TV effect; dictation sends only into focused native fields; raw-audio injection stays behind the voice spike gate and out of launch commitments.
- **CAP-5**
  - **intent:** User can power the TV off and wake it as two separate actions.
  - **success:** Power-off sends the power key with an asleep/offline state; Wake sends the magic packet from the stored MAC behind the setup checklist and reports success only when the TV answers on 8001/8002.
- **CAP-6**
  - **intent:** User can cast selected phone photo/audio/video to a discovered TV renderer and control playback, committing to rung-1 vectors with the ladder following per model.
  - **success:** JPEG/MP3/H.264-AAC short clips play with transport controls on validated renderers; HEVC, subtitles, seek, and 4K/long-file follow per model; serving is bounded to selected items with unguessable IDs, stops at session end, and survives screen-off during the session.
- **CAP-7**
  - **intent:** User can mirror the phone screen to the TV through the TV-side WebRTC receiver.
  - **success:** Mirrored output is visible on the receiver with latency and battery figures recorded per spike; mirroring without a TV-side receiver is excluded.
- **CAP-8**
  - **intent:** User can pair, persist, and switch between multiple TVs with explicit target selection.
  - **success:** Two TVs pair and persist across restart per device; reconnect uses backoff; pairing-required and identity-changed states surface a re-pair flow.

## Constraints

- Native Kotlin Android app with targetSdk 36 (Play requirement effective 2026-08-31) and minSdk 26 (no API-29-only dependency found; 29 sheds reach for nothing).
- No Samsung compatibility contract exists: every remote feature ships behind a provider with capability detection, and nothing claims support a physical test has not observed.
- Socket-write success is never presented as TV action; command outcomes are submitted/unconfirmed/observed.
- Per-TV trust-on-first-use SPKI pin plus token in a client scoped to that TV; the pin is validated before the token is sent, mismatch triggers a visible re-pair, and there are no trust-all managers and no silent WSS-to-WS downgrade.
- Pairing material lives in Keystore-backed encrypted storage, excluded from backup, never logged.
- The remote socket lives only while the remote UI is active; foreground services exist only for user-started cast/mirror/voice sessions with the matching service type.
- Casting is TV-fetches-from-phone: the TV receives an http(s) URL never a content:// URI, and the phone server supports HEAD and Range with stable reachability during the session.
- Discovery and wake assume one broadcast domain with manual IP entry always offered, and isolation is explained in onboarding diagnostics.
- Wake is committed per model on any network type — Wi-Fi standby included — only once the verification matrix records a pass for that model and network type; a model without a recorded pass surfaces no wake claim.
- Cloud remote stays out of the key-repeat hot path; key, text, and pointer traffic is local-LAN only.
- DLNA stack is jUPnP 3.0.5 behind an app-owned renderer gateway seam; UPnPCast adoption only if its Central provenance resolves or a pinned artifact is formally accepted.
- The V1 mirroring receiver ships as a Tizen dev-mode sideload for pilot and matrix validation; a store listing is pursued only after CAP-7 latency and battery figures are recorded.
- H/J 2014–15 sets, where attempted, are explicitly labeled unsupported best-effort in product and UI copy and never counted toward matrix pass claims.

## Non-goals

- Mirroring without a TV-side receiver.
- Compatibility claims beyond the tested matrix; "universal" means gated per model, never blanket.
- Pre-2016 Orsay sets and the legacy port-55000 protocol.
- Raw voice-audio injection as a launch commitment.
- Text entry into app-drawn character grids as a launch promise.
- An iOS client.

## Success signal

A user pairs a recent Tizen TV plus a second older set, then drives keys, sends a text string, moves the pointer, casts a photo and a video, powers off, wakes, and mirrors through the receiver — every step with observed on-TV effect — while the matrix records model, firmware, and network context per result. That demonstration, not merged code, is the release gate.

## Assumptions

- Target floor is Tizen 2016+ committed; Orsay is excluded per the research.
- H/J 2014–15 sets are attempted as unsupported best-effort via the encrypted flow, with failure falling back to declared-unsupported; the user accepted the support-load risk against the recommendation.
- Phone and TV share a LAN at pairing time, and the user can answer the TV Allow prompt with the physical remote.
- Phone-side speech-to-text mapped to keys plus short dictation counts as the launch voice implementation; raw injection is a spike, per the full-vision direction with legalities parked.
- Mirroring ships only with a TV-side receiver the project builds or adopts; V1 distribution is the dev-mode sideload pilot per the constraints.

## Open Questions

None — all spec-time questions resolved (see `.memlog.md`).
