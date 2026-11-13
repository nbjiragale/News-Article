package com.niranjan.englisharticle

import com.niranjan.englisharticle.domain.ArticleSummary
import com.niranjan.englisharticle.domain.CleanArticleResult
import com.niranjan.englisharticle.domain.MEANING_CONTEXT_MAX_WORDS
import com.niranjan.englisharticle.domain.contextAround
import com.niranjan.englisharticle.domain.ensureParagraphs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningContextTest {
    private val body = listOf(
        "The Cabinet discussed the issue. No decision was taken.",
        "The Congress has been caught in a Catch-22 situation, a minister said. " +
            "Going ahead with the final rolls would be contrary to its stand. " +
            "The BJP could also move court, the minister said. " +
            "A decision is still pending.",
        "KPCC president B.K. Hariprasad wants to go back to the old rolls."
    ).joinToString("\n\n")

    private fun wordCount(text: String) = text.split(Regex("\\s+")).count { it.isNotBlank() }

    @Test
    fun includesTappedSentenceAndItsNeighbours() {
        val context = body.contextAround(body.indexOf("move court"))

        assertEquals(
            "Going ahead with the final rolls would be contrary to its stand. " +
                "The BJP could also move court, the minister said. " +
                "A decision is still pending.",
            context
        )
    }

    @Test
    fun staysInsideTheParagraph() {
        val context = body.contextAround(body.indexOf("Catch-22"))

        assertTrue(context.startsWith("The Congress has been caught"))
        assertFalse("previous paragraph leaked in", context.contains("No decision was taken"))
    }

    @Test
    fun abbreviationsDoNotBreakTheSentence() {
        val context = body.contextAround(body.indexOf("Hariprasad"))

        assertEquals("KPCC president B.K. Hariprasad wants to go back to the old rolls.", context)
    }

    @Test
    fun longNeighboursAreTrimmedToTheCapButTheTappedSentenceStaysWhole() {
        val long = (1..200).joinToString(" ") { "word$it" } + "."
        val text = "$long The tapped sentence is here. $long"

        val context = text.contextAround(text.indexOf("tapped"))

        assertTrue(context.contains("The tapped sentence is here."))
        assertTrue(wordCount(context) <= MEANING_CONTEXT_MAX_WORDS)
    }

    @Test
    fun unpunctuatedTranscriptIsCappedAroundTheTappedWord() {
        // Raw captions: the reader splits them into 50-word paragraphs; even without that,
        // a run-on line must not be sent whole.
        val transcript = (1..1_000).joinToString(" ") { "w$it" }

        val context = transcript.contextAround(transcript.indexOf("w510 "))
        val fromReaderBody = transcript.ensureParagraphs().let { it.contextAround(it.indexOf("w510 ")) }

        assertTrue(wordCount(context) <= MEANING_CONTEXT_MAX_WORDS)
        assertTrue(context.split(" ").contains("w510"))
        assertTrue(wordCount(fromReaderBody) <= 50)
        assertTrue(fromReaderBody.split(" ").contains("w510"))
    }

    @Test
    fun lookupContextIsTitleSummaryAndNearbyTextNotTheWholeArticle() {
        val article = CleanArticleResult(
            title = "SEC directs GBA to use final SIR rolls",
            subtitle = "Subtitle",
            author = "Reporter",
            publishedDate = "November 13",
            cleanArticle = body,
            summary = ArticleSummary("The SEC told the GBA to use the final rolls.", "", "", "")
        )
        val nearby = body.contextAround(body.indexOf("move court"))

        val sent = article.contextForMeaning(nearby)

        assertEquals(
            "Title: SEC directs GBA to use final SIR rolls\n" +
                "Summary: The SEC told the GBA to use the final rolls.\n" +
                "Nearby text: $nearby",
            sent
        )
        assertFalse(sent.contains("Hariprasad"))
    }
}
