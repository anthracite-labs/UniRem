---
name: UniRem
status: draft
sources:
  - ../spec-samsung-remote/spec-samsung-remote.md
updated: 2026-10-22
---

# {appName} — Experience Spine

> The behavioral half of a two-spine UX contract. `DESIGN.md` owns how the product looks — every visual specification, color value, type ramp, radius, elevation and motion curve lives there and is referenced here by token, never restated. This file owns **how it works**: navigation, states, microcopy, interaction rules, accessibility behavior, and journeys.
> Frontmatter `name` keeps the initiative name by convention only. `{appName}` is the literal placeholder for the shipping product name in every user-visible string and every heading here — naming was deliberately deferred to its own pass before store listing.
> Both spines win on conflict with any mock, wireframe, or import.

## Foundation

Native Android, single-surface mobile app. Kotlin + **Jetpack Compose**. minSdk 26, targetSdk 36 (Play deadline 2026-08-31); the local-network permission becomes an enforced runtime gate for apps targeting API 37+, so the permission gate is built now, not later (Spike L).

Because Compose is named here, both spines inherit from it. `DESIGN.md` references or extends Compose defaults; **this file specifies only the behavioral delta.** Compose's own component behavior — how a `ModalBottomSheet` dismisses, how a `Text` field handles IME, what `contentDescription` plumbing exists — is not restated. Where a rule below would read as "Compose already does this," it is omitted.

The product is a remote control for Samsung Tizen TVs over the local network. Its moral core is **outcome honesty**: a successful socket write is not the TV having done the thing. Every behavior in this spine exists to make that distinction legible to a person in a dark room.

| Axis | Decision |
|---|---|
| Stakes | Consumer. Public Play Store. Every claim here is scrutinizable by a stranger. |
| Surface | One app, one hero, sheets off it. The TV side is not a client — except the CAP-7 Tizen receiver, which is a separate sideloaded app on the TV. |
| Lighting | **Dark-first.** Living-room lighting was a stated concern. Default theme `{themes.midnight-ember}`; a light treatment is a `DESIGN.md` token concern, not a behavioral one. |
| Motion | **Spring, not fade.** State changes carry weight and settle; fades are reserved for surfaces entering and leaving, never for state on the hot path. |
| Themes | Direction-constant skeleton + 6 user-switchable token themes (`{themes.midnight-ember}`, `{themes.tizen-blue}`, `{themes.graphite-mint}`, `{themes.warm-slate}`, `{themes.high-contrast-mono}`, `{themes.deep-teal}`). Themes swap color tokens only — never materials, geometry, type or motion. Switcher lives in Settings → Appearance. |
| Network | Local LAN only. Key, text and pointer traffic never leaves the LAN; there is no cloud fallback in the key path, so a LAN failure is explained, never routed around. |
| Naming | `{appName}` everywhere user-visible. No string in this app concatenates fragments at runtime. |

**i18n is a recorded deferral, not an omission.** The consumer stakes of a public Play Store listing make localization a deliberate deferral by the user. It is recorded here so a future pass has a seam to work from, and that seam is binding on authoring today:

- Every user-visible string is a **complete sentence or a complete noun phrase**, authored as one unit.
- **Copy must never be assembled by runtime string concatenation.** `"Sent to " + tvName` is forbidden. A parameterized whole-string form is correct: `"Sent to {tvName}."` carries its own word order, and word order is what changes when a language does.
- No sentence in this app depends on English word order, on a leading verb, on capitalization carrying meaning, or on a fragment standing alone once a noun is substituted.
- Placeholders are always named for their **role**, not their position: `{tvName}`, `{modelLabel}`, `{minutes}`, `{attempts}` — never `{arg1}`.

`[ASSUMPTION]` Launch ships English-only, with the deferral recorded — the store-listing locale plan, which was deferred alongside naming.
`[ASSUMPTION]` Key repeat and long-press are not part of the hero key hot path in v1 — CAP-1's MVP key set from Spike A, which the verification matrix has not yet returned.

## Information Architecture

**No bottom navigation.** There is no tab bar, no drawer, and no bottom-scrolling destination strip. The app opens onto the remote; everything else is a sheet off it.

### Surfaces (12 — closed, all covered, none added or merged)

| # | Surface | Reached from | Purpose | Capabilities |
|---|---|---|---|---|
| 1 | Onboarding | First launch after install | Sets expectations before anything can fail: what pairing needs, the one-broadcast-domain assumption, what the app cannot promise | CAP-1, CAP-8 |
| 2 | Pairing | Onboarding, TVs management, re-pair trigger | Discovery and manual IP side by side, TV Allow prompt, TOFU pin confirmation | CAP-8 |
| 3 | **Remote hero** | App open (warm), back from any sheet | The pad, the outcome ring, the sheet affordances | CAP-1 |
| 4 | Text sheet | Hero | Phone-typed text into the TV's focused native field, as an explicit send | CAP-2 |
| 5 | Voice sheet | Hero | Phone mic → mapped keys or short dictation | CAP-4 |
| 6 | Touchpad | Hero | Pointer control where the cursor probe passes | CAP-3 |
| 7 | Cast sheet | Hero | Item selection, phone-hosted serving, transport control | CAP-6 |
| 8 | Mirror sheet | Hero | Per-session consent, receiver status, session state | CAP-7 |
| 9 | Wake checklist | Hero | TV-side "Power On with Mobile" setup, then Wake as its own action | CAP-5 |
| 10 | TVs management | Top bar ▾ | Add, remove, switch, per-TV record, manual IP, re-pair | CAP-8 |
| 11 | Settings + diagnostics | From TVs management or hero overflow | Appearance/themes, permissions, network isolation diagnosis, capability readout | All |
| 12 | Tizen receiver companion | Mirror sheet | Install steps for the TV-side receiver; the honest reason mirroring may be unavailable | CAP-7 |

