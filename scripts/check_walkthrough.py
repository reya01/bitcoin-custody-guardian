"""D3 invariant lint (external-review deferred item): every walkthrough day
must carry at least one DO step (so the UI's "You did enough today" semantics
stay meaningful), and days must be contiguous 1..7. Days without DON'T items
are reported as notes only — approved read-only days (e.g. Day 3) may have none.
Run: python3 scripts/check_walkthrough.py  (exits 1 on violation)."""
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WT = os.path.join(ROOT, "app", "src", "main", "assets", "walkthrough.json")

errors = []
try:
    with open(WT, encoding="utf-8") as f:
        wt = json.load(f)
except Exception as exc:
    print(f"FAIL: cannot read walkthrough.json: {exc}")
    sys.exit(1)

days = wt.get("days") or []
if not days:
    print("FAIL: walkthrough has no days")
    sys.exit(1)

nums = [d.get("day") for d in days]
if nums != list(range(1, len(days) + 1)):
    errors.append(f"day numbering not contiguous 1..{len(days)}: {nums}")
if len(days) != 7:
    errors.append(f"expected 7 days, found {len(days)}")

notes = []
for d in days:
    label = f"day {d.get('day')}"
    if not d.get("dos"):
        errors.append(f"{label}: no DO steps — 'You did enough today' would be vacuous")
    if not d.get("donts"):
        notes.append(f"{label}: no DON'T items (ok for read-only days; verify it is one)")

if errors:
    print("FAIL: walkthrough invariant violations:")
    for e in errors:
        print(f"  - {e}")
    sys.exit(1)
if notes:
    print("notes:")
    for n in notes:
        print(f"  - {n}")
print(f"OK: walkthrough invariant holds ({len(days)} days, all with >=1 DO)")
