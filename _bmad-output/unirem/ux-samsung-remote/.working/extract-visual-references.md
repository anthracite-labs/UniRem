# Visual reference extract

Extraction of five preserved HTML boards in `_bmad-output/unirem/ux-visual-references/`. These are prior-run artifacts, treated here as reference only. Nothing below is a recommendation; values and strings are quoted or transcribed from the HTML/CSS.

## Board inventory

| File | Title | What it shows | Screen(s) depicted | States shown |
|---|---|---|---|---|
| `direction-noir.html` | Living Room Noir | A self-contained design-doc page with three 390×844 phone mocks in a dark, warm-neutral palette; CSS-drawn bezel, punch-island cutout, status bar, TV switcher, one-time chip, ring-and-D-pad hero, six-slot bottom launcher, and a five-state ring legend below the mocks. | 1) Hero, "thumb-arc" mode (a), D-pad in bottom third. 2) Same hero in offline state with banner and dimmed keys. 3) Cast session screen. Plus a five-state legend panel. | Submitted (soft ivory pulse); Offline (red ring + banner, keys at 45% opacity); Observed (jade ring on the cast play key); all five states in legend (Idle, Submitted, Observed, Unconfirmed, Offline). |
| `direction-native.html` | Samsung Native | Same doc format and 390×844 frame, in navy-blue "Samsung-adjacent" surfaces; side-rail volume/power, pill function keys, circular D-pad; a pairing/TOFU screen; a disabled Pad launcher slot with explanation chips; five-state legend. | 1) Hero, "thumb-arc" mode (a), observed ring, Pad disabled. 2) Pairing flow ("Press Allow on your TV"). 3) Hero variant with amber ring and toast. Plus legend. | Observed (green solid settle); Unconfirmed (amber ring + toast); Pad disabled (dashed/dimmed); pairing/TOFU; all five in legend. |
| `direction-tactile.html` | Tactile Retro | Same doc format, rendered as an espresso-brown molded remote body: bone key caps with hard side walls, etched leatherette swipe pad inside a routed groove, brass label plates, orange "Add a TV" cap; a TV-management screen; LED light-pipe legend. | 1) Hero, "big-pad" mode (b): five floating function caps, 308px routed groove with textured swipe pad and center OK dome, utility shelf, six-slot launcher. 2) TVs management with two TV cards (one supported, one unsupported). Plus LED legend. | Light pipe idle (dim); pressed travel defined in CSS (`:active`, not shown as static state); connected vs best-effort TV cards; all five light-pipe states in legend. |
| `direction-signal.html` | Signal Brutalist | Same doc format, pure black/white industrial layout: 4px white outlines, square keys, oversized uppercase type, no radii on controls; a 330px pad slab whose own outline is the ring; a monochrome status line under the pad; TV-management empty state; five-state legend. | 1) Hero, "big-pad" mode (b): five function boxes, pad slab, status line, utility strip, six-slot launcher. 2) Same hero offline, red outline + banner. 3) TVs empty state. Plus legend. | Idle (white outline); Offline (red outline + banner + last-confirmed timestamp); all five in legend, including Submitted as hard double outline and Unconfirmed as 1Hz blink (neither shown on a full phone). |
| `color-themes-1.html` | Color Theme Variations (6 directions · dark + light) | A token board, not a phone mock. Six theme cards, each showing a Dark · primary panel and a Light · secondary panel side by side, 16 labeled hex chips (light panels: 10 chips plus a mono note), an identical mini-remote preview, and a row of five ring swatches. | Mini-remote only: topbar "📺 Living Room TV ▾" / "● observed", 3×3 D-pad, power and volume keys, offline state chip, five labeled ring rows. No status bar, launcher, back/home, mute, or play/pause in these previews. | Every mini-remote simultaneously shows "● observed" in the topbar, an offline chip ("TV didn't answer — retry?"), and an OK key haloed in `ring.observed`. Ring states shown as five static swatches: idle, submitted, observed, unconfirmed, offline. |

---

## Direction 1 — Living Room Noir

**Personality in 2–3 sentences (as rendered).** A dark-room instrument: true OLED black as the screen, warm graphite and hairline-bordered controls, ivory type at 300–400 weight, and a single champagne accent on the OK key and carets. The page describes the register as "premium, quiet, cinematic," and the phone is staged as the brightest object in the room; the only saturated color inside the product is the state ring. There is very little on screen and most of the lower half is empty black, so the ring and the key cluster read as the two poles of the composition.

**Palette (roles as documented in `:root`).**

| Token | Hex | Role |
|---|---|---|
| page canvas | `#0c0b0a` | warm near-black page background |
| page copy | `#b7ae9e` | doc body text |
| page dim | `#6f675b` | captions / secondary doc copy |
| bezel | `#1c1a17` | phone bezel |
| bezel edge | `#332e26` | bezel rim catch |
| screen | `#000000` | true OLED black |
| surface | `#141109` | raised sheet / panels / launcher |
| surface-2 | `#1d1913` | key caps |
| hairline | `#2a251d` | borders and seams |
| ink | `#ece5d8` | primary on-screen text, warm ivory (not pure white) |
| ink-dim | `#8a8175` | secondary text |
| ink-faint | `#57503f` | disabled glyphs / nowline |
| accent | `#cbb994` | champagne: OK, caret, active states |
| ring idle | `#453e32` | dim warm graphite |
| ring pulse (submitted) | `#f0e4cc` | warm ivory glow |
| ring solid (observed) | `#63b18c` | muted jade |
| ring amber (unconfirmed) | `#cf9a3f` | dimmed amber |
| ring red (offline) | `#e05a52` | red |
| banner bg | `#2a1210` | oxblood wash |
| banner rim | `#4a211d` | dim oxblood border |
| banner text | `#f0cdc6` | pale rose |
| banner secondary action | `#9c7f78` | muted rose-gray |
| OK rim | `#3a3327` | bronze hairline |
| OK hover | `#241f16` | warm lift |

Additional one-off values: bezel outer line `#100e0b`; punch-island rim `#141210`; cast disc radial `#26211a → #14110c → #0a0906`; power hover uses `--ring-red`.

