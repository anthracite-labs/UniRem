# Digest — standard & adoption (round 1, assistant 1)

Lane: A. Standard & adoption. Budget used: ~12 sources / ~20 calls. Accessed 2026-10-23.

## Findings

1. **AGENTS.md is an unversioned Markdown convention, not an RFC-style spec; it is stewarded by the Agentic AI Foundation (Linux Foundation) and its canonical home is agents.md / `agentsmd/agents.md` (MIT).** — source: https://agents.md/ | publisher: AGENTS.md / AAIF | published: unknown (living document) | accessed: 2026-10-23 | confidence: high | class: standard
   - The site publishes purpose, examples and an FAQ only; the repo has no releases/tags (38 commits, `Technical_Charter.pdf`, copyright "AGENTS.md a Series of LF Projects, LLC"). No version number is stated anywhere on either; treat "AGENTS.md v1.x" claims sceptically.

2. **There is no formal compliance definition, so "AGENTS.md-compliant" is not a testable claim.** — source: https://github.com/agentsmd/agents.md/issues/24 | publisher: agentsmd/agents.md (GitHub issue) | published: 2025-08-26 | accessed: 2026-10-23 | confidence: medium | class: standard
   - Open issue #24, "Add formal definition of compliance with the AGENTS.md standard", requests exactly this and remains open. The FAQ instead says only: "No. AGENTS.md is just standard Markdown. Use any headings you like; the agent simply parses the text you provide."

