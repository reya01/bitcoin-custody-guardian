## Confirmed — Blocking

- ModelImporter.kt:~98 — GGUF branch hashes/counts the 4 peeked magic bytes but never writes them to `out`; imported file is 4 bytes short yet passes size+sha256 and reports Ok — write `head` to `out` before `drain` (both reviewers).
- ExtractiveAnswer.kt:~197 — `manifest.isEmpty() || sha256(...) in manifest` makes the documented integrity gate a no-op whenever AssetLoader.loadKeyPointManifest silently returns emptySet on any parse error — fail closed on empty manifest (both).
- extractive.py:58-72,219-224 — same fail-open in the prototype: missing/corrupt manifest caches `{}` and `_point_ok()` returns True process-wide, silently disabling the verbatim gate — fail closed when the manifest is absent (both).
- AnswerPipeline.kt:~46 — ScamRules verdict is computed but never shown to the user nor passed to the prompt (only `verdict != null`); on blank/vetoed model output the verdict is silently discarded, contradicting SAFETY_CASE M1 "verdict is primary and un-overridable" — prepend `scan.verdict` to the returned text and embed it in the system prompt (both).
- ScamCheckerScreen.kt:~20 — `explainVerdict` output is displayed raw with no VetoRules scrub; a corrupted model can restate "it's a scam" as "looks safe, reply" — pass model output through VetoRules before display (both).
- VetoRules.kt:~118 — normalize() never collapses whitespace or strips zero-width chars, so "share your\nseed" splits across segments and bypasses both veto and scrub — collapse `\s+` and strip zero-width/format chars before matching (both).
- VetoRules.kt:~60 — inserting "phrase"/"words" between "seed" and "into/on" defeats every website-veto variant and matching DANGER_PHRASES ("type your seed phrase into the site" passes veto and scrub); determiner gap ("this/any/that site") evades the same variants — regex with optional (phrase|words) and a full determiner set (both).
- INHERITANCE_WALKTHROUGH.md:165 — Day 6 advises "his old laptop, wiped/reinstalled" as the clean computer, contradicting Day 1's "do not delete anything" and risking destruction of wallet files, descriptors, and evidence — require a separate/new machine or an explicit full-backup-and-estate-settled precondition before any wipe (Kimi; corroborated by DeepSeek Day 6 findings).
- judge_semantic.py:33,80 — must_not_include and refusal-behavior failures are recoverable via `passed_final = passed or semantic_lenient`, passing forbidden-content and non-refusing items against the documented "never recoverable" policy and inflating the release gate — require `not must_not_include_hits` and `refusal_ok` before leniency (Kimi).
- judge_semantic.py:60 — partial judge output fails open: phrases missing from the LLM's 'judgements' are silently dropped, so 1 'same' of 3 required phrases passes — fail the item unless every mi_failed phrase has a verdict (Kimi).
- answer_step.py:287 — scam-verdict branch falls back to remote sail_answer with no disclosure note and even when backend="llama" was explicitly requested (main path re-raises) — pasted scam text silently leaves the machine — mirror the `if backend == "llama": raise` and append the downgrade note (Kimi).
- answer_step.py:241 — extractive mode hardcodes remote sail_answer as the selection model_call with no local option, undisclosed backend label, and CANNED_LOCAL_UNAVAILABLE falsely asserting "Nothing was sent anywhere" — route through llama_answer for auto/llama and make the canned message backend-aware (both).
- scam_rules.py:16 + ScamRules.kt:22 — seed_request misses the canonical phishing verbs; "Please verify your 12-word seed phrase within 24 hours or your funds will be frozen" scans clean → "No red flags" for active seed phishing — add verify|validate|paste|tell|input plus "within N hours"/frozen/suspended patterns in both mirrors (both; ruled Blocking).
- make_bundle.py:117,91 — no-INTERNET and cert-pin gates fail open: subprocess returncodes unchecked, so aapt/apksigner failure or empty output silently passes the core-guarantee gate on the shipped APK — capture results, die on nonzero exit or empty output (both; ruled Blocking).
- T10-recognizing-bitcoin-scams…json + 2 sibling T10 files — reviewed=false while the inventory, fact sheet, and Addendum 12 claim 51/51 reviewed; unreviewed scam- and sending-safety content ships — complete review or exclude and rebuild the sha256 manifest (both).
- EVAL_RESULTS.md:115 — T06-013 answer "Yes — powering them on just to look is okay" is opposite to the corpus day-one rule yet hit zero must_not_include; the strict walkthrough gate has no reversal/contradiction check, so the 35/40 "gate met" claim is unreliable — add opposite-advice must_not patterns or a contradiction verdict (Kimi).
- README.md:3 — install flow has no verification step: in-zip checksums are self-referential and no APK SHA-256 or signing-cert fingerprint is published in-repo (only same-channel on the release page), leaving spec §5's distribution adversary unmitigated — publish both in README and add a verify-before-install step (Kimi; DeepSeek README:30 concurs on the gap).

