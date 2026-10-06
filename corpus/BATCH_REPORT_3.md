# BATCH_REPORT_3.md — Corpus Batch 3 (2026-10-06)

Source files in /opt/data/cache/corpus-src/batch3/. All entries curated by corpus_curate.py
(Sail flex GLM-5.3) and set reviewed=false pending human sign-off.

## Batch summary

| entry | title | topics | level | risk_class | claims | warnings |
|---|---|---|---|---|---|---|
| T04-ledger-lost-recovery-phrase.json | I lost my Secret Recovery Phrase (Ledger) | T04 | novice | danger | 16 | 4 |
| T04-bitbox-restore.json | Restoring a BitBox02 from recovery words (BitBox) | T04 | novice | danger | 18 | 5 |
| T04-jade-setup-restore.json | Setting up and restoring a Blockstream Jade | T04 | novice | danger | 11 | 5 |
| T04T05-seedsigner.json | SeedSigner: air-gapped stateless signing device | T04,T05 | intermediate | danger | 19 | 5 |
| T08-ftc-investment-scams.json | Spotting investment scams (FTC) | T08 | novice | caution | 10 | 3 |
| T08-ftc-if-scammed.json | What to do if you were scammed (FTC) | T08 | novice | danger | 17 | 3 |

Fetched sources: 6 usable of 8 attempted. FAILED/SKIPPED:
- Krux (krux.link docs / GitHub wiki): the wiki Home page is a 147-byte stub and
  krux.link returned "Blocked: private/internal address" from the fetch service;
  no substantive recovery content could be extracted — skipped.
- Ledger factory-reset article (support.ledger.com/article/360017582434-zd): JS-walled
  to direct fetch; its content was largely covered by the successfully fetched
  "I lost my 24-word Secret Recovery Phrase" article, so not pursued separately.
- SeedSigner's seedsigner.github.io/seedsigner-docs/ returns 404; used the official
  SeedSigner GitHub README instead (repo is the project's canonical documentation
  entry point per seedsigner.com).

All six sources were verified as real content (no 404s, no bot walls, no empty shells)
before curation. FTC sources dated April 2026 and earlier; vendor articles show
"updated 2026" dates — staleness flags recorded per entry.

## Reviewer concerns (notes_for_reviewer, per entry)

### T04-ledger-lost-recovery-phrase.json — I lost my Secret Recovery Phrase (Ledger)
Source: /opt/data/cache/corpus-src/batch3/ledger-lost-recovery-phrase.txt
Warnings: ["Never reset your Ledger device — including by entering the wrong PIN three times — before moving every coin to a safe temporary account. Resetting erases the keys, and without your Secret Recovery Phrase the coins are lost forever.", "Never share your Secret Recovery Phrase. Ledger will never ask for it; anyone who does is a scammer.", "If both the Secret Recovery Phrase and the PIN are lost, no one can recover the funds — not even Ledger.", "If you have a second Ledger device, do not wipe the first one until you are certain every asset was moved off the lost phrase."]
Notes:
Staleness: cached article is dated 05/13/2026 — verify the live page. Reset-via-settings steps are given only for Nano X / Nano S / Nano S Plus; newer Ledger models (e.g., Stax, Flex) are not covered — confirm whether to extend. Naming: source uses 'Ledger Wallet (formerly Ledger Live)' — confirm current branding at review time. The step-1 parenthetical about outdated Ledger OS preventing sends is terse; I interpreted it as 'move funds before updating the OS' — please verify. Source's 'compromised accounts' in step 2 means accounts tied to the lost phrase; I paraphrased to avoid confusion. Content is Ledger-specific and covers crypto assets generally, not Bitcoin only. Step 1 suggests exchanges as temporary custody, which sits oddly with our self-custody teaching; consider app-side guidance to keep that window short or prefer a second hardware wallet.

