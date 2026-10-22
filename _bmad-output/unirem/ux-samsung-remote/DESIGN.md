---
name: UniRem
description: Visual identity contract for the {appName} Samsung Tizen TV remote — a direction-constant Tactile Retro skeleton plus six user-switchable color themes.
status: draft
sources:
  - ../spec-samsung-remote/spec-samsung-remote.md
updated: 2026-10-22

# ═══════════════════════════════════════════════════════════════════════
# TIER 1 — DIRECTION-CONSTANT (the Tactile Retro skeleton)
# These NEVER change between themes. They are materials, geometry,
# radii, type, motion, key travel, texture and elevation. Harvested
# from .working/direction-tactile.html, which skipped them because it
# is a color board. They are decided; this spine records them.
# ═══════════════════════════════════════════════════════════════════════
colors:
  # ── direction-constant materials ──
  const-shell: '#1D1510'
  const-shell-2: '#2A1F17'
  const-shell-floor: '#150F0A'
  const-bezel: '#241A12'
  const-bezel-hi: '#3D2E1F'
  const-frame: '#100A06'
  const-groove: '#120C08'
  const-cap-top: '#E9DFC9'
  const-cap-top-floor: '#D9CDB2'
  const-cap-side: '#B3A68C'
  const-cap-ink: '#3A2F22'
  const-cap-top-dark: '#4A3A2C'
  const-cap-top-dark-floor: '#3B2E22'
  const-cap-side-dark: '#33281E'
  const-cap-ink-light: '#E9DFC9'
  const-cap-rim: '#F4ECD8'
  const-cap-rim-dark: '#57442F'
  const-plate-brass: '#C9BDA2'
  const-plate-brass-floor: '#B0A488'
  const-plate-rim: '#8F8367'
  const-plate-ink: '#33281C'
  const-micro-ink: '#7A6A52'
  const-micro-ink-dark: '#A3906F'
  const-etch-ink: '#9C8563'
  const-stamp-ink: '#7F6C50'
  const-plate: '#33271D'          # etched utility shelf: the recessed panel the dark utility caps sit on (captured as --plate in the direction file)
  const-ink-faint: '#6B5C47'      # disabled / etched-out legends (captured as --ink-faint; reaches 2.1:1 on const-shell, so disabled copy is paired with the silhouette, never relied on alone)
  const-leather-top: '#4D3C2C'
  const-leather-mid: '#3A2C1F'
  const-leather-low: '#2B2015'
  const-leather-rim: '#5A4632'
  const-dome-wall: '#8F836A'
  const-power-red: '#B4402F'
  const-hairline: '#453527'

  # ── TIER 2 — THEME TOKEN, default theme = Midnight Ember · DARK (primary) ──
  # Every theme swaps exactly this group and nothing else.
  # Full six-theme matrix lives in the Colors section of the body.
  surface-base: '#000000'
  surface-raised: '#121010'
  surface-key: '#1F1A17'
  text-primary: '#F5EFE9'
  text-secondary: '#A89B8F'
  text-on-accent: '#1A0E00'
  accent-brand: '#FF9E2C'
  accent-brand-pressed: '#E07B10'
  ring-idle: '#463A32'
  ring-submitted: '#FFD37A'
  ring-observed: '#FF8A3D'
  ring-unconfirmed: '#E8B33D'
  ring-offline: '#E03131'
  sem-success: '#58C776'
  sem-warning: '#FFB84D'
  sem-danger: '#E5484D'

typography:
  display:
    fontFamily: 'Roboto'
    fontSize: 34
    fontWeight: 800
    lineHeight: 40
    letterSpacing: -0.01
  title:
    fontFamily: 'Roboto'
    fontSize: 22
    fontWeight: 800
    lineHeight: 28
    letterSpacing: -0.01
  etch:
    fontFamily: 'Roboto'
    fontSize: 20
    fontWeight: 800
    lineHeight: 24
    letterSpacing: 0
  cap:
    fontFamily: 'Roboto'
    fontSize: 17
    fontWeight: 700
    lineHeight: 20
    letterSpacing: 0
  name:
    fontFamily: 'Roboto'
    fontSize: 16
    fontWeight: 800
    lineHeight: 22
    letterSpacing: 0
  ok:
    fontFamily: 'Roboto'
    fontSize: 15
    fontWeight: 800
    lineHeight: 18
    letterSpacing: 0.1
  control:
    fontFamily: 'Roboto'
    fontSize: 14.5
    fontWeight: 700
    lineHeight: 20
    letterSpacing: 0.02
  action:
    fontFamily: 'Roboto'
    fontSize: 13
    fontWeight: 800
    lineHeight: 18
    letterSpacing: 0.04
  kicker:
    fontFamily: 'Roboto'
    fontSize: 12
    fontWeight: 700
    lineHeight: 16
    letterSpacing: 0.2
  honest:
    fontFamily: 'Roboto'
    fontSize: 12
    fontWeight: 700
    lineHeight: 19.2
    letterSpacing: 0
  caption:
    fontFamily: 'Roboto'
    fontSize: 12.5
    fontWeight: 400
    lineHeight: 21.25
    letterSpacing: 0
  meta:
    fontFamily: 'Roboto'
    fontSize: 11.5
    fontWeight: 400
    lineHeight: 17.8
    letterSpacing: 0
  chip:
    fontFamily: 'Roboto'
    fontSize: 11.5
    fontWeight: 700
    lineHeight: 16.7
    letterSpacing: 0.02
  plate:
    fontFamily: 'Roboto'
    fontSize: 10
    fontWeight: 800
    lineHeight: 14
    letterSpacing: 0.1
  micro-wide:
    fontFamily: 'Roboto'
    fontSize: 9.5
    fontWeight: 800
    lineHeight: 13
    letterSpacing: 0.18
  micro:
    fontFamily: 'Roboto'
    fontSize: 8
    fontWeight: 800
    lineHeight: 11
    letterSpacing: 0.12
  statusbar:
    fontFamily: 'Roboto'
    fontSize: 12
    fontWeight: 700
    lineHeight: 16
    letterSpacing: 0.05

rounded:
  micro: 3px
  plate: 6px
  chip: 8px
  key-small: 11px
  key: 12px
  cap: 14px
  panel: 16px
  sheet: 20px
  pad: 22px
  pipe: 28px
  groove: 34px
  frame: 52px
  full: 9999px

spacing:
  '1': 2px
  '2': 4px
  '3': 8px
  '4': 12px
  '5': 16px
  '6': 20px
  '7': 24px
  '8': 32px
  gutter: 20px
  hero-inset: 20px
  topbar-h: 60px
  statusbar-h: 46px
  cap-gap: 12px
  rocker-gap: 8px
  key-row-gap: 10px
  card-gap: 16px
  sheet-pad-x: 12px
  sheet-pad-bottom: 26px
  etch-pitch: 5px
  reference-width: 390px

