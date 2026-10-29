package com.custodyguardian

import com.custodyguardian.util.VetoRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the shipped veto layer (audit Medium 2026-10-19: the chat path
 * passed an empty must-not-include list, so the veto could never fire).
 * Core invariant: affirmative danger advice is vetoed; reviewed warning
 * sentences (which carry negations) always pass.
 */
class VetoRulesTest {

    // ---- findViolations: affirmative advice vetoes, warnings pass ----

    @Test
    fun affirmative_danger_advice_is_violation() {
        assertTrue(
            VetoRules.findViolations(
                "You can share your seed phrase with the support agent to verify your identity.",
            ).isNotEmpty(),
        )
    }

    @Test
    fun negated_warning_sentence_never_violates() {
        assertTrue(
            VetoRules.findViolations(
                "Never share your seed phrase with anyone — no real support agent will ever ask for it.",
            ).isEmpty(),
        )
    }

    @Test
    fun negation_only_needs_to_precede_the_phrase_in_its_sentence() {
        // negation AFTER the phrase must NOT rescue affirmative advice
        assertTrue(
            VetoRules.findViolations(
                "Share your seed phrase. Never do this with anyone.",
            ).isNotEmpty(),
        )
    }

    @Test
    fun non_seed_danger_phrases_still_veto_affirmatively() {
        assertTrue(
            VetoRules.findViolations(
                "It is fine — just allow the remote support session so they can fix it.",
            ).isNotEmpty(),
        )
    }

    @Test
    fun clean_text_has_no_violations() {
        assertTrue(
            VetoRules.findViolations(
                "Keep two copies of your backup in separate places. Waiting is safe.",
            ).isEmpty(),
        )
    }

    @Test
    fun affirmative_sentence_after_negated_one_still_vetoes() {
        // the negation in sentence 1 must not rescue the affirmative sentence 2
        assertTrue(
            VetoRules.findViolations("Never share your seed. Share your seed next time.").let {
                // the second sentence IS an affirmative violation — must fire
                it.isNotEmpty()
            },
        )
    }

    // ---- scrub: mandatory warning injection on seed-related answers ----

    @Test
    fun scrub_injects_warning_after_danger_sentence() {
        val (text, injected) = VetoRules.scrub(
            "If your wallet shows an error, do a factory reset and restore " +
                "from your backup words. Then check your funds.",
        )
        assertTrue(injected)
        assertTrue(text.contains(VetoRules.MANDATORY_WARNING))
        assertTrue(text.indexOf(VetoRules.MANDATORY_WARNING) > text.indexOf("factory reset"))
    }

    @Test
    fun scrub_does_not_inject_on_non_seed_topics() {
        val (text, injected) = VetoRules.scrub("The price of bitcoin changes every day.")
        assertFalse(injected)
        assertEquals("The price of bitcoin changes every day.", text)
    }

    @Test
    fun scrub_leaves_clean_seed_answer_unchanged() {
        val answer = "Store the words on paper, offline. Never photograph the word list."
        val (text, injected) = VetoRules.scrub(answer)
        assertFalse(injected)
        assertEquals(answer, text)
    }

    // ---- AnswerPipeline end-to-end: veto is reachable from the shipped path ----

    @Test
    fun pipeline_vetoes_affirmative_danger_advice() {
        val pipeline = AnswerPipeline { _, _ ->
            "First, do a factory reset, then enter your seed on the website they send you."
        }
        val result = pipeline.answer(
            "support agent says my wallet is broken",
            AnswerPipeline.ComposedContext(
                mode = "scam-check",
                isSecret = false,
                chunks = emptyList(),
                mustNotInclude = emptyList(), // the shipped state: global rules must still fire
                warnings = listOf("Never share your seed phrase with anyone."),
            ),
        )
        assertEquals("vetoed_refusal", result.backend)
        assertTrue(result.text.contains("Never share your seed phrase with anyone."))
    }

    @Test
    fun pipeline_passes_clean_answer_through() {
        val pipeline = AnswerPipeline { _, _ ->
            "Keep two copies of your backup in separate places."
        }
        val result = pipeline.answer(
            "how do I store my backup",
            AnswerPipeline.ComposedContext(
                mode = "learn",
                isSecret = false,
                chunks = emptyList(),
                mustNotInclude = emptyList(),
                warnings = emptyList(),
            ),
        )
        assertEquals("local_model", result.backend)
        assertTrue(result.text.contains("two copies"))
    }

    // ---- Extractive bridge: veto drops the bridge, points still render ----

    @Test
    fun bridge_with_danger_advice_is_dropped() {
        val violations = VetoRules.findViolations(
            "These points apply. It is safe to share the words with our agent.",
        )
        assertTrue(violations.isNotEmpty())
    }

