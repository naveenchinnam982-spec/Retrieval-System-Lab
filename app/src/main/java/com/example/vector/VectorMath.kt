package com.example.vector

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object VectorMath {

    fun dotProduct(a: FloatArray, b: FloatArray): Float {
        val size = min(a.size, b.size)
        var sum = 0.0f
        for (i in 0 until size) {
            sum += a[i] * b[i]
        }
        return sum
    }

    fun l2Norm(a: FloatArray): Float {
        var sumSquares = 0.0f
        for (v in a) {
            sumSquares += v * v
        }
        return sqrt(sumSquares)
    }

    fun l2Normalize(a: FloatArray): FloatArray {
        val norm = l2Norm(a)
        if (norm == 0.0f) return a.clone()
        val result = FloatArray(a.size)
        for (i in a.indices) {
            result[i] = a[i] / norm
        }
        return result
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || b.isEmpty() || a.size != b.size) {
            return 0.0f
        }
        val dot = dotProduct(a, b)
        val normA = l2Norm(a)
        val normB = l2Norm(b)
        if (normA == 0.0f || normB == 0.0f) {
            return 0.0f
        }
        val sim = dot / (normA * normB)
        return max(-1.0f, min(1.0f, sim))
    }
}
