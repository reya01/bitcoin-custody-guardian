package com.custodyguardian.ui.model

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.custodyguardian.data.model.ManifestEntry
import com.custodyguardian.data.model.ModelImporter
import com.custodyguardian.data.model.ModelLocations
import com.custodyguardian.data.model.ModelManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Full-screen gate shown until a verified model exists. Wraps the whole app:
 * `if (!state.ready) ModelGateScreen(...) else <app content>`.
 *
 * Phases: Welcome -> Picking -> Copying(progress) -> Success | Error(reason).
 * No network is ever used; the only side channels are the SAF picker (user-
 * initiated) and an ACTION_VIEW intent that hands off to the browser app.
 */
class ModelGateState(
    private val context: Context,
    val manifest: ModelManifest,
) {
    var ready by mutableStateOf(false)
        private set
    var phase by mutableStateOf(Phase.WELCOME)
        private set
    var progressBytes by mutableStateOf(0L)
        private set
    var totalBytes by mutableStateOf(1L)
        private set
    var errorText by mutableStateOf<String?>(null)
        private set
    var deleteSource by mutableStateOf(true)
    var pickedUri by mutableStateOf<Uri?>(null)
        private set

    val entry: ManifestEntry = manifest.byId(manifest.defaultModelId)!!

    enum class Phase { WELCOME, COPYING, SUCCESS, ERROR }

    init {
        refresh()
    }

    /** Re-checks for a usable model (internal record or legacy in-place verify). */
    fun refresh() {
        ready = ModelLocations.isReady(context, manifest)
        if (ready) phase = Phase.SUCCESS
    }

    fun onPicked(uri: Uri?) {
        if (uri == null) return // user cancelled the picker: stay on Welcome
        pickedUri = uri
        errorText = null
        startedFor = null
        phase = Phase.COPYING
        progressBytes = 0
        totalBytes = entry.sizeBytes
    }

    private var startedFor: Uri? = null

    /**
     * Runs the import on a worker dispatcher; UI observes [phase]/[progressBytes].
     * Called from the UI when the phase is COPYING; safe to call repeatedly —
     * only the first call for a given Uri does work.
     */
    fun startImport(scope: kotlinx.coroutines.CoroutineScope) {
        val uri = pickedUri ?: return
        if (startedFor == uri) return
        startedFor = uri
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                val resolver = context.contentResolver
                ModelImporter.import(
                    openStream = {
                        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                        resolver.openInputStream(uri)
                            ?: throw java.io.IOException("cannot open selected file")
                    },
                    entry = entry,
                    modelsDir = ModelLocations.internalModelsDir(context),
                    onProgress = { done, total ->
                        progressBytes = done
                        totalBytes = total
                    },
                    ensureActive = { ensureActive() },
                )
            }
            when (result) {
                is ModelImporter.ImportResult.Ok -> {
                    ModelLocations.writeRecord(context, entry)
                    if (deleteSource) deleteSourceQuietly(pickedUri)
                    phase = Phase.SUCCESS
                    ready = true
                }
                is ModelImporter.ImportResult.Incomplete ->
                    fail(
                        "The file is incomplete: got ${result.got / (1024 * 1024)} MB, " +
                            "expected ${result.expected / (1024 * 1024)} MB. " +
                            "The download probably did not finish — delete it and download again.",
                    )
                is ModelImporter.ImportResult.HashMismatch ->
                    fail(
                        "This file is not the model this app version expects (its content " +
                            "does not match the published SHA-256). Delete it and download " +
                            "the current file from the app's GitHub release page.",
                    )
                ModelImporter.ImportResult.NotAModel ->
                    fail(
                        "That file is not a model file. Select either the " +
                            "btcg-...-full.zip bundle or the ${entry.fileName} file.",
                    )
                ModelImporter.ImportResult.NoModelInZip ->
                    fail(
                        "That zip does not contain a model file. Download the current " +
                            "btcg-...-full.zip from the app's GitHub release page.",
                    )
                is ModelImporter.ImportResult.SourceUnreadable ->
                    fail(
                        "The selected file could not be read. If it is on cloud storage, " +
                            "download it to the device first, then pick it again.",
                    )
            }
        }
    }

    fun retry() {
        if (pickedUri != null) {
            startedFor = null
            phase = Phase.COPYING
        } else {
            phase = Phase.WELCOME
        }
    }

    fun backToWelcome() {
        phase = Phase.WELCOME
        errorText = null
    }

    private fun fail(text: String) {
        errorText = text
        phase = Phase.ERROR
    }

    private fun deleteSourceQuietly(uri: Uri?) {
        if (uri == null) return
        try {
            android.provider.DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (_: Exception) {
            // Non-fatal: the user can delete the file themselves.
        }
    }
}

