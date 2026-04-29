package com.niranjan.englisharticle

import com.niranjan.englisharticle.data.YouTubeTranscriptService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeTranscriptServiceTest {

    @Test
    fun extractVideoId_handlesStandardWatchUrl() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeTranscriptService.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        )
    }

    @Test
    fun extractVideoId_handlesWatchUrlWithExtraParams() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeTranscriptService.extractVideoId(
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42s&list=PL12345"
            )
        )
    }

    @Test
    fun extractVideoId_handlesShortUrl() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeTranscriptService.extractVideoId("https://youtu.be/dQw4w9WgXcQ?t=10")
        )
    }

    @Test
    fun extractVideoId_handlesShortsUrl() {
        assertEquals(
            "abcDEF12345",
            YouTubeTranscriptService.extractVideoId("https://www.youtube.com/shorts/abcDEF12345")
        )
    }

    @Test
    fun extractVideoId_handlesEmbedUrl() {
        assertEquals(
            "abcDEF12345",
            YouTubeTranscriptService.extractVideoId("https://www.youtube.com/embed/abcDEF12345?rel=0")
        )
    }

    @Test
    fun extractVideoId_handlesMobileUrl() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeTranscriptService.extractVideoId("https://m.youtube.com/watch?v=dQw4w9WgXcQ")
        )
    }

    @Test
    fun extractVideoId_handlesNoCookieEmbed() {
        assertEquals(
            "abcDEF12345",
            YouTubeTranscriptService.extractVideoId("https://www.youtube-nocookie.com/embed/abcDEF12345")
        )
    }

    @Test
    fun extractVideoId_handlesBareId() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeTranscriptService.extractVideoId("dQw4w9WgXcQ")
        )
    }

    @Test
    fun extractVideoId_returnsNullForUnrelatedUrl() {
        assertNull(YouTubeTranscriptService.extractVideoId("https://example.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun extractVideoId_returnsNullForBlank() {
        assertNull(YouTubeTranscriptService.extractVideoId("   "))
    }

    @Test
    fun extractVideoId_returnsNullForShortStringWithoutVideoId() {
        assertNull(YouTubeTranscriptService.extractVideoId("https://www.youtube.com/"))
    }

    @Test
    fun extractPlayerResponseJson_parsesVarAssignment() {
        val html = """
            <html><head></head><body>
              <script>
                var ytInitialPlayerResponse = {"videoDetails":{"title":"Hello \"World\""},"x":"}{"};
                var foo = 1;
              </script>
            </body></html>
        """.trimIndent()

        val json = YouTubeTranscriptService.extractPlayerResponseJson(html)
        assertEquals(
            """{"videoDetails":{"title":"Hello \"World\""},"x":"}{"}""",
            json
        )
    }

    @Test
    fun extractPlayerResponseJson_parsesEmbeddedKey() {
        val html = """{"foo":1,"ytInitialPlayerResponse":{"videoDetails":{"title":"x"}}}"""
        val json = YouTubeTranscriptService.extractPlayerResponseJson(html)
        assertEquals("""{"videoDetails":{"title":"x"}}""", json)
    }

    @Test
    fun extractPlayerResponseJson_returnsNullWhenAbsent() {
        val html = "<html><body>no player response here</body></html>"
        assertNull(YouTubeTranscriptService.extractPlayerResponseJson(html))
    }
}
