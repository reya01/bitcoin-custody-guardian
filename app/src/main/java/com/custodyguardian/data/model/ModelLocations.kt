package com.custodyguardian.data.model

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Resolves the model location and records verification state.
 *
 * Locations (in order):
 *  1. internal:  context.filesDir/models/<fileName>   (new installs — private, no permissions)
 *  2. legacy:    context.getExternalFilesDir(null)/models/<fileName>  (v0.1.0 adb upgraders)
 *
 * A small record file (verified.json) stores {fileName, sha256, sizeBytes} after a
 * successful import/verification, so launch does NOT re-hash 1.1 GB. The record is
 * accepted only when the file exists with the exact recorded size.
 */
object ModelLocations {

    const val RECORD_FILE = "verified.json"

    fun internalModelsDir(context: Context): File =
        File(context.filesDir, "models")

    fun legacyModelsDir(context: Context): File? {
        val ext = context.getExternalFilesDir(null) ?: return null
        return File(ext, "models")
    }

    /** The model file to load, or null when absent. Internal wins over legacy. */
    fun resolve(context: Context, manifest: ModelManifest): File? {
        val entry = manifest.byId(manifest.defaultModelId) ?: return null
        val record = readRecord(context)
        val internal = File(internalModelsDir(context), entry.fileName)
        if (internal.canRead() && recordMatches(record, internal, entry)) return internal
        val legacy = legacyModelsDir(context)?.let { File(it, entry.fileName) }
        if (legacy != null && legacy.canRead() && recordMatches(record, legacy, entry)) return legacy
        return null
    }

    /**
     * One-time legacy verification for v0.1.0 upgraders: hashes the external file
     * once, and on match writes the record so future launches are instant.
     * Returns the usable file, or null (record left unwritten; the UI shows Missing).
     */
    fun verifyLegacyInPlace(context: Context, manifest: ModelManifest): File? {
        val entry = manifest.byId(manifest.defaultModelId) ?: return null
        val legacy = legacyModelsDir(context)?.let { File(it, entry.fileName) } ?: return null
        if (!legacy.canRead() || legacy.length() != entry.sizeBytes) return null
        val sha = sha256(FileInputStream(legacy))
        if (!sha.equals(entry.sha256, ignoreCase = true)) return null
        writeRecord(context, entry)
        return legacy
    }

    /** True when the app has a usable, verified model (no hashing at launch). */
    fun isReady(context: Context, manifest: ModelManifest): Boolean =
        resolve(context, manifest) != null || verifyLegacyInPlace(context, manifest) != null

    fun recordFile(context: Context): File = File(internalModelsDir(context), RECORD_FILE)

    fun writeRecord(context: Context, entry: ManifestEntry) {
        internalModelsDir(context).mkdirs()
        recordFile(context).writeText(
            JSONObject()
                .put("fileName", entry.fileName)
                .put("sha256", entry.sha256)
                .put("sizeBytes", entry.sizeBytes)
                .toString(),
        )
    }

    private fun readRecord(context: Context): JSONObject? = try {
        val f = recordFile(context)
        if (f.canRead()) JSONObject(f.readText()) else null
    } catch (_: Exception) {
        null
    }

    private fun recordMatches(record: JSONObject?, f: File, entry: ManifestEntry): Boolean {
        if (record == null) return false
        return record.optString("fileName") == entry.fileName &&
            record.optString("sha256").equals(entry.sha256, ignoreCase = true) &&
            record.optLong("sizeBytes", -1L) == entry.sizeBytes &&
            f.length() == entry.sizeBytes
    }

    fun sha256(input: FileInputStream): String {
        val md = MessageDigest.getInstance("SHA-256")
        input.use { ins ->
            val buf = ByteArray(1024 * 1024)
            while (true) {
                val n = ins.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
