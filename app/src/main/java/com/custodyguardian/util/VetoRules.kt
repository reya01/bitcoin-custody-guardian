package com.custodyguardian.util

/**
 * VetoRules — the shipped-app port of the prototype's guardrail layer
 * (prototype/guardian_core.py DANGER_PHRASES + scrub). Closes the audit
 * Medium from 2026-10-19: the shipped chat path passed an empty
 * must-not-include list, so the veto could never fire on device.
 *
 * Deterministic only — the LLM never sits in a safety-critical path.
 *
 * Two phrase lists, two jobs (design review 2026-10-29, GLM-5.3):
 *  - DANGER_PHRASES (1:1 prototype parity): trigger the scrub WARNING
 *    injection. Fail-safe direction — an over-broad hit only adds a warning.
 *  - VETO_PHRASES (curated): trigger the veto (discard the whole free-form
 *    candidate). These are REWORDED variants of the prototype phrases so
 *    legitimate guidance ("enter your seed on your new hardware wallet")
 *    never vetoes — only website/online variants of the same danger do.
 *
 * A veto phrase is a violation only when its segment is affirmative:
 *  - no negation token within 5 tokens before the phrase (word-boundary
 *    matched, so "another" never matches "not"), and
 *  - no reported-speech marker ("asks you to", "tells you to", …) in the
 *    same window — scam-check output legitimately describes scammer asks.
 * Approved corpus text is NEVER vetoed (scope: free-form surfaces only).
 */
object VetoRules {

    /** 1:1 port of the prototype DANGER_PHRASES — keep the lists in sync. */
    val DANGER_PHRASES = listOf(
        "reset the device",
        "reset device",
        "wipe the device",
        "factory reset",
        "restore without checking",
        "type your seed into",
        "enter your seed on",
        "share your seed",
        "share your phrase",
        "verify by phone",
        "remote support",
    )

    /**
     * Veto triggers: affirmative constructions that are dangerous advice.
     * Every phrase must survive this test: could it legitimately appear in
     * reviewed warning text? (Warnings carry negations, which suppress.)
     */
    val VETO_PHRASES = listOf(
        // prototype phrases, safe to veto affirmatively
        "reset the device",
        "reset device",
        "wipe the device",
        "factory reset",
        "restore without checking",
        "share your seed",
        "share your phrase",
        "verify by phone",
        "remote support",
        // website/online variants only — on-device restore is legitimate
        "type your seed into a",
        "type your seed into the",
        "type your seed into their",
        "type your seed into any",
        "enter your seed on a",
        "enter your seed on the",
        "enter your seed on their",
        "enter your seed online",
        "enter your seed into a",
        "enter your seed into the",
        "enter your seed into their",
        // affirmative-only constructions (a warning would carry a negation)
        "it is safe to share",
        "you can share your seed",
        "read your seed phrase to",
        "read your seed phrase aloud",
        "read out your seed",
        "say your seed phrase",
        "tell them your seed",
        "take a photo of your seed",
        "photograph your seed",
        "screenshot your seed",
        "store your seed in the cloud",
        "email your seed",
        "send your seed",
        "paste your seed",
        "upload your seed",
        "dictate your seed",
        "type your mnemonic",
        "enter your mnemonic",
        "type your recovery phrase",
        "enter your recovery phrase",
        "share your mnemonic",
        "share your recovery phrase",
        "wipe your phone",
        "reset your phone",
    )

    /** 1:1 port of the prototype SEED_TOPIC_TERMS (scrub gate parity). */
    val SEED_TOPIC_TERMS = listOf(
        "seed", "mnemonic", "recovery phrase", "wordlist",
        "backup", "bip-39", "bip39", "bip 39", "words",
    )

    private val NEGATION_RE = Regex(
        "(?<!\\p{L})(never|no|not|cannot|can't|cant|won't|wont|shouldn't|shouldnt|" +
            "wouldn't|wouldnt|couldn't|couldnt|don't|dont|avoid|stop|refuse)(?!\\p{L})",
    )
    private val REPORTED_SPEECH_RE = Regex(
        "(?<!\\p{L})(as?ks?|asked|tells?|told|wants?|wished?|instructs?|instructed)\\s+" +
            "(you|him|her|them|us|me)\\s+to\\b",
    )

