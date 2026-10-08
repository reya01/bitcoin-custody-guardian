# Critical Review & Buildable Improvements for Bitcoin Custody Guardian

## Where the Current Design Falls Short

**The fundamental mismatch:** The docs promise a "first 7 days" guided experience, but the shipped product is a single Ask screen with a chat router. The walkthrough exists as a document, not as an app flow. A grieving beginner doesn't need a chatbot—they need a checklist that holds their hand through Day 1, then Day 2, with the model explaining *why* each step matters, not generating the steps themselves.

**The accuracy problem is misdiagnosed.** The eval shows retrieval coverage at 85-88% but answer quality at 30%. The README blames "model capability," but the real issue is architectural: you're asking a 1.7B model to *compose* answers from retrieved chunks. Small models are bad at composition but decent at extraction. The fix isn't waiting for better models—it's changing what the model does.

**The UX assumes a user who can formulate questions.** The target user doesn't know what a "seed phrase" is, let alone what to ask about it. The current design requires them to type questions into a chat box. That's backwards for the Day 1-3 user who needs to be *told* what to do, not asked what they want to know.

---

## A. Accuracy Improvements

### A1. Extractive Answer Mode (Build: M, Impact: HIGH)

**What:** Replace generative answers with template-guided extraction. For each corpus entry, pre-write answer templates with slots. The model's job becomes: (1) select the right template, (2) fill slots with retrieved spans, (3) output the assembled answer. No free-form composition.

**Why it helps:** The 1.7B model can copy text it's shown; it can't reliably rephrase it. Current eval shows strict-match scores at 19/40 while retrieval is at 88%—the model is *seeing* the right text but mangling it in output. Extraction eliminates the paraphrase problem entirely.

**Build:** 
- Extend corpus schema: each entry gets `answer_template` with `{slot}` markers
- Retrieval returns entry + template + candidate spans
- Prompt becomes: "Fill these slots with exact text from the provided spans"
- Post-process: verify slots are filled with verbatim corpus text (deterministic check)

**Expected impact:** Walkthrough strict scores from 19/40 → 30-35/40. Topical by-meaning from 24/105 → 40-50/105. This is the single highest-leverage change.

### A2. Deterministic Walkthrough Delivery (Build: S, Impact: HIGH)

**What:** The Day 1-7 walkthrough content should not go through the model at all. Ship it as structured data (JSON) rendered directly in the UI. The model only answers *questions about* the walkthrough, never generates the walkthrough itself.

**Why it helps:** The walkthrough is the highest-stakes content. It's already written, reviewed, and versioned. Running it through a 1.7B model adds error with zero benefit. The eval's T06 slice (19/40 strict) is testing something that shouldn't be model-generated in the first place.

