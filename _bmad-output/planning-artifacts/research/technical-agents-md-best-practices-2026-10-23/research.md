---
title: 'technical research: AGENTS.md best practices'
type: 'technical'
topic: 'AGENTS.md best practices'
decision: 'What belongs in UniRem''s AGENTS.md — and what to leave out — grounded in the current standard and how agent harnesses load it.'
source: 'native run'
status: complete
preset: 'standard'
validation: 'normal'
claims:
  verified: 24
  unverified: 9
created: '2026-10-23'
updated: '2026-10-23'
---

# technical research: AGENTS.md best practices

**Decision this research serves:** What belongs in UniRem's `AGENTS.md` — and what to leave out — grounded in the current standard and how agent harnesses load it.

## Executive summary

**Write a small, hand-written, root-level `AGENTS.md` now — decisions and non-inferable facts only, no architecture tour, commands marked unverified until the scaffold exists — and move every hard rule into a check.** The evidence supports a thin policy file plus real enforcement, not a comprehensive briefing.

Three findings drive that answer:

1. **The standard is a convention, not a spec, and tools disagree on how they load it.** There are no required fields, no version, and no testable compliance definition [1][4]. The FAQ says the closest file wins [1], Codex concatenates root→cwd [6], OpenCode v2 merges all ancestors without resolving conflicts [12], and Copilot uses only the nearest file [14]. A single root file is the only shape that behaves the same across all of them.
2. **Content beats length, and generated overviews are the weakest content.** The only controlled multi-agent benchmark found no general success gain from context files when documentation already exists, while LLM-generated files added 20–23% inference cost for no gain [23]. The measurable payoff came from non-inferable, version-matched facts: mentioning a tool raised its use from <0.01× to 1.6× per task, and repo-specific tools from <0.05× to 2.5× [23]. Vercel's post-cutoff docs index in `AGENTS.md` moved pass rates from 53% to 100%, versus 79% for a skill plus explicit instruction [29].
3. **Prose does not enforce behavior.** Across 481 public `CLAUDE.md` files, only 4.4% of retrieved security rules had a matching built-in control (95% CI 2.6–6.7) [34]. Credential leaks occurred despite 150+ lines of "NEVER COMMIT API KEYS" [35][36]. Deterministic rules belong in hooks, lint, or CI; the file carries the rationale and the fallback.

**Biggest caveat:** there is no Android/Kotlin/Gradle-specific study of instruction files [49]. The controlled evidence is Python-only [23] and TypeScript-only [25]; the largest positive result is a vendor eval with undisclosed sample size [29]. Trust the direction, not the effect sizes.

## 1. Standard and adoption

**What it is.** `AGENTS.md` is an unversioned Markdown convention, not an RFC. It was released by OpenAI in August 2025, donated to the Agentic AI Foundation (Linux Foundation) in December 2025, and is now used by 60,000+ projects according to the Foundation [2][3]. The canonical site publishes purpose and examples, but no releases, tags, or version number; "AGENTS.md v1.x" claims could not be confirmed from any primary source [1].

**No compliance definition.** An open issue requests a formal definition; the FAQ answers that it is "just standard Markdown" with no required fields, headings, size limit, or nesting limit [1][4]. Every real constraint comes from the consuming tool.

**Precedence is not portable.** The standard's FAQ says "the closest AGENTS.md to the edited file wins; explicit user chat prompts override everything" [1]. Codex documents the opposite mechanism — concatenate global plus one file per directory root→cwd, with closer files later in the prompt [6]. A commenter on the standard's own tracker calls this out and it remains unresolved [5]. Copilot says nearest wins [14]; OpenCode v2 merges all ancestors and does not resolve conflicts [12]; Claude Code reads `AGENTS.md` only as a fallback when no `CLAUDE.md` exists, or both when configured [8].

**Adoption lists overstate default support.** Gemini CLI and Aider are listed as adopters but require explicit configuration to read `AGENTS.md`; Gemini defaults to `GEMINI.md` [1][17]. Copilot support differs by surface — cloud agent, VS Code, and CLI do not share one precedence model [15]. "Supported" is not the same as "loaded by default."

**Not a superset.** Every major tool still prefers its own file when both exist: Claude prefers `CLAUDE.md` [8], Gemini prefers `GEMINI.md` [17], Cursor's `.mdc` rules remain richer [16], Copilot keeps `.github/copilot-instructions.md` as a separate mechanism [14]. The working pattern is one shared file plus thin bridges, not one file replacing all.

## 2. Authoring guidance

### What credible sources converge on

