# SECOND-ROUND REVIEW — Bitcoin Custody Guardian

## 1. VERDICT

**Grade: C-**

This is a materially improved project since round one, but it is not yet on track to be a trustworthy inheritance-guidance app. The honest framing: you have built a very good *evaluation harness* and a mediocre *product*. The gap between them is the story.

What changed for the better is real. The corpus gap is closed (T06/T07/T08/T10 entries exist, 122 must_include phrases now traceable). Retrieval was upgraded to entry-level aggregation with per-entry caps and completion — this is the right shape for a grounded answerer. Scam verdicts are deterministic, which was non-negotiable. The wrong-refusal bug is fixed. Temperature 0 everywhere. The Android build compiles with 13 JVM tests green. The spec was patched on directory roles and licensing. These are not cosmetic fixes; they address the structural failures I flagged.

But the headline number is 34% overall pass rate (49/145), and the interpretation in Addendum 6 is too generous. "Retrieval coverage is 88%, so the corpus and retrieval are no longer the bottleneck" is only half true. Retrieval *surfaces* the right text 88% of the time, but the answer models deliver the target guidance only 28–30% of the time on topical slices and 50% on walkthrough items. That is not a model-capability problem you can wave away with "the Pro brain will fix it." It is a system-design problem: you are asking a 1.7B model to perform extractive summarization with citation fidelity over multi-paragraph chunks, and it cannot do that reliably. The GLM-5.3 fallback gets to 60–65%, which is still below any defensible release bar for an app whose wrong answer can cost someone their inheritance.

The deeper issue: **the eval is now grading the wrong thing.** You have a strict-substring gate for walkthrough slices and a semantic rubric judge for topical slices, but neither measures what actually matters — *would a grieving non-technical person take the right action after reading this answer?* T06-013 is the clearest evidence. The model answered "Yes — powering them on just to look is okay" to a question where the corpus says "do not power them on yet on day one." The strict grader failed it for missing the target phrase, but the real failure is worse: the answer is *actively wrong* for the day-one inheritance context. The grader caught the phrasing miss but not the safety miss. A semantic judge might have passed it, because "powering on to look is okay" is semantically coherent — it's just wrong for day one. Your eval architecture cannot currently distinguish "phrasing mismatch" from "dangerous advice," and that is the gap that matters.

The Android app is a shell. ChatPlaceholderScreen is a deterministic router stub with no LLM, no retrieval-to-answer pipeline, no guardrail enforcement on generated text. The 13 JVM tests cover MemoryGuards, ScamRules, and CorpusRouter — all deterministic components. None of them test the answer path, because there is no answer path. The app as it stands cannot answer a single user question. That is fine for a prototype, but the spec and eval results imply a product that is much further along than it is.

The architecture is right. The privacy story is right. The corpus is now real. The deterministic core is solid. But the central loop — user asks question, grounded answer comes back, answer is safe and correct — does not exist end-to-end anywhere, and the eval results show that even the harness version of that loop fails more often than it succeeds. That is a C-: the foundation is poured, but the house is not standing.

## 2. TOP 5 HIGHEST-LEVERAGE IMPROVEMENTS

### 1. Make the answer step extractive, not generative — and enforce it mechanically

The current answer prompt says "ASSEMBLE, do not invent" and "quote the corpus claims as close to verbatim as possible," but it still asks the model to *compose* an answer. A 1.7B model cannot reliably do this. The fix: change the answer step to a two-stage deterministic-plus-model pipeline.

**Concrete implementation:** After retrieval, run a deterministic span-selection step that identifies the specific corpus sentences containing the must_include target phrases (you already have the phrase list from the eval items — bake a runtime version into the app). Pass only those selected spans to the model with the instruction: "Output these spans in order, with light connective tissue. Do not paraphrase. Do not add claims. If a span does not fit, skip it." Then post-process the output to verify each selected span appears verbatim or near-verbatim in the answer; if not, fall back to outputting the spans directly with no model at all.

