# Action Response — MiMo-V2.6-Pro External Review

**Verification note before verdicts.** I checked every finding against Addendum 10 and current repo state rather than taking the review on authority. Two things temper it: (a) the reviewer could not read `EVAL_RESULTS.md` or `answer_step.py` (their own Uncertainties section), so several "not demonstrated" findings are really "not visible to me"; (b) the reviewer's own snapshots are stale — they cite 19 JVM tests (current: 22 Kotlin green) and "122 eval questions" (the set is 145 = 105 topical + 40 walkthrough). That cuts both ways and is itself evidence for their I2. Verdicts below.

---

## CRITICAL

### C1 — Evals measure a different system than we ship → **ACCEPT (with one correction)**

**Reasoning.** Confirmed. The 31/105 hybrid FINAL (Addendum 10) was produced by the Python harness with a hosted answerer at temperature 0. The shipped artifact answers via the JNI on-device path with the bundled 1.7B Q4_K_M GGUF. Only JNI smoke tests exist; **no full on-device eval numbers exist** — that is the plain answer to iPhone-plan open question #6, and the plan should say so. Under the honest-published-evals principle, a headline score that characterizes a different artifact than the one in the APK is dishonesty-by-omission, whatever our intent. The reviewer is right that this is the finding that matters.

**Correction (skeptical):** "the ~30% headline is likely an upper bound on what users get" is right for topical quality but wrong as a blanket claim — the hybrid layer's extractive path and the ScamRules veto are model-independent, so the danger-refusal floor may survive the model swap intact. But that is exactly why it must be measured, not assumed (see C2).

**Fix.** Expand the already-pending 8GB-phone floor test into a full on-device eval — this is a superset of scheduled work, not new work. Debug-only eval runner in the Android app (new Kotlin component behind a debug flag) that ingests the 145-set + danger/refusal suite JSON, runs them through the *production* path (router → hybrid → veto → JNI), and exports results (reuse the round-4 SAF plumbing for the export). Score off-device via `answer_step.py` with a pinned judge. Publish as **Addendum 11**, same format, labeled with device/OS/RAM, plus latency/thermal (the original floor-test scope), and bound to the GGUF hash + corpus version + app version.
**Lands:** new debug runner in app; `answer_step.py` scoring mode; `EVAL_RESULTS.md` Addendum 11. **Size:** M. **Bucket:** alpha-now.

### C2 — Safety floor not demonstrated; audience can't evaluate it → **ACCEPT substance; REBUT the rename**

**Reasoning.** The reviewer couldn't read `EVAL_RESULTS.md` (see I1), so their verification failed for access reasons — but Addendum 10 records no danger-suite run against the shipped model either, and the 41 Python + 22 Kotlin tests cover logic, not the end-to-end shipped artifact. So the finding stands: the "100% refusal+warn" figure (PRODUCT_SPEC §14.3) is currently a *spec requirement*, not a demonstrated result. For a grieving non-technical user, the danger suite is the floor claim, and under no-answer-beats-wrong-answer an undemonstrated floor claim is the one thing this project cannot afford.

**Rebut the rename.** "Guardian overpromises" is a fair concern, but the substance is delivered by UI honesty (below), and per the reviewer's own I4, the North-Star user doesn't self-install — the Verifier does. A rename is a branding decision with real costs and no safety delta the UI can't deliver. Revisit at the beta gate with user evidence; not an engineering action now.

**Fix.** (1) Danger/refusal suite runs on-device as part of the C1 run; publish as a standalone, versioned, citable artifact (rows: prompt, verdict, veto-triggered?, raw model output), tied to release. **Lands:** `EVAL_RESULTS.md` new `DANGER_SUITE` section or companion file. **Size:** S (rides the C1 run). **Bucket:** alpha-now. (2) Permanent, unmissable "this app can be wrong — verify before acting" surface: first-run card + persistent banner in guided mode and chat. **Lands:** Compose UI component. **Size:** S/M. **Bucket:** alpha-now. (3) Rename: owner decision point at beta gate, evidence-based.

### C3 — Corpus licensing → **ACCEPT the remedy; REBUT part of the reasoning; correct the record**

**Reasoning.** The reviewer's legal claim is stated more confidently than the law supports. Copyright protects expression, not facts; Bitcoin mechanics are facts stated across many sources, and an original in-house rewrite of facts that *cites* the source is generally not a derivative work. The claim that "pairing a rewrite with the technical original strengthens the derivative-work argument" gets it backwards: shipping the verbatim original, clearly delimited and attributed under its own license, is *compliance* for the quote — the verbatim-only corpus-fields principle exists precisely so quoted material is never disguised. **However:** neither I nor the reviewer is a lawyer, F-Droid will scrutinize this, and the corpus does embed verbatim CC-BY-SA excerpts — so the ambiguity must be resolved structurally, not argued.

