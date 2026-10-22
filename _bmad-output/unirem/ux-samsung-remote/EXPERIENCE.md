---
name: UniRem
description: The behavior contract for a local-LAN Android remote for Samsung Tizen TVs — keys, text, voice, cast, mirror, wake, and the honesty states between them.
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
---

# UniRem — Experience Spine

> Native Android phone remote for Samsung Tizen TVs. Dark-only V1. This document owns behavior, information architecture, states, interaction, accessibility, and journeys. `DESIGN.md` owns visual identity; `{colors.accent}` and the other tokens below resolve there. Mockup references are composition evidence, not inherited decisions.

## Foundation

**One surface.** UniRem is a single-surface Android phone app, portrait-first, 360–480dp wide. There is no tablet layout, web client, iOS client, desktop app, or TV-side browsing UI in V1. On a foldable or a very large phone, the column centers at a maximum of 480dp; it does not stretch the D-pad or turn the launcher into a rail.

**Native.** Kotlin + Jetpack Compose. Material 3 supplies platform semantics — system back, predictive back, ripples where appropriate, focus, insets, TalkBack, font scale, haptics — while `DESIGN.md` overrides color, type, shape, elevation, and motion. The app uses the system IME; it does not ship a custom keyboard.

**Local.** Discovery, pairing, keys, text, pointer, cast control, and wake are local-LAN interactions. There is no cloud account and no remote control from outside the home. The phone and TV must share a broadcast domain; discovery assumes that; manual IP is always offered as a fallback, and client isolation is explained rather than hidden. If local-network access is denied, the app says so and neither scan nor manual IP can reach the TV until it is granted. The remote socket lives only while the Remote surface is in the foreground. Cast and mirror are user-started foreground services with the matching service type. Cast survives screen-off during a session; mirror depends on Android's capture session and is not promised to survive screen-off. The remote socket does not survive backgrounding.

**Capability-gated.** Every feature is gated per model by the verification matrix. The app never claims blanket support, never says "universal" without the per-model qualifier, and never presents a matrix miss as a bug in the TV. Tizen 2016+ is the committed floor. H/J 2014–15 sets, where attempted, are labeled **unsupported best-effort** in product and UI copy and never counted toward a pass.

**The contract.** The spec's eight capabilities are the product contract. This spine does not restate the protocol; it defines what the user sees and what the app may claim. `CAP-1` keys, `CAP-2` text, `CAP-3` pointer, `CAP-4` voice, `CAP-5` power/wake, `CAP-6` cast, `CAP-7` mirror, and `CAP-8` multi-TV pairing all resolve through the same state model and the same capability gate.

**Evidence.** The two journeys are composites built from public forum reports and Samsung support pages, not from interviews or usability tests. They establish that the failure patterns happen and how they feel; they do not establish prevalence. Full source table: [`.working/journey-options.md`](.working/journey-options.md). Palette reference: [`mockups/color-themes-1.html`](mockups/color-themes-1.html) (Graphite Mint only). Do not present the journeys as validated demand.

## Terms

These terms are canonical in the spine. UI labels are the plain-language translations in the last column.

| Term | Meaning | UI label |
|---|---|---|
| **TV** | A Samsung Tizen TV, identified by name, model, masked MAC, and per-TV trust material. | TV |
| **Paired** | Durable trust material exists for this TV. Pairing survives restart; it does not mean the socket is open. | Paired |
| **Connected** | The local socket is open now and the TV's identity still matches its pin. | Connected |
| **Submitted** | The phone wrote the command to the socket. This is not an on-TV effect. | Sent |
| **Unconfirmed** | The command was submitted; the app has no evidence the TV acted on it. It may still have worked. | Waiting for TV… |
| **Observed** | The app has evidence of the on-TV effect: a TV-originated event, a state probe, or an explicit user confirmation. | TV confirmed / You confirmed |
| **User-confirmed** | A specific kind of observed: the user saw the effect and tapped "It worked." The app never presents this as machine evidence. | You confirmed |
| **Best-effort** | A model or path the matrix has not validated for confirmation, or an H/J 2014–15 set. Keys may work; the app cannot prove it. | Best-effort — this TV can't confirm commands |
| **Session** | A user-started cast or mirror pinned to one TV and reachable through the session chip. | Casting to [TV] / Mirroring to [TV] |
| **Capability** | A per-model, matrix-backed feature gate. A capability is not a marketing claim. | Works / Not tested / Not supported / Best-effort |
| **Native field** | A TV-drawn text field the app can target with `SendInputString`, such as Settings or a browser URL field. | TV text field |
| **App-drawn grid** | A streaming app's own character grid, which the app cannot type into unless a per-surface probe passes. | This app draws its own keyboard |

**Observed is never socket-write success.** A green ring means the app saw the effect or the user explicitly confirmed it. A socket write alone is `submitted`, then `unconfirmed`. The app must be comfortable with an amber ring; that is the product telling the truth.

## Information Architecture

| Surface | Reached from | Purpose | Owns |
|---|---|---|---|
| **Remote** | App open with a target TV; TV switcher selection | Keys, state ring, launcher, session chip | The active TV target and the last command state |
| **TV switcher** | Topbar TV name | Switch TV, add TV, open TV details | Explicit target selection; never an implicit last TV |
| **Add TV / Discovery** | TV switcher → Add a TV; first run | Scan, manual IP, permission explainer, results | Discovery state and the no-TV empty state |
| **Pairing** | Selecting an unpaired TV | Identity card, Allow steps, TV-button diagram, re-pair | Trust-on-first-use and the pairing-required state |
| **TV details** | TV switcher → Manage | Identity, capability list, support label, fingerprint, forget, diagnostics | Durable truth about one TV |
| **Text sheet** | Launcher → Text | Draft, send, cancel, check the TV | Field-bound text entry; no live keystrokes |
| **Voice sheet** | Launcher → Voice | Hold-to-talk, lock-to-dictate, transcript, command/text review | Mic permission, speech result, send confirmation |
| **Cast sheet** | Launcher → Cast | Pick one photo/audio/video, check renderer, warn on format/subtitles, play | Cast setup and pre-play warnings |
| **Cast session** | After play; session chip | Transport, stop, return to the session | One pinned cast session |
| **Mirror sheet** | Launcher → Mirror | Receiver check, setup, capture consent, start | Receiver prerequisite and MediaProjection consent |
| **Mirror session** | After start; session chip | Status, stop, return | One pinned mirror session |
| **Wake sheet** | Launcher → Wake | Checklist, wake claim, progress, timeout | Matrix pass and wake evidence |
| **Settings** | Topbar gear | Haptics, reduced motion, permissions, diagnostics, about | App-level preferences, not TV capability |

**Navigation rules.**

- One sheet at a time. Opening a second sheet replaces the first; sheets never stack a second scrim. Back, scrim tap, or drag-down dismisses.
- The TV switcher is a sheet, not a tab. The Remote is always one dismissal away.
- With one stored TV, that TV is auto-selected. With two or more, cold start opens the TV switcher and requires an explicit choice; the previously used TV may sort first but is not silently targeted. "No implicit last TV" is a hard rule.
- Within an app session, the chosen TV stays the target across surfaces. A process restart or a return from a killed state reopens the picker when multiple TVs exist.
- Switching TVs closes Text, Voice, Cast picker, Mirror setup, and Wake sheets. It does not stop an active cast or mirror session; those are pinned to their TV and remain reachable through the session chip.
- Every surface has a way back to Remote. Every blocking state has one next action, not a maze.

