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
    // NOTE (2026-10-29 adjudication of the DS review finding "add avoid|stop|
    // refuse back"): DELIBERATELY NOT added — "stop and share your seed with
    // us" must keep vetoing (fail-open beats false-positive vetoes, which
    // merely fall back to the approved corpus render). Round-5 decision.
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
    // F-g: neutralize ALL format characters (Cf): U+00AD soft hyphen,
    // U+200B-F, U+2060-64, U+2066-9, U+FEFF — REPLACED WITH A SPACE, not
    // deleted: deletion glues "share your\u00ADseed" into "yourseed" which
    // still bypasses matching; a space keeps it matchable. U+2028/U+2029/
    // U+0085 are line/segment separators (LINE_BREAK), not Cf.
    private val ZERO_WIDTH = Regex("\\p{Cf}")
    private val LINE_BREAK = Regex("[\\n\\u2028\\u2029\\u0085]+")
    // F-h: sentence split fires even with ZERO spaces after the terminator
    // ("Never do that.Share your seed" = two segments).
    private val SENTENCE_SPLIT = Regex("(?<=[.!?;:])\\s*")
    private val BULLET = Regex("^\\s*[-*\u2022]\\s*")
    private val CLAUSE_SPLIT = Regex("[,;:\\u2014\\u2013]+")
    // F-c: coordinating conjunctions also split clauses — a negation before
    // "and|but|so" must not protect an affirmative after it (fail-safe).
    private val CONJUNCTION_RE = Regex("\\b(?:and|but|so)\\b")

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
    fun normalize(text: String): String = normalizeLine(LINE_BREAK.replace(text, " "))

    private fun normalizeLine(line: String): String =
        ZERO_WIDTH.replace(line, " ")
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
        // F-a redesign: phrase MATCHING runs on the fully-collapsed text t
        // (so a phrase split across a newline still matches), but NEGATION
        // suppression is scoped to the SOURCE LINE containing the match
        // start — a negation on line 1 must never rescue a trigger on line 2.
        val lineTexts = LINE_BREAK.split(text).map { normalizeLine(it) }
        val t = StringBuilder()
        val lineStarts = mutableListOf<Int>()
        val lineClauses = mutableListOf<List<Clause>>()
        for (line in lineTexts) {
            if (line.isEmpty()) continue
            if (t.isNotEmpty()) t.append(' ')
            lineStarts.add(t.length)
            t.append(line)
            lineClauses.add(clausesOf(line))
        }
        if (t.isEmpty()) return emptyList()
        val violations = mutableListOf<String>()

        // Suppression for a match at collapsed-text position idx: resolve the
        // source line of the match START, then the clause of THAT line
        // containing the within-line position. A match that starts in line i
        // and extends into i+1 uses only line i's clauses.
        fun suppressedAt(idx: Int): Boolean {
            var li = lineStarts.size - 1
            while (li > 0 && lineStarts[li] > idx) li--
            val within = idx - lineStarts[li]
            val clauses = lineClauses[li]
            if (clauses.isEmpty()) return false
            var ci = clauses.size - 1
            while (ci > 0 && clauses[ci].start > within) ci--
            val clause = clauses[ci]
            return suppressedIn(clause.text, within - clause.start)
        }

        for (pattern in VETO_PATTERNS) {
            // F-d: check ALL matches — a suppressed first hit must not hide a
            // later affirmative one.
            for (m in pattern.findAll(t)) {
                if (!suppressedAt(m.range.first) && pattern.pattern !in violations) {
                    violations.add(pattern.pattern)
                }
            }
        }
        for (phrase in VETO_PHRASES + extraPhrases) {
            val p = normalize(phrase)
            if (p.isEmpty()) continue
            var idx = t.indexOf(p)
            while (idx >= 0) {
                if (!suppressedAt(idx) && phrase !in violations) violations.add(phrase)
                idx = t.indexOf(p, idx + 1)
            }
        }
        return violations
    }

    /** A clause with its start offset within the normalized source line. */
    private class Clause(val start: Int, val text: String)

    /**
     * Segments (sentence enders, zero-or-more spaces after — F-h) and clauses
     * (negation windows reset at , ; : — – AND at and|but|so — F-c) of one
     * normalized line, with offsets relative to the line start so callers can
     * map a within-line match index to the correct in-clause index (F-f).
     */
    private fun clausesOf(line: String): List<Clause> {
        val clauses = mutableListOf<Clause>()
        for ((segStart, raw) in splitKeepOffsets(line, SENTENCE_SPLIT)) {
            val noBullet = BULLET.replace(raw, "")
            if (noBullet.isBlank()) continue
            val stripped = noBullet.trim()
            val lead = raw.length - noBullet.length +
                (noBullet.length - noBullet.trimStart().length)
            val cStart = segStart + lead
            for ((clStart, clause) in splitKeepOffsets(stripped, CLAUSE_SPLIT)) {
                for ((subStart, sub) in splitKeepOffsets(clause, CONJUNCTION_RE)) {
                    if (sub.isNotBlank()) clauses.add(Clause(cStart + clStart + subStart, sub))
                }
            }
        }
        return clauses
    }

    /** Split [s] on [re], keeping each part's start offset. */
    private fun splitKeepOffsets(s: String, re: Regex): List<Pair<Int, String>> {
        val out = mutableListOf<Pair<Int, String>>()
        var last = 0
        for (m in re.findAll(s)) {
            if (m.range.first >= last) {
                out.add(last to s.substring(last, m.range.first))
                last = m.range.last + 1
            }
        }
        out.add(last to s.substring(last))
        return out
    }

    /** Negation / reported-speech / scam-descriptor suppression within the clause. */
    private fun suppressedIn(clause: String, idx: Int): Boolean {
        val before = clause.substring(0, idx.coerceIn(0, clause.length))
        val tokens = before.split(" ").filter { it.isNotBlank() }
        // the 5 tokens immediately before the phrase = distances 1..5
        val window = tokens.takeLast(NEGATION_WINDOW_TOKENS).joinToString(" ")
        if (NEGATION_RE.containsMatchIn(window)) return true
        // F-b: reported speech restricted to the SAME 5-token window used for
        // negation — a distant "asks you to" no longer suppresses a later hit.
        if (REPORTED_SPEECH_RE.containsMatchIn(window)) return true
        // F-c: scam-descriptor suppresses only when it occurs AFTER the
        // trigger in the clause — "remote support is a scam" warns (trigger
        // first), while "it is a scam and you should share your seed" puts
        // the trigger AFTER the descriptor (advice riding a warning; the
        // conjunction split separates them, so this vetoes).
        if (SCAM_DESCRIPTOR_RE.findAll(clause).any { it.range.first > idx }) return true
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
