package com.niranjan.englisharticle

import com.niranjan.englisharticle.domain.isLikelyHeading
import com.niranjan.englisharticle.domain.toMeaningTokenGroups
import com.niranjan.englisharticle.domain.toWordTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextAnalysisTest {
    @Test
    fun isLikelyHeading_doesNotTreatLongFirstArticleParagraphAsHeading() {
        val paragraph = """
            Global equity markets surged on Thursday, driven by a strong recovery in the technology sector and positive economic data from major economies.
        """.trimIndent()

        assertFalse(paragraph.isLikelyHeading(paragraphIndex = 0))
    }

    @Test
    fun isLikelyHeading_treatsShortFirstLineAsHeading() {
        assertTrue("Global Markets Rally".isLikelyHeading(paragraphIndex = 0))
    }

    @Test
    fun toMeaningTokenGroups_groupsLikelyThreeWordPhrase() {
        val groups = "Global equity markets surged on Thursday."
            .toWordTokens()
            .toMeaningTokenGroups()

        assertTrue(groups.first().isPhrase)
        assertEquals("Global equity markets", groups.first().cleanText())
    }

    @Test
    fun toMeaningTokenGroups_groupsKnownPhrasesAndCollocations() {
        val groupTexts = "Markets rallied, driven by positive economic data."
            .toWordTokens()
            .toMeaningTokenGroups()
            .filter { it.isPhrase }
            .map { it.cleanText() }

        assertTrue("driven by" in groupTexts)
        assertTrue("positive economic data" in groupTexts)
    }

    @Test
    fun toMeaningTokenGroups_doesNotGroupAcrossSentenceBoundary() {
        val groupTexts = "The market saw a minor correction. Analysts point to rates."
            .toWordTokens()
            .toMeaningTokenGroups()
            .filter { it.isPhrase }
            .map { it.cleanText() }

        assertFalse("minor correction Analysts" in groupTexts)
    }

    @Test
    fun toMeaningTokenGroups_prioritizesAiIdiomaticPhrase() {
        val groupTexts = "It is water under the bridge now."
            .toWordTokens()
            .toMeaningTokenGroups(priorityPhrases = listOf("water under the bridge"))
            .filter { it.isPhrase }
            .map { it.cleanText() }

        assertTrue("water under the bridge" in groupTexts)
    }

    @Test
    fun toMeaningTokenGroups_ignoresAiPhraseAcrossSentenceBoundary() {
        val groupTexts = "It is water under. The bridge was old."
            .toWordTokens()
            .toMeaningTokenGroups(priorityPhrases = listOf("water under the bridge"))
            .filter { it.isPhrase }
            .map { it.cleanText() }

        assertFalse("water under The bridge" in groupTexts)
    }
}
