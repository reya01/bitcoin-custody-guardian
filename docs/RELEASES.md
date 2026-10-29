# Release verification (hashes live in-repo, not just on the release page)

Verify BEFORE installing: a hash you copy from the same page as the file proves
nothing. Copy these values into a terminal (`sha256sum <file>`) and compare
character for character. The signing-cert fingerprint is pinned in-repo at
`ci/release_cert_fingerprint.txt` and asserted by `ci/make_bundle.py` at
packaging time.

| Release | Artifact | SHA-256 |
|---|---|---|
| v0.1.1-alpha | btcg-v0.1.1-alpha.apk | `cd10ded5dd1f14481fbca99333214942881c18480ee5fe5a77a8ad887a50ae6b` |
| v0.1.1-alpha | btcg-v0.1.1-alpha-full.zip | `cc011e139feced2554fe7061ef5f80809e03d86aff8d71f1ca8433fe391c2f8c` |
| v0.1.1-alpha | btcg-v0.1.1-alpha-SHA256SUMS.txt | `caaa4e8c887b2136a30655de577a404f50117d34b01b74e37739f5c15f0507d3` |
| v0.1.1-alpha | Qwen3-1.7B GGUF (sideloaded) | `b139949c5bd74937ad8ed8c8cf3d9ffb1e99c866c823204dc42c0d91fa181897` |
| v0.1.0-alpha | btcg-0.1.0-alpha.apk | `1d71fc17b187661de5ea46e102610802c0bf2eb4cbca9a723155bd5a5c57f642` |

Artifact naming note: filenames carry the tag's `v` from v0.1.1-alpha onward
(`btcg-v0.1.1-alpha.apk`); the v0.1.0-alpha asset predates that convention
(`btcg-0.1.0-alpha.apk`, no `v` in the FILE name). Historical asset names are
not renamed — the tag column is authoritative.

The signing certificate (all alpha releases):
`079cf431…` — full value in `ci/release_cert_fingerprint.txt` (never paste a
fingerprint you got from a chat message; read it from the repo).

Install steps: download `btcg-<tag>-full.zip` from the GitHub release page,
verify the APK and GGUF hashes against this table, install the APK, then use
"Select the model file" to import the GGUF from the same zip — the app
verifies its SHA-256 again on import and refuses a mismatch.
