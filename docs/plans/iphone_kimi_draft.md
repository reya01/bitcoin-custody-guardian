# iPhone Mirror Plan

## 0. Beta gate (Android criteria before iOS starts)

iOS work starts only when **all** of the following hold. Numbers are proposals — the human ratifies them before they become the gate.

**Quality bar ("sufficiently accurate local AI", made concrete):**
- Topical FINAL ≥ **50/105** in the shipping configuration (up from 31/105), with zero regressions in any category that passed at alpha.
- T06 walkthrough ≥ **28/40 strict** (up from 19/40).
- ScamRules: **100%** on the deterministic scam suite. Non-negotiable — it's rules, not model.
- Veto layer: 100% catch on the seeded adversarial set; ≤5% false vetoes on a benign-prompt set (grieving users must not get over-blocked).
- Human audit: 30-answer sample, two reviewers, ≥90% "safe and useful," **zero fabricated source attributions**.

**Stability bar (hard because the app has no telemetry — evidence is manual):**
- ≥10 external alpha testers, ≥3 weeks, zero open P0/P1 crash/hang reports. Consider an opt-in, *user-initiated* local crash-log export (share sheet) so offline posture is preserved but crashes are reportable.
- Cold start ≤5s and first token ≤5s on a defined floor Android device.

**Release hygiene:**
- Signed APK, published sha256, EVAL_RESULTS.md current, all 70 tests (26 unit + 44 harness) green [corrected 2026-10-11: plan-time counts 22+41; check_claims-enforced counts are now 26+44] in CI.
- **Golden-vector exporter merged** (see §3) — the gate itself produces the artifact iOS needs.
- Repo layout: `/shared` created holding corpus JSON, prompt templates, checklist JSON, eval sets, golden vectors; Android references it via a one-line sourceSet change. This is the *last* Android-touching change before the freeze.

**Freeze rule once the gate passes:** Android gets crash-fixes only. Any pipeline change requires regenerating golden vectors and shipping both platforms in the same release train. iOS work never blocks an Android hotfix.

*Permitted pre-gate (no codebase impact, human may veto):* Apple Developer enrollment ($99/yr), App Store Connect record skeleton, hardware purchases (a Mac is required; plus one 4GB-RAM iPhone for floor testing).

## 1. Model delivery & privacy story (decision + alternatives rejected)

**Decision: v1 ships with zero networking code. The model arrives by user-initiated file import** — Safari download from the pinned GitHub Release URL → "Open in…" / document picker; or Finder File Sharing / AirDrop from a Mac. In-app sha256 (CryptoKit) checked against the hash embedded in that build and published in the repo. Same GGUF bytes as Android — one artifact, one hash, one supply chain.

**The honest repositioning:** iOS has **no INTERNET permission to withhold** — every app may open sockets by default, so Android's "permission-absence" proof *cannot be mirrored*. The iOS guarantee becomes: (a) no network code in the binary, auditable because the app is open source; (b) **user-verifiable via Settings → Privacy & Security → App Privacy Report**, which will show zero network activity. That is the strongest available iOS analog and should be stated plainly on the store page and README.

**The in-app download question, argued both sides (as requested):**
- *For:* Since permission-absence doesn't exist on iOS anyway, the marginal purity loss is small. A user-tapped, checksum-pinned fetch from a fixed URL is inbound-only, sends nothing, has no telemetry — data-flow-equivalent to the Safari path, with far better UX for a grieving-beginner audience, and it lets App Review test the app easily.
- *Against:* It downgrades the guarantee from **structural** ("physically cannot") to **attestational** ("doesn't — audit us"). It adds TLS/URL supply-chain surface, invites scope creep (update checks, telemetry temptation), and weakens the project's single most trusted claim.
- *Resolution:* import-only for TestFlight; measure import-flow completion and confusion in beta feedback; **re-decide at M8** before App Store submission. Pre-design the download path so adding it is a small diff, not a redesign.

**Rejected:**
- *Bundle the GGUF in the app:* fits under the 4GB bundle cap (verify current docs), best out-of-box UX, but forces 1.1GB on every user, couples model updates to app review, and makes every dev build a 1.1GB upload. **Kept as the emergency fallback** if review rejects the import flow under guideline 2.1.
- *On-Demand Resources:* Apple-hosted (no third-party server), but requires a runtime network fetch, assets are purgeable and must be re-fetched, model updates are tied to app versions, and the story degrades to "talks only to Apple." Rejected for v1.

Practical detail: users may download to iCloud Drive, where the file can be evicted (dataless). On import, **copy into the app container** and set `isExcludedFromBackup` — a 1.1GB model must not inflate iCloud backups (App Review checks this).

## 2. Inference engine (build integration, device floor)