## Voice and Tone

The app speaks like a calm, competent person standing next to the TV — not a brand, not an assistant, not a friend. It names the fact, the next action, and the limit. It does not blame the user, the TV, or the network. It does not use exclamation marks, jokes, or reassurance that isn't true.

**Principles.**

- **Plain.** "TV didn't answer" beats "Connection failure." "Press Allow on your TV" beats "Authorize the client."
- **Calm.** No alarm red text, no "Oops!", no "Uh-oh." Offline is a fact with a next action.
- **Specific.** Name the TV, the key, the field, the limit. "This app draws its own keyboard" beats "Text entry may not be supported."
- **Honest.** Say what the app knows and what it does not. Never call submitted "done." Never call unconfirmed "failed." Never call paired "connected."
- **Short.** One idea per sentence. Body copy is two sentences at most; a sheet body is three at most. Diagnostics can be longer, but they live behind "Details."
- **Second person, present tense.** "You can..." / "The TV is..." No "we" as a brand, no "I" as a persona.
- **No marketing.** No "seamless," "magic," "effortless," "universal," "works with any Samsung TV," "smart home," "AI-powered." The product earns trust by being precise.

### Microcopy

| Context | Use | Avoid |
|---|---|---|
| First run, no TVs | "No TVs yet." / "UniRem controls Samsung TVs on your Wi-Fi. Nothing is sent to the internet." / "Add your first TV" | "Welcome to UniRem!" / "Let's get started" |
| Discovery | "Scanning for TVs…" / "No TVs found. Check that your phone and TV are on the same Wi-Fi." / "Enter IP address" | "Searching for devices" / "No devices detected" |
| Pairing prompt | "Press Allow on your TV." / "A prompt is on your TV screen right now." | "Authorize this client" / "Pairing in progress" |
| No physical remote | "No remote? On many Samsung TVs, the button under the logo can select Allow. Press it once, then press and hold." / "If your TV's button can't select Allow, you'll need the remote." | "You don't need a remote" / "Remote-free pairing" |
| Pairing timeout | "The TV didn't answer." / "Keep the Allow prompt open and try again." / "Retry" / "Not now" | "Pairing failed" / "Error 408" |
| Pairing denied | "The TV said no." / "You can try again from the TV's Allow prompt." | "Access denied" / "Unauthorized" |
| Paired vs connected | "Paired" (durable) / "Connected" (socket open now) | Using them interchangeably |
| Submitted | "Sent" | "Done" / "Success" / "Key sent successfully" |
| Unconfirmed | "Waiting for TV…" / "The TV hasn't confirmed this yet. It may still have worked." / "It worked" | "Failed" / "No response" / "Error" |
| Observed by machine | "TV confirmed" | "Success!" / "✓" alone |
| Observed by user | "You confirmed" | "TV confirmed" when only the user saw it |
| Offline | "TV didn't answer — retry?" / "Keys are off until the TV reconnects." / "Retry" / "Not now" | "Connection lost" / "Please check your network" |
| Unsupported key | "This TV doesn't report Play — it may still work." | "Unsupported feature" / "Not available" |
| Best-effort TV | "Best-effort — this TV can't confirm commands." / "The ring stays amber here — that's the TV's limit, not a bug." | "Unsupported TV" / "Not compatible" |
| Text helper | "Phone typing works in TV settings and the browser." / "This app draws its own keyboard. Use the D-pad here." | "Type anywhere" / "Universal keyboard" |
| Text sent | "Sent. Check the TV." / "Nothing appeared? Open a text field on the TV and try again." | "Text sent successfully" |
| Voice | "Hold to talk. Slide up to lock." / "Listening…" / "I heard: [transcript]" / "Send to TV" | "Speak now" / "Voice assistant" |
| Cast | "Casting to Living Room TV." / "This TV may not show external subtitles." / "Play without subtitles" / "Try anyway" | "Seamless casting" / "Plays anything" |
| Mirror | "Mirror needs the UniRem receiver on your TV." / "Open the receiver on your TV, then check again." | "Mirroring unavailable" / "Install our app" |
| Wake | "Wake this TV?" / "Waking. This can take up to a minute." / "The TV didn't wake. Try the TV's power button." | "Powering on" / "Wake failed" |
| Destructive | "Stop casting?" / "Stop" / "Keep watching" / "Forget Living Room TV? You'll need to pair again." | "Are you sure you want to proceed?" |

**Error formula:** fact + next action + limit, in that order. "The TV didn't answer. Keep the Allow prompt open and try again." "Nothing appeared? Open a text field on the TV and try again." "This TV can't wake from the app. Use the TV's power button." No error codes in primary copy; they belong in TV details → Diagnostics.

**Punctuation and casing.** Sentence case for labels and buttons. Use the ellipsis character `…` for in-progress states. Use a period for a completed statement ("TV confirmed."). No exclamation marks. No emoji. No all-caps sentences; uppercase is reserved for short `micro` labels in the visual layer.

## Component Patterns

The components below are the behavior contract. Visual specs live in `DESIGN.md`; names match exactly.

