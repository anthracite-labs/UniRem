# Mockups — provenance and use

Copied on 2026-10-22 from `_bmad-output/unirem/ux-visual-references/`. These are the five preserved HTML boards from the earlier UX exploration. They are composition references, not inherited decisions.

## Canonical

- `direction-noir.html` — the composition spine: topbar, one-time chip, 236dp state ring around the D-pad, horizontal utility row, four function keys, six-slot launcher, five-state legend.
- `color-themes-1.html` — the Graphite Mint palette. Only the Graphite Mint theme is canonical; the other five themes are exploration.

## Reference only

- `direction-native.html` — pairing screen and trust-on-first-use card. Its copy assumes a physical remote is present. Flow 1 changes that: the TV's controller button is the documented fallback, and the app must say when it cannot promise it.
- `direction-tactile.html` — TV management cards and the best-effort label. Its amber "connected" LED conflicts with the spine's rule that the topbar dot is connection health only. The ring carries command outcome; the card carries the durable support label.
- `direction-signal.html` — empty state, offline banner, and state line. Its "keys still send when offline" copy conflicts with the accepted decision to disable keys when the socket is closed. The spines win.

## Rules

- The boards are static HTML. Motion, timing, state semantics, accessibility, and final copy are owned by `DESIGN.md` and `EXPERIENCE.md`.
- Do not copy emoji glyphs, unicode placeholders, or the boards' type sizes. The product uses the custom vector set and the tokenized type ramp.
- When a board and a spine disagree, the spine wins. The board remains useful for composition, proportion, and the feeling of the surface.
