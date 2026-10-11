#!/usr/bin/env bash
# fetch_and_verify_models.sh — downloads each manifest model from its
# ciDownloadUrl and verifies sha256 + byte size against the manifest.
# ANY mismatch fails the job. Used by CI (release + weekly hash canary) and
# locally to populate build/models/ before make_bundle.sh.
#
# Usage: ./ci/fetch_and_verify_models.sh [manifest] [outdir]
set -euo pipefail

MANIFEST="${1:-app/src/main/assets/model_manifest.json}"
OUT="${2:-build/models}"

command -v jq >/dev/null || { echo "jq required"; exit 1; }
command -v curl >/dev/null || { echo "curl required"; exit 1; }
mkdir -p "$OUT"

COUNT="$(jq -r '.models | length' "$MANIFEST")"
for i in $(seq 0 $((COUNT - 1))); do
  ID="$(jq -r ".models[$i].id" "$MANIFEST")"
  FILE="$(jq -r ".models[$i].fileName" "$MANIFEST")"
  URL="$(jq -r ".models[$i].ciDownloadUrl" "$MANIFEST")"
  SHA="$(jq -r ".models[$i].sha256" "$MANIFEST")"
  SIZE="$(jq -r ".models[$i].sizeBytes" "$MANIFEST")"
  DEST="$OUT/$FILE"

  echo "[$ID] $URL"
  if [ -f "$DEST" ] && [ "$(stat -c%s "$DEST")" = "$SIZE" ] && \
     [ "$(sha256sum "$DEST" | awk '{print $1}')" = "$SHA" ]; then
    echo "[$ID] already present and verified"
    continue
  fi
  curl -fL --retry 3 -o "$DEST" "$URL"
  ACT_SHA="$(sha256sum "$DEST" | awk '{print $1}')"
  ACT_SIZE="$(stat -c%s "$DEST")"
  [ "$ACT_SHA" = "$SHA" ] || { echo "FAIL[$ID]: sha256 $ACT_SHA != $SHA"; rm -f "$DEST"; exit 1; }
  [ "$ACT_SIZE" = "$SIZE" ] || { echo "FAIL[$ID]: size $ACT_SIZE != $SIZE"; rm -f "$DEST"; exit 1; }
  echo "[$ID] verified ($ACT_SIZE bytes)"
done
echo "OK: all manifest models verified in $OUT"
