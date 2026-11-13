package com.niranjan.englisharticle.domain

import org.json.JSONObject

data class CleanArticleResult(
    val title: String,
    val subtitle: String,
    val author: String,
    val publishedDate: String,
    val cleanArticle: String,
    val idiomaticPhrases: List<String> = emptyList(),
    val summary: ArticleSummary? = null
) {
    /**
     * What a word lookup sends as context: the title, the short summary when there is one,
     * and [nearbyText] (see [contextAround]). It used to send the whole article body on every
     * tap; the nearby text is enough to pick the right sense, and the title and summary keep
     * the topic and who's who in view.
     */
    fun contextForMeaning(nearbyText: String): String {
        val gist = summary?.whatHappenedEnglish.orEmpty()
        return listOfNotNull(
            title.takeIf { it.isNotBlank() }?.let { "Title: $it" },
            gist.takeIf { it.isNotBlank() }?.let { "Summary: $it" },
            nearbyText.takeIf { it.isNotBlank() }?.let { "Nearby text: $it" }
        ).joinToString("\n")
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
