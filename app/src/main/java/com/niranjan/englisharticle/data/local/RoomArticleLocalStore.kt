package com.niranjan.englisharticle.data.local

import com.niranjan.englisharticle.data.ArticleLocalStore
import com.niranjan.englisharticle.domain.CleanArticleResult
import com.niranjan.englisharticle.domain.MeaningLookupMode
import com.niranjan.englisharticle.domain.MeaningResult
import com.niranjan.englisharticle.domain.RecentArticle
import com.niranjan.englisharticle.domain.SavedWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class RoomArticleLocalStore(
    private val meaningDao: MeaningDao,
    private val recentArticleDao: RecentArticleDao,
    private val savedWordDao: SavedWordDao
) : ArticleLocalStore {
    override fun observeRecentArticles(): Flow<List<RecentArticle>> {
        return recentArticleDao.observeAll()
            .map { articles -> articles.map { it.toRecentArticle() } }
            .flowOn(Dispatchers.Default)
    }

    override fun observeSavedWords(): Flow<List<SavedWord>> {
        return savedWordDao.observeAll()
            .map { words -> words.map { it.toSavedWord() } }
            .flowOn(Dispatchers.Default)
    }

    override suspend fun saveRecentArticle(article: CleanArticleResult): Long {
        return recentArticleDao.insert(
            RecentArticleEntity.fromCleanArticleResult(
                article = article,
                savedAtMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getMeaning(
        word: String,
        sentence: String,
        lookupMode: MeaningLookupMode
    ): MeaningResult? {
        return meaningDao
            .findByCacheKey(createMeaningCacheKey(word, sentence, lookupMode))
            ?.toMeaningResult()
    }

    override suspend fun saveMeaning(
        word: String,
        sentence: String,
        lookupMode: MeaningLookupMode,
        meaning: MeaningResult
    ) {
        meaningDao.upsert(
            MeaningEntity.fromMeaningResult(
                word = word,
                sentence = sentence,
                lookupMode = lookupMode,
                meaning = meaning,
                savedAtMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun saveWord(
        word: String,
        sentence: String,
        lookupMode: MeaningLookupMode,
        articleTitle: String,
        meaning: MeaningResult
    ) {
        val savedAtMillis = System.currentTimeMillis()
        val newWord = SavedWordEntity.fromMeaning(
            word = word,
            sentence = sentence,
            lookupMode = lookupMode,
            articleTitle = articleTitle,
            meaning = meaning,
            savedAtMillis = savedAtMillis
        )
        val existing = savedWordDao.findByKey(newWord.savedKey)

        savedWordDao.upsert(
            existing?.copy(
                word = newWord.word,
                sentence = newWord.sentence,
                lookupMode = newWord.lookupMode,
                articleTitle = newWord.articleTitle,
                meaningKannada = newWord.meaningKannada,
                simpleEnglish = newWord.simpleEnglish,
                partOfSpeech = newWord.partOfSpeech,
                explanationKannada = newWord.explanationKannada,
                exampleEnglish = newWord.exampleEnglish,
                exampleKannada = newWord.exampleKannada
            ) ?: newWord
        )
    }

    override suspend fun deleteSavedWord(savedKey: String) {
        savedWordDao.delete(savedKey)
    }

    override suspend fun recordPracticeResult(savedKey: String, isCorrect: Boolean) {
        savedWordDao.recordPracticeResult(
            savedKey = savedKey,
            correctIncrement = if (isCorrect) 1 else 0,
            practicedAtMillis = System.currentTimeMillis()
        )
    }
}