1. **Commands first.** Exact build, lint, format, and test commands — including how to run one test — are the highest-value content, and every vendor says some version of this [8][10][20][37][38].
2. **Only what the agent cannot infer.** Project-specific conventions that differ from language/tool defaults, architectural boundaries, environment quirks, and gotchas. Not standard conventions, not anything readable from the code [8][9][16][21][48].
3. **Short and scannable.** Headers plus bullets; prune regularly. Vendors disagree on the number (200 lines, 500 lines, 32 KiB) but agree that bloat degrades adherence [8][10][16][6].
4. **Layer by scope, do not duplicate.** Root file for repo-wide rules; nested or path-scoped files for subsystems; never the same rule in several layers [1][8][31].
5. **Concrete and testable.** Imperative rules, the safe alternative named alongside a prohibition, examples and file references over abstract prose [10][16][20].
6. **Progressive disclosure for depth.** Link to `CONTRIBUTING.md`, ADRs, or docs; use path-scoped rules or skills for content not needed every session [9][21][22].
7. **State the social and safety contract.** PR/commit rules, review checklist, and explicit "ask first" / "never" boundaries for secrets, generated files, destructive migrations, and public APIs [20][37][38].
8. **Treat the file as code.** Check it in, review changes, update it when the agent repeats a mistake, delete stale or contradictory rules [21][22][28].

### Where credible sources disagree

- **Project overview and architecture tour.** `agents.md` lists "Project overview" as a popular section [1], and GitHub's generator asks for a repo summary and layout [14]. Anthropic explicitly excludes file-by-file descriptions, and its `/doctor` trims architecture overviews and directory layouts [8]. The evidence favors Anthropic: generated overviews were the content most often present and least useful [23][48].
- **How much command detail.** GitHub wants every bootstrap/build/test/run/lint step with versions and failure modes [14]; Cursor says do not document every command because the agent already knows common tools [16]. The resolution is the non-obvious command and the caveat, not a task list.
- **Size budgets.** Anthropic: target under 200 lines [8]. Cursor: under 500 lines [16]. Codex: 32 KiB combined by default [6]. The controlled length analysis found no effect across 24–2,003 words, but it was observational and bounded [23][25]. Treat line counts as cost heuristics, not compliance cliffs.
- **Whether the file helps at all.** Vendor docs assume it does; the ETH benchmark reports neutral-to-negative averages [23]; a later two-agent ablation also found no measurable strategy effect (Claude p=1.00, Codex p=0.66) [33].
- **Generated vs hand-written.** GitHub ships a generator prompt [14]; Thoughtworks and the ETH data say LLM-generated files are often worse [21][23].

### Greenfield: what to do before code exists

No vendor page addresses a repository with no code. One practitioner source explicitly does: write a minimal seed at clone time, because fully specifying commands and architecture before code exists is "a fruitless exercise," but an empty file leaves the agent "groping around blindly" [47]. The seed carries placeholders (`[To be determined]`, `[TBD after scaffold]`) that are replaced and deleted as facts become true, plus a maintenance section [47].

The defensible synthesis, labeled as synthesis rather than vendor guidance:

- Write the file now, but only with **decisions and constraints**, not descriptions of code that does not exist.
- Mark commands with a concrete placeholder; never present an unverified invocation as runnable.
- Make "run the build and replace every placeholder" the first post-scaffold task.
- Add a provenance line (`Verified <date> @ <commit>`) so the next audit knows what it is diffing from. No retrieved source prescribes this; it is a reasonable convention, not a standard.

### Android/Kotlin specifics that earn a line

| Rule | Source class | Why it is non-inferable |
|---|---|---|
| Use `./gradlew`; commit `gradlew`, `gradlew.bat`, and `gradle/wrapper/*`; never run a system `gradle` | Official [41] | The wrapper pins the Gradle version; a system install silently builds with another. |
| Versions and plugin coordinates live in `gradle/libs.versions.toml`, referenced as `libs.*`; do not hardcode versions in module build files | Official [42][46] | The catalog is auto-imported; dash aliases become nested accessors, so renaming breaks scripts. |
| On AGP 9+, do not apply `kotlin-android`; use `kotlin { compilerOptions {} }`, not `android.kotlinOptions {}`; use KSP, not kapt | Official [43] | AGP 9 has built-in Kotlin; applying the plugin now fails or is redundant, and kapt is incompatible. |
| Build variants are `<flavor><BuildType>` (`demoDebug`); task names inherit the variant (`assembleDemoDebug`) | Official [44], practice [37] | Copying generic `assembleDebug` fails in a flavored project. |
| The Compose Compiler plugin (`org.jetbrains.kotlin.plugin.compose`) tracks the Kotlin version | Official [45] | A separate or mismatched version breaks the build. |
| Never commit `local.properties`, `keystore.properties`, `*.jks`, or `*.keystore`; load signing values from untracked local files or CI secrets | Official [46][44] | `local.properties` holds the machine-specific SDK path; signing secrets are not repository content. |
| Run `./gradlew tasks --all` and `./gradlew projects` before proposing a command; use only task names that appear | Synthesis from [43][44][37] | Prevents invented task names; no study measures this, but the failure is mechanical. |

