package com.niranjan.englisharticle

import com.niranjan.englisharticle.data.local.RecentArticleEntity
import com.niranjan.englisharticle.data.local.mergedOver
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentArticleMergeTest {
    private fun row(
        phrasesJson: String = "[]",
        summaryEnglish: String = "",
        summaryKannada: String = ""
    ) = RecentArticleEntity(
        id = 0,
        contentHash = "hash",
        title = "Title",
        subtitle = "",
        author = "",
        publishedDate = "",
        cleanArticle = "Body text.",
        idiomaticPhrasesJson = phrasesJson,
        summaryWhatHappenedEnglish = summaryEnglish,
        summaryWhatHappenedKannada = summaryKannada,
        savedAtMillis = 1L
    )

    @Test
    fun phraseSave_keepsSummarySavedEarlier() {
        val stored = row(summaryEnglish = "A storm hit the city.", summaryKannada = "ನಗರಕ್ಕೆ ಚಂಡಮಾರುತ")
        val phraseSave = row(phrasesJson = "[\"hit the ground running\"]")

        val merged = phraseSave.mergedOver(stored)

        assertEquals("[\"hit the ground running\"]", merged.idiomaticPhrasesJson)
        assertEquals("A storm hit the city.", merged.summaryWhatHappenedEnglish)
        assertEquals("ನಗರಕ್ಕೆ ಚಂಡಮಾರುತ", merged.summaryWhatHappenedKannada)
    }

    @Test
    fun summarySave_keepsPhrasesSavedEarlier() {
        val stored = row(phrasesJson = "[\"hit the ground running\"]")
        val summarySave = row(summaryEnglish = "A storm hit the city.")

        val merged = summarySave.mergedOver(stored)

        assertEquals("[\"hit the ground running\"]", merged.idiomaticPhrasesJson)
        assertEquals("A storm hit the city.", merged.summaryWhatHappenedEnglish)
    }

    @Test
    fun newerValuesWin() {
        val stored = row(phrasesJson = "[\"old phrase\"]", summaryEnglish = "Old summary.")
        val newer = row(phrasesJson = "[\"new phrase\"]", summaryEnglish = "New summary.")

        val merged = newer.mergedOver(stored)

        assertEquals("[\"new phrase\"]", merged.idiomaticPhrasesJson)
        assertEquals("New summary.", merged.summaryWhatHappenedEnglish)
    }
}
