package com.custodyguardian

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the answer-path port (principles 7 + 8: no answer beats a wrong
 * answer; errors must be explicit; the veto discards wrong advice).
 */
class AnswerPipelineTest {

    private val warnings = listOf(
        "Never type the seed words into any website.",
        "Do not reset the device before the backup is verified.",
    )

    private fun ctx(
        mode: String = "learn",
        secret: Boolean = false,
        mustNot: List<String> = emptyList(),
    ) = AnswerPipeline.ComposedContext(
        mode = mode,
        isSecret = secret,
        chunks = listOf(
            AnswerPipeline.ComposedContext.Chunk("[T02:What is a seed phrase?]", "A seed phrase is the list of words that recreates your wallet."),
        ),
        mustNotInclude = mustNot,
        warnings = warnings,
    )

    private fun engine(reply: String) = AnswerPipeline.AnswerEngine { _, _ -> reply }

    @Test
    fun secretInput_refuses_deterministically_and_never_calls_model() {
        var called = false
        val p = AnswerPipeline(AnswerPipeline.AnswerEngine { _, _ -> called = true; "x" })
        val r = p.answer("my words are abandon ability able ...", ctx(secret = true))
        assertTrue(r.backend == "deterministic_secret_refusal")
        assertTrue(r.text.contains("STOP"))
        assertTrue(!called)
    }

    @Test
    fun veto_discards_wrong_advice_and_returns_refusal_with_warnings() {
        val p = AnswerPipeline(engine("You should just enter your seed into the site to check it."))
        val r = p.answer("how do I check my backup?", ctx(mustNot = listOf("enter your seed into the site")))
        assertTrue(r.backend == "vetoed_refusal")
        // distinct refusal: the cause is a failed safety check, not out-of-corpus
        assertTrue(r.text.contains("failed this app's safety check"))
        assertTrue(r.text.contains("Never type the seed words into any website."))
    }

    @Test
    fun veto_fires_from_the_GLOBAL_rule_set_with_empty_mustNot() {
        // audit Medium 2026-10-19 regression: the shipped path passes an empty
        // must-not list; the global VetoRules set must still veto.
        val p = AnswerPipeline(engine("You can share your seed phrase with the agent to verify."))
        val r = p.answer("is this agent legit?", ctx(mode = "scam-check", mustNot = emptyList()))
        assertTrue(r.backend == "vetoed_refusal")
    }

    @Test
    fun scam_verdict_is_shipped_on_blank_model_output() {
        // verdict is PRIMARY and un-overridable (SAFETY_CASE M1): a blank
        // model answer must not swallow the deterministic verdict.
        val p = AnswerPipeline(engine("   "))
        val r = p.answer(
            "A recovery agent says: pay a small fee and I unlock your wallet",
            ctx(mode = "scam-check"),
        )
        assertTrue(r.backend == "no_answer_with_verdict")
        assertTrue(r.text.contains("red flag"))
    }

    @Test
    fun scam_verdict_is_shipped_above_vetoed_model_output() {
        val p = AnswerPipeline(engine("It is fine — just share your seed with them."))
        val r = p.answer(
            "A recovery agent says: pay a small fee and I unlock your wallet",
            ctx(mode = "scam-check"),
        )
        assertTrue(r.backend == "vetoed_refusal")
        assertTrue(r.text.contains("red flag"))
        assertTrue(r.text.contains("failed this app's safety check"))
    }

    @Test
    fun scam_verdict_is_shipped_above_clean_model_output() {
        val p = AnswerPipeline(engine("This looks like the classic recovery-agent scam."))
        val r = p.answer(
            "A recovery agent says: pay a small fee and I unlock your wallet",
            ctx(mode = "scam-check"),
        )
        assertEquals("local_model", r.backend)
        // verdict text precedes the model text
        assertTrue(r.text.indexOf("red flag") < r.text.indexOf("classic recovery-agent scam"))
    }

    @Test
    fun engine_failure_produces_explicit_error_not_improvisation() {
        val p = AnswerPipeline(AnswerPipeline.AnswerEngine { _, _ -> error("engine dead") })
        val r = p.answer("what is a seed phrase?", ctx())
        assertTrue(r.backend == "error_reported")
        assertTrue(r.text.contains("Something went wrong"))
        assertTrue(r.text.contains("Technical detail"))
    }

    @Test
    fun empty_answer_becomes_honest_no_answer() {
        val p = AnswerPipeline(engine("   "))
        val r = p.answer("what is a seed phrase?", ctx())
        assertTrue(r.backend == "no_answer")
        assertTrue(r.text.contains("I don't know"))
    }

    @Test
    fun walkthrough_mode_appends_retrieved_warnings_verbatim() {
        val p = AnswerPipeline(engine("Step one: verify the checksum."))
        val r = p.answer("It's verification day, what now?", ctx(mode = "walkthrough"))
        assertTrue(r.text.contains("Safety rules for this step:"))
        assertTrue(r.text.contains("Do not reset the device before the backup is verified."))
    }

    @Test
    fun scam_check_mode_prepends_deterministic_verdict_context() {
        val p = AnswerPipeline(engine("This is the classic fake recovery agent scam."))
        val r = p.answer(
            "A recovery agent says: pay a small fee and I unlock your wallet",
            ctx(mode = "scam-check"),
        )
        // The engine still answers; the pipeline must not crash and must keep
        // the model text subject to the same rules.
        assertTrue(r.text.contains("classic fake recovery agent scam") || r.backend == "local_model")
    }
}