**Expected impact:** This collapses the hallucination surface to near zero, makes citations true by construction, and reduces the model's job from "summarize and compose" to "format and connect" — which a 1.7B can actually do. I would expect the walkthrough strict pass rate to jump from 50% to 70–80% immediately, because the target phrases will be present by construction rather than by model recall. The topical slices will improve less, but those are already gated by semantic judging.

### 2. Add a safety-specific grader that checks for *wrong* answers, not just missing phrases

T06-013 is the canary. The current grader checks must_include and must_not_include, but it does not check whether the answer contradicts the corpus guidance. A model can pass every must_include and still give dangerous advice.

**Concrete implementation:** For each eval item, add a `must_not_contradict` field listing the specific wrong claims the answer must not make (e.g., for T06-013: "powering on the device on day one is okay"). Grade it deterministically: if the answer contains the contradiction phrase, fail the item regardless of all other checks. Build this list from the corpus warnings and the danger-test set. Run it as a separate gate: 100% pass required on contradiction checks for all slices, not just DR/PJ/HO.

**Expected impact:** This catches the failure mode that matters most — confident wrong advice. It will likely drop your current pass rates further, which is the point: you need to know how often the system is actively dangerous, not just how often it misses a phrase. I would expect 3–5 of the current 20 T06 passes to fail this new check, which tells you exactly where the model is unsafe.

### 3. Decide the on-device model floor on real hardware, now

The eval results show local Qwen3-1.7B passing ~30% of its items and GLM-5.3 passing ~60–65%. The spec still says "1.7B primary, 4B optional Pro brain." That is backwards. The 1.7B is not good enough to ship, and the 4B was never tested because the dev host OOM-killed it.

**Concrete implementation:** Get an 8GB Android phone (or a cloud Android emulator with 8GB RAM). Run Qwen3-4B Q4_K_M via llama.cpp on it. Score the full eval set. Measure tokens/sec and memory headroom. If 4B passes at least 70% on walkthrough strict and 50% on topical semantic, make it the primary model and drop the 1.7B to a fallback for 6GB devices. If 4B also fails, the answer is not "ship 1.7B anyway" — it is "ship deterministic-only mode for v0.1 and defer the LLM to v1.0."

**Expected impact:** This is the single biggest unknown in the product. Until you know what the target hardware can actually run, every other decision is speculative. If 4B works, your pass rates roughly double. If it doesn't, you save months of building toward an impossible v0.1.

### 4. Wire the Android app to the real answer pipeline, end-to-end

The Android app currently has no answer path. ChatPlaceholderScreen routes and retrieves but does not generate. The Python harness has the full pipeline. These need to converge.

**Concrete implementation:** Port the Python `answer_step.py` logic to Kotlin (or better: define the answer-step contract as a JSON schema and implement it natively in Kotlin against the same corpus assets). Wire ChatPlaceholderScreen to call the real pipeline: secret detection → routing → retrieval → answer generation (llama.cpp JNI) → guardrail scrub → display. Run the same eval set against the Android implementation, not just the Python harness. The 13 JVM tests are necessary but not sufficient; add integration tests that run the full pipeline on-device (or on-JVM with a mocked llama.cpp) and assert the same pass/fail behavior as the Python harness.

**Expected impact:** This closes the dual-implementation drift risk I flagged in round one. Until the Android app runs the same pipeline the eval grades, the eval results are about a different system than the one you will ship. This is a prerequisite for any release.

### 5. Add a "do nothing" default answer for all inheritance-mode questions

The spec says "do nothing is a valid, encouraged answer," but the current system does not implement this as a default. T06-013 shows the model actively recommending an action ("power them on to look") when the corpus says not to. The inheritance walkthrough should have a hard-coded default: unless the question is specifically about a later-day action, the answer should begin with "Do nothing yet. Nothing is urgent." and only then address the specific question.

