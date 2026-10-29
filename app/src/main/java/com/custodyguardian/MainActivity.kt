package com.custodyguardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.custodyguardian.ui.ChatPlaceholderScreen
import com.custodyguardian.ui.CorpusBrowserScreen
import com.custodyguardian.ui.HomeModeScreen
import com.custodyguardian.ui.ScamCheckerScreen
import com.custodyguardian.ui.WalkthroughScreen

/**
 * MainActivity — the ONLY exported component. Compose-based navigation across
 * the app's screens. No network, no storage, no analytics.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                GuardianApp()
            }
        }
    }
}

object Routes {
    const val HOME = "home"
    const val WALKTHROUGH = "walkthrough"
    const val CORPUS = "corpus"
    const val CHAT = "chat"
    const val SCAM = "scam"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manifest = remember { com.custodyguardian.data.model.ModelPaths.manifest(context) }
    if (manifest == null) {
        // Fail closed (round-5 finding, MainActivity ~:53): an unreadable
        // manifest must never let the app silently skip the model gate and
        // show the unguarded content. Explicit error screen instead.
        ManifestErrorScreen()
        return
    }
    val gate = remember(manifest) {
        com.custodyguardian.ui.model.ModelGateState(context, manifest)
    }
    if (!gate.ready) {
        com.custodyguardian.ui.model.ModelGateScreen(gate) { gate.refresh() }
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.HOME

    val titles = mapOf(
        Routes.HOME to "Bitcoin Custody Guardian",
        Routes.WALKTHROUGH to "The First 7 Days",
        Routes.CORPUS to "Corpus browser",
        Routes.CHAT to "Ask (offline)",
        Routes.SCAM to "Scam checker",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titles[currentRoute] ?: "Bitcoin Custody Guardian") },
                navigationIcon = {
                    if (currentRoute != Routes.HOME) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(vertical = 0.dp),
        ) {
            composable(Routes.HOME) {
                HomeModeScreen(
                    onOpenWalkthrough = { navController.navigate(Routes.WALKTHROUGH) },
                    onOpenCorpusBrowser = { navController.navigate(Routes.CORPUS) },
                    onOpenChat = { navController.navigate(Routes.CHAT) },
                    onOpenScamChecker = { navController.navigate(Routes.SCAM) },
                )
            }
            composable(Routes.WALKTHROUGH) {
                WalkthroughScreen(
                    onAskAbout = { step ->
                        navController.navigate("${Routes.CHAT}?prefill=${android.net.Uri.encode(step)}")
                    },
                )
            }
            composable(Routes.CORPUS) { CorpusBrowserScreen() }
            composable("${Routes.CHAT}?prefill={prefill}") { backStack ->
                ChatPlaceholderScreen(
                    prefill = backStack.arguments?.getString("prefill"),
                )
            }
            composable(Routes.SCAM) { ScamCheckerScreen() }
        }
    }
}

/**
 * Fail-closed full-screen state for a manifest that could not be read
 * (round-5 finding, MainActivity ~:53). Replaces the entire app: the normal
 * screens are never rendered and there is no navigation out of this state —
 * the only remedy is reinstalling from the official release page. Plain
 * language only; no jargon.
 */
@Composable
private fun ManifestErrorScreen() {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                "Something is wrong with the app",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                "One of the app's built-in files — the one it uses to check that " +
                    "everything it shows you is genuine and safe — is damaged, so the " +
                    "app will not open its normal screens. That is on purpose: it is " +
                    "safer to stop than to show you anything it cannot check.\n\n" +
                    "Please uninstall the app and install it again from the official " +
                    "GitHub release page. The app has no internet access, so nothing " +
                    "you have done in it was ever sent anywhere.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
