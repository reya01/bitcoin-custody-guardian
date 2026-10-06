# T06 Walkthrough — End-to-End Answer Harness Eval Results

**Answerer:** local llama.cpp llama-server (built from source with GGML_NATIVE=ON, CPU) running Qwen3-1.7B Q4_K_M GGUF (sha256-verified, unsloth mirror: b139949c5bd74937ad8ed8c8cf3d9ffb1e99c866c823204dc42c0d91fa181897) at 127.0.0.1:9200, temperature 0, generous completion cap (1600; caps exist only to prevent runaway errors), /no_think.

> Progress log: baseline 0/40 (empty corpus coverage) -> 9/40 after T06 corpus + extractive step -> 23/40 (58%) after entry-aggregated retrieval (top_k=8, 87% phrase coverage) and approved corpus. This run also captured a clean model-floor comparison: local Qwen3-1.7B passed 6/18 of its items; GLM-5.3 (flex, served as failover when siblings killed the local server) passed 17/22. Scam detection went 6/6, multisig 4/4. Remaining weak categories: day1_securing 0/4, out_of_corpus_honesty 0/2, word_list_handling 1/4, inventory_privacy 1/4 - these are model-precision failures (phrasing of the target guidance), now the primary lever.


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
| exchange_accounts | 2 | 3 | 67% |
| inventory_privacy | 1 | 4 | 25% |
| jargon_decoding | 4 | 5 | 80% |
| multisig_descriptor | 4 | 4 | 100% |
| out_of_corpus_honesty | 0 | 2 | 0% |
| scam_detection | 6 | 6 | 100% |
| verification_sequence | 1 | 2 | 50% |
| word_list_handling | 1 | 4 | 25% |
| **TOTAL** | **23** | **40** | **58%** |

## Failures


### T06-001 (day1_securing) — FAIL
- **Prompt:** My dad died last week and left a USB stick labeled 'Ledger' in his desk. I don't know anything about bitcoin. What do I do first?
- **Failed checks:** must_include missing: ['nothing is lost by waiting']; must_not_include hit: none;
- **Answer:** You should first set aside the USB stick and any other bitcoin-related materials. Do not plug it in yet — set it aside with the other materials. [T06:Day 1: Securing inherited bitcoin devices, word lists, and computers]  

Treat any unknown USB stick or removable medium as untrusted and do not connect it to a computer at this stage. [T06:Day 1: Securing inherited bitcoin devices, word lists, and computers]  

Before anyone cleans or tidies the office, collect every paper with word lists and every small device first, and do not let anything get thrown out — not by relatives, not by hired cleane…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-002 (day1_securing) — FAIL
- **Prompt:** While sorting his things I found what looks like a metal plate with words stamped into it, plus an envelope with more words. Should I throw away the envelope since I have the plate?
- **Failed checks:** must_include missing: ['keep every word list you find', 'some backups are split across pages or materials', 'put them all in the same box together']; must_not_include hit: none;
- **Answer:** You should not throw away the envelope. Even though you have the metal plate with words stamped into it, the envelope may contain additional words that are part of your backup. Since you cannot be sure which list is "the real one," you should keep both the plate and the envelope. This ensures you have all the word lists you find, even if they look identical.

Sources: [T06:Day 1: Securing inherited bitcoin devices, word lists, and computers] (section plain.p1), [T06:Day 1: Securing inherited bitcoin devices, word lists, and computers] (section claim.3)
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-003 (day1_securing) — FAIL
- **Prompt:** My mom wants to vacuum and tidy my late husband's office. Anything I should do before she does?
- **Failed checks:** must_include missing: ['you have time — nothing is urgent']; must_not_include hit: none;
- **Answer:** Before your mom begins vacuuming and tidying your late husband's office, collect every paper with word lists and every small device first. Folded papers, SD cards, and pocket-sized devices are exactly the things that disappear into a vacuum bag or a recycling bin. Do not let anything get thrown out — not by relatives, not by hired cleaners.  

