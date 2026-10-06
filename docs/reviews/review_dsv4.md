# Review: Bitcoin Custody Guardian

## Top 5 Strengths

1. **The product insight is genuinely good.** "Grieving inheritor + fully offline + do-nothing-by-default" is a real, underserved, high-stakes niche. The threat model correctly identifies that *the app's own advice* is an attack surface — most projects never get this far. The "corpus is the moat, model is commodity" framing is strategically correct.

2. **The deterministic front-half / LLM back-half split is the right architecture for this domain.** Secret detection, routing, retrieval, guardrails, refusal templates — all deterministic — with the LLM constrained to grounded explanation over retrieved chunks. This is the correct inversion of the typical "LLM first, filters after" pattern. For a regulatory-adjacent, irreversible-action-adjacent domain, fail-closed before the model is exactly right.

3. **The eval set structure is unusually disciplined for this stage.** Zero-tolerance DR/PJ/HO sets as release gates, auto-gradable must_include/must_not_include, per-category pass rates, staleness checks. The 121 must_include phrases with 0 corpus coverage is a *feature* of the methodology — it caught the corpus gap before anyone shipped.

4. **Privacy is treated as architecture, not policy.** No INTERNET permission, no exported components, no shared storage, reproducible builds, F-Droid first. For the target user (someone whose situation itself is sensitive), this is not paranoia — it's the minimum viable trust model.

5. **Honest engineering status reporting.** The skeleton hasn't been compiled. The Kotlin port is broken. The corpus has 13 entries. The eval scored 0/40. All of this is stated plainly. That honesty about what's verified vs. aspirational is a good sign for a project where a false sense of completeness could genuinely hurt someone.

---

## Top 5 Risks / Wrong Turns

### 1. **The corpus is 13 entries and the largest eval category (T06, inheritance) has zero coverage. This isn't a bug — it's a scope inversion.**

The north-star user is the inheritor. The corpus should have been built *from the T06 eval items first* — every must_include phrase should trace to a corpus claim. Instead, you have T02/T03/T05-ish content (taxonomy, multisig) and the exact scenario the product exists for is unsupported. **Building a product for grieving heirs and having the day-one walkthrough be the *last* corpus content written is a roadmap error, not a task-ordering error.** Fix before touching the Android port: corpus is the product.

### 2. **The 0/40 eval result appears to have been treated as a "grader bug + small model" problem, not a "the system fundamentally cannot pass" problem.**

Read the T06-002 answer again. The model gave *wrong* advice — "put them all in the same box together" is about keeping split backups, not about throwing away the envelope. The model said "keep it safe and avoid sharing it" which is directionally fine, but it's glib where the spec demands precision. The root cause isn't the grader or Qwen3-1.7B. **It's that you're asking a 1.7B model to do high-stakes grounded summarization over a corpus that doesn't contain the answers.** The fix is corpus-first, not model-swap. If you swap to GLM-5.3 or Qwen3-8B and still score 0/40, the conclusion is that the corpus gap is total.

### 3. **The "two-stage" design has an unexamined broken middle.**

The deterministic pipeline hands the LLM a *retrieved context chunk*. But the retrieval is BM25 over 13 entries. At current corpus size, retrieval precision is effectively random. The guardrail enforcement (must_include/must_not_include) is described as deterministic, but if retrieval doesn't return the right chunk, the guardrails can't help — the LLM simply doesn't have the material to produce the target phrase. **There is a missing loop: eval must be run at the retrieval layer first, then at the compose layer, then at the LLM layer.** You cannot debug the LLM until you know the deterministic pipeline is feeding it the right context. Currently, the pipeline and the LLM are being evaluated as one black box.

### 4. **The corpus curation pipeline is under-specified relative to its strategic importance.**

The spec says "reviewed, rewritten into plain language, tagged, versioned" — but the actual workflow appears to be: Sail flex GLM drafts entries → you review. Given the eval showed the corpus needs ~200+ entries across 12 topics with citation-grade accuracy, this is the project's critical path. **Where is the contributor workflow? The entry template? The review checklist? The versioning/packaging format?** If the corpus is the moat, the *pipeline* for producing and reviewing it is the moat's drawbridge. Current state: 2 of 13 entries reviewed-approved. At that rate, this is a multi-month project before the app can answer a single T06 question.

### 5. **The Android port is being treated as a port when it should be treated as a rebuild with the deterministic pipeline as the spec.**

The Kotlin skeleton is currently broken (unresolved references). That's fine — it's early. But the bigger risk is that the Python prototype has evolved through the eval process (synonym tables, refusal regex, grading bugs) and the Kotlin side has a `CorpusRouter.kt` that is a *simplified* port. **A simplified port that doesn't match the Python behavior means the eval results don't apply to the shipped app.** The Kotlin port should be generated from the same eval-driven behavior contracts, not re-derived from the Python code by hand. This is a mechanical but serious correctness gap.

