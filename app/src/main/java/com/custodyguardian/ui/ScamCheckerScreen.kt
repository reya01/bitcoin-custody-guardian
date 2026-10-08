package com.custodyguardian.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.custodyguardian.LlamaEngine
import com.custodyguardian.util.MemoryGuards
import com.custodyguardian.util.ScamRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Model restatement of an already-fired verdict: select the loaded engine and ask. */
fun explainVerdict(context: android.content.Context, verdict: String): String {
    val mf = modelFile(context) ?: return ""
    val engine = LlamaEngine(mf.absolutePath)
    return try {
        engine.answer(
            systemPrompt = SIMPLER_WORDS_PROMPT,
            question = "The verdict shown on screen: \"$verdict\"\nRestate it.",
        ).trim()
    } finally {
        engine.unload()
    }
}

/**
 * ScamCheckerScreen — paste-a-message UI. Scanned by local regex rules
 * (ScamRules) with NO LLM — the verdict is instant and cannot be talked out
 * of. Optional model "Explain in simpler words" restates the verdict only
 * (≤2 sentences, no new claims). Secret material pasted by mistake is
 * blocked, not echoed. Copy: item2_copy.json (GLM-5.3 draft).
 */
const val SCAM_INTRO =
    "Paste any message below. It's checked instantly on your phone, and nothing ever leaves it."
const val SIMPLER_WORDS_PROMPT =
    "You are speaking to a grieving beginner who has just read a scam verdict on screen. " +
        "Restate that verdict in simpler, kinder words. Use at most two short sentences. " +
        "Add no new claims, details, or advice; only restate what the verdict already says. " +
        "Use everyday words only. Keep a calm, gentle tone."
@Composable
fun ScamCheckerScreen() {
    var text by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<ScamRules.ScanResult?>(null) }
    var secretBlocked by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Scam checker", style = MaterialTheme.typography.headlineSmall)
        Text(SCAM_INTRO, style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Paste the suspicious message") },
            minLines = 4,
        )
        val context = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val guard = MemoryGuards.detectSeedPhrase(context, text)
                if (guard is MemoryGuards.Result.SeedPhrase) {
                    secretBlocked = guard.message
                    result = null
                } else {
                    secretBlocked = null
                    result = ScamRules.scan(text)
                }
            }) { Text("Check message") }
            TextButton(onClick = { text = ""; result = null; secretBlocked = null }) {
                Text("Clear")
            }
        }

        val blocked = secretBlocked
        if (blocked != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
            ) { Text(blocked, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) }
        }

        val res = result
        if (res != null) {
            Text(
                res.verdict,
                style = MaterialTheme.typography.titleSmall,
                color = if (res.clean) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.error,
            )
            // Optional model restatement: only if a model is installed and a
            // verdict exists. The verdict above already fired — this is pure framing.
            if (modelFile(context) != null) {
                var simpler by remember { mutableStateOf<String?>(null) }
                var explaining by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                if (simpler != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            simpler!!,
                            Modifier.fillMaxWidth().padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    TextButton(
                        enabled = !explaining,
                        onClick = {
                            explaining = true
                            scope.launch(Dispatchers.Default) {
                                simpler = runCatching {
                                    explainVerdict(context, res.verdict)
                                }.getOrElse {
                                    "The explainer didn't work this time — the verdict above " +
                                        "still stands, and nothing was sent anywhere."
                                }
                                explaining = false
                            }
                        },
                    ) { Text(if (explaining) "One moment..." else "Explain in simpler words") }
                }
            }
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(res.flagged.size) { i ->
                    val f = res.flagged[i]
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("🚩 ${f.title}", style = MaterialTheme.typography.titleSmall)
                            Text(f.why, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
