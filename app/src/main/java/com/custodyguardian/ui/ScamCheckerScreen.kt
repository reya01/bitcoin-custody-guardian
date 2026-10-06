package com.custodyguardian.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.custodyguardian.util.MemoryGuards
import com.custodyguardian.util.ScamRules

/**
 * ScamCheckerScreen — paste-a-message UI. Scanned by local regex rules
 * (ScamRules) with NO LLM. Any text is also run through the secret-material
 * detector so seed words pasted by mistake are blocked, not echoed.
 */
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
        Text(
            "Paste the email / message / forum post below. Analysis runs fully offline " +
                "with local regex rules — the text never leaves this device.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Paste the suspicious message") },
            minLines = 4,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val guard = MemoryGuards.detectSeedPhrase(text)
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