components:
  key-cap:
    size: '66 × 58'
    radius: '{rounded.cap}'
    wall: '0 4px 0 {colors.const-cap-side}'
    travel: 3px
    touch-target: 48dp
    glyph: '{typography.cap}'
    micro-label: '{typography.micro}'
    variants: [bone, dark, orange]
  key-cap-dark:
    top: '{colors.const-cap-top-dark}'
    top-floor: '{colors.const-cap-top-dark-floor}'
    side: '{colors.const-cap-side-dark}'
    rim: '{colors.const-cap-rim-dark}'
    ink: '{colors.const-cap-ink-light}'
    micro-ink: '{colors.const-micro-ink-dark}'
  ok-dome:
    size: '96 × 96'
    radius: '{rounded.full}'
    wall: '0 5px 0 {colors.const-dome-wall}'
    travel: 4px
    touch-target: 48dp
    gloss: 'radial at 34% 26%, rgba(255,250,235,.5) → transparent 42%'
    glyph: '{typography.ok}'
    micro-label: '{typography.micro}'
  dpad:
    discrete-key: '42 × 36'
    gap: 6px
    continuous-pad: '308 × 308'
    groove-padding: 7px
    pipe-stroke: 3px
    pipe-inset: 10px
  ring:
    states: [idle, submitted, observed, unconfirmed, offline]
    colors: ['{colors.ring-idle}', '{colors.ring-submitted}', '{colors.ring-observed}', '{colors.ring-unconfirmed}', '{colors.ring-offline}']
    seat: '{colors.const-groove}'
    min-contrast: '3:1 against seat'
    min-contrast-secondary: '3:1 against surface-base'
    non-color-signal: 'required'
    submitted-duration: 200ms
    glow-core: 14px
    glow-spread: 34px
  function-row:
    cap: '{components.key-cap}'
    gap: '{spacing.cap-gap}'
    pad-y: '14px 18px'
  volume-rocker:
    size: '56 × 50'
    radius: '{rounded.key}'
    gap: '{spacing.rocker-gap}'
    travel: 3px
    touch-target: 48dp
  tv-switcher:
    height: 44px
    radius: '{rounded.cap}'
    pad: '9px 14px'
    gap: 9px
    wall: '0 3px 0 {colors.const-bezel}'
    caret: '{colors.accent-brand}'
    type: '{typography.control}'
  sheet:
    radius: '{rounded.sheet}'
    pad: '10px 12px 26px'
    lip: '2px {colors.const-hairline}'
    lift: '0 -4px 10px rgba(0,0,0,.45)'
    grabber: '44 × 5'
    max-height: '90%'
  inline-chip:
    radius: '{rounded.chip}'
    pad: '9px 12px'
    plate: '{colors.const-plate-brass}'
    rim: '{colors.const-plate-rim}'
    led: 9px
    type: '{typography.chip}'
  tv-card:
    radius: '{rounded.panel}'
    pad: 16px
    wall: '0 4px 0 {colors.const-groove}'
    led: 11px
    name: '{typography.name}'
    honest: '{typography.honest}'
  offline-banner:
    rule: '4px {colors.ring-offline}'
    surface: '{colors.const-plate-brass}'
    type: '{typography.honest}'
    action: '{typography.action}'
# ═══════════════════════════════════════════════════════════════════════
# THEMES — machine-readable mirror of the matrix in Colors.
# These were previously prose-only, so {theme.*} references in
# EXPERIENCE.md resolved to nothing. A downstream consumer reading
# tokens must be able to enumerate every theme without parsing a table.
# Values are verbatim from .working/color-themes-1.html.
# Ring colors are the ONLY themeable tokens that render inside the
# groove; see the seat rule in Colors.
# ═══════════════════════════════════════════════════════════════════════
themes:
  midnight-ember:      # cozy cinema — true-black OLED warmed by an ember
    default: true
    register: 'cozy cinema'
    dark:  { surface-base: '#000000', surface-raised: '#121010', surface-key: '#1F1A17', text-primary: '#F5EFE9', text-secondary: '#A89B8F', text-on-accent: '#1A0E00', accent-brand: '#FF9E2C', accent-brand-pressed: '#E07B10', ring-idle: '#463A32', ring-submitted: '#FFD37A', ring-observed: '#FF8A3D', ring-unconfirmed: '#E8B33D', ring-offline: '#E03131', sem-success: '#58C776', sem-warning: '#FFB84D', sem-danger: '#E5484D' }
    light: { surface-base: '#FFF8F2', surface-raised: '#FFFFFF', surface-key: '#F3E7DB', text-primary: '#241A12', text-secondary: '#7A6A5C', text-on-accent: '#FFFFFF', accent-brand: '#D9700E', accent-brand-pressed: '#B85C00', ring-idle: '#D8C8B8', ring-submitted: '#F2B95C', ring-observed: '#E86F1F', ring-unconfirmed: '#C98F12', ring-offline: '#CC2F2F', sem-success: '#2FA554', sem-warning: '#D89A21', sem-danger: '#CC2F2F' }
  tizen-blue:          # first-party trust — cool navy, Samsung-native blue
    register: 'first-party trust'
    dark:  { surface-base: '#091120', surface-raised: '#101C31', surface-key: '#1A2B48', text-primary: '#EAF1FA', text-secondary: '#8CA1C0', text-on-accent: '#FFFFFF', accent-brand: '#2F80FF', accent-brand-pressed: '#1B62D6', ring-idle: '#2C3E5C', ring-submitted: '#6FA8FF', ring-observed: '#2DD4A7', ring-unconfirmed: '#E8B33C', ring-offline: '#F05252', sem-success: '#34D399', sem-warning: '#F5B942', sem-danger: '#EF4444' }
    light: { surface-base: '#F3F7FC', surface-raised: '#FFFFFF', surface-key: '#E2EBF6', text-primary: '#10203A', text-secondary: '#5C708C', text-on-accent: '#FFFFFF', accent-brand: '#1B66E0', accent-brand-pressed: '#124FAF', ring-idle: '#B9C9DD', ring-submitted: '#6FA3F0', ring-observed: '#17A97F', ring-unconfirmed: '#B5831A', ring-offline: '#D64545', sem-success: '#17A97F', sem-warning: '#C99322', sem-danger: '#D64545' }
  graphite-mint:       # calm modern utility — neutral graphite, a breath of mint
    register: 'calm modern utility'
    dark:  { surface-base: '#151719', surface-raised: '#1E2124', surface-key: '#2A2E32', text-primary: '#ECEEF0', text-secondary: '#98A1A9', text-on-accent: '#0B1A13', accent-brand: '#3DDC97', accent-brand-pressed: '#22B877', ring-idle: '#343A3E', ring-submitted: '#86E9BE', ring-observed: '#3DDC97', ring-unconfirmed: '#E8D44D', ring-offline: '#FF7A6B', sem-success: '#3DDC97', sem-warning: '#E8C84D', sem-danger: '#FF6B5A' }
    light: { surface-base: '#F6F8F7', surface-raised: '#FFFFFF', surface-key: '#E7ECEE', text-primary: '#1A1E20', text-secondary: '#5E676D', text-on-accent: '#FFFFFF', accent-brand: '#149A63', accent-brand-pressed: '#0D7A4D', ring-idle: '#C4CCCF', ring-submitted: '#6FD3A8', ring-observed: '#149A63', ring-unconfirmed: '#9E8A10', ring-offline: '#E05244', sem-success: '#149A63', sem-warning: '#9E8A10', sem-danger: '#E05244' }
  warm-slate:          # earthy premium — clay, sage, terracotta; low blue light
    register: 'earthy premium'
    dark:  { surface-base: '#1B1816', surface-raised: '#262120', surface-key: '#342D28', text-primary: '#F2EDE7', text-secondary: '#ABA096', text-on-accent: '#FFF4EC', accent-brand: '#C96F4A', accent-brand-pressed: '#A85533', ring-idle: '#403830', ring-submitted: '#E3A87F', ring-observed: '#9CB97F', ring-unconfirmed: '#D9A441', ring-offline: '#C2483C', sem-success: '#9CB97F', sem-warning: '#D9A441', sem-danger: '#C2483C' }
    light: { surface-base: '#FAF6F1', surface-raised: '#FFFDFA', surface-key: '#EFE5DA', text-primary: '#2A221B', text-secondary: '#7C6E60', text-on-accent: '#FFF4EC', accent-brand: '#B25A38', accent-brand-pressed: '#8F4527', ring-idle: '#D3C5B6', ring-submitted: '#D89A6E', ring-observed: '#6E8F52', ring-unconfirmed: '#A87A1E', ring-offline: '#B23A2E', sem-success: '#6E8F52', sem-warning: '#A87A1E', sem-danger: '#B23A2E' }
  high-contrast-mono:  # brutalist legibility — the accessibility theme
    register: 'brutalist legibility'
    accessibility: true
    dark:  { surface-base: '#000000', surface-raised: '#0D0D0D', surface-key: '#1A1A1A', text-primary: '#FFFFFF', text-secondary: '#B5B5B5', text-on-accent: '#000000', accent-brand: '#E8FF2B', accent-brand-pressed: '#C6DB00', ring-idle: '#555555', ring-submitted: '#FFFFFF', ring-observed: '#E8FF2B', ring-unconfirmed: '#FFC400', ring-offline: '#FF3B30', sem-success: '#E8FF2B', sem-warning: '#FFC400', sem-danger: '#FF3B30' }
    light: { surface-base: '#FFFFFF', surface-raised: '#F4F4F4', surface-key: '#E4E4E4', text-primary: '#000000', text-secondary: '#4A4A4A', text-on-accent: '#FFFFFF', accent-brand: '#6E7A00', accent-brand-pressed: '#565F00', ring-idle: '#8A8A8A', ring-submitted: '#111111', ring-observed: '#6E7A00', ring-unconfirmed: '#8A6100', ring-offline: '#C0190F', sem-success: '#6E7A00', sem-warning: '#8A6100', sem-danger: '#C0190F' }
  deep-teal:           # upscale distinctive — teal ink on pale gold
    register: 'upscale distinctive'
    dark:  { surface-base: '#061A1D', surface-raised: '#0C2529', surface-key: '#143238', text-primary: '#E7F2F1', text-secondary: '#8FAEAC', text-on-accent: '#08201F', accent-brand: '#E9C87E', accent-brand-pressed: '#CBA659', ring-idle: '#1E454B', ring-submitted: '#F2DFAE', ring-observed: '#5FD3B4', ring-unconfirmed: '#E3B34C', ring-offline: '#E2564D', sem-success: '#5FD3B4', sem-warning: '#E3B34C', sem-danger: '#E2564D' }
    light: { surface-base: '#F1F7F6', surface-raised: '#FFFFFF', surface-key: '#DEEDEB', text-primary: '#0E2424', text-secondary: '#55706E', text-on-accent: '#FFFFFF', accent-brand: '#8A6A28', accent-brand-pressed: '#6E531D', ring-idle: '#B4C9C7', ring-submitted: '#CBA659', ring-observed: '#2A9D7E', ring-unconfirmed: '#96711A', ring-offline: '#C7443B', sem-success: '#2A9D7E', sem-warning: '#96711A', sem-danger: '#C7443B' }
