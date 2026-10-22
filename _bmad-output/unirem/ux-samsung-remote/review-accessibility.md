# Accessibility Review — UniRem

**Scope:** `DESIGN.md` (681 lines) and `EXPERIENCE.md` (438 lines), reviewed against WCAG 2.2 AA with TalkBack as the primary screen reader, for the stated use case — one-handed, dark living room, public Play Store.
**Method:** every hex value in both documents was extracted and contrast ratios computed from them (WCAG 2.x relative-luminance formula, sRGB). CVD collapse was computed with Viénot/Mollon 1999 protanopia/deuteranopia/tritanopia matrices at full severity. All figures below are measured, not estimated.

---

## Overall verdict

The documents are unusually disciplined about the one thing they decided to be rigorous about — the outcome ring has a five-clause testable gate and the "no fake-success green" rule is genuinely enforced — and genuinely silent almost everywhere else. The ring's colourblind story is well built but has a fatal interaction with reduced motion: `submitted`'s only non-colour signal is the pulse, and once motion is disabled the ring falls back to hue alone, which measures as indistinguishable from `observed` in five of six dark themes including the one designated as the accessibility theme. Separately, the accessibility theme's own light mode contains a false pass that the document records as verified, and the entire Tier-1 label layer — the stamped words on every key — sits between 2.08:1 and 3.95:1.

Rating the product against its own thesis: for a user who is blind, has reduced motion on, or is navigating with a screen reader, "confirmed" and "sent, no response" are not reliably separable. The promise is stated correctly and specified correctly in intent; the parts that would actually deliver it to a disabled user are the parts left as `[ASSUMPTION]` or never specified at all.

---

## Findings by severity

### Critical (5)

**[Screen reader] — The outcome announcement is unattributable, and its own anti-flood rule guarantees a flood** (§EXPERIENCE.md L145–153, L315)

The three announcement strings are `Sent. Your TV hasn't confirmed it yet.` / `Your TV did it.` / `Sent, but your TV hasn't answered.` None names the key. A TalkBack user presses Down and hears "Your TV did it." — *it* is unresolvable. Did the TV move its focus, or did the app send a byte? The ring cannot answer that, because the ring's own non-color signals (persistence, emission) are properties of the ring, not of the action. Worse, L315 specifies announcement "only on a *transition*, never per key press, so a burst of navigation does not become a torrent" — but every key press *is* two transitions (`idle→submitted`, `submitted→observed`), so each press queues two utterances. Holding a volume rocker for three seconds at ~10 presses/sec produces ~20 announcements. The mitigation does not work because it is stated at the wrong granularity.

Separately, and independently fatal: the ring's state is specified only as a **live-region announcement**. Nothing in either document places the ring in the accessibility tree with a `stateDescription` or `contentDescription`. A live region is not queryable — a user who missed the announcement, or who was interrupted, has no way to swipe back and ask what the ring is currently saying. There is no on-demand read of the product's central signal.

*For whom:* every TalkBack user, on every key press, for the product's headline feature.
*Fix:* name the action in the string (`Volume up sent. Your TV hasn't confirmed it yet.`); drop to one announcement per press by announcing only the terminal verdict; expose the ring as a focusable node whose `stateDescription` reads the current verdict on demand.

---

**[Reduced motion] — Removing motion removes `submitted`, the state the entire product exists to represent** (§DESIGN.md L386–390, L597, L679–680; §EXPERIENCE.md L310, L333)

`submitted`'s documented non-color signal is "**motion** — a single 200ms swell, then off." `observed` is "**persistence** — it stays lit." With motion disabled, `submitted` collapses to an instantaneous state change whose static form is *a solid ring in `ring-submitted`*. `observed` is *a solid ring in `ring-observed`*. Measured separation between those two solids:

| Theme (dark) | ΔL\* | ΔCR | Verdict |
|---|---|---|---|
| Tizen Blue | 7.9 | 1.27 | **indistinguishable** |
| Graphite Mint | 6.8 | 1.21 | **indistinguishable** |
| Warm Slate | 1.7 | 1.05 | **indistinguishable** |
| **High Contrast Mono** | **4.5** | **1.12** | **indistinguishable** |
| Deep Teal | 11.8 | 1.39 | **indistinguishable** |
| Midnight Ember | 17.3 | 1.66 | separated (the only one) |

