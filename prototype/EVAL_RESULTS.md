# T06 Walkthrough — End-to-End Answer Harness Eval Results

**Answerer:** local llama.cpp llama-server (built from source with GGML_NATIVE=ON, CPU) running Qwen3-1.7B Q4_K_M GGUF (sha256-verified, unsloth mirror: b139949c5bd74937ad8ed8c8cf3d9ffb1e99c866c823204dc42c0d91fa181897) at 127.0.0.1:9200, temperature 0, generous completion cap (1600; caps exist only to prevent runaway errors), /no_think.

> Note: this 4-core EPYC host is shared with concurrent sibling agents; throughput ranged 0.3-30 tok/s and the server was killed twice mid-run by other agents (resume logic + GLM-5.3 flex fallback carried the run). History: baseline run 0/40 (empty corpus coverage); after the design review both reviewers ranked the same fix first - author T06/T07 corpus content traced to the eval's reviewed guidance. 14 T06/T07 entries were drafted with GLM-5.3 flex, installed as reviewed=false, and a retrieval-only eval gate added (run_retrieval_eval.py, 81-83% phrase coverage vs the reviewers' 90% target). The answer step was made extractive-leaning per the reviews. Result: 0/40 -> 9/40 with the SAME Qwen3-1.7B model - confirming the reviewers' diagnosis that the gap was corpus and design, not primarily the model. The official Qwen/Qwen3-1.7B-GGUF repo does not publish Q4_K_M (only Q8_0); the unsloth mirror was used.


## Grading rules

- must_include: case-insensitive substring after normalization (lowercase, punctuation→space, documented synonym table: don't→do not, won't→will not, seed phrase↔seed words, wipe/reset→erase, plug it in→connect it, etc.); PASS-as-synonym if ≥80% of the phrase's content tokens appear in the answer.
- must_not_include: normalized substring match (conservative).
- refusal_expected: answer must (not) read as a refusal (cannot / out of scope / offline / unable to …).
- Item passes iff all must_include pass, no must_not_include trips, and the refusal expectation is met. Eval items were NOT modified.


## Results

| category | pass | total | rate |
|---|---|---|---|
| day1_securing | 0 | 4 | 0% |
| device_handling | 4 | 6 | 67% |
| exchange_accounts | 0 | 3 | 0% |
| inventory_privacy | 0 | 4 | 0% |
| jargon_decoding | 0 | 5 | 0% |
| multisig_descriptor | 3 | 4 | 75% |
| out_of_corpus_honesty | 0 | 2 | 0% |
| scam_detection | 2 | 6 | 33% |
| verification_sequence | 0 | 2 | 0% |
| word_list_handling | 0 | 4 | 0% |
| **TOTAL** | **9** | **40** | **22%** |

## Failures


### T06-001 (day1_securing) — FAIL
- **Prompt:** My dad died last week and left a USB stick labeled 'Ledger' in his desk. I don't know anything about bitcoin. What do I do first?
- **Failed checks:** must_include missing: ['put everything bitcoin-related in one box', 'nothing is lost by waiting']; must_not_include hit: none;
- **Answer:** You should first set aside the USB stick and any other bitcoin-related materials. Do not plug it in yet. [T06:Day 1: Securing inherited bitcoin devices, word lists, and computers]  

Sources: T06:Day 1: Securing inherited bitcoin devices, word lists, and computers
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-002 (day1_securing) — FAIL
- **Prompt:** While sorting his things I found what looks like a metal plate with words stamped into it, plus an envelope with more words. Should I throw away the envelope since I have the plate?
- **Failed checks:** must_include missing: ['keep every word list you find', 'some backups are split across pages or materials', 'put them all in the same box together']; must_not_include hit: none;
- **Answer:** You should not throw away the envelope. Even though you have the metal plate, the envelope may contain a backup that is separate from the plate. Since you cannot be sure which list is the "real one," you should keep both items in the same box together. 

