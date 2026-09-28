package com.example.embedding

import android.content.Context
import android.content.SharedPreferences

class EmbeddingManager(context: Context) {

    companion object {
        const val PREFS_NAME = "retrieval_lab_prefs"
        const val KEY_PROVIDER = "embedding_provider"
        const val KEY_API_KEY = "gemini_api_key"
        const val KEY_DEFAULT_CHUNK_SIZE = "default_chunk_size"
        const val KEY_DEFAULT_OVERLAP = "default_chunk_overlap"

        const val PROVIDER_LOCAL = "LOCAL_384"
        const val PROVIDER_GEMINI = "GEMINI_768"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val localService = DenseLocalEmbeddingService(dimension = 384)
    private val geminiService = GeminiEmbeddingService(prefs.getString(KEY_API_KEY, null))

    fun getActiveService(): EmbeddingService {
        val provider = prefs.getString(KEY_PROVIDER, PROVIDER_LOCAL)
        return if (provider == PROVIDER_GEMINI) {
            geminiService
        } else {
            localService
        }
    }

    fun isGeminiActive(): Boolean {
        return prefs.getString(KEY_PROVIDER, PROVIDER_LOCAL) == PROVIDER_GEMINI
    }

    fun setProvider(provider: String) {
        prefs.edit().putString(KEY_PROVIDER, provider).apply()
    }

    fun setApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply()
        geminiService.updateApiKey(apiKey)
    }

    fun getApiKey(): String {
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    fun getDefaultChunkSize(): Int {
        return prefs.getInt(KEY_DEFAULT_CHUNK_SIZE, 100)
    }

    fun setDefaultChunkSize(size: Int) {
        prefs.edit().putInt(KEY_DEFAULT_CHUNK_SIZE, size).apply()
    }

    fun getDefaultOverlap(): Int {
        return prefs.getInt(KEY_DEFAULT_OVERLAP, 20)
    }

    fun setDefaultOverlap(overlap: Int) {
        prefs.edit().putInt(KEY_DEFAULT_OVERLAP, overlap).apply()
    }
}