| Component | Behavior |
|---|---|
| **Topbar** | 56dp. On Remote: TV switcher left, 48dp settings right. On Discovery/Add TV, Pairing, TV details, and Settings: back + static title left, right empty. No command state in the topbar; no green check, no "last key" text. |
| **TV switcher** | Tap opens the TV switcher sheet. Shows every stored TV with health dot, name, model, and a durable capability label. With one TV, selecting it just closes; with several, selection is explicit. "Add a TV" is always last. Switching does not stop pinned sessions. Never auto-targets the last used TV on cold start when multiple exist. |
| **Health dot** | Socket health only: connected (`{colors.status-connected}`), reconnecting (`{colors.status-reconnecting}`), offline (`{colors.status-offline}`). Never mirrors the ring's command state. Its accessibility label names the TV and the health. It does not pulse when reduced motion is on. |
| **One-time chip** | Explains a gated capability or a model limit, once per TV/capability. One at a time; additional explanations live in TV details. Max two lines. Dismissible with a 48dp close. It must name what still works, not only what doesn't. It never returns unless the TV is re-paired, the firmware changes, or a probe result changes. |
| **State ring** | The five-state ambient object around the D-pad. Not tappable. Color + glow + the state line carry the state. See "State ring rules" below. |
| **D-pad** | Tap sends one key. Press-and-hold a direction repeats after 500ms, then every 80ms; release sends key-up. No diagonals, no swipe navigation. OK taps Enter. Keys are disabled when the socket is closed. |
| **Utility row** | Volume up/mute/down on a rocker; play/pause; power off. Volume and play/pause are ordinary keys. Power is power off only, never a toggle; Wake is a separate launcher slot. Power off has no confirmation sheet, but it is visually separated and never adjacent to OK. |
| **Function key** | Home, Back, Apps, Search. Tap sends the key. Back long-press for 1.2s sends a long Back only where the model supports it; a haptic marks the threshold. Search opens the TV's search, not a phone text field. If a model doesn't report the key, the key remains usable but a one-time chip says "This TV doesn't report this key — it may still work," and TV details records the limitation. |
| **Launcher** | Fixed six slots: Text, Voice, Pad, Cast, Mirror, Wake. Always visible, including with no TV. Disabled slots stay in place, dimmed, dashed, and tappable to explain. No reordering, no hide, no overflow menu. |
| **Launcher slot** | Tap opens the corresponding sheet or session. The Voice slot opens the Voice sheet; the large mic key inside is the hold target. A slot never starts capture or cast on a single tap without a review step. Disabled slots open their explanation sheet; the action inside is blocked. |
| **Bottom sheet** | One modal container for Text, Voice, Cast, Mirror, Wake, TV switching, and permissions. One at a time. Drag-down, scrim tap, or back dismisses. Primary action stays above the keyboard; destructive actions are separated. No nested sheets. |
| **Text field** | Single-line draft for the TV's focused native field or browser URL. No live keystrokes, no history, no clipboard read, no logging. Send is explicit. Cancel clears. The field does not claim to know whether the TV is focused. |
| **Mic key** | 72dp hold target inside the Voice sheet. Press-and-hold starts listening; release stops; slide up while holding locks; tap "Done" stops a locked session. The transcript is visible. Raw audio is never sent to the TV. |
| **Session chip** | Persistent 48dp chip above the launcher (or at the bottom of another surface) naming the pinned session and TV. Tapping returns to the session. It survives TV switching and app backgrounding; cast survives screen-off. It is not a command state and does not use the ring colors as a badge. |
| **Banner** | Only for blocking/actionable conditions: offline, reconnecting, identity changed, cast stopped, pairing required. One recovery action plus an optional "Not now" dismiss. Pushes content; does not overlay. No spinner. No banner for ordinary unconfirmed commands; the state line owns that. |
| **State line** | The text twin of the ring: "Idle", "Sent", "Waiting for TV…", "TV confirmed", "You confirmed", "Offline". 13sp, centered, live region. After 3s unconfirmed, it expands to include a 48dp "It worked" action. Never truncates the state word. |
| **Pairing card** | Name, model, Tizen version, masked MAC, "same Wi-Fi as this phone," and a first-time verification note. Primary: "This is my TV — connect." Secondary: "That's not my TV." The no-remote path is visible, not buried. |
| **TV card** | Durable identity plus one capability label: "Full control — confirms keys" or "Best-effort — can't confirm commands." Actions: Open remote / TV details. Forget lives in details. The health dot is current socket state, not capability. |
| **Primary button** | One per sheet/card. Mint fill; the action that moves the user forward. Never used for destructive actions. |
| **Secondary button** | "Not now," "Cancel," "That's not my TV." Hairline outline. Never mint. |
| **Destructive button** | "Stop casting," "Stop mirroring," "Forget TV." Salmon outline, never a filled red block. Requires a second tap in the confirmation sheet. |
| **Permission explainer** | Appears before the system prompt, in context. One sentence of why, one of what happens if denied. "Continue" triggers the system prompt; "Not now" leaves the feature unstarted. Denied state offers "Open settings" and a plain explanation. No permission wall on first launch. |
| **Empty state** | "No TVs yet." plus what the app knows (network, scan result, diagnostics) and one action. Launcher visible but disabled. Not a marketing page. |

### State ring rules

The ring is the command-outcome channel. It is not a progress spinner and not a connectivity indicator. Ring composition: [`mockups/direction-noir.html`](mockups/direction-noir.html). The state line and offline treatment: [`mockups/direction-signal.html`](mockups/direction-signal.html).

| State | Trigger | Ring | State line | Holds |
|---|---|---|---|---|
| **Idle** | No command in flight; last command settled | `{colors.ring-idle}`, no glow | "Idle" | Until the next press |
| **Submitted** | The phone wrote the command to the socket | `{colors.ring-submitted}`, one 900ms breath | "Sent" | Until observed or 3s |
| **Observed** | TV event/probe, or user confirmation | `{colors.ring-observed}`, 24dp glow | "TV confirmed" or "You confirmed" | 4s, then idle |
| **Unconfirmed** | 3s passed without observation; socket still open | `{colors.ring-unconfirmed}`, amber glow | "Waiting for TV…" + "It worked" | Persists until resolved or the next press |
| **Offline** | Socket closed | `{colors.ring-offline}`, salmon glow | "Offline" | Persists until reconnect |

**Rules.**

- A press always starts at Submitted, even if the previous state was amber or red. The ring belongs to the last press; late confirmations for older presses are ignored.
- The 3s submitted→unconfirmed threshold is a design assumption to validate in Spike A. It is not a protocol claim. If the matrix shows a longer normal latency, raise the threshold rather than blaming the TV.
- If the TV confirms the last press after the amber state, the ring moves to Observed. If the user taps "It worked," it moves to Observed with the text "You confirmed." Neither path is allowed to say "TV confirmed" unless the app has machine evidence.
- When the socket is open but the TV is silent, keys stay enabled. The user can keep pressing; each press resets the ring. When the socket is closed, keys are disabled and the banner owns the recovery. This is the only command/connectivity handoff.
- The 900ms breath runs once per press, not in a loop. Observed holds 4s (inside the accepted 3–5s range), then fades to idle. Reduced motion removes the breath and the glow pulse; the color and text change instantly.
- The ring is not focusable. The state line is the live region (`polite`). A blocking banner uses `assertive` only for identity change or offline; ordinary command states never interrupt.
- The ring never shows a checkmark, a percentage, a sweep, or a segmented arc. Its only shapes are circle and stroke.

### Text sheet rules

- Open from Text. The sheet title is "Text to [TV]"; the helper says "Typing goes to the TV's focused field."
- The sheet cannot reliably know whether the TV has a focused native field. It does not pretend to. It offers a field, a Send button, and a standing helper: "Phone typing works in TV settings and the browser. If this app draws its own keyboard, use the D-pad here." A recorded per-surface limit or a passing probe can make the second sentence more specific; without that, the helper covers both cases before the user types.
- Send is explicit: one tap or the IME Send action. There are no live keystrokes and no per-character writes. The draft is held in memory only, cleared on close, and never logged.
- After Send: the sheet says "Sent. Check the TV." The command moves to Submitted; after 3s without an observation it is Unconfirmed. The sheet offers "It's there" (marks You confirmed) and "Try again." It does not say "Text sent successfully."
- If no text appears, the helper says "Nothing appeared? Open a text field on the TV and try again." The draft remains for retry until the sheet closes.
- Text is never used for app-drawn character grids as a launch promise. A per-surface probe can enable a specific grid later; until then, the app says so before the attempt, not after.
- Passwords: the app cannot tell whether the TV field is a password. It does not log, retain, or suggest text; the phone keyboard's secure-entry behavior is the platform's. The sheet closes and clears on cancel or background.