## Confirmed — Should-fix

- AnswerPipeline.kt:37 + ChatPlaceholderScreen.kt:~175 — secret handling trusts caller-set `isSecret` and the button enabled-state; runAnswer never re-checks — run detectSecret inside runAnswer at click time and inside AnswerPipeline as defense-in-depth (both).
- AnswerPipeline.kt:48 — mode is a raw string; a typo silently disables the deterministic scam scan — enum/sealed type (both).
- AnswerPipeline.kt:55 — no empty-retrieval guard; the 1.7B model is invoked over an empty corpus and may improvise — short-circuit to the no-answer text when chunks are empty (Kimi).
- AnswerPipeline.kt:62,85 — exception class/message echoed into user-facing text, and vetoed answers return OUT_OF_CORPUS_REFUSAL misattributing the cause — generic error text plus a distinct "answer failed safety check" refusal (both/Kimi).
- AnswerPipeline (post-model) — no secret-like output filter; the model could emit key-like strings — run the detector on the candidate and veto/scrub (DeepSeek).
- MainActivity.kt:~53,58 — a null manifest silently skips the model gate ("works anyway"), and gating the whole app on `gate.ready` contradicts FALLBACK_MODEL_MISSING — explicit manifest-error state plus a skip path for non-model screens (both).
- ChatPlaceholderScreen.kt:~205,100 — `finally { engine.unload() }` can itself throw (UnsatisfiedLinkError) masking the fallback, and a throw before runAnswer's internal try leaves `busy=true` and crashes the coroutine — runCatching around unload, try/finally resetting busy (both).
- AssetLoader.kt:~24 — serves reviewed=false entries as "human-reviewed" and silently drops per-file parse failures — filter `reviewed == true` and surface the dropped count (both).
- AssetLoader.kt:~48 + ModelPaths.kt:~18 — asset readers never closed — wrap in `.use` (both).
- ExtractiveAnswer.kt:~48 — no self-test that Kotlin norm()+sha256 matches gen_manifest.py; normalization drift silently fails 100% of extractive answers closed — bundle one known hash vector and verify at startup (Kimi).
- ModelLocations.kt:~120,35 — a ~1.1GB hash-copy is reachable from composition with no dispatcher, and there is no re-verify path when verified.json is lost — background dispatcher plus a verify-existing-file entry point (Kimi).
- ChatPlaceholderScreen.kt:~80 — detectSecret runs on every keystroke and modelFile() does I/O during composition — cache the wordlist, hoist file checks (both).
- LlamaEngine.kt:~28 — ManifestEntry.minRamMb is parsed but never enforced before loadModel — enforce or document where it is checked (both).
- ScamCheckerScreen.kt:~130,150,100 — remembered explanation is not keyed on the result (stale explanation over a new verdict), blank model result renders a permanent empty box, and blank input scans to a clean verdict — key the remember, blank→error branch, require non-empty input (DeepSeek/Kimi).
- ModelGate.kt:55,70,66 — `manifest.byId(defaultModelId)!!` crashes on a malformed manifest, legacy migration races a picker import into the same dir, and the init IO scope is not lifecycle-tied — Phase.ERROR fallback, disable the picker during migration, caller-supplied scope (both).
- ModelGate.kt:48 + README.md:46 — deleteSource defaults to true, irreversibly deleting the user's only downloaded copy — default off (both).
- WalkthroughScreen.kt:25 — keyFor uses step.take(60), so prefix-sharing steps collide and copy edits orphan progress — key by day+step index (both).
- CorpusRouter.kt:78 — substring keyword matching misroutes ordinary sentences ("will"→inheritance, "pin"→hardware_wallet) — word-boundary matching (both).
- MemoryGuards.kt:20 — a missing/truncated bip39 wordlist caches an empty/partial set and seed detection fails open on every path — `require(words.size == 2048)`, fail closed (Kimi).
- MemoryGuards.kt:60 — the SeedPhrase result carries matchedWords; any logging/serialization leaks the phrase — drop the field or guarantee it is never persisted (DeepSeek).
- ScamRules.kt:44 — the remote_software alternative `\"support tool\"` embeds literal quotes and never matches — strip the quotes (both).
- VetoRules.kt:103 — reported-speech suppression lacks gerunds and breaks on colon/semicolon segment splits; "the caller is asking you to share your seed" and "asks you to: share your seed" are vetoed — add -ing forms, keep context across splits (both).
- VetoRules.kt:163,99,150 — negation is broken in both directions: clause-spanning "never" and stop/avoid/refuse-as-negation suppress real violations, while missing contractions and the small window veto legitimate warnings — reset the window at ,;—, drop non-adjacent negations, add n't forms, widen the window (both).
- VetoRules.kt:65,39 — "enter your seed on the" vetoes legitimate on-device restore (contradicting its own doc example) and unnegated warnings ("Remote support is a scam") are vetoed — narrow to website/online objects, add warning-context suppression (both).
- VetoRules.kt:137,122 — a blank extraPhrase matches at index 0 and vetoes all candidates; isSeedRelated substring-matches "words" ("passwords"/"keywords") — skip blanks, word-boundary the SEED_TOPIC_TERMS (both).
- ExtractiveAnswerTest.kt:124 — render() with an empty manifest still renders points, pinning the fail-open — flip to a fail-closed assertion together with the blocking fix (Kimi).
- AnswerPipelineTest.kt:46,77 + VetoRulesTest.kt:226,135 — vacuous tests: mustNot phrases already in the global VETO list, `|| backend == "local_model"` escapes the text assertion, the verdict prepend is never asserted, and the bridge-drop path is never exercised — non-global phrases, direct assertions, test the actual drop (Kimi).
- ModelImporterTest.kt:136 — no zip-slip/path-traversal, multi-gguf, or failed-import-preserves-previous tests — add malicious-entry tests (both).
- Test fixtures — manifest_missing_field/bad_default don't isolate the missing field (invalid sha256/minRamMb also absent) and the oversize test enshrines misleading Incomplete semantics — single-defect fixtures, distinct oversize result (DeepSeek).
- answer_step.py:168,150 — last_err unbound when call_flex returns empty twice (NameError escapes), and JSONDecodeError/KeyError from a malformed response bypass the LlamaServerError sail fallback — initialize last_err, wrap body parsing (both/Kimi).
- answer_step.py:131 — _strip_think splits on literal "NICALL"; real think blocks survive into user-facing answers — share extractive.py's regex-based stripper (both).
- answer_step.py:322,279,249 — veto or all-backends-failure in scam-check mode discards the deterministic verdict for a generic refusal, and must_include failures are ignored in both paths — ship verdict+warnings, enforce or loudly log must_include (Kimi).
- answer_step.py:368 — hybrid dedup appends "From the reviewed corpus" + Source with zero surviving bullets — require ≥1 bullet before merging (Kimi).
- extractive.py:84 + gen_manifest.py:12,21 + answer_step.py:388 — 40-char entry-id prefix collisions resolve to the wrong entry, key_points are admitted without reviewed=true, the manifest write is non-atomic, and corpus-path defaults differ between modules — collision detection, assert the flag, temp+os.replace, one shared default (both/Kimi).
- guardian_core.py:960,790 — compose builds constraints but never calls check_constraints, and scrub only appends a warning leaving "type your seed into…" intact (answer_step enforces downstream; in-pipeline enforcement is defense-in-depth) — enforce fail-closed and mirror the Kotlin veto-replace (both).
- guardian_core.py:855,360,470 — "mempool" refused as an offline term though T10/T11 teach it, raw substring keyword matching ("their"→heir, "feel"→fee), and the classify_mode tie-break contradicts the scam-check-priority comment — drop the term, word boundaries, fix the tie-break (both).
- guardian_core.py:87,945,545,940,81 — SLIP-39 shares undetected without literal "share N of M", detected secrets unredacted in context/eval logs, unreviewed entries indexed, a new Retriever built per compose call, and the hex detector both flags 64-hex txids and misses 128-hex seeds — add share detection, mask spans, skip reviewed=false, cache the retriever, bound hex lengths (Kimi/both).
- run_eval.py:60,50,82,95 — apostrophe synonyms dead (DASHES runs before SYNONYMS), seed-phrase synonym ordering leaves two normal forms, token presence by substring ("use" matches "because") inflates passes, and REFUSAL_RE is start-anchored — single-pass longest-first synonyms, word-boundary tokens, unanchored search (Kimi/both).
- run_eval.py:168 + make_report.py:20,88 — ERROR (infra) answers graded and counted as model failures, and stale generated claims persist ("all reviewed=false", "was 0/121") — tag/exclude infra errors, regenerate the report (both).
- judge_semantic.py:19,36 — the 'semantic_checked' skip flag is never written (re-runs re-judge everything), and the model answer is embedded raw in the judge prompt (injection can flip verdicts) — set the flag, delimit the answer and instruct the judge to ignore embedded instructions (both/Kimi).
- make_bundle.py:96,64,160 — cert pin verifies only the first signer, model hash is checked against the repo manifest rather than the APK-packaged one, and dev bundles are indistinguishable from gated ones (sidecar marker) — require exactly one signer, extract the manifest from the APK, embed/rename the marker (Kimi).
- make_bundle.py:36,132 — find_tool crashes when the build-tools dir is absent, and rmtree("dist") deletes previous releases and their SHA256SUMS — guard + PATH fallback, remove only current-tag outputs (both/Kimi).
- check_claims.py:35 — zero matched claim patterns prints OK, so reworded docs silently pass — error out when a doc matches nothing (both).
- scam_rules.py:90,5,126 — fake_official misses trezor/metamask/trust/exodus, "same rules as the shipped app" is unenforced (no harness test imports scam_rules), and no Unicode normalization before matching — extend the list, add per-rule tests + Kotlin↔Python golden vectors, NFKC-normalize (Kimi/DeepSeek).
- RELEASE.md:18 — runbook has no eval/danger-refusal gate step despite SAFETY_CASE calling it release-bound, and no post-sign verification that the release APK lacks INTERNET — add both as blocking steps (Kimi/DeepSeek).
- README-BUILD.md:99 — release grep scans only app/src/, missing build.gradle.kts/settings.gradle.kts where a dependency would be introduced — extend the grep (Kimi).
- README.md:63 — eval table omits gate status (topical 32/105 vs gate ≥50 OPEN) and DR/PJ/HO safety-set results are unpublished despite EVAL_SET §5 — add a gate-status column, publish the safety sets per release (Kimi).
- README.md:14,179 — "the model never touches secrets" is absolute while hex keys are unflagged and only the English wordlist is bundled; secret-stripping before the optional session log and scam-check paste-box coverage are unspecified — qualify the claim and specify both (Kimi).
- README.md:98 + SAFETY_CASE.md:31 + PRODUCT_SPEC.md:540 — veto rows/M4 still say "not wired until v0.2" though 69f23f0 shipped it — reconcile docs to code (both).
- README.md:48 + NOTICES:8 — model source unsloth vs Qwen/Qwen3-1.7B-GGUF; different repos mean different files/hashes — align attribution, download source, and pinned SHA-256 (both).
- README.md:176,40 — stale "best measured 31/105" vs table 32/105, and import free-space guidance ~1.1GB where zip+model needs ~2.2GB — update both (both/DeepSeek).
- README-BUILD.md:12,26 + PRODUCT_SPEC.md:195,270 — Gradle wrapper uncommitted with no dependency-verification metadata, and hardware-token signing plus "fingerprint published in README/About" stated as fact though neither is true for the alpha — commit the wrapper + verification-metadata.xml, describe actual signing (both).
- PRODUCT_SPEC.md:232,80 — §10 "pass gate before any release" vs the alpha-ships-failing rule, and "graceful degradation to a smaller model on 6GB" with only one pinned model — align to the beta gate, drop or specify the second model (both/Kimi).
- SAFETY_CASE.md:42 — evidence cites stale numbers (31/105, 19/40) and omits the OPEN topical gate — sync with EVAL_RESULTS (both).
- CORPUS_SOURCES.md:230,95,232 — "50 total" vs 51, directory policy mandates named vendor recs while prompt rule 5 forbids recommending paid vendors, and several provenance rows cite the entry itself — reconcile the count, scope rule 5 to exclude curated directory content, list real sources or mark in-house (both).
- CORPUS_SOURCES.md — River is UNVERIFIED/copyrighted reference-only against a permissive-only source policy — clarify the legal basis or remove derived entries (DeepSeek).
- INHERITANCE_WALKTHROUGH.md:172,8 — Day 6 "enter the seed words ONCE (into the verified software)" is unreconciled with Day 1's "never type the words into any app", and Rule #1's absolute "Bitcoin cannot be stolen by waiting" is false when key material is already exposed — explicit carve-out with hardware-restore-first, qualify Rule #1 (both/Kimi).
- EVAL_SET.md:46 — rubric demands ≥95% must_include / 0 must_not violations while the numeric gates are ≥50/105 and ≥28/40 — reconcile or cross-reference GATE_CONVENTIONS.md (Kimi).
- EVAL_RESULTS.md:400 — "the on-device 1.7B score… 31/105 (p7)" mislabels a host llama-server run as on-device (wording actually wrong, so flagged despite the disclosure carve-out) — relabel as host-local (both).
- EVAL_RESULTS.md:268 — the topical gate is GLM-5.3 judging GLM-5.3-generated answers — use a different-family judge or human spot-audit, and disclose (both).
- EVAL_RESULTS.md:262 — corpus fitted to the test: 14 GLM-drafted entries so all 122 must_include phrases exist in the corpus — build a held-out eval set (both).
- EVAL_RESULTS.md:282 — "complete reproducible run (temp=0)" is contradicted by 4 pass↔fail flips between identical runs — drop the claim, report variance bands (both).
- EVAL_RESULTS.md:420,411 — the T06 gate is declared "now met" on a hosted backend and the gate-binding 35/40 run publishes no itemized failures — label provisional until measured in the shipping configuration, publish the 5 failing items (Kimi).
- EVAL_RESULTS.md:415,321 — the Bitkey "score-neutral" A/B is confounded by a local→sail backend switch, and the judge's contradiction definition changed twice post-hoc while addenda compare across runs — re-run on one backend, freeze the judge prompt or stop cross-run comparisons (Kimi).
- EVAL_RESULTS.md:268,424 — strict grading is claimed for T03-T07/T09 but only T06 numbers appear, and zero veto-replaced answers are reported despite the "structurally unshippable" claim — report the slices or narrow the claim, publish veto fire/replace counts (Kimi).
- EVAL_RESULTS.md:235,122,10 — ScamRules "artificial urgency" fired on a first-person benign query, the root-cause heuristic labels opposite-advice and false-positive failures "directionally right," and the ≥80% synonym PASS can drop negation — third-party-message-only scam matching, add contradiction/FP triage classes, require negation/modal tokens verbatim (Kimi).
- EVAL_RESULTS.md:80 — "Purity: extractive 105/105 by design" hides LLM-assisted selection — clarify the label (DeepSeek).
- T10-*.json — two T10-prefixed files carry topic_ids ["T07","T08"], so T10 queries never surface them — retag or rename (Kimi).
- EVAL_RESULTS.md:292,5,339,294,428 — arithmetic/label cluster: Addendum 6 "21/145" vs column sum 25, log 23/40 vs table 20/40, "6/6" vs 5/6, 122 vs 121, "%%" typos, dangling "(Add. 11)", committed /opt path — one reconciliation pass (both).

