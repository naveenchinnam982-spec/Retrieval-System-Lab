package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<ChunkEntity>)

    @Query("SELECT * FROM chunks WHERE documentId = :documentId ORDER BY chunkIndex ASC")
    suspend fun getChunksForDocument(documentId: Long): List<ChunkEntity>

    @Query("SELECT * FROM chunks")
    suspend fun getAllChunksWithEmbeddings(): List<ChunkEntity>

    @Query("SELECT * FROM chunks WHERE documentId = :documentId")
    suspend fun getChunksForDocumentSync(documentId: Long): List<ChunkEntity>

    @Query("SELECT COUNT(*) FROM chunks")
    fun getChunkCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM chunks")
    suspend fun getChunkCountSync(): Int

    @Query("DELETE FROM chunks WHERE documentId = :documentId")
    suspend fun deleteChunksForDocument(documentId: Long)

    @Query("DELETE FROM chunks")
    suspend fun deleteAllChunks()
}
