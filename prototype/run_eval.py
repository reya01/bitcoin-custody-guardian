"""run_eval.py — run all T06-walkthrough eval items through compose() + answer_step()
and auto-grade. Writes per-item JSON results to eval_results.json and a human
report to EVAL_RESULTS.md.

Grading rules (documented synonym allowance):
- Normalization: lowercase, strip punctuation to spaces, collapse whitespace,
  apply a documented synonym table (don't->do not, etc.) to BOTH phrase and
  answer text before matching.
- must_include: PASS if the normalized phrase appears as a substring
  ("exact"); else PASS as "synonym" if >= 80% of the phrase's content tokens
  (words of length >= 3, stopword-light) appear in the answer; else FAIL.
- must_not_include: FAIL if the normalized phrase appears as a substring.
- refusal_expected=true items: PASS if the answer reads as a refusal
  (regex: cannot|can't|out of scope|offline|will not|unable to).
- Refusal leakage on non-refusal items is recorded as a flag.
An item PASSES if all must_include pass, no must_not_include trips, and the
refusal expectation is met.
"""

from __future__ import annotations

import json
import os
import re
import sys
import time
from collections import Counter, defaultdict

_HERE = os.path.dirname(os.path.abspath(__file__))
_ROOT = os.path.dirname(_HERE)
sys.path.insert(0, _HERE)

from guardian_core import compose  # noqa: E402
from answer_step import answer_step, LlamaServerError  # noqa: E402

EVAL_PATH = os.path.join(_ROOT, "eval", "T06-walkthrough.jsonl")
CORPUS = os.path.join(_ROOT, "corpus")
RESULTS_JSON = os.path.join(_HERE, "eval_results.json")

SYNONYMS = {
    "don't": "do not", "won't": "will not", "can't": "can not",
    "isn't": "is not", "shouldn't": "should not", "couldn't": "could not",
    "you're": "you are", "it's": "it is", "that's": "that is",
    "seed phrase": "seed words", "recovery phrase": "seed phrase",
    "word list": "wordlist", "plug it in": "connect it",
    "wipe": "erase", "reset": "erase", "hang up": "stop contact",
}
_STOP = set(
    "a an and are as at be by for from has have how i in is it its of on or "
    "that the this to was what when where which who will with you your do "
    "does my me we our not no yet so if then".split()
)
_TOKEN = re.compile(r"[a-z0-9]+")
DASHES = str.maketrans({c: " " for c in "—–-—/;:,.'\"()[]{}?!*"})


def normalize(text: str) -> str:
    t = text.lower().translate(DASHES)
    for k, v in SYNONYMS.items():
        t = t.replace(k, v)
    t = _TOKEN.sub(lambda m: m.group(0), t)
    return re.sub(r"\s+", " ", t).strip()


def _content_tokens(norm_phrase: str):
    return [t for t in norm_phrase.split() if len(t) >= 3 and t not in _STOP]


def match_include(answer_norm: str, phrase: str):
    p = normalize(phrase)
    if p and p in answer_norm:
        return "exact", None
    toks = _content_tokens(p)
    if toks:
        present = [t for t in toks if t in answer_norm]
        frac = len(present) / len(toks)
        if frac >= 0.8:
            return "synonym", (frac, [t for t in toks if t not in present])
    return "fail", None


def match_exclude(answer_norm: str, phrase: str) -> bool:
    return normalize(phrase) in answer_norm


REFUSAL_RE = re.compile(
    r"^(?:i (?:can ?not|can't|am unable|do not have)|the app (?:is offline|can ?not|can't)|"
    r"i (?:will not|won't) (?:answer|help|provide)|out of scope|outside (?:the|my) (?:app|corpus|scope)|"
    r"this question is out of|cannot answer that|can't answer that)",
    re.I,
)


def looks_like_refusal(answer: str) -> bool:
    return bool(REFUSAL_RE.search(answer))


def grade(item: dict, answer: str) -> dict:
    a_norm = normalize(answer)
    mi, mni = [], []
    for ph in item["must_include"]:
        how, _ = match_include(a_norm, ph)
        mi.append({"phrase": ph, "result": how})
    for ph in item["must_not_include"]:
        if match_exclude(a_norm, ph):
            mni.append(ph)
    refusal = looks_like_refusal(answer)
    ref_ok = (item["refusal_expected"] == refusal) or (
        item["refusal_expected"] is False and not refusal
    )
    passed = (
        all(m["result"] in ("exact", "synonym") for m in mi)
        and not mni
        and (refusal if item["refusal_expected"] else not refusal)
    )
    return {
        "passed": passed,
        "must_include": mi,
        "must_not_include_hits": mni,
        "refusal_detected": refusal,
        "refusal_ok": ref_ok,
    }


def main(limit=None, backend="auto"):
    items = [json.loads(l) for l in open(EVAL_PATH) if l.strip()]
    if limit:
        items = items[:limit]
    results = []
    if os.path.exists(RESULTS_JSON):  # resume: keep completed items
        try:
            done = json.load(open(RESULTS_JSON))
            results = [r for r in done if not r["answer"].startswith("ERROR")]
            print("resuming with %d completed" % len(results), flush=True)
        except Exception:
            results = []
    have = {r["id"] for r in results}
    for i, item in enumerate(items):
        t0 = time.time()
        comp = compose(item["prompt"], CORPUS, top_k=8)
        try:
            out = answer_step(item["prompt"], comp, backend=backend)
            answer, used = out["answer"], out["backend"]
        except LlamaServerError as e:
            answer, used = "ERROR: %s" % e, "error"
        g = grade(item, answer)
        rec = {
            "id": item["id"],
            "category": item["category"],
            "prompt": item["prompt"],
            "answer": answer,
            "backend": used,
            "retrieved_ids": [r["entry_id"] for r in comp.retrieved],
            "elapsed_s": round(time.time() - t0, 1),
            **g,
        }
        if rec["id"] in have:
            continue
        results.append(rec)
        print(
            "%s %s %s (%.0fs, %s)"
            % (rec["id"], "PASS" if g["passed"] else "FAIL", item["category"],
               rec["elapsed_s"], used),
            flush=True,
        )
        # incremental save so an interrupt keeps progress
        with open(RESULTS_JSON, "w") as fh:
            json.dump(results, fh, indent=1)
    n = len(results)
    p = sum(r["passed"] for r in results)
    print("\nTOTAL: %d/%d (%.0f%%)" % (p, n, 100.0 * p / n))


if __name__ == "__main__":
    lim = int(sys.argv[1]) if len(sys.argv) > 1 else None
    be = sys.argv[2] if len(sys.argv) > 2 else "auto"
    main(limit=lim, backend=be)
