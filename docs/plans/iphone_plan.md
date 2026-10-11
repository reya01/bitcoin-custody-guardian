# iPhone Mirror Plan (combined)

## 0. Beta gate (Android criteria before iOS starts)

**Decision: iOS starts only when ALL of the following are true, with thresholds pre-registered before eval runs (to avoid goalpost-moving).**

**A. Safety metrics — pass/fail, 100% required (non-negotiable):**
- ScamRules deterministic verdict suite: 100% correct verdicts (any miss = blocker).
- Veto layer fixture suite: 100% catch on seeded adversarial set; ≤5% false vetoes on benign-prompt set (grieving users must not get over-blocked).
- Verbatim safety block preserved in grounded chat answers: 100% of audited cases.
- Citation integrity: 0 fabricated source attributions in audited sample (N=100 chat answers).

**B. Quality metrics — thresholds (proposals; human must calibrate against rubric difficulty):**
- FINAL topical accuracy ≥ 50/105 (up from 31/105) — **not met today; this gap is the actual project schedule**.
- T06 walkthrough strict ≥ 28/40 (up from 19/40).
- Honest refusals: out-of-corpus questions produce refusal or hedged answers ≥ 95%, with 0 "confidently wrong" answers.
- Human audit: 30-answer sample, two reviewers, ≥90% "safe and useful."

**C. Stability — measured honestly, without telemetry (do not add crash reporting to measure this; it would break the privacy story):**
- ≥10 external alpha testers, ≥3 weeks, zero open P0/P1 crash/hang reports.
- Opt-in, user-initiated local crash-log export (share sheet) so offline posture is preserved but crashes are reportable.
- Cold start ≤5s and first token ≤5s on a defined floor Android device.
- Model load + sha256 verify success ≥ 98% of reported sessions.

**D. Product/process:**
- Corpus frozen at 50 entries; hash published.
- EVAL_RESULTS.md updated with beta numbers and addenda.
- Signed APK, published sha256, all tests green in CI. [corrected 2026-10-11: plan-time counts were 22+41; the check_claims-enforced counts are now 26 Kotlin + 44 Python = 70]
- Android tagged (e.g., v1.0-beta); repo in maintenance mode (safety fixes only).
- **Golden-vector exporter merged** (see §3) — the gate itself produces the artifact iOS needs.
- Repo layout: `/shared` created holding corpus JSON, prompt templates, checklist JSON, eval sets, golden vectors; Android references it via a one-line sourceSet change. This is the *last* Android-touching change before the freeze.

**E. Sequencing rule:** iOS work lives in a separate repo (or fully isolated branch). Android remains the behavioral source of truth until the iOS golden-fixture suite passes. No shared code coupling that could destabilize Android builds.

**Freeze rule once the gate passes:** Android gets crash-fixes only. Any pipeline change requires regenerating golden vectors and shipping both platforms in the same release train. iOS work never blocks an Android hotfix.

**Permitted pre-gate (no codebase impact, human may veto):** Apple Developer enrollment ($99/yr), App Store Connect record skeleton, hardware purchases (a Mac is required; plus one 4GB-RAM iPhone for floor testing).

---

## 1. Model delivery & privacy story (decision + rejected alternatives)

**Decision: v1 ships with zero networking code. The model arrives by user-initiated file import** — Safari download from the pinned GitHub Release URL → "Open in…" / document picker; or Finder File Sharing / AirDrop from a Mac. In-app sha256 (CryptoKit) checked against the hash embedded in that build and published in the repo. Same GGUF bytes as Android — one artifact, one hash, one supply chain.

