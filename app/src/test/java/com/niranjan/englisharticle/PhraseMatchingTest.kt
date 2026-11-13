package com.niranjan.englisharticle

import com.niranjan.englisharticle.data.phraseChunks
import com.niranjan.englisharticle.domain.PhraseCandidate
import com.niranjan.englisharticle.domain.PhraseType
import com.niranjan.englisharticle.domain.resolvePhraseSpans
import com.niranjan.englisharticle.domain.toMeaningTokenGroups
import com.niranjan.englisharticle.domain.toWordTokens
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhraseMatchingTest {
    private fun resolve(article: String, vararg candidates: PhraseCandidate) =
        resolvePhraseSpans(article, candidates.toList())

    @Test
    fun exactTextIsKept() {
        assertEquals(
            listOf("bite the bullet"),
            resolve("The minister decided to bite the bullet.", PhraseCandidate("bite the bullet"))
        )
    }

    @Test
    fun inflectedFormInArticleIsFoundFromBaseForm() {
        val article = "The minister was biting the bullet on Monday."
        assertEquals(
            listOf("biting the bullet"),
            resolve(article, PhraseCandidate(text = "bite the bullet", base = "bite the bullet"))
        )
    }

    @Test
    fun irregularPastTenseIsFound() {
        val article = "Investors took the plunge after the results."
        assertEquals(
            listOf("took the plunge"),
            resolve(article, PhraseCandidate(text = "take the plunge", base = "take the plunge"))
        )
    }

    @Test
    fun possessivePlaceholderMatchesRealPronoun() {
        val article = "He was pulling my leg about the news."
        assertEquals(
            listOf("pulling my leg"),
            resolve(article, PhraseCandidate(text = "pull someone's leg", base = "pull someone's leg"))
        )
    }

    @Test
    fun objectPlaceholderMatchesWordsInsideAPhrasalVerb() {
        val article = "The board turned the generous offer down on Friday."
        assertEquals(
            listOf("turned the generous offer down"),
            resolve(article, PhraseCandidate(text = "", base = "turn something down"))
        )
    }

    @Test
    fun curlyApostropheAndLineBreakDoNotPreventAMatch() {
        val article = "It’s water under\nthe bridge now, he said."
        assertEquals(
            listOf("It's water under the bridge"),
            resolve(article, PhraseCandidate("it's water under the bridge"))
        )
    }

    @Test
    fun phraseDoesNotCrossASentenceEnd() {
        val article = "They had to call it. A day later talks resumed."
        assertTrue(resolve(article, PhraseCandidate("call it a day")).isEmpty())
    }

    @Test
    fun shortWordsMustMatchExactly() {
        // "the" must not match "then" through the inflection rules.
        val article = "We went then bullet points were added."
        assertTrue(resolve(article, PhraseCandidate(text = "", base = "go the bullet")).isEmpty())
    }

    @Test
    fun namesAreDroppedEvenIfTheModelReturnsThem() {
        val article = "Wall Street rallied after the Prime Minister spoke."
        assertTrue(resolve(article, PhraseCandidate("Wall Street"), PhraseCandidate("Prime Minister")).isEmpty())
    }

    @Test
    fun phraseNotInArticleIsDropped() {
        assertTrue(resolve("Markets rose on Monday.", PhraseCandidate("hit the hay")).isEmpty())
    }

    @Test
    fun resolvedSpansHighlightInTheReader() {
        val article = "Officials will look into it after the minister came under fire, biting the bullet."
        val spans = resolve(
            article,
            PhraseCandidate("look into", "look into", PhraseType.PhrasalVerb),
            PhraseCandidate("come under fire", "come under fire", PhraseType.FixedExpression),
            PhraseCandidate("bite the bullet", "bite the bullet", PhraseType.Idiom)
        )

        val highlighted = article.toWordTokens()
            .toMeaningTokenGroups(spans)
            .filter { it.isPhrase }
            .map { it.cleanText() }

        assertEquals(listOf("look into", "came under fire", "biting the bullet"), highlighted)
    }

    @Test
    fun parsesNewObjectShapeAndOldStringShape() {
        val json = JSONObject(
            """
            {"phrases": [
              {"text": "came under fire", "base": "come under fire", "type": "fixed_expression"},
              {"text": "strong growth", "base": "strong growth", "type": "collocation"},
              {"text": "look into", "base": "look into", "type": "phrasal verb"},
              "water under the bridge"
            ]}
            """.trimIndent()
        )

        val candidates = PhraseCandidate.listFromJson(json)

        assertEquals(
            listOf("came under fire", "look into", "water under the bridge"),
            candidates.map { it.text }
        )
        assertEquals(PhraseType.PhrasalVerb, candidates[1].type)
    }

    @Test
    fun longArticlesAreChunkedBetweenParagraphs() {
        val paragraph = (1..400).joinToString(" ") { "word$it" }
        val article = List(5) { paragraph }.joinToString("\n\n")

        val chunks = article.phraseChunks(maxWords = 1_000)

        assertEquals(3, chunks.size)
        assertEquals(2_000, chunks.sumOf { it.split(Regex("\\s+")).size })
        assertTrue(chunks.all { it.split(Regex("\\s+")).size <= 1_000 })
    }

    @Test
    fun formsOfBeAreMatchedDespiteBeingShortWords() {
        // From a real article: the dictionary form "be yet to" must find "is yet to".
        val article = "The State government is yet to decide whether the rolls should be used."
        assertEquals(
            listOf("is yet to"),
            resolve(article, PhraseCandidate(text = "", base = "be yet to"))
        )
    }
}
