# Spine Pair Review — UniRem

Reviewed: `DESIGN.md` (682 lines, 45.4 KB) and `EXPERIENCE.md` (439 lines, 48.8 KB), against `spec-samsung-remote.md`, the `design-example-mobile.md` / `experience-example-mobile.md` shape references, and `design-md-spec.md` frontmatter rules.

## Overall verdict

**Thin, and not for lack of authored material — for lack of extractability.** The reasoning is genuinely strong: both spines have decided nearly every load-bearing question (the outcome ring, the two-tier color system, physical elevation, the socket-lifetime rule), the four theme-vs-material conflicts are closed and dated, and the flows carry real protagonists and real failure branches. But a downstream consumer that does what this pair says to do — *source-extract* — cannot do it. `EXPERIENCE.md` makes 22 token references, and **none of them resolve**: 11 use a `{theme.*}` group that `DESIGN.md` never defines, and 11 use `{colors.ring.idle}` where `DESIGN.md` defines `{colors.ring-idle}`. `DESIGN.md` itself carries 4 references to three tokens that do not exist. And the six themes — the single largest body of color data in the spine, 168 hex values — live only in a Markdown table, never in frontmatter, so there is no machine-readable theme data at all.

That said: no color token is missing a hex value, `sources` resolves in both files, both spines hit the canonical section order exactly, all three hero flows have numbered steps + climax + failure branches, and component names are consistent across both files. **Fixing the token layer is a contained change** — mostly adding a `theme` group to frontmatter and normalizing kebab-vs-dot. The judgment underneath is not in question.

---

## 1. Flow coverage — **thin**

The three hero flows are excellent. Flow 1 (Kai, L347-360), Flow 2 (Dana, L362-373) and Flow 3 (Maya, L375-388) each have a named protagonist, 6-8 numbered steps, a bolded `**Climax:**` beat, and an explicit `Failure branches:` paragraph. Flow 1's climax — the app earning trust *by having once admitted it didn't know* — is the best sentence in the pair. The four secondary flows (CAP-5, 7, 4, 8) carry real behavior and honest limits.

Coverage is where it breaks down.

### Findings