### Voice sheet rules

- Open from Voice. The sheet shows a 72dp mic key, "Hold to talk. Slide up to lock," and a permission status if mic access is not granted.
- Press-and-hold starts listening. Release stops. Slide up while holding locks the session; the label changes to "Locked" and the user taps "Done" to stop. There is no wake word and no always-listening state.
- The transcript is visible while listening and after stop. Non-destructive commands execute on release; destructive commands and dictation wait for an explicit review.
- Commands from the agreed key set execute on release for non-destructive keys (volume, direction, OK, play/pause, mute, home, back, apps, search). The sheet displays the parsed command and the resulting ring state. Power off is destructive: the sheet shows a "Power off [TV]" action that requires a tap; a misheard power command never fires on release.
- Dictation never auto-sends. It shows a draft and a "Send to TV" action. Before sending, the sheet asks the user to confirm that a native field is open, or uses a recorded per-surface pass; it never claims to detect focus. If the user says no, it says "Open a text field on the TV, then come back" and offers "Use the D-pad."
- Raw audio never leaves the phone for the TV. The app does not record or retain audio. V1 does not inject raw audio; any future raw-audio path is a spike gate, not a launch commitment. Whether the phone's speech recognizer runs on-device depends on the device; the app says "Speech is handled by your phone" in the permission explainer, not "on-device" unless the platform guarantees it.
- If mic permission is denied, the sheet explains the limit and offers "Open settings." It does not re-prompt in a loop.

### Cast rules

- Open from Cast. If a cast session is already active, the slot returns to that session instead of opening a new picker.
- The active TV is preselected and named in the sheet. Changing TVs is done through the TV switcher before starting; the sheet does not silently cast to another TV.
- One item per session: a photo, an audio file, or a video. The system Photo Picker is the source for photos and videos; the system document picker is the source for audio. The app does not request broad storage access. No queue, no autoplay next, no playlist.
- Before play, the app checks the renderer. If absent: "This TV isn't showing a cast receiver. Make sure it's on the same Wi-Fi." No session is created.
- Before play, the app surfaces known format/subtitle limits: "This TV may not show external subtitles." Actions are "Play without subtitles," "Try anyway," or "Choose another." The app does not claim subtitle support it has not verified.
- During play: play/pause, stop, and elapsed/duration when the renderer reports them. The session chip says "Casting to [TV]." A foreground-service notification says the same and offers Stop.
- Session ends on user Stop, renderer stop/error, TV disconnect, or app force-stop. For an in-app end, the app shows "Cast stopped" with the reason and a "Start again" action when the file is still available. After a force-stop, the next launch does not claim the cast survived. It does not silently restart.
- Screen-off does not stop the cast; the phone server continues serving the selected item until the session ends. The notification is the user's control surface while the app is backgrounded.
- The TV fetches from the phone over http(s); the app never hands the TV a `content://` URI. The UI never promises support for an unverified codec, subtitle format, seek, or long file.

### Mirror rules

- Open from Mirror. Receiver check first: "Mirror needs the UniRem receiver on your TV." If the receiver is not installed/running, the sheet shows the setup steps and "Check again." No MediaProjection consent is requested yet.
- If the receiver is ready, the sheet explains "Your phone screen will be shared until you stop," then "Start mirroring" triggers the Android capture consent. Denial returns to the sheet with "Screen capture wasn't allowed" and "Open settings" / "Not now."
- Mirroring without a receiver is excluded. The app never starts a capture session hoping a receiver appears. It never silently installs anything on the TV.
- During mirroring: a pinned session chip says "Mirroring to [TV]"; a foreground-service notification offers Stop. The app does not promise screen-off, audio, latency, or battery figures; those are spike results, not UI claims. If capture ends, the app says "Mirror stopped" and offers "Start again."
- Stop requires a confirmation: "Stop mirroring?" / "Stop" / "Keep mirroring." The session ends cleanly and the notification disappears.

### Wake rules

- Wake is a launcher slot, separate from power off. It is visible on every TV. If the matrix has no recorded wake pass for the model/network, the slot is disabled with a dashed border and a lock mark; tapping opens the explanation, not a magic packet.
- With a pass, the sheet shows a short checklist: the TV is plugged in; phone and TV are on the same network; "Power On with Mobile" is on in the TV's network settings; the TV's MAC is saved. The checklist is a confirmation, not a guarantee; the app does not claim a wake until the TV answers on 8001/8002.
- "Wake" sends the magic packet. The sheet says "Waking. This can take up to a minute." Success only when the TV answers. Timeout says "The TV didn't wake. Try the TV's power button." There is no retry loop and no false success.
- Power off is not a toggle. Power off sends the power key while the TV is on; Wake is the only action that attempts to bring it back. The app never uses one button for both.

### Pairing and TV details rules

Pairing composition: [`mockups/direction-native.html`](mockups/direction-native.html). TV card and best-effort composition: [`mockups/direction-tactile.html`](mockups/direction-tactile.html).

- Pairing is trust-on-first-use. The card shows the name, model, Tizen version, masked MAC, and "same Wi-Fi as this phone." The SPKI fingerprint is under "Details," not in the primary card. The full MAC and token are never shown.
- "This is my TV — connect" starts the pairing attempt. The TV shows Allow. The app says "Press Allow on your TV." The no-remote path is always visible: "No remote? On many Samsung TVs, the button under the logo can select Allow. Press it once, then press and hold." The app also says "If your TV's button can't select Allow, you'll need the remote." It never promises remote-free pairing.
- If the Allow prompt times out: "The TV didn't answer." Retry / Not now. If the TV denies: "The TV said no." The app does not auto-retry, and it does not store a token.
- After pairing, the app validates the pin before sending the token. If the identity changed: keys disabled, banner "This TV's identity changed. Re-pair to continue." / "Re-pair." The app never trusts a changed certificate silently.
- TV details lists each capability with one of four labels: Works, Not tested, Not supported, Best-effort. It shows the SPKI fingerprint, IP, masked MAC, Tizen version, last verified, and a "Forget TV" action. Forget confirms: "Forget [TV]? You'll need to pair again." It removes local trust material; it notes that the TV may still list the phone until removed on the TV.
- The durable capability label in TV details is the source of truth. The one-time chip is a nudge; it is not the record.

## State Patterns

### Connectivity and identity

