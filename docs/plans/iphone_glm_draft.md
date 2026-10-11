# iPhone Mirror Plan

Framing: this plan assumes the sequencing gate holds — no iOS engineering begins until the Android beta bar in §0 is met and signed off. Everything below is decision-ready but flags facts that must be verified against current Apple documentation rather than assumed (consolidated in §8).

---

## 0. Beta gate (Android criteria before iOS starts)

**Decision: iOS starts only when ALL of the following are true, with thresholds pre-registered before eval runs (to avoid goalpost-moving).**

**A. Safety metrics — pass/fail, 100% required (non-negotiable):**
- ScamRules deterministic verdict suite: 100% correct verdicts (any miss = blocker).
- Veto layer fixture suite: 100% (no escapes).
- Verbatim safety block preserved in grounded chat answers: 100% of audited cases.
- Citation integrity: 0 fabricated source attributions in an audited sample (suggest N=100 chat answers).

**B. Quality metrics — thresholds (proposals; human must calibrate against rubric difficulty):**
- FINAL topical accuracy ≥ 60% (current: 31/105 ≈ 30% — **not met today; this gap is the actual project schedule**).
- T06 walkthrough strict ≥ 70% (current: 19/40 = 47.5%).
- Honest refusals: out-of-corpus questions produce refusal or hedged answers ≥ 95%, with 0 "confidently wrong" answers.

**C. Stability — measured honestly, without telemetry (do not add crash reporting to measure this; it would break the privacy story):**
- 4-week soak with ≥ 10 external alpha testers via GitHub Releases.
- 0 unresolved crashes (user-exported diagnostics, opt-in manual reports only).
- Model load + sha256 verify success ≥ 98% of reported sessions.
- No open P0/P1 issues.

**D. Product/process:**
- Corpus frozen at 50 entries; hash published.
- EVAL_RESULTS.md updated with beta numbers and addenda.
- F-Droid beta build live (if F-Droid remains the beta channel — open question §8).
- Android tagged (e.g., v1.0-beta); repo in maintenance mode (safety fixes only).
- Shared data assets extracted to a platform-neutral location (see §3) — the **only** permitted Android-side change at gate time, and it must be a file move with zero logic changes, followed by a full Android eval re-run to confirm no regression.

**E. Sequencing rule:** iOS work lives in a separate repo (or fully isolated branch). Android remains the behavioral source of truth until the iOS golden-fixture suite passes. No shared code coupling that could destabilize Android builds.

---

## 1. Model delivery & privacy story

**Decision: user-supplied model file, imported via the Files app (system document picker) or Finder/iTunes file sharing, with in-app sha256 verification against the published checksum. A "Get the model" screen whose only action opens the GitHub release page in Safari. The app itself contains zero networking code.**

