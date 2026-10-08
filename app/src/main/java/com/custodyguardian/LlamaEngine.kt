package com.custodyguardian

import com.custodyguardian.util.CorpusRouter

/**
 * Real on-device AnswerEngine: corpus retrieval -> extractive-leaning chat
 * prompt (mirrors prototype/answer_step.py) -> llama.cpp greedy completion.
 *
 * Extractive prompt (rule 1): the model is a reader and explainer of vetted
 * material, not an oracle. Greedy sampling keeps runs reproducible.
 */
class LlamaEngine(
    private val modelPath: String,
    private val nCtx: Int = 3072,
    private val maxTokens: Int = 450,
) : AnswerPipeline.AnswerEngine {

    /** Call once from a background thread before answering. */
    fun ensureLoaded(): Boolean = LlamaBridge.loadModel(modelPath, nCtx)

    fun unload() = LlamaBridge.unload()

    override fun answer(systemPrompt: String, question: String): String {
        if (!ensureLoaded()) {
            throw IllegalStateException("on-device model failed to load")
        }
        // /no_think matches the Qwen3 chat template used in the prototype;
        // greedy sampling keeps answers deterministic on repeat questions.
        val prompt = "$systemPrompt\n\nUSER QUESTION: $question /no_think\nASSISTANT:"
        val raw = LlamaBridge.complete(prompt, maxTokens)
        return stripThink(raw)
    }

    companion object {
        /** Qwen3 emits <think>...</think> even under /no_think; drop it. */
        fun stripThink(text: String): String {
            val t = text.trim()
            if (!t.contains("<think>")) return t
            val end = t.indexOf("</think>")
            return (if (end >= 0) t.substring(end + "</think>".length) else "").trim()
        }
    }
}
