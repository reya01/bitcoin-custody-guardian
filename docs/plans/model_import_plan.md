# Model Import (SAF) + Release Zip Bundle — Implementation Plan

> **Implementation status (2026-10-11, v0.1.1-alpha):** P0 core landed —
> manifest JSON + fail-closed parser (`ModelManifest`), single-pass copy+hash
> importer with zip ingestion and oversize guard (`ModelImporter`), record-based
> launch gate (`ModelLocations`), Compose gate UI (`ui/model/ModelGate.kt`),
> legacy in-place verification for v0.1.0 upgraders, packaging scripts
> (`ci/make_bundle.sh`, `ci/fetch_and_verify_models.sh`), cert pin file, CI
> verify workflow (unsigned build + no-INTERNET gate + weekly upstream-hash
> canary), README primary-path rewrite with adb demoted to troubleshooting,
> and the full JVM import test matrix.
> **Deliberate deviations:** (1) no foreground service yet — the import runs in
> a coroutine with keep-screen-on; process death is recovered honestly (stale
> `.part` cleaned, gate shows Welcome again). FGS `dataSync` is a fast-follow
> (P1). (2) Signing/publishing stay offline per `docs/RELEASE.md`; CI never
> holds the release key. (3) `POST_NOTIFICATIONS` request deferred with the FGS.

**Repo:** `reya01/bitcoin-custody-guardian` · **Target:** v0.1.1-alpha (versionCode 2) · **Status:** ready for implementation
**Provenance:** merged from the Kimi-K3 and DeepSeek-V4-Pro drafts. Every disagreement is resolved in §1 with a one-line rationale; items neither draft contained are marked **[new]**.

---

## 1. Merge decisions

| # | Topic | Decision | Why (one line) |
|---|---|---|---|
| D1 | Zip ingestion | In-app `ZipInputStream` extraction; the user never unzips | SAF grants a byte stream for the whole picked file — DeepSeek's "the zip must be in app-accessible storage" is wrong: the grant *is* the access. |
| D2 | Destination storage | Internal `filesDir/models/`; legacy external path recognized in place | Private on every API level, invisible to other apps/MTP; upgraders handled by the legacy probe (P0-10). |
| D3 | Verification passes | Single pass — hash computed while copying | One 1.1 GB read instead of DeepSeek's three; the copy *is* the verification, and a failed hash just discards the `.part`. |
| D4 | Zip compression | STORED (`zip -0`) | GGUF is entropy-dense (deflate saves ~1–3%) and STORED makes in-app extraction a straight flash-speed copy. |
| D5 | Bare GGUF as release asset | Not published; zip + APK + sums only | A second canonical copy of the model invites hash drift between assets; HuggingFace stays the bare-file source. |
| D6 | Backgrounding | Foreground service (`dataSync`) | A plain ViewModel coroutine dies under memory pressure when backgrounded on an 8 GB phone mid-copy. |
| D7 | Manifest schema | Kimi's (`defaultModelId`, `ciDownloadUrl`, `minRamMb`) | `defaultModelId` is the one-line 1.7B→4B switch; `ciDownloadUrl` makes CI drift-proof; an int RAM field is machine-readable (DeepSeek's `min_app_version` is meaningless — the manifest ships inside the app it versions). |
| D8 | Launch gate | Recorded hash ∈ allowlist + size match; no cold-start re-hash | Length-only (DeepSeek) accepts any same-size file; re-hashing 1.1 GB at every launch costs seconds. |
| D9 | File/zip detection | Magic bytes, never MIME or extension | Providers report inconsistent MIME for `.gguf` and browsers rename files; content doesn't lie. |
| D10 | Error copy | Curated per-reason strings; raw exception text never shown | Exception messages are meaningless (and alarming) to non-technical bitcoiners. |
| D11 | Strings | All copy in `strings.xml` | Enables the `ErrorCopyTest` guard and later localization; DeepSeek hardcoded strings in composables. |
| D12 | Delete-source | `FLAG_SUPPORTS_DELETE` check + write grant on the picker intent | The API-correct method; DeepSeek's authority-substring heuristic is fragile. |
| D13 | DI | Follow the project's existing wiring; no new framework | Adding Hilt for one feature is scope creep — the `DocumentSource` seam (P0-3) already provides testability. |
| D14 | Model byte size | CI-measured, never hand-typed | DeepSeek's `1181116006` is exactly 1.1×2³⁰ and unverified — do not trust it; CI asserts the real value. |

**Kept from both (agreement):** allowlist JSON in assets; SHA-256 gate before first use; `OpenDocument` picker; `DocumentsContract.deleteDocument` cleanup, non-fatal on failure; delete only after verification; case-insensitive filename matching; free-space pre-check; atomic `.part`→final rename; v0.1.1-alpha; README primary-path rewrite with adb demoted.

