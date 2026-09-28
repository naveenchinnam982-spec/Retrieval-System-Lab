package com.example.embedding

interface EmbeddingService {
    val providerName: String
    val dimension: Int

    suspend fun generateEmbedding(text: String): FloatArray

    suspend fun generateEmbeddings(texts: List<String>): List<FloatArray> {
        return texts.map { generateEmbedding(it) }
    }
}