Every CAP-1..8 lands on a surface. Closure verified — a new surface is a change to this table, not an addition at the end of it.

**Coverage of Key Flows is not yet total, and this document does not claim it is.** Eight of the twelve surfaces are reached by a journey in **Key Flows**: Kai covers the remote hero and TV switching (CAP-1, CAP-8), Dana covers the text sheet (CAP-2), Maya covers cast (CAP-6), and the secondary flows cover wake (CAP-5), mirror (CAP-7), voice (CAP-4), and honest labelling for 2014–15 H/J sets (CAP-8). Four surfaces are specified here — onboarding, touchpad (CAP-3), settings + diagnostics, and the Tizen receiver companion — but do not yet have a protagonist journey. **This is an open item, recorded rather than papered over**, because an unclaimed journey is a missing design decision, not a formatting gap. Each is specified well enough to build; naming its protagonist is the open part.

### Navigation rules

- **Cold start, first launch after install → onboarding.** Nothing else is reachable first-run; there is no state in which the app can show an empty remote.
- **Cold start, later launches → remote hero** for the last-used TV, and reconnect runs in the background **with its state visible** in the ring and the top-bar TV line. The hero is usable before the connection resolves; keys press, and the ring says which outcome each press earned.
- **The TV switcher is the top bar ▾.** It opens **TVs management**, which is also the add/remove/re-pair surface. The targeted TV is always named in the top bar in plain words.
- **Six sheets, all off the hero**: text, voice, touchpad, cast, mirror, wake. A sheet is a temporary surface over the hero, not a destination. Closing it returns to the hero with TV state, pad layout and scroll position intact.
- Back closes the topmost sheet, then leaves the app. Back never disconnects a live session silently — a running cast, mirror or voice session surfaces its notification and its end action first.
- The remote socket lives **only while the remote UI is active** (spec constraint). Backgrounding the hero is therefore a real, observable disconnection, not a background service quietly holding the channel. See **State Patterns**.

### Hero layout — dual-mode and user-switchable

Two layouts, chosen by the user, **persisted per TV record**, so a living-room set can be big-pad while a bedroom set stays thumb-arc.

| | **thumb-arc** | **big-pad** |
|---|---|---|
| Primary | D-pad in the bottom third, within one-thumb reach | One large surface filling the hero |
| Swipe | n/a | A swipe past threshold emits a **D-pad key** (up / down / left / right) |
| Tap | n/a | Tap emits **OK** |
| Secondary row | A row of secondary keys above the D-pad | Revealed as small brass-labeled key chips |
| Volume | Edge rocker, left and right edges | In the secondary set |

**The distinction that must not be blurred: big-pad is key *emulation*, not the CAP-3 pointer touchpad.** A swipe in big-pad mode sends the same key frame the D-pad sends, and can never produce a pointer move. The touchpad is a separate sheet, is probe-gated per model, and is hidden entirely on models that fail the cursor probe. Conflating the two would imply a universal pointer where the evidence says otherwise.

`[ASSUMPTION]` Big-pad swipe threshold and direction→key mapping — a usability pass on the two thumb-arc/big-pad layouts, since no gesture geometry was captured.

### Multi-TV

- **Explicit target selection is required** whenever more than one TV is stored. There is no automatic "the TV in this room" heuristic and no broadcast to "all TVs".
- One persisted record per TV, holding: identity, MAC, last known IP, model label, capabilities, pairing state, pin, health.
- Reconnect uses backoff; records persist across app restart; two TVs pairing and surviving restart is a CAP-8 success condition, not a convenience.
- Stored per-TV data is what makes the outcome contract coherent: the ring's *observed* verdict for a key is scoped to a specific TV, not to the app.

## Voice and Tone

Brand personality lives in `DESIGN.md` → **Brand & Style**. Do not restate it here. This section is **microcopy**: the actual words, and the rules that produce them.

Warm, unhurried, honest-before-cheerful. A well-made tool that respects your living room. It would rather plainly say the TV stopped answering than perform a success it cannot prove.

### Rules

1. **Say what happened, then one thing to do about it.** Never a diagnosis without an action; never an action without a plain statement of what happened.
2. **One action per message.** The offline banner offers Retry and nothing else.
3. **Never name the protocol.** No port numbers, no `ms.channel.*` event names, no SPKI, no TLS jargon in user-facing text. This is a consumer app, not a diagnostics console.
4. **No error codes in user-facing text.** The internal code is logged, never shown.
5. **Never claim more than was observed.** "Sent" is the ceiling when a write went out. Confirmation language is reserved for a confirmed effect.
6. **Second person, present tense, short sentences.** No exclamation marks. No cheer.
7. **Name the TV the way a person would.** "Living Room TV," not `QN90x` or a MAC address. The model label lives in TVs management and diagnostics.

### The TV state, in one plain phrase

The entire connection situation is legible from one line. It is never a spinner, never a percentage, never a stack of status chips.

