package com.niranjan.englisharticle

import com.niranjan.englisharticle.data.YouTubeTranscriptService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

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
    fun normalizeTranscriptUrl_keepsAbsoluteUrlsAsIs() {
        val original = "https://www.youtube.com/api/timedtext?v=dQw4w9WgXcQ"
        assertEquals(original, YouTubeTranscriptService.normalizeTranscriptUrl(original))
    }

    @Test
    fun normalizeTranscriptUrl_convertsRootRelativePathToAbsolute() {
        assertEquals(
            "https://www.youtube.com/api/timedtext?v=dQw4w9WgXcQ",
            YouTubeTranscriptService.normalizeTranscriptUrl("/api/timedtext?v=dQw4w9WgXcQ")
        )
    }

    @Test
    fun fetchTranscript_liveVideo_producesTranscriptForProvidedLink() = runBlocking {
        assumeTrue(
            "Set RUN_LIVE_YOUTUBE_TESTS=true to run live YouTube transcript test.",
            System.getenv("RUN_LIVE_YOUTUBE_TESTS") == "true"
        )

        val service = YouTubeTranscriptService()
        val result = service.fetchTranscript("https://youtu.be/TozN7z3Yif8?si=_Z56Abt5z_y64U_P")

        assertEquals("TozN7z3Yif8", result.videoId)
        assertTrue("Expected non-empty title.", result.title.isNotBlank())
        assertTrue("Expected non-empty transcript.", result.transcript.isNotBlank())
        assertTrue(
            "Expected transcript with at least 20 words.",
            result.transcript.split(Regex("\\s+")).count { it.isNotBlank() } >= 20
        )
    }
}
