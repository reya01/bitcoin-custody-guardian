- FILE: README.md | SAYS: "Deterministic safety layer (ScamRules, secret refusal, veto) — **Implemented**" | TRUTH: veto mustNotInclude is emptyList() in the Android chat path; wiring lands in v0.2 | FIX: "**Implemented** (veto vocabulary not yet wired in Android chat path, v0.2); logic unit-tested; end-to-end on-device suite pending"
- FILE: README.md | SAYS: "schema fields are verbatim subsets of the human-approved entry text" | TRUTH: only key_points/dos/donts are verbatim; user_says is retrieval-index-only, non-authoritative | FIX: "key_points/dos/donts are verbatim subsets; user_says is retrieval-index-only (non-authoritative)"
- FILE: README.md | SAYS: "## License MIT — see [LICENSE](LICENSE)" | TRUTH: MIT covers code only; corpus is separately licensed (CC BY-SA 4.0 intended per docs/LICENSES.md) | FIX: "Code MIT; corpus CC BY-SA 4.0 — see docs/LICENSES.md and LICENSE"
- FILE: docs/releases/v0.1.0-alpha.md | SAYS: "Post-model veto: answers containing dangerous advice are discarded and replaced" | TRUTH: mustNotInclude is empty in the shipped Android chat path, so veto cannot fire there until v0.2 | FIX: "Post-model veto: implemented and unit-tested in the pipeline; per-question veto vocabulary wiring in the Android chat path lands in v0.2"
- FILE: docs/releases/v0.1.0-alpha.md | SAYS: "adb push Qwen3-1.7B-Q4_K_M.gguf /sdcard/Android/data/com.custodyguardian/files/models/" | TRUTH: code looks for lowercase qwen3-1.7b-q4_k_m.gguf (MODEL_NAME); filename case mismatches | FIX: "adb push Qwen3-1.7B-Q4_K_M.gguf /sdcard/Android/data/com.custodyguardian/files/models/qwen3-1.7b-q4_k_m.gguf"
- FILE: docs/releases/v0.1.0-alpha.md | SAYS: "see `docs/release-signing.md`" | TRUTH: that file does not exist (broken link) | FIX: "see the README 'Verification status' table (docs/release-signing.md pending)"
- FILE: docs/SAFETY_CASE.md | SAYS: "schema fields are verbatim subsets of the approved entry text" | TRUTH: key_points/dos/donts verbatim; user_says is retrieval-index-only, non-authoritative | FIX: "key_points/dos/donts are verbatim subsets of the approved entry text; user_says is retrieval-index-only, non-authoritative"
- FILE: docs/EVAL_SET.md | SAYS: "No app build ships without passing it." | TRUTH: v0.1.0-alpha shipped with hosted scores 24–31/105; gate is pre-registered for beta (iphone_plan §0) | FIX: "The eval set is the beta release gate; alpha builds ship with scores published, gate pass required at beta."
- FILE: docs/EVAL_SET.md | SAYS: "run eval set against the shipped model+corpus bundle on the reference device profile" | TRUTH: v0.1.0-alpha published hosted-pipeline scores only; on-device runs pending Addendum 11 | FIX: "publish eval scores in release notes; on-device reference-device runs begin with Addendum 11 (alpha cited hosted runs, labeled as such)"

## Missing
- Answer button is hard-disabled while MemoryGuards.detectSecret hits (docs say "refused/warned", never the disable behavior).
- Walkthrough progress is persisted keyed by day + step-text prefix.
- AndroidManifest sets allowBackup="false" — no OS auto-backup of any app data.
- scripts/check_claims.py enforces the 26 Kotlin / 44 Python test counts.
- Model-missing state shows in-app sideload + sha256 verification instructions with the hash (docs only cover the release-page hash).
- Chat screen header reads "Ask (offline) — alpha (experimental)".
- Secret-bearing query text is NOT cleared from the input field on refusal (code comment claims it is).

## OK
docs/GATE_CONVENTIONS.md, docs/LICENSES.md, NOTICES fully current; README metrics table (19/40, 24/105, 31/105, 43/145, 84% 103/122, Add-11 pending) matches ground truth exactly.