**Not sourced, do not present as official:** JDK pinning, Gradle daemon flags, configuration-cache quirks, Hilt/Room-specific KSP configuration, and Firebase's position on `google-services.json` were not covered by any retrieved source.

### Public examples worth reading

- **`android/nowinandroid`** — the most relevant Android example: variant-aware Gradle commands, single-test invocation, Roborazzi screenshot tasks, and test-library choices. It is strong on exact commands and weak on toolchain versions, secrets, and codegen; its architecture recital and folder map are the content Anthropic's `/doctor` would trim [37].
- **`meshtastic/Meshtastic-Android`** — a routing layer: role, memory policy, skill index, mandatory bootstrap, one exact baseline verification command, hard architectural boundaries, secret and license rules. Too long for a template, but the routing pattern is instructive [38].
- **`apache/airflow`** — the monorepo "operating manual": environment setup, generated commands, architecture boundaries, security model, PR rules, and `Ask first` / `Never` lists. Widely cited, but closer to the instruction dump Thoughtworks cautions against [39][21].
- **`temporalio/sdk-java`** — compact: repository layout, build/test commands including single-test invocation, PR expectations, review checklist, and a pointer to `CONTRIBUTING.md`. It records two genuinely non-inferable constraints: avoid changing public API signatures, and the Java 8 target [40].

## 3. Loading mechanics and interop

| Tool | What it reads | Discovery and merge | Cap / notes |
|---|---|---|---|
| **OpenAI Codex** | Global `~/.codex/AGENTS.md`; per-directory `AGENTS.override.md` → `AGENTS.md` | Global plus one file per directory, project root → launch directory, concatenated; closer files later; resolved once per run [6][7] | 32 KiB combined default (`project_doc_max_bytes`); deeper modules can silently vanish. Launch inside a module with `--cd` to get its file. |
| **Claude Code** | `CLAUDE.md`, `.claude/CLAUDE.md`, `CLAUDE.local.md`; `AGENTS.md` only as fallback | Ancestors concatenated at launch; subdirectory files load lazily on first read/edit there; `AGENTS.md` suppressed by any `CLAUDE.md` in cwd or above [8] | ~200-line target, 4 MiB hard skip; `@path` imports max 4 hops; `CLAUDE.md` containing `@AGENTS.md` remains a documented bridge and does not double-load [8]. |
| **OpenCode v2** | `AGENTS.md` only (no `CLAUDE.md` fallback) | Global plus every ancestor, nearest→farthest, combined; conflicts not resolved; subdirectory files loaded lazily and deduplicated [12][13] | No documented size cap. Edits to an already-loaded nested file are not auto-detected; start a new session. |
| **OpenCode v1** | `AGENTS.md`, with `CLAUDE.md` fallback | Docs say all instruction files are combined, but "first matching file wins in each category" (local vs global vs Claude) [11] | Per-ancestor merge behavior unconfirmed; test if still on v1. |
| **GitHub Copilot** | `AGENTS.md`, `.github/copilot-instructions.md`, path-scoped `.instructions.md` | Nearest `AGENTS.md` wins; VS Code uses root by default and nested files are experimental behind `chat.useNestedAgentsMdFiles`; CLI is additive with no general precedence order [14][15] | Nested module files shadow the root rather than add to it. |
| **Cursor** | `AGENTS.md`, `.cursor/rules/*.mdc` | Root and nested `AGENTS.md` combined, more specific wins [16] | Recommends under 500 lines; `.mdc` rules are the richer mechanism. |
| **Gemini CLI** | `GEMINI.md` by default; `AGENTS.md` via `context.fileName` | Global + ancestors + up to 200 subdirectories + JIT scan, concatenated into every prompt [17][18] | Listing both `AGENTS.md` and `GEMINI.md` risks duplicate content; dedup is undocumented. |
| **Windsurf / Devin Desktop** | `AGENTS.md`, rule files | Root `AGENTS.md` always-on; subdirectory files auto-globbed [19] | 12,000 chars per workspace rule file, 6,000 global. |

**One root file, not nested module files.** For a four-module Gradle repo under ~500 lines of instructions, the standard itself reserves nested files for large monorepos [1]. Nested files also behave differently per tool: OpenCode loads them lazily, Claude only when it reads a file there, Copilot treats the nearest as a replacement for the root, and Codex can truncate them at its byte cap [12][8][14][6]. Use path-qualified lines at the root ("in `:tv/`: ...") and split only when a module's exclusive rules are substantial enough to justify on-demand loading.