---

## Brand & Style

**Warm, unhurried, honest-before-cheerful.** A well-made tool from someone who respects your living room — espresso-and-brass warmth, not consumer-tech gloss. It would rather tell you plainly that the TV stopped answering than perform a success it cannot prove. *(Personality derived from the locked Tactile Retro direction; the user has not yet confirmed wording — expect to revise.)*

{appName} is a love letter to the remote you could operate without looking. Bone key caps with real 4mm side walls that sink under your thumb. An etched leatherette pad seated in a routed groove. The honesty ring reimagined as an LED light pipe glowing in that groove. Every control announces its affordance physically — chunky, deliberate, zero abstraction — while staying dark-first espresso plastic for a 9:47 PM living room.

The moral core is **outcome honesty**. Copy and visual state may never claim a remote action succeeded unless the TV was observed to have done it. This is not a tone guideline; it is a rendering constraint, and it is why the outcome ring exists as the loudest object on screen.

Practical consequences of that personality:

- **Restraint.** One accent per theme. The accent marks *selection and commitment*, never decoration.
- **No celebration confetti.** Nothing animates because something worked. The observed ring settles and holds; that is the whole reward.
- **No fake-success green.** Green appears only as `{colors.ring-observed}` — never as a generic "great!" toast, fill, or button tint on an unverified action.
- **Texture over gloss.** Grain and etch are the primary surface language; specular gloss is reserved for the single OK dome and for top-edge catch lights, where a real object would have it.
- **Warmth without nostalgia-kitsch.** Espresso, bone and brass — not wood-grain, not sepia, not rotary-dial nostalgia. The romance is in the tactility, not the period.
- **Generous negative space.** The hero is a single large object with air around it, not a grid of equal-weight buttons.
- **Honest state contrast.** When state is bad, it is legible from across a room. Uncertainty is never rendered with reduced effort.

## Colors

### The two-tier system

This product has exactly two color tiers, and conflating them is the single most expensive mistake available in this codebase.

**Tier 1 — Direction-constant (the Tactile Retro skeleton).** Materials: espresso plastic shell, bone key caps, brass label plates, etched leatherette, routed groove. These carry the `const-` prefix in frontmatter and **never change between themes.** Switching from Midnight Ember to Deep Teal must not soften a side wall, thicken a groove, or warm a cap.

**Tier 2 — Theme token (color only).** Six themes. Each swaps exactly: `surface-base`, `surface-raised`, `surface-key`, `text-primary`, `text-secondary`, `text-on-accent`, `accent-brand`, `accent-brand-pressed`, `ring-*` (five), `sem-success/warning/danger`. Dark is primary in every theme; light is secondary and simplified. A theme **never** swaps materials, geometry, radii, type, texture, or motion.

> **Naming rule that makes the split mechanical, not cultural:** any token prefixed `const-` is Tier 1. Anything unprefixed is Tier 2. A code review can enforce this with a grep. Do not add unprefixed tokens that describe materials, and do not add `const-` tokens that describe color themes.

### The six themes (actual approved values)

Values below are lifted verbatim from `.working/color-themes-1.html`.

