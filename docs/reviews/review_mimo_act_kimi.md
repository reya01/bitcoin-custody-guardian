## Verdict

**FAIL.** Two Critical findings: the commit's own safety-case claims are contradicted by the shipped chat code, and the new license doc is contradicted by the commit's own provenance table. Both are exactly the class of error this repo exists to catch.

## Critical

**1. Secret-input refusal is not enforced on the chat "Answer" path — contradicts README, SAFETY_CASE M3, and the file's own docstring.**
- `ChatPlaceholderScreen.kt`: the only `MemoryGuards.detectSeedPhrase` call is inside the **"Run router"** button handler. The **"Answer"** button calls `runAnswer(context, query)` directly, which builds `ComposedContext(isSecret = false, // secret questions are refused before reaching here)` — the asserted precondition is never established on this path.
- Even when detection *does* fire (via "Run router"), `secretWarning` is set but the "Answer" button stays enabled — the user can still send the seed to the model. That is a warning, not a refusal.
- Contradicted claims: README bullet 2 ("seed phrases / xprvs pasted into the app are detected, warned about, and **refused**"), SAFETY_CASE M3 ("Secret detection **first in every path**"), and the file's KDoc ("secret detection FIRST, then deterministic router + retrieval, then the on-device model").
- Sub-gap: only `detectSeedPhrase` is invoked; no xprv/WIF/hex detection is visible on the Android side, while README explicitly claims xprvs.

**2. LICENSES.md's "reference-only" claim is contradicted by the provenance table added in the same commit; the wholesale CC BY-SA overclaims rights.**
- `docs/LICENSES.md`: River Learn / vendor documentation "are cited but **not reproduced beyond short attributed references**."
- `docs/CORPUS_SOURCES.md` (new table): `what-is-multisig-river` reproduces 7 chunks of the River glossary (opening, first, second, third paragraphs + example + related terms — essentially the whole entry); `what-are-public-and-private-keys` lists 9 River sections; `cold-storage-and-hardware-wallets` 10 River sections; BitBox 17 sections; Ledger 13; Jade 9. README's verification row confirms the corpus rule is "schema fields are **verbatim subsets**, machine-checked" — i.e., verbatim reproduction, not short reference.
- Compounding it: the project cannot license River/Ledger/BitBox/Blockstream/Lopp/Casa text under CC BY-SA 4.0 — it doesn't hold those rights. "The knowledge corpus … is licensed as a whole under CC BY-SA 4.0" mislabels third-party content. Either the table or the license doc is false; as committed, both are published together.

## Important

3. **Stale test count in README.** "22/22 JVM unit tests and **41/41** Python harness tests green" — the commit adds 3 tests (the shown file contains exactly 44 test methods), the batch note says 44/44, and SAFETY_CASE.md says "22 Kotlin + **44** Python". Same-commit contradiction in the flagship honesty table.
4. **Broken links in `docs/LICENSES.md`.** `[LICENSE](LICENSE)` resolves to `docs/LICENSE` and `[NOTICES](NOTICES)` to `docs/NOTICES`; both files are at repo root (README links them from root). Both need `../`.
5. **Referral-regex false negatives.** `[?&](ref|referral|affiliate|utm_|aff|tag|partner)=` anchors `=` immediately after the token, so `?utm_source=`, `?utm_medium=`, `?utm_campaign=`, `?aff_id=`, `?ref_src=` all escape. `utm_=` matches no real parameter — the most common tracking params pass the lint.
6. **Dead `#1` alternative.** In `\b(…|#1|…)\b`, `\b` before `#` (a non-word char) requires a preceding word char, so "the #1 wallet" never matches. The alternative only fires glued to a word character.
7. **The two ranking tests encode contradictory policies.** `test_no_undated_vendor_rankings` fails on *any* vendor+superlative sentence — it has no date logic despite its name and the docstring ("no *undated* vendor rankings"). That makes `test_dates_present_near_rankings` (dated rankings acceptable) unreachable dead code. Fix one or the other.
8. **Non-portable, vacuous-prone lint.** `CORPUS` defaults to hardcoded `/opt/data/projects/custody-guardian/corpus` while the rest of the file computes `CORPUS_DIR` relative to the test file — the 3 tests error on any other machine, undercutting the reproducibility story. And no test asserts that >0 corpus files were scanned, so a wrong-but-existing `GUARDIAN_CORPUS` passes vacuously.
9. **Provenance table is malformed.** Rows range from 3 to ~19 pipe-cells; GFM pads short rows and *silently drops excess cells*, so rendered provenance is lost. At least three cells are truncated mid-token with unbalanced quotes: `"...into the BitBoxAp`, `"...access to my PIN  |`, `"...and 'Redeem  |`. Two Ledger cells are non-specific ("Same section", "Link embedded in the same section") — not actionable provenance.
10. **bitcoin.org license contradiction.** LICENSES.md calls bitcoin.org a CC BY-SA source; README says only Mastering Bitcoin is CC BY-SA and "the per-source status for **everything else** is recorded in CORPUS_SOURCES.md". (Real-world: bitcoin.org is released under the MIT license — so LICENSES.md is the wrong one.)
11. **On-device veto wired with nothing to enforce.** `runAnswer` passes `mustNotInclude = emptyList()` and the `ComposedContext` built has no must-include field — SAFETY_CASE M4's "veto layer replaces answers that violate per-question constraints" has no constraints in the shipped path.
12. **"beta" label contradicts the project's alpha framing.** Chat header reads "Ask (offline) — beta" while README says "Status: experimental **alpha** — not yet safe to rely on" and ships v0.1.0-alpha — and chat is the least-demonstrated surface (on-device quality "Not demonstrated").

## Minor

13. Documents list mislabels a link: `[docs/EVAL_RESULTS.md](prototype/EVAL_RESULTS.md)` — target works, label names a nonexistent path.
14. "Current numbers (**dated**…)" but the As-of column contains addendum refs, not dates. `~85% (103/122)` = 84.4%, rounded up. The 122 denominator is unexplained next to the advertised 145-item suite. GATE_CONVENTIONS' "retrieval-coverage numbers are upper bounds" caveat isn't carried into the README table.
15. M5 claims the model is "sha256-verified by the user", but the in-app sideload instructions (model-missing text) never mention verifying the published hash.
16. SAFETY_CASE claims a banner "on every model-touching surface"; this commit banners two. M1 says the model explains scam verdicts — if that explanation renders on a scam-check surface, it's unbannered. Unverified.
17. Lint coverage gaps: test 2 scans only `plain`/`technical`/string `key_points` (`title`, `warnings` escape); comparatives ("better than", "safer") aren't covered; splitting on `[.!?]\s` can sever vendor from superlative ("…the leading choice for U.S. users, Trezor…"); common-word vendor names (river, strike, jade, casa) are FP-prone.
18. README Documents omits the two new docs — the "F-Droid submission centerpiece" (SAFETY_CASE.md) and GATE_CONVENTIONS.md are unreachable from README.
19. GATE_CONVENTIONS cites "the iPhone plan §0" — no such document is linked or identifiable.
20. CC BY-SA attribution completeness: `source_url` alone doesn't satisfy BY-SA 4.0 §3(a) (creator, license notice+URI, change indication) for the Mastering Bitcoin excerpts; BIP-32 is BSD-2-Clause, whose notice must be retained on verbatim reproduction — retention not shown anywhere.
21. `test_word_boundaries` still ships the junk assertion `self.assertNotIn(24, [0])  # placeholder`.