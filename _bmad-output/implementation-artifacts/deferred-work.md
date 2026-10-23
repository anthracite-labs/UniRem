# Deferred work

- source_spec: `_bmad-output/implementation-artifacts/spec-1-4-gate-local-network-access-and-own-the-permission-state.md`
  summary: Rapid double-tap on explainer Continue may fire the system permission prompt twice.
  evidence: E7 maybe-false (medium-if-true); unsettleable on JVM — needs rapid double-tap of Continue on API-37 hardware to see whether the second launch() throws or re-fires.
- source_spec: `_bmad-output/implementation-artifacts/spec-1-4-gate-local-network-access-and-own-the-permission-state.md`
  summary: System-prompt firing rule (Continue once, never re-fire, Denied to settings) has no executing test.
  evidence: V3 pre-verified gap; Activity wiring is JVM-untestable and the pure rule (continueActionFor) is already unit-tested — settler is the owned-device prompt matrix on API 37/36 already assigned to the spec's manual Verification.
