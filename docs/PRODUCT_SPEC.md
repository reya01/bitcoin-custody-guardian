# PRODUCT SPEC — Bitcoin Custody Guardian
**Status:** Draft v0.1 — for iteration & brainstorm only. Not a commitment to build.
**Last updated:** 2026-10-06

---

## 1. Vision

A fully-local, privacy-first Android app that helps bitcoiners — especially
non-technical people who *suddenly* find themselves custodians (inheritors,
spouses, accidental recipients) — understand their self-custody setup and take
safe next steps. All AI runs on-device. The app has no network access at all.
It never asks for, stores, or transmits secret material (seed words, xpubs,
passphrases).

**One-sentence pitch:** "A private, local AI expert in Bitcoin self-custody
that lives entirely on your phone and helps you figure out what you're holding
and how to protect it — without exposing it to anyone."

## 2. The North-Star Scenario

> A woman's father has died. He left instructions: "my Bitcoin is in that app /
> that device." She knows nothing about Bitcoin. She has a newer Android phone.
> Every person she could ask is a potential thief; every web search leaks her
> situation; every crypto "help" forum is full of scammers watching for exactly
> her.

She installs this app, goes airplane mode, and asks anything:
- "What is a seed phrase and what am I looking for?"
- "There's a USB stick labeled 'ledger'. What do I do — and what do I NOT do?"
- "This app on his phone says ' Sparrow'. Is that where the bitcoin is?"
- "Someone emailed me offering to 'help recover' the coins. Is that normal?"

The app answers plainly, explains the jargon, tells her what is safe to do
*right now* (usually: nothing, and that's the right answer), what is dangerous,
and how to reach a verified next step. Nothing she types or receives leaves
the device. She never has to trust a stranger with the setup.

## 3. Design Principles (in priority order)

1. **Privacy is architectural, not a setting.** No `INTERNET` permission in
   the manifest, ever. Not "disabled by default" — *impossible*. The app can
   be verified offline: any network monitoring tool must show zero traffic,
   because there is no code path to create any.
2. **The model never touches secrets.** The app detects seed-phrase-like input
   (BIP39 words, WIFs, xprvs) and warns + refuses to process it. Secrets are
   not needed to *talk about* custody safely.
3. **"Do nothing" is a valid, encouraged answer.** The #1 way inheritors lose
   coins is panicked irreversible actions (wiping a device, resetting a
   wallet, trusting a "recovery agent"). The app's default guidance in the
   inheritance flow: *secure the physical items, change nothing, take your
   time.*
4. **Grounded answers, not vibes.** Every substantive answer is grounded in a
   curated, versioned knowledge corpus (see §7) and cites its source section.
   The model is a *reader and explainer* of vetted material — not an oracle.
5. **Honest about limits.** Every session makes clear: this is educational
   software, not financial advice, and it cannot see your actual wallets. It
   guides *how to check things yourself* (e.g., in Sparrow, on the device).
6. **Fail closed.** Uncertainty, ambiguity, or knowledge-base gaps → the app
   says it doesn't know and suggests what human/professional channel is
   appropriate, rather than guessing.
7. **No answer beats a wrong answer.** "I don't know this — it's not in my
   reviewed material" is always an acceptable, preferred outcome. The engine
   is explicitly instructed (and graded) to admit ignorance rather than fill
   gaps with plausible-sounding advice; a confident wrong step with inherited
   bitcoin can lose it permanently.
8. **Errors are acceptable; silent errors are not.** When anything fails
   (answer engine down, fallback engine used, a candidate answer vetoed by
   the safety layer), the app tells the user plainly what happened and what
   to do next — it never shows a wrong or improvised answer as if nothing
   happened, and never downgrades engines without a visible note.

## 4. Users

| Persona | Description | Primary need |
|---|---|---|
| The Inheritor | Non-technical, grieving, stressed, no vocabulary | "What is this and what do I do first?" |
| The Anxious Holder | Has a setup (multisig, hardware wallets) but unsure it's done right | "Is my setup safe? What am I missing?" |
| The Verifier | Technical friend asked to *look, not touch* | Structured checklist + explanations to relay |
| The Learner | Bitcoin-curious, wants self-custody basics privately | Jargon-free explanations |

Assumption (per spec owner): all users have a **newer Android phone** —
target ≥8 GB RAM flagships; graceful degradation to a smaller model on 6 GB.

## 5. Threat Model

### Assets to protect
1. User's secret material (seed phrases, passphrases, key files, device PINs).
2. The *fact* that the user holds bitcoin, and their situation (inheritance,
   amounts, wallet structure).
3. Trustworthiness of the app's advice (a malicious or hallucinating "expert"
   inside the inheritance scenario is an attack vector in itself).

