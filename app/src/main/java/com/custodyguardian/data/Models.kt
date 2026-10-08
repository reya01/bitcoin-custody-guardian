package com.custodyguardian.data

import kotlinx.serialization.Serializable

/**
 * Corpus JSON entries (assets/corpus/JSON files), e.g. T02-river-seed-phrase.json.
 * Parsed with kotlinx-serialization. Schema follows the curated corpus format
 * documented in the repo (see corpus/T02-river-seed-phrase.json).
 */
@Serializable
data class CorpusEntry(
    val title: String = "",
    val topic_ids: List<String> = emptyList(),
    val level: String = "",
    val risk_class: String = "",
    val plain: String = "",
    val technical: String = "",
    val claims: List<CorpusClaim> = emptyList(),
    val warnings: List<String> = emptyList(),
    val source_title: String = "",
    val source_url: String = "",
    val corpus_version: String = "",
    val reviewed: Boolean = false,
    // item-1 schema upgrade (2.2): verbatim subsets of approved text; rendered verbatim
    val key_points: List<String> = emptyList(),
    val dos: List<String> = emptyList(),
    val donts: List<String> = emptyList(),
    // retrieval-index-only paraphrases; NOT authoritative content
    val user_says: List<String> = emptyList(),
    val user_says_note: String = "",
)

@Serializable
data class CorpusClaim(
    val claim: String = "",
    val source: String = "",
)

/** walkthrough.json — the 7-day inheritance walkthrough, bundled at build time. */
@Serializable
data class Walkthrough(
    val title: String = "",
    val app: String = "",
    val version: String = "",
    val rule_one: String = "",
    val days: List<WalkthroughDay> = emptyList(),
)

@Serializable
data class WalkthroughDay(
    val day: Int = 0,
    val title: String = "",
    val goal: String = "",
    val dos: List<String> = emptyList(),
    val donts: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    val feel_after: String = "",
)