| Situation | Say |
|---|---|
| Nothing stored yet | `Looking for your TVs.` |
| Searching the network | `Looking for your TVs.` |
| Found, opening the channel | `Connecting to Living Room TV.` |
| Waiting on the user at the TV | `Waiting for you to allow this TV.` |
| Paired and answering | `Living Room TV is ready.` |
| Paired, in standby | `Living Room TV is asleep.` |
| Lost the channel | `Living Room TV isn't answering.` |
| Same TV, different identity | `This TV changed. Pair it again.` |

### Capability limits — the honest labels

A capability is either demonstrated for that model or it is not offered. Nothing is "probably fine."

| Situation | Say |
|---|---|
| Pointer fails the cursor probe | `Pointer input isn't available on this TV.` + one line on why: `Some TV screens only respond to the arrow keys.` |
| Wake not recorded as passing for this model/network | `Wake isn't confirmed for this TV yet.` + `It needs to be tested on your TV and network before we can promise it.` |
| A key the TV will not take | `This TV doesn't take that key.` |
| Ladder feature not validated | `This TV hasn't been tested for {feature} yet.` |
| **2014–15 H/J series** | `2014–15 H/J series — best effort, not supported.` + `Your TV is on a different, older system. Some things will work. We can't tell you which.` |

The H/J label appears in the product and the UI whenever that model is targeted, and such a set is **never counted toward a compatibility claim**. It is not a warning banner that appears once and is forgotten — it is a standing property of that TV's record, visible in TVs management and in the top-bar TV line.

### The three outcomes, in words

The ring is visual; these are the words that go with it — to TalkBack, to notifications, and anywhere the outcome has to be said rather than shown.

| Outcome | Say |
|---|---|
| Submitted | `Sent. Your TV hasn't confirmed it yet.` |
| Observed | `Your TV did it.` |
| Unconfirmed | `Sent, but your TV hasn't answered. This may not have worked.` |

Note the third one. "Unconfirmed" is not a softer "unconfirmed" — it is a distinct verdict with its own sentence, because it means we sent something and have no idea whether it landed. Collapsing it into either success or failure is the failure mode this product exists to avoid.

### Banned strings

| Do | Don't |
|---|---|
| `Sent to Living Room TV.` | `Successfully sent!` |
| `Your TV did it.` | `Command executed ✓` |
| `Sent, but your TV hasn't answered.` | `Timeout error (E-403)` |
| `Living Room TV isn't answering.` | `ms.channel.unauthorized` |
| `Waiting for you to allow this TV.` | `Pairing handshake in progress…` |
| `This TV changed. Pair it again.` | `Certificate mismatch (SPKI)` — surfaced silently, re-paired silently |
| `2014–15 H/J series — best effort, not supported.` | `Supported` (without qualification) |
| `Your TV doesn't take that key.` | *(nothing)* |
| `It needs to be tested on your TV and network first.` | `Wake isn't supported.` — when the honest truth is "we haven't measured it" |

Cheerful fake-success language is banned outright. So is `universal` as a user-facing claim, and so is any percentage ("works with 90% of TVs") — no fleet-wide figures exist, and the verification matrix forbids them until measurements do.

## Component Patterns

Behavioral. Visual specs — sizes, materials, key-cap geometry, brass plates, textures — live in `DESIGN.md` → **Components**.

| Component | Use | Behavioral rules |
|---|---|---|
| Outcome ring | Wraps the D-pad in thumb-arc and the pad in big-pad | Sole state channel on the key hot path. Carries all five states. Never overlays the key it wraps. |
| D-pad | thumb-arc | Four discrete keys plus OK. Each is independently labeled and independently announced. No key-combination chords. |
| Big-pad | big-pad | One surface. Swipe past threshold → one D-pad key. Tap → OK. A swipe that never crosses threshold emits nothing and says nothing. Exposes discrete key actions to accessibility services so switch access does not require performing a swipe. |
| Secondary key row | thumb-arc, and revealed in big-pad | Back, home, mute, and the rest of the MVP key set. Ordering is stable per layout. |
| Edge volume rocker | thumb-arc | Discrete volume steps, not a continuous analog drag. Each step that registers produces its own ring pulse. Key caps are individually reachable and separately labeled. |
| Sheet | All six | Opens over the hero, keeps the hero's TV state behind it, dismisses on drag, back gesture, or back button. Nothing is committed on drag-dismiss. |
| TV switcher ▾ | Top bar | Opens TVs management. The targeted TV is named in the bar at all times. |
| Offline banner | Hero | Exactly one. Explains, offers exactly one action. Only place `{colors.ring-offline}` red appears. |
| Unsupported chip | Hero | One-time, inline, below the pad. Never a modal, never a dialog, never repeated for the same key on the same TV record. |
| Capability row | TVs management, sheets | A capability the model lacks is either hidden or shown disabled-with-a-reason — never shown enabled and failing. |
| Manual IP entry | Onboarding, pairing, TVs management, diagnostics | Always offered beside discovery, on every surface where a TV is added or repaired. |
| PIN / identity confirm | Pairing | The pin capture is confirmed in the UI at first pairing. The stored token is never displayed, never validated by format, never logged. |
| Permission rationale card | Wherever a permission is needed | States what the permission enables, in the product's own terms, immediately before the system dialog. Denial is handled as a first-class state with a next action — never a dead end. |
| Transport controls | Cast sheet | Play, pause, stop, and seek only where the renderer advertises it. Stop ends the session and stops serving. |
| Wake step | Wake checklist | One TV-side instruction per step with its menu path, and a checkbox the user confirms — never a claim that the app detected the setting changed. |
| Diagnostic readout | Settings → Diagnostics | Plain-language findings, not raw counters: "No TVs answered the network search, but Living Room TV answered on its saved address. Your network likely isolates devices from each other." |
| Theme picker | Settings → Appearance | Six themes. Swapping changes color tokens only — geometry, type and motion do not move. |
| Session notification | Cast, mirror, voice | Names the session and the TV; carries exactly one action, ending the session. Never posted for key use. |

