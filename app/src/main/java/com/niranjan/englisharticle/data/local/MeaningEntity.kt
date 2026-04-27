package com.niranjan.englisharticle.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.niranjan.englisharticle.domain.MeaningLookupMode
import com.niranjan.englisharticle.domain.MeaningResult

@Entity(tableName = "cached_meanings")
data class MeaningEntity(
    @PrimaryKey val cacheKey: String,
    val word: String,
    val sentence: String,
    val meaningKannada: String,
    val simpleEnglish: String,
    val partOfSpeech: String,
    val explanationKannada: String,
    val exampleEnglish: String,
    val exampleKannada: String,
    val savedAtMillis: Long
) {
    fun toMeaningResult(): MeaningResult {
        return MeaningResult(
            word = word,
            meaningKannada = meaningKannada,
            simpleEnglish = simpleEnglish,
            partOfSpeech = partOfSpeech,
            explanationKannada = explanationKannada,
            exampleEnglish = exampleEnglish,
            exampleKannada = exampleKannada
        )
    }

    companion object {
        fun fromMeaningResult(
            word: String,
            sentence: String,
            lookupMode: MeaningLookupMode,
            meaning: MeaningResult,
            savedAtMillis: Long
        ): MeaningEntity {
            return MeaningEntity(
                cacheKey = createMeaningCacheKey(word, sentence, lookupMode),
                word = word.trim(),
                sentence = sentence.trim(),
                meaningKannada = meaning.meaningKannada,
                simpleEnglish = meaning.simpleEnglish,
                partOfSpeech = meaning.partOfSpeech,
                explanationKannada = meaning.explanationKannada,
                exampleEnglish = meaning.exampleEnglish,
                exampleKannada = meaning.exampleKannada,
                savedAtMillis = savedAtMillis
            )
        }
    }
}

fun createMeaningCacheKey(
    word: String,
    sentence: String,
    lookupMode: MeaningLookupMode
): String {
    return lookupMode.name.lowercase() + "|" + word.trim().lowercase() + "|" + sentence.trim()
}
