package com.niranjan.englisharticle.domain

interface ArticleAiService {
    suspend fun cleanArticle(rawText: String): CleanArticleResult

    suspend fun formatTranscript(
        rawTranscript: String,
        videoTitle: String,
        videoAuthor: String
    ): CleanArticleResult

    suspend fun extractIdiomaticPhrases(articleText: String): List<String>

    suspend fun summarizeArticle(articleText: String): ArticleSummary

    suspend fun fetchMeaning(
        articleText: String,
        sentence: String,
        word: String,
        lookupMode: MeaningLookupMode
    ): MeaningResult
}
