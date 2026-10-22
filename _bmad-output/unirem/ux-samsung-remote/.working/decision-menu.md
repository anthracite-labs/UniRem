---
title: UX decision menu — Living Room Noir + Graphite Mint
status: accepted — all recommendations
created: 2026-10-22
updated: 2026-10-22
---

# UX decision menu

Purpose: resolve the open forks before drafting `DESIGN.md` and `EXPERIENCE.md`. Each item lists viable options and a recommendation. Options that would violate the spec are marked **not viable** and are not counted as choices.

Chosen spine: **Living Room Noir** composition. Chosen skin: **Graphite Mint** palette.

> **Resolved 2026-10-22:** all 24 recommendations accepted. The canonical decision entries live in `.memlog.md`; this file records the menu they were chosen from.

## Visual system

### 1. Accent ownership
- **A.** Mint everywhere — OK key, caret, active states, `ring.observed`.
- **B.** Mint ring-only — OK stays warm ivory/champagne.
- **C.** Neutral OK — mint lives only in the ring.

**Recommend A.** The Graphite Mint board already resolves it this way; one accent is calmer than two, and the ring still separates state through glow and motion. Trade-off: the ring is no longer the only saturated element, so honesty leans more on behavior than color exclusivity.

### 2. Hero mode
- **A.** Thumb-arc only (Noir).
- **B.** Big-pad only (Tactile/Signal).
- **C.** Both, user-selectable.

**Recommend A for V1.** Noir's composition is built around the bottom-third thumb arc; shipping both doubles layout and QA and splits the design language. Keep big-pad as a post-V1 mode.

### 3. Iconography
- **A.** Emoji/Unicode, as mocked.
- **B.** Material Symbols.
- **C.** A small custom vector set (10–12 icons).

**Recommend C.** Emoji rendering varies by OEM and reads playful, which fights the Noir register. A custom set is cheap at this scale and makes the app feel designed. If timeline is tight, B is the pragmatic fallback.

### 4. Light mode
- **A.** Dark-only for V1.
- **B.** Dark + light at launch.
- **C.** Follow system, dark default.

**Recommend A.** The product lives in a dark room; the color board calls light "secondary, simplified"; light doubles token and contrast QA. Log it as a post-V1 accessibility item, not a launch promise.

### 5. Motion
- **A.** Noir breath — 900ms pulse on submit, 400ms settle on observed.
- **B.** System snaps — 150–300ms.
- **C.** No motion.

**Recommend A**, honoring reduced-motion. The ring is the honesty signal; motion is how "sent" becomes "confirmed" without a spinner. Nothing else moves.

## Structure and surfaces

### 6. Launcher model
- **A.** Six slots: Text, Voice, Pad, Cast, Mirror, Wake.
- **B.** Four primary + "More" sheet.
- **C.** Tabs or a side rail.

**Recommend A, with gated slots visible but disabled and explained.** Six capabilities are the product's promise; hiding them makes the app feel smaller. Noir's low density can absorb six 48–56dp slots.

### 7. Text entry
- **A.** Bottom sheet editor over the hero.
- **B.** Full-screen editor.
- **C.** Inline keyboard takeover.

**Recommend A.** The TV field is the context; a sheet keeps the TV identity and hero visible, and makes send/cancel explicit. Full-screen hides the remote; inline takeover is fragile because focus events are unreliable.

### 8. Voice
- **A.** Press-and-hold to talk (walkie-talkie).
- **B.** Tap to start, tap to stop.
- **C.** Always-listening wake word. **Not viable** — privacy, battery, and user-started-flow constraints.

**Recommend A for commands, with a lock-to-dictate option for longer text.** Press-and-hold makes the mic session explicit and bounded and prevents accidental capture.

### 9. Cast flow
- **A.** Pick media first, then confirm the TV.
- **B.** Pick the TV first, then media.
- **C.** Auto-cast to the active TV.

**Recommend A, with the active TV preselected.** Casting is item-scoped ("serving is bounded to selected items"), so the user starts with what they want to show. Auto-cast is fine with one TV but violates explicit selection when there are several.

### 10. Mirror flow
- **A.** Check the receiver first, then ask for capture consent.
- **B.** Ask for consent first, then check the receiver.
- **C.** Hide Mirror until a receiver is detected.

**Recommend A, with a receiver-required setup state rather than a hidden slot.** Never ask for screen capture before we know there's a destination. The receiver is the hard dependency; if it's missing, explain how to install or enable it.

### 11. Wake flow
- **A.** Inline Wake action + checklist sheet.
- **B.** Dedicated Wake screen.
- **C.** Show Wake but disable it on models without a recorded pass.

**Recommend A for claimed models, C for unclaimed ones.** Wake is an action, not a destination; the checklist appears only when prerequisites are unmet. "No recorded pass" means no wake claim — the slot should say so rather than fail.

### 12. Session ownership when switching TVs
- **A.** Sessions stay pinned to their TV; switching targets leaves them running with a persistent session chip.
- **B.** Switching targets stops all sessions.
- **C.** Prompt the user every time.