**Typography.** Entire board uses the system sans stack `-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif`; no webfont. Doc: kicker 12px / 400 / uppercase / 0.24em; H1 34px / 300; rationale 16px / 300 / 1.65; facts 13px / 400. In-phone: TV switcher 17px / 300 / 0.03em; nowline 11px / 400 / uppercase / 0.28em; secondary key labels 15px / 300 with 9.5px uppercase sublabels at 0.08em; OK 13px / 400 / 0.14em; cast title 24px / 300; cast kicker 11px / uppercase / 0.3em; banner 13.5px / 300; launcher labels 9.5px / 0.06em; legend heading 13px / 500 / uppercase / 0.22em. Casing is mixed: uppercase only for kickers, nowline, micro-labels, and legend headings; key labels are title case ("Home", "Back", "Apps", "Search"). No numerals are tabular except the cast progress (`font-variant-numeric: tabular-nums`).

**Shape / elevation / motion implication.** Phone: 390×844, 46px radius, 10px bezel ring plus a `#100e0b` outline, large soft drop shadow; screen is flat black. Controls use 14–16px rounded rectangles, 20–24px pills, and circles (48px utility keys, 70px OK, 236px ring). Separation is by 1px `#2a251d` hairlines and one-step surface lifts, not by shadows; the only glow is on the ring, the cast play key, and the cast art. The header states motion as "slow breathing ring pulse (~900ms) on submit, 400ms settle on confirm. Nothing else moves"; CSS is static (`transition:none` on the ring), so motion is annotated, not implemented. Hover is the only coded interaction change (rocker to ivory, power to red, OK to `#241f16`, launcher label to ivory).

**Layout signature and density.** Low density. Structure top-to-bottom: status bar (48px) → topbar with TV switcher and 38px gear → optional one-time chip → hero with a centered nowline at top and a bottom-anchored control stack (`margin-top:auto`) → utility row → 4-key secondary row → ring/D-pad → launcher sheet. The ring sits in the bottom third, not centered vertically; the upper half is mostly empty. Phones sit in a wrapping flex row with 56px gaps. Offline phone uses an 80px top margin instead of the chip, so controls shift down.

**Interaction ideas visible.** Horizontal volume rocker as one pill with three buttons (🔉− / 🔇 / 🔉＋), separate round play/pause (⏯) and power (⏻); four 78×52 secondary keys (⌂ Home, ↩ Back, 🎬 Apps, 🔍 Search); D-pad of four 60×56 rounded nav keys around a 70px circular OK; six launcher slots (⌨ Text, 🎤 Voice, 👆 Pad, 📶 Cast, 🪞 Mirror, ⏻ Wake); cast transport ⏮ / large ⏯ / ⏭ and "Stop casting"; banner "Retry" / "Not now"; chip dismiss "×". The ring is non-interactive. The offline phone dims D-pad, round keys, secondary keys, and rocker to 45% opacity while leaving them in the DOM; no `disabled` attributes.

**States / variants shown.** Submitted (ring 1.5px ivory border with three layered 5–16% ivory glows); Offline (1.5px red border, 8–22% red glows, red banner, dimmed keys, live dot removed from TV switcher); Observed (jade ring on cast play button, 15% jade glow); full legend with Idle, Submitted, Observed, Unconfirmed, Offline. The legend's descriptions: "dim, warm graphite — barely there"; "soft ivory pulse, one breath per keypress"; "solid jade settle — TV confirmed"; "amber — sent, no answer yet"; "red + banner — explains, offers retry."

**What it promises and where it could fail in real use.**
- Promise asserted: the TV stays the brightest thing in the room; one slow breath per press tells the truth; "no spinners, no error codes, no red anywhere else."
- Observable constraints: the ring is a 236px circle with a 1.5px stroke and glows at 5–16% alpha; at arm's length the difference between idle `#453e32` and the black screen is subtle, and submitted/observed/unconfirmed differ mainly by hue and glow. The hero screen itself carries no words for the state; the five written labels exist only in the legend below the mock.
- Micro-labels are 9.5px and the nowline is 11px at `#57503f` on black; hairlines are `#2a251d` on `#141109`. Both are low-contrast by construction.
- The offline banner packs message + "Not now" + "Retry" into one 350px-wide row; "Not now" is a borderless text button at 12.5px.
- Iconography is emoji/Unicode (📺 ⚙ ⏯ ⏻ 🔉 🔇 ⌂ ↩ 🎬 🔍 ⌨ 🎤 👆 📶 🪞), whose rendering varies by Android OEM/font.
- The cast screen's play key is the only element carrying the observed ring; the legend's observed swatch is a plain ring, so the "settle" idea appears in two different shapes on the same board.

---

## Direction 2 — Samsung Native

**Personality in 2–3 sentences (as rendered).** First-party Samsung software: navy-black surfaces, cool blue key rims, a 2px Tizen-blue accent, circular D-pad, pill function keys, and volume/power parked on side rails like the physical remote. Weights are 400–600 and spacing is conventional; the screen looks like a system utility rather than a designed object. The pairing screen switches to a stacked, instructional layout with numbered steps and a TOFU verification card.

**Palette (roles as documented in `:root`).**

| Token | Hex | Role |
|---|---|---|
| page canvas | `#05080e` | blue-black doc background |
| page copy | `#a9b8cc` | doc body |
| page dim | `#66788f` | captions |
| bezel | `#10161f` | graphite-blue phone bezel |
| bezel edge | `#26303e` | bezel rim |
| screen | `#09111d` | "Samsung dark-theme navy black" |
| surface | `#101b2c` | panels / bottom sheet |
| surface-2 | `#182740` | key faces |
| key edge | `#24385a` | key rims |
| hairline | `#1a2942` | dividers |
| ink | `#eaf2fb` | primary text |
| ink-dim | `#8fa8c4` | secondary text |
| ink-faint | `#4d607a` | disabled text/glyphs |
| accent | `#3f9bff` | Tizen/SmartThings-adjacent blue; caret, focus, active |
| accent deep | `#1d6fd0` | pressed blue / step numerals / hover fills |
| ring idle | `#2a3a52` | dim navy graphite |
| ring pulse (submitted) | `#7cc0ff` | light blue flash |
| ring solid (observed) | `#35c483` | confirmation green |
| ring amber (unconfirmed) | `#f2b33d` | amber |
| ring red (offline) | `#ef5b5b` | red |
| toast bg | `#1c2c47` | raised navy |
| OK face | `#132034` | deeper blue well |
| primary button ink | `#04121f` | near-black navy on blue |
| primary hover | `#5aa9ff` | one step lighter blue |

Additional one-offs: camera `#04070c` with rim `#1a2331`; outer frame `#060a10`; caption rule `#16202e`.

