# Deferred work

- source_spec: `_bmad-output/implementation-artifacts/spec-1-4-gate-local-network-access-and-own-the-permission-state.md`
  summary: Rapid double-tap on explainer Continue may fire the system permission prompt twice.
  evidence: E7 maybe-false (medium-if-true); unsettleable on JVM — needs rapid double-tap of Continue on API-37 hardware to see whether the second launch() throws or re-fires.
- source_spec: `_bmad-output/implementation-artifacts/spec-1-4-gate-local-network-access-and-own-the-permission-state.md`
  summary: System-prompt firing rule (Continue once, never re-fire, Denied to settings) has no executing test.
  evidence: V3 pre-verified gap; Activity wiring is JVM-untestable and the pure rule (continueActionFor) is already unit-tested — settler is the owned-device prompt matrix on API 37/36 already assigned to the spec's manual Verification.
- source_spec: `C:\Users\KAVEN\Desktop\UniRem\_bmad-output\implementation-artifacts\spec-1-9-switch-the-one-paired-tv-without-guessing.md`
  summary: Shell flashes the no-TV setup on cold start before the async Room load and auto-select apply.
  evidence: E3 real but pre-existing (identical under the old records.isNotEmpty() read); fixing needs a loading state the story intent does not define.
- source_spec: `C:\Users\KAVEN\Desktop\UniRem\_bmad-output\implementation-artifacts\spec-1-9-switch-the-one-paired-tv-without-guessing.md`
  summary: Switcher tap inside the millisecond trust-entry-load window could suppress both sheets with no dismiss path.
  evidence: E4 maybe-false (high-if-true); unsettleable on JVM — needs an owned-device attempt to tap the switcher while the trust entry is loading.
- source_spec: `C:\Users\KAVEN\Desktop\UniRem\_bmad-output\implementation-artifacts\spec-1-9-switch-the-one-paired-tv-without-guessing.md`
  summary: Durable-pair branch force-unwraps candidate.tvId and would crash on a null id.
  evidence: E5 pre-existing context line not introduced by this diff; out of this story's scope.
- source_spec: `C:\Users\KAVEN\Desktop\UniRem\_bmad-output\implementation-artifacts\spec-1-9-switch-the-one-paired-tv-without-guessing.md`
  summary: Bottom sheets move focus in and back but do not trap keyboard focus while open.
  evidence: E11/E14 real but idiom-level — the approved TrustSheet pattern has no trap either; fix belongs to the shared sheet idiom, not this story.
- source_spec: `C:\Users\KAVEN\Desktop\UniRem\_bmad-output\implementation-artifacts\spec-1-10-open-the-remote-and-connect-to-the-selected-tv.md`
  summary: Session-only remote whose TV starts answering /api/v2/ after pairing lands on IdentityChanged with a dead Re-pair and never connects.
  evidence: E9/E5 medium-if-true; unsettleable on JVM — needs an owned-device session-only flow where /api/v2/ answers post-pair to see whether the fresh-identity gate fires, and likely needs an identity-upgrade path beyond this story.
