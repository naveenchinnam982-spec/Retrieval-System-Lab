package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.converters.VectorTypeConverter
import com.example.data.local.dao.ChunkDao
import com.example.data.local.dao.DocumentDao
import com.example.data.local.dao.EvaluationDao
import com.example.data.local.entity.ChunkEntity
import com.example.data.local.entity.DocumentEntity
import com.example.data.local.entity.EvaluationRecordEntity

@Database(
    entities = [
        DocumentEntity::class,
        ChunkEntity::class,
        EvaluationRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(VectorTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao
    abstract fun chunkDao(): ChunkDao
    abstract fun evaluationDao(): EvaluationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "retrieval_lab.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
