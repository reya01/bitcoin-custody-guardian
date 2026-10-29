=== chunk_01.md ===
## Blocking
- README.md:3 — primary install flow has the user install the APK from inside full.zip with no verification step; the in-zip "checksums" are self-referential and no APK hash or signing-cert fingerprint is published in-repo (only same-channel on the release page), so the entire supply-chain trust story (spec §5 distribution adversary) has no second channel — publish APK SHA-256 + cert fingerprint in the repo and add a verify-before-install step.

## Should-fix
- README.md:63 — eval table omits gate thresholds/status (topical FINAL 32/105 vs gate ≥50 OPEN/failing; T06 35/40 vs ≥28 MET) — add a gate-status column so the unmet gate is visible next to the numbers.
- README.md:63 — no DR/PJ/HO (danger-refusal, injection, out-of-corpus honesty) results published, though EVAL_SET.md §5 requires per-release score publication and these are the 100%-pass safety sets — publish them per release.
- README.md:14 — "The model never touches secrets" is absolute, but raw 64-char hex keys are deliberately unflagged (line ~179) and only the English BIP39 wordlist is bundled (README-BUILD.md:50), so non-English seeds and hex keys reach the model — qualify the headline claim and list the gaps.
- README.md:98 — veto row labeled "Implemented" while stating per-question veto vocabulary "lands in the Android chat path in v0.2", contradicting the 2026-10-19 fix (69f23f0) — reconcile and mark Partial until the chat path actually ships it.
- README.md:48 — model source given as unsloth/Qwen3-1.7B-GGUF while NOTICES:8 cites Qwen/Qwen3-1.7B-GGUF; different repos mean different files/hashes — align attribution, download source, and the pinned SHA-256.
- README.md:176 — "hybrid … (best measured: 31/105 topical FINAL)" is stale; the table at line 68 shows 32/105 (p8sail) — update.
- README.md:179 — neither here nor spec §8 states that detected/refused secret input is excluded from the optional session log, despite the "never stores key material" promise — specify that secrets are stripped before any logging.
- README.md:179 — "both chat entry points route through a single guarded detector" never says whether the scam-check paste box is covered by detectSecret — state coverage explicitly.
- README-BUILD.md:99 — release checklist greps only `app/src/` for com.google/play-services/firebase/INTERNET, missing `app/build.gradle.kts`/`settings.gradle.kts` where such a dependency would actually be introduced — extend the grep to build scripts.
- README-BUILD.md:12 — Gradle wrapper jar intentionally uncommitted and no dependency-verification metadata, undercutting the reproducible-build/supply-chain claims — commit the wrapper and add `gradle/verification-metadata.xml`.
- README-BUILD.md:26 — "keys are generated once, stored on a hardware token" stated as fact while README.md:102 says hardware token is "Claimed (planned at beta)" — describe how the alpha is actually signed.
- PRODUCT_SPEC.md:232 — §10 "Pass gate before any release" contradicts EVAL_SET.md:4 ("beta release gate… alpha builds ship with scores published (likely failing)") and the shipped alpha with the topical gate OPEN — align §10 to the beta-gate rule.
- PRODUCT_SPEC.md:195 — §9.3 claims the signing fingerprint is "published on the project page, in the repo README, and inside the app's About screen" — it is published nowhere — mark as planned or publish it.
- PRODUCT_SPEC.md:80 — §4 promises "graceful degradation to a smaller model on 6 GB" but only one pinned model (1.7B) exists in the distribution — drop the claim or specify the second model.
- EVAL_SET.md:46 — rubric demands ≥95% must_include and 0 must_not_include violations, yet the published numeric gates are ≥50/105 and ≥28/40 with no cross-reference — reconcile or point to GATE_CONVENTIONS.md.

## Deferred
- README.md:28 — "answers on-device (v0.1.0-alpha, see Releases)" and the v0.1.0 citation in the verification table (line 96) are stale; current release is v0.1.1-alpha — bump.
- README.md:46 — default-on "delete the original file afterwards" destroys the user's only local copy of the APK + checksums (reinstall or re-verification needs a fresh 1.1 GB download) — default to off.
- README.md:54 — adb fallback demands an all-lowercase filename with no explanation — explain the requirement or remove it.
- PRODUCT_SPEC.md:2 — header still "v0.1.0-alpha SHIPPED … Last updated 2026-10-11", predating v0.1.1 and the 69f23f0 veto fix — bump.
- PRODUCT_SPEC.md:170 — "optional E2E-encrypted session log" — "E2E" is meaningless for local-only storage — reword to passphrase-encrypted local notes.
- PRODUCT_SPEC.md:170 — "Storage: nothing by default" is contradicted by README.md:187 (walkthrough progress always stored) — qualify the claim.
- PRODUCT_SPEC.md:160 — retrieval described as "BM25+embed" but no embedding model is bundled, pinned, or mentioned anywhere else — clarify or drop "embed".
- PRODUCT_SPEC.md:290 — §12 risk rows numbered 1,2,3,4,6,5,7,8 and row 1 still says "DECIDED: Option A (bundle)" pointing at superseded §9a — renumber and mark superseded.
- PRODUCT_SPEC.md:120 — §7 corpus source list (Bitcoin Core, BIPs, Sparrow, vendor docs, bitcoinops) diverges from README.md:87 (Mastering Bitcoin, Lopp, River, BTC Guide, BTC Sessions, Casa, bitcoin.org) — unify the lists.
- PRODUCT_SPEC.md:265 — "Scam-checker mode (SHIPPED in v0.1.0-alpha)" sits under the v1.0 heading — move to the shipped/MVP section.
- PRODUCT_SPEC.md:308 — §14.3 "100% grounded citations on the golden set" vs EVAL_SET.md's ≥95% must_include threshold — align.
- PRODUCT_SPEC.md:100 — prompt-injection heuristics are claimed in the §8 router but appear in no verification-status row in README.md — add a status entry.
- NOTICES:6 — llama.cpp copyright given as "2013-2026 The GGML Authors and The llama.cpp Authors"; upstream MIT notice reads "2023-2024 The ggml authors" — correct the attribution.
- NOTICES:15 — "see the v0.1.0-alpha release notes" is stale — reference the current release.
- README-BUILD.md:27 — "fingerprint is published in the signed release notes" — no release-note signing mechanism is described anywhere — clarify or drop "signed".
- README-BUILD.md:1 — title "(Android skeleton)" and "in this skeleton" (line 12) are stale for a shipped alpha — retitle.
- EVAL_SET.md:2 — still "Draft v0.1 · 2026-10-06" though the suite is operational at 145 items with 12 addenda and below the ≥200 target — update status and record current size.
- EVAL_SET.md:50 — plain-language check ("no sentence >20 words average >25th-percentile jargon without a definition") is garbled and unmeasurable as written — rewrite as a concrete metric.