## State Patterns

This is the center of the product. The socket write is not the outcome; the ring is where the difference between "sent" and "done" is made visible.

### The ambient outcome ring

| State | Signal | Meaning |
|---|---|---|
| **idle** | Dim ring, `{colors.ring-idle}` | Nothing submitted. This is the resting state and it is unremarkable. |
| **submitted** | Pulse, `{colors.ring-submitted}` | The socket write went out. **The TV has not confirmed anything.** |
| **observed** | Solid settle, `{colors.ring-observed}` | The on-TV probe confirmed the effect. **The only true success.** |
| **unconfirmed** | Amber **dashed** hold, `{colors.ring-unconfirmed}` | Sent, no rejection, no confirmation either. Say exactly that. |
| **offline** | Red ring, `{colors.ring-offline}` + keys dimmed to 45% + one banner | The TV stopped answering. |

Rules that make the ring the whole story on the hot path:

- **No toasts. No tickers. No transient anything on the key hot path.** State lives in the ring or it does not exist. A press produces exactly one ring transition and nothing else on screen.
- **Ring state is scoped to the targeted TV.** Switching targets resets the ring; a verdict about the bedroom TV is never displayed as the state of the living-room TV.
- **The `submitted` → `observed` distinction is never collapsed for speed.** A key that cannot be probed lands in `unconfirmed`, not `observed` and not a failure.
- Every theme must keep all five states mutually distinguishable — contrast and colorblind-safe. `{themes.high-contrast-mono}` is held to the same contract as `{themes.midnight-ember}`.
- `[ASSUMPTION]` Ring dwell timings — pulse duration, how long `observed` settles before returning to `idle`, and how long before `submitted` resolves to `unconfirmed` or `offline` — the measured key-latency figures from Spike A, which the verification matrix explicitly has not yet produced. Until measured, the timings must be tuned so that `unconfirmed` is reached by *timeout*, never by assumption.

### Unsupported keys

One-time inline chip, below the pad. It appears once per key per TV record, states the limitation in a plain phrase (`This TV doesn't take that key.`), and does not repeat. **Never a modal, never a dialog, never a toast** — a modal on the key hot path would block the next press of a remote the user is holding one-handed. The chip is stored per key per TV record, so a key that never worked on that set never nags again.

### Offline

The loudest moment the product allows itself, and it is still restrained:

- Exactly **one** banner, explaining the situation and offering exactly **one** next action — Retry.
- No spinner. No error code. No retry-count. No auto-retry storm, no modal takeover, no "try again?" dialog stack.
- Keys dim to 45%; the ring is `{colors.ring-offline}`. **Red appears nowhere else on screen** while offline.
- Pressing a key while offline does not queue it silently and does not report it as sent. It leaves the ring exactly where it was and the banner exactly where it was.
- Retry either restores the connection (ring returns to `idle` through a visible state) or re-states the offline situation in the same single banner.

### Connection state hierarchy

Eight observable states. Each is surfaced; none is ever collapsed into "looks fine."

| State | Where the user sees it | What they can do |
|---|---|---|
| `Disconnected` | Ring at `{colors.ring-idle}` + the TV-state phrase `Living Room TV isn't answering.` | Retry, open TVs management |
| `Discovering` | The same TV-state phrase: `Looking for your TVs.` Discovery is silent about TV state and only reports what it can support — manual IP is offered throughout | Wait, enter an IP manually |
| `Connecting` | `Connecting to Living Room TV.` The ring stays `idle` — nothing has been submitted | Wait, back out |
| `PairingRequired` | A blocking-but-dismissible sheet naming the TV, plus the state phrase `Waiting for you to allow this TV.` | Answer the TV's Allow/Deny prompt with the physical remote, then Retry |
| `Connected` | `Living Room TV is ready.` Ring returns to `idle`, keys at full presence | Use the remote |
| `Asleep` | `Living Room TV is asleep.` **Distinct from offline.** The TV powered itself down; that is expected, not a failure | Wake — from the wake checklist sheet, which knows the stored MAC |
| `IdentityChanged` | `This TV changed. Pair it again.` **Distinct from `PairingRequired`.** The pin no longer matches, so the stored token is *not* sent | Re-pair, with the pin mismatch stated in plain words |
| `Error` | The offline banner, with the specific reason in words rather than a code | Retry, then diagnostics |

Three distinctions this table exists to protect:

- **`PairingRequired` vs `IdentityChanged`.** Both need the user at the TV, but they are different facts: one is a first connection or a rejected token, the other is a certificate change on a TV that was already trusted. **Re-pairing is never silent.** A pin mismatch is thrown before the token is sent — the app never sends a credential to an identity it cannot vouch for, and it never quietly re-pairs to make the error go away.
- **`Asleep` vs offline.** The socket drops as a TV enters standby; that is normal behavior, not a fault. Treating it as failure would put a red banner in front of a user who simply turned the TV off. Send power, see `Asleep`, offer Wake.
- **Rejected pairing is terminal for the attempt.** Denied or timed-out at the TV's Allow prompt returns to a state that says so and offers a re-pair. There is no silent retry, and no retry storm against the TV.