Both fall under the document's own rule-3 thresholds (needs ΔCR ≥ 1.5 **or** ΔL\* ≥ 15). In **the accessibility theme**, a user with reduced motion on sees `Sent, awaiting evidence` and `TV confirmed it` as the same ring. This is the precise failure the product was built to prevent, landing on the user who did the most to accommodate themselves.

§EXPERIENCE.md L310 makes the error explicit and then repeats it: *"submitted then relies on the single swell completing and extinguishing, which is itself the signal."* A signal that reduced motion has removed cannot be relied upon.

*For whom:* users with reduced motion enabled — a vestibular, migraine, or distraction-sensitive population — compounded by anyone with a color vision deficiency.
*Fix:* give `submitted` a static non-color channel that does not depend on motion and does not collide with `observed`: a partial-arc ring (a progress-style sweep), or a dotted/thin stroke distinct from both solid strokes. Then the pulse is decoration on top of a channel that already works.

---

**[Theme] — High Contrast Mono light mode: the accessibility theme's "sent" state is invisible, and the document records it as a verified pass** (§DESIGN.md L402, L408)

§DESIGN.md L408, under a `[RESOLVED]` heading, states: *"High Contrast Mono light requires no special seat: its `ring-submitted` `#111111` reads 4.4:1 against the groove."*

Measured against `{colors.const-groove}` `#120C08`: **1.03:1**. That is not 4.4:1 — it is the same color. `#111111` and `#120C08` are indistinguishable to any eye, at any contrast sensitivity. The "verification" recorded here is a false pass on the theme explicitly designated as the accessibility theme, and it was recorded specifically to stop the next reviewer re-deriving it.

The rest of that theme's light ring survives (observed `#6E7A00` = 4.11:1, unconfirmed `#8A6100` = 3.50:1, offline `#C0190F` = 3.13:1) — so `submitted` is the one state that vanishes, and it is the state that says *your action is in flight*. The other three light-mode values were never listed in the light table at all, which suggests this cell was filled without running the number.

*For whom:* anyone who selects the accessibility theme and then switches to light mode. Under-validated because the theme "clearly" should work.
*Fix:* re-cut HC Mono light `ring-submitted` against the groove seat, and re-run every ring cell in both modes against the seat with an automated check — the gate is already written and testable; only this one cell was skipped.

---

**[Learnability] — The only affordance for the invented swipe gesture is 2.08:1, at 9.5sp, positioned under the user's thumb** (§DESIGN.md L450, L591; §EXPERIENCE.md L292, L89)

§DESIGN.md L450: the pad's hint — `Swipe = arrows · Tap pad = OK` — sits at `{typography.micro-wide}` (9.5sp/800, uppercase, tracking 0.18) in `{colors.const-stamp-ink}` `#7F6C50`, "12px above the pad's bottom edge."

Measured: `#7F6C50` on `{colors.const-leather-top}` `#4D3C2C` = **2.08:1**. On leather-mid = 2.67:1, on leather-low = 3.15:1. All fail 4.5:1 by a wide margin. It is 9.5sp — the second-smallest type in the system — in all caps with heavy tracking, which costs legibility at small sizes.

And it is placed at the bottom edge of the pad, which is precisely where a thumb rests in the one-handed grip the product is built around (§EXPERIENCE.md L291–292, L322). In the actual use position, the only instruction is under the thumb.

There is no second affordance. §EXPERIENCE.md L89 concedes the swipe threshold and direction mapping are an `[ASSUMPTION]` — "no gesture geometry was captured" — so this one low-contrast hint is carrying the entire learning burden for a gesture with no established convention.

*For whom:* every first-time big-pad user; doubly so for low-vision users, who are the group most likely to need the hint and least able to read it.
*Fix:* move the hint above the thumb's rest zone, raise it to ≥12sp in `text-primary` (measured 18.41:1 on base, 13.59:1 on shell), and keep the stamped treatment as a secondary rendering of it.

---

**[Keyboard / switch] — The OK key has no focus indication** (§DESIGN.md L651, L536)

