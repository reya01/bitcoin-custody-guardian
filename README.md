# Bitcoin Custody Guardian

A fully-local, no-network Android app: private on-device AI that helps bitcoiners
(especially inheritors) understand and protect their self-custody setup.

- **Privacy is architectural:** the app never holds the `INTERNET` permission.
  Without that permission the OS will not let it open a network socket — there
  is no network code path at all. Everything — retrieval, safety rules, the
  language model — runs on the phone.
- **The model never touches secrets:** seed phrases / xprvs pasted into the
  app are detected, warned about, and refused. The app never stores or
  transmits key material.
- **Grounded answers:** answers are drawn from a curated, versioned knowledge
  corpus. When retrieval can't ground a question, the app is designed to
  refuse rather than guess.
- **No answer beats a wrong answer:** "I don't know — it's not in my reviewed
  material" is always preferred over a confident guess. A wrong step with
  inherited bitcoin can lose it permanently.
- **Errors are acceptable; silent errors are not:** when anything fails or
  falls back, the app says so plainly and tells the user what to do next.
- **Educational only.** This app is an educational aid, not financial, legal,
  tax, or estate-planning advice. No warranty; you alone are responsible for
  custody decisions. For inheritance matters, consult a qualified professional.

> **Status: experimental alpha — not yet safe to rely on.** The app is real,
> installable, and answers on-device (v0.1.0-alpha, see Releases), but it
> should be treated as risky to use with actual funds until the model-quality
> floor (see *Long-term vision*) is met.

## Where the project stands

- **Working prototype harness** (`prototype/`): corpus retrieval, deterministic
  guardrails, scam-rule engine, answer generation, and a 145-item eval suite
  with strict + semantic grading. Current results (all runs at temperature 0;
  answerer and judge are hosted models, so expect some run-to-run movement):
  walkthrough slice **19/40 strict**; topical slices **24/105 (~23%)** on the
  by-meaning gate; **overall 43/145 (~30%)**. Honest and improving —
  **retrieval coverage is ~85% (103/122)**, so the remaining gap is answer
  quality, not knowledge availability.
- **Android app builds green** (debug + release) with **on-device llama.cpp
  inference packaged** (arm64): a JNI bridge runs a Qwen3-class model fully
  offline. **19/19** JVM unit tests and 41/41 Python harness tests green,
  covering the safety-critical logic (secret detection, scam rules, refusal
  fallback, post-model veto).
- **Curated corpus** of 30+ reviewed entries across 12 topics, drafted from
  designated references (Mastering Bitcoin 3rd ed., Lopp's security index,
  River, BTC Guide, BTC Sessions, Casa, bitcoin.org) with per-source license
  discipline — Mastering Bitcoin is CC-BY-SA 4.0; the per-source status for
  everything else is recorded in
  [docs/CORPUS_SOURCES.md](docs/CORPUS_SOURCES.md).

## Long-term vision

**Android first, iPhone later.** The app ships as an Android APK — the
easiest open, auditable distribution channel — and is intended to eventually
be available on iPhone once a viable local-model path exists there.

**It stays alpha until local AI is strong enough.** The whole product hangs on
one constraint: privacy and security requirements mean everything must run
locally with no internet access, ever. Today's small models that fit on a
phone are simply not good enough at careful, multi-step safety reasoning for
this task — so the project stays in an experimental, risky-to-use state until
more powerful local models exist on capable phones. The corpus, safety rules,
and eval gates built now are exactly what a future stronger model plugs into.

**Deterministic parts are deliberate corners, cut for safety.** Some
functionality (scam verdicts, secret-input refusal, dangerous-advice veto,
refusal routing) is handled by fixed rules rather than the AI. This is not the
ideal end state — it is the correct trade for today: a small local model can
be talked into softening a scam warning, but cannot be talked into changing a
rule lookup. When local models are strong enough, these rules stay as a
hard floor, and the AI takes over more of the judgment on top.

## What stronger future local models will fix

These are the current deficiencies that are model-bound, not design-bound:

1. **Paraphrase delivery.** The corpus usually contains the right guidance,
   but the small model rewords it loosely. A stronger model turns ~85%
   retrieval coverage into correspondingly high answer quality; today the
   strict-match scores sit far below retrieval coverage for exactly this
   reason.
2. **Verbatim walkthrough precision.** Step-by-step inheritance walkthroughs
   need exact, ordered instructions ("verify the checksum, then enter the
   words once, into the verified software"). Small models compress and drop
   ordering; stronger models can hold the full sequence.
3. **Scenario-specific scam explanations.** The rules engine already names
   the scam and blocks the ask; the model's job of explaining *why this
   exact message is a scam, in the user's own words* needs more capability.
4. **Honest uncertainty at scale.** Knowing *when it doesn't know* is the
   hardest skill; small models fill gaps with plausible text. Bigger local
   models calibrate refusal far better — and the graded "contradiction"
   judge exists precisely to catch the cases where they don't.
5. **Jargon decoding from real-world artifacts.** Explaining an xpub, a
   descriptor, or a fee-market error message in plain language requires
   reasoning over unfamiliar text the corpus didn't anticipate — beyond a
   1.7B-class model today, plausible for 4B–8B class on modern hardware.

The on-device model floor (what fits in RAM, and how well it performs) is
scheduled for measurement on a real 8 GB phone; the dev box cannot run the
4B-class model yet.

## Documents
- [docs/PRODUCT_SPEC.md](docs/PRODUCT_SPEC.md) — product specification (v0.1)
- [docs/CORPUS_SOURCES.md](docs/CORPUS_SOURCES.md) — knowledge corpus sources & curation plan
- [docs/EVAL_SET.md](docs/EVAL_SET.md) — golden eval set structure & samples (release gate)
- [docs/INHERITANCE_WALKTHROUGH.md](docs/INHERITANCE_WALKTHROUGH.md) — the "first 7 days" guided walkthrough (core UX template)
- [docs/EVAL_RESULTS.md](prototype/EVAL_RESULTS.md) — all eval addenda, honestly tracked
- [docs/reviews/](docs/reviews/) — external review rounds and the reconciled plan
- [NOTICES](NOTICES) — third-party code & model notices

## License
MIT — see [LICENSE](LICENSE).
