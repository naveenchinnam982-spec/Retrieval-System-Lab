package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val fileType: String,
    val fileSize: Long,
    val chunkCount: Int,
    val vectorDimension: Int,
    val embeddingProvider: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "Indexed"
)
