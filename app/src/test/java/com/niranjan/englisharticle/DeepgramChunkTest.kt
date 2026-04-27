package com.niranjan.englisharticle

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
        // Reassembled chunks should preserve original characters (modulo whitespace).
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
}
