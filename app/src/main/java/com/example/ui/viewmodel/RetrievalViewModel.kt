package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ChunkEntity
import com.example.data.local.entity.DocumentEntity
import com.example.embedding.EmbeddingManager
import com.example.repository.EvaluationMetrics
import com.example.repository.IndexingProgress
import com.example.repository.RetrievalRepository
import com.example.repository.SearchResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RetrievalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RetrievalRepository(application.applicationContext)
    val embeddingManager: EmbeddingManager = repository.getEmbeddingManager()

    val documents: StateFlow<List<DocumentEntity>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val documentCount: StateFlow<Int> = repository.documentCount
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val chunkCount: StateFlow<Int> = repository.chunkCount
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _indexingProgress = MutableStateFlow<IndexingProgress?>(null)
    val indexingProgress: StateFlow<IndexingProgress?> = _indexingProgress.asStateFlow()

    private val _evaluationMetrics = MutableStateFlow<EvaluationMetrics?>(null)
    val evaluationMetrics: StateFlow<EvaluationMetrics?> = _evaluationMetrics.asStateFlow()

    private val _activeEngineName = MutableStateFlow(embeddingManager.getActiveService().providerName)
    val activeEngineName: StateFlow<String> = _activeEngineName.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun refreshEngineStatus() {
        _activeEngineName.value = embeddingManager.getActiveService().providerName
    }

    fun indexFileUri(uri: Uri, chunkSize: Int, overlap: Int) {
        viewModelScope.launch {
            val result = repository.indexDocumentUri(
                uri = uri,
                chunkSize = chunkSize,
                chunkOverlap = overlap,
                onProgress = { progress -> _indexingProgress.value = progress }
            )
            result.fold(
                onSuccess = { doc ->
                    _messages.emit("Document '${doc.title}' indexed successfully (${doc.chunkCount} chunks)")
                },
                onFailure = { error ->
                    _messages.emit("Indexing failed: ${error.message}")
                }
            )
        }
    }

    fun indexRawText(title: String, text: String, chunkSize: Int, overlap: Int) {
        viewModelScope.launch {
            val result = repository.indexDocumentText(
                title = title,
                text = text,
                chunkSize = chunkSize,
                chunkOverlap = overlap,
                onProgress = { progress -> _indexingProgress.value = progress }
            )
            result.fold(
                onSuccess = { doc ->
                    _messages.emit("Document '${doc.title}' indexed successfully (${doc.chunkCount} chunks)")
                },
                onFailure = { error ->
                    _messages.emit("Indexing failed: ${error.message}")
                }
            )
        }
    }

    fun loadBenchmarkPapers() {
        viewModelScope.launch {
            _indexingProgress.value = IndexingProgress(stage = "Loading benchmark documents...", current = 1, total = 4)
            val result = repository.loadBenchmarkDocuments(
                onProgress = { progress -> _indexingProgress.value = progress }
            )
            result.fold(
                onSuccess = { count ->
                    _messages.emit("Successfully indexed $count benchmark research documents!")
                },
                onFailure = { error ->
                    _messages.emit("Failed to load benchmarks: ${error.message}")
                }
            )
        }
    }

    fun search(query: String, topK: Int = 5, threshold: Float = 0.0f, filterDocId: Long? = null) {
        if (query.isBlank()) {
            viewModelScope.launch { _messages.emit("Please enter a search query") }
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val results = repository.search(
                    query = query,
                    topK = topK,
                    threshold = threshold,
                    filterDocId = filterDocId
                )
                _searchResults.value = results
                if (results.isEmpty()) {
                    _messages.emit("No relevant chunks found above similarity threshold ($threshold)")
                }
            } catch (e: Exception) {
                _messages.emit("Search error: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun runEvaluation(query: String, topK: Int = 3, relevantIds: Set<Long>) {
        if (query.isBlank()) {
            viewModelScope.launch { _messages.emit("Please enter a query to evaluate") }
            return
        }
        viewModelScope.launch {
            try {
                val metrics = repository.evaluateRetrieval(
                    query = query,
                    topK = topK,
                    relevantChunkIds = relevantIds
                )
                _evaluationMetrics.value = metrics
            } catch (e: Exception) {
                _messages.emit("Evaluation error: ${e.message}")
            }
        }
    }

    fun recalculateLiveMetrics(
        query: String,
        topK: Int,
        relevantChunkIds: Set<Long>,
        currentResults: List<SearchResult>
    ) {
        var relevantRetrieved = 0
        var firstRelevantRank = 0

        for (i in currentResults.indices) {
            val res = currentResults[i]
            if (relevantChunkIds.contains(res.chunkId)) {
                relevantRetrieved++
                if (firstRelevantRank == 0) {
                    firstRelevantRank = i + 1
                }
            }
        }

        val k = currentResults.size.coerceAtLeast(1)
        val precisionAtK = relevantRetrieved.toFloat() / k
        val hitRateAtK = if (relevantRetrieved > 0) 1.0f else 0.0f
        val mrr = if (firstRelevantRank > 0) (1.0f / firstRelevantRank) else 0.0f
        val recallAtK = if (relevantRetrieved > 0) (relevantRetrieved.toFloat() / relevantRetrieved) else 0.0f

        _evaluationMetrics.value = EvaluationMetrics(
            query = query,
            topK = topK,
            retrievedCount = currentResults.size,
            relevantCount = relevantRetrieved,
            precisionAtK = (precisionAtK * 1000).toInt() / 1000.0f,
            recallAtK = (recallAtK * 1000).toInt() / 1000.0f,
            hitRateAtK = hitRateAtK,
            mrr = (mrr * 1000).toInt() / 1000.0f,
            results = currentResults
        )
    }

    fun deleteDocument(docId: Long) {
        viewModelScope.launch {
            repository.deleteDocument(docId)
            _messages.emit("Document and its vector embeddings deleted.")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _searchResults.value = emptyList()
            _evaluationMetrics.value = null
            _messages.emit("Database and vector store cleared.")
        }
    }

    suspend fun getChunksForDocument(docId: Long): List<ChunkEntity> {
        return repository.getChunksForDocument(docId)
    }

    suspend fun getDocumentById(docId: Long): DocumentEntity? {
        return repository.getDocumentById(docId)
    }
}