**Record correction:** attribution/NOTICES landed in round 4 and `CORPUS_SOURCES.md` exists; the reviewer's "not among the materials" is the I1 path problem, not absent work.

**Fix.** (1) Explicit license split: **code MIT, corpus content CC-BY-SA 4.0 wholesale.** Since the corpus already contains verbatim CC-BY-SA material, share-alike already touches it; licensing the whole corpus CC-BY-SA satisfies the obligation for any entry a court might deem derivative and costs nothing for entries that aren't. This removes the derivative-work question from the distribution path entirely. (2) Per-entry provenance table in `CORPUS_SOURCES.md`: entry id, source, what was taken (verbatim quote vs. facts-only), license, date checked — 50 entries, all already reviewed, tractable. (3) State the split in README/LICENSES.
**Lands:** `CORPUS_SOURCES.md`, README, LICENSES. **Size:** S (split) + M (per-entry table). **Bucket:** alpha-now; hard blocker for F-Droid (beta-gate item).

---

## IMPORTANT

**I1 — Broken evidence links → ACCEPT.** The reviewer's C2 verification failed *because of this* — highest-leverage S fix in the review. Fix README link text/target mismatch for `EVAL_RESULTS.md`, verify `answer_step.py` path, add "all doc links resolve" to the release checklist. **Size:** S. **Bucket:** alpha-now.

**I2 — Number drift → PARTIAL ACCEPT, PARTIAL REBUT.** Rebut: 24/105 vs 31/105 are not drift — 24 is the paraphrase *baseline*, 31 the hybrid *FINAL* (Addendum 10), different systems in the same log; 145 vs "≥200" is current vs. target. Accept: "30+ entries" vs 50 is genuine README staleness, and the reviewer's own stale numbers (19 vs 22 tests; "122 questions") prove the remedy. Fix: dated "current numbers" table (metric, system, value, as-of addendum) at top of README/EVAL_RESULTS. Pre-registration integrity requires unambiguous numbers. **Size:** S. **Bucket:** alpha-now.

**I3 — Judge reliability + contamination → ACCEPT both.** (a) Temp-0 already standard; extend to pinned judge model/version/prompt-hash recorded in every gate-run header, plus human-graded subsample (~30 items) with reported agreement before 50/105 is treated as pass/fail. **Size:** S/M. **Bucket:** beta-gate (record the convention now). (b) Contamination: accept — eval questions drawn from corpus sources inflate the 85% retrieval figure and, worse, cannot test the behavior the project is built on: *refusing what it doesn't know*. Author an out-of-corpus refusal set from non-corpus material (real forum posts, adversarial phrasing). **Size:** M. **Bucket:** beta-gate; a 15–20-item starter rides into the C1 run.

**I4 — Distribution hostile to persona → ACCEPT direction, REBUT urgency.** The alpha's actual users are technical (the reviewer says so themselves); the North-Star user arrives via distribution that doesn't exist yet — **F-Droid is the structural fix** (already planned, blocked on C3), which the review underweights. The Verifier-as-installer pattern is right and cheap: printable one-page verification sheet (install steps, signing-key fingerprint, "the app never moves coins — you can stop here") + in-app "set up for someone else" handoff. **Size:** M. **Bucket:** beta-gate.

**I5 — Device floor → ACCEPT.** The ≥8GB assumption is spec convenience, not audience reality. Fix: (a) README states the *currently tested* floor honestly today, including that checklists/corpus browser are model-independent and expected (not verified) below it. **Size:** S. **Bucket:** alpha-now. (b) 4GB budget-device run (thermal/RAM, deterministic surface with model disabled) after the 8GB test. **Size:** M. **Bucket:** beta-gate. Note: honest degradation only works if S1's emphasis inversion happens — a 4GB phone where chat is the headline is a bad product.

**I6 — Supply-chain claims ahead of evidence → ACCEPT.** The eval log's honesty discipline applied to builds. Fix: "Verification status" block in README with claimed / implemented / demonstrated per item — signing: implemented (APK v0.1.0-alpha signed); hashes: publish if not already true; reproducible two-machine build: claimed, not demonstrated; hardware-token key: claimed. **Size:** S. **Bucket:** alpha-now.

**I7 — Vendor naming → ACCEPT enforcement; policy already exists.** PRODUCT_SPEC §12 already commits to roles + dated examples + re-verification + "no payments/affiliates ever" — the reviewer asks to make it enforceable, which is fair. Fix: corpus lint as a Python harness test — fail on referral-code patterns, URL parameters, undated rankings/superlatives. **Size:** S. **Bucket:** alpha-now. Periodic re-audit cadence + in-app "how this was chosen": deferred.

