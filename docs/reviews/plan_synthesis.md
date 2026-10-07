## Where they agree (and we must act)

Despite the grade gap (C+ vs C-), the two reviews converge on the same facts, and every one of them is actionable this session:

1. **T06-013 is the canonical failure and nothing catches its class.** The model confidently said powering on inherited devices on day one is okay; no layer vetoed it; the grader that failed it did so for the wrong reason (missing phrase, not dangerous advice). Both demand a *deterministic, mechanical* interception — not a better prompt, not a better model.
2. **The evaluated artifact is not the shipped artifact.** The validated pipeline lives only in the Python harness; the APK has a stub chat screen, a keyword router with a dead boost, and 1-of-5 secret detectors. Both also agree the deterministic core (walkthrough, corpus browser, scam checker, secret detection) is real, tested, and shippable.
3. **Secret handling is wrong in both directions.** The harness can transit a secret to the cloud (sail_answer fallback on llama failure), and the Android detector misses whole formats (WIF, xprv/xpub, hex, SLIP-39) and is fragile to formatting. Refuse-don't-annotate is the spec; neither implementation honors it.
4. **The gate numbers don't measure the shipped configuration.** Mixed backends in the FINAL topical gate, GLM-5.3 judging GLM-5.3 answers, judge truncation at 1800 chars, 25 T10 items gated on one unreviewed entry, top_k 5 vs 8, coverage reported as 81/82/87/88%, EVAL_RESULTS.md as a palimpsest with an unexplained 23→20 walkthrough regression. Both want local-only gate runs, one config, one canonical number.
5. **The model floor on real 8GB hardware is the pivotal unknown and it is unowned.** The 4B OOM was on a 4GB dev host and says nothing about phones. Neither reviewer will let the 1.7B ship on current evidence (30–50% delivery, one demonstrated confident wrong answer).
6. **Drop the Pro brain / model import from v0.1** (Kimi implicitly via deterministic-only, DeepSeek explicitly on supply-chain grounds), and **stop building process scaffolding** (spec §9a, weekly audit cron) around an app that has never answered a question on a phone.

## Where they disagree (your ruling, with reasoning)

**1. What v0.1 ships.** Kimi: deterministic-only now, chat in v0.2 behind hardware evidence. DeepSeek: wire the full pipeline end-to-end into Android now. **Ruling: Kimi, and it's not close.** DeepSeek's motivation is exactly right — the eval grades a system that doesn't ship — but their fix is a multi-week port (JNI, bundled model, on-device eval parity) that cannot precede the model-floor answer. The cheaper way to satisfy their motivation is to remove the untested surface: ship only what is deterministic and tested. DeepSeek's own #3 concedes this — if the model fails the bars, "ship deterministic-only for v0.1" is their answer, and the 1.7B already fails those bars. Nothing is foreclosed; every harness fix below transfers directly to v0.2.