/** Opens the system document picker. Content is sniffed by magic bytes, not name. */
@Composable
fun rememberModelPicker(onPicked: (Uri?) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> onPicked(uri) }
    return {
        launcher.launch(arrayOf("*/*"))
    }
}

@Composable
fun ModelGateScreen(state: ModelGateState, onOpenChat: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pick = rememberModelPicker { state.onPicked(it) }

    // Kick off the import whenever we reach the COPYING phase (auto-retries too).
    LaunchedEffect(state.phase, state.pickedUri) {
        if (state.phase == ModelGateState.Phase.COPYING) state.startImport(scope)
    }
    // Keep the screen on during the long copy.
    if (state.phase == ModelGateState.Phase.COPYING) {
        val view = LocalView.current
        DisposableEffect(view) {
            view.keepScreenOn = true
            onDispose { view.keepScreenOn = false }
        }
    }

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
            when (state.phase) {
                ModelGateState.Phase.WELCOME -> {
                    Text(
                        "One thing to set up",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Bitcoin Custody Guardian runs entirely on your phone. It has no " +
                            "internet access at all — not a promise, but enforced by Android: " +
                            "the app never requests the internet permission, so the system " +
                            "blocks it from going online. Your questions never leave this " +
                            "device.\n\nBecause it can't go online, it can't download its AI " +
                            "model by itself. You give it the file once. After that, it works " +
                            "offline forever.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("You need: the model file (about 1.1 GB).", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Easiest: the btcg-…-full.zip from the app's GitHub Releases page — " +
                                    "the app reads the model straight out of the zip, no unzipping needed. " +
                                    "Or pick the qwen3-1.7b-q4_k_m.gguf file if you already have it.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "Needs about 1.1 GB of free storage · alpha software",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = state.deleteSource, onCheckedChange = { state.deleteSource = it })
                        Text(
                            "Delete the original file after it's safely imported (frees about 1.1 GB)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Button({ pick() }, Modifier.fillMaxWidth()) { Text("Select the model file") }
                    OutlinedButton(
                        {
                            try {
                                context.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(
                                            "https://github.com/reya01/bitcoin-custody-guardian/releases",
                                        ),
                                    ),
                                )
                            } catch (_: Exception) { /* no browser: user opens it themselves */ }
                        },
                        Modifier.fillMaxWidth(),
                    ) { Text("I don't have the file yet (open the download page)") }
                }

                ModelGateState.Phase.COPYING -> {
                    Text("Importing model…", style = MaterialTheme.typography.headlineSmall)
                    val frac = state.progressBytes.toFloat() / state.totalBytes.toFloat()
                    LinearProgressIndicator(
                        progress = { frac.coerceIn(0f, 1f) },
                        Modifier.fillMaxWidth(),
                    )
                    Text(
                        "${state.progressBytes / (1024 * 1024)} MB of ${state.totalBytes / (1024 * 1024)} MB\n" +
                            "This takes a few minutes — keep the app open. The file is checked " +
                            "against the published SHA-256 while it copies.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }

                ModelGateState.Phase.SUCCESS -> {
                    Text("You're set", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "The model is verified and stored inside the app. Everything now " +
                            "works offline — nothing you do here ever reaches the internet.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Button(onOpenChat, Modifier.fillMaxWidth()) { Text("Start") }
                }

                ModelGateState.Phase.ERROR -> {
                    Text("Import failed", style = MaterialTheme.typography.headlineSmall)
                    Text(state.errorText ?: "", style = MaterialTheme.typography.bodyMedium)
                    Button({ state.retry() }, Modifier.fillMaxWidth()) { Text("Try again") }
                    OutlinedButton({ state.backToWelcome() }, Modifier.fillMaxWidth()) {
                        Text("Choose a different file")
                    }
                }
            }
        }
    }
}