| Token | 1 · Midnight Ember | 2 · Tizen Blue | 3 · Graphite Mint | 4 · Warm Slate | 5 · High Contrast Mono | 6 · Deep Teal |
|---|---|---|---|---|---|---|
| *register* | cozy cinema | first-party trust | calm modern utility | earthy premium | brutalist legibility | upscale distinctive |
| **DARK (primary)** | | | | | | |
| `surface-base` | `#000000` | `#091120` | `#151719` | `#1B1816` | `#000000` | `#061A1D` |
| `surface-raised` | `#121010` | `#101C31` | `#1E2124` | `#262120` | `#0D0D0D` | `#0C2529` |
| `surface-key` | `#1F1A17` | `#1A2B48` | `#2A2E32` | `#342D28` | `#1A1A1A` | `#143238` |
| `text-primary` | `#F5EFE9` | `#EAF1FA` | `#ECEEF0` | `#F2EDE7` | `#FFFFFF` | `#E7F2F1` |
| `text-secondary` | `#A89B8F` | `#8CA1C0` | `#98A1A9` | `#ABA096` | `#B5B5B5` | `#8FAEAC` |
| `text-on-accent` | `#1A0E00` | `#FFFFFF` | `#0B1A13` | `#FFF4EC` | `#000000` | `#08201F` |
| `accent-brand` | `#FF9E2C` | `#2F80FF` | `#3DDC97` | `#C96F4A` | `#E8FF2B` | `#E9C87E` |
| `accent-brand-pressed` | `#E07B10` | `#1B62D6` | `#22B877` | `#A85533` | `#C6DB00` | `#CBA659` |
| `ring-idle` | `#463A32` | `#2C3E5C` | `#343A3E` | `#403830` | `#555555` | `#1E454B` |
| `ring-submitted` | `#FFD37A` | `#6FA8FF` | `#86E9BE` | `#E3A87F` | `#FFFFFF` | `#F2DFAE` |
| `ring-observed` | `#FF8A3D` | `#2DD4A7` | `#3DDC97` | `#9CB97F` | `#E8FF2B` | `#5FD3B4` |
| `ring-unconfirmed` | `#E8B33D` | `#E8B33C` | `#E8D44D` | `#D9A441` | `#FFC400` | `#E3B34C` |
| `ring-offline` | `#E03131` | `#F05252` | `#FF7A6B` | `#C2483C` | `#FF3B30` | `#E2564D` |
| `sem-success` | `#58C776` | `#34D399` | `#3DDC97` | `#9CB97F` | `#E8FF2B` | `#5FD3B4` |
| `sem-warning` | `#FFB84D` | `#F5B942` | `#E8C84D` | `#D9A441` | `#FFC400` | `#E3B34C` |
| `sem-danger` | `#E5484D` | `#EF4444` | `#FF6B5A` | `#C2483C` | `#FF3B30` | `#E2564D` |
| **LIGHT (secondary)** | | | | | | |
| `surface-base` | `#FFF8F2` | `#F3F7FC` | `#F6F8F7` | `#FAF6F1` | `#FFFFFF` | `#F1F7F6` |
| `surface-raised` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#FFFDFA` | `#F4F4F4` | `#FFFFFF` |
| `surface-key` | `#F3E7DB` | `#E2EBF6` | `#E7ECEE` | `#EFE5DA` | `#E4E4E4` | `#DEEDEB` |
| `accent-brand` | `#D9700E` | `#1B66E0` | `#149A63` | `#B25A38` | `#6E7A00` | `#8A6A28` |
| `accent-brand-pressed` | `#B85C00` | `#124FAF` | `#0D7A4D` | `#8F4527` | `#565F00` | `#6E531D` |
| `ring-idle` | `#D8C8B8` | `#B9C9DD` | `#C4CCCF` | `#D3C5B6` | `#8A8A8A` | `#B4C9C7` |
| `ring-submitted` | `#F2B95C` | `#6FA3F0` | `#6FD3A8` | `#D89A6E` | `#111111` | `#CBA659` |
| `ring-observed` | `#E86F1F` | `#17A97F` | `#149A63` | `#6E8F52` | `#6E7A00` | `#2A9D7E` |
| `ring-unconfirmed` | `#C98F12` | `#B5831A` | `#9E8A10` | `#A87A1E` | `#8A6100` | `#96711A` |
| `ring-offline` | `#CC2F2F` | `#D64545` | `#E05244` | `#B23A2E` | `#C0190F` | `#C7443B` |
| `text-primary` | `#241A12` | `#10203A` | `#1A1E20` | `#2A221B` | `#000000` | `#0E2424` |
| `text-secondary` | `#7A6A5C` | `#5C708C` | `#5E676D` | `#7C6E60` | `#4A4A4A` | `#55706E` |

**Default first-launch theme: Midnight Ember.** Switcher lives in Settings › Appearance.

### Where each token is allowed to appear

- `{colors.accent-brand}` — selection, commitment, the active TV's caret. **Never** on a key that has not been confirmed by the TV. An accent-filled key means "this is what you chose," not "this worked."
- `{colors.ring-*}` — the outcome ring, the state chips, the TV-card status LEDs, and nothing else. Ring hues are not a general palette.
- `{colors.sem-*}` — inline honest copy and non-hot-path banners. `sem-success` is **not** used on the key hot path; the ring owns observed confirmation.
- `{colors.const-plate-brass}` — the one-time unsupported-key chip and the unsupported plate on a TV card. Brass means *this TV has a limit*. It never means success.
- `{colors.const-cap-*}` — key caps only. Bone caps carry dark ink (`{colors.const-cap-ink}`); dark caps carry bone ink (`{colors.const-cap-ink-light}`).
- One saturated plastic per screen: the orange "Add a TV" cap (`{colors.accent-brand}`) is the only saturated object in the product. It is the only place the accent fills a whole cap.

### The outcome ring — five states, colorblind-safe by rule

The ring is the loudest, most honest object on screen. It sits in a routed groove, 3px stroke, 10px inset from the pad edge.

| State | Meaning | Ring color | Glow | Non-color signal |
|---|---|---|---|---|
| `idle` | no action pending | `{colors.ring-idle}` | none — groove reads as decoration | stroke at rest width, zero emission |
| `submitted` | key sent, awaiting evidence | `{colors.ring-submitted}` | swell over ~200ms, like a camera flash tube | **motion** — a single 200ms swell, then off |
| `observed` | TV confirmed the key | `{colors.ring-observed}` | steady, holds until the next action | **persistence** — it stays lit |
| `unconfirmed` | sent, no answer yet | `{colors.ring-unconfirmed}` | steady | **stroke pattern** — dashed, not solid |
| `offline` | TV unreachable | `{colors.ring-offline}` | steady | **inline banner** with one-tap reason/retry |

**Testable rule (not a sentiment).** Any shipped theme passes review only if all of the following hold. This is a gate, run per theme, per mode:

1. **Seat contrast.** Every one of the five ring colors reaches **≥ 3:1** contrast against the groove fill it renders on. `idle` is exempt — it is *supposed* to recede, and the direction states the groove "reads as decoration."
2. **Surface contrast.** Every one of the five reaches **≥ 3:1** against `{colors.surface-base}` in its own mode. (Applies to `submitted`, `observed`, `unconfirmed`, `offline`.)
3. **Adjacent-pair separation.** For every one of the ten unordered pairs, at least one of the following is true: contrast ratio differs by **≥ 1.5:1**, or ΔL\* differs by **≥ 15**, or a non-color signal differs. **Hue alone never satisfies separation.**
4. **CVD pass.** Simulate protanopia, deuteranopia and tritanopia. No two states may collapse to ΔRGB < 40 **unless** they are separated by their non-color signal. This clause is what makes the rule testable rather than decorative.
5. **Monochrome pass.** Render the five states as greyscale *and* with stroke patterns preserved. Every state must remain distinguishable — by lightness alone, or by stroke pattern, or by both. Because `unconfirmed` is dashed, this clause is satisfied on two independent channels for that state. This is the High Contrast Mono bar, and it is the floor for every other theme.