**[new] Added — neither draft caught these** (integrated below, marked where they appear):
1. Picker intent must add `CATEGORY_OPENABLE` and `FLAG_GRANT_WRITE_PERMISSION` — the default `ACTION_OPEN_DOCUMENT` grant is read-only, and source deletion needs write.
2. `POST_NOTIFICATIONS` runtime request on API 33+ — the FGS runs without it, but its progress notification may be suppressed.
3. Fail-fast byte cap: abort the copy the moment written bytes exceed `sizeBytes` (zip-bomb / lying-header protection).
4. Signing-cert fingerprint pinned in-repo and asserted by script + CI (`apksigner verify --print-certs`) so a wrong key can never ship and break upgrades.
5. Weekly scheduled CI canary re-verifying the upstream (HuggingFace) file still matches the pinned hash between releases.
6. Explicit refactor of the existing `data/AssetLoader.kt` and `ChatPlaceholderScreen.ModelMissing` branch — both drafts ignored the current code.
7. Accessibility pass (P1): progress announced via `liveRegion`, error screens semantically labeled.
8. Packaging script asserts zip contents (`unzip -l` = exactly the 4 expected flat entries).
9. `docs/RELEASE.md` maintainer runbook: the script is the primitive, CI calls the same script — one code path, no drift.
10. Uninstall wipes the model in *both* the old (`Android/data/…`) and new locations — the docs claim is accurate either way.

---

## 2. Ground truth (read first)

### 2.1 Zip-in-SAF on Android 8–14 — the precise behavior

- `ACTION_OPEN_DOCUMENT` and `ACTION_OPEN_DOCUMENT_TREE` never expose entries *inside* an archive as selectable documents on any API level 26–34; no `DocumentsProvider` browses zip contents. The user cannot pick "the .gguf inside the zip."
- Once the user picks the zip as a whole file, `ContentResolver.openInputStream(uri)` returns the raw byte stream, and `java.util.zip.ZipInputStream` inflates entries in-app — no extra permission, identical behavior on API 26–34. **The SAF grant is the access; the file does not need to be "in app-accessible storage."**
- Therefore **in-app zip ingestion is P0** (the app extracts the model from the picked zip); manual extraction (Files by Google → "Extract", Samsung My Files, some stock Files builds) is documented as a fallback only — some OEM file managers can browse zips, but we never rely on it.
- Caveat: `ZipEntry.getSize()` may return −1 when data descriptors are used; the expected uncompressed size always comes from the manifest, never from the zip header.

### 2.2 Where the model lives

- **New installs:** `context.filesDir/models/qwen3-1.7b-q4_k_m.gguf` (`/data/data/com.custodyguardian/files/models/…`). No permissions on any API level, invisible to other apps and file managers, mmappable by llama.cpp via absolute path.
- **Legacy (v0.1.0 upgraders):** the adb path `getExternalFilesDir(null)/models/<fileName>` is probed and used in place after a one-time hash verify (P0-10) — no forced 1.1 GB move.
- **Honest trade-off, stated in docs:** uninstalling wipes the model in either location → re-import needed.

---

## 3. User journeys

### Journey A — zip path, recommended (2 downloads, zero extraction)
1. Phone browser → `github.com/reya01/bitcoin-custody-guardian/releases` → latest release.
2. Download `btcg-v0.1.1-alpha.apk` (small) **and** `btcg-v0.1.1-alpha-full.zip` (~1.1 GB). Wi-Fi recommended.
3. Tap the APK in Downloads → "install unknown apps" prompt → allow for the browser → Install. (Docs: "this prompt is normal for apps installed outside a store.")
4. Open the app → Welcome screen explains the no-internet design → **"Select the model file."**
5. System picker → choose `btcg-v0.1.1-alpha-full.zip` in Downloads. Name doesn't matter — content is checked; `…(1).zip` is fine.
6. App streams the `.gguf` out of the zip, copies to private storage while hashing, verifies against the pinned hash → **Success** → chat, with suggested first question ("What is a seed phrase?").
7. "Delete the original after import" (default ON) removes the zip, freeing ~1.1 GB.

### Journey B — zip only (1 download, manual extraction)
1. Download only the full zip. 2. Files by Google (or OEM file manager) → Downloads → tap zip → **Extract**. 3. Install the extracted APK. 4. Open app → pick the extracted `qwen3-1.7b-q4_k_m.gguf`. 5. Copy → verify → success; delete the extracted folder when done.

### Journey C — bare .gguf (user already has the model)
1. User already has `qwen3-1.7b-q4_k_m.gguf` (HuggingFace phone-browser download, or USB copy into `Download/`). 2. Install the APK only. 3. Open app → pick the `.gguf` → size pre-check → copy+hash → verify → success. 4. Wrong build → hash mismatch → the app names exactly which file it needs.

---

## 4. Pinned-hash allowlist — `app/src/main/assets/model_manifest.json`

```json
{
  "schemaVersion": 1,
  "defaultModelId": "qwen3-1.7b-q4_k_m",
  "models": [
    {
      "id": "qwen3-1.7b-q4_k_m",
      "fileName": "qwen3-1.7b-q4_k_m.gguf",
      "sha256": "b139949c5bd74937ad8ed8c8cf3d9ffb1e99c866c823204dc42c0d91fa181897",
      "sizeBytes": 0,
      "minRamMb": 8192,
      "ciDownloadUrl": "https://huggingface.co/<org>/<repo>/resolve/main/qwen3-1.7b-q4_k_m.gguf"
    }
  ]
}
```