**Typography.** System sans stack, no webfont. Doc: kicker 12px / 400 / uppercase / 0.18em; H1 32px / 600 / -0.01em; rationale 15.5px / 400; facts 13px. In-phone: TV switcher 16px / 500; status bar 12px / 500; pill keys 13.5px / 500; D-pad nav 16px / 400; OK 13px / 600 / 0.06em; chip 12.5px / 400; launch labels 10px / 500; pairing H2 22px / 600 / -0.01em; lede 14px / 400; step text 13.5px / 400 with 12.5px secondary; step numerals 13px / 600; TV card name 16px / 600 and meta 12.5px; TOFU heading 13px / 600; primary button 15px / 600; ghost 14px / 500. Casing is sentence case except kickers and legend headings; no uppercase key labels.

**Shape / elevation / motion implication.** Phone 390×844, 42px radius, 10px bezel; punch-hole camera rather than island. Keys are 18px-rounded nav keys, 24px pill keys, 22–26px buttons, 14px launcher icons, circles for 52px rail keys and the 74px OK. Separation comes from filled `#182740` surfaces against `#09111d`, plus 1px `#24385a` rims. Ring is 250px with a 2px border and a single 25% glow. Header says “short system-like snaps — 150ms ease-out key flashes, 300ms ring pulse, no bounce, no flourish”; no transitions/keyframes are actually coded. Hover states are defined: gear and pills take the blue border, rail keys fill `#1d6fd0`, OK fills deep blue and turns text ivory, primary button goes `#5aa9ff`.

**Layout signature and density.** Medium density, modeled on the Samsung remote. Hero structure: status bar (44px) → topbar → chip(s) → 30px spacer → side rails flanking the content (vertical volume rocker left; power + play right) → four pill keys → centered ring/D-pad → spacers → optional blue chip → launcher. The rails sit above the pill row, not beside the pad. The D-pad and ring occupy the visual center rather than the bottom third. Pairing screen: H2, lede, three numbered steps, TV card, TOFU note, then bottom-anchored primary/ghost actions. Phones sit in a wrapping flex row with 52px gaps.

**Interaction ideas visible.** Vertical volume rocker with three 52×46 segments ordered ＋ / 🔇 / −; right rail power (⏻, red glyph) above play/pause (⏯); pill Home / Back / Apps / Search with 15px glyphs; D-pad 62×58 nav keys around a 74px OK; six launcher slots, with 👆 Pad rendered dashed, 60% opacity, `title="Disabled — TV failed the pointer probe"`; pairing actions "This is my TV — connect" (filled blue) and "That's not my TV" (ghost). Amber phone shows volume-up filled `#1d6fd0` and a toast; the TV switcher dot turns amber.

**States / variants shown.** Observed (2px `#35c483` ring + 25% glow, green dot in TV switcher); Unconfirmed (2px amber ring, amber topbar dot, toast "Volume sent — waiting for the TV to confirm.", pressed volume key retained in accent-deep); Pad disabled (dashed `#4d607a` icon, dimmed label, one-time blue chip "Pad stays hidden until your TV reports a pointer. All other keys work as normal."); pairing/TOFU; full five-state legend. Legend text: idle "dim navy — recedes until you look"; submitted "300ms blue pulse per keypress"; observed "solid green settle — TV confirmed"; unconfirmed "amber + plain-language toast"; offline "red + 'TV didn't answer — retry?'".

**What it promises and where it could fail in real use.**
- Promise asserted: "A user who owns the physical remote needs zero learning here"; conventional density and type so nothing competes with the TV.
- Observable constraints: the ring is a 2px stroke on a 250px circle; at 25% glow the observed/unconfirmed difference is largely hue. Green carries two meanings on the same screen: `--ring-solid` is both the observed ring and the "connected" dot in the TV switcher.
- The status bar reads "LTE" on all three phones while the pairing copy requires the TV to be "same Wi-Fi as this phone"; the mock does not show Wi-Fi as the transport.
- Several controls rely on emoji (📺 ⚙ ⏯ ⏻ 🔉 🔇 ⌂ ↩ 🎬 🔍 ⌨ 🎤 👆 📶 🪞); the Pad slot's disabled state is conveyed by dash + dim + `title`, not by a visible "why" on the slot itself (the chip is separate).
- The chip copy is 12.5px in a 358px-wide bar; the pairing card's MAC is masked (`8C:F5:A3:••:••:4D`), so the verification value is partial.
- "Pairing…" is rendered as a dropdown button in the topbar, with an accent caret, even though the screen is a linear flow.
- Multiple one-time disclosures stack (Play chip + Pad chip + toast) in a layout with no scroll model shown.

---

## Direction 3 — Tactile Retro

**Personality in 2–3 sentences (as rendered).** A molded remote body rather than an app: espresso-brown plastic with a fine diagonal texture, bone key caps that cast 4px side walls, engraved uppercase micro-labels, and a leatherette swipe pad recessed in a dark routed groove. The ring becomes a light pipe seated in that groove, and brass label plates carry the one-time notices. The register is playful, physical, and high-affordance; there is no flat surface without a bevel, inset, or shadow.

**Palette (roles as documented in `:root`).**

| Token | Hex | Role |
|---|---|---|
| page canvas | `#171009` | espresso doc background |
| page copy | `#c9b89c` | warm parchment gray |
| page dim | `#8a7a60` | captions |
| bezel | `#241a12` | dark walnut plastic |
| bezel highlight | `#3d2e1f` | molded edge catch |
| shell | `#1d1510` | screen / remote body |
| shell-2 | `#2a1f17` | raised plates |
| plate | `#33271d` | etched control plate |
| groove | `#120c08` | routed channel |
| cap top | `#e9dfc9` | bone ivory key face |
| cap side | `#b3a68c` | shaded bone side wall |
| cap ink | `#3a2f22` | engraved legend |
| cap top dark | `#4a3a2c` | smoked secondary key face |
| cap side dark | `#33281e` | dark key side wall |
| cap ink light | `#e9dfc9` | bone legend on dark caps |
| hairline | `#453527` | seams |
| ink | `#efe6d2` | primary text |
| ink-dim | `#a8967a` | secondary text |
| ink-faint | `#6b5c47` | disabled / etched-out |
| accent | `#e2622a` | retro orange; focus, power-adjacent trim, Add a TV |
| power red | `#b4402f` | power cap legend |
| LED idle | `#6a5b48` | unlit phosphor |
| LED pulse (submitted) | `#f4ead2` | warm ivory swell |
| LED solid (observed) | `#7fbf6a` | green LED |
| LED amber (unconfirmed) | `#e0a340` | amber LED |
| LED red (offline) | `#d4574a` | red LED |
| label plate | `#c9bda2` | oxidized brass |
| label ink | `#33281c` | brass engraving |

