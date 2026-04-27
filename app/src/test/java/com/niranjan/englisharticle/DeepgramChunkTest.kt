package com.niranjan.englisharticle

import com.niranjan.englisharticle.ui.tts.buildChunkPlans
import com.niranjan.englisharticle.ui.tts.chunkForTts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepgramChunkTest {
    @Test
    fun `short text returns single chunk`() {
        val result = "Hello world.".chunkForTts(maxChars = 1800)
        assertEquals(listOf("Hello world."), result)
    }

    @Test
    fun `splits at sentence boundaries when over limit`() {
        val sentence = "This is a sentence about news. "
        val text = sentence.repeat(10).trim()
        val result = text.chunkForTts(maxChars = sentence.length * 3)

        assertTrue("Expected multiple chunks, got ${result.size}", result.size >= 2)
        assertTrue(result.all { it.length <= sentence.length * 3 })
        val reassembled = result.joinToString(" ").replace(Regex("\\s+"), " ").trim()
        val original = text.replace(Regex("\\s+"), " ").trim()
        assertEquals(original, reassembled)
    }

    @Test
    fun `single very long sentence is hard-split`() {
        val longSentence = "a".repeat(5000)
        val result = longSentence.chunkForTts(maxChars = 1000)

        assertEquals(5, result.size)
        assertTrue(result.all { it.length <= 1000 })
        assertEquals(longSentence, result.joinToString(""))
    }

    @Test
    fun `blank input returns single blank chunk`() {
        val result = "   ".chunkForTts(maxChars = 100)
        assertEquals(listOf(""), result)
    }

    @Test
    fun `chunk plans assign global word indexes contiguously`() {
        val chunks = listOf("one two three", "four five", "six")
        val plans = buildChunkPlans(chunks)

        assertEquals(0, plans[0].firstGlobalWordIndex)
        assertEquals(3, plans[1].firstGlobalWordIndex)
        assertEquals(5, plans[2].firstGlobalWordIndex)
        assertEquals(intArrayOf(3, 3, 5).toList(), plans[0].wordCharLengths.toList())
    }

    @Test
    fun `word timings are monotonically increasing within a chunk`() {
        val plans = buildChunkPlans(listOf("alpha bravo charlie delta echo"))
        val timings = plans[0].wordTimingsForDuration(durationMs = 5000)

        assertEquals(5, timings.startMs.size)
        assertEquals(0, timings.startMs[0])
        for (i in 1 until timings.startMs.size) {
            assertTrue(
                "Timings must be non-decreasing: ${timings.startMs.toList()}",
                timings.startMs[i] >= timings.startMs[i - 1]
            )
            assertTrue(
                "All timings must be within duration",
                timings.startMs[i] < 5000
            )
        }
    }

    @Test
    fun `indexForPosition picks the latest word started by the given timestamp`() {
        val plans = buildChunkPlans(listOf("aa bb cc"))
        val timings = plans[0].wordTimingsForDuration(durationMs = 1500)

        assertEquals(-1, timings.indexForPosition(timings.startMs[0] - 1))
        assertEquals(0, timings.indexForPosition(timings.startMs[0]))
        assertEquals(1, timings.indexForPosition(timings.startMs[1]))
        assertEquals(1, timings.indexForPosition(timings.startMs[2] - 1))
        assertEquals(2, timings.indexForPosition(timings.startMs[2]))
        assertEquals(2, timings.indexForPosition(10_000))
    }
}
