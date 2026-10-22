---
name: UniRem
description: A dark-room Android remote for Samsung Tizen TVs. Quiet graphite, one mint signal, and a ring that tells the truth about every command.
status: final
updated: 2026-10-23
sources:
  - ../spec-samsung-remote/spec-samsung-remote.md
  - ../research-native-kotlin-samsung-remote/research-native-kotlin-samsung-remote.md
  - .working/journey-options.md
  - mockups/direction-noir.html
  - mockups/color-themes-1.html
  - mockups/direction-native.html
  - mockups/direction-tactile.html
  - mockups/direction-signal.html
colors:
  surface-base: '#151719'
  surface-raised: '#1E2124'
  surface-key: '#2A2E32'
  surface-key-pressed: '#343A3E'
  ink-primary: '#ECEEF0'
  ink-secondary: '#98A1A9'
  ink-disabled: '#8D959C'
  ink-on-accent: '#0B1A13'
  accent: '#3DDC97'
  accent-pressed: '#22B877'
  accent-wash: '#173A2B'
  hairline: '#343A3E'
  ring-idle: '#343A3E'
  ring-submitted: '#86E9BE'
  ring-observed: '#3DDC97'
  ring-unconfirmed: '#E8D44D'
  ring-offline: '#FF7A6B'
  status-connected: '#3DDC97'
  status-reconnecting: '#E8D44D'
  status-offline: '#FF7A6B'
  banner-surface: '#2B1A18'
  banner-border: '#5A2A25'
  banner-ink: '#FFD8D3'
  banner-action: '#FF9D92'
  focus-ring: '#86E9BE'
  scrim: '#0A0B0D'
typography:
  display:
    fontFamily: 'Roboto'
    fontSize: 32sp
    fontWeight: '300'
    lineHeight: '1.1'
    letterSpacing: '-0.01em'
  title:
    fontFamily: 'Roboto'
    fontSize: 24sp
    fontWeight: '400'
    lineHeight: '1.2'
    letterSpacing: '-0.005em'
  headline:
    fontFamily: 'Roboto'
    fontSize: 20sp
    fontWeight: '400'
    lineHeight: '1.25'
    letterSpacing: '0'
  body:
    fontFamily: 'Roboto'
    fontSize: 16sp
    fontWeight: '400'
    lineHeight: '1.5'
    letterSpacing: '0.005em'
  body-small:
    fontFamily: 'Roboto'
    fontSize: 14sp
    fontWeight: '400'
    lineHeight: '1.45'
    letterSpacing: '0.005em'
  label:
    fontFamily: 'Roboto'
    fontSize: 12sp
    fontWeight: '500'
    lineHeight: '1.3'
    letterSpacing: '0.08em'
  micro:
    fontFamily: 'Roboto'
    fontSize: 11sp
    fontWeight: '500'
    lineHeight: '1.3'
    letterSpacing: '0.12em'
  button:
    fontFamily: 'Roboto'
    fontSize: 15sp
    fontWeight: '500'
    lineHeight: '1.2'
    letterSpacing: '0.02em'
  state:
    fontFamily: 'Roboto'
    fontSize: 13sp
    fontWeight: '500'
    lineHeight: '1.3'
    letterSpacing: '0.02em'
  key:
    fontFamily: 'Roboto'
    fontSize: 15sp
    fontWeight: '400'
    lineHeight: '1.1'
    letterSpacing: '0.01em'
  code:
    fontFamily: 'monospace'
    fontSize: 12sp
    fontWeight: '400'
    lineHeight: '1.4'
    letterSpacing: '0.02em'
rounded:
  sm: 8dp
  md: 14dp
  lg: 18dp
  xl: 24dp
  full: 9999dp
spacing:
  '1': 4dp
  '2': 8dp
  '3': 12dp
  '4': 16dp
  '5': 20dp
  '6': 24dp
  '7': 32dp
  '8': 40dp
  '9': 48dp
  '10': 64dp
  screen-x: 20dp
  section: 24dp
  key-gap: 8dp
