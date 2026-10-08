# Review: closing the gap between a chat box and a bereaved beginner's first 7 days

## The core finding

Your own eval data contradicts your strategy. The README says the product "stays alpha until local AI is strong enough" — but the numbers say retrieval coverage is **~88%** while answer delivery is **~30%**, and a frontier model on the *same retrieved chunks* only reaches 60–65%. Two conclusions follow:

1. **The answer already exists inside the app** in human-approved form. The failure is that you ask a 1.7B model to *re-express* it. Stop asking. Render the approved text; demote the model to selector, framer, and simplifier.
2. **The product hierarchy is inverted.** Today the weakest component (free chat) is the home screen, while the strongest components (curated corpus, deterministic rules, the walkthrough) sit behind it. The North-Star user doesn't know what to ask — that's the defining feature of her situation. Chat should be the second tab, not the front door.

Also flagged: EVAL_RESULTS Addendum 6 mentions a "cloud fallback" delivering the GLM number. If that phrase describes any product path, it violates Principle 1 and must be scrubbed; if it's dev-harness-only, say so explicitly, because it reads like a roadmap item.

---

## Gaps between the docs and the user

1. **Walkthrough is a document, not a product.** The walkthrough's own design notes say "each Day maps to one guided mode with an explicit completion state." Shipped UX: a single Ask screen. This is the largest doc-vs-product gap.
2. **Model distribution is an adoption-killer.** The target user cannot `adb push` a GGUF. Meanwhile §5 says the model "is bundled in the APK," §9a says bundle 1.7B, and the fact sheet says sideload — three contradictory stories, none shippable for a grieving novice.
3. **Model strategy is unresolved.** Spec calls 4B the "sweet spot," size strategy bundles 1.7B, and "graceful degradation to 6GB" is promised with no two-model plan.
4. **Eval gate is misaligned with the product.** Strict substring grading penalizes exactly the paraphrase the product needs; there's no latency/on-device eval, no guided-flow completion eval, and temp-0 runs flip 4 items between identical runs — harness nondeterminism you must pin down before trusting any gate.
5. **Corpus is too thin.** 30 entries / 12 topics ≈ 2.5 per topic; T02 sits at 2/30. Entries lack the phrasings users actually type.
6. **No persistence story.** A 7-day journey needs progress, inventory, and notes to survive app restarts. "Optional E2E-encrypted session log" is incoherent for an app with no network — E2E to *whom*? You need local encrypted storage with backups disabled, stated plainly.
7. **No trust-building UX.** You tell the user everyone offering help is a scammer — then ask her to trust an app she sideloaded. There's no in-app verification ritual (permissions display, hash check, airplane-mode test).
8. **Scam-check latency is backwards.** The moment of maximum danger gets routed through the slowest component. Deterministic rules should answer *instantly*; the model explains afterward.
9. **Persona dilution.** Four personas in the spec; one user in the North-Star. Build for the Inheritor; let the others inherit her flow.

---

## (A) Answer accuracy

**A1 — Corpus-first extractive answers; model selects and frames, never paraphrases. (M · High)**
Restructure every corpus entry with a `key_points:` block of atomic, approved sentences. The pipeline becomes: retrieve entry → model outputs *which key-point IDs apply* (a classification task small models are good at, ~10 output tokens) → UI renders those sentences verbatim → model optionally writes a 1–2 sentence bridge ("You found a device labeled Ledger — here's the reviewed guidance"). Paraphrase drift — your #1 measured failure — is eliminated by construction, and walkthrough strict scores rise toward retrieval coverage because `must_include` phrases are present verbatim.

**A2 — Citation-gated generation with a GBNF grammar. (M · Med-High)**
Constrain output via llama.cpp grammar to `{framing, cited_chunk_ids, safety_note}`. Deterministic post-check: cited IDs ⊆ retrieved IDs, else strip and fall back to A1's passage-only rendering. Kills think-leak, rambling, and uncited claims; shortens outputs (also a perf win).

**A3 — Default to Qwen3-4B-Instruct-2507 Q4_K_M on 8GB; 1.7B as the 6GB tier. (S · High)**
~2.8GB weights + ~300MB KV (q8_0) fits. The 2507 Instruct variant is natively non-thinking, so you delete the `/no_think` + think-strip hack. Gate on the 145-item suite; expect a meaningful jump over 1.7B's 30%.

**A4 — Distill a frontier teacher into the small model at dev time. (L · High)**
You already run a frontier model as judge — use it as teacher. Generate 2–5k examples: paraphrased grieving-user questions per entry, ideal key-point selections, citation-formatted answers, refusals for out-of-corpus asks, scam explanations conditioned on rule verdicts. LoRA-SFT the 1.7B/4B, merge, convert to GGUF. A 1.7B trained to be extractive-faithful on *this* corpus can beat a generic 4B freelancing. Hold out a paraphrase set to catch overfitting.