**Seat is Tier 1.** `{colors.const-groove}` `#120C08` is the ring's seat in every theme and both modes. Contrast for rule 1 is always measured against the groove, never against `{colors.surface-base}`. A theme that renders the ring outside the groove is out of compliance regardless of its numbers.

**High Contrast Mono doubles as the accessibility theme** and carries its own load under rules 3–5: `submitted` `#FFFFFF` (L\* 100) → `observed` `#E8FF2B` (L\* ~93) → `unconfirmed` `#FFC400` (L\* ~79) is a deliberate descending lightness ladder, and `offline` `#FF3B30` sits apart in chroma. It is the reference implementation the other five are measured against.

### Known conflicts and gaps in the color sources

- **[RESOLVED] Bone caps vs. `surface-key` (user-confirmed 2026-10-22).** Every one of the six themes sets `surface-key` to a *dark* value (`#1F1A17`, `#1A2B48`, `#2A2E32`, `#342D28`, `#1A1A1A`, `#143238`). `color-themes-1.html` was authored against flat dark keys, so a literal swap would render the Tactile Retro bone caps as near-black caps on a near-black base — destroying the affordance story the whole direction rests on, and violating the memlog rule that themes "never swap materials." **The split is now decided, not provisional:** cap **color** is Tier 2 (it is color, so a theme may vary it), but cap **material** is Tier 1 and is owned by the `const-cap-*` group — the side wall depth, the etched rim, the leatherette grain, and the 4px travel are direction-constant in every theme. `surface-key` is **reserved for flat and ghost key fills** and never paints a bone cap. `const-cap-*` is present in frontmatter and is populated; a theme that wants to tint a cap adds a `const-cap-top` variant without touching material. The OK key follows the same rule: it is a bone dome with `{colors.const-cap-ink}` ink, never `{colors.accent-brand}`.
- **[GAP] OK key color collision.** `color-themes-1.html` fills the D-pad centre with `{colors.accent-brand}`; the Tactile Retro direction makes OK a bone dome with `{colors.const-cap-ink}` ink. The tactile version wins — OK is a physical object, not an accent. Do not fill OK with the accent.
- **[RESOLVED] Ring contrast is measured against the seat, not `surface-base`.** Re-audited 2026-10-22 against `{colors.const-groove}` `#120C08` — the value the ring actually renders on in every theme and both modes, since the groove is Tier 1 and the espresso shell does not change with theme. **All six themes pass in dark and light, with no theme color re-cut.** The earlier finding of failure across all six light themes was an artifact of measuring against `surface-base`, which is not the seat. `idle` is exempt by rule 1. High Contrast Mono light requires no special seat: its `ring-submitted` `#111111` reads 4.4:1 against the groove. Recorded so the next reviewer does not re-derive the wrong background.
- **[RESOLVED] `observed` ~ `unconfirmed` under red-green CVD.** These two were separated by hue alone and collapse under protanopia and deuteranopia (Midnight Ember `#FF8A3D` vs `#E8B33D` → CR 1.16, ΔL\* 0; Warm Slate `#9CB97F` vs `#D9A441` → CR 1.03, ΔL\* 0). **Decided 2026-10-22 by the user: the non-color channel is a dashed stroke, not a breathing blink.** `observed` renders a solid ring; `unconfirmed` renders a dashed ring of the same weight and color family. The stroke pattern carries the distinction, so it survives greyscale, all three CVD simulations, reduced-motion, and any display that clips color. Blink was the alternative and remains a one-line change if the pairing is later judged too subtle.
- **Remaining pair that leans on color.** `observed` (solid green) vs `offline` (solid red) are both steady, so color is the primary channel — but `offline` is the only state that also fires the inline banner and dims keys to 45%, so the two are never confusable in situ. Accepted, not overlooked.

## Typography

**Stack: Roboto.** The direction file's `-apple-system / Segoe UI / Roboto` stack is a web-preview artifact; this is Jetpack Compose on Android, where Roboto is the system face. Compose resolves it as `FontFamily.SansSerif`. The direction's weight requirement is **700–800** — nothing in the product is set lighter than 400 on a control, and the floor for a body label is 400 only on meta and caption copy.

`[ASSUMPTION]` **Display weight 800 requires bundling Roboto Flex.** Android's system Roboto exposes 300 / 400 / 500 / 700 / 900 — there is no native 800 (ExtraBold). Declaring `FontWeight.W800` against the system family silently snaps to 900 (Black), which is heavier and colder than the direction intends. To hold 800 exactly, Roboto Flex (variable, 100–1000) must ship as a font resource at roughly 1.7 MB. *What would confirm it:* the user's call on bundle-size budget versus the exact 800 weight. The fallback if declined is to cap display weights at 700 (Bold), which is available everywhere at zero cost.

**px → sp.** The source values are CSS px on a 390-unit preview frame, which maps 1:1 to dp. Type sizes are declared here in that same numeric frame and rendered as **sp** so Android's font-scale setting is honored. A full ramp must not overflow a cap at 130% font scale — this is a hard test, not a nicety.

### Scale (values are the captured CSS values)

| Role | Size | Weight | Line-height | Tracking | Where |
|---|---|---|---|---|---|
| `display` | 34 | 800 | 40 | -0.01 | sheet and screen titles of the first rank |
| `title` | 22 | 800 | 28 | -0.01 | section headers ("Your TVs") |
| `etch` | 20 | 800 | 24 | 0 | stamped D-pad direction glyphs |
| `cap` | 17 | 700 | 20 | 0 | key-cap glyph |
| `name` | 16 | 800 | 22 | 0 | TV card name |
| `ok` | 15 | 800 | 18 | 0.1 | OK dome label |
| `control` | 14.5 | 700 | 20 | 0.02 | TV switcher |
| `action` | 13 | 800 | 18 | 0.04 | card action buttons |
| `kicker` | 12 | 700 | 16 | 0.2 | eyebrow / section kicker, uppercase |
| `honest` | 12 | 700 | 19.2 (1.6) | 0 | honest-limit copy |
| `caption` | 12.5 | 400 | 21.25 (1.7) | 0 | explanatory prose |
| `meta` | 11.5 | 400 | 17.8 (1.55) | 0 | device model strings |
| `chip` | 11.5 | 700 | 16.7 (1.45) | 0.02 | inline chip copy |
| `plate` | 10 | 800 | 14 | 0.1 | brass plate label, uppercase |
| `micro-wide` | 9.5 | 800 | 13 | 0.18 | stamped pad instruction, uppercase |
| `micro` | 8 | 800 | 11 | 0.12 | key-cap sub-label, uppercase |
| `statusbar` | 12 | 700 | 16 | 0.05 | system status strip |

Line-heights of 1.6, 1.7, 1.55 and 1.45 are captured from the source CSS (`honest`, `caption`, `meta`, `chip` respectively). `[ASSUMPTION]` The remaining line-heights (40, 28, 24, 20, 22, 18, 16, 14, 13, 11) are **not** in the sources — the CSS declares none for those elements. They are set at ~1.18–1.2 for single-line display roles, which matches the source's effective rendering. *What would confirm it:* a rendering pass against the direction HTML, or the user's sign-off on the ramp.

