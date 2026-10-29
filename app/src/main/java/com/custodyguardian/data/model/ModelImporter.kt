package com.custodyguardian.data.model

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Core model import: single-pass copy + SHA-256 verify, atomic `.part` rename.
 *
 * Pure JVM (no Android imports) so the full matrix is unit-testable. The
 * Android layer supplies the content [InputStream] and performs the optional
 * SAF source deletion after success.
 *
 * Guarantees:
 *  - destination file name comes from the manifest entry, never the source name
 *  - copy aborts the moment written bytes exceed entry.sizeBytes (lying-header guard)
 *  - size mismatch or hash mismatch never leaves a final file behind
 *  - a failed/cancelled import always deletes the `.part` file
 */
object ModelImporter {

    private const val BUFFER = 1024 * 1024

    sealed class ImportResult {
        /** Import verified and renamed into place. [file] is the final model file. */
        data class Ok(val file: File, val entry: ManifestEntry) : ImportResult()

        /** Bytes read do not match entry.sizeBytes (truncated download or oversize). */
        data class Incomplete(val expected: Long, val got: Long) : ImportResult()

        /** Full file, wrong content: sha256 does not match the pinned value. */
        data class HashMismatch(val expected: String, val got: String) : ImportResult()

        /** Leading magic bytes are neither GGUF nor PK. */
        object NotAModel : ImportResult()

        /** Zip contained no *.gguf entry. */
        object NoModelInZip : ImportResult()

        /** The underlying stream failed mid-read. */
        data class SourceUnreadable(val cause: Exception) : ImportResult()
    }

    private class Resolved(val result: ImportResult) : Exception()

    /** First 4 bytes of any GGUF container: "GGUF". */
    const val GGUF_MAGIC = "GGUF"

    /** First 2 bytes of any zip container: "PK". */
    const val ZIP_MAGIC = "PK"

    fun looksLikeGguf(head: ByteArray): Boolean =
        head.size >= 4 && String(head, 0, 4, Charsets.US_ASCII) == GGUF_MAGIC

    fun looksLikeZip(head: ByteArray): Boolean =
        head.size >= 2 && String(head, 0, 2, Charsets.US_ASCII) == ZIP_MAGIC

    /**
     * Imports from [openStream] into [modelsDir], verifying against [entry].
     * [onProgress] receives (bytesWritten, totalBytes) at coarse granularity;
     * [ensureActive] lets the caller cancel.
     */
    fun import(
        openStream: () -> InputStream,
        entry: ManifestEntry,
        modelsDir: File,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
        ensureActive: () -> Unit = {},
    ): ImportResult {
        modelsDir.mkdirs()
        val part = File(modelsDir, entry.fileName + ".part")
        val final = File(modelsDir, entry.fileName)
        try {
            part.outputStream().use { out ->
                val md = MessageDigest.getInstance("SHA-256")
                val buf = ByteArray(BUFFER)
                var written = 0L
                var lastReport = 0L

                openStream().use { input ->
                    // Peek up to 4 magic bytes without committing to a mode.
                    val head = ByteArray(4)
                    var headFill = 0
                    while (headFill < 4) {
                        val n = input.read(head, headFill, 4 - headFill)
                        if (n < 0) break
                        headFill += n
                    }
                    if (headFill == 0) {
                        return ImportResult.SourceUnreadable(IOException("empty stream"))
                    }

                    if (looksLikeGguf(head)) {
                        // Write the peeked magic bytes FIRST (full-repo review
                        // 2026-10-29, both reviewers): they were hashed and
                        // counted but never written — the imported file was 4
                        // bytes short yet passed size+sha256 and reported Ok.
                        out.write(head, 0, headFill)
                        md.update(head, 0, headFill)
                        written += headFill
                        checkSize(written, entry)
                        val rest = drain(input, out, md, written, entry, onProgress, ensureActive) {
                            lastReport = it
                        }
                        written = rest
                    } else if (looksLikeZip(head)) {
                        // The replaying stream serves the peeked bytes first.
                        val replay = object : InputStream() {
                            override fun read(): Int = error("unused")
                            override fun read(b: ByteArray, off: Int, len: Int): Int {
                                if (headFill > 0) {
                                    val n = minOf(headFill, len)
                                    System.arraycopy(head, 0, b, off, n)
                                    System.arraycopy(head, n, head, 0, headFill - n)
                                    headFill -= n
                                    return n
                                }
                                return input.read(b, off, len)
                            }
                        }
                        val zis = ZipInputStream(replay)
                        var target: ZipEntry? = null
                        var ze: ZipEntry? = zis.nextEntry
                        while (ze != null) {
                            if (!ze.isDirectory &&
                                (ze.name == entry.fileName ||
                                    ze.name.endsWith(".gguf", ignoreCase = true))
                            ) {
                                // Stop immediately: the next nextEntry() would skip
                                // this entry's data.
                                target = ze
                                break
                            }
                            ze = zis.nextEntry
                        }
                        if (target == null) return ImportResult.NoModelInZip
                        written = drain(zis, out, md, 0L, entry, onProgress, ensureActive) {
                            lastReport = it
                        }
                    } else {
                        return ImportResult.NotAModel
                    }
                }

                if (written != entry.sizeBytes) {
                    return ImportResult.Incomplete(entry.sizeBytes, written)
                }
                val got = md.digest().joinToString("") { "%02x".format(it) }
                if (!got.equals(entry.sha256, ignoreCase = true)) {
                    return ImportResult.HashMismatch(entry.sha256, got)
                }
            }
            if (!part.renameTo(final)) {
                part.copyTo(final, overwrite = true)
                part.delete()
            }
            return ImportResult.Ok(final, entry)
        } catch (r: Resolved) {
            part.delete()
            return r.result
        } catch (e: IOException) {
            part.delete()
            return ImportResult.SourceUnreadable(e)
        } finally {
            if (part.exists()) part.delete()
        }
    }

    private fun checkSize(written: Long, entry: ManifestEntry) {
        if (written > entry.sizeBytes) {
            throw Resolved(ImportResult.Incomplete(entry.sizeBytes, written))
        }
    }

    /** Copies until EOF; throws [Resolved] on oversize. Returns total bytes written. */
    private fun drain(
        input: InputStream,
        out: java.io.OutputStream,
        md: MessageDigest,
        base: Long,
        entry: ManifestEntry,
        onProgress: (Long, Long) -> Unit,
        ensureActive: () -> Unit,
        setLastReport: (Long) -> Unit,
    ): Long {
        var lastReport = 0L
        val buf = ByteArray(BUFFER)
        var written = base
        while (true) {
            ensureActive()
            val n = input.read(buf)
            if (n < 0) break
            if (n == 0) continue
            md.update(buf, 0, n)
            out.write(buf, 0, n)
            written += n
            checkSize(written, entry)
            if (written - base - lastReport >= 256 * 1024) {
                onProgress(written, entry.sizeBytes)
                lastReport = written - base
                setLastReport(lastReport)
            }
        }
        return written
    }
}