components:
  topbar:
    height: 56dp
    background: '{colors.surface-base}'
    border-bottom: '1dp {colors.hairline}'
    padding-x: 20dp
  tv-switcher:
    min-height: 48dp
    text: '{typography.body}'
    gap: 8dp
    caret: '{colors.accent}'
  health-dot:
    size: 8dp
    halo: 2dp
    connected: '{colors.status-connected}'
    reconnecting: '{colors.status-reconnecting}'
    offline: '{colors.status-offline}'
  one-time-chip:
    min-height: 48dp
    background: '{colors.surface-raised}'
    border: '1dp {colors.hairline}'
    radius: '{rounded.md}'
    padding: 14dp
    text: '{typography.body-small}'
    text-color: '{colors.ink-secondary}'
    close-target: 48dp
  state-ring:
    size: 236dp
    stroke: 2dp
    idle: '{colors.ring-idle}'
    submitted: '{colors.ring-submitted}'
    observed: '{colors.ring-observed}'
    unconfirmed: '{colors.ring-unconfirmed}'
    offline: '{colors.ring-offline}'
    glow: 24dp
    submit-breath: 900ms
    observed-hold: 4000ms
  dpad:
    columns: '64dp 76dp 64dp'
    rows: '60dp 70dp 60dp'
    gap: 4dp
    nav-size: '60dp x 56dp'
    nav-radius: '{rounded.md}'
    ok-size: 70dp
    ok-radius: '{rounded.full}'
    ok-background: '{colors.accent}'
    ok-foreground: '{colors.ink-on-accent}'
  utility-row:
    height: 48dp
    gap: 14dp
    rocker: '180dp x 48dp'
    rocker-segment: 60dp
    round-key: 48dp
    background: '{colors.surface-base}'
    border: '1dp {colors.hairline}'
  function-key:
    height: 52dp
    min-width: 64dp
    radius: '{rounded.md}'
    background: '{colors.surface-key}'
    border: '1dp {colors.hairline}'
    icon: 18dp
    gap: 4dp
    text: '{typography.key}'
  launcher:
    height: 84dp
    background: '{colors.surface-raised}'
    border-top: '1dp {colors.hairline}'
    radius-top: '{rounded.xl}'
    padding-x: 20dp
    padding-bottom: 12dp
  launcher-slot:
    width: 52dp
    height: 60dp
    icon: 44dp
    icon-background: '{colors.surface-key}'
    icon-border: '1dp {colors.hairline}'
    label-gap: 4dp
    label: '{typography.micro}'
  bottom-sheet:
    background: '{colors.surface-raised}'
    radius-top: '{rounded.xl}'
    padding-x: 20dp
    padding-top: 12dp
    padding-bottom: 24dp
    max-height: '80%'
    scrim: '{colors.scrim}'
    grabber: '38dp x 4dp'
  text-field:
    height: 52dp
    background: '{colors.surface-key}'
    border: '1dp {colors.hairline}'
    radius: '{rounded.md}'
    caret: '{colors.accent}'
    text: '{typography.body}'
  mic-key:
    size: 72dp
    background: '{colors.accent}'
    foreground: '{colors.ink-on-accent}'
    radius: '{rounded.full}'
    icon: 28dp
  session-chip:
    height: 48dp
    background: '{colors.surface-key}'
    border: '1dp {colors.hairline}'
    radius: '{rounded.full}'
    padding-x: 14dp
    text: '{typography.body-small}'
    gap: 8dp
  banner:
    background: '{colors.banner-surface}'
    border: '1dp {colors.banner-border}'
    radius: '{rounded.md}'
    padding: 16dp
    text: '{colors.banner-ink}'
    action: '{colors.banner-action}'
  state-line:
    min-height: 24dp
    text: '{typography.state}'
    idle: '{colors.ink-secondary}'
    submitted: '{colors.ring-submitted}'
    observed: '{colors.ring-observed}'
    unconfirmed: '{colors.ring-unconfirmed}'
    offline: '{colors.ring-offline}'
  pairing-card:
    background: '{colors.surface-raised}'
    border: '1dp {colors.hairline}'
    radius: '{rounded.lg}'
    padding: 20dp
    title: '{typography.headline}'
    meta: '{typography.body-small}'
  tv-card:
    background: '{colors.surface-raised}'
    border: '1dp {colors.hairline}'
    radius: '{rounded.lg}'
    padding: 16dp
    title: '{typography.headline}'
    meta: '{typography.body-small}'
  primary-button:
    height: 52dp
    background: '{colors.accent}'
    foreground: '{colors.ink-on-accent}'
    radius: '{rounded.md}'
    text: '{typography.button}'
  secondary-button:
    height: 48dp
    background: 'transparent'
    border: '1dp {colors.hairline}'
    foreground: '{colors.ink-primary}'
    radius: '{rounded.md}'
    text: '{typography.button}'
  destructive-button:
    height: 48dp
    background: 'transparent'
    border: '1dp {colors.ring-offline}'
    foreground: '{colors.ring-offline}'
    radius: '{rounded.md}'
    text: '{typography.button}'
  permission-explainer:
    background: '{colors.surface-raised}'
    radius: '{rounded.xl}'
    padding: 24dp
    title: '{typography.title}'
    body: '{typography.body}'
  empty-state:
    title: '{typography.display}'
    body: '{typography.body}'
    action: '{components.primary-button}'