**2. How to fix the answer step.** Kimi: post-model deterministic veto plus verbatim warnings. DeepSeek: pre-model mechanical extraction — select spans, model formats, verify, fall back to raw spans. **Ruling: both, sequenced, with one correction.** The veto goes in now — it's model-agnostic, ~an hour, and works whether the backend is 1.7B, 4B, or GLM. The extractive architecture is the right v0.2 design, *but* DeepSeek's proposal to bake the eval items' must_include phrases into the runtime span-selector is teaching to the test: the eval would grade the system against its own answer key and every number becomes meaningless. Spans must come from the corpus's own structure (claims + warnings fields — exactly what Kimi's "return top 2–3 entries complete" simplification already points to), never from eval fixtures. DeepSeek's deterministic span-fallback (no model at all) is the single best idea in either review; it becomes the v0.2 default answer mode.

**3. The semantic judge.** Kimi: repair it (truncation, field collapse, backend split) and keep it in the gate. DeepSeek: demote to dev insight; gates must be deterministic. **Ruling: DeepSeek's structure, Kimi's repairs.** A non-reproducible LLM judge cannot be a zero-tolerance release gate. Release gates become deterministic-only: strict substring + must_not_contradict + citation resolution. The judge survives as a GLM-5.3 dev-time diagnostic — precisely the sanctioned use of Sail flex — with the 1800-char truncation fixed, never mixed into gate numbers, never grading its own backend's answers.

**4. Diagnosis of the 34%.** Kimi: model capability, honestly identified. DeepSeek: system design — we asked a 1.7B to do extractive summarization with citation fidelity, which it cannot do. **Ruling: DeepSeek's framing is more actionable** (it implies a fixable contract with the model rather than a waiting game), but the shipping decision is identical under either diagnosis, so we spend zero session time on it. The distinction only matters for v0.2, where the floor test and the extractive pipeline settle it empirically.

**5. Model floor timing.** Kimi: defer to a v0.2 decision. DeepSeek: do it now; biggest unknown in the project. **Ruling: DeepSeek on importance, Kimi on sequencing.** It doesn't block v0.1, so it doesn't own session time — but we prep the on-device test now so it's a 30-minute run the moment an 8GB phone is in hand, with the decision rule pre-agreed so the result can't be argued away.

**6. Smaller calls.** Walkthrough fail-closed (Kimi): adopted, 15 minutes. Walkthrough interactivity (DeepSeek): deferred to v0.2 — it's a feature build, and a calm, correct, static document is defensible for v0.1. Corpus browser plain-text default with technical behind a toggle (DeepSeek): adopted, trivial. Retrieval simplification and dual-corpus-schema unification: deferred to the v0.2 port — no point simplifying retrieval for an app whose v0.1 has no answer path.

## THE PLAN

Session exit criterion: items 1–6 done, one clean re-run published as the new canonical baseline. Items 1–3 are the safety core (~3 hours); if the session dies after item 3, the system is materially safer. GLM-5.3 absorbs all drafting load (must_not_contradict lists, Kotlin port, test cases, DR/PJ/HO drafts, corpus rewrite) — that leverage is what makes eight items feasible at all.

**1. Wire the deterministic veto into the answer path** — [DO NOW]
- **What:** Make wrong affirmative advice structurally unshippable; default inheritance mode to "do nothing yet." This is the item both reviews insist on.
- **How:** In `answer_step.py`, after generation: run `Guardrails.scrub()` then `Guardrails.check_constraints()`. On any must_not_include hit, discard the candidate entirely and return the refusal template plus the retrieved entries' `warnings` arrays verbatim. In walkthrough mode, deterministically append the `warnings` fields of all retrieved entries regardless of model output. Prepend "Do nothing yet. Nothing is urgent. Take your time." to inheritance-mode action questions (DeepSeek #5), enforced as must_include on T06 items. Fix `run_eval.py` to pass `candidate_answer` through `compose()` so the guarded path is the only path — today the guardrail layer is dead code in the eval.
- **Impact:** T06-013-class failures become impossible in any backend config without improving the model at all; panic-action default addresses the #1 loss vector. Walkthrough strict may tick up (deterministic warnings satisfy several must_include targets by construction), but the floor is the point, not the score.
- **Time:** ~1–1.5 hours.

**2. Deterministic contradiction and citation grading** — [DO NOW]
- **What:** Grade "actively dangerous" separately from "incomplete"; make citations verifiable; keep unreviewed corpus out of gate counts.
- **How:** Add `must_not_contradict` to the eval item schema. GLM-5.3 drafts the lists from corpus `warnings` arrays plus the danger-test set (e.g., T06-013: "powering on the device on day one is okay"); engineer reviews every entry — zero-tolerance sets get human review, no auto-merge. In `grade()`: deterministic substring check, any hit fails the item regardless of everything else, 100% required on all slices. Add citation resolution: every `[Txx:title]` must resolve against titles of retrieved chunks for that item; unresolved = fail. Exclude items backed by `reviewed=false` entries from gate aggregates (T10's 25 items, plus 6 across T01/T07/T08); report them separately as provisional.
- **Impact:** The eval finally distinguishes phrasing miss from dangerous advice. Expect current passes to drop on this axis (DeepSeek estimates 3–5 of 20 T06 passes) — that drop is the measurement working. Hallucinated citations, a trust exploit in a custody app, now fail deterministically.
- **Time:** ~1.5 hours (review is the bottleneck).

**3. Secret refusal and scam short-circuit scoping** — [DO NOW]
- **What:** Secrets never reach any backend; scam verdicts stop firing on innocent questions.
- **How:** In `answer_step.py`: when `SecretDetector` fires, return the warning template deterministically and exit — never `build_messages`, never llama, never `sail_answer`. This closes the only code path in the repo where a seed phrase could transit a network. Gate `scam_verdict_answer()` on router mode == scam-check or an explicit pasted-message flag instead of running unconditionally (fixes T06-039, where "right now" tripped the urgency regex on a price question).
- **Impact:** Removes a demonstrated false verdict — alarm fatigue is a harm vector, because the one real scam verdict must not be devalued. T06-039 can now pass legitimately.
- **Time:** ~30–45 min.

**4. Port the full SecretDetector to Kotlin** — [DO NOW]
- **What:** The shipped app detects every secret format the Python side detects, robustly.
- **How:** Extend `MemoryGuards.kt` with WIF, xprv/xpub, hex-key, and SLIP-39 detectors (port the Python regexes and sliding-window fuzz). Harden BIP-39: normalize whitespace and strip punctuation before the 8-word run check so line breaks and interleaved commentary can't evade it. JVM tests for every format including mangled pastes. GLM-5.3 drafts the port and test cases against the Python reference; engineer reviews.
- **Impact:** Closes the shipped-app gap where a pasted xprv or WIF produces no warning (spec principle 2 violation in the artifact) and removes the false-negative evasion DeepSeek flagged.
- **Time:** ~1–1.5 hours.

**5. Cut chat from v0.1 — ship deterministic-only** — [DO NOW]
- **What:** v0.1 = walkthrough + corpus browser + scam checker + full secret detector. No LLM, no model import, no Pro brain.
- **How:** Remove `ChatPlaceholderScreen` from navigation (code stays on a branch). Delete the dead `CorpusRouter` boost (`bestIntent in entry.topic_ids.map { it.lowercase() }` compares "seed_phrase" to "t02" — can never match). `WalkthroughScreen` fails closed on `walkthrough.json` parse errors with an explicit error state. `CorpusBrowserScreen` defaults to plain text, technical fields behind a toggle. No SAF model import in v0.1. Add the CI workflow (build + JVM tests on push) that round 1 asked for and round 2 couldn't find.
- **Impact:** The shipped artifact matches the class of the evaluated artifact; no grieving user meets the 1.7B's 30–50% delivery rate; the unverified model-file transfer surface is removed entirely.
- **Time:** ~1 hour.

**6. Eval hygiene: one config, local-only gates, clean re-run** — [DO NOW]
- **What:** Gate numbers measure the shipped configuration and nothing else.
- **How:** Remove the GLM-5.3 failover from `run_eval.py`'s gate path — gates run local-1.7B only; cloud runs execute separately and are reported as ceiling estimates, never mixed. Unify top_k across `run_eval.py` and `run_retrieval_eval.py` (pick one number); publish one canonical coverage figure. Fix `TOPIC_KEYWORDS` in `guardian_core.py`: regenerate from the actual corpus tag distribution (T06=inheritance, T08=scam, T10=jargon) or drop `prefer_topics` and measure BM25+title-boost alone — either kills the silent mis-boost on the three highest-stakes categories. Fix `judge_semantic.py`'s `answer[:1800]` truncation (judge is diagnostic-only per ruling 3). Replace the EVAL_RESULTS.md palimpsest with one generated results file plus raw JSON per run. Then one full clean re-run of all 145 items with items 1–3 and the topic fix in — that run becomes the single canonical baseline, and the unexplained 23→20 walkthrough regression is either reproduced (real signal) or eliminated (config artifact).
- **Impact:** Ends GLM-grading-GLM in gate numbers; one number per slice per backend; removes a silent mis-routing class; converts an audit failure into a measurement.
- **Time:** ~1.5 hours + re-run.

**7. Author and run DR/PJ/HO; resolve the T06-040 contradiction** — [DO NEXT]
- **What:** The spec'd zero-tolerance release gates (DR=25, PJ=15, HO=15) have never been run; the corpus contradicts Decision #4 on the directory.
- **How:** GLM-5.3 drafts the 55 items against the eval design's definitions; engineer reviews all of them. Run against local-1.7B only under the item-6 config; any failure is release-blocking per spec §14 — note out_of_corpus_honesty sits at 0/2, so expect failures and treat them as findings. Separately: rewrite T06-040 to match Decision #4 — if named, ranked recommendations ship, they carry why-listed/date-ranked/how-to-reverify, and the "no endorsements" language goes. That rewrite is ~30 minutes and can be pulled into this session if items 1–6 land early.
- **Impact:** Release gates that gate releases actually exist and run against the shipped configuration; the model stops quoting a policy the product doesn't follow.
- **Time:** ~2–3 hours next session.

**8. Model floor on an 8GB phone — the v0.2 chat gate** — [NEEDS USER/REAL HARDWARE]
- **What:** The decision that determines whether chat exists at all, made on evidence.
- **How:** Prep now (~1 hour): bundle Qwen3-4B Q4_K_M with llama.cpp plus a runner that executes a fixed 40-item stratified eval subset on-device, logging tokens/sec, memory headroom, and pass rates. When hardware arrives: run it. Decision rule pre-agreed per DeepSeek: ≥70% walkthrough strict and ≥50% topical semantic on-device → 4B is the v0.2 primary; below that → v0.2 ships deterministic-only and the LLM defers to v1.0. The v0.2 chat architecture is already settled by ruling 2: deterministic span-selection from corpus claims/warnings → model as formatter → item-1 veto mandatory → deterministic span-fallback on verification failure. No chat ships without all four layers.
- **Impact:** Converts the project's biggest unknown into a 30-minute measurement with a pre-committed decision rule.
- **Time:** ~1 hour prep now; ~30–60 min run when hardware is in hand.

**Explicitly not doing (the "do nothing" list):** spec §9a stays frozen and the weekly audit cron stays dead until a vertical slice exists on a phone. Walkthrough interactivity, the Kotlin BM25/guardrails port, retrieval simplification, and corpus-schema unification are all v0.2 work bundled behind item 8's decision. The corpus status ledger is folded into item 7's authoring review rather than maintained as separate documentation.