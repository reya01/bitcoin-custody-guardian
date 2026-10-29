package com.custodyguardian

import com.custodyguardian.data.CorpusEntry
import com.custodyguardian.util.VetoRules
import java.security.MessageDigest

/**
 * Extractive answers v2, ported from prototype/extractive.py (plan item 2.1,
 * post-review hardening). Philosophy: the model SELECTS, the corpus speaks.
 * The only free-form surface is a short bridge, deterministically validated;
 * everything else is verbatim approved text. Fails closed — extractive mode
 * never falls through to a free-form paraphrase.
 *
 * Deterministic guardrail rule (spec §8): the model only picks menu IDs;
 * the rendered text is the human-reviewed corpus verbatim, re-checked against
 * the entry it claims to come from AND the bundled key_points sha256 manifest.
 *
 * On-device adaptations vs the prototype:
 *  - the menu is built directly from the router's Retrieved items (id + entry),
 *    so no 40-char id-prefix resolution is needed;
 *  - the verbatim re-check is containment in the entry's own approved
 *    plain+technical text (the corpus discipline: key_points are verbatim
 *    subsets of approved text);
 *  - the menu is additionally capped at MAX_MENU_POINTS (the prototype capped
 *    entries only; the 1.7B on-device context is 3072 tokens).
 */
object ExtractiveAnswer {

    /** Fallback when selection fails: canned, fail-closed, never free-form. */
    const val CANNED_UNAVAILABLE =
        "The answer engine did not respond, so I cannot compose an answer right now. " +
            "Nothing was sent anywhere. General safety rule while you wait: never share, " +
            "type, or photograph seed words or private keys for anyone, ever — no " +
            "legitimate party will ask for them."

    const val MAX_MENU_ENTRIES = 10
    const val MAX_MENU_POINTS = 30
    const val MAX_SELECTED = 8

    private val BRIDGE_BLOCKLIST = Regex(
        "(safe to share|no risk|you can ignore|don't worry|it is fine|perfectly safe)",
        RegexOption.IGNORE_CASE,
    )
    private val WARNING_WORD = Regex("\\b(never|do not|don't|must not|only)\\b", RegexOption.IGNORE_CASE)
    private val URL_RE = Regex("https?://|www\\.|\\w+\\.(com|org|net|io|ca)\\b", RegexOption.IGNORE_CASE)

    fun norm(s: String): String = s.split(Regex("\\s+")).joinToString(" ").trim().lowercase()

    fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(norm(s).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    data class Point(
        val pid: String,
        val text: String,
        val entryId: String,
        val title: String,
        val isWarning: Boolean,
    )

    data class Selection(
        val text: String,
        val backend: String,
        val selected: List<String> = emptyList(),
        val pinned: List<String> = emptyList(),
    )

    /**
     * Menu of key points from the retrieved entries, verbatim, deduped by
     * normalized text. Cap: MAX_MENU_ENTRIES distinct entries (retrieval
     * order), then MAX_MENU_POINTS points overall.
     */
    fun buildMenu(retrieved: List<Pair<String, CorpusEntry>>): List<Point> {
        val menu = mutableListOf<Point>()
        val seenTexts = mutableSetOf<String>()
        val seenEntries = mutableSetOf<String>()
        outer@ for ((eid, entry) in retrieved) {
            if (eid in seenEntries) continue
            if (seenEntries.size >= MAX_MENU_ENTRIES) break
            seenEntries.add(eid)
            for (kp in entry.key_points) {
                val n = norm(kp)
                if (n.isEmpty() || n in seenTexts) continue
                seenTexts.add(n)
                menu.add(
                    Point("P${menu.size + 1}", kp, eid, entry.title, WARNING_WORD.containsMatchIn(kp)),
                )
                if (menu.size >= MAX_MENU_POINTS) break@outer
            }
        }
        return menu
    }

    fun buildSelectionPrompt(question: String, menu: List<Point>): String {
        val lines = menu.map { "${it.pid}. [${it.title}] ${it.text}" }
        return """
            |You are the selection step of an offline Bitcoin self-custody assistant.
            |A question from a grieving beginner is shown below, followed by numbered key
            |points drawn verbatim from a human-reviewed corpus.
            |
            |TASK: select ALL key points that help answer the question — including EVERY key
            |point carrying an applicable warning (never / do not / must not / only). Select
            |between 2 and $MAX_SELECTED. Do not write the answer, do not add information. Keep menu order.
            |
            |QUESTION: $question
            |
            |KEY POINTS:
            |${lines.joinToString("\n")}
            |
            |Return ONLY compact JSON:
            |{"selected": ["P1", "P3", "P4", "P7"], "bridge": "<=2 short plain sentences framing the selection. No new facts, no new numbers, no URLs, no product names; empty string if nothing to add."}
        """.trimMargin()
    }

    /** Parses {"selected": [...], "bridge": "..."}; strips Qwen3 think blocks and fences. */
    fun parseSelection(raw: String, menu: List<Point>): Pair<List<String>, String>? {
        var t = raw
        val open = "<think>"
        val close = "</think>"
        val start = t.indexOf(open)
        if (start >= 0) {
            val end = t.indexOf(close, start)
            t = if (end >= 0) t.removeRange(start, end + close.length) else t.substring(0, start)
        }
        t = t.trim()
        if (t.startsWith("```")) {
            t = t.removePrefix("```")
            if ("\n" in t) t = t.substringAfter("\n")
            t = t.substringBefore("```").trim()
        }
        val m = Regex("\\{.*\\}", RegexOption.DOT_MATCHES_ALL).find(t) ?: return null
        val obj = parseJson(m.value) ?: return null
        val valid = menu.map { it.pid }.toSet()
        val sel = mutableListOf<String>()
        for (s in obj.selected) {
            val p = s.trim().uppercase()
            if (p in valid && p !in sel) sel.add(p)
        }
        if (sel.isEmpty()) return null
        return sel to obj.bridge.trim()
    }

    private fun parseJson(json: String): ParsedSelection? = try {
        val sel = Regex("\"selected\"\\s*:\\s*\\[(.*?)\\]", RegexOption.DOT_MATCHES_ALL)
            .find(json)?.groupValues?.get(1)
            ?.split(",")?.map { it.trim().trim('"') }?.filter { it.isNotEmpty() }
            ?: emptyList()
        val bridge = Regex("\"bridge\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
            .find(json)?.groupValues?.get(1)?.replace("\\\"", "\"") ?: ""
        ParsedSelection(sel, bridge)
    } catch (e: Exception) {
        null
    }

    private data class ParsedSelection(val selected: List<String>, val bridge: String)

    /** Deterministic bridge checks. Returns the bridge or "" (drop, never re-draft). */
    fun validateBridge(bridge: String, selItems: List<Point>): String {
        if (bridge.isBlank()) return ""
        if (bridge.length > 300 || Regex("[.!?]").findAll(bridge).count() > 2) return ""
        if (URL_RE.containsMatchIn(bridge)) return ""
        val digitsInPoints = Regex("\\d+").findAll(selItems.joinToString(" ") { it.text })
            .map { it.value }.toSet()
        for (d in Regex("\\d+").findAll(bridge)) {
            if (d.value !in digitsInPoints) return ""
        }
        if (BRIDGE_BLOCKLIST.containsMatchIn(bridge)) return ""
        // Veto layer applies to every free-form surface (audit Medium
        // 2026-10-19): affirmative danger advice in the bridge drops the
        // bridge; the verbatim points still render (no dead ends).
        if (VetoRules.findViolations(bridge).isNotEmpty()) return ""
        return bridge
    }

    /**
     * Render verbatim, menu order, with deterministic warning pinning and the
     * manifest integrity gate on the FINAL set (pinning cannot bypass it).
     */
    fun render(
        sel: List<String>,
        bridge: String,
        menu: List<Point>,
        manifest: Set<String>,
    ): Pair<String, List<String>> {
        val byPid = menu.associateBy { it.pid }
        val ordered = sel.mapNotNull { byPid[it] }.toMutableList()
        val pinned = mutableListOf<String>()
        val selEntries = ordered.map { it.entryId }.toSet()
        for (p in menu) {
            if (p.isWarning && p.entryId in selEntries && p.pid !in sel) {
                ordered.add(p)
                pinned.add(p.pid)
            }
        }
        // Integrity gate is FAIL-CLOSED (full-repo review 2026-10-29): an
        // empty manifest (load error silently returned emptySet) must NOT
        // disable the gate — it must refuse to render. Missing manifest =
        // broken install, never a free pass.
        val ok = ordered.filter { sha256(it.text) in manifest }
        if (ok.isEmpty()) return "" to emptyList()
        val parts = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        for (p in ok) {
            val n = norm(p.text)
            if (n in seen) continue
            seen.add(n)
            parts.add("- ${p.text} [${p.entryId}]")
        }
        var text = parts.joinToString("\n\n")
        if (bridge.isNotBlank()) text += "\n\n$bridge"
        val entries = ok.map { it.entryId }.distinct().sorted()
        text += "\n\nSource: " + entries.joinToString(", ") + " (human-reviewed corpus)"
        return text to pinned
    }

    /** Verbatim re-check: the point must be a subset of the entry's approved text. */
    fun verbatimOk(point: Point, entry: CorpusEntry?): Boolean {
        if (entry == null) return false
        val approved = norm(entry.plain + "\n" + entry.technical)
        return norm(point.text) in approved
    }

    /**
     * Full extractive answer. modelCall = the SAME on-device engine (the
     * offline story stays true). Fail closed: one retry, then the canned
     * message plus the top retrieved passage verbatim (no dead ends) — never
     * a free-form paraphrase.
     */
    fun answer(
        question: String,
        menu: List<Point>,
        corpusById: Map<String, CorpusEntry>,
        manifest: Set<String>,
        modelCall: (system: String, user: String) -> String,
        fallbackPassage: String = "",
    ): Selection {
        return try {
            if (menu.isEmpty()) return failClosed(fallbackPassage)
            val system = "Return only the compact JSON object. No reasoning text."
            val user = buildSelectionPrompt(question, menu)
            var parsed = parseSelection(modelCall(system, user), menu)
            if (parsed == null) parsed = parseSelection(modelCall(system, user), menu) // one retry
            if (parsed == null) return failClosed(fallbackPassage)
            val (sel, bridgeRaw) = parsed
            val byPid = menu.associateBy { it.pid }
            for (s in sel) {
                val p = byPid.getValue(s)
                if (!verbatimOk(p, corpusById[p.entryId])) return failClosed(fallbackPassage)
            }
            val bridge = validateBridge(bridgeRaw, sel.map { byPid.getValue(it) })
            val (text, pinned) = render(sel, bridge, menu, manifest)
            if (text.isEmpty()) return failClosed(fallbackPassage)
            Selection(text = text, backend = "extractive_select", selected = sel, pinned = pinned)
        } catch (e: Exception) {
            failClosed(fallbackPassage)
        }
    }

    private fun failClosed(passage: String) = Selection(
        text = buildString {
            append(CANNED_UNAVAILABLE)
            if (passage.isNotBlank()) {
                append("\n\nFrom the reviewed corpus:\n")
                append(passage)
            }
        },
        backend = "extractive_fallback",
    )
}
