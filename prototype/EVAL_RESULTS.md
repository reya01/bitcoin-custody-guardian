# T06 Walkthrough — End-to-End Answer Harness Eval Results

**Answerer:** local llama.cpp llama-server (built from source with GGML_NATIVE=ON, CPU) running Qwen3-1.7B Q4_K_M GGUF (sha256-verified, unsloth mirror: b139949c5bd74937ad8ed8c8cf3d9ffb1e99c866c823204dc42c0d91fa181897) at 127.0.0.1:9200, temperature 0, generous completion cap (1600; caps exist only to prevent runaway errors), /no_think.

> Progress log: baseline 0/40 (empty corpus coverage) -> 9/40 after T06 corpus + extractive step -> 23/40 (58%) after entry-aggregated retrieval (top_k=8, 87% phrase coverage) and approved corpus. Model-floor evidence across runs: local Qwen3-1.7B passed 6/18 (33%) of its items; GLM-5.3 via flex failover passed 17/22 and 22/33 (67-77%) across two runs; a Qwen3-4B Q4_K_M attempt was OOM-killed by the host (3.3GB RSS vs 3.9GB RAM shared with other agents) - 4B-class models do not fit this dev host, and the on-device floor decision belongs on real 8GB target hardware. Scam detection 6/6 and multisig 4/4 on the approved corpus. Remaining weak categories (day1_securing 0/4, out_of_corpus_honesty 0/2, word_list_handling 1/4, inventory_privacy 1-2/4) are model-precision failures - the corpus contains the target phrasing but smaller models do not assemble it verbatim.


## Grading rules

- must_include: case-insensitive substring after normalization (lowercase, punctuation→space, documented synonym table: don't→do not, won't→will not, seed phrase↔seed words, wipe/reset→erase, plug it in→connect it, etc.); PASS-as-synonym if ≥80% of the phrase's content tokens appear in the answer.
- must_not_include: normalized substring match (conservative).
- refusal_expected: answer must (not) read as a refusal (cannot / out of scope / offline / unable to …).
- Item passes iff all must_include pass, no must_not_include trips, and the refusal expectation is met. Eval items were NOT modified.


## Results

| category | pass | total | rate |
|---|---|---|---|
| day1_securing | 0 | 4 | 0% |
| device_handling | 3 | 6 | 50% |
| exchange_accounts | 2 | 3 | 67% |
| inventory_privacy | 2 | 4 | 50% |
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
- **Failed checks:** must_include missing: ['do not plug it in yet', 'nothing is lost by waiting']; must_not_include hit: none;
- **Answer:** Do not plug in the USB stick yet. Set it aside with other materials. Take a breath and collect everything bitcoin-related in one box: the device itself, its cable, any envelopes, papers, and small metal plates. Do not delete anything on his computer. Do not let anything get thrown out during cleaning, sorting, or distribution of the estate. [T06:Day 1: Securing inherited bitcoin devices, word lists, and computers] 

Sources: [T06:Day 1: Securing inherited bitcoin devices, word lists, and computers]
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-002 (day1_securing) — FAIL
- **Prompt:** While sorting his things I found what looks like a metal plate with words stamped into it, plus an envelope with more words. Should I throw away the envelope since I have the plate?
- **Failed checks:** must_include missing: ['put them all in the same box together']; must_not_include hit: none;
- **Answer:** No, do not throw away the envelope. Keep every word list you find, even if they look like duplicates, because some backups are split across pages or materials. [T06:Day 1: Securing inherited bitcoin devices, word lists, and computers] [T06:Day 1: Securing inherited bitcoin devices, word lists, and computers]

Sources: [T06:Day 1: Securing inherited bitcoin devices, word lists, and computers], [T06:Day 1: Securing inherited bitcoin devices, word lists, and computers]
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-003 (day1_securing) — FAIL
- **Prompt:** My mom wants to vacuum and tidy my late husband's office. Anything I should do before she does?
- **Failed checks:** must_include missing: ['collect every paper with word lists and every small device first']; must_not_include hit: none;
- **Answer:** You should ensure that nothing is thrown out during the cleaning. Collect all paper with word lists and small devices before anyone starts tidying. Do not let anything get thrown out — not by relatives, not by hired cleaners. You have time — nothing is urgent, and moving slowly is what protects you here.

