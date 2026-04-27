package com.niranjan.englisharticle.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.niranjan.englisharticle.domain.MeaningLookupMode
import com.niranjan.englisharticle.domain.MeaningResult
import com.niranjan.englisharticle.domain.SavedWord
import com.niranjan.englisharticle.domain.createSavedWordKey

@Entity(tableName = "saved_words")
data class SavedWordEntity(
    @PrimaryKey val savedKey: String,
    val word: String,
    val sentence: String,
    val lookupMode: String,
    val articleTitle: String,
    val meaningKannada: String,
    val simpleEnglish: String,
    val partOfSpeech: String,
    val explanationKannada: String,
    val exampleEnglish: String,
    val exampleKannada: String,
    val savedAtMillis: Long,
    val practiceAttempts: Int,
    val correctAttempts: Int,
    val lastPracticedAtMillis: Long?
) {
    fun toSavedWord(): SavedWord {
        return SavedWord(
            savedKey = savedKey,
            word = word,
            sentence = sentence,
            lookupMode = runCatching { MeaningLookupMode.valueOf(lookupMode) }
                .getOrDefault(MeaningLookupMode.Word),
            articleTitle = articleTitle,
            meaning = MeaningResult(
                word = word,
                meaningKannada = meaningKannada,
                simpleEnglish = simpleEnglish,
                partOfSpeech = partOfSpeech,
                explanationKannada = explanationKannada,
                exampleEnglish = exampleEnglish,
                exampleKannada = exampleKannada
            ),
            savedAtMillis = savedAtMillis,
            practiceAttempts = practiceAttempts,
            correctAttempts = correctAttempts,
            lastPracticedAtMillis = lastPracticedAtMillis
        )
    }

    companion object {
        fun fromSavedWord(savedWord: SavedWord): SavedWordEntity {
            return SavedWordEntity(
                savedKey = savedWord.savedKey,
                word = savedWord.word.trim(),
                sentence = savedWord.sentence.trim(),
                lookupMode = savedWord.lookupMode.name,
                articleTitle = savedWord.articleTitle.trim(),
                meaningKannada = savedWord.meaning.meaningKannada,
                simpleEnglish = savedWord.meaning.simpleEnglish,
                partOfSpeech = savedWord.meaning.partOfSpeech,
                explanationKannada = savedWord.meaning.explanationKannada,
                exampleEnglish = savedWord.meaning.exampleEnglish,
                exampleKannada = savedWord.meaning.exampleKannada,
                savedAtMillis = savedWord.savedAtMillis,
                practiceAttempts = savedWord.practiceAttempts,
                correctAttempts = savedWord.correctAttempts,
                lastPracticedAtMillis = savedWord.lastPracticedAtMillis
            )
        }

        fun fromMeaning(
            word: String,
            sentence: String,
            lookupMode: MeaningLookupMode,
            articleTitle: String,
            meaning: MeaningResult,
            savedAtMillis: Long
        ): SavedWordEntity {
            return SavedWordEntity(
                savedKey = createSavedWordKey(word, sentence, lookupMode),
                word = word.trim(),
                sentence = sentence.trim(),
                lookupMode = lookupMode.name,
                articleTitle = articleTitle.trim(),
                meaningKannada = meaning.meaningKannada,
                simpleEnglish = meaning.simpleEnglish,
                partOfSpeech = meaning.partOfSpeech,
                explanationKannada = meaning.explanationKannada,
                exampleEnglish = meaning.exampleEnglish,
                exampleKannada = meaning.exampleKannada,
                savedAtMillis = savedAtMillis,
                practiceAttempts = 0,
                correctAttempts = 0,
                lastPracticedAtMillis = null
            )
        }
    }
}
