package com.niranjan.englisharticle.domain

data class SavedWord(
    val savedKey: String,
    val word: String,
    val sentence: String,
    val lookupMode: MeaningLookupMode,
    val articleTitle: String,
    val meaning: MeaningResult,
    val savedAtMillis: Long,
    val practiceAttempts: Int,
    val correctAttempts: Int,
    val lastPracticedAtMillis: Long?
) {
    val accuracyPercent: Int
        get() = if (practiceAttempts == 0) 0 else (correctAttempts * 100) / practiceAttempts
}

fun createSavedWordKey(
    word: String,
    sentence: String,
    lookupMode: MeaningLookupMode
): String {
    return lookupMode.name.lowercase() + "|" + word.trim().lowercase() + "|" + sentence.trim()
}
