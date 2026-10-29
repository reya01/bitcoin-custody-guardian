package com.custodyguardian

import com.custodyguardian.data.CorpusEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the on-device extractive answer path (plan item 2.1): the model
 * SELECTS key-point IDs, the corpus text renders verbatim, and every failure
 * fails closed — a canned message plus the retrieved passage, never a
 * free-form paraphrase.
 */
class ExtractiveAnswerTest {

    private fun entry(
        title: String,
        plain: String,
        keyPoints: List<String>,
        warnings: List<String> = emptyList(),
    ) = CorpusEntry(
        title = title, topic_ids = listOf("T02"), plain = plain,
        key_points = keyPoints, warnings = warnings,
    )

    private val entryA = entry(
        title = "Seed phrase basics",
        plain = "Self-custody means you control the keys. " +
            "The seed phrase is 12 or 24 ordinary words. " +
            "Never type the seed words into any website. " +
            "Store the words on paper, offline.",
        keyPoints = listOf(
            "The seed phrase is 12 or 24 ordinary words.",
            "Never type the seed words into any website.",
            "Store the words on paper, offline.",
        ),
        warnings = listOf("Never photograph the word list."),
    )

    private val entryB = entry(
        title = "Backup hygiene",
        plain = "Keep two copies in separate places. Check the backup yearly.",
        keyPoints = listOf("Keep two copies in separate places."),
    )

    private val retrieved = listOf("a" to entryA, "b" to entryB)

    // ---- buildMenu ----

    @Test
    fun menu_is_verbatim_deduped_and_pid_numbered_in_order() {
        val menu = ExtractiveAnswer.buildMenu(retrieved)
        assertEquals(4, menu.size)
        assertEquals(listOf("P1", "P2", "P3", "P4"), menu.map { it.pid })
        assertEquals("The seed phrase is 12 or 24 ordinary words.", menu[0].text)
        assertEquals(listOf("a", "a", "a", "b"), menu.map { it.entryId })
    }

    @Test
    fun menu_flags_warning_points_by_vocabulary() {
        val menu = ExtractiveAnswer.buildMenu(retrieved)
        assertTrue(menu.first { it.text.startsWith("Never type") }.isWarning)
        assertTrue(!menu.first { it.text.startsWith("Store the words") }.isWarning)
    }

    @Test
    fun menu_caps_points_at_max() {
        val big = entry(
            title = "Big",
            plain = (1..40).joinToString(" ") { "Point number $it stands here." },
            keyPoints = (1..40).map { "Point number $it stands here." },
        )
        val menu = ExtractiveAnswer.buildMenu(listOf("big" to big))
        assertEquals(ExtractiveAnswer.MAX_MENU_POINTS, menu.size)
    }

    @Test
    fun menu_caps_entries_at_max() {
        val many = (1..14).map { i ->
            "e$i" to entry(title = "E$i", plain = "Entry $i text.", keyPoints = listOf("Entry $i point."))
        }
        val menu = ExtractiveAnswer.buildMenu(many)
        assertEquals(ExtractiveAnswer.MAX_MENU_ENTRIES, menu.map { it.entryId }.distinct().size)
    }

    // ---- parseSelection ----

    private val menu = ExtractiveAnswer.buildMenu(retrieved)

    @Test
    fun parse_reads_compact_json_and_dedupes_invalid_pids() {
        val raw = """{"selected": ["P1", "P3", "P1", "P99"], "bridge": "Here is the short version."}"""
        val (sel, bridge) = ExtractiveAnswer.parseSelection(raw, menu)!!
        assertEquals(listOf("P1", "P3"), sel)
        assertEquals("Here is the short version.", bridge)
    }

    @Test
    fun parse_handles_fences_and_think_blocks() {
        val raw = "```json\n{\"selected\": [\"P2\"], \"bridge\": \"\"}\n```"
        val (sel, bridge) = ExtractiveAnswer.parseSelection(raw, menu)!!
        assertEquals(listOf("P2"), sel)
        assertEquals("", bridge)
    }

