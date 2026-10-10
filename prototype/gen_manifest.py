"""Fix 7: build the key_points manifest (sha256 of each approved key point).
Run after any corpus change: python3 gen_manifest.py"""
import hashlib, json, glob, re, os

CORPUS = os.environ.get("GUARDIAN_CORPUS", "/opt/data/projects/custody-guardian/corpus")

def norm(s):
    return " ".join(s.split()).lower()

manifest = {}
n = 0
for fn in sorted(glob.glob(CORPUS + "/*.json")):
    e = json.load(open(fn))
    for kp in e.get("key_points") or []:
        h = hashlib.sha256(norm(kp).encode()).hexdigest()
        manifest[h] = {"entry": e.get("title", ""), "file": fn.split("/")[-1]}
        n += 1
out = os.path.join(os.path.dirname(__file__) or ".", "key_points_manifest.json")
json.dump(manifest, open(out, "w"), indent=0)
print(f"manifest: {n} key points -> {out}")
