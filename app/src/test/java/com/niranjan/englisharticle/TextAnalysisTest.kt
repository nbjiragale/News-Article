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
    fun toMeaningTokenGroups_doesNotMarkNamesOrPlainWordPairsAsPhrases() {
        val groupTexts = "Wall Street rallied as the Prime Minister praised strong growth in global markets."
            .toWordTokens()
            .toMeaningTokenGroups()
            .filter { it.isPhrase }
            .map { it.cleanText() }

        assertTrue("Expected no phrases, got $groupTexts", groupTexts.isEmpty())
    }

    @Test
    fun toMeaningTokenGroups_groupsBuiltInFixedExpressions() {
        val groupTexts = "Prices rose because of the drought, according to officials."
            .toWordTokens()
            .toMeaningTokenGroups()
            .filter { it.isPhrase }
            .map { it.cleanText() }

        assertEquals(listOf("because of", "according to"), groupTexts)
    }

    @Test
    fun toMeaningTokenGroups_matchesAiPhraseDespiteCurlyApostropheAndCase() {
        val groupTexts = "Officials said it\u2019s water under the bridge now."
            .toWordTokens()
            .toMeaningTokenGroups(priorityPhrases = listOf("It's water under the bridge"))
            .filter { it.isPhrase }
            .map { it.cleanText() }

        assertEquals(listOf("it\u2019s water under the bridge"), groupTexts)
    }

    @Test
    fun toMeaningTokenGroups_doesNotGroupAcrossSentenceBoundary() {
        val groupTexts = "The market saw a minor correction. Analysts point to rates."
            .toWordTokens()
            .toMeaningTokenGroups(priorityPhrases = listOf("minor correction analysts"))
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

    @Test
    fun findSentenceContaining_doesNotSplitOnAbbreviationsOrDecimals() {
        val body = "Mr. Modi met Dr. Rao in the U.S. capital on Monday. Prices rose 3.5% this week."

        assertEquals(
            "Mr. Modi met Dr. Rao in the U.S. capital on Monday.",
            body.findSentenceContaining(body.indexOf("Rao"))
        )
        assertEquals(
            "Prices rose 3.5% this week.",
            body.findSentenceContaining(body.indexOf("week"))
        )
    }

    @Test
    fun findSentenceContaining_keepsClosingQuoteAndStopsAtLineBreak() {
        val body = "He said, \"We will win.\" Then he left.\n\nA new paragraph starts here."

        assertEquals("He said, \"We will win.\"", body.findSentenceContaining(body.indexOf("win")))
        assertEquals("Then he left.", body.findSentenceContaining(body.indexOf("left")))
        assertEquals("A new paragraph starts here.", body.findSentenceContaining(body.indexOf("new")))
    }

    @Test
    fun ensureParagraphs_groupsWholeSentencesWithAbbreviations() {
        val text = "Mr. Shah spoke first. Gen. Singh spoke next. Prices rose 2.5%. " +
            "The U.S. team arrived. Talks ended late."

        val paragraphs = text.ensureParagraphs().split("\n\n")

        assertEquals(
            listOf(
                "Mr. Shah spoke first. Gen. Singh spoke next. Prices rose 2.5%.",
                "The U.S. team arrived. Talks ended late."
            ),
            paragraphs
        )
    }
}
