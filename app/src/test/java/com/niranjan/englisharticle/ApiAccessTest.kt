package com.niranjan.englisharticle

import com.niranjan.englisharticle.data.ApiAccess
import com.niranjan.englisharticle.data.ApiProxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class ApiAccessTest {
    private val proxy = ApiProxy("https://my-server.example", "app-token", "install-1234567890")

    private fun select(serverUrl: String, openRouterKey: String = "or-key", deepgramKey: String = "dg-key") =
        ApiAccess.select(
            serverBaseUrl = serverUrl,
            serverProxy = { proxy },
            openRouterKey = openRouterKey,
            model = "google/gemini-2.5-flash",
            deepgramKey = deepgramKey
        )

    /**
     * Records headers. A real HttpURLConnection hides the Authorization header from
     * getRequestProperty (a JDK safety rule), so it cannot be used to check it.
     */
    private class RecordingConnection(url: String) : HttpURLConnection(URL(url)) {
        val headers = mutableMapOf<String, String>()
        override fun setRequestProperty(key: String, value: String) { headers[key] = value }
        override fun getRequestProperty(key: String): String? = headers[key]
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
    }

    private fun connection(url: String) = RecordingConnection(url)

    @Test
    fun withoutServerAddressTheAppUsesKeysDirectly() {
        val access = select(serverUrl = "")

        assertTrue(access is ApiAccess.Direct)
        assertEquals("https://openrouter.ai/api/v1/chat/completions", access.chatUrl)
        assertEquals("google/gemini-2.5-flash", access.chatModel)
        assertEquals(
            "https://api.deepgram.com/v1/speak?model=aura-asteria-en&encoding=mp3",
            access.speakUrl("aura-asteria-en")
        )
        val ai = connection(access.chatUrl).also(access::applyAiHeaders)
        val speech = connection(access.speakUrl("aura-asteria-en")).also(access::applySpeechHeaders)
        assertEquals("Bearer or-key", ai.getRequestProperty("Authorization"))
        assertEquals("Token dg-key", speech.getRequestProperty("Authorization"))
    }

    @Test
    fun serverAddressWinsAndNoKeyOrModelIsSent() {
        val access = select(serverUrl = "https://my-server.example")

        assertTrue(access is ApiAccess.Server)
        assertEquals("https://my-server.example/v1/chat", access.chatUrl)
        assertNull("the server chooses the model", access.chatModel)
        assertEquals("https://my-server.example/v1/speak?voice=aura-asteria-en", access.speakUrl("aura-asteria-en"))
        val ai = connection(access.chatUrl).also(access::applyAiHeaders)
        assertNull(ai.getRequestProperty("Authorization"))
        assertEquals("app-token", ai.getRequestProperty("X-App-Token"))
        assertEquals("install-1234567890", ai.getRequestProperty("X-Install-Id"))
    }

    @Test
    fun missingKeysAreReportedAsNotConfigured() {
        val access = select(serverUrl = "", openRouterKey = "", deepgramKey = "")

        assertFalse(access.isAiConfigured)
        assertFalse(access.isSpeechConfigured)
    }

    @Test
    fun deepgramIsOptionalInDirectMode() {
        val access = select(serverUrl = "", deepgramKey = "")

        assertTrue(access.isAiConfigured)
        assertFalse(access.isSpeechConfigured)
    }

    @Test
    fun keysNeverAppearInToString() {
        val text = select(serverUrl = "").toString()

        assertFalse(text, text.contains("or-key"))
        assertFalse(text, text.contains("dg-key"))
    }
}
