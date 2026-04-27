package com.niranjan.englisharticle

import com.niranjan.englisharticle.domain.MeaningResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningResultTest {
    @Test
    fun hasValidKannadaFields_acceptsKannadaScript() {
        val meaning = meaningResult(
            meaningKannada = "ಬೆಂಬಲ",
            explanationKannada = "ಈ ಪದವು ಸಹಾಯ ಅಥವಾ ಬೆಂಬಲ ನೀಡುವುದನ್ನು ಸೂಚಿಸುತ್ತದೆ.",
            exampleKannada = "ಹೂಡಿಕೆದಾರರು ಯೋಜನೆಯನ್ನು ಬೆಂಬಲಿಸಿದರು."
        )

        assertTrue(meaning.hasValidKannadaFields())
    }

    @Test
    fun hasValidKannadaFields_rejectsTeluguScript() {
        val meaning = meaningResult(
            meaningKannada = "మద్దతు",
            explanationKannada = "ఈ పదం సహాయాన్ని సూచిస్తుంది.",
            exampleKannada = "వారు ప్రణాళికకు మద్దతిచ్చారు."
        )

        assertFalse(meaning.hasValidKannadaFields())
    }

    @Test
    fun hasValidKannadaFields_rejectsMalayalamScript() {
        val meaning = meaningResult(
            meaningKannada = "പിന്തുണ",
            explanationKannada = "ഈ വാക്ക് സഹായത്തെ സൂചിപ്പിക്കുന്നു.",
            exampleKannada = "അവർ പദ്ധതിയെ പിന്തുണച്ചു."
        )

        assertFalse(meaning.hasValidKannadaFields())
    }

    @Test
    fun hasUsableSentenceTranslation_rejectsOnlyTranslatedWordForLongSentence() {
        val meaning = meaningResult(
            meaningKannada = "\u0C85",
            explanationKannada = "\u0C85 \u0C86 \u0C87",
            exampleKannada = "\u0C85"
        )

        assertFalse(meaning.hasUsableSentenceTranslation("The rally was largely backed by investors."))
    }

    @Test
    fun hasUsableSentenceTranslation_acceptsFullSentenceLikeTranslation() {
        val meaning = meaningResult(
            meaningKannada = "\u0C85",
            explanationKannada = "\u0C85 \u0C86 \u0C87",
            exampleKannada = "\u0C85 \u0C86 \u0C87 \u0C88"
        )

        assertTrue(meaning.hasUsableSentenceTranslation("The rally was largely backed by investors."))
    }

    private fun meaningResult(
        meaningKannada: String,
        explanationKannada: String,
        exampleKannada: String
    ): MeaningResult {
        return MeaningResult(
            word = "backed",
            meaningKannada = meaningKannada,
            simpleEnglish = "Supported or helped.",
            partOfSpeech = "verb",
            explanationKannada = explanationKannada,
            exampleEnglish = "They backed the plan.",
            exampleKannada = exampleKannada
        )
    }
}