| State | Trigger | UI | Rules |
|---|---|---|---|
| **Cold start, no TVs** | First launch or all TVs forgotten | Empty state: "No TVs yet." + scan readout + "Add your first TV"; launcher disabled | No auto-scan loop; one scan on open, then a manual "Scan again." |
| **Scanning** | Add TV / first run | "Scanning for TVs…" + discovered cards appear as found | No spinner; text state. Manual IP is always offered; it cannot connect while local-network access is denied. |
| **Scan empty** | Scan ends with zero results | "No TVs found. Check that your phone and TV are on the same Wi-Fi." + "Enter IP address" + "Scan again" | No silent fallback to a stored TV. Explain client isolation in a details link. |
| **One TV** | One stored, paired TV | Auto-selected; Remote opens with that TV | This is the only automatic target. |
| **Multiple TVs** | Two or more stored | TV switcher opens on cold start; explicit selection | No implicit last TV. Previously used TV may sort first; it is not selected. |
| **TV found, unpaired** | Discovery result | Pairing card with identity + "This is my TV — connect" | Refusal path always visible. |
| **Pairing prompt** | Connect tapped | "Press Allow on your TV." + no-remote path + TV card | App waits; no silent retries. |
| **Pairing timeout** | No token before the TV prompt closes | "The TV didn't answer." + Retry / Not now + button diagram | No token stored. |
| **Pairing denied** | TV says no | "The TV said no." + "Try again from the TV's Allow prompt." | No auto-retry. |
| **Local network denied** | System local-network permission denied | Explainer remains: "UniRem needs local network access to reach your TV." + "Open settings" / "Not now"; scan and manual IP cannot reach the TV until permission is granted | The limit is explicit; no repeated system prompts. |
| **Connected / idle** | Socket open, identity valid, no command | Topbar dot mint; ring idle; "Idle" | Paired is not connected; connected is not a command outcome. |
| **Reconnecting** | Socket lost, backoff active | Topbar dot amber; keys disabled; banner "Reconnecting…" / Retry / Not now | Backoff is visible; no spinner. |
| **Offline** | Socket closed after failure | Topbar dot salmon; ring offline; keys disabled; banner "TV didn't answer — retry?" / "Keys are off until the TV reconnects." | Keys do not send into a closed socket. No silent queue. |
| **Identity changed** | Pin mismatch | Topbar dot amber; keys disabled; banner "This TV's identity changed. Re-pair to continue." / "Re-pair" | Never trust a changed certificate silently. |
| **Pairing required** | Token rejected/expired | Banner "Pairing is required for this TV." / "Re-pair" | No retry with an old token. |

### Command states

The ring table above owns Submitted, Unconfirmed, Observed, and Offline. Two command-state variants need their own rules:

| State | Trigger | UI | Rules |
|---|---|---|---|
| **Unsupported key** | Model reports unsupported | Key remains; one-time chip "This TV doesn't report [key] — it may still work." | Do not call it failed; do not hide the key unless the model cannot accept it at all. |
| **Late observation** | TV confirms an older press | Ignored if a newer press exists | The ring belongs to the last press only. |

### Capability and permission states

| State | Trigger | UI | Rules |
|---|---|---|---|
| **Text, no field** | Send succeeds but the user reports nothing appeared | "Nothing appeared? Open a text field on the TV and try again." + "Try again" / "Close" | The draft stays until close; no automatic re-send. |
| **Text, app-drawn grid** | User reports an app-drawn keyboard, or a per-surface record says so | "This app draws its own keyboard. Use the D-pad here." | The standing helper says this before the attempt; a recorded limit makes it specific. |
| **Voice permission denied** | Mic denied | Permission explainer with "Open settings" / "Not now" | No repeated system prompts; no voice capture. |
| **Voice listening** | Mic key held | "Listening…" + live transcript + mic halo | Release stops; slide up locks. |
| **Voice review** | Release/lock done | Transcript + parsed command or dictation draft + action | Destructive commands require a tap; dictation requires Send. |
| **Pad not probed** | First Pad tap or no probe record | "Check pointer" sheet: "This will move the TV's pointer. You can stop anytime." + "Check pointer" / "Not now" | No capture; the pointer probe is explicit. |
| **Pad unsupported** | Probe fails | Pad slot disabled, dashed; explanation "This TV didn't pass the pointer check. Keys still work." | Visible, explained, not hidden. |
| **Cast renderer missing** | Cast tapped; no renderer | "This TV isn't showing a cast receiver. Make sure it's on the same Wi-Fi." + "Check again" / "Cancel" | No session, no serving. |
| **Cast warning** | Format/subtitle may not work | "This TV may not show external subtitles." + "Play without subtitles" / "Try anyway" / "Choose another" | Pre-play, not post-failure. |
| **Mirror receiver missing** | Mirror tapped; no receiver | "Mirror needs the UniRem receiver on your TV." + setup steps + "Check again" / "Not now" | No MediaProjection prompt until receiver ready. |
| **Mirror consent denied** | Capture denied | "Screen capture wasn't allowed." + "Open settings" / "Not now" | No capture; no hidden retry. |
| **Notification permission denied** | POST_NOTIFICATIONS denied | Session can run while the app is open; app explains that background control needs the notification; Stop stays in the app | No hidden capture; no repeated system prompt. |
| **Wake no pass** | Wake tapped; no matrix pass | Disabled slot; explanation "No wake pass for this TV. Use the TV's power button, or check TV details." | No magic packet; no false claim. |
| **Wake in progress** | Wake sent | "Waking. This can take up to a minute." | No spinner; success only when TV answers. |
| **Wake timeout** | No answer | "The TV didn't wake. Try the TV's power button." + "Try again" / "Close" | No loop. |
| **Background/lock** | App backgrounded | Cast/mirror: foreground notification with Stop; Remote socket closes | On return, reconnect and revalidate; an in-flight key is not claimed as observed. The ring resets to idle unless a fresh press starts a new command. |
| **Restored app** | Process recreated | No tokens from backup; re-pair if needed; picker if multiple TVs | Pairing material is excluded from backup. |

## Interaction Primitives

- **Tap** is the default. Every key, chip, card, and slot responds to a single tap. There are no double-tap, triple-tap, shake, or multi-finger gestures.
- **Press-and-hold** is reserved for three things: D-pad repeat after 500ms, Back long-press after 1.2s where supported, and the Voice mic key. Nothing else hides a long-press action.
- **Slide up to lock** is the only custom slide gesture on a control. Sheet drag-down is dismissal, and content scrolling stays inside sheets; neither navigates the remote. It exists on the Voice mic key while holding.
- **No swipe on the D-pad.** The thumb-arc is a placement, not a trackpad. Pointer input lives in the separate Pad sheet and is gated by a probe. The hero never mixes D-pad taps with pointer movement.
- **Sheets** dismiss on drag-down, scrim tap, or system back. One sheet at a time. Opening a sheet moves focus into it; dismissal returns focus to the trigger. No nested sheets.
- **Text input** uses the system IME. The sheet's Send action stays above the keyboard. The IME action is Send; the button is the primary action. No live per-character send, no hidden submit on blur.
- **Haptics** are light and sparse. A key press gets a short 8ms tick; observed gets two short ticks; unconfirmed gets one 12ms tick; offline gets a 20ms pulse. Haptics never replace the visual state, and the Settings toggle plus the system haptic setting are both honored.
- **Motion** is limited to the ring breath (900ms, once per press), the observed hold (4s), the sheet transition (220ms slide/fade), and the pressed-key scale (0.98, 80ms). Reduced motion removes the breath and the scale, uses an instant color change, and reduces the sheet to a fade. No other element animates.
- **Sound** is never used. The app does not beep, speak, or play a confirmation tone.
- **Loading** is text, not a spinner. Discovery says "Scanning for TVs…"; wake says "Waking…"; cast setup says "Checking the TV…". The ring breath is for commands, not for loading.
- **Scrolling** exists only inside sheets and settings. The Remote surface does not scroll; the hero is fixed. No pull-to-refresh; discovery has an explicit "Scan again."
- **Back** follows platform convention: dismiss the top sheet; from Remote, leave the app. No custom interception beyond the sheet stack. Predictive back previews the sheet dismissal.
- **Sessions** are pinned. Switching TVs never silently stops a cast or mirror; the session chip is the way back. Stopping requires a confirmation. A session never migrates to another TV.