### Degraded network

Discovery and wake assume one broadcast domain. The app must degrade honestly rather than appear broken.

| Condition | What breaks | Behavior |
|---|---|---|
| No SSDP responses, but a manual IP answers on 8001 | Discovery only | Diagnosis in onboarding diagnostics, in plain words: the network is separating devices from each other. **Manual IP entry is the working path and stays on every surface.** |
| Manual IP answering from another subnet | Nothing, when the router routes and the firewall allows | Works. Unicast control can cross subnets where multicast cannot. |
| Wake across routers | Wake | No wake claim. `Wake isn't confirmed on this network.` + the router setting that would be required (directed-broadcast forwarding), described in words a person can act on. |
| Multicast lock not held | Discovery reliability | Acquisition is treated as part of discovery, not an edge case; failures during discovery are never reported as "no TVs exist." |
| Stale discovery entries / ARP expiry | Unicast to a remembered IP | Expired entries are discarded; saved IPs and manual entry remain as the fallback. |

Onboarding diagnostics explains the isolation case in plain language and offers manual IP. It never claims a cloud route exists — there is none for keys.

### Sessions, services and notifications

- The remote socket lives **only while the remote UI is active**. There is **no always-on foreground service for ordinary keys**. Backgrounding the hero genuinely disconnects, and the UI says so on return rather than pretending continuity.
- Foreground services exist **only** for user-started **cast**, **mirror**, and **voice** sessions, each with its matching type. Nothing else earns one.
- Each session posts an ongoing notification that names the session and the TV and carries exactly one action — **end the session**. Ending the session stops serving and releases the channel.
- Mirror additionally carries the mediaProjection indicator in the status bar; that indicator is not optional and is not cosmetic.
- Session notifications survive screen-off, which is what makes casting and mirroring usable while the phone is face-down.
- **No notification, ever, for a key press.** Outcome lives in the ring or nowhere.

`[ASSUMPTION]` What a session does when notification permission is denied — whether the session still starts with a suppressed notification, or is blocked with an explanation. Android's own behavior differs across versions; confirm against targetSdk 36 behavior and decide whether ending the session must then happen in-app only.

### Feature-session states

- **Text (CAP-2).** Focus state is first-class and is *unreliable at the source*: the TV's IME focus events are described as occasional or inconsistent, so focus is a hint, never a gate. **A manual Type action is always available**, whether or not the TV believes a field is focused. Send is explicit with send and cancel; cancel discards the typed string with no side effect. Arrival is judged by glyphs appearing in the focused field — never by the write succeeding.
- **Cast (CAP-6).** Selecting → the TV fetching the URL → rendering with a responsive transport → playing. The phone is the control point and the TV fetches. Serving is bounded to the selected items with unguessable IDs, stops at session end, and survives screen-off. HEVC, subtitles, seek and long files follow the per-model ladder, and anything not validated for the model is not offered as working.
- **Mirror (CAP-7).** Receiver absent → say so and offer the companion install steps. Consent needed → ask, every session, because consent is single-use. Live → the session notification and status-bar indicator. Mirroring without a TV-side receiver is not offered, not attempted, and not implied.
- **Voice (CAP-4).** Phone-side speech-to-text only. Requested inside the user-started voice flow, never at first launch. Latency and accuracy are unmeasured, so the UI shows that it is listening and what it heard — it never claims an instant response.
- **Permissions.** Local-network, `RECORD_AUDIO`, `POST_NOTIFICATIONS`, and MediaProjection are all requested at the moment of use. Permission-denied is a first-class state with a plain explanation and a next action, on every surface where it can occur — including the hero.

`[ASSUMPTION]` The cast ladder follows the pointer's hidden-with-a-plain-explanation rule for HEVC, subtitles and seek — the spec mandates that treatment explicitly only for the pointer (CAP-3); confirm whether cast ladder rows are hidden or shown-disabled on unvalidated models.

## Interaction Primitives

- **Tap is press.** One discrete press is one key frame and one ring transition. On the hero there are no gestures that mean something other than a key.
- **Reach is a design constraint.** thumb-arc places the D-pad in the bottom third so a whole session can be driven one-handed without regripping. Reach zones do not change between devices or orientations.
- **Big-pad swipe means a key.** Direction of swipe maps to a D-pad key past a threshold; tap means OK. It is not a pointer, not a cursor, and not a drag. `[ASSUMPTION]` threshold — a usability pass, no gesture geometry was captured.
- **Volume steps, it does not glide.** Each step that registers gets its own ring pulse, so a held rocker reads as a sequence of honest presses rather than one smooth claim.
- **Sheets dismiss cleanly**: drag to dismiss, back to dismiss, nothing committed on dismissal. The hero behind never changes state because a sheet closed.
- **Manual IP is always present** wherever a TV is added or repaired. Discovery failing is never the end of the road.
- **Every destructive or session-ending action is explicit**: stop casting, end mirroring, end voice, forget this TV.
- **Pre-flight before the network.** Where a feature has a setup requirement on the TV itself (the wake setting is the only current case), the checklist is shown before the action is offered, not after it fails.

**Banned on the key hot path and everywhere else:** toasts, tickers, snackbars, spinners, badge counts, streaks, celebratory animation, auto-advancing wizards, carousels, confetti, badge-shaped praise, and any control that appears and disappears on its own. **Banned as claims:** `universal`, any fleet-wide percentage, any latency figure ("instant," "near-zero"), and any statement that a socket write means the TV acted.

## Accessibility Floor

