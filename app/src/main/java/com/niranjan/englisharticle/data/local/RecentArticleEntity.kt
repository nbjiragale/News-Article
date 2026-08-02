package com.niranjan.englisharticle.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.niranjan.englisharticle.domain.ArticleSummary
import com.niranjan.englisharticle.domain.CleanArticleResult
import com.niranjan.englisharticle.domain.RecentArticle
import org.json.JSONArray
import java.security.MessageDigest

@Entity(
    tableName = "recent_articles",
    indices = [
        // Content identity. Without it every save inserted a fresh row, so opening an
        // article and generating its summary left two full-body copies in history.
        Index(value = ["contentHash"], unique = true),
        // Every read of this table sorts by savedAtMillis DESC.
        Index(value = ["savedAtMillis"])
    ]
)
data class RecentArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentHash: String,
    val title: String,
    val subtitle: String,
    val author: String,
    val publishedDate: String,
    val cleanArticle: String,
    val idiomaticPhrasesJson: String,
    @ColumnInfo(defaultValue = "") val summaryWhatHappenedEnglish: String = "",
    @ColumnInfo(defaultValue = "") val summaryWhatHappenedKannada: String = "",
    @ColumnInfo(defaultValue = "") val summaryGistEnglish: String = "",
    @ColumnInfo(defaultValue = "") val summaryGistKannada: String = "",
    val savedAtMillis: Long
) {
    fun toRecentArticle(): RecentArticle {
        val summary = ArticleSummary(
            whatHappenedEnglish = summaryWhatHappenedEnglish,
            whatHappenedKannada = summaryWhatHappenedKannada,
            gistEnglish = summaryGistEnglish,
            gistKannada = summaryGistKannada
        ).takeIf { it.isNotBlank() }
        return RecentArticle(
            id = id,
            title = title,
            subtitle = subtitle,
            author = author,
            publishedDate = publishedDate,
            cleanArticle = cleanArticle,
            idiomaticPhrases = idiomaticPhrasesJson.toPhraseList(),
            summary = summary,
            savedAtMillis = savedAtMillis
        )
    }

    companion object {
        fun fromCleanArticleResult(
            article: CleanArticleResult,
            savedAtMillis: Long
        ): RecentArticleEntity {
            val summary = article.summary
            return RecentArticleEntity(
                contentHash = recentArticleContentHash(article.title, article.cleanArticle),
                title = article.title,
                subtitle = article.subtitle,
                author = article.author,
                publishedDate = article.publishedDate,
                cleanArticle = article.cleanArticle,
                idiomaticPhrasesJson = article.idiomaticPhrases.toPhraseJson(),
                summaryWhatHappenedEnglish = summary?.whatHappenedEnglish.orEmpty(),
                summaryWhatHappenedKannada = summary?.whatHappenedKannada.orEmpty(),
                summaryGistEnglish = summary?.gistEnglish.orEmpty(),
                summaryGistKannada = summary?.gistKannada.orEmpty(),
                savedAtMillis = savedAtMillis
            )
        }
    }
}

/**
 * Stable identity for an article, used to recognise a re-save of something already in
 * history instead of inserting a duplicate.
 *
 * Title and body are hashed together because the body alone is not quite enough — the
 * same transcript imported under a corrected title is legitimately a different entry.
 * Deliberately excluded: summary and idiomatic phrases. Both are enrichments added to an
 * article that already exists, and including them would make every enrichment mint a new
 * identity, which is the exact bug this is here to prevent.
 *
 * Also used by migration 4→5 to backfill the column, so it must stay stable: changing the
 * input format silently orphans every previously stored hash.
 */
internal fun recentArticleContentHash(title: String, cleanArticle: String): String {
    // NUL separator so ("ab", "c") and ("a", "bc") cannot collide. Written as an escape
    // rather than a literal control character so it stays visible in diffs and editors.
    val payload = title.trim() + "\u0000" + cleanArticle.trim()
    return MessageDigest.getInstance("SHA-256")
        .digest(payload.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

private fun List<String>.toPhraseJson(): String {
    val array = JSONArray()
    forEach { phrase -> array.put(phrase) }
    return array.toString()
}

private fun String.toPhraseList(): List<String> {
    return runCatching {
        val array = JSONArray(this)
        (0 until array.length()).mapNotNull { index ->
            array.optString(index).trim().takeIf { it.isNotBlank() }
        }
    }.getOrDefault(emptyList())
}
