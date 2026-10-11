## Verdict: CLEAN

No real personal information or live secrets found in any blob across full history. All key-material hits are public test vectors or published book examples; no keystore, .env, API-key, phone, address, SSN, or routable-IP content ever entered the tree. A few hardening notes below.

## Sensitive items (path, what, recommended purge step)

None.

## False positives (grouped, one line each)

- `keystore` × `.gitignore` (2 dup hits): Ignore rule excludes keystores; none ever committed.
- `xprv_like` × `app/src/test/.../CoreLogicTest.kt` (2 hits): Detector test fixture; mutated public BIP32 test vector.
- `wif_like` × `CoreLogicTest.kt`: Canonical Bitcoin Wiki example WIF, publicly documented.
- `wif_like` × `docs/references/ch04_keys.adoc` (4 dup hits): Mastering Bitcoin chapter 4 example keys, published.
- `xprv_like` × `docs/references/ch05_wallets.adoc`: Mastering Bitcoin chapter 5 example xprv, published.
- `xprv_like` × `prototype/test_guardian_core.py` (6 dup hits): BIP32 spec test vector 1, publicly documented.

## Notes (judgment calls worth a human look)

- **`.gitignore` `!debug.keystore` negation** — the file ignores `*.keystore` but then un-ignores `debug.keystore`. Nothing is tracked today, but this is a footgun for a future `git add .`. Recommend deleting the negation line. (Debug keystores are conventionally low-secrecy, hence note not sensitive.)
- **Third committer identity `mh15 <86735675+reya01@users.noreply.github.com>` (13 commits)** — beyond the two sanctioned pseudonyms, but it's a GitHub noreply address (no real email exposed) and maps to the already-public repo account `reya01`. Only action: confirm "mh15" isn't a handle that de-anonymizes the author elsewhere.
- **Suspicious filenames `T02-river-private-key.json` (both `corpus/` and `app/src/main/assets/corpus/` copies) and `docs/references/river-private-key.txt`** — names sound like actual key material, but they sit in a family of River Financial support-article scrapes (`river-seed-phrase.txt`, `river-cold-storage.txt`, `river-multisig.txt`, `river-self-custody.txt`), i.e., educational content *about* private keys. No key-material regex hit their contents. Worth a one-time human spot-check of content.
- **T02 fictional seed phrases (known-intentional carve-out)** — the carve-out depends on the fiction labeling being present; verify the shipped corpus/eval copies actually carry the "fictional scam persona" labeling convention.
- **`gradle.properties` is tracked** — standard build config; no pattern hits, but verify no signing passwords or tokens were ever added to it in history.
- **Negative results worth stating explicitly:** zero hits across all history for real names/emails (beyond pseudonyms + noreply), phone numbers, street addresses, SSNs, telegram-bot-token numerics, `sk_`-style keys, and routable IPs; `*.env` and `local.properties` are ignored and no such blob was ever committed; `docs/references/*` are scrapes of public docs (Mastering Bitcoin, River, FTC, Lopp, etc.) with no commenter/author PII surfaced.