### T04-bitbox-restore.json — Restoring a BitBox02 from recovery words (BitBox)
Source: /opt/data/cache/corpus-src/batch3/bitbox-restore.txt
Warnings: ["Enter recovery words only on a trusted hardware wallet device — never in the BitBoxApp, on websites, in a browser, in password managers, in cloud storage, or in photos or notes.", "Never share your recovery words; anyone with access to them can steal your funds, and Shift Crypto will never ask for them.", "If any app asks for your recovery words, it is almost certainly a fake app or phishing — stop immediately and contact support.", "If a passphrase was used, you must enter the same passphrase after restoring, or you will open a different, empty wallet.", "No balance after restoring usually does not mean coins are lost — check word order, passphrase, added accounts, and synchronization before taking any further action."]
Notes:
Source page states it was updated February 13th, 2026 — re-verify against the live page before release, since BitBoxApp setup screens and menu labels can change between versions (the step sequence and 'Restore from recovery words' label may drift). Two linked companion articles (the microSD restore guide and the emergency extraction of recovery words from a microSD backup) are NOT included in this source; this entry only asserts they exist — consider ingesting them as separate entries and cross-linking. The legacy-address remedy says the wallet 'may need to be restored in compatible software and then transferred' without naming the software; I deliberately did not add examples. The source never states the BIP-39 word list size (2,048 words); I left that well-known fact out because it is not in the source. Synchronization time ('a few seconds to several minutes') is typical, not guaranteed.

### T04-jade-setup-restore.json — Setting up and restoring a Blockstream Jade
Source: /opt/data/cache/corpus-src/batch3/jade-setup-restore.txt
Warnings: ["Never share your recovery phrase — it grants access to your wallet.", "Never enter your recovery phrase on another device except in a genuine emergency.", "Do not photograph your recovery phrase or store it online — write it on paper and keep it offline.", "Restoring a phrase that was created on a phone or computer app does not make it cold storage; for full cold storage, create a new wallet on Jade and send funds to it.", "The recovery phrase is not saved to Jade until PIN setup is complete — finish all setup steps before relying on the device."]
Notes:
Combined from two Blockstream help pages — re-verify both URLs before publishing, as Blockstream reorganizes its help center. Points to check: (1) The plain-text gloss of the blind oracle ('an outside service that helps unlock the device but cannot read your secrets') is inferred from the term 'blind' and Jade's documented design; the source only says the phrase is encrypted 'in cooperation with a blind oracle' and that decryption requires a companion device — confirm the gloss against Blockstream's security documentation. (2) The source never labels the recovery phrase itself as BIP39 (only the optional passphrase is called a 'BIP39 passphrase'), so neither version asserts that. (3) The Set up page does not enumerate which phrase lengths Advanced Setup offers; the 12/24-word options come from the restore page. (4) The anti-extraction property applies only to a locked Jade — an unlocked, PIN-entered Jade exposes its secrets by design. (5) source_url points to the combined cache file; replace with the two canonical help URLs after verification.

### T04T05-seedsigner.json — SeedSigner: air-gapped stateless signing device
Source: /opt/data/cache/corpus-src/batch3/seedsigner-readme.txt
Warnings: ["Flashing the software onto a MicroSD card erases everything on that card — use a blank or spare card only.", "The DD command can permanently erase the wrong disk — always confirm the target disk before running it.", "If download verification shows 'Bad signature' or 'FAILED', stop immediately and do not use the files.", "Never move large amounts without testing the setup first — practice on Testnet or with small amounts.", "Never share your seed words, passphrase, or any QR code made from them with anyone."]
Notes:
Source is a local snapshot (batch3/seedsigner-readme.txt) of the SeedSigner README; source_url is inferred as the GitHub repo — confirm it matches the file's origin. Highly version-sensitive: 'current' is v0.7.0 in the source; download links, image sizes, Taproot status ('not quite yet supported'), boot times, and verification steps may change with releases — recheck against the latest release before publishing. Internal inconsistencies in the source: (1) 'Considerations' says boot takes up to 60 seconds while the install section says allow about 45 seconds for the logo to appear; I used the conservative 60. (2) 'Planned Upcoming Improvements' still lists 'Reproducible builds' as future work, but the Minimizing Trust section states reproducibility shipped with v0.7.0; the latter appears authoritative. The '<$50' figure is the project's own 'usually' estimate and excludes tools and optional accessories. Risk_class set to danger because the build/install procedure embeds destructive steps (SD-card flashing, DD) even though seed handling itself is stateless.

