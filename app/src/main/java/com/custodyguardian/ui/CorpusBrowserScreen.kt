package com.custodyguardian.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.custodyguardian.data.AssetLoader
import com.custodyguardian.data.CorpusEntry

/**
 * CorpusBrowserScreen — list of bundled corpus entries (assets/corpus JSON files)
 * + a detail view with title, risk class, plain text, claims, warnings, source.
 */
@Composable
fun CorpusBrowserScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val corpus = remember { AssetLoader.loadCorpus(context) }
    var selected: Pair<String, CorpusEntry>? by remember { mutableStateOf(null) }

    val sel = selected
    if (sel != null) {
        CorpusDetail(sel.first, sel.second, onBack = { selected = null })
    } else {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Knowledge corpus", style = MaterialTheme.typography.headlineSmall)
            Text(
                "${corpus.size} bundled entries. Versioned, human-reviewed, built into the APK — " +
                    "no download, ever.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(corpus) { (id, entry) ->
                    ElevatedCard(
                        Modifier.fillMaxWidth().clickable { selected = id to entry },
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(entry.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "$id · level=${entry.level} · risk=${entry.risk_class} · reviewed=${entry.reviewed}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CorpusDetail(id: String, entry: CorpusEntry, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(entry.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "$id · topics=${entry.topic_ids.joinToString(",")} · risk=${entry.risk_class} · corpus ${entry.corpus_version}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            item {
                Text("Plain language", style = MaterialTheme.typography.labelLarge)
                Text(entry.plain, style = MaterialTheme.typography.bodyMedium)
            }
            item {
                Text("Technical", style = MaterialTheme.typography.labelLarge)
                Text(entry.technical, style = MaterialTheme.typography.bodySmall)
            }
            items(entry.warnings.size) { i ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        "⚠ " + entry.warnings[i],
                        Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item {
                Text("Cited claims", style = MaterialTheme.typography.labelLarge)
            }
            items(entry.claims.size) { i ->
                val c = entry.claims[i]
                Text("• ${c.claim}\n  [source: ${c.source}]",
                    style = MaterialTheme.typography.bodySmall)
            }
            item {
                Text("Source: ${entry.source_title}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