## Accessibility Floor

**Target:** WCAG 2.2 AA minimum; AAA for primary body text. The token contrast table in `DESIGN.md` is the reference, not an aspiration.

- **Text contrast.** Body uses `{colors.ink-primary}` on `{colors.surface-base}` (15.45:1, AAA). Secondary explanations use `{colors.ink-secondary}` (6.85:1, AA). `{colors.ink-disabled}` is for disabled controls only, never for information. Mint text/icons use `{colors.accent}` (10.17:1).
- **Non-text contrast.** Observed, unconfirmed, and offline rings exceed 3:1. The idle ring and hairlines are deliberately below 3:1 and are never the sole carrier of state; the state line always names it. Focus uses `{colors.focus-ring}` at 2dp (9.39:1 on a key).
- **Targets.** 48dp minimum for every touch target; 56dp direction keys; 70dp OK; 52dp launcher slots. Targets never overlap; 8dp gaps where possible. No action lives under the gesture bar.
- **Color is never alone.** Ring state = color + glow + state line. Health = dot + accessibility label. Capability = text plate, not a colored LED. Disabled = dashed + lock + label, not only opacity.
- **TalkBack.** Every key has a name and a role: "Volume up, button. Double tap to send." "Direction up, button." "OK, button." The TV switcher reads "Living Room TV, connected. Double tap to switch TV." The state line is a polite live region: "Waiting for TV. The TV hasn't confirmed this key." The banner is assertive only for offline/identity changed. The ring itself is not focusable; the state line is the spoken state.
- **Focus order** matches visual order: topbar → one-time chip → utility row → function keys → D-pad → state line → launcher. A sheet traps focus; dismissal returns to the trigger. Disabled slots remain focusable so their explanation is reachable.
- **Dynamic type.** All text uses `sp`; the layout supports 200% font scale. At 200%, launcher labels wrap to two lines and the launcher becomes 3×2; function-key labels wrap; the state line wraps to two lines and the hero shifts. No critical label truncates. The text field scrolls into view above the IME.
- **Reduced motion.** Follow the system setting. No ring breath, no glow pulse, no pressed scale; color and text update instantly. Sheet transitions become a short fade. No autoplay, no parallax, no looping animation.
- **Haptics** are supplementary. The app works with haptics off. The Settings toggle is independent of the system setting; if either is off, no haptics.
- **Screen reader and voice.** The Voice sheet exposes the transcript as text, not audio-only. The mic key's state ("Listening," "Locked") is announced. Dictation is reviewed before send.
- **RTL.** Text, sheets, and rows mirror in RTL. D-pad arrow directions, volume/mute glyphs, and media transport icons do not mirror; they represent physical directions, not reading order.
- **Localization.** Strings are externalized, not concatenated. Layouts tolerate 1.3× expansion. Sentence case is preserved; no string assumes English word order for a sentence assembled from parts.
- **No accessibility-only mode.** The accessible path is the main path; there is no separate simplified screen.

## Key Flows

Both protagonists are composites. Their names, ages, rooms, and times are narrative devices; the failure patterns are sourced in [`.working/journey-options.md`](.working/journey-options.md). The evidence is forum-reported and official-support material, not representative research. The flows show the intended behavior, not validated demand.

### Flow 1 — "The remote is gone" (Ruth, 43)

**Scene.** A rented flat, Tuesday 8:20pm. A 2019 Samsung TV in the living room. The remote is missing. The TV is on, stuck on the home screen, volume low. Ruth and her partner were halfway through an episode.

**Goal.** Get the volume up and press play. The phone is the only remote in the flat.

**Obstacle.** The TV's first pairing shows Allow/Deny on the TV. The app cannot press it. The phone-app pattern is a catch-22: the app asks the user to confirm with the remote they do not have. Samsung's controller button is the documented fallback, but it is obscure and limited. Discovery and local-network permission can fail before that.

**Steps.**

1. Ruth opens UniRem. There are no stored TVs. The empty state says "No TVs yet." and explains that UniRem controls Samsung TVs on the Wi-Fi; nothing goes to the internet. She taps "Add your first TV."
2. Android's local-network permission explainer appears in context: "UniRem needs to find your TV on this Wi-Fi. Nothing is sent to the internet." She taps Continue. The system prompt appears; she allows it.
3. Discovery starts automatically. The row says "Scanning for TVs…" and a card appears: **Living Room TV**, Samsung 2019 model, its Tizen version, masked MAC, "same Wi-Fi as this phone." It matches the TV in front of her.
4. She taps "This is my TV — connect." The Pairing surface opens: "Press Allow on your TV." A prompt is on the TV now.
5. The card includes the no-remote path, always visible: "No remote? On many Samsung TVs, the button under the logo can select Allow. Press it once, then press and hold." A small diagram shows the button under the Samsung logo. The card also says: "If your TV's button can't select Allow, you'll need the remote."
6. She finds the button, presses once, and the TV highlights **Allow**. She presses and holds. The TV accepts. The app validates the SPKI pin before storing the token; the card becomes "Paired."
7. The Remote surface opens. The topbar dot is mint; the ring is idle; the state line says "Idle." The TV name is in the topbar; the model is in the switcher and TV details.
8. She presses volume up. The ring breathes pale mint: "Sent." The TV's volume moves. If the TV sends an observation, the ring settles mint: "TV confirmed." If it does not, the ring turns amber after 3s: "Waiting for TV…" with "It worked." She sees the effect and taps "It worked"; the ring settles mint: "You confirmed."
9. She presses play. The episode resumes. The volume moved and the play icon responded; the phone stayed in her hand.

**Climax beat.** The first volume press. The ring changes color; the TV changes state. The app does not claim success before the TV acts.

**Failure paths.**

- **Allow times out.** The pairing card stays: "The TV didn't answer." / "Keep the Allow prompt open and try again." / "Retry" / "Not now." No token is stored. The button diagram remains.
- **TV denies.** "The TV said no." / "You can try again from the TV's Allow prompt." No auto-retry.
- **Discovery empty.** "No TVs found. Check that your phone and TV are on the same Wi-Fi." / "Enter IP address" / "Scan again." The app does not fall back to a stored TV.
- **TV button can't select Allow.** The app says so plainly and tells her she needs the remote. It does not pretend the phone can replace it on every model.
- **Wake has no pass.** Later, the Wake slot is disabled: "No wake pass for this TV. Use the TV's power button, or check TV details." The app does not send a magic packet as a guess.

**What it proves.** First pairing can work without a remote only when the TV's controller button can answer the Allow prompt. The app's job is to name that path, show the button, and admit the limit.

### Flow 2 — "The search box" (Marcus, 39)

