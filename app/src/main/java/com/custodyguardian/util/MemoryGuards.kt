package com.custodyguardian.util

import android.content.Context
import com.custodyguardian.data.AssetLoader

/**
 * Secret-material detector — spec §3 principle 2: "The model never touches
 * secrets." Detects seed-phrase-like input in any pasted/typed text and the
 * UI must warn + refuse to process it. No network, no storage.
 */
object MemoryGuards {

    /** Cached BIP-39 English wordlist (2,048 words) from assets/bip39_wordlist.txt. */
    @Volatile
    private var wordlist: Set<String>? = null

    /**
     * Fail closed (full-repo review 2026-10-29): a missing/truncated wordlist
     * silently disabled seed detection on every path. Now the load RESULT
     * carries validity; callers must treat an invalid list as "cannot scan"
     * and refuse to process text (see [detectSeedPhrase]).
     */
    data class Wordlist(val words: Set<String>, val valid: Boolean)

    fun loadWordlistValidated(context: Context): Wordlist =
        wordlist?.let { Wordlist(it, it.size == WORDLIST_SIZE) }
            ?: synchronized(this) {
                wordlist?.let { return Wordlist(it, it.size == WORDLIST_SIZE) }
                val words = AssetLoader.readAssetLines(context, "bip39_wordlist.txt")
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() }
                    .toSet()
                wordlist = words
                Wordlist(words, words.size == WORDLIST_SIZE)
            }

    const val WORDLIST_SIZE = 2048

    fun loadWordlist(context: Context): Set<String> = loadWordlistValidated(context).words

    sealed interface Result {
        /** Nothing secret-looking found. Safe to process. */
        data object Safe : Result
        /** Seed-phrase-like material detected. Refuse to process. */
        data class SeedPhrase(
            val wordCount: Int,
            val confidence: Double,
            override val message: String,
        ) : Refusal
    }

    /** Wordlist asset invalid — cannot scan safely; refuse to process (fail closed). */
    data class WordlistInvalid(
        override val message: String,
    ) : Refusal

    /** Any detected secret material: refuse to process, never echo, never store. */
    interface Refusal : Result {
        val message: String
    }

    /**
     * Detects a probable BIP-39 seed phrase in free text.
     *
     * Heuristic: tokenize on non-letters, count how many tokens (>=3 chars) are
     * BIP-39 words, and how many of them appear *consecutively* as a run.
     * A run of 8+ BIP-39 words, or >=90% of the tokens matching, is flagged.
     *
     * Fail closed (full-repo review 2026-10-29): an invalid wordlist
     * (missing/truncated asset) REFUSES to process instead of scanning with
     * a partial list — seed detection must never fail open.
     *
     * matchedWords was dropped from the result (review finding): the refusal
     * result must never carry a copy of the user's words.
     */
    fun detectSeedPhrase(text: String, wordlist: Set<String>): Result {
        if (text.isBlank()) return Result.Safe
        if (wordlist.size != WORDLIST_SIZE) {
            return WordlistInvalid(
                message = "⚠ This app's safety wordlist looks damaged, so it cannot " +
                    "check this text safely. Nothing was processed or sent anywhere. " +
                    "Reinstall the app from the official release before using it.",
            )
        }
        val tokens = text.lowercase()
            .split(Regex("[^a-z]+"))
            .filter { it.length >= 3 }
        if (tokens.isEmpty()) return Result.Safe

        val matched = tokens.filter { it in wordlist }
        if (matched.isEmpty()) return Result.Safe

        // Longest consecutive run of BIP-39 words.
        var longest = 0
        var run = 0
        for (t in tokens) {
            if (t in wordlist) { run++; longest = maxOf(longest, run) } else run = 0
        }

        val ratio = matched.size.toDouble() / tokens.size
        val flagged = longest >= 8 || (tokens.size in 12..24 && ratio >= 0.9) ||
            (longest >= 12)

        return if (flagged) {
            Result.SeedPhrase(
                wordCount = longest,
                confidence = ratio,
                message = "⚠ This looks like seed-phrase material ($longest BIP-39 words in a row). " +
                    "Never type or paste your seed phrase into ANY app — including this one. " +
                    "This app has refused to process the text. No legitimate party — wallet vendor, " +
                    "support agent, or 'recovery expert' — will ever need your words. If someone " +
                    "asked you for them, it is a scam. Move the text out of this screen: select-all " +
                    "and delete, and handle the phrase only on paper."
            )
        } else {
            Result.Safe
        }
    }

    /** Convenience wrapper that loads the wordlist from assets and scans the text. */
    fun detectSeedPhrase(context: Context, text: String): Result =
        detectSeedPhrase(text, loadWordlist(context))

    // xprv-family extended private keys (x/t/v/z/u/p/r/y prv) base58-ish bodies,
    // and WIF private keys (5/K/L prefixes). Raw 64-char hex is deliberately NOT
    // detected: it is indistinguishable from txids (guaranteed false positives).
    private val XPRV_RE = Regex("""\b[xtvzupry]prv[1-9A-HJ-NP-Za-km-z]{50,}\b""")
    private val WIF_RE = Regex("""\b[5KL][1-9A-HJ-NP-Za-km-z]{50,51}\b""")

    data class ExtendedSecret(
        val kind: String,
        override val message: String,
    ) : Refusal

    /**
     * Extended-secret detection: xprv-family and WIF keys. Same contract as
     * detectSeedPhrase: refuse to process, never echo the material.
     */
    fun detectExtendedSecret(text: String): Result {
        if (text.isBlank()) return Result.Safe
        val x = XPRV_RE.find(text)
        if (x != null) return ExtendedSecret(
            kind = "xprv",
            message = "⚠ This looks like an extended private key (${x.value.take(4)}…). " +
                "Anyone holding it can spend every wallet it derives. Never type or paste " +
                "it into ANY app — including this one. This app has refused to process the " +
                "text. Delete it from this screen and keep the key only in cold storage."
        )
        if (WIF_RE.containsMatchIn(text)) return ExtendedSecret(
            kind = "wif",
            message = "⚠ This looks like a private key in WIF format. Anyone holding it can " +
                "spend the funds. Never type or paste it into ANY app — including this one. " +
                "This app has refused to process the text. Delete it from this screen."
        )
        return Result.Safe
    }

    /**
     * Single guarded entry point for chat: seed-phrase detection FIRST, then
     * extended-secret detection. Both chat buttons must call this — no path
     * reaches the model without it (external review C1 / SAFETY_CASE M3).
     */
    fun detectSecret(context: Context, text: String): Result {
        val seed = detectSeedPhrase(context, text)
        if (seed is Result.SeedPhrase) return seed
        return detectExtendedSecret(text)
    }
}
