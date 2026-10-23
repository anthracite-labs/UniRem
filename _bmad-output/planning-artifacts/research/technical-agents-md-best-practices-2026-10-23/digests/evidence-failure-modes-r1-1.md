# Digest — evidence & failure modes (round 1, assistant 3)

Lane: empirical evidence, experience reports, failure modes, contrary evidence. Budget used: ~12 sources / ~20 calls. Accessed 2026-10-23.

## Findings

### Empirical

1. **Repository context files did not improve task success in the only controlled multi-agent benchmark, while raising inference cost by roughly 20%.** — source: https://arxiv.org/abs/2602.11988v3 | publisher: ETH Zurich (Gloaguen et al.) | published: 2026-02-12 (v3 2026-09-29) | accessed: 2026-10-23 | confidence: high | class: evidence
   - 300 SWE-bench Lite tasks plus 138 CTXbench instances from 12 niche Python repos; 4 agent/model pairs; one sample per task. LLM-generated files: −0.5% (SWE, p=0.87) and −2% (CTX, p=0.37) success, with cost +20%/+23% (p<0.001). Developer-written files: +2.4% (p=0.21), 7 points better than LLM-generated (p=0.038), up to +19% cost.

2. **Agents obey the file; the cost comes from extra work, not disobedience.** — source: https://arxiv.org/abs/2602.11988v3 | publisher: ETH Zurich | published: 2026-02-12 | accessed: 2026-10-23 | confidence: high | class: evidence
   - When a context file mentioned `uv`, agents used it 1.6× per instance versus <0.01× when absent; repo-specific tools 2.5× versus <0.05×. They ran more tests, grepped/read/wrote more files, and spent +22% (GPT-5.2) / +10% (GPT-5.1 mini) reasoning tokens on SWE-bench.

3. **Repository overviews and directory maps did not speed up localization.** — source: https://arxiv.org/abs/2602.11988v3 | publisher: ETH Zurich | published: 2026-02-12 | accessed: 2026-10-23 | confidence: medium-high | class: evidence
   - 8/12 developer files included an overview; 95–100% of files from three of four generators were flagged as containing one. Steps before first touching a PR-relevant file were not meaningfully reduced; GPT-5.1 mini wasted steps locating and re-reading a context file already in its prompt.

4. **File length did not predict success across 24–2,003 words.** — source: https://arxiv.org/abs/2602.11988v3 | publisher: ETH Zurich | published: 2026-02-12 | accessed: 2026-10-23 | confidence: medium | class: evidence
   - Mean 641 words; neither length nor instruction category had a strong performance effect. This is a null result inside the observed range — it does not establish that a 3,000-word file is safe, but it undercuts claims of a measured line-count cliff.

5. **The viral "AGENTS.md reduces success" claim is an overread of the paper's first version.** — source: https://arxiv.org/abs/2602.11988v3 (v1 vs v3) | publisher: ETH Zurich | published: 2026-02-12 (v3 2026-09-29) | accessed: 2026-10-23 | confidence: high | class: evidence
   - v1's abstract said files "tend to reduce task success rates"; v2/v3 say "does not generally improve task success rates." The measured deltas were −0.5% and −2%, both non-significant; cite the revision, not the headline.

6. **A static 8KB docs index in AGENTS.md produced a +47-point gain on post-cutoff API tasks.** — source: https://vercel.com/blog/agents-md-outperforms-skills-in-our-agent-evals | publisher: Vercel | published: 2026-01-27 | accessed: 2026-10-23 | confidence: medium | class: evidence
   - Baseline 53%, skill default 53% (skill never invoked in 56% of cases), skill plus explicit instruction 79%, AGENTS.md index 100% across build/lint/test. 40KB was compressed to 8KB with no loss; tasks were Next.js 16 APIs absent from training data. Vendor eval; the post does not disclose n, so treat the effect size as indicative.