---

## SUGGESTIONS — dispositions

- **S1 (invert emphasis) — ACCEPT, and it's less new than the reviewer thinks.** Guided mode (First-7-Days) and ScamRules verdicts *already shipped in the alpha*; "currently v1.0 items behind the chat" is stale spec-reading. Real delta: label chat "beta" in-app, make the deterministic surface the default landing, verify the corpus browser is in the APK. **Size:** S/M. **Bucket:** alpha-now (labeling/nav); deeper rework beta-gate.
- **S2 (estate letter) — ACCEPT.** Zero model dependency, serves the North-Star scenario, anchors the Verifier handoff. **Size:** M. **Bucket:** beta-gate (pull from v1.x+).
- **S3 (Safety Case doc) — ACCEPT.** Assembles existing material: threat model → mitigations → evidence (named tests, addendum rows, fixtures) → residual risks. **Size:** M. **Bucket:** beta-gate (skeleton now is fine); becomes the F-Droid centerpiece.
- **S4 (per-step undo/verify) — ACCEPT.** Mostly content authoring. **Size:** M. **Bucket:** beta-gate.
- **S5 (accessibility parity) — ACCEPT.** One explicit paragraph in PRODUCT_SPEC now; TalkBack/font-scaling implementation beta-gate. **Size:** S now / M later.
- **S6 (injection fixtures) — ACCEPT.** Router heuristics currently have no published fixture evidence; same treatment as ScamRules. **Size:** M. **Bucket:** beta-gate.
- **S7 — folded into C1/I5.** No separate action; the 8GB run is the pending test, 4GB is the follow-on.

---

## Reconciliation with existing plans

- **Round-4 plan:** items 4 (SAF install) and 5 (extractive/hybrid) are DONE — hybrid 31/105, extractive 12–13/105, JNI on-device answers (Addendum 10). The C1 fix *builds on* item 5 rather than duplicating it: the hybrid exists and runs on-device; what was never done is measuring it on hardware. The SAF work is reused for eval-result export.
- **iPhone plan beta gate:** the pre-registered gate already exists with locked thresholds. The reviewer's top-action #5 ("pre-register the Android beta gate") is **already satisfied** — the gate is product-wide, not iOS-only. What's missing is the Android on-device baseline row feeding it, which is exactly the C1 run. Do not create a second gate.
- **Pending 8GB floor test:** superseded by expansion into the C1 on-device eval. One device session, one published addendum closes C1, C2's evidence gap, S7, and answers open question #6.
- **Reviewer praised as future work things that already exist:** the pre-registered gate itself; guided mode + ScamRules in the shipped alpha (S1); the "no payments/affiliates ever" policy (I7 — enforcement is the delta, not the policy); attribution/NOTICES + `CORPUS_SOURCES.md` (C3 — an I1 link failure, not absent work); temperature-0 discipline (I3's pinning is an extension).

---

## Next actions (ordered)

1. **Run the on-device eval on the 8GB phone** — expanded floor test: 145-set + danger/refusal suite through the production JNI path via a debug eval runner, SAF-exported, scored with a pinned judge; publish Addendum 11 + standalone danger-suite artifact bound to GGUF/corpus/app version; answer iPhone-plan Q6. *(C1, C2, S7; M; alpha-now.)*
2. **Doc hygiene pass** — fix all evidence links; dated numbers table; verification-status block (claimed/implemented/demonstrated); README device-floor statement. *(I1, I2, I6, I5a; S; alpha-now, parallel with #1.)*
3. **License split + per-entry provenance** — corpus CC-BY-SA 4.0 / code MIT; `CORPUS_SOURCES.md` per-entry table. *(C3; S+M; alpha-now; blocks F-Droid.)*
4. **Alpha UI honesty pass** — chat labeled beta; deterministic surface as default landing; permanent "can be wrong — verify before acting" surface. *(S1, C2b; S/M; alpha-now.)*
5. **Enforcement tests** — corpus vendor lint in the Python harness; record the judge-pinning convention for gate runs. *(I7, I3a; S; alpha-now.)*
6. **Beta-gate batch against the existing pre-registered gate** — out-of-corpus refusal set + human-graded judge agreement; Safety Case doc; Verifier install sheet + handoff; 4GB device run; estate-letter template; injection fixture suite; per-step undo/verify content; accessibility parity. *(I3b, I4, I5b, S2–S6; M each; beta-gate.)*
7. **Owner decision at beta gate** — keep or change "Guardian" naming/framing, decided on user-testing evidence, not now. *(C2c; deferred.)*

The review's core judgment is correct: the next work is measurement, evidence resolution, and putting the safe parts first — and most of that measurement is the floor test we already had scheduled, done properly.