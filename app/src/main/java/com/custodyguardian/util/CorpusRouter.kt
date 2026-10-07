package com.custodyguardian.util

import android.content.Context
import com.custodyguardian.data.AssetLoader
import com.custodyguardian.data.CorpusEntry

/**
 * Deterministic router — spec §8 "Safety Router": intent classification with
 * NO LLM. Bundled port of the router logic: simple keyword scoring against
 * topic buckets, mapping a user query to an intent and a retrieved corpus
 * slice. Fail-closed: unknown input routes to the refusal template.
 */
object CorpusRouter {

    data class Retrieved(
        val id: String,
        val entry: CorpusEntry,
        val score: Int,
    )

    data class Route(
        val detectedIntent: String,
        val keywordsMatched: List<String>,
        val retrieved: List<Retrieved>,
        val isRefusal: Boolean,
        val note: String,
    )

    // Intent buckets: keyword -> weight. Low-tech, auditable, hand-reviewed.
    private val intentKeywords: Map<String, Map<String, Int>> = mapOf(
        "seed_phrase" to mapOf(
            "seed" to 3, "phrase" to 3, "words" to 2, "12" to 1, "24" to 1,
            "bip39" to 2, "mnemonic" to 3, "recovery" to 2, "backup" to 2,
        ),
        "hardware_wallet" to mapOf(
            "hardware" to 3, "ledger" to 3, "device" to 2, "trezor" to 2,
            "bitbox" to 2, "jade" to 2, "coldcard" to 2, "seedsigner" to 2,
            "bitkey" to 2, "pin" to 1, "reset" to 2, "wipe" to 2,
        ),
        "inheritance" to mapOf(
            "died" to 3, "inherit" to 3, "passed" to 2, "father" to 2, "mother" to 2,
            "spouse" to 2, "estate" to 2, "will" to 1, "heir" to 3, "walkthrough" to 2,
        ),
        "scams" to mapOf(
            "scam" to 3, "email" to 2, "message" to 1, "recovery agent" to 3,
            "support" to 2, "verify" to 1, "urgent" to 2, "deadline" to 2,
            "anydesk" to 3, "teamviewer" to 3, "offer" to 1,
        ),
        "exchanges" to mapOf(
            "exchange" to 3, "account" to 2, "river" to 2, "strike" to 2, "swan" to 2,
            "coinbase" to 2, "custodian" to 2, "proof of reserves" to 2,
        ),
        "software" to mapOf(
            "sparrow" to 3, "electrum" to 3, "bluewallet" to 2, "muun" to 2,
            "nunchuk" to 2, "app" to 1, "wallet file" to 2, "descriptor" to 2,
        ),
    )

    private const val MIN_SCORE = 3

    fun route(
        context: Context,
        query: String,
        corpus: List<Pair<String, CorpusEntry>>,
    ): Route {
        val q = query.lowercase()

        // Score each intent bucket.
        var bestIntent = ""
        var bestScore = 0
        val matched = mutableSetOf<String>()
        for ((intent, kws) in intentKeywords) {
            var score = 0
            for ((kw, w) in kws) {
                if (kw in q) { score += w; matched.add(kw) }
            }
            if (score > bestScore) { bestScore = score; bestIntent = intent }
        }

        // Keyword-score retrieval over the corpus titles + plain text.
        val retrieved = corpus.mapNotNull { (id, entry) ->
            var score = 0
            for (kw in matched) {
                if (kw in entry.title.lowercase()) score += 3
                val plainLower = entry.plain.lowercase()
                if (kw in plainLower) score += 1
            }
            if (bestIntent.isNotEmpty() && bestIntent in entry.topic_ids.map { it.lowercase() })
                score += 2
            if (score > 0) Retrieved(id, entry, score) else null
        }.sortedByDescending { it.score }

        return if (bestScore < MIN_SCORE || retrieved.isEmpty()) {
            Route(
                detectedIntent = bestIntent.ifEmpty { "unknown" },
                keywordsMatched = matched.toList(),
                retrieved = emptyList(),
                isRefusal = true,
                note = "This question is outside what I can answer from the bundled knowledge " +
                    "corpus. I would rather say \"I don't know\" than guess. Check the corpus " +
                    "browser for topics I cover, or ask a human professional for anything " +
                    "legal, tax, or high-stakes."
            )
        } else {
            Route(
                detectedIntent = bestIntent,
                keywordsMatched = matched.toList(),
                retrieved = retrieved.take(3),
                isRefusal = false,
                note = "Educational software, not financial advice. I cannot see your actual wallets."
            )
        }
    }
}
