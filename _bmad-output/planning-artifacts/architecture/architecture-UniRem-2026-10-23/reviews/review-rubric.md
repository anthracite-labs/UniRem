# Rubric review — UniRem architecture spine

- **Artifact:** `_bmad-output/planning-artifacts/architecture/architecture-UniRem-2026-10-23/ARCHITECTURE-SPINE.md` (2026-10-23, status: draft)
- **Reviewer:** rubric walker (subagent of the architecture reviewer gate)
- **Method:** full read of the spine; targeted reads of `spec-samsung-remote.md`, `protocol-reference.md`, `verification-matrix.md`, `EXPERIENCE.md`, the research report the spine cites, and the architecture `.memlog.md`; independent web spot-checks of the named stack (tagged ✓ below). No edits made.
- **Verdict:** **pass-with-fixes** — the spine is unusually disciplined for its altitude; the module shape, three-clock split, capability ladder, session ownership, trust boundary and cast bound all hold up. Six findings need to land before implementation starts, two of them because an AD as written cannot be enforced or silently blocks a spec-mandated capability.

---

## Findings (ordered by severity)

### F1 — HIGH — AD-8 either blocks wake (CAP-5) or is silently bypassable

**Checklist item:** 2 (enforceability), 6 (CAP-5).

AD-8 says cleartext is gated by "exactly one OkHttp interceptor [that] rejects any host that is not RFC1918 or link-local"), the jUPnP transport gets "the same predicate", and "**No custom socket factory or raw socket may bypass the check**." The Consistency Conventions repeat it as "Every outbound host passes the private-address predicate."

The wake path cannot satisfy that as written:

- `protocol-reference.md` (Wake packet) requires the magic packet to be sent to the **subnet-directed broadcast plus `255.255.255.255`**. `255.255.255.255` is not RFC1918 and not link-local, so a literal predicate rejects it; the subnet-directed broadcast (`192.168.x.255`) only survives if the predicate is implemented as CIDR containment rather than a host-class check, which the spine does not specify.
- The packet goes out over a **`DatagramSocket`**, which is exactly the "raw socket" AD-8 forbids and which no OkHttp interceptor can see.
- CAP-5's success definition (`spec-samsung-remote.md`, CAP-5: "reports success only when the TV answers on 8001/8002") depends on that packet going out.

Net effect: two implementers will resolve this differently — one exempts wake, one writes the predicate strictly and silently kills wake, one "bypasses" the rule at the UDP edge. Either way AD-8 stops being the single, checkable egress gate it claims to be.

**Concrete fix:** scope the rule and name the exception: the predicate governs TCP/HTTP peers (OkHttp interceptor in `:tv`, jUPnP transport + SSDP result filtering in `:cast`); the wake sender in `:tv/wake` is the one named UDP exception — destination is the subnet-directed broadcast computed from the active `LinkProperties` plus `255.255.255.255`, ports 9 and 7, socket bound to the private interface, and the exception is stated in both AD-8 and the network-security-config comment.

### F2 — HIGH — the capability model has no `surface` dimension, but CAP-2 and CAP-3 are surface-scoped

**Checklist item:** 1 (missed divergence point), 6 (CAP-2, CAP-3).

The spec's text and pointer capabilities are explicitly per-surface:

- CAP-2: "app-drawn character grids are excluded **unless a per-surface probe passes**."
- CAP-3: "the feature stays hidden elsewhere" / "passing the cursor probe" — the research and `EXPERIENCE.md` carry a browser-vs-grid pointer matrix.
- `EXPERIENCE.md` requires per-surface records: "A recorded per-surface limit or a passing probe can make the second sentence more specific"; "TV details records the limit" for a grid whose pointer is ignored; the Text states "Text, app-drawn grid".

AD-4/AD-5 and the ER diagram key everything capability-only (`CAPABILITY_OBSERVATION {capability, label}`, `PROBE_RESULT {capability, …}`, `CHIP_DISMISSAL {capability, firmware}`), and the matrix is keyed model/firmware/network. There is nowhere for "text Works on the native/browser surface, unknown on this grid" or "pointer follows in the browser, ignored in the streaming app" to live. Two implementers will model this differently (extra Room tables, key hacks, or over-claiming "Works" from the native-surface pass).