=== chunk_02.md ===
## Blocking
- AnswerPipeline.kt:46 — ScamRules verdict is computed but never shown to the user nor included in the prompt (only `verdict != null` is used); the model is told a verdict block exists that it never sees, and on blank/vetoed model output the scam verdict is silently discarded — contradicts SAFETY_CASE M1 "verdict is primary and un-overridable" — prepend `scan.verdict` to the returned text and embed it in the system prompt.
- INHERITANCE_WALKTHROUGH.md:165 — Day 6 advises using "his old laptop, wiped/reinstalled" as the clean computer, directly contradicting Day 1's "do not delete anything on his computer" and risking destruction of wallet files, descriptors, and browser-bookmark evidence — require a separate/new computer or explicit full-backup-and-estate-settled precondition before any wipe.

## Should-fix
- AnswerPipeline.kt:37 — secret handling trusts caller-set `composed.isSecret`; the pipeline never scans `question` itself, so any caller that mis-sets the flag fails open and secret material reaches the model — run the secret detector inside AnswerPipeline as defense-in-depth.
- AnswerPipeline.kt:48 — mode is a raw string ("scam-check"/"walkthrough"); a caller typo silently disables the deterministic scam scan and routes scam text to the model — replace with an enum/sealed type.
- AnswerPipeline.kt:55 — no empty-retrieval guard: with zero chunks the model is invoked over an empty corpus and a 1.7B model may improvise despite rule 0 — short-circuit to the no-answer text when `chunks.isEmpty()`.
- AnswerPipeline.kt:62 — exception message/class name is echoed into user-facing text, leaking internal paths/native details that the error text itself invites the user to share — log-free generic detail only.
- AnswerPipeline.kt:85 — vetoed answers return OUT_OF_CORPUS_REFUSAL ("can't answer from the verified corpus"), misstating the real cause (unsafe model output) and violating the honesty principle — use a distinct "answer failed safety check" message.
- SAFETY_CASE.md:31 — M4 still claims veto enforcement "not yet wired… planned v0.2", contradicting the shipped fix (commit 69f23f0) and AnswerPipeline's "global set is always active" comment — update the safety case.
- SAFETY_CASE.md:42 — evidence cites stale eval numbers (hybrid 31/105, T06 19/40) vs current 32/105 and 35/40, and omits that the topical gate (>=50) is OPEN — sync with EVAL_RESULTS and state gate status.
- CORPUS_SOURCES.md:230 — provenance header says "50 total" entries while SAFETY_CASE M4 and the fact sheet say 51 — reconcile the count.
- INHERITANCE_WALKTHROUGH.md:172 — Day 6 "enter the seed words ONCE (into the verified software)" is never reconciled with Day 1's "do not type the words into any app", and no hardware-wallet-restore-first path is offered — add an explicit carve-out and prefer on-device restore.
- INHERITANCE_WALKTHROUGH.md:8 — Rule #1's absolute "Bitcoin cannot be stolen by waiting" is false when key material is already exposed (cloud photo, co-known seed, helper with access) and can induce harmful delay — qualify the claim.
- INHERITANCE_WALKTHROUGH.md:130 — walkthrough never covers the "someone else may already know the seed" scenario or rotating to newly generated keys after verification — add a day/branch for key rotation.
- CORPUS_SOURCES.md:95 — directory policy mandates named vendor recommendations (River) while AnswerPipeline.kt system rule 5 orders the model to "never recommend a specific paid product, vendor"; retrieved T11 chunks put the model in direct instruction conflict — scope rule 5 to exclude curated directory content or drop named recs.
- CORPUS_SOURCES.md:232 — multiple provenance rows cite the entry itself as its own source ("Learning Bitcoin…", "Never Photograph…", "Day One…", "Checksum First…"), making provenance circular/meaningless — list real sources or mark as in-house.
- RELEASE.md:18 — runbook has no eval/danger-refusal-suite gate step despite SAFETY_CASE §3 calling that suite "release-bound" and §5 requiring eval regeneration before release claims — add a release-blocking step.

## Deferred
- RELEASE.md:22 — documented `apksigner sign --ks alpha.keystore` omits `--ks-pass file:alpha.pass`, leaving the documented password file unused (interactive prompt) — align command with the setup section.
- RELEASE.md:36 — manifest is single source of truth for the model hash with no step to verify it against an independent upstream source before signing; one repo-side JSON edit swaps the hash everywhere — add a manual cross-check step.
- CORPUS_SOURCES.md:60 — "ELLiptic" typo for Elliptic.
- CORPUS_SOURCES.md:150 — entry field list contains `corpus_version` twice — dedupe.
- SAFETY_CASE.md:35 — M5 says the model is "sha256-verified by the user" but the app verifies against the pinned manifest — fix wording.
- AnswerPipeline.kt:94 — walkthrough warning dedup uses substring `it !in text`, so paraphrased warnings are re-appended verbatim (noise, possible double-text) — track appended warnings explicitly.
- INHERITANCE_WALKTHROUGH.md:210 — design note promises "export as encrypted notes (opt-in)" with no corresponding feature described elsewhere — mark as unimplemented or drop.

=== chunk_03.md ===
## Blocking
- ModelImporter.kt:~98 — GGUF branch hashes and counts the 4 peeked magic bytes but never writes them to `out`, so the imported file is missing its first bytes yet passes size+sha256 and renames into place — verified-corrupt model, engine never loads — add `out.write(head, 0, headFill)` before `drain(...)`.
- ExtractiveAnswer.kt:~197 — `manifest.isEmpty() || sha256(...) in manifest` turns the documented "integrity gate on the FINAL set" into a no-op whenever AssetLoader.loadKeyPointManifest silently returns emptySet (its default on any parse error) — fail closed on empty manifest instead of skipping the check.

