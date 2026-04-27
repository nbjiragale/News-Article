package com.niranjan.englisharticle.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedWordDao {
    @Query("SELECT * FROM saved_words ORDER BY savedAtMillis DESC")
    fun observeAll(): Flow<List<SavedWordEntity>>

    @Query("SELECT * FROM saved_words WHERE savedKey = :savedKey LIMIT 1")
    suspend fun findByKey(savedKey: String): SavedWordEntity?

    @Upsert
    suspend fun upsert(savedWord: SavedWordEntity)

    @Query("DELETE FROM saved_words WHERE savedKey = :savedKey")
    suspend fun delete(savedKey: String)

    @Query(
        """
        UPDATE saved_words
        SET practiceAttempts = practiceAttempts + 1,
            correctAttempts = correctAttempts + :correctIncrement,
            lastPracticedAtMillis = :practicedAtMillis
        WHERE savedKey = :savedKey
        """
    )
    suspend fun recordPracticeResult(
        savedKey: String,
        correctIncrement: Int,
        practicedAtMillis: Long
    )
}
