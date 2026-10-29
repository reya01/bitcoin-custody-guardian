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

    /**
     * All corpus entries from assets/corpus/, with their file id (e.g. "T02-river-seed-phrase").
     *
     * Only `reviewed == true` entries are served (full-repo review 2026-10-29):
     * an unreviewed draft must never reach users as if human-approved. Parse
     * failures and skipped drafts are counted, not swallowed — the count is
     * visible in [skippedDrafts] for the browser screen to surface.
     */
    fun loadCorpus(context: Context): List<Pair<String, CorpusEntry>> {
        val out = mutableListOf<Pair<String, CorpusEntry>>()
        skippedDrafts = 0
        parseErrors = 0
        val files = context.assets.list("corpus")?.filter { it.endsWith(".json") } ?: return out
        for (file in files.sorted()) {
            runCatching {
                val entry = json.decodeFromString<CorpusEntry>(readAsset(context, "corpus/$file"))
                if (entry.reviewed) {
                    out.add(file.removeSuffix(".json") to entry)
                } else {
                    skippedDrafts++
                }
            }.onFailure { parseErrors++ }
        }
        return out
    }

    /** Entries skipped for reviewed=false in the last loadCorpus call. */
    var skippedDrafts: Int = 0
        private set

    /** Corpus files that failed to parse in the last loadCorpus call. */
    var parseErrors: Int = 0
        private set

    /**
     * The bundled key_points sha256 manifest (prototype/gen_manifest.py output,
     * regenerated from the repo corpus on every corpus change). Keys only: the
     * extractive renderer verifies each rendered point's hash against this set.
     */
    fun loadKeyPointManifest(context: Context): Set<String> = runCatching {
        json.decodeFromString<Map<String, kotlinx.serialization.json.JsonElement>>(
            readAsset(context, "key_points_manifest.json"),
        ).keys
    }.getOrDefault(emptySet())

    fun readAsset(context: Context, path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    fun readAssetLines(context: Context, path: String): List<String> =
        context.assets.open(path).bufferedReader().use { it.readLines() }
}
