package com.custodyguardian.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.custodyguardian.data.AssetLoader
import com.custodyguardian.data.Walkthrough
import com.custodyguardian.data.WalkthroughDay

/**
 * Guided mode v1 (plan item 1.1 + 1.8): the First-7-Days walkthrough rendered
 * one day at a time from the bundled approved JSON. The checklist content is
 * deterministic — the model can explain a step, never generate, reorder, or
 * complete one. Progress = plain booleans in SharedPreferences
 * (allowBackup=false; no secrets ever stored). Pacing: suggests the first day
 * with unfinished steps, shows the "done for today" state, no gamification.
 */
private const val PREFS = "walkthrough_progress"

private fun keyFor(day: Int, step: String) = "d${day}_s${step.hashCode()}"

private fun loadProgress(context: android.content.Context): MutableSet<String> =
    context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        .getStringSet("done", mutableSetOf()) ?: mutableSetOf()

private fun saveProgress(context: android.content.Context, done: Set<String>) {
    context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        .edit().putStringSet("done", done).apply()
}

private fun dayComplete(day: WalkthroughDay, done: Set<String>) =
    day.dos.isNotEmpty() && day.dos.all { keyFor(day.day, it) in done }

@Composable
fun WalkthroughScreen(onAskAbout: (String) -> Unit = {}) {
    val context = LocalContext.current
    val wt: Walkthrough = remember { AssetLoader.loadWalkthrough(context) }
    var done by remember { mutableStateOf(loadProgress(context).toSet()) }
    // pacing: default to the first day with unfinished steps
    var selectedDay by remember {
        mutableStateOf(wt.days.firstOrNull { !dayComplete(it, done) }?.day ?: 1)
    }
    val day = wt.days.firstOrNull { it.day == selectedDay } ?: wt.days.firstOrNull() ?: return
    val complete = dayComplete(day, done)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("The First 7 Days", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        // day chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            wt.days.forEach { d ->
                val isDone = dayComplete(d, done)
                FilterChip(
                    selected = d.day == selectedDay,
                    onClick = { selectedDay = d.day },
                    label = { Text(if (isDone) "✓${d.day}" else "${d.day}") },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        ElevatedCard {
            Text(
                "Rule #1 — Nothing is urgent. " + wt.rule_one,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Day ${day.day} — ${day.title}",
                    style = MaterialTheme.typography.titleLarge,
                )
                if (day.goal.isNotBlank()) {
                    Text("Goal today: ${day.goal}.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(day.notes) { n ->
                Text(n, style = MaterialTheme.typography.bodySmall)
            }
            if (day.dos.isNotEmpty()) {
                item { Text("DO — one at a time", style = MaterialTheme.typography.labelLarge) }
                items(day.dos) { step ->
                    val checked = keyFor(day.day, step) in done
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { on ->
                                    val s = loadProgress(context).toMutableSet()
                                    if (on) s.add(keyFor(day.day, step)) else s.remove(keyFor(day.day, step))
                                    saveProgress(context, s)
                                    done = s.toSet()
                                },
                            )
                            Column(Modifier.padding(vertical = 10.dp)) {
                                Text(step, style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { onAskAbout(step) }) {
                                    Text("Ask about this step")
                                }
                            }
                        }
                    }
                }
            }
            if (day.donts.isNotEmpty()) {
                item {
                    Text(
                        "DON'T — nothing here is ever safe to do",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                items(day.donts) { d ->
                    Text(
                        "✗ $d",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            item {
                if (complete) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(
                                "You did enough today. Come back tomorrow.",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            if (day.feel_after.isNotBlank()) {
                                Text(day.feel_after, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else if (day.dos.isNotEmpty()) {
                    Text(
                        "Unchecked steps stay for tomorrow. Nothing is lost by waiting.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
