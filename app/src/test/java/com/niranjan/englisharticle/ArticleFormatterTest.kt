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
}
