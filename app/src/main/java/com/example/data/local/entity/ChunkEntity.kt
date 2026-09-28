package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chunks",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"])]
)
data class ChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val chunkIndex: Int,
    val text: String,
    val wordCount: Int,
    val pageNumber: Int = 1,
    val embedding: FloatArray,
    val l2Norm: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChunkEntity

        if (id != other.id) return false
        if (documentId != other.documentId) return false
        if (chunkIndex != other.chunkIndex) return false
        if (text != other.text) return false
        if (wordCount != other.wordCount) return false
        if (pageNumber != other.pageNumber) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (l2Norm != other.l2Norm) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + documentId.hashCode()
        result = 31 * result + chunkIndex
        result = 31 * result + text.hashCode()
        result = 31 * result + wordCount
        result = 31 * result + pageNumber
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + l2Norm.hashCode()
        return result
    }
}