---

# UniRem — Design Spine

> Living Room Noir composition with the Graphite Mint palette. Native Android, dark-only V1. This document owns how the product looks; `EXPERIENCE.md` owns how it behaves. When a mockup disagrees with either spine, the spine wins. Shared terms — paired, connected, submitted, unconfirmed, observed, best-effort, session — are defined in `EXPERIENCE.md` and used verbatim here.

## Brand & Style

UniRem is the remote that tells the truth in a dark room.

The TV is the brightest object in the room. The phone is an instrument you can use without looking away from the screen. That hierarchy drives everything: low density, a bottom-third thumb arc, hairline structure instead of cards, and one ambient ring that reports what the TV actually did. The interface should feel like a well-made remote laid on a side table — present, precise, and uninterested in competing with the picture.

The composition is **Living Room Noir**: the topbar, a single one-time chip, a horizontal utility row, four function keys, the 236dp state ring around the D-pad, a state line, and the six-slot launcher. On the Remote surface, nothing floats. Nothing is a dashboard. The upper half of the screen stays quiet so the thumb can find the controls without hunting.

The skin is **Graphite Mint**. Noir's warm champagne is replaced by a cool mint signal against lifted graphite. The mint is not decoration; it belongs to action, focus, and observed effect. The ring is the primary command-state channel. The only other full-width color surface is the oxblood recovery banner. Everywhere else, hierarchy comes from tone, spacing, and one-pixel lines.

Composition references: [`mockups/direction-noir.html`](mockups/direction-noir.html) for the hero, ring, utility row, launcher, and five-state legend; [`mockups/color-themes-1.html`](mockups/color-themes-1.html) for Graphite Mint; [`mockups/direction-native.html`](mockups/direction-native.html) for the pairing card and trust-on-first-use composition; [`mockups/direction-tactile.html`](mockups/direction-tactile.html) for TV management cards and the best-effort label; [`mockups/direction-signal.html`](mockups/direction-signal.html) for the empty state, offline banner, and state line. These boards are references, not inherited decisions. Their static HTML and placeholder glyphs are not specifications.

The product is not a smart-home hub, a media browser, or a voice assistant. It is a remote with a phone keyboard, a cast path, and the discipline to say when it cannot confirm something.

## Colors

The palette is built on three base roles — ground, ink, and signal — plus command, connection, and recovery channels. The roles do not trade places.

### Graphite ground

- **Base (`surface-base`, `#151719`)** is the screen. A lifted near-black, not a void: it keeps the phone from disappearing into a black bezel and gives hairlines somewhere to live.
- **Raised (`surface-raised`, `#1E2124`)** is for sheets, the launcher shelf, and cards. One tonal step, no shadow.
- **Key (`surface-key`, `#2A2E32`)** is the resting surface of every key. **Pressed (`surface-key-pressed`, `#343A3E`)** is the only pressed tone; there is no bevel, gradient, or 3D travel.

No fourth neutral surface. The banner is a semantic recovery surface, not a home for content. If a new container needs to stand out, it uses a hairline or a different section, not another gray.

### Ink

- **Primary (`ink-primary`, `#ECEEF0`)** is the reading color: key labels, body, titles. Soft white, never `#FFFFFF`.
- **Secondary (`ink-secondary`, `#98A1A9`)** is for metadata, explanations, and the state line at idle.
- **Disabled (`ink-disabled`, `#8D959C`)** is only for genuinely unavailable controls and their labels. It is never used for information the user needs.
- **On accent (`ink-on-accent`, `#0B1A13`)** is the only text color allowed on a mint fill.

### Mint signal

- **Accent (`accent`, `#3DDC97`)** is the OK key, the TV-switcher caret, the text caret, active states, and the observed ring. One mint family, one job: mark the live, focused, or confirmed thing.
- **Pressed (`accent-pressed`, `#22B877`)** is the OK key under the thumb.
- **Wash (`accent-wash`, `#173A2B`)** is a quiet tint for an active row or a selected capability. It never fills a large surface.

Mint is a signal, not a surface treatment. Do not fill a banner, a card, or a whole sheet with it. Mint fills are reserved for three actions: the 70dp OK key, the primary button, and the 72dp voice mic key.

### Ring states

The ring is the honesty channel. Its color is never decorative; the 8dp topbar health dot reuses the same state hues for a separate connection channel and never reports a command outcome.

| Token | Hex | Meaning |
|---|---|---|
| `ring-idle` | `#343A3E` | No command in flight. A faint baseline, not a status badge. |
| `ring-submitted` | `#86E9BE` | The phone wrote the command. Nothing from the TV yet. |
| `ring-observed` | `#3DDC97` | The TV confirmed the effect, or the user confirmed what they saw. |
| `ring-unconfirmed` | `#E8D44D` | The command was sent; the TV has not confirmed it. |
| `ring-offline` | `#FF7A6B` | The socket is closed. Keys are disabled until reconnect. |

