package com.niranjan.englisharticle

import com.niranjan.englisharticle.ui.tts.SttWord
import com.niranjan.englisharticle.ui.tts.WordTimings
import com.niranjan.englisharticle.ui.tts.alignSttWithSource
import com.niranjan.englisharticle.ui.tts.alignSttWithSourceVerbose
import com.niranjan.englisharticle.ui.tts.applyAudioOffset
import com.niranjan.englisharticle.ui.tts.buildChunkPlans
import com.niranjan.englisharticle.ui.tts.chunkForTts
import com.niranjan.englisharticle.ui.tts.parseSttWords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        assertEquals(listOf("one", "two", "three"), plans[0].sourceWords)
        assertEquals(intArrayOf(3, 3, 5).toList(), plans[0].wordCharLengths.toList())
    }

    @Test
    fun `estimated word starts are monotonically increasing within a chunk`() {
        val plans = buildChunkPlans(listOf("alpha bravo charlie delta echo"))
        val starts = plans[0].estimatedWordStartsForDuration(durationMs = 5000)

        assertEquals(5, starts.size)
        assertEquals(0, starts[0])
        for (i in 1 until starts.size) {
            assertTrue(
                "Timings must be non-decreasing: ${starts.toList()}",
                starts[i] >= starts[i - 1]
            )
            assertTrue("All timings must be within duration", starts[i] < 5000)
        }
    }

    @Test
    fun `indexForPosition picks the latest word started by the given timestamp`() {
        val plans = buildChunkPlans(listOf("aa bb cc"))
        val starts = plans[0].estimatedWordStartsForDuration(durationMs = 1500)
        val timings = WordTimings(starts)

        assertEquals(-1, timings.indexForPosition(timings.startMs[0] - 1))
        assertEquals(0, timings.indexForPosition(timings.startMs[0]))
        assertEquals(1, timings.indexForPosition(timings.startMs[1]))
        assertEquals(1, timings.indexForPosition(timings.startMs[2] - 1))
        assertEquals(2, timings.indexForPosition(timings.startMs[2]))
        assertEquals(2, timings.indexForPosition(10_000))
    }

    // ---- STT alignment ----

    @Test
    fun `alignment produces exact start times when stt words match source 1 to 1`() {
        val source = listOf("Hello", "world", "today")
        val stt = listOf(
            SttWord("hello", 100, 400),
            SttWord("world", 500, 800),
            SttWord("today", 900, 1300)
        )
        val starts = alignSttWithSource(source, stt)
        assertNotNull(starts)
        assertEquals(listOf(100, 500, 900), starts!!.toList())
    }

    @Test
    fun `alignment is case insensitive and ignores punctuation in source word`() {
        val source = listOf("Hello,", "world!")
        val stt = listOf(
            SttWord("hello", 0, 300),
            SttWord("world", 400, 800)
        )
        val starts = alignSttWithSource(source, stt)
        assertNotNull(starts)
        assertEquals(listOf(0, 400), starts!!.toList())
    }

    @Test
    fun `alignment interpolates a missing source word between matched neighbors`() {
        val source = listOf("alpha", "xqz", "gamma")
        val stt = listOf(
            SttWord("alpha", 0, 200),
            SttWord("gamma", 800, 1200)
        )
        val starts = alignSttWithSource(source, stt)
        assertNotNull(starts)
        assertEquals(0, starts!![0])
        assertEquals(800, starts[2])
        assertTrue(
            "Missing word should be interpolated between neighbors: ${starts.toList()}",
            starts[1] in 1..799
        )
    }

    @Test
    fun `alignment returns null when fewer than half match`() {
        val source = listOf("alpha", "beta", "gamma", "delta")
        val stt = listOf(SttWord("zzz", 0, 100))
        val starts = alignSttWithSource(source, stt)
        assertNull(starts)
    }

    @Test
    fun `alignment is monotonically non-decreasing`() {
        val source = listOf("a", "b", "c", "d", "e", "f")
        val stt = listOf(
            SttWord("a", 0, 100),
            SttWord("b", 200, 300),
            SttWord("c", 400, 500),
            SttWord("d", 600, 700),
            SttWord("e", 800, 900),
            SttWord("f", 1000, 1100)
        )
        val starts = alignSttWithSource(source, stt)!!
        for (i in 1 until starts.size) {
            assertTrue("Non-decreasing required", starts[i] >= starts[i - 1])
        }
    }

    @Test
    fun `parseSttWords reads start times in milliseconds`() {
        val json = """
            {"results":{"channels":[{"alternatives":[{"words":[
              {"word":"hello","start":0.12,"end":0.45},
              {"word":"world","start":0.50,"end":0.95}
            ]}]}]}}
        """.trimIndent()
        val words = parseSttWords(json)
        assertEquals(2, words.size)
        assertEquals("hello", words[0].word)
        assertEquals(120, words[0].startMs)
        assertEquals(450, words[0].endMs)
        assertEquals(500, words[1].startMs)
        assertEquals(950, words[1].endMs)
    }

    @Test
    fun `parseSttWords prefers punctuated_word when present`() {
        val json = """
            {"results":{"channels":[{"alternatives":[{"words":[
              {"word":"hello","punctuated_word":"Hello,","start":0.0,"end":0.4}
            ]}]}]}}
        """.trimIndent()
        val words = parseSttWords(json)
        assertEquals("Hello,", words[0].word)
    }

    @Test
    fun `parseSttWords on empty results returns empty list`() {
        val words = parseSttWords("""{"results":{"channels":[]}}""")
        assertEquals(0, words.size)
    }

    @Test
    fun `alignment lookahead absorbs number-spelling token expansion`() {
        // "in 2024 the report" -> STT spells the year as four tokens.
        val source = listOf("in", "2024", "the", "report")
        val stt = listOf(
            SttWord("in", 0, 100),
            SttWord("twenty", 200, 400),
            SttWord("twenty", 400, 600),
            SttWord("twenty", 600, 800),
            SttWord("four", 800, 1000),
            SttWord("the", 1100, 1200),
            SttWord("report", 1300, 1700)
        )
        val starts = alignSttWithSource(source, stt)
        assertNotNull(starts)
        assertEquals(0, starts!![0])
        // "the" must align even though five extra STT tokens sit between "in" and "the".
        assertEquals(1100, starts[2])
        assertEquals(1300, starts[3])
    }

    @Test
    fun `verbose alignment reports matched count`() {
        val source = listOf("alpha", "beta", "gamma")
        val stt = listOf(
            SttWord("alpha", 0, 100),
            SttWord("gamma", 400, 500)
        )
        val (starts, matched) = alignSttWithSourceVerbose(source, stt)
        assertNotNull(starts)
        assertEquals(2, matched)
    }

    @Test
    fun `applyAudioOffset shifts every start by the given milliseconds`() {
        val starts = intArrayOf(0, 100, 250, 999)
        val shifted = applyAudioOffset(starts, 150)
        assertEquals(listOf(150, 250, 400, 1149), shifted.toList())
    }

    @Test
    fun `applyAudioOffset zero is a no-op`() {
        val starts = intArrayOf(10, 20, 30)
        val shifted = applyAudioOffset(starts, 0)
        assertEquals(starts.toList(), shifted.toList())
    }

    @Test
    fun `applyAudioOffset clamps negative results at zero`() {
        val starts = intArrayOf(0, 100, 200)
        val shifted = applyAudioOffset(starts, -150)
        assertEquals(listOf(0, 0, 50), shifted.toList())
    }
}
