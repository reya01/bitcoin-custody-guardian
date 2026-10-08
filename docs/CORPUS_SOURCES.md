# Knowledge Corpus — Source List & Curation Plan
**Bitcoin Custody Guardian · Draft v0.1 · 2026-10-06**

The corpus is the product. This file lists what goes in, where it comes from,
how it gets transformed, and the policy for naming real companies.

---

## 1. Corpus structure (topics)

| ID | Topic area | Examples of what it covers |
|---|---|---|
| T01 | Bitcoin fundamentals for novices | what bitcoin is, wallets vs exchanges, keys, "not your keys not your coins" |
| T02 | Key material taxonomy | seed phrase, passphrase (25th word), xprv/xpub, descriptors, WIF, SLIP-39 shares — what each is, what it looks like, its sensitivity class |
| T03 | Wallet types | hardware wallets, multisig, singlesig, watch-only, mobile wallets, Muun-style 2-of-2 |
| T04 | Hardware wallets per-vendor guides | Ledger, BitBox02, Jade, SeedSigner, Krux, Coldcard, Trezor, Bitkey — device inventory, what the buttons/words on screen mean, what NOT to do (factory reset = data loss if no backup) |
| T05 | Multisig & collaborative custody | 2-of-3 patterns, descriptor files (BIP-380), coordinators (Sparrow, Specter, Nunchuk), Casa/Unchained-style services, why the descriptor is essential |
| T06 | Inheritance & estates | the "first 7 days" walkthrough, letter-of-instruction anatomy, executors, probate basics (US), taxes intro, what to do while grieving, scam targeting of inheritors |
| T07 | Time-locks & advanced recovery | Miniscript/Liana-style recovery paths, OP_CSV/CLTV in plain language, dead-man switches, Shamir (SLIP-39) vs multisig distinction |
| T08 | Scam & social-engineering defense | recovery-agent scams, fake support, seed-phishing, urgency pressure, impersonation of well-known figures, "why an heir is a prime target" |
| T09 | Safe next steps | moving to watch-only verification, choosing a temporary custodial exchange vs staying self-custody, exchange account basics, selling small amounts safely |
| T10 | Jargon decoder | term → plain-language rewrite table (the in-app decoder reads from this slice) |
| T11 | Directory (named services) | per §3 below — opinionated, evidence-backed listings |
| T12 | Common mistakes & recovery stories | anonymized failure-mode catalog: wiped device with no backup, mistyped passphrase, lost descriptor, seed photographed and leaked |

## 2. Source list

All sources must be: publicly readable, permissively licensed or used with
explicit permission, and verifiable (frozen snapshot archived per corpus
version).

### Primary standards & software docs (technical ground truth)
- **BIPs**: 32, 39, 43, 44, 49, 84, 86, 174, 371, 327, 341/342 (Taproot), 380–382 (descriptors/output scripts) — github.com/bitcoin/bips
- **Bitcoin Core** docs & release notes — github.com/bitcoin/bitcoin
- **Sparrow Wallet** docs (wallet creation, multisig, verification) — sparrowwallet.com/docs
- **Bitcoin Design Guide** (CC-licensed, excellent plain-language material) — bitcoin.design/guide
- **Bitcoin Optech** topic pages (Taproot, output scripting, descriptors) — bitcoinops.org
- **Liana / wizardsardine** docs (inheritance/time-locked recovery) — liana wallet docs
- **Hardware wallet docs**: Ledger support articles, BitBox02 guides (shiftcrypto), Blockstream Jade docs, SeedSigner docs, Krux docs, Coldcard docs, Trezor knowledge base, Bitkey docs
- **Nunchuk** docs (collaborative wallets, inheritance plans)
- **BIP-329 labels / wallet-portability** writeups

### Inheritance & estate-planning material (T06, T05)
- Multisig inheritance planning guides (e.g., knowingbitcoin.com multisig inheritance guide; capbitcoin.me inheritance kit anatomy — 7-section heir kit structure)
- "What heirs actually face" catalogs: fiatisfake.org-style estate-planning failure modes ("what not to do" lists)
- Casa & Unchained public inheritance/estate content (their guides are widely shared; corpus cites them, does not endorse)
- Bitcoin-only estate attorney blog corpus (US probate + digital assets, e.g., public writings of Bitcoin estate attorneys)