`ring-submitted` is a pale mint, not a success color. `ring-observed` is the only mint that means "done." `ring-unconfirmed` is amber, not red; an unconfirmed key may still have worked. `ring-offline` is salmon, the loudest state the ring gets; it appears there only when the transport is down. The same salmon is reused for destructive action borders, never for a command state.

### Health and recovery

- **Health dot** uses `status-connected` (`#3DDC97`), `status-reconnecting` (`#E8D44D`), and `status-offline` (`#FF7A6B`). It lives in the topbar and reports the socket, never the last command. The ring reports the command, never the socket. Do not swap them.
- **Banner** is a desaturated oxblood (`banner-surface`, `#2B1A18`) with a warm hairline (`banner-border`, `#5A2A25`) and pale rose text (`banner-ink`, `#FFD8D3`). Offline is a fact to resolve, not an alarm. The action is `banner-action` (`#FF9D92`), not pure red.
- **Focus** is `focus-ring` (`#86E9BE`) at 2dp outside the focused control. It is deliberately the submitted mint, so focus reads as attention rather than success.

### Contrast

Load-bearing combinations, measured against WCAG 2.2:

| Pair | Ratio | Use |
|---|---:|---|
| `ink-primary` on `surface-base` | 15.45:1 | Body and key text — AAA |
| `ink-secondary` on `surface-base` | 6.85:1 | Metadata and explanations — AA |
| `accent` on `surface-base` | 10.17:1 | Mint text/icons on the screen — AAA |
| `ink-on-accent` on `accent` | 10.14:1 | OK key and primary button — AAA |
| `ink-on-accent` on `accent-pressed` | 6.99:1 | Pressed OK — AA |
| `ink-disabled` on `surface-key` | 4.50:1 | Disabled key labels — AA |
| `banner-action` on `banner-surface` | 8.30:1 | Banner action — AAA |
| `ring-observed` on `surface-base` | 10.17:1 | Observed ring — non-text, exceeds 3:1 |
| `ring-unconfirmed` on `surface-base` | 11.94:1 | Unconfirmed ring — non-text, exceeds 3:1 |
| `ring-offline` on `surface-base` | 7.06:1 | Offline ring — non-text, exceeds 3:1 |
| `banner-ink` on `banner-surface` | 12.67:1 | Banner copy — AAA |
| `focus-ring` on `surface-key` | 9.39:1 | Focus indicator — exceeds 3:1 |
| `ring-idle` on `surface-base` | 1.56:1 | **Exception:** decorative baseline only; never the sole state signal |
| `hairline` on `surface-base` | 1.56:1 | **Exception:** decorative divider only |

The idle ring and hairlines are intentionally below 3:1. Neither is allowed to carry information alone: the state line always names the state, and every key has a label. If a future surface needs a visible idle ring to be understood, it must raise the token rather than rely on the exception.

Never use color alone. Every ring state is paired with a text label; every health state has an accessibility label; every disabled slot explains itself.

## Typography

The type is Android's system sans, **Roboto**, with one monospace face for machine-readable strings. No downloaded display face, no serif, no condensed variant. The product should feel native in the hand, not branded at the expense of legibility.

| Role | Size / weight / line | Use |
|---|---|---|
| `display` | 32sp / 300 / 1.1 | Pairing title, empty-state title. Never below 28sp. |
| `title` | 24sp / 400 / 1.2 | Sheet titles, TV details section titles. |
| `headline` | 20sp / 400 / 1.25 | TV names, card titles. |
| `body` | 16sp / 400 / 1.5 | Instructions, explanations, permission copy. AAA target. |
| `body-small` | 14sp / 400 / 1.45 | Metadata, chip copy, secondary explanations. AA. |
| `label` | 12sp / 500 / 1.3 / 0.08em | Short section labels, button groups. |
| `micro` | 11sp / 500 / 1.3 / 0.12em | Launcher labels and tiny uppercase marks. Never sentences. |
| `button` | 15sp / 500 / 1.2 / 0.02em | Primary, secondary, destructive actions. |
| `state` | 13sp / 500 / 1.3 / 0.02em | "Sent", "Waiting for TV…", "TV confirmed." Tabular numerals. |
| `key` | 15sp / 400 / 1.1 | Key labels and function-key labels. |
| `code` | 12sp / 400 / 1.4 / 0.02em | Masked MAC, IP address, SPKI fingerprint, diagnostics. |

Rules:

