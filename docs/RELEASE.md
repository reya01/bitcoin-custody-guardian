# Release runbook (maintainer)

Signing and publishing are **offline on purpose**: the release signing key
never enters this repo or CI. CI (`release-verify` workflow) only verifies the
pinned models, runs the unit tests, builds an unsigned APK, and gates on the
no-INTERNET permission. Everything below happens on the maintainer's machine.

## One-time setup
- Keystore: `alpha.keystore` + `alpha.pass` live OFF-REPO (see the alpha key
  backup policy — losing it breaks upgrades for existing users).
- `ci/release_cert_fingerprint.txt` holds the SHA-256 fingerprint of that
  certificate; `ci/make_bundle.sh` refuses to package an APK signed with any
  other key. Fingerprint changes require a release-notes warning (upgrades
  would break).

## Per release
1. Bump `versionCode`/`versionName` in `app/build.gradle.kts` (alpha line: odd
   suffix `-alpha`).
2. `./ci/fetch_and_verify_models.sh` — downloads/verifies the pinned model(s)
   into `build/models/` (hash + size asserted against the manifest).
3. `./gradlew assembleRelease` then sign offline with the alpha keystore:
   `apksigner sign --ks alpha.keystore --out app-release.apk
   app/build/outputs/apk/release/app-release-unsigned.apk`
4. `./ci/make_bundle.sh v0.1.2-alpha` (adjust tag) — asserts model hash/size,
   cert pin, and no-INTERNET, then produces:
   - `dist/btcg-<tag>-full.zip` (APK + model + SHA256SUMS.txt + README-FIRST)
   - `dist/btcg-<tag>-SHA256SUMS.txt`
5. Create the GitHub release with the tag; upload the two artifacts **plus**
   the standalone signed APK; paste the hashes from
   `dist/btcg-<tag>-SHA256SUMS.txt` into the release notes (see
   `.github/RELEASE_NOTES_TEMPLATE.md`).
6. Update the README's download links to the new tag.

## Why the manifest is the source of truth
`app/src/main/assets/model_manifest.json` is consumed by the app (hash pin +
import verification), by `fetch_and_verify_models.sh` (download + verify), and
by `make_bundle.sh` (package + assert). Changing the model = one JSON edit +
one tag; artifacts cannot drift from the app's expectations because every
path reads the same file.