**A5 — Corpus schema upgrade + expansion. (M · High, ongoing)**
Add to every entry: `key_points`, `do/don't` lists, `risk_class`, and `user_says:` paraphrases ("USB stick labeled ledger," "app says Sparrow is that where the bitcoin is"). The paraphrases feed BM25 directly; the structure feeds A1/A2. Grow T02/T08/T10 coverage first — those slices are at 2/30, 5/25, 9/25.

**A6 — Hybrid retrieval + cross-encoder reranker + jargon query expansion. (M · Med-High)**
BM25 + a small int8 embedding model (bge-small class, ~35MB) with RRF fusion; then a MiniLM-class cross-encoder over the top 10 (<1s on CPU). Reuse the jargon decoder as a deterministic synonym/artifact map ("Ledger" → "hardware wallet"). Pushes 88% coverage toward 95%+ and lets you send *fewer, better* chunks — which is also your biggest prompt-latency lever.

**A7 — Sentence-window retrieval. (S · Med)**
Index small chunks, return the enclosing section. Small models handle complete self-contained passages far better than fragments.

**A8 — Unsupported-sentence scrubber. (M · Med)**
Post-model, embed each generated sentence with the bundled embedding model; drop sentences with low cosine overlap to any retrieved chunk, and say so ("I removed a sentence I couldn't ground"). A deterministic-ish hallucination net that complements the veto.

**A9 — Deterministic mandatory warnings per risk class. (S · Med)**
The walkthrough already demands "do not reset the device" / "never share words" appear near related topics. Append these as fixed blocks keyed off corpus `risk_class` — never model-composed.

**A10 — Eval additions. (S · Med)**
A grieving-phrasing slice (teacher-generated, human-reviewed); an on-device determinism check (same input twice → identical output; your hosted temp-0 flips must not reproduce on-device); a per-intent latency benchmark in dev builds. The scheduled 8GB hardware floor measurement gates A3 — run it first.

---

## (B) UX for the bereaved beginner

**B1 — Guided "First 7 Days" checklist as the home screen. (L · High)**
Day cards → 3–7 tasks each, checkboxes, "why this matters" expanders, progress persisted (encrypted). The state machine is pure Kotlin: the model can *explain* a step but can never mark it done, reorder it, or skip it. Day 6's verification sequence renders as a fixed ordered checklist with exact corpus wording. Each step gets an "Ask about this" button with retrieval pre-scoped to that step's corpus slice — which also makes the model more accurate (tiny context, known topic).

**B2 — First-run situation router. (S · High)**
Never show a blank text box to a first-run user. Four big buttons: *Someone died and I found bitcoin things / I got a suspicious message / I want to check my own setup / Just learning.* Routes to the right mode.

**B3 — One-tap scam flow, rules-first. (S · High)**
Persistent red button on every screen: "Someone contacted me about the bitcoin." Paste the message → deterministic rules verdict **instantly** (no model wait) → then the model explains *the verdict that already fired* in two sentences — a constrained task it can actually do. This directly serves the README's "scenario-specific scam explanations" goal by making the task easier instead of waiting for a bigger model.

**B4 — Model install via file picker + in-app hash verification. (S · High)**
The *app* has no INTERNET permission, but the *user's browser* does. She downloads the GGUF in Chrome, picks it via the Storage Access Framework file picker, and the app verifies SHA-256 against a bundled expected hash before loading. This replaces adb with two taps and preserves the supply-chain story. Bundle 1.7B in the APK only if the 2GiB GitHub limit allows; otherwise SAF-only.

**B5 — Failure UX: the passage always shows. (S · High)**
On veto, refusal, timeout, or engine error: display the matched approved corpus passage with a plain note ("I couldn't compose an answer, but here's the reviewed material your question matched"). No dead ends, ever. This makes Principle 8 tangible and means the app is useful even when the model fails completely.

**B6 — Structured inventory builder. (M · High)**
Day 2 operationalized: item type (device / paper / app / account), description, where found. The BIP39/secret detector runs on *every* text field. Stored encrypted (SQLCipher/EncryptedFile), `android:allowBackup="false"`, `FLAG_SECURE` on inventory screens, optional BiometricPrompt lock — her phone may be handled by family. Bonus: the inventory feeds deterministic routing ("you listed 2+ hardware wallets → read the multisig guide") with no model involved.

**B7 — Corpus library browser. (S · High)**
The corpus is human-approved — let her just *read it*, organized by the 7-day structure, no model required. On a bad day, reading beats chatting.

**B8 — Tappable jargon + "explain simpler." (S-M · Med)**
Glossary terms in every rendered answer are underlined and open the decoder sheet; an "Explain simpler" button re-asks with a constrained 3-sentence no-jargon prompt.

**B9 — Printable checklists and the Day-6 verification plan. (M · Med)**
Android PrintManager → PDF. The Day-6 checksum-verification sequence happens on a *computer with internet* — exactly where the app can't go. A printed, step-by-step plan is the correct artifact to hand across that air gap. Never print the inventory or anything secret-shaped.