Additional one-offs: body floor `#150f0a`; outer frame `#100a06`; cap gradient floor `#d9cdb2`; cap top rim `#f4ecd8`; micro-ink `#7a6a52`; dark cap rim `#57442f`; dark cap floor `#3b2e22`; dark micro-label `#a3906f`; power glyph `#e8b9ae`; pad rim `#5a4632`; pad radial `#4d3c2c → #3a2c1f → #2b2015`; etched arrows `#9c8563`; pad instruction `#7f6c50`; OK side wall `#8f836a`; card floor `#221910`; card side wall `#120c07`; orange cap top `#ef7a42`, floor `#c2531f`, rim `#f49a6a`, side wall `#8c3a15`, ink `#2a1c10`.

**Typography.** System sans stack only; no display face. Doc: kicker 12px / 700 / uppercase / 0.2em; H1 34px / 800 / -0.01em; rationale 15.5px / 400; facts 13px with 700 emphasis. In-phone: status bar 12px / 700 / 0.05em; TV switcher 14.5px / 700; function caps 17px / 700 with 8px / 800 uppercase sublabels at 0.12em; OK 15px / 800 / 0.1em plus 8px "TAP"; pad instruction 9.5px / 800 / uppercase / 0.18em; utility caps 15–16px / 700; TV card name 16px / 800; card meta 11.5px / 400; honest note 12px / 400; plate labels 10px / 800 / uppercase / 0.1em; action buttons 13px / 800; "Add a TV" 15px / 800. Casing is heavily uppercase on micro-labels and instructions, sentence case in prose. The "engraved" effect is text color plus a 1px dark shadow, not a font.

**Shape / elevation / motion implication.** Phone 390×844, 52px radius, warm plastic gradient plus a 2px diagonal weave at 1.2% white, inset top catch, 10px bezel and `#100a06` outline. Caps: 14px radius, 4px hard bottom shadow as the side wall, 7px contact shadow; dark caps same geometry. Groove: 308px rounded square, 34px radius, 7px padding, inset 85% shadow; light pipe 3px border; pad 22px radius. Cards are slabs with a 4px `#120c07` bottom edge. The header says 4mm side walls and 2–3px sink, "springs, not fades"; CSS implements 4px shadows and `translateY(2–4px)` on `:active` (gear 2px, caps/utility 3px, OK dome 4px, Add a TV 4px). Light pipe described as swelling ~200ms then holding; no animation is coded.

**Layout signature and density.** High density. Hero: status bar (46px) → topbar with molded TV switcher and gear → brass chip → five function caps in a floating row (Home, Back, Apps, Search, Power) → 308px groove/pad → utility shelf (volume −/mute/+ plus play and power) → six-slot launcher of small dark caps. The pad dominates the middle; every control has a visible top face and side wall. TV management: H2 "Your TVs", subline, two slab cards with LED dots and plate labels, one full-width orange "Add a TV" cap. The launcher is a wooden shelf with a grabber.

**Interaction ideas visible.** Large textured swipe surface: etched ▲◀▶▼ at the edges, 96px glossy OK dome labeled "OK" + "TAP", instruction "Swipe = arrows · Tap pad = OK". Five function caps (⌂ Home, ↩ Back, 🎬 Apps, 🔍 Search, ⏻ Power). Utility shelf: vol −, mute, vol + (dark caps); play/pause and power (bone caps), so power appears twice on the hero (function row and shelf). TV cards: "Open remote" / "Forget TV"; "＋ Add a TV" orange cap. Every cap has an `:active` travel state; there are no hover states. The brass chip dismisses with "×".

**States / variants shown.** Hero light pipe idle (3px `#6a5b48`, 40% glow); CSS also defines `.lightpipe.pulse` ivory glow, not used on the hero. TV management: connected green LED `#7fbf6a` with "Full control · confirms keys"; unsupported amber LED `#e0a340` with "Unsupported · Best-effort" and the note "We can try power, volume, and channel keys. This TV can't tell us whether they landed, so the ring will stay amber here — that's the TV's limit, not a bug." Legend: Idle "dim unlit phosphor — groove reads as decoration"; Submitted "ivory swell ~200ms, like a camera flash tube"; Observed "green LED holds — TV confirmed the key"; Unconfirmed "amber LED — sent, no answer yet"; Offline "red LED + banner: 'TV didn't answer — retry?'".

**What it promises and where it could fail in real use.**
- Promise asserted: controls you could operate without looking; "every control announces its affordance physically"; chunky and zero-abstraction.
- Observable constraints: the swipe pad has no visible zone boundaries or gesture feedback beyond etched arrows and a static instruction line; there is no rendered transition between swipe and D-pad. The board does not show a focus or drag state.
- 8px uppercase cap sublabels, 9.5px pad instruction, and 10px plate labels are small; the light-pipe idle border `#6a5b48` on `#120c08` is low-contrast. The prose says 4mm side walls while CSS renders 4px shadows.
- Power is duplicated (function row and utility shelf); play/pause exists only in the shelf. There is no back/home placement consistent with the other boards' bottom rows.
- Three different brass plates (Play chip, "Full control", "Unsupported") use the same high-contrast material, so the one-time notice and the persistent support tier look alike.
- The add-TV button is the only orange plastic; the power glyph is brick red with a 70% halo. All other color is material shading.
- The 308px groove inside the hero's 20px side padding leaves 21px side margins; the 390×844 frame is fully packed with five caps + pad + shelf + launcher.

---

## Direction 4 — Signal Brutalist

**Personality in 2–3 sentences (as rendered).** An industrial control panel: pure black, 4px white outlines, no corner radius on any key, 800–900 uppercase type, and only three signal hues (amber, red, green) in the entire product. The pad is one 330px slab whose own outline is the ring, and every ring state is also written in a mono status line. The layout is a low count of very large targets, intended to be operable by peripheral vision while the eyes stay on the TV.

**Palette (roles as documented in `:root`).**

| Token | Hex | Role |
|---|---|---|
| page canvas | `#000000` | pure black |
| page copy | `#9a9a9a` | doc body |
| page dim | `#5c5c5c` | captions |
| bezel | `#000000` | black; frame drawn as white outline |
| frame | `#ffffff` | phone frame and all key outlines |
| screen | `#000000` | pure black OLED |
| surface | `#0a0a0a` | recessed wells: banner bg, pad floor |
| ink | `#ffffff` | primary text/glyphs |
| ink-dim | `#9a9a9a` | secondary text |
| ink-faint | `#4d4d4d` | disabled text/outlines |
| signal amber | `#ffb400` | unconfirmed; only warm color |
| signal red | `#ff2a1f` | offline; only alert color |
| signal green | `#00e070` | observed/settle; allowed only on the ring |

