# Reconciled Improvement Plan — Bitcoin Custody Guardian

**Review A = DeepSeek, Review B = Kimi.** Both reviews independently converged on three structural conclusions, which I treat as settled and build the plan around:

1. **Stop paraphrasing; render the approved words.** Retrieval ~85–88% vs. delivery ~30% means the answer is found and then mangled. The model gets demoted to select / frame / simplify.
2. **The guided First-7-Days flow is the product; chat is a feature.** The North-Star user doesn't know what to ask — that's the defining fact of her situation.
3. **4B Q4 as default on 8GB phones.** Both call the 1.7B default a mistake; both gate it on the already-scheduled floor test.

---

## Conflicts resolved

| Conflict | Resolution | Why |
|---|---|---|
| Extraction mechanism: A's template slot-filling vs. B's key-point ID selection | **B's mechanism** | Picking IDs is a ~10-token classification task small models do reliably; verbatim span-copying is exactly the failure being measured. A's own design needs a deterministic verbatim-check fallback; B's makes the fallback unnecessary by construction — the rendered text *is* the approved text. A's framing survives as the optional 1–2 sentence bridge. |
| Sentence windows: A's embedding-indexed sentences vs. B's chunk-returns-enclosing-section | **B's** | Safety text must not be fragmented — the "never" often lives in the next sentence. S vs. M effort. A's embedding/reranker idea is deferred into the hybrid-retrieval item, pending measurement. |
| Model distribution: A's "bundle 4B in APK" vs. B's SAF picker + hash | **B's** | 2.8GB exceeds GitHub's 2GiB asset limit, wrecks the F-Droid reproducible-build story, and re-downloads gigabytes per update. SAF preserves no-INTERNET: the user's *browser* downloads; the app never fetches. |
| Capability path: A's model bake-off vs. B's distillation | **Sequence both** | Bake-off is cheap dev-time work that gates the 4B default; distillation is the bigger, riskier bet that comes after extraction + 4B are measured. |
| Speculative decoding calibration: A (L, 1.5–2×) vs. B (M, 1.3–1.8×) | **B's numbers** | Greedy decoding makes it bit-identical (accuracy-free), but CPU draft-acceptance is the open question. Late-stage either way. |

---

## Tier 1 — Impact for the grieving-beginner inheritor

