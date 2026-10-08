package com.custodyguardian.ui

import com.custodyguardian.Routes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Home screen — the situation router (plan item 1.2). A Day-1 user never sees
 * a blank text box: four situation cards map directly to the four
 * capabilities, the two danger-relevant ones first. Copy: item2_copy.json
 * (GLM-5.3 draft, human-reviewed). Spec §11 MVP + §8 pacing principles.
 */

data class Situation(val id: String, val title: String, val subtitle: String, val route: String, val emphasized: Boolean = false)

val SITUATIONS = listOf(
    Situation("found", "I found something",
        "You're not alone. We'll sort out what you found, gently, step by step.",
        Routes.WALKTHROUGH, emphasized = true),
    Situation("suspicious", "Someone contacted me",
        "Paste it here before you reply. Checking first is never a mistake.",
        Routes.SCAM, emphasized = true),
    Situation("own_setup", "My own setup",
        "Reviewed guides on safe storage, in plain words, ready whenever you are.",
        Routes.CORPUS),
    Situation("learning", "I want to learn",
        "Ask anything about bitcoin in plain words. There are no silly questions here.",
        Routes.CHAT),
)

const val URGENT_BANNER =
    "Nothing here is urgent. Waiting is safe. Rushing only helps people who want your money."
const val HOME_FOOTER =
    "Everything you do here stays on this phone. Nothing is sent, shared, or stored anywhere else. " +
        "This is educational software, not financial advice. It cannot see your actual wallets."

@Composable
fun HomeModeScreen(
    onOpenWalkthrough: () -> Unit,
    onOpenCorpusBrowser: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenScamChecker: () -> Unit,
) {
    val routeFor: (String) -> (() -> Unit) = { r ->
        when (r) {
            Routes.WALKTHROUGH -> onOpenWalkthrough
            Routes.CORPUS -> onOpenCorpusBrowser
            Routes.CHAT -> onOpenChat
            Routes.SCAM -> onOpenScamChecker
            else -> onOpenCorpusBrowser
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Bitcoin Custody Guardian", style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center)
        Surface(
            tonalElevation = 3.dp,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text(
                URGENT_BANNER,
                Modifier.fillMaxWidth().padding(12.dp),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
        }
        for (s in SITUATIONS) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = routeFor(s.route),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(
                        s.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (s.emphasized) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(s.subtitle, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            HOME_FOOTER,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

/** Simple card list used by several screens. */
@Composable
fun <T> CardList(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemContent: @Composable ColumnScope.(T) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items) { item ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) { itemContent(item) }
            }
        }
    }
}
