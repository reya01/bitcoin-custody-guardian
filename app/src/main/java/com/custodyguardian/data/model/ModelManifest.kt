package com.custodyguardian.data.model

import org.json.JSONObject

/**
 * Pinned model allowlist parsed from assets/model_manifest.json.
 *
 * The manifest is the single source of truth for the app, the packaging script,
 * and CI. Malformed or missing fields fail closed (ImportError-style: the app
 * shows an explicit error, never "works anyway").
 *
 * The 1.7B -> 4B switch is: add one entry to the JSON + flip defaultModelId.
 */
data class ManifestEntry(
    val id: String,
    val fileName: String,
    val sha256: String,
    val sizeBytes: Long,
    val minRamMb: Long,
    val ciDownloadUrl: String,
)

data class ModelManifest(
    val schemaVersion: Int,
    val defaultModelId: String,
    val models: List<ManifestEntry>,
) {
    fun byId(id: String): ManifestEntry? = models.firstOrNull { it.id == id }
    fun bySha256(hex: String): ManifestEntry? =
        models.firstOrNull { it.sha256.equals(hex, ignoreCase = true) }

    companion object {
        /** Parses [json]; throws IllegalArgumentException on any structural problem. */
        fun parse(json: String): ModelManifest {
            val root = JSONObject(json)
            val schemaVersion = root.optInt("schemaVersion", -1)
            if (schemaVersion != 1) {
                throw IllegalArgumentException("unsupported manifest schemaVersion: $schemaVersion")
            }
            val defaultModelId = root.optString("defaultModelId", "")
            if (defaultModelId.isBlank()) {
                throw IllegalArgumentException("manifest missing defaultModelId")
            }
            val arr = root.optJSONArray("models")
                ?: throw IllegalArgumentException("manifest missing models array")
            val models = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val id = o.optString("id", "")
                val fileName = o.optString("fileName", "")
                val sha256 = o.optString("sha256", "")
                val sizeBytes = o.optLong("sizeBytes", -1L)
                val minRamMb = o.optLong("minRamMb", 0L)
                val ciDownloadUrl = o.optString("ciDownloadUrl", "")
                if (id.isBlank() || fileName.isBlank() ||
                    !sha256.matches(Regex("[0-9a-fA-F]{64}")) || sizeBytes <= 0
                ) {
                    throw IllegalArgumentException("manifest entry $i incomplete or malformed")
                }
                ManifestEntry(id, fileName, sha256, sizeBytes, minRamMb, ciDownloadUrl)
            }
            if (models.isEmpty()) throw IllegalArgumentException("manifest has no models")
            if (models.none { it.id == defaultModelId }) {
                throw IllegalArgumentException("defaultModelId not present in models")
            }
            return ModelManifest(schemaVersion, defaultModelId, models)
        }
    }
}