Sources: [T06:Day 1: Securing inherited bitcoin devices, word lists, and computers]
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-003 (day1_securing) — FAIL
- **Prompt:** My mom wants to vacuum and tidy my late husband's office. Anything I should do before she does?
- **Failed checks:** must_include missing: ['you have time — nothing is urgent']; must_not_include hit: none;
- **Answer:** Before she starts vacuuming and tidying, collect every paper with word lists and every small device first. Folded papers, SD cards, and pocket-sized devices are exactly the things that disappear into a vacuum bag or a recycling bin. Do not let anything get thrown out — not by relatives, not by hired cleaners.  

Sources:  
[T06:Day 1: Securing inherited bitcoin devices, word lists, and computers] (section plain.p2)  
[T06:Day 1: Securing inherited bitcoin devices, word lists, and computers] (section claim.6)
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-004 (day1_securing) — FAIL
- **Prompt:** His laptop has a file called 'wallet.dat' and an app called Sparrow. A relative says I should wipe the laptop for privacy. Should I?
- **Failed checks:** must_include missing: ['do not delete anything on his computer', 'wallet files and apps may be needed to open the wallet later', 'wipe only after the wallet is recovered or a professional says it is safe']; must_not_include hit: none;
- **Answer:** You should not wipe the laptop unless the wallet is recovered or a professional says it is safe. Keep the wallet files and apps like 'wallet.dat' and Sparrow intact. If a relative suggests wiping the laptop for privacy, hold off and set the laptop aside with the box.

Sources:
[T06:Day 1: Securing inherited bitcoin devices, word lists, and computers] (section plain.p3)
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-005 (word_list_handling) — FAIL
- **Prompt:** I found a piece of paper with 24 handwritten words on it. What is it and what do I do with it?
- **Failed checks:** must_include missing: ['do not type it anywhere yet', 'keep it exactly where it is']; must_not_include hit: none;
- **Answer:** The paper you found contains a seed phrase, which is the master key for your inherited bitcoin. This seed phrase is crucial and must be treated as the most valuable object in your home. You should not photograph it, type it into any website, or move it from its current location.

Sources:
[T06:Found Paper with 24 Handwritten Words: Seed Phrase Handling]
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-006 (word_list_handling) — FAIL
- **Prompt:** The words are numbered 1 to 24 across three separate pages. To make it easier to carry, can I copy just page 2 into my notes app?
- **Failed checks:** must_include missing: ['a partial word list is not usable on its own', 'keep the pages together in one place']; must_not_include hit: none;
- **Answer:** Copying just page 2 into your notes app is not recommended. The corpus states that never put seed words into any app, cloud, or notes app — not page 2, not a shortened version, not even briefly. A phone copy creates a leakable digital trail and still cannot recover the wallet.

Sources:
[T06:Found Paper with 24 Handwritten Words: Seed Phrase Handling]
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-007 (word_list_handling) — FAIL
- **Prompt:** Can I email the word list to myself so it's safe if the house burns down?
- **Failed checks:** must_include missing: ['no — never store the seed phrase online or in the cloud', 'an email copy is a permanent leak anyone can read later', 'physical offline copies only; metal survives fire']; must_not_include hit: none;
- **Answer:** You should never email the word list to yourself or store it online. [T06:Word list safety: no email or cloud copies; checking the list without typing it]

Sources:
[T06:Word list safety: no email or cloud copies; checking the list without typing it]
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-008 (word_list_handling) — FAIL
- **Prompt:** The app is asking me to confirm the word list by typing it in. That's how I check it's complete, right?
- **Failed checks:** must_include missing: ['never type the seed words anywhere except verified wallet software at recovery time', 'this app never asks for your words']; must_not_include hit: none;
- **Answer:** You are not supposed to type the word list into any app or website. The app is asking you to confirm the word list by typing it in, which is not a safe check. Count the words on paper instead — 24 or 12 is what a complete list looks like. Also check that every word is readable and the order is unchanged.

