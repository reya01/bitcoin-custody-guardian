package com.custodyguardian.util

/**
 * VetoRules — the shipped-app port of the prototype's guardrail layer
 * (prototype/guardian_core.py DANGER_PHRASES + scrub). Closes the audit
 * Medium from 2026-10-19: the shipped chat path passed an empty
 * must-not-include list, so the veto could never fire on device.
 *
 * Deterministic only — the LLM never sits in a safety-critical path.
 *
 * Two trigger sets, two jobs (full-repo review 2026-10-29, Kimi-K3 +
 * DeepSeek-V4.1-Flash, GLM-5.3 synthesis):
 *  - DANGER_PHRASES (1:1 prototype parity): trigger the scrub WARNING
 *    injection. Fail-safe direction — an over-broad hit only adds a warning.
 *  - VETO_PHRASES / VETO_PATTERNS (curated): trigger the veto (discard the
 *    whole free-form candidate). Patterns use regex so "type your seed
 *    PHRASE into the site" can't slip between string variants, and
 *    on-device restore guidance ("enter your seed on your hardware wallet")
 *    still never matches — only website/online objects do.
 *
 * A veto trigger is a violation only when its clause is affirmative:
 *  - no negation token within the phrase's clause (negation windows reset at
 *    clause boundaries , ; : — –) and within 5 tokens before the phrase
 *    (word-boundary matched, so "another" never matches "not"), and
 *  - no reported-speech marker before the phrase in the clause ("asks you
 *    to", "telling you to", …) — scam-check output legitimately DESCRIBES
 *    scammer asks, and
 *  - no scam-descriptor in the clause ("remote support is a scam" is a
 *    warning, not advice).
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
     * Every entry must survive this test: could it legitimately appear in
     * reviewed warning text? (Warnings carry negations/scam-descriptors,
     * which suppress.)
     */
    val VETO_PHRASES = listOf(
        "reset the device",
        "reset device",
        "wipe the device",
        "factory reset",
        "restore without checking",
        "share your seed",
        "share your phrase",
        "share your mnemonic",
        "share your recovery phrase",
        "verify by phone",
        "remote support",
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
        "wipe your phone",
        "reset your phone",
    )

    /**
     * Regex triggers for the seed-entry family: optional middle noun
     * ("seed phrase" / "seed words" / bare "seed") + website/online objects
     * ONLY — on-device/hardware-wallet restore is legitimate guidance and
     * must never match (the earlier "enter your seed on" variant was too
     * broad and vetoed legitimate restore guidance).
     */
    val VETO_PATTERNS = listOf(
        Regex(
            "(type|enter)\\s+your\\s+seed\\s+(phrase\\s+|words\\s+)?(into|on)\\s+" +
                "(a\\s+|the\\s+|their\\s+|any\\s+|this\\s+|that\\s+)?(website|site|web\\s*page|webpage|form|browser|link)",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            "(type|enter)\\s+your\\s+seed\\s+(phrase\\s+|words\\s+)?online",
            RegexOption.IGNORE_CASE,
        ),
    )

    /** 1:1 port of the prototype SEED_TOPIC_TERMS (scrub gate parity). */
    val SEED_TOPIC_TERMS = listOf(
        "seed", "mnemonic", "recovery phrase", "wordlist",
        "backup", "bip-39", "bip39", "bip 39", "words",
    )

    private val NEGATION_RE = Regex(
        "(?<!\\p{L})(never|no|not|cannot|can't|cant|won't|wont|shouldn't|shouldnt|" +
            "wouldn't|wouldnt|couldn't|couldnt|don't|dont|do\\s+not|must\\s+not)(?!\\p{L})",
    )
    private val REPORTED_SPEECH_RE = Regex(
        "(?<!\\p{L})(as?ks?|asking|asked|tells?|telling|told|wants?|wanting|wished?|" +
            "instructs?|instructing|instructed)\\s+" +
            "(you|him|her|them|us|me|anyone)\\s+to\\b",
    )
    private val SCAM_DESCRIPTOR_RE = Regex(
        "\\b(is|are|it's|it\\s+is|was|were)\\s+(a\\s+|an\\s+)?(scam|fraud|phishing)\\b",
        RegexOption.IGNORE_CASE,
    )

    private val NEGATION_WINDOW_TOKENS = 5
    private val ZERO_WIDTH = Regex("[\\u200b-\u200f\u2060\ufeff]")

    val MANDATORY_WARNING =
        "WARNING: Never reset or wipe a device that still holds your Bitcoin " +
            "until your seed phrase backup has been fully verified. Never share " +
            "your seed phrase with anyone — no real support agent will ever ask " +
            "for it. If someone instructs you to do either, it is a scam. Stop " +
            "and verify your backup offline first."

    /**
     * Lowercase, curly apostrophes to straight, strip zero-width/format
     * characters, collapse all whitespace (so "share your\nseed" can't
     * bypass matching by splitting across a segment boundary).
     */
    fun normalize(text: String): String =
        ZERO_WIDTH.replace(text, "")
            .replace('\u2019', '\'')
            .replace('\u2018', '\'')
            .lowercase()
            .split(Regex("\\s+"))
            .joinToString(" ")
            .trim()

    /** True when the text talks about seed/backup topics (scrub gate). */
    fun isSeedRelated(text: String): Boolean {
        val t = normalize(text)
        return SEED_TOPIC_TERMS.any { it in t }
    }

    /**
     * Veto triggers appearing affirmatively. [extraPhrases] are caller-supplied
     * must-not-include constraints, judged by the SAME negation-aware rule
     * (a constraint like "share your seed" must not veto the warning
     * "Never share your seed"). Blank constraints are skipped (a blank
     * matches at index 0 and would veto everything).
     */
    fun findViolations(
        text: String,
        extraPhrases: List<String> = emptyList(),
    ): List<String> {
        val t = normalize(text)
        if (t.isEmpty()) return emptyList()
        val violations = mutableListOf<String>()
        // newline first so bullets/new lines start their own segment
        val segments = t.replace("\n", ". ").split(Regex("(?<=[.!?])\\s+"))
        for (rawSegment in segments) {
            val segment = rawSegment.replace(Regex("^\\s*[-*\u2022]\\s*"), "").trim()
            if (segment.isEmpty()) continue
            // clauses: negation windows reset at , ; : — –
            val clauses = segment.split(Regex("[,;:\u2014\u2013]+"))
            for (pattern in VETO_PATTERNS) {
                for (clause in clauses) {
                    val m = pattern.find(clause) ?: continue
                    if (!suppressedIn(clause, m.range.first)) {
                        if (pattern.pattern !in violations) violations.add(pattern.pattern)
                    }
                }
            }
            for (phrase in VETO_PHRASES + extraPhrases) {
                val p = normalize(phrase)
                if (p.isEmpty()) continue
                var idx = segment.indexOf(p)
                while (idx >= 0) {
                    // find the clause containing idx
                    var clauseStart = 0
                    for (clause in clauses) {
                        val end = clauseStart + clause.length
                        if (idx < end) {
                            if (!suppressedIn(clause, idx - clauseStart)) {
                                if (phrase !in violations) violations.add(phrase)
                            }
                            break
                        }
                        clauseStart = end + 1
                    }
                    idx = segment.indexOf(p, idx + 1)
                }
            }
        }
        return violations
    }

    /** Negation / reported-speech / scam-descriptor suppression within the clause. */
    private fun suppressedIn(clause: String, idx: Int): Boolean {
        val before = clause.substring(0, idx.coerceIn(0, clause.length))
        val tokens = before.split(" ").filter { it.isNotBlank() }
        // the 5 tokens immediately before the phrase = distances 1..5
        val window = tokens.takeLast(NEGATION_WINDOW_TOKENS).joinToString(" ")
        if (NEGATION_RE.containsMatchIn(window)) return true
        // reported speech anywhere before the phrase in the clause
        if (REPORTED_SPEECH_RE.containsMatchIn(before)) return true
        // scam-descriptor anywhere in the clause: warning context, not advice
        if (SCAM_DESCRIPTOR_RE.containsMatchIn(clause)) return true
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