## Should-fix
- ChatPlaceholderScreen.kt:~175 — runAnswer never re-checks secrets; enforcement is only the Answer button's enabled-state plus a TOCTOU read of `query` inside the coroutine, contradicting "secret detection FIRST on every path" — capture query at click time and re-run MemoryGuards.detectSecret inside runAnswer, refusing on hit.
- MainActivity.kt:~53 — a null manifest (parse failure) silently skips the model gate and the app "works anyway", contradicting ModelManifest's documented fail-closed explicit-error behavior — show an explicit manifest-error state.
- MainActivity.kt:~58 — gating the entire app on `gate.ready` contradicts FALLBACK_MODEL_MISSING's claim that checklists/scam-check/reading work without the model — provide a skip path for non-model screens (or verify ModelGateState offers one).
- ChatPlaceholderScreen.kt:~205 — `finally { engine.unload() }` can itself throw (e.g., UnsatisfiedLinkError when the native lib is absent), masking the fallback and crashing the coroutine — wrap unload in runCatching.
- ChatPlaceholderScreen.kt:~100 — if runAnswer throws before its internal try (loadCorpus/route are unguarded), `busy` stays true forever and the exception escapes `launch` → crash — try/finally around the call resetting busy.
- AssetLoader.kt:~24 — loadCorpus serves entries with reviewed=false as "human-reviewed" and silently drops per-file parse failures — filter `reviewed == true` and surface dropped-file count.
- AssetLoader.kt:~48 — readAssetLines never closes the reader (`readLines()` without `.use`) — wrap in `.use { it.readLines() }`.
- ModelPaths.kt:~18 — manifest asset reader never closed (`bufferedReader().readText()` without `.use`) — add `.use`.
- ExtractiveAnswer.kt:~48 — no in-app self-test that Kotlin norm()+sha256 matches gen_manifest.py byte-for-byte; any normalization drift silently fails 100% of extractive answers closed on-device — bundle one known hash vector and verify at startup.
- ModelLocations.kt:~120 — isReady/migrateLegacyModel can run a full ~1.1GB hash-copy with no thread guard, and is reachable from composition-time ModelGateState — enforce a background dispatcher.
- ChatPlaceholderScreen.kt:~80 — `MemoryGuards.detectSecret(context, query)` runs on every recomposition/keystroke; if it loads wordlists per call that is main-thread asset I/O — cache the wordlist and hoist the result.
- ChatPlaceholderScreen.kt:~25 — KDoc still says the model is sideloaded into Android/data/com.custodyguardian/files/models/, but v0.1.1 imports into internal filesDir/models — update the stale comment.
- ModelLocations.kt:~35 — no re-verify path for an existing valid file when verified.json is lost/corrupt; user is forced into a full re-import — add a verify-existing-file entry point.
- LlamaEngine.kt:~28 — ManifestEntry.minRamMb is parsed but never enforced before loadModel in any visible code path → OOM risk on low-RAM devices — enforce or document where it is checked.

## Deferred
- ModelLocations.kt:~35 — record+exact-size acceptance means a same-size post-import tamper of the internal model file is never detected (documented tradeoff) — consider periodic re-hash.
- ModelManifest.kt:~55 — fileName is not validated for path separators before use in `File(modelsDir, entry.fileName)` (manifest is bundled/trusted) — reject names containing `/`, `\`, `..`.
- ModelManifest.kt:~24 — ciDownloadUrl ships inside the offline app's bundled asset; CI-only — assert no app code path ever reads it.
- ExtractiveAnswer.kt:~44 — URL_RE TLD list misses .co/.me/.xyz/.app etc., so a bridge URL on another TLD bypasses the filter — broaden the pattern or invert to an allowlist.
- ExtractiveAnswer.kt:~215 — the retry reuses the identical greedy prompt, guaranteeing a duplicate inference with no new information — vary the prompt or drop the retry.
- LlamaEngine.kt:~31 — raw prompt lacks the Qwen3 chat-template tokens used in the prototype/hosted evals, so on-device selection quality is unvalidated against the eval claims — align the template.
- LlamaEngine.kt:~14 — load+unload per question re-loads a ~1.1GB model on every answer — keep the model resident between questions.
- ChatPlaceholderScreen.kt:~45 — stale answerState remains displayed after the query changes, and the answer coroutine is not cancelled on navigate-away — clear on query change, cancel on dispose.
- ChatPlaceholderScreen.kt:~30 — MODEL_NAME constant is dead (superseded by ModelPaths) and misleading — delete.
- ModelManifest.kt:~30 — structural type errors throw JSONException, not the documented IllegalArgumentException — wrap and rethrow.
- ModelImporter.kt:~75 — outer `lastReport` variable is write-only (dead store) — remove.
- AssetLoader.kt:~14 — loadWalkthrough silently falls back to an empty Walkthrough on any asset error, hiding corruption — surface an error state.
- CorpusBrowserScreen.kt:~20 — loadCorpus (51 JSON parses) runs on the main thread in `remember`; same in ChatPlaceholderScreen's "Run router" onClick — move off the UI thread.
- ExtractiveAnswer.kt:~150 — bridge unescaping handles only `\"`; literal `\n`/`\t` sequences render as raw backslash text — decode standard JSON escapes or reject bridges containing backslashes.
- ChatPlaceholderScreen.kt:~95 — when a secret is detected the Answer button is silently disabled with no explanation until "Run router" is pressed — show the refusal hint reactively.

=== chunk_04.md ===
## Blocking
- ScamCheckerScreen.kt:20 — `explainVerdict` output is shown raw with no VetoRules scrub, contradicting the fact-sheet claim that both free-form surfaces (scam-check guidance + extractive bridge) are veto-scrubbed; a quantized/corrupted model could restate "stop, it's a scam" as "looks safe, reply" to a grieving user — pass model output through VetoRules before assigning `simpler`.