### Scam-defense material (T08)
- Documented scam-pattern catalogs: fake "recovery agents", seed-phisher libraries, impersonation patterns (Bitfinexed/ScamAlert-style threads, r/Scams crypto patterns)
- Chainalysis/ELLiptic public reports on pig-butchering & recovery scams (public summaries only)
- First-party guidance: Sparrow's "verify your download", Bitcoin Core security advisories style

### Buy/sell & temporary-custody material (T09, T11)
- Exchange review data for directory ranking (see §3): bitcoin.diy reviews (River 2026 review with fee/PoR/closure-complaint detail), onramp.media comparisons, bitbo.io comparisons, Trustpilot + Reddit aggregate sentiment
- **River Learn / River blog educational library (river.com/learn + river.com blog)** — first-party Bitcoin-education articles (custody, proof of reserves, inheritance, market structure); widely regarded as among the best exchange-published educational content; high volume of plain-language material ideal for T01/T06/T09 rewrites. **License status: UNVERIFIED (2026-10-06) — River Learn carries no explicit open license.** Policy: use as *reference/ground-truth only*; every entry derived from it must be an original plain-language rewrite (in-house, MIT corpus license) with attribution and link-frozen snapshot, never a paraphrase-and-publish of their prose.
- **Onramp (onrampbitcoin.com/resources)** — first-party resource library: multi-institution custody architecture, inheritance, wealth-management-oriented explainers; complements River's library with a custody-architecture perspective
- Strike/Swan/Kraken public fee & security documentation for comparison entries

### Operator-designated trusted references (2026-10-07)
- **Mastering Bitcoin, 3rd Ed. (bitcoinbook/bitcoinbook, develop)** — chapters 04 (keys), 05 (wallets/mnemonics/backups), 06 (transactions/finality), 09 (fees), 13 (security). **License verified: CC-BY-SA 4.0 for the 3rd edition** (docs/references/bitcoinbook-license.txt). Attribution required; same-share for any verbatim reuse — our entries are original rewrites with attribution.
- **Jameson Lopp's Bitcoin security index** (lopp.net/bitcoin-information/security.html) — link registry; snapshot in docs/references/lopp-security-links.txt. Articles fetched so far: "How to Back up a Seed Phrase", plus Casa "Dos and Don'ts of Key Management" and bitcoin.org "Securing Your Wallet" (fetched as references; licensing per-site, all rewritten in-house).

### Licensing notes
- BIPs: PD/BSD-style (fine). Bitcoin Design Guide: CC-BY (fine with attribution).
- Vendor docs: quote *sparingly*, link-frozen, transformed into plain language — never wholesale copies.
- Everything rewritten in-house in plain language is original work (MIT corpus license), with source attribution per entry.

## 3. Directory policy (T11) — named, opinionated, data-backed

Per spec owner decision (2026-10-06):

1. **Bitcoin-only companies only.** No multi-coin platforms in the directory.
2. **Both categories AND named recommendations.** An heir gets: what kind of
   service solves this problem → the specific best options → the evidence.
3. **Opinionated where data supports it.** Example — the spec owner's own
   assessment, confirmed by 2026 research: **River** (US) — full-reserve
   custody (no lending), monthly cryptographic proof-of-reserves, SOC 1/2
   Type II, voluntarily published financial statements, strong human
   customer service, 0-fee recurring buys, free withdrawals; sensible as a
   *temporary custodial option while heirs figure out next steps*.
4. **Every named entry must carry:**
   - why it is listed (concrete data: audits, PoR, license count, support reputation)
   - **known criticisms & failure modes, stated honestly** (e.g., River:
     US-only 48 states, no NY/NV; documented pattern of compliance-driven
     account closures with held funds; single-institution custody — if
     something happens at River, all customer bitcoin is affected; customer
     service weak during compliance holds, per multiple 2026 user reports)
   - date ranked + data sources used
   - how the reader can re-verify (e.g., "verify River's PoR addresses on-chain")
5. **No payments, affiliates, referral codes, or links-with-trackers — ever.**
   The app is offline and neutral; the corpus text must reflect that.
6. **Refresh cadence:** directory entries re-verified every corpus release;
   stale (>6 months) entries are visibly marked stale in-app.
7. **Escalation rule:** for high-value or legally contested estates, the
   directory points to categories of professionals (Bitcoin-knowledgeable
   estate attorney, collaborative-custody providers with documented
   inheritance procedures) rather than a single "answer".

## 4. Curation pipeline (runs on Sail flex GLM-5.3)

For each source passage → batch job produces:
1. `plain.md` — plain-language rewrite (target: smart 12-year-old, no jargon without inline definition)
2. `technical.md` — the technically correct version (kept alongside)
3. `tags` — topic ID(s), level (novice/intermediate), risk class:
   `info` / `caution` / `danger`
