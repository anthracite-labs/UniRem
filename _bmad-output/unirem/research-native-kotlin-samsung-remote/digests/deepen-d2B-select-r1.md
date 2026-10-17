# Deepen D2B-R1 — DLNA library select + minSdk settle (digest)

Accessed (all web claims): 2026-10-16. Versions/compat re-verified fresh (≤1mo rule).

## Cling pre-cut (recorded, not researched)
Cling EOL 2019 per frame; jUPnP README confirms it "has been forked from the no-longer maintained Cling project" (github.com/jupnp/jupnp, accessed 2026-10-16). Cut stands.

## Q5 evidence per criterion (contested cells dual-sourced)

1. **Versions/maintained:** jUPnP 3.0.5 released Aug 2026 (GitHub releases list; Central `org/jupnp/org.jupnp.android/3.0.5` dir dated 2026-08-12; mvnrepository "Aug 13, 2026"; prior 3.0.4 Dec 2025) — gate PASS. UPnPCast v1.3.0 released 2026-09-02 (github.com/yinnho/UPnPCast/releases, "released this 02 Sep") — recency PASS, but see provenance.
2. **Maven provenance (DECIDING CELL):** Central `com/yinnho/upnpcast/upnpcast/` lists ONLY `1.1.2/` (2025-06-12) — 1.2.0/1.3.0 absent (repo1.maven.org listing, accessed 2026-10-16). Corroborated by UPnPCast README Installation: "Releases are distributed via JitPack" + `com.github.yinnho:UPnPCast:v1.3.0` (repo page, accessed 2026-10-16). The v1.3.0 release note's "(Maven Central)" dependency line is vendor claim contradicted by the registry — vendor-vs-registry divergence is itself a finding. UPnPCast at claimed version FAILS the Central-provenance hard gate; only stale 1.1.2 passes it.
3. **License:** jUPnP = CDDL-1.0 (repo badge + LICENSE.txt + licenses/cddl/). Per Mend/FOSSA CDDL analyses: shippable in commercial closed apps, weak (file-level) copyleft — must publish source of any *modified CDDL files*, retain notices; unmodified binary use = attribution-level load. PASS with obligations. UPnPCast = MIT (repo sidebar + LICENSE) — PASS, zero friction. (Authoritative texts + analyst writeups, not hearsay.)
4. **Transports/size:** jUPnP Android requires Jetty (jetty-server/servlet/client per jupnp.org Android docs) + slf4j — heavy; open issue #236 (NoClassDefFoundError SelectorManager in nova-video-player via Sentry) evidences Android weight/risk. UPnPCast: "no OkHttp, no Gson; local serving via NanoHTTPD only" (README; v1.2.0 notes removed okhttp/gson) — footprint PASS, decisively lighter.
5. **Tests:** UPnPCast v1.3.0 = "92 unit + protocol-level integration tests driven by an in-process fake DLNA renderer" (release notes; README "92 unit + protocol-level integration tests"). jUPnP: CI exists, no coverage/fake-renderer figure found — unverified. Edge to UPnPCast on paper; vendor-claimed (single source class).
6. **Samsung quirks:** UPnPCast explicit: `subtitleUri` incl. Samsung `sec:SubtitleUri`, MIME/class overrides, verbatim DIDL-Lite override (README + v1.2.0 notes); compat list (Samsung/LG/Sony/Xiaomi/WMP) is vendor-claimed, no independent report found. jUPnP: generic UPnP-AV/DLNA support lib, no Samsung-specific handling found. Edge UPnPCast.
7. **Kotlin/Android + screen-off:** UPnPCast coroutine-first suspend API, minSdk API 24, MulticastLock + ephemeral-port fallback (README/v1.2.0) — Kotlin 5/5. jUPnP Java API + AndroidUpnpService with documented tuning — Kotlin 2/5, service infra better documented. Screen-off/background: NO direct evidence for either (no FGS/service component in UPnPCast — app must own it; DLNA renderers play independently post-SetAVTransportURI so screen-off mostly affects discovery socket + local file server). Unverified for both — do not score as proven.

## Scores (weight order: Kotlin > Samsung > footprint > screen-off; /5)
- UPnPCast: Kotlin 5, Samsung 4*, footprint 5, screen-off 1 (unverified). Gates: 3/4 PASS, Central FAIL@1.3.0. (*vendor-claimed compat)
- jUPnP 3.0.5: Kotlin 2, Samsung 2, footprint 2, screen-off 2 (service infra, still unproven). Gates: 4/4 PASS (CDDL with obligations).

## Verdict
**Pick: jUPnP 3.0.5** — sole candidate passing all four hard gates. **Runner-up: UPnPCast v1.3.0**, which dominates every weighted pref and wins instead the moment `com.yinnho.upnpcast:upnpcast:1.3.x` is verifiable on Central (re-check listing) or the team formally accepts JitPack + commit-hash pinning + vendored artifact. **Strongest argument against the pick:** Java API + Jetty weight + CDDL compliance overhead vs a Kotlin-native, NanoHTTPD-light library with explicit Samsung subtitle handling — expect more glue code and APK bloat with jUPnP.
**Hedge (cheapest reversibility):** app-owned `RendererGateway` seam (discover/cast/play/pause/seek/volume/subtitle DTOs; DIDL building in app module) — both libs adapt behind it. **Staleness:** selection older than two quarters must refresh before action.

## Cost & lock-in
jUPnP: higher operational load (Jetty config, CDDL tracking, Java-interop glue), lower supply risk (Central, multi-year history, 202★/51 forks). UPnPCast: near-zero learning curve, single-maintainer bus factor (35★/7 forks, 8 releases in ~3mo — velocity + fragility). Exit cost contained by the seam either way.

## Q2 minSdk verdict: 26
Nothing in the stack needs 29: UPnPCast floor is API 24; jUPnP ships an Android module with no 29 floor found; FGS *types* are a targetSdk-34+ declaration (Play requires target 36 since 2026-08-31 — orthogonal to minSdk); Photo Picker/MediaProjection-29 deltas are runtime/targetSdk concerns, scoped storage applies by OS version regardless. Distribution (aggregator Jun-2026 figures): API 26 ≈ 96.1% vs API 29 ≈ 91.1% — minSdk 29 sheds ~5pp reach for zero DLNA benefit. Binding reason: no API-29-only dependency exists in either candidate path. AppT-29 precedent: no weight assignable (no evidence available). If local-file serving must survive screen-off, add FGS `mediaPlayback` + Play Console declaration at targetSdk 36 — still not a minSdk raiser.

## LEADS
- Re-check repo1 `com/yinnho/upnpcast/upnpcast/` for 1.3.x before any UPnPCast adoption.
- Confirm jUPnP `org.jupnp.android` minSdkVersion in POM + Jetty version/size impact on APK.
- CDDL: legal sign-off on unmodified-binary use + notice handling.

## NOT FOUND
- Independent (non-vendor) UPnPCast field reports; Samsung subtitle behavior independent confirmation.
- jUPnP coverage numbers / fake-renderer tests; screen-off evidence for either library.
- jUPnP numeric minSdk floor; AppT-29 precedent detail (no project context in this brief).
