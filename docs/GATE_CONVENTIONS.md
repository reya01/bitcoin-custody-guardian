# Gate-run conventions (evaluation integrity)

These conventions apply to every **gate run** — any eval whose numbers are
cited in README, release notes, or the beta gate (per the iPhone plan §0).

## Pinned judge
- Judge model, exact model version string, and the judge prompt text (or its
  sha256) are recorded in the header of every gate run's addendum.
- Judge temperature is always 0.
- The semantic-judge prompt hash is recomputed at run time and printed in the
  run log; a change in the prompt invalidates cross-addendum comparisons
  unless explicitly noted.

## Pinned answerer
- Answerer model id and temperature (0) recorded per run.
- The pipeline mode (paraphrase / hybrid / extractive) and ANSWER_MODE env
  value are recorded; purity report output accompanies gate runs.

## Human agreement subsample
- Before a gate threshold (e.g. the beta gate's FINAL ≥ 50/105) is treated as
  pass/fail, a ~30-item human-graded subsample is compared against the judge;
  agreement is reported alongside the score.

## Contamination
- Eval items are drawn from the corpus's own sources; retrieval-coverage
  numbers are therefore upper bounds. Refusal behavior is measured by the
  separate out-of-corpus refusal set (`eval/refusal_set.jsonl`), which is
  never drawn from corpus sources.

## Current judge (as of Addendum 10)
- Judge: `zai-org/GLM-5.3` (Sail flex, api.sailresearch.com/v1 — model id
  verified against that catalog), temperature 0, prompt in
  `prototype/judge_semantic.py` (extractive-aware variant added Addendum 10).
- Gate thresholds and the pre-registered beta gate live in
  `docs/plans/iphone_plan.md` §0 ("Beta gate") — that section is the product
  gate, not an iOS-only one.
