# License split

## Code
All source code, build scripts, and tooling in this repository are licensed
under the **MIT License** — see [LICENSE](../LICENSE).

## Corpus content
The knowledge corpus (`corpus/`, `app/src/main/assets/corpus/`, and the
walkthrough JSON derived from it) is written as **original expression of facts
drawn from cited third-party sources** — it is not a reproduction of those
sources. A machine check (8-gram overlap scan, run at review time) found no
verbatim passages of meaningful length from any non-open-licensed source.

Per-source status (full detail in [CORPUS_SOURCES.md](CORPUS_SOURCES.md)):

- **Mastering Bitcoin, 3rd ed. (Antonopoulos, Harding)** — CC BY-SA 4.0.
  Facts and technical content drawn from it; attribution given per entry
  (authors, license notice + URI
  https://creativecommons.org/licenses/by-sa/4.0/).
- **bitcoin.org** — MIT license (per the bitcoin.org source repository);
  attributed, MIT notice retained in [NOTICES](../NOTICES).
- **BIP documents** (e.g. BIP-32) — per-BIP licenses (BIP-32 is
  BSD-2-Clause); attributed, copyright/disclaimer notices retained in
  [NOTICES](../NOTICES).
- **River Learn and vendor documentation (Ledger, BitBox/Shift Crypto,
  Blockstream Jade, SeedSigner, Krux, etc.)** — these carry **no open-content
  license**. They are used as **facts-only references with attribution**: the
  corpus states the underlying facts in its own words and cites the source
  section. No open license is claimed over their text, and their expression is
  not reproduced. Before distribution channels that scrutinize content
  licensing (e.g. F-Droid), a legal review of this facts-only approach is
  planned — recorded here so the claim and its limits are visible.

The project-original corpus portions are intended for **CC BY-SA 4.0**; a
whole-corpus license declaration will accompany the legal review above.

SPDX identifiers: code `MIT`.

Third-party runtime components are covered in [NOTICES](../NOTICES)
(llama.cpp/ggml MIT; Qwen3 model Apache-2.0).
