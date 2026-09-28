package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.EvaluationRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvaluationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: EvaluationRecordEntity): Long

    @Query("SELECT * FROM evaluation_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<EvaluationRecordEntity>>

    @Query("DELETE FROM evaluation_records")
    suspend fun clearRecords()
}
