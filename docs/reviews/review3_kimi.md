# Document Audit vs. Fact Sheet

## README.md

**1. [Stale] "walkthrough slice 21/40 strict"**
Fact sheet: T06 walkthrough strict is **19/40**, and that's the *best yet* — 21/40 was never achieved.
→ Fix: "walkthrough slice 19/40 strict".

**2. [Stale/mislabeled] "topical slices ~30% on the by-meaning gate"**
Topical by-meaning is 24/105 ≈ **23%**. ~30% is the *overall* figure (43/145). The README attaches the overall number to the topical gate.
→ Fix: "topical slices 24/105 (~23%) on the by-meaning gate; overall 43/145 (~30%)".

**3. [Stale] "retrieval coverage is 88%"** (appears twice: here and "turns 88% retrieval coverage into…")
Fact sheet: cat_coverage 103/122 ≈ **85%**.
→ Fix both to "~85% (103/122)".

**4. [Overclaim] "the right guidance is found for nearly every question"**
85% is "most," not "nearly every" — 1 in 7 questions misses.
→ Fix: "the right guidance is found for ~85% of questions, so the remaining gap is mostly answer quality."

**5. [Stale] "Android app compiles with 13/13 JVM unit tests green"**
Fact sheet: **19/19** (13 CoreLogicTest + 6 AnswerPipelineTest), assembleDebug **and** assembleRelease green, and libllama_btcg.so verified inside the APK. "Compiles" badly undersells a build with working on-device inference.
→ Fix: "Android app builds green (debug + release) with on-device llama.cpp inference packaged (arm64); 19/19 JVM unit tests and 41/41 Python harness tests green."

**6. [Stale] "Curated corpus of 20+ reviewed entries"**
Fact sheet: **30+** entries, all reviewed=true.
→ Fix: "30+ reviewed entries".

**7. [Overclaim] "Verified by monitoring tools: no traffic, ever."**
No such verification appears in the fact sheet — only the absence of the INTERNET permission.
→ Fix: delete, or replace with "Enforced by the OS: without the INTERNET permission the app cannot open a socket." (If a monitoring run exists, name the tool and date.)

**8. [Overclaim] "every substantive answer is drawn from a curated, versioned knowledge corpus and cites its source"**
With 85% retrieval coverage and ~30% delivery, "every" is false; citation display in the UI is not confirmed anywhere in the fact sheet.
→ Fix: "Answers are grounded in a curated, versioned corpus; when retrieval can't ground a question, the app is designed to refuse rather than guess."

**9. [License misstatement] "…(Mastering Bitcoin 3rd ed., Lopp's security index, Casa, bitcoin.org) with license discipline (CC-BY-SA 4.0 attribution)"**
The parenthetical implies all four are CC-BY-SA. Only Mastering Bitcoin is; Lopp is per-site, Casa/bitcoin.org unrecorded.
→ Fix: "with per-source license discipline (Mastering Bitcoin is CC-BY-SA 4.0; per-source status in docs/CORPUS_SOURCES.md)".

**10. [Contradiction by omission] README never mentions that v0.1.0-alpha is live or that on-device inference shipped.**
The release notes describe a working on-device app; the README reads like the app is merely compiling. Also: link label `docs/EVAL_RESULTS.md` points to `prototype/EVAL_RESULTS.md` — fix label or path. And "Current reproducible results" overstates: scores moved between runs (21→19) and the judge is a hosted model — say "Current results (all runs at temperature 0)".

## docs/releases/v0.1.0-alpha.md

**11. [Missing disclaimer]** This is the install-time document and it has **no** "educational only / not financial, legal, or tax advice / no warranty / funds at risk" block. For an inheritance-focused custody tool this is the single most important place for it. The README's "not yet safe to rely on" covers reliance, not advice/liability.
→ Add: "This app is an educational aid. It is not financial, legal, tax, or estate-planning advice. No warranty; you alone are responsible for custody decisions. For inheritance matters, consult a qualified professional."

**12. [Security gap] No integrity hash for the model download.** You publish the APK SHA-256 but send users to Hugging Face for a 1.1 GB GGUF with no checksum — inconsistent for a security product.
→ Add the expected GGUF SHA-256, plus model attribution (Qwen3, Apache 2.0).

**13. [Stale framing] "Known limitations"** doesn't disclose that published eval numbers were produced on a **host** (llama-server / hosted judge), not on-device; the 8 GB phone floor test is pending.
→ Add one line: "Published scores are from host-side runs; on-device numbers pending the 8 GB phone test."

**14. [Inaccuracy] "(or use a file manager to move it into that folder)"** — Android 11+ scoped storage blocks most file managers from `Android/data/`. adb is the reliable path (or add SAF import). Also state the Android 10+ (minSdk 29) requirement.

## docs/CORPUS_SOURCES.md

**15. [License risk] "original plain-language rewrites (in-house, MIT corpus license) grounded in their material"**
If any entry is an *adaptation* of Mastering Bitcoin (CC-BY-SA 4.0), ShareAlike requires that entry be CC-BY-SA — MIT is not a compatible license. "Grounded in" + "rewrite" sits exactly in the gray zone.
→ Fix: either state explicitly that entries are original works using facts/procedures only (not adaptations), or mark MB-derived entries CC-BY-SA 4.0 per entry.

**16. [Attribution gaps] "with license status recorded below"** — but no status is recorded for **bitcoin.org, the BIPs, vendor docs, or Casa**.
→ Add a license or "copyrighted, reference-only" line for each; note BIPs carry per-BIP license headers.

**17. [Clarity] BIP list (~18 BIPs) vs. "Snapshots of the four used directly"** — name the four or reconcile the list.

**18. [Gap] No third-party code/model notices anywhere.** llama.cpp/ggml are MIT (notice retention required for the bundled static libs); Qwen3 is Apache 2.0. This doc is corpus-scoped, so add a `NOTICES` file or README section.

## docs/PRODUCT_SPEC.md (excerpt)

Consistent with the README's principles ("Fail closed," "No answer beats a wrong answer"). One nit: ensure the canonical refusal string is byte-identical across spec, README, and the app's UI copy.

## Prioritized fix list

1. README eval numbers: 21/40→**19/40**; "topical ~30%"→"topical 24/105 (~23%), overall 43/145 (~30%)"; 88%→**~85% (103/122)** both places.
2. README counts: 13/13→**19/19**; 20+→**30+** entries; add "debug+release green, on-device llama.cpp packaged in APK."
3. Delete or evidence "Verified by monitoring tools: no traffic, ever" → OS-enforcement wording.
4. Reword "every substantive answer…cites its source" / "nearly every question" to match 85% coverage + refuse-on-miss.
5. Add disclaimer block to README **and** release notes: educational only; not financial/legal/tax/estate advice; no warranty; user bears custody risk.
6. Fix CC-BY-SA scope: README parenthetical applies only to Mastering Bitcoin; resolve corpus-MIT vs. ShareAlike per entry.
7. Complete license statuses (bitcoin.org, BIPs, vendors, Casa); reconcile BIP list with "four used directly."
8. Add NOTICES for llama.cpp/ggml (MIT) and Qwen3 (Apache 2.0); publish GGUF SHA-256 in release notes.
9. Release notes: disclose host-side eval provenance (on-device pending); fix file-manager instruction for Android 11+; state Android 10+ requirement.
10. Fix EVAL_RESULTS link label/path; drop or qualify "reproducible" (hosted judge, scores moved between runs).