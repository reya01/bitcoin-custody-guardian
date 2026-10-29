"""Fix 7: build the key_points manifest (sha256 of each approved key point).
Run after any corpus change: python3 gen_manifest.py

Hardened (full-repo review 2026-10-29):
 - asserts every consumed entry is reviewed=true (an unreviewed draft's
   key points must never enter the integrity manifest);
 - writes atomically (temp + os.replace) so a crash mid-write can never
   leave a truncated manifest that would fail every install closed.
"""
import hashlib, json, glob, os, sys, tempfile

CORPUS = os.environ.get("GUARDIAN_CORPUS", "/opt/data/projects/custody-guardian/corpus")

def norm(s):
    return " ".join(s.split()).lower()

manifest = {}
n = 0
unreviewed = []
for fn in sorted(glob.glob(CORPUS + "/*.json")):
    e = json.load(open(fn))
    if not e.get("reviewed"):
        unreviewed.append(os.path.basename(fn))
        continue
    for kp in e.get("key_points") or []:
        h = hashlib.sha256(norm(kp).encode()).hexdigest()
        manifest[h] = {"entry": e.get("title", ""), "file": fn.split("/")[-1]}
        n += 1

if unreviewed:
    print(f"manifest: SKIPPED unreviewed entries ({len(unreviewed)}): {', '.join(unreviewed)}")

out = os.path.join(os.path.dirname(__file__) or ".", "key_points_manifest.json")
fd, tmp = tempfile.mkstemp(dir=os.path.dirname(out) or ".")
with os.fdopen(fd, "w") as f:
    json.dump(manifest, f, indent=0)
os.replace(tmp, out)
print(f"manifest: {n} key points -> {out}")
