package com.example.repository

data class SearchResult(
    val chunkId: Long,
    val documentId: Long,
    val documentTitle: String,
    val chunkIndex: Int,
    val pageNumber: Int,
    val text: String,
    val wordCount: Int,
    val similarity: Float,
    var rank: Int = 1,
    val vectorDimension: Int
)

data class EvaluationMetrics(
    val query: String,
    val topK: Int,
    val retrievedCount: Int,
    val relevantCount: Int,
    val precisionAtK: Float,
    val recallAtK: Float,
    val hitRateAtK: Float,
    val mrr: Float,
    val results: List<SearchResult>
)

data class IndexingProgress(
    val stage: String,
    val current: Int,
    val total: Int,
    val isCompleted: Boolean = false,
    val error: String? = null
) {
    val percentage: Int
        get() = if (total > 0) ((current.toFloat() / total) * 100).toInt().coerceIn(0, 100) else 0
}