7. **Instruction-following degrades as instruction count rises, and earlier instructions win.** — source: https://arxiv.org/abs/2507.11538 and https://arxiv.org/abs/2509.21051 | publisher: Distyl AI; EMNLP Findings | published: 2025-07-15; 2025-09-25 | accessed: 2026-10-23 | confidence: medium | class: evidence
   - IFScale: 20 models across 7 providers, 10–500 simultaneous constraints; best model reached 68% at 500, reasoning models stayed near-perfect through 100–250, with a bias toward earlier instructions. ManyIFEval: 10 LLMs, up to 10 instructions; prompt-level accuracy fell consistently as count rose. Synthetic tasks, not repo files.

8. **Real files are long, accretive, and almost never pruned; they are build/test manuals more than guardrails.** — source: https://arxiv.org/abs/2511.12884v2 | publisher: NAIST/Kasetsart/Queen's (Chatlatanagulchai et al.) | published: 2026-08-09 | accessed: 2026-10-23 | confidence: high | class: evidence
   - 2,303 files from 1,925 repos; median 335.5 (Codex), 485 (Claude), 535 (Copilot) words. 67% of Claude files were edited in multiple commits; median gap ~1 day; median deletions <15 words versus 57 words added per Claude commit; 92% of 100 sampled commits were substantive. Testing 75.9%, implementation 70.8%, architecture 68.1%, security 14.8%, performance 14.5%.

### Experience reports

9. **OpenAI's million-line agent-built repo keeps AGENTS.md to ~100 lines and uses it as a map.** — source: https://openai.com/index/harness-engineering/ | publisher: OpenAI | published: 2026-02-11 | accessed: 2026-10-23 | confidence: medium (practice), low (causal) | class: practice
   - 1M LOC, ~1,500 merged PRs, 3 engineers, 5 months; the initial AGENTS.md was itself written by Codex. The file "serves primarily as a map, with pointers to deeper sources of truth elsewhere"; no ablation isolates its effect.

10. **A two-repo audit found contradictions, not byte count, were the real damage — and it did not measure quality.** — source: https://www.digitalapplied.com/blog/ai-agent-instruction-file-audit-what-we-cut | publisher: Digital Applied | published: 2026-08-29 | accessed: 2026-10-23 | confidence: low-medium | class: practice
   - Cuts of ~11% (55KB→48.7KB) and ~50% (20,587B→10,255B). Found four live contradictory instruction pairs, one linter ruleset duplicated across five per-tool files, six uninstalled apps listed as installed, and a backup described as 32 files when the mirror held 92 and was 71 days stale. All 14 deterministic gates stayed green; the authors explicitly ran no output-quality comparison.

11. **Anthropic's own guidance is under ~200 lines, with deterministic constraints moved out of prose.** — source: https://claude.com/blog/steering-claude-code-skills-hooks-rules-subagents-and-more | publisher: Anthropic | published: 2026-06-18 | accessed: 2026-10-23 | confidence: medium | class: practice
   - "Keep CLAUDE.md under 200 lines"; every line loads into every session and "consumes tokens and dilutes adherence to the instructions that actually matter." "Never do this" in CLAUDE.md is "the wrong tool"; if behavior must happen reliably, use a hook or settings rather than prose.

12. **What teams actually measure is task success, steps, cost, and tokens — not PR rework or review comments.** — source: https://arxiv.org/abs/2602.11988v3; https://vercel.com/blog/agents-md-outperforms-skills-in-our-agent-evals; https://www.digitalapplied.com/blog/ai-agent-instruction-file-audit-what-we-cut | publisher: ETH Zurich; Vercel; Digital Applied | published: 2026-02-12; 2026-01-27; 2026-08-29 | accessed: 2026-10-23 | confidence: high | class: practice
   - ETH uses resolution rate, steps, inference cost, and reasoning tokens; Vercel uses pass rate on held-out tasks; Digital Applied measured bytes and 14 CI gates and reported no quality delta. No published study tying AGENTS.md to PR rework rate, CI-failure rate, review-comment count, or time-to-first-correct-edit was found; "green gates are a regression signal, not a quality result."