§DESIGN.md L651: *"for hardware-key remotes and accessibility focus: the OK dome's existing gloss is the focus affordance."*

§DESIGN.md L536 specifies the gloss as a static radial highlight at 34%/26%, warm white at 50% alpha, falling to transparent by 42% — a property of the dome's material, present identically whether the key is focused or not. Nothing in the document says the gloss changes on focus. Read literally, focusing the OK key produces no visual change at all.

The OK dome is the primary commit control — it is what presses Enter on every TV menu, every dialog, and every playback surface. WCAG 2.4.7 Focus Visible (AA) fails on it, and it fails on the one key where losing focus is most likely to send the user somewhere unintended.

Every other element gets the correct treatment — "a 1px outer rule in `{colors.accent-brand}` at 2px offset, added outside the cap silhouette… Focus must never be conveyed by changing the cap's fill." That rule is right. It was just not applied to the OK dome.

*Fix:* give the OK dome the same outer accent rule as every other cap; drop the gloss as a focus signal entirely.

---

### High (7)

**[Contrast] — The word label on every key cap fails AA in all four variants** (§DESIGN.md L39–42, L448, L574)

§DESIGN.md L448 is candid that the stamped micro-label "reads as ink pressed into plastic — it is inset-feeling and low-contrast by design." The numbers:

| Pair | Measured | Need |
|---|---|---|
| `const-micro-ink` `#7A6A52` on `const-cap-top` `#E9DFC9` | **3.95:1** | 4.5:1 |
| `const-micro-ink` `#7A6A52` on `const-cap-top-floor` `#D9CDB2` | **3.32:1** | 4.5:1 |
| `const-micro-ink-dark` `#A3906F` on `const-cap-top-dark` `#4A3A2C` | **3.51:1** | 4.5:1 |
| `const-micro-ink-dark` `#A3906F` on `const-cap-top-dark-floor` `#3B2E22` | **4.24:1** | 4.5:1 |

All four fail. The cap *glyph* passes comfortably (`const-cap-ink` on `const-cap-top` = 9.86:1), and §DESIGN.md's own rule correctly says the glyph carries the function. But the glyph is an icon — `▶`, a transport triangle, a mute speaker. For a user who does not build instant icon↔function mappings (cognitive disability, first-use, cultural difference), the word is the label, and it is the one that fails.

Compounding: `{typography.micro}` is **8sp** — the smallest type in the system — in all caps at 0.12 tracking.

Related, same family: the etched D-pad direction glyphs in big-pad are `const-etch-ink` `#9C8563` on leather-mid `#3A2C1F` = **3.81:1**, further multiplied down by the 75% opacity specified at L591. Big-pad's direction glyphs are the only cue that the pad has four directions at all.

*Fix:* raise the micro-label ink to ≥4.5:1 in all four cap variants and take the etched glyphs to full opacity; the "pressed into plastic" reading survives on a darker ink.

---

**[Color vision] — The TV-card status LED is hue-only, in all six themes, and it makes a reachability claim** (§DESIGN.md L637, L352–354)

§DESIGN.md L637: the card carries an 11px LED, `.on` at `{colors.sem-success}` with an 80% glow, `.warn` at `{colors.sem-warning}` with a 70% glow. Eleven pixels. No shape difference, no glyph, no text difference. Measured separation between `.on` and `.warn`:

| Theme | ΔCR | ΔL\* | Max CVD ΔRGB |
|---|---|---|---|
| Midnight Ember | 1.24 | 7.3 | 194 |
| Tizen Blue | 1.09 | 2.9 | 255 |
| Graphite Mint | 1.08 | 2.6 | 250 |
| **Warm Slate** | **1.03** | **1.1** | 77 |
| High Contrast Mono | 1.43 | 13.3 | 64 |
| Deep Teal | 1.06 | 2.0 | 216 |

Not one theme clears the document's own rule-3 thresholds. In Warm Slate the two LEDs differ by ΔL\* = 1.1 — they are the same lightness. This is the identical bug the document identified and fixed for the ring (`observed` vs `unconfirmed`, CR 1.16 → dashed stroke), applied to a component nobody ran the gate against.

