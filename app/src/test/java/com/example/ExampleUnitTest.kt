package com.example

import com.example.document.DocumentProcessor
import com.example.embedding.DenseLocalEmbeddingService
import com.example.vector.VectorMath
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testVectorMathCosineSimilarity() {
        val vecA = floatArrayOf(1.0f, 0.0f, 0.0f)
        val vecB = floatArrayOf(1.0f, 0.0f, 0.0f)
        val vecOrthogonal = floatArrayOf(0.0f, 1.0f, 0.0f)

        val simIdentical = VectorMath.cosineSimilarity(vecA, vecB)
        val simOrthogonal = VectorMath.cosineSimilarity(vecA, vecOrthogonal)

        assertEquals(1.0f, simIdentical, 0.001f)
        assertEquals(0.0f, simOrthogonal, 0.001f)
    }

    @Test
    fun testDocumentProcessorChunking() {
        val processor = DocumentProcessor(defaultChunkSize = 10, defaultOverlap = 3)
        val text = "One two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen eighteen nineteen twenty."
        val chunks = processor.chunkText(text, chunkSize = 10, chunkOverlap = 3)

        assertTrue(chunks.size >= 2)
        assertEquals(10, chunks[0].wordCount)
        assertTrue(chunks[0].text.startsWith("One two three"))
    }

    @Test
    fun testDenseLocalEmbeddingService() = runBlocking {
        val service = DenseLocalEmbeddingService(dimension = 384)
        val embedding1 = service.generateEmbedding("artificial intelligence machine learning")
        val embedding2 = service.generateEmbedding("machine learning deep learning neural network")
        val embedding3 = service.generateEmbedding("baking chocolate cake recipes kitchen oven")

        assertEquals(384, embedding1.size)
        assertEquals(384, embedding2.size)

        val norm = VectorMath.l2Norm(embedding1)
        assertEquals(1.0f, norm, 0.01f)

        val simRelated = VectorMath.cosineSimilarity(embedding1, embedding2)
        val simUnrelated = VectorMath.cosineSimilarity(embedding1, embedding3)

        assertTrue("Related texts must have higher cosine similarity", simRelated > simUnrelated)
    }
}
