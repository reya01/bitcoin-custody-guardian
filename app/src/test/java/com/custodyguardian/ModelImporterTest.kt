package com.custodyguardian

import com.custodyguardian.data.model.ManifestEntry
import com.custodyguardian.data.model.ModelImporter
import com.custodyguardian.data.model.ModelManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.SequenceInputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Full JVM matrix for the model import core: bare GGUF, zip ingestion, and every
 * failure mode (truncated, oversize, hash mismatch, wrong file, empty zip).
 */
class ModelImporterTest {

    private fun entry(sizeBytes: Long, sha256: String) = ManifestEntry(
        id = "test-model",
        fileName = "test-model.gguf",
        sha256 = sha256,
        sizeBytes = sizeBytes,
        minRamMb = 8192,
        ciDownloadUrl = "",
    )

    private fun gguf(content: ByteArray): ByteArray = "GGUF".toByteArray() + content
    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun tempDir(): File =
        File.createTempFile("models", "dir").apply { delete(); mkdirs() }

    private fun zipBytes(entries: Map<String, ByteArray>): ByteArray {
        val bos = java.io.ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            for ((name, data) in entries) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(data)
                zos.closeEntry()
            }
        }
        return bos.toByteArray()
    }

    @Test
    fun manifest_parse_ok() {
        val m = ModelManifest.parse(
            """{"schemaVersion":1,"defaultModelId":"a","models":[
               {"id":"a","fileName":"a.gguf","sha256":"${"0".repeat(64)}",
                "sizeBytes":10,"minRamMb":8192,"ciDownloadUrl":"https://x/y"}]}""".trimIndent(),
        )
        assertEquals(1, m.models.size)
        assertEquals("a.gguf", m.byId("a")!!.fileName)
    }

    @Test(expected = IllegalArgumentException::class)
    fun manifest_missing_field_fails_closed() {
        ModelManifest.parse(
            """{"schemaVersion":1,"defaultModelId":"a","models":[
               {"id":"a","fileName":"a.gguf","sha256":"short","sizeBytes":10}]}""".trimIndent(),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun manifest_bad_default_fails_closed() {
        ModelManifest.parse(
            """{"schemaVersion":1,"defaultModelId":"zz","models":[
               {"id":"a","fileName":"a.gguf","sha256":"${"0".repeat(64)}","sizeBytes":10}]}""".trimIndent(),
        )
    }

    @Test
    fun bare_gguf_ok_and_renamed_from_manifest() {
        val content = ByteArray(2048) { it.toByte() }
        val bytes = gguf(content)
        val dir = tempDir()
        val r = ModelImporter.import(
            { ByteArrayInputStream(bytes) },
            entry(bytes.size.toLong(), sha256(bytes)),
            dir,
        )
        assertTrue("expected Ok, got $r", r is ModelImporter.ImportResult.Ok)
        assertEquals("test-model.gguf", (r as ModelImporter.ImportResult.Ok).file.name)
        assertTrue(File(dir, "test-model.gguf").canRead())
        assertFalse(File(dir, "test-model.gguf.part").exists())
    }

    @Test
    fun bare_gguf_import_is_byte_identical_round_trip() {
        // Full-repo review 2026-10-29 regression: the GGUF branch hashed and
        // counted the 4 peeked magic bytes but never WROTE them — the imported
        // file was 4 bytes short yet passed size+sha256 and reported Ok.
        val bytes = gguf(ByteArray(2048) { (it * 31).toByte() })
        val dir = tempDir()
        val r = ModelImporter.import(
            { ByteArrayInputStream(bytes) },
            entry(bytes.size.toLong(), sha256(bytes)),
            dir,
        )
        assertTrue("expected Ok, got $r", r is ModelImporter.ImportResult.Ok)
        val imported = File(dir, "test-model.gguf").readBytes()
        assertTrue(
            "imported file must be byte-identical to the source",
            imported.contentEquals(bytes),
        )
    }

    @Test
    fun truncated_download_reports_incomplete() {
        val bytes = gguf(ByteArray(4096))
        val dir = tempDir()
        val r = ModelImporter.import(
            { ByteArrayInputStream(bytes.copyOf(bytes.size - 100)) },
            entry(bytes.size.toLong(), sha256(bytes)),
            dir,
        )
        assertTrue(r is ModelImporter.ImportResult.Incomplete)
        assertFalse(File(dir, "test-model.gguf").exists())
        assertFalse(File(dir, "test-model.gguf.part").exists())
    }

    @Test
    fun hash_mismatch_reports_and_leaves_nothing() {
        val bytes = gguf(ByteArray(2048))
        val dir = tempDir()
        val wrongSha = sha256(gguf(ByteArray(2048) { 7 }))
        val r = ModelImporter.import(
            { ByteArrayInputStream(bytes) },
            entry(bytes.size.toLong(), wrongSha),
            dir,
        )
        assertTrue(r is ModelImporter.ImportResult.HashMismatch)
        assertFalse(File(dir, "test-model.gguf").exists())
    }

    @Test
    fun wrong_magic_reports_not_a_model() {
        val dir = tempDir()
        val r = ModelImporter.import(
            { ByteArrayInputStream("JUNKJUNKJUNK".toByteArray()) },
            entry(12, "0".repeat(64)),
            dir,
        )
        assertTrue(r is ModelImporter.ImportResult.NotAModel)
    }

    @Test
    fun empty_zip_reports_no_model() {
        val bytes = zipBytes(mapOf("readme.txt" to "hello".toByteArray()))
        val dir = tempDir()
        val r = ModelImporter.import(
            { ByteArrayInputStream(bytes) },
            entry(0, sha256(bytes)),
            dir,
        )
        assertTrue(r is ModelImporter.ImportResult.NoModelInZip)
    }

    @Test
    fun zip_with_model_extracts_and_verifies() {
        val model = gguf(ByteArray(3000))
        val bytes = zipBytes(
            mapOf(
                "readme.txt" to "bundle".toByteArray(),
                "qwen3-1.7b-q4_k_m.gguf" to model,
            ),
        )
        val dir = tempDir()
        val r = ModelImporter.import(
            { ByteArrayInputStream(bytes) },
            entry(model.size.toLong(), sha256(model)),
            dir,
        )
        assertTrue("expected Ok, got $r", r is ModelImporter.ImportResult.Ok)
        assertEquals("test-model.gguf", (r as ModelImporter.ImportResult.Ok).file.name)
        // Content of the extracted file equals the model.
        assertTrue(File(dir, "test-model.gguf").readBytes().contentEquals(model))
    }

    @Test
    fun oversize_aborts_immediately() {
        // Manifest expects 10 bytes; the stream is a full GGUF with far more.
        val bytes = gguf(ByteArray(8192))
        val dir = tempDir()
        val r = ModelImporter.import(
            { ByteArrayInputStream(bytes) },
            entry(10, "0".repeat(64)),
            dir,
        )
        assertTrue(r is ModelImporter.ImportResult.Incomplete)
        assertFalse(File(dir, "test-model.gguf").exists())
    }

    @Test
    fun stream_read_failure_reports_source_unreadable() {
        val dir = tempDir()
        val failing = object : java.io.InputStream() {
            override fun read(): Int = throw java.io.IOException("disk gone")
        }
        val r = ModelImporter.import({ failing }, entry(100, "0".repeat(64)), dir)
        assertTrue(r is ModelImporter.ImportResult.SourceUnreadable)
    }

    @Test
    fun magic_byte_helpers() {
        assertTrue(ModelImporter.looksLikeGguf("GGUFxx".toByteArray()))
        assertTrue(ModelImporter.looksLikeZip("PK\u0003\u0004".toByteArray()))
        assertFalse(ModelImporter.looksLikeGguf("PK\u0003\u0004".toByteArray()))
        assertFalse(ModelImporter.looksLikeZip("GGUF".toByteArray()))
    }

    @Test
    fun large_stream_matches_file_hash_path() {
        // Simulate a multi-chunk stream (via a real temp file) to exercise the
        // drain loop with progress reporting.
        val model = gguf(ByteArray(3 * 1024 * 1024 + 17))
        val dir = tempDir()
        var lastPct = -1
        val tmp = File.createTempFile("model", "gguf")
        tmp.writeBytes(model)
        val r2 = ModelImporter.import(
            { FileInputStream(tmp) },
            entry(model.size.toLong(), sha256(model)),
            dir,
            onProgress = { done, total -> lastPct = ((done * 100) / total).toInt() },
        )
        tmp.delete()
        assertTrue("expected Ok, got $r2", r2 is ModelImporter.ImportResult.Ok)
        assertTrue(lastPct in 0..100)
    }
}
