# Building Bitcoin Custody Guardian (Android skeleton)

Kotlin 2.0 + Jetpack Compose, minSdk 29, targetSdk 35. **No INTERNET
permission, ever — this is architectural (see `app/src/main/AndroidManifest.xml`).**

## Prerequisites

- JDK 17 (Temurin recommended)
- Android SDK with platform 35 and build-tools; set `ANDROID_HOME` or
  `local.properties` (`sdk.dir=…`) — do NOT commit `local.properties`
- Android Studio Ladybug (2024.2.1)+ also works out of the box

The Gradle wrapper jar is intentionally not committed in this skeleton.
Generate it once with any Gradle ≥ 8.9:

```bash
gradle wrapper --gradle-version 8.9
```

## Build

```bash
./gradlew assembleDebug        # debug APK -> app/build/outputs/apk/debug/
./gradlew assembleRelease      # requires signingConfig set up OUTSIDE this repo
```

Release signing: keys are generated once, stored on a hardware token, never in
CI, never in this repo. The fingerprint is published in the signed release
notes and pinned for reproducible verification (PRODUCT_SPEC §9).

## Manifest security checklist (verify after every change)

```bash
# 1. NO android.permission.INTERNET anywhere in the manifest:
grep -rn "INTERNET" app/src/main/AndroidManifest.xml   # must output NOTHING
# 2. NO permissions at all:
grep -n "<uses-permission" app/src/main/AndroidManifest.xml   # must output NOTHING
# 3. Only the launcher activity is exported:
grep -n "exported=\"true\"" app/src/main/AndroidManifest.xml  # must show MainActivity only
# 4. Belt-and-braces on the built APK (after a build):
aapt dump permissions app/build/outputs/apk/debug/*.apk       # must print NOTHING
```

Any hit above is a build-blocker: revert and investigate.

## Assets bundled in the APK

- `app/src/main/assets/corpus/*.json` — curated corpus entries (versioned)
- `app/src/main/assets/walkthrough.json` — 7-day inheritance walkthrough
- `app/src/main/assets/bip39_wordlist.txt` — official BIP-39 English wordlist
  (2,048 words) used by `MemoryGuards.detectSeedPhrase()`

## Repo hygiene

- No keystores, no signing material, no tokens, no `.env` in this repo.
- `tools/make_walkthrough_json.py` regenerates `walkthrough.json` from
  INHERITANCE_WALKTHROUGH.md.