Additional one-off: outer case line `#2e2e2e`; banner retry uses black `#000000` ink on red. No brand/accent token beyond these signals.

**Typography.** System sans for UI, system mono (`ui-monospace, "Cascadia Mono", "Segoe UI Mono", Consolas, monospace`) for status/readout text and the swipe rule. Doc: kicker 12px / 700 / uppercase / 0.3em; H1 40px / 900 / uppercase / -0.02em; rationale 15.5px / 400; facts 13px. In-phone: status bar 13px / 700 / 0.08em; TV switcher 17px / 900 / uppercase; gear 22px; chip 12.5px / 700 / uppercase / 0.04em with a 10px / 800 "Note" tag; function labels 9px / 800 / uppercase / 0.14em; D-pad arrows 34px / 900; OK 30px / 900 / 0.06em with 10px "Tap pad"; rule 11px / 800 / 0.2em / uppercase; stateline 12px / 800 / 0.16em / uppercase mono; banner message 17px / 900 / uppercase, sub 12.5px / 600; buttons 14–15px / 900 / uppercase; empty-state heading 52px / 900 / uppercase / 1.02; empty-state lede 14px / 500; scan rows 12.5px / 700 / uppercase; big button 21px / 900 / uppercase; small note 11.5px / 600 / uppercase. Casing is uppercase nearly everywhere; only body prose is sentence case.

**Shape / elevation / motion implication.** Phone 390×844 with a 28px outer radius, but all controls are square: 4px white outlines, zero border radius on function keys, pad, banner, and launcher items. The phone frame is a 4px white border with `#2e2e2e` outer case line and a hard shadow; no soft elevation inside the screen. Ring is not a separate circle — it is the pad's 6px outline with 10px padding; offline simply changes that outline to red, and the inner pad border also turns red inline. The header says motion is none; "the only 'animation' permitted is a 1Hz hard blink on the unconfirmed state". No keyframes or transitions are coded; no hover or `:active` styles are defined either, so all controls are visually static in the file.

**Layout signature and density.** Low count of huge targets. Hero: status bar (44px) → topbar (4px bottom rule; TV switcher flexes, 58px-wide gear) → chip → function row of five equal 66px boxes → pad box (330px pad inside 6px ring + 10px padding) → mono stateline with a 14px square lamp → utility strip (segmented volume box + play box + red power box) → launcher of six 64px boxes. Offline replaces the chip with a 4px red-bordered banner (message, subline, two 52px actions), then pad → stateline → utility → launcher. Empty state uses 52px "No TVs yet." plus a 4px-bordered scan readout and an 88px inverted button; launcher items are dimmed to `#4d4d4d`.

**Interaction ideas visible.** 330px swipe slab: four arrow buttons in a 3×3 grid with faint 3px dividers, center OK block labeled "OK" + "●" + "Tap pad", rule "Swipe = arrows · Tap = OK". Five 66px function boxes: ⌂ Home, ↩ Back, 🎬 Apps, 🔍 Search, ⏻ Power (red glyph). Utility: segmented volume 🔉− / 🔇 / 🔉＋, play/pause ⏯, power ⏻ red. Banner: "Not now" (white outline) and "Retry" (red fill, black ink, the only inverted element on the offline screen). Empty state: "＋ Add your first TV" inverted white. Launcher: six slots (Text, Voice, Pad, Cast, Mirror, Wake), all dimmed in empty state, Pad dimmed in offline state. No pressed/focus styling in CSS.

**States / variants shown.** Idle (6px white pad outline; stateline "Ring: idle — last key confirmed 21:46"); Offline (6px red outline, red inner pad border, banner "TV didn't answer — retry?", subline "Last confirmed key: 21:46. Volume and power are blind until the TV answers.", stateline "Ring: red — offline since 21:49", pad rule "Offline — keys still send, nothing confirms"); Empty TVs (no cards, scan readout "Network Home-WiFi-5G / Scan 0 TVs found / Pointer probe Not run"); legend all five. Legend descriptions: idle "gray outline, recedes"; submitted "hard double outline, one 200ms cut"; observed "green — TV confirmed"; unconfirmed "amber, 1Hz hard blink"; offline "red + banner + retry".

**What it promises and where it could fail in real use.**
- Promise asserted: targets large enough for peripheral vision, "a color should never be the only messenger", no decoration or motion.
- Observable constraints: the 4px pure-white outlines and 52px white heading are maximum-luminance elements in a dark room; the board itself calls the context a dark living room. The white frame/outlines are the dominant glare source, not the TV.
- The 330px pad + 6px ring + 10px padding + 66px function row + 44px status + 60px topbar + chip + stateline + 60px utility + 64px launcher roughly fills the 844px frame; with the offline banner substituted for the chip, the stack is tighter. No scroll or overflow behavior is shown.
- The "ring" is the pad's border, so the red offline state also outlines the entire pad; the state is duplicated in the mono line and pad rule, but the border is the only color cue.
- The unconfirmed state is described as a 1Hz hard blink but is not depicted on a phone; the legend swatch is static.
- The stateline is 12px uppercase mono at `#9a9a9a` on black; the empty-state small note is 11.5px at `#4d4d4d` (low contrast). The chip's amber "Note" tag is the only chip color.
- Power is duplicated (function row and utility strip). The D-pad is a swipe surface, not five separate targets; the four arrow buttons are still rendered as buttons, so tap-vs-swipe behavior is not resolved by the markup.
- The empty state's launcher is dimmed but still present; there is no disabled attribute or explanation on the launcher itself.

---

## Color board — six themes

Board mechanics: six `.variation` cards on `#0E0F12` page, each with a `Dark · primary` panel and `Light · secondary` panel. Every dark panel shows 16 token chips; every light panel shows 10 chips (surfaces, accents, ring family) and puts `text.*` plus semantics in a two-line mono `.lnote`. Each panel repeats an identical mini-remote: topbar `📺 Living Room TV ▾` with `● observed`, 3×3 D-pad (`▲ ◀ ● ▶ ▼`), a power key `⏻`, a wide volume key `🔉`, an offline chip `TV didn't answer — retry?`, and five ring swatches labeled `idle`, `submitted`, `observed`, `unconfirmed`, `offline`. The OK key is filled `accent.brand` with a 2px `ring.observed` halo and a 14px observed blur; power is filled `accent.brandPressed`; volume is filled `accent.brand`.