    // ---- Design-review regressions (GLM-5.3 flex review, 2026-10-29) ----

    @Test
    fun explicit_negation_suppresses_veto() {
        assertTrue(
            VetoRules.findViolations("It is not safe to share your seed.").isEmpty(),
        )
    }

    @Test
    fun modal_negations_suppress_veto() {
        for (s in listOf(
            "You shouldn't share your seed with anyone.",
            "You can't share your seed, ever.",
            "You won't share your seed with the agent.",
        )) {
            assertTrue("should suppress: $s", VetoRules.findViolations(s).isEmpty())
        }
    }

    @Test
    fun curly_apostrophes_and_casing_are_normalized() {
        assertTrue(
            VetoRules.findViolations("Don\u2019t share your seed with anyone.").isEmpty(),
        )
        assertTrue(
            VetoRules.findViolations("NEVER SHARE YOUR SEED with anyone.").isEmpty(),
        )
    }

    @Test
    fun token_boundaries_stop_substring_negation_matches() {
        // "another" must NOT match the negation token "not"/"no"
        assertTrue(
            VetoRules.findViolations("Ask another agent and share your seed.").isNotEmpty(),
        )
    }

    @Test
    fun bullet_segments_are_judged_independently() {
        // sentence 1 is a warning; bullet 2 is affirmative advice -> violation
        assertTrue(
            VetoRules.findViolations(
                "Never share your seed with anyone.\n- if support asks, share your seed to verify",
            ).isNotEmpty(),
        )
    }

    @Test
    fun stale_negation_beyond_window_does_not_suppress() {
        assertTrue(
            VetoRules.findViolations(
                "Scammers stop at nothing to make you type your seed into their site.",
            ).isNotEmpty(),
        )
    }

    @Test
    fun reported_speech_is_not_advice() {
        // scam-check output legitimately DESCRIBES scammer asks
        assertTrue(
            VetoRules.findViolations(
                "If the caller asks you to share your seed, hang up immediately.",
            ).isEmpty(),
        )
    }

    @Test
    fun on_device_restore_guidance_never_vetoes() {
        assertTrue(
            VetoRules.findViolations(
                "Enter your seed on your new hardware wallet to restore your funds.",
            ).isEmpty(),
        )
    }

    @Test
    fun added_constructions_veto() {
        for (s in listOf(
            "Wipe your phone and start fresh.",
            "Take a photo of your seed so you have a copy.",
            "Store your seed in the cloud for safekeeping.",
            "Say your seed phrase slowly so they can write it down.",
        )) {
            assertTrue("should veto: $s", VetoRules.findViolations(s).isNotEmpty())
        }
    }

    @Test
    fun caller_constraints_are_judged_negation_aware() {
        assertTrue(
            VetoRules.findViolations(
                "Never share your seed phrase with anyone.",
                listOf("share your seed"),
            ).isEmpty(),
        )
        assertTrue(
            VetoRules.findViolations(
                "Just share your seed with the agent.",
                listOf("share your seed"),
            ).isNotEmpty(),
        )
    }

    @Test
    fun warning_only_seed_answer_renders_with_scrub_not_refusal() {
        // ordering regression: veto runs on the RAW candidate, so a scrub-
        // injected warning template can never self-trip into a refusal
        val pipeline = AnswerPipeline { _, _ ->
            "Never share your seed phrase with anyone. Keep the words on paper, offline."
        }
        val result = pipeline.answer(
            "what do I do with my seed phrase",
            AnswerPipeline.ComposedContext(
                mode = "learn",
                isSecret = false,
                chunks = emptyList(),
                mustNotInclude = emptyList(),
                warnings = emptyList(),
            ),
        )
        assertEquals("local_model", result.backend)
        assertTrue(result.text.contains("Keep the words on paper"))
    }

    @Test
    fun non_seed_danger_advice_vetoes_without_seed_gate() {
        assertTrue(
            VetoRules.findViolations("Factory reset your router to fix the Wi-Fi.").isNotEmpty(),
        )
    }

    @Test
    fun blank_and_empty_are_safe() {
        assertTrue(VetoRules.findViolations("").isEmpty())
        assertTrue(VetoRules.findViolations("   ").isEmpty())
    }

    // ---- Hardening round 2 (synthesis DO NOW 3, full-repo review 2026-10-29) ----

    @Test
    fun line_break_inside_phrase_cannot_bypass() {
        // normalize collapses whitespace, so a newline inside the phrase no
        // longer splits it across segments
        assertTrue(
            VetoRules.findViolations("Please share your\nseed phrase with our agent.").isNotEmpty(),
        )
    }

