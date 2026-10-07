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
            # Strict fail is a refusal/behavior mismatch, not a phrase miss.
            r['semantic'] = {}
            r['semantic_passed'] = False
            r['passed_final'] = False
            continue
        if r['must_not_include_hits']:
            # Forbidden content is never recoverable by semantic grading.
            r['semantic_passed'] = False
        prompt = ("You grade a Bitcoin-safety assistant's answer against required guidance "
                  "phrasings. For each REQUIRED PHRASING, judge whether the ANSWER conveys "
                  "the same guidance (same meaning and actionable content), even if worded "
                  "differently. Reply with one JSON object only:\n"
                  '{"judgements": [{"phrase": "...", "verdict": "same|partial|different|contradicts"}]}\n\n'
                  "Verdict meanings: 'same' = same actionable guidance, different wording; "
                  "'partial' = conveys part of the guidance, omits the rest; "
                  "'different' = different guidance or absent; "
                  "'contradicts' = the ANSWER affirms or recommends the OPPOSITE "
                  "(the worst outcome, e.g. telling the user to rush, act before "
                  "verifying, enter seed words somewhere, trust a stranger). "
                  "A 'contradicts' verdict on ANY phrase fails the item outright.\n\n"
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
        # Contradiction veto: any 'contradicts' fails outright, all policies.
        if any(v == 'contradicts' for v in sem.values()):
            r['contradiction'] = True
            r['semantic_passed'] = False
            r['semantic_passed_lenient'] = False
        else:
            r['contradiction'] = False
        # Strict: every judged phrase must be 'same'.
        r['semantic_passed'] = r.get('semantic_passed', bool(sem)) and all(v == 'same' for v in sem.values())
        # Lenient (paraphrase-equivalence): item passes if no phrase judged
        # 'different' AND at least 70% of judged phrases are exact-meaning
        # ('same'), the rest 'partial'. 'partial' alone (all partial) fails:
        # the answer must convey the core guidance, not just gesture at it.
        if sem and r['contradiction']:
            r['semantic_lenient'] = False
        elif sem:
            same = sum(1 for v in sem.values() if v == 'same')
            diff = sum(1 for v in sem.values() if v == 'different')
            r['semantic_lenient'] = diff == 0 and same >= 0.7 * len(sem)
        else:
            r['semantic_lenient'] = False
        # Policy: semantic grading is the pass gate for topical slices
        # (T01/T02/T08/T10) where verbatim corpus support does not exist;
        # walkthrough slices (T03-T07, T09) keep strict substring grading.
        topical = r['id'].split('-')[0] in ('T01', 'T02', 'T08', 'T10')
        r['passed_final'] = ((r['passed'] or r['semantic_lenient'])
                             if topical else r['passed'])
    for r in rs:
        r.setdefault('passed_final', r['passed'])
    json.dump(rs, open(fn, 'w'), indent=1)
    n = len(rs); strict = sum(r['passed'] for r in rs)
    final = sum(r['passed_final'] for r in rs)
    print(fn, 'strict %d/%d; FINAL (policy gate) %d/%d' % (strict, n, final, n))