- No weight below 400 below 18sp. The mockups' 300-weight microcopy is replaced by 400/500 at a larger size. Dark-room legibility beats thin elegance.
- Uppercase is reserved for `micro` labels of one to three words. Never set a sentence in uppercase, and never add letter-spacing to body text.
- Left-align everything. No justified text, no centered paragraphs longer than two lines.
- Keep body measure to roughly 40–60 characters. In a bottom sheet, that means 20dp side padding and no full-width paragraphs at tablet widths.
- Numbers in the state line, timestamps, and progress copy use tabular figures so values do not jitter.
- Dynamic type is honored in `sp`. At 200% font scale, launcher labels wrap to two lines and the launcher becomes a 3×2 grid; state copy wraps to two lines and the hero shifts upward. No critical state or action label may truncate to an ellipsis. Long TV names may truncate in the topbar because the full name is always available in the switcher and TV details.
- Text is never rendered inside an icon. The custom glyphs are line drawings, not letterforms.

## Layout & Spacing

The product is a single-column portrait phone surface, 360–480dp wide. The 4dp scale is the base; 20dp is the screen gutter; 24dp separates major sections.

### Vertical composition

1. **Status bar / cutout** — system, edge-to-edge, transparent.
2. **Topbar** — 56dp. TV switcher left, settings right.
3. **One-time chip** — 48dp minimum, 20dp side margins, only when a capability needs explaining.
4. **Quiet space** — flexible; the ring sits in the bottom third, not the middle.
5. **Utility row** — 48dp: volume rocker (180×48dp), play/pause (48dp), power (48dp).
6. **Function key row** — 52dp: Home, Back, Apps, Search, four equal columns with 8dp gaps.
7. **State ring / D-pad** — 236dp; ring centered; D-pad inside.
8. **State line** — 24dp minimum, centered, under the ring.
9. **Launcher** — 84dp plus the bottom safe area. Six slots.

The ring's center sits between 62% and 70% of the viewport height on a typical 800dp-tall phone. Extra height goes above the ring, never between the controls. On a short screen, reduce the quiet space first, then the ring to 208dp; never reduce a touch target below 48dp.

### D-pad

The D-pad is a 3×3 grid inside the ring:

- Columns: 64dp / 76dp / 64dp.
- Rows: 60dp / 70dp / 60dp.
- Gap: 4dp.
- Direction keys: 60×56dp, `rounded.md` (14dp), `surface-key`, 1dp hairline.
- OK: 70dp circle, mint fill, `ink-on-accent` label. Pressed: `accent-pressed`.
- No diagonal keys. No swipe surface in the hero. The pointer lives in its own Pad sheet and is gated separately.

The 4dp gap is structural, not a target gap; each key's touch target includes the gap to at least 48dp. The D-pad is the densest part of the product and should feel like a physical cluster, not a grid of UI buttons.

### Utility and function keys

- The utility row is centered: a three-segment volume rocker (up, mute, down), then play/pause and power. Each segment is 60×48dp; the round keys are 48dp, with 14dp gaps. Total width 304dp; it fits inside the 320dp content width at 360dp screen width.
- Function keys are four equal columns, 52dp high, `rounded.md`, with an 18dp line icon and a 15sp label. They are not pills; the D-pad and utility keys already carry the circular language.
- Power is visually distinct by position and a 1dp hairline, not by a state color or a filled red key. It is power off, not a toggle. Wake lives in the launcher.

### Launcher

- 84dp high, `surface-raised`, a 1dp top hairline, 24dp top corners. It reads as a shelf, not a card.
- Six slots in this order: **Text, Voice, Pad, Cast, Mirror, Wake**.
- Each slot is 52×60dp: a 44dp circular icon well, then an 11sp label. Six across at ≥360dp; 3×2 at <360dp or at 200% font scale.
- Disabled slots keep their position, icon, and label. They use a dashed 1dp hairline, `ink-disabled`, and a small lock mark. A disabled slot is still tappable to open its explanation; the action inside is what is blocked.
- The launcher is always visible on the Remote surface, including when no TV is selected. With no TV, all slots are disabled and the empty state owns the explanation.

### Sheets and insets

- Bottom sheets use 24dp top corners, 20dp side padding, and a 38×4dp grabber. Maximum height is 80% of the viewport; content scrolls inside the sheet, never behind the keyboard.
- One sheet at a time. A sheet can replace another sheet, but never stack a second scrim.
- Keyboard insets are applied to the text sheet: the Send action stays above the IME, and the field scrolls into view. The hero is not visible behind the keyboard; the sheet owns the moment.
- Respect the status bar, cutout, and gesture/navigation insets. The launcher's bottom padding includes the safe area; no key sits under the gesture bar.
- Max content width is 480dp on foldables and large phones. The app is not a tablet layout; it centers the column rather than stretching the D-pad.