**Decision: llama.cpp at the same pinned commit as Android, built as an XCFramework via CMake (device + simulator slices), Metal enabled, called through its existing C API from a thin Swift actor wrapper.** iOS integration is strictly easier than JNI here — `llama.h` is C, so Swift imports it via a module map; no bridging-header gymnastics. The wrapper owns the generation loop, so cancellation is a flag checked between tokens.

- Full GPU offload (`n_gpu_layers` = max); unified memory means no copy penalty.
- Context 2048 to match Android; KV cache at f16 ≈ ~230MB for this model at 2048 ctx (measure; q8 KV and/or 1024 ctx are the levers if tight). Total footprint estimate ~1.4–1.6GB.
- Greedy sampling, `/no_think` + think-strip, identical prompt templates (shared as data from `/shared`).
- Foreground-only inference: disable idle timer during generation, cancel gracefully on interruption (call, backgrounding), watch `ProcessInfo.thermalState` and degrade with an honest message.
- Track available memory with `os_proc_available_memory()`; request the `increased-memory-limit` entitlement (believed self-serve — **verify** current policy).

**Device floor:** iOS 16+, **4GB RAM minimum** (≈ iPhone 11/12/13), 6GB+ recommended. Two honest caveats: per-app memory ceilings are undocumented and device-dependent (empirically ~half of RAM — **measure in M1**), and `UIRequiredDeviceCapabilities` has **no RAM key** (verify), so the store can't filter — gate at runtime with a graceful, honest message. If the M1 spike shows 4GB is unreliable, the floor rises to 6GB and we say so.

## 3. Pipeline port (shared core vs Swift reimplementation — decision)

**Decision: Swift reimplementation, disciplined by shared golden test vectors.** Reject a Rust/C++ shared core for now; reject KMP.

Reasons:
1. The pipeline is **small and deterministic** (router, 50-entry retrieval, ScamRules, veto, checklist logic). Port cost is M-sized; divergence risk is bounded and *detectable*.
2. The safety-critical content is **data, not code**: corpus JSON, prompt templates, checklist JSON, verbatim safety blocks all live in `/shared` and are consumed byte-identical. Model-side numeric drift cannot corrupt a verbatim block or a deterministic verdict.
3. Extracting a Rust core (or a KMP shared module) means **refactoring Android mid-stabilization** — a direct violation of the gate. UniFFI/KMP toolchain drag is real cost for a one- or two-person project.
4. **Divergence control:** the gate (§0) produces a golden-vector exporter — JSON files of `input → expected router card / retrieval top-k / ScamRules verdict / veto decision` generated from the Kotlin implementation, committed to `/shared`, executed by XCTest on iOS and by the existing harness on Android. Same vectors, both CIs.

**Re-evaluation trigger (written down now):** the first cross-platform rule-divergence bug, or >2 ScamRules/retrieval changes per month for two consecutive months → reconsider a Rust+UniFFI core, at that point Android is stable enough to absorb the refactor.

## 4. UI port (screens, persistence, platform equivalents)

Four screens map 1:1 in SwiftUI (NavigationStack): router home (same four cards, same copy, same order), First-7-Days walkthrough, scam check, chat. **UI copy lives in `/shared` as a strings source** so wording — especially safety wording — cannot drift.

Equivalents:
- SharedPreferences → **UserDefaults** for checklist state and settings. Checklist step IDs are stable and versioned so progress survives checklist updates.
- Keychain → **not needed. There are no secrets.** v1 must not accept, store, or display seed phrases or keys — same rule as Android, stated in-app.
- Model file → `Application Support/models/`, excluded from backup; import via document picker (open-in-place, then copy into the container).
- sha256 (MessageDigest) → CryptoKit SHA256. Compose → SwiftUI; ViewModel → ObservableObject.
- **No push, no background modes** — confirmed; inference sessions are short and foreground-only.
- Accessibility is a first-class requirement, not polish: Dynamic Type, VoiceOver labels, large touch targets. The audience skews older and stressed.

**Explicitly NOT mirrored in v1 (and why):**
- JNI-style perf tuning / hand-optimization — greedy short answers on Metal are fast enough; measure before optimizing.
- In-app model download (deferred decision, §1), model picker / multiple models, OTA model updates.
- iPad layouts, Mac Catalyst, widgets, Siri/Shortcuts, push, accounts, any telemetry.
- Streaming-token UI *is* kept — on slow on-device inference it's core UX, not polish.

## 5. Distribution & App Store strategy (TestFlight path, review risks)

**Phase 1 — TestFlight external beta** (up to 10,000 testers; requires Beta App Review, lighter than full review). Recruit from the Android alpha list. **90-day build expiry is a real constraint:** commit to a new build every 4–8 weeks or graduate to App Store before relying on TestFlight. Note honestly: TestFlight crash reports are Apple-mediated and tester-controlled — the one OS-level exception to "nothing leaves the phone," disclosed in the beta notes.

**Phase 2 — App Store**, gated on: eval parity met (§6), import-flow UX validated by testers, M8 download decision made.

