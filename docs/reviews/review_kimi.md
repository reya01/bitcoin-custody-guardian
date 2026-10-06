## 1. Top 5 strengths

1. **Privacy as architecture, not policy.** Zero-permission manifest, no exported components, reproducible builds + F-Droid, weight hashes. Skeptics can verify it. This is your real differentiator — every other "offline AI" app is a promise; this is a fact.
2. **Deterministic front-half.** Secret detection, routing, and refusals happening *outside* the model is the only sane way to get reliability from a 1.7–4B model. Refusals bypassing the LLM entirely is exactly right.
3. **"Do nothing" as a first-class outcome.** Matches the actual loss vector (panic wipes, resets, "recovery agents"). The product optimizes for not-ruining-someone, not engagement.
4. **Eval-gate culture with honest failure reporting.** Publishing a 0/40 with root causes instead of hiding it is rare. Zero-tolerance DR/PJ/HO sets are the correct shape for a release gate.
5. **Corpus-as-moat framing.** Claim-level citations, dual plain/technical text, versioning. If executed, this is the defensible asset; the model is replaceable.

## 2. Top 5 risks / wrong turns

1. **Eval, corpus, and answer step are mutually incoherent — the 0/40 is a design bug, not just a model bug.** Your own report: none of the 121 must_include phrases exist in the corpus. You're demanding a *grounded* model recite sentences its only permitted source doesn't contain. Until every must_include is traceable to a corpus claim-id, every eval score is noise and every "root cause" analysis is fiction.
2. **Model floor chosen by dev host, not target device.** 1.7B was picked because the build box has 4GB RAM. The target is 8GB flagships, which run 3–4B Q4_K_M fine. T06-001 ("contact the person who owned the USB stick" — he's dead) is a comprehension failure below any safety floor. Related: the model *hedging scam calls* is disqualifying — scam verdicts must come from ScamRules deterministically; the LLM should only explain, never adjudicate.
3. **The "moat" is currently the weakest asset.** Single-vendor dependence (River Learn — license unverified, one broker's voice, and not CC-licensed, contra spec §7), zero T06 backup content, no T07 scam corpus *while evals for both exist*, and contradictory status ("2 approved/8 pending" vs "13 reviewed-unverified") — no corpus ledger exists.
4. **Premature infrastructure + dual-implementation drift.** Weekly audit crons, flex-tier corpus pipelines, judge models — while the app has never compiled and no answer has ever run on a phone. Meanwhile the safety core is being hand-ported Python→Kotlin, guaranteeing divergence in the exact code that must not diverge.
5. **Internal contradiction: offline-forever vs. versioned packs/directory.** "Signed, hash-pinned knowledge packs" can't reach an app with no network code path — they ship as APK updates or not at all. Worse, a "verified professionals" directory rots instantly offline, and every named professional becomes an impersonation target for exactly your user.

## 3. Next actions, ranked

1. **Write T06/T07 corpus entries first** (day-1 securing, metal backups, scam scripts) and make every existing eval must_include derivable from a specific claim-id. Create a single corpus status ledger. This is what makes eval scores meaningful.
2. **Split the grader.** Keep exact-substring zero-tolerance for DR/PJ/HO (those phrases are literal). Move topical sets to semantic grading (token-overlap now, rubric judge later). Raise the 240-token cap; bake the /no_think fix into the harness. Re-run — the corpus-gap failures should collapse, exposing true model failures.
3. **Decide the model floor on real hardware.** Qwen3-4B and Gemma-3-4B Q4_K_M via Termux/llama.cpp on an 8GB phone; score DR/PJ/HO + measure tok/s. Result determines whether the 6GB tier ships an LLM or deterministic-only mode.
4. **Compile the skeleton in CI** (GitHub Actions has Android SDKs your dev box lacks), port the 39 core tests to Kotlin, run on every push. Then wire ChatPlaceholder → llama.cpp JNI → bundled model → one real on-device answer. That vertical slice outranks everything else in the pipeline.
5. **Patch the spec now (cheap, prevents building toward impossible features):** APK-only corpus updates; directory = roles not names; correct the license claim; declare Kotlin canonical once the port lands.

Defer: weekly audits, flex corpus scaling, all personas beyond Inheritor/Verifier.

## 4. Fundamental rethinks

- **Make the answer step extractive-leaning, not generative.** Have the model select corpus sections and stitch quoted spans with light paraphrase rather than free-compose. Citations become true by construction, hallucination surface collapses, grading can check span fidelity, and the small-model burden drops. This is a tightening of what you already built (retrieval precedes generation), not a rebuild. Given the audience, free generation should survive only in jargon-decoding mode.
- **Drop the "verified next step" promise.** It's undeliverable offline and dangerous when stale. The pitch line overpromises; cut the directory to generic role guidance ("a bitcoin-literate estate attorney").
- The mission, the two-stage split, and offline purity all hold up under scrutiny. What's broken is sequencing: you built the process scaffolding (audits, evals, pipelines) before the content and the vertical slice, and you let the dev box pick the model. Reverse those two and the project is pointed the right way.