**The honest iOS privacy problem:** iOS has no INTERNET permission to omit. Android's "physically cannot send anything" is OS-enforced; iOS cannot replicate that. The strongest available equivalent:
1. **Zero networking code**, enforced by a CI check that the compiled binary links no networking symbols/frameworks (CFNetwork, Network.framework, URLSession) — publish the check and its output per release.
2. App Privacy label: "Data Not Collected" (truthful given #1).
3. A documented airplane-mode test protocol users can run themselves.
4. iOS App Privacy Report (Settings → Privacy) as a user-verifiable signal of zero network activity.

The app's privacy page should state this difference plainly: Android = OS-enforced impossibility; iOS = verifiable absence of network code.

**Alternatives considered:**

| Option | Verdict | Reasoning |
|---|---|---|
| Bundle model in app (~1.2GB) | Rejected for v1; **retained as fallback** | Pros: zero friction, trivially reviewable, still zero network. Cons: every app update re-downloads ~1.2GB; model updates require app releases; the user-verified sha256 ritual is replaced by App Store code signing (arguably stronger, less transparent). Under the long-standing 4GB bundle limit; cellular cap is user-controllable on modern iOS (verify both). |
| On-Demand Resources | Rejected | App Store-hosted, but requires `NSBundleResourceRequest` (network code), system can purge resources, designed for game content. Breaks the no-network-code property. ~20GB total limit (verify). |
| In-app downloader (URLSession, pinned URL + checksum) | Rejected for v1 — argued both sides below | |
| **File import (chosen)** | ✅ | Closest analog to the adb sideload; actually *easier* than adb. User action supplies the file; app verifies hash; nothing is ever fetched by the app. |

**Both sides of the user-initiated download question:**
- *For:* user-initiated, checksum-pinned, no telemetry — the only data flow is a GET to a fixed URL, equivalent to the user visiting the release page in Safari. Reveals IP + timing to the CDN, nothing more. Much lower friction for a grieving audience.
- *Against:* it reintroduces networking code, so the claim shifts from "structurally cannot phone home" (verifiable by binary inspection) to "trust that the code doesn't." Since iOS already lacks the manifest-level guarantee, this would leave **no structural privacy claim at all**, plus future creep risk (update pings, analytics).
- *Resolution:* capture the friction benefit without the code — the Safari handoff. Button opens the release page; Safari performs the download into Files; user imports from there. The app binary still contains zero networking code.

**Operational details:** stream-copy with progress and hash-while-copying (avoid double disk usage); explicit low-storage error handling; model stored in Application Support, deletable from the model manager; in-app import guide with screenshots.

---

## 2. Inference engine

**Decision: llama.cpp compiled as a static arm64 library with the Metal backend, wrapped in a thin Swift bridge (direct mirror of LlamaBridge.kt → libllama_btcg.so), pinned to an exact commit recorded in the About screen.**

- **Build:** prefer llama.cpp's own Xcode/Swift Package support if mature (verify current state); fallback is CMake → xcodebuild with the library vendored into the Xcode project. Pin the same commit on both platforms where possible; record the hash in-app for debugging and eval parity.
- **Runtime parity:** identical parameters to Android — greedy sampling, `/no_think` + think-strip, same context length, and critically the **same prompt/chat-template strings** (shared as fixtures, §3).
- **Device floor:** estimated footprint ~1.6–2.2GB (weights + KV cache + Metal buffers). 4GB devices (iPhone 12/13 class) are **marginal** — per-app jetsam limits are not publicly documented and must be tested. 6GB devices are comfortable; 8GB has headroom. Provisional floor: A14/iOS 16 with reduced context; hard floor may need to be 6GB devices (iPhone 13 Pro / 14 and later). **Set empirically in the M1 spike.**
- **Thermal:** sustained generation warms the device; mitigations already inherent (short answers, greedy decoding). Document the behavior; no active thermal management in v1.
- **Background:** no background-modes entitlement needed (keeps review simple). iOS suspends the app almost immediately on switch — in-flight generation dies; persist chat state so relaunch resumes gracefully. Accept and document this.
- **Explicitly not mirrored in v1:** advanced JNI-level perf tuning (thread pinning, custom memory-mapping tricks, speculative decoding, quantization experiments). Stock llama.cpp Metal configuration; measure first, tune only if the floor device is unusable.

---

## 3. Pipeline port (shared core vs Swift reimplementation)

**Decision: Swift reimplementation + shared data and golden-test fixtures. Do NOT extract a shared C++/Rust core in v1.**

**Reasoning:**
- The pipeline is small and mostly deterministic logic. The real risk of a second implementation is *divergence*, and divergence is better controlled by shared behavioral fixtures than by shared source.
- A shared core means rewriting safety-critical Kotlin in C++/Rust **now** — port-introduced bugs in the veto layer or ScamRules are a worse risk than fixture-checked drift. Plus dual FFI on both platforms, three test suites, and slower safety iteration during exactly the period when safety fixes must ship fast.
- Shared core pays off with a third platform or sustained pipeline churn. Define a revisit trigger: third platform, or more than ~2 significant pipeline rewrites post-beta.

**What is shared (data, not code)** — extracted at gate time, the only Android-side change permitted:
- Corpus JSON (50 entries), First-7-Days checklist JSON, ScamRules patterns, veto rules, prompt templates, eval prompt sets.
- **Golden fixtures** generated from the Android implementation: retrieval top-k results, ScamRules verdicts, veto decisions, think-strip edge cases. Both platforms' test suites consume the same files.

**Parity contract:** deterministic layers (ScamRules, veto, think-strip, checklist) must match fixtures **exactly**. Retrieval is asserted on top-k membership with documented tolerance if embeddings are involved. Generated answer text is *not* required to be token-identical (Metal vs CPU numerics, §6).

**Open question that sizes this section:** what exactly "hybrid" retrieval is implemented with — Qwen embeddings via llama.cpp (free on iOS, same engine), a Kotlin-side lexical scorer (reimplement in Swift + fixtures), or a separate embedding model (which must then also ship/import on iOS). Need the answer before finalizing M3 effort.

---

## 4. UI port

**Screens (SwiftUI), mirroring the Android four plus iOS-necessary additions:**
1. Situation router — 4 cards (found something / someone contacted me / my own setup / want to learn).
2. First-7-Days walkthrough — deterministic JSON checklist, checkboxes persisted, per-step "Ask about this" handoff into chat.
3. Scam check — verdict-first UI (deterministic ScamRules verdict final; model only re-explains in simpler words).
4. Chat — grounded paraphrase + verbatim safety block, veto layer, honest refusals, fail-closed.
5. **New:** model manager — import, sha256 verification with progress, storage usage, delete, llama.cpp commit + model hash display.
6. **New:** onboarding/privacy explainer — why there's no network, how import works, the honest iOS-vs-Android privacy wording from §1.
7. **New:** about/disclaimers — not-financial-advice, source attributions, offline copy of eval results summary.

**Persistence:** UserDefaults is the direct SharedPreferences analog (checklist state, prefs); JSON files in Application Support for chat history. **Explicitly not Keychain** — nothing in this app is a secret, and Keychain survives app uninstall, which is a privacy *negative* for this audience (a grieving user's data should vanish when they delete the app). Default iOS file data protection is sufficient.

**Platform equivalents:** Compose → SwiftUI; SharedPreferences → UserDefaults; adb sideload → Files/Finder import; JNI bridge → Swift/C interop; JUnit → XCTest; F-Droid → TestFlight.

**Explicitly NOT mirrored in v1 (and why):**
- Advanced JNI perf tuning (§2) — measure stock Metal first.
- iPad-optimized layout — ship iPhone-only device family; simplifies review; SwiftUI makes adding iPad later cheap.
- Widgets, watch app, app extensions, share extensions — review surface and testing burden with no core-use-case value.
- Push or local notifications — nothing to notify; no entitlements keeps review clean.
- Multi-model support / model switching — one pinned model, one verified hash.
- In-app downloader — never (§1).
- Localization beyond English — match Android parity.
- Voice input, transcript export/share — optional cheap add later, decide at M4.

**Accessibility:** Dynamic Type and VoiceOver come nearly free with SwiftUI — worth doing properly given the audience (grieving, possibly older, stressed users).

---

## 5. Distribution & App Store strategy

**Account:** $99/yr Apple Developer Program. Individual vs organization is a human decision — org requires D-U-N-S + legal entity + lead time; individual is faster but shows a personal name on the listing.

**Path:** TestFlight internal (≤100 testers, no beta review) for engineering → TestFlight external (up to 10,000 testers, requires Beta App Review — lighter than full review; **builds expire after 90 days**, so set a refresh cadence) → App Store release (full review). The GGUF stays on GitHub Releases for beta and store users alike — identical import flow everywhere, so beta and production behave the same.

**Review risks, concretely:**
- **Guideline 2.1 completeness / 4.2 minimum functionality — the biggest risk.** The app is inert until a 1.1GB model is imported; a reviewer sees an import screen. Mitigations: (a) a built-in, clearly-labeled **demo mode** (canned transcripts + sample checklist, no model) so every screen is reviewable; (b) App Review notes explaining the design; (c) fallback if rejected: bundle the model (§1) — app becomes self-contained. Decide the demo-mode approach before M4; it also improves first-run UX.
- **Bitcoin content:** position as educational Q&A. Not a wallet, no private keys, no transactions, no trading, no payments — state this explicitly in review notes. "Educational information, not financial or legal advice" disclaimer on first launch and in the description. Category: Education. Age rating: 4+.
- **Sensitive topics (grief, death, scams):** educational framing; keep parity with any crisis-resources content Android has (verify whether it exists).
- **Privacy label:** "Data Not Collected" — truthful and defensible with zero network code.
- **Executable-code guideline:** GGUF is inert data, not code, and the app downloads nothing anyway; note this in review notes.
- **Export compliance:** no encryption APIs used (no network at all) → standard no-encryption answer (verify the current questionnaire).
- **Precedents to research before submitting:** local-LLM apps already on the store (e.g., Private LLM, LLM Farm — verify current status and how they handled model delivery and review).
- **EU alternative marketplaces:** exist post-DMA; not worth it (EU-only, fees, notarization overhead). Skip.

**Calendar honesty:** budget 1–3 review cycles and 2–4 weeks of calendar time for App Store iteration.

---

## 6. Testing & eval parity

- **Do not port the Python harness to Swift.** It drives llama.cpp, not the Android app, so it's already platform-independent — it remains the single source of truth for pipeline/model evaluation on desktop.
- **Golden fixtures run in XCTest on iOS:** deterministic layers (ScamRules, veto, think-strip, checklist) must pass 100%.
- **Numerics caveat (important):** Metal kernels produce different floating-point results than Android's CPU path, so token-level divergence is possible even with greedy sampling. Therefore: (a) parity is asserted at the *decision level* (retrieval top-k sets, verdicts, veto decisions), not token-exact text; (b) the real quality check is running the actual eval **on-device**: a hidden debug eval mode runs the FINAL/T06 prompt sets and exports JSON transcripts for offline grading with the same rubric. Confirm exactly how the Android on-device numbers were produced and replicate that procedure.
- **Acceptance criterion for iOS beta:** iOS FINAL/T06 scores within a pre-registered band of Android's (propose ±5 points absolute on FINAL topical). A larger gap triggers investigation (template bug, context truncation, numerics) before shipping.
- **Device test matrix:** 4GB / 6GB / 8GB iPhones — peak memory, tokens/sec, thermal over a 20-prompt session, jetsam behavior under memory pressure, and the 1.1GB import path (duration, low-disk handling).
- **Unit tests:** port the 22 Kotlin unit tests to XCTest, plus the fixture suite.
- **Manual QA script:** full walkthrough in airplane mode (proves no network needed); fresh-install flow; model re-verification; deleting the model mid-chat.

---

## 7. Milestones with rough effort (S/M/L), ordered

Sizing assumes one developer, moderate iOS experience: **S ≤ 1 week, M = 1–4 weeks, L > 4 weeks.**

| # | Milestone | Size | Notes |
|---|---|---|---|
| M0 | Beta gate review (Android) | S + 4–6 wk soak | Publish criteria, run evals, tag v1.0-beta, extract shared assets, freeze. |
| M1 | iOS feasibility spike | S–M | Dev account; build llama.cpp; run Qwen3 1.7B on 4/6/8GB devices; measure memory, tok/s, thermal; set device floor. **GO/NO-GO checkpoint.** |
| M2 | Shared asset extraction | S | Corpus/checklist/rules/fixtures to neutral directory; Android consumes from new path; full Android eval re-run confirms no regression. |
| M3 | Swift pipeline port | M | All layers + XCTest golden fixtures green. Sizing depends on the hybrid-retrieval answer (§3). |
| M4 | UI port + demo mode | M | 4 screens + model manager + onboarding; overlaps M3. |
| M5 | Model import + verification | S | Document picker + Finder sharing, stream-copy, sha256, storage management. |
| M6 | On-device eval + parity run | S–M | Eval mode, transcript export, compare vs Android within pre-registered band. |
| M7 | TestFlight internal → external | S + review calendar | Beta App Review; establish 90-day refresh cadence. |
| M8 | App Store submission | M + calendar risk | Metadata, privacy label, disclaimers, review notes, demo-mode polish; iterate on rejections. |
| M9 | Maintenance process | ongoing S | Coordinated llama.cpp upgrades across platforms; corpus updates flow to both. |

**Total: roughly 2.5–4 months of engineering after the gate, plus review calendar time.**

One flagged exception for the human to approve: M1 is a pure spike that touches no Android code and materially de-risks the plan. Default is strict sequencing (nothing before the gate closes); allowing M1 early is a reasonable exception if you want the GO/NO-GO answer sooner.

---

## 8. Risks & open questions for the human

**Risks:**
1. **The gate is far away.** Current FINAL ≈ 30% vs a proposed 60% bar — the Android quality work is the long pole; the iOS port is comparatively cheap and predictable. Plan accordingly.
2. **App Review completeness rejection** (inert-until-import app) — demo mode mitigates; bundling is the fallback, which slightly changes the checksum/privacy ritual. Decision needed.
3. **4GB-device instability** — the floor may have to rise to 6GB devices, shrinking the audience. Test in M1 before committing.
4. **Metal-vs-CPU numerics divergence** — decision-level fixtures + on-device eval mitigate; residual risk accepted and documented.
5. **Structurally weaker privacy claim on iOS** — no permission to omit; compensated by CI symbol checks, privacy label, and honest in-app wording. This is a real, permanent difference from Android.
6. **llama.cpp version drift between platforms** — pin commits; coordinate upgrades; re-run evals on both after any bump.
7. **Doubled maintenance burden** — corpus, model, and eval updates ×2 platforms; single-developer bus factor.
8. **Import friction for grieving users** — the audience is the least equipped to handle file operations. Safari handoff + in-app guide mitigates; the bundling debate is genuinely open and is ultimately a harm-reduction vs purity tradeoff only you can weigh.
9. **TestFlight 90-day expiry churn** and review-cycle variability.

**Open questions for the human:**
1. Confirm/calibrate the §0 gate thresholds against rubric difficulty (the 60%/70% numbers are proposals).
2. What exactly is hybrid retrieval implemented with (embeddings source)? This sizes M3.
3. Demo mode vs bundbling the model — decide before M4.
4. Device floor policy: support 4GB iPhones with reduced context, or exclude?
5. Individual vs organization developer account?
6. How exactly were the Android on-device eval numbers produced? (Needed to replicate on iOS.)
7. Is the F-Droid beta release a hard prerequisite of the gate, or just "planned"?
8. Corpus freeze window during the iOS port — how strict?
9. iPhone-only device family for v1 — acceptable?
10. **Verify before M4 (do not assume):** current App Store bundle-size limit (long-standing 4GB), current cellular-download-cap behavior, ODR exact limits, TestFlight external review depth in practice, current status/approach of precedent local-LLM apps, llama.cpp Swift/Xcode package maturity, and per-device jetsam headroom (empirical only).