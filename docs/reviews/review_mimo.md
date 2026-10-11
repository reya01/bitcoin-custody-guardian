# External Review — Bitcoin Custody Guardian (v0.1.0-alpha)

**Overall assessment.** This is one of the most intellectually honest open-source safety projects I have reviewed. The core engineering judgments are correct and unusually well-reasoned: privacy as architecture (no `INTERNET` permission) rather than policy; deterministic rules as a hard floor beneath a weak model; "no answer beats a wrong answer"; an eval log that publishes ~30% scores and labels its own alpha "risky to use with actual funds." The iPhone plan's pre-registered beta gate, golden-vector divergence control, and refusal to port the Python harness are all the right calls. The threat model is real work, not decoration.

The problem is a gap between what the *documents* honestly say and what the *shipped artifact* honestly measures. The published eval numbers come from a hosted-model harness, while the product ships a 1.7B on-device model the project itself describes as "simply not good enough" — so the evals, as honest as they are, characterize a different system than the one a grieving user installs. Meanwhile the deterministic safety surface (corpus browser, checklists, ScamRules) that *is* reliable and model-independent is treated as secondary to the chat. The highest-value correction is to invert that emphasis and close the measurement gap.

---

## CRITICAL

**C1. The evals measure a different system than the one you ship.**
README: "all runs at temperature 0; answerer and judge are hosted models." The bundled artifact is a Qwen3 1.7B-class Q4_K_M model, and PRODUCT_SPEC §9a concedes the 1.7B tier is the workhorse "chat quality is bounded by the corpus" — while the README's own vision section says today's phone-sized models "are simply not good enough." So the ~30% headline is likely an *upper bound* on what users get, and the floor measurement on a real 8 GB phone is "scheduled," not done. The iPhone plan even lists as open question #6: "How exactly were the Android on-device eval numbers produced?" — the plan author doesn't know whether on-device numbers exist. For a project whose central virtue is honesty, an eval log that characterizes the harness while the shipped model goes unmeasured is the one dishonesty-by-omission that matters. *Fix:* run the full 145-set (plus the danger/refusal suite) against the actual bundled GGUF on device, publish as an EVAL_RESULTS addendum in the same format. This is also the single biggest de-risking action for the entire "wait for stronger local models" roadmap — you cannot know how far away the gate is until you measure the floor.

**C2. The audience cannot evaluate the safety floor, and the safety floor is not demonstrated in any artifact I could read.**
The North-Star persona is a grieving non-technical inheritor; the product name ("Guardian") reads as a promise. The success criteria require "100% refusal+warn on the danger test set" (PRODUCT_SPEC §14.3), and 41 harness tests + 19 JVM tests cover safety-critical *logic* — but I could not verify (see Uncertainties) any published evidence that the end-to-end pipeline, *with the shipped model*, refuses seeded dangerous patterns at 100%, nor the veto-layer fixture results referenced in the iOS gate. For this audience, the danger suite is not one metric among many — it is the product's floor claim. *Fix:* publish the danger-suite results as a standalone, citable artifact (rows, prompts, verdicts), versioned per release, and give the app a permanent, unmissable "this app can be wrong — verify before acting" surface plus a name/framing that signals *guide*, not *protector*.

**C3. Corpus licensing may be unsound, and it's blocking.**
README states the corpus is "drafted from designated references (Mastering Bitcoin 3rd ed....)" with "per-source license discipline — Mastering Bitcoin is CC-BY-SA 4.0." CC-BY-SA is share-alike: if corpus entries are *adaptations* of MB 3rd ed. content rather than independent works citing it, the derivative entries must themselves be CC-BY-SA — not MIT — and the obligations travel to distribution (F-Droid will check; so will anyone auditing). PRODUCT_SPEC §7 says entries are "original in-house rewrites" with "the technical original alongside" — pairing a rewrite *with* the original text strengthens the derivative-work argument, not weakens it. Per-entry license metadata and a clear split (corpus content license vs. code license) is needed before F-Droid submission. The CORPUS_SOURCES.md file that supposedly records this was not among the materials I could read.