Behavioral only. Contrast values, focus-ring colors and type sizes live in `DESIGN.md`; the themes that carry them include `{themes.high-contrast-mono}`, which **is** the accessibility theme and is a first-class option in Settings → Appearance, not a hidden debug build.

**Colorblind-safety is a stated user constraint, and the ring is where it is tested.**

- All five ring states must remain distinguishable by a **non-color signal** as well as hue. Each state carries a distinct differentiator: `idle` dimmest, unlit · `submitted` a visible pulse · `observed` a **solid, complete** ring · `unconfirmed` a **dashed** ring of the same weight · `offline` a solid red ring plus the dimmed keys and the banner.
- **Decided 2026-10-22 (user):** `unconfirmed` is distinguished from `observed` by **stroke pattern — dashed vs. solid**, not by a breathing blink. The two states differ by hue alone in every source, and hue alone is not a channel: they collapse to CR 1.16 under deuteranopia in Midnight Ember and CR 1.03 in Warm Slate. Stroke pattern survives greyscale, all three CVD simulations, and reduced motion. Blink remains available as a one-line change if the pairing proves too subtle in testing.
- This holds across **all six themes in both dark and light**, including `{themes.high-contrast-mono}`, where hue is doing no work at all and the static forms carry the whole contract. The ring renders only inside `{colors.const-groove}`, which is direction-constant — so light mode changes the surrounding surface, never the seat.
- The static differentiators must survive reduced motion, because reduced motion removes the pulse that is `submitted`'s signature; `submitted` then relies on the single swell completing and extinguishing, which is itself the signal.

**TalkBack.**

- Every key is named the way a person would name it on a remote: `Up`, `Down`, `Left`, `Right`, `OK`, `Back`, `Home`, `Mute`, `Volume up`, `Volume down`. Not "D-pad up," not "keycode 40."
- The ring announces the **outcome in words** on transition — `Sent. Your TV hasn't confirmed it yet.`, `Your TV did it.`, `Sent, but your TV hasn't answered.` — and only on a *transition*, never per key press, so a burst of navigation does not become a torrent of announcements.
- The D-pad is exposed as a labeled group whose state is up to date, so a user who swipes through the group hears where the TV is, not a bare list of labels.
- Every sheet is reachable and dismissible without sight of the sheet edge.

**Touch and reach.**

- Every interactive target is at least 48dp, **including the edge volume rocker and the big-pad threshold regions** — big-pad's large surface is not an excuse for small internal targets.
- A whole session must be operable one-handed without regripping: key placement follows reach, and the sheets open from the thumb's side of the screen.
- Long-press is reserved for system text selection inside the text sheet; on the hero it means nothing. `[ASSUMPTION]` The launch voice command set is the 1:1 high-confidence key set (power/nav/system keys, volume/channel/digits, transport and colour keys, GUIDE/INFO/TOOLS), with long-press and pointer-path commands excluded — the research flags "user picks the launch set" as still open, and the memlog does not close it. Confirm: the Spike V command-set agreement.

**Keyboard and switch access.**

- Every sheet is reachable and operable with a hardware keyboard and with switch scanning. Nothing is drag-only.
- big-pad's swipe-to-key mapping is exposed to accessibility services as **discrete, individually labeled key actions**, so switch access never depends on being able to produce a swipe. This is a hard requirement of the layout, not an enhancement.
- Volume adjustment has discrete `−` / `+` equivalents available to assistive tech, so the rocker is not the only route.

**Reduced motion.**

- Honored system-wide. The ring's `submitted` pulse resolves to an instantaneous state change, and `submitted` then depends entirely on its static form to stay distinguishable.
- Spring scale animations are replaced by a direct state step. Sheet slide-in becomes a plain appearance. Nothing disappears on a timer.

**Screen off, dark room, time.**

- Dark-first throughout; no light-emitting flashes, no white flashes, no full-screen brightness jumps for a state change.
- A background event never wakes the screen. Ending a session is possible from the lock screen via the session notification's single action.
- Nothing is time-limited without saying so: the TV's Allow/Deny window shows a plain countdown, and when it expires the app explains what happened and offers a re-pair rather than silently failing.
- Type scaling is honored through `{typography.*}` tokens. No key label truncates, no control collapses, and the ring and sheets stay usable at the largest system font setting.

## Key Flows

Three hero journeys, then four compact secondaries. Capability names mirror `spec-samsung-remote.md` verbatim.

### Flow 1 — Kai, the lost remote (CAP-1, CAP-8, outcome honesty)

Kai cannot find the physical remote and has three of them in a drawer somewhere. He opens {appName}. He does not want a tutorial; he wants the TV to move and for the app to be straight with him about whether it did.

1. {appName} opens on the **remote hero**, not a splash, not a tour — background reconnect is already running and the top bar says `Connecting to Living Room TV.`
2. The ring sits at `idle`, dim. Nothing has been submitted, so nothing is claimed.
3. Kai presses `Volume up`. The ring **pulses** — `{colors.ring-submitted}`. The volume rocker steps. On the TV, the volume overlay appears.
4. The on-TV probe confirms the response. The ring **settles solid** — `{colors.ring-observed}` — and returns to `idle`.
5. Kai keeps navigating. Each press is one pulse, one confirmation, one return to rest. The screen never fills with anything.
6. He reaches for the one thing the app cannot promise: he presses power to end his night. The TV enters standby, the socket drops — and the app says `Living Room TV is asleep.` **Not** a failure, **not** a red banner. The wake sheet offers Wake, which knows the stored MAC.
7. **Climax:** Kai's neighbour asks whether it worked. Kai looks at the app, which says `Your TV did it.` — and the app is telling the truth, because the last time he pressed something the ring had gone **amber**, not green: `Sent, but your TV hasn't answered. This may not have worked.` He trusts the app precisely because it once admitted it did not know. That admission is the product.
8. Much later the router reboots. The next key press puts the ring at `{colors.ring-offline}`, dims the keys to 45%, and raises exactly one banner: `Living Room TV isn't answering.` with one action, **Retry**. No spinner, no code, no red anywhere else. Retry restores it and the ring returns to `idle`.

