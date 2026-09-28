package com.example.data.local.converters

import androidx.room.TypeConverter

class VectorTypeConverter {

    @TypeConverter
    fun fromFloatArray(array: FloatArray?): String {
        if (array == null || array.isEmpty()) return ""
        val sb = StringBuilder(array.size * 9)
        for (i in array.indices) {
            sb.append(array[i])
            if (i < array.size - 1) {
                sb.append(',')
            }
        }
        return sb.toString()
    }

    @TypeConverter
    fun toFloatArray(data: String?): FloatArray {
        if (data.isNullOrEmpty()) return FloatArray(0)
        val tokens = data.split(',')
        val result = FloatArray(tokens.size)
        for (i in tokens.indices) {
            result[i] = tokens[i].toFloatOrNull() ?: 0.0f
        }
        return result
    }
}