## Should-fix
- ScamRules.kt:22 — seed_request regex misses the canonical phrasing "verify/validate your seed phrase"; concrete input "Please verify your 12-word seed phrase within 24 hours or your funds will be frozen" yields zero flags and the clean verdict — add verify/validate, "within N hours", and "frozen/suspended" patterns.
- ScamRules.kt:44 — remote_software alternative `\"support tool\"` embeds literal quote characters so it never matches unquoted text — strip the quotes.
- MemoryGuards.kt:20 — if `bip39_wordlist.txt` is missing/truncated, `loadWordlist` caches an empty/partial set and seed detection silently fails open on every path — `require(words.size == 2048)` and fail closed.
- ScamCheckerScreen.kt:150 — when the model file vanishes between check and click, `explainVerdict` returns "" and `simpler=""` renders a permanent empty explanation box with no retry — treat blank result as the error branch.
- ModelGate.kt:55 — `manifest.byId(manifest.defaultModelId)!!` crashes the gate screen if the bundled manifest lacks the default id — fail into Phase.ERROR with a message instead.
- ModelGate.kt:70 — legacy-migration copy (init, IO scope) races a user-initiated picker import: both write into `internalModelsDir` during the multi-minute copy window, risking a corrupted target file — disable the picker until migration resolves.
- ModelGate.kt:66 — fire-and-forget `CoroutineScope(Dispatchers.IO)` in `init` is not lifecycle-tied and posts to Main after the screen is gone — use a caller-supplied lifecycle/remembered scope.
- WalkthroughScreen.kt:25 — `keyFor` uses `step.take(60)`, so two steps sharing a 60-char prefix collide and checking one silently checks the other — key by day+step index or a full-string hash.
- CorpusRouter.kt:78 — substring keyword matching ("will"→inheritance, "pin"→hardware_wallet, "12"→seed_phrase) misroutes ordinary sentences to wrong corpus slices — match on word boundaries.

## Deferred
- ModelGate.kt:62 — post-import gate check is size-only (`resolve`, no re-hash); a post-import tamper surfaces only as an opaque engine load failure — re-verify hash at load or document the assumption.
- ModelGate.kt:48 — `deleteSource` defaults to true, irreversibly deleting the user's only downloaded copy after import — default off for an alpha.
- ScamCheckerScreen.kt:95 — `modelFile(context)` file I/O runs during composition and the 1.1 GB engine load blocks a Dispatchers.Default pool thread — hoist into remember/IO.
- MemoryGuards.kt:60 — `longest >= 12` disjunct is dead (subsumed by `>= 8`); English-only wordlist and leet/homoglyph evasion undetected — remove dead code, document limits.
- CorpusRouter.kt:95 — retrieval scores keywords matched in *all* intent buckets, not just the winning intent, pulling in off-topic entries — filter `matched` to `bestIntent`.
- WalkthroughScreen.kt:25 — progress keys embed step text, so any copy edit silently orphans user progress — use stable step IDs or a versioned prefs schema.
- WalkthroughScreen.kt:44 — `AssetLoader.loadWalkthrough` in `remember` has no error path; a corrupt asset crashes composition — wrap in runCatching with an error surface.
- ScamRules.kt:70 — bare "unlock", "100%", "i noticed" alternatives are false-positive-prone (fail-safe but noisy, may train users to dismiss flags) — require co-occurring context.
- HomeModeScreen.kt:52 — unknown route silently falls back to the corpus browser — make the `when` exhaustive or log the miss.

=== chunk_05.md ===
## Blocking
- VetoRules.kt:60 — inserting "phrase"/"words" between "seed" and "into/on" defeats every website-variant veto phrase and the matching DANGER_PHRASES, so "type your seed phrase into the site" passes both veto and scrub — add "seed phrase"/"seed words" variants or match via regex with optional "phrase"/"words".
- VetoRules.kt:118 — normalize() never collapses whitespace, so a newline/tab/double-space/zero-width char inside a phrase ("share your\nseed") splits it across segments in findViolations and misses the substring check in scrub — collapse \s+ to single spaces and strip zero-width/format chars before matching.

## Should-fix
- VetoRules.kt:103 — reported-speech suppression has no descriptive-context check: "Support asks you to share your seed — do it now" passes the veto — require a scam-verdict term in the segment or skip suppression when an imperative follows the phrase.
- VetoRules.kt:163 — negation window spans clause boundaries and counts punctuation tokens: "Never share your seed — share your seed with support" is suppressed by the first clause's "never" — reset the window at , — ; and drop punctuation-only tokens.
- VetoRules.kt:99 — "stop"/"avoid"/"refuse" count as negations, so "Stop reading and share your seed" is suppressed — remove them or require adjacency to the danger verb.
- VetoRules.kt:103 — REPORTED_SPEECH_RE lacks gerunds (asking/telling/wanting/instructing), so legitimate scam-check descriptions ("the caller is asking you to share your seed") are vetoed, contradicting the class doc — add -ing forms.
- VetoRules.kt:65 — "enter your seed on the" vetoes legitimate on-device restore ("enter your seed on the hardware wallet"), contradicting the doc comment's never-veto example — narrow these variants to website/online objects.
- VetoRules.kt:65 — determiner gap: "enter your seed on this/any/that site" evades all "enter your seed on …" variants (only a/the/their/online covered) — add remaining determiners or use a regex.
- VetoRules.kt:137 — a blank extraPhrase ("") matches at index 0 of every segment and vetoes all candidates — skip blank phrases in findViolations.
- VetoRules.kt:122 — isSeedRelated substring-matches "words", so "passwords"/"keywords" make non-seed text seed-related and scrub injects a Bitcoin seed warning into e.g. router advice — word-boundary the SEED_TOPIC_TERMS.
- ExtractiveAnswerTest.kt:124 — render() with an empty manifest set still renders points, i.e. an empty/corrupt bundled manifest silently disables the sha256 gate (fail-open) — pin fail-closed behavior for empty manifests.
- AnswerPipelineTest.kt:46 — veto test's mustNot phrase "enter your seed into the site" already contains the global VETO phrase "enter your seed into the", so the mustNotInclude path is never proven — use a phrase outside the global list.
- VetoRulesTest.kt:226 — caller_constraints test uses "share your seed", already a global VETO phrase, so extraPhrases handling is never isolated — pick a non-global constraint.
- VetoRulesTest.kt:135 — bridge_with_danger_advice_is_dropped only calls findViolations; it never exercises the ExtractiveAnswer bridge-drop path its name claims — test the actual bridge drop.
- AnswerPipelineTest.kt:77 — scam-check test's `|| r.backend == "local_model"` makes the text assertion vacuous, and the "deterministic verdict context" prepend in the test name is never asserted — assert the prepend directly.
- ExtractiveAnswerTest.kt:123 — test named "in_menu_order" asserts selection order (P4 rendered before P1) — rename the test or sort output by menu order.
- ModelImporterTest.kt:136 — no zip-slip/path-traversal ("../" or absolute-path entry) test; the importer's entry-name sanitization is unpinned — add a malicious-entry test.
- ModelImporterTest.kt:155 — oversize stream is asserted to report Incomplete, enshrining misleading "retry" semantics for a too-large file — assert a distinct oversize result.