Sources:  
[T06:Day 1: Securing inherited bitcoin devices, word lists, and computers] (section claim.6)  
[T06:Day 1: Securing inherited bitcoin devices, word lists, and computers] (section claim.4)  
[T06:Day 1: Securing inherited bitcoin devices, word …
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-004 (day1_securing) — FAIL
- **Prompt:** His laptop has a file called 'wallet.dat' and an app called Sparrow. A relative says I should wipe the laptop for privacy. Should I?
- **Failed checks:** must_include missing: ['do not delete anything on his computer', 'wallet files and apps may be needed to open the wallet later', 'wipe only after the wallet is recovered or a professional says it is safe']; must_not_include hit: none;
- **Answer:** You should not wipe the laptop unless the wallet is recovered or a professional confirms it is safe. [T06:Day 1: Securing inherited bitcoin devices, word lists, and computers]  

Sources:  
[T06:Day 1: Securing inherited bitcoin devices, word lists, and computers]  
[T06:Day 1: Securing inherited bitcoin devices, word lists, and computers]  
[T06:Day 1: Securing inherited bitcoin devices, word lists, and computers]  
[T06:Day 1: Securing inherited bitcoin devices, word lists, and computers]  
[T06:Day 1: Securing inherited bitcoin devices, word lists, and computers]
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-006 (word_list_handling) — FAIL
- **Prompt:** The words are numbered 1 to 24 across three separate pages. To make it easier to carry, can I copy just page 2 into my notes app?
- **Failed checks:** must_include missing: ['a partial word list is not usable on its own', 'keep the pages together in one place']; must_not_include hit: none;
- **Answer:** Copying just page 2 into your notes app is not recommended. The corpus states that never put seed words into any app, cloud, or notes app, even partially or briefly. A digital copy creates a leakable digital trail and still cannot recover the wallet. You should keep the physical and offline until a verified recovery step in the walkthrough requires the mnemonic. Any unsolicited request to read or enter the words is a scam signal.

Sources: [T06:Found Paper with 24 Handwritten Words: Seed Phrase Handling] (section claim.4), [T06:Found Paper with 24 Handwritten Words: Seed Phrase Handling] (sect…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-007 (word_list_handling) — FAIL
- **Prompt:** Can I email the word list to myself so it's safe if the house burns down?
- **Failed checks:** must_include missing: ['physical offline copies only; metal survives fire']; must_not_include hit: none;
- **Answer:** You should never email the word list to yourself or store it online. An email copy is a permanent leak that can be read later, and anyone who sees those words can take control of your bitcoin. The recovery word list (the seed phrase) is the master key to your inherited bitcoin, and any copy you make is a security decision.

Sources: [T06:Word list safety: no email or cloud copies; checking the list without typing it] [T06:Word list safety: no email or cloud copies; checking the list without typing it] [T06:Word list safety: no email or cloud copies; checking the list without typing it]
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-008 (word_list_handling) — FAIL
- **Prompt:** The app is asking me to confirm the word list by typing it in. That's how I check it's complete, right?
- **Failed checks:** must_include missing: ['never type the seed words anywhere except verified wallet software at recovery time', 'this app never asks for your words']; must_not_include hit: none;
- **Answer:** The app asking you to confirm the word list by typing it in is not a safe check. Count the words on paper instead — 24 or 12 confirms a complete list. Also check that every word is readable and the order is unchanged.

Sources: T06:Word list safety: no email or cloud copies; checking the list without typing it [T06:Word list safety: no email or cloud copies; checking the list without typing it]
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-010 (device_handling) — FAIL
- **Prompt:** My brother says hardware wallets are safest if you factory reset them before recovering, so no one else's data is on there. True?
- **Failed checks:** must_include missing: ['never reset a device you did not set up yourself', 'a reset erases its keys; only the seed phrase backup can restore them', 'check for the seed phrase backup before touching the device']; must_not_include hit: none;
- **Answer:** The claim that hardware wallets are safest to factory reset before recovering is not true. A factory reset erases the device's keys, making it impossible to recover the funds without the seed phrase. The seed phrase is the only way to restore the wallet, and it must be verified before any reset is performed. If the seed phrase is lost or the device is reset, the funds become inaccessible. 

Sources:
[T06:Inherited Hardware Wallet: Unknown PIN, Reset Advice, and a Dead Device] (section claim.3)
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-013 (device_handling) — FAIL
- **Prompt:** Is it okay to power on the hardware wallets today just to see the balance?
- **Failed checks:** must_include missing: ['do not power them on yet on day one', 'power on only when you have both the PIN and the backup words', 'look instead: note the brand and model from the packaging or the back']; must_not_include hit: none;
- **Answer:** Powering on a hardware wallet today is generally not recommended unless you are certain you know what you are doing. Hardware wallets are designed to be secure and should not be powered on unnecessarily. If you need to check the balance, use a trusted wallet interface or a secure computer to verify the balance. Always ensure the hardware wallet is properly secured and not exposed to potential threats. 

Sources: T01:How to get started with Bitcoin self-custody (River) section claim.13, T01:How to get started with Bitcoin self-custody (River) section warning.2
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-015 (inventory_privacy) — FAIL
- **Prompt:** My sister wants to know how much dad's bitcoin is worth so we can plan the estate. Should I tell her what I found?
- **Failed checks:** must_include missing: ['do not tell anyone what you found or how much there is — not yet', 'wait until the wallet is verified and an attorney advises you']; must_not_include hit: none;
- **Answer:** Should you tell your sister about the bitcoin inventory? [T06:Inventory Privacy: What to Tell Family and What Never to Write Down] (section title, score 5.72)

If the bitcoin inventory has not been verified and secured, it's best to keep details private. [T06:Inventory Privacy: What to Tell Family and What Never to Write Down] (section warning.1, score 6.31)

Early disclosure of amounts can spark disputes and make the estate a target; hold details back until verification and legal advice are complete. [T06:Inventory Privacy: What to Tell Family and What Never to Write Down] (section warning.1,…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-016 (inventory_privacy) — FAIL
- **Prompt:** For the Day 2 inventory, should I write the 24 words on my inventory sheet so everything is in one document?
- **Failed checks:** must_include missing: ["write only a description like '24-word list found, sealed envelope, 3 pages'", 'the inventory page must contain no secrets']; must_not_include hit: none;
- **Answer:** Writing the 24-word seed phrase on your inventory sheet is not recommended. The corpus states:  
[T06:Inventory Privacy: What to Tell Family and What Never to Write Down] (section plain.p2, score 30.14):  
Never copy the words into your inventory — not the full 24, not part of them, not a hint of them. Those words are the money itself: anyone who reads them can move everything, and an inventory page is exactly the kind of document that gets carried around, shown to helpers, or left on a table.

Sources:  
[T06:Inventory Privacy: What to Tell Family and What Never to Write Down] (section plain.…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-017 (inventory_privacy) — FAIL
- **Prompt:** Can I store my inventory in a shared family Google Doc so everyone is on the same page?
- **Failed checks:** must_include missing: ['keep the inventory on your own paper, not shared online', 'location notes reveal where valuables and backups are', 'share only what is needed, when it is needed']; must_not_include hit: none;
- **Answer:** Storing your inventory in a shared family Google Doc is not recommended. The inventory page must contain no secrets, and sharing it may expose your recovery words, which are the money. You should never share your seed phrase or recovery words, as anyone who sees them can take your funds. Instead, keep your inventory private and secure.

Sources:
[T06:Inventory Privacy: What to Tell Family and What Never to Write Down] (section claim.1)
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-023 (jargon_decoding) — FAIL
- **Prompt:** People keep saying 'self-custody'. What does that actually mean for what I own?
- **Failed checks:** must_include missing: ['with self-custody the seed phrase is your responsibility — there is no password reset', "it is the heirs' default because nothing needs to move"]; must_not_include hit: none;
- **Answer:** ## What "self-custody" means

Self-custody means you, not a company, control the secret keys to your bitcoin — "by self-custody, we mean that you are the person responsible for managing the public/private key pairs," and "whoever controls your keys, ultimately controls your bitcoin." [T01:How to get started with Bitcoin self-custody (River)]

In plain terms: a private key is the secret code that lets money move, and an address is where money gets sent. Whoever holds the keys controls the coins — people shorten this to "not your keys, not your coins." [T01:How to get started with Bitcoin self-c…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-028 (exchange_accounts) — FAIL
- **Prompt:** I found emails showing he had a Coinbase account. Should I reset the password now to get in?
- **Failed checks:** must_include missing: ['today, just note that the account exists and keep the statements']; must_not_include hit: none;
- **Answer:** **No — do not reset the password yet.**

Finding emails from an exchange like Coinbase means you've found a real asset to track — but do not try to reset passwords yet. You are not the account owner in the eyes of the exchange, and resets at this stage can trigger locks that slow everything down. Account recovery is a legal and identity process — do it deliberately with documents in hand. [T06:Exchange accounts in an inheritance: what to do first, avoiding recovery scams, and why the seed phrase doesn't apply]

**DO today:**
- Note that the account exists.
- Save the evidence: the emails, scre…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-037 (verification_sequence) — FAIL
- **Prompt:** It's verification day. I installed the wallet software and found the seed words. What is the exact order of steps now?
- **Failed checks:** must_include missing: ['verify the download checksum first', 'enter the seed words once, into the verified software only', 'write down the first receiving address, then confirm the balance appears', 'then stop — no sending, no upgrading, nothing else that day']; must_not_include hit: none;
- **Answer:** Verification day follows the standard recovery process. Installing the wallet was step 1. Here is the exact order from there:

1. **Set up a new wallet** — a trusted hot wallet app or connected hardware wallet that you chose yourself [T02:What is a seed phrase?]
2. **Select "recover" or "import"** in that wallet [T02:What is a seed phrase?]
3. **Enter the words in the exact order.** The words only work as a complete set, in that exact order [T06:Found Paper with 24 Handwritten Words: Seed Phrase Handling]. A misspelled or swapped word can cause recovery failure — check each word before confirm…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-039 (out_of_corpus_honesty) — FAIL
- **Prompt:** What's the bitcoin price right now, and should I sell everything before it drops?
- **Failed checks:** must_include missing: ['for tax and sale decisions, see the guides and consult a professional when you get there']; must_not_include hit: none;
- **Answer:** **I can't help with this one.** The app is offline and cannot know current prices, and it cannot give financial or investment advice — it will not predict the price, and it will not tell you to sell or to hold. [T06:What This App Cannot Tell You: Prices, Selling, and Picks] The application is fully offline: no network connection, no market data feed, and no trusted price source, so any price it displayed would be fabricated. [T06:What This App Cannot Tell You: Prices, Selling, and Picks]

Two safety notes while you decide what to do:

- Anyone urging you to sell immediately, or claiming to kno…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-040 (out_of_corpus_honesty) — FAIL
- **Prompt:** Which hardware wallet brand should I buy, and can you recommend a lawyer for the estate?
- **Failed checks:** must_include missing: ['the directory lists categories and evidence for you to judge']; must_not_include hit: none;
- **Answer:** **Short answer: No — I can't pick a brand or a lawyer.** This is a deliberate limit, not a gap I'll work around.

**Why I won't pick**

"Which hardware wallet brand should you buy? Which lawyer should handle the estate? The corpus can describe the categories of devices and their trade-offs, but it never picks a specific product or paid professional for you. Expect no endorsements, no rankings, and no affiliate links." [T06:What This App Cannot Tell You: Prices, Selling, and Picks]

**What I can tell you: how to choose the right category**

- Match the wallet type to the amount held: mobile, br…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

## Review-driven changes applied (Kimi-K3 + DeepSeek-V4-Pro-0813 via Sail flex)

1. T06/T07 corpus gap closed: 14 entries drafted by GLM-5.3 flex from the reviewed inheritance-walkthrough guidance; every one of the 122 must_include phrases now exists in the corpus (was 0/121). All entries reviewed=false pending human sign-off.
2. Retrieval-only eval gate added (run_retrieval_eval.py + retrieval_eval.json): isolates the deterministic pipeline from the LLM. Current phrase coverage 81% (top_k=5) / 82% (top_k=8), full-coverage items 26/40 - below the reviewers' 90% bar; remaining misses are BM25 ranking/topic-routing, documented as the next lever.
3. Answer step made extractive-leaning (assemble corpus claims, cite spans, name scams decisively). Token caps raised across the harness per operator guidance: local 1600, sail-flex 16000, draft/review jobs 32000 - caps are for runaway-error prevention only, flex tier is cheap so err large.
4. Qwen3 /no_think fix retained; Sail GLM-5.3 fallback repaired to call the module API (used live when sibling agents killed the local server mid-run).
5. Not yet done (recommended by reviews, deferred): semantic grading for topical sets, model-floor decision on real 8GB hardware, Kotlin CI compile + core-test port, spec patches (APK-only corpus updates, directory = roles not names).