3. **AAIF stewardship began 2025-12-09, when OpenAI donated AGENTS.md alongside Anthropic's MCP and Block's goose.** — source: https://www.linuxfoundation.org/press/linux-foundation-announces-the-formation-of-the-agentic-ai-foundation | publisher: Linux Foundation | published: 2025-12-09 | accessed: 2026-10-23 | confidence: high | class: standard
   - Release: "Released by OpenAI in August 2025"; "adopted by more than 60,000 open source projects and agent frameworks including Amp, Codex, Cursor, Devin, Factory, Gemini CLI, GitHub Copilot, Jules and VS Code". Charter adoption on 2025-12-08 and an individual-maintainer/TSC ladder are reported by a secondary source (https://javatask.dev/blog/agents-md-standard-part-2-who-governs-agents-md-now/, 2026-09-26, confidence medium) and are consistent with the charter PDF in the repo.

4. **AGENTS.md is not a superset of tool-specific files; every major tool still prefers its own file when both exist.** — source: https://docs.claude.com/en/docs/claude-code/memory | publisher: Anthropic | published: unknown (current) | accessed: 2026-10-23 | confidence: high | class: compatibility
   - Claude Code reads `AGENTS.md` only "when you have no `CLAUDE.md` in your working directory or above it"; Gemini CLI's default context filename is `GEMINI.md`; Cursor's `.mdc` project rules remain the richer mechanism; Copilot keeps `.github/copilot-instructions.md` as the Copilot-session default. Shared file + per-tool bridge is the working pattern, not one file replacing all.

5. **The standard states a "closest file wins" precedence rule, with chat prompts above all files.** — source: https://agents.md/ | publisher: AGENTS.md / AAIF | published: unknown | accessed: 2026-10-23 | confidence: high | class: standard
   - Exact FAQ wording: "The closest AGENTS.md to the edited file wins; explicit user chat prompts override everything." Monorepo guidance: "Place another AGENTS.md inside each package. Agents automatically read the nearest file in the directory tree, so the closest one takes precedence"; the OpenAI repo reportedly had 88 such files.

6. **The standard imposes no required fields, headings, size limit, or nesting limit; all real constraints come from the consuming tool.** — source: https://agents.md/ | publisher: AGENTS.md / AAIF | published: unknown | accessed: 2026-10-23 | confidence: high | class: standard
   - "Are there required fields? No." Popular sections are "offered as popular choices", not structure. Concrete caps are vendor-side: Codex 32 KiB combined, Claude Code 4 MiB per file, Cursor's <500-line guidance, Windsurf 12,000 chars per rule file.

7. **What belongs, per GitHub's analysis of 2,500+ agent files: commands first, testing, project structure, code style, git workflow, boundaries.** — source: https://github.blog/ai-and-ml/github-copilot/how-to-write-a-great-agents-md-lessons-from-over-2500-repositories/ | publisher: GitHub | published: 2025-11-19 (updated 2025-11-25) | accessed: 2026-10-23 | confidence: medium | class: evidence
   - "Cover six core areas: commands, testing, project structure, code style, git workflow, and boundaries"; "Put relevant executable commands in an early section"; "Code examples over explanations"; "'Never commit secrets' was the most common helpful constraint"; give stack versions, not "React project".

8. **What to leave out: anything the agent can derive from the code, and anything a linter/formatter already enforces.** — source: https://docs.claude.com/en/docs/claude-code/memory | publisher: Anthropic | published: unknown | accessed: 2026-10-23 | confidence: high | class: practice
   - Claude's `/doctor` auto-trim "cuts content Claude can derive from the codebase, such as directory layouts, dependency lists, and architecture overviews, and keeps pitfalls, rationale, and conventions that differ from tool defaults"; target "under 200 lines per CLAUDE.md file … Longer files consume more context and reduce adherence." Cursor says the same in rule form: don't copy entire style guides ("Use a linter instead"), don't document every command, don't duplicate the codebase; VS Code adds "Skip conventions that standard linters or formatters already enforce."

9. **Applied to a greenfield Android/Kotlin repo: pin the toolchain, the Gradle entry points, module boundaries, and the "never touch" list — omit directory tours, dependency inventories, and ktlint/detekt rules.** — source: https://github.blog/ai-and-ml/github-copilot/how-to-write-a-great-agents-md-lessons-from-over-2500-repositories/ | publisher: GitHub | published: 2025-11-19 | accessed: 2026-10-23 | confidence: medium | class: practice
   - Synthesis of findings 7–8, not a vendor Android doc: include JDK/AGP/Kotlin/Compose versions, exact `./gradlew` commands (assemble, unit test, lint, instrumented test), `libs.versions.toml` as version source, module graph and where new code goes, plus a Never list (`local.properties`, keystores/`keystore.properties`, secrets, editing generated/build output). Leave out what `./gradlew` and the linter already answer.

10. **Where symlinks need elevated privileges, prefer a one-line `@AGENTS.md` import in `CLAUDE.md` over a committed symlink.** — source: https://docs.claude.com/en/docs/claude-code/memory | publisher: Anthropic | published: unknown | accessed: 2026-10-23 | confidence: high | class: practice
   - Anthropic: where creating a symlink needs Administrator privileges or Developer Mode, use the `@AGENTS.md` import instead; Git may check a committed symlink out as a plain text file unless `core.symlinks` is enabled. Also: Edit/Write refuse to write through a symlink.

11. **Adoption lists overstate native default support: Gemini CLI and Aider are listed by agents.md but require explicit configuration.** — source: https://agents.md/ | publisher: AGENTS.md / AAIF | published: unknown | accessed: 2026-10-23 | confidence: high | class: compatibility
   - Aider: "Configure Aider to use AGENTS.md in `.aider.conf.yml`: `read: AGENTS.md`"; Gemini: `{"context": {"fileName": "AGENTS.md"}}`. Also note a naming collision: GitHub Copilot's "custom agents" feature uses per-persona files under `.github/agents/*.md`, which is not the root `AGENTS.md` convention.

12. **2025–2026 changes: released Aug 2025, donated Dec 2025, broad multi-vendor adoption, but no confirmed spec revision.** — source: https://openai.com/index/agentic-ai-foundation/ | publisher: OpenAI | published: 2025-12-09 | accessed: 2026-10-23 | confidence: medium | class: standard
   - Adoption milestone: 60,000+ projects by Dec 2025 (25.1k stars / 1.9k forks on the standard's repo as of this session). Claims of a drafted "v1.1" with optional `description`/`tags` frontmatter (https://codex.danielvaughan.com/2026/06/05/agentic-ai-foundation-agents-md-mcp-linux-foundation-codex-cli-developers/, 2026-06-05) could not be confirmed from any primary source and should be treated as unverified.

## Contradictions

- **Nearest-file-wins vs concatenate-everything.** The standard says "the closest AGENTS.md to the edited file wins" (https://agents.md/), while Codex documents concatenation root→cwd where "files closer … override earlier guidance because they appear later" (https://learn.chatgpt.com/docs/agent-configuration/agents-md). A commenter on the standard's own tracker calls this out: "The Codex instructions say exactly the opposite of what the AGENTS.md website implies is the spec" (https://github.com/agentsmd/agents.md/issues/53, 2026-04-28) — unresolved.
- **Copilot precedence.** GitHub repo docs say "the nearest `AGENTS.md` … will take precedence" (https://docs.github.com/en/copilot/how-tos/configure-custom-instructions/add-repository-instructions), while VS Code and Copilot CLI docs say there is "no general precedence order" among instruction files and that they are additive (https://code.visualstudio.com/docs/copilot/customization/custom-instructions, https://docs.github.com/en/copilot/how-tos/copilot-cli/customize-copilot/add-custom-instructions). Likely surface-specific, but not reconciled in either doc.
- **"Supported" vs "loaded by default" for Gemini CLI.** agents.md and the AAIF release list Gemini CLI as an adopter, yet current Gemini docs default to `GEMINI.md` and require `context.fileName` for `AGENTS.md`. Not a factual conflict, but the adoption list reads as native support when it is configuration.

## Gaps

- **No primary evidence of an AGENTS.md spec revision/version in 2026.** Searched "AGENTS.md 2026 specification update governance"; only secondary blogs claim a "v1.1" draft with optional frontmatter. Next query: `site:github.com/agentsmd/agents.md "v1.1" OR frontmatter OR "technical charter"`.
- **Amp, Factory, Jules, Aider loading mechanics unconfirmed from vendor docs.** Only the standard's logo list and Aider's `read: AGENTS.md` snippet were verified. Next: `ampcode.com manual AGENTS.md` / `docs.aider.chat conventions AGENTS.md`.
- **OpenCode ancestor-merge semantics ambiguous** ("first matching file wins in each category" could mean nearest-only). Next: `opencode nested AGENTS.md subdirectory merge` or the OpenCode v2 rules page (guessed `/v2/docs/rules` returned 404).
- **Copilot cloud agent token limits/truncation for AGENTS.md** not documented in the pages read. Next: `docs.github.com copilot AGENTS.md size limit truncation`.
- **Codex mid-session reload behavior** conflicts between "once per run" docs and the open tracker asking for dynamic nested loading (https://github.com/openai/codex/issues/12115). Next: `openai codex AGENTS.md reload subdirectory 2026`.