The light panels are explicitly "secondary, simplified" — fewer chips, same mini-remote, same observed topbar and offline chip. The board's header says: "Dark (primary) and light (secondary, simplified) shown side by side. Every panel carries the full semantic token set as labeled hex chips plus a mini-remote snippet in real strings."

### 1 · Midnight Ember

Register: *"Cozy cinema — true-black OLED warmed by a glowing ember; the living room at 11pm."*

| Role | Dark | Light |
|---|---|---|
| surface.base | `#000000` | `#FFF8F2` |
| surface.raised | `#121010` | `#FFFFFF` |
| surface.key | `#1F1A17` | `#F3E7DB` |
| text.primary | `#F5EFE9` | `#241A12` |
| text.secondary | `#A89B8F` | `#7A6A5C` |
| text.onAccent | `#1A0E00` | `#FFFFFF` |
| accent.brand | `#FF9E2C` | `#D9700E` |
| accent.brandPressed | `#E07B10` | `#B85C00` |
| ring.idle | `#463A32` | `#D8C8B8` |
| ring.submitted | `#FFD37A` | `#F2B95C` |
| ring.observed | `#FF8A3D` | `#E86F1F` |
| ring.unconfirmed | `#E8B33D` | `#C98F12` |
| ring.offline | `#E03131` | `#CC2F2F` |
| semantic.success | `#58C776` | `#2FA554` |
| semantic.warning | `#FFB84D` | `#D89A21` |
| semantic.danger | `#E5484D` | `#CC2F2F` |

Ring reading: idle is a dim warm brown; submitted is pale warm yellow; observed is orange (brand-family, not green); unconfirmed is a duller amber close to observed; offline is red. The five swatches are hue-only; observed and unconfirmed sit under 20° apart in the dark column.

### 2 · Tizen Blue

Register: *"First-party trust — cool navy and Samsung-native blue; confident and familiar."*

| Role | Dark | Light |
|---|---|---|
| surface.base | `#091120` | `#F3F7FC` |
| surface.raised | `#101C31` | `#FFFFFF` |
| surface.key | `#1A2B48` | `#E2EBF6` |
| text.primary | `#EAF1FA` | `#10203A` |
| text.secondary | `#8CA1C0` | `#5C708C` |
| text.onAccent | `#FFFFFF` | `#FFFFFF` |
| accent.brand | `#2F80FF` | `#1B66E0` |
| accent.brandPressed | `#1B62D6` | `#124FAF` |
| ring.idle | `#2C3E5C` | `#B9C9DD` |
| ring.submitted | `#6FA8FF` | `#6FA3F0` |
| ring.observed | `#2DD4A7` | `#17A97F` |
| ring.unconfirmed | `#E8B33C` | `#B5831A` |
| ring.offline | `#F05252` | `#D64545` |
| semantic.success | `#34D399` | `#17A97F` |
| semantic.warning | `#F5B942` | `#C99322` |
| semantic.danger | `#EF4444` | `#D64545` |

Ring reading: submitted is a lighter blue than brand; observed is mint/teal; unconfirmed amber; offline coral red. Light `ring.submitted` (`#6FA3F0`) and light `ring.idle` (`#B9C9DD`) are both desaturated blues; the state difference is mostly lightness.

### 3 · Graphite Mint

Register: *"Calm modern utility — quiet neutral graphite with a fresh breath of mint."*

| Role | Dark | Light |
|---|---|---|
| surface.base | `#151719` | `#F6F8F7` |
| surface.raised | `#1E2124` | `#FFFFFF` |
| surface.key | `#2A2E32` | `#E7ECEE` |
| text.primary | `#ECEEF0` | `#1A1E20` |
| text.secondary | `#98A1A9` | `#5E676D` |
| text.onAccent | `#0B1A13` | `#FFFFFF` |
| accent.brand | `#3DDC97` | `#149A63` |
| accent.brandPressed | `#22B877` | `#0D7A4D` |
| ring.idle | `#343A3E` | `#C4CCCF` |
| ring.submitted | `#86E9BE` | `#6FD3A8` |
| ring.observed | `#3DDC97` | `#149A63` |
| ring.unconfirmed | `#E8D44D` | `#9E8A10` |
| ring.offline | `#FF7A6B` | `#E05244` |
| semantic.success | `#3DDC97` | `#149A63` |
| semantic.warning | `#E8C84D` | `#9E8A10` |
| semantic.danger | `#FF6B5A` | `#E05244` |

Ring reading: observed is identical to `accent.brand` in both modes; submitted is a paler mint; unconfirmed is yellow-green (`#E8D44D`), which is closer to the green family than in any other theme; offline is salmon. The ring family here is effectively "mint / pale mint / mint / yellow / salmon."

### 4 · Warm Slate

Register: *"Earthy premium — clay, sage and terracotta; low blue light for night viewing."*

| Role | Dark | Light |
|---|---|---|
| surface.base | `#1B1816` | `#FAF6F1` |
| surface.raised | `#262120` | `#FFFDFA` |
| surface.key | `#342D28` | `#EFE5DA` |
| text.primary | `#F2EDE7` | `#2A221B` |
| text.secondary | `#ABA096` | `#7C6E60` |
| text.onAccent | `#FFF4EC` | `#FFF4EC` |
| accent.brand | `#C96F4A` | `#B25A38` |
| accent.brandPressed | `#A85533` | `#8F4527` |
| ring.idle | `#403830` | `#D3C5B6` |
| ring.submitted | `#E3A87F` | `#D89A6E` |
| ring.observed | `#9CB97F` | `#6E8F52` |
| ring.unconfirmed | `#D9A441` | `#A87A1E` |
| ring.offline | `#C2483C` | `#B23A2E` |
| semantic.success | `#9CB97F` | `#6E8F52` |
| semantic.warning | `#D9A441` | `#A87A1E` |
| semantic.danger | `#C2483C` | `#B23A2E` |

Ring reading: submitted is a terracotta tint close to the brand; observed is sage green; unconfirmed and observed are both muted and warm, so the hue separation is smaller than in the blue/mint themes. Offline is brick.

### 5 · High Contrast Mono

Register: *"Brutalist legibility — pure black & white with a single volt of color; ring states via lightness + hue shifts."*