- `sizeBytes` is set from the CI-measured `stat -c%s` of the hash-verified file at first CI run (D14 — never hand-typed); CI fails the build on any mismatch.
- `ciDownloadUrl` is consumed **only by CI** — the app never networks.
- Parser: `data/model/ModelManifest.kt` (matches the existing `data/` package), `org.json` (zero new deps), unknown fields ignored (forward-compat), malformed/missing fields **fail closed**.
- Runtime rules: import accepts a computed SHA-256 matching **any** entry (the matched entry's metadata is used); launch gate requires the recorded hash ∈ allowlist; `defaultModelId` drives "expected file" messaging and the update-required comparison.
- **1.7B → 4B switch = add one entry + flip `defaultModelId`** (one line). Old 1.7B installs keep working (still allowlisted); new installs are asked for the 4B file; the 4B entry's `minRamMb` powers an honest "this model wants 12 GB" advisory (P1-5). Revoking a hash = remove its entry → existing installs get `UpdateRequired`.

---

## 5. Screen-by-screen spec + copy deck

All strings in `strings.xml` (D11). Tone rules: no jargon without a plain-language gloss; honest time labels (never fake countdowns); never blame the user.

### S1 — Welcome / model-missing (first run, and any launch with no usable model)
- **Title:** "One thing to set up"
- **Body:** "Bitcoin Custody Guardian runs entirely on your phone. It has **no internet access at all** — not a promise, but enforced by Android: the app never requests the internet permission, so the system blocks it from going online. Your questions never leave this device. Because it can't go online, it can't download its AI model by itself. You give it the file **once**. After that, it works offline forever."
- **Info card:** "You need: the model file (about 1.1 GB). Easiest: `btcg-v0.1.1-alpha-full.zip` from the app's GitHub page — the app reads the model straight out of the zip, no unzipping needed. Or: `qwen3-1.7b-q4_k_m.gguf` if you already have it."
- **Checkbox (default ON):** "Delete the original file after it's safely imported (frees about 1.1 GB)"
- **Primary button:** "Select the model file"
- **Secondary button:** "I don't have the file yet" → fires `Intent(ACTION_VIEW, <releases URL>)` — legal without INTERNET permission; the *browser* does the networking.
- **Footer:** "Needs 1.1 GB of free storage · alpha software"

### S2 — Pre-flight space error (shown instead of launching the picker when `StatFs` says space is short)
- **Title:** "Not enough storage"
- **Body:** "The model needs {needed} free; this phone has {available}. Free up some space and come back — nothing has been changed."
- **Button:** "Check again"

### S3 — Import (one screen, three phase labels; determinate bar from byte counts)
- **Title:** "Setting up your model"
- Phase **Inspecting:** "Checking the file… · {displayName}"
- Phase **Copying:** "Copying into the app's private storage — {done} MB of {total} MB" + progress bar + coarse estimate: "Usually under a minute" (P0) / rolling-rate buckets "a few seconds left" / "about 30 seconds left" / "about a minute left" / "a few minutes left" (P1-3).
- Phase **Verifying:** "Checking the fingerprint… The app compares the file's SHA-256 fingerprint against the one published with this release. If they don't match, the file is refused — never silently used." (Brief by design: hashing happens inline during the copy, so this phase is just digest finalization + compare + rename — the UI must not fake a long verification.)
- **Note:** "You can leave the app — this continues in the background." (FGS notification mirrors progress.)
- **Button:** "Cancel" (confirms; deletes partial file; returns to S1)

### S4 — Success
- **Title:** "Done — your model is verified ✓"
- **Body:** "Fingerprint `b139949c…181897` — matches this release."
- If source deletion failed or was unchecked: "The original file is still in your Downloads. You can delete it to free about 1.1 GB."
- **Button:** "Ask your first question" → chat with suggestion chip "What is a seed phrase?" (auto-advances after ~2.5 s on first run)

### S5 — Failure (one screen, reason-driven copy)

| Reason | Title | Body | Actions |
|---|---|---|---|
| `NotAModel` | "That file isn't a model" | "You chose “{name}”. The app needs the model file (`qwen3-1.7b-q4_k_m.gguf`) or the full package (`btcg-…-full.zip`). The file's *name* doesn't matter — but its contents aren't a GGUF model." | "Choose again" |
| `Incomplete` | "This file is incomplete" | "“{name}” is {actual} MB, but the model should be {expected} MB. The download was probably interrupted. Delete this copy and download it again — Wi-Fi is best for 1.1 GB." | "Choose again" |
| `HashMismatch` | "Couldn't verify this file" | "The file's fingerprint doesn't match the one published with this release. It may be the wrong model, a modified file, or a damaged download. This app only runs a model it can verify — it won't guess." | "Choose again" · "Get the file again" |
| `NoModelInZip` | "No model inside this zip" | "“{name}” doesn't contain `qwen3-1.7b-q4_k_m.gguf`. Make sure it's `btcg-…-full.zip` from the release page — **not** GitHub's “Source code (zip)”. If you picked `btcg-….apk`: that's the installer — open it from Downloads to install the app instead." | "Choose again" |
| `NotEnoughSpace` | "Not enough storage" | "The model needs {needed} free; this phone has {available}. Free up space and try again — nothing was kept from this attempt." | "Try again" |
| `SourceUnreadable` | "Couldn't read that file" | "If it's in Google Drive or another cloud app, download it to this phone first (it must be in Downloads or another local folder). If it's on an SD card, make sure the card is inserted." | "Choose again" |
| `WriteFailed` | "Something went wrong while copying" | "No changes were kept. Try again; if it keeps failing, restart the phone and retry." | "Try again" |
| `Unknown` **[new]** | "Something went wrong" | "No changes were kept. Try again; if it keeps failing, please open an issue on GitHub quoting the text on this screen." (Raw exception text is logged locally only — never shown.) | "Try again" |

### S6 — Model update required (after an app update changes the pinned model)
- **Title:** "This version needs a different model"
- **Body:** "The app was updated and now expects a different model file. The old one can't be used with this version."
- **Buttons:** "Select the new model" · "Delete the old model (frees 1.1 GB)"

### S7 — Legacy one-time verify (upgraders from v0.1.0 with an adb-sideloaded model)
- "Checking your existing model… One-time check: making sure the model you already installed is genuine and complete." → then Ready.

### S8 — Settings → "AI model" card (P1)
- "`qwen3-1.7b-q4_k_m.gguf` — 1.1 GB · Verified ✓ · fingerprint `b139…1897`" · buttons "Replace model…" and "Re-check integrity" (re-hash with progress).

---

## 6. Numbered implementation plan

### P0 — ship-blockers for v0.1.1-alpha

**P0-1. Model manifest asset + parser.** As §4. Acceptance: parser unit tests green; malformed/missing fields fail closed; multi-entry allowlist parses.

**P0-2. State machine + repository.** `data/model/ModelState.kt` — sealed interface: `Loading, Missing, Importing(phase, bytesDone, bytesTotal), Ready(record), Failed(ImportError), UpdateRequired(installedId, requiredId)`. `data/model/ModelRepository.kt` — owns `MutableStateFlow<ModelState>`; persists `ModelRecord(fileName, sha256, sizeBytes, absolutePath, importedAt, sourceDisplayName, verifiedByAppVersion)` in preferences DataStore. Launch gate (fast path): file exists **and** `length() == record.sizeBytes` **and** record hash ∈ allowlist → `Ready`. **No re-hash on cold start** (1.1 GB hash = seconds; unacceptable). Full hash happens at import, on demand ("Re-check integrity"), and once for legacy installs — honest trade-off, documented: a file corrupted *after* import fails at llama.cpp load → existing inference error state. On init: delete any stale `*.part`. **[new]** Absorb `data/AssetLoader.kt`: it delegates path resolution to the repository (single source of truth), keeping its public API for llama.cpp init; `ChatPlaceholderScreen`'s `ModelMissing` branch routes to the new gate instead of its own copy.

**P0-3. SAF abstraction (testability seam).** `data/model/DocumentSource.kt` — interface: `openStream(): InputStream`, `size(): Long?`, `displayName(): String?`, `delete(): Boolean`. `data/model/SafDocumentSource.kt` — implements over `ContentResolver` (`OpenableColumns.SIZE`/`DISPLAY_NAME`, `DocumentsContract.deleteDocument`). Acceptance: importer unit tests run on JVM against a fake.

**P0-4. Import pipeline.** `data/model/ModelImporter.kt` — `suspend fun import(source: DocumentSource, deleteSourceAfter: Boolean)` on `Dispatchers.IO`:
1. Query size/name. Read 4 magic bytes: `GGUF` (47 47 55 46) → bare model; leading `PK` (50 4B) → zip; else `ImportError.NotAModel`.
2. Bare GGUF: if size known and `size != entry.sizeBytes` → `Incomplete` **before copying** (instant fail on truncated downloads).
3. Zip: `ZipInputStream`; select entry `name == manifest.fileName`, else first `*.gguf` (case-insensitive); none → `NoModelInZip`. Stream-inflate that entry.
4. Copy loop: 1 MiB buffer; `MessageDigest(SHA-256).update()` per chunk; write to `models/<fileName>.part`; emit progress every ≥256 KiB; `ensureActive()`; **[new] abort the instant written bytes exceed `sizeBytes`** (zip-bomb / lying-header guard).
5. EOF: total bytes ≠ `sizeBytes` → `Incomplete`; digest hex ≠ `entry.sha256` (case-insensitive) → `HashMismatch`.
6. Atomic `renameTo(models/<fileName>)` (same mount; safe even over an in-use mmap — POSIX keeps the old inode alive for open FDs). Write `ModelRecord`. If `deleteSourceAfter` → `source.delete()` (try/catch; failure only flips a flag shown on S4).
7. `IOException` on read → `SourceUnreadable`; on write → re-stat `StatFs` → `NotEnoughSpace` or `WriteFailed`; unexpected → `Unknown` (logged locally, never shown raw). Always delete `.part` on failure/cancel.
Destination file name comes **from the manifest**, never from the source display name → lowercase `qwen3-1.7b-q4_k_m.gguf` guaranteed regardless of browser renames. Acceptance: full unit matrix (§11) green.

**P0-5. Zip ingestion.** Folded into P0-4's branch — listed separately because it's the feature that makes the zip viable. The shipped zip uses STORED entries (§7) so this path is a straight copy at flash speed.

**P0-6. Foreground import service.** `data/model/ModelImportService.kt` — `foregroundServiceType="dataSync"`, `android:exported="false"`; manifest permissions `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` (no-ops on older API levels, enforced on 34+). Partial `WakeLock` with timeout for the duration. Notification channel `model_import` (IMPORTANCE_LOW), progress throttled to 500 ms, cancel action. Service writes progress into the repository's flow; UI is a pure observer → rotation and backgrounding are non-events. Process death → next launch finds stale `.part`, cleans it, shows `Missing` (honest, recoverable). **[new]** Request `POST_NOTIFICATIONS` at runtime on API 33+ before import; if denied, import continues (the notification may be hidden — the in-app UI still shows progress when foregrounded).

**P0-7. UI.** `ui/model/ModelGate.kt` — root composable wrapping the existing NavHost: `when (state) { … Ready -> AppNavHost() … }`. `ui/model/WelcomeModelScreen.kt`, `ImportScreen.kt` (one screen, phase-driven), `ImportSuccessScreen.kt`, `ImportErrorScreen.kt`, `ModelUpdateScreen.kt` (minimal for P0), `ModelViewModel.kt`. Picker: `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())` launched with `arrayOf("*/*")` — GGUF has no registered MIME type and providers variously report `application/octet-stream` or null; `*/*` plus content sniffing is the only reliable filter. **[new]** Add `CATEGORY_OPENABLE` and `FLAG_GRANT_WRITE_PERMISSION` to the picker intent (default grant is read-only; delete needs write). Validate by magic bytes, not extension. `FLAG_KEEP_SCREEN_ON` during import as belt-and-braces.

**P0-8. Free-space checks.** Injectable wrapper over `StatFs(filesDir).availableBytes`. Pre-flight on Welcome (S2) and re-check after pick: require `entry.sizeBytes + 150 MB` headroom. Mid-copy write failure → re-stat → `NotEnoughSpace(needed, available)`.

**P0-9. Delete-source-after-verify.** Checkbox state from S1 → passed to importer. Delete only **after** successful rename + record write. Check `DocumentsContract.Document.COLUMN_FLAGS and FLAG_SUPPORTS_DELETE` when available; attempt regardless, catch `SecurityException`/`UnsupportedOperationException` → S4 shows the manual-cleanup line. Never block success on deletion.

**P0-10. Legacy model recognition (v0.1.0 upgraders).** `data/model/LegacyModelMigration.kt` — if internal file absent, probe `getExternalFilesDir(null)/models/<fileName>`. If present and size matches manifest → one-time hash verify (S7) → write `ModelRecord` pointing at the legacy path → `Ready`. Hash fail → `Missing` with explanation. No forced move (copying 1.1 GB costs transient space; using in place is fine — it's our own dir, no permissions needed).

**P0-11. Packaging script.** `ci/make_bundle.sh` (§7) — the single packaging primitive; asserts signing cert, no-INTERNET permission, and model hash/size against the manifest; builds zip + sums; refuses on any failure.

**P0-12. CI workflow.** `.github/workflows/release.yml` (§7) calling the same scripts; manifest-driven model fetch/verify; no-INTERNET gate on the *signed* APK; **[new]** weekly schedule canary for upstream model drift.

**P0-13. Docs (minimum viable).** README primary-flow rewrite, `dist/README-FIRST.txt` (ships inside the zip), release-notes template, adb demoted to troubleshooting (§10).

**P0-14. Tests (core).** §11 unit suite + one instrumented happy path + Compose state-render tests.

### P1 — fast follow (v0.1.1 if capacity, else v0.1.2)

- **P1-1.** Settings "AI model" card (S8): replace model, re-check integrity with progress. After a replace, delete the legacy external file if the record pointed there. (Replacing while the old model is loaded is safe — rename-over-mmap semantics — but prefer unloading llama.cpp first.)
- **P1-2.** Full `UpdateRequired` UX (S6) incl. delete-old-model action; exercised when the 4B entry lands.
- **P1-3.** Rolling-rate time estimate (measure after 2 s; coarse buckets only — honest labels).
- **P1-4.** Retry without re-picking: retain the granted `Uri` in the ViewModel for the process lifetime; "Try again" reuses it.
- **P1-5.** Low-RAM advisory: if `ActivityManager` total RAM < `minRamMb`, show an informational line on Welcome ("This model is tuned for 8 GB phones; yours has X"). Non-blocking.
- **P1-6.** CI generates release notes from `.github/RELEASE_NOTES_TEMPLATE.md` with hashes injected.
- **P1-7.** Instrumented picker-wiring test with a fake `ActivityResultRegistry`.
- **P1-8. [new]** Accessibility pass: `liveRegion` progress announcements, semantic headings on error screens, TalkBack walkthrough added to the manual checklist.
- **P1-9. [new]** GitHub issue template for import failures asking users to quote the exact S5 error text (no telemetry exists — no network — so this is the only feedback channel).

### P2 — later

- **P2-1.** "Verify on every launch" debug setting.
- **P2-2.** `.part` resume (probably not worth it for 1.1 GB — revisit only on field reports).
- **P2-3.** Localization (strings already externalized).
- **P2-4.** Multi-model picker UI when the allowlist grows (1.7B + 4B).
- **P2-5.** Optional "move legacy model into private storage" action.
- **P2-6.** Desktop helper script (`tools/adb_fallback.sh`: `adb push … /sdcard/Download/`) for the troubleshooting path.

**Suggested build order:** P0-1→2→3→4→5 (pure JVM, fully testable), then 6→7 (service + UI), then 8→9→10 (hardening), then 11→12 (packaging), with 13→14 running throughout.

---

## 7. Release assets & packaging

**Assets on the release page (3):**

| Asset | Contents |
|---|---|
| `btcg-v0.1.1-alpha-full.zip` | primary — APK + model + sums + README-FIRST (≈1.1 GB) |
| `btcg-v0.1.1-alpha.apk` | the app only (upgraders, Journey A) |
| `btcg-v0.1.1-alpha-SHA256SUMS.txt` | sha256 of the zip and the APK |

No bare-GGUF asset (D5): the zip is directly importable in-app, and HuggingFace remains the canonical bare-file source — one canonical copy, no drift.

**Zip contents (flat — no subdirectory; friendlier when extracted):**
```
btcg-v0.1.1-alpha-full.zip
├── btcg-v0.1.1-alpha.apk      # the signed release APK, renamed
├── qwen3-1.7b-q4_k_m.gguf     # STORED (zip -0): GGUF is entropy-dense;
│                               #   deflate saves ~1–3% at big extraction cost
├── SHA256SUMS.txt             # sha256 of the two files above (for desktop verifiers;
│                               #   the app does NOT trust it — it checks against the
│                               #   fingerprint compiled into the app)
└── README-FIRST.txt           # 5-line install instructions, from dist/README-FIRST.txt
```

**`ci/make_bundle.sh` — the single packaging primitive (drift-proof by construction):**
1. Inputs: tag, signed APK path (default: `assembleRelease` output), model dir. `set -euo pipefail`.
2. **[new]** Assert `apksigner verify --print-certs` fingerprint == `ci/release_cert_fingerprint.txt` (pinned in-repo; must equal the v0.1.0 cert so upgrades keep working).
3. Assert `aapt dump permissions` on the APK contains **no** `android.permission.INTERNET` — protects the core guarantee from manifest-merger regressions.
4. Assert model sha256 + byte size == manifest values (manifest read via `jq`).
5. Stage flat: renamed APK, `<manifest fileName>`, `SHA256SUMS.txt`, `dist/README-FIRST.txt`.
6. `zip -0 -X` → `btcg-<tag>-full.zip`; **[new]** assert `unzip -l` lists exactly the 4 expected flat entries.
7. Write `btcg-<tag>-SHA256SUMS.txt` (zip + apk).

**CI (`.github/workflows/release.yml`)** — same scripts, so maintainer-made and CI-made releases cannot diverge:
```yaml
name: release
on:
  push: { tags: ['v*'] }
  schedule: [{ cron: '0 6 * * 1' }]        # [new] weekly upstream-hash canary
permissions: { contents: write }
jobs:
  model:
    steps:
      - uses: actions/checkout@v4
      - run: ./ci/fetch_and_verify_models.sh app/src/main/assets/model_manifest.json build/models
        # jq the manifest → curl ciDownloadUrl → sha256sum + stat -c%s must equal
        # the pinned values. ANY mismatch fails the job.
      - uses: actions/upload-artifact@v4
        with: { name: models, path: build/models }
  release:
    needs: model
    if: startsWith(github.ref, 'refs/tags/v')
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }
      - uses: actions/download-artifact@v4
        with: { name: models, path: build/models }
      - run: ./gradlew assembleRelease        # signing from repo secrets (docs/RELEASE.md)
      - run: ./ci/make_bundle.sh "$GITHUB_REF_NAME"
      - uses: softprops/action-gh-release@v2
        with: { files: 'dist/*' }
```

**[new] `docs/RELEASE.md` maintainer runbook:** the repo has no CI release pipeline yet, so the script is the primitive and CI is a wrapper. Document (a) CI secrets setup (base64 keystore + passwords), and (b) the local path until CI is trusted: `./gradlew assembleRelease && ./ci/make_bundle.sh v0.1.1-alpha`. Both paths call the same script; the manifest is the single source of truth consumed by the app, the verifier, and the bundler — artifacts cannot drift from the signed APK because the script stages from the exact build output and asserts its signature and permissions before zipping.

**Why the zip is worth it:** it guarantees a version-matched APK+model pair in one download, carries its own sums and instructions, and (P0-5) doubles as a directly importable artifact. Cost: ~1.1 GB of release storage (GitHub allows 2 GB/asset).

---

## 8. Storage plan

- **Final:** `/data/data/com.custodyguardian/files/models/qwen3-1.7b-q4_k_m.gguf`. **Temp:** sibling `*.part`; atomic rename on success (same mount).
- **Transient footprint:** source (1.1 GB zip or gguf, already downloaded) + destination copy (1.1 GB) ⇒ **~2.2 GB during setup, 1.1 GB after** the delete-source checkbox does its job. Docs state this plainly.
- **Pre-check formula:** `availableBytes ≥ manifest.sizeBytes + 150 MB`. For the zip path the *uncompressed* size comes from the manifest — no reliance on `ZipEntry.getSize()` (unreliable from `ZipInputStream` when data descriptors are used).
- **Delete source:** `DocumentsContract.deleteDocument(contentResolver, uri)` post-verify, using the write grant requested in P0-7; `ExternalStorageProvider` (which backs Downloads on primary storage and SD) supports delete; cloud providers may not — all failures non-fatal.
- **Uninstall wipes the model** (true for both the old `Android/data` location and the new internal one) — documented in README + FAQ.

---

## 9. Edge-case matrix

| Case | Detection | UX | Recovery |
|---|---|---|---|
| Truncated download (bare gguf) | `OpenableColumns.SIZE ≠ sizeBytes` | `Incomplete`, **before copying** | Re-download |
| Truncated zip / interrupted stream | EOF byte count ≠ `sizeBytes`, or `ZipException` | `Incomplete` / `SourceUnreadable` | Re-download |
| Wrong file (photo, random file) | Magic bytes ≠ GGUF/PK | `NotAModel` | Choose again |
| GitHub "Source code (zip)" picked | Zip with no `.gguf` entry | `NoModelInZip` (names this exact mistake) | Get the right zip |
| Extracted APK picked as model | PK magic, no `.gguf` entry | `NoModelInZip` (copy names this mistake too) | Install the APK instead |
| Valid GGUF, wrong model/build | Hash mismatch | `HashMismatch` ("won't guess") | Get correct file |
| Browser renamed (`…(1).zip`, uppercase) | Ignored — content sniffed, dest name from manifest | Works silently | — |
| Zip entry larger than `sizeBytes` **[new]** | Fail-fast cap in copy loop | Aborts immediately as `Incomplete`/`NotAModel`; `.part` deleted | Get the right zip |
| Storage full before start | Pre-flight `StatFs` | S2 with real numbers | Free space, "Check again" |
| Storage full mid-copy | `IOException` on write + re-stat | `NotEnoughSpace`; `.part` deleted | Free space, retry |
| User cancels | Coroutine cancellation | Silent return to S1; `.part` deleted | Restart import |
| User cancels the picker itself | Null URI result | Returns to Welcome, nothing changed | — |
| Process death mid-import | Stale `.part` on next init | Cleaned; `Missing` | Restart import |
| Retry after failure | Re-pick (P0) / retained URI (P1-4) | Idempotent; `.part` overwritten | — |
| Re-import over existing model | Same flow; `.part` + atomic rename replaces | Record updated (safe even if old file is mmap'd — POSIX rename semantics) | — |
| App update changes pinned hash | Record hash ∉ allowlist | `UpdateRequired` (S6) | Import new model; delete old |
| v0.1.0 adb model present | Legacy path probe | One-time verify (S7) | None needed |
| Source in Google Drive / cloud | Stream stalls or fails | `SourceUnreadable` ("download to this phone first") | Copy locally |
| Source on SD card | Works via SAF; slower | Progress estimate adapts | — |
| Delete-source fails | Exception / no `SUPPORTS_DELETE` | Success stands; S4 shows manual-cleanup line | User deletes manually |
| Notification permission denied (13+) **[new]** | Permission check | Import continues; no visible notification when backgrounded | None needed |
| Airplane mode | — | **Everything works** — this is the demo | — |

---

## 10. Documentation updates

**README rewrite outline:**
1. Title + one-liner + **alpha** badge.
2. **"Private by design"** box (unchanged, prominent): no INTERNET permission, OS-enforced; consequence — *you* supply the model file once.
3. **Install (about 2 minutes + one 1.1 GB download)** — the new primary path: download the two assets → install APK ("the 'install unknown apps' prompt is normal") → open app → "Select the model file" → pick the zip ("no unzipping needed") → ~a minute of copy + verify. 4 screenshot placeholders.
4. "Only downloaded the zip?" — extract via Files by Google, install APK, pick the extracted `.gguf`.
5. "Already have the model file?" — bare-gguf path; HuggingFace link; must match the release fingerprint.
6. **Verify your download** — `SHA256SUMS.txt`; `shasum -a 256 -c` / `certutil -hashfile … SHA256`.
7. **Storage** — ~2.2 GB transient, 1.1 GB after; the app offers to delete the original; uninstall wipes the model.
8. **Troubleshooting** — table mapping every in-app error to cause + fix; then **"Advanced: adb fallback"** (demoted): `adb push qwen3-1.7b-q4_k_m.gguf /sdcard/Download/` → use the in-app picker (works on Android 11+ where `Android/data/` is locked); the old direct-to-`Android/data` method kept as a footnote for existing users. Wording: "This is a fallback for when the in-app import misbehaves — not the normal path."
9. **FAQ** — why no auto-download; is the zip safe; what fingerprint is pinned; update policy.
10. **For developers** — build, manifest, CI, `docs/RELEASE.md`.

**Release-notes template** (`.github/RELEASE_NOTES_TEMPLATE.md`):
```markdown
## vX.Y.Z-alpha

Alpha software. **No internet permission: the app cannot and does not go online.**

### New
- In-app model import: pick the file, the app copies and verifies it — no adb, no computer.
- One-download bundle: btcg-vX.Y.Z-alpha-full.zip (APK + model + checksums).

### Install (new users)
1. Download btcg-vX.Y.Z-alpha.apk and btcg-vX.Y.Z-alpha-full.zip from this page (Wi-Fi).
2. Install the APK (the "install unknown apps" prompt is normal outside a store).
3. Open the app → "Select the model file" → pick the zip. No unzipping needed.

### Upgrading from v0.1.0
Install over the top. Your existing model is recognized and verified once on first
launch — no re-download.

### Files
| Asset | What it is |
|---|---|
| btcg-vX.Y.Z-alpha-full.zip | APK + model + SHA256SUMS.txt (primary) |
| btcg-vX.Y.Z-alpha.apk | the app only |
| btcg-vX.Y.Z-alpha-SHA256SUMS.txt | hashes of the two assets above |

### Verify
Model sha256: b139949c5bd74937ad8ed8c8cf3d9ffb1e99c866c823204dc42c0d91fa181897
Desktop: `shasum -a 256 -c` against SHA256SUMS.txt (inside the zip) or the sums asset.

### Known issues
- Uninstalling removes the model (re-import required).
- Import needs ~2.2 GB free temporarily, 1.1 GB after.
```

**`dist/README-FIRST.txt` (ships inside the zip) [new draft]:**
```
Bitcoin Custody Guardian — offline setup
1. Install: tap btcg-v0.1.1-alpha.apk (allow "install unknown apps" for your
   browser or Files app — normal for apps outside a store).
2. Open the app, tap "Select the model file", and choose this zip
   (btcg-v0.1.1-alpha-full.zip). No unzipping needed.
3. Wait about a minute while it copies and verifies. Done — the app never goes online.
Full instructions and verification hashes:
github.com/reya01/bitcoin-custody-guardian
```

**In-app text:** as drafted in §5; all in `strings.xml`.

---

## 11. Test plan

**Unit (JVM):**
- `ModelManifestParserTest` — valid file; missing/malformed fields fail closed; unknown fields ignored; multi-entry allowlist; case-insensitive filename lookup (kept from DeepSeek).
- `StreamingHasherTest` — known vectors (`"abc"` → `ba7816bf…`); chunked == whole.
- `ModelImporterTest` (fake `DocumentSource`, temp dir) — happy-path gguf; zip with STORED entry; zip with deflated entry; zip without gguf; wrong magic; size mismatch (pre-copy); hash mismatch; truncated stream; **[new]** oversized entry → fail-fast abort; write failure → `NotEnoughSpace` vs `WriteFailed`; atomic rename (final exists, `.part` gone); retry after failure; delete-source success/failure.
- `ModelRepositoryTest` — state transitions; fast-path launch check; stale `.part` cleanup; legacy-path recognition; record persistence; **[new]** two-entry manifest with `defaultModelId` flip → `UpdateRequired` for an old install.
- `ErrorCopyTest` — every `ImportError` variant maps to non-empty strings (guards the copy deck).

**Instrumented (androidTest):**
- `ImportFlowTest` — fixture: 1 MB file with GGUF magic, hash computed at test time and injected via a test-only manifest override; URI served via `FileProvider`; call `ModelViewModel.onFilePicked(uri)` directly; assert final file, record, and `Ready`. (P1-7 adds the fake-`ActivityResultRegistry` wiring test.)
- Compose tests — each `ModelState` renders its expected strings.

**Manual checklist (8 GB floor device, Android 14; repeat core items on an Android 10 device):**
1. Journey A end-to-end, timed (target ≤ 3 min total on Wi-Fi). 2. Journey B with Files-by-Google extract. 3. Journey C (gguf via phone-browser HF download; via desktop→USB). 4. Truncated gguf (`dd`-cut to ~800 MB) → instant `Incomplete`. 5. Pick a photo → `NotAModel`. 6. Pick GitHub "Source code (zip)" → `NoModelInZip`. 7. Renamed `model (1).gguf` → imports. 8. Fill storage < 1.3 GB → S2 pre-flight; free space → success. 9. Background mid-import → notification continues → success. 10. Rotate mid-import. 11. Cancel mid-import → no `.part`. 12. **Entire post-download flow in airplane mode** (screenshot for docs — the guarantee, demonstrated). 13. Delete-source ON → zip gone; OFF → remains. 14. Re-import over existing model. 15. **Upgrade path:** v0.1.0 with adb-placed model → install v0.1.1 over it → one-time verify → `Ready`, no re-import. 16. Kill app mid-import → relaunch → clean `Missing`. 17. First question "What is a seed phrase?" answered; an out-of-scope query still gets an honest no-answer. 18. `adb shell dumpsys package com.custodyguardian | grep -i internet` → empty. 19. Cold start with model present < 1 s gate (no re-hash). 20. Displayed free-space numbers match `df`. 21. **[new]** Android 13/14: deny notification permission → import still completes. 22. **[new]** Zip stored on SD card → imports (slower; estimate adapts). 23. **[new]** Cancel the picker itself → returns to Welcome, nothing changed. 24. **[new]** TalkBack walkthrough S1→S4 (P1-8 acceptance).

---

## 12. Rollout & re-verification

- **Version:** `v0.1.1-alpha`, `versionCode 2`. (Setup-only feature, model unchanged → patch bump, not minor.)
- **Re-verify before publishing:**
  1. **Hash chain:** manifest ↔ CI-verified file ↔ in-zip `SHA256SUMS.txt` ↔ release-notes model hash all identical.
  2. `unzip -l btcg-v0.1.1-alpha-full.zip` lists exactly the 4 specified flat entries with exact names.
  3. No-INTERNET CI gate green on the *signed* APK (`aapt dump permissions`).
  4. **[new]** `apksigner verify --print-certs` fingerprint == pinned cert == v0.1.0 cert (upgrade path intact).
  5. Same signing key as v0.1.0; upgrade path tested (checklist #15).
  6. Fresh-install Journey A on the 8 GB floor device.
  7. **[new]** README audit: no stale "0.1.0"/adb-first text; sizes (1.1 GB, ~2.2 GB transient) and hash correct; all links resolve.
  8. Release notes carry honest alpha labeling and the known-issues list.
- **Post-release:** no telemetry exists (no network — say so); pin the P1-9 issue template asking users to quote the exact on-screen error text from S5 for import failures.

---

## 13. Rejected alternatives (do not relitigate)

- Bundling a downloader (violates the no-network guarantee — the whole point).
- Play Asset Delivery / asset packs (requires Play).
- `MANAGE_EXTERNAL_STORAGE` (massive overreach, scary prompt).
- `ACTION_OPEN_DOCUMENT_TREE` (wrong tool — grants a directory, still can't see into zips).
- Publishing the bare GGUF as a release asset (a second canonical copy to keep in sync — HuggingFace remains the bare-file source).
- Verify-then-copy-then-verify (three passes over 1.1 GB for no security gain — D3).
- MIME/extension-based file filtering (unreliable — D9).
- Adding a DI framework for this feature (D13).