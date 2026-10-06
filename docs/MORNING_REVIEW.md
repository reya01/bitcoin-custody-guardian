# Morning Review — Bitcoin Custody Guardian, First Architecture Pass
**2026-10-06 · Prepared overnight per request · All work verified, not self-reported**

---

## Where the project stands

The spec phase is done. The first architecture pass is built, tested, and
pushed to `github.com/reya01/bitcoin-custody-guardian` (private). Everything
below was re-verified this morning by direct execution or repo read-back.

## 1. Deterministic prototype core ✅ (39/39 tests passing — re-run by me)

`prototype/guardian_core.py` — the entire safety-critical front-half, pure
stdlib Python (portable to Kotlin later):

- **SecretDetector** — bundled official BIP-39 wordlist (2,048 words, in
  `prototype/data/`); detects 12/15/18/21/24-word phrases inline with
  typo-fuzz, WIF keys, xprv/zprv-family, 64-hex keys, SLIP-39 shares →
  returns kind + confidence + standardized warning
- **Router** — mode classification (learn / scam-check / verify / directory /
  walkthrough) + T01–T12 topic scoring
- **Retrieval** — BM25 over the corpus JSON (title/plain/technical/claims),
  top-k with entry ids for citations
- **Guardrails** — must_include / must_not_include enforcement, danger-phrase
  scrub with mandatory warning injection, refusal templates (out-of-corpus,
  offline questions)
- **compose()** — full pipeline: question → mode → detections → retrieval →
  constraints → refusal-or-grounded-context. The LLM answer step is a
  documented stub (the real one ships in Kotlin against the bundled model).

Tests: `prototype/test_guardian_core.py`, 39 tests, all pass (0.9s).

## 2. Android skeleton ✅ (23 files pushed; manifest verified clean)

Kotlin 2.0 + Compose, minSdk 29 / targetSdk 35:
- `AndroidManifest.xml` — **zero permissions declared** (verified by grep);
  only launcher activity exported; comment documents why
- Screens: HomeMode, Walkthrough (renders bundled 7-day walkthrough.json),
  CorpusBrowser (reads corpus JSON), ChatPlaceholder (Kotlin port of the
  deterministic router — intent + retrieved entries, no LLM), ScamChecker
  (local regex red-flag scan)
- `MemoryGuards.kt` — seed-phrase detection with bundled wordlist asset
- Assets: corpus entries, walkthrough.json, BIP-39 wordlist
- `README-BUILD.md` — build steps + post-change manifest checklist
- **GrapheneOS addendum (your request):** audited and documented — zero
  Google/Play dependencies (pure androidx), zero permissions (user-verifiable
  on GrapheneOS's indicators), sideload-native, no providers/WebView/
  cleartext. Commit `a984a90`.

⚠️ **Honest caveat:** this machine has no Java/Gradle/Android SDK, so the
skeleton has never been compiled. Kotlin files are syntax-checked only.
First build must happen on your machine or CI (`README-BUILD.md`).

## 3. Corpus ✅ 10 entries total (2 reviewed-approved, 8 awaiting you)

- **Approved:** T02-river-seed-phrase (20% figure dropped per your call),
  and T02-bip39-standard pending your one-word approval
- **Batch #2 (unreviewed):** River Learn private-key, passphrase,
  cold-storage, multisig, self-custody + BIP-32/44/84/49 → each with
  plain/technical versions, claims with citations, warnings, and
  reviewer-notes flagging judgment calls
- 1 source failed (no dedicated River multisig article at the tried slugs);
  multisig entry was produced from available material
- Full audit trail: `corpus/BATCH_REPORT.md`

**Your review queue today:** the 8 unreviewed entries — the flagged items
are mostly (a) uncited statistics, (b) risk_class calibration (danger vs
caution), (c) community-standard knowledge not literally in the source.

## 4. Eval set ✅ 40 T06 items (JSONL, all validated)

`eval/T06-walkthrough.jsonl` — category counts exactly per spec
(day-1 securing 4, word-list handling 4, device/do-not-reset 6, inventory
privacy 4, jargon 5, multisig/descriptor 4, exchange accounts 3, scam 6,
verification sequence 2, out-of-corpus honesty 2). **11 danger-class items**
(seed-typing, device-reset, seed-sharing). Distribution explained in
`eval/README-T06.md`. Remaining categories (T01–T12, ~160 more items) are
next once you bless the format.

## Infrastructure running quietly

- Weekly repo audit job (871bcf73e807) — Mondays 16:00 UTC, K3+GLM flex
  auditors → Tinfoil judge; delivered to this chat. First run Oct 12.

---

## Recommended next steps (in order)

1. **Compile the Android skeleton** (your machine or CI) — the one thing
   overnight work couldn't do. Fixes are usually small; I'll handle them.
2. **Review the 8 corpus entries** — batch approve or send me edits.
3. **Wire the real answerer into the prototype** — run the bundled Qwen3
   1.7B Q4 locally (llama.cpp on a desktop) behind the compose() stub to
   measure grounded-answer quality on the 40 eval items. This is the first
   true end-to-end quality gate and needs no Android build.
4. **Remaining eval categories** (T01/T02/T08/T10 sets) after T06 format
   blessing.
5. Corpus batches #3+ on flex tier (T04 hardware-wallet vendor guides, T08
   scam patterns) — slow overnight work again.
