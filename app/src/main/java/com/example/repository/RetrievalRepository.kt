package com.example.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChunkEntity
import com.example.data.local.entity.DocumentEntity
import com.example.data.local.entity.EvaluationRecordEntity
import com.example.document.DocumentProcessor
import com.example.document.SampleDocuments
import com.example.embedding.EmbeddingManager
import com.example.vector.VectorMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RetrievalRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context),
    private val embeddingManager: EmbeddingManager = EmbeddingManager(context),
    private val documentProcessor: DocumentProcessor = DocumentProcessor()
) {

    private val documentDao = database.documentDao()
    private val chunkDao = database.chunkDao()
    private val evaluationDao = database.evaluationDao()

    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()
    val documentCount: Flow<Int> = documentDao.getDocumentCount()
    val chunkCount: Flow<Int> = chunkDao.getChunkCount()
    val evaluationHistory: Flow<List<EvaluationRecordEntity>> = evaluationDao.getAllRecords()

    fun getEmbeddingManager(): EmbeddingManager = embeddingManager

    suspend fun getDocumentCountSync(): Int = withContext(Dispatchers.IO) {
        documentDao.getDocumentCountSync()
    }

    suspend fun getChunkCountSync(): Int = withContext(Dispatchers.IO) {
        chunkDao.getChunkCountSync()
    }

    suspend fun getChunksForDocument(docId: Long): List<ChunkEntity> = withContext(Dispatchers.IO) {
        chunkDao.getChunksForDocument(docId)
    }

    suspend fun getDocumentById(docId: Long): DocumentEntity? = withContext(Dispatchers.IO) {
        documentDao.getDocumentById(docId)
    }

    suspend fun deleteDocument(docId: Long) = withContext(Dispatchers.IO) {
        documentDao.deleteDocumentById(docId)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        chunkDao.deleteAllChunks()
        documentDao.deleteAllDocuments()
        evaluationDao.clearRecords()
    }

    suspend fun indexDocumentText(
        title: String,
        text: String,
        fileType: String = "TXT",
        fileSize: Long = 0L,
        chunkSize: Int = embeddingManager.getDefaultChunkSize(),
        chunkOverlap: Int = embeddingManager.getDefaultOverlap(),
        onProgress: (IndexingProgress) -> Unit = {}
    ): Result<DocumentEntity> = withContext(Dispatchers.IO) {
        try {
            onProgress(IndexingProgress(stage = "Cleaning text...", current = 1, total = 5))
            val cleaned = documentProcessor.cleanText(text)
            if (cleaned.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Document text is empty or blank."))
            }

            onProgress(IndexingProgress(stage = "Splitting into chunks...", current = 2, total = 5))
            val chunks = documentProcessor.chunkText(
                text = cleaned,
                chunkSize = chunkSize,
                chunkOverlap = chunkOverlap
            )

            if (chunks.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No valid text chunks could be extracted."))
            }

            val embeddingService = embeddingManager.getActiveService()
            onProgress(
                IndexingProgress(
                    stage = "Generating embeddings (${embeddingService.providerName})...",
                    current = 3,
                    total = 5
                )
            )

            val actualFileSize = if (fileSize > 0) fileSize else text.toByteArray(Charsets.UTF_8).size.toLong()

            // 1. Insert Document Entity
            val docEntity = DocumentEntity(
                title = title.ifBlank { "Untitled_Document.txt" },
                fileType = fileType,
                fileSize = actualFileSize,
                chunkCount = chunks.size,
                vectorDimension = embeddingService.dimension,
                embeddingProvider = embeddingService.providerName,
                status = "Indexed"
            )
            val docId = documentDao.insertDocument(docEntity)

            // 2. Compute embeddings and store chunks
            val chunkEntities = ArrayList<ChunkEntity>(chunks.size)
            for (i in chunks.indices) {
                val chunk = chunks[i]
                onProgress(
                    IndexingProgress(
                        stage = "Embedding chunk ${i + 1}/${chunks.size}...",
                        current = i + 1,
                        total = chunks.size
                    )
                )
                val vector = embeddingService.generateEmbedding(chunk.text)
                val norm = VectorMath.l2Norm(vector)

                chunkEntities.add(
                    ChunkEntity(
                        documentId = docId,
                        chunkIndex = chunk.chunkIndex,
                        text = chunk.text,
                        wordCount = chunk.wordCount,
                        pageNumber = chunk.pageNumber,
                        embedding = vector,
                        l2Norm = norm
                    )
                )
            }

            onProgress(IndexingProgress(stage = "Storing vectors in database...", current = 4, total = 5))
            chunkDao.insertChunks(chunkEntities)

            val finalDoc = docEntity.copy(id = docId)
            onProgress(
                IndexingProgress(
                    stage = "Document indexed successfully (${chunks.size} chunks)",
                    current = 5,
                    total = 5,
                    isCompleted = true
                )
            )

            Result.success(finalDoc)
        } catch (e: Exception) {
            onProgress(IndexingProgress(stage = "Error: ${e.message}", current = 0, total = 1, error = e.message))
            Result.failure(e)
        }
    }

    suspend fun indexDocumentUri(
        uri: Uri,
        chunkSize: Int = embeddingManager.getDefaultChunkSize(),
        chunkOverlap: Int = embeddingManager.getDefaultOverlap(),
        onProgress: (IndexingProgress) -> Unit = {}
    ): Result<DocumentEntity> = withContext(Dispatchers.IO) {
        try {
            onProgress(IndexingProgress(stage = "Reading document file...", current = 1, total = 6))
            val extracted = documentProcessor.extractDocumentFromUri(context, uri)

            if (extracted.content.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Could not extract readable text from file '${extracted.title}'.")
                )
            }

            indexDocumentText(
                title = extracted.title,
                text = extracted.content,
                fileType = extracted.fileType,
                fileSize = extracted.fileSize,
                chunkSize = chunkSize,
                chunkOverlap = chunkOverlap,
                onProgress = onProgress
            )
        } catch (e: Exception) {
            onProgress(IndexingProgress(stage = "Error reading file", current = 0, total = 1, error = e.message))
            Result.failure(e)
        }
    }

    suspend fun loadBenchmarkDocuments(
        onProgress: (IndexingProgress) -> Unit = {}
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val papers = SampleDocuments.BENCHMARK_PAPERS
            for (idx in papers.indices) {
                val paper = papers[idx]
                onProgress(
                    IndexingProgress(
                        stage = "Indexing benchmark paper ${idx + 1}/${papers.size}: ${paper.title}...",
                        current = idx + 1,
                        total = papers.size
                    )
                )
                indexDocumentText(
                    title = paper.title,
                    text = paper.text,
                    fileType = paper.fileType,
                    chunkSize = 60,
                    chunkOverlap = 15
                )
            }
            onProgress(IndexingProgress(stage = "Benchmark library ready", current = papers.size, total = papers.size, isCompleted = true))
            Result.success(papers.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun search(
        query: String,
        topK: Int = 5,
        threshold: Float = 0.0f,
        filterDocId: Long? = null
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return@withContext emptyList()

        val embeddingService = embeddingManager.getActiveService()
        val queryVector = embeddingService.generateEmbedding(trimmedQuery)

        // Retrieve chunks and compute real cosine similarities
        val allChunks = if (filterDocId != null && filterDocId > 0) {
            chunkDao.getChunksForDocumentSync(filterDocId)
        } else {
            chunkDao.getAllChunksWithEmbeddings()
        }

        if (allChunks.isEmpty()) return@withContext emptyList()

        // Fetch documents for titles
        val docMap = mutableMapOf<Long, String>()

        val rawResults = ArrayList<SearchResult>()
        for (chunk in allChunks) {
            if (chunk.embedding.size != queryVector.size) {
                // If dimension differs (e.g. from previously indexed provider), skip or calculate on common size
                continue
            }
            val similarity = VectorMath.cosineSimilarity(queryVector, chunk.embedding)
            if (similarity >= threshold) {
                val docTitle = docMap.getOrPut(chunk.documentId) {
                    documentDao.getDocumentById(chunk.documentId)?.title ?: "Document #${chunk.documentId}"
                }
                rawResults.add(
                    SearchResult(
                        chunkId = chunk.id,
                        documentId = chunk.documentId,
                        documentTitle = docTitle,
                        chunkIndex = chunk.chunkIndex,
                        pageNumber = chunk.pageNumber,
                        text = chunk.text,
                        wordCount = chunk.wordCount,
                        similarity = (similarity * 10000).toInt() / 10000.0f,
                        vectorDimension = chunk.embedding.size
                    )
                )
            }
        }

        // Sort descending by vector cosine similarity
        rawResults.sortByDescending { it.similarity }

        val topResults = rawResults.take(topK)
        topResults.forEachIndexed { index, item ->
            item.rank = index + 1
        }
        topResults
    }

    suspend fun evaluateRetrieval(
        query: String,
        topK: Int,
        relevantChunkIds: Set<Long>,
        totalKnownRelevantInCorpus: Int = 0
    ): EvaluationMetrics = withContext(Dispatchers.IO) {
        val results = search(query = query, topK = topK, threshold = -1.0f)
        var relevantRetrieved = 0
        var firstRelevantRank = 0

        for (i in results.indices) {
            val res = results[i]
            if (relevantChunkIds.contains(res.chunkId)) {
                relevantRetrieved++
                if (firstRelevantRank == 0) {
                    firstRelevantRank = i + 1
                }
            }
        }

        val k = results.size.coerceAtLeast(1)
        val precisionAtK = relevantRetrieved.toFloat() / k
        val hitRateAtK = if (relevantRetrieved > 0) 1.0f else 0.0f
        val mrr = if (firstRelevantRank > 0) (1.0f / firstRelevantRank) else 0.0f

        val totalRelevant = if (totalKnownRelevantInCorpus > 0) totalKnownRelevantInCorpus else relevantRetrieved.coerceAtLeast(1)
        val recallAtK = relevantRetrieved.toFloat() / totalRelevant

        val metrics = EvaluationMetrics(
            query = query,
            topK = topK,
            retrievedCount = results.size,
            relevantCount = relevantRetrieved,
            precisionAtK = (precisionAtK * 1000).toInt() / 1000.0f,
            recallAtK = (recallAtK * 1000).toInt() / 1000.0f,
            hitRateAtK = hitRateAtK,
            mrr = (mrr * 1000).toInt() / 1000.0f,
            results = results
        )

        // Save evaluation record to Room DB
        evaluationDao.insertRecord(
            EvaluationRecordEntity(
                queryText = query,
                topK = topK,
                retrievedCount = results.size,
                relevantCount = relevantRetrieved,
                precisionAtK = metrics.precisionAtK,
                hitRateAtK = metrics.hitRateAtK,
                mrr = metrics.mrr
            )
        )

        metrics
    }
}
