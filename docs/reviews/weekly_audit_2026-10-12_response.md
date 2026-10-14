## Verdict per finding

**Medium — model not re-hashed after first verification → FIX NOW (as migration, not per-launch re-hash)**
- The adjudicated exposure is specifically the **legacy external path** (writable via adb / All-Files-Access apps). The internal path's record+size is acceptable: writing `filesDir` requires root or a compromised app, and a compromised app can rewrite `verified.json` alongside the file — so per-launch re-hash of internal adds ~nothing against realistic threats.
- Per-launch re-hash of 1.1 GB: **WONT-FIX** (plan D8; launch-speed constraint; 8GB floor test still pending).
- Correct fix: eliminate the legacy path via a **one-time hash-verified migration** into internal storage, then never read external again. This closes the window permanently and also heals any legacy file tampered *after* its first verification (hash is recomputed during the copy itself).

**Low — packaging gates downgrade to WARN when tools missing → FIX NOW.** Supply-chain gates that silently skip are exactly what they exist to prevent. Default must fail closed; dev hosts get a conscious, explicit opt-out.

**Low — shell injection via APK path → FIX NOW.** Trivial (subprocess arg lists), same file and pass as above. Realistic attacker today is the release manager's own filenames, but the fix is two lines and removes the class.

**Info — `mustNotInclude` veto not wired in Android chat path → FIX SOON (v0.2, already scheduled).** Disclosed, docs accurate, and current mitigations (guarded entry point, Answer disabled while secret present) hold the line. It is feature work with UX decisions; rushing a half-wired veto would itself violate honest labeling.

**Info — comment claims input cleared on refusal, but it isn't → FIX NOW.** One line. A comment claiming more than implemented is a direct violation of the repo's own culture, and clearing is the better behavior for a custody app regardless.

## Fix-now implementations (concrete, minimal, ordered)

**1. `ModelLocations.kt` — one-time legacy migration (Medium)**
- Add `suspend fun migrateLegacyModel(): MigrationResult` (sealed: `Noop`, `Migrated`, `NeedsSpace(bytes)`, `Tampered`, `CopyFailed(io)`) — invoked from the existing model-resolution entry point (where `verifyLegacyInPlace` runs today), on a background dispatcher, before path resolution.
- Logic:
  1. Legacy file absent → `Noop`. Fast path; normal launches unchanged (internal record+size only, no hashing).
  2. Internal final file exists with valid record+size → delete legacy leftover, return `Migrated` (idempotent).
  3. Delete any stale `<internal>.part` from a prior crash.
  4. Space check: `internalDir.usableSpace >= legacy.length() + 64 MB`. Insufficient → `NeedsSpace`; this session falls back to current legacy record+size behavior **with a persistent UI warning** ("free X MB to complete security migration"); migration retries next launch. Documented residual, not hidden.
  5. Stream-copy legacy → `.part` (1 MB buffer), computing SHA-256 during the copy (cost is the copy itself — one-time, not per-launch).
  6. Hash == pinned manifest hash (same constant import verification uses) → atomic rename `.part` → final (same filesystem, so rename is atomic); write verified.json record (fileName+sha256+size) for the internal file; delete legacy file; `Migrated`.
  7. Hash mismatch → delete `.part`, keep legacy file, return `Tampered`; refuse model load with "model verification failed — re-import" (matches existing import-mismatch behavior: no unverified final file is ever accepted).
- `resolve()` thereafter consults the internal path only; the legacy path is reachable only from the migration function.
- Docs: append to `model_import_plan.md` D8 — legacy external path eliminated by verified one-time migration; internal remains record+size **by design** (root-only write; compromised app defeats re-hash anyway). Do **not** claim tamper-proofing.
- Tests (temp dirs / Robolectric): happy path (legacy deleted, record written), tampered legacy (refused, `.part` removed), crash-leftover `.part` (cleaned), insufficient space (fallback + `NeedsSpace`), idempotent re-run.

**2. `ci/make_bundle.py` — fail-closed gates + no shell strings (both Lows)**
- Replace every `apksigner`/`aapt` shell-string invocation with subprocess list form, e.g. `subprocess.run([apksigner, "verify", "--print-certs", str(apk)], capture_output=True, text=True, check=True)`; delete the manual single-quote wrapping entirely.
- Add CLI flag `--allow-missing-tools` (default **off**). When a tool is missing:
  - without flag → `ERROR: <gate> requires <tool>; install Android build-tools, or pass --allow-missing-tools for dev-only bundles` → `sys.exit(1)`;
  - with flag → keep WARN + skip, print final line `BUNDLE BUILT WITH GATES SKIPPED (dev-only)`, and write sidecar `<artifact>.DEVGATES-SKIPPED` so a dev bundle can't be mistaken for a gated one.
- Confirm the CI image actually contains `apksigner`/`aapt` so release builds always run the gates; add to CI setup if missing.
- README/CONTRIBUTING: one paragraph — release bundles must never be built with the flag.

**3. `ChatPlaceholderScreen` — clear input on secret refusal (Info)**
- In the refusal branch (where the refusal message is emitted), clear the input state, e.g. `inputText = ""` (or `onValueChange("")` per the existing state shape). Keep the refusal message so the user knows why the field was cleared. Comment now matches behavior; no doc change needed (docs never claimed it).

Order: 1 → 2 → 3 by severity; 2 and 3 are independent and small enough to land in the same pass.

## Backlog items

- **Wire `mustNotInclude` veto in the Android chat path (v0.2, as scheduled).** Rationale: disclosed gap, current mitigations hold, and it needs UX decisions (what the user sees on veto). Acceptance criteria: veto evaluated on-device **before** any model invocation; fixtures for xprv/WIF/12- and 24-word seeds; docs flip from "planned v0.2" to "implemented" only once shipped and tested.
- **Considered and rejected — per-launch or async post-launch re-hash of the internal model.** A root/compromised-app attacker can rewrite `verified.json` along with the file, so re-hash buys ~nothing; it costs seconds per launch or adds unload-mid-session complexity. Revisit only if the threat model grows (e.g., restore of app data from untrusted backups).
- **Follow-up cleanup (minor):** once telemetry-free confidence exists that legacy users have migrated, delete the legacy-path code entirely in a later release to shrink the trusted surface.