## Confirmed — Deferred

- README.md:28,96 + PRODUCT_SPEC.md:2 + NOTICES:15 + README-BUILD.md:1 — stale v0.1.0/"skeleton" references — version/reference bump pass (both).
- README.md:54 — adb fallback's all-lowercase filename requirement unexplained — explain or remove (Kimi).
- PRODUCT_SPEC.md:170,160 — "E2E-encrypted" is meaningless for local storage, "Storage: nothing by default" is contradicted by always-stored walkthrough progress, and "BM25+embed" has no embedding model — reword/qualify/drop "embed" (Kimi).
- PRODUCT_SPEC.md:290,250,180,500 — superseded §9a bundling text, "DECIDED: Option A" risk row, Gemma candidate mention, F-Droid research table, legal-review timeline — one superseded-content cleanup pass (both).
- PRODUCT_SPEC.md:120 vs README.md:87 — corpus source lists diverge — unify (Kimi).
- PRODUCT_SPEC.md:265,308,100 — scam-checker "SHIPPED" under the v1.0 heading, "100% grounded" vs ≥95%, prompt-injection heuristics without a verification-status row — align (Kimi).
- NOTICES:6 — llama.cpp copyright "2013-2026" vs upstream "2023-2024" — correct (Kimi).
- EVAL_SET.md:2,50 — draft status/size stale and the plain-language metric is garbled — update/rewrite (both).
- LICENSE/NOTICES — corpus CC BY-SA 4.0 unmentioned, Mastering Bitcoin share-alike analysis, per-BIP license verification — counsel review pass (DeepSeek).
- RELEASE.md:22,36 — apksigner command omits `--ks-pass file:`, and the manifest hash has no independent cross-check before signing — align command, add a manual step (Kimi).
- ModelLocations.kt:~35 + ModelGate.kt:62 — size-only record acceptance means a same-size post-import tamper surfaces only as an opaque load failure — re-hash at load or document the tradeoff (both).
- ModelManifest.kt:~55,24,30 — fileName path separators unvalidated (trusted manifest), JSONException vs documented IllegalArgumentException, ciDownloadUrl in the offline asset — harden/assert-unused (Kimi).
- ExtractiveAnswer.kt:44,215,150 — URL_RE TLD gaps, identical retry prompt, bridge unescape only `\"` — broaden/vary/decode (Kimi).
- LlamaEngine.kt:31,14 — raw prompt lacks the Qwen3 chat template used in hosted evals (on-device selection quality unvalidated) and the model reloads per question — align template, keep resident (Kimi).
- ChatPlaceholderScreen.kt:45,30 — stale answerState after query change, coroutine not cancelled on navigate-away, dead MODEL_NAME — clear/cancel/delete (Kimi).
- AssetLoader.kt:14 + WalkthroughScreen.kt:44 — loadWalkthrough silently falls back to empty on asset error — surface an error state (Kimi).
- CorpusBrowserScreen.kt:20 — 51 JSON parses on the main thread in `remember` — move off the UI thread (Kimi).
- ChatPlaceholderScreen.kt:95 — secret detection silently disables Answer with no hint until "Run router" — show the refusal hint reactively (Kimi).
- MemoryGuards.kt:60,75 — dead `>= 12` disjunct; leet/homoglyph limits undocumented — clean up/document (both).
- CorpusRouter.kt:95 — retrieval scores keywords across all intent buckets — filter to bestIntent (Kimi).
- ScamRules.kt:70 — bare "unlock"/"100%"/"i noticed" false-positive-prone — require co-occurring context (both).
- HomeModeScreen.kt:52 — unknown route silently falls back to the corpus browser — exhaustive `when`/log (Kimi).
- VetoRules.kt:31,179 — DANGER_PHRASES parity with guardian_core.py is manual, and scrub is not idempotent (the injected warning contains "share your seed") — parity test + re-injection guard (Kimi).
- Test hygiene — racy tempDir/cleanup, missing think-block case, schema test tolerates 5 missing key_points, detect_secret combined entry untested, in_menu_order naming, corpus counted by file not entry, @Test regex counts comments/@Disabled, DATE regex, GUARDIAN_CORPUS CI override, unclosed file handles — one hygiene pass (both).
- answer_step.py/extractive.py — "fixed seed" claim without a seed field, unused imports/dead _veto_replace, comment mismatches ("Inheritance" vs walkthrough), zero-warning header, exception name in refusal, backend label detail, empty-answer fallthrough, menu-order docstring, duplicated _norm/_hash, panic-prefix over-trigger, 65536-token/1200s caps — cleanup pass (both).
- guardian_core.py — xpub mislabeled xprv_key, "what does.*mean" literal regex, entry-completion comment, _MNEMONIC_LENGTHS types, _slug 40-char collision, _detect_word_runs cost, _build_constraints over-breadth, REFUSAL_ERROR ordering — cleanup pass (both).
- run_eval/make_report/purity_report — resume re-runs paid items after compose, no-op _TOKEN.sub, inconsistent clean predicate, make_report KeyError on non-T06, extra_dir path, non-atomic writes, None guards — harness cleanup (both).
- make_bundle/check_claims/check_walkthrough — tag sanitization, dead assert, docstring sync, versionName-vs-tag check, N/M denominator capture, docs-list extension, dos/donts type+emptiness validation — script hardening pass (both).
- EVAL_RESULTS/corpus — secret-refusal claim without counts, extractive-aware judge doc, "1 red flag(s)" grammar and scaffolding leaks, ~40 null entry ids, model-floor inconsistency — polish pass (both).
- INHERITANCE_WALKTHROUGH.md:130,210 — no key-rotation scenario and unimplemented encrypted-notes export — add/mark planned (Kimi).
- AnswerPipeline/ChatPlaceholder minor — clipboard-clear advice on secret refusal, output length cap, parseSelection MAX_SELECTED unenforced, Exception-vs-Throwable in extractive answer — minor hardening (DeepSeek).
- CORPUS_SOURCES/INHERITANCE minor — source_url tappable-link rendering, Day 5 T11 "planned" vs active policy, keystore backup policy, change-control hook — reconcile/document (DeepSeek).
- WalkthroughScreen/MemoryGuards minor — dayComplete requires non-empty dos, WIF testnet prefixes — handle (DeepSeek).
- EVAL_SET.md:60,140 — must_include items for seed-backup handling lack a paired "do not photograph/type it" requirement — add the warning phrases (DeepSeek).

