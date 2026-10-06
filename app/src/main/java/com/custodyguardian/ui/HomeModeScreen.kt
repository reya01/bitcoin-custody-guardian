package com.custodyguardian.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Home screen: the three domain modes + chat. Spec §11 MVP. */
@Composable
fun HomeModeScreen(
    onOpenWalkthrough: () -> Unit,
    onOpenCorpusBrowser: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenScamChecker: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Bitcoin Custody Guardian", style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center)
        Text(
            "A private, local expert in Bitcoin self-custody that lives entirely on your phone. " +
                "No internet access — ever.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        ModeButton("Inheritance mode — The First 7 Days", onOpenWalkthrough)
        ModeButton("Corpus browser — what I know & where it's from", onOpenCorpusBrowser)
        ModeButton("Ask (offline chat)", onOpenChat)
        ModeButton("Scam checker — paste a message", onOpenScamChecker)
        Spacer(Modifier.height(8.dp))
        Surface(tonalElevation = 3.dp, shape = MaterialTheme.shapes.medium) {
            Text(
                "This is educational software, not financial advice. " +
                    "It cannot see your actual wallets.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ModeButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
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
