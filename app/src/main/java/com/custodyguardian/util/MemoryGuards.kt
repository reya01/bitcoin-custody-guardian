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

    fun loadWordlist(context: Context): Set<String> =
        wordlist ?: synchronized(this) {
            wordlist ?: run {
                val words = AssetLoader.readAssetLines(context, "bip39_wordlist.txt")
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() }
                    .toSet()
                wordlist = words
                words
            }
        }

    sealed interface Result {
        /** Nothing secret-looking found. Safe to process. */
        data object Safe : Result
        /** Seed-phrase-like material detected. Refuse to process. */
        data class SeedPhrase(
            val wordCount: Int,
            val matchedWords: List<String>,
            val confidence: Double,
            val message: String,
        ) : Result
    }

    /**
     * Detects a probable BIP-39 seed phrase in free text.
     *
     * Heuristic: tokenize on non-letters, count how many tokens (>=3 chars) are
     * BIP-39 words, and how many of them appear *consecutively* as a run.
     * A run of 8+ BIP-39 words, or >=90% of the tokens matching, is flagged.
     */
    fun detectSeedPhrase(text: String, wordlist: Set<String>): Result {
        if (text.isBlank()) return Result.Safe
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
                matchedWords = matched,
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
}