## Deferred
- VetoRules.kt:31 — DANGER_PHRASES "1:1 port … keep the lists in sync" with prototype/guardian_core.py is manual; no parity test pins drift — add a sync test.
- VetoRules.kt:179 — scrub is not idempotent: the injected MANDATORY_WARNING itself contains "share your seed", so a second scrub pass double-injects — guard against re-injection.
- ModelImporterTest.kt:37 — tempDir() delete();mkdirs() is racy and no test cleans up, leaking temp dirs; the large_stream temp file also leaks on failure — use @Rule TemporaryFolder.
- CoreLogicTest.kt:168 — detect_secret_covers_both_detectors never exercises the combined entry point for the seed path (comment admits); the name overclaims — rename or test the combined entry.
- ExtractiveAnswerTest.kt:90 — parse_handles_fences_and_think_blocks input contains no think block; the think-block stripping path is untested — add a think-block case.
- SchemaUpgradeTest.kt:55 — corpus test tolerates up to 5 entries missing key_points, weakening the schema gate the fact sheet claims (401 points, all reviewed) — require 100%.
- ModelImporterTest.kt:136 — no test for zips with multiple .gguf files or for preserving a previously imported model when a new import fails — add both.

=== chunk_06.md ===
## Blocking
answer_step.py:287 — scam-verdict branch falls back to remote sail_answer with no disclosure note and even when backend="llama" was explicitly requested (main path re-raises) — pasted scam text silently leaves the machine — mirror the `if backend == "llama": raise` and append the same downgrade note.
answer_step.py:241 — extractive mode hardcodes remote sail_answer as the selection model_call; no local option and the returned backend label ("extractive_select") doesn't disclose it — route through llama_answer when backend is auto/llama.
extractive.py:58-72,219-224 — sha256 manifest gate fails open: missing/corrupt/empty manifest caches {} and _point_ok() returns True process-wide, silently disabling the verbatim-integrity gate backing the "approved text" claim — fail closed when the manifest is absent.

## Should-fix
answer_step.py:168-177 — sail_answer: `last_err` unbound when call_flex returns empty twice without raising → UnboundLocalError escapes instead of LlamaServerError — initialize last_err before the loop.
answer_step.py:150-158 — llama_answer wraps only URLError/TimeoutError/OSError; JSONDecodeError/KeyError from a malformed response bypass the `except LlamaServerError` sail fallback — wrap body parsing into LlamaServerError.
answer_step.py:131 — _strip_think splits on literal "NICALL"; real `<think>…</think>` blocks survive into user-facing answers (extractive.py uses the correct regex) — share one regex-based stripper.
answer_step.py:29 — GUARDIAN_LLAMA_URL accepts any host with no loopback validation; env can redirect all questions+corpus off-machine — assert hostname is 127.0.0.1/localhost/::1.
answer_step.py:322-334 — veto in scam-check mode replaces the whole candidate, deleting the deterministic scam verdict when only the model's body tripped must_not_include — on veto in scam mode ship verdict+warnings, not the generic refusal.
answer_step.py:279-317 — if all backends fail in scam-check mode the deterministic verdict (which needs no model) is discarded for a generic error refusal — return verdict-only on backend failure.
answer_step.py:249,322 — check_constraints must_include failures are ignored in both extractive and generative paths; mandated content can be silently absent — enforce or at least log loudly.
answer_step.py:368-376 — hybrid dedup only checks "- " lines, so a fully-deduped block still appends "From the reviewed corpus, verbatim:" + Source line with zero points — require ≥1 surviving bullet before merging.
extractive.py:84-90 — 40-char entry-id prefix keys collide silently (setdefault keeps first); a retrieved id can resolve to the wrong entry → mis-cited key points — detect collisions at index build and fail loud.
extractive.py:255,296 — "independent verbatim re-check against a fresh corpus load" reads the same module-level _CORPUS_CACHE — the check is vacuous; fix the docstring or actually reload.
gen_manifest.py:12-17 — key_points are admitted into the "approved" manifest without checking reviewed=true — assert the flag per entry before hashing.
gen_manifest.py:21 — non-atomic manifest write; a truncated file plus extractive's fail-open load silently disables the gate — write to temp then os.replace.
answer_step.py:388 vs extractive.py:28 — corpus-path defaults differ (prototype/corpus vs /opt/data/projects/custody-guardian/corpus); with GUARDIAN_CORPUS unset extractive resolves ids against a different tree than compose() retrieved → empty menus/fallbacks — use one shared default.

## Deferred
answer_step.py:10 — module docstring claims "fixed seed" but the llama payload has no seed field — add seed or drop the claim.
answer_step.py:20-27 — unused imports (subprocess, re) and unused SAIL_FLEX constant; sail_answer re-prepends sys.path on every call — clean up and insert once.
answer_step.py:192 — _veto_replace() is dead code (veto inlined at step 3) — delete.
answer_step.py:336 — comment says "Inheritance mode" but condition checks mode=="walkthrough" — align comment or condition.
answer_step.py:330 — veto replacement emits "Safety rules I must repeat:" header even when zero warnings were retrieved — guard the header.
answer_step.py:315 — user-facing refusal appends the exception type name — log it instead of showing it.
answer_step.py:291 — backend label "scam_rules+auto" doesn't record which engine actually answered — eval logs misattribute — set from the actual path taken.
answer_step.py:246 — an empty answer from extractive_answer would fall through to free-form backends, breaking the fail-closed contract — return _fallback on empty.
extractive.py:184 — docstring claims "menu-order render" but pinned warnings are appended at the end — fix doc or ordering.
extractive.py:46-51 + gen_manifest.py:8-9 — _norm/_hash duplicated across files; drift silently desynchronizes the manifest gate — share one implementation.
answer_step.py:337-339 — panic prefix triggers on "send"/"exchange"/"transfer", prepending "Do nothing yet" to in-scope how-to questions — narrow the trigger list.
answer_step.py:172 — sail_answer max_completion_tokens=65536 and timeout up to 1200s — harness cost/hang exposure — cap both.

