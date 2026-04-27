package com.niranjan.englisharticle.domain

data class RecentArticle(
    val id: Long,
    val title: String,
    val subtitle: String,
    val author: String,
    val publishedDate: String,
    val cleanArticle: String,
    val idiomaticPhrases: List<String>,
    val summary: ArticleSummary?,
    val savedAtMillis: Long
) {
    fun toCleanArticleResult(): CleanArticleResult {
        return CleanArticleResult(
            title = title,
            subtitle = subtitle,
            author = author,
            publishedDate = publishedDate,
            cleanArticle = cleanArticle,
            idiomaticPhrases = idiomaticPhrases,
            summary = summary
        )
    }
}
