package com.niranjan.englisharticle.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MeaningDao {
    @Query("SELECT * FROM cached_meanings WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun findByCacheKey(cacheKey: String): MeaningEntity?

    @Upsert
    suspend fun upsert(meaning: MeaningEntity)
}