**Build:**
- Convert INHERITANCE_WALKTHROUGH.md to structured JSON (days, steps, DO/DON'T lists, explanations)
- UI renders walkthrough natively with progress tracking
- Model is only invoked for "explain this step in simpler terms" or "what does this word mean"
- Remove walkthrough items from model eval; add UI-rendering tests instead

**Expected impact:** Walkthrough accuracy goes from 50% to 100% (it's deterministic). Frees model capacity for genuine Q&A.

### A3. Query Rewriting Before Retrieval (Build: S, Impact: MED)

**What:** Add a deterministic query-rewriting step before BM25 retrieval. Map user questions to canonical topic phrases using the existing keyword tables. E.g., "my dad left me a ledger thing" → "hardware wallet inheritance first steps".

**Why it helps:** The eval shows retrieval misses are "BM25 ranking/topic-routing" problems. A grieving beginner won't use corpus vocabulary. The keyword tables already exist (mentioned in Addendum 7); they just need to be applied to query rewriting, not just topic classification.

**Build:**
- Expand keyword tables with common non-technical phrasings ("dad's bitcoin", "left me crypto", "found a usb stick with words")
- Prepend rewritten query to retrieval call
- Log original vs rewritten query for eval

**Expected impact:** Retrieval coverage from 88% → 92-95%. Modest but real.

### A4. Sentence-Window Retrieval with Reranking (Build: M, Impact: MED)

**What:** Instead of retrieving whole entries, retrieve sentence-level windows (3-5 sentences around key phrases). Rerank candidates with a lightweight cross-encoder or heuristic scorer (keyword overlap, position in entry, risk-class match).

**Why it helps:** The current retrieval surfaces whole entries, which may be 500+ words. The model gets lost in long contexts. Sentence windows give it exactly what it needs to fill template slots.

**Build:**
- Pre-compute sentence embeddings for corpus (offline, bundled)
- At query time: retrieve top-20 sentences, rerank to top-5
- Feed only those 5 sentences to the model (not the whole entry)

**Expected impact:** Reduces context length, improves slot-filling accuracy. Combined with A1, could push topical scores to 50+.

### A5. Model Upgrade Path: 4B Q4_K_M as Default (Build: S, Impact: HIGH)

**What:** Ship the 4B model as the default for 8GB devices, with 1.7B as fallback for 6GB. The README says "4B Q4 fits in RAM on 8GB phones (est ~2.8GB weights)"—that's the target device. Stop optimizing for the 1.7B tier.

**Why it helps:** The eval gap between 1.7B (~30%) and GLM-5.3 (~60-65%) is model capability. A 4B model with the same scaffolding should land at 40-50%. The current design is leaving 10-20 points on the table by defaulting to 1.7B.

**Build:**
- Bundle 4B Q4_K_M as primary model (~2.8GB APK)
- Keep 1.7B as separate "lite" APK for 6GB devices
- RAM gate at startup: if <7GB available, suggest lite APK

**Expected impact:** Immediate 10-20 point accuracy gain on all slices. This is the cheapest high-impact change available.

---

## B. UX Improvements for the Bereaved Beginner

### B1. Guided Mode as Default (Build: L, Impact: HIGH)

**What:** Replace the single Ask screen with a structured "First 7 Days" flow as the app's home screen. Day 1 is a checklist: "Collect these items into one box" with checkboxes. Each item has an "Explain why" button that invokes the model for plain-language explanation. Day 2 is an inventory form (paper-first, with fields for "what it looks like", "where found", "what's written on it"). The model helps classify items ("sounds like a Ledger Nano S") without ever seeing secrets.

**Why it helps:** The target user doesn't know what to ask. The walkthrough document already exists and is excellent—it just needs to be an interactive flow, not a PDF. Grieving users need structure, not open-ended chat.

**Build:**
- Convert walkthrough to Compose screens with progress state
- Each day: checklist items, DO/DON'T cards, "explain this" buttons
- Model invoked only for explanations, never for generating the steps
- Progress saved locally (encrypted, opt-in)
- Printable checklist export (PDF or plain text)

**Expected impact:** This is the product. The current chat interface serves the "Anxious Holder" and "Learner" personas but fails the primary "Inheritor" persona. Guided mode makes the app actually useful for its stated target user.

### B2. One Question at a Time (Build: S, Impact: MED)

**What:** In guided mode, never show more than one question or decision at a time. Day 1 has 6 DO items and 6 DON'T items—show them one at a time with a "Done" or "Next" button. The current walkthrough document is dense; the app should be sparse.

**Why it helps:** Grieving users have reduced cognitive capacity. A wall of text (even well-written) is overwhelming. One item per screen with a clear action is manageable.

**Build:**
- Paginate all checklist content
- Progress indicator ("Day 1: 3 of 6 items checked")
- No back button anxiety—explicit "I'm not sure, explain more" option

**Expected impact:** Higher completion rates, fewer abandoned sessions. Hard to measure in eval but critical for real users.

### B3. Scam-Check as One-Tap from Every Screen (Build: S, Impact: HIGH)

**What:** The scam-defense card is already written. Make it a persistent bottom-sheet or floating action button reachable from every screen in one tap. Paste any message, get a deterministic scam verdict in <1 second (no model needed for the rules engine).

**Why it helps:** The scam rules are deterministic and already built. The target user will be contacted by scammers—probably before they finish Day 1. One-tap access to "is this a scam?" is the highest-value safety feature.

**Build:**
- Floating action button on all screens
- Opens scam-check screen with paste box
- Deterministic rules engine runs first (instant verdict)
- Model explains the verdict in plain language (optional, async)

**Expected impact:** This is the feature that prevents catastrophic loss. It should be more prominent than the chat interface.

### B4. Plain-Language Error Messages (Build: S, Impact: MED)

**What:** The spec says "errors are acceptable; silent errors are not" but doesn't specify what error messages look like. For the target user, "Model inference failed with exit code 137" is useless. Error messages should be: "The app's brain hit a problem. Your information is safe. Here's what to do: [restart the app / try again / read the guide instead]."

**Why it helps:** The target user is already stressed. A cryptic error message adds anxiety and may cause them to abandon the app or, worse, try something unsafe outside the app.

**Build:**
- Error message template system with plain-language strings
- Every error path gets a user-facing message + suggested next action
- Test error paths with non-technical users

**Expected impact:** Reduces abandonment, maintains trust. The eval already tests error communication (2 new tests in Addendum 7); extend this to UI-level testing.

### B5. Printable Checklists (Build: S, Impact: MED)

**What:** Every day's checklist can be exported as a plain-text or PDF file for printing. The user can work from paper, not the phone, when handling physical items.

**Why it helps:** Day 1 involves collecting physical items. The user's hands are full. A printed checklist is more practical than a phone screen. Also, paper doesn't run out of battery.

**Build:**
- "Print" button on each day's screen
- Generates plain-text or PDF via Android's print framework
- No secrets in the printout (inventory fields are generic)

**Expected impact:** Practical usability improvement. Low effort, real benefit.

---

## C. Performance Improvements (8GB RAM, CPU-only)

### C1. KV Cache Reuse for Repeated Prompts (Build: M, Impact: MED)

**What:** The system prompt, safety rules, and corpus context are identical across many queries. Cache the KV states for these fixed prefixes and reuse them across requests. Only the user's question and retrieved chunks change.

**Why it helps:** On CPU-only inference, prompt processing is the bottleneck. Reusing KV cache for the fixed prefix can cut latency by 30-50% for repeated queries in the same session.

**Build:**
- Identify fixed prompt components (system prompt, safety rules, mode instructions)
- Pre-compute KV cache for these at session start
- On each query, only process the new tokens

**Expected impact:** Faster responses, less battery drain. Especially important for guided mode where the user asks many questions in sequence.

### C2. Speculative Decoding with a Tiny Draft Model (Build: L, Impact: MED)

**What:** Run a 0.5B-class draft model alongside the 4B main model. The draft model proposes tokens; the main model verifies them in parallel. On CPU, this can give 1.5-2x speedup if the draft model is accurate enough.

**Why it helps:** The current 10-25 tok/s for 4B Q4 is usable but slow. Speculative decoding could push it to 20-40 tok/s, making the app feel responsive.

**Build:**
- Bundle a 0.5B Q4 model (~300MB additional)
- Implement speculative decoding in the JNI bridge
- Fall back to non-speculative if draft accuracy is low

**Expected impact:** 1.5-2x speedup. Significant engineering effort. Worth it only after A1-A5 are done.

### C3. Context Window Discipline (Build: S, Impact: MED)

**What:** Hard-cap context at 2048 tokens for 1.7B, 4096 for 4B. The current spec mentions "hard context budget" but doesn't specify numbers. With sentence-window retrieval (A4), you don't need long contexts.

**Why it helps:** Smaller context = faster inference, less RAM, less chance of the model getting lost. The eval shows the model fails when given too much context; less is more.

**Build:**
- Enforce context limits in the JNI bridge
- Truncate retrieved chunks to fit
- Log context length for eval

**Expected impact:** Faster responses, better accuracy (less context = less confusion for small models).

### C4. Quantization Tuning: Q4_K_M vs Q5_K_M (Build: S, Impact: LOW)

**What:** Test Q5_K_M for the 4B model. It's ~15% larger but often noticeably better on reasoning tasks. On 8GB devices, the extra ~400MB is affordable.

**Why it helps:** The eval gap between 1.7B and 4B is partly quantization loss. Q5_K_M recovers some of that loss at modest RAM cost.

**Build:**
- Quantize 4B model to Q5_K_M
- Benchmark on-device: speed, RAM, accuracy
- Ship Q5_K_M if it fits in 8GB budget

**Expected impact:** Small accuracy gain (2-5 points), small speed penalty. Worth testing but not a priority.

### C5. Model Choice: Qwen3 vs Gemma vs Phi (Build: M, Impact: MED)

**What:** The spec mentions Qwen3 4B, Gemma 4 E2B/E4B, and Phi-4-mini as candidates. Run the custody-domain eval against all three at 4B scale and pick the winner. The current eval only tests Qwen3 1.7B.

**Why it helps:** Different model families have different strengths. Phi-4-mini is known for instruction-following; Gemma for safety; Qwen for general reasoning. The custody domain may favor one over the others.

**Build:**
- Download and quantize all three 4B-class models
- Run the full 145-item eval against each
- Pick the winner for the primary APK

**Expected impact:** Could be 5-10 points of accuracy. The eval infrastructure already exists; this is a matter of running it.

---

## D. Safety: What Must Stay Deterministic

### D1. Secret Detection (Already Built, Keep as Is)

The BIP39 word detection, WIF detection, and xprv detection are deterministic and must stay that way. No model involvement, ever. This is the highest-priority safety feature.

### D2. Scam Rules (Already Built, Keep as Is)

The scam rules engine is deterministic and must stay that way. The model can explain *why* something is a scam, but the verdict comes from rules.

### D3. Post-Model Veto (Already Built, Keep as Is)

The dangerous-advice veto is deterministic and must stay that way. The model can propose an answer, but the veto layer has final say.

### D4. Walkthrough Steps (New: Make Deterministic)

As proposed in A2, the Day 1-7 walkthrough steps should be deterministic data, not model output. The model explains; it doesn't generate.

### D5. Refusal Routing (Already Built, Keep as Is)

When retrieval confidence is low, the app refuses. This is deterministic and must stay that way. The model doesn't get to override a refusal.

---

## Prioritized Top-10 List

1. **A2: Deterministic walkthrough delivery** (S, HIGH) — Ship Day 1-7 as structured data, not model output; walkthrough accuracy goes to 100%.
2. **A1: Extractive answer mode** (M, HIGH) — Templates + slot-filling instead of free-form generation; biggest accuracy gain for Q&A.
3. **A5: 4B model as default** (S, HIGH) — Stop optimizing for 1.7B; 4B Q4_K_M on 8GB devices is the target.
4. **B1: Guided mode as default** (L, HIGH) — Replace chat with structured Day 1-7 flow; this is the actual product for the target user.
5. **B3: One-tap scam check** (S, HIGH) — Persistent access to deterministic scam verdicts from every screen.
6. **A3: Query rewriting** (S, MED) — Map non-technical phrasings to corpus vocabulary; improves retrieval for beginners.
7. **A4: Sentence-window retrieval** (M, MED) — Give the model exactly what it needs, not whole entries.
8. **C1: KV cache reuse** (M, MED) — 30-50% latency reduction for repeated prompts in guided mode.
9. **B4: Plain-language error messages** (S, MED) — Every error path gets a user-facing message with next steps.
10. **C5: Model family comparison** (M, MED) — Test Qwen3 vs Gemma vs Phi at 4B scale; pick the custody-domain winner.

---

## Critical Assessment of Current Design

**The good:** The corpus is excellent. The safety architecture (deterministic rules + model + veto) is sound. The eval infrastructure is honest and rigorous. The no-INTERNET-permission approach is the right call.

**The bad:** The product is a chat app when it should be a guided checklist. The model is asked to do too much (generate answers) when it should do less (fill templates, explain steps). The 1.7B default is a mistake for the target 8GB device.

**The ugly:** The eval shows 30% accuracy, which the README honestly reports. But the fix isn't "wait for better models"—it's "change what the model does." The current architecture wastes the model's limited capability on composition when it should be doing extraction and explanation.

**The bottom line:** This app can be genuinely useful today for the bereaved-beginner inheritor, but only if it stops trying to be a chatbot and starts being a guided checklist with a helpful explainer attached. The walkthrough document is the product; the model is a feature.