### Labels on brass plates and etched caps

Two label treatments, and they are not interchangeable.

- **Engraved caps** — the glyph *is* the label. Cap faces carry a large primary glyph at `{typography.cap}` (17/700) plus, where the function needs a word, a stamped micro-label at `{typography.micro}` (8/800, uppercase, tracking 0.12) in `{colors.const-micro-ink}`. The stamped label reads as ink pressed into plastic — it is inset-feeling and low-contrast by design. It is never the only carrier of meaning; the glyph always carries the function too.
- **Brass plates** — a plate is a *statement about a limit*, not a control label. Set at `{typography.plate}` (10/800, uppercase, tracking 0.1) in `{colors.const-plate-ink}` on `{colors.const-plate-brass}`, with a 1px inset highlight at 40% white. The single approved plate string is the unsupported-key / unsupported-TV marker.
- **Stamped instruction** — the pad's own hint ("Swipe = arrows · Tap pad = OK") sits at `{typography.micro-wide}` (9.5/800, tracking 0.18, uppercase) in `{colors.const-stamp-ink}`, 12px above the pad's bottom edge. It is stamped, not printed: no gloss, no color.
- **Eyebrows and section markers** — `{typography.kicker}`, uppercase, tracking 0.2, in the theme's `text-secondary`.

Never set body copy below `{typography.meta}` (11.5). Never set an all-caps paragraph — uppercase is reserved for the four treatments above.

## Layout & Spacing

**Scale: 2 / 4 / 8 / 12 / 16 / 20 / 24 / 32.** Base unit is 4; the 2px step exists solely for hairline physical offsets (side-wall thicknesses, groove padding, 1px rim insets) where a 4px step would read as a gap. Values between scale steps that appear in the source — 6, 9, 10, 14, 18, 26 — are physical refinements of the scale, not new steps.

**Reference frame: 390 wide.** All captured geometry assumes this width. The hero is a single centered object with air around it; it never stretches to fill a wide screen, it scales from the center.

Key fixed dimensions (captured):

| Element | Value |
|---|---|
| Status strip height | 46 |
| Top bar height | 60 |
| Hero horizontal inset | 20 |
| Key-cap | 66 × 58, row gap 12 |
| Continuous pad | 308 × 308, groove padding 7, pipe stroke 3, pipe inset 10 |
| OK dome | 96 × 96 |
| Volume rocker cap | 56 × 50, bank gap 8 |
| Utility shelf inset | 12, gap 10 |
| Sheet | 10 / 12 / 26 padding, grabber 44 × 5 |
| Card stack gap | 16, card padding 16 |
| Card action button height | 46 |
| "Add a TV" cap height | 60, 26 above the stack |

**Rhythm.** The largest gaps land between major surfaces — hero to sheet, card to card. The smallest sit between a cap and its own label. The bottom sheet has generous bottom padding (26) so the caps clear the system gesture inset.

**Sheets are one level deep.** The hero opens a sheet; the sheet does not open another sheet. Anything that would needs a screen, not a sheet. *(Whether a given action opens a sheet or a screen is behavioral and lives in `EXPERIENCE.md`.)*

**Touch targets.** The Android floor is **48dp**. Four captured elements fall below it and must be corrected without changing their visual size:

| Element | Captured | Fix |
|---|---|---|
| Settings gear | 44 × 44 | transparent hit-slop to 48 |
| Launcher sheet key-cap | 46 × 42 | transparent hit-slop to 48 |
| Card action button | 46 high | transparent hit-slop to 48 |
| Discrete D-pad key (theme board) | 42 × 36 | transparent hit-slop to 48 |

In Compose this is `Modifier.minimumInteractiveComponentSize()` plus explicit `pointerInput` hit-slop, never a resize of the cap. **Shrinking the visible cap to fit a hit target is never the fix** — the cap is the affordance.

## Elevation & Depth

Depth in this system is **physical**, not atmospheric. There is no diffuse glow-to-nothing elevation ladder. A thing is raised because it has a side wall, and it is recessed because it is routed. Shadows only ever describe contact.

**The raised formula (everything you can press):**

```
box-shadow: 0 {N}px 0 {side-wall-color},   /* opaque, zero-blur, zero-spread */
            0 {M}px {K}px rgba(0,0,0,{A});
```

The first shadow is a **hard 0-blur offset in an opaque darker material color.** That is the side wall, and it is what makes the object read as thick plastic. It is never a translucent shadow — a translucent shadow cannot read as an edge. The second is the contact shadow: blur 6–16px, opacity 0.5–0.6, and it only ever sits below the wall.

| Object | Wall | Contact shadow |
|---|---|---|
| Key cap (66 × 58) | `0 4px 0 const-cap-side` | `0 7px 12px rgba(0,0,0,.55)` |
| Key cap (dark) | `0 4px 0 const-cap-side-dark` | `0 7px 12px rgba(0,0,0,.55)` |
| OK dome (96) | `0 5px 0 const-dome-wall` | `0 9px 16px rgba(0,0,0,.6)` |
| Volume / transport cap (56 × 50) | `0 4px 0` | `0 6px 10px rgba(0,0,0,.5)` |
| Sheet key-cap (46 × 42) | `0 3px 0` | `0 5px 8px rgba(0,0,0,.5)` |
| TV switcher | `0 3px 0 const-bezel` | `0 5px 8px rgba(0,0,0,.5)` |
| Card button | `0 4px 0` | `0 6px 10px rgba(0,0,0,.5)` |
| "Add a TV" cap | `0 5px 0` (burnt orange) | `0 9px 16px rgba(0,0,0,.55)` |
| TV card slab | `0 4px 0 const-groove` | `0 8px 16px rgba(0,0,0,.5)` |

**The recessed formula (everything routed):**

```
box-shadow: inset 0 2px {K}px rgba(0,0,0,{A}),   /* the channel */
            0 1px 0 rgba(255,235,200,.06);      /* floor highlight */
```

| Object | Recess |
|---|---|
| Ring groove | `inset 0 2px 6px rgba(0,0,0,.85)` |
| Utility shelf (etched control plate) | `inset 0 2px 5px rgba(0,0,0,.5)`, `0 1px 0 rgba(255,235,200,.05)` |
| Pad leatherette bevel | `inset 0 2px 3px rgba(255,230,190,.12)` (top catch) + `inset 0 -4px 8px rgba(0,0,0,.55)` (bottom shade) |
| Sheet lip | `0 -4px 10px rgba(0,0,0,.45)` |
| Brass plate | `inset 0 1px 0 rgba(255,255,255,.4)` |
| Speaker slot | 6 tall, radius 3, fill `#0D0906`, 1px hairline rim |

**Catch lights.** Warm white, never neutral white: the molded top edge uses `inset 0 2px 0 rgba(255,235,200,.07)`; the groove floor uses `.06`; the pad bevel top uses `.12`. A real plastic part catches a warm room light, not a studio softbox.

**The OK dome is the only gloss.** One radial highlight at 34% / 26%, warm white at 50%, falling to transparent by 42%, over a `180deg` bone gradient (`const-cap-top` → `#CFC2A4` → `const-cap-side`). If another object grows a specular highlight, one of them is wrong.

## Shapes

Radii are the strongest signal in the system: **tall things have small radii, flat things have large radii.**