### Failure modes

13. **Contradictory rules coexist within one file and across layers, and the agent has no principled tie-break.** — source: https://www.digitalapplied.com/blog/ai-agent-instruction-file-audit-what-we-cut and https://developers.openai.com/codex/agent-configuration/agents-md | publisher: Digital Applied; OpenAI | published: 2026-08-29; undated | accessed: 2026-10-23 | confidence: high (existence), medium (prevalence) | class: failure
   - One file said spawn subagents in parallel (line 121) and do not over-delegate (line 123); a rebuild document instructed recreating a defect the drift checker now blocks; a fact pack declared its stalest section authoritative. Codex concatenates root→cwd and calls later files "override," which is positional, not semantic.

14. **Nested loading and hard caps are tool-specific; the same tree can load everything, nearest-only, or be silently truncated.** — source: https://developers.openai.com/codex/agent-configuration/agents-md and https://github.com/microsoft/vscode/issues/271489 | publisher: OpenAI; Microsoft | published: undated; 2025-10-15 | accessed: 2026-10-23 | confidence: high | class: failure
   - Codex includes at most one file per directory and stops adding once the combined size reaches `project_doc_max_bytes` (32 KiB default), so a large modular repo can lose rules without warning. VS Code/Copilot ignored nested AGENTS.md entirely in Oct 2025 and later added experimental support in v1.105 — re-test old complaints against current versions.

15. **Staleness is structural: files accrete additions and rarely delete.** — source: https://arxiv.org/abs/2511.12884v2 and https://www.digitalapplied.com/blog/ai-agent-instruction-file-audit-what-we-cut | publisher: Agent READMEs; Digital Applied | published: 2026-08-09; 2026-08-29 | accessed: 2026-10-23 | confidence: high | class: failure
   - Median 57 words added per Claude commit versus <15 deleted; 92% of sampled commits changed instructions. The audit found a wrong default model, a post count of ~1,000 when the live count was >2,100, and a backup described as 32 files when the mirror held 92 — all stated with the same confidence as correct facts.

16. **Prose security rules do not gate tool calls; documented leaks happened anyway.** — source: https://github.com/anthropics/claude-code/issues/2142, https://github.com/anthropics/claude-code/issues/32523, https://github.com/anthropics/claude-code/issues/58043 | publisher: Anthropic issue tracker (user reports) | published: 2025-06-16; 2026-03-09; 2026-05-11 | accessed: 2026-10-23 | confidence: medium | class: failure
   - #2142: 150+ lines of "NEVER COMMIT API KEYS" in CLAUDE.md; the agent committed Gmail/Maps/Firecrawl keys across three incidents in one session. #58043: a global "never display credentials" rule did not stop `cat ~/.claude/settings.json`, exposing a live Pinecone key and forcing rotation. User-filed reports, not an audited study; the consistent pattern is that intent prose is not an enforcement layer.

17. **Literal obedience to a stale rule can recreate the defect the rule was meant to prevent.** — source: https://claude.com/blog/steering-claude-code-skills-hooks-rules-subagents-and-more and https://www.digitalapplied.com/blog/ai-agent-instruction-file-audit-what-we-cut | publisher: Anthropic; Digital Applied | published: 2026-06-18; 2026-08-29 | accessed: 2026-10-23 | confidence: medium | class: failure
   - Anthropic says a prohibition in CLAUDE.md is the "wrong tool" when something absolutely must not happen. Digital Applied's rebuild document, step 7, instructed a future agent to recreate a package-manager setup that the current drift checker flags as a known defect — correct obedience to an obsolete instruction.