## Disagreement rulings

- Veto-in-chat-path severity (DeepSeek Blocking README:70 vs Kimi Should-fix README:98) — Should-fix: commit 69f23f0 wired the global veto set into AnswerPipeline; only the README/SAFETY_CASE/PRODUCT_SPEC rows are stale — the shipped-code gap is verdict display, ruled Blocking separately.
- ScamRules seed_request verb coverage (DeepSeek Blocking vs Kimi Should-fix) — Blocking: "No red flags" on canonical seed phishing is a silent wrong answer from the product's deterministic core; small diff, testable now.
- ScamRules "support tool" quoting (DeepSeek Blocking vs Kimi Should-fix) — Should-fix: one broken alternative in a multi-alternative rule; other patterns still fire.
- CorpusRouter substring matching (DeepSeek Blocking vs Kimi Should-fix) — Should-fix: misrouting degrades answer quality; veto and extractive fail-closed downstream.
- ScamCheckerScreen stale remember (DeepSeek Blocking, unopposed) — Should-fix: the deterministic verdict line updates; only the model explanation lags.
- MemoryGuards matchedWords retention (DeepSeek Blocking, unopposed) — Should-fix: no logging/serialization path shown; trivial defense-in-depth removal.
- guardian_core compose constraint non-enforcement (DeepSeek Blocking vs Kimi Should-fix) — Should-fix: answer_step's step-3 veto enforces constraints downstream; the in-pipeline check is defense-in-depth.
- guardian_core scrub warn-only (DeepSeek Blocking vs Kimi Should-fix) — Should-fix: the shipped Kotlin path veto-replaces; the prototype gap does not reach users.
- mempool refusal / classify_mode tie-break / out-of-corpus threshold (DeepSeek Blocking vs Kimi Should-fix) — Should-fix: all three fail closed or degrade quality only; veto/scrub still apply on every path.
- _strip_think "NICALL" (DeepSeek Blocking vs Kimi Should-fix) — Should-fix: downstream scrub/veto/constraints still process the leaked text; quality bug, not a safety bypass.
- sail_answer last_err NameError (DeepSeek Blocking vs Kimi Should-fix) — Should-fix: the outer handler converts it to an error refusal; no crash beyond that.
- scam_rules Unicode normalization (DeepSeek Blocking) — Should-fix: the scammer is not targeting this detector; verb coverage is the live risk.
- check_claims zero-match (DeepSeek Blocking vs Kimi Should-fix) — Should-fix: dev script fail-open with no shipped-artifact impact.
- make_bundle returncode gates (DeepSeek Blocking vs Kimi Should-fix) — Blocking: the no-INTERNET check is the core-guarantee gate on the shipped APK; a silent pass on tool failure is exactly the silent-failure class.
- Teaching-to-the-test (DeepSeek Blocking vs Kimi Should-fix) — Should-fix [DO NEXT]: real methodological flaw, but the remedy (held-out set) is larger work and hosted status is disclosed.
- README supply-chain verification (Kimi Blocking vs DeepSeek Should-fix README:30) — Blocking: spec §5's distribution adversary is unmitigated for the direct-install goal and the fix is small.
- README 2.2GB free-space (DeepSeek only) — Accepted Should-fix: zip plus extracted copy genuinely needs ~2× the model size.
- EVAL_SET must_include warning phrases (DeepSeek only) — Accepted Deferred: eval-content improvement, not a defect.

