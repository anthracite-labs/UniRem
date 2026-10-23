# Digest — authoring guidance (round 1, assistant 2)

Lane: authoring guidance from vendors and practitioners. Budget used: ~12 sources / ~20 calls. Accessed 2026-10-23.

## Findings

1. **agents.md: AGENTS.md is a "README for agents", deliberately separate from README, with no required fields or schema.** — source: https://agents.md/ | publisher: AGENTS.md / Agentic AI Foundation | published: unknown | accessed: 2026-10-23 | confidence: high | class: guidance
   - Popular sections it lists are project overview, build/test commands, code style, testing instructions, security considerations, plus commit/PR rules and "anything you'd tell a new teammate". It says agents will execute listed test commands and fix failures before finishing, and calls the file "living documentation".

2. **agents.md: large monorepos should use nested AGENTS.md files, with the nearest file winning.** — source: https://agents.md/ | publisher: AGENTS.md / Agentic AI Foundation | published: unknown | accessed: 2026-10-23 | confidence: high | class: guidance
   - "The closest AGENTS.md to the edited file wins; explicit user chat prompts override everything." It cites the main OpenAI repo having 88 AGENTS.md files at time of writing.

3. **OpenAI Codex: guidance is discovered as a layered chain, concatenated root→cwd, with a hard 32 KiB default budget.** — source: https://developers.openai.com/codex/guides/agents-md | publisher: OpenAI | published: unknown | accessed: 2026-10-23 | confidence: high | class: guidance
   - Codex reads `~/.codex/AGENTS.md`, then one file per directory from project root to cwd, preferring `AGENTS.override.md`; closer files appear later and override earlier ones. It stops adding files at `project_doc_max_bytes` (32 KiB default; docs suggest raising it or splitting across nested directories), and supports fallback filenames.

4. **OpenAI Codex: review rules belong in a `## Code Review Rules` section nearest the governed code, and should name the bad behavior plus a safe path.** — source: https://developers.openai.com/codex/guides/agents-md | publisher: OpenAI | published: unknown | accessed: 2026-10-23 | confidence: high | class: guidance
   - "Keep rules concise, explain the behavior to flag and any safe path or exception, and reserve formatting and lint checks for CI." Repository-wide checks go at the root, service-specific ones in a nested file.

5. **Anthropic: CLAUDE.md is context, not enforced configuration; anything that must happen belongs in a hook or managed setting.** — source: https://docs.claude.com/en/docs/claude-code/memory | publisher: Anthropic | published: unknown | accessed: 2026-10-23 | confidence: high | class: guidance
   - Add to it when Claude repeats a mistake, review catches something it should have known, you repeat a correction, or a new teammate would need the same context. The companion best-practices page gives an explicit include/exclude table (commands, non-default style, test runners, repo etiquette, architecture decisions, env quirks, gotchas vs. anything derivable from code, standard conventions, API docs, fast-changing info, tutorials, file-by-file descriptions, "write clean code").

6. **Anthropic: target under 200 lines per CLAUDE.md; longer files reduce adherence, and imports do not reduce context cost.** — source: https://docs.claude.com/en/docs/claude-code/memory | publisher: Anthropic | published: unknown | accessed: 2026-10-23 | confidence: high | class: guidance
   - Files over 200 lines "consume more context and may reduce adherence"; Claude Code skips any file over 4 MiB. Imported `@path` files load together with the importer, so imports aid organization but not budget; path-scoped `.claude/rules/*.md` (with `paths:` frontmatter) load only for matching files.

7. **Anthropic: prune with "Would removing this cause Claude to make mistakes?", and emphasize at most one instruction.** — source: https://www.anthropic.com/engineering/claude-code-best-practices | publisher: Anthropic | published: unknown | accessed: 2026-10-23 | confidence: high | class: guidance
   - "Bloated CLAUDE.md files cause Claude to ignore your actual instructions"; if a rule keeps being skipped the file is probably too long. If one instruction keeps getting skipped, add "IMPORTANT" to that line alone — "If you emphasize many lines, none of them stands out."

