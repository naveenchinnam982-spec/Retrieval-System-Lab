package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "evaluation_records")
data class EvaluationRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val queryText: String,
    val topK: Int,
    val retrievedCount: Int,
    val relevantCount: Int,
    val precisionAtK: Float,
    val hitRateAtK: Float,
    val mrr: Float,
    val timestamp: Long = System.currentTimeMillis()
)