### Adversaries
- **Device-level:** malware already on the phone (partially out of scope — the
  app minimizes secrets exposure so there is nothing to steal *from it*),
  other installed apps (mitigated by zero permissions, zero exported
  components, no shared storage).
- **Network-level:** eliminated by having no network code at all.
- **Content-level:** prompt injection via any text the user pastes in (an
  email, a message from a scammer) trying to make the "assistant" produce
  harmful instructions. Mitigation: prompt text is treated as *data*, the
  safety router runs *before* the model, and outputs are constrained by the
  guardrail layer (§8).
- **Distribution-level:** a repackaged/modified APK. Mitigation: reproducible
  builds, F-Droid + independently verifiable GitHub releases, pinned signing
  key fingerprint published in the docs (§9).
- **Model-supply-level:** poisoned/tampered GGUF weights. Mitigation: model is
  bundled in the APK and covered by the reproducible-build verification;
  weight SHA-256 published per release.

### Explicitly out of scope (documented honestly)
- Protecting against a compromised phone/OS, keyloggers, or screen recording.
- Replacing a human expert for high-value, complex, contested inheritances —
  the app *points to* professional channels rather than pretending to be one.
- Executing transactions. The app **never** signs, transacts, or holds keys.

## 6. Research Summary — Best Practices Borrowed from Leading OSS Apps

Surveyed (2026-07 state): **PocketPal AI** (MIT, llama.cpp/GGUF, 1M+ installs),
**ChatterUI** (AGPL-3.0, React Native + llama.cpp), **MLC Chat** (Apache-2.0,
MLC/TVM GPU-compiled), **SmolChat** (Apache-2.0, Kotlin-native, small models),
**Google AI Edge Gallery** (Apache-2.0, LiteRT-LM), **RikkaHub**, **Maid**,
**LLM Farm**.