8. **Anthropic: `/doctor` proposes cuts for content the codebase already reveals.** — source: https://docs.claude.com/en/docs/claude-code/memory | publisher: Anthropic | published: unknown | accessed: 2026-10-23 | confidence: high | class: guidance
   - It "cuts content Claude can derive from the codebase, such as directory layouts, dependency lists, and architecture overviews, and keeps pitfalls, rationale, and conventions that differ from tool defaults." `prompt-audit` also flags references to nonexistent files/commands and contradictions across instruction files.

9. **Anthropic: a repo can use AGENTS.md alone, but Claude reads it only when no CLAUDE.md exists in the working directory or above.** — source: https://docs.claude.com/en/docs/claude-code/memory | publisher: Anthropic | published: unknown | accessed: 2026-10-23 | confidence: high | class: guidance
   - Recommended interop is a one-line `CLAUDE.md` containing `@AGENTS.md`, not a symlink, because Edit/Write refuse to write through symlinks and symlinks on some platforms need admin rights or Developer Mode.

10. **GitHub Copilot: three instruction mechanisms — repo-wide, path-scoped, and agent instructions — with `AGENTS.md` supported anywhere and nearest-wins.** — source: https://docs.github.com/en/copilot/how-tos/configure-custom-instructions/add-repository-instructions | publisher: GitHub | published: unknown | accessed: 2026-10-23 | confidence: high | class: guidance
    - Repository-wide rules live in `.github/copilot-instructions.md`; path-specific files are `.github/instructions/NAME.instructions.md` with `applyTo:` glob frontmatter and optional `excludeAgent`; `AGENTS.md` may sit anywhere, and a single root `CLAUDE.md` or `GEMINI.md` is also accepted.

11. **GitHub: its own Copilot onboarding prompt caps the file at two pages and forbids task-specific instructions.** — source: https://docs.github.com/en/copilot/how-tos/configure-custom-instructions/add-repository-instructions | publisher: GitHub | published: unknown | accessed: 2026-10-23 | confidence: high | class: guidance
    - The prompt asks for validated bootstrap/build/test/run/lint sequences with tool versions, architecture/layout pointers, CI checks, and workarounds, and says to "use language to indicate when something should always be done". It also asks the generator to fill remaining space with a root file listing, README contents, and key source snippets.

12. **GitHub: all applicable instruction sets are supplied, so conflicting sets should be avoided.** — source: https://docs.github.com/en/copilot/how-tos/configure-custom-instructions/add-repository-instructions | publisher: GitHub | published: unknown | accessed: 2026-10-23 | confidence: high | class: guidance
    - Priority is personal, then repository, then organization, but "all sets of relevant instructions are provided" — priority is not filtering, so contradictions still reach the model.

13. **Cursor: `.cursor/rules/*.mdc` rules carry `alwaysApply`, `description`, and `globs`, and AGENTS.md is the plain-markdown alternative with nested-file precedence.** — source: https://cursor.com/docs/context/rules | publisher: Cursor (Anysphere) | published: unknown | accessed: 2026-10-23 | confidence: high | class: guidance
    - Four rule types: always applied, agent-selected by description, auto-attached by glob, or manual `@`-mention. Cursor supports root and nested `AGENTS.md`, combining parent and child with "more specific instructions taking precedence".

14. **Cursor: keep rules under 500 lines and avoid style-guide dumps, exhaustive command lists, rare edge cases, and duplicated code.** — source: https://cursor.com/docs/context/rules | publisher: Cursor | published: unknown | accessed: 2026-10-23 | confidence: high | class: guidance
    - It advises concrete examples and file references instead of copied content, and "Start simple. Add rules only when you notice Agent making the same mistake repeatedly." Its examples include "Never modify generated files in the `dist/` or `build/` directories" and reversible-migration rules.

15. **Google Gemini CLI: GEMINI.md is hierarchical and concatenated into every prompt, with just-in-time loading of subdirectory files.** — source: https://geminicli.com/docs/cli/gemini-md/ | publisher: Google | published: 2026-06-18 (page update) | accessed: 2026-10-23 | confidence: high | class: guidance
    - Load order is `~/.gemini/GEMINI.md`, then workspace and parent directories, then JIT files discovered when tools touch a directory; `@file.md` imports modularize it, and `context.fileName` can be set to `["AGENTS.md", ...]`.