=== chunk_07.md ===
## Blocking
- judge_semantic.py:33 — must_not_include violations are recoverable: `semantic_lenient` is computed without checking `must_not_include_hits`, so `passed_final = passed or semantic_lenient` passes forbidden-content items on topical slices, contradicting the documented "never recoverable" policy and inflating the release gate — require `not r['must_not_include_hits']` before setting semantic_lenient/passed_final.
- judge_semantic.py:80 — refusal-behavior failures are recoverable the same way: an item that answered instead of refusing still gets passed_final=True if phrases judge 'same' — require `r['refusal_ok']` for passed_final.
- judge_semantic.py:60 — partial judge output fails open: phrases missing from the LLM's 'judgements' are silently dropped, so 1 'same' of 3 required phrases yields semantic_passed/lenient=True — fail the item unless every mi_failed phrase has a verdict.

## Should-fix
- guardian_core.py:960 — compose() builds constraints but never calls check_constraints on candidate_answer; must_not_include rules are decorative in the reference pipeline — enforce constraints fail-closed (or veto) before returning the answer.
- guardian_core.py:790 — scrub() only appends a warning; an answer instructing "type your seed into…" survives intact — replace/remove the offending sentence or refuse, mirroring Kotlin VetoRules.
- guardian_core.py:855 — detect_refusal_kind lists "mempool" as an offline term, so "what is the mempool?" is refused although corpus T10/T11 teach it — drop mempool (and audit "latest"/"stock") from offline_terms.
- guardian_core.py:360 — keyword matching is raw substring: "their"→heir (T06+walkthrough), "feel"→fee (T10), "admit"→dm (scam-check), "opinion"→pin (T04), "studied"→died — match on word boundaries via regex.
- guardian_core.py:470 — classify_mode tie-break `-MODES.index(m)` lets learn win boosted ties (learn raw = scam raw + 1), contradicting the "scam-check outranks learn on ties" comment — make priority dominate the tie-break or fix the comment.
- guardian_core.py:87 — SLIP-39 shares are undetected unless the literal "share N of M" phrasing appears; the SLIP-39 wordlist is never used despite the docstring claiming share detection — add a SLIP-39 wordlist run detector.
- guardian_core.py:945 — detected secrets are not redacted: the raw question (with seed/key) flows into retrieved context, the answer step, and eval logs — mask detected spans before context assembly.
- guardian_core.py:955 — when refusal is set, compose still returns the stub/candidate answer; consumers must know to short-circuit — set answer to the refusal text (fail-closed).
- guardian_core.py:552 — entry_id uses only topic_ids[0]; the "+"-split in retrieve() is dead, so multi-topic entries lose prefer_topics boosting — join all topic ids in the eid.
- guardian_core.py:545 — add_corpus_dir indexes entries regardless of the reviewed flag; unreviewed drafts (per make_report, 14 reviewed=false entries existed) are served — skip reviewed=false entries.
- guardian_core.py:940 — compose() constructs a new Retriever (full corpus read + BM25 build) on every call — build once and reuse.
- guardian_core.py:163 — _detect_word_runs fuzz path runs edit-distance against all 2048 words per window token: O(windows×L×2048) — cap input length and/or prefix-index candidate words.
- guardian_core.py:81 — hex detector matches exactly 64 hex chars; 128-hex BIP39 seed material escapes detection — accept 64/128 (or ≥64 bounded) lengths.
- guardian_core.py:707 — DANGER_PHRASES are fixed substrings, trivially evaded by paraphrase ("wipe your device", "reset it first") — broaden to pattern families aligned with Kotlin veto rules.
- run_eval.py:60 — apostrophe synonyms are dead: DASHES translates `'` to space before SYNONYMS replacement, so the documented don't→do not etc. never fire — apply synonyms before punctuation translation.
- run_eval.py:50 — synonym ordering bug: "seed phrase"→"seed words" runs before "recovery phrase"→"seed phrase", leaving two different normal forms for the same meaning — single-pass longest-first replacement.
- run_eval.py:82 — match_include counts token presence by substring (`t in answer_norm`), so "use" matches "because"; inflates ≥80% synonym passes — match tokens on word boundaries.
- run_eval.py:95 — REFUSAL_RE is anchored to answer start; mid-answer refusals ("…I cannot answer that") are missed, mis-grading refusal_expected items — search the full answer.
- run_eval.py:175 — resume check `rec["id"] in have` happens after compose()+answer_step(), re-running (and re-paying LLM calls for) completed items — skip before compose.
- run_eval.py:168 — ERROR (infra) answers are graded and counted as FAIL in the same run's TOTAL — tag as infra-error and exclude from pass-rate.
- judge_semantic.py:19 — the 'already judged' skip guard checks a 'semantic_checked' flag that is never written; re-runs re-judge everything — set the flag after judging.
- judge_semantic.py:36 — the model answer is embedded raw in the judge prompt; adversarial answer text can instruct the judge to mark 'same' — delimit/quote the answer and instruct the judge to ignore embedded instructions.
- make_report.py:20 — ERROR answers are counted as category failures in EVAL_RESULTS.md, conflating infra errors with model errors — exclude or list separately.
- make_report.py:88 — stale claims persist in the generated report ("All entries reviewed=false pending human sign-off" vs fact sheet all-reviewed=true; "122 phrases (was 0/121)") — regenerate or annotate as historical.

