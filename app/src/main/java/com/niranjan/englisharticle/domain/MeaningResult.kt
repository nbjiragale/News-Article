package com.niranjan.englisharticle.domain

import org.json.JSONObject

data class MeaningResult(
    val word: String,
    val meaningKannada: String,
    val simpleEnglish: String,
    val partOfSpeech: String,
    val explanationKannada: String,
    val exampleEnglish: String,
    val exampleKannada: String
) {
    fun hasValidKannadaFields(): Boolean {
        return listOf(meaningKannada, explanationKannada, exampleKannada)
            .all { it.hasKannadaScript() && !it.hasNonKannadaIndicScript() }
    }

    fun hasUsableSentenceTranslation(sourceSentence: String): Boolean {
        val translation = exampleKannada.trim()
        if (!translation.hasKannadaScript() || translation.hasNonKannadaIndicScript()) return false

        val sourceWordCount = sourceSentence.split(Regex("\\s+"))
            .count { token -> token.any { it.isLetterOrDigit() } }
        if (sourceWordCount <= 2) return true

        val translationWordCount = translation.split(Regex("\\s+"))
            .count { token -> token.any { it.isLetterOrDigit() } }
        val onlyTranslatedTappedWord = translation.equals(meaningKannada.trim(), ignoreCase = true)

        return !onlyTranslatedTappedWord && translationWordCount >= 3
    }

    companion object {
        fun fromJson(json: JSONObject): MeaningResult = MeaningResult(
            word = json.optString("word"),
            meaningKannada = json.optString("meaningKannada"),
            simpleEnglish = json.optString("simpleEnglish"),
            partOfSpeech = json.optString("partOfSpeech"),
            explanationKannada = json.optString("explanationKannada"),
            exampleEnglish = json.optString("exampleEnglish"),
            exampleKannada = json.optString("exampleKannada")
        )
    }
}

private fun String.hasKannadaScript(): Boolean {
    return any { it in '\u0C80'..'\u0CFF' }
}

private fun String.hasNonKannadaIndicScript(): Boolean {
    return any {
        it in '\u0C00'..'\u0C7F' ||
            it in '\u0B80'..'\u0BFF' ||
            it in '\u0D00'..'\u0D7F' ||
            it in '\u0900'..'\u097F'
    }
}
