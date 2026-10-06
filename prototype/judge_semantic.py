"""judge_semantic.py - GLM-5.3 rubric judge for failed eval items.

Both external reviews recommended semantic grading for topical sets while
keeping strict substring grading as the release gate. This script takes one
or more eval_results JSON files, finds failed items, and asks GLM-5.3 (Sail
flex) per item: does the answer convey the same guidance as each failed
must_include phrase? Adds 'semantic' field per phrase:
  'same' | 'partial' | 'different' | 'refusal_mismatch'
and a 'semantic_passed' bool (all 'same', no must_not_include violations).
"""
import json, os, sys, glob
sys.path.insert(0, "/opt/data/scripts")
from sail_flex import call_flex

files = sys.argv[1:]
for fn in files:
    rs = json.load(open(fn))
    if rs and 'semantic_checked' in rs[0]:
        print(fn, 'already judged; skip'); continue
    failed = [r for r in rs if not r['passed'] and not r['answer'].startswith('ERROR')]
    print(fn, '- judging', len(failed), 'failed items')
    for r in failed:
        mi_failed = [m['phrase'] for m in r['must_include'] if m['result'] == 'fail']
        if not mi_failed and not r['must_not_include_hits']:
            # refusal mismatch only
            r['semantic'] = {}
            r['semantic_passed'] = False
            continue
        prompt = ("You grade a Bitcoin-safety assistant's answer against required guidance "
                  "phrasings. For each REQUIRED PHRASING, judge whether the ANSWER conveys "
                  "the same guidance (same meaning and actionable content), even if worded "
                  "differently. Reply with one JSON object only:\n"
                  '{"judgements": [{"phrase": "...", "verdict": "same|partial|different"}]}\n\n'
                  "ANSWER:\n" + r['answer'][:1800] + "\n\nREQUIRED PHRASINGS:\n")
        for p in mi_failed:
            prompt += "- " + p + "\n"
        try:
            out = call_flex("zai-org/GLM-5.3", [{"role": "user", "content": prompt}],
                            max_completion_tokens=32000)
            m = json.loads(out[out.find('{'):out.rfind('}')+1])
            sem = {}
            for j in m.get('judgements', []):
                sem[j['phrase']] = j['verdict']
        except Exception as e:
            sem = {p: 'error' for p in mi_failed}
        r['semantic'] = sem
        r['semantic_passed'] = bool(sem) and all(v == 'same' for v in sem.values())
    json.dump(rs, open(fn, 'w'), indent=1)
    n = len(rs); strict = sum(r['passed'] for r in rs)
    sfail = [r for r in rs if not r['passed']]
    sem_p = sum(1 for r in sfail if r.get('semantic_passed'))
    print(fn, 'strict %d/%d; semantic recover: %d' % (strict, n, sem_p))
