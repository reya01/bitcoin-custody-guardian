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

**Current numbers** (dated; see [prototype/EVAL_RESULTS.md](prototype/EVAL_RESULTS.md)
for the full addenda trail):

| Metric | System | Value | As of |
|---|---|---|---|
| Walkthrough strict | hosted pipeline (p2) | 19/40 | 2026-10-09 (Add. 9) |
| Topical FINAL | hosted paraphrase (p2) | 24/105 | 2026-10-09 (Add. 9) |
| Topical FINAL | hosted hybrid — paraphrase + verbatim safety block (p7) | 31/105 | 2026-10-10 (Add. 10) |
| Overall strict+semantic | hosted pipeline (p2) | 43/145 (~30%) | 2026-10-09 (Add. 9) |
| Retrieval coverage | hosted pipeline | 84% (103/122)* | 2026-10-09 (Add. 9) |
| On-device APK eval | **not yet measured** | — | pending (Addendum 11) |

\* Retrieval coverage is an upper bound — eval questions share sources with the
corpus ([GATE_CONVENTIONS.md](docs/GATE_CONVENTIONS.md)). The 122 denominator is
as recorded in Addendum 9's run; re-derivation and per-item breakdown are
scheduled with the on-device eval (Addendum 11).

All runs at temperature 0 with hosted answerer and judge (Sail flex), so expect some
run-to-run movement. **The numbers above describe the hosted pipeline; on-device
(offline 1.7B) results have not yet been measured and will likely be lower.**

- **Working prototype harness** (`prototype/`): corpus retrieval, deterministic
  guardrails, scam-rule engine, answer generation, and a 145-item eval suite
  with strict + semantic grading.
- **Android app builds green** (debug + release) with **on-device llama.cpp
  inference packaged** (arm64): a JNI bridge runs a Qwen3-class model fully
  offline. **26/26** JVM unit tests and **44/44** Python harness tests green,
  covering the safety-critical logic (secret detection, scam rules, refusal
  fallback, post-model veto).
- **Curated corpus** of 50 reviewed entries across 12 topics, drafted from
  designated references (Mastering Bitcoin 3rd ed., Lopp's security index,
  River, BTC Guide, BTC Sessions, Casa, bitcoin.org) with per-source license
  discipline — Mastering Bitcoin is CC-BY-SA 4.0; the per-source status for
  everything else is recorded in
  [docs/CORPUS_SOURCES.md](docs/CORPUS_SOURCES.md).

### Verification status (claimed / implemented / demonstrated)

| Claim | Status |
|---|---|
| APK signed, sha256 published | **Demonstrated** (v0.1.0-alpha, hash on the release page) |
| No INTERNET permission; nothing leaves the phone | **Demonstrated** (manifest + Android OS enforcement) |
| Deterministic safety layer (ScamRules, secret refusal, veto) | **Implemented**; logic unit-tested; end-to-end on-device suite pending |
| Corpus verbatim integrity | **Demonstrated** (schema fields are verbatim subsets of the human-approved entry text, sha256 manifest recorded in `prototype/key_points_manifest.json`) |
| Model GGUF sha256 published | **Demonstrated** (release notes) |
| Reproducible two-machine build | **Claimed, not demonstrated** |
| Production signing key on hardware token | **Claimed** (planned at beta) |
| On-device answer quality | **Not demonstrated** (Addendum 11 pending) |

**Device floor:** the currently tested configuration is a 1.7B Q4_K_M model on
flagship-class Android phones (≥8GB RAM assumed but **not yet measured on
hardware**). The guided checklist, corpus browser, and scam-check verdicts are
model-independent and are expected to work on much weaker phones; chat quality
on 4GB-class devices is unverified. Honest device testing is scheduled (Addendum 11).

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
- [docs/LICENSES.md](docs/LICENSES.md) — license split: code MIT, corpus CC BY-SA 4.0
- [docs/CORPUS_SOURCES.md](docs/CORPUS_SOURCES.md) — knowledge corpus sources & curation plan
- [docs/EVAL_SET.md](docs/EVAL_SET.md) — golden eval set structure & samples (release gate)
- [docs/INHERITANCE_WALKTHROUGH.md](docs/INHERITANCE_WALKTHROUGH.md) — the "first 7 days" guided walkthrough (core UX template)
- [prototype/EVAL_RESULTS.md](prototype/EVAL_RESULTS.md) — all eval addenda, honestly tracked
- [docs/reviews/](docs/reviews/) — external review rounds and the reconciled plan
- [docs/SAFETY_CASE.md](docs/SAFETY_CASE.md) — safety case: threat model, mitigations, evidence, residual risks
- [docs/GATE_CONVENTIONS.md](docs/GATE_CONVENTIONS.md) — evaluation integrity rules for gate runs
- [NOTICES](NOTICES) — third-party code & model notices

## License
MIT — see [LICENSE](LICENSE).