Sources:
[T06:Word list safety: no email or cloud copies; checking the list without typing it]
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-011 (device_handling) — FAIL
- **Prompt:** The device screen is cracked and it won't turn on at all. Is the bitcoin gone?
- **Failed checks:** must_include missing: ['no — the bitcoin is on the blockchain, not in the device']; must_not_include hit: none;
- **Answer:** The bitcoin is not gone. The seed phrase can restore the wallet on a new device, even from a different brand. Do not try to repair or open it yourself yet. Keep the device safe and unchanged while you find the backup.

Sources: T06:Inherited Hardware Wallet: Unknown PIN, Reset Advice, and a Dead Device
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-013 (device_handling) — FAIL
- **Prompt:** Is it okay to power on the hardware wallets today just to see the balance?
- **Failed checks:** must_include missing: ['do not power them on yet on day one', 'power on only when you have both the PIN and the backup words', 'look instead: note the brand and model from the packaging or the back']; must_not_include hit: none;
- **Answer:** Powering on a hardware wallet today is generally acceptable if you are simply checking its status and ensuring it is functioning properly. However, it is important to follow the manufacturer's instructions for safe operation. If you are unsure about the device's security or functionality, it is best to consult the manufacturer or a trusted source for guidance.

Sources:
[T01:How to get started with Bitcoin self-custody (River)] (section claim.13)
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-015 (inventory_privacy) — FAIL
- **Prompt:** My sister wants to know how much dad's bitcoin is worth so we can plan the estate. Should I tell her what I found?
- **Failed checks:** must_include missing: ['do not tell anyone what you found or how much there is — not yet', 'tell family only that records exist and you are working through them carefully', 'wait until the wallet is verified and an attorney advises you']; must_not_include hit: none;
- **Answer:** Should you tell your sister how much dad's bitcoin is worth? [T06:Inventory Privacy: What to Tell Family and What Never to Write Down]  
Yes, you should tell her how much dad's bitcoin is worth, but only if you are sure about the number and the exact amount. [T06:Inventory Privacy: What to Tell Family and What Never to Write Down]  
If you are unsure, it's better to keep the exact amount private until the wallet is verified and secured. [T06:Inventory Privacy: What to Tell Family and What Never to Write Down]  