It also sits in direct tension with the product's own moral core: a green LED on a TV card is a claim that the TV is reachable. §DESIGN.md L375 says `sem-success` "is **not** used on the key hot path; the ring owns observed confirmation" — true, this is not the hot path — but the same honesty rule should govern a reachability claim.

*Fix:* give the LED a second channel — filled ring vs hollow ring, or solid vs dotted — and pair it with a text state that is already in the component's vocabulary.

---

**[Touch] — Hit-slop on the discrete D-pad manufactures the overlapping touch regions that cause wrong-key presses** (§DESIGN.md L239, L485–491, L593)

§DESIGN.md L489 proposes the discrete D-pad key (42×36, 6px gaps) be corrected to 48dp via transparent hit-slop. Doing the arithmetic on the specified geometry:

- Cell 42 wide × 36 tall, gap 6 both axes → pitch X = 48, pitch **Y = 42**.
- Expanding each cell to a 48dp hit region gives vertical neighbours **6px of overlapping touch ownership**.
- Horizontally they abut exactly (48 pitch, 48 target) — no overlap, but also no tolerance.

So the prescribed fix converts a sub-48dp target into a pair of targets that both claim the same 6px strip. A thumb overshooting by six pixels vertically now hits two live regions instead of one, and the resolution is arbitrary. The D-pad is the highest-frequency control in the product — it is how a user moves through every TV menu — and a mis-tap there sends a navigation command to a television.

There is also no room budget. A 3×3 grid at 48dp cells is 144×144; the specified grid is 138×120. The D-pad must grow 6px in each axis inside a hero whose layout is otherwise fixed.

§DESIGN.md L491 gets the principle right — *"Shrinking the visible cap to fit a hit target is never the fix"* — but that principle is about *appearance*. Hit-slop is not free: it enlarges the interaction region, which at this cell density means enlarging the region shared with a neighbour.

*Fix:* keep hit-slop where neighbours are ≥48dp apart (gear, sheet launcher, card action — those are fine), but for the D-pad either grow the caps to 48dp and re-fit the hero, or reduce the ring of discrete keys from 3×3 to a 4-key cross with generous separation, which is also a better thumb-arc layout.

---

**[Type scale] — The ramp's stated fit bar is 130%; WCAG 1.4.4 requires 200%, and at 200% the caps overflow** (§DESIGN.md L418, L393–440)

§DESIGN.md L418: *"A full ramp must not overflow a cap at 130% font scale — this is a hard test, not a nicety."* WCAG 1.4.4 Resize Text (AA) requires 200%. The document has set its own bar below the requirement it is measuring against.

At 200%:
- Key cap, 58dp tall: glyph `{typography.cap}` 17sp → 34sp, linebox **40.8px**; micro-label `{typography.micro}` 8sp → 16sp, linebox **19.2px**. Total **60px > 58px**.
- Discrete D-pad key, 36dp tall: the cap glyph alone needs a **40.8px** linebox in a 36px cell. Overflow by 4.8px before the label is considered.

A user at maximum font scale — someone reading the app from arm's length in a dark room, which is this product's stated context — loses the glyph, the label, or the cap's own geometry. §EXPERIENCE.md L341 promises "No key label truncates, no control collapses, and the ring and sheets stay usable at the largest system font setting." That promise does not hold against the specified numbers.

*Fix:* treat 200% as the gate. Decouple cap interior layout from type size (fixed glyph slot, label wraps or hides rather than pushing), and specify the discrete D-pad at a size that accommodates 200%.

---

**[Contrast] — Disabled and offline-dimmed text is not legible, and one of the two tokens is undefined** (§DESIGN.md L653; §EXPERIENCE.md L208, L228)

**Disabled** (§DESIGN.md L653): "glyph and micro-label drop to `{colors.const-ink-faint}` at roughly 38% alpha." Two problems. The token `const-ink-faint` **does not exist in the frontmatter `colors` block** — the treatment is both non-compliant and unimplementable from the token set. And at 38% alpha the measured result is:

| Over | Result | Ratio |
|---|---|---|
| bone cap-top `#E9DFC9` | `#A69C8A` | **2.05:1** |
| dark cap-top `#4A3A2C` | `#443628` | **1.07:1** |
| `surface-key` `#1F1A17` | `#29221B` | **1.10:1** |

