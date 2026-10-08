package com.custodyguardian

/**
 * Thin JNI bridge to llama.cpp (app/src/main/cpp/llama_jni.cpp).
 * Loads a bundled GGUF once, then serves greedy completions.
 * All methods block; call from a background dispatcher only.
 */
object LlamaBridge {
    init {
        System.loadLibrary("llama_btcg")
    }

    /** Returns true when the model is resident. Idempotent. */
    external fun loadModel(modelPath: String, nCtx: Int): Boolean

    /** Greedy completion. Empty string when no model is loaded. */
    external fun complete(prompt: String, maxTokens: Int): String

    external fun unload()
}