    @Test
    fun middle_noun_variants_cannot_bypass() {
        // "seed phrase" / "seed words" between "your" and the verb/object
        assertTrue(
            VetoRules.findViolations("Type your seed phrase into the website to check it.").isNotEmpty(),
        )
        assertTrue(
            VetoRules.findViolations("Enter your seed words on that site.").isNotEmpty(),
        )
    }

    @Test
    fun determiner_gaps_cannot_bypass() {
        assertTrue(
            VetoRules.findViolations("Type your seed into this website.").isNotEmpty(),
        )
    }

    @Test
    fun blank_extra_phrase_does_not_veto_everything() {
        assertTrue(
            VetoRules.findViolations("Keep two copies of your backup.", listOf("   ")).isEmpty(),
        )
    }

    @Test
    fun scam_descriptor_is_warning_context_not_advice() {
        assertTrue(
            VetoRules.findViolations("Remote support is a scam — refuse it.").isEmpty(),
        )
    }

    @Test
    fun on_device_restore_with_the_still_narrowed_patterns() {
        assertTrue(
            VetoRules.findViolations(
                "Enter your seed phrase on your hardware wallet to restore.",
            ).isEmpty(),
        )
    }

    @Test
    fun stop_and_avoid_are_not_negations() {
        // "stop at nothing to make you X" must not suppress X
        assertTrue(
            VetoRules.findViolations(
                "They will stop at nothing to make you share your seed.",
            ).isNotEmpty(),
        )
    }

    // ---- 2026-10-29 DS+K3 review: segmentation + suppression precision ----

    @Test
    fun veto_negation_never_crosses_line_boundary() {
        // F-a: a negation on line 1 must NOT suppress the trigger on line 2
        assertTrue(
            VetoRules.findViolations(
                "Never share your seed\nShare your seed with the support agent",
            ).isNotEmpty(),
        )
    }

    @Test
    fun veto_unicode_line_separator_is_boundary_and_still_matches() {
        // F-g: U+2028 becomes a segment boundary, but the phrase split
        // across it must STILL match (never plain-deleted)
        assertTrue(
            VetoRules.findViolations("share your\u2028seed with the agent").isNotEmpty(),
        )
    }

    @Test
    fun veto_soft_hyphen_cannot_bypass_matching() {
        // F-g: U+00AD is a Cf format character and must be stripped
        assertTrue(
            VetoRules.findViolations("share your\u00ADseed with the agent").isNotEmpty(),
        )
    }

    @Test
    fun veto_sentence_split_fires_without_following_space() {
        // F-h: "that.Share" is two segments; the negation in segment 1
        // must not rescue the affirmative segment 2
        assertTrue(
            VetoRules.findViolations("Never do that.Share your seed with them").isNotEmpty(),
        )
    }

    @Test
    fun veto_newline_split_phrase_still_matches_and_line_negation_still_suppresses() {
        // matching survives the newline split (F-a) ...
        assertTrue(
            VetoRules.findViolations("Please share your\nseed with our agent.").isNotEmpty(),
        )
        // ... while a negation on the SAME line still suppresses (F-a scope)
        assertTrue(
            VetoRules.findViolations("Never share your\nseed with anyone.").isEmpty(),
        )
    }

    @Test
    fun veto_reported_speech_beyond_window_does_not_suppress() {
        // F-b: the reported-speech marker is 6+ tokens before the SECOND
        // trigger — the old whole-clause check suppressed it, the 5-token
        // window must not
        assertTrue(
            VetoRules.findViolations(
                "The agent told me to enter my seed and you should enter your seed online",
            ).isNotEmpty(),
        )
    }

    @Test
    fun veto_scam_descriptor_after_conjunction_does_not_suppress() {
        // F-c: "is a scam" is in the clause BEFORE "and" — the affirmative
        // after the conjunction must still be vetoed
        assertTrue(
            VetoRules.findViolations(
                "This is a scam and you should share your seed with them",
            ).isNotEmpty(),
        )
        assertTrue(
            VetoRules.findViolations(
                "Remote support is a scam so share your seed with us",
            ).isNotEmpty(),
        )
    }

    @Test
    fun veto_warning_with_descriptor_before_trigger_stays_suppressed() {
        // F-c: descriptor BEFORE the trigger = warning context, not advice
        assertTrue(
            VetoRules.findViolations(
                "Remote support is a scam, never share your seed",
            ).isEmpty(),
        )
    }

    @Test
    fun veto_second_pattern_occurrence_found_after_suppressed_first() {
        // F-d: findAll — the suppressed first occurrence must not hide the
        // later affirmative one
        assertTrue(
            VetoRules.findViolations(
                "Never enter your seed online and also enter your seed online",
            ).isNotEmpty(),
        )
    }
}