Failure branches: unsupported key → one-time inline chip, never a modal. Unanswered send → `unconfirmed`, held, then resolved. Multi-TV → Kai's flat has two sets, so the top-bar ▾ names the target and switching is explicit, never guessed.

### Flow 2 — Dana, the password ordeal (CAP-2, text entry)

Dana is signing into a service on a TV keyboard field she has to lean across the room to read. The physical remote is in a drawer. She is holding her phone at arm's length and would rather not lean over.

1. She opens the **text sheet** from the hero — a sheet, not a new app.
2. The phone keyboard is up and empty. She types on the phone; she does not type into the TV, one character at a time, through a key-repeat loop.
3. The TV's IME focus events are unreliable at the source, so **focus state is shown as a hint, never as a gate** — and the manual Type action is available regardless. The focus line reads in plain words when the TV reports it and stays neutral when it does not.
4. She taps **Send**. This is explicit. There is no keystroke stream, and **Cancel** discards the string with no side effect on the TV.
5. The ring pulses — sent. Whether it worked is a separate question.
6. **Climax:** the code appears in the TV's field. The ring **settles solid** — observed — and Dana never leans over the room. The glyphs on the TV screen are the proof; the socket write was only ever the delivery, never the promise.

Failure branches: she tries the same flow on an app-drawn search grid, where the TV may never raise a native field at all. The ring goes **amber**: `Sent, but your TV hasn't answered.` Then the honest limit appears — that kind of screen may reject typed text entirely. The app does not market this as typing into Netflix or YouTube search, and it does not silently do nothing. If the write never arrives, the sheet says so and offers the field-bound route again, not a retry that will fail identically.

### Flow 3 — Maya, the birthday cast (CAP-6)

Maya is casting a birthday video to the living-room TV while her phone keeps doing other things. She will not stand there holding it.

1. She taps **Cast** on the hero; the cast sheet opens.
2. She picks a video with the system photo picker — no broad media permission is requested, and only what she selected is ever served.
3. The TV is discovered as a renderer; the cast sheet names it and shows what it can do. Nothing is assumed — a TV's internal codec table is not a DLNA guarantee, so what the renderer advertises is what appears.
4. **The phone becomes the control point and the TV fetches.** The TV receives an address, never a phone-local file reference. Playback starts and the TV renders.
5. Transport controls live on the cast sheet, and only where the renderer advertises them. Features not validated for this model — HEVC, subtitles, seek on long files — are not offered as working.
6. Maya puts her phone face-down. A **user-started** session continues, because an ongoing notification is keeping it alive, with exactly one action: **end the session**.
7. **Climax:** the video is playing on the TV, Maya's phone screen has gone dark, and the only thing left on her phone is a notification she can end the session from without unlocking. The cast was genuinely hers to hand off — and the session cannot outlive her attention, because ending it is one tap from the notification.
8. She taps **Stop**. Serving stops, the channel is released, the notification clears.

Failure branches: the TV will not fetch → the sheet says so in words, no code. A long file that will not seek → seek is not offered for that renderer. Screen-off never ends the session mid-playback, because the session is a user-started service, not a background one.

### Secondary flows

**Wake (CAP-5) — Omar, evenings.** He opens the wake sheet and gets a **checklist**, not a button: the TV-side "Power On with Mobile" setting, with the menu path for his model year, and a checkbox **he** confirms — the app never claims it detected the change. With the MAC stored at pairing, **Wake** is offered as its own action, separate from Power, in the wake sheet and nowhere on the hero — power-off and wake are two actions, never a toggle. The ring reports success **only when the TV answers on 8001/8002**; a magic packet that leaves the phone is `submitted`, and if the TV does not come up the honest answer is that it did not confirm. On a network where broadcast cannot reach, there is no wake claim at all: `Wake isn't confirmed on this network.` — plus the router setting that would change that.

**Mirror (CAP-7) — Priya, showing photos to family.** The mirror sheet states plainly whether the TV-side receiver is installed; if not, it offers the **companion** steps and stops there, because mirroring without a receiver is not something this app can fake. Consent is requested **every** session — it is single-use, and reuse would fail. A user-started session starts, the ongoing notification and status-bar indicator appear, and the phone screen stays on with its own battery cost, which is why the app never claims mirroring is free and never claims it is fast: those numbers are unmeasured until the spikes record them.

**Voice (CAP-4) — Noor, hands busy.** He taps **Voice**; the microphone permission is requested there, inside the flow, not at first launch. The app shows that it is listening and what it heard. A confident command becomes a mapped key and rides the same ring as a tap — pulse, then settle or amber — so voice is not a privileged path with its own honesty rules. Dictation sends only into a focused native field, on the same explicit Send as typed text. The long-press and pointer-style commands are not offered, because they have no dependable single-key equivalent. The UI never claims the TV is listening: speech-to-text happens on the phone, and saying otherwise would be exactly the kind of claim this product refuses to make.