**Recommend A.** The spec says navigation or receiver changes must not redirect or stop unrelated sessions. A visible "Casting to Living Room TV" chip keeps the state honest without a modal.

### 13. Onboarding and discovery
- **A.** Auto-discovery first, manual IP only on failure.
- **B.** Guided setup wizard from first launch.
- **C.** Manual IP first.

**Recommend A.** Discovery failure is normal, not exceptional; the manual path must be visible but not the default. The empty-state mockups already support this shape.

## State and failure

### 14. Two-channel state model
- **A.** Topbar dot = connection health; ring = last command outcome.
- **B.** One channel for both.
- **C.** Text-only status line.

**Recommend A.** The color board's simultaneous "observed" and offline chip is a contradiction. Separating connection health from command outcome removes it and gives each signal one job.

### 15. Ring persistence
- **A.** Observed holds 3–5s, then returns to idle; unconfirmed/offline hold until resolved.
- **B.** Ring holds the last state indefinitely.
- **C.** Ring returns to idle after every press.

**Recommend A.** The ring is a momentary honesty signal. Holding observed forever makes idle invisible and turns the ring into decoration; unconfirmed and offline need to persist because they need resolution.

### 16. Offline key behavior
- **A.** Disable keys when the socket is closed; allow sends with "unconfirmed" when the socket is open but the TV isn't answering.
- **B.** Always allow sends, mark unconfirmed.
- **C.** Queue commands until reconnect. **Not viable** — a silent queue is a false promise.

**Recommend A.** It distinguishes "no channel" from "channel open, no confirmation," which is the truth.

### 17. State labels
- **A.** Spec terms in the UI: submitted / unconfirmed / observed.
- **B.** Plain language: Sent / Waiting for TV / TV confirmed.
- **C.** Icons only.

**Recommend B in the UI, A in the docs.** "Submitted" is protocol language. The spec requires three distinct levels, not the exact words; document the mapping so the contract stays traceable.

## Trust, platform, and tone

### 18. Multi-TV selection
- **A.** Auto-select when one TV; explicit picker when more than one.
- **B.** Always show the picker.
- **C.** Implicit last-used. **Not viable** — the spec forbids it when multiple TVs exist.

**Recommend A.** It matches the spec's "explicit target selection" without adding friction to the common single-TV case.

### 19. Pairing identity display
- **A.** Name + model + masked MAC, as in the Native mockup.
- **B.** Name + model + short SPKI fingerprint.
- **C.** Name + model only.

**Recommend A, with the fingerprint available under "Details."** The MAC is a familiar secondary check; the fingerprint is the actual trust anchor but too technical for the primary card. The real confirmation is still the TV's Allow overlay.

### 20. Capability explanation placement
- **A.** One-time chip at the moment of relevance, plus a "TV details" screen.
- **B.** Persistent badge on every gated key.
- **C.** Settings-only documentation.

**Recommend A.** Explain at the moment it matters, then keep a durable answer in TV details. Persistent badges clutter the hero; settings-only hides the explanation from the person who needs it.

### 21. Permission sequencing
- **A.** In-context, just-in-time, with a short pre-explanation before the OS sheet.
- **B.** Pre-flight permission wall at first launch.
- **C.** Ask only when the OS forces it.

**Recommend A.** Local network at discovery, mic at voice, capture at mirror. A permission wall at launch is hostile and teaches nothing.

### 22. Accessibility floor
- **A.** WCAG AA minimum, AAA for body text; 48dp targets; TalkBack labels and live state announcements; honor reduced motion.
- **B.** AA only.
- **C.** No formal target.

**Recommend A.** Dark-room legibility is the product's promise. Graphite Mint has low-contrast pairs (`ring.idle` on `surface.base`, `text.secondary` on `surface.key`) that need checking before they become tokens.

### 23. Tone
- **A.** Plain, calm, specific — the mockup voice.
- **B.** Technical/status-heavy.
- **C.** Playful/warm.

**Recommend A.** The trust story depends on precise, non-marketing language. Technical leaks implementation; playful undercuts the honesty.

### 24. Haptics
- **A.** Light press + distinct observed/offline patterns.
- **B.** Press only.
- **C.** None.

**Recommend A, with a settings toggle.** It's a physical remote; haptics replace the key travel the phone doesn't have and let the user keep their eyes on the TV.

## Not on the table

- Silent command queue.
- Implicit last-used TV when multiple are paired.
- Mirroring without a Tizen receiver.
- Always-listening microphone.
- Unqualified "universal" copy.
- App-grid text as a launch promise.
- Raw voice-audio injection as a launch commitment.
- Presenting socket-write success as TV action.

## Recommendation summary

If you accept all recommendations: 1A, 2A, 3C, 4A, 5A, 6A, 7A, 8A, 9A, 10A, 11A/C, 12A, 13A, 14A, 15A, 16A, 17B, 18A, 19A, 20A, 21A, 22A, 23A, 24A.

The one thing not on this menu is the protagonist. That is a story, not a choice: give me one real session and I will build the journey spine around it.