---

## IMPORTANT

**I1. Evidence documents don't resolve.** README links `[docs/EVAL_RESULTS.md](prototype/EVAL_RESULTS.md)` (link text and target disagree) and the file was not readable at either path in this review; `answer_step.py` likewise. A project whose trust model rests on auditable evidence needs its evidence links to work. Hygiene, but trust-relevant hygiene.

**I2. Numbers drift across documents.** Corpus: "30+ entries" (README) vs. "50 entries" frozen at gate (iPhone plan §0D) vs. PRODUCT_SPEC §7's pack language. Eval set: 145 items (README) vs. "≥200 Q&A" (§10) vs. 105+40 breakdown. Topical scores: "24/105" (README) vs. "31/105" (iPhone plan §0B baseline) vs. "50/105" target. If these are snapshots over time, say so with dates; if not, reconcile. Pre-registration only works if the numbers are unambiguous.

**I3. Eval methodology: judge reliability and contamination risk.** The semantic judge is a hosted model with "run-to-run movement" — fine for development, not for a release gate. Freeze judge model/version/prompt per gate run; report agreement with a human-graded subsample before treating 50/105 as a threshold. Separately: the 122 eval questions were presumably drawn from the same reference set as the corpus, which inflates the 85% retrieval-coverage figure. Real questions from grieving users will hit gaps the eval can't model. Add an out-of-corpus refusal set generated from *non*-corpus material (real forum posts, adversarial phrasing).

**I4. The distribution path is hostile to the stated persona — and the plan knows it.** Sideload an ~1.8 GB APK from GitHub Releases, enable unknown sources, verify a signing-key fingerprint: this is beyond the North-Star user. The iPhone plan frets over Safari-import friction while Android sideload friction is comparable or worse. The realistic install path runs through the **Verifier** persona ("technical friend asked to look, not touch") — who is currently a secondary persona. Make the Verifier the explicit installer/onboarder: a "how to install this safely, from a friend" flow, a printable one-page verification sheet, and an in-app handoff that lets the Verifier configure the app *for* the inheritor. This is a product-direction correction, not a feature.

**I5. Device-floor assumption contradicts audience reality.** The spec assumes "newer Android phone, ≥8 GB RAM flagships, graceful degradation to 6 GB." An inheritor uses *whatever phone the household has* — often a 4 GB budget device. Either extend the floor work to include a realistic low-end test and degrade honestly (checklist + corpus browser still work — see S1), or state the exclusion plainly in the README where the persona story is told.

**I6. Claims ahead of evidence on supply chain.** Reproducible builds ("verified by two independent machines"), hardware-token signing key, published APK hashes — these are all success criteria and threat-model mitigations, but nothing in the readable materials shows current status. The alpha README should carry a short "verification status" line distinguishing *claimed*, *implemented*, and *demonstrated*, exactly as rigorously as the eval log does for model quality.

**I7. Vendor naming inside the corpus is a live trust surface.** PRODUCT_SPEC §12 risk #4 resolves to roles + named examples + dates + re-verification instructions and "no payments/affiliates ever" — good. Keep it enforceable: a hard corpus rule (no entry may contain a referral code, URL parameter, or un-dated ranking), an in-app "how this was chosen" affordance, and periodic re-audit. Grieving users are the population most exploitable by an endorsement that has drifted since its ranking date.

---

## SUGGESTIONS

**S1. Invert the product emphasis: ship the deterministic surface first.** The corpus browser, the First-7-Days checklist, ScamRules verdicts, and checklists are model-independent, safe, and immediately valuable. They are currently v1.0 items *behind* the chat. A user with a failing model should still get full value from the walkthrough and the browser — PRODUCT_SPEC §9a.4 gestures at this ("degrade gracefully") but it deserves to be the alpha's headline, not its fallback. Pull the corpus browser and checklist forward; make the chat visibly "beta" inside the app.