    private val NEGATION_WINDOW_TOKENS = 5

    val MANDATORY_WARNING =
        "WARNING: Never reset or wipe a device that still holds your Bitcoin " +
            "until your seed phrase backup has been fully verified. Never share " +
            "your seed phrase with anyone — no real support agent will ever ask " +
            "for it. If someone instructs you to do either, it is a scam. Stop " +
            "and verify your backup offline first."

    /** Lowercase, curly apostrophes to straight, trim. */
    fun normalize(text: String): String =
        text.lowercase().replace('\u2019', '\'').replace('\u2018', '\'').trim()

    /** True when the text talks about seed/backup topics (scrub gate). */
    fun isSeedRelated(text: String): Boolean {
        val t = normalize(text)
        return SEED_TOPIC_TERMS.any { it in t }
    }

    /**
     * Veto phrases appearing affirmatively: no negation and no reported-speech
     * marker within [NEGATION_WINDOW_TOKENS] tokens before the phrase, inside
     * the phrase's segment (segments split on sentence punctuation, newlines
     * and semicolons — bullets are their own segment). [extraPhrases] are
     * caller-supplied must-not-include constraints, judged by the SAME
     * negation-aware rule (a constraint like "share your seed" must not veto
     * the warning "Never share your seed").
     */
    fun findViolations(
        text: String,
        extraPhrases: List<String> = emptyList(),
    ): List<String> {
        val t = normalize(text)
        if (t.isBlank()) return emptyList()
        val violations = mutableListOf<String>()
        // newline first so bullets/new lines start their own segment
        val segments = t.replace("\n", ". ").split(Regex("(?<=[.!?;:])\\s*"))
        for (rawSegment in segments) {
            val segment = rawSegment.replace(Regex("^\\s*[-*\u2022]\\s*"), "").trim()
            if (segment.isEmpty()) continue
            for (phrase in VETO_PHRASES + extraPhrases) {
                val p = normalize(phrase)
                var idx = segment.indexOf(p)
                while (idx >= 0) {
                    if (!suppressedBefore(segment, idx)) {
                        if (phrase !in violations) violations.add(phrase)
                        break
                    }
                    idx = segment.indexOf(p, idx + 1)
                }
            }
        }
        return violations
    }

    /** Negation or reported-speech marker within the token window before idx. */
    private fun suppressedBefore(segment: String, idx: Int): Boolean {
        val before = segment.substring(0, idx)
        val tokens = before.split(Regex("\\s+")).filter { it.isNotBlank() }
        // the 5 tokens immediately before the phrase = distances 1..5
        val window = tokens.takeLast(NEGATION_WINDOW_TOKENS).joinToString(" ")
        if (NEGATION_RE.containsMatchIn(window)) return true
        if (REPORTED_SPEECH_RE.containsMatchIn(window)) return true
        return false
    }

    /**
     * Guardrail scrub (prototype behavior): if the answer contains a danger
     * phrase and is seed-related, inject the mandatory warning after the
     * first offending sentence. Returns the (possibly modified) text and
     * whether an injection happened. Call AFTER the veto — an injected
     * warning template must never be able to trip the veto.
     */
    fun scrub(answer: String): Pair<String, Boolean> {
        if (answer.isBlank()) return answer to false
        val danger = DANGER_PHRASES.filter { it in normalize(answer) }
        if (danger.isEmpty() || !isSeedRelated(answer)) return answer to false
        val sentences = answer.split(Regex("(?<=[.!?])\\s+"))
        val out = StringBuilder()
        var placed = false
        for (s in sentences) {
            out.append(s)
            out.append(' ')
            if (!placed && danger.any { normalize(s).contains(it) }) {
                out.append(MANDATORY_WARNING)
                out.append(' ')
                placed = true
            }
        }
        if (!placed) {
            out.append(MANDATORY_WARNING)
        }
        return out.toString().trim() to true
    }
}
