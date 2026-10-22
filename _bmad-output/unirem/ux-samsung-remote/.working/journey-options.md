# Journey options — grounded in online evidence

Status: accepted — both journeys, A opens into B. User chose the blend on 2026-10-22.

Purpose: replace the invented "Nadia" straw man with two named-protagonist journeys built from recurring, publicly reported Samsung TV owner experiences. These are **composites**, not interviews. Names, ages, rooms, and times are narrative devices; the behaviors and failure modes are sourced below.

## Evidence base

**Important limit:** this is forum and support-document evidence, not representative user research. It establishes that these failures happen and how they feel; it does **not** establish how often they happen, or that any one segment is the majority. Quotes are verbatim from public threads or official support pages. "Observed" below means an official Samsung page or documented platform behavior; "Forum-reported" means a public community thread.

| ID | Source | Type | What it grounds |
|---|---|---|---|
| S1 | [SmartThings Community — Use phone keyboard on SmartThings to control Samsung smart TV?](https://community.smartthings.com/t/use-phone-keyboard-on-smartthings-to-control-samsung-smart-tv/167840) (created 2019-07-09, ongoing) | Forum-reported | The phone-keyboard gap in Samsung's own app. "Typing with the remote is really worthless"; "Pecking away passwords with the remote is terrible UX." |
| S2 | [r/SmartThings — Connect SmartThings without requiring a remote](https://www.reddit.com/r/SmartThings/comments/rd6lv1/connect_smartthings_without_requiring_a_remote/) (2021) | Forum-reported | The pairing catch-22. The app asks the user to confirm on the TV with the remote they do not have. |
| S3 | [r/SmartThings — "holy shit this is the worst invention for tv remote"](https://www.reddit.com/r/SmartThings/comments/1k0b968/holy_shit_this_is_the_worst_invention_for_tv) (2025) | Forum-reported | Lost-remote desperation, "registering your device" for 15+ minutes, repeated PIN prompts. |
| S4 | [r/techsupport — Lost Samsung TV remote, can't connect to WiFi without it](https://www.reddit.com/r/techsupport/comments/ju4i20/lost_samsung_tv_remote_cant_connect_to_wifi) (2020) | Forum-reported | The pet ate the remote; the phone app cannot connect because the TV is not on Wi-Fi. |
| S5 | [r/samsung — Lost the remote to my Samsung TV, trying to use SmartThings](https://www.reddit.com/r/samsung/comments/1f7cujt/lost_the_remote_to_my_samsung_tv_trying_to_use) (2024) | Forum-reported | The obscure workaround: press the TV's physical button, then press and hold to select Allow. |
| S6 | [Samsung AU — How to Control a Samsung TV without a Remote](https://www.samsung.com/au/support/tv-audio-video/control-tv-without-remote/) (updated 2026-10-07) | Observed (official) | The controller button exists, but has "limited functionality"; SmartThings requires the TV to be connected first. |
| S7 | [Samsung US — Control your Samsung TV or projector with your phone using SmartThings](https://www.samsung.com/us/support/answer/ANS10007047/) | Observed (official) | SmartThings requires a Samsung account, internet, and the TV already added as a device. |
| S8 | [r/4kTV — Samsung smart TV keeps asking for permission to use...](https://www.reddit.com/r/4kTV/comments/n69df7/samsung_smart_tv_keeps_asking_for_permission_to) (2021) | Forum-reported | The TV's permission prompt recurs on some firmware even after pairing. |
| S9 | [r/SmartThings — No keyboard to search in TV apps in SmartThings?](https://www.reddit.com/r/SmartThings/comments/13grr6q/no_keyboard_to_search_in_tv_apps_in_smartthings) (2023) | Forum-reported | The exact text-scope gap: phone keyboard does not reach streaming-app search grids. |
| S10 | [r/samsung — SmartThings App - no keyboard?](https://www.reddit.com/r/samsung/comments/rzk59j/smartthings_app_no_keyboard) (2022) | Forum-reported | Same gap on a new Samsung TV; the app's keyboard function does not work for typing. |
| S11 | [Samsung UK — How to control your TV from mobile SmartThings app](https://www.samsung.com/uk/support/tv-audio-video/how-to-control-your-tv-from-mobile-smartthings-app/) (updated 2022-03-30) | Observed (official) | Samsung claims mobile-keyboard search only in the **Search menu**, not arbitrary app grids. |
| S12 | [r/samsung — Samsung 65" smart tv remote lag](https://www.reddit.com/r/samsung/comments/1kk9l3w/samsung_65_smart_tv_remote_lag) (2025) | Forum-reported | 5–10 second remote delay; the physical remote is not a reliable fast path. |
| S13 | [Jellyfin issue #2629 — No external subtitles (.srt) on 10.5.0 using DLNA](https://github.com/jellyfin/jellyfin/issues/2629) | Forum-reported / verified bug report | External SRT does not display via DLNA on a Samsung RU7400. |
| S14 | [WD Community — No SRT subtitles via DLNA on Samsung UE48JS9000](https://community.wd.com/t/no-srt-subtitles-via-dlna-on-samsung-ue48js9000-with-my-book-live-4tb/96327) | Forum-reported | Older Samsung sets display SRT via DLNA; a newer set does not. Per-model/firmware variability. |
| S15 | [r/Chromecast — Which app can cast video AND subtitles?](https://www.reddit.com/r/Chromecast/comments/jui5pv/which_app_can_cast_video_and_subtitles) | Forum-reported | Users hunt for workarounds; BubbleUPnP handles video + subtitles where the default path does not. |
| S16 | [r/daddit — The toddlers hid the TV remote](https://www.reddit.com/r/daddit/comments/1os4py2/the_toddlers_hid_the_tv_remote) (2025) | Forum-reported | Lost remotes are a recurring household event; parents buy spares. |
| S17 | [r/SmartThings — Why do my Samsung TVs constantly go offline?](https://www.reddit.com/r/SmartThings/comments/125ofsz/why_do_my_samsung_tvs_constantly_go_offline) (2023) | Forum-reported | Multi-TV persistence is fragile; users delete and re-add devices. |
| S18 | [r/TheFrame — Two Samsung TVs in one room, remote triggers both TVs](https://www.reddit.com/r/TheFrame/comments/1qas6zv/two_samsung_tvs_in_one_room_remote_triggers_both) | Forum-reported | Multiple Samsung TVs in one home; explicit target selection matters. |
| S19 | [r/AndroidQuestions — How do I access the built-in screen mirroring function](https://www.reddit.com/r/AndroidQuestions/comments/1abppll/how_do_i_access_the_builtin_screen_mirroring) | Forum-reported | Users resent ad-filled third-party mirroring apps and cannot find the built-in path. |
| S20 | [SmartThings Community — Samsung TV - Can't Turn On](https://community.smartthings.com/t/samsung-tv-cant-turn-on/114960) | Forum-reported | Power-off works; power-on fails. Wake is a separate, unreliable path. |

### What the evidence does not establish

- No prevalence: we cannot say "most users lose their remote" or "text entry is the #1 problem."
- No usability tests: no one has watched a user try UniRem.
- No measured latency, failure rate, or support burden.
- No evidence that users want one app; that is the spec's product bet, not a research finding.
- No demographic data; the protagonists' ages and household details are composites.

## Option A — "The remote is gone"

**Journey type:** recovery / first-run. **Core tension:** trust at the threshold.

**Protagonist:** Ruth, 43. A rented flat with her partner and a two-year-old rescue dog. It is 8:20pm on a Tuesday. The living room has a 2019 Samsung TV. The remote disappeared after dinner — under the sofa, or in the dog's bed. The TV is on, stuck on the home screen, volume low.

**Goal:** finish the episode she and her partner started. She needs volume, then play. She downloads UniRem because it is the only remote in the flat.

**Obstacle:** The TV's first pairing shows Allow/Deny on the TV. The app cannot press it. The phone-app pattern is a catch-22: the app asks the user to confirm with the missing remote. Samsung's own answer is the TV's controller button, which is obscure and limited. The app also needs local-network permission and discovery; if the TV is on another band or the network isolates clients, discovery fails and manual IP is the fallback. The emotional failure mode is a spinner: "registering your device" for 15 minutes, or a PIN prompt that repeats.

**Climax:** The pairing card says, plainly: "First pairing needs one press on the TV. The button is under the Samsung logo." Ruth finds it, presses once, the TV highlights Allow, she presses and holds to select. The app's ring goes from idle to observed. She presses volume up; the TV responds. The room exhales.

**Honesty beat:** The app never says "connected" until the TV confirms. If Allow times out, it says "The TV didn't answer" and offers "Try again" plus the button diagram. If Wake has no matrix pass for this model/network, the Wake slot says so instead of failing. It does not pretend the phone can replace the physical remote for first pairing. That honesty — naming the limit before the user hits it — is the product's differentiator.

**What it stresses:** first-run discovery; manual IP; pairing/Allow; re-pair and repeated prompts; local-network permission; physical-remote dependency; the first observed key; honest timeouts.

**Evidence:** S2, S3, S4, S5, S6, S7, S8, S16, S20. The pattern recurs across unrelated causes (pet, toddler, moving), which is why the scenario is credible; it is not a prevalence claim.

**Design implications:**
- Onboarding is a "find the button" moment, not a progress spinner.
- The first promise is "we'll tell you what the TV needs," not "remote-free."
- A pairing timeout is a designed state with a diagram and retry, not an error toast.
- The first observed key is the emotional payoff; the ring breath earns it.

## Option B — "The search box"

**Journey type:** everyday / media session. **Core tension:** convenience vs. scope honesty.

**Protagonist:** Marcus, 39. A house with his partner and two kids. It is 7:40pm on a Friday; the kids are finally down. The living room has a 2022 Samsung; the bedroom has a 2016 Samsung. The physical remote is on the coffee table, but its on-screen keyboard is slow and the remote itself sometimes lags 5–10 seconds.

**Goal:** find a movie before his partner gives up and goes to bed, then show a 30-second phone video from the week.

**Obstacle:** The phone keyboard should be the obvious fix, but the TV's streaming apps draw their own keyboards. Phone text injection only works in native TV fields — Settings, the browser URL field, some search fields — not in the Netflix/YouTube/Prime grids. He tries to type into a streaming app's search and nothing appears. The cast also has a subtitle trap: the video file has an external SRT; the TV may not display it over DLNA. The bedroom TV is best-effort and may not confirm keys.

**Climax:** He opens the TV's native browser/Settings search (or the app's Search menu), types the movie title on the phone keyboard, and the glyphs appear on the TV. The bottom sheet says "Sent" then "TV confirmed." For the streaming app, the app had already said, before he tried: "This app draws its own keyboard. Use the D-pad here." He does not waste ten minutes pecking. The phone video casts; the app warns "External subtitles may not show on this TV" and offers "Play without subtitles" or "Try anyway." The evening works, with no false promises.

**Honesty beat:** The app explicitly distinguishes native fields from app-drawn grids; it never markets "type anywhere." It marks the 2016 bedroom TV "Best-effort — this TV can't confirm commands." It labels cast format/subtitle support per model. It does not show a green ring when the TV is silent.

**What it stresses:** text-entry scope; focus state; explicit send/cancel; cast flow; subtitle gating; multi-TV; best-effort labeling; per-press truth.

**Evidence:** S1, S9, S10, S11, S12, S13, S14, S15, S17, S18. Samsung's own support says the mobile keyboard works in the **Search menu**; users report it does not reach streaming-app grids. That gap is the journey.

**Design implications:**
- Text entry is a field-bound tool, not a universal keyboard.
- The app explains the surface boundary before the user types, not after a failed send.
- Casting surfaces subtitle/format limits before play; "try anyway" is a choice, not a surprise.
- Multi-TV status is per TV; a best-effort set never borrows the newer TV's confidence.

## Recommendation

**Use both, in sequence.** Option A is the first-run prologue: the first 10 minutes establish whether the product tells the truth when the user is most vulnerable. Option B is the everyday session: weeks later, the same person uses the app for the small, repeated jobs that make it worth keeping. Two named flows, one relationship.

If the spine must have a single core journey, **A is the stronger design test** (it tests the hardest limit) and **B is the stronger product story** (it shows daily value). The blend avoids choosing between them and gives the EXPERIENCE spine a natural arc.

## Open choice

Ask the user to pick:
- Both, A opening into B (recommended)
- A only
- B only
- Neither; user will narrate the real session
