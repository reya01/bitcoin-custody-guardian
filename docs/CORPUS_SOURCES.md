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
- Entry = JSON: id, topic_ids, title, plain, technical, key_points, dos, donts,
user_says (retrieval-index-only, non-authoritative), claims[], source_url, corpus_version, source{title,url-frozen,license,date}, corpus_version.
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
repository are **original plain-language works** that use facts, procedures, and
warnings drawn from these references — not adaptations, not paraphrase-and-publish
of their prose — and are credited per entry via `source_title` /
`source_snapshot`, with license status recorded below. The corpus JSON is MIT
licensed; entries derived from CC-BY-SA material (Mastering Bitcoin) use only
facts and procedures, which are not copyrightable, and contain no sustained
structural or expressive reuse; if you believe any entry crosses into an
adaptation, please open an issue and it will be relicensed CC-BY-SA or rewritten.

- **Andreas M. Antonopoulos** — *Mastering Bitcoin*, 3rd ed. (with David A. Harding),
  O'Reilly / github.com/bitcoinbook. **CC-BY-SA 4.0** (license verified:
  docs/references/bitcoinbook-license.txt). Corpus chapters grounded here: keys
  (ch04), wallets & mnemonics (ch05), transactions (ch06), fees (ch09), security (ch13).
  Snapshots: docs/references/ch04_keys.adoc etc.
- **Jameson Lopp** — lopp.net Bitcoin security resources and "How to Back up a
  Seed Phrase" (blog.lopp.net). Snapshots: docs/references/lopp-security-links.txt,
  lopp-backup-seed.txt. License: **copyrighted, per-site — reference-only**, original
  rewrite with attribution.
- **River** (river.com/learn) — first-party Bitcoin-education library: self-custody,
  seed phrases, cold storage, multisig, public/private keys. **No explicit open
  license (UNVERIFIED 2026-10-06) — copyrighted, reference-only**, never
  paraphrase-and-publish. Snapshots: docs/references/river-*.txt.
- **BTC Guide** (btcguide.github.io, by @mflaxman) — multisig walkthroughs.
  **MIT** — rewrites with attribution OK. Snapshot: docs/references/btcguide.md.
- **BTC Sessions / Ben Perrin** (btcsessions.ca) — bitcoin-only video educator,
  free "Zero to Hero" track. **Copyrighted video curriculum — reference-only**
  (rewrite-from-notes with attribution).
  Snapshot of outline: docs/references/btcsessions_learn.md.
- **Bitcoin.org** — "Securing your wallet" page. **MIT** (site-wide MIT license,
  per bitcoin.org/github). Snapshot: bitcoinorg-secure-wallet.txt.
- **BIP authors** (bitcoin/bips repo) — the BIPs used directly in entries:
  **32, 39, 44, 49, 84** (the broader 43/86/174/329/371/327/341/342/380–382 set
  informed the corpus plan). BIPs carry **per-BIP license headers** (mostly BSD or
  AGPL-3.0; BIP-32 is BSD-2-Clause; BIP-39/44 per-header BSD). Snapshots: docs/references/bip-00xx.txt.
- **Hardware-wallet vendors** — Ledger support, BitBox02/Shiftcrypto, Blockstream
  Jade, SeedSigner (MIT, github.com/SeedSigner/SeedSigner). Vendor support docs:
  **copyrighted, reference-only**. Snapshots: docs/references/{jade-setup-restore,
  seedsigner-readme, bitbox-restore, ledger-lost-recovery-phrase}.txt.
- **Casa** — "Dos and Don'ts of Key Management". **Copyrighted, reference-only.**
  Snapshot: casa-dos-donts.txt.
- **FTC Consumer Advice** — scam-recovery and investment-scam articles.
  **US government work, public domain.**
  Snapshots: ftc-if-scammed.txt, ftc-investment-scams.txt.

Corrections welcome: if any attribution or license note above is wrong or
missing, please open an issue.

## Per-entry provenance

One row per corpus entry (50 total, all human-reviewed). **"Sources drawn on"
lists the reference sections whose FACTS informed the entry — the entry text is
original expression, not a reproduction of these sections** (verified by an
8-gram overlap scan; see docs/LICENSES.md). Short attributed quotes, where
present, are marked.

