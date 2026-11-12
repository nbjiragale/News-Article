package com.niranjan.englisharticle.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentArticleDao {
    @Query("SELECT * FROM recent_articles ORDER BY savedAtMillis DESC")
    fun observeAll(): Flow<List<RecentArticleEntity>>

    @Query("SELECT * FROM recent_articles WHERE contentHash = :contentHash LIMIT 1")
    suspend fun findByContentHash(contentHash: String): RecentArticleEntity?

    @Insert
    suspend fun insert(article: RecentArticleEntity): Long

    @Update
    suspend fun update(article: RecentArticleEntity)

    @Query("DELETE FROM recent_articles WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM recent_articles")
    suspend fun deleteAll()

    /**
     * Drops the oldest rows beyond [keep]. History stores full article bodies and nothing
     * else bounded its growth.
     */
    @Query(
        """
        DELETE FROM recent_articles
        WHERE id NOT IN (
            SELECT id FROM recent_articles ORDER BY savedAtMillis DESC LIMIT :keep
        )
        """
    )
    suspend fun trimTo(keep: Int)

    /**
     * Inserts [article], or updates the existing row holding the same content.
     *
     * Deliberately not `@Upsert`. Room implements that as insert-then-update-by-primary-key,
     * so a row that collides on the `contentHash` unique index while carrying `id = 0` would
     * fail the insert and then update nothing — silently dropping the save. Resolving the id
     * from the hash first keeps the update pointed at a real primary key.
     *
     * Enrichments already stored (summary, idiomatic phrases) are kept when [article] does
     * not carry them. The summary and the phrase extraction finish at different times and
     * each saves the article; without this merge, the later save erased the earlier one's work.
     *
     * `@Transaction` so the lookup and the write cannot interleave with a concurrent save of
     * the same article and recreate the duplicate this exists to prevent.
     */
    @Transaction
    suspend fun save(article: RecentArticleEntity, keep: Int): Long {
        val existing = findByContentHash(article.contentHash)
        val savedId = if (existing != null) {
            update(article.mergedOver(existing).copy(id = existing.id))
            existing.id
        } else {
            insert(article)
        }
        trimTo(keep)
        return savedId
    }
}