### T08-ftc-investment-scams.json — Spotting investment scams (FTC)
Source: /opt/data/cache/corpus-src/batch3/ftc-investment-scams.txt
Warnings: ["If anyone promises big, fast, or 'risk-free' returns, stop — that is a scam signal, not an opportunity. Do not send money.", "Do not trust 'proof' of gains shown by someone coaching you — scammers fabricate screenshots and account balances.", "Never share seed words (recovery phrase), wallet passwords, or device access with anyone offering crypto 'coaching' or investment help — no legitimate professional will ever ask for them."]
Notes:
Figures ($7.9B total losses; >$10,000 median individual loss; calendar year 2025) come from FTC complaint data and will go stale — re-verify against the FTC's latest release before publication; reported losses likely understate actual losses. The seed-word warning in warnings[] is an app-added guardrail bridging FTC guidance to Bitcoin self-custody; the source never mentions seed words — confirm this addition is intended policy. The plain text glosses 'cryptocurrency' as 'digital money like Bitcoin'; the source does not name Bitcoin specifically. The source gives only domain-level references (Investor.gov search tool, CFTC database, ReportFraud.ftc.gov) with no deep links — verify exact URLs before in-app deep-linking. 'You end up losing all your money' is the FTC's characterization of this scam pattern and is retained as a safety message. Alert is dated April 2026; check for superseding alerts.

### T08-ftc-if-scammed.json — What to do if you were scammed (FTC)
Source: /opt/data/cache/corpus-src/batch3/ftc-if-scammed.txt
Warnings: ["Never share your seed words (recovery phrase) with anyone — including anyone who promises to recover money you lost to a scam.", "Delete only the files your security scan itself flags as a problem — never manually delete wallet files, backups, or written seed-word records.", "Do not type your seed words into a computer a scammer had access to until it has been scanned and cleaned."]
Notes:
Risk class set to danger solely because the computer-access procedure includes a deletion step ('delete anything it identifies as a problem'); our plain/technical versions add 'only' to scope that step safely — confirm this rendering is acceptable, and whether 'caution' would suffice instead. The three seed-word warnings are app-side safety additions, not statements from the FTC source — verify wording against app policy. Key ambiguity: the FTC's crypto advice ('ask them to reverse the transaction') is aimed at exchanges and ATM operators; it does not address Bitcoin sent from self-custody wallets, where on-chain transactions are effectively irreversible — ensure the entry does not create false hope for self-custody losses and consider cross-linking to wallet-specific recovery content. The state-law claim is vague in the source ('might help you') and state legislation changes frequently — kept hedged.

## Fetch log (attempted → outcome)

- Ledger "I lost my 24-word Secret Recovery Phrase" → OK (web_extract; full text)
- Ledger "reset to factory settings" article → FAILED via direct fetch (JS-walled);
  not separately curated (covered by the lost-phrase article)
- BitBox "How to restore your wallet using recovery words" → OK (direct fetch)
- Blockstream Jade "Set up Jade" + "Restore recovery phrase to Jade" → OK (web_extract; combined)
- SeedSigner docs site → 404; SeedSigner GitHub README → OK (raw fetch)
- Krux docs → FAILED (wiki stub / fetch blocked) — skipped
- FTC "With people losing big to investment scams..." → OK (web_extract)
- FTC "What To Do if You Were Scammed" → OK (web_extract; crypto-relevant excerpts)