Related defect in the same model: AD-4 lists "IME events" as a **declared surface** alongside the UPnP service/action list and `TokenAuthSupport`. A declaration is a device capability inspectable before/at connect; `ms.remote.imeStart/imeEnd` are per-field runtime events. As written, ladder rule (1) ("a declared surface is absent → Not supported") cannot be evaluated for text — the events may simply never fire if no native field is focused — and rule outcomes get muddy.

**Concrete fix:** add a `surface` axis (`NativeIme`, `Browser`, `AppGrid`, `PointerSurface`, …) to probe results, observations and resolver inputs; have the resolver return a per-surface verdict where the capability is surface-scoped; keep the matrix at model/firmware/network and/or per-surface test columns. Restate declared surfaces as device-level only (UPnP service/action list, `TokenAuthSupport`) and classify IME events as `TvEvent` evidence.

### F3 — MEDIUM — "no module caches a resolved label" collides with the durable label UX requires

**Checklist item:** 1, 2, 6 (CAP-8's persistent per-TV truth).

AD-4's rule ends: ":core resolves from inputs; `:app` supplies matrix rows and probe results; **no module caches a resolved label independently**." The ER diagram stores `CAPABILITY_OBSERVATION {capability, label}` in Room, and the Persistence convention gives Room "capability observations". Meanwhile `EXPERIENCE.md` (Pairing and TV details rules) says "The durable capability label in TV details is the source of truth," and the chip rules define its invalidation triggers (re-pair, firmware change, probe result change).

So the app must persist a per-TV answer, but the spine both stores a `label` column and forbids caching a label, and it never defines when a persisted resolution is invalidated or recomputed (matrix version bump, firmware change, probe change, re-pair). Offline TV details is the forcing case: declaration-derived results (UPnP service list, `TokenAuthSupport`) are only observable while connected, so a live-resolve-only reading of AD-4 loses them.

**Concrete fix:** state the resolution lifecycle explicitly — either rename `CAPABILITY_OBSERVATION.label` to a raw input and require re-resolution, or define the persisted resolution as an `:app` concern: inputs' versions (matrix version + firmware + probe records) are recorded, the label is recomputed on any input change, and a declaration observed while connected is persisted as an *input*, not as a label.

### F4 — MEDIUM — AD-1's "only cross-module declarations are…" list is inaccurate and unenforceable

**Checklist item:** 2 (enforceability), 1.

AD-1 permits exactly "`TvControl`, `RendererGateway`, `CapabilityResolver`, `CastSession`, and their input/output types; every other declaration is `internal`." The real public surface is larger:

- AD-2 has `:app` call `CommandTracker.submit()` and consume its `StateFlow<CommandOutcome>` — `CommandTracker`, `CommandOutcome` and `Evidence` are cross-module and are not I/O types of the four seams.
- The Structural Seed puts a "PairingStore implementation" in `:app/data`, implying a `:tv`-side `PairingStore` port implemented by `:app` — a fifth seam.
- `:core`'s shared IDs (`TvId`, `SessionId`, `MediaId`) and the sealed failure types are used by `:app`, `:tv` and `:cast`.
- The Logging convention requires a shared `RedactedLog` wrapper across modules.
- AD-8 requires one private-address predicate shared by `:tv` (OkHttp interceptor) and `:cast` (jUPnP transport + SSDP filtering) — none of these are seam I/O types.

As written, an implementer honoring AD-1 literally would keep the predicate and logger `internal` and then cannot satisfy AD-8/logging; another would invent a `:common` module. This is the exact seam-rule divergence the spine claims to prevent.

**Concrete fix:** restate the allowlist in three buckets — the four seam interfaces + their I/O types; `:core`'s shared model/utility surface (IDs, failures, `CommandOutcome`/`Evidence`, private-address predicate, `RedactedLog`); and each module's single public state-owner type (`CommandTracker`, `CastSession`) plus the `:tv` → `:app` storage ports (`PairingStore`). State how it is checked (module dependency review or a Gradle-level check).

### F5 — MEDIUM — the operational/environmental envelope is mostly undecided and undeferred

**Checklist item:** 8 (deployment & environments, infra/provider strategy, operations).

Nothing in the spine decides, defers or asks about: app distribution/release track and signing (only the application-ID question is open); build variants beyond "BuildConfig flags"; CI or who runs the Spike A/B/C/L physical verification and where results land; or the **telemetry/crash-reporting policy**. That last one is load-bearing rather than cosmetic: AD-8's predicate makes any analytics/crash SDK an AD-8 violation, and `EXPERIENCE.md` promises the user "Nothing is sent to the internet" — the line between "the app quits making network egress" and "we deliberately have no telemetry" should be a decision, not an inference two units make. The same section is the natural home for the platform envelope that is currently scattered:

- FGS service types are never named (research §6.3 leaves cast as `mediaPlayback` vs `connectedDevice`; mirror as `mediaProjection`), yet they are a one-time manifest + Play declaration decision.
- The `MulticastLock`/`CHANGE_WIFI_MULTICAST_STATE` needed for SSDP in both `:tv` discovery and `:cast`'s jUPnP has no owner; AD-11's "LanAccessGate owns permission requests and diagnostics" implies but does not state it.
- AD-11 says "Manual IP entry is always offered" while `EXPERIENCE.md` (Local network denied) says both scan and manual IP cannot reach the TV until permission is granted.

**Concrete fix:** add a short "Operations & platform envelope" section (or 4–5 Deferred/Open entries): V1 distribution + signing owner; telemetry = none in V1 (or the AD-8 exemption process for adding it); permission/manifest inventory with owners (`LanAccessGate`: local-network access, multicast lock, POST_NOTIFICATIONS; Activity: MediaProjection; Voice sheet: RECORD_AUDIO); FGS service types pinned per session type.

### F6 — MEDIUM — matrix harness rows have nowhere to live, contradicting the read-only-asset rule

**Checklist item:** 3 (Deferred/short-tail divergence), 2 (AD-5).

AD-5: "A debug harness may append observed-effect rows with the same schema." Persistence convention: "Matrix data is a read-only asset, not a Room table." So the harness's appended rows cannot go into the asset (read-only) and cannot go into Room (excluded by convention) — yet they must be readable by the same resolver, and the release gate "must be readable by the code that makes the claim" (memlog entry 36). Two implementers will fork: debug-instrumented asset writes, a Room overlay, or a DataStore blob; and the resolver's merge/precedence semantics (harness row vs asset row for the same model+firmware) are undefined.

**Concrete fix:** pin it in AD-5: harness writes to a debug-only Room table (or a debug asset overlay); the resolver takes `assetRows (release+debug) + harnessRows (debug builds only) with defined precedence`; release builds read the versioned asset only, and harness rows never satisfy a release gate.

### Low-severity observations

- **L1 — Deferred / Backoff curve:** the entry claims "the type and owner are fixed here," but no connection-health type or owner is ever named in the spine (AD-3 says only "separate types with separate owners"). Name it (e.g. `ConnectionHealth` in `:tv/channel`) or move it to Open Questions.
- **L2 — Deferred / UPnPCast:** the spine narrows the spec constraint (spec §Constraints: adoption if "Central provenance resolves **or a pinned artifact is formally accepted**", i.e. the JitPack + commit-pin route the research defined) to "only if its provenance resolves". Either carry both conditions or record the deliberate tightening.
- **L3 — AD-11:** "owns permission requests" reads as *all* permissions and collides with AD-6 (MediaProjection requested by an Activity) and the UX (mic inside the Voice sheet, notifications in context). Scope the sentence to local-network access.
- **L4 — AD-2:** "Cast transport and wake map their native results to the same three visual states" is under-specified — `EXPERIENCE.md` never shows Submitted/Unconfirmed semantics for cast transport (the session chip and transport UI own that surface). Say which surfaces render which states, or drop cast from that sentence.
- **L5 — Stack / Kotlin row:** with AGP 9's built-in Kotlin, AGP bundles its own Kotlin default (2.2.10 at AGP 9.0; still behind at 9.4) and 2.4.21 is obtained by putting the Kotlin Gradle plugin on the buildscript classpath — `kotlin-android` not applied per module, but not "free" either. Reword so the builder does not expect 2.4.21 without the classpath override.
- **L6 — CAP-4 has no architecture line of its own:** the spec and `EXPERIENCE.md` both make "raw audio never goes to the TV, no audio retained" load-bearing, and the voice command mapping ("the agreed set", which research §16.7 returned for user decision) has no owning artifact or version story, unlike the matrix in AD-5. One convention line plus one Deferred line (`voice mapping table — content owned by spec/UX, shipped as in-app data`) closes it.
- **L7 — Ktor CIO server:** the embedded media server relies on Ktor's server engine inside an Android app; that is community-validated rather than an officially supported target. If kept, record it as an accepted risk with the fallback (raw `ServerSocket` / NanoHTTPD alternative already named in the research), especially since AD-9's bound makes the seam cheap to swap.

---

## Checklist walk

**1. Fixes the real divergence points for the level below, misses none — PARTIAL.**
Strong: module graph and "no `:domain`/`:data`/universal-TV layer"; the three-clock split; the command-truth model (submit-once per gesture, stale-generation rejection, evidence-with-provenance); declared-vs-silent capability split; matrix as versioned code-readable data; session ownership (process-scoped cast/mirror vs UI-scoped socket); TOFU pin ordering; bounded cast serving; receiver-required mirroring; one LAN gate. Missed/at-risk: per-surface capability (F2), resolved-label persistence lifecycle (F3), the actual public-surface allowlist including the shared predicate/logger/ports (F4), multicast-lock and FGS-type ownership (F5), harness-row storage (F6). No other unit-level fork found: naming/IDs/time/errors/state/persistence/logging/testing conventions are all pinned or trivially unit-local.

**2. Every AD's Rule is enforceable and prevents its stated divergence — PARTIAL.**
AD-3, AD-6, AD-7, AD-9, AD-10 hold up as written (name the FGS types for AD-6, scope AD-11's permission sentence). AD-8 fails as stated (F1). AD-1's rule is over-broad (F4). AD-4's inputs are incomplete (F2/F3). AD-5's harness clause is unenforceable without a storage decision (F6). AD-2 is mostly type-enforced (generation + injected timeout + typed evidence); its caller rule ("submit exactly once per gesture") is a review/test rule and its cast/wake sentence is vague (L4).

**3. Nothing under Deferred could let two units diverge — PASS with two nits.**
`:mirror`/`build-logic`/design-system deferrals keep single owners; UPnPCast keeps the `RendererGateway` seam; matrix columns stay with bmad-spec; H/J is matrix-gated with "no separate code path"; receiver internals are out of scope. Nits: the backoff entry's claim that "the type and owner are fixed here" is not true of the current text (L1), and the UPnPCast entry quietly narrows the spec's win condition (L2). The deferred voice mapping (L6) is absent rather than wrong.

**4. Named tech is verified-current — PASS (spot-checked 2026-10-23), one wording nit.**

| Spine pin | Check | Result |
|---|---|---|
| AGP 9.4.0 | AGP 9.4.0 release notes (Sept 2026) | ✓ current; min/default Gradle 9.6.0 |
| Gradle 9.6.0 | AGP 9.4 compatibility table | ✓ equals AGP's required version (latest Gradle is 9.8.2 — pinning the minimum looks deliberate; say so) |
| Kotlin 2.4.21 | Kotlin docs/tooling (Compose compiler guide; KSP quickstart; a public project pairing Kotlin 2.4.21 + KSP 2.3.12 + BOM 2026.09.00) | ✓ exists and is in current use; wording nit L5 (built-in Kotlin needs the KGP classpath override to reach 2.4.21) |
| KSP 2.3.12 | same project pairing; KSP docs | ✓ plausible current; direct release note not retrieved |
| Compose BOM 2026.09.00 | Maven metadata (published 2026-09-09) | ✓ current |
| Room 2.8.5 | androidx release notes (2026-09-09) | ✓ current stable |
| Hilt 2.60.1 | Dagger releases / Hilt docs (2026-07-07) | ✓ current |
| jUPnP 3.0.5 | Maven Central `org.jupnp.android/3.0.5` (2026-08-12) + research §7.2 | ✓ current; matches spec constraint |
| Ktor CIO 3.6.0 | Ktor release notes (2026-09-17) | ✓ current (see L7 on Android server support) |
| compileSdk/targetSdk 36, minSdk 26 | Spec §Constraints; Play target-36 effective 2026-08-31 | ✓ ratifies spec |
| OkHttp 5.5.1, Activity Compose 1.13.0, Lifecycle 2.11.0, Navigation 2.10.2, DataStore 1.3.0, kotlinx.serialization 1.12.0, coroutines 1.11.0 | not individually confirmed | ⚠ residual; consistent with the memo's "web-verified 2026-10-23" claim (`.memlog.md`) |

**5. Ratifies rather than contradicts the spec's constraints — PASS.**
Line-by-line against spec §Constraints: targetSdk 36/minSdk 26 ✓; no compatibility claim without recorded observation ✓ (AD-4/AD-5); socket-write never presented as action ✓ (AD-2); TOFU pin before token, no trust-all, no silent downgrade, Keystore + backup-excluded + never logged ✓ (AD-7); remote socket UI-scoped, FGS only for user-started sessions ✓ (AD-6); TV-fetches-URL, HEAD+Range, bounded IDs, screen-off survival ✓ (AD-9); one broadcast domain + manual IP + isolation diagnostics ✓ (AD-11); wake per model+network only on recorded pass ✓ (AD-4/AD-5 + UX); cloud out of the hot path, keys/text/pointer LAN-only ✓ (Network conventions, AD-8); jUPnP 3.0.5 behind the seam ✓ (one tightening: L2). Spec's H/J "never counted toward pass claims" ✓ (AD-4 step 4). No contradiction found.

**6. Covers CAP-1..CAP-8 — PARTIAL.**
All eight appear in the Capability → Architecture Map with owners and governing ADs, and each capability's spec-level constraint is honored at the architectural layer (CAP-1 outcome states + unsupported reporting surface; CAP-2 explicit send + IME-scoped text; CAP-3 probe-gated pointer; CAP-5 matrix-gated wake; CAP-6 bounded serving ladder; CAP-7 receiver-first mirror; CAP-8 per-TV identity + re-pair + pinned sessions). Gaps: CAP-2/CAP-3's per-surface requirement is unmodelled (F2); CAP-4 is mapped but has no architecture statement of its own (L6); CAP-1's per-key "doesn't report this key" reporting relies on UX only — acceptable at this altitude, but worth one matrix note.

**7. Parent spine inheritance — N/A.** No parent spine exists; nothing to weaken.

**8. Every dimension this altitude owns is decided, deferred, or an open question — PARTIAL.**
Covered: paradigm, module boundaries, seams, lifecycle/scoping, state ownership, persistence split, security/privacy, network policy, capability model, matrix, receiver contract, testing. Under-covered: operations/environment envelope (F5) — distribution, telemetry/egress intent, permission/manifest ownership incl. multicast lock and FGS types; and the matrix-harness flow (F6). Two deliberate non-decisions are fine as-is: WebRTC artifact and per-capability timeouts (Open Questions), receiver schema (Open Question).

---

## Bottom line

The spine does what a spine should: it fixes the forks that would otherwise fork the code, and it refuses to abstract ahead of pressure. The blockers are local and fixable in place: **AD-8 must be rewritten so the wake path is a named, checkable exception rather than a contradiction (F1); the capability model needs its `surface` axis (F2) and a stated persistence/invalidation rule for labels (F3); AD-1's public-surface allowlist must match reality (F4); AD-5 must say where harness rows live (F6); and the operational envelope should be decided or deferred explicitly (F5).** None of these require reopening the paradigm or the accepted UX decisions.