**Honest labels for 2014–15 H/J sets (CAP-8) — Wendy, the older set in the bedroom.** She adds her 2015 TV. Discovery finds it, pairing is attempted, and from the moment the model is identified the app says plainly: `2014–15 H/J series — best effort, not supported. Your TV is on a different, older system. Some things will work. We can't tell you which.` The label is a standing property of that TV's record, visible in the top-bar TV line and in TVs management — not a one-time warning she will forget. If the encrypted flow fails, the app falls back to declared-unsupported and says so. Nothing on this set is ever counted toward a compatibility claim, and the app never tells her it works "on most Samsung TVs," because no one has measured that.

## Inspiration & Anti-patterns

**Lifted from day one — Tactile Retro.** Bone key caps with real 2–3px of travel, etched surfaces, brass label plates. The physicality is not decoration; it is the honesty metaphor made tactile. A key travels when it is pressed (submitted), settles when the TV confirms (observed), and springs back. The motion language is spring, not fade, because a spring has mass and a fade does not — and this product's entire argument is that a write has not yet become an effect.

**Lifted from hardware — the status LED.** Real remotes and industrial controls solved exactly this problem decades ago: one element, several states, no toasts, no codes, readable across a room in the dark. The outcome ring is that LED.

**Lifted from the openHAB / Home Assistant model — the per-device capability profile.** Lift the honesty of a capability list that differs per device. Do **not** lift their habit of advertising a universal remote: here, "universal" is a non-goal, and the per-model record is the whole point.

**Rejected — cloud remote in the key path.** The first-party cloud path rate-limits at roughly 12 requests per minute per device. A user pressing volume or holding a D-pad would hit the ceiling within seconds. Keys, text and pointer stay on the LAN, which is also why local failure must be explained rather than routed around: there is no server to fall back to.

**Rejected — optimistic UI.** The single most damaging pattern available to this product. Showing "sent" or a tick the instant a socket write returns converts an unproven claim into a visible lie, and it is indistinguishable from the real thing until the moment it matters most. There is no state of this app in which a write is drawn as a success.

**Rejected — the universal-remote marketing frame.** No fleet-wide percentages, no "works with your TV," no compatibility claim that a physical test has not observed. The verification matrix forbids fleet-wide figures until measurements exist; the UI holds the same line.

**Rejected — text entry into app-drawn character grids as a launch promise.** Native fields work. Netflix-style grids draw their own keyboard and may never raise the system IME at all. The product contract is a field-bound explicit Send, and the UI says where it does not apply.

**Rejected — raw voice-audio injection.** The hypothesis is unverified and needs both a proven TV ingestion endpoint and Pastebin retrieval before it can be designed toward. Launch voice is phone-side STT mapped to keys, and the UI never pretends otherwise.

**Rejected — the always-on foreground service.** A background socket for ordinary keys costs the user battery and stands in for a product that should not need one. The socket lives with the UI.

**Rejected — pre-2016 Orsay and the legacy port-55000 protocol.** Out of scope by spec. The app does not detect-and-explain them either; it does not pretend to a reach it has not built.

## Responsive & Platform

Native Android. Platform behavior shapes the UX in five specific places.

**Permissions are requested at the moment of use, and denial is a state, not an error.**
Local-network access (SSDP discovery, raw sockets, inbound and outbound LAN traffic, and the wake packet) becomes an **enforced runtime permission** for apps targeting API 37+ — Spike L exists to prove the whole flow under that behavior before rollout, so the gate is built now rather than bolted on. When it is denied, the hero says what is blocked and offers the route that still works: manual IP entry for control, since unicast can cross a subnet where multicast cannot. `RECORD_AUDIO` is requested inside the voice flow. `POST_NOTIFICATIONS` is requested when a session starts, not at first launch.

**MediaProjection consent is single-use.** Android grants it per session; reuse throws. Every mirror session therefore contains its own consent step, always — and this is stated to the user as a reason mirroring is deliberate, not as an Android implementation detail.

**Foreground services are earned.** Only user-started cast, mirror and voice sessions run one, each with its matching type, each with an ongoing notification carrying exactly one action. The `dataSync` type is not available to this product — it is capped at six hours per 24 hours on Android 15+, and the design has no use for a budget it should not spend.

**The channel's lifetime is the UI's lifetime.** Because the socket lives only while the remote UI is active, backgrounding the hero disconnects. The app treats that as honest behavior rather than a failure to engineer around: on return, the state is re-established visibly through the state hierarchy, and a stale `Connected` is never displayed.

**Cast serving survives the screen going off.** The TV fetches from the phone over an address that stays reachable for the session, with byte-range support so seeking and long files work, bounded to the items Maya selected, with unguessable IDs, stopping at session end. Unicast control and the wake packet are separately constrained by the network the phone is on — and where the network cannot carry them, the app makes no claim.

**The TV side is the TV side.** One capability — CAP-7 mirroring — needs something installed on the television, shipped as a Tizen dev-mode sideload for pilot and matrix validation, with a store listing pursued only after latency and battery figures are recorded. It has its own surface (the **Tizen receiver companion**) precisely because it is the one part of this product that is not on the phone, and pretending otherwise would be the same dishonesty in a different costume.

`[ASSUMPTION]` Whether a large-screen layout ships in v1 — whether the hero and sheets are designed for `sw600dp` tablets and foldables, or the app stays a phone layout. Nothing in the captured decisions addresses form factors above phone.