4. `claims` — list of factual claims with source citation
5. `reviewed` — boolean; **human (spec owner) reviews before any pack ships**

**Danger-class content gets stricter rules:** any passage that could induce
data loss (resetting a device, deleting a wallet file, "just re-enter your
seed") must be tagged `danger` and phrased with an explicit warning prefix.
The deterministic guardrails (spec §8) check tags at runtime.

## 5. Corpus format & versioning

- Packaged as a single compressed bundle in the APK (≤150 MB).
- Entry = JSON: id, topic, title, plain, technical, tags, claims[], source{title,url-frozen,license,date}, corpus_version.
- Retrieval index built at build time (BM25 + small embedding model, both local).
- Every release ships: corpus version, changelog, SHA-256, signed release notes.
- Stale marking: entries carry `verified_on` dates; UI shows staleness for >6 months.

## Added 2026-10-07 (operator-designated round 2)
- **BTC Guide** (btcguide.github.io) — multisig walkthroughs: why-multisig, quorum
  choices, seeds-vs-public-keys backups, coordinating multisig, verify-receive-address,
  emergency recovery. Snapshot: docs/references/btcguide.md. License: MIT (repo
  api.spdx=MIT) — rewrites with attribution OK.
- **BTC Sessions** (btcsessions.ca/learn, Ben Perrin) — bitcoin-only educator, free
  "Zero to Hero" track (mobile wallets, hardware wallets, buying, privacy, nodes,
  multisig, inheritance). Video-based: reference-only, rewrite-from-notes with
  attribution; snapshot of curriculum outline: docs/references/btcsessions_learn.md.

## Authors & educators cited (attribution)

The corpus is built on the work of these authors and publishers. Entries in this
repository are original plain-language rewrites (in-house, MIT corpus license)
grounded in their material, credited per entry via `source_title` /
`source_snapshot`, with license status recorded below.

- **Andreas M. Antonopoulos** — *Mastering Bitcoin*, 3rd ed. (with David A. Harding),
  O'Reilly / github.com/bitcoinbook. **CC-BY-SA 4.0** (license verified:
  docs/references/bitcoinbook-license.txt). Corpus chapters grounded here: keys
  (ch04), wallets & mnemonics (ch05), transactions (ch06), fees (ch09), security (ch13).
  Snapshots: docs/references/ch04_keys.adoc etc.
- **Jameson Lopp** — lopp.net Bitcoin security resources and "How to Back up a
  Seed Phrase" (blog.lopp.net). Snapshots: docs/references/lopp-security-links.txt,
  lopp-backup-seed.txt. Licensing per-site; all reuse is original rewrite with attribution.
- **River** (river.com/learn) — first-party Bitcoin-education library: self-custody,
  seed phrases, cold storage, multisig, public/private keys. **No explicit open
  license (UNVERIFIED 2026-10-06)** — reference/ground-truth only, never
  paraphrase-and-publish. Snapshots: docs/references/river-*.txt.
- **BTC Guide** (btcguide.github.io, by @mflaxman) — multisig walkthroughs.
  **MIT** — rewrites with attribution OK. Snapshot: docs/references/btcguide.md.
- **BTC Sessions / Ben Perrin** (btcsessions.ca) — bitcoin-only video educator,
  free "Zero to Hero" track. Reference-only (video curriculum).
  Snapshot of outline: docs/references/btcsessions_learn.md.
- **Bitcoin.org** — "Securing your wallet" page. Snapshot: bitcoinorg-secure-wallet.txt.
- **BIP authors** (bitcoin/bips repo) — BIP-32/39/43/44/49/84/86/174/329/371/327/341/342/380–382.
  Snapshots of the four used directly: docs/references/ (bip-00xx.txt).
- **Hardware-wallet vendors** — Ledger support, BitBox02/Shiftcrypto, Blockstream
  Jade, SeedSigner. Snapshots: docs/references/{jade-setup-restore,seedsigner-readme,
  bitbox-restore,ledger-lost-recovery-phrase}.txt.
- **Casa** — "Dos and Don'ts of Key Management". Snapshot: casa-dos-donts.txt.
- **FTC Consumer Advice** — scam-recovery and investment-scam articles.
  Snapshots: ftc-if-scammed.txt, ftc-investment-scams.txt (US government work).

Corrections welcome: if any attribution or license note above is wrong or
missing, please open an issue.