| Token | Value | Applies to |
|---|---|---|
| `micro` | 3 | grabber, speaker slot |
| `plate` | 6 | brass label plates |
| `chip` | 8 | inline unsupported-key chip |
| `key-small` | 11 | sheet launcher caps |
| `key` | 12 | volume / transport caps, card buttons, gear |
| `cap` | 14 | key caps, TV switcher, ring legend swatches |
| `panel` | 16 | utility shelf, TV cards, "Add a TV" |
| `sheet` | 20 | bottom sheet top corners (`20px 20px 0 0` — square where it meets the screen edge) |
| `pad` | 22 | continuous swipe pad |
| `pipe` | 28 | light-pipe inner edge |
| `groove` | 34 | routed groove outer edge |
| `frame` | 52 | device frame (presentation only) |
| `full` | 9999 | OK dome, status LEDs |

Rules:

- **The sheet has two corners, not four.** `20px 20px 0 0`. A sheet that is rounded where it meets the screen edge is a floating card pretending to be a sheet.
- **The OK dome is the only perfect circle among controls.** Status LEDs are 9–11px circles and are indicators, not controls.
- **Nothing is pill-shaped except the volume rocker** (a physical rocker genuinely has radiused ends) and the grabber.
- **The light pipe inset is exactly 10px from the groove inner edge**, and the groove padding is exactly 7px. This is what seats the ring in hardware rather than floating it.

## Components

Behavior — *when* a state fires, what a flow does, which surface wins — lives in **`EXPERIENCE.md`**. This section owns appearance only.

### Key cap (three variants)

Bone, dark, and orange. Bone caps are the default; dark caps are for the utility shelf and secondary actions; the orange cap exists exactly once per screen at most.

- **Bone:** `linear-gradient(180deg, {colors.const-cap-top} 0%, {colors.const-cap-top-floor} 100%)`, 1px `{colors.const-cap-rim}` rim, ink `{colors.const-cap-ink}`, 4px wall in `{colors.const-cap-side}`, contact shadow at 55%.
- **Dark:** `linear-gradient(180deg, {colors.const-cap-top-dark} 0%, {colors.const-cap-top-dark-floor} 100%)`, 1px `{colors.const-cap-rim-dark}` rim, ink `{colors.const-cap-ink-light}`, micro-label `{colors.const-micro-ink-dark}`, 4px wall in `{colors.const-cap-side-dark}`.
- **Orange:** `{colors.accent-brand}` body, rim `{colors.const-cap-rim}`, ink `{colors.const-cap-ink}` at 2a1c10-equivalent, 5px burnt-orange wall. The only saturated plastic in the product.

**Travel: 3px.** On press the cap translates down 3px and its wall collapses from 4px to 1px while the contact shadow halves. The cap does not scale, does not fade, and does not bounce.

| Tier | Travel | Objects |
|---|---|---|
| compact | 2px | settings gear, sheet launcher caps |
| **standard** | **3px** | all key caps, volume and transport caps, card buttons, TV switcher |
| deep | 4px | OK dome, "Add a TV" cap |

This tiering is captured, not invented — the CSS declares 2px, 3px and 4px across different objects. `[ASSUMPTION]` The **tier assignment** (which object belongs to which tier beyond what the CSS literally shows) is an extension. *What would confirm it:* a felt-through pass, or simply following the captured per-object values verbatim, which is the safe default until then.

### D-pad and the ambient ring

Two forms, one skeleton.

**Continuous (big-pad mode).** 308 × 308. A 3 × 3 etched grid of direction glyphs stamped in `{colors.const-etch-ink}` at 75% opacity with a `0 1px 0 rgba(0,0,0,.6)` engraved shadow. The pad surface is leatherette: horizontal grain at 5px pitch, white 2%; cross grain at 5px pitch, black 5%; over a radial dome shading `#4D3C2C → #3A2C1F → #2B2015`, 1px `{colors.const-leather-rim}` bevel rim.

**Discrete (thumb-arc mode).** 3 × 3 grid of caps, 42 × 36 cells at 6px gap. `[ASSUMPTION]` The tactile direction file depicts **only** the continuous big-pad. The discrete D-pad exists in `color-themes-1.html` as a *flat* dark key at 42 × 36. Thumb-arc's discrete caps have **no captured tactile treatment** — they must be re-derived from `{components.key-cap}` at 42 × 36, which also lands under the 48dp touch floor (see Layout & Spacing). *What would confirm it:* a thumb-arc rendering in the tactile material system.

**The ring.** Seated in the routed groove around the D-pad: 3px stroke, 10px inset, radius `{rounded.pipe}` inside a `{rounded.groove}` channel. Glow geometry, captured: core `0 0 14px 4px` at 50% of the state color, spread `0 0 34px 10px` at 18%. `idle` gets `0 0 4px` at 40% — present but dead, so the groove still reads as a machined channel rather than a void.

**Stroke pattern — the second channel.** `observed` and `unconfirmed` are the only two states that differ by hue alone, so they are separated by geometry: `observed` draws a **continuous** stroke, `unconfirmed` draws a **dashed** stroke of identical weight (3px) and dash rhythm. Dash spec: `8px` on, `6px` gap, round caps, phase aligned to the ring's start angle so the pattern is rotationally stable and never appears to crawl. Compose: `PathEffect.dashPathEffect(floatArrayOf(8f, 6f), phase = 0f)` on a `Stroke(width = 3.dp, cap = StrokeCap.Round)`. **Round caps only** — butt caps at this size read as a dotted line and lose the "ring" identity. The dash carries no animation; it is static geometry, so it survives reduced motion and low refresh rates. `submitted`, `offline`, and `idle` are all continuous — `unconfirmed` is the only dashed state in the family, which is what makes it unambiguous.

`[ASSUMPTION]` **Motion parameters.** The sources fix the `submitted` swell at **~200ms** and state "springs, not fades." They do not specify the spring's damping ratio or stiffness. *What would confirm it:* the user's read on feel, or a motion spec from the platform team. Proposed starting point: `spring(dampingRatio = 0.7, stiffness = 700f)` for the cap travel and the ring swell, with no alpha animation on the ring stroke — the ring changes color by swapping the token value, it never cross-fades.

### Secondary key row (function row)

Chips of the standard key cap floating above the hero, 12px gap, 14px top / 18px bottom padding. Uses bone caps by default, dark caps where the row sits against the shell. Each cap carries an optional stamped micro-label.

### Edge volume rocker

A bank of two 56 × 50 dark caps at 8px gap, seated on the etched utility shelf (`{colors.const-plate}`, inset 50% recess). Standard 3px travel. The rocker is always reachable by thumb in thumb-arc mode — its position is the reason that mode exists.

### Thumb-arc mode and big-pad mode

Both modes share the entire skeleton. **The mode switch changes layout and cap construction only — never material, radius, type, texture, motion, or color role.** A key that means Power looks identical in both modes.

`[GAP]` Only big-pad has a captured visual. Thumb-arc is the other locked hero mode (memlog: D-pad in the bottom third, secondary row, edge volume rocker) and has no rendering in either HTML. It is specified here by composing the captured cap, rocker and shelf tokens, but the arrangement itself is unapproved.

### TV switcher

