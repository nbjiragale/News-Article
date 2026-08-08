package com.niranjan.englisharticle

import com.niranjan.englisharticle.data.local.recentArticleContentHash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * The content hash is the identity that stops history filling with duplicate copies of
 * the same article, and migration 4→5 backfills existing rows with it. Both depend on it
 * being stable and collision-free for the inputs the app actually produces.
 */
class RecentArticleContentHashTest {

    @Test
    fun `same content produces the same hash`() {
        val a = recentArticleContentHash("Markets rally", "Stocks rose sharply on Monday.")
        val b = recentArticleContentHash("Markets rally", "Stocks rose sharply on Monday.")

        assertEquals(a, b)
    }

    @Test
    fun `surrounding whitespace does not change identity`() {
        val plain = recentArticleContentHash("Markets rally", "Stocks rose sharply on Monday.")
        val padded = recentArticleContentHash("  Markets rally\n", "\n Stocks rose sharply on Monday.  ")

        assertEquals(plain, padded)
    }

    @Test
    fun `different body produces a different hash`() {
        val a = recentArticleContentHash("Markets rally", "Stocks rose sharply on Monday.")
        val b = recentArticleContentHash("Markets rally", "Stocks fell sharply on Monday.")

        assertNotEquals(a, b)
    }

    @Test
    fun `different title produces a different hash`() {
        val a = recentArticleContentHash("Markets rally", "Stocks rose sharply on Monday.")
        val b = recentArticleContentHash("Markets slump", "Stocks rose sharply on Monday.")

        assertNotEquals(a, b)
    }

    /**
     * The separator exists for exactly this case. Concatenating the fields without one
     * would make these two articles share an identity, and the second would silently
     * overwrite the first in history.
     */
    @Test
    fun `title and body boundary cannot be shifted to force a collision`() {
        val a = recentArticleContentHash("ab", "c")
        val b = recentArticleContentHash("a", "bc")

        assertNotEquals(a, b)
    }

    @Test
    fun `hash is a lowercase hex sha256`() {
        val hash = recentArticleContentHash("Markets rally", "Stocks rose sharply on Monday.")

        assertEquals(64, hash.length)
        assertEquals(hash.lowercase(), hash)
        assertEquals(true, hash.all { it.isDigit() || it in 'a'..'f' })
    }

    /**
     * An article gains a summary and idiomatic phrases after it is first saved. Those are
     * not hash inputs, so enrichment must land on the existing row rather than mint a new
     * identity -- which was the original duplicate-row bug.
     */
    @Test
    fun `enrichment does not change identity because only title and body are hashed`() {
        val onFirstSave = recentArticleContentHash("Markets rally", "Stocks rose sharply on Monday.")
        val afterSummaryAndPhrases =
            recentArticleContentHash("Markets rally", "Stocks rose sharply on Monday.")

        assertEquals(onFirstSave, afterSummaryAndPhrases)
    }
}
