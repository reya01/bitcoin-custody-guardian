"""run_retrieval_eval.py - retrieval-layer eval (review action item).

For each T06 item: retrieve chunks via compose(), then check whether the
must_include target phrases are (near-)present in the RETRIEVED TEXT. This
isolates the deterministic pipeline from the LLM: if retrieval misses,
no model can pass. Per the review: success criterion >= 90% phrase coverage.
"""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from guardian_core import compose
from run_eval import normalize, match_include

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EVAL = os.path.join(ROOT, 'eval', 'T06-walkthrough.jsonl')
CORPUS = os.path.join(ROOT, 'corpus')
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'retrieval_eval.json')

items = [json.loads(l) for l in open(EVAL) if l.strip()]
results = []
for it in items:
    comp = compose(it['prompt'], CORPUS, top_k=5)
    corpus_text = normalize(' '.join(c['text'] for c in comp.retrieved))
    per = []
    for ph in it['must_include']:
        how, _ = match_include(corpus_text, ph)
        per.append({'phrase': ph, 'result': how})
    hit = sum(1 for p in per if p['result'] != 'fail')
    results.append({
        'id': it['id'], 'category': it['category'],
        'retrieved': [r['entry_id'] for r in comp.retrieved],
        'phrase_hits': hit, 'phrases_total': len(per),
        'phrases': per,
    })
    print('%s retrieval-phrase-coverage %d/%d' % (it['id'], hit, len(per)), flush=True)

json.dump(results, open(OUT, 'w'), indent=1)
tot_h = sum(r['phrase_hits'] for r in results)
tot_p = sum(r['phrases_total'] for r in results)
full = sum(1 for r in results if r['phrase_hits'] == r['phrases_total'])
print('TOTAL phrase coverage: %d/%d (%.0f%%); items with full coverage: %d/%d'
      % (tot_h, tot_p, 100.0 * tot_h / tot_p, full, len(results)))