**Concrete implementation:** In the answer prompt, add a rule: "If the question is about any action involving inherited devices, word lists, or computers, and the user has not indicated they are past day one of the walkthrough, begin your answer with: 'Do nothing yet. Nothing is urgent. Take your time.' Then answer the specific question." Enforce this with a must_include constraint on all T06 items.

**Expected impact:** This directly addresses the #1 loss vector (panic actions) and is cheap to implement. It will also improve T06 pass rates, since many T06 items already have "do not X yet" as a must_include.

## 3. SAFETY GAPS

**The app can give confident wrong advice, and nothing catches it.** T06-013 is the proof. The model said "Yes — powering them on just to look is okay" when the corpus says the opposite for day one. The strict grader failed it for phrasing, but a semantic judge might pass it. The guardrails only check for danger phrases ("reset," "wipe," "share your seed") — they do not check for *wrong affirmative advice*. A user who powers on an inherited hardware wallet on day one, before securing the seed phrase backup, is taking a real risk (device could be compromised, PIN could lock, etc.). The current system would not stop this.

**The secret detector has a false-negative risk on pasted text.** The Python `SecretDetector` and the Android `MemoryGuards` both use wordlist matching with fuzz tolerance. But the Android version only flags runs of 8+ consecutive BIP-39 words or 90% ratio. A user who pastes a seed phrase with line breaks, punctuation, or interleaved commentary between words could evade detection. The Python version is more robust (sliding window with fuzz), but the Android port is simplified. This is exactly the kind of divergence I warned about. A user who pastes their seed phrase into the scam checker and the detector misses it has just exposed their keys on a screen — and the app's whole promise is that it never touches secrets.

**The "Pro brain" import path is a supply-chain risk.** The spec says the user imports a 2–3GB model file via SAF file picker and the app verifies the hash. But the app has no network access, so the user must obtain the file from somewhere else — presumably a computer, then transfer via USB. That transfer path is unverified. A malicious actor could distribute a poisoned model file with a matching hash if they also control the distribution channel. The spec says the hash is "published in the signed release notes," but a non-technical inheritor will not verify a SHA-256 by hand. This is a real attack surface that the spec hand-waves.

**The corpus browser exposes technical text without context.** The Android `CorpusBrowserScreen` shows the `technical` field of every entry. For a non-technical inheritor, reading "BIP-39 seed phrases encode the wallet's master private key" without the plain-language framing could lead to confusion or panic. The browser should default to plain text only, with technical text behind an explicit "show technical details" toggle.

**The walkthrough is not actually a walkthrough.** `WalkthroughScreen` renders a static list of days with DO/DON'T bullets. It does not adapt to the user's situation, does not ask what they found, does not branch. The spec says "guided, numbered first 7 days walkthrough," but the implementation is a static document viewer. A grieving user who opens this and sees "Day 1: secure the physical items" but has not found any items yet has no path forward. The walkthrough needs to be interactive: ask what they found, then show the relevant day.

## 4. DESIGN SMOOTHING

**Over-engineered: the semantic rubric judge.** You built a GLM-5.3-based judge to grade failed eval items, but the judge itself is not deterministic and its verdicts are not reproducible. The eval results show "semantic_lenient" passing items that strict grading failed, but there is no evidence the semantic judge is reliable. For a release gate, you need deterministic grading. The semantic judge is useful for development insight, but it should not be part of the pass/fail decision. Replace it with the contradiction-check grader I proposed above, which is deterministic and catches the failure mode that matters.

**Over-engineered: the Sail flex fallback in the answer step.** The Python harness falls back to GLM-5.3 when llama-server is unreachable. This is a dev-time convenience, not a product feature. The shipped app will never have a cloud fallback (no network). Keeping the fallback in the harness means your eval results are a mix of local and cloud model performance, which makes them hard to interpret. Remove the fallback from the eval path; run evals against the local model only, and report cloud results separately as a ceiling estimate.