### What is not in the layout

No tabs, no drawer, no floating action button, no carousel, no horizontal scrolling, no dashboard cards, no persistent bottom navigation beyond the six-slot launcher. The TV switcher is a sheet, not a second navigation system.

## Elevation & Depth

Depth is tonal, not shadowed.

- **No drop shadows** on keys, cards, chips, or sheets. Hierarchy comes from `surface-base` → `surface-raised` → `surface-key`, plus a 1dp hairline.
- **The ring glow is the only large glow.** The 8dp health dot's 2dp halo is a local state mark; no other glow is allowed.
  - Idle: 2dp stroke, no glow.
  - Submitted: 2dp stroke plus a 900ms breath at 16% opacity, once per press.
  - Observed: 2dp stroke plus a 24dp mint glow at 12%, held for 4s, then fade to idle.
  - Unconfirmed: 2dp amber stroke plus a 12% glow; held until resolved.
  - Offline: 2dp salmon stroke plus a 16% glow; held until reconnect.
- **Sheets** sit above a `scrim` at 64% opacity and a 1dp top hairline. No shadow. The scrim is the depth.
- **Banners** are flat oxblood with a 1dp border. They do not float; they push content.
- **Keys** are flat at rest; pressed keys change tone and scale to 0.98 for 80ms. The scale is feedback, not a 3D bevel. Haptics carry more of the physicality than the pixels do.
- **No glassmorphism, no blur, no gradients.** The one exception is the ring's radial glow and the health dot's 2dp halo, both state signals; the ring's glow-pulse is removed under reduced motion.

## Shapes

- **Keys** use `rounded.md` (14dp). This is the default for D-pad directions, function keys, text fields, and buttons.
- **The OK key** is a 70dp circle. It is the only large filled circle on the Remote surface.
- **Utility keys** are 48dp circles; the volume rocker is a 24dp-radius capsule with three segments.
- **Launcher icons** are 44dp circles inside 52dp slots. The circular well is the touch affordance; the label sits below.
- **Sheets** use `rounded.xl` (24dp) on the top corners only. Cards use `rounded.lg` (18dp). Chips and session chips use `rounded.full` only when they are single-line status objects.
- **Banners** use `rounded.md`. They are not pills.
- **The ring** is a 236dp circle with a 2dp stroke. It is the only large outline in the product; do not outline other containers just to echo it.

### Iconography

A small custom vector set, drawn on a 24dp grid with 1.5dp strokes, rounded caps, and rounded joins. Line icons only; no filled icons except the 8dp health dot and the 6dp state lamp. No Material icons, no emoji, no unicode arrows, no text-as-icon.

Twelve core glyphs: TV, power, speaker, play/pause, home, back, apps grid, search, keyboard, microphone, pointer, cast. Mirror (two overlapping screens) and Wake (sun/power) are the two capability glyphs drawn in the same family. Close, retry, check, chevron, info, and the settings gear are simple utility marks, not product iconography. The set is intentionally small; if a new concept needs a glyph, it must be drawn in the same family and added to this list, not borrowed from a library.

Icons are 18dp in function keys, 20dp in utility keys, 20dp in launcher wells, 24dp in the topbar. The OK key uses the word **OK**, not a glyph. The D-pad uses the custom chevrons, not triangles.

## Components

The component set is deliberately small. Sheets are one component with different content; the remote surface is composed of the topbar, chip, utility row, function keys, ring, D-pad, state line, and launcher.

### Topbar

56dp high, `surface-base`, 1dp hairline bottom, 20dp side padding. On Remote, the left side is the TV switcher and the right side is a 48dp settings target. No separate title on the Remote surface; the TV name is the context. On Discovery/Add TV, Pairing, TV details, and Settings, the left side becomes a back affordance and a static title, and the right side is empty. The topbar never contains a command state; that is the ring's job.

### TV switcher

A text button, not a pill: 8dp health dot, TV name in `body` (16sp/400), 12dp mint caret, 8dp gaps, 48dp minimum height. The dot is the socket health; the caret is the way to change TVs. The switcher truncates a long TV name with an ellipsis but keeps the health dot and caret visible. The full name is in the TV switcher sheet and TV details.

### Health dot

8dp circle with a 2dp halo at 18% opacity. Connected is mint, reconnecting is amber, offline is salmon. It has no text label on the Remote surface; its accessibility label says "Living Room TV, connected," "Living Room TV, reconnecting," or "Living Room TV, off." It never pulses except during reconnect, and that pulse is removed under reduced motion. It never indicates a command outcome.

### One-time chip