Top bar, right-aligned caret in `{colors.accent-brand}`, 44 tall, radius `{rounded.cap}`, `9px 14px` padding, 3px bezel wall. The caret is the only accent element in the bar. It names the current TV; it never claims that TV is connected — connectivity is a separate LED/signal elsewhere, because a name and a status are different claims.

### Sheets

Bottom sheet, radius `{rounded.sheet}`, padding `10px 12px 26px`, 2px hairline lip, lift `0 -4px 10px rgba(0,0,0,.45)`, 44 × 5 grabber. Contents: text, voice, touchpad, cast, mirror, wake — six launch caps at `{rounded.key-small}` with stamped `{typography.micro}`-scale labels in `text-secondary`.

The sheet's own material is the shelf-of-caps idea: a warm top edge (`#241A11` into `{colors.const-shell}`) reading as the underside of a wooden shelf. The cast, mirror and wake entries are the ones most likely to overpromise — *(what each sheet may claim is behavioral; see `EXPERIENCE.md`)*.

### Inline unsupported chip

The one-time chip that appears when a key is unsupported. Brass plate: `linear-gradient(180deg, {colors.const-plate-brass}, {colors.const-plate-brass-floor})`, 1px `{colors.const-plate-rim}`, radius `{rounded.chip}`, `9px 12px` padding, inset white highlight 40%, drop shadow 45%. 9px amber LED with an 80% glow at `{colors.const-plate}`-family amber. Copy at `{typography.chip}`. Dismiss affordance in a 6b5f47-family screw-ink at 800.

**It is inline and it is brass. It is never a modal and never a toast.** Brass means *this TV has a limit*; it never means success.

### Pairing flow cards

`[ASSUMPTION]` **No pairing visuals were captured in either HTML.** The pairing card anatomy is specified here as an extension of the TVs-management card, which is the only card system the direction actually renders: `{rounded.panel}`, 16px padding, a 4px slab wall in `{colors.const-groove}`, 8px contact shadow, name at `{typography.name}`, meta at `{typography.meta}`, one action per row. A pairing card additionally carries a **progress row, not a spinner-as-decoration** — each step is a row that resolves to a cap. *What would confirm it:* a pairing rendering in the tactile material system, or approval of this derived anatomy.

### TV cards (reference card system)

Slab with a 4px side wall, 16px padding, 11px status LED (`.on` at `{colors.sem-success}` with an 80% glow, `.warn` at `{colors.sem-warning}` with a 70% glow). Name, model meta line, optional brass plate, and honest-limit copy at `{typography.honest}`. Two actions per card, `{rounded.key}`, 46 tall.

### Offline banner

`{rounded.chip}`-to-`{rounded.panel}` surface in `{colors.const-plate-brass}` with a **4px left rule in `{colors.ring-offline}`**. Copy at `{typography.honest}`; the action at `{typography.action}` as a standard 46-tall cap. It is the *only* banner permitted to reference the offline state, and it always carries the reason and the retry in one tap.

`[GAP]` No banner render exists in either source. The 4px left rule is captured (it is the `.facts` rule in the direction file); the brass surface is an extension chosen to match the inline chip, so the two honesty messages read as one family.

### Key states — focus, selected, disabled

`[ASSUMPTION]` **Only partial state coverage is captured.** The sources show a cap at rest, a cap pressed, and two LED variants. There is no captured focus, selected or disabled treatment. Specified below as an extension of the captured rest/press pair; *what would confirm it:* user review of the three treatments.

- **Rest** — full 4px wall, full contact shadow, full gradient.
- **Pressed** — 3px travel, wall collapses to 1px, contact shadow at half opacity. No alpha change.
- **Focus** — for hardware-key remotes and accessibility focus: the OK dome's existing gloss is the focus affordance; elsewhere, a 1px outer rule in `{colors.accent-brand}` at 2px offset, added *outside* the cap silhouette so it never alters the cap's geometry. Focus must never be conveyed by changing the cap's fill.
- **Selected** — the active TV's caps and the accent-filled launch affordance use `{colors.accent-brand}` at rest and `{colors.accent-brand-pressed}` pressed. Selection is a commitment marker. **A selected cap never means a confirmed action.**
- **Disabled** — wall collapses to 1px, contact shadow removed, gradient flattened toward `const-groove`, glyph and micro-label drop to `{colors.const-ink-faint}` at roughly 38% alpha. Disabled caps keep their silhouette so the layout never shifts under the thumb.

### Textures

Materials, all direction-constant:

- **Shell weave** — 45° diagonal, 2px period, white at 1.2%. Over a `160deg` gradient `const-shell-2 → const-shell 46% → const-shell-floor`.
- **Leatherette (pad)** — cross grain, 5px pitch, white 2% horizontal and black 5% vertical, over the radial dome shading.
- **Etching** — glyphs are stamped, never printed: `text-shadow` plus reduced opacity, always darker than the surface they sit on.

`[ASSUMPTION]` **`repeating-linear-gradient` has no direct Compose `Brush` equivalent.** The 2px diagonal weave and the 5px cross grain must be built as a `TileMode.Repeated` `ImageShader` over a 2px/5px raster tile, or as a `drawBehind` line loop. *What would confirm it:* a rendering spike comparing both approaches on-device at the target densities. Density matters — a 2px tile is 2dp at 1.0 but reads as noise at 3.0, so the tile should be authored per density bucket rather than scaled.

## Do's and Don'ts

| Do | Don't |
|---|---|
| Keep the two tiers separate: `const-` tokens are materials and skeleton, unprefixed tokens are theme color | Let a theme redefine a radius, a type size, a travel distance, a shadow, or a texture |
| Ship the ring in the groove, seated, inset 10px from the channel edge | Render the ring free-floating on the surface, or over the pad |
| Make all five ring states pass the 3:1 / ΔL\* / CVD / greyscale gate in **every** theme, in both modes | Treat hue alone as a sufficient differentiator between `observed` and `unconfirmed` |
| Pair every ring color with a non-color signal — emission, motion, persistence, breathing, banner | Let `unconfirmed` and `observed` differ by hue only |
| Reserve `{colors.sem-success}` for non-hot-path surfaces | Put a green fill on a key whose effect the TV has not confirmed |
| Show brass when the TV has a limit; show the ring when the TV has answered | Use brass as a generic warning surface, or the accent as a generic success surface |
| Build elevation as an opaque side wall plus a contact shadow | Use a translucent offset shadow with no side wall — it reads as a blur, not a thickness |
| Give every interactive element a 48dp hit target via hit-slop | Shrink a cap to reach 48dp, or let the layout shift between states |
| Keep the one orange cap to one per screen, for one commitment | Scatter the accent across decorative elements |
| Set brass-plate and etched-cap labels in the captured micro roles, with a glyph always carrying the function too | Set an all-caps paragraph, or make a stamped micro-label the sole carrier of meaning |
| Render `idle` as a dead channel — present, quiet, deliberate | Let `idle` glow, pulse, or draw attention; it means nothing is pending |
| Let `submitted` be a ~200ms transient that goes away | Let `submitted` linger, or reuse its bright color for any persistent state |
| Scale the hero from the center of a 390 reference frame | Stretch the hero edge to edge on wide screens |
| Cross-reference `EXPERIENCE.md` for when a state fires and what a flow does | Restate behavioral rules here — this file owns how it looks, not what it does |