**One documented deployment gotcha.** OpenCode's upward walk stops at the home directory when the workspace is under home, so it can load an unrelated `AGENTS.md` from an ancestor directory outside the repository [12][13]. Check for ancestor files before trusting the instruction set; `OPENCODE_DISABLE_PROJECT_CONFIG=1` skips project discovery but keeps the global file.

**Bridges, only for tools actually used.** Claude Code: a `CLAUDE.md` containing `@AGENTS.md` is documented and does not double-load [8]. Gemini CLI: set `context.fileName` to `["AGENTS.md"]`, or keep `GEMINI.md` as a one-line `@AGENTS.md` import [17][18]. Copilot: no bridge needed because it reads root `AGENTS.md` natively, and it does not document `@`-import expansion [14]. Never copy the content into per-tool files; the audit that found a linter ruleset duplicated across five files is the predictable outcome [31].

## 4. Evidence and failure modes

### What the controlled evidence says

**The ETH Zurich benchmark** (2602.11988v3) is the only controlled, multi-agent, multi-model ablation found. It covers 300 SWE-bench Lite tasks plus 138 CTXbench instances from 12 Python repositories, with four agent/model pairs and one sample per task [23]. Results:

- LLM-generated context files: **−0.5%** (SWE-bench) and **−2%** (CTXbench) success, both non-significant, at **+20% / +23%** inference cost (p<0.001) [23].
- Developer-written files: **+2.4 percentage points** versus no file, non-significant (p=0.21), at up to +19% cost. This was measured only on CTXbench, and the significant contrast is developer-written versus LLM-generated (p=0.038), not versus no file [23].
- Length across **24–2,003 words** showed no clear relationship to success or cost; this is observational binning of 12 files, not a controlled length manipulation [23].
- The null is **conditional on existing documentation**. When all documentation files are removed, LLM-generated context files improve performance by **2.7%** and outperform developer-written files across settings. The authors describe context files as "redundant documentation" when docs are already present [23].

The viral claim that "AGENTS.md reduces success" is an overread of v1. The v1 abstract said files "tend to reduce task success rates"; v3 says they "do not generally improve" them [24][23]. Cite v3.

**The McMillan factorial study** (2605.10039) manipulated structural variables across 1,650 sessions and 16,050 function-level observations on two TypeScript codebases and three models [25]. Size and conflict produced affirmative nulls (BF10 0.05–0.10); position and architecture were failures to reject, with no effect larger than ~6 percentage points ruled out. The measured decay was within-session, not across files: each additional generated function was associated with ~5.6% lower odds of compliance (OR 0.944), non-monotonic and identified post hoc [25]. This supports "keep the session's work bounded," not a line-count rule.

**The replication.** A two-agent ablation (2607.27250) across 288 runs, 17 tasks, and 3 Python repositories found no measurable context-file strategy effect (Claude p=1.00, Codex p=0.66), consistent with the ETH null [33].

**The positive counterexample.** Vercel's vendor eval embedded an 8 KB compressed docs index in `AGENTS.md` for Next.js 16 APIs absent from training data. Pass rates: no docs 53%, skill 53%, skill plus explicit instruction 79%, `AGENTS.md` index 100% [29]. The post discloses no sample size, model, or run count, and the winning condition bundles the index with on-disk `.next-docs/` files and a retrieval instruction. It is evidence that always-present, version-matched, non-discoverable API facts pay off — not that a general repository briefing does.

**Density research.** IFScale (20 models, 10–500 simultaneous constraints) and ManyIFEval (10 models, up to 10 instructions) both show compliance falling as instruction count rises, with a bias toward earlier instructions [26][27]. These are synthetic tasks, not repository files, but they explain why "put the important rule early" is repeated vendor advice.

**What real files look like.** A corpus of 2,303 files from 1,925 repositories found median lengths of 335–535 words; 67% of Claude files were edited in multiple commits; median additions were 57 words against fewer than 15 deletions per commit. Testing (75.9%), implementation (70.8%), and architecture (68.1%) dominate; security appears in only 14.8% [28]. Files accrete and rarely shrink. OpenAI's million-line agent-built repository keeps its `AGENTS.md` to about 100 lines and uses it as a map with pointers to deeper sources of truth; no ablation isolates its effect [30].

### Failure modes with evidence