**S2. Pull the estate-letter template earlier.** Listed under "Later (v1.x+)," it is one of the few features that is deterministic, safe, and directly serves the North-Star scenario ("what do I leave my heir?"). It has zero model dependency. It is also the natural anchor for the Verifier handoff (I4).

**S3. Write a Safety Case document.** Threat model → mitigations → evidence (named tests, eval rows, fixture files) → residual risks, in that order. Right now the safety argument is dispersed across four documents. A single Safety Case is what lets the Verifier persona — or an external auditor, or a future you — actually verify the floor. It also becomes the F-Droid/trust listing's centerpiece.

**S4. Per-walkthrough-step undo/verify affordances.** "Do nothing" is the right default, but if a user *does* take step 5 wrongly, what's the recovery? Each walkthrough step should carry "how to check this is still safe" and "how to reverse this if unsure."

**S5. Accessibility parity.** The iPhone plan makes Dynamic Type/VoiceOver "first-class"; Android (Compose, grieving/older users) should state the same explicitly in PRODUCT_SPEC.

**S6. Prompt-injection heuristics need their own fixture suite.** The threat model names content-level injection (§5) and the router claims heuristics, but I saw no fixture set or results for it. Same treatment as ScamRules: seeded adversarial pastes, published verdicts.

**S7. On-device floor measurement should include thermal and RAM on a 4 GB device** (see I5), not just the 8 GB flagship — and it is arguably the most schedule-relevant experiment in the whole roadmap (it tells you how far the "wait for stronger models" gate actually is).

---

## Uncertainty — flagged honestly

- **`EVAL_RESULTS.md` and `answer_step.py` were not readable** in this review (the former not at the path the README's link implies, the latter not at the provided path). My assessment of eval honesty rests on README summaries and the iPhone plan's gate numbers; the "honestly tracked addenda" claim could not be independently verified.
- **Whether on-device eval numbers exist is genuinely ambiguous** from the materials (C1) — the iPhone plan's open question #6 suggests even internally this is unclear.
- **Corpus entry quality, per-entry license metadata, and actual rewrite-vs-derivative status** could not be examined; C3 is raised on documentary grounds.
- **CI/reproducible-build/signing-key current state** (I6) is unverifiable from docs alone.
- **Number drift (I2) may be my reading of snapshots at different dates** rather than real inconsistency.
- I have not run the app or the harness; all behavioral claims here are documentary.

---

## Top 5 actions, prioritized

1. **Run and publish the on-device eval**: full 145-set + danger/refusal suite against the *bundled* 1.7B GGUF, in the EVAL_RESULTS format, with a frozen judge (model, version, prompt pinned) and human-graded subsample agreement. Fix the EVAL_RESULTS.md path. *(Closes C1/C2; tells you the true distance to the beta gate.)*
2. **Licensing audit of the corpus**: per-entry source + license metadata; resolve the CC-BY-SA derivative question for MB-derived entries; separate corpus license from MIT code license. *(C3 — blocking for F-Droid and legal cleanliness.)*
3. **Lead with the deterministic surface**: promote corpus browser + First-7-Days checklist + ScamRules UI to the alpha's headline, demote chat to visibly-beta; pull the estate-letter template forward. *(S1/S2 — real, safe value for the actual user regardless of model quality.)*
4. **Design for the Verifier as installer**: install/verify handoff flow, printable verification sheet, in-app trust explanation; plus device-floor measurement on a 4 GB device alongside the 8 GB flagship. *(I4/I5/S7 — the persona-real distribution and hardware reality.)*
5. **Write the Safety Case + pre-register the Android beta gate**: one document tying threat model to evidence, with thresholds locked before runs and the danger-suite results published per release; fix the doc path/number hygiene (I1/I2) as part of it. *(C2/I2/I6/S3.)**

The foundation here is better than most shipped products' marketing claims. The next work is not more vision — it is measurement, evidence resolution, and putting the safe parts first.