"""Fix 9 Phase 0: per-backend purity report for an eval results JSON.
Usage: python3 purity_report.py results.json [...]
Flags backend contamination (non-extractive backends in an extractive run)."""
import json, sys
from collections import Counter

for fn in sys.argv[1:]:
    rs = json.load(open(fn))
    backends = Counter()
    for r in rs:
        b = r.get("backend", "?")
        if "extractive" in b or "deterministic" in b:
            backends[b] += 1
        else:
            backends["CONTAMINATION:" + b.split("(")[0].strip()] += 1
    n = len(rs)
    clean = sum(v for k, v in backends.items() if not k.startswith("CONTAMINATION"))
    final = sum(1 for r in rs if r.get("passed_final"))
    final_clean = sum(1 for r in rs if r.get("passed_final")
                      and "extractive" in r.get("backend", "") + "deterministic")
    print(f"{fn.split('/')[-1]}: {n} items, purity {clean}/{n}")
    for k, v in backends.most_common():
        print(f"   {v:3d}  {k}")
    print(f"   FINAL: {final}/{n} (purity-clean FINAL: {final_clean}/{n})")