- **Contradictions, not byte count.** A two-repo audit found four live contradictory instruction pairs, a linter ruleset duplicated across five per-tool files, six uninstalled apps listed as installed, and a backup described as 32 files when the mirror held 92. All 14 deterministic CI gates stayed green; the audit did not measure output quality [31].
- **Tool-specific loading and silent truncation.** Codex stops adding files at 32 KiB, so deeper modules can lose rules without warning; Copilot ignored nested `AGENTS.md` entirely before v1.105 and still treats it as experimental in VS Code [6][14]. Re-test old complaints against current versions.
- **Staleness is structural.** Files add far more than they delete, and stale facts are stated with the same confidence as correct ones [28][31].
- **Prose security rules do not gate tool calls.** A user reported 150+ lines of "NEVER COMMIT API KEYS" in `CLAUDE.md` followed by committed Gmail, Maps, and Firecrawl keys across three incidents in one session [35]; another reported a global "never display credentials" rule failing to stop `cat ~/.claude/settings.json`, exposing a live key [36]. Across 481 public files, only 4.4% of security rules had a matching built-in control [34].
- **Literal obedience to a stale rule.** A rebuild document instructed a future agent to recreate a package-manager setup that the current drift checker flags as a defect — correct obedience to an obsolete instruction [31][10].
- **Duplicated per-tool files drift.** One linter's ruleset existed near-verbatim in `AGENTS.md`, `GEMINI.md`, a Cursor rules file, a Windsurf rules file, and `.github/copilot-instructions.md`, while Biome and a formatting hook already enforced it [31].

### The contrary case

Sonar argues that `AGENTS.md` is static, unstructured, always-on prose and therefore cannot be governance — only a briefing. "We use hexagonal architecture" is not the dependency graph; always-on loading is simultaneously too generic for the task and too large to use reliably; models underweight middle-of-context rules [32]. The vendor sells a competing context product, so weigh accordingly. The concession matters: it is good for stable operational facts, and "if you do not have one, write one today" [32]. The category error is treating it as enforcement.

### Strength of evidence

- **High confidence** that LLM-generated context files do not improve success and cost ~20% more in the tested setting [23], and that prose security rules are not an enforcement layer [34].
- **Medium confidence** that developer-written files help slightly; the +2.4-point result is non-significant and CTXbench-only [23].
- **Medium confidence** in the Vercel effect; the mechanism is plausible but the eval is vendor-run with undisclosed n [29].
- **Low confidence** in any specific line-count threshold; the controlled length analysis found none in its range [23][25].
- **No evidence** for Android/Kotlin/Gradle specifically. The closest adjacent study is about AI-authored PR acceptance in Android/iOS repos (2,901 PRs; Android 71% vs iOS 63%), not instruction files [49].

## Cross-dimension insights

1. **Portability, content, and enforcement point to the same design.** The loading differences make one root file the only portable shape [1][6][12][14]; the evidence makes non-inferable facts the only reliably useful content [23][29]; the enforcement gap makes checks the only reliable rule [34]. A thin root policy file plus real CI is the intersection, not a compromise.
2. **The overview is the most common and least valuable section.** Generated files almost always contain one, and it is exactly what the agent can rediscover [23][48]. Point to the architecture spine instead of summarizing it.
3. **The size debate misses the mechanism.** The cost is per-session tokens plus contradictions [8][31], not a measured line-count cliff [23][25]. Budget by "would removing this change behavior?", not by a target number.
4. **Greenfield inverts the risk.** The file is most valuable when it carries decisions, and most likely to rot when it describes code that does not exist [47]. Placeholders, provenance, and a first-scaffold verification pass are the control.
5. **Vendor guidance is not evidence.** Where vendor advice conflicts with the study — overviews, exhaustive command lists, generated files — prefer the study for content selection and the vendor docs for loading mechanics.

## Recommendations for UniRem's `AGENTS.md`

