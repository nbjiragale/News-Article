package com.niranjan.englisharticle.data

import android.content.Context
import com.niranjan.englisharticle.BuildConfig
import java.net.HttpURLConnection

/**
 * How the app reaches OpenRouter (AI) and Deepgram (read-aloud). Two modes:
 *
 * - [Direct]: the app calls OpenRouter and Deepgram itself with keys from local.properties.
 *   Simple, no server; fine for a personal build that stays on your own phone. The keys
 *   end up inside the APK, so never share that APK, and set spending limits on both keys.
 * - [Server]: the app calls its own server (server/ in the repository), which holds the
 *   keys. Use this before giving the app to anyone else.
 *
 * Chosen at build time: api.base.url set -> [Server]; otherwise [Direct].
 */
sealed interface ApiAccess {
    /** True when AI requests can be made (a key or a server address is set). */
    val isAiConfigured: Boolean

    /** True when Deepgram requests can be made. */
    val isSpeechConfigured: Boolean

    val chatUrl: String

    /** The model to name in the request, or null when the server chooses it. */
    val chatModel: String?

    fun speakUrl(voice: String): String

    val listenUrl: String

    fun applyAiHeaders(connection: HttpURLConnection)

    fun applySpeechHeaders(connection: HttpURLConnection)

    /** Message shown when the service answers 401/403. */
    val rejectedMessage: String

    data class Direct(
        val openRouterKey: String,
        val model: String,
        val deepgramKey: String,
        val openRouterUrl: String = OPENROUTER_CHAT_URL,
        val deepgramBaseUrl: String = DEEPGRAM_BASE_URL
    ) : ApiAccess {
        override val isAiConfigured: Boolean get() = openRouterKey.isNotBlank()
        override val isSpeechConfigured: Boolean get() = deepgramKey.isNotBlank()
        override val chatUrl: String get() = openRouterUrl
        override val chatModel: String get() = model
        override fun speakUrl(voice: String): String = "$deepgramBaseUrl/v1/speak?model=$voice&encoding=mp3"
        override val listenUrl: String
            get() = "$deepgramBaseUrl/v1/listen?model=nova-3&punctuate=true&smart_format=false&filler_words=false"

        override fun applyAiHeaders(connection: HttpURLConnection) {
            connection.setRequestProperty("Authorization", "Bearer $openRouterKey")
            connection.setRequestProperty("X-Title", "ArthaReader")
        }

        override fun applySpeechHeaders(connection: HttpURLConnection) {
            connection.setRequestProperty("Authorization", "Token $deepgramKey")
        }

        override val rejectedMessage: String
            get() = "The API key was rejected. Check openrouter.api.key in local.properties."

        // Keys must never reach logs or crash reports.
        override fun toString(): String = "ApiAccess.Direct(model=$model)"
    }

    data class Server(val proxy: ApiProxy) : ApiAccess {
        override val isAiConfigured: Boolean get() = proxy.isConfigured
        override val isSpeechConfigured: Boolean get() = proxy.isConfigured
        override val chatUrl: String get() = proxy.url("/v1/chat")
        override val chatModel: String? get() = null
        override fun speakUrl(voice: String): String = proxy.url("/v1/speak?voice=$voice")
        override val listenUrl: String get() = proxy.url("/v1/listen")
        override fun applyAiHeaders(connection: HttpURLConnection) = proxy.applyHeaders(connection)
        override fun applySpeechHeaders(connection: HttpURLConnection) = proxy.applyHeaders(connection)
        override val rejectedMessage: String
            get() = "The app's server did not accept this app. Please update the app."
    }

    companion object {
        const val OPENROUTER_CHAT_URL = "https://openrouter.ai/api/v1/chat/completions"
        const val DEEPGRAM_BASE_URL = "https://api.deepgram.com"

        /** A server address wins; otherwise the app uses the keys directly. */
        fun select(
            serverBaseUrl: String,
            serverProxy: () -> ApiProxy,
            openRouterKey: String,
            model: String,
            deepgramKey: String
        ): ApiAccess = if (serverBaseUrl.isNotBlank()) {
            Server(serverProxy())
        } else {
            Direct(openRouterKey = openRouterKey, model = model, deepgramKey = deepgramKey)
        }

        fun fromBuildConfig(context: Context): ApiAccess = select(
            serverBaseUrl = BuildConfig.API_BASE_URL,
            serverProxy = { ApiProxy.fromBuildConfig(context) },
            openRouterKey = BuildConfig.OPENROUTER_API_KEY,
            model = BuildConfig.OPENROUTER_MODEL,
            deepgramKey = BuildConfig.DEEPGRAM_API_KEY
        )
    }
}
