package com.custodyguardian.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.custodyguardian.data.AssetLoader
import com.custodyguardian.util.CorpusRouter
import com.custodyguardian.util.MemoryGuards

/**
 * ChatPlaceholderScreen — deterministic-router stub (no LLM yet).
 * Pipeline per spec §8: secret detector runs FIRST (warn + refuse), then the
 * deterministic intent router scores keywords and retrieves corpus entries.
 */
@Composable
fun ChatPlaceholderScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var query by remember { mutableStateOf("") }
    var route by remember { mutableStateOf<CorpusRouter.Route?>(null) }
    var secretWarning by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Ask (offline)", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Stub: answers come from a deterministic router + retrieval only — no language " +
                "model yet. Never type seed words or keys here (or anywhere).",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Your question") },
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                // Safety router step 1: secret-material detection.
                val guard = MemoryGuards.detectSeedPhrase(context, query)
                if (guard is MemoryGuards.Result.SeedPhrase) {
                    secretWarning = guard.message
                    route = null
                } else {
                    secretWarning = null
                    val corpus = AssetLoader.loadCorpus(context)
                    route = CorpusRouter.route(context, query, corpus)
                }
            },
        ) { Text("Run router") }

        val warn = secretWarning
        if (warn != null) {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
            ) { Text(warn, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) }
        }

        val r = route
        if (r != null) {
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text("Detected intent: ${r.detectedIntent}", style = MaterialTheme.typography.titleMedium)
                    if (r.keywordsMatched.isNotEmpty()) {
                        Text("Keywords: ${r.keywordsMatched.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (r.isRefusal) {
                    item {
                        ElevatedCard {
                            Text(r.note, Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    items(r.retrieved.size) { i ->
                        val ret = r.retrieved[i]
                        ElevatedCard {
                            Column(Modifier.padding(12.dp)) {
                                Text(ret.entry.title, style = MaterialTheme.typography.titleSmall)
                                Text("(${ret.id} · score ${ret.score})",
                                    style = MaterialTheme.typography.bodySmall)
                                val first = ret.entry.plain.take(400)
                                Text(
                                    if (ret.entry.plain.length > 400) first + "…" else first,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
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
