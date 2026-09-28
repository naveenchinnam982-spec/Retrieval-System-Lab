package com.example.document

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

data class ProcessedChunk(
    val chunkIndex: Int,
    val text: String,
    val wordCount: Int,
    val pageNumber: Int = 1
)

data class ExtractedDocument(
    val title: String,
    val fileType: String,
    val fileSize: Long,
    val content: String
)

class DocumentProcessor(
    private val defaultChunkSize: Int = 100,
    private val defaultOverlap: Int = 20
) {

    fun cleanText(text: String): String {
        if (text.isBlank()) return ""
        // Remove non-printable control characters except standard whitespace
        val sanitized = text.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]"), "")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\t', ' ')
        // Collapse excessive newlines (>2) into double newlines
        val normalizedLines = sanitized.replace(Regex("(\\n\\s*){3,}"), "\n\n")
        // Collapse multiple spaces
        val collapsedSpaces = normalizedLines.replace(Regex("[ ]{2,}"), " ")
        return collapsedSpaces.trim()
    }

    fun chunkText(
        text: String,
        chunkSize: Int = defaultChunkSize,
        chunkOverlap: Int = defaultOverlap,
        pageNumber: Int = 1
    ): List<ProcessedChunk> {
        val cleaned = cleanText(text)
        if (cleaned.isBlank()) return emptyList()

        val size = if (chunkSize > 5) chunkSize else defaultChunkSize
        val overlap = if (chunkOverlap in 0 until size) chunkOverlap else (size / 5)
        val step = (size - overlap).coerceAtLeast(1)

        val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()

        val chunks = ArrayList<ProcessedChunk>()
        var chunkIndex = 0
        var startIdx = 0

        while (startIdx < words.size) {
            val endIdx = (startIdx + size).coerceAtMost(words.size)
            val chunkWords = words.subList(startIdx, endIdx)
            val chunkText = chunkWords.joinToString(" ")

            chunks.add(
                ProcessedChunk(
                    chunkIndex = chunkIndex++,
                    text = chunkText,
                    wordCount = chunkWords.size,
                    pageNumber = pageNumber
                )
            )

            if (endIdx >= words.size) break
            startIdx += step
        }

        return chunks
    }

    fun extractDocumentFromUri(context: Context, uri: Uri): ExtractedDocument {
        var fileName = "document.txt"
        var fileSize = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
            }
        }

        val extension = fileName.substringAfterLast('.', "").uppercase()
        val mimeType = context.contentResolver.getType(uri) ?: ""

        val isPdf = extension == "PDF" || mimeType.contains("pdf", ignoreCase = true)
        val extractedText = context.contentResolver.openInputStream(uri)?.use { stream ->
            if (isPdf) {
                extractPdfText(stream)
            } else {
                readPlainText(stream)
            }
        } ?: ""

        return ExtractedDocument(
            title = fileName,
            fileType = if (isPdf) "PDF" else "TXT",
            fileSize = if (fileSize > 0) fileSize else extractedText.toByteArray().size.toLong(),
            content = extractedText
        )
    }

    private fun readPlainText(inputStream: InputStream): String {
        return BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }

    private fun extractPdfText(inputStream: InputStream): String {
        // Robust pure-stream plain text extraction for PDF streams
        val bytes = inputStream.readBytes()
        val textParts = mutableListOf<String>()

        try {
            val content = String(bytes, Charsets.ISO_8859_1)
            // Match text inside BT (Begin Text) ... ET (End Text) blocks or Tj / TJ operators
            val tjPattern = Pattern.compile("\\(([^\\)]+)\\)\\s*(?:Tj|TJ)")
            val matcher = tjPattern.matcher(content)
            while (matcher.find()) {
                val matched = matcher.group(1)
                if (!matched.isNullOrBlank()) {
                    textParts.add(matched)
                }
            }
        } catch (_: Exception) { }

        if (textParts.isEmpty()) {
            // Fallback: extract continuous ASCII printable strings
            val asciiPattern = Pattern.compile("[\\x20-\\x7E\\s]{4,}")
            val matcher = asciiPattern.matcher(String(bytes, Charsets.ISO_8859_1))
            while (matcher.find()) {
                val group = matcher.group()
                if (group.isNotBlank() && !group.startsWith("%PDF")) {
                    textParts.add(group)
                }
            }
        }

        return textParts.joinToString(" ")
    }
}