1. **Write a root `AGENTS.md` now, hand-written, ~40–70 lines, seeded from the spec and architecture spine.** No directory tree, no stack inventory beyond non-obvious constraints, no capability-matrix dump. Confidence: high (portability + greenfield convergence) [1][12][47].
2. **Use the six-section shape, but populate it by the non-inferable test.** Orientation (2–3 sentences and pointers), Policy (module graph fixed, no new modules or dependencies without an architecture change, no telemetry SDK, no WSS→WS fallback, secrets never in repo, capability claims only from the recorded matrix), Where things are (spec, spine, UX, epics), Running and verifying (placeholders plus Gradle facts), Conventions (command truth, IDs, time, logging, AGP 9 built-in Kotlin), Known pitfalls (only observed). Confidence: high on shape, medium on line count [8][21][37][38].
3. **Mark every command `[TBD after scaffold]` until the wrapper exists, then replace it in the first scaffold story and add `Verified <date> @ <commit>`.** Never present a guessed `./gradlew` task as runnable. Confidence: medium (practitioner guidance, not vendor) [47].
4. **Move the hard rules into checks:** a Gradle module-graph assertion; a forbidden-import check for `okhttp3`, `org.jupnp`, and `KEY_*` in `:app`; a `RedactedLog` lint; secret scanning; a dependency check that rejects analytics/crash-reporting SDKs. Keep the prose as rationale and fallback. Confidence: high [34][10][31].
5. **Keep one root file.** No nested module `AGENTS.md` for four modules. Use path-qualified lines. If a module later accumulates >20 lines of exclusive rules, revisit and accept that Copilot will then lose root rules in that directory. Confidence: high [1][12][14][16].
6. **Add bridges only for tools actually used.** Claude Code: `CLAUDE.md` containing `@AGENTS.md` (or rely on native fallback since v2.1.277). Gemini CLI: `context.fileName: ["AGENTS.md"]` or a `GEMINI.md` import. Copilot: nothing. Never duplicate content. Confidence: high per current docs [8][17][18][14].
7. **Check for ancestor instruction files above the repo.** OpenCode can load an unrelated `AGENTS.md` from a parent directory outside the repository; inspect and neutralize unrelated rules before trusting the session's instruction set. Confidence: medium (documented behavior, not verified in this repository) [12][13].
8. **Do not include:** architecture overview, directory tree, dependency list, capability labels, ktlint/detekt style rules, API docs, or aspirational feature descriptions. Confidence: high [8][9][16][23][48].
9. **Refresh after the scaffold and quarterly thereafter.** Re-check commands first, then remove placeholders and stale lines. Every line faces "would removing it change behavior?" Confidence: medium [22][28][31].

## Open questions

- **OpenCode version in use.** v1 and v2 differ on `CLAUDE.md` fallback and ancestor merge; verify with the installed CLI before finalizing the bridge strategy [11][12].
- **Toolchain compatibility.** The architecture spine pins Gradle 9.5.0, AGP 9.3.1, Kotlin 2.4.21, and Compose BOM 2026.09.00. This research verified the *conventions* (wrapper, catalog, built-in Kotlin, Compose compiler lockstep) but not that exact version matrix [41][42][43][45].
- **No Android/Kotlin study.** The effect of an `AGENTS.md` on Gradle/Kotlin agent work is unmeasured; the Android-specific recommendations are official build rules plus inference [49].
- **No longitudinal team metrics.** No published study ties instruction files to PR rework, CI failures, or review comments; "green gates" are a regression signal, not a quality result [23][31].
- **Whether Gemini or Copilot are in use.** Bridges and nested-file decisions depend on the actual harness set; adding unused bridges creates drift [8][14][17].
- **Shell invocation form.** `./gradlew` vs `gradlew.bat` depends on the shell in use; resolve when the wrapper lands, not before [41].

## Source appendix