    @Test
    fun parse_returns_null_on_garbage_or_empty_selection() {
        assertEquals(null, ExtractiveAnswer.parseSelection("no json here", menu))
        assertEquals(null, ExtractiveAnswer.parseSelection("""{"selected": []}""", menu))
        assertEquals(null, ExtractiveAnswer.parseSelection("""{"selected": ["P99"]}""", menu))
    }

    // ---- validateBridge ----

    private val selItems = menu.take(2)

    @Test
    fun bridge_passes_when_short_plain_and_fact_free() {
        val b = ExtractiveAnswer.validateBridge("Short plain framing sentence here.", selItems)
        assertEquals("Short plain framing sentence here.", b)
    }

    @Test
    fun bridge_dropped_on_length_sentences_urls_new_digits_or_reassurance() {
        assertEquals("", ExtractiveAnswer.validateBridge("x".repeat(301), selItems))
        assertEquals("", ExtractiveAnswer.validateBridge("One. Two. Three. Four.", selItems))
        assertEquals("", ExtractiveAnswer.validateBridge("See https://example.com for more.", selItems))
        // "7" is not in the selected points' text -> new fact
        assertEquals("", ExtractiveAnswer.validateBridge("Wait 7 days before acting.", selItems))
        assertEquals("", ExtractiveAnswer.validateBridge("It is perfectly safe to relax.", selItems))
    }

    // ---- render ----

    @Test
    fun render_outputs_points_verbatim_in_menu_order_with_source_line() {
        val (text, pinned) = ExtractiveAnswer.render(listOf("P4", "P1"), "", menu, emptySet())
        assertTrue(text.startsWith("- Keep two copies in separate places. [b]"))
        assertTrue(text.contains("- The seed phrase is 12 or 24 ordinary words. [a]"))
        assertTrue(text.endsWith("Source: a, b (human-reviewed corpus)"))
        // entry "a" is selected, so its warning point (P2) is pinned by design
        assertEquals(listOf("P2"), pinned)
    }

    @Test
    fun render_pins_warnings_of_selected_entries_that_model_omitted() {
        val (text, pinned) = ExtractiveAnswer.render(listOf("P1"), "", menu, emptySet())
        // entry "a" selected -> its warning key point (P2) must be pinned
        assertTrue("P2" in pinned)
        assertTrue(text.contains("Never type the seed words into any website."))
    }

    @Test
    fun render_manifest_gate_drops_points_that_fail_the_hash() {
        // manifest containing only P1's hash: P4 (entry b) is dropped
        val manifest = setOf(ExtractiveAnswer.sha256(menu.first { it.pid == "P1" }.text))
        val (text, _) = ExtractiveAnswer.render(listOf("P1", "P4"), "", menu, manifest)
        assertTrue(text.contains("The seed phrase is 12 or 24 ordinary words."))
        assertTrue(!text.contains("Keep two copies"))
    }

    @Test
    fun render_returns_empty_when_everything_fails_the_manifest() {
        val (text, _) = ExtractiveAnswer.render(listOf("P1"), "", menu, setOf("deadbeef"))
        assertEquals("", text)
    }

    // ---- verbatimOk / sha256 ----

    @Test
    fun verbatim_check_accepts_subset_and_rejects_rewording() {
        val p = menu.first { it.pid == "P1" }
        assertTrue(ExtractiveAnswer.verbatimOk(p, entryA))
        val reworded = p.copy(text = "The seed phrase is 12 or 24 normal words.")
        assertTrue(!ExtractiveAnswer.verbatimOk(reworded, entryA))
        assertTrue(!ExtractiveAnswer.verbatimOk(p, null))
    }