**The honest iOS privacy problem:** iOS has no INTERNET permission to omit. Android's "physically cannot send anything" is OS-enforced; iOS cannot replicate that. The strongest available equivalent:
1. **Zero networking code**, enforced by a CI check that the compiled binary links no networking symbols/frameworks (CFNetwork, Network.framework, URLSession) — publish the check and its output per release.
2. App Privacy label: "Data Not Collected" (truthful given #1).
3. A documented airplane-mode test protocol users can run themselves.
4. iOS App Privacy Report (Settings → Privacy) as a user-verifiable signal of zero network activity.

The app's privacy page should state this difference plainly: Android = OS-enforced impossibility; iOS = verifiable absence of network code.

**The in-app download question, argued both sides:**
- *For:* Since permission-absence doesn't exist on iOS anyway, the marginal purity loss is small. A user-tapped, checksum-pinned fetch from a fixed URL is inbound-only, sends nothing, has no telemetry — data-flow-equivalent to the Safari path, with far better UX for a grieving-beginner audience, and it lets App Review test the app easily.
- *Against:* It downgrades the guarantee from **structural** ("physically cannot") to **attestational** ("doesn't — audit us"). It adds TLS/URL supply-chain surface, invites scope creep (update checks, telemetry temptation), and weakens the project's single most trusted claim.
- *Resolution:* import-only for TestFlight; measure import-flow completion and confusion in beta feedback; **re-decide at M8** before App Store submission. Pre-design the download path so adding it is a small diff, not a redesign.

**Rejected alternatives:**

| Option | Verdict | Reasoning |
|---|---|---|
| Bundle model in app (~1.2GB) | Rejected for v1; **retained as fallback** | Pros: zero friction, trivially reviewable, still zero network. Cons: every app update re-downloads ~1.2GB; model updates require app releases; the user-verified sha256 ritual is replaced by App Store code signing (arguably stronger, less transparent). Under the long-standing 4GB bundle limit; cellular cap is user-controllable on modern iOS (verify both). |
| On-Demand Resources | Rejected | Apple-hosted (no third-party server), but requires a runtime network fetch, assets are purgeable and must be re-fetched, model updates are tied to app versions, and the story degrades to "talks only to Apple." Breaks the no-network-code property. ~20GB total limit (verify). |
| In-app downloader (URLSession, pinned URL + checksum) | Deferred to M8 | See argued-both-sides above. |

**Operational details:** stream-copy with progress and hash-while-copying (avoid double disk usage); explicit low-storage error handling; model stored in Application Support, deletable from the model manager; in-app import guide with screenshots. Users may download to iCloud Drive, where the file can be evicted (dataless). On import, **copy into the app container** and set `isExcludedFromBackup` — a 1.1GB model must not inflate iCloud backups (App Review checks this).

---

## 2. Inference engine

**Decision: llama.cpp at the same pinned commit as Android, built as an XCFramework via CMake (device + simulator slices), Metal enabled, called through its existing C API from a thin Swift actor wrapper.** iOS integration is strictly easier than JNI here — `llama.h` is C, so Swift imports it via a module map; no bridging-header gymnastics. The wrapper owns the generation loop, so cancellation is a flag checked between tokens.

- **Build:** prefer llama.cpp's own Xcode/Swift Package support if mature (verify current state); fallback is CMake → xcodebuild with the library vendored into the Xcode project. Pin the same commit on both platforms where possible; record the hash in-app for debugging and eval parity.
- **Runtime parity:** identical parameters to Android — greedy sampling, `/no_think` + think-strip, same context length, and critically the **same prompt/chat-template strings** (shared as fixtures, §3).
- Full GPU offload (`n_gpu_layers` = max); unified memory means no copy penalty.
- Context 2048 to match Android; KV cache at f16 ≈ ~230MB for this model at 2048 ctx (measure; q8 KV and/or 1024 ctx are the levers if tight). Total footprint estimate ~1.4–1.6GB.
- **Device floor:** iOS 16+, **4GB RAM minimum** (≈ iPhone 11/12/13), 6GB+ recommended. Two honest caveats: per-app memory ceilings are undocumented and device-dependent (empirically ~half of RAM — **measure in M1**), and `UIRequiredDeviceCapabilities` has **no RAM key** (verify), so the store can't filter — gate at runtime with a graceful, honest message. If the M1 spike shows 4GB is unreliable, the floor rises to 6GB and we say so.
- **Thermal:** sustained generation warms the device; mitigations already inherent (short answers, greedy decoding). Watch `ProcessInfo.thermalState` and degrade with an honest message. No active thermal management in v1.
- **Background:** no background-modes entitlement needed (keeps review simple). iOS suspends the app almost immediately on switch — in-flight generation dies; persist chat state so relaunch resumes gracefully. Accept and document this. Disable idle timer during generation, cancel gracefully on interruption (call, backgrounding, lock).
- Track available memory with `os_proc_available_memory()`; request the `increased-memory-limit` entitlement (believed self-serve — **verify** current policy).
- **Explicitly not mirrored in v1:** advanced JNI-level perf tuning (thread pinning, custom memory-mapping tricks, speculative decoding, quantization experiments). Stock llama.cpp Metal configuration; measure first, tune only if the floor device is unusable.

---

## 3. Pipeline port (shared core vs Swift reimplementation — decision)

**Decision: Swift reimplementation, disciplined by shared golden test vectors.** Reject a Rust/C++ shared core for now; reject KMP.

**Reasoning:**
1. The pipeline is **small and deterministic** (router, 50-entry retrieval, ScamRules, veto, checklist logic). Port cost is M-sized; divergence risk is bounded and *detectable*.
2. The safety-critical content is **data, not code**: corpus JSON, prompt templates, checklist JSON, verbatim safety blocks all live in `/shared` and are consumed byte-identical. Model-side numeric drift cannot corrupt a verbatim block or a deterministic verdict.
3. Extracting a Rust core (or a KMP shared module) means **refactoring Android mid-stabilization** — a direct violation of the gate. UniFFI/KMP toolchain drag is real cost for a one- or two-person project.
4. **Divergence control:** the gate (§0) produces a golden-vector exporter — JSON files of `input → expected router card / retrieval top-k / ScamRules verdict / veto decision` generated from the Kotlin implementation, committed to `/shared`, executed by XCTest on iOS and by the existing harness on Android. Same vectors, both CIs.

**What is shared (data, not code)** — extracted at gate time, the only Android-side change permitted:
- Corpus JSON (50 entries), First-7-Days checklist JSON, ScamRules patterns, veto rules, prompt templates, eval prompt sets.
- **Golden fixtures** generated from the Android implementation: retrieval top-k results, ScamRules verdicts, veto decisions, think-strip edge cases. Both platforms' test suites consume the same files.

**Parity contract:** deterministic layers (ScamRules, veto, think-strip, checklist) must match fixtures **exactly**. Retrieval is asserted on top-k membership with documented tolerance if embeddings are involved. Generated answer text is *not* required to be token-identical (Metal vs CPU numerics, §6).

**Re-evaluation trigger (written down now):** the first cross-platform rule-divergence bug, or >2 ScamRules/retrieval changes per month for two consecutive months → reconsider a Rust+UniFFI core, at that point Android is stable enough to absorb the refactor.

**Open question that sizes this section:** what exactly "hybrid" retrieval is implemented with — Qwen embeddings via llama.cpp (free on iOS, same engine), a Kotlin-side lexical scorer (reimplement in Swift + fixtures), or a separate embedding model (which must then also ship/import on iOS). Need the answer before finalizing M3 effort.

---

## 4. UI port

**Screens (SwiftUI), mirroring the Android four plus iOS-necessary additions:**
1. Situation router — 4 cards (found something / someone contacted me / my own setup / want to learn).
2. First-7-Days walkthrough — deterministic JSON checklist, checkboxes persisted, per-step "Ask about this" handoff into chat.
3. Scam check — verdict-first UI (deterministic ScamRules verdict final; model only re-explains in simpler words).
4. Chat — grounded paraphrase + verbatim safety block, veto layer, honest refusals, fail-closed. Streaming-token UI *is* kept — on slow on-device inference it's core UX, not polish.
5. **New:** model manager — import, sha256 verification with progress, storage usage, delete, llama.cpp commit + model hash display.
6. **New:** onboarding/privacy explainer — why there's no network, how import works, the honest iOS-vs-Android privacy wording from §1.
7. **New:** about/disclaimers — not-financial-advice, source attributions, offline copy of eval results summary.

**UI copy lives in `/shared` as a strings source** so wording — especially safety wording — cannot drift.

**Persistence:** UserDefaults is the direct SharedPreferences analog (checklist state, prefs); JSON files in Application Support for chat history. Checklist step IDs are stable and versioned so progress survives checklist updates. **Explicitly not Keychain** — nothing in this app is a secret, and Keychain survives app uninstall, which is a privacy *negative* for this audience (a grieving user's data should vanish when they delete the app). Default iOS file data protection is sufficient. v1 must not accept, store, or display seed phrases or keys — same rule as Android, stated in-app.

**Platform equivalents:** Compose → SwiftUI; SharedPreferences → UserDefaults; adb sideload → Files/Finder import; JNI bridge → Swift/C interop; JUnit → XCTest; F-Droid → TestFlight; MessageDigest → CryptoKit SHA256; ViewModel → ObservableObject.

**Explicitly NOT mirrored in v1 (and why):**
- Advanced JNI perf tuning (§2) — measure stock Metal first.
- iPad-optimized layout — ship iPhone-only device family; simplifies review; SwiftUI makes adding iPad later cheap.
- Widgets, watch app, app extensions, share extensions — review surface and testing burden with no core-use-case value.
- Push or local notifications — nothing to notify; no entitlements keeps review clean.
- Multi-model support / model switching — one pinned model, one verified hash.
- In-app model download (deferred decision, §1), model picker / multiple models, OTA model updates.
- Localization beyond English — match Android parity.
- Voice input, transcript export/share — optional cheap add later, decide at M4.
- Mac Catalyst, Siri/Shortcuts, accounts, any telemetry.

**Accessibility:** Dynamic Type and VoiceOver come nearly free with SwiftUI — worth doing properly given the audience (grieving, possibly older, stressed users). First-class requirement, not polish: Dynamic Type, VoiceOver labels, large touch targets.

---

## 5. Distribution & App Store strategy

**Account:** $99/yr Apple Developer Program. Individual vs organization is a human decision — org requires D-U-N-S + legal entity + lead time; individual is faster but shows a personal name on the listing. Individual is fine now (the "organization required" rule applies to wallet/exchange apps, not education — verify at submission).

**Path:** TestFlight internal (≤100 testers, no beta review) for engineering → TestFlight external (up to 10,000 testers, requires Beta App Review — lighter than full review; **builds expire after 90 days**, so commit to a new build every 4–8 weeks or graduate to App Store before relying on TestFlight) → App Store release (full review). The GGUF stays on GitHub Releases for beta and store users alike — identical import flow everywhere, so beta and production behave the same. Note honestly: TestFlight crash reports are Apple-mediated and tester-controlled — the one OS-level exception to "nothing leaves the phone," disclosed in the beta notes.

**Review risks, concretely:**
- **Guideline 2.1 completeness / 4.2 minimum functionality — the biggest risk.** The app is inert until a 1.1GB model is imported; a reviewer sees an import screen. Mitigations: (a) a built-in, clearly-labeled **demo mode** (canned transcripts + sample checklist, no model) so every screen is reviewable; (b) App Review notes explaining the design with direct URL + 3-step import instructions; (c) fallback if rejected: bundle the model (§1) — app becomes self-contained. Decide the demo-mode approach before M4; it also improves first-run UX.
- **Bitcoin content:** position as educational Q&A. Not a wallet, no private keys, no transactions, no trading, no payments — state this explicitly in review notes. "Educational information, not financial or legal advice" disclaimer on first launch and in the description. Category: Education. Age rating: 4+ (no UGC, no web views → low rating; avoid any in-app browser). Crypto rules (guideline 3.1.5: wallets, mining) don't apply — nothing transacts — but expect reviewer pattern-matching on "bitcoin"; the review notes must pre-empt it.
- **Sensitive topics (grief, death, scams):** educational framing; keep parity with any crisis-resources content Android has (verify whether it exists).
- **Privacy label:** "Data Not Collected" — truthful and defensible with zero network code. A genuine marketing asset; keep it true.
- **Executable-code guideline:** GGUF is inert data, not code, and the app downloads nothing anyway; note this in review notes.
- **Export compliance:** Apple-crypto hashing only → exempt (`ITSAppUsesNonExemptEncryption = false`; verify wording).
- **Precedents to research before submitting:** local-LLM apps already on the store (e.g., Private LLM, LLM Farm — verify current status and how they handled model delivery and review).
- **EU alternative marketplaces:** exist post-DMA; not worth it (EU-only, fees, notarization overhead). Skip.

**Calendar honesty:** budget 1–3 review cycles and 2–4 weeks of calendar time for App Store iteration.

---

## 6. Testing & eval parity

- **Do not port the Python harness to Swift.** It drives llama.cpp, not the Android app, so it's already platform-independent — it remains the single source of truth for pipeline/model evaluation on desktop.
- **Golden vectors** (from §0/§3) run in XCTest — this *is* the harness port; the Python harness remains the generator and source of truth. Deterministic layers (ScamRules, veto, think-strip, checklist) must pass 100%.
- Swift unit tests mirroring the 22 Kotlin tests, with ScamRules at 100% coverage of the scam suite.
- UI tests (XCUITest) for the four screens using a **tiny dummy GGUF bundled only in the test target** — the real 1.1GB model never touches CI.
- **Numerics caveat (important):** Metal kernels produce different floating-point results than Android's CPU path, so token-level divergence is possible even with greedy sampling. Therefore: (a) parity is asserted at the *decision level* (retrieval top-k sets, verdicts, veto decisions), not token-exact text; (b) the real quality check is running the actual eval **on-device**: a hidden debug eval mode runs the FINAL/T06 prompt sets and exports JSON transcripts for offline grading with the same rubric. Confirm exactly how the Android on-device numbers were produced and replicate that procedure.
- **Eval parity:** run the full 105-question eval on **macOS with the same llama.cpp commit + same GGUF + greedy** as the iOS-fidelity proxy; then a ~15-question **on-device smoke run** (overnight, plugged in) to confirm the Metal path matches. Greedy decoding minimizes but doesn't eliminate CPU/Metal numeric divergence — acceptance: within ±2/105 of the proxy and **zero divergence in any safety-critical answer** (bounded anyway by verbatim blocks + deterministic verdicts).
- **Acceptance criterion for iOS beta:** iOS FINAL/T06 scores within a pre-registered band of Android's (propose ±5 points absolute on FINAL topical). A larger gap triggers investigation (template bug, context truncation, numerics) before shipping.
- Publish **EVAL_RESULTS_IOS.md** as an addendum with the same honesty standard as the Android eval.
- **Device test matrix:** 4GB / 6GB / 8GB iPhones — peak memory, tokens/sec, thermal over a 20-prompt session, jetsam behavior under memory pressure, and the 1.1GB import path (duration, low-disk handling). Manual matrix: one 4GB device + one 6/8GB device; 30-minute thermal soak; interruption tests (call, background, lock).
- **Manual QA script:** full walkthrough in airplane mode (proves no network needed); fresh-install flow; model re-verification; deleting the model mid-chat.

---

## 7. Milestones (S/M/L, ordered)

Sizing assumes one developer, moderate iOS experience: **S ≤ 1 week, M = 1–4 weeks, L > 4 weeks.** Rough single-dev estimates — **flag as uncertain**.

| # | Milestone | Size | Depends on | Done when |
|---|---|---|---|---|
| M0 | Apple account, hardware, Connect skeleton (pre-gate, optional) | S | human approval | account live |
| — | **BETA GATE PASSES (§0)** | — | Android | gate checklist signed off |
| M1 | Inference spike: XCFramework, Metal, load + generate on a 4GB device | S–M | gate | memory/thermal numbers; floor go/no-go |
| M2 | Model import + sha256 verify + backup exclusion + first-run UX | M | M1 | model in, verified, excluded |
| M3 | Swift pipeline + golden vectors green | M | gate (vectors) | XCTest parity with Kotlin |
| M4 | Four screens + persistence + accessibility + demo mode | M | M3 | UI tests pass with dummy model |
| M5 | Integration: walkthrough / scam / chat + veto + fail-closed | S | M2–M4 | end-to-end on device |
| M6 | Eval parity run + EVAL_RESULTS_IOS.md | S–M | M5 | within tolerance |
| M7 | TestFlight external beta + privacy label + review notes | S | M6 | external testers live |
| M8 | App Store decision incl. in-app download question | M | M7 feedback | submit or iterate |
| M9 | Maintenance process | ongoing S | — | coordinated llama.cpp upgrades across platforms; corpus updates flow to both |

**Total: roughly 2.5–4 months of engineering after the gate, plus review calendar time.**

M1 is deliberately first post-gate: it kills the largest technical unknown (4GB headroom) before any UI money is spent.

One flagged exception for the human to approve: M1 is a pure spike that touches no Android code and materially de-risks the plan. Default is strict sequencing (nothing before the gate closes); allowing M1 early is a reasonable exception if you want the GO/NO-GO answer sooner.

---

## 8. Risks & open questions for the human

**Risks:**
1. **The gate is far away.** Current FINAL ≈ 30% vs a proposed 50/105 bar — the Android quality work is the long pole; the iOS port is comparatively cheap and predictable. Plan accordingly.
2. **App Review completeness rejection** (inert-until-import app) — demo mode mitigates; bundling is the fallback, which slightly changes the checksum/privacy ritual. Decision needed.
3. **4GB-device instability** — undocumented jetsam limits; M1 may force a 6GB floor, shrinking the audience. Mitigations ready: q8 KV, 1024 ctx, increased-memory entitlement. Test in M1 before committing.
4. **Metal-vs-CPU numerics divergence** — decision-level fixtures + on-device eval mitigate; residual risk accepted and documented.
5. **Structurally weaker privacy claim on iOS** — no permission to omit; compensated by CI symbol checks, privacy label, and honest in-app wording. This is a real, permanent difference from Android.
6. **llama.cpp version drift between platforms** — pin commits; coordinate upgrades at most quarterly with a full eval re-run, both platforms together.
7. **Doubled maintenance burden** — corpus, model, and eval updates ×2 platforms; single-developer bus factor.
8. **Import friction for grieving users** — the audience is the least equipped to handle file operations. Safari handoff + in-app guide mitigates; the bundling debate is genuinely open and is ultimately a harm-reduction vs purity tradeoff only you can weigh. Beta feedback decides M8.
9. **TestFlight 90-day expiry churn** vs. a slow beta — cadence commitment or early App Store.
10. **Cross-platform rule drift** — mitigated by golden vectors + the written re-evaluation trigger.

**Open questions for the human:**
1. Ratify the §0 gate numbers — especially topical ≥50/105 and T06 ≥28/40 (the 60%/70% numbers from Plan A were proposals; Plan B's 50/28 are more conservative).
2. What exactly is hybrid retrieval implemented with (embeddings source)? This sizes M3.
3. Demo mode vs bundling the model — decide before M4.
4. Device floor policy: support 4GB iPhones with reduced context, or exclude?
5. Individual vs organization developer account?
6. How exactly were the Android on-device eval numbers produced? (Needed to replicate on iOS.)
7. Is the F-Droid beta release a hard prerequisite of the gate, or just "planned"?
8. Corpus freeze window during the iOS port — how strict?
9. iPhone-only device family for v1 — acceptable?
10. Pre-decision leaning on in-app download for App Store (import-only purity vs. UX/review safety)?
11. Budget approval: Mac, one 4GB iPhone, $99/yr.
12. Confirm Qwen3 license (believed Apache-2.0 — verify the file in-repo) and re-run whatever legal/disclaimer review was done for Android against the iOS store copy.
13. **Verify before M4 (do not assume):** current App Store bundle-size limit (long-standing 4GB), current cellular-download-cap behavior, ODR exact limits, TestFlight external review depth in practice, current status/approach of precedent local-LLM apps, llama.cpp Swift/Xcode package maturity, per-device jetsam headroom (empirical only), `increased-memory-limit` entitlement policy, `UIRequiredDeviceCapabilities` RAM key existence, `ITSAppUsesNonExemptEncryption` wording.

---

## Where the two drafts disagreed

| # | Disagreement | Plan A | Plan B | Resolution | Why |
|---|---|---|---|---|---|
| 1 | **Gate quality thresholds** | FINAL ≥ 60% (63/105); T06 ≥ 70% (28/40) | FINAL ≥ 50/105; T06 ≥ 28/40 | **Adopt Plan B's numbers** | Plan A's 60% is aspirational but risks indefinite gate delay; Plan B's 50/105 is a meaningful improvement over 31/105 while remaining achievable. The human must ratify either way. |
| 2 | **Veto false-positive metric** | Not specified | ≤5% false vetoes on benign set | **Adopt Plan B's metric** | Over-blocking is a real harm for grieving users; Plan A's omission was a gap. |
| 3 | **Human audit sample** | N=100 chat answers for citation integrity | 30-answer sample, two reviewers, ≥90% safe/useful | **Adopt both** | They measure different things: citation integrity (larger N) vs holistic quality (smaller N, two reviewers). |
| 4 | **Stability soak duration** | 4 weeks | 3 weeks | **Adopt Plan A's 4 weeks** | More conservative; the gate is the long pole anyway. |
| 5 | **Crash reporting** | User-exported diagnostics, opt-in manual reports only | Opt-in, user-initiated local crash-log export (share sheet) | **Adopt Plan B's specificity** | The share-sheet mechanism is concrete and preserves offline posture. |
| 6 | **Cold start / first token metric** | Not specified | ≤5s cold start, ≤5s first token on floor device | **Adopt Plan B's metric** | Concrete performance floor prevents shipping an unusable experience. |
| 7 | **Golden-vector exporter timing** | Extracted at gate time as "shared data assets" | Exporter merged as part of the gate itself | **Adopt Plan B's framing** | The exporter is a code change that must be merged and tested before the gate closes, not an afterthought. |
| 8 | **In-app download decision timing** | Rejected for v1 outright | Import-only for TestFlight; re-decide at M8 | **Adopt Plan B's deferral** | Plan A's outright rejection forecloses a legitimate UX/review-safety option; Plan B's deferral keeps the door open while preserving purity for beta. |
| 9 | **Device floor** | A14/iOS 16 with reduced context; hard floor may need 6GB | iOS 16+, 4GB RAM minimum (≈ iPhone 11/12/13) | **Adopt Plan B's floor** | Plan B is more specific and testable; both agree M1 decides empirically. |
| 10 | **Memory entitlement** | Not mentioned | Request `increased-memory-limit` entitlement | **Adopt Plan B's addition** | Concrete mitigation for 4GB devices; verify current policy. |
| 11 | **Thermal management** | Document behavior; no active management | Watch `ProcessInfo.thermalState` and degrade with honest message | **Adopt Plan B's addition** | Cheap to implement, honest UX. |
| 12 | **Streaming-token UI** | Not mentioned | Kept as core UX | **Adopt Plan B's addition** | On slow on-device inference, streaming is essential, not polish. |
| 13 | **UI copy in `/shared`** | Not mentioned | UI copy lives in `/shared` as strings source | **Adopt Plan B's addition** | Safety wording must not drift between platforms. |
| 14 | **Keychain stance** | Explicitly not Keychain (survives uninstall = privacy negative) | Keychain not needed; no secrets | **Adopt Plan A's reasoning** | Plan A's rationale is stronger: Keychain persistence after uninstall is a privacy *negative* for this audience. |
| 15 | **Eval parity method** | On-device eval mode runs full FINAL/T06 | macOS proxy (full 105) + ~15-question on-device smoke | **Adopt Plan B's method** | Running full 105 on-device is impractical (overnight, thermal); macOS proxy with same commit + GGUF is a reasonable fidelity proxy. |
| 16 | **Eval acceptance band** | ±5 points absolute on FINAL topical | ±2/105 of proxy | **Adopt Plan A's band** | ±5 points is more realistic given Metal/CPU numerics; ±2 is too tight. |
| 17 | **UI tests with dummy model** | Not mentioned | Tiny dummy GGUF bundled only in test target | **Adopt Plan B's addition** | Enables CI UI testing without 1.1GB model. |
| 18 | **M1 timing** | S–M, flagged as possible pre-gate exception | S, first post-gate | **Adopt Plan A's sizing (S–M) and flag** | M1 involves building llama.cpp and testing on multiple devices — more than S. The pre-gate exception is worth flagging for the human. |
| 19 | **M4 scope** | UI port + demo mode | Four screens + persistence + accessibility | **Adopt Plan A's inclusion of demo mode** | Demo mode is critical for App Review 2.1 mitigation; must be in M4. |
| 20 | **M8 scope** | App Store submission | App Store decision incl. in-app download question | **Adopt Plan B's framing** | M8 is the decision point, not just submission; the download question must be resolved here. |
| 21 | **F-Droid as gate prerequisite** | Open question | Not mentioned | **Keep as open question** | Plan A flagged it; Plan B omitted it. The human must decide. |
| 22 | **Qwen3 license verification** | Not mentioned | Confirm Apache-2.0, re-run legal review | **Adopt Plan B's addition** | Critical for App Store submission. |
| 23 | **EU alternative marketplaces** | Skip | Not mentioned | **Adopt Plan A's skip** | Not worth the overhead for v1. |
| 24 | **Precedent local-LLM apps** | Research before submitting | Not mentioned | **Adopt Plan A's addition** | Knowing how Private LLM, LLM Farm handled review is valuable intelligence. |
| 25 | **Export compliance wording** | Standard no-encryption answer (verify) | `ITSAppUsesNonExemptEncryption = false` (verify) | **Adopt Plan B's specificity** | Concrete key name is more actionable. |
| 26 | **Age rating** | 4+ | Low rating; no UGC, no web views | **Adopt Plan A's 4+** | More specific; both agree on the reasoning. |
| 27 | **Organization vs individual account** | Human decision; org requires D-U-N-S | Individual fine now; org rule applies to wallet/exchange apps | **Adopt Plan B's leaning** | Plan B provides a concrete recommendation with reasoning; still human's call. |
| 28 | **Re-evaluation trigger for shared core** | Third platform or >2 significant pipeline rewrites post-beta | First cross-platform rule-divergence bug, or >2 ScamRules/retrieval changes per month for two consecutive months | **Adopt Plan B's trigger** | More concrete and measurable; Plan A's "significant pipeline rewrites" is vague. |
| 29 | **Retrieval tolerance** | Top-k membership with documented tolerance | Not specified | **Adopt Plan A's tolerance** | Necessary because embeddings may introduce numeric drift. |
| 30 | **Demo mode decision timing** | Decide before M4 | Not explicitly timed | **Adopt Plan A's timing** | Demo mode must be designed before M4 implementation begins. |