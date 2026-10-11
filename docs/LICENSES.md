# License split

## Code
All source code, build scripts, and tooling in this repository are licensed
under the **MIT License** — see [LICENSE](LICENSE).

## Corpus content
The knowledge corpus (`corpus/`, `app/src/main/assets/corpus/`, and the
walkthrough JSON derived from it) is licensed as a whole under
**Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)**.

Rationale: the corpus contains verbatim excerpts from CC BY-SA sources
(notably *Mastering Bitcoin* 3rd ed. and bitcoin.org). Licensing the corpus
wholesale under CC BY-SA 4.0 satisfies the share-alike obligation for any
portion a court might treat as derivative, while costing nothing for the
entries that are original in-house text. Verbatim quotes are always clearly
attributed in the entry's `source_url` and claim sources — the verbatim-only
corpus-field rule exists so quoted material is never disguised.

Third-party sources used under facts-only, reference-only terms (e.g. River
Learn, vendor documentation, which carry no open license) are cited but not
reproduced beyond short attributed references; their license status is
recorded per source and per entry in
[CORPUS_SOURCES.md](CORPUS_SOURCES.md).

Third-party runtime components are covered in [NOTICES](NOTICES)
(llama.cpp/ggml MIT; Qwen3 model Apache-2.0).

SPDX identifiers: code `MIT`; corpus `CC-BY-SA-4.0`.
