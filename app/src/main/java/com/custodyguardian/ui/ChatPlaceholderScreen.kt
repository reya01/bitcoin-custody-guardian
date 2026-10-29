package com.custodyguardian.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.custodyguardian.AnswerPipeline
import com.custodyguardian.ExtractiveAnswer
import com.custodyguardian.LlamaEngine
import com.custodyguardian.data.AssetLoader
import com.custodyguardian.util.CorpusRouter
import com.custodyguardian.util.MemoryGuards
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Chat screen: secret detection FIRST, then deterministic router + retrieval,
 * then the on-device model behind the full guardrail pipeline
 * (spec §8; principles 4, 7, 8).
 *
 * The model GGUF is sideloaded by the user (adb push or file manager) into
 * Android/data/com.custodyguardian/files/models/ — the app has no INTERNET
 * permission, so it can never download one itself.
 */
private const val MODEL_NAME = "qwen3-1.7b-q4_k_m.gguf"

fun modelFile(context: android.content.Context): File? =
    com.custodyguardian.data.model.ModelPaths.modelFile(context)

@Composable
fun ChatPlaceholderScreen(prefill: String? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(prefill ?: "") }
    var route by remember { mutableStateOf<CorpusRouter.Route?>(null) }
    var secretWarning by remember { mutableStateOf<String?>(null) }
    var answerState by remember { mutableStateOf<AnswerPipeline.Result?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Ask (offline) — alpha (experimental)", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Answers come from the curated corpus via the on-device model, checked by " +
                "deterministic safety rules. Never type seed words or keys into this app " +
                "or any online service.",
            style = MaterialTheme.typography.bodySmall,
        )
        // Permanent honesty surface: this app can be wrong (external review C2b).
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            Text(
                "This app can be wrong. Verify every instruction against your wallet's " +
                    "official documentation before you act. When unsure, stop and wait.",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Your question") },
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        // Single guarded entry point: secret detection FIRST on every path
        // (external review C1). On a hit, refuse — the model is never invoked,
        // and the secret-bearing text is cleared from the editable field.
        val secretHit = MemoryGuards.detectSecret(context, query) as? MemoryGuards.Refusal
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    when (val guard = MemoryGuards.detectSecret(context, query)) {
                        is MemoryGuards.Refusal -> {
                            secretWarning = guard.message
                            route = null
                            // Clear the pasted secret from the field (the old
                            // comment claimed this happened; now it does).
                            query = ""
                        }
                        else -> {
                            secretWarning = null
                            val corpus = AssetLoader.loadCorpus(context)
                            route = CorpusRouter.route(context, query, corpus)
                        }
                    }
                },
            ) { Text("Run router") }

            Button(
                enabled = !busy && modelFile(context) != null && secretHit == null,
                onClick = {
                    busy = true
                    scope.launch(Dispatchers.Default) {
                        // try/finally: a throw before runAnswer's internal try
                        // must never leave the button permanently disabled
                        // (full-repo review 2026-10-29).
                        val result = try {
                            runAnswer(context, query)
                        } catch (t: Throwable) {
                            AnswerPipeline.Result(
                                text = FALLBACK_ENGINE_ERROR,
                                backend = "engine_error",
                            )
                        } finally {
                            busy = false
                        }
                        answerState = result
                    }
                },
            ) { Text(if (busy) "Thinking..." else "Answer") }
        }

        if (modelFile(context) == null) {
            Text(
                "No on-device model found. Restart the app to open the import screen, " +
                    "or see the README's \"Getting the model\" section. Until then, only " +
                    "router retrieval is available.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        val warn = secretWarning
        if (warn != null) {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
            ) { Text(warn, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) }
        }

        val ans = answerState
        if (ans != null) {
            Spacer(Modifier.height(12.dp))
            ElevatedCard {
                Column(Modifier.padding(12.dp)) {
                    Text(ans.text, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("engine: ${ans.backend}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        val r = route
        if (r != null) {
            Spacer(Modifier.height(12.dp))
            Text("Detected intent: ${r.detectedIntent}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(r.retrieved.size) { i ->
                    val ret = r.retrieved[i]
                    ElevatedCard {
                        Column(Modifier.padding(12.dp)) {
                            Text(ret.entry.title, style = MaterialTheme.typography.titleSmall)
                            Text("(${ret.id} · score ${ret.score})",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    Text(r.note, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

const val FALLBACK_MODEL_MISSING =
    "The question helper isn't installed yet. Checklists, the scam check, and all reading " +
        "still work. You can add the helper later — see the app instructions."
const val FALLBACK_ENGINE_ERROR =
    "We're sorry. Something went wrong inside the app. The reviewed material below is still " +
        "shown, and nothing was sent anywhere."
const val FALLBACK_REFUSED =
    "I only speak from reviewed material. Here is the closest reviewed passage to your question."

private fun runAnswer(context: android.content.Context, question: String): AnswerPipeline.Result {
    // Defense-in-depth (full-repo review 2026-10-29): the guard also ran at
    // click time; re-check INSIDE the answer path so no code path can reach
    // the model with secret material, whatever the UI state said.
    when (val guard = MemoryGuards.detectSecret(context, question)) {
        is MemoryGuards.Refusal -> return AnswerPipeline.Result(
            text = guard.message,
            backend = "deterministic_secret_refusal",
        )
        else -> {}
    }
    val corpus = AssetLoader.loadCorpus(context)
    val routed = CorpusRouter.route(context, question, corpus)
    val mode = when {
        routed.detectedIntent.contains("scam", ignoreCase = true) -> "scam-check"
        routed.detectedIntent.contains("walkthrough", ignoreCase = true) -> "walkthrough"
        else -> "learn"
    }
    val mf = modelFile(context)
    if (mf == null) {
        return fallback(fallbackText = FALLBACK_MODEL_MISSING, backend = "model_missing", routed = routed)
    }

    // Learn / walkthrough questions go through the EXTRACTIVE path
    // (round-4 review consensus, plan item 2.1): the model only SELECTS
    // key-point IDs from the retrieved approved entries; the answer text is
    // the human-reviewed corpus verbatim, hash-checked against the bundled
    // manifest and re-checked verbatim against the claimed entry. Fail-closed:
    // any selection failure renders the canned message plus the top retrieved
    // passage — never a free-form paraphrase from the 1.7B model.
    if (mode != "scam-check") {
        val manifest = AssetLoader.loadKeyPointManifest(context)
        val corpusById = routed.retrieved.associate { it.id to it.entry }
        val menu = ExtractiveAnswer.buildMenu(routed.retrieved.map { it.id to it.entry })
        val passage = routed.retrieved.firstOrNull()?.entry?.plain?.take(600) ?: ""
        val engine = LlamaEngine(mf.absolutePath)
        return try {
            val sel = ExtractiveAnswer.answer(
                question = question,
                menu = menu,
                corpusById = corpusById,
                manifest = manifest,
                modelCall = { system: String, user: String -> engine.answer(system, user) },
                fallbackPassage = passage,
            )
            AnswerPipeline.Result(text = sel.text, backend = sel.backend)
        } catch (t: Throwable) {
            fallback(FALLBACK_ENGINE_ERROR, "engine_error", routed)
        } finally {
            runCatching { engine.unload() }
        }
    }

    // Scam-check keeps the existing pipeline: deterministic ScamRules verdict
    // (final) + model scenario guidance, subject to the veto layer.
    val chunks = routed.retrieved.take(6).map {
        AnswerPipeline.ComposedContext.Chunk("[${it.id}] ${it.entry.title}", it.entry.plain)
    }
    val warnings = routed.retrieved.flatMap { it.entry.warnings }
    val composed = AnswerPipeline.ComposedContext(
        mode = mode,
        isSecret = false, // secret questions are refused before reaching here
        chunks = chunks,
        mustNotInclude = emptyList(),
        warnings = warnings,
    )
    val engine = LlamaEngine(mf.absolutePath)
    return try {
        AnswerPipeline(engine).answer(question, composed)
    } catch (t: Throwable) {
        fallback(FALLBACK_ENGINE_ERROR, "engine_error", routed)
    } finally {
        runCatching { engine.unload() }
    }
}

/** No dead ends (plan item 1.5): any failure still renders the matched approved passage. */
private fun fallback(
    fallbackText: String,
    backend: String,
    routed: CorpusRouter.Route,
): AnswerPipeline.Result {
    val passage = routed.retrieved.firstOrNull()
    val text = buildString {
        append(fallbackText)
        if (passage != null) {
            append("\n\nFrom the reviewed corpus [")
            append(passage.id)
            append("] ")
            append(passage.entry.title)
            append(":\n")
            append(passage.entry.plain.take(600))
        }
    }
    return AnswerPipeline.Result(text = text, backend = backend)
}
