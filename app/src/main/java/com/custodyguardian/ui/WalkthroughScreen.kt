package com.custodyguardian.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.custodyguardian.data.AssetLoader
import com.custodyguardian.data.Walkthrough

/**
 * WalkthroughScreen — renders the bundled 7-day inheritance walkthrough
 * (assets/walkthrough.json, converted from INHERITANCE_WALKTHROUGH.md).
 * Each day shows title, goal, DOs and DON'Ts. Never a forced sequence.
 */
@Composable
fun WalkthroughScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val wt: Walkthrough = remember { AssetLoader.loadWalkthrough(context) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("The First 7 Days", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        ElevatedCard {
            Text(
                "Rule #1 — Nothing is urgent. " + wt.rule_one,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
            items(wt.days.size) { i ->
                val day = wt.days[i]
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                        Text(
                            "Day ${day.day} — ${day.title}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (day.goal.isNotBlank()) {
                            Text("Goal today: ${day.goal}.", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        if (day.dos.isNotEmpty()) Text("DO", style = MaterialTheme.typography.labelLarge)
                        day.dos.forEach { Bullet("• $it") }
                        if (day.donts.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text("DON'T", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error)
                        }
                        day.donts.forEach { Bullet("✗ $it", error = true) }
                    }
                }
            }
        },
        )
    }
}

@Composable
private fun Bullet(text: String, error: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 8.dp, top = 2.dp),
    )
}