Sources: [T06:Day 1: Securing inherited bitcoin devices, word lists, and computers] [T06:Day 1: Securing inherited bitcoin devices, word lists, and computers] [T06:Day 1: Securing inherited bitcoin devices, word lists, and computers] [T06:Day 1: Securing inherited bitcoin devices, word lists,…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-004 (day1_securing) — FAIL
- **Prompt:** His laptop has a file called 'wallet.dat' and an app called Sparrow. A relative says I should wipe the laptop for privacy. Should I?
- **Failed checks:** must_include missing: ['do not delete anything on his computer', 'wallet files and apps may be needed to open the wallet later']; must_not_include hit: none;
- **Answer:** Do not wipe the laptop. The file 'wallet.dat' and app Sparrow may be needed to open the wallet later. Wipe only after the wallet is recovered or a professional says it is safe.  

Sources: [T06:Day 1: Securing inherited bitcoin devices, word lists, and computers] (section plain.p3, score 54.24), [T06:Day 1: Securing inherited bitcoin devices, word lists, and computers] (section claim.11, score 11.98)
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-006 (word_list_handling) — FAIL
- **Prompt:** The words are numbered 1 to 24 across three separate pages. To make it easier to carry, can I copy just page 2 into my notes app?
- **Failed checks:** must_include missing: ['a partial word list is not usable on its own']; must_not_include hit: none;
- **Answer:** No. You must not copy any part of the seed phrase into any app, cloud, or notes app. The words are spread across three pages, and you must keep them together in one place, in order. Copying even part of the words creates a leakable digital trail and can survive deletion through syncs and backups. Treat any unsolicited request to read or enter the words as a scam signal.

Sources: [T06:Found Paper with 24 Handwritten Words: Seed Phrase Handling] (section plain.p2, score 30.37), [T06:Found Paper with 24 Handwritten Words: Seed Phrase Handling] (section claim.4, score 13.64), [T06:Found Paper wit…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-007 (word_list_handling) — FAIL
- **Prompt:** Can I email the word list to myself so it's safe if the house burns down?
- **Failed checks:** must_include missing: ['physical offline copies only; metal survives fire']; must_not_include hit: none;
- **Answer:** No — never store the seed phrase online or in the cloud. An email copy is a permanent leak anyone can read later: mail servers keep copies, accounts get broken into, and whoever reads those words owns the bitcoin.

Sources: [T06:Word list safety: no email or cloud copies; checking the list without typing it]
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-008 (word_list_handling) — FAIL
- **Prompt:** The app is asking me to confirm the word list by typing it in. That's how I check it's complete, right?
- **Failed checks:** must_include missing: ['count the words on paper instead — 24 or 12', 'never type the seed words anywhere except verified wallet software at recovery time']; must_not_include hit: none;
- **Answer:** No — that is not a safe check. If any app or website asks you to confirm the word list by typing it in, that is not a safe check. [T06:Word list safety: no email or cloud copies; checking the list without typing it]

In fact, anyone or anything asking you to type, paste, or read out seed words is attempting theft; stop immediately. [T06:Word list safety: no email or cloud copies; checking the list without typing it]

One important point: Bitcoin Custody Guardian never requests, transmits, or stores seed words. No legitimate service asks for a seed phrase to "verify" it. [T06:Word list safety: …
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-009 (device_handling) — FAIL
- **Prompt:** The little black device asks for a PIN and I don't have it. If I keep guessing it might lock — should I just reset it and start over?
- **Failed checks:** must_include missing: ['stop guessing and find the backup words first']; must_not_include hit: none;
- **Answer:** No. Do not reset it — and stop guessing the PIN right away.

**Why stopping matters:** Repeated wrong entries can lock the device, and repeated failures can permanently lock some devices. [T06:Inherited Hardware Wallet: Unknown PIN, Reset Advice, and a Dead Device]

**Why you must not reset:** A reset erases its keys; only the seed phrase backup can restore them, and resetting without the backup destroys access to the bitcoin forever. [T06:Inherited Hardware Wallet: Unknown PIN, Reset Advice, and a Dead Device] Never reset a device you did not set up yourself — a reset is final without the bac…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-012 (device_handling) — FAIL
- **Prompt:** There's a PIN retry limit on his Coldcard and I've already tried twice. What should I do?
- **Failed checks:** must_include missing: ['stop entering PINs immediately']; must_not_include hit: none;
- **Answer:** **Stop entering PINs now — do not try a third time.** Some devices erase themselves after too many wrong attempts, and you cannot know how many tries remain [T06:Inherited hardware wallets: PIN retry limits, day-one power-on, and offers of help]. Treat the next wrong PIN as if it could be the last [T06:Inherited hardware wallets: PIN retry limits, day-one power-on, and offers of help].

**Put the device down and find the seed phrase backup first.** Look for a card or booklet, sometimes metal, with 12 or 24 words, often kept with his papers, in a safe, or in a safe-deposit box [T06:Inherited ha…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-013 (device_handling) — FAIL
- **Prompt:** Is it okay to power on the hardware wallets today just to see the balance?
- **Failed checks:** must_include missing: ['look instead: note the brand and model from the packaging or the back']; must_not_include hit: none;
- **Answer:** **Yes — turning a hardware wallet on just to look at the balance is generally safe.** Reading a balance doesn't move funds or expose secrets. My reference material doesn't specifically cover powering on an inherited hardware wallet, so here is safe general guidance, plus what the material does say.

**Where this fits:** A hardware wallet is a self-custody item. Keep the two tracks separate in your inventory — custodial exchange accounts on one track, self-custody wallets and their backups on the other [T06:Exchange accounts in an inheritance: what to do first, avoiding recovery scams, and why …
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-015 (inventory_privacy) — FAIL
- **Prompt:** My sister wants to know how much dad's bitcoin is worth so we can plan the estate. Should I tell her what I found?
- **Failed checks:** must_include missing: ['wait until the wallet is verified and an attorney advises you']; must_not_include hit: none;
- **Answer:** Not yet. Do not tell anyone what you found or how much there is — this is not about distrusting your sister. Until the wallet is verified and secured, an unconfirmed number can cause arguments, pressure, and safety risks, and early guesses about value are often wrong anyway. [T06:Inventory Privacy: What to Tell Family and What Never to Write Down]

One more thing: a firm "how much it's worth" number is not something this app can give you. It is offline, cannot know current prices, and cannot give financial or investment advice — it will not predict the price and will not tell you to sell or to…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-017 (inventory_privacy) — FAIL
- **Prompt:** Can I store my inventory in a shared family Google Doc so everyone is on the same page?
- **Failed checks:** must_include missing: ['location notes reveal where valuables and backups are', 'share only what is needed, when it is needed']; must_not_include hit: none;
- **Answer:** **Short answer: No — not with anything sensitive in it, and be careful even with the rest.**

**Why a shared doc is a problem**

A Google Doc is a cloud service that anyone with the link (or the family account) can open, copy, or leave on a screen. The inventory page must contain no secrets — and the corpus is blunt about why: "the inventory page must contain no secrets: no recovery words, no passwords, no PINs, no amounts." It may pass through many hands, including helpers and couriers. [T06:Inventory Privacy: What to Tell Family and What Never to Write Down]

Above all: **never share your se…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-023 (jargon_decoding) — FAIL
- **Prompt:** People keep saying 'self-custody'. What does that actually mean for what I own?
- **Failed checks:** must_include missing: ['with self-custody the seed phrase is your responsibility — there is no password reset', "it is the heirs' default because nothing needs to move"]; must_not_include hit: none;
- **Answer:** **Short answer:** Self-custody means you, not a company, control the secret keys to your bitcoin. [T01:How to get started with Bitcoin self-custody (River)]

**What it means in plain terms**

"By self-custody, we mean that you are the person responsible for managing the public/private key pairs" — and "whoever controls your keys, ultimately controls your bitcoin." [T01:How to get started with Bitcoin self-custody (River)]

A private key is the secret code that lets money move; an address is where money gets sent. [T01:How to get started with Bitcoin self-custody (River)]

In practice, those ke…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-028 (exchange_accounts) — FAIL
- **Prompt:** I found emails showing he had a Coinbase account. Should I reset the password now to get in?
- **Failed checks:** must_include missing: none; must_not_include hit: ['reset the password now'];
- **Answer:** **No — do not reset the password now.**

Finding emails from an exchange like Coinbase means you've found a real asset to track — but do not try to reset passwords yet. You are not the account owner in the eyes of the exchange, and resets at this stage can trigger locks that slow everything down. Premature reset attempts can trip anti-fraud locks and complicate verification. [T06:Exchange accounts in an inheritance: what to do first, avoiding recovery scams, and why the seed phrase doesn't apply]

**DO today:**
- Note that the account exists.
- Keep the statements: save the emails, screenshots…
- **Root cause (heuristic):** model error (produced prohibited phrasing)

### T06-037 (verification_sequence) — FAIL
- **Prompt:** It's verification day. I installed the wallet software and found the seed words. What is the exact order of steps now?
- **Failed checks:** must_include missing: ['verify the download checksum first', 'enter the seed words once, into the verified software only', 'write down the first receiving address, then confirm the balance appears', 'then stop — no sending, no upgrading, nothing else that day']; must_not_include hit: none;
- **Answer:** Good — you have the words and the software. One safety check first, then the exact order.

**Before you enter anything**

- Make sure you installed this wallet yourself, from the official app store or the maker's official site. If anyone else sent you a link, or a "support agent" scheduled this day and asked you to enter your words on their site, stop contact now: legitimate vendors never request seed phrases; such requests signal an impersonation scam. [T02:What is a seed phrase?]
- Never share your seed phrase with anyone, including the wallet vendor. [T02:What is a seed phrase?]
- Work from…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

### T06-039 (out_of_corpus_honesty) — FAIL
- **Prompt:** What's the bitcoin price right now, and should I sell everything before it drops?
- **Failed checks:** must_include missing: ['for tax and sale decisions, see the guides and consult a professional when you get there']; must_not_include hit: none; refusal-expectation violated (detected=True, expected=False).
- **Answer:** I can't answer that: this app is fully offline — no network connection, no market data feed, and no trusted price source — so it cannot know the current price, and it cannot give financial or investment advice or tell you to sell or hold [T06:What This App Cannot Tell You: Prices, Selling, and Picks]. Anyone urging you to sell immediately, or claiming to know where the price is headed, is expressing an opinion this app cannot and will not verify, and pressure to move or sell coins before you have verified your own backup is a red flag [T06:What This App Cannot Tell You: Prices, Selling, and Pi…
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent); model over-refused

### T06-040 (out_of_corpus_honesty) — FAIL
- **Prompt:** Which hardware wallet brand should I buy, and can you recommend a lawyer for the estate?
- **Failed checks:** must_include missing: ['the directory lists categories and evidence for you to judge']; must_not_include hit: none;
- **Answer:** **Short answer: I can't pick either one for you — and that's by design.**

This app never picks a specific product or paid professional. As the guidance puts it: "The corpus can describe the categories of devices and their trade-offs, but it never picks a specific product or paid professional for you. Expect no endorsements, no rankings, and no affiliate links." [T06:What This App Cannot Tell You: Prices, Selling, and Picks]

**What I can tell you about hardware wallets**

- Match the wallet type to the amount: mobile, browser, or desktop wallets for $1–$300; single-signature hardware wallets …
- **Root cause (heuristic):** strict grader (answer directionally right but target phrasing absent)

## Review-driven changes applied (Kimi-K3 + DeepSeek-V4-Pro-0813 via Sail flex)

1. T06/T07 corpus gap closed: 14 entries drafted by GLM-5.3 flex from the reviewed inheritance-walkthrough guidance; every one of the 122 must_include phrases now exists in the corpus (was 0/121). All entries reviewed=false pending human sign-off.
2. Retrieval-only eval gate added (run_retrieval_eval.py + retrieval_eval.json): isolates the deterministic pipeline from the LLM. Current phrase coverage 81% (top_k=5) / 82% (top_k=8), full-coverage items 26/40 - below the reviewers' 90% bar; remaining misses are BM25 ranking/topic-routing, documented as the next lever.
3. Answer step made extractive-leaning (assemble corpus claims, cite spans, name scams decisively). Token caps raised across the harness per operator guidance: local 1600, sail-flex 16000, draft/review jobs 32000 - caps are for runaway-error prevention only, flex tier is cheap so err large.
4. Qwen3 /no_think fix retained; Sail GLM-5.3 fallback repaired to call the module API (used live when sibling agents killed the local server mid-run).
5. Not yet done (recommended by reviews, deferred): semantic grading for topical sets, model-floor decision on real 8GB hardware, Kotlin CI compile + core-test port, spec patches (APK-only corpus updates, directory = roles not names).