A raised strip below the topbar: 48dp minimum, 14dp padding, `rounded.md`, 1dp hairline, 14sp secondary text, 18dp info glyph, 48dp close target. Maximum two lines. One chip at a time; additional explanations live in TV details. The chip names the limit and what still works: "This TV doesn't report Play — it may still work." It is dismissible and does not return for the same TV/capability unless the pairing, firmware, or a probe result changes.

### State ring

236dp, 2dp stroke, centered around the D-pad. No fill; the interior is the remote. The ring is not a button and has no press state. It is an ambient state object with a text twin in the state line. The five colors are the only colors it uses; no gradients, no segmented arcs, no progress sweep. The glow is a soft radial shadow, never a hard neon line. Under reduced motion, the ring changes color instantly and does not breathe or glow-pulse.

### D-pad

The 3×3 cluster described in Layout. Direction keys are `surface-key` with a 1dp hairline and a 15sp chevron; the OK key is mint with `ink-on-accent` and a 15sp/500 label. Pressed direction keys use `surface-key-pressed`; pressed OK uses `accent-pressed`. Disabled keys use `ink-disabled` and a 45% opacity surface; the key remains in place. No key uses a shadow, gradient, or inner bevel.

### Utility row

A centered 48dp row: volume rocker, play/pause, power. The rocker is a capsule with three 60×48dp segments and hairline dividers; the icons are speaker-plus, speaker-mute, speaker-minus. Play/pause and power are 48dp circles with 20dp line icons. Power's glyph is `ink-primary`; the key is never a filled red block, and it never borrows the ring's state colors. The utility row is visually quieter than the D-pad: same surface, smaller footprint, no mint fill.

### Function key

A 52dp-high key with an 18dp line icon and a 15sp label, `rounded.md`, `surface-key`, 1dp hairline. Four across, 8dp gaps. The icon sits above the label; the whole key is the target. Function keys never use mint fill; mint appears only on the focus ring or a transient active state. They are not pills, and they do not use uppercase labels.

### Launcher

84dp shelf, `surface-raised`, top hairline, 24dp top corners. It is the only surface with a top radius on the Remote screen. The six slots are evenly distributed, not scrollable. The launcher is always present; with no TV, it is visibly disabled rather than hidden. The shelf does not float above the content; it is the bottom of the screen.

### Launcher slot

52×60dp, no background of its own. The 44dp icon well is `surface-key` with a 1dp hairline; the label is 11sp `micro`, one or two lines, centered. The icon well is the visual target; the whole 52×60dp slot is tappable. Disabled slots use a dashed hairline, `ink-disabled`, and a small lock mark; the label stays legible. Active slots do not use mint fill; a mint dot or a mint icon is enough to show that a session is live. The slot is not a card and has no shadow.

### Bottom sheet

The single modal container for Text, Voice, Cast, Mirror, Wake, TV switching, and permissions. `surface-raised`, 24dp top corners, 20dp side padding, 12dp top padding, 24dp bottom padding, 38×4dp grabber, 64% scrim, 1dp top hairline. Max height 80%; the content scrolls inside. A sheet has one title (`title`, 24sp), optional body copy (`body`, 16sp), and at most one primary-filled action; secondary actions can be cards, rows, or text buttons. Destructive actions are separated by 24dp and use the destructive button, never a filled red block. Sheets do not stack; opening a second sheet replaces the first. The keyboard never covers the Send action.

### Text field

52dp minimum height, `surface-key`, 1dp hairline, `rounded.md`, 20dp horizontal padding, `body` text, 2dp mint caret, 2dp mint focus ring. A multiline field for the TV's focused native text field or browser URL; it grows to three lines before the sheet scrolls. The field has no label inside; the sheet title and helper text say where the text goes. Error copy appears below the field in `body-small`, not as a red outline. The field never shows a live mirror of the TV; it is a draft until Send.

### Mic key

72dp circle, mint fill, `ink-on-accent`, 28dp microphone line icon, centered in the Voice sheet. While listening, a 2dp `accent` halo at 40% opacity and a static "Listening…" label; while locked, the halo is full-strength and a small lock mark appears next to "Locked." The key is the sheet's primary hold target. It is not a toggle button with a play/stop icon; press-and-hold is the interaction, and the label teaches it.

### Session chip

48dp-high single-line chip, `surface-key`, 1dp hairline, `rounded.full`, 14dp horizontal padding, 8dp gaps, 14sp text. It names the session and the TV: "Casting to Living Room TV" or "Mirroring to Bedroom TV." It sits above the launcher on the Remote surface and at the bottom of other surfaces. It is the way back to a session that is still running. It is not a command state; the ring is not shown inside it.

### Banner

