package com.example.embedding

import android.util.Log
import com.example.BuildConfig
import com.example.vector.VectorMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiEmbeddingService(
    private var customApiKey: String? = null
) : EmbeddingService {

    companion object {
        private const val TAG = "GeminiEmbeddingService"
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent"
    }

    override val providerName: String = "Google Gemini text-embedding-004 (768-dim)"
    override val dimension: Int = 768

    private val localFallback = DenseLocalEmbeddingService(dimension = 768)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun updateApiKey(apiKey: String?) {
        customApiKey = apiKey
    }

    private fun resolveApiKey(): String {
        if (!customApiKey.isNullOrBlank()) {
            return customApiKey!!.trim()
        }
        val buildKey = runCatching {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            field.get(null) as? String
        }.getOrNull()

        return buildKey?.trim().orEmpty()
    }

    override suspend fun generateEmbedding(text: String): FloatArray = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey()
        if (apiKey.isBlank()) {
            Log.w(TAG, "No Gemini API Key provided. Using local 768-dim semantic projection.")
            return@withContext localFallback.generateEmbedding(text)
        }

        try {
            val url = "$BASE_URL?key=$apiKey"
            val jsonPayload = JSONObject().apply {
                put("model", "models/text-embedding-004")
                put("content", JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", text)
                        })
                    })
                })
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (response.isSuccessful && bodyString.isNotBlank()) {
                    val root = JSONObject(bodyString)
                    val embeddingObj = root.optJSONObject("embedding")
                    val valuesArray = embeddingObj?.optJSONArray("values")
                    if (valuesArray != null && valuesArray.length() > 0) {
                        val result = FloatArray(valuesArray.length())
                        for (i in 0 until valuesArray.length()) {
                            result[i] = valuesArray.getDouble(i).toFloat()
                        }
                        return@withContext VectorMath.l2Normalize(result)
                    }
                } else {
                    Log.e(TAG, "Gemini API HTTP Error ${response.code}: $bodyString")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API call failed: ${e.message}. Falling back to local semantic engine.", e)
        }

        localFallback.generateEmbedding(text)
    }
}
