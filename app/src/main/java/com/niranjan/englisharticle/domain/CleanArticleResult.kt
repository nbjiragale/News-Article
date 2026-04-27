package com.niranjan.englisharticle.domain

import org.json.JSONObject

data class CleanArticleResult(
    val title: String,
    val subtitle: String,
    val author: String,
    val publishedDate: String,
    val cleanArticle: String,
    val idiomaticPhrases: List<String> = emptyList()
) {
    fun contextForMeaning(): String {
        return listOf(title, subtitle, author, publishedDate, cleanArticle)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }

    fun looksSummarizedComparedTo(rawText: String): Boolean {
        val localArticleEstimateWordCount = ArticleFormatter.format(rawText).articleWordCount()
        val cleanWordCount = cleanArticle.articleWordCount()

        if (localArticleEstimateWordCount < 250) return false
        if (cleanWordCount < 50) return true

        return cleanWordCount < localArticleEstimateWordCount * 0.12
    }

    fun withLocalArticleFallback(rawText: String): CleanArticleResult {
        if (cleanArticle.isNotBlank() && !looksSummarizedComparedTo(rawText)) return this

        val localArticle = ArticleFormatter.format(rawText)
        if (localArticle.isBlank()) return this

        return copy(cleanArticle = localArticle)
    }

    companion object {
        fun fromJson(json: JSONObject): CleanArticleResult = CleanArticleResult(
            title = json.optString("title").trim(),
            subtitle = json.optString("subtitle").trim(),
            author = json.optString("author").trim(),
            publishedDate = json.optString("publishedDate").trim(),
            cleanArticle = json.optString("cleanArticle").trim()
        )
    }
}

fun String.articleWordCount(): Int {
    return Regex("[A-Za-z0-9']+").findAll(this).count()
}
