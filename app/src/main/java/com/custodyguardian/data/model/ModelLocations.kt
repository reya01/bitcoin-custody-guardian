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

    /**
     * The model file to load, or null when absent. Since the one-time verified
     * migration (migrateLegacyModel), normal resolution is internal-only; the
     * legacy external path is consulted by the migration itself.
     */
    fun resolve(context: Context, manifest: ModelManifest): File? {
        val entry = manifest.byId(manifest.defaultModelId) ?: return null
        val record = readRecord(context)
        val internal = File(internalModelsDir(context), entry.fileName)
        if (internal.canRead() && recordMatches(record, internal, entry)) return internal
        return null
    }

    sealed class MigrationResult {
        /** No legacy file present — nothing to do. */
        object Noop : MigrationResult()

        /** Legacy file copied into internal storage, hash-verified, legacy deleted. */
        data class Migrated(val file: File) : MigrationResult()

        /** Internal copy already valid; stale legacy file removed. */
        object AlreadyMigrated : MigrationResult()

        /** Not enough internal space for the one-time copy; legacy kept (warned). */
        data class NeedsSpace(val needed: Long, val available: Long) : MigrationResult()

        /** Legacy file hash != pinned manifest hash — refused, nothing accepted. */
        object Tampered : MigrationResult()

        /** Copy/IO failure; legacy kept. */
        data class CopyFailed(val cause: Exception) : MigrationResult()
    }

    /**
     * One-time verified migration of the v0.1.0 adb-sideloaded model from the
     * app's external files dir (writable via adb / All-Files-Access) into
     * internal storage. SHA-256 is computed during the copy itself, so a
     * same-size tampered replacement is refused (weekly-audit Medium fix).
     * After a successful migration the legacy path is never read again.
     */
    fun migrateLegacyModel(context: Context, manifest: ModelManifest): MigrationResult {
        val entry = manifest.byId(manifest.defaultModelId) ?: return MigrationResult.Noop
        val legacy = legacyModelsDir(context)?.let { File(it, entry.fileName) }
            ?: return MigrationResult.Noop
        if (!legacy.canRead()) return MigrationResult.Noop

        val internalDir = internalModelsDir(context)
        val final = File(internalDir, entry.fileName)
        // Idempotent: a valid internal copy makes the legacy file a leftover.
        if (final.canRead() && recordMatches(readRecord(context), final, entry)) {
            legacy.delete()
            return MigrationResult.AlreadyMigrated
        }
        // Clean a stale .part from a prior crash.
        File(internalDir, entry.fileName + ".part").delete()

        internalDir.mkdirs()
        val needed = legacy.length()
        val available = internalDir.usableSpace
        if (available < needed + 64L * 1024 * 1024) {
            return MigrationResult.NeedsSpace(needed, available)
        }

        val part = File(internalDir, entry.fileName + ".part")
        return try {
            val sha = FileInputStream(legacy).use { input ->
                val md = java.security.MessageDigest.getInstance("SHA-256")
                part.outputStream().use { out ->
                    val buf = ByteArray(1024 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        md.update(buf, 0, n)
                        out.write(buf, 0, n)
                    }
                }
                md.digest().joinToString("") { "%02x".format(it) }
            }
            if (!sha.equals(entry.sha256, ignoreCase = true)) {
                part.delete()
                return MigrationResult.Tampered
            }
            if (!part.renameTo(final)) {
                part.copyTo(final, overwrite = true)
                part.delete()
            }
            writeRecord(context, entry)
            legacy.delete()
            MigrationResult.Migrated(final)
        } catch (e: java.io.IOException) {
            part.delete()
            MigrationResult.CopyFailed(e)
        }
    }

    /**
     * True when the app has a usable, verified model. Runs the one-time legacy
     * migration when an external-file copy exists (normal launches never
     * re-hash — record + exact size only).
     */
    fun isReady(context: Context, manifest: ModelManifest): Boolean {
        if (resolve(context, manifest) != null) return true
        return when (migrateLegacyModel(context, manifest)) {
            is MigrationResult.Migrated -> true
            is MigrationResult.AlreadyMigrated -> true
            else -> false
        }
    }

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