## Rejected findings

- DeepSeek chunk_01 README:100 "re-verify on each load or move to private storage" — v0.1.1 already imports into internal filesDir (private); the residual same-size-tamper case is the documented Deferred tradeoff.
- DeepSeek chunk_03 "navigate(Routes.CHAT) may not match chat?prefill={prefill}" — Navigation Compose treats query params as optional; speculative with no failure evidence.
- DeepSeek chunk_04 "Compose state written from Dispatchers.Default" — snapshot-state writes are thread-safe by design; recomposition is main-dispatched.
- DeepSeek chunk_04 "ModelGate ACTION_VIEW is an indirect network path" — user-initiated external-browser launch; no app network code or INTERNET permission involved.
- DeepSeek chunk_02 "scam card 'fully offline' claim false because the pipeline returns a model answer" — the scam-check model is local; the defect is verdict precedence (Blocking), not the offline claim.
- DeepSeek chunk_07 "judge_semantic sail_flex import violates no-network" — dev-only eval harness; hosted evals are disclosed (standing alpha-honesty carve-out).
- DeepSeek chunk_09 "cloud fallback claim violates core promise" — conditional on shipping; the fallback exists only in the dev prototype (covered in chunk_06); no app-scope cloud path shown.
- DeepSeek chunk_09 "6-8GB phone expected to deliver the local-1.7B number" — labeled as expectation; on-device-unmeasured is disclosed; wording not wrong.
- Kimi chunk_09 "Bitkey vendor entry conflicts with the no-endorsement rule" — standing decision: opinionated Bitcoin-only directory with named recs; the actionable fix is scoping prompt rule 5 (kept as Should-fix).
- DeepSeek chunk_01 "PRODUCT_SPEC mentions Gemma" — standing decision (no Gemma); the historical spec text folds into the superseded-sections cleanup, not a separate defect.
- DeepSeek chunk_09 duplicated block (same ~10 findings repeated with incrementing line numbers) — generation artifact; only the distinct findings were merged.