**Scene.** A house, Friday 7:40pm. The living room has a 2022 Samsung; the bedroom has a 2016 set. The physical remote is on the coffee table, but its on-screen keyboard is slow and the remote sometimes lags. Marcus and his partner want to find a movie, then show a 30-second phone video.

**Goal.** Find the movie quickly, then cast the phone video. Use the phone keyboard where it actually works; do not waste ten minutes pecking at a streaming-app grid.

**Steps.**

1. Marcus opens UniRem. Two TVs are stored. The app does not silently pick the last one; the TV switcher opens with both cards. He picks **Living Room TV**. The topbar dot is mint; the ring is idle.
2. He taps **Text**. The sheet says "Text to Living Room TV" and "Typing goes to the TV's focused field." He opens the TV browser's search field, then returns to the phone.
3. He types the movie title and taps Send. The command moves to "Sent"; the sheet says "Sent. Check the TV." The title appears on the TV. He taps "It's there"; the sheet says "You confirmed" and clears the draft. If the app has a machine observation, it says "TV confirmed" instead. It never says "Text sent successfully." He picks the movie with the D-pad and presses play. The movie starts.
4. Later, he tries a streaming app's search. The sheet's standing helper says "Phone typing works in TV settings and the browser. If this app draws its own keyboard, use the D-pad here." He closes the sheet and uses the D-pad; he does not lose ten minutes to a promise the app cannot keep.
5. After the movie, he taps **Cast**, picks the 30-second video from the system Photo Picker, and sees the active TV already named. The app checks the renderer. The sheet warns "This TV may not show external subtitles." He chooses "Play without subtitles" or "Try anyway." The video starts. The session chip says "Casting to Living Room TV"; the foreground notification offers Stop; the phone can lock and the video keeps playing.
6. Later he switches to the **Bedroom TV (2016)** to check something. The TV card says "Best-effort — this TV can't confirm commands." When he presses a key there, the ring stays amber; the app does not borrow the living room TV's confidence. The cast session chip still says "Casting to Living Room TV"; switching TVs does not stop it. Tapping the chip returns to the cast.

**Climax beat.** The phone-typed title lands in the TV's native field, then the movie starts. The app's value is specific: type where the TV accepts it, and say where that is before the attempt.

**Failure paths.**

- **Nothing appeared.** "Nothing appeared? Open a text field on the TV and try again." / "Try again" / "Close." The draft stays until close; no automatic re-send.
- **Streaming-app grid.** The helper says "This app draws its own keyboard. Use the D-pad here." No send attempt is encouraged.
- **Cast format unsupported.** "This TV may not play this file." / "Choose another" / "Try anyway" (if the renderer accepts the attempt). The app does not start a session it knows is likely to fail without saying so.
- **TV drops during cast.** "TV didn't answer — retry?" / "Retry" / "Not now." The cast may stop; the app says "Cast stopped" with the reason and "Start again" if the file is still available. No silent restart.
- **Mirror without receiver.** If he tries Mirror, the sheet says "Mirror needs the UniRem receiver on your TV." and offers setup. No capture consent is requested.
- **Wake without a pass.** The Wake slot is disabled with "No wake pass for this TV." No magic packet is sent.

**What it proves.** The phone is a better input device where the TV exposes a native field, and a worse one where it doesn't. The app stays useful by making that boundary visible before the user hits it. Sessions stay pinned; the older TV stays labeled.

### Supporting flow — Voice, without a raw audio channel

1. Marcus taps **Voice**. The sheet explains "Hold to talk. Slide up to lock." If mic permission is not granted, the explainer says "Speech is handled by your phone. UniRem doesn't send audio to the TV." He allows it.
2. He holds the 72dp mic key and says "volume up." The transcript appears; the parsed command shows "Volume up." On release, the command executes; the ring reports Submitted → Observed/Unconfirmed exactly like a key press.
3. He holds again and says "search for the movie." The app shows the parsed command; if the key is supported, it sends Search. It does not pretend to type into the streaming app's grid.
4. For dictation, he slides up to lock, speaks a title, taps Done, reviews the transcript, and taps "Send to TV." If he hasn't confirmed that a native field is open, the sheet says "Open a text field on the TV, then come back" and offers "Use the D-pad."
5. For "power off," the sheet requires a tap on the destructive action. A misheard power command never fires on release. Raw audio never goes to the TV; no recording is retained.

### Supporting flow — Mirror, receiver first

1. He taps **Mirror**. The app checks for the UniRem receiver on the TV before asking for capture permission. If it is absent: "Mirror needs the UniRem receiver on your TV." + setup steps + "Check again" / "Not now."
2. If present: "Your phone screen will be shared until you stop." / "Start mirroring." The Android capture consent appears. If denied: "Screen capture wasn't allowed." / "Open settings" / "Not now."
3. During mirroring: session chip "Mirroring to Living Room TV"; foreground notification with Stop. The app does not promise screen-off. Stop asks "Stop mirroring?" / "Stop" / "Keep mirroring." If capture ends, the app says "Mirror stopped" and offers "Start again."
4. No receiver means no capture. The app never starts MediaProjection hoping the receiver appears, and it never installs anything on the TV silently.

### Supporting flow — Wake, only with a pass

1. He taps **Wake**. If the matrix has no pass, the slot is disabled: "No wake pass for this TV. Use the TV's power button, or check TV details." No packet is sent.
2. If there is a pass, the sheet shows the checklist: plugged in, same network, "Power On with Mobile" on, MAC saved. He taps Wake.
3. "Waking. This can take up to a minute." Success is reported only when the TV answers on 8001/8002. Timeout says "The TV didn't wake. Try the TV's power button." + "Try again" / "Close." No loop, no false success.
4. Power off remains a separate key. Wake never becomes a toggle.

### Supporting flow — Pad, probe first

1. Marcus taps **Pad**. If the TV has no passing pointer probe, the sheet says "Check pointer" and explains, "This will move the TV's pointer. You can stop anytime." He taps "Check pointer."
2. The pointer moves on the TV. If it moves and clicks where he aims, the probe passes and the Pad sheet opens. If it does not, the Pad slot becomes disabled with "This TV didn't pass the pointer check. Keys still work," and TV details records "Not supported."
3. In the Pad sheet, drag moves the pointer; tap clicks. The hero D-pad never becomes a trackpad, and the Pad sheet never opens without the probe. Back or Close returns to Remote.
4. If a streaming app ignores the pointer, TV details records the limit; the app does not claim the pad works everywhere.

## Inspiration & Anti-Patterns

**Kept from the physical remote.** The D-pad cluster, the volume rocker, the separation of power and wake, the idea that keys have places your thumb learns. The phone should feel like a remote before it feels like an app.

**Kept from the best phone remotes.** A phone keyboard that replaces pecking, a clear target TV, and a cast session that survives screen-off. Rejected: swipe-only navigation, hidden long-presses, and a touch surface that pretends to be a D-pad.

**Kept from SmartThings.** The TV switcher and the device card as a place to verify identity. Rejected: the cloud account, the permission catch-22, and capabilities that appear only after they fail.