A flat oxblood strip with a 1dp border, `rounded.md`, 16dp padding, 16sp `banner-ink` copy, and one recovery action in `banner-action` plus an optional "Not now" text dismiss. It appears only for a blocking or actionable condition: offline, reconnecting, identity changed, cast stopped, or pairing required. It pushes the hero down; it does not overlay it. Maximum two lines of copy plus one action row. It never contains a command state or a spinner. The recovery action is the only inverted element on the screen.

### State line

A 24dp-minimum line under the ring, centered, 13sp/500 `state`, tabular numerals. It pairs a 6dp lamp with a short label: "Idle", "Sent", "Waiting for TV…", "TV confirmed", "You confirmed", "Offline". The lamp uses the same ring color; the text is the primary signal. In the unconfirmed state the row expands to 48dp to host a 48dp text action, "It worked." It is the live region for screen readers. It never truncates the state word; at 200% it wraps to two lines and the hero shifts.

### Pairing card

A raised card, 20dp padding, `rounded.lg`, 1dp hairline. The TV name is `headline`; model, Tizen version, masked MAC, and "same Wi-Fi as this phone" are `body-small` secondary. A first-time note sits below in a `surface-key` inset with a 3dp mint left rule: "First time connecting — verify it's yours." The primary action is "This is my TV — connect"; the refusal "That's not my TV" is a secondary button, always visible. The card never shows a full MAC or a token.

### TV card

A raised card, 16dp padding, `rounded.lg`, 1dp hairline. Name in `headline`, model/Tizen/status in `body-small`, and one durable capability label: "Full control — confirms keys" or "Best-effort — can't confirm commands." The label is a text plate, not a colored badge. Actions are "Open remote" and "TV details"; "Forget TV" lives in details. The card can show the health dot, but the dot is health, not capability; the capability label is the durable truth.

### Primary, secondary, destructive buttons

- **Primary:** 52dp high, mint fill, `ink-on-accent`, `rounded.md`, 15sp/500. One per sheet or card. One of the three mint fills, with the OK key and the mic key. Never used for destructive actions.
- **Secondary:** 48dp high, transparent, 1dp hairline, `ink-primary`, `rounded.md`. Used for "Not now," "That's not my TV," "Cancel."
- **Destructive:** 48dp high, transparent, 1dp `ring-offline` border, `ring-offline` text. Used for "Stop casting," "Forget TV," "Stop mirroring." Never a filled red block; the outline is enough.

Buttons do not have icons unless the icon is the only label (close, back). They do not have loading spinners; when an action is in flight, the button stays in place and the state line or sheet body explains what is happening.

### Permission explainer

A bottom sheet variant with a 24dp title, 16sp body, and two actions. It explains the permission in one sentence, what the app will do with it, and what happens if the user says no. The system prompt appears only after the user taps Continue. Denied states keep the same sheet with an "Open settings" action and a plain explanation; they do not dead-end.

### Empty state

`display` title ("No TVs yet."), 16sp body, a quiet diagnostic readout in `code` (network, scan result, diagnostics), and one primary action ("Add your first TV"). The launcher is visible and disabled. The empty state is not a marketing page; it states what the app knows and what to do next.

## Do's and Don'ts

| Do | Don't |
|---|---|
| Use mint for the OK key, caret, active states, and `ring-observed` | Fill cards, banners, or whole sheets with mint |
| Let the ring be the only ambient object | Add progress spinners, pulsing skeletons, or decorative glows |
| Build hierarchy with graphite tone steps and 1dp hairlines | Use drop shadows, glassmorphism, blur, or gradients |
| Keep the D-pad in the bottom third, inside the 236dp ring | Center the controls vertically or scatter them across the screen |
| Use the custom 1.5dp line icon set | Use Material icons, emoji, unicode arrows, or text-as-icon |
| Pair every ring state with a text label | Use color alone, or show a green check before the TV confirms |
| Use `ink-primary` on `surface-base` for body copy | Use pure white, pure black, or low-contrast gray for information |
| Use 48dp minimum targets and 56dp direction keys | Shrink keys to fit more chrome, or put actions under the gesture bar |
| Keep the launcher fixed and visible, with disabled slots explained | Hide gated capabilities without a trace, or stack multiple chips |
| Use sentence case, plain verbs, and specific limits | Use "universal," "works with any Samsung TV," "seamless," "magic," or "just" |
| Let the phone keyboard be a field-bound tool | Promise "type anywhere" or live-mirror the TV keyboard |
| Make power off and wake separate, explicit actions | Use a power toggle that can do either, or wake without a matrix pass |
| Use the ring's 900ms breath once per press | Loop the breath, animate every key, or ignore reduced motion |
| Respect the system font scale and reduced-motion setting | Lock text size, truncate state labels, or force animation |
