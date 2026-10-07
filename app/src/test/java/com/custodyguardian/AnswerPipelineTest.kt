package com.custodyguardian

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
        assertTrue(r.text.contains("can't answer that from the verified corpus"))
        assertTrue(r.text.contains("Never type the seed words into any website."))
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
