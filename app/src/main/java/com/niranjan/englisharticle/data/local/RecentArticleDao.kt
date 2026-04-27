package com.niranjan.englisharticle.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentArticleDao {
    @Query("SELECT * FROM recent_articles ORDER BY savedAtMillis DESC")
    fun observeAll(): Flow<List<RecentArticleEntity>>

    @Insert
    suspend fun insert(article: RecentArticleEntity): Long
}