Review posture:
- **Positioning:** Education category; "offline educational reference — not financial advice, no transactions, no wallet, no custody." Disclaimers in-app *and* in the description. Crypto rules (guideline 3.1.5: wallets, mining) don't apply — nothing transacts — but expect reviewer pattern-matching on "bitcoin"; the review notes must pre-empt it.
- **Privacy label: "Data Not Collected"** — a genuine marketing asset; keep it true.
- **Guideline 2.1 (completeness) is the top risk:** the reviewer needs the model to test AI features. Mitigation: direct URL + 3-step import instructions in App Review notes; fallback is the bundled-model build (§1).
- Age rating: no UGC, no web views → low rating; avoid any in-app browser. Export compliance: Apple-crypto hashing only → exempt (`ITSAppUsesNonExemptEncryption = false`; verify wording).
- Account: individual $99/yr is fine now (the "organization required" rule applies to wallet/exchange apps, not education — verify at submission).

## 6. Testing & eval parity (port the eval harness? on-device eval?)

- **Golden vectors** (from §0/§3) run in XCTest — this *is* the harness port; the Python harness remains the generator and source of truth.
- Swift unit tests mirroring the 22 Kotlin tests, with ScamRules at 100% coverage of the scam suite.
- UI tests (XCUITest) for the four screens using a **tiny dummy GGUF bundled only in the test target** — the real 1.1GB model never touches CI.
- **Eval parity:** run the full 105-question eval on **macOS with the same llama.cpp commit + same GGUF + greedy** as the iOS-fidelity proxy; then a ~15-question **on-device smoke run** (overnight, plugged in) to confirm the Metal path matches. Greedy decoding minimizes but doesn't eliminate CPU/Metal numeric divergence — acceptance: within ±2/105 of the proxy and **zero divergence in any safety-critical answer** (bounded anyway by verbatim blocks + deterministic verdicts).
- Publish **EVAL_RESULTS_IOS.md** as an addendum with the same honesty standard as the Android eval.
- Manual matrix: one 4GB device + one 6/8GB device; 30-minute thermal soak; interruption tests (call, background, lock).

## 7. Milestones with rough effort (S/M/L), ordered

S ≈ days, M ≈ 1–3 weeks, L ≈ multi-week. Rough single-dev estimates — **flag as uncertain**.

| # | Milestone | Size | Depends on | Done when |
|---|-----------|------|-----------|-----------|
| M0 | Apple account, hardware, Connect skeleton (pre-gate, optional) | S | human approval | account live |
| — | **BETA GATE PASSES (§0)** | — | Android | gate checklist signed off |
| M1 | Inference spike: XCFramework, Metal, load + generate on a 4GB device | S | gate | memory/thermal numbers; floor go/no-go |
| M2 | Model import + sha256 verify + backup exclusion + first-run UX | M | M1 | model in, verified, excluded |
| M3 | Swift pipeline + golden vectors green | M | gate (vectors) | XCTest parity with Kotlin |
| M4 | Four screens + persistence + accessibility | M | M3 | UI tests pass with dummy model |
| M5 | Integration: walkthrough / scam / chat + veto + fail-closed | S | M2–M4 | end-to-end on device |
| M6 | Eval parity run + EVAL_RESULTS_IOS.md | S | M5 | within tolerance |
| M7 | TestFlight external beta + privacy label + review notes | S | M6 | external testers live |
| M8 | App Store decision incl. in-app download question | M | M7 feedback | submit or iterate |

M1 is deliberately first post-gate: it kills the largest technical unknown (4GB headroom) before any UI money is spent.

## 8. Risks & open questions for the human

**Risks:**
1. **4GB jetsam headroom** — undocumented limits; M1 may force a 6GB floor. Mitigations ready: q8 KV, 1024 ctx, increased-memory entitlement.
2. **App Review 2.1 rejection** of the import flow — fallback is the bundled build; costs a 1.1GB listing.
3. **Cross-platform rule drift** — mitigated by golden vectors + the written re-evaluation trigger.
4. **llama.cpp upstream churn** — pin the commit, upgrade at most quarterly with a full eval re-run, both platforms together.
5. **90-day TestFlight expiry** vs. a slow beta — cadence commitment or early App Store.
6. **Import UX for the target audience** — a 1.1GB Safari download + import is a real hurdle for grieving beginners (though arguably *easier* than adb). Beta feedback decides M8.

**Open questions for the human:**
1. Ratify the §0 gate numbers — especially topical ≥50/105 and T06 ≥28/40.
2. Individual vs. organization developer account?
3. Pre-decision leaning on in-app download for App Store (import-only purity vs. UX/review safety)?
4. iPad in scope for v1? (Recommendation: no.)
5. Budget approval: Mac, one 4GB iPhone, $99/yr.
6. Confirm Qwen3 license (believed Apache-2.0 — verify the file in-repo) and re-run whatever legal/disclaimer review was done for Android against the iOS store copy.