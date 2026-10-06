package com.custodyguardian.data

import android.content.Context
import kotlinx.serialization.json.Json

/**
 * Reads bundled JSON assets. All content ships inside the APK at build time:
 * there is no download path in this app (no INTERNET permission by design).
 */
object AssetLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun loadWalkthrough(context: Context): Walkthrough = runCatching {
        json.decodeFromString<Walkthrough>(readAsset(context, "walkthrough.json"))
    }.getOrDefault(Walkthrough())

    /** All corpus entries from assets/corpus/, with their file id (e.g. "T02-river-seed-phrase"). */
    fun loadCorpus(context: Context): List<Pair<String, CorpusEntry>> {
        val out = mutableListOf<Pair<String, CorpusEntry>>()
        val files = context.assets.list("corpus")?.filter { it.endsWith(".json") } ?: return out
        for (file in files.sorted()) {
            runCatching {
                val entry = json.decodeFromString<CorpusEntry>(readAsset(context, "corpus/$file"))
                out.add(file.removeSuffix(".json") to entry)
            }
        }
        return out
    }

    fun readAsset(context: Context, path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    fun readAssetLines(context: Context, path: String): List<String> =
        context.assets.open(path).bufferedReader().readLines()
}
