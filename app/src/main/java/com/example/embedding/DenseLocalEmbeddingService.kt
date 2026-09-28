package com.example.embedding

import com.example.vector.VectorMath
import java.security.MessageDigest
import kotlin.math.ln1p

class DenseLocalEmbeddingService(
    override val dimension: Int = 384
) : EmbeddingService {

    override val providerName: String = "Dense Local Semantic (${dimension}-dim)"

    private val digestThreadLocal = ThreadLocal.withInitial {
        MessageDigest.getInstance("SHA-256")
    }

    private fun hashSubword(token: String, seed: Int): Int {
        val md = digestThreadLocal.get() ?: MessageDigest.getInstance("SHA-256")
        md.reset()
        md.update("$token:$seed".toByteArray(Charsets.UTF_8))
        val bytes = md.digest()
        return ((bytes[0].toInt() and 0xFF) shl 24) or
                ((bytes[1].toInt() and 0xFF) shl 16) or
                ((bytes[2].toInt() and 0xFF) shl 8) or
                (bytes[3].toInt() and 0xFF)
    }

    override suspend fun generateEmbedding(text: String): FloatArray {
        if (text.isBlank()) {
            return FloatArray(dimension)
        }

        val rawWords = text.split(Regex("\\s+"))
        val words = ArrayList<String>(rawWords.size)
        for (w in rawWords) {
            val cleaned = w.trim { it in ".,!?;:()[]{}\"'`~@#$%^&*-_+=/\\|<>" }.lowercase()
            if (cleaned.isNotEmpty()) {
                words.add(cleaned)
            }
        }

        if (words.isEmpty()) {
            return FloatArray(dimension)
        }

        val vector = FloatArray(dimension)

        // 1. Unigram & Morphological Subword Projection
        for (pos in words.indices) {
            val word = words[pos]
            val weight = (ln1p(word.length.toDouble()) * (1.0 + 0.1 / (1.0 + pos * 0.05))).toFloat()

            // Multi-seed projection for dense dimension coverage
            for (k in 0..2) {
                val hVal = hashSubword(word, seed = k * 1337)
                val idx = (hVal and 0x7FFFFFFF) % dimension
                val sign = if (((hVal ushr 16) and 1) == 0) 1.0f else -1.0f
                vector[idx] += sign * weight
            }

            // Character trigrams for morphological similarity (e.g., "learn" in "learning")
            if (word.length >= 3) {
                for (ci in 0..word.length - 3) {
                    val trigram = word.substring(ci, ci + 3)
                    val hTri = hashSubword(trigram, seed = 7919)
                    val idxTri = (hTri and 0x7FFFFFFF) % dimension
                    val signTri = if (((hTri ushr 16) and 1) == 0) 1.0f else -1.0f
                    vector[idxTri] += signTri * (weight * 0.4f)
                }
            }
        }

        // 2. Bigram Contextual Semantic Projection (captures phrases like "machine learning")
        for (i in 0 until words.size - 1) {
            val bigram = "${words[i]}_${words[i + 1]}"
            val hBi = hashSubword(bigram, seed = 9973)
            val idxBi = (hBi and 0x7FFFFFFF) % dimension
            val signBi = if (((hBi ushr 16) and 1) == 0) 1.0f else -1.0f
            vector[idxBi] += signBi * 1.5f
        }

        // 3. L2 Normalization so ||v|| = 1.0
        return VectorMath.l2Normalize(vector)
    }
}
