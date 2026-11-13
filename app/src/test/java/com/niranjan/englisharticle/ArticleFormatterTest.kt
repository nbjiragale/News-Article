package com.niranjan.englisharticle

import com.niranjan.englisharticle.domain.CleanArticleResult
import com.niranjan.englisharticle.domain.ArticleFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleFormatterTest {
    @Test
    fun format_removesCommonNewsPageClutterAndKeepsArticleText() {
        val rawArticle = """
            Home
            News
            Advertisement
            Government announces new education policy
            Subscribe to our newsletter
            The government announced a new education policy on Monday.
            The policy will focus on language learning and digital classrooms.
            Read more
            https://example.com/news/story
            The policy will focus on language learning and digital classrooms.
        """.trimIndent()

        val formatted = ArticleFormatter.format(rawArticle)

        assertTrue(formatted.contains("Government announces new education policy"))
        assertTrue(formatted.contains("The government announced a new education policy on Monday."))
        assertTrue(formatted.contains("The policy will focus on language learning and digital classrooms."))
        assertFalse(formatted.contains("Advertisement"))
        assertFalse(formatted.contains("Subscribe"))
        assertFalse(formatted.contains("https://example.com"))
        assertFalse(formatted.contains("Home"))
    }

    @Test
    fun articleFallback_replacesShortAiExtractionWithLocalFormattedArticle() {
        val articleLines = (1..35).joinToString("\n") {
            "The education department said classroom library number $it will receive new English books for students this year."
        }
        val rawArticle = """
            Home
            News
            Advertisement
            Education department expands classroom libraries
            Subscribe to our newsletter
            $articleLines
        """.trimIndent()
        val aiResult = CleanArticleResult(
            title = "Education department expands classroom libraries",
            subtitle = "",
            author = "",
            publishedDate = "",
            cleanArticle = "The department announced a reading plan."
        )

        val recovered = aiResult.withLocalArticleFallback(rawArticle)

        assertEquals("Education department expands classroom libraries", recovered.title)
        assertTrue(recovered.cleanArticle.contains("classroom library number 35"))
        assertFalse(recovered.cleanArticle.contains("Advertisement"))
        assertFalse(recovered.cleanArticle.contains("Subscribe"))
    }

    @Test
    fun postClean_keepsRealParagraphsThatMentionClutterWords() {
        val paragraphs = listOf(
            "The minister's comments drew sharp criticism from opposition leaders on Tuesday, who said the plan ignored farmers.",
            "Officials said climate-related losses had doubled, and insurance premium costs were rising across the region.",
            "She was promoted to chief executive last year after the company's shares jumped on strong quarterly results.",
            "Facebook and YouTube said they would remove the videos, while Twitter did not respond to a request for comment.",
            "His comments drew sharp criticism."
        )
        val aiOutput = paragraphs.joinToString("\n\n")

        val cleaned = ArticleFormatter.postClean(aiOutput)

        paragraphs.forEach { paragraph ->
            assertTrue("Paragraph was dropped: $paragraph", cleaned.contains(paragraph))
        }
    }

    @Test
    fun postClean_stillRemovesShortClutterLines() {
        val aiOutput = listOf(
            "The council approved the new budget on Monday after a long debate.",
            "Read more",
            "Related stories",
            "Follow us on Facebook",
            "Story continues below advertisement",
            "View comments",
            "The budget sets aside more money for schools and roads next year."
        ).joinToString("\n\n")

        val cleaned = ArticleFormatter.postClean(aiOutput)

        assertTrue(cleaned.contains("The council approved the new budget"))
        assertTrue(cleaned.contains("The budget sets aside more money"))
        assertFalse(cleaned.contains("Read more"))
        assertFalse(cleaned.contains("Related stories"))
        assertFalse(cleaned.contains("Follow us"))
        assertFalse(cleaned.contains("Story continues"))
        assertFalse(cleaned.contains("View comments"))
    }

    @Test
    fun format_keepsLongBodyLinesThatMentionClutterWords() {
        val rawArticle = """
            Home
            Advertisement
            Minister responds to criticism
            The minister's comments drew sharp criticism from opposition leaders on Tuesday.
            Officials said the recommended changes would be promoted in schools from June.
            Subscribe to our newsletter
        """.trimIndent()

        val formatted = ArticleFormatter.format(rawArticle)

        assertTrue(formatted.contains("The minister's comments drew sharp criticism"))
        assertTrue(formatted.contains("recommended changes would be promoted"))
        assertFalse(formatted.contains("Subscribe"))
        assertFalse(formatted.contains("Advertisement"))
    }

    @Test
    fun format_removesLongCallToActionAndPhotoCreditLines() {
        // Lines from a real news page that the short-line rule alone let through.
        val rawArticle = """
            SEC directs GBA to use final SIR rolls for civic polls
            A view of the Greater Bengaluru Authority office in Bengaluru. | Photo Credit: File photo
            The State Election Commission has directed the authority to use the final rolls.
            Sign up for free and get curated news updates
            The exercise has to be completed by November 12, the letter stated.
        """.trimIndent()

        val formatted = ArticleFormatter.format(rawArticle)

        assertTrue(formatted.contains("The State Election Commission has directed the authority"))
        assertTrue(formatted.contains("The exercise has to be completed by November 12"))
        assertFalse(formatted.contains("Sign up"))
        assertFalse(formatted.contains("Photo Credit"))
    }
}