**Under-engineered: the Android answer path.** As noted above, there is no answer path in the Android app. This is the single biggest gap between the eval harness and the product. The fix is not to port the Python code line-by-line, but to define the answer-step contract (input: question + retrieved chunks + constraints; output: answer + citations + warnings) and implement it natively in Kotlin against the same corpus assets. The Python harness then becomes a reference implementation for testing, not the source of truth.

**Architecturally messy: the dual corpus formats.** The Python harness reads corpus JSON files with a specific schema (title, topic_ids, plain, technical, claims, warnings). The Android app reads the same files but with a different parser (kotlinx-serialization) and a slightly different schema (source_title, source_url, corpus_version added). The two parsers must be kept in sync manually. Define a single JSON schema with a version field, and generate both parsers from it (or at minimum, add a schema validation test that runs against both implementations).

**Simplify now: drop the "Pro brain" for v0.1.** The optional 2–3GB model import path adds supply-chain risk, UI complexity, and testing burden. For v0.1, ship one model (whatever you decide in improvement #3) and defer the Pro brain to v1.0. The spec already says the app degrades gracefully without a model; that is enough for v0.1.

## 5. ROUND-1 RECOMMENDATIONS NOT PROPERLY APPLIED

**"Make the answer step extractive-leaning, not generative" — partially applied, not enforced.** The prompt now says "ASSEMBLE, do not invent" and "quote the corpus claims as close to verbatim as possible," but there is no mechanical enforcement. The model still free-composes, and the eval results show it paraphrases away the target phrases. The fix I proposed in round one was to have the model select spans and stitch them, with citations true by construction. That has not been implemented.

**"Decide the model floor on real hardware" — not done.** The eval results explicitly say "the on-device floor decision belongs on real 8GB target hardware," but no such test has been run. The 4B model was OOM-killed on the dev host, and the conclusion was "defer to real hardware" — but that deferral has no date or owner. This is the single most important unknown in the project, and it remains unknown.

**"Compile the skeleton in CI and wire the vertical slice" — partially done.** The Android build compiles and 13 JVM tests pass, which is real progress. But the vertical slice — user question → grounded answer on-device — does not exist. The ChatPlaceholderScreen is still a stub. The round-one recommendation was to wire ChatPlaceholder → llama.cpp JNI → bundled model → one real on-device answer. That has not happened.

**"Split the grader: strict for DR/PJ/HO, semantic for topical" — done, but the semantic judge is not trustworthy.** The policy gate now uses semantic grading for T01/T02/T08/T10, but the judge is a GLM-5.3 call with no reproducibility and no calibration. The round-one recommendation was "token-overlap now, rubric judge later" — you skipped the token-overlap step and went straight to an unvalidated LLM judge. The semantic pass rates (8/25, 6/30, 7/25, 8/25) are not evidence the system works; they are evidence the judge is lenient.

**"Patch the spec: APK-only corpus updates, directory = roles not names, correct the license claim" — done.** The spec now says directory = roles, the license claim is corrected, and corpus updates ship with app releases. This was applied properly.

**"Create a single corpus status ledger" — not done.** The corpus inventory shows T01: 5/6 reviewed, T07: 12/14 reviewed, T08: 3/5 reviewed, T10: 0/1 reviewed. There is no ledger tracking which entries are approved, which are pending, and which eval items they support. The round-one recommendation was to make every must_include traceable to a specific claim-id. The corpus now contains the phrases, but the traceability is not documented.

## FINAL WORD

The project is pointed the right way. The architecture is sound, the privacy story is real, the corpus is now substantive, and the deterministic core is solid. But the central loop — user asks, grounded answer comes back, answer is safe and correct — does not exist end-to-end, and the eval results show that even the harness version of that loop fails more often than it succeeds. The next three months should be spent on three things: (1) make the answer step mechanically extractive, (2) test the real model on real hardware, (3) wire the Android app to the real pipeline. Everything else is premature.