## Deferred
- judge_semantic.py:14 — dev-time external API (sail_flex → GLM-5.3) with hardcoded sys.path /opt/data/scripts; fine per fact sheet but ensure harness is excluded from any shipped artifact — document/guard.
- judge_semantic.py:62 — stray key 'semantic_passed_lenient' is written but the lenient key used elsewhere is 'semantic_lenient' — remove the dead key.
- judge_semantic.py:45 — answer truncated to 1800 chars in the judge prompt; judged content may be cut — raise limit or log truncation.
- judge_semantic.py:28 — LLM is called with an empty REQUIRED PHRASINGS list when only must_not_include hits — skip the call.
- guardian_core.py:300 — xpub/ypub/zpub matches are labeled kind='xprv_key' with "extended private key" detail — relabel as extended key (safe direction, wrong text).
- guardian_core.py:455 — T10 keyword "what does.*mean" is a regex pattern used as a literal substring; never matches — remove or convert to regex.
- guardian_core.py:660 — entry-completion comment says "remaining claim chunks" but the code appends any missing section — align comment or filter to claims.
- guardian_core.py:96 — _MNEMONIC_LENGTHS is a set used as a Tuple default; min_len/max_len params of _detect_word_runs are unused — tidy types/signature.
- guardian_core.py:605 — _slug truncates titles to 40 chars; two entries sharing a 40-char prefix collide into one entry_id and aggregate together — add a short hash suffix.
- run_eval.py:64 — `_TOKEN.sub(lambda m: m.group(0), t)` is an identity no-op — delete.
- purity_report.py:18 — final_clean tests `"extractive" in backend+"deterministic"` while the purity count treats "deterministic" backends as clean; inconsistent clean definitions — share one predicate.
- make_report.py:11 — Failures section does `items[r["id"]]` against only the T06 file; results from other slices crash with KeyError — use .get with fallback.
- make_report.py:115 — "topical total" row sums every slice in eval/extra, including walkthrough slices — filter to topical slice ids or relabel.

=== chunk_08.md ===
## Blocking
- scam_rules.py:16 — seed_request misses the most common phishing verbs: "verify your seed phrase", "validate your wallet", "paste your 12 words", "tell me your words" all scan clean → app shows the "No red flags" verdict for active seed-phishing — add verify|validate|paste|tell|input alternatives and mirror in ScamRules.kt.

## Should-fix
- make_bundle.py:117 — no-INTERNET gate fails open: subprocess returncode unchecked, so aapt crashing/empty stdout (java failure, unparseable APK) silently passes the core-guarantee gate — capture result, die on nonzero exit or empty output (same for apksigner at :91).
- make_bundle.py:96 — cert pin compares only the first signer's SHA-256 (break after first match); a multi-signer APK with an extra unexpected signer passes the pin — require exactly one signer and verify all printed certs equal the pin.
- make_bundle.py:64 — model hash/size are checked against the repo's model_manifest.json, not the manifest packaged inside the APK; APK/manifest drift ships a model the app's sha256-gated import will reject — extract model_manifest.json from the APK (aapt/unzip) for the comparison.
- make_bundle.py:160 — dev bundles (--allow-missing-tools) produce a zip with identical name/content to a gated bundle; the .DEVGATES-SKIPPED marker is a sidecar that does not travel with the zip — embed the marker inside the zip or rename output to btcg-<tag>-UNGATED.zip.
- make_bundle.py:36 — find_tool crashes with FileNotFoundError when the build-tools dir doesn't exist instead of falling back to PATH lookup — wrap os.listdir in try/except FileNotFoundError.
- make_bundle.py:132 — shutil.rmtree("dist") deletes previous releases' zips and their published SHA256SUMS before building — remove only dist/stage and current-tag outputs.
- scam_rules.py:55 — pay_to_unlock matches the bare word "unlock" anywhere, so "How do I unlock my wallet after a firmware update?" is adjudicated a scam under the title "Send a small amount first" — require fee/send/test context around unlock.
- scam_rules.py:90 — fake_official lists ledger/coinbase/blockchain support but not trezor/metamask/trust wallet/exodus, the most-impersonated wallet brands — extend the vendor-impersonation list and mirror in Kotlin.
- scam_rules.py:5 — docstring claims "Same rules, same verdicts as the shipped app" but nothing enforces it, and none of the 44 harness tests import scam_rules — add per-rule positive/negative tests plus a Kotlin↔Python golden-vector parity test.
- check_claims.py:35 — if README/SAFETY_CASE reword the claim sentences, every regex matches nothing and the script prints OK — error out when zero claim patterns matched in a doc.

## Deferred
- make_bundle.py:59 — tag is unsanitized; a tag containing "/" or ".." writes outputs outside dist — validate tag against [A-Za-z0-9._-]+.
- make_bundle.py:69 — dead `assert entry is not None` immediately after die() on the None branch — remove.
- make_bundle.py:9 — docstring usage omits --allow-missing-tools and says gates run "if apksigner available" though the code dies when tools are missing — sync docstring with fail-closed behavior.
- make_bundle.py:117 — no check that the APK's versionName/versionCode matches the release tag — add aapt dump badging comparison.
- check_claims.py:35 — the "N/M" claim's denominator M is never captured or compared, so "85/200" passes — capture and check both numbers.
- check_claims.py:14 — @Test regex counts commented-out and @Disabled tests, inflating the "all passing" claim — strip comments and exclude @Disabled-annotated methods.
- check_claims.py:20 — Python count only globs prototype/test_*.py; tests added elsewhere are silently uncounted — glob recursively or document the constraint.
- check_claims.py:29 — only README.md and docs/SAFETY_CASE.md are checked; other docs carrying counts drift unchecked — extend the docs list.
- test_guardian_core.py:82 — `"K" * 0` dead expression in the WIF construction — remove.
- test_guardian_core.py:46 — loop body asserting assertIsNotNone(d) is vacuous; only the final run-length assertion does work — drop the loop.
- test_guardian_core.py:283 — DATE regex treats "currently"/"as of" as dating, so "currently the best wallet" passes the undated-ranking lint — require a 4-digit year for the exemption.
- test_guardian_core.py:322 — test named "matches_readme_claim" hardcodes >=50 while README claims 51 entries — parse the README number or assert equality.
- test_guardian_core.py:267 — GUARDIAN_CORPUS env override lets CI run neutrality tests against a different corpus than the shipped one — drop the override or pin it in CI.
- test_guardian_core.py:299 — corpus files opened without close() or encoding — use `with open(..., encoding="utf-8")`.
- check_walkthrough.py:36 — dos/donts are only checked for presence; a day with dos=[""] passes — validate steps are non-empty strings.
- scam_rules.py:75 — guarantee/unsolicited rules fire on benign text ("surely", "I noticed", "100% battery"); fail-closed is acceptable but tune or log to limit wolf-crying against the clean-verdict's credibility.