18. **Duplicated per-tool instruction files drift into five conflicting copies.** — source: https://www.digitalapplied.com/blog/ai-agent-instruction-file-audit-what-we-cut | publisher: Digital Applied | published: 2026-08-29 | accessed: 2026-10-23 | confidence: low-medium | class: failure
   - One linter's ruleset existed near-verbatim in AGENTS.md, GEMINI.md, a Cursor rules file, a Windsurf rules file, and `.github/copilot-instructions.md`, while Biome plus a formatting hook already enforced it. The duplication accreted one tool at a time; the audit calls it the "most widely reproducible finding" in the estate.

### Contrary evidence

19. **Steelman: AGENTS.md is static, unstructured, always-on prose, so it cannot be governance — only a briefing.** — source: https://www.sonarsource.com/blog/agents-md-is-a-workaround-not-a-solution | publisher: Sonar | published: 2026-09-14 | accessed: 2026-10-23 | confidence: medium | class: contrary
   - No schema or required fields; "we use hexagonal architecture" is not the dependency graph; always-on loading is simultaneously too generic for the task and too large to use reliably; models underweight middle-of-context rules (Sonar cites Lost in the Middle and Chroma's Context Rot). Sonar concedes it is good for stable operational facts and says "if you do not have one, write one today," but warns teams treating it as governance have made a category error. The vendor sells a competing context product; weigh accordingly.

## Strength of evidence

- **The causal claim rests on one study.** ETH's benchmark is the only controlled, multi-agent, multi-model ablation found; high confidence that LLM-generated context files do not improve success and cost ~20% more, medium confidence for developer-written files (small +2.4%, non-significant) and for generalization beyond Python, 4 agents, one sample per task.
- **The strongest positive counterexample is a vendor eval.** Vercel's 53%→100% result is large and mechanistically plausible — always-present, version-matched, post-cutoff knowledge — but it is a small, undisclosed-n, vendor-run suite testing API docs, not process rules or a general repository briefing.
- **Density research is robust but synthetic.** IFScale and ManyIFEval show count/position effects on keyword or style constraints; ETH's AGENTS.md-specific length ablation found no effect across 24–2,003 words. "Keep it under 200 lines" is therefore a cost-and-contradiction heuristic, not a measured compliance cliff.
- **Failure modes are documented but prevalence is unknown.** The contradiction, staleness, duplication, and security accounts come from one self-audit and user-filed issues, not random samples. They establish that these failures are possible and expensive, not how common they are.
- **No longitudinal or operational metrics exist.** OpenAI's 5-month report and Digital Applied's one-day audit are the closest to experience reports; neither isolates AGENTS.md's contribution to outcomes, and neither publishes PR rework, CI-failure, or review-comment deltas.

## Gaps

- **No numbers-backed 6–12 month team retrospective found.** Multiple phrasings searched; results were vendor advocacy, template lists, or corpus descriptions. The absence is itself a finding: claims that a maintained file improves team outcomes over time are currently not publicly evidenced.
- **No Android/Kotlin/Gradle-specific study.** ETH is Python, Vercel is Next.js. The closest inference is that version-matched Gradle/AGP/Compose facts are the kind of non-discoverable content most likely to pay off, but that is extrapolation, not measurement.
- **No rigorous measurement of secrets committed inside AGENTS.md/CLAUDE.md.** Issue reports of prose security rules failing to prevent leaks elsewhere exist, plus vendor scans claiming most files lack security boundaries, but no primary study quantifying credentials pasted into the instruction file itself.
- **No standard metric for "the file is working."** PR rework rate, CI failures, review comments, tool calls, and time-to-first-correct-edit are not reported in any AGENTS.md study found; ETH's success/steps/cost/token set is the de facto research standard.
- **Nested-conflict prevalence and current tool behavior are version-dependent.** Copilot's nested-file support changed during the period studied; Codex's concatenation and 32 KiB cap are documented but may change. Any claim about nested precedence should be re-tested on the exact agent versions in use.