- **[critical]** **CAP-3 (pointer/touchpad) has no Key Flow at all.** Every other capability gets one; CAP-3 gets a surface (IA #6, L56), honest-label copy (L135), a feature-session note (L286), and an explicit disambiguation from big-pad (L87) — but appears nowhere in Key Flows (L343-399). It is the one capability whose *success condition is a probe result* ("Pointer visibly moves and click activates on models passing the cursor probe") and whose spec mandates hidden-with-a-plain-explanation on failure, which is exactly the kind of branch a flow should pin down. *Fix:* add a compact secondary flow in the Wake/Mirror/Voice register: probe passes → pointer; probe fails → surface absent, with the `Pointer input isn't available on this TV.` copy from L135 and the "never conflate with big-pad" rule from L87 as its climax beat.

- **[high]** **The closure claim at L64 is false.** *"Every surface has a journey in **Key Flows** that lands on it."* Four of the twelve surfaces never appear in Key Flows: **Onboarding**, **Touchpad**, **Settings + diagnostics**, and **Tizen receiver companion**. Settings is the home of the theme picker, permissions, and the network-isolation diagnosis — three of the most implementation-heavy surfaces in the app — and it has no journey at all. *Fix:* either add three compact secondary flows (onboarding → pair → hero; settings → appearance/permissions/diagnostics; mirror → receiver absent → companion install), or downgrade the claim to name which surfaces are covered and which are specified by section instead. A claim a reader can disprove by counting is worse than no claim.

- **[medium]** **The four secondary flows are prose, not flows.** CAP-5 (Wake, L392), CAP-7 (Mirror, L394), CAP-4 (Voice, L396) and CAP-8 (H/J labels, L398) are each a single dense paragraph with no numbered steps and no `**Climax:**` marker. They are genuinely good paragraphs, but a story-dev consumer extracting "the wake flow" gets no step boundary and no terminal beat. CAP-5 is the worst case: it carries the most safety-relevant branching in the file — Wake is offered only after a user-confirmed checklist, is never a toggle, reports success *only* on an 8001/8002 answer, and makes no claim at all where directed broadcast cannot cross — and it is the least structurally pinned. *Fix:* lift each to 4-6 numbered steps plus a `**Climax:**` line, keeping the existing prose as the framing sentence.

---

## 2. Token completeness — **broken**

This is where the pair fails the question it was asked. `DESIGN.md` frontmatter is well-formed and unusually rich — 244 defined paths, `colors` / `typography` / `rounded` / `spacing` / `components` all present, correct kebab-case under `colors`, correct nested objects under `typography`. **Every single `colors.*` token carries a valid hex value; there are zero color tokens missing a hex.** Contrast targets for the load-bearing combination (ring vs. seat) are stated as a five-rule testable gate (L392-398), which is exactly right.

What is missing is everything that isn't a Tier-1 or default-theme token.

### Findings

- **[critical]** **The `{theme.*}` group does not exist.** `EXPERIENCE.md` references `{theme.midnight-ember}`, `{theme.tizen-blue}`, `{theme.graphite-mint}`, `{theme.warm-slate}`, `{theme.high-contrast-mono}` and `{theme.deep-teal}` — **11 references, 6 distinct tokens** — at L27, L29, L215, L303 and L309. `DESIGN.md` frontmatter has top-level keys `name, description, status, sources, updated, colors, typography, rounded, spacing, components` and **no `theme` group at all**. The themes exist only as the Markdown matrix at L335-367: 6 themes × 16 tokens dark + 6 × 12 light = **168 hex values that no extractor can reach.** This is the single largest machine-readability failure in the pair, and it hits the load-bearing decisions: default theme (L369), the accessibility theme (L402), and the theme swap contract (L327). *Fix:* add a `theme:` group to frontmatter keyed by slug, each theme carrying its own token set — or, minimally, add `theme.midnight-ember.name` / `.slug` anchors plus a stated rule that theme *values* are read from the L335 matrix. The full matrix is the right answer; the six anchors are the cheap one.

- **[critical]** **All 11 `{colors.ring.*}` references use the wrong separator.** `EXPERIENCE.md` writes `{colors.ring.idle}`, `{colors.ring.submitted}`, `{colors.ring.observed}`, `{colors.ring.unconfirmed}`, `{colors.ring.offline}` at L184, L204-208, L228, L238, L353, L354, L358. `DESIGN.md` defines `colors.ring-idle`, `colors.ring-submitted`, `colors.ring-observed`, `colors.ring-unconfirmed`, `colors.ring-offline` — **kebab-case, flat under `colors`**, per the `design-md-spec.md` rule ("Kebab-case keys" for `colors`). Every one of the 11 fails to resolve, including in the most-referenced places in the file (the ring state table at L204-208 and all three hero-flow climaxes). This directly contradicts `EXPERIENCE.md`'s own L11 promise that visual specs "are referenced here by token, never restated" — the tokens are referenced, but under names that don't exist. *Fix:* global replace `.` → `-` in the 11 refs. Note the collision risk: `colors.ring` is *also* a real component key (`components.ring.states`), so the dot form may have been an attempt to mirror `components.ring`; the fix is kebab on `colors`, and `components.ring` should be cited as `{components.ring.states}` if it needs to be.

- **[high]** **`{colors.const-plate}` does not exist — 2 references.** `DESIGN.md` L607 uses it as the utility shelf's material ("seated on the etched utility shelf (`{colors.const-plate}`, inset 50% recess)") and L627 uses it for the inline chip's LED glow ("9px amber LED with an 80% glow at `{colors.const-plate}`-family amber"). The frontmatter has `const-plate-brass`, `const-plate-brass-floor`, `const-plate-rim` and `const-plate-ink`, but no base `const-plate`. L607 is load-bearing: the utility shelf is a named surface in the hero layout and in `EXPERIENCE.md`'s degraded-network story. L627 is load-bearing: it is the amber glow on the honesty chip, the one element the whole "brass means this TV has a limit" rule depends on. *Fix:* add `const-plate: '#C9BDA2'` (or whichever of the four is correct — that value is itself currently uncommitted) and repoint both refs, or repoint them to the specific existing token and say which.

- **[high]** **`{colors.const-ink-faint}` does not exist — 1 reference.** `DESIGN.md` L653, in the **Disabled** key state: "glyph and micro-label drop to `{colors.const-ink-faint}` at roughly 38% alpha." There is no such token in frontmatter. The disabled treatment is load-bearing — it is the state that carries the explicit "Disabled caps keep their silhouette so the layout never shifts under the thumb" rule — and its ink currently resolves to nothing. `const-ink-faint` is also the only reference to a "faint" ink family; `const-micro-ink` and `const-etch-ink` fill adjacent roles but neither is named as the disabled ink. *Fix:* define `const-ink-faint` in frontmatter with a hex, or repoint to `const-micro-ink` and state the 38% alpha as the actual mechanism.

- **[high]** **The light-mode matrix is missing 4 of 16 Tier-2 tokens.** `DESIGN.md` L356-367 gives light-mode values for 12 tokens. Absent: **`text-on-accent`**, **`sem-success`**, **`sem-warning`**, **`sem-danger`**. The dark block has all 16 (L339-354). This breaks the pair's own repeated claim that all six themes pass "in dark and light" (L408, L671) and leaves rule 2 of the gate ("Every one of the five reaches ≥ 3:1 against `{colors.surface-base}` in its own mode") unrunnable for the semantic tier. `sem-success` and `sem-warning` are referenced in the body as load-bearing — TV-card status LEDs (L637) and the `sem-success` do/don't row (L673). *Fix:* add the four light rows, or state explicitly that light mode reuses the dark semantic values and is compliant by exception.

- **[medium]** **`{side-wall-color}` is a formula placeholder wearing a token's clothes.** `DESIGN.md` L500, inside the raised-formula CSS block: `box-shadow: 0 {N}px 0 {side-wall-color}, 0 {M}px {K}px rgba(0,0,0,{A});`. Per `design-md-spec.md`, `{path.to.token}` is *reserved* for frontmatter references, so an extractor will try to resolve `{side-wall-color}` and fail. `{N}`, `{M}`, `{K}`, `{A}` are obviously generic slots and read as such, but `{side-wall-color}` reads as a named token. *Fix:* change the block's placeholders to a non-brace form (`0 <N>px 0 <side-wall>`, or `<side-wall-color>` in angle brackets) so the entire formula block is unambiguously not token syntax.

---

## 3. Component coverage — **thin**

Both spines use component names consistently — no spelling drift anywhere, and every name in one file has a counterpart in the other. The failure is in coverage direction, and it runs both ways.

`DESIGN.md` frontmatter defines 12 component keys (`key-cap`, `key-cap-dark`, `ok-dome`, `dpad`, `ring`, `function-row`, `volume-rocker`, `tv-switcher`, `sheet`, `inline-chip`, `tv-card`, `offline-banner`). `EXPERIENCE.md` Component Patterns has 18 behavioral rows with real rules — not one-word descriptions — including the "A swipe that never crosses threshold emits nothing and says nothing" and "The stored token is never displayed, never validated by format, never logged" lines, which are exactly the kind of rule that makes the section worth having.

### Findings

- **[high]** **Eight behaviorally-specified components have no visual spec at all.** These rows exist in `EXPERIENCE.md` with real rules but no `components.*` token in `DESIGN.md` frontmatter: **Capability row** (L186), **Manual IP entry** (L187), **PIN / identity confirm** (L188), **Permission rationale card** (L189), **Transport controls** (L190), **Wake step** (L191), **Diagnostic readout** (L192), **Theme picker** (L193), plus **Session notification** (L194). A story-dev consumer told to build the theme picker, the permission rationale card, or the transport controls has behavior with no appearance — no size, no radius, no type role, no elevation. The permission rationale card is the most conspicuous: it is called "first-class" behavior with a denial path, and it is precisely the kind of surface whose absence of a visual spec gets improvised badly. *Fix:* add frontmatter entries for at minimum the permission rationale card, the theme picker and the transport controls; the notification can reasonably inherit the offline-banner treatment by reference.

- **[medium]** **Two `DESIGN.md` components have no behavioral row.** `key-cap-dark` is a top-level component key (L222-228) but is a *variant* of `key-cap`, not a component — key-press behavior is real and load-bearing (travel collapses 4px→1px, no scale, no fade, no bounce) but it is specified only as visual prose at L577, never as a rule in `EXPERIENCE.md`'s Component Patterns or Interaction Primitives. `ok-dome` likewise has no row; its behavior is folded into Big-pad's "Tap emits OK" and the D-pad row. *Fix:* fold `key-cap-dark` into `key-cap.variants` in frontmatter (it is already half-way there — `key-cap.variants: [bone, dark, orange]` at L221 names it as a variant *and* L222 gives it its own key), and add one OK-dome row or fold OK into the D-pad row by reference.

- **[medium]** **The Orange cap has two contradictory ink values.** `DESIGN.md` L575: "ink `{colors.const-cap-ink}` at 2a1c10-equivalent". `const-cap-ink` is `#3A2F22`; `2a1c10` is neither it nor any token. One property, two values, and the second one is a bare hex with no token — in a file whose stated rule (L673-678, L313) is that color goes through tokens. *Fix:* pick one. If the orange cap genuinely needs a warmer ink than the bone cap's, add `const-cap-ink-orange` and reference it.

---

## 4. State coverage — **strong**

This is the strongest section in the pair, and it is genuinely strong rather than nominally strong.

The ring state table (L202-208) gives all five states with signal, meaning, and color. The connection state hierarchy (L234-245) enumerates **eight** observable states — `Disconnected`, `Discovering`, `Connecting`, `PairingRequired`, `Connected`, `Asleep`, `IdentityChanged`, `Error` — each with where the user sees it and what they can do, and then L247-251 spends three bullets protecting exactly the distinctions that matter (`PairingRequired` vs `IdentityChanged`, `Asleep` vs offline, rejected-pairing-is-terminal). Degraded network (L257-263) covers five conditions including the SSDP-fails-but-unicast-works case. Session/service state (L269-274) and feature-session state (L280-284) close the loop, with permission-denied explicitly first-class on every surface including the hero.

Empty, cold-load, focus, error, offline, and permission-denied are all covered. The `unconfirmed` timeout rule (L216) is a good catch — *"the timings must be tuned so that `unconfirmed` is reached by timeout, never by assumption."*

### Findings

- **[low]** **Onboarding has no empty or failure state.** It is surface #1 and the first thing a new user sees, and it carries the network-isolation diagnosis (L259, L265) — the single most likely first-run failure. It gets copy ("what pairing needs, the one-broadcast-domain assumption") and a behavior ("Manual IP entry is the working path and stays on every surface"), but no enumerated state set: no empty-TV-set state, no discovery-found-nothing state, no permission-denied-at-onboarding state. *Fix:* three rows alongside the connection hierarchy.

---

## 5. Visual reference coverage — **thin**

`EXPERIENCE.md` L13 states the precedence rule correctly and once: *"Both spines win on conflict with any mock, wireframe, or import."* DESIGN.md L567 and L623 point across to `EXPERIENCE.md` for behavior, and L682 makes it a do/don't row. Cross-file discipline is good — I found no place where either spine restates the other's material.

The file inventory is where this fails.

### Findings

- **[medium]** **Four of six `.working/` source files are orphans.** `direction-tactile.html` (linked at DESIGN L13) and `color-themes-1.html` (linked at L333) are referenced with a stated purpose. **`direction-native.html`, `direction-noir.html`, `direction-signal.html` and `extract-spec-research.md` are referenced by neither spine.** The three `direction-*.html` files are almost certainly the *rejected* direction explorations — but a downstream reader or agent opening `.working/` has no way to tell a deliberately-rejected direction from an unreviewed one. *Fix:* one line in DESIGN's Brand & Style (or a short `## Sources` note) naming the three rejected boards as rejected, and pointing `extract-spec-research.md` at whatever it underpins.

- **[medium]** **The composition-reference pattern is absent, though the spine claims to implement it.** `experience-example-mobile.md` L28 carries `→ Composition reference: mockups/today-cold.html, mockups/composer.html. Spine wins on conflict.` — inline, at the section it illustrates. `EXPERIENCE.md` has no `→ Composition reference` line anywhere, and correspondingly there is no `mockups/`, no `wireframes/`, and an **empty** `imports/`. L13's precedence rule therefore governs zero artifacts. This is not wrong today — nothing exists to reference — but it means the rule is currently inert, and a story-dev consumer has no visual reference for any of the twelve surfaces. *Fix:* either compose two or three hero mockups and link them inline at IA, or state in L13 that no composition artifacts exist yet so the rule is future-facing. Either is defensible; silence is not.

- **[low]** **DESIGN.md L13's provenance comment sits inside the YAML frontmatter.** The `# ═══ TIER 1 ═══` block (L9-15) is 7 lines of comment at indent 0 inside a 299-line frontmatter. It parses fine and it is genuinely useful context, but it means 60% of the frontmatter's opening is prose. *Fix:* move the comment into the body above Brand & Style, or to the `## Brand & Style` section.

---

## 6. Bloat & overspecification — **adequate**

Surprisingly clean for a pair this size. The restraint is real and it is load-bearing restraint, not padding: `EXPERIENCE.md` bans toasts, tickers, spinners and badge counts (L299) and then never uses them; `DESIGN.md` forbids translucent offset shadows (L675) and never specifies one. The Do's-and-Don'ts table (L667-682) is 14 rows that each encode a decision rather than a platitude.

The editorial voice split the brief asks about is correctly assigned: `DESIGN.md` carries it (Brand & Style, L301-317, is genuinely good writing), `EXPERIENCE.md` does not — its Voice and Tone section is microcopy rules and copy tables, not brand prose, and it explicitly disclaims the job at L100 ("Brand personality lives in `DESIGN.md`. Do not restate it here").

- **[low]** **`spacing` carries 23 tokens where ~12 would do.** Eight numeric steps plus 15 named values (`gutter`, `hero-inset`, `topbar-h`, `statusbar-h`, `cap-gap`, `rocker-gap`, `key-row-gap`, `card-gap`, `sheet-pad-x`, `sheet-pad-bottom`, `etch-pitch`, `reference-width`), several of which duplicate each other (`gutter` and `hero-inset` are both 20px). *Fix:* collapse `gutter`/`hero-inset` to one; keep the rest — physical refinement values are justified by the "2px step exists solely for hairline offsets" rule at L457.

- **[low]** **Elevation tables restate component geometry.** The raised-formula table (L506-516) repeats sizes already in `components.*` frontmatter (key cap 66×58, OK dome 96, rocker 56×50). Given the spine's own rule that a table should not duplicate a token, this is mild duplication — though the elevation table's value-add is the wall/shadow *pairing* per object, which frontmatter does not carry. *Fix:* drop the size column, keep object → wall → contact-shadow.

---

## 7. Inheritance discipline — **thin**

`sources` resolves in both files (verified by path resolution, not by inspection). CAP names are verbatim from `spec-samsung-remote.md` — CAP-1 through CAP-8 all appear with the spec's own intent preserved. Glossary is consistent: "outcome honesty," "unconfirmed," "spines win," "the socket lives with the UI" all carry one meaning across both files.

Component names are identical across all sections in both files — I checked every name in every table and heading, and found no drift.

The token half of this section is where it fails, and both spines are implicated.

### Findings

- **[critical]** **`EXPERIENCE.md` token references resolve to `DESIGN.md` tokens by name: 0 of 22.** As detailed in §2: 11 `{theme.*}` refs point at a group that does not exist, 11 `{colors.ring.*}` refs use dot notation against kebab-case keys. `EXPERIENCE.md` L11 explicitly promises the opposite — that visual specs are "referenced here by token, never restated" — so this is a broken promise inside the file that makes it, not just an extraction failure. *Fix:* as §2. This is the highest-value single change in the pair.

- **[medium]** **4 of `DESIGN.md`'s own 131 token references do not resolve**: `{colors.const-plate}` ×2 (L607, L627), `{colors.const-ink-faint}` (L653), `{side-wall-color}` (L500). Note that `DESIGN.md`'s *internal* frontmatter references are all clean — every `{rounded.cap}`, `{colors.const-cap-side}`, `{typography.cap}` inside the `components.*` block resolves. The failures are all in body prose.

- **[low]** **`{appName}` is a live placeholder inside `DESIGN.md` frontmatter.** The `description` field (L3) reads "Visual identity contract for the {appName} Samsung Tizen TV remote." `EXPERIENCE.md` L12 explains this is deliberate (naming deferred to its own pass before store listing), and `name: UniRem` is the convention-carrier. Correct as documented — but a machine reading `description` gets a literal brace token. *Fix:* none required; noted so the next pass does not "fix" it by accident.

---

## 8. Shape fit — **strong**

Both files match the required section order exactly, with no reordering and no omissions.

**`DESIGN.md`** — canonical order, verified in sequence: Brand & Style (L301) → Colors (L319) → Typography (L412) → Layout & Spacing (L455) → Elevation & Depth (L493) → Shapes (L538) → Components (L565) → Do's and Don'ts (L665). All eight, in order. Frontmatter carries `name`, `description`, `status`, `sources`, `updated` plus the four token groups and `components`, matching `design-md-spec.md`.

**`EXPERIENCE.md`** — all eight required sections present and in order: Foundation (L15) → Information Architecture (L43) → Voice and Tone (L98) → Component Patterns (L171) → State Patterns (L196) → Interaction Primitives (L288) → Accessibility Floor (L301) → Key Flows (L343). The two extras (Inspiration & Anti-patterns L400, Responsive & Platform L422) are both present in the shape reference, so they are sanctioned, not bloat.

Sub-headings within Colors (two-tier system / six themes / token placement / ring / conflicts) and Components (14 subsections) are additions that earn their place.

- **[low]** **`EXPERIENCE.md` frontmatter omits `description`.** `DESIGN.md` has one (L3); `EXPERIENCE.md` goes straight from `name` to `status`. `design-md-spec.md` marks only `name` as required, and the EXPERIENCE shape reference also omits it — so this is conformant. *Fix:* optional; a one-liner would make the pair symmetric.

---

## Mechanical notes

**Frontmatter.** Both files parse. `DESIGN.md` frontmatter is 299 lines (L1-299, closing `---` at L299); top-level keys `name, description, status, sources, updated, colors, typography, rounded, spacing, components`; 244 leaf paths defined. `EXPERIENCE.md` frontmatter is 7 lines; `name, status, sources, updated`. `sources` in both resolves to `../spec-samsung-remote/spec-samsung-remote.md` (verified by filesystem resolution). `updated: 2026-10-22` in both, consistent with the dates in the resolved-conflict records.

**Broken cross-references — complete list (26).**

| Location | Reference | Defined as |
|---|---|---|
| EXPERIENCE L27, L29, L215, L303, L309 (11 refs) | `{theme.midnight-ember}` and 5 siblings | **nothing** — no `theme` group in frontmatter |
| EXPERIENCE L184, L204-208, L228, L238, L353, L354, L358 (11 refs) | `{colors.ring.idle}` etc. | `colors.ring-idle` (kebab, flat) |
| DESIGN L607, L627 | `{colors.const-plate}` | `const-plate-brass` / `-brass-floor` / `-rim` / `-ink` — no base token |
| DESIGN L653 | `{colors.const-ink-faint}` | **nothing** |
| DESIGN L500 | `{side-wall-color}` | **nothing** — formula slot, not a token |

All 62 other brace references in both files resolve correctly, including all 244-path frontmatter cross-refs inside the `components.*` block and all 60-odd `{typography.*}`, `{rounded.*}`, `{spacing.*}` and `{colors.const-*}` prose refs.

**Non-token brace placeholders (intentional, correctly excluded from the above):** `{appName}` (spine-wide), `{tvName}` `{modelLabel}` `{minutes}` `{attempts}` `{arg1}` `{feature}` (i18n role-named placeholders, per the rule at EXPERIENCE L38), and `{A} {K} {M} {N}` (CSS formula slots in DESIGN's elevation blocks). All are either documented conventions or formula slots.

**ASSUMPTION tags.** 17 total — **8** in `DESIGN.md` (L416, 442, 585, 593, 599, 633, 647, 663), **9** in `EXPERIENCE.md` (L40, 41, 89, 216, 276, 286, 292, 323, 439). All 8 in `DESIGN.md` use the explicit `*What would confirm it:*` formula and name a specific confirmer. In `EXPERIENCE.md`, four do (L216, 276, 286, 323); five state a confirmer in other words (L41 "the verification matrix has not yet returned"; L89 and L292 both "a usability pass"; L439 "nothing in the captured decisions addresses form factors"). Of those five, **two are unfalsifiable about their confirmer**: **L40** (i18n/English-only launch — "recorded here so a future pass has a seam to work from") names no evidence source that could close it, only an intention to look later; **L439** (whether a large-screen layout ships in v1 — "Nothing in the captured decisions addresses form factors above phone") names no confirmer at all and reads as a permanent open question rather than a tagged assumption. *Fix:* give L40 a confirmer (the store-listing locale plan it defers to) and either tag L439 against a decision-maker and date or move it to the spec's open questions.

**GAP tags.** 3, all in `DESIGN.md`: L407 (OK key color collision between `color-themes-1.html` and the tactile direction — resolved in favor of tactile, but still marked `[GAP]` rather than `[RESOLVED]`; **this looks like a tag-state slip, since the very next line records the decision**), L613 (thumb-arc has no captured visual), L643 (no banner render exists). The last two are honest and correctly paired with `[ASSUMPTION]` entries at L593 and L643.

**Name consistency.** Zero drift found. All 18 `EXPERIENCE.md` Component Patterns names have a recognizable `DESIGN.md` counterpart; all 12 `DESIGN.md` frontmatter component names appear in `EXPERIENCE.md` prose. All CAP-1..8 names match `spec-samsung-remote.md` verbatim. The only naming inconsistency of substance is the token-path separator (§2) and the Orange-cap ink (§3).

**Light-mode matrix completeness.** 16 Tier-2 tokens in the dark block (L339-354), 12 in light (L356-367). Missing from light: `text-on-accent`, `sem-success`, `sem-warning`, `sem-danger`.

**File inventory.** `.working/` holds 6 files; 2 linked (`direction-tactile.html`, `color-themes-1.html`), 4 orphaned. `mockups/` and `wireframes/` do not exist. `imports/` exists and is **empty**.