## Reconciled verdict

**FAIL — merge blocked pending P0 fixes (Items 1–3). Kimi-K3's verdict is upheld; DeepSeek-V4-Pro's PASS-with-fixes is overruled.**

**Reasoning.** The two inspectors did not disagree about *what* is broken — they disagreed about *severity*. DeepSeek's own findings #4, #5, and #11 are the same defects as Kimi's C2 and C1, graded Important/Important/Minor. Under this repo's stated constraints ("claims must be demonstrated"; "no-answer-beats-wrong-answer"), a shipped README/SAFETY_CASE claim contradicted by shipped code in the same commit is a merge blocker, not a fix-later item — especially for the F-Droid centerpiece. A verdict that concedes the substance of both criticals cannot stand as PASS.

- **C1 (confirmed by operator):** the "Answer" path skips `MemoryGuards.detectSeedPhrase` entirely, and even the "Run router" path only *warns* (Answer stays enabled) — so the README's "detected, warned about, and **refused**" and SAFETY_CASE M3's "first in every path" are both false as shipped. The `isSecret = false // secret questions are refused before reaching here` comment asserts a precondition nothing establishes.
- **C2 (substance confirmed by operator):** given the corpus discipline is "verbatim subsets, human-approved," the provenance table is the credible record and `LICENSES.md`'s "not reproduced beyond short attributed references" is the false statement. The wholesale CC BY-SA claim additionally mislabels third-party text the project has no rights to. The legal analysis needs care (see Fix 2 and Deferred D1), but the *contradiction* is not in doubt.