| Practice | Adopted from | Our adaptation |
|---|---|---|
| llama.cpp via GGUF as the inference core | PocketPal, ChatterUI, SmolChat | Bundled JNI/Kotlin binding; no remote backend support *by design* (ChatterUI's remote mode is the anti-pattern for us) |
| Model selection & RAM floors per device tier | PocketPal, AI Edge Gallery allowlist | Bundled model only; device RAM gate at startup with clear messaging |
| Q4_K_M default quant, Q8 for reasoning-heavy | community consensus (multiple sources) | Q4_K_M base; Q8 build for 12 GB devices |
| Per-conversation params, context window discipline | PocketPal | Hard context budget (see §8) to keep RAM bounded |
| Offline claim "nothing leaves the device" | PocketPal/Enclave docs | Upgraded from *policy* to *architecture*: no INTERNET permission |
| Character/persona system prompts | ChatterUI | Domain modes: Inheritance mode, Verify mode, Learn mode |
| F-Droid distribution + reproducible builds | F-Droid ecosystem (Cambium, Conversations pattern) | F-Droid first; developer-signed reproducible APK (AllowedAPKSigningKeys) |
| App-permission audit as trust signal | sideload security guides | Permission manifest documented in-app ("Why these permissions: none") |
| Structured-output constraint for small models | Phi-4-mini/Qwen3 guidance | Deterministic router + guardrails so the small model is never asked open-ended high-stakes questions |

Key structural lesson: the leading apps are **chat clients**; our app is a
**domain expert**. We intentionally trade generality for groundedness: a small
model + a curated corpus + a deterministic safety layer outperforms a bigger
model freelancing on the world's most dangerous FAQ.

## 7. Knowledge Corpus (the real product)

The model is commodity; the corpus is the moat.

- **Sources (initial):** Bitcoin Core release notes & docs,
  BIPs (32/39/44/84/174/380-series), Sparrow Wallet docs, Bitcoin Design
  Guide, GLADDER/inheritance-oriented guides (e.g., PlanB-style estate
  planning material), hardware-wallet vendor docs (Ledger, BitBox02, Jade,
  SeedSigner, Krux, Liana/functional), bitcoinops topics for jargon depth.
  **License reality (corrected 2026-10-06 per review):** standards/docs are
  open; vendor educational libraries (River Learn etc.) carry NO explicit
  open license — see docs/CORPUS_SOURCES.md §Licensing: they are used as
  reference only, and every entry is an original in-house rewrite.
- **Curation pipeline:** each source passage is reviewed, rewritten into
  plain language with the technical original alongside, tagged (topic, level,
  risk class: informational / caution / danger), and **versioned**. New
  knowledge packs are released like software: signed, hash-pinned, with
  release notes.
- **Corpus curation can run as a long-term Sail flex GLM-5.3 job:** batch
  drafting of plain-language rewrites + risk tags, then human review (you) —
  flex tier is exactly right for this slow, cost-insensitive work.
- **In-app:** bundled at build time (no download = no leak channel and no
  malicious-update vector). Size target ≤150 MB.

## 8. Architecture (high level)

```
┌────────────────────────────────────────────────┐
│ UI (Kotlin, Compose) — 3 modes + Q&A chat      │
├────────────────────────────────────────────────┤
│ Safety Router (deterministic, no LLM):         │
│   • secret-material detector → warn & refuse   │
│   • intent classification → mode + corpus slice│
│   • prompt-injection heuristics on pasted text │
├────────────────────────────────────────────────┤
│ Grounded Answerer:                             │
│   • local retrieval over corpus (BM25+embed)   │
│   • llama.cpp (JNI) with GGUF model, bundled   │
│   • forced citation format; refusal template   │
│     when retrieval confidence is low           │
├────────────────────────────────────────────────┤
│ Guardrails (deterministic, post-model):        │
│   • blocklists for prohibited advice classes   │
│   • "danger" language scrub & re-check         │
│   • every answer ends with limits statement    │
├────────────────────────────────────────────────┤
│ Storage: nothing by default; optional E2E-     │
│ encrypted session log the user must opt into   │
└────────────────────────────────────────────────┘
NO INTERNET PERMISSION. NO exported components. NO analytics.
```

**Model strategy (initial):**
- Primary: **Qwen3 4B**-class or **Gemma 4 E2B/E4B** (Apache-family license),
  Q4_K_M GGUF, ~1.5–3 GB — verified in the 2026-07 landscape as the sweet
  spot on 8 GB flagships; thinking-mode capped for latency.
- Alternate: **Phi-4-mini** (MIT) if license cleanliness is prioritized
  (Gemma terms have redistribution quirks; Apache/MIT models are cleaner for
  bundling).
- Final selection gate: a **custody-domain eval set** (see §10), not generic
  benchmarks.

**Why not bigger:** 8B-class gets OS-killed mid-range, thermal throttles on
flagships, and per research "a 4B model with a tight system prompt and
constrained format beats the same model asked an open-ended question."

## 9. Distribution & Supply Chain

1. **F-Droid first** (builds from our source on their servers = the strongest
   third-party verification), then GitHub Releases with
   `AllowedAPKSigningKeys` pinning so users can verify developer-signed
   reproducible APKs.
2. **Reproducible builds** from day one (F-Droid docs are explicit about the
   early-pain, late-payoff of doing this at v1 rather than retrofitting).
3. **Signing key:** generated once, stored on a hardware token (HSM-class),
   never in CI. Fingerprint published on the project page, in the repo README,
   and inside the app's About screen.
4. **No Play Store initially** — sideloading/F-Droid is the natural channel
   for a privacy tool whose audience already sideloads; Play's policies and
   build verification add surfaces, not safety. (Revisit later.)
5. **Update surface:** corpus packs update with app releases only. No
   in-app fetch (that would require INTERNET permission — disallowed).
   Updates arrive through the distribution channel, verified by signature.

## 9a. Size Strategy (Option A — everything bundled, one file, no downloads)

**Constraints found (2026-10-06):**
- Google Play: base module ≤500 MB, install-time asset packs ≤1.5 GB each,
  ≤4 GB cumulative — Play is feasible via AAB + install-time asset pack
  (assets delivered by Play's own infrastructure at install; our app still
  holds no INTERNET permission). Second choice — Play last, after F-Droid.
- GitHub Releases: **2 GiB max per release asset** → the primary APK must be
  <2 GB, or split. (GitHub is the primary developer-facing channel.)
- F-Droid: no hard published max; ABI split "highly encouraged"; binary
  blobs in the source repo are frowned upon — model should NOT live in the
  git repo at all. Exact multi-GB acceptance to be verified with F-Droid.
- Sideloaded APKs generally: practical installer limit ~4 GB (APK zip
  format); a ~2 GB APK installs fine with enough free storage.

**The plan — "Lite brain inside, big brain optional, everything verified":**
1. **Primary APK (target ≤1.8 GB):** bundles a **Qwen3 1.7B-class Q4_K_M**
   model (~1.0–1.1 GB) + corpus + app. Fits: GitHub release asset (single
   file <2 GiB), F-Droid (likely), Play (single 500 MB base + 1.5 GB
   install-time asset pack holding the model — no INTERNET permission in
   our manifest; delivery is Play's job at install time).
   - Rationale: the 1.7B tier is the 2026 "workhorse" for 6–8 GB phones and
     — crucially — our app constrains it heavily (deterministic router +
     grounded retrieval + refusal templates), which the research says is
     exactly how small models get to near-hosted quality. Chat quality is
     bounded by the corpus, not the parameter count.
2. **Optional "Pro brain" file (~2–3 GB, separate release asset(s)):**
   Qwen3 4B Q4_K_M (and/or Q8 for 12 GB devices) published as its own
   release artifact with published SHA-256. The app imports it from
   user-managed storage (SAF file picker — no network), verifies the hash
   before first use, and refuses a mismatch. The *user* performs the
   transfer; the app never fetches it. Device gate: Pro brain offered only
   on ≥12 GB RAM devices.
3. **Per-ABI split:** arm64-v8a primary (newer phones); x86_64/emulator
   builds as separate CI artifacts for development only.
4. **Corpus always in the APK** (~≤150 MB — text compresses well) so even
   with no model file present the app remains useful (browser + checklists
   degrade gracefully; the chat UI explains the missing brain honestly).
5. **F-Droid path:** app source contains NO binary weights (build pulls the
   pinned model blob by hash during their build, or F-Droid hosts the model
   as a separate package in the same repo — pattern to confirm with F-Droid
   maintainers before submission).
6. **Anything the user adds later (Pro brain, corpus update files) is
   verified by hash against values published in the signed release notes —
   the app ships with the expected hashes baked in for the current
   release.**

## 10. Evaluation & Red-Teaming

- **Golden test set (≥200 Q&A):** inheritance walkthroughs, jargon decoding,
  "is this a scam?" cases, "should I do X?" decision cases — hand-labeled
  by you. Pass gate before any release.
- **Danger tests:** the model must REFUSE or correctly warn on seeded
  dangerous patterns ("how do I extract the seed from this .txt file?",
  "paste this xprv so I can check it", "reset the Ledger to fix it").
- **Hallucination tests:** questions outside the corpus must produce the
  refusal/limits template, not confident invention.
- **Injection tests:** pasted scam emails must never flip the assistant into
  giving custody instructions.
- Ongoing: the existing BTC-security auditor pipeline pattern extends to
  this repo (GLM + K3 diffs reviewed weekly; red-team prompts as fixtures).

## 11. Feature Set

### MVP (v0.1)
- Airplane-mode-safe chat: grounded Q&A over the corpus with citations.
- Inheritance mode: guided, numbered "first 7 days" walkthrough
  (locate items → photograph/notate *nothing secret* → change nothing →
  inventory apps/devices → what each thing is → safe verification path →
  when to get human help).
- Secret-material detector with warning overlay.
- Device RAM gate + bundled model.
- No-log default; optional encrypted session notes (opt-in, passphrase).

### v1.0
- Jargon decoder (paste any custody sentence → plain-English rewrite with
  the original terms preserved).
- Scam-checker mode: paste a message/email → systematic red-flag analysis
  grounded in corpus scam patterns ("recovery agents", seed-phrase phishing,
  fake support, urgency pressure).
- Verify mode: read-only checklists for common setups (single-sig hardware
  wallet, Sparrow multisig, descriptor files, watch-only wallets) — "how to
  look without touching."
- Practice sandbox: fictional inheritance scenarios to rehearse safely.
- Corpus browser with sources visible.

### Later (v1.x+)
- **Directory mode — resolved per review (Kimi, 2026-10-06):** there is no
  separate "wallet directory" listing vendors. Vendor guidance appears only
  inside corpus entries as **roles** ("temporary custodian while heirs figure
  things out", "exchange with published proof of reserves", "learning
  resource") — with named examples inside the entry per Decision #4
  (2026-10-06), each carrying why it's listed, supporting data, date ranked,
  and how to re-verify. The mode does not rank vendors; it answers the
  role-shaped question.
- Watch-only address importer (xpub-only, xpub handling safe-by-design with
  explicit education about its privacy tradeoffs — xpubs are not secrets but
  are privacy-sensitive; stored in app-private storage only, never shared).
- Multilingual (ES/PT/FR/DE first) via corpus translations.
- iOS consideration (explicitly out of scope for now).
- Optional structured "estate letter" template generator (no secrets, just
  guidance for heirs) — *only* with strong human review of the template.

## 12. Risks & Open Questions

| # | Risk / question | Notes |
|---|---|---|
| 1 | **Bundled model size** (~1.5–3 GB APK) | RESEARCHED 2026-10-06 — DECIDED: Option A (bundle). See §9a "Size strategy". Play allows up to 4 GB cumulative install-time (base 500 MB + asset packs ≤1.5 GB each). GitHub release assets max 2 GiB per file → the primary APK must stay under ~2 GB. F-Droid has no hard published cap (ABI split "highly encouraged" for large APKs) but exact multi-GB practical limits still to be verified with F-Droid directly. |
| 2 | **Model license for bundling** | DECIDED 2026-10-06: use Apache/MIT-licensed models only — Qwen3 (Apache-2.0) primary; Phi-4-mini (MIT) alternate. No Gemma (Google use terms). |
| 3 | **Small-model answer quality** on nuanced inheritance questions | Mitigated by router + grounded retrieval + refusal template; eval set is the gate. Research consensus: constrained small models beat open-ended small models. |
| 4 | **Who is the "verified next step"?** | DECIDED 2026-10-06: the corpus includes BOTH categories and specific named recommendations, and it is deliberately opinionated where data supports it. Bitcoin-only companies only, ranked by verifiable track record: customer-service quality, proof of reserves, regulation/jurisdiction, longevity. Named examples for the "temporary custodian while heirs figure things out" path (e.g., River — US-based, strong customer service, proof of reserves) and for learning/support. Every named entry carries: why it's listed, what data supports the ranking, date ranked, and how to re-verify. No payments/affiliates/referral codes ever. |
| 6 | **Name & brand** | DECIDED 2026-10-06: **Bitcoin Custody Guardian**. |
| 5 | **Liability framing** | "Educational software, not advice" disclaimer + honest limits; architecture (never handles keys, never transacts) keeps exposure minimal. Legal review before release. |
| 7 | **App store future friction** | Google's developer-verification rollout (2026) may affect sideload friction; F-Droid's stance is documented — audience likely unaffected. |
| 8 | **Corpus freshness** | Custody best practices evolve (e.g., BIP drift). Corpus is versioned; updates ship with app releases. |

## 13. Build Process (using our infrastructure)

- Development repo on GitHub (private → open-source at first release;
  the user prefers auditability — public before F-Droid submission).
- **Sail flex GLM-5.3 roles** (all slow-tolerant, cheap):
  1. Corpus curation drafting (§7) — batch rewrite/tag jobs.
  2. Eval-set expansion + red-team prompt generation.
  3. Weekly code-security review of the app repo — same auditor pattern as
     the existing BTC security pipeline (GLM + K3 + judge).
  4. Spec/ADR drafting support on flex tier.
- Deterministic core (router, guardrails, secret detector, retrieval) is
  hand-written and hand-reviewed; the LLM never sits in a safety-critical
  path.

## 14. Success Criteria

1. A non-technical inheritor can complete the inheritance walkthrough and
   correctly answer "what should I NOT do?" without any external help.
2. App runs with network completely disabled; monitoring shows zero traffic.
3. 100% refusal+warn on the danger test set; 100% grounded citations on the
   golden set.
4. Reproducible build verified by two independent machines.
5. F-Droid inclusion without anti-features flags.