16. **Thoughtworks: "agent instruction bloat" is a Caution on the Technology Radar.** — source: https://www.thoughtworks.com/radar/techniques/summary/agent-instruction-bloat | publisher: Thoughtworks | published: 2026-04-15 | accessed: 2026-10-23 | confidence: high | class: practice
    - Context files accumulate codebase overviews, architecture explanations, conventions, and rules until they conflict; models attend less to the middle of long contexts. It says research suggests hand-written files are often more effective than LLM-generated ones, and recommends progressive disclosure of only task-relevant instructions.

17. **Birgitta Böckeler (Thoughtworks): build the rules file incrementally, not upfront, and expect less context to be necessary as models improve.** — source: https://martinfowler.com/articles/exploring-gen-ai/context-engineering-coding-agents.html | publisher: martinfowler.com | published: 2026-02-05 | accessed: 2026-10-23 | confidence: high | class: practice
    - Copying a stranger's config risks repeating or contradicting instructions; there are "no unit tests for context engineering", so a setup has to be used for a while before judging it. She also warns of an "illusion of control": instructions raise the probability of good output but never enforce it.

18. **A 1,650-session factorial study found no detectable adherence effect from file size, instruction position, file architecture, or adjacent-file contradictions.** — source: https://arxiv.org/abs/2605.10039 | publisher: arXiv (Damon McMillan) | published: 2026-05-11 | accessed: 2026-10-23 | confidence: high | class: evidence
    - Across 16,050 function-level observations on two TypeScript codebases and three models, size and conflict nulls had affirmative Bayes factors (BF10 0.05–0.10); the measured decay was within-session, ~5.6% lower odds of compliance per additional generated function. Caveat: this tests a bounded range and does not show that 10,000-line files are harmless.

19. **A separate evaluation found context files can reduce task success versus no file, with LLM-generated files worst.** — source: https://arxiv.org/html/2602.11988v1 | publisher: arXiv | published: 2026-02-12 | accessed: 2026-10-23 | confidence: medium | class: evidence
    - Reported figures: LLM-generated files cut success ~3% while raising inference cost >20%; developer-written files gained ~4% on AGENTbench at up to ~19% extra cost; navigation facts were redundant when the repo was already documented. Medium confidence because I read the abstract and a secondary summary, not the full paper.

20. **Apache Airflow's AGENTS.md shows the monorepo "operating manual" pattern, including explicit do-not-touch boundaries.** — source: https://github.com/apache/airflow/blob/main/AGENTS.md | publisher: Apache Airflow | published: unknown | accessed: 2026-10-23 | confidence: high | class: practice
    - It has environment setup, a generated commands block, repository structure, architecture boundaries, a security model, coding/testing standards, output conventions, PR rules, and `Ask first` / `Never` lists ("Never edit generated files by hand when a generation workflow exists", "Ask first: destructive data or migration changes"). Criticism: it is far longer than Anthropic's ~200-line target and closer to the instruction dump Thoughtworks cautions against.

21. **Temporal's Java SDK AGENTS.md is a compact example organized around commands, constraints, PR expectations, and a pointer to deeper docs.** — source: https://github.com/temporalio/sdk-java/blob/master/AGENTS.md | publisher: Temporal | published: unknown | accessed: 2026-10-23 | confidence: high | class: practice
    - Sections are Repository Layout, General Guidance, Building and Testing, Tests, Commit Messages and Pull Requests, Review Checklist, and "For more details see CONTRIBUTING.md." It records two non-inferable constraints — avoid changing public API signatures, and the SDK targets Java 8 — plus exact Gradle commands including single-test invocation.

22. **Google's Now in Android AGENTS.md is a directly relevant Android/Kotlin example: architecture, modules, variant-aware Gradle commands, and test conventions.** — source: https://github.com/android/nowinandroid/blob/main/AGENTS.md | publisher: Google / Android | published: unknown | accessed: 2026-10-23 | confidence: high | class: practice
    - It names Compose/Material 3, UDF with Coroutines/Flow, Hilt, Navigation, Room/DataStore, Retrofit/OkHttp, and WorkManager; gives commands like `./gradlew assembleDemoDebug`, `spotlessApply`, `{variant}Test --tests`, and Roborazzi screenshot tests; and states which test frameworks to use where. It is short and mostly non-inferable, though its architecture summary is the kind of content Anthropic's `/doctor` would trim.

