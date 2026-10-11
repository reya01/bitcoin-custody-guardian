#!/usr/bin/env bash
# make_bundle.sh — the single packaging primitive for Bitcoin Custody Guardian
# releases. Produces the full bundle zip + SHA256SUMS from a signed APK and the
# hash-verified model. Drift-proof by construction: it asserts the model's
# sha256+size against app/src/main/assets/model_manifest.json, and (when the
# Android build tools are present) the APK's signing certificate and the
# absence of the INTERNET permission, before zipping anything.
#
# Usage: ./ci/make_bundle.sh <tag> [signed-apk] [model-file]
set -euo pipefail

TAG="${1:?usage: make_bundle.sh <tag> [signed-apk] [model-file]}"
APK="${2:-app/build/outputs/apk/release/app-release.apk}"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

MANIFEST="app/src/main/assets/model_manifest.json"
CERT_PIN="ci/release_cert_fingerprint.txt"

command -v jq >/dev/null || { echo "jq required"; exit 1; }
[ -f "$APK" ] || { echo "APK not found: $APK"; exit 1; }

ID="$(jq -r .defaultModelId "$MANIFEST")"
FILE_NAME="$(jq -r --arg id "$ID" '.models[] | select(.id==$id) | .fileName' "$MANIFEST")"
EXPECT_SHA="$(jq -r --arg id "$ID" '.models[] | select(.id==$id) | .sha256' "$MANIFEST")"
EXPECT_SIZE="$(jq -r --arg id "$ID" '.models[] | select(.id==$id) | .sizeBytes' "$MANIFEST")"

MODEL="${3:-build/models/$FILE_NAME}"
[ -f "$MODEL" ] || { echo "model not found: $MODEL (run ci/fetch_and_verify_models.sh)"; exit 1; }

# --- assertion 1: model hash + size match the manifest ----------------------
ACTUAL_SHA="$(sha256sum "$MODEL" | awk '{print $1}')"
ACTUAL_SIZE="$(stat -c%s "$MODEL")"
[ "$ACTUAL_SHA" = "$EXPECT_SHA" ] || { echo "FAIL: model sha256 mismatch"; exit 1; }
[ "$ACTUAL_SIZE" = "$EXPECT_SIZE" ] || { echo "FAIL: model size mismatch ($ACTUAL_SIZE != $EXPECT_SIZE)"; exit 1; }

# --- assertion 2: signing certificate matches the pinned fingerprint --------
if command -v apksigner >/dev/null; then
  ACT_FP="$(apksigner verify --print-certs "$APK" | grep -i 'SHA-256' | head -1 | sed 's/.*: //' | tr -d ' :' | tr 'A-F' 'a-f')"
  PIN_FP="$(tr -d ' :' < "$CERT_PIN" | tr 'A-F' 'a-f')"
  [ "$ACT_FP" = "$PIN_FP" ] || { echo "FAIL: APK signing cert != pinned fingerprint"; exit 1; }
else
  echo "WARN: apksigner not available; skipping cert pin check" >&2
fi

# --- assertion 3: the APK must NOT hold INTERNET ----------------------------
if command -v aapt >/dev/null; then
  if aapt dump permissions "$APK" | grep -q 'android.permission.INTERNET'; then
    echo "FAIL: APK requests android.permission.INTERNET — core guarantee broken"
    exit 1
  fi
else
  echo "WARN: aapt not available; skipping INTERNET-permission gate" >&2
fi

# --- stage + zip ------------------------------------------------------------
STAGE="dist/stage"
rm -rf dist && mkdir -p "$STAGE"
cp "$APK" "$STAGE/btcg-$TAG.apk"
cp "$MODEL" "$STAGE/$FILE_NAME"
cp dist/README-FIRST.txt "$STAGE/README-FIRST.txt"
( cd "$STAGE" && sha256sum "btcg-$TAG.apk" "$FILE_NAME" > SHA256SUMS.txt )
( cd "$STAGE" && zip -q -0 -X "btcg-$TAG-full.zip" "btcg-$TAG.apk" "$FILE_NAME" SHA256SUMS.txt README-FIRST.txt )
mv "$STAGE/btcg-$TAG-full.zip" dist/
( cd "$STAGE" && sha256sum "btcg-$TAG-full.zip" ) > "dist/btcg-$TAG-SHA256SUMS.txt"
cp "$STAGE/SHA256SUMS.txt" "dist/SHA256SUMS-inside-bundle.txt"
rm -rf "$STAGE"

# --- assertion 4: zip contents are exactly the 4 expected flat entries ------
if command -v unzip >/dev/null; then
  COUNT="$(unzip -l "dist/btcg-$TAG-full.zip" | grep -cE '\s(btcg-.*\.apk|'"$FILE_NAME"'|SHA256SUMS\.txt|README-FIRST\.txt)$')"
  [ "$COUNT" = "4" ] || { echo "FAIL: zip contents unexpected ($COUNT expected entries)"; exit 1; }
fi

echo "OK: dist/btcg-$TAG-full.zip + dist/btcg-$TAG-SHA256SUMS.txt ready"
ls -la dist/