| Role | Dark | Light |
|---|---|---|
| surface.base | `#000000` | `#FFFFFF` |
| surface.raised | `#0D0D0D` | `#F4F4F4` |
| surface.key | `#1A1A1A` | `#E4E4E4` |
| text.primary | `#FFFFFF` | `#000000` |
| text.secondary | `#B5B5B5` | `#4A4A4A` |
| text.onAccent | `#000000` | `#FFFFFF` |
| accent.brand | `#E8FF2B` | `#6E7A00` |
| accent.brandPressed | `#C6DB00` | `#565F00` |
| ring.idle | `#555555` | `#8A8A8A` |
| ring.submitted | `#FFFFFF` | `#111111` |
| ring.observed | `#E8FF2B` | `#6E7A00` |
| ring.unconfirmed | `#FFC400` | `#8A6100` |
| ring.offline | `#FF3B30` | `#C0190F` |
| semantic.success | `#E8FF2B` | `#6E7A00` |
| semantic.warning | `#FFC400` | `#8A6100` |
| semantic.danger | `#FF3B30` | `#C0190F` |

Ring reading: the only theme where `ring.submitted` is pure white in dark and near-black (`#111111`) in light, so "submitted" reads as an inversion/lightness cut, not a glow. `ring.observed` equals the volt accent and `semantic.success`; unconfirmed is pure amber; offline is pure red. This is the only theme whose light `ring.idle` (`#8A8A8A`) is lighter than `ring.submitted` (`#111111`).

### 6 · Deep Teal

Register: *"Upscale distinctive — teal ink written in pale gold; unlike any first-party remote app."*

| Role | Dark | Light |
|---|---|---|
| surface.base | `#061A1D` | `#F1F7F6` |
| surface.raised | `#0C2529` | `#FFFFFF` |
| surface.key | `#143238` | `#DEEDEB` |
| text.primary | `#E7F2F1` | `#0E2424` |
| text.secondary | `#8FAEAC` | `#55706E` |
| text.onAccent | `#08201F` | `#FFFFFF` |
| accent.brand | `#E9C87E` | `#8A6A28` |
| accent.brandPressed | `#CBA659` | `#6E531D` |
| ring.idle | `#1E454B` | `#B4C9C7` |
| ring.submitted | `#F2DFAE` | `#CBA659` |
| ring.observed | `#5FD3B4` | `#2A9D7E` |
| ring.unconfirmed | `#E3B34C` | `#96711A` |
| ring.offline | `#E2564D` | `#C7443B` |
| semantic.success | `#5FD3B4` | `#2A9D7E` |
| semantic.warning | `#E3B34C` | `#96711A` |
| semantic.danger | `#E2564D` | `#C7443B` |

Ring reading: submitted is pale gold, observed is mint-teal, unconfirmed amber-gold, offline salmon. In light mode submitted (`#CBA659`, gold) and unconfirmed (`#96711A`, dark gold) are the same hue family, separated by lightness.

### Ring-state concept as rendered on the color board

- Every theme uses the same five names in the same order: `idle → submitted (soft pulse) → observed (settled) · unconfirmed (TV state unknown) · offline (TV unreachable)`. The header calls this the "signature honesty element."
- Each state is a 13×13px circle with a 2.5px border and transparent fill; there is no shape change, no size change, no pulse, no glow, and no blink in the swatches. States differ only by border hue (and, where the theme makes it so, lightness).
- In dark themes, `submitted` is typically a lighter/brighter version of the brand or a warm white; `observed` is green/teal/mint in T2, T3, T4, T6 but orange in T1 and volt in T5; `unconfirmed` is amber/yellow; `offline` is red. In light themes the same roles are darkened for contrast, and `submitted` sometimes becomes darker than `idle` (notably T5).
- The only place the ring is shown at app scale is the OK key: a 2px `ring.observed` halo plus a 14px observed blur around the brand-filled OK. The mini-remote therefore always depicts an observed command while the text chip below depicts an offline one.
- The topbar of every mini-remote reads `● observed` in `ring.observed`; the state chip reads `TV didn't answer — retry?` in `ring.offline`. The two states coexist on the same preview, and the OK halo is observed. This is an internal inconsistency in the board, not a state sequence.
- No motion is coded in the color board; "soft pulse" exists only as the name of `ring.submitted` and in the header sentence.

### Controls and copy on the color board

- Depicted: four D-pad arrows (`▲ ◀ ▶ ▼`), center OK rendered as a filled dot (`●`, no "OK" text), power (`⏻`), volume (`🔉`). No back, home, mute, play/pause, or launcher in the mini-remote.
- Labels/copy: `Dark · primary`, `Light · secondary`, `📺 Living Room TV ▾`, `● observed`, `TV didn't answer — retry?`, `idle`, `submitted`, `observed`, `unconfirmed`, `offline`; each theme's name and one-line register; all semantic token names and hex codes. The board title is `UniRem · Color Theme Variations`; the intro says "Six palette directions for a dark-mode-first Android remote for Samsung Tizen TVs."

---

## Cross-board observations

### What all four directions share (assumptions baked in)

1. **Form factor and frame.** All four are portrait Android phone mocks at 390×844, drawn in pure HTML/CSS with a bezel, status bar, and no scrolling. The color board uses the same product assumption but only a mini-remote preview.
2. **Dark-first, TV-adjacent use.** Every direction is dark; the color board labels dark "primary" and light "secondary, simplified." The shared rationale is a dark living room, one-handed use, eyes on the TV.
3. **Same top-level anatomy.** Status bar → TV switcher ("Living Room TV") + gear → one-time capability chip → control hero → six-slot bottom launcher. All four have the same six launcher labels: Text, Voice, Pad, Cast, Mirror, Wake.
4. **Same core key set.** Four-direction D-pad with a center OK; Home, Back, Apps, Search; volume down/mute/up; play/pause; power. Noir, Native, and Tactile also carry the sample TV identity "Living Room TV" + "Samsung UE55NU8000" (Native adds "Tizen 5.0"; Noir says "Tizen"; Tactile says "Tizen 5.0"); Signal uses "Living Room TV" but shows no model string.
5. **The ring as a five-state honesty element.** All four use the same five state names and order: Idle, Submitted, Observed, Unconfirmed, Offline. Each board renders it differently, but the vocabulary is shared. The color board states the concept most explicitly: idle → submitted (soft pulse) → observed (settled); unconfirmed = TV state unknown; offline = TV unreachable.
6. **One-time disclosure instead of hidden limitations.** The exact sentence "This TV doesn't report Play — it may still work." appears in all four directions. Native additionally gates the Pad slot on a failed pointer probe; Tactile labels a 2014 TV "Unsupported · Best-effort"; Signal exposes "Pointer probe: Not run" in its scan readout.
7. **Plain-language failure copy with one next action.** The offline message is "TV didn't answer — retry?" in all four (Tactile only in its legend; the others render it in a banner or legend). "Retry" and "Not now" recur.
8. **System fonts, emoji/Unicode iconography, no webfonts or image assets.** All four use the same `-apple-system … sans-serif` stack; Signal adds a system mono stack for readouts. Icons are characters (📺 ⚙ ⏻ ⏯ 🔉 🔇 ⌂ ↩ 🎬 🔍 ⌨ 🎤 👆 📶 🪞). The only CSS-drawn illustration is Noir's cast disc.
9. **Motion is annotated, not implemented.** No board contains `@keyframes` or a transition except Noir's `transition:none`. Each header describes intended motion (900ms breath / 150–300ms snaps / 200ms swell + key travel / none + 1Hz blink), but the mocks are static.
10. **Two hero modes are assumed.** Each direction names mode (a) "thumb-arc" and mode (b) "big-pad," says the other exists as an alternate hero, and depicts only one. Noir and Native depict thumb-arc; Tactile and Signal depict big-pad.