**Kept from BubbleUPnP and Jellyfin.** Explicit renderer choice, format warnings, and the understanding that DLNA support varies by model. Rejected: ad-filled utility apps and the idea that "cast" means "any file will play."

**Kept from the reference boards.** Noir's bottom-third composition and ring, Native's pairing card, Tactile's best-effort plate, Signal's state line and offline banner. Rejected wherever they conflict with the accepted decisions: the topbar dot is health, not outcome; offline keys are disabled, not "still send"; the pairing copy must include the no-remote path.

**Anti-patterns.**

- A smart-home dashboard with cards, tabs, and status tiles. UniRem is a remote.
- Green checkmarks, confetti, or "Success!" before the TV confirms. The ring and its state line are the success signal, not confetti.
- Spinners for pairing, discovery, or wake. Text states and the ring are enough.
- "Universal," "works with any Samsung TV," "seamless," "magic," "just." The product's credibility is per-model and specific.
- Silent queues, silent retries, and silent fallback to the last TV. Every send is explicit; every target is chosen.
- Mirroring without a TV-side receiver; raw voice-audio injection; always-listening microphones; "type anywhere."
- A permission wall on first launch. Permissions appear when the feature needs them.
- Autoplay, playlists, queues, or a media browser. Cast is one selected item, with transport.
- Hiding a gated capability with no explanation. Disabled slots stay visible and explain themselves.
- Using the ring for connectivity or the topbar dot for command outcome. The two channels never swap.

## Responsive & Platform

**Form factor.** Android phone, portrait-first, 360–480dp. Tablets, foldables in tabletop mode, web, iOS, and desktop are not V1 surfaces. On a large phone or unfolded device, the column centers at 480dp max; the D-pad does not grow. Landscape does not get a separate layout; the same column centers, and the launcher stays at the bottom. If the viewport is too short for the full hero, the quiet space collapses before the ring or targets shrink.

**System UI.** Edge-to-edge, transparent status bar with light icons, transparent navigation bar with a scrim only when a sheet is open. Respect cutouts and gesture insets. The launcher's bottom padding includes the safe area. No immersive mode; the status bar remains available.

**Android platform.**

- minSdk 26, targetSdk 36.
- Local network permission: explain in context before the system prompt; manual IP is always offered as a fallback; when permission is denied, both discovery and manual IP are blocked with a plain explanation; client isolation is explained in diagnostics.
- Microphone: only while the Voice sheet is active and the user is holding/locked; no background capture; no wake word.
- MediaProjection: only after the receiver check and the user's "Start mirroring"; the system consent is never pre-requested.
- Notifications: POST_NOTIFICATIONS for cast/mirror foreground-service notifications; if denied, the session can run while the app is open, and the app explains that background control needs the notification.
- Photo Picker for photos/videos and the system document picker for audio; no broad storage permission.
- No accessibility service, no overlay permission, no VPN, no device admin.
- Foreground services use the correct service types for cast and mirror; the remote socket closes when the Remote surface leaves the foreground.
- Predictive back is supported by the platform; sheets dismiss with the system gesture. Full-screen secondary surfaces use the platform back stack; the app adds no custom gesture navigation.
- Haptics use the platform API; reduced motion follows the system animator scale. Dark-only; no dynamic color, no light theme.
- No custom keyboard; the system IME handles text and autocorrect. Secure entry is the platform's behavior.

**Text and layout.** `sp` for type, `dp` for spacing. 200% font scale supported. At 200%, the launcher is 3×2, function labels wrap, the state line wraps, and sheets scroll. No critical label truncates. Long TV names truncate in the topbar but show in full in the switcher and details.

**RTL and locale.** Text and rows mirror; D-pad arrows and media transport do not. Strings are externalized; no concatenation; 1.3× expansion tolerated. Dates/times use the locale; state words stay short.

**Motion.** The only motion is the ring breath, the observed hold transition, the sheet transition, and the pressed-key scale. Reduced motion removes the breath and scale and turns the sheet transition into a fade; color and text change instantly. No autoplay, parallax, or looping animation.

## Assumptions & Open Questions

These are design commitments that the matrix or user testing must validate. They are not protocol claims.

- **Unconfirmed timeout.** 3s is a provisional threshold for Submitted → Unconfirmed. Spike A must measure normal key latency per model; if 3s is too short, raise it. The UI must not blame the TV for a design threshold.
- **Manual observation.** "It worked" is an explicit user confirmation that produces "You confirmed," never "TV confirmed." Validate whether users understand the difference and whether the action is needed for keys/text that lack a machine event.
- **Text focus.** There is no reliable focus event across Tizen apps. The sheet uses a helper and explicit Send; it does not claim to know the field. Voice dictation uses the same user-confirmed handshake. Test whether users open the field first, and whether "Nothing appeared?" is clear.
- **Pairing without a physical remote.** The TV's controller button can select Allow on many Samsung models, but not all; the app must say so. The spec's assumption still names the physical remote; Flow 1 treats the button as a documented fallback, not a guarantee.
- **Wake checklist.** The exact "Power On with Mobile" path varies by model and firmware. The sheet should use plain language and a "Where is this?" link, not a hard-coded menu path that may be wrong. Wake claims remain per model and network type.
- **Cast session end.** The app defines session end as user Stop, renderer stop/error, TV disconnect, or app force-stop. Validate renderer error codes and whether a stopped TV can be distinguished from a paused one.
- **Mirror receiver.** V1 distribution is a dev-mode sideload. The setup sheet can explain, but it cannot install the receiver. Validate the support burden and whether "Check again" is enough.
- **Power-off observation.** Power off may be unconfirmed on models without an off-state event. The app must be willing to show amber after power off; it must not show a green "TV confirmed" from a socket write.
- **Haptics.** The 8ms/12ms/20ms patterns are a starting point; test distinguishability without becoming noisy.
- **Idle ring contrast.** `ring-idle` is 1.56:1 on the base and is a deliberate exception because the state line always names "Idle." If a future design relies on the ring alone, raise the token.
- **No user research.** The journeys are composites from forum and support evidence. They are not representative, and they are not a substitute for watching real users pair, type, cast, and recover.
- **Visible gates vs. CAP-3's "hidden" wording.** CAP-3 says the pointer feature stays hidden on unsupported models; the accepted six-slot launcher decision keeps the slot visible but disabled and explained. The action is unavailable; only the explanation is visible. If the spec wording is strict, collapse the slot into a single "Why?" entry.
- **CAP-4's raw-audio gate.** The spec keeps raw-audio injection behind a voice spike gate and out of launch commitments. V1 uses phone-side recognition and mapped keys/text only; no raw audio is injected. Any future raw-audio path is a gate, not a promise.

## What This Spine Does Not Decide

- The wire protocol, ports, TLS details, and exact Tizen key codes. Those live in the protocol reference and the provider layer.
- The final verification matrix results. The app's capability labels come from the matrix, not from optimism.
- The Tizen receiver packaging and sideload instructions beyond the setup copy.
- The exact speech recognizer used by the phone. The app owns the permission, transcript, and mapping; the platform owns recognition.
- Visual values are owned by `DESIGN.md`. This spine references tokens and repeats only the target sizes needed to describe behavior.