| Entry | Topics | Sources drawn on (facts + attribution) |
|---|---|---|
| Exchange Bankruptcy and Frozen Withdrawals | T01,T08 | Public records of exchange insolvency proceedings, 2014–2024 |
| Learning Bitcoin: a Free Path for Beginners | T01 | Learning Bitcoin: a Free Path for Beginners |
| No Hotline, No Refunds: Self-Custody Loss Is Final | T01,T09 | Bitcoin developer documentation (consensus rules) |
| How to get started with Bitcoin self-custody (River) | T01,T09 | 'Value thresholds for Bitcoin wallets' table.; 'What are seed phrases?' section and Key Takeaways.; 'What are seed phrases?' section.; Hardware wallets section.; Intro: 'By self-custody, we mean that you are the person responsible for managing the public/private key pairs' and 'whoever controls your keys, ultimately controls your bitcoin.'; Intro: 'the five steps outlined below, which should take about 30 minutes.'; Multisig section.; Opening paragraph.; Seedless custody section.; Step 2 backup guidance.; Step 2.; Step 3 note.; Step 3.; Step 4 and Key Takeaways.; Wallet selection paragraph before Step 1.; https://river.com/learn/ |
| What This App Cannot Know: Honest Limits | T02,T07 | ch13_security.adoc; https://github.com/bitcoinbook/bitcoinbook/blob/develop/ch13_security.adoc |
| BIP-39 — the standard behind your 12/24 words | T02 | Abstract and From mnemonic to seed section.; BIP header block (Title, Status, Assigned, License).; From mnemonic to seed section.; Generating the mnemonic section.; Motivation section.; Related Work section.; Shortcomings section (flagged for review — see notes).; Shortcomings section.; Table in Generating the mnemonic section.; Test vectors section.; Wordlist section, criteria a, b, and c.; Wordlists section.; see docs/references/bip-39.txt |
| BIP-44 multi-account hierarchy | T02 | Account discovery section; Account section; Address gap limit section; BIP header (Assigned, Status); BIP header (Requires: 32, 43); Abstract; Path levels section; Change section; Coin type section; Examples table; Motivation; Coin type section; Purpose, Coin type, Account, Change, and Index sections; Registered coin types section and table; https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki |
| BIP-49 P2WPKH-nested addresses | T02 | Abstract; BIP header and Copyright section; Backwards Compatibility; Considerations; Motivation; Specifications, Address derivation; Specifications, Extended Key Version; Specifications, Public key derivation; Test vectors; https://github.com/bitcoin/bips/blob/master/bip-0049.mediawiki |
| BIP-84 bech32 addresses | T02 | Abstract; BIP header metadata; Backwards Compatibility; Extended Key Version; Motivation; Specifications - Address derivation; Specifications - Public key derivation; Test vectors; https://github.com/bitcoin/bips/blob/master/bip-0084.mediawiki |
| Derivation paths and why you must not photograph your seed phrase | T02 | BIP 32 hierarchical deterministic wallet specification; BIP 44, BIP 49 and BIP 84 specifications; Mobile OS backup default-behavior documentation |
| Never Photograph or Digitize the Word List | T02,T06 | Never Photograph or Digitize the Word List |
| Recovery Phrase Safety Basics | T02 | BIP-39 and BIP-32 specifications |
| What Are Public and Private Keys? | T02 | Callout note in 'How private keys are used in a transaction'.; Opening paragraph of the article.; Section 'Cold storage vs. hot wallet'.; Section 'How do I store my bitcoin private key?'.; Section 'How is a private key generated?'.; Section 'Key Takeaways'.; Section 'Private key and public key'.; Section 'Private keys and digital signatures'.; https://river.com/learn/ |
| What is a seed phrase? | T02,T01 | 'Is a seed phrase the same as a private key?' Q&A; Hot and cold wallets paragraph; Opening definition and Importance section; Opening definition of the article; Recovery process (5 steps) section; Risks section; Security and storage section; Structure and types section; WARNING callout from River; Why seed phrases matter for self-custody section; https://river.com/learn/what-is-a-seed-phrase/ |
| Seed Backup Copies and Materials: Paper, Laminate, and Steel | T02 | in-house / walkthrough content |
| Back Up the Word List on More Than One Medium, in More Than One Place | T02 | Back Up the Word List on More Than One Medium, in More Than One Place |
| Handling a Word List: Copying, Storing, Who May Touch It | T02,T06 | ch05_wallets.adoc; https://blog.lopp.net/how-to-back-up-a-seed-phrase/; lopp-backup-seed.txt |
| BIP-32 hierarchical deterministic wallets | T02,T05 | Abstract; BIP header block; Compatibility; Motivation; Security, Implications; Specification, 'Public parent key → private child key' and CKDpub; Specification, Child key derivation (CKDpriv); Specification, Extended keys; Specification, Key identifiers; Specification, Master key generation; Specification, Serialization format; Specification, The default wallet layout; Use cases, 'Unsecure money receiver'; https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki |
| Cold Storage and Hardware Wallets | T03,T04 | Cold storage methods section and Key Takeaways; Cold wallet risk and utility analysis section; Hardware wallets section; Hardware wallets section and BIP 39 section; Hot vs. cold storage section; How is bitcoin stored? section; Introduction and Hot vs. cold storage sections; Mental wallets and BIP 39 sections; Paper wallets section; https://river.com/learn/ |
| Restoring a BitBox02 from recovery words (BitBox) | T04 | 'After restoration – no balance visible' section; 'Every position must match' note under 'Restore your wallet'; 'How word auto-completion works' section; 'Legacy address types' section; 'What can you do?' under 'Legacy address types'; 'What you need' section; 'What you need' section, passphrase note; 'Why this happens' note under 'After restoration – no balance visible'; FAQ: 'Can I restore on another hardware wallet?'; FAQ: 'Can I use the microSD backup instead of the words?'; FAQ: 'Can a wrong input still open a valid wallet?'; FAQ: 'Does the 12/24-word backup contain my passphrase?'; FAQ: 'How can I read my recovery words from the microSD backup?'; FAQ: 'How long does synchronization take?'; Intro paragraph of the article; Security warning block and FAQ: 'Can I enter my recovery words into the BitBoxApp?'; Security warning block at the top of the article; https://shiftcrypto.ch/ |
| Setting up and restoring a Blockstream Jade | T04 | Restore recovery phrase to Jade, intro and step 2; Restore recovery phrase to Jade, step 3; Set up Jade, Tip; Set up Jade, opening paragraph; Set up Jade, step 2; Set up Jade, step 3 note; Set up Jade, step 4; Set up Jade, step 5; https://support.blockstream.com/hc/en-us/articles/10762988581145-Restore-recovery-phrase-to-Jade |
| I lost my Secret Recovery Phrase (Ledger) | T04 | Link embedded in the same section; Same section; Section 'I lost my Secret Recovery Phrase but I no longer have access to my PIN or my Ledger device has reset'; Section 'Lost your Secret Recovery Phrase?', intro paragraph; Section 'Not sure that your Secret Recovery Phrase is correctly backed up?'; Section 'What is a 24-word Secret Recovery Phrase and why is it so important?'; Step 1 parenthetical note; Step 2; Step 3; Step 3 note; Step 3, 'Reset from device settings' and 'Reset from PIN' subsections; Step 4; Step 5; Step 6; https://support.ledger.com/article/lost-recovery-phrase-or-backup |
| SeedSigner: air-gapped stateless signing device | T04,T05 | Considerations; Downloading the Software; Feature Highlights; Project Summary; Shopping List; Software Installation - A Special Note On Minimizing Trust; Verifying the Software; Writing the software onto your MicroSD card; https://github.com/SeedSigner/SeedSigner |
| Multisig Backups: Seeds Are Not Enough | T05 | Backup Wallet; Emergency Recovery |
| Receiving and Verifying on a Multisig Wallet | T05 | Emergency Recovery; Verify Receive Address |
| What is multisig? (River) | T05 | River multisig glossary, Related Terms: 'Pay-to-Script-Hash (P2SH)' and 'Redeem script'; River multisig glossary, Related Terms: 'Private key'; River multisig glossary, example paragraph; River multisig glossary, first paragraph; River multisig glossary, opening paragraph; River multisig glossary, second paragraph; River multisig glossary, third paragraph; https://river.com/learn/ |
| Why Multisig for an Inheritance | T05 | Backup Wallet; Emergency Recovery; Send Bitcoin; Setup Hardware Wallet Overview |
| Day One: Confirm the Balance, Then Stop | T06 | Day One: Confirm the Balance, Then Stop |
| Day 1: Securing inherited bitcoin devices, word lists, and computers | T06,T07,T02 | in-house / walkthrough content |
| First Hours: Securing Inherited Bitcoin Safely | T06,T01 | bitcoinorg-secure-wallet.txt; casa-dos-donts.txt; ch13_security.adoc; https://bitcoin.org/en/secure-your-wallet |
| Decoding Inherited Bitcoin Jargon: Seed Phrase, xpub, Watch-Only, Passphrase, Self-Custody | T06,T02,T07 | in-house / walkthrough content |
| Exchange accounts in an inheritance: what to do first, avoiding recovery scams, and why the seed phrase doesn't apply | T06,T07,T02 | in-house / walkthrough content |
| Found Paper with 24 Handwritten Words: Seed Phrase Handling | T06,T07 | in-house / walkthrough content |
| Inheritance Scam Alerts: Remote-Control Requests, Pay-First 'Unlocks', and Telling Real from Fake | T06 | in-house / walkthrough content |
| Inheritance Scams: Fake Support Calls, 'Recovery Agents,' and Deadline Emails | T06,T07 | in-house / walkthrough content |
| Inherited Hardware Wallet: Unknown PIN, Reset Advice, and a Dead Device | T06,T07 | in-house / walkthrough content |
| Inherited hardware wallets: PIN retry limits, day-one power-on, and offers of help | T06,T07 | in-house / walkthrough content |
| Inherited Multisig Wallets: '2 of 3' Setups and the Descriptor File | T06,T07,T02 | in-house / walkthrough content |
| Inventory and privacy: listing what you found without exposing it | T06,T07 | in-house / walkthrough content |
| Making a Private Inventory of What Was Inherited | T06,T09 | ch05_wallets.adoc; ch13_security.adoc; https://github.com/bitcoinbook/bitcoinbook |
| Inventory Privacy: What to Tell Family and What Never to Write Down | T06 | in-house / walkthrough content |
| Verification Day: Checksum, Seed Entry, Balance, Then Stop | T06 | in-house / walkthrough content |
| Checksum First: Verify the Software Before the Words | T06 | Checksum First: Verify the Software Before the Words |
| What This App Cannot Tell You: Prices, Selling, and Picks | T06,T07,T02 | in-house / walkthrough content |
| Word list safety: no email or cloud copies; checking the list without typing it | T06,T07 | in-house / walkthrough content |
| What to do if you were scammed (FTC) | T08 | 'How To Get Your Money Back: Bank Transfers'; 'How To Get Your Money Back: Credit Card Payments'; 'How To Get Your Money Back: Cryptocurrency'; 'How To Get Your Money Back: Cryptocurrency', opening line; 'How To Get Your Money Back: Cryptocurrency', steps 1-3; 'How To Get Your Money Back: Debit Card Payments'; 'How To Get Your Money Back: Gift Cards'; 'How To Get Your Money Back: Payment Apps'; 'How To Get Your Money Back: Wire Transfers'; 'How To Report a Scammer to the FTC'; 'What To Do if You Gave a Scammer Access to Your Computer'; 'What To Do if You Gave a Scammer Your Personal Information', intro; 'What to do if you gave a scammer your Social Security number'; 'What to do if you gave a scammer your account information'; End of 'How To Get Your Money Back: Cryptocurrency' section; Intro to 'How To Get Your Money Back if You Paid a Scammer'; https://consumer.ftc.gov/ |
| Spotting investment scams (FTC) | T08 | Closing line of the alert.; First bullet under 'To avoid an investment scam'.; Opening paragraph, FTC loss statistics.; Second bullet under 'To avoid an investment scam'.; Second paragraph, description of how scammers make contact.; Second paragraph.; Third bullet under 'To avoid an investment scam'.; https://consumer.ftc.gov/ |
| Recognizing Bitcoin Scams: Phishing Texts to Fake Recovery | T07,T08 | in-house / walkthrough content |
| Sending Bitcoin Safely: Units, Fees, Confirmations, and Refusing Scam Scripts | T10,T01 | in-house / walkthrough content |
| Social-Engineering Scams: Romance, Fake Jobs, and Urgency Scripts | T07,T08 | in-house / walkthrough content |
