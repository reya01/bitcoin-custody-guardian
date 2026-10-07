package com.custodyguardian

import com.custodyguardian.util.ScamRules

/**
 * AnswerPipeline - the Kotlin port of the Python answer_step flow
 * (docs/PRODUCT_SPEC.md principles 4, 7, 8).
 *
 * Order:
 *   1. Secret input -> deterministic refusal, never reaches a model.
 *   2. Scam-check mode -> deterministic ScamRules verdict (final, never hedged).
 *   3. Model answer via [AnswerEngine] (local llama.cpp on device).
 *   4. Guardrail scrub + veto: any must-not-include hit DISCARDS the candidate
 *      and returns the refusal plus the retrieved entries' warnings verbatim.
 *   5. Walkthrough mode: retrieved warnings are appended verbatim, always.
 *   6. Engine failure -> explicit plain-language error; never improvised.
 *
 * Honesty over confidence (principle 7): the system prompt embeds rule 0, and
 * an empty model answer is treated as "no answer" rather than fabricated text.
 */
class AnswerPipeline(
    private val engine: AnswerEngine,
) {
    /** Abstraction over the on-device model so tests can run without it. */
    fun interface AnswerEngine {
        fun answer(systemPrompt: String, question: String): String
    }

    fun answer(question: String, composed: ComposedContext): Result {
        // 1. Secrets: refuse deterministically (spec §3.2).
        if (composed.isSecret) {
            return Result(
                text = SECRET_REFUSAL,
                backend = "deterministic_secret_refusal",
            )
        }
        // 2. Scam verdict: rules only, scam-check mode only.
        val verdict = if (composed.mode == "scam-check") {
            val scan = ScamRules.scan(question)
            if (!scan.clean) scan.verdict else null
        } else null
        val systemPrompt = buildSystemPrompt(composed, verdict != null)

        val candidate: String
        val backend: String
        try {
            candidate = engine.answer(systemPrompt, question)
            backend = "local_model"
        } catch (e: Exception) {
            // 6. Errors must be explicit; never improvise through an outage.
            return Result(
                text = ERROR_TEXT + "\n\n(Technical detail: " + (e.message ?: e.javaClass.simpleName) + ")",
                backend = "error_reported",
            )
        }
        if (candidate.isBlank()) {
            return Result(
                text = "I don't know this one — it's not in my reviewed material. " +
                    "Please try asking in a different way, or ask about wallets, seed " +
                    "phrases, backups, scams, or inheritance planning.",
                backend = "no_answer",
            )
        }

        // 4. Veto: wrong advice is structurally unshippable.
        var text = candidate
        val violated = composed.mustNotInclude.firstOrNull { text.lowercase().contains(it.lowercase()) }
        if (violated != null) {
            val warns = composed.warnings.distinct().joinToString("\n") { "• $it" }
            text = OUT_OF_CORPUS_REFUSAL +
                (if (composed.warnings.isNotEmpty()) "\n\nSafety rules I must repeat:\n$warns" else "")
            return Result(text = text, backend = "vetoed_refusal")
        }

        // 5. Walkthrough mode ships retrieved warnings verbatim.
        if (composed.mode == "walkthrough") {
            val missing = composed.warnings.filter { it !in text }.distinct()
            if (missing.isNotEmpty()) {
                text += "\n\nSafety rules for this step:\n" + missing.joinToString("\n") { "• $it" }
            }
        }
        return Result(text = text, backend = backend)
    }

    private fun buildSystemPrompt(composed: ComposedContext, hasVerdict: Boolean): String {
        val sb = StringBuilder(SYSTEM_PREAMBLE)
        sb.append("\n\nCORPUS CHUNKS:\n")
        composed.chunks.forEachIndexed { i, ch ->
            sb.append("CHUNK ${i + 1} [${ch.cite}] ${ch.text}\n\n")
        }
        if (hasVerdict) {
            sb.append(
                "The deterministic rule engine has ALREADY flagged this message as a scam " +
                    "and produced a verdict block. That verdict is final: do not soften, " +
                    "hedge, or re-derive it. Add ONLY scenario-specific guidance grounded " +
                    "in the retrieved corpus chunks.\n"
            )
        }
        return sb.toString()
    }

    data class ComposedContext(
        val mode: String,
        val isSecret: Boolean,
        val chunks: List<Chunk>,
        val mustNotInclude: List<String>,
        val warnings: List<String>,
    ) {
        data class Chunk(val cite: String, val text: String)
    }

    data class Result(val text: String, val backend: String)

    companion object {
        val SECRET_REFUSAL = (
            "STOP — this looks like secret wallet material (seed words or a private key). " +
                "This app has refused to process it. Never type or paste seed words into ANY " +
                "app, website, or chat — including this one. No legitimate party — wallet " +
                "vendor, support agent, or 'recovery expert' — will ever need your words. " +
                "If someone asked you for them, it is a scam. Handle the phrase only on " +
                "paper, offline, and move the text out of this screen."
            )
        val OUT_OF_CORPUS_REFUSAL =
            "I can't answer that from the verified corpus. This app only answers questions " +
                "covered by its reviewed curriculum. Try asking about wallets, seed phrases, " +
                "backups, scams, or inheritance planning."
        val ERROR_TEXT =
            "Something went wrong inside the app while preparing this answer (the answer " +
                "engine could not be reached). No answer was produced — this app never " +
                "improvises when its safety-checked engine is unavailable. What you can " +
                "do: close and reopen the app and ask again. If it keeps failing, the " +
                "question text and this message can be shared with a support contact — " +
                "the app never sends anything by itself."
        val SYSTEM_PREAMBLE = """
You are the answer engine of Bitcoin Custody Guardian, an offline, privacy-first
app that teaches Bitcoin self-custody. You are a careful, grounded assistant for
a non-technical person who may have inherited bitcoin.

RULES (obey exactly):
0. HONESTY OVER CONFIDENCE: giving NO answer is always better than giving an incorrect one. If the corpus chunks do not clearly cover what the user asked, say plainly: "I don't know this — it's not in my reviewed material," and say what IS covered. Never guess or fill gaps with plausible-sounding advice.
1. ASSEMBLE, do not invent. Quote the corpus claims as close to verbatim as possible; smooth the joins for readability.
2. Cite each chunk you take material from like [T02:What is a seed phrase?].
3. Decline only when NO chunk relates; never invent facts.
4. NEVER include secrets, seed words, or private keys in an answer. Never advise resetting or wiping a device that may hold bitcoin before the backup words are verified.
5. Never recommend a specific paid product, vendor, lawyer, or financial decision.
6. For scam situations: name the scam plainly and confidently; do not hedge.
7. Be concrete and calm. Short sentences. Clear DO / DO NOT guidance.
""".trim()
    }
}
