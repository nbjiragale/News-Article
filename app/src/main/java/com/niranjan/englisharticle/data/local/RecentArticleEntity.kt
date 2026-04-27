package com.niranjan.englisharticle.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.niranjan.englisharticle.domain.ArticleSummary
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