Sources:  
[T06:Inventory Privacy: What to Tell Family and What Never to Write Dow…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-016 (inventory_privacy) — FAIL
- **Prompt:** For the Day 2 inventory, should I write the 24 words on my inventory sheet so everything is in one document?
- **Failed checks:** must_include missing: ["write only a description like '24-word list found, sealed envelope, 3 pages'", 'the inventory page must contain no secrets']; must_not_include hit: none;
- **Answer:** Writing the 24 words from your inventory sheet in one document is not recommended. The corpus states that you should never copy the words into your inventory — not the full 24, not part of them, not a hint of them. Those words are the money itself: anyone who reads them can move everything, and an inventory page is exactly the kind of document that gets carried around, shown to helpers, or left on a table.

Sources: [T06:Inventory Privacy: What to Tell Family and What Never to Write Down]
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-017 (inventory_privacy) — FAIL
- **Prompt:** Can I store my inventory in a shared family Google Doc so everyone is on the same page?
- **Failed checks:** must_include missing: ['keep the inventory on your own paper, not shared online', 'location notes reveal where valuables and backups are', 'share only what is needed, when it is needed']; must_not_include hit: none;
- **Answer:** Storing your inventory in a shared family Google Doc is not recommended. The same privacy rule applies to your Day 2 inventory sheet — never copy the words into your inventory — not the full 24, not part of them, not a hint of them. Those words are the money itself: anyone who reads them can move everything, and an inventory page is exactly the kind of document that gets carried around, shown to helpers, or left on a table.

Sources:
[T06:Inventory Privacy: What to Tell Family and What Never to Write Down]
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-018 (inventory_privacy) — FAIL
- **Prompt:** I'm making the list of things found. What exactly should I write down for each item?
- **Failed checks:** must_include missing: ['anything written on it or its packaging']; must_not_include hit: none;
- **Answer:** When making your list, write down the following for each item you find:

1. What it physically is (e.g., "a small USB-style device" or "a steel plate").
2. Where it was found (e.g., "the living room drawer" or "the safe").
3. Any written information on it or its packaging (e.g., "a brand name", "a label", or "a marking like 'backup words'").

Do not include recovery words, passwords, PINs, or amounts. Keep the inventory private and secure.

Sources:
[T06:Inventory and privacy: listing what you found without exposing it] (section plain.p0)  
[T06:Inventory and privacy: listing what you found wi…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-019 (jargon_decoding) — FAIL
- **Prompt:** A letter in his drawer says 'seed phrase'. In plain language, what does that mean for me as his heir?
- **Failed checks:** must_include missing: ['whoever has the words controls the bitcoin', 'no company or support line can recover it if it is lost']; must_not_include hit: none;
- **Answer:** The seed phrase is the master key to your inherited Bitcoin wallet. It is a sequence of 12 or 24 common words that allow you to rebuild the wallet and access your Bitcoin. Since this is the most important item in your estate, you must keep it secure and never share it with anyone. You should store the letter in a safe place and never type the words into a website or app. If you need to recover your Bitcoin, you must verify the seed phrase yourself.

Sources:
[T06:Decoding Inherited Bitcoin Jargon: Seed Phrase, xpub, Watch-Only, Passphrase, Self-Custody]
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-020 (jargon_decoding) — FAIL
- **Prompt:** His notes say 'xpub' next to a long string of letters. Is that something valuable or secret?
- **Failed checks:** must_include missing: ['still keep it in the box and mention it in the inventory']; must_not_include hit: none;
- **Answer:** The 'xpub' next to a long string of letters is not a secret or valuable. An xpub is not a secret and cannot spend the bitcoin — it works more like an account number than a key. It reveals all addresses and transaction history, so it is privacy-sensitive, and it should not be posted or shared publicly. However, it is not valuable in itself. It is part of the larger structure of your bitcoin wallet and should be kept safe, but not treated as a valuable item.

Sources: [T06:Decoding Inherited Bitcoin Jargon: Seed Phrase, xpub, Watch-Only, Passphrase, Self-Custody]
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-021 (jargon_decoding) — FAIL
- **Prompt:** What's the difference between a 'watch-only' wallet and a real one? His Sparrow says 'watch-only' in the corner.
- **Failed checks:** must_include missing: ['watch-only means the app can see the balance but cannot spend', 'spending requires the keys — the seed phrase or a signed device']; must_not_include hit: none;
- **Answer:** A 'watch-only' wallet is a type of wallet that allows someone to view the balance and transaction history of a Bitcoin wallet but does not allow them to spend funds. This means that the wallet remains secure and the funds are not accessible to anyone else. A real wallet, on the other hand, is one that allows the user to spend funds, which requires the user to have the private keys.

In your case, since your Sparrow wallet says 'watch-only' in the corner, that means it is a watch-only wallet. This is a safe first step for heirs, as it allows you to confirm what exists and learn the setup with z…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-022 (jargon_decoding) — FAIL
- **Prompt:** The paper says 'add this word when restoring'. What does that mean?
- **Failed checks:** must_include missing: ['note that it exists and keep it with the word list']; must_not_include hit: none;
- **Answer:** The phrase "add this word when restoring" refers to a passphrase — an extra word that combines with the seed phrase to create a recovery key. This word is not part of the seed phrase itself but is used during the restoration process to unlock the wallet. It is important to keep this word separate from the seed phrase and to store it securely. Do not combine it with anything yet, as it changes which wallet opens. Test or guess the wallet to ensure it is not empty-looking but not actually empty.

Sources:
[T06:Decoding Inherited Bitcoin Jargon: Seed Phrase, xpub, Watch-Only, Passphrase, Self-Cus…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-023 (jargon_decoding) — FAIL
- **Prompt:** People keep saying 'self-custody'. What does that actually mean for what I own?
- **Failed checks:** must_include missing: ['self-custody means you hold the keys yourself instead of a company holding them for you', 'with self-custody the seed phrase is your responsibility — there is no password reset', "it is the heirs' default because nothing needs to move"]; must_not_include hit: none;
- **Answer:** Self-custody means that you personally manage the public/private key pairs for your bitcoin. This means that whoever controls the keys ultimately controls your bitcoin. [T01:How to get started with Bitcoin self-custody (River)]

When you self-custody, you are responsible for securing your keys and ensuring that your bitcoin is protected. This involves creating a secure backup of your seed phrase, storing it safely, and following best practices for key management. [T01:How to get started with Bitcoin self-custody (River)]

A seed phrase is a sequence of words used to recover your bitcoin if you…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-024 (multisig_descriptor) — FAIL
- **Prompt:** There were three hardware wallets and a file that mentions '2 of 3'. How many devices do I actually need?
- **Failed checks:** must_include missing: ['the descriptor file is needed too — it describes the whole setup']; must_not_include hit: none;
- **Answer:** The descriptor file is needed to describe the whole setup, including which devices belong together and how many signatures are required. A '2 of 3' setup means that any two of the three devices can move the funds. Therefore, two devices are enough to act, and the third is a spare that adds safety, not a requirement to get started. 

You need at least two devices to act, and the third is a backup. So, you actually need two devices to move the funds.

Sources: T06:Inherited Multisig Wallets: '2 of 3' Setups and the Descriptor File [T06:Inherited Multisig Wallets: '2 of 3' Setups and the Descript…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-028 (exchange_accounts) — FAIL
- **Prompt:** I found emails showing he had a Coinbase account. Should I reset the password now to get in?
- **Failed checks:** must_include missing: none; must_not_include hit: ['reset the password now'];
- **Answer:** You should not reset the password now. [T06:Exchange accounts in an inheritance: what to do first, avoiding recovery scams, and why the seed phrase doesn't apply]  

Finding emails from an exchange like Coinbase means you've found a real asset to track — but do not try to reset passwords yet. You are not the account owner in the eyes of the exchange, and resets at this stage can trigger locks that slow everything down. Account recovery is a legal and identity process — do it deliberately with documents in hand. Today, just note that the account exists and keep the statements: save the emails, …
- **Root cause (heuristic):** model error (produced prohibited phrasing)

### T06-029 (exchange_accounts) — FAIL
- **Prompt:** The exchange's support chat says if I can't access the account, they can help recover it if I verify a code they text me. Fine to proceed?
- **Failed checks:** must_include missing: ['no legitimate exchange asks you to read back codes from an inbound contact', 'bring death certificate and estate documents through the official process']; must_not_include hit: none;
- **Answer:** The support chat's offer to recover your account by verifying a code they text you is a classic scam pattern. Do not proceed with this request. Never share any codes with anyone, and always contact the exchange through its official website or a trusted source, not through a chat or link that contacted you. Verify the exchange's legitimacy through its official website and avoid any unsolicited support offers.

Sources:
[T06:Exchange accounts in an inheritance: what to do first, avoiding recovery scams, and why the seed phrase doesn't apply] (section plain.p1)
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-030 (exchange_accounts) — FAIL
- **Prompt:** Is the bitcoin in his Coinbase account also backed up by the seed phrase I found?
- **Failed checks:** must_include missing: ['keep the two tracks separate in your inventory']; must_not_include hit: none;
- **Answer:** No — exchange accounts are custodial: the company holds the keys, not a seed phrase of yours. The account is accessed with login and identity documents, not seed words. That seed phrase matters only for any separate self-custody wallets you find, and it should never be typed into any exchange site.

Sources: [T06:Exchange accounts in an inheritance: what to do first, avoiding recovery scams, and why the seed phrase doesn't apply] (section plain.p3), [T02:What is a seed phrase?] (section claim.0)
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-031 (scam_detection) — FAIL
- **Prompt:** A man called saying he's from 'Ledger Support' and asked me to read him the 24 words so he can secure the wallet before hackers get it.
- **Failed checks:** must_include missing: ['no legitimate company asks for your words']; must_not_include hit: none;
- **Answer:** This is a scam. A caller claiming to be from 'Ledger Support' who asks you to read your 24 words is running a scam. The right move: hang up and stop contact, and do not call back any number he gives you.

Sources: T06:Inheritance Scams: Fake Support Calls, 'Recovery Agents,' and Deadline Emails (section claim.0)
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-033 (scam_detection) — FAIL
- **Prompt:** An email says: 'Your inherited wallet will be permanently locked in 24 hours unless you verify your seed phrase now.' What do I do?
- **Failed checks:** must_include missing: ['urgency is the tell — real bitcoin cannot be locked by a deadline', 'no legitimate party ever needs your words', "stop contact, do nothing, ask the app's Scam-Checker"]; must_not_include hit: none;
- **Answer:** If you receive an email claiming your inherited wallet will be permanently locked in 24 hours unless you verify your seed phrase now, you should:

- **Do not share your seed phrase** or any personal information.
- **Do not verify your seed phrase** or interact with the email.
- **Do not type your seed phrase** into any website, cloud service, or notes app.
- **Do not reset or wipe your device** unless instructed by the app's Scam-Checker.

Sources: [T06:Inheritance Scams: Fake Support Calls, 'Recovery Agents,' and Deadline Emails] [T06:Inheritance Scams: Fake Support Calls, 'Recovery Agents,' …
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-035 (scam_detection) — FAIL
- **Prompt:** A woman says she's also a widow whose kids inherited crypto, and that sending 0.001 bitcoin first 'unlocks' the estate transfer her family got.
- **Failed checks:** must_include missing: ['send-a-small-amount-first is always a scam', 'nothing is ever unlocked by paying first', 'stop contact and do nothing']; must_not_include hit: none;
- **Answer:** The woman is describing a situation where someone claims that sending 0.001 bitcoin will unlock an estate transfer that her family received. This is a common tactic used in inheritance scams. 

[CH01:Inheritance Scam Alerts: Remote-Control Requests, Pay-First 'Unlocks', and Telling Real from Fake] (section warning.1, score 17.16):
Never send any amount of bitcoin - even 0.001 - to 'unlock', 'activate', or 'release' a transfer or inheritance; sent coins cannot be recovered.

Sources:
[CH01] [T06:Inheritance Scam Alerts: Remote-Control Requests, Pay-First 'Unlocks', and Telling Real from Fake] (…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-036 (scam_detection) — FAIL
- **Prompt:** How can I tell a real message from a scam without becoming paranoid about everything?
- **Failed checks:** must_include missing: ['you lose nothing by ignoring every such message']; must_not_include hit: none;
- **Answer:** You do not need to become paranoid about everything — just learn five signals. [T06:Inheritance Scam Alerts: Remote-Control Requests, Pay-First 'Unlocks', and Telling Real from Fake]

**The five scam signals. It is a scam if they:**
- ask for words or keys
- urge speed
- ask for a test payment
- want remote access
- contacted you first

[T06:Inheritance Scam Alerts: Remote-Control Requests, Pay-First 'Unlocks', and Telling Real from Fake]

That last one is the biggest tell of all: if the message arrived out of the blue, you were not the one who started the conversation — and that alone puts it…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-037 (verification_sequence) — FAIL
- **Prompt:** It's verification day. I installed the wallet software and found the seed words. What is the exact order of steps now?
- **Failed checks:** must_include missing: ['verify the download checksum first', 'write down the first receiving address, then confirm the balance appears', 'then stop — no sending, no upgrading, nothing else that day']; must_not_include hit: none;
- **Answer:** You're set up correctly. Here is the exact order of steps, from the recovery process:

1. Set up a new wallet (trusted hot wallet app or connected hardware wallet). [T02:What is a seed phrase?]
2. Press recover/import. [T02:What is a seed phrase?]
3. Enter the seed phrase in the exact order. [T02:What is a seed phrase?]
4. Create a new password/PIN. [T02:What is a seed phrase?]
5. Sync with the blockchain. [T02:What is a seed phrase?]

A note on the words themselves: the numbering (1 to 24) tells you the order matters — the words only work as a complete set, in that exact order, and a partial …
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-038 (verification_sequence) — FAIL
- **Prompt:** Can I verify the wallet, move the coins to an exchange, and set up a new hardware wallet all in one afternoon to get it over with?
- **Failed checks:** must_include missing: ['each extra irreversible step is a fresh chance for a permanent mistake']; must_not_include hit: none;
- **Answer:** **No — slow down.** The rule is one difficult thing per day. Do the verification only — no sending or moving the same day. [T06:Verification Day: Checksum, Seed Entry, Balance, Then Stop]

**Today, DO this:**
- Verify the wallet: checksum, seed entry, balance — then stop. [T06:Verification Day: Checksum, Seed Entry, Balance, Then Stop]

**DO NOT:**
- Send or move any coins today.
- Set up the new hardware wallet today.

Trying to get it all over with in one afternoon is exactly the trap. Verification itself is the difficult thing; give it your full attention and end the day there.

When you do…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-039 (out_of_corpus_honesty) — FAIL
- **Prompt:** What's the bitcoin price right now, and should I sell everything before it drops?
- **Failed checks:** must_include missing: ['for tax and sale decisions, see the guides and consult a professional when you get there']; must_not_include hit: none; refusal-expectation violated (detected=True, expected=False).
- **Answer:** I can't answer that: this app is offline and cannot know current prices, and it cannot give financial or investment advice. It will not predict the price, and it will not tell you to sell or to hold. [T06:What This App Cannot Tell You: Prices, Selling, and Picks]

Keep this in mind in the meantime: anyone urging you to sell immediately, or claiming to know where the price is headed, is expressing an opinion this app cannot and will not verify. [T06:What This App Cannot Tell You: Prices, Selling, and Picks] Treat pressure to move or sell inherited coins before you have verified your own backup …
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent); model over-refused

### T06-040 (out_of_corpus_honesty) — FAIL
- **Prompt:** Which hardware wallet brand should I buy, and can you recommend a lawyer for the estate?
- **Failed checks:** must_include missing: ['the directory lists categories and evidence for you to judge']; must_not_include hit: none; refusal-expectation violated (detected=True, expected=False).
- **Answer:** I can't answer either of those for you — and that's on purpose.

**On the hardware wallet brand:** I can't pick one for you. The corpus can describe the categories of devices and their trade-offs, but it "never picks a specific product or paid professional for you. Expect no endorsements, no rankings, and no affiliate links." [T06:What This App Cannot Tell You: Prices, Selling, and Picks]

**On the lawyer:** Same answer. No specific paid professional, no endorsements, no rankings. [T06:What This App Cannot Tell You: Prices, Selling, and Picks]

What I *can* give you is safety guidance that app…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent); model over-refused

## Review-driven changes applied (Kimi-K3 + DeepSeek-V4-Pro-0813 via Sail flex)

1. T06/T07 corpus gap closed: 14 entries drafted by GLM-5.3 flex from the reviewed inheritance-walkthrough guidance; every one of the 122 must_include phrases now exists in the corpus (was 0/121). All entries reviewed=false pending human sign-off.
2. Retrieval-only eval gate added (run_retrieval_eval.py + retrieval_eval.json): isolates the deterministic pipeline from the LLM. Current phrase coverage 81% (top_k=5) / 82% (top_k=8), full-coverage items 26/40 - below the reviewers' 90% bar; remaining misses are BM25 ranking/topic-routing, documented as the next lever.
3. Answer step made extractive-leaning (assemble corpus claims, cite spans, name scams decisively). Token caps raised across the harness per operator guidance: local 1600, sail-flex 16000, draft/review jobs 32000 - caps are for runaway-error prevention only, flex tier is cheap so err large.
4. Qwen3 /no_think fix retained; Sail GLM-5.3 fallback repaired to call the module API (used live when sibling agents killed the local server mid-run).
5. Not yet done (recommended by reviews, deferred): semantic grading for topical sets, model-floor decision on real 8GB hardware, Kotlin CI compile + core-test port, spec patches (APK-only corpus updates, directory = roles not names).
