package com.custodyguardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