Both reviews were complementary: Kimi uniquely caught the regex forensics (utm_/#1), the malformed provenance table, the `../` links, and the beta/alpha mismatch; DeepSeek uniquely caught the dead `FALLBACK_REFUSED` constant (which becomes the fix for C1), the "machine-checked" overclaim, and the hashCode key. Nothing architectural is required; the remainder is doc/test hygiene. Recommend a focused delta re-review of Items 1–3 only, after they land.

**Finding disposition (all 41):**

| Finding | Disposition | Fix |
|---|---|---|
| Kimi C1 = DS#11 | Accept (operator-confirmed); DS severity Minor→**Critical** | 1 |
| Kimi C2 = DS#4 = DS#5 | Accept (operator-confirmed); DS severity→**Critical** | 2, D1 |
| Kimi I3 = DS#1 = DS#6 | Accept (duplicate ×3) | 4 |
| Kimi I4 | Accept | 2c |
| Kimi I5 | Accept | 11a |
| Kimi I6 | Accept | 11b |
| Kimi I7 ≈ DS#9 | Accept (DS#9 is a partial duplicate; merged) | 11d |
| Kimi I8 = DS#3 | Accept (duplicate) | 10 |
| Kimi I9 | Accept | 3 |
| Kimi I10 | Accept (verify MIT before merge) | 2b |
| Kimi I11 | Accept | 5, D2 |
| Kimi I12 | Accept | 9 |
| Kimi M13 = DS#2 | Accept (duplicate) | 12a |
| Kimi I17 ≈ DS#7 ≈ DS#8 | Accept (three distinct gaps, one work item) | 11 |
| Kimi M14 | Accept | 12b |
| Kimi M15 | Accept | 7 |
| Kimi M16 | Accept (audit-first, per Kimi's own "unverified" flag) | 8 |
| Kimi M18 | Accept | 12a |
| Kimi M19 + DS#15 | Accept (same file, distinct defects) | 13 |
| Kimi M20 | Accept | 2b |
| Kimi M21 | Accept | 11g |
| DS#10 | Accept (cross-check task) | 3b |
| DS#12 | Accept (folds into C1 fix) | 1b |
| DS#13 | Accept (product decision) | D3 |
| DS#14 | Accept | 12c |
| DS#16 = DS#17 | Accept (DS self-duplicate) | 6 |
| DS#18 | Accept | 1e |
| DS#19 | **Partial rebuttal** (fix accepted, rationale wrong) | 14 |
| DS#20 | Accept | 12a |

## Fix list (ordered)

**P0 — merge blockers**

**1. Enforce secret refusal on every chat path.** *(Kimi C1, DS#11, DS#12, DS#18, C1 xprv sub-gap)*
File: `ChatPlaceholderScreen.kt`, `MemoryGuards.kt`.
- Add a single guarded entry point; both "Answer" and "Run router" `onClick` handlers call it: run detection **first**; on hit → set `secretWarning`, set `answer = FALLBACK_REFUSED` (wires the dead constant), clear `query` so the secret doesn't persist in editable state, and return **without invoking the model**. This converts the router path's warning into an actual refusal, per README/SAFETY_CASE M3.
- Add `MemoryGuards.detectExtendedSecret()`: xprv-family (`[xtvzup]prv[1-9A-HJ-NP-Za-km-z]{50,}`) and WIF (`\b[5KL][1-9A-HJ-NP-Za-km-z]{50,51}\b`). Do **not** claim raw-hex detection (64-hex collides with txids — see D4). Update README bullet 2 to list exactly what is implemented (BIP-39 phrases, xprv/tprv, WIF).
- Never echo or log the typed secret on the refusal path.
- Reword banner: "Never type seed words or keys into this app or any online service." (DS#18)
- New JVM tests: seed via Answer → `FALLBACK_REFUSED`, model not invoked; xprv via Answer → refused; seed via Run router → refused; clean query → answered.

**2. Make LICENSES.md tell the truth; correct per-source licensing.** *(Kimi C2, DS#4, DS#5, I4, I10, M20)*
File: `docs/LICENSES.md`.
- Replace "cited but not reproduced beyond short attributed references" with an accurate statement matching the provenance table: section-level verbatim extracts exist from River Learn and vendor documentation, which grant no reproduction license; redistribution of builds containing these extracts is **blocked pending resolution** (D1). Delete the "facts-only, reference-only terms" legal conclusion — facts aren't copyrightable, but the extracted *expression* is; the project must not assert a license position it can't back (no-answer-beats-wrong-answer, applied to legal claims).
- Scope CC BY-SA 4.0 to project-original content + Mastering Bitcoin excerpts only. Per-source corrections: **bitcoin.org → MIT** (verify against the source repo before merge), with notice retention; **BIP-32 → BSD-2-Clause**, retain copyright/disclaimer text in `NOTICES.md` or per-excerpt; **Mastering Bitcoin → CC BY-SA 4.0** with full §3(a) attribution (creator, license notice + URI, indication that excerpts are unmodified subsets / list of changes).
- Fix relative links: `[LICENSE](../LICENSE)`, `[NOTICES](../NOTICES)`.

**3. Rebuild the provenance table.** *(Kimi I9, DS#10)*
File: `docs/CORPUS_SOURCES.md`.
- Normalize every row to a fixed column schema (entry | source | section | license | url | notes); no truncated cells; escape pipes inside quoted text; replace "Same section" / "Link embedded in the same section" with specific section titles/URLs (GFM silently drops excess cells, so rendered provenance is currently being lost).
- Reconcile counts: table rows == corpus JSON files == README's "50 entries across 12 topics"; add a `license` column so LICENSES.md and this table cannot drift (cross-checkable by the script in Fix 4).

**P1 — same batch (claims-accuracy; complete before re-review)**

**4. Test-count truth + machine guard.** *(I3, DS#1, DS#6)*
Files: `README.md` ("Where the project stands"), `SAFETY_CASE.md` §3.
- Set both to the same actual numbers **at merge time** (Fix 1 adds JVM tests; Fix 11 adds Python tests — counts will move).
- Add `scripts/check_claims.py`: count `@Test` methods and `def test_` methods, parse the numbers out of README/SAFETY_CASE, fail CI on mismatch. Converts a stale-prone claim into a demonstrated one.

**5. SAFETY_CASE M4 veto claim.** *(Kimi I11)*
File: `SAFETY_CASE.md` §2 M4 (and README verification table if repeated).
- Reword to: "veto vocabulary (`mustNotInclude`) is defined per question; enforcement is **not yet wired** in the shipped chat path (currently `emptyList()`) — planned v0.2." Wiring the constraints is D2; do not ship the claim ahead of the mechanism.

**6. "Machine-checked verbatim subsets" claim.** *(DS#16, DS#17)*
Files: `SAFETY_CASE.md` §2 M4, `README.md` verification table.
- Locate any existing checker. If it exists, cite it by path in both docs. If not, reword to "human-reviewed; sha256 manifest recorded" **before merge**, and file the subset-verification tool as a follow-up.

**7. Model hash verification instructions.** *(Kimi M15)*
File: `ChatPlaceholderScreen.kt` (model-missing text) + README model section.
- Display the expected sha256 and verification steps (`sha256sum model.gguf` / on-device method). SAFETY_CASE M5's "sha256-verified by the user" may only stand once the instructions ship.

**8. Banner coverage audit.** *(Kimi M16)*
Files: `SAFETY_CASE.md`, scam-check surface.
- Enumerate model-touching surfaces. If the scam-verdict explanation renders model output unbannered, add the banner composable there; otherwise narrow the claim to the enumerated bannered surfaces.

**9. beta → alpha.** *(Kimi I12)*
File: `ChatPlaceholderScreen.kt` header: "Ask (offline) — alpha (experimental)", matching README status and v0.1.0-alpha.

**P2 — robustness/quality (same or immediately following batch)**

**10. Lint portability + non-vacuity.** *(I8, DS#3, DS#10)*
File: `TestCorpusVendorNeutrality`.
- `CORPUS = os.environ.get("GUARDIAN_CORPUS", str(CORPUS_DIR))` — reuse the repo-relative `CORPUS_DIR` already computed at the top of the file.
- After listing: `self.assertGreater(len(files), 0)`; optionally `assertEqual(50, ...)` to pin the README claim.

**11. Lint hardening.** *(I5, I6, I7, I17, DS#7, DS#8, DS#9, M21)*
File: `TestCorpusVendorNeutrality`.
- Referral regex → `[?&](ref\w*|utm_[a-z_]+|aff\w*|tag|partner)=` (catches `utm_source/medium/campaign`, `aff_id`, `ref_src`; note `reference=`/`refresh=` FPs — acceptable for a human-review lint; use the explicit `(ref|referral|ref_src|…)` variant if precision matters more).
- Superlative regex → `(?:\b(?:the best|the top|the leading|the most trusted|number one|the safest|the most secure|the easiest|the gold standard|industry standard|best-in-class|top-rated|highly recommended|our pick|the only choice)\b|(?:^|\s)#1\b)` — fixes the dead `#1` alternative (`\b` before `#` can never match after a space) and broadens coverage.
- Vendors: add `foundation devices|passport|coinkite|specter|bluewallet|electrum|nunchuk|shift crypto`; for common-word names (river, strike, jade, casa) require word boundaries and consider case-sensitivity to cut FPs.
- Ranking-test policy: add `DATE = r"\b(19|20)\d{2}\b|as of|currently"`; `test_no_undated_vendor_rankings` flags vendor+superlative **only when no date appears in the sentence ±1** (currently it has no date logic, making `test_dates_present_near_rankings` unreachable dead code); broaden the dates test from `the best|the top` to the full SUPERLATIVE set.
- Scan `title` and `warnings` fields, not just `plain`/`technical`/`key_points`; guard sentence-splitting against `U.S.`-style abbreviations or evaluate a ±1-sentence window around vendor mentions.
- Delete `self.assertNotIn(24, [0])  # placeholder`.

**12. README Documents + numbers cleanup.** *(M13/DS#2, M18, DS#20, DS#14, M14)*
File: `README.md`.
- Documents section: fix label to `[prototype/EVAL_RESULTS.md](prototype/EVAL_RESULTS.md)`; add `docs/SAFETY_CASE.md` and `docs/GATE_CONVENTIONS.md`; make the `docs/reviews/` link relative.
- "Current numbers": populate As-of with real dates or rename the column; `~85% (103/122)` → `84% (103/122)` (84.4% rounds down); footnote the 122-vs-145 denominator (which items excluded, why); carry GATE_CONVENTIONS' "retrieval-coverage numbers are upper bounds" caveat into the table caption.
- Replace "the dev box cannot run…" with "the development machine (8 GB RAM) cannot yet load the 4B-class model."

**13. GATE_CONVENTIONS cleanup.** *(M19, DS#15)*
File: `docs/GATE_CONVENTIONS.md`. Link or remove the "iPhone plan §0" reference; pin the judge to an exact, verifiable model + version (verify `zai-org/GLM-5.3` exists; if an internal alias, state the mapping).

**14. WalkthroughScreen stable progress key.** *(DS#19 — see Rebuttal R2)*
File: `WalkthroughScreen.kt`. Replace the `step.hashCode()` persistence key with the step string itself (or SHA-256 of it).

## Rebuttals

**R1 — DeepSeek's overall PASS-with-fixes verdict (rejected).** DeepSeek's findings #4, #5, and #11 identify the same defects as the confirmed criticals but grade them Important/Important/Minor. The repo's own bar — "claims must be demonstrated," with SAFETY_CASE as the F-Droid submission centerpiece — makes a same-commit contradiction between shipped claims and shipped code a blocker. The verdict is internally inconsistent with its own findings list; the *findings* are accepted, the *verdict* is not.

**R2 — DS#19's rationale (rebutted; fix accepted on corrected grounds).** DS#19 claims `String.hashCode()` is "not guaranteed stable across JVM versions or Android API levels." This is incorrect: the algorithm (`h = 31·h + s[i]`, empty string = 0) is normatively specified in the Java API documentation, and ART implements the spec — the value is deterministic and stable. The recommendation is nonetheless accepted, for the *correct* reasons: (a) 32-bit hash collision risk as the step set grows, and (b) key orphaning whenever step wording changes. Fix 14 stands; the justification is corrected.

**R3 — DS#13's "may be intentional" hedge (not rebutted; routed to decision).** The observation is accurate and the intent question is genuinely a product call — moved to Deferred D3 rather than accepted as a defect.

No Kimi finding is rebutted. Kimi I10's bitcoin.org-MIT assertion is accepted per the operator's constraint, with a verify-before-merge step folded into Fix 2b (site-code MIT license should be confirmed against the source repository).

## Deferred (owner decisions)

**D1. Corpus content strategy for unlicensed sources (River, vendor docs).** Options: (a) rewrite extracted sections as original expression of the underlying facts — recommended; preserves the 50-entry corpus and aligns with the "human-approved" corpus discipline; (b) trim to genuinely short attributed quotes; (c) remove entries. Blocks *release/publication*, not merge, once Fix 2's accurate disclosure lands. Legal review recommended before the F-Droid submission.

**D2. Wire `mustNotInclude` constraints now vs. v0.2.** Fix 5 makes the claim honest today; wiring per-question constraints into `runAnswer`'s `ComposedContext` is a scope decision (if question metadata already carries constraints, the wiring is small — check before deciding).

**D3. Walkthrough DO-less day semantics (DS#13).** Auto-complete days with no DOs, or enforce a data invariant that every day has ≥1 DO. Recommend the invariant + a lint/test, so "You did enough today" semantics stay meaningful.

**D4. Raw-hex secret detection.** Recommend *not* claiming it (64-char hex is indistinguishable from txids → guaranteed false positives). If product wants it, it needs confirmation-gated detection, not silent matching.

**D5. Verbatim-subset machine checker.** Whether to build the subset-verification tool now (Fix 6's reword is the interim honest state) — effort call tied to the F-Droid timeline.