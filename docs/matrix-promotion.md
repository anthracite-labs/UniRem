# Matrix promotion runbook (Story 1.8)

How an observed TV behavior becomes an earned capability label in a release.
No label ships without a recorded matrix pass for the exact model, byte-exact
firmware, and network type (NFR2/NFR14). H/J 2014–15 stays best-effort
regardless of a pass.

## 1. Record in the debug harness

1. Pair the owned TV with a debug build and open the paired summary.
2. Open the **Matrix harness** (debug builds only, below the Done action).
3. Confirm the read-only identity line: exact model, firmware, network, and
   asset `matrixVersion`.
4. For each key you exercised on the TV, select the key, pick the observed
   result, add a short note if it helps review, and tap **Save row**. Save
   replaces the prior row for the same exact key.
5. The paired summary plate reflects the save immediately; stale rows (recorded
   against an older asset) show as stale with Clear and never resolve.

Rules while recording:

- Record only what you observed on the TV. A socket write is never success;
  keys need the on-TV response, text needs glyphs in the field, pointer
  needs visible cursor motion, wake needs 8001/8002 answers.
- Never invent a pilot row. Never type a token, pin, SPKI, media URL, or
  full MAC anywhere near the harness.
- The harness opens no socket and sends nothing.

## 2. Copy rows into the asset

1. For each overlay row you want to promote, tap **Copy as JSON**. The
   snippet carries matrix fields only (never the note).
2. Paste the rows into `app/src/main/assets/verification-matrix.json`.
3. Bump the top-level `matrixVersion` by one and set every promoted row's
   `matrixVersion` to the same value. Mixed versions fail the build.
4. Update the human matrix
   (`_bmad-output/unirem/spec-samsung-remote/verification-matrix.md`) with
   the same evidence.

## 3. Review and merge

1. Run `./gradlew :app:verifyMatrix` — a structural backstop that mirrors
   the `:core` validator (brace discipline, trailing content, per-row
   versions, duplicate exact keys, the Keys/Device Partial fallback,
   unknown capability/scope pairs, wildcard/blank discipline, URL-like
   identities, and secret material). The oracle stays the real
   `MatrixRules` parser/validator, which runs in release load and in
   `MatrixAssetTest` against this exact file; both must pass.
2. Run `./gradlew build lint :core:test` and
   `./gradlew :app:testDebugUnitTest`; expect BUILD SUCCESSFUL, 0 lint
   errors, and green JVM tests.
3. Run a release smoke on an owned TV: confirm the harness route is absent,
   the card reads the asset only, and `adb logcat` shows no token, pin,
   SPKI, MAC, or URL.
4. Commit the asset and the human matrix together. Promoted rows are cleared
   from the debug overlay (Clear in the harness) on every developer device
   that recorded them; rows are never auto-promoted.

## Asset shape

```json
{
  "matrixVersion": 1,
  "rows": [
    {
      "model": "*",
      "firmware": "*unknown*",
      "networkType": "Unknown",
      "capability": "Keys",
      "scope": "Device",
      "result": "Partial",
      "matrixVersion": 1
    }
  ]
}
```

Model and firmware are byte-exact (no trim, fold, or normalization). The
`model: "*"` / `firmware: "*unknown*"` row is the fallback; it matches any
identity for its key when no exact row matches. Release builds read the
asset only; debug builds layer same-version exact-key overlay rows on top.
