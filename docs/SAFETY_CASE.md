# Safety Case (skeleton)

Assembles the project's safety argument: threat model → mitigations →
evidence → residual risks. This document is the F-Droid submission centerpiece
at beta; sections marked **[evidence pending]** fill as Addendum 11 (on-device
eval) lands.

## 1. Threat model (who/what can hurt the user)
- T1 Scam/social engineering: "support agent", "giveaway", "verification"
  requests for seed words, keys, or urgent action.
- T2 User error under grief/stress: rushed actions, wrong wallet, wrong network.
- T3 Secrets leaking: seed words/keys typed, photographed, or pasted anywhere.
- T4 Wrong or fabricated guidance: model hallucination, paraphrase drift,
  outdated vendor instructions.
- T5 App compromise/supply chain: tampered APK or model file.

## 2. Mitigations (design, not just code)
- M1 (T1) Deterministic ScamRules verdict — demands-not-mentions regex engine;
  verdict is primary and un-overridable; model only explains in simpler words.
- M2 (T2) Guided First-7-Days walkthrough: fixed, reviewed, deterministic
  checklist; model explains steps, never generates/reorders/completes them;
  "nothing is urgent" pacing; red un-checkable DON'Ts.
- M3 (T3) Secret detection first in every path: seed-phrase, xprv-family, and
  WIF key input triggers an explicit refusal and guidance on EVERY chat path
  (enforced by a single guarded entry point both buttons call); nothing is
  echoed or stored.
- M4 (T4) Grounded answers only: retrieval over a 51-entry corpus, all
  human-reviewed; `key_points`/`dos`/`donts` are verbatim subsets of the approved entry
  text (human-reviewed; sha256 manifest in `prototype/key_points_manifest.json`);
  `user_says` is retrieval-index-only, non-authoritative;
  hybrid mode appends a verbatim safety block; veto layer replaces answers that
  violate safety constraints — shipped in commit 69f23f0 (`util/VetoRules.kt`):
  a global negation-aware veto + scrub applies to every free-form model surface
  (scam-check guidance and the extractive bridge); caller-supplied
  `mustNotInclude` constraints are judged by the same rule. Honest refusals
  (no-answer-beats-wrong-answer); errors are explicit, never silent.
- M5 (T4/T1) Privacy by permission-absence: no INTERNET permission; the app
  physically cannot send anything anywhere; model sideloaded and
  sha256-verified by the user.
- M6 (T5) Signed APK with published sha256; model GGUF hash published.

## 3. Evidence
- Logic tests: 110 Kotlin + 52 Python (incl. corpus vendor-neutrality lint + model-import matrix).
- Hosted-pipeline evals: EVAL_RESULTS.md addenda 1–10 (best: hybrid 31/105
  topical FINAL; T06 19/40 strict) — **hosted, not the shipped model**.
- On-device eval (production JNI path, 145 items + danger/refusal suite):
  **[evidence pending — Addendum 11]**
- Danger/refusal suite results, versioned and release-bound:
  **[evidence pending — Addendum 11]**
- Verification status per claim: README "Verification status" table.

## 4. Residual risks (stated plainly)
- The 1.7B on-device model has not been measured end-to-end; quality may be
  materially lower than hosted numbers.
- Deterministic layers cover known patterns; novel scam phrasing may pass
  rules and only receive grounded-but-generic advice.
- The app advises; it cannot verify the user's specific device, wallet app
  version, or firmware. Users must verify against official documentation
  (permanent in-app banner on every model-touching surface).
- Small-n human review (one curator + AI-assisted drafting with human
  approval of every entry).

## 5. Change control
- Any pipeline change regenerates eval numbers before release claims change.
- Corpus edits require human approval before `reviewed=true`.
- Gate runs follow docs/GATE_CONVENTIONS.md (pinned judge, purity reports).
