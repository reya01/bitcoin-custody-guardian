#!/usr/bin/env python3
"""Claims checker: keeps README/SAFETY_CASE test counts honest (MiMo review fix 4).
Counts real tests in the repo and compares against the numbers claimed in docs.
Exit 1 on mismatch. Run in CI before release.
"""
import json, os, re, sys, glob

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def count_kotlin():
    n = 0
    for f in glob.glob(f"{ROOT}/app/src/test/**/*.kt", recursive=True):
        n += len(re.findall(r"@Test\b", open(f).read()))
    return n

def count_python():
    n = 0
    for f in glob.glob(f"{ROOT}/prototype/test_*.py"):
        n += len(re.findall(r"def test_\w+", open(f).read()))
    return n

def count_corpus():
    return len(glob.glob(f"{ROOT}/corpus/*.json"))

kt, py, corpus = count_kotlin(), count_python(), count_corpus()
print(f"actual: {kt} Kotlin @Test, {py} python test functions, {corpus} corpus entries")

docs = {
    "README.md": open(f"{ROOT}/README.md").read(),
    "docs/SAFETY_CASE.md": open(f"{ROOT}/docs/SAFETY_CASE.md").read(),
}
errors = []
for path, text in docs.items():
    m = re.search(r"\*\*(\d+)/\d+\*\* JVM unit tests and \*\*(\d+)/\d+\*\* Python", text)
    if m:
        for label, claimed, actual in (("Kotlin", int(m.group(1)), kt), ("Python", int(m.group(2)), py)):
            if claimed != actual:
                errors.append(f"{path}: claims {claimed} {label} tests, actual {actual}")
    m2 = re.search(r"(\d+) Kotlin \+ (\d+) Python", text)
    if m2:
        for label, claimed, actual in (("Kotlin", int(m2.group(1)), kt), ("Python", int(m2.group(2)), py)):
            if claimed != actual:
                errors.append(f"{path}: claims {claimed} {label} tests, actual {actual}")
    if re.search(r"\b(\d+) reviewed entries", text):
        claimed = int(re.search(r"\b(\d+) reviewed entries", text).group(1))  # guarded above
        if claimed != corpus:
            errors.append(f"{path}: claims {claimed} corpus entries, actual {corpus}")

if errors:
    print("CLAIMS MISMATCH:")
    for e in errors:
        print(" -", e)
    sys.exit(1)
print("OK: documented test/corpus counts match the repo")
