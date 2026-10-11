- FILE: docs/PRODUCT_SPEC.md | SAYS: "Draft v0.1 — for iteration & brainstorm only. Not a commitment to build." | TRUTH: app is built; v0.1.0-alpha signed APK is on GitHub Releases. | FIX: "Status: v0.1.0-alpha shipped (signed APK, GitHub Releases); spec tracks the implementation."
- FILE: docs/PRODUCT_SPEC.md | SAYS: "never asks for, stores, or transmits secret material (seed words, xpubs, passphrases)" | TRUTH: xpubs are not secrets — undetected by MemoryGuards; spec §11 says so itself. | FIX: "(seed words, private keys, passphrases)"
- FILE: docs/PRODUCT_SPEC.md | SAYS: "model is bundled in the APK and covered by the reproducible-build verification" | TRUTH: model is sideloaded; app provides in-app sha256 verify instructions. | FIX: "model is sideloaded and verified in-app against the published SHA-256"
- FILE: docs/PRODUCT_SPEC.md | SAYS: "Bundled model only; device RAM gate at startup with clear messaging" | TRUTH: model is sideloaded, not bundled. | FIX: "One pinned, sideloaded, hash-verified model; device RAM gate at startup with clear messaging"
- FILE: docs/PRODUCT_SPEC.md | SAYS: "Primary: Qwen3 4B-class or Gemma 4 E2B/E4B" | TRUTH: shipped on-device model is Qwen3 1.7B Q4_K_M (per §9a and the build). | FIX: "Primary: Qwen3 1.7B Q4_K_M (shipped); 4B remains the optional Pro brain"
- FILE: docs/PRODUCT_SPEC.md | SAYS: "llama.cpp (JNI) with GGUF model, bundled" | TRUTH: GGUF is sideloaded; stack is NDK static lib + LlamaBridge JNI. | FIX: "llama.cpp (JNI) with sideloaded, hash-verified GGUF model"
- FILE: docs/PRODUCT_SPEC.md | SAYS: "F-Droid first (builds from our source on their servers…), then GitHub Releases" | TRUTH: v0.1.0-alpha shipped on GitHub Releases; F-Droid awaits legal review. | FIX: "GitHub Releases first (v0.1.0-alpha shipped); F-Droid after legal review"
- FILE: docs/PRODUCT_SPEC.md | SAYS: "bundles a Qwen3 1.7B-class Q4_K_M model (~1.0–1.1 GB) + corpus + app" | TRUTH: APK ships app + corpus only; model is sideloaded and hash-verified. | FIX: "ships app + corpus; the 1.7B Q4_K_M (~1.1 GB) is sideloaded and hash-verified in-app"
- FILE: docs/PRODUCT_SPEC.md | SAYS: "Device RAM gate + bundled model." | TRUTH: model is sideloaded. | FIX: "Device RAM gate + sideloaded, hash-verified model."
- FILE: docs/PRODUCT_SPEC.md | SAYS: "Scam-checker mode: paste a message/email → systematic red-flag analysis" | TRUTH: listed under v1.0 but scam-check mode already shipped in v0.1.0-alpha. | FIX: move to MVP list — "Scam-checker mode (shipped in v0.1.0-alpha)"
- FILE: docs/PRODUCT_SPEC.md | SAYS: "Bundled model size (~1.5–3 GB APK)" | TRUTH: model is ~1.1 GB and sideloaded; APK carries app + corpus only. | FIX: "Model size (~1.1 GB GGUF, sideloaded + hash-verified; APK stays small)"
- FILE: docs/CORPUS_SOURCES.md | SAYS: "Entry = JSON: id, topic, title, plain, technical, tags, claims[]" | TRUTH: real entries use key_points/dos/donts (verbatim subsets) + user_says (retrieval-index-only). | FIX: "Entry = JSON: id, topic, title, key_points, dos, donts, user_says (index-only), source{…}, corpus_version"
- FILE: docs/CORPUS_SOURCES.md | SAYS: "(mostly BSD or AGPL-3.0; BIP-32/39/44 BSD-3)" | TRUTH: BIP-32 is BSD-2-Clause. | FIX: "(mostly BSD or AGPL-3.0; BIP-32 BSD-2-Clause; BIP-39/44 per-header BSD)"
- FILE: docs/INHERITANCE_WALKTHROUGH.md | SAYS: "The app's Scam-Checker mode will analyze any message you paste" | TRUTH: scam-check mode is shipped. | FIX: "The app's Scam-Checker mode analyzes any message you paste — fully offline."
- FILE: docs/INHERITANCE_WALKTHROUGH.md | SAYS: "the corpus directory names Bitcoin-only companies with strong customer service" | TRUTH: no T11 directory entries exist in the 50-entry corpus. | FIX: "the app's guides explain temporary-custodian options and their risks (T11 directory not yet built)"
- FILE: docs/plans/iphone_plan.md | SAYS: "all 63 tests (22 unit + 41 harness) green in CI" | TRUTH: 26 Kotlin @Test + 44 Python = 70, enforced by scripts/check_claims.py. | FIX: "all 70 tests (26 Kotlin + 44 Python) green in CI"
- FILE: docs/plans/iphone_plan.md | SAYS: "router, 50-entry retrieval, ScamRules, veto, checklist logic" | TRUTH: veto mustNotInclude is not yet wired in the Android chat path (v0.2). | FIX: "router, 50-entry retrieval, ScamRules, checklist logic (chat veto lands in v0.2)"
- FILE: prototype/EVAL_RESULTS.md | SAYS: "| **TOTAL** | 20 | 40 | 50% |" | TRUTH: verified strict result is 19/40 (Add9); category rows need re-derivation. | FIX: "| **TOTAL** | 19 | 40 | 48% |"
- FILE: prototype/EVAL_RESULTS.md | SAYS: "23/40 (58%) after entry-aggregated retrieval" | TRUTH: superseded — verified approved-corpus strict score is 19/40 (Add9). | FIX: "19/40 strict after entry-aggregated retrieval (Add9 re-grade)"
- FILE: prototype/EVAL_RESULTS.md | SAYS: "Scam detection 6/6 and multisig 4/4 on the approved corpus" | TRUTH: current results table shows scam_detection 5/6. | FIX: "Scam detection 5/6 and multisig 4/4 on the approved corpus"
- FILE: prototype/EVAL_RESULTS.md | SAYS: "day1_securing 0/4, out_of_corpus_honesty 0/2, word_list_handling 1/4, inventory_privacy 1-2/4" | TRUTH: table shows day1 1/4, word_list 0/4, inventory 1/4. | FIX: "day1_securing 1/4, out_of_corpus_honesty 0/2, word_list_handling 0/4, inventory_privacy 1/4"

