package com.niranjan.englisharticle

import com.niranjan.englisharticle.domain.ensureParagraphs
import com.niranjan.englisharticle.domain.findSentenceContaining
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

    @Test
    fun ensureParagraphs_splitsUnpunctuatedTranscriptIntoShortParagraphs() {
        val transcript = (1..230).joinToString(" ") { "word$it" }

        val paragraphs = transcript.ensureParagraphs().split("\n\n")

        assertTrue("Expected several paragraphs, got ${paragraphs.size}", paragraphs.size >= 4)
        assertTrue(paragraphs.all { it.split(" ").size <= 50 })
        assertEquals(transcript, paragraphs.joinToString(" "))
    }

    @Test
    fun findSentenceContaining_staysInsideOneParagraphOfATranscript() {
        val body = (1..230).joinToString(" ") { "word$it" }.ensureParagraphs()
        val index = body.indexOf("word120")

        val sentence = body.findSentenceContaining(index)

        assertTrue(sentence.contains("word120"))
        assertTrue(sentence.split(" ").size <= 50)
    }

    @Test
    fun ensureParagraphs_keepsShortPunctuatedTextUnchanged() {
        val text = "One sentence. Two sentence. Three sentence."
        assertEquals(text, text.ensureParagraphs())
    }
}
