## Addendum 12 — Bitkey corpus entry rerun (hosted hybrid, p8sail, 2026-10-11)

**Purpose:** measure whether the new Bitkey learning-hub entry (T09T05T08-bitkey-
2of3-multisig, approved 2026-10-11; corpus 51/51 reviewed) moves the eval scores.

**Run config:** 5 slices (T01/T02/T08/T10/T06), ANSWER_MODE=hybrid, backend =
**sail** (GLM-5.3 via Sail flex, honestly labeled per record; max output 32K).
Rationale: llama-server on the build VPS became unstable today (requests stalled
across four configurations after the host RAM change) — the sail-backend run
measures the *pipeline* (retrieval + verbatim safety layer + veto + grading),
matching the methodology of the hosted p2 numbers. It is NOT the on-device 1.7B
score, which remains 31/105 (p7) and will be re-measured on-device (Add. 11).

**Results**

| Slice | strict (run_eval) | FINAL (policy gate) |
|---|---|---|
| T01 | 0/25 | 9/25 |
| T02 | 0/30 | 5/30 |
| T08 | 2/25 | 7/25 |
| T10 | 1/25 | 11/25 |
| **Topical FINAL** | 3/105 | **32/105** |
| T06 (strict) | 35/40 | — |

**Findings**

1. **Topical FINAL 32/105 vs 31/105 (p7 local hybrid):** the Bitkey entry is
   score-neutral (+1, within judge noise). Retrieval measure: the entry is
   retrieved for 6/145 eval questions, concentrated in hardware/multisig/KYC
   items — no question hijacking. The entry adds coverage without distorting
   the corpus.
2. **T06 strict 35/40 vs 19/40 (p2 hosted):** the walkthrough lane improved
   substantially since Addendum 9 — attributable to the item-1 schema work and
   router changes (the Bitkey entry does not target T06 walkthrough items).
   The pre-registered beta gate for T06 is >=28/40: **now met** (on this
   hosted measurement; on-device confirmation pending).
3. **Topical FINAL did NOT meet the >=50/105 beta gate.** The gate remains
   open; the binding constraint is still answer quality of the 1.7B-class
   model, to be addressed by the on-device eval / model-floor decision.
4. Backends across 145 records (honest labels): sail_glm53+extractive_safety
   126, sail_glm53 12, deterministic_secret_refusal 4, scam_rules+sail 3.
   Secret refusals fired correctly on every secret-bearing prompt.

**Artifacts:** p8sail_T{01,02,08,10,06}.json (in /opt/data/work/answerer/ and
committed under prototype/eval/), judge log j10_sail2.log, judge timeout
patch (120 s/call; a timed-out judge call counts as strict-fail — never
inflates the score).