**B10 — Trust screen + airplane-mode ritual. (S · Med)**
First run: "This app can never use the internet. Verify it: turn on airplane mode — everything still works." A "Why trust this?" page shows the live permission list (none), the model's SHA-256, and the APK signing fingerprint for comparison against the published value. You are asking a scam-wary person to trust software; teach her to verify it.

**B11 — Pacing and tone. (S · Med)**
Persistent Rule #1 banner ("Nothing is urgent"), end-of-day "you did enough today" states, one-difficult-thing-per-day enforcement in the checklist. No streaks, no gamification — the doc is right; keep it. Default to large type and full TalkBack labels: many inheritors are 50+.

---

## (C) Performance in the 8GB / CPU-only budget

**C1 — Prefix KV caching everywhere. (M · High)**
Prompt eval is your real latency killer: a 1,200-token system+few-shot+context prompt at mobile prompt-eval speeds costs 15–40s *before* the first answer token. Cache the fixed system prompt and per-intent few-shot prefixes to disk (`llama_state_save/load`); per turn, only retrieved chunks + question get evaluated. In guided mode, go further: each checklist step's context is known in advance — precompute its prompt state in the background during onboarding.

**C2 — Streaming + hard output caps via the A2 grammar. (S · High)**
At 10–25 tok/s, a 150-token answer is 6–15s — fine with token-by-token display and an honest progress state; an 800-token ramble is not. The schema enforces brevity.

**C3 — Tuned arm64 build. (S · Med-High)**
Verify the llama.cpp build uses KleidiAI/i8mm kernels for Armv9, threads pinned to big cores only (4–6), mmap on. A correctly tuned build is commonly 1.5–2× a naive one. This is nearly free performance you're likely leaving on the table.

**C4 — Speculative decoding with a Qwen3-0.6B draft. (M · Med)**
llama.cpp supports draft-model speculative decoding; with greedy decoding outputs are bit-identical, so it's accuracy-free. Extractive/template answers have high draft-acceptance. ~0.4GB RAM cost; measure before shipping — expect 1.3–1.8× if acceptance is good.

**C5 — RAM discipline. (S · Med)**
`largeHeap`, startup memory gate, q8_0 KV cache, 4B→1.7B fallback with a visible note (Principle 8), embeddings precomputed at build time and shipped as vectors.

**C6 — Context budget. (S · Med-High)**
Cap retrieved context at ~800–1,200 tokens. The A6 reranker is what makes this safe. Shorter prompts = faster TTFT *and* better small-model behavior.

---

## What must stay deterministic (non-negotiable floor)

Secret detection & refusal · scam verdicts · post-model veto · mandatory warning blocks · checklist content, ordering, and completion · citation validation · model/APK hash verification · refusal templates. The model may frame, simplify, select, and explain — it may never compose a safety-critical instruction freehand, override a verdict, or mark a task done.

## Deliberately not recommended

8B models (RAM/thermal, your own spec says why) · GPU/NPU offload (driver variance, audit surface, marginal gains) · multi-pass self-consistency (latency doubles for unreliable gains at this scale) · on-device voice (scope creep) · any cloud component, including "fallback" (violates the architecture that is your entire reason to exist) · in-app corpus updates (ship with signed releases, as specced).

---

## Top 10, prioritized

1. **Corpus-first extractive answers** — model selects approved key-point IDs, UI renders them verbatim; kills the 88%→30% paraphrase gap. (M · High)
2. **Guided First-7-Days checklist as home screen** — deterministic state machine, per-step scoped Q&A; the product the docs already promise. (L · High)
3. **SAF file-picker model install + SHA-256 verify; 4B Q4 default on 8GB** — replaces adb, resolves the 1.7B/4B contradiction. (S · High)
4. **First-run situation router** — four buttons, not a blank chat box. (S · High)
5. **One-tap scam flow, rules verdict instantly, model explains after** — fastest answer at the moment of maximum danger. (S · High)
6. **Failure UX: matched approved passage always renders** on veto/refusal/error — no dead ends. (S · High)
7. **Corpus schema upgrade** (`key_points`, `do/don't`, `risk_class`, `user_says` paraphrases) + T02/T08/T10 expansion. (M · High)
8. **Encrypted inventory builder** with secret-detector on every field, backups off, biometric lock. (M · High)
9. **Latency pack: prefix KV caching + streaming + GBNF caps + KleidiAI/i8mm build tuning.** (M · High)
10. **Distill the frontier teacher into the local model** on corpus Q/A, refusals, and citation format. (L · High)

*Honorable mentions:* hybrid retrieval + reranker (M · Med-High), trust screen + airplane-mode ritual (S · Med), printable Day-6 verification plan (M · Med), speculative decoding (M · Med).

**The one-sentence version:** stop waiting for a model good enough to paraphrase your corpus — you already paid the human-review cost for the words, so *show the words*, and let the model do only what small models do well: route, select, simplify, and explain.