=== chunk_09.md ===
## Blocking
- T10-recognizing-bitcoin-scams-phishing-texts-to-f.json:1 — reviewed=false (also T10-sending-bitcoin-safely-units-fees-confirmatio.json, T10-social-engineering-scams-romance-fake-jobs-an.json) while the inventory header and fact sheet claim 51/51 reviewed — unreviewed scam- and sending-safety content ships in the alpha corpus — complete human review of the three files or exclude them and rebuild the sha256 manifest
- EVAL_RESULTS.md:339 — Addendum 9 "Corpus complete (every entry human-approved)" and Addendum 12 "corpus 51/51 reviewed" are false per the inventory (3 T10 entries reviewed=false) — correct the claims or the corpus
- EVAL_RESULTS.md:115 — T06-013 answer "Yes — powering them on just to look is okay" is affirmative advice opposite to the corpus day-one no-power-on rule yet hit zero must_not_include; the strict walkthrough gate has no reversal/contradiction check, so the 35/40 "gate met" claim is unreliable — add opposite-advice must_not_include patterns or a contradiction verdict to the T06 strict gate

## Should-fix
- EVAL_RESULTS.md:235 — T06-039: ScamRules "artificial urgency" fired on a first-person price/sell question, prepending "classic scam red flags / 1 red flag(s) detected" to a benign query — restrict scam matching to third-party-message mode or require scammer-directed patterns
- EVAL_RESULTS.md:122 — root-cause heuristic labels every failure "directionally right but target phrasing absent", including T06-013 (opposite advice) and T06-039 (scam false positive) — failure triage understates harm; add contradiction and false-positive classes
- EVAL_RESULTS.md:10 — "PASS-as-synonym if ≥80% of content tokens appear" can pass an answer that drops the negation (e.g. "plug it in yet" vs "do not plug it in yet") — require negation/modal tokens verbatim
- EVAL_RESULTS.md:268 — topical FINAL gate is GLM-5.3 judging answers generated by GLM-5.3 (Addendum 12 backend sail_glm53) — self-grading inflates the gate; use a different-family judge or human spot-audit and disclose
- EVAL_RESULTS.md:292 — Addendum 6 strict OVERALL "21/145" contradicts its own column sum (2+0+2+1+20=25) — fix the arithmetic
- EVAL_RESULTS.md:5 — progress log claims 23/40 and "Scam detection 6/6" but the headline table is 20/40 with scam_detection 5/6 — unexplained regression; label which run the table is or reconcile
- EVAL_RESULTS.md:282 — "Complete reproducible run (temp=0 everywhere)" is contradicted by Addendum 9's "4 items flipped pass<->fail between identical runs" (:355) — drop the reproducibility claim and report variance bands
- EVAL_RESULTS.md:400 — "the on-device 1.7B score, which remains 31/105 (p7)" mislabels a host llama-server run as on-device; nothing has run on a phone — relabel as host local-1.7B
- EVAL_RESULTS.md:400 — dangling reference "(Add. 11)" for the on-device re-measurement; no Addendum 11 exists — add it or fix the pointer
- EVAL_RESULTS.md:420 — T06 beta gate declared "now met" on a hosted sail backend explicitly "NOT the on-device score" — keep the gate open until measured in the shipping configuration
- EVAL_RESULTS.md:411 — the gate-binding 35/40 T06 run (p8sail) publishes no itemized failures or answer samples; only the superseded 20/40 run is itemized — publish the 5 failing items for auditability
- EVAL_RESULTS.md:415 — "the Bitkey entry is score-neutral" is confounded: the backend switched local→sail in the same comparison — re-run the A/B on a single backend
- EVAL_RESULTS.md:262 — corpus fitted to the test: 14 GLM-drafted entries created so "every one of the 122 must_include phrases now exists in the corpus" — walkthrough scores now measure phrase memorization; build a held-out eval set
- EVAL_RESULTS.md:321 — judge's contradiction definition changed twice post-hoc (:321, :357) while addenda keep comparing scores across those runs — freeze the judge prompt and re-run, or stop cross-run comparisons
- EVAL_RESULTS.md:268 — policy gate claims strict grading for "walkthrough slices (T03-T07, T09)" but only T06 walkthrough numbers appear anywhere — report the other slices or narrow the claim
- EVAL_RESULTS.md:424 — backend labels sum to 145 with zero veto-replaced answers despite Addendum 7's "wrong advice structurally unshippable" — report veto fire/replace counts per run
- T10-recognizing-bitcoin-scams-phishing-texts-to-f.json:1 — file prefixed T10 but topic_ids ["T07","T08"] (same for T10-social-engineering-scams-romance-fake-jobs-an.json) — topic routing may never surface them for T10 queries — retag or rename
- T09T05T08-bitkey-2of3-multisig.json:1 — vendor/product-specific entry conflicts with the corpus's own "never picks a specific product… no endorsements" rule (quoted in T06-040) — reframe as category guidance or document the exception

## Deferred
- EVAL_RESULTS.md:262 — "122 must_include phrases … (was 0/121)" — 122 vs 121 mismatch
- EVAL_RESULTS.md:294 — double-percent typos ("~28-30%%", "~50%%", "~60-65%%") in Addendum 6
- EVAL_RESULTS.md:428 — absolute host path /opt/data/work/answerer/ committed in docs
- corpus inventory — ~40 of 51 entries have "id": null — populate ids or document that title is the key
- EVAL_RESULTS.md:5 — model-floor numbers inconsistent: log "GLM-5.3 67-77%" vs Addendum 6 "~60-65%" (:294)
- EVAL_RESULTS.md:197 — user-facing artifacts: "1 red flag(s)" grammar, inline "(section claim.10)" ids (:55), and "The corpus states that…" scaffolding (:76) leak into answers
- EVAL_RESULTS.md:367 — "extractive-aware judge" (13/105) is a second, more lenient judge used only for extractive mode — document that it can never serve as a gate