---

## Concrete Next Actions (ranked)

### 1. **Close the corpus gap against T06 first — do not pass Go.**

Pick 20 of the 40 T06 items. Extract every must_include phrase. Write corpus entries whose `claims` and `plain` fields are designed to enable the deterministic pipeline (BM25) to retrieve them for those prompts, and the LLM to synthesize them into target-phrase-preserving answers. Run retrieval-only eval (does the right entry come back for each prompt?) before running compose/LLM eval. **Success criterion: retrieval precision for the 20-item subset ≥90%.**

### 2. **Build the corpus authoring pipeline before writing more entries.**

You need: entry template (plain/technical/claims/citations/warnings/risk_class/reviewer_flags), a human review checklist (source-verified, risk_class calibrated, citation present, plain-language readability pass), and a packaging/signing format for the bundled corpus. The Sail flex GLM drafting idea is fine — but the bottleneck is your review, so the pipeline should be designed to make review fast and mechanical. **If reviewing 8 entries takes more than an hour, the pipeline needs work.**

### 3. **Fix the Kotlin port to match the Python prototype's actual behavior — or decide the Python prototype is the reference implementation until corpus is done.**

Option A: port now, keep both in sync. Option B: defer Android work entirely until the corpus is at 50+ entries and the Python pipeline passes the T06 subset. I'd choose **Option B**, because every hour spent on Kotlin before the corpus exists is an hour spent on scaffolding for a house with no foundation. The Python prototype IS the product iteration loop for now.

### 4. **Define the model selection gate properly.**

Current trajectory: local Qwen3-1.7B Q4_K_M scored 0/40; spec mentions Q8 for 12GB devices, Q4_K_M default, and Sail flex GLM-5.3 fallback. But there's no explicit performance gate. **Proposal: the model is a pass/fail decision made against the eval set with a frozen corpus.** Pick 2-3 candidate models (Qwen3-4B/8B at Q4/Q8, GLM-5.3 sensible quant) and run the same T06 subset with the same retrieval pipeline. The model that passes with the fewest compute constraints wins. If none pass, the problem is the corpus/retrieval, not the model.

### 5. **Move the sail_flex GLM-5.3 fallback from answer step to corpus drafting and eval grading.**

The spec positions GLM-5.3 as an answer-step fallback — but architecturally, an online fallback on a no-network app is incoherent (it would need a network path, and the product's core differentiator is zero network). Instead, GLM-5.3's actual high-value use is: (a) batch drafting corpus entries for your review, (b) auto-grading eval runs with a human spot-check, (c) stress-testing eval items for ambiguity before you freeze them. **This removes the architectural tension and puts the flex spend where the real bottleneck is.**

---

## Fundamental Rethinks

### The app's scope may be too broad relative to the corpus-constrained reality.

The spec describes four personas and five modes (learn/scam-check/verify/directory/walkthrough). But the corpus has 13 entries. **Constraint reality: at current corpus velocity, the product ships as a T06 inheritance-walkthrough app first, with other modes as empty shells.** That's not a downgrade — it's a sharpening. The north-star user doesn't need a corpus browser, a scam checker, or a wallet directory. She needs a guided, patient, plain-language inheritance walkthrough that works. **Propose a v0.1 scope change: Inheritance Mode only, 30-50 high-quality T06/T02 entries, no directory, no scam checker beyond the deterministic secret-detector warnings, one bundled model.** Everything else is roadmap fiction for now. The spec even says "do nothing" is a valid answer — apply that to the roadmap.

### Rethink the citation model.

Eval must_include checks expect exact-ish long phrases ("nothing is lost by waiting"). The LLM is not going to reliably produce those phrases from paraphrase-level retrieval context. **Consider templated synthesis:** for danger-class and walkthrough items, the guardrail layer could insert the target phrases verbatim into the LLM's constrained output — either via a scaffold the model fills in, or via a post-generation reconciliation pass at the deterministic layer. The ground truth phrasing *is in the corpus*. The model's job should be to *select and assemble* the right claims, not to independently generate the right thing to say. This reduces the model-quality requirement and makes the eval more about retrieval correctness and less about generation quality.

---

## Verdict

The direction is right, some of the decisions are exactly what I'd want to see, and the eval discipline is better than most projects at this stage. The problem is the roadmap order and the smallness of the corpus. **If the corpus gets to 100+ entries with the T06 walkthrough as the foundation, the deterministic pipeline passing retrieval eval, and one model crossing the T06 threshold, this becomes a credible project. If the Android port remains the near-term focus while the corpus stays at 13 entries, the project is building a locked vault with no contents.**

The corpus is the product. Build that first. You already know this — the spec says it in section 7. But the actual work allocation doesn't reflect it yet. Align the work with the thesis.