### Where they genuinely diverge

| Axis | Noir | Native | Tactile | Signal |
|---|---|---|---|---|
| Register | premium, quiet, cinematic | first-party, conventional, trustworthy | playful physical-remote homage, chunky | industrial utility, brutalist |
| Surface color | warm near-black / true black | navy blue-black | espresso brown | pure black |
| Accent strategy | one champagne accent; ring is only saturation | blue accent on rims/focus/fills | orange plastic + brass + LED colors | no accent; three signal hues only |
| Type weight | 300–400 | 400–600 | 700–800 | 800–900, uppercase |
| Density | low | medium | high | low count of huge targets |
| Key shape | rounded rects + circles, hairlines | rounded rects, pills, circles | beveled caps with 4px side walls | square, 4px outlines, no radius |
| Ring geometry | 236px circle, 1.5px stroke + glow | 250px circle, 2px stroke + glow | 308px rounded-square light pipe in a groove, 3px | the pad's own 6px square outline |
| Ring color logic | ivory pulse / jade observed / amber / red | blue pulse / green observed / amber / red | ivory LED / green LED / amber LED / red LED | white idle / double white submitted / green / amber / red |
| Hero depicted | thumb-arc (a) | thumb-arc (a) | big-pad (b) | big-pad (b) |
| Secondary screen | cast session | pairing/TOFU | TV management (2 cards) | TV management (empty) |
| Failure treatment | red ring + banner + 45% dimmed keys | amber toast, escalates to red + banner | amber LED + brass "Unsupported" plate | red outline + banner + last-confirmed timestamp, keys still usable |
| Motion implication | 900ms breath / 400ms settle | 150ms key flash / 300ms pulse | 200ms light-pipe swell / 2–4px key travel | none / 1Hz blink on unconfirmed |

### UI strings and copy that recur

- `UniRem · UX Direction 1 of 4` … `4 of 4`; `UniRem · Color Theme Variations`.
- `Living Room TV`; `Samsung UE55NU8000`; `Tizen` / `Tizen 5.0`; `Home-WiFi-5G` (Tactile, Signal).
- `This TV doesn't report Play — it may still work.`
- `TV didn't answer — retry?`; `Retry`; `Not now`.
- `Home`, `Back`, `Apps`, `Search`; `Text`, `Voice`, `Pad`, `Cast`, `Mirror`, `Wake`.
- `Swipe = arrows · Tap = OK` (Tactile, Signal); `Tap pad` (Signal); `OK` (all D-pads; the color board shows only `●`).
- `Ring: idle — last key confirmed 21:46`; `Ring: red — offline since 21:49` (Signal).
- `Full control · confirms keys`; `Unsupported · Best-effort`; `Open remote`; `Forget TV`; `＋ Add a TV` (Tactile).
- `No TVs yet.`; `＋ Add your first TV`; `The TV must be on the same Wi-Fi as this phone.`; `Pointer probe`; `Scan 0 TVs found` (Signal).
- `Press Allow on your TV`; `This is my TV — connect`; `That's not my TV`; `First time connecting — verify it's yours`; `MAC 8C:F5:A3:••:••:4D · same Wi-Fi as this phone` (Native).
- `This TV doesn't report Play` chip; `Pad stays hidden until your TV reports a pointer. All other keys work as normal.` (Native).
- `TV didn't answer — retry?` chip in every color-board mini-remote; `● observed` in every topbar.

### Prior decisions embedded in the boards (reference, not inheritance)

These are specific decisions the discarded run already made. They should be treated as artifacts of the reference boards, not as requirements:

1. **The five-state ring and its names** — Idle, Submitted, Observed, Unconfirmed, Offline — with `submitted` described as a soft pulse and `observed` as a settle. The color board fixes the semantic token names `ring.idle`, `ring.submitted`, `ring.observed`, `ring.unconfirmed`, `ring.offline`.
2. **Ring as the single saturated/honesty element**, including the Noir rule that it is "the ONLY saturated element on screen" and Signal's rule that all three hues belong to the ring. Native and Tactile soften this (green dot, amber LED) but keep the ring central.
3. **The D-pad center key wears the ring in its observed color** — stated in the color-board header and rendered as the OK key's observed halo.
4. **Two hero modes** — (a) thumb-arc, (b) big-pad — with one shown per direction and the other described as an alternate.
5. **The six-slot launcher** and its labels: Text, Voice, Pad, Cast, Mirror, Wake.
6. **Capability gating** — Pad hidden/disabled after a failed pointer probe; Play flagged as unreported; TV support tiers "Full control · confirms keys" and "Unsupported · Best-effort."
7. **TOFU pairing** — name, model, masked MAC, "This is my TV — connect" / "That's not my TV."
8. **The six theme names** — Midnight Ember, Tizen Blue, Graphite Mint, Warm Slate, High Contrast Mono, Deep Teal — and the dark-primary/light-secondary split.
9. **The 390×844 portrait frame**, status-bar/topbar/chip/hero/launcher anatomy, and the `Living Room TV` / `Samsung UE55NU8000` sample device identity.
10. **Offline copy and one-action recovery** — "TV didn't answer — retry?" with Retry / Not now, and a banner rather than a modal.
11. **Power marked red (or red on hover), volume as a three-segment rocker, play/pause as a separate transport key**, and a six-item launcher as the bottom sheet.
12. **The color board's simultaneous "● observed" topbar and offline chip** — an unresolved contradiction in the reference; useful to know it exists, not to carry it forward.
