package com.niranjan.englisharticle.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.niranjan.englisharticle.domain.CleanArticleResult
import com.niranjan.englisharticle.domain.RecentArticle
import org.json.JSONArray

@Entity(tableName = "recent_articles")
data class RecentArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subtitle: String,
    val author: String,
    val publishedDate: String,
    val cleanArticle: String,
    val idiomaticPhrasesJson: String,
    val savedAtMillis: Long
) {
    fun toRecentArticle(): RecentArticle {
        return RecentArticle(
            id = id,
            title = title,
            subtitle = subtitle,
            author = author,
            publishedDate = publishedDate,
            cleanArticle = cleanArticle,
            idiomaticPhrases = idiomaticPhrasesJson.toPhraseList(),
            savedAtMillis = savedAtMillis
        )
    }

    companion object {
        fun fromCleanArticleResult(
            article: CleanArticleResult,
            savedAtMillis: Long
        ): RecentArticleEntity {
            return RecentArticleEntity(
                title = article.title,
                subtitle = article.subtitle,
                author = article.author,
                publishedDate = article.publishedDate,
                cleanArticle = article.cleanArticle,
                idiomaticPhrasesJson = article.idiomaticPhrases.toPhraseJson(),
                savedAtMillis = savedAtMillis
            )
        }
    }
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