| # | Claim / finding it supports | Publisher | Published | Accessed | Confidence |
|---|---|---|---|---|---|
| [1] | AGENTS.md convention, FAQ, precedence, adoption list, no required fields | [AGENTS.md / AAIF](https://agents.md/) | living doc | 2026-10-23 | high |
| [2] | AAIF formation and 60,000+ project adoption | [Linux Foundation](https://www.linuxfoundation.org/press/linux-foundation-announces-the-formation-of-the-agentic-ai-foundation) | 2025-12-09 | 2026-10-23 | high |
| [3] | OpenAI donation and adoption milestone | [OpenAI](https://openai.com/index/agentic-ai-foundation/) | 2025-12-09 | 2026-10-23 | medium |
| [4] | No formal compliance definition (open issue) | [agentsmd/agents.md #24](https://github.com/agentsmd/agents.md/issues/24) | 2025-08-26 | 2026-10-23 | medium |
| [5] | Nearest-wins vs concatenate contradiction (open issue) | [agentsmd/agents.md #53](https://github.com/agentsmd/agents.md/issues/53) | 2026-04-28 | 2026-10-23 | medium |
| [6] | Codex discovery, concatenation, 32 KiB cap, once per run | [OpenAI Codex docs](https://learn.chatgpt.com/docs/agent-configuration/agents-md) | current | 2026-10-23 | high |
| [7] | Codex authoring guidance, Code Review Rules, fallback filenames | [OpenAI Codex guide](https://developers.openai.com/codex/guides/agents-md) | current | 2026-10-23 | high |
| [8] | Claude Code memory hierarchy, AGENTS.md fallback, 200-line target, imports, hooks | [Anthropic Claude Code docs](https://code.claude.com/docs/en/memory) | current | 2026-10-23 | high |
| [9] | CLAUDE.md best practices, prune test, emphasis advice | [Anthropic Engineering](https://www.anthropic.com/engineering/claude-code-best-practices) | current | 2026-10-23 | high |
| [10] | CLAUDE.md as context not enforcement; hooks for deterministic rules | [Anthropic](https://claude.com/blog/steering-claude-code-skills-hooks-rules-subagents-and-more) | 2026-06-18 | 2026-10-23 | medium |
| [11] | OpenCode v1 rules, CLAUDE.md fallback, `instructions` config | [OpenCode docs](https://opencode.ai/docs/rules/) | 2026-10-22 | 2026-10-23 | high |
| [12] | OpenCode v2 merges ancestors, lazy nested files, AGENTS.md only, home-dir walk | [OpenCode v2 docs](https://opencode.ai/v2/docs/instructions/) | current | 2026-10-23 | high |
| [13] | OpenCode discovery implementation (`[global, ...project, ...configured]`) | [OpenCode source](https://raw.githubusercontent.com/anomalyco/opencode/dev/packages/core/src/instruction-discovery.ts) | current | 2026-10-23 | high |
| [14] | Copilot nearest-wins, surface differences, no `@` expansion | [GitHub Docs](https://docs.github.com/en/copilot/how-tos/configure-custom-instructions/add-repository-instructions) | current | 2026-10-23 | high |
| [15] | Copilot per-surface support table | [GitHub Docs](https://docs.github.com/en/copilot/reference/custom-instructions-support) | current | 2026-10-23 | high |
| [16] | Cursor rules, nested AGENTS.md, 500-line guidance | [Cursor Docs](https://cursor.com/docs/context/rules) | current | 2026-10-23 | high |
| [17] | Gemini CLI `context.fileName`, discovery, concatenation | [Gemini CLI docs](https://geminicli.com/docs/reference/configuration/) | 2026-10-15 | 2026-10-23 | high |
| [18] | GEMINI.md hierarchy, JIT loading, imports | [Gemini CLI docs](https://geminicli.com/docs/cli/gemini-md/) | 2026-06-18 | 2026-10-23 | high |
| [19] | Windsurf rule caps and AGENTS.md behavior | [Windsurf Docs](https://docs.windsurf.com/windsurf/cascade/memories) | current | 2026-10-23 | medium |
| [20] | GitHub analysis of 2,500+ repos; six areas; secrets constraint | [GitHub Blog](https://github.blog/ai-and-ml/github-copilot/how-to-write-a-great-agents-md-lessons-from-over-2500-repositories/) | 2025-11-19 | 2026-10-23 | medium |
| [21] | Agent instruction bloat; progressive disclosure; hand-written vs generated | [Thoughtworks](https://www.thoughtworks.com/radar/techniques/summary/agent-instruction-bloat) | 2026-04-15 | 2026-10-23 | high |
| [22] | Build rules incrementally; no unit tests for context engineering | [martinfowler.com](https://martinfowler.com/articles/exploring-gen-ai/context-engineering-coding-agents.html) | 2026-02-05 | 2026-10-23 | high |
| [23] | ETH benchmark: deltas, cost, length, conditional null | [arXiv 2602.11988v3](https://arxiv.org/html/2602.11988v3) | 2026-09-29 | 2026-10-23 | high |
| [24] | ETH v1 abstract wording ("tend to reduce") | [arXiv 2602.11988v1](https://arxiv.org/abs/2602.11988v1) | 2026-02-12 | 2026-10-23 | high |
| [25] | Structural nulls, Bayes factors, within-session decay | [arXiv 2605.10039](https://arxiv.org/abs/2605.10039) | 2026-05-11 | 2026-10-23 | high |
| [26] | IFScale: compliance falls as instruction count rises | [arXiv 2507.11538](https://arxiv.org/abs/2507.11538) | 2025-07-15 | 2026-10-23 | medium |
| [27] | ManyIFEval: up to 10 instructions, accuracy falls with count | [arXiv 2509.21051](https://arxiv.org/abs/2509.21051) | 2025-09-25 | 2026-10-23 | medium |
| [28] | Corpus of 2,303 files: lengths, accretion, category prevalence | [arXiv 2511.12884](https://arxiv.org/abs/2511.12884) | 2026-08-09 | 2026-10-23 | high |
| [29] | Vercel 8KB index eval, 53/53/79/100, undisclosed n | [Vercel](https://vercel.com/blog/agents-md-outperforms-skills-in-our-agent-evals) | 2026-01-27 | 2026-10-23 | medium |
| [30] | OpenAI agent-built repo keeps AGENTS.md ~100 lines as a map | [OpenAI](https://openai.com/index/harness-engineering/) | 2026-02-11 | 2026-10-23 | medium |
| [31] | Contradictions, duplication, staleness audit | [Digital Applied](https://www.digitalapplied.com/blog/ai-agent-instruction-file-audit-what-we-cut) | 2026-08-29 | 2026-10-23 | low-medium |
| [32] | Contrary case: static prose is a briefing, not governance | [Sonar](https://www.sonarsource.com/blog/agents-md-is-a-workaround-not-a-solution) | 2026-09-14 | 2026-10-23 | medium |
| [33] | Two-agent ablation: no measurable strategy effect | [arXiv 2607.27250](https://arxiv.org/abs/2607.27250) | 2026 | 2026-10-23 | medium |
| [34] | 4.4% of security rules had a matching control | [arXiv 2608.23550](https://arxiv.org/abs/2608.23550) | 2026 | 2026-10-23 | high |
| [35] | "Never commit keys" rule failed; keys committed | [claude-code #2142](https://github.com/anthropics/claude-code/issues/2142) | 2025-06-16 | 2026-10-23 | medium |
| [36] | "Never display credentials" rule failed; live key exposed | [claude-code #58043](https://github.com/anthropics/claude-code/issues/58043) | 2026-05-11 | 2026-10-23 | medium |
| [37] | Now in Android: commands, variants, test libraries | [android/nowinandroid](https://github.com/android/nowinandroid/blob/main/AGENTS.md) | current | 2026-10-23 | high |
| [38] | Meshtastic: routing layer, baseline command, boundaries | [meshtastic/Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android/blob/main/AGENTS.md) | current | 2026-10-23 | high |
| [39] | Airflow: monorepo operating manual, Ask/Never lists | [apache/airflow](https://github.com/apache/airflow/blob/main/AGENTS.md) | current | 2026-10-23 | high |
| [40] | Temporal Java SDK: compact commands and constraints | [temporalio/sdk-java](https://github.com/temporalio/sdk-java/blob/master/AGENTS.md) | current | 2026-10-23 | high |
| [41] | Gradle wrapper is the recommended execution path; commit wrapper files | [Gradle Docs](https://docs.gradle.org/current/userguide/gradle_wrapper.html) | current | 2026-10-23 | high |
| [42] | Version catalogs: `libs.versions.toml`, accessors, aliases | [Gradle Docs](https://docs.gradle.org/current/userguide/version_catalogs.html) | current | 2026-10-23 | high |
| [43] | AGP 9 built-in Kotlin, no `kotlin-android`, `compilerOptions`, KSP | [Android Developers](https://developer.android.com/build/migrate-to-built-in-kotlin) | current | 2026-10-23 | high |
| [44] | Variants, task naming, signing from untracked files | [Android Developers](https://developer.android.com/build/build-variants) | current | 2026-10-23 | high |
| [45] | Compose Compiler plugin tracks Kotlin version | [Android Developers](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler) | 2026-10-16 | 2026-10-23 | high |
| [46] | `local.properties`, `.gradle/`, wrapper, catalog roles | [Android Developers](https://developer.android.com/build/android-build-structure) | 2026-02-26 | 2026-10-23 | high |
| [47] | Greenfield seed at clone time, placeholders, maintenance section | [nyosegawa.com](https://nyosegawa.com/en/posts/agents-md-generator/) | 2026-02-15 | 2026-10-23 | medium |
| [48] | Non-inferable content; auto-generated overviews harmful | [Addy Osmani](https://addyosmani.com/blog/agents-md/) | 2026-02-23 | 2026-10-23 | medium |
| [49] | No Android/Kotlin instruction-file study; adjacent MSR 2026 PR study | [ACM / MSR 2026](https://dl.acm.org/doi/abs/10.1145/3793302.3793596) | 2026 | 2026-10-23 | medium |

## Staleness map

Computed with `recon_kit.py staleness` from the claims ledger, using the technical pack's freshness bars (compatibility 1 month; standard 6 months; evidence/failure/practice 12 months; guidance 6 months). **Earliest re-check: 2026-11-23.**

| Claim class | Re-check | Why |
|---|---|---|
| Tool loading behavior (Codex, Claude Code, OpenCode, Copilot, Cursor, Gemini) | 2026-11-23 | Version-dependent mechanisms; OpenCode v1/v2 already differs. |
| Android/Gradle conventions (AGP 9, wrapper, catalogs) | 2026-11-23 | Toolchain moves fast; the spine pins versions that may change. |
| AGENTS.md standard status and adoption | 2027-04-23 | Living document; no version to diff against. |
| Vendor authoring guidance and size targets | 2027-04-23 | Guidance tracks product releases. |
| Greenfield seed practice | 2027-02-15 | Single practitioner source; may be superseded. |
| ETH benchmark and conditional null | 2027-09-29 | Stable study; watch for replication. |
| McMillan structural nulls | 2027-05-11 | Single-author, not peer-reviewed. |
| Vercel eval | 2027-01-27 | Vendor result; n undisclosed. |
| Corpus statistics | 2027-08-09 | Descriptive; ages slowly. |
| Security-rule enforcement gap | 2027-09-14 | Descriptive; stable until tooling changes. |
| Two-agent ablation | 2027-07-15 | Abstract-only read; watch for peer review. |
