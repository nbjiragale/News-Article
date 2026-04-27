package com.niranjan.englisharticle.data

import com.niranjan.englisharticle.domain.CleanArticleResult
import com.niranjan.englisharticle.domain.MeaningLookupMode
import com.niranjan.englisharticle.domain.MeaningResult
import com.niranjan.englisharticle.domain.RecentArticle
import com.niranjan.englisharticle.domain.SavedWord
import kotlinx.coroutines.flow.Flow

interface ArticleLocalStore {
    fun observeRecentArticles(): Flow<List<RecentArticle>>

    fun observeSavedWords(): Flow<List<SavedWord>>

    suspend fun saveRecentArticle(article: CleanArticleResult): Long

    suspend fun getMeaning(word: String, sentence: String, lookupMode: MeaningLookupMode): MeaningResult?

    suspend fun saveMeaning(
        word: String,
        sentence: String,
        lookupMode: MeaningLookupMode,
        meaning: MeaningResult
    )

    suspend fun saveWord(
        word: String,
        sentence: String,
        lookupMode: MeaningLookupMode,
        articleTitle: String,
        meaning: MeaningResult
    )

    suspend fun deleteSavedWord(savedKey: String)

    suspend fun recordPracticeResult(savedKey: String, isCorrect: Boolean)
}