## Convergence

- **Commands are the highest-value content**: exact build, lint, format, and test commands — including how to run one test — plus any nonstandard wrapper or environment prerequisite. Every vendor says some version of this.
- **Include only what the agent cannot infer**: project-specific conventions that differ from language/tool defaults, architectural boundaries, environment quirks, and gotchas. Exclude standard conventions and anything readable from the code.
- **Keep it short and scannable**: headers plus bullets, no dense prose, pruned regularly. Vendors disagree on the number but agree that bloat degrades adherence.
- **Layer by scope instead of growing one file**: root file for repo-wide rules, nested or path-scoped files for subsystems, nearest/most-specific wins; do not duplicate the same rule in several layers.
- **Make each rule concrete and testable**, preferably imperative, with the safe alternative stated alongside a prohibition; examples and file references beat abstract prose.
- **Use progressive disclosure for depth**: link to `CONTRIBUTING.md`, ADRs, or docs; use path-scoped rules, skills, or task prompts for content that is not needed every session.
- **State the social and safety contract**: PR/commit conventions, review checklist, and explicit "ask first" / "never" boundaries (secrets, generated files, destructive migrations, public API).
- **Treat the file as code**: check it in, review changes, update it when the agent repeats a mistake, and delete stale or contradictory rules.

## Disagreements

- **Project overview and architecture tour.** agents.md lists "Project overview" as a popular section (https://agents.md/), and GitHub's generator asks for a repo summary, layout, and even root-file listings (https://docs.github.com/en/copilot/how-tos/configure-custom-instructions/add-repository-instructions). Anthropic explicitly excludes "file-by-file descriptions of the codebase", and `/doctor` trims architecture overviews and directory layouts (https://docs.claude.com/en/docs/claude-code/memory).
- **How much command detail.** GitHub wants every bootstrap/build/test/run/lint step documented with versions, failure modes, and timings. Cursor says do not document every possible command because "Agent knows common tools like npm, git, and pytest" (https://cursor.com/docs/context/rules).
- **Size budgets.** Anthropic: under 200 lines. Cursor: under 500 lines. Codex: 32 KiB combined by default. The factorial study found no size effect in its tested range (https://arxiv.org/abs/2605.10039). These are heuristics from different products, not a shared measured threshold.
- **Whether the file helps at all.** Vendor docs assume it does; the ETH-affiliated evaluation reports neutral-to-negative average effects and higher cost (https://arxiv.org/html/2602.11988v1).
- **Generated vs. hand-written.** GitHub ships a Copilot prompt to generate the file, while Thoughtworks says hand-written files are often more effective than LLM-generated ones.

## Gaps

- **Greenfield repos with no code**: no vendor page addresses this. `/init` and Copilot's generator both analyze an existing codebase, and agents.md only says an agent can scaffold a file. Best next query: `"AGENTS.md" greenfield "no code yet"`.
- **Android/Kotlin-specific vendor guidance**: an example file was found, not official Android guidance on what to put in one. Best next query: `site:developer.android.com AGENTS.md agent instructions Gradle`.
- **Secrets in the file**: none of the fetched pages explicitly says "do not put secrets or tokens in AGENTS.md". Airflow's "never commit secrets" is about commits, not the instruction file. Best next query: `"AGENTS.md" secrets credentials do not include`.
- **OpenAI's own example file**: the root `AGENTS.md` returned 404 on `main`, and the agents.md link uses a `/-/` placeholder. Best next step: browse the `openai/codex` tree or try `codex-rs/AGENTS.md`.
- **Exact effect sizes and task types in arXiv 2602.11988**: only the abstract/secondary summary was read; fetch the full HTML/PDF to verify the 3%/4%/20%/19% figures.
- **Imperative vs. descriptive phrasing**: vendor guidance favors concrete imperatives, but no measured comparison was found; treat that as opinion, not evidence.