**1.1 Guided "First 7 Days" home screen (deterministic walkthrough)** — Effort L · Impact High · A(A2, B1, B2), B(B1)
Convert the walkthrough to structured JSON (days, steps, DO/DON'T, why-explanations); render natively in Compose with checkboxes, encrypted progress, one item per screen; "Explain simpler" / "Ask about this" buttons invoke the model with retrieval pre-scoped to that step's corpus slice. The model can explain a step but never generate, reorder, or complete one. Walkthrough accuracy → 100% by construction; per-step scoping also shrinks context (accuracy + latency). This is the product.

**1.2 First-run situation router** — S · High · B(B2)
Four big buttons — *someone died and I found bitcoin things / I got a suspicious message / check my own setup / just learning* — routing to guided mode, scam check, corpus, decoder. Chat becomes the second tab, not the front door. Never show a blank text box to a Day-1 user.

**1.3 One-tap scam check, rules-first** — S · High · A(B3), B(B3)
Persistent red button on every screen: paste message → deterministic rules verdict **instantly** (<1s, no model wait) → model then explains the verdict that already fired, in ≤2 sentences. The moment of maximum danger gets the fastest, most reliable component; the model does only a constrained task it can actually do.

**1.4 Model install via SAF file picker + SHA-256 verify; app useful with no model** — S · High · B(B4)
User downloads the GGUF in Chrome, picks it via Storage Access Framework, app verifies hash against bundled expected values before loading. Startup RAM gate; deterministic-only mode (walkthrough, scam rules, corpus reader, decoder) works with zero model installed. Replaces adb — the target user cannot `adb push`, and this is the adoption killer today.

**1.5 Failure UX: the matched passage always renders** — S · High · A(B4), B(B5)
On veto, refusal, timeout, or engine error: display the matched approved corpus passage plus a plain-language note and next action ("I couldn't compose an answer, but here's the reviewed material your question matched"). Error templates with zero exit codes. No dead ends — the app stays useful even when the model fails completely.

**1.6 Encrypted inventory builder (Day 2)** — M · High · A(B1 partial), B(B6)
Structured form (device / paper / app / account, description, where found); secret detector on **every** field; EncryptedFile/SQLCipher, `allowBackup="false"`, `FLAG_SECURE`, optional BiometricPrompt (her phone may be handled by relatives). Feeds deterministic routing ("you listed 2+ hardware wallets → read the multisig guide") with no model involved. Also fixes the incoherent "E2E session log" language — there's no network; call it local encrypted storage, backups off.

**1.7 Corpus reader + tappable jargon + "explain simpler"** — S-M · Med-High · B(B7, B8)
Browse the human-approved corpus organized by the 7-day structure, no model required. Glossary terms in rendered answers are tappable → decoder sheet; "explain simpler" re-asks with a constrained 3-sentence no-jargon prompt. On a bad day, reading beats chatting.

**1.8 Pacing, tone, accessibility** — S · Med · A(B2 partial), B(B11)
Persistent "Nothing is urgent" banner; end-of-day "you did enough today" states; one difficult thing per day; large default type; full TalkBack labels. Grieving users have reduced cognitive capacity, and many inheritors are 50+. No gamification — both reviews agree.

**1.9 Printable checklists + Day-6 verification plan** — S-M · Med · A(B5), B(B9)
PrintManager → PDF per day. Day 1 is physical (hands full); Day 6's checksum verification happens on an internet-connected computer — a printed plan is the artifact that crosses that air gap. Never print inventory or secret-shaped fields.

**1.10 Trust & honesty surface** — S · Med · B(B10 + core-finding note)
First-run "Why trust this?" page: live permission list (none), model SHA-256, airplane-mode test ritual ("turn it on — everything still works"). The app teaches "everyone offering help is a scammer" and then asks for trust; it must teach verification. Also: scrub/clarify any "cloud fallback" language in the eval docs — state plainly that frontier-model numbers are dev-harness baselines and no cloud path exists in the product.

---

## Tier 2 — Accuracy

**2.1 Corpus-first extractive answers** — M · High · A(A1), B(A1) — *mechanism per B, see conflict table*
Every entry gets a `key_points:` block of atomic approved sentences. Pipeline: retrieve → model outputs **which key-point IDs apply** (~10 tokens, GBNF-constrained to valid IDs) → UI renders verbatim → optional 1–2 sentence bridge. Deterministic citation check (IDs ⊆ retrieved); failure falls back to passage-only rendering (1.5). Eliminates paraphrase drift — the #1 measured failure — by construction.

**2.2 Corpus schema upgrade + expansion** — M · High · B(A5), A(A3)
Add `key_points`, `do/don't`, `risk_class`, and `user_says:` paraphrases ("USB stick labeled ledger," "app says Sparrow is that where the bitcoin is"). Paraphrases double as BM25 index expansion — this subsumes A's query-rewriting item, applied at index time. Grow T02/T08/T10 first (2/30, 5/25, 9/25 — the retrieval misses live here). Feeds 1.1, 2.1, 2.4.

**2.3 GBNF grammar + hard output caps** — M · Med-High · B(A2)
Constrain output to `{framing, cited_chunk_ids, safety_note}`; post-check citations against retrieved set. Kills think-leak, rambling, and uncited claims; shorter outputs are also the cheapest latency win (pairs with 3.2).

**2.4 Deterministic mandatory warnings per risk class** — S · Med-High · B(A9)
Fixed blocks ("never share these words," "do not reset the device") appended keyed off `risk_class`, never model-composed. The highest-stakes sentences must not depend on model output. Nearly free once 2.2 lands.

**2.5 4B default on 8GB + tiering** — S · High · A(A5), B(A3, C5)
Qwen3-4B-Instruct-2507 Q4_K_M where RAM allows — natively non-thinking, so the `/no_think` + think-strip hack gets deleted. Startup RAM gate, 1.7B fallback tier with a visible note, q8_0 KV. **Gated on the already-scheduled floor test — do not displace it.** Cheapest 10–20 points available.

**2.6 Model-family bake-off at 4B** — M (dev-time) · Med · A(C5, C4)
Run the 145-item eval on Qwen3-4B vs. Gemma vs. Phi-4-mini, plus a Q5_K_M variant (A's standalone Q5 item folds in here as a checkbox). The harness exists; this is dev-machine work that informs the 2.5 default.

**2.7 Hybrid retrieval + reranker — measure first** — M · Med · B(A6), A(A3/A4 partial)
Only if 2.2's paraphrase expansion doesn't close the gap: bge-small-class int8 embeddings (~35MB) + RRF fusion + MiniLM cross-encoder over top-10, plus the jargon decoder as a deterministic synonym map. With a 30-entry corpus, lexical + paraphrases may already reach 95% — don't add an embedding model and complexity without a measurement saying so.

**2.8 Distill the frontier teacher into the local model** — L · High (later) · B(A4)
2–5k teacher-generated examples (paraphrased grieving-user questions, key-point selections, refusals, scam explanations conditioned on rule verdicts); LoRA-SFT, merge, GGUF; held-out paraphrase set. A 1.7B trained to be extractive-faithful on this corpus may beat a generic 4B freelancing — but see Disagreements #5 for sequencing cautions.

**2.9 Eval additions** — S · Med · B(A10), A(A2/B4 partial)
Grieving-phrasing slice (teacher-generated, human-reviewed); on-device determinism check (the 4 temp-0 flips must not reproduce on-device); per-intent latency benchmark; guided-flow completion metric; re-scope the walkthrough slice to UI tests once it's deterministic. The gate must measure the product, not a paraphrase penalty.

---

## Tier 3 — Performance (8GB, CPU-only)

**3.1 Prefix KV caching** — M · High · A(C1), B(C1)
Cache the fixed system+safety prefix via `llama_state_save/load`; per-intent few-shot prefixes; precompute guided-step prompt states in the background during onboarding. Prompt eval is the real latency killer (15–40s for a ~1200-token prompt before first token); guided mode is many sequential questions.

**3.2 Streaming + hard output caps** — S · High · B(C2)
Token-by-token display with honest progress states; caps enforced by the 2.3 grammar. A 150-token answer at 10–25 tok/s is 6–15s — fine streamed, unacceptable as a spinner.

**3.3 Build tuning** — S · Med-High · B(C3)
Verify KleidiAI/i8mm kernels for Armv9, threads pinned to big cores (4–6), mmap on. Commonly 1.5–2× over a naive build; nearly free.

**3.4 Context budget** — S · Med · A(C3), B(C6)
Cap retrieved context at ~800–1200 tokens; truncate to fit; log lengths. Shorter prompts = faster TTFT *and* better small-model behavior. Made safe by 2.2/2.7.

**3.5 Speculative decoding (Qwen3-0.6B draft)** — M · Med · A(C2), B(C4)
Greedy decoding → bit-identical outputs (accuracy-free); extractive answers have high draft acceptance; ~0.4GB RAM. Measure acceptance before shipping; expect 1.3–1.8×.

**Non-negotiable deterministic floor (both reviews agree; endorse as-is):** secret detection/refusal, scam verdicts, post-model veto, mandatory warning blocks, checklist content/order/completion, citation validation, hash verification, refusal templates. The model may frame, select, simplify, explain — never compose a safety-critical instruction, override a verdict, or mark a task done.

---

## Where I disagree with the reviews

1. **A's A1 impact math.** A projects extraction taking walkthrough strict from 19/40 → 30–35/40, but A's own A2 makes the walkthrough deterministic (40/40) and removes those items from the model eval. The projection double-counts; A2 supersedes it.
2. **A's "bundle 4B in the APK."** Rejected — GitHub's 2GiB asset limit, F-Droid constraints, update economics. B's SAF solution is strictly better under the no-INTERNET architecture.
3. **B's A8 unsupported-sentence scrubber — defer.** Under key-point rendering there's almost no free-form output left to scrub (only the bridge sentence); it requires bundling an embedding model and a cosine threshold that's a crude instrument for safety decisions. The veto + citation check covers the residual risk. Revisit only if bridges ship and misbehave.
4. **B's fingerprint-comparison ritual — trim.** Permission display and the airplane-mode test are right for this user; asking a grieving novice to compare APK signing fingerprints is expert behavior that adds anxiety. Keep as an optional "for experts" link.
5. **B's distillation timing — caution, not rejection.** It adds a retrain-on-every-corpus-change maintenance tax and risks confidently-wrong behavior off-distribution (refusal routing must be re-validated post-training). Sequence it after 2.1 + 2.5 are measured; the combination may capture most of the gain for far less complexity.
6. **B's hybrid-retrieval priority (Med-High) — demote behind measurement.** With 30 entries, paraphrase-expanded BM25 likely suffices; don't ship a 35MB embedding model + cross-encoder until 2.2's numbers say otherwise.

**Dropped as generic or out of scope:** both reviews' "deliberately not recommended" lists agree and I endorse them (8B models, GPU/NPU offload, self-consistency, voice, any cloud component). A's standalone Q5_K_M item folds into 2.6.

---

## Execution order — next 5 work items

*(The 8GB-phone model-floor test stays scheduled as-is and runs in parallel; it gates 2.5, not anything below.)*

1. **Corpus schema upgrade + walkthrough→structured JSON** (2.2 + the content layer of 1.1). Pure content/schema work, no device dependency, no conflict with the floor test — and it's the substrate for items 1.1, 2.1, and 2.4. Nothing downstream should start before this exists.
2. **Situation router + one-tap scam flow** (1.2 + 1.3, with 1.5's plain-language error templates riding along). Two S items in one release; the instant rules verdict needs no model, so this ships real safety value immediately and restructures the front door.
3. **Guided mode v1** (1.1 + 1.8): Days 1–3, deterministic rendering, encrypted progress, one-item-per-screen pacing, per-step scoped "Ask about this" on the current pipeline. Deterministic content is immune to the 30% problem, and step-scoped context already improves model answers.
4. **SAF install + SHA-256 verify + no-model floor** (1.4), timed to land with the floor-test results. If the test clears 4B, flip the default (2.5) in the same release — this is the item that actually puts the 4B model in users' hands without adb.
5. **Extractive answers v1** (2.1 + 2.3 + the full 1.5 fallback): key-point selection, verbatim rendering, citation check, passage-always failure path. Then re-run the eval with the 2.9 additions to measure the real jump.

*Parallel zero-slot aside:* verify the llama.cpp build flags (3.3) — a one-day check, not a work item, and it makes every subsequent on-device measurement honest.

**The one-sentence version, reconciled:** both reviews are right about the same thing — you already paid the human-review cost for the words, so show the words; make the checklist the product, the model the explainer, and the deterministic layer the floor that works even on the worst day.