## Missing
- ANSWER_MODE env selector: default paraphrase+veto; hybrid = paraphrase + verbatim safety block (31/105); extractive fail-closed (12–13/105, scam/safety layering) — undocumented.
- Secret hit disables the Answer button; both chat entry points route through MemoryGuards.detectSecret.
- Chat header reads "alpha (experimental)"; alpha banners on chat, walkthrough, and scam-check screens.
- Walkthrough progress persists keyed by day + step-text prefix (not stable IDs).
- scripts/check_claims.py enforces the 26-Kotlin / 44-Python test-count claims.
- Corpus sha256 manifest lives in prototype/ — no doc gives its location.
- All eval numbers are hosted-only; on-device Addendum 11 (8 GB phone) still pending.
- Retrieval hit-rate 84% (103/122); denominator re-derivation pending — unrecorded.
- MiMo review action batch landed at commit b61604b — unrecorded.
- Raw 64-char hex deliberately not flagged as secret (txid false-positive tradeoff) — code comments only.

## OK
CORPUS_SOURCES 50-entry provenance table + Mastering Bitcoin CC BY-SA 4.0 / bitcoin.org MIT / River reference-only notes · iphone_plan §0 pre-registered baselines (31/105, 19/40; 50-entry freeze) · EVAL_RESULTS answerer config (Qwen3-1.7B Q4_K_M, temp 0, /no_think, cap 1600) · INHERITANCE_WALKTHROUGH 7-day structure + do-nothing rule · PRODUCT_SPEC §3.2 detector scope (BIP39/WIF/xprv) + §3.8 visible-downgrade note.