On dark caps and dark keys the "disabled" ink is within 7–10% luminance of the surface — it is not visibly different from background at all. §EXPERIENCE.md L186 makes disabled-with-a-reason the designated treatment for a capability the model lacks, so the reason attached to it is unreadable too.

**Offline** (§EXPERIENCE.md L208, L228): keys dim to 45%. Compositing `const-cap-ink` at 45% over a dimmed bone cap gives **3.24:1**; the stamped micro-label at 45% gives **2.23:1**. §DESIGN.md L317 states the principle this violates: *"When state is bad, it is legible from across a room."* The product's loudest failure state dims its own key labels below AA.

*Fix:* define `const-ink-faint` in the token set at ≥4.5:1 against every cap variant; set the offline dim to a value that keeps the glyph ≥4.5:1 (roughly 65–70% of the cap's own ink, or dim the cap fill while holding ink constant).

---

**[Contrast] — The focus rule is drawn in the same color as the surface it lands on** (§DESIGN.md L651, L345–346)

§DESIGN.md L651: focus is "a 1px outer rule in `{colors.accent-brand}` at 2px offset." §DESIGN.md L379 and L652 also make `{colors.accent-brand}` the fill for **selected** caps and the accent-filled launch affordance.

Measured `accent-brand` against `accent-brand-pressed` (what a focus ring 2px outside a selected, pressed cap sits against):

| Theme | Ratio |
|---|---|
| Midnight Ember | 1.45:1 |
| Tizen Blue | 1.50:1 |
| Graphite Mint | 1.45:1 |
| Warm Slate | 1.46:1 |
| **High Contrast Mono** | **1.39:1** |
| Deep Teal | 1.43:1 |

All six fail 3:1. The focus ring is invisible in every theme precisely when the focused element is also selected — which is the common case in TV navigation, where selection follows focus.

*Fix:* the focus rule should use a color that is not the selection color. `text-primary` measured against every `accent-brand` clears 3:1 in all six themes and never collides with selection.

---

### Medium (7)

**[Contrast] — The elevation system's own edges are below 3:1, so the physical language is invisible** (§DESIGN.md L499–534, L37, L49, L277, L531)

§DESIGN.md L504: *"The first shadow is a hard 0-blur offset in an opaque darker material color. That is the side wall, and it is what makes the object read as thick plastic."* The wall is the whole tactile thesis. Measured against the surface it sits on:

| Element | Measured | Need |
|---|---|---|
| `const-cap-side-dark` `#33281E` on shell `#1D1510` | **1.25:1** | 3:1 |
| `const-hairline` `#453527` on shell `#1D1510` (sheet lip) | **1.54:1** | 3:1 |
| `const-hairline` on shell-2 `#2A1F17` | **1.37:1** | 3:1 |
| `const-plate-rim` `#8F8367` on brass `#C9BDA2` (chip edge) | **2.01:1** | 3:1 |
| leather-top `#4D3C2C` on groove `#120C08` (pad boundary) | **1.85:1** | 3:1 |

The dark key-cap variant and the sheet lip are worst. §EXPERIENCE.md L317 requires "Every sheet is reachable and dismissible without sight of the sheet edge" — behaviorally correct, and for a low-vision user the edge is at 1.54:1. The bright bone cap side wall passes (7.50:1) precisely because it is light on dark; the dark cap, which is specified for the utility shelf and secondary actions (§DESIGN.md L571), has no visible wall at all.

---

**[Contrast] — The offline banner's only red marker is 2.43:1 on brass** (§DESIGN.md L294–298, L641; §EXPERIENCE.md L228)

The banner is `{const-plate-brass}` `#C9BDA2` with a 4px left rule in `{ring-offline}` `#E03131`. Measured: **2.43:1**. Fails the 3:1 non-text minimum by 0.57.

This compounds with a stated design constraint. §EXPERIENCE.md L228: "Red appears nowhere else on screen while offline," and §DESIGN.md L641: the banner "is the *only* banner permitted to reference the offline state." So the banner's left rule is the sole red object on the banner, and it is below threshold. The banner's own text carries the meaning, which is the right architecture — but the rule that was added specifically to make the banner read as an offline surface does not do so.

---

**[Contrast] — The inline unsupported chip's LED is 1.17:1 on its own plate** (§DESIGN.md L627, L35)

§DESIGN.md L627: the chip carries a "9px amber LED with an 80% glow at `{colors.const-plate}`-family amber," on a `{const-plate-brass}` `#C9BDA2` plate. A representative amber `#FFC400` on that plate measures **1.17:1** — the LED is the same lightness as its own background. The chip's text (`{typography.chip}`, plate ink on brass = 7.72:1) does carry the meaning, so this is not a comprehension failure; the LED is simply not doing the job it was specified to do.

---

**[Outcome honesty] — The D-pad group's announced state is an unprobed claim about the TV** (§EXPERIENCE.md L316)

§EXPERIENCE.md L316: *"The D-pad is exposed as a labeled group whose state is up to date, so a user who swipes through the group hears where the TV is, not a bare list of labels."*

"Where the TV is" is the TV's on-screen focus position. Establishing that requires the probe — the same probe the document elsewhere says is unreliable, model-dependent, and unavailable on the 2014–15 H/J series entirely (§EXPERIENCE.md L139). Announcing it as up-to-date group state presents an unverified reading of a remote device as a fact, in the one surface where a screen reader user is navigating.

This is the exact pattern §EXPERIENCE.md L410 calls the "most damaging pattern available to this product." It is correct everywhere else in the document; here it has leaked into the accessibility layer.

*Fix:* scope the group state to "what the app last observed, at <time>", or drop the state and keep only the per-key labels until the probe is confirmed on that model.

---

**[Color vision] — Colour keys have no non-color name specified** (§EXPERIENCE.md L314, L323)

§EXPERIENCE.md L314 gives a TalkBack label rule and lists eleven names. §EXPERIENCE.md L323 then puts "colour keys" in the launch set. On a Samsung remote those four keys are identified by a coloured dot and nothing else, so their accessible names will be "Red," "Green," "Yellow," "Blue" — which are (a) meaningless to a user with a color vision deficiency, (b) meaningless without the TV screen in view, and (c) unresolvable for the common case where all four are present and the user is looking at the phone, not the TV.

No shape, glyph, or function name is specified for them anywhere in either document.

*Fix:* name colour keys by function with colour as a secondary channel (e.g. "Red button — colour key, first of four"), matching the document's own stated principle at L314 that keys are named "the way a person would name it on a remote."

---

**[Contrast] — Light mode: four themes fail `text-secondary` on `surface-key`, and `text-on-accent` is missing from the light table entirely** (§DESIGN.md L355–367)

`text-secondary` on `surface-key`, light mode:

| Theme | Measured | Need |
|---|---|---|
| Warm Slate | **3.97:1** | 4.5:1 |
| Tizen Blue | **4.20:1** | 4.5:1 |
| Midnight Ember | **4.27:1** | 4.5:1 |
| Deep Teal | **4.43:1** | 4.5:1 |

Four of six fail. `surface-key` is specified as "reserved for flat and ghost key fills" (§DESIGN.md L406) — which is exactly where small secondary labels sit.

Separately, the LIGHT block of the six-theme table (L355–367) lists no `text-on-accent` row at all. The frontmatter dark value therefore carries into light mode, producing:

| Theme (dark `text-on-accent` on light `accent-brand`) | Measured |
|---|---|
| Deep Teal `#08201F` on `#8A6A28` | **3.37:1** |
| Warm Slate `#FFF4EC` on `#B25A38` | **4.40:1** |
| High Contrast Mono `#000000` on `#6E7A00` | **4.45:1** |

All three fail 4.5:1. And in dark mode, `text-on-accent` on `accent-brand` already fails in Tizen Blue (3.72:1) and Warm Slate (3.31:1). The same two themes also fail on the "Add a TV" orange cap, which uses `const-cap-ink` on `accent-brand`: Tizen Blue **3.51:1**, Warm Slate **3.64:1**. One theme re-cut on two tokens fixes both.

---

**[Alternative input] — "Nothing is drag-only" is false for the touchpad surface** (§EXPERIENCE.md L327, L56, L87)

§EXPERIENCE.md L327: *"Every sheet is reachable and operable with a hardware keyboard and with switch scanning. Nothing is drag-only."* Surface 6 (Touchpad, CAP-3) is by definition a drag-only surface, and §EXPERIENCE.md L87 makes it probe-gated per model — so on models that fail the cursor probe the feature is hidden entirely (§L87, L186).

The blanket claim is therefore untrue as written. Switch-access and keyboard users have no route to pointer movement. This may be an acceptable scoping decision (pointer is a convenience, not a core function), but the document states it as a guarantee rather than as an exception, which is the more serious problem.

*Fix:* scope the sentence — "every sheet except Touchpad (CAP-3), which is pointer-only by nature and probe-gated per model" — and note the exclusion in the surface table.

---

### Low (4)

**[Duration] — The 200ms swell is below the perceptual floor, and the success state may be the shortest-lived one** (§DESIGN.md L253, L382, L680; §EXPERIENCE.md L216)

`submitted-duration: 200ms`. A discrete visual change shorter than roughly 400ms is difficult to register as a state change rather than a flicker — and 200ms is also well inside the window where it is competing with the announcement queue. §DESIGN.md L680 makes the 200ms transient load-bearing ("Let `submitted` linger" is the Don't).

Related, and unaddressed: §EXPERIENCE.md L216 concedes that dwell timings — including *"how long `observed` settles before returning to `idle`"* — are unmeasured. If `observed` returns to `idle` on a similar timescale, the product's only true success signal is no longer on screen than its failure signal was. Given the thesis, `observed` should hold noticeably longer than `submitted`, not for the same duration.

---

**[Low vision] — Bone caps and primary text sit at 13.6:1 and 18.4:1 on pure black, in a dark room** (§DESIGN.md L54, L57; §EXPERIENCE.md L27, L338)

`const-cap-top` `#E9DFC9` on `const-shell` `#1D1510` = **13.59:1**. `text-primary` `#F5EFE9` on `surface-base` `#000000` = **18.41:1**.

Neither is a contrast failure — both are far above AA. They are glare and blooming risks: an 18:1 white on pure black produces halation for users with astigmatia, and a field of 13.6:1 bone caps is the brightest object in a dark living room when the user's eyes are adapted to the dark. §EXPERIENCE.md L338 already commits to "no light-emitting flashes, no white flashes, no full-screen brightness jumps for a state change" — the resting state is static, so no flash rule is breached, but the halation exposure is real and is not addressed anywhere. Pure `#000000` as `surface-base` on OLED additionally smears on scroll.

---

**[Alternative input] — The hero's keyboard model is unspecified** (§EXPERIENCE.md L290, L321–329)

§EXPERIENCE.md L290: "Tap is press. One discrete press is one key frame… On the hero there are no gestures that mean something other than a key." That is a touch model. With a hardware keyboard attached, the arrow keys are ambiguous: do they move accessibility focus around the hero, or do they emit D-pad keys to the television? Both are defensible; the two are mutually exclusive; neither document decides. §EXPERIENCE.md L327 guarantees sheet operation by keyboard but says nothing about the hero, which is where the keyboard user spends their time.

Given that the design explicitly contemplates hardware-key remotes (§DESIGN.md L651), this needs an answer.

---

**[Contrast] — The ring at rest is 1.68–2.60:1, so the product's primary status object cannot be located** (§DESIGN.md L347–351, L394)

§DESIGN.md L394 exempts `ring-idle` from the 3:1 gate: *"it is supposed to recede, and the direction states the groove reads as decoration."* The exemption is reasonable for a state that means nothing is pending. The measurement is still worth recording:

| Theme (dark) | `ring-idle` vs groove | Ratio |
|---|---|---|
| Graphite Mint | `#343A3E` | **1.68:1** |
| Warm Slate | `#403830` | **1.69:1** |
| Midnight Ember | `#463A32` | **1.77:1** |
| Tizen Blue | `#2C3E5C` | **1.80:1** |
| Deep Teal | `#1E454B` | **1.86:1** |
| High Contrast Mono | `#555555` | **2.60:1** |

§DESIGN.md L404 calls the ring "the loudest, most honest object on screen." A low-vision user scanning for it finds nothing at rest. The fix is not to make idle loud — the exemption is right — but to give it a *minimum* threshold (say 2:1) so the object is locatable without competing for attention.

---

## Examined and clear

- **The `observed` / `unconfirmed` dashed-vs-solid distinction** (§DESIGN.md L389, L597; §EXPERIENCE.md L308). This is the best piece of accessibility engineering in either document. The dash spec is complete (8px on / 6px gap, round caps, phase-aligned, `PathEffect.dashPathEffect`), the rationale for round caps is correct, and the analysis of why the original pairing collapsed (CR 1.16 deuteranopia, CR 1.03 Warm Slate) is measured rather than asserted. It survives greyscale, all three CVD simulations, reduced motion, and color clipping. The one caveat is in the Critical finding above: `submitted` was not given the same treatment and is the state that actually breaks under reduced motion.
- **The five-clause ring gate** (§DESIGN.md L392–398). Rules 1–5 are written as testable criteria with numeric thresholds, not as sentiment. This is exactly the right artifact — a code review can run it per theme per mode. It caught a real bug (`submitted` at 1.03:1) that would otherwise have shipped. It needs to actually be run, not just written.
- **Seat is Tier 1.** Measuring the ring against `const-groove` rather than `surface-base` (§DESIGN.md L400) is correct and important, and the note that stops the next reviewer re-deriving the wrong background is genuinely useful documentation.
- **"Red appears nowhere else on screen while offline"** (§EXPERIENCE.md L228). Excellent constraint. It makes `offline` the single loudest moment without introducing a second red to decode.
- **Focus must never be conveyed by changing the cap's fill** (§DESIGN.md L651). Correct, and it protects against the most common Compose focus bug.
- **Brass plate ink** — `const-plate-ink` `#33281C` on `const-plate-brass` `#C9BDA2` = **7.72:1** (5.83:1 on `plate-brass-floor`). The one Tier-1 label treatment that clears AA cleanly, and the single approved plate string (the unsupported marker) is therefore legible.
- **Big-pad exposed as discrete, individually labeled key actions to accessibility services** (§EXPERIENCE.md L179, L328). Explicitly named a hard requirement of the layout, not an enhancement. Correct.
- **Volume `−`/`+` assistive-tech equivalents** (§EXPERIENCE.md L329), and **discrete volume steps each getting their own ring pulse** (§EXPERIENCE.md L181, L293) — each step being an honest separate press rather than one smooth claim is exactly right.
- **The copy architecture.** Complete sentences, no runtime concatenation, role-named placeholders (§EXPERIENCE.md L33–38), and the banned-strings table (§EXPERIENCE.md L155–169) banning `Successfully sent!`, `Command executed ✓`, and any fleet-wide percentage. This is the right spine for the honesty promise, and it is unusually well built. A screen reader user is reading this copy on every single key press.
- **The outcome strings are semantically correct** — "unconfirmed" is a distinct verdict with its own sentence rather than a softened success, and §EXPERIENCE.md L153 explains why. The *attribution* problem in the Critical finding above is about which action the verdict refers to, not about the honesty of the verdict itself.
- **Text sheet: focus is a hint, never a gate** (§EXPERIENCE.md L280, L368), with a manual Type action always available. Correct handling of an unreliable upstream signal.
- **Nothing time-limited without saying so** (§EXPERIENCE.md L340) — the TV Allow/Deny window shows a plain countdown and explains its expiry rather than silently failing. Correct for cognitive and screen-reader users alike.
- **Sheets reachable and dismissible without sight of the sheet edge** (§EXPERIENCE.md L317). The behavioral requirement is right; only the measured 1.54:1 lip makes it hard in practice (see Medium).
- **Touch targets other than the D-pad.** Settings gear (44×44), sheet launcher cap (46×42), and card action (46 tall) all sit in arrangements with ≥8px gutters, so hit-slop to 48 is clean for them. The D-pad is the only geometry where the fix is unsafe.
- **The i18n deferral is recorded with a binding seam** (§EXPERIENCE.md L33–40), which is the correct way to defer localization without deferring the architecture that makes it possible.