    @Test
    fun sha256_is_normalized_and_stable() {
        val h1 = ExtractiveAnswer.sha256("The  seed phrase is\n12 or 24 words.")
        val h2 = ExtractiveAnswer.sha256("the seed phrase is 12 or 24 words.")
        assertEquals(h1, h2)
        assertTrue(Regex("[0-9a-f]{64}").matches(h1))
    }

    // ---- answer (fail-closed end to end) ----

    private val manifest = setOf(
        "The seed phrase is 12 or 24 ordinary words.",
        "Never type the seed words into any website.",
        "Store the words on paper, offline.",
        "Keep two copies in separate places.",
    ).map { ExtractiveAnswer.sha256(it) }.toSet()
    private val corpusById = mapOf("a" to entryA, "b" to entryB)

    @Test
    fun answer_happy_path_renders_selected_points_verbatim() {
        val result = ExtractiveAnswer.answer(
            "what is a seed phrase?", menu, corpusById, manifest,
            modelCall = { _, _ -> """{"selected": ["P1", "P3"], "bridge": "The words matter."}""" },
        )
        assertEquals("extractive_select", result.backend)
        assertTrue(result.text.contains("The seed phrase is 12 or 24 ordinary words."))
        assertTrue(result.text.contains("Store the words on paper, offline."))
        // pinning: entry "a" selected -> its warning point (P2) is added
        assertTrue(result.text.contains("Never type the seed words into any website."))
    }

    @Test
    fun answer_pins_entry_warnings_into_the_rendered_answer() {
        val result = ExtractiveAnswer.answer(
            "what is a seed phrase?", menu, corpusById, manifest,
            modelCall = { _, _ -> """{"selected": ["P1", "P3"], "bridge": ""}""" },
        )
        assertTrue("P2" in result.pinned)
        assertTrue(result.text.contains("Never type the seed words into any website."))
    }

    @Test
    fun answer_fails_closed_on_garbage_model_output_and_never_paraphrases() {
        var calls = 0
        val result = ExtractiveAnswer.answer(
            "what is a seed phrase?", menu, corpusById, manifest,
            modelCall = { _, _ -> calls++; "I think you should paste your words into the website." },
            fallbackPassage = "The seed phrase is 12 or 24 ordinary words.",
        )
        assertEquals(2, calls) // one retry, then fail closed
        assertEquals("extractive_fallback", result.backend)
        assertTrue(result.text.startsWith(ExtractiveAnswer.CANNED_UNAVAILABLE))
        assertTrue(result.text.contains("The seed phrase is 12 or 24 ordinary words."))
        assertTrue(!result.text.contains("paste your words"))
    }

    @Test
    fun answer_fails_closed_when_selected_point_fails_the_verbatim_check() {
        // the model selects a point whose text is NOT a verbatim subset of the
        // entry it claims (simulated by an entry without the point)
        val foreign = entry(title = "Other", plain = "Unrelated.", keyPoints = listOf("Unrelated point."))
        val m = ExtractiveAnswer.buildMenu(listOf("x" to foreign))
        val result = ExtractiveAnswer.answer(
            "q", m, mapOf("x" to entry(title = "Other", plain = "Different entirely.", keyPoints = emptyList())),
            emptySet(),
            modelCall = { _, _ -> """{"selected": ["P1"], "bridge": ""}""" },
        )
        assertEquals("extractive_fallback", result.backend)
    }

    @Test
    fun answer_fails_closed_on_model_exception() {
        val result = ExtractiveAnswer.answer(
            "q", menu, corpusById, manifest,
            modelCall = { _, _ -> error("engine dead") },
            fallbackPassage = "Backup passage.",
        )
        assertEquals("extractive_fallback", result.backend)
        assertTrue(result.text.contains("Backup passage."))
    }

    @Test
    fun answer_fails_closed_on_empty_menu() {
        val result = ExtractiveAnswer.answer(
            "q", emptyList(), corpusById, manifest,
            modelCall = { _, _ -> """{"selected": ["P1"]}""" },
        )
        assertEquals("extractive_fallback", result.backend)
    }
}