## Implementation plan

1. [DO NOW] Import + integrity fail-closed: ModelImporter.kt write the 4 GGUF head bytes before drain (byte-identical round-trip test); ExtractiveAnswer.kt and extractive.py fail closed on empty/missing key-point manifest; gen_manifest.py assert reviewed=true + atomic os.replace; flip ExtractiveAnswerTest.kt:124 to pin fail-closed.
2. [DO NOW] Deterministic scam verdict precedence: prepend `scan.verdict` and embed it in the system prompt (AnswerPipeline.kt); VetoRules-scrub explainVerdict and treat blank results as the error branch (ScamCheckerScreen.kt); distinct "answer failed safety check" refusal for vetoed answers.
3. [DO NOW] VetoRules bypass hardening: collapse whitespace/strip zero-width in normalize; "seed (phrase|words)" + full determiner regex variants; skip blank extraPhrases; reset the negation window at clause boundaries and drop stop/avoid/refuse-as-negation; add bypass regression tests.
4. [DO NOW] ScamRules coverage: add verify|validate|paste|tell|input verbs plus "within N hours"/frozen/suspended urgency, unquote "support tool", extend fake_official (trezor/metamask/trust/exodus) — mirrored in ScamRules.kt and scam_rules.py with a golden-vector parity test.
5. [DO NOW] Secret-detection hardening: MemoryGuards require exactly 2048 words (fail closed on missing/truncated list); re-run detectSecret inside runAnswer and AnswerPipeline; drop matchedWords from the refusal result.
6. [DO NOW] Release trust: make_bundle.py check aapt/apksigner returncodes and die on nonzero/empty; publish APK SHA-256 + signing-cert fingerprint in-repo; extend the no-INTERNET grep to gradle scripts; review-or-exclude the 3 unreviewed T10 entries, rebuild the manifest, and correct the 51/51 claims.
7. [DO NEXT] Safety content and gate integrity: INHERITANCE_WALKTHROUGH Day 6 rewrite (no-wipe precondition, hardware-restore-first, clean-machine requirement); judge_semantic must_not/refusal non-recoverability + partial-output fail-closed + T06 opposite-advice patterns; prototype scam/extractive local-only routing with downgrade disclosure; held-out T06 eval items; UI crash-hardening (ModelGate `!!`, busy-stuck, unload runCatching, manifest-error state).
8. [NEEDS USER] deleteSource default-off; River-derived entries legal basis (remove or document permission); scope prompt rule 5 against the opinionated directory; specify secret-stripping for the optional session log.

